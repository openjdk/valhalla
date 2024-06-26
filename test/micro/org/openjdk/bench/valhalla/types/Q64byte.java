/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

public value class Q64byte implements Int64, ByByte {

    public final Q32byte v0;
    public final Q32byte v1;

    public Q64byte() {
        this.v0 = new Q32byte();
        this.v1 = new Q32byte();
    }

    public Q64byte(long v) {
        this((int) (v >>> 32), (int) v);
    }

    public Q64byte(int hi, int lo) {
        this.v0 = new Q32byte(hi);
        this.v1 = new Q32byte(lo);
    }

    private static final long MASK = 0xFFFFFFFFL;

    @Override
    public long longValue() {
        return (v0.intValue() & MASK) << 32 | (v1.intValue() & MASK);
    }

    @Override
    public int intValue() {
        return v1.intValue();
    }

    @Override
    public byte byteSum() {
        return (byte) (v0.byteSum() + v1.byteSum());
    }
}
