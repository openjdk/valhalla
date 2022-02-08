/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Basic test for Array::get, Array::set, Arrays::setAll on primitive class array
 * @run testng/othervm -XX:FlatArrayElementMaxSize=-1 ValueArray
 * @run testng/othervm -XX:FlatArrayElementMaxSize=0  ValueArray
 */

import java.lang.reflect.Array;
import java.util.Arrays;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ValueArray {
    @DataProvider(name="elementTypes")
    static Object[][] elementTypes() {
        return new Object[][]{
            new Object[] { Point.class.asValueType(), Point.default },
            new Object[] { Point.ref.class, null },
            new Object[] { ValueOptional.class, null },
        };
    }

    /*
     * Test an array created from the given element type via Array::newInstance
     */
    @Test(dataProvider="elementTypes")
    public void testElementType(Class<?> elementType, Object defaultValue) {
        assertTrue(elementType.isValue());
        assertTrue(elementType.isPrimaryType() || defaultValue != null);

        Object[] array = (Object[])Array.newInstance(elementType, 1);
        Class<?> arrayType = array.getClass();
        assertTrue(arrayType.componentType() == elementType);
        // Array is a reference type
        assertTrue(arrayType.isArray());
        assertTrue(arrayType.isPrimaryType());
        assertEquals(arrayType.asPrimaryType(), arrayType);
        assertTrue(array[0] == defaultValue);

        // check the element type of multi-dimensional array
        Object[][] multiArray = (Object[][])Array.newInstance(elementType, 1, 2, 3);
        Class<?> c = multiArray.getClass();
        while (c.getComponentType() != null) {
            c = c.getComponentType();
        }
        assertTrue(c == elementType);
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
            new Object[] { Point.ref[].class,
                           new Point.ref[] { Point.makePoint(11, 22),
                                             Point.makePoint(110, 220),
                                             null }},
            new Object[] { NonFlattenValue[].class,
                           new NonFlattenValue[] { NonFlattenValue.make(1, 2),
                                                   NonFlattenValue.make(10, 20),
                                                   NonFlattenValue.make(100, 200)}},
            new Object[] { Point[].class,  new Point[0] },
            new Object[] { Point.ref[].class,  new Point.ref[0] },
            new Object[] { ValueOptional[].class,  new ValueOptional[0] },
        };
    }

    /*
     * Test the following properties of an array of value class:
     * - class name
     * - array element can be null or not
     * - array covariance if the element type is a primitive value type
     */
    @Test(dataProvider="arrayTypes")
    public void testArrays(Class<?> arrayClass, Object[] array) {
        testClassName(arrayClass);
        testArrayElements(arrayClass, array);
        Class<?> componentType = arrayClass.componentType();
        if (componentType.isPrimitiveClass()) {
            Object[] qArray = (Object[]) Array.newInstance(componentType.asValueType(), 0);
            Object[] lArray = (Object[]) Array.newInstance(componentType.asPrimaryType(), 0);
            testArrayCovariance(componentType, qArray, lArray);
        }
    }

    /**
     * Verify the array class's name of the form "[QPoint;" or "[LPoint;"
     */
    static void testClassName(Class<?> arrayClass) {
        // test class names
        String arrayClassName = arrayClass.getName();
        StringBuilder sb = new StringBuilder();
        Class<?> c = arrayClass;
        while (c.isArray()) {
            sb.append("[");
            c = c.getComponentType();
        }
        sb.append(c.isPrimitiveValueType() ? "Q" : "L").append(c.getName()).append(";");
        assertEquals(sb.toString(), arrayClassName);
    }

    /**
     * Setting the elements of an array.
     * NPE will be thrown if null is set on an element in an array of primitive value type
     */
    static void testArrayElements(Class<?> arrayClass, Object[] array) {
        Class<?> componentType = arrayClass.getComponentType();
        assertTrue(arrayClass.isArray());
        assertTrue(array.getClass() == arrayClass);
        Object[] newArray = (Object[]) Array.newInstance(componentType, array.length);
        assertTrue(newArray.getClass() == arrayClass);
        assertTrue(newArray.getClass().getComponentType() == componentType);

        // set elements
        for (int i = 0; i < array.length; i++) {
            Array.set(newArray, i, array[i]);
        }
        for (int i = 0; i < array.length; i++) {
            Object o = Array.get(newArray, i);
            assertEquals(o, array[i]);
        }
        Arrays.setAll(newArray, i -> array[i]);

        // test nullable
        if (!componentType.isPrimitiveValueType()) {
            for (int i = 0; i < newArray.length; i++) {
                Array.set(newArray, i, null);
            }
        } else {
            for (int i = 0; i < newArray.length; i++) {
                try {
                    Array.set(newArray, i, null);
                    fail("expect NPE but not thrown");
                } catch (NullPointerException e) {
                }
            }
        }
    }

    /**
     * Point[] is a subtype of Point.ref[], which is a subtype of Object[].
     */
    static void testArrayCovariance(Class<?> componentType, Object[] qArray, Object[] lArray) {
        assertTrue(componentType.isPrimitiveClass());

        // Class.instanceOf (self)
        assertTrue(qArray.getClass().isInstance(qArray));
        assertTrue(lArray.getClass().isInstance(lArray));

        // Class.isAssignableFrom (self)
        assertTrue(qArray.getClass().isAssignableFrom(qArray.getClass()));
        assertTrue(lArray.getClass().isAssignableFrom(lArray.getClass()));

        // V.val[] is a subtype of V.ref[]
        assertFalse(qArray.getClass().isInstance(lArray));
        assertTrue(lArray.getClass().isInstance(qArray));

        // V.val[] is a subtype of V.ref[]
        assertTrue(lArray.getClass().isAssignableFrom(qArray.getClass()));
        assertFalse(qArray.getClass().isAssignableFrom(lArray.getClass()));

        // Class.cast (self)
        qArray.getClass().cast(qArray);
        lArray.getClass().cast(lArray);

        // Class.cast
        lArray.getClass().cast(qArray);
        try {
            qArray.getClass().cast(lArray);
            fail("cast of Point.ref[] to Point[] should not succeed");
        } catch (ClassCastException cce) {
        }
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
            fail("IAE not thrown");
        } catch (IllegalArgumentException e) {}

        try {
            Array.set(o, 0, o);
            fail("IAE not thrown");
        } catch (IllegalArgumentException e) {}

    }

    @Test
    static void testInstanceOf() {
        Point[] qArray = new Point[0];
        Point.ref[] lArray = new Point.ref[0];
        ValueOptional[] vArray = new ValueOptional[0];

        // language instanceof
        assertTrue(qArray instanceof Point[]);
        assertTrue(lArray instanceof Point.ref[]);
        assertTrue(vArray instanceof ValueOptional[]);
    }
}
