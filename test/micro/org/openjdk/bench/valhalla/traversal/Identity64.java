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
package org.openjdk.bench.valhalla.traversal;

import org.openjdk.bench.valhalla.util.SizeBase;
import org.openjdk.bench.valhalla.types.A64long;
import org.openjdk.bench.valhalla.types.Int64;
import org.openjdk.bench.valhalla.types.R64long;
import org.openjdk.bench.valhalla.util.Utils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Setup;

public class Identity64 extends SizeBase {

    public static abstract class IntState extends SizeState {
        public Int64[] arr;
        void fill() {
            int[] a = Utils.makeRandomRing(arr.length);
            for (int i = 0; i < a.length; i++) {
                arr[i] = new R64long(a[i]);
            }
        }
    }

    public static abstract class AbsState extends SizeState {
        public A64long[] arr;
        void fill() {
            int[] a = Utils.makeRandomRing(arr.length);
            for (int i = 0; i < a.length; i++) {
                arr[i] = new R64long(a[i]);
            }
        }
    }

    public static abstract class RefState extends SizeState {
        public R64long[] arr;
        void fill() {
            int[] a = Utils.makeRandomRing(arr.length);
            for (int i = 0; i < a.length; i++) {
                arr[i] = new R64long(a[i]);
            }
        }
    }


    public static class Int_as_Int extends IntState {
        @Setup
        public void setup() {
            arr = new Int64[size];
            fill();
        }
    }

    public static class Abs_as_Int extends IntState {
        @Setup
        public void setup() {
            arr = new A64long[size];
            fill();
        }
    }

    public static class Ref_as_Int extends IntState {
        @Setup
        public void setup() {
            arr = new R64long[size];
            fill();
        }
    }

    public static class Abs_as_Abs extends AbsState {
        @Setup
        public void setup() {
            arr = new A64long[size];
            fill();
        }
    }

    public static class Ref_as_Abs extends AbsState {
        @Setup
        public void setup() {
            arr = new R64long[size];
            fill();
        }
    }

    public static class Ref_as_Ref extends RefState {
        @Setup
        public void setup() {
            arr = new R64long[size];
            fill();
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int Int_as_Int_walk(Int_as_Int s) {
        int steps = 0;
        Int64[] values = s.arr;
        for (int i = values[0].intValue(); i != 0; i = values[i].intValue()) steps++;
        return steps;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int Abs_as_Int_walk(Abs_as_Int s) {
        int steps = 0;
        Int64[] values = s.arr;
        for (int i = values[0].intValue(); i != 0; i = values[i].intValue()) steps++;
        return steps;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int Ref_as_Int_walk(Ref_as_Int s) {
        int steps = 0;
        Int64[] values = s.arr;
        for (int i = values[0].intValue(); i != 0; i = values[i].intValue()) steps++;
        return steps;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int Abs_as_Abs_walk(Abs_as_Abs s) {
        int steps = 0;
        A64long[] values = s.arr;
        for (int i = values[0].intValue(); i != 0; i = values[i].intValue()) steps++;
        return steps;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int Ref_as_Abs_walk(Ref_as_Abs s) {
        int steps = 0;
        A64long[] values = s.arr;
        for (int i = values[0].intValue(); i != 0; i = values[i].intValue()) steps++;
        return steps;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int Ref_as_Ref_walk(Ref_as_Ref s) {
        int steps = 0;
        A64long[] values = s.arr;
        for (int i = values[0].intValue(); i != 0; i = values[i].intValue()) steps++;
        return steps;
    }

}
