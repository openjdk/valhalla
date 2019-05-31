/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.valhalla.lworld.acmp;

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
import org.openjdk.bench.valhalla.ACmpBase;
import org.openjdk.bench.valhalla.lworld.types.Val1;
import org.openjdk.bench.valhalla.types.Vector;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/*
 *  For proper results it should be executed:
 *  java -jar target/benchmarks.jar baseline.acmp.IsCmpBranch  -wmb baseline.acmp.IsCmpBranch.equals050
 */
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
public class IsCmpBranch1 extends ACmpBase {

    public static final int SIZE = 1024;

    Val1[] value1_00, value2_00;
    Val1[] value1_25, value2_25;
    Val1[] value1_50, value2_50;
    Val1[] value1_75, value2_75;
    Val1[] value1_100, value2_100;

    Val1?[] boxed1_00, boxed2_00;
    Val1?[] boxed1_25, boxed2_25;
    Val1?[] boxed1_50, boxed2_50;
    Val1?[] boxed1_75, boxed2_75;
    Val1?[] boxed1_100, boxed2_100;

    Vector[] covariance1_00, covariance2_00;
    Vector[] covariance1_25, covariance2_25;
    Vector[] covariance1_50, covariance2_50;
    Vector[] covariance1_75, covariance2_75;
    Vector[] covariance1_100, covariance2_100;


    @Setup
    public void setup() {
        value1_00 = populateValues1();
        value2_00 = populateValues2(value1_00, 0);
        value1_25 = populateValues1();
        value2_25 = populateValues2(value1_25, 25);
        value1_50 = populateValues1();
        value2_50 = populateValues2(value1_50, 50);
        value1_75 = populateValues1();
        value2_75 = populateValues2(value1_75, 75);
        value1_100 = populateValues1();
        value2_100 = populateValues2(value1_100, 100);
        boxed1_00 = new Val1?[SIZE];
        boxed2_00 = new Val1?[SIZE];
        boxed1_25 = new Val1?[SIZE];
        boxed2_25 = new Val1?[SIZE];
        boxed1_50 = new Val1?[SIZE];
        boxed2_50 = new Val1?[SIZE];
        boxed1_75 = new Val1?[SIZE];
        boxed2_75 = new Val1?[SIZE];
        boxed1_100 = new Val1?[SIZE];
        boxed2_100 = new Val1?[SIZE];
        for(int i = 0; i< SIZE; i++) {
            boxed1_00[i] = value1_00[i];
            boxed2_00[i] = value2_00[i];
            boxed1_25[i] = value1_25[i];
            boxed2_25[i] = value2_25[i];
            boxed1_50[i] = value1_50[i];
            boxed2_50[i] = value2_50[i];
            boxed1_75[i] = value1_75[i];
            boxed2_75[i] = value2_75[i];
            boxed1_100[i] = value1_100[i];
            boxed2_100[i] = value2_100[i];
        }
        covariance1_00 = value1_00;
        covariance2_00 = value2_00;
        covariance1_25 = value1_25;
        covariance2_25 = value2_25;
        covariance1_50 = value1_50;
        covariance2_50 = value2_50;
        covariance1_75 = value1_75;
        covariance2_75 = value2_75;
        covariance1_100 = value1_100;
        covariance2_100 = value2_100;
    }

    public static Val1[] populateValues1() {
        Val1[] values = new Val1[SIZE];
        for(int i=0; i< SIZE; i++) {
            values[i] = new Val1(i);
        }
        return values;
    }

    public static Val1[] populateValues2(Val1[] base, int eq) {
        Val1[] values2 = new Val1[base.length];
        for(int i=0; i< SIZE; i++) {
            values2[i] = new Val1(SIZE + i);
        }
        if (eq == 0) {
            // nothing to do
        } else if (eq >= 100) {
            System.arraycopy(base, 0, values2, 0, base.length);
        } else {
            int[] eq_indices = new Random(42)
                    .ints(0, base.length)
                    .distinct()
                    .limit((eq * base.length)/ 100)
                    .sorted()
                    .toArray();
            for(int i : eq_indices) {
                values2[i] = base[i];
            }
        }
        return values2;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static int value_comparison(Val1[] objects1, Val1[] objects2) {
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            if (objects1[i] == objects2[i]) {
                s += 1;
            } else {
                s -= 1;
            }
        }
        return s;
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int value000() {
        return value_comparison(value1_00, value2_00);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int value025() {
        return value_comparison(value1_25, value2_25);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int value050() {
        return value_comparison(value1_50, value2_50);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int value075() {
        return value_comparison(value1_75, value2_75);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int value100() {
        return value_comparison(value1_100, value2_100);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static int boxed_comparison(Val1?[] objects1, Val1?[] objects2) {
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            if (objects1[i] == objects2[i]) {
                s += 1;
            } else {
                s -= 1;
            }
        }
        return s;
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int boxed000() {
        return boxed_comparison(boxed1_00, boxed2_00);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int boxed025() {
        return boxed_comparison(boxed1_25, boxed2_25);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int boxed050() {
        return boxed_comparison(boxed1_50, boxed2_50);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int boxed075() {
        return boxed_comparison(boxed1_75, boxed2_75);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int boxed100() {
        return boxed_comparison(boxed1_100, boxed2_100);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static int covariance_comparison(Vector[] objects1, Vector[] objects2) {
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            if (objects1[i] == objects2[i]) {
                s += 1;
            } else {
                s -= 1;
            }
        }
        return s;
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int covariance000() {
        return covariance_comparison(covariance1_00, covariance2_00);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int covariance025() {
        return covariance_comparison(covariance1_25, covariance2_25);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int covariance050() {
        return covariance_comparison(covariance1_50, covariance2_50);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int covariance075() {
        return covariance_comparison(covariance1_75, covariance2_75);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int covariance100() {
        return covariance_comparison(covariance1_100, covariance2_100);
    }

}
