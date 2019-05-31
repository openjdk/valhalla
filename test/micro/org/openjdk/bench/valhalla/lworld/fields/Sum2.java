package org.openjdk.bench.valhalla.lworld.fields;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.lworld.types.Val2;

public class Sum2 extends SizedBase {

    NodeVal2[] values;
    NodeBox2[] boxed;

    @Setup
    public void setup() {
        values = NodeVal2.fill(NodeVal2.set(new NodeVal2[size]));
        boxed = NodeBox2.fill(NodeBox2.set(new NodeBox2[size]));
    }


    @Benchmark
    public int value() {
        NodeVal2[] v = this.values;
        Val2 sum = new Val2(0,0);
        for (int i = 0; i < size; i++) {
            sum = sum.add(v[i].f);
        }
        return sum.reduce();
    }

    @Benchmark
    public int valScalarized() {
        NodeVal2[] v = this.values;
        int f0 = 0;
        int f1 = 0;
        for (int i = 0; i < size; i++) {
            f0 += v[i].f.f0;
            f1 += v[i].f.f1;
        }
        return f0 + f1;
    }

    @Benchmark
    public int boxedV() {
        NodeBox2[] v = this.boxed;
        Val2 sum = new Val2(0, 0);
        for (int i = 0; i < size; i++) {
            sum = sum.add((Val2)v[i].f);
        }
        return sum.reduce();
    }

    @Benchmark
    public int boxedB() {
        NodeBox2[] v = this.boxed;
        Val2? sum = new Val2(0, 0);
        for (int i = 0; i < size; i++) {
            sum = sum.add((Val2)v[i].f);
        }
        return sum.reduce();
    }

    @Benchmark
    public int boxScalarized() {
        NodeBox2[] v = this.boxed;
        int f0 = 0;
        int f1 = 0;
        for (int i = 0; i < size; i++) {
            f0 += v[i].f.f0;
            f1 += v[i].f.f1;
        }
        return f0 + f1;
    }
    
}
