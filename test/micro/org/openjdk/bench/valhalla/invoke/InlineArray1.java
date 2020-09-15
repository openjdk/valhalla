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
package org.openjdk.bench.valhalla.invoke;

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
public class InlineArray1 {

    public static final int SIZE = 128;

    public interface MyInterface {
        public int my_method();
    }

    public static inline class Val1 implements MyInterface {
        public final int f0;
        public Val1(int f0) {
            this.f0 = f0;
        }
        @Override
        @CompilerControl(CompilerControl.Mode.DONT_INLINE)
        public int my_method() {
            return f0;
        }
    }

    public static inline class Val2 implements MyInterface {
        public final int f0;
        public Val2(int f0) {
            this.f0 = f0;
        }
        @Override
        @CompilerControl(CompilerControl.Mode.DONT_INLINE)
        public int my_method() {
            return f0;
        }
    }

    public static inline class Val3 implements MyInterface {
        public final int f0;
        public Val3(int f0) {
            this.f0 = f0;
        }
        @Override
        @CompilerControl(CompilerControl.Mode.DONT_INLINE)
        public int my_method() {
            return f0;
        }
    }

    @State(Scope.Thread)
    public static abstract class IntState {
        public MyInterface[] arr;
    }

    @State(Scope.Thread)
    public static abstract class Ref1State {
        public Val1.ref[] arr;
    }

    @State(Scope.Thread)
    public static abstract class Val1State {
        public Val1[] arr;
    }

