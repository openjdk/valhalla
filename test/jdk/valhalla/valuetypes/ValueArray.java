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

/*
 * @test
 * @summary Basic test for Array::get, Array::set, Arrays::setAll on value array
 * @compile -XDallowWithFieldOperator Point.java NonFlattenValue.java
 * @compile -XDallowWithFieldOperator ValueArray.java
 * @run testng/othervm -XX:+EnableValhalla -XX:+ValueArrayFlatten ValueArray
 * @run testng/othervm -XX:+EnableValhalla -XX:-ValueArrayFlatten ValueArray
 */

import java.lang.reflect.*;
import java.util.Arrays;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ValueArray {
    @DataProvider(name="arrayTypes")
    static Object[][] arrayTypes() {
        return new Object[][] {
            new Object[] { Object[].class,
                           new Object[] { new Object(), new Object()}},
            new Object[] { Point[].class,
                           new Point[] { Point.makePoint(1, 2),
                                         Point.makePoint(10, 20),
                                         Point.makePoint(100, 200)}},
            new Object[] { Point[][].class,
                           new Point[][] { new Point[] { Point.makePoint(1, 2),
                                                         Point.makePoint(10, 20)}}},

            new Object[] { NonFlattenValue[].class,
                           new NonFlattenValue[] { NonFlattenValue.make(1, 2),
                                                   NonFlattenValue.make(10, 20),
                                                   NonFlattenValue.make(100, 200)}},
        };
    }

    @Test(dataProvider="arrayTypes")
    public static void test(Class<?> c, Object[] elements) {
        ValueArray test = new ValueArray(c, elements.length);
        test.run(elements);
        if (c.getComponentType().isValue()) {
            test.ensureNonNullable();
        }
     }

    @Test
    public static void testPointArray() {
        PointArray array = PointArray.makeArray(Point.makePoint(1, 2), Point.makePoint(10, 20));
        ValueArray test = new ValueArray(array.points);
        test.run(Point.makePoint(3, 4), Point.makePoint(30, 40));
    }

    @Test
    public static void testIntArray() {
        int[] array = new int[] { 1, 2, 3};
        for (int i=0; i < array.length; i++) {
            Array.set(array, i, Integer.valueOf(i*10));
        }

        for (int i=0; i < array.length; i++) {
            Integer o = (Integer) Array.get(array, i);
            assertTrue(o.intValue() == i*10);
        }
        Arrays.setAll(array, i -> array[i]);
    }

    @Test
    public static void testNonArrayObject() {
        Object o = new Object();
        try {
            Array.get(o, 0);
            throw new AssertionError("IAE not thrown");
        } catch (IllegalArgumentException e) {}

        try {
            Array.set(o, 0, o);
            throw new AssertionError("IAE not thrown");
        } catch (IllegalArgumentException e) {}

    }
    private final Object[] array;
    ValueArray(Class<?> arrayClass, int len) {
        this((Object[])Array.newInstance(arrayClass.getComponentType(), len));
        assertTrue(array.getClass() == arrayClass);
    }
    ValueArray(Object[] array) {
        this.array = array;
    }

    void run(Object... elements) {
        for (int i=0; i < elements.length; i++) {
            Array.set(array, i, elements[i]);
        }

        for (int i=0; i < elements.length; i++) {
            Object o = Array.get(array, i);
            assertEquals(o, elements[i]);
        }

        Arrays.setAll(array, i -> elements[i]);
    }

    void ensureNonNullable() {
        assert(array.getClass().getComponentType().isValue());
        for (int i=0; i < array.length; i++) {
            try {
                Array.set(array, i, null);
                throw new AssertionError("NPE not thrown");
            } catch (NullPointerException e) {}
        }
    }

    static value class PointArray {
        public Point[] points;
        PointArray() {
            points = new Point[0];
        }
        public static PointArray makeArray(Point... points) {
            PointArray a = PointArray.default;
            a = __WithField(a.points, points);
            return a;
        }
    }
}
