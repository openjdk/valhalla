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
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.SymbolMetadata;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

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

    @SuppressWarnings("this-escape")
    protected NullChecksWriter(Context context) {
        context.put(nullChecksWriterKey, this);
        make = TreeMaker.instance(context);
        types = Types.instance(context);
        attr = Attr.instance(context);
        syms = Symtab.instance(context);
    }

    public JCTree translateTopLevelClass(JCTree cdef, TreeMaker make) {
        try {
            this.make = make;
            return translate(cdef);
        } finally {
            // note that recursive invocations of this method fail hard
            this.make = null;
        }
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
        // temporary hack, this is only to test null restriction in the VM for fields of value classes
        if (tree.sym.type.isValueClass() && types.isNonNullable(tree.sym.type)) {
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
        super.visitAssign(tree);
        Symbol lhsSym = TreeInfo.symbolFor(tree.lhs);
        if (lhsSym != null &&
                types.isNonNullable(lhsSym.type)) {
            tree.rhs = attr.makeNullCheck(tree.rhs, true);
        }
        result = tree;
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
}
