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
package org.openjdk.bench.valhalla.field.copy;

import org.openjdk.bench.valhalla.field.util.StatesQ64long;
import org.openjdk.bench.valhalla.types.Int64;
import org.openjdk.bench.valhalla.types.Q64long;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;

public class Inline64long extends StatesQ64long {

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Obj_copy(ObjState s, ObjState d) {
        ObjWrapper[] src = s.arr;
        ObjWrapper[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i].f = src[i].f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Int_copy(ObjState s, IntState d) {
        ObjWrapper[] src = s.arr;
        IntWrapper[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i].f = (Int64) src[i].f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Ref_copy(ObjState s, RefState d) {
        ObjWrapper[] src = s.arr;
        RefWrapper[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i].f = (Q64long.ref)src[i].f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Obj_to_Val_copy(ObjState s, ValState d) {
        ObjWrapper[] src = s.arr;
        ValWrapper[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i].f = (Q64long) src[i].f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Obj_copy(IntState s, ObjState d) {
        IntWrapper[] src = s.arr;
        ObjWrapper[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i].f = src[i].f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Int_copy(IntState s, IntState d) {
        IntWrapper[] src = s.arr;
        IntWrapper[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i].f = src[i].f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Ref_copy(IntState s, RefState d) {
        IntWrapper[] src = s.arr;
        RefWrapper[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i].f = (Q64long.ref)src[i].f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Int_to_Val_copy(IntState s, ValState d) {
        IntWrapper[] src = s.arr;
        ValWrapper[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i].f = (Q64long) src[i].f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Obj_copy(RefState s, ObjState d) {
        RefWrapper[] src = s.arr;
        ObjWrapper[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i].f = src[i].f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Int_copy(RefState s, IntState d) {
        RefWrapper[] src = s.arr;
        IntWrapper[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i].f = src[i].f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Ref_copy(RefState s, RefState d) {
        RefWrapper[] src = s.arr;
        RefWrapper[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i].f = src[i].f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Ref_to_Val_copy(RefState s, ValState d) {
        RefWrapper[] src = s.arr;
        ValWrapper[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i].f = src[i].f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Obj_copy(ValState s, ObjState d) {
        ValWrapper[] src = s.arr;
        ObjWrapper[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i].f = src[i].f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Int_copy(ValState s, IntState d) {
        ValWrapper[] src = s.arr;
        IntWrapper[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i].f = src[i].f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Ref_copy(ValState s, RefState d) {
        ValWrapper[] src = s.arr;
        RefWrapper[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i].f = src[i].f;
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void Val_to_Val_copy(ValState s, ValState d) {
        ValWrapper[] src = s.arr;
        ValWrapper[] dst = d.arr;
        for (int i = 0; i < src.length; i++) {
            dst[i].f = src[i].f;
        }
    }


}
