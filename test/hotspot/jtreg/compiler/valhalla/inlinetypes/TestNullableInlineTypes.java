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
import jdk.experimental.bytecode.TypeTag;
import test.java.lang.invoke.lib.InstructionHelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

import static compiler.valhalla.inlinetypes.InlineTypes.IRNode.*;
import static compiler.valhalla.inlinetypes.InlineTypes.*;

/*
 * @test
 * @key randomness
 * @summary Test correct handling of nullable inline types.
 * @library /test/lib /test/jdk/lib/testlibrary/bytecode /test/jdk/java/lang/invoke/common /
 * @requires (os.simpleArch == "x64" | os.simpleArch == "aarch64")
 * @build test.java.lang.invoke.lib.InstructionHelper
 * @run driver/timeout=300 compiler.valhalla.inlinetypes.TestNullableInlineTypes
 */

@ForceCompileClassInitializer
public class TestNullableInlineTypes {

    public static void main(String[] args) {

        Scenario[] scenarios = InlineTypes.DEFAULT_SCENARIOS;
        scenarios[3].addFlags("-XX:-MonomorphicArrayCheck", "-XX:FlatArrayElementMaxSize=-1");
        scenarios[4].addFlags("-XX:-MonomorphicArrayCheck");

        InlineTypes.getFramework()
                   .addScenarios(scenarios)
                   .addHelperClasses(MyValue1.class,
                                     MyValue2.class,
                                     MyValue2Inline.class,
                                     MyValue3.class,
                                     MyValue3Inline.class)
                   .start();
    }

