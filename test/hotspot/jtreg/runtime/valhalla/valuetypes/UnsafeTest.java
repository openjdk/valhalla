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

package runtime.valhalla.valuetypes;

/*
 * @test UnsafeTest
 * @summary unsafe get/put/with value
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile -XDallowWithFieldOperator Point.java UnsafeTest.java
 * @run main/othervm -Xint -XX:+EnableValhalla runtime.valhalla.valuetypes.UnsafeTest
 * @run main/othervm -Xcomp -XX:+EnableValhalla runtime.valhalla.valuetypes.UnsafeTest
 */

import jdk.internal.misc.Unsafe;

import java.lang.reflect.*;
import java.util.List;
import static jdk.test.lib.Asserts.*;

public class UnsafeTest {
    static final Unsafe U = Unsafe.getUnsafe();

    static value class Value1 {
        Point point;
        Point[] array;
        Value1() {
            this.point = Point.createPoint(1, 1);
            this.array = new Point[0];
        }

        static Value1 create(Point p, Point... points) {
            Value1 o = Value1.default;
            o = __WithField(o.point, p);
            o = __WithField(o.array, points);
            return o;
        }
    }

    static value class Value2 {
        int i;
        Value1 v;

        Value2() {
            this.i = 0;
            this.v = Value1.create(Point.createPoint(0,0), new Point[0]);
        }

        static Value2 create(Value1 v, int i) {
            Value2 o = Value2.default;
            o = __WithField(o.v, v);
            o = __WithField(o.i, i);
            return o;
        }
    }

    static value class Value3 {
        Object o;
        Value2 v;

        Value3() {
            this.v = Value2.create(Value1.create(Point.createPoint(0,0), new Point[0]), 0);
            this.o = new Object();
        }

        static Value3 create(Value2 v, Object ref) {
            Value3 o = Value3.default;
            o = __WithField(o.v, v);
            o = __WithField(o.o, ref);
            return o;
        }
    }


    public static void main(String[] args) throws Throwable {
        printValueClass(Value3.class, 0);

        Value1 v1 = Value1.create(Point.createPoint(10,10), Point.createPoint(20,20), Point.createPoint(30,30));
        Value2 v2 = Value2.create(v1, 20);
        Value3 v3 = Value3.create(v2, List.of("Value3"));
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
        Value3 v = v3;
        try {
            v = U.makePrivateBuffer(v);
            // patch v3.o
            U.putObject(v, off_o, List.of("Value1", "Value2", "Value3"));
            // patch v3.v.i;
            U.putInt(v, off_v + off_i - U.valueHeaderSize(Value2.class), 999);
            // patch v3.v.v.point
            U.putValue(v, off_v + off_v2 - U.valueHeaderSize(Value2.class) + off_point - U.valueHeaderSize(Value1.class),
                       Point.class, Point.createPoint(100, 100));
        } finally {
            v = U.finishPrivateBuffer(v);
        }

        assertEquals(v.v.v.point, Point.createPoint(100, 100));
        assertEquals(v.v.i, 999);
        assertEquals(v.o, List.of("Value1", "Value2", "Value3"));
        assertEquals(v.v.v.array, v1.array);

        Value1 nv1 = Value1.create(Point.createPoint(70,70), Point.createPoint(80,80), Point.createPoint(90,90));
        Value2 nv2 = Value2.create(nv1, 100);
        Value3 nv3 = Value3.create(nv2, List.of("Value1", "Value2", "Value3"));

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
                              U.isFlattened(f) ? "flattened " : "", f.getType(),
                              U.objectFieldOffset(vc, f.getName()));
            if (U.isFlattened(f)) {
                printValueClass(f.getType(), level+1);
            }
        }
    }
}
