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
 * @summary Test that NegativeArraySizeException reports the wrong size for value class arrays.
 * @library /test/lib
 * @compile NegativeArraySizeExceptionValueTest.java
 * @run main NegativeArraySizeExceptionValueTest
 */
import java.lang.reflect.Array;
import jdk.test.lib.Asserts;
public class NegativeArraySizeExceptionValueTest {
    private static void fail() throws Exception {
        throw new RuntimeException("Array allocation with negative size expected to fail!");
    }
    public static void main(String[] args) throws Exception {
        int minusOne = -1;
        Object r = null;
        // Value class array allocation with negative size.
        try {
            r = new Integer[minusOne];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }
        try {
            r = new Integer[Integer.MIN_VALUE];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }
        // Multidimensional value class array, inner array has wrong size.
        try {
            r = new Integer[3][minusOne];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }
        try {
            r = new Integer[3][Integer.MIN_VALUE];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }
        // Multidimensional value class array, outer array has wrong size.
        try {
            r = new Integer[minusOne][3];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }
        try {
            r = new Integer[Integer.MIN_VALUE][3];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }
        // Multidimensional value class array, inner array wrong size, outer size 0.
        try {
            r = new Integer[0][minusOne];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }
        try {
            r = new Integer[0][Integer.MIN_VALUE];
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }
        // Reflection: Array.newInstance with value class type.
        try {
            Array.newInstance(Integer.class, minusOne);
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }
        try {
            Array.newInstance(Integer.class, Integer.MIN_VALUE);
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }
        // Reflection: multi-dimensional with value class type.
        try {
            Array.newInstance(Integer.class, new int[] {3, minusOne});
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }
        try {
            Array.newInstance(Integer.class, new int[] {3, Integer.MIN_VALUE});
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }
        try {
            Array.newInstance(Integer.class, new int[] {0, minusOne});
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }
        try {
            Array.newInstance(Integer.class, new int[] {0, Integer.MIN_VALUE});
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }
        try {
            Array.newInstance(Integer.class, new int[] {minusOne, 3});
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-1", e.getMessage());
        }
        try {
            Array.newInstance(Integer.class, new int[] {Integer.MIN_VALUE, 3});
            fail();
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ("-2147483648", e.getMessage());
        }
        Asserts.assertEQ(r, null, "Expected all tries to allocate negative array to fail.");
    }
}
