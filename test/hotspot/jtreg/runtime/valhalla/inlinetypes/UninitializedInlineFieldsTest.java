/*
 * Copyright (c) 2017, 2024, Oracle and/or its affiliates. All rights reserved.
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
package runtime.valhalla.inlinetypes;

import jdk.internal.vm.annotation.NullRestricted;
import jdk.test.lib.Asserts;

/*
 * @test
 * @summary Uninitialized inline fields test
 * @library /test/lib
 * @modules java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @compile Point.java JumboInline.java UninitializedInlineFieldsTest.java
 * @run main/othervm -XX:InlineFieldMaxFlatSize=64 runtime.valhalla.inlinetypes.UninitializedInlineFieldsTest
 */
public class UninitializedInlineFieldsTest {
    static Point nonFlattenableStaticPoint;
    @NullRestricted
    static Point staticPoint;

    @NullRestricted
    Point instancePoint;

    static JumboInline sj1;
    @NullRestricted
    static JumboInline sj2;

    JumboInline j1;
    @NullRestricted
    JumboInline j2;

    static Object getNull() {
        return null;
    }

    UninitializedInlineFieldsTest() { }

    public static void main(String[] args) {
        checkUninitializedPoint(UninitializedInlineFieldsTest.staticPoint, 0, 0);
        Asserts.assertEquals(nonFlattenableStaticPoint, null, "invalid non flattenable static inline field");
        UninitializedInlineFieldsTest.staticPoint = new Point(456, 678);
        checkUninitializedPoint(UninitializedInlineFieldsTest.staticPoint, 456, 678);
        UninitializedInlineFieldsTest test = new UninitializedInlineFieldsTest();
        checkUninitializedPoint(test.instancePoint, 0, 0);
        test.instancePoint = new Point(123, 345);
        checkUninitializedPoint(test.instancePoint, 123, 345);

        Asserts.assertEquals(test.j1, null, "invalid non flattenable instance inline field");
        Asserts.assertEquals(test.j2.l0, 0L, "invalid flattenable instance inline field");
    }

    static void checkUninitializedPoint(Point p, int x, int y) {
        Asserts.assertEquals(p.x, x, "invalid x value");
        Asserts.assertEquals(p.y, y, "invalid y value");
    }
}
