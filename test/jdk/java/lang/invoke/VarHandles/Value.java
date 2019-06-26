/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

final inline class Value {
    final char char_v;
    final byte byte_v;
    final boolean boolean_v;
    final int int_v;
    final short short_v;
    final long long_v;
    final double double_v;
    final float float_v;
    final Point point_v;
    Value() {
        char_v = 'z';
        boolean_v = true;
        byte_v = 0;
        int_v = 1;
        short_v = 2;
        long_v = 3;
        float_v = 0.1f;
        double_v = 0.2d;
        point_v = new Point(1, 1);
    }
    public Value(char c, boolean z, byte b, int x, short y, long l, float f, double d, Point p) {
        this.char_v = c;
        this.byte_v = b;
        this.boolean_v = z;
        this.int_v = x;
        this.short_v = y;
        this.long_v = l;
        this.float_v = f;
        this.double_v = d;
        this.point_v = p;
    }

    static Value getInstance() {
        return new Value('\u0123', true, (byte)0x01, 0x01234567, (short)0x0123,
                         0x0123456789ABCDEFL, 1.0f, 1.0d, new Point(1, 1));
    }

}
