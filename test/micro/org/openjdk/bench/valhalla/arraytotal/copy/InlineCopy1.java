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

import org.openjdk.bench.valhalla.arraytotal.util.StatesQ64long;
import org.openjdk.bench.valhalla.types.Int64;
import org.openjdk.bench.valhalla.types.Q64long;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;

public class InlineCopy1 extends StatesQ64long {

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Obj_as_Obj_to_Obj_copy(Obj_as_Obj s, Obj_as_Obj d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Int_as_Obj_to_Obj_copy(Obj_as_Obj s, Int_as_Obj d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Ref_as_Obj_to_Obj_copy(Obj_as_Obj s, Ref_as_Obj d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Val_as_Obj_to_Obj_copy(Obj_as_Obj s, Val_as_Obj d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Int_as_Obj_to_Int_copy(Obj_as_Obj s, Int_as_Int d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Int64)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Ref_as_Obj_to_Int_copy(Obj_as_Obj s, Ref_as_Int d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Int64)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Val_as_Obj_to_Int_copy(Obj_as_Obj s, Val_as_Int d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Int64)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Ref_as_Obj_to_Ref_copy(Obj_as_Obj s, Ref_as_Ref d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Val_as_Obj_to_Ref_copy(Obj_as_Obj s, Val_as_Ref d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Val_as_Obj_to_Val_copy(Obj_as_Obj s, Val_as_Val d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Obj_as_Obj_to_Obj_copy(Int_as_Obj s, Obj_as_Obj d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Int_as_Obj_to_Obj_copy(Int_as_Obj s, Int_as_Obj d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Ref_as_Obj_to_Obj_copy(Int_as_Obj s, Ref_as_Obj d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Val_as_Obj_to_Obj_copy(Int_as_Obj s, Val_as_Obj d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Int_as_Obj_to_Int_copy(Int_as_Obj s, Int_as_Int d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Int64)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Ref_as_Obj_to_Int_copy(Int_as_Obj s, Ref_as_Int d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Int64)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Val_as_Obj_to_Int_copy(Int_as_Obj s, Val_as_Int d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Int64)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Ref_as_Obj_to_Ref_copy(Int_as_Obj s, Ref_as_Ref d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Val_as_Obj_to_Ref_copy(Int_as_Obj s, Val_as_Ref d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Val_as_Obj_to_Val_copy(Int_as_Obj s, Val_as_Val d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Obj_as_Obj_to_Obj_copy(Ref_as_Obj s, Obj_as_Obj d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Int_as_Obj_to_Obj_copy(Ref_as_Obj s, Int_as_Obj d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Obj_to_Obj_copy(Ref_as_Obj s, Ref_as_Obj d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_as_Obj_to_Obj_copy(Ref_as_Obj s, Val_as_Obj d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Int_as_Obj_to_Int_copy(Ref_as_Obj s, Int_as_Int d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Int64)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Obj_to_Int_copy(Ref_as_Obj s, Ref_as_Int d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Int64)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_as_Obj_to_Int_copy(Ref_as_Obj s, Val_as_Int d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Int64)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Obj_to_Ref_copy(Ref_as_Obj s, Ref_as_Ref d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_as_Obj_to_Ref_copy(Ref_as_Obj s, Val_as_Ref d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_as_Obj_to_Val_copy(Ref_as_Obj s, Val_as_Val d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Obj_as_Obj_to_Obj_copy(Val_as_Obj s, Obj_as_Obj d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Int_as_Obj_to_Obj_copy(Val_as_Obj s, Int_as_Obj d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_as_Obj_to_Obj_copy(Val_as_Obj s, Ref_as_Obj d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Obj_to_Obj_copy(Val_as_Obj s, Val_as_Obj d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Int_as_Obj_to_Int_copy(Val_as_Obj s, Int_as_Int d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Int64)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_as_Obj_to_Int_copy(Val_as_Obj s, Ref_as_Int d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Int64)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Obj_to_Int_copy(Val_as_Obj s, Val_as_Int d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Int64)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_as_Obj_to_Ref_copy(Val_as_Obj s, Ref_as_Ref d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Obj_to_Ref_copy(Val_as_Obj s, Val_as_Ref d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Obj_to_Val_copy(Val_as_Obj s, Val_as_Val d) {
        Object[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Obj_as_Int_to_Obj_copy(Int_as_Int s, Obj_as_Obj d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Int_as_Int_to_Obj_copy(Int_as_Int s, Int_as_Obj d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Ref_as_Int_to_Obj_copy(Int_as_Int s, Ref_as_Obj d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Val_as_Int_to_Obj_copy(Int_as_Int s, Val_as_Obj d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Int_as_Int_to_Int_copy(Int_as_Int s, Int_as_Int d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Ref_as_Int_to_Int_copy(Int_as_Int s, Ref_as_Int d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Val_as_Int_to_Int_copy(Int_as_Int s, Val_as_Int d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Ref_as_Int_to_Ref_copy(Int_as_Int s, Ref_as_Ref d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Val_as_Int_to_Ref_copy(Int_as_Int s, Val_as_Ref d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Val_as_Int_to_Val_copy(Int_as_Int s, Val_as_Val d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Obj_as_Int_to_Obj_copy(Ref_as_Int s, Obj_as_Obj d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Int_as_Int_to_Obj_copy(Ref_as_Int s, Int_as_Obj d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Int_to_Obj_copy(Ref_as_Int s, Ref_as_Obj d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_as_Int_to_Obj_copy(Ref_as_Int s, Val_as_Obj d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Int_as_Int_to_Int_copy(Ref_as_Int s, Int_as_Int d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Int_to_Int_copy(Ref_as_Int s, Ref_as_Int d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_as_Int_to_Int_copy(Ref_as_Int s, Val_as_Int d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Int_to_Ref_copy(Ref_as_Int s, Ref_as_Ref d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_as_Int_to_Ref_copy(Ref_as_Int s, Val_as_Ref d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_as_Int_to_Val_copy(Ref_as_Int s, Val_as_Val d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Obj_as_Int_to_Obj_copy(Val_as_Int s, Obj_as_Obj d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Int_as_Int_to_Obj_copy(Val_as_Int s, Int_as_Obj d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_as_Int_to_Obj_copy(Val_as_Int s, Ref_as_Obj d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Int_to_Obj_copy(Val_as_Int s, Val_as_Obj d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Int_as_Int_to_Int_copy(Val_as_Int s, Int_as_Int d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_as_Int_to_Int_copy(Val_as_Int s, Ref_as_Int d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Int_to_Int_copy(Val_as_Int s, Val_as_Int d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_as_Int_to_Ref_copy(Val_as_Int s, Ref_as_Ref d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Int_to_Ref_copy(Val_as_Int s, Val_as_Ref d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Int_to_Val_copy(Val_as_Int s, Val_as_Val d) {
        Int64[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = (Q64long)src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Obj_as_Ref_to_Obj_copy(Ref_as_Ref s, Obj_as_Obj d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Int_as_Ref_to_Obj_copy(Ref_as_Ref s, Int_as_Obj d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Ref_to_Obj_copy(Ref_as_Ref s, Ref_as_Obj d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_as_Ref_to_Obj_copy(Ref_as_Ref s, Val_as_Obj d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Int_as_Ref_to_Int_copy(Ref_as_Ref s, Int_as_Int d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Ref_to_Int_copy(Ref_as_Ref s, Ref_as_Int d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_as_Ref_to_Int_copy(Ref_as_Ref s, Val_as_Int d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Ref_to_Ref_copy(Ref_as_Ref s, Ref_as_Ref d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_as_Ref_to_Ref_copy(Ref_as_Ref s, Val_as_Ref d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_as_Ref_to_Val_copy(Ref_as_Ref s, Val_as_Val d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Obj_as_Ref_to_Obj_copy(Val_as_Ref s, Obj_as_Obj d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Int_as_Ref_to_Obj_copy(Val_as_Ref s, Int_as_Obj d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_as_Ref_to_Obj_copy(Val_as_Ref s, Ref_as_Obj d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Ref_to_Obj_copy(Val_as_Ref s, Val_as_Obj d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Int_as_Ref_to_Int_copy(Val_as_Ref s, Int_as_Int d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_as_Ref_to_Int_copy(Val_as_Ref s, Ref_as_Int d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Ref_to_Int_copy(Val_as_Ref s, Val_as_Int d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_as_Ref_to_Ref_copy(Val_as_Ref s, Ref_as_Ref d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Ref_to_Ref_copy(Val_as_Ref s, Val_as_Ref d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Ref_to_Val_copy(Val_as_Ref s, Val_as_Val d) {
        Q64long.ref[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Obj_as_Val_to_Obj_copy(Val_as_Val s, Obj_as_Obj d) {
        Q64long[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Int_as_Val_to_Obj_copy(Val_as_Val s, Int_as_Obj d) {
        Q64long[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_as_Val_to_Obj_copy(Val_as_Val s, Ref_as_Obj d) {
        Q64long[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Val_to_Obj_copy(Val_as_Val s, Val_as_Obj d) {
        Q64long[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Int_as_Val_to_Int_copy(Val_as_Val s, Int_as_Int d) {
        Q64long[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_as_Val_to_Int_copy(Val_as_Val s, Ref_as_Int d) {
        Q64long[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Val_to_Int_copy(Val_as_Val s, Val_as_Int d) {
        Q64long[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_as_Val_to_Ref_copy(Val_as_Val s, Ref_as_Ref d) {
        Q64long[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Val_to_Ref_copy(Val_as_Val s, Val_as_Ref d) {
        Q64long[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Val_to_Val_copy(Val_as_Val s, Val_as_Val d) {
        Q64long[] src = s.arr;
        for (int i = 0; i < src.length; i++) {
            d.arr[i] = src[i];
        }
    }

}
