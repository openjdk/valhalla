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

/*
 * @test
 * @summary Test value type specific profiling
 * @modules java.base/jdk.experimental.value
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires (os.simpleArch == "x64")
 * @compile TestLWorldProfiling.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=300 -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                               compiler.valhalla.valuetypes.ValueTypeTest
 *                               compiler.valhalla.valuetypes.TestLWorldProfiling
 */
public class TestLWorldProfiling extends ValueTypeTest {

    static final String[][] scenarios = {
        {"-XX:-UseArrayLoadStoreProfile",
         "-XX:TypeProfileLevel=0",
         "-XX:-MonomorphicArrayCheck" },
        { "-XX:+UseArrayLoadStoreProfile",
          "-XX:TypeProfileLevel=0" },
        { "-XX:-UseArrayLoadStoreProfile",
          "-XX:TypeProfileLevel=222",
          "-XX:-MonomorphicArrayCheck" },
        { "-XX:-UseArrayLoadStoreProfile",
          "-XX:TypeProfileLevel=0",
          "-XX:-MonomorphicArrayCheck",
          "-XX:-TieredCompilation" },
        { "-XX:+UseArrayLoadStoreProfile",
          "-XX:TypeProfileLevel=0",
          "-XX:-TieredCompilation" },
        { "-XX:-UseArrayLoadStoreProfile",
          "-XX:TypeProfileLevel=222",
          "-XX:-MonomorphicArrayCheck",
          "-XX:-TieredCompilation" }
    };

    public int getNumScenarios() {
        return scenarios.length;
    }

    public String[] getVMParameters(int scenario) {
        return scenarios[scenario];
    }

    public static void main(String[] args) throws Throwable {
        TestLWorldProfiling test = new TestLWorldProfiling();
        test.run(args, MyValue1.class, MyValue2.class);
    }

    private static final MyValue1 testValue1 = MyValue1.createWithFieldsInline(rI, rL);
    private static final MyValue2 testValue2 = MyValue2.createWithFieldsInline(rI, true);
    private static final MyValue1[] testValue1Array = new MyValue1[] {testValue1};
    private static final MyValue2[] testValue2Array = new MyValue2[] {testValue2};
    private static final Integer[] testIntegerArray = new Integer[] {42};
    private static final Long[] testLongArray = new Long[] {42L};
    private static final Double[] testDoubleArray = new Double[] {42.0D};
    private static final MyValue1?[] testValue1NotFlatArray = new MyValue1?[] {testValue1};
    private static final MyValue1[][] testValue1ArrayArray = new MyValue1[][] {testValue1Array};

