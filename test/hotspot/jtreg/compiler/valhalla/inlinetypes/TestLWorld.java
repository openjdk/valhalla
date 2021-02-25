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
import test.java.lang.invoke.lib.InstructionHelper;

/*
 * @test
 * @key randomness
 * @summary Test inline types in LWorld.
 * @library /test/lib /test/jdk/lib/testlibrary/bytecode /test/jdk/java/lang/invoke/common /testlibrary /compiler/whitebox /
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @requires (os.simpleArch == "x64" | os.simpleArch == "aarch64")
 * @compile TestLWorld.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=300 -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                               compiler.valhalla.inlinetypes.InlineTypeTest
 *                               compiler.valhalla.inlinetypes.TestLWorld
 */
public class TestLWorld extends InlineTypeTest {
    // Extra VM parameters for some test scenarios. See InlineTypeTest.getVMParameters()
    @Override
    public String[] getExtraVMParameters(int scenario) {
        switch (scenario) {
        case 2: return new String[] {"-DVerifyIR=false"};
        case 3: return new String[] {"-XX:-MonomorphicArrayCheck", "-XX:FlatArrayElementMaxSize=-1"};
        case 4: return new String[] {"-XX:-MonomorphicArrayCheck"};
        }
        return null;
    }

    public static void main(String[] args) throws Throwable {
        TestLWorld test = new TestLWorld();
        test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class, MyValue3.class,
                 MyValue3Inline.class, Test51Value.class);
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

    @Test()
    public MyValue1 test1() {
        MyValue1 vt = testValue1;
        vt = (MyValue1)test1_dontinline1(vt);
        vt =           test1_dontinline2(vt);
        vt = (MyValue1)test1_inline1(vt);
        vt =           test1_inline2(vt);
        return vt;
    }

