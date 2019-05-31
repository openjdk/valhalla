package org.openjdk.bench.valhalla.lworld.arrays;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.lworld.types.Val8;
import org.openjdk.bench.valhalla.types.Vector;

public class Set8 extends SizedBase {

    Val8[] values;
    Val8?[] boxed;
    Vector[] covariance;

    @Setup
    public void setup() {
        values = new Val8[size];
        boxed = new Val8?[size];
        covariance = new Val8[size];
    }


    @Benchmark
    public Object boxed() {
        Val8?[] values = boxed;
        for (int i = 0, k = 0; i < size; i++, k += 8) {
            values[i] = new Val8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }

    @Benchmark
    public Object value() {
        Val8[] values = this.values;
        for (int i = 0, k = 0; i < size; i++, k += 8) {
            values[i] = new Val8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }

    @Benchmark
    public Object covariance() {
        Vector[] values = covariance;
        for (int i = 0, k = 0; i < size; i++, k += 8) {
            values[i] = new Val8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }

    /*
     *  Hotspot successfully eliminated GC write barriers in case of assignment to a newly created array.
     */
    @Benchmark
    public Object newBoxed() {
        Val8?[] values = new Val8?[size];
        for (int i = 0, k = 0; i < size; i++, k += 8) {
            values[i] = new Val8(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }

}
