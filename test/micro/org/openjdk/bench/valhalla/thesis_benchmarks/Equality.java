package org.openjdk.bench.valhalla.thesis_benchmarks;

import java.util.Random;

import jdk.internal.value.ValueClass;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.bench.valhalla.types.Q32int;


import java.util.concurrent.TimeUnit;

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
public class Equality {

    public static value class Line {
        @NullRestricted
        Point p1;
        @NullRestricted
        Point p2;

        public Line(Point p1, Point p2) {
            this.p1 = p1;
            this.p2 = p2;
        }
    }

    @ImplicitlyConstructible
    public static value class Point {
        int x;
        int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    static final int N = 100;
    static Line[] lineArray = new Line[N];

    public static void initialize() {
        // initialize randomly
        Random rand = new Random();
        for (int i = 0; i < lineArray.length; i++) {
            Point p1 = new Point(rand.nextInt(), rand.nextInt());
            Point p2 = new Point(rand.nextInt(), rand.nextInt());
            lineArray[i] = new Line(p1, p2);
        }
    }

    public static int findDuplicates() {
        int count = 0;
        for (int i = 0; i < lineArray.length; i++) {
            for (int j = i + 1; j < lineArray.length; j++) {
                if (lineArray[i] == lineArray[j]) {
                    count++;
                }
            }
        }
        return count;
    }


    @Benchmark
    @OperationsPerInvocation(100)
    public void benchmark() {
        initialize();
        findDuplicates();
    }
}