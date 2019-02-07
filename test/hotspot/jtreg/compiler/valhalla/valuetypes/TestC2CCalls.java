/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @summary Test value type calling convention with compiled to compiled calls.
 * @run main/othervm -XX:+EnableValhalla
 *                   TestC2CCalls
 * @run main/othervm -XX:+EnableValhalla -XX:-UseBimorphicInlining -Xbatch
 *                   -XX:CompileCommand=compileonly,TestC2CCalls*::test*
 *                   -XX:CompileCommand=dontinline,TestC2CCalls*::test*
 *                   TestC2CCalls
 * @run main/othervm -XX:+EnableValhalla -XX:-UseBimorphicInlining -Xbatch -XX:-ProfileInterpreter
 *                   -XX:CompileCommand=compileonly,TestC2CCalls*::test*
 *                   -XX:CompileCommand=dontinline,TestC2CCalls*::test*
 *                   TestC2CCalls
 * @run main/othervm -XX:+EnableValhalla -XX:-UseBimorphicInlining -Xbatch
 *                   -XX:CompileCommand=compileonly,TestC2CCalls::test*
 *                   -XX:CompileCommand=dontinline,TestC2CCalls*::test*
 *                   TestC2CCalls
 * @run main/othervm -XX:+EnableValhalla -XX:-UseBimorphicInlining -Xbatch -XX:-ProfileInterpreter
 *                   -XX:CompileCommand=compileonly,TestC2CCalls::test*
 *                   -XX:CompileCommand=dontinline,TestC2CCalls*::test*
 *                   TestC2CCalls
 */

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;

public class TestC2CCalls {

    public static final int rI = Utils.getRandomInstance().nextInt() % 1000;

    static value class OtherVal {
        public final int x;

        private OtherVal(int x) {
            this.x = x;
        }
    }

    static interface MyInterface1 {
        public int test1(OtherVal other, int y);
        public int test2(OtherVal.val other1, OtherVal.box other2, int y);
        public int test3(OtherVal.val other1, OtherVal.box other2, int y, boolean deopt);
        public int test4(OtherVal.val other1, OtherVal.box other2, int y);
        public int test5(OtherVal.val other1, OtherVal.box other2, int y);
        public int test6();
        public int test7(int i1, int i2, int i3, int i4, int i5, int i6);
        public int test8(int i1, int i2, int i3, int i4, int i5, int i6, int i7);
        public int test9(MyValue3 other, int i1, int i2, int i3, int i4, int i5, int i6);
        public int test10(MyValue4 other, int i1, int i2, int i3, int i4, int i5, int i6);
    }

    static value class MyValue1 implements MyInterface1 {
        public final int x;

        private MyValue1(int x) {
            this.x = x;
        }

        @Override
        public int test1(OtherVal other, int y) {
            return x + other.x + y;
        }

        @Override
        public int test2(OtherVal.val other1, OtherVal.box other2, int y) {
            return x + other1.x + other2.x + y;
        }

        @Override
        public int test3(OtherVal.val other1, OtherVal.box other2, int y, boolean deopt) {
            if (!deopt) {
              return x + other1.x + other2.x + y;
            } else {
              // Uncommon trap
              return test1(other1, y);
            }
        }

        @Override
        public int test4(OtherVal.val other1, OtherVal.box other2, int y) {
            return x + other1.x + other2.x + y;
        }

        @Override
        public int test5(OtherVal.val other1, OtherVal.box other2, int y) {
            return x + other1.x + other2.x + y;
        }

        @Override
        public int test6() {
            return x;
        }

        @Override
        public int test7(int i1, int i2, int i3, int i4, int i5, int i6) {
            return x + i1 + i2 + i3 + i4 + i5 + i6;
        }

        @Override
        public int test8(int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
            return x + i1 + i2 + i3 + i4 + i5 + i6 + i7;
        }

        public int test9(MyValue3 other, int i1, int i2, int i3, int i4, int i5, int i6) {
            return x + (int)(other.d1 + other.d2 + other.d3 + other.d4) + i1 + i2 + i3 + i4 + i5 + i6;
        }

        public int test10(MyValue4 other, int i1, int i2, int i3, int i4, int i5, int i6) {
            return x + other.x1 + other.x2 + other.x3 + other.x4 + i1 + i2 + i3 + i4 + i5 + i6;
        }
    }

