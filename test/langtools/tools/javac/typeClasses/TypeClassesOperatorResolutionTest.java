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
 * @run junit TypeClassesOperatorResolutionTest
 */

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TypeClassesOperatorResolutionTest {
    record NumBox(int x) {
        __witness Numerical<NumBox> NUM = new Numerical<>() {
            @Override
            public NumBox add(NumBox addend, NumBox augend) {
                return new NumBox(addend.x + augend.x);
            }

            @Override
            public NumBox subtract(NumBox minuend, NumBox subtrahend) {
                return new NumBox(minuend.x - subtrahend.x);
            }

            @Override
            public NumBox multiply(NumBox multiplier, NumBox multiplicand) {
                return new NumBox(multiplier.x * multiplicand.x);
            }

            @Override
            public NumBox divide(NumBox dividend, NumBox divisor) {
                return new NumBox(dividend.x / divisor.x);
            }

            @Override
            public NumBox remainder(NumBox dividend, NumBox divisor) {
                return new NumBox(dividend.x % divisor.x);
            }

            @Override
            public NumBox plus(NumBox operand) {
                return operand;
            }

            @Override
            public NumBox negate(NumBox operand) {
                return new NumBox(-operand.x);
            }
        };

        __witness Orderable<NumBox> ORD = new Orderable<NumBox>() {
            @Override
            public boolean lessThan(NumBox op1, NumBox op2) {
                return op1.x < op2.x;
            }

            @Override
            public boolean lessThanEqual(NumBox op1, NumBox op2) {
                return op1.x <= op2.x;
            }

            @Override
            public boolean greaterThan(NumBox op1, NumBox op2) {
                return op1.x > op2.x;
            }

            @Override
            public boolean greaterThanEqual(NumBox op1, NumBox op2) {
                return op1.x >= op2.x;
            }

            @Override
            public NumBox min(NumBox op1, NumBox op2) {
                return new NumBox(Math.min(op1.x, op2.x));
            }

            @Override
            public NumBox max(NumBox op1, NumBox op2) {
                return new NumBox(Math.max(op1.x, op2.x));
            }
        };

        __witness Integral<NumBox> INT = new Integral<>() {
            @Override
            public NumBox add(NumBox addend, NumBox augend) {
                return NUM.add(addend, augend);
            }

            @Override
            public NumBox subtract(NumBox minuend, NumBox subtrahend) {
                return NUM.subtract(minuend, subtrahend);
            }

            @Override
            public NumBox multiply(NumBox multiplier, NumBox multiplicand) {
                return NUM.multiply(multiplier, multiplicand);
            }

            @Override
            public NumBox divide(NumBox dividend, NumBox divisor) {
                return NUM.divide(dividend, divisor);
            }

            @Override
            public NumBox remainder(NumBox dividend, NumBox divisor) {
                return NUM.remainder(dividend, divisor);
            }

            @Override
            public NumBox plus(NumBox operand) {
                return NUM.plus(operand);
            }

            @Override
            public NumBox negate(NumBox operand) {
                return NUM.negate(operand);
            }

            @Override
            public NumBox and(NumBox op1, NumBox op2) {
                return new NumBox(op1.x & op2.x);
            }

            @Override
            public NumBox or(NumBox op1, NumBox op2) {
                return new NumBox(op1.x | op2.x);
            }

            @Override
            public NumBox xor(NumBox op1, NumBox op2) {
                return new NumBox(op1.x ^ op2.x);
            }

            @Override
            public NumBox complement(NumBox op1) {
                return new NumBox(~op1.x);
            }

            @Override
            public NumBox shiftLeft(NumBox x, int shiftDistance) {
                return new NumBox(x.x << shiftDistance);
            }

            @Override
            public NumBox shiftRight(NumBox x, int shiftDistance) {
                return new NumBox(x.x >> shiftDistance);
            }

            @Override
            public NumBox shiftRightUnsigned(NumBox x, int shiftDistance) {
                return new NumBox(x.x >>> shiftDistance);
            }

            @Override
            public boolean lessThan(NumBox op1, NumBox op2) {
                return ORD.lessThan(op1, op2);
            }

            @Override
            public boolean lessThanEqual(NumBox op1, NumBox op2) {
                return ORD.lessThanEqual(op1, op2);
            }

            @Override
            public boolean greaterThan(NumBox op1, NumBox op2) {
                return ORD.greaterThan(op1, op2);
            }

            @Override
            public boolean greaterThanEqual(NumBox op1, NumBox op2) {
                return ORD.greaterThanEqual(op1, op2);
            }

            @Override
            public NumBox min(NumBox op1, NumBox op2) {
                return ORD.min(op1, op2);
            }

            @Override
            public NumBox max(NumBox op1, NumBox op2) {
                return ORD.max(op1, op2);
            }
        };
    }

    static NumBox numBox(int x) {
        return new NumBox(x);
    }

    @Test
    void testNumerical() {
        assertEquals(numBox(1 + 2), numBox(1) + numBox(2)); // 3
        assertEquals(numBox(1 - 2), numBox(1) - numBox(2)); // -1
        assertEquals(numBox(2 * 3), numBox(2) * numBox(3)); // 6
        assertEquals(numBox(5 / 2), numBox(5) / numBox(2)); // 2
        assertEquals(numBox(5 % 2), numBox(5) % numBox(2)); // 1
        assertEquals(numBox(+1), +numBox(1));                       // 1
        assertEquals(numBox(-1), -numBox(1));                       // -1
    }

    @Test
    void testIntegral() {
        assertEquals(numBox(1 & 2), numBox(1) & numBox(2));        // 0
        assertEquals(numBox(1 | 2), numBox(1) | numBox(2));        // 3
        assertEquals(numBox(1 ^ 2), numBox(1) ^ numBox(2));        // 3
        assertEquals(numBox(~1), ~numBox(1));                           // -2
        assertEquals(numBox(1 << 2), numBox(1) << 2);                 // 4
        assertEquals(numBox(4 >> 2), numBox(4) >> 2);                 // 1
        assertEquals(numBox(-4 >>> 30), numBox(-4) >>> 30);           // 3
    }

    @Test
    void testOrderable() {
        assertTrue(numBox(1) < numBox(2));
        assertTrue(numBox(1) <= numBox(2));
        assertTrue(numBox(2) > numBox(1));
        assertTrue(numBox(2) >= numBox(1));
        assertTrue(numBox(2) >= numBox(2));
        assertTrue(numBox(2) <= numBox(2));
    }

    @Test
    void testAssignopNumerical() {
        var plus = numBox(1);
        plus += numBox(2);
        assertEquals(numBox(1 + 2), plus); // 3

        var sub = numBox(1);
        sub -= numBox(2);
        assertEquals(numBox(1 - 2), sub); // -1

        var mul = numBox(2);
        mul *= numBox(3);
        assertEquals(numBox(2 * 3), mul); // 6

        var div = numBox(5);
        div /= numBox(2);
        assertEquals(numBox(5 / 2), div); // 2

        var mod = numBox(5);
        mod %= numBox(2);
        assertEquals(numBox(5 % 2), mod); // 1
    }

    @Test
    void testAssignopIntegral() {
        var and = numBox(1);
        and &= numBox(2);
        assertEquals(numBox(1 & 2), and); // 0

        var or = numBox(1);
        or |= numBox(2);
        assertEquals(numBox(1 | 2), or); // 3

        var xor = numBox(1);
        xor ^= numBox(2);
        assertEquals(numBox(1 ^ 2), xor); // 3

        var shl = numBox(1);
        shl <<= 2;
        assertEquals(numBox(1 << 2), shl); // 4

        var shr = numBox(4);
        shr >>= 2;
        assertEquals(numBox(4 >> 2), shr); // 1

        var ushr = numBox(-4);
        ushr >>= 30;
        assertEquals(numBox(-4 >> 30), ushr); // 3
    }

    @Test
    void testStackmapUnary() {
        NumBox e1 = numBox(1);
        NumBox eR  = testStackmapUnary(e1, true);
        assertEquals(eR, numBox(-1));
    }

    @Test
    static NumBox testStackmapUnary(NumBox e1, boolean cond) {
        NumBox eR = null;
        if (cond) {
            eR = -e1;
        } else {
            eR = e1;
        }
        return eR;
    }

    @Test
    void testStackmapBinary() {
        NumBox e1 = numBox(1);
        NumBox e2 = numBox(2);
        NumBox eR  = testStackmapBinary(e1, e2, true);
        assertEquals(eR, numBox(3));
    }

    NumBox testStackmapBinary(NumBox e1, NumBox e2, boolean cond) {
        NumBox eR = null;
        if (cond) {
            eR = e1 + e2;
        } else {
            eR = e1;
        }
        return eR;
    }

    @Test
    void testStackmapAssignop() {
        NumBox e1 = numBox(1);
        NumBox e2 = numBox(2);
        NumBox eR  = testStackmapAssignop(e1, e2, true);
        assertEquals(eR, numBox(3));
    }

    NumBox testStackmapAssignop(NumBox e1, NumBox e2, boolean cond) {
        NumBox eR = e1;
        if (cond) {
            eR += e2;
        } else {
            eR = e1;
        }
        return eR;
    }
}
