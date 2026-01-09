/*
 * Copyright (c) 1999, 2025, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;

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
    private final Names names;
    private final Symtab syms;
    private TreeMaker make;
    private final Attr attr;

    @SuppressWarnings("this-escape")
    protected NullChecksWriter(Context context) {
        context.put(nullChecksWriterKey, this);
        make = TreeMaker.instance(context);
        types = Types.instance(context);
        names = Names.instance(context);
        syms = Symtab.instance(context);
        attr = Attr.instance(context);
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

    JCExpression generateNullCheckIfNeeded(JCExpression tree, JCNullableTypeExpression.NullMarker expectedNullness) {
        if (expectedNullness == JCNullableTypeExpression.NullMarker.NOT_NULL && !types.isNonNullable(tree.type)) {
            return attr.makeNullCheck(tree, true);
        }
        return tree;
    }

    /* ************************************************************************
     * Visitor methods
     *************************************************************************/

    /** Visitor argument: proto-type.
     */

    @Override
    public void visitVarDef(JCVariableDecl tree) {
        if (tree.init != null) {
            tree.init = translate(tree.init);
            tree.init = generateNullCheckIfNeeded(tree.init, tree.sym.type.getNullMarker());
        }
        result = tree;
    }

    @Override
    public void visitAssign(JCAssign tree) {
        // could be null for indexed array accesses, we should deal with those later
        Symbol lhsSym = TreeInfo.symbolFor(tree.lhs);
        Symbol rhsSym = TreeInfo.symbolFor(tree.rhs);
        if (lhsSym != null &&
                rhsSym != null) {
            tree.rhs = translate(tree.rhs);
            tree.rhs = generateNullCheckIfNeeded(tree.rhs, lhsSym.type.getNullMarker());
        }
        result = tree;
    }

    @Override
    public void visitAssignop(JCAssignOp tree) {
        Symbol lhsSym = TreeInfo.symbolFor(tree.lhs);
        Symbol rhsSym = TreeInfo.symbolFor(tree.rhs);
        if (lhsSym != null &&
                rhsSym != null) {
            tree.rhs = translate(tree.rhs);
            tree.rhs = generateNullCheckIfNeeded(tree.rhs, lhsSym.type.getNullMarker());
        }
        result = tree;
    }

    /*public void visitTypeCast(JCTypeCast tree) {
        if (types.isNonNullable(tree.clazz.type) && !types.isNonNullable(tree.expr.type)) {
            tree.expr = translate(tree.expr, tree.type);
            tree.expr = attr.makeNullCheck(tree.expr, true);
        }
        result = tree;
    }*/

    Type returnType = null;

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        Type prevRetType = returnType;
        try {
            returnType = tree.sym.type.getReturnType();
            ListBuffer<JCStatement> paramNullChecks = new ListBuffer<>();
            for (JCVariableDecl param : tree.params) {
                if (types.isNonNullable(param.sym.type)) {
                    paramNullChecks.add(
                            make.at(tree.body.pos())
                                    .Exec(generateNullCheckIfNeeded(make.at(tree.body.pos()).Ident(param),
                                    JCNullableTypeExpression.NullMarker.NOT_NULL))
                    );
                }
            }
            if (!paramNullChecks.isEmpty()) {
                tree.body.stats = tree.body.stats.prependList(paramNullChecks.toList());
            }
            tree.body = translate(tree.body);
            result = tree;
        } finally {
            returnType = prevRetType;
        }
    }

    @Override
    public void visitReturn(JCReturn tree) {
        if (returnType != null && !returnType.hasTag(VOID)) {
            Symbol sym = TreeInfo.symbolFor(tree.expr);
            if (sym != null && !types.isNonNullable(sym.type)) {
                tree.expr = generateNullCheckIfNeeded(tree.expr, returnType.getNullMarker());
            }
            tree.expr = translate(tree.expr);
        }
        result = tree;
    }
}
