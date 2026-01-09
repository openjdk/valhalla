/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.javac.PreviewFeature;
import jdk.internal.javac.PreviewFeature.Feature;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.function.IntFunction;
import java.lang.invoke.*;
import sun.invoke.util.Wrapper;

import static java.lang.invoke.MethodHandles.*;

/**
 * Factory and bootstrap methods for strictly-initialized array creation.
 */
@PreviewFeature(reflective = true, feature = Feature.VALUE_OBJECTS)
public class ArrayCreation {

    private ArrayCreation() {}

    /* FACTORIES */

    /**
     * Create an empty array with the given component type and modifiers.
     *
     * @param componentType array component type
     * @param modifiers array modifiers
     * @return a new array
     * @throws IllegalArgumentException if componentType is {@link Void#TYPE}
     */
    public static Object empty(Class<?> componentType, int modifiers) {
        Object arr = alloc(componentType, modifiers, 0);
        return finish(arr, componentType, modifiers, 0);
    }

    /**
     * Create an array with the given component type and modifiers, with every
     * component initialized to {@code initial}.
     *
     * @param componentType array component type
     * @param modifiers array modifiers
     * @param length array length
     * @param initial initial value for each component
     * @return a new array
     * @throws IllegalArgumentException if componentType is {@link Void#TYPE}
     * @throws NegativeArraySizeException if {@code length < 0}
     */
    public static Object filled(Class<?> componentType, int modifiers,
                                int length, Object initial) {
        Object arr = alloc(componentType, modifiers, length);
        if (!isDefault(componentType, initial)) {
            if (arr instanceof Object[]) {
                initFilled(arr, componentType, modifiers, length, initial);
            } else if (arr instanceof int[]) {
                initFilled_int(arr, componentType, modifiers, length, initial);
            } else if (arr instanceof long[]) {
                initFilled_long(arr, componentType, modifiers, length, initial);
            } else if (arr instanceof float[]) {
                initFilled_float(arr, componentType, modifiers, length, initial);
            } else if (arr instanceof double[]) {
                initFilled_double(arr, componentType, modifiers, length, initial);
            } else if (arr instanceof byte[]) {
                initFilled_byte(arr, componentType, modifiers, length, initial);
            } else if (arr instanceof short[]) {
                initFilled_short(arr, componentType, modifiers, length, initial);
            } else if (arr instanceof char[]) {
                initFilled_char(arr, componentType, modifiers, length, initial);
            } else if (arr instanceof boolean[]) {
                initFilled_boolean(arr, componentType, modifiers, length, initial);
            } else {
                throw new AssertionError();
            }
        }
        return finish(arr, componentType, modifiers, length);
    }

    /**
     * Create an array with the given component type and modifiers, with initial
     * components provided by an {@code IntFunction}.
     *
     * @param componentType array component type
     * @param modifiers array modifiers
     * @param length array length
     * @param generator function to produce the initial value for each component
     * @return a new array
     * @throws IllegalArgumentException if componentType is {@link Void#TYPE}
     * @throws NegativeArraySizeException if {@code length < 0}
     */
    public static Object computed(Class<?> componentType, int modifiers,
                                  int length, IntFunction<?> generator) {
        Object arr = alloc(componentType, modifiers, length);
        if (arr instanceof Object[]) {
            initComputed(arr, componentType, modifiers, length, generator);
        } else if (arr instanceof int[]) {
            initComputed_int(arr, componentType, modifiers, length, generator);
        } else if (arr instanceof long[]) {
            initComputed_long(arr, componentType, modifiers, length, generator);
        } else if (arr instanceof float[]) {
            initComputed_float(arr, componentType, modifiers, length, generator);
        } else if (arr instanceof double[]) {
            initComputed_double(arr, componentType, modifiers, length, generator);
        } else if (arr instanceof byte[]) {
            initComputed_byte(arr, componentType, modifiers, length, generator);
        } else if (arr instanceof short[]) {
            initComputed_short(arr, componentType, modifiers, length, generator);
        } else if (arr instanceof char[]) {
            initComputed_char(arr, componentType, modifiers, length, generator);
        } else if (arr instanceof boolean[]) {
            initComputed_boolean(arr, componentType, modifiers, length, generator);
        } else {
            throw new AssertionError();
        }
        return finish(arr, componentType, modifiers, length);
    }

