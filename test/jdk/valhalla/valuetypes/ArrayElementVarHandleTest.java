/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

    private static final ValueOptional[] VALUES = new ValueOptional[]{
            new ValueOptional(null),
            new ValueOptional(P),
            null
    };

    @DataProvider(name="data")
    static Object[][] data() throws Throwable {
        int plen = POINTS.length;
        int llen = LINES.length;
        int vlen = VALUES.length;
        return new Object[][]{
                // Point[] <: Point.ref[] <: Object[]
                new Object[] { newArray(Object[].class, plen),    POINTS },
                new Object[] { newArray(Object[].class, plen),    NULL_POINTS },
                new Object[] { newArray(Object[].class, plen),    new Object[] { "abc", Point.makePoint(1, 2) } },
                new Object[] { newArray(Point.ref[].class, plen), NULL_POINTS },
                new Object[] { newArray(Point[].class, plen),     POINTS },
                new Object[] { new Point.ref[plen],               POINTS },
                new Object[] { new Point.ref[plen],               NULL_POINTS },
                new Object[] { new Point[plen],                   POINTS },

                // Line[] <: Line.ref[]
                new Object[] { newArray(Object[].class, llen),    LINES },
                new Object[] { newArray(Object[].class, llen),    NULL_LINES },
                new Object[] { newArray(Object[].class, llen),    LINES },
                new Object[] { newArray(Line.ref[].class, llen),  NULL_LINES },
                new Object[] { newArray(Line[].class, llen),      LINES },
                new Object[] { new Line.ref[llen],                LINES },
                new Object[] { new Line.ref[llen],                NULL_LINES },
                new Object[] { new Line[llen],                    LINES },

                // value class
                new Object[] { newArray(Object[].class, vlen),        VALUES },
                new Object[] { newArray(ValueOptional[].class, vlen), VALUES },
                new Object[] { new ValueOptional[vlen],               VALUES },

                // non flattened values
                new Object[] { newArray(NonFlattenValue[].class, NFV_ARRAY.length), NFV_ARRAY },
                new Object[] { new NonFlattenValue[NFV_ARRAY.length], NFV_ARRAY }
        };
    }

    /*
     * Test VarHandle to set elements of the given array with
     * various access mode.
     */
    @Test(dataProvider = "data")
    public void testSetArrayElements(Object[] array, Object[] data) throws Throwable {
        setElements(array, data);
    }

    /*
     * Constructs a new array of the specified type and size using
     * MethodHandle.
     */
    static Object[] newArray(Class<?> arrayType, int size) throws Throwable {
        MethodHandle ctor = MethodHandles.arrayConstructor(arrayType);
        return (Object[]) ctor.invoke(size);
    }

    /*
     * Sets the given array with the given elements.
     * This tests several VarHandle access mode.
     */
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
