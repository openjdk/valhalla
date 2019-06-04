/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @test
 * @bug 8222555 8222553
 * @summary Prove that code suspected of not compiling actually compiles fine.
 * @compile -XDallowWithFieldOperator CompilesJustFine.java
 */

class CompilesFine {

    static Point? nfspQm;

    public static void main(String[] args) {
        nfspQm = null;
    }
}
inline final class Point {
    final int x;
    final int y;

    Point() {
        x = 0;
        y = 0;
    }
}

class CompilesJustFine {

    static final inline class Value {
        final PointBug2? nfpQm;

        private Value() {
            nfpQm = PointBug2.createPoint(0, 0);
        }
    }
}
inline final class PointBug2 {
    final int x;
    final int y;

    PointBug2() {
        x = 0;
        y = 0;
    }

    public static PointBug2 createPoint(int x, int y) {
        PointBug2 p = PointBug2.default;
        p = __WithField(p.x, x);
        p = __WithField(p.y, y);
        return p;
    }
}
