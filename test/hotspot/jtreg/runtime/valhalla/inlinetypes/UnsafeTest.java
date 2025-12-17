/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

package runtime.valhalla.inlinetypes;



/*
 * @test UnsafeTest
 * @requires vm.debug == true
 * @summary unsafe get/put/with inline type
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.value
 * @library /test/lib
 * @modules java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @requires vm.flagless
 * @compile Point.java UnsafeTest.java
 * @run main/othervm -Xint -XX:+UseNullableValueFlattening -XX:+UseArrayFlattening -XX:+UseFieldFlattening -XX:+PrintInlineLayout runtime.valhalla.inlinetypes.UnsafeTest
 */

// TODO 8350865 Implement unsafe intrinsics for nullable flat fields/arrays in C2

import jdk.internal.misc.Unsafe;
import jdk.internal.value.ValueClass;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;
import jdk.test.lib.Asserts;

import java.lang.reflect.*;
import java.util.List;
import static jdk.test.lib.Asserts.*;

public class UnsafeTest {
    static final Unsafe U = Unsafe.getUnsafe();

    @LooselyConsistentValue
    static value class Value1 {
        @NullRestricted
        Point point;
        Point[] array;
        Value1(Point p, Point... points) {
            this.point = p;
            this.array = points;
        }
    }

    @LooselyConsistentValue
    static value class Value2 {
        int i;
        @NullRestricted
        Value1 v;

        Value2(Value1 v, int i) {
            this.v = v;
            this.i = i;
        }
    }

    @LooselyConsistentValue
    static value class Value3 {
        Object o;
        @NullRestricted
        Value2 v;

        Value3(Value2 v, Object ref) {
            this.v = v;
            this.o = ref;
        }

    }

    public static void test0() throws Throwable {
        printValueClass(Value3.class, 0);

        Value1 v1 = new Value1(new Point(10,10), new Point(20,20), new Point(30,30));
        Value2 v2 = new Value2(v1, 20);
        Value3 v3 = new Value3(v2, List.of("Value3"));
        long off_o = U.objectFieldOffset(Value3.class, "o");
        long off_v = U.objectFieldOffset(Value3.class, "v");
        long off_i = U.objectFieldOffset(Value2.class, "i");
        long off_v2 = U.objectFieldOffset(Value2.class, "v");
        int layout_v2 = U.fieldLayout(Value2.class.getDeclaredField("v"));

        long off_point = U.objectFieldOffset(Value1.class, "point");
        int layout_point = U.fieldLayout(Value1.class.getDeclaredField("point"));

        List<String> list = List.of("Value1", "Value2", "Value3");
        Value3 v = v3;
        try {
            v = U.makePrivateBuffer(v);
            // patch v3.o
            U.putReference(v, off_o, list);
            // patch v3.v.i;
            U.putInt(v, off_v + off_i - U.valueHeaderSize(Value2.class), 999);
            // patch v3.v.v.point
            U.putFlatValue(v, off_v + off_v2 - U.valueHeaderSize(Value2.class) + off_point - U.valueHeaderSize(Value1.class),
                           layout_point, Point.class, new Point(100, 100));
        } finally {
            v = U.finishPrivateBuffer(v);
        }

        assertEquals(v.v.v.point, new Point(100, 100));
        assertEquals(v.v.i, 999);
        assertEquals(v.o, list);
        assertEquals(v.v.v.array, v1.array);

        Value1 nv1 = new Value1(new Point(70,70), new Point(80,80), new Point(90,90));
        Value2 nv2 = new Value2(nv1, 100);
        Value3 nv3 = new Value3(nv2, list);

        try {
            v = U.makePrivateBuffer(v);
            // patch v3.v
            U.putFlatValue(v, off_v2, layout_v2, Value2.class, nv2);
        } finally {
            v = U.finishPrivateBuffer(v);
        }
        assertEquals(v, nv3);
    }

