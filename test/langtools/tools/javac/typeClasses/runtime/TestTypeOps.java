/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.reflect.TypeContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.runtime.WitnessSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * @test
 * @modules java.base/jdk.internal.reflect
 * @summary Smoke test for witness support type operations
 * @run junit/othervm TestTypeOps
 */

public class TestTypeOps {

    @ParameterizedTest
    @MethodSource("primitivePairs")
    public void testSamePrimitive(Class<?> s, Class<?> t) {
        if (s.equals(t)) {
            assertSameType(s, t);
        } else {
            assertNotSameType(s, t);
        }
    }

    @Test
    public void testSameClass() {
        assertSameType(String.class, String.class);
        assertNotSameType(Integer.class, String.class);
    }

    @Test
    public void testSameArray() {
        assertSameType(arr(String.class), arr(String.class));
        assertNotSameType(arr(String.class), arr(Integer.class));
        assertNotSameType(String.class, arr(String.class));
        assertNotSameType(arr(String.class), String.class);
        assertSameType(arr(arr(String.class)), arr(arr(String.class)));
        assertNotSameType(arr(arr(String.class)), arr(arr(Integer.class)));
        assertNotSameType(arr(String.class), arr(arr(String.class)));
        assertNotSameType(arr(arr(String.class)), arr(String.class));
    }

    @Test
    public void testSameGenericSingle() {
        assertSameType(pt(List.class, String.class), pt(List.class, String.class));
        assertNotSameType(pt(List.class, String.class), pt(List.class, Integer.class));
        assertSameType(pt(List.class, arr(String.class)), pt(List.class, arr(String.class)));
        assertNotSameType(pt(List.class, arr(String.class)), pt(List.class, arr(Integer.class)));
        assertSameType(pt(List.class, pt(List.class, String.class)), pt(List.class, pt(List.class, String.class)));
        assertNotSameType(pt(List.class, pt(List.class, String.class)), pt(List.class, pt(List.class, Integer.class)));
        assertSameType(pt(List.class, pt(List.class, arr(String.class))), pt(List.class, pt(List.class, arr(String.class))));
        assertNotSameType(pt(List.class, pt(List.class, arr(String.class))), pt(List.class, pt(List.class, arr(Integer.class))));
        assertSameType(pt(List.class, arr(int.class)), pt(List.class, arr(int.class)));
        assertNotSameType(pt(List.class, arr(int.class)), pt(List.class, arr(float.class)));
    }

    @Test
    public void testSameGenericMulti() {
        assertSameType(pt(Map.class, String.class, Integer.class), pt(Map.class, String.class, Integer.class));
        assertNotSameType(pt(Map.class, String.class, Integer.class), pt(Map.class, Integer.class, String.class));
        assertSameType(pt(Map.class, arr(String.class), Integer.class), pt(Map.class, arr(String.class), Integer.class));
        assertNotSameType(pt(Map.class, arr(String.class), Integer.class), pt(Map.class, String.class, arr(Integer.class)));
        assertSameType(pt(Map.class, pt(List.class, String.class), pt(List.class, Integer.class)), pt(Map.class, pt(List.class, String.class), pt(List.class, Integer.class)));
        assertNotSameType(pt(Map.class, pt(List.class, String.class), pt(List.class, Integer.class)), pt(Map.class, pt(List.class, Integer.class), pt(List.class, String.class)));
        assertSameType(pt(Map.class, pt(List.class, String.class), pt(List.class, arr(Integer.class))), pt(Map.class, pt(List.class, String.class), pt(List.class, arr(Integer.class))));
        assertNotSameType(pt(Map.class, pt(List.class, String.class), pt(List.class, arr(Integer.class))), pt(Map.class, pt(List.class, arr(String.class)), pt(List.class, Integer.class)));
        assertSameType(pt(Map.class, arr(int.class), arr(float.class)), pt(Map.class, arr(int.class), arr(float.class)));
        assertNotSameType(pt(Map.class, arr(int.class), arr(float.class)), pt(Map.class, arr(float.class), arr(int.class)));
    }

    @ParameterizedTest
    @MethodSource("primitivePairs")
    public void testSubPrimitive(Class<?> s, Class<?> t) {
        if (t.isAssignableFrom(s)) {
            assertSubtype(s, t);
        } else {
            assertNotSubtype(s, t);
        }
    }

    @Test
    public void testSubClass() {
        assertSubtype(String.class, Object.class);
        assertNotSubtype(Object.class, String.class);
        assertSubtype(String.class, Serializable.class);
        assertNotSubtype(Serializable.class, String.class);
    }

    @Test
    public void testSubArray() {
        for (Class<?> elem : List.of(Number.class, Integer.class)) {
            assertSubtype(arr(elem), arr(Number.class));
            assertSubtype(arr(elem), arr(Object.class));
            assertNotSubtype(arr(elem), arr(String.class));
            assertNotSubtype(arr(elem), Number.class);
            assertNotSubtype(elem, arr(Number.class));
            assertSubtype(arr(arr(elem)), arr(arr(Number.class)));
            assertSubtype(arr(arr(elem)), arr(arr(Object.class)));
            assertNotSubtype(arr(arr(elem)), arr(arr(String.class)));
            assertNotSubtype(arr(arr(elem)), arr(Number.class));
            assertNotSubtype(arr(elem), arr(arr(Number.class)));
            assertSubtype(arr(elem), Serializable.class);
            assertSubtype(arr(elem), Cloneable.class);
        }
    }

