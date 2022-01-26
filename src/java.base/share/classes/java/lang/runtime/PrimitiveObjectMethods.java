/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;
import jdk.internal.access.JavaLangInvokeAccess;
import jdk.internal.access.SharedSecrets;
import sun.invoke.util.Wrapper;
import sun.security.action.GetIntegerAction;
import sun.security.action.GetPropertyAction;

import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.countedLoop;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.filterArguments;
import static java.lang.invoke.MethodHandles.filterReturnValue;
import static java.lang.invoke.MethodHandles.guardWithTest;
import static java.lang.invoke.MethodHandles.permuteArguments;
import static java.lang.invoke.MethodType.methodType;
import static java.lang.runtime.ObjectMethods.primitiveEquals;

/**
 * Implementation for Object::equals and Object::hashCode for primitive classes.
 *
 * PrimitiveObjectMethods::isSubstitutable and primitiveObjectHashCode are
 * private entry points called by VM.
 */
final class PrimitiveObjectMethods {
    private PrimitiveObjectMethods() {}
    private static final boolean VERBOSE =
        GetPropertyAction.privilegedGetProperty("value.bsm.debug") != null;
    private static final JavaLangInvokeAccess JLIA = SharedSecrets.getJavaLangInvokeAccess();

    static class MethodHandleBuilder {
        static MethodHandle[] getters(Class<?> type) {
            return getters(type, null);
        }

