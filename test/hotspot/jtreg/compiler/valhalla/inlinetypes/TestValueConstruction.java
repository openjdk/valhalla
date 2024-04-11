/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Asserts;

/**
 * @test TestValueConstruction
 * @summary Test construction of value objects.
 * @library /testlibrary /test/lib /
 * @enablePreview
 * @run main/othervm -XX:+EnableValhalla -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -XX:+EnableValhalla
 *                   -XX:CompileCommand=compileonly,*TestValueConstruction::test* -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -XX:+EnableValhalla
 *                   -XX:CompileCommand=dontinline,*MyValue*::<init> -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -XX:+EnableValhalla
 *                   -XX:CompileCommand=dontinline,*Object::<init> -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -XX:+EnableValhalla
 *                   -XX:CompileCommand=dontinline,*MyAbstract::<init> -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 *
 * @run main/othervm -XX:+EnableValhalla -Xbatch -XX:-TieredCompilation -XX:+UnlockDiagnosticVMOptions -XX:+StressIncrementalInlining
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -XX:+EnableValhalla -XX:-TieredCompilation -XX:+UnlockDiagnosticVMOptions -XX:+StressIncrementalInlining
 *                   -XX:CompileCommand=compileonly,*TestValueConstruction::test* -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -XX:+EnableValhalla -XX:-TieredCompilation -XX:+UnlockDiagnosticVMOptions -XX:+StressIncrementalInlining
 *                   -XX:CompileCommand=dontinline,*MyValue*::<init> -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -XX:+EnableValhalla -XX:-TieredCompilation -XX:+UnlockDiagnosticVMOptions -XX:+StressIncrementalInlining
 *                   -XX:CompileCommand=dontinline,*Object::<init> -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -XX:+EnableValhalla -XX:-TieredCompilation -XX:+UnlockDiagnosticVMOptions -XX:+StressIncrementalInlining
 *                   -XX:CompileCommand=dontinline,*MyAbstract::<init> -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 */

// TODO 8325106 Add -XX:+DeoptimizeALot
// TODO 8325106 Convert this to an IR Framework test but make sure that test coverage doesn't suffer

public class TestValueConstruction {

    static interface MyInterface {

    }

    static value class MyValue1 implements MyInterface {
        int x;

        public MyValue1(int x) {
            this.x = x;
        }
    }

    static abstract value class MyAbstract1 { }

    static value class MyValue2 extends MyAbstract1 {
        int x;

        public MyValue2(int x) {
            this.x = x;
        }
    }

    static abstract value class MyAbstract2 {
        public MyAbstract2(int x) {

        }
    }

    static value class MyValue3 extends MyAbstract2 {
        int x;

        public MyValue3(int x) {
            this(x, 0);
            helper1(this, x); // 'this' escapes through argument
            helper2(x); // 'this' escapes through receiver
        }

        public MyValue3(int x, int unused) {
            this.x = helper3(x);
            super(x);
            helper1(this, x); // 'this' escapes through argument
            helper2(x); // 'this' escapes through receiver
        }

        public static void helper1(MyValue3 obj, int x) {
            Asserts.assertEQ(obj.x, x);
        }

        public void helper2(int x) {
            Asserts.assertEQ(this.x, x);
        }

        public static int helper3(int x) {
            return x;
        }
    }

    static value class MyValue4 {
        Integer x;

        public MyValue4(int x) {
            this.x = x;
        }
    }

    static value class MyValue5 {
        int x;

        public MyValue5(int x, boolean b) {
            if (b) {
                this.x = 42;
            } else {
                this.x = x;
            }
        }
    }

    static value class MyValue6 {
        int x;
        MyValue1 val;

        public MyValue6(int x) {
            this.x = x;
            this.val = new MyValue1(x);
            super();
        }
    }

    // Same as MyValue6 but unused MyValue1 construction
    static value class MyValue7 {
        int x;

        public MyValue7(int x) {
            this.x = x;
            new MyValue1(42);
            super();
        }
    }

    // Constructor calling another constructor of the same value class with control flow dependent initialization
    static value class MyValue8 {
        int x;

        public MyValue8(int x) {
            this(x, 0);
        }

        public MyValue8(int x, int unused1) {
            if ((x % 2) == 0) {
                this.x = 42;
            } else {
                this.x = x;
            }
        }

        public MyValue8(int x, int unused1, int unused2) {
            this.x = x;
        }

        public static MyValue8 valueOf(int x) {
            if ((x % 2) == 0) {
                return new MyValue8(42, 0, 0);
            } else {
                return new MyValue8(x, 0, 0);
            }
        }
    }

    // Constructor calling another constructor of a different value class
    static value class MyValue9 {
        MyValue8 val;

        public MyValue9(int x) {
            this(x, 0);
        }

        public MyValue9(int i, int unused1) {
            val = new MyValue8(i);
        }

        public MyValue9(int x, int unused1, int unused2) {
            this(x, 0, 0, 0);
        }

        public MyValue9(int i, int unused1, int unused2, int unused3) {
            val = MyValue8.valueOf(i);
        }
    }

