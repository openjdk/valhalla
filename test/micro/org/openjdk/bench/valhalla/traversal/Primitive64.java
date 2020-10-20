/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.bench.valhalla.util.Utils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Setup;

public class Primitive64 extends SizeBase {

    public static class LongState extends SizeState {
        public long[] arr;

        @Setup
        public void setup() {
            int[] a = Utils.makeRandomRing(size);
            arr = new long[a.length];
            for (int i = 0; i < a.length; i++) {
                arr[i] = a[i];
            }
        }

    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int walk(LongState s) {
        int steps = 0;
        long[] values = s.arr;
        for (int i = (int) values[0]; i != 0; i = (int) values[i]) steps++;
        return steps;
    }

}

