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
package oracle.micro.valhalla.baseline.arrayfill;

import oracle.micro.valhalla.ArrayfillBase;
import oracle.micro.valhalla.baseline.types.Box8;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;

public class Arrayfill8 extends ArrayfillBase {

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object primitive() {
        int[] values = new int[SIZE * 8];
        for (int i = 0, k = 0; i < values.length; i += 8, k += 8) {
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
    @OperationsPerInvocation(SIZE)
    public Object boxed() {
        Box8[] values = new Box8[SIZE];
        for (int i = 0, k = 0; i < values.length; i++, k += 8) {
            values[i] = new Box8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }


}
