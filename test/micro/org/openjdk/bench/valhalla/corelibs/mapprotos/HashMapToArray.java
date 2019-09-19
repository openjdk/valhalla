/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.valhalla.corelibs.mapprotos;


import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark                               (mapType)  (size)  Mode  Cnt         Score         Error  Units
 * XHashMapToArray.testKeySetToArray        XHashMap       0  avgt   10         9.996 ±       2.010  ns/op
 * XHashMapToArray.testKeySetToArray        XHashMap       1  avgt   10      4142.267 ±     180.789  ns/op
 * XHashMapToArray.testKeySetToArray        XHashMap      10  avgt   10      4240.651 ±     202.434  ns/op
 * XHashMapToArray.testKeySetToArray        XHashMap    1000  avgt   10    494789.609 ±   13140.044  ns/op
 * XHashMapToArray.testKeySetToArray        XHashMap  100000  avgt   10  60665517.560 ± 2386064.520  ns/op
 * XHashMapToArray.testKeySetToArray         HashMap       0  avgt   10        10.547 ±       1.317  ns/op
 * XHashMapToArray.testKeySetToArray         HashMap       1  avgt   10        30.381 ±       2.341  ns/op
 * XHashMapToArray.testKeySetToArray         HashMap      10  avgt   10        71.436 ±       1.356  ns/op
 * XHashMapToArray.testKeySetToArray         HashMap    1000  avgt   10      6184.091 ±     391.274  ns/op
 * XHashMapToArray.testKeySetToArray         HashMap  100000  avgt   10    635572.324 ±   12811.539  ns/op
 * XHashMapToArray.testKeySetToArrayTyped   XHashMap       0  avgt   10         7.350 ±       1.093  ns/op
 * XHashMapToArray.testKeySetToArrayTyped   XHashMap       1  avgt   10      3949.242 ±     162.359  ns/op
 * XHashMapToArray.testKeySetToArrayTyped   XHashMap      10  avgt   10      4078.345 ±     194.142  ns/op
 * XHashMapToArray.testKeySetToArrayTyped   XHashMap    1000  avgt   10    527815.326 ±   27872.781  ns/op
 * XHashMapToArray.testKeySetToArrayTyped   XHashMap  100000  avgt   10  63033220.728 ± 3078028.203  ns/op
 * XHashMapToArray.testKeySetToArrayTyped    HashMap       0  avgt   10         7.607 ±       0.863  ns/op
 * XHashMapToArray.testKeySetToArrayTyped    HashMap       1  avgt   10        34.312 ±       0.831  ns/op
 * XHashMapToArray.testKeySetToArrayTyped    HashMap      10  avgt   10       105.760 ±       0.670  ns/op
 * XHashMapToArray.testKeySetToArrayTyped    HashMap    1000  avgt   10      9994.524 ±     156.952  ns/op
 * XHashMapToArray.testKeySetToArrayTyped    HashMap  100000  avgt   10    991598.536 ±   12037.671  ns/op
 * XHashMapToArray.testValuesToArray        XHashMap       0  avgt   10        10.078 ±       1.625  ns/op
 * XHashMapToArray.testValuesToArray        XHashMap       1  avgt   10      3798.391 ±     192.066  ns/op
 * XHashMapToArray.testValuesToArray        XHashMap      10  avgt   10      4035.747 ±     181.076  ns/op
 * XHashMapToArray.testValuesToArray        XHashMap    1000  avgt   10    500692.191 ±   17923.078  ns/op
 * XHashMapToArray.testValuesToArray        XHashMap  100000  avgt   10  64450148.368 ± 5112519.509  ns/op
 * XHashMapToArray.testValuesToArray         HashMap       0  avgt   10        10.510 ±       1.691  ns/op
 * XHashMapToArray.testValuesToArray         HashMap       1  avgt   10        40.833 ±       4.335  ns/op
 * XHashMapToArray.testValuesToArray         HashMap      10  avgt   10        70.101 ±       2.298  ns/op
 * XHashMapToArray.testValuesToArray         HashMap    1000  avgt   10      6154.291 ±     173.222  ns/op
 * XHashMapToArray.testValuesToArray         HashMap  100000  avgt   10    647159.888 ±   28626.401  ns/op
 * XHashMapToArray.testValuesToArrayTyped   XHashMap       0  avgt   10         7.440 ±       1.522  ns/op
 * XHashMapToArray.testValuesToArrayTyped   XHashMap       1  avgt   10      3912.895 ±     229.372  ns/op
 * XHashMapToArray.testValuesToArrayTyped   XHashMap      10  avgt   10      4221.022 ±     248.348  ns/op
 * XHashMapToArray.testValuesToArrayTyped   XHashMap    1000  avgt   10    508209.209 ±   20811.752  ns/op
 * XHashMapToArray.testValuesToArrayTyped   XHashMap  100000  avgt   10  63079414.458 ± 3164118.537  ns/op
 * XHashMapToArray.testValuesToArrayTyped    HashMap       0  avgt   10         7.948 ±       1.340  ns/op
 * XHashMapToArray.testValuesToArrayTyped    HashMap       1  avgt   10        35.295 ±       2.964  ns/op
 * XHashMapToArray.testValuesToArrayTyped    HashMap      10  avgt   10       102.239 ±       1.685  ns/op
 * XHashMapToArray.testValuesToArrayTyped    HashMap    1000  avgt   10      9913.778 ±     577.131  ns/op
 * XHashMapToArray.testValuesToArrayTyped    HashMap  100000  avgt   10   1178696.903 ±   39326.922  ns/op
 * Finished running test 'micro:valhalla.corelibs.XHashMapToArray'
 */

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class HashMapToArray {

    private IntFunction<Map<Integer, Integer>> mapSupplier;
    Map<Integer, Integer> map;


    @Param(value = {"org.openjdk.bench.valhalla.corelibs.mapprotos.YHashMap",
            "org.openjdk.bench.valhalla.corelibs.mapprotos.XHashMap",
            "java.util.HashMap"})
    private String mapType;

    @Param({"1", "10", "1000", "100000"})
    public int size;

    @Setup
    public void setup() {
        try {
            Class<?> mapClass = Class.forName(mapType);
            mapSupplier =  (size) -> newInstance(mapClass, size);
        } catch (Exception ex) {
            System.out.printf("%s: %s%n", mapType, ex.getMessage());
            return;
        }

        map = mapSupplier.apply(0);
        for (int i = 0; i < size; i++) {
            map.put(i, i * i);
        }
    }

    Map<Integer, Integer> newInstance(Class<?> mapClass, int size) {
        try {
            return (Map<Integer, Integer>)mapClass.getConstructor(int.class).newInstance(size);
        } catch (Exception ex) {
            throw new RuntimeException("failed", ex);
        }
    }

    @Benchmark
    public Object[] testKeySetToArray() {
        return map.keySet().toArray();
    }

    @Benchmark
    public Object[] testKeySetToArrayTyped() {
        return map.keySet().toArray(new Integer[0]);
    }

    @Benchmark
    public Object[] testValuesToArray() {
        return map.values().toArray();
    }

    @Benchmark
    public Object[] testValuesToArrayTyped() {
        return map.values().toArray(new Integer[0]);
    }
}
