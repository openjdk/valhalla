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

// Value type definition with too many fields to return in registers
__ByValue final class MyValue4 {
    final MyValue3 v1;
    final MyValue3 v2;

    private MyValue4() {
        this.v1 = MyValue3.createDefault();
        this.v2 = MyValue3.createDefault();
    }

    @ForceInline
    __ValueFactory static MyValue4 setV1(MyValue4 v, MyValue3 v1) {
        v.v1 = v1;
        return v;
    }

    @ForceInline
    __ValueFactory static MyValue4 setV2(MyValue4 v, MyValue3 v2) {
        v.v2 = v2;
        return v;
    }

    @ForceInline
    __ValueFactory public static MyValue4 createDefault() {
        return __MakeDefault MyValue4();
    }

    @ForceInline
    public static MyValue4 create() {
        MyValue4 v = createDefault();
        MyValue3 v1 = MyValue3.create();
        v = setV1(v, v1);
        MyValue3 v2 = MyValue3.create();
        v = setV2(v, v2);
        return v;
    }

    public void verify(MyValue4 other) {
        v1.verify(other.v1);
        v2.verify(other.v2);
    }
}
