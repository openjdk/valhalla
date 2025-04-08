package org.openjdk.bench.valhalla.thesis_benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.bench.valhalla.thesis_benchmarks.MyInterface1;
import org.openjdk.bench.valhalla.thesis_benchmarks.MyObject;
import org.openjdk.bench.valhalla.thesis_benchmarks.MyValue1;
import org.openjdk.bench.valhalla.thesis_benchmarks.MyValue2;
import org.openjdk.bench.valhalla.thesis_benchmarks.MyValue3;
import org.openjdk.bench.valhalla.thesis_benchmarks.MyValue4;
import org.openjdk.bench.valhalla.thesis_benchmarks.OtherVal;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.annotations.CompilerControl;

import java.util.concurrent.TimeUnit;

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
public class CallingConvention {

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static int test21(MyInterface1 intf, MyValue4 v, int y) {
        return intf.test10(v, y, y, y, y, y, y).getValue();
    }

    public static final int rI = 1;

    @Benchmark
    @OperationsPerInvocation(1)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void testFunc() {
        MyValue1 val1 = new MyValue1(rI);
        MyValue2 val2 = new MyValue2(rI + 1);
        MyValue3 val3 = new MyValue3(rI + 2);
        MyValue4 val4 = new MyValue4(rI + 3);
        OtherVal other = new OtherVal(rI + 4);
        MyObject obj = new MyObject(rI + 5);
        test21(val1, val4, rI);
        test21(val2, val4, rI);
        test21(val3, val4, rI);
        test21(val4, val4, rI);
        test21(obj, val4, rI);
    }

}