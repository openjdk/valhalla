package oracle.micro.valhalla.lworld.arraycopy;

import oracle.micro.valhalla.ArraycopyBase;
import oracle.micro.valhalla.lworld.types.Value2;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public class Arraycopy2 extends ArraycopyBase {


    @State(Scope.Thread)
    public static class StateValue {
        Value2[] src;
        Value2[] dst;

        @Setup
        public void setup() {
            src = new Value2[size];
            for (int i = 0, k = 0; i < src.length; i++, k += 2) {
                src[i] = Value2.of(k, k + 1);
            }
            dst = new Value2[size];
        }
    }


    @Benchmark
    public Object loopValue(StateValue st) {
        Value2[] src = st.src;
        Value2[] dst = st.dst;
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
