/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test correct handling of value classes.
 * @library /test/lib /test/jdk/lib/testlibrary/bytecode /test/jdk/java/lang/invoke/common /
 * @requires (os.simpleArch == "x64" | os.simpleArch == "aarch64")
 * @build test.java.lang.invoke.lib.InstructionHelper
 * @run driver/timeout=300 compiler.valhalla.inlinetypes.TestValueClasses
 */

@ForceCompileClassInitializer
public class TestValueClasses {

    public static void main(String[] args) {

        Scenario[] scenarios = InlineTypes.DEFAULT_SCENARIOS;
        scenarios[3].addFlags("-XX:-MonomorphicArrayCheck", "-XX:FlatArrayElementMaxSize=-1");
        scenarios[4].addFlags("-XX:-MonomorphicArrayCheck");

        InlineTypes.getFramework()
                   .addScenarios(scenarios)
                   .addHelperClasses(MyValueClass1.class,
                                     MyValueClass2.class,
                                     MyValueClass2Inline.class)
                   .start();
    }

    private static final MyValueClass1 testValue1 = MyValueClass1.createWithFieldsInline(rI, rL);

    MyValueClass1 nullValField = null;
    MyValueClass1 testField1;
    MyValueClass1 testField2;
    MyValueClass1 testField3;
    MyValueClass1 testField4;
    static MyValueClass1 testField5;
    static MyValueClass1 testField6;
    static MyValueClass1 testField7;
    static MyValueClass1 testField8;

    // Test field loads
    @Test
    public long test1(boolean b) {
        MyValueClass1 val1 = b ? testField3 : MyValueClass1.createWithFieldsInline(rI, rL);
        MyValueClass1 val2 = b ? testField7 : MyValueClass1.createWithFieldsInline(rI, rL);
        long res = 0;
        res += testField1.hash();
        res += ((Object)testField2 == null) ? 42 : testField2.hash();
        res += val1.hash();
        res += testField4.hash();

        res += testField5.hash();
        res += ((Object)testField6 == null) ? 42 : testField6.hash();
        res += val2.hash();
        res += testField8.hash();
        return res;
    }

    @Run(test = "test1")
    public void test1_verifier() {
        testField1 = testValue1;
        testField2 = nullValField;
        testField3 = testValue1;
        testField4 = testValue1;

        testField5 = testValue1;
        testField6 = nullValField;
        testField7 = testValue1;
        testField8 = testValue1;
        long res = test1(true);
        Asserts.assertEquals(res, 2*42 + 6*testValue1.hash());

        testField2 = testValue1;
        testField6 = testValue1;
        res = test1(false);
        Asserts.assertEquals(res, 8*testValue1.hash());
    }

    // Test field stores
    @Test
    public MyValueClass1 test2(MyValueClass1 val1) {
        MyValueClass1 ret = MyValueClass1.createWithFieldsInline(rI, rL);
        MyValueClass1 val2 = MyValueClass1.setV4(testValue1, null);
        testField1 = testField4;
        testField2 = val1;
        testField3 = val2;

        testField5 = ret;
        testField6 = val1;
        testField7 = val2;
        testField8 = testField4;
        return ret;
    }

    @Run(test = "test2")
    public void test2_verifier() {
        testField4 = testValue1;
        MyValueClass1 ret = test2(null);
        MyValueClass1 val2 = MyValueClass1.setV4(testValue1, null);
        Asserts.assertEquals(testField1, testValue1);
        Asserts.assertEquals(testField2, null);
        Asserts.assertEquals(testField3, val2);

        Asserts.assertEquals(testField5, ret);
        Asserts.assertEquals(testField6, null);
        Asserts.assertEquals(testField7, val2);
        Asserts.assertEquals(testField8, testField4);

        testField4 = null;
        test2(null);
        Asserts.assertEquals(testField1, testField4);
        Asserts.assertEquals(testField8, testField4);
    }

    // Non-primitive Wrapper
    static class Test3Wrapper {
        MyValueClass1 val;

        public Test3Wrapper(MyValueClass1 val) {
            this.val = val;
        }
    }

    // Test scalarization in safepoint debug info and re-allocation on deopt
    @Test
    @IR(failOn = {ALLOC, STORE})
    public long test3(boolean deopt, boolean b1, boolean b2, Method m) {
        MyValueClass1 ret = MyValueClass1.createWithFieldsInline(rI, rL);
        if (b1) {
            ret = null;
        }
        if (b2) {
            ret = MyValueClass1.setV4(ret, null);
        }
        Test3Wrapper wrapper = new Test3Wrapper(ret);
        if (deopt) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        long res = ((Object)ret != null && (Object)ret.v4 != null) ? ret.hash() : 42;
        res += ((Object)wrapper.val != null && (Object)wrapper.val.v4 != null) ? wrapper.val.hash() : 0;
        return res;
    }

