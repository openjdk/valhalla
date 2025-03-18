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

import jdk.internal.misc.Unsafe;
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
import java.util.function.BiFunction;

/*
 * @test
 * @summary Test atomic access modes on var handles for flattened values
 * @enablePreview
 * @modules java.base/jdk.internal.value java.base/jdk.internal.vm.annotation java.base/jdk.internal.misc
 * @run junit/othervm FlatVarHandleTest
 */
public class FlatVarHandleTest {

    static final Unsafe UNSAFE = Unsafe.getUnsafe();

    interface Pointable { }

    @ImplicitlyConstructible
    @LooselyConsistentValue
    static value class WeakPoint implements Pointable {
        int x,y;
        long x2 = 0L,y2 = 0L;
        long x3 = 0L,y3 = 0L;
        long x4 = 0L,y4 = 0L;
        long x5 = 0L,y5 = 0L;
        long x6 = 0L,y6 = 0L;
        WeakPoint(int i, int j) { x = i; y = j; }

        static WeakPoint[] makePoints(int len) {
            WeakPoint[] array = (WeakPoint[])ValueClass.newNullRestrictedArray(WeakPoint.class, len);
            for (int i = 0; i < len; ++i) {
                array[i] = new WeakPoint(i, i);
            }
            return array;
        }
    }

    static class WeakPointHolder {
        WeakPoint p_i = new WeakPoint(0, 0);
        static WeakPoint p_s = new WeakPoint(0, 0);
        @NullRestricted
        WeakPoint p_i_nr = new WeakPoint(0, 0);
        @NullRestricted
        static WeakPoint p_s_nr = new WeakPoint(0, 0);
    }

    static value class StrongPoint implements Pointable {
        short x,y;
        StrongPoint(short i, short j) { x = i; y = j; }

        static StrongPoint[] makePoints(int len) {
            StrongPoint[] array = (StrongPoint[])ValueClass.newNullableAtomicArray(StrongPoint.class, len);
            for (int i = 0; i < len; ++i) {
                array[i] = new StrongPoint((short)i, (short)i);
            }
            return array;
        }
    }

    static class StrongPointHolder {
        StrongPoint p_i = new StrongPoint((short)0, (short)0);
        static StrongPoint p_s = new StrongPoint((short)0, (short)0);
    }

    private static List<Arguments> fieldAccessProvider() {
        try {
            List<Field> fields = List.of(
                    WeakPointHolder.class.getDeclaredField("p_s"),
                    WeakPointHolder.class.getDeclaredField("p_i"),
                    WeakPointHolder.class.getDeclaredField("p_s_nr"),
                    WeakPointHolder.class.getDeclaredField("p_i_nr"),
                    StrongPointHolder.class.getDeclaredField("p_s"),
                    StrongPointHolder.class.getDeclaredField("p_i"));
            List<Arguments> arguments = new ArrayList<>();
            for (AccessMode accessMode : AccessMode.values()) {
                for (Field field : fields) {
                    boolean isStatic = (field.getModifiers() & Modifier.STATIC) != 0;
                    boolean isWeak = field.getDeclaringClass().equals(WeakPointHolder.class);
                    Object holder = null;
                    if (!isStatic) {
                        holder = isWeak ? new WeakPointHolder() : new StrongPointHolder();
                    }
                    BiFunction<Integer, Integer, Object> factory = isWeak ?
                            (i1, i2) -> new WeakPoint(i1, i2) :
                            (i1, i2) -> new StrongPoint((short)(int)i1, (short)(int)i2);
                    boolean supported = !field.getName().equals("p_i_nr");
                    arguments.add(Arguments.of(accessMode, holder, factory, field, supported));
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
    public void testFieldAccess(AccessMode accessMode, Object holder, BiFunction<Integer, Integer, Object> factory, Field field, boolean supported) throws Throwable {
        VarHandle varHandle = MethodHandles.lookup().unreflectVarHandle(field);
        if (varHandle.isAccessModeSupported(accessMode)) {
            assertTrue(isPlain(accessMode) || (supported && !isBitwise(accessMode) && !isNumeric(accessMode)));
            MethodHandle methodHandle = varHandle.toMethodHandle(accessMode);
            List<Object> arguments = new ArrayList<>();
            if (holder != null) {
                arguments.add(holder); // receiver
            }
            for (int i = arguments.size(); i < methodHandle.type().parameterCount(); i++) {
                arguments.add(factory.apply(i, i)); // add extra setter param
            }
            methodHandle.invokeWithArguments(arguments.toArray());
        } else {
            assertTrue(!supported || isBitwise(accessMode) || isNumeric(accessMode));
        }
    }

    private static List<Arguments> arrayAccessProvider() {
        List<Object[]> arrayObjects = List.of(
                WeakPoint.makePoints(10),
                new WeakPoint[10],
                StrongPoint.makePoints(10),
                new StrongPoint[10]);

        List<Arguments> arguments = new ArrayList<>();
        for (AccessMode accessMode : AccessMode.values()) {
            for (Object[] arrayObject : arrayObjects) {
                boolean isWeak = arrayObject.getClass().getComponentType().equals(WeakPoint.class);
                List<Class<?>> arrayTypes = List.of(
                        isWeak ? WeakPoint[].class : StrongPoint[].class, Pointable[].class, Object[].class);
                for (Class<?> arrayType : arrayTypes) {
                    BiFunction<Integer, Integer, Object> factory = isWeak ?
                            (i1, i2) -> new WeakPoint(i1, i2) :
                            (i1, i2) -> new StrongPoint((short)(int)i1, (short)(int)i2);
                    boolean supported = !ValueClass.isFlatArray(arrayObject) || !isWeak;
                    arguments.add(Arguments.of(accessMode, arrayObject, factory, arrayType, supported));
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
    public void testArrayAccess(AccessMode accessMode, Object[] arrayObject, BiFunction<Integer, Integer, Object> factory, Class<?> arrayType, boolean supported) throws Throwable {
        VarHandle varHandle = MethodHandles.arrayElementVarHandle(arrayType);
        if (varHandle.isAccessModeSupported(accessMode)) {
            assertTrue(!isBitwise(accessMode) && !isNumeric(accessMode));
            MethodHandle methodHandle = varHandle.toMethodHandle(accessMode);
            List<Object> arguments = new ArrayList<>();
            arguments.add(arrayObject); // array receiver
            arguments.add(0); // index
            for (int i = 2; i < methodHandle.type().parameterCount(); i++) {
                arguments.add(factory.apply(i, i)); // add extra setter param
            }
            try {
                methodHandle.invokeWithArguments(arguments.toArray());
            } catch (UnsupportedOperationException ex) {
                assertFalse(supported);
            }
        } else {
            assertTrue(isBitwise(accessMode) || isNumeric(accessMode));
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
