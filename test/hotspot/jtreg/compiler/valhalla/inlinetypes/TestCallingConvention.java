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

/*
 * @test
 * @key randomness
 * @summary Test inline type calling convention optimizations
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires (os.simpleArch == "x64" | os.simpleArch == "aarch64")
 * @compile TestCallingConvention.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=300 -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                               compiler.valhalla.inlinetypes.InlineTypeTest
 *                               compiler.valhalla.inlinetypes.TestCallingConvention
 */
public class TestCallingConvention extends InlineTypeTest {
    // Extra VM parameters for some test scenarios. See InlineTypeTest.getVMParameters()
    @Override
    public String[] getExtraVMParameters(int scenario) {
        switch (scenario) {
        case 0: return new String[] {"-Dsun.reflect.inflationThreshold=10000"}; // Don't generate bytecodes but call through runtime for reflective calls
        case 1: return new String[] {"-Dsun.reflect.inflationThreshold=10000"};
        case 3: return new String[] {"-XX:FlatArrayElementMaxSize=0"};
        }
        return null;
    }

    static {
        try {
            Class<?> clazz = TestCallingConvention.class;
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodType mt = MethodType.methodType(MyValue2.class, boolean.class);
            test32_mh = lookup.findVirtual(clazz, "test32_interp", mt);

            mt = MethodType.methodType(Object.class, boolean.class);
            test33_mh = lookup.findVirtual(clazz, "test33_interp", mt);

            mt = MethodType.methodType(int.class);
            test37_mh = lookup.findVirtual(Test37Value.class, "test", mt);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("Method handle lookup failed");
        }
    }

