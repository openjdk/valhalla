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
import org.openjdk.bench.valhalla.baseline.types.Utils;

public class Arraycopy8 extends SizedBase {

    int[] srcPrimitive;
    int[] dstPrimitive;
    Ref8[] srcReference;
    Ref8[] dstReference;

    @Setup
    public void setup() {
        srcPrimitive = Utils.fill(new int[size * 8]);
        dstPrimitive = new int[size * 8];
        srcReference = Utils.fill(new Ref8[size]);
        dstReference = new Ref8[size];
    }

    @Benchmark
    public void primitive() {
        System.arraycopy(srcPrimitive, 0, dstPrimitive, 0, size * 8);
    }

    @Benchmark
    public void reference() {
        System.arraycopy(srcReference, 0, dstReference, 0, size);
    }

}
