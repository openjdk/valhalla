/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/**
 * @test
 * @bug 8237072
 * @summary Test various relationships between a value type and its reference projection.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.util
 */

import java.io.StringWriter;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import javax.lang.model.element.Element;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

import toolbox.ToolBox;

public class ProjectionRelationsTest {

    private static final ToolBox tb = new ToolBox();

    enum Relation {
        SUBTYPING,
        CASTING,
        ASSIGNING,
    }

    public static void main(String... args) throws Exception {
        String code = "primitive class C {\n" +
                "         C.ref cref     = new C();\n" +
                "         C []  ca       = null;\n" +
                "         C.ref [] cra   = null;\n" +
                "         Object[]  oa   = null;\n" +
                      "}\n";
        List<JavaFileObject> files = List.of(new ToolBox.JavaSource(code));

        JavacTool compiler = (JavacTool) ToolProvider.getSystemJavaCompiler();
        StringWriter out = new StringWriter();

        Context context = new Context();

        JavacTask task = (JavacTask) compiler.getTask(out, null, null, List.of("-XDinlinesAreIslands"), null, files, context);
        Iterable<? extends Element> elements = task.analyze();
        if (elements == null || !elements.iterator().hasNext()) {
            throw new RuntimeException("Didn't compile alright!");
        }

        Names names =  Names.instance(context);

        ClassSymbol valueCls = (ClassSymbol) elements.iterator().next();
        Type vType = valueCls.type;
        Type vDotRefType = valueCls.members().findFirst(names.fromString("cref")).type;
        Type vArrayType = valueCls.members().findFirst(names.fromString("ca")).type;
        Type vRefArrayType = valueCls.members().findFirst(names.fromString("cra")).type;
        Type jlOArrayType = valueCls.members().findFirst(names.fromString("oa")).type;

        for (Relation relation : Relation.values()) {
            testRelation(context, relation, vType, vDotRefType);
            testRelation(context, relation, vArrayType, vRefArrayType, jlOArrayType);
        }
    }

    static void testRelation(Context context, Relation relation, Type vType, Type vDotRefType) {
        Types types =  Types.instance(context);
        Symtab syms =  Symtab.instance(context);
        Type intType = syms.intType;
        Type objectType = syms.objectType;
        Type integerType = types.boxedTypeOrType(syms.intType);
        Type stringType = syms.stringType;

        System.out.println("Testing relation " + relation + " between " +
                                       vType.tsym.name + " and " + vDotRefType.tsym.name);
            switch (relation) {
                case SUBTYPING:

                    // self check
                    Assert.check(types.isSubtype(vType, vType));
                    Assert.check(types.isSubtype(vDotRefType, vDotRefType));

                    Assert.check(types.isSubtype(vType, vDotRefType) ==
                                 types.isSubtype(intType, integerType));
                    Assert.check(types.isSubtype(vDotRefType, vType) ==
                                 types.isSubtype(integerType, intType));

                    Assert.check(types.isSubtype(vType, objectType) ==
                                 types.isSubtype(intType, objectType));
                    Assert.check(types.isSubtype(objectType, vType) ==
                                 types.isSubtype(objectType, intType));

                    Assert.check(types.isSubtype(vDotRefType, objectType) ==
                                 types.isSubtype(integerType, objectType));
                    Assert.check(types.isSubtype(objectType, vDotRefType) ==
                                 types.isSubtype(objectType, integerType));

                    // check against a totally unrelated class.
                    Assert.check(types.isSubtype(vType, stringType) ==
                                 types.isSubtype(intType, stringType));
                    Assert.check(types.isSubtype(stringType, vType) ==
                                 types.isSubtype(stringType, intType));

                    Assert.check(types.isSubtype(vDotRefType, stringType) ==
                                 types.isSubtype(integerType, stringType));
                    Assert.check(types.isSubtype(stringType, vDotRefType) ==
                                 types.isSubtype(stringType, integerType));
                    break;

                case CASTING:

                    // self check
                    Assert.check(types.isCastable(vType, vType));
                    Assert.check(types.isCastable(vDotRefType, vDotRefType));

                    Assert.check(types.isCastable(vType, vDotRefType) ==
                                 types.isCastable(intType, integerType));
                    Assert.check(types.isCastable(vDotRefType, vType) ==
                                 types.isCastable(integerType, intType));
                    Assert.check(types.isCastable(vType, objectType) ==
                                 types.isCastable(intType, objectType));
                    Assert.check(types.isCastable(objectType, vType) ==
                                 types.isCastable(objectType, intType));
                    Assert.check(types.isCastable(vDotRefType, objectType) ==
                                 types.isCastable(integerType, objectType));
                    Assert.check(types.isCastable(objectType, vDotRefType) ==
                                 types.isCastable(objectType, integerType));
                    // check against a totally unrelated class.
                    Assert.check(types.isCastable(vType, stringType) ==
                                 types.isCastable(intType, stringType));
                    Assert.check(types.isCastable(stringType, vType) ==
                                 types.isCastable(stringType, intType));

                    Assert.check(types.isCastable(vDotRefType, stringType) ==
                                 types.isCastable(integerType, stringType));
                    Assert.check(types.isCastable(stringType, vDotRefType) ==
                                 types.isCastable(stringType, integerType));
                    break;

                case ASSIGNING:

                    // self check
                    Assert.check(types.isAssignable(vType, vType));
                    Assert.check(types.isAssignable(vDotRefType, vDotRefType));

                    Assert.check(types.isAssignable(vType, vDotRefType) ==
                                 types.isAssignable(intType, integerType));
                    Assert.check(types.isAssignable(vDotRefType, vType) ==
                                 types.isAssignable(integerType, intType));
                    Assert.check(types.isAssignable(vType, objectType) ==
                                 types.isAssignable(intType, objectType));
                    Assert.check(types.isAssignable(objectType, vType) ==
                                 types.isAssignable(objectType, intType));
                    Assert.check(types.isAssignable(vDotRefType, objectType) ==
                                 types.isAssignable(integerType, objectType));
                    Assert.check(types.isAssignable(objectType, vDotRefType) ==
                                 types.isAssignable(objectType, integerType));
                    // check against a totally unrelated class.
                    Assert.check(types.isAssignable(vType, stringType) ==
                                 types.isAssignable(intType, stringType));
                    Assert.check(types.isAssignable(stringType, vType) ==
                                 types.isAssignable(stringType, intType));

                    Assert.check(types.isAssignable(vDotRefType, stringType) ==
                                 types.isAssignable(integerType, stringType));
                    Assert.check(types.isAssignable(stringType, vDotRefType) ==
                                 types.isAssignable(stringType, integerType));
                    break;
            }
    }

