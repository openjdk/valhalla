/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test reflection on primitive classes
 * @compile --enable-preview --source ${jdk.version} BasicTest.java
 * @run testng/othervm --enable-preview BasicTest
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

public class BasicTest {
    static primitive class Point {
        int x;
        int y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        int x() {
            return x;
        }

        int y() {
            return y;
        }
        static Point.val newVal(int x, int y) {
            return new Point(x, y);
        }
        static Point.ref toRef(Object o) {
            return (Point.ref) o;
        }
        static Point.val toVal(Point.ref o) {
            return (Point.val) o;
        }
    }

    static value class Value {
        int v;
        Value(int v) {
            this.v = v;
        }
    }

    @DataProvider(name="constants")
    static Object[][] constants() {
        return new Object[][]{
            new Object[] { Point.class, Point.class.asPrimaryType()},
            new Object[] { Point.val.class, Point.class.asValueType()},
            new Object[] { Point.ref.class, Point.class.asPrimaryType()},
            new Object[] { Value.class, Value.class.asPrimaryType()},
        };
    }

    @Test(dataProvider="constants")
    public void ldc(Class<?> type, Class<?> expected) {
        assertTrue(type == expected);
    }

    @DataProvider(name="refTypes")
    static Object[][] refTypes() {
        return new Object[][]{
                new Object[] { int.class, true},
                new Object[] { Integer.class, true},
                new Object[] { Object.class, true},
                new Object[] { Point.ref.class, true},
                new Object[] { Point.val.class, false},
                new Object[] { Point.class.asPrimaryType(), true},
                new Object[] { Point.class.asValueType(), false},
                new Object[] { Value.class, true},
        };
    }
    @Test(dataProvider="refTypes")
    public void isPrimaryType(Class<?> type, boolean isRefType) {
        assertTrue(type.isPrimaryType() == isRefType);
    }

    /*
     * Tests the primary and secondary mirror.
     */
    @Test
    public void testMirrors() {
        Class<?> refType = Point.class.asPrimaryType();
        Class<?> valType = Point.class.asValueType();

        assertTrue(refType == Point.ref.class);
        assertTrue(valType == Point.val.class);
        assertTrue(refType != valType);

        assertTrue(refType.isPrimitiveClass());
        assertTrue(valType.isPrimitiveClass());

        assertTrue(refType.isPrimaryType());
        assertFalse(refType.isPrimitiveValueType());

        assertTrue(valType.isPrimitiveValueType());
        assertFalse(valType.isPrimaryType());

        assertEquals(refType.getName(), valType.getName());
        assertEquals(refType.getName(), "BasicTest$Point");
        assertEquals(refType.getSimpleName(),"Point");
        assertEquals(valType.getSimpleName(),"Point");

        assertEquals(valType.getTypeName(), "BasicTest$Point");
        assertEquals(refType.getTypeName(), "BasicTest$Point.ref");

        assertEquals(valType.descriptorString(), "QBasicTest$Point;");
        assertEquals(refType.descriptorString(), "LBasicTest$Point;");
    }

    /*
     * Tests subtyping relationship: Point <: Point.ref and Point <: Object
     *
     * Class:isAssignableFrom
     * Class::isInstance
     * Class::asSubclass
     */
    @Test
    public void testSubtypes() {
        // Point <: Point.ref and Point <: Object
        assertTrue(Point.ref.class.isAssignableFrom(Point.class.asValueType()));
        assertTrue(Object.class.isAssignableFrom(Point.class.asValueType()));
        assertFalse(Point.class.asValueType().isAssignableFrom(Point.ref.class));
        assertTrue(Object.class.isAssignableFrom(Point.ref.class));

        assertTrue(Point.class.asValueType().asSubclass(Point.ref.class) == Point.class.asValueType());
        try {
            Class<?> c = Point.ref.class.asSubclass(Point.class.asValueType());
            fail("Point.ref cannot be cast to Point.class");
        } catch (ClassCastException e) { }

        Point o = new Point(10, 20);
        assertTrue(Point.class.asValueType().isInstance(o));
        assertTrue(Point.ref.class.isInstance(o));
        assertFalse(Point.class.asValueType().isInstance(null));
        assertFalse(Point.ref.class.isInstance(null));
    }

    @DataProvider(name="names")
    static Object[][] names() {
        return new Object[][]{
                new Object[] { "BasicTest$Point", Point.class.asPrimaryType()},
                new Object[] { "[QBasicTest$Point;", Point[].class},
                new Object[] { "[[LBasicTest$Point;", Point.ref[][].class},
                new Object[] { "BasicTest$Value", Value.class},
                new Object[] { "[LBasicTest$Value;", Value[].class},
        };
    }
    @Test(dataProvider="names")
    public void classForName(String name, Class<?> expected) throws ClassNotFoundException {
        Class<?> type = Class.forName(name);
        assertTrue(type == expected);
        assertEquals(type.getName(), name);
    }

