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
 * @bug 8383809
 * @summary mostly random tests, checked against BigInteger.
 * @comment Use -DCOUNT=n to specify the number of iterations for each test
 *      (default is 100_000)
 * @run junit UInt128Test
 */

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.math.UInt128;
import java.math.UInt64;
import java.util.Locale;
import java.util.random.RandomGenerator;

import static java.lang.Integer.signum;
import static java.math.BigInteger.valueOf;
import static java.math.UInt128.*;
import static org.junit.jupiter.api.Assertions.*;

public class UInt128Test {

    private static final BigInteger MASK_64 = valueOf(-1).shiftLeft(64);
    private static final BigInteger MASK_128 = valueOf(-1).shiftLeft(128);
    private static final RandomGenerator rnd = RandomGenerator.getDefault();

    private static final int COUNT;

    static {
        COUNT = Integer.getInteger("COUNT", 100_000);
    }

    @Test
    void testLen10() {
        for (int e = 0; e <= DEC_SIZE; ++e) {
            UInt128 pow10 = pow10(e);
            assertEquals(e, len10(subtract(pow10, 1)));
            assertEquals(e + 1, len10(pow10));
            assertEquals(e + 1, len10(add(pow10, 1)));
        }
    }

    @Test
    void testParse() {
        testParse("0");
        testParse("1");
        testParse("0000");
        testParse("00_00");
        testParse("000000000000001");
        testParse("123_456");
        testParse("123__456");
        testParse("000_123_456");
        testParse("000____123_456");
        testParse("000____123____456");
        testParse("0".repeat(1_000_000));
        testParse("9".repeat(DEC_SIZE));
        testParse("340282366920938463463374607431768211455");  // 2^128 - 1
        assertThrows(IllegalArgumentException.class,
                () -> testParse("-1"));
        assertThrows(IllegalArgumentException.class,
                () -> testParse("  0"));  // non digit chars
        assertThrows(IllegalArgumentException.class,
                () -> testParse("000  "));  // non digit chars
        assertThrows(IllegalArgumentException.class,
                () -> testParse("cafe_babe"));  // non digit chars
        assertThrows(IllegalArgumentException.class,
                () -> testParse("๑๒๓๔"));  // non ASCII digits
        assertThrows(IllegalArgumentException.class,
                () -> testParse("_000"));  // leading _
        assertThrows(IllegalArgumentException.class,
                () -> testParse("_123"));  // leading _
        assertThrows(IllegalArgumentException.class,
                () -> testParse("000_"));  // trailing _
        assertThrows(IllegalArgumentException.class,
                () -> testParse("123_"));  // trailing _
        assertThrows(ArithmeticException.class,
                () -> testParse("340282366920938463463374607431768211456"));  // 2^128
        assertThrows(IndexOutOfBoundsException.class,
                () -> testParse(""));
        for (int i = 0; i < COUNT; ++i) {
            String s = (randomLong() + "" + randomLong()).replace("-", "");
            testParse(s);
        }
    }

    private void testParse(String s) {
        UInt128 x = UInt128.parse(s, 0, s.length(), 10, '_');
        assertEqualsUInt128(x, new BigInteger(s.replace("_", "")), x);
    }

