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
package oracle.micro.valhalla.lword.map;

import oracle.micro.valhalla.MapBase;
import oracle.micro.valhalla.lword.util.HashMapIteratorCursor;
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
