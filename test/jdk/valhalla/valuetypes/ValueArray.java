/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Basic test for Array::get, Array::set, Arrays::setAll on inline class array
 * @compile -XDallowWithFieldOperator Point.java NonFlattenValue.java
 * @compile -XDallowWithFieldOperator ValueArray.java
 * @run testng/othervm -XX:+EnableValhalla -XX:ValueArrayElemMaxFlatSize=-1 ValueArray
 * @run testng/othervm -XX:+EnableValhalla -XX:ValueArrayElemMaxFlatSize=0  ValueArray
 */

import java.lang.reflect.Array;
import java.util.Arrays;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ValueArray {
    private static Class<?> nullablePointArrayClass() {
        Object a = new Point?[0];
        return a.getClass();
    }

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
            new Object[] { nullablePointArrayClass(),
                           new Point?[] { Point.makePoint(11, 22),
                                          Point.makePoint(110, 220),
                                          null}},
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
        Class<?> compType = c.getComponentType();
        if (compType.isValue()) {
            test.testSetNullElement(compType == compType.asBoxType());
        }
     }

    @Test
    public static void testPointArray() {
        PointArray array = PointArray.makeArray(Point.makePoint(1, 2), Point.makePoint(10, 20));
        ValueArray test = new ValueArray(array.points);
        test.run(Point.makePoint(3, 4), Point.makePoint(30, 40));
    }

    @Test
    public static void testNullablePointArray() {
        Point ?[]array = new Point ?[3];
        array[0] = Point.makePoint(1, 2);
        array[1] = null;
        array[2] = Point.makePoint(3, 4);

        ValueArray test = new ValueArray(array);
        test.run(null, Point.makePoint(3, 4), null);
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

    @Test()
    static void testArrayCovariance() {
        Point[] qArray = new Point[0];
        Point?[] lArray = new Point?[0];

        // language instanceof
        assertTrue(qArray instanceof Point[]);
        assertTrue(lArray instanceof Point?[]);

        // Class.instanceOf (self)
        assertTrue(qArray.getClass().isInstance(qArray));
        assertTrue(lArray.getClass().isInstance(lArray));

        // Class.instanceof inline vs indirect
        assertFalse(qArray.getClass().isInstance(lArray));
        assertTrue(lArray.getClass().isInstance(qArray));

        // Class.isAssignableFrom (self)
        assertTrue(qArray.getClass().isAssignableFrom(qArray.getClass()));
        assertTrue(lArray.getClass().isAssignableFrom(lArray.getClass()));

        // Class.isAssignableFrom inline vs indirect
        assertTrue(lArray.getClass().isAssignableFrom(qArray.getClass()));
        assertFalse(qArray.getClass().isAssignableFrom(lArray.getClass()));

        // Class.cast (self)
        qArray.getClass().cast(qArray);
        lArray.getClass().cast(lArray);

        // Class.cast inline vs indirect
        lArray.getClass().cast(qArray);
        try {
            qArray.getClass().cast(lArray);
            fail("cast of Point? to Point should not succeed");
        } catch (ClassCastException cce) {
            // expected
        }
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

    void testSetNullElement(boolean nullable) {
        assert(array.getClass().getComponentType().isValue());
        for (int i=0; i < array.length; i++) {
            try {
                Array.set(array, i, null);
                if (!nullable)
                    throw new AssertionError("NPE not thrown");
            } catch (NullPointerException e) {
                assertFalse(nullable);
            }
        }
    }

  static inline class PointArray {
        public Point[] points;
        PointArray() {
            points = new Point[0];
        }
        public static PointArray makeArray(Point... points) {
            PointArray a = PointArray.default;
            Point[] array = new Point[points.length];
            for (int i=0; i < points.length; i++) {
                array[i] = points[i];
            }
            a = __WithField(a.points, array);
            return a;
        }
    }
}
