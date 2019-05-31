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
package org.openjdk.bench.valhalla.baseline.fields;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.baseline.types.Ref8;

public class Sum8 extends SizedBase {

    NodePrim8[] primitive;
    NodeRef8[] reference;

    @Setup
    public void setup() {
        primitive = NodePrim8.fill(NodePrim8.set(new NodePrim8[size]));
        reference = NodeRef8.fill(NodeRef8.set(new NodeRef8[size]));

    }

    @Benchmark
    public int primitive() {
        NodePrim8[] p = primitive;
        int f0 = 0;
        int f1 = 0;
        int f2 = 0;
        int f3 = 0;
        int f4 = 0;
        int f5 = 0;
        int f6 = 0;
        int f7 = 0;
        for (int i = 0; i < size; i ++) {
            f0 += p[i].f0;
            f1 += p[i].f1;
            f2 += p[i].f2;
            f3 += p[i].f3;
            f4 += p[i].f4;
            f5 += p[i].f5;
            f6 += p[i].f6;
            f7 += p[i].f7;
        }
        return f0 + f1 + f2 + f3 + f4 + f5 + f6 + f7;
    }

    @Benchmark
    public int reference() {
        NodeRef8[] r = reference;
        Ref8 sum = new Ref8(0, 0, 0, 0, 0, 0, 0, 0);
        for (int i = 0; i < size; i++) {
            sum = sum.add(r[i].f);
        }
        return sum.reduce();
    }

    @Benchmark
    public int refScalarized() {
        NodeRef8[] r = reference;
        int f0 = 0;
        int f1 = 0;
        int f2 = 0;
        int f3 = 0;
        int f4 = 0;
        int f5 = 0;
        int f6 = 0;
        int f7 = 0;
        for (int i = 0; i < size; i++) {
            f0 += r[i].f.f0;
            f1 += r[i].f.f1;
            f2 += r[i].f.f2;
            f3 += r[i].f.f3;
            f4 += r[i].f.f4;
            f5 += r[i].f.f5;
            f6 += r[i].f.f6;
            f7 += r[i].f.f7;
        }
        return f0 + f1 + f2 + f3 + f4 + f5 + f6 + f7;
    }

}