    /**
     * Create an array with the given component type and modifiers, with initial
     * components provided by the first {@code length} components of
     * {@code sourceArray}.
     *
     * @param componentType array component type
     * @param modifiers array modifiers
     * @param length array length
     * @param sourceArray array to copy from
     * @return a new array
     * @throws IllegalArgumentException if componentType is {@link Void#TYPE}
     * @throws NegativeArraySizeException if {@code length < 0}
     */
    public static Object copied(Class<?> componentType, int modifiers,
                                int length, Object sourceArray) {
        return Array.newInstance(componentType, modifiers, length, sourceArray, 0);
    }

    /**
     * Create an array with the given component type and modifiers, with initial
     * components provided by {@code sourceArray}, from index {@code offset}
     * (inclusive) to index {@code offset + length} (exclusive).
     *
     * @param componentType array component type
     * @param modifiers array modifiers
     * @param length array length
     * @param sourceArray array to copy from
     * @param offset first index in {@code sourceArray} to copy from
     * @return a new array
     * @throws IllegalArgumentException if componentType is {@link Void#TYPE}
     * @throws NegativeArraySizeException if {@code length < 0}
     */
    public static Object offsetCopied(Class<?> componentType, int modifiers,
                                      int length, Object sourceArray, int offset) {
        return Array.newInstance(componentType, modifiers, length,
                                 sourceArray, offset);
    }

    /**
     * Create array of length 1 with the given component type and modifiers,
     * initialized with the given component value.
     *
     * @param componentType array component type
     * @param modifiers array modifiers
     * @param v0 initial value for the component at index 0
     * @return a new array
     * @throws IllegalArgumentException if componentType is {@link Void#TYPE}
     */
    public static Object enumerated(Class<?> componentType, int modifiers,
                                    Object v0) {
        Object arr = alloc(componentType, modifiers, 1);
        Array.set(arr, 0, v0);
        return finish(arr, componentType, modifiers, 1);
    }

    /**
     * Create array of length 2 with the given component type and modifiers,
     * initialized with the given component values.
     *
     * @param componentType array component type
     * @param modifiers array modifiers
     * @param v0 initial value for the component at index 0
     * @param v1 initial value for the component at index 1
     * @return a new array
     * @throws IllegalArgumentException if componentType is {@link Void#TYPE}
     */
    public static Object enumerated(Class<?> componentType, int modifiers,
                                    Object v0, Object v1) {
        Object arr = alloc(componentType, modifiers, 2);
        Array.set(arr, 0, v0);
        Array.set(arr, 1, v1);
        return finish(arr, componentType, modifiers, 2);
    }

    /**
     * Create array of length 3 with the given component type and modifiers,
     * initialized with the given component values.
     *
     * @param componentType array component type
     * @param modifiers array modifiers
     * @param v0 initial value for the component at index 0
     * @param v1 initial value for the component at index 1
     * @param v2 initial value for the component at index 2
     * @return a new array
     * @throws IllegalArgumentException if componentType is {@link Void#TYPE}
     */
    public static Object enumerated(Class<?> componentType, int modifiers,
                                    Object v0, Object v1, Object v2) {
        Object arr = alloc(componentType, modifiers, 3);
        Array.set(arr, 0, v0);
        Array.set(arr, 1, v1);
        Array.set(arr, 2, v2);
        return finish(arr, componentType, modifiers, 3);
    }

    /**
     * Create array of length 4 with the given component type and modifiers,
     * initialized with the given component values.
     *
     * @param componentType array component type
     * @param modifiers array modifiers
     * @param v0 initial value for the component at index 0
     * @param v1 initial value for the component at index 1
     * @param v2 initial value for the component at index 2
     * @param v3 initial value for the component at index 3
     * @return a new array
     * @throws IllegalArgumentException if componentType is {@link Void#TYPE}
     */
    public static Object enumerated(Class<?> componentType, int modifiers,
                                    Object v0, Object v1, Object v2, Object v3) {
        Object arr = alloc(componentType, modifiers, 4);
        Array.set(arr, 0, v0);
        Array.set(arr, 1, v1);
        Array.set(arr, 2, v2);
        Array.set(arr, 3, v3);
        return finish(arr, componentType, modifiers, 4);
    }

