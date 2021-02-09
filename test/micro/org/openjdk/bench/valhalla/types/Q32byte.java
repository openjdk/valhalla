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

public primitive class Q32byte implements Int32, ByByte {

    public final byte v0;
    public final byte v1;
    public final byte v2;
    public final byte v3;

    public Q32byte(int v) {
        this((byte) (v >>> 24), (byte) (v >>> 16), (byte) (v >>> 8), (byte) (v));
    }

    public Q32byte(byte v0, byte v1, byte v2, byte v3) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }

    private static final int MASK = 0xFF;

    @Override
    public long longValue() {
        return (long) intValue();
    }

    @Override
    public int intValue() {
        return (((v0 & MASK) << 24) | ((v1 & MASK) << 16) | ((v2 & MASK) << 8) | (v3 & MASK));
    }

    @Override
    public Int32 neg() {
        return new Q32byte(-intValue());
    }

    @Override
    public Int32 add(Int32 o) {
        return new Q32byte(intValue() + o.intValue());
    }

    @Override
    public Int32 sub(Int32 o) {
        return new Q32byte(intValue() - o.intValue());
    }

    @Override
    public Int32 mult(Int32 o) {
        return new Q32byte(intValue() * o.intValue());
    }

    @Override
    public int compareTo(Int32 o) {
        return Integer.compare(intValue(), o.intValue());
    }

    @Override
    public byte byteSum() {
        return (byte) (v0 + v1 + v2 + v3);
    }
}
