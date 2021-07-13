/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
import compiler.lib.ir_framework.*;
import static compiler.valhalla.inlinetypes.InlineTypes.rI;
import static compiler.valhalla.inlinetypes.InlineTypes.rL;
import static compiler.valhalla.inlinetypes.InlineTypes.rD;
import static compiler.valhalla.inlinetypes.InlineTypes.IRNode.*;

import java.lang.reflect.Method;
import java.util.Arrays;

/*
 * @test
 * @key randomness
 * @summary Test nullable inline type arrays
 * @library /test/lib /
 * @requires (os.simpleArch == "x64" | os.simpleArch == "aarch64")
 * @run driver/timeout=300 compiler.valhalla.inlinetypes.TestNullableArrays
 */

@ForceCompileClassInitializer
public class TestNullableArrays {

    public static void main(String[] args) {

        Scenario[] scenarios = InlineTypes.DEFAULT_SCENARIOS;
        scenarios[2].addFlags("-XX:-MonomorphicArrayCheck", "-XX:-UncommonNullCast", "-XX:+StressArrayCopyMacroNode");
        scenarios[3].addFlags("-XX:-MonomorphicArrayCheck", "-XX:-UncommonNullCast");
        scenarios[4].addFlags("-XX:-MonomorphicArrayCheck", "-XX:-UncommonNullCast");
        scenarios[5].addFlags("-XX:-MonomorphicArrayCheck", "-XX:-UncommonNullCast", "-XX:+StressArrayCopyMacroNode");

        InlineTypes.getFramework()
                   .addScenarios(scenarios)
                   .addHelperClasses(MyValue1.class,
                                     MyValue2.class,
                                     MyValue2Inline.class)
                   .start();
    }

    // Helper methods

    protected long hash() {
        return hash(rI, rL);
    }

    protected long hash(int x, long y) {
        return MyValue1.createWithFieldsInline(x, y).hash();
    }

    private static final MyValue1 testValue1 = MyValue1.createWithFieldsInline(rI, rL);

    // Test nullable inline type array creation and initialization
    @Test
    @IR(applyIf = {"FlatArrayElementMaxSize", "= -1"},
        counts = {ALLOCA, "= 1"})
    @IR(applyIf = {"FlatArrayElementMaxSize", "!= -1"},
        counts = {ALLOCA, "= 1"},
        failOn = LOAD)
    public MyValue1.ref[] test1(int len) {
        MyValue1.ref[] va = new MyValue1.ref[len];
        if (len > 0) {
            va[0] = null;
        }
        for (int i = 1; i < len; ++i) {
            va[i] = MyValue1.createWithFieldsDontInline(rI, rL);
        }
        return va;
    }

    @Run(test = "test1")
    public void test1_verifier() {
        int len = Math.abs(rI % 10);
        MyValue1.ref[] va = test1(len);
        if (len > 0) {
            Asserts.assertEQ(va[0], null);
        }
        for (int i = 1; i < len; ++i) {
            Asserts.assertEQ(va[i].hash(), hash());
        }
    }

    // Test creation of an inline type array and element access
    @Test
    @IR(failOn = {ALLOC, ALLOCA, LOOP, LOAD, STORE, TRAP})
    public long test2() {
        MyValue1.ref[] va = new MyValue1.ref[1];
        va[0] = MyValue1.createWithFieldsInline(rI, rL);
        return va[0].hash();
    }

    @Run(test = "test2")
    public void test2_verifier() {
        long result = test2();
        Asserts.assertEQ(result, hash());
    }

    // Test receiving an inline type array from the interpreter,
    // updating its elements in a loop and computing a hash.
    @Test
    @IR(failOn = {ALLOCA})
    public long test3(MyValue1.ref[] va) {
        long result = 0;
        for (int i = 0; i < 10; ++i) {
            if (va[i] != null) {
                result += va[i].hash();
            }
            va[i] = MyValue1.createWithFieldsInline(rI + 1, rL + 1);
        }
        va[0] = null;
        return result;
    }

    @Run(test = "test3")
    public void test3_verifier() {
        MyValue1.ref[] va = new MyValue1.ref[10];
        long expected = 0;
        for (int i = 1; i < 10; ++i) {
            va[i] = MyValue1.createWithFieldsDontInline(rI + i, rL + i);
            expected += va[i].hash();
        }
        long result = test3(va);
        Asserts.assertEQ(expected, result);
        Asserts.assertEQ(va[0], null);
        for (int i = 1; i < 10; ++i) {
            if (va[i].hash() != hash(rI + 1, rL + 1)) {
                Asserts.assertEQ(va[i].hash(), hash(rI + 1, rL + 1));
            }
        }
    }

    // Test returning an inline type array received from the interpreter
    @Test
    @IR(failOn = {ALLOC, ALLOCA, LOAD, STORE, LOOP, TRAP})
    public MyValue1.ref[] test4(MyValue1.ref[] va) {
        return va;
    }

    @Run(test = "test4")
    public void test4_verifier() {
        MyValue1.ref[] va = new MyValue1.ref[10];
        for (int i = 0; i < 10; ++i) {
            va[i] = MyValue1.createWithFieldsDontInline(rI + i, rL + i);
        }
        va = test4(va);
        for (int i = 0; i < 10; ++i) {
            Asserts.assertEQ(va[i].hash(), hash(rI + i, rL + i));
        }
    }

    // Merge inline type arrays created from two branches
    @Test
    public MyValue1.ref[] test5(boolean b) {
        MyValue1.ref[] va;
        if (b) {
            va = new MyValue1.ref[5];
            for (int i = 0; i < 5; ++i) {
                va[i] = MyValue1.createWithFieldsInline(rI, rL);
            }
            va[4] = null;
        } else {
            va = new MyValue1.ref[10];
            for (int i = 0; i < 10; ++i) {
                va[i] = MyValue1.createWithFieldsInline(rI + i, rL + i);
            }
            va[9] = null;
        }
        long sum = va[0].hashInterpreted();
        if (b) {
            va[0] = MyValue1.createWithFieldsDontInline(rI, sum);
        } else {
            va[0] = MyValue1.createWithFieldsDontInline(rI + 1, sum + 1);
        }
        return va;
    }

    @Run(test = "test5")
    public void test5_verifier() {
        MyValue1.ref[] va = test5(true);
        Asserts.assertEQ(va.length, 5);
        Asserts.assertEQ(va[0].hash(), hash(rI, hash()));
        for (int i = 1; i < 4; ++i) {
            Asserts.assertEQ(va[i].hash(), hash());
        }
        Asserts.assertEQ(va[4], null);
        va = test5(false);
        Asserts.assertEQ(va.length, 10);
        Asserts.assertEQ(va[0].hash(), hash(rI + 1, hash(rI, rL) + 1));
        for (int i = 1; i < 9; ++i) {
            Asserts.assertEQ(va[i].hash(), hash(rI + i, rL + i));
        }
        Asserts.assertEQ(va[9], null);
    }

    // Test creation of inline type array with single element
    @Test
    @IR(failOn = {ALLOC, ALLOCA, LOOP, LOAD, STORE, TRAP})
    public MyValue1.ref test6() {
        MyValue1.ref[] va = new MyValue1.ref[1];
        return va[0];
    }

    @Run(test = "test6")
    public void test6_verifier() {
        MyValue1.ref[] va = new MyValue1.ref[1];
        MyValue1.ref v = test6();
        Asserts.assertEQ(v, null);
    }

    // Test default initialization of inline type arrays
    @Test
    @IR(failOn = LOAD)
    public MyValue1.ref[] test7(int len) {
        return new MyValue1.ref[len];
    }

