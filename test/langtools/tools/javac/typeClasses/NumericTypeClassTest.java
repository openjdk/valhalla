/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * @test
 * @summary Smoke test for numeric type class hierarchy
 * @run junit/othervm NumericTypeClassTest
 */
public class NumericTypeClassTest {
    interface Eq<X> {
        boolean eq(X x1, X x2);

        __witness Eq<Integer> INT = (x1, x2) -> x1 == x2;
    }

    interface Ord<X> extends Eq<X> {
        int compare(X x1, X x2);

        __witness Ord<Integer> INT = new Ord<>() {
            public int compare(Integer x, Integer y) {
                return x.compareTo(y);
            }
            public boolean eq(Integer x, Integer y) {
                return Eq<Integer>.__witness.eq(x, y);
            }
        };
    }

    interface Num<X> extends Eq<X> {
        X plus(X a, X b);
        X minus(X a, X b);
        X mul(X a, X b);

        __witness Num<Integer> INT = new Num<>() {
            public Integer mul(Integer x, Integer y) {
                return x * y;
            }
            public Integer plus(Integer x, Integer y) {
                return x + y;
            }
            public Integer minus(Integer x, Integer y) {
                return x - y;
            }
            public boolean eq(Integer x, Integer y) {
                return Eq<Integer>.__witness.eq(x, y);
            }
        };
    }

    interface Enumeration<X> {
        X succ(X x);
        X pred(X x);

        __witness Enumeration<Integer> INT = new Enumeration<>() {
            public Integer succ(Integer x) {
                return x + 1;
            }
            public Integer pred(Integer x) {
                return x - 1;
            }
        };
    }

    interface Bounded<X> {
        X min();
        X max();

        __witness Bounded<Integer> INT = new Bounded<>() {
            public Integer max() {
                return Integer.MAX_VALUE;
            }
            public Integer min() {
                return Integer.MIN_VALUE;
            }
        };
    }

    interface Integral<X> extends Num<X>, Ord<X>, Enumeration<X> {
        X div(X a, X b);

        __witness Integral<Integer> INT = new Integral<>() {
            public Integer div(Integer x, Integer y) {
                return x / y;
            }
            public Integer succ(Integer x) {
                return Enumeration<Integer>.__witness.succ(x);
            }
            public Integer pred(Integer x) {
                return Enumeration<Integer>.__witness.pred(x);
            }
            public Integer mul(Integer x, Integer y) {
                return Num<Integer>.__witness.mul(x, y);
            }
            public Integer plus(Integer x, Integer y) {
                return Num<Integer>.__witness.plus(x, y);
            }
            public Integer minus(Integer x, Integer y) {
                return Num<Integer>.__witness.minus(x, y);
            }
            public boolean eq(Integer x, Integer y) {
                return Eq<Integer>.__witness.eq(x, y);
            }
            public int compare(Integer x, Integer y) {
                return Ord<Integer>.__witness.compare(x, y);
            }
        };
    }

    @Test
    public void testEq() {
        testEq(Eq<Integer>.__witness);
    }

    @Test
    public void testOrd() {
        testOrd(Ord<Integer>.__witness);
        testEq(Ord<Integer>.__witness);
    }

    @Test
    public void testEnum() {
        testEnum(Enumeration<Integer>.__witness);
    }

    @Test
    public void testBounded() {
        testBounded(Bounded<Integer>.__witness);
    }

    @Test
    public void testNum() {
        testNum(Num<Integer>.__witness);
        testEq(Num<Integer>.__witness);
    }

    @Test
    public void testIntegral() {
        testIntegral(Integral<Integer>.__witness);
        testNum(Integral<Integer>.__witness);
        testOrd(Integral<Integer>.__witness);
        testEnum(Integral<Integer>.__witness);
    }

    void testEq(Eq<Integer> eq) {
        assertFalse(eq.eq(1, 3));
        assertTrue(eq.eq(3, 3));
    }

    void testOrd(Ord<Integer> ord) {
        assertTrue(ord.compare(1, 3) < 0);
        assertTrue(ord.compare(3, 1) > 0);
        assertTrue(ord.compare(3, 3) == 0);
    }

    void testEnum(Enumeration<Integer> en) {
        assertEquals(en.succ(1), 2);
        assertEquals(en.pred(2), 1);
    }

    void testBounded(Bounded<Integer> bounded) {
        assertEquals(bounded.max(), Integer.MAX_VALUE);
        assertEquals(bounded.min(), Integer.MIN_VALUE);
    }

    void testNum(Num<Integer> num) {
        assertEquals(num.plus(1, 2), 3);
        assertEquals(num.minus(3, 2), 1);
        assertEquals(num.mul(3, 2), 6);
    }

    void testIntegral(Integral<Integer> intg) {
        assertEquals(intg.div(4, 2), 2);
        assertEquals(intg.div(5, 2), 2);
    }
}
