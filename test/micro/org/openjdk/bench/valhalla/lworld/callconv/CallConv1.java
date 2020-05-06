package org.openjdk.bench.valhalla.lworld.callconv;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.bench.valhalla.CallBase;


public class CallConv1 extends CallBase {

    public static inline class Val {

        public final int f0;

        public Val(int f0) {
            this.f0 = f0;
        }
    }

    abstract static class InvocationLogic {

        public abstract Val.ref computeBox(Val.ref v1);
        public abstract Val.ref computeBox(Val.ref v1, Val.ref v2);
        public abstract Val.ref computeBox(Val.ref v1, Val.ref v2, Val.ref v3, Val.ref v4);
        public abstract Val.ref computeBox(Val.ref v1, Val.ref v2, Val.ref v3, Val.ref v4, Val.ref v5, Val.ref v6, Val.ref v7, Val.ref v8);

        public abstract Val compute(Val v1);
        public abstract Val compute(Val v1, Val v2);
        public abstract Val compute(Val v1, Val v2, Val v3, Val v4);
        public abstract Val compute(Val v1, Val v2, Val v3, Val v4, Val v5, Val v6, Val v7, Val v8);

    }

    static class InvokeImpl1 extends InvocationLogic {

        @Override
        public Val.ref computeBox(Val.ref v1) {
            return v1;
        }

        @Override
        public Val.ref computeBox(Val.ref v1, Val.ref v2) {
            return v1;
        }

        @Override
        public Val.ref computeBox(Val.ref v1, Val.ref v2, Val.ref v3, Val.ref v4) {
            return v1;
        }

        @Override
        public Val.ref computeBox(Val.ref v1, Val.ref v2, Val.ref v3, Val.ref v4, Val.ref v5, Val.ref v6, Val.ref v7, Val.ref v8) {
            return v1;
        }

        @Override
        public Val compute(Val v1) {
            return v1;
        }

        @Override
        public Val compute(Val v1, Val v2) {
            return v1;
        }

        @Override
        public Val compute(Val v1, Val v2, Val v3, Val v4) {
            return v1;
        }

        @Override
        public Val compute(Val v1, Val v2, Val v3, Val v4, Val v5, Val v6, Val v7, Val v8) {
            return v1;
        }

    }

    static class InvokeImpl2 extends InvocationLogic {

        @Override
        public Val.ref computeBox(Val.ref v1) {
            return v1;
        }

        @Override
        public Val.ref computeBox(Val.ref v1, Val.ref v2) {
            return v1;
        }

        @Override
        public Val.ref computeBox(Val.ref v1, Val.ref v2, Val.ref v3, Val.ref v4) {
            return v1;
        }

        @Override
        public Val.ref computeBox(Val.ref v1, Val.ref v2, Val.ref v3, Val.ref v4, Val.ref v5, Val.ref v6, Val.ref v7, Val.ref v8) {
            return v1;
        }

        @Override
        public Val compute(Val v1) {
            return v1;
        }

        @Override
        public Val compute(Val v1, Val v2) {
            return v1;
        }

        @Override
        public Val compute(Val v1, Val v2, Val v3, Val v4) {
            return v1;
        }

        @Override
        public Val compute(Val v1, Val v2, Val v3, Val v4, Val v5, Val v6, Val v7, Val v8) {
            return v1;
        }

    }

    static class InvokeImpl3 extends InvocationLogic {

        @Override
        public Val.ref computeBox(Val.ref v1) {
            return v1;
        }

        @Override
        public Val.ref computeBox(Val.ref v1, Val.ref v2) {
            return v1;
        }

        @Override
        public Val.ref computeBox(Val.ref v1, Val.ref v2, Val.ref v3, Val.ref v4) {
            return v1;
        }

        @Override
        public Val.ref computeBox(Val.ref v1, Val.ref v2, Val.ref v3, Val.ref v4, Val.ref v5, Val.ref v6, Val.ref v7, Val.ref v8) {
            return v1;
        }

        @Override
        public Val compute(Val v1) {
            return v1;
        }

        @Override
        public Val compute(Val v1, Val v2) {
            return v1;
        }

        @Override
        public Val compute(Val v1, Val v2, Val v3, Val v4) {
            return v1;
        }