    static value class MyValue2 implements MyInterface1 {
        public final int x;

        private MyValue2(int x) {
            this.x = x;
        }

        @Override
        public int test1(OtherVal other, int y) {
            return x + other.x + y;
        }

        @Override
        public int test2(OtherVal.val other1, OtherVal.box other2, int y) {
            return x + other1.x + other2.x + y;
        }

        @Override
        public int test3(OtherVal.val other1, OtherVal.box other2, int y, boolean deopt) {
            if (!deopt) {
              return x + other1.x + other2.x + y;
            } else {
              // Uncommon trap
              return test1(other1, y);
            }
        }

        @Override
        public int test4(OtherVal.val other1, OtherVal.box other2, int y) {
            return x + other1.x + other2.x + y;
        }

        @Override
        public int test5(OtherVal.val other1, OtherVal.box other2, int y) {
            return x + other1.x + other2.x + y;
        }

        @Override
        public int test6() {
            return x;
        }

        @Override
        public int test7(int i1, int i2, int i3, int i4, int i5, int i6) {
            return x + i1 + i2 + i3 + i4 + i5 + i6;
        }

        @Override
        public int test8(int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
            return x + i1 + i2 + i3 + i4 + i5 + i6 + i7;
        }

        public int test9(MyValue3 other, int i1, int i2, int i3, int i4, int i5, int i6) {
            return x + (int)(other.d1 + other.d2 + other.d3 + other.d4) + i1 + i2 + i3 + i4 + i5 + i6;
        }

        public int test10(MyValue4 other, int i1, int i2, int i3, int i4, int i5, int i6) {
            return x + other.x1 + other.x2 + other.x3 + other.x4 + i1 + i2 + i3 + i4 + i5 + i6;
        }
    }

    static value class MyValue3 implements MyInterface1 {
        private final double d1;
        private final double d2;
        private final double d3;
        private final double d4;

        private MyValue3(double d) {
            this.d1 = d;
            this.d2 = d;
            this.d3 = d;
            this.d4 = d;
        }

        @Override
        public int test1(OtherVal other, int y) { return 0; }
        @Override
        public int test2(OtherVal.val other1, OtherVal.box other2, int y)  { return 0; }
        @Override
        public int test3(OtherVal.val other1, OtherVal.box other2, int y, boolean deopt)  { return 0; }
        @Override
        public int test4(OtherVal.val other1, OtherVal.box other2, int y)  { return 0; }
        @Override
        public int test5(OtherVal.val other1, OtherVal.box other2, int y)  { return 0; }
        @Override
        public int test6()  { return 0; }

        @Override
        public int test7(int i1, int i2, int i3, int i4, int i5, int i6)  {
            return (int)(d1 + d2 + d3 + d4) + i1 + i2 + i3 + i4 + i5 + i6;
        }

        @Override
        public int test8(int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
            return (int)(d1 + d2 + d3 + d4) + i1 + i2 + i3 + i4 + i5 + i6 + i7;
        }

        public int test9(MyValue3 other, int i1, int i2, int i3, int i4, int i5, int i6) {
            return (int)(d1 + d2 + d3 + d4) + (int)(other.d1 + other.d2 + other.d3 + other.d4) + i1 + i2 + i3 + i4 + i5 + i6;
        }

        public int test10(MyValue4 other, int i1, int i2, int i3, int i4, int i5, int i6) {
            return (int)(d1 + d2 + d3 + d4) + other.x1 + other.x2 + other.x3 + other.x4 + i1 + i2 + i3 + i4 + i5 + i6;
        }
    }