    @Run(test = "test7")
    public void test7_verifier() {
        int len = Math.abs(rI % 10);
        MyValue1.ref[] va = test7(len);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(va[i], null);
            va[i] = null;
        }
    }

    // Test creation of inline type array with zero length
    @Test
    @IR(failOn = {ALLOC, LOAD, STORE, LOOP, TRAP})
    public MyValue1.ref[] test8() {
        return new MyValue1.ref[0];
    }

    @Run(test = "test8")
    public void test8_verifier() {
        MyValue1.ref[] va = test8();
        Asserts.assertEQ(va.length, 0);
    }

    static MyValue1.ref[] test9_va;

    // Test that inline type array loaded from field has correct type
    @Test
    @IR(failOn = LOOP)
    public long test9() {
        return test9_va[0].hash();
    }

    @Run(test = "test9")
    public void test9_verifier() {
        test9_va = new MyValue1.ref[1];
        test9_va[0] = testValue1;
        long result = test9();
        Asserts.assertEQ(result, hash());
    }

    // Multi-dimensional arrays
    @Test
    public MyValue1.ref[][][] test10(int len1, int len2, int len3) {
        MyValue1.ref[][][] arr = new MyValue1.ref[len1][len2][len3];
        for (int i = 0; i < len1; i++) {
            for (int j = 0; j < len2; j++) {
                for (int k = 0; k < len3; k++) {
                    arr[i][j][k] = MyValue1.createWithFieldsDontInline(rI + i , rL + j + k);
                    if (k == 0) {
                        arr[i][j][k] = null;
                    }
                }
            }
        }
        return arr;
    }

    @Run(test = "test10")
    public void test10_verifier() {
        MyValue1.ref[][][] arr = test10(2, 3, 4);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 4; k++) {
                    if (k == 0) {
                        Asserts.assertEQ(arr[i][j][k], null);
                    } else {
                        Asserts.assertEQ(arr[i][j][k].hash(), MyValue1.createWithFieldsDontInline(rI + i , rL + j + k).hash());
                    }
                    arr[i][j][k] = null;
                }
            }
        }
    }

    @Test
    public void test11(MyValue1.ref[][][] arr, long[] res) {
        int l = 0;
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].length; j++) {
                for (int k = 0; k < arr[i][j].length; k++) {
                    if (arr[i][j][k] != null) {
                        res[l] = arr[i][j][k].hash();
                    }
                    arr[i][j][k] = null;
                    l++;
                }
            }
        }
    }

    @Run(test = "test11")
    public void test11_verifier() {
        MyValue1.ref[][][] arr = new MyValue1.ref[2][3][4];
        long[] res = new long[2*3*4];
        long[] verif = new long[2*3*4];
        int l = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 4; k++) {
                    if (j != 2) {
                        arr[i][j][k] = MyValue1.createWithFieldsDontInline(rI + i, rL + j + k);
                        verif[l] = arr[i][j][k].hash();
                    }
                    l++;
                }
            }
        }
        test11(arr, res);
        for (int i = 0; i < verif.length; i++) {
            Asserts.assertEQ(res[i], verif[i]);
        }
    }

    // Array load out of bounds (upper bound) at compile time
    @Test
    public int test12() {
        int arraySize = Math.abs(rI) % 10;
        MyValue1.ref[] va = new MyValue1.ref[arraySize];

        for (int i = 0; i < arraySize; i++) {
            va[i] = MyValue1.createWithFieldsDontInline(rI + 1, rL);
        }

        try {
            return va[arraySize + 1].x;
        } catch (ArrayIndexOutOfBoundsException e) {
            return rI;
        }
    }

    @Run(test = "test12")
    public void test12_verifier() {
        Asserts.assertEQ(test12(), rI);
    }

    // Array load  out of bounds (lower bound) at compile time
    @Test
    public int test13() {
        int arraySize = Math.abs(rI) % 10;
        MyValue1.ref[] va = new MyValue1.ref[arraySize];

        for (int i = 0; i < arraySize; i++) {
            va[i] = MyValue1.createWithFieldsDontInline(rI + i, rL);
        }

        try {
            return va[-arraySize].x;
        } catch (ArrayIndexOutOfBoundsException e) {
            return rI;
        }
    }

    @Run(test = "test13")
    public void test13_verifier() {
        Asserts.assertEQ(test13(), rI);
    }

    // Array load out of bound not known to compiler (both lower and upper bound)
    @Test
    public int test14(MyValue1.ref[] va, int index)  {
        return va[index].x;
    }

    @Run(test = "test14")
    public void test14_verifier() {
        int arraySize = Math.abs(rI) % 10;
        MyValue1.ref[] va = new MyValue1.ref[arraySize];

        for (int i = 0; i < arraySize; i++) {
            va[i] = MyValue1.createWithFieldsDontInline(rI, rL);
        }

        int result;
        for (int i = -20; i < 20; i++) {
            try {
                result = test14(va, i);
            } catch (ArrayIndexOutOfBoundsException e) {
                result = rI;
            }
            Asserts.assertEQ(result, rI);
        }
    }

    // Array store out of bounds (upper bound) at compile time
    @Test
    public int test15() {
        int arraySize = Math.abs(rI) % 10;
        MyValue1.ref[] va = new MyValue1.ref[arraySize];

        try {
            for (int i = 0; i <= arraySize; i++) {
                va[i] = MyValue1.createWithFieldsDontInline(rI + 1, rL);
            }
            return rI - 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            return rI;
        }
    }

    @Run(test = "test15")
    public void test15_verifier() {
        Asserts.assertEQ(test15(), rI);
    }

    // Array store out of bounds (lower bound) at compile time
    @Test
    public int test16() {
        int arraySize = Math.abs(rI) % 10;
        MyValue1.ref[] va = new MyValue1.ref[arraySize];

        try {
            for (int i = -1; i <= arraySize; i++) {
                va[i] = MyValue1.createWithFieldsDontInline(rI + 1, rL);
            }
            return rI - 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            return rI;
        }
    }

    @Run(test = "test16")
    public void test16_verifier() {
        Asserts.assertEQ(test16(), rI);
    }

    // Array store out of bound not known to compiler (both lower and upper bound)
    @Test
    public int test17(MyValue1.ref[] va, int index, MyValue1 vt)  {
        va[index] = vt;
        return va[index].x;
    }

    @Run(test = "test17")
    public void test17_verifier() {
        int arraySize = Math.abs(rI) % 10;
        MyValue1.ref[] va = new MyValue1.ref[arraySize];

        for (int i = 0; i < arraySize; i++) {
            va[i] = MyValue1.createWithFieldsDontInline(rI, rL);
        }

        MyValue1 vt = MyValue1.createWithFieldsDontInline(rI + 1, rL);
        int result;
        for (int i = -20; i < 20; i++) {
            try {
                result = test17(va, i, vt);
            } catch (ArrayIndexOutOfBoundsException e) {
                result = rI + 1;
            }
            Asserts.assertEQ(result, rI + 1);
        }

        for (int i = 0; i < arraySize; i++) {
            Asserts.assertEQ(va[i].x, rI + 1);
        }
    }

    // clone() as stub call
    @Test
    public MyValue1.ref[] test18(MyValue1.ref[] va) {
        return va.clone();
    }

    @Run(test = "test18")
    public void test18_verifier(RunInfo info) {
        int len = Math.abs(rI) % 10;
        MyValue1.ref[] va1 = new MyValue1.ref[len];
        MyValue1[]  va2 = new MyValue1[len];
        for (int i = 1; i < len; ++i) {
            va1[i] = testValue1;
            va2[i] = testValue1;
        }
        MyValue1.ref[] result1 = test18(va1);
        if (len > 0) {
            Asserts.assertEQ(result1[0], null);
        }
        for (int i = 1; i < len; ++i) {
            Asserts.assertEQ(result1[i].hash(), va1[i].hash());
        }
        // make sure we do deopt: GraphKit::new_array assumes an
        // array of references
        for (int j = 0; j < 10; j++) {
            MyValue1.ref[] result2 = test18(va2);

            for (int i = 0; i < len; ++i) {
                Asserts.assertEQ(result2[i].hash(), va2[i].hash());
            }
        }
        if (compile_and_run_again_if_deoptimized(info)) {
            MyValue1.ref[] result2 = test18(va2);
            for (int i = 0; i < len; ++i) {
                Asserts.assertEQ(result2[i].hash(), va2[i].hash());
            }
        }
    }

    // clone() as series of loads/stores
    static MyValue1.ref[] test19_orig = null;

    @Test
    public MyValue1.ref[] test19() {
        MyValue1.ref[] va = new MyValue1.ref[8];
        for (int i = 1; i < va.length; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        test19_orig = va;

        return va.clone();
    }

    @Run(test = "test19")
    public void test19_verifier() {
        MyValue1.ref[] result = test19();
        Asserts.assertEQ(result[0], null);
        for (int i = 1; i < test19_orig.length; ++i) {
            Asserts.assertEQ(result[i].hash(), test19_orig[i].hash());
        }
    }

    // arraycopy() of inline type array with oop fields
    @Test
    public void test20(MyValue1.ref[] src, MyValue1.ref[] dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @Run(test = "test20")
    public void test20_verifier() {
        int len = Math.abs(rI) % 10;
        MyValue1.ref[] src1 = new MyValue1.ref[len];
        MyValue1.ref[] src2 = new MyValue1.ref[len];
        MyValue1[]  src3 = new MyValue1[len];
        MyValue1[]  src4 = new MyValue1[len];
        MyValue1.ref[] dst1 = new MyValue1.ref[len];
        MyValue1[]  dst2 = new MyValue1[len];
        MyValue1.ref[] dst3 = new MyValue1.ref[len];
        MyValue1[]  dst4 = new MyValue1[len];
        if (len > 0) {
            src2[0] = testValue1;
        }
        for (int i = 1; i < len; ++i) {
            src1[i] = testValue1;
            src2[i] = testValue1;
            src3[i] = testValue1;
            src4[i] = testValue1;
        }
        test20(src1, dst1);
        test20(src2, dst2);
        test20(src3, dst3);
        test20(src4, dst4);
        if (len > 0) {
            Asserts.assertEQ(dst1[0], null);
            Asserts.assertEQ(dst2[0].hash(), src2[0].hash());
            Asserts.assertEQ(dst3[0].hash(), src3[0].hash());
            Asserts.assertEQ(dst4[0].hash(), src4[0].hash());
        }
        for (int i = 1; i < len; ++i) {
            Asserts.assertEQ(src1[i].hash(), dst1[i].hash());
            Asserts.assertEQ(src2[i].hash(), dst2[i].hash());
            Asserts.assertEQ(src3[i].hash(), dst3[i].hash());
            Asserts.assertEQ(src4[i].hash(), dst4[i].hash());
        }
    }

    // arraycopy() of inline type array with no oop field
    @Test
    public void test21(MyValue2.ref[] src, MyValue2.ref[] dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @Run(test = "test21")
    public void test21_verifier() {
        int len = Math.abs(rI) % 10;
        MyValue2.ref[] src1 = new MyValue2.ref[len];
        MyValue2.ref[] src2 = new MyValue2.ref[len];
        MyValue2[]  src3 = new MyValue2[len];
        MyValue2[]  src4 = new MyValue2[len];
        MyValue2.ref[] dst1 = new MyValue2.ref[len];
        MyValue2[]  dst2 = new MyValue2[len];
        MyValue2.ref[] dst3 = new MyValue2.ref[len];
        MyValue2[]  dst4 = new MyValue2[len];
        if (len > 0) {
            src2[0] = MyValue2.createWithFieldsInline(rI, rD);
        }
        for (int i = 1; i < len; ++i) {
            src1[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
            src2[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
            src3[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
            src4[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test21(src1, dst1);
        test21(src2, dst2);
        test21(src3, dst3);
        test21(src4, dst4);
        if (len > 0) {
            Asserts.assertEQ(dst1[0], null);
            Asserts.assertEQ(dst2[0].hash(), src2[0].hash());
            Asserts.assertEQ(dst3[0].hash(), src3[0].hash());
            Asserts.assertEQ(dst4[0].hash(), src4[0].hash());
        }
        for (int i = 1; i < len; ++i) {
            Asserts.assertEQ(src1[i].hash(), dst1[i].hash());
            Asserts.assertEQ(src2[i].hash(), dst2[i].hash());
            Asserts.assertEQ(src3[i].hash(), dst3[i].hash());
            Asserts.assertEQ(src4[i].hash(), dst4[i].hash());
        }
    }

    // arraycopy() of inline type array with oop field and tightly
    // coupled allocation as dest
    @Test
    public MyValue1.ref[] test22(MyValue1.ref[] src) {
        MyValue1.ref[] dst = new MyValue1.ref[src.length];
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    @Run(test = "test22")
    public void test22_verifier() {
        int len = Math.abs(rI) % 10;
        MyValue1.ref[] src1 = new MyValue1.ref[len];
        MyValue1[]  src2 = new MyValue1[len];
        for (int i = 1; i < len; ++i) {
            src1[i] = testValue1;
            src2[i] = testValue1;
        }
        MyValue1.ref[] dst1 = test22(src1);
        MyValue1.ref[] dst2 = test22(src2);
        if (len > 0) {
            Asserts.assertEQ(dst1[0], null);
            Asserts.assertEQ(dst2[0].hash(), MyValue1.default.hash());
        }
        for (int i = 1; i < len; ++i) {
            Asserts.assertEQ(src1[i].hash(), dst1[i].hash());
            Asserts.assertEQ(src2[i].hash(), dst2[i].hash());
        }
    }

    // arraycopy() of inline type array with oop fields and tightly
    // coupled allocation as dest
    @Test
    public MyValue1.ref[] test23(MyValue1.ref[] src) {
        MyValue1.ref[] dst = new MyValue1.ref[src.length + 10];
        System.arraycopy(src, 0, dst, 5, src.length);
        return dst;
    }

    @Run(test = "test23")
    public void test23_verifier() {
        int len = Math.abs(rI) % 10;
        MyValue1.ref[] src1 = new MyValue1.ref[len];
        MyValue1[] src2 = new MyValue1[len];
        for (int i = 0; i < len; ++i) {
            src1[i] = testValue1;
            src2[i] = testValue1;
        }
        MyValue1.ref[] dst1 = test23(src1);
        MyValue1.ref[] dst2 = test23(src2);
        for (int i = 0; i < 5; ++i) {
            Asserts.assertEQ(dst1[i], null);
            Asserts.assertEQ(dst2[i], null);
        }
        for (int i = 5; i < len; ++i) {
            Asserts.assertEQ(src1[i].hash(), dst1[i].hash());
            Asserts.assertEQ(src2[i].hash(), dst2[i].hash());
        }
    }

    // arraycopy() of inline type array passed as Object
    @Test
    public void test24(MyValue1.ref[] src, Object dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @Run(test = "test24")
    public void test24_verifier() {
        int len = Math.abs(rI) % 10;
        MyValue1.ref[] src1 = new MyValue1.ref[len];
        MyValue1.ref[] src2 = new MyValue1.ref[len];
        MyValue1[]  src3 = new MyValue1[len];
        MyValue1[]  src4 = new MyValue1[len];
        MyValue1.ref[] dst1 = new MyValue1.ref[len];
        MyValue1[]  dst2 = new MyValue1[len];
        MyValue1.ref[] dst3 = new MyValue1.ref[len];
        MyValue1[]  dst4 = new MyValue1[len];
        if (len > 0) {
            src2[0] = testValue1;
        }
        for (int i = 1; i < len; ++i) {
            src1[i] = testValue1;
            src2[i] = testValue1;
            src3[i] = testValue1;
            src4[i] = testValue1;
        }
        test24(src1, dst1);
        test24(src2, dst2);
        test24(src3, dst3);
        test24(src4, dst4);
        if (len > 0) {
            Asserts.assertEQ(dst1[0], null);
            Asserts.assertEQ(dst2[0].hash(), src2[0].hash());
            Asserts.assertEQ(dst3[0].hash(), src3[0].hash());
            Asserts.assertEQ(dst4[0].hash(), src4[0].hash());
        }
        for (int i = 1; i < len; ++i) {
            Asserts.assertEQ(src1[i].hash(), dst1[i].hash());
            Asserts.assertEQ(src2[i].hash(), dst2[i].hash());
            Asserts.assertEQ(src3[i].hash(), dst3[i].hash());
            Asserts.assertEQ(src4[i].hash(), dst4[i].hash());
        }
    }

    // short arraycopy() with no oop field
    @Test
    public void test25(MyValue2.ref[] src, MyValue2.ref[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @Run(test = "test25")
    public void test25_verifier() {
        MyValue2.ref[] src1 = new MyValue2.ref[8];
        MyValue2.ref[] src2 = new MyValue2.ref[8];
        MyValue2[]  src3 = new MyValue2[8];
        MyValue2[]  src4 = new MyValue2[8];
        MyValue2.ref[] dst1 = new MyValue2.ref[8];
        MyValue2[]  dst2 = new MyValue2[8];
        MyValue2.ref[] dst3 = new MyValue2.ref[8];
        MyValue2[]  dst4 = new MyValue2[8];
        src2[0] = MyValue2.createWithFieldsInline(rI, rD);
        for (int i = 1; i < 8; ++i) {
            src1[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
            src2[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
            src3[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
            src4[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test25(src1, dst1);
        test25(src2, dst2);
        test25(src3, dst3);
        test25(src4, dst4);
        Asserts.assertEQ(dst1[0], null);
        Asserts.assertEQ(dst2[0].hash(), src2[0].hash());
        Asserts.assertEQ(dst3[0].hash(), src3[0].hash());
        Asserts.assertEQ(dst4[0].hash(), src4[0].hash());
        for (int i = 1; i < 8; ++i) {
            Asserts.assertEQ(src1[i].hash(), dst1[i].hash());
            Asserts.assertEQ(src2[i].hash(), dst2[i].hash());
            Asserts.assertEQ(src3[i].hash(), dst3[i].hash());
            Asserts.assertEQ(src4[i].hash(), dst4[i].hash());
        }
    }

    // short arraycopy() with oop fields
    @Test
    public void test26(MyValue1.ref[] src, MyValue1.ref[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @Run(test = "test26")
    public void test26_verifier() {
        MyValue1.ref[] src1 = new MyValue1.ref[8];
        MyValue1.ref[] src2 = new MyValue1.ref[8];
        MyValue1[]  src3 = new MyValue1[8];
        MyValue1[]  src4 = new MyValue1[8];
        MyValue1.ref[] dst1 = new MyValue1.ref[8];
        MyValue1[]  dst2 = new MyValue1[8];
        MyValue1.ref[] dst3 = new MyValue1.ref[8];
        MyValue1[]  dst4 = new MyValue1[8];
        src2[0] = testValue1;
        for (int i = 1; i < 8 ; ++i) {
            src1[i] = testValue1;
            src2[i] = testValue1;
            src3[i] = testValue1;
            src4[i] = testValue1;
        }
        test26(src1, dst1);
        test26(src2, dst2);
        test26(src3, dst3);
        test26(src4, dst4);
        Asserts.assertEQ(dst1[0], null);
        Asserts.assertEQ(dst2[0].hash(), src2[0].hash());
        Asserts.assertEQ(dst3[0].hash(), src3[0].hash());
        Asserts.assertEQ(dst4[0].hash(), src4[0].hash());
        for (int i = 1; i < 8; ++i) {
            Asserts.assertEQ(src1[i].hash(), dst1[i].hash());
            Asserts.assertEQ(src2[i].hash(), dst2[i].hash());
            Asserts.assertEQ(src3[i].hash(), dst3[i].hash());
            Asserts.assertEQ(src4[i].hash(), dst4[i].hash());
        }
    }

    // short arraycopy() with oop fields and offsets
    @Test
    public void test27(MyValue1.ref[] src, MyValue1.ref[] dst) {
        System.arraycopy(src, 1, dst, 2, 6);
    }

    @Run(test = "test27")
    public void test27_verifier() {
        MyValue1.ref[] src1 = new MyValue1.ref[8];
        MyValue1.ref[] src2 = new MyValue1.ref[8];
        MyValue1[]  src3 = new MyValue1[8];
        MyValue1[]  src4 = new MyValue1[8];
        MyValue1.ref[] dst1 = new MyValue1.ref[8];
        MyValue1[]  dst2 = new MyValue1[8];
        MyValue1.ref[] dst3 = new MyValue1.ref[8];
        MyValue1[]  dst4 = new MyValue1[8];
        for (int i = 1; i < 8; ++i) {
            src1[i] = testValue1;
            src2[i] = testValue1;
            src3[i] = testValue1;
            src4[i] = testValue1;
        }
        test27(src1, dst1);
        test27(src2, dst2);
        test27(src3, dst3);
        test27(src4, dst4);
        for (int i = 0; i < 2; ++i) {
            Asserts.assertEQ(dst1[i], null);
            Asserts.assertEQ(dst2[i].hash(), MyValue1.default.hash());
            Asserts.assertEQ(dst3[i], null);
            Asserts.assertEQ(dst4[i].hash(), MyValue1.default.hash());
        }
        for (int i = 2; i < 8; ++i) {
            Asserts.assertEQ(src1[i].hash(), dst1[i].hash());
            Asserts.assertEQ(src2[i].hash(), dst2[i].hash());
            Asserts.assertEQ(src3[i].hash(), dst3[i].hash());
            Asserts.assertEQ(src4[i].hash(), dst4[i].hash());
        }
    }

    // non escaping allocations
    // TODO 8252027: Make sure this is optimized with ZGC
    @Test
    @IR(applyIf = {"UseZGC", "false"},
        failOn = {ALLOC, ALLOCA, LOOP, LOAD, STORE, TRAP})
    public MyValue2.ref test28() {
        MyValue2.ref[] src = new MyValue2.ref[10];
        src[0] = null;
        MyValue2.ref[] dst = (MyValue2.ref[])src.clone();
        return dst[0];
    }

    @Run(test = "test28")
    public void test28_verifier() {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, rD);
        MyValue2.ref result = test28();
        Asserts.assertEQ(result, null);
    }

    // non escaping allocations
    // TODO 8227588: shouldn't this have the same IR matching rules as test6?
    @Test
    @IR(failOn = {ALLOCA, LOOP, TRAP})
    public MyValue2.ref test29(MyValue2.ref[] src) {
        MyValue2.ref[] dst = new MyValue2.ref[10];
        System.arraycopy(src, 0, dst, 0, 10);
        return dst[0];
    }

    @Run(test = "test29")
    public void test29_verifier(RunInfo info) {
        MyValue2.ref[] src1 = new MyValue2.ref[10];
        MyValue2.val[] src2 = new MyValue2.val[10];
        for (int i = 0; i < 10; ++i) {
            src1[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
            src2[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        MyValue2.ref v = test29(src1);
        Asserts.assertEQ(src1[0].hash(), v.hash());
        if (!info.isWarmUp()) {
            v = test29(src2);
            Asserts.assertEQ(src2[0].hash(), v.hash());
        }
    }

    // non escaping allocation with uncommon trap that needs
    // eliminated inline type array element as debug info
    @Test
    public MyValue2.ref test30(MyValue2.ref[] src, boolean flag) {
        MyValue2.ref[] dst = new MyValue2.ref[10];
        System.arraycopy(src, 0, dst, 0, 10);
        if (flag) { }
        return dst[0];
    }

    @Run(test = "test30")
    @Warmup(10000)
    public void test30_verifier(RunInfo info) {
        MyValue2.ref[] src1 = new MyValue2.ref[10];
        MyValue2.val[] src2 = new MyValue2.val[10];
        for (int i = 0; i < 10; ++i) {
            src1[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
            src2[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        MyValue2.ref v = test30(src1, !info.isWarmUp());
        Asserts.assertEQ(src1[0].hash(), v.hash());
        if (!info.isWarmUp()) {
            v = test30(src2, true);
            Asserts.assertEQ(src2[0].hash(), v.hash());
        }
    }

    // non escaping allocation with memory phi
    @Test()
    // TODO 8227588
    // @Test(failOn = ALLOC + ALLOCA + LOOP + LOAD + STORE + TRAP)
    public long test31(boolean b, boolean deopt, Method m) {
        MyValue2.ref[] src = new MyValue2.ref[1];
        if (b) {
            src[0] = MyValue2.createWithFieldsInline(rI, rD);
        } else {
            src[0] = MyValue2.createWithFieldsInline(rI+1, rD+1);
        }
        if (deopt) {
            // uncommon trap
            TestFramework.deoptimize(m);
        }
        return src[0].hash();
    }

    @Run(test = "test31")
    public void test31_verifier(RunInfo info) {
        MyValue2 v1 = MyValue2.createWithFieldsInline(rI, rD);
        long result1 = test31(true, !info.isWarmUp(), info.getTest());
        Asserts.assertEQ(result1, v1.hash());
        MyValue2 v2 = MyValue2.createWithFieldsInline(rI+1, rD+1);
        long result2 = test31(false, !info.isWarmUp(), info.getTest());
        Asserts.assertEQ(result2, v2.hash());
    }

    // Tests with Object arrays and clone/arraycopy
    // clone() as stub call
    @Test
    public Object[] test32(Object[] va) {
        return va.clone();
    }

    @Run(test = "test32")
    public void test32_verifier() {
        int len = Math.abs(rI) % 10;
        MyValue1.ref[] va1 = new MyValue1.ref[len];
        MyValue1[] va2 = new MyValue1[len];
        for (int i = 1; i < len; ++i) {
            va1[i] = testValue1;
            va2[i] = testValue1;
        }
        MyValue1.ref[] result1 = (MyValue1.ref[])test32(va1);
        MyValue1.ref[] result2 = (MyValue1.ref[])test32(va2);
        if (len > 0) {
            Asserts.assertEQ(result1[0], null);
            Asserts.assertEQ(result2[0].hash(), MyValue1.default.hash());
        }
        for (int i = 1; i < len; ++i) {
            Asserts.assertEQ(((MyValue1)result1[i]).hash(), ((MyValue1)va1[i]).hash());
            Asserts.assertEQ(((MyValue1)result2[i]).hash(), ((MyValue1)va2[i]).hash());
        }
    }

    @Test
    public Object[] test33(Object[] va) {
        return va.clone();
    }

    @Run(test = "test33")
    public void test33_verifier() {
        int len = Math.abs(rI) % 10;
        Object[] va = new Object[len];
        for (int i = 0; i < len; ++i) {
            va[i] = testValue1;
        }
        Object[] result = test33(va);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(((MyValue1)result[i]).hash(), ((MyValue1)va[i]).hash());
        }
    }

    // clone() as series of loads/stores
    static Object[] test34_orig = null;

    @ForceInline
    public Object[] test34_helper(boolean flag) {
        Object[] va = null;
        if (flag) {
            va = new MyValue1.ref[8];
            for (int i = 0; i < va.length; ++i) {
                va[i] = MyValue1.createWithFieldsDontInline(rI, rL);
            }
        } else {
            va = new Object[8];
        }
        return va;
    }

    @Test
    public Object[] test34(boolean flag) {
        Object[] va = test34_helper(flag);
        test34_orig = va;
        return va.clone();
    }

    @Run(test = "test34")
    public void test34_verifier(RunInfo info) {
        test34(false);
        for (int i = 0; i < 10; i++) { // make sure we do deopt
            Object[] result = test34(true);
            verify(test34_orig, result);
        }
        if (compile_and_run_again_if_deoptimized(info)) {
            Object[] result = test34(true);
            verify(test34_orig, result);
        }
    }

    static void verify(Object[] src, Object[] dst) {
        for (int i = 0; i < src.length; ++i) {
            if (src[i] != null) {
                Asserts.assertEQ(((MyInterface)src[i]).hash(), ((MyInterface)dst[i]).hash());
            } else {
                Asserts.assertEQ(dst[i], null);
            }
        }
    }

    static void verify(MyValue1.ref[] src, MyValue1.ref[] dst) {
        for (int i = 0; i < src.length; ++i) {
            if (src[i] != null) {
                Asserts.assertEQ(src[i].hash(), dst[i].hash());
            } else {
                Asserts.assertEQ(dst[i], null);
            }
        }
    }

    static void verify(MyValue1.ref[] src, Object[] dst) {
        for (int i = 0; i < src.length; ++i) {
            if (src[i] != null) {
                Asserts.assertEQ(src[i].hash(), ((MyInterface)dst[i]).hash());
            } else {
                Asserts.assertEQ(dst[i], null);
            }
        }
    }

    static void verify(MyValue2.ref[] src, MyValue2.ref[] dst) {
        for (int i = 0; i < src.length; ++i) {
            if (src[i] != null) {
                Asserts.assertEQ(src[i].hash(), dst[i].hash());
            } else {
                Asserts.assertEQ(dst[i], null);
            }
        }
    }

    static void verify(MyValue2.ref[] src, Object[] dst) {
        for (int i = 0; i < src.length; ++i) {
            if (src[i] != null) {
                Asserts.assertEQ(src[i].hash(), ((MyInterface)dst[i]).hash());
            } else {
                Asserts.assertEQ(dst[i], null);
            }
        }
    }

    static boolean compile_and_run_again_if_deoptimized(RunInfo info) {
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            if (TestFramework.isCompiled(m)) {
                TestFramework.compile(m, CompLevel.C2);
            }
        }
        return false;
    }

    // arraycopy() of inline type array of unknown size
    @Test
    public void test35(Object src, Object dst, int len) {
        System.arraycopy(src, 0, dst, 0, len);
    }

    @Run(test = "test35")
    public void test35_verifier(RunInfo info) {
        int len = Math.abs(rI) % 10;
        MyValue1.ref[] src = new MyValue1.ref[len];
        MyValue1.ref[] dst = new MyValue1.ref[len];
        for (int i = 1; i < len; ++i) {
            src[i] = testValue1;
        }
        test35(src, dst, src.length);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(info)) {
            test35(src, dst, src.length);
            verify(src, dst);
        }
    }

    @Test
    public void test36(Object src, MyValue2.ref[] dst) {
        System.arraycopy(src, 0, dst, 0, dst.length);
    }

    @Run(test = "test36")
    public void test36_verifier(RunInfo info) {
        int len = Math.abs(rI) % 10;
        MyValue2.ref[] src = new MyValue2.ref[len];
        MyValue2.ref[] dst = new MyValue2.ref[len];
        for (int i = 1; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test36(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(info)) {
            test36(src, dst);
            verify(src, dst);
        }
    }

    @Test
    public void test37(MyValue2.ref[] src, Object dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @Run(test = "test37")
    public void test37_verifier(RunInfo info) {
        int len = Math.abs(rI) % 10;
        MyValue2.ref[] src = new MyValue2.ref[len];
        MyValue2.ref[] dst = new MyValue2.ref[len];
        for (int i = 1; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test37(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(info)) {
            test37(src, dst);
            verify(src, dst);
        }
    }

    @Test
    public void test38(Object src, MyValue2.ref[] dst) {
        System.arraycopy(src, 0, dst, 0, dst.length);
    }

    @Run(test = "test38")
    @Warmup(1) // Avoid early compilation
    public void test38_verifier(RunInfo info) {
        int len = Math.abs(rI) % 10;
        Object[] src = new Object[len];
        MyValue2.ref[] dst = new MyValue2.ref[len];
        for (int i = 1; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test38(src, dst);
        verify(dst, src);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertDeoptimizedByC2(m);
            TestFramework.compile(m, CompLevel.C2);
            test38(src, dst);
            verify(dst, src);
            TestFramework.assertCompiled(m);
        }
    }

    @Test
    public void test39(MyValue2.ref[] src, Object dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @Run(test = "test39")
    public void test39_verifier(RunInfo info) {
        int len = Math.abs(rI) % 10;
        MyValue2.ref[] src = new MyValue2.ref[len];
        Object[] dst = new Object[len];
        for (int i = 1; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test39(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(info)) {
            test39(src, dst);
            verify(src, dst);
        }
    }

    @Test
    public void test40(Object[] src, Object dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @Run(test = "test40")
    @Warmup(1) // Avoid early compilation
    public void test40_verifier(RunInfo info) {
        int len = Math.abs(rI) % 10;
        Object[] src = new Object[len];
        MyValue2.ref[] dst = new MyValue2.ref[len];
        for (int i = 1; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test40(src, dst);
        verify(dst, src);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertDeoptimizedByC2(m);
            TestFramework.compile(m, CompLevel.C2);
            test40(src, dst);
            verify(dst, src);
            TestFramework.assertCompiled(m);
        }
    }

    @Test
    public void test41(Object src, Object[] dst) {
        System.arraycopy(src, 0, dst, 0, dst.length);
    }

    @Run(test = "test41")
    public void test41_verifier(RunInfo info) {
        int len = Math.abs(rI) % 10;
        MyValue2.ref[] src = new MyValue2.ref[len];
        Object[] dst = new Object[len];
        for (int i = 1; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test41(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(info)) {
            test41(src, dst);
            verify(src, dst);
        }
    }

    @Test
    public void test42(Object[] src, Object[] dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @Run(test = "test42")
    public void test42_verifier(RunInfo info) {
        int len = Math.abs(rI) % 10;
        Object[] src = new Object[len];
        Object[] dst = new Object[len];
        for (int i = 1; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test42(src, dst);
        verify(src, dst);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertCompiled(m);
        }
    }

    // short arraycopy()'s
    @Test
    public void test43(Object src, Object dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @Run(test = "test43")
    public void test43_verifier(RunInfo info) {
        MyValue1.ref[] src = new MyValue1.ref[8];
        MyValue1.ref[] dst = new MyValue1.ref[8];
        for (int i = 1; i < 8; ++i) {
            src[i] = testValue1;
        }
        test43(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(info)) {
            test43(src, dst);
            verify(src, dst);
        }
    }

    @Test
    public void test44(Object src, MyValue2.ref[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @Run(test = "test44")
    public void test44_verifier(RunInfo info) {
        MyValue2.ref[] src = new MyValue2.ref[8];
        MyValue2.ref[] dst = new MyValue2.ref[8];
        for (int i = 1; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test44(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(info)) {
            test44(src, dst);
            verify(src, dst);
        }
    }

    @Test
    public void test45(MyValue2.ref[] src, Object dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @Run(test = "test45")
    public void test45_verifier(RunInfo info) {
        MyValue2.ref[] src = new MyValue2.ref[8];
        MyValue2.ref[] dst = new MyValue2.ref[8];
        for (int i = 1; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test45(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(info)) {
            test45(src, dst);
            verify(src, dst);
        }
    }

    @Test
    public void test46(Object[] src, MyValue2.ref[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @Run(test = "test46")
    @Warmup(1) // Avoid early compilation
    public void test46_verifier(RunInfo info) {
        Object[] src = new Object[8];
        MyValue2.ref[] dst = new MyValue2.ref[8];
        for (int i = 1; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test46(src, dst);
        verify(dst, src);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertDeoptimizedByC2(m);
            TestFramework.compile(m, CompLevel.C2);
            test46(src, dst);
            verify(dst, src);
            TestFramework.assertCompiled(m);
        }
    }

    @Test
    public void test47(MyValue2.ref[] src, Object[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @Run(test = "test47")
    public void test47_verifier(RunInfo info) {
        MyValue2.ref[] src = new MyValue2.ref[8];
        Object[] dst = new Object[8];
        for (int i = 1; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test47(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(info)) {
            test47(src, dst);
            verify(src, dst);
        }
    }

    @Test
    public void test48(Object[] src, Object dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @Run(test = "test48")
    @Warmup(1) // Avoid early compilation
    public void test48_verifier(RunInfo info) {
        Object[] src = new Object[8];
        MyValue2.ref[] dst = new MyValue2.ref[8];
        for (int i = 1; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test48(src, dst);
        verify(dst, src);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertDeoptimizedByC2(m);
            TestFramework.compile(m, CompLevel.C2);
            test48(src, dst);
            verify(dst, src);
            TestFramework.assertCompiled(m);
        }
    }

    @Test
    public void test49(Object src, Object[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @Run(test = "test49")
    public void test49_verifier(RunInfo info) {
        MyValue2.ref[] src = new MyValue2.ref[8];
        Object[] dst = new Object[8];
        for (int i = 1; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test49(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(info)) {
            test49(src, dst);
            verify(src, dst);
        }
    }

    @Test
    public void test50(Object[] src, Object[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @Run(test = "test50")
    public void test50_verifier(RunInfo info) {
        Object[] src = new Object[8];
        Object[] dst = new Object[8];
        for (int i = 1; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test50(src, dst);
        verify(src, dst);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertCompiled(m);
        }
    }

    @Test
    public MyValue1.ref[] test51(MyValue1.ref[] va) {
        return Arrays.copyOf(va, va.length, MyValue1.ref[].class);
    }

    @Run(test = "test51")
    public void test51_verifier() {
        int len = Math.abs(rI) % 10;
        MyValue1.ref[] va = new MyValue1.ref[len];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
        }
        MyValue1.ref[] result = test51(va);
        verify(va, result);
    }

    static final MyValue1.ref[] test52_va = new MyValue1.ref[8];

    @Test
    public MyValue1.ref[] test52() {
        return Arrays.copyOf(test52_va, 8, MyValue1.ref[].class);
    }

    @Run(test = "test52")
    public void test52_verifier() {
        for (int i = 1; i < 8; ++i) {
            test52_va[i] = testValue1;
        }
        MyValue1.ref[] result = test52();
        verify(test52_va, result);
    }

    @Test
    public MyValue1.ref[] test53(Object[] va) {
        return Arrays.copyOf(va, va.length, MyValue1.ref[].class);
    }

    @Run(test = "test53")
    public void test53_verifier() {
        int len = Math.abs(rI) % 10;
        MyValue1.ref[] va = new MyValue1.ref[len];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
        }
        MyValue1.ref[] result = test53(va);
        verify(result, va);
    }

    @Test
    public Object[] test54(MyValue1.ref[] va) {
        return Arrays.copyOf(va, va.length, Object[].class);
    }

    @Run(test = "test54")
    public void test54_verifier() {
        int len = Math.abs(rI) % 10;
        MyValue1.ref[] va = new MyValue1.ref[len];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
        }
        Object[] result = test54(va);
        verify(va, result);
    }

    @Test
    public Object[] test55(Object[] va) {
        return Arrays.copyOf(va, va.length, Object[].class);
    }

    @Run(test = "test55")
    public void test55_verifier() {
        int len = Math.abs(rI) % 10;
        MyValue1.ref[] va = new MyValue1.ref[len];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
        }
        Object[] result = test55(va);
        verify(va, result);
    }

    @Test
    public MyValue1.ref[] test56(Object[] va) {
        return Arrays.copyOf(va, va.length, MyValue1.ref[].class);
    }

    @Run(test = "test56")
    public void test56_verifier() {
        int len = Math.abs(rI) % 10;
        Object[] va = new Object[len];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
        }
        MyValue1.ref[] result = test56(va);
        verify(result, va);
    }

   @Test
    public Object[] test57(Object[] va, Class klass) {
        return Arrays.copyOf(va, va.length, klass);
    }

    @Run(test = "test57")
    public void test57_verifier() {
        int len = Math.abs(rI) % 10;
        Object[] va = new MyValue1.ref[len];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
        }
        Object[] result = test57(va, MyValue1.ref[].class);
        verify(va, result);
    }

    @Test
    public Object[] test58(MyValue1.ref[] va, Class klass) {
        return Arrays.copyOf(va, va.length, klass);
    }

    @Run(test = "test58")
    public void test58_verifier(RunInfo info) {
        int len = Math.abs(rI) % 10;
        MyValue1.ref[] va = new MyValue1.ref[len];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
        }
        for (int i = 1; i < 10; i++) {
            Object[] result = test58(va, MyValue1.ref[].class);
            verify(va, result);
        }
        if (compile_and_run_again_if_deoptimized(info)) {
            Object[] result = test58(va, MyValue1.ref[].class);
            verify(va, result);
        }
    }

    @Test
    public Object[] test59(MyValue1.ref[] va) {
        return Arrays.copyOf(va, va.length+1, MyValue1.ref[].class);
    }

    @Run(test = "test59")
    public void test59_verifier() {
        int len = Math.abs(rI) % 10;
        MyValue1.ref[] va = new MyValue1.ref[len];
        MyValue1.ref[] verif = new MyValue1.ref[len+1];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
            verif[i] = va[i];
        }
        Object[] result = test59(va);
        verify(verif, result);
    }

    @Test
    public Object[] test60(Object[] va, Class klass) {
        return Arrays.copyOf(va, va.length+1, klass);
    }

    @Run(test = "test60")
    public void test60_verifier() {
        int len = Math.abs(rI) % 10;
        MyValue1.ref[] va = new MyValue1.ref[len];
        MyValue1.ref[] verif = new MyValue1.ref[len+1];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
            verif[i] = (MyValue1)va[i];
        }
        Object[] result = test60(va, MyValue1.ref[].class);
        verify(verif, result);
    }

    @Test
    public Object[] test61(Object[] va, Class klass) {
        return Arrays.copyOf(va, va.length+1, klass);
    }

    @Run(test = "test61")
    public void test61_verifier() {
        int len = Math.abs(rI) % 10;
        Object[] va = new Integer[len];
        for (int i = 1; i < len; ++i) {
            va[i] = Integer.valueOf(rI);
        }
        Object[] result = test61(va, Integer[].class);
        for (int i = 0; i < va.length; ++i) {
            Asserts.assertEQ(va[i], result[i]);
        }
    }

    @ForceInline
    public Object[] test62_helper(int i, MyValue1.ref[] va, Integer[] oa) {
        Object[] arr = null;
        if (i == 10) {
            arr = oa;
        } else {
            arr = va;
        }
        return arr;
    }

    @Test
    public Object[] test62(MyValue1.ref[] va, Integer[] oa) {
        int i = 0;
        for (; i < 10; i++);

        Object[] arr = test62_helper(i, va, oa);

        return Arrays.copyOf(arr, arr.length+1, arr.getClass());
    }

    @Run(test = "test62")
    public void test62_verifier() {
        int len = Math.abs(rI) % 10;
        MyValue1.ref[] va = new MyValue1.ref[len];
        Integer[] oa = new Integer[len];
        for (int i = 1; i < len; ++i) {
            oa[i] = Integer.valueOf(rI);
        }
        test62_helper(42, va, oa);
        Object[] result = test62(va, oa);
        for (int i = 0; i < va.length; ++i) {
            Asserts.assertEQ(oa[i], result[i]);
        }
    }

    @ForceInline
    public Object[] test63_helper(int i, MyValue1.ref[] va, Integer[] oa) {
        Object[] arr = null;
        if (i == 10) {
            arr = va;
        } else {
            arr = oa;
        }
        return arr;
    }

    @Test
    public Object[] test63(MyValue1.ref[] va, Integer[] oa) {
        int i = 0;
        for (; i < 10; i++);

        Object[] arr = test63_helper(i, va, oa);

        return Arrays.copyOf(arr, arr.length+1, arr.getClass());
    }

    @Run(test = "test63")
    public void test63_verifier() {
        int len = Math.abs(rI) % 10;
        MyValue1.ref[] va = new MyValue1.ref[len];
        MyValue1.ref[] verif = new MyValue1.ref[len+1];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
            verif[i] = va[i];
        }
        Integer[] oa = new Integer[len];
        test63_helper(42, va, oa);
        Object[] result = test63(va, oa);
        verify(verif, result);
    }

    // Test default initialization of inline type arrays: small array
    @Test
    public MyValue1.ref[] test64() {
        return new MyValue1.ref[8];
    }

    @Run(test = "test64")
    public void test64_verifier() {
        MyValue1.ref[] va = test64();
        for (int i = 0; i < 8; ++i) {
            Asserts.assertEQ(va[i], null);
        }
    }

    // Test default initialization of inline type arrays: large array
    @Test
    public MyValue1.ref[] test65() {
        return new MyValue1.ref[32];
    }

    @Run(test = "test65")
    public void test65_verifier() {
        MyValue1.ref[] va = test65();
        for (int i = 0; i < 32; ++i) {
            Asserts.assertEQ(va[i], null);
        }
    }

    // Check init store elimination
    @Test
    @IR(counts = {ALLOCA, "= 1"})
    public MyValue1.ref[] test66(MyValue1.ref vt) {
        MyValue1.ref[] va = new MyValue1.ref[1];
        va[0] = vt;
        return va;
    }

    @Run(test = "test66")
    public void test66_verifier() {
        MyValue1.ref vt = MyValue1.createWithFieldsDontInline(rI, rL);
        MyValue1.ref[] va = test66(vt);
        Asserts.assertEQ(va[0].hashPrimitive(), vt.hashPrimitive());
    }

    // Zeroing elimination and arraycopy
    @Test
    public MyValue1.ref[] test67(MyValue1.ref[] src) {
        MyValue1.ref[] dst = new MyValue1.ref[16];
        System.arraycopy(src, 0, dst, 0, 13);
        return dst;
    }

    @Run(test = "test67")
    public void test67_verifier() {
        MyValue1.ref[] va = new MyValue1.ref[16];
        MyValue1.ref[] var = test67(va);
        for (int i = 0; i < 16; ++i) {
            Asserts.assertEQ(var[i], null);
        }
    }

    // A store with a default value can be eliminated
    @Test
    public MyValue1.ref[] test68() {
        MyValue1.ref[] va = new MyValue1.ref[2];
        va[0] = va[1];
        return va;
    }

    @Run(test = "test68")
    public void test68_verifier() {
        MyValue1.ref[] va = test68();
        for (int i = 0; i < 2; ++i) {
            Asserts.assertEQ(va[i], null);
        }
    }

    // Requires individual stores to init array
    @Test
    public MyValue1.ref[] test69(MyValue1.ref vt) {
        MyValue1.ref[] va = new MyValue1.ref[4];
        va[0] = vt;
        va[3] = vt;
        return va;
    }

    @Run(test = "test69")
    public void test69_verifier() {
        MyValue1.ref vt = MyValue1.createWithFieldsDontInline(rI, rL);
        MyValue1.ref[] va = new MyValue1.ref[4];
        va[0] = vt;
        va[3] = vt;
        MyValue1.ref[] var = test69(vt);
        for (int i = 0; i < va.length; ++i) {
            Asserts.assertEQ(va[i], var[i]);
        }
    }

    // A store with a default value can be eliminated: same as test68
    // but store is farther away from allocation
    @Test
    public MyValue1.ref[] test70(MyValue1.ref[] other) {
        other[1] = other[0];
        MyValue1.ref[] va = new MyValue1.ref[2];
        other[0] = va[1];
        va[0] = va[1];
        return va;
    }

    @Run(test = "test70")
    public void test70_verifier() {
        MyValue1.ref[] va = new MyValue1.ref[2];
        MyValue1.ref[] var = test70(va);
        for (int i = 0; i < 2; ++i) {
            Asserts.assertEQ(va[i], var[i]);
        }
    }

    // EA needs to consider oop fields in flattened arrays
    @Test
    public void test71() {
        int len = 10;
        MyValue2.ref[] src = new MyValue2.ref[len];
        MyValue2.ref[] dst = new MyValue2.ref[len];
        for (int i = 1; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsDontInline(rI+i, rD+i);
        }
        System.arraycopy(src, 0, dst, 0, src.length);
        for (int i = 0; i < len; ++i) {
            if (src[i] == null) {
                Asserts.assertEQ(dst[i], null);
            } else {
                Asserts.assertEQ(src[i].hash(), dst[i].hash());
            }
        }
    }

    @Run(test = "test71")
    public void test71_verifier() {
        test71();
    }

    // Test EA with leaf call to 'store_unknown_value'
    @Test
    public void test72(Object[] o, boolean b, Object element) {
        Object[] arr1 = new Object[10];
        Object[] arr2 = new Object[10];
        if (b) {
            arr1 = o;
        }
        arr1[0] = element;
        arr2[0] = element;
    }

    @Run(test = "test72")
    public void test72_verifier() {
        Object[] arr = new Object[1];
        Object elem = new Object();
        test72(arr, true, elem);
        test72(arr, false, elem);
    }

    @Test
    public void test73(Object[] oa, MyValue1.ref v, Object o) {
        // TestLWorld.test38 use a C1 Phi node for the array. This test
        // adds the case where the stored value is a C1 Phi node.
        Object o2 = (o == null) ? v : o;
        oa[0] = v;  // The stored value is known to be flattenable
        oa[1] = o;  // The stored value may be flattenable
        oa[2] = o2; // The stored value may be flattenable (a C1 Phi node)
        oa[0] = oa; // The stored value is known to be not flattenable (an Object[])
    }

    @Run(test = "test73")
    public void test73_verifier() {
        MyValue1.ref v0 = MyValue1.createWithFieldsDontInline(rI, rL);
        MyValue1.ref v1 = MyValue1.createWithFieldsDontInline(rI+1, rL+1);
        MyValue1.ref[] arr = new MyValue1.ref[3];
        try {
            test73(arr, v0, v1);
            throw new RuntimeException("ArrayStoreException expected");
        } catch (ArrayStoreException t) {
            // expected
        }
        Asserts.assertEQ(arr[0].hash(), v0.hash());
        Asserts.assertEQ(arr[1].hash(), v1.hash());
        Asserts.assertEQ(arr[2].hash(), v1.hash());
    }

    // Some more array clone tests
    @ForceInline
    public Object[] test74_helper(int i, MyValue1.ref[] va, Integer[] oa) {
        Object[] arr = null;
        if (i == 10) {
            arr = oa;
        } else {
            arr = va;
        }
        return arr;
    }

    @Test
    public Object[] test74(MyValue1.ref[] va, Integer[] oa) {
        int i = 0;
        for (; i < 10; i++);

        Object[] arr = test74_helper(i, va, oa);
        return arr.clone();
    }

    @Run(test = "test74")
    public void test74_verifier() {
        int len = Math.abs(rI) % 10;
        MyValue1.ref[] va = new MyValue1.ref[len];
        Integer[] oa = new Integer[len];
        for (int i = 1; i < len; ++i) {
            oa[i] = Integer.valueOf(rI);
        }
        test74_helper(42, va, oa);
        Object[] result = test74(va, oa);

        for (int i = 0; i < va.length; ++i) {
            Asserts.assertEQ(oa[i], result[i]);
            // Check that array has correct properties (null-ok)
            result[i] = null;
        }
    }

    @ForceInline
    public Object[] test75_helper(int i, MyValue1.ref[] va, Integer[] oa) {
        Object[] arr = null;
        if (i == 10) {
            arr = va;
        } else {
            arr = oa;
        }
        return arr;
    }

    @Test
    public Object[] test75(MyValue1.ref[] va, Integer[] oa) {
        int i = 0;
        for (; i < 10; i++);

        Object[] arr = test75_helper(i, va, oa);
        return arr.clone();
    }

    @Run(test = "test75")
    public void test75_verifier() {
        int len = Math.abs(rI) % 10;
        MyValue1.ref[] va = new MyValue1.ref[len];
        MyValue1.ref[] verif = new MyValue1.ref[len];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
            verif[i] = va[i];
        }
        Integer[] oa = new Integer[len];
        test75_helper(42, va, oa);
        Object[] result = test75(va, oa);
        verify(verif, result);
        if (len > 0) {
            // Check that array has correct properties (null-ok)
            result[0] = null;
        }
    }

    // Test mixing nullable and non-nullable arrays
    @Test
    public Object[] test76(MyValue1[] vva, MyValue1.ref[] vba, MyValue1 vt, Object[] out, int n) {
        Object[] result = null;
        if (n == 0) {
            result = vva;
        } else if (n == 1) {
            result = vba;
        } else if (n == 2) {
            result = new MyValue1[42];
        } else if (n == 3) {
            result = new MyValue1.ref[42];
        }
        result[0] = vt;
        out[0] = result[1];
        return result;
    }

    @Run(test = "test76")
    public void test76_verifier() {
        MyValue1 vt = testValue1;
        Object[] out = new Object[1];
        MyValue1[] vva = new MyValue1[42];
        MyValue1[] vva_r = new MyValue1[42];
        vva_r[0] = vt;
        MyValue1.ref[] vba = new MyValue1.ref[42];
        MyValue1.ref[] vba_r = new MyValue1.ref[42];
        vba_r[0] = vt;
        Object[] result = test76(vva, vba, vt, out, 0);
        verify(result, vva_r);
        Asserts.assertEQ(out[0], vva_r[1]);
        result = test76(vva, vba, vt, out, 1);
        verify(result, vba_r);
        Asserts.assertEQ(out[0], vba_r[1]);
        result = test76(vva, vba, vt, out, 2);
        verify(result, vva_r);
        Asserts.assertEQ(out[0], vva_r[1]);
        result = test76(vva, vba, vt, out, 3);
        verify(result, vba_r);
        Asserts.assertEQ(out[0], vba_r[1]);
    }

    @Test
    public Object[] test77(boolean b) {
        Object[] va;
        if (b) {
            va = new MyValue1.ref[5];
            for (int i = 0; i < 5; ++i) {
                va[i] = testValue1;
            }
        } else {
            va = new MyValue1[10];
            for (int i = 0; i < 10; ++i) {
                va[i] = MyValue1.createWithFieldsInline(rI + i, rL + i);
            }
        }
        long sum = ((MyValue1)va[0]).hashInterpreted();
        if (b) {
            va[0] = MyValue1.createWithFieldsDontInline(rI, sum);
        } else {
            va[0] = MyValue1.createWithFieldsDontInline(rI + 1, sum + 1);
        }
        return va;
    }

    @Run(test = "test77")
    public void test77_verifier() {
        Object[] va = test77(true);
        Asserts.assertEQ(va.length, 5);
        Asserts.assertEQ(((MyValue1)va[0]).hash(), hash(rI, hash()));
        for (int i = 1; i < 5; ++i) {
            Asserts.assertEQ(((MyValue1)va[i]).hash(), hash());
        }
        va = test77(false);
        Asserts.assertEQ(va.length, 10);
        Asserts.assertEQ(((MyValue1)va[0]).hash(), hash(rI + 1, hash(rI, rL) + 1));
        for (int i = 1; i < 10; ++i) {
            Asserts.assertEQ(((MyValue1)va[i]).hash(), hash(rI + i, rL + i));
        }
    }

    // Same as test76 but with non inline type array cases
    @Test
    public Object[] test78(MyValue1[] vva, MyValue1.ref[] vba, Object val, Object[] out, int n) {
        Object[] result = null;
        if (n == 0) {
            result = vva;
        } else if (n == 1) {
            result = vba;
        } else if (n == 2) {
            result = new MyValue1[42];
        } else if (n == 3) {
            result = new MyValue1.ref[42];
        } else if (n == 4) {
            result = new Integer[42];
        }
        result[0] = val;
        out[0] = result[1];
        return result;
    }

    @Run(test = "test78")
    public void test78_verifier() {
        MyValue1 vt = testValue1;
        Integer i = Integer.valueOf(42);
        Object[] out = new Object[1];
        MyValue1[] vva = new MyValue1[42];
        MyValue1[] vva_r = new MyValue1[42];
        vva_r[0] = vt;
        MyValue1.ref[] vba = new MyValue1.ref[42];
        MyValue1.ref[] vba_r = new MyValue1.ref[42];
        vba_r[0] = vt;
        Object[] result = test78(vva, vba, vt, out, 0);
        verify(result, vva_r);
        Asserts.assertEQ(out[0], vva_r[1]);
        result = test78(vva, vba, vt, out, 1);
        verify(result, vba_r);
        Asserts.assertEQ(out[0], vba_r[1]);
        result = test78(vva, vba, vt, out, 2);
        verify(result, vva_r);
        Asserts.assertEQ(out[0], vva_r[1]);
        result = test78(vva, vba, vt, out, 3);
        verify(result, vba_r);
        Asserts.assertEQ(out[0], vba_r[1]);
        result = test78(vva, vba, i, out, 4);
        Asserts.assertEQ(result[0], i);
        Asserts.assertEQ(out[0], null);
    }

    // Test widening conversions from [Q to [L
    @Test
    @IR(failOn = {ALLOC, ALLOCA, LOOP, LOAD, STORE, TRAP})
    public static MyValue1.ref[] test79(MyValue1[] va) {
        return va;
    }

    @Run(test = "test79")
    public void test79_verifier() {
        MyValue1[] va = new MyValue1[1];
        va[0] = testValue1;
        MyValue1.ref[] res = test79(va);
        Asserts.assertEquals(res[0].hash(), testValue1.hash());
        try {
            res[0] = null;
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException npe) {
            // Expected
        }
        res[0] = testValue1;
        test79(null); // Should not throw NPE
    }

    // Same as test79 but with explicit cast and Object return
    @Test
    @IR(failOn = {ALLOC, ALLOCA, LOOP, LOAD, STORE, TRAP})
    public static Object[] test80(MyValue1[] va) {
        return (MyValue1.ref[])va;
    }

    @Run(test = "test80")
    public void test80_verifier() {
        MyValue1[] va = new MyValue1[1];
        va[0] = testValue1;
        Object[] res = test80(va);
        Asserts.assertEquals(((MyValue1)res[0]).hash(), testValue1.hash());
        try {
            res[0] = null;
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException npe) {
            // Expected
        }
        res[0] = testValue1;
        test80(null); // Should not throw NPE
    }

    // Test mixing widened and boxed array type
    @Test()
    public static long test81(MyValue1[] va1, MyValue1.ref[] va2, MyValue1 vt, boolean b, boolean shouldThrow) {
        MyValue1.ref[] result = b ? va1 : va2;
        try {
            result[0] = vt;
        } catch (NullPointerException npe) {
            // Ignored
        }
        return result[1].hash();
    }

    @Run(test = "test81")
    public void test81_verifier() {
        MyValue1[] va = new MyValue1[2];
        MyValue1.ref[] vaB = new MyValue1.ref[2];
        va[1] = testValue1;
        vaB[1] = testValue1;
        long res = test81(va, vaB, testValue1, true, true);
        Asserts.assertEquals(va[0].hash(), testValue1.hash());
        Asserts.assertEquals(res, testValue1.hash());
        res = test81(va, vaB, testValue1, false, false);
        Asserts.assertEquals(vaB[0].hash(), testValue1.hash());
        Asserts.assertEquals(res, testValue1.hash());
        res = test81(va, va, testValue1, false, true);
        Asserts.assertEquals(va[0].hash(), testValue1.hash());
        Asserts.assertEquals(res, testValue1.hash());
    }

    // Same as test81 but more cases and null writes
    @Test()
    public static long test82(MyValue1[] va1, MyValue1.ref[] va2, MyValue1 vt1, MyValue1.ref vt2, int i, boolean shouldThrow) {
        MyValue1.ref[] result = null;
        if (i == 0) {
            result = va1;
        } else if (i == 1) {
            result = va2;
        } else if (i == 2) {
            result = new MyValue1.ref[2];
            result[1] = vt1;
        } else if (i == 3) {
            result = new MyValue1[2];
            result[1] = vt1;
        }
        try {
            result[0] = (i <= 1) ? null : vt2;
            if (shouldThrow) {
                throw new RuntimeException("NullPointerException expected");
            }
        } catch (NullPointerException npe) {
            Asserts.assertTrue(shouldThrow, "NullPointerException thrown");
        }
        result[0] = vt1;
        return result[1].hash();
    }

    @Run(test = "test82")
    public void test82_verifier() {
        MyValue1[] va = new MyValue1[2];
        MyValue1.ref[] vaB = new MyValue1.ref[2];
        va[1] = testValue1;
        vaB[1] = testValue1;
        long res = test82(va, vaB, testValue1, testValue1, 0, true);
        Asserts.assertEquals(va[0].hash(), testValue1.hash());
        Asserts.assertEquals(res, testValue1.hash());
        res = test82(va, vaB, testValue1, testValue1, 1, false);
        Asserts.assertEquals(vaB[0].hash(), testValue1.hash());
        Asserts.assertEquals(res, testValue1.hash());
        res = test82(va, va, testValue1, testValue1, 1, true);
        Asserts.assertEquals(va[0].hash(), testValue1.hash());
        Asserts.assertEquals(res, testValue1.hash());
        res = test82(va, va, testValue1, null, 2, false);
        Asserts.assertEquals(va[0].hash(), testValue1.hash());
        Asserts.assertEquals(res, testValue1.hash());
        res = test82(va, va, testValue1, null, 3, true);
        Asserts.assertEquals(va[0].hash(), testValue1.hash());
        Asserts.assertEquals(res, testValue1.hash());
    }

    @Test
    @IR(failOn = {ALLOC, ALLOCA, STORE})
    public static long test83(MyValue1[] va) {
        MyValue1.ref[] result = va;
        return result[0].hash();
    }

    @Run(test = "test83")
    public void test83_verifier() {
        MyValue1[] va = new MyValue1[42];
        va[0] = testValue1;
        long res = test83(va);
        Asserts.assertEquals(res, testValue1.hash());
    }

    @Test
    @IR(applyIf = {"FlatArrayElementMaxSize", "= -1"},
        failOn = {ALLOC, LOOP, STORE, TRAP})
    public static MyValue1.ref[] test84(MyValue1 vt1, MyValue1.ref vt2) {
        MyValue1.ref[] result = new MyValue1[2];
        result[0] = vt1;
        result[1] = vt2;
        return result;
    }

    @Run(test = "test84")
    public void test84_verifier() {
        MyValue1.ref[] res = test84(testValue1, testValue1);
        Asserts.assertEquals(res[0].hash(), testValue1.hash());
        Asserts.assertEquals(res[1].hash(), testValue1.hash());
        try {
            test84(testValue1, null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException npe) {
            // Expected
        }
    }

    @Test()
    public static long test85(MyValue1.ref[] va, MyValue1 val) {
        va[0] = val;
        return va[1].hash();
    }

    @Run(test = "test85")
    public void test85_verifier() {
        MyValue1[] va = new MyValue1[2];
        MyValue1.ref[] vab = new MyValue1.ref[2];
        va[1] = testValue1;
        vab[1] = testValue1;
        long res = test85(va, testValue1);
        Asserts.assertEquals(res, testValue1.hash());
        Asserts.assertEquals(va[0].hash(), testValue1.hash());
        res = test85(vab, testValue1);
        Asserts.assertEquals(res, testValue1.hash());
        Asserts.assertEquals(vab[0].hash(), testValue1.hash());
    }

    // Same as test85 but with ref value
    @Test()
    public static long test86(MyValue1.ref[] va, MyValue1.ref val) {
        va[0] = val;
        return va[1].hash();
    }

    @Run(test = "test86")
    public void test86_verifier() {
        MyValue1[] va = new MyValue1[2];
        MyValue1.ref[] vab = new MyValue1.ref[2];
        va[1] = testValue1;
        vab[1] = testValue1;
        long res = test86(va, testValue1);
        Asserts.assertEquals(res, testValue1.hash());
        Asserts.assertEquals(va[0].hash(), testValue1.hash());
        try {
            test86(va, null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException npe) {
            // Expected
        }
        res = test86(vab, testValue1);
        Asserts.assertEquals(res, testValue1.hash());
        Asserts.assertEquals(vab[0].hash(), testValue1.hash());
        res = test86(vab, null);
        Asserts.assertEquals(res, testValue1.hash());
        Asserts.assertEquals(vab[0], null);
    }

    // Test initialization of nullable array with constant
    @Test()
    public long test87() {
        MyValue1.ref[] va = new MyValue1.ref[1];
        va[0] = testValue1;
        return va[0].hash();
    }

    @Run(test = "test87")
    public void test87_verifier() {
        long result = test87();
        Asserts.assertEQ(result, hash());
    }

    // Test narrowing conversion from [L to [Q
    @Test
    @IR(failOn = {ALLOC, ALLOCA, LOOP, LOAD, STORE, TRAP})
    public static MyValue1[] test88(MyValue1.ref[] va) {
        return (MyValue1[])va;
    }

    @Run(test = "test88")
    public void test88_verifier() {
        MyValue1[] va = new MyValue1[1];
        va[0] = testValue1;
        MyValue1[] res = test88(va);
        Asserts.assertEquals(res[0].hash(), testValue1.hash());
        res[0] = testValue1;
        test88(null); // Should not throw NPE
        try {
            test88(new MyValue1.ref[1]);
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException cce) {
            // Expected
        }
    }

    // Same as test88 but with explicit cast and Object argument
    @Test
    @IR(failOn = {ALLOC, ALLOCA, LOOP, LOAD, STORE, TRAP})
    public static MyValue1[] test89(Object[] va) {
        return (MyValue1[])va;
    }

    @Run(test = "test89")
    public void test89_verifier() {
        MyValue1[] va = new MyValue1[1];
        va[0] = testValue1;
        MyValue1[] res = test89(va);
        Asserts.assertEquals(((MyValue1)res[0]).hash(), testValue1.hash());
        res[0] = testValue1;
        test89(null); // Should not throw NPE
        try {
            test89(new MyValue1.ref[1]);
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException cce) {
            // Expected
        }
    }

    // More cast tests
    @Test()
    public static MyValue1.ref[] test90(Object va) {
        return (MyValue1.ref[])va;
    }

    @Run(test = "test90")
    public void test90_verifier() {
        MyValue1[] va = new MyValue1[1];
        MyValue1.ref[] vab = new MyValue1.ref[1];
        try {
          // Trigger some ClassCastExceptions so C2 does not add an uncommon trap
          test90(new Integer[0]);
        } catch (ClassCastException cce) {
          // Expected
        }
        test90(va);
        test90(vab);
        test90(null);
    }

    @Test()
    public static MyValue1.ref[] test91(Object[] va) {
        return (MyValue1.ref[])va;
    }

    @Run(test = "test91")
    public void test91_verifier() {
        MyValue1[] va = new MyValue1[1];
        MyValue1.ref[] vab = new MyValue1.ref[1];
        try {
          // Trigger some ClassCastExceptions so C2 does not add an uncommon trap
          test91(new Integer[0]);
        } catch (ClassCastException cce) {
          // Expected
        }
        test91(va);
        test91(vab);
        test91(null);
    }

    // Test if arraycopy intrinsic correctly checks for flattened source array
    @Test()
    public static void test92(MyValue1.ref[] src, MyValue1.ref[] dst) {
        System.arraycopy(src, 0, dst, 0, 2);
    }

    @Run(test = "test92")
    public void test92_verifier() {
        MyValue1[]  va = new MyValue1[2];
        MyValue1.ref[] vab = new MyValue1.ref[2];
        va[0] = testValue1;
        vab[0] = testValue1;
        test92(va, vab);
        Asserts.assertEquals(va[0], vab[0]);
        Asserts.assertEquals(va[1], vab[1]);
    }

    @Test()
    public static void test93(Object src, MyValue1.ref[] dst) {
        System.arraycopy(src, 0, dst, 0, 2);
    }

    @Run(test = "test93")
    public void test93_verifier() {
        MyValue1[]  va = new MyValue1[2];
        MyValue1.ref[] vab = new MyValue1.ref[2];
        va[0] = testValue1;
        vab[0] = testValue1;
        test93(va, vab);
        Asserts.assertEquals(va[0], vab[0]);
        Asserts.assertEquals(va[1], vab[1]);
    }

    // Test non-escaping allocation with arraycopy
    // that does not modify loaded array element.
    @Test()
    public static long test94() {
        MyValue1.ref[] src = new MyValue1.ref[8];
        MyValue1[]  dst = new MyValue1[8];
        for (int i = 1; i < 8; ++i) {
            src[i] = testValue1;
        }
        System.arraycopy(src, 1, dst, 2, 6);
        return dst[0].hash();
    }

    @Run(test = "test94")
    public static void test94_verifier() {
        long result = test94();
        Asserts.assertEquals(result, MyValue1.default.hash());
    }

    // Test meeting constant TypeInstPtr with InlineTypeNode
    @ForceInline
    public long test95_callee() {
        MyValue1.ref[] va = new MyValue1.ref[1];
        va[0] = testValue1;
        return va[0].hashInterpreted();
    }

    @Test()
    public long test95() {
        return test95_callee();
    }

    @Run(test = "test95")
    @Warmup(0)
    public void test95_verifier() {
        long result = test95();
        Asserts.assertEQ(result, hash());
    }

    // Matrix multiplication test to exercise type flow analysis with nullable inline type arrays
    primitive static class Complex {
        private final double re;
        private final double im;

        Complex(double re, double im) {
            this.re = re;
            this.im = im;
        }

        public Complex add(Complex that) {
            return new Complex(this.re + that.re, this.im + that.im);
        }

        public Complex mul(Complex that) {
            return new Complex(this.re * that.re - this.im * that.im,
                               this.re * that.im + this.im * that.re);
        }
    }

    @Test()
    public Complex.ref[][] test96(Complex.ref[][] A, Complex.ref[][] B) {
        int size = A.length;
        Complex.ref[][] R = new Complex.ref[size][size];
        for (int i = 0; i < size; i++) {
            for (int k = 0; k < size; k++) {
                Complex.ref aik = A[i][k];
                for (int j = 0; j < size; j++) {
                    R[i][j] = B[i][j].add(aik.mul((Complex)B[k][j]));
                }
            }
        }
        return R;
    }

    static Complex.ref[][] test96_A = new Complex.ref[10][10];
    static Complex.ref[][] test96_B = new Complex.ref[10][10];
    static Complex.ref[][] test96_R;

    static {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                test96_A[i][j] = new Complex(rI, rI);
                test96_B[i][j] = new Complex(rI, rI);
            }
        }
    }

    @Run(test = "test96")
    public void test96_verifier() {
        Complex.ref[][] result = test96(test96_A, test96_B);
        if (test96_R == null) {
            test96_R = result;
        }
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Asserts.assertEQ(result[i][j], test96_R[i][j]);
            }
        }
    }

    // Test loads from vararg arrays
    @Test
    @IR(failOn = {LOAD_UNKNOWN_INLINE})
    public static Object test97(Object... args) {
        return args[0];
    }

    @Run(test = "test97")
    public static void test97_verifier() {
        Object obj = new Object();
        Object result = test97(obj);
        Asserts.assertEquals(result, obj);
        Integer[] myInt = new Integer[1];
        myInt[0] = rI;
        result = test97((Object[])myInt);
        Asserts.assertEquals(result, rI);
    }

    @Test()
    public static Object test98(Object... args) {
        return args[0];
    }

    @Run(test = "test98")
    public static void test98_verifier(RunInfo info) {
        Object obj = new Object();
        Object result = test98(obj);
        Asserts.assertEquals(result, obj);
        Integer[] myInt = new Integer[1];
        myInt[0] = rI;
        result = test98((Object[])myInt);
        Asserts.assertEquals(result, rI);
        if (!info.isWarmUp()) {
            MyValue1[] va = new MyValue1[1];
            MyValue1.ref[] vab = new MyValue1.ref[1];
            result = test98((Object[])va);
            Asserts.assertEquals(((MyValue1)result).hash(), MyValue1.default.hash());
            result = test98((Object[])vab);
            Asserts.assertEquals(result, null);
        }
    }

    @Test()
    public static Object test99(Object... args) {
        return args[0];
    }

    @Run(test = "test99")
    public static void test99_verifier(RunInfo info) {
        Object obj = new Object();
        Object result = test99(obj);
        Asserts.assertEquals(result, obj);
        Integer[] myInt = new Integer[1];
        myInt[0] = rI;
        result = test99((Object[])myInt);
        Asserts.assertEquals(result, rI);
        if (!info.isWarmUp()) {
            try {
                test99((Object[])null);
                throw new RuntimeException("No NPE thrown");
            } catch (NullPointerException npe) {
                // Expected
            }
        }
    }

    @Test()
    public static Object test100(Object... args) {
        return args[0];
    }

    @Run(test = "test100")
    public static void test100_verifier(RunInfo info) {
        Object obj = new Object();
        Object result = test100(obj);
        Asserts.assertEquals(result, obj);
        Integer[] myInt = new Integer[1];
        myInt[0] = rI;
        result = test100((Object[])myInt);
        Asserts.assertEquals(result, rI);
        if (!info.isWarmUp()) {
            try {
                test100();
                throw new RuntimeException("No AIOOBE thrown");
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                // Expected
            }
        }
    }

    // Test stores to varag arrays
    @Test
    @IR(failOn = STORE_UNKNOWN_INLINE)
    public static void test101(Object val, Object... args) {
        args[0] = val;
    }

    @Run(test = "test101")
    public static void test101_verifier() {
        Object obj = new Object();
        test101(obj, obj);
        Integer[] myInt = new Integer[1];
        test101(rI, (Object[])myInt);
        Asserts.assertEquals(myInt[0], rI);
        test101(null, (Object[])myInt);
        Asserts.assertEquals(myInt[0], null);
    }

    @Test()
    public static void test102(Object val, Object... args) {
        args[0] = val;
    }

    @Run(test = "test102")
    public static void test102_verifier(RunInfo info) {
        Object obj = new Object();
        test102(obj, obj);
        Integer[] myInt = new Integer[1];
        test102(rI, (Object[])myInt);
        Asserts.assertEquals(myInt[0], rI);
        test102(null, (Object[])myInt);
        Asserts.assertEquals(myInt[0], null);
        if (!info.isWarmUp()) {
            MyValue1[] va = new MyValue1[1];
            MyValue1.ref[] vab = new MyValue1.ref[1];
            test102(testValue1, (Object[])va);
            Asserts.assertEquals(va[0].hash(), testValue1.hash());
            test102(testValue1, (Object[])vab);
            Asserts.assertEquals(vab[0].hash(), testValue1.hash());
            test102(null, (Object[])vab);
            Asserts.assertEquals(vab[0], null);
        }
    }

    @Test()
    public static void test103(Object val, Object... args) {
        args[0] = val;
    }

    @Run(test = "test103")
    public static void test103_verifier(RunInfo info) {
        Object obj = new Object();
        test103(obj, obj);
        Integer[] myInt = new Integer[1];
        test103(rI, (Object[])myInt);
        Asserts.assertEquals(myInt[0], rI);
        test103(null, (Object[])myInt);
        Asserts.assertEquals(myInt[0], null);
        if (!info.isWarmUp()) {
            MyValue1[] va = new MyValue1[1];
            try {
                test103(null, (Object[])va);
                throw new RuntimeException("No NPE thrown");
            } catch (NullPointerException npe) {
                // Expected
            }
        }
    }

    @Test()
    public static void test104(Object val, Object... args) {
        args[0] = val;
    }

    @Run(test = "test104")
    public static void test104_verifier(RunInfo info) {
        Object obj = new Object();
        test104(obj, obj);
        Integer[] myInt = new Integer[1];
        test104(rI, (Object[])myInt);
        Asserts.assertEquals(myInt[0], rI);
        test104(null, (Object[])myInt);
        Asserts.assertEquals(myInt[0], null);
        if (!info.isWarmUp()) {
            try {
                test104(testValue1);
                throw new RuntimeException("No AIOOBE thrown");
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                // Expected
            }
        }
    }

    @Test()
    public static void test105(Object val, Object... args) {
        args[0] = val;
    }

    @Run(test = "test105")
    public static void test105_verifier(RunInfo info) {
        Object obj = new Object();
        test105(obj, obj);
        Integer[] myInt = new Integer[1];
        test105(rI, (Object[])myInt);
        Asserts.assertEquals(myInt[0], rI);
        test105(null, (Object[])myInt);
        Asserts.assertEquals(myInt[0], null);
        if (!info.isWarmUp()) {
            try {
                test105(testValue1, (Object[])null);
                throw new RuntimeException("No NPE thrown");
            } catch (NullPointerException npe) {
                // Expected
            }
        }
    }

    @Test()
    public static Object[] test106(Object[] dst, Object... args) {
        // Access array to speculate on non-flatness
        if (args[0] == null) {
            args[0] = testValue1;
        }
        System.arraycopy(args, 0, dst, 0, args.length);
        System.arraycopy(dst, 0, args, 0, dst.length);
        Object[] clone = args.clone();
        if (clone[0] == null) {
            throw new RuntimeException("Unexpected null");
        }
        return Arrays.copyOf(args, args.length, Object[].class);
    }

    @Run(test = "test106")
    public static void test106_verifier(RunInfo info) {
        Object[] dst = new Object[1];
        Object obj = new Object();
        Object[] result = test106(dst, obj);
        Asserts.assertEquals(result[0], obj);
        Integer[] myInt = new Integer[1];
        myInt[0] = rI;
        result = test106(myInt, (Object[])myInt);
        Asserts.assertEquals(result[0], rI);
        if (!info.isWarmUp()) {
            MyValue1[] va = new MyValue1[1];
            MyValue1.ref[] vab = new MyValue1.ref[1];
            result = test106(va, (Object[])va);
            Asserts.assertEquals(((MyValue1)result[0]).hash(), MyValue1.default.hash());
            result = test106(vab, (Object[])vab);
            Asserts.assertEquals(((MyValue1)result[0]).hash(), testValue1.hash());
        }
    }

    // Test that allocation is not replaced by non-dominating allocation
    public long test107_helper(MyValue1.ref[] va, MyValue1 vt) {
        try {
            va[0] = vt;
        } catch (NullPointerException npe) { }
        return va[1].hash();
    }

    @Test()
    public void test107() {
        MyValue1[] va = new MyValue1[2];
        MyValue1.ref[] tmp = new MyValue1.ref[2];
        long res1 = test107_helper(va, testValue1);
        long res2 = test107_helper(va, testValue1);
        Asserts.assertEquals(va[0].hash(), testValue1.hash());
        Asserts.assertEquals(res1, MyValue1.default.hash());
        Asserts.assertEquals(res2, MyValue1.default.hash());
    }

    @Run(test = "test107")
    public void test107_verifier() {
        test107();
    }

    @Test
    public Object test108(MyValue1.ref[] src, boolean flag) {
        MyValue1.ref[] dst = new MyValue1.ref[8];
        System.arraycopy(src, 1, dst, 2, 6);
        if (flag) {} // uncommon trap
        return dst[2];
    }

    @Run(test = "test108")
    @Warmup(10000)
    public void test108_verifier(RunInfo info) {
        MyValue1.ref[] src = new MyValue1.ref[8];
        test108(src, !info.isWarmUp());
    }

    // Test LoadNode::can_see_arraycopy_value optimization
    @Test()
    public static void test109() {
        MyValue1[] src = new MyValue1[1];
        MyValue1.ref[] dst = new MyValue1.ref[1];
        src[0] = testValue1;
        System.arraycopy(src, 0, dst, 0, 1);
        Asserts.assertEquals(src[0], dst[0]);
    }

    @Run(test = "test109")
    public void test109_verifier() {
        test109();
    }

    // Same as test109 but with Object destination array
    @Test()
    public static void test110() {
        MyValue1[] src = new MyValue1[1];
        Object[] dst = new Object[1];
        src[0] = testValue1;
        System.arraycopy(src, 0, dst, 0, 1);
        Asserts.assertEquals(src[0], dst[0]);
    }

    @Run(test = "test110")
    public void test110_verifier() {
        test110();
    }

    // Same as test109 but with Arrays.copyOf
    @Test()
    public static void test111() {
        MyValue1[] src = new MyValue1[1];
        src[0] = testValue1;
        MyValue1.ref[] dst = Arrays.copyOf(src, src.length, MyValue1.ref[].class);
        Asserts.assertEquals(src[0], dst[0]);
    }

    @Run(test = "test111")
    public void test111_verifier() {
        test111();
    }
}
