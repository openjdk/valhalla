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
 * @summary test MethodHandle/VarHandle on inline types
 * @run testng/othervm MethodHandleTest
 */

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

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
        MutablePath path = MutablePath.makePath(1, 2, 3, 44);
        Point p = Point.makePoint(100, 200);
        test.setValueField("p1", path, p);
        test.setValueField("p2", path, p);
    }

    @Test
    public static void testValueFields() throws Throwable {
        MutablePath path = MutablePath.makePath(1, 2, 3, 4);
        // p1 and p2 are a non-final field of inline type in a reference
        MethodHandleTest test1 = new MethodHandleTest("Point", path.p1, "x", "y");
        test1.run();

        MethodHandleTest test2 = new MethodHandleTest("Point", path.p2, "x", "y");
        test2.run();
    }

    @Test
    public static void testMixedValues() throws Throwable {
        MixedValues mv = new MixedValues(P, L, PATH, "mixed", "types");
        MethodHandleTest test =
            new MethodHandleTest("MixedValues", mv, "p", "l", "mutablePath", "list", "nfp");
        test.run();

        Point p = Point.makePoint(100, 200);
        Line l = Line.makeLine(100, 200, 300, 400);
        test.setValueField("p", mv, p);
        test.setValueField("nfp", mv, p);
        test.setValueField("l", mv, l);
        test.setValueField("l", mv, l);
        test.setValueField("staticPoint", null, p);
        test.setValueField("staticLine", null, l);
        // staticLine is a nullable field
        test.setValueField("staticLine", null, null);
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

        Class<?> elementType = c.getComponentType();
        if (elementType.isInlineClass()) {
            assertTrue(elementType == elementType.asPrimaryType());
        }
        // set an array element to null
        try {
            Object v = (Object)setter.invoke(array, 0, null);
            assertFalse(elementType.isInlineClass(), "should fail to set an inline class array element to null");
        } catch (NullPointerException e) {
            assertTrue(elementType.isInlineClass(), "should only fail to set an inline class array element to null");
        }
    }

    @Test
    public static void testNullableArray() throws Throwable {
        Class<?> arrayClass = (new Point?[0]).getClass();
        Class<?> elementType = arrayClass.getComponentType();
        assertTrue(elementType == Point.class.asIndirectType(), arrayClass.getComponentType().toString());

        MethodHandle setter = MethodHandles.arrayElementSetter(arrayClass);
        MethodHandle getter = MethodHandles.arrayElementGetter(arrayClass);
        MethodHandle ctor = MethodHandles.arrayConstructor(arrayClass);
        Object[] array = (Object[]) ctor.invoke(2);
        setter.invoke(array, 0, P);
        setter.invoke(array, 1, null);
        assertEquals((Point)getter.invoke(array, 0), P);
        assertNull((Object)getter.invoke(array, 1));
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
            if (c.isInlineClass())
                ensureImmutable(f);
            else
                ensureNullable(f);
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

    /*
     * Test setting a field of an inline type to a new value.
     * The field must be flattenable but may or may not be flattened.
     */
    void setValueField(String name, Object obj, Object value) throws Throwable {
        Field f = c.getDeclaredField(name);
        boolean isStatic = Modifier.isStatic(f.getModifiers());
        assertTrue(f.getType().isInlineClass());
        assertTrue((isStatic && obj == null) || (!isStatic && obj != null));
        Object v = f.get(obj);

        // Field::set
        try {
            f.set(obj, value);
            assertEquals(f.get(obj), value);
        } finally {
            f.set(obj, v);
        }


        if (isStatic) {
            setStaticField(f, value);
        } else {
            setInstanceField(f, obj, value);
        }
    }

    private void setInstanceField(Field f, Object obj, Object value) throws Throwable {
        Object v = f.get(obj);
        // MethodHandle::invoke
        try {
            MethodHandle mh = MethodHandles.lookup().findSetter(c, f.getName(), f.getType());
            mh.invoke(obj, value);
            assertEquals(f.get(obj), value);
        } finally {
            f.set(obj, v);
        }
        // VarHandle::set
        try {
            VarHandle vh = MethodHandles.lookup().findVarHandle(c, f.getName(), f.getType());
            vh.set(obj, value);
            assertEquals(f.get(obj), value);
        } finally {
            f.set(obj, v);
        }
    }

    private void setStaticField(Field f, Object value) throws Throwable {
        Object v = f.get(null);
        // MethodHandle::invoke
        try {
            MethodHandle mh = MethodHandles.lookup().findStaticSetter(c, f.getName(), f.getType());
            mh.invoke(f.getType().cast(value));
            assertEquals(f.get(null), value);
        } finally {
            f.set(null, v);
        }
        // VarHandle::set
        try {
            VarHandle vh = MethodHandles.lookup().findStaticVarHandle(c, f.getName(), f.getType());
            vh.set(f.getType().cast(value));
            assertEquals(f.get(null), value);
        } finally {
            f.set(null, v);
        }
    }

    /*
     * Test setting the given field to null via reflection, method handle
     * and var handle.
     */
    void ensureNullable(Field f) throws Throwable {
        assertFalse(Modifier.isStatic(f.getModifiers()));
        boolean canBeNull = f.getType().isNullableType();
        // test reflection
        try {
            f.set(o, null);
            assertTrue(canBeNull, f + " cannot be set to null");
        } catch (NullPointerException e) {
            assertFalse(canBeNull, f + " should allow be set to null");
        }
        // test method handle, i.e. putfield bytecode behavior
        try {
            MethodHandle mh = MethodHandles.lookup().findSetter(c, f.getName(), f.getType());
            mh.invoke(o, null);
            assertTrue(canBeNull, f + " cannot be set to null");
        } catch (NullPointerException e) {
            assertFalse(canBeNull, f + " should allow be set to null");
        }
        // test var handle
        try {
            VarHandle vh = MethodHandles.lookup().findVarHandle(c, f.getName(), f.getType());
            vh.set(o, null);
            assertTrue(canBeNull, f + " cannot be set to null");
        } catch (NullPointerException e) {
            assertFalse(canBeNull, f + " should allow be set to null");
        }
    }

    void ensureImmutable(Field f) throws Throwable {
        assertFalse(Modifier.isStatic(f.getModifiers()));
        Object v = f.get(o);
        // test reflection
        try {
            f.set(o, v);
            throw new RuntimeException(f + " should be immutable");
        } catch (IllegalAccessException e) {}

        // test method handle, i.e. putfield bytecode behavior
        try {
            MethodHandle mh = MethodHandles.lookup().findSetter(c, f.getName(), f.getType());
            mh.invoke(o, v);
            throw new RuntimeException(f + " should be immutable");
        } catch (IllegalAccessException e) { }
        // test var handle
        try {
            VarHandle vh = MethodHandles.lookup().findVarHandle(c, f.getName(), f.getType());
            vh.set(o, v);
            throw new RuntimeException(f + " should be immutable");
        } catch (UnsupportedOperationException e) {}
    }

    boolean isFlattened(Field f) {
        return (f.getModifiers() & 0x00008000) == 0x00008000;
    }
}
