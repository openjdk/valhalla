/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.lang.runtime.ArrayCreation;

/**
 * @test
 * @summary java.lang.runtime.ArrayCreation factories and bootstraps
 * @compile ArrayCreationInvoker.jasm
 * @run main ArrayCreationTest
 */
public class ArrayCreationTest {

    private static final int PLAIN = 0;
    private static final int CHECKED = 0x0200;

    public static void main(String... args) {

        checkArray(String.class, PLAIN, List.of(),
                   ArrayCreation.empty(String.class, PLAIN),
                   ArrayCreationInvoker.emptyS());
        checkArray(int.class, PLAIN, List.of(),
                   ArrayCreation.empty(int.class, PLAIN),
                   ArrayCreationInvoker.emptyI());
        checkArray(String.class, CHECKED, List.of(),
                   ArrayCreation.empty(String.class, CHECKED),
                   ArrayCreationInvoker.emptyS_N());

        checkArray(String.class, PLAIN, List.of("c", "c", "c"),
                   ArrayCreation.filled(String.class, PLAIN, 3, "c"),
                   ArrayCreationInvoker.constantFilled3S());
        checkArray(int.class, PLAIN, List.of(3, 3, 3),
                   ArrayCreation.filled(int.class, PLAIN, 3, 3),
                   ArrayCreationInvoker.constantFilled3I());
        checkArray(String.class, PLAIN, List.of(),
                   ArrayCreation.filled(String.class, PLAIN, 0, "c"),
                   ArrayCreationInvoker.constantFilled0S());
        checkArray(int.class, PLAIN, List.of(),
                   ArrayCreation.filled(int.class, PLAIN, 0, 3),
                   ArrayCreationInvoker.constantFilled0I());
        checkArray(String.class, PLAIN, List.of("c", "c", "c", "c", "c"),
                   ArrayCreation.filled(String.class, PLAIN, 5, "c"),
                   ArrayCreationInvoker.constantFilledNS(5));
        checkArray(String.class, PLAIN, List.of(),
                   ArrayCreation.filled(String.class, PLAIN, 0, "c"),
                   ArrayCreationInvoker.constantFilledNS(0));
        checkArray(int.class, PLAIN, List.of(3, 3, 3, 3, 3),
                   ArrayCreation.filled(int.class, PLAIN, 5, 3),
                   ArrayCreationInvoker.constantFilledNI(5));
        checkArray(int.class, PLAIN, List.of(),
                   ArrayCreation.filled(int.class, PLAIN, 0, 3),
                   ArrayCreationInvoker.constantFilledNI(0));
        checkArray(String.class, CHECKED, List.of("c", "c", "c"),
                   ArrayCreation.filled(String.class, CHECKED, 3, "c"),
                   ArrayCreationInvoker.constantFilled3S_N());
        checkArray(String.class, CHECKED, List.of(),
                   ArrayCreation.filled(String.class, CHECKED, 0, "c"),
                   ArrayCreationInvoker.constantFilled0S_N());
        checkArray(String.class, CHECKED, List.of("c", "c", "c", "c", "c"),
                   ArrayCreation.filled(String.class, CHECKED, 5, "c"),
                   ArrayCreationInvoker.constantFilledNS_N(5));
        checkArray(String.class, CHECKED, List.of(),
                   ArrayCreation.filled(String.class, CHECKED, 0, "c"),
                   ArrayCreationInvoker.constantFilledNS_N(0));

        checkArray(String.class, PLAIN, List.of("hi", "hi", "hi"),
                   ArrayCreation.filled(String.class, PLAIN, 3, "hi"),
                   ArrayCreationInvoker.dynamicFilled3S("hi"));
        checkArray(int.class, PLAIN, List.of(23, 23, 23),
                   ArrayCreation.filled(int.class, PLAIN, 3, 23),
                   ArrayCreationInvoker.dynamicFilled3I(23));
        checkArray(String.class, PLAIN, List.of(),
                   ArrayCreation.filled(String.class, PLAIN, 0, "hi"),
                   ArrayCreationInvoker.dynamicFilled0S("hi"));
        checkArray(int.class, PLAIN, List.of(),
                   ArrayCreation.filled(int.class, PLAIN, 0, 23),
                   ArrayCreationInvoker.dynamicFilled0I(23));
        checkArray(String.class, PLAIN, List.of("hi", "hi"),
                   ArrayCreation.filled(String.class, PLAIN, 2, "hi"),
                   ArrayCreationInvoker.dynamicFilledNS(2, "hi"));
        checkArray(String.class, PLAIN, List.of(),
                   ArrayCreation.filled(String.class, PLAIN, 0, "hi"),
                   ArrayCreationInvoker.dynamicFilledNS(0, "hi"));
        checkArray(int.class, PLAIN, List.of(23, 23),
                   ArrayCreation.filled(int.class, PLAIN, 2, 23),
                   ArrayCreationInvoker.dynamicFilledNI(2, 23));
        checkArray(int.class, PLAIN, List.of(),
                   ArrayCreation.filled(int.class, PLAIN, 0, 23),
                   ArrayCreationInvoker.dynamicFilledNI(0, 23));
        checkArray(String.class, CHECKED, List.of("hi", "hi", "hi"),
                   ArrayCreation.filled(String.class, CHECKED, 3, "hi"),
                   ArrayCreationInvoker.dynamicFilled3S_N("hi"));
        checkArray(String.class, CHECKED, List.of(),
                   ArrayCreation.filled(String.class, CHECKED, 0, "hi"),
                   ArrayCreationInvoker.dynamicFilled0S_N("hi"));
        checkArray(String.class, CHECKED, List.of("hi", "hi"),
                   ArrayCreation.filled(String.class, CHECKED, 2, "hi"),
                   ArrayCreationInvoker.dynamicFilledNS_N(2, "hi"));
        checkArray(String.class, CHECKED, List.of(),
                   ArrayCreation.filled(String.class, CHECKED, 0, "hi"),
                   ArrayCreationInvoker.dynamicFilledNS_N(0, "hi"));

        checkArray(String.class, PLAIN, List.of("x0", "x1", "x2"),
                   ArrayCreation.computed(String.class, PLAIN, 3, i -> "x"+i),
                   ArrayCreationInvoker.computed3S(i -> "x"+i));
        checkArray(int.class, PLAIN, List.of(5, 6, 7),
                   ArrayCreation.computed(int.class, PLAIN, 3, i -> i+5),
                   ArrayCreationInvoker.computed3I(i -> i+5));
        checkArray(String.class, PLAIN, List.of(),
                   ArrayCreation.computed(String.class, PLAIN, 0, i -> "y"+i),
                   ArrayCreationInvoker.computed0S(i -> "y"+i));
        checkArray(int.class, PLAIN, List.of(),
                   ArrayCreation.computed(int.class, PLAIN, 0, i -> i+6),
                   ArrayCreationInvoker.computed0I(i -> i+6));
        checkArray(String.class, PLAIN, List.of("z0", "z1", "z2", "z3", "z4"),
                   ArrayCreation.computed(String.class, PLAIN, 5, i -> "z"+i),
                   ArrayCreationInvoker.computedNS(5, i -> "z"+i));
        checkArray(String.class, PLAIN, List.of(),
                   ArrayCreation.computed(String.class, PLAIN, 0, i -> "w"+i),
                   ArrayCreationInvoker.computedNS(0, i -> "w"+i));
        checkArray(int.class, PLAIN, List.of(7, 8, 9, 10, 11),
                   ArrayCreation.computed(int.class, PLAIN, 5, i -> i+7),
                   ArrayCreationInvoker.computedNI(5, i -> i+7));
        checkArray(int.class, PLAIN, List.of(),
                   ArrayCreation.computed(int.class, PLAIN, 0, i -> i+8),
                   ArrayCreationInvoker.computedNI(0, i -> i+8));
        checkArray(String.class, CHECKED, List.of("x0", "x1", "x2"),
                   ArrayCreation.computed(String.class, CHECKED, 3, i -> "x"+i),
                   ArrayCreationInvoker.computed3S_N(i -> "x"+i));
        checkArray(String.class, CHECKED, List.of(),
                   ArrayCreation.computed(String.class, CHECKED, 0, i -> "y"+i),
                   ArrayCreationInvoker.computed0S_N(i -> "y"+i));
        checkArray(String.class, CHECKED, List.of("z0", "z1", "z2", "z3", "z4"),
                   ArrayCreation.computed(String.class, CHECKED, 5, i -> "z"+i),
                   ArrayCreationInvoker.computedNS_N(5, i -> "z"+i));
        checkArray(String.class, CHECKED, List.of(),
                   ArrayCreation.computed(String.class, CHECKED, 0, i -> "w"+i),
                   ArrayCreationInvoker.computedNS_N(0, i -> "w"+i));

        Object[] srcS = new Object[]{ "a", "b", "c", "d", "e" };
        int[] srcI = new int[]{ 1, 2, 3, 4, 5 };

        checkArray(String.class, PLAIN, List.of("a", "b", "c"),
                   ArrayCreation.copied(String.class, PLAIN, 3, srcS),
                   ArrayCreationInvoker.copied3S(srcS));
        checkArray(int.class, PLAIN, List.of(1, 2, 3),
                   ArrayCreation.copied(int.class, PLAIN, 3, srcI),
                   ArrayCreationInvoker.copied3I(srcI));
        checkArray(String.class, PLAIN, List.of(),
                   ArrayCreation.copied(String.class, PLAIN, 0, srcS),
                   ArrayCreationInvoker.copied0S(srcS));
        checkArray(int.class, PLAIN, List.of(),
                   ArrayCreation.copied(int.class, PLAIN, 0, srcI),
                   ArrayCreationInvoker.copied0I(srcI));
        checkArray(String.class, PLAIN, List.of("a", "b", "c", "d"),
                   ArrayCreation.copied(String.class, PLAIN, 4, srcS),
                   ArrayCreationInvoker.copiedNS(4, srcS));
        checkArray(String.class, PLAIN, List.of(),
                   ArrayCreation.copied(String.class, PLAIN, 0, srcS),
                   ArrayCreationInvoker.copiedNS(0, srcS));
        checkArray(int.class, PLAIN, List.of(1, 2, 3, 4),
                   ArrayCreation.copied(int.class, PLAIN, 4, srcI),
                   ArrayCreationInvoker.copiedNI(4, srcI));
        checkArray(int.class, PLAIN, List.of(),
                   ArrayCreation.copied(int.class, PLAIN, 0, srcI),
                   ArrayCreationInvoker.copiedNI(0, srcI));
        checkArray(String.class, CHECKED, List.of("a", "b", "c"),
                   ArrayCreation.copied(String.class, CHECKED, 3, srcS),
                   ArrayCreationInvoker.copied3S_N(srcS));
        checkArray(String.class, CHECKED, List.of(),
                   ArrayCreation.copied(String.class, CHECKED, 0, srcS),
                   ArrayCreationInvoker.copied0S_N(srcS));
        checkArray(String.class, CHECKED, List.of("a", "b", "c", "d"),
                   ArrayCreation.copied(String.class, CHECKED, 4, srcS),
                   ArrayCreationInvoker.copiedNS_N(4, srcS));
        checkArray(String.class, CHECKED, List.of(),
                   ArrayCreation.copied(String.class, CHECKED, 0, srcS),
                   ArrayCreationInvoker.copiedNS_N(0, srcS));

        checkArray(String.class, PLAIN, List.of("a", "b", "c"),
                   ArrayCreation.offsetCopied(String.class, PLAIN, 3, srcS, 0),
                   ArrayCreationInvoker.offsetCopied3S(srcS, 0));
        checkArray(String.class, PLAIN, List.of("c", "d", "e"),
                   ArrayCreation.offsetCopied(String.class, PLAIN, 3, srcS, 2),
                   ArrayCreationInvoker.offsetCopied3S(srcS, 2));
        checkArray(int.class, PLAIN, List.of(1, 2, 3),
                   ArrayCreation.offsetCopied(int.class, PLAIN, 3, srcI, 0),
                   ArrayCreationInvoker.offsetCopied3I(srcI, 0));
        checkArray(int.class, PLAIN, List.of(3, 4, 5),
                   ArrayCreation.offsetCopied(int.class, PLAIN, 3, srcI, 2),
                   ArrayCreationInvoker.offsetCopied3I(srcI, 2));
        checkArray(String.class, PLAIN, List.of(),
                   ArrayCreation.offsetCopied(String.class, PLAIN, 0, srcS, 0),
                   ArrayCreationInvoker.offsetCopied0S(srcS, 0));
        checkArray(String.class, PLAIN, List.of(),
                   ArrayCreation.offsetCopied(String.class, PLAIN, 0, srcS, 2),
                   ArrayCreationInvoker.offsetCopied0S(srcS, 2));
        checkArray(int.class, PLAIN, List.of(),
                   ArrayCreation.offsetCopied(int.class, PLAIN, 0, srcI, 0),
                   ArrayCreationInvoker.offsetCopied0I(srcI, 0));
        checkArray(int.class, PLAIN, List.of(),
                   ArrayCreation.offsetCopied(int.class, PLAIN, 0, srcI, 2),
                   ArrayCreationInvoker.offsetCopied0I(srcI, 2));
        checkArray(String.class, PLAIN, List.of("b", "c", "d", "e"),
                   ArrayCreation.offsetCopied(String.class, PLAIN, 4, srcS, 1),
                   ArrayCreationInvoker.offsetCopiedNS(4, srcS, 1));
        checkArray(String.class, PLAIN, List.of(),
                   ArrayCreation.offsetCopied(String.class, PLAIN, 0, srcS, 4),
                   ArrayCreationInvoker.offsetCopiedNS(0, srcS, 4));
        checkArray(int.class, PLAIN, List.of(2, 3, 4, 5),
                   ArrayCreation.offsetCopied(int.class, PLAIN, 4, srcI, 1),
                   ArrayCreationInvoker.offsetCopiedNI(4, srcI, 1));
        checkArray(int.class, PLAIN, List.of(),
                   ArrayCreation.offsetCopied(int.class, PLAIN, 0, srcI, 4),
                   ArrayCreationInvoker.offsetCopiedNI(0, srcI, 4));
        checkArray(String.class, CHECKED, List.of("a", "b", "c"),
                   ArrayCreation.offsetCopied(String.class, CHECKED, 3, srcS, 0),
                   ArrayCreationInvoker.offsetCopied3S_N(srcS, 0));
        checkArray(String.class, CHECKED, List.of("c", "d", "e"),
                   ArrayCreation.offsetCopied(String.class, CHECKED, 3, srcS, 2),
                   ArrayCreationInvoker.offsetCopied3S_N(srcS, 2));
        checkArray(String.class, CHECKED, List.of(),
                   ArrayCreation.offsetCopied(String.class, CHECKED, 0, srcS, 0),
                   ArrayCreationInvoker.offsetCopied0S_N(srcS, 0));
        checkArray(String.class, CHECKED, List.of(),
                   ArrayCreation.offsetCopied(String.class, CHECKED, 0, srcS, 2),
                   ArrayCreationInvoker.offsetCopied0S_N(srcS, 2));
        checkArray(String.class, CHECKED, List.of("b", "c", "d", "e"),
                   ArrayCreation.offsetCopied(String.class, CHECKED, 4, srcS, 1),
                   ArrayCreationInvoker.offsetCopiedNS_N(4, srcS, 1));
        checkArray(String.class, CHECKED, List.of(),
                   ArrayCreation.offsetCopied(String.class, CHECKED, 0, srcS, 4),
                   ArrayCreationInvoker.offsetCopiedNS_N(0, srcS, 4));

        checkArray(String.class, PLAIN, List.of("c1", "c2", "c3"),
                   ArrayCreation.enumerated(String.class, PLAIN, "c1", "c2", "c3"),
                   ArrayCreationInvoker.constantEnumerated3S());
        checkArray(int.class, PLAIN, List.of(30, 40, 50),
                   ArrayCreation.enumerated(int.class, PLAIN, 30, 40, 50),
                   ArrayCreationInvoker.constantEnumerated3I());
        checkArray(String.class, PLAIN, List.of(),
                   ArrayCreation.empty(String.class, PLAIN),
                   ArrayCreationInvoker.constantEnumerated0S());
        checkArray(int.class, PLAIN, List.of(),
                   ArrayCreation.empty(int.class, PLAIN),
                   ArrayCreationInvoker.constantEnumerated0I());
        checkArray(String.class, CHECKED, List.of("c1", "c2", "c3"),
                   ArrayCreation.enumerated(String.class, CHECKED, "c1", "c2", "c3"),
                   ArrayCreationInvoker.constantEnumerated3S_N());
        checkArray(String.class, CHECKED, List.of(),
                   ArrayCreation.empty(String.class, CHECKED),
                   ArrayCreationInvoker.constantEnumerated0S_N());

        checkArray(String.class, PLAIN, List.of("x", "y", "z"),
                   ArrayCreation.enumerated(String.class, PLAIN, "x", "y", "z"),
                   ArrayCreationInvoker.dynamicEnumerated3S("x", "y", "z"));
        checkArray(int.class, PLAIN, List.of(-12, 0, 583),
                   ArrayCreation.enumerated(int.class, PLAIN, -12, 0, 583),
                   ArrayCreationInvoker.dynamicEnumerated3I(-12, 0, 583));
        checkArray(String.class, PLAIN, List.of(),
                   ArrayCreation.empty(String.class, PLAIN),
                   ArrayCreationInvoker.dynamicEnumerated0S());
        checkArray(int.class, PLAIN, List.of(),
                   ArrayCreation.empty(int.class, PLAIN),
                   ArrayCreationInvoker.dynamicEnumerated0I());
        checkArray(String.class, CHECKED, List.of("x", "y", "z"),
                   ArrayCreation.enumerated(String.class, CHECKED, "x", "y", "z"),
                   ArrayCreationInvoker.dynamicEnumerated3S_N("x", "y", "z"));
        checkArray(String.class, PLAIN, List.of(),
                   ArrayCreation.empty(String.class, CHECKED),
                   ArrayCreationInvoker.dynamicEnumerated0S_N());
    }

