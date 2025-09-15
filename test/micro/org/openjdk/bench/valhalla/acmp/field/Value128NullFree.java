/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.valhalla.acmp.field;

import jdk.internal.vm.annotation.NullRestricted;
import jdk.internal.vm.annotation.Strict;
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

import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/*
 *  For proper results it should be executed:
 *  java -jar target/benchmarks.jar org.openjdk.bench.valhalla.acmp.field.Value  -wmb "org.openjdk.bench.valhalla.acmp.field.Value.*050"
 */

@Fork(value = 3, jvmArgsAppend = {"--enable-preview"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
public class Value128NullFree {

    public static final int SIZE = 100;

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static int cmp_branch_val(ValWrapper[] objects1, ValWrapper[] objects2) {
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            if (objects1[i].f == objects2[i].f) {
                s += 1;
            } else {
                s -= 1;
            }
        }
        return s;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static boolean cmp_result_val(ValWrapper[] objects1, ValWrapper[] objects2) {
        boolean s = false;
        for (int i = 0; i < SIZE; i++) {
            s ^= objects1[i].f == objects2[i].f;
        }
        return s;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int branch_val_equals000(ValState00 st) {
        return cmp_branch_val(st.arr1, st.arr2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int branch_val_equals025(ValState25 st) {
        return cmp_branch_val(st.arr1, st.arr2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int branch_val_equals050(ValState50 st) {
        return cmp_branch_val(st.arr1, st.arr2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int branch_val_equals075(ValState75 st) {
        return cmp_branch_val(st.arr1, st.arr2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int branch_val_equals100(ValState100 st) {
        return cmp_branch_val(st.arr1, st.arr2);
    }


    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public boolean result_val_equals000(ValState00 st) {
        return cmp_result_val(st.arr1, st.arr2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public boolean result_val_equals025(ValState25 st) {
        return cmp_result_val(st.arr1, st.arr2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public boolean result_val_equals050(ValState50 st) {
        return cmp_result_val(st.arr1, st.arr2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public boolean result_val_equals075(ValState75 st) {
        return cmp_result_val(st.arr1, st.arr2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public boolean result_val_equals100(ValState100 st) {
        return cmp_result_val(st.arr1, st.arr2);
    }

    public static value class ValueInt4 {

        public final int v0;
        public final int v1;
        public final int v2;
        public final int v3;

        public ValueInt4(int v) {
            this.v0 = v;
            this.v1 = v + 1;
            this.v2 = v + 2;
            this.v3 = v + 3;
        }

    }

    private static void populate(ValWrapper[] arr1, ValWrapper[] arr2, int eq) {
        if (eq <= 0) {
            for (int i = 0; i < SIZE; i++) {
                arr1[i] = new ValWrapper(new ValueInt4(2 * i));
                arr2[i] = new ValWrapper(new ValueInt4(2 * i + 1));
            }
        } else if (eq >= 100) {
            for (int i = 0; i < SIZE; i++) {
                ValueInt4 x = new ValueInt4(i);
                arr2[i] = new ValWrapper(x);
                arr1[i] = new ValWrapper(x);
            }
        } else {
            BitSet eqset = new Random(42).ints(0, SIZE).distinct().limit(eq * SIZE / 100).collect(BitSet::new, BitSet::set, BitSet::or);
            for (int i = 0; i < SIZE; i++) {
                if (eqset.get(i)) {
                    ValueInt4 x = new ValueInt4(i);
                    arr2[i] = new ValWrapper(x);
                    arr1[i] = new ValWrapper(x);
                } else {
                    arr1[i] = new ValWrapper(new ValueInt4(2 * i));
                    arr2[i] = new ValWrapper(new ValueInt4(2 * i + 1));
                }
            }
        }
    }

    public static class ValWrapper {

        @Strict
        @NullRestricted
        public final ValueInt4 f;

        public ValWrapper(ValueInt4 f) {
            this.f = f;
        }
    }


    @State(Scope.Thread)
    public abstract static class ValState {
        ValWrapper[] arr1, arr2;

        public void setup(int eq) {
            arr1 = new ValWrapper[SIZE];
            arr2 = new ValWrapper[SIZE];
            populate(arr1, arr2, eq);
        }
    }

    public static class ValState00 extends ValState {
        @Setup
        public void setup() {
            setup(0);
        }
    }

    public static class ValState25 extends ValState {
        @Setup
        public void setup() {
            setup(25);
        }
    }

    public static class ValState50 extends ValState {
        @Setup
        public void setup() {
            setup(50);
        }
    }

    public static class ValState75 extends ValState {
        @Setup
        public void setup() {
            setup(75);
        }
    }

    public static class ValState100 extends ValState {
        @Setup
        public void setup() {
            setup(100);
        }
    }

}
