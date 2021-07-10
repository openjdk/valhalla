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
 * @summary test VarHandle on primitive class array
 * @run testng/othervm -XX:FlatArrayElementMaxSize=-1 ArrayElementVarHandleTest
 * @run testng/othervm -XX:FlatArrayElementMaxSize=0  ArrayElementVarHandleTest
 */

import java.lang.invoke.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ArrayElementVarHandleTest {
    private static final Point P = Point.makePoint(10, 20);
    private static final Line L = Line.makeLine(10, 20, 30, 40);
    private static final MutablePath PATH = MutablePath.makePath(10, 20, 30, 40);

    private static final Point[] POINTS = new Point[]{
            Point.makePoint(1, 2),
            Point.makePoint(10, 20),
            Point.makePoint(100, 200)
    };

    private static final Point.ref[] NULL_POINTS = new Point.ref[]{
            Point.makePoint(11, 22),
            Point.makePoint(110, 220),
            null
    };

    private static final Line[] LINES = new Line[]{
            Line.makeLine(1, 2, 3, 4),
            Line.makeLine(10, 20, 30, 40),
            Line.makeLine(15, 25, 35, 45),
            Line.makeLine(20, 30, 40, 50)
    };

    private static final Line.ref[] NULL_LINES = new Line.ref[] { null, null };

    private static final NonFlattenValue[] NFV_ARRAY = new NonFlattenValue[]{
            NonFlattenValue.make(1, 2),
            NonFlattenValue.make(10, 20),
            NonFlattenValue.make(100, 200)
    };

    /*
     * VarHandle of Object[].class
     */
    @Test
    public void testObjectArrayVarHandle() throws Throwable {
        // Point[] <: Point.ref[] <: Object
        Object[] array1 = newArray(Object[].class, POINTS.length);
        setElements(array1, POINTS);
        setElements(array1, NULL_POINTS);
        setElements(array1, new Object[] { "abc", Point.makePoint(1, 2) });

        Point.ref[] array2 = new Point.ref[NULL_POINTS.length];
        setElements(array2, POINTS);
        setElements(array2, NULL_POINTS);

        Point[] array3 = new Point[POINTS.length];
        setElements(array3, POINTS);
    }

    /*
     * VarHandle of Point.ref[].class
     */
    @Test
    public void testPointRefVarHandle() throws Throwable {
        // Point[] <: Point.ref[] <: Object
        Point.ref[] array1 = (Point.ref[])newArray(Point.ref[].class, POINTS.length);
        assertTrue(array1.getClass().componentType() == Point.ref.class);

        setElements(array1, POINTS);
        setElements(array1, NULL_POINTS);

        Point.ref[] array2 = new Point.ref[NULL_POINTS.length];
        setElements(array2, POINTS);
        setElements(array2, NULL_POINTS);

        Point[] array3 = new Point[POINTS.length];
        setElements(array3, POINTS);
    }

    /*
     * VarHandle of Point[].class
     */
    @Test
    public void testPointArrayVarHandle()  throws Throwable {
        // Point[] <: Point.ref[] <: Object
        Point[] array1 = (Point[]) newArray(Point[].class, POINTS.length);
        assertTrue(array1.getClass().componentType() == Point.class);
        setElements(array1, POINTS);

        Point[] array3 = new Point[POINTS.length];
        setElements(array3, POINTS);
    }

    /*
     * VarHandle of Line.ref[].class
     */
    @Test
    public void testLineRefVarHandle() throws Throwable {
        // Line[] <: Line.ref[]
        Line.ref[] array1 = (Line.ref[])newArray(Line.ref[].class, LINES.length);
        assertTrue(array1.getClass().componentType() == Line.ref.class);

        setElements(array1, LINES);
        setElements(array1, NULL_LINES);

        Line.ref[] array2 = new Line.ref[LINES.length];
        setElements(array2, LINES);
        setElements(array2, NULL_LINES);

        Line[] array3 = new Line[LINES.length];
        setElements(array3, LINES);
    }

    /*
     * VarHandle of Line[].class
     */
    @Test
    public void testLineVarHandle() throws Throwable {
        Line[] array1 = (Line[])newArray(Line[].class, LINES.length);
        assertTrue(array1.getClass().componentType() == Line.class);
        setElements(array1, LINES);

        Line[] array3 = new Line[LINES.length];
        setElements(array3, LINES);
    }

    /*
     * VarHandle of NonFlattenValue[].class
     */
    @Test
    public void testNonFlattenedValueVarHandle() throws Throwable {
        NonFlattenValue[] array1 = (NonFlattenValue[])newArray(NonFlattenValue[].class, NFV_ARRAY.length);
        assertTrue(array1.getClass().componentType() == NonFlattenValue.class);
        setElements(array1, NFV_ARRAY);

        NonFlattenValue[] array3 = new NonFlattenValue[POINTS.length];
        setElements(array3, NFV_ARRAY);
    }

    Object[] newArray(Class<?> arrayType, int size) throws Throwable {
        MethodHandle ctor = MethodHandles.arrayConstructor(arrayType);
        return (Object[]) ctor.invoke(size);
    }

    void setElements(Object[] array, Object[] elements) {
        Class<?> arrayType = array.getClass();
        assertTrue(array.length >= elements.length);

        VarHandle vh = MethodHandles.arrayElementVarHandle(arrayType);
        set(vh, array.clone(), elements);
        setVolatile(vh, array.clone(), elements);
        setOpaque(vh, array.clone(), elements);
        setRelease(vh, array.clone(), elements);
        getAndSet(vh, array.clone(), elements);
        compareAndSet(vh, array.clone(), elements);
        compareAndExchange(vh, array.clone(), elements);
    }

    // VarHandle::set
    void set(VarHandle vh, Object[] array, Object[] elements) {
        for (int i = 0; i < elements.length; i++) {
            vh.set(array, i, elements[i]);
        }
        for (int i = 0; i < elements.length; i++) {
            Object v = (Object) vh.get(array, i);
            assertEquals(v, elements[i]);
        }
    }

    // VarHandle::setVolatile
    void setVolatile(VarHandle vh, Object[] array, Object[] elements) {
        for (int i = 0; i < elements.length; i++) {
            vh.setVolatile(array, i, elements[i]);
        }
        for (int i = 0; i < elements.length; i++) {
            Object v = (Object) vh.getVolatile(array, i);
            assertEquals(v, elements[i]);
        }
    }

    // VarHandle::setOpaque
    void setOpaque(VarHandle vh, Object[] array, Object[] elements) {
        for (int i = 0; i < elements.length; i++) {
            vh.setOpaque(array, i, elements[i]);
        }
        for (int i = 0; i < elements.length; i++) {
            Object v = (Object) vh.getOpaque(array, i);
            assertEquals(v, elements[i]);
        }
    }

    // VarHandle::setRelease
    void setRelease(VarHandle vh, Object[] array, Object[] elements) {
        for (int i = 0; i < elements.length; i++) {
            vh.setRelease(array, i, elements[i]);
        }
        for (int i = 0; i < elements.length; i++) {
            Object v = (Object) vh.getAcquire(array, i);
            assertEquals(v, elements[i]);
        }
    }

    void getAndSet(VarHandle vh, Object[] array, Object[] elements) {
        for (int i = 0; i < elements.length; i++) {
            Object o = vh.getAndSet(array, i, elements[i]);
        }
        for (int i = 0; i < elements.length; i++) {
            Object v = (Object) vh.get(array, i);
            assertEquals(v, elements[i]);
        }
    }

    // sanity CAS test
    // see test/jdk/java/lang/invoke/VarHandles tests
    void compareAndSet(VarHandle vh, Object[] array, Object[] elements) {
        // initialize to some values
        for (int i = 0; i < elements.length; i++) {
            vh.set(array, i, elements[i]);
        }
        // shift to the right element
        for (int i = 0; i < elements.length; i++) {
            Object v = elements[i + 1 < elements.length ? i + 1 : 0];
            boolean cas = vh.compareAndSet(array, i, elements[i], v);
            if (!cas)
                System.out.format("cas = %s array[%d] = %s vs old = %s new = %s%n", cas, i, array[i], elements[i], v);
            assertTrue(cas);
        }
    }

    void compareAndExchange(VarHandle vh, Object[] array, Object[] elements) {
        // initialize to some values
        for (int i = 0; i < elements.length; i++) {
            vh.set(array, i, elements[i]);
        }
        // shift to the right element
        for (int i = 0; i < elements.length; i++) {
            Object v = elements[i + 1 < elements.length ? i + 1 : 0];
            assertEquals(vh.compareAndExchange(array, i, elements[i], v), elements[i]);
        }
    }


}