    // Constructor with a loop
    static value class MyValue10 {
        int x;
        int y;

        public MyValue10(int x, int cnt) {
            this.x = x;
            int res = 0;
            for (int i = 0; i < cnt; ++i) {
                res += x;
            }
            this.y = res;
        }
    }

    public static int test1(int x) {
        MyValue1 val = new MyValue1(x);
        return val.x;
    }

    public static MyValue1 helper1(int x) {
        return new MyValue1(x);
    }

    public static Object test2(int x) {
        return helper1(x);
    }

    public static Object test3(int limit) {
        MyValue1 res = null;
        for (int i = 0; i <= 10; ++i) {
            res = new MyValue1(i);
        }
        return res;
    }

    public static MyValue1 test4(int x) {
        MyValue1 v = new MyValue1(x);
        v = new MyValue1(x);
        return v;
    }

    public static int test5(int x) {
        MyValue2 val = new MyValue2(x);
        return val.x;
    }

    public static MyValue2 helper2(int x) {
        return new MyValue2(x);
    }

    public static Object test6(int x) {
        return helper2(x);
    }

    public static Object test7(int limit) {
        MyValue2 res = null;
        for (int i = 0; i <= 10; ++i) {
            res = new MyValue2(i);
        }
        return res;
    }

    public static MyValue2 test8(int x) {
        MyValue2 v = new MyValue2(x);
        v = new MyValue2(x);
        return v;
    }

    public static int test9(int x) {
        MyValue3 val = new MyValue3(x);
        return val.x;
    }

    public static MyValue3 helper3(int x) {
        return new MyValue3(x);
    }

    public static Object test10(int x) {
        return helper3(x);
    }

    public static Object test11(int limit) {
        MyValue3 res = null;
        for (int i = 0; i <= 10; ++i) {
            res = new MyValue3(i);
        }
        return res;
    }

    public static MyValue3 test12(int x) {
        MyValue3 v = new MyValue3(x);
        v = new MyValue3(x);
        return v;
    }

    public static MyValue4 test13(int x) {
        return new MyValue4(x);
    }

    public static MyValue5 test14(int x, boolean b) {
        return new MyValue5(x, b);
    }

    public static Object test15(int x) {
        return new MyValue6(x);
    }

    public static Object test16(int x) {
        return new MyValue7(x);
    }

    public static MyValue8 test17(int x) {
        return new MyValue8(x);
    }

    public static MyValue8 test18(int x) {
        return new MyValue8(x, 0);
    }

    public static MyValue8 test19(int x) {
        return MyValue8.valueOf(x);
    }

    public static MyValue9 test20(int x) {
        return new MyValue9(x);
    }

    public static MyValue9 test21(int x) {
        return new MyValue9(x, 0);
    }

    public static MyValue9 test22(int x) {
        return new MyValue9(x, 0, 0);
    }

    public static MyValue9 test23(int x) {
        return new MyValue9(x, 0, 0, 0);
    }

    public static MyValue10 test24(int x, int cnt) {
        return new MyValue10(x, cnt);
    }

    public static void main(String[] args) {
        for (int x = 0; x < 50_000; ++x) {
            Asserts.assertEQ(test1(x), x);
            Asserts.assertEQ(test2(x), new MyValue1(x));
            Asserts.assertEQ(test3(10), new MyValue1(10));
            Asserts.assertEQ(test4(x), new MyValue1(x));
            Asserts.assertEQ(test5(x), x);
            Asserts.assertEQ(test6(x), new MyValue2(x));
            Asserts.assertEQ(test7(10), new MyValue2(10));
            Asserts.assertEQ(test8(x), new MyValue2(x));
            Asserts.assertEQ(test9(x), x);
            Asserts.assertEQ(test10(x), new MyValue3(x));
            Asserts.assertEQ(test11(10), new MyValue3(10));
            Asserts.assertEQ(test12(x), new MyValue3(x));
            Asserts.assertEQ(test13(x), new MyValue4(x));
            Asserts.assertEQ(test14(x, (x % 2) == 0), new MyValue5(x, (x % 2) == 0));
            Asserts.assertEQ(test15(x), new MyValue6(x));
            Asserts.assertEQ(test16(x), new MyValue7(x));
            Asserts.assertEQ(test17(x), new MyValue8(x));
            Asserts.assertEQ(test18(x), new MyValue8(x));
            Asserts.assertEQ(test19(x), new MyValue8(x));
            Asserts.assertEQ(test20(x), new MyValue9(x));
            Asserts.assertEQ(test21(x), new MyValue9(x));
            Asserts.assertEQ(test22(x), new MyValue9(x));
            Asserts.assertEQ(test23(x), new MyValue9(x));
            Asserts.assertEQ(test24(x, x % 10), new MyValue10(x, x % 10));
        }
    }
}
