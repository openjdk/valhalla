/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

package compiler.valhalla.inlinetypes;

import java.lang.invoke.*;
import java.lang.reflect.Method;

import jdk.test.lib.Asserts;

import sun.hotspot.WhiteBox;

/**
 * @test TestDeoptimizationWhenBuffering
 * @summary Test correct execution after deoptimizing from inline type specific runtime calls.
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @compile -XDallowWithFieldOperator TestDeoptimizationWhenBuffering.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+DeoptimizeALot -XX:CompileCommand=dontinline,compiler.valhalla.inlinetypes.*::test*
 *                   compiler.valhalla.inlinetypes.TestDeoptimizationWhenBuffering C1
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+DeoptimizeALot -XX:-UseTLAB -Xbatch
 *                   compiler.valhalla.inlinetypes.TestDeoptimizationWhenBuffering
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+DeoptimizeALot -XX:-UseTLAB -Xbatch -XX:-MonomorphicArrayCheck -XX:-AlwaysIncrementalInline
 *                   -XX:-InlineTypePassFieldsAsArgs -XX:-InlineTypeReturnedAsFields -XX:FlatArrayElementMaxSize=1
 *                   -XX:CompileCommand=dontinline,compiler.valhalla.inlinetypes.*::test*
 *                   compiler.valhalla.inlinetypes.TestDeoptimizationWhenBuffering
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+DeoptimizeALot -XX:-UseTLAB -Xbatch -XX:-MonomorphicArrayCheck -XX:+AlwaysIncrementalInline
 *                   -XX:-InlineTypePassFieldsAsArgs -XX:-InlineTypeReturnedAsFields -XX:FlatArrayElementMaxSize=1
 *                   -XX:CompileCommand=dontinline,compiler.valhalla.inlinetypes.*::test*
 *                   compiler.valhalla.inlinetypes.TestDeoptimizationWhenBuffering
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+DeoptimizeALot -XX:-UseTLAB -Xbatch -XX:-MonomorphicArrayCheck -XX:-AlwaysIncrementalInline
 *                   -XX:+InlineTypePassFieldsAsArgs -XX:+InlineTypeReturnedAsFields -XX:FlatArrayElementMaxSize=-1
 *                   -XX:CompileCommand=dontinline,compiler.valhalla.inlinetypes.*::test*
 *                   compiler.valhalla.inlinetypes.TestDeoptimizationWhenBuffering
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+DeoptimizeALot -XX:-UseTLAB -Xbatch -XX:-MonomorphicArrayCheck -XX:+AlwaysIncrementalInline
 *                   -XX:+InlineTypePassFieldsAsArgs -XX:+InlineTypeReturnedAsFields -XX:FlatArrayElementMaxSize=-1
 *                   -XX:CompileCommand=dontinline,compiler.valhalla.inlinetypes.*::test*
 *                   compiler.valhalla.inlinetypes.TestDeoptimizationWhenBuffering
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+DeoptimizeALot -XX:-UseTLAB -Xbatch -XX:-MonomorphicArrayCheck -XX:-AlwaysIncrementalInline
 *                   -XX:+InlineTypePassFieldsAsArgs -XX:+InlineTypeReturnedAsFields -XX:FlatArrayElementMaxSize=-1 -XX:InlineFieldMaxFlatSize=0
 *                   -XX:CompileCommand=dontinline,compiler.valhalla.inlinetypes.*::test*
 *                   compiler.valhalla.inlinetypes.TestDeoptimizationWhenBuffering
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+DeoptimizeALot -XX:-UseTLAB -Xbatch -XX:-MonomorphicArrayCheck -XX:+AlwaysIncrementalInline
 *                   -XX:+InlineTypePassFieldsAsArgs -XX:+InlineTypeReturnedAsFields -XX:FlatArrayElementMaxSize=-1 -XX:InlineFieldMaxFlatSize=0
 *                   -XX:CompileCommand=dontinline,compiler.valhalla.inlinetypes.*::test*
 *                   compiler.valhalla.inlinetypes.TestDeoptimizationWhenBuffering
 */

final primitive class MyValue1 {
    static int cnt = 0;
    final int x;
    final MyValue2 vtField1;
    final MyValue2.ref vtField2;

    public MyValue1() {
        this.x = ++cnt;
        this.vtField1 = new MyValue2();
        this.vtField2 = new MyValue2();
    }

    public int hash() {
        return x + vtField1.x + vtField2.x;
    }

    public MyValue1 testWithField(int x) {
        return __WithField(this.x, x);
    }
}

final primitive class MyValue2 {
    static int cnt = 0;
    final int x;
    public MyValue2() {
        this.x = ++cnt;
    }
}

public class TestDeoptimizationWhenBuffering {
    static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    static final int COMP_LEVEL_FULL_OPTIMIZATION = 4; // C2 or JVMCI

