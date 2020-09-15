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
package org.openjdk.bench.valhalla.acmp.field;

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

    private static void populate(ObjWrapper[] arr1, ObjWrapper[] arr2, int eq) {
        BitSet eqset = (eq > 0 && eq < 100) ? indices(new Random(42), SIZE, (eq * SIZE) / 100) : null;
        for (int i = 0; i < SIZE; i++) {
            if (eq > 0 && (eq >= 100 || eqset.get(i))) {
                arr2[i] = arr1[i] = new ObjWrapper(new Q64long(i));
            } else {
                arr1[i] = new ObjWrapper(new Q64long(2 * i));
                arr2[i] = new ObjWrapper(new Q64long(2 * i + 1));
            }
        }
    }

    private static void populate(IntWrapper[] arr1, IntWrapper[] arr2, int eq) {
        BitSet eqset = (eq > 0 && eq < 100) ? indices(new Random(42), SIZE, (eq * SIZE) / 100) : null;
        for (int i = 0; i < SIZE; i++) {
            if (eq > 0 && (eq >= 100 || eqset.get(i))) {
                arr2[i] = arr1[i] = new IntWrapper(new Q64long(i));
            } else {
                arr1[i] = new IntWrapper(new Q64long(2 * i));
                arr2[i] = new IntWrapper(new Q64long(2 * i + 1));
            }
        }
    }

    private static void populate(RefWrapper[] arr1, RefWrapper[] arr2, int eq) {
        BitSet eqset = (eq > 0 && eq < 100) ? indices(new Random(42), SIZE, (eq * SIZE) / 100) : null;
        for (int i = 0; i < SIZE; i++) {
            if (eq > 0 && (eq >= 100 || eqset.get(i))) {
                arr2[i] = arr1[i] = new RefWrapper(new Q64long(i));
            } else {
                arr1[i] = new RefWrapper(new Q64long(2 * i));
                arr2[i] = new RefWrapper(new Q64long(2 * i + 1));
            }
        }
    }

    private static void populate(ValWrapper[] arr1, ValWrapper[] arr2, int eq) {
        BitSet eqset = (eq > 0 && eq < 100) ? indices(new Random(42), SIZE, (eq * SIZE) / 100) : null;
        for (int i = 0; i < SIZE; i++) {
            if (eq > 0 && (eq >= 100 || eqset.get(i))) {
                arr2[i] = arr1[i] = new ValWrapper(new Q64long(i));
            } else {
                arr1[i] = new ValWrapper(new Q64long(2 * i));
                arr2[i] = new ValWrapper(new Q64long(2 * i + 1));
            }
        }
    }

    public static class ObjWrapper {
        public Object f;

        public ObjWrapper(Object f) {
            this.f = f;
        }
    }


    @State(Scope.Thread)
    public abstract static class ObjState {
        ObjWrapper[] arr1, arr2;
    }

    public static class IntWrapper {
        public Int64 f;

        public IntWrapper(Int64 f) {
            this.f = f;
        }
    }

    @State(Scope.Thread)
    public abstract static class IntState {
        IntWrapper[] arr1, arr2;
    }

    public static class RefWrapper {
        public Q64long.ref f;

        public RefWrapper(Q64long.ref f) {
            this.f = f;
        }
    }


    @State(Scope.Thread)
    public abstract static class RefState {
        RefWrapper[] arr1, arr2;
    }

    public static class ValWrapper {
        public Q64long f;

        public ValWrapper(Q64long f) {
            this.f = f;
        }
    }


    @State(Scope.Thread)
    public abstract static class ValState {
        ValWrapper[] arr1, arr2;
    }

    public static class ObjState00 extends ObjState {
        @Setup
        public void setup() {
            arr1 = new ObjWrapper[SIZE];
            arr2 = new ObjWrapper[SIZE];
            populate(arr1, arr2, 0);
        }

    }

    public static class ObjState25 extends ObjState {
        @Setup
        public void setup() {
            arr1 = new ObjWrapper[SIZE];
            arr2 = new ObjWrapper[SIZE];
            populate(arr1, arr2, 25);
        }

    }

    public static class ObjState50 extends ObjState {
        @Setup
        public void setup() {
            arr1 = new ObjWrapper[SIZE];
            arr2 = new ObjWrapper[SIZE];
            populate(arr1, arr2, 50);
        }

    }

    public static class ObjState75 extends ObjState {
        @Setup
        public void setup() {
            arr1 = new ObjWrapper[SIZE];
            arr2 = new ObjWrapper[SIZE];
            populate(arr1, arr2, 75);
        }

    }

    public static class ObjState100 extends ObjState {
        @Setup
        public void setup() {
            arr1 = new ObjWrapper[SIZE];
            arr2 = new ObjWrapper[SIZE];
            populate(arr1, arr2, 100);
        }
    }

    public static class IntState00 extends IntState {
        @Setup
        public void setup() {
            arr1 = new IntWrapper[SIZE];
            arr2 = new IntWrapper[SIZE];
            populate(arr1, arr2, 0);
        }
    }

    public static class IntState25 extends IntState {
        @Setup
        public void setup() {
            arr1 = new IntWrapper[SIZE];
            arr2 = new IntWrapper[SIZE];
            populate(arr1, arr2, 25);
        }
    }

    public static class IntState50 extends IntState {
        @Setup
        public void setup() {
            arr1 = new IntWrapper[SIZE];
            arr2 = new IntWrapper[SIZE];
            populate(arr1, arr2, 50);
        }
    }

    public static class IntState75 extends IntState {
        @Setup
        public void setup() {
            arr1 = new IntWrapper[SIZE];
            arr2 = new IntWrapper[SIZE];
            populate(arr1, arr2, 75);
        }
    }

    public static class IntState100 extends IntState {
        @Setup
        public void setup() {
            arr1 = new IntWrapper[SIZE];
            arr2 = new IntWrapper[SIZE];
            populate(arr1, arr2, 100);
        }
    }

    public static class RefState00 extends RefState {
        @Setup
        public void setup() {
            arr1 = new RefWrapper[SIZE];
            arr2 = new RefWrapper[SIZE];
            populate(arr1, arr2, 0);
        }
    }

    public static class RefState25 extends RefState {
        @Setup
        public void setup() {
            arr1 = new RefWrapper[SIZE];
            arr2 = new RefWrapper[SIZE];
            populate(arr1, arr2, 25);
        }
    }

    public static class RefState50 extends RefState {
        @Setup
        public void setup() {
            arr1 = new RefWrapper[SIZE];
            arr2 = new RefWrapper[SIZE];
            populate(arr1, arr2, 50);
        }
    }

    public static class RefState75 extends RefState {
        @Setup
        public void setup() {
            arr1 = new RefWrapper[SIZE];
            arr2 = new RefWrapper[SIZE];
            populate(arr1, arr2, 75);
        }
    }

    public static class RefState100 extends RefState {
        @Setup
        public void setup() {
            arr1 = new RefWrapper[SIZE];
            arr2 = new RefWrapper[SIZE];
            populate(arr1, arr2, 100);
        }
    }

    public static class ValState00 extends ValState {
        @Setup
        public void setup() {
            arr1 = new ValWrapper[SIZE];
            arr2 = new ValWrapper[SIZE];
            populate(arr1, arr2, 0);
        }
    }

    public static class ValState25 extends ValState {
        @Setup
        public void setup() {
            arr1 = new ValWrapper[SIZE];
            arr2 = new ValWrapper[SIZE];
            populate(arr1, arr2, 25);
        }
    }

    public static class ValState50 extends ValState {
        @Setup
        public void setup() {
            arr1 = new ValWrapper[SIZE];
            arr2 = new ValWrapper[SIZE];
            populate(arr1, arr2, 50);
        }
    }

    public static class ValState75 extends ValState {
        @Setup
        public void setup() {
            arr1 = new ValWrapper[SIZE];
            arr2 = new ValWrapper[SIZE];
            populate(arr1, arr2, 75);
        }
    }

    public static class ValState100 extends ValState {
        @Setup
        public void setup() {
            arr1 = new ValWrapper[SIZE];
            arr2 = new ValWrapper[SIZE];
            populate(arr1, arr2, 100);
        }
    }

}
