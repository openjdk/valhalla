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
    private final Class<?> arrayClass;
    private final Class<?> componentType;
    private final Object[] array;
    ValueArray(Class<?> arrayClass, Object[] array) {
        this.arrayClass = arrayClass;
        this.array = array;
        this.componentType = arrayClass.getComponentType();
        assertTrue(arrayClass.isArray());
        assertTrue(array.getClass() == arrayClass);
    }

    private static Class<?> nullablePointArrayClass() {
        Object a = new Point?[0];
        return a.getClass();
    }

    void run() {
        testClassName();
        testArrayElements();

        if (componentType.isInlineClass()) {
            Object[] qArray = (Object[]) Array.newInstance(componentType.asPrimaryType(), 0);
            Object[] lArray = (Object[]) Array.newInstance(componentType.asIndirectType(), 0);
            testInlineArrayCovariance(componentType, qArray, lArray);
        }
    }

    void testClassName() {
        // test class names
        String arrayClassName = arrayClass.getName();
        StringBuilder sb = new StringBuilder();
        Class<?> c = arrayClass;
        while (c.isArray()) {
            sb.append("[");
            c = c.getComponentType();
        }
        sb.append(c.isIndirectType() ? "L" : "Q").append(c.getName()).append(";");
        assertEquals(sb.toString(), arrayClassName);
        assertEquals(c.getTypeName(), c.getName() + (c.isInlineClass() && c.isIndirectType() ? "?" : ""));
    }

    void testArrayElements() {
        Object[] array = (Object[]) Array.newInstance(componentType, this.array.length);
        assertTrue(array.getClass() == arrayClass);
        assertTrue(array.getClass().getComponentType() == componentType);

        // set elements
        for (int i=0; i < this.array.length; i++) {
            Array.set(array, i, this.array[i]);
        }
        for (int i=0; i < this.array.length; i++) {
            Object o = Array.get(array, i);
            assertEquals(o, this.array[i]);
        }
        Arrays.setAll(array, i -> this.array[i]);

        // test nullable
        if (componentType.isNullableType()) {
            for (int i=0; i < array.length; i++) {
                Array.set(array, i, null);
            }
        } else {
            for (int i=0; i < array.length; i++) {
                try {
                    Array.set(array, i, null);
                    assertFalse(true, "expect NPE but not thrown");
                } catch (NullPointerException e) { }
            }
        }
    }

    void testInlineArrayCovariance(Class<?> componentType, Object[] qArray, Object[] lArray) {
        assertTrue(componentType.isInlineClass());

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
            assertFalse(true, "cast of Point? to Point should not succeed");
        } catch (ClassCastException cce) { }
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
                                          null }},
            new Object[] { NonFlattenValue[].class,
                           new NonFlattenValue[] { NonFlattenValue.make(1, 2),
                                                   NonFlattenValue.make(10, 20),
                                                   NonFlattenValue.make(100, 200)}},
        };
    }

    @Test(dataProvider="arrayTypes")
    public static void test(Class<?> arrayClass, Object[] array) {
        ValueArray test = new ValueArray(arrayClass, array);
        test.run();
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

    @Test
    static void testPointArray() {
        Point[] qArray = new Point[0];
        Point?[] lArray = new Point?[0];

        ValueArray test = new ValueArray(Point[].class, qArray);
        test.run();

        ValueArray test1 = new ValueArray(Point?[].class, lArray);
        test.run();

        // language instanceof
        assertTrue(qArray instanceof Point[]);
        assertTrue(lArray instanceof Point?[]);
    }
}
