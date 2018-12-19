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

import sun.invoke.util.Wrapper;
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

    static class MethodHandleBuilder {
        static MethodHandle[] getters(Lookup lookup) {
            Class<?> type = lookup.lookupClass().asValueType();
            return Arrays.stream(type.getDeclaredFields())
                         .filter(f -> !Modifier.isStatic(f.getModifiers()))
                         .map(f -> {
                             try {
                                 return lookup.unreflectGetter(f);
                             } catch (IllegalAccessException e) {
                                 throw newLinkageError(e);
                             }
                         }).sorted((mh1, mh2) -> {
                             // sort the getters with the return type
                             Class<?> t1 = mh1.type().returnType();
                             Class<?> t2 = mh2.type().returnType();
                             if (t1.isPrimitive()) {
                                 if (!t2.isPrimitive()) {
                                     return 1;
                                 }
                             } else {
                                 if (t2.isPrimitive()) {
                                     return -1;
                                 }
                             }
                             return -1;
                         }).toArray(MethodHandle[]::new);
        }

        static MethodHandle primitiveEquals(Class<?> primitiveType) {
            int index = Wrapper.forPrimitiveType(primitiveType).ordinal();
            return EQUALS[index];
        }

        static MethodHandle referenceEquals(Class<?> type) {
            return EQUALS[Wrapper.OBJECT.ordinal()].asType(methodType(boolean.class, type, type));
        }

        /*
         * Produces a MethodHandle that returns boolean if two instances
         * of the given interface class are substitutable.
         *
         * Two interface values are i== iff
         * 1. if o1 and o2 are both reference objects then o1 r== o2; or
         * 2. if o1 and o2 are both values then o1 v== o2
         */
        static MethodHandle interfaceEquals(Class<?> type) {
            assert type.isInterface() || type == Object.class;
            MethodType mt = methodType(boolean.class, type, type);
            return guardWithTest(IS_SAME_VALUE_CLASS.asType(mt), VALUE_EQUALS.asType(mt), referenceEquals(type));
            // return INTERFACE_EQUALS.asType(methodType(boolean.class, type, type));
        }

        /*
         * Produces a MethodHandle that returns boolean if two value instances
         * of the given value class are substitutable.
         */
        static MethodHandle valueEquals(Class<?> c) {
            assert c.isValue();
            Class<?> type = c.asValueType();
            MethodHandles.Lookup lookup = new MethodHandles.Lookup(type);
            MethodHandle[] getters = getters(lookup);

            MethodHandle instanceFalse = dropArguments(FALSE, 0, type, Object.class)
                .asType(methodType(boolean.class, type, type));
            MethodHandle accumulator = dropArguments(TRUE, 0, type, type);
            for (MethodHandle getter : getters) {
                MethodHandle eq = substitutableInvoker(getter.type().returnType());
                MethodHandle thisFieldEqual = filterArguments(eq, 0, getter, getter);
                accumulator = guardWithTest(thisFieldEqual, accumulator, instanceFalse);
            }
            return accumulator;
        }

        private static boolean eq(byte a, byte b)       { return a == b; }
        private static boolean eq(short a, short b)     { return a == b; }
        private static boolean eq(char a, char b)       { return a == b; }
        private static boolean eq(int a, int b)         { return a == b; }
        private static boolean eq(long a, long b)       { return a == b; }
        private static boolean eq(float a, float b)     { return Float.compare(a, b) == 0; }
        private static boolean eq(double a, double b)   { return Double.compare(a, b) == 0; }
        private static boolean eq(boolean a, boolean b) { return a == b; }
        private static boolean eq(Object a, Object b)   { return a == b; }

        private static boolean isSameValueClass(Object a, Object b) {
            return (a != null && b != null
                    && a.getClass().asBoxType() == b.getClass().asBoxType()
                    && a.getClass().isValue());
        }

        private static boolean valueEq(Object a, Object b) {
            assert isSameValueClass(a, b);
            try {
                Class<?> type = a.getClass().asValueType();
                return (boolean) valueEquals(type).invoke(type.cast(a), type.cast(b));
            } catch (Throwable e) {
                throw new InternalError(e);
            }
        }

        static final MethodHandle[] EQUALS = initEquals();
        static final MethodHandle IS_SAME_VALUE_CLASS =
            findStatic("isSameValueClass", methodType(boolean.class, Object.class, Object.class));
        static final MethodHandle VALUE_EQUALS =
            findStatic("valueEq", methodType(boolean.class, Object.class, Object.class));
        static final MethodHandle FALSE = constant(boolean.class, false);
        static final MethodHandle TRUE = constant(boolean.class, true);
        static MethodHandle[] initEquals() {
            MethodHandle[] mhs = new MethodHandle[Wrapper.COUNT];
            for (Wrapper wrapper : Wrapper.values()) {
                if (wrapper == Wrapper.VOID) continue;

                Class<?> type = wrapper.primitiveType();
                mhs[wrapper.ordinal()] = findStatic("eq", methodType(boolean.class, type, type));
            }
            return mhs;
        }

        private static MethodHandle findStatic(String name, MethodType methodType) {
            try {
                return IMPL_LOOKUP.findStatic(MethodHandleBuilder.class, name, methodType);
            } catch (NoSuchMethodException|IllegalAccessException e) {
                throw newLinkageError(e);
            }
        }
    }

    private static LinkageError newLinkageError(Throwable e) {
        return (LinkageError) new LinkageError().initCause(e);
    }

    /**
     * Returns {@code true} if the arguments are <em>substitutable</em> to each
     * other and {@code false} otherwise.
     * <em>Substitutability</em> means that they cannot be distinguished from
     * each other in any data-dependent way, meaning that it is safe to substitute
     * one for the other.
     *
     * <ul>
     * <li>If {@code a} and {@code b} are both {@code null}, this method returns
     *     {@code true}.
     * <li>If {@code a} and {@code b} are both value instances of the same class
     *     {@code V}, this method returns {@code true} if, for all fields {@code f}
     *      declared in {@code V}, {@code a.f} and {@code b.f} are substitutable.
     * <li>If {@code a} and {@code b} are both primitives of the same type,
     *     this method returns {@code a == b} with the following exception:
     *     <ul>
     *     <li> If {@code a} and {@code b} both represent {@code NaN},
     *          this method returns {@code true}, even though {@code NaN == NaN}
     *          has the value {@code false}.
     *     <li> If {@code a} is floating point positive zero while {@code b} is
     *          floating point negative zero, or vice versa, this method
     *          returns {@code false}, even though {@code +0.0 == -0.0} has
     *          the value {@code true}.
     *     </ul>
     * <li>If {@code a} and {@code b} are both instances of the same reference type,
     *     this method returns {@code a == b}.
     * <li>Otherwise this method returns {@code false}.
     * </ul>
     *
     * <p>For example,
     * <pre>{@code interface Number { ... }
     * // ordinary reference class
     * class IntNumber implements Number { ... }
     * // value class
     * value class IntValue implements Number {
     *     int i;
     *     :
     *     public static IntValue of(int i) {...}     // IntValue::of creates a new value instance
     * }
     * // value class with an Object field
     * value class RefValue {
     *     Object o;
     *     :
     * }
     *
     * var val1 = IntValue.of(10);
     * var val2 = IntValue.of(10);                    // val1 and val2 have the same value
     * var ref1 = new IntNumber(10);                  // ref1 and ref2 are two reference instances
     * var ref2 = new IntNumber(10);
     * assertTrue(isSubstitutable(val1, val2));       // val1 and val2 are both value instances of IntValue
     * assertFalse(isSubstitutable(ref1, ref2));      // ref1 and ref2 are two reference instances that are not substitutable
     * assertTrue(isSubstitutable(ref1, ref1));       // a reference instance is substitutable with itself
     *
     * var rval1 = RefValue.of(List.of("list"));      // rval1.o and rval2.o both contain a list of one-single element "list"
     * var rval2 = RefValue.of(List.of("list");
     * var rval3 = RefValue.of(rval1.o);
     *
     * assertFalse(isSubstitutable(rval1, rval2));    // rval1.o and rval2.o are two different List instances and hence not substitutable
     * assertTrue(isSubstitutable(rval1, rval3 ));    // rval1.o and rval3.o are the same reference instance
     * }</pre>
     *
     * @apiNote
     * This API is intended for performance evaluation of this idiom for
     * {@code acmp}.  Hence it is not in the {@link System} class.
     *
     * @param a an object
     * @param b an object to be compared with {@code a} for substitutability
     * @return {@code true} if the arguments are substitutable to each other;
     *         {@code false} otherwise.
     * @param <T> type
     * @see Float#equals(Object)
     * @see Double#equals(Object)
     */
    public static <T> boolean isSubstitutable(T a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.getClass() != b.getClass()) return false;

        try {
            Class<?> type = a.getClass().isValue() ? a.getClass().asValueType() : a.getClass();
            return (boolean) substitutableInvoker(type).invoke(a, b);
        } catch (Throwable e) {
            if (VERBOSE) e.printStackTrace();
            throw new InternalError(e);
        }
    }

    /**
     * Produces a method handle which tests if two arguments are
     * {@linkplain #isSubstitutable(Object, Object) substitutable}.
     *
     * <ul>
     * <li>If {@code T} is a non-floating point primitive type, this method
     *     returns a method handle testing the two arguments are the same value,
     *     i.e. {@code a == b}.
     * <li>If {@code T} is {@code float} or {@code double}, this method
     *     returns a method handle representing {@link Float#equals(Object)} or
     *     {@link Double#equals(Object)} respectively.
     * <li>If {@code T} is a reference type that is not {@code Object} and not an
     *     interface, this method returns a method handle testing
     *     the two arguments are the same reference, i.e. {@code a == b}.
     * <li>If {@code T} is a value type, this method returns
     *     a method handle that returns {@code true} if
     *     for all fields {@code f} declared in {@code T}, where {@code U} is
     *     the type of {@code f}, if {@code a.f} and {@code b.f} are substitutable
     *     with respect to {@code U}.
     * <li>If {@code T} is an interface or {@code Object}, and
     *     {@code a} and {@code b} are of the same value class {@code V},
     *     this method returns a method handle that returns {@code true} if
     *     {@code a} and {@code b} are substitutable with respect to {@code V}.
     * </ul>
     *
     * @param type class type
     * @param <T> type
     * @return a method handle for substitutability test
     */
    static <T> MethodHandle substitutableInvoker(Class<T> type) {
        if (type.isPrimitive())
            return MethodHandleBuilder.primitiveEquals(type);

        if (type.isInterface() || type == Object.class)
            return MethodHandleBuilder.interfaceEquals(type);

        if (type.isValue())
            return SUBST_TEST_METHOD_HANDLES.get(type.asValueType());

        return MethodHandleBuilder.referenceEquals(type);
    }

    // store the method handle for value types in ClassValue
    private static ClassValue<MethodHandle> SUBST_TEST_METHOD_HANDLES = new ClassValue<>() {
        @Override protected MethodHandle computeValue(Class<?> c) {
            return MethodHandleBuilder.valueEquals(c);
        }
    };

}
