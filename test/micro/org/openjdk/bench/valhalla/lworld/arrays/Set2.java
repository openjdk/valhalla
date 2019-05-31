package org.openjdk.bench.valhalla.lworld.arrays;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.lworld.types.Val2;
import org.openjdk.bench.valhalla.types.Vector;

public class Set2 extends SizedBase {

    Val2[] values;
    Val2?[] boxed;
    Vector[] covariance;

    @Setup
    public void setup() {
        values = new Val2[size];
        boxed = new Val2?[size];
        covariance = new Val2[size];
    }


    @Benchmark
    public Object boxed() {
        Val2?[] values = boxed;
        for (int i = 0, k = 0; i < size; i++, k += 2) {
            values[i] = new Val2(k, k + 1);
        }
        return values;
    }

    @Benchmark
    public Object value() {
        Val2[] values = this.values;
        for (int i = 0, k = 0; i < size; i++, k += 2) {
            values[i] = new Val2(k, k + 1);
        }
        return values;
    }

    @Benchmark
    public Object covariance() {
        Vector[] values = covariance;
        for (int i = 0, k = 0; i < size; i++, k += 2) {
            values[i] = new Val2(k, k + 1);
        }
        return values;
    }

    /*
     *  Hotspot successfully eliminated GC write barriers in case of assignment to a newly created array.
     */
    @Benchmark
    public Object newBoxed() {
        Val2?[] values = new Val2?[size];
        for (int i = 0, k = 0; i < size; i++, k += 2) {
            values[i] = new Val2(k, k + 1);
        }
        return values;
    }

}
