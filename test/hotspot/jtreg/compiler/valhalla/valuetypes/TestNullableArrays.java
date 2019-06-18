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

package compiler.valhalla.valuetypes;

import jdk.test.lib.Asserts;
import java.lang.reflect.Method;
import java.util.Arrays;

/*
 * @test
 * @summary Test nullable value type arrays
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires os.simpleArch == "x64"
 * @compile TestNullableArrays.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                               compiler.valhalla.valuetypes.ValueTypeTest
 *                               compiler.valhalla.valuetypes.TestNullableArrays
 */
public class TestNullableArrays extends ValueTypeTest {
    // Unlike C2, C1 intrinsics never deoptimize System.arraycopy. Instead, we fall back to
    // a normal method invocation when encountering flattened arrays.
    private static void assertDeoptimizedByC2(Method m) {
        int CompLevel_none              = 0,         // Interpreter
            CompLevel_simple            = 1,         // C1
            CompLevel_limited_profile   = 2,         // C1, invocation & backedge counters
            CompLevel_full_profile      = 3,         // C1, invocation & backedge counters + mdo
            CompLevel_full_optimization = 4;         // C2 or JVMCI

        if (USE_COMPILER && !XCOMP && WHITE_BOX.isMethodCompiled(m, false) &&
            WHITE_BOX.getMethodCompilationLevel(m, false) >= CompLevel_full_optimization) {
            throw new RuntimeException("Type check should have caused it to deoptimize");
        }
    }

    // Extra VM parameters for some test scenarios. See ValueTypeTest.getVMParameters()
    @Override
    public String[] getExtraVMParameters(int scenario) {
        switch (scenario) {
        case 3: return new String[] {"-XX:-MonomorphicArrayCheck", "-XX:+ValueArrayFlatten"};
        case 4: return new String[] {"-XX:-MonomorphicArrayCheck"};
        }
        return null;
    }

    public static void main(String[] args) throws Throwable {
        TestNullableArrays test = new TestNullableArrays();
        test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class);
    }

    // Helper methods

    protected long hash() {
        return hash(rI, rL);
    }

    protected long hash(int x, long y) {
        return MyValue1.createWithFieldsInline(x, y).hash();
    }

    private static final MyValue1 testValue1 = MyValue1.createWithFieldsInline(rI, rL);

    // Test nullable value type array creation and initialization
    @Test(valid = ValueTypeArrayFlattenOff, failOn = LOAD)
    @Test(valid = ValueTypeArrayFlattenOn)
    public MyValue1?[] test1(int len) {
        MyValue1?[] va = new MyValue1?[len];
        if (len > 0) {
            va[0] = null;
        }
        for (int i = 1; i < len; ++i) {
            va[i] = MyValue1.createWithFieldsDontInline(rI, rL);
        }
        return va;
    }

    @DontCompile
    public void test1_verifier(boolean warmup) {
        int len = Math.abs(rI % 10);
        MyValue1?[] va = test1(len);
        if (len > 0) {
            Asserts.assertEQ(va[0], null);
        }
        for (int i = 1; i < len; ++i) {
            Asserts.assertEQ(va[i].hash(), hash());
        }
    }

    // Test creation of a value type array and element access
    @Test()
