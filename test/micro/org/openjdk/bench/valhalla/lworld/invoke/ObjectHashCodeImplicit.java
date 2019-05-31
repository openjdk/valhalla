package org.openjdk.bench.valhalla.lworld.invoke;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.CallBase;

public class ObjectHashCodeImplicit extends CallBase {

    public static inline class Val1 {
        public final int f0;
        public Val1(int f0) {
            this.f0 = f0;
        }
    }

    public static inline class Val2 {
        public final int f0;
        public Val2(int f0) {
            this.f0 = f0;
        }
    }

    public static inline class Val3 {
        public final int f0;
        public Val3(int f0) {
            this.f0 = f0;
        }
    }


    Val1[] values1;
    Val2[] values2;
    Val3[] values3;
    Val1?[] boxed1;
    Val2?[] boxed2;
    Val3?[] boxed3;

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
        boxed1 = new Val1?[SIZE];
        for (int i = 0; i < SIZE; i++) {
            boxed1[i] = new Val1(42);
        }
        boxed2 = new Val2?[SIZE];
        for (int i = 0; i < SIZE; i++) {
            boxed2[i] = new Val2(42);
        }
        boxed3 = new Val3?[SIZE];
        for (int i = 0; i < SIZE; i++) {
            boxed3[i] = new Val3(42);
        }
    }


    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int hashObject(Object[] arr) {
        int r = 0;
        for(Object o : arr) {
            r += o.hashCode();
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int hashExact(Val1[] arr) {
        int r = 0;
        for(Val1 o : arr) {
            r += o.hashCode();
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int hashExactBoxed(Val1?[] arr) {
        int r = 0;
        for(Val1? o : arr) {
            r += o.hashCode();
        }
        return r;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int value_exact() {
        return hashExact(values1) +
               hashExact(values1) +
               hashExact(values1) +
               hashExact(values1) +
               hashExact(values1) +
               hashExact(values1) ;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int boxed_exact() {
        return hashExactBoxed(boxed1) +
                hashExactBoxed(boxed1) +
                hashExactBoxed(boxed1) +
                hashExactBoxed(boxed1) +
                hashExactBoxed(boxed1) +
                hashExactBoxed(boxed1) ;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int boxed_targets1() {
        return hashObject(boxed1) +
                hashObject(boxed1) +
                hashObject(boxed1) +
                hashObject(boxed1) +
                hashObject(boxed1) +
                hashObject(boxed1) ;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int boxed_targets2() {
        return hashObject(boxed1) +
                hashObject(boxed2) +
                hashObject(boxed1) +
                hashObject(boxed2) +
                hashObject(boxed1) +
                hashObject(boxed2) ;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int boxed_targets3() {
        return hashObject(boxed1) +
                hashObject(boxed2) +
                hashObject(boxed3) +
                hashObject(boxed1) +
                hashObject(boxed2) +
                hashObject(boxed3) ;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int covariance_targets1() {
        return hashObject(values1) +
                hashObject(values1) +
                hashObject(values1) +
                hashObject(values1) +
                hashObject(values1) +
                hashObject(values1) ;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int covariance_targets2() {
        return hashObject(values1) +
                hashObject(values2) +
                hashObject(values1) +
                hashObject(values2) +
                hashObject(values1) +
                hashObject(values2) ;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int covariance_targets3() {
        return hashObject(values1) +
                hashObject(values2) +
                hashObject(values3) +
                hashObject(values1) +
                hashObject(values2) +
                hashObject(values3) ;
    }


}