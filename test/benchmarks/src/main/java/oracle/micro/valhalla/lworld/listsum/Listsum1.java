package oracle.micro.valhalla.lworld.listsum;

import oracle.micro.valhalla.ListsumBase;
import oracle.micro.valhalla.lworld.types.Value1;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

public class Listsum1 extends ListsumBase {

    static class Node {
        public __Flattenable Value1 value;
        public Node next;


        public Node(Value1 value, Node next) {
            this.value = value;
            this.next = next;
        }
    }

    Node list = null;

    @Setup
    public void setup() {
        for (int i = 0; i < size; i++) {
            list = new Node(Value1.of(i), list);
        }
    }

    public static int sumScalarized(Node list) {
        int sum = 0;
        for (Node n  = list; n!=null; n = n.next) {
            sum += n.value.f0;
        }
        return sum;
    }

    public static int sum(Node list) {
        Value1 sum = Value1.of(0);
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