    @Test
    public void testNull() {
        Point.ref ref = Point.toRef(null);
        assertTrue(ref == null);
        try {
            Point.toVal(null);
            throw new RuntimeException("expected NPE thrown");
        } catch (NullPointerException e) {}
    }

    @Test
    public void testConversion() {
        Point p = new Point(10,20);
        Point.ref ref = Point.toRef(p);
        Point.val val = Point.toVal(ref);
        assertEquals(ref, p);
        assertEquals(val, p);
    }

    @Test
    public void testMembers() {
        Method[] refMethods = Point.ref.class.getDeclaredMethods();
        Method[] valMethods = Point.val.class.getDeclaredMethods();
        assertEquals(refMethods, valMethods);
        assertTrue(valMethods.length == 5);

        Field[] refFields = Point.ref.class.getDeclaredFields();
        Field[] valFields = Point.val.class.getDeclaredFields();
        assertEquals(refFields, valFields);
        assertTrue(valFields.length == 2);

        Constructor[] refCtors = Point.ref.class.getDeclaredConstructors();
        Constructor[] valCtors = Point.val.class.getDeclaredConstructors();
        assertEquals(refCtors, valCtors);
        assertTrue(valCtors.length == 1);
        assertTrue(Modifier.isStatic(valCtors[0].getModifiers()));
    }

    @DataProvider(name="methods")
    static Object[][] methods() {
        return new Object[][]{
                new Object[] { "toVal", Point.val.class, new Class<?>[] { Point.ref.class }},
                new Object[] { "toRef", Point.ref.class, new Class<?>[] { Object.class }},
        };
    }

    @Test(dataProvider = "methods")
    public void testMethod(String name, Class<?> returnType, Class<?>[] paramTypes) throws ReflectiveOperationException {
        Method m = Point.class.getDeclaredMethod(name, paramTypes);
        System.out.print(m.toString() + "  ");
        System.out.println(m.getReturnType().descriptorString());
        assertTrue(m.getDeclaringClass() == Point.class.asPrimaryType());
        assertTrue(m.getReturnType() == returnType);
        assertEquals(m.getParameterTypes(), paramTypes);
    }

    @DataProvider(name="ctors")
    static Object[][] ctors() {
        return new Object[][]{
                new Object[] { Point.class, new Class<?>[] { int.class, int.class}, new Object[] { 10, 10 }},
                new Object[] { Value.class, new Class<?>[] { int.class }, new Object[] { 20 }},
        };
    }

    @Test(dataProvider = "ctors")
    public void testConstructor(Class<?> c, Class<?>[] paramTypes, Object[] params) throws ReflectiveOperationException {
        Constructor<?> ctor = c.getDeclaredConstructor(paramTypes);
        assertTrue(ctor.getDeclaringClass() == c.asPrimaryType());
        Object o = ctor.newInstance(params);
    }

    class C { }
    primitive class T { }

    @DataProvider(name="intfs")
    Object[][] intfs() {
        Point point = new Point(10, 20);
        Point[] array = new Point[] { point };
        Value value = new Value(10);
        Class<?> [] empty_intfs = new Class<?>[0];
        return new Object[][]{
                new Object[]{ new BasicTest(), empty_intfs },
                new Object[]{ point, empty_intfs },
                new Object[]{ new T(), empty_intfs },
                new Object[]{ new C(), empty_intfs },
                new Object[]{ new Object(), empty_intfs },
                new Object[]{ array, new Class<?>[] { Cloneable.class, Serializable.class }},
                new Object[]{ value, empty_intfs },
        };
    }

    @Test(dataProvider = "intfs")
    public void testGetInterfaces(Object o, Class<?>[] expectedInterfaces) {
        Class<?> type = o.getClass();
        assertEquals(type.getInterfaces(), expectedInterfaces);
    }

    @Test
    public void testNestMembership() {
        assertTrue(Point.class.getNestHost() == BasicTest.class);
        assertTrue(T.class.getNestHost() == BasicTest.class);
        assertTrue(C.class.getNestHost() == BasicTest.class);

        Class<?>[] members = BasicTest.class.getNestMembers();
        assertEquals(Point.class.getNestMembers(), members);
        assertEquals(T.class.getNestMembers(), members);
        assertEquals(C.class.getNestMembers(), members);
        assertEquals(Arrays.stream(members).collect(Collectors.toSet()),
                     Set.of(BasicTest.class,
                            Value.class,
                            Point.class.asPrimaryType(),
                            C.class.asPrimaryType(),
                            T.class.asPrimaryType()));
    }
}
