/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.reflect.TypeContext;
import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.parser.SignatureParser;
import sun.reflect.generics.scope.Scope;
import sun.reflect.generics.tree.TypeSignature;
import sun.reflect.generics.visitor.Reifier;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class defines helper method to perform dynamic witness lookups.
 */
public class WitnessSupport {

    // just the one
    private WitnessSupport() {

    }

    /**
     * {@return a witness object with the provided lookup type}
     * @param lookup lookup
     * @param witnessLookupType the witness lookup type
     */
    public static Object lookupWitness(MethodHandles.Lookup lookup, Type witnessLookupType) {
        if (!(witnessLookupType instanceof ParameterizedType)) {
               throw new IllegalArgumentException("Witness is not a generic type: " + witnessLookupType.getTypeName());
        }
        return lookupWitness(lookup, witnessLookupType, Set.of());
    }

    /**
     * {@return a reflective type modelling the provided signature string}
     * @param lookup lookup
     * @param signatureString the signature string
     */
    public static Type type(MethodHandles.Lookup lookup, String signatureString) {
        class Holder {
            private static final Scope DUMMY_SCOPE = _ -> { throw new UnsupportedOperationException("DUMMY SCOPE!"); };
        }
        SignatureParser parser = SignatureParser.make();
        TypeSignature signature = parser.parseTypeSig(signatureString);
        CoreReflectionFactory coreReflectionFactory = CoreReflectionFactory.make(lookup.lookupClass(), Holder.DUMMY_SCOPE);
        Reifier reifier = Reifier.make(coreReflectionFactory);
        signature.accept(reifier);
        return reifier.getResult();
    }

