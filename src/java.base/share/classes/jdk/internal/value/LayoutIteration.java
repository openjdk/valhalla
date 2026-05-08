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

package jdk.internal.value;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import jdk.internal.access.JavaLangInvokeAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.Unsafe;
import sun.invoke.util.Wrapper;

/**
 * Iterates the layout elements of a value type.
 * <p>
 * In the long run, we should do this:
 * Iterate the layout, create a mask masking bytes used by Object/abstract value
 * class reference fields.  Do a byte-wise compare and get the mask of value
 * mismatch; if the mask's all clear, fine; if the mask has bits beyond our
 * mask, fail; otherwise, compare reference fields indicated by the mismatch
 * mask. There may be paddings to ignore, too, depends...
 */
public final class LayoutIteration {
    // Initializer in static initializers below, order dependent
    public static final ClassValue<List<MethodHandle>> ELEMENTS;

    /**
     * {@return a list of method handles accessing the basic elements}
     * Basic elements are 8 primitives and pointers (to identity or value objects).
     * Primitives and pointers are distinguished by the MH return types.
     * The MH types are {@code flatType -> fieldType}.
     *
     * @param flatType the class that has a flat layout
     * @return the accessors
     * @throws IllegalArgumentException if argument has no flat layout
     */
    public static List<MethodHandle> computeElementGetters(Class<?> flatType) {
        if (!ValueClass.isConcreteValueClass(flatType))
            throw new IllegalArgumentException(flatType + " cannot be flat");
        var sink = new Sink(flatType);
        iterateFields(U.valueHeaderSize(flatType), flatType, sink);
        return List.copyOf(sink.getters);
    }

    private static final class Sink {
        final Class<?> receiverType;
        final List<MethodHandle> getters = new ArrayList<>();

        Sink(Class<?> receiverType) {
            this.receiverType = receiverType;
        }

        void accept(long offsetNoHeader, Class<?> itemType) {
            Wrapper w = itemType.isPrimitive() ? Wrapper.forPrimitiveType(itemType) : Wrapper.OBJECT;
            var mh = MethodHandles.insertArguments(FIELD_GETTERS.get(w.ordinal()), 1, offsetNoHeader);
            assert mh.type() == MethodType.methodType(w.primitiveType(), Object.class);
            mh = JLIA.assertAsType(mh, MethodType.methodType(itemType, receiverType));
            getters.add(mh);
        }
    }

    // Sink is good for one to many mappings
    private static void iterateFields(long enclosingOffset, Class<?> currentClass, Sink sink) {
        assert ValueClass.isConcreteValueClass(currentClass) : currentClass + " cannot be flat";
        long memberOffsetDelta = enclosingOffset - U.valueHeaderSize(currentClass);
        for (Field f : currentClass.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()))
                continue;
            var type = f.getType();
            long memberOffset = U.objectFieldOffset(f) + memberOffsetDelta;
            if (!U.isFlatField(f)) {
                sink.accept(memberOffset, type);
            } else {
                if (U.hasNullMarker(f)) {
                    sink.accept(U.nullMarkerOffset(f) + memberOffsetDelta, byte.class);
                }
                iterateFields(memberOffset, type, sink);
            }
        }
    }

    private static boolean getBoolean(Object o, long offset) {
        return U.getBoolean(o, offset);
    }
    private static byte getByte(Object o, long offset) {
        return U.getByte(o, offset);
    }
    private static short getShort(Object o, long offset) {
        return U.getShort(o, offset);
    }
    private static char getCharacter(Object o, long offset) {
        return U.getChar(o, offset);
    }
    private static int getInteger(Object o, long offset) {
        return U.getInt(o, offset);
    }
    private static long getLong(Object o, long offset) {
        return U.getLong(o, offset);
    }
    private static float getFloat(Object o, long offset) {
        return U.getFloat(o, offset);
    }
    private static double getDouble(Object o, long offset) {
        return U.getDouble(o, offset);
    }
    public static Object getObject(Object o, long offset) {
        return U.getReference(o, offset);
    }

    private static final Unsafe U = Unsafe.getUnsafe();
    private static final JavaLangInvokeAccess JLIA = SharedSecrets.getJavaLangInvokeAccess();
    private static final List<MethodHandle> FIELD_GETTERS;
    static {
        MethodHandle[] fieldGetters = new MethodHandle[9];
        var lookup = MethodHandles.lookup();
        var type = MethodType.methodType(void.class, Object.class, long.class);
        try {
            for (Wrapper w : Wrapper.values()) {
                if (w != Wrapper.VOID) {
                    fieldGetters[w.ordinal()] = lookup.findStatic(LayoutIteration.class,
                            "get" + w.wrapperSimpleName(), type.changeReturnType(w.primitiveType()));
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new ExceptionInInitializerError(ex);
        }
        FIELD_GETTERS = List.of(fieldGetters);
        ELEMENTS = new ClassValue<>() {
            @Override
            protected List<MethodHandle> computeValue(Class<?> type) {
                return computeElementGetters(type);
            }
        };
    }

    private LayoutIteration() {}
}
