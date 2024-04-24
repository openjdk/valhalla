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

import java.util.Random;

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.test.whitebox.WhiteBox;

/**
 * @test TestValueConstruction
 * @summary Test construction of value objects.
 * @key randomness
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @enablePreview
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+WhiteBoxAPI -Xbatch
 *                   -XX:CompileCommand=inline,TestValueConstruction::checkDeopt
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+WhiteBoxAPI -Xbatch -XX:+DeoptimizeALot
 *                   -XX:CompileCommand=inline,TestValueConstruction::checkDeopt
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=compileonly,*TestValueConstruction::test* -Xbatch
 *                   -XX:CompileCommand=inline,TestValueConstruction::checkDeopt
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=dontinline,*MyValue*::<init> -Xbatch
 *                   -XX:CompileCommand=inline,TestValueConstruction::checkDeopt
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=dontinline,*Object::<init> -Xbatch
 *                   -XX:CompileCommand=inline,TestValueConstruction::checkDeopt
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+WhiteBoxAPI -XX:+DeoptimizeALot
 *                   -XX:CompileCommand=dontinline,*Object::<init> -Xbatch
 *                   -XX:CompileCommand=inline,TestValueConstruction::checkDeopt
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=dontinline,*MyAbstract::<init> -Xbatch
 *                   -XX:CompileCommand=inline,TestValueConstruction::checkDeopt
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+WhiteBoxAPI -Xbatch
 *                   -XX:-TieredCompilation -XX:+UnlockDiagnosticVMOptions -XX:+StressIncrementalInlining
 *                   -XX:CompileCommand=inline,TestValueConstruction::checkDeopt
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+WhiteBoxAPI
 *                   -XX:-TieredCompilation -XX:+UnlockDiagnosticVMOptions -XX:+StressIncrementalInlining
 *                   -XX:CompileCommand=inline,TestValueConstruction::checkDeopt
 *                   -XX:CompileCommand=compileonly,*TestValueConstruction::test* -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+WhiteBoxAPI
 *                   -XX:-TieredCompilation -XX:+UnlockDiagnosticVMOptions -XX:+StressIncrementalInlining
 *                   -XX:CompileCommand=inline,TestValueConstruction::checkDeopt
 *                   -XX:CompileCommand=dontinline,*MyValue*::<init> -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+WhiteBoxAPI
 *                   -XX:-TieredCompilation -XX:+UnlockDiagnosticVMOptions -XX:+StressIncrementalInlining
 *                   -XX:CompileCommand=inline,TestValueConstruction::checkDeopt
 *                   -XX:CompileCommand=dontinline,*Object::<init> -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+WhiteBoxAPI
 *                   -XX:-TieredCompilation -XX:+UnlockDiagnosticVMOptions -XX:+StressIncrementalInlining
 *                   -XX:CompileCommand=inline,TestValueConstruction::checkDeopt
 *                   -XX:CompileCommand=dontinline,*MyAbstract::<init> -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 */

public class TestValueConstruction {
    static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    static boolean VERBOSE = false;
    static boolean deopt[] = new boolean[13];

    static void reportDeopt(int deoptNum) {
        System.out.println("Deopt " + deoptNum + " triggered");
        if (VERBOSE) {
            new Exception().printStackTrace(System.out);
        }
    }

    // Trigger deopts at various places
    static void checkDeopt(int deoptNum) {
        if (deopt[deoptNum]) {
            // C2 will add an uncommon trap here
            reportDeopt(deoptNum);
        }
    }

    static interface MyInterface {

    }

    static value class MyValue1 implements MyInterface {
        int x;

        public MyValue1(int x) {
            checkDeopt(0);
            this.x = x;
            checkDeopt(1);
            super();
            checkDeopt(2);
        }

        public MyValue1(int x, int deoptNum1, int deoptNum2, int deoptNum3) {
            checkDeopt(deoptNum1);
            this.x = x;
            checkDeopt(deoptNum2);
            super();
            checkDeopt(deoptNum3);
        }

        public String toString() {
            return "x: " + x;
        }
    }

    static abstract value class MyAbstract1 { }

    static value class MyValue2 extends MyAbstract1 {
        int x;

        public MyValue2(int x) {
            checkDeopt(0);
            this.x = x;
            checkDeopt(1);
            super();
            checkDeopt(2);
        }

        public String toString() {
            return "x: " + x;
        }
    }

    static abstract value class MyAbstract2 {
        public MyAbstract2(int x) {
            checkDeopt(0);
        }
    }

