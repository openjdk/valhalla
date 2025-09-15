/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.valhalla.field.util;

import org.openjdk.bench.valhalla.util.SizeBase;
import org.openjdk.jmh.annotations.Setup;

public class PrimitiveStates extends SizeBase {

    public static class P64byte {

        public byte f0;
        public byte f1;
        public byte f2;
        public byte f3;
        public byte f4;
        public byte f5;
        public byte f6;
        public byte f7;

        public P64byte(long v) {
            this((byte) (v >>> 56), (byte) (v >>> 48), (byte) (v >>> 40), (byte) (v >>> 32), (byte) (v >>> 24), (byte) (v >>> 16), (byte) (v >>> 8), (byte) (v));
        }

        public P64byte(byte v0, byte v1, byte v2, byte v3, byte v4, byte v5, byte v6, byte v7) {
            this.f0 = v0;
            this.f1 = v1;
            this.f2 = v2;
            this.f3 = v3;
            this.f4 = v4;
            this.f5 = v5;
            this.f6 = v6;
            this.f7 = v7;
        }

    }

    public static class Primitive64byte extends SizeState {
        public P64byte[] arr;
        @Setup
        public void setup() {
            arr = new P64byte[size];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new P64byte(i);
            }
        }
    }


    public static class P64int {

        public int f0;
        public int f1;

        public P64int(long v) {
            this((int) (v >>> 32), (int) v);
        }

        public P64int(int hi, int lo) {
            this.f0 = hi;
            this.f1 = lo;
        }

    }

    public static class Primitive64int extends SizeState {
        public P64int[] arr;
        @Setup
        public void setup() {
            arr = new P64int[size];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new P64int(i);
            }
        }
    }

    public static class P64long {

        public long f0;

        public P64long(long v0) {
            this.f0 = v0;
        }

    }

    public static class Primitive64long extends SizeState {
        public P64long[] arr;
        @Setup
        public void setup() {
            arr = new P64long[size];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new P64long(i);
            }
        }
    }


    public static class P32int {

        public int f0;

        public P32int(int val) {
            this.f0 = val;
        }
    }

    public static class Primitive32int extends SizeState {
        public P32int[] arr;
        @Setup
        public void setup() {
            arr = new P32int[size];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new P32int(i);
            }
        }
    }

    public static class P128int {

        public int f0;
        public int f1;
        public int f2;
        public int f3;

        public P128int(long v) {
            this(0, 0, (int) (v >>> 32), (int) v);
        }

        public P128int(int v0, int v1, int v2, int v3) {
            this.f0 = v0;
            this.f1 = v1;
            this.f2 = v2;
            this.f3 = v3;
        }
    }

    public static class Primitive128int extends SizeState {
        public P128int[] arr;
        @Setup
        public void setup() {
            arr = new P128int[size];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new P128int(i);
            }
        }
    }

}

