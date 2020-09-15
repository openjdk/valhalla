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
package org.openjdk.bench.valhalla.arraytotal.copy;

import org.openjdk.bench.valhalla.arraytotal.util.StatesR64long;
import org.openjdk.bench.valhalla.types.A64long;
import org.openjdk.bench.valhalla.types.Int64;
import org.openjdk.bench.valhalla.types.R64long;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;

public class IdentityCopy2 extends StatesR64long {

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Obj_as_Obj_to_Obj_copy(Obj_as_Obj s, Obj_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Int_as_Obj_to_Obj_copy(Obj_as_Obj s, Int_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Abs_as_Obj_to_Obj_copy(Obj_as_Obj s, Abs_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Ref_as_Obj_to_Obj_copy(Obj_as_Obj s, Ref_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Int_as_Obj_to_Int_copy(Obj_as_Obj s, Int_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (Int64)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Abs_as_Obj_to_Int_copy(Obj_as_Obj s, Abs_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (Int64)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Ref_as_Obj_to_Int_copy(Obj_as_Obj s, Ref_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (Int64)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Abs_as_Obj_to_Abs_copy(Obj_as_Obj s, Abs_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (A64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Ref_as_Obj_to_Abs_copy(Obj_as_Obj s, Ref_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (A64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Ref_as_Obj_to_Ref_copy(Obj_as_Obj s, Ref_as_Ref d) {
        int len = s.arr.length;
        R64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (R64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Obj_as_Obj_to_Obj_copy(Int_as_Obj s, Obj_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Int_as_Obj_to_Obj_copy(Int_as_Obj s, Int_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Abs_as_Obj_to_Obj_copy(Int_as_Obj s, Abs_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Ref_as_Obj_to_Obj_copy(Int_as_Obj s, Ref_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Int_as_Obj_to_Int_copy(Int_as_Obj s, Int_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (Int64)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Abs_as_Obj_to_Int_copy(Int_as_Obj s, Abs_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (Int64)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Ref_as_Obj_to_Int_copy(Int_as_Obj s, Ref_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (Int64)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Abs_as_Obj_to_Abs_copy(Int_as_Obj s, Abs_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (A64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Ref_as_Obj_to_Abs_copy(Int_as_Obj s, Ref_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (A64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Ref_as_Obj_to_Ref_copy(Int_as_Obj s, Ref_as_Ref d) {
        int len = s.arr.length;
        R64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (R64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Obj_as_Obj_to_Obj_copy(Abs_as_Obj s, Obj_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Int_as_Obj_to_Obj_copy(Abs_as_Obj s, Int_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Abs_as_Obj_to_Obj_copy(Abs_as_Obj s, Abs_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Ref_as_Obj_to_Obj_copy(Abs_as_Obj s, Ref_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Int_as_Obj_to_Int_copy(Abs_as_Obj s, Int_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (Int64)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Abs_as_Obj_to_Int_copy(Abs_as_Obj s, Abs_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (Int64)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Ref_as_Obj_to_Int_copy(Abs_as_Obj s, Ref_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (Int64)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Abs_as_Obj_to_Abs_copy(Abs_as_Obj s, Abs_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (A64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Ref_as_Obj_to_Abs_copy(Abs_as_Obj s, Ref_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (A64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Ref_as_Obj_to_Ref_copy(Abs_as_Obj s, Ref_as_Ref d) {
        int len = s.arr.length;
        R64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (R64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Obj_as_Obj_to_Obj_copy(Ref_as_Obj s, Obj_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Int_as_Obj_to_Obj_copy(Ref_as_Obj s, Int_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Abs_as_Obj_to_Obj_copy(Ref_as_Obj s, Abs_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Obj_to_Obj_copy(Ref_as_Obj s, Ref_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Int_as_Obj_to_Int_copy(Ref_as_Obj s, Int_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (Int64)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Abs_as_Obj_to_Int_copy(Ref_as_Obj s, Abs_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (Int64)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Obj_to_Int_copy(Ref_as_Obj s, Ref_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (Int64)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Abs_as_Obj_to_Abs_copy(Ref_as_Obj s, Abs_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (A64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Obj_to_Abs_copy(Ref_as_Obj s, Ref_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (A64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Obj_to_Ref_copy(Ref_as_Obj s, Ref_as_Ref d) {
        int len = s.arr.length;
        R64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (R64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Obj_as_Int_to_Obj_copy(Int_as_Int s, Obj_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Int_as_Int_to_Obj_copy(Int_as_Int s, Int_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Abs_as_Int_to_Obj_copy(Int_as_Int s, Abs_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Ref_as_Int_to_Obj_copy(Int_as_Int s, Ref_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Int_as_Int_to_Int_copy(Int_as_Int s, Int_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Abs_as_Int_to_Int_copy(Int_as_Int s, Abs_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Ref_as_Int_to_Int_copy(Int_as_Int s, Ref_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Abs_as_Int_to_Abs_copy(Int_as_Int s, Abs_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (A64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Ref_as_Int_to_Abs_copy(Int_as_Int s, Ref_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (A64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Ref_as_Int_to_Ref_copy(Int_as_Int s, Ref_as_Ref d) {
        int len = s.arr.length;
        R64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (R64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Obj_as_Int_to_Obj_copy(Abs_as_Int s, Obj_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Int_as_Int_to_Obj_copy(Abs_as_Int s, Int_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Abs_as_Int_to_Obj_copy(Abs_as_Int s, Abs_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Ref_as_Int_to_Obj_copy(Abs_as_Int s, Ref_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Int_as_Int_to_Int_copy(Abs_as_Int s, Int_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Abs_as_Int_to_Int_copy(Abs_as_Int s, Abs_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Ref_as_Int_to_Int_copy(Abs_as_Int s, Ref_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Abs_as_Int_to_Abs_copy(Abs_as_Int s, Abs_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (A64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Ref_as_Int_to_Abs_copy(Abs_as_Int s, Ref_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (A64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Ref_as_Int_to_Ref_copy(Abs_as_Int s, Ref_as_Ref d) {
        int len = s.arr.length;
        R64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (R64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Obj_as_Int_to_Obj_copy(Ref_as_Int s, Obj_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Int_as_Int_to_Obj_copy(Ref_as_Int s, Int_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Abs_as_Int_to_Obj_copy(Ref_as_Int s, Abs_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Int_to_Obj_copy(Ref_as_Int s, Ref_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Int_as_Int_to_Int_copy(Ref_as_Int s, Int_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Abs_as_Int_to_Int_copy(Ref_as_Int s, Abs_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Int_to_Int_copy(Ref_as_Int s, Ref_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Abs_as_Int_to_Abs_copy(Ref_as_Int s, Abs_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (A64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Int_to_Abs_copy(Ref_as_Int s, Ref_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (A64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Int_to_Ref_copy(Ref_as_Int s, Ref_as_Ref d) {
        int len = s.arr.length;
        R64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (R64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Obj_as_Abs_to_Obj_copy(Abs_as_Abs s, Obj_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Int_as_Abs_to_Obj_copy(Abs_as_Abs s, Int_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Abs_as_Abs_to_Obj_copy(Abs_as_Abs s, Abs_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Ref_as_Abs_to_Obj_copy(Abs_as_Abs s, Ref_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Int_as_Abs_to_Int_copy(Abs_as_Abs s, Int_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Abs_as_Abs_to_Int_copy(Abs_as_Abs s, Abs_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Ref_as_Abs_to_Int_copy(Abs_as_Abs s, Ref_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Abs_as_Abs_to_Abs_copy(Abs_as_Abs s, Abs_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Ref_as_Abs_to_Abs_copy(Abs_as_Abs s, Ref_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Abs_to_Ref_as_Abs_to_Ref_copy(Abs_as_Abs s, Ref_as_Ref d) {
        int len = s.arr.length;
        R64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (R64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Obj_as_Abs_to_Obj_copy(Ref_as_Abs s, Obj_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Int_as_Abs_to_Obj_copy(Ref_as_Abs s, Int_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Abs_as_Abs_to_Obj_copy(Ref_as_Abs s, Abs_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Abs_to_Obj_copy(Ref_as_Abs s, Ref_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Int_as_Abs_to_Int_copy(Ref_as_Abs s, Int_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Abs_as_Abs_to_Int_copy(Ref_as_Abs s, Abs_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Abs_to_Int_copy(Ref_as_Abs s, Ref_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Abs_as_Abs_to_Abs_copy(Ref_as_Abs s, Abs_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Abs_to_Abs_copy(Ref_as_Abs s, Ref_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Abs_to_Ref_copy(Ref_as_Abs s, Ref_as_Ref d) {
        int len = s.arr.length;
        R64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = (R64long)s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Obj_as_Ref_to_Obj_copy(Ref_as_Ref s, Obj_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Int_as_Ref_to_Obj_copy(Ref_as_Ref s, Int_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Abs_as_Ref_to_Obj_copy(Ref_as_Ref s, Abs_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Ref_to_Obj_copy(Ref_as_Ref s, Ref_as_Obj d) {
        int len = s.arr.length;
        Object[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Int_as_Ref_to_Int_copy(Ref_as_Ref s, Int_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Abs_as_Ref_to_Int_copy(Ref_as_Ref s, Abs_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Ref_to_Int_copy(Ref_as_Ref s, Ref_as_Int d) {
        int len = s.arr.length;
        Int64[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Abs_as_Ref_to_Abs_copy(Ref_as_Ref s, Abs_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Ref_to_Abs_copy(Ref_as_Ref s, Ref_as_Abs d) {
        int len = s.arr.length;
        A64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Ref_to_Ref_copy(Ref_as_Ref s, Ref_as_Ref d) {
        int len = s.arr.length;
        R64long[] dst = d.arr;
        for (int i = 0; i < len; i++) {
            dst[i] = s.arr[i];
        }
    }

}
