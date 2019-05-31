/*
 * Copyright (c) 8016, 8018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 8 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 8 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 8 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 08110-1301 USA.
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

public class Set8 extends SizedBase {

    NodePrim8[] primitive;
    NodeRef8[] reference;

    @Setup
    public void setup() {
        primitive = NodePrim8.set(new NodePrim8[size]);
        reference = NodeRef8.set(new NodeRef8[size]);
    }

    @Benchmark
    public Object primitive() {
        NodePrim8[] values = primitive;
        for (int i = 0, k = 0; i < size; i++, k += 8) {
            values[i].f0 = k;
            values[i].f1 = k + 1;
            values[i].f2 = k + 2;
            values[i].f3 = k + 3;
            values[i].f4 = k + 4;
            values[i].f5 = k + 5;
            values[i].f6 = k + 6;
            values[i].f7 = k + 7;
        }
        return values;
    }

    @Benchmark
    public Object reference() {
        NodeRef8[] values = reference;
        for (int i = 0, k = 0; i < size; i++, k += 8) {
            values[i].f = new Ref8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }

}
