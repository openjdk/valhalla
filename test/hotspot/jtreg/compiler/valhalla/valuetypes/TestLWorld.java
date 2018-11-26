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
 * @summary Test value types in LWorld.
 * @modules java.base/jdk.experimental.bytecode
 *          java.base/jdk.experimental.value
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires os.simpleArch == "x64"
 * @compile -XDemitQtypes -XDenableValueTypes -XDallowWithFieldOperator -XDallowFlattenabilityModifiers TestLWorld.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -ea -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+EnableValhalla
 *                               compiler.valhalla.valuetypes.ValueTypeTest
 *                               compiler.valhalla.valuetypes.TestLWorld
 */
public class TestLWorld extends ValueTypeTest {
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
        TestLWorld test = new TestLWorld();
        test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class, MyValue3.class,
                 MyValue3Inline.class, Test51Value.class, Test77Value.class);
    }

    // Helper methods

    private static final MyValue1 testValue1 = MyValue1.createWithFieldsInline(rI, rL);
    private static final MyValue2 testValue2 = MyValue2.createWithFieldsInline(rI, true);

    protected long hash() {
        return testValue1.hash();
    }

    // Test passing a value type as an Object
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

    // Test storing/loading value types to/from Object and value type fields
    Object objectField1 = null;
    Object objectField2 = null;
    Object objectField3 = null;
    Object objectField4 = null;
    Object objectField5 = null;
    Object objectField6 = null;

    MyValue1.val valueField1 = testValue1;
    MyValue1.val valueField2 = testValue1;
    MyValue1.box valueField3 = testValue1;
    MyValue1.val valueField4;
    MyValue1.box valueField5;

    static MyValue1.box staticValueField1 = testValue1;
    static MyValue1.val staticValueField2 = testValue1;
    static MyValue1.val staticValueField3;
    static MyValue1.box staticValueField4;

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

    // Test merging value types and objects
    @Test()
    public Object test3(int state) {
        Object res = null;
        if (state == 0) {
            res = new Integer(rI);
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
            res = MyValue2.createWithFieldsInline(rI, true);
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

    // Test merging value types and objects in loops
    @Test()
    public Object test4(int iters) {
        Object res = new Integer(rI);
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

    // Test value types in object variables that are live at safepoint
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

    // Test comparing value types with objects
    @Test(failOn = ALLOC + LOAD + STORE + LOOP)
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

    // merge of value and non value
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

    // merge of values in a loop, stored in an object local
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

    // merge of values in an object local
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

    // Test storing/loading value types to/from interface and value type fields
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

    class MyObject implements MyInterface {
        public int x;

        public MyObject(int x) {
            this.x = x;
        }

        @ForceInline
        public long hash() {
            return x;
        }
    }

    // Test merging value types and interfaces
    @Test()
    public MyInterface test13(int state) {
        MyInterface res = null;
        if (state == 0) {
            res = new MyObject(rI);
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
        Asserts.assertEQ(((MyObject)result).x, rI);
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

    // Test merging value types and interfaces in loops
    @Test()
    public MyInterface test14(int iters) {
        MyInterface res = new MyObject(rI);
        for (int i = 0; i < iters; ++i) {
            if (res instanceof MyObject) {
                res = MyValue1.createWithFieldsInline(rI, rL);
            } else {
                res = MyValue1.createWithFieldsInline(((MyValue1)res).x + 1, rL);
            }
        }
        return res;
    }

    @DontCompile
    public void test14_verifier(boolean warmup) {
        MyObject result1 = (MyObject)test14(0);
        Asserts.assertEQ(result1.x, rI);
        int iters = (Math.abs(rI) % 10) + 1;
        MyValue1 result2 = (MyValue1)test14(iters);
        MyValue1 vt = MyValue1.createWithFieldsInline(rI + iters - 1, rL);
        Asserts.assertEQ(result2.hash(), vt.hash());
    }

    // Test value types in interface variables that are live at safepoint
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

    // Test comparing value types with interfaces
    @Test(failOn = ALLOC + LOAD + STORE + LOOP)
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

    // Test subtype check when casting to value type
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
        MyValue1 result = test17(vt, new Integer(rI));
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

    // Test load from (flattened) value type array disguised as object array
// TODO Re-enable if value type arrays become covariant with object arrays
/*
    @Test()
    public Object test21(Object[] oa, int index) {
        return oa[index];
    }

    @DontCompile
    public void test21_verifier(boolean warmup) {
        MyValue1 result = (MyValue1)test21(testValue1Array, Math.abs(rI) % 3);
        Asserts.assertEQ(result.hash(), hash());
    }

    // Test load from (flattened) value type array disguised as interface array
    @Test()
    public Object test22(MyInterface[] ia, int index) {
        return ia[index];
    }

    @DontCompile
    public void test22_verifier(boolean warmup) {
        MyValue1 result = (MyValue1)test22(testValue1Array, Math.abs(rI) % 3);
        Asserts.assertEQ(result.hash(), hash());
    }

    // Test value store to (flattened) value type array disguised as object array

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
*/

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

    // Test value store to (flattened) value type array disguised as interface array
// TODO Re-enable if value type arrays become covariant with object arrays
/*
    @ForceInline
    public void test26_inline(MyInterface[] ia, MyInterface i, int index) {
        ia[index] = i;
    }

    @Test()
    public void test26(MyInterface[] ia, MyValue1 vt, int index) {
      test26_inline(ia, vt, index);
    }

    @DontCompile
    public void test26_verifier(boolean warmup) {
        int index = Math.abs(rI) % 3;
        MyValue1 vt = MyValue1.createWithFieldsInline(rI + 1, rL + 1);
        test26(testValue1Array, vt, index);
        Asserts.assertEQ(testValue1Array[index].hash(), vt.hash());
        testValue1Array[index] = testValue1;
        try {
            test26(testValue2Array, vt, index);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
        Asserts.assertEQ(testValue2Array[index].hash(), testValue2.hash());
    }
*/

    @ForceInline
    public void test27_inline(MyInterface[] ia, MyInterface i, int index) {
        ia[index] = i;
    }

    @Test()
    public void test27(MyInterface[] ia, MyValue1 vt, int index) {
        test27_inline(ia, vt, index);
    }

    @DontCompile
    public void test27_verifier(boolean warmup) {
        int index = Math.abs(rI) % 3;
        try {
            test27(null, testValue1, index);
            throw new RuntimeException("No NPE thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    // Test object store to (flattened) value type array disguised as object array
// TODO Re-enable if value type arrays become covariant with object arrays
/*
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
*/

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

    // Test value store to (flattened) value type array disguised as interface array
// TODO Re-enable if value type arrays become covariant with object arrays
/*
    @ForceInline
    public void test31_inline(MyInterface[] ia, MyInterface i, int index) {
        ia[index] = i;
    }

    @Test()
    public void test31(MyInterface[] ia, MyInterface i, int index) {
        test31_inline(ia, i, index);
    }

    @DontCompile
    public void test31_verifier(boolean warmup) {
        int index = Math.abs(rI) % 3;
        MyValue1 vt1 = MyValue1.createWithFieldsInline(rI + 1, rL + 1);
        test31(testValue1Array, vt1, index);
        Asserts.assertEQ(testValue1Array[index].hash(), vt1.hash());
        try {
            test31(testValue1Array, testValue2, index);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
        Asserts.assertEQ(testValue1Array[index].hash(), vt1.hash());
        testValue1Array[index] = testValue1;
    }

    @ForceInline
    public void test32_inline(MyInterface[] ia, MyInterface i, int index) {
        ia[index] = i;
    }

    @Test()
    public void test32(MyInterface[] ia, MyInterface i, int index) {
        test32_inline(ia, i, index);
    }

    @DontCompile
    public void test32_verifier(boolean warmup) {
        int index = Math.abs(rI) % 3;
        try {
            test32(testValue2Array, testValue1, index);
            throw new RuntimeException("No ArrayStoreException thrown");
        } catch (ArrayStoreException e) {
            // Expected
        }
    }
*/

    // Test writing null to a (flattened) value type array disguised as object array
// TODO Re-enable if value type arrays become covariant with object arrays
/*
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

    // Test writing constant null to a (flattened) value type array disguised as object array

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

    // Test writing constant null to a (flattened) value type array

    private static final MethodHandle setArrayElementNull = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
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
*/

    // Test writing a value type to a null value type array
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
// TODO Re-enable if value type arrays become covariant with object arrays
/*
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

    // Test merging of value type arrays

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
*/

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
            result = new Integer(42);
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

    // Test instanceof with value types and arrays
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
        result = test40(new Long(42), index);
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
    private static final MyValue1.box test42VT1 = MyValue1.createWithFieldsInline(rI, rL);
    private static final MyValue1.box test42VT2 = MyValue1.createWithFieldsInline(rI + 1, rL + 1);

    @Test()
    public void test42() {
        MyValue1[] vals = new MyValue1[] {test42VT1, test42VT2};
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
        MyValue1[] vals = new MyValue1[] {test42VT1, test42VT2};

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
    private static final MethodHandle setArrayElementIncompatible = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
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
// TODO Re-enable if value type arrays become covariant with object arrays
/*
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
*/

    // instanceof tests with values
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
        boolean result = test49(new Integer(42));
        Asserts.assertFalse(result);
    }

    // Value type with some non-flattened fields
    value final class Test51Value {
        final Object objectField1 = null;
        final Object objectField2 = null;
        final Object objectField3 = null;
        final Object objectField4 = null;
        final Object objectField5 = null;
        final Object objectField6 = null;

        final MyValue1.val valueField1;
        final MyValue1.val valueField2;
        final MyValue1.box valueField3;
        final MyValue1.val valueField4;
        final MyValue1.box valueField5;

        private Test51Value() {
            valueField1 = testValue1;
            valueField2 = testValue1;
            valueField3 = testValue1;
            valueField4 = MyValue1.createDefaultDontInline();
            valueField5 = MyValue1.createDefaultDontInline();
        }

        public Test51Value init() {
            Test51Value vt = __WithField(this.valueField1, testValue1);
            vt = __WithField(vt.valueField2, testValue1);
            vt = __WithField(vt.valueField3, testValue1);
            return vt;
        }

        @ForceInline
        public long test(Test51Value holder, MyValue1 vt1, Object vt2) {
            holder = __WithField(holder.objectField1, vt1);
            holder = __WithField(holder.objectField2, (MyValue1)vt2);
            holder = __WithField(holder.objectField3, testValue1);
            holder = __WithField(holder.objectField4, MyValue1.createWithFieldsDontInline(rI, rL));
            holder = __WithField(holder.objectField5, holder.valueField1);
            holder = __WithField(holder.objectField6, holder.valueField3);
            holder = __WithField(holder.valueField1, (MyValue1)holder.objectField1);
            holder = __WithField(holder.valueField2, (MyValue1)vt2);
            holder = __WithField(holder.valueField3, (MyValue1)vt2);

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

    // Same as test2 but with field holder being a value type
    @Test()
    public long test51(Test51Value holder, MyValue1 vt1, Object vt2) {
        return holder.test(holder, vt1, vt2);
    }

    @DontCompile
    public void test51_verifier(boolean warmup) {
        MyValue1 vt = testValue1;
        MyValue1 def = MyValue1.createDefaultDontInline();
        Test51Value holder = Test51Value.default;
        Asserts.assertEQ(testValue1.hash(), vt.hash());
        holder = holder.init();
        Asserts.assertEQ(holder.valueField1.hash(), vt.hash());
        long result = test51(holder, vt, vt);
        Asserts.assertEQ(result, 9*vt.hash() + def.hashPrimitive());
    }

    // Access non-flattened, uninitialized value type field with value type holder
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

    // Merging value types of different types
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
        MyValue2 vt2 = MyValue2.createWithFieldsInline(rI, true);
        return b ? vt1 : vt2;
    }

    @DontCompile
    public void test55_verifier(boolean warmup) {
        MyValue1 result1 = (MyValue1)test55(true);
        Asserts.assertEQ(result1.hash(), hash());
        MyValue2 result2 = (MyValue2)test55(false);
        Asserts.assertEQ(result2.hash(), testValue2.hash());
    }

    // Test synchronization on value types
    @Test()
    public void test56(Object vt) {
        synchronized (vt) {
            throw new RuntimeException("test56 failed: synchronization on value type should not succeed");
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
            throw new RuntimeException("test57 failed: synchronization on value type should not succeed");
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
            throw new RuntimeException("test58 failed: synchronization on value type should not succeed");
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
                throw new RuntimeException("test59 failed: synchronization on value type should not succeed");
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
            throw new RuntimeException("test60 failed: synchronization on value type should not succeed");
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

    // type system test with interface and value type
    @ForceInline
    public MyInterface test64_helper(MyValue1 vt) {
        return vt;
    }

    @Test()
    public MyInterface test64(MyValue1 vt) {
        return test64_helper(vt);
    }

    @DontCompile
    public void test64_verifier(boolean warmup) {
        test64(testValue1);
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

// TODO Re-enable if value type arrays become covariant with object arrays
/*
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
*/

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

    // Test convertion between a value type and java.lang.Object without an allocation
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
    public MyInterface test70_sum(MyInterface a, MyInterface b) {
        int sum = ((MyValue1)a).x + ((MyValue1)b).x;
        return MyValue1.setX(((MyValue1)a), sum);
    }

    @Test(failOn = ALLOC + STORE)
    public int test70(MyValue1[] array) {
        MyValue1 result = MyValue1.createDefaultInline();
        for (int i = 0; i < array.length; ++i) {
            result = (MyValue1)test70_sum(result, array[i]);
        }
        return result.x;
    }

    @DontCompile
    public void test70_verifier(boolean warmup) {
        int result = test70(testValue1Array);
        Asserts.assertEQ(result, rI * testValue1Array.length);
    }

    // Test that allocated value type is not used in non-dominated path
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

    // Test calling a method on an uninitialized value type
    value final class Test72Value {
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
// TODO Re-enable if value type arrays become covariant with object arrays
/*
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
*/

    // Cast an Integer to a value type
    private static final MethodHandle castIntegerToValue = MethodHandleBuilder.loadCode(MethodHandles.lookup(),
        "castIntegerToValue",
        MethodType.methodType(void.class, TestLWorld.class, Integer.class),
        CODE -> {
            CODE.
            aload_1().
            checkcast(MyValue1.class).
            return_();
        });

    // Casting a null Integer to a value type should throw an exception
    @Test
    public void test76(Integer i) throws Throwable {
        castIntegerToValue.invoke(this, i);
    }

    @DontCompile
    public void test76_verifier(boolean warmup) throws Throwable {
        try {
            test76(null);
// TODO enable this once we've fixed the interpreter to throw a NPE
//            throw new RuntimeException("ClassCastException expected");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    // Test writing null to a flattenable/non-flattenable value type field in a value type
    value final class Test77Value {
        final MyValue1.box valueField1;
        final MyValue1.val valueField2;
        final MyValue1.box alwaysNull;

        private Test77Value() {
            valueField1 = testValue1;
            valueField2 = testValue1;
            alwaysNull  = testValue1;
        }

        @ForceInline
        public Test77Value test1() {
            return __WithField(this.valueField1, alwaysNull); // Should not throw NPE
        }

        @ForceInline
        public Test77Value test2() {
            return __WithField(this.valueField2, alwaysNull); // Should throw NPE
        }
    }

    @Test
    public Test77Value test77(Test77Value vt) {
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
    public void test77_verifier(boolean warmup) {
        test77(Test77Value.default);
    }
}
