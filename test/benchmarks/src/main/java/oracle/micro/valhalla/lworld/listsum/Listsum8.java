package oracle.micro.valhalla.lworld.listsum;

import oracle.micro.valhalla.ListsumBase;
import oracle.micro.valhalla.lworld.types.Value8;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

public class Listsum8 extends ListsumBase {

    static class Node {
        public __Flattenable Value8 value;
        public Node next;


        public Node(Value8 value, Node next) {
            this.value = value;
            this.next = next;
        }
    }

    Node list = null;

    @Setup
    public void setup() {
        for (int i = 0, k = 0; i < size; i++, k += 8) {
            list = new Node(Value8.of(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7), list);
        }
    }

    public static int sumScalarized(Node list) {
        int f0 = 0;
        int f1 = 0;
        int f2 = 0;
        int f3 = 0;
        int f4 = 0;
        int f5 = 0;
        int f6 = 0;
        int f7 = 0;
        for (Node n  = list; n!=null; n = n.next) {
            f0 += n.value.f0;
            f1 += n.value.f1;
            f2 += n.value.f2;
            f3 += n.value.f3;
            f4 += n.value.f4;
            f5 += n.value.f5;
            f6 += n.value.f6;
            f7 += n.value.f7;
        }
        return f0 + f1 + f2 + f3 + f4 + f5 + f6 + f7;
    }

    public static int sum(Node list) {
        Value8 sum = Value8.of(0,0,0,0,0,0,0,0);
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
