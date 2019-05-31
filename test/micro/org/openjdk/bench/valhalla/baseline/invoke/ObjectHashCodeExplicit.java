package org.openjdk.bench.valhalla.baseline.invoke;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.CallBase;

public class ObjectHashCodeExplicit extends CallBase {

    public static class Ref1 {
        public final int f0;
        public Ref1(int f0) {
            this.f0 = f0;
        }
        @Override
        public int hashCode() {
            return f0;
        }
    }

    public static class Ref2 {
        public final int f0;
        public Ref2(int f0) {
            this.f0 = f0;
        }
        @Override
        public int hashCode() {
            return f0;
        }
    }

    public static class Ref3 {
        public final int f0;
        public Ref3(int f0) {
            this.f0 = f0;
        }
        @Override
        public int hashCode() {
            return f0;
        }
    }


    Ref1[] refs1;
    Ref2[] refs2;
    Ref3[] refs3;

    @Setup
    public void setup() {
        refs1 = new Ref1[SIZE];
        for (int i = 0; i < SIZE; i++) {
            refs1[i] = new Ref1(42);
        }
        refs2 = new Ref2[SIZE];
        for (int i = 0; i < SIZE; i++) {
            refs2[i] = new Ref2(42);
        }
        refs3 = new Ref3[SIZE];
        for (int i = 0; i < SIZE; i++) {
            refs3[i] = new Ref3(42);
        }
    }


    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int hash(Object[] objects) {
        int r = 0;
        for(Object o : objects) {
            r += o.hashCode();
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int hashExact(Ref1[] objects) {
        int r = 0;
        for(Ref1 o : objects) {
            r += o.hashCode();
        }
        return r;
    }

    /* targets 0 - means exact type */
    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int reference_exact() {
        return hashExact(refs1) +
               hashExact(refs1) +
               hashExact(refs1) +
               hashExact(refs1) +
               hashExact(refs1) +
               hashExact(refs1) ;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int reference_targets1() {
        return hash(refs1) +
                hash(refs1) +
                hash(refs1) +
                hash(refs1) +
                hash(refs1) +
                hash(refs1) ;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int reference_targets2() {
        return hash(refs1) +
                hash(refs2) +
                hash(refs1) +
                hash(refs2) +
                hash(refs1) +
                hash(refs2) ;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE * 6)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int reference_targets3() {
        return hash(refs1) +
                hash(refs2) +
                hash(refs3) +
                hash(refs1) +
                hash(refs2) +
                hash(refs3) ;
    }


}