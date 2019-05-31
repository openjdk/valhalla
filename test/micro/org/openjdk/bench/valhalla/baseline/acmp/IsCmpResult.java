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
package org.openjdk.bench.valhalla.baseline.acmp;

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

import java.util.concurrent.TimeUnit;

/*
 *  For proper results it should be executed:
 *  java -jar target/benchmarks.jar baseline.acmp.IsCmpResult  -wmb baseline.acmp.IsCmpResult.equals050
 */
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
public class IsCmpResult extends ACmpBase {

    Object[] array1_00, array2_00;
    Object[] array1_25, array2_25;
    Object[] array1_50, array2_50;
    Object[] array1_75, array2_75;
    Object[] array1_100, array2_100;


    @Setup
    public void setup() {
        array1_00 = populate1();
        array2_00 = populate2(array1_00, 0);
        array1_25 = populate1();
        array2_25 = populate2(array1_25, 25);
        array1_50 = populate1();
        array2_50 = populate2(array1_50, 50);
        array1_75 = populate1();
        array2_75 = populate2(array1_75, 75);
        array1_100 = populate1();
        array2_100 = populate2(array1_100, 100);
    }


    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static boolean comparison(Object[] objects1, Object[] objects2) {
        boolean s = false;
        for (int i = 0; i < SIZE; i++) {
            s ^= objects1[i] == objects2[i];
        }
        return s;
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public boolean equals000() {
        return comparison(array1_00, array2_00);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public boolean equals025() {
        return comparison(array1_25, array2_25);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public boolean equals050() {
        return comparison(array1_50, array2_50);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public boolean equals075() {
        return comparison(array1_75, array2_75);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public boolean equals100() {
        return comparison(array1_100, array2_100);
    }

}
