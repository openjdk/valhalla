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

package runtime.valhalla.inlinetypes;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import static jdk.test.lib.Asserts.*;

/*
 * @test InlineTypeArray
 * @summary Plain array test for Inline Types
 * @library /test/lib
 * @compile InlineTypeArray.java Point.java Long8Inline.java Person.java
 * @run main/othervm -Xint  -XX:FlatArrayElementMaxSize=-1 runtime.valhalla.inlinetypes.InlineTypeArray
 * @run main/othervm -Xint  -XX:FlatArrayElementMaxSize=0  runtime.valhalla.inlinetypes.InlineTypeArray
 * @run main/othervm -Xcomp -XX:FlatArrayElementMaxSize=-1 runtime.valhalla.inlinetypes.InlineTypeArray
 * @run main/othervm -Xcomp -XX:FlatArrayElementMaxSize=0  runtime.valhalla.inlinetypes.InlineTypeArray
 * @run main/othervm -Xbatch -XX:+UnlockDiagnosticVMOptions -XX:ForceNonTearable=* runtime.valhalla.inlinetypes.InlineTypeArray
 */
public class InlineTypeArray {
    public static void main(String[] args) {
        InlineTypeArray inlineTypeArray = new InlineTypeArray();
        inlineTypeArray.run();
    }

    public void run() {
        testClassForName();
        testSimplePointArray();
        testLong8Array();
        testMixedPersonArray();
        testMultiDimPointArray();
        testComposition();

        testSanityCheckcasts();
        testObjectArrayOfInlines();

        testReflectArray();
        testUtilArrays();

        testInlineArrayOom();
    }

