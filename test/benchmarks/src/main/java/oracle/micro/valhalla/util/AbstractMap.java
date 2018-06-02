/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package oracle.micro.valhalla.util;

import java.util.Iterator;
import java.util.Map;

public abstract class AbstractMap<K, V> {

    protected static final int DEFAULT_INITIAL_CAPACITY = 16;
    protected static final int MAXIMUM_CAPACITY = 1 << 30;
    protected static final float LOAD_FACTOR = 0.75f;

    protected int size;
    protected int threshold;
    protected int modCount;

    protected AbstractMap() {
    }

    protected static final int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    protected static int hash(Object key) {
        int h = key.hashCode();
        return (h ) ^ (h >>> 16);
    }

    /**
     * Returns index for hash code h.
     */
    protected static int indexFor(int h, int length) {
        return h & (length-1);
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return size;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns <tt>size() == 0</tt>.
     */
    public boolean isEmpty() {
        return size == 0;
    }

//############################################################

    public abstract V get(K key);
    public abstract V put(K key, V value);
    public abstract V remove(K key);

    // TODO contains?

    public abstract Iterator<K> keyIterator();
    public abstract Iterator<V> valueIterator();
    public abstract Iterator<Map.Entry<K,V>> entryIterator();

    public abstract Cursor<Map.Entry<K,V>> entryCursor();
    public abstract Cursor<K> keyCursor();

}
