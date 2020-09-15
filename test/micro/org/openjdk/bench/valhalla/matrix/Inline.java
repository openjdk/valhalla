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
import org.openjdk.bench.valhalla.types.QComplex;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Setup;

import java.util.concurrent.ThreadLocalRandom;

public abstract class Inline extends Base {

    public static final Complex IZERO = new QComplex(0,0);
    public static final QComplex VZERO = new QComplex(0,0);
    public static final QComplex.ref RZERO = new QComplex(0,0);

    private static void populate(Complex[][] m) {
        int size = m.length;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                m[i][j] = new QComplex(ThreadLocalRandom.current().nextDouble(), ThreadLocalRandom.current().nextDouble());
            }
        }
    }

//    public static class Ref extends Inline {
//
//    }

    public static class Ref extends Inline {
        QComplex.ref[][] A;
        QComplex.ref[][] B;

        @Setup
        public void setup() {
            A = new QComplex.ref[size][size];
            populate(A);
            B = new QComplex.ref[size][size];
            populate(B);
        }

        @Benchmark
        @CompilerControl(CompilerControl.Mode.DONT_INLINE)
        public QComplex.ref[][] multiply() {
            int size = A.length;
            QComplex.ref[][] R = new QComplex.ref[size][size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    QComplex.ref s = RZERO;
                    for (int k = 0; k < size; k++) {
                        s = s.add(A[i][k].mul(B[k][j]));
                    }
                    R[i][j] = s;
                }
            }
            return R;
        }

//        @Benchmark
//        public QComplex[][] multiplyCacheFriendly() {
//            int size = A.length;
//            QComplex[][] R = new QComplex[size][size];
//            for (int i = 0; i < size; i++) {
//                for (int k = 0; k < size; k++) {
//                    QComplex aik = A[i][k];
//                    for (int j = 0; j < size; j++) {
//                        R[i][j] = R[i][j].add(aik.mul(B[k][j]));
//                    }
//                }
//            }
//            return R;
//        }

    }

    public static class Val extends Inline {
        QComplex[][] A;
        QComplex[][] B;

        @Setup
        public void setup() {
            A = new QComplex[size][size];
            populate(A);
            B = new QComplex[size][size];
            populate(B);
        }

        @Benchmark
        @CompilerControl(CompilerControl.Mode.DONT_INLINE)
        public QComplex[][] multiply() {
            int size = A.length;
            QComplex[][] R = new QComplex[size][size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    QComplex s = VZERO;
                    for (int k = 0; k < size; k++) {
                        s = s.add(A[i][k].mul(B[k][j]));
                    }
                    R[i][j] = s;
                }
            }
            return R;
        }

//        @Benchmark
//        public QComplex[][] multiplyCacheFriendly() {
//            int size = A.length;
//            QComplex[][] R = new QComplex[size][size];
//            for (int i = 0; i < size; i++) {
//                for (int k = 0; k < size; k++) {
//                    QComplex aik = A[i][k];
//                    for (int j = 0; j < size; j++) {
//                        R[i][j] = R[i][j].add(aik.mul(B[k][j]));
//                    }
//                }
//            }
//            return R;
//        }

    }

    public static class Int extends Inline {
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

    public static class ICov extends Inline {
        Complex[][] A;
        Complex[][] B;

        @Setup
        public void setup() {
            A = new QComplex[size][size];
            populate(A);
            B = new QComplex[size][size];
            populate(B);
        }

        @Benchmark
        @CompilerControl(CompilerControl.Mode.DONT_INLINE)
        public Complex[][] multiply() {
            int size = A.length;
            Complex[][] R = new QComplex[size][size];
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

//        @Benchmark
//        public Complex[][] multiplyCacheFriendly() {
//            int size = A.length;
//            Complex[][] R = new QComplex[size][size];
//            for (int i = 0; i < size; i++) {
//                for (int k = 0; k < size; k++) {
//                    Complex aik = A[i][k];
//                    for (int j = 0; j < size; j++) {
//                        R[i][j] = R[i][j].add(aik.mul(B[k][j]));
//                    }
//                }
//            }
//            return R;
//        }

    }

    public static class RCov extends Inline {
        QComplex.ref[][] A;
        QComplex.ref[][] B;

        @Setup
        public void setup() {
            A = new QComplex[size][size];
            populate(A);
            B = new QComplex[size][size];
            populate(B);
        }

        @Benchmark
        @CompilerControl(CompilerControl.Mode.DONT_INLINE)
        public QComplex.ref[][] multiply() {
            int size = A.length;
            QComplex.ref[][] R = new QComplex[size][size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    QComplex.ref s = RZERO;
                    for (int k = 0; k < size; k++) {
                        s = s.add(A[i][k].mul(B[k][j]));
                    }
                    R[i][j] = s;
                }
            }
            return R;
        }

//        @Benchmark
//        public Complex[][] multiplyCacheFriendly() {
//            int size = A.length;
//            Complex[][] R = new QComplex[size][size];
//            for (int i = 0; i < size; i++) {
//                for (int k = 0; k < size; k++) {
//                    Complex aik = A[i][k];
//                    for (int j = 0; j < size; j++) {
//                        R[i][j] = R[i][j].add(aik.mul(B[k][j]));
//                    }
//                }
//            }
//            return R;
//        }

    }

}
