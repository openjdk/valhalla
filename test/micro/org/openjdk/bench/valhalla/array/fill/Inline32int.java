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
package org.openjdk.bench.valhalla.array.fill;

import org.openjdk.bench.valhalla.array.util.StatesQ32int;
import org.openjdk.bench.valhalla.types.Q32int;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.Arrays;

public class Inline32int extends StatesQ32int {

    public static class RefStaticField {
        static Q32int.ref f = new Q32int(42);
    }

    public static class ValStaticField {
        static Q32int f = new Q32int(42);
    }

    @State(Scope.Thread)
    public static class RefInstanceField {
        Q32int.ref f = new Q32int(42);
    }

    @State(Scope.Thread)
    public static class ValInstanceField {
        Q32int f = new Q32int(42);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Def_to_Val_as_Val_fill(Val_as_Val st) {
        Q32int[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = Q32int.default;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void New_to_Val_as_Val_fill(Val_as_Val st) {
        Q32int[] arr = st.arr;
        Q32int v = new Q32int(42);
        for (int i = 0; i < arr.length; i++) {
            arr[i] = v;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_as_Val_fillstat(Val_as_Val st) {
        Q32int[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = RefStaticField.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Val_fillstat(Val_as_Val st) {
        Q32int[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = ValStaticField.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_as_Val_fillinst(Val_as_Val st, RefInstanceField f) {
        Q32int[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Val_fillinst(Val_as_Val st, ValInstanceField f) {
        Q32int[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Def_to_Ref_as_Ref_fill(Ref_as_Ref st) {
        Q32int.ref[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = Q32int.default;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void New_to_Ref_as_Ref_fill(Ref_as_Ref st) {
        Q32int.ref[] arr = st.arr;
        Q32int v = new Q32int(42);
        for (int i = 0; i < arr.length; i++) {
            arr[i] = v;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Ref_fillstat(Ref_as_Ref st) {
        Q32int.ref[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = RefStaticField.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_as_Ref_fillstat(Ref_as_Ref st) {
        Q32int.ref[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = ValStaticField.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Ref_fillinst(Ref_as_Ref st, RefInstanceField f) {
        Q32int.ref[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_as_Ref_fillinst(Ref_as_Ref st, ValInstanceField f) {
        Q32int.ref[] arr = st.arr;
        for (int i = 0; i < arr.length; i++) {
            arr[i] = f.f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Def_to_Val_as_Val_arrayfill(Val_as_Val st) {
        Arrays.fill(st.arr, Q32int.default);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void New_to_Val_as_Val_arrayfill(Val_as_Val st) {
        Arrays.fill(st.arr, new Q32int(42));
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_as_Val_arrayfillstat(Val_as_Val st) {
        Arrays.fill(st.arr, RefStaticField.f);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Val_arrayfillstat(Val_as_Val st) {
        Arrays.fill(st.arr, ValStaticField.f);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_as_Val_arrayfillinst(Val_as_Val st, RefInstanceField f) {
        Arrays.fill(st.arr, f.f);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_as_Val_arrayfillinst(Val_as_Val st, ValInstanceField f) {
        Arrays.fill(st.arr, f.f);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Def_to_Ref_as_Ref_arrayfill(Ref_as_Ref st) {
        Arrays.fill(st.arr, Q32int.default);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void New_to_Ref_as_Ref_arrayfill(Ref_as_Ref st) {
        Arrays.fill(st.arr, new Q32int(42));
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Ref_arrayfillstat(Ref_as_Ref st) {
        Arrays.fill(st.arr, RefStaticField.f);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_as_Ref_arrayfillstat(Ref_as_Ref st) {
        Arrays.fill(st.arr, ValStaticField.f);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_as_Ref_arrayfillinst(Ref_as_Ref st, RefInstanceField f) {
        Arrays.fill(st.arr, f.f);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_as_Ref_arrayfillinst(Ref_as_Ref st, ValInstanceField f) {
        Arrays.fill(st.arr, f.f);
    }

}
