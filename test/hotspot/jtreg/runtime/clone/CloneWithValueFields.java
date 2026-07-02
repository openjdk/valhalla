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
 * @summary Verify clone works correctly with value class fields and arrays
 * @run main CloneWithValueFields
 */
public class CloneWithValueFields {

    public static void main(String[] args) throws Exception {
        testBasicClone();
        testCloneIndependence();
        testNullValueFields();
        testInheritedValueFields();
        testValueArrayClone();
        testMixedArrayClone();
        testCloneGCSurvival();
    }

    // Clone an identity object with various value class fields
    static void testBasicClone() throws Exception {
        Container orig = new Container(Integer.valueOf(42), Long.valueOf(100L),
                                       Double.valueOf(3.14), Short.valueOf((short)7));
        Container copy = orig.clone();

        assertEquals(orig.intVal, copy.intVal, "intVal");
        assertEquals(orig.longVal, copy.longVal, "longVal");
        assertEquals(orig.doubleVal, copy.doubleVal, "doubleVal");
        assertEquals(orig.shortVal, copy.shortVal, "shortVal");
    }

    // Verify clone produces an independent copy
    static void testCloneIndependence() throws Exception {
        Container orig = new Container(Integer.valueOf(42), Long.valueOf(100L),
                                       Double.valueOf(3.14), Short.valueOf((short)7));
        Container copy = orig.clone();

        orig.intVal = Integer.valueOf(99);
        orig.longVal = Long.valueOf(200L);
        orig.doubleVal = Double.valueOf(2.71);
        orig.shortVal = Short.valueOf((short)0);

        assertEquals(copy.intVal, Integer.valueOf(42), "intVal independence");
        assertEquals(copy.longVal, Long.valueOf(100L), "longVal independence");
        assertEquals(copy.doubleVal, Double.valueOf(3.14), "doubleVal independence");
        assertEquals(copy.shortVal, Short.valueOf((short)7), "shortVal independence");
    }

    // Clone with null value class fields
    static void testNullValueFields() throws Exception {
        Container orig = new Container(null, null, null, null);
        Container copy = orig.clone();

        if (copy.intVal != null) throw new RuntimeException("intVal should be null");
        if (copy.longVal != null) throw new RuntimeException("longVal should be null");
        if (copy.doubleVal != null) throw new RuntimeException("doubleVal should be null");
        if (copy.shortVal != null) throw new RuntimeException("shortVal should be null");
    }

    // Clone with value fields at different inheritance levels
    static void testInheritedValueFields() throws Exception {
        Child orig = new Child(Integer.valueOf(1), Long.valueOf(2L),
                               Float.valueOf(3.0f), Byte.valueOf((byte)4));
        Child copy = orig.clone();

        assertEquals(orig.intVal, copy.intVal, "parent intVal");
        assertEquals(orig.longVal, copy.longVal, "parent longVal");
        assertEquals(orig.floatVal, copy.floatVal, "child floatVal");
        assertEquals(orig.byteVal, copy.byteVal, "child byteVal");

        orig.intVal = Integer.valueOf(99);
        orig.floatVal = Float.valueOf(99.0f);
        assertEquals(copy.intVal, Integer.valueOf(1), "parent intVal independence");
        assertEquals(copy.floatVal, Float.valueOf(3.0f), "child floatVal independence");
    }

    // Clone arrays of value class elements
    static void testValueArrayClone() {
        Integer[] intArr = new Integer[]{1, 2, 3, 4, 5};
        Integer[] intCopy = intArr.clone();
        assertArrayIndependent(intArr, intCopy, "Integer array");

        Long[] longArr = new Long[]{10L, 20L, 30L};
        Long[] longCopy = longArr.clone();
        assertArrayIndependent(longArr, longCopy, "Long array");

        Double[] dblArr = new Double[]{1.1, 2.2, 3.3};
        Double[] dblCopy = dblArr.clone();
        assertArrayIndependent(dblArr, dblCopy, "Double array");
    }

