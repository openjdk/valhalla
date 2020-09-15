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

public class R64byte implements Int64, ByByte {

    public final byte v0;
    public final byte v1;
    public final byte v2;
    public final byte v3;
    public final byte v4;
    public final byte v5;
    public final byte v6;
    public final byte v7;

    public R64byte(long v) {
        this((byte) (v >>> 56), (byte) (v >>> 48), (byte) (v >>> 40), (byte) (v >>> 32), (byte) (v >>> 24), (byte) (v >>> 16), (byte) (v >>> 8), (byte) (v));
    }

    public R64byte(byte v0, byte v1, byte v2, byte v3, byte v4, byte v5, byte v6, byte v7) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.v4 = v4;
        this.v5 = v5;
        this.v6 = v6;
        this.v7 = v7;
    }

    private static final long MASK = 0xFFL;
    private static final int IMASK = 0xFF;

    @Override
    public long longValue() {
        return ((v0 & MASK) << 56) | ((v1 & MASK) << 48) | ((v2 & MASK) << 40) | ((v3 & MASK) << 32) | ((v4 & MASK) << 24) | ((v5 & MASK) << 16) | ((v6 & MASK) << 8) | (v7 & MASK);
    }

    @Override
    public int intValue() {
        return ((v4 & IMASK) << 24) | ((v5 & IMASK) << 16) | ((v6 & IMASK) << 8) | (v7 & IMASK);
    }

    @Override
    public byte byteSum() {
        return (byte) (v0 + v1 + v2 + v3 + v4 + v5 + v6 + v7);
    }
}