    @Test
    public void testSubGenericSingle() {
        for (Class<?> list : List.of(List.class, ArrayList.class)) {
            assertSubtype(pt(list, String.class), pt(List.class, String.class));
            assertNotSubtype(pt(list, String.class), pt(List.class, Integer.class));
            assertSubtype(pt(list, arr(String.class)), pt(List.class, arr(String.class)));
            assertNotSubtype(pt(list, arr(String.class)), pt(List.class, arr(Integer.class)));
            assertSubtype(pt(list, pt(List.class, String.class)), pt(List.class, pt(List.class, String.class)));
            assertNotSubtype(pt(list, pt(List.class, String.class)), pt(List.class, pt(List.class, Integer.class)));
            assertSubtype(pt(list, pt(List.class, arr(String.class))), pt(List.class, pt(List.class, arr(String.class))));
            assertNotSubtype(pt(list, pt(List.class, arr(String.class))), pt(List.class, pt(List.class, arr(Integer.class))));
            assertSubtype(pt(list, arr(int.class)), pt(List.class, arr(int.class)));
            assertNotSubtype(pt(list, arr(int.class)), pt(List.class, arr(float.class)));
        }
    }

    @Test
    public void testSubGenericMulti() {
        for (Class<?> map : List.of(Map.class, HashMap.class)) {
            assertSubtype(pt(map, String.class, Integer.class), pt(Map.class, String.class, Integer.class));
            assertNotSubtype(pt(map, String.class, Integer.class), pt(Map.class, Integer.class, String.class));
            assertSubtype(pt(map, arr(String.class), Integer.class), pt(Map.class, arr(String.class), Integer.class));
            assertNotSubtype(pt(map, arr(String.class), Integer.class), pt(Map.class, String.class, arr(Integer.class)));
            assertSubtype(pt(map, pt(List.class, String.class), pt(List.class, Integer.class)), pt(Map.class, pt(List.class, String.class), pt(List.class, Integer.class)));
            assertNotSubtype(pt(map, pt(List.class, String.class), pt(List.class, Integer.class)), pt(Map.class, pt(List.class, Integer.class), pt(List.class, String.class)));
            assertSubtype(pt(map, pt(List.class, String.class), pt(List.class, arr(Integer.class))), pt(Map.class, pt(List.class, String.class), pt(List.class, arr(Integer.class))));
            assertNotSubtype(pt(map, pt(List.class, String.class), pt(List.class, arr(Integer.class))), pt(Map.class, pt(List.class, arr(String.class)), pt(List.class, Integer.class)));
            assertSubtype(pt(map, arr(int.class), arr(float.class)), pt(Map.class, arr(int.class), arr(float.class)));
            assertNotSubtype(pt(map, arr(int.class), arr(float.class)), pt(Map.class, arr(float.class), arr(int.class)));
        }
    }

    void assertSameType(Type s, Type t) {
        System.err.println(format(s, t, "=="));
        assertTrue(new TypeContext().isSameType(s, t), format(s, t, "!="));
    }

    void assertNotSameType(Type s, Type t) {
        System.err.println(format(s, t, "!="));
        assertFalse(new TypeContext().isSameType(s, t), format(s, t, "=="));
    }

    void assertSubtype(Type s, Type t) {
        System.err.println(format(s, t, "<:"));
        assertTrue(new TypeContext().isSubType(s, t), format(s, t, "</:"));
    }

    void assertNotSubtype(Type s, Type t) {
        System.err.println(format(s, t, "</:"));
        assertFalse(new TypeContext().isSubType(s, t), format(s, t, "<:"));
    }

    Type pt(Class<?> baseType, Type... typeArgs) {
        return WitnessSupport.type(MethodHandles.lookup(), classSig(baseType, typeArgs));
    }

    Type arr(Type elementType) {
        return WitnessSupport.type(MethodHandles.lookup(), arraySig(elementType));
    }

    String format(Type s, Type t, String op) {
        return s.getTypeName() + " " + op + " " + t.getTypeName();
    }

    String typeSig(Type t) {
        if (t instanceof ParameterizedType pt) {
            return classSig((Class<?>)pt.getRawType(), pt.getActualTypeArguments());
        } else if (t instanceof GenericArrayType gat) {
            return arraySig(gat.getGenericComponentType());
        } else {
            return ((Class<?>)t).describeConstable().get().descriptorString();
        }
    }

    String classSig(Class<?> baseType, Type... typeArgs) {
        StringBuilder buf = new StringBuilder();
        String baseSig = baseType.describeConstable().get().descriptorString();
        buf.append(baseSig, 0, baseSig.length() - 1);
        if (typeArgs.length > 0) {
            buf.append("<");
            Stream.of(typeArgs).map(this::typeSig).forEach(buf::append);
            buf.append(">");
        }
        buf.append(";");
        return buf.toString();
    }

    String arraySig(Type elementType) {
        return "[" + typeSig(elementType);
    }

    private static Stream<Arguments> primitivePairs() {
        List<Class<?>> primitives = List.of(boolean.class, char.class, byte.class, short.class,
                int.class, float.class, long.class, double.class, void.class);
        return primitives.stream()
                .flatMap(a -> primitives.stream().flatMap(b -> Stream.of(Arguments.of(a, b))));
    }
}
