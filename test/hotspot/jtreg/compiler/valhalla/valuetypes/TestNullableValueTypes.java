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

import java.lang.invoke.*;
import java.lang.reflect.Method;

import jdk.test.lib.Asserts;

/*
 * @test
 * @summary Test correct handling of nullable value types.
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires (os.simpleArch == "x64" | os.simpleArch == "aarch64")
 * @compile TestNullableValueTypes.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=300 -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                               compiler.valhalla.valuetypes.ValueTypeTest
 *                               compiler.valhalla.valuetypes.TestNullableValueTypes
 */
public class TestNullableValueTypes extends ValueTypeTest {
    // Extra VM parameters for some test scenarios. See ValueTypeTest.getVMParameters()
    @Override
    public String[] getExtraVMParameters(int scenario) {
        switch (scenario) {
        case 1: return new String[] {"-XX:-UseOptoBiasInlining"};
        case 2: return new String[] {"-XX:-UseBiasedLocking"};
        case 3: return new String[] {"-XX:-MonomorphicArrayCheck", "-XX:-UseBiasedLocking", "-XX:ValueArrayElemMaxFlatSize=-1"};
        case 4: return new String[] {"-XX:-MonomorphicArrayCheck"};
        }
        return null;
    }

    public static void main(String[] args) throws Throwable {
        TestNullableValueTypes test = new TestNullableValueTypes();
        test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class, Test17Value.class, Test21Value.class);
    }

    static {
        try {
            Class<?> clazz = TestNullableValueTypes.class;
            ClassLoader loader = clazz.getClassLoader();
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodType test18_mt = MethodType.methodType(void.class, MyValue1.class.asNullableType());
            test18_mh1 = lookup.findStatic(clazz, "test18_target1", test18_mt);
            test18_mh2 = lookup.findStatic(clazz, "test18_target2", test18_mt);

            MethodType test19_mt = MethodType.methodType(void.class, MyValue1.class.asNullableType());
            test19_mh1 = lookup.findStatic(clazz, "test19_target1", test19_mt);
            test19_mh2 = lookup.findStatic(clazz, "test19_target2", test19_mt);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("Method handle lookup failed");
        }
    }

    private static final MyValue1 testValue1 = MyValue1.createWithFieldsInline(rI, rL);
    private static final MyValue1[] testValue1Array = new MyValue1[] {testValue1,
                                                                      testValue1,
                                                                      testValue1};

    MyValue1? nullField;
    MyValue1 valueField1 = testValue1;

    @Test
    public long test1(MyValue1? vt) {
        long result = 0;
        try {
            result = vt.hash();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        return result;
    }

    @DontCompile
    public void test1_verifier(boolean warmup) throws Throwable {
        long result = test1(null);
        Asserts.assertEquals(result, 0L);
    }

    @Test
    public long test2(MyValue1? vt) {
        long result = 0;
        try {
            result = vt.hashInterpreted();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        return result;
    }

    @DontCompile
    public void test2_verifier(boolean warmup) {
        long result = test2(nullField);
        Asserts.assertEquals(result, 0L);
    }

    @Test
    public long test3() {
        long result = 0;
        try {
            if ((Object)nullField != null) {
                throw new RuntimeException("nullField should be null");
            }
            result = nullField.hash();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        return result;
    }

    @DontCompile
    public void test3_verifier(boolean warmup) {
        long result = test3();
        Asserts.assertEquals(result, 0L);
    }

    @Test
    public void test4() {
        try {
            valueField1 = (MyValue1) nullField;
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @DontCompile
    public void test4_verifier(boolean warmup) {
        test4();
    }

    @Test
    public MyValue1? test5(MyValue1? vt) {
        try {
            Object o = vt;
            vt = (MyValue1)o;
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }

        // Should not throw
        vt = test5_dontinline(vt);
        vt = test5_inline(vt);
        return vt;
    }

    @DontCompile
    public void test5_verifier(boolean warmup) {
        MyValue1? vt = test5(nullField);
        Asserts.assertEquals((Object)vt, null);
    }

    @DontInline
    public MyValue1? test5_dontinline(MyValue1? vt) {
        return vt;
    }

    @ForceInline
    public MyValue1? test5_inline(MyValue1? vt) {
        return vt;
    }

    @Test
    public MyValue1 test6(Object obj) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        try {
            vt = (MyValue1)obj;
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        return vt;
    }

    @DontCompile
    public void test6_verifier(boolean warmup) {
        MyValue1 vt = test6(null);
        Asserts.assertEquals(vt.hash(), testValue1.hash());
    }

    @ForceInline
    public MyValue1? getNullInline() {
        return null;
    }

    @DontInline
    public MyValue1? getNullDontInline() {
        return null;
    }

    @Test
    public void test7() throws Throwable {
        nullField = getNullInline();     // Should not throw
        nullField = getNullDontInline(); // Should not throw
        try {
            valueField1 = (MyValue1) getNullInline();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        try {
            valueField1 = (MyValue1) getNullDontInline();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @DontCompile
    public void test7_verifier(boolean warmup) throws Throwable {
        test7();
    }

    @Test
    public void test8() throws Throwable {
        try {
            valueField1 = (MyValue1) nullField;
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @DontCompile
    public void test8_verifier(boolean warmup) throws Throwable {
        test8();
    }

    // merge of 2 values, one being null
    @Test
    public void test9(boolean flag1) {
        MyValue1 v;
        if (flag1) {
            v = valueField1;
        } else {
            v = (MyValue1) nullField;
        }
        valueField1 = v;
    }

    @DontCompile
    public void test9_verifier(boolean warmup) {
        test9(true);
        try {
            test9(false);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    // null constant
    @Test
    public void test10(boolean flag) throws Throwable {
        MyValue1? val = flag ? valueField1 : null;
        valueField1 = (MyValue1) val;
    }

    @DontCompile
    public void test10_verifier(boolean warmup) throws Throwable {
        test10(true);
        try {
            test10(false);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    // null constant
    @Test
    public void test11(boolean flag) throws Throwable {
        MyValue1? val = flag ? null : valueField1;
        valueField1 = (MyValue1) val;
    }

    @DontCompile
    public void test11_verifier(boolean warmup) throws Throwable {
        test11(false);
        try {
            test11(true);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    // null return
    int test12_cnt;

    @DontInline
    public MyValue1? test12_helper() {
        test12_cnt++;
        return nullField;
    }

    @Test
    public void test12() {
        valueField1 = (MyValue1) test12_helper();
    }

    @DontCompile
    public void test12_verifier(boolean warmup) {
        try {
            test12_cnt = 0;
            test12();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        if (test12_cnt != 1) {
            throw new RuntimeException("call executed twice");
        }
    }

    // null return at virtual call
    class A {
        public MyValue1? test13_helper() {
            return nullField;
        }
    }

    class B extends A {
        public MyValue1 test13_helper() {
            return (MyValue1) nullField;
        }
    }

    class C extends A {
        public MyValue1? test13_helper() {
            return nullField;
        }
    }

    class D extends C {
        public MyValue1 test13_helper() {
            return (MyValue1) nullField;
        }
    }

    @Test
    public void test13(A a) {
        valueField1 = (MyValue1) a.test13_helper();
    }

    @DontCompile
    public void test13_verifier(boolean warmup) {
        A a = new A();
        A b = new B();
        A c = new C();
        A d = new D();
        try {
            test13(a);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        try {
            test13(b);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        try {
            test13(c);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        try {
            test13(d);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    // Test writing null to a (flattened) value type array
    @ForceInline
    public void test14_inline(Object[] oa, Object o, int index) {
        oa[index] = o;
    }

    @Test()
    public void test14(MyValue1[] va, int index) {
        test14_inline(va, nullField, index);
    }

    @DontCompile
    public void test14_verifier(boolean warmup) {
        int index = Math.abs(rI) % 3;
        try {
            test14(testValue1Array, index);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        Asserts.assertEQ(testValue1Array[index].hash(), testValue1.hash());
    }

    @DontInline
    MyValue1? getNullField1() {
        return nullField;
    }

    @DontInline
    MyValue1 getNullField2() {
        return (MyValue1) nullField;
    }

    @Test()
    public void test15() {
        nullField = getNullField1(); // should not throw
        try {
            valueField1 = (MyValue1) getNullField1();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        try {
            valueField1 = getNullField2();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @DontCompile
    public void test15_verifier(boolean warmup) {
        test15();
    }

    @DontInline
    public boolean test16_dontinline(MyValue1? vt) {
        return (Object)vt == null;
    }

    // Test c2c call passing null for a value type
    @Test
    @Warmup(10000) // Warmup to make sure 'test17_dontinline' is compiled
    public boolean test16(Object arg) throws Exception {
        Method test16method = getClass().getMethod("test16_dontinline", MyValue1.class.asNullableType());
        return (boolean)test16method.invoke(this, arg);
    }

    @DontCompile
    public void test16_verifier(boolean warmup) throws Exception {
        boolean res = test16(null);
        Asserts.assertTrue(res);
    }

    // Test scalarization of default value type with non-flattenable field
    final inline class Test17Value {
        public final MyValue1? valueField;

        @ForceInline
        public Test17Value(MyValue1? valueField) {
            this.valueField = valueField;
        }
    }

    @Test()
    public Test17Value test17(boolean b) {
        Test17Value vt1 = Test17Value.default;
        if ((Object)vt1.valueField != null) {
            throw new RuntimeException("Should be null");
        }
        Test17Value vt2 = new Test17Value(testValue1);
        return b ? vt1 : vt2;
    }

    @DontCompile
    public void test17_verifier(boolean warmup) {
        test17(true);
        test17(false);
    }

    static final MethodHandle test18_mh1;
    static final MethodHandle test18_mh2;

    static MyValue1? nullValue;

    @DontInline
    static void test18_target1(MyValue1? vt) {
        nullValue = vt;
    }

    @ForceInline
    static void test18_target2(MyValue1? vt) {
        nullValue = vt;
    }

    // Test passing null for a value type
    @Test
    @Warmup(11000) // Make sure lambda forms get compiled
    public void test18() throws Throwable {
        test18_mh1.invokeExact(nullValue);
        test18_mh2.invokeExact(nullValue);
    }

    @DontCompile
    public void test18_verifier(boolean warmup) {
        try {
            test18();
        } catch (Throwable t) {
            throw new RuntimeException("test18 failed", t);
        }
    }

    static MethodHandle test19_mh1;
    static MethodHandle test19_mh2;

    @DontInline
    static void test19_target1(MyValue1? vt) {
        nullValue = vt;
    }

    @ForceInline
    static void test19_target2(MyValue1? vt) {
        nullValue = vt;
    }

    // Same as test12 but with non-final mh
    @Test
    @Warmup(11000) // Make sure lambda forms get compiled
    public void test19() throws Throwable {
        test19_mh1.invokeExact(nullValue);
        test19_mh2.invokeExact(nullValue);
    }

    @DontCompile
    public void test19_verifier(boolean warmup) {
        try {
            test19();
        } catch (Throwable t) {
            throw new RuntimeException("test19 failed", t);
        }
    }

    // Same as test12/13 but with constant null
    @Test
    @Warmup(11000) // Make sure lambda forms get compiled
    public void test20(MethodHandle mh) throws Throwable {
        mh.invoke(null);
    }

    @DontCompile
    public void test20_verifier(boolean warmup) {
        try {
            test20(test18_mh1);
            test20(test18_mh2);
            test20(test19_mh1);
            test20(test19_mh2);
        } catch (Throwable t) {
            throw new RuntimeException("test20 failed", t);
        }
    }

    // Test writing null to a flattenable/non-flattenable value type field in a value type
    final inline class Test21Value {
        final MyValue1? valueField1;
        final MyValue1 valueField2;
        final MyValue1? alwaysNull = null;

        @ForceInline
        public Test21Value(MyValue1? valueField1, MyValue1 valueField2) {
            this.valueField1 = testValue1;
            this.valueField2 = testValue1;
        }

        @ForceInline
        public Test21Value test1() {
            return new Test21Value(alwaysNull, this.valueField2); // Should not throw NPE
        }

        @ForceInline
        public Test21Value test2() {
            return new Test21Value(this.valueField1, (MyValue1) alwaysNull); // Should throw NPE
        }
    }

    @Test
    public Test21Value test21(Test21Value vt) {
        vt = vt.test1();
        try {
            vt = vt.test2();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        return vt;
    }

    @DontCompile
    public void test21_verifier(boolean warmup) {
        test21(Test21Value.default);
    }

    @DontInline
    public MyValue1 test22_helper() {
        return (MyValue1) nullField;
    }

    @Test
    public void test22() {
        valueField1 = test22_helper();
    }

    @DontCompile
    public void test22_verifier(boolean warmup) {
        try {
            test22();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public void test23(MyValue1[] arr, MyValue1? b) {
        arr[0] = (MyValue1) b;
    }

    @DontCompile
    public void test23_verifier(boolean warmup) {
        MyValue1[] arr = new MyValue1[2];
        MyValue1? b = null;
        try {
            test23(arr, b);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    static MyValue1? nullBox;

    @Test
    public MyValue1 test24() {
        return (MyValue1) nullBox;
    }

    @DontCompile
    public void test24_verifier(boolean warmup) {
        try {
            test24();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @DontInline
    public void test25_callee(MyValue1 val) { }

    // Test that when checkcasting from null-ok to null-free and back to null-ok we
    // keep track of the information that the value can never be null.
    @Test(failOn = ALLOC + STORE)
    public int test25(boolean b, MyValue1? vt1, MyValue1 vt2) {
        vt1 = (MyValue1)vt1;
        Object obj = b ? vt1 : vt2; // We should not allocate here
        test25_callee((MyValue1) vt1);
        return ((MyValue1)obj).x;
    }

    @DontCompile
    public void test25_verifier(boolean warmup) {
        int res = test25(true, testValue1, testValue1);
        Asserts.assertEquals(res, testValue1.x);
        res = test25(false, testValue1, testValue1);
        Asserts.assertEquals(res, testValue1.x);
    }

    // Test that chains of casts are folded and don't trigger an allocation
    @Test(failOn = ALLOC + STORE)
    public MyValue3 test26(MyValue3 vt) {
        return ((MyValue3)((Object)((MyValue3?)(MyValue3)((MyValue3?)((Object)vt)))));
    }

    @DontCompile
    public void test26_verifier(boolean warmup) {
        MyValue3 vt = MyValue3.create();
        MyValue3 result = test26(vt);
        Asserts.assertEquals(result, vt);
    }

    @Test(failOn = ALLOC + STORE)
    public MyValue3? test27(MyValue3? vt) {
        return ((MyValue3?)((Object)((MyValue3)(MyValue3?)((MyValue3)((Object)vt)))));
    }

    @DontCompile
    public void test27_verifier(boolean warmup) {
        MyValue3 vt = MyValue3.create();
        MyValue3 result = (MyValue3) test27(vt);
        Asserts.assertEquals(result, vt);
    }

    // Some more casting tests
    @Test()
    public MyValue1? test28(MyValue1 vt, MyValue1? vtBox, int i) {
        MyValue1? result = null;
        if (i == 0) {
            result = (MyValue1?)vt;
            result = null;
        } else if (i == 1) {
            result = (MyValue1?)vt;
        } else if (i == 2) {
            result = vtBox;
        }
        return result;
    }

    @DontCompile
    public void test28_verifier(boolean warmup) {
        MyValue1? result = test28(testValue1, null, 0);
        Asserts.assertEquals(result, null);
        result = test28(testValue1, testValue1, 1);
        Asserts.assertEquals(result, testValue1);
        result = test28(testValue1, null, 2);
        Asserts.assertEquals(result, null);
        result = test28(testValue1, testValue1, 2);
        Asserts.assertEquals(result, testValue1);
    }

    @Test()
    public long test29(MyValue1 vt, MyValue1? vtBox) {
        long result = 0;
        for (int i = 0; i < 100; ++i) {
            MyValue1? box;
            if (i == 0) {
                box = (MyValue1?)vt;
                box = null;
            } else if (i < 99) {
                box = (MyValue1?)vt;
            } else {
                box = vtBox;
            }
            if (box != null) {
                result += box.hash();
            }
        }
        return result;
    }

    @DontCompile
    public void test29_verifier(boolean warmup) {
        long result = test29(testValue1, null);
        Asserts.assertEquals(result, testValue1.hash()*98);
        result = test29(testValue1, testValue1);
        Asserts.assertEquals(result, testValue1.hash()*99);
    }

    // Test null check of value type receiver with incremental inlining
    public long test30_callee(MyValue1? vt) {
        long result = 0;
        try {
            result = vt.hashInterpreted();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        return result;
    }

    @Test
    public long test30() {
        return test30_callee(nullField);
    }

    @DontCompile
    public void test30_verifier(boolean warmup) {
        long result = test30();
        Asserts.assertEquals(result, 0L);
    }

    // Test casting null to unloaded value type
    final inline class Test31Value {
        private final int i = 0;
    }

    @Test
    public void test31(Object o) {
        try {
            o = (Test31Value)o;
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @DontCompile
    public void test31_verifier(boolean warmup) {
        test31(null);
    }

    private static final MyValue1? constNullField = null;

    @Test
    public MyValue1? test32() {
        return constNullField;
    }

    @DontCompile
    public void test32_verifier(boolean warmup) {
        MyValue1? result = test32();
        Asserts.assertEquals(result, null);
    }

    static inline class Test33Value1 {
        int x = 0;
    }

    static inline class Test33Value2 {
        Test33Value1? vt;

        public Test33Value2() {
            vt = new Test33Value1();
        }
    }

    public static final Test33Value2 test33Val = new Test33Value2();

    @Test
    public Test33Value2 test33() {
        return test33Val;
    }

    @DontCompile
    public void test33_verifier(boolean warmup) {
        Test33Value2 result = test33();
        Asserts.assertEquals(result, test33Val);
    }
}
