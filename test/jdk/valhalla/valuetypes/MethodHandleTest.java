/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test MethodHandle and VarHandle of value classes
 * @compile -XDenablePrimitiveClasses MethodHandleTest.java
 * @run junit/othervm -XX:+EnableValhalla MethodHandleTest
 */

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Stream;

import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.NullRestricted;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

public class MethodHandleTest {
    @ImplicitlyConstructible
    static value class Point {
        public int x;
        public int y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    @ImplicitlyConstructible
    static value class Line {
        @NullRestricted
        Point p1;
        @NullRestricted
        Point p2;

        Line(int x1, int y1, int x2, int y2) {
            this.p1 = new Point(x1, y1);
            this.p2 = new Point(x2, y2);
        }
    }

    static class Ref {
        @NullRestricted
        Point p;
        Line l;
        List<String> list;
        ValueOptional vo;

        Ref(Point p, Line l) {
            this.p = p;
            this.l = l;
        }
    }

    @ImplicitlyConstructible
    static value class Value {
        @NullRestricted
        Point p;
        @NullRestricted
        Line l;
        Ref r;
        String s;
        Value(Point p, Line l, Ref r, String s) {
            this.p = p;
            this.l = l;
            this.r = r;
            this.s = s;
        }
    }

    @ImplicitlyConstructible
    static value class ValueOptional {
        private Object o;
        public ValueOptional(Object o) {
            this.o = o;
        }
    }

    static value record ValueRecord(int i, String name) {}
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    static final Point P = new Point(1, 2);
    static final Line L = new Line(1, 2, 3, 4);
    static final Ref R = new Ref(P, null);
    static final Value V = new Value(P, L, R, "value");

    static Stream<Arguments> fields() {
        return Stream.of(
                // value class with int fields
                Arguments.of("MethodHandleTest$Point", P, new String[] { "x", "y"}),
                // value class whose fields are null-restricted and of value class
                Arguments.of( "MethodHandleTest$Line", L, new String[] { "p1", "p2"}),
                // identity class whose non-final fields are of value type,
                Arguments.of( "MethodHandleTest$Ref", R, new String[] {"p", "l", "list", "vo"})
        );
    }

    /**
     * Test MethodHandle invocation on the fields of a given class.
     * MethodHandle produced by Lookup::unreflectGetter, Lookup::findGetter,
     * Lookup::findVarHandle.
     */
    @ParameterizedTest
    @MethodSource("fields")
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

            if (c.isValue())
                ensureImmutable(f, o);
            // else
               // ensureNullable(f, o);
        }
    }

    static Stream<Arguments> arrays() {
        return Stream.of(
                Arguments.of(Point[].class, P),
                Arguments.of(Line[].class, L),
                Arguments.of(Value[].class, V)
        );
    }

    private static final int ARRAY_SIZE = 5;

    @ParameterizedTest
    @MethodSource("arrays")
    public void testArrayElementSetterAndGetter(Class<?> arrayClass, Object o) throws Throwable {
        Class<?> elementType = arrayClass.getComponentType();
        MethodHandle ctor = MethodHandles.arrayConstructor(arrayClass);
        Object[] array = (Object[])ctor.invoke(ARRAY_SIZE);
        testArrayElement(array, o, false);
    }

    private void testArrayElement(Object array, Object o, boolean nullRestricted) throws Throwable {
        MethodHandle setter = MethodHandles.arrayElementSetter(array.getClass());
        MethodHandle getter = MethodHandles.arrayElementGetter(array.getClass());
        for (int i=0; i < ARRAY_SIZE; i++) {
            setter.invoke(array, i, o);
        }
        for (int i=0; i < ARRAY_SIZE; i++) {
            Object v = (Object)getter.invoke(array, i);
            assertEquals(v, o);
        }
        // set an array element to null
        if (nullRestricted) {
            assertThrows(NullPointerException.class, () -> setter.invoke(array, 1, null));
        } else {
            setter.invoke(array, 1, null);
            assertNull(getter.invoke(array, 1));
        }
    }

    /*
     * Test setting the given field to null via method handle
     * and var handle.
     */
    static void ensureNullable(Field f, Object o, boolean nullRestricted) throws Throwable {
        Class<?> c = f.getDeclaringClass();
        assertFalse(Modifier.isFinal(f.getModifiers()));
        assertFalse(Modifier.isStatic(f.getModifiers()));

        assertThrows(IllegalAccessException.class, () -> LOOKUP.findSetter(c, f.getName(), f.getType()));
        MethodHandle mh = LOOKUP.unreflectSetter(f);
        VarHandle vh = LOOKUP.findVarHandle(c, f.getName(), f.getType());

        if (nullRestricted) {
            // test method handle, i.e. putfield bytecode behavior
            assertThrows(NullPointerException.class, () -> mh.invoke(o, null));
            // test var handle
            assertThrows(NullPointerException.class, () -> vh.set(o, null));
        } else {
            mh.invoke(o, null);
            vh.set(o, null);
        }
    }

    static void ensureImmutable(Field f, Object o) throws Throwable {
        Class<?> c = f.getDeclaringClass();
        assertTrue(Modifier.isFinal(f.getModifiers()));
        assertFalse(Modifier.isStatic(f.getModifiers()));
        assertTrue(f.trySetAccessible());

        Object v = f.get(o);

        assertThrows(IllegalAccessException.class, () -> LOOKUP.findSetter(c, f.getName(), f.getType()));
        assertThrows(IllegalAccessException.class, () -> LOOKUP.unreflectSetter(f));
        VarHandle vh = LOOKUP.findVarHandle(c, f.getName(), f.getType());

        // test var handle
        assertThrows(UnsupportedOperationException.class, () -> vh.set(o, v));
    }
}
