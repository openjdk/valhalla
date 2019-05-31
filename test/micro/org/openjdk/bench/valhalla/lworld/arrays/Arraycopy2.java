package org.openjdk.bench.valhalla.lworld.arrays;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.lworld.types.Utils;
import org.openjdk.bench.valhalla.lworld.types.Val2;
import org.openjdk.bench.valhalla.types.Vector;

public class Arraycopy2 extends SizedBase {

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
        System.arraycopy(srcValue, 0, dstValue, 0, size);
    }

    @Benchmark
    public void covariance() {
        System.arraycopy(srcCovariance, 0, dstCovariance, 0, size);
    }

    @Benchmark
    public void boxed() {
        System.arraycopy(srcBoxed, 0, dstBoxed, 0, size);
    }
}
