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

public primitive class Q32int implements Int32, ByInt {

    public final int v0;

    public Q32int(int val) {
        this.v0 = val;
    }

    @Override
    public long longValue() {
        return (long)v0;
    }

    @Override
    public int intValue() {
        return v0;
    }

    @Override
    public Int32 neg() {
        return new Q32int(-v0);
    }

    @Override
    public Int32 add(Int32 o) {
        return new Q32int(v0 + o.intValue());
    }

    public Int32 add(Q32int o) {
        return new Q32int(v0 + o.v0);
    }

    @Override
    public Int32 sub(Int32 o) {
        return new Q32int(v0 - o.intValue());
    }

    public Int32 sub(Q32int o) {
        return new Q32int(v0 - o.v0);
    }

    @Override
    public Int32 mult(Int32 o) {
        return new Q32int(v0 * o.intValue());
    }

    public Int32 mult(Q32int o) {
        return new Q32int(v0 * o.v0);
    }

    @Override
    public int compareTo(Int32 o) {
        return Integer.compare(v0, o.intValue());
    }

    public int compareTo(Q32int o) {
        return Integer.compare(v0, o.v0);
    }

    @Override
    public int intSum() {
        return v0;
    }
}
