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
import org.openjdk.bench.valhalla.baseline.types.Ref1;
import org.openjdk.bench.valhalla.baseline.types.Utils;

public class Sum1 extends SizedBase {

    int[] primitive;
    Ref1[] reference;

    @Setup
    public void setup() {
        primitive = Utils.fill(new int[size]);
        reference = Utils.fill(new Ref1[size]);

    }

    @Benchmark
    public int primitive() {
        int[] p = primitive;
        int sum = 0;
        for (int i = 0; i < size; i++) {
            sum += p[i];
        }
        return sum;
    }

    @Benchmark
    public int reference() {
        Ref1[] r = reference;
        Ref1 sum = new Ref1(0);
        for (int i = 0; i < size; i++) {
            sum = sum.add(r[i]);
        }
        return sum.reduce();
    }

    @Benchmark
    public int refScalarized() {
        Ref1[] r = reference;
        int sum = 0;
        for (int i = 0; i < size; i++) {
            sum += r[i].f0;
        }
        return sum;
    }

}