    void testClassForName() {
        String arrayClsName = "[Lruntime.valhalla.inlinetypes.Point$ref;";
        String qarrayClsName = "[Qruntime.valhalla.inlinetypes.Point;";
        try {
            // L-type..
            Class<?> arrayCls = Class.forName(arrayClsName);
            assertTrue(arrayCls.isArray(), "Expected an array class");

            arrayClsName = "[" + arrayClsName;
            Class<?> mulArrayCls = Class.forName(arrayClsName);
            assertTrue(mulArrayCls.isArray());
            assertTrue(mulArrayCls.getComponentType() == arrayCls);

            // Q-type...
            arrayCls = Class.forName(qarrayClsName);
            assertTrue(arrayCls.isArray(), "Expected an array class");

            assertTrue(arrayCls.getComponentType() == Point.class,
                       arrayCls +
                       " Expected component type of Point.class got: " + arrayCls.getComponentType());

            qarrayClsName = "[" + qarrayClsName;
            mulArrayCls = Class.forName(qarrayClsName);
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
        System.gc(); // check that VTs survive GC
        checkSimplePointArray(points);
        assertTrue(points instanceof Point[], "Instance of");

        testSimplePointArrayCopy();

        // Locked/unlocked flat array type checks
        points = createSimplePointArray();
        Point[] pointsCopy = new Point[points.length];
        synchronized (points) {
            assertTrue(points instanceof Point[], "Instance of");
            checkSimplePointArray(points);
            System.arraycopy(points, 0, pointsCopy, 0, points.length);
            synchronized (pointsCopy) {
                assertTrue(pointsCopy instanceof Point[], "Instance of");
                checkSimplePointArray(pointsCopy);
                System.gc();
            }
            System.gc();
        }
        assertTrue(pointsCopy instanceof Point[], "Instance of");
        checkSimplePointArray(pointsCopy);
    }

    void testSimplePointArrayCopy() {
        Point[] points = createSimplePointArray();
        Point[] pointsCopy = new Point[points.length];
        System.arraycopy(points, 0, pointsCopy, 0, points.length);
        checkSimplePointArray(pointsCopy);

        // Conjoint, overlap...left
        System.arraycopy(points, 0, points, 1, 2);
        checkArrayElementsEqual(points, new Point[] { pointsCopy[0], pointsCopy[0], pointsCopy[1], pointsCopy[3] });

        // Conjoint, overlap...right
        points = createSimplePointArray();
        System.arraycopy(points, 2, points, 1, 2);
        checkArrayElementsEqual(points, new Point[] { pointsCopy[0], pointsCopy[2], pointsCopy[3], pointsCopy[3] });
    }

    static Point[] createSimplePointArray() {
        Point[] ps = new Point[4];
        assertEquals(ps.length, 4, "Length");
        ps.toString();
        ps[0] = Point.createPoint(1, 2);
        ps[1] = Point.createPoint(3, 4);
        ps[2] = Point.createPoint(5, 6);
        ps[3] = Point.createPoint(7, 8);
        boolean sawOob = false;
        try {
            ps[ps.length] = Point.createPoint(0, 0);
        } catch (ArrayIndexOutOfBoundsException aioobe) { sawOob = true; }
        assertTrue(sawOob, "Didn't see AIOOBE");
        return ps;
    }

    static void checkSimplePointArray(Point[] points) {
        assertEquals(points[0].x, 1, "invalid 0 point x value");
        assertEquals(points[0].y, 2, "invalid 0 point y value");
        assertEquals(points[1].x, 3, "invalid 1 point x value");
        assertEquals(points[1].y, 4, "invalid 1 point y value");
        assertEquals(points[2].x, 5, "invalid 2 point x value");
        assertEquals(points[2].y, 6, "invalid 2 point y value");
        assertEquals(points[3].x, 7, "invalid 3 point x value");
        assertEquals(points[3].y, 8, "invalid 3 point y value");
    }

    void testLong8Array() {
        Long8Inline[] values = new Long8Inline[3];
        assertEquals(values.length, 3, "length");
        values.toString();
        Long8Inline value = values[1];
        long zl = 0;
        Long8Inline.check(value, zl, zl, zl, zl, zl, zl, zl, zl);
        values[1] = Long8Inline.create(1, 2, 3, 4, 5, 6, 7, 8);
        value = values[1];
        Long8Inline.check(value, 1, 2, 3, 4, 5, 6, 7, 8);

        Long8Inline[] copy = new Long8Inline[values.length];
        System.arraycopy(values, 0, copy, 0, values.length);
        value = copy[1];
        Long8Inline.check(value, 1, 2, 3, 4, 5, 6, 7, 8);
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

        // Now create ObjArrays of InlineArray...
        Point.ref[][] barray = (Point.ref[][]) Array.newInstance(Point.ref.class, 1, 2);
        assertEquals(barray.length, 1, "Incorrect length");
        assertEquals(barray[0].length, 2, "Incorrect length");
        barray[0][1] = Point.createPoint(1, 2);
        Point.ref pb = barray[0][1];
        int x = pb.getX();
        assertEquals(x, 1, "Bad Point Value");
    }

    static final primitive class MyInt implements Comparable<MyInt.ref> {
        final int value;

        private MyInt() { this(0); }
        private MyInt(int v) { value = v; }
        public int getValue() { return value; }
        public String toString() { return "MyInt: " + getValue(); }
        public int compareTo(MyInt.ref that) { return Integer.compare(this.getValue(), that.getValue()); }
        public boolean equals(Object o) {
            if (o instanceof MyInt) {
                return this.getValue() == ((MyInt) o).getValue();
            }
            return false;
        }

        public static MyInt create(int v) {
            return new MyInt(v);
        }

        // Null-able fields here are a temp hack to avoid ClassCircularityError
        public static final MyInt.ref MIN = MyInt.create(Integer.MIN_VALUE);
        public static final MyInt.ref ZERO = MyInt.create(0);
        public static final MyInt.ref MAX = MyInt.create(Integer.MAX_VALUE);
    }

    static MyInt staticMyInt = MyInt.create(-1);
    static MyInt[] staticMyIntArray = new MyInt[] { staticMyInt };
    static MyInt[][] staticMyIntArrayArray = new MyInt[][] { staticMyIntArray, staticMyIntArray };

    static interface SomeSecondaryType {
        default String hi() { return "Hi"; }
    }

    static final primitive class MyOtherInt implements SomeSecondaryType {
        final int value;
        private MyOtherInt() { value = 0; }
    }

    void testSanityCheckcasts() {
        MyInt[] myInts = new MyInt[1];
        assertTrue(myInts instanceof Object[]);
        assertTrue(myInts instanceof Comparable[]);
        assertTrue(myInts instanceof MyInt.ref[]);

        Class<?> cls = MyInt.class;
        assertTrue(cls.isPrimitiveClass());
        Object arrObj = Array.newInstance(cls, 1);
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
        assertTrue(matrix[0] instanceof MyOtherInt.ref[]);

        // Box types vs Inline...
        MyInt.ref[] myValueRefs = new MyInt.ref[1];
        assertTrue(myValueRefs instanceof MyInt.ref[]);
        assertTrue(myValueRefs instanceof Object[]);
        assertTrue(myValueRefs instanceof Comparable[]);
        assertFalse(myValueRefs instanceof MyInt[]);

        MyInt.ref[][] myMdValueRefs = new MyInt.ref[1][1];
        assertTrue(myMdValueRefs[0] instanceof MyInt.ref[]);
        assertTrue(myMdValueRefs[0] instanceof Object[]);
        assertTrue(myMdValueRefs[0] instanceof Comparable[]);
        assertFalse(myMdValueRefs[0] instanceof MyInt[]);

        // Did we break checkcast...
        MyInt.ref[]     va1 = (MyInt.ref[])null;
        MyInt.ref[]     va2 = null;
        MyInt.ref[][]   va3 = (MyInt.ref[][])null;
        MyInt.ref[][][] va4 = (MyInt.ref[][][])null;
        MyInt[]      va5 = null;
        MyInt[]      va6 = (MyInt[])null;
        MyInt[][]    va7 = (MyInt[][])null;
        MyInt[][][]  va8 = (MyInt[][][])null;
    }


    void testUtilArrays() {
        // Sanity check j.u.Arrays

        // cast to q-type temp effect of avoiding circularity error (decl static MyInt.ref)
        MyInt[] myInts = new MyInt[] { (MyInt) MyInt.MAX, (MyInt) MyInt.MIN };
        // Sanity sort another copy
        MyInt[] copyMyInts = (MyInt[]) Arrays.copyOf(myInts, myInts.length + 1);
        checkArrayElementsEqual(copyMyInts, new MyInt[] { myInts[0], myInts[1], (MyInt) MyInt.ZERO});

        Arrays.sort(copyMyInts);
        checkArrayElementsEqual(copyMyInts, new MyInt[] { (MyInt) MyInt.MIN, (MyInt) MyInt.ZERO, (MyInt) MyInt.MAX });

        List myIntList = Arrays.asList(copyMyInts);
        checkArrayElementsEqual(copyMyInts, myIntList.toArray(new MyInt[copyMyInts.length]));
        // This next line needs testMixedLayoutArrays to work
        checkArrayElementsEqual(copyMyInts, myIntList.toArray());

        // Sanity check j.u.ArrayList
        ArrayList<MyInt.ref> aList = new ArrayList<MyInt.ref>(Arrays.asList(copyMyInts));
        assertTrue(aList.indexOf(MyInt.MIN) == 0, "Bad Index");
        assertTrue(aList.indexOf(MyInt.ZERO) == 1, "Bad Index");
        assertTrue(aList.indexOf(MyInt.MAX) == 2, "Bad Index");

        aList.remove(2);
        aList.add(MyInt.create(5));
    }


    void testObjectArrayOfInlines() {
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

        MyInt.ref[] myIntRefArray = new MyInt.ref[1];
        assertTrue(myIntRefArray[0] == null, "Got: " + myIntRefArray[0]);
        myIntRefArray[0] = null;

        MyInt.ref[] srcNulls = new MyInt.ref[2];
        MyInt.ref[] dstNulls = new MyInt.ref[2];
        System.arraycopy(srcNulls, 0, dstNulls, 0, 2);
        checkArrayElementsEqual(srcNulls, dstNulls);
        srcNulls[1] = MyInt.create(1);
        System.arraycopy(srcNulls, 0, dstNulls, 0, 2);
        checkArrayElementsEqual(srcNulls, dstNulls);


        // Locked/unlocked flat array type checks
        synchronized (srcNulls) {
            System.arraycopy(srcNulls, 0, dstNulls, 0, 2);
            checkArrayElementsEqual(srcNulls, dstNulls);
            System.gc();
        }
        System.gc();
        checkArrayElementsEqual(srcNulls, dstNulls);
    }

    void testMixedLayoutArrays() {
        Object[] objArray = new Object[3];
        Comparable[] compArray = new Comparable[3];
        MyInt[] valArray = new MyInt[] { (MyInt) MyInt.MIN, (MyInt) MyInt.ZERO, (MyInt) MyInt.MAX };

        arrayCopy(valArray, 0, objArray, 0, 3);
        checkArrayElementsEqual(valArray, objArray);
        arrayCopy(valArray, 0, objArray, 0, 3);

        objArray = new Object[3];
        System.arraycopy(valArray, 0, objArray, 0, 3);
        checkArrayElementsEqual(valArray, objArray);

        System.arraycopy(valArray, 0, compArray, 0, 3);
        checkArrayElementsEqual(valArray, compArray);

        valArray = new MyInt[] { (MyInt) MyInt.ZERO, (MyInt) MyInt.ZERO, (MyInt) MyInt.ZERO };
        System.arraycopy(compArray, 0, valArray, 0, 3);
        checkArrayElementsEqual(valArray, compArray);

        valArray = new MyInt[] { (MyInt) MyInt.ZERO, (MyInt) MyInt.ZERO, (MyInt) MyInt.ZERO };
        System.arraycopy(objArray, 0, valArray, 0, 3);
        checkArrayElementsEqual(valArray, objArray);

        // Sanity check dst == src
        System.arraycopy(valArray, 0, valArray, 0, 3);
        checkArrayElementsEqual(valArray, objArray);

        objArray[0] = "Not an inline object";
        try {
            System.arraycopy(objArray, 0, valArray, 0, 3);
            throw new RuntimeException("Expected ArrayStoreException");
        } catch (ArrayStoreException ase) {}

        MyInt.ref[] myIntRefArray = new MyInt.ref[3];
        System.arraycopy(valArray, 0, myIntRefArray, 0, 3);
        checkArrayElementsEqual(valArray, myIntRefArray);

        myIntRefArray[0] = null;
        try {
            System.arraycopy(myIntRefArray, 0, valArray, 0, 3);
            throw new RuntimeException("Expected NullPointerException");
        } catch (NullPointerException npe) {}
    }

    static final primitive class MyPoint {
        final               MyInt x;
        final               MyInt y;

        private MyPoint() { this(0, 0); }
        private MyPoint(int x, int y) {
            this.x = new MyInt(x);
            this.y = new MyInt(y);
        }
        public boolean equals(Object that) {
            if (that instanceof MyPoint) {
                MyPoint thatPoint = (MyPoint) that;
                return x.equals(thatPoint.x) && java.util.Objects.equals(y, thatPoint.y);
            }
            return false;
        }
        static MyPoint create(int x) {
            return new MyPoint(x, x);
        }
        static MyPoint create(int x, int y) {
            return new MyPoint(x, y);
        }
        static final MyPoint.ref ORIGIN = create(0);
    }

    void testComposition() {
        // Test array operations with compostion of inline types, check element payload is correct...
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
            throw new RuntimeException("Expected NPE");
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

    void checkArrayElementsEqual(MyInt[] arr1, Object[] arr2) {
        assertTrue(arr1.length == arr2.length, "Bad length");
        for (int i = 0; i < arr1.length; i++) {
            assertTrue(java.util.Objects.equals(arr1[i], arr2[i]), "Element " + i + " not equal");
        }
    }

    void checkArrayElementsEqual(MyPoint[] arr1, Object[] arr2) {
        assertTrue(arr1.length == arr2.length, "Bad length");
        for (int i = 0; i < arr1.length; i++) {
            assertTrue(java.util.Objects.equals(arr1[i], arr2[i]), "Element " + i + " not equal");
        }
    }

    void checkArrayElementsEqual(Object[] arr1, Object[] arr2) {
        assertTrue(arr1.length == arr2.length, "Bad length");
        for (int i = 0; i < arr1.length; i++) {
            assertTrue(java.util.Objects.equals(arr1[i], arr2[i]), "Element " + i + " not equal");
        }
    }

    void arrayCopy(MyInt[] src, int srcPos, Object[] dst, int dstPos, int length) {
        for (int i = 0; i < length ; i++) {
            dst[dstPos++] = src[srcPos++];
        }
    }
    void arrayCopy(MyPoint[] src, int srcPos, Object[] dst, int dstPos, int length) {
        for (int i = 0; i < length ; i++) {
            dst[dstPos++] = src[srcPos++];
        }
    }

    Object getNull() { return null; }


    void testInlineArrayOom() {
        int size = Integer.MAX_VALUE;
        try {
            MyPoint[] pts = new MyPoint[size];
            throw new RuntimeException("Excepted OOM");
        } catch (OutOfMemoryError oom) {}
    }

}
