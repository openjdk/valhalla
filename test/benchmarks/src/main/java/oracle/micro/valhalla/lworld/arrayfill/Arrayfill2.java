package oracle.micro.valhalla.lworld.arrayfill;

import oracle.micro.valhalla.ArrayfillBase;
import oracle.micro.valhalla.lworld.types.Value2;
import oracle.micro.valhalla.types.PNumber;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;

public class Arrayfill2 extends ArrayfillBase {

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object value() {
        Value2[] values = new Value2[SIZE];
        for (int i = 0, k = 0; i < values.length; i++, k += 2) {
            values[i] = Value2.of(k, k + 1);
        }
        return values;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object object() {
        Object[] values = new Object[SIZE];
        for (int i = 0, k = 0; i < values.length; i++, k += 2) {
            values[i] = Value2.of(k, k + 1);
        }
        return values;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object inter() {
        PNumber[] values = new PNumber[SIZE];
        for (int i = 0, k = 0; i < values.length; i++, k += 2) {
            values[i] = Value2.of(k, k + 1);
        }
        return values;
    }


}
