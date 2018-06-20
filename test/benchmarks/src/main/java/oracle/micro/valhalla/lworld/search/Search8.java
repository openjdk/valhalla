package oracle.micro.valhalla.lworld.search;

import oracle.micro.valhalla.SearchBase;
import oracle.micro.valhalla.lworld.types.Value8;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

public class Search8 extends SearchBase {

    @State(Scope.Thread)
    public static class StateValue {
        Value8[] arr;

        @Setup
        public void setup() {
            baseSetup();
            arr = new Value8[size];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = Value8.of(i, i, i, i, i, i, i, i);
            }
        }
    }

    private static int binarySearch(Value8[] a,  int key) {
        int low = 0;
        int high = a.length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = a[mid].f0;

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }


    @Benchmark
    @OperationsPerInvocation(OPS)
    public void value(StateValue st, Blackhole bh) {
        for (int t : targets) {
            bh.consume(binarySearch(st.arr, t));
        }
    }


}
