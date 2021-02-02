/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @compile --enable-preview --source ${jdk.version} Reflection.java
 * @run testng/othervm --enable-preview Reflection
 */

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class Reflection {
    @Test
    public static void sanityTest() {
        assertTrue(Point.ref.class.getPermittedSubclasses().length == 1);
        assertTrue(Line.ref.class.getPermittedSubclasses().length == 1);
        assertTrue(NonFlattenValue.ref.class.getPermittedSubclasses().length == 1);
    }

    @Test
    public static void testPointClass() throws Exception  {
        Point o = Point.makePoint(10, 20);
        Reflection test = new Reflection(Point.class, "Point", o);
        test.newInstance();
        test.constructor();
        test.accessFieldX(o.x);
        test.staticField();
    }

    @Test
    public static void testLineClass() throws Exception {
        Line l = Line.makeLine(10, 20, 30, 40);
        Reflection test = new Reflection(Line.class, "Line", l);
        test.checkField("public final Point Line.p1", "p1", Point.class);
        test.checkField("public final Point Line.p2", "p2", Point.class);
        test.checkMethod("public Point Line.p1()",           "p1", Point.class);
        test.checkMethod("public Point Line.p2()",           "p2", Point.class);
    }

    @Test
    public static void testNonFlattenValue() throws Exception {
        NonFlattenValue nfv = NonFlattenValue.make(10, 20);
        Reflection test = new Reflection(NonFlattenValue.class, "NonFlattenValue", nfv);
        test.checkField("final Point$ref NonFlattenValue.nfp", "nfp", Point.ref.class);
        test.checkMethod("public Point NonFlattenValue.pointValue()", "pointValue", Point.class);
        test.checkMethod("public Point$ref NonFlattenValue.point()", "point", Point.ref.class);
        test.checkMethod("public boolean NonFlattenValue.has(Point,Point$ref)", "has", boolean.class, Point.class, Point.ref.class);
    }

    /*
     * Tests reflection APIs with the value and reference projection type
     */
    @Test
    public static void testMirrors() throws Exception {
        Class<?> inlineClass = Point.class;
        assertTrue(inlineClass.isInlineClass());
        assertFalse(Point.ref.class.isInlineClass());
        assertEquals(inlineClass.valueType().get(), Point.class);
        assertEquals(inlineClass.referenceType().get(), Point.ref.class);
        assertEquals(Point.ref.class.valueType().get(), Point.class);
        assertEquals(Point.ref.class.referenceType().get(), Point.ref.class);

        Point o = Point.makePoint(10, 20);
        assertTrue(Point.class.isInstance(o));
        assertTrue(Point.ref.class.isInstance(o));

    }

    @Test
    public static void testAssignableFrom() {
        // V <: V? and V <: Object
        assertTrue(Point.ref.class.isAssignableFrom(Point.class));
        assertTrue(Object.class.isAssignableFrom(Point.class));
        assertFalse(Point.class.isAssignableFrom(Point.ref.class));
        assertTrue(Object.class.isAssignableFrom(Point.ref.class));

        assertEquals(Point.class, Point.class.asSubclass(Point.ref.class));
        try {
            Class<?> c = Point.ref.class.asSubclass(Point.class);
            assertTrue(false);
        } catch (ClassCastException e) { }
    }

    @Test
    public static void testClassName() {
        assertEquals(Point.class.getName(), "Point");
        assertEquals(Point.ref.class.getName(), "Point$ref");
        assertEquals(Line.class.getName(), "Line");
        assertEquals((new Point[0]).getClass().getName(), "[QPoint;");
        assertEquals((new Point.ref[0][0]).getClass().getName(), "[[LPoint$ref;");
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
        assertEquals(type, c.valueType().get());
        assertEquals(c, c.valueType().get());

        this.ctor = c.getDeclaredConstructor();
        this.o = o;


        // test the primary mirror and secondary mirror
        testMirrors(this.c);
        // test array of Q-type and L-type
        testArray(c.valueType().get());
        testArray(c.referenceType().get());
    }

    private static void testMirrors(Class<?> c) {
        Class<?> valType = c.valueType().get();
        Class<?> refType = c.referenceType().get();

        assertTrue(valType != null);
        assertEquals(refType.getTypeName(), c.getTypeName() + "$ref");
        assertEquals(refType.getSimpleName(), c.getSimpleName() + "$ref");

        assertEquals(refType.getName(), valType.getName() + "$ref");
        assertEquals(refType.getTypeName(), valType.getTypeName() + "$ref");
        assertEquals(refType.getSimpleName(), valType.getSimpleName() + "$ref");

        assertEquals(valType.referenceType().get(), refType);
        assertEquals(refType.valueType().get(), valType);
    }

    void testArray(Class<?> elementType) {
        Object[] array = (Object[])Array.newInstance(elementType, 1);
        Class<?> arrayType = array.getClass();
        assertTrue(arrayType.isArray());
        Class<?> componentType = arrayType.getComponentType();
        assertTrue(componentType.isInlineClass() || componentType.valueType().isPresent());
        assertEquals(componentType, elementType);
        // Array is a reference type
        assertEquals(arrayType.referenceType().get(), arrayType);
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
        f.trySetAccessible();
        assertTrue(f.isAccessible());
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
}
