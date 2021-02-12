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
import java.lang.reflect.Method;

/*
 * @test
 * @key randomness
 * @summary Test inline type specific profiling
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires (os.simpleArch == "x64")
 * @compile TestLWorldProfiling.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=300 -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:FlatArrayElementMaxSize=-1
 *                               compiler.valhalla.inlinetypes.InlineTypeTest
 *                               compiler.valhalla.inlinetypes.TestLWorldProfiling
 */
public class TestLWorldProfiling extends InlineTypeTest {

    static final String[][] scenarios = {
        {"-XX:-UseArrayLoadStoreProfile",
         "-XX:-UseACmpProfile",
         "-XX:TypeProfileLevel=0",
         "-XX:-MonomorphicArrayCheck" },
        { "-XX:+UseArrayLoadStoreProfile",
          "-XX:+UseACmpProfile",
          "-XX:TypeProfileLevel=0" },
        { "-XX:-UseArrayLoadStoreProfile",
          "-XX:-UseACmpProfile",
          "-XX:TypeProfileLevel=222",
          "-XX:-MonomorphicArrayCheck" },
        { "-XX:-UseArrayLoadStoreProfile",
          "-XX:-UseACmpProfile",
          "-XX:TypeProfileLevel=0",
          "-XX:-MonomorphicArrayCheck",
          "-XX:TieredStopAtLevel=4",
          "-XX:-TieredCompilation" },
        { "-XX:+UseArrayLoadStoreProfile",
          "-XX:+UseACmpProfile",
          "-XX:TypeProfileLevel=0",
          "-XX:TieredStopAtLevel=4",
          "-XX:-TieredCompilation" },
        { "-XX:-UseArrayLoadStoreProfile",
          "-XX:-UseACmpProfile",
          "-XX:TypeProfileLevel=222",
          "-XX:-MonomorphicArrayCheck",
          "-XX:TieredStopAtLevel=4",
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
    private static final MyValue2 testValue2 = MyValue2.createWithFieldsInline(rI, rD);
    private static final MyValue1[] testValue1Array = new MyValue1[] {testValue1};
    private static final MyValue2[] testValue2Array = new MyValue2[] {testValue2};
    private static final Integer[] testIntegerArray = new Integer[] {42};
    private static final Long[] testLongArray = new Long[] {42L};
    private static final Double[] testDoubleArray = new Double[] {42.0D};
    private static final MyValue1.ref[] testValue1NotFlatArray = new MyValue1.ref[] {testValue1};
    private static final MyValue1[][] testValue1ArrayArray = new MyValue1[][] {testValue1Array};

    // aaload

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, failOn = LOAD_UNKNOWN_INLINE)
    @Test(valid = TypeProfileOn, failOn = LOAD_UNKNOWN_INLINE)
    @Test(match = { LOAD_UNKNOWN_INLINE }, matchCount = { 1 })
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
    @Test(valid = ArrayLoadStoreProfileOn, failOn = LOAD_UNKNOWN_INLINE)
    @Test(valid = TypeProfileOn, failOn = LOAD_UNKNOWN_INLINE)
    @Test(match = { LOAD_UNKNOWN_INLINE }, matchCount = { 1 })
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
    @Test(match = { LOAD_UNKNOWN_INLINE }, matchCount = { 1 })
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
    @Test(valid = ArrayLoadStoreProfileOn, failOn = LOAD_UNKNOWN_INLINE)
    @Test(match = { LOAD_UNKNOWN_INLINE }, matchCount = { 1 })
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
    @Test(match = { LOAD_UNKNOWN_INLINE }, matchCount = { 1 })
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
    @Test(match = { CALL, RANGE_CHECK_TRAP, NULL_CHECK_TRAP }, matchCount = { 5, 1, 1 })
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
    @Test(valid = TypeProfileOn, match = { CALL, CLASS_CHECK_TRAP, NULL_CHECK_TRAP, RANGE_CHECK_TRAP }, matchCount = { 4, 1, 2, 1 })
    @Test(match = { CALL, RANGE_CHECK_TRAP, NULL_CHECK_TRAP }, matchCount = { 6, 1, 2 })
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
    @Test(valid = ArrayLoadStoreProfileOn, failOn = STORE_UNKNOWN_INLINE)
    @Test(valid = TypeProfileOn, failOn = STORE_UNKNOWN_INLINE)
    @Test(match = { STORE_UNKNOWN_INLINE }, matchCount = { 1 })
    public void test9(Object[] array, Object v) {
        array[0] = v;
    }

    @DontCompile
    public void test9_verifier(boolean warmup) {
        test9(testValue1Array, testValue1);
        Asserts.assertEQ(testValue1Array[0].hash(), testValue1.hash());
    }

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, failOn = STORE_UNKNOWN_INLINE)
    @Test(valid = TypeProfileOn, failOn = STORE_UNKNOWN_INLINE)
    @Test(match = { STORE_UNKNOWN_INLINE }, matchCount = { 1 })
    public void test10(Object[] array, Object v) {
        array[0] = v;
    }