    static value class MyValue3 extends MyAbstract2 {
        int x;

        public MyValue3(int x) {
            checkDeopt(1);
            this(x, 0);
            helper1(this, x, 2); // 'this' escapes through argument
            helper2(x, 3); // 'this' escapes through receiver
            checkDeopt(4);
        }

        public MyValue3(int x, int unused) {
            this.x = helper3(x, 5);
            super(x);
            helper1(this, x, 6); // 'this' escapes through argument
            helper2(x, 7); // 'this' escapes through receiver
            checkDeopt(8);
        }

        public static void helper1(MyValue3 obj, int x, int deoptNum) {
            checkDeopt(deoptNum);
            Asserts.assertEQ(obj.x, x);
        }

        public void helper2(int x, int deoptNum) {
            checkDeopt(deoptNum);
            Asserts.assertEQ(this.x, x);
        }

        public static int helper3(int x, int deoptNum) {
            checkDeopt(deoptNum);
            return x;
        }

        public String toString() {
            return "x: " + x;
        }
    }

    static value class MyValue4 {
        Integer x;

        public MyValue4(int x) {
            checkDeopt(0);
            this.x = x;
            checkDeopt(1);
            super();
            checkDeopt(2);
        }

        public String toString() {
            return "x: " + x;
        }
    }

    static value class MyValue5 {
        int x;

        public MyValue5(int x, boolean b) {
            checkDeopt(0);
            if (b) {
                checkDeopt(1);
                this.x = 42;
                checkDeopt(2);
            } else {
                checkDeopt(3);
                this.x = x;
                checkDeopt(4);
            }
            checkDeopt(5);
            super();
            checkDeopt(6);
        }

        public String toString() {
            return "x: " + x;
        }
    }

    static value class MyValue6 {
        int x;
        MyValue1 val1;
        MyValue1 val2;

        public MyValue6(int x) {
            checkDeopt(0);
            this.x = x;
            checkDeopt(1);
            this.val1 = new MyValue1(x, 2, 3, 4);
            checkDeopt(5);
            this.val2 = new MyValue1(x + 1, 6, 7, 8);
            checkDeopt(9);
            super();
            checkDeopt(10);
        }

        public String toString() {
            return "x: " + x + ", val1: [" + val1 + "], val2: [" + val2 + "]";
        }
    }

    // Same as MyValue6 but unused MyValue1 construction
    static value class MyValue7 {
        int x;

        public MyValue7(int x) {
            checkDeopt(0);
            this.x = x;
            checkDeopt(1);
            new MyValue1(42, 2, 3, 4);
            checkDeopt(5);
            new MyValue1(43, 6, 7, 8);
            checkDeopt(9);
            super();
            checkDeopt(10);
        }

        public String toString() {
            return "x: " + x;
        }
    }

    // Constructor calling another constructor of the same value class with control flow dependent initialization
    static value class MyValue8 {
        int x;

        public MyValue8(int x) {
            checkDeopt(0);
            this(x, 0);
            checkDeopt(1);
        }

        public MyValue8(int x, int unused1) {
            checkDeopt(2);
            if ((x % 2) == 0) {
                checkDeopt(3);
                this.x = 42;
                checkDeopt(4);
            } else {
                checkDeopt(5);
                this.x = x;
                checkDeopt(6);
            }
            checkDeopt(7);
            super();
            checkDeopt(8);
        }

        public MyValue8(int x, int unused1, int unused2) {
            checkDeopt(3);
            this.x = x;
            checkDeopt(4);
        }

        public static MyValue8 valueOf(int x) {
            checkDeopt(0);
            if ((x % 2) == 0) {
                checkDeopt(1);
                return new MyValue8(42, 0, 0);
            } else {
                checkDeopt(2);
                return new MyValue8(x, 0, 0);
            }
        }

        public String toString() {
            return "x: " + x;
        }
    }

    // Constructor calling another constructor of a different value class
    static value class MyValue9 {
        MyValue8 val;

        public MyValue9(int x) {
            checkDeopt(9);
            this(x, 0);
            checkDeopt(10);
        }

        public MyValue9(int i, int unused1) {
            checkDeopt(11);
            val = new MyValue8(i);
            checkDeopt(12);
        }

        public MyValue9(int x, int unused1, int unused2) {
            checkDeopt(5);
            this(x, 0, 0, 0);
            checkDeopt(6);
        }

        public MyValue9(int i, int unused1, int unused2, int unused3) {
            checkDeopt(7);
            val = MyValue8.valueOf(i);
            checkDeopt(8);
        }

        public String toString() {
            return "val: [" + val + "]";
        }
    }

    // Constructor with a loop
    static value class MyValue10 {
        int x;
        int y;

        public MyValue10(int x, int cnt) {
            checkDeopt(0);
            this.x = x;
            checkDeopt(1);
            int res = 0;
            for (int i = 0; i < cnt; ++i) {
                checkDeopt(2);
                res += x;
                checkDeopt(3);
            }
            checkDeopt(4);
            this.y = res;
            checkDeopt(5);
            super();
            checkDeopt(6);
        }

        public String toString() {
            return "x: " + x + ", y: " + y;
        }
    }

