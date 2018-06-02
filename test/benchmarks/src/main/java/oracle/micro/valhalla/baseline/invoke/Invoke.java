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
package oracle.micro.valhalla.baseline.invoke;


import oracle.micro.valhalla.InvokeBase;
import oracle.micro.valhalla.baseline.types.Box1;
import oracle.micro.valhalla.baseline.types.Box2;
import oracle.micro.valhalla.baseline.types.Box8;
import oracle.micro.valhalla.types.Total;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.ThreadLocalRandom;

public class Invoke extends InvokeBase {

    @State(Scope.Thread)
    public static class StateBoxed {
        Box1[] src;

        @Setup
        public void setup() {
            src = new Box1[SIZE];
            for (int i = 0; i < src.length; i++) {
                src[i] = new Box1(i);
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object invokeExactType(StateBoxed st) {
        Box1[] src = st.src;
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            s += src[i].f0();
        }
        return s;
    }

    @State(Scope.Thread)
    public static class StateObjectTarget1 {
        Object[] src;

        @Setup
        public void setup() {
            src = new Object[SIZE];
            for (int i = 0; i < src.length; i++) {
                src[i] = new Box1(i);
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object invokeObject1(StateObjectTarget1 st) {
        Object[] src = st.src;
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            s += src[i].hashCode();
        }
        return s;
    }


    @State(Scope.Thread)
    public static class StateObjectTarget2 {
        Object[] src;

        @Setup
        public void setup() {
            src = new Object[SIZE];
            Integer[] d = random2();
            for (int i = 0; i < src.length; i++) {
                switch (d[i]) {
                    case 0:
                        src[i] = new Box1(i);
                        break;
                    case 1:
                        src[i] = new Box2(i, i + 1);
                        break;
                }

            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object invokeObject2(StateObjectTarget2 st) {
        Object[] src = st.src;
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            s += src[i].hashCode();
        }
        return s;
    }

    @State(Scope.Thread)
    public static class StateObjectTarget3 {
        Object[] src;

        @Setup
        public void setup() {
            src = new Object[SIZE];
            Integer[] d = random3();
            for (int i = 0; i < src.length; i++) {
                switch (d[i]) {
                    case 0:
                        src[i] = new Box1(i);
                        break;
                    case 1:
                        src[i] = new Box2(i, i + 1);
                        break;
                    case 2:
                        src[i] = new Box8(i, i + 1, i + 2, i + 3, i + 4, i + 5, i + 6, i + 7);
                        break;
                }

            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object invokeObject3(StateObjectTarget3 st) {
        Object[] src = st.src;
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            s += src[i].hashCode();
        }
        return s;
    }

    @State(Scope.Thread)
    public static class StateInterfaceTarget1 {
        Total[] src;

        @Setup
        public void setup() {
            src = new Total[SIZE];
            for (int i = 0; i < src.length; i++) {
                src[i] = new Box1(i);
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object invokeInterface1(StateInterfaceTarget1 st) {
        Total[] src = st.src;
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            s += src[i].totalsum();
        }
        return s;
    }


    @State(Scope.Thread)
    public static class StateInterfaceTarget2 {
        Total[] src;

        @Setup
        public void setup() {
            src = new Total[SIZE];
            Integer[] d = random2();
            for (int i = 0; i < src.length; i++) {
                switch (d[i]) {
                    case 0:
                        src[i] = new Box1(i);
                        break;
                    case 1:
                        src[i] = new Box2(i, i + 1);
                        break;
                }

            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object invokeInterface2(StateInterfaceTarget2 st) {
        Total[] src = st.src;
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            s += src[i].totalsum();
        }
        return s;
    }

    @State(Scope.Thread)
    public static class StateInterfaceTarget3 {
        Total[] src;

        @Setup
        public void setup() {
            src = new Total[SIZE];
            for (int i = 0; i < src.length; i++) {
                Integer[] d = random3();
                switch (d[i]) {
                    case 0:
                        src[i] = new Box1(i);
                        break;
                    case 1:
                        src[i] = new Box2(i, i + 1);
                        break;
                    case 2:
                        src[i] = new Box8(i, i + 1, i + 2, i + 3, i + 4, i + 5, i + 6, i + 7);
                        break;
                }

            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object invokeInterface3(StateInterfaceTarget3 st) {
        Total[] src = st.src;
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            s += src[i].totalsum();
        }
        return s;
    }


}
