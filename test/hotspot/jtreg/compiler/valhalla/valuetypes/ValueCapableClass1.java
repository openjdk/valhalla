/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * A value-capable class (VCC) from which HotSpot derives a value
 * type. The derived value type (DVT) is referred to as
 * ValueCapableClass1$Value.
 */
package compiler.valhalla.valuetypes;

@jdk.incubator.mvt.ValueCapableClass
public final class ValueCapableClass1 {
    public final long t;
    public final int x;
    public final short y;
    public final short z;

    private ValueCapableClass1(long t, int x, short y, short z) {
        this.t = t;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static ValueCapableClass1 create(long t, int x, short y, short z) {
        return new ValueCapableClass1(t, x, y, z);
    }

    @ForceInline
    public static ValueCapableClass1 createInline() {
        return new ValueCapableClass1(17L, 2, (short)3, (short)4);
    }

    int value() {
        return x + y + z;
    }
}
