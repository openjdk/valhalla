package org.openjdk.bench.valhalla.lworld.traversal;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.TraversalBase;
import org.openjdk.bench.valhalla.lworld.types.Val1;

public abstract class Boxed extends TraversalBase {

    Val1?[] values;

    public void setup(int[] a) {
        values = new Val1?[a.length];
        for (int i = 0; i < a.length; i++) {
            values[i] = new Val1(a[i]);
        }
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public static int walk(Val1?[] a) {
        int steps = 0;
        for(int i = a[0].reduce(); i!=0; i=a[i].reduce()) steps++;
        return steps;
    }


    public static class W0001 extends Boxed {

        private static final int SIZE = 1*K;

        @Setup
        public void setup() {
            setup(prepare(SIZE, shuffle));
        }

        @Benchmark
        @OperationsPerInvocation(SIZE)
        public int walk() {
            return walk(values);
        }
    }

    public static class W0004 extends Boxed {

        private static final int SIZE = 4*K;

        @Setup
        public void setup() {
            setup(prepare(SIZE, shuffle));
        }

        @Benchmark
        @OperationsPerInvocation(SIZE)
        public int walk() {
            return walk(values);
        }
    }

    public static class W0016 extends Boxed {

        private static final int SIZE = 16*K;

        @Setup
        public void setup() {
            setup(prepare(SIZE, shuffle));
        }

        @Benchmark
        @OperationsPerInvocation(SIZE)
        public int walk() {
            return walk(values);
        }
    }

    public static class W0032 extends Boxed {

        private static final int SIZE = 32*K;

        @Setup
        public void setup() {
            setup(prepare(SIZE, shuffle));
        }

        @Benchmark
        @OperationsPerInvocation(SIZE)
        public int walk() {
            return walk(values);
        }
    }

    public static class W0128 extends Boxed {

        private static final int SIZE = 128*K;

        @Setup
        public void setup() {
            setup(prepare(SIZE, shuffle));
        }

        @Benchmark
        @OperationsPerInvocation(SIZE)
        public int walk() {
            return walk(values);
        }
    }

    public static class W0256 extends Boxed {

        private static final int SIZE = 256*K;

        @Setup
        public void setup() {
            setup(prepare(SIZE, shuffle));
        }

        @Benchmark
        @OperationsPerInvocation(SIZE)
        public int walk() {
            return walk(values);
        }
    }

    public static class W1024 extends Boxed {

        private static final int SIZE = 1024*K;

        @Setup
        public void setup() {
            setup(prepare(SIZE, shuffle));
        }

        @Benchmark
        @OperationsPerInvocation(SIZE)
        public int walk() {
            return walk(values);
        }
    }


}
