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

package org.openjdk.bench.valhalla.sandbox.corelibs;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;

import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Measure performance of List of Integer operations.
 * - Set all of int from a set of random numbers (with a seed)
 * - Get all
 * - Shuffle the array
 * - Sort the array
 */


@Fork(3)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 3)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
public class ArrayListOfIntBench {

    @Param({
            "100",
            "1_000_000",
    })
    public int size;

    ArrayListOfInt arrayListOfInt;
    ArrayList<Integer> arrayListOfInteger;
    ArrayListOfPrimitiveInt arrayListOfPrimitiveInt;

    @Setup
    public void setup() {
        arrayListOfInt = new ArrayListOfInt(size);
        for (int i = 0; i < size; i++) {
            arrayListOfInt.add(i, i);
        }

        arrayListOfInteger = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            arrayListOfInteger.add(i, i);
        }

        arrayListOfPrimitiveInt = new ArrayListOfPrimitiveInt(size);
        for (int i = 0; i < size; i++) {
            arrayListOfPrimitiveInt.add(i, new PrimitiveInt(i));
        }
    }

    @Benchmark
    public Object ofIntAdd() {
        ArrayListOfInt list = new ArrayListOfInt(size);
        for (int i = 0; i < size; i++) {
            list.add(i, i);
        }
        return list;
    }

    @Benchmark
    public Object ofIntegerAdd() {
        ArrayList<Integer> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(i, i);
        }
        return list;
    }

    @Benchmark
    public Object ofPrimitiveIntAdd() {
        ArrayListOfPrimitiveInt list = new ArrayListOfPrimitiveInt(size);
        for (int i = 0; i < size; i++) {
            list.add(i, new PrimitiveInt(i));
        }
        return list;
    }

    @Benchmark
    public Object ofIntAdd0() {
        ArrayListOfInt list = new ArrayListOfInt(size);
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
        return list;
    }

    @Benchmark
    public Object ofIntegerAdd0() {
        ArrayList<Integer> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
        return list;
    }

    @Benchmark
    public Object ofPrimitiveIntAdd0() {
        ArrayListOfPrimitiveInt list = new ArrayListOfPrimitiveInt(size);
        for (int i = 0; i < size; i++) {
            list.add(new PrimitiveInt(i));
        }
        return list;
    }

    @Benchmark
    public int ofIntSum() {
        int sum = 0;
        for (int i = 0; i < size; i++) {
            sum += arrayListOfInt.get(i);
        }
        return sum;
    }

    @Benchmark
    public int ofIntegerSum() {
        int sum = 0;
        for (int i = 0; i < size; i++) {
            sum += arrayListOfInteger.get(i);
        }
        return sum;
    }


    @Benchmark
    public int ofPrimitiveIntSum() {
        int sum = 0;
        for (int i = 0; i < size; i++) {
            sum += arrayListOfPrimitiveInt.get(i).value();
        }
        return sum;
    }

}
