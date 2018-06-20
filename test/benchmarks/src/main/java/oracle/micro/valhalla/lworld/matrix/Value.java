package oracle.micro.valhalla.lworld.matrix;


import oracle.micro.valhalla.MatrixBase;
import oracle.micro.valhalla.lworld.types.Value2;
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
