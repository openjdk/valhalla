/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test reflection on inline types
 * @run main/othervm Reflection
 */

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;

public class Reflection {
    public static void main(String... args) throws Exception {
        testPointClass();
        testLineClass();
        testNonFlattenValue();
        testMirrors();
        testClassName();
    }

    static void testPointClass() throws Exception  {
        Point o = Point.makePoint(10, 20);
        Reflection test = new Reflection(Point.class, "Point", o);
        test.newInstance();
        test.constructor();
        test.accessFieldX(o.x);
        test.staticField();
    }

    static void testLineClass() throws Exception {
        Line l = Line.makeLine(10, 20, 30, 40);
        Reflection test = new Reflection(Line.class, "Line", l);
        test.checkField("public final Point Line.p1", "p1", Point.class);
        test.checkField("public final Point Line.p2", "p2", Point.class);
        test.checkMethod("public Point Line.p1()",           "p1", Point.class);
        test.checkMethod("public Point Line.p2()",           "p2", Point.class);
    }

    static void testNonFlattenValue() throws Exception {
        NonFlattenValue nfv = NonFlattenValue.make(10, 20);
        Reflection test = new Reflection(NonFlattenValue.class, "NonFlattenValue", nfv);
        test.checkField("final Point? NonFlattenValue.nfp", "nfp", Point.class.asIndirectType());
        test.checkMethod("public Point NonFlattenValue.pointValue()", "pointValue", Point.class);
        test.checkMethod("public Point? NonFlattenValue.point()", "point", Point.class.asIndirectType());
        test.checkMethod("public boolean NonFlattenValue.has(Point,Point?)", "has", boolean.class, Point.class, Point.class.asIndirectType());
    }

    /*
     * Tests reflection APIs with the primary type and indirect/nullable projection type
     */
    static void testMirrors() throws Exception {
        Class<?> primary = Point.class;
        Class<?> indirect = Point.class.asIndirectType();

        assertEquals(primary, Point.class);
        assertEquals(indirect, Point.class.asNullableType());
        assertTrue(primary.isInlineClass());
        assertFalse(primary.isIndirectType());
        assertFalse(primary.isNullableType());

        assertTrue(indirect.isInlineClass());
        assertTrue(indirect.isIndirectType());
        assertTrue(indirect.isNullableType());

        Point o = Point.makePoint(10, 20);
        assertTrue(primary.isInstance(o));
        assertTrue(indirect.isInstance(o));

        // V <: V? and V <: Object
        assertTrue(indirect.isAssignableFrom(primary));
        assertTrue(Object.class.isAssignableFrom(primary));
        assertFalse(primary.isAssignableFrom(indirect));
        assertTrue(Object.class.isAssignableFrom(indirect));

        assertEquals(primary, primary.asSubclass(indirect));
        try {
            Class<?> c = indirect.asSubclass(primary);
            assertTrue(false);
        } catch (ClassCastException e) { }

        // indirect class
        assertEquals(Reflection.class.asPrimaryType(), Reflection.class);
        assertEquals(Reflection.class.asIndirectType(), Reflection.class);
        assertEquals(Reflection.class.asNullableType(), Reflection.class);
        assertTrue(Reflection.class.isIndirectType());
        assertTrue(Reflection.class.isNullableType());
    }

    static void testClassName() {
        assertEquals(Point.class.getName(), "Point");
        assertEquals(Point.class.asNullableType().getName(), "Point");
        assertEquals(Line.class.getName(), "Line");
        assertEquals((new Point[0]).getClass().getName(), "[QPoint;");
        assertEquals((new Point?[0][0]).getClass().getName(), "[[LPoint;");
    }

    private final Class<?> c;
    private final Constructor<?> ctor;
    private final Object o;
    Reflection(Class<?> type, String cn, Object o) throws Exception {
        this.c = Class.forName(cn);
        if (!c.isInlineClass() || c != type) {
            throw new RuntimeException(cn + " is not an inline class");
        }

        // V.class, Class.forName, and the type of the object return the primary mirror
        assertEquals(type, o.getClass());
        assertEquals(type, c.asPrimaryType());
        assertEquals(c, c.asPrimaryType());

        this.ctor = c.getDeclaredConstructor();
        this.o = o;


        // test the primary mirror and secondary mirror
        testMirrors(this.c);
        // test array of Q-type and L-type
        testArray(c.asPrimaryType());
        testArray(c.asNullableType());
    }

    private static void testMirrors(Class<?> c) {
        Class<?> inlineType = c.asPrimaryType();
        Class<?> nullableType = c.asNullableType();

        assertTrue(inlineType != null);
        assertEquals(nullableType.getTypeName(), c.getTypeName() + "?");

        assertEquals(nullableType.getName(), inlineType.getName());
        assertEquals(nullableType.getTypeName(), inlineType.getTypeName() + "?");
        assertEquals(inlineType.asNullableType(), nullableType);
        assertEquals(nullableType.asPrimaryType(), inlineType);
    }

    void testArray(Class<?> elementType) {
        Object[] array = (Object[])Array.newInstance(elementType, 1);
        Class<?> arrayType = array.getClass();
        assertTrue(arrayType.isArray());
        Class<?> componentType = arrayType.getComponentType();
        assertTrue(componentType.isInlineClass());
        assertEquals(componentType, elementType);
        // Array is a reference type
        assertEquals(arrayType.asNullableType(), arrayType);
        if (array[0] == null) {
            System.out.println("array[0] = null");
        } else {
            System.out.println("array[0] = " + array[0]);
        }
    }

    void accessFieldX(int x) throws Exception {
        Field field = c.getField("x");
        if (field.getInt(o) != x) {
            throw new RuntimeException("Unexpected Point.x value: " +  field.getInt(o));
        }

        try {
            field.setInt(o, 100);
            throw new RuntimeException("IllegalAccessException not thrown");
        } catch (IllegalAccessException e) {}
    }

    @SuppressWarnings("deprecation")
    void newInstance() throws Exception {
        Object o = c.newInstance();
        assertEquals(o.getClass(), c);
    }

    void constructor() throws Exception {
        Object o = ctor.newInstance();
        assertEquals(o.getClass(), c);
    }

    void staticField() throws Exception {
        Field f = c.getDeclaredField("STATIC_FIELD");
        if (f.trySetAccessible()) {
            throw new RuntimeException("trySetAccessible should not succeed");
        }
        try {
            f.setAccessible(true);
            throw new RuntimeException("IllegalAccessException not thrown");
        } catch (InaccessibleObjectException e) { }
    }

    void checkField(String source, String name, Class<?> type) throws Exception {
        Field f = c.getDeclaredField(name);
        assertEquals(f.getType(), type);
        assertEquals(f.toString(), source);
    }

    void checkMethod(String source, String name, Class<?> returnType, Class<?>... params) throws Exception {
        Method m = c.getDeclaredMethod(name, params);
        assertEquals(m.toString(), source);
    }

    static void assertEquals(Object o1, Object o2) {
        if (o1 == o2 || o1.equals(o2))
            return;

        throw new AssertionError(o1 + " != " + o2);
    }

    static void assertTrue(boolean value) {
        if (!value)
            throw new AssertionError("expected true");
    }

    static void assertFalse(boolean value) {
        if (value)
            throw new AssertionError("expected false");
    }
}
