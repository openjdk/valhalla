/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.Arrays;

/*
 * @test
 * @key randomness
 * @summary Test inline type arrays
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires (os.simpleArch == "x64" | os.simpleArch == "aarch64")
 * @compile TestArrays.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=300 -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                               compiler.valhalla.inlinetypes.InlineTypeTest
 *                               compiler.valhalla.inlinetypes.TestArrays
 */
public class TestArrays extends InlineTypeTest {
    // Extra VM parameters for some test scenarios. See InlineTypeTest.getVMParameters()
    @Override
    public String[] getExtraVMParameters(int scenario) {
        switch (scenario) {
        case 2: return new String[] {"-XX:-MonomorphicArrayCheck", "-XX:-UncommonNullCast", "-XX:+StressArrayCopyMacroNode"};
        case 3: return new String[] {"-XX:-MonomorphicArrayCheck", "-XX:FlatArrayElementMaxSize=-1", "-XX:-UncommonNullCast"};
        case 4: return new String[] {"-XX:-MonomorphicArrayCheck", "-XX:-UncommonNullCast"};
        case 5: return new String[] {"-XX:-MonomorphicArrayCheck", "-XX:-UncommonNullCast", "-XX:+StressArrayCopyMacroNode"};
        }
        return null;
    }

    public static void main(String[] args) throws Throwable {
        TestArrays test = new TestArrays();
        test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class);
    }

    // Helper methods

    protected long hash() {
        return hash(rI, rL);
    }

    protected long hash(int x, long y) {
        return MyValue1.createWithFieldsInline(x, y).hash();
    }

    // Test inline type array creation and initialization
    @Test(valid = InlineTypeArrayFlattenOn, match = { ALLOCA }, matchCount = { 1 })
    @Test(valid = InlineTypeArrayFlattenOff, match = { ALLOCA }, matchCount = { 1 }, failOn = LOAD)
    public MyValue1[] test1(int len) {
        MyValue1[] va = new MyValue1[len];
        for (int i = 0; i < len; ++i) {
            va[i] = MyValue1.createWithFieldsDontInline(rI, rL);
        }
        return va;
    }

    @DontCompile
    public void test1_verifier(boolean warmup) {
        int len = Math.abs(rI % 10);
        MyValue1[] va = test1(len);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(va[i].hash(), hash());
        }
    }

    // Test creation of an inline type array and element access
    @Test(failOn = ALLOC + ALLOCA + LOOP + LOAD + STORE + TRAP)
    public long test2() {
        MyValue1[] va = new MyValue1[1];
        va[0] = MyValue1.createWithFieldsInline(rI, rL);
        return va[0].hash();
    }

    @DontCompile
    public void test2_verifier(boolean warmup) {
        long result = test2();
        Asserts.assertEQ(result, hash());
    }

    // Test receiving an inline type array from the interpreter,
    // updating its elements in a loop and computing a hash.
    @Test(failOn = ALLOCA)
    public long test3(MyValue1[] va) {
        long result = 0;
        for (int i = 0; i < 10; ++i) {
            result += va[i].hash();
            va[i] = MyValue1.createWithFieldsInline(rI + 1, rL + 1);
        }
        return result;
    }

    @DontCompile
    public void test3_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[10];
        long expected = 0;
        for (int i = 0; i < 10; ++i) {
            va[i] = MyValue1.createWithFieldsDontInline(rI + i, rL + i);
            expected += va[i].hash();
        }
        long result = test3(va);
        Asserts.assertEQ(expected, result);
        for (int i = 0; i < 10; ++i) {
            if (va[i].hash() != hash(rI + 1, rL + 1)) {
                Asserts.assertEQ(va[i].hash(), hash(rI + 1, rL + 1));
            }
        }
    }

    // Test returning an inline type array received from the interpreter
    @Test(failOn = ALLOC + ALLOCA + LOAD + STORE + LOOP + TRAP)
    public MyValue1[] test4(MyValue1[] va) {
        return va;
    }

    @DontCompile
    public void test4_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[10];
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
    public MyValue1[] test5(boolean b) {
        MyValue1[] va;
        if (b) {
            va = new MyValue1[5];
            for (int i = 0; i < 5; ++i) {
                va[i] = MyValue1.createWithFieldsInline(rI, rL);
            }
        } else {
            va = new MyValue1[10];
            for (int i = 0; i < 10; ++i) {
                va[i] = MyValue1.createWithFieldsInline(rI + i, rL + i);
            }
        }
        long sum = va[0].hashInterpreted();
        if (b) {
            va[0] = MyValue1.createWithFieldsDontInline(rI, sum);
        } else {
            va[0] = MyValue1.createWithFieldsDontInline(rI + 1, sum + 1);
        }
        return va;
    }

    @DontCompile
    public void test5_verifier(boolean warmup) {
        MyValue1[] va = test5(true);
        Asserts.assertEQ(va.length, 5);
        Asserts.assertEQ(va[0].hash(), hash(rI, hash()));
        for (int i = 1; i < 5; ++i) {
            Asserts.assertEQ(va[i].hash(), hash());
        }
        va = test5(false);
        Asserts.assertEQ(va.length, 10);
        Asserts.assertEQ(va[0].hash(), hash(rI + 1, hash(rI, rL) + 1));
        for (int i = 1; i < 10; ++i) {
            Asserts.assertEQ(va[i].hash(), hash(rI + i, rL + i));
        }
    }

    // Test creation of inline type array with single element
    @Test(failOn = ALLOC + ALLOCA + LOOP + LOAD + STORE + TRAP)
    public MyValue1 test6() {
        MyValue1[] va = new MyValue1[1];
        return va[0];
    }

    @DontCompile
    public void test6_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[1];
        MyValue1 v = test6();
        Asserts.assertEQ(v.hashPrimitive(), va[0].hashPrimitive());
    }

    // Test default initialization of inline type arrays
    @Test(failOn = LOAD)
    public MyValue1[] test7(int len) {
        return new MyValue1[len];
    }

    @DontCompile
    public void test7_verifier(boolean warmup) {
        int len = Math.abs(rI % 10);
        MyValue1[] va = new MyValue1[len];
        MyValue1[] var = test7(len);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(va[i].hashPrimitive(), var[i].hashPrimitive());
        }
    }

    // Test creation of inline type array with zero length
    @Test(failOn = ALLOC + LOAD + STORE + LOOP + TRAP)
    public MyValue1[] test8() {
        return new MyValue1[0];
    }

    @DontCompile
    public void test8_verifier(boolean warmup) {
        MyValue1[] va = test8();
        Asserts.assertEQ(va.length, 0);
    }

    static MyValue1[] test9_va;

    // Test that inline type array loaded from field has correct type
    @Test(failOn = LOOP)
    public long test9() {
        return test9_va[0].hash();
    }

    @DontCompile
    public void test9_verifier(boolean warmup) {
        test9_va = new MyValue1[1];
        test9_va[0] = MyValue1.createWithFieldsInline(rI, rL);
        long result = test9();
        Asserts.assertEQ(result, hash());
    }

    // Multi-dimensional arrays
    @Test
    public MyValue1[][][] test10(int len1, int len2, int len3) {
        MyValue1[][][] arr = new MyValue1[len1][len2][len3];
        for (int i = 0; i < len1; i++) {
            for (int j = 0; j < len2; j++) {
                for (int k = 0; k < len3; k++) {
                    arr[i][j][k] = MyValue1.createWithFieldsDontInline(rI + i , rL + j + k);
                }
            }
        }
        return arr;
    }

    @DontCompile
    public void test10_verifier(boolean warmup) {
        MyValue1[][][] arr = test10(2, 3, 4);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 4; k++) {
                    Asserts.assertEQ(arr[i][j][k].hash(), MyValue1.createWithFieldsDontInline(rI + i , rL + j + k).hash());
                }
            }
        }
    }

    @Test
    public void test11(MyValue1[][][] arr, long[] res) {
        int l = 0;
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].length; j++) {
                for (int k = 0; k < arr[i][j].length; k++) {
                    res[l] = arr[i][j][k].hash();
                    l++;
                }
            }
        }
    }

    @DontCompile
    public void test11_verifier(boolean warmup) {
        MyValue1[][][] arr = new MyValue1[2][3][4];
        long[] res = new long[2*3*4];
        long[] verif = new long[2*3*4];
        int l = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 4; k++) {
                    arr[i][j][k] = MyValue1.createWithFieldsDontInline(rI + i, rL + j + k);
                    verif[l] = arr[i][j][k].hash();
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
        int arraySize = Math.abs(rI) % 10;;
        MyValue1[] va = new MyValue1[arraySize];

        for (int i = 0; i < arraySize; i++) {
            va[i] = MyValue1.createWithFieldsDontInline(rI + 1, rL);
        }

        try {
            return va[arraySize + 1].x;
        } catch (ArrayIndexOutOfBoundsException e) {
            return rI;
        }
    }

    @DontCompile
    public void test12_verifier(boolean warmup) {
        Asserts.assertEQ(test12(), rI);
    }

    // Array load  out of bounds (lower bound) at compile time
    @Test
    public int test13() {
        int arraySize = Math.abs(rI) % 10;;
        MyValue1[] va = new MyValue1[arraySize];

        for (int i = 0; i < arraySize; i++) {
            va[i] = MyValue1.createWithFieldsDontInline(rI + i, rL);
        }

        try {
            return va[-arraySize].x;
        } catch (ArrayIndexOutOfBoundsException e) {
            return rI;
        }
    }

    @DontCompile
    public void test13_verifier(boolean warmup) {
        Asserts.assertEQ(test13(), rI);
    }

    // Array load out of bound not known to compiler (both lower and upper bound)
    @Test
    public int test14(MyValue1[] va, int index)  {
        return va[index].x;
    }

    @DontCompile
    public void test14_verifier(boolean warmup) {
        int arraySize = Math.abs(rI) % 10;
        MyValue1[] va = new MyValue1[arraySize];

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
        int arraySize = Math.abs(rI) % 10;;
        MyValue1[] va = new MyValue1[arraySize];

        try {
            for (int i = 0; i <= arraySize; i++) {
                va[i] = MyValue1.createWithFieldsDontInline(rI + 1, rL);
            }
            return rI - 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            return rI;
        }
    }

    @DontCompile
    public void test15_verifier(boolean warmup) {
        Asserts.assertEQ(test15(), rI);
    }

    // Array store out of bounds (lower bound) at compile time
    @Test
    public int test16() {
        int arraySize = Math.abs(rI) % 10;;
        MyValue1[] va = new MyValue1[arraySize];

        try {
            for (int i = -1; i <= arraySize; i++) {
                va[i] = MyValue1.createWithFieldsDontInline(rI + 1, rL);
            }
            return rI - 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            return rI;
        }
    }

    @DontCompile
    public void test16_verifier(boolean warmup) {
        Asserts.assertEQ(test16(), rI);
    }

    // Array store out of bound not known to compiler (both lower and upper bound)
    @Test
    public int test17(MyValue1[] va, int index, MyValue1 vt)  {
        va[index] = vt;
        return va[index].x;
    }

    @DontCompile
    public void test17_verifier(boolean warmup) {
        int arraySize = Math.abs(rI) % 10;
        MyValue1[] va = new MyValue1[arraySize];

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
    public MyValue1[] test18(MyValue1[] va) {
        return va.clone();
    }

    @DontCompile
    public void test18_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1[] va = new MyValue1[len];
        for (int i = 0; i < len; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        MyValue1[] result = test18(va);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(result[i].hash(), va[i].hash());
        }
    }

    // clone() as series of loads/stores
    static MyValue1[] test19_orig = null;

    @Test
    public MyValue1[] test19() {
        MyValue1[] va = new MyValue1[8];
        for (int i = 0; i < va.length; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        test19_orig = va;

        return va.clone();
    }

    @DontCompile
    public void test19_verifier(boolean warmup) {
        MyValue1[] result = test19();
        for (int i = 0; i < test19_orig.length; ++i) {
            Asserts.assertEQ(result[i].hash(), test19_orig[i].hash());
        }
    }

    // arraycopy() of inline type array with oop fields
    @Test
    public void test20(MyValue1[] src, MyValue1[] dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @DontCompile
    public void test20_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1[] src = new MyValue1[len];
        MyValue1[] dst = new MyValue1[len];
        for (int i = 0; i < len; ++i) {
            src[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        test20(src, dst);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(src[i].hash(), dst[i].hash());
        }
    }

    // arraycopy() of inline type array with no oop field
    @Test
    public void test21(MyValue2[] src, MyValue2[] dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @DontCompile
    public void test21_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue2[] src = new MyValue2[len];
        MyValue2[] dst = new MyValue2[len];
        for (int i = 0; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test21(src, dst);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(src[i].hash(), dst[i].hash());
        }
    }

    // arraycopy() of inline type array with oop field and tightly
    // coupled allocation as dest
    @Test
    public MyValue1[] test22(MyValue1[] src) {
        MyValue1[] dst = new MyValue1[src.length];
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    @DontCompile
    public void test22_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1[] src = new MyValue1[len];
        for (int i = 0; i < len; ++i) {
            src[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        MyValue1[] dst = test22(src);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(src[i].hash(), dst[i].hash());
        }
    }

    // arraycopy() of inline type array with oop fields and tightly
    // coupled allocation as dest
    @Test
    public MyValue1[] test23(MyValue1[] src) {
        MyValue1[] dst = new MyValue1[src.length + 10];
        System.arraycopy(src, 0, dst, 5, src.length);
        return dst;
    }

    @DontCompile
    public void test23_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1[] src = new MyValue1[len];
        for (int i = 0; i < len; ++i) {
            src[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        MyValue1[] dst = test23(src);
        for (int i = 5; i < len; ++i) {
            Asserts.assertEQ(src[i].hash(), dst[i].hash());
        }
    }

    // arraycopy() of inline type array passed as Object
    @Test
    public void test24(MyValue1[] src, Object dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @DontCompile
    public void test24_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1[] src = new MyValue1[len];
        MyValue1[] dst1 = new MyValue1[len];
        Object[] dst2 = new Object[len];
        for (int i = 0; i < len; ++i) {
            src[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        test24(src, dst1);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(src[i].hash(), dst1[i].hash());
        }
        test24(src, dst2);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(src[i].hash(), ((MyValue1)dst2[i]).hash());
        }
    }

    // short arraycopy() with no oop field
    @Test
    public void test25(MyValue2[] src, MyValue2[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @DontCompile
    public void test25_verifier(boolean warmup) {
        MyValue2[] src = new MyValue2[8];
        MyValue2[] dst = new MyValue2[8];
        for (int i = 0; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test25(src, dst);
        for (int i = 0; i < 8; ++i) {
            Asserts.assertEQ(src[i].hash(), dst[i].hash());
        }
    }

    // short arraycopy() with oop fields
    @Test
    public void test26(MyValue1[] src, MyValue1[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @DontCompile
    public void test26_verifier(boolean warmup) {
        MyValue1[] src = new MyValue1[8];
        MyValue1[] dst = new MyValue1[8];
        for (int i = 0; i < 8; ++i) {
            src[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        test26(src, dst);
        for (int i = 0; i < 8; ++i) {
            Asserts.assertEQ(src[i].hash(), dst[i].hash());
        }
    }

    // short arraycopy() with oop fields and offsets
    @Test
    public void test27(MyValue1[] src, MyValue1[] dst) {
        System.arraycopy(src, 1, dst, 2, 6);
    }

    @DontCompile
    public void test27_verifier(boolean warmup) {
        MyValue1[] src = new MyValue1[8];
        MyValue1[] dst = new MyValue1[8];
        for (int i = 0; i < 8; ++i) {
            src[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        test27(src, dst);
        for (int i = 2; i < 8; ++i) {
            Asserts.assertEQ(src[i-1].hash(), dst[i].hash());
        }
    }

    // non escaping allocations
    // TODO 8252027: Make sure this is optimized with ZGC
    @Test(valid = ZGCOff, failOn = ALLOCA + LOOP + LOAD + TRAP)
    @Test(valid = ZGCOn)
    public MyValue2 test28() {
        MyValue2[] src = new MyValue2[10];
        src[0] = MyValue2.createWithFieldsInline(rI, rD);
        MyValue2[] dst = (MyValue2[])src.clone();
        return dst[0];
    }

    @DontCompile
    public void test28_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, rD);
        MyValue2 result = test28();
        Asserts.assertEQ(result.hash(), v.hash());
    }

    // non escaping allocations
    // TODO 8227588: shouldn't this have the same IR matching rules as test6?
    // @Test(failOn = ALLOC + ALLOCA + LOOP + LOAD + STORE + TRAP)
    @Test(valid = InlineTypeArrayFlattenOn, failOn = ALLOCA + LOOP + LOAD + TRAP)
    @Test(valid = InlineTypeArrayFlattenOff, failOn = ALLOCA + LOOP + TRAP)
    public MyValue2 test29(MyValue2[] src) {
        MyValue2[] dst = new MyValue2[10];
        System.arraycopy(src, 0, dst, 0, 10);
        return dst[0];
    }

    @DontCompile
    public void test29_verifier(boolean warmup) {
        MyValue2[] src = new MyValue2[10];
        for (int i = 0; i < 10; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        MyValue2 v = test29(src);
        Asserts.assertEQ(src[0].hash(), v.hash());
    }

    // non escaping allocation with uncommon trap that needs
    // eliminated inline type array element as debug info
    @Test
    @Warmup(10000)
    public MyValue2 test30(MyValue2[] src, boolean flag) {
        MyValue2[] dst = new MyValue2[10];
        System.arraycopy(src, 0, dst, 0, 10);
        if (flag) { }
        return dst[0];
    }

    @DontCompile
    public void test30_verifier(boolean warmup) {
        MyValue2[] src = new MyValue2[10];
        for (int i = 0; i < 10; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        MyValue2 v = test30(src, !warmup);
        Asserts.assertEQ(src[0].hash(), v.hash());
    }

    // non escaping allocation with memory phi
    @Test(failOn = ALLOC + ALLOCA + LOOP + LOAD + STORE + TRAP)
    public long test31(boolean b, boolean deopt) {
        MyValue2[] src = new MyValue2[1];
        if (b) {
            src[0] = MyValue2.createWithFieldsInline(rI, rD);
        } else {
            src[0] = MyValue2.createWithFieldsInline(rI+1, rD+1);
        }
        if (deopt) {
            // uncommon trap
            WHITE_BOX.deoptimizeMethod(tests.get(getClass().getSimpleName() + "::test31"));
        }
        return src[0].hash();
    }

    @DontCompile
    public void test31_verifier(boolean warmup) {
        MyValue2 v1 = MyValue2.createWithFieldsInline(rI, rD);
        long result1 = test31(true, !warmup);
        Asserts.assertEQ(result1, v1.hash());
        MyValue2 v2 = MyValue2.createWithFieldsInline(rI+1, rD+1);
        long result2 = test31(false, !warmup);
        Asserts.assertEQ(result2, v2.hash());
    }

    // Tests with Object arrays and clone/arraycopy
    // clone() as stub call
    @Test
    public Object[] test32(Object[] va) {
        return va.clone();
    }

    @DontCompile
    public void test32_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1[] va = new MyValue1[len];
        for (int i = 0; i < len; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        MyValue1[] result = (MyValue1[])test32(va);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(((MyValue1)result[i]).hash(), ((MyValue1)va[i]).hash());
        }
    }

    @Test
    public Object[] test33(Object[] va) {
        return va.clone();
    }

    @DontCompile
    public void test33_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        Object[] va = new Object[len];
        for (int i = 0; i < len; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        Object[] result = test33(va);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(((MyValue1)result[i]).hash(), ((MyValue1)va[i]).hash());
            // Check that array has correct properties (null-ok)
            result[i] = null;
        }
    }

    // clone() as series of loads/stores
    static Object[] test34_orig = null;

    @ForceInline
    public Object[] test34_helper(boolean flag) {
        Object[] va = null;
        if (flag) {
            va = new MyValue1[8];
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

    @DontCompile
    public void test34_verifier(boolean warmup) {
        test34(false);
        for (int i = 0; i < 10; i++) { // make sure we do deopt
            Object[] result = test34(true);
            verify(test34_orig, result);
            // Check that array has correct properties (null-free)
            try {
                result[0] = null;
                throw new RuntimeException("Should throw NullPointerException");
            } catch (NullPointerException e) {
                // Expected
            }
        }
        if (compile_and_run_again_if_deoptimized(warmup, "TestArrays::test34")) {
            Object[] result = test34(true);
            verify(test34_orig, result);
            // Check that array has correct properties (null-free)
            try {
                result[0] = null;
                throw new RuntimeException("Should throw NullPointerException");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    static void verify(Object[] src, Object[] dst) {
        if (src instanceof MyInterface[] && dst instanceof MyInterface[]) {
            for (int i = 0; i < src.length; ++i) {
                Asserts.assertEQ(((MyInterface)src[i]).hash(), ((MyInterface)dst[i]).hash());
            }
        } else {
            for (int i = 0; i < src.length; ++i) {
                Asserts.assertEQ(src[i], dst[i]);
            }
        }
    }

    static void verify(MyValue1[] src, MyValue1[] dst) {
        for (int i = 0; i < src.length; ++i) {
            Asserts.assertEQ(src[i].hash(), dst[i].hash());
        }
    }

    static void verify(MyValue1[] src, Object[] dst) {
        for (int i = 0; i < src.length; ++i) {
            Asserts.assertEQ(src[i].hash(), ((MyInterface)dst[i]).hash());
        }
    }

    static void verify(MyValue2[] src, MyValue2[] dst) {
        for (int i = 0; i < src.length; ++i) {
            Asserts.assertEQ(src[i].hash(), dst[i].hash());
        }
    }

    static void verify(MyValue2[] src, Object[] dst) {
        for (int i = 0; i < src.length; ++i) {
            Asserts.assertEQ(src[i].hash(), ((MyInterface)dst[i]).hash());
        }
    }

    static boolean compile_and_run_again_if_deoptimized(boolean warmup, String test) {
        if (!warmup) {
            Method m = tests.get(test);
            if (USE_COMPILER && !WHITE_BOX.isMethodCompiled(m, false)) {
                if (!InlineTypeArrayFlatten && !XCOMP && !STRESS_CC) {
                    throw new RuntimeException("Unexpected deoptimization");
                }
                enqueueMethodForCompilation(m, COMP_LEVEL_FULL_OPTIMIZATION);
                return true;
            }
        }
        return false;
    }

    // arraycopy() of inline type array of unknown size
    @Test
    public void test35(Object src, Object dst, int len) {
        System.arraycopy(src, 0, dst, 0, len);
    }

    @DontCompile
    public void test35_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1[] src = new MyValue1[len];
        MyValue1[] dst1 = new MyValue1[len];
        Object[] dst2 = new Object[len];
        for (int i = 0; i < len; ++i) {
            src[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        test35(src, dst1, src.length);
        verify(src, dst1);
        test35(src, dst2, src.length);
        verify(src, dst2);
        if (compile_and_run_again_if_deoptimized(warmup, "TestArrays::test35")) {
            test35(src, dst1, src.length);
            verify(src, dst1);
        }
    }

    @Test
    public void test36(Object src, MyValue2[] dst) {
        System.arraycopy(src, 0, dst, 0, dst.length);
    }

    @DontCompile
    public void test36_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue2[] src = new MyValue2[len];
        MyValue2[] dst = new MyValue2[len];
        for (int i = 0; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test36(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestArrays::test36")) {
            test36(src, dst);
            verify(src, dst);
        }
    }

    @Test
    public void test37(MyValue2[] src, Object dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @DontCompile
    public void test37_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue2[] src = new MyValue2[len];
        MyValue2[] dst = new MyValue2[len];
        for (int i = 0; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test37(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestArrays::test37")) {
            test37(src, dst);
            verify(src, dst);
        }
    }

    @Test
    @Warmup(1) // Avoid early compilation
    public void test38(Object src, MyValue2[] dst) {
        System.arraycopy(src, 0, dst, 0, dst.length);
    }

    @DontCompile
    public void test38_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        Object[] src = new Object[len];
        MyValue2[] dst = new MyValue2[len];
        for (int i = 0; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test38(src, dst);
        verify(dst, src);
        if (!warmup) {
            Method m = tests.get("TestArrays::test38");
            assertDeoptimizedByC2(m);
            enqueueMethodForCompilation(m, COMP_LEVEL_FULL_OPTIMIZATION);
            test38(src, dst);
            verify(dst, src);
            if (USE_COMPILER && !WHITE_BOX.isMethodCompiled(m, false) && !XCOMP && !STRESS_CC) {
                throw new RuntimeException("unexpected deoptimization");
            }
        }
    }

    @Test
    public void test39(MyValue2[] src, Object dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @DontCompile
    public void test39_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue2[] src = new MyValue2[len];
        Object[] dst = new Object[len];
        for (int i = 0; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test39(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestArrays::test39")) {
            test39(src, dst);
            verify(src, dst);
        }
    }

    @Test
    @Warmup(1) // Avoid early compilation
    public void test40(Object[] src, Object dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @DontCompile
    public void test40_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        Object[] src = new Object[len];
        MyValue2[] dst = new MyValue2[len];
        for (int i = 0; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test40(src, dst);
        verify(dst, src);
        if (!warmup) {
            Method m = tests.get("TestArrays::test40");
            assertDeoptimizedByC2(m);
            enqueueMethodForCompilation(m, COMP_LEVEL_FULL_OPTIMIZATION);
            test40(src, dst);
            verify(dst, src);
            if (USE_COMPILER && !WHITE_BOX.isMethodCompiled(m, false) && !XCOMP && !STRESS_CC) {
                throw new RuntimeException("unexpected deoptimization");
            }
        }
    }

    @Test
    public void test41(Object src, Object[] dst) {
        System.arraycopy(src, 0, dst, 0, dst.length);
    }

    @DontCompile
    public void test41_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue2[] src = new MyValue2[len];
        Object[] dst = new Object[len];
        for (int i = 0; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test41(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestArrays::test41")) {
            test41(src, dst);
            verify(src, dst);
        }
    }

    @Test
    public void test42(Object[] src, Object[] dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @DontCompile
    public void test42_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        Object[] src = new Object[len];
        Object[] dst = new Object[len];
        for (int i = 0; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test42(src, dst);
        verify(src, dst);
        if (!warmup) {
            Method m = tests.get("TestArrays::test42");
            if (USE_COMPILER && !WHITE_BOX.isMethodCompiled(m, false) && !XCOMP && !STRESS_CC) {
                throw new RuntimeException("unexpected deoptimization");
            }
        }
    }

    // short arraycopy()'s
    @Test
    public void test43(Object src, Object dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @DontCompile
    public void test43_verifier(boolean warmup) {
        MyValue1[] src = new MyValue1[8];
        MyValue1[] dst = new MyValue1[8];
        for (int i = 0; i < 8; ++i) {
            src[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        test43(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestArrays::test43")) {
            test43(src, dst);
            verify(src, dst);
        }
    }

    @Test
    public void test44(Object src, MyValue2[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @DontCompile
    public void test44_verifier(boolean warmup) {
        MyValue2[] src = new MyValue2[8];
        MyValue2[] dst = new MyValue2[8];
        for (int i = 0; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test44(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestArrays::test44")) {
            test44(src, dst);
            verify(src, dst);
        }
    }

    @Test
    public void test45(MyValue2[] src, Object dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @DontCompile
    public void test45_verifier(boolean warmup) {
        MyValue2[] src = new MyValue2[8];
        MyValue2[] dst = new MyValue2[8];
        for (int i = 0; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test45(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestArrays::test45")) {
            test45(src, dst);
            verify(src, dst);
        }
    }

    @Test
    @Warmup(1) // Avoid early compilation
    public void test46(Object[] src, MyValue2[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @DontCompile
    public void test46_verifier(boolean warmup) {
        Object[] src = new Object[8];
        MyValue2[] dst = new MyValue2[8];
        for (int i = 0; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test46(src, dst);
        verify(dst, src);
        if (!warmup) {
            Method m = tests.get("TestArrays::test46");
            assertDeoptimizedByC2(m);
            enqueueMethodForCompilation(m, COMP_LEVEL_FULL_OPTIMIZATION);
            test46(src, dst);
            verify(dst, src);
            if (USE_COMPILER && !WHITE_BOX.isMethodCompiled(m, false) && !XCOMP && !STRESS_CC) {
                throw new RuntimeException("unexpected deoptimization");
            }
        }
    }

    @Test
    public void test47(MyValue2[] src, Object[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @DontCompile
    public void test47_verifier(boolean warmup) {
        MyValue2[] src = new MyValue2[8];
        Object[] dst = new Object[8];
        for (int i = 0; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test47(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestArrays::test47")) {
            test47(src, dst);
            verify(src, dst);
        }
    }

    @Test
    @Warmup(1) // Avoid early compilation
    public void test48(Object[] src, Object dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @DontCompile
    public void test48_verifier(boolean warmup) {
        Object[] src = new Object[8];
        MyValue2[] dst = new MyValue2[8];
        for (int i = 0; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test48(src, dst);
        verify(dst, src);
        if (!warmup) {
            Method m = tests.get("TestArrays::test48");
            assertDeoptimizedByC2(m);
            enqueueMethodForCompilation(m, COMP_LEVEL_FULL_OPTIMIZATION);
            test48(src, dst);
            verify(dst, src);
            if (USE_COMPILER && !WHITE_BOX.isMethodCompiled(m, false) && !XCOMP && !STRESS_CC) {
                throw new RuntimeException("unexpected deoptimization");
            }
        }
    }

    @Test
    public void test49(Object src, Object[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @DontCompile
    public void test49_verifier(boolean warmup) {
        MyValue2[] src = new MyValue2[8];
        Object[] dst = new Object[8];
        for (int i = 0; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test49(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestArrays::test49")) {
            test49(src, dst);
            verify(src, dst);
        }
    }

    @Test
    public void test50(Object[] src, Object[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @DontCompile
    public void test50_verifier(boolean warmup) {
        Object[] src = new Object[8];
        Object[] dst = new Object[8];
        for (int i = 0; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        test50(src, dst);
        verify(src, dst);
        if (!warmup) {
            Method m = tests.get("TestArrays::test50");
            if (USE_COMPILER && !WHITE_BOX.isMethodCompiled(m, false) && !XCOMP && !STRESS_CC) {
                throw new RuntimeException("unexpected deoptimization");
            }
        }
    }

    @Test
    public MyValue1[] test51(MyValue1[] va) {
        // TODO 8244562: Remove cast as workaround once javac is fixed
        Object[] res = Arrays.copyOf(va, va.length, MyValue1[].class);
        return (MyValue1[]) res;
    }

    @DontCompile
    public void test51_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1[] va = new MyValue1[len];
        for (int i = 0; i < len; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        MyValue1[] result = test51(va);
        verify(va, result);
    }

    static final MyValue1[] test52_va = new MyValue1[8];

    @Test
    public MyValue1[] test52() {
        // TODO 8244562: Remove cast as workaround once javac is fixed
        Object[] res = Arrays.copyOf(test52_va, 8, MyValue1[].class);
        return (MyValue1[]) res;
    }

    @DontCompile
    public void test52_verifier(boolean warmup) {
        for (int i = 0; i < 8; ++i) {
            test52_va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        MyValue1[] result = test52();
        verify(test52_va, result);
    }

    @Test
    public MyValue1[] test53(Object[] va) {
        // TODO 8244562: Remove cast as workaround once javac is fixed
        Object[] res = Arrays.copyOf(va, va.length, MyValue1[].class);
        return (MyValue1[]) res;
    }

    @DontCompile
    public void test53_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1[] va = new MyValue1[len];
        for (int i = 0; i < len; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        MyValue1[] result = test53(va);
        verify(result, va);
    }

    @Test
    public Object[] test54(MyValue1[] va) {
        return Arrays.copyOf(va, va.length, Object[].class);
    }

    @DontCompile
    public void test54_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1[] va = new MyValue1[len];
        for (int i = 0; i < len; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        Object[] result = test54(va);
        verify(va, result);
    }

    @Test
    public Object[] test55(Object[] va) {
        return Arrays.copyOf(va, va.length, Object[].class);
    }

    @DontCompile
    public void test55_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1[] va = new MyValue1[len];
        for (int i = 0; i < len; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        Object[] result = test55(va);
        verify(va, result);
    }

    @Test
    public MyValue1[] test56(Object[] va) {
        // TODO 8244562: Remove cast as workaround once javac is fixed
        Object[] res = Arrays.copyOf(va, va.length, MyValue1[].class);
        return (MyValue1[]) res;
    }

    @DontCompile
    public void test56_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        Object[] va = new Object[len];
        for (int i = 0; i < len; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        MyValue1[] result = test56(va);
        verify(result, va);
    }

   @Test
    public Object[] test57(Object[] va, Class klass) {
        return Arrays.copyOf(va, va.length, klass);
    }

    @DontCompile
    public void test57_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        Object[] va = new MyValue1[len];
        for (int i = 0; i < len; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        Object[] result = test57(va, MyValue1[].class);
        verify(va, result);
    }

    @Test
    public Object[] test58(MyValue1[] va, Class klass) {
        return Arrays.copyOf(va, va.length, klass);
    }

    @DontCompile
    public void test58_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1[] va = new MyValue1[len];
        for (int i = 0; i < len; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        for (int i = 0; i < 10; i++) {
            Object[] result = test58(va, MyValue1[].class);
            verify(va, result);
        }
        if (compile_and_run_again_if_deoptimized(warmup, "TestArrays::test58")) {
            Object[] result = test58(va, MyValue1[].class);
            verify(va, result);
        }
    }

    @Test
    public Object[] test59(MyValue1[] va) {
        return Arrays.copyOf(va, va.length+1, MyValue1[].class);
    }

    @DontCompile
    public void test59_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1[] va = new MyValue1[len];
        MyValue1[] verif = new MyValue1[len+1];
        for (int i = 0; i < len; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
            verif[i] = va[i];
        }
        Object[] result = test59(va);
        verify(verif, result);
    }

    @Test
    public Object[] test60(Object[] va, Class klass) {
        return Arrays.copyOf(va, va.length+1, klass);
    }

    @DontCompile
    public void test60_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1[] va = new MyValue1[len];
        MyValue1[] verif = new MyValue1[len+1];
        for (int i = 0; i < len; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
            verif[i] = (MyValue1)va[i];
        }
        Object[] result = test60(va, MyValue1[].class);
        verify(verif, result);
    }

    @Test
    public Object[] test61(Object[] va, Class klass) {
        return Arrays.copyOf(va, va.length+1, klass);
    }

    @DontCompile
    public void test61_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        Object[] va = new Integer[len];
        for (int i = 0; i < len; ++i) {
            va[i] = Integer.valueOf(rI);
        }
        Object[] result = test61(va, Integer[].class);
        for (int i = 0; i < va.length; ++i) {
            Asserts.assertEQ(va[i], result[i]);
        }
    }

    @ForceInline
    public Object[] test62_helper(int i, MyValue1[] va, Integer[] oa) {
        Object[] arr = null;
        if (i == 10) {
            arr = oa;
        } else {
            arr = va;
        }
        return arr;
    }

    @Test
    public Object[] test62(MyValue1[] va, Integer[] oa) {
        int i = 0;
        for (; i < 10; i++);

        Object[] arr = test62_helper(i, va, oa);

        return Arrays.copyOf(arr, arr.length+1, arr.getClass());
    }

    @DontCompile
    public void test62_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1[] va = new MyValue1[len];
        Integer[] oa = new Integer[len];
        for (int i = 0; i < len; ++i) {
            oa[i] = Integer.valueOf(rI);
        }
        test62_helper(42, va, oa);
        Object[] result = test62(va, oa);
        for (int i = 0; i < va.length; ++i) {
            Asserts.assertEQ(oa[i], result[i]);
        }
    }

    @ForceInline
    public Object[] test63_helper(int i, MyValue1[] va, Integer[] oa) {
        Object[] arr = null;
        if (i == 10) {
            arr = va;
        } else {
            arr = oa;
        }
        return arr;
    }

    @Test
    public Object[] test63(MyValue1[] va, Integer[] oa) {
        int i = 0;
        for (; i < 10; i++);

        Object[] arr = test63_helper(i, va, oa);

        return Arrays.copyOf(arr, arr.length+1, arr.getClass());
    }

    @DontCompile
    public void test63_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1[] va = new MyValue1[len];
        MyValue1[] verif = new MyValue1[len+1];
        for (int i = 0; i < len; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
            verif[i] = va[i];
        }
        Integer[] oa = new Integer[len];
        test63_helper(42, va, oa);
        Object[] result = test63(va, oa);
        verify(verif, result);
    }

    // Test default initialization of inline type arrays: small array
    @Test
    public MyValue1[] test64() {
        return new MyValue1[8];
    }

    @DontCompile
    public void test64_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[8];
        MyValue1[] var = test64();
        for (int i = 0; i < 8; ++i) {
            Asserts.assertEQ(va[i].hashPrimitive(), var[i].hashPrimitive());
        }
    }

    // Test default initialization of inline type arrays: large array
    @Test
    public MyValue1[] test65() {
        return new MyValue1[32];
    }

    @DontCompile
    public void test65_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[32];
        MyValue1[] var = test65();
        for (int i = 0; i < 32; ++i) {
            Asserts.assertEQ(va[i].hashPrimitive(), var[i].hashPrimitive());
        }
    }

    // Check init store elimination
    @Test(match = { ALLOCA }, matchCount = { 1 })
    public MyValue1[] test66(MyValue1 vt) {
        MyValue1[] va = new MyValue1[1];
        va[0] = vt;
        return va;
    }

    @DontCompile
    public void test66_verifier(boolean warmup) {
        MyValue1 vt = MyValue1.createWithFieldsDontInline(rI, rL);
        MyValue1[] va = test66(vt);
        Asserts.assertEQ(va[0].hashPrimitive(), vt.hashPrimitive());
    }

    // Zeroing elimination and arraycopy
    @Test
    public MyValue1[] test67(MyValue1[] src) {
        MyValue1[] dst = new MyValue1[16];
        System.arraycopy(src, 0, dst, 0, 13);
        return dst;
    }

    @DontCompile
    public void test67_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[16];
        MyValue1[] var = test67(va);
        for (int i = 0; i < 16; ++i) {
            Asserts.assertEQ(va[i].hashPrimitive(), var[i].hashPrimitive());
        }
    }

    // A store with a default value can be eliminated
    @Test
    public MyValue1[] test68() {
        MyValue1[] va = new MyValue1[2];
        va[0] = va[1];
        return va;
    }

    @DontCompile
    public void test68_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[2];
        MyValue1[] var = test68();
        for (int i = 0; i < 2; ++i) {
            Asserts.assertEQ(va[i].hashPrimitive(), var[i].hashPrimitive());
        }
    }

    // Requires individual stores to init array
    @Test
    public MyValue1[] test69(MyValue1 vt) {
        MyValue1[] va = new MyValue1[4];
        va[0] = vt;
        va[3] = vt;
        return va;
    }

    @DontCompile
    public void test69_verifier(boolean warmup) {
        MyValue1 vt = MyValue1.createWithFieldsDontInline(rI, rL);
        MyValue1[] va = new MyValue1[4];
        va[0] = vt;
        va[3] = vt;
        MyValue1[] var = test69(vt);
        for (int i = 0; i < va.length; ++i) {
            Asserts.assertEQ(va[i].hashPrimitive(), var[i].hashPrimitive());
        }
    }

    // A store with a default value can be eliminated: same as test68
    // but store is farther away from allocation
    @Test
    public MyValue1[] test70(MyValue1[] other) {
        other[1] = other[0];
        MyValue1[] va = new MyValue1[2];
        other[0] = va[1];
        va[0] = va[1];
        return va;
    }

    @DontCompile
    public void test70_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[2];
        MyValue1[] var = test70(va);
        for (int i = 0; i < 2; ++i) {
            Asserts.assertEQ(va[i].hashPrimitive(), var[i].hashPrimitive());
        }
    }

    // EA needs to consider oop fields in flattened arrays
    @Test
    public void test71() {
        int len = 10;
        MyValue2[] src = new MyValue2[len];
        MyValue2[] dst = new MyValue2[len];
        for (int i = 0; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsDontInline(rI+i, rD+i);
        }
        System.arraycopy(src, 0, dst, 0, src.length);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(src[i].hash(), dst[i].hash());
        }
    }

    @DontCompile
    public void test71_verifier(boolean warmup) {
        test71();
    }

    // Test EA with leaf call to 'store_unknown_inline'
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

    @DontCompile
    public void test72_verifier(boolean warmup) {
        Object[] arr = new Object[1];
        Object elem = new Object();
        test72(arr, true, elem);
        test72(arr, false, elem);
    }

    @Test
    public void test73(Object[] oa, MyValue1 v, Object o) {
        // TestLWorld.test38 use a C1 Phi node for the array. This test
        // adds the case where the stored value is a C1 Phi node.
        Object o2 = (o == null) ? v : o;
        oa[0] = v;  // The stored value is known to be flattenable
        oa[1] = o;  // The stored value may be flattenable
        oa[2] = o2; // The stored value may be flattenable (a C1 Phi node)
        oa[0] = oa; // The stored value is known to be not flattenable (an Object[])
    }

    @DontCompile
    public void test73_verifier(boolean warmup) {
        MyValue1 v0 = MyValue1.createWithFieldsDontInline(rI, rL);
        MyValue1 v1 = MyValue1.createWithFieldsDontInline(rI+1, rL+1);
        MyValue1[] arr = new MyValue1[3];
        try {
            test73(arr, v0, v1);
            throw new RuntimeException("ArrayStoreException expected");
        } catch (ArrayStoreException e) {
            // expected
        }
        Asserts.assertEQ(arr[0].hash(), v0.hash());
        Asserts.assertEQ(arr[1].hash(), v1.hash());
        Asserts.assertEQ(arr[2].hash(), v1.hash());
    }

    public static void test74Callee(MyValue1[] va) { }

    // Tests invoking unloaded method with inline type array in signature
    @Test
    @Warmup(0)
    public void test74(MethodHandle m, MyValue1[] va) throws Throwable {
        m.invoke(va);
    }

    @DontCompile
    public void test74_verifier(boolean warmup) throws Throwable {
        MethodHandle m = MethodHandles.lookup().findStatic(TestArrays.class, "test74Callee", MethodType.methodType(void.class, MyValue1[].class));
        MyValue1[] va = new MyValue1[0];
        test74(m, va);
    }

    // Some more array clone tests
    @ForceInline
    public Object[] test75_helper(int i, MyValue1[] va, Integer[] oa) {
        Object[] arr = null;
        if (i == 10) {
            arr = oa;
        } else {
            arr = va;
        }
        return arr;
    }

    @Test
    public Object[] test75(MyValue1[] va, Integer[] oa) {
        int i = 0;
        for (; i < 10; i++);

        Object[] arr = test75_helper(i, va, oa);
        return arr.clone();
    }

    @DontCompile
    public void test75_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1[] va = new MyValue1[len];
        Integer[] oa = new Integer[len];
        for (int i = 0; i < len; ++i) {
            oa[i] = Integer.valueOf(rI);
        }
        test75_helper(42, va, oa);
        Object[] result = test75(va, oa);

        for (int i = 0; i < va.length; ++i) {
            Asserts.assertEQ(oa[i], result[i]);
            // Check that array has correct properties (null-ok)
            result[i] = null;
        }
    }

    @ForceInline
    public Object[] test76_helper(int i, MyValue1[] va, Integer[] oa) {
        Object[] arr = null;
        if (i == 10) {
            arr = va;
        } else {
            arr = oa;
        }
        return arr;
    }

    @Test
    public Object[] test76(MyValue1[] va, Integer[] oa) {
        int i = 0;
        for (; i < 10; i++);

        Object[] arr = test76_helper(i, va, oa);
        return arr.clone();
    }

    @DontCompile
    public void test76_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1[] va = new MyValue1[len];
        MyValue1[] verif = new MyValue1[len];
        for (int i = 0; i < len; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
            verif[i] = va[i];
        }
        Integer[] oa = new Integer[len];
        test76_helper(42, va, oa);
        Object[] result = test76(va, oa);
        verify(verif, result);
        // Check that array has correct properties (null-free)
        if (len > 0) {
            try {
                result[0] = null;
                throw new RuntimeException("Should throw NullPointerException");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    @Test
    public void test77() {
        MyValue1 v0 = MyValue1.createWithFieldsDontInline(rI, rL);
        MyValue1 v1 = MyValue1.createWithFieldsDontInline(rI+1, rL+1);
        MyValue1[] arr = new MyValue1[1];

        Object[] oa = arr;
        Object o1 = v1;
        Object o = (o1 == null) ? v0 : o1;

        oa[0] = o; // For C1, due to IfOp optimization, the declared_type of o becomes NULL.

        Asserts.assertEQ(arr[0].hash(), v1.hash());
    }


    @DontCompile
    public void test77_verifier(boolean warmup) {
        test77();
    }

    @Test
    public long test78(MyValue1 v, int n) {
        long x = 0;
        for (int i = 0; i<n; i++) {
        }

        MyValue1[] a = new MyValue1[n];
        a[0] = v;
        for (int i = 0; i<n; i++) {
            x += a[i].hash(); // C1 PhiSimplifier changes "a" from a Phi node to a NewObjectArray node
        }

        return x;
    }

    @DontCompile
    public void test78_verifier(boolean warmup) {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        Asserts.assertEQ(test78(v, 1), v.hash());
    }

    // Verify that casting an array element to a non-flattenable type marks the array as not-flat
    @Test(valid = InlineTypeArrayFlattenOn, match = { ALLOC_G, LOAD_UNKNOWN_INLINE }, matchCount = { 1, 1 })
    @Test(valid = InlineTypeArrayFlattenOff, failOn = ALLOC_G + ALLOCA_G + LOAD_UNKNOWN_INLINE)
    public Object test79(Object[] array, int i) {
        Integer i1 = (Integer)array[0];
        Object o = array[1];
        return array[i];
    }

    @DontCompile
    public void test79_verifier(boolean warmup) {
        Integer i = Integer.valueOf(rI);
        Integer[] array = new Integer[2];
        array[1] = i;
        Object result = test79(array, 1);
        Asserts.assertEquals(result, i);
    }

    primitive static class NotFlattenable {
        private final Object o1 = null;
        private final Object o2 = null;
        private final Object o3 = null;
        private final Object o4 = null;
        private final Object o5 = null;
        private final Object o6 = null;
    }

    // Same as test79 but with not-flattenable inline type
    @Test(valid = InlineTypeArrayFlattenOn, match = { ALLOC_G, LOAD_UNKNOWN_INLINE }, matchCount = { 1, 1 })
    @Test(valid = InlineTypeArrayFlattenOff, failOn = ALLOC_G + ALLOCA_G + LOAD_UNKNOWN_INLINE)
    public Object test80(Object[] array, int i) {
        NotFlattenable vt = (NotFlattenable)array[0];
        Object o = array[1];
        return array[i];
    }

    @DontCompile
    public void test80_verifier(boolean warmup) {
        NotFlattenable vt = new NotFlattenable();
        NotFlattenable[] array = new NotFlattenable[2];
        array[1] = vt;
        Object result = test80(array, 1);
        Asserts.assertEquals(result, vt);
    }

    // Verify that writing an object of a non-inline, non-null type to an array marks the array as not-null-free and not-flat
    @Test(failOn = ALLOC_G + ALLOCA_G + LOAD_UNKNOWN_INLINE + STORE_UNKNOWN_INLINE + INLINE_ARRAY_NULL_GUARD)
    public Object test81(Object[] array, Integer v, Object o, int i) {
        if (v == null) {
          return null;
        }
        array[0] = v;
        array[1] = array[0];
        array[2] = o;
        return array[i];
    }

    @DontCompile
    public void test81_verifier(boolean warmup) {
        Integer i = Integer.valueOf(rI);
        Integer[] array1 = new Integer[3];
        Object[] array2 = new Object[3];
        Object result = test81(array1, i, i, 0);
        Asserts.assertEquals(array1[0], i);
        Asserts.assertEquals(array1[1], i);
        Asserts.assertEquals(array1[2], i);
        Asserts.assertEquals(result, i);
        result = test81(array2, i, i, 1);
        Asserts.assertEquals(array2[0], i);
        Asserts.assertEquals(array2[1], i);
        Asserts.assertEquals(array2[2], i);
        Asserts.assertEquals(result, i);
    }

    // Verify that writing an object of a non-flattenable inline type to an array marks the array as not-flat
    @Test(valid = InlineTypePassFieldsAsArgsOn, failOn = ALLOCA_G + LOAD_UNKNOWN_INLINE + STORE_UNKNOWN_INLINE)
    @Test(valid = InlineTypePassFieldsAsArgsOff, failOn = ALLOC_G + ALLOCA_G + LOAD_UNKNOWN_INLINE + STORE_UNKNOWN_INLINE)
    public Object test82(Object[] array, NotFlattenable vt, Object o, int i) {
        array[0] = vt;
        array[1] = array[0];
        array[2] = o;
        return array[i];
    }

    @DontCompile
    public void test82_verifier(boolean warmup) {
        NotFlattenable vt = new NotFlattenable();
        NotFlattenable[] array1 = new NotFlattenable[3];
        Object[] array2 = new Object[3];
        Object result = test82(array1, vt, vt, 0);
        Asserts.assertEquals(array1[0], vt);
        Asserts.assertEquals(array1[1], vt);
        Asserts.assertEquals(array1[2], vt);
        Asserts.assertEquals(result, vt);
        result = test82(array2, vt, vt, 1);
        Asserts.assertEquals(array2[0], vt);
        Asserts.assertEquals(array2[1], vt);
        Asserts.assertEquals(array2[2], vt);
        Asserts.assertEquals(result, vt);
    }

    // Verify that casting an array element to a non-inline type type marks the array as not-null-free and not-flat
    @Test(valid = InlineTypeArrayFlattenOn, match = { ALLOC_G, LOAD_UNKNOWN_INLINE }, matchCount = { 1, 1 }, failOn = ALLOCA_G + STORE_UNKNOWN_INLINE + INLINE_ARRAY_NULL_GUARD)
    @Test(valid = InlineTypeArrayFlattenOff, failOn = ALLOC_G + ALLOCA_G + LOAD_UNKNOWN_INLINE + STORE_UNKNOWN_INLINE + INLINE_ARRAY_NULL_GUARD)
    public void test83(Object[] array, Object o) {
        Integer i = (Integer)array[0];
        array[1] = o;
    }

    @DontCompile
    public void test83_verifier(boolean warmup) {
        Integer i = Integer.valueOf(rI);
        Integer[] array1 = new Integer[2];
        Object[] array2 = new Object[2];
        test83(array1, i);
        Asserts.assertEquals(array1[1], i);
        test83(array2, null);
        Asserts.assertEquals(array2[1], null);
    }

    // Verify that writing constant null into an array marks the array as not-null-free and not-flat
    @Test(failOn = ALLOC_G + ALLOCA_G + LOAD_UNKNOWN_INLINE + STORE_UNKNOWN_INLINE, match = { INLINE_ARRAY_NULL_GUARD }, matchCount = { 1 })
    public Object test84(Object[] array, int i) {
        array[0] = null;
        array[1] = null;
        return array[i];
    }

    @DontCompile
    public void test84_verifier(boolean warmup) {
        NotFlattenable.ref[] array1 = new NotFlattenable.ref[2];
        Object[] array2 = new Object[2];
        Object result = test84(array1, 0);
        Asserts.assertEquals(array1[0], null);
        Asserts.assertEquals(result, null);
        result = test84(array2, 1);
        Asserts.assertEquals(array2[0], null);
        Asserts.assertEquals(result, null);
        if (!warmup) {
            NotFlattenable[] array3 = new NotFlattenable[2];
            try {
                test84(array3, 1);
                throw new RuntimeException("Should throw NullPointerException");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    // Same as test84 but with branches
    @Test(failOn = ALLOC_G + ALLOCA_G + LOAD_UNKNOWN_INLINE + STORE_UNKNOWN_INLINE, match = { INLINE_ARRAY_NULL_GUARD }, matchCount = { 2 })
    public void test85(Object[] array, Object o, boolean b) {
        if (b) {
            array[0] = null;
        } else {
            array[1] = null;
        }
        array[1] = o;
    }

    @DontCompile
    public void test85_verifier(boolean warmup) {
        Integer i = Integer.valueOf(rI);
        Integer[] array1 = new Integer[2];
        Object[] array2 = new Object[2];
        test85(array1, i, true);
        Asserts.assertEquals(array1[1], i);
        test85(array1, null, false);
        Asserts.assertEquals(array1[1], null);
        test85(array2, i, true);
        Asserts.assertEquals(array2[1], i);
        test85(array2, null, false);
        Asserts.assertEquals(array2[1], null);
        if (!warmup) {
            NotFlattenable[] array3 = new NotFlattenable[2];
            try {
                test85(array3, null, true);
                throw new RuntimeException("Should throw NullPointerException");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    // Same as test85 but with not-flattenable inline type array
    @Test(failOn = ALLOC_G + ALLOCA_G + LOAD_UNKNOWN_INLINE + STORE_UNKNOWN_INLINE, match = { INLINE_ARRAY_NULL_GUARD }, matchCount = { 2 })
    public void test86(NotFlattenable.ref[] array, NotFlattenable.ref o, boolean b) {
        if (b) {
            array[0] = null;
        } else {
            array[1] = null;
        }
        array[1] = o;
    }

    @DontCompile
    public void test86_verifier(boolean warmup) {
        NotFlattenable vt = new NotFlattenable();
        NotFlattenable.ref[] array1 = new NotFlattenable.ref[2];
        test86(array1, vt, true);
        Asserts.assertEquals(array1[1], vt);
        test86(array1, null, false);
        Asserts.assertEquals(array1[1], null);
        if (!warmup) {
            NotFlattenable[] array2 = new NotFlattenable[2];
            try {
                test86(array2, null, true);
                throw new RuntimeException("Should throw NullPointerException");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    // Same as test85 but with inline type array
    @Test(failOn = ALLOC_G + ALLOCA_G + LOAD_UNKNOWN_INLINE + STORE_UNKNOWN_INLINE, match = { INLINE_ARRAY_NULL_GUARD }, matchCount = { 2 })
    public void test87(MyValue1.ref[] array, MyValue1.ref o, boolean b) {
        if (b) {
            array[0] = null;
        } else {
            array[1] = null;
        }
        array[1] = o;
    }

    @DontCompile
    public void test87_verifier(boolean warmup) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1.ref[] array1 = new MyValue1.ref[2];
        test87(array1, vt, true);
        Asserts.assertEquals(array1[1], vt);
        test87(array1, null, false);
        Asserts.assertEquals(array1[1], null);
        if (!warmup) {
            MyValue1[] array2 = new MyValue1[2];
            try {
                test87(array2, null, true);
                throw new RuntimeException("Should throw NullPointerException");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    // Additional correctness tests to make sure we have the required null checks
    @Test()
    public void test88(Object[] array, Integer v) {
        array[0] = v;
    }

    @DontCompile
    public void test88_verifier(boolean warmup) {
        Integer[] array1 = new Integer[1];
        Object[] array2 = new Object[1];
        test88(array1, null);
        Asserts.assertEquals(array1[0], null);
        test88(array2, null);
        Asserts.assertEquals(array2[0], null);
        if (!warmup) {
            MyValue1[] array3 = new MyValue1[1];
            try {
                test88(array3, null);
                throw new RuntimeException("Should throw NullPointerException");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    @Test()
    public void test89(MyValue1.ref[] array, Integer v) {
        Object o = v;
        array[0] = (MyValue1.ref)o;
    }

    @DontCompile
    public void test89_verifier(boolean warmup) {
        MyValue1.ref[] array1 = new MyValue1.ref[1];
        test89(array1, null);
        Asserts.assertEquals(array1[0], null);
        if (!warmup) {
            MyValue1[] array2 = new MyValue1[1];
            try {
                test89(array2, null);
                throw new RuntimeException("Should throw NullPointerException");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    @Test
    public boolean test90() {
        boolean b = true;

        MyValue1[] qArray = new MyValue1[0];
        MyValue1.ref[] lArray = new MyValue1.ref[0];

        b = b && (qArray instanceof MyValue1[]);
        b = b && (lArray instanceof MyValue1.ref[]);

        MyValue1[][] qArray2 = new MyValue1[0][0];
        MyValue1.ref[][] lArray2 = new MyValue1.ref[0][0];

        b = b && (qArray2 instanceof MyValue1[][]);
        b = b && (lArray2 instanceof MyValue1.ref[][]);

        return b;
    }

    @DontCompile
    public void test90_verifier(boolean warmup) {
        Asserts.assertEQ(test90(), true);
    }

    primitive static final class Test91Value {
        public final int f0;
        public final int f1;
        public final int f2;
        public final int f3;
        public final int f4;
        public final int f5;

        public Test91Value(int i) {
            this.f0 = i;
            this.f1 = i;
            this.f2 = i;
            this.f3 = i;
            this.f4 = i;
            this.f5 = i;
        }

        public void verify() {
            if ((f0 != f1) || (f1 != f2) || (f2 != f3) || (f3 != f4) || (f4 != f5)) {
                throw new RuntimeException("test91 failed");
            }
        }
    }

    // Test anti-dependencies between loads and stores from flattened array
    @Test
    @Warmup(0)
    public int test91(Test91Value[] array, int lo, int val) {
        int i = 3;
        while (lo < i) {
            Test91Value tmp = array[lo];
            array[lo++] = array[i];
            array[i--] = tmp;
        }
        return val;
    }

    @DontCompile
    public void test91_verifier(boolean warmup) {
        Test91Value[] array = new Test91Value[5];
        for (int i = 0; i < 5; ++i) {
            array[i] = new Test91Value(i);
            array[i].verify();
        }
        Asserts.assertEQ(test91(array, 0, 5), 5);
        for (int i = 0; i < 5; ++i) {
            array[i].verify();
        }
    }

    @Test
    public void test92(Object[] src, Object[] dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @DontCompile
    public void test92_verifier(boolean warmup) {
        MyValue1[] a = new MyValue1[1];
        MyValue1[] b = new MyValue1[1];
        try {
            test92(a, null);
            throw new RuntimeException("Should throw NullPointerException");
        } catch (NullPointerException expected) {}

        try {
            test92(null, b);
            throw new RuntimeException("Should throw NullPointerException");
        } catch (NullPointerException expected) {}

        a[0] = MyValue1.createWithFieldsInline(rI, rL);
        test92(a, b);
        verify(a, b);
    }

    // Same as test30 but accessing all elements of the non-escaping array
    @Test
    @Warmup(10000)
    public long test93(MyValue2[] src, boolean flag) {
        MyValue2[] dst = new MyValue2[10];
        System.arraycopy(src, 0, dst, 0, 10);
        if (flag) {  }
        return dst[0].hash() + dst[1].hash() + dst[2].hash() + dst[3].hash() + dst[4].hash() +
               dst[5].hash() + dst[6].hash() + dst[7].hash() + dst[8].hash() + dst[9].hash();
    }

    @DontCompile
    public void test93_verifier(boolean warmup) {
        MyValue2[] src = new MyValue2[10];
        for (int i = 0; i < 10; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        long res = test93(src, !warmup);
        long expected = 0;
        for (int i = 0; i < 10; ++i) {
            expected += src[i].hash();
        }
        Asserts.assertEQ(res, expected);
    }

    // Same as test93 but with variable source array offset
    @Test
    @Warmup(10000)
    public long test94(MyValue2[] src, int i, boolean flag) {
        MyValue2[] dst = new MyValue2[10];
        System.arraycopy(src, i, dst, 0, 1);
        if (flag) {  }
        return dst[0].hash() + dst[1].hash() + dst[2].hash() + dst[3].hash() + dst[4].hash() +
               dst[5].hash() + dst[6].hash() + dst[7].hash() + dst[8].hash() + dst[9].hash();
    }

    @DontCompile
    public void test94_verifier(boolean warmup) {
        MyValue2[] src = new MyValue2[10];
        for (int i = 0; i < 10; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        for (int i = 0; i < 10; ++i) {
            long res = test94(src, i, !warmup);
            long expected = src[i].hash() + 9*MyValue2.default.hash();
            Asserts.assertEQ(res, expected);
        }
    }

    // Test propagation of not null-free/flat information
    @Test(failOn = CHECKCAST_ARRAY)
    public MyValue1[] test95(Object[] array) {
        array[0] = null;
        // Always throws a ClassCastException because we just successfully
        // stored null and therefore the array can't be an inline type array.
        return (MyValue1[])array;
    }

    @DontCompile
    public void test95_verifier(boolean warmup) {
        MyValue1[] array1 = new MyValue1[1];
        Integer[] array2 = new Integer[1];
        try {
            test95(array1);
            throw new RuntimeException("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
        try {
            test95(array2);
            throw new RuntimeException("Should throw ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    // Same as test95 but with cmp user of cast result
    @Test(failOn = CHECKCAST_ARRAY)
    public boolean test96(Object[] array) {
        array[0] = null;
        // Always throws a ClassCastException because we just successfully
        // stored null and therefore the array can't be an inline type array.
        MyValue1[] casted = (MyValue1[])array;
        return casted != null;
    }

    @DontCompile
    public void test96_verifier(boolean warmup) {
        MyValue1[] array1 = new MyValue1[1];
        Integer[] array2 = new Integer[1];
        try {
            test96(array1);
            throw new RuntimeException("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
        try {
            test96(array2);
            throw new RuntimeException("Should throw ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    // Same as test95 but with instanceof instead of cast
    @Test(failOn = CHECKCAST_ARRAY)
    public boolean test97(Object[] array) {
        array[0] = 42;
        // Always throws a ClassCastException because we just successfully stored
        // a non-inline value and therefore the array can't be an inline type array.
        return array instanceof MyValue1[];
    }

    @DontCompile
    public void test97_verifier(boolean warmup) {
        MyValue1[] array1 = new MyValue1[1];
        Integer[] array2 = new Integer[1];
        try {
            test97(array1);
            throw new RuntimeException("Should throw ArrayStoreException");
        } catch (ArrayStoreException e) {
            // Expected
        }
        boolean res = test97(array2);
        Asserts.assertFalse(res);
    }

    // Same as test95 but with non-flattenable store
    @Test(valid = InlineTypeArrayFlattenOn, failOn = CHECKCAST_ARRAY)
    @Test(valid = InlineTypeArrayFlattenOff)
    public MyValue1[] test98(Object[] array) {
        array[0] = NotFlattenable.default;
        // Always throws a ClassCastException because we just successfully stored a
        // non-flattenable value and therefore the array can't be a flat array.
        return (MyValue1[])array;
    }

    @DontCompile
    public void test98_verifier(boolean warmup) {
        MyValue1[] array1 = new MyValue1[1];
        NotFlattenable[] array2 = new NotFlattenable[1];
        try {
            test98(array1);
            throw new RuntimeException("Should throw ArrayStoreException");
        } catch (ArrayStoreException e) {
            // Expected
        }
        try {
            test98(array2);
            throw new RuntimeException("Should throw ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    // Same as test98 but with cmp user of cast result
    @Test(valid = InlineTypeArrayFlattenOn, failOn = CHECKCAST_ARRAY)
    @Test(valid = InlineTypeArrayFlattenOff)
    public boolean test99(Object[] array) {
        array[0] = NotFlattenable.default;
        // Always throws a ClassCastException because we just successfully stored a
        // non-flattenable value and therefore the array can't be a flat array.
        MyValue1[] casted = (MyValue1[])array;
        return casted != null;
    }

    @DontCompile
    public void test99_verifier(boolean warmup) {
        MyValue1[] array1 = new MyValue1[1];
        NotFlattenable[] array2 = new NotFlattenable[1];
        try {
            test99(array1);
            throw new RuntimeException("Should throw ArrayStoreException");
        } catch (ArrayStoreException e) {
            // Expected
        }
        try {
            test99(array2);
            throw new RuntimeException("Should throw ClassCastException");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    // Same as test98 but with instanceof instead of cast
    @Test(valid = InlineTypeArrayFlattenOn, failOn = CHECKCAST_ARRAY)
    @Test(valid = InlineTypeArrayFlattenOff)
    public boolean test100(Object[] array) {
        array[0] = NotFlattenable.default;
        // Always throws a ClassCastException because we just successfully stored a
        // non-flattenable value and therefore the array can't be a flat array.
        return array instanceof MyValue1[];
    }

    @DontCompile
    public void test100_verifier(boolean warmup) {
        MyValue1[] array1 = new MyValue1[1];
        NotFlattenable[] array2 = new NotFlattenable[1];
        try {
            test100(array1);
            throw new RuntimeException("Should throw ArrayStoreException");
        } catch (ArrayStoreException e) {
            // Expected
        }
        boolean res = test100(array2);
        Asserts.assertFalse(res);
    }

    // Test that CHECKCAST_ARRAY matching works as expected
    @Test(match = { CHECKCAST_ARRAY }, matchCount = { 1 })
    public boolean test101(Object[] array) {
        return array instanceof MyValue1[];
    }

    @DontCompile
    public void test101_verifier(boolean warmup) {
        MyValue1[] array1 = new MyValue1[1];
        NotFlattenable[] array2 = new NotFlattenable[1];
        Asserts.assertTrue(test101(array1));
        Asserts.assertFalse(test101(array2));
    }

    static final MyValue2[] val_src = new MyValue2[8];
    static final MyValue2[] val_dst = new MyValue2[8];
    static final Object[]   obj_src = new Object[8];
    static final Object[]   obj_null_src = new Object[8];
    static final Object[]   obj_dst = new Object[8];

    static Object get_val_src() { return val_src; }
    static Object get_val_dst() { return val_dst; }
    static Class get_val_class() { return MyValue2[].class; }
    static Class get_int_class() { return Integer[].class; }
    static Object get_obj_src() { return obj_src; }
    static Object get_obj_null_src() { return obj_null_src; }
    static Object get_obj_dst() { return obj_dst; }
    static Class get_obj_class() { return Object[].class; }

    static {
        for (int i = 0; i < 8; ++i) {
            val_src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
            obj_src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
            obj_null_src[i] = MyValue2.createWithFieldsInline(rI+i, rD+i);
        }
        obj_null_src[0] = null;
    }

    // Arraycopy with constant source and destination arrays
    @Test(valid = InlineTypeArrayFlattenOn, match = { INTRINSIC_SLOW_PATH }, matchCount = { 1 })
    @Test(valid = InlineTypeArrayFlattenOff, failOn = INTRINSIC_SLOW_PATH)
    public void test102() {
        System.arraycopy(val_src, 0, obj_dst, 0, 8);
    }

    @DontCompile
    public void test102_verifier(boolean warmup) {
        test102();
        verify(val_src, obj_dst);
    }

    // Same as test102 but with MyValue2[] dst
    @Test(failOn = INTRINSIC_SLOW_PATH)
    public void test103() {
        System.arraycopy(val_src, 0, val_dst, 0, 8);
    }

    @DontCompile
    public void test103_verifier(boolean warmup) {
        test103();
        verify(val_src, val_dst);
    }

    // Same as test102 but with Object[] src
    @Test(failOn = INTRINSIC_SLOW_PATH)
    public void test104() {
        System.arraycopy(obj_src, 0, obj_dst, 0, 8);
    }

    @DontCompile
    public void test104_verifier(boolean warmup) {
        test104();
        verify(obj_src, obj_dst);
    }

    // Same as test103 but with Object[] src
    @Test(match = { INTRINSIC_SLOW_PATH }, matchCount = { 1 })
    public void test105() {
        System.arraycopy(obj_src, 0, val_dst, 0, 8);
    }

    @DontCompile
    public void test105_verifier(boolean warmup) {
        test105();
        verify(obj_src, val_dst);
    }

    // Same as test103 but with Object[] src containing null
    @Test(match = { INTRINSIC_SLOW_PATH }, matchCount = { 1 })
    public void test105_null() {
        System.arraycopy(obj_null_src, 0, val_dst, 0, 8);
    }

    @DontCompile
    public void test105_null_verifier(boolean warmup) {
        try {
            test105_null();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    // Below tests are equal to test102-test105 but hide the src/dst types until
    // after the arraycopy intrinsic is emitted (with incremental inlining).

    @Test(valid = InlineTypeArrayFlattenOn, match = { INTRINSIC_SLOW_PATH }, matchCount = { 1 })
    @Test(valid = InlineTypeArrayFlattenOff, failOn = INTRINSIC_SLOW_PATH)
    public void test106() {
        System.arraycopy(get_val_src(), 0, get_obj_dst(), 0, 8);
    }

    @DontCompile
    public void test106_verifier(boolean warmup) {
        test106();
        verify(val_src, obj_dst);
    }

    // TODO 8251971: Should be optimized but we are bailing out because
    // at parse time it looks as if src could be flat and dst could be not flat.
    @Test(valid = InlineTypeArrayFlattenOn)
    @Test(valid = InlineTypeArrayFlattenOff, failOn = INTRINSIC_SLOW_PATH)
    public void test107() {
        System.arraycopy(get_val_src(), 0, get_val_dst(), 0, 8);
    }

    @DontCompile
    public void test107_verifier(boolean warmup) {
        test107();
        verify(val_src, val_dst);
    }

    @Test(failOn = INTRINSIC_SLOW_PATH)
    public void test108() {
        System.arraycopy(get_obj_src(), 0, get_obj_dst(), 0, 8);
    }

    @DontCompile
    public void test108_verifier(boolean warmup) {
        test108();
        verify(obj_src, obj_dst);
    }

    @Test(match = { INTRINSIC_SLOW_PATH }, matchCount = { 1 })
    public void test109() {
        System.arraycopy(get_obj_src(), 0, get_val_dst(), 0, 8);
    }

    @DontCompile
    public void test109_verifier(boolean warmup) {
        test109();
        verify(obj_src, val_dst);
    }

    @Test(match = { INTRINSIC_SLOW_PATH }, matchCount = { 1 })
    public void test109_null() {
        System.arraycopy(get_obj_null_src(), 0, get_val_dst(), 0, 8);
    }

    @DontCompile
    public void test109_null_verifier(boolean warmup) {
        try {
            test109_null();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    // Arrays.copyOf with constant source and destination arrays
    @Test(valid = InlineTypeArrayFlattenOn, match = { INTRINSIC_SLOW_PATH }, matchCount = { 1 })
    @Test(valid = InlineTypeArrayFlattenOff, failOn = INTRINSIC_SLOW_PATH + CLASS_CHECK_TRAP)
    public Object[] test110() {
        return Arrays.copyOf(val_src, 8, Object[].class);
    }

    @DontCompile
    public void test110_verifier(boolean warmup) {
        Object[] res = test110();
        verify(val_src, res);
    }

    // Same as test110 but with MyValue2[] dst
    @Test(failOn = INTRINSIC_SLOW_PATH + CLASS_CHECK_TRAP)
    public Object[] test111() {
        return Arrays.copyOf(val_src, 8, MyValue2[].class);
    }

    @DontCompile
    public void test111_verifier(boolean warmup) {
        Object[] res = test111();
        verify(val_src, res);
    }

    // Same as test110 but with Object[] src
    @Test(failOn = INTRINSIC_SLOW_PATH + CLASS_CHECK_TRAP)
    public Object[] test112() {
        return Arrays.copyOf(obj_src, 8, Object[].class);
    }

    @DontCompile
    public void test112_verifier(boolean warmup) {
        Object[] res = test112();
        verify(obj_src, res);
    }

    // Same as test111 but with Object[] src
    @Test(match = { INTRINSIC_SLOW_PATH + CLASS_CHECK_TRAP }, matchCount = { 1 })
    public Object[] test113() {
        return Arrays.copyOf(obj_src, 8, MyValue2[].class);
    }

    @DontCompile
    public void test113_verifier(boolean warmup) {
        Object[] res = test113();
        verify(obj_src, res);
    }

    // Same as test111 but with Object[] src containing null
    @Test(match = { INTRINSIC_SLOW_PATH + CLASS_CHECK_TRAP }, matchCount = { 1 })
    public Object[] test113_null() {
        return Arrays.copyOf(obj_null_src, 8, MyValue2[].class);
    }

    @DontCompile
    public void test113_null_verifier(boolean warmup) {
        try {
            test113_null();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    // Below tests are equal to test110-test113 but hide the src/dst types until
    // after the arraycopy intrinsic is emitted (with incremental inlining).

    @Test(valid = InlineTypeArrayFlattenOn, match = { INTRINSIC_SLOW_PATH }, matchCount = { 1 })
    @Test(valid = InlineTypeArrayFlattenOff, failOn = INTRINSIC_SLOW_PATH + CLASS_CHECK_TRAP)
    public Object[] test114() {
        return Arrays.copyOf((Object[])get_val_src(), 8, get_obj_class());
    }

    @DontCompile
    public void test114_verifier(boolean warmup) {
        Object[] res = test114();
        verify(val_src, res);
    }

    // TODO 8251971: Should be optimized but we are bailing out because
    // at parse time it looks as if src could be flat and dst could be not flat
    @Test(valid = InlineTypeArrayFlattenOn)
    @Test(valid = InlineTypeArrayFlattenOff, failOn = INTRINSIC_SLOW_PATH + CLASS_CHECK_TRAP)
    public Object[] test115() {
        return Arrays.copyOf((Object[])get_val_src(), 8, get_val_class());
    }

    @DontCompile
    public void test115_verifier(boolean warmup) {
        Object[] res = test115();
        verify(val_src, res);
    }

    @Test(failOn = INTRINSIC_SLOW_PATH + CLASS_CHECK_TRAP)
    public Object[] test116() {
        return Arrays.copyOf((Object[])get_obj_src(), 8, get_obj_class());
    }

    @DontCompile
    public void test116_verifier(boolean warmup) {
        Object[] res = test116();
        verify(obj_src, res);
    }

    @Test(match = { INTRINSIC_SLOW_PATH + CLASS_CHECK_TRAP }, matchCount = { 1 })
    public Object[] test117() {
        return Arrays.copyOf((Object[])get_obj_src(), 8, get_val_class());
    }

    @DontCompile
    public void test117_verifier(boolean warmup) {
        Object[] res = test117();
        verify(obj_src, res);
    }

    @Test(match = { INTRINSIC_SLOW_PATH + CLASS_CHECK_TRAP }, matchCount = { 1 })
    public Object[] test117_null() {
        return Arrays.copyOf((Object[])get_obj_null_src(), 8, get_val_class());
    }

    @DontCompile
    public void test117_null_verifier(boolean warmup) {
        try {
            test117_null();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    // Some more Arrays.copyOf tests with only constant class

    @Test(match = { CLASS_CHECK_TRAP }, matchCount = { 1 }, failOn = INTRINSIC_SLOW_PATH)
    public Object[] test118(Object[] src) {
        return Arrays.copyOf(src, 8, MyValue2[].class);
    }

    @DontCompile
    public void test118_verifier(boolean warmup) {
        Object[] res = test118(obj_src);
        verify(obj_src, res);
        res = test118(val_src);
        verify(val_src, res);
        try {
            test118(obj_null_src);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public Object[] test119(Object[] src) {
        return Arrays.copyOf(src, 8, Object[].class);
    }

    @DontCompile
    public void test119_verifier(boolean warmup) {
        Object[] res = test119(obj_src);
        verify(obj_src, res);
        res = test119(val_src);
        verify(val_src, res);
    }

    @Test(match = { CLASS_CHECK_TRAP }, matchCount = { 1 }, failOn = INTRINSIC_SLOW_PATH)
    public Object[] test120(Object[] src) {
        return Arrays.copyOf(src, 8, Integer[].class);
    }

    @DontCompile
    public void test120_verifier(boolean warmup) {
        Integer[] arr = new Integer[8];
        for (int i = 0; i < 8; ++i) {
            arr[i] = rI + i;
        }
        Object[] res = test120(arr);
        verify(arr, res);
        try {
            test120(val_src);
            throw new RuntimeException("ArrayStoreException expected");
        } catch (ArrayStoreException e) {
            // expected
        }
    }

    @Test
    @Warmup(10000) // Make sure we hit too_many_traps for the src <: dst check
    public Object[] test121(Object[] src) {
        return Arrays.copyOf(src, 8, MyValue2[].class);
    }

    @DontCompile
    public void test121_verifier(boolean warmup) {
        Object[] res = test121(obj_src);
        verify(obj_src, res);
        res = test121(val_src);
        verify(val_src, res);
        try {
            test121(obj_null_src);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    @Warmup(10000) // Make sure we hit too_many_traps for the src <: dst check
    public Object[] test122(Object[] src) {
        return Arrays.copyOf(src, 8, get_val_class());
    }

    @DontCompile
    public void test122_verifier(boolean warmup) {
        Object[] res = test122(obj_src);
        verify(obj_src, res);
        res = test122(val_src);
        verify(val_src, res);
        try {
            test122(obj_null_src);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    @Warmup(10000) // Make sure we hit too_many_traps for the src <: dst check
    public Object[] test123(Object[] src) {
        return Arrays.copyOf(src, 8, Integer[].class);
    }

    @DontCompile
    public void test123_verifier(boolean warmup) {
        Integer[] arr = new Integer[8];
        for (int i = 0; i < 8; ++i) {
            arr[i] = rI + i;
        }
        Object[] res = test123(arr);
        verify(arr, res);
        try {
            test123(val_src);
            throw new RuntimeException("ArrayStoreException expected");
        } catch (ArrayStoreException e) {
            // expected
        }
    }

    @Test
    @Warmup(10000) // Make sure we hit too_many_traps for the src <: dst check
    public Object[] test124(Object[] src) {
        return Arrays.copyOf(src, 8, get_int_class());
    }

    @DontCompile
    public void test124_verifier(boolean warmup) {
        Integer[] arr = new Integer[8];
        for (int i = 0; i < 8; ++i) {
            arr[i] = rI + i;
        }
        Object[] res = test124(arr);
        verify(arr, res);
        try {
            test124(val_src);
            throw new RuntimeException("ArrayStoreException expected");
        } catch (ArrayStoreException e) {
            // expected
        }
    }

    @Test
    @Warmup(10000) // Make sure we hit too_many_traps for the src <: dst check
    public Object[] test125(Object[] src, Class klass) {
        return Arrays.copyOf(src, 8, klass);
    }

    @DontCompile
    public void test125_verifier(boolean warmup) {
        Integer[] arr = new Integer[8];
        for (int i = 0; i < 8; ++i) {
            arr[i] = rI + i;
        }
        Object[] res = test125(arr, Integer[].class);
        verify((Object[])arr, res);
        res = test125(val_src, MyValue2[].class);
        verify(val_src, res);
        res = test125(obj_src, MyValue2[].class);
        verify(val_src, res);
        try {
            test125(obj_null_src, MyValue2[].class);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            test125(arr, MyValue2[].class);
            throw new RuntimeException("ArrayStoreException expected");
        } catch (ArrayStoreException e) {
            // expected
        }
        try {
            test125(val_src, MyValue1[].class);
            throw new RuntimeException("ArrayStoreException expected");
        } catch (ArrayStoreException e) {
            // expected
        }
    }

    // Verify that clone from (flat) inline type array not containing oops is always optimized.
    @Test(valid = InlineTypeArrayFlattenOn, match = { JLONG_ARRAYCOPY }, matchCount = { 1 }, failOn = CHECKCAST_ARRAYCOPY + CLONE_INTRINSIC_SLOW_PATH)
    @Test(valid = InlineTypeArrayFlattenOff, failOn = CLONE_INTRINSIC_SLOW_PATH)
    public Object[] test126(MyValue2[] src) {
        return src.clone();
    }

    @DontCompile
    public void test126_verifier(boolean warmup) {
        Object[] res = test126(val_src);
        verify(val_src, res);
    }

    // Verify correctness of generic_copy stub
    @Test
    public void test127(Object src, Object dst, int len) {
        System.arraycopy(src, 0, dst, 0, len);
    }

    @DontCompile
    public void test127_verifier(boolean warmup) {
        test127(val_src, obj_dst, 8);
        verify(val_src, obj_dst);
        test127(val_src, val_dst, 8);
        verify(val_src, val_dst);
        test127(obj_src, val_dst, 8);
        verify(obj_src, val_dst);
        try {
            test127(obj_null_src, val_dst, 8);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // expected
        }
    }

    // Verify that copyOf with known source and unknown destination class is optimized
    @Test(valid = InlineTypeArrayFlattenOn, match = { JLONG_ARRAYCOPY }, matchCount = { 1 }, failOn = CHECKCAST_ARRAYCOPY)
    @Test(valid = InlineTypeArrayFlattenOff)
    public Object[] test128(MyValue2[] src, Class klass) {
        return Arrays.copyOf(src, 8, klass);
    }

    @DontCompile
    public void test128_verifier(boolean warmup) {
        Object[] res = test128(val_src, MyValue2[].class);
        verify(val_src, res);
        res = test128(val_src, Object[].class);
        verify(val_src, res);
        try {
            test128(val_src, MyValue1[].class);
            throw new RuntimeException("ArrayStoreException expected");
        } catch (ArrayStoreException e) {
            // expected
        }
    }

    // Arraycopy with non-array src/dst
    @Test
    public void test129(Object src, Object dst, int len) {
        System.arraycopy(src, 0, dst, 0, len);
    }

    @DontCompile
    public void test129_verifier(boolean warmup) {
        try {
            test129(new Object(), new Object[0], 0);
            throw new RuntimeException("ArrayStoreException expected");
        } catch (ArrayStoreException e) {
            // expected
        }
        try {
            test129(new Object[0], new Object(), 0);
            throw new RuntimeException("ArrayStoreException expected");
        } catch (ArrayStoreException e) {
            // expected
        }
    }

    // Empty inline type array access
    @Test(failOn = ALLOC + ALLOCA + LOAD + STORE)
    public MyValueEmpty test130(MyValueEmpty[] array) {
        array[0] = new MyValueEmpty();
        return array[1];
    }

    @DontCompile
    public void test130_verifier(boolean warmup) {
        MyValueEmpty[] array = new MyValueEmpty[2];
        MyValueEmpty empty = test130(array);
        Asserts.assertEquals(array[0], MyValueEmpty.default);
        Asserts.assertEquals(empty, MyValueEmpty.default);
    }

    static primitive class EmptyContainer {
        MyValueEmpty empty = MyValueEmpty.default;
    }

    // Empty inline type container array access
    @Test(failOn = ALLOC + ALLOCA + LOAD + STORE)
    public MyValueEmpty test131(EmptyContainer[] array) {
        array[0] = new EmptyContainer();
        return array[1].empty;
    }

    @DontCompile
    public void test131_verifier(boolean warmup) {
        EmptyContainer[] array = new EmptyContainer[2];
        MyValueEmpty empty = test131(array);
        Asserts.assertEquals(array[0], EmptyContainer.default);
        Asserts.assertEquals(empty, MyValueEmpty.default);
    }

    // Empty inline type array access with unknown array type
    @Test()
    public Object test132(Object[] array) {
        array[0] = new MyValueEmpty();
        return array[1];
    }

    @DontCompile
    public void test132_verifier(boolean warmup) {
        Object[] array = new MyValueEmpty[2];
        Object empty = test132(array);
        Asserts.assertEquals(array[0], MyValueEmpty.default);
        Asserts.assertEquals(empty, MyValueEmpty.default);
        array = new Object[2];
        empty = test132(array);
        Asserts.assertEquals(array[0], MyValueEmpty.default);
        Asserts.assertEquals(empty, null);
    }

    // Empty inline type container array access with unknown array type
    @Test()
    public Object test133(Object[] array) {
        array[0] = new EmptyContainer();
        return array[1];
    }

    @DontCompile
    public void test133_verifier(boolean warmup) {
        Object[] array = new EmptyContainer[2];
        Object empty = test133(array);
        Asserts.assertEquals(array[0], EmptyContainer.default);
        Asserts.assertEquals(empty, EmptyContainer.default);
        array = new Object[2];
        empty = test133(array);
        Asserts.assertEquals(array[0], EmptyContainer.default);
        Asserts.assertEquals(empty, null);
    }

    // Non-escaping empty inline type array access
    @Test(failOn = ALLOC + ALLOCA + LOAD + STORE)
    public static MyValueEmpty test134(MyValueEmpty val) {
        MyValueEmpty[] array = new MyValueEmpty[1];
        array[0] = val;
        return array[0];
    }

    @DontCompile
    public void test134_verifier(boolean warmup) {
        MyValueEmpty empty = test134(MyValueEmpty.default);
        Asserts.assertEquals(empty, MyValueEmpty.default);
    }

    // Test accessing a locked (inline type) array
    @Test()
    public Object test135(Object[] array, Object val) {
        array[0] = val;
        return array[1];
    }

    @DontCompile
    public void test135_verifier(boolean warmup) {
        MyValue1[] array1 = new MyValue1[2];
        array1[1] = MyValue1.createWithFieldsInline(rI, rL);
        synchronized (array1) {
            Object res = test135(array1, array1[1]);
            Asserts.assertEquals(((MyValue1)res).hash(), array1[1].hash());
            Asserts.assertEquals(array1[0].hash(), array1[1].hash());
        }
        Integer[] array2 = new Integer[2];
        array2[1] = rI;
        synchronized (array2) {
            Object res = test135(array2, array2[1]);
            Asserts.assertEquals(res, array2[1]);
            Asserts.assertEquals(array2[0], array2[1]);
        }
    }

    // Same as test135 but with locking in compiled method
    @Test()
    public Object test136(Object[] array, Object val) {
        Object res = null;
        synchronized (array) {
            array[0] = val;
            res = array[1];
        }
        return res;
    }

    @DontCompile
    public void test136_verifier(boolean warmup) {
        MyValue1[] array1 = new MyValue1[2];
        array1[1] = MyValue1.createWithFieldsInline(rI, rL);
        Object res = test136(array1, array1[1]);
        Asserts.assertEquals(((MyValue1)res).hash(), array1[1].hash());
        Asserts.assertEquals(array1[0].hash(), array1[1].hash());
        Integer[] array2 = new Integer[2];
        array2[1] = rI;
        res = test136(array2, array2[1]);
        Asserts.assertEquals(res, array2[1]);
        Asserts.assertEquals(array2[0], array2[1]);
    }

    Object oFld1, oFld2;

    // Test loop unwswitching with locked (inline type) array accesses
    @Test()
    public void test137(Object[] array1, Object[] array2) {
        for (int i = 0; i < array1.length; i++) {
            oFld1 = array1[i];
            oFld2 = array2[i];
        }
    }

    @DontCompile
    public void test137_verifier(boolean warmup) {
        MyValue1[] array1 = new MyValue1[100];
        Arrays.fill(array1, MyValue1.createWithFieldsInline(rI, rL));
        Integer[] array2 = new Integer[100];
        Arrays.fill(array2, rI);
        synchronized (array1) {
            test137(array1, array1);
            Asserts.assertEquals(oFld1, array1[0]);
            Asserts.assertEquals(oFld2, array1[0]);
            test137(array1, array2);
            Asserts.assertEquals(oFld1, array1[0]);
            Asserts.assertEquals(oFld2, array2[0]);
            test137(array2, array1);
            Asserts.assertEquals(oFld1, array2[0]);
            Asserts.assertEquals(oFld2, array1[0]);
        }
        synchronized (array2) {
            test137(array2, array2);
            Asserts.assertEquals(oFld1, array2[0]);
            Asserts.assertEquals(oFld2, array2[0]);
            test137(array1, array2);
            Asserts.assertEquals(oFld1, array1[0]);
            Asserts.assertEquals(oFld2, array2[0]);
            test137(array2, array1);
            Asserts.assertEquals(oFld1, array2[0]);
            Asserts.assertEquals(oFld2, array1[0]);
        }
    }

    // Same as test137 but with locking in loop
    @Test()
    public void test138(Object[] array1, Object[] array2) {
        for (int i = 0; i < array1.length; i++) {
            synchronized (array1) {
                oFld1 = array1[i];
            }
            synchronized (array2) {
                oFld2 = array2[i];
            }
        }
    }

    @DontCompile
    public void test138_verifier(boolean warmup) {
        MyValue1[] array1 = new MyValue1[100];
        Arrays.fill(array1, MyValue1.createWithFieldsInline(rI, rL));
        Integer[] array2 = new Integer[100];
        Arrays.fill(array2, rI);
        test138(array1, array1);
        Asserts.assertEquals(oFld1, array1[0]);
        Asserts.assertEquals(oFld2, array1[0]);
        test138(array1, array2);
        Asserts.assertEquals(oFld1, array1[0]);
        Asserts.assertEquals(oFld2, array2[0]);
        test138(array2, array1);
        Asserts.assertEquals(oFld1, array2[0]);
        Asserts.assertEquals(oFld2, array1[0]);
        test138(array2, array2);
        Asserts.assertEquals(oFld1, array2[0]);
        Asserts.assertEquals(oFld2, array2[0]);
        Asserts.assertEquals(oFld2, array2[0]);
    }

    // Test load from array that is only known to be non-inline after parsing
    @Test(failOn = ALLOC + ALLOCA + ALLOC_G + ALLOCA_G + LOOP + LOAD + STORE + TRAP + LOAD_UNKNOWN_INLINE + STORE_UNKNOWN_INLINE + INLINE_ARRAY_NULL_GUARD)
    public Object test139() {
        Object[]  array = null;
        Object[] iarray = new Integer[1];
        Object[] varray = new MyValue1[1];
        for (int i = 0; i < 10; i++) {
            array = varray;
            varray = iarray;
        }
        return array[0];
    }

    @DontCompile
    public void test139_verifier(boolean warmup) {
        Object res = test139();
        Asserts.assertEquals(res, null);
    }

    // Test store to array that is only known to be non-inline after parsing
    @Test(failOn = ALLOC + ALLOCA + ALLOC_G + LOOP + LOAD + STORE + TRAP + LOAD_UNKNOWN_INLINE + STORE_UNKNOWN_INLINE + INLINE_ARRAY_NULL_GUARD)
    public Object[] test140(Object val) {
        Object[]  array = null;
        Object[] iarray = new Integer[1];
        Object[] varray = new MyValue1[1];
        for (int i = 0; i < 10; i++) {
            array = varray;
            varray = iarray;
        }
        array[0] = val;
        return array;
    }

    @DontCompile
    public void test140_verifier(boolean warmup) {
        Object[] res = test140(rI);
        Asserts.assertEquals(res[0], rI);
        res = test140(null);
        Asserts.assertEquals(res[0], null);
    }

    // Test load from array that is only known to be inline after parsing
    // TODO 8255938
    // @Test(failOn = ALLOC + ALLOCA + ALLOC_G + ALLOCA_G + LOOP + LOAD + STORE + TRAP + LOAD_UNKNOWN_INLINE + STORE_UNKNOWN_INLINE + INLINE_ARRAY_NULL_GUARD)
    @Test
    public Object test141() {
        Object[]  array = null;
        Object[] iarray = new Integer[1];
        Object[] varray = new MyValue1[1];
        for (int i = 0; i < 10; i++) {
            array = iarray;
            iarray = varray;
        }
        return array[0];
    }

    @DontCompile
    public void test141_verifier(boolean warmup) {
        Object res = test141();
        Asserts.assertEquals(res, MyValue1.default);
    }

    // Test store to array that is only known to be inline after parsing
    // TODO 8255938
    // @Test(failOn = ALLOC + ALLOCA + ALLOC_G + LOOP + LOAD + STORE + TRAP + LOAD_UNKNOWN_INLINE + STORE_UNKNOWN_INLINE + INLINE_ARRAY_NULL_GUARD)
    @Test
    public Object[] test142(Object val) {
        Object[]  array = null;
        Object[] iarray = new Integer[1];
        Object[] varray = new MyValue1[1];
        for (int i = 0; i < 10; i++) {
            array = iarray;
            iarray = varray;
        }
        array[0] = val;
        return array;
    }

    @DontCompile
    public void test142_verifier(boolean warmup) {
        Object[] res = test142(MyValue1.default);
        Asserts.assertEquals(res[0], MyValue1.default);
        if (!warmup) {
            try {
                test142(null);
                throw new RuntimeException("Should throw NullPointerException");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    static interface MyInterface143 {
        public int hash();
    }

    static class MyObject143 implements MyInterface143 {
        public int hash() { return 42; }
    }

    volatile MyInterface143[] array143 = new MyObject143[1];
    int len143 = 0;

    volatile int vf = 0;

    // Test that triggers an anti dependency failure when array mark word is loaded from immutable memory
    @Test
    @Warmup(0)
    public void test143() {
        MyInterface143[] arr = array143;
        int tmp = arr.length;
        for (int i = 0; i < len143; i++) {
            if (arr[i].hash() > 0) {
                return;
            }
        }
    }

    @DontCompile
    public void test143_verifier(boolean warmup) {
        test143();
    }

    // Same as test143 but with two flat array checks that are unswitched
    @Test
    @Warmup(0)
    public void test144() {
        MyInterface143[] arr1 = array143;
        MyInterface143[] arr2 = array143;
        int tmp1 = arr1.length;
        int tmp2 = arr2.length;
        for (int i = 0; i < len143; i++) {
            if (arr1[i].hash() > 0 && arr2[i].hash() > 0) {
                return;
            }
        }
    }

    @DontCompile
    public void test144_verifier(boolean warmup) {
        test144();
    }

    // Test that array load slow path correctly initializes non-flattened field of empty inline type
    @Test()
    public Object test145(Object[] array) {
        return array[0];
    }

    @DontCompile
    public void test145_verifier(boolean warmup) {
        Object[] array = new EmptyContainer[1];
        EmptyContainer empty = (EmptyContainer)test145(array);
        Asserts.assertEquals(empty, EmptyContainer.default);
    }

    // Test that non-flattened array does not block inline type scalarization
    @Test(failOn = ALLOC + ALLOCA + LOOP + LOAD + STORE)
    @Warmup(50000)
    public void test146(boolean b) {
        MyValue2 vt = MyValue2.createWithFieldsInline(rI, rD);
        MyValue2[] array = { vt };
        if (b) {
            for (int i = 0; i < 10; ++i) {
                if (array != array) {
                    array = null;
                }
            }
        }
    }

    @DontCompile
    public void test146_verifier(boolean warmup) {
        test146(true);
    }

    // Test that non-flattened array does not block inline type scalarization
    @Test(failOn = ALLOC + ALLOCA + LOOP + LOAD + STORE)
    @Warmup(50000)
    public int test147(boolean deopt) {
        // Both vt and array should be scalarized
        MyValue2 vt = MyValue2.createWithFieldsInline(rI, rD);
        MyValue2[] array = new MyValue2[1];

        // Delay scalarization to after loop opts
        boolean store = false;
        for (int i = 0; i < 5; ++i) {
            if (i == 1) {
                store = true;
            }
        }
        if (store) {
            array[0] = vt;
        }

        if (deopt) {
            // Uncommon trap referencing array
            return array[0].x + 42;
        }
        return array[0].x;
    }

    @DontCompile
    public void test147_verifier(boolean warmup) {
        int res = test147(!warmup);
        Asserts.assertEquals(res, MyValue2.createWithFieldsInline(rI, rD).x + (warmup ? 0 : 42));
    }
}