    // aaload

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, failOn = LOAD_UNKNOWN_VALUE)
    @Test(valid = TypeProfileOn, failOn = LOAD_UNKNOWN_VALUE)
    @Test(match = { LOAD_UNKNOWN_VALUE }, matchCount = { 1 })
    public Object test1(Object[] array) {
        return array[0];
    }

    @DontCompile
    public void test1_verifier(boolean warmup) {
        if (warmup) {
            Object o = test1(testValue1Array);
            Asserts.assertEQ(((MyValue1)o).hash(), testValue1.hash());
        } else {
            Object o = test1(testValue2Array);
            Asserts.assertEQ(((MyValue2)o).hash(), testValue2.hash());
        }
    }

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, failOn = LOAD_UNKNOWN_VALUE)
    @Test(valid = TypeProfileOn, failOn = LOAD_UNKNOWN_VALUE)
    @Test(match = { LOAD_UNKNOWN_VALUE }, matchCount = { 1 })
    public Object test2(Object[] array) {
        return array[0];
    }

    @DontCompile
    public void test2_verifier(boolean warmup) {
        if (warmup) {
            Object o = test2(testIntegerArray);
            Asserts.assertEQ(o, 42);
        } else {
            Object o = test2(testLongArray);
            Asserts.assertEQ(o, 42L);
        }
    }

    @Warmup(10000)
    @Test(match = { LOAD_UNKNOWN_VALUE }, matchCount = { 1 })
    public Object test3(Object[] array) {
        return array[0];
    }

    @DontCompile
    public void test3_verifier(boolean warmup) {
        Object o = test3(testValue1Array);
        Asserts.assertEQ(((MyValue1)o).hash(), testValue1.hash());
        o = test3(testValue2Array);
        Asserts.assertEQ(((MyValue2)o).hash(), testValue2.hash());
    }

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, failOn = LOAD_UNKNOWN_VALUE)
    @Test(match = { LOAD_UNKNOWN_VALUE }, matchCount = { 1 })
    public Object test4(Object[] array) {
        return array[0];
    }

    @DontCompile
    public void test4_verifier(boolean warmup) {
        if (warmup) {
            Object o = test4(testIntegerArray);
            Asserts.assertEQ(o, 42);
            o = test4(testLongArray);
            Asserts.assertEQ(o, 42L);
        } else {
            Object o = test4(testValue2Array);
            Asserts.assertEQ(((MyValue2)o).hash(), testValue2.hash());
        }
    }

    @Warmup(10000)
    @Test(match = { LOAD_UNKNOWN_VALUE }, matchCount = { 1 })
    public Object test5(Object[] array) {
        return array[0];
    }

    @DontCompile
    public void test5_verifier(boolean warmup) {
        Object o = test5(testValue1Array);
        Asserts.assertEQ(((MyValue1)o).hash(), testValue1.hash());
        o = test5(testValue1NotFlatArray);
        Asserts.assertEQ(((MyValue1)o).hash(), testValue1.hash());
    }

    // Check that profile data that's useless at the aaload is
    // leveraged at a later point
    @DontInline
    public void test6_no_inline() {
    }


    public void test6_helper(Number[] arg) {
        if (arg instanceof Long[]) {
            test6_no_inline();
        }
    }

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, match = { CALL, CLASS_CHECK_TRAP, NULL_CHECK_TRAP, RANGE_CHECK_TRAP }, matchCount = { 3, 1, 1, 1 })
    @Test(valid = TypeProfileOn, match = { CALL, CLASS_CHECK_TRAP, NULL_CHECK_TRAP, RANGE_CHECK_TRAP }, matchCount = { 3, 1, 1, 1 })
    @Test(match = { CALL, RANGE_CHECK_TRAP, NULL_CHECK_TRAP }, matchCount = { 3, 1, 1 })
    public Object test6(Number[] array) {
        Number v = array[0];
        test6_helper(array);
        return v;
    }

    @DontCompile
    public void test6_verifier(boolean warmup) {
        if (warmup) {
            // pollute profile
            test6_helper(testLongArray);
            test6_helper(testDoubleArray);
        }
        test6(testIntegerArray);
    }

    @DontInline
    public void test7_no_inline() {
    }


    public void test7_helper(Number arg) {
        if (arg instanceof Long) {
            test7_no_inline();
        }
    }


    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, match = { CALL, CLASS_CHECK_TRAP, NULL_CHECK_TRAP, RANGE_CHECK_TRAP }, matchCount = { 4, 1, 2, 1 })
    @Test(match = { CALL, RANGE_CHECK_TRAP, NULL_CHECK_TRAP }, matchCount = { 4, 1, 2 })
    public Object test7(Number[] array) {
        Number v = array[0];
        test7_helper(v);
        return v;
    }

    @DontCompile
    public void test7_verifier(boolean warmup) {
        if (warmup) {
            // pollute profile
            test7_helper(42L);
            test7_helper(42.0D);
        }
        test7(testIntegerArray);
    }

    @DontInline
    public void test8_no_inline() {
    }


    public void test8_helper(Object arg) {
        if (arg instanceof Long) {
            test8_no_inline();
        }
    }

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, match = { CALL, CLASS_CHECK_TRAP, NULL_CHECK_TRAP, RANGE_CHECK_TRAP, UNHANDLED_TRAP, ALLOC_G }, matchCount = { 6, 1, 2, 1, 1, 1 })
    @Test(match = { CALL, RANGE_CHECK_TRAP, NULL_CHECK_TRAP, UNHANDLED_TRAP, ALLOC_G }, matchCount = { 6, 1, 2, 1, 1 })
    public Object test8(Object[] array) {
        Object v = array[0];
        test8_helper(v);
        return v;
    }

    @DontCompile
    public void test8_verifier(boolean warmup) {
        if (warmup) {
            // pollute profile
            test8_helper(42L);
            test8_helper(42.0D);
        }
        test8(testValue1Array);
        test8(testValue1NotFlatArray);
    }

    // aastore

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, failOn = STORE_UNKNOWN_VALUE)
    @Test(valid = TypeProfileOn, failOn = STORE_UNKNOWN_VALUE)
    @Test(match = { STORE_UNKNOWN_VALUE }, matchCount = { 1 })
    public void test9(Object[] array, Object v) {
        array[0] = v;
    }

    @DontCompile
    public void test9_verifier(boolean warmup) {
        test9(testValue1Array, testValue1);
        Asserts.assertEQ(testValue1Array[0].hash(), testValue1.hash());
    }

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, failOn = STORE_UNKNOWN_VALUE)
    @Test(valid = TypeProfileOn, failOn = STORE_UNKNOWN_VALUE)
    @Test(match = { STORE_UNKNOWN_VALUE }, matchCount = { 1 })
    public void test10(Object[] array, Object v) {
        array[0] = v;
    }

    @DontCompile
    public void test10_verifier(boolean warmup) {
        test10(testIntegerArray, 42);
    }

    @Warmup(10000)
    @Test(match = { STORE_UNKNOWN_VALUE }, matchCount = { 1 })
    public void test11(Object[] array, Object v) {
        array[0] = v;
    }

    @DontCompile
    public void test11_verifier(boolean warmup) {
        test11(testValue1Array, testValue1);
        test11(testValue2Array, testValue2);
    }

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, failOn = STORE_UNKNOWN_VALUE)
    @Test(match = { STORE_UNKNOWN_VALUE }, matchCount = { 1 })
    public void test12(Object[] array, Object v) {
        array[0] = v;
    }

    @DontCompile
    public void test12_verifier(boolean warmup) {
        test12(testIntegerArray, 42);
        test12(testLongArray, 42L);
    }

    @Warmup(10000)
    @Test(match = { STORE_UNKNOWN_VALUE }, matchCount = { 1 })
    public void test13(Object[] array, Object v) {
        array[0] = v;
    }

    @DontCompile
    public void test13_verifier(boolean warmup) {
        test13(testValue1Array, testValue1);
        test13(testValue1NotFlatArray, testValue1);
    }

    // MonomorphicArrayCheck
    @Warmup(10000)
    @Test
    public void test14(Number[] array, Number v) {
        array[0] = v;
    }

    @DontCompile
    public void test14_verifier(boolean warmup) {
        if (warmup) {
            test14(testIntegerArray, 42);
        } else {
            Method m = tests.get("TestLWorldProfiling::test14");
            boolean deopt = false;
            for (int i = 0; i < 100; i++) {
                test14(testIntegerArray, 42);
                if (!WHITE_BOX.isMethodCompiled(m, false)) {
                    deopt = true;
                }
            }
            if (!TieredCompilation && (deopt && (UseArrayLoadStoreProfile || TypeProfileLevel == 222))) {
                throw new RuntimeException("Monomorphic array check should rely on profiling and be accurate");
            }

        }

    }

    // null free array profiling

    inline static class NotFlattenable {
        private final Object o1 = null;
        private final Object o2 = null;
        private final Object o3 = null;
        private final Object o4 = null;
        private final Object o5 = null;
        private final Object o6 = null;
    }

    private static final NotFlattenable notFlattenable = new NotFlattenable();
    private static final NotFlattenable[] testNotFlattenableArray = new NotFlattenable[] { notFlattenable };

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, match = { NULL_CHECK_TRAP }, matchCount = { 2 }, failOn = STORE_UNKNOWN_VALUE)
    @Test(valid = TypeProfileOn, match = { NULL_CHECK_TRAP }, matchCount = { 2 }, failOn = STORE_UNKNOWN_VALUE)
    @Test(match = { NULL_CHECK_TRAP, STORE_UNKNOWN_VALUE }, matchCount = { 3, 1 })
    public void test15(Object[] array, Object v) {
        array[0] = v;
    }

    @DontCompile
    public void test15_verifier(boolean warmup) {
        test15(testNotFlattenableArray, notFlattenable);
        try {
            test15(testNotFlattenableArray, null);
        } catch (NullPointerException npe) {  }
    }

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, match = { NULL_CHECK_TRAP }, matchCount = { 2 }, failOn = STORE_UNKNOWN_VALUE)
    @Test(match = { NULL_CHECK_TRAP, STORE_UNKNOWN_VALUE }, matchCount = { 3, 1 })
    public void test16(Object[] array, Object v) {
        array[0] = v;
    }

    @DontCompile
    public void test16_verifier(boolean warmup) {
        test16(testNotFlattenableArray, notFlattenable);
        try {
            test16(testNotFlattenableArray, null);
        } catch (NullPointerException npe) {  }
        test16(testIntegerArray, 42);
    }

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, match = { NULL_CHECK_TRAP }, matchCount = { 1 }, failOn = STORE_UNKNOWN_VALUE)
    @Test(match = { NULL_CHECK_TRAP, STORE_UNKNOWN_VALUE }, matchCount = { 3, 1 })
    public void test17(Object[] array, Object v) {
        array[0] = v;
    }

    @DontCompile
    public void test17_verifier(boolean warmup) {
        test17(testIntegerArray, 42);
        try {
            test17(testIntegerArray, null);
        } catch (NullPointerException npe) {  }
        test17(testLongArray, 42L);
    }

    public void test18_helper(Object[] array, Object v) {
        array[0] = v;
    }

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, match = { NULL_CHECK_TRAP }, matchCount = { 1 }, failOn = STORE_UNKNOWN_VALUE)
    @Test(match = { NULL_CHECK_TRAP, STORE_UNKNOWN_VALUE }, matchCount = { 3, 1 })
    public Object test18(Object[] array, Object v1) {
        Object v2 = array[0];
        test18_helper(array, v1);
        return v2;
    }

    @DontCompile
    public void test18_verifier(boolean warmup) {
        test18_helper(testValue1Array, testValue1); // pollute profile
        test18(testIntegerArray, 42);
        try {
            test18(testIntegerArray, null);
        } catch (NullPointerException npe) {  }
        test18(testLongArray, 42L);
    }

    // maybe null free, not flat

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, failOn = LOAD_UNKNOWN_VALUE)
    @Test(match = { LOAD_UNKNOWN_VALUE }, matchCount = { 1 })
    public Object test19(Object[] array) {
        return array[0];
    }

    @DontCompile
    public void test19_verifier(boolean warmup) {
        Object o = test19(testIntegerArray);
        Asserts.assertEQ(o, 42);
        o = test19(testNotFlattenableArray);
        Asserts.assertEQ(o, notFlattenable);
    }

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, failOn = STORE_UNKNOWN_VALUE)
    @Test(match = { STORE_UNKNOWN_VALUE }, matchCount = { 1 })
    public void test20(Object[] array, Object o) {
        array[0] = o;
    }

    @DontCompile
    public void test20_verifier(boolean warmup) {
        test20(testIntegerArray, 42);
        test20(testNotFlattenableArray, notFlattenable);
    }
}
