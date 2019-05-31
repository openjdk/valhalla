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
import org.openjdk.bench.valhalla.baseline.types.Ref2;

public class Set2 extends SizedBase {

    NodePrim2[] primitive;
    NodeRef2[] reference;

    @Setup
    public void setup() {
        primitive = NodePrim2.set(new NodePrim2[size]);
        reference = NodeRef2.set(new NodeRef2[size]);
    }

    @Benchmark
    public Object primitive() {
        NodePrim2[] values = primitive;
        for (int i = 0, k = 0; i < size; i++, k += 2) {
            values[i].f0 = k;
            values[i].f1 = k + 1;
        }
        return values;
    }

    @Benchmark
    public Object reference() {
        NodeRef2[] values = reference;
        for (int i = 0, k = 0; i < size; i++, k += 2) {
            values[i].f = new Ref2(k, k + 1);
        }
        return values;
    }

}