    static void printValueClass(Class<?> vc, int level) {
        String indent = "";
        for (int i=0; i < level; i++) {
            indent += "  ";
        }
        System.out.format("%s%s header size %d%n", indent, vc, U.valueHeaderSize(vc));
        for (Field f : vc.getDeclaredFields()) {
            System.out.format("%s%s: %s%s offset %d%n", indent, f.getName(),
                              U.isFlatField(f) ? "flattened " : "", f.getType(),
                              U.objectFieldOffset(vc, f.getName()));
            if (U.isFlatField(f)) {
                printValueClass(f.getType(), level+1);
            }
        }
    }

    // Requires -XX:+UseNullableValueFlattening
    static value class MyValue0 {
        int val;

        public MyValue0(int i) {
            val = i;
        }
    }

    static class Container0 {
        MyValue0 v;
    }

    public static void test1() throws Throwable {
        Container0 c = new Container0();
        Class<?> cc = Container0.class;
        Field[] fields = cc.getDeclaredFields();
        Asserts.assertEquals(fields.length, 1);
        Field f = fields[0];
        System.out.println("Field found: " + f);
        Asserts.assertTrue(U.isFlatField(f));
        Asserts.assertTrue(U.hasNullMarker(f));
        int nmOffset = U.nullMarkerOffset(f);
        Asserts.assertNotEquals(nmOffset, -1);
        byte nm = U.getByte(c, nmOffset);
        Asserts.assertEquals(nm, (byte)0);
        c.v = new MyValue0(42);
        Asserts.assertNotNull(c.v);
        nm = U.getByte(c, nmOffset);
        Asserts.assertNotEquals(nm, 0);
        U.getAndSetByteRelease(c, nmOffset, (byte)0);
        Asserts.assertNull(c.v);
    }

    static value class TestValue1  {
        short s0,s1;

        TestValue1() {
            s0 = 0;
            s1 = 0;
        }

        TestValue1(short v0, short v1) {
            s0 = v0;
            s1 = v1;
        }
    }

    static class Container1 {
        TestValue1 value;
    }

    // Testing of nullable flat field supports in Unsafe.getFlatValue()/Unsafe.putFlatValue()
    public static void testNullableFlatFields() throws Throwable {
        Container1 c = new Container1();
        Class<?> cc = Container1.class;
        Field field = cc.getDeclaredField("value");
        Class<?> fc = TestValue1.class;
        long offset = U.objectFieldOffset(field);
        int layoutKind = U.fieldLayout(field);
        if (!U.isFlatField(field)) return; // Field not flattened (due to VM flags?), test doesn't apply
        // Initial value of the field must be null
        Asserts.assertNull(U.getFlatValue(c, offset, layoutKind, fc));
        // Writing all zero value to the field, field must become non-null
        TestValue1 val0 = new TestValue1((short)0, (short)0);
        U.putFlatValue(c, offset, layoutKind, fc, val0);
        TestValue1 rval = U.getFlatValue(c, offset, layoutKind, fc);
        Asserts.assertNotNull(rval);
        Asserts.assertEQ((short)0, rval.s0);
        Asserts.assertEQ((short)0, rval.s1);
        Asserts.assertEQ((short)0, c.value.s0);
        Asserts.assertEQ((short)0, c.value.s1);
        // Writing null to the field, field must become null again
        U.putFlatValue(c, offset, layoutKind, fc, null);
        Asserts.assertNull(U.getFlatValue(c, offset, layoutKind, fc));
        Asserts.assertNull(c.value);
        // Writing non zero value to the field
        TestValue1 val1 = new TestValue1((short)-1, (short)-2);
        U.putFlatValue(c, offset, layoutKind, fc, val1);
        rval = U.getFlatValue(c, offset, layoutKind, fc);
        Asserts.assertNotNull(rval);
        Asserts.assertNotNull(c.value);
        Asserts.assertEQ((short)-1, rval.s0);
        Asserts.assertEQ((short)-2, rval.s1);
        Asserts.assertEQ((short)-1, c.value.s0);
        Asserts.assertEQ((short)-2, c.value.s1);
        // Writing a different non zero value
        TestValue1 val2 = new TestValue1((short)Short.MAX_VALUE, (short)3);
        U.putFlatValue(c, offset, layoutKind, fc, val2);
        rval = U.getFlatValue(c, offset, layoutKind, fc);
        Asserts.assertNotNull(rval);
        Asserts.assertNotNull(c.value);
        Asserts.assertEQ(Short.MAX_VALUE, c.value.s0);
        Asserts.assertEQ((short)3, rval.s1);
        Asserts.assertEQ(Short.MAX_VALUE, c.value.s0);
        Asserts.assertEQ((short)3, rval.s1);
    }

