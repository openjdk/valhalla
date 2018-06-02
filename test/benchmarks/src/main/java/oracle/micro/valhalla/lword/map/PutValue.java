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
import oracle.micro.valhalla.lword.util.HashMapValueTotal;
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
