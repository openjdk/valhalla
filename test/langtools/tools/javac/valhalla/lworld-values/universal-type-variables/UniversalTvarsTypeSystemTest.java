/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8159970
 * @summary tests for the type system of universal type variables
 * @library /tools/lib/types
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.compiler/com.sun.tools.javac.file
 * @build TypeHarness
 * @run main UniversalTvarsTypeSystemTest
 */

import java.util.ArrayList;
import java.util.List;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.code.Type.UndetVar;
import com.sun.tools.javac.code.Type.UndetVar.InferenceBound;
import com.sun.tools.javac.code.Types;

import com.sun.tools.javac.comp.Infer;
import com.sun.tools.javac.comp.InferenceContext;

import com.sun.tools.javac.util.Assert;

public class UniversalTvarsTypeSystemTest extends TypeHarness {
    StrToTypeFactory strToTypeFactory;
    Types types;
    Infer infer;

    UniversalTvarsTypeSystemTest() {
        types = Types.instance(context);
        infer = Infer.instance(context);
    }

    public static void main(String... args) throws Exception {
        new UniversalTvarsTypeSystemTest().runAll();
    }

    void runAll() {
        strToTypeFactory = new StrToTypeFactory();
        testLUB_and_GLB();
        test_isBoundedBy();
    }

    void testLUB_and_GLB() {
        java.util.Map<String, Type> typeMap = strToTypeFactory.getTypes(
                List.of(),
                """
                interface I {}
                static abstract class A {}
                primitive class Point extends A implements I {}
                primitive class Circle extends A implements I {}
                """,
                "Point", "Point.ref", "Circle", "Circle.ref", "A", "I");
        Assert.check(types.lub(typeMap.get("Point"), typeMap.get("Point.ref")).tsym == typeMap.get("Point.ref").tsym);
        Assert.check(types.lub(typeMap.get("Point"), typeMap.get("A")).tsym == typeMap.get("A").tsym);
        Assert.check(types.lub(typeMap.get("Point"), typeMap.get("I")).tsym == typeMap.get("I").tsym);
        Assert.check(types.lub(typeMap.get("Circle"), typeMap.get("I")).tsym == typeMap.get("I").tsym);

        Assert.check(types.glb(typeMap.get("Point"), typeMap.get("Point.ref")).tsym == typeMap.get("Point").tsym);
        Assert.check(types.glb(typeMap.get("Point"), typeMap.get("I")).tsym == typeMap.get("Point").tsym);
        Assert.check(types.glb(typeMap.get("Point.ref"), typeMap.get("I")).tsym == typeMap.get("Point.ref").tsym);
        Assert.check(types.glb(typeMap.get("Circle"), typeMap.get("I")).tsym == typeMap.get("Circle").tsym);
        Assert.check(types.glb(typeMap.get("Circle.ref"), typeMap.get("I")).tsym == typeMap.get("Circle.ref").tsym);
    }

    void test_isBoundedBy() {
        java.util.Map<String, Type> typeMap =  strToTypeFactory.getTypes(
                List.of(),
                """
                class MyList<__universal T> {}
                interface Shape {}
                primitive class Point implements Shape {}
                """,
                "Point", "Point.ref", "Object",
                "MyList<Point>", "MyList<? extends Shape>",
                "MyList<Shape>", "MyList<? super Point>");
        Assert.check(types.isBoundedBy(typeMap.get("Point"), typeMap.get("Object")));
        Assert.check(types.isBoundedBy(typeMap.get("Point.ref"), typeMap.get("Object")));
        Assert.check(types.isSubtype(typeMap.get("MyList<Point>"), typeMap.get("MyList<? extends Shape>")));
        Assert.check(types.isSubtype(typeMap.get("MyList<Shape>"), typeMap.get("MyList<? super Point>")));
    }
}
