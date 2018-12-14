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

public value class Value {
    char char_v;
    byte byte_v;
    boolean boolean_v;
    int int_v;
    short short_v;
    long long_v;
    double double_v;
    float float_v;
    Number number_v;
    Point point_v;
    Object ref_v;

    Value() {
        char_v = 'z';
        boolean_v = true;
        byte_v = 0;
        int_v = 1;
        short_v = 2;
        long_v = 3;
        float_v = 0.1f;
        double_v = 0.2d;
        point_v = Point.makePoint(0,0);
        number_v = null;
        ref_v = null;
    }
    static Value makeValue(char c, boolean z, byte b, int x, short y, long l, float f, double d, Number number, Point p, Object o) {
        Value v = Value.default;
        v = __WithField(v.char_v, c);
        v = __WithField(v.byte_v, b);
        v = __WithField(v.boolean_v, z);
        v = __WithField(v.int_v, x);
        v = __WithField(v.short_v, y);
        v = __WithField(v.long_v, l);
        v = __WithField(v.float_v, f);
        v = __WithField(v.double_v, d);
        v = __WithField(v.number_v, number);
        v = __WithField(v.point_v, p);
        v = __WithField(v.ref_v, o);
        return v;
    }

    static class Builder {
        private static final Object REF = new Object();
        private char c;
        private byte b;
        private boolean z;
        private int i;
        private short s;
        private long l;
        private double d;
        private float f;
        private Number n = Number.intValue(0);
        private Point p = Point.makePoint(0,0);
        private Object ref = REF;

        public Builder() {}
        Builder setChar(char c) {
            this.c = c;
            return this;
        }
        Builder setByte(byte b) {
            this.b = b;
            return this;
        }
        Builder setBoolean(boolean z) {
            this.z = z;
            return this;
        }
        Builder setInt(int i) {
            this.i = i;
            return this;
        }
        Builder setShort(short s) {
            this.s = s;
            return this;
        }
        Builder setLong(long l) {
            this.l = l;
            return this;
        }
        Builder setDouble(double d) {
            this.d = d;
            return this;
        }
        Builder setFloat(float f) {
            this.f = f;
            return this;
        }
        Builder setNumber(Number n) {
            this.n = n;
            return this;
        }
        Builder setPoint(Point p) {
            this.p = p;
            return this;
        }
        Builder setReference(Object o) {
            this.ref = o;
            return this;
        }
        Value build() {
            return Value.makeValue(c, z, b, i, s, l, f, d, n, p, ref);
        }
    }

    interface Number {
        default int intValue() {
            throw new UnsupportedOperationException();
        }
        default short shortValue() {
            throw new UnsupportedOperationException();
        }

        static IntValue intValue(int i) {
            IntValue v = IntValue.default;
            v = __WithField(v.i, i);
            return v;
        }

        static ShortValue shortValue(short s) {
            ShortValue v = ShortValue.default;
            v = __WithField(v.s, s);
            return v;
        }
    }

    static value class IntValue implements Number {
        int i;
        IntValue() {
            i = 0;
        }
        public int intValue() {
            return i;
        }
    }

    static value class ShortValue implements Number {
        short s;
        ShortValue() {
            s = 0;
        }
        public short shortValue() {
            return s;
        }
    }

    static class IntNumber implements Number {
        final int i;
        public IntNumber(int i) {
            this.i = i;
        }

        public int intValue() {
            return i;
        }
    }
}
