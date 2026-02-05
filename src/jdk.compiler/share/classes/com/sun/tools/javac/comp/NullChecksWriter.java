/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Preview;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Options;

import java.util.Locale;

import static com.sun.tools.javac.code.Kinds.Kind.TYP;
import static com.sun.tools.javac.code.Kinds.Kind.VAR;
import static com.sun.tools.javac.code.TypeTag.VOID;

/** This pass generates null checks for the compiler to check for assertions on
 *  null restricted types.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class NullChecksWriter extends TreeTranslator {

    protected static final Context.Key<NullChecksWriter> nullChecksWriterKey = new Context.Key<>();

    public static NullChecksWriter instance(Context context) {
        NullChecksWriter instance = context.get(nullChecksWriterKey);
        if (instance == null)
            instance = new NullChecksWriter(context);
        return instance;
    }

    private final Types types;
    private TreeMaker make;
    private final Attr attr;
    private final Symtab syms;
    /** are null restricted types allowed?
      */
    private final boolean allowNullRestrictedTypes;
    private final UseSiteNullChecks useSiteNullChecks;
    private boolean checkNulls;

    @SuppressWarnings("this-escape")
    protected NullChecksWriter(Context context) {
        context.put(nullChecksWriterKey, this);
        make = TreeMaker.instance(context);
        types = Types.instance(context);
        attr = Attr.instance(context);
        syms = Symtab.instance(context);
        Preview preview = Preview.instance(context);
        Source source = Source.instance(context);
        allowNullRestrictedTypes = (!preview.isPreview(Source.Feature.NULL_RESTRICTED_TYPES) || preview.isEnabled()) &&
                Source.Feature.NULL_RESTRICTED_TYPES.allowedInSource(source);
        useSiteNullChecks = UseSiteNullChecks.of(Options.instance(context).get("useSiteNullChecks"));
    }

    private enum UseSiteNullChecks {
        NONE(false, false),
        METHODS(true, false),
        METHODS_AND_FIELDS(true, true, "methods+fields");

        final boolean generateChecksForMethods;
        final boolean generateChecksForFields;
        final String compilerOpt;

        UseSiteNullChecks(boolean generateChecksForMethods, boolean generateChecksForFields) {
            this.generateChecksForMethods = generateChecksForMethods;
            this.generateChecksForFields = generateChecksForFields;
            this.compilerOpt = name().toLowerCase(Locale.ROOT);
        }

        UseSiteNullChecks(boolean generateChecksForMethods, boolean generateChecksForFields, String compilerOpt) {
            this.generateChecksForMethods = generateChecksForMethods;
            this.generateChecksForFields = generateChecksForFields;
            this.compilerOpt = compilerOpt;
        }

        static UseSiteNullChecks of(String compilerOpt) {
            if (compilerOpt == null) {
                return METHODS_AND_FIELDS;
            }
            for (UseSiteNullChecks useSiteNullChecks: UseSiteNullChecks.values()) {
                if (useSiteNullChecks.compilerOpt.equals(compilerOpt)) {
                    return useSiteNullChecks;
                }
            }
            Assert.error("Unknown useSiteNullChecks: " + compilerOpt);
            throw new IllegalStateException("Unknown useSiteNullChecks: " + compilerOpt);
        }
    }

    Env<AttrContext> env;

    public JCTree translateTopLevelClass(Env<AttrContext> env, JCTree cdef, TreeMaker make) {
        if (allowNullRestrictedTypes) {
            try {
                this.make = make;
                this.env = env;
                return translate(cdef);
            } finally {
                // note that recursive invocations of this method fail hard
                this.make = null;
                this.env = null;
            }
        }
        return cdef;
    }

    /* ************************************************************************
     * Visitor methods
     *************************************************************************/

    @Override
    public void visitVarDef(JCVariableDecl tree) {
        super.visitVarDef(tree);
        if (tree.init != null) {
            if (types.isNonNullable(tree.sym.type)) {
                tree.init = attr.makeNullCheck(tree.init, true);
            }
        }
        // temporary hack, this is only to test null restriction in the VM for fields of a value class type
        if (tree.sym.owner.kind == TYP &&
                tree.sym.type.isValueClass() &&
                types.isNonNullable(tree.sym.type)) {
            List<Attribute.Compound> rawAttrs = tree.sym.getRawAttributes();
            if (rawAttrs.isEmpty() || !rawAttrs.stream().anyMatch(ac -> ac.type.tsym == syms.nullRestrictedType.tsym)) {
                Attribute.Compound ac = new Attribute.Compound(syms.nullRestrictedType, List.nil());
                tree.sym.appendAttributes(List.of(ac));
            }
        }
        result = tree;
    }

    @Override
    public void visitAssign(JCAssign tree) {
        // could be null for indexed array accesses, we should deal with those later
        tree.lhs = translate(tree.lhs, false);
        tree.rhs = translate(tree.rhs);
        Symbol lhsSym = TreeInfo.symbolFor(tree.lhs);
        if (lhsSym != null &&
                types.isNonNullable(lhsSym.type)) {
            tree.rhs = attr.makeNullCheck(tree.rhs, true);
        }
        result = tree;
    }

    @Override
    public void visitIdent(JCIdent tree) {
        super.visitIdent(tree);
        identSelectVisitHelper(tree);
    }

    @Override
    public void visitSelect(JCFieldAccess tree) {
        super.visitSelect(tree);
        identSelectVisitHelper(tree);
    }

    // where
        private void identSelectVisitHelper(JCTree tree) {
            if (needsUseSiteNullCheck(tree) &&
                checkNulls) {
                /* we are accessing a non-nullable field declared in another
                 * compilation unit
                 */
                result = attr.makeNullCheck((JCExpression) tree, true);
            }
        }

    public void visitTypeCast(JCTypeCast tree) {
        super.visitTypeCast(tree);
        if (tree.strict) {
            tree.expr = attr.makeNullCheck(tree.expr, true);
        }
        result = tree;
    }

    Type returnType = null;

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        Type prevRetType = returnType;
        try {
            if (tree.body != null) {
                returnType = tree.sym.type.getReturnType();
                ListBuffer<JCStatement> paramNullChecks = new ListBuffer<>();
                for (JCVariableDecl param : tree.params) {
                    if (types.isNonNullable(param.sym.type)) {
                        paramNullChecks.add(make.at(tree.body.pos())
                                .Exec(attr.makeNullCheck(make.at(tree.body.pos()).Ident(param), true)));
                    }
                }
                if (!paramNullChecks.isEmpty()) {
                    tree.body.stats = tree.body.stats.prependList(paramNullChecks.toList());
                }
            }
            super.visitMethodDef(tree);
            result = tree;
        } finally {
            returnType = prevRetType;
        }
    }

    @Override
    public void visitReturn(JCReturn tree) {
        super.visitReturn(tree);
        if (tree.expr != null && returnType != null && !returnType.hasTag(VOID)) {
            if (types.isNonNullable(returnType)) {
                tree.expr = attr.makeNullCheck(tree.expr, true);
            }
        }
        result = tree;
    }

    @Override
    public void visitApply(JCMethodInvocation tree) {
        MethodSymbol msym = (MethodSymbol) TreeInfo.symbolFor(tree.meth);
        boolean canBeOverriden = (msym.flags_field & (Flags.PRIVATE | Flags.STATIC | Flags.FINAL)) == 0 &&
                !msym.owner.isFinal();
        if (useSiteNullChecks.generateChecksForMethods &&
                hasNonNullArgs(msym) &&
                canBeOverriden) {
            tree.args = newArgs(msym, tree.args);
        }
        super.visitApply(tree);
        result = tree;
        if (useSiteNullChecks.generateChecksForMethods &&
                types.isNonNullable(msym.type.asMethodType().restype) &&
                canBeOverriden) {
            result = attr.makeNullCheck(tree, true);
        }
    }

    @Override
    public void visitNewClass(JCNewClass tree) {
        if (useSiteNullChecks.generateChecksForMethods &&
                hasNonNullArgs((MethodSymbol) tree.constructor) &&
                !isInThisSameCompUnit(tree.constructor)) {
            tree.args = newArgs((MethodSymbol) tree.constructor, tree.args);
        }
        super.visitNewClass(tree);
        result = tree;
    }

    private boolean hasNonNullArgs(MethodSymbol msym) {
        return msym.type.asMethodType().argtypes.stream().anyMatch(argType -> types.isNonNullable(argType));
    }

    private List<JCExpression> newArgs(MethodSymbol msym, List<JCExpression> actualArgs) {
        // skip signature polymorphic methods they won't have null restricted arguments
        if ((msym.flags_field & Flags.SIGNATURE_POLYMORPHIC) != 0) {
            return actualArgs;
        }
        ListBuffer<JCExpression> newArgs = new ListBuffer<>();
        List<Type> declaredArgTypes = msym.type.asMethodType().argtypes;
        /* there can be prefix arguments added by Lower, for example captured variables, etc
         * nothing to check for them
         */
        int declaredArgSize = declaredArgTypes.size();
        int prefixArgsLength = msym.externalType(types).getParameterTypes().size() - declaredArgSize;
        List<JCExpression> actualArgsTmp = actualArgs;
        while (prefixArgsLength-- > 0) {
            newArgs.add(actualArgsTmp.head);
            actualArgsTmp = actualArgsTmp.tail;
        }
        int noOfArgsToCheck = msym.isVarArgs() ? declaredArgSize - 1 : declaredArgSize;
        while (noOfArgsToCheck-- > 0) {
            Type formalArgType = declaredArgTypes.head;
            if (types.isNonNullable(formalArgType)) {
                newArgs.add(attr.makeNullCheck(actualArgsTmp.head, true));
            } else {
                newArgs.add(actualArgsTmp.head);
            }
            actualArgsTmp = actualArgsTmp.tail;
        }
        /* now add the last vararg argument if applicable, no checks are needed here as varargs can't be
         * null restricted. Also note that at this point the vararg arguments have already been translated
         * by Lower into an array. This is not true for signature polymorphic methods, but we have already
         * filtered them out
         */
        if (msym.isVarArgs()) {
            newArgs.add(actualArgsTmp.head);
        }
        return newArgs.toList();
    }

    @Override
    public <T extends JCTree> T translate(T tree) {
        return translate(tree, true);
    }

    public <T extends JCTree> T translate(T tree, boolean checkNulls) {
        boolean prevCheckNulls = this.checkNulls;
        try {
            this.checkNulls = checkNulls;
            return super.translate(tree);
        } finally {
            this.checkNulls = prevCheckNulls;
        }
    }

    private boolean needsUseSiteNullCheck(JCTree tree) {
        return needsUseSiteNullCheck(tree, env);
    }

    public boolean needsUseSiteNullCheck(JCTree tree, Env<AttrContext> env) {
        Symbol sym = TreeInfo.symbolFor(tree);
        return sym != null &&
                useSiteNullChecks.generateChecksForFields &&
                sym.owner.kind == TYP &&
                sym.kind == VAR &&
                types.isNonNullable(sym.type) &&
                !isInThisSameCompUnit(sym, env);
    }

    // where
        private boolean isInThisSameCompUnit(Symbol sym) {
            return isInThisSameCompUnit(sym, env);
        }

        private boolean isInThisSameCompUnit(Symbol sym, Env<AttrContext> env) {
            return env.toplevel.getTypeDecls().stream()
                    .anyMatch(tree -> TreeInfo.symbolFor(tree) == sym.outermostClass());
        }
}
