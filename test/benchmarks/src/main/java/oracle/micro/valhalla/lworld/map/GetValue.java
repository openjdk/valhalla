package oracle.micro.valhalla.lworld.map;

import oracle.micro.valhalla.MapBase;
import oracle.micro.valhalla.lworld.util.HashMapValueTotal;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.Collections;

public class GetValue extends MapBase {

    HashMapValueTotal<Integer, Integer> map;

    Integer[] mixed;

    @Setup
    public void setup() {
        super.init(size);
        map = new HashMapValueTotal<>();
        for (Integer k : keys) {
            map.put(k, k);
        }
        mixed = new Integer[size];
        System.arraycopy(keys, 0, mixed, 0, size / 2);
        System.arraycopy(nonKeys, 0, mixed, size / 2, size / 2);
        Collections.shuffle(Arrays.asList(mixed), rnd);
    }

    @Benchmark
    public void getHit(Blackhole bh) {
        Integer[] keys = this.keys;
        HashMapValueTotal<Integer, Integer> map = this.map;
        for (Integer k : keys) {
            bh.consume(map.get(k));
        }
    }

    @Benchmark
    public void getMix(Blackhole bh) {
        Integer[] keys = this.mixed;
        HashMapValueTotal<Integer, Integer> map = this.map;
        for (Integer k : keys) {
            bh.consume(map.get(k));
        }
    }


}
