/*
 * Copyright (c) 2017, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test core reflection on value classes
 * @compile -XDenablePrimitiveClasses Reflection.java
 * @run junit/othervm -XX:+EnableValhalla Reflection
 */

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.NullRestricted;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

public class Reflection {
    @ImplicitlyConstructible
    static value class V {
        int x;
        V(int x) {
            this.x = x;
        }
    }

    @ImplicitlyConstructible
    static value class Value {
        @NullRestricted
        V v1;
        V v2;
        Value(V v1, V v2) {
            this.v1 = v1;
            this.v2 = v2;
        }

        static Value newValue(V v1, V v2) {
            return new Value(v1, v2);
        }
    }

    @Test
    void testNewInstance() throws Exception {
        V v = new V(10);
        Constructor<Value> ctor = Value.class.getDeclaredConstructor(V.class, V.class);
        Value o = ctor.newInstance(v, v);
        assertEquals(o.getClass(), Value.class);
    }

    @Test
    void testAccess() throws Exception {
        Field field = Value.class.getDeclaredField("v1");
        V v = new V(10);
        Value o = new Value(v, null);
        assertEquals(v, field.get(o));

        // accessible but no write access
        assertTrue(field.trySetAccessible());
        assertTrue(field.isAccessible());
        assertThrows(IllegalAccessException.class, () -> field.set(o, v));
    }

    @Test
    void testNullRestricted() throws Exception {
        Method m = Value.class.getDeclaredMethod("newValue", V.class, V.class);
        Throwable t = assertThrows(InvocationTargetException.class, () -> m.invoke(null, new Object[] {null, null}));
        assertEquals(NullPointerException.class, t.getCause().getClass());
    }

    static Stream<Arguments> arrays() {
        V v1 = new V(10);
        V v2 = new V(20);

        V[] varray = new V[] { v1, v2 };
        Value[] valuearray = new Value[] { new Value(v1, v2), new Value(v1, null)};

        return Stream.of(
                Arguments.of(V[].class, varray, false),
                Arguments.of(Value[].class, valuearray, false)
        );
    }

    /**
     * Setting the elements of an array.
     * NPE will be thrown if null is set on an element in a null-restricted value class array
     */
    @ParameterizedTest
    @MethodSource("arrays")
    public void testArrays(Class<?> arrayClass, Object[] array, boolean nullRestricted) {
        Class<?> componentType = arrayClass.getComponentType();
        assertTrue(arrayClass.isArray());
        Object[] newArray = (Object[]) Array.newInstance(componentType, array.length);
        assertTrue(newArray.getClass().getComponentType() == componentType);

        // set elements
        for (int i = 0; i < array.length; i++) {
            Array.set(newArray, i, array[i]);
        }
        for (int i = 0; i < array.length; i++) {
            Object o = Array.get(newArray, i);
            assertEquals(o, array[i]);
        }
        Arrays.setAll(newArray, i -> array[i]);

        for (int i = 0; i < newArray.length; i++) {
            // test nullable
            if (nullRestricted) {
                final int index = i;
                assertThrows(NullPointerException.class, () -> Array.set(newArray, index, null));
            } else {
                Array.set(newArray, i, null);
            }
        }
    }
}
