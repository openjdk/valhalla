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
package org.openjdk.bench.valhalla.array.util;

import org.openjdk.bench.valhalla.util.SizeBase;
import org.openjdk.jmh.annotations.Setup;

public class PrimitiveStates extends SizeBase {

    public static abstract class ByteState extends SizeState {
        public byte[] arr;
        void fill() {
            for (int i = 0; i < arr.length; i++) {
                arr[i] = (byte) i;
            }
        }
    }

    public static abstract class IntState extends SizeState {
        public int[] arr;
        void fill() {
            for (int i = 0; i < arr.length; i++) {
                arr[i] = i;
            }
        }
    }

    public static abstract class LongState extends SizeState {
        public long[] arr;
        void fill() {
            for (int i = 0; i < arr.length; i++) {
                arr[i] = i;
            }
        }
    }

    public static class Primitive32int extends IntState {
        @Setup
        public void setup() {
            arr = new int[size];
            fill();
        }
    }

    public static class Primitive64byte extends ByteState {
        @Setup
        public void setup() {
            arr = new byte[size * 8];
            fill();
        }
    }

    public static class Primitive64int extends IntState {
        @Setup
        public void setup() {
            arr = new int[size * 2];
            fill();
        }
    }

    public static class Primitive64long extends LongState {
        @Setup
        public void setup() {
            arr = new long[size];
            fill();
        }
    }

    public static class Primitive128int extends IntState {
        @Setup
        public void setup() {
            arr = new int[size * 4];
            fill();
        }
    }


}
