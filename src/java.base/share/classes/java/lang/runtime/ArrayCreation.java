/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.runtime;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.RuntimeType;
import java.util.Arrays;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.lang.invoke.*;
import sun.invoke.util.Wrapper;

import static java.lang.invoke.MethodHandles.*;

/**
 * Bootstrap methods for strictly-initialized array creation.
 */
public class ArrayCreation {

    private ArrayCreation() {}

    private static final Lookup LOOKUP = lookup();
    private static final MethodType MAKE_LARVAL_TYPE =
            MethodType.methodType(Object.class, RuntimeType.class, int.class, int.class);
    private static final MethodType MAKE_DEFAULT_TYPE =
            MethodType.methodType(Object.class, RuntimeType.class, int.class, int.class);
    private static final MethodType MAKE_FILLED_TYPE =
            MethodType.methodType(Object.class, RuntimeType.class, int.class, int.class, Object.class);
    private static final MethodType MAKE_COMPUTED_TYPE =
            MethodType.methodType(Object.class, RuntimeType.class, int.class, int.class, IntFunction.class);
    private static final MethodType MAKE_COPY_TYPE =
            MethodType.methodType(Object.class, RuntimeType.class, int.class, int.class, Object.class, int.class);

    private static boolean isDefault(RuntimeType<?> componentType, Object value) {
        Class<?> c = componentType.baseClass();
        if (c.isPrimitive()) {
            return Wrapper.forPrimitiveType(c).zero().equals(value);
        } else {
            return value == null && componentType.canCast(null);
        }
    }

    // makeLarval

    private static Object makeLarval(RuntimeType<?> componentType, int flags, int length) {
        return Array.newInstance(componentType.baseClass(), length);
    }

    // makeDefault

    private static Object makeDefault(RuntimeType<?> componentType, int flags, int length) {
        if (!componentType.baseClass().isPrimitive() && !componentType.canCast(null)) {
            throw new AssertionError("unsupported component type: " + componentType);
        }
        return Array.newInstance(componentType.baseClass(), length);
    }

    // makeFilled

    private static Object makeFilled(RuntimeType<?> componentType, int flags, int length, Object init) {
        Object[] result = (Object[]) Array.newInstance(componentType.baseClass(), length);
        Arrays.fill(result, init);
        return result;
    }

    private static Object makeFilled_byte(RuntimeType<?> componentType, int flags, int length, Object init) {
        byte[] result = new byte[length];
        Arrays.fill(result, (byte) init);
        return result;
    }

    private static Object makeFilled_short(RuntimeType<?> componentType, int flags, int length, Object init) {
        short[] result = new short[length];
        Arrays.fill(result, (short) init);
        return result;
    }

    private static Object makeFilled_int(RuntimeType<?> componentType, int flags, int length, Object init) {
        int[] result = new int[length];
        Arrays.fill(result, (int) init);
        return result;
    }

    private static Object makeFilled_long(RuntimeType<?> componentType, int flags, int length, Object init) {
        long[] result = new long[length];
        Arrays.fill(result, (long) init);
        return result;
    }

    private static Object makeFilled_float(RuntimeType<?> componentType, int flags, int length, Object init) {
        float[] result = new float[length];
        Arrays.fill(result, (float) init);
        return result;
    }

    private static Object makeFilled_double(RuntimeType<?> componentType, int flags, int length, Object init) {
        double[] result = new double[length];
        Arrays.fill(result, (double) init);
        return result;
    }

    private static Object makeFilled_boolean(RuntimeType<?> componentType, int flags, int length, Object init) {
        boolean[] result = new boolean[length];
        Arrays.fill(result, (boolean) init);
        return result;
    }

    private static Object makeFilled_char(RuntimeType<?> componentType, int flags, int length, Object init) {
        char[] result = new char[length];
        Arrays.fill(result, (char) init);
        return result;
    }

    // makeComputed

    private static Object makeComputed(RuntimeType<?> componentType, int flags,
                                         int length, IntFunction<?> func) {
        Object[] result = (Object[]) Array.newInstance(componentType.baseClass(), length);
        Arrays.setAll(result, func);
        return result;
    }

