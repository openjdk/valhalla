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


public class Copy8 extends SizedBase {

    NodeVal8[] srcValue;
    NodeVal8[] dstValue;
    NodeBox8[] srcBoxed;
    NodeBox8[] dstBoxed;

    @Setup
    public void setup() {
        srcValue = NodeVal8.fill(NodeVal8.set(new NodeVal8[size]));
        dstValue = NodeVal8.set(new NodeVal8[size]);
        srcBoxed = NodeBox8.fill(NodeBox8.set(new NodeBox8[size]));
        dstBoxed = NodeBox8.set(new NodeBox8[size]);
    }

    @Benchmark
    public void value() {
        NodeVal8[] src = srcValue;
        NodeVal8[] dst = dstValue;
        for (int i = 0; i < size; i++) {
            dst[i].f = src[i].f;
        }
    }

    @Benchmark
    public void boxed() {
        NodeBox8[] src = srcBoxed;
        NodeBox8[] dst = dstBoxed;
        for (int i = 0; i < size; i++) {
            dst[i].f = src[i].f;
        }
    }

}