    @DontCompile
    public void test1_verifier(boolean warmup) {
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

    @Test()
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

    @DontCompile
    public void test2_verifier(boolean warmup) {
        MyValue1 vt = testValue1;
        MyValue1 def = MyValue1.createDefaultDontInline();
        long result = test2(vt, vt);
        Asserts.assertEQ(result, 11*vt.hash() + 2*def.hashPrimitive());
    }

    // Test merging inline types and objects
    @Test()
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

    @DontCompile
    public void test3_verifier(boolean warmup) {
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
    @Test()
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

    @DontCompile
    public void test4_verifier(boolean warmup) {
        Integer result1 = (Integer)test4(0);
        Asserts.assertEQ(result1, rI);
        int iters = (Math.abs(rI) % 10) + 1;
        MyValue1 result2 = (MyValue1)test4(iters);
        MyValue1 vt = MyValue1.createWithFieldsInline(rI + iters - 1, rL);
        Asserts.assertEQ(result2.hash(), vt.hash());
    }

    // Test inline types in object variables that are live at safepoint
    @Test(failOn = ALLOC + STORE + LOOP)
    public long test5(MyValue1 arg, boolean deopt) {
        Object vt1 = MyValue1.createWithFieldsInline(rI, rL);
        Object vt2 = MyValue1.createWithFieldsDontInline(rI, rL);
        Object vt3 = arg;
        Object vt4 = valueField1;
        if (deopt) {
            // uncommon trap
            WHITE_BOX.deoptimizeMethod(tests.get(getClass().getSimpleName() + "::test5"));
        }
        return ((MyValue1)vt1).hash() + ((MyValue1)vt2).hash() +
               ((MyValue1)vt3).hash() + ((MyValue1)vt4).hash();
    }

    @DontCompile
    public void test5_verifier(boolean warmup) {
        long result = test5(valueField1, !warmup);
        Asserts.assertEQ(result, 4*hash());
    }

    // Test comparing inline types with objects
    @Test(failOn = LOAD + LOOP)
    public boolean test6(Object arg) {
        Object vt = MyValue1.createWithFieldsInline(rI, rL);
        if (vt == arg || vt == (Object)valueField1 || vt == objectField1 || vt == null ||
            arg == vt || (Object)valueField1 == vt || objectField1 == vt || null == vt) {
            return true;
        }
        return false;
    }

    @DontCompile
    public void test6_verifier(boolean warmup) {
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

    @DontCompile
    public void test7_verifier(boolean warmup) {
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

    @DontCompile
    public void test8_verifier(boolean warmup) {
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

    @DontCompile
    public void test9_verifier(boolean warmup) {
        test9();
    }

    // merge of inline types in an object local
    @ForceInline
    public Object test10_helper() {
        return valueField1;
    }

    @Test(failOn = ALLOC + LOAD + STORE)
    public void test10(boolean flag) {
        Object o = null;
        if (flag) {
            o = valueField1;
        } else {
            o = test10_helper();
        }
        valueField1 = (MyValue1)o;
    }

    @DontCompile
    public void test10_verifier(boolean warmup) {
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

    @Test()
    public MyValue1 test11() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        vt = (MyValue1)test11_dontinline1(vt);
        vt =           test11_dontinline2(vt);
        vt = (MyValue1)test11_inline1(vt);
        vt =           test11_inline2(vt);
        return vt;
    }

    @DontCompile
    public void test11_verifier(boolean warmup) {
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

    @Test()
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

    @DontCompile
    public void test12_verifier(boolean warmup) {
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
    @Test()
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

    @DontCompile
    public void test13_verifier(boolean warmup) {
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
    @Test()
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

    @DontCompile
    public void test14_verifier(boolean warmup) {
        MyObject1 result1 = (MyObject1)test14(0);
        Asserts.assertEQ(result1.x, rI);
        int iters = (Math.abs(rI) % 10) + 1;
        MyValue1 result2 = (MyValue1)test14(iters);
        MyValue1 vt = MyValue1.createWithFieldsInline(rI + iters - 1, rL);
        Asserts.assertEQ(result2.hash(), vt.hash());
    }

    // Test inline types in interface variables that are live at safepoint
    @Test(failOn = ALLOC + STORE + LOOP)
    public long test15(MyValue1 arg, boolean deopt) {
        MyInterface vt1 = MyValue1.createWithFieldsInline(rI, rL);
        MyInterface vt2 = MyValue1.createWithFieldsDontInline(rI, rL);
        MyInterface vt3 = arg;
        MyInterface vt4 = valueField1;
        if (deopt) {
            // uncommon trap
            WHITE_BOX.deoptimizeMethod(tests.get(getClass().getSimpleName() + "::test15"));
        }
        return ((MyValue1)vt1).hash() + ((MyValue1)vt2).hash() +
               ((MyValue1)vt3).hash() + ((MyValue1)vt4).hash();
    }

    @DontCompile
    public void test15_verifier(boolean warmup) {
        long result = test15(valueField1, !warmup);
        Asserts.assertEQ(result, 4*hash());
    }

    // Test comparing inline types with interfaces
    @Test(failOn = LOAD + LOOP)
    public boolean test16(Object arg) {
        MyInterface vt = MyValue1.createWithFieldsInline(rI, rL);
        if (vt == arg || vt == (MyInterface)valueField1 || vt == interfaceField1 || vt == null ||
            arg == vt || (MyInterface)valueField1 == vt || interfaceField1 == vt || null == vt) {
            return true;
        }
        return false;
    }

    @DontCompile
    public void test16_verifier(boolean warmup) {
        boolean result = test16(null);
        Asserts.assertFalse(result);
    }

    // Test subtype check when casting to inline type
    @Test
    public MyValue1 test17(MyValue1 vt, Object obj) {
        try {
            vt = (MyValue1)obj;
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException e) {
            // Expected
        }
        return vt;
    }

    @DontCompile
    public void test17_verifier(boolean warmup) {
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

    @DontCompile
    public void test18_verifier(boolean warmup) {
        MyValue1 vt = testValue1;
        MyValue1 result = test18(vt);
        Asserts.assertEquals(result.hash(), vt.hash());
    }

    @Test
    public void test19(MyValue1 vt) {
        Object obj = vt;
        try {
            MyValue2 vt2 = (MyValue2)obj;
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    @DontCompile
    public void test19_verifier(boolean warmup) {
        test19(valueField1);
    }

    @Test
    public void test20(MyValue1 vt) {
        Object obj = vt;
        try {
            Integer i = (Integer)obj;
            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    @DontCompile
    public void test20_verifier(boolean warmup) {
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
    @Test()
    public Object test21(Object[] oa, int index) {
        return oa[index];
    }

    @DontCompile
    public void test21_verifier(boolean warmup) {
        MyValue1 result = (MyValue1)test21(testValue1Array, Math.abs(rI) % 3);
        Asserts.assertEQ(result.hash(), hash());
    }

    // Test load from (flattened) inline type array disguised as interface array
    @Test()
    public Object test22Interface(MyInterface[] ia, int index) {
        return ia[index];
    }

    @DontCompile
    public void test22Interface_verifier(boolean warmup) {
        MyValue1 result = (MyValue1)test22Interface(testValue1Array, Math.abs(rI) % 3);
        Asserts.assertEQ(result.hash(), hash());
    }

    // Test load from (flattened) inline type array disguised as abstract array
    @Test()
    public Object test22Abstract(MyAbstract[] ia, int index) {
        return ia[index];
    }

    @DontCompile
    public void test22Abstract_verifier(boolean warmup) {
        MyValue1 result = (MyValue1)test22Abstract(testValue1Array, Math.abs(rI) % 3);
        Asserts.assertEQ(result.hash(), hash());
    }

    // Test inline store to (flattened) inline type array disguised as object array
    @ForceInline
    public void test23_inline(Object[] oa, Object o, int index) {
        oa[index] = o;
    }

    @Test()
    public void test23(Object[] oa, MyValue1 vt, int index) {
        test23_inline(oa, vt, index);
    }

    @DontCompile
    public void test23_verifier(boolean warmup) {
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

    @Test()
    public void test24(Object[] oa, MyValue1 vt, int index) {
        test24_inline(oa, vt, index);
    }

    @DontCompile
    public void test24_verifier(boolean warmup) {
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

    @Test()
    public void test25(Object[] oa, MyValue1 vt, int index) {
        test25_inline(oa, vt, index);
    }

    @DontCompile
    public void test25_verifier(boolean warmup) {
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

    @Test()
    public void test26Interface(MyInterface[] ia, MyValue1 vt, int index) {
      test26Interface_inline(ia, vt, index);
    }

    @DontCompile
    public void test26Interface_verifier(boolean warmup) {
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

    @Test()
    public void test27Interface(MyInterface[] ia, MyValue1 vt, int index) {
        test27Interface_inline(ia, vt, index);
    }

    @DontCompile
    public void test27Interface_verifier(boolean warmup) {
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

    @Test()
    public void test26Abstract(MyAbstract[] ia, MyValue1 vt, int index) {
      test26Abstract_inline(ia, vt, index);
    }

    @DontCompile
    public void test26Abstract_verifier(boolean warmup) {
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

    @Test()
    public void test27Abstract(MyAbstract[] ia, MyValue1 vt, int index) {
        test27Abstract_inline(ia, vt, index);
    }

    @DontCompile
    public void test27Abstract_verifier(boolean warmup) {
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

    @Test()
    public void test28(Object[] oa, Object o, int index) {
        test28_inline(oa, o, index);
    }

    @DontCompile
    public void test28_verifier(boolean warmup) {
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

    @Test()
    public void test29(Object[] oa, Object o, int index) {
        test29_inline(oa, o, index);
    }

    @DontCompile
    public void test29_verifier(boolean warmup) {
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

    @Test()
    public void test30(Object[] oa, Object o, int index) {
        test30_inline(oa, o, index);
    }

    @DontCompile
    public void test30_verifier(boolean warmup) {
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

    @Test()
    public void test31Interface(MyInterface[] ia, MyInterface i, int index) {
        test31Interface_inline(ia, i, index);
    }

    @DontCompile
    public void test31Interface_verifier(boolean warmup) {
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

    @Test()
    public void test32Interface(MyInterface[] ia, MyInterface i, int index) {
        test32Interface_inline(ia, i, index);
    }

    @DontCompile
    public void test32Interface_verifier(boolean warmup) {
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

    @Test()
    public void test31Abstract(MyAbstract[] ia, MyAbstract i, int index) {
        test31Abstract_inline(ia, i, index);
    }

    @DontCompile
    public void test31Abstract_verifier(boolean warmup) {
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

    @Test()
    public void test32Abstract(MyAbstract[] ia, MyAbstract i, int index) {
        test32Abstract_inline(ia, i, index);
    }

    @DontCompile
    public void test32Abstract_verifier(boolean warmup) {
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

    @Test()
    public void test33(Object[] oa, Object o, int index) {
        test33_inline(oa, o, index);
    }

    @DontCompile
    public void test33_verifier(boolean warmup) {
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

    @Test()
    public void test34(Object[] oa, int index) {
        test34_inline(oa, null, index);
    }

    @DontCompile
    public void test34_verifier(boolean warmup) {
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

    @Test()
    public void test35(MyValue1[] va, int index) throws Throwable {
        setArrayElementNull.invoke(this, va, index);
    }

    @DontCompile
    public void test35_verifier(boolean warmup) throws Throwable {
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
    @Test()
    public void test36(MyValue1[] va, MyValue1 vt, int index) {
        va[index] = vt;
    }

    @DontCompile
    public void test36_verifier(boolean warmup) {
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

    @Test()
    public void test37(MyValue1[] va, Object o, int index) {
        test37_inline(va, o, index);
    }

    @DontCompile
    public void test37_verifier(boolean warmup) {
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

    @Test()
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

    @DontCompile
    public void test38_verifier(boolean warmup) {
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
    @Test()
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

    @DontCompile
    public void test39_verifier(boolean warmup) {
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
    @Test()
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

    @DontCompile
    public void test40_verifier(boolean warmup) {
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

    @Test()
    public void test41() {
        MyValue1[] vals = new MyValue1[] {testValue1};
        test41_dontinline(vals[0].oa[0]);
        test41_dontinline(vals[0].oa[0]);
    }

    @DontCompile
    public void test41_verifier(boolean warmup) {
        test41();
    }

    // Test for bug in Escape Analysis
    private static final MyValue1.ref test42VT1 = MyValue1.createWithFieldsInline(rI, rL);
    private static final MyValue1.ref test42VT2 = MyValue1.createWithFieldsInline(rI + 1, rL + 1);

    @Test()
    public void test42() {
        MyValue1[] vals = new MyValue1[] {(MyValue1) test42VT1, (MyValue1) test42VT2};
        Asserts.assertEQ(vals[0].hash(), test42VT1.hash());
        Asserts.assertEQ(vals[1].hash(), test42VT2.hash());
    }

    @DontCompile
    public void test42_verifier(boolean warmup) {
        if (!warmup) test42(); // We need -Xcomp behavior
    }

    // Test for bug in Escape Analysis
    @Test()
    public long test43(boolean deopt) {
        MyValue1[] vals = new MyValue1[] {(MyValue1) test42VT1, (MyValue1) test42VT2};

        if (deopt) {
            // uncommon trap
            WHITE_BOX.deoptimizeMethod(tests.get(getClass().getSimpleName() + "::test43"));
            Asserts.assertEQ(vals[0].hash(), test42VT1.hash());
            Asserts.assertEQ(vals[1].hash(), test42VT2.hash());
        }

        return vals[0].hash();
    }

    @DontCompile
    public void test43_verifier(boolean warmup) {
        test43(!warmup);
    }

    // Tests writing an array element with a (statically known) incompatible type
    private static final MethodHandle setArrayElementIncompatible = InstructionHelper.loadCode(MethodHandles.lookup(),
        "setArrayElementIncompatible",
        MethodType.methodType(void.class, TestLWorld.class, MyValue1[].class, int.class, MyValue2.class),
        CODE -> {
            CODE.
            aload_1().
            iload_2().
            aload_3().
            aastore().
            return_();
        });

    @Test()
    public void test44(MyValue1[] va, int index, MyValue2 v) throws Throwable {
        setArrayElementIncompatible.invoke(this, va, index, v);
    }

    @DontCompile
    public void test44_verifier(boolean warmup) throws Throwable {
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

    @Test()
    public void test45(MyValue1[] va, int index, MyValue2 v) throws Throwable {
        test45_inline(va, v, index);
    }

    @DontCompile
    public void test45_verifier(boolean warmup) throws Throwable {
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
    public boolean test46(MyValue1 vt) {
        Object obj = vt;
        return obj instanceof MyValue1;
    }

    @DontCompile
    public void test46_verifier(boolean warmup) {
        MyValue1 vt = testValue1;
        boolean result = test46(vt);
        Asserts.assertTrue(result);
    }

    @Test
    public boolean test47(MyValue1 vt) {
        Object obj = vt;
        return obj instanceof MyValue2;
    }

    @DontCompile
    public void test47_verifier(boolean warmup) {
        MyValue1 vt = testValue1;
        boolean result = test47(vt);
        Asserts.assertFalse(result);
    }

    @Test
    public boolean test48(Object obj) {
        return obj instanceof MyValue1;
    }

    @DontCompile
    public void test48_verifier(boolean warmup) {
        MyValue1 vt = testValue1;
        boolean result = test48(vt);
        Asserts.assertTrue(result);
    }

    @Test
    public boolean test49(Object obj) {
        return obj instanceof MyValue2;
    }

    @DontCompile
    public void test49_verifier(boolean warmup) {
        MyValue1 vt = testValue1;
        boolean result = test49(vt);
        Asserts.assertFalse(result);
    }

    @Test
    public boolean test50(Object obj) {
        return obj instanceof MyValue1;
    }

    @DontCompile
    public void test50_verifier(boolean warmup) {
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
    @Test()
    public long test51(Test51Value holder, MyValue1 vt1, Object vt2) {
        return holder.test(holder, vt1, vt2);
    }

    @DontCompile
    public void test51_verifier(boolean warmup) {
        MyValue1 vt = testValue1;
        MyValue1 def = MyValue1.createDefaultDontInline();
        Test51Value holder = new Test51Value();
        Asserts.assertEQ(testValue1.hash(), vt.hash());
        Asserts.assertEQ(holder.valueField1.hash(), vt.hash());
        long result = test51(holder, vt, vt);
        Asserts.assertEQ(result, 9*vt.hash() + def.hashPrimitive());
    }

    // Access non-flattened, uninitialized inline type field with inline type holder
    @Test()
    public void test52(Test51Value holder) {
        if ((Object)holder.valueField5 != null) {
            throw new RuntimeException("Should be null");
        }
    }

    @DontCompile
    public void test52_verifier(boolean warmup) {
        Test51Value vt = Test51Value.default;
        test52(vt);
    }

    // Merging inline types of different types
    @Test()
    public Object test53(Object o, boolean b) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        return b ? vt : o;
    }

    @DontCompile
    public void test53_verifier(boolean warmup) {
        test53(new Object(), false);
        MyValue1 result = (MyValue1)test53(new Object(), true);
        Asserts.assertEQ(result.hash(), hash());
    }

    @Test()
    public Object test54(boolean b) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        return b ? vt : testValue2;
    }

    @DontCompile
    public void test54_verifier(boolean warmup) {
        MyValue1 result1 = (MyValue1)test54(true);
        Asserts.assertEQ(result1.hash(), hash());
        MyValue2 result2 = (MyValue2)test54(false);
        Asserts.assertEQ(result2.hash(), testValue2.hash());
    }

    @Test()
    public Object test55(boolean b) {
        MyValue1 vt1 = MyValue1.createWithFieldsInline(rI, rL);
        MyValue2 vt2 = MyValue2.createWithFieldsInline(rI, rD);
        return b ? vt1 : vt2;
    }

    @DontCompile
    public void test55_verifier(boolean warmup) {
        MyValue1 result1 = (MyValue1)test55(true);
        Asserts.assertEQ(result1.hash(), hash());
        MyValue2 result2 = (MyValue2)test55(false);
        Asserts.assertEQ(result2.hash(), testValue2.hash());
    }

    // Test synchronization on inline types
    @Test()
    public void test56(Object vt) {
        synchronized (vt) {
            throw new RuntimeException("test56 failed: synchronization on inline type should not succeed");
        }
    }

    @DontCompile
    public void test56_verifier(boolean warmup) {
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

    @Test()
    public void test57(MyValue1 vt) {
        test57_inline(vt);
    }

    @DontCompile
    public void test57_verifier(boolean warmup) {
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

    @Test()
    public void test58() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        test58_inline(vt);
    }

    @DontCompile
    public void test58_verifier(boolean warmup) {
        try {
            test58();
            throw new RuntimeException("test58 failed: no exception thrown");
        } catch (IllegalMonitorStateException ex) {
            // Expected
        }
    }

    @Test()
    public void test59(Object o, boolean b) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        Object sync = b ? vt : o;
        synchronized (sync) {
            if (b) {
                throw new RuntimeException("test59 failed: synchronization on inline type should not succeed");
            }
        }
    }

    @DontCompile
    public void test59_verifier(boolean warmup) {
        test59(new Object(), false);
        try {
            test59(new Object(), true);
            throw new RuntimeException("test59 failed: no exception thrown");
        } catch (IllegalMonitorStateException ex) {
            // Expected
        }
    }

    @Test()
    public void test60(boolean b) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        Object sync = b ? vt : testValue2;
        synchronized (sync) {
            throw new RuntimeException("test60 failed: synchronization on inline type should not succeed");
        }
    }

    @DontCompile
    public void test60_verifier(boolean warmup) {
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
    @Test()
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

    @DontCompile
    public void test61_verifier(boolean warmup) {
        test61(testValue1);
    }

    @Test()
    public void test62(Object o) {
        try {
            synchronized (o) { }
        } catch (IllegalMonitorStateException ex) {
            // Expected
            return;
        }
        throw new RuntimeException("test62 failed: no exception thrown");
    }

    @DontCompile
    public void test62_verifier(boolean warmup) {
        test62(testValue1);
    }

    // Test synchronization without any instructions in the synchronized block
    @Test()
    public void test63(Object o) {
        synchronized (o) { }
    }

    @DontCompile
    public void test63_verifier(boolean warmup) {
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

    @Test()
    public MyInterface test64Interface(MyValue1 vt) {
        return test64Interface_helper(vt);
    }

    @DontCompile
    public void test64Interface_verifier(boolean warmup) {
        test64Interface(testValue1);
    }

    // type system test with abstract and inline type
    @ForceInline
    public MyAbstract test64Abstract_helper(MyValue1 vt) {
        return vt;
    }

    @Test()
    public MyAbstract test64Abstract(MyValue1 vt) {
        return test64Abstract_helper(vt);
    }

    @DontCompile
    public void test64Abstract_verifier(boolean warmup) {
        test64Abstract(testValue1);
    }

    // Array store tests
    @Test()
    public void test65(Object[] array, MyValue1 vt) {
        array[0] = vt;
    }

    @DontCompile
    public void test65_verifier(boolean warmup) {
        Object[] array = new Object[1];
        test65(array, testValue1);
        Asserts.assertEQ(((MyValue1)array[0]).hash(), testValue1.hash());
    }

    @Test()
    public void test66(Object[] array, MyValue1 vt) {
        array[0] = vt;
    }

    @DontCompile
    public void test66_verifier(boolean warmup) {
        MyValue1[] array = new MyValue1[1];
        test66(array, testValue1);
        Asserts.assertEQ(array[0].hash(), testValue1.hash());
    }

    @Test()
    public void test67(Object[] array, Object vt) {
        array[0] = vt;
    }

    @DontCompile
    public void test67_verifier(boolean warmup) {
        MyValue1[] array = new MyValue1[1];
        test67(array, testValue1);
        Asserts.assertEQ(array[0].hash(), testValue1.hash());
    }

    @Test()
    public void test68(Object[] array, Integer o) {
        array[0] = o;
    }

    @DontCompile
    public void test68_verifier(boolean warmup) {
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

    @Test(failOn = ALLOC + STORE)
    public int test69(MyValue1[] array) {
        MyValue1 result = MyValue1.createDefaultInline();
        for (int i = 0; i < array.length; ++i) {
            result = (MyValue1)test69_sum(result, array[i]);
        }
        return result.x;
    }

    @DontCompile
    public void test69_verifier(boolean warmup) {
        int result = test69(testValue1Array);
        Asserts.assertEQ(result, rI * testValue1Array.length);
    }

    // Same as test69 but with an Interface
    @ForceInline
    public MyInterface test70Interface_sum(MyInterface a, MyInterface b) {
        int sum = ((MyValue1)a).x + ((MyValue1)b).x;
        return MyValue1.setX(((MyValue1)a), sum);
    }

    @Test(failOn = ALLOC + STORE)
    public int test70Interface(MyValue1[] array) {
        MyValue1 result = MyValue1.createDefaultInline();
        for (int i = 0; i < array.length; ++i) {
            result = (MyValue1)test70Interface_sum(result, array[i]);
        }
        return result.x;
    }

    @DontCompile
    public void test70Interface_verifier(boolean warmup) {
        int result = test70Interface(testValue1Array);
        Asserts.assertEQ(result, rI * testValue1Array.length);
    }

    // Same as test69 but with an Abstract
    @ForceInline
    public MyAbstract test70Abstract_sum(MyAbstract a, MyAbstract b) {
        int sum = ((MyValue1)a).x + ((MyValue1)b).x;
        return MyValue1.setX(((MyValue1)a), sum);
    }

    @Test(failOn = ALLOC + STORE)
    public int test70Abstract(MyValue1[] array) {
        MyValue1 result = MyValue1.createDefaultInline();
        for (int i = 0; i < array.length; ++i) {
            result = (MyValue1)test70Abstract_sum(result, array[i]);
        }
        return result.x;
    }

    @DontCompile
    public void test70Abstract_verifier(boolean warmup) {
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
    public MyValue1 test71() {
        return test71_inline(null);
    }

    @DontCompile
    public void test71_verifier(boolean warmup) {
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
    @Warmup(0)
    public int test72() {
        Test72Value vt = Test72Value.default;
        return vt.get();
    }

    @DontCompile
    public void test72_verifier(boolean warmup) {
        int result = test72();
        Asserts.assertEquals(result, 0);
    }

    // Tests for loading/storing unkown values
    @Test
    public Object test73(Object[] va) {
        return va[0];
    }

    @DontCompile
    public void test73_verifier(boolean warmup) {
        MyValue1 vt = (MyValue1)test73(testValue1Array);
        Asserts.assertEquals(testValue1Array[0].hash(), vt.hash());
    }

    @Test
    public void test74(Object[] va, Object vt) {
        va[0] = vt;
    }

    @DontCompile
    public void test74_verifier(boolean warmup) {
        MyValue1[] va = new MyValue1[1];
        test74(va, testValue1);
        Asserts.assertEquals(va[0].hash(), testValue1.hash());
    }

    // Verify that mixing instances and arrays with the clone api
    // doesn't break anything
    @Test
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

    @DontCompile
    public void test75_verifier(boolean warmup) {
        test75(42);
    }

    // Casting a null Integer to a (non-nullable) inline type should throw a NullPointerException
    @ForceInline
    public MyValue1 test76_helper(Object o) {
        return (MyValue1)o;
    }

    @Test
    public MyValue1 test76(Integer i) throws Throwable {
        return test76_helper(i);
    }

    @DontCompile
    public void test76_verifier(boolean warmup) throws Throwable {
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
    public MyValue1 test77(Integer i) throws Throwable {
        return test77_helper(i);
    }

    @DontCompile
    public void test77_verifier(boolean warmup) throws Throwable {
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
    public MyValue1.ref test78(Integer i) throws Throwable {
        return test78_helper(i);
    }

    @DontCompile
    public void test78_verifier(boolean warmup) throws Throwable {
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
    public MyValue1.ref test79(Integer i) throws Throwable {
        return test79_helper(i);
    }

    @DontCompile
    public void test79_verifier(boolean warmup) throws Throwable {
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

    @DontCompile
    public void test80_verifier(boolean warmup) throws Throwable {
        long result = test80();
        Asserts.assertEQ(result, rI + 2*rL);
    }

    // Test scalarization with exceptional control flow
    public int test81Callee(MyValue1 vt)  {
        return vt.x;
    }

    @Test(failOn = ALLOC + LOAD + STORE)
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

    @DontCompile
    public void test81_verifier(boolean warmup) {
        int result = test81();
        Asserts.assertEQ(result, 10*rI);
    }

    // Test check for null free array when storing to inline tpye array
    @Test
    public void test82(Object[] dst, Object v) {
        dst[0] = v;
    }

    @DontCompile
    public void test82_verifier(boolean warmup) {
        MyValue2[] dst = new MyValue2[1];
        test82(dst, testValue2);
        if (!warmup) {
            try {
                test82(dst, null);
                throw new RuntimeException("No ArrayStoreException thrown");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    @Test
    @Warmup(10000)
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

    @DontCompile
    public void test83_verifier(boolean warmup) {
        MyValue2[] dst = new MyValue2[1];
        test83(dst, testValue2, false);
        test83(dst, testValue2, true);
        if (!warmup) {
            try {
                test83(dst, null, true);
                throw new RuntimeException("No ArrayStoreException thrown");
            } catch (NullPointerException e) {
                // Expected
            }
        }
    }

    private void rerun_and_recompile_for(String name, int num, Runnable test) {
        Method m = tests.get(name);

        for (int i = 1; i < num; i++) {
            test.run();

            if (!WHITE_BOX.isMethodCompiled(m, false)) {
                enqueueMethodForCompilation(m, COMP_LEVEL_FULL_OPTIMIZATION);
            }
        }
    }

    // Tests for the Loop Unswitching optimization
    // Should make 2 copies of the loop, one for non flattened arrays, one for other cases.
    @Test(match = { COUNTEDLOOP_MAIN }, matchCount = { 2 } )
    @Warmup(0)
    public void test84(Object[] src, Object[] dst) {
        for (int i = 0; i < src.length; i++) {
            dst[i] = src[i];
        }
    }

    @DontCompile
    public void test84_verifier(boolean warmup) {
        MyValue2[] src = new MyValue2[100];
        Arrays.fill(src, testValue2);
        MyValue2[] dst = new MyValue2[100];
        rerun_and_recompile_for("TestLWorld::test84", 10,
                                () ->  { test84(src, dst);
                                         Asserts.assertTrue(Arrays.equals(src, dst)); });
    }

    @Test(valid = G1GCOn, match = { COUNTEDLOOP, LOAD_UNKNOWN_INLINE }, matchCount = { 2, 1 } )
    @Test(valid = G1GCOff, match = { COUNTEDLOOP_MAIN, LOAD_UNKNOWN_INLINE }, matchCount = { 2, 4 } )
    @Warmup(0)
    public void test85(Object[] src, Object[] dst) {
        for (int i = 0; i < src.length; i++) {
            dst[i] = src[i];
        }
    }

    @DontCompile
    public void test85_verifier(boolean warmup) {
        Object[] src = new Object[100];
        Arrays.fill(src, new Object());
        src[0] = null;
        Object[] dst = new Object[100];
        rerun_and_recompile_for("TestLWorld::test85", 10,
                                () -> { test85(src, dst);
                                        Asserts.assertTrue(Arrays.equals(src, dst)); });
    }

    @Test(valid = G1GCOn, match = { COUNTEDLOOP }, matchCount = { 2 } )
    @Test(valid = G1GCOff, match = { COUNTEDLOOP_MAIN }, matchCount = { 2 } )
    @Warmup(0)
    public void test86(Object[] src, Object[] dst) {
        for (int i = 0; i < src.length; i++) {
            dst[i] = src[i];
        }
    }

    @DontCompile
    public void test86_verifier(boolean warmup) {
        MyValue2[] src = new MyValue2[100];
        Arrays.fill(src, testValue2);
        Object[] dst = new Object[100];
        rerun_and_recompile_for("TestLWorld::test86", 10,
                                () -> { test86(src, dst);
                                        Asserts.assertTrue(Arrays.equals(src, dst)); });
    }

    @Test(match = { COUNTEDLOOP_MAIN }, matchCount = { 2 } )
    @Warmup(0)
    public void test87(Object[] src, Object[] dst) {
        for (int i = 0; i < src.length; i++) {
            dst[i] = src[i];
        }
    }

    @DontCompile
    public void test87_verifier(boolean warmup) {
        Object[] src = new Object[100];
        Arrays.fill(src, testValue2);
        MyValue2[] dst = new MyValue2[100];

        rerun_and_recompile_for("TestLWorld::test87", 10,
                                () -> { test87(src, dst);
                                        Asserts.assertTrue(Arrays.equals(src, dst)); });
    }

    @Test(match = { COUNTEDLOOP_MAIN }, matchCount = { 2 } )
    @Warmup(0)
    public void test88(Object[] src1, Object[] dst1, Object[] src2, Object[] dst2) {
        for (int i = 0; i < src1.length; i++) {
            dst1[i] = src1[i];
            dst2[i] = src2[i];
        }
    }

    @DontCompile
    public void test88_verifier(boolean warmup) {
        MyValue2[] src1 = new MyValue2[100];
        Arrays.fill(src1, testValue2);
        MyValue2[] dst1 = new MyValue2[100];
        Object[] src2 = new Object[100];
        Arrays.fill(src2, new Object());
        Object[] dst2 = new Object[100];

        rerun_and_recompile_for("TestLWorld::test88", 10,
                                () -> { test88(src1, dst1, src2, dst2);
                                        Asserts.assertTrue(Arrays.equals(src1, dst1));
                                        Asserts.assertTrue(Arrays.equals(src2, dst2)); });
    }

    @Test
    public boolean test89(Object obj) {
        return obj.getClass() == Integer.class;
    }

    @DontCompile
    public void test89_verifier(boolean warmup) {
        Asserts.assertTrue(test89(Integer.valueOf(42)));
        Asserts.assertFalse(test89(new Object()));
    }

    @Test
    public Integer test90(Object obj) {
        return (Integer)obj;
    }

    @DontCompile
    public void test90_verifier(boolean warmup) {
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

    @DontCompile
    public void test91_verifier(boolean warmup) {
        Asserts.assertTrue(test91(new MyValue2[1]));
        Asserts.assertFalse(test91(new Object()));
    }

    static primitive class Test92Value {
        final int field;
        public Test92Value() {
            field = 0x42;
        }
    }

    @Warmup(10000)
    @Test(match = { CLASS_CHECK_TRAP }, matchCount = { 2 }, failOn = LOAD_UNKNOWN_INLINE + ALLOC_G + MEMBAR)
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

    @DontCompile
    public void test92_verifier(boolean warmup) {
        Object[] array = new Object[1];
        array[0] = 0x42;
        Object result = test92(array);
        Asserts.assertEquals(result, 0x42);
    }

    // If the class check succeeds, the flattened array check that
    // precedes will never succeed and the flat array branch should
    // trigger an uncommon trap.
    @Test
    @Warmup(10000)
    public Object test93(Object[] array) {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
            }
        }

        Object v = (Integer)array[0];
        return v;
    }

    @DontCompile
    public void test93_verifier(boolean warmup) {
        if (warmup) {
            Object[] array = new Object[1];
            array[0] = 0x42;
            Object result = test93(array);
            Asserts.assertEquals(result, 0x42);
        } else {
            Object[] array = new Test92Value[1];
            Method m = tests.get("TestLWorld::test93");
            int extra = 3;
            for (int j = 0; j < extra; j++) {
                for (int i = 0; i < 10; i++) {
                    try {
                        test93(array);
                    } catch (ClassCastException cce) {
                    }
                }
                boolean compiled = isCompiledByC2(m);
                Asserts.assertTrue(!USE_COMPILER || XCOMP || STRESS_CC || TEST_C1 || !ProfileInterpreter || compiled || (j != extra-1));
                if (!compiled) {
                    enqueueMethodForCompilation(m, COMP_LEVEL_FULL_OPTIMIZATION);
                }
            }
        }
    }

    @Warmup(10000)
    @Test(match = { CLASS_CHECK_TRAP, LOOP }, matchCount = { 2, 1 }, failOn = LOAD_UNKNOWN_INLINE + ALLOC_G + MEMBAR)
    public int test94(Object[] array) {
        int res = 0;
        for (int i = 1; i < 4; i *= 2) {
            Object v = array[i];
            res += (Integer)v;
        }
        return res;
    }

    @DontCompile
    public void test94_verifier(boolean warmup) {
        Object[] array = new Object[4];
        array[0] = 0x42;
        array[1] = 0x42;
        array[2] = 0x42;
        array[3] = 0x42;
        int result = test94(array);
        Asserts.assertEquals(result, 0x42 * 2);
    }

    @Warmup(10000)
    @Test
    public boolean test95(Object o1, Object o2) {
        return o1 == o2;
    }

    @DontCompile
    public void test95_verifier(boolean warmup) {
        Object o1 = new Object();
        Object o2 = new Object();
        Asserts.assertTrue(test95(o1, o1));
        Asserts.assertTrue(test95(null, null));
        Asserts.assertFalse(test95(o1, null));
        Asserts.assertFalse(test95(o1, o2));
    }

    @Warmup(10000)
    @Test
    public boolean test96(Object o1, Object o2) {
        return o1 == o2;
    }

    @DontCompile
    public void test96_verifier(boolean warmup) {
        Object o1 = new Object();
        Object o2 = new Object();
        Asserts.assertTrue(test96(o1, o1));
        Asserts.assertFalse(test96(o1, o2));
        if (!warmup) {
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

    @Test()
    public MyValue1 test97() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        vt = (MyValue1)test97_dontinline1(vt);
        vt =           test97_dontinline2(vt);
        vt = (MyValue1)test97_inline1(vt);
        vt =           test97_inline2(vt);
        return vt;
    }

    @DontCompile
    public void test97_verifier(boolean warmup) {
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

    @Test()
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

    @DontCompile
    public void test98_verifier(boolean warmup) {
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
    @Test()
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

    @DontCompile
    public void test99_verifier(boolean warmup) {
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
    @Test()
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

    @DontCompile
    public void test100_verifier(boolean warmup) {
        MyObject2 result1 = (MyObject2)test100(0);
        Asserts.assertEQ(result1.x, rI);
        int iters = (Math.abs(rI) % 10) + 1;
        MyValue1 result2 = (MyValue1)test100(iters);
        MyValue1 vt = MyValue1.createWithFieldsInline(rI + iters - 1, rL);
        Asserts.assertEQ(result2.hash(), vt.hash());
    }

    // Test inline types in abstract class variables that are live at safepoint
    @Test(failOn = ALLOC + STORE + LOOP)
    public long test101(MyValue1 arg, boolean deopt) {
        MyAbstract vt1 = MyValue1.createWithFieldsInline(rI, rL);
        MyAbstract vt2 = MyValue1.createWithFieldsDontInline(rI, rL);
        MyAbstract vt3 = arg;
        MyAbstract vt4 = valueField1;
        if (deopt) {
            // uncommon trap
            WHITE_BOX.deoptimizeMethod(tests.get(getClass().getSimpleName() + "::test101"));
        }
        return ((MyValue1)vt1).hash() + ((MyValue1)vt2).hash() +
               ((MyValue1)vt3).hash() + ((MyValue1)vt4).hash();
    }

    @DontCompile
    public void test101_verifier(boolean warmup) {
        long result = test101(valueField1, !warmup);
        Asserts.assertEQ(result, 4*hash());
    }

    // Test comparing inline types with abstract classes
    @Test(failOn = LOAD + LOOP)
    public boolean test102(Object arg) {
        MyAbstract vt = MyValue1.createWithFieldsInline(rI, rL);
        if (vt == arg || vt == (MyAbstract)valueField1 || vt == abstractField1 || vt == null ||
            arg == vt || (MyAbstract)valueField1 == vt || abstractField1 == vt || null == vt) {
            return true;
        }
        return false;
    }

    @DontCompile
    public void test102_verifier(boolean warmup) {
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
    @Test(failOn = ALLOC_G + MEMBAR + ALLOCA_G + LOAD_UNKNOWN_INLINE + STORE_UNKNOWN_INLINE + INLINE_ARRAY_NULL_GUARD)
    public NoValueImplementors1 test103(NoValueImplementors1[] array, int i) {
        return array[i];
    }

    @DontCompile
    public void test103_verifier(boolean warmup) {
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
    @Test(failOn = ALLOC_G + ALLOCA_G + LOAD_UNKNOWN_INLINE + STORE_UNKNOWN_INLINE + INLINE_ARRAY_NULL_GUARD)
    public NoValueImplementors1 test104(NoValueImplementors1[] array, NoValueImplementors1 v, MyObject3 o, int i) {
        array[0] = v;
        array[1] = array[0];
        array[2] = o;
        return array[i];
    }

    @DontCompile
    public void test104_verifier(boolean warmup) {
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
    @Test(failOn = ALLOC_G + MEMBAR + ALLOCA_G + LOAD_UNKNOWN_INLINE + STORE_UNKNOWN_INLINE + INLINE_ARRAY_NULL_GUARD)
    public NoValueImplementors2 test105(NoValueImplementors2[] array, int i) {
        return array[i];
    }

    @DontCompile
    public void test105_verifier(boolean warmup) {
        NoValueImplementors2[] array1 = new NoValueImplementors2[3];
        MyObject5[] array2 = new MyObject5[3];
        NoValueImplementors2 result = test105(array1, 0);
        Asserts.assertEquals(result, array1[0]);

        result = test105(array2, 1);
        Asserts.assertEquals(result, array1[1]);
    }

    // Storing to an abstract class array does not require a flatness/null check if the abstract class has no inline implementor
    @Test(failOn = ALLOC_G + ALLOCA_G + LOAD_UNKNOWN_INLINE + STORE_UNKNOWN_INLINE + INLINE_ARRAY_NULL_GUARD)
    public NoValueImplementors2 test106(NoValueImplementors2[] array, NoValueImplementors2 v, MyObject5 o, int i) {
        array[0] = v;
        array[1] = array[0];
        array[2] = o;
        return array[i];
    }

    @DontCompile
    public void test106_verifier(boolean warmup) {
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

    @Test(valid = G1GCOn, failOn = STORE_UNKNOWN_INLINE + INLINE_ARRAY_NULL_GUARD, match = { COUNTEDLOOP, LOAD_UNKNOWN_INLINE }, matchCount = { 2, 2 } )
    @Test(valid = G1GCOff, failOn = STORE_UNKNOWN_INLINE + INLINE_ARRAY_NULL_GUARD, match = { COUNTEDLOOP, LOAD_UNKNOWN_INLINE }, matchCount = { 3, 2 } )
    @Warmup(0)
    public void test107(Object[] src1, Object[] src2) {
        for (int i = 0; i < src1.length; i++) {
            oFld1 = src1[i];
            oFld2 = src2[i];
        }
    }

    @DontCompile
    public void test107_verifier(boolean warmup) {
        MyValue2[] src1 = new MyValue2[100];
        Arrays.fill(src1, testValue2);
        Object[] src2 = new Object[100];
        Object obj = new Object();
        Arrays.fill(src2, obj);
        rerun_and_recompile_for("TestLWorld::test107", 10,
                                () -> { test107(src1, src2);
                                        Asserts.assertEquals(oFld1, testValue2);
                                        Asserts.assertEquals(oFld2, obj);
                                        test107(src2, src1);
                                        Asserts.assertEquals(oFld1, obj);
                                        Asserts.assertEquals(oFld2, testValue2);  });
    }

    @Test(valid = G1GCOn, failOn = LOAD_UNKNOWN_INLINE + INLINE_ARRAY_NULL_GUARD, match = { COUNTEDLOOP, STORE_UNKNOWN_INLINE }, matchCount = { 4, 9 } )
    @Test(valid = G1GCOff, failOn = LOAD_UNKNOWN_INLINE + INLINE_ARRAY_NULL_GUARD, match = { COUNTEDLOOP, STORE_UNKNOWN_INLINE }, matchCount = { 4, 12 } )
    @Warmup(0)
    public void test108(Object[] dst1, Object[] dst2, Object o1, Object o2) {
        for (int i = 0; i < dst1.length; i++) {
            dst1[i] = o1;
            dst2[i] = o2;
        }
    }

    @DontCompile
    public void test108_verifier(boolean warmup) {
        MyValue2[] dst1 = new MyValue2[100];
        Object[] dst2 = new Object[100];
        Object o1 = new Object();
        rerun_and_recompile_for("TestLWorld::test108", 10,
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

        static WrapperInterface wrap(long val) {
            return (val == 0L) ? ZERO : new LongWrapper(val);
        }
    }

    static primitive class LongWrapper implements WrapperInterface {
        final static LongWrapper ZERO = new LongWrapper(0);
        private long val;

        LongWrapper(long val) {
            this.val = val;
        }

        static LongWrapper wrap(long val) {
            return (val == 0L) ? ZERO : new LongWrapper(val);
        }

        public long value() {
            return val;
        }
    }

    static class InterfaceBox {
        WrapperInterface content;

        InterfaceBox(WrapperInterface content) {
            this.content = content;
        }

        static InterfaceBox box_sharp(long val) {
            return new InterfaceBox(LongWrapper.wrap(val));
        }

        static InterfaceBox box(long val) {
            return new InterfaceBox(WrapperInterface.wrap(val));
        }
    }

    static class ObjectBox {
        Object content;

        ObjectBox(Object content) {
            this.content = content;
        }

        static ObjectBox box_sharp(long val) {
            return new ObjectBox(LongWrapper.wrap(val));
        }

        static ObjectBox box(long val) {
            return new ObjectBox(WrapperInterface.wrap(val));
        }
    }

    static class RefBox {
        LongWrapper.ref content;

        RefBox(LongWrapper.ref content) {
            this.content = content;
        }

        static RefBox box_sharp(long val) {
            return new RefBox(LongWrapper.wrap(val));
        }

        static RefBox box(long val) {
            return new RefBox((LongWrapper.ref)WrapperInterface.wrap(val));
        }
    }

    static class InlineBox {
        LongWrapper content;

        InlineBox(long val) {
            this.content = LongWrapper.wrap(val);
        }

        static InlineBox box(long val) {
            return new InlineBox(val);
        }
    }

    static class GenericBox<T> {
        T content;

        static GenericBox<LongWrapper.ref> box_sharp(long val) {
            GenericBox<LongWrapper.ref> res = new GenericBox<>();
            res.content = LongWrapper.wrap(val);
            return res;
        }

        static GenericBox<WrapperInterface> box(long val) {
            GenericBox<WrapperInterface> res = new GenericBox<>();
            res.content = WrapperInterface.wrap(val);
            return res;
        }
    }

    long[] lArr = {0L, rL, 0L, rL, 0L, rL, 0L, rL, 0L, rL};

    // Test removal of allocations when inline type instance is wrapped into box object
    @Warmup(10000) // Make sure interface calls are inlined
    @Test(failOn = ALLOC_G + MEMBAR, match = { PREDICATE_TRAP }, matchCount = { 1 })
    public long test109() {
        long res = 0;
        for (int i = 0 ; i < lArr.length; i++) {
            res += InterfaceBox.box(lArr[i]).content.value();
        }
        return res;
    }

    @DontCompile
    public void test109_verifier(boolean warmup) {
        long res = test109();
        Asserts.assertEquals(res, 5*rL);
    }

    @Warmup(10000) // Make sure interface calls are inlined
    @Test(failOn = ALLOC_G + MEMBAR, match = { PREDICATE_TRAP }, matchCount = { 1 })
    public long test109_sharp() {
        long res = 0;
        for (int i = 0 ; i < lArr.length; i++) {
            res += InterfaceBox.box_sharp(lArr[i]).content.value();
        }
        return res;
    }

    @DontCompile
    public void test109_sharp_verifier(boolean warmup) {
        long res = test109_sharp();
        Asserts.assertEquals(res, 5*rL);
    }

    // Same as test109 but with ObjectBox
    @Test(failOn = ALLOC_G + MEMBAR, match = { PREDICATE_TRAP }, matchCount = { 1 })
    @Warmup(10000) // Make sure interface calls are inlined
    public long test110() {
        long res = 0;
        for (int i = 0 ; i < lArr.length; i++) {
            res += ((WrapperInterface)ObjectBox.box(lArr[i]).content).value();
        }
        return res;
    }

    @DontCompile
    public void test110_verifier(boolean warmup) {
        long res = test110();
        Asserts.assertEquals(res, 5*rL);
    }

    @Test(failOn = ALLOC_G + MEMBAR, match = { PREDICATE_TRAP }, matchCount = { 1 })
    @Warmup(10000) // Make sure interface calls are inlined
    public long test110_sharp() {
        long res = 0;
        for (int i = 0 ; i < lArr.length; i++) {
            res += ((WrapperInterface)ObjectBox.box_sharp(lArr[i]).content).value();
        }
        return res;
    }

    @DontCompile
    public void test110_sharp_verifier(boolean warmup) {
        long res = test110_sharp();
        Asserts.assertEquals(res, 5*rL);
    }

    // Same as test109 but with RefBox
    @Test(failOn = ALLOC_G + MEMBAR, match = { PREDICATE_TRAP }, matchCount = { 1 })
    public long test111() {
        long res = 0;
        for (int i = 0 ; i < lArr.length; i++) {
            res += RefBox.box(lArr[i]).content.value();
        }
        return res;
    }

    @DontCompile
    public void test111_verifier(boolean warmup) {
        long res = test111();
        Asserts.assertEquals(res, 5*rL);
    }

    @Test(failOn = ALLOC_G + MEMBAR, match = { PREDICATE_TRAP }, matchCount = { 1 })
    public long test111_sharp() {
        long res = 0;
        for (int i = 0 ; i < lArr.length; i++) {
            res += RefBox.box_sharp(lArr[i]).content.value();
        }
        return res;
    }

    @DontCompile
    public void test111_sharp_verifier(boolean warmup) {
        long res = test111_sharp();
        Asserts.assertEquals(res, 5*rL);
    }

    // Same as test109 but with InlineBox
    @Test(failOn = ALLOC_G + MEMBAR, match = { PREDICATE_TRAP }, matchCount = { 1 })
    public long test112() {
        long res = 0;
        for (int i = 0 ; i < lArr.length; i++) {
            res += InlineBox.box(lArr[i]).content.value();
        }
        return res;
    }

    @DontCompile
    public void test112_verifier(boolean warmup) {
        long res = test112();
        Asserts.assertEquals(res, 5*rL);
    }

    // Same as test109 but with GenericBox
    @Test(failOn = ALLOC_G + MEMBAR, match = { PREDICATE_TRAP }, matchCount = { 1 })
    @Warmup(10000) // Make sure interface calls are inlined
    public long test113() {
        long res = 0;
        for (int i = 0 ; i < lArr.length; i++) {
            res += GenericBox.box(lArr[i]).content.value();
        }
        return res;
    }

    @DontCompile
    public void test113_verifier(boolean warmup) {
        long res = test113();
        Asserts.assertEquals(res, 5*rL);
    }

    @Test(failOn = ALLOC_G + MEMBAR, match = { PREDICATE_TRAP }, matchCount = { 1 })
    @Warmup(10000) // Make sure interface calls are inlined
    public long test113_sharp() {
        long res = 0;
        for (int i = 0 ; i < lArr.length; i++) {
            res += GenericBox.box_sharp(lArr[i]).content.value();
        }
        return res;
    }

    @DontCompile
    public void test113_sharp_verifier(boolean warmup) {
        long res = test113_sharp();
        Asserts.assertEquals(res, 5*rL);
    }

    static interface WrapperInterface2 {
        public long value();

        static final InlineWrapper.ref ZERO = new InlineWrapper(0);

        public static WrapperInterface2 wrap(long val) {
            return (val == 0) ? ZERO.content : new LongWrapper2(val);
        }

        public static WrapperInterface2 wrap_default(long val) {
            return (val == 0) ? LongWrapper2.default : new LongWrapper2(val);
        }
    }

    static primitive class LongWrapper2 implements WrapperInterface2 {
        private long val;

        public LongWrapper2(long val) {
            this.val = val;
        }

        public long value() {
            return val;
        }
    }

    static primitive class InlineWrapper {
        WrapperInterface2 content;

        public InlineWrapper(long val) {
            content = new LongWrapper2(val);
        }
    }

    static class InterfaceBox2 {
        WrapperInterface2 content;

        public InterfaceBox2(long val, boolean def) {
            this.content = def ? WrapperInterface2.wrap_default(val) : WrapperInterface2.wrap(val);
        }

        static InterfaceBox2 box(long val) {
            return new InterfaceBox2(val, false);
        }

        static InterfaceBox2 box_default(long val) {
            return new InterfaceBox2(val, true);
        }
    }

    // Same as tests above but with ZERO hidden in field of another inline type
    @Test(failOn = ALLOC_G + MEMBAR, match = { PREDICATE_TRAP }, matchCount = { 1 })
    @Warmup(10000)
    public long test114() {
        long res = 0;
        for (int i = 0; i < lArr.length; i++) {
            res += InterfaceBox2.box(lArr[i]).content.value();
        }
        return res;
    }

    @DontCompile
    public void test114_verifier(boolean warmup) {
        long res = test114();
        Asserts.assertEquals(res, 5*rL);
    }

    // Same as test114 but with .default instead of ZERO field
    @Test(failOn = ALLOC_G + MEMBAR, match = { PREDICATE_TRAP }, matchCount = { 1 })
    @Warmup(10000)
    public long test115() {
        long res = 0;
        for (int i = 0; i < lArr.length; i++) {
            res += InterfaceBox2.box_default(lArr[i]).content.value();
        }
        return res;
    }

    @DontCompile
    public void test115_verifier(boolean warmup) {
        long res = test115();
        Asserts.assertEquals(res, 5*rL);
    }

    static MyValueEmpty     fEmpty1;
    static MyValueEmpty.ref fEmpty2 = MyValueEmpty.default;
           MyValueEmpty     fEmpty3;
           MyValueEmpty.ref fEmpty4 = MyValueEmpty.default;

    // Test fields loads/stores with empty inline types
    @Test(failOn = ALLOC + ALLOC_G + LOAD + STORE + TRAP)
    public void test116() {
        fEmpty1 = fEmpty4;
        fEmpty2 = fEmpty1;
        fEmpty3 = fEmpty2;
        fEmpty4 = fEmpty3;
    }

    @DontCompile
    public void test116_verifier(boolean warmup) {
        test116();
        Asserts.assertEquals(fEmpty1, fEmpty2);
        Asserts.assertEquals(fEmpty2, fEmpty3);
        Asserts.assertEquals(fEmpty3, fEmpty4);
    }

    // Test array loads/stores with empty inline types
    @Test(failOn = ALLOC + ALLOC_G)
    public MyValueEmpty test117(MyValueEmpty[] arr1, MyValueEmpty.ref[] arr2) {
        arr1[0] = arr2[0];
        arr2[0] = new MyValueEmpty();
        return arr1[0];
    }

    @DontCompile
    public void test117_verifier(boolean warmup) {
        MyValueEmpty[] arr1 = new MyValueEmpty[]{MyValueEmpty.default};
        MyValueEmpty res = test117(arr1, arr1);
        Asserts.assertEquals(res, MyValueEmpty.default);
        Asserts.assertEquals(arr1[0], MyValueEmpty.default);
    }

    // Test acmp with empty inline types
    @Test(failOn = ALLOC + ALLOC_G)
    public boolean test118(MyValueEmpty v1, MyValueEmpty.ref v2, Object o1) {
        return (v1 == v2) && (v2 == o1);
    }

    @DontCompile
    public void test118_verifier(boolean warmup) {
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
    public void test119(boolean deopt) {
        MyValueEmpty[]   array1 = new MyValueEmpty[]{MyValueEmpty.default};
        EmptyContainer[] array2 = new EmptyContainer[]{EmptyContainer.default};
        MixedContainer[] array3 = new MixedContainer[]{MixedContainer.default};
        if (deopt) {
            // uncommon trap
            WHITE_BOX.deoptimizeMethod(tests.get(getClass().getSimpleName() + "::test119"));
        }
        Asserts.assertEquals(array1[0], MyValueEmpty.default);
        Asserts.assertEquals(array2[0], EmptyContainer.default);
        Asserts.assertEquals(array3[0], MixedContainer.default);
    }

    @DontCompile
    public void test119_verifier(boolean warmup) {
        test119(!warmup);
    }

    // Test removal of empty inline type field stores
    @Test(failOn = ALLOC + ALLOC_G + LOAD + STORE + FIELD_ACCESS + NULL_CHECK_TRAP + TRAP)
    public void test120(MyValueEmpty empty) {
        fEmpty1 = empty;
        fEmpty3 = empty;
        // fEmpty2 and fEmpty4 could be null, store can't be removed
    }

    @DontCompile
    public void test120_verifier(boolean warmup) {
        test120(MyValueEmpty.default);
        Asserts.assertEquals(fEmpty1, MyValueEmpty.default);
        Asserts.assertEquals(fEmpty2, MyValueEmpty.default);
    }

    // Test removal of empty inline type field loads
    @Test(failOn = ALLOC + ALLOC_G + LOAD + STORE + FIELD_ACCESS + NULL_CHECK_TRAP + TRAP)
    public boolean test121() {
        return fEmpty1.equals(fEmpty3);
        // fEmpty2 and fEmpty4 could be null, load can't be removed
    }

    @DontCompile
    public void test121_verifier(boolean warmup) {
        boolean res = test121();
        Asserts.assertTrue(res);
    }

    // Verify that empty inline type field loads check for null holder
    @Test()
    public MyValueEmpty test122(TestLWorld t) {
        return t.fEmpty3;
    }

    @DontCompile
    public void test122_verifier(boolean warmup) {
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
    @Test()
    public void test123(TestLWorld t) {
        t.fEmpty3 = MyValueEmpty.default;
    }

    @DontCompile
    public void test123_verifier(boolean warmup) {
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
    @Test(failOn = SUBSTITUTABILITY_TEST)
    public boolean test124(Integer o1, Object o2) {
        return o1 == o2;
    }

    @DontCompile
    public void test124_verifier(boolean warmup) {
        test124(42, 42);
        test124(42, testValue1);
    }

    // acmp doesn't need substitutability test when one input null
    @Test(failOn = SUBSTITUTABILITY_TEST)
    public boolean test125(Object o1) {
        Object o2 = null;
        return o1 == o2;
    }

    @DontCompile
    public void test125_verifier(boolean warmup) {
        test125(testValue1);
        test125(null);
    }

    // Test inline type that can only be scalarized after loop opts
    @Test(failOn = ALLOC + LOAD + STORE)
    @Warmup(10000)
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

    @DontCompile
    public void test126_verifier(boolean warmup) {
        long res = test126(false);
        Asserts.assertEquals(res, 0L);
        if (!warmup) {
            res = test126(true);
            Asserts.assertEquals(res, testValue2.hash());
        }
    }

    // Same as test126 but with interface type
    @Test(failOn = ALLOC + LOAD + STORE)
    @Warmup(10000)
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

    @DontCompile
    public void test127_verifier(boolean warmup) {
        long res = test127(false);
        Asserts.assertEquals(res, 0L);
        if (!warmup) {
            res = test127(true);
            Asserts.assertEquals(res, testValue2.hash());
        }
    }

    // Test inline type that can only be scalarized after CCP
    @Test(failOn = ALLOC + LOAD + STORE)
    @Warmup(10000)
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

    @DontCompile
    public void test128_verifier(boolean warmup) {
        long res = test128(false);
        Asserts.assertEquals(res, 0L);
        if (!warmup) {
            res = test128(true);
            Asserts.assertEquals(res, testValue2.hash());
        }
    }

    // Same as test128 but with interface type
    @Test(failOn = ALLOC + LOAD + STORE)
    @Warmup(10000)
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

    @DontCompile
    public void test129_verifier(boolean warmup) {
        long res = test129(false);
        Asserts.assertEquals(res, 0L);
        if (!warmup) {
            res = test129(true);
            Asserts.assertEquals(res, testValue2.hash());
        }
    }

    // Lock on inline type (known after inlining)
    @ForceInline
    public Object test130_inlinee() {
        return MyValue1.createWithFieldsInline(rI, rL);
    }

    @Test()
    public void test130() {
        Object obj = test130_inlinee();
        synchronized (obj) {
            throw new RuntimeException("test130 failed: synchronization on inline type should not succeed");
        }
    }

    @DontCompile
    public void test130_verifier(boolean warmup) {
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

    @Test()
    public void test131() {
        Object obj = test131_inlinee();
        synchronized (obj) {
            throw new RuntimeException("test131 failed: synchronization on inline type should not succeed");
        }
    }

    @DontCompile
    public void test131_verifier(boolean warmup) {
        try {
            test131();
            throw new RuntimeException("test131 failed: no exception thrown");
        } catch (IllegalMonitorStateException ex) {
            // Expected
        }
    }

    // Test locking on object that is known to be an inline type only after CCP
    @Test()
    @Warmup(10000)
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

    @DontCompile
    public void test132_verifier(boolean warmup) {
        try {
            test132();
            throw new RuntimeException("test132 failed: no exception thrown");
        } catch (IllegalMonitorStateException ex) {
            // Expected
        }
    }

    // Test conditional locking on inline type and non-escaping object
    @Test()
    public void test133(boolean b) {
        Object obj = b ? Integer.valueOf(42) : MyValue2.createWithFieldsInline(rI, rD);
        synchronized (obj) {
            if (!b) {
                throw new RuntimeException("test133 failed: synchronization on inline type should not succeed");
            }
        }
    }

    @DontCompile
    public void test133_verifier(boolean warmup) {
        test133(true);
        try {
            test133(false);
            throw new RuntimeException("test133 failed: no exception thrown");
        } catch (IllegalMonitorStateException ex) {
            // Expected
        }
    }

    // Variant with non-scalarized inline type
    @Test()
    public static void test134(boolean b) {
        Object obj = null;
        if (b) {
            obj = MyValue2.createWithFieldsInline(rI, rD);
        }
        synchronized (obj) {

        }
    }

    @DontCompile
    public void test134_verifier(boolean warmup) {
        try {
            test134(true);
            throw new RuntimeException("test134 failed: no exception thrown");
        } catch (IllegalMonitorStateException ex) {
            // Expected
        }
    }

    // Test that acmp of the same inline object is removed
    @Test(failOn = ALLOC + LOAD + STORE + NULL_CHECK_TRAP + TRAP)
    public static boolean test135() {
        MyValue1 val = MyValue1.createWithFieldsInline(rI, rL);
        return val == val;
    }

    @DontCompile
    public void test135_verifier(boolean warmup) {
        Asserts.assertTrue(test135());
    }

    // Same as test135 but with .ref
    @Test(failOn = ALLOC + LOAD + STORE + NULL_CHECK_TRAP + TRAP)
    public static boolean test136(boolean b) {
        MyValue1.ref val = MyValue1.createWithFieldsInline(rI, rL);
        if (b) {
            val = null;
        }
        return val == val;
    }

    @DontCompile
    public void test136_verifier(boolean warmup) {
        Asserts.assertTrue(test136(false));
        Asserts.assertTrue(test136(true));
    }

    static final primitive class SimpleInlineType {
        final int x;
        public SimpleInlineType(int x) {
            this.x = x;
        }
    }

    // Test that acmp of different inline objects with same content is removed
    @Test(failOn = ALLOC + LOAD + STORE + NULL_CHECK_TRAP + TRAP)
    public static boolean test137(int i) {
        SimpleInlineType val1 = new SimpleInlineType(i);
        SimpleInlineType val2 = new SimpleInlineType(i);
        return val1 == val2;
    }

    @DontCompile
    public void test137_verifier(boolean warmup) {
        Asserts.assertTrue(test137(rI));
    }

    // Same as test137 but with .ref
    @Test(failOn = ALLOC + LOAD + STORE + NULL_CHECK_TRAP + TRAP)
    public static boolean test138(int i, boolean b) {
        SimpleInlineType.ref val1 = new SimpleInlineType(i);
        SimpleInlineType.ref val2 = new SimpleInlineType(i);
        if (b) {
            val1 = null;
            val2 = null;
        }
        return val1 == val2;
    }

    @DontCompile
    public void test138_verifier(boolean warmup) {
        Asserts.assertTrue(test138(rI, false));
        Asserts.assertTrue(test138(rI, true));
    }
}