    public static void main(String[] args) throws Throwable {
        TestCallingConvention test = new TestCallingConvention();
        test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class, MyValue3.class, MyValue3Inline.class, MyValue4.class,
                 Test27Value1.class, Test27Value2.class, Test27Value3.class, Test37Value.class, EmptyContainer.class, MixedContainer.class);
    }

    // Test interpreter to compiled code with various signatures
    @Test(failOn = ALLOC + STORE + TRAP)
    public long test1(MyValue2 v) {
        return v.hash();
    }

    @DontCompile
    public void test1_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, rD);
        long result = test1(v);
        Asserts.assertEQ(result, v.hashInterpreted());
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test2(int i1, MyValue2 v, int i2) {
        return v.hash() + i1 - i2;
    }

    @DontCompile
    public void test2_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, rD);
        long result = test2(rI, v, 2*rI);
        Asserts.assertEQ(result, v.hashInterpreted() - rI);
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test3(long l1, MyValue2 v, long l2) {
        return v.hash() + l1 - l2;
    }

    @DontCompile
    public void test3_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, rD);
        long result = test3(rL, v, 2*rL);
        Asserts.assertEQ(result, v.hashInterpreted() - rL);
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test4(int i, MyValue2 v, long l) {
        return v.hash() + i + l;
    }

    @DontCompile
    public void test4_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, rD);
        long result = test4(rI, v, rL);
        Asserts.assertEQ(result, v.hashInterpreted() + rL + rI);
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test5(long l, MyValue2 v, int i) {
        return v.hash() + i + l;
    }

    @DontCompile
    public void test5_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, rD);
        long result = test5(rL, v, rI);
        Asserts.assertEQ(result, v.hashInterpreted() + rL + rI);
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test6(long l, MyValue1 v1, int i, MyValue2 v2) {
        return v1.hash() + i + l + v2.hash();
    }

    @DontCompile
    public void test6_verifier(boolean warmup) {
        MyValue1 v1 = MyValue1.createWithFieldsDontInline(rI, rL);
        MyValue2 v2 = MyValue2.createWithFieldsInline(rI, rD);
        long result = test6(rL, v1, rI, v2);
        Asserts.assertEQ(result, v1.hashInterpreted() + rL + rI + v2.hashInterpreted());
    }

    // Test compiled code to interpreter with various signatures
    @DontCompile
    public long test7_interp(MyValue2 v) {
        return v.hash();
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test7(MyValue2 v) {
        return test7_interp(v);
    }

    @DontCompile
    public void test7_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, rD);
        long result = test7(v);
        Asserts.assertEQ(result, v.hashInterpreted());
    }

    @DontCompile
    public long test8_interp(int i1, MyValue2 v, int i2) {
        return v.hash() + i1 - i2;
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test8(int i1, MyValue2 v, int i2) {
        return test8_interp(i1, v, i2);
    }

    @DontCompile
    public void test8_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, rD);
        long result = test8(rI, v, 2*rI);
        Asserts.assertEQ(result, v.hashInterpreted() - rI);
    }

    @DontCompile
    public long test9_interp(long l1, MyValue2 v, long l2) {
        return v.hash() + l1 - l2;
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test9(long l1, MyValue2 v, long l2) {
        return test9_interp(l1, v, l2);
    }

    @DontCompile
    public void test9_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, rD);
        long result = test9(rL, v, 2*rL);
        Asserts.assertEQ(result, v.hashInterpreted() - rL);
    }

    @DontCompile
    public long test10_interp(int i, MyValue2 v, long l) {
        return v.hash() + i + l;
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test10(int i, MyValue2 v, long l) {
        return test10_interp(i, v, l);
    }

    @DontCompile
    public void test10_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, rD);
        long result = test10(rI, v, rL);
        Asserts.assertEQ(result, v.hashInterpreted() + rL + rI);
    }

    @DontCompile
    public long test11_interp(long l, MyValue2 v, int i) {
        return v.hash() + i + l;
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test11(long l, MyValue2 v, int i) {
        return test11_interp(l, v, i);
    }

    @DontCompile
    public void test11_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, rD);
        long result = test11(rL, v, rI);
        Asserts.assertEQ(result, v.hashInterpreted() + rL + rI);
    }

    @DontCompile
    public long test12_interp(long l, MyValue1 v1, int i, MyValue2 v2) {
        return v1.hash() + i + l + v2.hash();
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test12(long l, MyValue1 v1, int i, MyValue2 v2) {
        return test12_interp(l, v1, i, v2);
    }

    @DontCompile
    public void test12_verifier(boolean warmup) {
        MyValue1 v1 = MyValue1.createWithFieldsDontInline(rI, rL);
        MyValue2 v2 = MyValue2.createWithFieldsInline(rI, rD);
        long result = test12(rL, v1, rI, v2);
        Asserts.assertEQ(result, v1.hashInterpreted() + rL + rI + v2.hashInterpreted());
    }

    // Test that debug info at a call is correct
    @DontCompile
    public long test13_interp(MyValue2 v, MyValue1[] va, boolean deopt) {
        if (deopt) {
            // uncommon trap
            WHITE_BOX.deoptimizeMethod(tests.get(getClass().getSimpleName() + "::test13"));
        }
        return v.hash() + va[0].hash() + va[1].hash();
    }

    @Test(failOn = ALLOC + STORE + TRAP)
    public long test13(MyValue2 v, MyValue1[] va, boolean flag, long l) {
        return test13_interp(v, va, flag) + l;
    }

    @DontCompile
    public void test13_verifier(boolean warmup) {
        MyValue2 v = MyValue2.createWithFieldsInline(rI, rD);
        MyValue1[] va = new MyValue1[2];
        va[0] = MyValue1.createWithFieldsDontInline(rI, rL);
        va[1] = MyValue1.createWithFieldsDontInline(rI, rL);
        long result = test13(v, va, !warmup, rL);
        Asserts.assertEQ(result, v.hashInterpreted() + va[0].hash() + va[1].hash() + rL);
    }

    // Test deoptimization at call return with inline type returned in registers
    @DontCompile
    public MyValue2 test14_interp(boolean deopt) {
        if (deopt) {
            // uncommon trap
            WHITE_BOX.deoptimizeMethod(tests.get(getClass().getSimpleName() + "::test14"));
        }
        return MyValue2.createWithFieldsInline(rI, rD);
    }

    @Test()
    public MyValue2 test14(boolean flag) {
        return test14_interp(flag);
    }

    @DontCompile
    public void test14_verifier(boolean warmup) {
        MyValue2 result = test14(!warmup);
        MyValue2 v = MyValue2.createWithFieldsInline(rI, rD);
        Asserts.assertEQ(result.hash(), v.hash());
    }

    // Return inline types in registers from interpreter -> compiled
    final MyValue3 test15_vt = MyValue3.create();
    @DontCompile
    public MyValue3 test15_interp() {
        return test15_vt;
    }

    MyValue3 test15_vt2;
    @Test(valid = InlineTypeReturnedAsFieldsOn, failOn = ALLOC + LOAD + TRAP)
    @Test(valid = InlineTypeReturnedAsFieldsOff)
    public void test15() {
        test15_vt2 = test15_interp();
    }

    @DontCompile
    public void test15_verifier(boolean warmup) {
        test15();
        test15_vt.verify(test15_vt2);
    }

    // Return inline types in registers from compiled -> interpreter
    final MyValue3 test16_vt = MyValue3.create();
    @Test(valid = InlineTypeReturnedAsFieldsOn, failOn = ALLOC + STORE + TRAP)
    @Test(valid = InlineTypeReturnedAsFieldsOff)
    public MyValue3 test16() {
        return test16_vt;
    }

    @DontCompile
    public void test16_verifier(boolean warmup) {
        MyValue3 vt = test16();
        test16_vt.verify(vt);
    }

    // Return inline types in registers from compiled -> compiled
    final MyValue3 test17_vt = MyValue3.create();
    @DontInline
    public MyValue3 test17_comp() {
        return test17_vt;
    }

    MyValue3 test17_vt2;
    @Test(valid = InlineTypeReturnedAsFieldsOn, failOn = ALLOC + LOAD + TRAP)
    @Test(valid = InlineTypeReturnedAsFieldsOff)
    public void test17() {
        test17_vt2 = test17_comp();
    }

    @DontCompile
    public void test17_verifier(boolean warmup) throws Exception {
        Method helper_m = getClass().getDeclaredMethod("test17_comp");
        if (!warmup && USE_COMPILER && !WHITE_BOX.isMethodCompiled(helper_m, false)) {
            enqueueMethodForCompilation(helper_m, COMP_LEVEL_FULL_OPTIMIZATION);
            Asserts.assertTrue(WHITE_BOX.isMethodCompiled(helper_m, false), "test17_comp not compiled");
        }
        test17();
        test17_vt.verify(test17_vt2);
    }

    // Same tests as above but with an inline type that cannot be returned in registers

    // Return inline types in registers from interpreter -> compiled
    final MyValue4 test18_vt = MyValue4.create();
    @DontCompile
    public MyValue4 test18_interp() {
        return test18_vt;
    }

    MyValue4 test18_vt2;
    @Test
    public void test18() {
        test18_vt2 = test18_interp();
    }

    @DontCompile
    public void test18_verifier(boolean warmup) {
        test18();
        test18_vt.verify(test18_vt2);
    }

    // Return inline types in registers from compiled -> interpreter
    final MyValue4 test19_vt = MyValue4.create();
    @Test
    public MyValue4 test19() {
        return test19_vt;
    }

    @DontCompile
    public void test19_verifier(boolean warmup) {
        MyValue4 vt = test19();
        test19_vt.verify(vt);
    }

    // Return inline types in registers from compiled -> compiled
    final MyValue4 test20_vt = MyValue4.create();
    @DontInline
    public MyValue4 test20_comp() {
        return test20_vt;
    }

    MyValue4 test20_vt2;
    @Test
    public void test20() {
        test20_vt2 = test20_comp();
    }

    @DontCompile
    public void test20_verifier(boolean warmup) throws Exception {
        Method helper_m = getClass().getDeclaredMethod("test20_comp");
        if (!warmup && USE_COMPILER && !WHITE_BOX.isMethodCompiled(helper_m, false)) {
            enqueueMethodForCompilation(helper_m, COMP_LEVEL_FULL_OPTIMIZATION);
            Asserts.assertTrue(WHITE_BOX.isMethodCompiled(helper_m, false), "test20_comp not compiled");
        }
        test20();
        test20_vt.verify(test20_vt2);
    }

    // Test no result from inlined method for incremental inlining
    final MyValue3 test21_vt = MyValue3.create();
    public MyValue3 test21_inlined() {
        throw new RuntimeException();
    }

    @Test
    public MyValue3 test21() {
        try {
            return test21_inlined();
        } catch (RuntimeException ex) {
            return test21_vt;
        }
    }

    @DontCompile
    public void test21_verifier(boolean warmup) {
        MyValue3 vt = test21();
        test21_vt.verify(vt);
    }

    // Test returning a non-flattened inline type as fields
    MyValue3.ref test22_vt = MyValue3.create();

    @Test
    public MyValue3 test22() {
        return (MyValue3) test22_vt;
    }

    @DontCompile
    public void test22_verifier(boolean warmup) {
        MyValue3 vt = test22();
        test22_vt.verify(vt);
    }

    // Test calling a method that has circular register/stack dependencies when unpacking inline type arguments
    primitive class TestValue23 {
        final double f1;
        TestValue23(double val) {
            f1 = val;
        }
    }

    static double test23Callee(int i1, int i2, int i3, int i4, int i5, int i6,
                               TestValue23 v1, TestValue23 v2, TestValue23 v3, TestValue23 v4, TestValue23 v5, TestValue23 v6, TestValue23 v7, TestValue23 v8,
                               double d1, double d2, double d3, double d4, double d5, double d6, double d7, double d8) {
        return i1 + i2 + i3 + i4 + i5 + i6 + v1.f1 + v2.f1 + v3.f1 + v4.f1 + v5.f1 + v6.f1 + v7.f1 + v8.f1 + d1 + d2 + d3 + d4 + d5 + d6 + d7 + d8;
    }

    @Test
    public double test23(int i1, int i2, int i3, int i4, int i5, int i6,
                         TestValue23 v1, TestValue23 v2, TestValue23 v3, TestValue23 v4, TestValue23 v5, TestValue23 v6, TestValue23 v7, TestValue23 v8,
                         double d1, double d2, double d3, double d4, double d5, double d6, double d7, double d8) {
        return test23Callee(i1, i2, i3, i4, i5, i6,
                            v1, v2, v3, v4, v5, v6, v7, v8,
                            d1, d2, d3, d4, d5, d6, d7, d8);
    }

    @DontCompile
    public void test23_verifier(boolean warmup) {
        TestValue23 vt = new TestValue23(rI);
        double res1 = test23(rI, rI, rI, rI, rI, rI,
                            vt, vt, vt, vt, vt, vt, vt, vt,
                            rI, rI, rI, rI, rI, rI, rI, rI);
        double res2 = test23Callee(rI, rI, rI, rI, rI, rI,
                                   vt, vt, vt, vt, vt, vt, vt, vt,
                                   rI, rI, rI, rI, rI, rI, rI, rI);
        double res3 = 6*rI + 8*rI + 8*rI;
        Asserts.assertEQ(res1, res2);
        Asserts.assertEQ(res2, res3);
    }

    // Should not return a nullable inline type as fields
    @Test
    public MyValue2.ref test24() {
        return null;
    }

    @DontCompile
    public void test24_verifier(boolean warmup) {
        MyValue2.ref vt = test24();
        Asserts.assertEQ(vt, null);
    }

    // Same as test24 but with control flow and inlining
    @ForceInline
    public MyValue2.ref test26_callee(boolean b) {
        if (b) {
            return null;
        } else {
            return MyValue2.createWithFieldsInline(rI, rD);
        }
    }

    @Test
    public MyValue2.ref test26(boolean b) {
        return test26_callee(b);
    }

    @DontCompile
    public void test26_verifier(boolean warmup) {
        MyValue2.ref vt = test26(true);
        Asserts.assertEQ(vt, null);
        vt = test26(false);
        Asserts.assertEQ(vt.hash(), MyValue2.createWithFieldsInline(rI, rD).hash());
    }

    // Test calling convention with deep hierarchy of flattened fields
    final primitive class Test27Value1 {
        final Test27Value2 valueField;

        private Test27Value1(Test27Value2 val2) {
            valueField = val2;
        }

        @DontInline
        public int test(Test27Value1 val1) {
            return valueField.test(valueField) + val1.valueField.test(valueField);
        }
    }

    final primitive class Test27Value2 {
        final Test27Value3 valueField;

        private Test27Value2(Test27Value3 val3) {
            valueField = val3;
        }

        @DontInline
        public int test(Test27Value2 val2) {
            return valueField.test(valueField) + val2.valueField.test(valueField);
        }
    }

    final primitive class Test27Value3 {
        final int x;

        private Test27Value3(int x) {
            this.x = x;
        }

        @DontInline
        public int test(Test27Value3 val3) {
            return x + val3.x;
        }
    }

    @Test
    public int test27(Test27Value1 val) {
        return val.test(val);
    }

    @DontCompile
    public void test27_verifier(boolean warmup) {
        Test27Value3 val3 = new Test27Value3(rI);
        Test27Value2 val2 = new Test27Value2(val3);
        Test27Value1 val1 = new Test27Value1(val2);
        int result = test27(val1);
        Asserts.assertEQ(result, 8*rI);
    }

    static final MyValue1.ref test28Val = MyValue1.createWithFieldsDontInline(rI, rL);

    @Test
    @Warmup(0)
    public String test28() {
        return test28Val.toString();
    }

    @DontCompile
    public void test28_verifier(boolean warmup) {
        String result = test28();
    }

    // Test calling a method returning an inline type as fields via reflection
    MyValue3 test29_vt = MyValue3.create();

    @Test
    public MyValue3 test29() {
        return test29_vt;
    }

    @DontCompile
    public void test29_verifier(boolean warmup) throws Exception {
        MyValue3 vt = (MyValue3)TestCallingConvention.class.getDeclaredMethod("test29").invoke(this);
        test29_vt.verify(vt);
    }

    @Test
    public MyValue3 test30(MyValue3[] array) {
        MyValue3 result = MyValue3.create();
        array[0] = result;
        return result;
    }

    @DontCompile
    public void test30_verifier(boolean warmup) throws Exception {
        MyValue3[] array = new MyValue3[1];
        MyValue3 vt = (MyValue3)TestCallingConvention.class.getDeclaredMethod("test30", MyValue3[].class).invoke(this, (Object)array);
        array[0].verify(vt);
    }

    MyValue3 test31_vt;

    @Test
    public MyValue3 test31() {
        MyValue3 result = MyValue3.create();
        test31_vt = result;
        return result;
    }

    @DontCompile
    public void test31_verifier(boolean warmup) throws Exception {
        MyValue3 vt = (MyValue3)TestCallingConvention.class.getDeclaredMethod("test31").invoke(this);
        test31_vt.verify(vt);
    }

    // Test deoptimization at call return with inline type returned in registers.
    // Same as test14, except the interpreted method is called via a MethodHandle.
    static MethodHandle test32_mh;

    @DontCompile
    public MyValue2 test32_interp(boolean deopt) {
        if (deopt) {
            // uncommon trap
            WHITE_BOX.deoptimizeMethod(tests.get(getClass().getSimpleName() + "::test32"));
        }
        return MyValue2.createWithFieldsInline(rI+32, rD);
    }

    @Test()
    public MyValue2 test32(boolean flag) throws Throwable {
        return (MyValue2)test32_mh.invokeExact(this, flag);
    }

    @DontCompile
    public void test32_verifier(boolean warmup) throws Throwable {
        MyValue2 result = test32(!warmup);
        MyValue2 v = MyValue2.createWithFieldsInline(rI+32, rD);
        Asserts.assertEQ(result.hash(), v.hash());
    }

    // Same as test32, except the return type is not flattenable.
    static MethodHandle test33_mh;

    @DontCompile
    public Object test33_interp(boolean deopt) {
        if (deopt) {
            // uncommon trap
            WHITE_BOX.deoptimizeMethod(tests.get(getClass().getSimpleName() + "::test33"));
        }
        return MyValue2.createWithFieldsInline(rI+33, rD);
    }

    @Test()
    public MyValue2 test33(boolean flag) throws Throwable {
        Object o = test33_mh.invokeExact(this, flag);
        return (MyValue2)o;
    }

    @DontCompile
    public void test33_verifier(boolean warmup) throws Throwable {
        MyValue2 result = test33(!warmup);
        MyValue2 v = MyValue2.createWithFieldsInline(rI+33, rD);
        Asserts.assertEQ(result.hash(), v.hash());
    }

    // Test selection of correct entry point in SharedRuntime::handle_wrong_method
    static boolean test34_deopt = false;

    @DontInline
    public static long test34_callee(MyValue2 vt, int i1, int i2, int i3, int i4) {
        Asserts.assertEQ(i1, rI);
        Asserts.assertEQ(i2, rI);
        Asserts.assertEQ(i3, rI);
        Asserts.assertEQ(i4, rI);

        if (test34_deopt) {
            // uncommon trap
            int result = 0;
            for (int i = 0; i < 10; ++i) {
                result += rL;
            }
            return vt.hash() + i1 + i2 + i3 + i4 + result;
        }
        return vt.hash() + i1 + i2 + i3 + i4;
    }

    @Test()
    @Warmup(10000) // Make sure test34_callee is compiled
    public static long test34(MyValue2 vt, int i1, int i2, int i3, int i4) {
        return test34_callee(vt, i1, i2, i3, i4);
    }

    @DontCompile
    public void test34_verifier(boolean warmup) {
        MyValue2 vt = MyValue2.createWithFieldsInline(rI, rD);
        long result = test34(vt, rI, rI, rI, rI);
        Asserts.assertEQ(result, vt.hash()+4*rI);
        if (!warmup) {
            test34_deopt = true;
            for (int i = 0; i < 100; ++i) {
                result = test34(vt, rI, rI, rI, rI);
                Asserts.assertEQ(result, vt.hash()+4*rI+10*rL);
            }
        }
    }

    // Test OSR compilation of method with scalarized argument
    @Test()
    public static long test35(MyValue2 vt, int i1, int i2, int i3, int i4) {
        int result = 0;
        // Trigger OSR compilation
        for (int i = 0; i < 10_000; ++i) {
            result += i1;
        }
        return vt.hash() + i1 + i2 + i3 + i4 + result;
    }

    @DontCompile
    public void test35_verifier(boolean warmup) {
        MyValue2 vt = MyValue2.createWithFieldsInline(rI, rD);
        long result = test35(vt, rI, rI, rI, rI);
        Asserts.assertEQ(result, vt.hash()+10004*rI);
    }

    // Same as test31 but with GC in callee to verify that the
    // pre-allocated buffer for the returned inline type remains valid.
    MyValue3 test36_vt;

    @Test
    public MyValue3 test36() {
        MyValue3 result = MyValue3.create();
        test36_vt = result;
        System.gc();
        return result;
    }

    @DontCompile
    public void test36_verifier(boolean warmup) throws Exception {
        MyValue3 vt = (MyValue3)TestCallingConvention.class.getDeclaredMethod("test36").invoke(this);
        test36_vt.verify(vt);
    }

    // Test method resolution with scalarized inline type receiver at invokespecial
    static final MethodHandle test37_mh;

    primitive class Test37Value {
        int x = rI;

        @DontInline
        public int test() {
            return x;
        }
    }

    @Test
    public int test37(Test37Value vt) throws Throwable {
        // Generates invokespecial call of Test37Value::test
        return (int)test37_mh.invokeExact(vt);
    }

    @DontCompile
    public void test37_verifier(boolean warmup) throws Throwable {
        Test37Value vt = new Test37Value();
        int res = test37(vt);
        Asserts.assertEQ(res, rI);
    }

    // Test passing/returning an empty inline type
    @Test(failOn = ALLOC + LOAD + STORE + TRAP)
    public MyValueEmpty test38(MyValueEmpty vt) {
        return vt.copy(vt);
    }

    @DontCompile
    public void test38_verifier(boolean warmup) {
        MyValueEmpty vt = new MyValueEmpty();
        MyValueEmpty res = test38(vt);
        Asserts.assertEQ(res, vt);
    }

    static primitive class LargeValueWithOops {
        // Use all 6 int registers + 50/2 on stack = 29
        Object o1 = null;
        Object o2 = null;
        Object o3 = null;
        Object o4 = null;
        Object o5 = null;
        Object o6 = null;
        Object o7 = null;
        Object o8 = null;
        Object o9 = null;
        Object o10 = null;
        Object o11 = null;
        Object o12 = null;
        Object o13 = null;
        Object o14 = null;
        Object o15 = null;
        Object o16 = null;
        Object o17 = null;
        Object o18 = null;
        Object o19 = null;
        Object o20 = null;
        Object o21 = null;
        Object o22 = null;
        Object o23 = null;
        Object o24 = null;
        Object o25 = null;
        Object o26 = null;
        Object o27 = null;
        Object o28 = null;
        Object o29 = null;
    }

    static primitive class LargeValueWithoutOops {
        // Use all 6 int registers + 50/2 on stack = 29
        int i1 = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        int i6 = 0;
        int i7 = 0;
        int i8 = 0;
        int i9 = 0;
        int i10 = 0;
        int i11 = 0;
        int i12 = 0;
        int i13 = 0;
        int i14 = 0;
        int i15 = 0;
        int i16 = 0;
        int i17 = 0;
        int i18 = 0;
        int i19 = 0;
        int i20 = 0;
        int i21 = 0;
        int i22 = 0;
        int i23 = 0;
        int i24 = 0;
        int i25 = 0;
        int i26 = 0;
        int i27 = 0;
        int i28 = 0;
        int i29 = 0;
        // Use all 7 float registers
        double d1 = 0;
        double d2 = 0;
        double d3 = 0;
        double d4 = 0;
        double d5 = 0;
        double d6 = 0;
        double d7 = 0;
        double d8 = 0;
    }

    // Test passing/returning a large inline type with oop fields
    @Test()
    public static LargeValueWithOops test39(LargeValueWithOops vt) {
        return vt;
    }

    @DontCompile
    public void test39_verifier(boolean warmup) {
        LargeValueWithOops vt = new LargeValueWithOops();
        LargeValueWithOops res = test39(vt);
        Asserts.assertEQ(res, vt);
    }

    // Test passing/returning a large inline type with only int/float fields
    @Test()
    public static LargeValueWithoutOops test40(LargeValueWithoutOops vt) {
        return vt;
    }

    @DontCompile
    public void test40_verifier(boolean warmup) {
        LargeValueWithoutOops vt = new LargeValueWithoutOops();
        LargeValueWithoutOops res = test40(vt);
        Asserts.assertEQ(res, vt);
    }

    // Test passing/returning an empty inline type together with non-empty
    // inline types such that only some inline type arguments are scalarized.
    @Test(failOn = ALLOC + LOAD + STORE + TRAP)
    public MyValueEmpty test41(MyValue1 vt1, MyValueEmpty vt2, MyValue1 vt3) {
        return vt2.copy(vt2);
    }

    @DontCompile
    public void test41_verifier(boolean warmup) {
        MyValueEmpty res = test41(MyValue1.default, MyValueEmpty.default, MyValue1.default);
        Asserts.assertEQ(res, MyValueEmpty.default);
    }

    // More empty inline type tests with containers

    static primitive class EmptyContainer {
        private MyValueEmpty empty;

        EmptyContainer(MyValueEmpty empty) {
            this.empty = empty;
        }

        @ForceInline
        MyValueEmpty getInline() { return empty; }

        @DontInline
        MyValueEmpty getNoInline() { return empty; }
    }

    static primitive class MixedContainer {
        public int val;
        private EmptyContainer empty;

        MixedContainer(int val, EmptyContainer empty) {
            this.val = val;
            this.empty = empty;
        }

        @ForceInline
        EmptyContainer getInline() { return empty; }

        @DontInline
        EmptyContainer getNoInline() { return empty; }
    }

    // Empty inline type return
    @Test(failOn = ALLOC + LOAD + STORE + TRAP)
    public MyValueEmpty test42() {
        EmptyContainer c = new EmptyContainer(MyValueEmpty.default);
        return c.getInline();
    }

    @DontCompile
    public void test42_verifier(boolean warmup) {
        MyValueEmpty empty = test42();
        Asserts.assertEquals(empty, MyValueEmpty.default);
    }

    // Empty inline type container return
    @Test(failOn = ALLOC + LOAD + STORE + TRAP)
    public EmptyContainer test43(EmptyContainer c) {
        return c;
    }

    @DontCompile
    public void test43_verifier(boolean warmup) {
        EmptyContainer c = test43(EmptyContainer. default);
        Asserts.assertEquals(c, EmptyContainer.default);
    }

    // Empty inline type container (mixed) return
    @Test(failOn = ALLOC + LOAD + STORE + TRAP)
    public MixedContainer test44() {
        MixedContainer c = new MixedContainer(rI, EmptyContainer.default);
        c = new MixedContainer(rI, c.getInline());
        return c;
    }

    @DontCompile
    public void test44_verifier(boolean warmup) {
        MixedContainer c = test44();
        Asserts.assertEquals(c, new MixedContainer(rI, EmptyContainer.default));
    }

    // Empty inline type container argument
    @Test(failOn = ALLOC + LOAD + STORE + TRAP)
    public EmptyContainer test45(EmptyContainer c) {
        return new EmptyContainer(c.getInline());
    }

    @DontCompile
    public void test45_verifier(boolean warmup) {
        EmptyContainer empty = test45(EmptyContainer.default);
        Asserts.assertEquals(empty, EmptyContainer.default);
    }

    // Empty inline type container and mixed container arguments
    @Test(failOn = ALLOC + LOAD + STORE + TRAP)
    public MyValueEmpty test46(EmptyContainer c1, MixedContainer c2, MyValueEmpty empty) {
        c2 = new MixedContainer(c2.val, c1);
        return c2.getNoInline().getNoInline();
    }

    @DontCompile
    public void test46_verifier(boolean warmup) {
        MyValueEmpty empty = test46(EmptyContainer.default, MixedContainer.default, MyValueEmpty.default);
        Asserts.assertEquals(empty, MyValueEmpty.default);
    }

    // No receiver and only empty argument
    @Test(failOn = ALLOC + LOAD + STORE + TRAP)
    public static MyValueEmpty test47(MyValueEmpty empty) {
        return empty;
    }

    @DontCompile
    public void test47_verifier(boolean warmup) {
        MyValueEmpty empty = test47(MyValueEmpty.default);
        Asserts.assertEquals(empty, MyValueEmpty.default);
    }

    // No receiver and only empty container argument
    @Test(failOn = ALLOC + LOAD + STORE + TRAP)
    public static MyValueEmpty test48(EmptyContainer empty) {
        return empty.getNoInline();
    }

    @DontCompile
    public void test48_verifier(boolean warmup) {
        MyValueEmpty empty = test48(EmptyContainer.default);
        Asserts.assertEquals(empty, MyValueEmpty.default);
    }

    // Test conditional inline type return with incremental inlining
    public MyValue3 test49_inlined1(boolean b) {
        if (b) {
            return MyValue3.create();
        } else {
            return MyValue3.create();
        }
    }

    public MyValue3 test49_inlined2(boolean b) {
        return test49_inlined1(b);
    }

    @Test
    public void test49(boolean b) {
        test49_inlined2(b);
    }

    @DontCompile
    public void test49_verifier(boolean warmup) {
        test49(true);
        test49(false);
    }

    // Variant of test49 with result verification (triggered different failure mode)
    final MyValue3 test50_vt = MyValue3.create();
    final MyValue3 test50_vt2 = test50_vt;

    public MyValue3 test50_inlined1(boolean b) {
        if (b) {
            return test50_vt;
        } else {
            return test50_vt2;
        }
    }

    public MyValue3 test50_inlined2(boolean b) {
        return test50_inlined1(b);
    }

    @Test
    public void test50(boolean b) {
        MyValue3 vt = test50_inlined2(b);
        test50_vt.verify(vt);
    }

    @DontCompile
    public void test50_verifier(boolean warmup) {
        test50(true);
        test50(false);
    }
}