        static MethodHandle[] getters(Class<?> type, Comparator<MethodHandle> comparator) {
            // filter static fields
            Stream<MethodHandle> s = Arrays.stream(type.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(f -> {
                    try {
                        return JLIA.unreflectField(f, false);
                    } catch (IllegalAccessException e) {
                        throw newLinkageError(e);
                    }
                });
            if (comparator != null) {
                s = s.sorted(comparator);
            }
            return s.toArray(MethodHandle[]::new);
        }

        static MethodHandle hashCodeForType(Class<?> type) {
            if (type.isPrimitive()) {
                int index = Wrapper.forPrimitiveType(type).ordinal();
                return HASHCODE[index];
            } else {
                return HASHCODE[Wrapper.OBJECT.ordinal()].asType(methodType(int.class, type));
            }
        }

        static MethodHandle primitiveEquals(Class<?> primitiveType) {
            return primitiveEquals.get(primitiveType);
        }

        /*
         * Produces a MethodHandle that returns boolean if two instances
         * of the given reference type are substitutable.
         *
         * Two values of reference type are substitutable i== iff
         * 1. if o1 and o2 are both reference objects then o1 r== o2; or
         * 2. if o1 and o2 are both values then o1 v== o2
         *
         * At invocation time, it needs a dynamic check on the objects and
         * do the substitutability test if they are of a primitive type.
         */
        static MethodHandle referenceTypeEquals(Class<?> type) {
            return OBJECT_EQUALS.asType(methodType(boolean.class, type, type));
        }

        static Class<?> fieldType(MethodHandle getter) {
            Class<?> ftype = getter.type().returnType();
            return ftype;
        }

        /*
         * Produces a MethodHandle that returns boolean if two value instances
         * of the given primitive class are substitutable.
         */
        static MethodHandle primitiveTypeEquals(Class<?> type) {
            assert type.isPrimitiveValueType() || (type.isValue() && !type.isPrimitiveClass());
            MethodType mt = methodType(boolean.class, type, type);
            MethodHandle[] getters = getters(type, TYPE_SORTER);
            MethodHandle instanceTrue = dropArguments(TRUE, 0, type, Object.class).asType(mt);
            MethodHandle instanceFalse = dropArguments(FALSE, 0, type, Object.class).asType(mt);
            MethodHandle accumulator = dropArguments(TRUE, 0, type, type);
            for (MethodHandle getter : getters) {
                Class<?> ftype = fieldType(getter);
                MethodHandle eq = substitutableInvoker(ftype).asType(methodType(boolean.class, ftype, ftype));
                MethodHandle thisFieldEqual = filterArguments(eq, 0, getter, getter);
                accumulator = guardWithTest(thisFieldEqual, accumulator, instanceFalse);
            }
            // if both arguments are null, return true;
            // otherwise return accumulator;
            return guardWithTest(IS_NULL.asType(mt),
                                 instanceTrue,
                                 guardWithTest(IS_SAME_VALUE_CLASS.asType(mt),
                                               accumulator,
                                               instanceFalse));
        }

        static MethodHandle primitiveTypeHashCode(Class<?> type) {
            assert type.isPrimitiveValueType() || (type.isValue() && !type.isPrimitiveClass());
            MethodHandle target = dropArguments(constant(int.class, SALT), 0, type);
            MethodHandle cls = dropArguments(constant(Class.class, type),0, type);
            MethodHandle classHashCode = filterReturnValue(cls, hashCodeForType(Class.class));
            MethodHandle combiner = filterArguments(HASH_COMBINER, 0, target, classHashCode);
            // int v = SALT * 31 + type.hashCode();
            MethodHandle init = permuteArguments(combiner, target.type(), 0, 0);
            MethodHandle[] getters = MethodHandleBuilder.getters(type);
            MethodHandle iterations = dropArguments(constant(int.class, getters.length), 0, type);
            MethodHandle[] hashers = new MethodHandle[getters.length];
            for (int i=0; i < getters.length; i++) {
                MethodHandle getter = getters[i];
                Class<?> ftype = fieldType(getter);

                // For primitive type or reference type, this calls Objects::hashCode.
                // If the instance is of primitive type and the hashCode method is not
                // overridden, VM will call primitiveObjectHashCode to compute the
                // hash code.
                MethodHandle hasher = hashCodeForType(ftype);
                hashers[i] = filterReturnValue(getter, hasher);
            }

            // for (int i=0; i < getters.length; i++) {
            //   v = computeHash(v, i, a);
            // }
            MethodHandle body = COMPUTE_HASH.bindTo(hashers)
                    .asType(methodType(int.class, int.class, int.class, type));
            return countedLoop(iterations, init, body);
        }

        // ------ utility methods ------
        private static boolean eq(Object a, Object b)   {
            if (a == null && b == null) return true;
            if (a == null || b == null) return false;
            if (a.getClass() != b.getClass()) return false;
            return a.getClass().isValue() ? valueEq(a, b) : (a == b);
        }

        /*
         * Returns true if two values are substitutable.
         */
        private static boolean valueEq(Object a, Object b) {
            assert a != null && b != null && isSameValueClass(a, b);
            try {
                Class<?> type = a.getClass();
                if (type.isPrimitiveClass()) {
                    type = type.asValueType();
                }
                return (boolean) substitutableInvoker(type).invoke(type.cast(a), type.cast(b));
            } catch (Error|RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new InternalError(e);
            }
        }

        private static boolean isNull(Object a, Object b) {
            // avoid acmp that will call isSubstitutable
            if (a != null) return false;
            if (b != null) return false;
            return true;
        }

        /*
         * Returns true if the given objects are of the same primitive class.
         *
         * Two objects are of the same primitive class iff:
         * 1. a != null and b != null
         * 2. the declaring class of a and b is the same primitive class
         */
        private static boolean isSameValueClass(Object a, Object b) {
            if (a == null || b == null) return false;

            return a.getClass().isValue() && a.getClass() == b.getClass();
        }

        private static int hashCombiner(int accumulator, int value) {
            return accumulator * 31 + value;
        }

        private static int computeHashCode(MethodHandle[] hashers, int v, int i, Object o) {
            try {
                int hc = (int)hashers[i].invoke(o);
                return hashCombiner(v, hc);
            } catch (Error|RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new InternalError(e);
            }
        }

        private static final MethodHandle FALSE = constant(boolean.class, false);
        private static final MethodHandle TRUE = constant(boolean.class, true);
        private static final MethodHandle OBJECT_EQUALS = findStatic("eq", methodType(boolean.class, Object.class, Object.class));
        private static final MethodHandle IS_SAME_VALUE_CLASS =
            findStatic("isSameValueClass", methodType(boolean.class, Object.class, Object.class));
        private static final MethodHandle IS_NULL =
            findStatic("isNull", methodType(boolean.class, Object.class, Object.class));
        private static final MethodHandle HASH_COMBINER =
            findStatic("hashCombiner", methodType(int.class, int.class, int.class));
        private static final MethodHandle COMPUTE_HASH =
            findStatic("computeHashCode", methodType(int.class, MethodHandle[].class, int.class, int.class, Object.class));
        private static final MethodHandle[] HASHCODE = initHashCode();

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
                return JLIA.findStatic(cls, name, methodType);
            } catch (IllegalAccessException e) {
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
     * @param a an object
     * @param b an object to be compared with {@code a} for substitutability
     * @return {@code true} if the arguments are substitutable to each other;
     *         {@code false} otherwise.
     * @param <T> type
     * @see Float#equals(Object)
     * @see Double#equals(Object)
     */
    private static <T> boolean isSubstitutable(T a, Object b) {
        if (VERBOSE) {
            System.out.println("substitutable " + a + " vs " + b);
        }

        // Called directly from the VM.
        //
        // DO NOT use "==" or "!=" on args "a" and "b", with this code or any of
        // its callees. Could be inside of if_acmp<eq|ne> bytecode implementation.

        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.getClass() != b.getClass()) return false;

        try {
            Class<?> type = a.getClass();
            if (type.isPrimitiveClass()) {
                type = type.asValueType();
            }
            return (boolean) substitutableInvoker(type).invoke(a, b);
        } catch (Error|RuntimeException e) {
            if (VERBOSE) e.printStackTrace();
            throw e;
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
    private static <T> MethodHandle substitutableInvoker(Class<T> type) {
        if (type.isPrimitive())
            return MethodHandleBuilder.primitiveEquals(type);

        if (type.isPrimitiveValueType() || (type.isValue() && !type.isPrimitiveClass())) {
            return SUBST_TEST_METHOD_HANDLES.get(type);
        }
        return MethodHandleBuilder.referenceTypeEquals(type);
    }

    // store the method handle for value types in ClassValue
    private static ClassValue<MethodHandle> SUBST_TEST_METHOD_HANDLES = new ClassValue<>() {
        @Override protected MethodHandle computeValue(Class<?> type) {
            return MethodHandleBuilder.primitiveTypeEquals(type);
        }
    };

    /**
     * Invoke the bootstrap methods hashCode for the given primitive class object.
     * @param o the instance to hash.
     * @return the hash code of the given primitive class object.
     */
    private static int primitiveObjectHashCode(Object o) {
        Class<?> c = o.getClass();
        try {
            // Note: javac disallows user to call super.hashCode if user implemented
            // risk for recursion for experts crafting byte-code
            if (!c.isValue())
                throw new InternalError("must be value or primitive class: " + c.getName());
            Class<?> type = c.isPrimitiveClass() ? c.asValueType() : c;
            return (int) HASHCODE_METHOD_HANDLES.get(type).invoke(o);
        } catch (Error|RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            if (VERBOSE) e.printStackTrace();
            throw new InternalError(e);
        }
    }

    private static ClassValue<MethodHandle> HASHCODE_METHOD_HANDLES = new ClassValue<>() {
        @Override protected MethodHandle computeValue(Class<?> type) {
            return MethodHandleBuilder.primitiveTypeHashCode(type);
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