    @DontCompile
    public void test10_verifier(boolean warmup) {
        test10(testIntegerArray, 42);
    }

    @Warmup(10000)
    @Test(match = { STORE_UNKNOWN_INLINE }, matchCount = { 1 })
    public void test11(Object[] array, Object v) {
        array[0] = v;
    }

    @DontCompile
    public void test11_verifier(boolean warmup) {
        test11(testValue1Array, testValue1);
        test11(testValue2Array, testValue2);
    }

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, failOn = STORE_UNKNOWN_INLINE)
    @Test(match = { STORE_UNKNOWN_INLINE }, matchCount = { 1 })
    public void test12(Object[] array, Object v) {
        array[0] = v;
    }

    @DontCompile
    public void test12_verifier(boolean warmup) {
        test12(testIntegerArray, 42);
        test12(testLongArray, 42L);
    }

    @Warmup(10000)
    @Test(match = { STORE_UNKNOWN_INLINE }, matchCount = { 1 })
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
            if (deopt && !TieredCompilation && !STRESS_CC && ProfileInterpreter && (UseArrayLoadStoreProfile || TypeProfileLevel == 222)) {
                throw new RuntimeException("Monomorphic array check should rely on profiling and be accurate");
            }
        }
    }

    // null free array profiling

    primitive static class NotFlattenable {
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
    @Test(valid = ArrayLoadStoreProfileOn, match = { NULL_CHECK_TRAP }, matchCount = { 2 }, failOn = STORE_UNKNOWN_INLINE)
    @Test(valid = TypeProfileOn, match = { NULL_CHECK_TRAP }, matchCount = { 2 }, failOn = STORE_UNKNOWN_INLINE)
    @Test(match = { NULL_CHECK_TRAP, STORE_UNKNOWN_INLINE }, matchCount = { 3, 1 })
    public void test15(Object[] array, Object v) {
        array[0] = v;
    }

    @DontCompile
    public void test15_verifier(boolean warmup) {
        test15(testNotFlattenableArray, notFlattenable);
        try {
            test15(testNotFlattenableArray, null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException npe) {
            // Expected
        }
    }

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, match = { NULL_CHECK_TRAP }, matchCount = { 2 }, failOn = STORE_UNKNOWN_INLINE)
    @Test(match = { NULL_CHECK_TRAP, STORE_UNKNOWN_INLINE }, matchCount = { 3, 1 })
    public void test16(Object[] array, Object v) {
        array[0] = v;
    }

    @DontCompile
    public void test16_verifier(boolean warmup) {
        test16(testNotFlattenableArray, notFlattenable);
        try {
            test16(testNotFlattenableArray, null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException npe) {
            // Expected
        }
        test16(testIntegerArray, 42);
    }

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, match = { NULL_CHECK_TRAP }, matchCount = { 1 }, failOn = STORE_UNKNOWN_INLINE)
    @Test(match = { NULL_CHECK_TRAP, STORE_UNKNOWN_INLINE }, matchCount = { 3, 1 })
    public void test17(Object[] array, Object v) {
        array[0] = v;
    }

    @DontCompile
    public void test17_verifier(boolean warmup) {
        test17(testIntegerArray, 42);
        test17(testIntegerArray, null);
        testIntegerArray[0] = 42;
        test17(testLongArray, 42L);
    }

    public void test18_helper(Object[] array, Object v) {
        array[0] = v;
    }

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, match = { NULL_CHECK_TRAP }, matchCount = { 1 }, failOn = STORE_UNKNOWN_INLINE)
    @Test(match = { NULL_CHECK_TRAP, STORE_UNKNOWN_INLINE }, matchCount = { 3, 1 })
    public Object test18(Object[] array, Object v1) {
        Object v2 = array[0];
        test18_helper(array, v1);
        return v2;
    }

    @DontCompile
    public void test18_verifier(boolean warmup) {
        test18_helper(testValue1Array, testValue1); // pollute profile
        test18(testIntegerArray, 42);
        test18(testIntegerArray, null);
        testIntegerArray[0] = 42;
        test18(testLongArray, 42L);
    }

    // maybe null free, not flat

    @Warmup(10000)
    @Test(valid = ArrayLoadStoreProfileOn, failOn = LOAD_UNKNOWN_INLINE)
    @Test(match = { LOAD_UNKNOWN_INLINE }, matchCount = { 1 })
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
    @Test(valid = ArrayLoadStoreProfileOn, failOn = STORE_UNKNOWN_INLINE)
    @Test(match = { STORE_UNKNOWN_INLINE }, matchCount = { 1 })
    public void test20(Object[] array, Object o) {
        array[0] = o;
    }

    @DontCompile
    public void test20_verifier(boolean warmup) {
        test20(testIntegerArray, 42);
        test20(testNotFlattenableArray, notFlattenable);
    }

    // acmp tests

    // branch frequency profiling causes not equal branch to be optimized out
    @Warmup(10000)
    @Test(failOn = SUBSTITUTABILITY_TEST)
    public boolean test21(Object o1, Object o2) {
        return o1 == o2;
    }

    @DontCompile
    public void test21_verifier(boolean warmup) {
        test21(42, 42);
        test21(testValue1, testValue1);
    }

    // Input profiled non null
    @Warmup(10000)
    @Test(valid = ACmpProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { NULL_ASSERT_TRAP }, matchCount = { 1})
    @Test(match = { SUBSTITUTABILITY_TEST }, matchCount = { 1})
    public boolean test22(Object o1, Object o2) {
        return o1 == o2;
    }

    @DontCompile
    public void test22_verifier(boolean warmup) {
        test22(42, null);
        test22(42.0, null);
        if (!warmup) {
            assertCompiledByC2(tests.get("TestLWorldProfiling::test22"));
            test22(42, 42.0);
            if (UseACmpProfile) {
                assertDeoptimizedByC2(tests.get("TestLWorldProfiling::test22"));
            }
        }
    }

    @Warmup(10000)
    @Test(valid = ACmpProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { NULL_ASSERT_TRAP }, matchCount = { 1})
    @Test(valid = TypeProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { NULL_ASSERT_TRAP }, matchCount = { 1})
    @Test(match = { SUBSTITUTABILITY_TEST }, matchCount = { 1})
    public boolean test23(Object o1, Object o2) {
        return o1 == o2;
    }

    @DontCompile
    public void test23_verifier(boolean warmup) {
        test23(null, 42);
        test23(null, 42.0);
        if (!warmup) {
            assertCompiledByC2(tests.get("TestLWorldProfiling::test23"));
            test23(42, 42.0);
            if (UseACmpProfile || TypeProfileLevel != 0) {
                assertDeoptimizedByC2(tests.get("TestLWorldProfiling::test23"));
            }
        }
    }

    @Warmup(10000)
    @Test(valid = ACmpProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { NULL_ASSERT_TRAP }, matchCount = { 1})
    @Test(match = { SUBSTITUTABILITY_TEST }, matchCount = { 1})
    public boolean test24(Object o1, Object o2) {
        return o1 != o2;
    }

    @DontCompile
    public void test24_verifier(boolean warmup) {
        test24(42, null);
        test24(42.0, null);
        if (!warmup) {
            assertCompiledByC2(tests.get("TestLWorldProfiling::test24"));
            test24(42, 42.0);
             if (UseACmpProfile) {
                assertDeoptimizedByC2(tests.get("TestLWorldProfiling::test24"));
            }
        }
    }

    @Warmup(10000)
    @Test(valid = ACmpProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { NULL_ASSERT_TRAP }, matchCount = { 1})
    @Test(valid = TypeProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { NULL_ASSERT_TRAP }, matchCount = { 1})
    @Test(match = { SUBSTITUTABILITY_TEST }, matchCount = { 1})
    public boolean test25(Object o1, Object o2) {
        return o1 != o2;
    }

    @DontCompile
    public void test25_verifier(boolean warmup) {
        test25(null, 42);
        test25(null, 42.0);
        if (!warmup) {
            assertCompiledByC2(tests.get("TestLWorldProfiling::test25"));
            test25(42, 42.0);
            if (UseACmpProfile || TypeProfileLevel != 0) {
                assertDeoptimizedByC2(tests.get("TestLWorldProfiling::test25"));
            }
        }
    }

    // Input profiled not inline type with known type
    @Warmup(10000)
    @Test(valid = ACmpProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { NULL_CHECK_TRAP, CLASS_CHECK_TRAP }, matchCount = { 1, 1})
    @Test(valid = TypeProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { NULL_CHECK_TRAP, CLASS_CHECK_TRAP }, matchCount = { 1, 1})
    @Test(match = { SUBSTITUTABILITY_TEST }, matchCount = { 1})
    public boolean test26(Object o1, Object o2) {
        return o1 == o2;
    }

    @DontCompile
    public void test26_verifier(boolean warmup) {
        test26(42, 42);
        test26(42, 42.0);
        if (!warmup) {
            assertCompiledByC2(tests.get("TestLWorldProfiling::test26"));
            for (int i = 0; i < 10; i++) {
                test26(42.0, 42);
            }
            if (UseACmpProfile || TypeProfileLevel != 0) {
                assertDeoptimizedByC2(tests.get("TestLWorldProfiling::test26"));
            }
        }
    }

    @Warmup(10000)
    @Test(valid = ACmpProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { NULL_CHECK_TRAP, CLASS_CHECK_TRAP }, matchCount = { 1, 1})
    @Test(match = { SUBSTITUTABILITY_TEST }, matchCount = { 1})
    public boolean test27(Object o1, Object o2) {
        return o1 == o2;
    }

    @DontCompile
    public void test27_verifier(boolean warmup) {
        test27(42, 42);
        test27(42.0, 42);
        if (!warmup) {
            assertCompiledByC2(tests.get("TestLWorldProfiling::test27"));
            for (int i = 0; i < 10; i++) {
                test27(42, 42.0);
            }
            if (UseACmpProfile) {
                assertDeoptimizedByC2(tests.get("TestLWorldProfiling::test27"));
            }
        }
    }

    @Warmup(10000)
    @Test(valid = ACmpProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { NULL_CHECK_TRAP, CLASS_CHECK_TRAP }, matchCount = { 1, 1})
    @Test(valid = TypeProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { NULL_CHECK_TRAP, CLASS_CHECK_TRAP }, matchCount = { 1, 1})
    @Test(match = { SUBSTITUTABILITY_TEST }, matchCount = { 1})
    public boolean test28(Object o1, Object o2) {
        return o1 != o2;
    }

    @DontCompile
    public void test28_verifier(boolean warmup) {
        test28(42, 42);
        test28(42, 42.0);
        if (!warmup) {
            assertCompiledByC2(tests.get("TestLWorldProfiling::test28"));
            for (int i = 0; i < 10; i++) {
                test28(42.0, 42);
            }
            if (UseACmpProfile || TypeProfileLevel != 0) {
                assertDeoptimizedByC2(tests.get("TestLWorldProfiling::test28"));
            }
        }
    }

    @Warmup(10000)
    @Test(valid = ACmpProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { NULL_CHECK_TRAP, CLASS_CHECK_TRAP }, matchCount = { 1, 1})
    @Test(match = { SUBSTITUTABILITY_TEST }, matchCount = { 1})
    public boolean test29(Object o1, Object o2) {
        return o1 != o2;
    }

    @DontCompile
    public void test29_verifier(boolean warmup) {
        test29(42, 42);
        test29(42.0, 42);
        if (!warmup) {
            assertCompiledByC2(tests.get("TestLWorldProfiling::test29"));
            for (int i = 0; i < 10; i++) {
                test29(42, 42.0);
            }
            if (UseACmpProfile) {
                assertDeoptimizedByC2(tests.get("TestLWorldProfiling::test29"));
            }
        }
    }

    @Warmup(10000)
    @Test(valid = ACmpProfileOn, failOn = SUBSTITUTABILITY_TEST + NULL_CHECK_TRAP, match = { CLASS_CHECK_TRAP }, matchCount = { 1})
    @Test(valid = TypeProfileOn, failOn = SUBSTITUTABILITY_TEST + NULL_CHECK_TRAP, match = { CLASS_CHECK_TRAP }, matchCount = { 1})
    @Test(match = { SUBSTITUTABILITY_TEST }, matchCount = { 1})
    public boolean test30(Object o1, Object o2) {
        return o1 == o2;
    }

    @DontCompile
    public void test30_verifier(boolean warmup) {
        test30(42, 42);
        test30(42, 42.0);
        test30(null, 42);
        if (!warmup) {
            assertCompiledByC2(tests.get("TestLWorldProfiling::test30"));
            for (int i = 0; i < 10; i++) {
                test30(42.0, 42);
            }
            if (UseACmpProfile || TypeProfileLevel != 0) {
                assertDeoptimizedByC2(tests.get("TestLWorldProfiling::test30"));
            }
        }
    }

    @Warmup(10000)
    @Test(valid = ACmpProfileOn, failOn = SUBSTITUTABILITY_TEST + NULL_CHECK_TRAP)
    @Test(match = { SUBSTITUTABILITY_TEST }, matchCount = { 1})
    public boolean test31(Object o1, Object o2) {
        return o1 == o2;
    }

    @DontCompile
    public void test31_verifier(boolean warmup) {
        test31(42, 42);
        test31(42.0, 42);
        test31(42, null);
        if (!warmup) {
            assertCompiledByC2(tests.get("TestLWorldProfiling::test31"));
            for (int i = 0; i < 10; i++) {
                test31(42, 42.0);
            }
            if (UseACmpProfile) {
                assertDeoptimizedByC2(tests.get("TestLWorldProfiling::test31"));
            }
        }
    }

    // Input profiled not inline type with unknown type
    @Warmup(10000)
    @Test(valid = ACmpProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { NULL_CHECK_TRAP, CLASS_CHECK_TRAP }, matchCount = { 1, 1})
    @Test(match = { SUBSTITUTABILITY_TEST }, matchCount = { 1})
    public boolean test32(Object o1, Object o2) {
        return o1 == o2;
    }

    @DontCompile
    public void test32_verifier(boolean warmup) {
        test32(42, 42);
        test32(42, testValue1);
        test32(42.0, 42);
        if (!warmup) {
            assertCompiledByC2(tests.get("TestLWorldProfiling::test32"));
            for (int i = 0; i < 10; i++) {
                test32(testValue1, 42);
            }
            if (UseACmpProfile) {
                assertDeoptimizedByC2(tests.get("TestLWorldProfiling::test32"));
            }
        }
    }

    @Warmup(10000)
    @Test(valid = ACmpProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { NULL_CHECK_TRAP, CLASS_CHECK_TRAP }, matchCount = { 1, 1})
    @Test(match = { SUBSTITUTABILITY_TEST }, matchCount = { 1})
    public boolean test33(Object o1, Object o2) {
        return o1 == o2;
    }

    @DontCompile
    public void test33_verifier(boolean warmup) {
        test33(42, 42);
        test33(testValue1, 42);
        test33(42, 42.0);
        if (!warmup) {
            assertCompiledByC2(tests.get("TestLWorldProfiling::test33"));
            for (int i = 0; i < 10; i++) {
                test33(42, testValue1);
            }
            if (UseACmpProfile) {
                assertDeoptimizedByC2(tests.get("TestLWorldProfiling::test33"));
            }
        }
    }

    @Warmup(10000)
    @Test(valid = ACmpProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { NULL_CHECK_TRAP, CLASS_CHECK_TRAP }, matchCount = { 1, 1})
    @Test(match = { SUBSTITUTABILITY_TEST }, matchCount = { 1})
    public boolean test34(Object o1, Object o2) {
        return o1 != o2;
    }

    @DontCompile
    public void test34_verifier(boolean warmup) {
        test34(42, 42);
        test34(42, testValue1);
        test34(42.0, 42);
        if (!warmup) {
            assertCompiledByC2(tests.get("TestLWorldProfiling::test34"));
            for (int i = 0; i < 10; i++) {
                test34(testValue1, 42);
            }
            if (UseACmpProfile) {
                assertDeoptimizedByC2(tests.get("TestLWorldProfiling::test34"));
            }
        }
    }

    @Warmup(10000)
    @Test(valid = ACmpProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { NULL_CHECK_TRAP, CLASS_CHECK_TRAP }, matchCount = { 1, 1})
    @Test(match = { SUBSTITUTABILITY_TEST }, matchCount = { 1})
    public boolean test35(Object o1, Object o2) {
        return o1 != o2;
    }

    @DontCompile
    public void test35_verifier(boolean warmup) {
        test35(42, 42);
        test35(testValue1, 42);
        test35(42, 42.0);
        if (!warmup) {
            assertCompiledByC2(tests.get("TestLWorldProfiling::test35"));
            for (int i = 0; i < 10; i++) {
                test35(42, testValue1);
            }
            if (UseACmpProfile) {
                assertDeoptimizedByC2(tests.get("TestLWorldProfiling::test35"));
            }
        }
    }

    @Warmup(10000)
    @Test(valid = ACmpProfileOn, failOn = SUBSTITUTABILITY_TEST + NULL_CHECK_TRAP, match = { CLASS_CHECK_TRAP }, matchCount = { 1})
    @Test(match = { SUBSTITUTABILITY_TEST }, matchCount = { 1})
    public boolean test36(Object o1, Object o2) {
        return o1 == o2;
    }

    @DontCompile
    public void test36_verifier(boolean warmup) {
        test36(42, 42.0);
        test36(42.0, testValue1);
        test36(null, 42);
        if (!warmup) {
            assertCompiledByC2(tests.get("TestLWorldProfiling::test36"));
            for (int i = 0; i < 10; i++) {
                test36(testValue1, 42);
            }
            if (UseACmpProfile) {
                assertDeoptimizedByC2(tests.get("TestLWorldProfiling::test36"));
            }
        }
    }

    @Warmup(10000)
    @Test(valid = ACmpProfileOn, failOn = SUBSTITUTABILITY_TEST + NULL_CHECK_TRAP)
    @Test(match = { SUBSTITUTABILITY_TEST }, matchCount = { 1})
    public boolean test37(Object o1, Object o2) {
        return o1 == o2;
    }

    @DontCompile
    public void test37_verifier(boolean warmup) {
        test37(42.0, 42);
        test37(testValue1, 42.0);
        test37(42, null);
        if (!warmup) {
            assertCompiledByC2(tests.get("TestLWorldProfiling::test37"));
            for (int i = 0; i < 10; i++) {
                test37(42, testValue1);
            }
            if (UseACmpProfile) {
                assertDeoptimizedByC2(tests.get("TestLWorldProfiling::test37"));
            }
        }
    }

    // Test that acmp profile data that's unused at the acmp is fed to
    // speculation and leverage later
    @Warmup(10000)
    @Test(valid = ACmpProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { CLASS_CHECK_TRAP }, matchCount = { 1})
    @Test(valid = TypeProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { CLASS_CHECK_TRAP }, matchCount = { 1})
    @Test(match = { SUBSTITUTABILITY_TEST }, matchCount = { 1 })
    public void test38(Object o1, Object o2, Object o3) {
        if (o1 == o2) {
            test38_helper2();
        }
        test38_helper(o1, o3);
    }

    public void test38_helper(Object o1, Object o2) {
        if (o1 == o2) {
        }
    }

    public void test38_helper2() {
    }

    @DontCompile
    public void test38_verifier(boolean warmup) {
        test38(42, 42, 42);
        test38_helper(testValue1, testValue2);
    }

    @Warmup(10000)
    @Test(valid = ACmpProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { CLASS_CHECK_TRAP }, matchCount = { 1})
    @Test(valid = TypeProfileOn, failOn = SUBSTITUTABILITY_TEST, match = { CLASS_CHECK_TRAP }, matchCount = { 1})
    @Test(match = { SUBSTITUTABILITY_TEST }, matchCount = { 1 })
    public void test39(Object o1, Object o2, Object o3) {
        if (o1 == o2) {
            test39_helper2();
        }
        test39_helper(o2, o3);
    }

    public void test39_helper(Object o1, Object o2) {
        if (o1 == o2) {
        }
    }

    public void test39_helper2() {
    }

    @DontCompile
    public void test39_verifier(boolean warmup) {
        test39(42, 42, 42);
        test39_helper(testValue1, testValue2);
    }

    // Test array access with polluted array type profile
    static abstract class Test40Abstract { }
    static class Test40Class extends Test40Abstract { }
    static primitive class Test40Inline extends Test40Abstract { }

    @ForceInline
    public Object test40_access(Object[] array) {
        return array[0];
    }

    @Warmup(10000)
    @Test()
    public Object test40(Test40Abstract[] array) {
        return test40_access(array);
    }

    @DontCompile
    public void test40_verifier(boolean warmup) {
        // Make sure multiple implementors of Test40Abstract are loaded
        Test40Inline tmp1 = new Test40Inline();
        Test40Class tmp2 = new Test40Class();
        if (warmup) {
            // Pollute profile with Object[] (exact)
            test40_access(new Object[1]);
        } else {
            // When inlining test40_access, profiling contradicts actual type of array
            test40(new Test40Class[1]);
        }
    }

    // Same as test40 but with array store
    @ForceInline
    public void test41_access(Object[] array, Object val) {
        array[0] = val;
    }

    @Warmup(10000)
    @Test()
    public void test41(Test40Inline[] array, Object val) {
        test41_access(array, val);
    }

    @DontCompile
    public void test41_verifier(boolean warmup) {
        // Make sure multiple implementors of Test40Abstract are loaded
        Test40Inline tmp1 = new Test40Inline();
        Test40Class tmp2 = new Test40Class();
        if (warmup) {
            // Pollute profile with exact Object[]
            test41_access(new Object[1], new Object());
        } else {
            // When inlining test41_access, profiling contradicts actual type of array
            test41(new Test40Inline[1], new Test40Inline());
        }
    }
}
