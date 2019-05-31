package org.openjdk.bench.valhalla.lworld.fields;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.lworld.types.Val8;

public class Sum8 extends SizedBase {

    NodeVal8[] values;
    NodeBox8[] boxed;

    @Setup
    public void setup() {
        values = NodeVal8.fill(NodeVal8.set(new NodeVal8[size]));
        boxed = NodeBox8.fill(NodeBox8.set(new NodeBox8[size]));
    }


    @Benchmark
    public int value() {
        NodeVal8[] v = this.values;
        Val8 sum = new Val8(0,0,0,0,0,0,0,0);
        for (int i = 0; i < size; i++) {
            sum = sum.add(v[i].f);
        }
        return sum.reduce();
    }

    @Benchmark
    public int valScalarized() {
        NodeVal8[] v = this.values;
        int f0 = 0;
        int f1 = 0;
        int f2 = 0;
        int f3 = 0;
        int f4 = 0;
        int f5 = 0;
        int f6 = 0;
        int f7 = 0;
        for (int i = 0; i < size; i++) {
            f0 += v[i].f.f0;
            f1 += v[i].f.f1;
            f2 += v[i].f.f2;
            f3 += v[i].f.f3;
            f4 += v[i].f.f4;
            f5 += v[i].f.f5;
            f6 += v[i].f.f6;
            f7 += v[i].f.f7;
        }
        return f0 + f1 + f2 + f3 + f4 + f5 + f6 + f7;
    }

    @Benchmark
    public int boxedV() {
        NodeBox8[] v = this.boxed;
        Val8 sum = new Val8(0,0,0,0,0,0,0,0);
        for (int i = 0; i < size; i++) {
            sum = sum.add((Val8)v[i].f);
        }
        return sum.reduce();
    }

    @Benchmark
    public int boxedB() {
        NodeBox8[] v = this.boxed;
        Val8? sum = new Val8(0,0,0,0,0,0,0,0);
        for (int i = 0; i < size; i++) {
            sum = sum.add((Val8)v[i].f);
        }
        return sum.reduce();
    }

    @Benchmark
    public int boxScalarized() {
        NodeBox8[] v = this.boxed;
        int f0 = 0;
        int f1 = 0;
        int f2 = 0;
        int f3 = 0;
        int f4 = 0;
        int f5 = 0;
        int f6 = 0;
        int f7 = 0;
        for (int i = 0; i < size; i++) {
            f0 += v[i].f.f0;
            f1 += v[i].f.f1;
            f2 += v[i].f.f2;
            f3 += v[i].f.f3;
            f4 += v[i].f.f4;
            f5 += v[i].f.f5;
            f6 += v[i].f.f6;
            f7 += v[i].f.f7;
        }
        return f0 + f1 + f2 + f3 + f4 + f5 + f6 + f7;
    }


}
