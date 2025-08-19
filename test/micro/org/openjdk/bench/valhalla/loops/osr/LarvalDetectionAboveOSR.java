package org.openjdk.bench.valhalla.loops.osr;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1, jvmArgsAppend = {"--enable-preview"})
@Warmup(iterations = 0)
@Measurement(iterations = 10)
public class LarvalDetectionAboveOSR {
    @Benchmark
    public long test() {
        return MyNumber.loop();
    }
}

value class MyNumber {
    static int MANY = 1_000_000_000;
    private long d0;

    MyNumber(long d0) {
        this.d0 = d0;
    }

    MyNumber add(long v) {
        return new MyNumber(d0 + v);
    }

    public static long loop() {
        MyNumber dec = new MyNumber(123);
        for (int i = 0; i < MANY; ++i) {
            dec = dec.add(i);
        }
        return dec.d0;
    }
}

