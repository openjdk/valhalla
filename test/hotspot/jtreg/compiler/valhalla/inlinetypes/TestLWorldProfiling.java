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

import compiler.lib.ir_framework.*;
import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;

import static compiler.valhalla.inlinetypes.InlineTypes.IRNode.*;
import static compiler.valhalla.inlinetypes.InlineTypes.*;

/*
 * @test
 * @key randomness
 * @summary Test inline type specific profiling
 * @library /test/lib /
 * @requires (os.simpleArch == "x64")
 * @run driver/timeout=300 compiler.valhalla.inlinetypes.TestLWorldProfiling
 */

@ForceCompileClassInitializer
public class TestLWorldProfiling {

    public static void main(String[] args) {
        final Scenario[] scenarios = {
                new Scenario(0,
                        "-XX:FlatArrayElementMaxSize=-1",
                        "-XX:-UseArrayLoadStoreProfile",
                        "-XX:-UseACmpProfile",
                        "-XX:TypeProfileLevel=0",
                        "-XX:-MonomorphicArrayCheck"),
                new Scenario(1,
                        "-XX:FlatArrayElementMaxSize=-1",
                        "-XX:+UseArrayLoadStoreProfile",
                        "-XX:+UseACmpProfile",
                        "-XX:TypeProfileLevel=0"),
                new Scenario(2,
                        "-XX:FlatArrayElementMaxSize=-1",
                        "-XX:-UseArrayLoadStoreProfile",
                        "-XX:-UseACmpProfile",
                        "-XX:TypeProfileLevel=222",
                        "-XX:-MonomorphicArrayCheck"),
                new Scenario(3,
                        "-XX:FlatArrayElementMaxSize=-1",
                        "-XX:-UseArrayLoadStoreProfile",
                        "-XX:-UseACmpProfile",
                        "-XX:TypeProfileLevel=0",
                        "-XX:-MonomorphicArrayCheck",
                        "-XX:TieredStopAtLevel=4",
                        "-XX:-TieredCompilation"),
                new Scenario(4,
                        "-XX:FlatArrayElementMaxSize=-1",
                        "-XX:+UseArrayLoadStoreProfile",
                        "-XX:+UseACmpProfile",
                        "-XX:TypeProfileLevel=0",
                        "-XX:TieredStopAtLevel=4",
                        "-XX:-TieredCompilation"),
                new Scenario(5,
                        "-XX:FlatArrayElementMaxSize=-1",
                        "-XX:-UseArrayLoadStoreProfile",
                        "-XX:-UseACmpProfile",
                        "-XX:TypeProfileLevel=222",
                        "-XX:-MonomorphicArrayCheck",
                        "-XX:TieredStopAtLevel=4",
                        "-XX:-TieredCompilation")
        };

        InlineTypes.getFramework()
                   .addScenarios(scenarios)
                   .addFlags("-XX:+IgnoreUnrecognizedVMOptions")
                   .addHelperClasses(MyValue1.class,
                                     MyValue2.class)
                   .start();
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

    // Wrap these variables into helper class because
    // WhiteBox API needs to be initialized by TestFramework first.
    static class WBFlags {
        static final boolean UseACmpProfile = (Boolean) WhiteBox.getWhiteBox().getVMFlag("UseACmpProfile");
        static final boolean TieredCompilation = (Boolean) WhiteBox.getWhiteBox().getVMFlag("TieredCompilation");
        static final boolean ProfileInterpreter = (Boolean) WhiteBox.getWhiteBox().getVMFlag("ProfileInterpreter");
        static final boolean UseArrayLoadStoreProfile = (Boolean) WhiteBox.getWhiteBox().getVMFlag("UseArrayLoadStoreProfile");
        static final long TypeProfileLevel = (Long) WhiteBox.getWhiteBox().getVMFlag("TypeProfileLevel");
    }

    // aaload

    @Test
    @IR(applyIfOr = {"UseArrayLoadStoreProfile", "true", "TypeProfileLevel", "= 222"},
        failOn = {LOAD_UNKNOWN_INLINE})
    @IR(applyIfAnd={"UseACmpProfile", "false", "TypeProfileLevel", "!= 222"},
        counts = {LOAD_UNKNOWN_INLINE, "= 1"})
    public Object test1(Object[] array) {
        return array[0];
    }

    @Run(test = "test1")
    @Warmup(10000)
    public void test1_verifier(RunInfo info) {
        if (info.isWarmUp()) {
            Object o = test1(testValue1Array);
            Asserts.assertEQ(((MyValue1)o).hash(), testValue1.hash());
        } else {
            Object o = test1(testValue2Array);
            Asserts.assertEQ(((MyValue2)o).hash(), testValue2.hash());
        }
    }

    @Test
    @IR(applyIfOr = {"UseArrayLoadStoreProfile", "true", "TypeProfileLevel", "= 222"},
        failOn = {LOAD_UNKNOWN_INLINE})
    @IR(applyIfAnd = {"UseArrayLoadStoreProfile", "false", "TypeProfileLevel", "!= 222"},
        counts = {LOAD_UNKNOWN_INLINE, "= 1"})
    public Object test2(Object[] array) {
        return array[0];
    }

    @Run(test = "test2")
    @Warmup(10000)
    public void test2_verifier(RunInfo info) {
        if (info.isWarmUp()) {
            Object o = test2(testIntegerArray);
            Asserts.assertEQ(o, 42);
        } else {
            Object o = test2(testLongArray);
            Asserts.assertEQ(o, 42L);
        }
    }

    @Test
    @IR(counts = {LOAD_UNKNOWN_INLINE, "= 1"})
    public Object test3(Object[] array) {
        return array[0];
    }

    @Run(test = "test3")
    @Warmup(10000)
    public void test3_verifier() {
        Object o = test3(testValue1Array);
        Asserts.assertEQ(((MyValue1)o).hash(), testValue1.hash());
        o = test3(testValue2Array);
        Asserts.assertEQ(((MyValue2)o).hash(), testValue2.hash());
    }

    @Test
    @IR(applyIf = {"UseArrayLoadStoreProfile", "true"},
        failOn = {LOAD_UNKNOWN_INLINE})
    @IR(applyIf = {"UseArrayLoadStoreProfile", "false"},
        counts = {LOAD_UNKNOWN_INLINE, "= 1"})
    public Object test4(Object[] array) {
        return array[0];
    }

    @Run(test = "test4")
    @Warmup(10000)
    public void test4_verifier(RunInfo info) {
        if (info.isWarmUp()) {
            Object o = test4(testIntegerArray);
            Asserts.assertEQ(o, 42);
            o = test4(testLongArray);
            Asserts.assertEQ(o, 42L);
        } else {
            Object o = test4(testValue2Array);
            Asserts.assertEQ(((MyValue2)o).hash(), testValue2.hash());
        }
    }

    @Test
    @IR(counts = {LOAD_UNKNOWN_INLINE, "= 1"})
    public Object test5(Object[] array) {
        return array[0];
    }

    @Run(test = "test5")
    @Warmup(10000)
    public void test5_verifier() {
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

    @Test
    @IR(applyIfOr = {"UseArrayLoadStoreProfile", "true", "TypeProfileLevel", "= 222"},
        counts = {CALL, "= 3", CLASS_CHECK_TRAP, "= 1", NULL_CHECK_TRAP, "= 1", RANGE_CHECK_TRAP, "= 1"})
    @IR(applyIfAnd = {"UseArrayLoadStoreProfile", "false", "TypeProfileLevel", "!= 222"},
        counts = {CALL, "= 5", RANGE_CHECK_TRAP, "= 1", NULL_CHECK_TRAP, "= 1"})
    public Object test6(Number[] array) {
        Number v = array[0];
        test6_helper(array);
        return v;
    }

    @Run(test = "test6")
    @Warmup(10000)
    public void test6_verifier(RunInfo info) {
        if (info.isWarmUp()) {
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

    @Test
    @IR(applyIfOr = {"UseArrayLoadStoreProfile", "true", "TypeProfileLevel", "= 222"},
        counts = {CALL, "= 4", CLASS_CHECK_TRAP, "= 1", NULL_CHECK_TRAP, "= 2", RANGE_CHECK_TRAP, "= 1"})
    @IR(applyIfAnd = {"UseArrayLoadStoreProfile", "false", "TypeProfileLevel", "!= 222"},
        counts = {CALL, "= 6", RANGE_CHECK_TRAP, "= 1", NULL_CHECK_TRAP, "= 2"})
    public Object test7(Number[] array) {
        Number v = array[0];
        test7_helper(v);
        return v;
    }

    @Run(test = "test7")
    @Warmup(10000)
    public void test7_verifier(RunInfo info) {
        if (info.isWarmUp()) {
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

    @Test
    @IR(applyIf = {"UseArrayLoadStoreProfile", "true"},
        counts = {CALL, "= 6", CLASS_CHECK_TRAP, "= 1", NULL_CHECK_TRAP, "= 2",
                  RANGE_CHECK_TRAP, "= 1", UNHANDLED_TRAP, "= 1", ALLOC_G, "= 1"})
    @IR(applyIf = {"UseArrayLoadStoreProfile", "false"},
        counts = {CALL, "= 6", RANGE_CHECK_TRAP, "= 1", NULL_CHECK_TRAP, "= 2",
                  UNHANDLED_TRAP, "= 1", ALLOC_G, "= 1"})
    public Object test8(Object[] array) {
        Object v = array[0];
        test8_helper(v);
        return v;
    }

    @Run(test = "test8")
    @Warmup(10000)
    public void test8_verifier(RunInfo info) {
        if (info.isWarmUp()) {
            // pollute profile
            test8_helper(42L);
            test8_helper(42.0D);
        }
        test8(testValue1Array);
        test8(testValue1NotFlatArray);
    }

    // aastore

    @Test
    @IR(applyIfOr = {"UseArrayLoadStoreProfile", "true", "TypeProfileLevel", "= 222"},
        failOn = {STORE_UNKNOWN_INLINE})
    @IR(applyIfAnd = {"UseArrayLoadStoreProfile", "false", "TypeProfileLevel", "!= 222"},
        counts = {STORE_UNKNOWN_INLINE, "= 1"})
    public void test9(Object[] array, Object v) {
        array[0] = v;
    }

    @Run(test = "test9")
    @Warmup(10000)
    public void test9_verifier() {
        test9(testValue1Array, testValue1);
        Asserts.assertEQ(testValue1Array[0].hash(), testValue1.hash());
    }


    @Test
    @IR(applyIfOr = {"UseArrayLoadStoreProfile", "true", "TypeProfileLevel", "= 222"},
        failOn = {STORE_UNKNOWN_INLINE})
    @IR(applyIfAnd = {"UseArrayLoadStoreProfile", "false", "TypeProfileLevel", "!= 222"},
        counts = {STORE_UNKNOWN_INLINE, "= 1"})
    public void test10(Object[] array, Object v) {
        array[0] = v;
    }

    @Run(test = "test10")
    @Warmup(10000)
    public void test10_verifier() {
        test10(testIntegerArray, 42);
    }

    @Test
    @IR(counts = {STORE_UNKNOWN_INLINE, "= 1"})
    public void test11(Object[] array, Object v) {
        array[0] = v;
    }

    @Run(test = "test11")
    @Warmup(10000)
    public void test11_verifier() {
        test11(testValue1Array, testValue1);
        test11(testValue2Array, testValue2);
    }

    @Test
    @IR(applyIf = {"UseArrayLoadStoreProfile", "true"},
        failOn = {STORE_UNKNOWN_INLINE})
    @IR(applyIf = {"UseArrayLoadStoreProfile", "false"},
        counts = {STORE_UNKNOWN_INLINE, "= 1"})
    public void test12(Object[] array, Object v) {
        array[0] = v;
    }

    @Run(test = "test12")
    @Warmup(10000)
    public void test12_verifier() {
        test12(testIntegerArray, 42);
        test12(testLongArray, 42L);
    }

    @Test
    @IR(counts = {STORE_UNKNOWN_INLINE, "= 1"})
    public void test13(Object[] array, Object v) {
        array[0] = v;
    }

    @Run(test = "test13")
    @Warmup(10000)
    public void test13_verifier() {
        test13(testValue1Array, testValue1);
        test13(testValue1NotFlatArray, testValue1);
    }


    // MonomorphicArrayCheck
    @Test
    public void test14(Number[] array, Number v) {
        array[0] = v;
    }

    @Run(test = "test14")
    @Warmup(10000)
    public void test14_verifier(RunInfo info) {
        if (info.isWarmUp()) {
            test14(testIntegerArray, 42);
        } else {
            Method m = info.getTest();
            boolean deopt = false;
            for (int i = 0; i < 100; i++) {
                test14(testIntegerArray, 42);
                if (!info.isCompilationSkipped() && !TestFramework.isCompiled(m)) {
                    deopt = true;
                }
            }

            if (deopt && !WBFlags.TieredCompilation && WBFlags.ProfileInterpreter &&
                         (WBFlags.UseArrayLoadStoreProfile || WBFlags.TypeProfileLevel == 222)) {
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

    @Test
    @IR(applyIfOr = {"UseArrayLoadStoreProfile", "true", "TypeProfileLevel", "= 222"},
        counts = {NULL_CHECK_TRAP, "= 2"},
        failOn = {STORE_UNKNOWN_INLINE})
    @IR(applyIfAnd = {"UseArrayLoadStoreProfile", "false", "TypeProfileLevel", "!= 222"},
        counts = {NULL_CHECK_TRAP, "= 3", STORE_UNKNOWN_INLINE, "= 1"})
    public void test15(Object[] array, Object v) {
        array[0] = v;
    }

    @Run(test = "test15")
    @Warmup(10000)
    public void test15_verifier() {
        test15(testNotFlattenableArray, notFlattenable);
        try {
            test15(testNotFlattenableArray, null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException npe) {
            // Expected
        }
    }

    @Test
    @IR(applyIf = {"UseArrayLoadStoreProfile", "true"},
        counts = {NULL_CHECK_TRAP, "= 2"},
        failOn = {STORE_UNKNOWN_INLINE})
    @IR(applyIf = {"UseArrayLoadStoreProfile", "false"},
        counts = {NULL_CHECK_TRAP, "= 3", STORE_UNKNOWN_INLINE, "= 1"})
    public void test16(Object[] array, Object v) {
        array[0] = v;
    }

    @Run(test = "test16")
    @Warmup(10000)
    public void test16_verifier() {
        test16(testNotFlattenableArray, notFlattenable);
        try {
            test16(testNotFlattenableArray, null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException npe) {
            // Expected
        }
        test16(testIntegerArray, 42);
    }

    @Test
    @IR(applyIf = {"UseArrayLoadStoreProfile", "true"},
        counts = {NULL_CHECK_TRAP, "= 1"},
        failOn = {STORE_UNKNOWN_INLINE})
    @IR(applyIf = {"UseArrayLoadStoreProfile", "false"},
        counts = {NULL_CHECK_TRAP, "= 3", STORE_UNKNOWN_INLINE, "= 1"})
    public void test17(Object[] array, Object v) {
        array[0] = v;
    }

    @Run(test = "test17")
    @Warmup(10000)
    public void test17_verifier() {
        test17(testIntegerArray, 42);
        test17(testIntegerArray, null);
        testIntegerArray[0] = 42;
        test17(testLongArray, 42L);
    }

    public void test18_helper(Object[] array, Object v) {
        array[0] = v;
    }

    @Test
    @IR(applyIf = {"UseArrayLoadStoreProfile", "true"},
        counts = {NULL_CHECK_TRAP, "= 1"},
        failOn = {STORE_UNKNOWN_INLINE})
    @IR(applyIf = {"UseArrayLoadStoreProfile", "false"},
        counts = {NULL_CHECK_TRAP, "= 3", STORE_UNKNOWN_INLINE, "= 1"})
    public Object test18(Object[] array, Object v1) {
        Object v2 = array[0];
        test18_helper(array, v1);
        return v2;
    }

    @Run(test = "test18")
    @Warmup(10000)
    public void test18_verifier() {
        test18_helper(testValue1Array, testValue1); // pollute profile
        test18(testIntegerArray, 42);
        test18(testIntegerArray, null);
        testIntegerArray[0] = 42;
        test18(testLongArray, 42L);
    }

    // maybe null free, not flat

    @Test
    @IR(applyIf = {"UseArrayLoadStoreProfile", "true"},
        failOn = {LOAD_UNKNOWN_INLINE})
    @IR(applyIf = {"UseArrayLoadStoreProfile", "false"},
        counts = {LOAD_UNKNOWN_INLINE, "= 1"})
    public Object test19(Object[] array) {
        return array[0];
    }

    @Run(test = "test19")
    @Warmup(10000)
    public void test19_verifier() {
        Object o = test19(testIntegerArray);
        Asserts.assertEQ(o, 42);
        o = test19(testNotFlattenableArray);
        Asserts.assertEQ(o, notFlattenable);
    }

    @Test
    @IR(applyIf = {"UseArrayLoadStoreProfile", "true"},
        failOn = {STORE_UNKNOWN_INLINE})
    @IR(applyIf = {"UseArrayLoadStoreProfile", "false"},
        counts = {STORE_UNKNOWN_INLINE, "= 1"})
    public void test20(Object[] array, Object o) {
        array[0] = o;
    }

    @Run(test = "test20")
    @Warmup(10000)
    public void test20_verifier() {
        test20(testIntegerArray, 42);
        test20(testNotFlattenableArray, notFlattenable);
    }

    // acmp tests

    // branch frequency profiling causes not equal branch to be optimized out
    @Test
    @IR(failOn = {SUBSTITUTABILITY_TEST})
    public boolean test21(Object o1, Object o2) {
        return o1 == o2;
    }

    @Run(test = "test21")
    @Warmup(10000)
    public void test21_verifier() {
        test21(42, 42);
        test21(testValue1, testValue1);
    }

    // Input profiled non null
    @Test
    @IR(applyIf = {"UseACmpProfile", "true"},
        failOn = {SUBSTITUTABILITY_TEST},
        counts = {NULL_ASSERT_TRAP, "= 1"})
    @IR(applyIf = {"UseACmpProfile", "false"},
        counts = {SUBSTITUTABILITY_TEST, "= 1"})
    public boolean test22(Object o1, Object o2) {
        return o1 == o2;
    }

    @Run(test = "test22")
    @Warmup(10000)
    public void test22_verifier(RunInfo info) {
        test22(42, null);
        test22(42.0, null);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertCompiledByC2(m);
            test22(42, 42.0);
            if (WBFlags.UseACmpProfile) {
                TestFramework.assertDeoptimizedByC2(m);
            }
        }
    }

    @Test
    @IR(applyIfOr = {"UseACmpProfile", "true", "TypeProfileLevel", "= 222"},
        failOn = {SUBSTITUTABILITY_TEST},
        counts = {NULL_ASSERT_TRAP, "= 1"})
    @IR(applyIfAnd = {"UseACmpProfile", "false", "TypeProfileLevel", "!= 222"},
        counts = {SUBSTITUTABILITY_TEST, "= 1"})
    public boolean test23(Object o1, Object o2) {
        return o1 == o2;
    }

    @Run(test = "test23")
    @Warmup(10000)
    public void test23_verifier(RunInfo info) {
        test23(null, 42);
        test23(null, 42.0);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertCompiledByC2(m);
            test23(42, 42.0);
            if (WBFlags.UseACmpProfile || WBFlags.TypeProfileLevel != 0) {
                TestFramework.assertDeoptimizedByC2(m);
            }
        }
    }

    @Test
    @IR(applyIf = {"UseACmpProfile", "true"},
        failOn = {SUBSTITUTABILITY_TEST},
        counts = {NULL_ASSERT_TRAP, "= 1"})
    @IR(applyIf = {"UseACmpProfile", "false"},
        counts = {SUBSTITUTABILITY_TEST, "= 1"})
    public boolean test24(Object o1, Object o2) {
        return o1 != o2;
    }

    @Run(test = "test24")
    @Warmup(10000)
    public void test24_verifier(RunInfo info) {
        test24(42, null);
        test24(42.0, null);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertCompiledByC2(m);
            test24(42, 42.0);
             if (WBFlags.UseACmpProfile) {
                 TestFramework.assertDeoptimizedByC2(m);
            }
        }
    }

    @Test
    @IR(applyIfOr = {"UseACmpProfile", "true", "TypeProfileLevel", "= 222"},
        failOn = {SUBSTITUTABILITY_TEST},
        counts = {NULL_ASSERT_TRAP, "= 1"})
    @IR(applyIfAnd = {"UseACmpProfile", "false", "TypeProfileLevel", "!= 222"},
        counts = {SUBSTITUTABILITY_TEST, "= 1"})
    public boolean test25(Object o1, Object o2) {
        return o1 != o2;
    }

    @Run(test = "test25")
    @Warmup(10000)
    public void test25_verifier(RunInfo info) {
        test25(null, 42);
        test25(null, 42.0);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertCompiledByC2(m);
            test25(42, 42.0);
            if (WBFlags.UseACmpProfile || WBFlags.TypeProfileLevel != 0) {
                TestFramework.assertDeoptimizedByC2(m);
            }
        }
    }

    // Input profiled not inline type with known type
    @Test
    @IR(applyIfOr = {"UseACmpProfile", "true", "TypeProfileLevel", "= 222"},
        failOn = {SUBSTITUTABILITY_TEST},
        counts = {NULL_CHECK_TRAP, "= 1", CLASS_CHECK_TRAP, "= 1"})
    @IR(applyIfAnd = {"UseACmpProfile", "false", "TypeProfileLevel", "!= 222"},
        counts = {SUBSTITUTABILITY_TEST, "= 1"})
    public boolean test26(Object o1, Object o2) {
        return o1 == o2;
    }

    @Run(test = "test26")
    @Warmup(10000)
    public void test26_verifier(RunInfo info) {
        test26(42, 42);
        test26(42, 42.0);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertCompiledByC2(m);
            for (int i = 0; i < 10; i++) {
                test26(42.0, 42);
            }
            if (WBFlags.UseACmpProfile || WBFlags.TypeProfileLevel != 0) {
                TestFramework.assertDeoptimizedByC2(m);
            }
        }
    }

    @Test
    @IR(applyIf = {"UseACmpProfile", "true"},
        failOn = {SUBSTITUTABILITY_TEST},
        counts = { NULL_CHECK_TRAP, "= 1", CLASS_CHECK_TRAP, "= 1"})
    @IR(applyIf = {"UseACmpProfile", "false"},
        counts = {SUBSTITUTABILITY_TEST, "= 1"})
    public boolean test27(Object o1, Object o2) {
        return o1 == o2;
    }

    @Run(test = "test27")
    @Warmup(10000)
    public void test27_verifier(RunInfo info) {
        test27(42, 42);
        test27(42.0, 42);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertCompiledByC2(m);
            for (int i = 0; i < 10; i++) {
                test27(42, 42.0);
            }
            if (WBFlags.UseACmpProfile) {
                TestFramework.assertDeoptimizedByC2(m);
            }
        }
    }

    @Test
    @IR(applyIfOr = {"UseACmpProfile", "true", "TypeProfileLevel", "= 222"},
        failOn = {SUBSTITUTABILITY_TEST},
        counts = {NULL_CHECK_TRAP, "= 1", CLASS_CHECK_TRAP, "= 1"})
    @IR(applyIfAnd = {"UseACmpProfile", "false", "TypeProfileLevel", "!= 222"},
        counts = {SUBSTITUTABILITY_TEST, "= 1"})
    public boolean test28(Object o1, Object o2) {
        return o1 != o2;
    }

    @Run(test = "test28")
    @Warmup(10000)
    public void test28_verifier(RunInfo info) {
        test28(42, 42);
        test28(42, 42.0);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertCompiledByC2(m);
            for (int i = 0; i < 10; i++) {
                test28(42.0, 42);
            }
            if (WBFlags.UseACmpProfile || WBFlags.TypeProfileLevel != 0) {
                TestFramework.assertDeoptimizedByC2(m);
            }
        }
    }

    @Test
    @IR(applyIf = {"UseACmpProfile", "true"},
        failOn = {SUBSTITUTABILITY_TEST},
        counts = {NULL_CHECK_TRAP, "= 1", CLASS_CHECK_TRAP, "= 1"})
    @IR(applyIf = {"UseACmpProfile", "false"},
        counts = {SUBSTITUTABILITY_TEST, "= 1"})
    public boolean test29(Object o1, Object o2) {
        return o1 != o2;
    }

    @Run(test = "test29")
    @Warmup(10000)
    public void test29_verifier(RunInfo info) {
        test29(42, 42);
        test29(42.0, 42);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertCompiledByC2(m);
            for (int i = 0; i < 10; i++) {
                test29(42, 42.0);
            }
            if (WBFlags.UseACmpProfile) {
                TestFramework.assertDeoptimizedByC2(m);
            }
        }
    }

    @Test
    @IR(applyIfOr = {"UseACmpProfile", "true", "TypeProfileLevel", "= 222"},
        failOn = {SUBSTITUTABILITY_TEST, NULL_CHECK_TRAP},
        counts = {CLASS_CHECK_TRAP, "= 1"})
    @IR(applyIfAnd = {"UseACmpProfile", "false", "TypeProfileLevel", "!= 222"},
        counts = {SUBSTITUTABILITY_TEST, "= 1"})
    public boolean test30(Object o1, Object o2) {
        return o1 == o2;
    }

    @Run(test = "test30")
    @Warmup(10000)
    public void test30_verifier(RunInfo info) {
        test30(42, 42);
        test30(42, 42.0);
        test30(null, 42);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertCompiledByC2(m);
            for (int i = 0; i < 10; i++) {
                test30(42.0, 42);
            }
            if (WBFlags.UseACmpProfile || WBFlags.TypeProfileLevel != 0) {
                TestFramework.assertDeoptimizedByC2(m);
            }
        }
    }

    @Test
    @IR(applyIf = {"UseACmpProfile", "true"},
        failOn = {SUBSTITUTABILITY_TEST, NULL_CHECK_TRAP})
    @IR(applyIf = {"UseACmpProfile", "false"},
        counts = {SUBSTITUTABILITY_TEST, "= 1"})
    public boolean test31(Object o1, Object o2) {
        return o1 == o2;
    }

    @Run(test = "test31")
    @Warmup(10000)
    public void test31_verifier(RunInfo info) {
        test31(42, 42);
        test31(42.0, 42);
        test31(42, null);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertCompiledByC2(m);
            for (int i = 0; i < 10; i++) {
                test31(42, 42.0);
            }
            if (WBFlags.UseACmpProfile) {
                TestFramework.assertDeoptimizedByC2(m);
            }
        }
    }

    // Input profiled not inline type with unknown type
    @Test
    @IR(applyIf = {"UseACmpProfile", "true"},
        failOn = {SUBSTITUTABILITY_TEST},
        counts = {NULL_CHECK_TRAP, "= 1", CLASS_CHECK_TRAP, "= 1"})
    @IR(applyIf = {"UseACmpProfile", "false"},
        counts = {SUBSTITUTABILITY_TEST, "= 1"})
    public boolean test32(Object o1, Object o2) {
        return o1 == o2;
    }

    @Run(test = "test32")
    @Warmup(10000)
    public void test32_verifier(RunInfo info) {
        test32(42, 42);
        test32(42, testValue1);
        test32(42.0, 42);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertCompiledByC2(m);
            for (int i = 0; i < 10; i++) {
                test32(testValue1, 42);
            }
            if (WBFlags.UseACmpProfile) {
                TestFramework.assertDeoptimizedByC2(m);
            }
        }
    }

