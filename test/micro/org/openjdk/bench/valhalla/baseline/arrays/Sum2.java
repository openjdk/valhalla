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
package org.openjdk.bench.valhalla.baseline.arrays;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.baseline.types.Ref2;
import org.openjdk.bench.valhalla.baseline.types.Utils;

public class Sum2 extends SizedBase {

    int[] primitive;
    Ref2[] reference;

    @Setup
    public void setup() {
        primitive = Utils.fill(new int[size * 2]);
        reference = Utils.fill(new Ref2[size]);

    }

    @Benchmark
    public int primitive() {
        int[] p = primitive;
        int f0 = 0;
        int f1 = 0;
        for (int i = 0; i < size * 2; i += 2) {
            f0 += p[i];
            f1 += p[i + 1];
        }
        return f0 + f1;
    }

    @Benchmark
    public int reference() {
        Ref2[] r = reference;
        Ref2 sum = new Ref2(0, 0);
        for (int i = 0; i < size; i++) {
            sum = sum.add(r[i]);
        }
        return sum.reduce();
    }

    @Benchmark
    public int refScalarized() {
        Ref2[] r = reference;
        int f0 = 0;
        int f1 = 0;
        for (int i = 0; i < size; i++) {
            f0 += r[i].f0;
            f1 += r[i].f1;
        }
        return f0 + f1;
    }

}
