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
 * @modules java.base/java.lang:open
 * @enablePreview
 * @run main CloneValueClass
 */
public class CloneValueClass {

    static value class Point {
        int x;
        int y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    static value class MixedValue {
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
        int primitive;

        Holder(Point p, MixedValue m, int i) {
            this.point = p;
            this.mixed = m;
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

    // Clone an identity object holding custom value class fields
    static void testCloneHolderWithValueFields() throws Exception {
        Point p = new Point(10, 20);
        MixedValue m = new MixedValue(42, "hello");
        Holder orig = new Holder(p, m, 7);
        Holder copy = orig.clone();

        if (orig.point.x != copy.point.x || orig.point.y != copy.point.y)
            throw new RuntimeException("Point field not cloned correctly");
        if (orig.mixed.id != copy.mixed.id || !orig.mixed.name.equals(copy.mixed.name))
            throw new RuntimeException("MixedValue field not cloned correctly");
        if (orig.primitive != copy.primitive)
            throw new RuntimeException("Primitive field not cloned correctly");
    }

    // Modifying original value fields doesn't affect clone
    static void testCloneIndependence() throws Exception {
        Holder orig = new Holder(new Point(1, 2), new MixedValue(3, "test"), 5);
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

    // Clone arrays of custom value class elements
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

    // Clone holder with null value fields
    static void testNullValueFields() throws Exception {
        Holder orig = new Holder(null, null, 0);
        Holder copy = orig.clone();

        if (copy.point != null) throw new RuntimeException("Point should be null");
        if (copy.mixed != null) throw new RuntimeException("MixedValue should be null");
    }
}