    @Test
    void testParseLiteral() {
        testParseLiteral("0");
        testParseLiteral("1");
        testParseLiteral("0000");
        testParseLiteral("00_00");
        testParseLiteral("000000000000001");
        testParseLiteral("123_456");
        testParseLiteral("123__456");
        testParseLiteral("000_123_456");
        testParseLiteral("000____123_456");
        testParseLiteral("000____123____456");
        testParseLiteral("0" + "_0".repeat(1_000_000));
        testParseLiteral("9".repeat(DEC_SIZE));
        testParseLiteral("340282366920938463463374607431768211455");  // 2^128 - 1
        testParseLiteral("0xffff_FFFF__ffff_fFfF___ffff_FFFF__ffff_fFfF");  // 2^128 - 1
        testParseLiteral("0B" + "1".repeat(128));  // 2^128 - 1
        assertThrows(IllegalArgumentException.class,
                () -> testParseLiteral("-1"));
        assertThrows(IllegalArgumentException.class,
                () -> testParseLiteral("0B_000"));  // leading _
        assertThrows(IllegalArgumentException.class,
                () -> testParseLiteral("0x_123"));  // leading _
        assertThrows(IllegalArgumentException.class,
                () -> testParseLiteral("000_"));  // trailing _
        assertThrows(IllegalArgumentException.class,
                () -> testParseLiteral("123_"));  // trailing _
        assertThrows(IllegalArgumentException.class,
                () -> testParseLiteral("08"));
        assertThrows(IllegalArgumentException.class,
                () -> testParseLiteral("000000009"));
        assertThrows(IndexOutOfBoundsException.class,
                () -> testParseLiteral(""));
        assertThrows(ArithmeticException.class,
                () -> testParseLiteral("340282366920938463463374607431768211456"));  // 2^128
        assertThrows(ArithmeticException.class,
                () -> testParseLiteral("0X1" + "0".repeat(32)));  // 2^128
        assertThrows(ArithmeticException.class,
                () -> testParseLiteral("0B1" + "0".repeat(128)));  // 2^128
        for (int i = 0; i < COUNT; ++i) {
            String s = (randomNonzeroLong() + "" + randomLong()).replace("-", "");
            testParseLiteral(s);
        }
    }

    private void testParseLiteral(String s) {
        UInt128 x = parseLiteral(s, 0, s.length());
        s = s.replace("_", "").toLowerCase(Locale.ROOT);
        int radix =
                s.indexOf("0x") == 0
                ? 16
                : s.indexOf("0b") == 0
                ? 2
                : s.indexOf("0") == 0
                ? 8
                : 10;
        if (radix == 16 || radix == 2) {
            s = s.substring(2);
        }
        assertEqualsUInt128(x, new BigInteger(s, radix), x);
    }

    @Test
    void testToString() {
        testToString(_0());
        testToString(_1());
        testToString(MAX_VALUE());
        testToString(parse("9".repeat(UInt64.DEC_SIZE - 1)));
        testToString(parse("9".repeat(UInt64.DEC_SIZE)));
        testToString(parse("9".repeat(UInt64.DEC_SIZE + 1)));
        testToString(parse("9".repeat(2 * UInt64.DEC_SIZE - 1)));
        testToString(parse("9".repeat(2 * UInt64.DEC_SIZE)));
        testToString(parse("1".repeat(2 * UInt64.DEC_SIZE + 1)));
        for (int i = 0; i < COUNT; ++i) {
            testToString(randomUInt128());
        }
    }

    private UInt128 parse(String s) {
        return UInt128.parse(s, 0, s.length(), 10, -1);
    }

    private void testToString(UInt128 x) {
        assertEquals(x.toBigInteger().toString(), x.toString());
    }

    @Test
    void testAdd() {
        for (int i = 0; i < COUNT; ++i) {
            testAdd(randomUInt128(), randomLong());
        }

        for (int i = 0; i < COUNT; ++i) {
            testAdd(randomUInt128(), randomUInt128());
        }
    }

    private void testAdd(UInt128 x, long y) {
        BigInteger xp = x.toBigInteger();
        BigInteger yp = unsignedToBigInteger(y);
        assertEqualsUInt128(x, y, xp.add(yp), add(x, y));
    }

    private void testAdd(UInt128 x, UInt128 y) {
        BigInteger xp = x.toBigInteger();
        BigInteger yp = y.toBigInteger();
        assertEqualsUInt128(x, y, xp.add(yp), add(x, y));
    }

    @Test
    void testSubtract() {
        for (int i = 0; i < COUNT; ++i) {
            testSubtract(randomUInt128(), randomLong());
        }

        for (int i = 0; i < COUNT; ++i) {
            testSubtract(randomUInt128(), randomUInt128());
        }
    }

    private void testSubtract(UInt128 x, long y) {
        BigInteger xp = x.toBigInteger();
        BigInteger yp = unsignedToBigInteger(y);
        assertEqualsUInt128(x, y, xp.subtract(yp), subtract(x, y));
    }

