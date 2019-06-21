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
 * @summary test VarHandle on inline class array
 * @run testng/othervm -XX:ValueArrayElemMaxFlatSize=-1 ArrayElementVarHandleTest
 * @run testng/othervm -XX:ValueArrayElemMaxFlatSize=0  ArrayElementVarHandleTest
 */

import java.lang.invoke.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ArrayElementVarHandleTest {
    private final Class<?> varHandleArrayType;
    private final Class<?> componentType;
    private final VarHandle vh;

    ArrayElementVarHandleTest(Class<?> arrayType) {
        this.varHandleArrayType = arrayType;
        this.componentType = arrayType.getComponentType();
        this.vh = MethodHandles.arrayElementVarHandle(arrayType);
    }

    Object[] newArray(int size) throws Throwable {
        MethodHandle ctor = MethodHandles.arrayConstructor(varHandleArrayType);
        return (Object[]) ctor.invoke(size);
    }

    void setElements(Object[] array, Object[] elements) {
        Class<?> arrayType = array.getClass();
        assertTrue(varHandleArrayType.isAssignableFrom(arrayType));
        assertTrue(array.length >= elements.length);
        set(array.clone(), elements);
        setVolatile(array.clone(), elements);
        setOpaque(array.clone(), elements);
        setRelease(array.clone(), elements);
        getAndSet(array.clone(), elements);
        compareAndSet(array.clone(), elements);
        compareAndExchange(array.clone(), elements);
    }

    // VarHandle::set
    void set(Object[] array, Object[] elements) {
        for (int i = 0; i < elements.length; i++) {
            vh.set(array, i, elements[i]);
        }
        for (int i = 0; i < elements.length; i++) {
            Object v = (Object) vh.get(array, i);
            assertEquals(v, elements[i]);
        }
    }

    // VarHandle::setVolatile
    void setVolatile(Object[] array, Object[] elements) {
        for (int i = 0; i < elements.length; i++) {
            vh.setVolatile(array, i, elements[i]);
        }
        for (int i = 0; i < elements.length; i++) {
            Object v = (Object) vh.getVolatile(array, i);
            assertEquals(v, elements[i]);
        }
    }

    // VarHandle::setOpaque
    void setOpaque(Object[] array, Object[] elements) {
        for (int i = 0; i < elements.length; i++) {
            vh.setOpaque(array, i, elements[i]);
        }
        for (int i = 0; i < elements.length; i++) {
            Object v = (Object) vh.getOpaque(array, i);
            assertEquals(v, elements[i]);
        }
    }

    // VarHandle::setRelease
    void setRelease(Object[] array, Object[] elements) {
        for (int i = 0; i < elements.length; i++) {
            vh.setRelease(array, i, elements[i]);
        }
        for (int i = 0; i < elements.length; i++) {
            Object v = (Object) vh.getAcquire(array, i);
            assertEquals(v, elements[i]);
        }
    }

    void getAndSet(Object[] array, Object[] elements) {
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
    void compareAndSet(Object[] array, Object[] elements) {
        // initialize to some values
        for (int i = 0; i < elements.length; i++) {
            vh.set(array, i, elements[i]);
        }
        // shift to the right element
        for (int i = 0; i < elements.length; i++) {
            Object v = elements[i+1 < elements.length ? i+1 : 0];
            boolean cas = vh.compareAndSet(array, i, elements[i], v);
            if (!cas)
                System.out.format("cas = %s array[%d] = %s vs old = %s new = %s%n", cas, i, array[i], elements[i], v);
            assertTrue(cas);
        }
    }
    void compareAndExchange(Object[] array, Object[] elements) {
        // initialize to some values
        for (int i = 0; i < elements.length; i++) {
            vh.set(array, i, elements[i]);
        }
        // shift to the right element
        for (int i = 0; i < elements.length; i++) {
            Object v = elements[i+1 < elements.length ? i+1 : 0];
            assertEquals(vh.compareAndExchange(array, i, elements[i], v), elements[i]);
        }
    }

    private static final Point P = Point.makePoint(10, 20);
    private static final Line L = Line.makeLine(10, 20, 30, 40);
    private static final MutablePath PATH = MutablePath.makePath(10, 20, 30, 40);

    private static final Point[] POINTS = new Point[]{
            Point.makePoint(1, 2),
            Point.makePoint(10, 20),
            Point.makePoint(100, 200)
    };

    private static final Point?[] NULLABLE_POINTS = new Point?[]{
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

    private static final NonFlattenValue[] NFV_ARRAY = new NonFlattenValue[]{
            NonFlattenValue.make(1, 2),
            NonFlattenValue.make(10, 20),
            NonFlattenValue.make(100, 200)
    };

    /*
     * VarHandle of Object[].class
     */
    @Test
    public static void testObjectArrayVarHandle() throws Throwable {
        ArrayElementVarHandleTest test = new ArrayElementVarHandleTest(Object[].class);
        // Point[] <: Point?[] <: Object
        Object[] array1 = test.newArray(POINTS.length);
        test.setElements(array1, POINTS);
        test.setElements(array1, NULLABLE_POINTS);
        test.setElements(array1, new Object[] { "abc", Point.makePoint(1, 2) });

        Point ?[]array2 = new Point ?[NULLABLE_POINTS.length];
        test.setElements(array2, POINTS);
        test.setElements(array2, NULLABLE_POINTS);

        Point[] array3 = new Point[POINTS.length];
        test.setElements(array3, POINTS);
    }

    /*
     * VarHandle of Point?[].class
     */
    @Test
    public static void testIndirectPointVarHandle() throws Throwable {
        Object o = new Point?[0];
        ArrayElementVarHandleTest test = new ArrayElementVarHandleTest(o.getClass());
        assertTrue(test.componentType.isIndirectType());

        // Point[] <: Point?[] <: Object
        Point?[] array1 = (Point?[])test.newArray(POINTS.length);
        test.setElements(array1, POINTS);
        test.setElements(array1, NULLABLE_POINTS);

        Point?[] array2 = new Point?[NULLABLE_POINTS.length];
        test.setElements(array2, POINTS);
        test.setElements(array2, NULLABLE_POINTS);

        Point[] array3 = new Point[POINTS.length];
        test.setElements(array3, POINTS);
    }

    /*
     * VarHandle of Point[].class
     */
    @Test
    public static void testPointArrayVarHandle()  throws Throwable {
        ArrayElementVarHandleTest test = new ArrayElementVarHandleTest(Point[].class);
        assertFalse(test.componentType.isIndirectType());

        // Point[] <: Point?[] <: Object
        Point[] array1 = (Point[]) test.newArray(POINTS.length);
        test.setElements(array1, POINTS);

        Point[] array3 = new Point[POINTS.length];
        test.setElements(array3, POINTS);
    }

    /*
     * VarHandle of Line?[].class
     */
    @Test
    public static void testIndirectLineVarHandle() throws Throwable {
        Line?[] nullableLines = new Line?[] { null, null };
        ArrayElementVarHandleTest test = new ArrayElementVarHandleTest(nullableLines.getClass());
        assertTrue(test.componentType.isIndirectType());

        // Line[] <: Line?[]
        Line?[] array1 = (Line?[])test.newArray(LINES.length);
        test.setElements(array1, LINES);
        test.setElements(array1, nullableLines);

        Line?[] array2 = new Line?[LINES.length];
        test.setElements(array2, LINES);
        test.setElements(array2, nullableLines);

        Line[] array3 = new Line[LINES.length];
        test.setElements(array3, LINES);
    }

    /*
     * VarHandle of Line[].class
     */
    @Test
    public static void testLineVarHandle() throws Throwable {
        ArrayElementVarHandleTest test = new ArrayElementVarHandleTest(Line[].class);
        assertFalse(test.componentType.isIndirectType());

        Line[] array1 = (Line[]) test.newArray(LINES.length);
        test.setElements(array1, LINES);

        Line[] array3 = new Line[LINES.length];
        test.setElements(array3, LINES);
    }

    /*
     * VarHandle of NonFlattenValue[].class
     */
    @Test
    public static void testNonFlattenedValueVarHandle() throws Throwable {
        ArrayElementVarHandleTest test = new ArrayElementVarHandleTest(NonFlattenValue[].class);
        assertFalse(test.componentType.isIndirectType());

        NonFlattenValue[] array1 = (NonFlattenValue[]) test.newArray(NFV_ARRAY.length);
        test.setElements(array1, NFV_ARRAY);

        NonFlattenValue[] array3 = new NonFlattenValue[POINTS.length];
        test.setElements(array3, NFV_ARRAY);
    }
}
