/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8275825
 * @summary Verify that trivial accessor methods operating on an inline type
 *          field are C2 compiled to enable scalarization of the arg/return value.
 * @requires vm.compiler2.enabled
 * @library /test/lib /compiler/whitebox /
 * @compile TestTrivialMethods.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbatch
 *                   -XX:+InlineTypePassFieldsAsArgs -XX:+InlineTypeReturnedAsFields
 *                   -XX:CompileCommand=dontinline,*::getter* -XX:CompileCommand=dontinline,*::setter*
 *                   compiler.valhalla.inlinetypes.TestTrivialMethods
 */

package compiler.valhalla.inlinetypes;

import compiler.whitebox.CompilerWhiteBoxTest;

import java.lang.reflect.Method;

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;

import sun.hotspot.WhiteBox;

public class TestTrivialMethods {
    public static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    static MyValue3 staticField = MyValue3.create();
    MyValue3 field = MyValue3.create();

    public MyValue3 getter1() {
        return staticField;
    }

    public static MyValue3 getter2() {
        return staticField;
    }

    public MyValue3 getter3() {
        return field;
    }

    public void setter1(MyValue3 val) {
        staticField = val;
    }

    public static void setter2(MyValue3 val) {
        staticField = val;
    }

    public void setter3(MyValue3 val) {
        field = val;
    }

    public static void main(String[] args) throws Exception {
        TestTrivialMethods t = new TestTrivialMethods();
        // Warmup to trigger compilation
        for (int i = 0; i < 100_000; ++i) {
            t.getter1();
            t.getter2();
            t.getter3();
            t.setter1(staticField);
            t.setter2(staticField);
            t.setter3(staticField);
        }
        Method m = TestTrivialMethods.class.getMethod("getter1");
        Asserts.assertEQ(WHITE_BOX.getMethodCompilationLevel(m, false), CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION, "getter1 is not C2 compiled");
        m = TestTrivialMethods.class.getMethod("getter2");
        Asserts.assertEQ(WHITE_BOX.getMethodCompilationLevel(m, false), CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION, "getter2 is not C2 compiled");
        m = TestTrivialMethods.class.getMethod("getter3");
        Asserts.assertEQ(WHITE_BOX.getMethodCompilationLevel(m, false), CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION, "getter3 is not C2 compiled");
        m = TestTrivialMethods.class.getMethod("setter1", MyValue3.class.asValueType());
        Asserts.assertEQ(WHITE_BOX.getMethodCompilationLevel(m, false), CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION, "setter1 is not C2 compiled");
        m = TestTrivialMethods.class.getMethod("setter2", MyValue3.class.asValueType());
        Asserts.assertEQ(WHITE_BOX.getMethodCompilationLevel(m, false), CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION, "setter2 is not C2 compiled");
        m = TestTrivialMethods.class.getMethod("setter3", MyValue3.class.asValueType());
        Asserts.assertEQ(WHITE_BOX.getMethodCompilationLevel(m, false), CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION, "setter3 is not C2 compiled");
    }
}
