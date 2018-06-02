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
package oracle.micro.valhalla.baseline.convcopy;

import oracle.micro.valhalla.ArraycopyBase;
import oracle.micro.valhalla.baseline.types.Box2;
import oracle.micro.valhalla.types.Total;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public class Convcopy2 extends ArraycopyBase {

    @State(Scope.Thread)
    public static class StateSrcBoxed {
        Box2[] src;

        @Setup
        public void setup() {
            src = new Box2[size];
            for (int i = 0, k = 0; i < src.length; i++, k += 2) {
                src[i] = new Box2(k, k + 1);
            }
        }
    }

    @State(Scope.Thread)
    public static class StateDstBoxed {
        Box2[] dst;

        @Setup
        public void setup() {
            dst = new Box2[size];
        }
    }

    @State(Scope.Thread)
    public static class StateSrcObject {
        Object[] src;

        @Setup
        public void setup() {
            src = new Object[size];
            for (int i = 0, k = 0; i < src.length; i++, k += 2) {
                src[i] = new Box2(k, k + 1);
            }
        }
    }

    @State(Scope.Thread)
    public static class StateDstObject {
        Object[] dst;

        @Setup
        public void setup() {
            dst = new Object[size];
        }
    }

    @State(Scope.Thread)
    public static class StateSrcInterface {
        Total[] src;

        @Setup
        public void setup() {
            src = new Total[size];
            for (int i = 0, k = 0; i < src.length; i++, k += 2) {
                src[i] = new Box2(k, k + 1);
            }
        }
    }

    @State(Scope.Thread)
    public static class StateDstInterface {
        Total[] dst;

        @Setup
        public void setup() {
            dst = new Total[size];
        }
    }

    @Benchmark
    public Object loopBoxedToObject(StateSrcBoxed srcst, StateDstObject dstst) {
        Box2[] src = srcst.src;
        Object[] dst = dstst.dst;
        for (int i = 0; i < size; i++) {
            dst[i] = src[i];
        }
        return dst;
    }

    @Benchmark
    public Object copyBoxedToObject(StateSrcBoxed srcst, StateDstObject dstst) {
        System.arraycopy(srcst.src, 0, dstst.dst, 0, size);
        return dstst.dst;
    }

    @Benchmark
    public Object loopObjectToBoxed(StateSrcObject srcst, StateDstBoxed dstst) {
        Object[] src = srcst.src;
        Box2[] dst = dstst.dst;
        for (int i = 0; i < size; i++) {
            dst[i] = (Box2)src[i];
        }
        return dst;
    }

    @Benchmark
    public Object copyObjectToBoxed(StateSrcObject srcst, StateDstBoxed dstst) {
        System.arraycopy(srcst.src, 0, dstst.dst, 0, size);
        return dstst.dst;
    }

    @Benchmark
    public Object loopBoxedToInterface(StateSrcBoxed srcst, StateDstInterface dstst) {
        Box2[] src = srcst.src;
        Total[] dst = dstst.dst;
        for (int i = 0; i < size; i++) {
            dst[i] = src[i];
        }
        return dst;
    }

    @Benchmark
    public Object copyBoxedToInterface(StateSrcBoxed srcst, StateDstInterface dstst) {
        System.arraycopy(srcst.src, 0, dstst.dst, 0, size);
        return dstst.dst;
    }

    @Benchmark
    public Object loopInterfaceToBoxed(StateSrcInterface srcst, StateDstBoxed dstst) {
        Total[] src = srcst.src;
        Box2[] dst = dstst.dst;
        for (int i = 0; i < size; i++) {
            dst[i] = (Box2)src[i];
        }
        return dst;
    }

    @Benchmark
    public Object copyInterfaceToBoxed(StateSrcInterface srcst, StateDstBoxed dstst) {
        System.arraycopy(srcst.src, 0, dstst.dst, 0, size);
        return dstst.dst;
    }
    
}
