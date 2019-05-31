package org.openjdk.bench.valhalla.lworld.fields;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.lworld.types.Val2;

public class Set2 extends SizedBase {

    NodeVal2[] values;
    NodeBox2[] boxed;

    @Setup
    public void setup() {
        values = NodeVal2.set(new NodeVal2[size]);
        boxed = NodeBox2.set(new NodeBox2[size]);
    }


    @Benchmark
    public Object boxed() {
        NodeBox2[] values = boxed;
        for (int i = 0, k = 0; i < size; i++, k += 2) {
            values[i].f = new Val2(k, k + 1);
        }
        return values;
    }

    @Benchmark
    public Object value() {
        NodeVal2[] values = this.values;
        for (int i = 0, k = 0; i < size; i++, k += 2) {
            values[i].f = new Val2(k, k + 1);
        }
        return values;
    }

}
