package org.openjdk.bench.valhalla.thesis_benchmarks;

import jdk.internal.value.ValueClass;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
public class NBodySimulation {


    static final int N = 1_000_000;
    static Particle[] particles = /*new Particle[N];*/(Particle[]) ValueClass.newNullRestrictedArray(Particle.class, N);

    static {
        initialize();
    }

    static void initialize() {
        for (int i = 0; i < N; i++)
            particles[i] = new Particle(Math.random(), Math.random(), Math.random(), 0.01, 0.01, 0.01);
    }

    @Benchmark
    @OperationsPerInvocation(N)
    //@CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void simulate() {
        for (int i = 0; i < N; i++)
            particles[i] = particles[i].update();
    }

}