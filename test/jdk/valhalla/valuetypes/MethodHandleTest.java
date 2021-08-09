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
 * @summary test MethodHandle/VarHandle o primitive classes
 * @run testng/othervm MethodHandleTest
 */

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class MethodHandleTest {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final Point P = Point.makePoint(10, 20);
    private static final Line L = Line.makeLine(10, 20, 30, 40);
    private static final MutablePath PATH = MutablePath.makePath(10, 20, 30, 40);

    @DataProvider(name="fields")
    static Object[][] fields() {
        MutablePath path = MutablePath.makePath(1, 2, 3, 4);
        MixedValues mv = new MixedValues(P, L, PATH, "mixed", "types");
        return new Object[][]{
                // primitive class with int fields
                new Object[] { "Point", P, new String[] { "x", "y"} },
                // primitive class whose fields are of primitive value type
                new Object[] { "Line", L, new String[] { "p1", "p2"} },
                // non-primitive class whose non-final fields are of primitive value type
                new Object[] { "MutablePath", PATH, new String[] {"p1", "p2"} },
                new Object[] { "Point", path.p1, new String[] {"x", "y"} },
                new Object[] { "Point", path.p2, new String[] {"x", "y"} },
                new Object[] { "MixedValues", mv, new String[] {"p", "l", "mutablePath", "list", "nfp"} },
        };
    }

    /**
     * Test MethodHandle invocation on the fields of a given class.
     * MethodHandle produced by Lookup::unreflectGetter, Lookup::findGetter,
     * Lookup::findVarHandle.
     */
    @Test(dataProvider = "fields")
    public void testFieldGetterAndSetter(String cn, Object o, String[] fieldNames) throws Throwable  {
        Class<?> c = Class.forName(cn);
        for (String name : fieldNames) {
            Field f = c.getDeclaredField(name);

            MethodHandle mh = LOOKUP.findGetter(c, f.getName(), f.getType());
            Object v1 = mh.invoke(o);

            VarHandle vh = LOOKUP.findVarHandle(c, f.getName(), f.getType());
            Object v2 = vh.get(o);

            MethodHandle mh3 = LOOKUP.unreflectGetter(f);
            Object v3 = mh.invoke(o);

            if (c.isPrimitiveClass())
                ensureImmutable(f, o);
            else
                ensureNullable(f, o);
        }
    }

    @Test
    public void testValueFields() throws Throwable {
        // set the mutable value fields
        MutablePath path = MutablePath.makePath(1, 2, 3, 44);
        Point p = Point.makePoint(100, 200);
        setValueField(MutablePath.class, "p1", path, p);
        setValueField(MutablePath.class, "p2", path, p);
    }

    // Test writing to a field of primitive value type and of primitive
    // reference type
    @Test
    public void testMixedValues() throws Throwable {
        // set the mutable fields
        MutablePath path = MutablePath.makePath(1, 2, 3, 44);
        MixedValues mv = new MixedValues(P, L, PATH, "mixed", "types");
        Point p = Point.makePoint(100, 200);
        Line l = Line.makeLine(100, 200, 300, 400);

        setValueField(MutablePath.class, "p1", path, p);
        setValueField(MutablePath.class, "p2", path, p);
        setValueField(MixedValues.class, "p", mv, p);
        setValueField(MixedValues.class, "l", mv, l);
        setValueField(MixedValues.class, "staticPoint", null, p);
        // the following are nullable fields
        setField(MixedValues.class, "nfp", mv, p, false);
        setField(MixedValues.class, "staticLine", null, l, false);
        setField(MixedValues.class, "staticLine", null, null, false);
    }

    @DataProvider(name="arrays")
    static Object[][] arrays() {
        return new Object[][]{
                new Object[] { Point[].class, P },
                new Object[] { Point.ref[].class, P },
                new Object[] { Line[].class, L },
                new Object[] { MutablePath[].class, PATH },
        };
    }

    @Test(dataProvider = "arrays")
    public void testArrayElementSetterAndGetter(Class<?> arrayClass, Object o) throws Throwable {
        Class<?> elementType = arrayClass.getComponentType();
        MethodHandle setter = MethodHandles.arrayElementSetter(arrayClass);
        MethodHandle getter = MethodHandles.arrayElementGetter(arrayClass);
        MethodHandle ctor = MethodHandles.arrayConstructor(arrayClass);
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
        try {
            Object v = (Object)setter.invoke(array, 1, null);
            assertFalse(elementType.isValueType(), "should fail to set a primitive class array element to null");
            assertNull((Object)getter.invoke(array, 1));
        } catch (NullPointerException e) {
            assertTrue(elementType.isValueType(), "should only fail to set a primitive class array element to null");
        }
    }

    /*
     * Test setting the given field to null via reflection, method handle
     * and var handle.
     */
    static void ensureNullable(Field f, Object o) throws Throwable {
        Class<?> c = f.getDeclaringClass();
        assertFalse(Modifier.isFinal(f.getModifiers()));
        assertFalse(Modifier.isStatic(f.getModifiers()));
        boolean canBeNull = f.getType().isPrimaryType();
        // test reflection
        try {
            f.set(o, null);
            assertTrue(canBeNull, f + " cannot be set to null");
        } catch (NullPointerException e) {
            assertFalse(canBeNull, f + " should allow be set to null");
        }
        // test method handle, i.e. putfield bytecode behavior
        try {
            MethodHandle mh = LOOKUP.findSetter(c, f.getName(), f.getType());
            mh.invoke(o, null);
            assertTrue(canBeNull, f + " cannot be set to null");
        } catch (NullPointerException e) {
            assertFalse(canBeNull, f + " should allow be set to null");
        }
        // test var handle
        try {
            VarHandle vh = LOOKUP.findVarHandle(c, f.getName(), f.getType());
            vh.set(o, null);
            assertTrue(canBeNull, f + " cannot be set to null");
        } catch (NullPointerException e) {
            assertFalse(canBeNull, f + " should allow be set to null");
        }
    }

    static void ensureImmutable(Field f, Object o) throws Throwable {
        Class<?> c = f.getDeclaringClass();
        assertTrue(Modifier.isFinal(f.getModifiers()));
        assertFalse(Modifier.isStatic(f.getModifiers()));
        Object v = f.get(o);
        // test Field::set
        try {
            f.set(o, v);
            throw new RuntimeException(f + " should be immutable");
        } catch (IllegalAccessException e) {
        }

        // test method handle, i.e. putfield bytecode behavior
        try {
            MethodHandle mh = LOOKUP.findSetter(c, f.getName(), f.getType());
            mh.invoke(o, v);
            throw new RuntimeException(f + " should be immutable");
        } catch (IllegalAccessException e) {
        }
        // test var handle
        try {
            VarHandle vh = LOOKUP.findVarHandle(c, f.getName(), f.getType());
            vh.set(o, v);
            throw new RuntimeException(f + " should be immutable");
        } catch (UnsupportedOperationException e) {
        }
    }

    /*
     * Test setting a field of a primitive class to a new value.
     * The field must be flattenable but may or may not be flattened.
     */
    static void setValueField(Class<?> c, String name, Object obj, Object value) throws Throwable {
        setField(c, name, obj, value, true);
    }

    /*
     * Test Field::set, MethodHandle::set on a method handle of a field
     * and VarHandle::compareAndSet and compareAndExchange.
     */
    static void setField(Class<?> c, String name, Object obj, Object value, boolean isValue) throws Throwable {
        Field f = c.getDeclaredField(name);
        boolean isStatic = Modifier.isStatic(f.getModifiers());
        assertTrue(f.getType().isPrimitiveClass());
        assertTrue(f.getType().isValueType() == isValue);
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

    static void setInstanceField(Field f, Object obj, Object value) throws Throwable {
        Object v = f.get(obj);
        // MethodHandle::invoke
        try {
            MethodHandle mh = LOOKUP.findSetter(f.getDeclaringClass(), f.getName(), f.getType());
            mh.invoke(obj, value);
            assertEquals(f.get(obj), value);
        } finally {
            f.set(obj, v);
        }

        // VarHandle tests
        VarHandle vh = LOOKUP.findVarHandle(f.getDeclaringClass(), f.getName(), f.getType());
        try {
            vh.set(obj, value);
            assertEquals(f.get(obj), value);
        } finally {
            f.set(obj, v);
        }

        try {
            assertTrue(vh.compareAndSet(obj, v, value));
            assertEquals(f.get(obj), value);
        } finally {
            f.set(obj, v);
        }

        try {
            assertEquals(vh.compareAndExchange(obj, v, value), v);
            assertEquals(f.get(obj), value);
        } finally {
            f.set(obj, v);
        }
    }

    static void setStaticField(Field f, Object value) throws Throwable {
        Object v = f.get(null);
        // MethodHandle::invoke
        try {
            MethodHandle mh = LOOKUP.findStaticSetter(f.getDeclaringClass(), f.getName(), f.getType());
            mh.invoke(f.getType().cast(value));
            assertEquals(f.get(null), value);
        } finally {
            f.set(null, v);
        }
        // VarHandle tests
        VarHandle vh = LOOKUP.findStaticVarHandle(f.getDeclaringClass(), f.getName(), f.getType());
        try {
            vh.set(f.getType().cast(value));
            assertEquals(f.get(null), value);
        } finally {
            f.set(null, v);
        }

        try {
            assertTrue(vh.compareAndSet(v, f.getType().cast(value)));
            assertEquals(f.get(null), value);
        } finally {
            f.set(null, v);
        }

        try {
            assertEquals(vh.compareAndExchange(v, f.getType().cast(value)), v);
            assertEquals(f.get(null), value);
        } finally {
            f.set(null, v);
        }
    }
}
