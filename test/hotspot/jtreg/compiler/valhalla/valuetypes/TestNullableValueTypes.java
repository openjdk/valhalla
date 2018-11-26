/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.experimental.value.MethodHandleBuilder;
import jdk.experimental.bytecode.MacroCodeBuilder;
import jdk.experimental.bytecode.MacroCodeBuilder.CondKind;
import jdk.experimental.bytecode.TypeTag;
import jdk.test.lib.Asserts;

/*
 * @test
 * @summary Test correct handling of nullable value types.
 * @modules java.base/jdk.experimental.bytecode
 *          java.base/jdk.experimental.value
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires os.simpleArch == "x64"
 * @compile -XDenableValueTypes -XDallowWithFieldOperator TestNullableValueTypes.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+EnableValhalla -XX:+NullableValueTypes
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
        case 3: return new String[] {"-XX:-MonomorphicArrayCheck", "-XX:-UseBiasedLocking", "-XX:+ValueArrayFlatten"};
        case 4: return new String[] {"-XX:-MonomorphicArrayCheck"};
        }
        return null;
    }

    public static void main(String[] args) throws Throwable {
        TestNullableValueTypes test = new TestNullableValueTypes();
        test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class, Test17Value.class);
    }

    static {
        try {
            Class<?> clazz = TestNullableValueTypes.class;
            ClassLoader loader = clazz.getClassLoader();
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodType test18_mt = MethodType.methodType(void.class, MyValue1.class);
            test18_mh1 = lookup.findStatic(clazz, "test18_target1", test18_mt);
            test18_mh2 = lookup.findStatic(clazz, "test18_target2", test18_mt);

            MethodType test19_mt = MethodType.methodType(void.class, MyValue1.class);
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

    MyValue1.box nullField;
    MyValue1.val valueField1 = testValue1;

    @Test
    @Warmup(10000) // Warmup to make sure 'callTest1WithNull' is compiled
    public long test1(MyValue1 vt) {
        long result = 0;
        try {
            result = vt.hash();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        return result;
    }

    private static final MethodHandle callTest1WithNull = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
        "callTest1WithNull",
        MethodType.methodType(void.class, TestNullableValueTypes.class),
        CODE -> {
            CODE.
            aload_0().
            aconst_null().
            invokevirtual(TestNullableValueTypes.class, "test1", "(Lcompiler/valhalla/valuetypes/MyValue1;)J", false).
            return_();
        });

    @DontCompile
    public void test1_verifier(boolean warmup) throws Throwable {
        long result = (long)callTest1WithNull.invoke(this);
        Asserts.assertEquals(result, 0L);
    }

    @Test
    public long test2(MyValue1 vt) {
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
            valueField1 = nullField;
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
    public MyValue1 test5(MyValue1 vt) {
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
        MyValue1 vt = test5(nullField);
        Asserts.assertEquals((Object)vt, null);
    }

    @DontInline
    public MyValue1 test5_dontinline(MyValue1 vt) {
        return vt;
    }

    @ForceInline
    public MyValue1 test5_inline(MyValue1 vt) {
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

    private static final MethodHandle getNull = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
        "getNull",
        MethodType.methodType(MyValue1.class),
        CODE -> {
            CODE.
            aconst_null().
            areturn();
        });

    @Test
    public void test7() throws Throwable {
        try {
            valueField1 = (MyValue1)getNull.invoke();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        nullField = (MyValue1)getNull.invoke(); // Should not throw
    }

    @DontCompile
    public void test7_verifier(boolean warmup) throws Throwable {
        test7();
    }

    // null constant
    private static final MethodHandle setNull = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
        "setNull",
        MethodType.methodType(void.class, TestNullableValueTypes.class),
        CODE -> {
            CODE.
            aload_0().
            aconst_null().
            putfield(TestNullableValueTypes.class, "valueField1", "Lcompiler/valhalla/valuetypes/MyValue1;").
            return_();
        });

    @Test
    public void test8() throws Throwable {
        try {
            setNull.invoke(this);
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
            v = nullField;
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
    private static final MethodHandle mergeNull = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
        "mergeNull",
        MethodType.methodType(void.class, TestNullableValueTypes.class, boolean.class),
        CODE -> {
            CODE.
            iload_1().
            iconst_0().
            ifcmp(TypeTag.I, CondKind.EQ, "null").
            aload_0().
            getfield(TestNullableValueTypes.class, "valueField1", "Lcompiler/valhalla/valuetypes/MyValue1;").
            goto_("continue").
            label("null").
            aconst_null().
            label("continue").
            aload_0().
            swap().
            putfield(TestNullableValueTypes.class, "valueField1", "Lcompiler/valhalla/valuetypes/MyValue1;").
            return_();
        });

    @Test
    public void test10(boolean flag) throws Throwable {
        mergeNull.invoke(this, flag);
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
    private static final MethodHandle mergeNull2 = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
        "mergeNull2",
        MethodType.methodType(void.class, TestNullableValueTypes.class, boolean.class),
        CODE -> {
            CODE.
            iload_1().
            iconst_0().
            ifcmp(TypeTag.I, CondKind.EQ, "not_null").
            aconst_null().
            goto_("continue").
            label("not_null").
            aload_0().
            getfield(TestNullableValueTypes.class, "valueField1", "Lcompiler/valhalla/valuetypes/MyValue1;").
            label("continue").
            aload_0().
            swap().
            putfield(TestNullableValueTypes.class, "valueField1", "Lcompiler/valhalla/valuetypes/MyValue1;").
            return_();
        });

    @Test
    public void test11(boolean flag) throws Throwable {
        mergeNull2.invoke(this, flag);
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
    public MyValue1 test12_helper() {
        test12_cnt++;
        return nullField;
    }

    @Test
    public void test12() {
        valueField1 = test12_helper();
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
        public MyValue1 test13_helper() {
            return nullField;
        }
    }

    class B extends A {
        public MyValue1 test13_helper() {
            return nullField;
        }
    }

    class C extends A {
        public MyValue1 test13_helper() {
            return nullField;
        }
    }

    class D extends A {
        public MyValue1 test13_helper() {
            return nullField;
        }
    }

    @Test
    public void test13(A a) {
        valueField1 = a.test13_helper();
    }

    @DontCompile
    public void test13_verifier(boolean warmup) {
        A b = new B();
        A c = new C();
        A d = new D();
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
// TODO Re-enable if value type arrays become covariant with object arrays
/*
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
*/

    @DontInline
    MyValue1 get_nullField() {
        return nullField;
    }

    @Test()
    public void test15() {
        try {
            valueField1 = get_nullField();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }

        nullField = get_nullField(); // should not throw
    }

    @DontCompile
    public void test15_verifier(boolean warmup) {
        test15();
    }

    @DontInline
    public boolean test16_dontinline(MyValue1 vt) {
        return (Object)vt == null;
    }

    // Test c2c call passing null for a value type
    @Test
    @Warmup(10000) // Warmup to make sure 'test17_dontinline' is compiled
    public boolean test16(Object arg) throws Exception {
        Method test16method = getClass().getMethod("test16_dontinline", MyValue1.class);
        return (boolean)test16method.invoke(this, arg);
    }

    @DontCompile
    public void test16_verifier(boolean warmup) throws Exception {
        boolean res = test16(null);
        Asserts.assertTrue(res);
    }

    // Test scalarization of default value type with non-flattenable field
    value final class Test17Value {
        public final MyValue1.box valueField;

        public Test17Value() {
            valueField = MyValue1.createDefaultDontInline();
        }

        @ForceInline
        public Test17Value setValueField(MyValue1 valueField) {
            return __WithField(this.valueField, valueField);
        }
    }

    @Test()
    public Test17Value test17(boolean b) {
        Test17Value vt1 = Test17Value.default;
        if ((Object)vt1.valueField != null) {
            throw new RuntimeException("Should be null");
        }
        Test17Value vt2 = vt1.setValueField(testValue1);
        return b ? vt1 : vt2;
    }

    @DontCompile
    public void test17_verifier(boolean warmup) {
        test17(true);
        test17(false);
    }

    static final MethodHandle test18_mh1;
    static final MethodHandle test18_mh2;

    static MyValue1.box nullValue;

    @DontInline
    static void test18_target1(MyValue1 vt) {
        nullValue = vt;
    }

    @ForceInline
    static void test18_target2(MyValue1 vt) {
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
    static void test19_target1(MyValue1 vt) {
        nullValue = vt;
    }

    @ForceInline
    static void test19_target2(MyValue1 vt) {
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
}
