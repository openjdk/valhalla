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
package org.openjdk.bench.valhalla.arraytotal.sum;

import org.openjdk.bench.valhalla.arraytotal.util.StatesQ64long;
import org.openjdk.bench.valhalla.types.ByLong;
import org.openjdk.bench.valhalla.types.Q64long;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;

public class Inline64long extends StatesQ64long {

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Val_as_Val_fields0(Val_as_Val st) {
        long s = 0;
        Q64long[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            s += arr[i].v0;
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Val_as_Val_fields1(Val_as_Val st) {
        long s = 0;
        int len = st.arr.length;
        for(int i=0; i < len; i++) {
            s += st.arr[i].v0;
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Val_as_Ref_fields0(Val_as_Ref st) {
        long s = 0;
        Q64long.ref[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            s += arr[i].v0;
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Val_as_Ref_fields1(Val_as_Ref st) {
        long s = 0;
        int len = st.arr.length;
        for(int i=0; i < len; i++) {
            s += st.arr[i].v0;
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Ref_as_Ref_fields0(Ref_as_Ref st) {
        long s = 0;
        Q64long.ref[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            s += arr[i].v0;
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Ref_as_Ref_fields1(Ref_as_Ref st) {
        long s = 0;
        int len = st.arr.length;
        for(int i=0; i < len; i++) {
            s += st.arr[i].v0;
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Val_as_Val_sum0(Val_as_Val st) {
        long s = 0;
        Q64long[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            s += arr[i].longSum();
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Val_as_Val_sum1(Val_as_Val st) {
        long s = 0;
        int len = st.arr.length;
        for(int i=0; i < len; i++) {
            s += st.arr[i].longSum();
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Val_as_Ref_sum0(Val_as_Ref st) {
        long s = 0;
        Q64long.ref[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            s += arr[i].longSum();
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Val_as_Ref_sum1(Val_as_Ref st) {
        long s = 0;
        int len = st.arr.length;
        for(int i=0; i < len; i++) {
            s += st.arr[i].longSum();
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Val_as_Int_sum0(Val_as_By st) {
        long s = 0;
        ByLong[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            s += arr[i].longSum();
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Val_as_Int_sum1(Val_as_By st) {
        long s = 0;
        int len = st.arr.length;
        for(int i=0; i < len; i++) {
            s += st.arr[i].longSum();
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Ref_as_Ref_sum0(Ref_as_Ref st) {
        long s = 0;
        Q64long.ref[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            s += arr[i].longSum();
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Ref_as_Ref_sum1(Ref_as_Ref st) {
        long s = 0;
        int len = st.arr.length;
        for(int i=0; i < len; i++) {
            s += st.arr[i].longSum();
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Ref_as_Int_sum0(Ref_as_By st) {
        long s = 0;
        ByLong[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            s += arr[i].longSum();
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Ref_as_Int_sum1(Ref_as_By st) {
        long s = 0;
        int len = st.arr.length;
        for(int i=0; i < len; i++) {
            s += st.arr[i].longSum();
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Int_as_Int_sum0(By_as_By st) {
        long s = 0;
        ByLong[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            s += arr[i].longSum();
        }
        return s;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long Int_as_Int_sum1(By_as_By st) {
        long s = 0;
        int len = st.arr.length;
        for(int i=0; i < len; i++) {
            s += st.arr[i].longSum();
        }
        return s;
    }

}
