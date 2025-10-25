/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import jdk.internal.access.JavaLangInvokeAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.Unsafe;
import jdk.internal.value.LayoutIteration;
import jdk.internal.value.ValueClass;

import sun.invoke.util.Wrapper;

import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.filterArguments;
import static java.lang.invoke.MethodHandles.foldArguments;
import static java.lang.invoke.MethodHandles.guardWithTest;
import static java.lang.invoke.MethodHandles.permuteArguments;
import static java.lang.invoke.MethodType.methodType;
import static java.lang.runtime.ObjectMethods.primitiveEquals;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Implementation for Object::equals and Object::hashCode for value objects.
 *
 * ValueObjectMethods::isSubstitutable and valueObjectHashCode are
 * private entry points called by VM.
 */
final class ValueObjectMethods {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private ValueObjectMethods() {}
    private static final boolean VERBOSE =
            System.getProperty("value.bsm.debug") != null;
    private static final int MAX_NODE_VISITS =
            Integer.getInteger("jdk.value.recursion.threshold", Integer.MAX_VALUE);
    private static final JavaLangInvokeAccess JLIA = SharedSecrets.getJavaLangInvokeAccess();

    static class MethodHandleBuilder {
        private static final HashMap<Class<?>, MethodHandle> primitiveSubstitutable = new HashMap<>();

        static {
            primitiveSubstitutable.putAll(primitiveEquals); // adopt all the primitive eq methods
            primitiveSubstitutable.put(float.class,
                    findStatic("eqValue", methodType(boolean.class, float.class, float.class)));
            primitiveSubstitutable.put(double.class,
                    findStatic("eqValue", methodType(boolean.class, double.class, double.class)));
        }

        static Stream<MethodHandle> getterStream(Class<?> type, Comparator<MethodHandle> comparator) {
            // filter static fields
            List<MethodHandle> mhs = LayoutIteration.ELEMENTS.get(type);
            if (comparator != null) {
                mhs = new ArrayList<>(mhs);
                mhs.sort(comparator);
            }
            return mhs.stream();
        }

        static MethodHandle hashCodeForType(Class<?> type) {
            if (type.isPrimitive()) {
                int index = Wrapper.forPrimitiveType(type).ordinal();
                return HASHCODE[index];
            } else {
                return HASHCODE[Wrapper.OBJECT.ordinal()].asType(methodType(int.class, type));
            }
        }

