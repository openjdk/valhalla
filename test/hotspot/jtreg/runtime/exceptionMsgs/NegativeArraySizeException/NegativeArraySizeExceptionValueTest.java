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
 * @summary Test NegativeArraySizeException message for value class arrays (migrated and custom).
 * @library /test/lib
 * @enablePreview
 * @run main NegativeArraySizeExceptionValueTest
 */
import java.lang.reflect.Array;
import jdk.test.lib.Asserts;
public class NegativeArraySizeExceptionValueTest {

    static value class Point {
        int x;
        int y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    @FunctionalInterface
    interface AllocAction {
        void run() throws Exception;
    }

    private static void assertNASE(int size, AllocAction action) throws Exception {
        try {
            action.run();
            throw new RuntimeException("Array allocation with negative size expected to fail!");
        } catch (NegativeArraySizeException e) {
            Asserts.assertEQ(Integer.toString(size), e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        int[] sizes = { -1, Integer.MIN_VALUE };
        Class<?>[] types = { Integer.class, Point.class };

        for (int size : sizes) {
            for (Class<?> type : types) {
                // Direct allocation via reflection
                assertNASE(size, () -> Array.newInstance(type, size));

                // Multi-dimensional: inner wrong
                assertNASE(size, () -> Array.newInstance(type, new int[] {3, size}));

                // Multi-dimensional: outer wrong
                assertNASE(size, () -> Array.newInstance(type, new int[] {size, 3}));

                // Multi-dimensional: inner wrong, outer zero
                assertNASE(size, () -> Array.newInstance(type, new int[] {0, size}));
            }
        }

        // Direct new[] expressions — Integer
        for (int size : sizes) {
            assertNASE(size, () -> { Object r = new Integer[size]; });
            assertNASE(size, () -> { Object r = new Integer[3][size]; });
            assertNASE(size, () -> { Object r = new Integer[size][3]; });
            assertNASE(size, () -> { Object r = new Integer[0][size]; });
        }

        // Direct new[] expressions — custom value class Point
        for (int size : sizes) {
            assertNASE(size, () -> { Object r = new Point[size]; });
            assertNASE(size, () -> { Object r = new Point[3][size]; });
            assertNASE(size, () -> { Object r = new Point[size][3]; });
            assertNASE(size, () -> { Object r = new Point[0][size]; });
        }
    }
}
