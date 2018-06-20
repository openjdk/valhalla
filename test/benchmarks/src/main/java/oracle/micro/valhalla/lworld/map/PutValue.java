package oracle.micro.valhalla.lworld.map;

import oracle.micro.valhalla.MapBase;
import oracle.micro.valhalla.lworld.util.HashMapValueTotal;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;

public class PutValue extends MapBase {

    @Setup
    public void setup() {
        super.init(size);
    }

    @Benchmark
    public HashMapValueTotal<Integer, Integer> put() {
        Integer[] keys = this.keys;
        HashMapValueTotal<Integer, Integer> map = new HashMapValueTotal<>();
        for (Integer k : keys) {
            map.put(k, k);
        }
        return map;
    }

    @Benchmark
    public HashMapValueTotal<Integer, Integer> putSized() {
        Integer[] keys = this.keys;
        HashMapValueTotal<Integer, Integer> map = new HashMapValueTotal<>(size*2);
        for (Integer k : keys) {
            map.put(k, k);
        }
        return map;
    }

}
