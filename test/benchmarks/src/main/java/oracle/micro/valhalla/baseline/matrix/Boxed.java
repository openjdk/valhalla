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
package oracle.micro.valhalla.baseline.matrix;


import oracle.micro.valhalla.MatrixBase;
import oracle.micro.valhalla.baseline.types.Box2;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class Boxed extends MatrixBase {

    public static final Box2 ZERO = new Box2(0, 0);

    public static Box2[][] multBoxedIJK(Box2[][] A, Box2[][] B) {
        int size = A.length;
        Box2[][] R = new Box2[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Box2 s = ZERO;
                for (int k = 0; k < size; k++) {
                    s = s.add(A[i][k].mul(B[k][j]));
                }
                R[i][j] = s;
            }
        }
        return R;
    }

    public static Box2[][] multBoxedIKJ(Box2[][] A, Box2[][] B) {
        int size = A.length;
        Box2[][] R = new Box2[size][size];
        for (int i = 0; i < size; i++) {
            Arrays.fill(R[i], ZERO);
        }
        for (int i = 0; i < size; i++) {
            for (int k = 0; k < size; k++) {
                Box2 aik = A[i][k];
                for (int j = 0; j < size; j++) {
                    R[i][j] = R[i][j].add(aik.mul(B[k][j]));
                }
            }
        }
        return R;
    }

    Box2[][] A;
    Box2[][] B;

    @Setup
    public void setup() {
        A = new Box2[size][size];
        populate(A);
        B = new Box2[size][size];
        populate(B);
    }

    private void populate(Box2[][] m) {
        int size = m.length;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                m[i][j] = new Box2(ThreadLocalRandom.current().nextInt(), ThreadLocalRandom.current().nextInt());
            }
        }

    }

    @Benchmark
    public Box2[][] multIJK() {
        return multBoxedIJK(A, B);
    }

    @Benchmark
    public Box2[][] multIKJ() {
        return multBoxedIKJ(A, B);
    }

}
