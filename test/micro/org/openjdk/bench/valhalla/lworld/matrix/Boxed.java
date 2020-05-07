
package org.openjdk.bench.valhalla.lworld.matrix;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.MatrixBase;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;


public class Boxed extends MatrixBase {

    Complex.ref[][] A;
    Complex.ref[][] B;

    @Setup
    public void setup() {
        A = populate(new Complex.ref[size][size]);
        B = populate(new Complex.ref[size][size]);
    }

    private Complex.ref[][] populate(Complex.ref[][] m) {
        int size = m.length;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                m[i][j] = new Complex(ThreadLocalRandom.current().nextDouble(), ThreadLocalRandom.current().nextDouble());
            }
        }
        return m;
    }

    @Benchmark
    public Complex.ref[][] multiply() {
        int size = A.length;
        Complex.ref[][] R = new Complex.ref[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Complex s = Complex.H.ZERO;
                for (int k = 0; k < size; k++) {
                    s = s.add(A[i][k].mul((Complex)B[k][j]));
                }
                R[i][j] = s;
            }
        }
        return R;
    }

    @Benchmark
    public Complex.ref[][] multiplyCacheFriendly() {
        int size = A.length;
        Complex.ref[][] R = new Complex.ref[size][size];
        for (int i = 0; i < size; i++) {
            Arrays.fill(R[i], Complex.H.ZERO);
        }
        for (int i = 0; i < size; i++) {
            for (int k = 0; k < size; k++) {
                Complex.ref aik = A[i][k];
                for (int j = 0; j < size; j++) {
                    R[i][j] = R[i][j].add(aik.mul((Complex)B[k][j]));
                }
            }
        }
        return R;
    }

    @Benchmark
    public Complex.ref[][] multiplyCacheFriendly1() {
        int size = A.length;
        Complex.ref[][] R = new Complex.ref[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                R[i][j] = Complex.H.ZERO;
            }
        }
        for (int i = 0; i < size; i++) {
            for (int k = 0; k < size; k++) {
                Complex.ref aik = A[i][k];
                for (int j = 0; j < size; j++) {
                    R[i][j] = R[i][j].add(aik.mul((Complex)B[k][j]));
                }
            }
        }
        return R;
    }


}