    /**
     * Create array of length 5 with the given component type and modifiers,
     * initialized with the given component values.
     *
     * @param componentType array component type
     * @param modifiers array modifiers
     * @param v0 initial value for the component at index 0
     * @param v1 initial value for the component at index 1
     * @param v2 initial value for the component at index 2
     * @param v3 initial value for the component at index 3
     * @param v4 initial value for the component at index 4
     * @return a new array
     * @throws IllegalArgumentException if componentType is {@link Void#TYPE}
     */
    public static Object enumerated(Class<?> componentType, int modifiers,
                                    Object v0, Object v1, Object v2, Object v3,
                                    Object v4) {
        Object arr = alloc(componentType, modifiers, 5);
        Array.set(arr, 0, v0);
        Array.set(arr, 1, v1);
        Array.set(arr, 2, v2);
        Array.set(arr, 3, v3);
        Array.set(arr, 4, v4);
        return finish(arr, componentType, modifiers, 5);
    }

    /* BOOTSTRAPS */

    /**
     * Bootstrap method to create an empty array. Intended for use with
     * {@code invokedynamic} call sites.
     *
     * @param lookup        Ignored
     * @param methodName    Ignored
     * @param type          MethodType for the MethodHandle: no parameters, with
     *                      an array return type
     * @param componentType Class representing the array component type
     * @param modifiers     Array modifiers
     * @return              a call site wrapping the array creation MethodHandle
     * @throws IllegalArgumentException if the bootstrap arguments are invalid
     *                                  or inconsistent
     * @throws NullPointerException if any argument is {@code null}
     * @throws Throwable if any exception is thrown during call site construction
     */
    public static CallSite empty(Lookup lookup, String methodName,
                                 MethodType type, Class<?> componentType,
                                 int modifiers) throws Throwable {
        var factory = foldArguments(FINISH_MH, ALLOC_MH);
        factory = insertArguments(factory, 0, componentType, modifiers, 0);
        return new ConstantCallSite(factory.asType(type));
    }

