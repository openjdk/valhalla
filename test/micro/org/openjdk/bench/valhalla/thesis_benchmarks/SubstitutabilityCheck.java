package org.openjdk.bench.valhalla.thesis_benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
//import org.openjdk.bench.valhalla.thesis_benchmarks.CompareValue1;
//import org.openjdk.bench.valhalla.thesis_benchmarks.CompareValue2;

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
//@Fork(value = 1,
//        jvmArgsAppend = {"--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
//                "--enable-preview"})
@Fork(1)
//@CompilerControl(CompilerControl.Mode.COMPILE_ONLY)
public class SubstitutabilityCheck {

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static boolean compareFunc(CompareValue1 a, Object b) {
        return a == b;
    }

    @Benchmark
    @OperationsPerInvocation(1)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public boolean compare() {
        compareFunc(new CompareValue1(), new CompareValue1(6));
        return compareFunc(new CompareValue1(), new Object());
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static boolean compareFuncPEA() {
        CompareValue1 a = new CompareValue1();
        CompareValue1 b = new CompareValue1(7);
        return a == b;
    }

    @Benchmark
    @OperationsPerInvocation(1)
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public boolean comparePEA() {
        return compareFuncPEA();
    }


}