    private static void checkArray(Class<?> comp, int mods, List<?> expected, Object... actual) {
        for (Object o : actual) {
            if (!(o.getClass().isArray()) ||
                !o.getClass().getComponentType().equals(comp) ||
                // TODO: check modifiers once implemented
                !arrayToList(o).equals(expected)) { 
                throw new AssertionError("Unexpected result: %s, expected %s".formatted(
                                        arrayString(o), arrayString(comp, mods, expected)));
            }
        }
    }

    private static List<?> arrayToList(Object arr) {
        return switch (arr) {
            case Object[] a -> Arrays.asList(a);
            case int[] a -> IntStream.range(0, a.length).mapToObj(i -> a[i]).toList();
            case long[] a -> IntStream.range(0, a.length).mapToObj(i -> a[i]).toList();
            case float[] a -> IntStream.range(0, a.length).mapToObj(i -> a[i]).toList();
            case double[] a -> IntStream.range(0, a.length).mapToObj(i -> a[i]).toList();
            case byte[] a -> IntStream.range(0, a.length).mapToObj(i -> a[i]).toList();
            case short[] a -> IntStream.range(0, a.length).mapToObj(i -> a[i]).toList();
            case char[] a -> IntStream.range(0, a.length).mapToObj(i -> a[i]).toList();
            case boolean[] a -> IntStream.range(0, a.length).mapToObj(i -> a[i]).toList();
            default -> throw new AssertionError();
        };
    }

    private static String arrayString(Object o) {
        if (o.getClass().isArray()) {
            var comp = o.getClass().getComponentType();
            var mods = 0; // TODO
            return arrayString(comp, mods, arrayToList(o));
        } else {
            return "object " + o.toString();
        }
    }

    private static String arrayString(Class<?> comp, int mods, List<?> contents) {
        return "%s:%04x%s".formatted(comp.getName(), mods, contents);
    }

}
