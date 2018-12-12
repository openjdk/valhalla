/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

import sun.security.action.GetPropertyAction;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.invoke.MethodHandles.Lookup.IMPL_LOOKUP;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;

/**
 * Bootstrap methods for value types
 */
public final class ValueBootstrapMethods {
    private ValueBootstrapMethods() {}
    // lookup() throws IAE as privileged
    // private static final MethodHandles.Lookup THIS_LOOKUP = MethodHandles.lookup();
    private static final boolean VERBOSE =
        GetPropertyAction.privilegedGetProperty("value.bsm.debug") != null;

    /**
     * Makes a bootstrap method for the named operation for the given Class.
     *
     * @apiNote {@code c} parameter for testing purpose.  This method will be removed.
     *
     * @param caller    A lookup context
     * @param name      The name of the method to implement.
     * @param type      The expected signature of the {@code CallSite}
     * @param c         Class
     * @return a CallSite whose target can be used to perform the named operation
     */
    public static CallSite makeBootstrapMethod(MethodHandles.Lookup caller,
                                               String name,
                                               MethodType type,
                                               Class<?> c) {
        MethodHandles.Lookup lookup = caller;
        if (caller.lookupClass() != c) {
            lookup = new MethodHandles.Lookup(c);
        }
        return makeBootstrapMethod(lookup, name, type);
    }

    /**
     * Makes a bootstrap method for the named operation for the given Class.
     *
     * @param lookup    A lookup context
     * @param name      The name of the method to implement.
     * @param type      The expected signature of the {@code CallSite}
     * @return a CallSite whose target can be used to perform the named operation
     */
    public static CallSite makeBootstrapMethod(MethodHandles.Lookup lookup,
                                               String name,
                                               MethodType type) {
        return new ConstantCallSite(generateTarget(lookup, name, type));
    }

    /*
     * Produces a MethodHandle for the specified method.
     *
     * If the method handle is invoked with an value object, the implementation
     * of the specified method is equivalent to calling
     *     List.of(Class, f1, f2, f3...).$name
     *
     *  extracts the field values as follows:
     *
     * Object[] objs = new Object[numFields+1];
     * objs[0] = c;
     * for (int i=1; i <= numFields; i++) {
     *      MethodHandle mh = getters[i];
     *      objs[i] = mh.invokeExact(param);
     * }
     */
    private static MethodHandle generateTarget(MethodHandles.Lookup lookup, String name, MethodType type) {
        if (VERBOSE) {
            System.out.println("generate BSM for " + lookup + "::" + name);
        }

        // build MethodHandle to get the field values of a value class
        MethodHandle mh = ValueBsmFactory.build(lookup);
        MethodType mt;
        switch (name) {
            case "hashCode":
            case "valueHashCode":
                mt = methodType(int.class, MethodHandle.class, Object.class);
                break;
            case "equals":
                mt = methodType(boolean.class, MethodHandle.class, Object.class, Object.class);
                break;
            case "toString":
                mt = methodType(String.class, MethodHandle.class, Object.class);
                break;
            default:
                throw new IllegalArgumentException(name + " not valid method name");
        }
        try {
            // return the method handle that implements the named method
            // which first invokes the getters method handle of a value object
            // and then compute the result
            return IMPL_LOOKUP.findStatic(ValueBootstrapMethods.class, name, mt).bindTo(mh).asType(type);
        } catch (ReflectiveOperationException e) {
            throw new BootstrapMethodError(e);
        }
    }

    static class ValueBsmFactory {
        private static final MethodHandle NEW_ARRAY;
        private static final MethodHandle GET_FIELD_VALUE;

        static {
            try {
                GET_FIELD_VALUE = IMPL_LOOKUP.findStatic(ValueBsmFactory.class, "getFieldValue",
                            methodType(Object[].class, MethodHandle[].class, Object[].class, int.class, Object.class));
                NEW_ARRAY = IMPL_LOOKUP.findStatic(ValueBsmFactory.class, "newObjectArray",
                            methodType(Object[].class, MethodHandle[].class));
            } catch (ReflectiveOperationException e) {
                throw new BootstrapMethodError(e);
            }
        }

        /*
         * Produce a MethodHandle that returns
         *    new Object[] { Class, field1, field2, ... }
         *
         * int size = getters.length;
         * Object[] result = new Object[size];
         * for (int i=0; i < size; ++i) {
         *     result[i] = getters.invoke(obj);
         * }
         * return result;
         */
        static MethodHandle build(MethodHandles.Lookup lookup) {
            // build a MethodHandle[] { Class, getter1, getter2, ...} for the lookup class
            Class<?> type = lookup.lookupClass().asValueType();
            MethodHandle valueClass = dropArguments(constant(Class.class, type), 0, Object.class);
            MethodHandle[] getters = Stream.concat(Stream.of(valueClass), fields(lookup))
                                           .toArray(MethodHandle[]::new);

            MethodHandle iterations = dropArguments(constant(int.class, getters.length), 0, Object.class);
            MethodHandle init = ValueBsmFactory.NEW_ARRAY.bindTo(getters);
            MethodHandle body = ValueBsmFactory.GET_FIELD_VALUE.bindTo(getters);
            return countedLoop(iterations, init, body);
        }

        static Object[] newObjectArray(MethodHandle[] getters) {
            return new Object[getters.length];
        }

        /*
         * Set the value of the field value at index {@code i} in the given
         * {@code values} array.
         */
        static Object[] getFieldValue(MethodHandle[] getters, Object[] values, int i, Object o) throws Throwable {
            values[i] = getters[i].invoke(o);
            return values;
        }

        static Stream<MethodHandle> fields(MethodHandles.Lookup lookup) {
            return Arrays.stream(lookup.lookupClass().getDeclaredFields())
                         .filter(f -> !Modifier.isStatic(f.getModifiers()))
                         .map(f -> {
                            try {
                                return lookup.unreflectGetter(f);
                            } catch (IllegalAccessException e) {
                                throw new BootstrapMethodError(e);
                            }
                         });
        }
    }

    private static int hashCode(MethodHandle getters, Object obj) {
        return isValue(obj) ? valueHashCode(getters, obj) : obj.hashCode();
    }

    private static int valueHashCode(MethodHandle getters, Object obj) {
        if (!isValue(obj)) {
            throw new IllegalArgumentException(obj + " not value type");
        }
        Object[] values = invoke(getters, obj);
        return List.of(values).hashCode();
    }

    /**
     * Returns a string representation of the specified object.
     */
    private static String toString(MethodHandle getters, Object obj) {
        if (isValue(obj)) {
            Object[] values = invoke(getters, obj);
            return Arrays.stream(values)
                         .map(Object::toString)
                         .collect(Collectors.joining(", ", "[", "]"));
        } else {
            return obj.toString();
        }
    }

    /**
     * Returns true if o1 equals o2.
     */
    private static boolean equals(MethodHandle getters, Object o1, Object o2) {
        if (o1 == o2) return true;
        if (o1 == null || o2 == null) return false;
        if (o1.getClass() == o2.getClass() && isValue(o1)) {
            Object[] values1 = invoke(getters, o1);
            Object[] values2 = invoke(getters, o2);
            return Arrays.equals(values1, values2);
        }
        return o1.equals(o2);
    }

    private static boolean isValue(Object obj) {
        return obj.getClass().isValue();
    }

    private static Object[] invoke(MethodHandle getters, Object obj) {
        try {
            Object[] values = (Object[]) getters.invoke(obj);
            if (VERBOSE) {
                System.out.println(Arrays.toString(values));
            }
            return values;
        } catch (Throwable e) {
            throw new InternalError(e);
        }
    }
}
