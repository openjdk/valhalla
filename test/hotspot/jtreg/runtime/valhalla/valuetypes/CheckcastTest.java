/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test CheckcastTest
 * @summary checkcast bytecode test
 * @library /test/lib
 * @compile VDefaultTest.java
 * @run main/othervm -Xint runtime.valhalla.valuetypes.CheckcastTest
 * @run main/othervm -Xcomp runtime.valhalla.valuetypes.CheckcastTest
 */

public class CheckcastTest {

    static inline class Point {
        int x;
        int y;

        public Point() {
            x = 0;
            y = 0;
        }

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }


    static void testCastingFromObjectToVal(Object o) {
        boolean npe = false;
        try {
            Point pv = (Point)o;
        } catch(NullPointerException e) {
            npe = true;
        }
        Asserts.assertTrue(npe == false || o == null, "Casting null to val should throw a NPE");
    }

    static void testCastingFromValToBox(Point p) {
        boolean npe = false;
        try {
            Point? pb = p;
        } catch(NullPointerException e) {
            npe = true;
        }
        Asserts.assertFalse(npe, "Casting from val to box should not throw an NPE");
    }

    static void testCastingFromBoxToVal(Point? p) {
        boolean npe = false;
        try {
            Point pv = (Point) p;
        } catch(NullPointerException e) {
            npe = true;
        }
        if (npe) {
            Asserts.assertEquals(p, null, "NPE must be thrown only if p is null");
        } else {
            Asserts.assertNotEquals(p, null, "Casting null to val must thrown a NPE");
        }

    }

    public static void main(String[] args) {
        // Testing casting from box to val
        // First invocation: casting null to Point with an unresolved class entry
        testCastingFromBoxToVal(null);
        // Second invocation: casting non-null to val, will trigger resolution of the class entry
        testCastingFromBoxToVal(new Point(3,4));
        // Third invocation: casting null to Point with a resolved class entry
        testCastingFromBoxToVal(null);

        // Testing casting from val to box
        testCastingFromBoxToVal(new Point(3,4));

        // Testing casting from object to val
        // First invocation: casting null to Point with an unresolved class entry
        testCastingFromObjectToVal(null);
        // Second invocation: casting non-null to al, will trigger resolution of the class entry
        testCastingFromObjectToVal(new Point(3,4));
        // Third invocation: casting null to Point with a resolved class entry");
        testCastingFromObjectToVal(null);
        // Fourth invocation: with something not the right type
        boolean cce = false;
        try {
            testCastingFromObjectToVal(new String("NotPoint"));
        } catch(ClassCastException e) {
            cce = true;
        }
        Asserts.assertTrue(cce,"casting invalid type to val should throw CCE");
    }
}
