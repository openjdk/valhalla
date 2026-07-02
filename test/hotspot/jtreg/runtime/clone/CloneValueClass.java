/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8384108
 * @summary Verify clone behavior with custom value classes
 * @library /test/lib
 * @modules java.base/java.lang:open
 * @run main CloneValueClass
 */
import jdk.test.lib.valueclass.AsValueClass;
public class CloneValueClass {

    @AsValueClass
    static class Point {
        int x;
        int y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    @AsValueClass
    static class MixedValue {
        int id;
        String name;
        MixedValue(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static class Holder implements Cloneable {
        Point point;
        MixedValue mixed;
        Point[] pointArray;
        MixedValue[] mixedArray;
        int primitive;

        Holder(Point p, MixedValue m, Point[] pa, MixedValue[] ma, int i) {
            this.point = p;
            this.mixed = m;
            this.pointArray = pa;
            this.mixedArray = ma;
            this.primitive = i;
        }

        public Holder clone() throws CloneNotSupportedException {
            return (Holder) super.clone();
        }
    }

    public static void main(String[] args) throws Exception {
        testValueObjectCloneThrows();
        testCloneHolderWithValueFields();
        testCloneIndependence();
        testArrayFieldShallowClone();
        testValueArrayClone();
        testNullValueFields();
    }

    // Cloning a value object directly should throw CloneNotSupportedException
    static void testValueObjectCloneThrows() throws Exception {
        Point p = new Point(1, 2);
        try {
            java.lang.reflect.Method m = Object.class.getDeclaredMethod("clone");
            m.setAccessible(true);
            m.invoke(p);
            throw new RuntimeException("Expected CloneNotSupportedException for value object");
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (!(e.getCause() instanceof CloneNotSupportedException)) {
                throw new RuntimeException("Expected CloneNotSupportedException, got " +
                        e.getCause().getClass().getName());
            }
        }
    }

    // Clone an identity object holding custom value class fields and arrays
    static void testCloneHolderWithValueFields() throws Exception {
        Point[] pa = new Point[]{new Point(5, 6), new Point(7, 8)};
        MixedValue[] ma = new MixedValue[]{new MixedValue(10, "a"), new MixedValue(20, "b")};
        Holder orig = new Holder(new Point(1, 2), new MixedValue(42, "hello"), pa, ma, 7);
        Holder copy = orig.clone();

        if (orig.point.x != copy.point.x || orig.point.y != copy.point.y)
            throw new RuntimeException("Point field not cloned correctly");
        if (orig.mixed.id != copy.mixed.id || !orig.mixed.name.equals(copy.mixed.name))
            throw new RuntimeException("MixedValue field not cloned correctly");
        if (copy.pointArray.length != 2 || copy.pointArray[0].x != 5)
            throw new RuntimeException("Point array not cloned correctly");
        if (copy.mixedArray.length != 2 || !copy.mixedArray[1].name.equals("b"))
            throw new RuntimeException("MixedValue array not cloned correctly");
        if (orig.primitive != copy.primitive)
            throw new RuntimeException("Primitive field not cloned correctly");
    }

    // Modifying original value fields doesn't affect clone
    static void testCloneIndependence() throws Exception {
        Point[] pa = new Point[]{new Point(1, 1)};
        MixedValue[] ma = new MixedValue[]{new MixedValue(1, "x")};
        Holder orig = new Holder(new Point(1, 2), new MixedValue(3, "test"), pa, ma, 5);
        Holder copy = orig.clone();

        orig.point = new Point(99, 99);
        orig.mixed = new MixedValue(0, "changed");
        orig.primitive = 0;

        if (copy.point.x != 1 || copy.point.y != 2)
            throw new RuntimeException("Point clone not independent");
        if (copy.mixed.id != 3 || !copy.mixed.name.equals("test"))
            throw new RuntimeException("MixedValue clone not independent");
        if (copy.primitive != 5)
            throw new RuntimeException("Primitive clone not independent");
    }

    // Shallow clone shares array references
    static void testArrayFieldShallowClone() throws Exception {
        Point[] pa = new Point[]{new Point(1, 2), new Point(3, 4)};
        MixedValue[] ma = new MixedValue[]{new MixedValue(10, "a")};
        Holder orig = new Holder(new Point(0, 0), new MixedValue(0, ""), pa, ma, 0);
        Holder copy = orig.clone();

        // Shallow clone — array references are shared
        if (orig.pointArray != copy.pointArray)
            throw new RuntimeException("Shallow clone should share pointArray reference");
        if (orig.mixedArray != copy.mixedArray)
            throw new RuntimeException("Shallow clone should share mixedArray reference");

        // Replacing array on original doesn't affect copy
        orig.pointArray = new Point[]{new Point(99, 99)};
        if (copy.pointArray.length != 2 || copy.pointArray[0].x != 1)
            throw new RuntimeException("Array field replacement should not affect clone");
    }

    // Clone arrays of custom value class elements directly
    static void testValueArrayClone() {
        Point[] orig = new Point[]{new Point(1, 2), new Point(3, 4), new Point(5, 6)};
        Point[] copy = orig.clone();

        if (orig == copy)
            throw new RuntimeException("Array clone returned same reference");
        for (int i = 0; i < orig.length; i++) {
            if (orig[i].x != copy[i].x || orig[i].y != copy[i].y)
                throw new RuntimeException("Array element " + i + " not cloned correctly");
        }
        orig[0] = new Point(99, 99);
        if (copy[0].x != 1 || copy[0].y != 2)
            throw new RuntimeException("Array clone not independent");
    }

    // Clone holder with null value fields and null arrays
    static void testNullValueFields() throws Exception {
        Holder orig = new Holder(null, null, null, null, 0);
        Holder copy = orig.clone();

        if (copy.point != null) throw new RuntimeException("Point should be null");
        if (copy.mixed != null) throw new RuntimeException("MixedValue should be null");
        if (copy.pointArray != null) throw new RuntimeException("pointArray should be null");
        if (copy.mixedArray != null) throw new RuntimeException("mixedArray should be null");
    }
}
