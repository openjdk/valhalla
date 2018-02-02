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

package compiler.valhalla.valuetypes;

__ByValue final class MyValue2Inline {
    final boolean b;
    final long c;

    private MyValue2Inline() {
        this.b = false;
        this.c = 0;
    }

    @ForceInline
    __ValueFactory static MyValue2Inline setB(MyValue2Inline v, boolean b) {
        v.b = b;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue2Inline setC(MyValue2Inline v, long c) {
        v.c = c;
        return v;
    }

    @ForceInline
    __ValueFactory public static MyValue2Inline createDefault() {
        return __MakeDefault MyValue2Inline();
    }

    @ForceInline
    public static MyValue2Inline createWithFieldsInline(boolean b, long c) {
        MyValue2Inline v = MyValue2Inline.createDefault();
        v = MyValue2Inline.setB(v, b);
        v = MyValue2Inline.setC(v, c);
        return v;
    }
}

__ByValue public final class MyValue2 {
    final int x;
    final byte y;
    final MyValue2Inline v1;

    private MyValue2() {
        this.x = 0;
        this.y = 0;
        this.v1 = MyValue2Inline.createDefault();
    }

    @ForceInline
    __ValueFactory public static MyValue2 createDefaultInline() {
        return __MakeDefault MyValue2();
    }

    @ForceInline
    public static MyValue2 createWithFieldsInline(int x, boolean b) {
        MyValue2 v = createDefaultInline();
        v = setX(v, x);
        v = setY(v, (byte)x);
        v = setV1(v, MyValue2Inline.createWithFieldsInline(b, ValueTypeTest.rL));
        return v;
    }

    @ForceInline
    public long hash() {
        return x + y + (v1.b ? 0 : 1) + v1.c;
    }

    @DontInline
    public long hashInterpreted() {
        return x + y + (v1.b ? 0 : 1) + v1.c;
    }

    @ForceInline
    public void print() {
        System.out.print("x=" + x + ", y=" + y + ", b=" + v1.b + ", c=" + v1.c);
    }

    @ForceInline
    __ValueFactory static MyValue2 setX(MyValue2 v, int x) {
        v.x = x;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue2 setY(MyValue2 v, byte y) {
        v.y = y;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue2 setV1(MyValue2 v, MyValue2Inline v1) {
        v.v1 = v1;
        return v;
    }
}
