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
package org.openjdk.bench.valhalla.acmp.array;

import org.openjdk.bench.valhalla.types.Int64;
import org.openjdk.bench.valhalla.types.Q64long;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.BitSet;
import java.util.Random;

public class StatesQ64long {

    public static final int SIZE = 100;

    private static BitSet indices(Random rnd, int bound, int size) {
        return rnd.ints(0, bound)
                .distinct()
                .limit(size)
                .collect(BitSet::new, BitSet::set, BitSet::or);
    }

    private static void populate(Object[] arr1, Object[] arr2, int eq) {
        BitSet eqset = (eq > 0 && eq < 100) ? indices(new Random(42), SIZE, (eq * SIZE) / 100) : null;
        for (int i = 0; i < SIZE; i++) {
            if (eq > 0 && (eq >= 100 || eqset.get(i))) {
                arr2[i] = arr1[i] = new Q64long(i);
            } else {
                arr1[i] = new Q64long(2 * i);
                arr2[i] = new Q64long(2 * i + 1);
            }
        }
    }

    @State(Scope.Thread)
    public abstract static class ObjState {
        Object[] arr1, arr2;
    }

    @State(Scope.Thread)
    public abstract static class IntState {
        Int64[] arr1, arr2;
    }

    @State(Scope.Thread)
    public abstract static class RefState {
        Q64long.ref[] arr1, arr2;
    }

    @State(Scope.Thread)
    public abstract static class ValState {
        Q64long[] arr1, arr2;
    }

    public static class ObjState00 extends ObjState {
        @Setup
        public void setup() {
            arr1 = new Object[SIZE];
            arr2 = new Object[SIZE];
            populate(arr1, arr2, 0);
        }

    }

    public static class ObjState25 extends ObjState {
        @Setup
        public void setup() {
            arr1 = new Object[SIZE];
            arr2 = new Object[SIZE];
            populate(arr1, arr2, 25);
        }

    }

    public static class ObjState50 extends ObjState {
        @Setup
        public void setup() {
            arr1 = new Object[SIZE];
            arr2 = new Object[SIZE];
            populate(arr1, arr2, 50);
        }

    }

    public static class ObjState75 extends ObjState {
        @Setup
        public void setup() {
            arr1 = new Object[SIZE];
            arr2 = new Object[SIZE];
            populate(arr1, arr2, 75);
        }

    }

    public static class ObjState100 extends ObjState {
        @Setup
        public void setup() {
            arr1 = new Object[SIZE];
            arr2 = new Object[SIZE];
            populate(arr1, arr2, 100);
        }
    }

    public static class IntState00 extends IntState {
        @Setup
        public void setup() {
            arr1 = new Int64[SIZE];
            arr2 = new Int64[SIZE];
            populate(arr1, arr2, 0);
        }
    }

    public static class IntState25 extends IntState {
        @Setup
        public void setup() {
            arr1 = new Int64[SIZE];
            arr2 = new Int64[SIZE];
            populate(arr1, arr2, 25);
        }
    }

    public static class IntState50 extends IntState {
        @Setup
        public void setup() {
            arr1 = new Int64[SIZE];
            arr2 = new Int64[SIZE];
            populate(arr1, arr2, 50);
        }
    }

    public static class IntState75 extends IntState {
        @Setup
        public void setup() {
            arr1 = new Int64[SIZE];
            arr2 = new Int64[SIZE];
            populate(arr1, arr2, 75);
        }
    }

    public static class IntState100 extends IntState {
        @Setup
        public void setup() {
            arr1 = new Int64[SIZE];
            arr2 = new Int64[SIZE];
            populate(arr1, arr2, 100);
        }
    }

    public static class RefState00 extends RefState {
        @Setup
        public void setup() {
            arr1 = new Q64long.ref[SIZE];
            arr2 = new Q64long.ref[SIZE];
            populate(arr1, arr2, 0);
        }
    }

    public static class RefState25 extends RefState {
        @Setup
        public void setup() {
            arr1 = new Q64long.ref[SIZE];
            arr2 = new Q64long.ref[SIZE];
            populate(arr1, arr2, 25);
        }
    }

    public static class RefState50 extends RefState {
        @Setup
        public void setup() {
            arr1 = new Q64long.ref[SIZE];
            arr2 = new Q64long.ref[SIZE];
            populate(arr1, arr2, 50);
        }
    }

    public static class RefState75 extends RefState {
        @Setup
        public void setup() {
            arr1 = new Q64long.ref[SIZE];
            arr2 = new Q64long.ref[SIZE];
            populate(arr1, arr2, 75);
        }
    }

    public static class RefState100 extends RefState {
        @Setup
        public void setup() {
            arr1 = new Q64long.ref[SIZE];
            arr2 = new Q64long.ref[SIZE];
            populate(arr1, arr2, 100);
        }
    }

    public static class ValState00 extends ValState {
        @Setup
        public void setup() {
            arr1 = new Q64long[SIZE];
            arr2 = new Q64long[SIZE];
            populate(arr1, arr2, 0);
        }
    }

    public static class ValState25 extends ValState {
        @Setup
        public void setup() {
            arr1 = new Q64long[SIZE];
            arr2 = new Q64long[SIZE];
            populate(arr1, arr2, 25);
        }
    }

    public static class ValState50 extends ValState {
        @Setup
        public void setup() {
            arr1 = new Q64long[SIZE];
            arr2 = new Q64long[SIZE];
            populate(arr1, arr2, 50);
        }
    }

    public static class ValState75 extends ValState {
        @Setup
        public void setup() {
            arr1 = new Q64long[SIZE];
            arr2 = new Q64long[SIZE];
            populate(arr1, arr2, 75);
        }
    }

    public static class ValState100 extends ValState {
        @Setup
        public void setup() {
            arr1 = new Q64long[SIZE];
            arr2 = new Q64long[SIZE];
            populate(arr1, arr2, 100);
        }
    }

}
