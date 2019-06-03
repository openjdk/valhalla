package org.openjdk.bench.valhalla.lworld.fields;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.lworld.types.Val1;

public class Sum1 extends SizedBase {

    NodeVal1[] values;
    NodeBox1[] boxed;

    @Setup
    public void setup() {
        values = NodeVal1.fill(NodeVal1.set(new NodeVal1[size]));
        boxed = NodeBox1.fill(NodeBox1.set(new NodeBox1[size]));
    }

    @Benchmark
    public int value() {
        NodeVal1[] v = this.values;
        Val1 sum = new Val1(0);
        for (int i = 0; i < size; i++) {
            sum = sum.add(v[i].f);
        }
        return sum.reduce();
    }

    @Benchmark
    public int valScalarized() {
        NodeVal1[] v = this.values;
        int sum = 0;
        for (int i = 0; i < size; i++) {
            sum += v[i].f.f0;
        }
        return sum;
    }

    @Benchmark
    public int boxed() {
        NodeBox1[] v = this.boxed;
        Val1? sum = new Val1(0);
        for (int i = 0; i < size; i++) {
            sum = sum.add((Val1)v[i].f);
        }
        return sum.reduce();
    }

    @Benchmark
    public int boxScalarized() {
        NodeBox1[] v = this.boxed;
        int sum = 0;
        for (int i = 0; i < size; i++) {
            sum += v[i].f.f0;
        }
        return sum;
    }

}
