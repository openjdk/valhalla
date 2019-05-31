package org.openjdk.bench.valhalla.lworld.arrays;

import org.openjdk.bench.valhalla.lworld.types.Utils;
import org.openjdk.bench.valhalla.lworld.types.Val8;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.types.Vector;

public class Copy8 extends SizedBase {

    Val8[] srcValue;
    Val8[] dstValue;

    Vector[] srcCovariance;
    Vector[] dstCovariance;

    Val8?[] srcBoxed;
    Val8?[] dstBoxed;

    @Setup
    public void setup() {
        srcValue = Utils.fillV(new Val8[size]);
        dstValue = new Val8[size];
        srcCovariance = Utils.fillV(new Val8[size]);
        dstCovariance = new Val8[size];
        srcBoxed = Utils.fillB(new Val8?[size]);
        dstBoxed = new Val8?[size];
    }

    @Benchmark
    public void value() {
        Val8[] s = srcValue;
        Val8[] d = dstValue;
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
        Val8?[] s = srcBoxed;
        Val8?[] d = dstBoxed;
        for (int i = 0; i < size; i++) {
            d[i] = s[i];
        }
    }

}
