/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @modules java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @compile Point.java UnsafeTest.java
 * @run main/othervm -XX:FlatArrayElementMaxSize=-1 -XX:InlineFieldMaxFlatSize=-1 -XX:+EnableNullableFieldFlattening runtime.valhalla.inlinetypes.UnsafeTest
 */

import jdk.internal.misc.Unsafe;
import jdk.internal.misc.VM;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;
import jdk.test.lib.Asserts;

import java.lang.reflect.*;
import java.util.List;
import static jdk.test.lib.Asserts.*;

public class UnsafeTest {
    static final Unsafe U = Unsafe.getUnsafe();

    @ImplicitlyConstructible
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

    @ImplicitlyConstructible
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

    @ImplicitlyConstructible
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

        long off_point = U.objectFieldOffset(Value1.class, "point");

        /*
         * Layout of Value3
         *
         * | valueheader | o | i | x | y | array |
         *                       ^-------^
         *                        Point
         *                       ^---------------^
         *                        Value1
         *
         *                   ^-------------------^
         *                    Value2
         */
        List<String> list = List.of("Value1", "Value2", "Value3");
        Value3 v = v3;
        try {
            v = U.makePrivateBuffer(v);
            // patch v3.o
            U.putObject(v, off_o, list);
            // patch v3.v.i;
            U.putInt(v, off_v + off_i - U.valueHeaderSize(Value2.class), 999);
            // patch v3.v.v.point
            U.putValue(v, off_v + off_v2 - U.valueHeaderSize(Value2.class) + off_point - U.valueHeaderSize(Value1.class),
                       Point.class, new Point(100, 100));
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
            U.putValue(v, off_v2, Value2.class, nv2);
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

    public static void main(String[] args) throws Throwable {
        test0();
        test1();
    }

}
