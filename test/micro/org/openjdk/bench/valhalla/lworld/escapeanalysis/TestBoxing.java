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

package org.openjdk.bench.valhalla.lworld.escapeanalysis;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(3)
public class TestBoxing {
    static final int ELEM_SIZE = 1_000_000;
    long[] arr;

    @Setup
    public void setup() {
        arr = LongStream.range(0, ELEM_SIZE).toArray();
    }

    @Benchmark
    public int pojo_loop() {
        int sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += PojoWrapper.from(arr[i]).value();
        }
        return sum;
    }

    @Benchmark
    public int inline_loop() {
        int sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += LongWrapper.from(arr[i]).value();
        }
        return sum;
    }

    @Benchmark
    public int box_inline_loop() {
        int sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += BoxInline.from(arr[i]).box().value();
        }
        return sum;
    }

    @Benchmark
    public int box_intf_loop() {
        int sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += BoxInterface.from(arr[i]).box().value();
        }
        return sum;
    }

    @Benchmark
    public int box_intf_loop_sharp() {
        int sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += BoxInterface.from_sharp(arr[i]).box().value();
        }
        return sum;
    }

    @Benchmark
    public int box_ref_loop() {
        int sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += BoxRef.from(arr[i]).box().value();
        }
        return sum;
    }

    @Benchmark
    public int box_ref_loop_sharp() {
        int sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += BoxRef.from_sharp(arr[i]).box().value();
        }
        return sum;
    }

    @Benchmark
    public int box_generic_loop() {
        int sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += BoxGeneric.from(arr[i]).box().value();
        }
        return sum;
    }

    @Benchmark
    public int box_generic_loop_sharp() {
        int sum = 0;
        for (int i = 0; i < ELEM_SIZE; i++) {
            sum += BoxGeneric.from_sharp(arr[i]).box().value();
        }
        return sum;
    }

    interface ValueBox {
        long value();

        final static LongWrapper ZERO = new LongWrapper(0);

        static ValueBox from(long i) {
            return (i == 0L) ? ZERO : new LongWrapper(i);
        }
    }

    static class PojoWrapper {
        final long i;

        PojoWrapper(long i) {
            this.i = i;
        }

        public long value() {
            return i;
        }

        final static PojoWrapper ZERO = new PojoWrapper(0);

        static PojoWrapper from(long i) {
            return (i == 0L) ? ZERO : new PojoWrapper(i);
        }
    }

    static inline class LongWrapper implements ValueBox {
        final long i;

        LongWrapper(long i) {
            this.i = i;
        }

        public long value() {
            return i;
        }

        final static LongWrapper ZERO = new LongWrapper(0);

        static LongWrapper from(long i) {
            return (i == 0L) ? ZERO : new LongWrapper(i);
        }
    }

    static class BoxInterface {
        final ValueBox inlineBox;

        public BoxInterface(ValueBox inlineBox) {
            this.inlineBox = inlineBox;
        }

        ValueBox box() {
            return inlineBox;
        }

        static BoxInterface from_sharp(long i) {
            LongWrapper box = LongWrapper.from(i);
            return new BoxInterface(box);
        }

        static BoxInterface from(long i) {
            ValueBox box = ValueBox.from(i);
            return new BoxInterface(box);
        }
    }

    static class BoxInline {
        final LongWrapper inlineBox;

        public BoxInline(LongWrapper inlineBox) {
            this.inlineBox = inlineBox;
        }

        ValueBox box() {
            return inlineBox;
        }

        static BoxInline from(long i) {
            LongWrapper box = LongWrapper.from(i);
            return new BoxInline(box);
        }
    }

    static class BoxRef {
        final LongWrapper.ref inlineBox;

        public BoxRef(LongWrapper.ref inlineBox) {
            this.inlineBox = inlineBox;
        }

        ValueBox box() {
            return inlineBox;
        }

        static BoxRef from_sharp(long i) {
            LongWrapper box = LongWrapper.from(i);
            return new BoxRef(box);
        }

        static BoxRef from(long i) {
            LongWrapper.ref box = LongWrapper.from(i);
            return new BoxRef(box);
        }
    }

    static class BoxGeneric<T> {
        final T inlineBox;

        public BoxGeneric(T inlineBox) {
            this.inlineBox = inlineBox;
        }

        T box() {
            return inlineBox;
        }

        static BoxGeneric<LongWrapper.ref> from_sharp(long i) {
            LongWrapper box = LongWrapper.from(i);
            return new BoxGeneric<LongWrapper.ref>(box);
        }

        static BoxGeneric<ValueBox> from(long i) {
            ValueBox box = ValueBox.from(i);
            return new BoxGeneric<ValueBox>(box);
        }
    }
}