    private void testSubtract(UInt128 x, UInt128 y) {
        BigInteger xp = x.toBigInteger();
        BigInteger yp = y.toBigInteger();
        assertEqualsUInt128(x, y, xp.subtract(yp), subtract(x, y));
    }

    @Test
    void testMultiply() {
        testMultiply(0, randomLong());
        for (int i = 0; i < COUNT; ++i) {
            testMultiply(randomLong(), randomLong());
        }

        testMultiply(_0(), randomLong());
        for (int i = 0; i < COUNT; ++i) {
            testMultiply(randomUInt128(), randomLong());
        }

        testMultiply(_0(), randomUInt128());
        for (int i = 0; i < COUNT; ++i) {
            testMultiply(randomUInt128(), randomUInt128());
        }
    }

    private void testMultiply(long x, long y) {
        BigInteger xp = unsignedToBigInteger(x);
        BigInteger yp = unsignedToBigInteger(y);
        assertEqualsUInt128(x, y, xp.multiply(yp), multiply(x, y));
    }

    private void testMultiply(UInt128 x, long y) {
        BigInteger xp = x.toBigInteger();
        BigInteger yp = unsignedToBigInteger(y);
        assertEqualsUInt128(x, y, xp.multiply(yp), multiply(x, y));
    }

    private void testMultiply(UInt128 x, UInt128 y) {
        BigInteger xp = x.toBigInteger();
        BigInteger yp = y.toBigInteger();
        assertEqualsUInt128(x, y, xp.multiply(yp), multiply(x, y));
    }

    @Test
    void testAnd() {
        testAnd(_0(), randomUInt128());
        testAnd(MAX_VALUE(), randomUInt128());
        for (int i = 0; i < COUNT; ++i) {
            testAnd(randomUInt128(), randomUInt128());
        }
    }

    private void testAnd(UInt128 x, UInt128 y) {
        BigInteger xp = x.toBigInteger();
        BigInteger yp = y.toBigInteger();
        assertEqualsUInt128(x, y, xp.and(yp), and(x, y));
    }

    @Test
    void testAndNot() {
        testAndNot(_0(), randomUInt128());
        testAndNot(MAX_VALUE(), randomUInt128());
        testAndNot(randomUInt128(), _0());
        testAndNot(randomUInt128(), new UInt128(-1));
        testAndNot(randomUInt128(), MAX_VALUE());
        for (int i = 0; i < COUNT; ++i) {
            testAndNot(randomUInt128(), randomUInt128());
        }
    }

    private void testAndNot(UInt128 x, UInt128 y) {
        BigInteger xp = x.toBigInteger();
        BigInteger yp = y.toBigInteger();
        assertEqualsUInt128(x, y, xp.andNot(yp), andNot(x, y));
    }

    @Test
    void testOr() {
        testOr(_0(), randomUInt128());
        testOr(MAX_VALUE(), randomUInt128());
        for (int i = 0; i < COUNT; ++i) {
            testOr(randomUInt128(), randomUInt128());
        }
    }

    private void testOr(UInt128 x, UInt128 y) {
        BigInteger xp = x.toBigInteger();
        BigInteger yp = y.toBigInteger();
        assertEqualsUInt128(x, y, xp.or(yp), or(x, y));
    }

    @Test
    void testXor() {
        testXor(_0(), randomUInt128());
        testXor(MAX_VALUE(), randomUInt128());
        for (int i = 0; i < COUNT; ++i) {
            testXor(randomUInt128(), randomUInt128());
        }
    }

    private void testXor(UInt128 x, UInt128 y) {
        BigInteger xp = x.toBigInteger();
        BigInteger yp = y.toBigInteger();
        assertEqualsUInt128(x, y, xp.xor(yp), xor(x, y));
    }

    @Test
    void testNegate() {
        testNegate(_0());
        testNegate(_1());
        testNegate(MAX_VALUE());
        for (int i = 0; i < COUNT; ++i) {
            testNegate(randomUInt128());
        }
    }

    private void testNegate(UInt128 x) {
        BigInteger xp = x.toBigInteger();
        assertEqualsUInt128(x, xp.negate(), negate(x));
    }

