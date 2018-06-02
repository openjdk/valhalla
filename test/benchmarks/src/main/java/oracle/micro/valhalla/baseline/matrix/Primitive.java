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
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

import java.util.concurrent.ThreadLocalRandom;

public class Primitive extends MatrixBase {

    public static int[][] multBaselineIJK(int[][] A, int[][] B) {
        int size = A.length;
        int[][] R = new int[size][size * 2];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int s_re = 0;
                int s_im = 0;
                for (int k = 0; k < size; k++) {
                    int are = A[i][k * 2 + 0];
                    int aim = A[i][k * 2 + 1];
                    int bre = B[k][j * 2 + 0];
                    int bim = B[k][j * 2 + 1];
                    s_re += are * bre - aim * bim;
                    s_im += are * bim + bre * aim;
                }
                R[i][j * 2 + 0] = s_re;
                R[i][j * 2 + 1] = s_im;
            }
        }
        return R;
    }

    public static int[][] multBaselineIKJ(int[][] A, int[][] B) {
        int size = A.length;
        int[][] R = new int[size][size * 2];
        for (int i = 0; i < size; i++) {
            for (int k = 0; k < size; k++) {
                int are = A[i][k * 2 + 0];
                int aim = A[i][k * 2 + 1];
                for (int j = 0; j < size; j++) {
                    int bre = B[k][j * 2 + 0];
                    int bim = B[k][j * 2 + 1];
                    R[i][j * 2 + 0] += are * bre - aim * bim;
                    R[i][j * 2 + 1] += are * bim + bre * aim;
                }
            }
        }
        return R;
    }

    int[][] A;
    int[][] B;

    @Setup
    public void setup() {
        A = new int[size][size*2];
        populate(A);
        B = new int[size][size*2];
        populate(B);
    }

    private void populate(int[][] m) {
        int size = m.length;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                m[i][j*2+0] = ThreadLocalRandom.current().nextInt();
                m[i][j*2+1] = ThreadLocalRandom.current().nextInt();
            }
        }

    }

    @Benchmark
    public int[][] multIJK() {
        return multBaselineIJK(A, B);
    }

    @Benchmark
    public int[][] multIKJ() {
        return multBaselineIKJ(A, B);
    }

}
