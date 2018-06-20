package oracle.micro.valhalla.lworld.convcopy;

import oracle.micro.valhalla.ArraycopyBase;
import oracle.micro.valhalla.lworld.types.Value8;
import oracle.micro.valhalla.types.Total;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public class Convcopy8 extends ArraycopyBase {

    @State(Scope.Thread)
    public static class StateSrcValue {
        Value8[] src;

        @Setup
        public void setup() {
            src = new Value8[size];
            for (int i = 0, k = 0; i < src.length; i++, k += 8) {
                src[i] = Value8.of(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
            }
        }
    }

    @State(Scope.Thread)
    public static class StateDstValue {
        Value8[] dst;

        @Setup
        public void setup() {
            dst = new Value8[size];
        }
    }

    @State(Scope.Thread)
    public static class StateSrcObject {
        Object[] src;

        @Setup
        public void setup() {
            src = new Object[size];
            for (int i = 0, k = 0; i < src.length; i++, k += 8) {
                src[i] = Value8.of(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
            }
        }
    }

    @State(Scope.Thread)
    public static class StateDstObject {
        Object[] dst;

        @Setup
        public void setup() {
            dst = new Object[size];
        }
    }

    @State(Scope.Thread)
    public static class StateSrcInterface {
        Total[] src;

        @Setup
        public void setup() {
            src = new Total[size];
            for (int i = 0, k = 0; i < src.length; i++, k += 8) {
                src[i] = Value8.of(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
            }
        }
    }

    @State(Scope.Thread)
    public static class StateDstInterface {
        Total[] dst;

        @Setup
        public void setup() {
            dst = new Total[size];
        }
    }

    @Benchmark
    public Object loopValueToObject(StateSrcValue srcst, StateDstObject dstst) {
        Value8[] src = srcst.src;
        Object[] dst = dstst.dst;
        for (int i = 0; i < size; i++) {
            dst[i] = src[i];
        }
        return dst;
    }

    @Benchmark
    public Object copyValueToObject(StateSrcValue srcst, StateDstObject dstst) {
        System.arraycopy(srcst.src, 0, dstst.dst, 0, size);
        return dstst.dst;
    }

    @Benchmark
    public Object loopObjectToValue(StateSrcObject srcst, StateDstValue dstst) {
        Object[] src = srcst.src;
        Value8[] dst = dstst.dst;
        for (int i = 0; i < size; i++) {
            dst[i] = (Value8)src[i];
        }
        return dst;
    }

    @Benchmark
    public Object copyObjectToValue(StateSrcObject srcst, StateDstValue dstst) {
        System.arraycopy(srcst.src, 0, dstst.dst, 0, size);
        return dstst.dst;
    }

    @Benchmark
    public Object loopValueToInterface(StateSrcValue srcst, StateDstInterface dstst) {
        Value8[] src = srcst.src;
        Total[] dst = dstst.dst;
        for (int i = 0; i < size; i++) {
            dst[i] = src[i];
        }
        return dst;
    }

    @Benchmark
    public Object copyValueToInterface(StateSrcValue srcst, StateDstInterface dstst) {
        System.arraycopy(srcst.src, 0, dstst.dst, 0, size);
        return dstst.dst;
    }

    @Benchmark
    public Object loopInterfaceToValue(StateSrcInterface srcst, StateDstValue dstst) {
        Total[] src = srcst.src;
        Value8[] dst = dstst.dst;
        for (int i = 0; i < size; i++) {
            dst[i] = (Value8)src[i];
        }
        return dst;
    }

    @Benchmark
    public Object copyInterfaceToValue(StateSrcInterface srcst, StateDstValue dstst) {
        System.arraycopy(srcst.src, 0, dstst.dst, 0, size);
        return dstst.dst;
    }

}
