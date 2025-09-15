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
import org.openjdk.bench.valhalla.types.Int32;
import org.openjdk.bench.valhalla.types.R32int;
import org.openjdk.bench.valhalla.util.Utils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Setup;

public class Identity32 extends SizeBase {

    public static abstract class IntState extends SizeState {
        public Int32[] arr;
        void fill() {
            int[] a = Utils.makeRandomRing(arr.length);
            for (int i = 0; i < a.length; i++) {
                arr[i] = new R32int(a[i]);
            }
        }
    }

    public static abstract class RefState extends SizeState {
        public R32int[] arr;
        void fill() {
            int[] a = Utils.makeRandomRing(arr.length);
            for (int i = 0; i < a.length; i++) {
                arr[i] = new R32int(a[i]);
            }
        }
    }

    public static class Int_as_Int extends IntState {
        @Setup
        public void setup() {
            arr = new Int32[size];
            fill();
        }
    }

    public static class Ref_as_Ref extends RefState {
        @Setup
        public void setup() {
            arr = new R32int[size];
            fill();
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int Int_as_Int_walk(Int_as_Int s) {
        int steps = 0;
        Int32[] values = s.arr;
        for (int i = values[0].intValue(); i != 0; i = values[i].intValue()) steps++;
        return steps;
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int Ref_as_Ref_walk(Ref_as_Ref s) {
        int steps = 0;
        R32int[] values = s.arr;
        for (int i = values[0].intValue(); i != 0; i = values[i].intValue()) steps++;
        return steps;
    }

}
