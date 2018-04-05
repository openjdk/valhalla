/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
package runtime.valhalla.valuetypes;

public __ByValue final class Point {
    final int x;
    final int y;

    private Point() {
        x = 0;
        y = 0;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public boolean isSamePoint(Point that) {
        return this.getX() == that.getX() && this.getY() == that.getY();
    }

    public String toString() {
        return "Point: x=" + getX() + " y=" + getY();
    }

    public boolean equals(Object o) {
        if(o instanceof Point) {
            return ((Point)o).x == x &&  ((Point)o).y == y;
        } else {
            return false;
        }
    }

    public static Point createPoint(int x, int y) {
        Point p = __MakeDefault Point();
        p = __WithField(p.x, x);
        p = __WithField(p.y, y);
        return p;
    }
}