// TODO fix
//    @Test(failOn = ALLOC + ALLOCA + LOOP + LOAD + STORE + TRAP)
    public long test2() {
        MyValue1?[] va = new MyValue1?[1];
        va[0] = MyValue1.createWithFieldsInline(rI, rL);
        return va[0].hash();
    }

    @DontCompile
    public void test2_verifier(boolean warmup) {
        long result = test2();
        Asserts.assertEQ(result, hash());
    }

    // Test receiving a value type array from the interpreter,
    // updating its elements in a loop and computing a hash.
    @Test(failOn = ALLOCA)
    public long test3(MyValue1?[] va) {
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

    @DontCompile
    public void test3_verifier(boolean warmup) {
        MyValue1?[] va = new MyValue1?[10];
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

    // Test returning a value type array received from the interpreter
    @Test(failOn = ALLOC + ALLOCA + LOAD + STORE + LOOP + TRAP)
    public MyValue1?[] test4(MyValue1?[] va) {
        return va;
    }

    @DontCompile
    public void test4_verifier(boolean warmup) {
        MyValue1?[] va = new MyValue1?[10];
        for (int i = 0; i < 10; ++i) {
            va[i] = MyValue1.createWithFieldsDontInline(rI + i, rL + i);
        }
        va = test4(va);
        for (int i = 0; i < 10; ++i) {
            Asserts.assertEQ(va[i].hash(), hash(rI + i, rL + i));
        }
    }

    // Merge value type arrays created from two branches
    @Test
    public MyValue1?[] test5(boolean b) {
        MyValue1?[] va;
        if (b) {
            va = new MyValue1?[5];
            for (int i = 0; i < 5; ++i) {
                va[i] = MyValue1.createWithFieldsInline(rI, rL);
            }
            va[4] = null;
        } else {
            va = new MyValue1?[10];
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

    @DontCompile
    public void test5_verifier(boolean warmup) {
        MyValue1?[] va = test5(true);
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

    // Test creation of value type array with single element
    @Test(failOn = ALLOCA + LOOP + LOAD + TRAP)
    public MyValue1? test6() {
        MyValue1?[] va = new MyValue1?[1];
        return va[0];
    }

    @DontCompile
    public void test6_verifier(boolean warmup) {
        MyValue1?[] va = new MyValue1?[1];
        MyValue1? v = test6();
        Asserts.assertEQ(v, null);
    }

    // Test default initialization of value type arrays
    @Test(failOn = LOAD)
    public MyValue1?[] test7(int len) {
        return new MyValue1?[len];
    }

    @DontCompile
    public void test7_verifier(boolean warmup) {
        int len = Math.abs(rI % 10);
        MyValue1?[] va = test7(len);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(va[i], null);
            va[i] = null;
        }
    }

    // Test creation of value type array with zero length
    @Test(failOn = ALLOC + LOAD + STORE + LOOP + TRAP)
    public MyValue1?[] test8() {
        return new MyValue1?[0];
    }

    @DontCompile
    public void test8_verifier(boolean warmup) {
        MyValue1?[] va = test8();
        Asserts.assertEQ(va.length, 0);
    }

    static MyValue1?[] test9_va;

    // Test that value type array loaded from field has correct type
    @Test(failOn = LOOP)
    public long test9() {
        return test9_va[0].hash();
    }

    @DontCompile
    public void test9_verifier(boolean warmup) {
        test9_va = new MyValue1?[1];
        test9_va[0] = testValue1;
        long result = test9();
        Asserts.assertEQ(result, hash());
    }

    // Multi-dimensional arrays
    @Test
    public MyValue1?[][][] test10(int len1, int len2, int len3) {
        MyValue1?[][][] arr = new MyValue1?[len1][len2][len3];
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

    @DontCompile
    public void test10_verifier(boolean warmup) {
        MyValue1?[][][] arr = test10(2, 3, 4);
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
    public void test11(MyValue1?[][][] arr, long[] res) {
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

    @DontCompile
    public void test11_verifier(boolean warmup) {
        MyValue1?[][][] arr = new MyValue1?[2][3][4];
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
        MyValue1?[] va = new MyValue1?[arraySize];

        for (int i = 0; i < arraySize; i++) {
            va[i] = MyValue1.createWithFieldsDontInline(rI + 1, rL);
        }

        try {
            return va[arraySize + 1].x;
        } catch (ArrayIndexOutOfBoundsException e) {
            return rI;
        }
    }

    public void test12_verifier(boolean warmup) {
        Asserts.assertEQ(test12(), rI);
    }

    // Array load  out of bounds (lower bound) at compile time
    @Test
    public int test13() {
        int arraySize = Math.abs(rI) % 10;
        MyValue1?[] va = new MyValue1?[arraySize];

        for (int i = 0; i < arraySize; i++) {
            va[i] = MyValue1.createWithFieldsDontInline(rI + i, rL);
        }

        try {
            return va[-arraySize].x;
        } catch (ArrayIndexOutOfBoundsException e) {
            return rI;
        }
    }

    public void test13_verifier(boolean warmup) {
        Asserts.assertEQ(test13(), rI);
    }

    // Array load out of bound not known to compiler (both lower and upper bound)
    @Test
    public int test14(MyValue1?[] va, int index)  {
        return va[index].x;
    }

    public void test14_verifier(boolean warmup) {
        int arraySize = Math.abs(rI) % 10;
        MyValue1?[] va = new MyValue1?[arraySize];

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
        MyValue1?[] va = new MyValue1?[arraySize];

        try {
            for (int i = 0; i <= arraySize; i++) {
                va[i] = MyValue1.createWithFieldsDontInline(rI + 1, rL);
            }
            return rI - 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            return rI;
        }
    }

    public void test15_verifier(boolean warmup) {
        Asserts.assertEQ(test15(), rI);
    }

    // Array store out of bounds (lower bound) at compile time
    @Test
    public int test16() {
        int arraySize = Math.abs(rI) % 10;
        MyValue1?[] va = new MyValue1?[arraySize];

        try {
            for (int i = -1; i <= arraySize; i++) {
                va[i] = MyValue1.createWithFieldsDontInline(rI + 1, rL);
            }
            return rI - 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            return rI;
        }
    }

    public void test16_verifier(boolean warmup) {
        Asserts.assertEQ(test16(), rI);
    }

    // Array store out of bound not known to compiler (both lower and upper bound)
    @Test
    public int test17(MyValue1?[] va, int index, MyValue1 vt)  {
        va[index] = vt;
        return va[index].x;
    }

    @DontCompile
    public void test17_verifier(boolean warmup) {
        int arraySize = Math.abs(rI) % 10;
        MyValue1?[] va = new MyValue1?[arraySize];

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
    public MyValue1?[] test18(MyValue1?[] va) {
        return va.clone();
    }

    @DontCompile
    public void test18_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1?[] va1 = new MyValue1?[len];
        MyValue1[]  va2 = new MyValue1[len];
        for (int i = 1; i < len; ++i) {
            va1[i] = testValue1;
            va2[i] = testValue1;
        }
        MyValue1?[] result1 = test18(va1);
        if (len > 0) {
            Asserts.assertEQ(result1[0], null);
        }
        for (int i = 1; i < len; ++i) {
            Asserts.assertEQ(result1[i].hash(), va1[i].hash());
        }
        // make sure we do deopt: GraphKit::new_array assumes an
        // array of references
        for (int j = 0; j < 10; j++) {
            MyValue1?[] result2 = test18(va2);

            for (int i = 0; i < len; ++i) {
                Asserts.assertEQ(result2[i].hash(), va2[i].hash());
            }
        }
        if (compile_and_run_again_if_deoptimized(warmup, "TestNullableArrays::test18")) {
            MyValue1?[] result2 = test18(va2);
            for (int i = 0; i < len; ++i) {
                Asserts.assertEQ(result2[i].hash(), va2[i].hash());
            }
        }
    }

    // clone() as series of loads/stores
    static MyValue1?[] test19_orig = null;

    @Test
    public MyValue1?[] test19() {
        MyValue1?[] va = new MyValue1?[8];
        for (int i = 1; i < va.length; ++i) {
            va[i] = MyValue1.createWithFieldsInline(rI, rL);
        }
        test19_orig = va;

        return va.clone();
    }

    @DontCompile
    public void test19_verifier(boolean warmup) {
        MyValue1?[] result = test19();
        Asserts.assertEQ(result[0], null);
        for (int i = 1; i < test19_orig.length; ++i) {
            Asserts.assertEQ(result[i].hash(), test19_orig[i].hash());
        }
    }

    // arraycopy() of value type array with oop fields
    @Test
    public void test20(MyValue1?[] src, MyValue1?[] dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @DontCompile
    public void test20_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1?[] src1 = new MyValue1?[len];
        MyValue1?[] src2 = new MyValue1?[len];
        MyValue1[]  src3 = new MyValue1[len];
        MyValue1[]  src4 = new MyValue1[len];
        MyValue1?[] dst1 = new MyValue1?[len];
        MyValue1[]  dst2 = new MyValue1[len];
        MyValue1?[] dst3 = new MyValue1?[len];
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

    // arraycopy() of value type array with no oop field
    @Test
    public void test21(MyValue2?[] src, MyValue2?[] dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @DontCompile
    public void test21_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue2?[] src1 = new MyValue2?[len];
        MyValue2?[] src2 = new MyValue2?[len];
        MyValue2[]  src3 = new MyValue2[len];
        MyValue2[]  src4 = new MyValue2[len];
        MyValue2?[] dst1 = new MyValue2?[len];
        MyValue2[]  dst2 = new MyValue2[len];
        MyValue2?[] dst3 = new MyValue2?[len];
        MyValue2[]  dst4 = new MyValue2[len];
        if (len > 0) {
            src2[0] = MyValue2.createWithFieldsInline(rI, true);
        }
        for (int i = 1; i < len; ++i) {
            src1[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
            src2[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
            src3[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
            src4[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
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

    // arraycopy() of value type array with oop field and tightly
    // coupled allocation as dest
    @Test
    public MyValue1?[] test22(MyValue1?[] src) {
        MyValue1?[] dst = new MyValue1?[src.length];
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    @DontCompile
    public void test22_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1?[] src1 = new MyValue1?[len];
        MyValue1[]  src2 = new MyValue1[len];
        for (int i = 1; i < len; ++i) {
            src1[i] = testValue1;
            src2[i] = testValue1;
        }
        MyValue1?[] dst1 = test22(src1);
        MyValue1?[] dst2 = test22(src2);
        if (len > 0) {
            Asserts.assertEQ(dst1[0], null);
            Asserts.assertEQ(dst2[0].hash(), MyValue1.default.hash());
        }
        for (int i = 1; i < len; ++i) {
            Asserts.assertEQ(src1[i].hash(), dst1[i].hash());
            Asserts.assertEQ(src2[i].hash(), dst2[i].hash());
        }
    }

    // arraycopy() of value type array with oop fields and tightly
    // coupled allocation as dest
    @Test
    public MyValue1?[] test23(MyValue1?[] src) {
        MyValue1?[] dst = new MyValue1?[src.length + 10];
        System.arraycopy(src, 0, dst, 5, src.length);
        return dst;
    }

    @DontCompile
    public void test23_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1?[] src1 = new MyValue1?[len];
        MyValue1[] src2 = new MyValue1[len];
        for (int i = 0; i < len; ++i) {
            src1[i] = testValue1;
            src2[i] = testValue1;
        }
        MyValue1?[] dst1 = test23(src1);
        MyValue1?[] dst2 = test23(src2);
        for (int i = 0; i < 5; ++i) {
            Asserts.assertEQ(dst1[i], null);
            Asserts.assertEQ(dst2[i], null);
        }
        for (int i = 5; i < len; ++i) {
            Asserts.assertEQ(src1[i].hash(), dst1[i].hash());
            Asserts.assertEQ(src2[i].hash(), dst2[i].hash());
        }
    }

    // arraycopy() of value type array passed as Object
    @Test
    public void test24(MyValue1?[] src, Object dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @DontCompile
    public void test24_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1?[] src1 = new MyValue1?[len];
        MyValue1?[] src2 = new MyValue1?[len];
        MyValue1[]  src3 = new MyValue1[len];
        MyValue1[]  src4 = new MyValue1[len];
        MyValue1?[] dst1 = new MyValue1?[len];
        MyValue1[]  dst2 = new MyValue1[len];
        MyValue1?[] dst3 = new MyValue1?[len];
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
    public void test25(MyValue2?[] src, MyValue2?[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @DontCompile
    public void test25_verifier(boolean warmup) {
        MyValue2?[] src1 = new MyValue2?[8];
        MyValue2?[] src2 = new MyValue2?[8];
        MyValue2[]  src3 = new MyValue2[8];
        MyValue2[]  src4 = new MyValue2[8];
        MyValue2?[] dst1 = new MyValue2?[8];
        MyValue2[]  dst2 = new MyValue2[8];
        MyValue2?[] dst3 = new MyValue2?[8];
        MyValue2[]  dst4 = new MyValue2[8];
        src2[0] = MyValue2.createWithFieldsInline(rI, true);
        for (int i = 1; i < 8; ++i) {
            src1[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
            src2[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
            src3[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
            src4[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
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
    public void test26(MyValue1?[] src, MyValue1?[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @DontCompile
    public void test26_verifier(boolean warmup) {
        MyValue1?[] src1 = new MyValue1?[8];
        MyValue1?[] src2 = new MyValue1?[8];
        MyValue1[]  src3 = new MyValue1[8];
        MyValue1[]  src4 = new MyValue1[8];
        MyValue1?[] dst1 = new MyValue1?[8];
        MyValue1[]  dst2 = new MyValue1[8];
        MyValue1?[] dst3 = new MyValue1?[8];
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
    public void test27(MyValue1?[] src, MyValue1?[] dst) {
        System.arraycopy(src, 1, dst, 2, 6);
    }

    @DontCompile
    public void test27_verifier(boolean warmup) {
        MyValue1?[] src1 = new MyValue1?[8];
        MyValue1?[] src2 = new MyValue1?[8];
        MyValue1[]  src3 = new MyValue1[8];
        MyValue1[]  src4 = new MyValue1[8];
        MyValue1?[] dst1 = new MyValue1?[8];
        MyValue1[]  dst2 = new MyValue1[8];
        MyValue1?[] dst3 = new MyValue1?[8];
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
// TODO fix
//    @Test(failOn = ALLOCA + LOOP + LOAD + TRAP)
    public MyValue2? test28() {
        MyValue2?[] src = new MyValue2?[10];
        src[0] = null;
        MyValue2?[] dst = (MyValue2?[])src.clone();
        return dst[0];
    }

    @DontCompile
    public void test28_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, false);
        MyValue2? result = test28();
        Asserts.assertEQ(result, null);
    }

    // non escaping allocations
    @Test(failOn = ALLOCA + LOOP + TRAP)
    public MyValue2? test29(MyValue2?[] src) {
        MyValue2?[] dst = new MyValue2?[10];
        System.arraycopy(src, 0, dst, 0, 10);
        return dst[0];
    }

    @DontCompile
    public void test29_verifier(boolean warmup) {
        MyValue2?[] src = new MyValue2?[10];
        for (int i = 0; i < 10; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
        }
        MyValue2? v = test29(src);
        Asserts.assertEQ(src[0].hash(), v.hash());
    }

    // non escaping allocation with uncommon trap that needs
    // eliminated value type array element as debug info
    @Test
    @Warmup(10000)
    public MyValue2? test30(MyValue2?[] src, boolean flag) {
        MyValue2?[] dst = new MyValue2?[10];
        System.arraycopy(src, 0, dst, 0, 10);
        if (flag) { }
        return dst[0];
    }

    @DontCompile
    public void test30_verifier(boolean warmup) {
        MyValue2?[] src = new MyValue2?[10];
        for (int i = 0; i < 10; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
        }
        MyValue2? v = test30(src, false);
        Asserts.assertEQ(src[0].hash(), v.hash());
    }

    // non escaping allocation with memory phi
    @Test()
// TODO fix
//    @Test(failOn = ALLOC + ALLOCA + LOOP + LOAD + TRAP)
    public long test31(boolean b, boolean deopt) {
        MyValue2?[] src = new MyValue2?[1];
        if (b) {
            src[0] = MyValue2.createWithFieldsInline(rI, true);
        } else {
            src[0] = MyValue2.createWithFieldsInline(rI, false);
        }
        if (deopt) {
            // uncommon trap
            WHITE_BOX.deoptimizeMethod(tests.get(getClass().getSimpleName() + "::test31"));
        }
        return src[0].hash();
    }

    @DontCompile
    public void test31_verifier(boolean warmup) {
        MyValue2 v1 = MyValue2.createWithFieldsInline(rI, true);
        long result1 = test31(true, !warmup);
        Asserts.assertEQ(result1, v1.hash());
        MyValue2 v2 = MyValue2.createWithFieldsInline(rI, false);
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
        MyValue1?[] va1 = new MyValue1?[len];
        MyValue1[] va2 = new MyValue1[len];
        for (int i = 1; i < len; ++i) {
            va1[i] = testValue1;
            va2[i] = testValue1;
        }
        MyValue1?[] result1 = (MyValue1?[])test32(va1);
        MyValue1?[] result2 = (MyValue1?[])test32(va2);
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

    @DontCompile
    public void test33_verifier(boolean warmup) {
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
            va = new MyValue1?[8];
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
        }
        if (compile_and_run_again_if_deoptimized(warmup, "TestNullableArrays::test34")) {
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

    static void verify(MyValue1?[] src, MyValue1?[] dst) {
        for (int i = 0; i < src.length; ++i) {
            if (src[i] != null) {
                Asserts.assertEQ(src[i].hash(), dst[i].hash());
            } else {
                Asserts.assertEQ(dst[i], null);
            }
        }
    }

    static void verify(MyValue1?[] src, Object[] dst) {
        for (int i = 0; i < src.length; ++i) {
            if (src[i] != null) {
                Asserts.assertEQ(src[i].hash(), ((MyInterface)dst[i]).hash());
            } else {
                Asserts.assertEQ(dst[i], null);
            }
        }
    }

    static void verify(MyValue2?[] src, MyValue2?[] dst) {
        for (int i = 0; i < src.length; ++i) {
            if (src[i] != null) {
                Asserts.assertEQ(src[i].hash(), dst[i].hash());
            } else {
                Asserts.assertEQ(dst[i], null);
            }
        }
    }

    static void verify(MyValue2?[] src, Object[] dst) {
        for (int i = 0; i < src.length; ++i) {
            if (src[i] != null) {
                Asserts.assertEQ(src[i].hash(), ((MyInterface)dst[i]).hash());
            } else {
                Asserts.assertEQ(dst[i], null);
            }
        }
    }

    static boolean compile_and_run_again_if_deoptimized(boolean warmup, String test) {
        if (!warmup) {
            Method m = tests.get(test);
            if (USE_COMPILER &&  !WHITE_BOX.isMethodCompiled(m, false)) {
                if (!ValueTypeArrayFlatten && !XCOMP) {
                    throw new RuntimeException("Unexpected deoptimization");
                }
                WHITE_BOX.enqueueMethodForCompilation(m, COMP_LEVEL_FULL_OPTIMIZATION);
                return true;
            }
        }
        return false;
    }

    // arraycopy() of value type array of unknown size
    @Test
    public void test35(Object src, Object dst, int len) {
        System.arraycopy(src, 0, dst, 0, len);
    }

    @DontCompile
    public void test35_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1?[] src = new MyValue1?[len];
        MyValue1?[] dst = new MyValue1?[len];
        for (int i = 1; i < len; ++i) {
            src[i] = testValue1;
        }
        test35(src, dst, src.length);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestNullableArrays::test35")) {
            test35(src, dst, src.length);
            verify(src, dst);
        }
    }

    @Test
    public void test36(Object src, MyValue2?[] dst) {
        System.arraycopy(src, 0, dst, 0, dst.length);
    }

    @DontCompile
    public void test36_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue2?[] src = new MyValue2?[len];
        MyValue2?[] dst = new MyValue2?[len];
        for (int i = 1; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
        }
        test36(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestNullableArrays::test36")) {
            test36(src, dst);
            verify(src, dst);
        }
    }

    @Test
    public void test37(MyValue2?[] src, Object dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @DontCompile
    public void test37_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue2?[] src = new MyValue2?[len];
        MyValue2?[] dst = new MyValue2?[len];
        for (int i = 1; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
        }
        test37(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestNullableArrays::test37")) {
            test37(src, dst);
            verify(src, dst);
        }
    }

    @Test
    @Warmup(1) // Avoid early compilation
    public void test38(Object src, MyValue2?[] dst) {
        System.arraycopy(src, 0, dst, 0, dst.length);
    }

    @DontCompile
    public void test38_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        Object[] src = new Object[len];
        MyValue2?[] dst = new MyValue2?[len];
        for (int i = 1; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
        }
        test38(src, dst);
        verify(dst, src);
        if (!warmup) {
            Method m = tests.get("TestNullableArrays::test38");
            assertDeoptimizedByC2(m);
            WHITE_BOX.enqueueMethodForCompilation(m, COMP_LEVEL_FULL_OPTIMIZATION);
            test38(src, dst);
            verify(dst, src);
            if (USE_COMPILER && !WHITE_BOX.isMethodCompiled(m, false) && !XCOMP) {
                throw new RuntimeException("unexpected deoptimization");
            }
        }
    }

    @Test
    public void test39(MyValue2?[] src, Object dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    @DontCompile
    public void test39_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue2?[] src = new MyValue2?[len];
        Object[] dst = new Object[len];
        for (int i = 1; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
        }
        test39(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestNullableArrays::test39")) {
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
        MyValue2?[] dst = new MyValue2?[len];
        for (int i = 1; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
        }
        test40(src, dst);
        verify(dst, src);
        if (!warmup) {
            Method m = tests.get("TestNullableArrays::test40");
            assertDeoptimizedByC2(m);
            WHITE_BOX.enqueueMethodForCompilation(m, COMP_LEVEL_FULL_OPTIMIZATION);
            test40(src, dst);
            verify(dst, src);
            if (USE_COMPILER && !WHITE_BOX.isMethodCompiled(m, false) && !XCOMP) {
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
        MyValue2?[] src = new MyValue2?[len];
        Object[] dst = new Object[len];
        for (int i = 1; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
        }
        test41(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestNullableArrays::test41")) {
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
        for (int i = 1; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
        }
        test42(src, dst);
        verify(src, dst);
        if (!warmup) {
            Method m = tests.get("TestNullableArrays::test42");
            if (USE_COMPILER && !WHITE_BOX.isMethodCompiled(m, false) && !XCOMP) {
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
        MyValue1?[] src = new MyValue1?[8];
        MyValue1?[] dst = new MyValue1?[8];
        for (int i = 1; i < 8; ++i) {
            src[i] = testValue1;
        }
        test43(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestNullableArrays::test43")) {
            test43(src, dst);
            verify(src, dst);
        }
    }

    @Test
    public void test44(Object src, MyValue2?[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @DontCompile
    public void test44_verifier(boolean warmup) {
        MyValue2?[] src = new MyValue2?[8];
        MyValue2?[] dst = new MyValue2?[8];
        for (int i = 1; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
        }
        test44(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestNullableArrays::test44")) {
            test44(src, dst);
            verify(src, dst);
        }
    }

    @Test
    public void test45(MyValue2?[] src, Object dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @DontCompile
    public void test45_verifier(boolean warmup) {
        MyValue2?[] src = new MyValue2?[8];
        MyValue2?[] dst = new MyValue2?[8];
        for (int i = 1; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
        }
        test45(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestNullableArrays::test45")) {
            test45(src, dst);
            verify(src, dst);
        }
    }

    @Test
    @Warmup(1) // Avoid early compilation
    public void test46(Object[] src, MyValue2?[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @DontCompile
    public void test46_verifier(boolean warmup) {
        Object[] src = new Object[8];
        MyValue2?[] dst = new MyValue2?[8];
        for (int i = 1; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
        }
        test46(src, dst);
        verify(dst, src);
        if (!warmup) {
            Method m = tests.get("TestNullableArrays::test46");
            assertDeoptimizedByC2(m);
            WHITE_BOX.enqueueMethodForCompilation(m, COMP_LEVEL_FULL_OPTIMIZATION);
            test46(src, dst);
            verify(dst, src);
            if (USE_COMPILER && !WHITE_BOX.isMethodCompiled(m, false) && !XCOMP) {
                throw new RuntimeException("unexpected deoptimization");
            }
        }
    }

    @Test
    public void test47(MyValue2?[] src, Object[] dst) {
        System.arraycopy(src, 0, dst, 0, 8);
    }

    @DontCompile
    public void test47_verifier(boolean warmup) {
        MyValue2?[] src = new MyValue2?[8];
        Object[] dst = new Object[8];
        for (int i = 1; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
        }
        test47(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestNullableArrays::test47")) {
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
        MyValue2?[] dst = new MyValue2?[8];
        for (int i = 1; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
        }
        test48(src, dst);
        verify(dst, src);
        if (!warmup) {
            Method m = tests.get("TestNullableArrays::test48");
            assertDeoptimizedByC2(m);
            WHITE_BOX.enqueueMethodForCompilation(m, COMP_LEVEL_FULL_OPTIMIZATION);
            test48(src, dst);
            verify(dst, src);
            if (USE_COMPILER && !WHITE_BOX.isMethodCompiled(m, false) && !XCOMP) {
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
        MyValue2?[] src = new MyValue2?[8];
        Object[] dst = new Object[8];
        for (int i = 1; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
        }
        test49(src, dst);
        verify(src, dst);
        if (compile_and_run_again_if_deoptimized(warmup, "TestNullableArrays::test49")) {
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
        for (int i = 1; i < 8; ++i) {
            src[i] = MyValue2.createWithFieldsInline(rI, (i % 2) == 0);
        }
        test50(src, dst);
        verify(src, dst);
        if (!warmup) {
            Method m = tests.get("TestNullableArrays::test50");
            if (USE_COMPILER && !WHITE_BOX.isMethodCompiled(m, false) && !XCOMP) {
                throw new RuntimeException("unexpected deoptimization");
            }
        }
    }

    @Test
    public MyValue1?[] test51(MyValue1?[] va) {
        return Arrays.copyOf(va, va.length, MyValue1?[].class);
    }

    @DontCompile
    public void test51_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1?[] va = new MyValue1?[len];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
        }
        MyValue1?[] result = test51(va);
        verify(va, result);
    }

    static final MyValue1?[] test52_va = new MyValue1?[8];

    @Test
    public MyValue1?[] test52() {
        return Arrays.copyOf(test52_va, 8, MyValue1?[].class);
    }

    @DontCompile
    public void test52_verifier(boolean warmup) {
        for (int i = 1; i < 8; ++i) {
            test52_va[i] = testValue1;
        }
        MyValue1?[] result = test52();
        verify(test52_va, result);
    }

    @Test
    public MyValue1?[] test53(Object[] va) {
        return Arrays.copyOf(va, va.length, MyValue1?[].class);
    }

    @DontCompile
    public void test53_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1?[] va = new MyValue1?[len];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
        }
        MyValue1?[] result = test53(va);
        verify(result, va);
    }

    @Test
    public Object[] test54(MyValue1?[] va) {
        return Arrays.copyOf(va, va.length, Object[].class);
    }

    @DontCompile
    public void test54_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1?[] va = new MyValue1?[len];
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

    @DontCompile
    public void test55_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1?[] va = new MyValue1?[len];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
        }
        Object[] result = test55(va);
        verify(va, result);
    }

    @Test
    public MyValue1?[] test56(Object[] va) {
        return Arrays.copyOf(va, va.length, MyValue1?[].class);
    }

    @DontCompile
    public void test56_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        Object[] va = new Object[len];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
        }
        MyValue1?[] result = test56(va);
        verify(result, va);
    }

   @Test
    public Object[] test57(Object[] va, Class klass) {
        return Arrays.copyOf(va, va.length, klass);
    }

    @DontCompile
    public void test57_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        Object[] va = new MyValue1?[len];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
        }
        Object[] result = test57(va, MyValue1?[].class);
        verify(va, result);
    }

    @Test
    public Object[] test58(MyValue1?[] va, Class klass) {
        return Arrays.copyOf(va, va.length, klass);
    }

    @DontCompile
    public void test58_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1?[] va = new MyValue1?[len];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
        }
        for (int i = 1; i < 10; i++) {
            Object[] result = test58(va, MyValue1?[].class);
            verify(va, result);
        }
        if (compile_and_run_again_if_deoptimized(warmup, "TestNullableArrays::test58")) {
            Object[] result = test58(va, MyValue1?[].class);
            verify(va, result);
        }
    }

    @Test
    public Object[] test59(MyValue1?[] va) {
        return Arrays.copyOf(va, va.length+1, MyValue1?[].class);
    }

    @DontCompile
    public void test59_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1?[] va = new MyValue1?[len];
        MyValue1?[] verif = new MyValue1?[len+1];
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

    @DontCompile
    public void test60_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1?[] va = new MyValue1?[len];
        MyValue1?[] verif = new MyValue1?[len+1];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
            verif[i] = (MyValue1)va[i];
        }
        Object[] result = test60(va, MyValue1?[].class);
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
        for (int i = 1; i < len; ++i) {
            va[i] = new Integer(rI);
        }
        Object[] result = test61(va, Integer[].class);
        for (int i = 0; i < va.length; ++i) {
            Asserts.assertEQ(va[i], result[i]);
        }
    }

    @ForceInline
    public Object[] test62_helper(int i, MyValue1?[] va, Integer[] oa) {
        Object[] arr = null;
        if (i == 10) {
            arr = oa;
        } else {
            arr = va;
        }
        return arr;
    }

    @Test
    public Object[] test62(MyValue1?[] va, Integer[] oa) {
        int i = 0;
        for (; i < 10; i++);

        Object[] arr = test62_helper(i, va, oa);

        return Arrays.copyOf(arr, arr.length+1, arr.getClass());
    }

    @DontCompile
    public void test62_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1?[] va = new MyValue1?[len];
        Integer[] oa = new Integer[len];
        for (int i = 1; i < len; ++i) {
            oa[i] = new Integer(rI);
        }
        test62_helper(42, va, oa);
        Object[] result = test62(va, oa);
        for (int i = 0; i < va.length; ++i) {
            Asserts.assertEQ(oa[i], result[i]);
        }
    }

    @ForceInline
    public Object[] test63_helper(int i, MyValue1?[] va, Integer[] oa) {
        Object[] arr = null;
        if (i == 10) {
            arr = va;
        } else {
            arr = oa;
        }
        return arr;
    }

    @Test
    public Object[] test63(MyValue1?[] va, Integer[] oa) {
        int i = 0;
        for (; i < 10; i++);

        Object[] arr = test63_helper(i, va, oa);

        return Arrays.copyOf(arr, arr.length+1, arr.getClass());
    }

    @DontCompile
    public void test63_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1?[] va = new MyValue1?[len];
        MyValue1?[] verif = new MyValue1?[len+1];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
            verif[i] = va[i];
        }
        Integer[] oa = new Integer[len];
        test63_helper(42, va, oa);
        Object[] result = test63(va, oa);
        verify(verif, result);
    }

    // Test default initialization of value type arrays: small array
    @Test
    public MyValue1?[] test64() {
        return new MyValue1?[8];
    }

    @DontCompile
    public void test64_verifier(boolean warmup) {
        MyValue1?[] va = test64();
        for (int i = 0; i < 8; ++i) {
            Asserts.assertEQ(va[i], null);
        }
    }

    // Test default initialization of value type arrays: large array
    @Test
    public MyValue1?[] test65() {
        return new MyValue1?[32];
    }

    @DontCompile
    public void test65_verifier(boolean warmup) {
        MyValue1?[] va = test65();
        for (int i = 0; i < 32; ++i) {
            Asserts.assertEQ(va[i], null);
        }
    }

    // Check init store elimination
    @Test
    public MyValue1?[] test66(MyValue1? vt) {
        MyValue1?[] va = new MyValue1?[1];
        va[0] = vt;
        return va;
    }

    @DontCompile
    public void test66_verifier(boolean warmup) {
        MyValue1? vt = MyValue1.createWithFieldsDontInline(rI, rL);
        MyValue1?[] va = test66(vt);
        Asserts.assertEQ(va[0].hashPrimitive(), vt.hashPrimitive());
    }

    // Zeroing elimination and arraycopy
    @Test
    public MyValue1?[] test67(MyValue1?[] src) {
        MyValue1?[] dst = new MyValue1?[16];
        System.arraycopy(src, 0, dst, 0, 13);
        return dst;
    }

    @DontCompile
    public void test67_verifier(boolean warmup) {
        MyValue1?[] va = new MyValue1?[16];
        MyValue1?[] var = test67(va);
        for (int i = 0; i < 16; ++i) {
            Asserts.assertEQ(var[i], null);
        }
    }

    // A store with a default value can be eliminated
    @Test
    public MyValue1?[] test68() {
        MyValue1?[] va = new MyValue1?[2];
        va[0] = va[1];
        return va;
    }

    @DontCompile
    public void test68_verifier(boolean warmup) {
        MyValue1?[] va = test68();
        for (int i = 0; i < 2; ++i) {
            Asserts.assertEQ(va[i], null);
        }
    }

    // Requires individual stores to init array
    @Test
    public MyValue1?[] test69(MyValue1? vt) {
        MyValue1?[] va = new MyValue1?[4];
        va[0] = vt;
        va[3] = vt;
        return va;
    }

    @DontCompile
    public void test69_verifier(boolean warmup) {
        MyValue1? vt = MyValue1.createWithFieldsDontInline(rI, rL);
        MyValue1?[] va = new MyValue1?[4];
        va[0] = vt;
        va[3] = vt;
        MyValue1?[] var = test69(vt);
        for (int i = 0; i < va.length; ++i) {
            Asserts.assertEQ(va[i], var[i]);
        }
    }

    // A store with a default value can be eliminated: same as test68
    // but store is farther away from allocation
    @Test
    public MyValue1?[] test70(MyValue1?[] other) {
        other[1] = other[0];
        MyValue1?[] va = new MyValue1?[2];
        other[0] = va[1];
        va[0] = va[1];
        return va;
    }

    @DontCompile
    public void test70_verifier(boolean warmup) {
        MyValue1?[] va = new MyValue1?[2];
        MyValue1?[] var = test70(va);
        for (int i = 0; i < 2; ++i) {
            Asserts.assertEQ(va[i], var[i]);
        }
    }

    // EA needs to consider oop fields in flattened arrays
    @Test
    public void test71() {
        int len = 10;
        MyValue2?[] src = new MyValue2?[len];
        MyValue2?[] dst = new MyValue2?[len];
        for (int i = 1; i < len; ++i) {
            src[i] = MyValue2.createWithFieldsDontInline(rI, (i % 2) == 0);
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

    @DontCompile
    public void test71_verifier(boolean warmup) {
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

    @DontCompile
    public void test72_verifier(boolean warmup) {
        Object[] arr = new Object[1];
        Object elem = new Object();
        test72(arr, true, elem);
        test72(arr, false, elem);
    }

    @Test
    public void test73(Object[] oa, MyValue1? v, Object o) {
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
        MyValue1? v0 = MyValue1.createWithFieldsDontInline(rI, rL);
        MyValue1? v1 = MyValue1.createWithFieldsDontInline(rI+1, rL+1);
        MyValue1?[] arr = new MyValue1?[3];
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
    public Object[] test74_helper(int i, MyValue1?[] va, Integer[] oa) {
        Object[] arr = null;
        if (i == 10) {
            arr = oa;
        } else {
            arr = va;
        }
        return arr;
    }

    @Test
    public Object[] test74(MyValue1?[] va, Integer[] oa) {
        int i = 0;
        for (; i < 10; i++);

        Object[] arr = test74_helper(i, va, oa);
        return arr.clone();
    }

    @DontCompile
    public void test74_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1?[] va = new MyValue1?[len];
        Integer[] oa = new Integer[len];
        for (int i = 1; i < len; ++i) {
            oa[i] = new Integer(rI);
        }
        test74_helper(42, va, oa);
        Object[] result = test74(va, oa);

        for (int i = 0; i < va.length; ++i) {
            Asserts.assertEQ(oa[i], result[i]);
            // Check that array has correct storage properties (null-ok)
            result[i] = null;
        }
    }

    @ForceInline
    public Object[] test75_helper(int i, MyValue1?[] va, Integer[] oa) {
        Object[] arr = null;
        if (i == 10) {
            arr = va;
        } else {
            arr = oa;
        }
        return arr;
    }

    @Test
    public Object[] test75(MyValue1?[] va, Integer[] oa) {
        int i = 0;
        for (; i < 10; i++);

        Object[] arr = test75_helper(i, va, oa);
        return arr.clone();
    }

    @DontCompile
    public void test75_verifier(boolean warmup) {
        int len = Math.abs(rI) % 10;
        MyValue1?[] va = new MyValue1?[len];
        MyValue1?[] verif = new MyValue1?[len];
        for (int i = 1; i < len; ++i) {
            va[i] = testValue1;
            verif[i] = va[i];
        }
        Integer[] oa = new Integer[len];
        test75_helper(42, va, oa);
        Object[] result = test75(va, oa);
        verify(verif, result);
        if (len > 0) {
            // Check that array has correct storage properties (null-ok)
            result[0] = null;
        }
    }

    // Test mixing nullable and non-nullable arrays
    @Test
    public Object[] test76(MyValue1[] vva, MyValue1?[] vba, MyValue1 vt, Object[] out, int n) {
        Object[] result = null;
        if (n == 0) {
            result = vva;
        } else if (n == 1) {
            result = vba;
        } else if (n == 2) {
            result = new MyValue1[42];
        } else if (n == 3) {
            result = new MyValue1?[42];
        }
        result[0] = vt;
        out[0] = result[1];
        return result;
    }

    @DontCompile
    public void test76_verifier(boolean warmup) {
        MyValue1 vt = testValue1;
        Object[] out = new Object[1];
        MyValue1[] vva = new MyValue1[42];
        MyValue1[] vva_r = new MyValue1[42];
        vva_r[0] = vt;
        MyValue1?[] vba = new MyValue1?[42];
        MyValue1?[] vba_r = new MyValue1?[42];
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
            va = new MyValue1?[5];
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

    @DontCompile
    public void test77_verifier(boolean warmup) {
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

    // Same as test76 but with non value type array cases
    @Test
    public Object[] test78(MyValue1[] vva, MyValue1?[] vba, Object val, Object[] out, int n) {
        Object[] result = null;
        if (n == 0) {
            result = vva;
        } else if (n == 1) {
            result = vba;
        } else if (n == 2) {
            result = new MyValue1[42];
        } else if (n == 3) {
            result = new MyValue1?[42];
        } else  if (n == 4) {
            result = new Integer[42];
        }
        result[0] = val;
        out[0] = result[1];
        return result;
    }

    @DontCompile
    public void test78_verifier(boolean warmup) {
        MyValue1 vt = testValue1;
        Integer i = new Integer(42);
        Object[] out = new Object[1];
        MyValue1[] vva = new MyValue1[42];
        MyValue1[] vva_r = new MyValue1[42];
        vva_r[0] = vt;
        MyValue1?[] vba = new MyValue1?[42];
        MyValue1?[] vba_r = new MyValue1?[42];
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
    @Test(failOn = ALLOC + ALLOCA + STORE)
    public static MyValue1?[] test79(MyValue1[] va) {
        return va;
    }

    @DontCompile
    public void test79_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[1];
        va[0] = testValue1;
        MyValue1?[] res = test79(va);
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
    @Test(failOn = ALLOC + ALLOCA + STORE)
    public static Object[] test80(MyValue1[] va) {
        return (MyValue1?[])va;
    }

    @DontCompile
    public void test80_verifier(boolean warmup) {
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
    public static long test81(MyValue1[] va1, MyValue1?[] va2, MyValue1 vt, boolean b, boolean shouldThrow) {
        MyValue1?[] result = b ? va1 : va2;
        try {
            result[0] = vt;
        } catch (NullPointerException npe) {
            // Ignored
        }
        return result[1].hash();
    }

    @DontCompile
    public void test81_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[2];
        MyValue1?[] vaB = new MyValue1?[2];
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
    public static long test82(MyValue1[] va1, MyValue1?[] va2, MyValue1 vt1, MyValue1? vt2, int i, boolean shouldThrow) {
        MyValue1?[] result = null;
        if (i == 0) {
            result = va1;
        } else if (i == 1) {
            result = va2;
        } else if (i == 2) {
            result = new MyValue1?[2];
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

    @DontCompile
    public void test82_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[2];
        MyValue1?[] vaB = new MyValue1?[2];
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

    @Test(failOn = ALLOC + ALLOCA + STORE)
    public static long test83(MyValue1[] va) {
        MyValue1?[] result = va;
        return result[0].hash();
    }

    @DontCompile
    public void test83_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[42];
        va[0] = testValue1;
        long res = test83(va);
        Asserts.assertEquals(res, testValue1.hash());
    }

    @Test(failOn = ALLOC + ALLOCA + STORE)
    public static MyValue1?[] test84(MyValue1 vt1, MyValue1? vt2) {
        MyValue1?[] result = new MyValue1[2];
        result[0] = vt1;
        result[1] = vt2;
        return result;
    }

    @DontCompile
    public void test84_verifier(boolean warmup) {
        MyValue1?[] res = test84(testValue1, testValue1);
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
    public static long test85(MyValue1?[] va, MyValue1 val) {
        va[0] = val;
        return va[1].hash();
    }

    @DontCompile
    public void test85_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[2];
        MyValue1?[] vab = new MyValue1?[2];
        va[1] = testValue1;
        vab[1] = testValue1;
        long res = test85(va, testValue1);
        Asserts.assertEquals(res, testValue1.hash());
        Asserts.assertEquals(va[0].hash(), testValue1.hash());
        res = test85(vab, testValue1);
        Asserts.assertEquals(res, testValue1.hash());
        Asserts.assertEquals(vab[0].hash(), testValue1.hash());
    }

    // Same as test85 but with box value
    @Test()
    public static long test86(MyValue1?[] va, MyValue1? val) {
        va[0] = val;
        return va[1].hash();
    }

    @DontCompile
    public void test86_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[2];
        MyValue1?[] vab = new MyValue1?[2];
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
        MyValue1?[] va = new MyValue1?[1];
        va[0] = testValue1;
        return va[0].hash();
    }

    @DontCompile
    public void test87_verifier(boolean warmup) {
        long result = test87();
        Asserts.assertEQ(result, hash());
    }

    // Test narrowing conversion from [L to [Q
    @Test(failOn = ALLOC + ALLOCA + STORE)
    public static MyValue1[] test88(MyValue1?[] va) {
        return (MyValue1[])va;
    }

    @DontCompile
    public void test88_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[1];
        va[0] = testValue1;
        MyValue1[] res = test88(va);
        Asserts.assertEquals(res[0].hash(), testValue1.hash());
        res[0] = testValue1;
        test88(null); // Should not throw NPE
        try {
            test88(new MyValue1?[1]);
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException cce) {
            // Expected
        }
    }

    // Same as test88 but with explicit cast and Object argument
    @Test(failOn = ALLOC + ALLOCA + STORE)
    public static MyValue1[] test89(Object[] va) {
        return (MyValue1[])va;
    }

    @DontCompile
    public void test89_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[1];
        va[0] = testValue1;
        MyValue1[] res = test89(va);
        Asserts.assertEquals(((MyValue1)res[0]).hash(), testValue1.hash());
        res[0] = testValue1;
        test89(null); // Should not throw NPE
        try {
            test89(new MyValue1?[1]);
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException cce) {
            // Expected
        }
    }

    // More cast tests
    @Test()
    public static MyValue1?[] test90(Object va) {
        return (MyValue1?[])va;
    }

    @DontCompile
    public void test90_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[1];
        MyValue1?[] vab = new MyValue1?[1];
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
    public static MyValue1?[] test91(Object[] va) {
        return (MyValue1?[])va;
    }

    @DontCompile
    public void test91_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[1];
        MyValue1?[] vab = new MyValue1?[1];
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
    public static void test92(MyValue1?[] src, MyValue1?[] dst) {
        System.arraycopy(src, 0, dst, 0, 2);
    }

    @DontCompile
    public void test92_verifier(boolean warmup) {
        MyValue1[]  va = new MyValue1[2];
        MyValue1?[] vab = new MyValue1?[2];
        va[0] = testValue1;
        vab[0] = testValue1;
        test92(va, vab);
        Asserts.assertEquals(va[0], vab[0]);
        Asserts.assertEquals(va[1], vab[1]);
    }

    @Test()
    public static void test93(Object src, MyValue1?[] dst) {
        System.arraycopy(src, 0, dst, 0, 2);
    }

    @DontCompile
    public void test93_verifier(boolean warmup) {
        MyValue1[]  va = new MyValue1[2];
        MyValue1?[] vab = new MyValue1?[2];
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
        MyValue1?[] src = new MyValue1?[8];
        MyValue1[]  dst = new MyValue1[8];
        for (int i = 1; i < 8; ++i) {
            src[i] = testValue1;
        }
        System.arraycopy(src, 1, dst, 2, 6);
        return dst[0].hash();
    }

    @DontCompile
    public static void test94_verifier(boolean warmup) {
        long result = test94();
        Asserts.assertEquals(result, MyValue1.default.hash());
    }

    // Test meeting constant TypeInstPtr with ValueTypeNode
    @ForceInline
    public long test95_callee() {
        MyValue1?[] va = new MyValue1?[1];
        va[0] = testValue1;
        return va[0].hashInterpreted();
    }

    @Test()
    @Warmup(0)
    public long test95() {
        return test95_callee();
    }

    @DontCompile
    public void test95_verifier(boolean warmup) {
        long result = test95();
        Asserts.assertEQ(result, hash());
    }

    // Matrix multiplication test to exercise type flow analysis with nullable value arrays
    inline static class Complex {
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
    public Complex?[][] test96(Complex?[][] A, Complex?[][] B) {
        int size = A.length;
        Complex?[][] R = new Complex?[size][size];
        for (int i = 0; i < size; i++) {
            for (int k = 0; k < size; k++) {
                Complex? aik = A[i][k];
                for (int j = 0; j < size; j++) {
                    R[i][j] = B[i][j].add(aik.mul((Complex)B[k][j]));
                }
            }
        }
        return R;
    }

    static Complex?[][] test96_A = new Complex?[10][10];
    static Complex?[][] test96_B = new Complex?[10][10];
    static Complex?[][] test96_R;

    static {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                test96_A[i][j] = new Complex(rI, rI);
                test96_B[i][j] = new Complex(rI, rI);
            }
        }
    }

    @DontCompile
    public void test96_verifier(boolean warmup) {
        Complex?[][] result = test96(test96_A, test96_B);
        if (test96_R == null) {
            test96_R = result;
        }
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Asserts.assertEQ(result[i][j], test96_R[i][j]);
            }
        }
    }
}
