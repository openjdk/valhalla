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

public class R128int implements Int128, ByInt {

    public final int v0;
    public final int v1;
    public final int v2;
    public final int v3;

    public R128int(long v) {
        this(0, 0, (int) (v >>> 32), (int) v);
    }

    public R128int(int v0, int v1, int v2, int v3) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }

    private static final long MASK = 0xFFFFFFFFL;

    @Override
    public int intValue() {
        return v3;
    }

    @Override
    public long longValue() {
        return loValue();
    }

    @Override
    public long hiValue() {
        return (v0 & MASK) << 32 | (v1 & MASK);
    }

    @Override
    public long loValue() {
        return (v2 & MASK) << 32 | (v3 & MASK);
    }

    @Override
    public int intSum() {
        return v0 + v1 + v2 + v3;
    }
}
