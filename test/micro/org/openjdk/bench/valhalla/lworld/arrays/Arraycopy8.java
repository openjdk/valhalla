package org.openjdk.bench.valhalla.lworld.arrays;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.lworld.types.Utils;
import org.openjdk.bench.valhalla.lworld.types.Val8;
import org.openjdk.bench.valhalla.types.Vector;

public class Arraycopy8 extends SizedBase {

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