        static MethodHandle builtinPrimitiveSubstitutable(Class<?> type) {
            return primitiveSubstitutable.get(type);
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

        private static List<Class<?>> valueTypeFields(Class<?> type) {
            return LayoutIteration.ELEMENTS.get(type).stream()
                    .<Class<?>>map(mh -> mh.type().returnType())
                    .filter(ValueClass::isConcreteValueClass)
                    .distinct()
                    .toList();
        }

        /*
         * Produces a MethodHandle that returns boolean if two value objects
         * are substitutable.  The method type is (V, V)boolean.
         */
        static MethodHandle valueTypeEquals(Class<?> type) {
            var builder = METHOD_HANDLE_BUILDERS.get(type);
            if (builder == null) {
                builder = newBuilder(type);
            }
            return builder.equalsTarget();
        }

        /*
         * Produces a MethodHandle that computes the hash code of a value object.
         * The method type of the return MethodHandle is (V)int.
         */
        static MethodHandle valueTypeHashCode(Class<?> type) {
            var builder = METHOD_HANDLE_BUILDERS.get(type);
            if (builder == null) {
                builder = newBuilder(type);
            }
            return builder.hashCodeTarget();
        }

        /*
         * Produces a MethodHandle that returns boolean if the given non-recursively typed
         * fields of the two value objects are substitutable. The method type is (V, V)boolean
         */
        static MethodHandle valueTypeEquals(Class<?> type, List<MethodHandle> getters) {
            assert ValueClass.isConcreteValueClass(type);

            MethodType mt = methodType(boolean.class, type, type);
            MethodHandle instanceTrue = dropArguments(TRUE, 0, type, Object.class).asType(mt);
            MethodHandle instanceFalse = dropArguments(FALSE, 0, type, Object.class).asType(mt);
            MethodHandle accumulator = dropArguments(TRUE, 0, type, type);
            for (MethodHandle getter : getters) {
                Class<?> ftype = fieldType(getter);
                var eq = substitutableInvoker(ftype).asType(methodType(boolean.class, ftype, ftype));
                var thisFieldEqual = filterArguments(eq, 0, getter, getter);
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
         * Produces a MethodHandle that returns the hash code computed from
         * the given non-recursively-typed fields of a value object.
         * The method type is (V)int.
         */
        static MethodHandle valueTypeHashCode(Class<?> type, List<MethodHandle> getters) {
            assert ValueClass.isConcreteValueClass(type);

            MethodHandle target = dropArguments(constant(int.class, SALT), 0, type);
            MethodHandle classHasher = dropArguments(hashCodeForType(Class.class).bindTo(type), 0, type);
            MethodHandle hashCombiner = dropArguments(HASH_COMBINER, 2, type);
            MethodHandle accumulator = foldArguments(foldArguments(hashCombiner, 1, classHasher), 0, target);
            for (MethodHandle getter : getters) {
                Class<?> ft = fieldType(getter);
                // For primitive types or reference types, this calls Objects::hashCode.
                // For value objects and the hashCode method is not overridden,
                // VM will call valueObjectHashCode to compute the hash code.
                var hasher = hashCodeForType(ft);
                var hashThisField = filterArguments(hasher, 0, getter);    // (R)I
                var combineHashes = foldArguments(hashCombiner, 1, hashThisField);
                accumulator = foldArguments(combineHashes, 0, accumulator);
            }
            return accumulator;
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

        private static int[] newCounter(int[] counter) {
            return new int[] { counter[0] };
        }

        private static boolean recurValueEq(MethodHandle target, Object o1, Object o2, int[] counter) {
            assert counter[0] > 0;

            if (o1 == null && o2 == null) return true;
            if (o1 == null || o2 == null) return false;
            if (o1.getClass() != o2.getClass()) return false;

            if (--counter[0] == 0) {
                throw new StackOverflowError("fail to evaluate == for value class " + o1.getClass().getName());
            }

            try {
                return (boolean) target.invoke(o1, o2, counter);
            } catch (Error|RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new InternalError(e);
            }
        }

        private static int recurValueHashCode(MethodHandle target, Object o, int[] counter) {
            assert counter[0] > 0;

            if (o == null) return 0;

            if (--counter[0] == 0) {
                throw new StackOverflowError("fail to evaluate hashCode for value class " + o.getClass().getName());
            }

            try {
                return (int) target.invoke(o, counter);
            } catch (Error|RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new InternalError(e);
            }
        }

        private static final MethodHandle FALSE = constant(boolean.class, false);
        private static final MethodHandle TRUE = constant(boolean.class, true);
        // Substitutability test for float
        private static boolean eqValue(float a, float b) {
            return Float.floatToRawIntBits(a) == Float.floatToRawIntBits(b);
        }
        // Substitutability test for double
        private static boolean eqValue(double a, double b) {
            return Double.doubleToRawLongBits(a) == Double.doubleToRawLongBits(b);
        }
        private static final MethodHandle OBJECT_EQUALS =
                findStatic("eq", methodType(boolean.class, Object.class, Object.class));
        private static final MethodHandle IS_SAME_VALUE_CLASS =
                findStatic("isSameValueClass", methodType(boolean.class, Object.class, Object.class));
        private static final MethodHandle IS_NULL =
                findStatic("isNull", methodType(boolean.class, Object.class, Object.class));
        private static final MethodHandle HASH_COMBINER =
                findStatic("hashCombiner", methodType(int.class, int.class, int.class));
        private static final MethodHandle[] HASHCODE = initHashCode();
        private static final MethodHandle RECUR_VALUE_EQ =
                findStatic("recurValueEq", methodType(boolean.class, MethodHandle.class, Object.class, Object.class, int[].class));
        private static final MethodHandle RECUR_VALUE_HASHCODE =
                findStatic("recurValueHashCode", methodType(int.class, MethodHandle.class, Object.class, int[].class));
        private static final MethodHandle NEW_COUNTER =
                findStatic("newCounter", methodType(int[].class, int[].class));

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
            SALT = Integer.getInteger("value.bsm.salt", value);
        }

        static MethodHandleBuilder newBuilder(Class<?> type) {
            assert ValueClass.isConcreteValueClass(type);

            Deque<Class<?>> deque = new ArrayDeque<>();
            deque.add(type);
            Map<Class<?>, MethodHandleBuilder> visited = new HashMap<>();
            var builder = new MethodHandleBuilder(type, deque, visited);
            visited.put(type, builder);
            return builder;
        }

        enum Status {
            NOT_START,
            IN_PROGRESS,
            TRAVERSAL_DONE,
            READY
        }

        final Class<?> type;
        final List<Class<?>> fieldValueTypes;
        // a map of the type of a field T to a cycle of T -> ... -> V
        // where V is this builder's value type
        final Map<Class<?>, List<Class<?>>> cyclicMembers = new HashMap<>();
        // recursively-typed fields declared in this builder's value type
        final Set<Class<?>> recurFieldTypes = new HashSet<>();
        final Deque<Class<?>> path;
        final Map<Class<?>, MethodHandleBuilder> visited;;
        volatile Status status = Status.NOT_START;
        volatile MethodHandle equalsTarget;
        volatile MethodHandle hashCodeTarget;

        static final VarHandle STATUS_HANDLE;
        static {
            VarHandle vh = null;
            try {
                vh = MethodHandles.lookup().findVarHandle(MethodHandleBuilder.class, "status", Status.class);
            } catch (ReflectiveOperationException e) {
                throw new InternalError(e);
            }
            STATUS_HANDLE = vh;
        }

        /**
         * Constructs a new MethodHandleBuilder for the given value type.
         *
         * @param type a value class
         * @param path the graph traversal
         * @param visited a map of a visited type to a builder
         */
        private MethodHandleBuilder(Class<?> type, Deque<Class<?>> path, Map<Class<?>, MethodHandleBuilder> visited) {
            assert ValueClass.isConcreteValueClass(type) : type;
            this.type = type;
            this.fieldValueTypes = valueTypeFields(type);
            this.path = path;
            this.visited = visited;
            if (VERBOSE) {
                System.out.println("New builder for " + type.getName() + " " + path);
            }
        }

        /*
         * Returns a method handle that implements equals method for this builder's value class.
         */
        MethodHandle equalsTarget() {
            if (status != Status.READY)
                throw new IllegalStateException(type.getName() + " not ready");

            var mh = equalsTarget;
            if (mh != null) return mh;

            generateMethodHandle();
            return equalsTarget;
        }

        /*
         * Returns a method handle that implements hashCode method for this builder's value class.
         */
        MethodHandle hashCodeTarget() {
            if (status != Status.READY)
                throw new IllegalStateException(type.getName() + " not ready");

            var mh = hashCodeTarget;
            if (mh != null) return mh;

            generateMethodHandle();
            return hashCodeTarget;
        }

        /*
         * Build the graph for this builder's value type.  Detect all cycles.
         * This builder after this method returns is in DONE_TRAVERSAL or READY status.
         *
         * A builder for type V will change to READY status when the entire graph for V
         * is traversed (i.e. all builders in this graph are in DONE_TRAVERSAL or READY
         * status).
         */
        MethodHandleBuilder build() {
            if (status == Status.READY) return this;

            if (!STATUS_HANDLE.compareAndSet(this, Status.NOT_START, Status.IN_PROGRESS)) {
                throw new RuntimeException(type.getName() + " in progress");
            }

            // traverse the graph and find all cycles
            detectCycles();

            if (!STATUS_HANDLE.compareAndSet(this, Status.IN_PROGRESS, Status.TRAVERSAL_DONE)) {
                throw new RuntimeException(type.getName() + " failed to set done traversal. " + status);
            }

            // Check if this node V is ready for building equals/hashCode method handles.
            // V is ready if the types of all its fields are done traversal.
            if (ready()) {
                // Do a pass on all the cycles containing V.  V is ready.
                // If a node N in the cycle has completed the traversal (i.e. cycles are detected),
                // call ready() on N to update its status if ready.
                for (List<Class<?>> cycle : cyclicMembers.values()) {
                    cycle.stream().filter(c -> c != type)
                                  .map(visited::get)
                                  .filter(b -> b.status == Status.TRAVERSAL_DONE)
                                  .forEach(MethodHandleBuilder::ready);
                }
            }
            return this;
        }

        /*
         * Traverses the graph and finds all cycles.
         */
        private void detectCycles() {
            LinkedList<Class<?>> deque = new LinkedList<>();
            deque.addAll(fieldValueTypes);
            while (!deque.isEmpty()) {
                Class<?> n = deque.pop();
                // detect cyclic membership
                if (path.contains(n)) {
                    List<Class<?>> cycle = new ArrayList<>();
                    Iterator<Class<?>> iter = path.iterator();
                    while (iter.hasNext()) {
                        Class<?> c = iter.next();
                        cycle.add(c);
                        if (c == n) break;
                    }
                    cyclicMembers.put(n, cycle);
                    path.pop();
                    continue;
                }

                try {
                    path.push(n);
                    if (!visited.containsKey(n)) {
                        // Duplicate the path and pass it to an unvisited node
                        Deque<Class<?>> newPath = new ArrayDeque<>();
                        newPath.addAll(path);
                        visited.computeIfAbsent(n, c -> new MethodHandleBuilder(n, newPath, visited));
                    }

                    var builder = visited.get(n);
                    switch (builder.status) {
                        case NOT_START -> builder.build();
                        case TRAVERSAL_DONE -> builder.ready();
                    }
                } finally {
                    path.pop();
                }
            }

            // propagate the cycles to the recursively-typed value classes
            // For each cycle A -> B -> C -> A, the cycle is recorded in all
            // the nodes (A, B, and C) in this cycle.
            for (Map.Entry<Class<?>, List<Class<?>>> e : cyclicMembers.entrySet()) {
                Class<?> c = e.getKey();
                List<Class<?>> cycle = e.getValue();
                var builder = visited.get(c);
                for (Class<?> ft : cycle) {
                    if (ft != c && builder.fieldValueTypes.contains(ft)) {
                        var v = builder.cyclicMembers.put(ft, cycle);
                        assert v == null || cycle.equals(v) : "mismatched cycle: " + v + " vs " + cycle;
                    }
                }
            }
        }

        /*
         * Tests if this builder is ready for generating equals and hashCode
         * method handles for the value class.
         *
         * This builder is ready if and only if the type graph of all its fields
         * are traversed and all cycles are detected.
         *
         * Before setting to READY, the recursively-typed fields are recorded
         * that includes all types in the cycles and the field types which
         * references recursive types
         */
        private boolean ready() {
            if (status == Status.READY) return true;

            boolean inProgress = fieldValueTypes.stream().map(visited::get)
                                                .anyMatch(b -> b.status == Status.IN_PROGRESS);
            if (inProgress)
                return false;

            // collect the recursively-typed value classes required by this method handle
            // all types in the cycles and the field types which references recursive types
            recurFieldTypes.addAll(cyclicMembers.keySet());
            for (Class<?> c : fieldValueTypes) {
                if (c == type) continue;

                // if this field type references a recursive type
                var b = visited.get(c);
                if (b.cyclicMembers.size() > 0 || b.recurFieldTypes.size() > 0)
                    recurFieldTypes.add(c);
            };

            // Finished recording recursively-typed fields.  Set to READY.
            if (!STATUS_HANDLE.compareAndSet(this, Status.TRAVERSAL_DONE, Status.READY)) {
                throw new RuntimeException(type.getName() + " failed to set READY. " + status);
            }
            return true;
        }

        void generateMethodHandle() {
            if (status != Status.READY)
                throw new IllegalStateException(type.getName() + " not ready");

            // non-recursive value type
            if (recurFieldTypes.isEmpty()) {
                if (cyclicMembers.size() > 0)
                    throw new RuntimeException(type.getName() + " should not reach here");

                this.equalsTarget = valueTypeEquals(type, getterStream(type, TYPE_SORTER).toList());
                this.hashCodeTarget = valueTypeHashCode(type, getterStream(type, null).toList());
                return;
            }

            if (VERBOSE) {
                System.out.println(debugString());
            }

            // generate the base function for each recursive type
            // boolean base1(MethodHandle entry, MethodHandle base1, MethodHandle base2,....., Object o1, Object o2, int[] counter)
            // :
            // boolean baseN(MethodHandle entry, MethodHandle base1, MethodHandle base2,....., Object o1, Object o2, int[] counter)
            //
            List<Class<?>> recursiveTypes = aggregateRecursiveTypes();
            Map<Class<?>, MethodHandle> bases = new LinkedHashMap<>();
            Map<Class<?>, MethodHandle> hashCodeBases = new LinkedHashMap<>();
            for (Class<?> c : recursiveTypes) {
                bases.put(c, visited.get(c).generateSubstBase(recursiveTypes));
                hashCodeBases.put(c, visited.get(c).generateHashCodeBase(recursiveTypes));
            }

            var handles = bases.values().stream().toArray(MethodHandle[]::new);
            var hashCodeHandles = hashCodeBases.values().stream().toArray(MethodHandle[]::new);

            // The entry point for equals for this value type T looks like this:
            //
            // boolean entry(MethodHandle entry, MethodHandle base1, MethodHandle base2,....., Object o1, Object o2, int[] counter) {
            //    int[] newCounter = new int[] { counter[0] } ;
            //    return baseT(o1, o2, newCounter);
            // }
            this.equalsTarget = newValueEquals(recursiveTypes, handles);
            this.hashCodeTarget = newValueHashCode(recursiveTypes, hashCodeHandles);

            // Precompute the method handles for all recursive data types in the cycles
            // They share the generated base method handles.
            var cycles = cyclicMembers.values().stream().flatMap(List::stream)
                                .filter(c -> c != type)
                                .collect(toMap(Function.identity(), visited::get));
            for (Class<?> n : cycles.keySet()) {
                var builder = cycles.get(n);
                if (builder.status != Status.READY) {
                    throw new InternalError(type.getName() + " is not ready: " + status);
                }

                var mh = builder.equalsTarget;
                var mh2 = builder.hashCodeTarget;
                if (mh != null && mh2 != null) {
                    continue;
                }

                // precompute the method handles for each recursive type in the cycle
                if (mh == null) {
                    builder.equalsTarget = builder.newValueEquals(recursiveTypes, handles);
                }
                if (mh2 == null) {
                    builder.hashCodeTarget = builder.newValueHashCode(recursiveTypes, hashCodeHandles);
                }
            }

            // cache the builders with precomputed method handles in the cache
            synchronized (CACHED_METHOD_HANDLE_BUILDERS) {
                for (Class<?> n : cycles.keySet()) {
                    try {
                        // the builder is added to the builder cache and propapate to
                        // the class value
                        CACHED_METHOD_HANDLE_BUILDERS.computeIfAbsent(n, cycles::get);
                        METHOD_HANDLE_BUILDERS.get(n);
                    } finally {
                        // Remove it from the builder cache once it's in class value
                        CACHED_METHOD_HANDLE_BUILDERS.remove(n);
                    }
                }
            }

            // equals and hashCode are generated.  Clear the path and visited builders.
            clear();
        }

        private void clear() {
            path.clear();
            visited.clear();
        }

        /*
         * Aggregates all recursive data types for this builder's value types.
         * The result is used in generating a recursive method handle
         * for this builder's value type.
         *
         * A graph of V:
         * V -> P -> V
         *   -> N -> N (self-recursive)
         *   -> E -> F -> E
         *
         * V, P, N, E, F are the mutual recursive types for V. The recursive method handle
         * for V is created with the base functions for V, P, N, E, F and it can mutually
         * call the recursive method handle for these types.  Specifically, MH_V calls
         * MH_P which calls MH_V, MH_N which calls itself, and MH_E which calls MH_F.
         */
        private List<Class<?>> aggregateRecursiveTypes() {
            boolean ready = true;
            for (List<Class<?>> cycle : cyclicMembers.values()) {
                // ensure all nodes in all cycles that are done traversal and ready for
                // method handle generation
                cycle.stream().filter(c -> c != type)
                              .map(visited::get)
                              .filter(b -> b.status == Status.TRAVERSAL_DONE)
                              .forEach(MethodHandleBuilder::ready);

                // check the status
                ready = ready && cycle.stream().filter(c -> c != type)
                                      .map(visited::get)
                                      .allMatch(b -> b.status == Status.READY);
            }

            if (!ready) {
                throw new IllegalStateException(type.getName() + " " + status);
            }

            /*
             * Traverse the graph for V to find all mutual recursive types for V.
             *
             * Node T is a mutual recursive type for V if any of the following:
             * 1. T is a recursively-typed field in V
             * 2. T is a type involved the cycles from V ... -> T ... -> V
             * 3. T is a mutual recursive type for N where N is a mutual recursive type for V.
             */
            Deque<Class<?>> deque = new ArrayDeque<>();
            List<Class<?>> recurTypes = new ArrayList<>();
            recurTypes.add(type);
            Stream.concat(recurFieldTypes.stream(),
                          cyclicMembers.values().stream().flatMap(List::stream))
                  .filter(Predicate.not(deque::contains)).forEach(deque::add);
            while (!deque.isEmpty()) {
                Class<?> c = deque.pop();
                if (recurTypes.contains(c)) continue;

                recurTypes.add(c);

                var builder = visited.get(c);
                Stream.concat(builder.recurFieldTypes.stream(),
                              builder.cyclicMembers.values().stream().flatMap(List::stream))
                      .filter(n -> !recurTypes.contains(n) && !deque.contains(n))
                      .forEach(deque::push);
            }
            return recurTypes;
        }

        /*
         * Create a new method handle that implements equals(T, Object) for value class T
         * for this builder using the given base method handles.  The return method handle
         * is capable of recursively calling itself for value class T whose entry point:
         *   boolean entry(MethodHandle entry, MethodHandle base1, MethodHandle base2, ..., Object o1, Object o2, int[] counter) {
         *       int[] newCounter = new int[] { counter[0] };
         *       return baseT(o1, o2, newCounter);
         *   }
         *
         * The counter is used to keep of node visits and throw StackOverflowError
         * if the counter gets "unreasonably" large of a cyclic value graph
         * (regardless of how many real stack frames were consumed.)
         */
        MethodHandle newValueEquals(List<Class<?>> recursiveTypes, MethodHandle[] bases) {
            var entry = equalsEntry(recursiveTypes);
            var mh = MethodHandles.insertArguments(recursive(entry, bases), 2, new int[] {MAX_NODE_VISITS});
            return mh.asType(methodType(boolean.class, type, type));
        }

        /*
         * Create a new method handle that implements hashCode(T) for value class T
         * for this builder using the given base method handles.  The return method handle
         * is capable of recursively calling itself for value class T whose entry point:
         *   boolean entry(MethodHandle entry, MethodHandle base1, MethodHandle base2, ..., Object o, int[] counter) {
         *       int[] newCounter = new int[] { counter[0] };
         *       return baseT(o, newCounter);
         *   }
         *
         * The counter is used to keep of node visits and throw StackOverflowError
         * if the counter gets "unreasonably" large of a cyclic value graph
         * (regardless of how many real stack frames were consumed.)
         */
        MethodHandle newValueHashCode(List<Class<?>> recursiveTypes, MethodHandle[] bases) {
            var entry = hashCodeEntry(recursiveTypes);
            var mh = MethodHandles.insertArguments(recursive(entry, bases), 1, new int[] {MAX_NODE_VISITS});
            return mh.asType(methodType(int.class, type));
        }

        /*
         * Create a method handle where the first N+1 parameters are MethodHandle and
         * N is the number of the recursive value types and followed with
         * Object, Object and int[] parameters.  The pseudo code looks like this:
         *
         * boolean eq(MethodHandle entry, MethodHandle base1, MethodHandle base2, ..., Object o1, Object o2, int[] counter) {
         *    if (o1 == null && o2 == null) return true;
         *    if (o1 == null || o2 == null) return false;
         *    if (o1.getClass() != o2. getClass()) return false;
         *
         *    int[] newCounter = new int[] { counter[0]; }
         *    return (boolean) baseT.invoke(o1, o2, newCounter);
         * }
         */
        MethodHandle equalsEntry(List<Class<?>> recursiveTypes) {
            List<Class<?>> leadingMHParams = new ArrayList<>();
            // the first MethodHandle parameter is this entry point
            // followed with MethodHandle parameter for each mutual exclusive value class
            int mhParamCount = recursiveTypes.size()+1;
            for (int i=0; i < mhParamCount; i++) {
                leadingMHParams.add(MethodHandle.class);
            }

            MethodType mt = methodType(boolean.class, Stream.concat(leadingMHParams.stream(), Stream.of(type, type, int[].class))
                    .collect(toList()));
            var allParameters = mt.parameterList();
            MethodHandle instanceTrue = dropArguments(TRUE, 0, allParameters).asType(mt);
            MethodHandle instanceFalse = dropArguments(FALSE, 0, allParameters).asType(mt);
            MethodHandle isNull = dropArguments(dropArguments(IS_NULL, 0, leadingMHParams), mhParamCount+2, int[].class).asType(mt);
            MethodHandle isSameValueType = dropArguments(dropArguments(IS_SAME_VALUE_CLASS, 0, leadingMHParams), mhParamCount+2, int[].class).asType(mt);

            int index = recursiveTypes.indexOf(type);
            var mtype = methodType(boolean.class, Stream.concat(leadingMHParams.stream(), Stream.of(type, type, int[].class)).collect(toList()));
            var recurEq = RECUR_VALUE_EQ.asType(methodType(boolean.class, MethodHandle.class, type, type, int[].class));
            var eq = permuteArguments(recurEq, mtype, index+1, mhParamCount, mhParamCount+1, mhParamCount+2);
            eq = filterArguments(eq, mhParamCount+2, NEW_COUNTER);

            // if both arguments are null, return true;
            // otherwise return the method handle corresponding to this type
            return guardWithTest(isNull,
                                 instanceTrue,
                                 guardWithTest(isSameValueType, eq, instanceFalse));
        }

        /*
         * A base method for substitutability test for a recursive data type,
         * a value class with cyclic membership.
         *
         * The signature of this base method is:
         *    boolean base(MethodHandle entry, MethodHandle base1, MethodHandle base2, ..., V o1, V o2, int[] counter)
         *
         * where the first N+1 parameters are MethodHandle and N is the number of
         * the recursive value types and followed with Object, Object and int[] parameters.
         *
         * This method first calls the method handle that tests the substitutability
         * of all fields that are not recursively-typed, if any, and then test
         * the substitutability of the fields that are of each recursive value type.
         * The pseudo code looks like this:
         *
         * boolean base(MethodHandle entry, MethodHandle base1, MethodHandle base2, ..., V o1, V o2, int[] counter) {
         *    if (o1 == null && o2 == null) return true;
         *    if (o1 == null || o2 == null) return false;
         *    if (o1.getClass() != o2. getClass()) return false;
         *
         *    for each non-recursively-typed field {
         *        if (field value of o1 != field value of o2) return false;
         *    }
         *
         *    for each recursively-typed field of type T {
         *        if (--counter[0] == 0) throw new StackOverflowError();
         *        // baseT is the method handle corresponding to the recursive type T
         *        boolean rc = (boolean) baseT.invoke(o1, o2, counter);
         *        if (!rc) return false;
         *    }
         *    return true;
         * }
         */
        MethodHandle generateSubstBase(List<Class<?>> recursiveTypes) {
            List<MethodHandle> nonRecurGetters = new ArrayList<>();
            Map<Class<?>, List<MethodHandle>> recurGetters = new LinkedHashMap<>();
            getterStream(type, TYPE_SORTER).forEach(mh -> {
                Class<?> ft = fieldType(mh);
                if (!this.recurFieldTypes.contains(ft)) {
                    nonRecurGetters.add(mh);
                } else {
                    assert recursiveTypes.contains(ft);
                    recurGetters.computeIfAbsent(ft, t -> new ArrayList<>()).add(mh);
                }
            });

            // The first parameter is the method handle of the entry point
            // followed with one MethodHandle for each recursive value type
            List<Class<?>> leadingMHParams = new ArrayList<>();
            int mhParamCount = recursiveTypes.size()+1;
            for (int i=0; i < mhParamCount; i++) {
                leadingMHParams.add(MethodHandle.class);
            }

            MethodType mt = methodType(boolean.class,
                                       Stream.concat(leadingMHParams.stream(), Stream.of(type, type, int[].class)).collect(toList()));
            var allParameters = mt.parameterList();

            var instanceTrue = dropArguments(TRUE, 0, allParameters).asType(mt);
            var instanceFalse = dropArguments(FALSE, 0, allParameters).asType(mt);
            var accumulator = dropArguments(TRUE, 0, allParameters).asType(mt);
            var isNull = dropArguments(dropArguments(IS_NULL, 0, leadingMHParams), mhParamCount+2, int[].class).asType(mt);
            var isSameValueType = dropArguments(dropArguments(IS_SAME_VALUE_CLASS, 0, leadingMHParams), mhParamCount+2, int[].class).asType(mt);

            // This value class contains cyclic membership.
            // Create a method handle that first calls the method handle that tests
            // the substitutability of all fields that are not recursively-typed, if any,
            // and then test the substitutability of the fields that are of each recursive
            // value type.
            //
            // Method handle for the substitutability test for recursive types is built
            // before that for non-recursive types.
            for (Map.Entry<Class<?>, List<MethodHandle>> e : recurGetters.entrySet()) {
                Class<?> ft = e.getKey();
                int index = recursiveTypes.indexOf(ft);
                var mtype = methodType(boolean.class,
                                Stream.concat(leadingMHParams.stream(), Stream.of(ft, ft, int[].class)).collect(toList()));
                var recurEq = RECUR_VALUE_EQ.asType(methodType(boolean.class, MethodHandle.class, ft, ft, int[].class));
                var eq = permuteArguments(recurEq, mtype, index+1, mhParamCount, mhParamCount+1, mhParamCount+2);
                for (MethodHandle getter : e.getValue()) {
                    assert ft == fieldType(getter);
                    var thisFieldEqual = filterArguments(eq, mhParamCount, getter, getter);
                    accumulator = guardWithTest(thisFieldEqual, accumulator, instanceFalse);
                }
            }

            if (nonRecurGetters.isEmpty()) {
                // if both arguments are null, return true;
                // otherwise return accumulator;
                return guardWithTest(isNull,
                                     instanceTrue,
                                     guardWithTest(isSameValueType, accumulator, instanceFalse));
            } else {
                // method handle for substitutability test of the non-recursive-typed fields
                var mh = valueTypeEquals(type, nonRecurGetters);
                mh = dropArguments(dropArguments(mh, 0, leadingMHParams), mhParamCount+2, int[].class).asType(mt);
                return guardWithTest(mh, accumulator, instanceFalse);
            }
        }

        /*
         * Create a method handle where the first N+1 parameters are MethodHandle and
         * N is the number of the recursive value types and followed with
         * Object and int[] parameters.  The pseudo code looks like this:
         *
         * int hashCode(MethodHandle entry, MethodHandle base1, MethodHandle base2, ..., Object o, int[] counter) {
         *    int[] newCounter = new int[] { counter[0]; }
         *    return (int) baseT.invoke(o, newCounter);
         * }
         */
        MethodHandle hashCodeEntry(List<Class<?>> recursiveTypes) {
            List<Class<?>> leadingMHParams = new ArrayList<>();
            int mhParamCount = recursiveTypes.size()+1;
            // the first MethodHandle parameter is this entry point
            // followed with MethodHandle parameter for each mutual exclusive value class
            for (int i=0; i < mhParamCount; i++) {
                leadingMHParams.add(MethodHandle.class);
            }

            int index = recursiveTypes.indexOf(type);
            var mtype = methodType(int.class, Stream.concat(leadingMHParams.stream(), Stream.of(type, int[].class)).collect(toList()));
            var recurHashCode = RECUR_VALUE_HASHCODE.asType(methodType(int.class, MethodHandle.class, type, int[].class));
            var mh = permuteArguments(recurHashCode, mtype, index+1, mhParamCount, mhParamCount+1);
            return filterArguments(mh, mhParamCount+1, NEW_COUNTER);
        }

        /**
         * A base method for computing the hashcode for a recursive data type,
         * a value class with cyclic membership.
         *
         * The signature of this base method is:
         *    int base(MethodHandle entry, MethodHandle base1, MethodHandle base2, ..., V o, int[] counter)
         *
         * where the first N+1 parameters are MethodHandle and N is the number of
         * the recursive value types and followed with Object and int[] parameters.
         *
         * This method will first invoke a method handle to compute the hash code
         * of the not recursively-typed fields.  Then compute the hash code of the
         * remaining recursively-typed fields.
         */
        MethodHandle generateHashCodeBase(List<Class<?>> recursiveTypes) {
            assert status == Status.READY;

            List<MethodHandle> nonRecurGetters = new ArrayList<>();
            Map<Class<?>, List<MethodHandle>> recurGetters = new LinkedHashMap<>();
            getterStream(type, null).forEach(mh -> {
                Class<?> ft = fieldType(mh);
                if (!this.recurFieldTypes.contains(ft)) {
                    nonRecurGetters.add(mh);
                } else {
                    assert recursiveTypes.contains(ft);
                    recurGetters.computeIfAbsent(ft, t -> new ArrayList<>()).add(mh);
                }
            });

            int mhParamCount = recursiveTypes.size()+1;
            List<Class<?>> leadingMHParams = new ArrayList<>();
            for (int i=0; i < mhParamCount; i++) {    // include entry point
                leadingMHParams.add(MethodHandle.class);
            }

            MethodType mt = methodType(int.class,
                                       Stream.concat(leadingMHParams.stream(), Stream.of(type, int[].class)).collect(toList()));
            var allParameters = mt.parameterList();
            var hashCombiner = dropArguments(HASH_COMBINER, 2, allParameters);
            var salt = dropArguments(constant(int.class, SALT), 0, allParameters);
            var classHasher = dropArguments(hashCodeForType(Class.class).bindTo(type), 0, allParameters);
            var accumulator = foldArguments(foldArguments(hashCombiner, 1, classHasher), 0, salt);
            for (MethodHandle getter : nonRecurGetters) {
                Class<?> ft = fieldType(getter);
                var hasher = dropArguments(hashCodeForType(ft), 0, leadingMHParams);
                var hashThisField = filterArguments(hasher, mhParamCount, getter);
                var combineHashes = foldArguments(hashCombiner, 1, hashThisField);
                accumulator = foldArguments(combineHashes, 0, accumulator);
            }

            for (Map.Entry<Class<?>, List<MethodHandle>> e : recurGetters.entrySet()) {
                Class<?> ft = e.getKey();
                int index = recursiveTypes.indexOf(ft);
                var mtype = methodType(int.class, Stream.concat(leadingMHParams.stream(), Stream.of(ft, int[].class)).collect(toList()));
                var recurHashCode = RECUR_VALUE_HASHCODE.asType(methodType(int.class, MethodHandle.class, ft, int[].class));
                var hasher = permuteArguments(recurHashCode, mtype, index + 1, mhParamCount, mhParamCount + 1);
                for (MethodHandle getter : e.getValue()) {
                    assert ft == fieldType(getter);
                    var hashThisField = filterArguments(hasher, mhParamCount, getter);
                    var combineHashes = foldArguments(hashCombiner, 1, hashThisField);
                    accumulator = foldArguments(combineHashes, 0, accumulator);
                }
            }
            return accumulator;
        }

        private String debugString() {
            StringBuilder sb = new StringBuilder();
            sb.append(type.getName()).append(" ").append(status).append("\n");
            sb.append(fieldValueTypes.stream().filter(c -> !recurFieldTypes.contains(c))
                            .map(Class::getName)
                            .collect(joining(" ", "  non-recursive types: ", "\n")));
            sb.append(recurFieldTypes.stream().map(Class::getName)
                            .collect(joining(" ", "  recursive types: ", "\n")));
            for (var n : cyclicMembers.keySet()) {
                List<Class<?>> cycle = cyclicMembers.get(n);
                sb.append("  cycle: ");
                int start = cycle.indexOf(n);
                for (int i=start; i < cycle.size(); i++ ) {
                    sb.append(cycle.get(i).getName()).append(" -> ");
                }
                for (int i=0; i < start; i++) {
                    sb.append(cycle.get(i).getName()).append(" -> ");
                }
                sb.append(n.getName()).append("\n");
            };
            return sb.toString();
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
     *     <li> For primitive types {@code float} and {@code double} the
     *          comparison uses the raw bits corresponding to {@link Float#floatToRawIntBits(float)}
     *          and {@link Double#doubleToRawLongBits(double)} respectively.
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
     * @see Float#floatToRawIntBits(float)
     * @see Double#doubleToRawLongBits(double)
     */
    private static <T> boolean isSubstitutable(T a, Object b) {
        if (VERBOSE) {
            System.out.println("substitutable " + a.getClass() + ": " + a + " vs " + b);
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
     *     returns a method handle representing {@link Float#floatToRawIntBits(float)} or
     *     {@link Double#doubleToRawLongBits(double)} respectively.
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
        if (type.isPrimitive()) {
            return MethodHandleBuilder.builtinPrimitiveSubstitutable(type);
        }
        if (ValueClass.isConcreteValueClass(type)) {
            return SUBST_TEST_METHOD_HANDLES.get(type);
        }
        return MethodHandleBuilder.referenceTypeEquals(type);
    }

    private static final ClassValue<MethodHandleBuilder> METHOD_HANDLE_BUILDERS = new ClassValue<>() {
        @Override protected MethodHandleBuilder computeValue(Class<?> type) {
            var builder = CACHED_METHOD_HANDLE_BUILDERS.get(type);
            if (builder == null) {
                builder = MethodHandleBuilder.newBuilder(type).build();
            }
            return builder;
        }
    };

    // This cache is only used to propagate the builders of mutual recursive types
    // A -> B -> C -> A as method handles for equals/hashCode for A, B, C are
    // all precomputed.  This map should only be non-empty only during the short
    // window propagating to the method handle builder class value.
    private static Map<Class<?>, MethodHandleBuilder> CACHED_METHOD_HANDLE_BUILDERS = new ConcurrentHashMap<>();

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
        Class<?> type = o.getClass();
        try {
            // Note: javac disallows user to call super.hashCode if user implemented
            // risk for recursion for experts crafting byte-code
            if (!type.isValue())
                throw new InternalError("must be value class: " + type.getName());

            return (int) HASHCODE_METHOD_HANDLES.get(type).invoke(o);
        } catch (Error|RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            if (VERBOSE) e.printStackTrace();
            throw new InternalError(e);
        }
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
     *   return base3(&recursive, &recursive2, &recursive3, a3...);
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

    private static boolean isSubstitutableAlt(Object a, Object b) {
      // This method assumes a and b are not null and their are both instances of the same value class
      final Unsafe U = UNSAFE;
      int[] map = U.getFieldMap(a.getClass());
      int nbNonRef = map[0];
      for (int i = 0; i < nbNonRef; i++) {
        int offset = map[i * 2 + 1];
        int size = map[i * 2 + 2];
        int nlong = size / 8;
        for (int j = 0; j < nlong; j++) {
          long la = U.getLong(a, offset);
          long lb = U.getLong(b, offset);
          if (la != lb) return false;
        }
        size -= nlong * 8;
        int nint = size / 4;
        for (int j = 0; j < nint; j++) {
          int ia = U.getInt(a, offset);
          int ib = U.getInt(b, offset);
          if (ia != ib) return false;
        }
        size -= nint * 4;
        int nshort = size / 2;
        for (int j = 0; j < nshort; j++) {
          short sa = U.getShort(a, offset);
          short sb = U.getShort(b, offset);
          if (sa != sb) return false;
        }
        size -= nshort * 2;
        for (int j = 0; j < size; j++) {
          byte ba = U.getByte(a, offset);
          byte bb = U.getByte(b, offset);
          if (ba != bb) return false;
        }
      }
      for (int i = nbNonRef * 2 + 1; i < map.length; i++) {
        int offset = map[i];
        Object oa = U.getReference(a, offset);
        Object ob = U.getReference(b, offset);
        if (oa != ob) return false;
      }
      return true;
    }
}