    @Test
    @IR(applyIf = {"UseACmpProfile", "true"},
        failOn = {SUBSTITUTABILITY_TEST},
        counts = {NULL_CHECK_TRAP, "= 1", CLASS_CHECK_TRAP, "= 1"})
    @IR(applyIf = {"UseACmpProfile", "false"},
        counts = {SUBSTITUTABILITY_TEST, "= 1"})
    public boolean test33(Object o1, Object o2) {
        return o1 == o2;
    }

    @Run(test = "test33")
    @Warmup(10000)
    public void test33_verifier(RunInfo info) {
        test33(42, 42);
        test33(testValue1, 42);
        test33(42, 42.0);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertCompiledByC2(m);
            for (int i = 0; i < 10; i++) {
                test33(42, testValue1);
            }
            if (WBFlags.UseACmpProfile) {
                TestFramework.assertDeoptimizedByC2(m);
            }
        }
    }

    @Test
    @IR(applyIf = {"UseACmpProfile", "true"},
        failOn = {SUBSTITUTABILITY_TEST},
        counts = {NULL_CHECK_TRAP, "= 1", CLASS_CHECK_TRAP, "= 1"})
    @IR(applyIf = {"UseACmpProfile", "false"},
        counts = {SUBSTITUTABILITY_TEST, "= 1"})
    public boolean test34(Object o1, Object o2) {
        return o1 != o2;
    }

    @Run(test = "test34")
    @Warmup(10000)
    public void test34_verifier(RunInfo info) {
        test34(42, 42);
        test34(42, testValue1);
        test34(42.0, 42);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertCompiledByC2(m);
            for (int i = 0; i < 10; i++) {
                test34(testValue1, 42);
            }
            if (WBFlags.UseACmpProfile) {
                TestFramework.assertDeoptimizedByC2(m);
            }
        }
    }

    @Test
    @IR(applyIf = {"UseACmpProfile", "true"},
        failOn = {SUBSTITUTABILITY_TEST},
        counts = {NULL_CHECK_TRAP, "= 1", CLASS_CHECK_TRAP, "= 1"})
    @IR(applyIf = {"UseACmpProfile", "false"},
        counts = {SUBSTITUTABILITY_TEST, "= 1"})
    public boolean test35(Object o1, Object o2) {
        return o1 != o2;
    }

    @Run(test = "test35")
    @Warmup(10000)
    public void test35_verifier(RunInfo info) {
        test35(42, 42);
        test35(testValue1, 42);
        test35(42, 42.0);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertCompiledByC2(m);
            for (int i = 0; i < 10; i++) {
                test35(42, testValue1);
            }
            if (WBFlags.UseACmpProfile) {
                TestFramework.assertDeoptimizedByC2(m);
            }
        }
    }

    @Test
    @IR(applyIf = {"UseACmpProfile", "true"},
        failOn = {SUBSTITUTABILITY_TEST, NULL_CHECK_TRAP},
        counts = {CLASS_CHECK_TRAP, "= 1"})
    @IR(applyIf = {"UseACmpProfile", "false"},
        counts = {SUBSTITUTABILITY_TEST, "= 1"})
    public boolean test36(Object o1, Object o2) {
        return o1 == o2;
    }

    @Run(test = "test36")
    @Warmup(10000)
    public void test36_verifier(RunInfo info) {
        test36(42, 42.0);
        test36(42.0, testValue1);
        test36(null, 42);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertCompiledByC2(m);
            for (int i = 0; i < 10; i++) {
                test36(testValue1, 42);
            }
            if (WBFlags.UseACmpProfile) {
                TestFramework.assertDeoptimizedByC2(m);
            }
        }
    }

    @Test
    @IR(applyIf = {"UseACmpProfile", "true"},
        failOn = {SUBSTITUTABILITY_TEST, NULL_CHECK_TRAP})
    @IR(applyIf = {"UseACmpProfile", "false"},
        counts = {SUBSTITUTABILITY_TEST, "= 1"})
    public boolean test37(Object o1, Object o2) {
        return o1 == o2;
    }

    @Run(test = "test37")
    @Warmup(10000)
    public void test37_verifier(RunInfo info) {
        test37(42.0, 42);
        test37(testValue1, 42.0);
        test37(42, null);
        if (!info.isWarmUp()) {
            Method m = info.getTest();
            TestFramework.assertCompiledByC2(m);
            for (int i = 0; i < 10; i++) {
                test37(42, testValue1);
            }
            if (WBFlags.UseACmpProfile) {
                TestFramework.assertDeoptimizedByC2(m);
            }
        }
    }

    // Test that acmp profile data that's unused at the acmp is fed to
    // speculation and leverage later
    @Test
    @IR(applyIfOr = {"UseACmpProfile", "true", "TypeProfileLevel", "= 222"},
        failOn = {SUBSTITUTABILITY_TEST},
        counts = {CLASS_CHECK_TRAP, "= 1"})
    @IR(applyIfAnd = {"UseACmpProfile", "false", "TypeProfileLevel", "!= 222"},
        counts = {SUBSTITUTABILITY_TEST, "= 1"})
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

    @Run(test = "test38")
    @Warmup(10000)
    public void test38_verifier() {
        test38(42, 42, 42);
        test38_helper(testValue1, testValue2);
    }


    @Test
    @IR(applyIfOr = {"UseACmpProfile", "true", "TypeProfileLevel", "= 222"},
        failOn = {SUBSTITUTABILITY_TEST},
        counts = {CLASS_CHECK_TRAP, "= 1"})
    @IR(applyIfAnd = {"UseACmpProfile", "false", "TypeProfileLevel", "!= 222"},
        counts = {SUBSTITUTABILITY_TEST, "= 1"})
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

    @Run(test = "test39")
    @Warmup(10000)
    public void test39_verifier() {
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

    @Test
    public Object test40(Test40Abstract[] array) {
        return test40_access(array);
    }

    @Run(test = "test40")
    @Warmup(10000)
    public void test40_verifier(RunInfo info) {
        // Make sure multiple implementors of Test40Abstract are loaded
        Test40Inline tmp1 = new Test40Inline();
        Test40Class tmp2 = new Test40Class();
        if (info.isWarmUp()) {
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

    @Test
    public void test41(Test40Inline[] array, Object val) {
        test41_access(array, val);
    }

    @Run(test = "test41")
    @Warmup(10000)
    public void test41_verifier(RunInfo info) {
        // Make sure multiple implementors of Test40Abstract are loaded
        Test40Inline tmp1 = new Test40Inline();
        Test40Class tmp2 = new Test40Class();
        if (info.isWarmUp()) {
            // Pollute profile with exact Object[]
            test41_access(new Object[1], new Object());
        } else {
            // When inlining test41_access, profiling contradicts actual type of array
            test41(new Test40Inline[1], new Test40Inline());
        }
    }
}
