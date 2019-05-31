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
package org.openjdk.bench.valhalla.lworld.fields;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;


public class Copy1 extends SizedBase {

    NodeVal1[] srcValue;
    NodeVal1[] dstValue;
    NodeBox1[] srcBoxed;
    NodeBox1[] dstBoxed;

    @Setup
    public void setup() {
        srcValue = NodeVal1.fill(NodeVal1.set(new NodeVal1[size]));
        dstValue = NodeVal1.set(new NodeVal1[size]);
        srcBoxed = NodeBox1.fill(NodeBox1.set(new NodeBox1[size]));
        dstBoxed = NodeBox1.set(new NodeBox1[size]);
    }

    @Benchmark
    public void value() {
        NodeVal1[] src = srcValue;
        NodeVal1[] dst = dstValue;
        for (int i = 0; i < size; i++) {
            dst[i].f = src[i].f;
        }
    }

    @Benchmark
    public void boxed() {
        NodeBox1[] src = srcBoxed;
        NodeBox1[] dst = dstBoxed;
        for (int i = 0; i < size; i++) {
            dst[i].f = src[i].f;
        }
    }

}