    static {
        try {
            Class<?> clazz = TestDeoptimizationWhenBuffering.class;
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodType mt = MethodType.methodType(MyValue1.class);
            test9_mh = lookup.findStatic(clazz, "test9Callee", mt);
            test10_mh = lookup.findStatic(clazz, "test10Callee", mt);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("Method handle lookup failed");
        }
    }

    MyValue1 test1() {
        return new MyValue1();
    }

    static MyValue1 vtField1;

    MyValue1 test2() {
        vtField1 = new MyValue1();
        return vtField1;
    }

    public int test3Callee(MyValue1 vt) {
        return vt.hash();
    }

    int test3() {
        MyValue1 vt = new MyValue1();
        return test3Callee(vt);
    }

    static MyValue1[] vtArray = new MyValue1[1];

    MyValue1 test4() {
        vtArray[0] = new MyValue1();
        return vtArray[0];
    }

    Object test5(Object[] array) {
        array[0] = new MyValue1();
        return array[0];
    }

    boolean test6(Object obj) {
        MyValue1 vt = new MyValue1();
        return vt == obj;
    }

    Object test7(Object[] obj) {
        return obj[0];
    }

    MyValue1.ref test8(MyValue1.ref[] obj) {
        return obj[0];
    }

    static final MethodHandle test9_mh;

    public static MyValue1 test9Callee() {
        return new MyValue1();
    }

    MyValue1 test9() throws Throwable {
        return (MyValue1)test9_mh.invokeExact();
    }

    static final MethodHandle test10_mh;
    static final MyValue1 test10Field = new MyValue1();
    static int test10Counter = 0;

    public static MyValue1 test10Callee() {
        test10Counter++;
        return test10Field;
    }

    Object test10() throws Throwable {
        return test10_mh.invoke();
    }

    MyValue1 test11(MyValue1 vt) {
        return vt.testWithField(42);
    }

    MyValue1 vtField2;

    MyValue1 test12() {
        vtField2 = new MyValue1();
        return vtField2;
    }

    public static void main(String[] args) throws Throwable {
        if (args.length > 0) {
            // Compile callees with C1 only, to exercise deoptimization while buffering at method entry
            Asserts.assertEQ(args[0], "C1", "unsupported mode");
            Method m = MyValue1.class.getMethod("testWithField", int.class);
            WHITE_BOX.makeMethodNotCompilable(m, COMP_LEVEL_FULL_OPTIMIZATION, false);
            m = TestDeoptimizationWhenBuffering.class.getMethod("test3Callee", MyValue1.class);
            WHITE_BOX.makeMethodNotCompilable(m, COMP_LEVEL_FULL_OPTIMIZATION, false);
            m = TestDeoptimizationWhenBuffering.class.getMethod("test9Callee");
            WHITE_BOX.makeMethodNotCompilable(m, COMP_LEVEL_FULL_OPTIMIZATION, false);
            m = TestDeoptimizationWhenBuffering.class.getMethod("test10Callee");
            WHITE_BOX.makeMethodNotCompilable(m, COMP_LEVEL_FULL_OPTIMIZATION, false);
        }

        MyValue1[] va = new MyValue1[3];
        va[0] = new MyValue1();
        Object[] oa = new Object[3];
        oa[0] = va[0];
        TestDeoptimizationWhenBuffering t = new TestDeoptimizationWhenBuffering();
        for (int i = 0; i < 100_000; ++i) {
            // Check counters to make sure that we don't accidentially reexecute calls when deoptimizing
            int expected = MyValue1.cnt + MyValue2.cnt + MyValue2.cnt;
            Asserts.assertEQ(t.test1().hash(), expected + 4);
            vtField1 = MyValue1.default;
            Asserts.assertEQ(t.test2().hash(), expected + 9);
            Asserts.assertEQ(vtField1.hash(), expected + 9);
            Asserts.assertEQ(t.test3(), expected + 14);
            Asserts.assertEQ(t.test4().hash(), expected + 19);
            Asserts.assertEQ(((MyValue1)t.test5(vtArray)).hash(), expected + 24);
            Asserts.assertEQ(t.test6(vtField1), false);
            Asserts.assertEQ(t.test7(((i % 2) == 0) ? va : oa), va[0]);
            Asserts.assertEQ(t.test8(va), va[0]);
            Asserts.assertEQ(t.test8(va), va[0]);
            Asserts.assertEQ(t.test9().hash(), expected + 34);
            int count = test10Counter;
            Asserts.assertEQ(((MyValue1)t.test10()).hash(), test10Field.hash());
            Asserts.assertEQ(t.test10Counter, count + 1);
            Asserts.assertEQ(t.test11(va[0]).hash(), va[0].testWithField(42).hash());
            t.vtField2 = MyValue1.default;
            Asserts.assertEQ(t.test12().hash(), expected + 39);
            Asserts.assertEQ(t.vtField2.hash(), expected + 39);
        }
    }
}
