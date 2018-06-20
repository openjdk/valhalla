package oracle.micro.valhalla.lworld.arrayfill;

import oracle.micro.valhalla.ArrayfillBase;
import oracle.micro.valhalla.lworld.types.Value8;
import oracle.micro.valhalla.types.PNumber;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;

public class Arrayfill8 extends ArrayfillBase {

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object value() {
        Value8[] values = new Value8[SIZE];
        for (int i = 0, k = 0; i < values.length; i++, k += 8) {
            values[i] = Value8.of(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object objects() {
        Object[] values = new Object[SIZE];
        for (int i = 0, k = 0; i < values.length; i++, k += 8) {
            values[i] = Value8.of(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object inter() {
        PNumber[] values = new PNumber[SIZE];
        for (int i = 0, k = 0; i < values.length; i++, k += 8) {
            values[i] = Value8.of(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }

}
