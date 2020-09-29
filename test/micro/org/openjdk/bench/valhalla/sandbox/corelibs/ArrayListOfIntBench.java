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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Measure performance of List of Integer operations.
 * - Set all of int from a set of random numbers (with a seed)
 * - Get all
 * - Shuffle the array
 * - Sort the array
 */


@Fork(value = 1, jvmArgsAppend = "--enable-preview")
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
public class ArrayListOfIntBench {

    @Param({
//            "100",
            "10000",
    })
    public int size;

    @Param({
            "42",
    })
    public int seed;

    public Random rnd;
    public int[] keys;
    public int[] nonKeys;
    ArrayListOfInt arrayListOfInt;
    ArrayList<Integer> arrayListOfInteger;
    ArrayListOfPrimitiveInt arrayListOfPrimitiveInt;

    public void init() {
        Integer[] all;
        if (seed != 0) {
            rnd = new Random(seed);
            all = rnd.ints().distinct().limit(size * 2).boxed().toArray(Integer[]::new);
            Collections.shuffle(Arrays.asList(all), rnd);
        } else {
            rnd = new Random(seed);
            all = IntStream.range(0, size * 2).boxed().toArray(Integer[]::new);
            Collections.shuffle(Arrays.asList(all));
        }
        keys = new int[size];
        for (int i = 0; i < size; i++)
            keys[i] = all[i];

        nonKeys = new int[size];
        for (int i = 0; i < size; i++)
            nonKeys[i] = all[i + size];

        arrayListOfInt = new ArrayListOfInt(size);
        for (int i = 0; i < keys.length; i++) {
            arrayListOfInt.add(i, keys[i]);
        }

        arrayListOfInteger = new ArrayList<>(size);
        for (int i = 0; i < keys.length; i++) {
            arrayListOfInteger.add(i, keys[i]);
        }

        arrayListOfPrimitiveInt = new ArrayListOfPrimitiveInt(size);
        for (int i = 0; i < keys.length; i++) {
            arrayListOfPrimitiveInt.add(i, new PrimitiveInt(keys[i]));
        }
    }

    @Setup
    public void setup() {
        init();
    }

    @TearDown
    public void teardown() {

    }

    @Benchmark
    public void ofIntAdd1() {
        ArrayListOfInt arr = new ArrayListOfInt(size);
        for (int i = 0; i < keys.length; i++) {
            arr.add(i, keys[i]);
        }
    }

    @Benchmark
    public int ofIntSum1(Blackhole bh) {
        ArrayListOfInt arr = new ArrayListOfInt(size);
        int sum = 0;
        for (int i = 0; i < keys.length; i++) {
            sum += arrayListOfInt.get(i);
        }
        return sum;
    }

    @Benchmark
    public void ofIntegerAdd1() {
        ArrayList<Integer> arr = new ArrayList<>(size);
        for (int i = 0; i < keys.length; i++) {
            arr.add(i, keys[i]);
        }
    }

    @Benchmark
    public int ofIntegerSum() {
        int sum = 0;
        for (int i = 0; i < keys.length; i++) {
            sum += arrayListOfInteger.get(i);
        }
        return sum;
    }

    @Benchmark
    public void ofPrimitiveIntAdd1() {
        ArrayListOfPrimitiveInt arr = new ArrayListOfPrimitiveInt(size);
        for (int i = 0; i < keys.length; i++) {
            arr.add(i, arrayListOfPrimitiveInt.get(i));
        }
    }

    @Benchmark
    public int ofPrimitiveIntSum() {
        int sum = 0;
        for (int i = 0; i < keys.length; i++) {
            sum += arrayListOfPrimitiveInt.get(i).value();
        }
        return sum;
    }

}
