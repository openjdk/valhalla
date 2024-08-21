/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

/**
 * @test
 * @summary invokedynamic usage of java.lang.runtime.ArrayCreation bootstraps
 * @compile ArrayCreationInvoker.jasm
 * @run main ArrayCreationTest
 */
public class ArrayCreationTest {

    public static void main(String... args) {

        checkArray(ArrayCreationInvoker.constantFilled3S(),
                   new String[]{ "c", "c", "c" });
        checkArray(ArrayCreationInvoker.constantFilled3I(),
                   new int[]{ 3, 3, 3 });
        checkArray(ArrayCreationInvoker.constantFilled0S(),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.constantFilled0I(),
                   new int[]{ });
        checkArray(ArrayCreationInvoker.constantFilledNS(5),
                   new String[]{ "c", "c", "c", "c", "c" });
        checkArray(ArrayCreationInvoker.constantFilledNS(0),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.constantFilledNI(5),
                   new int[]{ 3, 3, 3, 3, 3 });
        checkArray(ArrayCreationInvoker.constantFilledNI(0),
                   new int[]{ });

        checkArray(ArrayCreationInvoker.dynamicFilled3S("hi"),
                   new String[]{ "hi", "hi", "hi" });
        checkArray(ArrayCreationInvoker.dynamicFilled3I(23),
                   new int[]{ 23, 23, 23 });
        checkArray(ArrayCreationInvoker.dynamicFilled0S("hi"),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.dynamicFilled0I(23),
                   new int[]{ });
        checkArray(ArrayCreationInvoker.dynamicFilledNS(2, "hi"),
                   new String[]{ "hi", "hi" });
        checkArray(ArrayCreationInvoker.dynamicFilledNS(0, "hi"),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.dynamicFilledNI(2, 23),
                   new int[]{ 23, 23 });
        checkArray(ArrayCreationInvoker.dynamicFilledNI(0, 23),
                   new int[]{ });

        checkArray(ArrayCreationInvoker.computed3S(i -> "x"+i),
                   new String[]{ "x0", "x1", "x2" });
        checkArray(ArrayCreationInvoker.computed3I(i -> i+5),
                   new int[]{ 5, 6, 7 });
        checkArray(ArrayCreationInvoker.computed0S(i -> "y"+i),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.computed0I(i -> i+6),
                   new int[]{ });
        checkArray(ArrayCreationInvoker.computedNS(5, i -> "z"+i),
                   new String[]{ "z0", "z1", "z2", "z3", "z4" });
        checkArray(ArrayCreationInvoker.computedNS(0, i -> "w"+i),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.computedNI(5, i -> i+7),
                   new int[]{ 7, 8, 9, 10, 11 });
        checkArray(ArrayCreationInvoker.computedNI(0, i -> i+8),
                   new int[]{ });

        Object[] srcS = new Object[]{ "a", "b", "c", "d", "e" };
        int[] srcI = new int[]{ 1, 2, 3, 4, 5 };

        checkArray(ArrayCreationInvoker.copied3S(srcS),
                   new String[]{ "a", "b", "c" });
        checkArray(ArrayCreationInvoker.copied3I(srcI),
                   new int[]{ 1, 2, 3 });
        checkArray(ArrayCreationInvoker.copied0S(srcS),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.copied0I(srcI),
                   new int[]{ });
        checkArray(ArrayCreationInvoker.copiedNS(4, srcS),
                   new String[]{ "a", "b", "c", "d" });
        checkArray(ArrayCreationInvoker.copiedNS(0, srcS),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.copiedNI(4, srcI),
                   new int[]{ 1, 2, 3, 4 });
        checkArray(ArrayCreationInvoker.copiedNI(0, srcI),
                   new int[]{ });

        checkArray(ArrayCreationInvoker.offsetCopied3S(srcS, 0),
                   new String[]{ "a", "b", "c" });
        checkArray(ArrayCreationInvoker.offsetCopied3S(srcS, 2),
                   new String[]{ "c", "d", "e" });
        checkArray(ArrayCreationInvoker.offsetCopied3I(srcI, 0),
                   new int[]{ 1, 2, 3 });
        checkArray(ArrayCreationInvoker.offsetCopied3I(srcI, 2),
                   new int[]{ 3, 4, 5 });
        checkArray(ArrayCreationInvoker.offsetCopied0S(srcS, 0),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.offsetCopied0S(srcS, 2),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.offsetCopied0I(srcI, 0),
                   new int[]{ });
        checkArray(ArrayCreationInvoker.offsetCopied0I(srcI, 2),
                   new int[]{ });
        checkArray(ArrayCreationInvoker.offsetCopiedNS(4, srcS, 1),
                   new String[]{ "b", "c", "d", "e" });
        checkArray(ArrayCreationInvoker.offsetCopiedNS(0, srcS, 4),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.offsetCopiedNI(4, srcI, 1),
                   new int[]{ 2, 3, 4, 5 });
        checkArray(ArrayCreationInvoker.offsetCopiedNI(0, srcI, 4),
                   new int[]{ });

        checkArray(ArrayCreationInvoker.constantEnumerated3S(),
                   new String[]{ "c1", "c2", "c3" });
        checkArray(ArrayCreationInvoker.constantEnumerated3I(),
                   new int[]{ 30, 40, 50 });
        checkArray(ArrayCreationInvoker.constantEnumerated0S(),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.constantEnumerated0I(),
                   new int[]{ });

        checkArray(ArrayCreationInvoker.dynamicEnumerated3S("x", "y", "z"),
                   new String[]{ "x", "y", "z" });
        checkArray(ArrayCreationInvoker.dynamicEnumerated3I(-12, 0, 583),
                   new int[]{ -12, 0, 583 });
        checkArray(ArrayCreationInvoker.dynamicEnumerated0S(),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.dynamicEnumerated0I(),
                   new int[]{ });
    }

    private static void checkArray(Object[] arr, Object[] expected) {
        if (!arr.getClass().equals(expected.getClass()) || !Arrays.equals(arr, expected)) {
            throw new AssertionError("Unexpected result: %s, expected %s".formatted(
                                     Arrays.toString(arr), Arrays.toString(expected)));
        }
    }

    private static void checkArray(int[] arr, int[] expected) {
        if (!Arrays.equals(arr, expected)) {
            throw new AssertionError("Unexpected result: %s, expected %s".formatted(
                                     Arrays.toString(arr), Arrays.toString(expected)));
        }
    }

}