    static {
        // Make sure RuntimeException is loaded to prevent uncommon traps in IR verified tests
        RuntimeException tmp = new RuntimeException("42");
        try {
            Class<?> clazz = TestNullableInlineTypes.class;
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodType test18_mt = MethodType.methodType(void.class, MyValue1.ref.class);
            test18_mh1 = lookup.findStatic(clazz, "test18_target1", test18_mt);
            test18_mh2 = lookup.findStatic(clazz, "test18_target2", test18_mt);

            MethodType test19_mt = MethodType.methodType(void.class, MyValue1.ref.class);
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

    MyValue1.ref nullField;
    MyValue1 valueField1 = testValue1;

    @Test
    @IR(failOn = {ALLOC})
    public long test1(MyValue1.ref vt) {
        long result = 0;
        try {
            result = vt.hash();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        return result;
    }

    @Run(test = "test1")
    public void test1_verifier() {
        long result = test1(null);
        Asserts.assertEquals(result, 0L);
    }

    @Test
    @IR(failOn = {ALLOC})
    public long test2(MyValue1.ref vt) {
        long result = 0;
        try {
            result = vt.hashInterpreted();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        return result;
    }

    @Run(test = "test2")
    public void test2_verifier() {
        long result = test2(nullField);
        Asserts.assertEquals(result, 0L);
    }

    @Test
    @IR(failOn = {ALLOC})
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

    @Run(test = "test3")
    public void test3_verifier() {
        long result = test3();
        Asserts.assertEquals(result, 0L);
    }

    @Test
    @IR(failOn = {ALLOC})
    public void test4() {
        try {
            valueField1 = (MyValue1) nullField;
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Run(test = "test4")
    public void test4_verifier() {
        test4();
    }

    @Test
    // TODO 8284443 When passing vt to test5_inline and incrementally inlining, we lose the oop
    @IR(applyIfOr = {"InlineTypePassFieldsAsArgs", "false", "AlwaysIncrementalInline", "false"},
        failOn = {ALLOC})
    public MyValue1.ref test5(MyValue1.ref vt) {
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

    @Run(test = "test5")
    public void test5_verifier() {
        MyValue1 val = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1.ref vt = test5(nullField);
        Asserts.assertEquals(vt, null);
    }

    @DontInline
    public MyValue1.ref test5_dontinline(MyValue1.ref vt) {
        return vt;
    }

    @ForceInline
    public MyValue1.ref test5_inline(MyValue1.ref vt) {
        return vt;
    }

    @Test
    @IR(failOn = {ALLOC})
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

    @Run(test = "test6")
    public void test6_verifier() {
        MyValue1 vt = test6(null);
        Asserts.assertEquals(vt.hash(), testValue1.hash());
    }

    @ForceInline
    public MyValue1.ref getNullInline() {
        return null;
    }

    @DontInline
    public MyValue1.ref getNullDontInline() {
        return null;
    }

    @Test
    @IR(failOn = {ALLOC})
    public void test7() {
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

    @Run(test = "test7")
    public void test7_verifier() {
        test7();
    }

    @Test
    @IR(failOn = {ALLOC})
    public void test8() {
        try {
            valueField1 = (MyValue1) nullField;
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Run(test = "test8")
    public void test8_verifier() {
        test8();
    }

    // merge of 2 inline types, one being null
    @Test
    @IR(failOn = {ALLOC})
    public void test9(boolean flag) {
        MyValue1 v;
        if (flag) {
            v = valueField1;
        } else {
            v = (MyValue1) nullField;
        }
        valueField1 = v;
    }

    @Run(test = "test9")
    public void test9_verifier() {
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
    @IR(failOn = {ALLOC})
    public void test10(boolean flag) {
        MyValue1.ref val = flag ? valueField1 : null;
        valueField1 = (MyValue1) val;
    }

    @Run(test = "test10")
    public void test10_verifier() {
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
    @IR(failOn = {ALLOC})
    public void test11(boolean flag) {
        MyValue1.ref val = flag ? null : valueField1;
        valueField1 = (MyValue1) val;
    }

    @Run(test = "test11")
    public void test11_verifier() {
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
    public MyValue1.ref test12_helper() {
        test12_cnt++;
        return nullField;
    }

    @Test
    @IR(failOn = {ALLOC})
    public void test12() {
        valueField1 = (MyValue1) test12_helper();
    }

    @Run(test = "test12")
    public void test12_verifier() {
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
        public MyValue1.ref test13_helper() {
            return nullField;
        }
    }

    class B extends A {
        public MyValue1 test13_helper() {
            return (MyValue1) nullField;
        }
    }

    class C extends A {
        public MyValue1.ref test13_helper() {
            return nullField;
        }
    }

    class D extends C {
        public MyValue1 test13_helper() {
            return (MyValue1) nullField;
        }
    }

    @Test
    @IR(failOn = {ALLOC})
    public void test13(A a) {
        valueField1 = (MyValue1) a.test13_helper();
    }

    @Run(test = "test13")
    public void test13_verifier() {
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

    // Test writing null to a (flattened) inline type array
    @ForceInline
    public void test14_inline(Object[] oa, Object o, int index) {
        oa[index] = o;
    }

    @Test
    @IR(failOn = {ALLOC})
    public void test14(MyValue1[] va, int index) {
        test14_inline(va, nullField, index);
    }

    @Run(test = "test14")
    public void test14_verifier() {
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
    MyValue1.ref getNullField1() {
        return nullField;
    }

    @DontInline
    MyValue1 getNullField2() {
        return (MyValue1) nullField;
    }

    @Test
    @IR(failOn = {ALLOC})
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

    @Run(test = "test15")
    public void test15_verifier() {
        test15();
    }

    @DontInline
    public boolean test16_dontinline(MyValue1.ref vt) {
        return vt == null;
    }

    // Test c2c call passing null for an inline type
    @Test
    @IR(failOn = {ALLOC})
    public boolean test16(Object arg) throws Exception {
        Method test16method = getClass().getMethod("test16_dontinline", MyValue1.ref.class);
        return (boolean)test16method.invoke(this, arg);
    }

    @Run(test = "test16")
    @Warmup(10000) // Warmup to make sure 'test17_dontinline' is compiled
    public void test16_verifier() throws Exception {
        boolean res = test16(null);
        Asserts.assertTrue(res);
    }

    // Test scalarization of default inline type with non-flattenable field
    final primitive class Test17Value {
        public final MyValue1.ref valueField;

        @ForceInline
        public Test17Value(MyValue1.ref valueField) {
            this.valueField = valueField;
        }
    }

    @Test
    // TODO 8284443 When passing testValue1 to the constructor in scalarized form and incrementally inlining, we lose the oop
    @IR(applyIfOr = {"InlineTypePassFieldsAsArgs", "false", "AlwaysIncrementalInline", "false"},
        failOn = {ALLOC})
    public Test17Value test17(boolean b) {
        Test17Value vt1 = Test17Value.default;
        Test17Value vt2 = new Test17Value(testValue1);
        return b ? vt1 : vt2;
    }

    @Run(test = "test17")
    public void test17_verifier() {
        test17(true);
        test17(false);
    }

    static final MethodHandle test18_mh1;
    static final MethodHandle test18_mh2;

    static MyValue1.ref nullValue;

    @DontInline
    static void test18_target1(MyValue1.ref vt) {
        nullValue = vt;
    }

    @ForceInline
    static void test18_target2(MyValue1.ref vt) {
        nullValue = vt;
    }

    // Test passing null for an inline type
    @Test
    @IR(failOn = {ALLOC})
    public void test18() throws Throwable {
        test18_mh1.invokeExact(nullValue);
        test18_mh2.invokeExact(nullValue);
    }

    @Run(test = "test18")
    @Warmup(11000) // Make sure lambda forms get compiled
    public void test18_verifier() {
        try {
            test18();
        } catch (Throwable t) {
            throw new RuntimeException("test18 failed", t);
        }
    }

    static MethodHandle test19_mh1;
    static MethodHandle test19_mh2;

    @DontInline
    static void test19_target1(MyValue1.ref vt) {
        nullValue = vt;
    }

    @ForceInline
    static void test19_target2(MyValue1.ref vt) {
        nullValue = vt;
    }

    // Same as test12 but with non-final mh
    @Test
    @IR(failOn = {ALLOC})
    public void test19() throws Throwable {
        test19_mh1.invokeExact(nullValue);
        test19_mh2.invokeExact(nullValue);
    }

    @Run(test = "test19")
    @Warmup(11000) // Make sure lambda forms get compiled
    public void test19_verifier() {
        try {
            test19();
        } catch (Throwable t) {
            throw new RuntimeException("test19 failed", t);
        }
    }

    // Same as test12/13 but with constant null
    @Test
    @IR(failOn = {ALLOC})
    public void test20(MethodHandle mh) throws Throwable {
        mh.invoke(null);
    }

    @Run(test = "test20")
    @Warmup(11000) // Make sure lambda forms get compiled
    public void test20_verifier() {
        try {
            test20(test18_mh1);
            test20(test18_mh2);
            test20(test19_mh1);
            test20(test19_mh2);
        } catch (Throwable t) {
            throw new RuntimeException("test20 failed", t);
        }
    }

    // Test writing null to a flattenable/non-flattenable inline type field in an inline type
    final primitive class Test21Value {
        final MyValue1.ref valueField1;
        final MyValue1 valueField2;
        final MyValue1.ref alwaysNull = null;

        @ForceInline
        public Test21Value(MyValue1.ref valueField1, MyValue1 valueField2) {
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
    @IR(failOn = {ALLOC})
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

    @Run(test = "test21")
    public void test21_verifier() {
        test21(Test21Value.default);
    }

    @DontInline
    public MyValue1 test22_helper() {
        return (MyValue1) nullField;
    }

    @Test
    @IR(failOn = {ALLOC})
    public void test22() {
        valueField1 = test22_helper();
    }

    @Run(test = "test22")
    public void test22_verifier() {
        try {
            test22();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    @IR(applyIfAnd = {"FlatArrayElementMaxSize", "= -1", "InlineTypePassFieldsAsArgs", "true"},
        failOn = {ALLOC})
    @IR(applyIfAnd = {"FlatArrayElementMaxSize", "= 0", "InlineTypePassFieldsAsArgs", "false"},
        failOn = {ALLOC})
    public void test23(MyValue1[] arr, MyValue1.ref b) {
        arr[0] = (MyValue1) b;
    }

    @Run(test = "test23")
    public void test23_verifier() {
        MyValue1[] arr = new MyValue1[2];
        MyValue1.ref b = null;
        try {
            test23(arr, b);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    static MyValue1.ref nullBox;

    @Test
    @IR(failOn = {ALLOC})
    public MyValue1 test24() {
        return (MyValue1) nullBox;
    }

    @Run(test = "test24")
    public void test24_verifier() {
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
    // keep track of the information that the inline type can never be null.
    @Test
    @IR(failOn = {ALLOC, STORE})
    public int test25(boolean b, MyValue1.ref vt1, MyValue1 vt2) {
        vt1 = (MyValue1)vt1;
        Object obj = b ? vt1 : vt2; // We should not allocate here
        test25_callee((MyValue1) vt1);
        return ((MyValue1)obj).x;
    }

    @Run(test = "test25")
    public void test25_verifier(RunInfo info) {
        int res = test25(true, testValue1, testValue1);
        Asserts.assertEquals(res, testValue1.x);
        res = test25(false, testValue1, testValue1);
        Asserts.assertEquals(res, testValue1.x);
        if (!info.isWarmUp()) {
            try {
                test25(false, null, testValue1);
                throw new RuntimeException("NullPointerException expected");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    // Test that chains of casts are folded and don't trigger an allocation
    @Test
    @IR(failOn = {ALLOC, STORE})
    public MyValue3 test26(MyValue3 vt) {
        return ((MyValue3)((Object)((MyValue3.ref)(MyValue3)((MyValue3.ref)((Object)vt)))));
    }

    @Run(test = "test26")
    public void test26_verifier() {
        MyValue3 vt = MyValue3.create();
        MyValue3 result = test26(vt);
        Asserts.assertEquals(result, vt);
    }

    @Test
    @IR(failOn = {ALLOC, STORE})
    public MyValue3.ref test27(MyValue3.ref vt) {
        return ((MyValue3.ref)((Object)((MyValue3)(MyValue3.ref)((MyValue3)((Object)vt)))));
    }

    @Run(test = "test27")
    public void test27_verifier() {
        MyValue3 vt = MyValue3.create();
        MyValue3 result = (MyValue3) test27(vt);
        Asserts.assertEquals(result, vt);
    }

    // Some more casting tests
    @Test
    public MyValue1.ref test28(MyValue1 vt, MyValue1.ref vtBox, int i) {
        MyValue1.ref result = null;
        if (i == 0) {
            result = (MyValue1.ref)vt;
            result = null;
        } else if (i == 1) {
            result = (MyValue1.ref)vt;
        } else if (i == 2) {
            result = vtBox;
        }
        return result;
    }

    @Run(test = "test28")
    public void test28_verifier() {
        MyValue1.ref result = test28(testValue1, null, 0);
        Asserts.assertEquals(result, null);
        result = test28(testValue1, testValue1, 1);
        Asserts.assertEquals(result, testValue1);
        result = test28(testValue1, null, 2);
        Asserts.assertEquals(result, null);
        result = test28(testValue1, testValue1, 2);
        Asserts.assertEquals(result, testValue1);
    }

    @Test
    @IR(failOn = {ALLOC})
    public long test29(MyValue1 vt, MyValue1.ref vtBox) {
        long result = 0;
        for (int i = 0; i < 100; ++i) {
            MyValue1.ref box;
            if (i == 0) {
                box = (MyValue1.ref)vt;
                box = null;
            } else if (i < 99) {
                box = (MyValue1.ref)vt;
            } else {
                box = vtBox;
            }
            if (box != null) {
                result += box.hash();
            }
        }
        return result;
    }

    @Run(test = "test29")
    public void test29_verifier() {
        long result = test29(testValue1, null);
        Asserts.assertEquals(result, testValue1.hash()*98);
        result = test29(testValue1, testValue1);
        Asserts.assertEquals(result, testValue1.hash()*99);
    }

    // Test null check of inline type receiver with incremental inlining
    public long test30_callee(MyValue1.ref vt) {
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
    @IR(failOn = {ALLOC})
    public long test30() {
        return test30_callee(nullField);
    }

    @Run(test = "test30")
    public void test30_verifier() {
        long result = test30();
        Asserts.assertEquals(result, 0L);
    }

    // Test casting null to unloaded inline type
    final primitive class Test31Value {
        private final int i = 0;
    }

    @Test
    @IR(failOn = {ALLOC})
    public void test31(Object o) {
        try {
            o = (Test31Value)o;
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Run(test = "test31")
    public void test31_verifier() {
        test31(null);
    }

    private static final MyValue1.ref constNullRefField = null;

    @Test
    @IR(failOn = {ALLOC})
    public MyValue1.ref test32() {
        return constNullRefField;
    }

    @Run(test = "test32")
    public void test32_verifier() {
        MyValue1.ref result = test32();
        Asserts.assertEquals(result, null);
    }

    static primitive class Test33Value1 {
        int x = 0;
    }

    static primitive class Test33Value2 {
        Test33Value1.ref vt;

        public Test33Value2() {
            vt = new Test33Value1();
        }
    }

    public static final Test33Value2 test33Val = new Test33Value2();

    @Test
    @IR(failOn = {ALLOC})
    public Test33Value2 test33() {
        return test33Val;
    }

    @Run(test = "test33")
    public void test33_verifier() {
        Test33Value2 result = test33();
        Asserts.assertEquals(result, test33Val);
    }

    // Verify that static nullable inline-type fields are not
    // treated as never-null by C2 when initialized at compile time.
    private static MyValue1.ref test34Val;

    @Test
    public void test34(MyValue1 vt) {
        if (test34Val == null) {
            test34Val = vt;
        }
    }

    @Run(test = "test34")
    public void test34_verifier(RunInfo info) {
        test34(testValue1);
        if (!info.isWarmUp()) {
            test34Val = null;
            test34(testValue1);
            Asserts.assertEquals(test34Val, testValue1);
        }
    }

    // Same as test17 but with non-allocated inline type at withfield
    @Test
    public Test17Value test35(boolean b) {
        Test17Value vt1 = Test17Value.default;
        if ((Object)vt1.valueField != null) {
            throw new RuntimeException("Should be null");
        }
        MyValue1 vt3 = MyValue1.createWithFieldsInline(rI, rL);
        Test17Value vt2 = new Test17Value(vt3);
        return b ? vt1 : vt2;
    }

    @Run(test = "test35")
    public void test35_verifier() {
        test35(true);
        test35(false);
    }

    // Test that when explicitly null checking an inline type, we keep
    // track of the information that the inline type can never be null.
    @Test
    @IR(failOn = {ALLOC, STORE})
    public int test37(boolean b, MyValue1.ref vt1, MyValue1.val vt2) {
        if (vt1 == null) {
            return 0;
        }
        // vt1 should be scalarized because it's always non-null
        Object obj = b ? vt1 : vt2; // We should not allocate vt2 here
        test25_callee(vt1);
        return ((MyValue1)obj).x;
    }

    @Run(test = "test37")
    public void test37_verifier() {
        int res = test37(true, testValue1, testValue1);
        Asserts.assertEquals(res, testValue1.x);
        res = test37(false, testValue1, testValue1);
        Asserts.assertEquals(res, testValue1.x);
    }

    // Test that when explicitly null checking an inline type receiver,
    // we keep track of the information that the inline type can never be null.
    @Test
    @IR(failOn = {ALLOC, STORE})
    public int test38(boolean b, MyValue1.ref vt1, MyValue1.val vt2) {
        vt1.hash(); // Inlined - Explicit null check
        // vt1 should be scalarized because it's always non-null
        Object obj = b ? vt1 : vt2; // We should not allocate vt2 here
        test25_callee(vt1);
        return ((MyValue1)obj).x;
    }

    @Run(test = "test38")
    public void test38_verifier() {
        int res = test38(true, testValue1, testValue1);
        Asserts.assertEquals(res, testValue1.x);
        res = test38(false, testValue1, testValue1);
        Asserts.assertEquals(res, testValue1.x);
    }

    // Test that when implicitly null checking an inline type receiver,
    // we keep track of the information that the inline type can never be null.
    @Test
    @IR(failOn = {ALLOC, STORE})
    public int test39(boolean b, MyValue1.ref vt1, MyValue1.val vt2) {
        vt1.hashInterpreted(); // Not inlined - Implicit null check
        // vt1 should be scalarized because it's always non-null
        Object obj = b ? vt1 : vt2; // We should not allocate vt2 here
        test25_callee(vt1);
        return ((MyValue1)obj).x;
    }

    @Run(test = "test39")
    public void test39_verifier() {
        int res = test39(true, testValue1, testValue1);
        Asserts.assertEquals(res, testValue1.x);
        res = test39(false, testValue1, testValue1);
        Asserts.assertEquals(res, testValue1.x);
    }

    // Test NPE when casting constant null to inline type
    @Test
    @IR(failOn = {ALLOC})
    public MyValue1 test40() {
        Object NULL = null;
        return (MyValue1)NULL;
    }

    @Run(test = "test40")
    public void test40_verifier() {
        try {
            test40();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    MyValue1.ref refField;
    MyValue1 flatField;

    // Test scalarization of .ref
    @Test
    @IR(failOn = {ALLOC_G, STORE, TRAP})
    public int test41(boolean b) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        if (b) {
            val = refField;
        }
        return val.x;
    }

    @Run(test = "test41")
    public void test41_verifier(RunInfo info) {
        refField = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test41(true), refField.x);
        Asserts.assertEquals(test41(false), testValue1.x);
        if (!info.isWarmUp()) {
            refField = null;
            try {
                Asserts.assertEquals(test41(false), testValue1.x);
                test41(true);
                throw new RuntimeException("NullPointerException expected");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    // Same as test41 but with call to hash()
    @Test
    @IR(failOn = {ALLOC, STORE, TRAP})
    public long test42(boolean b) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        if (b) {
            val = refField;
        }
        return val.hash();
    }

    @Run(test = "test42")
    public void test42_verifier(RunInfo info) {
        refField = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test42(true), refField.hash());
        Asserts.assertEquals(test42(false), testValue1.hash());
        if (!info.isWarmUp()) {
            refField = null;
            try {
                Asserts.assertEquals(test42(false), testValue1.hash());
                test42(true);
                throw new RuntimeException("NullPointerException expected");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    @Test
    public MyValue1.ref test43(boolean b) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        if (b) {
            val = refField;
        }
        return val;
    }

    @Run(test = "test43")
    public void test43_verifier(RunInfo info) {
        refField = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test43(true).hash(), refField.hash());
        Asserts.assertEquals(test43(false).hash(), testValue1.hash());
        if (!info.isWarmUp()) {
            refField = null;
            Asserts.assertEquals(test43(true), null);
        }
    }

    // Test scalarization when .ref is referenced in safepoint debug info
    @Test
    @IR(failOn = {ALLOC, STORE})
    public int test44(boolean b1, boolean b2, Method m) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        if (b1) {
            val = refField;
        }
        if (b2) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        return val.x;
    }

    @Run(test = "test44")
    public void test44_verifier(RunInfo info) {
        refField = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test44(true, false, info.getTest()), refField.x);
        Asserts.assertEquals(test44(false, false, info.getTest()), testValue1.x);
        if (!info.isWarmUp()) {
            refField = null;
            try {
                Asserts.assertEquals(test44(false, false, info.getTest()), testValue1.x);
                test44(true, false, info.getTest());
                throw new RuntimeException("NullPointerException expected");
            } catch (NullPointerException e) {
                // Expected
            }
            refField = MyValue1.createWithFieldsInline(rI+1, rL+1);
            Asserts.assertEquals(test44(true, true, info.getTest()), refField.x);
            Asserts.assertEquals(test44(false, true, info.getTest()), testValue1.x);
        }
    }

    @Test
    public MyValue1.ref test45(boolean b1, boolean b2, Method m) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        if (b1) {
            val = refField;
        }
        if (b2) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        return val;
    }

    @Run(test = "test45")
    public void test45_verifier(RunInfo info) {
        refField = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test45(true, false, info.getTest()).hash(), refField.hash());
        Asserts.assertEquals(test45(false, false, info.getTest()).hash(), testValue1.hash());
        if (!info.isWarmUp()) {
            refField = null;
            Asserts.assertEquals(test45(true, false, info.getTest()), null);
            refField = MyValue1.createWithFieldsInline(rI+1, rL+1);
            Asserts.assertEquals(test45(true, true, info.getTest()).hash(), refField.hash());
            Asserts.assertEquals(test45(false, true, info.getTest()).hash(), testValue1.hash());
        }
    }

    @Test
    @IR(failOn = {ALLOC_G, LOAD, STORE, TRAP})
    public int test46(boolean b) {
        MyValue1.ref val = null;
        if (b) {
            val = MyValue1.createWithFieldsInline(rI, rL);
        }
        return val.x;
    }

    @Run(test = "test46")
    public void test46_verifier() {
        Asserts.assertEquals(test46(true), testValue1.x);
        try {
            test46(false);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public MyValue1.ref test47(boolean b) {
        MyValue1.ref val = null;
        if (b) {
            val = MyValue1.createWithFieldsInline(rI, rL);
        }
        return val;
    }

    @Run(test = "test47")
    public void test47_verifier() {
        Asserts.assertEquals(test47(true).hash(), testValue1.hash());
        Asserts.assertEquals(test47(false), null);
    }

    @Test
    @IR(failOn = {ALLOC_G, LOAD, STORE, TRAP})
    public int test48(boolean b) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        if (b) {
            val = null;
        }
        return val.x;
    }

    @Run(test = "test48")
    public void test48_verifier() {
        Asserts.assertEquals(test48(false), testValue1.x);
        try {
            test48(true);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public MyValue1.ref test49(boolean b) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        if (b) {
            val = null;
        }
        return val;
    }

    @Run(test = "test49")
    public void test49_verifier() {
        Asserts.assertEquals(test49(false).hash(), testValue1.hash());
        Asserts.assertEquals(test49(true), null);
    }

    @ForceInline
    public Object test50_helper() {
        return flatField;
    }

    @Test
    @IR(failOn = {ALLOC_G, TRAP})
    public void test50(boolean b) {
        Object o = null;
        if (b) {
            o = testValue1;
        } else {
            o = test50_helper();
        }
        flatField = (MyValue1)o;
    }

    @Run(test = "test50")
    public void test50_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI+1, rL+1);
        flatField = vt;
        test50(false);
        Asserts.assertEquals(flatField.hash(), vt.hash());
        test50(true);
        Asserts.assertEquals(flatField.hash(), testValue1.hash());
    }

    static final primitive class MyValue1Wrapper {
        final MyValue1.ref vt;

        @ForceInline
        public MyValue1Wrapper(MyValue1.ref vt) {
            this.vt = vt;
        }

        @ForceInline
        public long hash() {
            return (vt != null) ? vt.hash() : 0;
        }
    }

    MyValue1Wrapper wrapperField;

    @Test
    @IR(failOn = {ALLOC_G, STORE, TRAP})
    public long test51(boolean b) {
        MyValue1Wrapper.ref val = MyValue1Wrapper.default;
        if (b) {
            val = wrapperField;
        }
        return val.hash();
    }

    @Run(test = "test51")
    public void test51_verifier() {
        wrapperField = new MyValue1Wrapper(testValue1);
        Asserts.assertEquals(test51(true), wrapperField.hash());
        Asserts.assertEquals(test51(false), MyValue1Wrapper.default.hash());
    }

    @Test
    @IR(failOn = {ALLOC_G, LOAD, STORE, TRAP})
    public boolean test52(boolean b) {
        MyValue1.ref val = MyValue1.default;
        if (b) {
            val = null;
        }
        MyValue1Wrapper.ref w = new MyValue1Wrapper(val);
        return w.vt == null;
    }

    @Run(test = "test52")
    public void test52_verifier() {
        Asserts.assertTrue(test52(true));
        Asserts.assertFalse(test52(false));
    }

    @Test
    @IR(failOn = {ALLOC_G, LOAD, STORE, TRAP})
    public boolean test53(boolean b) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        if (b) {
            val = null;
        }
        MyValue1Wrapper.ref w = new MyValue1Wrapper(val);
        return w.vt == null;
    }

    @Run(test = "test53")
    public void test53_verifier() {
        Asserts.assertTrue(test53(true));
        Asserts.assertFalse(test53(false));
    }

    @Test
    @IR(failOn = {ALLOC, LOAD, STORE, TRAP})
    public long test54(boolean b1, boolean b2) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        if (b1) {
            val = null;
        }
        MyValue1Wrapper.ref w = MyValue1Wrapper.default;
        if (b2) {
            w = new MyValue1Wrapper(val);
        }
        return w.hash();
    }

    @Run(test = "test54")
    public void test54_verifier() {
        MyValue1Wrapper w = new MyValue1Wrapper(MyValue1.createWithFieldsInline(rI, rL));
        Asserts.assertEquals(test54(false, false), MyValue1Wrapper.default.hash());
        Asserts.assertEquals(test54(false, true), w.hash());
        Asserts.assertEquals(test54(true, false), MyValue1Wrapper.default.hash());
        Asserts.assertEquals(test54(true, true), 0L);
    }

    @Test
    @IR(failOn = {ALLOC_G, STORE, TRAP})
    public int test55(boolean b) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1Wrapper.ref w = new MyValue1Wrapper(val);
        if (b) {
            w = new MyValue1Wrapper(refField);
        }
        return w.vt.x;
    }

    @Run(test = "test55")
    public void test55_verifier(RunInfo info) {
        refField = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test55(true), refField.x);
        Asserts.assertEquals(test55(false), testValue1.x);
        if (!info.isWarmUp()) {
            refField = null;
            try {
                Asserts.assertEquals(test55(false), testValue1.x);
                test55(true);
                throw new RuntimeException("NullPointerException expected");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    @Test
    @IR(failOn = {ALLOC, STORE, TRAP})
    public long test56(boolean b) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1Wrapper.ref w = new MyValue1Wrapper(val);
        if (b) {
            w = new MyValue1Wrapper(refField);
        }
        return w.vt.hash();
    }

    @Run(test = "test56")
    public void test56_verifier(RunInfo info) {
        refField = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test56(true), refField.hash());
        Asserts.assertEquals(test56(false), testValue1.hash());
        if (!info.isWarmUp()) {
            refField = null;
            try {
                Asserts.assertEquals(test56(false), testValue1.hash());
                test56(true);
                throw new RuntimeException("NullPointerException expected");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    @Test
    public MyValue1.ref test57(boolean b) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1Wrapper.ref w = new MyValue1Wrapper(val);
        if (b) {
            w = new MyValue1Wrapper(refField);
        }
        return w.vt;
    }

    @Run(test = "test57")
    public void test57_verifier(RunInfo info) {
        refField = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test57(true).hash(), refField.hash());
        Asserts.assertEquals(test57(false).hash(), testValue1.hash());
        if (!info.isWarmUp()) {
            refField = null;
            Asserts.assertEquals(test57(true), null);
        }
    }

    // Test scalarization when .ref is referenced in safepoint debug info
    @Test
    @IR(failOn = {ALLOC, STORE})
    public int test58(boolean b1, boolean b2, Method m) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1Wrapper.ref w = new MyValue1Wrapper(val);
        if (b1) {
            w = new MyValue1Wrapper(refField);
        }
        if (b2) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        return w.vt.x;
    }

    @Run(test = "test58")
    public void test58_verifier(RunInfo info) {
        refField = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test58(true, false, info.getTest()), refField.x);
        Asserts.assertEquals(test58(false, false, info.getTest()), testValue1.x);
        if (!info.isWarmUp()) {
            refField = null;
            try {
                Asserts.assertEquals(test58(false, false, info.getTest()), testValue1.x);
                test58(true, false, info.getTest());
                throw new RuntimeException("NullPointerException expected");
            } catch (NullPointerException e) {
                // Expected
            }
            refField = MyValue1.createWithFieldsInline(rI+1, rL+1);
            Asserts.assertEquals(test58(true, true, info.getTest()), refField.x);
            Asserts.assertEquals(test58(false, true, info.getTest()), testValue1.x);
        }
    }

    @Test
    public MyValue1.ref test59(boolean b1, boolean b2, Method m) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1Wrapper.ref w = new MyValue1Wrapper(val);
        if (b1) {
            w = new MyValue1Wrapper(refField);
        }
        if (b2) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        return w.vt;
    }

    @Run(test = "test59")
    public void test59_verifier(RunInfo info) {
        refField = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test59(true, false, info.getTest()).hash(), refField.hash());
        Asserts.assertEquals(test59(false, false, info.getTest()).hash(), testValue1.hash());
        if (!info.isWarmUp()) {
            refField = null;
            Asserts.assertEquals(test59(true, false, info.getTest()), null);
            refField = MyValue1.createWithFieldsInline(rI+1, rL+1);
            Asserts.assertEquals(test59(true, true, info.getTest()).hash(), refField.hash());
            Asserts.assertEquals(test59(false, true, info.getTest()).hash(), testValue1.hash());
        }
    }

    @Test
    @IR(failOn = {ALLOC_G, LOAD, STORE, TRAP})
    public int test60(boolean b) {
        MyValue1Wrapper.ref w = new MyValue1Wrapper(null);
        if (b) {
            MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
            w = new MyValue1Wrapper(val);
        }
        return w.vt.x;
    }

    @Run(test = "test60")
    public void test60_verifier() {
        Asserts.assertEquals(test60(true), testValue1.x);
        try {
            test60(false);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public MyValue1.ref test61(boolean b) {
        MyValue1Wrapper.ref w = new MyValue1Wrapper(null);
        if (b) {
            MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
            w = new MyValue1Wrapper(val);
        }
        return w.vt;
    }

    @Run(test = "test61")
    public void test61_verifier() {
        Asserts.assertEquals(test61(true).hash(), testValue1.hash());
        Asserts.assertEquals(test61(false), null);
    }

    @Test
    @IR(failOn = {ALLOC_G, LOAD, STORE, TRAP})
    public int test62(boolean b) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1Wrapper.ref w = new MyValue1Wrapper(val);
        if (b) {
            w = new MyValue1Wrapper(null);
        }
        return w.vt.x;
    }

    @Run(test = "test62")
    public void test62_verifier() {
        Asserts.assertEquals(test62(false), testValue1.x);
        try {
            test62(true);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public MyValue1.ref test63(boolean b) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1Wrapper.ref w = new MyValue1Wrapper(val);
        if (b) {
            w = new MyValue1Wrapper(null);
        }
        return w.vt;
    }

    @Run(test = "test63")
    public void test63_verifier() {
        Asserts.assertEquals(test63(false).hash(), testValue1.hash());
        Asserts.assertEquals(test63(true), null);
    }

    @ForceInline
    public MyValue1.ref test64_helper() {
        return flatField;
    }

    @Test
    @IR(failOn = {ALLOC_G, TRAP})
    public void test64(boolean b) {
        MyValue1Wrapper.ref w = new MyValue1Wrapper(null);
        if (b) {
            w = new MyValue1Wrapper(testValue1);
        } else {
            w = new MyValue1Wrapper(test64_helper());
        }
        flatField = w.vt;
    }

    @Run(test = "test64")
    public void test64_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI+1, rL+1);
        flatField = vt;
        test64(false);
        Asserts.assertEquals(flatField.hash(), vt.hash());
        test64(true);
        Asserts.assertEquals(flatField.hash(), testValue1.hash());
    }

    @Test
    @IR(failOn = {ALLOC_G, LOAD, STORE, TRAP})
    public long test65(boolean b) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        if (b) {
            val = null;
        }
        if (val != null) {
            return val.hashPrimitive();
        }
        return 42;
    }

    @Run(test = "test65")
    public void test65_verifier() {
        Asserts.assertEquals(test65(true), 42L);
        Asserts.assertEquals(test65(false), MyValue1.createWithFieldsInline(rI, rL).hashPrimitive());
    }

    @ForceInline
    public Object test66_helper(Object arg) {
        return arg;
    }

    // Test that .ref arg does not block scalarization
    @Test
    @IR(failOn = {ALLOC, STORE})
    public int test66(boolean b1, boolean b2, MyValue1.ref arg, Method m) {
        Object val = MyValue1.createWithFieldsInline(rI, rL);
        if (b1) {
            val = test66_helper(arg);
        }
        if (b2) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        return ((MyValue1)val).x;
    }

    @Run(test = "test66")
    public void test66_verifier(RunInfo info) {
        MyValue1 arg = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test66(true, false, arg, info.getTest()), arg.x);
        Asserts.assertEquals(test66(false, false, arg, info.getTest()), testValue1.x);
        if (!info.isWarmUp()) {
            try {
                Asserts.assertEquals(test66(false, false, arg, info.getTest()), testValue1.x);
                test66(true, false, null, info.getTest());
                throw new RuntimeException("NullPointerException expected");
            } catch (NullPointerException e) {
                // Expected
            }
            Asserts.assertEquals(test66(true, true, arg, info.getTest()), arg.x);
            Asserts.assertEquals(test66(false, true, arg, info.getTest()), testValue1.x);
        }
    }

    @DontInline
    public MyValue1.ref test67_helper1() {
        return refField;
    }

    @ForceInline
    public Object test67_helper2() {
        return test67_helper1();
    }

    // Test that .ref return does not block scalarization
    @Test
    @IR(failOn = {ALLOC, STORE})
    public long test67(boolean b1, boolean b2, Method m) {
        Object val = MyValue1.createWithFieldsInline(rI, rL);
        if (b1) {
            val = test67_helper2();
        }
        if (b2) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        return ((MyValue1)val).hash();
    }

    @Run(test = "test67")
    public void test67_verifier(RunInfo info) {
        refField = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test67(true, false, info.getTest()), refField.hash());
        Asserts.assertEquals(test67(false, false, info.getTest()), testValue1.hash());
        if (!info.isWarmUp()) {
            refField = null;
            try {
                Asserts.assertEquals(test67(false, false, info.getTest()), testValue1.hash());
                test67(true, false, info.getTest());
                throw new RuntimeException("NullPointerException expected");
            } catch (NullPointerException e) {
                // Expected
            }
            refField = MyValue1.createWithFieldsInline(rI+1, rL+1);
            Asserts.assertEquals(test67(true, true, info.getTest()), refField.hash());
            Asserts.assertEquals(test67(false, true, info.getTest()), testValue1.hash());
        }
    }

    @ForceInline
    public Object test68_helper(Object arg) {
        MyValue1.ref tmp = (MyValue1)arg; // Result of cast is unused
        return arg;
    }

    // Test that scalarization enabled by cast is applied to parsing map
    @Test
    @IR(failOn = {ALLOC, STORE})
    public int test68(boolean b1, boolean b2, Object arg, Method m) {
        Object val = MyValue1.createWithFieldsInline(rI, rL);
        if (b1) {
            val = test68_helper(arg);
        }
        if (b2) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        return ((MyValue1)val).x;
    }

    @Run(test = "test68")
    public void test68_verifier(RunInfo info) {
        MyValue1 arg = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test68(true, false, arg, info.getTest()), arg.x);
        Asserts.assertEquals(test68(false, false, arg, info.getTest()), testValue1.x);
        if (!info.isWarmUp()) {
            try {
                Asserts.assertEquals(test68(false, false, arg, info.getTest()), testValue1.x);
                test68(true, false, null, info.getTest());
                throw new RuntimeException("NullPointerException expected");
            } catch (NullPointerException e) {
                // Expected
            }
            Asserts.assertEquals(test68(true, true, arg, info.getTest()), arg.x);
            Asserts.assertEquals(test68(false, true, arg, info.getTest()), testValue1.x);
        }
    }

    @ForceInline
    public Object test69_helper(Object arg) {
        MyValue1.ref tmp = (MyValue1)arg; // Result of cast is unused
        return arg;
    }

    // Same as test68 but with ClassCastException
    @Test
    @IR(failOn = {ALLOC, STORE})
    public int test69(boolean b1, boolean b2, Object arg, Method m) {
        Object val = MyValue1.createWithFieldsInline(rI, rL);
        if (b1) {
            val = test69_helper(arg);
        }
        if (b2) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        return ((MyValue1)val).x;
    }

    @Run(test = "test69")
    @Warmup(10000) // Make sure precise profile information is available
    public void test69_verifier(RunInfo info) {
        MyValue1 arg = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test69(true, false, arg, info.getTest()), arg.x);
        Asserts.assertEquals(test69(false, false, arg, info.getTest()), testValue1.x);
        try {
            test69(true, false, 42, info.getTest());
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException e) {
            // Expected
        }
        if (!info.isWarmUp()) {
            try {
                Asserts.assertEquals(test69(false, false, arg, info.getTest()), testValue1.x);
                test69(true, false, null, info.getTest());
                throw new RuntimeException("NullPointerException expected");
            } catch (NullPointerException e) {
                // Expected
            }
            Asserts.assertEquals(test69(true, true, arg, info.getTest()), arg.x);
            Asserts.assertEquals(test69(false, true, arg, info.getTest()), testValue1.x);
        }
    }

    @ForceInline
    public Object test70_helper(Object arg) {
        MyValue1.ref tmp = (MyValue1)arg; // Result of cast is unused
        return arg;
    }

    // Same as test68 but with ClassCastException and frequent NullPointerException
    @Test
    @IR(failOn = {ALLOC, STORE})
    public int test70(boolean b1, boolean b2, Object arg, Method m) {
        Object val = MyValue1.createWithFieldsInline(rI, rL);
        if (b1) {
            val = test70_helper(arg);
        }
        if (b2) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        return ((MyValue1)val).x;
    }

    @Run(test = "test70")
    @Warmup(10000) // Make sure precise profile information is available
    public void test70_verifier(RunInfo info) {
        MyValue1 arg = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test70(true, false, arg, info.getTest()), arg.x);
        Asserts.assertEquals(test70(false, false, arg, info.getTest()), testValue1.x);
        try {
            test70(true, false, 42, info.getTest());
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException e) {
            // Expected
        }
        try {
            Asserts.assertEquals(test70(false, false, arg, info.getTest()), testValue1.x);
            test70(true, false, null, info.getTest());
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        if (!info.isWarmUp()) {
            Asserts.assertEquals(test70(true, true, arg, info.getTest()), arg.x);
            Asserts.assertEquals(test70(false, true, arg, info.getTest()), testValue1.x);
        }
    }

    @ForceInline
    public Object test71_helper(Object arg) {
        MyValue1.ref tmp = (MyValue1.ref)arg; // Result of cast is unused
        return arg;
    }

    // Same as test68 but with .ref cast
    @Test
    @IR(failOn = {ALLOC, STORE})
    public int test71(boolean b1, boolean b2, Object arg, Method m) {
        Object val = MyValue1.createWithFieldsInline(rI, rL);
        if (b1) {
            val = test71_helper(arg);
        }
        if (b2) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        return ((MyValue1.ref)val).x;
    }

    @Run(test = "test71")
    public void test71_verifier(RunInfo info) {
        MyValue1 arg = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test71(true, false, arg, info.getTest()), arg.x);
        Asserts.assertEquals(test71(false, false, arg, info.getTest()), testValue1.x);
        if (!info.isWarmUp()) {
            try {
                Asserts.assertEquals(test71(false, false, arg, info.getTest()), testValue1.x);
                test71(true, false, null, info.getTest());
                throw new RuntimeException("NullPointerException expected");
            } catch (NullPointerException e) {
                // Expected
            }
            Asserts.assertEquals(test71(true, true, arg, info.getTest()), arg.x);
            Asserts.assertEquals(test71(false, true, arg, info.getTest()), testValue1.x);
        }
    }

    @ForceInline
    public Object test72_helper(Object arg) {
        MyValue1.ref tmp = (MyValue1.ref)arg; // Result of cast is unused
        return arg;
    }

    // Same as test71 but with ClassCastException and hash() call
    @Test
    @IR(failOn = {ALLOC, STORE})
    public long test72(boolean b1, boolean b2, Object arg, Method m) {
        Object val = MyValue1.createWithFieldsInline(rI, rL);
        if (b1) {
            val = test72_helper(arg);
        }
        if (b2) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        return ((MyValue1.ref)val).hash();
    }

    @Run(test = "test72")
    @Warmup(10000) // Make sure precise profile information is available
    public void test72_verifier(RunInfo info) {
        MyValue1 arg = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test72(true, false, arg, info.getTest()), arg.hash());
        Asserts.assertEquals(test72(false, false, arg, info.getTest()), testValue1.hash());
        try {
            test72(true, false, 42, info.getTest());
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException e) {
            // Expected
        }
        if (!info.isWarmUp()) {
            try {
                Asserts.assertEquals(test72(false, false, arg, info.getTest()), testValue1.hash());
                test72(true, false, null, info.getTest());
                throw new RuntimeException("NullPointerException expected");
            } catch (NullPointerException e) {
                // Expected
            }
            Asserts.assertEquals(test72(true, true, arg, info.getTest()), arg.hash());
            Asserts.assertEquals(test72(false, true, arg, info.getTest()), testValue1.hash());
        }
    }

    @ForceInline
    public Object test73_helper(Object arg) {
        MyValue1.ref tmp = (MyValue1.ref)arg; // Result of cast is unused
        return arg;
    }

    // Same as test71 but with ClassCastException and frequent NullPointerException
    @Test
    @IR(failOn = {ALLOC, STORE})
    public int test73(boolean b1, boolean b2, Object arg, Method m) {
        Object val = MyValue1.createWithFieldsInline(rI, rL);
        if (b1) {
            val = test73_helper(arg);
        }
        if (b2) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        return ((MyValue1.ref)val).x;
    }

    @Run(test = "test73")
    @Warmup(10000) // Make sure precise profile information is available
    public void test73_verifier(RunInfo info) {
        MyValue1 arg = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test73(true, false, arg, info.getTest()), arg.x);
        Asserts.assertEquals(test73(false, false, arg, info.getTest()), testValue1.x);
        try {
            test73(true, false, 42, info.getTest());
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException e) {
            // Expected
        }
        try {
            Asserts.assertEquals(test73(false, false, arg, info.getTest()), testValue1.x);
            test73(true, false, null, info.getTest());
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        if (!info.isWarmUp()) {
            Asserts.assertEquals(test73(true, true, arg, info.getTest()), arg.x);
            Asserts.assertEquals(test73(false, true, arg, info.getTest()), testValue1.x);
        }
    }

    @ForceInline
    public Object test74_helper(Object arg) {
        return (MyValue1.ref)arg;
    }

    // Same as test73 but result of cast is used and hash() is called
    @Test
    @IR(failOn = {ALLOC, STORE})
    public long test74(boolean b1, boolean b2, Object arg, Method m) {
        Object val = MyValue1.createWithFieldsInline(rI, rL);
        if (b1) {
            val = test74_helper(arg);
        }
        if (b2) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        return ((MyValue1.ref)val).hash();
    }

    @Run(test = "test74")
    @Warmup(10000) // Make sure precise profile information is available
    public void test74_verifier(RunInfo info) {
        MyValue1 arg = MyValue1.createWithFieldsInline(rI+1, rL+1);
        Asserts.assertEquals(test74(true, false, arg, info.getTest()), arg.hash());
        Asserts.assertEquals(test74(false, false, arg, info.getTest()), testValue1.hash());
        try {
            test74(true, false, 42, info.getTest());
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException e) {
            // Expected
        }
        try {
            Asserts.assertEquals(test74(false, false, arg, info.getTest()), testValue1.hash());
            test74(true, false, null, info.getTest());
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        if (!info.isWarmUp()) {
            Asserts.assertEquals(test74(true, true, arg, info.getTest()), arg.hash());
            Asserts.assertEquals(test74(false, true, arg, info.getTest()), testValue1.hash());
        }
    }

    // Test new merge path being added for exceptional control flow
    @Test
    @IR(failOn = {ALLOC})
    public MyValue1.ref test75(MyValue1.ref vt, Object obj) {
        try {
            vt = (MyValue1.ref)obj;
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException e) {
            // Expected
        }
        return vt;
    }

    @Run(test = "test75")
    public void test75_verifier() {
        MyValue1.ref vt = testValue1;
        MyValue1.ref result = test75(vt, Integer.valueOf(rI));
        Asserts.assertEquals(result.hash(), vt.hash());
    }

    @ForceInline
    public Object test76_helper() {
        return constNullRefField;
    }

    // Test that constant null .ref field does not block scalarization
    @Test
    @IR(failOn = {ALLOC, LOAD, STORE})
    public long test76(boolean b1, boolean b2, Method m) {
        Object val = MyValue1.createWithFieldsInline(rI, rL);
        if (b1) {
            val = test76_helper();
        }
        if (b2) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        return ((MyValue1)val).hash();
    }

    @Run(test = "test76")
    public void test76_verifier(RunInfo info) {
        Asserts.assertEquals(test76(false, false, info.getTest()), testValue1.hash());
        try {
            test76(true, false, info.getTest());
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        if (!info.isWarmUp()) {
            Asserts.assertEquals(test76(false, true, info.getTest()), testValue1.hash());
            try {
                test76(true, true, info.getTest());
                throw new RuntimeException("NullPointerException expected");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    private static final Object constObjectValField = MyValue1.createWithFieldsInline(rI+1, rL+1);

    @ForceInline
    public Object test77_helper() {
        return constObjectValField;
    }

    // Test that constant object field with inline type content does not block scalarization
    @Test
    @IR(failOn = {ALLOC, LOAD, STORE})
    public long test77(boolean b1, boolean b2, Method m) {
        Object val = MyValue1.createWithFieldsInline(rI, rL);
        if (b1) {
            val = test77_helper();
        }
        if (b2) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        return ((MyValue1)val).hash();
    }

    @Run(test = "test77")
    public void test77_verifier(RunInfo info) {
        Asserts.assertEquals(test77(true, false, info.getTest()), ((MyValue1)constObjectValField).hash());
        Asserts.assertEquals(test77(false, false, info.getTest()), testValue1.hash());
        if (!info.isWarmUp()) {
          Asserts.assertEquals(test77(true, false, info.getTest()), ((MyValue1)constObjectValField).hash());
          Asserts.assertEquals(test77(false, false, info.getTest()), testValue1.hash());
        }
    }

    @ForceInline
    public Object test78_helper() {
        return null;
    }

    // Test that constant null does not block scalarization
    @Test
    @IR(failOn = {ALLOC, LOAD, STORE})
    public long test78(boolean b1, boolean b2, Method m) {
        Object val = MyValue1.createWithFieldsInline(rI, rL);
        if (b1) {
            val = test78_helper();
        }
        if (b2) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        return ((MyValue1)val).hash();
    }

    @Run(test = "test78")
    public void test78_verifier(RunInfo info) {
        Asserts.assertEquals(test78(false, false, info.getTest()), testValue1.hash());
        try {
            test78(true, false, info.getTest());
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        if (!info.isWarmUp()) {
            Asserts.assertEquals(test78(false, true, info.getTest()), testValue1.hash());
            try {
                test78(true, true, info.getTest());
                throw new RuntimeException("NullPointerException expected");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    @ForceInline
    public Object test79_helper() {
        return null;
    }

    // Same as test78 but will trigger different order of PhiNode inputs
    @Test
    @IR(failOn = {ALLOC, LOAD, STORE})
    public long test79(boolean b1, boolean b2, Method m) {
        Object val = test79_helper();
        if (b1) {
            val = MyValue1.createWithFieldsInline(rI, rL);
        }
        if (b2) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        return ((MyValue1)val).hash();
    }

    @Run(test = "test79")
    public void test79_verifier(RunInfo info) {
        Asserts.assertEquals(test79(true, false, info.getTest()), testValue1.hash());
        try {
            test79(false, false, info.getTest());
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        if (!info.isWarmUp()) {
            Asserts.assertEquals(test79(true, true, info.getTest()), testValue1.hash());
            try {
                test79(false, true, info.getTest());
                throw new RuntimeException("NullPointerException expected");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    @ForceInline
    public Object test80_helper(Object obj, int i) {
        if ((i % 2) == 0) {
            return MyValue1.createWithFieldsInline(i, i);
        }
        return obj;
    }

    // Test that phi nodes referencing themselves (loops) do not block scalarization
    @Test
    @IR(failOn = {ALLOC, LOAD, STORE})
    public long test80() {
        Object val = MyValue1.createWithFieldsInline(rI, rL);
        for (int i = 0; i < 100; ++i) {
            val = test80_helper(val, i);
        }
        return ((MyValue1.ref)val).hash();
    }

    private final long test80Result = test80();

    @Run(test = "test80")
    public void test80_verifier() {
        Asserts.assertEquals(test80(), test80Result);
    }

    @ForceInline
    public Object test81_helper(Object obj, int i) {
        if ((i % 2) == 0) {
            return MyValue1.createWithFieldsInline(i, i);
        }
        return obj;
    }

    // Test nested loops
    @Test
    @IR(failOn = {ALLOC, LOAD, STORE})
    public long test81() {
        Object val = null;
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                for (int k = 0; k < 10; ++k) {
                    val = test81_helper(val, i + j + k);
                }
                val = test81_helper(val, i + j);
            }
            val = test81_helper(val, i);
        }
        return ((MyValue1.ref)val).hash();
    }

    private final long test81Result = test81();

    @Run(test = "test81")
    public void test81_verifier() {
        Asserts.assertEquals(test82(), test82Result);
    }

    @ForceInline
    public Object test82_helper(Object obj, int i) {
        if ((i % 2) == 0) {
            return MyValue1.createWithFieldsInline(i, i);
        }
        return obj;
    }

    // Test loops with casts
    @Test
    @IR(failOn = {ALLOC, LOAD, STORE})
    public long test82() {
        Object val = null;
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                for (int k = 0; k < 10; ++k) {
                    val = test82_helper(val, i + j + k);
                }
                if (val != null) {
                    val = test82_helper(val, i + j);
                }
            }
            val = test82_helper(val, i);
        }
        return ((MyValue1.ref)val).hash();
    }

    private final long test82Result = test82();

    @Run(test = "test82")
    public void test82_verifier() {
        Asserts.assertEquals(test82(), test82Result);
    }

    @ForceInline
    public Object test83_helper(boolean b) {
        if (b) {
            return MyValue1.createWithFieldsInline(rI, rL);
        }
        return null;
    }

    // Test that CastPP does not block sclarization in safepoints
    @Test
    @IR(failOn = {ALLOC, LOAD, STORE})
    public long test83(boolean b, Method m) {
        Object val = test83_helper(b);
        if (val != null) {
            // Uncommon trap
            TestFramework.deoptimize(m);
            return ((MyValue1.ref)val).hash();
        }
        return 0;
    }

    @Run(test = "test83")
    public void test83_verifier(RunInfo info) {
        Asserts.assertEquals(test83(false, info.getTest()), 0L);
        if (!info.isWarmUp()) {
            Asserts.assertEquals(test83(true, info.getTest()), testValue1.hash());
        }
    }

    @ForceInline
    public Object test84_helper(Object obj, int i) {
        if ((i % 2) == 0) {
            return new MyValue1Wrapper(MyValue1.createWithFieldsInline(i, i));
        }
        return obj;
    }

    // Same as test80 but with wrapper
    @Test
    @IR(failOn = {ALLOC, LOAD, STORE})
    public long test84() {
        Object val = new MyValue1Wrapper(MyValue1.createWithFieldsInline(rI, rL));
        for (int i = 0; i < 100; ++i) {
            val = test84_helper(val, i);
        }
        return ((MyValue1Wrapper.ref)val).vt.hash();
    }

    private final long test84Result = test84();

    @Run(test = "test84")
    public void test84_verifier() {
        Asserts.assertEquals(test84(), test84Result);
    }

    @ForceInline
    public Object test85_helper(Object obj, int i) {
        if ((i % 2) == 0) {
            return new MyValue1Wrapper(MyValue1.createWithFieldsInline(i, i));
        }
        return obj;
    }

    // Same as test81 but with wrapper
    @Test
    @IR(failOn = {ALLOC, LOAD, STORE})
    public long test85() {
        Object val = new MyValue1Wrapper(null);
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                for (int k = 0; k < 10; ++k) {
                    val = test85_helper(val, i + j + k);
                }
                val = test85_helper(val, i + j);
            }
            val = test85_helper(val, i);
        }
        return ((MyValue1Wrapper.ref)val).vt.hash();
    }

    private final long test85Result = test85();

    @Run(test = "test85")
    public void test85_verifier() {
        Asserts.assertEquals(test85(), test85Result);
    }

    static final class ObjectWrapper {
        public Object obj;

        @ForceInline
        public ObjectWrapper(Object obj) {
            this.obj = obj;
        }
    }

    // Test scalarization with phi referencing itself
    @Test
    @IR(applyIf = {"InlineTypePassFieldsAsArgs", "true"},
        failOn = {ALLOC, STORE},
        counts = {LOAD, " = 4"}) // 4 loads from the non-flattened MyValue1.v4 fields
    @IR(applyIf = {"InlineTypePassFieldsAsArgs", "false"},
        failOn = {ALLOC, STORE})
    public long test86(MyValue1 vt) {
        ObjectWrapper val = new ObjectWrapper(vt);
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                val.obj = val.obj;
            }
        }
        return ((MyValue1.ref)val.obj).hash();
    }

    @Run(test = "test86")
    public void test86_verifier() {
        test86(testValue1);
        Asserts.assertEquals(test86(testValue1), testValue1.hash());
    }

    public static primitive class Test87C0 {
        int x = rI;
    }

    public static primitive class Test87C1 {
        Test87C0 field = Test87C0.default;
    }

    public static primitive class Test87C2 {
        Test87C1 field = Test87C1.default;
    }

    // Test merging .val and .ref in return
    @Test
    public Test87C1 test87(boolean b, Test87C2.val v1, Test87C2.ref v2) {
        if (b) {
            return v1.field;
        } else {
            return v2.field;
        }
    }

    @Run(test = "test87")
    public void test87_verifier() {
        Test87C2 v = new Test87C2();
        Asserts.assertEQ(test87(true, v, v), v.field);
        Asserts.assertEQ(test87(false, v, v), v.field);
    }

    static primitive class Test88Value {
        int x = 0;
    }

    static class Test88MyClass {
        int x = 0;
        int y = rI;
    }

    @ForceInline
    Object test88Helper() {
        return new Test88Value();
    }

    // Test LoadNode::Identity optimization with always failing checkcast
    @Test
    public int test88() {
        Object obj = test88Helper();
        return ((Test88MyClass)obj).y;
    }

    @Run(test = "test88")
    public void test88_verifier() {
        try {
            test88();
            throw new RuntimeException("No ClassCastException thrown");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    // Same as test88 but with Phi
    @Test
    public int test89(boolean b) {
        Test88MyClass obj = b ? (Test88MyClass)test88Helper() : (Test88MyClass)test88Helper();
        return obj.y;
    }

    @Run(test = "test89")
    public void test89_verifier() {
        try {
            test89(false);
            throw new RuntimeException("No ClassCastException thrown");
        } catch (ClassCastException e) {
            // Expected
        }
        try {
            test89(true);
            throw new RuntimeException("No ClassCastException thrown");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    @ForceInline
    public boolean test90_inline(MyValue1.ref vt) {
        return vt == null;
    }

    // Test scalarization with speculative NULL type
    @Test
    @IR(failOn = {ALLOC})
    public boolean test90(Method m) throws Exception {
        Object arg = null;
        return (boolean)m.invoke(this, arg);
    }

    @Run(test = "test90")
    @Warmup(10000)
    public void test90_verifier() throws Exception {
        Method m = getClass().getMethod("test90_inline", MyValue1.ref.class);
        Asserts.assertTrue(test90(m));
    }

    // Test that scalarization does not introduce redundant/unused checks
    @Test
    @IR(applyIf = {"InlineTypePassFieldsAsArgs", "false"},
        failOn = {ALLOC, CMPP})
    public Object test91(MyValue1.ref vt) {
        return vt;
    }

    @Run(test = "test91")
    public void test91_verifier() {
        Asserts.assertEQ(test91(testValue1), testValue1);
    }

    MyValue1.ref test92Field = testValue1;

    // Same as test91 but with field access
    @Test
    @IR(failOn = {ALLOC, CMPP})
    public Object test92() {
        return test92Field;
    }

    @Run(test = "test92")
    public void test92_verifier() {
        Asserts.assertEQ(test92(), testValue1);
    }

    private static final MethodHandle refCheckCast = InstructionHelper.loadCode(MethodHandles.lookup(),
        "refCheckCast",
        MethodType.methodType(MyValue2.class.asPrimaryType(), TestNullableInlineTypes.class, MyValue1.class.asPrimaryType()),
        CODE -> {
            CODE.
            aload_1().
            checkcast(MyValue2.class.asPrimaryType()).
            return_(TypeTag.A);
        });

    // Test checkcast that only passes with null
    @Test
    public Object test93(MyValue1.ref vt) throws Throwable {
        return refCheckCast.invoke(this, vt);
    }

    @Run(test = "test93")
    @Warmup(10000)
    public void test93_verifier() throws Throwable {
        Asserts.assertEQ(test93(null), null);
    }

    @DontInline
    public MyValue1.ref test94_helper1(MyValue1.ref vt) {
        return vt;
    }

    @ForceInline
    public MyValue1.ref test94_helper2(MyValue1.ref vt) {
        return test94_helper1(vt);
    }

    @ForceInline
    public MyValue1.ref test94_helper3(Object vt) {
        return test94_helper2((MyValue1.ref)vt);
    }

    // Test that calling convention optimization prevents buffering of arguments
    @Test
    @IR(applyIf = {"InlineTypePassFieldsAsArgs", "true"},
        counts = {ALLOC_G, " = 2"}) // 1 MyValue2 allocation + 1 Integer allocation
    @IR(applyIf = {"InlineTypePassFieldsAsArgs", "false"},
        counts = {ALLOC_G, " = 3"}) // 1 MyValue1 allocation + 1 MyValue2 allocation + 1 Integer allocation
    public MyValue1.ref test94(MyValue1.ref vt) {
        MyValue1.ref res = test94_helper1(vt);
        vt = MyValue1.createWithFieldsInline(rI, rL);
        test94_helper1(vt);
        test94_helper2(vt);
        test94_helper3(vt);
        return res;
    }

    @Run(test = "test94")
    public void test94_verifier() {
        Asserts.assertEQ(test94(testValue1), testValue1);
        Asserts.assertEQ(test94(null), null);
    }

    @DontInline
    public static MyValue1.ref test95_helper1(MyValue1.ref vt) {
        return vt;
    }

    @ForceInline
    public static MyValue1.ref test95_helper2(MyValue1.ref vt) {
        return test95_helper1(vt);
    }

    @ForceInline
    public static MyValue1.ref test95_helper3(Object vt) {
        return test95_helper2((MyValue1.ref)vt);
    }

    // Same as test94 but with static methods to trigger simple adapter logic
    @Test
    @IR(applyIf = {"InlineTypePassFieldsAsArgs", "true"},
        counts = {ALLOC_G, " = 2"}) // 1 MyValue2 allocation + 1 Integer allocation
    @IR(applyIf = {"InlineTypePassFieldsAsArgs", "false"},
        counts = {ALLOC_G, " = 3"}) // 1 MyValue1 allocation + 1 MyValue2 allocation + 1 Integer allocation
    public static MyValue1.ref test95(MyValue1.ref vt) {
        MyValue1.ref res = test95_helper1(vt);
        vt = MyValue1.createWithFieldsInline(rI, rL);
        test95_helper1(vt);
        test95_helper2(vt);
        test95_helper3(vt);
        return res;
    }

    @Run(test = "test95")
    public void test95_verifier() {
        Asserts.assertEQ(test95(testValue1), testValue1);
        Asserts.assertEQ(test95(null), null);
    }

    @DontInline
    public MyValue2.ref test96_helper1(boolean b) {
        return b ? null : MyValue2.createWithFieldsInline(rI, rD);
    }

    @ForceInline
    public MyValue2.ref test96_helper2() {
        return null;
    }

    @ForceInline
    public MyValue2.ref test96_helper3(boolean b) {
        return b ? null : MyValue2.createWithFieldsInline(rI, rD);
    }

    // Test that calling convention optimization prevents buffering of return values
    @Test
    @IR(applyIf = {"InlineTypeReturnedAsFields", "true"},
        failOn = {ALLOC_G})
    @IR(applyIf = {"InlineTypeReturnedAsFields", "false"},
        counts = {ALLOC_G, " = 1"})
    public MyValue2.ref test96(int c, boolean b) {
        MyValue2.ref res = null;
        if (c == 1) {
            res = test96_helper1(b);
        } else if (c == 2) {
            res = test96_helper2();
        } else if (c == 3) {
            res = test96_helper3(b);
        }
        return res;
    }

    @Run(test = "test96")
    public void test96_verifier() {
        Asserts.assertEQ(test96(0, false), null);
        Asserts.assertEQ(test96(1, false).hash(), MyValue2.createWithFieldsInline(rI, rD).hash());
        Asserts.assertEQ(test96(1, true), null);
        Asserts.assertEQ(test96(2, false), null);
        Asserts.assertEQ(test96(3, false).hash(), MyValue2.createWithFieldsInline(rI, rD).hash());
        Asserts.assertEQ(test96(3, true), null);
    }

    @DontInline
    public MyValue3.ref test97_helper1(boolean b) {
        return b ? null: MyValue3.create();
    }

    @ForceInline
    public MyValue3.ref test97_helper2() {
        return null;
    }

    @ForceInline
    public MyValue3.ref test97_helper3(boolean b) {
        return b ? null: MyValue3.create();
    }

    MyValue3 test97_res1;
    MyValue3 test97_res3;

    // Same as test96 but with MyValue3 return
    @Test
    @IR(applyIf = {"InlineTypeReturnedAsFields", "true"},
        counts = {ALLOC_G, " = 1"}) // 1 Object allocation
    @IR(applyIf = {"InlineTypeReturnedAsFields", "false"},
        counts = {ALLOC_G, " = 2"}) // 1 MyValue3 allocation + 1 Object allocation
    public MyValue3.ref test97(int c, boolean b) {
        MyValue3.ref res = null;
        if (c == 1) {
            res = test97_helper1(b);
            if (res != null) {
                test97_res1 = res;
            }
        } else if (c == 2) {
            res = test97_helper2();
        } else if (c == 3) {
            res = test97_helper3(b);
            if (res != null) {
                test97_res3 = res;
            }
        }
        return res;
    }

    @Run(test = "test97")
    public void test97_verifier() {
        Asserts.assertEQ(test97(0, false), null);
        Asserts.assertEQ(test97(1, false), test97_res1);
        Asserts.assertEQ(test97(1, true), null);
        Asserts.assertEQ(test97(2, false), null);
        Asserts.assertEQ(test97(3, false), test97_res3);
        Asserts.assertEQ(test97(3, true), null);
    }
}