    // Value class with recursive field definitions
    static value class MyValue11 {
        int x;
        MyValue11 val1;
        MyValue11 val2;

        public MyValue11(int x) {
            checkDeopt(0);
            this.x = x;
            checkDeopt(1);
            this.val1 = new MyValue11(x + 1, 2, 3, 4, 5);
            checkDeopt(6);
            this.val2 = new MyValue11(x + 2, 7, 8, 9, 10);
            checkDeopt(11);
        }

        public MyValue11(int x, int deoptNum1, int deoptNum2, int deoptNum3, int deoptNum4) {
            checkDeopt(deoptNum1);
            this.x = x;
            checkDeopt(deoptNum2);
            this.val1 = null;
            checkDeopt(deoptNum3);
            this.val2 = null;
            checkDeopt(deoptNum4);
        }

        public String toString() {
            return "x: " + x + ", val1: [" + (val1 != this ? val1 : "this") + "], val2: [" + (val2 != this ? val2 : "this") + "]";
        }
    }

    public static int test1(int x) {
        MyValue1 val = new MyValue1(x);
        checkDeopt(3);
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
            checkDeopt(3);
        }
        return res;
    }

    public static MyValue1 test4(int x) {
        MyValue1 v = new MyValue1(x);
        checkDeopt(3);
        v = new MyValue1(x);
        return v;
    }

    public static int test5(int x) {
        MyValue2 val = new MyValue2(x);
        checkDeopt(3);
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
            checkDeopt(3);
        }
        return res;
    }

    public static MyValue2 test8(int x) {
        MyValue2 v = new MyValue2(x);
        checkDeopt(3);
        v = new MyValue2(x);
        return v;
    }

    public static int test9(int x) {
        MyValue3 val = new MyValue3(x);
        checkDeopt(9);
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
            checkDeopt(9);
            res = new MyValue3(i);
        }
        return res;
    }

    public static MyValue3 test12(int x) {
        MyValue3 v = new MyValue3(x);
        checkDeopt(9);
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

    public static MyValue11 test25(int x) {
        return new MyValue11(x);
    }

    public static void main(String[] args) throws Exception {
        Random rand = Utils.getRandomInstance();

        // Randomly exclude some constructors from inlining via the WhiteBox API because CompileCommands don't match on different signatures.
        WHITE_BOX.testSetDontInlineMethod(MyValue1.class.getConstructor(int.class), rand.nextBoolean());
        WHITE_BOX.testSetDontInlineMethod(MyValue1.class.getConstructor(int.class, int.class, int.class, int.class), rand.nextBoolean());
        WHITE_BOX.testSetDontInlineMethod(MyValue3.class.getConstructor(int.class), rand.nextBoolean());
        WHITE_BOX.testSetDontInlineMethod(MyValue3.class.getConstructor(int.class, int.class), rand.nextBoolean());
        WHITE_BOX.testSetDontInlineMethod(MyValue8.class.getConstructor(int.class), rand.nextBoolean());
        WHITE_BOX.testSetDontInlineMethod(MyValue8.class.getConstructor(int.class, int.class), rand.nextBoolean());
        WHITE_BOX.testSetDontInlineMethod(MyValue8.class.getConstructor(int.class, int.class, int.class), rand.nextBoolean());
        WHITE_BOX.testSetDontInlineMethod(MyValue9.class.getConstructor(int.class), rand.nextBoolean());
        WHITE_BOX.testSetDontInlineMethod(MyValue9.class.getConstructor(int.class, int.class), rand.nextBoolean());
        WHITE_BOX.testSetDontInlineMethod(MyValue9.class.getConstructor(int.class, int.class, int.class), rand.nextBoolean());
        WHITE_BOX.testSetDontInlineMethod(MyValue9.class.getConstructor(int.class, int.class, int.class, int.class), rand.nextBoolean());
        WHITE_BOX.testSetDontInlineMethod(MyValue11.class.getConstructor(int.class), rand.nextBoolean());
        WHITE_BOX.testSetDontInlineMethod(MyValue11.class.getConstructor(int.class, int.class, int.class, int.class, int.class), rand.nextBoolean());

        Integer deoptNum = Integer.getInteger("deoptNum");
        if (deoptNum == null) {
            deoptNum = rand.nextInt(deopt.length);
        }
        for (int x = 0; x <= 50_000; ++x) {
            if (x == 50_000) {
                // Last iteration, trigger deoptimization
                deopt[deoptNum] = true;
            }
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
            Asserts.assertEQ(test25(x), new MyValue11(x));
        }
    }
}
