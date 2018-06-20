package oracle.micro.valhalla.lworld.arraysum;

import oracle.micro.valhalla.ArraysumBase;
import oracle.micro.valhalla.lworld.types.Value2;
import org.openjdk.jmh.annotations.*;

public class Arraysum2 extends ArraysumBase {

    public static Value2[] setupValue(int size) {
        Value2[] values = new Value2[size];
        for (int i = 0, k = 0; i < values.length; i++, k += 2) {
            values[i] = Value2.of(k, k + 1);
        }
        return values;
    }

    public static int sumScalarized(Value2[] values ) {
        int f0 = 0;
        int f1 = 0;
        for (int i = 0; i < values.length; i++) {
            f0 += values[i].f0;
            f1 += values[i].f1;
        }
        return f0 + f1;
    }

    public static int sum(Value2[] values) {
        Value2 sum = Value2.of(0, 0);
        for (int i = 0; i < values.length; i++) {
            sum = sum.add(values[i]);
        }
        return sum.totalsum();
    }

    Value2[] values;

    @Setup
    public void setup() {
        values = setupValue(size);
    }

    @Benchmark
    public int valueScalarized() {
        return sumScalarized(values);
    }

    @Benchmark
    public int value() {
        return sum(values);
    }

}
