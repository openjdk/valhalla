package oracle.micro.valhalla.lworld.arrayfill;

import oracle.micro.valhalla.ArrayfillBase;
import oracle.micro.valhalla.lworld.types.Value1;
import oracle.micro.valhalla.types.PNumber;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;

public class Arrayfill1 extends ArrayfillBase {

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object value() {
        Value1[] values = new Value1[SIZE];
        for (int i = 0; i < values.length; i++) {
            values[i] = Value1.of(i);
        }
        return values;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object object() {
        Object[] values = new Object[SIZE];
        for (int i = 0; i < values.length; i++) {
            values[i] = Value1.of(i);
        }
        return values;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object inter() {
        PNumber[] values = new PNumber[SIZE];
        for (int i = 0; i < values.length; i++) {
            values[i] = Value1.of(i);
        }
        return values;
    }

}
