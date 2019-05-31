package org.openjdk.bench.valhalla.lworld.arrays;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.lworld.types.Utils;
import org.openjdk.bench.valhalla.lworld.types.Val1;

public class Sum1 extends SizedBase {

    Val1[] values;
    Val1?[] boxed;

    @Setup
    public void setup() {
        values = Utils.fillV(new Val1[size]);
        boxed = Utils.fillB(new Val1?[size]);
    }

    @Benchmark
    public int value() {
        Val1[] v = this.values;
        Val1 sum = new Val1(0);
        for (int i = 0; i < size; i++) {
            sum = sum.add(v[i]);
        }
        return sum.reduce();
    }

    @Benchmark
    public int valScalarized() {
        Val1[] v = this.values;
        int sum = 0;
        for (int i = 0; i < size; i++) {
            sum += v[i].f0;
        }
        return sum;
    }

    @Benchmark
    public int boxedV() {
        Val1?[] v = this.boxed;
        Val1 sum = new Val1(0);
        for (int i = 0; i < size; i++) {
            sum = sum.add((Val1)v[i]);
        }
        return sum.reduce();
    }

    @Benchmark
    public int boxedB() {
        Val1?[] v = this.boxed;
        Val1? sum = new Val1(0);
        for (int i = 0; i < size; i++) {
            sum = sum.add((Val1)v[i]);
        }
        return sum.reduce();
    }

    @Benchmark
    public int boxScalarized() {
        Val1?[] v = this.boxed;
        int sum = 0;
        for (int i = 0; i < size; i++) {
            sum += v[i].f0;
        }
        return sum;
    }

}
