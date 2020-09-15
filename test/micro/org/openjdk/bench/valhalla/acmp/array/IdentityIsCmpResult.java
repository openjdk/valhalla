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
package org.openjdk.bench.valhalla.acmp.array;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/*
 *  For proper results it should be executed:
 *  java -jar target/benchmarks.jar IdentityIsCmpResult  -wmb IdentityIsCmpResult.Obj_equals050
 */

@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
public class IdentityIsCmpResult extends StatesR64long {

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static boolean cmp_Obj(Object[] objects1, Object[] objects2) {
        boolean s = false;
        for (int i = 0; i < SIZE; i++) {
            s ^= objects1[i] == objects2[i];
        }
        return s;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public boolean Obj_equals000(ObjState00 st) {
        return cmp_Obj(st.arr1, st.arr2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public boolean Obj_equals025(ObjState25 st) {
        return cmp_Obj(st.arr1, st.arr2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public boolean Obj_equals050(ObjState50 st) {
        return cmp_Obj(st.arr1, st.arr2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public boolean Obj_equals075(ObjState75 st) {
        return cmp_Obj(st.arr1, st.arr2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public boolean Obj_equals100(ObjState100 st) {
        return cmp_Obj(st.arr1, st.arr2);
    }

}
