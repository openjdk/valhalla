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
package oracle.micro.valhalla.baseline.arraycopy;

import oracle.micro.valhalla.ArraycopyBase;
import oracle.micro.valhalla.baseline.types.Box1;
import org.openjdk.jmh.annotations.*;

public class Arraycopy1 extends ArraycopyBase {

    @State(Scope.Thread)
    public static class StatePrimitive {
        int[] src;
        int[] dst;

        @Setup
        public void setup() {
            src = new int[size];
            for (int i = 0; i < src.length; i++) {
                src[i] = i;
            }
            dst = new int[size];
        }
    }

    @Benchmark
    public Object loopPrimitive(StatePrimitive st) {
        int[] src = st.src;
        int[] dst = st.dst;
        for (int i = 0; i < size; i++) {
            dst[i] = src[i];
        }
        return dst;
    }

    @Benchmark
    public Object copyPrimitive(StatePrimitive st) {
        int[] src = st.src;
        int[] dst = st.dst;
        System.arraycopy(src, 0, dst, 0, size);
        return dst;
    }

    @State(Scope.Thread)
    public static class StateBoxed {
        Box1[] src;
        Box1[] dst;

        @Setup
        public void setup() {
            src = new Box1[size];
            for (int i = 0; i < src.length; i++) {
                src[i] = new Box1(i);
            }
            dst = new Box1[size];
        }
    }

    @Benchmark
    public Object loopBoxed(StateBoxed st) {
        Box1[] src = st.src;
        Box1[] dst = st.dst;
        for (int i = 0; i < size; i++) {
            dst[i] = src[i];
        }
        return dst;
    }

    @Benchmark
    public Object copyBoxed(StateBoxed st) {
        Box1[] src = st.src;
        Box1[] dst = st.dst;
        System.arraycopy(src, 0, dst, 0, size);
        return dst;
    }

}
