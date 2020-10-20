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
package org.openjdk.bench.valhalla.array.sum;

import org.openjdk.bench.valhalla.array.util.StatesQ64int;
import org.openjdk.bench.valhalla.types.ByInt;
import org.openjdk.bench.valhalla.types.Q64int;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;

public class Inline64int extends StatesQ64int {

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int Val_as_Val_fields(Val_as_Val st) {
        int s = 0;
        Q64int[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            s += arr[i].v0.v0;
            s += arr[i].v1.v0;
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int Val_as_Ref_fields(Val_as_Ref st) {
        int s = 0;
        Q64int.ref[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            s += arr[i].v0.v0;
            s += arr[i].v1.v0;
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int Ref_as_Ref_fields(Ref_as_Ref st) {
        int s = 0;
        Q64int.ref[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            s += arr[i].v0.v0;
            s += arr[i].v1.v0;
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int Val_as_Val_sum(Val_as_Val st) {
        int s = 0;
        Q64int[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            s += arr[i].intSum();
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int Val_as_Ref_sum(Val_as_Ref st) {
        int s = 0;
        Q64int.ref[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            s += arr[i].intSum();
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int Ref_as_Ref_sum(Ref_as_Ref st) {
        int s = 0;
        Q64int.ref[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            s += arr[i].intSum();
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int Val_as_Int_sum(Val_as_By st) {
        int s = 0;
        ByInt[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            s += arr[i].intSum();
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int Ref_as_Int_sum(Ref_as_By st) {
        int s = 0;
        ByInt[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            s += arr[i].intSum();
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int Int_as_Int_sum(By_as_By st) {
        int s = 0;
        ByInt[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            s += arr[i].intSum();
        }
        return s;
    }

}
