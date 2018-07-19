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

public final __ByValue class Line {
    public Point p1;
    public Point p2;
    Line () {
        this.p1 = Point.makePoint(0, 0);
        this.p2 = Point.makePoint(0, 0);
    }

    public Point p1() {
        return p1;
    }

    public Point p2() {
        return p2;
    }

    public static Line makeLine(int x1, int y1, int x2, int y2) {
        return makeLine(Point.makePoint(x1, y1), Point.makePoint(x2, y2));
    }

    public static Line makeLine(Point p1, Point p2) {
        Line l = __MakeDefault Line();

        l = __WithField(l.p1, p1);
        l = __WithField(l.p2, p2);
        return l;
    }
}
