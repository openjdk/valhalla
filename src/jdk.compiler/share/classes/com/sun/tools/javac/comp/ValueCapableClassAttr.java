/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.javac.code.*;

import static com.sun.tools.javac.code.Flags.asFlagSet;
import static com.sun.tools.javac.code.Kinds.Kind.*;
import com.sun.tools.javac.code.Lint.LintCategory;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.OperatorSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.jvm.ByteCodes;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.*;

/**
 * Support for "Minimal Value Types" : Process classes annotated with @ValueCapableClass -
 * All semantic checks are centralized in this place so that we can blow them all away when
 * moving to "Maximal Value Types".
 *
 * see: http://cr.openjdk.java.net/~jrose/values/shady-values.html
 *
 */

public class ValueCapableClassAttr extends TreeTranslator {

    protected static final Context.Key<ValueCapableClassAttr> valueCapableClassAttr = new Context.Key<>();
    private JCMethodDecl currentMethod;
    private Log log;
    private Names names;
    private Symtab syms;
    private final JCDiagnostic.Factory diags;
    private final Types types;
    private final Check chk;
    private boolean inValue = false;

    public static ValueCapableClassAttr instance(Context context) {
        ValueCapableClassAttr instance = context.get(valueCapableClassAttr);
        if (instance == null)
            instance = new ValueCapableClassAttr(context);
        return instance;
    }

    protected ValueCapableClassAttr(Context context) {
        context.put(valueCapableClassAttr, this);
        log = Log.instance(context);
        names = Names.instance(context);
        syms = Symtab.instance(context);
        diags = JCDiagnostic.Factory.instance(context);
        types = Types.instance(context);
        chk = Check.instance(context);
    }

    public void visitClassDef(JCClassDecl tree) {
        boolean oldInValue = inValue;
        try {
            inValue = false;
            for (List<JCAnnotation> al = tree.mods.annotations; !al.isEmpty(); al = al.tail) {
                JCAnnotation a = al.head;
                if (a.annotationType.type == syms.valueCapableClass && a.args.isEmpty()) {
                    inValue = true;
                    tree.sym.flags_field |= Flags.VALUE_CAPABLE;
                    break;
                }
            }
            if (inValue) {
                if (tree.extending != null) {
                    log.warning(LintCategory.VALUES, tree.pos(), "value.may.not.extend");
                }
                if ((tree.mods.flags & Flags.FINAL) == 0) {
                    log.warning(LintCategory.VALUES, tree.pos(), "value.must.be.final");
                }
                chk.checkNonCyclicMembership(tree);
            }
            super.visitClassDef(tree);
        } finally {
            inValue = oldInValue;
        }
    }

    public void visitMethodDef(JCMethodDecl tree) {
        JCMethodDecl previousMethod = currentMethod;
        try {
            currentMethod = tree;
            if (tree.sym != null && tree.sym.owner.isValueCapable()) {
                if ((tree.sym.flags() & (Flags.SYNCHRONIZED | Flags.STATIC)) == Flags.SYNCHRONIZED) {
                    log.warning(LintCategory.VALUES, tree.pos(), "mod.not.allowed.here", asFlagSet(Flags.SYNCHRONIZED));
                }
                if (tree.sym.attribute(syms.overrideType.tsym) != null) {
                    MethodSymbol m = tree.sym;
                    TypeSymbol owner = (TypeSymbol)m.owner;
                    for (Type sup : types.closure(owner.type)) {
                        if (sup == owner.type)
                            continue; // skip "this"
                        Scope scope = sup.tsym.members();
                        for (Symbol sym : scope.getSymbolsByName(m.name)) {
                            if (!sym.isStatic() && m.overrides(sym, owner, types, true)) {
                                switch (sym.name.toString()) {
                                    case "hashCode":
                                    case "equals":
                                    case "toString":
                                        break;
                                    default:
                                        log.warning(LintCategory.VALUES, tree.pos(), "value.does.not.support", "overriding java.lang.Object's method: " + sym.name.toString());
                                        break;
                                }
                            }
                        }
                    }
                }
            }
            super.visitMethodDef(tree);
        } finally {
            currentMethod = previousMethod;
        }

    }

    public void visitVarDef(JCVariableDecl tree) {
        if (tree.sym != null && tree.sym.owner.kind == TYP && tree.sym.owner.isValueCapable()) {
            if ((tree.mods.flags & (Flags.FINAL | Flags.STATIC)) == 0) {
                log.warning(LintCategory.VALUES, tree.pos(), "value.field.must.be.final");
            }
        }
        if (tree.init != null && tree.init.type != null && tree.init.type.hasTag(TypeTag.BOT)) {
            if (types.isValueCapable(tree.vartype.type))
                log.warning(LintCategory.VALUES, tree.init.pos(), "prob.found.req", diags.fragment("inconvertible.types", syms.botType, tree.vartype.type));
        }
        super.visitVarDef(tree);
    }

