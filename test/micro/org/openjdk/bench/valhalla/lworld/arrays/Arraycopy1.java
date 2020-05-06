package org.openjdk.bench.valhalla.lworld.arrays;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.SizedBase;
import org.openjdk.bench.valhalla.lworld.types.Utils;
import org.openjdk.bench.valhalla.lworld.types.Val1;
import org.openjdk.bench.valhalla.types.Vector;

public class Arraycopy1 extends SizedBase {

    Val1[] srcValue;
    Val1[] dstValue;

    Vector[] srcCovariance;
    Vector[] dstCovariance;

    Val1.ref[] srcBoxed;
    Val1.ref[] dstBoxed;

    @Setup
    public void setup() {
        srcValue = Utils.fillV(new Val1[size]);
        dstValue = new Val1[size];
        srcCovariance = Utils.fillV(new Val1[size]);
        dstCovariance = new Val1[size];
        srcBoxed = Utils.fillB(new Val1.ref[size]);
        dstBoxed = new Val1.ref[size];
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
