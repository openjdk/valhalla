/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.valhalla.baseline.acmp;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.bench.valhalla.ACmpBase;

/*
 * to provide proper measurement the benchmark have to be executed in two modes:
 *  -wm INDI
 *  -wm BULK
 */
public class TrivialEquals extends ACmpBase {
    Object o1 = new Object();
    Object o2 = new Object();

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static boolean cmpEquals(Object a, Object b) {
        return a == b; // new acmp
    }

    @Benchmark
    public void isCmp_null_null() {
        cmpEquals(null, null);
    }

    @Benchmark
    public void isCmp_o1_null() {
        cmpEquals(o1, null);
    }

    @Benchmark
    public void isCmp_null_o1() {
        cmpEquals(null, o1);
    }

    @Benchmark
    public void isCmp_o1_o1() {
        cmpEquals(o1, o1);
    }

    @Benchmark
    public void isCmp_o1_o2() {
        cmpEquals(o1, o2);
    }

}
