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
import sun.security.action.GetIntegerAction;
import sun.security.action.GetPropertyAction;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.invoke.MethodHandles.Lookup.IMPL_LOOKUP;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
import static java.lang.invoke.ValueBootstrapMethods.MethodHandleBuilder.*;


/**
 * Bootstrap methods for value types
 */
public final class ValueBootstrapMethods {
    private ValueBootstrapMethods() {}
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

    private static MethodHandle generateTarget(Lookup lookup, String name, MethodType methodType) {
        if (VERBOSE) {
            System.out.println("generate BSM for " + lookup + "::" + name);
        }
        switch (name) {
            case "hashCode":
                return hashCodeInvoker(lookup, name, methodType);
            case "equals":
                return equalsInvoker(lookup, name, methodType);
            case "toString":
                return toStringInvoker(lookup, name, methodType);
            default:
                throw new IllegalArgumentException(name + " not valid method name");
        }
    }

    static class MethodHandleBuilder {
        static MethodHandle[] getters(Lookup lookup) {
            return getters(lookup, null);
        }

        static MethodHandle[] getters(Lookup lookup, Comparator<MethodHandle> comparator) {
            Class<?> type = lookup.lookupClass().asValueType();
            Stream<MethodHandle> s = Arrays.stream(type.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(f -> {
                    try {
                        return lookup.unreflectGetter(f);
                    } catch (IllegalAccessException e) {
                        throw newLinkageError(e);
                    }
                });
            if (comparator != null) {
                s = s.sorted(comparator);
            }
            return s.toArray(MethodHandle[]::new);
        }

        static MethodHandle primitiveEquals(Class<?> primitiveType) {
            int index = Wrapper.forPrimitiveType(primitiveType).ordinal();
            return EQUALS[index];
        }

        static MethodHandle referenceEquals(Class<?> type) {
            return EQUALS[Wrapper.OBJECT.ordinal()].asType(methodType(boolean.class, type, type));
        }

        static MethodHandle referenceEq() {
            return EQUALS[Wrapper.OBJECT.ordinal()];
        }

        static MethodHandle hashCodeForType(Class<?> type) {
            if (type.isPrimitive()) {
                int index = Wrapper.forPrimitiveType(type).ordinal();
                return HASHCODE[index];
            } else {
                return HASHCODE[Wrapper.OBJECT.ordinal()].asType(methodType(int.class, type));
            }
        }

        static MethodHandle equalsForType(Class<?> type) {
            if (type.isPrimitive()) {
                return primitiveEquals(type);
            } else {
                return OBJECTS_EQUALS.asType(methodType(boolean.class, type, type));
            }
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
        }

        /*
         * Produces a MethodHandle that returns boolean if two value instances
         * of the given value class are substitutable.
         */
        static MethodHandle valueEquals(Class<?> c) {
            assert c.isValue();
            Class<?> type = c.asValueType();
            MethodHandles.Lookup lookup = new MethodHandles.Lookup(type);
            MethodHandle[] getters = getters(lookup, TYPE_SORTER);

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

        // ------ utility methods ------
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
                    && a.getClass().isValue()
                    && a.getClass().asBoxType() == b.getClass().asBoxType());
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

        private static String toString(Object o) {
            return o != null ? o.toString() : "null";
        }

        private static MethodHandle toString(Class<?> type) {
            if (type.isArray()) {
                Class<?> componentType = type.getComponentType();
                if (componentType.isPrimitive()) {
                    int index = Wrapper.forPrimitiveType(componentType).ordinal();
                    return ARRAYS_TO_STRING[index];
                } else {
                    return ARRAYS_TO_STRING[Wrapper.OBJECT.ordinal()].asType(methodType(String.class, type));
                }
            } else {
                return TO_STRING.asType(methodType(String.class, type));
            }
        }

        private static int hashCombiner(int accumulator, int value) {
            return accumulator * 31 + value;
        }

        private static int computeHashCode(MethodHandle[] hashers, int v, int i, Object o) {
            try {
                int hc = (int)hashers[i].invoke(o);
                return hashCombiner(v, hc);
            } catch (Throwable e) {
                throw new InternalError(e);
            }
        }

        private static final MethodHandle[] EQUALS = initEquals();
        private static final MethodHandle[] ARRAYS_TO_STRING = initArraysToString();
        private static final MethodHandle[] HASHCODE = initHashCode();

        static final MethodHandle IS_SAME_VALUE_CLASS =
            findStatic("isSameValueClass", methodType(boolean.class, Object.class, Object.class));
        static final MethodHandle VALUE_EQUALS =
             findStatic("valueEq", methodType(boolean.class, Object.class, Object.class));
        static final MethodHandle TO_STRING =
            findStatic("toString", methodType(String.class, Object.class));
        static final MethodHandle OBJECTS_EQUALS =
            findStatic(Objects.class, "equals", methodType(boolean.class, Object.class, Object.class));

        static final MethodHandle FALSE = constant(boolean.class, false);
        static final MethodHandle TRUE = constant(boolean.class, true);
        static final MethodHandle HASH_COMBINER =
            findStatic("hashCombiner", methodType(int.class, int.class, int.class));
        static final MethodHandle COMPUTE_HASH =
            findStatic("computeHashCode", methodType(int.class, MethodHandle[].class, int.class, int.class, Object.class));

        private static MethodHandle[] initEquals() {
            MethodHandle[] mhs = new MethodHandle[Wrapper.COUNT];
            for (Wrapper wrapper : Wrapper.values()) {
                if (wrapper == Wrapper.VOID) continue;

                Class<?> type = wrapper.primitiveType();
                mhs[wrapper.ordinal()] = findStatic("eq", methodType(boolean.class, type, type));
            }
            return mhs;
        }

        private static MethodHandle[] initArraysToString() {
            MethodHandle[] mhs = new MethodHandle[Wrapper.COUNT];
            for (Wrapper wrapper : Wrapper.values()) {
                if (wrapper == Wrapper.VOID) continue;

                Class<?> arrayType = wrapper.arrayType();
                mhs[wrapper.ordinal()] = findStatic(Arrays.class, "toString", methodType(String.class, arrayType));
            }
            return mhs;
        }

        private static MethodHandle[] initHashCode() {
            MethodHandle[] mhs = new MethodHandle[Wrapper.COUNT];
            for (Wrapper wrapper : Wrapper.values()) {
                if (wrapper == Wrapper.VOID) continue;
                Class<?> cls = wrapper == Wrapper.OBJECT ? Objects.class : wrapper.wrapperType();
                mhs[wrapper.ordinal()] = findStatic(cls, "hashCode",
                                                    methodType(int.class, wrapper.primitiveType()));
            }
            return mhs;
        }

        private static MethodHandle findStatic(String name, MethodType methodType) {
            return findStatic(MethodHandleBuilder.class, name, methodType);
        }
        private static MethodHandle findStatic(Class<?> cls, String name, MethodType methodType) {
            try {
                return IMPL_LOOKUP.findStatic(cls, name, methodType);
            } catch (NoSuchMethodException|IllegalAccessException e) {
                throw newLinkageError(e);
            }
        }

        /**
         * A "salt" value used for this internal hashcode implementation that
         * needs to vary sufficiently from one run to the next so that
         * the default hashcode for value types will vary between JVM runs.
         */
        static final int SALT;
        static {
            long nt = System.nanoTime();
            int value = (int)((nt >>> 32) ^ nt);
            SALT = GetIntegerAction.privilegedGetProperty("value.bsm.salt", value);
        }
    }

