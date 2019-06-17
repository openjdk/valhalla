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

public inline class Value {
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
        byte_v = 0;
        boolean_v = true;
        int_v = 1;
        short_v = 2;
        long_v = 3;
        float_v = 0.1f;
        double_v = 0.2d;
        number_v = null;
        point_v = Point.makePoint(0,0);
        ref_v = null;
    }
    Value(char c, boolean z, byte b, int x, short y, long l, float f, double d, Number number, Point p, Object o) {
        char_v = c;
        byte_v = b;
        boolean_v = z;
        int_v = x;
        short_v = y;
        long_v = l;
        float_v = f;
        double_v = d;
        number_v = number;
        point_v = p;
        ref_v = o;
    }

    static class Builder {
        private char c;
        private byte b;
        private boolean z;
        private int i;
        private short s;
        private long l;
        private double d;
        private float f;
        private Number n;
        private Point p = Point.makePoint(0,0);
        private Object ref;

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
            return new Value(c, z, b, i, s, l, f, d, n, p, ref);
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
            return new IntValue(i);
        }

        static ShortValue shortValue(short s) {
            return new ShortValue(s);
        }
    }

    static inline class IntValue implements Number {
        int i;
        IntValue(int i) {
            this.i = i;
        }
        public int intValue() {
            return i;
        }
    }

    static inline class ShortValue implements Number {
        short s;
        ShortValue(short s) {
            this.s = s;
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

        @Override
        public String toString() {
            return Integer.toString(i);
        }
    }
}
