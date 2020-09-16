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

import org.openjdk.bench.valhalla.types.Int32;
import org.openjdk.bench.valhalla.types.Q32int;
import org.openjdk.bench.valhalla.util.SizeBase;
import org.openjdk.jmh.annotations.Setup;

public class StatesQ32int extends SizeBase {

    public static class ObjWrapper {
        public Object f;

        public ObjWrapper(Object f) {
            this.f = f;
        }
    }

    public static class ObjState extends SizeState {
        public ObjWrapper[] arr;
        @Setup
        public void setup() {
            arr = new ObjWrapper[size];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new ObjWrapper(new Q32int(i));
            }
        }
    }

    public static class IntWrapper {
        public Int32 f;

        public IntWrapper(Int32 f) {
            this.f = f;
        }
    }

    public static class IntState extends SizeState {
        public IntWrapper[] arr;
        @Setup
        public void setup() {
            arr = new IntWrapper[size];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new IntWrapper(new Q32int(i));
            }
        }
    }


    public static class RefWrapper {
        public Q32int.ref f;

        public RefWrapper(Q32int.ref f) {
            this.f = f;
        }
    }

    public static class RefState extends SizeState {
        public RefWrapper[] arr;
        @Setup
        public void setup() {
            arr = new RefWrapper[size];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new RefWrapper(new Q32int(i));
            }
        }
    }


    public static class ValWrapper {
        public Q32int f;

        public ValWrapper(Q32int f) {
            this.f = f;
        }
    }

    public static class ValState extends SizeState {
        public ValWrapper[] arr;
        @Setup
        public void setup() {
            arr = new ValWrapper[size];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = new ValWrapper(new Q32int(i));
            }
        }
    }


}