    /*
     * Produces a method handle that computes the hashcode
     */
    private static MethodHandle hashCodeInvoker(Lookup lookup, String name, MethodType mt) {
        Class<?> type = lookup.lookupClass().asValueType();
        MethodHandle target = dropArguments(constant(int.class, SALT), 0, type);
        MethodHandle cls = dropArguments(constant(Class.class, type),0, type);
        MethodHandle classHashCode = filterReturnValue(cls, hashCodeForType(Class.class));
        MethodHandle combiner = filterArguments(HASH_COMBINER, 0, target, classHashCode);
        // int v = SALT * 31 + type.hashCode();
        MethodHandle init = permuteArguments(combiner, target.type(), 0, 0);
        MethodHandle[] getters = MethodHandleBuilder.getters(lookup);
        MethodHandle iterations = dropArguments(constant(int.class, getters.length), 0, type);
        MethodHandle[] hashers = new MethodHandle[getters.length];
        for (int i=0; i < getters.length; i++) {
            MethodHandle getter = getters[i];
            MethodHandle hasher = hashCodeForType(getter.type().returnType());
            hashers[i] = filterReturnValue(getter, hasher);
        }

        // for (int i=0; i < getters.length; i++) {
        //   v = computeHash(v, i, a);
        // }
        MethodHandle body = COMPUTE_HASH.bindTo(hashers)
                                        .asType(methodType(int.class, int.class, int.class, type));
        return countedLoop(iterations, init, body);
    }