    @Test
    void testNot() {
        testNot(_0());
        testNot(MAX_VALUE());
        for (int i = 0; i < COUNT; ++i) {
            testNot(randomUInt128());
        }
    }

    private void testNot(UInt128 x) {
        BigInteger xp = x.toBigInteger();
        assertEqualsUInt128(x, xp.not(), not(x));
    }

    @Test
    void testShiftLeft() {
        for (int i = 0; i < COUNT; ++i) {
            testShiftLeft(randomUInt128(), rnd.nextInt());
        }
    }

    private void testShiftLeft(UInt128 x, int sh) {
        BigInteger xp = x.toBigInteger();
        assertEqualsUInt128(x, sh,
                xp.shiftLeft(sh & SIZE - 1), shiftLeft(x, sh));
    }

    @Test
    void testShiftRight() {
        for (int i = 0; i < COUNT; ++i) {
            testShiftRight(randomUInt128(), rnd.nextInt());
        }
    }

    private void testShiftRight(UInt128 x, int sh) {
        BigInteger xp = x.toBigInteger();
        assertEqualsUInt128(x, sh,
                xp.shiftRight(sh & SIZE - 1), shiftRight(x, sh));
    }

    @Test
    void testDivideAndRemainder64_64() {
        testDivideAndRemainder(_0(), new UInt128(randomNonzeroLong()));

        for (int i = 0; i < COUNT; ++i) {
            UInt128 x = new UInt128(randomNonzeroLong());
            testDivideAndRemainder(x, x);
        }

        for (int i = 0; i < COUNT; ++i) {
            testDivideAndRemainder(
                    new UInt128(randomLong()),
                    new UInt128(randomNonzeroLong()));
        }
    }

    @Test
    void testDivideAndRemainder128_32() {
        testDivideAndRemainder(_0(), new UInt128(randomNonzeroInt()));

        for (int i = 0; i < COUNT; ++i) {
            UInt128 x = new UInt128(randomNonzeroInt());
            testDivideAndRemainder(x, x);
        }

        for (int i = 0; i < COUNT; ++i) {
            testDivideAndRemainder(
                    randomUInt128(),
                    new UInt128(randomNonzeroInt()));
        }
    }

    @Test
    void testDivideAndRemainder() {
        assertThrows(ArithmeticException.class,
                () -> divideAndRemainder(_0(), _0()));
        assertThrows(ArithmeticException.class,
                () -> divideAndRemainder(randomUInt128(), _0()));

        /* Triggers last rare correction only in the 1st part of the division algorithm. */
        testDivideAndRemainder(
                new UInt128(0x0000_0000_0000_0000L, 0x8000_0000_0000_0000L),
                new UInt128(0x0000_0000_0000_0001L, 0x0000_0000_4000_0000L));

        /* Triggers last rare correction only in the 2nd part of the division algorithm. */
        testDivideAndRemainder(
                new UInt128(0x0000_0000_0000_0000L, 0x0000_0001_0000_0000L),
                new UInt128(0x0000_0000_0000_0001L, 0x0000_0000_8000_0000L));

        /* Triggers last rare correction in both parts of the division algorithm. */
        testDivideAndRemainder(
                new UInt128(0x0000_0000_0000_0000L, 0x8000_0000_0000_0000L),
                new UInt128(0x0000_0000_0000_0001L, 0x0000_0000_8000_0000L));

        testDivideAndRemainder(_0(), randomNonzeroUInt128());

        for (int i = 0; i < COUNT; ++i) {
            UInt128 x = randomNonzeroUInt128();
            testDivideAndRemainder(x, x);
        }

        for (int i = 0; i < COUNT; ++i) {
            testDivideAndRemainder(
                    randomUInt128(),
                    randomNonzeroUInt128());
        }
    }

    @Test
    void testComparisons() {
        testComparisons(_0(), _0());
        testComparisons(_0(), randomUInt128());
        testComparisons(_1(), _1());
        testComparisons(_1(), randomUInt128());
        testComparisons(MAX_VALUE(), MAX_VALUE());
        testComparisons(MAX_VALUE(), randomUInt128());
        UInt128 x = randomUInt128();
        testComparisons(x, x);

        testComparisons(_0(), new UInt128(1, -1));
        testComparisons(new UInt128(1, -1), new UInt128(2, -1));

        for (int i = 0; i < COUNT; ++i) {
            testComparisons(randomUInt128(), randomUInt128());
        }
    }

