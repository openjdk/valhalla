/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary Test value type arrays
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires os.simpleArch == "x64"
 * @modules java.base/jdk.experimental.bytecode
 *          java.base/jdk.experimental.value
 *          java.base/jdk.internal.misc:+open
 *          jdk.incubator.mvt
 * @compile -XDenableValueTypes TestArrays.java
 * @run main ClassFileInstaller sun.hotspot.WhiteBox
 * @run main ClassFileInstaller jdk.test.lib.Platform
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+AlwaysIncrementalInline
 *                   -XX:+EnableValhalla -XX:+ValueTypePassFieldsAsArgs -XX:+ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   -Djdk.lang.reflect.DVT=true compiler.valhalla.valuetypes.TestArrays
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:-UseCompressedOops
 *                   -XX:+EnableValhalla -XX:-ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   -Djdk.lang.reflect.DVT=true compiler.valhalla.valuetypes.TestArrays
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:-UseCompressedOops
 *                   -XX:+EnableValhalla -XX:+ValueTypePassFieldsAsArgs -XX:+ValueTypeReturnedAsFields -XX:-ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=0 -XX:ValueArrayElemMaxFlatOops=0
 *                   -Djdk.lang.reflect.DVT=true -DVerifyIR=false compiler.valhalla.valuetypes.TestArrays
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+AlwaysIncrementalInline
 *                   -XX:+EnableValhalla -XX:-ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:-ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=0 -XX:ValueArrayElemMaxFlatOops=0
 *                   -Djdk.lang.reflect.DVT=true -DVerifyIR=false compiler.valhalla.valuetypes.TestArrays
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+EnableValhalla -XX:+ValueTypePassFieldsAsArgs -XX:-ValueTypeReturnedAsFields -XX:+ValueArrayFlatten
 *                   -XX:ValueFieldMaxFlatSize=0 -XX:ValueArrayElemMaxFlatSize=-1 -XX:ValueArrayElemMaxFlatOops=-1
 *                   -Djdk.lang.reflect.DVT=true -DVerifyIR=false compiler.valhalla.valuetypes.TestArrays
 */
public class TestArrays extends ValueTypeTest {

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

    // Test value type array creation and initialization
    @Test(valid = ValueTypeArrayFlattenOff, failOn = (LOAD))
    @Test(valid = ValueTypeArrayFlattenOn)
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

    // Test creation of a value type array and element access
    @Test(valid = ValueTypeArrayFlattenOff, failOn = (LOOP + TRAP))
    @Test(valid = ValueTypeArrayFlattenOn, failOn = (ALLOC + ALLOCA + LOOP + LOAD + LOADP + STORE + TRAP))
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

    // Test receiving a value type array from the interpreter,
    // updating its elements in a loop and computing a hash.
    @Test(failOn = (ALLOCA))
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

    // Test returning a value type array received from the interpreter
    @Test(failOn = ALLOC + ALLOCA + LOAD + LOADP + STORE + LOOP + TRAP)
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

    // Merge value type arrays created from two branches
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

    // Test creation of value type array with single element
    @Test(valid = ValueTypeArrayFlattenOff, failOn = (LOAD + LOOP + TRAP))
    @Test(valid = ValueTypeArrayFlattenOn, failOn = (ALLOCA + LOAD + LOOP + TRAP))
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

    // Test default initialization of value type arrays
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

    // Test creation of value type array with zero length
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

    // Test that value type array loaded from field has correct type
    @Test(failOn = (LOOP))
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

    public void test13_verifier(boolean warmup) {
        Asserts.assertEQ(test13(), rI);
    }

    // Array load out of bound not known to compiler (both lower and upper bound)
    @Test
    public int test14(MyValue1[] va, int index)  {
        return va[index].x;
    }

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
}