    static Object lookupWitness(MethodHandles.Lookup lookup, Type witnessLookupType, Set<TypeVariable<?>> allowedTvars) {
        WitnessGraph witnessGraph = new WitnessGraph(witnessLookupType, allowedTvars);
        List<Candidate> candidates = List.of();
        for (Class<?> definingClass : witnessGraph.nodes()) {
            try {
                candidates = findWitnesses(witnessGraph, lookup, candidates, definingClass, witnessLookupType);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }
        if (candidates.size() == 1) {
            try {
                Candidate candidate = candidates.get(0);
                return candidate.handle().invoke();
            } catch (Throwable ex) {
                throw new IllegalStateException(ex);
            }
        } else if (candidates.size() > 1) {
            throw new IllegalArgumentException("Ambiguous witnesses for type: " + witnessLookupType.getTypeName());
        } else {
            // no canidates
            throw new IllegalArgumentException("Witness not found for type: " + witnessLookupType.getTypeName());
        }
    }

    /*
     * 1. find a graph of all generic types (edge is added between A and B if A<B>)
     * 2. for all the nodes in the graph, run the witness lookup
     * 3. if two results are found, say in A and B, and A dominates B in the graph, then prefer A.
     * 4. otherwise, ambiguous`
     *
     * Note: this is a port of the compiler code in Resolve::findWitness
     */

    static class WitnessGraph {

        private final Map<Class<?>, List<Class<?>>> deps = new LinkedHashMap<>();

        WitnessGraph(Type target, Set<TypeVariable<?>> allowedTvars) {
            new Builder(allowedTvars).buildGraph(0, target);
        }

        void addNode(Class<?> node, Stack<Class<?>> dominated) {
            deps.computeIfAbsent(node, _ -> new ArrayList<>());
            for (Class<?> d : dominated) {
                deps.compute(d, (_, targets) -> {
                    targets.add(node);
                    return targets;
                });
            }
        }

        boolean isDominatedBy(Class<?> dominated, Class<?> dominating) {
            return deps.get(dominated).contains(dominating) &&
                    !deps.get(dominating).contains(dominated);
        }

        boolean isDominatedBy(Class<?> dominated, List<Class<?>> dominating) {
            return isDominatedBy(List.of(dominated), dominating);
        }

        boolean isDominatedBy(List<Class<?>> dominated, Class<?> dominating) {
            return isDominatedBy(dominated, List.of(dominating));
        }

        boolean isDominatedBy(List<Class<?>> dominated, List<Class<?>> dominating) {
            for (Class<?> s : dominated) {
                for (Class<?> t : dominating) {
                    if (!isDominatedBy(s, t)) {
                        return false;
                    }
                }
            }
            return true;
        }

        Collection<Class<?>> nodes() {
            return deps.keySet();
        }

        class Builder {

            final Set<TypeVariable<?>> allowedTvars;
            final Stack<Class<?>> stack = new Stack<>();

            Builder(Set<TypeVariable<?>> allowedTvars) {
                this.allowedTvars = allowedTvars;
            }

            void buildGraph(int level, Type target) {
                switch (target) {
                    case Class<?> clazz when clazz.isArray() -> buildGraph(level, clazz.getComponentType());
                    case Class<?> clazz -> addNode(clazz, stack);
                    case WildcardType wt -> {
                        if (level == 1) {
                            throw new IllegalArgumentException("Unsupported toplevel wildcard type in witness: " + target);
                        }
                        if (wt.getLowerBounds().length > 0) {
                            buildGraph(level, wt.getLowerBounds());
                        } else {
                            buildGraph(level, wt.getUpperBounds());
                        }
                    }
                    case TypeVariable<?> tv -> {
                        if (!allowedTvars.contains(tv)) {
                            throw new IllegalArgumentException("Unsupported type variables in witness: " + target);
                        }
                    }
                    case GenericArrayType at -> buildGraph(level, at.getGenericComponentType());
                    case ParameterizedType pt -> {
                        Class<?> base = (Class<?>) pt.getRawType();
                        addNode(base, stack);
                        try {
                            stack.push((Class<?>) pt.getRawType());
                            buildGraph(level + 1, pt.getActualTypeArguments());
                        } finally {
                            stack.pop();
                        }
                    }
                    default -> {
                        // do nothing
                    }
                }
            }

            void buildGraph(int level, Type[] target) {
                Stream.of(target).forEach(t -> buildGraph(level, t));
            }
        }
    }

    private static List<Candidate> findWitnesses(WitnessGraph witnessGraph, MethodHandles.Lookup lookup, List<Candidate> candidates, Class<?> current, Type target) throws ReflectiveOperationException {
        for (Field field : current.getDeclaredFields()) {
            if (field.isAnnotationPresent(Witness.class)) {
                TypeContext tc = new TypeContext();
                if (tc.isSameType(field.getGenericType(), target)) {
                    if (!tc.checkBounds()) {
                        continue;
                    }
                    candidates = merge(witnessGraph, new Candidate(field, lookup.unreflectGetter(field)), candidates);
                }
            }
        }
        outer: for (Method method : current.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Witness.class)) {
                TypeContext tc = new TypeContext();
                if (tc.isSameType(method.getGenericReturnType(), target)) {
                    if (!tc.checkBounds()) {
                        continue;
                    }
                    MethodHandle handle = lookup.unreflect(method);
                    for (Type p : method.getGenericParameterTypes()) {
                        Object o = lookupWitness(lookup, tc.subst(p),
                                Stream.of(method.getTypeParameters())
                                        .collect(Collectors.toSet()));
                        if (o == null) continue outer; // fail
                        handle = handle.bindTo(o);
                    }
                    candidates = merge(witnessGraph, new Candidate(method, handle), candidates);
                }
            }
        }
        return candidates;
    }

    record Candidate(Member member, MethodHandle handle) { }

    static List<Candidate> merge(WitnessGraph graph, Candidate result, List<Candidate> candidates) {
        if (candidates.isEmpty()) {
            return List.of(result);
        }
        Class<?> resultClass = result.member.getDeclaringClass();
        List<Class<?>> candidateClasses = candidates.stream().map(c -> c.member.getDeclaringClass())
                .collect(Collectors.toList());
        if (graph.isDominatedBy(resultClass, candidateClasses)) {
            return candidates;
        } else if (graph.isDominatedBy(candidateClasses, resultClass)) {
            return List.of(result);
        } else {
            List<Candidate> merged = new ArrayList<>(candidates);
            merged.add(result);
            return merged;
        }
    }
}
