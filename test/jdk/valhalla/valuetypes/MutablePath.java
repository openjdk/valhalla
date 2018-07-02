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

public final class MutablePath {
    public Point p1;
    public Point p2;

    public Point p1() {
        return p1;
    }

    public Point p2() {
        return p2;
    }

    public void set(int x1, int y1, int x2, int y2) {
        this.p1 = Point.makePoint(x1, y1);
        this.p2 = Point.makePoint(x2, y2);
    }

    @Override
    public String toString() {
        return "MutablePath" + p1 + ", " + p2;
    }

    public static MutablePath makePath(int x1, int y1, int x2, int y2) {
        MutablePath path = new MutablePath();
        path.set(x1, y1, x2, y2);
        return path;
    }
}
