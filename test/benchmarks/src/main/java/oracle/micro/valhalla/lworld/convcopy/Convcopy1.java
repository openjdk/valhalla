package oracle.micro.valhalla.lworld.convcopy;

import oracle.micro.valhalla.ArraycopyBase;
import oracle.micro.valhalla.lworld.types.Value1;
import oracle.micro.valhalla.types.Total;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public class Convcopy1 extends ArraycopyBase {

    @State(Scope.Thread)
    public static class StateSrcValue {
        Value1[] src;

        @Setup
        public void setup() {
            src = new Value1[size];
            for (int i = 0; i < src.length; i++) {
                src[i] = Value1.of(i);
            }
        }
    }

    @State(Scope.Thread)
    public static class StateDstValue {
        Value1[] dst;

        @Setup
        public void setup() {
            dst = new Value1[size];
        }
    }

    @State(Scope.Thread)
    public static class StateSrcObject {
        Object[] src;

        @Setup
        public void setup() {
            src = new Object[size];
            for (int i = 0; i < src.length; i++) {
                src[i] = Value1.of(i);
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
            for (int i = 0; i < src.length; i++) {
                src[i] = Value1.of(i);
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
        Value1[] src = srcst.src;
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
        Value1[] dst = dstst.dst;
        for (int i = 0; i < size; i++) {
            dst[i] = (Value1)src[i];
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
        Value1[] src = srcst.src;
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
        Value1[] dst = dstst.dst;
        for (int i = 0; i < size; i++) {
            dst[i] = (Value1)src[i];
        }
        return dst;
    }

    @Benchmark
    public Object copyInterfaceToValue(StateSrcInterface srcst, StateDstValue dstst) {
        System.arraycopy(srcst.src, 0, dstst.dst, 0, size);
        return dstst.dst;
    }

}
