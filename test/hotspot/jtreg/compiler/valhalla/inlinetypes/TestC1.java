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
import java.util.Arrays;

import jdk.test.lib.Asserts;

/*
 * @test
 * @key randomness
 * @summary Various tests that are specific for C1.
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires os.simpleArch == "x64"
 * @compile -XDallowWithFieldOperator TestC1.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=300 -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                               compiler.valhalla.inlinetypes.InlineTypeTest
 *                               compiler.valhalla.inlinetypes.TestC1
 */
public class TestC1 extends InlineTypeTest {
    public static final int C1 = COMP_LEVEL_SIMPLE;
    public static final int C2 = COMP_LEVEL_FULL_OPTIMIZATION;

    public static void main(String[] args) throws Throwable {
        TestC1 test = new TestC1();
        test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class, MyValue3.class, MyValue3Inline.class);
    }

    @Override
    public int getNumScenarios() {
        return 5;
    }

    @Override
    public String[] getVMParameters(int scenario) {
        switch (scenario) {
        case 0: return new String[] { // C1 only
                "-XX:TieredStopAtLevel=1",
                "-XX:+TieredCompilation",
            };
        case 1: return new String[] { // C2 only. (Make sure the tests are correctly written)
                "-XX:TieredStopAtLevel=4",
                "-XX:-TieredCompilation",
            };
        case 2: return new String[] { // interpreter only
                "-Xint",
            };
        case 3: return new String[] {
                // Xcomp Only C1.
                "-XX:TieredStopAtLevel=1",
                "-XX:+TieredCompilation",
                "-Xcomp",
            };
        case 4: return new String[] {
                // Xcomp Only C2.
                "-XX:TieredStopAtLevel=4",
                "-XX:-TieredCompilation",
                "-Xcomp",
            };
        }
        return null;
    }

    // JDK-8229799
    @Test(compLevel=C1)
    public long test1(Object a, Object b, long n) {
        long r;
        n += (a == b) ? 0x5678123456781234L : 0x1234567812345678L;
        n -= 1;
        return n;
    }

    @DontCompile
    public void test1_verifier(boolean warmup) {
        MyValue1 v1 = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 v2 = MyValue1.createWithFieldsInline(rI, rL+1);
        long r1 = test1(v1, v1, 1);
        long r2 = test1(v1, v2, 1);
        Asserts.assertEQ(r1, 0x5678123456781234L);
        Asserts.assertEQ(r2, 0x1234567812345678L);
    }

    static primitive class SimpleValue2 {
        final int value;
        SimpleValue2(int value) {
            this.value = value;
        }
    }

    // JDK-8231961
    // Test that the value numbering optimization does not remove
    // the second load from the buffered array element.
    @Test(compLevel=C1)
    public int test2(SimpleValue2[] array) {
        return array[0].value + array[0].value;
    }

    @DontCompile
    public void test2_verifier(boolean warmup) {
        SimpleValue2[] array = new SimpleValue2[1];
        array[0] = new SimpleValue2(rI);
        int result = test2(array);
        Asserts.assertEQ(result, 2*rI);
    }


    // Tests below (3 to 8) check the behavior of the C1 optimization to access
    // sub-elements of a flattened array without copying the element first

    // Test access to a null array
    @Test(compLevel=C1)
    public int test3(MyValue2[] array, int index) {
        return array[index].x;
    }

    @DontCompile
    public void test3_verifier(boolean warmup) {
        NullPointerException npe = null;
        try {
            test3(null, 0);
        } catch(NullPointerException e) {
            npe = e;
        }
        Asserts.assertNE(npe, null);
    }

    // Test out of bound accesses
    @Test(compLevel=C1)
    public int test4(MyValue2[] array, int index) {
        return array[index].x;
    }

    @DontCompile
    public void test4_verifier(boolean warmup) {
        MyValue2[] array = new MyValue2[2];
        ArrayIndexOutOfBoundsException aioob = null;
        try {
            test3(array, -1);
        } catch(ArrayIndexOutOfBoundsException e) {
            aioob = e;
        }
        Asserts.assertNE(aioob, null);
        aioob = null;
        try {
            test3(array, 2);
        } catch(ArrayIndexOutOfBoundsException e) {
            aioob = e;
        }
        Asserts.assertNE(aioob, null);
    }

    // Test 1st level sub-element access to primitive field
    @Test(compLevel=C1)
    public int test5(MyValue2[] array, int index) {
        return array[index].x;
    }

    @DontCompile
    public void test5_verifier(boolean warmup) {
        MyValue2[] array = new MyValue2[2];
        MyValue2 v = new MyValue2(1,(byte)2, new MyValue2Inline(5.0d, 345L));
        array[1] = v;
        int x = test5(array, 1);
        Asserts.assertEQ(x, 1);
    }

    // Test 1st level sub-element access to flattened field
    @Test(compLevel=C1)
    public MyValue2Inline test6(MyValue2[] array, int index) {
        return array[index].v;
    }

    @DontCompile
    public void test6_verifier(boolean warmup) {
        MyValue2[] array = new MyValue2[2];
        MyValue2Inline vi = new MyValue2Inline(3.5d, 678L);
        MyValue2 v = new MyValue2(1,(byte)2, vi);
        array[0] = v;
        MyValue2Inline vi2 = test6(array, 0);
        Asserts.assertEQ(vi, vi2);
    }

    // Test 1st level sub-element access to non-flattened field
    static primitive class Big {
        long l0,l1,l2,l3,l4,l5,l6,l7,l8,l9,l10,l11,l12,l13,l14,l15,l16,l17,l18,l19 ;

        Big(long n) {
            l0 = n++; l1 = n++; l2 = n++; l3 = n++; l4 = n++; l5 = n++; l6 = n++; l7 = n++; l8 = n++;
            l9 = n++; l10 = n++; l11 = n++; l12 = n++; l13 = n++; l14 = n++; l15 = n++; l16= n++;
            l17 = n++; l18 = n++; l19 = n++;
        }

        void check(long n, int i) {
            Asserts.assertEQ(l0, n); n += i;
            Asserts.assertEQ(l1, n); n += i;
            Asserts.assertEQ(l2, n); n += i;
            Asserts.assertEQ(l3, n); n += i;
            Asserts.assertEQ(l4, n); n += i;
            Asserts.assertEQ(l5, n); n += i;
            Asserts.assertEQ(l6, n); n += i;
            Asserts.assertEQ(l7, n); n += i;
            Asserts.assertEQ(l8, n); n += i;
            Asserts.assertEQ(l9, n); n += i;
            Asserts.assertEQ(l10, n); n += i;
            Asserts.assertEQ(l11, n); n += i;
            Asserts.assertEQ(l12, n); n += i;
            Asserts.assertEQ(l13, n); n += i;
            Asserts.assertEQ(l14, n); n += i;
            Asserts.assertEQ(l15, n); n += i;
            Asserts.assertEQ(l16, n); n += i;
            Asserts.assertEQ(l17, n); n += i;
            Asserts.assertEQ(l18, n); n += i;
            Asserts.assertEQ(l19, n);
        }
    }

    static primitive class TestValue {
        int i;
        Big big;

        TestValue(int n) {
            i = n;
            big = new Big(n);
        }
    }

    @Test(compLevel=C1)
    public Big test7(TestValue[] array, int index) {
        return array[index].big;
    }

    @DontCompile
    public void test7_verifier(boolean warmup) {
        TestValue[] array = new TestValue[7];
        Big b0 = test7(array, 3);
        b0.check(0, 0);
        TestValue tv = new TestValue(9);
        array[5] = tv;
        Big b1 = test7(array, 5);
        b1.check(9, 1);
    }

    // Test 2nd level sub-element access to primitive field
    @Test(compLevel=C1)
    public byte test8(MyValue1[] array, int index) {
        return array[index].v2.y;
    }

    @DontCompile
    public void test8_verifier(boolean warmup) {
        MyValue1[] array = new MyValue1[23];
        MyValue2 mv2a = MyValue2.createWithFieldsInline(7, 63L, 8.9d);
        MyValue2 mv2b = MyValue2.createWithFieldsInline(11, 69L, 17.3d);
        MyValue1 mv1 = new MyValue1(1, 2L, (short)3, 4, null, mv2a, mv2b, 'z');
        array[19] = mv1;
        byte b = test8(array, 19);
        Asserts.assertEQ(b, (byte)11);
    }


    // Test optimizations for arrays of empty types
    // (read/write are not performed, pre-allocated instance is used for reads)
    // Most tests check that error conditions are still correctly handled
    // (OOB, null pointer)
    static primitive class EmptyType {}

    @Test(compLevel=C1)
    public EmptyType test9() {
        EmptyType[] array = new EmptyType[10];
        return array[4];
    }

    @DontCompile
    public void test9_verifier(boolean warmup) {
        EmptyType et = test9();
        Asserts.assertEQ(et, EmptyType.default);
    }

    @Test(compLevel=C1)
    public EmptyType test10(EmptyType[] array) {
        return array[0];
    }

    @DontCompile
    public void test10_verifier(boolean warmup) {
        EmptyType[] array = new EmptyType[16];
        EmptyType et = test10(array);
        Asserts.assertEQ(et, EmptyType.default);
    }

    @Test(compLevel=C1)
    public EmptyType test11(EmptyType[] array, int index) {
        return array[index];
    }

    @DontCompile
    public void test11_verifier(boolean warmup) {
        Exception e = null;
        EmptyType[] array = new EmptyType[10];
        try {
            EmptyType et = test11(array, 11);
        } catch (ArrayIndexOutOfBoundsException ex) {
            e = ex;
        }
        Asserts.assertNotNull(e);
        e = null;
        try {
            EmptyType et = test11(array, -1);
        } catch (ArrayIndexOutOfBoundsException ex) {
            e = ex;
        }
        Asserts.assertNotNull(e);
        e = null;
        try {
            EmptyType et = test11(null, 1);
        } catch (NullPointerException ex) {
            e = ex;
        }
        Asserts.assertNotNull(e);
    }

    @Test(compLevel=C1)
    public void test12(EmptyType[] array, int index, EmptyType value) {
        array[index] = value;
    }

    @DontCompile
    public void test12_verifier(boolean warmup) {
        EmptyType[] array = new EmptyType[16];
        test12(array, 2, EmptyType.default);
        Exception e = null;
        try {
            test12(null, 2, EmptyType.default);
        } catch(NullPointerException ex) {
            e = ex;
        }
        Asserts.assertNotNull(e);
        e = null;
        try {
            test12(array, 17, EmptyType.default);
        } catch(ArrayIndexOutOfBoundsException ex) {
            e = ex;
        }
        Asserts.assertNotNull(e);
        e = null;
        try {
            test12(array, -8, EmptyType.default);
        } catch(ArrayIndexOutOfBoundsException ex) {
            e = ex;
        }
        Asserts.assertNotNull(e);
    }

}
