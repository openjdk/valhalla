package oracle.micro.valhalla.lworld.listsum;

import oracle.micro.valhalla.ListsumBase;
import oracle.micro.valhalla.lworld.types.Value2;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

public class Listsum2 extends ListsumBase {

    static class Node {
        public __Flattenable Value2 value;
        public Node next;


        public Node(Value2 value, Node next) {
            this.value = value;
            this.next = next;
        }
    }

    Node list = null;

    @Setup
    public void setup() {
        for (int i = 0, k = 0; i < size; i++, k += 2) {
            list = new Node( Value2.of(k, k+1), list);

        }
    }

    public static int sumScalarized(Node list) {
        int f0 = 0;
        int f1 = 0;
        for (Node n  = list; n!=null; n = n.next) {
            f0  += n.value.f0;
            f1  += n.value.f1;
        }
        return f0 + f1;
    }

    public static int sum(Node list) {
        Value2 sum = Value2.of(0,0);
        for (Node n  = list; n!=null; n = n.next) {
            sum = sum.add(n.value);
        }
        return sum.totalsum();
    }

    @Benchmark
    public int valueScalarized() {
        return sumScalarized(list);
    }

    @Benchmark
    public int value() {
        return sum(list);
    }

}
