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

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;

__ByValue final class MyValue3Inline {
    final float f7;
    final double f8;

    private MyValue3Inline() {
        this.f7 = 0;
        this.f8 = 0;
    }

    @ForceInline
    __ValueFactory static MyValue3Inline setF7(MyValue3Inline v, float f7) {
        v.f7 = f7;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3Inline setF8(MyValue3Inline v, double f8) {
        v.f8 = f8;
        return v;
    }

    @ForceInline
    __ValueFactory public static MyValue3Inline createDefault() {
        return __MakeDefault MyValue3Inline();
    }

    @ForceInline
    public static MyValue3Inline createWithFieldsInline(float f7, double f8) {
        MyValue3Inline v = createDefault();
        v = setF7(v, f7);
        v = setF8(v, f8);
        return v;
    }
}

// Value type definition to stress test return of a value in registers
// (uses all registers of calling convention on x86_64)
__ByValue public final class MyValue3 {
    final char c;
    final byte bb;
    final short s;
    final int i;
    final long l;
    final Object o;
    final float f1;
    final double f2;
    final float f3;
    final double f4;
    final float f5;
    final double f6;
    final MyValue3Inline v1;

    private MyValue3() {
        this.c = 0;
        this.bb = 0;
        this.s = 0;
        this.i = 0;
        this.l = 0;
        this.o = null;
        this.f1 = 0;
        this.f2 = 0;
        this.f3 = 0;
        this.f4 = 0;
        this.f5 = 0;
        this.f6 = 0;
        this.v1 = MyValue3Inline.createDefault();
    }

    @ForceInline
    __ValueFactory static MyValue3 setC(MyValue3 v, char c) {
        v.c = c;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setBB(MyValue3 v, byte bb) {
        v.bb = bb;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setS(MyValue3 v, short s) {
        v.s = s;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setI(MyValue3 v, int i) {
        v.i = i;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setL(MyValue3 v, long l) {
        v.l = l;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setO(MyValue3 v, Object o) {
        v.o = o;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setF1(MyValue3 v, float f1) {
        v.f1 = f1;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setF2(MyValue3 v, double f2) {
        v.f2 = f2;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setF3(MyValue3 v, float f3) {
        v.f3 = f3;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setF4(MyValue3 v, double f4) {
        v.f4 = f4;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setF5(MyValue3 v, float f5) {
        v.f5 = f5;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setF6(MyValue3 v, double f6) {
        v.f6 = f6;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue3 setV1(MyValue3 v, MyValue3Inline v1) {
        v.v1 = v1;
        return v;
    }

    @ForceInline
    __ValueFactory public static MyValue3 createDefault() {
        return __MakeDefault MyValue3();
    }

    @ForceInline
    public static MyValue3 create() {
        java.util.Random r = Utils.getRandomInstance();
        MyValue3 v = createDefault();
        v = setC(v, (char)r.nextInt());
        v = setBB(v, (byte)r.nextInt());
        v = setS(v, (short)r.nextInt());
        v = setI(v, r.nextInt());
        v = setL(v, r.nextLong());
        v = setO(v, new Object());
        v = setF1(v, r.nextFloat());
        v = setF2(v, r.nextDouble());
        v = setF3(v, r.nextFloat());
        v = setF4(v, r.nextDouble());
        v = setF5(v, r.nextFloat());
        v = setF6(v, r.nextDouble());
        v = setV1(v, MyValue3Inline.createWithFieldsInline(r.nextFloat(), r.nextDouble()));
        return v;
    }

    @DontInline
    public static MyValue3 createDontInline() {
        return create();
    }

    @ForceInline
    public static MyValue3 copy(MyValue3 other) {
        MyValue3 v = createDefault();
        v = setC(v, other.c);
        v = setBB(v, other.bb);
        v = setS(v, other.s);
        v = setI(v, other.i);
        v = setL(v, other.l);
        v = setO(v, other.o);
        v = setF1(v, other.f1);
        v = setF2(v, other.f2);
        v = setF3(v, other.f3);
        v = setF4(v, other.f4);
        v = setF5(v, other.f5);
        v = setF6(v, other.f6);
        v = setV1(v, other.v1);
        return v;
    }

    @DontInline
    public void verify(MyValue3 other) {
        Asserts.assertEQ(c, other.c);
        Asserts.assertEQ(bb, other.bb);
        Asserts.assertEQ(s, other.s);
        Asserts.assertEQ(i, other.i);
        Asserts.assertEQ(l, other.l);
        Asserts.assertEQ(o, other.o);
        Asserts.assertEQ(f1, other.f1);
        Asserts.assertEQ(f2, other.f2);
        Asserts.assertEQ(f3, other.f3);
        Asserts.assertEQ(f4, other.f4);
        Asserts.assertEQ(f5, other.f5);
        Asserts.assertEQ(f6, other.f6);
        Asserts.assertEQ(v1.f7, other.v1.f7);
        Asserts.assertEQ(v1.f8, other.v1.f8);
    }
}
