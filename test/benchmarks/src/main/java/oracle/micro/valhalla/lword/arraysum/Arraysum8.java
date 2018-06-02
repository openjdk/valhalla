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
package oracle.micro.valhalla.lword.arraysum;

import oracle.micro.valhalla.ArraysumBase;
import oracle.micro.valhalla.BigDataSize;
import oracle.micro.valhalla.SmallDataSize;
import oracle.micro.valhalla.lword.types.Value8;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

public class Arraysum8 extends ArraysumBase {

    public static Value8[] setupValue(int size) {
        Value8[] values = new Value8[size];
        for (int i = 0, k = 0; i < values.length; i++, k += 8) {
            values[i] = Value8.of(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }

    public static int sumScalarized(Value8[] values ) {
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

    public static int sum(Value8[] values) {
        Value8 sum = Value8.of(0, 0, 0, 0, 0, 0, 0, 0);
        for (int i = 0; i < values.length; i++) {
            sum = sum.add(values[i]);
        }
        return sum.totalsum();
    }

    Value8[] values;

    @Setup
    public void setup() {
        values = setupValue(size);
    }

    @Benchmark
    public int valueScalarized() {
        return sumScalarized(values);
    }

    @Benchmark
    public int value() {
        return sum(values);
    }
}