    private void testComparisons(UInt128 x, UInt128 y) {
        int cmp = x.toBigInteger().compareTo(y.toBigInteger());
        assertEquals(cmp == 0, equal(x, y));
        assertEquals(cmp != 0, !equal(x, y));
        assertEquals(cmp < 0, lessThan(x, y));
        assertEquals(cmp <= 0, lessEqual(x, y));
        assertEquals(cmp > 0, greaterThan(x, y));
        assertEquals(cmp >= 0, greaterEqual(x, y));
        assertEquals(signum(cmp), signum(x.compareTo(y)));
    }

    private static void testDivideAndRemainder(UInt128 x, UInt128 y) {
        QuoRem actual = divideAndRemainder(x, y);
        assertTrue(lessThan(actual.rem(), y));
        assertEqualsUInt128(x, y, x, add(multiply(actual.quo(), y), actual.rem()));

        BigInteger xp = x.toBigInteger();
        BigInteger yp = y.toBigInteger();
        BigInteger[] expected = xp.divideAndRemainder(yp);
        assertEqualsUInt128(x, y, expected[0], actual.quo());
        assertEqualsUInt128(x, y, expected[1], actual.rem());
    }

    private static long randomLong() {
        return rnd.nextLong();
    }

    private static long randomNonzeroLong() {
        for (int i = 0; i < 1_000; ++i) {
            long x = randomLong();
            if (x != 0) {
                return x;
            }
        }
        throw new RuntimeException("can't get a non-zero random number!");
    }

    private static long randomInt() {
        return rnd.nextInt() & 0xffff_ffffL;
    }

    private static long randomNonzeroInt() {
        for (int i = 0; i < 1_000; ++i) {
            long x = randomInt();
            if (x != 0) {
                return x;
            }
        }
        throw new RuntimeException("can't get a non-zero random number!");
    }

    private static UInt128 randomUInt128() {
        return new UInt128(rnd.nextLong(), rnd.nextLong());
    }

    private static UInt128 randomNonzeroUInt128() {
        for (int i = 0; i < 1_000; ++i) {
            UInt128 x = randomUInt128();
            if (!isZero(x)) {
                return x;
            }
        }
        throw new RuntimeException("can't get a non-zero random number!");
    }

    private static BigInteger unsignedToBigInteger(long v) {
        return valueOf(v).andNot(MASK_64);
    }

    private static void assertEqualsUInt128(UInt128 x, UInt128 y,
            UInt128 expected, UInt128 actual) {
        if (!equal(expected, actual)) {
            throw new RuntimeException(String.format("x=%s, y=%s, expected=%s, actual=%s", x, y, expected, actual));
        }
    }

    private static void assertEqualsUInt128(UInt128 x, UInt128 y,
            BigInteger expectedp, UInt128 actual) {
        UInt128 expected = new UInt128(expectedp.andNot(MASK_128));
        if (!equal(expected, actual)) {
            throw new RuntimeException(String.format("x=%s, y=%s, expected=%s, actual=%s", x, y, expected, actual));
        }
    }

    private static void assertEqualsUInt128(UInt128 x,
            BigInteger expectedp, UInt128 actual) {
        UInt128 expected = new UInt128(expectedp.andNot(MASK_128));
        if (!equal(expected, actual)) {
            throw new RuntimeException(String.format("x=%s, expected=%s, actual=%s", x, expected, actual));
        }
    }

    private static void assertEqualsUInt128(long x, long y,
            BigInteger expected, UInt128 actual) {
        assertEqualsUInt128(new UInt128(x), new UInt128(y), expected, actual);
    }

    private static void assertEqualsUInt128(UInt128 x, long y,
            BigInteger expected, UInt128 actual) {
        assertEqualsUInt128(x, new UInt128(y), expected, actual);
    }

}