    static value class MyValue4 implements MyInterface1 {
        private final int x1;
        private final int x2;
        private final int x3;
        private final int x4;

        private MyValue4(int i) {
            this.x1 = i;
            this.x2 = i;
            this.x3 = i;
            this.x4 = i;
        }

        @Override
        public int test1(OtherVal other, int y) { return 0; }
        @Override
        public int test2(OtherVal.val other1, OtherVal.box other2, int y)  { return 0; }
        @Override
        public int test3(OtherVal.val other1, OtherVal.box other2, int y, boolean deopt)  { return 0; }
        @Override
        public int test4(OtherVal.val other1, OtherVal.box other2, int y)  { return 0; }
        @Override
        public int test5(OtherVal.val other1, OtherVal.box other2, int y)  { return 0; }
        @Override
        public int test6()  { return 0; }

        @Override
        public int test7(int i1, int i2, int i3, int i4, int i5, int i6)  {
            return x1 + x2 + x3 + x4 + i1 + i2 + i3 + i4 + i5 + i6;
        }

        @Override
        public int test8(int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
            return x1 + x2 + x3 + x4 + i1 + i2 + i3 + i4 + i5 + i6 + i7;
        }

        public int test9(MyValue3 other, int i1, int i2, int i3, int i4, int i5, int i6) {
            return x1 + x2 + x3 + x4 + (int)(other.d1 + other.d2 + other.d3 + other.d4) + i1 + i2 + i3 + i4 + i5 + i6;
        }

        public int test10(MyValue4 other, int i1, int i2, int i3, int i4, int i5, int i6) {
            return x1 + x2 + x3 + x4 + other.x1 + other.x2 + other.x3 + other.x4 + i1 + i2 + i3 + i4 + i5 + i6;
        }
    }

    static class MyObject implements MyInterface1 {
        private final int x;

        private MyObject(int x) {
            this.x = x;
        }

        @Override
        public int test1(OtherVal other, int y) {
            return x + other.x + y;
        }

        @Override
        public int test2(OtherVal.val other1, OtherVal.box other2, int y) {
            return x + other1.x + other2.x + y;
        }

        @Override
        public int test3(OtherVal.val other1, OtherVal.box other2, int y, boolean deopt) {
            if (!deopt) {
              return x + other1.x + other2.x + y;
            } else {
              // Uncommon trap
              return test1(other1, y);
            }
        }

        @Override
        public int test4(OtherVal.val other1, OtherVal.box other2, int y) {
            return x + other1.x + other2.x + y;
        }

        @Override
        public int test5(OtherVal.val other1, OtherVal.box other2, int y) {
            return x + other1.x + other2.x + y;
        }

        @Override
        public int test6() {
            return x;
        }

        @Override
        public int test7(int i1, int i2, int i3, int i4, int i5, int i6) {
            return x + i1 + i2 + i3 + i4 + i5 + i6;
        }

        @Override
        public int test8(int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
            return x + i1 + i2 + i3 + i4 + i5 + i6 + i7;
        }

        public int test9(MyValue3 other, int i1, int i2, int i3, int i4, int i5, int i6) {
            return x + (int)(other.d1 + other.d2 + other.d3 + other.d4) + i1 + i2 + i3 + i4 + i5 + i6;
        }

        public int test10(MyValue4 other, int i1, int i2, int i3, int i4, int i5, int i6) {
            return x + other.x1 + other.x2 + other.x3 + other.x4 + i1 + i2 + i3 + i4 + i5 + i6;
        }
    }

    // Test calling methods with value type arguments through an interface
    public static int test1(MyInterface1 intf, OtherVal other, int y) {
        return intf.test1(other, y);
    }

    public static int test2(MyInterface1 intf, OtherVal other, int y) {
        return intf.test2(other, other, y);
    }

    // Test mixing null-tolerant and null-free value type arguments
    public static int test3(MyValue1 vt, OtherVal other, int y) {
        return vt.test2(other, other, y);
    }

