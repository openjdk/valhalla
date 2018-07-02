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


/*
 * @test
 * @summary test MethodHandles on value types
 * @build Point Line MutablePath
 * @run testng/othervm -XX:+EnableValhalla MethodHandleTest
 */

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.util.*;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class MethodHandleTest {
    private static final Point P = Point.makePoint(10, 20);
    private static final Line L = Line.makeLine(10, 20, 30, 40);
    private static final MutablePath PATH = MutablePath.makePath(10, 20, 30, 40);

    @Test
    public static void testPointClass() throws Throwable  {
        MethodHandleTest test = new MethodHandleTest("Point", P, "x", "y");
        test.run();
    }

    @Test
    public static void testLineClass() throws Throwable {
        MethodHandleTest test = new MethodHandleTest("Line", L, "p1", "p2");
        test.run();
    }

    @Test
    public static void testMutablePath() throws Throwable {
        MethodHandleTest test = new MethodHandleTest("MutablePath", PATH, "p1", "p2");
        test.run();

        // set the mutable fields
        Point p = Point.makePoint(100, 200);
        test.setFlattenedField(p);
    }

    @Test
    public static void testMixedValues() throws Throwable {
        MixedValues mv = new MixedValues(P, L, PATH, "mixed", "types");
        MethodHandleTest test = new MethodHandleTest("MethodHandleTest$MixedValues", mv, "p", "l", "mutablePath", "list");
        test.run();
    }

    @Test
    public static void testArrayElementSetterAndGetter() throws Throwable {
        testArray(Point[].class, P);
        testArray(Line[].class, L);
        testArray(MutablePath[].class, PATH);
    }

    static void testArray(Class<?> c, Object o) throws Throwable {
        MethodHandle setter = MethodHandles.arrayElementSetter(c);
        MethodHandle getter = MethodHandles.arrayElementGetter(c);
        MethodHandle ctor = MethodHandles.arrayConstructor(c);
        int size = 5;
        Object[] array = (Object[])ctor.invoke(size);
        for (int i=0; i < size; i++) {
            setter.invoke(array, i, o);
        }
        for (int i=0; i < size; i++) {
            Object v = (Object)getter.invoke(array, i);
            assertEquals(v, o);
        }

        // set an array element to null
        /*
        Class<?> elementType = c.getComponentType();
        try {
            Object v = (Object)setter.invoke(array, 0, null);
            assertFalse(elementType.isValue(), "should fail to set a value array element to null");
        } catch (NullPointerException e) {
            assertTrue(elementType.isValue(), "should only fail to set a value array element to null");
        }
        */
    }

    private final Class<?> c;
    private final Object o;
    private final List<String> names;
    public MethodHandleTest(String cn, Object o, String... fields) throws Exception {
        this.c = Class.forName(cn);
        this.o = o;
        this.names = List.of(fields);
    }

    public void run() throws Throwable {
        for (String name : names) {
            Field f = c.getDeclaredField(name);
            unreflectField(f);
            findGetter(f);
            varHandle(f);
            // ensureNonNullable(f);
        }
    }

    public List<String> names() {
        return names;
    }

    void findGetter(Field f) throws Throwable {
        MethodHandle mh = MethodHandles.lookup().findGetter(c, f.getName(), f.getType());
        Object value = mh.invoke(o);
    }

    void varHandle(Field f) throws Throwable {
        VarHandle vh = MethodHandles.lookup().findVarHandle(c, f.getName(), f.getType());
        Object value = vh.get(o);
    }

    void unreflectField(Field f) throws Throwable {
        MethodHandle mh = MethodHandles.lookup().unreflectGetter(f);
        Object value = mh.invoke(o);
    }

    void setFlattenedField(Object value) throws Exception {
        for (String name : names) {
            Field f = c.getDeclaredField(name);
            assertTrue(isFlattened(f));
            f.set(o, value);
            Object nv = f.get(o);
            assertEquals(nv, value);
        }
    }

    void ensureNonNullable(Field f) throws Throwable {
        boolean nullable = !f.getType().isValue();
        try {
            f.set(o, null);
            assertTrue(nullable, f + " cannot be set to null");
        } catch (NullPointerException e) {
            assertFalse(nullable, f + " should allow be set to null");
        } catch (IllegalAccessException e) {
            assertTrue(c.isValue());
        }
        try {
            MethodHandle mh = MethodHandles.lookup().findSetter(c, f.getName(), f.getType());
            mh.invoke(o, null);
            assertTrue(nullable, f + " cannot be set to null");
        } catch (NullPointerException e) {
            assertFalse(nullable, f + " should allow be set to null");
        } catch (IllegalAccessException e) {
            assertTrue(c.isValue());
        }
        try {
            VarHandle vh = MethodHandles.lookup().findVarHandle(c, f.getName(), f.getType());
            vh.set(o, null);
            assertTrue(nullable, f + " cannot be set to null");
        } catch (NullPointerException e) {
            assertFalse(nullable, f + " should allow be set to null");
        } catch (UnsupportedOperationException e) {  // TODO: this should be IAE
            assertTrue(c.isValue());
        }
    }


    boolean isFlattened(Field f) {
        return (f.getModifiers() & 0x00008000) == 0x00008000;
    }

    static class MixedValues {
        Point p;
        Line l;
        MutablePath mutablePath;
        List<String> list;

        public MixedValues(Point p, Line l, MutablePath path, String... names) {
            this.p = p;
            this.l = l;
            this.mutablePath = path;
            this.list = List.of(names);
        }
    }

}