    /*
     * Produces a method handle that invokes the toString method of a value object.
     */
    private static MethodHandle toStringInvoker(Lookup lookup, String name, MethodType mt) {
        Class<?> type = lookup.lookupClass().asValueType();
        MethodHandle[] getters = MethodHandleBuilder.getters(lookup);
        int length = getters.length;
        StringBuilder format = new StringBuilder();
        Class<?>[] parameterTypes = new Class<?>[length];
        // append the value class name
        format.append("[").append(type.getName());
        String separator = " ";
        for (int i = 0; i < length; i++) {
            MethodHandle getter = getters[i];
            MethodHandleInfo fieldInfo = lookup.revealDirect(getter);
            Class<?> ftype = fieldInfo.getMethodType().returnType();
            format.append(separator)
                  .append(fieldInfo.getName())
                  .append("=\1");
            getters[i]= filterReturnValue(getter, MethodHandleBuilder.toString(ftype));
            parameterTypes[i] = String.class;
        }
        format.append("]");
        try {
            MethodHandle target = StringConcatFactory.makeConcatWithConstants(lookup, "toString",
                    methodType(String.class, parameterTypes), format.toString()).dynamicInvoker();
            // apply getters
            target = filterArguments(target, 0, getters);
            // duplicate "this" argument from o::toString for each getter invocation
            target = permuteArguments(target, methodType(String.class, type), new int[length]);
            return target;
        } catch (StringConcatException e) {
            throw newLinkageError(e);
        }
    }

    /*
     * Produces a method handle that tests if two arguments are equals.
     */
    private static MethodHandle equalsInvoker(Lookup lookup, String name, MethodType mt) {
        Class<?> type = lookup.lookupClass().asValueType();
        // MethodHandle to compare all fields of two value objects
        MethodHandle[] getters = MethodHandleBuilder.getters(lookup, TYPE_SORTER);
        MethodHandle accumulator = dropArguments(TRUE, 0, type, type);
        MethodHandle instanceFalse = dropArguments(FALSE, 0, type, Object.class)
                                        .asType(methodType(boolean.class, type, type));
        for (MethodHandle getter : getters) {
            MethodHandle eq = equalsForType(getter.type().returnType());
            MethodHandle thisFieldEqual = filterArguments(eq, 0, getter, getter);
            accumulator = guardWithTest(thisFieldEqual, accumulator, instanceFalse);
        }

        // if o1 == o2 return true;
        // if (o1 and o2 are same value class) return accumulator;
        // return false;
        MethodHandle instanceTrue = dropArguments(TRUE, 0, type, Object.class).asType(mt);
        return guardWithTest(referenceEq().asType(mt),
                             instanceTrue.asType(mt),
                             guardWithTest(IS_SAME_VALUE_CLASS.asType(mt),
                                           accumulator.asType(mt),
                                           dropArguments(FALSE, 0, type, Object.class)));
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

    private static final Comparator<MethodHandle> TYPE_SORTER = (mh1, mh2) -> {
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
    };
}
