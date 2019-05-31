package org.openjdk.bench.valhalla.lworld.fields;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.lworld.types.Val8;

public class Set8 extends SizedBase {

    NodeVal8[] values;
    NodeBox8[] boxed;

    @Setup
    public void setup() {
        values = NodeVal8.set(new NodeVal8[size]);
        boxed = NodeBox8.set(new NodeBox8[size]);
    }


    @Benchmark
    public Object boxed() {
        NodeBox8[] values = boxed;
        for (int i = 0, k = 0; i < size; i++, k += 8) {
            values[i].f = new Val8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }

    @Benchmark
    public Object value() {
        NodeVal8[] values = this.values;
        for (int i = 0, k = 0; i < size; i++, k += 8) {
            values[i].f = new Val8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }

}
