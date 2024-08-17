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

        checkArray(ArrayCreationInvoker.constantFilled3(),
                   new String[]{ "c", "c", "c" });
        checkArray(ArrayCreationInvoker.constantFilled0(),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.constantFilledN(5),
                   new String[]{ "c", "c", "c", "c", "c" });

        checkArray(ArrayCreationInvoker.dynamicFilled3("hi"),
                   new String[]{ "hi", "hi", "hi" });
        checkArray(ArrayCreationInvoker.dynamicFilled0("hi"),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.dynamicFilledN(2, "hi"),
                   new String[]{ "hi", "hi" });
        checkArray(ArrayCreationInvoker.dynamicFilledN(0, "hi"),
                   new String[]{ });

        checkArray(ArrayCreationInvoker.computed3(i -> "x"+i),
                   new String[]{ "x0", "x1", "x2" });
        checkArray(ArrayCreationInvoker.computed0(i -> "y"+i),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.computedN(5, i -> "z"+i),
                   new String[]{ "z0", "z1", "z2", "z3", "z4" });
        checkArray(ArrayCreationInvoker.computedN(0, i -> "w"+i),
                   new String[]{ });

        Object[] src = new Object[]{ "a", "b", "c", "d", "e" };

        checkArray(ArrayCreationInvoker.copied3(src),
                   new String[]{ "a", "b", "c" });
        checkArray(ArrayCreationInvoker.copied0(src),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.copiedN(4, src),
                   new String[]{ "a", "b", "c", "d" });
        checkArray(ArrayCreationInvoker.copiedN(0, src),
                   new String[]{ });

        checkArray(ArrayCreationInvoker.offsetCopied3(src, 0),
                   new String[]{ "a", "b", "c" });
        checkArray(ArrayCreationInvoker.offsetCopied3(src, 2),
                   new String[]{ "c", "d", "e" });
        checkArray(ArrayCreationInvoker.offsetCopied0(src, 0),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.offsetCopied0(src, 2),
                   new String[]{ });
        checkArray(ArrayCreationInvoker.offsetCopiedN(4, src, 1),
                   new String[]{ "b", "c", "d", "e" });
        checkArray(ArrayCreationInvoker.offsetCopiedN(0, src, 4),
                   new String[]{ });

        checkArray(ArrayCreationInvoker.constantEnumerated3(),
                   new String[]{ "c1", "c2", "c3" });
        checkArray(ArrayCreationInvoker.constantEnumerated0(),
                   new String[]{ });

        checkArray(ArrayCreationInvoker.dynamicEnumerated3("x", "y", "z"),
                   new String[]{ "x", "y", "z" });
        checkArray(ArrayCreationInvoker.dynamicEnumerated0(),
                   new String[]{ });
    }

    private static void checkArray(Object[] arr, Object[] expected) {
        if (!Arrays.equals(arr, expected)) {
            throw new AssertionError("Unexpected result: %s, expected %s".formatted(
                                     Arrays.toString(arr), Arrays.toString(expected)));
        }
    }
}
