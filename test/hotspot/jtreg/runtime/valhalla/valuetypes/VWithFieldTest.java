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

package runtime.valhalla.valuetypes;

import jdk.test.lib.Asserts;

/*
 * @test VWithFieldTest
 * @summary vwithfield bytecode test
 * @library /test/lib
 * @compile -XDenableValueTypes -XDallowWithFieldOperator Point.java
 * @compile -XDenableValueTypes -XDallowWithFieldOperator VWithFieldTest.java
 * @run main/othervm -Xint -XX:+EnableValhalla runtime.valhalla.valuetypes.VWithFieldTest
 * @run main/othervm -Xcomp -XX:+EnableValhalla runtime.valhalla.valuetypes.VWithFieldTest
 */

public class VWithFieldTest {

    static __ByValue final class Point {
        final private int x;
        final private int y;

        static Point make(int x, int y) {
            Point p = Point.default;
            Asserts.assertEquals(p.x, 0, "invalid x default value");
            Asserts.assertEquals(p.y, 0, "invalid y default value");
            p = __WithField(p.x, x);
            Asserts.assertEquals(p.x, x, "invalid x value");
            Asserts.assertEquals(p.y, 0, "invalid y value");
            p = __WithField(p.y, y);
            Asserts.assertEquals(p.x, x, "invalid x value");
            Asserts.assertEquals(p.y, y, "invalid y value");
            return p;
        }

        Point () {
            x = 0;
            y = 0;
        }

        public int getX() {
            return x;
        }

        static Point setX(Point p, int x) {
            p = __WithField(p.x, x);
            return p;
        }

        public int getY() {
            return y;
        }

        static Point setY(Point p, int y) {
            p = __WithField(p.y, y);
            return p;
        }
    }

    public static void main(String[] args) {
        creationTest();
        creationTest();
        witherTest();
        witherTest();
    }

    static void creationTest() {
        Point p = Point.make(10,20);
        Asserts.assertEquals(p.x, 10, "invalid x value");
        Asserts.assertEquals(p.y, 20, "invalid y value");
    }

    static void witherTest() {
        Point p1 = Point.make(2,12);
        Asserts.assertEquals(p1.x, 2, "invalid x value");
        Asserts.assertEquals(p1.y, 12, "invalid y value");
        Point p2 = Point.setX(p1,3);
        Asserts.assertEquals(p2.x, 3, "invalid x value");
        Asserts.assertEquals(p2.y, 12, "invalid y value");
        Point p3 = Point.setY(p2, 14);
        Asserts.assertEquals(p3.x, 3, "invalid x value");
        Asserts.assertEquals(p3.y, 14, "invalid y value");
    }

}
