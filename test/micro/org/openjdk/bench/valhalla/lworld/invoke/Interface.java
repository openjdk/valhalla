package org.openjdk.bench.valhalla.lworld.invoke;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.CallBase;

public class Interface extends CallBase {

    public interface MyInterface {
        public int my_method();
    }

    public static inline class Val1 implements MyInterface {
        public final int f0;
        public Val1(int f0) {
            this.f0 = f0;
        }
        @Override
        public int my_method() {
            return f0;
        }
    }

    public static inline class Val2 implements MyInterface {
        public final int f0;
        public Val2(int f0) {
            this.f0 = f0;
        }
        @Override
        public int my_method() {
            return f0;
        }
    }

    public static inline class Val3 implements MyInterface {
        public final int f0;
        public Val3(int f0) {
            this.f0 = f0;
        }
        @Override
        public int my_method() {
            return f0;
        }
    }


    Val1[] values1;
    Val2[] values2;
    Val3[] values3;
    MyInterface[] boxed1;
    MyInterface[] boxed2;
    MyInterface[] boxed3;

    @Setup
    public void setup() {
        values1 = new Val1[SIZE];
        for (int i = 0; i < SIZE; i++) {
            values1[i] = new Val1(42);
        }
        values2 = new Val2[SIZE];
        for (int i = 0; i < SIZE; i++) {
            values2[i] = new Val2(42);
        }
        values3 = new Val3[SIZE];
        for (int i = 0; i < SIZE; i++) {
            values3[i] = new Val3(42);
        }
        boxed1 = new MyInterface[SIZE];
        for (int i = 0; i < SIZE; i++) {
            boxed1[i] = new Val1(42);
        }
        boxed2 = new MyInterface[SIZE];
        for (int i = 0; i < SIZE; i++) {
            boxed2[i] = new Val2(42);
        }
        boxed3 = new MyInterface[SIZE];
        for (int i = 0; i < SIZE; i++) {
            boxed3[i] = new Val3(42);
        }
    }


    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int reduceInterface(MyInterface[] arr) {
        int r = 0;
        for(MyInterface o : arr) {
            r += o.my_method();
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int reduceExact(Val1[] arr) {
        int r = 0;
        for(Val1 o : arr) {
            r += o.my_method();
        }
        return r;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int value_exact() {
        return reduceExact(values1) +
               reduceExact(values1) +
               reduceExact(values1) +
               reduceExact(values1) +
               reduceExact(values1) +
               reduceExact(values1) ;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int boxed_targets1() {
        return reduceInterface(boxed1) +
                reduceInterface(boxed1) +
                reduceInterface(boxed1) +
                reduceInterface(boxed1) +
                reduceInterface(boxed1) +
                reduceInterface(boxed1) ;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int boxed_targets2() {
        return reduceInterface(boxed1) +
                reduceInterface(boxed2) +
                reduceInterface(boxed1) +
                reduceInterface(boxed2) +
                reduceInterface(boxed1) +
                reduceInterface(boxed2) ;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int boxed_targets3() {
        return reduceInterface(boxed1) +
                reduceInterface(boxed2) +
                reduceInterface(boxed3) +
                reduceInterface(boxed1) +
                reduceInterface(boxed2) +
                reduceInterface(boxed3) ;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int covariance_targets1() {
        return reduceInterface(values1) +
                reduceInterface(values1) +
                reduceInterface(values1) +
                reduceInterface(values1) +
                reduceInterface(values1) +
                reduceInterface(values1) ;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int covariance_targets2() {
        return reduceInterface(values1) +
                reduceInterface(values2) +
                reduceInterface(values1) +
                reduceInterface(values2) +
                reduceInterface(values1) +
                reduceInterface(values2) ;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int covariance_targets3() {
        return reduceInterface(values1) +
                reduceInterface(values2) +
                reduceInterface(values3) +
                reduceInterface(values1) +
                reduceInterface(values2) +
                reduceInterface(values3) ;
    }


}