    // Clone array containing mixed value and identity objects
    static void testMixedArrayClone() {
        Object[] orig = new Object[]{Integer.valueOf(1), Long.valueOf(2L),
                                     "three", new Object(), null};
        Object[] copy = orig.clone();

        if (orig == copy)
            throw new RuntimeException("Mixed array clone returned same reference");
        if (orig.length != copy.length)
            throw new RuntimeException("Mixed array length mismatch");
        for (int i = 0; i < orig.length; i++) {
            if (orig[i] == null) {
                if (copy[i] != null)
                    throw new RuntimeException("Mixed array null mismatch at " + i);
            } else if (orig[i] instanceof Number) {
                if (!orig[i].equals(copy[i]))
                    throw new RuntimeException("Mixed array Number mismatch at " + i);
            } else if (orig[i] instanceof String) {
                if (!orig[i].equals(copy[i]))
                    throw new RuntimeException("Mixed array String mismatch at " + i);
            } else {
                if (orig[i] != copy[i])
                    throw new RuntimeException("Mixed array Object identity mismatch at " + i);
            }
        }
    }

    // Cloned objects with value fields survive GC
    static void testCloneGCSurvival() throws Exception {
        Container[] copies = new Container[1000];
        for (int i = 0; i < copies.length; i++) {
            Container orig = new Container(Integer.valueOf(i), Long.valueOf(i * 10L),
                                           Double.valueOf(i * 1.1), Short.valueOf((short)(i % 100)));
            copies[i] = orig.clone();
        }

        System.gc();

        for (int i = 0; i < copies.length; i++) {
            assertEquals(copies[i].intVal, Integer.valueOf(i), "gc intVal " + i);
            assertEquals(copies[i].longVal, Long.valueOf(i * 10L), "gc longVal " + i);
            assertEquals(copies[i].doubleVal, Double.valueOf(i * 1.1), "gc doubleVal " + i);
            assertEquals(copies[i].shortVal, Short.valueOf((short)(i % 100)), "gc shortVal " + i);
        }
    }

    static void assertEquals(Object actual, Object expected, String msg) {
        if (!java.util.Objects.equals(actual, expected)) {
            throw new RuntimeException(msg + ": expected " + expected + " got " + actual);
        }
    }

    static void assertArrayIndependent(Object[] orig, Object[] copy, String name) {
        if (orig == copy)
            throw new RuntimeException(name + " clone returned same reference");
        if (orig.length != copy.length)
            throw new RuntimeException(name + " length mismatch");
        for (int i = 0; i < orig.length; i++) {
            if (!orig[i].equals(copy[i]))
                throw new RuntimeException(name + " element " + i + " mismatch");
        }
        Object saved = copy[0];
        orig[0] = null;
        if (copy[0] == null)
            throw new RuntimeException(name + " clone not independent");
        orig[0] = saved;
    }

    static class Container implements Cloneable {
        Integer intVal;
        Long longVal;
        Double doubleVal;
        Short shortVal;

        Container(Integer i, Long l, Double d, Short s) {
            this.intVal = i;
            this.longVal = l;
            this.doubleVal = d;
            this.shortVal = s;
        }

        public Container clone() throws CloneNotSupportedException {
            return (Container) super.clone();
        }
    }

    static class Parent implements Cloneable {
        Integer intVal;
        Long longVal;

        Parent(Integer i, Long l) {
            this.intVal = i;
            this.longVal = l;
        }

        public Parent clone() throws CloneNotSupportedException {
            return (Parent) super.clone();
        }
    }

    static class Child extends Parent {
        Float floatVal;
        Byte byteVal;

        Child(Integer i, Long l, Float f, Byte b) {
            super(i, l);
            this.floatVal = f;
            this.byteVal = b;
        }

        public Child clone() throws CloneNotSupportedException {
            return (Child) super.clone();
        }
    }
}