    private static Object makeComputed_byte(RuntimeType<?> componentType, int flags,
                                            int length, IntFunction<?> func) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = (byte) func.apply(i);
        }
        return result;
    }

    private static Object makeComputed_short(RuntimeType<?> componentType, int flags,
                                              int length, IntFunction<?> func) {
        short[] result = new short[length];
        for (int i = 0; i < length; i++) {
            result[i] = (short) func.apply(i);
        }
        return result;
    }

    private static Object makeComputed_int(RuntimeType<?> componentType, int flags,
                                          int length, IntFunction<?> func) {
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = (int) func.apply(i);
        }
        return result;
    }

    private static Object makeComputed_long(RuntimeType<?> componentType, int flags,
                                            int length, IntFunction<?> func) {
        long[] result = new long[length];
        for (int i = 0; i < length; i++) {
            result[i] = (long) func.apply(i);
        }
        return result;
    }

    private static Object makeComputed_float(RuntimeType<?> componentType, int flags,
                                              int length, IntFunction<?> func) {
        float[] result = new float[length];
        for (int i = 0; i < length; i++) {
            result[i] = (float) func.apply(i);
        }
        return result;
    }

    private static Object makeComputed_double(RuntimeType<?> componentType, int flags,
                                                int length, IntFunction<?> func) {
        double[] result = new double[length];
        for (int i = 0; i < length; i++) {
            result[i] = (double) func.apply(i);
        }
        return result;
    }

    private static Object makeComputed_boolean(RuntimeType<?> componentType, int flags,
                                                  int length, IntFunction<?> func) {
        boolean[] result = new boolean[length];
        for (int i = 0; i < length; i++) {
            result[i] = (boolean) func.apply(i);
        }
        return result;
    }

    private static Object makeComputed_char(RuntimeType<?> componentType, int flags,
                                            int length, IntFunction<?> func) {
        char[] result = new char[length];
        for (int i = 0; i < length; i++) {
            result[i] = (char) func.apply(i);
        }
        return result;
    }

    // makeCopy

    private static Object makeCopy(RuntimeType<?> componentType, int flags,
                                   int length, Object source, int start) {
        Object result = Array.newInstance(componentType.baseClass(), length);
        System.arraycopy(source, start, result, 0, length);
        return result;
    }

    // bootstraps

    /**
     * Bootstrap method to create an array filled with a given constant initial value.
     * Intended for use with {@code invokedynamic} call sites.
     *
     * @param lookup        Ignored
     * @param methodName    Ignored
     * @param type          MethodType for the MethodHandle: an {@code int} length,
     *                      if necessary, with an array return type
     * @param componentType RuntimeType representing the array component type
     * @param flags         Kind of array to create (always 0 for now)
     * @param length        Length of the array, or -1 for dynamic
     * @param init          Initial value
     * @return              a call site wrapping the array creation MethodHandle
     * @throws IllegalArgumentException if the bootstrap arguments are invalid
     *                                  or inconsistent
     * @throws NullPointerException if any argument is {@code null} or if any element
     *                              in the {@code getters} array is {@code null}
     * @throws Throwable if any exception is thrown during call site construction
     */
    public static CallSite constantFilled(Lookup lookup, String methodName,
                                          MethodType type, RuntimeType<?> componentType,
                                          int flags, int length, Object init) throws Throwable {
        flags = flags | Modifier.STRICT;
        MethodHandle factory;
        if (isDefault(componentType, init)) {
            factory = LOOKUP.findStatic(ArrayCreation.class, "makeDefault", MAKE_DEFAULT_TYPE);
            if (length < 0) {
                factory = insertArguments(factory, 0, componentType, flags);
            } else {
                factory = insertArguments(factory, 0, componentType, flags, length);
            }
        } else {
            String mname = "makeFilled";
            if (componentType.baseClass().isPrimitive()) {
                mname += "_" + componentType;
            }
            factory = LOOKUP.findStatic(ArrayCreation.class, mname, MAKE_FILLED_TYPE);
            if (length < 0) {
                factory = insertArguments(factory, 0, componentType, flags);
                factory = insertArguments(factory, 1, init);
            } else {
                factory = insertArguments(factory, 0, componentType, flags, length, init);
            }
        }
        return new ConstantCallSite(factory.asType(type));
    }

    /**
     * Bootstrap method to create an array filled with a dynamically-evaluated
     * initial value. Intended for use with {@code invokedynamic} call sites.
     *
     * @param lookup        Ignored
     * @param methodName    Ignored
     * @param type          MethodType for the MethodHandle: an {@code int} length,
     *                      if necessary, and an appropriately-typed initial value,
     *                      with an array return type
     * @param componentType RuntimeType representing the array component type
     * @param flags         Kind of array to create (always 0 for now)
     * @param length        Length of the array, or -1 for dynamic
     * @return              a call site wrapping the array creation MethodHandle
     * @throws IllegalArgumentException if the bootstrap arguments are invalid
     *                                  or inconsistent
     * @throws NullPointerException if any argument is {@code null} or if any element
     *                              in the {@code getters} array is {@code null}
     * @throws Throwable if any exception is thrown during call site construction
     */
    public static CallSite dynamicFilled(Lookup lookup, String methodName,
                                         MethodType type, RuntimeType<?> componentType,
                                         int flags, int length) throws Throwable {
        flags = flags | Modifier.STRICT;
        String mname = "makeFilled";
        if (componentType.baseClass().isPrimitive()) {
            mname += "_" + componentType;
        }
        MethodHandle factory = LOOKUP.findStatic(ArrayCreation.class, mname, MAKE_FILLED_TYPE);
        if (length  < 0) {
            factory = insertArguments(factory, 0, componentType, flags);
        } else {
            factory = insertArguments(factory, 0, componentType, flags, length);
        }
        return new ConstantCallSite(factory.asType(type));
    }

    /**
     * Bootstrap method to create an array filled with lazily-computed initial
     * values. Intended for use with {@code invokedynamic} call sites.
     *
     * @param lookup        Ignored
     * @param methodName    Ignored
     * @param type          MethodType for the MethodHandle: an {@code int} length,
     *                      if necessary, and an IntFunction producing initial values,
     *                      with an array return type
     * @param componentType RuntimeType representing the array component type
     * @param flags         Kind of array to create (always 0 for now)
     * @param length        Length of the array, or -1 for dynamic
     * @return              a call site wrapping the array creation MethodHandle
     * @throws IllegalArgumentException if the bootstrap arguments are invalid
     *                                  or inconsistent
     * @throws NullPointerException if any argument is {@code null} or if any element
     *                              in the {@code getters} array is {@code null}
     * @throws Throwable if any exception is thrown during call site construction
     */
    public static CallSite computed(Lookup lookup, String methodName,
                                    MethodType type, RuntimeType<?> componentType,
                                    int flags, int length) throws Throwable {
        flags = flags | Modifier.STRICT;
        String mname = "makeComputed";
        if (componentType.baseClass().isPrimitive()) {
            mname += "_" + componentType;
        }
        MethodHandle factory = LOOKUP.findStatic(ArrayCreation.class, mname, MAKE_COMPUTED_TYPE);
        if (length < 0) {
            factory = insertArguments(factory, 0, componentType, flags);
        } else {
            factory = insertArguments(factory, 0, componentType, flags, length);
        }
        return new ConstantCallSite(factory.asType(type));
    }

    /**
     * Bootstrap method to create an array filled with values copied from another
     * array. Intended for use with {@code invokedynamic} call sites.
     *
     * @param lookup        Ignored
     * @param methodName    Ignored
     * @param type          MethodType for the MethodHandle: an {@code int} length,
     *                      if necessary, and a source array of initial values,
     *                      with an array return type
     * @param componentType RuntimeType representing the array component type
     * @param flags         Kind of array to create (always 0 for now)
     * @param length        Length of the array, or -1 for dynamic
     * @return              a call site wrapping the array creation MethodHandle
     * @throws IllegalArgumentException if the bootstrap arguments are invalid
     *                                  or inconsistent
     * @throws NullPointerException if any argument is {@code null} or if any element
     *                              in the {@code getters} array is {@code null}
     * @throws Throwable if any exception is thrown during call site construction
     */
    public static CallSite copied(Lookup lookup, String methodName,
                                  MethodType type, RuntimeType<?> componentType,
                                  int flags, int length) throws Throwable {
        flags = flags | Modifier.STRICT;
        MethodHandle factory = LOOKUP.findStatic(ArrayCreation.class, "makeCopy", MAKE_COPY_TYPE);
        if (length < 0) {
            factory = insertArguments(factory, 0, componentType, flags);
            factory = insertArguments(factory, 2, 0);
        } else {
            factory = insertArguments(factory, 0, componentType, flags, length);
            factory = insertArguments(factory, 1, 0);
        }
        return new ConstantCallSite(factory.asType(type));
    }

    /**
     * Bootstrap method to create an array filled with values copied from another
     * array, starting at an offset. Intended for use with {@code invokedynamic}
     * call sites.
     *
     * @param lookup        Ignored
     * @param methodName    Ignored
     * @param type          MethodType for the MethodHandle: an {@code int} length,
     *                      if necessary, a source array of initial values, and
     *                      a start offset into the array, with an array return type
     * @param componentType RuntimeType representing the array component type
     * @param flags         Kind of array to create (always 0 for now)
     * @param length        Length of the array, or -1 for dynamic
     * @return              a call site wrapping the array creation MethodHandle
     * @throws IllegalArgumentException if the bootstrap arguments are invalid
     *                                  or inconsistent
     * @throws NullPointerException if any argument is {@code null} or if any element
     *                              in the {@code getters} array is {@code null}
     * @throws Throwable if any exception is thrown during call site construction
     */
    public static CallSite offsetCopied(Lookup lookup, String methodName,
                                        MethodType type, RuntimeType<?> componentType,
                                        int flags, int length) throws Throwable {
        flags = flags | Modifier.STRICT;
        MethodHandle factory = LOOKUP.findStatic(ArrayCreation.class, "makeCopy", MAKE_COPY_TYPE);
        if (length < 0) {
            factory = insertArguments(factory, 0, componentType, flags);
        } else {
            factory = insertArguments(factory, 0, componentType, flags, length);
        }
        return new ConstantCallSite(factory.asType(type));
    }

    /**
     * Bootstrap method to create an array initialized to an enumerated list of
     * constant values. Intended for use with {@code invokedynamic} call sites.
     *
     * @param lookup        Ignored
     * @param methodName    Ignored
     * @param type          MethodType for the MethodHandle: no parameters, with
     *                      an array return type
     * @param componentType RuntimeType representing the array component type
     * @param flags         Kind of array to create (always 0 for now)
     * @param values        Initial values
     * @return              a call site wrapping the array creation MethodHandle
     * @throws IllegalArgumentException if the bootstrap arguments are invalid
     *                                  or inconsistent
     * @throws NullPointerException if any argument is {@code null} or if any element
     *                              in the {@code getters} array is {@code null}
     * @throws Throwable if any exception is thrown during call site construction
     */
    public static CallSite constantEnumerated(Lookup lookup, String methodName,
                                              MethodType type, RuntimeType<?> componentType,
                                              int flags, Object... values) throws Throwable {
        flags = flags | Modifier.STRICT;
        MethodHandle factory = LOOKUP.findStatic(ArrayCreation.class, "makeCopy", MAKE_COPY_TYPE);
        Object src = values;
        if (componentType.baseClass().isPrimitive()) {
            // unbox values
            src = Array.newInstance(componentType.baseClass(), values.length);
            for (int i = 0; i < values.length; i++) {
                Array.set(src, i, values[i]);
            }
        }
        factory = insertArguments(factory, 0, componentType, flags, values.length, src, 0);
        return new ConstantCallSite(factory.asType(type));
    }

    /**
     * Bootstrap method to create an array initialized to an enumerated list of
     * dynamically-evaluated values. Intended for use with {@code invokedynamic}
     * call sites.
     *
     * @param lookup        Ignored
     * @param methodName    Ignored
     * @param type          MethodType for the MethodHandle: a parameter for each
     *                      array component, with an array return type
     * @param componentType RuntimeType representing the array component type
     * @param flags         Kind of array to create (always 0 for now)
     * @return              a call site wrapping the array creation MethodHandle
     * @throws IllegalArgumentException if the bootstrap arguments are invalid
     *                                  or inconsistent
     * @throws NullPointerException if any argument is {@code null} or if any element
     *                              in the {@code getters} array is {@code null}
     * @throws Throwable if any exception is thrown during call site construction
     */
    public static CallSite dynamicEnumerated(Lookup lookup, String methodName,
                                             MethodType type, RuntimeType<?> componentType,
                                             int flags) throws Throwable {
        flags = flags | Modifier.STRICT;
        Class<?> ccls = componentType.baseClass();
        Class<?> acls = ccls.arrayType();
        int arity = type.parameterCount();
        MethodHandle binaryId = dropArguments(identity(acls), 1, ccls);
        // binaryId type: (acls, ccls)->acls
        MethodHandle alloc = LOOKUP.findStatic(ArrayCreation.class, "makeLarval", MAKE_LARVAL_TYPE);
        alloc = insertArguments(alloc, 0, componentType, flags, arity);
        MethodHandle result = alloc.asType(MethodType.methodType(acls));
        // result type: ()->acls
        for (int i = 0; i < arity; i++) {
            MethodHandle setter = arrayElementSetter(acls);
            setter = insertArguments(setter, 1, i);
            setter = foldArguments(binaryId, setter);
            // setter type: (acls, ccls)->acls
            result = collectArguments(setter, 0, result);
            // result type: (..., ccls)->acls
        }
        return new ConstantCallSite(result.asType(type));
    }

}
