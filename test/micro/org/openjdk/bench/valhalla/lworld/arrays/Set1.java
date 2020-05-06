package org.openjdk.bench.valhalla.lworld.arrays;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.lworld.types.Val1;
import org.openjdk.bench.valhalla.types.Vector;

public class Set1 extends SizedBase {

    Val1[] values;
    Val1.ref[] boxed;
    Vector[] covariance;

    @Setup
    public void setup() {
        values = new Val1[size];
        boxed = new Val1.ref[size];
        covariance = new Val1[size];
    }


    @Benchmark
    public Object boxed() {
        Val1.ref[] values = boxed;
        for (int i = 0; i < size; i++) {
            values[i] = new Val1(i);
        }
        return values;
    }

    @Benchmark
    public Object value() {
        Val1[] values = this.values;
        for (int i = 0; i < size; i++) {
            values[i] = new Val1(i);
        }
        return values;
    }

    @Benchmark
    public Object covariance() {
        Vector[] values = covariance;
        for (int i = 0; i < size; i++) {
            values[i] = new Val1(i);
        }
        return values;
    }

    /*
     *  Hotspot successfully eliminated GC write barriers in case of assignment to a newly created array.
     */
    @Benchmark
    public Object newBoxed() {
        Val1.ref[] values = new Val1.ref[size];
        for (int i = 0; i < size; i++) {
            values[i] = new Val1(i);
        }
        return values;
    }


}
