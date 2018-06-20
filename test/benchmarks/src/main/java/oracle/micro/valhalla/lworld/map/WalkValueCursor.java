package oracle.micro.valhalla.lworld.map;

import oracle.micro.valhalla.MapBase;
import oracle.micro.valhalla.lworld.util.HashMapValueCursor;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Setup;

import java.util.Iterator;

public class WalkValueCursor extends MapBase {

    HashMapValueCursor<Integer, Integer> map;

    @Setup
    public void setup() {
        super.init(size);
        map = new HashMapValueCursor<>();
        for (Integer k : keys) {
            map.put(k, k);
        }
    }


    @Benchmark
    public int sumIterator() {
        int s = 0;
        for (Iterator<Integer> iterator = map.keyIterator(); iterator.hasNext(); ) {
            s += iterator.next();
        }
        return s;
    }

    @Benchmark
    public int sumIteratorHidden() {
        int s = 0;
        for (Iterator<Integer> iterator = hide(map.keyIterator()); iterator.hasNext(); ) {
            s += iterator.next();
        }
        return s;
    }

    @Benchmark
    public int sumCursor() {
        int s = 0;
        for (oracle.micro.valhalla.util.Cursor<Integer> cursor = map.keyCursor(); cursor.hasElement(); cursor = cursor.next()) {
            s += cursor.get();
        }
        return s;
    }

    @Benchmark
    public int sumCursorSpecialized() {
        int s = 0;
        for (HashMapValueCursor.KeyCursor<Integer, Integer> cursor = HashMapValueCursor.KeyCursor.of(map); cursor.hasElement(); cursor = cursor.nextEntry()) {
            s += cursor.get();
        }
        return s;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static Iterator<Integer> hide(Iterator<Integer> it) {
        return it;
    }

}