        @Override
        public Val compute(Val v1, Val v2, Val v3, Val v4, Val v5, Val v6, Val v7, Val v8) {
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

    Val.ref b1 = new Val(42);
    Val.ref b2 = new Val(43);
    Val.ref b3 = new Val(44);
    Val.ref b4 = new Val(45);
    Val.ref b5 = new Val(46);
    Val.ref b6 = new Val(47);
    Val.ref b7 = new Val(48);
    Val.ref b8 = new Val(49);

    @CompilerControl(CompilerControl.Mode.INLINE)
    public int boxed1(InvocationLogic[] logic) {
        int r = 0;
        for(InvocationLogic t : logic) {
            r += t.computeBox(b1).f0;
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public int boxed2(InvocationLogic[] logic) {
        int r = 0;
        for(InvocationLogic t : logic) {
            r += t.computeBox(b1, b2).f0;
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public int boxed4(InvocationLogic[] logic) {
        int r = 0;
        for(InvocationLogic t : logic) {
            r += t.computeBox(b1, b2, b3, b4).f0;
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public int boxed8(InvocationLogic[] logic) {
        int r = 0;
        for(InvocationLogic t : logic) {
            r += t.computeBox(b1, b2, b3, b4, b5, b6, b7, b8).f0;
        }
        return r;
    }

    Val a1 = new Val(42);
    Val a2 = new Val(43);
    Val a3 = new Val(44);
    Val a4 = new Val(45);
    Val a5 = new Val(46);
    Val a6 = new Val(47);
    Val a7 = new Val(48);
    Val a8 = new Val(49);

    @CompilerControl(CompilerControl.Mode.INLINE)
    public int value1(InvocationLogic[] logic) {
        int r = 0;
        for(InvocationLogic t : logic) {
            r += t.compute(a1).f0;
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public int value2(InvocationLogic[] logic) {
        int r = 0;
        for(InvocationLogic t : logic) {
            r += t.compute(a1, a2).f0;
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public int value4(InvocationLogic[] logic) {
        int r = 0;
        for(InvocationLogic t : logic) {
            r += t.compute(a1, a2, a3, a4).f0;
        }
        return r;
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    public int value8(InvocationLogic[] logic) {
        int r = 0;
        for(InvocationLogic t : logic) {
            r += t.compute(a1, a2, a3, a4, a5, a6, a7, a8).f0;
        }
        return r;
    }

    /* targets 0 - means exact type */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int boxed_args1_targets0() {
        return boxed1(logic0);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int boxed_args1_targets1() {
        return boxed1(logic1);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int boxed_args1_targets2() {
        return boxed1(logic2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int boxed_args1_targets3() {
        return boxed1(logic3);
    }

    /* targets 0 - means exact type */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int boxed_args2_targets0() {
        return boxed2(logic0);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int boxed_args2_targets1() {
        return boxed2(logic1);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int boxed_args2_targets2() {
        return boxed2(logic2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int boxed_args2_targets3() {
        return boxed2(logic3);
    }

    /* targets 0 - means exact type */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int boxed_args4_targets0() {
        return boxed4(logic0);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int boxed_args4_targets1() {
        return boxed4(logic1);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int boxed_args4_targets2() {
        return boxed4(logic2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int boxed_args4_targets3() {
        return boxed4(logic3);
    }

    /* targets 0 - means exact type */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int boxed_args8_targets0() {
        return boxed8(logic0);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int boxed_args8_targets1() {
        return boxed8(logic1);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int boxed_args8_targets2() {
        return boxed8(logic2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int boxed_args8_targets3() {
        return boxed8(logic3);
    }

    /* targets 0 - means exact type */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int value_args1_targets0() {
        return value1(logic0);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int value_args1_targets1() {
        return value1(logic1);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int value_args1_targets2() {
        return value1(logic2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int value_args1_targets3() {
        return value1(logic3);
    }

    /* targets 0 - means exact type */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int value_args2_targets0() {
        return value2(logic0);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int value_args2_targets1() {
        return value2(logic1);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int value_args2_targets2() {
        return value2(logic2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int value_args2_targets3() {
        return value2(logic3);
    }

    /* targets 0 - means exact type */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int value_args4_targets0() {
        return value4(logic0);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int value_args4_targets1() {
        return value4(logic1);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int value_args4_targets2() {
        return value4(logic2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int value_args4_targets3() {
        return value4(logic3);
    }

    /* targets 0 - means exact type */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int value_args8_targets0() {
        return value8(logic0);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int value_args8_targets1() {
        return value8(logic1);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int value_args8_targets2() {
        return value8(logic2);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int value_args8_targets3() {
        return value8(logic3);
    }

}
