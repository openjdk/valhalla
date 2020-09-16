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
package org.openjdk.bench.valhalla.array.copy;

import org.openjdk.bench.valhalla.array.util.StatesQ64byte;
import org.openjdk.bench.valhalla.types.Q64byte;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;

public class Inline64byte extends StatesQ64byte {

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_copy(Val_as_Val s, Val_as_Val d) {
        Q64byte[] src = s.arr;
        Q64byte[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_arraycopy(Val_as_Val s, Val_as_Val d) {
        System.arraycopy(s.arr, 0, d.arr, 0, s.arr.length);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_copy(Ref_as_Ref s, Val_as_Val d) {
        Q64byte.ref[] src = s.arr;
        Q64byte[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_arraycopy(Ref_as_Ref s, Val_as_Val d) {
        System.arraycopy(s.arr, 0, d.arr, 0, s.arr.length);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_copy(Val_as_Val s, Ref_as_Ref d) {
        Q64byte[] src = s.arr;
        Q64byte.ref[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_arraycopy(Val_as_Val s, Ref_as_Ref d) {
        System.arraycopy(s.arr, 0, d.arr, 0, s.arr.length);
    }

}
