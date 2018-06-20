package oracle.micro.valhalla.lworld.ackermann;

import oracle.micro.valhalla.AckermannBase;
import oracle.micro.valhalla.types.PNumber;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;

public class AckermannMix extends AckermannBase {

    private static PNumber ack_interface(PNumber one, PNumber x, PNumber y) {
        return x.totalsum() == 0 ?
                y.inc() :
                (y.totalsum() == 0 ?
                        ack_interface(one, x.dec(), one) :
                        ack_interface(one, x.dec(), ack_interface(one , x, y.dec())));
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int interface1() {
        return ack_interface(Ackermann1.V1_ONE, Ackermann1.V1_X1, Ackermann1.V1_Y1).totalsum() +
               ack_interface(Ackermann1.V1_ONE, Ackermann1.V1_X2, Ackermann1.V1_Y2).totalsum() +
               ack_interface(Ackermann1.V1_ONE, Ackermann1.V1_X3, Ackermann1.V1_Y3).totalsum();
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int interface2() {
        return ack_interface(Ackermann2.V2_ONE, Ackermann2.V2_X1, Ackermann2.V2_Y1).totalsum() +
               ack_interface(Ackermann2.V2_ONE, Ackermann2.V2_X2, Ackermann2.V2_Y2).totalsum() +
               ack_interface(Ackermann2.V2_ONE, Ackermann2.V2_X3, Ackermann2.V2_Y3).totalsum();
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int interface8() {
        return ack_interface(Ackermann8.V8_ONE, Ackermann8.V8_X1, Ackermann8.V8_Y1).totalsum() +
               ack_interface(Ackermann8.V8_ONE, Ackermann8.V8_X2, Ackermann8.V8_Y2).totalsum() +
               ack_interface(Ackermann8.V8_ONE, Ackermann8.V8_X3, Ackermann8.V8_Y3).totalsum();
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int interfaceMixDepth() {
        return ack_interface(Ackermann1.V1_ONE, Ackermann1.V1_X1, Ackermann1.V1_Y1).totalsum() +
               ack_interface(Ackermann2.V2_ONE, Ackermann2.V2_X2, Ackermann2.V2_Y2).totalsum() +
               ack_interface(Ackermann8.V8_ONE, Ackermann8.V8_X3, Ackermann8.V8_Y3).totalsum();
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int interfaceMixWidth() {
        return ack_interface(Ackermann1.V1_ONE, Ackermann2.V2_X1, Ackermann8.V8_Y1).totalsum() +
               ack_interface(Ackermann2.V2_ONE, Ackermann8.V8_X2, Ackermann1.V1_Y2).totalsum() +
               ack_interface(Ackermann8.V8_ONE, Ackermann1.V1_X3, Ackermann2.V2_Y3).totalsum();
    }


}
