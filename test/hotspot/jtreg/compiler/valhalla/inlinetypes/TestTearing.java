/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import jdk.test.lib.Asserts;
import jdk.internal.misc.Unsafe;

import jdk.internal.value.ValueClass;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;
import jdk.internal.vm.annotation.Strict;

/**
 * @test TestTearing
 * @key randomness
 * @summary Detect tearing on flat writes and buffering due to missing barriers.
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @enablePreview
 * @modules java.base/jdk.internal.misc
 *          java.base/jdk.internal.value
 *          java.base/jdk.internal.vm.annotation
 * @run main/othervm -XX:-UseFieldFlattening -XX:-UseArrayFlattening
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+StressGCM -XX:+StressLCM
 *                   compiler.valhalla.inlinetypes.TestTearing
 * @run main/othervm -XX:-UseFieldFlattening -XX:-UseArrayFlattening
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+StressGCM -XX:+StressLCM
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+AlwaysIncrementalInline
 *                   compiler.valhalla.inlinetypes.TestTearing
 * @run main/othervm -XX:-UseFieldFlattening -XX:-UseArrayFlattening
 *                   -XX:CompileCommand=dontinline,*::incrementAndCheck*
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+StressGCM -XX:+StressLCM
 *                   compiler.valhalla.inlinetypes.TestTearing
 * @run main/othervm -XX:-UseFieldFlattening -XX:-UseArrayFlattening
 *                   -XX:CompileCommand=dontinline,*::incrementAndCheck*
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+StressGCM -XX:+StressLCM
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+AlwaysIncrementalInline
 *                   compiler.valhalla.inlinetypes.TestTearing
 *
 * @run main/othervm -XX:+UseNullableValueFlattening -XX:+UseAtomicValueFlattening
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+StressGCM -XX:+StressLCM
 *                   compiler.valhalla.inlinetypes.TestTearing
 * @run main/othervm -XX:+UseNullableValueFlattening -XX:+UseAtomicValueFlattening
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+StressGCM -XX:+StressLCM
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+AlwaysIncrementalInline
 *                   compiler.valhalla.inlinetypes.TestTearing
 * @run main/othervm -XX:+UseNullableValueFlattening -XX:+UseAtomicValueFlattening
 *                   -XX:CompileCommand=dontinline,*::incrementAndCheck*
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+StressGCM -XX:+StressLCM
 *                   compiler.valhalla.inlinetypes.TestTearing
 * @run main/othervm -XX:+UseNullableValueFlattening -XX:+UseAtomicValueFlattening
 *                   -XX:CompileCommand=dontinline,*::incrementAndCheck*
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+StressGCM -XX:+StressLCM
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+AlwaysIncrementalInline
 *                   compiler.valhalla.inlinetypes.TestTearing
 */

