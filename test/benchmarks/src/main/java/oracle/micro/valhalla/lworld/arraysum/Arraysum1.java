package oracle.micro.valhalla.lworld.arraysum;

import oracle.micro.valhalla.ArraysumBase;
import oracle.micro.valhalla.lworld.types.Value1;
import org.openjdk.jmh.annotations.*;

public class Arraysum1 extends ArraysumBase {

    public static Value1[] setupValue(int size) {
        Value1[] values = new Value1[size];
        for (int i = 0; i < values.length; i++) {
            values[i] = Value1.of(i);
        }
        return values;
    }

    public static int sumScalarized(Value1[] values ) {
        int sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += values[i].f0;
        }
        return sum;
    }

    public static int sum(Value1[] values) {
        Value1 sum = Value1.of(0);
        for (int i = 0; i < values.length; i++) {
            sum = sum.add(values[i]);
        }
        return sum.totalsum();
    }

    Value1[] values;

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
