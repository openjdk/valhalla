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
import test.java.lang.invoke.lib.InstructionHelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Arrays;

import static compiler.valhalla.inlinetypes.InlineTypes.IRNode.*;
import static compiler.valhalla.inlinetypes.InlineTypes.*;

/*
 * @test
 * @key randomness
 * @summary Test inline types in LWorld.
 * @library /test/lib /test/jdk/lib/testlibrary/bytecode /test/jdk/java/lang/invoke/common /
 * @requires (os.simpleArch == "x64" | os.simpleArch == "aarch64")
 * @build test.java.lang.invoke.lib.InstructionHelper
 * @run driver/timeout=450 compiler.valhalla.inlinetypes.TestLWorld
 */

@ForceCompileClassInitializer
public class TestLWorld {

    public static void main(String[] args) {
        // Make sure Test140Value is loaded but not linked
        Class<?> class1 = Test140Value.class;
        // Make sure Test141Value is linked but not initialized
        Class<?> class2 = Test141Value.class;
        class2.getDeclaredFields();

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

    // Helper methods

    private static final MyValue1 testValue1 = MyValue1.createWithFieldsInline(rI, rL);
    private static final MyValue2 testValue2 = MyValue2.createWithFieldsInline(rI, rD);

    protected long hash() {
        return testValue1.hash();
    }

    // Test passing an inline type as an Object
    @DontInline
    public Object test1_dontinline1(Object o) {
        return o;
    }

    @DontInline
    public MyValue1 test1_dontinline2(Object o) {
        return (MyValue1)o;
    }

    @ForceInline
    public Object test1_inline1(Object o) {
        return o;
    }

    @ForceInline
    public MyValue1 test1_inline2(Object o) {
        return (MyValue1)o;
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public MyValue1 test1() {
        MyValue1 vt = testValue1;
        vt = (MyValue1)test1_dontinline1(vt);
        vt =           test1_dontinline2(vt);
        vt = (MyValue1)test1_inline1(vt);
        vt =           test1_inline2(vt);
        return vt;
    }

    @Run(test = "test1")
    public void test1_verifier() {
        Asserts.assertEQ(test1().hash(), hash());
    }

    // Test storing/loading inline types to/from Object and inline type fields
    Object objectField1 = null;
    Object objectField2 = null;
    Object objectField3 = null;
    Object objectField4 = null;
    Object objectField5 = null;
    Object objectField6 = null;

    MyValue1 valueField1 = testValue1;
    MyValue1 valueField2 = testValue1;
    MyValue1.ref valueField3 = testValue1;
    MyValue1 valueField4;
    MyValue1.ref valueField5;

    static MyValue1.ref staticValueField1 = testValue1;
    static MyValue1 staticValueField2 = testValue1;
    static MyValue1 staticValueField3;
    static MyValue1.ref staticValueField4;

    @DontInline
    public Object readValueField5() {
        return (Object)valueField5;
    }

    @DontInline
    public Object readStaticValueField4() {
        return (Object)staticValueField4;
    }

    @Test
    public long test2(MyValue1 vt1, Object vt2) {
        objectField1 = vt1;
        objectField2 = (MyValue1)vt2;
        objectField3 = testValue1;
        objectField4 = MyValue1.createWithFieldsDontInline(rI, rL);
        objectField5 = valueField1;
        objectField6 = valueField3;
        valueField1 = (MyValue1)objectField1;
        valueField2 = (MyValue1)vt2;
        valueField3 = (MyValue1)vt2;
        staticValueField1 = (MyValue1)objectField1;
        staticValueField2 = (MyValue1)vt1;
        // Don't inline these methods because reading NULL will trigger a deoptimization
        if (readValueField5() != null || readStaticValueField4() != null) {
            throw new RuntimeException("Should be null");
        }
        return ((MyValue1)objectField1).hash() + ((MyValue1)objectField2).hash() +
               ((MyValue1)objectField3).hash() + ((MyValue1)objectField4).hash() +
               ((MyValue1)objectField5).hash() + ((MyValue1)objectField6).hash() +
                valueField1.hash() + valueField2.hash() + valueField3.hash() + valueField4.hashPrimitive() +
                staticValueField1.hash() + staticValueField2.hash() + staticValueField3.hashPrimitive();
    }

    @Run(test = "test2")
    public void test2_verifier() {
        MyValue1 vt = testValue1;
        MyValue1 def = MyValue1.createDefaultDontInline();
        long result = test2(vt, vt);
        Asserts.assertEQ(result, 11*vt.hash() + 2*def.hashPrimitive());
    }

    // Test merging inline types and objects
    @Test
    public Object test3(int state) {
        Object res = null;
        if (state == 0) {
            res = Integer.valueOf(rI);
        } else if (state == 1) {
            res = MyValue1.createWithFieldsInline(rI, rL);
        } else if (state == 2) {
            res = MyValue1.createWithFieldsDontInline(rI, rL);
        } else if (state == 3) {
            res = (MyValue1)objectField1;
        } else if (state == 4) {
            res = valueField1;
        } else if (state == 5) {
            res = null;
        } else if (state == 6) {
            res = MyValue2.createWithFieldsInline(rI, rD);
        } else if (state == 7) {
            res = testValue2;
        }
        return res;
    }

    @Run(test = "test3")
    public void test3_verifier() {
        objectField1 = valueField1;
        Object result = null;
        result = test3(0);
        Asserts.assertEQ((Integer)result, rI);
        result = test3(1);
        Asserts.assertEQ(((MyValue1)result).hash(), hash());
        result = test3(2);
        Asserts.assertEQ(((MyValue1)result).hash(), hash());
        result = test3(3);
        Asserts.assertEQ(((MyValue1)result).hash(), hash());
        result = test3(4);
        Asserts.assertEQ(((MyValue1)result).hash(), hash());
        result = test3(5);
        Asserts.assertEQ(result, null);
        result = test3(6);
        Asserts.assertEQ(((MyValue2)result).hash(), testValue2.hash());
        result = test3(7);
        Asserts.assertEQ(((MyValue2)result).hash(), testValue2.hash());
    }

    // Test merging inline types and objects in loops
    @Test
    public Object test4(int iters) {
        Object res = Integer.valueOf(rI);
        for (int i = 0; i < iters; ++i) {
            if (res instanceof Integer) {
                res = MyValue1.createWithFieldsInline(rI, rL);
            } else {
                res = MyValue1.createWithFieldsInline(((MyValue1)res).x + 1, rL);
            }
        }
        return res;
    }

    @Run(test = "test4")
    public void test4_verifier() {
        Integer result1 = (Integer)test4(0);
        Asserts.assertEQ(result1, rI);
        int iters = (Math.abs(rI) % 10) + 1;
        MyValue1 result2 = (MyValue1)test4(iters);
        MyValue1 vt = MyValue1.createWithFieldsInline(rI + iters - 1, rL);
        Asserts.assertEQ(result2.hash(), vt.hash());
    }

    // Test inline types in object variables that are live at safepoint
    @Test
    @IR(failOn = {ALLOC, STORE, LOOP})
    public long test5(MyValue1 arg, boolean deopt, Method m) {
        Object vt1 = MyValue1.createWithFieldsInline(rI, rL);
        Object vt2 = MyValue1.createWithFieldsDontInline(rI, rL);
        Object vt3 = arg;
        Object vt4 = valueField1;
        if (deopt) {
            // uncommon trap
            TestFramework.deoptimize(m);
        }
        return ((MyValue1)vt1).hash() + ((MyValue1)vt2).hash() +
               ((MyValue1)vt3).hash() + ((MyValue1)vt4).hash();
    }

    @Run(test = "test5")
    public void test5_verifier(RunInfo info) {
        long result = test5(valueField1, !info.isWarmUp(), info.getTest());
        Asserts.assertEQ(result, 4*hash());
    }

    // Test comparing inline types with objects
    @Test
    public boolean test6(Object arg) {
        Object vt = MyValue1.createWithFieldsInline(rI, rL);
        if (vt == arg || vt == (Object)valueField1 || vt == objectField1 || vt == null ||
            arg == vt || (Object)valueField1 == vt || objectField1 == vt || null == vt) {
            return true;
        }
        return false;
    }

    @Run(test = "test6")
    public void test6_verifier() {
        boolean result = test6(null);
        Asserts.assertFalse(result);
    }

    // merge of inline type and non-inline type
    @Test
    public Object test7(boolean flag) {
        Object res = null;
        if (flag) {
            res = valueField1;
        } else {
            res = objectField1;
        }
        return res;
    }

    @Run(test = "test7")
    public void test7_verifier() {
        test7(true);
        test7(false);
    }

    @Test
    public Object test8(boolean flag) {
        Object res = null;
        if (flag) {
            res = objectField1;
        } else {
            res = valueField1;
        }
        return res;
    }

    @Run(test = "test8")
    public void test8_verifier() {
        test8(true);
        test8(false);
    }

    // merge of inline types in a loop, stored in an object local
    @Test
    public Object test9() {
        Object o = valueField1;
        for (int i = 1; i < 100; i *= 2) {
            MyValue1 v = (MyValue1)o;
            o = MyValue1.setX(v, v.x + 1);
        }
        return o;
    }

    @Run(test = "test9")
    public void test9_verifier() {
        test9();
    }

    // merge of inline types in an object local
    @ForceInline
    public Object test10_helper() {
        return valueField1;
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public void test10(boolean flag) {
        Object o = null;
        if (flag) {
            o = valueField1;
        } else {
            o = test10_helper();
        }
        valueField1 = (MyValue1)o;
    }

    @Run(test = "test10")
    public void test10_verifier() {
        test10(true);
        test10(false);
    }

    // Interface tests

    @DontInline
    public MyInterface test11_dontinline1(MyInterface o) {
        return o;
    }

    @DontInline
    public MyValue1 test11_dontinline2(MyInterface o) {
        return (MyValue1)o;
    }

    @ForceInline
    public MyInterface test11_inline1(MyInterface o) {
        return o;
    }

    @ForceInline
    public MyValue1 test11_inline2(MyInterface o) {
        return (MyValue1)o;
    }

    @Test
    public MyValue1 test11() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        vt = (MyValue1)test11_dontinline1(vt);
        vt =           test11_dontinline2(vt);
        vt = (MyValue1)test11_inline1(vt);
        vt =           test11_inline2(vt);
        return vt;
    }

    @Run(test = "test11")
    public void test11_verifier() {
        Asserts.assertEQ(test11().hash(), hash());
    }

    // Test storing/loading inline types to/from interface and inline type fields
    MyInterface interfaceField1 = null;
    MyInterface interfaceField2 = null;
    MyInterface interfaceField3 = null;
    MyInterface interfaceField4 = null;
    MyInterface interfaceField5 = null;
    MyInterface interfaceField6 = null;

    @DontInline
    public MyInterface readValueField5AsInterface() {
        return (MyInterface)valueField5;
    }

    @DontInline
    public MyInterface readStaticValueField4AsInterface() {
        return (MyInterface)staticValueField4;
    }

    @Test
    public long test12(MyValue1 vt1, MyInterface vt2) {
        interfaceField1 = vt1;
        interfaceField2 = (MyValue1)vt2;
        interfaceField3 = MyValue1.createWithFieldsInline(rI, rL);
        interfaceField4 = MyValue1.createWithFieldsDontInline(rI, rL);
        interfaceField5 = valueField1;
        interfaceField6 = valueField3;
        valueField1 = (MyValue1)interfaceField1;
        valueField2 = (MyValue1)vt2;
        valueField3 = (MyValue1)vt2;
        staticValueField1 = (MyValue1)interfaceField1;
        staticValueField2 = (MyValue1)vt1;
        // Don't inline these methods because reading NULL will trigger a deoptimization
        if (readValueField5AsInterface() != null || readStaticValueField4AsInterface() != null) {
            throw new RuntimeException("Should be null");
        }
        return ((MyValue1)interfaceField1).hash() + ((MyValue1)interfaceField2).hash() +
               ((MyValue1)interfaceField3).hash() + ((MyValue1)interfaceField4).hash() +
               ((MyValue1)interfaceField5).hash() + ((MyValue1)interfaceField6).hash() +
                valueField1.hash() + valueField2.hash() + valueField3.hash() + valueField4.hashPrimitive() +
                staticValueField1.hash() + staticValueField2.hash() + staticValueField3.hashPrimitive();
    }

    @Run(test = "test12")
    public void test12_verifier() {
        MyValue1 vt = testValue1;
        MyValue1 def = MyValue1.createDefaultDontInline();
        long result = test12(vt, vt);
        Asserts.assertEQ(result, 11*vt.hash() + 2*def.hashPrimitive());
    }

    class MyObject1 implements MyInterface {
        public int x;

        public MyObject1(int x) {
            this.x = x;
        }

        @ForceInline
        public long hash() {
            return x;
        }
    }

    // Test merging inline types and interfaces
    @Test
    public MyInterface test13(int state) {
        MyInterface res = null;
        if (state == 0) {
            res = new MyObject1(rI);
        } else if (state == 1) {
            res = MyValue1.createWithFieldsInline(rI, rL);
        } else if (state == 2) {
            res = MyValue1.createWithFieldsDontInline(rI, rL);
        } else if (state == 3) {
            res = (MyValue1)objectField1;
        } else if (state == 4) {
            res = valueField1;
        } else if (state == 5) {
            res = null;
        }
        return res;
    }

    @Run(test = "test13")
    public void test13_verifier() {
        objectField1 = valueField1;
        MyInterface result = null;
        result = test13(0);
        Asserts.assertEQ(((MyObject1)result).x, rI);
        result = test13(1);
        Asserts.assertEQ(((MyValue1)result).hash(), hash());
        result = test13(2);
        Asserts.assertEQ(((MyValue1)result).hash(), hash());
        result = test13(3);
        Asserts.assertEQ(((MyValue1)result).hash(), hash());
        result = test13(4);
        Asserts.assertEQ(((MyValue1)result).hash(), hash());
        result = test13(5);
        Asserts.assertEQ(result, null);
    }

    // Test merging inline types and interfaces in loops
    @Test
    public MyInterface test14(int iters) {
        MyInterface res = new MyObject1(rI);
        for (int i = 0; i < iters; ++i) {
            if (res instanceof MyObject1) {
                res = MyValue1.createWithFieldsInline(rI, rL);
            } else {
                res = MyValue1.createWithFieldsInline(((MyValue1)res).x + 1, rL);
            }
        }
        return res;
    }

    @Run(test = "test14")
    public void test14_verifier() {
        MyObject1 result1 = (MyObject1)test14(0);
        Asserts.assertEQ(result1.x, rI);
        int iters = (Math.abs(rI) % 10) + 1;
        MyValue1 result2 = (MyValue1)test14(iters);
        MyValue1 vt = MyValue1.createWithFieldsInline(rI + iters - 1, rL);
        Asserts.assertEQ(result2.hash(), vt.hash());
    }

    // Test inline types in interface variables that are live at safepoint
    @Test
    @IR(failOn = {ALLOC, STORE, LOOP})
    public long test15(MyValue1 arg, boolean deopt, Method m) {
        MyInterface vt1 = MyValue1.createWithFieldsInline(rI, rL);
        MyInterface vt2 = MyValue1.createWithFieldsDontInline(rI, rL);
        MyInterface vt3 = arg;
        MyInterface vt4 = valueField1;
        if (deopt) {
            // uncommon trap
            TestFramework.deoptimize(m);
        }
        return ((MyValue1)vt1).hash() + ((MyValue1)vt2).hash() +
               ((MyValue1)vt3).hash() + ((MyValue1)vt4).hash();
    }

    @Run(test = "test15")
    public void test15_verifier(RunInfo info) {
        long result = test15(valueField1, !info.isWarmUp(), info.getTest());
        Asserts.assertEQ(result, 4*hash());
    }

    // Test comparing inline types with interfaces
    @Test
    public boolean test16(Object arg) {
        MyInterface vt = MyValue1.createWithFieldsInline(rI, rL);
        if (vt == arg || vt == (MyInterface)valueField1 || vt == interfaceField1 || vt == null ||
            arg == vt || (MyInterface)valueField1 == vt || interfaceField1 == vt || null == vt) {
            return true;
        }
        return false;
    }

    @Run(test = "test16")
    public void test16_verifier() {
        boolean result = test16(null);
        Asserts.assertFalse(result);
    }

    // Test subtype check when casting to inline type
    @Test
    @IR(failOn = {ALLOC_G})
    public MyValue1 test17(MyValue1 vt, Object obj) {
        try {
            vt = (MyValue1)obj;
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException e) {
            // Expected
        }
        return vt;
    }

    @Run(test = "test17")
    public void test17_verifier() {
        MyValue1 vt = testValue1;
        MyValue1 result = test17(vt, Integer.valueOf(rI));
        Asserts.assertEquals(result.hash(), vt.hash());
    }

    @Test
    public MyValue1 test18(MyValue1 vt) {
        Object obj = vt;
        vt = (MyValue1)obj;
        return vt;
    }

    @Run(test = "test18")
    public void test18_verifier() {
        MyValue1 vt = testValue1;
        MyValue1 result = test18(vt);
        Asserts.assertEquals(result.hash(), vt.hash());
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public void test19(MyValue1 vt) {
        Object obj = vt;
        try {
            MyValue2 vt2 = (MyValue2)obj;
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    @Run(test = "test19")
    public void test19_verifier() {
        test19(valueField1);
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public void test20(MyValue1 vt) {
        Object obj = vt;
        try {
            Integer i = (Integer)obj;
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    @Run(test = "test20")
    public void test20_verifier() {
        test20(valueField1);
    }

    // Array tests

    private static final MyValue1[] testValue1Array = new MyValue1[] {testValue1,
                                                                      testValue1,
                                                                      testValue1};

    private static final MyValue1[][] testValue1Array2 = new MyValue1[][] {testValue1Array,
                                                                           testValue1Array,
                                                                           testValue1Array};

    private static final MyValue2[] testValue2Array = new MyValue2[] {testValue2,
                                                                      testValue2,
                                                                      testValue2};

    private static final Integer[] testIntegerArray = new Integer[42];

    // Test load from (flattened) inline type array disguised as object array
    @Test
    public Object test21(Object[] oa, int index) {
        return oa[index];
    }

    @Run(test = "test21")
    public void test21_verifier() {
        MyValue1 result = (MyValue1)test21(testValue1Array, Math.abs(rI) % 3);
        Asserts.assertEQ(result.hash(), hash());
    }

    // Test load from (flattened) inline type array disguised as interface array
    @Test
    public Object test22Interface(MyInterface[] ia, int index) {
        return ia[index];
    }

    @Run(test = "test22Interface")
    public void test22Interface_verifier() {
        MyValue1 result = (MyValue1)test22Interface(testValue1Array, Math.abs(rI) % 3);
        Asserts.assertEQ(result.hash(), hash());
    }

    // Test load from (flattened) inline type array disguised as abstract array
    @Test
    public Object test22Abstract(MyAbstract[] ia, int index) {
        return ia[index];
    }

    @Run(test = "test22Abstract")
    public void test22Abstract_verifier() {
        MyValue1 result = (MyValue1)test22Abstract(testValue1Array, Math.abs(rI) % 3);
        Asserts.assertEQ(result.hash(), hash());
    }

    // Test inline store to (flattened) inline type array disguised as object array
    @ForceInline
    public void test23_inline(Object[] oa, Object o, int index) {
        oa[index] = o;
    }

    @Test
    public void test23(Object[] oa, MyValue1 vt, int index) {
        test23_inline(oa, vt, index);
    }

    @Run(test = "test23")
    public void test23_verifier() {
        int index = Math.abs(rI) % 3;
        MyValue1 vt = MyValue1.createWithFieldsInline(rI + 1, rL + 1);
        test23(testValue1Array, vt, index);
        Asserts.assertEQ(testValue1Array[index].hash(), vt.hash());
        testValue1Array[index] = testValue1;
        try {
            test23(testValue2Array, vt, index);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
        Asserts.assertEQ(testValue2Array[index].hash(), testValue2.hash());
    }

    @ForceInline
    public void test24_inline(Object[] oa, Object o, int index) {
        oa[index] = o;
    }

    @Test
    public void test24(Object[] oa, MyValue1 vt, int index) {
        test24_inline(oa, vt, index);
    }

    @Run(test = "test24")
    public void test24_verifier() {
        int index = Math.abs(rI) % 3;
        try {
            test24(testIntegerArray, testValue1, index);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
    }

    @ForceInline
    public void test25_inline(Object[] oa, Object o, int index) {
        oa[index] = o;
    }

    @Test
    public void test25(Object[] oa, MyValue1 vt, int index) {
        test25_inline(oa, vt, index);
    }

    @Run(test = "test25")
    public void test25_verifier() {
        int index = Math.abs(rI) % 3;
        try {
            test25(null, testValue1, index);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    // Test inline store to (flattened) inline type array disguised as interface array
    @ForceInline
    public void test26Interface_inline(MyInterface[] ia, MyInterface i, int index) {
        ia[index] = i;
    }

    @Test
    public void test26Interface(MyInterface[] ia, MyValue1 vt, int index) {
      test26Interface_inline(ia, vt, index);
    }

    @Run(test = "test26Interface")
    public void test26Interface_verifier() {
        int index = Math.abs(rI) % 3;
        MyValue1 vt = MyValue1.createWithFieldsInline(rI + 1, rL + 1);
        test26Interface(testValue1Array, vt, index);
        Asserts.assertEQ(testValue1Array[index].hash(), vt.hash());
        testValue1Array[index] = testValue1;
        try {
            test26Interface(testValue2Array, vt, index);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
        Asserts.assertEQ(testValue2Array[index].hash(), testValue2.hash());
    }

    @ForceInline
    public void test27Interface_inline(MyInterface[] ia, MyInterface i, int index) {
        ia[index] = i;
    }

    @Test
    public void test27Interface(MyInterface[] ia, MyValue1 vt, int index) {
        test27Interface_inline(ia, vt, index);
    }

    @Run(test = "test27Interface")
    public void test27Interface_verifier() {
        int index = Math.abs(rI) % 3;
        try {
            test27Interface(null, testValue1, index);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    // Test inline store to (flattened) inline type array disguised as abstract array
    @ForceInline
    public void test26Abstract_inline(MyAbstract[] ia, MyAbstract i, int index) {
        ia[index] = i;
    }

    @Test
    public void test26Abstract(MyAbstract[] ia, MyValue1 vt, int index) {
      test26Abstract_inline(ia, vt, index);
    }

    @Run(test = "test26Abstract")
    public void test26Abstract_verifier() {
        int index = Math.abs(rI) % 3;
        MyValue1 vt = MyValue1.createWithFieldsInline(rI + 1, rL + 1);
        test26Abstract(testValue1Array, vt, index);
        Asserts.assertEQ(testValue1Array[index].hash(), vt.hash());
        testValue1Array[index] = testValue1;
        try {
            test26Abstract(testValue2Array, vt, index);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
        Asserts.assertEQ(testValue2Array[index].hash(), testValue2.hash());
    }

    @ForceInline
    public void test27Abstract_inline(MyAbstract[] ia, MyAbstract i, int index) {
        ia[index] = i;
    }

    @Test
    public void test27Abstract(MyAbstract[] ia, MyValue1 vt, int index) {
        test27Abstract_inline(ia, vt, index);
    }

    @Run(test = "test27Abstract")
    public void test27Abstract_verifier() {
        int index = Math.abs(rI) % 3;
        try {
            test27Abstract(null, testValue1, index);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    // Test object store to (flattened) inline type array disguised as object array
    @ForceInline
    public void test28_inline(Object[] oa, Object o, int index) {
        oa[index] = o;
    }

    @Test
    public void test28(Object[] oa, Object o, int index) {
        test28_inline(oa, o, index);
    }

    @Run(test = "test28")
    public void test28_verifier() {
        int index = Math.abs(rI) % 3;
        MyValue1 vt1 = MyValue1.createWithFieldsInline(rI + 1, rL + 1);
        test28(testValue1Array, vt1, index);
        Asserts.assertEQ(testValue1Array[index].hash(), vt1.hash());
        try {
            test28(testValue1Array, testValue2, index);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
        Asserts.assertEQ(testValue1Array[index].hash(), vt1.hash());
        testValue1Array[index] = testValue1;
    }

    @ForceInline
    public void test29_inline(Object[] oa, Object o, int index) {
        oa[index] = o;
    }

    @Test
    public void test29(Object[] oa, Object o, int index) {
        test29_inline(oa, o, index);
    }

    @Run(test = "test29")
    public void test29_verifier() {
        int index = Math.abs(rI) % 3;
        try {
            test29(testValue2Array, testValue1, index);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
        Asserts.assertEQ(testValue2Array[index].hash(), testValue2.hash());
    }

    @ForceInline
    public void test30_inline(Object[] oa, Object o, int index) {
        oa[index] = o;
    }

    @Test
    public void test30(Object[] oa, Object o, int index) {
        test30_inline(oa, o, index);
    }

    @Run(test = "test30")
    public void test30_verifier() {
        int index = Math.abs(rI) % 3;
        try {
            test30(testIntegerArray, testValue1, index);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
    }

    // Test inline store to (flattened) inline type array disguised as interface array
    @ForceInline
    public void test31Interface_inline(MyInterface[] ia, MyInterface i, int index) {
        ia[index] = i;
    }

    @Test
    public void test31Interface(MyInterface[] ia, MyInterface i, int index) {
        test31Interface_inline(ia, i, index);
    }

    @Run(test = "test31Interface")
    public void test31Interface_verifier() {
        int index = Math.abs(rI) % 3;
        MyValue1 vt1 = MyValue1.createWithFieldsInline(rI + 1, rL + 1);
        test31Interface(testValue1Array, vt1, index);
        Asserts.assertEQ(testValue1Array[index].hash(), vt1.hash());
        try {
            test31Interface(testValue1Array, testValue2, index);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
        Asserts.assertEQ(testValue1Array[index].hash(), vt1.hash());
        testValue1Array[index] = testValue1;
    }

    @ForceInline
    public void test32Interface_inline(MyInterface[] ia, MyInterface i, int index) {
        ia[index] = i;
    }

    @Test
    public void test32Interface(MyInterface[] ia, MyInterface i, int index) {
        test32Interface_inline(ia, i, index);
    }

    @Run(test = "test32Interface")
    public void test32Interface_verifier() {
        int index = Math.abs(rI) % 3;
        try {
            test32Interface(testValue2Array, testValue1, index);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
    }

    // Test inline store to (flattened) inline type array disguised as abstract array
    @ForceInline
    public void test31Abstract_inline(MyAbstract[] ia, MyAbstract i, int index) {
        ia[index] = i;
    }

    @Test
    public void test31Abstract(MyAbstract[] ia, MyAbstract i, int index) {
        test31Abstract_inline(ia, i, index);
    }

    @Run(test = "test31Abstract")
    public void test31Abstract_verifier() {
        int index = Math.abs(rI) % 3;
        MyValue1 vt1 = MyValue1.createWithFieldsInline(rI + 1, rL + 1);
        test31Abstract(testValue1Array, vt1, index);
        Asserts.assertEQ(testValue1Array[index].hash(), vt1.hash());
        try {
            test31Abstract(testValue1Array, testValue2, index);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
        Asserts.assertEQ(testValue1Array[index].hash(), vt1.hash());
        testValue1Array[index] = testValue1;
    }

    @ForceInline
    public void test32Abstract_inline(MyAbstract[] ia, MyAbstract i, int index) {
        ia[index] = i;
    }

    @Test
    public void test32Abstract(MyAbstract[] ia, MyAbstract i, int index) {
        test32Abstract_inline(ia, i, index);
    }

    @Run(test = "test32Abstract")
    public void test32Abstract_verifier() {
        int index = Math.abs(rI) % 3;
        try {
            test32Abstract(testValue2Array, testValue1, index);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
    }

    // Test writing null to a (flattened) inline type array disguised as object array
    @ForceInline
    public void test33_inline(Object[] oa, Object o, int index) {
        oa[index] = o;
    }

    @Test
    public void test33(Object[] oa, Object o, int index) {
        test33_inline(oa, o, index);
    }

    @Run(test = "test33")
    public void test33_verifier() {
        int index = Math.abs(rI) % 3;
        try {
            test33(testValue1Array, null, index);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        Asserts.assertEQ(testValue1Array[index].hash(), hash());
    }

    // Test writing constant null to a (flattened) inline type array disguised as object array

    @ForceInline
    public void test34_inline(Object[] oa, Object o, int index) {
        oa[index] = o;
    }

    @Test
    public void test34(Object[] oa, int index) {
        test34_inline(oa, null, index);
    }

    @Run(test = "test34")
    public void test34_verifier() {
        int index = Math.abs(rI) % 3;
        try {
            test34(testValue1Array, index);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        Asserts.assertEQ(testValue1Array[index].hash(), hash());
    }

    // Test writing constant null to a (flattened) inline type array

    private static final MethodHandle setArrayElementNull = InstructionHelper.loadCode(MethodHandles.lookup(),
        "setArrayElementNull",
        MethodType.methodType(void.class, TestLWorld.class, MyValue1[].class, int.class),
        CODE -> {
            CODE.
            aload_1().
            iload_2().
            aconst_null().
            aastore().
            return_();
        });

    @Test
    @IR(failOn = {ALLOC_G})
    public void test35(MyValue1[] va, int index) throws Throwable {
        setArrayElementNull.invoke(this, va, index);
    }

    @Run(test = "test35")
    @Warmup(10000)
    public void test35_verifier() throws Throwable {
        int index = Math.abs(rI) % 3;
        try {
            test35(testValue1Array, index);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        Asserts.assertEQ(testValue1Array[index].hash(), hash());
    }

    // Test writing an inline type to a null inline type array
    @Test
    @IR(applyIfAnd = {"UseG1GC", "true", "FlatArrayElementMaxSize", "= -1"},
        failOn = {ALLOC_G})
    public void test36(MyValue1[] va, MyValue1 vt, int index) {
        va[index] = vt;
    }

    @Run(test = "test36")
    public void test36_verifier() {
        int index = Math.abs(rI) % 3;
        try {
            test36(null, testValue1Array[index], index);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    // Test incremental inlining
    @ForceInline
    public void test37_inline(Object[] oa, Object o, int index) {
        oa[index] = o;
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public void test37(MyValue1[] va, Object o, int index) {
        test37_inline(va, o, index);
    }

    @Run(test = "test37")
    public void test37_verifier() {
        int index = Math.abs(rI) % 3;
        MyValue1 vt1 = MyValue1.createWithFieldsInline(rI + 1, rL + 1);
        test37(testValue1Array, vt1, index);
        Asserts.assertEQ(testValue1Array[index].hash(), vt1.hash());
        try {
            test37(testValue1Array, testValue2, index);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
        Asserts.assertEQ(testValue1Array[index].hash(), vt1.hash());
        testValue1Array[index] = testValue1;
    }

    // Test merging of inline type arrays

    @ForceInline
    public Object[] test38_inline() {
        return new MyValue1[42];
    }

    @Test
    @IR(failOn = {ALLOC})
    public Object[] test38(Object[] oa, Object o, int i1, int i2, int num) {
        Object[] result = null;
        switch (num) {
        case 0:
            result = test38_inline();
            break;
        case 1:
            result = oa;
            break;
        case 2:
            result = testValue1Array;
            break;
        case 3:
            result = testValue2Array;
            break;
        case 4:
            result = testIntegerArray;
            break;
        case 5:
            result = null;
            break;
        case 6:
            result = testValue1Array2;
            break;
        }
        result[i1] = result[i2];
        result[i2] = o;
        return result;
    }

    @Run(test = "test38")
    public void test38_verifier() {
        int index = Math.abs(rI) % 3;
        MyValue1[] va = new MyValue1[42];
        Object[] result = test38(null, testValue1, index, index, 0);
        Asserts.assertEQ(((MyValue1)result[index]).hash(), testValue1.hash());
        result = test38(testValue1Array, testValue1, index, index, 1);
        Asserts.assertEQ(((MyValue1)result[index]).hash(), testValue1.hash());
        result = test38(null, testValue1, index, index, 2);
        Asserts.assertEQ(((MyValue1)result[index]).hash(), testValue1.hash());
        result = test38(null, testValue2, index, index, 3);
        Asserts.assertEQ(((MyValue2)result[index]).hash(), testValue2.hash());
        try {
            result = test38(null, null, index, index, 3);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        result = test38(null, null, index, index, 4);
        try {
            result = test38(null, testValue1, index, index, 4);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
        try {
            result = test38(null, testValue1, index, index, 5);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        result = test38(null, testValue1Array, index, index, 6);
        Asserts.assertEQ(((MyValue1[][])result)[index][index].hash(), testValue1.hash());
    }

    @ForceInline
    public Object test39_inline() {
        return new MyValue1[42];
    }

    // Same as above but merging into Object instead of Object[]
    @Test
    public Object test39(Object oa, Object o, int i1, int i2, int num) {
        Object result = null;
        switch (num) {
        case 0:
            result = test39_inline();
            break;
        case 1:
            result = oa;
            break;
        case 2:
            result = testValue1Array;
            break;
        case 3:
            result = testValue2Array;
            break;
        case 4:
            result = testIntegerArray;
            break;
        case 5:
            result = null;
            break;
        case 6:
            result = testValue1;
            break;
        case 7:
            result = testValue2;
            break;
        case 8:
            result = MyValue1.createWithFieldsInline(rI, rL);
            break;
        case 9:
            result = Integer.valueOf(42);
            break;
        case 10:
            result = testValue1Array2;
            break;
        }
        if (result instanceof Object[]) {
            ((Object[])result)[i1] = ((Object[])result)[i2];
            ((Object[])result)[i2] = o;
        }
        return result;
    }

    @Run(test = "test39")
    public void test39_verifier() {
        int index = Math.abs(rI) % 3;
        MyValue1[] va = new MyValue1[42];
        Object result = test39(null, testValue1, index, index, 0);
        Asserts.assertEQ(((MyValue1[])result)[index].hash(), testValue1.hash());
        result = test39(testValue1Array, testValue1, index, index, 1);
        Asserts.assertEQ(((MyValue1[])result)[index].hash(), testValue1.hash());
        result = test39(null, testValue1, index, index, 2);
        Asserts.assertEQ(((MyValue1[])result)[index].hash(), testValue1.hash());
        result = test39(null, testValue2, index, index, 3);
        Asserts.assertEQ(((MyValue2[])result)[index].hash(), testValue2.hash());
        try {
            result = test39(null, null, index, index, 3);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
        result = test39(null, null, index, index, 4);
        try {
            result = test39(null, testValue1, index, index, 4);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
        result = test39(null, testValue1, index, index, 5);
        Asserts.assertEQ(result, null);
        result = test39(null, testValue1, index, index, 6);
        Asserts.assertEQ(((MyValue1)result).hash(), testValue1.hash());
        result = test39(null, testValue1, index, index, 7);
        Asserts.assertEQ(((MyValue2)result).hash(), testValue2.hash());
        result = test39(null, testValue1, index, index, 8);
        Asserts.assertEQ(((MyValue1)result).hash(), testValue1.hash());
        result = test39(null, testValue1, index, index, 9);
        Asserts.assertEQ(((Integer)result), 42);
        result = test39(null, testValue1Array, index, index, 10);
        Asserts.assertEQ(((MyValue1[][])result)[index][index].hash(), testValue1.hash());
    }

    // Test instanceof with inline types and arrays
    @Test
    @IR(applyIf = {"InlineTypePassFieldsAsArgs", "true"},
        failOn = {ALLOC_G})
    public long test40(Object o, int index) {
        if (o instanceof MyValue1) {
          return ((MyValue1)o).hashInterpreted();
        } else if (o instanceof MyValue1[]) {
          return ((MyValue1[])o)[index].hashInterpreted();
        } else if (o instanceof MyValue2) {
          return ((MyValue2)o).hash();
        } else if (o instanceof MyValue2[]) {
          return ((MyValue2[])o)[index].hash();
        } else if (o instanceof MyValue1[][]) {
          return ((MyValue1[][])o)[index][index].hash();
        } else if (o instanceof Long) {
          return (long)o;
        }
        return 0;
    }

    @Run(test = "test40")
    public void test40_verifier() {
        int index = Math.abs(rI) % 3;
        long result = test40(testValue1, 0);
        Asserts.assertEQ(result, testValue1.hash());
        result = test40(testValue1Array, index);
        Asserts.assertEQ(result, testValue1.hash());
        result = test40(testValue2, index);
        Asserts.assertEQ(result, testValue2.hash());
        result = test40(testValue2Array, index);
        Asserts.assertEQ(result, testValue2.hash());
        result = test40(testValue1Array2, index);
        Asserts.assertEQ(result, testValue1.hash());
        result = test40(Long.valueOf(42), index);
        Asserts.assertEQ(result, 42L);
    }

    // Test for bug in Escape Analysis
    @DontInline
    public void test41_dontinline(Object o) {
        Asserts.assertEQ(o, rI);
    }

    @Test
    @IR(failOn = {ALLOC})
    public void test41() {
        MyValue1[] vals = new MyValue1[] {testValue1};
        test41_dontinline(vals[0].oa[0]);
        test41_dontinline(vals[0].oa[0]);
    }

    @Run(test = "test41")
    public void test41_verifier() {
        test41();
    }

    // Test for bug in Escape Analysis
    private static final MyValue1.ref test42VT1 = MyValue1.createWithFieldsInline(rI, rL);
    private static final MyValue1.ref test42VT2 = MyValue1.createWithFieldsInline(rI + 1, rL + 1);

    @Test
    @IR(failOn = {ALLOC})
    public void test42() {
        MyValue1[] vals = new MyValue1[] {(MyValue1) test42VT1, (MyValue1) test42VT2};
        Asserts.assertEQ(vals[0].hash(), test42VT1.hash());
        Asserts.assertEQ(vals[1].hash(), test42VT2.hash());
    }

    @Run(test = "test42")
    public void test42_verifier(RunInfo info) {
        if (!info.isWarmUp()) test42(); // We need -Xcomp behavior
    }

    // Test for bug in Escape Analysis
    @Test
    @IR(failOn = {ALLOC})
    public long test43(boolean deopt, Method m) {
        MyValue1[] vals = new MyValue1[] {(MyValue1) test42VT1, (MyValue1) test42VT2};

        if (deopt) {
            // uncommon trap
            TestFramework.deoptimize(m);
            Asserts.assertEQ(vals[0].hash(), test42VT1.hash());
            Asserts.assertEQ(vals[1].hash(), test42VT2.hash());
        }

        return vals[0].hash();
    }

    @Run(test = "test43")
    public void test43_verifier(RunInfo info) {
        test43(!info.isWarmUp(), info.getTest());
    }

    // Tests writing an array element with a (statically known) incompatible type
    private static final MethodHandle setArrayElementIncompatible = InstructionHelper.loadCode(MethodHandles.lookup(),
        "setArrayElementIncompatible",
        MethodType.methodType(void.class, TestLWorld.class, MyValue1[].class, int.class, MyValue2.class.asValueType()),
        CODE -> {
            CODE.
            aload_1().
            iload_2().
            aload_3().
            aastore().
            return_();
        });

    @Test
    @IR(failOn = {ALLOC_G})
    public void test44(MyValue1[] va, int index, MyValue2 v) throws Throwable {
        setArrayElementIncompatible.invoke(this, va, index, v);
    }

    @Run(test = "test44")
    @Warmup(10000)
    public void test44_verifier() throws Throwable {
        int index = Math.abs(rI) % 3;
        try {
            test44(testValue1Array, index, testValue2);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
        Asserts.assertEQ(testValue1Array[index].hash(), hash());
    }

    // Tests writing an array element with a (statically known) incompatible type
    @ForceInline
    public void test45_inline(Object[] oa, Object o, int index) {
        oa[index] = o;
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public void test45(MyValue1[] va, int index, MyValue2 v) throws Throwable {
        test45_inline(va, v, index);
    }

    @Run(test = "test45")
    public void test45_verifier() throws Throwable {
        int index = Math.abs(rI) % 3;
        try {
            test45(testValue1Array, index, testValue2);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
        Asserts.assertEQ(testValue1Array[index].hash(), hash());
    }

    // instanceof tests with inline types
    @Test
    @IR(failOn = {ALLOC_G})
    public boolean test46(MyValue1 vt) {
        Object obj = vt;
        return obj instanceof MyValue1;
    }

    @Run(test = "test46")
    public void test46_verifier() {
        MyValue1 vt = testValue1;
        boolean result = test46(vt);
        Asserts.assertTrue(result);
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public boolean test47(MyValue1 vt) {
        Object obj = vt;
        return obj instanceof MyValue2;
    }

    @Run(test = "test47")
    public void test47_verifier() {
        MyValue1 vt = testValue1;
        boolean result = test47(vt);
        Asserts.assertFalse(result);
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public boolean test48(Object obj) {
        return obj instanceof MyValue1;
    }

    @Run(test = "test48")
    public void test48_verifier() {
        MyValue1 vt = testValue1;
        boolean result = test48(vt);
        Asserts.assertTrue(result);
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public boolean test49(Object obj) {
        return obj instanceof MyValue2;
    }

    @Run(test = "test49")
    public void test49_verifier() {
        MyValue1 vt = testValue1;
        boolean result = test49(vt);
        Asserts.assertFalse(result);
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public boolean test50(Object obj) {
        return obj instanceof MyValue1;
    }

    @Run(test = "test50")
    public void test50_verifier() {
        boolean result = test49(Integer.valueOf(42));
        Asserts.assertFalse(result);
    }

    // Inline type with some non-flattened fields
    final primitive class Test51Value {
        final Object objectField1;
        final Object objectField2;
        final Object objectField3;
        final Object objectField4;
        final Object objectField5;
        final Object objectField6;

        final MyValue1 valueField1;
        final MyValue1 valueField2;
        final MyValue1.ref valueField3;
        final MyValue1 valueField4;
        final MyValue1.ref valueField5;

        public Test51Value() {
            objectField1 = null;
            objectField2 = null;
            objectField3 = null;
            objectField4 = null;
            objectField5 = null;
            objectField6 = null;
            valueField1 = testValue1;
            valueField2 = testValue1;
            valueField3 = testValue1;
            valueField4 = MyValue1.createDefaultDontInline();
            valueField5 = MyValue1.createDefaultDontInline();
        }

        public Test51Value(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6,
                           MyValue1 vt1, MyValue1 vt2, MyValue1.ref vt3, MyValue1 vt4, MyValue1.ref vt5) {
            objectField1 = o1;
            objectField2 = o2;
            objectField3 = o3;
            objectField4 = o4;
            objectField5 = o5;
            objectField6 = o6;
            valueField1 = vt1;
            valueField2 = vt2;
            valueField3 = vt3;
            valueField4 = vt4;
            valueField5 = vt5;
        }

        @ForceInline
        public long test(Test51Value holder, MyValue1 vt1, Object vt2) {
            holder = new Test51Value(vt1, holder.objectField2, holder.objectField3, holder.objectField4, holder.objectField5, holder.objectField6,
                                     holder.valueField1, holder.valueField2, holder.valueField3, holder.valueField4, holder.valueField5);
            holder = new Test51Value(holder.objectField1, (MyValue1)vt2, holder.objectField3, holder.objectField4, holder.objectField5, holder.objectField6,
                                     holder.valueField1, holder.valueField2, holder.valueField3, holder.valueField4, holder.valueField5);
            holder = new Test51Value(holder.objectField1, holder.objectField2, testValue1, holder.objectField4, holder.objectField5, holder.objectField6,
                                     holder.valueField1, holder.valueField2, holder.valueField3, holder.valueField4, holder.valueField5);
            holder = new Test51Value(holder.objectField1, holder.objectField2, holder.objectField3, MyValue1.createWithFieldsDontInline(rI, rL), holder.objectField5, holder.objectField6,
                                     holder.valueField1, holder.valueField2, holder.valueField3, holder.valueField4, holder.valueField5);
            holder = new Test51Value(holder.objectField1, holder.objectField2, holder.objectField3, holder.objectField4, holder.valueField1, holder.objectField6,
                                     holder.valueField1, holder.valueField2, holder.valueField3, holder.valueField4, holder.valueField5);
            holder = new Test51Value(holder.objectField1, holder.objectField2, holder.objectField3, holder.objectField4, holder.objectField5, holder.valueField3,
                                     holder.valueField1, holder.valueField2, holder.valueField3, holder.valueField4, holder.valueField5);
            holder = new Test51Value(holder.objectField1, holder.objectField2, holder.objectField3, holder.objectField4, holder.objectField5, holder.objectField6,
                                     (MyValue1)holder.objectField1, holder.valueField2, holder.valueField3, holder.valueField4, holder.valueField5);
            holder = new Test51Value(holder.objectField1, holder.objectField2, holder.objectField3, holder.objectField4, holder.objectField5, holder.objectField6,
                                     holder.valueField1, (MyValue1)vt2, holder.valueField3, holder.valueField4, holder.valueField5);
            holder = new Test51Value(holder.objectField1, holder.objectField2, holder.objectField3, holder.objectField4, holder.objectField5, holder.objectField6,
                                     holder.valueField1, holder.valueField2, (MyValue1)vt2, holder.valueField4, holder.valueField5);

            return ((MyValue1)holder.objectField1).hash() +
                   ((MyValue1)holder.objectField2).hash() +
                   ((MyValue1)holder.objectField3).hash() +
                   ((MyValue1)holder.objectField4).hash() +
                   ((MyValue1)holder.objectField5).hash() +
                   ((MyValue1)holder.objectField6).hash() +
                   holder.valueField1.hash() +
                   holder.valueField2.hash() +
                   holder.valueField3.hash() +
                   holder.valueField4.hashPrimitive();
        }
    }

    // Same as test2 but with field holder being an inline type
    @Test
    public long test51(Test51Value holder, MyValue1 vt1, Object vt2) {
        return holder.test(holder, vt1, vt2);
    }

    @Run(test = "test51")
    public void test51_verifier() {
        MyValue1 vt = testValue1;
        MyValue1 def = MyValue1.createDefaultDontInline();
        Test51Value holder = new Test51Value();
        Asserts.assertEQ(testValue1.hash(), vt.hash());
        Asserts.assertEQ(holder.valueField1.hash(), vt.hash());
        long result = test51(holder, vt, vt);
        Asserts.assertEQ(result, 9*vt.hash() + def.hashPrimitive());
    }

    // Access non-flattened, uninitialized inline type field with inline type holder
    @Test
    @IR(failOn = {ALLOC_G})
    public void test52(Test51Value holder) {
        if ((Object)holder.valueField5 != null) {
            throw new RuntimeException("Should be null");
        }
    }

    @Run(test = "test52")
    public void test52_verifier() {
        Test51Value vt = Test51Value.default;
        test52(vt);
    }

    // Merging inline types of different types
    @Test
    public Object test53(Object o, boolean b) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        return b ? vt : o;
    }

    @Run(test = "test53")
    public void test53_verifier() {
        test53(new Object(), false);
        MyValue1 result = (MyValue1)test53(new Object(), true);
        Asserts.assertEQ(result.hash(), hash());
    }

    @Test
    public Object test54(boolean b) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        return b ? vt : testValue2;
    }

    @Run(test = "test54")
    public void test54_verifier() {
        MyValue1 result1 = (MyValue1)test54(true);
        Asserts.assertEQ(result1.hash(), hash());
        MyValue2 result2 = (MyValue2)test54(false);
        Asserts.assertEQ(result2.hash(), testValue2.hash());
    }

    @Test
    public Object test55(boolean b) {
        MyValue1 vt1 = MyValue1.createWithFieldsInline(rI, rL);
        MyValue2 vt2 = MyValue2.createWithFieldsInline(rI, rD);
        return b ? vt1 : vt2;
    }

    @Run(test = "test55")
    public void test55_verifier() {
        MyValue1 result1 = (MyValue1)test55(true);
        Asserts.assertEQ(result1.hash(), hash());
        MyValue2 result2 = (MyValue2)test55(false);
        Asserts.assertEQ(result2.hash(), testValue2.hash());
    }

    // Test synchronization on inline types
    @Test
    public void test56(Object vt) {
        synchronized (vt) {
            throw new RuntimeException("test56 failed: synchronization on inline type should not succeed");
        }
    }

    @Run(test = "test56")
    public void test56_verifier() {
        try {
            test56(testValue1);
            throw new RuntimeException("test56 failed: no exception thrown");
        } catch (IllegalMonitorStateException ex) {
            // Expected
        }
    }

    @ForceInline
    public void test57_inline(Object vt) {
        synchronized (vt) {
            throw new RuntimeException("test57 failed: synchronization on inline type should not succeed");
        }
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public void test57(MyValue1 vt) {
        test57_inline(vt);
    }

    @Run(test = "test57")
    public void test57_verifier() {
        try {
            test57(testValue1);
            throw new RuntimeException("test57 failed: no exception thrown");
        } catch (IllegalMonitorStateException ex) {
            // Expected
        }
    }

    @ForceInline
    public void test58_inline(Object vt) {
        synchronized (vt) {
            throw new RuntimeException("test58 failed: synchronization on inline type should not succeed");
        }
    }

    @Test
    @IR(failOn = {ALLOC})
    public void test58() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        test58_inline(vt);
    }

    @Run(test = "test58")
    public void test58_verifier() {
        try {
            test58();
            throw new RuntimeException("test58 failed: no exception thrown");
        } catch (IllegalMonitorStateException ex) {
            // Expected
        }
    }

    @Test
    public void test59(Object o, boolean b) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        Object sync = b ? vt : o;
        synchronized (sync) {
            if (b) {
                throw new RuntimeException("test59 failed: synchronization on inline type should not succeed");
            }
        }
    }

    @Run(test = "test59")
    public void test59_verifier() {
        test59(new Object(), false);
        try {
            test59(new Object(), true);
            throw new RuntimeException("test59 failed: no exception thrown");
        } catch (IllegalMonitorStateException ex) {
            // Expected
        }
    }

    @Test
    public void test60(boolean b) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        Object sync = b ? vt : testValue2;
        synchronized (sync) {
            throw new RuntimeException("test60 failed: synchronization on inline type should not succeed");
        }
    }

    @Run(test = "test60")
    public void test60_verifier() {
        try {
            test60(false);
            throw new RuntimeException("test60 failed: no exception thrown");
        } catch (IllegalMonitorStateException ex) {
            // Expected
        }
        try {
            test60(true);
            throw new RuntimeException("test60 failed: no exception thrown");
        } catch (IllegalMonitorStateException ex) {
            // Expected
        }
    }

    // Test catching the IllegalMonitorStateException in compiled code
    @Test
    public void test61(Object vt) {
        boolean thrown = false;
        try {
            synchronized (vt) {
                throw new RuntimeException("test61 failed: no exception thrown");
            }
        } catch (IllegalMonitorStateException ex) {
            thrown = true;
        }
        if (!thrown) {
            throw new RuntimeException("test61 failed: no exception thrown");
        }
    }

    @Run(test = "test61")
    public void test61_verifier() {
        test61(testValue1);
    }

    @Test
    public void test62(Object o) {
        try {
            synchronized (o) { }
        } catch (IllegalMonitorStateException ex) {
            // Expected
            return;
        }
        throw new RuntimeException("test62 failed: no exception thrown");
    }

    @Run(test = "test62")
    public void test62_verifier() {
        test62(testValue1);
    }

    // Test synchronization without any instructions in the synchronized block
    @Test
    public void test63(Object o) {
        synchronized (o) { }
    }

    @Run(test = "test63")
    public void test63_verifier() {
        try {
            test63(testValue1);
        } catch (IllegalMonitorStateException ex) {
            // Expected
            return;
        }
        throw new RuntimeException("test63 failed: no exception thrown");
    }

    // type system test with interface and inline type
    @ForceInline
    public MyInterface test64Interface_helper(MyValue1 vt) {
        return vt;
    }

    @Test
    public MyInterface test64Interface(MyValue1 vt) {
        return test64Interface_helper(vt);
    }

    @Run(test = "test64Interface")
    public void test64Interface_verifier() {
        test64Interface(testValue1);
    }

    // type system test with abstract and inline type
    @ForceInline
    public MyAbstract test64Abstract_helper(MyValue1 vt) {
        return vt;
    }

    @Test
    public MyAbstract test64Abstract(MyValue1 vt) {
        return test64Abstract_helper(vt);
    }

    @Run(test = "test64Abstract")
    public void test64Abstract_verifier() {
        test64Abstract(testValue1);
    }

    // Array store tests
    @Test
    public void test65(Object[] array, MyValue1 vt) {
        array[0] = vt;
    }

    @Run(test = "test65")
    public void test65_verifier() {
        Object[] array = new Object[1];
        test65(array, testValue1);
        Asserts.assertEQ(((MyValue1)array[0]).hash(), testValue1.hash());
    }

    @Test
    public void test66(Object[] array, MyValue1 vt) {
        array[0] = vt;
    }

    @Run(test = "test66")
    public void test66_verifier() {
        MyValue1[] array = new MyValue1[1];
        test66(array, testValue1);
        Asserts.assertEQ(array[0].hash(), testValue1.hash());
    }

    @Test
    public void test67(Object[] array, Object vt) {
        array[0] = vt;
    }

    @Run(test = "test67")
    public void test67_verifier() {
        MyValue1[] array = new MyValue1[1];
        test67(array, testValue1);
        Asserts.assertEQ(array[0].hash(), testValue1.hash());
    }

    @Test
    public void test68(Object[] array, Integer o) {
        array[0] = o;
    }

    @Run(test = "test68")
    public void test68_verifier() {
        Integer[] array = new Integer[1];
        test68(array, 1);
        Asserts.assertEQ(array[0], Integer.valueOf(1));
    }

    // Test convertion between an inline type and java.lang.Object without an allocation
    @ForceInline
    public Object test69_sum(Object a, Object b) {
        int sum = ((MyValue1)a).x + ((MyValue1)b).x;
        return MyValue1.setX(((MyValue1)a), sum);
    }

    @Test
    @IR(failOn = {ALLOC_G, STORE})
    public int test69(MyValue1[] array) {
        MyValue1 result = MyValue1.createDefaultInline();
        for (int i = 0; i < array.length; ++i) {
            result = (MyValue1)test69_sum(result, array[i]);
        }
        return result.x;
    }

    @Run(test = "test69")
    public void test69_verifier() {
        int result = test69(testValue1Array);
        Asserts.assertEQ(result, rI * testValue1Array.length);
    }

    // Same as test69 but with an Interface
    @ForceInline
    public MyInterface test70Interface_sum(MyInterface a, MyInterface b) {
        int sum = ((MyValue1)a).x + ((MyValue1)b).x;
        return MyValue1.setX(((MyValue1)a), sum);
    }

    @Test
    @IR(failOn = {ALLOC_G, STORE})
    public int test70Interface(MyValue1[] array) {
        MyValue1 result = MyValue1.createDefaultInline();
        for (int i = 0; i < array.length; ++i) {
            result = (MyValue1)test70Interface_sum(result, array[i]);
        }
        return result.x;
    }

    @Run(test = "test70Interface")
    public void test70Interface_verifier() {
        int result = test70Interface(testValue1Array);
        Asserts.assertEQ(result, rI * testValue1Array.length);
    }

    // Same as test69 but with an Abstract
    @ForceInline
    public MyAbstract test70Abstract_sum(MyAbstract a, MyAbstract b) {
        int sum = ((MyValue1)a).x + ((MyValue1)b).x;
        return MyValue1.setX(((MyValue1)a), sum);
    }

    @Test
    @IR(failOn = {ALLOC_G, STORE})
    public int test70Abstract(MyValue1[] array) {
        MyValue1 result = MyValue1.createDefaultInline();
        for (int i = 0; i < array.length; ++i) {
            result = (MyValue1)test70Abstract_sum(result, array[i]);
        }
        return result.x;
    }

    @Run(test = "test70Abstract")
    public void test70Abstract_verifier() {
        int result = test70Abstract(testValue1Array);
        Asserts.assertEQ(result, rI * testValue1Array.length);
    }

    // Test that allocated inline type is not used in non-dominated path
    public MyValue1 test71_inline(Object obj) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        try {
            vt = (MyValue1)obj;
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        return vt;
    }

    @Test
    @IR(failOn = {ALLOC})
    public MyValue1 test71() {
        return test71_inline(null);
    }

    @Run(test = "test71")
    public void test71_verifier() {
        MyValue1 vt = test71();
        Asserts.assertEquals(vt.hash(), hash());
    }

    // Test calling a method on an uninitialized inline type
    final primitive class Test72Value {
        final int x = 42;
        public int get() {
            return x;
        }
    }

    // Make sure Test72Value is loaded but not initialized
    public void unused(Test72Value vt) { }

    @Test
    @IR(failOn = {ALLOC_G})
    public int test72() {
        Test72Value vt = Test72Value.default;
        return vt.get();
    }

    @Run(test = "test72")
    @Warmup(0)
    public void test72_verifier() {
        int result = test72();
        Asserts.assertEquals(result, 0);
    }

    // Tests for loading/storing unkown values
    @Test
    public Object test73(Object[] va) {
        return va[0];
    }

    @Run(test = "test73")
    public void test73_verifier() {
        MyValue1 vt = (MyValue1)test73(testValue1Array);
        Asserts.assertEquals(testValue1Array[0].hash(), vt.hash());
    }

    @Test
    public void test74(Object[] va, Object vt) {
        va[0] = vt;
    }

    @Run(test = "test74")
    public void test74_verifier() {
        MyValue1[] va = new MyValue1[1];
        test74(va, testValue1);
        Asserts.assertEquals(va[0].hash(), testValue1.hash());
    }

    // Verify that mixing instances and arrays with the clone api
    // doesn't break anything
    @Test
    @IR(failOn = {ALLOC_G})
    public Object test75(Object o) {
        MyValue1[] va = new MyValue1[1];
        Object[] next = va;
        Object[] arr = va;
        for (int i = 0; i < 10; i++) {
            arr = next;
            next = new Integer[1];
        }
        return arr[0];
    }

    @Run(test = "test75")
    public void test75_verifier() {
        test75(42);
    }

    // Casting a null Integer to a (non-nullable) inline type should throw a NullPointerException
    @ForceInline
    public MyValue1 test76_helper(Object o) {
        return (MyValue1)o;
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public MyValue1 test76(Integer i) throws Throwable {
        return test76_helper(i);
    }

    @Run(test = "test76")
    public void test76_verifier() throws Throwable {
        try {
            test76(null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        } catch (Exception e) {
            throw new RuntimeException("test76 failed: unexpected exception", e);
        }
    }

    // Casting an Integer to a (non-nullable) inline type should throw a ClassCastException
    @ForceInline
    public MyValue1 test77_helper(Object o) {
        return (MyValue1)o;
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public MyValue1 test77(Integer i) throws Throwable {
        return test77_helper(i);
    }

    @Run(test = "test77")
    public void test77_verifier() throws Throwable {
        try {
            test77(Integer.valueOf(42));
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException e) {
            // Expected
        } catch (Exception e) {
            throw new RuntimeException("test77 failed: unexpected exception", e);
        }
    }

    // Casting a null Integer to a nullable inline type should not throw
    @ForceInline
    public MyValue1.ref test78_helper(Object o) {
        return (MyValue1.ref)o;
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public MyValue1.ref test78(Integer i) throws Throwable {
        return test78_helper(i);
    }

    @Run(test = "test78")
    public void test78_verifier() throws Throwable {
        try {
            test78(null); // Should not throw
        } catch (Exception e) {
            throw new RuntimeException("test78 failed: unexpected exception", e);
        }
    }

    // Casting an Integer to a nullable inline type should throw a ClassCastException
    @ForceInline
    public MyValue1.ref test79_helper(Object o) {
        return (MyValue1.ref)o;
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public MyValue1.ref test79(Integer i) throws Throwable {
        return test79_helper(i);
    }

    @Run(test = "test79")
    public void test79_verifier() throws Throwable {
        try {
            test79(Integer.valueOf(42));
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException e) {
            // Expected
        } catch (Exception e) {
            throw new RuntimeException("test79 failed: unexpected exception", e);
        }
    }

    // Test flattened field with non-flattenend (but flattenable) inline type field
    static primitive class Small {
        final int i;
        final Big big; // Too big to be flattened

        private Small() {
            i = rI;
            big = new Big();
        }
    }

    static primitive class Big {
        long l0,l1,l2,l3,l4,l5,l6,l7,l8,l9;
        long l10,l11,l12,l13,l14,l15,l16,l17,l18,l19;
        long l20,l21,l22,l23,l24,l25,l26,l27,l28,l29;

        private Big() {
            l0 = l1 = l2 = l3 = l4 = l5 = l6 = l7 = l8 = l9 = rL;
            l10 = l11 = l12 = l13 = l14 = l15 = l16 = l17 = l18 = l19 = rL+1;
            l20 = l21 = l22 = l23 = l24 = l25 = l26 = l27 = l28 = l29 = rL+2;
        }
    }

    Small small = new Small();
    Small smallDefault;
    Big big = new Big();
    Big bigDefault;

    @Test
    public long test80() {
        return small.i + small.big.l0 + smallDefault.i + smallDefault.big.l29 + big.l0 + bigDefault.l29;
    }

    @Run(test = "test80")
    public void test80_verifier() throws Throwable {
        long result = test80();
        Asserts.assertEQ(result, rI + 2*rL);
    }

    // Test scalarization with exceptional control flow
    public int test81Callee(MyValue1 vt)  {
        return vt.x;
    }

    @Test
    @IR(failOn = {ALLOC_G, LOAD, STORE})
    public int test81()  {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        int result = 0;
        for (int i = 0; i < 10; i++) {
            try {
                result += test81Callee(vt);
            } catch (NullPointerException npe) {
                result += rI;
            }
        }
        return result;
    }

    @Run(test = "test81")
    public void test81_verifier() {
        int result = test81();
        Asserts.assertEQ(result, 10*rI);
    }

    // Test check for null free array when storing to inline tpye array
    @Test
    public void test82(Object[] dst, Object v) {
        dst[0] = v;
    }

    @Run(test = "test82")
    public void test82_verifier(RunInfo info) {
        MyValue2[] dst = new MyValue2[1];
        test82(dst, testValue2);
        if (!info.isWarmUp()) {
            try {
                test82(dst, null);
                throw new RuntimeException("No ArrayStoreException thrown");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public void test83(Object[] dst, Object v, boolean flag) {
        if (dst == null) { // null check
        }
        if (flag) {
            if (dst.getClass() == MyValue1[].class) { // trigger split if
            }
        } else {
            dst = new MyValue2[1]; // constant null free property
        }
        dst[0] = v;
    }

    @Run(test = "test83")
    @Warmup(10000)
    public void test83_verifier(RunInfo info) {
        MyValue2[] dst = new MyValue2[1];
        test83(dst, testValue2, false);
        test83(dst, testValue2, true);
        if (!info.isWarmUp()) {
            try {
                test83(dst, null, true);
                throw new RuntimeException("No ArrayStoreException thrown");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    private void rerun_and_recompile_for(Method m, int num, Runnable test) {
        for (int i = 1; i < num; i++) {
            test.run();

            if (!TestFramework.isCompiled(m)) {
                TestFramework.compile(m, CompLevel.C2);
            }
        }
    }

    // Tests for the Loop Unswitching optimization
    // Should make 2 copies of the loop, one for non flattened arrays, one for other cases.
    @Test
    @IR(applyIf = {"FlatArrayElementMaxSize", "= -1"},
        counts = {COUNTEDLOOP_MAIN, "= 2"})
    @IR(applyIf = {"FlatArrayElementMaxSize", "!= -1"},
        counts = {COUNTEDLOOP_MAIN, "= 1"})
    public void test84(Object[] src, Object[] dst) {
        for (int i = 0; i < src.length; i++) {
            dst[i] = src[i];
        }
    }

    @Run(test = "test84")
    @Warmup(0)
    public void test84_verifier(RunInfo info) {
        MyValue2[] src = new MyValue2[100];
        Arrays.fill(src, testValue2);
        MyValue2[] dst = new MyValue2[100];
        rerun_and_recompile_for(info.getTest(), 10,
                                () ->  { test84(src, dst);
                                         Asserts.assertTrue(Arrays.equals(src, dst)); });
    }

    @Test
    @IR(applyIfAnd = {"UseG1GC", "true", "FlatArrayElementMaxSize", "= -1"},
        counts = {COUNTEDLOOP, "= 2", LOAD_UNKNOWN_INLINE, "= 1"})
    @IR(applyIfAnd = {"UseG1GC", "false", "FlatArrayElementMaxSize", "= -1"},
        counts = {COUNTEDLOOP_MAIN, "= 2", LOAD_UNKNOWN_INLINE, "= 4"})
    public void test85(Object[] src, Object[] dst) {
        for (int i = 0; i < src.length; i++) {
            dst[i] = src[i];
        }
    }

    @Run(test = "test85")
    @Warmup(0)
    public void test85_verifier(RunInfo info) {
        Object[] src = new Object[100];
        Arrays.fill(src, new Object());
        src[0] = null;
        Object[] dst = new Object[100];
        rerun_and_recompile_for(info.getTest(), 10,
                                () -> { test85(src, dst);
                                        Asserts.assertTrue(Arrays.equals(src, dst)); });
    }

    @Test
    @IR(applyIfAnd = {"UseG1GC", "true", "FlatArrayElementMaxSize", "= -1"},
        counts = {COUNTEDLOOP, "= 2"})
    @IR(applyIfAnd = {"UseG1GC", "false", "FlatArrayElementMaxSize", "= -1"},
        counts = {COUNTEDLOOP_MAIN, "= 2"})
    public void test86(Object[] src, Object[] dst) {
        for (int i = 0; i < src.length; i++) {
            dst[i] = src[i];
        }
    }

    @Run(test = "test86")
    @Warmup(0)
    public void test86_verifier(RunInfo info) {
        MyValue2[] src = new MyValue2[100];
        Arrays.fill(src, testValue2);
        Object[] dst = new Object[100];
        rerun_and_recompile_for(info.getTest(), 10,
                                () -> { test86(src, dst);
                                        Asserts.assertTrue(Arrays.equals(src, dst)); });
    }

    @Test
    @IR(applyIf = {"FlatArrayElementMaxSize", "= -1"},
        counts = {COUNTEDLOOP_MAIN, "= 2"})
    @IR(applyIf = {"FlatArrayElementMaxSize", "!= -1"},
        counts = {COUNTEDLOOP_MAIN, "= 1"})
    public void test87(Object[] src, Object[] dst) {
        for (int i = 0; i < src.length; i++) {
            dst[i] = src[i];
        }
    }

    @Run(test = "test87")
    @Warmup(0)
    public void test87_verifier(RunInfo info) {
        Object[] src = new Object[100];
        Arrays.fill(src, testValue2);
        MyValue2[] dst = new MyValue2[100];

        rerun_and_recompile_for(info.getTest(), 10,
                                () -> { test87(src, dst);
                                        Asserts.assertTrue(Arrays.equals(src, dst)); });
    }

    @Test
    @IR(applyIf = {"FlatArrayElementMaxSize", "= -1"},
        counts = {COUNTEDLOOP_MAIN, "= 2"})
    @IR(applyIf = {"FlatArrayElementMaxSize", "!= -1"},
        counts = {COUNTEDLOOP_MAIN, "= 0"})
    public void test88(Object[] src1, Object[] dst1, Object[] src2, Object[] dst2) {
        for (int i = 0; i < src1.length; i++) {
            dst1[i] = src1[i];
            dst2[i] = src2[i];
        }
    }

    @Run(test = "test88")
    @Warmup(0)
    public void test88_verifier(RunInfo info) {
        MyValue2[] src1 = new MyValue2[100];
        Arrays.fill(src1, testValue2);
        MyValue2[] dst1 = new MyValue2[100];
        Object[] src2 = new Object[100];
        Arrays.fill(src2, new Object());
        Object[] dst2 = new Object[100];

        rerun_and_recompile_for(info.getTest(), 10,
                                () -> { test88(src1, dst1, src2, dst2);
                                        Asserts.assertTrue(Arrays.equals(src1, dst1));
                                        Asserts.assertTrue(Arrays.equals(src2, dst2)); });
    }

    @Test
    public boolean test89(Object obj) {
        return obj.getClass() == Integer.class;
    }

    @Run(test = "test89")
    public void test89_verifier() {
        Asserts.assertTrue(test89(Integer.valueOf(42)));
        Asserts.assertFalse(test89(new Object()));
    }

    @Test
    public Integer test90(Object obj) {
        return (Integer)obj;
    }

    @Run(test = "test90")
    public void test90_verifier() {
        test90(Integer.valueOf(42));
        try {
            test90(new Object());
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    @Test
    public boolean test91(Object obj) {
        return obj.getClass() == MyValue2[].class;
    }

    @Run(test = "test91")
    public void test91_verifier() {
        Asserts.assertTrue(test91(new MyValue2[1]));
        Asserts.assertFalse(test91(new Object()));
    }

    static primitive class Test92Value {
        final int field;
        public Test92Value() {
            field = 0x42;
        }
    }

    @Test
    @IR(applyIf = {"FlatArrayElementMaxSize", "= -1"},
        counts = {CLASS_CHECK_TRAP, "= 2"},
        failOn = {LOAD_UNKNOWN_INLINE, ALLOC_G, MEMBAR})
    public Object test92(Object[] array) {
        // Dummy loops to ensure we run enough passes of split if
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
              for (int k = 0; k < 2; k++) {
              }
            }
        }

        return (Integer)array[0];
    }

    @Run(test = "test92")
    @Warmup(10000)
    public void test92_verifier() {
        Object[] array = new Object[1];
        array[0] = 0x42;
        Object result = test92(array);
        Asserts.assertEquals(result, 0x42);
    }

    // If the class check succeeds, the flattened array check that
    // precedes will never succeed and the flat array branch should
    // trigger an uncommon trap.
    @Test
    public Object test93(Object[] array) {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
            }
        }

        Object v = (Integer)array[0];
        return v;
    }

    @Run(test = "test93")
    @Warmup(10000)
    public void test93_verifier(RunInfo info) {
        if (info.isWarmUp()) {
            Object[] array = new Object[1];
            array[0] = 0x42;
            Object result = test93(array);
            Asserts.assertEquals(result, 0x42);
        } else {
            Object[] array = new Test92Value[1];
            Method m = info.getTest();
            int extra = 3;
            for (int j = 0; j < extra; j++) {
                for (int i = 0; i < 10; i++) {
                    try {
                        test93(array);
                    } catch (ClassCastException cce) {
                    }
                }
                boolean compiled = TestFramework.isCompiled(m);
                boolean compilationSkipped = info.isCompilationSkipped();
                Asserts.assertTrue(compilationSkipped || compiled || (j != extra-1));
                if (!compilationSkipped && !compiled) {
                    TestFramework.compile(m, CompLevel.ANY);
                }
            }
        }
    }

    @Test
    @IR(applyIf = {"FlatArrayElementMaxSize", "= -1"},
        counts = {CLASS_CHECK_TRAP, "= 2", LOOP, "= 1"},
        failOn = {LOAD_UNKNOWN_INLINE, ALLOC_G, MEMBAR})
    public int test94(Object[] array) {
        int res = 0;
        for (int i = 1; i < 4; i *= 2) {
            Object v = array[i];
            res += (Integer)v;
        }
        return res;
    }

    @Run(test = "test94")
    @Warmup(10000)
    public void test94_verifier() {
        Object[] array = new Object[4];
        array[0] = 0x42;
        array[1] = 0x42;
        array[2] = 0x42;
        array[3] = 0x42;
        int result = test94(array);
        Asserts.assertEquals(result, 0x42 * 2);
    }

    @Test
    public boolean test95(Object o1, Object o2) {
        return o1 == o2;
    }

    @Run(test = "test95")
    @Warmup(10000)
    public void test95_verifier() {
        Object o1 = new Object();
        Object o2 = new Object();
        Asserts.assertTrue(test95(o1, o1));
        Asserts.assertTrue(test95(null, null));
        Asserts.assertFalse(test95(o1, null));
        Asserts.assertFalse(test95(o1, o2));
    }

    @Test
    public boolean test96(Object o1, Object o2) {
        return o1 == o2;
    }

    @Run(test = "test96")
    @Warmup(10000)
    public void test96_verifier(RunInfo info) {
        Object o1 = new Object();
        Object o2 = new Object();
        Asserts.assertTrue(test96(o1, o1));
        Asserts.assertFalse(test96(o1, o2));
        if (!info.isWarmUp()) {
            Asserts.assertTrue(test96(null, null));
            Asserts.assertFalse(test96(o1, null));
        }
    }

    // Abstract class tests

    @DontInline
    public MyAbstract test97_dontinline1(MyAbstract o) {
        return o;
    }

    @DontInline
    public MyValue1 test97_dontinline2(MyAbstract o) {
        return (MyValue1)o;
    }

    @ForceInline
    public MyAbstract test97_inline1(MyAbstract o) {
        return o;
    }

    @ForceInline
    public MyValue1 test97_inline2(MyAbstract o) {
        return (MyValue1)o;
    }

    @Test
    public MyValue1 test97() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        vt = (MyValue1)test97_dontinline1(vt);
        vt =           test97_dontinline2(vt);
        vt = (MyValue1)test97_inline1(vt);
        vt =           test97_inline2(vt);
        return vt;
    }

    @Run(test = "test97")
    public void test97_verifier() {
        Asserts.assertEQ(test97().hash(), hash());
    }

    // Test storing/loading inline types to/from abstract and inline type fields
    MyAbstract abstractField1 = null;
    MyAbstract abstractField2 = null;
    MyAbstract abstractField3 = null;
    MyAbstract abstractField4 = null;
    MyAbstract abstractField5 = null;
    MyAbstract abstractField6 = null;

    @DontInline
    public MyAbstract readValueField5AsAbstract() {
        return (MyAbstract)valueField5;
    }

    @DontInline
    public MyAbstract readStaticValueField4AsAbstract() {
        return (MyAbstract)staticValueField4;
    }

    @Test
    public long test98(MyValue1 vt1, MyAbstract vt2) {
        abstractField1 = vt1;
        abstractField2 = (MyValue1)vt2;
        abstractField3 = MyValue1.createWithFieldsInline(rI, rL);
        abstractField4 = MyValue1.createWithFieldsDontInline(rI, rL);
        abstractField5 = valueField1;
        abstractField6 = valueField3;
        valueField1 = (MyValue1)abstractField1;
        valueField2 = (MyValue1)vt2;
        valueField3 = (MyValue1)vt2;
        staticValueField1 = (MyValue1)abstractField1;
        staticValueField2 = (MyValue1)vt1;
        // Don't inline these methods because reading NULL will trigger a deoptimization
        if (readValueField5AsAbstract() != null || readStaticValueField4AsAbstract() != null) {
            throw new RuntimeException("Should be null");
        }
        return ((MyValue1)abstractField1).hash() + ((MyValue1)abstractField2).hash() +
               ((MyValue1)abstractField3).hash() + ((MyValue1)abstractField4).hash() +
               ((MyValue1)abstractField5).hash() + ((MyValue1)abstractField6).hash() +
                valueField1.hash() + valueField2.hash() + valueField3.hash() + valueField4.hashPrimitive() +
                staticValueField1.hash() + staticValueField2.hash() + staticValueField3.hashPrimitive();
    }

    @Run(test = "test98")
    public void test98_verifier() {
        MyValue1 vt = testValue1;
        MyValue1 def = MyValue1.createDefaultDontInline();
        long result = test98(vt, vt);
        Asserts.assertEQ(result, 11*vt.hash() + 2*def.hashPrimitive());
    }

    class MyObject2 extends MyAbstract {
        public int x;

        public MyObject2(int x) {
            this.x = x;
        }

        @ForceInline
        public long hash() {
            return x;
        }
    }

    // Test merging inline types and abstract classes
    @Test
    public MyAbstract test99(int state) {
        MyAbstract res = null;
        if (state == 0) {
            res = new MyObject2(rI);
        } else if (state == 1) {
            res = MyValue1.createWithFieldsInline(rI, rL);
        } else if (state == 2) {
            res = MyValue1.createWithFieldsDontInline(rI, rL);
        } else if (state == 3) {
            res = (MyValue1)objectField1;
        } else if (state == 4) {
            res = valueField1;
        } else if (state == 5) {
            res = null;
        }
        return res;
    }

    @Run(test = "test99")
    public void test99_verifier() {
        objectField1 = valueField1;
        MyAbstract result = null;
        result = test99(0);
        Asserts.assertEQ(((MyObject2)result).x, rI);
        result = test99(1);
        Asserts.assertEQ(((MyValue1)result).hash(), hash());
        result = test99(2);
        Asserts.assertEQ(((MyValue1)result).hash(), hash());
        result = test99(3);
        Asserts.assertEQ(((MyValue1)result).hash(), hash());
        result = test99(4);
        Asserts.assertEQ(((MyValue1)result).hash(), hash());
        result = test99(5);
        Asserts.assertEQ(result, null);
    }

    // Test merging inline types and abstract classes in loops
    @Test
    public MyAbstract test100(int iters) {
        MyAbstract res = new MyObject2(rI);
        for (int i = 0; i < iters; ++i) {
            if (res instanceof MyObject2) {
                res = MyValue1.createWithFieldsInline(rI, rL);
            } else {
                res = MyValue1.createWithFieldsInline(((MyValue1)res).x + 1, rL);
            }
        }
        return res;
    }

    @Run(test = "test100")
    public void test100_verifier() {
        MyObject2 result1 = (MyObject2)test100(0);
        Asserts.assertEQ(result1.x, rI);
        int iters = (Math.abs(rI) % 10) + 1;
        MyValue1 result2 = (MyValue1)test100(iters);
        MyValue1 vt = MyValue1.createWithFieldsInline(rI + iters - 1, rL);
        Asserts.assertEQ(result2.hash(), vt.hash());
    }

    // Test inline types in abstract class variables that are live at safepoint
    @Test
    @IR(failOn = {ALLOC, STORE, LOOP})
    public long test101(MyValue1 arg, boolean deopt, Method m) {
        MyAbstract vt1 = MyValue1.createWithFieldsInline(rI, rL);
        MyAbstract vt2 = MyValue1.createWithFieldsDontInline(rI, rL);
        MyAbstract vt3 = arg;
        MyAbstract vt4 = valueField1;
        if (deopt) {
            // uncommon trap
            TestFramework.deoptimize(m);
        }
        return ((MyValue1)vt1).hash() + ((MyValue1)vt2).hash() +
               ((MyValue1)vt3).hash() + ((MyValue1)vt4).hash();
    }

    @Run(test = "test101")
    public void test101_verifier(RunInfo info) {
        long result = test101(valueField1, !info.isWarmUp(), info.getTest());
        Asserts.assertEQ(result, 4*hash());
    }

    // Test comparing inline types with abstract classes
    @Test
    public boolean test102(Object arg) {
        MyAbstract vt = MyValue1.createWithFieldsInline(rI, rL);
        if (vt == arg || vt == (MyAbstract)valueField1 || vt == abstractField1 || vt == null ||
            arg == vt || (MyAbstract)valueField1 == vt || abstractField1 == vt || null == vt) {
            return true;
        }
        return false;
    }

    @Run(test = "test102")
    public void test102_verifier() {
        boolean result = test102(null);
        Asserts.assertFalse(result);
    }

    // An abstract class with a non-static field can never be implemented by an inline type
    abstract class NoValueImplementors1 {
        int field = 42;
    }

    class MyObject3 extends NoValueImplementors1 {

    }

    class MyObject4 extends NoValueImplementors1 {

    }

    // Loading from an abstract class array does not require a flatness check if the abstract class has a non-static field
    @Test
    @IR(failOn = {ALLOC_G, MEMBAR, LOAD_UNKNOWN_INLINE, STORE_UNKNOWN_INLINE, INLINE_ARRAY_NULL_GUARD})
    public NoValueImplementors1 test103(NoValueImplementors1[] array, int i) {
        return array[i];
    }

    @Run(test = "test103")
    public void test103_verifier() {
        NoValueImplementors1[] array1 = new NoValueImplementors1[3];
        MyObject3[] array2 = new MyObject3[3];
        MyObject4[] array3 = new MyObject4[3];
        NoValueImplementors1 result = test103(array1, 0);
        Asserts.assertEquals(result, array1[0]);

        result = test103(array2, 1);
        Asserts.assertEquals(result, array1[1]);

        result = test103(array3, 2);
        Asserts.assertEquals(result, array1[2]);
    }

    // Storing to an abstract class array does not require a flatness/null check if the abstract class has a non-static field
    @Test
    @IR(failOn = {ALLOC_G, LOAD_UNKNOWN_INLINE, STORE_UNKNOWN_INLINE, INLINE_ARRAY_NULL_GUARD})
    public NoValueImplementors1 test104(NoValueImplementors1[] array, NoValueImplementors1 v, MyObject3 o, int i) {
        array[0] = v;
        array[1] = array[0];
        array[2] = o;
        return array[i];
    }

    @Run(test = "test104")
    public void test104_verifier() {
        MyObject4 v = new MyObject4();
        MyObject3 o = new MyObject3();
        NoValueImplementors1[] array1 = new NoValueImplementors1[3];
        MyObject3[] array2 = new MyObject3[3];
        MyObject4[] array3 = new MyObject4[3];
        NoValueImplementors1 result = test104(array1, v, o, 0);
        Asserts.assertEquals(array1[0], v);
        Asserts.assertEquals(array1[1], v);
        Asserts.assertEquals(array1[2], o);
        Asserts.assertEquals(result, v);

        result = test104(array2, o, o, 1);
        Asserts.assertEquals(array2[0], o);
        Asserts.assertEquals(array2[1], o);
        Asserts.assertEquals(array2[2], o);
        Asserts.assertEquals(result, o);

        result = test104(array3, v, null, 1);
        Asserts.assertEquals(array3[0], v);
        Asserts.assertEquals(array3[1], v);
        Asserts.assertEquals(array3[2], null);
        Asserts.assertEquals(result, v);
    }

    // An abstract class with a single, non-inline implementor
    abstract class NoValueImplementors2 {

    }

    class MyObject5 extends NoValueImplementors2 {

    }

    // Loading from an abstract class array does not require a flatness check if the abstract class has no inline implementor
    @Test
    @IR(failOn = {ALLOC_G, MEMBAR, LOAD_UNKNOWN_INLINE, STORE_UNKNOWN_INLINE, INLINE_ARRAY_NULL_GUARD})
    public NoValueImplementors2 test105(NoValueImplementors2[] array, int i) {
        return array[i];
    }

    @Run(test = "test105")
    public void test105_verifier() {
        NoValueImplementors2[] array1 = new NoValueImplementors2[3];
        MyObject5[] array2 = new MyObject5[3];
        NoValueImplementors2 result = test105(array1, 0);
        Asserts.assertEquals(result, array1[0]);

        result = test105(array2, 1);
        Asserts.assertEquals(result, array1[1]);
    }

    // Storing to an abstract class array does not require a flatness/null check if the abstract class has no inline implementor
    @Test
    @IR(failOn = {ALLOC_G, LOAD_UNKNOWN_INLINE, STORE_UNKNOWN_INLINE, INLINE_ARRAY_NULL_GUARD})
    public NoValueImplementors2 test106(NoValueImplementors2[] array, NoValueImplementors2 v, MyObject5 o, int i) {
        array[0] = v;
        array[1] = array[0];
        array[2] = o;
        return array[i];
    }

    @Run(test = "test106")
    public void test106_verifier() {
        MyObject5 v = new MyObject5();
        NoValueImplementors2[] array1 = new NoValueImplementors2[3];
        MyObject5[] array2 = new MyObject5[3];
        NoValueImplementors2 result = test106(array1, v, null, 0);
        Asserts.assertEquals(array1[0], v);
        Asserts.assertEquals(array1[1], v);
        Asserts.assertEquals(array1[2], null);
        Asserts.assertEquals(result, v);

        result = test106(array2, v, v, 1);
        Asserts.assertEquals(array2[0], v);
        Asserts.assertEquals(array2[1], v);
        Asserts.assertEquals(array2[2], v);
        Asserts.assertEquals(result, v);
    }

    // More tests for the Loop Unswitching optimization (similar to test84 and following)
    Object oFld1, oFld2;

    @Test
    @IR(applyIfAnd = {"UseG1GC", "true", "FlatArrayElementMaxSize", "= -1"},
        failOn = {STORE_UNKNOWN_INLINE, INLINE_ARRAY_NULL_GUARD},
        counts = {COUNTEDLOOP, "= 2", LOAD_UNKNOWN_INLINE, "= 2"})
    @IR(applyIfAnd = {"UseG1GC", "false", "FlatArrayElementMaxSize", "= -1"},
        failOn = {STORE_UNKNOWN_INLINE, INLINE_ARRAY_NULL_GUARD},
        counts = {COUNTEDLOOP, "= 3", LOAD_UNKNOWN_INLINE, "= 2"})
    public void test107(Object[] src1, Object[] src2) {
        for (int i = 0; i < src1.length; i++) {
            oFld1 = src1[i];
            oFld2 = src2[i];
        }
    }

    @Run(test = "test107")
    @Warmup(0)
    public void test107_verifier(RunInfo info) {
        MyValue2[] src1 = new MyValue2[100];
        Arrays.fill(src1, testValue2);
        Object[] src2 = new Object[100];
        Object obj = new Object();
        Arrays.fill(src2, obj);
        rerun_and_recompile_for(info.getTest(), 10,
                                () -> { test107(src1, src2);
                                        Asserts.assertEquals(oFld1, testValue2);
                                        Asserts.assertEquals(oFld2, obj);
                                        test107(src2, src1);
                                        Asserts.assertEquals(oFld1, obj);
                                        Asserts.assertEquals(oFld2, testValue2);  });
    }

    @Test
    @IR(applyIfAnd = {"UseG1GC", "true", "FlatArrayElementMaxSize", "= -1"},
        failOn = {LOAD_UNKNOWN_INLINE, INLINE_ARRAY_NULL_GUARD},
        counts = {COUNTEDLOOP, "= 4", STORE_UNKNOWN_INLINE, "= 9"})
    @IR(applyIfAnd = {"UseG1GC", "false", "FlatArrayElementMaxSize", "= -1"},
        failOn = {LOAD_UNKNOWN_INLINE, INLINE_ARRAY_NULL_GUARD},
        counts = {COUNTEDLOOP, "= 4", STORE_UNKNOWN_INLINE, "= 12"})
    public void test108(Object[] dst1, Object[] dst2, Object o1, Object o2) {
        for (int i = 0; i < dst1.length; i++) {
            dst1[i] = o1;
            dst2[i] = o2;
        }
    }

    @Run(test = "test108")
    @Warmup(0)
    public void test108_verifier(RunInfo info) {
        MyValue2[] dst1 = new MyValue2[100];
        Object[] dst2 = new Object[100];
        Object o1 = new Object();
        rerun_and_recompile_for(info.getTest(), 10,
                                () -> { test108(dst1, dst2, testValue2, o1);
                                        for (int i = 0; i < dst1.length; i++) {
                                            Asserts.assertEquals(dst1[i], testValue2);
                                            Asserts.assertEquals(dst2[i], o1);
                                        }
                                        test108(dst2, dst1, o1, testValue2);
                                        for (int i = 0; i < dst1.length; i++) {
                                            Asserts.assertEquals(dst1[i], testValue2);
                                            Asserts.assertEquals(dst2[i], o1);
                                        } });
    }

    // Escape analysis tests

    static interface WrapperInterface {
        long value();

        final static WrapperInterface ZERO = new LongWrapper(0);

        @ForceInline
        static WrapperInterface wrap(long val) {
            return (val == 0L) ? ZERO : new LongWrapper(val);
        }
    }

    @ForceCompileClassInitializer
    static primitive class LongWrapper implements WrapperInterface {
        final static LongWrapper ZERO = new LongWrapper(0);
        private long val;

        @ForceInline
        LongWrapper(long val) {
            this.val = val;
        }

        @ForceInline
        static LongWrapper wrap(long val) {
            return (val == 0L) ? ZERO : new LongWrapper(val);
        }

        @ForceInline
        public long value() {
            return val;
        }
    }

    static class InterfaceBox {
        WrapperInterface content;

        @ForceInline
        InterfaceBox(WrapperInterface content) {
            this.content = content;
        }

        @ForceInline
        static InterfaceBox box_sharp(long val) {
            return new InterfaceBox(LongWrapper.wrap(val));
        }

        @ForceInline
        static InterfaceBox box(long val) {
            return new InterfaceBox(WrapperInterface.wrap(val));
        }
    }

    static class ObjectBox {
        Object content;

        @ForceInline
        ObjectBox(Object content) {
            this.content = content;
        }

        @ForceInline
        static ObjectBox box_sharp(long val) {
            return new ObjectBox(LongWrapper.wrap(val));
        }

        @ForceInline
        static ObjectBox box(long val) {
            return new ObjectBox(WrapperInterface.wrap(val));
        }
    }

    static class RefBox {
        LongWrapper.ref content;

        @ForceInline
        RefBox(LongWrapper.ref content) {
            this.content = content;
        }

        @ForceInline
        static RefBox box_sharp(long val) {
            return new RefBox(LongWrapper.wrap(val));
        }

        @ForceInline
        static RefBox box(long val) {
            return new RefBox((LongWrapper.ref)WrapperInterface.wrap(val));
        }
    }

    static class InlineBox {
        LongWrapper content;

        @ForceInline
        InlineBox(long val) {
            this.content = LongWrapper.wrap(val);
        }

        @ForceInline
        static InlineBox box(long val) {
            return new InlineBox(val);
        }
    }

    static class GenericBox<T> {
        T content;

        @ForceInline
        static GenericBox<LongWrapper.ref> box_sharp(long val) {
            GenericBox<LongWrapper.ref> res = new GenericBox<>();
            res.content = LongWrapper.wrap(val);
            return res;
        }

        @ForceInline
        static GenericBox<WrapperInterface> box(long val) {
            GenericBox<WrapperInterface> res = new GenericBox<>();
            res.content = WrapperInterface.wrap(val);
            return res;
        }
    }

    long[] lArr = {0L, rL, 0L, rL, 0L, rL, 0L, rL, 0L, rL};

    // Test removal of allocations when inline type instance is wrapped into box object
    @Test
    @IR(failOn = {ALLOC_G, MEMBAR},
        counts = {PREDICATE_TRAP, "= 1"})
    public long test109() {
        long res = 0;
        for (int i = 0; i < lArr.length; i++) {
            res += InterfaceBox.box(lArr[i]).content.value();
        }
        return res;
    }

    @Run(test = "test109")
    @Warmup(10000) // Make sure interface calls are inlined
    public void test109_verifier() {
        long res = test109();
        Asserts.assertEquals(res, 5*rL);
    }

    @Test
    @IR(failOn = {ALLOC_G, MEMBAR},
        counts = {PREDICATE_TRAP, "= 1"})
    public long test109_sharp() {
        long res = 0;
        for (int i = 0; i < lArr.length; i++) {
            res += InterfaceBox.box_sharp(lArr[i]).content.value();
        }
        return res;
    }

    @Run(test = "test109_sharp")
    @Warmup(10000) // Make sure interface calls are inlined
    public void test109_sharp_verifier() {
        long res = test109_sharp();
        Asserts.assertEquals(res, 5*rL);
    }

    // Same as test109 but with ObjectBox
    @Test
    @IR(failOn = {ALLOC_G, MEMBAR},
        counts = {PREDICATE_TRAP, "= 1"})
    public long test110() {
        long res = 0;
        for (int i = 0; i < lArr.length; i++) {
            res += ((WrapperInterface)ObjectBox.box(lArr[i]).content).value();
        }
        return res;
    }

    @Run(test = "test110")
    @Warmup(10000) // Make sure interface calls are inlined
    public void test110_verifier() {
        long res = test110();
        Asserts.assertEquals(res, 5*rL);
    }

    @Test
    @IR(failOn = {ALLOC_G, MEMBAR},
        counts = {PREDICATE_TRAP, "= 1"})
    public long test110_sharp() {
        long res = 0;
        for (int i = 0; i < lArr.length; i++) {
            res += ((WrapperInterface)ObjectBox.box_sharp(lArr[i]).content).value();
        }
        return res;
    }

    @Run(test = "test110_sharp")
    @Warmup(10000) // Make sure interface calls are inlined
    public void test110_sharp_verifier() {
        long res = test110_sharp();
        Asserts.assertEquals(res, 5*rL);
    }

    // Same as test109 but with RefBox
    @Test
    @IR(failOn = {ALLOC_G, MEMBAR},
        counts = {PREDICATE_TRAP, "= 1"})
    public long test111() {
        long res = 0;
        for (int i = 0; i < lArr.length; i++) {
            res += RefBox.box(lArr[i]).content.value();
        }
        return res;
    }

    @Run(test = "test111")
    public void test111_verifier() {
        long res = test111();
        Asserts.assertEquals(res, 5*rL);
    }

    @Test
    @IR(failOn = {ALLOC_G, MEMBAR},
        counts = {PREDICATE_TRAP, "= 1"})
    public long test111_sharp() {
        long res = 0;
        for (int i = 0; i < lArr.length; i++) {
            res += RefBox.box_sharp(lArr[i]).content.value();
        }
        return res;
    }

    @Run(test = "test111_sharp")
    public void test111_sharp_verifier() {
        long res = test111_sharp();
        Asserts.assertEquals(res, 5*rL);
    }

    // Same as test109 but with InlineBox
    @Test
    @IR(failOn = {ALLOC_G, MEMBAR},
        counts = {PREDICATE_TRAP, "= 1"})
    public long test112() {
        long res = 0;
        for (int i = 0; i < lArr.length; i++) {
            res += InlineBox.box(lArr[i]).content.value();
        }
        return res;
    }

    @Run(test = "test112")
    public void test112_verifier() {
        long res = test112();
        Asserts.assertEquals(res, 5*rL);
    }

    // Same as test109 but with GenericBox
    @Test
    @IR(failOn = {ALLOC_G, MEMBAR},
        counts = {PREDICATE_TRAP, "= 1"})
    public long test113() {
        long res = 0;
        for (int i = 0; i < lArr.length; i++) {
            res += GenericBox.box(lArr[i]).content.value();
        }
        return res;
    }

    @Run(test = "test113")
    @Warmup(10000) // Make sure interface calls are inlined
    public void test113_verifier() {
        long res = test113();
        Asserts.assertEquals(res, 5*rL);
    }

    @Test
    @IR(failOn = {ALLOC_G, MEMBAR},
        counts = {PREDICATE_TRAP, "= 1"})
    public long test113_sharp() {
        long res = 0;
        for (int i = 0; i < lArr.length; i++) {
            res += GenericBox.box_sharp(lArr[i]).content.value();
        }
        return res;
    }

    @Run(test = "test113_sharp")
    @Warmup(10000) // Make sure interface calls are inlined
    public void test113_sharp_verifier() {
        long res = test113_sharp();
        Asserts.assertEquals(res, 5*rL);
    }

    static interface WrapperInterface2 {
        public long value();

        static final InlineWrapper.ref ZERO = new InlineWrapper(0);

        @ForceInline
        public static WrapperInterface2 wrap(long val) {
            return (val == 0) ? ZERO.content : new LongWrapper2(val);
        }

        @ForceInline
        public static WrapperInterface2 wrap_default(long val) {
            return (val == 0) ? LongWrapper2.default : new LongWrapper2(val);
        }
    }

    static primitive class LongWrapper2 implements WrapperInterface2 {
        private long val;

        @ForceInline
        public LongWrapper2(long val) {
            this.val = val;
        }

        @ForceInline
        public long value() {
            return val;
        }
    }

    static primitive class InlineWrapper {
        WrapperInterface2 content;

        @ForceInline
        public InlineWrapper(long val) {
            content = new LongWrapper2(val);
        }
    }

    static class InterfaceBox2 {
        WrapperInterface2 content;

        @ForceInline
        public InterfaceBox2(long val, boolean def) {
            this.content = def ? WrapperInterface2.wrap_default(val) : WrapperInterface2.wrap(val);
        }

        @ForceInline
        static InterfaceBox2 box(long val) {
            return new InterfaceBox2(val, false);
        }

        @ForceInline
        static InterfaceBox2 box_default(long val) {
            return new InterfaceBox2(val, true);
        }
    }

    // Same as tests above but with ZERO hidden in field of another inline type
    @Test
    @IR(failOn = {ALLOC_G, MEMBAR},
        counts = {PREDICATE_TRAP, "= 1"})
    public long test114() {
        long res = 0;
        for (int i = 0; i < lArr.length; i++) {
            res += InterfaceBox2.box(lArr[i]).content.value();
        }
        return res;
    }

    @Run(test = "test114")
    @Warmup(10000)
    public void test114_verifier() {
        long res = test114();
        Asserts.assertEquals(res, 5*rL);
    }

    // Same as test114 but with .default instead of ZERO field
    @Test
    @IR(failOn = {ALLOC_G, MEMBAR},
        counts = {PREDICATE_TRAP, "= 1"})
    public long test115() {
        long res = 0;
        for (int i = 0; i < lArr.length; i++) {
            res += InterfaceBox2.box_default(lArr[i]).content.value();
        }
        return res;
    }

    @Run(test = "test115")
    @Warmup(10000)
    public void test115_verifier() {
        long res = test115();
        Asserts.assertEquals(res, 5*rL);
    }

    static MyValueEmpty     fEmpty1;
    static MyValueEmpty.ref fEmpty2 = MyValueEmpty.default;
           MyValueEmpty     fEmpty3;
           MyValueEmpty.ref fEmpty4 = MyValueEmpty.default;

    // Test fields loads/stores with empty inline types
    @Test
    @IR(failOn = {ALLOC_G, TRAP})
    public void test116() {
        fEmpty1 = fEmpty4;
        fEmpty2 = fEmpty1;
        fEmpty3 = fEmpty2;
        fEmpty4 = fEmpty3;
    }

    @Run(test = "test116")
    public void test116_verifier() {
        test116();
        Asserts.assertEquals(fEmpty1, fEmpty2);
        Asserts.assertEquals(fEmpty2, fEmpty3);
        Asserts.assertEquals(fEmpty3, fEmpty4);
    }

    // Test array loads/stores with empty inline types
    @Test
    @IR(failOn = {ALLOC_G})
    public MyValueEmpty test117(MyValueEmpty[] arr1, MyValueEmpty.ref[] arr2) {
        arr1[0] = arr2[0];
        arr2[0] = new MyValueEmpty();
        return arr1[0];
    }

    @Run(test = "test117")
    public void test117_verifier() {
        MyValueEmpty[] arr1 = new MyValueEmpty[]{MyValueEmpty.default};
        MyValueEmpty res = test117(arr1, arr1);
        Asserts.assertEquals(res, MyValueEmpty.default);
        Asserts.assertEquals(arr1[0], MyValueEmpty.default);
    }

    // Test acmp with empty inline types
    @Test
    @IR(failOn = {ALLOC_G})
    public boolean test118(MyValueEmpty v1, MyValueEmpty.ref v2, Object o1) {
        return (v1 == v2) && (v2 == o1);
    }

    @Run(test = "test118")
    public void test118_verifier() {
        boolean res = test118(MyValueEmpty.default, MyValueEmpty.default, new MyValueEmpty());
        Asserts.assertTrue(res);
    }

    static primitive class EmptyContainer {
        private MyValueEmpty empty = MyValueEmpty.default;
    }

    static primitive class MixedContainer {
        public int val = rI;
        private EmptyContainer empty = EmptyContainer.default;
    }

    // Test re-allocation of empty inline type array during deoptimization
    @Test
    @IR(failOn = {ALLOC_G})
    public void test119(boolean deopt, Method m) {
        MyValueEmpty[]   array1 = new MyValueEmpty[]{MyValueEmpty.default};
        EmptyContainer[] array2 = new EmptyContainer[]{EmptyContainer.default};
        MixedContainer[] array3 = new MixedContainer[]{MixedContainer.default};
        if (deopt) {
            // uncommon trap
            TestFramework.deoptimize(m);
        }
        Asserts.assertEquals(array1[0], MyValueEmpty.default);
        Asserts.assertEquals(array2[0], EmptyContainer.default);
        Asserts.assertEquals(array3[0], MixedContainer.default);
    }

    @Run(test = "test119")
    public void test119_verifier(RunInfo info) {
        test119(!info.isWarmUp(), info.getTest());
    }

    // Test removal of empty inline type field stores
    @Test
    @IR(failOn = {ALLOC_G, LOAD, STORE, FIELD_ACCESS, NULL_CHECK_TRAP, TRAP})
    public void test120(MyValueEmpty empty) {
        fEmpty1 = empty;
        fEmpty3 = empty;
        // fEmpty2 and fEmpty4 could be null, store can't be removed
    }

    @Run(test = "test120")
    public void test120_verifier() {
        test120(MyValueEmpty.default);
        Asserts.assertEquals(fEmpty1, MyValueEmpty.default);
        Asserts.assertEquals(fEmpty2, MyValueEmpty.default);
    }

    // Test removal of empty inline type field loads
    @Test
    @IR(failOn = {ALLOC_G, LOAD, STORE, FIELD_ACCESS, NULL_CHECK_TRAP, TRAP})
    public boolean test121() {
        return fEmpty1.equals(fEmpty3);
        // fEmpty2 and fEmpty4 could be null, load can't be removed
    }

    @Run(test = "test121")
    public void test121_verifier() {
        boolean res = test121();
        Asserts.assertTrue(res);
    }

    // Verify that empty inline type field loads check for null holder
    @Test
    @IR(failOn = {ALLOC_G})
    public MyValueEmpty test122(TestLWorld t) {
        return t.fEmpty3;
    }

    @Run(test = "test122")
    public void test122_verifier() {
        MyValueEmpty res = test122(this);
        Asserts.assertEquals(res, MyValueEmpty.default);
        try {
            test122(null);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    // Verify that empty inline type field stores check for null holder
    @Test
    @IR(failOn = {ALLOC_G})
    public void test123(TestLWorld t) {
        t.fEmpty3 = MyValueEmpty.default;
    }

    @Run(test = "test123")
    public void test123_verifier() {
        test123(this);
        Asserts.assertEquals(fEmpty3, MyValueEmpty.default);
        try {
            test123(null);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    // acmp doesn't need substitutability test when one input is known
    // not to be a value type
    @Test
    @IR(failOn = SUBSTITUTABILITY_TEST)
    public boolean test124(Integer o1, Object o2) {
        return o1 == o2;
    }

    @Run(test = "test124")
    public void test124_verifier() {
        test124(42, 42);
        test124(42, testValue1);
    }

    // acmp doesn't need substitutability test when one input null
    @Test
    @IR(failOn = {SUBSTITUTABILITY_TEST})
    public boolean test125(Object o1) {
        Object o2 = null;
        return o1 == o2;
    }

    @Run(test = "test125")
    public void test125_verifier() {
        test125(testValue1);
        test125(null);
    }

    // Test inline type that can only be scalarized after loop opts
    @Test
    @IR(failOn = {ALLOC_G, LOAD, STORE})
    public long test126(boolean trap) {
        MyValue2 nonNull = MyValue2.createWithFieldsInline(rI, rD);
        MyValue2.ref val = null;

        for (int i = 0; i < 4; i++) {
            if ((i % 2) == 0) {
                val = nonNull;
            }
        }
        // 'val' is always non-null here but that's only known after loop opts
        if (trap) {
            // Uncommon trap with an inline input that can only be scalarized after loop opts
            return val.hash();
        }
        return 0;
    }

    @Run(test = "test126")
    @Warmup(10000)
    public void test126_verifier(RunInfo info) {
        long res = test126(false);
        Asserts.assertEquals(res, 0L);
        if (!info.isWarmUp()) {
            res = test126(true);
            Asserts.assertEquals(res, testValue2.hash());
        }
    }

    // Same as test126 but with interface type
    @Test
    @IR(failOn = {ALLOC_G, LOAD, STORE})
    public long test127(boolean trap) {
        MyValue2 nonNull = MyValue2.createWithFieldsInline(rI, rD);
        MyInterface val = null;

        for (int i = 0; i < 4; i++) {
            if ((i % 2) == 0) {
                val = nonNull;
            }
        }
        // 'val' is always non-null here but that's only known after loop opts
        if (trap) {
            // Uncommon trap with an inline input that can only be scalarized after loop opts
            return val.hash();
        }
        return 0;
    }

    @Run(test = "test127")
    @Warmup(10000)
    public void test127_verifier(RunInfo info) {
        long res = test127(false);
        Asserts.assertEquals(res, 0L);
        if (!info.isWarmUp()) {
            res = test127(true);
            Asserts.assertEquals(res, testValue2.hash());
        }
    }

    // Test inline type that can only be scalarized after CCP
    @Test
    @IR(failOn = {ALLOC_G, LOAD, STORE})
    public long test128(boolean trap) {
        MyValue2 nonNull = MyValue2.createWithFieldsInline(rI, rD);
        MyValue2.ref val = null;

        int limit = 2;
        for (; limit < 4; limit *= 2);
        for (int i = 2; i < limit; i++) {
            val = nonNull;
        }
        // 'val' is always non-null here but that's only known after CCP
        if (trap) {
            // Uncommon trap with an inline input that can only be scalarized after CCP
            return val.hash();
        }
        return 0;
    }

    @Run(test = "test128")
    @Warmup(10000)
    public void test128_verifier(RunInfo info) {
        long res = test128(false);
        Asserts.assertEquals(res, 0L);
        if (!info.isWarmUp()) {
            res = test128(true);
            Asserts.assertEquals(res, testValue2.hash());
        }
    }

    // Same as test128 but with interface type
    @Test
    @IR(failOn = {ALLOC_G, LOAD, STORE})
    public long test129(boolean trap) {
        MyValue2 nonNull = MyValue2.createWithFieldsInline(rI, rD);
        MyInterface val = null;

        int limit = 2;
        for (; limit < 4; limit *= 2);
        for (int i = 0; i < limit; i++) {
            val = nonNull;
        }
        // 'val' is always non-null here but that's only known after CCP
        if (trap) {
            // Uncommon trap with an inline input that can only be scalarized after CCP
            return val.hash();
        }
        return 0;
    }

    @Run(test = "test129")
    @Warmup(10000)
    public void test129_verifier(RunInfo info) {
        long res = test129(false);
        Asserts.assertEquals(res, 0L);
        if (!info.isWarmUp()) {
            res = test129(true);
            Asserts.assertEquals(res, testValue2.hash());
        }
    }

    // Lock on inline type (known after inlining)
    @ForceInline
    public Object test130_inlinee() {
        return MyValue1.createWithFieldsInline(rI, rL);
    }

    @Test
    @IR(failOn = {ALLOC, LOAD, STORE})
    public void test130() {
        Object obj = test130_inlinee();
        synchronized (obj) {
            throw new RuntimeException("test130 failed: synchronization on inline type should not succeed");
        }
    }

    @Run(test = "test130")
    public void test130_verifier() {
        try {
            test130();
            throw new RuntimeException("test130 failed: no exception thrown");
        } catch (IllegalMonitorStateException ex) {
            // Expected
        }
    }

    // Same as test130 but with field load instead of allocation
    @ForceInline
    public Object test131_inlinee() {
        return testValue1;
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public void test131() {
        Object obj = test131_inlinee();
        synchronized (obj) {
            throw new RuntimeException("test131 failed: synchronization on inline type should not succeed");
        }
    }

    @Run(test = "test131")
    public void test131_verifier() {
        try {
            test131();
            throw new RuntimeException("test131 failed: no exception thrown");
        } catch (IllegalMonitorStateException ex) {
            // Expected
        }
    }

    // Test locking on object that is known to be an inline type only after CCP
    @Test
    @IR(failOn = {ALLOC, LOAD, STORE})
    public void test132() {
        MyValue2 vt = MyValue2.createWithFieldsInline(rI, rD);
        Object obj = Integer.valueOf(42);

        int limit = 2;
        for (; limit < 4; limit *= 2);
        for (int i = 2; i < limit; i++) {
            obj = vt;
        }
        synchronized (obj) {
            throw new RuntimeException("test132 failed: synchronization on inline type should not succeed");
        }
    }

    @Run(test = "test132")
    @Warmup(10000)
    public void test132_verifier() {
        try {
            test132();
            throw new RuntimeException("test132 failed: no exception thrown");
        } catch (IllegalMonitorStateException ex) {
            // Expected
        }
    }

    // Test conditional locking on inline type and non-escaping object
    @Test
    public void test133(boolean b) {
        Object obj = b ? Integer.valueOf(42) : MyValue2.createWithFieldsInline(rI, rD);
        synchronized (obj) {
            if (!b) {
                throw new RuntimeException("test133 failed: synchronization on inline type should not succeed");
            }
        }
    }

    @Run(test = "test133")
    public void test133_verifier() {
        test133(true);
        try {
            test133(false);
            throw new RuntimeException("test133 failed: no exception thrown");
        } catch (IllegalMonitorStateException ex) {
            // Expected
        }
    }

    // Variant with non-scalarized inline type
    @Test
    @IR(failOn = {ALLOC_G})
    public static void test134(boolean b) {
        Object obj = null;
        if (b) {
            obj = MyValue2.createWithFieldsInline(rI, rD);
        }
        synchronized (obj) {

        }
    }

    @Run(test = "test134")
    public void test134_verifier() {
        try {
            test134(true);
            throw new RuntimeException("test134 failed: no exception thrown");
        } catch (IllegalMonitorStateException ex) {
            // Expected
        }
    }

    // Test that acmp of the same inline object is removed
    @Test
    @IR(failOn = {ALLOC_G, LOAD, STORE, NULL_CHECK_TRAP, TRAP})
    public static boolean test135() {
        MyValue1 val = MyValue1.createWithFieldsInline(rI, rL);
        return val == val;
    }

    @Run(test = "test135")
    public void test135_verifier() {
        Asserts.assertTrue(test135());
    }

    // Same as test135 but with .ref
    @Test
    @IR(failOn = {ALLOC_G, LOAD, STORE, NULL_CHECK_TRAP, TRAP})
    public static boolean test136(boolean b) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        if (b) {
            val = null;
        }
        return val == val;
    }

    @Run(test = "test136")
    public void test136_verifier() {
        Asserts.assertTrue(test136(false));
        Asserts.assertTrue(test136(true));
    }

    // Test that acmp of different inline objects with same content is removed
    @Test
    // TODO 8228361
    // @IR(failOn = {ALLOC_G, LOAD, STORE, NULL_CHECK_TRAP, TRAP})
    public static boolean test137(int i) {
        MyValue2 val1 = MyValue2.createWithFieldsInline(i, rD);
        MyValue2 val2 = MyValue2.createWithFieldsInline(i, rD);
        return val1 == val2;
    }

    @Run(test = "test137")
    public void test137_verifier() {
        Asserts.assertTrue(test137(rI));
    }

    // Same as test137 but with .ref
    @Test
    // TODO 8228361
    // @IR(failOn = {ALLOC_G, LOAD, STORE, NULL_CHECK_TRAP, TRAP})
    public static boolean test138(int i, boolean b) {
        MyValue2.ref val1 = MyValue2.createWithFieldsInline(i, rD);
        MyValue2.ref val2 = MyValue2.createWithFieldsInline(i, rD);
        if (b) {
            val1 = null;
            val2 = null;
        }
        return val1 == val2;
    }

    @Run(test = "test138")
    public void test138_verifier() {
        Asserts.assertTrue(test138(rI, false));
        Asserts.assertTrue(test138(rI, true));
    }

    static primitive class Test139Value {
        Object obj = null;
        MyValueEmpty empty = MyValueEmpty.default;
    }

    static primitive class Test139Wrapper {
        Test139Value value = Test139Value.default;
    }

    @Test
    @IR(failOn = {ALLOC_G, LOAD, STORE, TRAP})
    public MyValueEmpty test139() {
        Test139Wrapper w = new Test139Wrapper();
        return w.value.empty;
    }

    @Run(test = "test139")
    public void test139_verifier() {
        MyValueEmpty empty = test139();
        Asserts.assertEquals(empty, MyValueEmpty.default);
    }

    // Test calling a method on a loaded but not linked inline type
    final primitive class Test140Value {
        final int x = 42;
        public int get() {
            return x;
        }
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public int test140() {
        Test140Value vt = Test140Value.default;
        return vt.get();
    }

    @Run(test = "test140")
    @Warmup(0)
    public void test140_verifier() {
        int result = test140();
        Asserts.assertEquals(result, 0);
    }

    // Test calling a method on a linked but not initialized inline type
    final primitive class Test141Value {
        final int x = 42;
        public int get() {
            return x;
        }
    }

    @Test
    @IR(failOn = {ALLOC_G})
    public int test141() {
        Test141Value vt = Test141Value.default;
        return vt.get();
    }

    @Run(test = "test141")
    @Warmup(0)
    public void test141_verifier() {
        int result = test141();
        Asserts.assertEquals(result, 0);
    }

    // Test that virtual calls on inline type receivers are properly inlined
    @Test
    @IR(failOn = {ALLOC_G, LOAD, STORE})
    public long test142() {
        MyValue2 nonNull = MyValue2.createWithFieldsInline(rI, rD);
        MyInterface val = null;

        for (int i = 0; i < 4; i++) {
            if ((i % 2) == 0) {
                val = nonNull;
            }
        }
        return val.hash();
    }

    @Run(test = "test142")
    public void test142_verifier() {
        long res = test142();
        Asserts.assertEquals(res, testValue2.hash());
    }

    public int intField;

    private static final MethodHandle withfieldWithInvalidHolder = InstructionHelper.loadCode(MethodHandles.lookup(),
        "withfieldWithInvalidHolder",
        MethodType.methodType(void.class, TestLWorld.class, int.class),
        CODE -> {
            CODE.
            aload_0().
            iload_1().
            withfield(TestLWorld.class, "intField", "I").
            return_();
        });

    // Test withfield on identity class
    @Test
    public void test143() throws Throwable {
        withfieldWithInvalidHolder.invoke(this, 0);
    }

    @Run(test = "test143")
    @Warmup(10000)
    public void test143_verifier() throws Throwable {
        try {
            test143();
            throw new RuntimeException("IncompatibleClassChangeError expected");
        } catch (IncompatibleClassChangeError e) {
            // Expected
        }
    }

    // Test merging of buffered default and non-default inline types
    @Test
    @IR(failOn = {ALLOC_G})
    public Object test144(int i) {
        if (i == 0) {
            return MyValue1.default;
        } else if (i == 1) {
            return testValue1;
        } else {
            return MyValue1.default;
        }
    }

    @Run(test = "test144")
    public void test144_verifier() {
        Asserts.assertEquals(test144(0), MyValue1.default);
        Asserts.assertEquals(test144(1), testValue1);
        Asserts.assertEquals(test144(2), MyValue1.default);
    }

    // Tests writing an array element with a (statically known) incompatible type
    private static final MethodHandle setArrayElementIncompatibleRef = InstructionHelper.loadCode(MethodHandles.lookup(),
        "setArrayElementIncompatibleRef",
        MethodType.methodType(void.class, TestLWorld.class, MyValue1[].class, int.class, MyValue2.class.asPrimaryType()),
        CODE -> {
            CODE.
            aload_1().
            iload_2().
            aload_3().
            aastore().
            return_();
        });

    // Same as test44 but with .ref store to array
    @Test
    @IR(failOn = {ALLOC_G})
    public void test145(MyValue1[] va, int index, MyValue2.ref v) throws Throwable {
        setArrayElementIncompatibleRef.invoke(this, va, index, v);
    }

    @Run(test = "test145")
    @Warmup(10000)
    public void test145_verifier() throws Throwable {
        int index = Math.abs(rI) % 3;
        try {
            test145(testValue1Array, index, testValue2);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
        Asserts.assertEQ(testValue1Array[index].hash(), hash());
    }

    // Test inline type connected to result node
    @Test
    @IR(failOn = {ALLOC_G})
    public MyValue1 test146(Object obj) {
        return (MyValue1)obj;
    }

    @Run(test = "test146")
    @Warmup(10000)
    public void test146_verifier() {
        Asserts.assertEQ(test146(testValue1), testValue1);
    }

    // Same as test146 but with .ref cast
    @Test
    @IR(failOn = {ALLOC_G})
    public MyValue1.ref test147(Object obj) {
        return (MyValue1.ref)obj;
    }

    @Run(test = "test147")
    @Warmup(10000)
    public void test147_verifier() {
        Asserts.assertEQ(test147(testValue1), testValue1);
        Asserts.assertEQ(test147(null), null);
    }

    @ForceInline
    public Object test148_helper(Object obj) {
        return (MyValue1)obj;
    }

    // Same as test146 but with helper method
    @Test
    public Object test148(Object obj) {
        return test148_helper(obj);
    }

    @Run(test = "test148")
    @Warmup(10000)
    public void test148_verifier() {
        Asserts.assertEQ(test148(testValue1), testValue1);
    }

    @ForceInline
    public Object test149_helper(Object obj) {
        return (MyValue1.ref)obj;
    }

    // Same as test147 but with helper method
    @Test
    public Object test149(Object obj) {
        return test149_helper(obj);
    }

    @Run(test = "test149")
    @Warmup(10000)
    public void test149_verifier() {
        Asserts.assertEQ(test149(testValue1), testValue1);
        Asserts.assertEQ(test149(null), null);
    }

    // Test post-parse call devirtualization with inline type receiver
    @Test
    @IR(applyIf = {"InlineTypePassFieldsAsArgs", "true"},
        failOn = {ALLOC})
    @IR(failOn = {compiler.lib.ir_framework.IRNode.DYNAMIC_CALL_OF_METHOD, "MyValue2::hash"},
        counts = {compiler.lib.ir_framework.IRNode.STATIC_CALL_OF_METHOD, "MyValue2::hash", "= 1"})
    public long test150() {
        MyValue2 val = MyValue2.createWithFieldsInline(rI, rD);
        MyInterface receiver = MyValue1.createWithFieldsInline(rI, rL);

        for (int i = 0; i < 4; i++) {
            if ((i % 2) == 0) {
                receiver = val;
            }
        }
        // Trigger post parse call devirtualization (strength-reducing
        // virtual calls to direct calls).
        return receiver.hash();
    }

    @Run(test = "test150")
    public void test150_verifier() {
        Asserts.assertEquals(test150(), testValue2.hash());
    }

    // Same as test150 but with val not being allocated in the scope of the method
    @Test
    @IR(failOn = {compiler.lib.ir_framework.IRNode.DYNAMIC_CALL_OF_METHOD, "MyValue2::hash"},
        counts = {compiler.lib.ir_framework.IRNode.STATIC_CALL_OF_METHOD, "MyValue2::hash", "= 1"})
    public long test151(MyValue2 val) {
        MyAbstract receiver = MyValue1.createWithFieldsInline(rI, rL);

        for (int i = 0; i < 100; i++) {
            if ((i % 2) == 0) {
                receiver = val;
            }
        }
        // Trigger post parse call devirtualization (strength-reducing
        // virtual calls to direct calls).
        return receiver.hash();
    }

    @Run(test = "test151")
    @Warmup(0) // Make sure there is no receiver type profile
    public void test151_verifier() {
        Asserts.assertEquals(test151(testValue2), testValue2.hash());
    }
}
