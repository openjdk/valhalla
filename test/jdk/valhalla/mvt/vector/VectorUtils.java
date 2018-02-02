/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import jdk.incubator.mvt.ValueType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.IntStream;

import static java.lang.invoke.MethodType.methodType;

public class VectorUtils {
    private static final Class<?> THIS_KLASS = VectorUtils.class;
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    /**
     * liftedOp(VT v1,v2,...,va) =
     *   let opFi(v,w) = op(v1.fi,v2.fi,...,va.fi),
     *       factory(f1,...,fn) = zeroVT().withF1(f1)...withFn(fn) in
     *   factory(opF1(v1,v2,...,va),...,opFn(v1,v2,...,va))
     */
    static MethodHandle lift(ValueType<?> vt, MethodHandle op, MethodHandle factory) {
        Class<?> elementType = op.type().returnType();
        int arity = op.type().parameterCount();
        int count = 0;
        MethodHandle liftedOp = factory;
        for (Field f : vt.valueFields()) {
            MethodHandle getter = Utils.compute(() -> vt.findGetter(LOOKUP, f.getName(), elementType));
            MethodHandle[] getters = new MethodHandle[arity];
            Arrays.fill(getters, getter);
            MethodHandle fieldOp = MethodHandles.filterArguments(op, 0, getters);
            liftedOp = MethodHandles.collectArguments(liftedOp, arity*count, fieldOp);
            count++;
        }
        Class<?> dvt = vt.valueClass();
        MethodType liftedOpType = naryType(arity, dvt); // (dvt,...,dvt)dvt
        int[] reorder = IntStream.range(0, arity*count).map(i -> i % arity).toArray(); // [0, 1, ..., arity, ..., 0, 1, ..., arity]
        liftedOp = MethodHandles.permuteArguments(liftedOp, liftedOpType, reorder);
        return liftedOp;
    }

    /**
     * T reducer(VT v) = op(...op(op(zero, v.f1), v.f2), ..., v.fn)
     */
    static MethodHandle reducer(ValueType<?> vt, MethodHandle op, MethodHandle zero) {
        Class<?> elementType = op.type().returnType();
        MethodHandle reducer = zero;
        Field[] valueFields = vt.valueFields();
        for (Field f : valueFields) {
            MethodHandle getter = Utils.compute(() -> vt.findGetter(LOOKUP, f.getName(), elementType));
            MethodHandle reduceOp = MethodHandles.filterArguments(op, 1, getter);
            reducer = MethodHandles.collectArguments(reduceOp, 0, reducer); // (x, ...) -> factory(...).with(x));
        }
        int[] reorder = new int[valueFields.length]; // [0, ..., 0]
        reducer = MethodHandles.permuteArguments(reducer, methodType(long.class, vt.valueClass()), reorder);
        return reducer;
    }

    /**
     * T reducerLoop(DVT[] a) {
     *   DVT v = zero();
     *   for (int i = 0; i < a.length; i++) {
     *     v = op(v, a[i]);
     *   }
     *   return reducer(v);
     * }
     */
    public static MethodHandle reducerLoop(ValueType<?> vt, MethodHandle op, MethodHandle reducer, MethodHandle zero) {
        MethodHandle iterations = MethodHandles.arrayLength(vt.arrayValueClass());        // (DVT[]) int
        MethodHandle init = MethodHandles.dropArguments(zero, 0, vt.arrayValueClass());   // (DVT[]) DVT

        // DVT body(DVT v, int i, DVT[] arr) { return op(v, arr[i])); }
        MethodHandle getElement = MethodHandles.arrayElementGetter(vt.arrayValueClass()); // (DVT[], int) DVT
        MethodHandle body = MethodHandles.permuteArguments( // (0:DVT, 2:int, 1:DVT[]) DVT
                MethodHandles.collectArguments(op, 1, getElement), // (DVT, DVT[], int) DVT
                methodType(vt.valueClass(), vt.valueClass(), int.class, vt.arrayValueClass()),
                0, 2, 1);

        // A = [ DVT[] ], V = [ DVT ]
        //
        // int iterations(A...);
        // V init(A...);
        // V body(V, int, A...);
        //
        // V countedLoop(A...) {
        //   int end = iterations(A...);
        //   V v = init(A...);
        //   for (int i = 0; i < end; ++i) {
        //     v = body(v, i, A...);
        //   }
        //   return v;
        // }
        MethodHandle loop = MethodHandles.countedLoop(iterations, init, body); // (DVT[])DVT

        //MH(DVT[])DVT => MH(DVT[])T
        MethodHandle result = MethodHandles.filterReturnValue(loop, reducer);
        return result;
    }

    static MethodType naryType(int arity, Class<?> c) {
        Class<?>[] parameterTypes = new Class<?>[arity];
        Arrays.fill(parameterTypes, c);
        return methodType(c, parameterTypes);
    }

    static MethodHandle valueFactory(Class<?> vcc, MethodHandles.Lookup lookup) {
        ValueType<?> vt = ValueType.forClass(vcc);
        return Utils.compute(() -> vt.unreflectWithers(lookup, true, vt.valueFields()));
    }
}