    @Override
    public void visitAssign(JCAssign tree) {
        if (tree.rhs.type != null && tree.rhs.type.hasTag(TypeTag.BOT)) {
            Type lType = tree.lhs.type;
            if (lType != null && types.isValueCapable(lType)) {
                log.warning(LintCategory.VALUES, tree.rhs.pos(), "prob.found.req", diags.fragment("inconvertible.types", syms.botType, lType));
            }
        }
        super.visitAssign(tree);
    }

    @Override
    public void visitReturn(JCReturn tree) {
        if (currentMethod != null && tree.expr != null && tree.expr.type != null && tree.expr.type.hasTag(TypeTag.BOT)) {
            if (currentMethod.restype != null && types.isValueCapable(currentMethod.restype.type)) {
                log.warning(LintCategory.VALUES, tree.expr.pos(), "prob.found.req", diags.fragment("inconvertible.types", syms.botType, currentMethod.restype.type));
            }
        }
        super.visitReturn(tree);
    }

    @Override
    public void visitTypeTest(JCInstanceOf tree) {
        if (tree.expr.type.hasTag(TypeTag.BOT)) {
            if (types.isValueCapable(tree.clazz.type)) {
                log.warning(LintCategory.VALUES, tree.expr.pos(), "prob.found.req", diags.fragment("inconvertible.types", syms.botType, tree.clazz.type));
            }
        }
        super.visitTypeTest(tree);
    }

    @Override
    public void visitTypeCast(JCTypeCast tree) {
        if (tree.expr.type != null && tree.expr.type.hasTag(TypeTag.BOT) &&
                tree.clazz.type != null && types.isValueCapable(tree.clazz.type)) {
            log.warning(LintCategory.VALUES, tree.expr.pos(), "prob.found.req", diags.fragment("inconvertible.types", syms.botType, tree.clazz.type));
        }
        super.visitTypeCast(tree);
    }

    public void visitBinary(JCBinary tree) {
        Type left = tree.lhs.type;
        Type right = tree.rhs.type;
        Symbol operator = tree.operator;

        if (operator != null && operator.kind == MTH &&
                left != null && !left.isErroneous() &&
                right != null && !right.isErroneous()) {
            int opc = ((OperatorSymbol)operator).opcode;
            if ((opc == ByteCodes.if_acmpeq || opc == ByteCodes.if_acmpne)) {
                if ((left.hasTag(TypeTag.BOT) && types.isValueCapable(right)) ||
                        (right.hasTag(TypeTag.BOT) && types.isValueCapable(left))) {
                    log.warning(LintCategory.VALUES, tree.pos(), "incomparable.types", left, right);
                }
            }
            // this is likely to change.
            if (operator.name.contentEquals("==") || operator.name.contentEquals("!=")) {
                if (types.isValueCapable(tree.lhs.type) ||
                        types.isValueCapable(tree.rhs.type))
                    log.warning(LintCategory.VALUES, tree.pos(), "value.does.not.support", tree.operator.name.toString());
            }
        }
        super.visitBinary(tree);
    }

    @Override
    public void visitSynchronized(JCSynchronized tree) {
        if (types.isValueCapable(tree.lock.type)) {
            log.warning(LintCategory.VALUES, tree.pos(), "type.found.req", tree.lock.type, diags.fragment("type.req.ref"));
        }
        super.visitSynchronized(tree);
    }

    public void visitApply(JCMethodInvocation tree) {
        final Symbol method = TreeInfo.symbolFor(tree);
        if (method != null && method.kind != ERR) {
            if (method.name.contentEquals("identityHashCode") && method.owner.type == syms.systemType) {
                if ((tree.args.length() == 1) && types.isValueCapable(tree.args.head.type))
                    log.warning(LintCategory.VALUES, tree.pos(), "value.does.not.support", "identityHashCode");
            }

            if (method.name != names.init && method.name != names.getClass && method.owner.type == syms.objectType) {
                boolean receiverIsValue = false;
                switch (tree.meth.getTag()) {
                    case IDENT:
                        receiverIsValue = inValue;
                        break;
                    case SELECT:
                        final Symbol symbol = TreeInfo.symbol(((JCFieldAccess)tree.meth).selected);
                        receiverIsValue = symbol != null &&
                                (symbol.name == names._super ? inValue : types.isValueCapable(symbol.type));
                        break;
                }
                if (receiverIsValue) {
                    log.warning(LintCategory.VALUES, tree.pos(), "value.does.not.support", "calling java.lang.Object's method: " + method.name.toString());
                }
            }
            final List<Type> parameterTypes = method.type.getParameterTypes();
            for (int i = 0; i < tree.args.size(); i++) {
                final JCExpression arg = tree.args.get(i);
                if (arg.type != null && arg.type.hasTag(TypeTag.BOT)) {
                    Type param = i < parameterTypes.size() ? parameterTypes.get(i) :
                            types.elemtype(parameterTypes.get(parameterTypes.size() - 1));
                    if (types.isValueCapable(param))
                        log.warning(LintCategory.VALUES, arg.pos(), "prob.found.req", diags.fragment("inconvertible.types", syms.botType, param));
                }
            }
        }
        super.visitApply(tree);
    }
}
