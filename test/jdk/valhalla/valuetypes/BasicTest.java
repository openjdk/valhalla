/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @compile --enable-preview --source ${jdk.version} BasicTest.java
 * @run testng/othervm --enable-preview BasicTest
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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

    @DataProvider(name="constants")
    static Object[][] constants() {
        return new Object[][]{
            new Object[] { Point.class, Point.class.asValueType()},
            new Object[] { Point.val.class, Point.class.asValueType()},
            new Object[] { Point.ref.class, Point.class.asPrimaryType()},
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
        };
    }
    @Test(dataProvider="refTypes")
    public void isPrimaryType(Class<?> type, boolean isRefType) {
        assertTrue(type.isPrimaryType() == isRefType);
    }

    @Test
    public void testMirrors() {
        Class<?> refType = Point.class.asPrimaryType();
        Class<?> valType = Point.class.asValueType();

        assertTrue(refType == Point.ref.class);
        // ## ldc not implemented
        assertTrue(valType == Point.val.class);
        assertTrue(refType != valType);
        assertTrue(refType.isPrimitiveClass());
        assertTrue(valType.isPrimitiveClass());
        assertTrue(refType.isPrimaryType());
        assertFalse(valType.isPrimaryType());

        assertEquals(refType.getName(), "BasicTest$Point");
        assertEquals(valType.getName(), "BasicTest$Point");
    }

    @DataProvider(name="names")
    static Object[][] names() {
        return new Object[][]{
                new Object[] { "BasicTest$Point", Point.class.asPrimaryType()},
                new Object[] { "[QBasicTest$Point;", Point[].class},
                new Object[] { "[LBasicTest$Point;", Point.ref[].class},
        };
    }
    @Test(dataProvider="names")
    public void classForName(String name, Class<?> expected) throws ClassNotFoundException {
        Class<?> type = Class.forName(name);
        assertTrue(type == expected);
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

    @Test
    public void testConstructor() throws ReflectiveOperationException {
        Constructor<?> ctor = Point.class.getDeclaredConstructor(int.class, int.class);
        assertTrue(ctor.getDeclaringClass() == Point.class.asPrimaryType());
    }

}
