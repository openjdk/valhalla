package org.openjdk.bench.valhalla.lworld.arrays;

import org.openjdk.bench.valhalla.lworld.types.Utils;
import org.openjdk.bench.valhalla.lworld.types.Val2;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.types.Vector;

public class Copy2 extends SizedBase {

    Val2[] srcValue;
    Val2[] dstValue;

    Vector[] srcCovariance;
    Vector[] dstCovariance;

    Val2?[] srcBoxed;
    Val2?[] dstBoxed;

    @Setup
    public void setup() {
        srcValue = Utils.fillV(new Val2[size]);
        dstValue = new Val2[size];
        srcCovariance = Utils.fillV(new Val2[size]);
        dstCovariance = new Val2[size];
        srcBoxed = Utils.fillB(new Val2?[size]);
        dstBoxed = new Val2?[size];
    }

    @Benchmark
    public void value() {
        Val2[] s = srcValue;
        Val2[] d = dstValue;
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
        Val2?[] s = srcBoxed;
        Val2?[] d = dstBoxed;
        for (int i = 0; i < size; i++) {
            d[i] = s[i];
        }
    }
}
