/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import static org.junit.jupiter.api.Assertions.*;

import jdk.internal.value.ValueClass;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.invoke.VarHandle.AccessMode;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/*
 * @test
 * @summary Test atomic access modes on var handles for flattened values
 * @enablePreview
 * @modules java.base/jdk.internal.value java.base/jdk.internal.vm.annotation
 * @run junit/othervm FlatVarHandleTest
 */
public class FlatVarHandleTest {

    interface Pointable { }

    @ImplicitlyConstructible
    @LooselyConsistentValue
    static value class Point implements Pointable {
        int x,y;
        Point(int i, int j) { x = i; y = j; }

        static Point[] makePoints(int len) {
            Point[] array = (Point[])ValueClass.newNullRestrictedArray(Point.class, len);
            for (int i = 0; i < len; ++i) {
                array[i] = new Point(i, i);
            }
            return array;
        }
    }

    static class PointHolder {
        Point p_i = new Point(0, 0);
        static Point p_s = new Point(0, 0);
        @NullRestricted
        Point p_i_nr = new Point(0, 0);
        @NullRestricted
        static Point p_s_nr = new Point(0, 0);
    }

    private static List<Arguments> fieldAccessProvider() {
        try {
            List<Field> fields = List.of(
                    PointHolder.class.getDeclaredField("p_s"),
                    PointHolder.class.getDeclaredField("p_i"),
                    PointHolder.class.getDeclaredField("p_s_nr"),
                    PointHolder.class.getDeclaredField("p_i_nr"));
            List<Arguments> arguments = new ArrayList<>();
            for (AccessMode accessMode : AccessMode.values()) {
                for (Field field : fields) {
                    arguments.add(Arguments.of(accessMode, field));
                }
            }
            return arguments;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /*
     * Verify that atomic access modes are not supported on flat fields.
     */
    @ParameterizedTest
    @MethodSource("fieldAccessProvider")
    public void testFieldAccess(AccessMode accessMode, Field field) throws Throwable {
        VarHandle varHandle = MethodHandles.lookup().unreflectVarHandle(field);
        boolean isStatic = (field.getModifiers() & Modifier.STATIC) != 0;
        boolean isNullRestricted = (field.isAnnotationPresent(NullRestricted.class));
        boolean isFlatField = !isStatic && isNullRestricted;
        boolean supported = (!isFlatField && !isNumeric(accessMode) && !isBitwise(accessMode)) || isPlain(accessMode);
        if (supported) {
            assertTrue(varHandle.isAccessModeSupported(accessMode));
            MethodHandle methodHandle = varHandle.toMethodHandle(accessMode);
            List<Object> arguments = new ArrayList<>();
            if (!isStatic) {
                arguments.add(new PointHolder()); // receiver
            }
            for (int i = arguments.size() ; i < methodHandle.type().parameterCount() ; i++) {
                arguments.add(new Point(i, i)); // add extra setter param
            }
            methodHandle.invokeWithArguments(arguments.toArray());
        } else {
            assertFalse(varHandle.isAccessModeSupported(accessMode));
        }
    }

    private static List<Arguments> arrayAccessProvider() {
        List<Point[]> arrayObjects = List.of(
                Point.makePoints(10),
                new Point[10]);
        List<Class<?>> arrayTypes = List.of(
                Point[].class, Pointable[].class, Object[].class);
        List<Arguments> arguments = new ArrayList<>();
        for (AccessMode accessMode : AccessMode.values()) {
            for (Point[] arrayObject : arrayObjects) {
                for (Class<?> arrayType : arrayTypes) {
                    arguments.add(Arguments.of(accessMode, arrayObject, arrayType));
                }
            }
        }
        return arguments;
    }

    /*
     * Verify that atomic access modes are not supported on flat array instances.
     */
    @ParameterizedTest
    @MethodSource("arrayAccessProvider")
    public void testArrayAccess(AccessMode accessMode, Point[] arrayObject, Class<?> arrayType) throws Throwable {
        VarHandle varHandle = MethodHandles.arrayElementVarHandle(arrayType);
        boolean supported = !isBitwise(accessMode) && !isNumeric(accessMode);
        if (supported) {
            assertTrue(varHandle.isAccessModeSupported(accessMode));
            MethodHandle methodHandle = varHandle.toMethodHandle(accessMode);
            List<Object> arguments = new ArrayList<>();
            arguments.add(arrayObject); // array receiver
            arguments.add(0); // index
            for (int i = 2 ; i < methodHandle.type().parameterCount() ; i++) {
                arguments.add(new Point(i, i)); // add extra setter param
            }
            try {
                methodHandle.invokeWithArguments(arguments.toArray());
                assertTrue(isPlain(accessMode) || !ValueClass.isFlatArray(arrayObject));
            } catch (IllegalArgumentException ex) {
                assertTrue(!isPlain(accessMode) && ValueClass.isFlatArray(arrayObject));
            }
        } else {
            assertFalse(varHandle.isAccessModeSupported(accessMode));
        }
    }

    boolean isBitwise(AccessMode accessMode) {
        return switch (accessMode) {
            case GET_AND_BITWISE_AND, GET_AND_BITWISE_AND_ACQUIRE,
                 GET_AND_BITWISE_AND_RELEASE, GET_AND_BITWISE_OR,
                 GET_AND_BITWISE_OR_ACQUIRE, GET_AND_BITWISE_OR_RELEASE,
                 GET_AND_BITWISE_XOR, GET_AND_BITWISE_XOR_ACQUIRE,
                 GET_AND_BITWISE_XOR_RELEASE -> true;
            default -> false;
        };
    }

    boolean isNumeric(AccessMode accessMode) {
        return switch (accessMode) {
            case GET_AND_ADD, GET_AND_ADD_ACQUIRE, GET_AND_ADD_RELEASE -> true;
            default -> false;
        };
    }

    boolean isPlain(AccessMode accessMode) {
        return switch (accessMode) {
            case GET, SET -> true;
            default -> false;
        };
    }
}
