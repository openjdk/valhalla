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


public class Copy8 extends SizedBase {

    NodePrim8[] srcPrimitive;
    NodePrim8[] dstPrimitive;
    NodeRef8[] srcReference;
    NodeRef8[] dstReference;

    @Setup
    public void setup() {
        srcPrimitive = NodePrim8.fill(NodePrim8.set(new NodePrim8[size]));
        dstPrimitive = NodePrim8.set(new NodePrim8[size]);
        srcReference = NodeRef8.fill(NodeRef8.set(new NodeRef8[size]));
        dstReference = NodeRef8.set(new NodeRef8[size]);
    }

    @Benchmark
    public void primitive() {
        NodePrim8[] src = srcPrimitive;
        NodePrim8[] dst = dstPrimitive;
        for (int i = 0; i < size; i++) {
            dst[i].f0 = src[i].f0;
            dst[i].f1 = src[i].f1;
            dst[i].f2 = src[i].f2;
            dst[i].f3 = src[i].f3;
            dst[i].f4 = src[i].f4;
            dst[i].f5 = src[i].f5;
            dst[i].f6 = src[i].f6;
            dst[i].f7 = src[i].f7;
        }
    }

    @Benchmark
    public void reference() {
        NodeRef8[] src = srcReference;
        NodeRef8[] dst = dstReference;
        for (int i = 0; i < size; i++) {
            dst[i].f = src[i].f;
        }
    }

}
