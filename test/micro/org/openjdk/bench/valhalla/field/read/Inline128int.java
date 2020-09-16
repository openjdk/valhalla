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
package org.openjdk.bench.valhalla.field.read;

import org.openjdk.bench.valhalla.field.util.StatesQ128int;
import org.openjdk.bench.valhalla.types.Q128int;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;

public class Inline128int extends StatesQ128int {

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void val_consume(Q128int v) {
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void ref_consume(Q128int.ref v) {
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_read(ValState st) {
        ValWrapper[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            val_consume(arr[i].f);
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_read(ValState st) {
        ValWrapper[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            ref_consume(arr[i].f);
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_read(RefState st) {
        RefWrapper[] arr = st.arr;
        for(int i=0; i < arr.length; i++) {
            val_consume(arr[i].f);
        }
    }

}
