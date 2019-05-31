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
import org.openjdk.bench.valhalla.baseline.types.Ref8;
import org.openjdk.bench.valhalla.types.Vector;

public class Set8 extends SizedBase {

    int[] primitive;
    Ref8[] reference;
    Vector[] covariance;

    @Setup
    public void setup() {
        primitive = new int[size * 8];
        reference = new Ref8[size];
        covariance = new Ref8[size];
    }

    @Benchmark
    public Object primitive() {
        int[] values = primitive;
        for (int i = 0, k = 0; i < size * 8; i += 8, k += 8) {
            values[i] = k;
            values[i + 1] = k + 1;
            values[i + 2] = k + 2;
            values[i + 3] = k + 3;
            values[i + 4] = k + 4;
            values[i + 5] = k + 5;
            values[i + 6] = k + 6;
            values[i + 7] = k + 7;
        }
        return values;
    }

    @Benchmark
    public Object reference() {
        Ref8[] values = reference;
        for (int i = 0, k = 0; i < size; i++, k += 8) {
            values[i] = new Ref8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }

    @Benchmark
    public Object covariance() {
        Vector[] values = covariance;
        for (int i = 0, k = 0; i < size; i++, k += 8) {
            values[i] = new Ref8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }

    /*
     *  Hotspot successfully eliminated GC write barriers in case of assignment to a newly created array.
     */
    @Benchmark
    public Object newReference() {
        Ref8[] values = new Ref8[size];
        for (int i = 0, k = 0; i < size; i++, k += 8) {
            values[i] = new Ref8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }

}
