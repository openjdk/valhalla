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
package org.openjdk.bench.valhalla.arraytotal.fill;

import org.openjdk.bench.valhalla.arraytotal.util.StatesQ64long;
import org.openjdk.bench.valhalla.types.Int64;
import org.openjdk.bench.valhalla.types.Q64long;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.Arrays;

public class Inline64longFillInstObj extends StatesQ64long {


    @State(Scope.Thread)
    public static class InstanceField {
        Object f = new Q64long(42);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Val_as_Val_fillinst0(Val_as_Val st, InstanceField f) {
        Q64long[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (Q64long) f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Val_as_Val_fillinst1(Val_as_Val st, InstanceField f) {
        int len = st.arr.length;
        for (int i = 0; i < len; i++) {
            st.arr[i] = (Q64long) f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Val_as_Ref_fillinst0(Val_as_Ref st, InstanceField f) {
        Q64long.ref[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (Q64long) f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Val_as_Ref_fillinst1(Val_as_Ref st, InstanceField f) {
        int len = st.arr.length;
        for (int i = 0; i < len; i++) {
            st.arr[i] = (Q64long) f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Ref_as_Ref_fillinst0(Ref_as_Ref st, InstanceField f) {
        Q64long.ref[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (Q64long) f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Ref_as_Ref_fillinst1(Ref_as_Ref st, InstanceField f) {
        int len = st.arr.length;
        for (int i = 0; i < len; i++) {
            st.arr[i] = (Q64long) f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Val_as_Int_fillinst0(Val_as_Int st, InstanceField f) {
        Int64[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (Int64) f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Val_as_Int_fillinst1(Val_as_Int st, InstanceField f) {
        int len = st.arr.length;
        for (int i = 0; i < len; i++) {
            st.arr[i] = (Int64) f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Ref_as_Int_fillinst0(Ref_as_Int st, InstanceField f) {
        Int64[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (Int64) f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Ref_as_Int_fillinst1(Ref_as_Int st, InstanceField f) {
        int len = st.arr.length;
        for (int i = 0; i < len; i++) {
            st.arr[i] = (Int64) f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Int_as_Int_fillinst0(Int_as_Int st, InstanceField f) {
        Int64[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (Int64) f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Int_as_Int_fillinst1(Int_as_Int st, InstanceField f) {
        int len = st.arr.length;
        for (int i = 0; i < len; i++) {
            st.arr[i] = (Int64) f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Val_as_Obj_fillinst0(Val_as_Obj st, InstanceField f) {
        Object[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Val_as_Obj_fillinst1(Val_as_Obj st, InstanceField f) {
        int len = st.arr.length;
        for (int i = 0; i < len; i++) {
            st.arr[i] = f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Ref_as_Obj_fillinst0(Ref_as_Obj st, InstanceField f) {
        Object[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Ref_as_Obj_fillinst1(Ref_as_Obj st, InstanceField f) {
        int len = st.arr.length;
        for (int i = 0; i < len; i++) {
            st.arr[i] = f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Int_as_Obj_fillinst0(Int_as_Obj st, InstanceField f) {
        Object[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Int_as_Obj_fillinst1(Int_as_Obj st, InstanceField f) {
        int len = st.arr.length;
        for (int i = 0; i < len; i++) {
            st.arr[i] = f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Obj_as_Obj_fillinst0(Obj_as_Obj st, InstanceField f) {
        Object[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Obj_as_Obj_fillinst1(Obj_as_Obj st, InstanceField f) {
        int len = st.arr.length;
        for (int i = 0; i < len; i++) {
            st.arr[i] = f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Val_as_Val_arrayfillinst(Val_as_Val st, InstanceField f) {
        Arrays.fill(st.arr, f.f);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Val_as_Ref_arrayfillinst(Val_as_Ref st, InstanceField f) {
        Arrays.fill(st.arr, f.f);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Ref_as_Ref_arrayfillinst(Ref_as_Ref st, InstanceField f) {
        Arrays.fill(st.arr, f.f);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Val_as_Int_arrayfillinst(Val_as_Int st, InstanceField f) {
        Arrays.fill(st.arr, f.f);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Ref_as_Int_arrayfillinst(Ref_as_Int st, InstanceField f) {
        Arrays.fill(st.arr, f.f);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Int_as_Int_arrayfillinst(Int_as_Int st, InstanceField f) {
        Arrays.fill(st.arr, f.f);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Val_as_Obj_arrayfillinst(Val_as_Obj st, InstanceField f) {
        Arrays.fill(st.arr, f.f);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Ref_as_Obj_arrayfillinst(Ref_as_Obj st, InstanceField f) {
        Arrays.fill(st.arr, f.f);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Int_as_Obj_arrayfillinst(Int_as_Obj st, InstanceField f) {
        Arrays.fill(st.arr, f.f);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Obj_as_Obj_arrayfillinst(Obj_as_Obj st, InstanceField f) {
        Arrays.fill(st.arr, f.f);
    }

}
