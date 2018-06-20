package oracle.micro.valhalla.lworld.arraycopy;

import oracle.micro.valhalla.ArraycopyBase;
import oracle.micro.valhalla.lworld.types.Value8;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public class Arraycopy8 extends ArraycopyBase {


    @State(Scope.Thread)
    public static class StateValue {
        Value8[] src;
        Value8[] dst;

        @Setup
        public void setup() {
            src = new Value8[size];
            for (int i = 0, k = 0; i < src.length; i++, k += 8) {
                src[i] = Value8.of(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
            }
            dst = new Value8[size];
        }
    }

    @Benchmark
    public Object loopValue(StateValue st) {
        Value8[] src = st.src;
        Value8[] dst = st.dst;
        for (int i = 0; i < size; i++) {
            dst[i] = src[i];
        }
        return dst;
    }

    @Benchmark
    public Object copyValue(StateValue st) {
        Value8[] dst = st.dst;
        System.arraycopy(st.src, 0, dst, 0, size);
        return dst;
    }

}
