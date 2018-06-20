package oracle.micro.valhalla.lworld.ackermann;

import oracle.micro.valhalla.AckermannBase;
import oracle.micro.valhalla.lworld.types.Value1;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;

public class Ackermann1 extends AckermannBase {

    public static final Value1 V1_ONE = Value1.of(1);
    public static final Value1 V1_X1 = V1_ONE;
    public static final Value1 V1_Y1 = Value1.of(1748);
    public static final Value1 V1_X2 = Value1.of(2);
    public static final Value1 V1_Y2 = Value1.of(1897);
    public static final Value1 V1_X3 = Value1.of(3);
    public static final Value1 V1_Y3 = Value1.of(8);


    private static Value1 ack_value(Value1 x, Value1 y) {
        return x.totalsum() == 0 ?
                y.inc() :
                (y.totalsum() == 0 ?
                        ack_value(x.dec(), V1_ONE) :
                        ack_value(x.dec(), ack_value(x, y.dec())));
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int value() {
        return ack_value(V1_X1, V1_Y1).totalsum() + ack_value(V1_X2, V1_Y2).totalsum() + ack_value(V1_X3, V1_Y3).totalsum();
    }

}
