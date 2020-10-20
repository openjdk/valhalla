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

public inline class Q128int implements Int128, ByInt {

    public final Q64int v0;
    public final Q64int v1;

    public Q128int(long v) {
        this(0, v);
    }

    public Q128int(long hi, long lo) {
        this.v0 = new Q64int(hi);
        this.v1 = new Q64int(lo);
    }

    @Override
    public int intValue() {
        return v1.intValue();
    }

    @Override
    public long longValue() {
        return loValue();
    }

    @Override
    public long hiValue() {
        return v0.longValue();
    }

    @Override
    public long loValue() {
        return v1.longValue();
    }

    @Override
    public int intSum() {
        return v0.intSum() + v1.intSum();
    }
}
