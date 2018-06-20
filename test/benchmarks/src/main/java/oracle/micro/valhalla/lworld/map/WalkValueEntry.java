package oracle.micro.valhalla.lworld.map;

import oracle.micro.valhalla.MapBase;
import oracle.micro.valhalla.lworld.util.HashMapValueEntry;
import oracle.micro.valhalla.util.Cursor;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Setup;

import java.util.Iterator;

public class WalkValueEntry extends MapBase {

    HashMapValueEntry<Integer, Integer> map;

    @Setup
    public void setup() {
        super.init(size);
        map = new HashMapValueEntry<>();
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
        for (Cursor<Integer> cursor = map.keyCursor(); cursor.hasElement(); cursor = cursor.next()) {
            s += cursor.get();
        }
        return s;
    }

    @Benchmark
    public int sumCursorSpecialized() {
        int s = 0;
        for (HashMapValueEntry.KeyCursor<Integer, Integer> cursor = HashMapValueEntry.KeyCursor.of(map); cursor.hasElement(); cursor = cursor.nextEntry()) {
            s += cursor.get();
        }
        return s;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static Iterator<Integer> hide(Iterator<Integer> it) {
        return it;
    }


}
