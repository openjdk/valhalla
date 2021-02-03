/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

package compiler.valhalla.inlinetypes;

public final inline class MyValue1 extends MyAbstract {
    static int s;
    static final long sf = InlineTypeTest.rL;
    final int x;
    final long y;
    final short z;
    final Integer o;
    final int[] oa;
    final MyValue2 v1;
    final MyValue2 v2;
    static final MyValue2 v3 = MyValue2.createWithFieldsInline(InlineTypeTest.rI, InlineTypeTest.rD);
    final int c;

    @ForceInline
    public MyValue1(int x, long y, short z, Integer o, int[] oa, MyValue2 v1, MyValue2 v2, int c) {
        s = 0;
        this.x = x;
        this.y = y;
        this.z = z;
        this.o = o;
        this.oa = oa;
        this.v1 = v1;
        this.v2 = v2;
        this.c = c;
    }

    @DontInline
    static MyValue1 createDefaultDontInline() {
        return createDefaultInline();
    }

    @ForceInline
    static MyValue1 createDefaultInline() {
        return MyValue1.default;
    }

    @DontInline
    static MyValue1 createWithFieldsDontInline(int x, long y) {
        return createWithFieldsInline(x, y);
    }

    @ForceInline
    static MyValue1 createWithFieldsInline(int x, long y) {
        MyValue1 v = createDefaultInline();
        v = setX(v, x);
        v = setY(v, y);
        v = setZ(v, (short)x);
        // Don't use Integer.valueOf here to avoid control flow added by Integer cache check
        v = setO(v, new Integer(x));
        int[] oa = {x};
        v = setOA(v, oa);
        v = setV1(v, MyValue2.createWithFieldsInline(x, y, InlineTypeTest.rD));
        v = setV2(v, MyValue2.createWithFieldsInline(x, y, InlineTypeTest.rD+x));
        v = setC(v, (int)(x+y));
        return v;
    }

    // Hash only primitive and inline type fields to avoid NullPointerException
    @ForceInline
    public long hashPrimitive() {
        return s + sf + x + y + z + c + v1.hash() + v2.hash() + v3.hash();
    }

    @ForceInline
    public long hash() {
        long res = hashPrimitive();
        try {
            res += o;
        } catch(NullPointerException npe) {}
        try {
            res += oa[0];
        } catch(NullPointerException npe) {}
        return res;
    }

    @DontCompile
    public long hashInterpreted() {
        return s + sf + x + y + z + o + oa[0] + c + v1.hashInterpreted() + v2.hashInterpreted() + v3.hashInterpreted();
    }

    @ForceInline
    public void print() {
        System.out.print("s=" + s + ", sf=" + sf + ", x=" + x + ", y=" + y + ", z=" + z + ", o=" + (o != null ? (Integer)o : "NULL") + ", oa=" + (oa != null ? oa[0] : "NULL") + ", v1[");
        v1.print();
        System.out.print("], v2[");
        v2.print();
        System.out.print("], v3[");
        v3.print();
        System.out.print("], c=" + c);
    }

    @ForceInline
    static MyValue1 setX(MyValue1 v, int x) {
        return new MyValue1(x, v.y, v.z, v.o, v.oa, v.v1, v.v2, v.c);
    }

    @ForceInline
    static MyValue1 setY(MyValue1 v, long y) {
        return new MyValue1(v.x, y, v.z, v.o, v.oa, v.v1, v.v2, v.c);
    }

    @ForceInline
    static MyValue1 setZ(MyValue1 v, short z) {
        return new MyValue1(v.x, v.y, z, v.o, v.oa, v.v1, v.v2, v.c);
    }

    @ForceInline
    static MyValue1 setO(MyValue1 v, Integer o) {
        return new MyValue1(v.x, v.y, v.z, o, v.oa, v.v1, v.v2, v.c);
    }

    @ForceInline
    static MyValue1 setOA(MyValue1 v, int[] oa) {
        return new MyValue1(v.x, v.y, v.z, v.o, oa, v.v1, v.v2, v.c);
    }

    @ForceInline
    static MyValue1 setC(MyValue1 v, int c) {
        return new MyValue1(v.x, v.y, v.z, v.o, v.oa, v.v1, v.v2, c);
    }

    @ForceInline
    static MyValue1 setV1(MyValue1 v, MyValue2 v1) {
        return new MyValue1(v.x, v.y, v.z, v.o, v.oa, v1, v.v2, v.c);
    }

    @ForceInline
    static MyValue1 setV2(MyValue1 v, MyValue2 v2) {
        return new MyValue1(v.x, v.y, v.z, v.o, v.oa, v.v1, v2, v.c);
    }
}