    static void testRelation(Context context, Relation relation, Type vArrayType, Type vDotRefArrayType, Type objectArrayType) {
        Types types =  Types.instance(context);
        Symtab syms =  Symtab.instance(context);

        System.out.println("Testing relation " + relation + " between " +
                                       vArrayType.tsym.name + " and " + vDotRefArrayType.tsym.name);
            switch (relation) {
                case SUBTYPING:

                    /* check against self */
                    Assert.check(types.isSubtype(vArrayType, vArrayType));
                    Assert.check(types.isSubtype(vDotRefArrayType, vDotRefArrayType));

                    /* check against valid supers */
                    Assert.check(types.isSubtype(vArrayType, vDotRefArrayType));
                    Assert.check(types.isSubtype(vArrayType, objectArrayType));
                    Assert.check(types.isSubtype(vArrayType, syms.objectType));
                    Assert.check(types.isSubtype(vDotRefArrayType, objectArrayType));
                    Assert.check(types.isSubtype(vDotRefArrayType, syms.objectType));

                    /* check negative cases */
                    Assert.check(!types.isSubtype(vDotRefArrayType, vArrayType));
                    Assert.check(!types.isSubtype(objectArrayType, vArrayType));
                    Assert.check(!types.isSubtype(objectArrayType, vDotRefArrayType));

                    break;

                case CASTING:

                    /* check self cast */
                    Assert.check(types.isCastable(vArrayType, vArrayType));
                    Assert.check(types.isCastable(vDotRefArrayType, vDotRefArrayType));

                    /* check widening cast of V */
                    Assert.check(types.isCastable(vArrayType, vDotRefArrayType));
                    Assert.check(types.isCastable(vArrayType, objectArrayType));
                    Assert.check(types.isCastable(vArrayType, syms.objectType));

                    /* check cast of V.ref to supers */
                    Assert.check(types.isCastable(vDotRefArrayType, objectArrayType));
                    Assert.check(types.isCastable(vDotRefArrayType, syms.objectType));

                    /* check downcasts */
                    Assert.check(types.isCastable(vDotRefArrayType, vArrayType));
                    Assert.check(types.isCastable(objectArrayType, vArrayType));
                    Assert.check(types.isCastable(objectArrayType, vDotRefArrayType));
                    Assert.check(types.isCastable(syms.objectType, vArrayType));
                    Assert.check(types.isCastable(syms.objectType, vDotRefArrayType));

                    break;

                case ASSIGNING:

                    /* check self  */
                    Assert.check(types.isAssignable(vArrayType, vArrayType));
                    Assert.check(types.isAssignable(vDotRefArrayType, vDotRefArrayType));

                    /* check widening */
                    Assert.check(types.isAssignable(vArrayType, vDotRefArrayType));
                    Assert.check(types.isAssignable(vArrayType, objectArrayType));
                    Assert.check(types.isAssignable(vArrayType, syms.objectType));

                    /* check more widening */
                    Assert.check(types.isAssignable(vDotRefArrayType, objectArrayType));
                    Assert.check(types.isAssignable(vDotRefArrayType, syms.objectType));

                    /* misc */
                    Assert.check(!types.isAssignable(vDotRefArrayType, vArrayType));
                    Assert.check(!types.isAssignable(objectArrayType, vArrayType));
                    Assert.check(!types.isAssignable(objectArrayType, vDotRefArrayType));
                    Assert.check(!types.isAssignable(syms.objectType, vArrayType));
                    Assert.check(!types.isAssignable(syms.objectType, vDotRefArrayType));

                    break;
            }
    }
}
