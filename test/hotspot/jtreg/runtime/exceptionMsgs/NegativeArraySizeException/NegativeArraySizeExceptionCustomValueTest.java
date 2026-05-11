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
/**
 * @test
 * @bug 8384109
 * @summary Test that NegativeArraySizeException reports the wrong size for custom value class arrays.
 * @library /test/lib
 * @enablePreview
 * @run main NegativeArraySizeExceptionCustomValueTest
 */
import java.lang.reflect.Array;
import jdk.test.lib.Asserts;
public class NegativeArraySizeExceptionCustomValueTest {
    static value class Point {
        int x;
        int y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
    private static void fail() throws Exception {
        throw new RuntimeException("Array allocation with negative size expected to fail!");
    }
    public static void main(String[] args) throws Exception {
        int minusOne = -1;
        Object r = null;
        // Custom value class array with negative size.
        try {
            r = new Point[minusOne];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }
        try {
            r = new Point[Integer.MIN_VALUE];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }
        // Multidimensional, inner array wrong size.
        try {
            r = new Point[3][minusOne];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }
        try {
            r = new Point[3][Integer.MIN_VALUE];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }
        // Multidimensional, outer array wrong size.
        try {
            r = new Point[minusOne][3];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }
        try {
            r = new Point[Integer.MIN_VALUE][3];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }
        // Multidimensional, inner wrong, outer zero.
        try {
            r = new Point[0][minusOne];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }
        try {
            r = new Point[0][Integer.MIN_VALUE];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }
        // Reflection with custom value class.
        try {
            Array.newInstance(Point.class, minusOne);
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }
        try {
            Array.newInstance(Point.class, Integer.MIN_VALUE);
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }
        // Reflection multi-dimensional with custom value class.
        try {
            Array.newInstance(Point.class, new int[] {3, minusOne});
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }
        try {
            Array.newInstance(Point.class, new int[] {minusOne, 3});
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }
        Asserts.assertEQ(r, null, "Expected all tries to allocate negative array to fail.");
    }
}
