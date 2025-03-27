/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.tools.javac.comp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import com.sun.tools.javac.code.Symbol.MethodSymbol;

import static com.sun.tools.javac.code.Flags.FINAL;
import static com.sun.tools.javac.code.Flags.SYNTHETIC;
import static com.sun.tools.javac.tree.JCTree.Tag.VARDEF;

import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Options;

/** This phase will add local variable proxies to value classes constructors.
 *  Assignments to instance fields in a constructor will be rewritten as assignments
 *  to the corresponding local proxy variable. Fields will be assigned with its
 *  corresponding local variable proxy just before the super invocation and after
 *  the arguments for the super invocation, if any, have been evaluated.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class LocalProxyVarsGen extends TreeTranslator {

    protected static final Context.Key<LocalProxyVarsGen> valueInitializersKey = new Context.Key<>();

    public static LocalProxyVarsGen instance(Context context) {
        LocalProxyVarsGen instance = context.get(valueInitializersKey);
        if (instance == null)
            instance = new LocalProxyVarsGen(context);
        return instance;
    }

    private final Types types;
    private final Names names;
    private final Target target;
    private TreeMaker make;
    private final UnsetFieldsInfo unsetFieldsInfo;

    private ClassSymbol currentClass = null;
    private JCClassDecl currentClassTree = null;
    private MethodSymbol currentMethodSym = null;

    private final boolean noLocalProxyVars;

    @SuppressWarnings("this-escape")
    protected LocalProxyVarsGen(Context context) {
        context.put(valueInitializersKey, this);
        make = TreeMaker.instance(context);
        types = Types.instance(context);
        names = Names.instance(context);
        target = Target.instance(context);
        unsetFieldsInfo = UnsetFieldsInfo.instance(context);
        Options options = Options.instance(context);
        noLocalProxyVars = options.isSet("noLocalProxyVars");
    }

    public JCTree translateTopLevelClass(JCTree cdef, TreeMaker make) {
        if (!noLocalProxyVars) {
            try {
                this.make = make;
                return translate(cdef);
            } finally {
                // note that recursive invocations of this method fail hard
                this.make = null;
            }
        } else {
            return cdef;
        }
    }

    java.util.List<JCVariableDecl> strictInstanceFields;

    @Override
    public void visitClassDef(JCClassDecl tree) {
        ClassSymbol prevCurrentClass = currentClass;
        JCClassDecl prevCurrentClassTree = currentClassTree;
        MethodSymbol prevMethodSym = currentMethodSym;
        java.util.List<JCVariableDecl> prevStrictInstanceFields = strictInstanceFields;
        try {
            currentClass = tree.sym;
            currentClassTree = tree;
            currentMethodSym = null;
            super.visitClassDef(tree);
            strictInstanceFields = tree.defs.stream()
                    .filter(t -> t.hasTag(VARDEF))
                    .map(t -> (JCVariableDecl)t)
                    .filter(vd -> vd.sym.isStrict() && !vd.sym.isStatic())
                    .collect(List.collector());
            if (!strictInstanceFields.isEmpty()) {
                for (JCTree t : tree.defs) {
                    if (t.hasTag(JCTree.Tag.METHODDEF)) {
                        JCMethodDecl md = (JCMethodDecl) t;
                        // ignore telescopic and generated constructors
                        if (TreeInfo.isConstructor(md) &&
                                TreeInfo.hasConstructorCall(md, names._super) &&
                                (md.sym.flags_field & Flags.GENERATEDCONSTR) == 0) {
                            // now we need to analyze the constructor's body, it could be that it is empty or that
                            // no assignment to strict fields is done
                            ConstructorScanner cs = new ConstructorScanner();
                            cs.scan(md);
                            java.util.List<JCVariableDecl> strictInstanceFieldsAssignedTo = new ArrayList<>();
                            for (Symbol sym : cs.strictFieldsAssignedTo.keySet()) {
                                JCVariableDecl keep = null;
                                // if there is only one assignment there is no point in creating proxy locals, the code
                                // is good as is
                                if (cs.strictFieldsAssignedTo.get(sym) > 1) {
                                    for (JCVariableDecl strictField : strictInstanceFields) {
                                        if (strictField.sym == sym) {
                                            keep = strictField;
                                        }
                                    }
                                    if (keep != null) {
                                        strictInstanceFieldsAssignedTo.add(keep);
                                    }
                                }
                            }
                            if (!strictInstanceFieldsAssignedTo.isEmpty()) {
                                addLocalProxiesFor(md, strictInstanceFieldsAssignedTo);
                            }
                        }
                    }
                }
            }
        } finally {
            currentClass = prevCurrentClass;
            currentClassTree = prevCurrentClassTree;
            currentMethodSym = prevMethodSym;
            strictInstanceFields = prevStrictInstanceFields;
        }
    }

    /**
     * this map will hold the assignments from proxy locals back to fields that should be done
     * after the code for the super invocation arguments and just before the super invocation
     * this information should be consumed by Gen
     */
    // public Map<JCMethodDecl, JCBlock> assigmentsBeforeSuperMap = new HashMap<>();

    void addLocalProxiesFor(JCMethodDecl constructor, java.util.List<JCVariableDecl> strictInstanceFieldsAssignedTo) {
        ListBuffer<JCStatement> localDeclarations = new ListBuffer<>();
        Map<Symbol, Symbol> fieldToLocalMap = new LinkedHashMap<>();

        for (JCVariableDecl fieldDecl : strictInstanceFieldsAssignedTo) {
            long flags = SYNTHETIC;
            VarSymbol proxy = new VarSymbol(flags, newLocalName(fieldDecl.name.toString()), fieldDecl.sym.erasure(types), constructor.sym);
            fieldToLocalMap.put(fieldDecl.sym, proxy);
            JCVariableDecl localDecl = make.at(constructor.pos).VarDef(proxy, fieldDecl.init);
            localDecl.vartype = fieldDecl.vartype;
            localDeclarations = localDeclarations.append(localDecl);
        }

        FieldRewriter fr = new FieldRewriter(constructor, fieldToLocalMap);
        ListBuffer<JCStatement> newBody = new ListBuffer<>();
        for (JCStatement st : constructor.body.stats) {
            newBody = newBody.append(fr.translate(st));
        }
        localDeclarations.addAll(newBody);
        ListBuffer<JCStatement> assigmentsBeforeSuper = new ListBuffer<>();
        for (Symbol vsym : fieldToLocalMap.keySet()) {
            Symbol local = fieldToLocalMap.get(vsym);
            assigmentsBeforeSuper.append(make.at(constructor.pos()).Assignment(vsym, make.at(constructor.pos()).Ident(local)));
        }
        constructor.body.stats = localDeclarations.toList();
        if (!assigmentsBeforeSuper.isEmpty()) {
            JCTree.JCMethodInvocation constructorCall = TreeInfo.findConstructorCall(constructor);
            if (constructorCall.args.isEmpty()) {
                // this is just a super invocation with no arguments we can set the fields just before the invocation
                // and let Gen do the rest
                TreeInfo.mapSuperCalls(constructor.body, supercall -> make.Block(0, assigmentsBeforeSuper.toList().append(supercall)));
            } else {
                // we need to generate fresh local variables to catch the values of the arguments, then
                // assign the proxy locals to the fields and finally invoke the super with the fresh local variables
                int argPosition = 0;
                ListBuffer<JCStatement> superArgsProxies = new ListBuffer<>();
                for (JCExpression arg : constructorCall.args) {
                    long flags = SYNTHETIC | FINAL;
                    VarSymbol proxyForArgSym = new VarSymbol(flags, newLocalName("" + argPosition), types.erasure(arg.type), constructor.sym);
                    JCVariableDecl proxyForArgDecl = make.at(constructor.pos).VarDef(proxyForArgSym, arg);
                    superArgsProxies = superArgsProxies.append(proxyForArgDecl);
                    argPosition++;
                }
                List<JCStatement> superArgsProxiesList = superArgsProxies.toList();
                ListBuffer<JCExpression> newArgs = new ListBuffer<>();
                for (JCStatement argProxy : superArgsProxies) {
                    newArgs.add(make.at(argProxy.pos).Ident((JCVariableDecl) argProxy));
                }
                constructorCall.args = newArgs.toList();
                TreeInfo.mapSuperCalls(constructor.body, supercall -> make.Block(0, superArgsProxiesList.appendList(assigmentsBeforeSuper.toList()).append(supercall)));
            }
        }
    }

    Name newLocalName(String name) {
        return names.fromString("local" + target.syntheticNameChar() + name);
    }

    class FieldRewriter extends TreeTranslator {
        JCMethodDecl md;
        Map<Symbol, Symbol> fieldToLocalMap;

        public FieldRewriter(JCMethodDecl md, Map<Symbol, Symbol> fieldToLocalMap) {
            this.md = md;
            this.fieldToLocalMap = fieldToLocalMap;
        }

        @Override
        public void visitIdent(JCTree.JCIdent tree) {
            if (fieldToLocalMap.get(tree.sym) != null) {
                result = make.at(md).Ident(fieldToLocalMap.get(tree.sym));
            } else {
                result = tree;
            }
        }

        @Override
        public void visitSelect(JCTree.JCFieldAccess tree) {
            super.visitSelect(tree);
            if (fieldToLocalMap.get(tree.sym) != null) {
                result = make.at(md).Ident(fieldToLocalMap.get(tree.sym));
            } else {
                result = tree;
            }
        }

        @Override
        public void visitAssign(JCAssign tree) {
            JCExpression previousLHS = tree.lhs;
            super.visitAssign(tree);
            if (previousLHS != tree.lhs) {
                unsetFieldsInfo.removeUnsetFieldInfo(currentClass, tree);
            }
        }
    }

    // the idea of this class is to find if there are assignments to fields that are not in the same
    // nesting level as the super invocation, those are the ones for which we need a new local variable
    // it is not clear if we will allow free assignment to final fields, in that case we could need to add
    // proxy locals regardless of the nesting level
    private class ConstructorScanner extends TreeScanner {
        // Match this scan stack: 1=JCMethodDecl, 2=JCExpressionStatement, 3=JCMethodInvocation
        private int scanDepth = 0;              // current scan recursion depth in method body
        Map<Symbol, Integer> strictFieldsAssignedTo = new HashMap<>();

        @Override
        public void visitAssign(JCAssign tree) {
            Symbol lhsSym = TreeInfo.symbol(tree.lhs);
            if (lhsSym != null && lhsSym instanceof VarSymbol vs && vs.isStrict()) {
                Integer noOfAssigments = strictFieldsAssignedTo.get(lhsSym);
                if (noOfAssigments == null) {
                    noOfAssigments = 0;
                }
                noOfAssigments++;
                strictFieldsAssignedTo.put(vs, noOfAssigments);
            }
            super.visitAssign(tree);
        }

        @Override
        public void visitClassDef(JCClassDecl tree) {
            // don't descend any further
        }

        @Override
        public void visitLambda(JCLambda tree) {
            // don't descend any further
        }
    }
}
