/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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

package runtime.valhalla.typerestrictions;

import java.lang.invoke.RestrictedType;

class PointBox {

    @RestrictedType("Qruntime/valhalla/typerestrictions/PointBox$Point;")
    static public Point.ref p84;

    @RestrictedType("Qruntime/valhalla/typerestrictions/PointBox$Point;")
    static public Object p71;

    static inline class Point {
        public double x;
        public double y;

        public Point(double x, double y) { this.x = x; this.y = y; }
    }

    static inline class Rec {
        @RestrictedType("Qruntime/valhalla/typerestrictions/PointBox$Point;")
        public Point.ref p37;

        @RestrictedType("Qruntime/valhalla/typerestrictions/PointBox$Point;")
        public Object p23;

        public Rec() { this.p37 = new Point(0.0, 0.0); this.p23 = new Point(0.0, 0.0);}

        Rec setp37(Point.ref p) {
            Rec r = Rec.default;
            r = __WithField(r.p37, p);
            r = __WithField(r.p23, this.p23);
            return r;
        }

        Rec setp23(Object o) {
            Rec r = Rec.default;
            r = __WithField(r.p37, this.p37);
            r = __WithField(r.p23, o);
            return r;
        }
    }

    @RestrictedType("Qruntime/valhalla/typerestrictions/PointBox$Point;")
    public Point.ref p368;

    @RestrictedType("Qruntime/valhalla/typerestrictions/PointBox$Point;")
    public Object p397;
}