@LooselyConsistentValue
value class MyValue {
    // Make sure the payload size is <= 64-bit to enable atomic flattening
    short x;
    short y;

    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long X_OFFSET;
    private static final long Y_OFFSET;
    static {
        try {
            Field xField = MyValue.class.getDeclaredField("x");
            X_OFFSET = U.objectFieldOffset(xField);
            Field yField = MyValue.class.getDeclaredField("y");
            Y_OFFSET = U.objectFieldOffset(yField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static final MyValue DEFAULT = new MyValue((short)0, (short)0);

    MyValue(short x, short y) {
        this.x = x;
        this.y = y;
    }

    MyValue incrementAndCheck() {
        Asserts.assertEQ(x, y, "Inconsistent field values");
        return new MyValue((short)(x + 1), (short)(y + 1));
    }

    MyValue incrementAndCheckUnsafe() {
        Asserts.assertEQ(x, y, "Inconsistent field values");
        MyValue vt = U.makePrivateBuffer(this);
        U.putShort(vt, X_OFFSET, (short)(x + 1));
        U.putShort(vt, Y_OFFSET, (short)(y + 1));
        return U.finishPrivateBuffer(vt);
    }
}

public class TestTearing {
    // Null-free, volatile -> atomic access
    @Strict
    @NullRestricted
    volatile static MyValue field1 = MyValue.DEFAULT;
    @Strict
    @NullRestricted
    volatile MyValue field2 = MyValue.DEFAULT;

    // Nullable fields are always atomic
    static MyValue field3 = new MyValue((short)0, (short)0);
    MyValue field4 = new MyValue((short)0, (short)0);

    // Final arrays
    static final MyValue[] array1 = (MyValue[])ValueClass.newNullRestrictedAtomicArray(MyValue.class, 1, MyValue.DEFAULT);
    static final MyValue[] array2 = (MyValue[])ValueClass.newNullableAtomicArray(MyValue.class, 1);
    static {
        array2[0] = new MyValue((short)0, (short)0);
    }
    static final MyValue[] array3 = new MyValue[] { new MyValue((short)0, (short)0) };

    // Non-final arrays
    static MyValue[] array4 = (MyValue[])ValueClass.newNullRestrictedAtomicArray(MyValue.class, 1, MyValue.DEFAULT);
    static MyValue[] array5 = (MyValue[])ValueClass.newNullableAtomicArray(MyValue.class, 1);
    static {
        array5[0] = new MyValue((short)0, (short)0);
    }
    static MyValue[] array6 = new MyValue[] { new MyValue((short)0, (short)0) };

    // Object arrays
    static Object[] array7 = (MyValue[])ValueClass.newNullRestrictedAtomicArray(MyValue.class, 1, MyValue.DEFAULT);
    static Object[] array8 = (MyValue[])ValueClass.newNullableAtomicArray(MyValue.class, 1);
    static {
        array8[0] = new MyValue((short)0, (short)0);
    }
    static Object[] array9 = new MyValue[] { new MyValue((short)0, (short)0) };

    // Object arrays stored in volatile fields (for safe publication)
    static volatile Object[] array10 = (MyValue[])ValueClass.newNullRestrictedAtomicArray(MyValue.class, 1, MyValue.DEFAULT);
    static volatile Object[] array11 = (MyValue[])ValueClass.newNullableAtomicArray(MyValue.class, 1);
    static {
        array11[0] = new MyValue((short)0, (short)0);
    }
    static volatile Object[] array12 = new MyValue[] { new MyValue((short)0, (short)0) };

    static final MethodHandle incrementAndCheck_mh;

    static {
        try {
            Class<?> clazz = MyValue.class;
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodType mt = MethodType.methodType(MyValue.class);
            incrementAndCheck_mh = lookup.findVirtual(clazz, "incrementAndCheck", mt);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("Method handle lookup failed");
        }
    }

    static class Runner extends Thread {
        TestTearing test;

        public Runner(TestTearing test) {
            this.test = test;
        }

        public void run() {
            for (int i = 0; i < 1_000_000; ++i) {
                // Create "local" arrays so that C2 has full type info
                MyValue[] localArray1 = (MyValue[])ValueClass.newNullRestrictedAtomicArray(MyValue.class, 1, MyValue.DEFAULT);
                MyValue[] localArray2 = (MyValue[])ValueClass.newNullableAtomicArray(MyValue.class, 1);
                localArray2[0] = new MyValue((short)0, (short)0);
                MyValue[] localArray3 = new MyValue[] { new MyValue((short)0, (short)0) };

                Asserts.assertTrue(ValueClass.isAtomicArray(localArray1));
                Asserts.assertTrue(ValueClass.isAtomicArray(localArray2));
                Asserts.assertTrue(ValueClass.isAtomicArray(localArray3));
                Asserts.assertTrue(ValueClass.isNullRestrictedArray(localArray1));
                Asserts.assertFalse(ValueClass.isNullRestrictedArray(localArray2));
                Asserts.assertFalse(ValueClass.isNullRestrictedArray(localArray3));

                // Let them escape safely via a volatile field
                array10 = localArray1;
                array11 = localArray2;
                array12 = localArray3;

                localArray1[0] = localArray1[0].incrementAndCheck();
                localArray2[0] = localArray2[0].incrementAndCheck();
                localArray3[0] = localArray3[0].incrementAndCheck();

                test.field1 = test.field1.incrementAndCheck();
                test.field2 = test.field2.incrementAndCheck();
                test.field3 = test.field3.incrementAndCheck();
                test.field4 = test.field4.incrementAndCheck();
                array1[0] = array1[0].incrementAndCheck();
                array2[0] = array2[0].incrementAndCheck();
                array3[0] = array3[0].incrementAndCheck();
                array4[0] = array4[0].incrementAndCheck();
                array5[0] = array5[0].incrementAndCheck();
                array6[0] = array6[0].incrementAndCheck();
                array7[0] = ((MyValue)array7[0]).incrementAndCheck();
                array8[0] = ((MyValue)array8[0]).incrementAndCheck();
                array9[0] = ((MyValue)array9[0]).incrementAndCheck();
                array10[0] = ((MyValue)array10[0]).incrementAndCheck();
                array11[0] = ((MyValue)array11[0]).incrementAndCheck();
                array12[0] = ((MyValue)array12[0]).incrementAndCheck();

                test.field1 = test.field1.incrementAndCheckUnsafe();
                test.field2 = test.field2.incrementAndCheckUnsafe();
                test.field3 = test.field3.incrementAndCheckUnsafe();
                test.field4 = test.field4.incrementAndCheckUnsafe();
                array1[0] = array1[0].incrementAndCheckUnsafe();
                array2[0] = array2[0].incrementAndCheckUnsafe();
                array3[0] = array3[0].incrementAndCheckUnsafe();
                array4[0] = array4[0].incrementAndCheckUnsafe();
                array5[0] = array5[0].incrementAndCheckUnsafe();
                array6[0] = array6[0].incrementAndCheckUnsafe();
                array7[0] = ((MyValue)array7[0]).incrementAndCheckUnsafe();
                array8[0] = ((MyValue)array8[0]).incrementAndCheckUnsafe();
                array9[0] = ((MyValue)array9[0]).incrementAndCheckUnsafe();
                array10[0] = ((MyValue)array10[0]).incrementAndCheckUnsafe();
                array11[0] = ((MyValue)array11[0]).incrementAndCheckUnsafe();
                array12[0] = ((MyValue)array12[0]).incrementAndCheckUnsafe();

                try {
                    test.field1 = (MyValue)incrementAndCheck_mh.invokeExact(test.field1);
                    test.field2 = (MyValue)incrementAndCheck_mh.invokeExact(test.field2);
                    test.field3 = (MyValue)incrementAndCheck_mh.invokeExact(test.field1);
                    test.field4 = (MyValue)incrementAndCheck_mh.invokeExact(test.field2);
                    array1[0] = (MyValue)incrementAndCheck_mh.invokeExact(array1[0]);
                    array2[0] = (MyValue)incrementAndCheck_mh.invokeExact(array2[0]);
                    array3[0] = (MyValue)incrementAndCheck_mh.invokeExact(array3[0]);
                    array4[0] = (MyValue)incrementAndCheck_mh.invokeExact(array4[0]);
                    array5[0] = (MyValue)incrementAndCheck_mh.invokeExact(array5[0]);
                    array6[0] = (MyValue)incrementAndCheck_mh.invokeExact(array6[0]);
                    array7[0] = (MyValue)incrementAndCheck_mh.invokeExact((MyValue)array7[0]);
                    array8[0] = (MyValue)incrementAndCheck_mh.invokeExact((MyValue)array8[0]);
                    array9[0] = (MyValue)incrementAndCheck_mh.invokeExact((MyValue)array9[0]);
                    array10[0] = (MyValue)incrementAndCheck_mh.invokeExact((MyValue)array10[0]);
                    array11[0] = (MyValue)incrementAndCheck_mh.invokeExact((MyValue)array11[0]);
                    array12[0] = (MyValue)incrementAndCheck_mh.invokeExact((MyValue)array12[0]);
                } catch (Throwable t) {
                    throw new RuntimeException("Test failed", t);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // Create threads that concurrently update some value class (array) fields
        // and check the fields of the value classes for consistency to detect tearing.
        TestTearing test = new TestTearing();
        Thread runner = null;
        for (int i = 0; i < 10; ++i) {
            runner = new Runner(test);
            runner.start();
        }
        runner.join();
    }
}
