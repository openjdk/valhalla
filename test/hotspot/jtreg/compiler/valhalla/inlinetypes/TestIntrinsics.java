/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.misc.Unsafe;
import jdk.test.lib.Asserts;
import compiler.lib.ir_framework.*;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;

import static compiler.valhalla.inlinetypes.InlineTypes.IRNode.*;
import static compiler.valhalla.inlinetypes.InlineTypes.rI;
import static compiler.valhalla.inlinetypes.InlineTypes.rL;

/*
 * @test
 * @key randomness
 * @summary Test intrinsic support for inline types
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @requires (os.simpleArch == "x64" | os.simpleArch == "aarch64")
 * @run driver/timeout=300 compiler.valhalla.inlinetypes.TestIntrinsics
 */

@ForceCompileClassInitializer
public class TestIntrinsics {

    public static void main(String[] args) {

        Scenario[] scenarios = InlineTypes.DEFAULT_SCENARIOS;
        for (Scenario scenario: scenarios) {
            scenario.addFlags("--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED");
        }
        scenarios[3].addFlags("-XX:-MonomorphicArrayCheck", "-XX:FlatArrayElementMaxSize=-1");
        scenarios[4].addFlags("-XX:-MonomorphicArrayCheck", "-XX:+UnlockExperimentalVMOptions", "-XX:PerMethodSpecTrapLimit=0", "-XX:PerMethodTrapLimit=0");

        InlineTypes.getFramework()
                   .addScenarios(scenarios)
                   .addHelperClasses(MyValue1.class,
                                     MyValue2.class,
                                     MyValue2Inline.class)
                   .start();
    }

    // Test correctness of the Class::isAssignableFrom intrinsic
    @Test
    public boolean test1(Class<?> supercls, Class<?> subcls) {
        return supercls.isAssignableFrom(subcls);
    }

    @Run(test = "test1")
    public void test1_verifier() {
        Asserts.assertTrue(test1(java.util.AbstractList.class, java.util.ArrayList.class), "test1_1 failed");
        Asserts.assertTrue(test1(MyValue1.class.asPrimaryType(), MyValue1.class.asPrimaryType()), "test1_2 failed");
        Asserts.assertTrue(test1(MyValue1.class.asValueType(), MyValue1.class.asValueType()), "test1_3 failed");
        Asserts.assertTrue(test1(MyValue1.class.asPrimaryType(), MyValue1.class.asValueType()), "test1_4 failed");
        Asserts.assertFalse(test1(MyValue1.class.asValueType(), MyValue1.class.asPrimaryType()), "test1_5 failed");
        Asserts.assertTrue(test1(Object.class, java.util.ArrayList.class), "test1_6 failed");
        Asserts.assertTrue(test1(Object.class, MyValue1.class.asPrimaryType()), "test1_7 failed");
        Asserts.assertTrue(test1(Object.class, MyValue1.class.asValueType()), "test1_8 failed");
        Asserts.assertTrue(!test1(MyValue1.class.asPrimaryType(), Object.class), "test1_9 failed");
        Asserts.assertTrue(!test1(MyValue1.class.asValueType(), Object.class), "test1_10 failed");
    }

    // Verify that Class::isAssignableFrom checks with statically known classes are folded
    @Test
    @IR(failOn = {LOADK})
    public boolean test2() {
        boolean check1 = java.util.AbstractList.class.isAssignableFrom(java.util.ArrayList.class);
        boolean check2 = MyValue1.class.asPrimaryType().isAssignableFrom(MyValue1.class.asPrimaryType());
        boolean check3 = MyValue1.class.asValueType().isAssignableFrom(MyValue1.class.asValueType());
        boolean check4 = MyValue1.class.asPrimaryType().isAssignableFrom(MyValue1.class.asValueType());
        boolean check5 = !MyValue1.class.asValueType().isAssignableFrom(MyValue1.class.asPrimaryType());
        boolean check6 = Object.class.isAssignableFrom(java.util.ArrayList.class);
        boolean check7 = Object.class.isAssignableFrom(MyValue1.class.asPrimaryType());
        boolean check8 = Object.class.isAssignableFrom(MyValue1.class.asValueType());
        boolean check9 = !MyValue1.class.asPrimaryType().isAssignableFrom(Object.class);
        boolean check10 = !MyValue1.class.asValueType().isAssignableFrom(Object.class);
        return check1 && check2 && check3 && check4 && check5 && check6 && check7 && check8 && check9 && check10;
    }

    @Run(test = "test2")
    public void test2_verifier() {
        Asserts.assertTrue(test2(), "test2 failed");
    }

    // Test correctness of the Class::getSuperclass intrinsic
    @Test
    public Class<?> test3(Class<?> cls) {
        return cls.getSuperclass();
    }

    @Run(test = "test3")
    public void test3_verifier() {
        Asserts.assertTrue(test3(Object.class) == null, "test3_1 failed");
        Asserts.assertTrue(test3(MyValue1.class.asPrimaryType()) == MyAbstract.class, "test3_2 failed");
        Asserts.assertTrue(test3(MyValue1.class.asValueType()) == MyAbstract.class, "test3_3 failed");
        Asserts.assertTrue(test3(Class.class) == Object.class, "test3_4 failed");
    }

    // Verify that Class::getSuperclass checks with statically known classes are folded
    @Test
    @IR(failOn = {LOADK})
    public boolean test4() {
        boolean check1 = Object.class.getSuperclass() == null;
        boolean check2 = MyValue1.class.asPrimaryType().getSuperclass() == MyAbstract.class;
        boolean check3 = MyValue1.class.asValueType().getSuperclass() == MyAbstract.class;
        boolean check4 = Class.class.getSuperclass() == Object.class;
        return check1 && check2 && check3 && check4;
    }

    @Run(test = "test4")
    public void test4_verifier() {
        Asserts.assertTrue(test4(), "test4 failed");
    }

    // Test toString() method
    @Test
    public String test5(MyValue1 v) {
        return v.toString();
    }

    @Run(test = "test5")
    public void test5_verifier() {
        MyValue1 v = MyValue1.createDefaultInline();
        test5(v);
    }

