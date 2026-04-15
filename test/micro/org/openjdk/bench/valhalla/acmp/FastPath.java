/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.valhalla.acmp;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

@Fork(value = 3, jvmArgsAppend = {"--enable-preview"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
public class FastPath {
    public static final int SIZE = 100;

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static int acmp(Object[] objects1, Object[] objects2) {
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            if (objects1[i] == objects2[i]) {
                s += 1;
            } else {
                s -= 1;
            }
        }
        return s;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int numeric_classes(ObjStateNumeric st) {
        return acmp(st.arr1, st.arr2);
    }
    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int custom_single_field(ObjStateSingleField st) {
        return acmp(st.arr1, st.arr2);
    }
    @Benchmark
    @OperationsPerInvocation(SIZE)
    @CompilerControl(CompilerControl.Mode.INLINE)
    public int custom_multi_field(ObjStateMultiField st) {
        return acmp(st.arr1, st.arr2);
    }


    @State(Scope.Thread)
    public static class ObjStateNumeric {
        Object[] arr1, arr2;

        @Setup
        public void setup() {
            arr1 = new Object[SIZE];
            arr2 = new Object[SIZE];

            for (int i = 0; i < SIZE; i++) {
                switch (i % 6) {
                    case 0:
                        arr1[i] = new Integer(i);
                        arr2[i] = new Integer(i);
                        break;
                    case 1:
                        arr1[i] = new Integer(i);
                        arr2[i] = new Integer(-i);
                        break;
                    case 2:
                        arr1[i] = new Integer(i);
                        arr2[i] = new Short((short)i);
                        break;
                    case 3:
                        arr1[i] = new Short((short)i);
                        arr2[i] = new Integer(i);
                        break;
                    case 4:
                        arr1[i] = new Short((short)i);
                        arr2[i] = new Short((short)i);
                        break;
                    case 5:
                        arr1[i] = new Short((short)i);
                        arr2[i] = new Short((short)-i);
                        break;
                }
            }
        }
    }

    static value class CustomShort {
        short s;
        CustomShort(int s) {
            this.s = (short)s;
        }
    }
    static value class CustomChar {
        char c;
        CustomChar(int c) {
            this.c = (char)c;
        }
    }
    @State(Scope.Thread)
    public static class ObjStateSingleField {
        Object[] arr1, arr2;

        @Setup
        public void setup() {
            arr1 = new Object[SIZE];
            arr2 = new Object[SIZE];

            for (int i = 0; i < SIZE; i++) {
                switch (i % 6) {
                    case 0:
                        arr1[i] = new CustomShort(i);
                        arr2[i] = new CustomShort(i);
                        break;
                    case 1:
                        arr1[i] = new CustomShort(i);
                        arr2[i] = new CustomShort(-i);
                        break;
                    case 2:
                        arr1[i] = new CustomShort(i);
                        arr2[i] = new CustomChar(i);
                        break;
                    case 3:
                        arr1[i] = new CustomChar(i);
                        arr2[i] = new CustomShort(i);
                        break;
                    case 4:
                        arr1[i] = new CustomChar(i);
                        arr2[i] = new CustomChar(i);
                        break;
                    case 5:
                        arr1[i] = new CustomChar(i);
                        arr2[i] = new CustomChar(-i);
                        break;
                }
            }
        }
    }

    static value class ShortByte {
        short s;
        byte b;
        ShortByte(int s, int b) {
            this.s = (short)s;
            this.b = (byte)b;
        }
    }
    static value class ByteByte {
        byte b1;
        byte b2;
        ByteByte(int b1, int b2) {
            this.b1 = (byte)b1;
            this.b2 = (byte)b2;
        }
    }
    @State(Scope.Thread)
    public static class ObjStateMultiField {
        Object[] arr1, arr2;

        @Setup
        public void setup() {
            arr1 = new Object[SIZE];
            arr2 = new Object[SIZE];

            for (int i = 0; i < SIZE; i++) {
                switch (i % 6) {
                    case 0:
                        arr1[i] = new ShortByte(i, i);
                        arr2[i] = new ShortByte(i, i);
                        break;
                    case 1:
                        arr1[i] = new ShortByte(i, i);
                        arr2[i] = new ShortByte(i, 0);
                        break;
                    case 2:
                        arr1[i] = new ShortByte(i, 0);
                        arr2[i] = new ByteByte(i, 0);
                        break;
                    case 3:
                        arr1[i] = new ByteByte(i, i);
                        arr2[i] = new ShortByte(i, 0);
                        break;
                    case 4:
                        arr1[i] = new ByteByte(i, i);
                        arr2[i] = new ByteByte(i, i);
                        break;
                    case 5:
                        arr1[i] = new ByteByte(i, i);
                        arr2[i] = new ByteByte(i, 0);
                        break;
                }
            }
        }
    }
}