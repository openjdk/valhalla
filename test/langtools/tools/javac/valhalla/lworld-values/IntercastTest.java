/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8217875
 * @summary [lworld] Javac does not allow express casts between value types and their light weight box types
 * @run main/othervm -Xverify:none -XX:+EnableValhalla  IntercastTest
 */

public value class IntercastTest {

    int ARRAY[] = { 10, 20, 30 };

    static value class Tuple {
        private final int index;
        private final int element;

        private Tuple(int index, int element) {
            this.index = index;
            this.element = element;
        }
    }

    static value class Cursor {
        private final int[] array;
        private final int index;

        private Cursor(int[] array, int index) {
            this.array = array;
            this.index = index;
        }

        Tuple current() {
            return new Tuple(index, array[index]);
        }

        Cursor.box next() {
            if (index + 1 == array.length) {
                return null;
            }
            return new Cursor(array, index + 1);
        }
    }

    private static Cursor.box indexedElements(int[] array) {
        if (array.length == 0) {
            return null;
        }
        return new Cursor(array, 0);
    }

    public int sum() {
        int sum = 0;
        for (Cursor.box cursor = indexedElements(ARRAY); cursor != null; cursor = cursor.next()) {
            Tuple tuple = cursor.current();
            sum += tuple.index + tuple.element;
        }
        return sum;
    }

    public static void main(String [] args) {
        IntercastTest x = new IntercastTest();
        if (x.sum() != 63 || x.ARRAY.length != 3) {
            throw new AssertionError("Broken");
        }
        IntercastTest.box xbox = (IntercastTest.box) x;
        if (xbox.sum() != 63 || xbox.ARRAY.length != 3) {
            throw new AssertionError("Broken");
        }
        x = (IntercastTest) xbox;
        if (x.sum() != 63 || x.ARRAY.length != 3) {
            throw new AssertionError("Broken");
        }
    }
}
