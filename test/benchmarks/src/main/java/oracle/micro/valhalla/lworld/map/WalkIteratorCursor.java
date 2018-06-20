package oracle.micro.valhalla.lworld.map;

import oracle.micro.valhalla.MapBase;
import oracle.micro.valhalla.lworld.util.HashMapIteratorCursor;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Setup;

import java.util.Iterator;

public class WalkIteratorCursor extends MapBase {

    HashMapIteratorCursor<Integer, Integer> map;

    @Setup
    public void setup() {
        super.init(size);
        map = new HashMapIteratorCursor<>();
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
        for (HashMapIteratorCursor.KeyCursor<Integer, Integer> cursor = HashMapIteratorCursor.KeyCursor.of(map); cursor.hasElement(); cursor = cursor.nextEntry()) {
            s += cursor.get();
        }
        return s;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static Iterator<Integer> hide(Iterator<Integer> it) {
        return it;
    }

}
