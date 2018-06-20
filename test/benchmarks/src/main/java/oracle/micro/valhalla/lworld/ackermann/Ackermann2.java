package oracle.micro.valhalla.lworld.ackermann;

import oracle.micro.valhalla.AckermannBase;
import oracle.micro.valhalla.lworld.types.Value2;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;

public class Ackermann2 extends AckermannBase {

    public static final Value2 V2_ONE = Value2.of(0, 1);
    public static final Value2 V2_X1 = V2_ONE;
    public static final Value2 V2_Y1 = Value2.of(0, 1748);
    public static final Value2 V2_X2 = Value2.of(0, 2);
    public static final Value2 V2_Y2 = Value2.of(0, 1897);
    public static final Value2 V2_X3 = Value2.of(0, 3);
    public static final Value2 V2_Y3 = Value2.of(0, 8);


    private static Value2 ack_value(Value2 x, Value2 y) {
        return x.totalsum() == 0 ?
                y.inc() :
                (y.totalsum() == 0 ?
                        ack_value(x.dec(), V2_ONE) :
                        ack_value(x.dec(), ack_value(x, y.dec())));
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int value() {
        return ack_value(V2_X1, V2_Y1).totalsum() + ack_value(V2_X2, V2_Y2).totalsum() + ack_value(V2_X3, V2_Y3).totalsum();
    }

}
