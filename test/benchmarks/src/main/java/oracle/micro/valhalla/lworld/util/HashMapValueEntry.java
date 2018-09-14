package oracle.micro.valhalla.lworld.util;

import oracle.micro.valhalla.util.AbstractMap;
import oracle.micro.valhalla.util.Cursor;

import java.util.*;

/*
 * HashMap where Entry is value type
 */
public class HashMapValueEntry<K, V> extends AbstractMap<K, V> {

    Entry<K, V>[] table;

    public HashMapValueEntry(int initialCapacity) {
        super();
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                    initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;

        // Find a power of 2 >= initialCapacity
        int capacity = tableSizeFor(initialCapacity);
        threshold = (int) (capacity * LOAD_FACTOR);
        @SuppressWarnings({"rawtypes", "unchecked"})
        Entry<K, V>[] newTab = (Entry<K, V>[]) new Entry[capacity];
        table = newTab;
    }

    public HashMapValueEntry() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    __ByValue public static final class Entry<K, V> implements Map.Entry<K, V> {
        final K key;
        final V value;
        final HEntry<K, V> next;
        final int hash;

        public static <K,V> Entry<K,V> empty() {
            Entry e = Entry.default;
            return e;
        }

        public static <K,V> Entry<K,V> of(int h, K k, V v, HEntry<K, V> n) {
            Entry e = Entry.default;
            e = __WithField(e.key, k);
            e = __WithField(e.value, v);
            e = __WithField(e.next, n);
            e = __WithField(e.hash, h);
            return e;
        }

        Entry() {
            key = null;
            value = null;
            next = null;
            hash = 0;
        }

        boolean isEmpty() {
            return key == null;
        }

        public final K getKey() {
            return key;
        }

        public final V getValue() {
            return value;
        }

        public final V setValue(V newValue) {
            throw new UnsupportedOperationException();
        }

        public final boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry) o;
            return Objects.equals(key, e.getKey()) && Objects.equals(value, e.getValue());
        }

        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public final String toString() {
            return key + "=" + value;
        }

    }

    public static final class HEntry<K, V> implements Map.Entry<K, V> {

        __Flattenable Entry <K, V> entry;

        public HEntry(Entry<K, V> e) {
            this.entry = e;
        }

        public static <K,V> HEntry<K,V> of(Entry<K, V> e) {
            return new HEntry<>(e);
        }

        @Override
        public K getKey() {
            return entry.key;
        }

        @Override
        public V getValue() {
            return entry.value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        public final boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry) o;
            return Objects.equals(entry.key, e.getKey()) && Objects.equals(entry.value, e.getValue());
        }

        public final int hashCode() {
            return Objects.hashCode(entry.key) ^ Objects.hashCode(entry.value);
        }

        public final String toString() {
            return entry.key + "=" + entry.value;
        }


    }

    //############################################
    @Override
    public V get(K key) {
        Entry<K, V>[] table = this.table;
        int hash = hash(key);
        int idx = indexFor(hash, table.length);
        K k = table[idx].key;
        if (k != null) {
            if (table[idx].hash == hash && Objects.equals(key, k)) {
                return table[idx].value;
            }
            for (HEntry<K, V> e = table[idx].next;
                 e != null;
                 e = e.entry.next) {
                if (e.entry.hash == hash && Objects.equals(key, e.entry.key))
                    return e.entry.value;
            }
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        int hash = hash(key);
        Entry<K, V>[] table = this.table;
        int idx = indexFor(hash, table.length);
        K k = table[idx].key;
        if (k != null) {
            if (table[idx].hash == hash && Objects.equals(key, k)) {
                V oldValue = table[idx].value;
                table[idx] = Entry.of(hash, k, value, table[idx].next);
                return oldValue;
            }
            for (HEntry<K, V> e = table[idx].next;
                 e != null;
                 e = e.entry.next) {
                if (e.entry.hash == hash && Objects.equals(key, e.entry.key)) {
                    V oldValue = e.entry.value;
                    e.entry = Entry.of(hash, e.entry.key, value, e.entry.next);
                    return oldValue;
                }
            }
            // not found, but there are entries in this bucket.
            table[idx] = Entry.of(hash, key, value, new HEntry<>(table[idx]));
        } else {
            table[idx] = Entry.of(hash, key, value, null);
        }

        modCount++;
        if (size++ >= threshold)
            resize(2 * table.length);
        return null;
    }

    void resize(int newCapacity) {
        Entry<K,V>[] oldTable = table;
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }
        @SuppressWarnings({"rawtypes","unchecked"})
        Entry<K,V>[] newTable = (Entry<K,V>[])new Entry[newCapacity];
        transfer(newTable);
        table = newTable;
        threshold = (int)(newCapacity * LOAD_FACTOR);
    }

    /**
     * Transfers all entries from current table to newTable.
     */
    void transfer(Entry<K,V>[] newTable) {
        Entry<K,V>[] src = table;
        int newCapacity = newTable.length;
        for (int j = 0; j < src.length; j++) {
            if (src[j].key != null) {
                HEntry<K,V> next = src[j].next;
                int i = indexFor(src[j].hash, newCapacity);
                if(newTable[i].key == null) {
                    if(next == null) {
                        newTable[i] = src[j];
                    } else {
                        newTable[i] = Entry.of(src[j].hash, src[j].key, src[j].value, null);
                    }
                } else {
                    newTable[i] = Entry.of(src[j].hash, src[j].key, src[j].value, new HEntry<>(newTable[i]));
                }
                while(next != null) {
                    HEntry<K,V> e = next;
                    next = e.entry.next;
                    int ehash = e.entry.hash;
                    K ekey = e.entry.key;
                    V evalue = e.entry.value;
                    i = indexFor(ehash, newCapacity);
                    if(newTable[i].key == null) {
                        if(next==null) {
                            newTable[i] = e.entry;
                        } else {
                            newTable[i] = Entry.of(ehash, ekey, evalue, null);
                        }
                    } else {
                        e.entry = newTable[i];
                        newTable[i] = Entry.of(ehash, ekey, evalue, e);
                    }
                }
            }
        }
    }

    @Override
    public V remove(K key) {

        Entry<K, V>[] table = this.table;
        int hash = hash(key);
        int idx = indexFor(hash, table.length);
        K k = table[idx].key;
        if (k != null) {
            if (table[idx].hash == hash && Objects.equals(key, k)) {
                V oldValue = table[idx].value;
                modCount++;
                size--;
                if(table[idx].next == null) {
                    table[idx] = Entry.empty();
                } else {
                    table[idx] = table[idx].next.entry;
                }
                return oldValue;
            }
            HEntry<K, V> prev = null;
            for (HEntry<K, V> e = table[idx].next;
                 e != null;
                 e = e.entry.next) {
                if (e.entry.hash == hash && Objects.equals(key, e.entry.key)) {
                    V oldValue = e.entry.value;
                    modCount++;
                    size--;
                    if(prev == null) {
                        table[idx] = Entry.of(table[idx].hash, table[idx].key, table[idx].value, e.entry.next);
                    } else {
                        prev.entry = Entry.of(prev.entry.hash, prev.entry.key, prev.entry.value, e.entry.next);
                    }
                    return oldValue;
                }
                prev = e;
            }
        }
        return null;
    }

    private static int getFilledSlot(Entry[] t, int from) {
        while (from < t.length && t[from].key == null) {
            from++;
        }
        return from;
    }

    private class HashIterator  {
        HEntry<K,V> next;        // next entry to return
        final int expectedModCount;   // For fast-fail
        int index;              // current slot

        HashIterator() {
            expectedModCount = modCount;
            if (size > 0) { // advance to first entry
                Entry<K,V>[] t = table;
                index = getFilledSlot(t, 0);
                next = null;
            } else {
                index = table.length;
            }
        }

        public boolean hasNext() {
            return index < table.length; // unsafe (e.g. add then hasNext
        }

        public Map.Entry<K,V> nextEntry() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (!hasNext())
                throw new NoSuchElementException();
            Entry<K,V>[] t = table;
            if(next == null) {
                int oldIdx = index;
                next = table[oldIdx].next;
                if(next == null) {
                    index = getFilledSlot(t, index + 1);
                }
                return table[oldIdx];
            } else {
                HEntry<K,V> e = next;
                next = e.entry.next;
                if(next == null) {
                    index = getFilledSlot(t, index + 1);
                }
                return e;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    private class EntryIterator extends HashIterator implements Iterator<Map.Entry<K, V>> {

        public Map.Entry<K, V> next() {
            return nextEntry();
        }
    }

    private class KeyIterator extends HashIterator implements Iterator<K> {

        public K next() {
            return nextEntry().getKey();
        }
    }

    private class ValueIterator extends HashIterator implements Iterator<V> {

        public V next() {
            return nextEntry().getValue();
        }
    }

    @Override
    public Iterator<K> keyIterator() {
        return new KeyIterator();
    }

    @Override
    public Iterator<V> valueIterator() {
        return new ValueIterator();
    }

    @Override
    public Iterator<Map.Entry<K, V>> entryIterator() {
        return new EntryIterator();
    }

    @Override
    public Cursor<Map.Entry<K, V>> entryCursor() {
        return EntryCursor.of(this);
    }

    @Override
    public Cursor<K> keyCursor() {
        return KeyCursor.of(this);
    }

    public static class EntryCursor<K,V> implements Cursor<Map.Entry<K,V>> {
        private final HashMapValueEntry<K,V> map;
        private final int expectedModCount;
        private final int index;
        private final HEntry<K,V> current;

        public static <K,V> EntryCursor<K,V> of(HashMapValueEntry<K,V> map) {
            if (map.size > 0) { // advance to first entry
                Entry<K, V>[] t = map.table;
                int from = getFilledSlot(t, 0);
                return new EntryCursor<K, V>(map, map.modCount, from, null);
            } else {
                return new EntryCursor<K, V>(map, map.modCount, map.table.length, null);
            }
        }

        EntryCursor(HashMapValueEntry<K, V> map, int expectedModCount, int index, HEntry<K, V> current) {
            this.map = map;
            this.expectedModCount = expectedModCount;
            this.index = index;
            this.current = current;
        }

        public boolean hasElement() {
            return index < map.table.length;
        }

        public Map.Entry<K, V> get() {
            return current != null ? current : map.table[index];
        }

        public Cursor<Map.Entry<K, V>> next() {
            return nextEntry();
        }

        public EntryCursor<K, V> nextEntry() {
            if(!hasElement())
                throw new NoSuchElementException();
            if (map.modCount != expectedModCount)
                throw new ConcurrentModificationException();
            Entry<K, V>[] t = map.table;
            HEntry<K,V> next = current == null ? t[index].next : current.entry.next;
            if (next != null) {
                return new EntryCursor<K, V>(map, expectedModCount, index, next);
            } else {
                return new EntryCursor<K, V>(map, expectedModCount, getFilledSlot(t, index + 1), null);
            }
        }
    }

    public static class KeyCursor<K,V> implements Cursor<K> {
        private final HashMapValueEntry<K,V> map;
        private final int expectedModCount;
        private final int index;
        private final HEntry<K,V> current;

        public static <K,V> KeyCursor<K,V> of(HashMapValueEntry<K,V> map) {
            if (map.size > 0) { // advance to first entry
                Entry<K, V>[] t = map.table;
                int from = getFilledSlot(t, 0);
                return new KeyCursor<K, V>(map, map.modCount, from, null);
            } else {
                return new KeyCursor<K, V>(map, map.modCount, map.table.length, null);
            }
        }

        KeyCursor(HashMapValueEntry<K, V> map, int expectedModCount, int index, HEntry<K, V> current) {
            this.map = map;
            this.expectedModCount = expectedModCount;
            this.index = index;
            this.current = current;
        }

        public boolean hasElement() {
            return index < map.table.length;
        }

        public K get() {
            return current != null ? current.entry.key : map.table[index].key;
        }

        public Cursor<K> next() {
            return nextEntry();
        }

        public KeyCursor<K, V> nextEntry() {
            if(!hasElement())
                throw new NoSuchElementException();
            if (map.modCount != expectedModCount)
                throw new ConcurrentModificationException();
            Entry<K, V>[] t = map.table;
            HEntry<K,V> next = current == null ? t[index].next : current.entry.next;
            if (next != null) {
                return new KeyCursor<K, V>(map, expectedModCount, index, next);
            } else {
                return new KeyCursor<K, V>(map, expectedModCount, getFilledSlot(t, index + 1), null);
            }
        }
    }

}
