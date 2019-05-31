package org.openjdk.bench.valhalla.lworld.arrays;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.lworld.types.Utils;
import org.openjdk.bench.valhalla.lworld.types.Val1;
import org.openjdk.bench.valhalla.types.Vector;

public class Copy1 extends SizedBase {

    Val1[] srcValue;
    Val1[] dstValue;

    Vector[] srcCovariance;
    Vector[] dstCovariance;

    Val1?[] srcBoxed;
    Val1?[] dstBoxed;



    @Setup
    public void setup() {
        srcValue = Utils.fillV(new Val1[size]);
        dstValue = new Val1[size];
        srcCovariance = Utils.fillV(new Val1[size]);
        dstCovariance = new Val1[size];
        srcBoxed = Utils.fillB(new Val1?[size]);
        dstBoxed = new Val1?[size];
    }

    @Benchmark
    public void value() {
        Val1[] s = srcValue;
        Val1[] d = dstValue;
        for (int i = 0; i < size; i++) {
            d[i] = s[i];
        }
    }

    @Benchmark
    public void covariance() {
        Vector[] s = srcCovariance;
        Vector[] d = dstCovariance;
        for (int i = 0; i < size; i++) {
            d[i] = s[i];
        }
    }

    @Benchmark
    public void boxed() {
        Val1?[] s = srcBoxed;
        Val1?[] d = dstBoxed;
        for (int i = 0; i < size; i++) {
            d[i] = s[i];
        }
    }

}
