package oracle.micro.valhalla.lworld.arraycopy;

import oracle.micro.valhalla.ArraycopyBase;
import oracle.micro.valhalla.lworld.types.Value1;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public class Arraycopy1 extends ArraycopyBase {


    @State(Scope.Thread)
    public static class StateValue {
        Value1[] src;
        Value1[] dst;

        @Setup
        public void setup() {
            src = new Value1[size];
            for (int i = 0; i < src.length; i++) {
                src[i] = Value1.of(i);
            }
            dst = new Value1[size];
        }
    }

    @Benchmark
    public Object loopValue(StateValue st) {
        Value1[] src = st.src;
        Value1[] dst = st.dst;
        for (int i = 0; i < size; i++) {
            dst[i] = src[i];
        }
        return dst;
    }

    @Benchmark
    public Object copyValue(StateValue st) {
        System.arraycopy(st.src, 0, st.dst, 0, size);
        return st.dst;
    }

}
