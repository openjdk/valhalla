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
public class InlineField {

    public static final int SIZE = 128;

    public interface MyInterface {
        public int my_method();
    }

    public static primitive class Val1 implements MyInterface {
        public final int f0;
        public Val1(int f0) {
            this.f0 = f0;
        }
        @Override
        public int my_method() {
            return f0;
        }
    }

    public static primitive class Val2 implements MyInterface {
        public final int f0;
        public Val2(int f0) {
            this.f0 = f0;
        }
        @Override
        public int my_method() {
            return f0;
        }
    }

    public static primitive class Val3 implements MyInterface {
        public final int f0;
        public Val3(int f0) {
            this.f0 = f0;
        }
        @Override
        public int my_method() {
            return f0;
        }
    }

    public static class Val1Wrapper {
        public Val1 f;

        public Val1Wrapper(Val1 f) {
            this.f = f;
        }
    }

    public static class Ref1Wrapper {
        public Val1.ref f;

        public Ref1Wrapper(Val1.ref f) {
            this.f = f;
        }
    }

    public static class IntWrapper {
        public MyInterface f;

        public IntWrapper(MyInterface f) {
            this.f = f;
        }
    }

    @State(Scope.Thread)
    public static class Val1State {
        public Val1Wrapper[] arr;
        @Setup
        public void setup() {
            arr = new Val1Wrapper[SIZE];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new Val1Wrapper(new Val1(i));
            }
        }
    }

    @State(Scope.Thread)
    public static class Ref1State {
        public Ref1Wrapper[] arr;
        @Setup
        public void setup() {
            arr = new Ref1Wrapper[SIZE];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new Ref1Wrapper(new Val1(i));
            }
        }
    }

    @State(Scope.Thread)
    public static class IntStateBase {
        public IntWrapper[] arr;
    }

    public static class Int1State extends IntStateBase {
        @Setup
        public void setup() {
            arr = new IntWrapper[SIZE];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new IntWrapper(new Val1(i));
            }
        }
    }

    public static class Int2State extends IntStateBase {
        @Setup
        public void setup() {
            arr = new IntWrapper[SIZE];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new IntWrapper(new Val2(i));
            }
        }
    }

    public static class Int3State extends IntStateBase {
        @Setup
        public void setup() {
            arr = new IntWrapper[SIZE];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new IntWrapper(new Val3(i));
            }
        }
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int reduceVal(Val1Wrapper[] arr) {
        int r = 0;
        for (int i = 0; i < arr.length; i++) {
            r += arr[i].f.my_method();
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int reduceRef(Ref1Wrapper[] arr) {
        int r = 0;
        for (int i = 0; i < arr.length; i++) {
            r += arr[i].f.my_method();
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int reduceInt(IntWrapper[] arr) {
        int r = 0;
        for (int i = 0; i < arr.length; i++) {
            r += arr[i].f.my_method();
        }
        return r;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int target1_Val(Val1State st0, Val1State st1, Val1State st2, Val1State st3, Val1State st4, Val1State st5) {
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
    public int target1_Ref(Ref1State st0, Ref1State st1, Ref1State st2, Ref1State st3, Ref1State st4, Ref1State st5) {
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
    public int target1_Int(Int1State st0, Int1State st1, Int1State st2, Int1State st3, Int1State st4, Int1State st5) {
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
    public int target2_Int(Int1State st0, Int2State st1, Int1State st2, Int2State st3, Int1State st4, Int2State st5) {
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
    public int target3_Int(Int1State st0, Int2State st1, Int3State st2, Int1State st3, Int2State st4, Int3State st5) {
        return reduceInt(st0.arr) +
                reduceInt(st1.arr) +
                reduceInt(st2.arr) +
                reduceInt(st3.arr) +
                reduceInt(st4.arr) +
                reduceInt(st5.arr);
    }



}
