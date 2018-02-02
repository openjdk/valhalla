/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

import static jdk.test.lib.Asserts.*;

/*
 * @test ValueTypeArray
 * @summary Plain array test for Value Types
 * @library /test/lib
 * @compile -XDenableValueTypes ValueTypeArray.java Point.java Long8Value.java Person.java
 * @run main/othervm -Xint -XX:+ValueArrayFlatten -XX:+EnableValhalla runtime.valhalla.valuetypes.ValueTypeArray
 * @run main/othervm -Xint -XX:-ValueArrayFlatten -XX:+EnableValhalla runtime.valhalla.valuetypes.ValueTypeArray
 * @run main/othervm -Xcomp -XX:+ValueArrayFlatten -XX:+EnableValhalla runtime.valhalla.valuetypes.ValueTypeArray
 * @run main/othervm -Xcomp -XX:-ValueArrayFlatten -XX:+EnableValhalla runtime.valhalla.valuetypes.ValueTypeArray
 */
public class ValueTypeArray {
    public static void main(String[] args) {
        ValueTypeArray valueTypeArray = new ValueTypeArray();
        valueTypeArray.run();
    }

    public void run() {
        // Class.forName does not support loading of DVT
        // - should call ValueType::arrayTypeClass instead?
        // testClassForName();
        testSimplePointArray();
        testLong8Array();
        // embedded oops not yet supported
        //testMixedPersonArray();
        testMultiDimPointArray();
        // Some design issues, ignore for now
        //testAtomicArray();
        //testArrayCopy();
        testReflectArray();
    }

    void testClassForName() {
        String arrayClsName = "[Qruntime.valhalla.valuetypes.Point;";
        try {
            Class<?> arrayCls = Class.forName(arrayClsName);
            assertTrue(arrayCls.isArray(), "Expected an array class");
            assertTrue(arrayCls.getComponentType() == Point.class,
                       "Expected component type of Point.class");

            arrayClsName = "[" + arrayClsName;
            Class<?> mulArrayCls = Class.forName(arrayClsName);
            assertTrue(mulArrayCls.isArray());
            assertTrue(mulArrayCls.getComponentType() == arrayCls);
        }
        catch (ClassNotFoundException cnfe) {
            fail("Class.forName(" + arrayClsName + ") failed", cnfe);
        }
    }

    void testSimplePointArray() {
        Point[] points = createSimplePointArray();
        checkSimplePointArray(points);
        System.gc(); // check that VTs survive GC

        assertTrue(points instanceof Point[], "Instance of");

        Point[] pointsCopy = new Point[points.length];
        System.arraycopy(points, 0, pointsCopy, 0, points.length);
        checkSimplePointArray(pointsCopy);
    }

    static Point[] createSimplePointArray() {
        Point[] ps = new Point[2];
        assertEquals(ps.length, 2, "Length");
        System.out.println(ps);
        ps[0] = Point.createPoint(1, 2);
        ps[1] = Point.createPoint(3, 4);
        System.gc(); // check that VTs survive GC
        return ps;
    }

    static void checkSimplePointArray(Point[] points) {
        assertEquals(points[0].x, 1, "invalid 0 point x value");
        assertEquals(points[0].y, 2, "invalid 0 point y value");
        assertEquals(points[1].x, 3, "invalid 1 point x value");
        assertEquals(points[1].y, 4, "invalid 1 point y value");
    }

    void testLong8Array() {
        Long8Value[] values = new Long8Value[3];
        assertEquals(values.length, 3, "length");
        System.out.println(values);
        Long8Value value = values[1];
        long zl = 0;
        Long8Value.check(value, zl, zl, zl, zl, zl, zl, zl, zl);
        values[1] = Long8Value.create(1, 2, 3, 4, 5, 6, 7, 8);
        value = values[1];
        Long8Value.check(value, 1, 2, 3, 4, 5, 6, 7, 8);

        Long8Value[] copy = new Long8Value[values.length];
        System.arraycopy(values, 0, copy, 0, values.length);
        value = copy[1];
        Long8Value.check(value, 1, 2, 3, 4, 5, 6, 7, 8);
    }

    void testMixedPersonArray() {
        Person[] people = new Person[3];

        people[0] = Person.create(1, "First", "Last");
        assertEquals(people[0].getId(), 1L, "Invalid Id");
        assertEquals(people[0].getFirstName(), "First", "Invalid First Name");
        assertEquals(people[0].getLastName(), "Last", "Invalid Last Name");

        people[1] = Person.create(2, "Jane", "Wayne");
        people[2] = Person.create(3, "Bob", "Dobalina");

        Person[] peopleCopy = new Person[people.length];
        System.arraycopy(people, 0, peopleCopy, 0, people.length);
        assertEquals(peopleCopy[2].getId(), 3L, "Invalid Id");
        assertEquals(peopleCopy[2].getFirstName(), "Bob", "Invalid First Name");
        assertEquals(peopleCopy[2].getLastName(), "Dobalina", "Invalid Last Name");
    }

    void testMultiDimPointArray() {
        Point[][][] multiPoints = new Point[2][3][4];
        assertEquals(multiPoints.length, 2, "1st dim length");
        assertEquals(multiPoints[0].length, 3, "2st dim length");
        assertEquals(multiPoints[0][0].length, 4, "3rd dim length");

        Point defaultPoint = multiPoints[1][2][3];
        assertEquals(defaultPoint.x, 0, "invalid point x value");
        assertEquals(defaultPoint.y, 0, "invalid point x value");
    }


    @SuppressWarnings("unchecked")
    void testArrayCopy() {
        // Test copy atomic vs relax combos...
        int testLength = 3;
        Long8Value long8Value = Long8Value.create(1, 2, 3, 4, 5, 6, 7, 8);
        Long8Value long8ValueZero = Long8Value.create(0, 0, 0, 0, 0, 0, 0, 0);
        Long8Value[] relaxedValues = new Long8Value[testLength];
        for (int i = 0; i < testLength; i++) {
            relaxedValues[i] = long8Value;
        }

        // relaxed -> relaxed
        Long8Value[] relaxedValues2 = new Long8Value[testLength];
        System.arraycopy(relaxedValues, 0, relaxedValues2, 0, testLength);
        Long8Value.check(relaxedValues2[testLength-1], 1, 2, 3, 4, 5, 6, 7, 8);
    }

    void testReflectArray() {
        // Check the java.lang.reflect.Array.newInstance methods...
        Class<?> cls = (Class<?>) Point[].class;
        Point[][] array = (Point[][]) java.lang.reflect.Array.newInstance(cls, 1);
        assertEquals(array.length, 1, "Incorrect length");
        assertTrue(array[0] == null, "Expected NULL");

        Point[][][] array3 = (Point[][][]) java.lang.reflect.Array.newInstance(cls, 1, 2);
        assertEquals(array3.length, 1, "Incorrect length");
        assertEquals(array3[0].length, 2, "Incorrect length");
        assertTrue(array3[0][0] == null, "Expected NULL");

        // Now create ObjArrays of ValueArray...
        cls = (Class<?>) Point.class;
        array = (Point[][]) java.lang.reflect.Array.newInstance(cls, 1, 2);
        assertEquals(array.length, 1, "Incorrect length");
        assertEquals(array[0].length, 2, "Incorrect length");
        Point p = array[0][1];
        int x = p.x;
        assertEquals(x, 0, "Bad Point Value");
    }

}
