package oracle.micro.valhalla.lworld.invoke;


import oracle.micro.valhalla.InvokeBase;
import oracle.micro.valhalla.lworld.types.Value1;
import oracle.micro.valhalla.lworld.types.Value2;
import oracle.micro.valhalla.lworld.types.Value8;
import oracle.micro.valhalla.types.Total;
import org.openjdk.jmh.annotations.*;

public class Invoke extends InvokeBase {

    @State(Scope.Thread)
    public static class StateValue {
        Value1[] src;

        @Setup
        public void setup() {
            src = new Value1[SIZE];
            for (int i = 0; i < src.length; i++) {
                src[i] = Value1.of(i);
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object invokeExactType(StateValue st) {
        Value1[] src = st.src;
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            s += src[i].f0();
        }
        return s;
    }

    @State(Scope.Thread)
    public static class StateObjectTarget1 {
        Object[] src;

        @Setup
        public void setup() {
            src = new Object[SIZE];
            for (int i = 0; i < src.length; i++) {
                src[i] = Value1.of(i);
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object invokeObject1(StateObjectTarget1 st) {
        Object[] src = st.src;
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            s += src[i].hashCode();
        }
        return s;
    }


    @State(Scope.Thread)
    public static class StateObjectTarget2 {
        Object[] src;

        @Setup
        public void setup() {
            src = new Object[SIZE];
            Integer[] d = random2();
            for (int i = 0; i < src.length; i++) {
                switch (d[i]) {
                    case 0:
                        src[i] = Value1.of(i);
                        break;
                    case 1:
                        src[i] = Value2.of(i, i + 1);
                        break;
                }

            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object invokeObject2(StateObjectTarget2 st) {
        Object[] src = st.src;
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            s += src[i].hashCode();
        }
        return s;
    }

    @State(Scope.Thread)
    public static class StateObjectTarget3 {
        Object[] src;

        @Setup
        public void setup() {
            src = new Object[SIZE];
            Integer[] d = random3();
            for (int i = 0; i < src.length; i++) {
                switch (d[i]) {
                    case 0:
                        src[i] = Value1.of(i);
                        break;
                    case 1:
                        src[i] = Value2.of(i, i + 1);
                        break;
                    case 2:
                        src[i] = Value8.of(i, i + 1, i + 2, i + 3, i + 4, i + 5, i + 6, i + 7);
                        break;
                }

            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object invokeObject3(StateObjectTarget3 st) {
        Object[] src = st.src;
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            s += src[i].hashCode();
        }
        return s;
    }

    @State(Scope.Thread)
    public static class StateInterfaceTarget1 {
        Total[] src;

        @Setup
        public void setup() {
            src = new Total[SIZE];
            for (int i = 0; i < src.length; i++) {
                src[i] = Value1.of(i);
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object invokeInterface1(StateInterfaceTarget1 st) {
        Total[] src = st.src;
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            s += src[i].totalsum();
        }
        return s;
    }


    @State(Scope.Thread)
    public static class StateInterfaceTarget2 {
        Total[] src;

        @Setup
        public void setup() {
            src = new Total[SIZE];
            Integer[] d = random2();
            for (int i = 0; i < src.length; i++) {
                switch (d[i]) {
                    case 0:
                        src[i] = Value1.of(i);
                        break;
                    case 1:
                        src[i] = Value2.of(i, i + 1);
                        break;
                }

            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object invokeInterface2(StateInterfaceTarget2 st) {
        Total[] src = st.src;
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            s += src[i].totalsum();
        }
        return s;
    }

    @State(Scope.Thread)
    public static class StateInterfaceTarget3 {
        Total[] src;

        @Setup
        public void setup() {
            src = new Total[SIZE];
            Integer[] d = random3();
            for (int i = 0; i < src.length; i++) {
                switch (d[i]) {
                    case 0:
                        src[i] = Value1.of(i);
                        break;
                    case 1:
                        src[i] = Value2.of(i, i + 1);
                        break;
                    case 2:
                        src[i] = Value8.of(i, i + 1, i + 2, i + 3, i + 4, i + 5, i + 6, i + 7);
                        break;
                }

            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object invokeInterface3(StateInterfaceTarget3 st) {
        Total[] src = st.src;
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            s += src[i].totalsum();
        }
        return s;
    }


}
