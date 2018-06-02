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
import oracle.micro.valhalla.baseline.types.Box2;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

public class Arraysum2 extends ArraysumBase {

    public static int[] setupPrimitive(int size) {
        int[] values = new int[size*2];
        for (int i = 0, k = 0; i < values.length; i += 2, k += 2) {
            values[i] = k;
            values[i + 1] = k + 1;
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

    public static Box2[] setupBoxed(int size) {
        Box2[] values = new Box2[size];
        for (int i = 0, k = 0; i < values.length; i++, k += 2) {
            values[i] = new Box2(k, k + 1);
        }

        return values;
    }

    public static int sumScalarized(Box2[] values ) {
        int f0 = 0;
        int f1 = 0;
        for (int i = 0; i < values.length; i++) {
            f0 += values[i].f0;
            f1 += values[i].f1;
        }
        return f0 + f1;
    }

    public static int sum(Box2[] values) {
        Box2 sum = new Box2(0, 0);
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
        Box2[] values;

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
