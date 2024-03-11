/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.bench.valhalla.intrinsics;

import org.openjdk.jmh.annotations.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.TimeUnit;
import jdk.internal.misc.Unsafe;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 1,
      jvmArgsAppend = {"--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
                       "-XX:+EnableValhalla", "-XX:+EnablePrimitiveClasses"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class IsFlattenedArray {

    private static final Unsafe U = Unsafe.getUnsafe();
    private static final VarHandle objectArrayVarHandle =
        MethodHandles.arrayElementVarHandle(Object[].class);

    @State(Scope.Benchmark)
    public static class ClassState {
        public Class flattenedArrayClass = Point[].class;
        public Class nonFlattenedArrayClass = String[].class;

        public Object[] objectArray = new Object[10];
        public Object objectElement = new Object();
        public int arrayIndex = 0;
    }

    @Benchmark
    public boolean testKnownFlattenedClass() {
        return U.isFlattenedArray(Point[].class);
    }

    @Benchmark
    public boolean testKnownNonFlattenedClass() {
        return U.isFlattenedArray(String[].class);
    }

    @Benchmark
    public boolean testUnknownFlattenedClass(ClassState state) {
        return U.isFlattenedArray(state.flattenedArrayClass);
    }

    @Benchmark
    public boolean testUnknownNonFlattenedClass(ClassState state) {
        return U.isFlattenedArray(state.nonFlattenedArrayClass);
    }

    @Benchmark
    public void setArrayElement(ClassState state) {
        objectArrayVarHandle.set(state.objectArray, state.arrayIndex, state.objectElement);
    }

    @Benchmark
    public VarHandle makeArrayVarHandle() {
        return MethodHandles.arrayElementVarHandle(Object[].class);
    }

}

value class Point {
    int x;
    int y;
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
