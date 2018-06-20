package oracle.micro.valhalla.lworld.ackermann;

import oracle.micro.valhalla.AckermannBase;
import oracle.micro.valhalla.lworld.types.Value8;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;

public class Ackermann8 extends AckermannBase {

    public static final Value8 V8_ONE = Value8.of(0, 0, 0, 0, 0, 0, 0, 1);
    public static final Value8 V8_X1 = V8_ONE;
    public static final Value8 V8_Y1 = Value8.of(0, 0, 0, 0, 0, 0, 0, 1748);
    public static final Value8 V8_X2 = Value8.of(0, 0, 0, 0, 0, 0, 0, 2);
    public static final Value8 V8_Y2 = Value8.of(0, 0, 0, 0, 0, 0, 0, 1897);
    public static final Value8 V8_X3 = Value8.of(0, 0, 0, 0, 0, 0, 0, 3);
    public static final Value8 V8_Y3 = Value8.of(0, 0, 0, 0, 0, 0, 0, 8);


    private static Value8 ack_value(Value8 x, Value8 y) {
        return x.totalsum() == 0 ?
                y.inc() :
                (y.totalsum() == 0 ?
                        ack_value(x.dec(), V8_ONE) :
                        ack_value(x.dec(), ack_value(x, y.dec())));
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int value() {
        return ack_value(V8_X1, V8_Y1).totalsum() + ack_value(V8_X2, V8_Y2).totalsum() + ack_value(V8_X3, V8_Y3).totalsum();
    }

}