    public static int test4(MyObject obj, OtherVal other, int y) {
        return obj.test2(other, other, y);
    }

    // Optimized interface call with value receiver
    public static int test5(MyInterface1 intf, OtherVal other, int y) {
        return intf.test1(other, y);
    }

    public static int test6(MyInterface1 intf, OtherVal other, int y) {
        return intf.test2(other, other, y);
    }

    // Optimized interface call with object receiver
    public static int test7(MyInterface1 intf, OtherVal other, int y) {
        return intf.test1(other, y);
    }

    public static int test8(MyInterface1 intf, OtherVal other, int y) {
        return intf.test2(other, other, y);
    }

    // Interface calls with deoptimized callee
    public static int test9(MyInterface1 intf, OtherVal other, int y, boolean deopt) {
        return intf.test3(other, other, y, deopt);
    }

    public static int test10(MyInterface1 intf, OtherVal other, int y, boolean deopt) {
        return intf.test3(other, other, y, deopt);
    }

    // Optimized interface calls with deoptimized callee
    public static int test11(MyInterface1 intf, OtherVal other, int y, boolean deopt) {
        return intf.test3(other, other, y, deopt);
    }

    public static int test12(MyInterface1 intf, OtherVal other, int y, boolean deopt) {
        return intf.test3(other, other, y, deopt);
    }

    public static int test13(MyInterface1 intf, OtherVal other, int y, boolean deopt) {
        return intf.test3(other, other, y, deopt);
    }

    public static int test14(MyInterface1 intf, OtherVal other, int y, boolean deopt) {
        return intf.test3(other, other, y, deopt);
    }

    // Interface calls without warmed up / compiled callees
    public static int test15(MyInterface1 intf, OtherVal other, int y) {
        return intf.test4(other, other, y);
    }

    public static int test16(MyInterface1 intf, OtherVal other, int y) {
        return intf.test5(other, other, y);
    }

    // Interface call with no arguments
    public static int test17(MyInterface1 intf) {
        return intf.test6();
    }

    // Calls that require stack extension
    public static int test18(MyInterface1 intf, int y) {
        return intf.test7(y, y, y, y, y, y);
    }

    public static int test19(MyInterface1 intf, int y) {
        return intf.test8(y, y, y, y, y, y, y);
    }

    public static int test20(MyInterface1 intf, MyValue3 v, int y) {
        return intf.test9(v, y, y, y, y, y, y);
    }

    public static int test21(MyInterface1 intf, MyValue4 v, int y) {
        return intf.test10(v, y, y, y, y, y, y);
    }

