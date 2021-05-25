/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.jvm;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Scope.LookupKind;
import com.sun.tools.javac.code.Scope.WriteableScope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import java.util.HashMap;
import java.util.Map;

import static com.sun.tools.javac.code.Flags.STATIC;
import static com.sun.tools.javac.code.Kinds.Kind.MTH;
import static com.sun.tools.javac.code.Kinds.Kind.TYP;
import static com.sun.tools.javac.code.Kinds.Kind.VAR;
import static com.sun.tools.javac.tree.JCTree.Tag.APPLY;
import static com.sun.tools.javac.tree.JCTree.Tag.EXEC;
import static com.sun.tools.javac.tree.JCTree.Tag.IDENT;

/**
 * This pass translates value constructors into static factory methods and patches up constructor
 * calls to become invocations of those static factory methods.
 *
 * We get commissioned as a subpass of Gen. Constructor trees undergo plenty of change in Lower
 * (enclosing instance injection, captured locals ...) and in Gen (instance field initialization,
 * see normalizeDefs) and so it is most effective to wait until things reach a quiescent state
 * before undertaking the tinkering that we do.
 *
 * See https://bugs.openjdk.java.net/browse/JDK-8198749 for the kind of transformations we do.
 *
 */
public class TransValues extends TreeTranslator {

    protected static final Context.Key<TransValues> transValuesKey = new Context.Key<>();

    private Symtab syms;
    private TreeMaker make;
    private Types types;
    private Names names;

    /* Is an assignment undergoing translation just an assignment statement ?
       Or is also a value ??
    */
    private boolean requireRVal;

    // class currently undergoing translation.
    private JCClassDecl currentClass;

    // method currently undergoing translation.
    private JCMethodDecl currentMethod;

    // list of factories synthesized so far.
    private List<JCTree> staticFactories;

    // Map from constructor symbols to factory symbols.
    private Map<MethodSymbol, MethodSymbol> init2factory = new HashMap<>();

    public static TransValues instance(Context context) {
        TransValues instance = context.get(transValuesKey);
        if (instance == null)
            instance = new TransValues(context);
        return instance;
    }

    protected TransValues(Context context) {
        context.put(transValuesKey, this);
        syms = Symtab.instance(context);
        make = TreeMaker.instance(context);
        types = Types.instance(context);
        names = Names.instance(context);
    }

    @SuppressWarnings("unchecked")
    public <T extends JCTree> T translate(T tree, boolean requireRVal) {
        boolean priorRequireRVal = this.requireRVal;
        try {
            this.requireRVal = requireRVal;
            if (tree == null) {
                return null;
            } else {
                tree.accept(this);
                JCTree tmpResult = this.result;
                this.result = null;
                return (T)tmpResult; // XXX cast
            }
        } finally {
             this.requireRVal = priorRequireRVal;
        }
    }

    @Override
    public <T extends JCTree> T translate(T tree) {
        return translate(tree, true);
    }

    public JCClassDecl translateTopLevelClass(JCClassDecl classDecl, TreeMaker make) {
        try {
            this.make = make;
            translate(classDecl);
        } finally {
            // note that recursive invocations of this method fail hard
            this.make = null;
        }
        init2factory = new HashMap<>();
        return classDecl;
    }

