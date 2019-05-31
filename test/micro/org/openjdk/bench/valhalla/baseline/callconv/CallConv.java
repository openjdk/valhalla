package org.openjdk.bench.valhalla.baseline.callconv;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.CallBase;

public class CallConv extends CallBase {

    public static class Ref {

        public final int f0;

        public Ref(int f0) {
            this.f0 = f0;
        }
    }

    abstract static class InvocationLogic {

        public abstract int compute(int v1);
        public abstract int compute(int v1, int v2);
        public abstract int compute(int v1, int v2, int v3, int v4);
        public abstract int compute(int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8);

        public abstract Ref compute(Ref v1);
        public abstract Ref compute(Ref v1, Ref v2);
        public abstract Ref compute(Ref v1, Ref v2, Ref v3, Ref v4);
        public abstract Ref compute(Ref v1, Ref v2, Ref v3, Ref v4, Ref v5, Ref v6, Ref v7, Ref v8);

    }

    static class InvokeImpl1 extends InvocationLogic {

        @Override
        public int compute(int v1) {
            return v1;
        }

        @Override
        public int compute(int v1, int v2) {
            return v1;
        }

        @Override
        public int compute(int v1, int v2, int v3, int v4) {
            return v1;
        }

        @Override
        public int compute(int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8) {
            return v1;
        }

        @Override
        public Ref compute(Ref v1) {
            return v1;
        }

        @Override
        public Ref compute(Ref v1, Ref v2) {
            return v1;
        }

        @Override
        public Ref compute(Ref v1, Ref v2, Ref v3, Ref v4) {
            return v1;
        }

        @Override
        public Ref compute(Ref v1, Ref v2, Ref v3, Ref v4, Ref v5, Ref v6, Ref v7, Ref v8) {
            return v1;
        }

    }

    static class InvokeImpl2 extends InvocationLogic {

        @Override
        public int compute(int v1) {
            return v1;
        }

        @Override
        public int compute(int v1, int v2) {
            return v1;
        }

        @Override
        public int compute(int v1, int v2, int v3, int v4) {
            return v1;
        }

        @Override
        public int compute(int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8) {
            return v1;
        }

        @Override
        public Ref compute(Ref v1) {
            return v1;
        }

        @Override
        public Ref compute(Ref v1, Ref v2) {
            return v1;
        }

        @Override
        public Ref compute(Ref v1, Ref v2, Ref v3, Ref v4) {
            return v1;
        }

        @Override
        public Ref compute(Ref v1, Ref v2, Ref v3, Ref v4, Ref v5, Ref v6, Ref v7, Ref v8) {
            return v1;
        }

    }

    static class InvokeImpl3 extends InvocationLogic {

        @Override
        public int compute(int v1) {
            return v1;
        }

        @Override
        public int compute(int v1, int v2) {
            return v1;
        }

        @Override
        public int compute(int v1, int v2, int v3, int v4) {
            return v1;
        }

        @Override
        public int compute(int v1, int v2, int v3, int v4, int v5, int v6, int v7, int v8) {
            return v1;
        }

        @Override
        public Ref compute(Ref v1) {
            return v1;
        }

        @Override
        public Ref compute(Ref v1, Ref v2) {
            return v1;
        }

        @Override
        public Ref compute(Ref v1, Ref v2, Ref v3, Ref v4) {
            return v1;
        }

        @Override
        public Ref compute(Ref v1, Ref v2, Ref v3, Ref v4, Ref v5, Ref v6, Ref v7, Ref v8) {
            return v1;
        }

    }

    public static final int SIZE = 1 * 2 * 3; // all possible targets

    InvokeImpl1[] logic0;
    InvocationLogic[] logic1;
    InvocationLogic[] logic2;
    InvocationLogic[] logic3;



    @Setup
    public void setup() {
        logic0 = new InvokeImpl1[SIZE];
        for (int i = 0; i < SIZE; i++) {
            logic0[i] = new InvokeImpl1();
        }
        logic1 = populate(1);
        logic2 = populate(2);
        logic3 = populate(3);
    }

