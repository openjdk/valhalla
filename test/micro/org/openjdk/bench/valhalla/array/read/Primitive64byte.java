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
package org.openjdk.bench.valhalla.array.read;

import org.openjdk.bench.valhalla.array.util.PrimitiveStates;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;

public class Primitive64byte extends PrimitiveStates {

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void consume_byte(byte v0, byte v1, byte v2, byte v3, byte v4, byte v5, byte v6, byte v7) {
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void p64byte_read(Primitive64byte st) {
        byte[] values = st.arr;
        for (int i = 0; i < values.length; i+=8) {
            consume_byte(values[i], values[i+1], values[i+2], values[i+3], values[i+4], values[i+5], values[i+6], values[i+7]);
        }
    }

}
