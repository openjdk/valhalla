/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.internal.access.JavaLangInvokeAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.value.PrimitiveClass;
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
 * Implementation for Object::equals and Object::hashCode for value objects.
 *
 * ValueObjectMethods::isSubstitutable and valueObjectHashCode are
 * private entry points called by VM.
 */
final class ValueObjectMethods {
    private ValueObjectMethods() {}
    private static final boolean VERBOSE =
            GetPropertyAction.privilegedGetProperty("value.bsm.debug") != null;
    private static final int THRESHOLD =
            GetIntegerAction.privilegedGetProperty("jdk.value.recursion.threshold", Integer.MAX_VALUE);
    private static final JavaLangInvokeAccess JLIA = SharedSecrets.getJavaLangInvokeAccess();

    static class MethodHandleBuilder {
        static Stream<MethodHandle> getterStream(Class<?> type, Comparator<MethodHandle> comparator) {
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
            return s;
        }

        static List<MethodHandle> getters(Class<?> type, Comparator<MethodHandle> comparator) {
            return getterStream(type, comparator).toList();
        }

        static MethodHandle hashCodeForType(Class<?> type) {
            if (type.isPrimitive()) {
                int index = Wrapper.forPrimitiveType(type).ordinal();
                return HASHCODE[index];
            } else {
                return HASHCODE[Wrapper.OBJECT.ordinal()].asType(methodType(int.class, type));
            }
        }