    @Run(test = "test3")
    public void test3_verifier(RunInfo info) {
        Asserts.assertEquals(test3(false, false, false, info.getTest()), 2*testValue1.hash());
        Asserts.assertEquals(test3(false, true, false, info.getTest()), 42L);
        if (!info.isWarmUp()) {
            switch (rI % 4) {
            case 0:
                Asserts.assertEquals(test3(true, false, false, info.getTest()), 2*testValue1.hash());
                break;
            case 1:
                Asserts.assertEquals(test3(true, true, false, info.getTest()), 42L);
                break;
            case 2:
                Asserts.assertEquals(test3(true, false, true, info.getTest()), 42L);
                break;
            case 3:
                try {
                    Asserts.assertEquals(test3(true, true, true, info.getTest()), 42L);
                    throw new RuntimeException("NullPointerException expected");
                } catch (NullPointerException e) {
                    // Expected
                }
                break;
            }
        }
    }

    // Test scalarization in safepoint debug info and re-allocation on deopt
    @Test
    @IR(failOn = {ALLOC, STORE})
    public boolean test4(boolean deopt, boolean b, Method m) {
        MyValueClass1 val = b ? null : MyValueClass1.createWithFieldsInline(rI, rL);
        Test3Wrapper wrapper = new Test3Wrapper(val);
        if (deopt) {
            // Uncommon trap
            TestFramework.deoptimize(m);
        }
        return (Object)wrapper.val == null;
    }

    @Run(test = "test4")
    public void test4_verifier(RunInfo info) {
        Asserts.assertTrue(test4(false, true, info.getTest()));
        Asserts.assertFalse(test4(false, false, info.getTest()));
        if (!info.isWarmUp()) {
            switch (rI % 2) {
                case 0:
                    Asserts.assertTrue(test4(true, true, info.getTest()));
                    break;
                case 1:
                    Asserts.assertFalse(test4(false, false, info.getTest()));
                    break;
            }
        }
    }

    static value class SmallNullable2 {
        float f1;
        double f2;
        public SmallNullable2() {
            f1 = (float)rL;
            f2 = (double)rL;
        }
    }

    static value class SmallNullable1 {
        char c;
        byte b;
        short s;
        int i;
        SmallNullable2 vt;

        public SmallNullable1(boolean useNull) {
            c = (char)rL;
            b = (byte)rL;
            s = (short)rL;
            i = (int)rL;
            vt = useNull ? null : new SmallNullable2();
        }
    }

    @DontCompile
    public SmallNullable1 test5_interpreted(boolean b1, boolean b2) {
        return b1 ? null : new SmallNullable1(b2);
    }

    @DontInline
    public SmallNullable1 test5_compiled(boolean b1, boolean b2) {
        return b1 ?null : new SmallNullable1(b2);
    }

    SmallNullable1 test5_field1;
    SmallNullable1 test5_field2;

    // Test scalarization in returns
    @Test
    public SmallNullable1 test5(boolean b1, boolean b2) {
        SmallNullable1 ret = test5_interpreted(b1, b2);
        if (b1 != ((Object)ret == null)) {
            throw new RuntimeException("test5 failed");
        }
        test5_field1 = ret;
        ret = test5_compiled(b1, b2);
        if (b1 != ((Object)ret == null)) {
            throw new RuntimeException("test5 failed");
        }
        test5_field2 = ret;
        return ret;
    }

    @Run(test = "test5")
    public void test5_verifier() {
        SmallNullable1 vt = new SmallNullable1(false);
        Asserts.assertEquals(test5(true, false), null);
        Asserts.assertEquals(test5_field1, null);
        Asserts.assertEquals(test5_field2, null);
        Asserts.assertEquals(test5(false, false), vt);
        Asserts.assertEquals(test5_field1, vt);
        Asserts.assertEquals(test5_field2, vt);
        vt = new SmallNullable1(true);
        Asserts.assertEquals(test5(true, true), null);
        Asserts.assertEquals(test5_field1, null);
        Asserts.assertEquals(test5_field2, null);
        Asserts.assertEquals(test5(false, true), vt);
        Asserts.assertEquals(test5_field1, vt);
        Asserts.assertEquals(test5_field2, vt);
    }

    static value class Empty2 {

    }

    static value class Empty1 {
        Empty2 empty2 = Empty2.default;
    }

