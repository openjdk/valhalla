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
package oracle.micro.valhalla.baseline.arraysum;

import oracle.micro.valhalla.ArraysumBase;
import oracle.micro.valhalla.BigDataSize;
import oracle.micro.valhalla.SmallDataSize;
import oracle.micro.valhalla.baseline.types.Box8;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

public class Arraysum8 extends ArraysumBase {

    public static int[] setupPrimitive(int size) {
        int[] values = new int[size * 8];
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


    public static int sumPrimitive(int[] values ) {
        int sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += values[i];
        }
        return sum;
    }

    public static Box8[] setupBoxed(int size) {
        Box8[] values = new Box8[size];
        for (int i = 0, k = 0; i < values.length; i++, k += 8) {
            values[i] = new Box8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }

    public static int sumScalarized(Box8[] values ) {
        int f0 = 0;
        int f1 = 0;
        int f2 = 0;
        int f3 = 0;
        int f4 = 0;
        int f5 = 0;
        int f6 = 0;
        int f7 = 0;
        for (int i = 0; i < values.length; i++) {
            f0 += values[i].f0;
            f1 += values[i].f1;
            f2 += values[i].f2;
            f3 += values[i].f3;
            f4 += values[i].f4;
            f5 += values[i].f5;
            f6 += values[i].f6;
            f7 += values[i].f7;
        }
        return f0 + f1 + f2 + f3 + f4 + f5 + f6 + f7;
    }

    public static int sum(Box8[] values) {
        Box8 sum = new Box8(0, 0, 0, 0, 0, 0, 0, 0);
        for (int i = 0; i < values.length; i++) {
            sum = sum.add(values[i]);
        }
        return sum.totalsum();
    }

    @State(Scope.Thread)
    public static class StatePrimitive {
        int[] values;

        @Setup
        public void setup() {
            values = setupPrimitive(size);
        }
    }

    @State(Scope.Thread)
    public static class StateBoxed {
        Box8[] values;

        @Setup
        public void setup() {
            values = setupBoxed(size);
        }
    }

    @Benchmark
    public int boxedScalarized(StateBoxed st) {
        return sumScalarized(st.values);
    }

    @Benchmark
    public int boxed(StateBoxed st) {
        return sum(st.values);
    }

    @Benchmark
    public int primitive(StatePrimitive st) {
        return sumPrimitive(st.values);
    }
}
