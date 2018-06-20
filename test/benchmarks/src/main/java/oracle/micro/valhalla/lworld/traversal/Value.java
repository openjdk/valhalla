package oracle.micro.valhalla.lworld.traversal;

import oracle.micro.valhalla.TraversalBase;
import oracle.micro.valhalla.lworld.types.Value1;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Setup;

public abstract class Value extends TraversalBase {

    Value1[] values;

    public void setup(int[] a) {
        values = new Value1[a.length];
        for (int i = 0; i < a.length; i++) {
            values[i] = Value1.of(a[i]);
        }
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public static int walk(Value1[] a) {
        int steps = 1;
        for(int i = a[0].f0; i!=0; i=a[i].f0) steps++;
        return steps;
    }


    public static class W0001 extends Value {

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

    public static class W0004 extends Value {

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

    public static class W0016 extends Value {

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

    public static class W0032 extends Value {

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

    public static class W0128 extends Value {

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

    public static class W0256 extends Value {

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

    public static class W1024 extends Value {

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