    @Override
    public void visitClassDef(JCClassDecl classDecl) {
        JCClassDecl previousClass = currentClass;
        List<JCTree> previousFactories = staticFactories;
        staticFactories = List.nil();
        currentClass = classDecl;
        try {
            super.visitClassDef(classDecl);
            classDecl.defs = classDecl.defs.appendList(staticFactories);
            staticFactories = List.nil();
        }
        finally {
            currentClass = previousClass;
            staticFactories = previousFactories;
        }
    }

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        JCMethodDecl previousMethod = currentMethod;
        currentMethod = tree;
        try {
            if (constructingValue()) {

                // Mutate this value constructor into an equivalent static value factory
                make.at(tree.pos());
                JCExpressionStatement exec = chainedConstructorCall(tree);
                Assert.check(exec != null && TreeInfo.isSelfCall(exec));
                JCMethodInvocation call = (JCMethodInvocation) exec.expr;

                /* Unlike the reference construction sequence where `this' is allocated ahead of time and
                   is passed as an argument into the <init> method, a value factory must allocate the value
                   instance that forms the `product' by itself. We do that by injecting a prologue here.
                */
                VarSymbol product = currentMethod.factoryProduct = new VarSymbol(0, names.dollarValue, currentClass.sym.type, currentMethod.sym); // TODO: owner needs rewiring
                JCExpression rhs;

                final Name name = TreeInfo.name(call.meth);
                MethodSymbol symbol = (MethodSymbol)TreeInfo.symbol(call.meth);
                if (names._super.equals(name)) { // "initial" constructor.
                    // Synthesize code to allocate factory "product" via: V $this = V.default;
                    Assert.check(symbol.type.getParameterTypes().size() == 0);
                    final JCExpression type = make.Type(currentClass.type);
                    rhs = make.DefaultValue(type);
                    rhs.type = currentClass.type;
                } else {
                    // This must be a chained call of form `this(args)'; Mutate it into a factory invocation i.e V $this = V.init(args);
                    Assert.check(TreeInfo.name(TreeInfo.firstConstructorCall(tree).meth) == names._this);
                    MethodSymbol factory = getValueFactory(symbol);
                    final JCIdent ident = make.Ident(factory);
                    rhs = make.App(ident, call.args);
                    ((JCMethodInvocation)rhs).varargsElement = call.varargsElement;
                }

                /* The value product allocation prologue must precede any synthetic inits !!!
                   as these may reference `this' which gets pre-allocated for references but
                   not for values.
                */
                JCStatement prologue = make.VarDef(product, rhs);
                tree.body.stats = tree.body.stats.prepend(prologue).diff(List.of(exec));
                tree.body = translate(tree.body);

                MethodSymbol factorySym = getValueFactory(tree.sym);
                currentMethod.setType(factorySym.type);
                currentMethod.factoryProduct = product;
                currentClass.sym.members().remove(tree.sym);
                tree.sym = factorySym;
                currentClass.sym.members().enter(factorySym);
                tree.mods.flags |= STATIC;

                /* We may need an epilogue that returns the value product, but we can't eagerly insert
                   a return here, since we don't know much about control flow here. Gen#genMethod
                   will insert a return of the factory product if control does reach the end and would
                   "fall off the cliff" otherwise.
                */

                result = tree;
                return;
            }
            super.visitMethodDef(tree);
        } finally {
            currentMethod = previousMethod;
        }
    }

    @Override
    public void visitReturn(JCReturn tree) {
        if (constructingValue()) {
            result = make.Return(make.Ident(currentMethod.factoryProduct));
        } else {
            super.visitReturn(tree);
        }
    }

    /* Note: 1. Assignop does not call for any translation, since value instance fields are final and
       so cannot be AssignedOped. 2. Any redundantly qualified this would have been lowered already.
    */
    @Override
    public void visitAssign(JCAssign tree) {
        if (constructingValue()) {
            Symbol symbol = null;
            switch(tree.lhs.getTag()) {
                case IDENT:
                    symbol = ((JCIdent)tree.lhs).sym;
                    break;
                case SELECT:
                    JCFieldAccess fieldAccess = (JCFieldAccess) tree.lhs;
                    if (fieldAccess.selected.hasTag(IDENT) && ((JCIdent)fieldAccess.selected).name == names._this) {
                        symbol = fieldAccess.sym;
                    }
                    break;
                default:
                    break;
            }
            if (isInstanceMemberAccess(symbol)) {
                final JCIdent facHandle = make.Ident(currentMethod.factoryProduct);
                result = make.Assign(facHandle, make.WithField(make.Select(facHandle, symbol), translate(tree.rhs)).setType(currentClass.type)).setType(currentClass.type);
                if (requireRVal) {
                    result = make.Select(make.Parens((JCExpression) result).setType(currentClass.type), symbol);
                }
                return;
            }
        }
        super.visitAssign(tree);
    }

    @Override
    public void visitExec(JCExpressionStatement tree) {
        if (constructingValue()) {
            tree.expr = translate(tree.expr, false);
            result = tree;
        } else {
            super.visitExec(tree);
        }
    }

    @Override
    public void visitIdent(JCIdent ident) {
        if (constructingValue()) {
            Symbol symbol = ident.sym;
            if (isInstanceMemberAccess(symbol)) {
                final JCIdent facHandle = make.Ident(currentMethod.factoryProduct);
                result = make.Select(facHandle, symbol);
                return;
            } else if (symbol.name == names._this) {
                result = make.Ident(currentMethod.factoryProduct);
                return;
            }
        }
        super.visitIdent(ident);
    }

    @Override
    public void visitSelect(JCFieldAccess fieldAccess) {
        if (constructingValue()) { // Qualified this would have been lowered already.
            if (fieldAccess.selected.hasTag(IDENT) && ((JCIdent)fieldAccess.selected).name == names._this) {
                Symbol symbol = fieldAccess.sym;
                if (isInstanceMemberAccess(symbol)) {
                    final JCIdent facHandle = make.Ident(currentMethod.factoryProduct);
                    result = make.Select(facHandle, symbol);
                    return;
                }
            }
        }
        /* Rewrite any accesses of the form V.ref.member to ((V) V.ref).member OR
           if a static member is being selected via a V.ref as a TYP, rewrite
           V.ref.member to V.member
        */
        fieldAccess.selected = translate(fieldAccess.selected);
        if (fieldAccess.name != names._class) {  // TODO: this and super ??
            Symbol sym = TreeInfo.symbol(fieldAccess);
            Symbol sitesym = TreeInfo.symbol(fieldAccess.selected);
            Type selectedType = fieldAccess.selected.type;
            if (selectedType.isReferenceProjection()) {
                switch (sym.kind) {
                    case MTH:
                    case VAR:
                        if (sym.isStatic() && sitesym != null && sitesym.kind == TYP) {
                            fieldAccess.selected = make.Type(types.erasure(selectedType.valueProjection()));
                        } else {
                            if (types.splitPrimitiveClass) {
                                fieldAccess.selected =
                                        make.TypeCast(types.erasure(selectedType.valueProjection()), fieldAccess.selected);
                            } // else members of Foo can be accessed via LFoo or QFoo at the VM level without a cast.
                        }
                        break;
                    case TYP:
                        fieldAccess.selected = make.Type(types.erasure(selectedType.valueProjection()));
                        break;
                }
            }
        }
        result = fieldAccess;
    }

    // Translate a reference style instance creation attempt on a value type to a static factory call.
    @Override
    public void visitNewClass(JCNewClass tree) {
        if (types.isPrimitiveClass(tree.clazz.type)) {
            // Enclosing instances or anonymous classes should have been eliminated by now.
            Assert.check(tree.encl == null && tree.def == null);
            tree.args = translate(tree.args);
            Assert.check(tree.def == null);
            MethodSymbol sFactory = getValueFactory((MethodSymbol) tree.constructor);
            make.at(tree.pos());
            JCExpression declClass = make.Type(tree.constructor.owner.type);
            JCExpression meth = make.Select(declClass, sFactory);
            meth.type = types.erasure(meth.type);
            final JCMethodInvocation apply = make.Apply(tree.typeargs, meth, tree.args);
            apply.varargsElement = tree.varargsElement;
            apply.type = meth.type.getReturnType();
            result = apply;
            return;
        }
        super.visitNewClass(tree);
    }

    // Utility methods ...
    private boolean constructingValue() {
        return currentClass != null && (currentClass.sym.flags() & Flags.PRIMITIVE_CLASS) != 0 && currentMethod != null && currentMethod.sym.isConstructor();
    }

    private boolean isInstanceMemberAccess(Symbol symbol) {
        return symbol != null
                && (symbol.name != names._this && symbol.name != names._super)
                && (symbol.kind == VAR || symbol.kind == MTH)
                && symbol.owner == currentClass.sym && !symbol.isStatic();
    }

    private MethodSymbol getValueFactory(MethodSymbol init) {
        Assert.check(init.name.equals(names.init));
        Assert.check(types.isPrimitiveClass(init.owner.type));
        MethodSymbol factory = init2factory.get(init);
        if (factory != null)
            return factory;

        MethodType factoryType = new MethodType(init.erasure(types).getParameterTypes(),
                                                init.owner.type,
                                                init.type.getThrownTypes(),
                                                init.owner.type.tsym);
        factory = new MethodSymbol(init.flags_field | STATIC,
                                        names.init,
                                        factoryType,
                                        init.owner);
        factory.setAttributes(init);
        init2factory.put(init, factory);
        return factory;
    }

    /** Return the *statement* in the constructor that `chains' to another constructor call either
     *  in the same class or its superclass. One MUST exist except for jlO, though may be buried
     *  under synthetic initializations.
     */
    private JCExpressionStatement chainedConstructorCall(JCMethodDecl md) {
        if (md.name == names.init && md.body != null) {
            for (JCStatement statement : md.body.stats) {
                if (statement.hasTag(EXEC)) {
                    JCExpressionStatement exec = (JCExpressionStatement)statement;
                    if (exec.expr.hasTag(APPLY)) {
                        JCMethodInvocation apply = (JCMethodInvocation)exec.expr;
                        Name name = TreeInfo.name(apply.meth);
                        if (name == names._super || name == names._this)
                            return exec;
                    }
                }
            }
        }
        return null;
    }

    private MethodSymbol getDefaultConstructor(Symbol klass) {
        for (Symbol method : klass.members().getSymbolsByName(names.init, s->s.kind == MTH && s.type.getParameterTypes().size() == 0, LookupKind.NON_RECURSIVE)) {
            return (MethodSymbol) method;
        }
        // class defines a non-nullary but no nullary constructor, fabricate a symbol.
        MethodType dctorType = new MethodType(List.nil(),
                klass.type,
                List.nil(),
                klass.type.tsym);
        return new MethodSymbol(Flags.PUBLIC,
                names.init,
                dctorType,
                klass);
    }
}
