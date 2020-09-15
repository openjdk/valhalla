/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package org.openjdk.bench.valhalla.callconv;

import org.openjdk.bench.valhalla.types.Q64byte;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

@Fork(3)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
public class Inline64byte {

    public static final int SIZE = 96;  // must be divisible by 2 and 3 and around 100

    public abstract static class InvocationLogic {
        public abstract Q64byte compute(Q64byte v1);
        public abstract Q64byte compute(Q64byte v1, Q64byte v2);
        public abstract Q64byte compute(Q64byte v1, Q64byte v2, Q64byte v3, Q64byte v4);
        public abstract Q64byte compute(Q64byte v1, Q64byte v2, Q64byte v3, Q64byte v4, Q64byte v5, Q64byte v6, Q64byte v7, Q64byte v8);
    }

    public static class InvokeImpl1 extends InvocationLogic {

        @Override
        public Q64byte compute(Q64byte v1) {
            return v1;
        }

        @Override
        public Q64byte compute(Q64byte v1, Q64byte v2) {
            return v1;
        }

        @Override
        public Q64byte compute(Q64byte v1, Q64byte v2, Q64byte v3, Q64byte v4) {
            return v1;
        }

        @Override
        public Q64byte compute(Q64byte v1, Q64byte v2, Q64byte v3, Q64byte v4, Q64byte v5, Q64byte v6, Q64byte v7, Q64byte v8) {
            return v1;
        }

    }

    public static class InvokeImpl2 extends InvocationLogic {

        @Override
        public Q64byte compute(Q64byte v1) {
            return v1;
        }

        @Override
        public Q64byte compute(Q64byte v1, Q64byte v2) {
            return v1;
        }

        @Override
        public Q64byte compute(Q64byte v1, Q64byte v2, Q64byte v3, Q64byte v4) {
            return v1;
        }

        @Override
        public Q64byte compute(Q64byte v1, Q64byte v2, Q64byte v3, Q64byte v4, Q64byte v5, Q64byte v6, Q64byte v7, Q64byte v8) {
            return v1;
        }

    }

    public static class InvokeImpl3 extends InvocationLogic {

        @Override
        public Q64byte compute(Q64byte v1) {
            return v1;
        }

        @Override
        public Q64byte compute(Q64byte v1, Q64byte v2) {
            return v1;
        }

        @Override
        public Q64byte compute(Q64byte v1, Q64byte v2, Q64byte v3, Q64byte v4) {
            return v1;
        }

        @Override
        public Q64byte compute(Q64byte v1, Q64byte v2, Q64byte v3, Q64byte v4, Q64byte v5, Q64byte v6, Q64byte v7, Q64byte v8) {
            return v1;
        }

    }


    private static InvocationLogic getImpl(int i, int targets) {
        switch (i % targets) {
            case 0:
                return new InvokeImpl1();
            case 1:
                return new InvokeImpl2();
            case 2:
                return new InvokeImpl3();
        }
        return null;
    }

    @State(Scope.Thread)
    public static class StateTargets0 {
        InvokeImpl1[] arr;
        @Setup
        public void setup() {
            arr = new InvokeImpl1[SIZE];
            for(int i=0; i < arr.length; i++) {
                arr[i] = new InvokeImpl1();
            }
        }
    }

    @State(Scope.Thread)
    public static abstract class StateTargets {
        InvocationLogic[] arr;

        public void init(int targets) {
            arr = new InvocationLogic[SIZE];
            for(int i=0; i < arr.length; i++) {
                arr[i] = getImpl(i, targets);
            }
        }
    }

    public static class StateTargets1 extends StateTargets {
        @Setup
        public void setup() {
            init(1);
        }
    }

    public static class StateTargets2 extends StateTargets {
        @Setup
        public void setup() {
            init(2);
        }
    }

    public static class StateTargets3 extends StateTargets {
        @Setup
        public void setup() {
            init(3);
        }
    }

    Q64byte a0 = new Q64byte(42);
    Q64byte a1 = new Q64byte(43);
    Q64byte a2 = new Q64byte(44);
    Q64byte a3 = new Q64byte(45);
    Q64byte a4 = new Q64byte(46);
    Q64byte a5 = new Q64byte(47);
    Q64byte a6 = new Q64byte(48);
    Q64byte a7 = new Q64byte(49);

    @CompilerControl(CompilerControl.Mode.INLINE)
    public long args1(InvocationLogic[] logic) {
        long r = 0;
        for(InvocationLogic t : logic) {
            r += t.compute(a0).longValue();
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public long args2(InvocationLogic[] logic) {
        long r = 0;
        for(InvocationLogic t : logic) {
            r += t.compute(a0, a1).longValue();
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public long args4(InvocationLogic[] logic) {
        long r = 0;
        for(InvocationLogic t : logic) {
            r += t.compute(a0, a1, a2, a3).longValue();
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public long args8(InvocationLogic[] logic) {
        long r = 0;
        for(InvocationLogic t : logic) {
            r += t.compute(a0, a1, a2, a3, a4, a5, a6, a7).longValue();
        }
        return r;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Q64byte_args1_targets0(StateTargets0 st) {
        InvokeImpl1[] arr = st.arr;
        long r = 0;
        for(InvocationLogic t : arr) {
            r += t.compute(a0).longValue();
        }
        return r;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Q64byte_args2_targets0(StateTargets0 st) {
        InvokeImpl1[] arr = st.arr;
        long r = 0;
        for(InvocationLogic t : arr) {
            r += t.compute(a0, a1).longValue();
        }
        return r;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Q64byte_args4_targets0(StateTargets0 st) {
        InvokeImpl1[] arr = st.arr;
        long r = 0;
        for(InvocationLogic t : arr) {
            r += t.compute(a0, a1, a2, a3).longValue();
        }
        return r;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Q64byte_args8_targets0(StateTargets0 st) {
        InvokeImpl1[] arr = st.arr;
        long r = 0;
        for(InvocationLogic t : arr) {
            r += t.compute(a0, a1, a2, a3, a4, a5, a6, a7).longValue();
        }
        return r;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Q64byte_args1_targets1(StateTargets1 st) {
        return args1(st.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Q64byte_args2_targets1(StateTargets1 st) {
        return args2(st.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Q64byte_args4_targets1(StateTargets1 st) {
        return args4(st.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Q64byte_args8_targets1(StateTargets1 st) {
        return args8(st.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Q64byte_args1_targets2(StateTargets2 st) {
        return args1(st.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Q64byte_args2_targets2(StateTargets2 st) {
        return args2(st.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Q64byte_args4_targets2(StateTargets2 st) {
        return args4(st.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Q64byte_args8_targets2(StateTargets2 st) {
        return args8(st.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Q64byte_args1_targets3(StateTargets3 st) {
        return args1(st.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Q64byte_args2_targets3(StateTargets3 st) {
        return args2(st.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Q64byte_args4_targets3(StateTargets3 st) {
        return args4(st.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Q64byte_args8_targets3(StateTargets3 st) {
        return args8(st.arr);
    }


}