    private InvocationLogic[] populate(int targets) {
        assert targets > 0 && targets <= 3;
        InvocationLogic[] logic = new InvocationLogic[SIZE];
        for (int i = 0; i < SIZE; i++) {
            switch (i % targets) {
                case 0:
                    logic[i] = new InvokeImpl1();
                    break;
                case 1:
                    logic[i] = new InvokeImpl2();
                    break;
                case 2:
                    logic[i] = new InvokeImpl3();
                    break;
            }
        }
        return logic;
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public int primitive1(InvocationLogic[] logic) {
        int r = 0;
        for(InvocationLogic t : logic) {
            r += t.compute(42);
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public int primitive2(InvocationLogic[] logic) {
        int r = 0;
        for(InvocationLogic t : logic) {
            r += t.compute(42, 43);
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public int primitive4(InvocationLogic[] logic) {
        int r = 0;
        for(InvocationLogic t : logic) {
            r += t.compute(42, 43, 44, 45);
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public int primitive8(InvocationLogic[] logic) {
        int r = 0;
        for(InvocationLogic t : logic) {
            r += t.compute(42, 43, 44, 45, 46, 47, 48, 49);
        }
        return r;
    }

    Ref a1 = new Ref(42);
    Ref a2 = new Ref(43);
    Ref a3 = new Ref(44);
    Ref a4 = new Ref(45);
    Ref a5 = new Ref(46);
    Ref a6 = new Ref(47);
    Ref a7 = new Ref(48);
    Ref a8 = new Ref(49);

    @CompilerControl(CompilerControl.Mode.INLINE)
    public int reference1(InvocationLogic[] logic) {
        int r = 0;
        for(InvocationLogic t : logic) {
            r += t.compute(a1).f0;
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public int reference2(InvocationLogic[] logic) {
        int r = 0;
        for(InvocationLogic t : logic) {
            r += t.compute(a1, a2).f0;
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public int reference4(InvocationLogic[] logic) {
        int r = 0;
        for(InvocationLogic t : logic) {
            r += t.compute(a1, a2, a3, a4).f0;
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public int reference8(InvocationLogic[] logic) {
        int r = 0;
        for(InvocationLogic t : logic) {
            r += t.compute(a1, a2, a3, a4, a5, a6, a7, a8).f0;
        }
        return r;
    }

    /* targets 0 - means exact type */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int primitive_args1_targets0() {
        return primitive1(logic0);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int primitive_args1_targets1() {
        return primitive1(logic1);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int primitive_args1_targets2() {
        return primitive1(logic2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int primitive_args1_targets3() {
        return primitive1(logic3);
    }

    /* targets 0 - means exact type */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int primitive_args2_targets0() {
        return primitive2(logic0);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int primitive_args2_targets1() {
        return primitive2(logic1);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int primitive_args2_targets2() {
        return primitive2(logic2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int primitive_args2_targets3() {
        return primitive2(logic3);
    }

    /* targets 0 - means exact type */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int primitive_args4_targets0() {
        return primitive4(logic0);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int primitive_args4_targets1() {
        return primitive4(logic1);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int primitive_args4_targets2() {
        return primitive4(logic2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int primitive_args4_targets3() {
        return primitive4(logic3);
    }

    /* targets 0 - means exact type */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int primitive_args8_targets0() {
        return primitive8(logic0);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int primitive_args8_targets1() {
        return primitive8(logic1);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int primitive_args8_targets2() {
        return primitive8(logic2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int primitive_args8_targets3() {
        return primitive8(logic3);
    }

    /* targets 0 - means exact type */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int reference_args1_targets0() {
        return reference1(logic0);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int reference_args1_targets1() {
        return reference1(logic1);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int reference_args1_targets2() {
        return reference1(logic2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int reference_args1_targets3() {
        return reference1(logic3);
    }

    /* targets 0 - means exact type */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int reference_args2_targets0() {
        return reference2(logic0);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int reference_args2_targets1() {
        return reference2(logic1);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int reference_args2_targets2() {
        return reference2(logic2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int reference_args2_targets3() {
        return reference2(logic3);
    }

    /* targets 0 - means exact type */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int reference_args4_targets0() {
        return reference4(logic0);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int reference_args4_targets1() {
        return reference4(logic1);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int reference_args4_targets2() {
        return reference4(logic2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int reference_args4_targets3() {
        return reference4(logic3);
    }

    /* targets 0 - means exact type */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int reference_args8_targets0() {
        return reference8(logic0);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int reference_args8_targets1() {
        return reference8(logic1);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int reference_args8_targets2() {
        return reference8(logic2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int reference_args8_targets3() {
        return reference8(logic3);
    }

}