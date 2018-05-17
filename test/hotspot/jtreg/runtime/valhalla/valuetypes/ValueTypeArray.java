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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

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
        testClassForName();
        testSimplePointArray();
        testLong8Array();
        testMixedPersonArray();
        testMultiDimPointArray();
        testComposition();

        testSanityCheckcasts();
        testObjectArrayOfValues();

        testReflectArray();
        testUtilArrays();
    }

    void testClassForName() {
        String arrayClsName = "[Lruntime.valhalla.valuetypes.Point;";
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
        Point[] defaultPoint = new Point[1];
        Point p = defaultPoint[0];
        assertEquals(p.x, 0, "invalid default loaded from array");
        assertEquals(p.y, 0, "invalid default loaded from array");
        boolean gotNpe = false;
        try {
            defaultPoint[0] = (Point) getNull();
        } catch (NullPointerException npe) {
            gotNpe = true;
        }
        assertTrue(gotNpe, "Expected NullPointerException");

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
        ps.toString();
        ps[0] = Point.createPoint(1, 2);
        ps[1] = Point.createPoint(3, 4);
        boolean sawOob = false;
        try {
            ps[2] = Point.createPoint(0, 0);
        } catch (ArrayIndexOutOfBoundsException aioobe) { sawOob = true; }
        assertTrue(sawOob, "Didn't see AIOOBE");
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
        values.toString();
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
        assertEquals(people[0].getId(), 1, "Invalid Id person");
        assertEquals(people[0].getFirstName(), "First", "Invalid First Name");
        assertEquals(people[0].getLastName(), "Last", "Invalid Last Name");

        people[1] = Person.create(2, "Jane", "Wayne");
        people[2] = Person.create(3, "Bob", "Dobalina");

        Person[] peopleCopy = new Person[people.length];
        System.arraycopy(people, 0, peopleCopy, 0, people.length);
        assertEquals(peopleCopy[2].getId(), 3, "Invalid Id");
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

    void testReflectArray() {
        // Check the java.lang.reflect.Array.newInstance methods...
        Class<?> cls = (Class<?>) Point[].class;
        Point[][] array = (Point[][]) Array.newInstance(cls, 1);
        assertEquals(array.length, 1, "Incorrect length");
        assertTrue(array[0] == null, "Expected NULL");

        Point[][][] array3 = (Point[][][]) Array.newInstance(cls, 1, 2);
        assertEquals(array3.length, 1, "Incorrect length");
        assertEquals(array3[0].length, 2, "Incorrect length");
        assertTrue(array3[0][0] == null, "Expected NULL");

        // Now create ObjArrays of ValueArray...
        cls = (Class<?>) Point.class;
        array = (Point[][]) Array.newInstance(cls, 1, 2);
        assertEquals(array.length, 1, "Incorrect length");
        assertEquals(array[0].length, 2, "Incorrect length");
        Point p = array[0][1];
        int x = p.x;
        assertEquals(x, 0, "Bad Point Value");
    }

    static final __ByValue class MyInt implements Comparable<MyInt> {
        final int value;

        private MyInt() { value = 0; }
        public int getValue() { return value; }
        public String toString() { return "MyInt: " + getValue(); }
        public int compareTo(MyInt that) { return Integer.compare(this.getValue(), that.getValue()); }
        public boolean equals(Object o) {
            if (o instanceof MyInt) {
                return this.getValue() == ((MyInt) o).getValue();
            }
            return false;
        }

        public static MyInt create(int v) {
            MyInt mi = __MakeDefault MyInt();
            mi = __WithField(mi.value, v);
            return mi;
        }

        public static __NotFlattened final MyInt MIN = MyInt.create(Integer.MIN_VALUE);
        public static __NotFlattened final MyInt ZERO = MyInt.create(0);
        public static __NotFlattened final MyInt MAX = MyInt.create(Integer.MAX_VALUE);
    }

    static interface SomeSecondaryType {
        default String hi() { return "Hi"; }
    }

    static final __ByValue class MyOtherInt implements SomeSecondaryType {
        final int value;
        private MyOtherInt() { value = 0; }
    }

    void testSanityCheckcasts() {
        MyInt[] myInts = new MyInt[1];
        assertTrue(myInts instanceof Object[]);
        assertTrue(myInts instanceof Comparable[]);

        Object arrObj = Array.newInstance(MyInt.class, 1);
        assertTrue(arrObj instanceof Object[], "Not Object array");
        assertTrue(arrObj instanceof Comparable[], "Not Comparable array");
        assertTrue(arrObj instanceof MyInt[], "Not MyInt array");

        Object[] arr = (Object[]) arrObj;
        assertTrue(arr instanceof Comparable[], "Not Comparable array");
        assertTrue(arr instanceof MyInt[], "Not MyInt array");
        Comparable[] comparables = (Comparable[])arr;
        MyInt[] myIntArr = (MyInt[]) arr;

        // multi-dim, check secondary array types are setup...
        MyOtherInt[][] matrix = new MyOtherInt[1][1];
        assertTrue(matrix[0] instanceof MyOtherInt[]);
        assertTrue(matrix[0] instanceof SomeSecondaryType[]);
    }

    void testUtilArrays() {
        // Sanity check j.u.Arrays
        MyInt[] myInts = new MyInt[] { MyInt.MAX, MyInt.MIN };
        // Sanity sort another copy
        MyInt[] copyMyInts = Arrays.copyOf(myInts, myInts.length + 1);
        checkArrayElementsEqual(copyMyInts, new MyInt[] { myInts[0], myInts[1], MyInt.ZERO});

        Arrays.sort(copyMyInts);
        checkArrayElementsEqual(copyMyInts, new MyInt[] { MyInt.MIN, MyInt.ZERO, MyInt.MAX });

        List myIntList = Arrays.asList(copyMyInts);
        checkArrayElementsEqual(copyMyInts, myIntList.toArray(new MyInt[copyMyInts.length]));
        // This next line needs testMixedLayoutArrays to work
        checkArrayElementsEqual(copyMyInts, myIntList.toArray());

        // Sanity check j.u.ArrayList
        ArrayList<MyInt> aList = new ArrayList<MyInt>(Arrays.asList(copyMyInts));
        assertTrue(aList.indexOf(MyInt.MIN) == 0, "Bad Index");
        assertTrue(aList.indexOf(MyInt.ZERO) == 1, "Bad Index");
        assertTrue(aList.indexOf(MyInt.MAX) == 2, "Bad Index");

        aList.remove(2);
        aList.add(MyInt.create(5));

        // Interesting:
        //aList.add((MyInt)getNull());

        // javac currently generating "java/util/Objects.requireNonNull
        // should checkcast treat null against Value class as CCE ?
        // Then in the end, ArrayList.elementData is Object[], (that's why remove works)
        // why can't I write add(null) then ?
    }

    void testObjectArrayOfValues() {
        testSanityObjectArrays();
        testMixedLayoutArrays();
    }

    void testSanityObjectArrays() {
        Object[] objects = new Object[2];
        assertTrue(objects[0] == null && objects[1] == null, "Not null ?");

        objects[0] = MyInt.create(1);
        objects[1] = Integer.valueOf(2);
        assertTrue(objects[0].equals(MyInt.create(1)), "Bad Value");
        assertTrue(objects[1].equals(Integer.valueOf(2)), "Bad Object");

        Comparable[] copyComparables = new Comparable[objects.length];
        System.arraycopy(objects, 0, copyComparables, 0, objects.length);
        checkArrayElementsEqual(objects, copyComparables);

        objects[0] = null;
        objects[1] = null;
        assertTrue(objects[0] == null && objects[1] == null, "Not null ?");

        Comparable[] comparables = new Comparable[2];
        assertTrue(comparables[0] == null && comparables[1] == null, "Not null ?");
        comparables[0] = MyInt.create(3);
        comparables[1] = Integer.valueOf(4);
        assertTrue(comparables[0].equals(MyInt.create(3)), "Bad Value");
        assertTrue(comparables[1].equals(Integer.valueOf(4)), "Bad Object");

        Object[] copyObjects = new Object[2];
        System.arraycopy(comparables, 0, copyObjects, 0, comparables.length);
        checkArrayElementsEqual(comparables, copyObjects);

        comparables[0] = null;
        comparables[1] = null;
        assertTrue(comparables[0] == null && comparables[1] == null, "Not null ?");
    }

    void testMixedLayoutArrays() {
        Object[] objArray = new Object[3];
        Comparable[] compArray = new Comparable[3];
        MyInt[] valArray = new MyInt[] { MyInt.MIN, MyInt.ZERO, MyInt.MAX };

        arrayCopy(valArray, 0, objArray, 0, 3);
        checkArrayElementsEqual(valArray, objArray);
        arrayCopy(valArray, 0, objArray, 0, 3);

        objArray = new Object[3];
        System.arraycopy(valArray, 0, objArray, 0, 3);
        checkArrayElementsEqual(valArray, objArray);

        System.arraycopy(valArray, 0, compArray, 0, 3);
        checkArrayElementsEqual(valArray, compArray);

        valArray = new MyInt[] { MyInt.ZERO, MyInt.ZERO, MyInt.ZERO };
        System.arraycopy(compArray, 0, valArray, 0, 3);
        checkArrayElementsEqual(valArray, compArray);

        valArray = new MyInt[] { MyInt.ZERO, MyInt.ZERO, MyInt.ZERO };
        System.arraycopy(objArray, 0, valArray, 0, 3);
        checkArrayElementsEqual(valArray, objArray);

        // Sanity check dst == src
        System.arraycopy(valArray, 0, valArray, 0, 3);
        checkArrayElementsEqual(valArray, objArray);

        objArray[0] = "Not a value object";
        try {
            System.arraycopy(objArray, 0, valArray, 0, 3);
            throw new RuntimeException("Expected ArrayStoreException");
        } catch (ArrayStoreException ase) {}
    }

    static final __ByValue class MyPoint {
        final __Flattenable MyInt x;
        final               MyInt y;

        private MyPoint() {
            x = MyInt.ZERO;
            y = x;
        }
        public boolean equals(Object that) {
            if (that instanceof MyPoint) {
                MyPoint thatPoint = (MyPoint) that;
                return x.equals(thatPoint.x) && java.util.Objects.equals(y, thatPoint.y);
            }
            return false;
        }
        static MyPoint create(int x) {
            MyPoint mp = __MakeDefault MyPoint();
            mp = __WithField(mp.x, MyInt.create(x));
            return mp;
        }
        static MyPoint create(int x, int y) {
            MyPoint mp = __MakeDefault MyPoint();
            mp = __WithField(mp.x, MyInt.create(x));
            mp = __WithField(mp.y, MyInt.create(y));
            return mp;
        }
        static __NotFlattened final MyPoint ORIGIN = create(0);
    }

    void testComposition() {
        // Test array operations with compostion of values, check element payload is correct...
        MyPoint a = MyPoint.create(1, 2);
        MyPoint b = MyPoint.create(7, 21);
        MyPoint c = MyPoint.create(Integer.MAX_VALUE, Integer.MIN_VALUE);

        MyPoint[] pts = new MyPoint[3];
        if (!pts[0].equals(MyPoint.ORIGIN)) {
            throw new RuntimeException("Equals failed: " + pts[0] + " vs " + MyPoint.ORIGIN);
        }
        pts = new MyPoint[] { a, b, c };
        checkArrayElementsEqual(pts, new Object[] { a, b, c});
        Object[] oarr = new Object[3];

        arrayCopy(pts, 0, oarr, 0, 3);
        checkArrayElementsEqual(pts, oarr);

        oarr = new Object[3];
        System.arraycopy(pts, 0, oarr, 0, 3);
        checkArrayElementsEqual(pts, oarr);

        System.arraycopy(oarr, 0, pts, 0, 3);
        checkArrayElementsEqual(pts, oarr);

        oarr = new Object[3];
        try {
            System.arraycopy(oarr, 0, pts, 0, 3);
            // TODO fix this
            //throw new RuntimeException("Expected NPE");
        }
        catch (NullPointerException npe) {}

        oarr = new Object[3];
        oarr[0] = new Object();
        try {
            System.arraycopy(oarr, 0, pts, 0, 3);
            throw new RuntimeException("Expected ASE");
        }
        catch (ArrayStoreException ase) {}
    }

    void checkArrayElementsEqual(Object[] arr1, Object[] arr2) {
        assertTrue(arr1.length == arr2.length, "Bad length");
        for (int i = 0; i < arr1.length; i++) {
            assertTrue(java.util.Objects.equals(arr1[i], arr2[i]), "Element " + i + " not equal");
        }
    }

    void arrayCopy(Object[] src, int srcPos, Object[] dst, int dstPos, int length) {
        for (int i = 0; i < length ; i++) {
            dst[dstPos++] = src[srcPos++];
        }
    }

    Object getNull() { return null; }

}
