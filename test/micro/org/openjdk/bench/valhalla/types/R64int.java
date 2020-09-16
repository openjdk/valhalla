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
package org.openjdk.bench.valhalla.types;

public class R64int implements Int64, ByInt {

    public final int v0;
    public final int v1;

    public R64int(long v) {
        this((int) (v >>> 32), (int) v);
    }

    public R64int(int hi, int lo) {
        this.v0 = hi;
        this.v1 = lo;
    }

    private static final long MASK = 0xFFFFFFFFL;

    @Override
    public long longValue() {
        return (v0 & MASK) << 32 | (v1 & MASK);
    }

    @Override
    public int intValue() {
        return v1;
    }

    @Override
    public int intSum() {
        return v0 + v1 ;
    }
}
