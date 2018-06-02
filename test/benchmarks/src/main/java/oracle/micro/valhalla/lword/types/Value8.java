/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package oracle.micro.valhalla.lword.types;

import oracle.micro.valhalla.types.PNumber;

__ByValue public final class Value8 implements PNumber {

    public final int f0;
    public final int f1;
    public final int f2;
    public final int f3;
    public final int f4;
    public final int f5;
    public final int f6;
    public final int f7;

    private Value8() {
        this.f0 = 0;
        this.f1 = 0;
        this.f2 = 0;
        this.f3 = 0;
        this.f4 = 0;
        this.f5 = 0;
        this.f6 = 0;
        this.f7 = 0;
    }

    public static Value8 of(int f0,int f1,int f2,int f3,int f4,int f5,int f6,int f7) {
        Value8 v = __MakeDefault Value8();
        v = __WithField(v.f0, f0);
        v = __WithField(v.f1, f1);
        v = __WithField(v.f2, f2);
        v = __WithField(v.f3, f3);
        v = __WithField(v.f4, f4);
        v = __WithField(v.f5, f5);
        v = __WithField(v.f6, f6);
        v = __WithField(v.f7, f7);
        return v;
    }

    public int f0() {   return f0;   }
    public int f1() {   return f1;   }
    public int f2() {   return f2;   }
    public int f3() {   return f3;   }
    public int f4() {   return f4;   }
    public int f5() {   return f5;   }
    public int f6() {   return f6;   }
    public int f7() {   return f7;   }

    public Value8 add(Value8 v) {
        return of(this.f0 + v.f0, this.f1 + v.f1, this.f2 + v.f2, this.f3 + v.f3, this.f4 + v.f4, this.f5 + v.f5, this.f6 + v.f6, this.f7 + v.f7);
    }

    public int totalsum() {
        return f0 + f1 + f2 + f3 + f4 + f5 + f6 + f7;
    }

    @Override
    public int hashCode() {
        return f0 + f1 + f2 + f3 + f4 + f5 + f6 + f7;
    }

    @Override
    public Value8 inc() {
        return of(f1, f2, f3, f4, f5, f6, f7, f0 + 1);
    }

    @Override
    public Value8 dec() {
        return of(f1, f2, f3, f4, f5, f6, f7, f0 - 1);
    }

}