    /**
     * Bootstrap method to create an array filled with a given constant initial
     * value. Intended for use with {@code invokedynamic} call sites.
     *
     * @param lookup        Ignored
     * @param methodName    Ignored
     * @param type          MethodType for the MethodHandle: an {@code int} length,
     *                      if necessary, with an array return type
     * @param componentType Class representing the array component type
     * @param modifiers     Array modifiers
     * @param length        Length of the array, or -1 for dynamic
     * @param initial       Initial value
     * @return              a call site wrapping the array creation MethodHandle
     * @throws IllegalArgumentException if the bootstrap arguments are invalid
     *                                  or inconsistent
     * @throws NullPointerException if any argument is {@code null}
     * @throws Throwable if any exception is thrown during call site construction
     */
    public static CallSite constantFilled(Lookup lookup, String methodName,
                                          MethodType type, Class<?> componentType,
                                          int modifiers, int length, Object initial) throws Throwable {
        MethodHandle factory;
        if (isDefault(componentType, initial)) {
            factory = foldArguments(FINISH_MH, ALLOC_MH);
            factory = insertArguments(factory, 0, componentType, modifiers);
            if (length >= 0) {
                factory = insertArguments(factory, 0, length);
            }
        } else {
            String initName = "initFilled";
            if (componentType.isPrimitive()) {
                initName += "_" + componentType;
            }
            var init = LOOKUP.findStatic(ArrayCreation.class, initName, INIT_FILLED_TYPE);
            var alloc = dropArguments(ALLOC_MH, ALLOC_TYPE.parameterCount(),
                                      Object.class);
            var finish = dropArguments(FINISH_MH, FINISH_TYPE.parameterCount(),
                                       Object.class);
            factory = foldArguments(foldArguments(finish, init), alloc);
            factory = insertArguments(factory, 0, componentType, modifiers);
            factory = insertArguments(factory, 1, initial);
            if (length >= 0) {
                factory = insertArguments(factory, 0, length);
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
     * @param componentType Class representing the array component type
     * @param modifiers     Array modifiers
     * @param length        Length of the array, or -1 for dynamic
     * @return              a call site wrapping the array creation MethodHandle
     * @throws IllegalArgumentException if the bootstrap arguments are invalid
     *                                  or inconsistent
     * @throws NullPointerException if any argument is {@code null}
     * @throws Throwable if any exception is thrown during call site construction
     */
    public static CallSite dynamicFilled(Lookup lookup, String methodName,
                                         MethodType type, Class<?> componentType,
                                         int modifiers, int length) throws Throwable {
        String initName = "initFilled";
        if (componentType.isPrimitive()) {
            initName += "_" + componentType;
        }
        var init = LOOKUP.findStatic(ArrayCreation.class, initName, INIT_FILLED_TYPE);
        var alloc = dropArguments(ALLOC_MH, ALLOC_TYPE.parameterCount(),
                                  Object.class);
        var finish = dropArguments(FINISH_MH, FINISH_TYPE.parameterCount(),
                                   Object.class);
        var factory = foldArguments(foldArguments(finish, init), alloc);
        factory = insertArguments(factory, 0, componentType, modifiers);
        if (length >= 0) {
            factory = insertArguments(factory, 0, length);
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
     * @param componentType Class representing the array component type
     * @param modifiers     Array modifiers
     * @param length        Length of the array, or -1 for dynamic
     * @return              a call site wrapping the array creation MethodHandle
     * @throws IllegalArgumentException if the bootstrap arguments are invalid
     *                                  or inconsistent
     * @throws NullPointerException if any argument is {@code null}
     * @throws Throwable if any exception is thrown during call site construction
     */
    public static CallSite computed(Lookup lookup, String methodName,
                                    MethodType type, Class<?> componentType,
                                    int modifiers, int length) throws Throwable {
        String initName = "initComputed";
        if (componentType.isPrimitive()) {
            initName += "_" + componentType;
        }
        var init = LOOKUP.findStatic(ArrayCreation.class, initName, INIT_COMPUTED_TYPE);
        var alloc = dropArguments(ALLOC_MH, ALLOC_TYPE.parameterCount(),
                                  IntFunction.class);
        var finish = dropArguments(FINISH_MH, FINISH_TYPE.parameterCount(),
                                   IntFunction.class);
        var factory = foldArguments(foldArguments(finish, init), alloc);
        factory = insertArguments(factory, 0, componentType, modifiers);
        if (length >= 0) {
            factory = insertArguments(factory, 0, length);
        }
        return new ConstantCallSite(factory.asType(type));
    }

    /**
     * Bootstrap method to create an array filled with values copied from
     * another array. Intended for use with {@code invokedynamic} call sites.
     *
     * @param lookup        Ignored
     * @param methodName    Ignored
     * @param type          MethodType for the MethodHandle: an {@code int} length,
     *                      if necessary, and a source array of initial values,
     *                      with an array return type
     * @param componentType Class representing the array component type
     * @param modifiers     Array modifiers
     * @param length        Length of the array, or -1 for dynamic
     * @return              a call site wrapping the array creation MethodHandle
     * @throws IllegalArgumentException if the bootstrap arguments are invalid
     *                                  or inconsistent
     * @throws NullPointerException if any argument is {@code null}
     * @throws Throwable if any exception is thrown during call site construction
     */
    public static CallSite copied(Lookup lookup, String methodName,
                                  MethodType type, Class<?> componentType,
                                  int modifiers, int length) throws Throwable {
        var factory = insertArguments(COPY_MH, 0, componentType, modifiers);
        factory = insertArguments(factory, 2, 0);
        if (length >= 0) {
            factory = insertArguments(factory, 0, length);
        }
        return new ConstantCallSite(factory.asType(type));
    }

    /**
     * Bootstrap method to create an array filled with values copied from
     * another array, starting at an offset. Intended for use with
     * {@code invokedynamic} call sites.
     *
     * @param lookup        Ignored
     * @param methodName    Ignored
     * @param type          MethodType for the MethodHandle: an {@code int} length,
     *                      if necessary, a source array of initial values, and
     *                      a start offset into the array, with an array return type
     * @param componentType Class representing the array component type
     * @param modifiers     Array modifiers
     * @param length        Length of the array, or -1 for dynamic
     * @return              a call site wrapping the array creation MethodHandle
     * @throws IllegalArgumentException if the bootstrap arguments are invalid
     *                                  or inconsistent
     * @throws NullPointerException if any argument is {@code null}
     * @throws Throwable if any exception is thrown during call site construction
     */
    public static CallSite offsetCopied(Lookup lookup, String methodName,
                                        MethodType type, Class<?> componentType,
                                        int modifiers, int length) throws Throwable {
        var factory = insertArguments(COPY_MH, 0, componentType, modifiers);
        if (length >= 0) {
            factory = insertArguments(factory, 0, length);
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
     * @param componentType Class representing the array component type
     * @param modifiers     Array modifiers
     * @param values        Initial values
     * @return              a call site wrapping the array creation MethodHandle
     * @throws IllegalArgumentException if the bootstrap arguments are invalid
     *                                  or inconsistent
     * @throws NullPointerException if any argument is {@code null}
     * @throws Throwable if any exception is thrown during call site construction
     */
    public static CallSite constantEnumerated(Lookup lookup, String methodName,
                                              MethodType type, Class<?> componentType,
                                              int modifiers, Object... values) throws Throwable {
        Object src = values;
        if (componentType.isPrimitive()) {
            // unbox values
            src = Array.newInstance(componentType, values.length);
            for (int i = 0; i < values.length; i++) {
                Array.set(src, i, values[i]);
            }
        }
        var factory = insertArguments(COPY_MH, 0, componentType, modifiers,
                                      values.length, src, 0);
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
     * @param componentType Class representing the array component type
     * @param modifiers     Array modifiers
     * @return              a call site wrapping the array creation MethodHandle
     * @throws IllegalArgumentException if the bootstrap arguments are invalid
     *                                  or inconsistent
     * @throws NullPointerException if any argument is {@code null}
     * @throws Throwable if any exception is thrown during call site construction
     */
    public static CallSite dynamicEnumerated(Lookup lookup, String methodName,
                                             MethodType type, Class<?> componentType,
                                             int modifiers) throws Throwable {
        Class<?> acls = componentType.arrayType();
        int arity = type.parameterCount();
        List<Class<?>> initArgs = Collections.nCopies(arity, Object.class);
        MethodHandle binaryId = dropArguments(identity(acls), 1, componentType);
        // binaryId type: (acls, comp)->acls

       var init = MethodHandles.empty(MethodType.methodType(void.class, acls));
        for (int i = arity - 1; i >= 0; i--) {
            var setter = arrayElementSetter(acls);
            setter = insertArguments(setter, 1, i);
            setter = foldArguments(binaryId, setter);
            // setter type: (acls, comp)->acls
            init = collectArguments(init, 0, setter);
            // init type: (acls, comp, ...)->void
        }
        init = init.asType(init.type().generic().changeReturnType(void.class));
        init = dropArguments(init, 1, Class.class, int.class, int.class);
        // init type: (Object, Class, int, int, Object, ...)->void

        var alloc = dropArguments(ALLOC_MH, ALLOC_TYPE.parameterCount(), initArgs);
        var finish = dropArguments(FINISH_MH, FINISH_TYPE.parameterCount(), initArgs);
        var factory = foldArguments(foldArguments(finish, init), alloc);
        factory = insertArguments(factory, 0, componentType, modifiers, arity);
        return new ConstantCallSite(factory.asType(type));
    }

    /* SUPPORT CODE */

    private static boolean isDefault(Class<?> componentType, Object value) {
        if (componentType.isPrimitive()) {
            return Wrapper.forPrimitiveType(componentType).zero().equals(value);
        } else {
            return value == null;
        }
    }

    private static final Lookup LOOKUP = lookup();

    // copy

    private static final MethodType COPY_TYPE =
            MethodType.methodType(Object.class, Class.class, int.class, int.class,
                                  Object.class, int.class);

    private static final MethodHandle COPY_MH;
    static {
        try {
            COPY_MH = LOOKUP.findStatic(Array.class, "newInstance", COPY_TYPE);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // alloc

    private static final MethodType ALLOC_TYPE =
            MethodType.methodType(Object.class, Class.class, int.class, int.class);

    private static final MethodHandle ALLOC_MH;
    static {
        try {
            ALLOC_MH = LOOKUP.findStatic(ArrayCreation.class, "alloc", ALLOC_TYPE);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object alloc(Class<?> comp, int mods, int len) {
        return Array.newInstance(comp, len);
    }

    // finish

    private static final MethodType FINISH_TYPE =
            MethodType.methodType(Object.class, Object.class, Class.class, int.class, int.class);

    private static final MethodHandle FINISH_MH;
    static {
        try {
            FINISH_MH = LOOKUP.findStatic(ArrayCreation.class, "finish", FINISH_TYPE);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object finish(Object arr, Class<?> comp, int mods, int len) {
        if (mods == 0) {
            return arr;
        } else {
            return Array.newInstance(comp, mods, len, arr, 0);
        }
    }

    // initFilled

    private static final MethodType INIT_FILLED_TYPE =
            MethodType.methodType(void.class, Object.class, Class.class, int.class, int.class,
                                  Object.class);

    private static void initFilled(Object arr, Class<?> comp, int mods, int len,
                                     Object init) {
        Arrays.fill((Object[]) arr, init);
    }

    private static void initFilled_int(Object arr, Class<?> comp, int mods, int len,
                                         Object init) {
        Arrays.fill((int[]) arr, (int) init);
    }

    private static void initFilled_long(Object arr, Class<?> comp, int mods, int len,
                                          Object init) {
        Arrays.fill((long[]) arr, (long) init);
    }

    private static void initFilled_float(Object arr, Class<?> comp, int mods, int len,
                                          Object init) {
        Arrays.fill((float[]) arr, (float) init);
    }

    private static void initFilled_double(Object arr, Class<?> comp, int mods, int len,
                                            Object init) {
        Arrays.fill((double[]) arr, (double) init);
    }

    private static void initFilled_byte(Object arr, Class<?> comp, int mods, int len,
                                          Object init) {
        Arrays.fill((byte[]) arr, (byte) init);
    }

    private static void initFilled_short(Object arr, Class<?> comp, int mods, int len,
                                           Object init) {
        Arrays.fill((short[]) arr, (short) init);
    }

    private static void initFilled_char(Object arr, Class<?> comp, int mods, int len,
                                          Object init) {
        Arrays.fill((char[]) arr, (char) init);
    }

    private static void initFilled_boolean(Object arr, Class<?> comp, int mods, int len,
                                             Object init) {
        Arrays.fill((boolean[]) arr, (boolean) init);
    }

    // initComputed

    private static final MethodType INIT_COMPUTED_TYPE =
            MethodType.methodType(void.class, Object.class, Class.class, int.class, int.class,
                                  IntFunction.class);

    private static void initComputed(Object arr, Class<?> comp, int mods, int len,
                                     IntFunction<?> f) {
        Arrays.setAll((Object[]) arr, f);
    }

    private static void initComputed_int(Object arr, Class<?> comp, int mods, int len,
                                         IntFunction<?> f) {
        int[] a = (int[]) arr;
        for (int i = 0; i < len; i++) a[i] = (int) f.apply(i);
    }

    private static void initComputed_long(Object arr, Class<?> comp, int mods, int len,
                                          IntFunction<?> f) {
        long[] a = (long[]) arr;
        for (int i = 0; i < len; i++) a[i] = (long) f.apply(i);
    }

    private static void initComputed_float(Object arr, Class<?> comp, int mods, int len,
                                           IntFunction<?> f) {
        float[] a = (float[]) arr;
        for (int i = 0; i < len; i++) a[i] = (float) f.apply(i);
    }

    private static void initComputed_double(Object arr, Class<?> comp, int mods, int len,
                                            IntFunction<?> f) {
        double[] a = (double[]) arr;
        for (int i = 0; i < len; i++) a[i] = (double) f.apply(i);
    }

    private static void initComputed_byte(Object arr, Class<?> comp, int mods, int len,
                                          IntFunction<?> f) {
        byte[] a = (byte[]) arr;
        for (int i = 0; i < len; i++) a[i] = (byte) f.apply(i);
    }

    private static void initComputed_short(Object arr, Class<?> comp, int mods, int len,
                                          IntFunction<?> f) {
        short[] a = (short[]) arr;
        for (int i = 0; i < len; i++) a[i] = (short) f.apply(i);
    }

    private static void initComputed_char(Object arr, Class<?> comp, int mods, int len,
                                          IntFunction<?> f) {
        char[] a = (char[]) arr;
        for (int i = 0; i < len; i++) a[i] = (char) f.apply(i);
    }

    private static void initComputed_boolean(Object arr, Class<?> comp, int mods, int len,
                                          IntFunction<?> f) {
        boolean[] a = (boolean[]) arr;
        for (int i = 0; i < len; i++) a[i] = (boolean) f.apply(i);
    }
}