    // Testing of nullable flat arrays supports in Unsafe.getFlatValue()/Unsafe.putFlatValue()
    public static void testNullableFlatArrays() throws Throwable {
        final int ARRAY_LENGTH = 10;
        TestValue1[] array = (TestValue1[])ValueClass.newNullableAtomicArray(TestValue1.class, ARRAY_LENGTH);
        long baseOffset = U.arrayInstanceBaseOffset(array);
        int scaleIndex = U.arrayInstanceIndexScale(array);
        int layoutKind = U.arrayLayout(array);
        for (int i = 0; i < ARRAY_LENGTH; i++) {
            Asserts.assertNull(U.getFlatValue(array, baseOffset + i * scaleIndex, layoutKind, TestValue1.class));
        }
        TestValue1 val = new TestValue1((short)0, (short)0);
        for (int i = 0; i < ARRAY_LENGTH; i++) {
            if (i % 2 == 0) {
                U.putFlatValue(array, baseOffset + i * scaleIndex, layoutKind, TestValue1.class, val );
            }
        }
        for (int i = 0; i < ARRAY_LENGTH; i++) {
            if (i % 2 == 0) {
                Asserts.assertNotNull(U.getFlatValue(array, baseOffset + i * scaleIndex, layoutKind, TestValue1.class));
                Asserts.assertNotNull(array[i]);
            } else {
                Asserts.assertNull(U.getFlatValue(array, baseOffset + i * scaleIndex, layoutKind, TestValue1.class));
                Asserts.assertNull(array[i]);
            }
        }
        TestValue1 val2 = new TestValue1((short)Short.MAX_VALUE, (short)Short.MIN_VALUE);
        for (int i = 0; i < ARRAY_LENGTH; i++) {
            if (i % 2 != 0) {
                U.putFlatValue(array, baseOffset + i * scaleIndex, layoutKind, TestValue1.class, val2 );
            } else {
                U.putFlatValue(array, baseOffset + i * scaleIndex, layoutKind, TestValue1.class, null );
            }
        }
        for (int i = 0; i < ARRAY_LENGTH; i++) {
            if (i % 2 != 0) {
                TestValue1 rval = U.getFlatValue(array, baseOffset + i * scaleIndex, layoutKind, TestValue1.class);
                Asserts.assertNotNull(rval);
                Asserts.assertEQ(val2.s0, rval.s0);
                Asserts.assertEQ(val2.s1, rval.s1);
                Asserts.assertNotNull(array[i]);
                Asserts.assertEQ(val2.s0, array[i].s0);
                Asserts.assertEQ(val2.s1, array[i].s1);
            } else {
                Asserts.assertNull(U.getFlatValue(array, baseOffset + i * scaleIndex, layoutKind, TestValue1.class));
                Asserts.assertNull(array[i]);
            }
        }
        for (int i = 0; i < ARRAY_LENGTH; i++) {
            U.putFlatValue(array, baseOffset + i * scaleIndex, layoutKind, TestValue1.class, null );
        }
        for (int i = 0; i < ARRAY_LENGTH; i++) {
            Asserts.assertNull(U.getFlatValue(array, baseOffset + i * scaleIndex, layoutKind, TestValue1.class));
            Asserts.assertNull(array[i]);
        }
    }

    public static void main(String[] args) throws Throwable {
        test0();
        test1();
        testNullableFlatFields();
        testNullableFlatArrays();
    }

}
