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
package oracle.micro.valhalla.lword.matrix;


import oracle.micro.valhalla.MatrixBase;
import oracle.micro.valhalla.lword.types.Value2;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import java.util.concurrent.ThreadLocalRandom;

public class Value extends MatrixBase {

    public static Value2[][] multValueIJK(Value2[][] A, Value2[][] B) {
        int size = A.length;
        Value2[][] R = new Value2[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Value2 s = Value2.of(0, 0);
                for (int k = 0; k < size; k++) {
                    s = s.add(A[i][k].mul(B[k][j]));
                }
                R[i][j] = s;
            }
        }
        return R;
    }

    public static Value2[][] multValueIKJ(Value2[][] A, Value2[][] B) {
        int size = A.length;
        Value2[][] R = new Value2[size][size];
        for (int i = 0; i < size; i++) {
            for (int k = 0; k < size; k++) {
                Value2 aik = A[i][k];
                for (int j = 0; j < size; j++) {
                    R[i][j] = R[i][j].add(aik.mul(B[k][j]));
                }
            }
        }
        return R;
    }

    Value2[][] A;
    Value2[][] B;

    @Setup
    public void setup() {
        A = new Value2[size][size];
        populate(A);
        B = new Value2[size][size];
        populate(B);
    }

    private void populate(Value2[][] m) {
        int size = m.length;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                m[i][j] = Value2.of( ThreadLocalRandom.current().nextInt(), ThreadLocalRandom.current().nextInt());
            }
        }

    }

    @Benchmark
    public Value2[][] multIJK() {
        return multValueIJK(A, B);
    }

    @Benchmark
    public Value2[][] multIKJ() {
        return multValueIKJ(A, B);
    }

}
