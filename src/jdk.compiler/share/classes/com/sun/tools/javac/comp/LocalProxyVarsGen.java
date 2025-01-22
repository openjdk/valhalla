/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

/** blah blah
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

    private ClassSymbol currentClass = null;
    private JCClassDecl currentClassTree = null;
    private MethodSymbol currentMethodSym = null;

    @SuppressWarnings("this-escape")
    protected LocalProxyVarsGen(Context context) {
        context.put(valueInitializersKey, this);
        make = TreeMaker.instance(context);
        types = Types.instance(context);
        names = Names.instance(context);
        target = Target.instance(context);
    }

    public JCTree translateTopLevelClass(Env<AttrContext> env, JCTree cdef, TreeMaker make) {
        try {
            this.make = make;
            translate(cdef);
        } finally {
            // note that recursive invocations of this method fail hard
            this.make = null;
        }
        return cdef;
    }

    List<JCVariableDecl> strictInstanceFields;

    @Override
    public void visitClassDef(JCClassDecl tree) {
        ClassSymbol prevCurrentClass = currentClass;
        JCClassDecl prevCurrentClassTree = currentClassTree;
        MethodSymbol prevMethodSym = currentMethodSym;
        List<JCVariableDecl> prevStrictInstanceFieldsWithNoInitializer = strictInstanceFields;
        try {
            currentClass = tree.sym;
            currentClassTree = tree;
            currentMethodSym = null;
            super.visitClassDef(tree);
            if (tree.sym.isValueClass()) {
                strictInstanceFields = tree.defs.stream()
                        .filter(t -> t.hasTag(VARDEF))
                        .map(t -> (JCVariableDecl)t)
                        .filter(vd -> vd.sym.isStrict() && !vd.sym.isStatic())
                        .collect(List.collector());
                if (!strictInstanceFields.isEmpty()) {
                    for (JCTree t : tree.defs) {
                        if (t.hasTag(JCTree.Tag.METHODDEF)) {
                            JCMethodDecl md = (JCMethodDecl) t;
                            if (TreeInfo.isConstructor(md) && TreeInfo.hasConstructorCall(md, names._super)) {
                                addLocalProxiesFor(md);
                            }
                        }
                    }
                }
            }
        } finally {
            currentClass = prevCurrentClass;
            currentClassTree = prevCurrentClassTree;
            currentMethodSym = prevMethodSym;
            strictInstanceFields = prevStrictInstanceFieldsWithNoInitializer;
        }
    }

    /**
     * this map will hold the assignments from proxy locals back to fields that should be done
     * after the code for the super invocation arguments and just before the super invocation
     * this information should be consumed by Gen
     */
    // public Map<JCMethodDecl, JCBlock> assigmentsBeforeSuperMap = new HashMap<>();

    void addLocalProxiesFor(JCMethodDecl constructor) {
        ListBuffer<JCStatement> localDeclarations = new ListBuffer<>();
        Map<Symbol, Symbol> fieldToLocalMap = new LinkedHashMap<>();

        for (JCVariableDecl fieldDecl : strictInstanceFields) {
            long flags = SYNTHETIC;
            VarSymbol proxy = new VarSymbol(flags, newLocalName(fieldDecl.name.toString()), fieldDecl.sym.erasure(types), constructor.sym);
            fieldToLocalMap.put(fieldDecl.sym, proxy);
            JCVariableDecl localDecl = make.at(constructor.pos).VarDef(proxy, fieldDecl.init);
            localDecl.vartype = fieldDecl.vartype;
            localDeclarations = localDeclarations.append(localDecl);
        }
        FieldRewriter fr = new FieldRewriter(constructor, fieldToLocalMap, make);

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
            // TreeInfo.mapSuperCalls(constructor.body, supercall -> make.Block(0, assigmentsBeforeSuper.toList().append(supercall)));
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
            //assigmentsBeforeSuperMap.put(constructor, make.Block(0, assigmentsBeforeSuper.toList()));
        }
    }

    Name newLocalName(String name) {
        return names.fromString("local" + target.syntheticNameChar() + name);
    }

    static class FieldRewriter extends TreeTranslator {
        JCMethodDecl md;
        Map<Symbol, Symbol> fieldToLocalMap;
        TreeMaker make;

        public FieldRewriter(JCMethodDecl md, Map<Symbol, Symbol> fieldToLocalMap, TreeMaker make) {
            this.md = md;
            this.fieldToLocalMap = fieldToLocalMap;
            this.make = make;
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
    }
/*
    // the idea of this class is to find if there are assignments to fields that are not in the same
    // nesting level as the super invocation, those are the ones for which we need a new local variable
    private class SuperThisChecker extends TreeScanner {

        // Match this scan stack: 1=JCMethodDecl, 2=JCExpressionStatement, 3=JCMethodInvocation
        private static final int MATCH_SCAN_DEPTH = 3;
        private int scanDepth = 0;              // current scan recursion depth in method body
        boolean invokesSuper = false;

        public void check(JCClassDecl classDef) {
            scan(classDef.defs);
        }

        @Override
        public void visitMethodDef(JCMethodDecl tree) {
            Assert.check(scanDepth == 1);
            // Scan method body
            if (tree.body != null) {
                for (List<JCStatement> l = tree.body.stats; l.nonEmpty(); l = l.tail) {
                    scan(l.head);
                }
            }
        }

        @Override
        public void scan(JCTree tree) {
            scanDepth++;
            try {
                super.scan(tree);
            } finally {
                scanDepth--;
            }
        }

        @Override
        public void visitApply(JCTree.JCMethodInvocation apply) {
                // Is this a super() or this() call?
            Name methodName = TreeInfo.name(apply.meth);
            invokesSuper = methodName == names._super;
            // Proceed
            super.visitApply(apply);
        }

        @Override
        public void visitAssign(JCAssign tree) {
            Symbol lhsSym = TreeInfo.symbol(tree.lhs);
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
*/
}