    // Test hashCode() method
    @Test
    public int test6(MyValue1 v) {
        return v.hashCode();
    }

    @Run(test = "test6")
    public void test6_verifier() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        int res = test6(v);
        Asserts.assertEQ(res, v.hashCode());
    }

    // Test default inline type array creation via reflection
    @Test
    public Object[] test7(Class<?> componentType, int len) {
        Object[] va = (Object[])Array.newInstance(componentType, len);
        return va;
    }

    @Run(test = "test7")
    public void test7_verifier() {
        int len = Math.abs(rI) % 42;
        long hash = MyValue1.createDefaultDontInline().hashPrimitive();
        Object[] va = test7(MyValue1.class.asValueType(), len);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(((MyValue1)va[i]).hashPrimitive(), hash);
        }
    }

    // Class.isInstance
    @Test
    public boolean test8(Class c, MyValue1 vt) {
        return c.isInstance(vt);
    }

    @Run(test = "test8")
    public void test8_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        boolean result = test8(MyValue1.class.asValueType(), vt);
        Asserts.assertTrue(result);
        result = test8(MyValue1.class.asPrimaryType(), vt);
        Asserts.assertTrue(result);
    }

    @Test
    public boolean test9(Class c, MyValue1 vt) {
        return c.isInstance(vt);
    }

    @Run(test = "test9")
    public void test9_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        boolean result = test9(MyValue2.class, vt);
        Asserts.assertFalse(result);
        result = test9(MyValue2.class.asPrimaryType(), vt);
        Asserts.assertFalse(result);
    }

    // Class.cast
    @Test
    public Object test10(Class c, MyValue1 vt) {
        return c.cast(vt);
    }

    @Run(test = "test10")
    public void test10_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        Object result = test10(MyValue1.class.asValueType(), vt);
        Asserts.assertEQ(((MyValue1)result).hash(), vt.hash());
    }

    @Test
    public Object test11(Class c, MyValue1 vt) {
        return c.cast(vt);
    }

    @Run(test = "test11")
    public void test11_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        try {
            test11(MyValue2.class, vt);
            throw new RuntimeException("should have thrown");
        } catch (ClassCastException cce) {
        }
    }

    @Test
    public Object test12(MyValue1 vt) {
        return MyValue1.class.asValueType().cast(vt);
    }

    @Run(test = "test12")
    public void test12_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        Object result = test12(vt);
        Asserts.assertEQ(((MyValue1)result).hash(), vt.hash());
    }

    @Test
    public Object test13(MyValue1 vt) {
        return MyValue2.class.cast(vt);
    }

    @Run(test = "test13")
    public void test13_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        try {
            test13(vt);
            throw new RuntimeException("should have thrown");
        } catch (ClassCastException cce) {
        }
    }

    // inline type array creation via reflection
    @Test
    public void test14(int len, long hash) {
        Object[] va = (Object[])Array.newInstance(MyValue1.class.asValueType(), len);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(((MyValue1)va[i]).hashPrimitive(), hash);
        }
    }

    @Run(test = "test14")
    public void test14_verifier() {
        int len = Math.abs(rI) % 42;
        long hash = MyValue1.createDefaultDontInline().hashPrimitive();
        test14(len, hash);
    }

    // Test hashCode() method
    @Test
    public int test15(Object v) {
        return v.hashCode();
    }

    @Run(test = "test15")
    public void test15_verifier() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        int res = test15(v);
        Asserts.assertEQ(res, v.hashCode());
    }

    @Test
    public int test16(Object v) {
        return System.identityHashCode(v);
    }

    @Run(test = "test16")
    public void test16_verifier() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        int res = test16(v);
        Asserts.assertEQ(res, System.identityHashCode((Object)v));
    }

    @Test
    public int test17(Object v) {
        return System.identityHashCode(v);
    }

    @Run(test = "test17")
    public void test17_verifier() {
        Integer v = Integer.valueOf(rI);
        int res = test17(v);
        Asserts.assertEQ(res, System.identityHashCode(v));
    }

    @Test
    public int test18(Object v) {
        return System.identityHashCode(v);
    }

    @Run(test = "test18")
    public void test18_verifier() {
        Object v = null;
        int res = test18(v);
        Asserts.assertEQ(res, System.identityHashCode(v));
    }

    // hashCode() and toString() with different inline types
    @Test
    public int test19(MyValue1 vt1, MyValue1 vt2, boolean b) {
        MyValue1 res = b ? vt1 : vt2;
        return res.hashCode();
    }

    @Run(test = "test19")
    public void test19_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        int res = test19(vt, vt, true);
        Asserts.assertEQ(res, vt.hashCode());
        res = test19(vt, vt, false);
        Asserts.assertEQ(res, vt.hashCode());
    }

    @Test
    public String test20(MyValue1 vt1, MyValue1 vt2, boolean b) {
        MyValue1 res = b ? vt1 : vt2;
        return res.toString();
    }

    @Run(test = "test20")
    public void test20_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        String res = test20(vt, vt, true);
        Asserts.assertEQ(res, vt.toString());
        res = test20(vt, vt, false);
        Asserts.assertEQ(res, vt.toString());
    }

    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long X_OFFSET;
    private static final long Y_OFFSET;
    private static final long V1_OFFSET;
    private static final boolean V1_FLATTENED;
    static {
        try {
            Field xField = MyValue1.class.asValueType().getDeclaredField("x");
            X_OFFSET = U.objectFieldOffset(xField);
            Field yField = MyValue1.class.asValueType().getDeclaredField("y");
            Y_OFFSET = U.objectFieldOffset(yField);
            Field v1Field = MyValue1.class.asValueType().getDeclaredField("v1");
            V1_OFFSET = U.objectFieldOffset(v1Field);
            V1_FLATTENED = U.isFlattened(v1Field);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static final String CALL_Unsafe = START + "CallStaticJava" + MID + "# Static  jdk.internal.misc.Unsafe::" + END;

    @Test
    @IR(failOn = {CALL_Unsafe})
    public int test21(MyValue1 v) {
       return U.getInt(v, X_OFFSET);
    }

    @Run(test = "test21")
    public void test21_verifier() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        int res = test21(v);
        Asserts.assertEQ(res, v.x);
    }

    MyValue1 test22_vt;
    @Test
    @IR(failOn = {CALL_Unsafe, ALLOC})
    public void test22(MyValue1 v) {
        v = U.makePrivateBuffer(v);
        U.putInt(v, X_OFFSET, rI);
        v = U.finishPrivateBuffer(v);
        test22_vt = v;
    }

    @Run(test = "test22")
    public void test22_verifier() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        test22(v.setX(v, 0));
        Asserts.assertEQ(test22_vt.hash(), v.hash());
    }

    @Test
    @IR(failOn = {CALL_Unsafe})
    public int test23(MyValue1 v, long offset) {
        return U.getInt(v, offset);
    }

    @Run(test = "test23")
    public void test23_verifier() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        int res = test23(v, X_OFFSET);
        Asserts.assertEQ(res, v.x);
    }

    MyValue1 test24_vt = MyValue1.createWithFieldsInline(rI, rL);

    @Test
    @IR(failOn = {CALL_Unsafe})
    public int test24(long offset) {
        return U.getInt(test24_vt, offset);
    }

    @Run(test = "test24")
    public void test24_verifier() {
        int res = test24(X_OFFSET);
        Asserts.assertEQ(res, test24_vt.x);
    }

    // Test copyOf intrinsic with allocated inline type in it's debug information
    final primitive class Test25Value {
        final int x;
        public Test25Value() {
            this.x = 42;
        }
    }

    final Test25Value[] test25Array = new Test25Value[10];

    @Test
    public Test25Value[] test25(Test25Value element) {
        Object[] newArray = Arrays.copyOf(test25Array, test25Array.length + 1);
        newArray[test25Array.length] = element;
        return (Test25Value[]) newArray;
    }

    @Run(test = "test25")
    public void test25_verifier() {
        Test25Value vt = new Test25Value();
        test25(vt);
    }

    @Test
    public Object test26() {
        Class<?>[] ca = new Class<?>[1];
        for (int i = 0; i < 1; ++i) {
          // Folds during loop opts
          ca[i] = MyValue1.class.asValueType();
        }
        return Array.newInstance(ca[0], 1);
    }

    @Run(test = "test26")
    public void test26_verifier() {
        Object[] res = (Object[])test26();
        Asserts.assertEQ(((MyValue1)res[0]).hashPrimitive(), MyValue1.createDefaultInline().hashPrimitive());
    }

    // Load non-flattenable inline type field with unsafe
    MyValue1.ref test27_vt;
    private static final long TEST27_OFFSET;
    static {
        try {
            Field field = TestIntrinsics.class.getDeclaredField("test27_vt");
            TEST27_OFFSET = U.objectFieldOffset(field);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @IR(failOn = {CALL_Unsafe})
    public MyValue1.ref test27() {
        return (MyValue1.ref)U.getReference(this, TEST27_OFFSET);
    }

    @Run(test = "test27")
    public void test27_verifier() {
        test27_vt = null;
        MyValue1.ref res = test27();
        Asserts.assertEQ(res, null);
        test27_vt = MyValue1.createWithFieldsInline(rI, rL);
        res = test27();
        Asserts.assertEQ(res.hash(), test24_vt.hash());
    }

    // Mismatched type
    @Test
    @IR(failOn = {CALL_Unsafe})
    public int test28(MyValue1 v) {
        return U.getByte(v, X_OFFSET);
    }

    @Run(test = "test28")
    public void test28_verifier() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        int res = test28(v);
        if (java.nio.ByteOrder.nativeOrder() == java.nio.ByteOrder.LITTLE_ENDIAN) {
            Asserts.assertEQ(res, (int)((byte)v.x));
        } else {
            Asserts.assertEQ(res, (int)((byte)Integer.reverseBytes(v.x)));
        }
    }

    // Wrong alignment
    @Test
    @IR(failOn = {CALL_Unsafe})
    public long test29(MyValue1 v) {
        // Read the field that's guaranteed to not be last in the
        // inline type so we don't read out of bounds.
        if (X_OFFSET < Y_OFFSET) {
            return U.getInt(v, X_OFFSET+1);
        }
        return U.getLong(v, Y_OFFSET+1);
    }

    @Run(test = "test29")
    public void test29_verifier() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        long res = test29(v);
        if (java.nio.ByteOrder.nativeOrder() == java.nio.ByteOrder.LITTLE_ENDIAN) {
            if (X_OFFSET < Y_OFFSET) {
                Asserts.assertEQ(((int)res) << 8, (v.x >> 8) << 8);
            } else {
                Asserts.assertEQ(res << 8, (v.y >> 8) << 8);
            }
        } else {
            if (X_OFFSET < Y_OFFSET) {
                Asserts.assertEQ(((int)res), v.x >>> 8);
            } else {
                Asserts.assertEQ(res, v.y >>> 8);
            }
        }
    }

    // getValue to retrieve flattened field from inline type
    @Test
    @IR(failOn = {CALL_Unsafe})
    public MyValue2 test30(MyValue1 v) {
        if (V1_FLATTENED) {
            return U.getValue(v, V1_OFFSET, MyValue2.class.asValueType());
        }
        return (MyValue2)U.getReference(v, V1_OFFSET);
    }

    @Run(test = "test30")
    public void test30_verifier(RunInfo info) {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        MyValue2 res = test30(v);
        Asserts.assertEQ(res.hash(), v.v1.hash());
    }

    MyValue1 test31_vt;
    private static final long TEST31_VT_OFFSET;
    private static final boolean TEST31_VT_FLATTENED;
    static {
        try {
            Field test31_vt_Field = TestIntrinsics.class.getDeclaredField("test31_vt");
            TEST31_VT_OFFSET = U.objectFieldOffset(test31_vt_Field);
            TEST31_VT_FLATTENED = U.isFlattened(test31_vt_Field);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // getValue to retrieve flattened field from object
    @Test
    @IR(failOn = {CALL_Unsafe})
    public MyValue1 test31() {
        if (TEST31_VT_FLATTENED) {
            return U.getValue(this, TEST31_VT_OFFSET, MyValue1.class.asValueType());
        }
        return (MyValue1)U.getReference(this, TEST31_VT_OFFSET);
    }

    @Run(test = "test31")
    public void test31_verifier() {
        test31_vt = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 res = test31();
        Asserts.assertEQ(res.hash(), test31_vt.hash());
    }

    // putValue to set flattened field in object
    @Test
    @IR(failOn = {CALL_Unsafe})
    public void test32(MyValue1 vt) {
        if (TEST31_VT_FLATTENED) {
            U.putValue(this, TEST31_VT_OFFSET, MyValue1.class.asValueType(), vt);
        } else {
            U.putReference(this, TEST31_VT_OFFSET, vt);
        }
    }

    @Run(test = "test32")
    public void test32_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        test31_vt = MyValue1.createDefaultInline();
        test32(vt);
        Asserts.assertEQ(vt.hash(), test31_vt.hash());
    }

    private static final int TEST33_BASE_OFFSET;
    private static final int TEST33_INDEX_SCALE;
    private static final boolean TEST33_FLATTENED_ARRAY;
    static {
        try {
            TEST33_BASE_OFFSET = U.arrayBaseOffset(MyValue1[].class);
            TEST33_INDEX_SCALE = U.arrayIndexScale(MyValue1[].class);
            TEST33_FLATTENED_ARRAY = U.isFlattenedArray(MyValue1[].class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    // getValue to retrieve flattened field from array
    @Test
    @IR(failOn = {CALL_Unsafe})
    public MyValue1 test33(MyValue1[] arr) {
        if (TEST33_FLATTENED_ARRAY) {
            return U.getValue(arr, TEST33_BASE_OFFSET + TEST33_INDEX_SCALE, MyValue1.class.asValueType());
        }
        return (MyValue1)U.getReference(arr, TEST33_BASE_OFFSET + TEST33_INDEX_SCALE);
    }

    @Run(test = "test33")
    public void test33_verifier() {
        MyValue1[] arr = new MyValue1[2];
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        arr[1] = vt;
        MyValue1 res = test33(arr);
        Asserts.assertEQ(res.hash(), vt.hash());
    }

    // putValue to set flattened field in array
    @Test
    @IR(failOn = {CALL_Unsafe})
    public void test34(MyValue1[] arr, MyValue1 vt) {
        if (TEST33_FLATTENED_ARRAY) {
            U.putValue(arr, TEST33_BASE_OFFSET + TEST33_INDEX_SCALE, MyValue1.class.asValueType(), vt);
        } else {
            U.putReference(arr, TEST33_BASE_OFFSET + TEST33_INDEX_SCALE, vt);
        }
    }

    @Run(test = "test34")
    public void test34_verifier() {
        MyValue1[] arr = new MyValue1[2];
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        test34(arr, vt);
        Asserts.assertEQ(arr[1].hash(), vt.hash());
    }

    // getValue to retrieve flattened field from object with unknown
    // container type
    @Test
    @IR(failOn = {CALL_Unsafe})
    public MyValue1 test35(Object o) {
        if (TEST31_VT_FLATTENED) {
            return U.getValue(o, TEST31_VT_OFFSET, MyValue1.class.asValueType());
        }
        return (MyValue1)U.getReference(o, TEST31_VT_OFFSET);
    }

    @Run(test = "test35")
    public void test35_verifier() {
        test31_vt = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 res = test35(this);
        Asserts.assertEQ(res.hash(), test31_vt.hash());
    }

    // getValue to retrieve flattened field from object at unknown
    // offset
    @Test
    @IR(failOn = {CALL_Unsafe})
    public MyValue1 test36(long offset) {
        if (TEST31_VT_FLATTENED) {
            return U.getValue(this, offset, MyValue1.class.asValueType());
        }
        return (MyValue1)U.getReference(this, offset);
    }

    @Run(test = "test36")
    public void test36_verifier() {
        test31_vt = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 res = test36(TEST31_VT_OFFSET);
        Asserts.assertEQ(res.hash(), test31_vt.hash());
    }

    // putValue to set flattened field in object with unknown
    // container
    @Test
    @IR(failOn = {CALL_Unsafe})
    public void test37(Object o, MyValue1 vt) {
        if (TEST31_VT_FLATTENED) {
            U.putValue(o, TEST31_VT_OFFSET, MyValue1.class.asValueType(), vt);
        } else {
            U.putReference(o, TEST31_VT_OFFSET, vt);
        }
    }

    @Run(test = "test37")
    public void test37_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        test31_vt = MyValue1.createDefaultInline();
        test37(this, vt);
        Asserts.assertEQ(vt.hash(), test31_vt.hash());
    }

    // putValue to set flattened field in object, non inline argument
    // to store
    @Test
    @IR(counts = {CALL_Unsafe, "= 1"})
    public void test38(Object o) {
        if (TEST31_VT_FLATTENED) {
            U.putValue(this, TEST31_VT_OFFSET, MyValue1.class.asValueType(), o);
        } else {
            U.putReference(this, TEST31_VT_OFFSET, o);
        }
    }

    @Run(test = "test38")
    public void test38_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        test31_vt = MyValue1.createDefaultInline();
        test38(vt);
        Asserts.assertEQ(vt.hash(), test31_vt.hash());
    }

    @Test
    @IR(failOn = {CALL_Unsafe})
    public MyValue1 test39(MyValue1 v) {
        v = U.makePrivateBuffer(v);
        U.putInt(v, X_OFFSET, rI);
        v = U.finishPrivateBuffer(v);
        return v;
    }

    @Run(test = "test39")
    public void test39_verifier() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 res = test39(v.setX(v, 0));
        Asserts.assertEQ(res.hash(), v.hash());
    }

    // Test default inline type array creation via reflection
    @Test
    public Object[] test40(Class<?> componentType, int len) {
        Object[] va = (Object[])Array.newInstance(componentType, len);
        return va;
    }

    @Run(test = "test40")
    public void test40_verifier() {
        int len = Math.abs(rI) % 42;
        Object[] va = test40(MyValue1.class.asPrimaryType(), len);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(va[i], null);
        }
    }

    // Class.isInstance
    @Test
    public boolean test41(Class c, MyValue1.ref vt) {
        return c.isInstance(vt);
    }

    @Run(test = "test41")
    public void test41_verifier() {
        MyValue1.ref vt = MyValue1.createWithFieldsInline(rI, rL);
        boolean result = test41(MyValue1.class.asPrimaryType(), vt);
        Asserts.assertTrue(result);
        result = test41(MyValue1.class.asValueType(), vt);
        Asserts.assertTrue(result);
    }

    @Test
    public boolean test42(Class c, MyValue1.ref vt) {
        return c.isInstance(vt);
    }

    @Run(test = "test42")
    public void test42_verifier() {
        MyValue1.ref vt = MyValue1.createWithFieldsInline(rI, rL);
        boolean result = test42(MyValue2.class.asPrimaryType(), vt);
        Asserts.assertFalse(result);
        result = test42(MyValue2.class.asValueType(), vt);
        Asserts.assertFalse(result);
    }

    // Class.cast
    @Test
    public Object test43(Class c, MyValue1.ref vt) {
        return c.cast(vt);
    }

    @Run(test = "test43")
    public void test43_verifier() {
        MyValue1.ref vt = MyValue1.createWithFieldsInline(rI, rL);
        Object result = test43(MyValue1.class.asPrimaryType(), vt);
        Asserts.assertEQ(((MyValue1)result).hash(), vt.hash());
        result = test43(MyValue1.class.asPrimaryType(), null);
        Asserts.assertEQ(result, null);
    }

    @Test
    public Object test44(Class c, MyValue1.ref vt) {
        return c.cast(vt);
    }

    @Run(test = "test44")
    public void test44_verifier() {
        MyValue1.ref vt = MyValue1.createWithFieldsInline(rI, rL);
        try {
            test44(MyValue2.class.asPrimaryType(), vt);
            throw new RuntimeException("should have thrown");
        } catch (ClassCastException cce) {
        }
    }

    @Test
    public Object test45(MyValue1.ref vt) {
        return MyValue1.class.asPrimaryType().cast(vt);
    }

    @Run(test = "test45")
    public void test45_verifier() {
        MyValue1.ref vt = MyValue1.createWithFieldsInline(rI, rL);
        Object result = test45(vt);
        Asserts.assertEQ(((MyValue1)result).hash(), vt.hash());
        result = test45(null);
        Asserts.assertEQ(result, null);
    }

    @Test
    public Object test46(MyValue1.ref vt) {
        return MyValue2.class.asPrimaryType().cast(vt);
    }

    @Run(test = "test46")
    public void test46_verifier() {
        MyValue1.ref vt = MyValue1.createWithFieldsInline(rI, rL);
        test46(null);
        try {
            test46(vt);
            throw new RuntimeException("should have thrown");
        } catch (ClassCastException cce) {
        }
    }

    @Test
    public Object test47(MyValue1.ref vt) {
        return MyValue1.class.asValueType().cast(vt);
    }

    @Run(test = "test47")
    public void test47_verifier() {
        MyValue1.ref vt = MyValue1.createWithFieldsInline(rI, rL);
        Object result = test47(vt);
        Asserts.assertEQ(((MyValue1)result).hash(), vt.hash());
        try {
            test47(null);
            throw new RuntimeException("should have thrown");
        } catch (NullPointerException npe) {
        }
    }

    @Test
    public Object test48(Class c, MyValue1.ref vt) {
        return c.cast(vt);
    }

    @Run(test = "test48")
    public void test48_verifier() {
        MyValue1.ref vt = MyValue1.createWithFieldsInline(rI, rL);
        Object result = test48(MyValue1.class.asValueType(), vt);
        Asserts.assertEQ(((MyValue1)result).hash(), vt.hash());
        try {
            test48(MyValue1.class.asValueType(), null);
            throw new RuntimeException("should have thrown");
        } catch (NullPointerException npe) {
        }
    }

    @Test
    public Object test49(MyValue1 vt) {
        return MyValue1.class.asPrimaryType().cast(vt);
    }

    @Run(test = "test49")
    public void test49_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        Object result = test49(vt);
        Asserts.assertEQ(((MyValue1)result).hash(), vt.hash());
    }

    @Test
    public Object test50(Class c, Object obj) {
        return c.cast(obj);
    }

    @Run(test = "test50")
    public void test50_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1[] va  = new MyValue1[42];
        MyValue1.ref[] vba = new MyValue1.ref[42];
        Object result = test50(MyValue1.class.asValueType(), vt);
        Asserts.assertEQ(((MyValue1)result).hash(), vt.hash());
        result = test50(MyValue1.class.asPrimaryType(), vt);
        Asserts.assertEQ(((MyValue1)result).hash(), vt.hash());
        result = test50(MyValue1[].class, va);
        Asserts.assertEQ(result, va);
        result = test50(MyValue1.ref[].class, vba);
        Asserts.assertEQ(result, vba);
        result = test50(MyValue1.ref[].class, va);
        Asserts.assertEQ(result, va);
        try {
            test50(MyValue1.class.asValueType(), null);
            throw new RuntimeException("should have thrown");
        } catch (NullPointerException npe) {
        }
        try {
            test50(MyValue1[].class, vba);
            throw new RuntimeException("should have thrown");
        } catch (ClassCastException cce) {
        }
    }

    // inline type array creation via reflection
    @Test
    public void test51(int len) {
        Object[] va = (Object[])Array.newInstance(MyValue1.class.asPrimaryType(), len);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(va[i], null);
        }
    }

    @Run(test = "test51")
    public void test51_verifier() {
        int len = Math.abs(rI) % 42;
        test51(len);
    }

    // multidimensional inline type array creation via reflection
    @Test
    public Object[][] test52(int len, int val) {
        MyValue1[][] va1 = (MyValue1[][])Array.newInstance(MyValue1[].class, len);
        MyValue1.ref[][] va2 = (MyValue1.ref[][])Array.newInstance(MyValue1.ref[].class, len);
        Object[][] result;
        if (val == 1) {
            va1[0] = new MyValue1[1];
            result = va1;
        } else {
            va2[0] = new MyValue1.ref[1];
            result = va2;
        }
        if (val == 1) {
            Asserts.assertEQ(va1[0][0].hash(), ((MyValue1)result[0][0]).hash());
        } else {
            Asserts.assertEQ(result[0][0], null);
            result[0][0] = null;
        }
        return result;
    }

    @Run(test = "test52")
    public void test52_verifier() {
        test52(1, 1);
        test52(1, 2);
    }

    @Test
    public Object[][] test53(Class<?> c1, Class<?> c2, int len, int val) {
        MyValue1[][] va1 = (MyValue1[][])Array.newInstance(MyValue1[].class, len);
        MyValue1.ref[][] va2 = (MyValue1.ref[][])Array.newInstance(MyValue1.ref[].class, len);
        Object[][] va3 = (Object[][])Array.newInstance(c1, len);
        Object[][] va4 = (Object[][])Array.newInstance(c2, len);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(va1[i], null);
            Asserts.assertEQ(va2[i], null);
            Asserts.assertEQ(va3[i], null);
            Asserts.assertEQ(va4[i], null);
            va1[i] = new MyValue1[1];
            va2[i] = new MyValue1.ref[1];
            va3[i] = new MyValue1[1];
            va4[i] = new MyValue1.ref[1];
            Asserts.assertEQ(va1[i][0].hash(), ((MyValue1)va3[i][0]).hash());
            Asserts.assertEQ(va2[i][0], null);
            Asserts.assertEQ(va4[i][0], null);
        }
        Object[][] result;
        if (val == 1) {
            result = va1;
        } else if (val == 2) {
            result = va2;
        } else if (val == 3) {
            result = va3;
        } else {
            result = va4;
        }
        if ((val == 1 || val == 3) && len > 0) {
            Asserts.assertEQ(va1[0][0].hash(), ((MyValue1)result[0][0]).hash());
        } else if (len > 0) {
            Asserts.assertEQ(result[0][0], null);
            result[0][0] = null;
        }
        return result;
    }

    @Run(test = "test53")
    public void test53_verifier() {
        int len = Math.abs(rI) % 42;
        test53(MyValue1[].class, MyValue1.ref[].class, len, 1);
        test53(MyValue1[].class, MyValue1.ref[].class, len, 2);
        test53(MyValue1[].class, MyValue1.ref[].class, len, 3);
        test53(MyValue1[].class, MyValue1.ref[].class, len, 4);
    }

    // Same as test39 but Unsafe.putInt to buffer is not intrinsified/compiled
    @DontCompile
    public void test54_callee(MyValue1.ref v) { // Use .ref here to make sure the argument is not scalarized (otherwise larval information is lost)
        U.putInt(v, X_OFFSET, rI);
    }

    @Test
    public MyValue1 test54(MyValue1 v) {
        v = U.makePrivateBuffer(v);
        test54_callee(v);
        v = U.finishPrivateBuffer(v);
        return v;
    }

    @Run(test = "test54")
    @Warmup(10000) // Fill up the TLAB to trigger slow path allocation
    public void test54_verifier() {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 res = test54(v.setX(v, 0));
        Asserts.assertEQ(res.hash(), v.hash());
    }

    static final MyValue1 test55_vt = MyValue1.createWithFieldsInline(rI, rL);

    // Same as test30 but with constant field holder
    @Test
    @IR(failOn = {CALL_Unsafe})
    public MyValue2 test55() {
        if (V1_FLATTENED) {
            return U.getValue(test55_vt, V1_OFFSET, MyValue2.class.asValueType());
        }
        return (MyValue2)U.getReference(test55_vt, V1_OFFSET);
    }

    @Run(test = "test55")
    public void test55_verifier() {
        MyValue2 res = test55();
        Asserts.assertEQ(res.hash(), test55_vt.v1.hash());
    }

    // Test OptimizePtrCompare part of Escape Analysis
    @Test
    public void test56(int idx) {
        Object[] va = (Object[])Array.newInstance(MyValue1.class.asValueType(), 1);
        if (va[idx] == null) {
            throw new RuntimeException("Unexpected null");
        }
    }

    @Run(test = "test56")
    public void test56_verifier() {
        test56(0);
    }

    // Same as test56 but with load from known array index
    @Test
    public void test57() {
        Object[] va = (Object[])Array.newInstance(MyValue1.class.asValueType(), 1);
        if (va[0] == null) {
            throw new RuntimeException("Unexpected null");
        }
    }

    @Run(test = "test57")
    public void test57_verifier() {
        test57();
    }

    // Test unsafe allocation
    @Test
    public boolean test58(Class<?> c1, Class<?> c2) throws Exception {
        Object obj1 = U.allocateInstance(c1);
        Object obj2 = U.allocateInstance(c2);
        return obj1 == obj2;
    }

    @Run(test = "test58")
    public void test58_verifier() throws Exception {
        boolean res = test58(MyValue1.class.asValueType(), MyValue1.class.asValueType());
        Asserts.assertTrue(res);
        res = test58(Object.class, MyValue1.class.asValueType());
        Asserts.assertFalse(res);
        res = test58(MyValue1.class.asValueType(), Object.class);
        Asserts.assertFalse(res);
    }

    // Test synchronization on unsafe inline type allocation
    @Test
    public void test59(Class<?> c) throws Exception {
        Object obj = U.allocateInstance(c);
        synchronized (obj) {

        }
    }

    @Run(test = "test59")
    public void test59_verifier() throws Exception {
        test59(Integer.class);
        try {
            test59(MyValue1.class.asValueType());
            throw new RuntimeException("test59 failed: synchronization on inline type should not succeed");
        } catch (IllegalMonitorStateException e) {

        }
    }

    // Test mark word load optimization on unsafe inline type allocation
    @Test
    public boolean test60(Class<?> c1, Class<?> c2, boolean b1, boolean b2) throws Exception {
        Object obj1 = b1 ? new Object() : U.allocateInstance(c1);
        Object obj2 = b2 ? new Object() : U.allocateInstance(c2);
        return obj1 == obj2;
    }

    @Run(test = "test60")
    public void test60_verifier() throws Exception {
        Asserts.assertTrue(test60(MyValue1.class.asValueType(), MyValue1.class.asValueType(), false, false));
        Asserts.assertFalse(test60(MyValue1.class.asValueType(), MyValue2.class, false, false));
        Asserts.assertFalse(test60(MyValue1.class.asValueType(), MyValue1.class.asValueType(), false, true));
        Asserts.assertFalse(test60(MyValue1.class.asValueType(), MyValue1.class.asValueType(), true, false));
        Asserts.assertFalse(test60(MyValue1.class.asValueType(), MyValue1.class.asValueType(), true, true));
    }

    // Test asPrimaryType intrinsic with non-value mirror
    @Test
    public Class<?> test61(Class<?> c) {
        if (c.asPrimaryType() != Integer.class) {
            throw new RuntimeException("Unexpected class");
        }
        return Integer.class.asPrimaryType();
    }

    @Run(test = "test61")
    public void test61_verifier() {
        Class<?> result = test61(Integer.class);
        Asserts.assertEQ(result, Integer.class);
    }

    // Test asValueType intrinsic with non-value mirror
    @Test
    public Class<?> test62(Class<?> c) {
        try {
            c.asValueType();
            throw new RuntimeException("No exception thrown");
        } catch (UnsupportedOperationException ex) {
            // Expected
        }
        return Integer.class.asValueType();
    }

    @Run(test = "test62")
    public void test62_verifier() {
        try {
            test62(Integer.class);
            throw new RuntimeException("No exception thrown");
        } catch (UnsupportedOperationException ex) {
            // Expected
        }
    }

    // compareAndSet to flattened field in object
    @Test
    public boolean test63(MyValue1 oldVal, MyValue1 newVal) {
        if (TEST31_VT_FLATTENED) {
            return U.compareAndSetValue(this, TEST31_VT_OFFSET, MyValue1.class.asValueType(), oldVal, newVal);
        } else {
            return U.compareAndSetReference(this, TEST31_VT_OFFSET, oldVal, newVal);
        }
    }

    @Run(test = "test63")
    public void test63_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        test31_vt = MyValue1.default;

        boolean res = test63(test31_vt, vt);
        // Checks are disabled for non-flattened field because reference comparison
        // fails if C2 scalarizes and re-allocates the inline type arguments.
        if (TEST31_VT_FLATTENED) {
            Asserts.assertTrue(res);
            Asserts.assertEQ(test31_vt, vt);
        }

        res = test63(MyValue1.default, MyValue1.default);
        if (TEST31_VT_FLATTENED) {
            Asserts.assertFalse(res);
            Asserts.assertEQ(test31_vt, vt);
        }
    }

    // compareAndSet to flattened field in array
    @Test
    public boolean test64(MyValue1[] arr, MyValue1 oldVal, Object newVal) {
        if (TEST33_FLATTENED_ARRAY) {
            return U.compareAndSetValue(arr, TEST33_BASE_OFFSET + TEST33_INDEX_SCALE, MyValue1.class.asValueType(), oldVal, newVal);
        } else {
            return U.compareAndSetReference(arr, TEST33_BASE_OFFSET + TEST33_INDEX_SCALE, oldVal, newVal);
        }
    }

    @Run(test = "test64")
    public void test64_verifier() {
        MyValue1[] arr = new MyValue1[2];
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);

        boolean res = test64(arr, arr[1], vt);
        // Checks are disabled for non-flattened array because reference comparison
        // fails if C2 scalarizes and re-allocates the inline type arguments.
        if (TEST33_FLATTENED_ARRAY) {
            Asserts.assertTrue(res);
            Asserts.assertEQ(arr[1], vt);
        }

        res = test64(arr, MyValue1.default, MyValue1.default);
        if (TEST33_FLATTENED_ARRAY) {
            Asserts.assertFalse(res);
            Asserts.assertEQ(arr[1], vt);
        }
    }

    // compareAndSet to flattened field in object with unknown container
    @Test
    public boolean test65(Object o, Object oldVal, MyValue1 newVal) {
        if (TEST31_VT_FLATTENED) {
            return U.compareAndSetValue(o, TEST31_VT_OFFSET, MyValue1.class.asValueType(), oldVal, newVal);
        } else {
            return U.compareAndSetReference(o, TEST31_VT_OFFSET, oldVal, newVal);
        }
    }

    @Run(test = "test65")
    public void test65_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        test31_vt = MyValue1.default;

        boolean res = test65(this, test31_vt, vt);
        Asserts.assertTrue(res);
        Asserts.assertEQ(test31_vt, vt);

        res = test65(this, MyValue1.default, MyValue1.default);
        Asserts.assertFalse(res);
        Asserts.assertEQ(test31_vt, vt);
    }

    // compareAndSet to flattened field in object, non-inline arguments to compare and set
    @Test
    public boolean test66(Object oldVal, Object newVal) {
        if (TEST31_VT_FLATTENED) {
            return U.compareAndSetValue(this, TEST31_VT_OFFSET, MyValue1.class.asValueType(), oldVal, newVal);
        } else {
            return U.compareAndSetReference(this, TEST31_VT_OFFSET, oldVal, newVal);
        }
    }

    @Run(test = "test66")
    public void test66_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        test31_vt = MyValue1.default;

        boolean res = test66(test31_vt, vt);
        Asserts.assertTrue(res);
        Asserts.assertEQ(test31_vt, vt);

        res = test66(MyValue1.default, MyValue1.default);
        Asserts.assertFalse(res);
        Asserts.assertEQ(test31_vt, vt);
    }

    // compareAndExchange to flattened field in object
    @Test
    public Object test67(MyValue1 oldVal, MyValue1 newVal) {
        if (TEST31_VT_FLATTENED) {
            return U.compareAndExchangeValue(this, TEST31_VT_OFFSET, MyValue1.class.asValueType(), oldVal, newVal);
        } else {
            return U.compareAndExchangeReference(this, TEST31_VT_OFFSET, oldVal, newVal);
        }
    }

    @Run(test = "test67")
    public void test67_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 oldVal = MyValue1.default;
        test31_vt = oldVal;

        Object res = test67(test31_vt, vt);
        // Checks are disabled for non-flattened field because reference comparison
        // fails if C2 scalarizes and re-allocates the inline type arguments.
        if (TEST31_VT_FLATTENED) {
            Asserts.assertEQ(res, oldVal);
            Asserts.assertEQ(test31_vt, vt);
        }

        res = test67(MyValue1.default, MyValue1.default);
        if (TEST31_VT_FLATTENED) {
            Asserts.assertEQ(res, vt);
            Asserts.assertEQ(test31_vt, vt);
        }
    }

    // compareAndExchange to flattened field in array
    @Test
    public Object test68(MyValue1[] arr, MyValue1 oldVal, Object newVal) {
        if (TEST33_FLATTENED_ARRAY) {
            return U.compareAndExchangeValue(arr, TEST33_BASE_OFFSET + TEST33_INDEX_SCALE, MyValue1.class.asValueType(), oldVal, newVal);
        } else {
            return U.compareAndExchangeReference(arr, TEST33_BASE_OFFSET + TEST33_INDEX_SCALE, oldVal, newVal);
        }
    }

    @Run(test = "test68")
    public void test68_verifier() {
        MyValue1[] arr = new MyValue1[2];
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);

        Object res = test68(arr, arr[1], vt);
        // Checks are disabled for non-flattened array because reference comparison
        // fails if C2 scalarizes and re-allocates the inline type arguments.
        if (TEST33_FLATTENED_ARRAY) {
            Asserts.assertEQ(res, MyValue1.default);
            Asserts.assertEQ(arr[1], vt);
        }

        res = test68(arr, MyValue1.default, MyValue1.default);
        if (TEST33_FLATTENED_ARRAY) {
            Asserts.assertEQ(res, vt);
            Asserts.assertEQ(arr[1], vt);
        }
    }

    // compareAndExchange to flattened field in object with unknown container
    @Test
    public Object test69(Object o, Object oldVal, MyValue1 newVal) {
        if (TEST31_VT_FLATTENED) {
            return U.compareAndExchangeValue(o, TEST31_VT_OFFSET, MyValue1.class.asValueType(), oldVal, newVal);
        } else {
            return U.compareAndExchangeReference(o, TEST31_VT_OFFSET, oldVal, newVal);
        }
    }

    @Run(test = "test69")
    public void test69_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 oldVal = MyValue1.default;
        test31_vt = oldVal;

        Object res = test69(this, test31_vt, vt);
        Asserts.assertEQ(res, oldVal);
        Asserts.assertEQ(test31_vt, vt);

        res = test69(this, MyValue1.default, MyValue1.default);
        Asserts.assertEQ(res, vt);
        Asserts.assertEQ(test31_vt, vt);
    }

    // compareAndExchange to flattened field in object, non-inline arguments to compare and set
    @Test
    public Object test70(Object oldVal, Object newVal) {
        if (TEST31_VT_FLATTENED) {
            return U.compareAndExchangeValue(this, TEST31_VT_OFFSET, MyValue1.class.asValueType(), oldVal, newVal);
        } else {
            return U.compareAndExchangeReference(this, TEST31_VT_OFFSET, oldVal, newVal);
        }
    }

    @Run(test = "test70")
    public void test70_verifier() {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 oldVal = MyValue1.default;
        test31_vt = oldVal;

        Object res = test70(test31_vt, vt);
        Asserts.assertEQ(res, oldVal);
        Asserts.assertEQ(test31_vt, vt);

        res = test70(MyValue1.default, MyValue1.default);
        Asserts.assertEQ(res, vt);
        Asserts.assertEQ(test31_vt, vt);
    }

    // getValue to retrieve flattened field from (nullable) inline type
    @Test
    @IR(failOn = {CALL_Unsafe})
    public MyValue2 test71(boolean b, MyValue1.val v1, MyValue1.ref v2) {
        if (b) {
            if (V1_FLATTENED) {
                return U.getValue(v1, V1_OFFSET, MyValue2.class.asValueType());
            }
            return (MyValue2)U.getReference(v1, V1_OFFSET);
        } else {
            if (V1_FLATTENED) {
                return U.getValue(v2, V1_OFFSET, MyValue2.class.asValueType());
            }
            return (MyValue2)U.getReference(v2, V1_OFFSET);
        }
    }

    @Run(test = "test71")
    public void test71_verifier(RunInfo info) {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        Asserts.assertEQ(test71(true, v, v), v.v1);
        Asserts.assertEQ(test71(false, v, v), v.v1);
    }

    // Same as test71 but with non-constant offset
    @Test
    @IR(failOn = {CALL_Unsafe})
    public MyValue2 test72(boolean b, MyValue1.val v1, MyValue1.ref v2, long offset) {
        if (b) {
            if (V1_FLATTENED) {
                return U.getValue(v1, offset, MyValue2.class.asValueType());
            }
            return (MyValue2)U.getReference(v1, V1_OFFSET);
        } else {
            if (V1_FLATTENED) {
                return U.getValue(v2, offset, MyValue2.class.asValueType());
            }
            return (MyValue2)U.getReference(v2, V1_OFFSET);
        }
    }

    @Run(test = "test72")
    public void test72_verifier(RunInfo info) {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        Asserts.assertEQ(test72(true, v, v, V1_OFFSET), v.v1);
        Asserts.assertEQ(test72(false, v, v, V1_OFFSET), v.v1);
    }

    static final MyValue1.val test73_value1 = MyValue1.createWithFieldsInline(rI, rL);
    static final MyValue1.ref test73_value2 = MyValue1.createWithFieldsInline(rI+1, rL+1);

    // Same as test72 but with constant base
    @Test
    @IR(failOn = {CALL_Unsafe})
    public MyValue2 test73(boolean b, long offset) {
        if (b) {
            if (V1_FLATTENED) {
                return U.getValue(test73_value1, offset, MyValue2.class.asValueType());
            }
            return (MyValue2)U.getReference(test73_value1, V1_OFFSET);
        } else {
            if (V1_FLATTENED) {
                return U.getValue(test73_value2, offset, MyValue2.class.asValueType());
            }
            return (MyValue2)U.getReference(test73_value2, V1_OFFSET);
        }
    }

    @Run(test = "test73")
    public void test73_verifier(RunInfo info) {
        Asserts.assertEQ(test73(true, V1_OFFSET), test73_value1.v1);
        Asserts.assertEQ(test73(false, V1_OFFSET), test73_value2.v1);
    }
}
