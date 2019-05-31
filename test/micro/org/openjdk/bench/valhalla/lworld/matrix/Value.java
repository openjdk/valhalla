
package org.openjdk.bench.valhalla.lworld.matrix;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.MatrixBase;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;


public class Value extends MatrixBase {

    Complex[][] A;
    Complex[][] B;

    @Setup
    public void setup() {
        A = populate(new Complex[size][size]);
        B = populate(new Complex[size][size]);
    }

    private Complex[][] populate(Complex[][] m) {
        int size = m.length;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                m[i][j] = new Complex(ThreadLocalRandom.current().nextDouble(), ThreadLocalRandom.current().nextDouble());
            }
        }
        return m;
    }

    @Benchmark
    public Complex[][] multiply() {
        int size = A.length;
        Complex[][] R = new Complex[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Complex s = Complex.H.ZERO;
                for (int k = 0; k < size; k++) {
                    s = s.add(A[i][k].mul(B[k][j]));
                }
                R[i][j] = s;
            }
        }
        return R;
    }

    @Benchmark
    public Complex[][] multiplyCacheFriendly() {
        int size = A.length;
        Complex[][] R = new Complex[size][size];
        for (int i = 0; i < size; i++) {
            for (int k = 0; k < size; k++) {
                Complex aik = A[i][k];
                for (int j = 0; j < size; j++) {
                    R[i][j] = R[i][j].add(aik.mul(B[k][j]));
                }
            }
        }
        return R;
    }

}

