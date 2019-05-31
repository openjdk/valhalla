package org.openjdk.bench.valhalla.lworld.arrays;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.lworld.types.Utils;
import org.openjdk.bench.valhalla.lworld.types.Val2;

public class Sum2 extends SizedBase {

    Val2[] values;
    Val2?[] boxed;

    @Setup
    public void setup() {
        values = Utils.fillV(new Val2[size]);
        boxed = Utils.fillB(new Val2?[size]);
    }

    @Benchmark
    public int value() {
        Val2[] v = this.values;
        Val2 sum = new Val2(0,0);
        for (int i = 0; i < size; i++) {
            sum = sum.add(v[i]);
        }
        return sum.reduce();
    }

    @Benchmark
    public int valScalarized() {
        Val2[] v = this.values;
        int f0 = 0;
        int f1 = 0;
        for (int i = 0; i < size; i++) {
            f0 += v[i].f0;
            f1 += v[i].f1;
        }
        return f0 + f1;
    }

    @Benchmark
    public int boxedV() {
        Val2?[] v = this.boxed;
        Val2 sum = new Val2(0, 0);
        for (int i = 0; i < size; i++) {
            sum = sum.add((Val2)v[i]);
        }
        return sum.reduce();
    }

    @Benchmark
    public int boxedB() {
        Val2?[] v = this.boxed;
        Val2? sum = new Val2(0, 0);
        for (int i = 0; i < size; i++) {
            sum = sum.add((Val2)v[i]);
        }
        return sum.reduce();
    }

    @Benchmark
    public int boxScalarized() {
        Val2?[] v = this.boxed;
        int f0 = 0;
        int f1 = 0;
        for (int i = 0; i < size; i++) {
            f0 += v[i].f0;
            f1 += v[i].f1;
        }
        return f0 + f1;
    }
    
}
