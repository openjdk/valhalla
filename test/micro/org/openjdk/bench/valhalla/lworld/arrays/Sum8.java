package org.openjdk.bench.valhalla.lworld.arrays;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.lworld.types.Utils;
import org.openjdk.bench.valhalla.lworld.types.Val8;

public class Sum8 extends SizedBase {

    Val8[] values;
    Val8?[] boxed;

    @Setup
    public void setup() {
        values = Utils.fillV(new Val8[size]);
        boxed = Utils.fillB(new Val8?[size]);
    }

    @Benchmark
    public int value() {
        Val8[] v = this.values;
        Val8 sum = new Val8(0,0,0,0,0,0,0,0);
        for (int i = 0; i < size; i++) {
            sum = sum.add(v[i]);
        }
        return sum.reduce();
    }

    @Benchmark
    public int valScalarized() {
        Val8[] v = this.values;
        int f0 = 0;
        int f1 = 0;
        int f2 = 0;
        int f3 = 0;
        int f4 = 0;
        int f5 = 0;
        int f6 = 0;
        int f7 = 0;
        for (int i = 0; i < size; i++) {
            f0 += v[i].f0;
            f1 += v[i].f1;
            f2 += v[i].f2;
            f3 += v[i].f3;
            f4 += v[i].f4;
            f5 += v[i].f5;
            f6 += v[i].f6;
            f7 += v[i].f7;
        }
        return f0 + f1 + f2 + f3 + f4 + f5 + f6 + f7;
    }

    @Benchmark
    public int boxed() {
        Val8?[] v = this.boxed;
        Val8? sum = new Val8(0,0,0,0,0,0,0,0);
        for (int i = 0; i < size; i++) {
            sum = sum.add((Val8)v[i]);
        }
        return sum.reduce();
    }

    @Benchmark
    public int boxScalarized() {
        Val8?[] v = this.boxed;
        int f0 = 0;
        int f1 = 0;
        int f2 = 0;
        int f3 = 0;
        int f4 = 0;
        int f5 = 0;
        int f6 = 0;
        int f7 = 0;
        for (int i = 0; i < size; i++) {
            f0 += v[i].f0;
            f1 += v[i].f1;
            f2 += v[i].f2;
            f3 += v[i].f3;
            f4 += v[i].f4;
            f5 += v[i].f5;
            f6 += v[i].f6;
            f7 += v[i].f7;
        }
        return f0 + f1 + f2 + f3 + f4 + f5 + f6 + f7;
    }


}
