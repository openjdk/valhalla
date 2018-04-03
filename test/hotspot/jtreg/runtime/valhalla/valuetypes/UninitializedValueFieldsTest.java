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
package runtime.valhalla.valuetypes;

import jdk.test.lib.Asserts;

/*
 * @test
 * @summary Uninitialized value fields test
 * @library /test/lib
 * @compile -XDenableValueTypes Point.java JumboValue.java
 * @run main/othervm -Xint -XX:ValueFieldMaxFlatSize=64 -XX:+EnableValhalla runtime.valhalla.valuetypes.UninitializedValueFieldsTest
 */


/*
 * @run main/othervm -Xcomp -XX:+EnableValhalla -XX:ValueFieldMaxFlatSize=64 runtime.valhalla.valuetypes.UninitializedValueFieldsTest
 */
public class UninitializedValueFieldsTest {

    static __NotFlattened Point nonFlattenableStaticPoint;
    static Point staticPoint;

    Point instancePoint;

    static __NotFlattened JumboValue sj1;
    static JumboValue sj2;

    __NotFlattened JumboValue j1;
    JumboValue j2;

    static Object getNull() {
        return null;
    }

    UninitializedValueFieldsTest() { }

    public static void main(String[] args) {
        checkUninitializedPoint(UninitializedValueFieldsTest.staticPoint, 0, 0);
        Asserts.assertEquals(nonFlattenableStaticPoint, null, "invalid non flattenable static value field");
        UninitializedValueFieldsTest.staticPoint = Point.createPoint(456, 678);
        checkUninitializedPoint(UninitializedValueFieldsTest.staticPoint, 456, 678);
        UninitializedValueFieldsTest test = new UninitializedValueFieldsTest();
        checkUninitializedPoint(test.instancePoint, 0, 0);
        test.instancePoint = Point.createPoint(123, 345);
        checkUninitializedPoint(test.instancePoint, 123, 345);

        Asserts.assertEquals(test.j1, null, "invalid non flattenable instance value field");
        Asserts.assertEquals(test.j2.l0, 0L, "invalid flattenable instance value field");
    }

    static void checkUninitializedPoint(Point p, int x, int y) {
        Asserts.assertEquals(p.x, x, "invalid x value");
        Asserts.assertEquals(p.y, y, "invalid y value");
    }
}
