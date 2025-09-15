/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.valhalla.matrix;

import org.openjdk.bench.valhalla.types.Complex;
import org.openjdk.bench.valhalla.types.RComplex;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Setup;

import java.util.concurrent.ThreadLocalRandom;

public abstract class Identity extends Base {

    public static final Complex IZERO = new RComplex(0,0);
    public static final RComplex RZERO = new RComplex(0,0);

    private static void populate(Complex[][] m) {
        int size = m.length;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                m[i][j] = new RComplex(ThreadLocalRandom.current().nextDouble(), ThreadLocalRandom.current().nextDouble());
            }
        }
    }

    public static class Ref extends Identity {
        RComplex[][] A;
        RComplex[][] B;

        @Setup
        public void setup() {
            A = new RComplex[size][size];
            populate(A);
            B = new RComplex[size][size];
            populate(B);
        }

        @Benchmark
        @CompilerControl(CompilerControl.Mode.DONT_INLINE)
        public RComplex[][] multiply() {
            int size = A.length;
            RComplex[][] R = new RComplex[size][size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    RComplex s = RZERO;
                    for (int k = 0; k < size; k++) {
                        s = s.add(A[i][k].mul(B[k][j]));
                    }
                    R[i][j] = s;
                }
            }
            return R;
        }
    }

    public static class Int extends Identity {
        Complex[][] A;
        Complex[][] B;

        @Setup
        public void setup() {
            A = new Complex[size][size];
            populate(A);
            B = new Complex[size][size];
            populate(B);
        }

        @Benchmark
        @CompilerControl(CompilerControl.Mode.DONT_INLINE)
        public Complex[][] multiply() {
            int size = A.length;
            Complex[][] R = new Complex[size][size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    Complex s = IZERO;
                    for (int k = 0; k < size; k++) {
                        s = s.add(A[i][k].mul(B[k][j]));
                    }
                    R[i][j] = s;
                }
            }
            return R;
        }
    }

}
