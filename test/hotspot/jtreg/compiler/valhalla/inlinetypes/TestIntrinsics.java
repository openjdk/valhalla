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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import jdk.test.lib.Asserts;
import jdk.internal.misc.Unsafe;

/*
 * @test
 * @key randomness
 * @summary Test intrinsic support for inline types
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @modules java.base/jdk.internal.misc
 * @requires (os.simpleArch == "x64" | os.simpleArch == "aarch64")
 * @compile TestIntrinsics.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=300 -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                               compiler.valhalla.inlinetypes.InlineTypeTest
 *                               compiler.valhalla.inlinetypes.TestIntrinsics
 */
public class TestIntrinsics extends InlineTypeTest {
    // Extra VM parameters for some test scenarios. See InlineTypeTest.getVMParameters()
    @Override
    public String[] getExtraVMParameters(int scenario) {
        switch (scenario) {
        case 3: return new String[] {"-XX:-MonomorphicArrayCheck", "-XX:FlatArrayElementMaxSize=-1"};
        case 4: return new String[] {"-XX:-MonomorphicArrayCheck"};
        }
        return null;
    }

    public static void main(String[] args) throws Throwable {
        TestIntrinsics test = new TestIntrinsics();
        test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class);
    }

    // Test correctness of the Class::isAssignableFrom intrinsic
    @Test()
    public boolean test1(Class<?> supercls, Class<?> subcls) {
        return supercls.isAssignableFrom(subcls);
    }

    @DontCompile
    public void test1_verifier(boolean warmup) {
        Asserts.assertTrue(test1(java.util.AbstractList.class, java.util.ArrayList.class), "test1_1 failed");
        Asserts.assertTrue(test1(MyValue1.ref.class, MyValue1.ref.class), "test1_2 failed");
        Asserts.assertTrue(test1(MyValue1.class, MyValue1.class), "test1_3 failed");
        Asserts.assertTrue(test1(MyValue1.ref.class, MyValue1.class), "test1_4 failed");
        Asserts.assertFalse(test1(MyValue1.class, MyValue1.ref.class), "test1_5 failed");
        Asserts.assertTrue(test1(Object.class, java.util.ArrayList.class), "test1_6 failed");
        Asserts.assertTrue(test1(Object.class, MyValue1.ref.class), "test1_7 failed");
        Asserts.assertTrue(test1(Object.class, MyValue1.class), "test1_8 failed");
        Asserts.assertTrue(!test1(MyValue1.ref.class, Object.class), "test1_9 failed");
        Asserts.assertTrue(!test1(MyValue1.class, Object.class), "test1_10 failed");
    }

    // Verify that Class::isAssignableFrom checks with statically known classes are folded
    @Test(failOn = LOADK)
    public boolean test2() {
        boolean check1 = java.util.AbstractList.class.isAssignableFrom(java.util.ArrayList.class);
        boolean check2 = MyValue1.ref.class.isAssignableFrom(MyValue1.ref.class);
        boolean check3 = MyValue1.class.isAssignableFrom(MyValue1.class);
        boolean check4 = MyValue1.ref.class.isAssignableFrom(MyValue1.class);
        boolean check5 = !MyValue1.class.isAssignableFrom(MyValue1.ref.class);
        boolean check6 = Object.class.isAssignableFrom(java.util.ArrayList.class);
        boolean check7 = Object.class.isAssignableFrom(MyValue1.ref.class);
        boolean check8 = Object.class.isAssignableFrom(MyValue1.class);
        boolean check9 = !MyValue1.ref.class.isAssignableFrom(Object.class);
        boolean check10 = !MyValue1.class.isAssignableFrom(Object.class);
        return check1 && check2 && check3 && check4 && check5 && check6 && check7 && check8 && check9 && check10;
    }

    @DontCompile
    public void test2_verifier(boolean warmup) {
        Asserts.assertTrue(test2(), "test2 failed");
    }

    // Test correctness of the Class::getSuperclass intrinsic
    @Test()
    public Class<?> test3(Class<?> cls) {
        return cls.getSuperclass();
    }

    @DontCompile
    public void test3_verifier(boolean warmup) {
        Asserts.assertTrue(test3(Object.class) == null, "test3_1 failed");
        Asserts.assertTrue(test3(MyValue1.ref.class) == MyAbstract.class, "test3_2 failed");
        Asserts.assertTrue(test3(MyValue1.val.class) == MyValue1.ref.class, "test3_3 failed");
        Asserts.assertTrue(test3(Class.class) == Object.class, "test3_4 failed");
    }

    // Verify that Class::getSuperclass checks with statically known classes are folded
    @Test(failOn = LOADK)
    public boolean test4() {
        boolean check1 = Object.class.getSuperclass() == null;
        // TODO 8244562: Remove cast as workaround once javac is fixed
        boolean check2 = (Class<?>)MyValue1.ref.class.getSuperclass() == MyAbstract.class;
        // TODO 8244562: Remove cast as workaround once javac is fixed
        boolean check3 = (Class<?>)MyValue1.val.class.getSuperclass() == MyValue1.ref.class;
        boolean check4 = Class.class.getSuperclass() == Object.class;
        return check1 && check2 && check3 && check4;
    }

    @DontCompile
    public void test4_verifier(boolean warmup) {
        Asserts.assertTrue(test4(), "test4 failed");
    }

    // Test toString() method
    @Test()
    public String test5(MyValue1 v) {
        return v.toString();
    }

    @DontCompile
    public void test5_verifier(boolean warmup) {
        MyValue1 v = MyValue1.createDefaultInline();
        test5(v);
    }

    // Test hashCode() method
    @Test()
    public int test6(MyValue1 v) {
        return v.hashCode();
    }

    @DontCompile
    public void test6_verifier(boolean warmup) {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        int res = test6(v);
        Asserts.assertEQ(res, v.hashCode());
    }

    // Test default inline type array creation via reflection
    @Test()
    public Object[] test7(Class<?> componentType, int len) {
        Object[] va = (Object[])Array.newInstance(componentType, len);
        return va;
    }

    @DontCompile
    public void test7_verifier(boolean warmup) {
        int len = Math.abs(rI) % 42;
        long hash = MyValue1.createDefaultDontInline().hashPrimitive();
        Object[] va = test7(MyValue1.class, len);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(((MyValue1)va[i]).hashPrimitive(), hash);
        }
    }

    // Class.isInstance
    @Test()
    public boolean test8(Class c, MyValue1 vt) {
        return c.isInstance(vt);
    }

    @DontCompile
    public void test8_verifier(boolean warmup) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        boolean result = test8(MyValue1.class, vt);
        Asserts.assertTrue(result);
        result = test8(MyValue1.ref.class, vt);
        Asserts.assertTrue(result);
    }

    @Test()
    public boolean test9(Class c, MyValue1 vt) {
        return c.isInstance(vt);
    }

    @DontCompile
    public void test9_verifier(boolean warmup) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        boolean result = test9(MyValue2.class, vt);
        Asserts.assertFalse(result);
        result = test9(MyValue2.ref.class, vt);
        Asserts.assertFalse(result);
    }

    // Class.cast
    @Test()
    public Object test10(Class c, MyValue1 vt) {
        return c.cast(vt);
    }

    @DontCompile
    public void test10_verifier(boolean warmup) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        Object result = test10(MyValue1.class, vt);
        Asserts.assertEQ(((MyValue1)result).hash(), vt.hash());
    }

    @Test()
    public Object test11(Class c, MyValue1 vt) {
        return c.cast(vt);
    }

    @DontCompile
    public void test11_verifier(boolean warmup) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        try {
            test11(MyValue2.class, vt);
            throw new RuntimeException("should have thrown");
        } catch (ClassCastException cce) {
        }
    }

    @Test()
    public Object test12(MyValue1 vt) {
        return MyValue1.class.cast(vt);
    }

    @DontCompile
    public void test12_verifier(boolean warmup) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        Object result = test12(vt);
        Asserts.assertEQ(((MyValue1)result).hash(), vt.hash());
    }

    @Test()
    public Object test13(MyValue1 vt) {
        return MyValue2.class.cast(vt);
    }

    @DontCompile
    public void test13_verifier(boolean warmup) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        try {
            test13(vt);
            throw new RuntimeException("should have thrown");
        } catch (ClassCastException cce) {
        }
    }

    // inline type array creation via reflection
    @Test()
    public void test14(int len, long hash) {
        Object[] va = (Object[])Array.newInstance(MyValue1.val.class, len);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(((MyValue1)va[i]).hashPrimitive(), hash);
        }
    }

    @DontCompile
    public void test14_verifier(boolean warmup) {
        int len = Math.abs(rI) % 42;
        long hash = MyValue1.createDefaultDontInline().hashPrimitive();
        test14(len, hash);
    }

    // Test hashCode() method
    @Test()
    public int test15(Object v) {
        return v.hashCode();
    }

    @DontCompile
    public void test15_verifier(boolean warmup) {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        int res = test15(v);
        Asserts.assertEQ(res, v.hashCode());
    }

    @Test()
    public int test16(Object v) {
        return System.identityHashCode(v);
    }

    @DontCompile
    public void test16_verifier(boolean warmup) {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        int res = test16(v);
        Asserts.assertEQ(res, System.identityHashCode((Object)v));
    }

    @Test()
    public int test17(Object v) {
        return System.identityHashCode(v);
    }

    @DontCompile
    public void test17_verifier(boolean warmup) {
        Integer v = Integer.valueOf(rI);
        int res = test17(v);
        Asserts.assertEQ(res, System.identityHashCode(v));
    }

    @Test()
    public int test18(Object v) {
        return System.identityHashCode(v);
    }

    @DontCompile
    public void test18_verifier(boolean warmup) {
        Object v = null;
        int res = test18(v);
        Asserts.assertEQ(res, System.identityHashCode(v));
    }

    // hashCode() and toString() with different inline types
    @Test()
    public int test19(MyValue1 vt1, MyValue1 vt2, boolean b) {
        MyValue1 res = b ? vt1 : vt2;
        return res.hashCode();
    }

    @DontCompile
    public void test19_verifier(boolean warmup) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        int res = test19(vt, vt, true);
        Asserts.assertEQ(res, vt.hashCode());
        res = test19(vt, vt, false);
        Asserts.assertEQ(res, vt.hashCode());
    }

    @Test()
    public String test20(MyValue1 vt1, MyValue1 vt2, boolean b) {
        MyValue1 res = b ? vt1 : vt2;
        return res.toString();
    }

    @DontCompile
    public void test20_verifier(boolean warmup) {
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
            Field xField = MyValue1.class.getDeclaredField("x");
            X_OFFSET = U.objectFieldOffset(xField);
            Field yField = MyValue1.class.getDeclaredField("y");
            Y_OFFSET = U.objectFieldOffset(yField);
            Field v1Field = MyValue1.class.getDeclaredField("v1");
            V1_OFFSET = U.objectFieldOffset(v1Field);
            V1_FLATTENED = U.isFlattened(v1Field);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static final String CALL_Unsafe = START + "CallStaticJava" + MID + "# Static  jdk.internal.misc.Unsafe::" + END;

    @Test(failOn=CALL_Unsafe)
    public int test21(MyValue1 v) {
       return U.getInt(v, X_OFFSET);
    }

    @DontCompile
    public void test21_verifier(boolean warmup) {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        int res = test21(v);
        Asserts.assertEQ(res, v.x);
    }

    MyValue1 test22_vt;
    @Test(failOn=CALL_Unsafe + ALLOC)
    public void test22(MyValue1 v) {
        v = U.makePrivateBuffer(v);
        U.putInt(v, X_OFFSET, rI);
        v = U.finishPrivateBuffer(v);
        test22_vt = v;
    }

    @DontCompile
    public void test22_verifier(boolean warmup) {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        test22(v.setX(v, 0));
        Asserts.assertEQ(test22_vt.hash(), v.hash());
    }

    @Test(failOn=CALL_Unsafe)
    public int test23(MyValue1 v, long offset) {
        return U.getInt(v, offset);
    }

    @DontCompile
    public void test23_verifier(boolean warmup) {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        int res = test23(v, X_OFFSET);
        Asserts.assertEQ(res, v.x);
    }

    MyValue1 test24_vt = MyValue1.createWithFieldsInline(rI, rL);

    @Test(failOn=CALL_Unsafe)
    public int test24(long offset) {
        return U.getInt(test24_vt, offset);
    }

    @DontCompile
    public void test24_verifier(boolean warmup) {
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

    @DontCompile
    public void test25_verifier(boolean warmup) {
        Test25Value vt = new Test25Value();
        test25(vt);
    }

    @Test
    public Object test26() {
        Class<?>[] ca = new Class<?>[1];
        for (int i = 0; i < 1; ++i) {
          // Folds during loop opts
          ca[i] = MyValue1.val.class;
        }
        return Array.newInstance(ca[0], 1);
    }

    @DontCompile
    public void test26_verifier(boolean warmup) {
        Object[] res = (Object[])test26();
        Asserts.assertEQ(((MyValue1)res[0]).hashPrimitive(), MyValue1.createDefaultInline().hashPrimitive());
    }

    // Load non-flattenable inline type field with unsafe
    MyValue1.ref test27_vt = MyValue1.createWithFieldsInline(rI, rL);
    private static final long TEST27_OFFSET;
    static {
        try {
            Field field = TestIntrinsics.class.getDeclaredField("test27_vt");
            TEST27_OFFSET = U.objectFieldOffset(field);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test(failOn=CALL_Unsafe)
    public MyValue1 test27() {
        return (MyValue1)U.getReference(this, TEST27_OFFSET);
    }

    @DontCompile
    public void test27_verifier(boolean warmup) {
        MyValue1 res = test27();
        Asserts.assertEQ(res.hash(), test24_vt.hash());
    }

    // Mismatched type
    @Test(failOn=CALL_Unsafe)
    public int test28(MyValue1 v) {
        return U.getByte(v, X_OFFSET);
    }

    @DontCompile
    public void test28_verifier(boolean warmup) {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        int res = test28(v);
        if (java.nio.ByteOrder.nativeOrder() == java.nio.ByteOrder.LITTLE_ENDIAN) {
            Asserts.assertEQ(res, (int)((byte)v.x));
        } else {
            Asserts.assertEQ(res, (int)((byte)Integer.reverseBytes(v.x)));
        }
    }

    // Wrong alignment
    @Test(failOn=CALL_Unsafe)
    public long test29(MyValue1 v) {
        // Read the field that's guaranteed to not be last in the
        // inline type so we don't read out of bounds.
        if (X_OFFSET < Y_OFFSET) {
            return U.getInt(v, X_OFFSET+1);
        }
        return U.getLong(v, Y_OFFSET+1);
    }

    @DontCompile
    public void test29_verifier(boolean warmup) {
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
    @Test(failOn=CALL_Unsafe)
    public MyValue2 test30(MyValue1 v) {
        if (V1_FLATTENED) {
            return U.getValue(v, V1_OFFSET, MyValue2.val.class);
        }
        return (MyValue2)U.getReference(v, V1_OFFSET);
    }

    @DontCompile
    public void test30_verifier(boolean warmup) {
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
    @Test(failOn=CALL_Unsafe)
    public MyValue1 test31() {
        if (TEST31_VT_FLATTENED) {
            return U.getValue(this, TEST31_VT_OFFSET, MyValue1.val.class);
        }
        return (MyValue1)U.getReference(this, TEST31_VT_OFFSET);
    }

    @DontCompile
    public void test31_verifier(boolean warmup) {
        test31_vt = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 res = test31();
        Asserts.assertEQ(res.hash(), test31_vt.hash());
    }

    // putValue to set flattened field in object
    @Test(failOn=CALL_Unsafe)
    public void test32(MyValue1 vt) {
        if (TEST31_VT_FLATTENED) {
            U.putValue(this, TEST31_VT_OFFSET, MyValue1.val.class, vt);
        } else {
            U.putReference(this, TEST31_VT_OFFSET, vt);
        }
    }

    @DontCompile
    public void test32_verifier(boolean warmup) {
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
    @Test(failOn=CALL_Unsafe)
    public MyValue1 test33(MyValue1[] arr) {
        if (TEST33_FLATTENED_ARRAY) {
            return U.getValue(arr, TEST33_BASE_OFFSET + TEST33_INDEX_SCALE, MyValue1.val.class);
        }
        return (MyValue1)U.getReference(arr, TEST33_BASE_OFFSET + TEST33_INDEX_SCALE);
    }

    @DontCompile
    public void test33_verifier(boolean warmup) {
        MyValue1[] arr = new MyValue1[2];
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        arr[1] = vt;
        MyValue1 res = test33(arr);
        Asserts.assertEQ(res.hash(), vt.hash());
    }

    // putValue to set flattened field in array
    @Test(failOn=CALL_Unsafe)
    public void test34(MyValue1[] arr, MyValue1 vt) {
        if (TEST33_FLATTENED_ARRAY) {
            U.putValue(arr, TEST33_BASE_OFFSET + TEST33_INDEX_SCALE, MyValue1.val.class, vt);
        } else {
            U.putReference(arr, TEST33_BASE_OFFSET + TEST33_INDEX_SCALE, vt);
        }
    }

    @DontCompile
    public void test34_verifier(boolean warmup) {
        MyValue1[] arr = new MyValue1[2];
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        test34(arr, vt);
        Asserts.assertEQ(arr[1].hash(), vt.hash());
    }

    // getValue to retrieve flattened field from object with unknown
    // container type
    @Test(failOn=CALL_Unsafe)
    public MyValue1 test35(Object o) {
        if (TEST31_VT_FLATTENED) {
            return U.getValue(o, TEST31_VT_OFFSET, MyValue1.val.class);
        }
        return (MyValue1)U.getReference(o, TEST31_VT_OFFSET);
    }

    @DontCompile
    public void test35_verifier(boolean warmup) {
        test31_vt = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 res = test35(this);
        Asserts.assertEQ(res.hash(), test31_vt.hash());
    }

    // getValue to retrieve flattened field from object at unknown
    // offset
    @Test(failOn=CALL_Unsafe)
    public MyValue1 test36(long offset) {
        if (TEST31_VT_FLATTENED) {
            return U.getValue(this, offset, MyValue1.val.class);
        }
        return (MyValue1)U.getReference(this, offset);
    }

    @DontCompile
    public void test36_verifier(boolean warmup) {
        test31_vt = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 res = test36(TEST31_VT_OFFSET);
        Asserts.assertEQ(res.hash(), test31_vt.hash());
    }

    // putValue to set flattened field in object with unknown
    // container
    @Test(failOn=CALL_Unsafe)
    public void test37(Object o, MyValue1 vt) {
        if (TEST31_VT_FLATTENED) {
            U.putValue(o, TEST31_VT_OFFSET, MyValue1.val.class, vt);
        } else {
            U.putReference(o, TEST31_VT_OFFSET, vt);
        }
    }

    @DontCompile
    public void test37_verifier(boolean warmup) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        test31_vt = MyValue1.createDefaultInline();
        test37(this, vt);
        Asserts.assertEQ(vt.hash(), test31_vt.hash());
    }

    // putValue to set flattened field in object, non inline argument
    // to store
    @Test(match = { CALL_Unsafe }, matchCount = { 1 })
    public void test38(Object o) {
        if (TEST31_VT_FLATTENED) {
            U.putValue(this, TEST31_VT_OFFSET, MyValue1.val.class, o);
        } else {
            U.putReference(this, TEST31_VT_OFFSET, o);
        }
    }

    @DontCompile
    public void test38_verifier(boolean warmup) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        test31_vt = MyValue1.createDefaultInline();
        test38(vt);
        Asserts.assertEQ(vt.hash(), test31_vt.hash());
    }

    @Test(failOn=CALL_Unsafe)
    public MyValue1 test39(MyValue1 v) {
        v = U.makePrivateBuffer(v);
        U.putInt(v, X_OFFSET, rI);
        v = U.finishPrivateBuffer(v);
        return v;
    }

    @DontCompile
    public void test39_verifier(boolean warmup) {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 res = test39(v.setX(v, 0));
        Asserts.assertEQ(res.hash(), v.hash());
    }

    // Test default inline type array creation via reflection
    @Test()
    public Object[] test40(Class<?> componentType, int len) {
        Object[] va = (Object[])Array.newInstance(componentType, len);
        return va;
    }

    @DontCompile
    public void test40_verifier(boolean warmup) {
        int len = Math.abs(rI) % 42;
        Object[] va = test40(MyValue1.ref.class, len);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(va[i], null);
        }
    }

    // Class.isInstance
    @Test()
    public boolean test41(Class c, MyValue1.ref vt) {
        return c.isInstance(vt);
    }

    @DontCompile
    public void test41_verifier(boolean warmup) {
        MyValue1.ref vt = MyValue1.createWithFieldsInline(rI, rL);
        boolean result = test41(MyValue1.ref.class, vt);
        Asserts.assertTrue(result);
        result = test41(MyValue1.class, vt);
        Asserts.assertTrue(result);
    }

    @Test()
    public boolean test42(Class c, MyValue1.ref vt) {
        return c.isInstance(vt);
    }

    @DontCompile
    public void test42_verifier(boolean warmup) {
        MyValue1.ref vt = MyValue1.createWithFieldsInline(rI, rL);
        boolean result = test42(MyValue2.ref.class, vt);
        Asserts.assertFalse(result);
        result = test42(MyValue2.class, vt);
        Asserts.assertFalse(result);
    }

    // Class.cast
    @Test()
    public Object test43(Class c, MyValue1.ref vt) {
        return c.cast(vt);
    }

    @DontCompile
    public void test43_verifier(boolean warmup) {
        MyValue1.ref vt = MyValue1.createWithFieldsInline(rI, rL);
        Object result = test43(MyValue1.ref.class, vt);
        Asserts.assertEQ(((MyValue1)result).hash(), vt.hash());
        result = test43(MyValue1.ref.class, null);
        Asserts.assertEQ(result, null);
    }

    @Test()
    public Object test44(Class c, MyValue1.ref vt) {
        return c.cast(vt);
    }

    @DontCompile
    public void test44_verifier(boolean warmup) {
        MyValue1.ref vt = MyValue1.createWithFieldsInline(rI, rL);
        try {
            test44(MyValue2.ref.class, vt);
            throw new RuntimeException("should have thrown");
        } catch (ClassCastException cce) {
        }
    }

    @Test()
    public Object test45(MyValue1.ref vt) {
        return MyValue1.ref.class.cast(vt);
    }

    @DontCompile
    public void test45_verifier(boolean warmup) {
        MyValue1.ref vt = MyValue1.createWithFieldsInline(rI, rL);
        Object result = test45(vt);
        Asserts.assertEQ(((MyValue1)result).hash(), vt.hash());
        result = test45(null);
        Asserts.assertEQ(result, null);
    }

    @Test()
    public Object test46(MyValue1.ref vt) {
        return MyValue2.ref.class.cast(vt);
    }

    @DontCompile
    public void test46_verifier(boolean warmup) {
        MyValue1.ref vt = MyValue1.createWithFieldsInline(rI, rL);
        test46(null);
        try {
            test46(vt);
            throw new RuntimeException("should have thrown");
        } catch (ClassCastException cce) {
        }
    }

    @Test()
    public Object test47(MyValue1.ref vt) {
        return MyValue1.val.class.cast(vt);
    }

    @DontCompile
    public void test47_verifier(boolean warmup) {
        MyValue1.ref vt = MyValue1.createWithFieldsInline(rI, rL);
        Object result = test47(vt);
        Asserts.assertEQ(((MyValue1)result).hash(), vt.hash());
        try {
            test47(null);
            throw new RuntimeException("should have thrown");
        } catch (NullPointerException npe) {
        }
    }

    @Test()
    public Object test48(Class c, MyValue1.ref vt) {
        return c.cast(vt);
    }

    @DontCompile
    public void test48_verifier(boolean warmup) {
        MyValue1.ref vt = MyValue1.createWithFieldsInline(rI, rL);
        Object result = test48(MyValue1.class, vt);
        Asserts.assertEQ(((MyValue1)result).hash(), vt.hash());
        try {
            test48(MyValue1.class, null);
            throw new RuntimeException("should have thrown");
        } catch (NullPointerException npe) {
        }
    }

    @Test()
    public Object test49(MyValue1 vt) {
        return MyValue1.ref.class.cast(vt);
    }

    @DontCompile
    public void test49_verifier(boolean warmup) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        Object result = test49(vt);
        Asserts.assertEQ(((MyValue1)result).hash(), vt.hash());
    }

    @Test()
    public Object test50(Class c, Object obj) {
        return c.cast(obj);
    }

    @DontCompile
    public void test50_verifier(boolean warmup) {
        MyValue1 vt = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1[] va  = new MyValue1[42];
        MyValue1.ref[] vba = new MyValue1.ref[42];
        Object result = test50(MyValue1.class, vt);
        Asserts.assertEQ(((MyValue1)result).hash(), vt.hash());
        result = test50(MyValue1.ref.class, vt);
        Asserts.assertEQ(((MyValue1)result).hash(), vt.hash());
        result = test50(MyValue1[].class, va);
        Asserts.assertEQ(result, va);
        result = test50(MyValue1.ref[].class, vba);
        Asserts.assertEQ(result, vba);
        result = test50(MyValue1.ref[].class, va);
        Asserts.assertEQ(result, va);
        try {
            test50(MyValue1.class, null);
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
    @Test()
    public void test51(int len) {
        Object[] va = (Object[])Array.newInstance(MyValue1.ref.class, len);
        for (int i = 0; i < len; ++i) {
            Asserts.assertEQ(va[i], null);
        }
    }

    @DontCompile
    public void test51_verifier(boolean warmup) {
        int len = Math.abs(rI) % 42;
        test51(len);
    }

    // multidimensional inline type array creation via reflection
    @Test()
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

    @DontCompile
    public void test52_verifier(boolean warmup) {
        test52(1, 1);
        test52(1, 2);
    }

    @Test()
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

    @DontCompile
    public void test53_verifier(boolean warmup) {
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

    @Test()
    @Warmup(10000) // Fill up the TLAB to trigger slow path allocation
    public MyValue1 test54(MyValue1 v) {
        v = U.makePrivateBuffer(v);
        test54_callee(v);
        v = U.finishPrivateBuffer(v);
        return v;
    }

    @DontCompile
    public void test54_verifier(boolean warmup) {
        MyValue1 v = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 res = test54(v.setX(v, 0));
        Asserts.assertEQ(res.hash(), v.hash());
    }

    static final MyValue1 test55_vt = MyValue1.createWithFieldsInline(rI, rL);

    // Same as test30 but with constant field holder
    @Test(failOn=CALL_Unsafe)
    public MyValue2 test55() {
        if (V1_FLATTENED) {
            return U.getValue(test55_vt, V1_OFFSET, MyValue2.val.class);
        }
        return (MyValue2)U.getReference(test55_vt, V1_OFFSET);
    }

    @DontCompile
    public void test55_verifier(boolean warmup) {
        MyValue2 res = test55();
        Asserts.assertEQ(res.hash(), test55_vt.v1.hash());
    }

    // Test OptimizePtrCompare part of Escape Analysis
    @Test()
    public void test56(int idx) {
        Object[] va = (Object[])Array.newInstance(MyValue1.val.class, 1);
        if (va[idx] == null) {
            throw new RuntimeException("Unexpected null");
        }
    }

    @DontCompile
    public void test56_verifier(boolean warmup) {
        test56(0);
    }

    // Same as test56 but with load from known array index
    @Test()
    public void test57() {
        Object[] va = (Object[])Array.newInstance(MyValue1.val.class, 1);
        if (va[0] == null) {
            throw new RuntimeException("Unexpected null");
        }
    }

    @DontCompile
    public void test57_verifier(boolean warmup) {
        test57();
    }

    // Test unsafe allocation
    @Test()
    public boolean test58(Class<?> c1, Class<?> c2) throws Exception {
        Object obj1 = U.allocateInstance(c1);
        Object obj2 = U.allocateInstance(c2);
        return obj1 == obj2;
    }

    @DontCompile
    public void test58_verifier(boolean warmup) throws Exception {
        boolean res = test58(MyValue1.class, MyValue1.class);
        Asserts.assertTrue(res);
        res = test58(Object.class, MyValue1.class);
        Asserts.assertFalse(res);
        res = test58(MyValue1.class, Object.class);
        Asserts.assertFalse(res);
    }

    // Test synchronization on unsafe inline type allocation
    @Test()
    public void test59(Class<?> c) throws Exception {
        Object obj = U.allocateInstance(c);
        synchronized (obj) {

        }
    }

    @DontCompile
    public void test59_verifier(boolean warmup) throws Exception {
        test59(Integer.class);
        try {
            test59(MyValue1.class);
            throw new RuntimeException("test59 failed: synchronization on inline type should not succeed");
        } catch (IllegalMonitorStateException e) {

        }
    }

    // Test mark word load optimization on unsafe inline type allocation
    @Test()
    public boolean test60(Class<?> c1, Class<?> c2, boolean b1, boolean b2) throws Exception {
        Object obj1 = b1 ? new Object() : U.allocateInstance(c1);
        Object obj2 = b2 ? new Object() : U.allocateInstance(c2);
        return obj1 == obj2;
    }

    @DontCompile
    public void test60_verifier(boolean warmup) throws Exception {
        Asserts.assertTrue(test60(MyValue1.class, MyValue1.class, false, false));
        Asserts.assertFalse(test60(MyValue1.class, MyValue2.class, false, false));
        Asserts.assertFalse(test60(MyValue1.class, MyValue1.class, false, true));
        Asserts.assertFalse(test60(MyValue1.class, MyValue1.class, true, false));
        Asserts.assertFalse(test60(MyValue1.class, MyValue1.class, true, true));
    }
}