        static MethodHandle builtinPrimitiveEquals(Class<?> type) {
            return primitiveEquals.get(type);
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
         * do the substitutability test if they are of a value class.
         */
        static MethodHandle referenceTypeEquals(Class<?> type) {
            return OBJECT_EQUALS.asType(methodType(boolean.class, type, type));
        }

        static Class<?> fieldType(MethodHandle getter) {
            Class<?> ftype = getter.type().returnType();
            return ftype;
        }

        /**
         * A base method for testing substitutability on a recursive data type,
         * a value class with cyclic membership.
         *
         * This method will first invoke a method handle to test the substitutability
         * of fields whose type is not recursively-typed.  If true, then compares the
         * value of the fields whose type is a recursive data type.
         * For a field of its own type {@code f}, invoke the method handle for
         * this base method on the field value of the given objects.
         * For a field of other recursive data type, invoke {@link #isSubstitutable(Object, Object)}
         * on the field value of the given objects.
         *
         * @param type  a value class
         * @param mh    a MethodHandle that tests substitutability of all fields whose type
         *              is not recursively-typed.  The method type is (V, V)boolean
         * @param getters all getters for the fields whose type is a recursive data type
         * @param recur MethodHandle that is capable of recursively calling itself
         *              to test if two objects of the given value class are substitutable.
         *              The method type is (Object, Object, AtomicInteger)boolean.
         * @param o1    an object
         * @param o2    an object to be compared for substitutability
         * @param counter an AtomicInteger counter to keep track of the traversal count
         * @return
         * @param <V>   a value class
         */
        private static <V> boolean substitutableBase(Class<V> type, MethodHandle mh, MethodHandle[] getters,
                                                     MethodHandle recur, Object o1, Object o2,
                                                     AtomicInteger counter) throws Throwable {
            assert isValueClass(type) : type.getName() + " not a value class";

            if (o1 == null && o2 == null) return true;
            if (o1 == null || o2 == null) return false;

            if (counter.getAndDecrement() == 0) {
                throw new StackOverflowError("fail to evaluate == for value class " + type.getName());
            }

            // test if the substitutability of all fields whose type is not recursively-typed
            var result = (boolean) mh.invoke(o1, o2);
            if (result) {
                assert o1.getClass() == type && o2.getClass() == type;

                // test if the fields of a recursive data type are substitutable
                for (MethodHandle getter : getters) {
                    Class<?> ftype = fieldType(getter);
                    var f1 = getter.invoke(o1);
                    var f2 = getter.invoke(o2);

                    assert JLIA.isNullRestrictedField(getter) && f1 != null && f2 != null :
                            "null restricted field " + ftype.getName() + " in container " + type.getName();

                    boolean substitutable;
                    if (ftype == type) {
                        substitutable = (boolean)recur.invokeExact(f1, f2, counter);
                    } else {
                        MethodHandle recur2 = RECUR_SUBST_METHOD_HANDLES.get(ftype);
                        substitutable = (boolean)recur2.invokeExact(f1, f2, counter);
                    }
                    if (!substitutable) {
                        return false;
                    }
                }
            }
            return result;
        }


        /**
         * A base method for computing the hashcode for a recursive data type,
         * a value class with cyclic membership.
         *
         * This method will first invoke a method handle to compute the hash code
         * of the fields whose type is not recursively-typed.  Then compute the
         * hash code of the remaining fields whose type is a recursive data type.
         * For a field of its own type {@code f}, invoke the method handle for
         * this base method on the field value of the given object.
         * For a field of other recursive data type, invoke {@link Object#hashCode()}
         * on the field value of the given object.
         *
         * @param type  a value class
         * @param mh    a MethodHandle that computes the hash code of all fields whose
         *              type is not recursively-typed.  The method type is (V)int
         * @param getters all getters for the fields whose type is a recursive data type
         * @param recur MethodHandle that is capable of recursively calling itself
         *              to compute the hash code of a field of the same type.
         *              The method type is (Object, AtomicInteger)int.
         * @param obj   an object
         * @param counter an AtomicInteger counter to keep track of the traversal count
         * @return the hash code of a value object of the given type
         * @param <V>   a value class
         */
        private static <V> int hashCodeBase(Class<V> type, MethodHandle mh, MethodHandle[] getters,
                                            MethodHandle recur, Object obj,
                                            AtomicInteger counter) throws Throwable {
            assert isValueClass(type) : type.getName() + " not a value class";

            if (obj == null) return 0;

            if (counter.getAndDecrement() == 0) {
                throw new StackOverflowError("fail to evaluate hashCode of a value object: " + type.getName());
            }

            // compute the hash code of all fields whose type is not recursively-typed
            var result = (int) mh.invoke(obj);
            if (obj != null) {
                assert obj.getClass() == type;

                // test if the fields of a recursive data type are substitutable
                for (MethodHandle getter : getters) {
                    Class<?> ftype = fieldType(getter);
                    var f = getter.invoke(obj);
                    assert JLIA.isNullRestrictedField(getter) && f != null :
                            "null restricted field " + ftype.getName() + " in container " + type.getName();

                    int hc;
                    if (ftype == type) {
                        hc = (int)recur.invokeExact(f, counter);
                    } else {
                        MethodHandle recur2 = RECUR_HASHCODE_METHOD_HANDLES.get(ftype);
                        hc = (int)recur2.invokeExact(f, counter);
                    }
                    result = hashCombiner(result, hc);
                }
            }
            return result;
        }

        /*
         * Finds all value class memberships of the given type involved in cycles
         */
        private static Set<Class<?>> recursiveValueTypes(Class<?> type) {
            if (!isValueClass(type)) {
                return Set.of();
            }

            Deque<Class<?>> deque = new ArrayDeque<>();
            Set<Class<?>> visited = new HashSet<>();
            Set<Class<?>> recursiveTypes = new HashSet<>();
            Map<Class<?>, List<Class<?>>> unvisitedEdges = new HashMap<>();

            Class<?> c;
            deque.add(type);
            while ((c = deque.peek()) != null) {
                if (visited.contains(c)) {
                    // remove the current node being visited
                    deque.pop();
                    if (deque.contains(c)) {
                        // include all types in the cycle
                        for (Class<?> n : deque) {
                            recursiveTypes.add(n);
                            if (n == c) {
                                break;
                            }
                        }
                    }

                    // continue the depth-first search from the parent of c
                    if ((c = deque.peek()) == null)
                        continue;
                } else {
                    visited.add(c);
                }

                // depth-first search on the field types of type c that are value classes
                List<Class<?>> nodes = unvisitedEdges.computeIfAbsent(c, (k) -> fieldTypes(k));
                if (nodes.isEmpty()) {
                    // all field types are traversed
                    deque.pop();
                } else {
                    Class<?> n = nodes.remove(0);
                    deque.push(n);
                }
            }

            if (recursiveTypes.isEmpty())
                return Set.of();

            return Arrays.stream(type.getDeclaredFields())
                         .filter(f -> !Modifier.isStatic(f.getModifiers()))
                         .map(f -> f.getType())
                         .filter(recursiveTypes::contains)
                         .collect(Collectors.toSet());
        }

        private static List<Class<?>> fieldTypes(Class<?> type) {
            List<Class<?>> result = new ArrayList<>();
            Arrays.stream(type.getDeclaredFields())
                  .filter(f -> !Modifier.isStatic(f.getModifiers()))
                  .map(f -> f.getType())
                  .filter(ft -> isValueClass(ft) && !result.contains(ft))
                  .forEach(result::add);
            return result;
        }

        private static final ClassValue<MethodHandle> RECUR_SUBST_METHOD_HANDLES = new ClassValue<>() {
            /*
             * Produces a MethodHandle that returns boolean if two value objects of
             * a recursive data type are substitutable.  This method is invoked by
             * the substitutableBase method.
             *
             * The method type is (Object, Object, AtomicInteger)boolean.
             */
            @Override protected MethodHandle computeValue(Class<?> type) {
                return recurValueTypeEquals(type, recursiveValueTypes(type));
            }
        };

        /*
         * Produces a MethodHandle that returns boolean if two value objects
         * are substitutable.  The method type is (V, V)boolean.
         */
        static MethodHandle valueTypeEquals(Class<?> type) {
            // ensure the reference type of a primitive class not used in the method handle
            assert isValueClass(type) || PrimitiveClass.isPrimitiveValueType(type);

            Set<Class<?>> recursiveTypes = recursiveValueTypes(type);
            if (recursiveTypes.isEmpty()) {
                return valueTypeEquals(type, getters(type, TYPE_SORTER));
            } else {
                MethodHandle target = recurValueTypeEquals(type, recursiveTypes);
                return MethodHandles.insertArguments(target, 2, new AtomicInteger(THRESHOLD))
                                    .asType(methodType(boolean.class, type, type));
            }
        }

        /*
         * Produces a MethodHandle that returns boolean if the given fields
         * of the two value objects are substitutable. The method type is (V, V)boolean
         */
        static MethodHandle valueTypeEquals(Class<?> type, List<MethodHandle> getters) {
            // ensure the reference type of a primitive class not used in the method handle
            assert isValueClass(type) || PrimitiveClass.isPrimitiveValueType(type);

            MethodType mt = methodType(boolean.class, type, type);
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

        /*
         * Produces a MethodHandle that returns boolean if two value objects of
         * a recursive data type are substitutable.
         *
         * The method type is (Object, Object, AtomicInteger)boolean.
         */
        static MethodHandle recurValueTypeEquals(Class<?> type, Set<Class<?>> recursiveTypes) {
            Stream<MethodHandle> getterStream = getterStream(type, TYPE_SORTER);;
            List<MethodHandle> nonRecurTypeGetters = new ArrayList<>();
            List<MethodHandle> recurTypeGetters = new ArrayList<>();
            getterStream.forEach(getter -> {
                Class<?> ftype = fieldType(getter);
                if (recursiveTypes.contains(ftype)) {
                    // skip the value class that is involved in a cyclic membership
                    recurTypeGetters.add(getter);
                } else {
                    nonRecurTypeGetters.add(getter);
                }
            });

            if (recurTypeGetters.isEmpty()) {
                throw new InternalError("must be a recursive data type: " + type.getName());
            }

            MethodHandle target = valueTypeEquals(type, nonRecurTypeGetters);
            // This value class contains cyclic membership
            // Create a method handle that is capable of calling itself.
            // - the substitutableBase method first calls the method handle that tests
            //   the substitutability of all fields that are not a recursive data type
            // - for a field of its own type, call the recursive method
            // - for a field of a recursive data type, call isSubstitutable
            Object[] arguments = new Object[]{type, target, recurTypeGetters.toArray(MethodHandle[]::new)};
            target = MethodHandles.insertArguments(RECUR_EQUALS, 0, arguments);
            return recursive(target);
        }

        private static final ClassValue<MethodHandle> RECUR_HASHCODE_METHOD_HANDLES = new ClassValue<>() {
            /*
             * Produces a MethodHandle that returns the hashcode of a value object of
             * a recursive data type.  This method is invoked by the hashCodeBase method.
             *
             * The method type is (Object, AtomicInteger)int.
             */
            @Override protected MethodHandle computeValue(Class<?> type) {
                return recurValueTypeHashCode(type, recursiveValueTypes(type));
            }
        };

        /*
         * Produces a MethodHandle that computes the hash code of a value object.
         * The method type of the return MethodHandle is (V)int.
         */
        static MethodHandle valueTypeHashCode(Class<?> type) {
            // ensure the reference type of a primitive class not used in the method handle
            assert isValueClass(type) || PrimitiveClass.isPrimitiveValueType(type);

            Set<Class<?>> recursiveTypes = recursiveValueTypes(type);
            if (recursiveTypes.isEmpty()) {
                return valueTypeHashCode(type, getterStream(type, null).toList());
            } else {
                MethodHandle target = recurValueTypeHashCode(type, recursiveTypes);
                return MethodHandles.insertArguments(target, 1, new AtomicInteger(THRESHOLD))
                                    .asType(methodType(int.class, type));
            }
        }

        /*
         * Produces a MethodHandle that returns the hash code computed from
         * the given fields of a value object. The method type is (V)int.
         */
        static MethodHandle valueTypeHashCode(Class<?> type, List<MethodHandle> getters) {
            // ensure the reference type of a primitive class not used in the method handle
            assert isValueClass(type) || PrimitiveClass.isPrimitiveValueType(type);

            MethodHandle target = dropArguments(constant(int.class, SALT), 0, type);
            MethodHandle cls = dropArguments(constant(Class.class, type),0, type);
            MethodHandle classHashCode = filterReturnValue(cls, hashCodeForType(Class.class));
            MethodHandle combiner = filterArguments(HASH_COMBINER, 0, target, classHashCode);
            // int v = SALT * 31 + type.hashCode();
            MethodHandle init = permuteArguments(combiner, target.type(), 0, 0);
            int length = getters.size();
            MethodHandle iterations = dropArguments(constant(int.class, length), 0, type);
            MethodHandle[] hashers = new MethodHandle[length];
            for (int i=0; i < length; i++) {
                MethodHandle getter = getters.get(i);
                Class<?> ftype = fieldType(getter);

                // For primitive types or reference types, this calls Objects::hashCode.
                // For value objects and the hashCode method is not overridden,
                // VM will call valueObjectHashCode to compute the hash code.
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

        /*
         * Produces a MethodHandle that returns the hashcode of a value object of
         * a recursive data type.  This method is invoked by the hashCodeBase method.
         *
         * The method type is (Object, AtomicInteger)int.
         */
        static MethodHandle recurValueTypeHashCode(Class<?> type, Set<Class<?>> recursiveTypes) {
            Stream<MethodHandle> getterStream = getterStream(type, null);;
            List<MethodHandle> nonRecurTypeGetters = new ArrayList<>();
            List<MethodHandle> recurTypeGetters = new ArrayList<>();
            getterStream.forEach(getter -> {
                Class<?> ftype = fieldType(getter);
                if (recursiveTypes.contains(ftype)) {
                    // skip the value class that is involved in a cyclic membership
                    recurTypeGetters.add(getter);
                } else {
                    nonRecurTypeGetters.add(getter);
                }
            });

            if (recurTypeGetters.isEmpty()) {
                throw new InternalError("must be a recursive data type: " + type.getName());
            }

            MethodHandle target = valueTypeHashCode(type, nonRecurTypeGetters);
            if (VERBOSE) {
                System.out.println(type.getName() + " valueHashCode " + nonRecurTypeGetters + " recursive types " + recurTypeGetters);
            }
            // This value class contains cyclic membership
            // Create a method handle that is capable of calling itself.
            // - the hashCodeBase method first calls the method handle that computes the hash code
            //   of all fields whose type is not recursively-typed
            // - for a field of its own type, call the recursive method
            // - for a field of a recursive data type, call valueObjectHashCode
            Object[] arguments = new Object[]{type, target, recurTypeGetters.toArray(MethodHandle[]::new)};
            target = MethodHandles.insertArguments(RECUR_HASHCODE, 0, arguments);
            return recursive(target);
        }

        // ------ utility methods ------
        private static boolean eq(Object a, Object b)   {
            if (a == null && b == null) return true;
            if (a == null || b == null) return false;
            if (a.getClass() != b.getClass()) return false;
            return a.getClass().isValue() ? valueEquals(a, b) : (a == b);
        }

        /*
         * Returns true if two value objects are substitutable.
         */
        private static boolean valueEquals(Object a, Object b) {
            assert a != null && b != null && isSameValueClass(a, b);
            try {
                Class<?> type = a.getClass();
                if (PrimitiveClass.isPrimitiveClass(type)) {
                    type = PrimitiveClass.asValueType(type);
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
         * Returns true if the given objects are of the same value class.
         *
         * Two objects are of the same value class iff:
         * 1. a != null and b != null
         * 2. the declaring class of a and b is the same value class
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
        private static final MethodHandle OBJECT_EQUALS =
            findStatic("eq", methodType(boolean.class, Object.class, Object.class));
        private static final MethodHandle IS_SAME_VALUE_CLASS =
            findStatic("isSameValueClass", methodType(boolean.class, Object.class, Object.class));
        private static final MethodHandle IS_NULL =
            findStatic("isNull", methodType(boolean.class, Object.class, Object.class));
        private static final MethodHandle HASH_COMBINER =
            findStatic("hashCombiner", methodType(int.class, int.class, int.class));
        private static final MethodHandle COMPUTE_HASH =
            findStatic("computeHashCode", methodType(int.class, MethodHandle[].class, int.class, int.class, Object.class));
        private static final MethodHandle[] HASHCODE = initHashCode();
        private static final MethodHandle RECUR_EQUALS =
            findStatic("substitutableBase",
                       methodType(boolean.class, Class.class, MethodHandle.class, MethodHandle[].class,
                                  MethodHandle.class, Object.class, Object.class, AtomicInteger.class));
        private static final MethodHandle RECUR_HASHCODE =
            findStatic("hashCodeBase",
                       methodType(int.class, Class.class, MethodHandle.class, MethodHandle[].class,
                                  MethodHandle.class, Object.class, AtomicInteger.class));

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
         * the default hashcode for value classes will vary between JVM runs.
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
     * <li>If {@code a} and {@code b} are both instances of the same value class
     *     {@code V}, this method returns {@code true} if, for all fields {@code f}
     *      declared in {@code V}, {@code a.f} and {@code b.f} are substitutable.
     * <li>If {@code a} and {@code b} are both values of the same builtin primitive type,
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
            if (PrimitiveClass.isPrimitiveClass(type)) {
                type = PrimitiveClass.asValueType(type);
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
     * <li>If {@code T} is a value class, this method returns
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
            return MethodHandleBuilder.builtinPrimitiveEquals(type);

        if (isValueClass(type) || PrimitiveClass.isPrimitiveValueType(type)) {
            return SUBST_TEST_METHOD_HANDLES.get(type);
        }
        return MethodHandleBuilder.referenceTypeEquals(type);
    }

    // store the method handle for value classes in ClassValue
    private static final ClassValue<MethodHandle> SUBST_TEST_METHOD_HANDLES = new ClassValue<>() {
        @Override protected MethodHandle computeValue(Class<?> type) {
            return MethodHandleBuilder.valueTypeEquals(type);
        }
    };

    /**
     * Invoke the hashCode method for the given value object.
     * @param o the instance to hash.
     * @return the hash code of the given value object.
     */
    private static int valueObjectHashCode(Object o) {
        Class<?> c = o.getClass();
        try {
            // Note: javac disallows user to call super.hashCode if user implemented
            // risk for recursion for experts crafting byte-code
            if (!c.isValue())
                throw new InternalError("must be value or primitive class: " + c.getName());

            Class<?> type = PrimitiveClass.isPrimitiveClass(c) ? PrimitiveClass.asValueType(c) : c;
            return (int) HASHCODE_METHOD_HANDLES.get(type).invoke(o);
        } catch (Error|RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            if (VERBOSE) e.printStackTrace();
            throw new InternalError(e);
        }
    }

    /**
     * Returns true if the given type is a value class.
     */
    private static boolean isValueClass(Class<?> type) {
        return type.isValue() && !PrimitiveClass.isPrimitiveClass(type);
    }

    private static final ClassValue<MethodHandle> HASHCODE_METHOD_HANDLES = new ClassValue<>() {
        @Override protected MethodHandle computeValue(Class<?> type) {
            return MethodHandleBuilder.valueTypeHashCode(type);
        }
    };

    private static final Comparator<MethodHandle> TYPE_SORTER = (mh1, mh2) -> {
        // sort the getters with the return type
        Class<?> t1 = mh1.type().returnType();
        Class<?> t2 = mh2.type().returnType();
        if (t1 == t2) return 0;

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


    /**
     * Constructs a method handle that is capable of recursively
     * calling itself, whose behavior is determined by a non-recursive
     * base method handle which gets both the original arguments and a
     * reference to the recursive method handle.
     * <p>
     * Here is pseudocode for the resulting loop handle, plus a sketch
     * of the behavior of the base function. The symbols {@code A},
     * {@code a}, and {@code R} represent arguments and return value
     * for both the recursive function and the base function.
     *
     * <blockquote><pre>{@code
     * R recursive(A... a) {
     *   MethodHandle recur = &recursive;
     *   return base(recur, a...);
     * }
     * R base(MethodHandle recur, A... a) {
     *   ... if (no recursion)  return f(a);  ...
     *   var r2 = recur.invokeExact(a2...);
     *   var r3 = recur.invokeExact(a3...);
     *   ... do stuff with r2, r3, etc. ...
     * }
     * }</pre></blockquote>
     * <p>
     * To make several functions mutually recursive, additional base
     * arguments can be passed to this combinator.  For each base
     * function, a recursive adapter is formed (like {@code recur}
     * above).  The sequence of recursive adapters is passed as
     * initial arguments to each base function.  Here is pseudocode
     * that corresponds to three base functions:
     * <blockquote><pre>{@code
     * R recursive(A... a) {
     *   return base(&recursive, &recursive2, &recursive3, a...);
     * }
     * R2 recursive2(A2... a2) {
     *   return base2(&recursive, &recursive2, &recursive3, a2...);
     * }
     * R3 recursive3(A3... a3) {
     *   return base2(&recursive, &recursive2, &recursive3, a3...);
     * }
     * R base(MethodHandle recur, MethodHandle recur2,
     *        MethodHandle recur3, A... a) {
     *   ... if (no recursion)  return f(a);  ...
     *   var r2 = recur2.invokeExact(a2...);
     *   var r3 = recur3.invokeExact(a3...);
     *   ... do stuff with r2, r3, etc. ...
     * }
     * R2 base2(MethodHandle recur, MethodHandle recur2,
     *        MethodHandle recur3, A2... a2) { ... }
     * R3 base3(MethodHandle recur, MethodHandle recur2,
     *        MethodHandle recur3, A3... a3) { ... }
     * }</pre></blockquote>
     *
     * @apiNote Example:
     * {@snippet lang="java" :
     * // classic recursive implementation of the factorial function
     * static int base(MethodHandle recur, int k) throws Throwable {
     *   if (k <= 1)  return 1;
     *   return k * (int) recur.invokeExact(k - 1);
     * }
     * // assume MH_base is a handle to the above method
     * MethodHandle recur = MethodHandles.recursive(MH_base);
     * assertEquals(120, recur.invoke(5));
     * }
     * <p>
     * A constructed recursive method handle is made varargs
     * if its corresponding base method handle is varargs.
     * @implSpec
     * For a single base function, this produces a result equivalent to:
     * <pre>{@code
     * class Holder {
     *   final MethodHandle recur;
     *   static final MH_recur = ...;  //field getter
     *   Holder(MethodHandle base) {
     *     recur = filterArguments(base, 0, MH_recur).bindTo(this);
     *   }
     * }
     * return new Holder(base).recur;
     * }</pre>
     * @param base the logic of the function to make recursive
     * @param moreBases additional base functions to be made mutually recursive
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if any base function does not accept
     *          the required leading arguments of type {@code MethodHandle}
     *
     * @return a method handle which invokes the (first) base function
     *         on the incoming arguments, with recursive versions of the
     *         base function (or functions) prepended as extra arguments
     *
     * @since Valhalla
     */
    static MethodHandle recursive(MethodHandle base, MethodHandle... moreBases) {
        // freeze the varargs and check for nulls:
        List<MethodHandle> bases2 = List.of(moreBases);
        int baseCount = 1 + bases2.size();
        recursiveChecks(base, baseCount);
        for (var base2 : bases2) { recursiveChecks(base2, baseCount); }
        class Holder {
            final MethodHandle recur;
            final List<MethodHandle> recurs2;
            MethodHandle recurs2(int i) { return recurs2.get(i); }
            Holder() {
                // Feed the first baseCount parameters of each base
                // with a fetch of each recur, so we can bind to this:
                var fetchers = new MethodHandle[baseCount];
                fetchers[0] = MH_recur;
                for (int pos = 1; pos < fetchers.length; pos++) {
                    int i = pos-1;  // index into recurs2
                    fetchers[pos] = MethodHandles.insertArguments(MH_recurs2, 1, i);
                }
                this.recur = makeRecur(base, fetchers);
                if (baseCount == 1) {
                    this.recurs2 = List.of();
                } else {
                    var recurs2 = new MethodHandle[baseCount-1];
                    for (int i = 0; i < recurs2.length; i++) {
                        recurs2[i] = makeRecur(bases2.get(i), fetchers);
                    }
                    this.recurs2 = List.of(recurs2);
                }
            }
            MethodHandle makeRecur(MethodHandle base, MethodHandle[] fetchers) {
                var adapt = filterArguments(base, 0, fetchers);
                for (int pos = 0; pos < fetchers.length; pos++) {
                    adapt = adapt.bindTo(this);
                }
                return adapt.withVarargs(base.isVarargsCollector());
            }
            static final MethodHandle MH_recur, MH_recurs2;
            static {
                try {
                    MH_recur = MethodHandles.lookup()
                            .findGetter(Holder.class, "recur", MethodHandle.class);
                    MH_recurs2 = MethodHandles.lookup()
                            .findVirtual(Holder.class, "recurs2",
                                    methodType(MethodHandle.class, int.class));
                } catch (ReflectiveOperationException ex) {
                    throw new InternalError(ex);
                }
            }
        }
        return new Holder().recur;
    }

    private static void recursiveChecks(MethodHandle base, int baseCount) {
        MethodType mt = base.type();  // implicit null check
        boolean wrong = (mt.parameterCount() < baseCount);
        for (int i = 0; i < baseCount && !wrong; i++) {
            if (mt.parameterType(i) != MethodHandle.class) {
                wrong = true;
            }
        }
        if (!wrong)  return;
        throw new IllegalArgumentException("missing leading MethodHandle parameters: " + mt);
    }

}
