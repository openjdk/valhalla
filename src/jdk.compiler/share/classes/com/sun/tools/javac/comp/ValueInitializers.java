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

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.TargetType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.jvm.Gen;
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
import static com.sun.tools.javac.code.Flags.STATIC;
import static com.sun.tools.javac.code.Flags.SYNTHETIC;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Position;

/**
 *
 */
public class ValueInitializers extends TreeTranslator {

    protected static final Context.Key<ValueInitializers> valueInitializersKey = new Context.Key<>();

    public static ValueInitializers instance(Context context) {
        ValueInitializers instance = context.get(valueInitializersKey);
        if (instance == null)
            instance = new ValueInitializers(context);
        return instance;
    }

    private final Types types;
    private final Names names;
    private final Target target;
    private TreeMaker make;
    private final Lower lower;

    private ClassSymbol currentClass = null;
    private JCClassDecl currentClassTree = null;
    private MethodSymbol currentMethodSym = null;

    @SuppressWarnings("this-escape")
    protected ValueInitializers(Context context) {
        context.put(valueInitializersKey, this);
        make = TreeMaker.instance(context);
        types = Types.instance(context);
        names = Names.instance(context);
        target = Target.instance(context);
        lower = Lower.instance(context);
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

    @Override
    public void visitClassDef(JCClassDecl tree) {
        ClassSymbol prevCurrentClass = currentClass;
        JCClassDecl prevCurrentClassTree = currentClassTree;
        MethodSymbol prevMethodSym = currentMethodSym;
        try {
            currentClass = tree.sym;
            currentClassTree = tree;
            currentMethodSym = null;
            super.visitClassDef(tree);
            if (tree.sym.isValueClass()) {
                tree.defs = normalizeDefs(tree.defs, tree.sym);
            }
        } finally {
            currentClass = prevCurrentClass;
            currentClassTree = prevCurrentClassTree;
            currentMethodSym = prevMethodSym;
        }
    }

    Map<Symbol, JCVariableDecl> fieldSymInDeclaration = new HashMap<>();

    /** Distribute member initializer code into constructors and {@code <clinit>}
     *  method.
     *  @param defs         The list of class member declarations.
     *  @param c            The enclosing class.
     */
    List<JCTree> normalizeDefs(List<JCTree> defs, ClassSymbol c) {
        ListBuffer<JCStatement> initCode = new ListBuffer<>();
        // only used for value classes
        ListBuffer<Attribute.TypeCompound> initTAs = new ListBuffer<>();
        ListBuffer<JCTree> newDefs = new ListBuffer<>();
        // Sort definitions into one listbuffer:
        //  - initCode for instance initializers
        for (List<JCTree> l = defs; l.nonEmpty(); l = l.tail) {
            JCTree def = l.head;
            switch (def.getTag()) {
                case VARDEF:
                    JCVariableDecl vdef = (JCVariableDecl) def;
                    VarSymbol sym = vdef.sym;
                    if (vdef.init != null) {
                        if ((sym.flags() & STATIC) == 0) {
                            if (fieldSymInDeclaration.get(sym) == null) {
                                fieldSymInDeclaration.put(sym, vdef);
                            }
                            // Always initialize instance variables.
                            JCStatement init = make.at(vdef.pos()).
                                    Assignment(sym, vdef.init);
                            initCode.append(init);
                            initTAs.addAll(getAndRemoveNonFieldTAs(sym));
                            // now we can remove the initializer
                            vdef.init = null;
                        }
                    }
                    newDefs.append(vdef);
                    break;
                default:
                    newDefs.append(def);
            }
        }
        // Insert any instance initializers into all constructors.
        if (initCode.length() != 0) {
            initTAs.addAll(c.getInitTypeAttributes());
            List<Attribute.TypeCompound> initTAlist = initTAs.toList();
            for (JCTree t : newDefs) {
                if (t.hasTag(JCTree.Tag.METHODDEF)) {
                    normalizeMethod((JCMethodDecl) t, initCode.toList(), initTAlist);
                }
            }
        }
        // Return all method definitions.
        return newDefs.toList();
    }

    private List<Attribute.TypeCompound> getAndRemoveNonFieldTAs(VarSymbol sym) {
        List<Attribute.TypeCompound> tas = sym.getRawTypeAttributes();
        ListBuffer<Attribute.TypeCompound> fieldTAs = new ListBuffer<>();
        ListBuffer<Attribute.TypeCompound> nonfieldTAs = new ListBuffer<>();
        for (Attribute.TypeCompound ta : tas) {
            Assert.check(ta.getPosition().type != TargetType.UNKNOWN);
            if (ta.getPosition().type == TargetType.FIELD) {
                fieldTAs.add(ta);
            } else {
                nonfieldTAs.add(ta);
            }
        }
        sym.setTypeAttributes(fieldTAs.toList());
        return nonfieldTAs.toList();
    }

    /** Insert instance initializer code into constructors prior to the super() call.
     *  @param md        The tree potentially representing a
     *                   constructor's definition.
     *  @param initCode  The list of instance initializer statements.
     *  @param initTAs  Type annotations from the initializer expression.
     */
    void normalizeMethod(JCMethodDecl md, List<JCStatement> initCode,  List<Attribute.TypeCompound> initTAs) {
        if (TreeInfo.isConstructor(md) && TreeInfo.hasConstructorCall(md, names._super)) {
            // We are seeing a constructor that has a super() call.
            // Find the super() invocation and append the given initializer code.
            rewriteInitializersIfNeeded(md, initCode);

            List<JCStatement> initCodeWithLocals = createFinalLocals(md, initCode);
            TreeInfo.mapSuperCalls(md.body, supercall -> make.Block(0, initCodeWithLocals.append(supercall)));

            if (md.body.endpos == Position.NOPOS)
                md.body.endpos = TreeInfo.endPos(md.body.stats.last());

            md.sym.appendUniqueTypeAttributes(initTAs);
        }
    }

    void rewriteInitializersIfNeeded(JCMethodDecl md, List<JCStatement> initCode) {
        if (lower.initializerOuterThis.containsKey(md.sym.owner)) {
            Gen.InitializerVisitor initializerVisitor = new Gen.InitializerVisitor(md, lower.initializerOuterThis.get(md.sym.owner));
            for (JCStatement init : initCode) {
                initializerVisitor.scan(init);
            }
        }
    }

    List<JCStatement> createFinalLocals(JCMethodDecl md, List<JCStatement> initCode) {
        ListBuffer<JCStatement> resultLB = new ListBuffer<>();
        Map<Symbol, Symbol> fieldToLocalMap = new LinkedHashMap<>();

        for (JCStatement originalInit : initCode) {
            JCTree.JCAssign originalFieldDecl = (JCTree.JCAssign) ((JCTree.JCExpressionStatement) originalInit).expr;
            long flags = FINAL | SYNTHETIC;
            Name localName = newLocalName(TreeInfo.name(originalFieldDecl.lhs));
            Symbol originalVarSym = TreeInfo.symbol(originalFieldDecl.lhs);
            VarSymbol proxy = new VarSymbol(flags, localName, originalVarSym.erasure(types), md.sym);
            fieldToLocalMap.put(originalVarSym, proxy);
            JCVariableDecl localDecl = make.at(md.pos).VarDef(proxy, originalFieldDecl.rhs);
            localDecl.vartype = fieldSymInDeclaration.get(originalVarSym).vartype;
            resultLB = resultLB.append(localDecl);
        }
        FieldRewriter fr = new FieldRewriter(md, fieldToLocalMap, make);

        ListBuffer<JCStatement> tmpResult = new ListBuffer<>();
        for (JCStatement st : resultLB) {
            tmpResult = tmpResult.append(fr.translate(st));
        }
        resultLB = tmpResult;
        for (Symbol vsym : fieldToLocalMap.keySet()) {
            Symbol local = fieldToLocalMap.get(vsym);
            resultLB = resultLB.append(make.at(md.pos()).Assignment(vsym, make.at(md.pos()).Ident(local)));
        }
        return resultLB.toList();
    }

    Name newLocalName(Name name) {
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
    }
}