    public static class Val1_as_Val extends Val1State {
        @Setup
        public void setup() {
            arr = new Val1[SIZE];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new Val1(i);
            }
        }
    }

    public static class Val1_as_Ref extends Ref1State {
        @Setup
        public void setup() {
            arr = new Val1[SIZE];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new Val1(i);
            }
        }
    }

    public static class Val1_as_Int extends IntState {
        @Setup
        public void setup() {
            arr = new Val1[SIZE];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new Val1(i);
            }
        }
    }

    public static class Ref1_as_Ref extends Ref1State {
        @Setup
        public void setup() {
            arr = new Val1.ref[SIZE];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new Val1(i);
            }
        }
    }

    public static class Ref1_as_Int extends IntState {
        @Setup
        public void setup() {
            arr = new Val1.ref[SIZE];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new Val1(i);
            }
        }
    }

    public static class Int1_as_Int extends IntState {
        @Setup
        public void setup() {
            arr = new MyInterface[SIZE];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new Val1(i);
            }
        }
    }

    public static class Val2_as_Int extends IntState {
        @Setup
        public void setup() {
            arr = new Val2[SIZE];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new Val2(i);
            }
        }
    }

    public static class Ref2_as_Int extends IntState {
        @Setup
        public void setup() {
            arr = new Val2.ref[SIZE];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new Val2(i);
            }
        }
    }

    public static class Int2_as_Int extends IntState {
        @Setup
        public void setup() {
            arr = new MyInterface[SIZE];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new Val2(i);
            }
        }
    }

    public static class Val3_as_Int extends IntState {
        @Setup
        public void setup() {
            arr = new Val3[SIZE];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new Val3(i);
            }
        }
    }

    public static class Ref3_as_Int extends IntState {
        @Setup
        public void setup() {
            arr = new Val3.ref[SIZE];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new Val3(i);
            }
        }
    }

    public static class Int3_as_Int extends IntState {
        @Setup
        public void setup() {
            arr = new MyInterface[SIZE];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new Val3(i);
            }
        }
    }


    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int reduceInt(MyInterface[] arr) {
        int r = 0;
        for (int i = 0; i < arr.length; i++) {
            r += arr[i].my_method();
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int reduceRef(Val1.ref[] arr) {
        int r = 0;
        for (int i = 0; i < arr.length; i++) {
            r += arr[i].my_method();
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int reduceVal(Val1[] arr) {
        int r = 0;
        for (int i = 0; i < arr.length; i++) {
            r += arr[i].my_method();
        }
        return r;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target1_Val_v(Val1_as_Val st0, Val1_as_Val st1, Val1_as_Val st2, Val1_as_Val st3, Val1_as_Val st4, Val1_as_Val st5) {
        return reduceVal(st0.arr) +
               reduceVal(st1.arr) +
               reduceVal(st2.arr) +
               reduceVal(st3.arr) +
               reduceVal(st4.arr) +
               reduceVal(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target1_Ref_v(Val1_as_Ref st0, Val1_as_Ref st1, Val1_as_Ref st2, Val1_as_Ref st3, Val1_as_Ref st4, Val1_as_Ref st5) {
        return reduceRef(st0.arr) +
               reduceRef(st1.arr) +
               reduceRef(st2.arr) +
               reduceRef(st3.arr) +
               reduceRef(st4.arr) +
               reduceRef(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target1_Ref_r(Ref1_as_Ref st0, Ref1_as_Ref st1, Ref1_as_Ref st2, Ref1_as_Ref st3, Ref1_as_Ref st4, Ref1_as_Ref st5) {
        return reduceRef(st0.arr) +
               reduceRef(st1.arr) +
               reduceRef(st2.arr) +
               reduceRef(st3.arr) +
               reduceRef(st4.arr) +
               reduceRef(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target1_Ref_vr(Val1_as_Ref st0, Ref1_as_Ref st1, Val1_as_Ref st2, Ref1_as_Ref st3, Val1_as_Ref st4, Ref1_as_Ref st5) {
        return reduceRef(st0.arr) +
               reduceRef(st1.arr) +
               reduceRef(st2.arr) +
               reduceRef(st3.arr) +
               reduceRef(st4.arr) +
               reduceRef(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target1_Int_v(Val1_as_Int st0, Val1_as_Int st1, Val1_as_Int st2, Val1_as_Int st3, Val1_as_Int st4, Val1_as_Int st5) {
        return reduceInt(st0.arr) +
               reduceInt(st1.arr) +
               reduceInt(st2.arr) +
               reduceInt(st3.arr) +
               reduceInt(st4.arr) +
               reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target1_Int_r(Ref1_as_Int st0, Ref1_as_Int st1, Ref1_as_Int st2, Ref1_as_Int st3, Ref1_as_Int st4, Ref1_as_Int st5) {
        return reduceInt(st0.arr) +
               reduceInt(st1.arr) +
               reduceInt(st2.arr) +
               reduceInt(st3.arr) +
               reduceInt(st4.arr) +
               reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target1_Int_i(Int1_as_Int st0, Int1_as_Int st1, Int1_as_Int st2, Int1_as_Int st3, Int1_as_Int st4, Int1_as_Int st5) {
        return reduceInt(st0.arr) +
               reduceInt(st1.arr) +
               reduceInt(st2.arr) +
               reduceInt(st3.arr) +
               reduceInt(st4.arr) +
               reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target1_Int_vr(Val1_as_Int st0, Ref1_as_Int st1, Val1_as_Int st2, Ref1_as_Int st3, Val1_as_Int st4, Ref1_as_Int st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target1_Int_vi(Val1_as_Int st0, Int1_as_Int st1, Val1_as_Int st2, Int1_as_Int st3, Val1_as_Int st4, Int1_as_Int st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target1_Int_ri(Ref1_as_Int st0, Int1_as_Int st1, Ref1_as_Int st2, Int1_as_Int st3, Ref1_as_Int st4, Int1_as_Int st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target1_Int_vri(Val1_as_Int st0, Ref1_as_Int st1, Int1_as_Int st2, Val1_as_Int st3, Ref1_as_Int st4, Int1_as_Int st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target2_Int_v(Val1_as_Int st0, Val2_as_Int st1, Val1_as_Int st2, Val2_as_Int st3, Val1_as_Int st4, Val2_as_Int st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target2_Int_r(Ref1_as_Int st0, Ref2_as_Int st1, Ref1_as_Int st2, Ref2_as_Int st3, Ref1_as_Int st4, Ref2_as_Int st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target2_Int_i(Int1_as_Int st0, Int2_as_Int st1, Int1_as_Int st2, Int2_as_Int st3, Int1_as_Int st4, Int2_as_Int st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target2_Int_vr(Val1_as_Int st0, Ref2_as_Int st1, Val2_as_Int st2, Ref1_as_Int st3, Val1_as_Int st4, Ref2_as_Int st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target2_Int_vi(Val1_as_Int st0, Int2_as_Int st1, Val2_as_Int st2, Int1_as_Int st3, Val1_as_Int st4, Int2_as_Int st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target2_Int_ri(Ref1_as_Int st0, Int1_as_Int st1, Ref2_as_Int st2, Int1_as_Int st3, Ref1_as_Int st4, Int2_as_Int st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target2_Int_vri(Val1_as_Int st0, Ref1_as_Int st1, Int1_as_Int st2, Val2_as_Int st3, Ref2_as_Int st4, Int2_as_Int st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }


    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target3_Int_v(Val1_as_Int st0, Val2_as_Int st1, Val3_as_Int st2, Val1_as_Int st3, Val2_as_Int st4, Val3_as_Int st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target3_Int_r(Ref1_as_Int st0, Ref2_as_Int st1, Ref3_as_Int st2, Ref1_as_Int st3, Ref2_as_Int st4, Ref3_as_Int st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target3_Int_i(Int1_as_Int st0, Int2_as_Int st1, Int3_as_Int st2, Int1_as_Int st3, Int2_as_Int st4, Int3_as_Int st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target3_Int_vr(Val1_as_Int st0, Ref2_as_Int st1, Val3_as_Int st2, Ref1_as_Int st3, Val2_as_Int st4, Ref3_as_Int st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target3_Int_vi(Val1_as_Int st0, Int2_as_Int st1, Val3_as_Int st2, Int1_as_Int st3, Val3_as_Int st4, Int3_as_Int st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target3_Int_ri(Ref1_as_Int st0, Int2_as_Int st1, Ref3_as_Int st2, Int1_as_Int st3, Ref2_as_Int st4, Int3_as_Int st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target3_Int_vri(Val1_as_Int st0, Ref2_as_Int st1, Int3_as_Int st2, Val2_as_Int st3, Ref3_as_Int st4, Int1_as_Int st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }


}