    public static void main(String[] args) {
        MyValue1 val1 = new MyValue1(rI);
        MyValue2 val2 = new MyValue2(rI);
        MyValue3 val3 = new MyValue3(rI);
        MyValue4 val4 = new MyValue4(rI);
        OtherVal other = new OtherVal(rI+1);
        MyObject obj = new MyObject(rI+2);

        // Make sure callee methods are compiled
        for (int i = 0; i < 10_000; ++i) {
            Asserts.assertEQ(val1.test1(other, rI), 3*rI+1);
            Asserts.assertEQ(val2.test1(other, rI), 3*rI+1);
            Asserts.assertEQ(obj.test1(other, rI), 3*rI+3);
            Asserts.assertEQ(val1.test2(other, other, rI), 4*rI+2);
            Asserts.assertEQ(val2.test2(other, other, rI), 4*rI+2);
            Asserts.assertEQ(obj.test2(other, other, rI), 4*rI+4);
            Asserts.assertEQ(val1.test3(other, other, rI, false), 4*rI+2);
            Asserts.assertEQ(val2.test3(other, other, rI, false), 4*rI+2);
            Asserts.assertEQ(obj.test3(other, other, rI, false), 4*rI+4);
            Asserts.assertEQ(val1.test7(rI, rI, rI, rI, rI, rI), 7*rI);
            Asserts.assertEQ(val2.test7(rI, rI, rI, rI, rI, rI), 7*rI);
            Asserts.assertEQ(val3.test7(rI, rI, rI, rI, rI, rI), 10*rI);
            Asserts.assertEQ(val4.test7(rI, rI, rI, rI, rI, rI), 10*rI);
            Asserts.assertEQ(obj.test7(rI, rI, rI, rI, rI, rI), 7*rI+2);
            Asserts.assertEQ(val1.test8(rI, rI, rI, rI, rI, rI, rI), 8*rI);
            Asserts.assertEQ(val2.test8(rI, rI, rI, rI, rI, rI, rI), 8*rI);
            Asserts.assertEQ(val3.test8(rI, rI, rI, rI, rI, rI, rI), 11*rI);
            Asserts.assertEQ(val4.test8(rI, rI, rI, rI, rI, rI, rI), 11*rI);
            Asserts.assertEQ(obj.test8(rI, rI, rI, rI, rI, rI, rI), 8*rI+2);
            Asserts.assertEQ(val1.test9(val3, rI, rI, rI, rI, rI, rI), 11*rI);
            Asserts.assertEQ(val2.test9(val3, rI, rI, rI, rI, rI, rI), 11*rI);
            Asserts.assertEQ(val3.test9(val3, rI, rI, rI, rI, rI, rI), 14*rI);
            Asserts.assertEQ(val4.test9(val3, rI, rI, rI, rI, rI, rI), 14*rI);
            Asserts.assertEQ(obj.test9(val3, rI, rI, rI, rI, rI, rI), 11*rI+2);
            Asserts.assertEQ(val1.test10(val4, rI, rI, rI, rI, rI, rI), 11*rI);
            Asserts.assertEQ(val2.test10(val4, rI, rI, rI, rI, rI, rI), 11*rI);
            Asserts.assertEQ(val3.test10(val4, rI, rI, rI, rI, rI, rI), 14*rI);
            Asserts.assertEQ(val4.test10(val4, rI, rI, rI, rI, rI, rI), 14*rI);
            Asserts.assertEQ(obj.test10(val4, rI, rI, rI, rI, rI, rI), 11*rI+2);
        }

        // Polute call profile
        for (int i = 0; i < 100; ++i) {
            Asserts.assertEQ(test15(val1, other, rI), 4*rI+2);
            Asserts.assertEQ(test16(obj, other, rI), 4*rI+4);
            Asserts.assertEQ(test17(obj), rI+2);
        }

        // Trigger compilation of caller methods
        for (int i = 0; i < 100_000; ++i) {
            Asserts.assertEQ(test1(val1, other, rI), 3*rI+1);
            Asserts.assertEQ(test1(obj, other, rI), 3*rI+3);
            Asserts.assertEQ(test2(obj, other, rI), 4*rI+4);
            Asserts.assertEQ(test2(val1, other, rI), 4*rI+2);
            Asserts.assertEQ(test3(val1, other, rI), 4*rI+2);
            Asserts.assertEQ(test4(obj, other, rI), 4*rI+4);
            Asserts.assertEQ(test5(val1, other, rI), 3*rI+1);
            Asserts.assertEQ(test6(val1, other, rI), 4*rI+2);
            Asserts.assertEQ(test7(obj, other, rI), 3*rI+3);
            Asserts.assertEQ(test8(obj, other, rI), 4*rI+4);
            Asserts.assertEQ(test9(val1, other, rI, false), 4*rI+2);
            Asserts.assertEQ(test9(obj, other, rI, false), 4*rI+4);
            Asserts.assertEQ(test10(val1, other, rI, false), 4*rI+2);
            Asserts.assertEQ(test10(obj, other, rI, false), 4*rI+4);
            Asserts.assertEQ(test11(val1, other, rI, false), 4*rI+2);
            Asserts.assertEQ(test12(val1, other, rI, false), 4*rI+2);
            Asserts.assertEQ(test13(obj, other, rI, false), 4*rI+4);
            Asserts.assertEQ(test14(obj, other, rI, false), 4*rI+4);
            Asserts.assertEQ(test15(obj, other, rI), 4*rI+4);
            Asserts.assertEQ(test16(val1, other, rI), 4*rI+2);
            Asserts.assertEQ(test17(val1), rI);
            Asserts.assertEQ(test18(val1, rI), 7*rI);
            Asserts.assertEQ(test18(val2, rI), 7*rI);
            Asserts.assertEQ(test18(val3, rI), 10*rI);
            Asserts.assertEQ(test18(val4, rI), 10*rI);
            Asserts.assertEQ(test18(obj, rI), 7*rI+2);
            Asserts.assertEQ(test19(val1, rI), 8*rI);
            Asserts.assertEQ(test19(val2, rI), 8*rI);
            Asserts.assertEQ(test19(val3, rI), 11*rI);
            Asserts.assertEQ(test19(val4, rI), 11*rI);
            Asserts.assertEQ(test19(obj, rI), 8*rI+2);
            Asserts.assertEQ(test20(val1, val3, rI), 11*rI);
            Asserts.assertEQ(test20(val2, val3, rI), 11*rI);
            Asserts.assertEQ(test20(val3, val3, rI), 14*rI);
            Asserts.assertEQ(test20(val4, val3, rI), 14*rI);
            Asserts.assertEQ(test20(obj, val3, rI), 11*rI+2);
            Asserts.assertEQ(test21(val1, val4, rI), 11*rI);
            Asserts.assertEQ(test21(val2, val4, rI), 11*rI);
            Asserts.assertEQ(test21(val3, val4, rI), 14*rI);
            Asserts.assertEQ(test21(val4, val4, rI), 14*rI);
            Asserts.assertEQ(test21(obj, val4, rI), 11*rI+2);
        }

        // Trigger deoptimization
        Asserts.assertEQ(val1.test3(other, other, rI, true), 3*rI+1);
        Asserts.assertEQ(obj.test3(other, other, rI, true), 3*rI+3);

        // Check results of methods still calling the deoptimized methods
        Asserts.assertEQ(test9(val1, other, rI, false), 4*rI+2);
        Asserts.assertEQ(test9(obj, other, rI, false), 4*rI+4);
        Asserts.assertEQ(test10(obj, other, rI, false), 4*rI+4);
        Asserts.assertEQ(test10(val1, other, rI, false), 4*rI+2);
        Asserts.assertEQ(test11(val1, other, rI, false), 4*rI+2);
        Asserts.assertEQ(test11(obj, other, rI, false), 4*rI+4);
        Asserts.assertEQ(test12(obj, other, rI, false), 4*rI+4);
        Asserts.assertEQ(test12(val1, other, rI, false), 4*rI+2);
        Asserts.assertEQ(test13(val1, other, rI, false), 4*rI+2);
        Asserts.assertEQ(test13(obj, other, rI, false), 4*rI+4);
        Asserts.assertEQ(test14(obj, other, rI, false), 4*rI+4);
        Asserts.assertEQ(test14(val1, other, rI, false), 4*rI+2);

        // Check with unexpected arguments
        Asserts.assertEQ(test1(val2, other, rI), 3*rI+1);
        Asserts.assertEQ(test2(val2, other, rI), 4*rI+2);
        Asserts.assertEQ(test5(val2, other, rI), 3*rI+1);
        Asserts.assertEQ(test6(val2, other, rI), 4*rI+2);
        Asserts.assertEQ(test7(val1, other, rI), 3*rI+1);
        Asserts.assertEQ(test8(val1, other, rI), 4*rI+2);
        Asserts.assertEQ(test15(val1, other, rI), 4*rI+2);
        Asserts.assertEQ(test16(obj, other, rI), 4*rI+4);
        Asserts.assertEQ(test17(obj), rI+2);
    }
}
