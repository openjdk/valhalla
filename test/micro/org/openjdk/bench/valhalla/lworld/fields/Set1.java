package org.openjdk.bench.valhalla.lworld.fields;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.lworld.types.Val1;

public class Set1 extends SizedBase {

    NodeVal1[] values;
    NodeBox1[] boxed;

    @Setup
    public void setup() {
        values = NodeVal1.set(new NodeVal1[size]);
        boxed = NodeBox1.set(new NodeBox1[size]);
    }


    @Benchmark
    public Object boxed() {
        NodeBox1[] values = boxed;
        for (int i = 0; i < size; i++) {
            values[i].f = new Val1(i);
        }
        return values;
    }

    @Benchmark
    public Object value() {
        NodeVal1[] values = this.values;
        for (int i = 0; i < size; i++) {
            values[i].f = new Val1(i);
        }
        return values;
    }

}
