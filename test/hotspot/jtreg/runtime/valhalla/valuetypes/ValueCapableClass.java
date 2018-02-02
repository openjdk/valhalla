/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package runtime.valhalla.valuetypes;

@jdk.incubator.mvt.ValueCapableClass
public final class ValueCapableClass {

    public static final int DEFAULT_X = 11;
    public static final short DEFAULT_Y = 13;
    public static final short DEFAULT_Z = 15;
    public static final String STATIC_FIELD = "Should be left alone";

    public final int   x;
    public final short y;
    public final short z;

    private ValueCapableClass() {
        this(DEFAULT_X, DEFAULT_Y, DEFAULT_Z);
    }

    private ValueCapableClass(int x, short y, short z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public short getY() {
        return y;
    }

    public short getZ() {
        return z;
    }

    public String toString() {
        int ax = getX();
        short ay = getY();
        short az = getZ();
        return "ValueCapableClass x=" + ax + " y=" + ay + " z=" + az;
    }

    public static ValueCapableClass create(int x, short y, short z) {
        return new ValueCapableClass(x, y, z);
    }

    public static ValueCapableClass create() {
        return new ValueCapableClass();
    }

    public static void test() {
        ValueCapableClass value = create(4711, (short)7, (short)11);
        String s = value.toString();
        if ((value.getX() != 4711) || (value.getY() != 7) || value.getZ() != 11) {
            throw new IllegalStateException("Bad value: " + s);
        }
        System.out.println(s);
        ValueCapableClass defaultValue = create();
        s = defaultValue.toString();
        if ((defaultValue.getX() != DEFAULT_X) ||
            (defaultValue.getY() != DEFAULT_Y) ||
            (defaultValue.getZ() != DEFAULT_Z)) {
            throw new IllegalStateException("Bad value: " + s);
        }

        if (!STATIC_FIELD.equals("Should be left alone")) {
            throw new IllegalStateException("Bad static field: " + STATIC_FIELD);
        }
    }

    public static void main(String[] args) {
        test();
    }
}
