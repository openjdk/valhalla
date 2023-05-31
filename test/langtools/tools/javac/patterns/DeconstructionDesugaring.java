/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8291769 8301858 8304694 8304883
 * @summary Verify more complex switches work properly
 * @compile DeconstructionDesugaring.java
 * @run main DeconstructionDesugaring
 */

import java.util.Objects;
import java.util.function.ToIntFunction;
public class DeconstructionDesugaring {

    public static void main(String... args) throws Throwable {
        new DeconstructionDesugaring().test();
    }

    private void test() {
        test(this::runCheckStatement);
        test(this::runCheckExpression);
        assertEquals(runCheckExpressionWithUnconditional(new R5(new R4(new Sub3()))), 3);
        assertEquals(runCheckExpressionWithUnconditional(new R5(new R4(null))), 3);
        assertEquals(runCheckExpressionWithUnconditional1(new R5(new R4(null))), 2);
        assertEquals(runCheckExpressionWithUnconditional1(new R5(null)), 3);
        assertEquals(runCheckExpressionWithUnconditionalAndParams(new R1(42)), 1);
        assertEquals(runCheckExpressionWithUnconditionalAndParams(new R1(new Object())), 2);
        assertEquals(switchNullable1(new R6(0, 0)), "int: 0, int: 0");
        assertEquals(switchNullable1(new R6(0L, 0)), "obj: 0, obj: 0");
        assertEquals(switchNullable2(new R6(0, 0)), "int: 0, int: 0");
        assertEquals(switchNullable2(new R6(0L, 0)), "obj: 0, int: 0");
        assertEquals(switchNullable2(new R6(0, 0L)), "int: 0, obj: 0");
        assertEquals(switchNullable2(new R6(0L, 0L)), "obj: 0, obj: 0");
        assertEquals(switchNullableNPE(new R6(1, 1)), "obj: 1, obj: 1");
        try {
            switchNullableNPE(new R6(null, 1));
            throw new AssertionError("Expected NPE, but got none.");
        } catch (NullPointerException ex) {
            //expected.
        }
        assertEquals(runFallThrough(new R7(1, 1)), 0);
        assertEquals(runFallThrough(new R7(0, 0)), 1);
    }

    private void test(ToIntFunction<Object> task) {
        assertEquals(1, task.applyAsInt(new R1(new R2(""))));
        assertEquals(2, task.applyAsInt(new R1(new R2(1))));
        assertEquals(3, task.applyAsInt(new R1(new R2(1.0))));
        assertEquals(-1, task.applyAsInt(new R1(new R2(null))));
        assertEquals(4, task.applyAsInt(new R1(new R2(new StringBuilder()))));
        assertEquals(5, task.applyAsInt(new R1(new R3(""))));
        assertEquals(6, task.applyAsInt(new R1(new R3(1))));
        assertEquals(7, task.applyAsInt(new R1(new R3(1.0))));
        assertEquals(8, task.applyAsInt(new R1(new R3(new StringBuilder()))));
        assertEquals(-1, task.applyAsInt(new R1(1.0f)));
        assertEquals(-1, task.applyAsInt("foo"));
    }

    private int runCheckStatement(Object o) {
        switch (o) {
            case R1(R2(String s)) -> { return 1; }
            case R1(R2(Integer i)) -> { return 2; }
            case R1(R2(Double d)) -> { return 3; }
            case R1(R2(CharSequence cs)) -> { return 4; }
            case R1(R3(String s)) -> { return 5; }
            case R1(R3(Integer i)) -> { return 6; }
            case R1(R3(Double f)) -> { return 7; }
            case R1(R3(CharSequence cs)) -> { return 8; }
            default -> { return -1; }
        }
    }

    private int runCheckExpression(Object o) {
        return switch (o) {
            case R1(R2(String s)) -> 1;
            case R1(R2(Integer i)) -> 2;
            case R1(R2(Double d)) -> 3;
            case R1(R2(CharSequence cs)) -> 4;
            case R1(R3(String s)) -> 5;
            case R1(R3(Integer i)) -> 6;
            case R1(R3(Double f)) -> 7;
            case R1(R3(CharSequence cs)) -> 8;
            default -> -1;
        };
    }

    private int runCheckExpressionWithUnconditional(R5 o) {
        return switch (o) {
            case R5(R4(Sub1 s)) -> 1;
            case R5(R4(Sub2 s)) -> 2;
            case R5(R4(Super s)) -> 3;
        };
    }

    private int runCheckExpressionWithUnconditional1(R5 o) {
        return switch (o) {
            case R5(R4(Sub1 s)) -> 1;
            case R5(R4(Super s)) -> 2;
            case R5(Object obj) -> 3;
        };
    }

    public static int runCheckExpressionWithUnconditionalAndParams(R1 r) {
        switch (r) {
            case R1(Integer i):
                return meth_I(i);
            case R1(Object o):
                return meth_O(o);
        }
    }

    private String switchNullable1(R6 r) {
        return switch (r) {
            case R6(Integer i1, Integer i2) -> "int: " + i1 + ", int: " + i2;
            case R6(Object o1, Object o2) -> "obj: " + o1 + ", obj: " + o2;
        };
    }

    private String switchNullable2(R6 r) {
        return switch (r) {
            case R6(Integer i1, Integer i2) -> "int: " + i1 + ", int: " + i2;
            case R6(Integer i1, Object o2) -> "int: " + i1 + ", obj: " + o2;
            case R6(Object o1, Integer i2) -> "obj: " + o1 + ", int: " + i2;
            case R6(Object o1, Object o2) -> "obj: " + o1 + ", obj: " + o2;
        };
    }

    private String switchNullableNPE(R6 r) {
        return switch (r) {
            case R6(Object o1, Object o2) when ((int) o1) == 0 && ((int) o2) == 0 -> "int: " + o1 + ", int: " + o2;
            case R6(Object o1, Object o2) -> "obj: " + o1 + ", obj: " + o2;
        };
    }

    public static int runFallThrough(R7 r) {
        switch (r) {
            case R7(var v1, var v2) when v1 != 0: return 0;
            case R7(var v1, var v2):
        }
        return 1;
    }

    public static int meth_I(Integer i) { return 1; }
    public static int meth_O(Object o) { return 2;}

    private void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("expected: " + expected + ", " +
                                     "actual: " + actual);
        }
    }

    private void assertEquals(String expected, String actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected: " + expected + ", " +
                                     "actual: " + actual);
        }
    }

    record R1(Object o) {}
    record R2(Object o) {}
    record R3(Object o) {}

    sealed class Super permits Sub1, Sub2, Sub3 {}
    final class Sub1 extends Super {}
    final class Sub2 extends Super {}
    final class Sub3 extends Super {}

    record R4(Super o) {}
    record R5(R4 o) {}
    record R6(Object o1, Object o2) {}
    record R7(int i1, int i2) {}
}