    static value class Container {
        int x = 0;
        Empty1 empty1;
        Empty2 empty2 = Empty2.default;
        public Container(Empty1 val) {
            empty1 = val;
        }
    }

    @DontInline
    public static Empty1 test6_helper1(Empty1 vt) {
        return vt;
    }

    @DontInline
    public static Empty2 test6_helper2(Empty2 vt) {
        return vt;
    }

    @DontInline
    public static Container test6_helper3(Container vt) {
        return vt;
    }

    // Test scalarization in calls and returns with empty nullable inline types
    @Test
    public Empty1 test6(Empty1 vt) {
        Empty1 empty1 = test6_helper1(vt);
        test6_helper2((empty1 != null) ? empty1.empty2 : null);
        Container c = test6_helper3(new Container(empty1));
        return c.empty1;
    }

    @Run(test = "test6")
    @Warmup(10000) // Warmup to make sure helper methods are compiled as well
    public void test6_verifier() {
        Asserts.assertEQ(test6(Empty1.default), Empty1.default);
        Asserts.assertEQ(test6(null), null);
    }

    @DontCompile
    public void test7_helper2(boolean doit) {
        if (doit) {
            // uncommon trap
            try {
                TestFramework.deoptimize(getClass().getDeclaredMethod("test7", boolean.class, boolean.class, boolean.class));
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Test deoptimization at call return with inline type returned in registers
    @DontInline
    public SmallNullable1 test7_helper1(boolean deopt, boolean b1, boolean b2) {
        test7_helper2(deopt);
        return b1 ? null : new SmallNullable1(b2);
    }

    @Test
    public SmallNullable1 test7(boolean flag, boolean b1, boolean b2) {
        return test7_helper1(flag, b1, b2);
    }

    @Run(test = "test7")
    @Warmup(10000)
    public void test7_verifier(RunInfo info) {
        boolean b1 = ((rI % 3) == 0);
        boolean b2 = ((rI % 3) == 1);
        SmallNullable1 result = test7(!info.isWarmUp(), b1, b2);
        SmallNullable1 vt = new SmallNullable1(b2);
        Asserts.assertEQ(result, b1 ? null : vt);
    }

    // Test calling a method returning a nullable inline type as fields via reflection
    @Test
    public SmallNullable1 test8(boolean b1, boolean b2) {
        return b1 ? null : new SmallNullable1(b2);
    }

    @Run(test = "test8")
    @Warmup(1) // Make sure we call through runtime instead of generating bytecodes for reflective call
    public void test8_verifier() throws Exception {
        Method m = getClass().getDeclaredMethod("test8", boolean.class, boolean.class);
        Asserts.assertEQ(m.invoke(this, false, true), new SmallNullable1(true));
        Asserts.assertEQ(m.invoke(this, false, false), new SmallNullable1(false));
        Asserts.assertEQ(m.invoke(this, true, false), null);
    }

    // Test value classes as arg/return
    @Test
    public SmallNullable1 test9(MyValueClass1 vt1, MyValueClass1 vt2, boolean b1, boolean b2) {
        Asserts.assertEQ(vt1, testValue1);
        if (b1) {
            Asserts.assertEQ(vt2, null);
        } else {
            Asserts.assertEQ(vt2, testValue1);
        }
        return b1 ? null : new SmallNullable1(b2);
    }

    @Run(test = "test9")
    public void test9_verifier() {
        Asserts.assertEQ(test9(testValue1, testValue1, false, true), new SmallNullable1(true));
        Asserts.assertEQ(test9(testValue1, testValue1, false, false), new SmallNullable1(false));
        Asserts.assertEQ(test9(testValue1, null, true, false), null);
    }

    // Class.cast
    @Test
    public Object test10(Class c, MyValueClass1 vt) {
        return c.cast(vt);
    }

    @Run(test = "test10")
    public void test10_verifier() {
        Asserts.assertEQ(test10(MyValueClass1.class, testValue1), testValue1);
        Asserts.assertEQ(test10(MyValueClass1.class.asPrimaryType(), null), null);
        Asserts.assertEQ(test10(MyValueClass2.class.asPrimaryType(), null), null);
        Asserts.assertEQ(test10(Integer.class, null), null);
        try {
            test10(MyValueClass2.class.asPrimaryType(), testValue1);
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    // Test acmp
    @Test
    public boolean test12(MyValueClass1 vt1, MyValueClass1 vt2) {
        return vt1 == vt2;
    }

    @Run(test = "test12")
    public void test12_verifier() {
        Asserts.assertTrue(test12(testValue1, testValue1));
        Asserts.assertTrue(test12(null, null));
        Asserts.assertFalse(test12(testValue1, null));
        Asserts.assertFalse(test12(null, testValue1));
        Asserts.assertFalse(test12(testValue1, MyValueClass1.default));
    }

    // Same as test13 but with Object argument
    @Test
    public boolean test13(Object obj, MyValueClass1 vt2) {
        return obj == vt2;
    }

    @Run(test = "test13")
    public void test13_verifier() {
        Asserts.assertTrue(test13(testValue1, testValue1));
        Asserts.assertTrue(test13(null, null));
        Asserts.assertFalse(test13(testValue1, null));
        Asserts.assertFalse(test13(null, testValue1));
        Asserts.assertFalse(test13(testValue1, MyValueClass1.default));
    }

    static MyValueClass1 test14_field1;
    static MyValueClass1 test14_field2;

    // Test buffer checks emitted by acmp followed by buffering
    @Test
    public boolean test14(MyValueClass1 vt1, MyValueClass1 vt2) {
        // Trigger buffer checks
        if (vt1 != vt2) {
            throw new RuntimeException("Should be equal");
        }
        if (vt2 != vt1) {
            throw new RuntimeException("Should be equal");
        }
        // Trigger buffering
        test14_field1 = vt1;
        test14_field2 = vt2;
        return vt1 == null;
    }

    @Run(test = "test14")
    public void test14_verifier() {
        Asserts.assertFalse(test14(testValue1, testValue1));
        Asserts.assertTrue(test14(null, null));
    }

    @DontInline
    public MyValueClass1 test15_helper1(MyValueClass1 vt) {
        return vt;
    }

    @ForceInline
    public MyValueClass1 test15_helper2(MyValueClass1 vt) {
        return test15_helper1(vt);
    }

    @ForceInline
    public MyValueClass1 test15_helper3(Object vt) {
        return test15_helper2((MyValueClass1)vt);
    }

    // Test that calling convention optimization prevents buffering of arguments
    @Test
    @IR(applyIf = {"InlineTypePassFieldsAsArgs", "true"},
        counts = {ALLOC_G, " = 7"}) // 6 MyValueClass2/MyValueClass2Inline allocations + 1 Integer allocation
    @IR(applyIf = {"InlineTypePassFieldsAsArgs", "false"},
        counts = {ALLOC_G, " = 8"}) // 1 MyValueClass1 allocation, 6 MyValueClass2/MyValueClass2Inline allocations + 1 Integer allocation
    public MyValueClass1 test15(MyValueClass1 vt) {
        MyValueClass1 res = test15_helper1(vt);
        vt = MyValueClass1.createWithFieldsInline(rI, rL);
        test15_helper1(vt);
        test15_helper2(vt);
        test15_helper3(vt);
        return res;
    }

    @Run(test = "test15")
    public void test15_verifier() {
        Asserts.assertEQ(test15(testValue1), testValue1);
        Asserts.assertEQ(test15(null), null);
    }

    @DontInline
    public MyValueClass1 test16_helper1() {
        return MyValueClass1.createWithFieldsInline(rI, rL);
    }

    @ForceInline
    public MyValueClass1 test16_helper2() {
        return null;
    }

    @ForceInline
    public MyValueClass1 test16_helper3() {
        return MyValueClass1.createWithFieldsInline(rI, rL);
    }

    // Test that calling convention optimization prevents buffering of return values
    @Test
// TODO
//    @IR(applyIf = {"InlineTypeReturnedAsFields", "true"},
//        counts = {ALLOC_G, " = 7"}) // 6 MyValueClass2/MyValueClass2Inline allocations + 1 Integer allocation
//    @IR(applyIf = {"InlineTypeReturnedAsFields", "false"},
//        counts = {ALLOC_G, " = 8"}) // 1 MyValueClass1 allocation, 6 MyValueClass2/MyValueClass2Inline allocations + 1 Integer allocation
    public MyValueClass1 test16(int c) {
        MyValueClass1 res = null;
        if (c == 1) {
            res = test16_helper1();
        } else if (c == 2) {
            res = test16_helper2();
        } else if (c == 3) {
            res = test16_helper3();
        }
        return res;
    }

    @Run(test = "test16")
    public void test16_verifier() {
        Asserts.assertEQ(test16(0), null);
        Asserts.assertEQ(test16(1).hash(), testValue1.hash());
        Asserts.assertEQ(test16(2), null);
        Asserts.assertEQ(test16(3).hash(), testValue1.hash());
    }

// TODO but non-flattened fields should still be scalarized!!!
// TODO what about circularity? What about type not being loaded???
// TODO run all tests with AbortVMOnCompilationFailure
}
