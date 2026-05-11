/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package java.math;

import jdk.internal.vm.annotation.Stable;

import static java.lang.Character.*;
import static java.lang.Long.divideUnsigned;
import static java.lang.Long.toUnsignedString;
import static java.math.UInt64.*;

/**
 * Immutable unsigned 128-bit integers ({@link #SIZE} = 128).
 * <p>
 * All operations are performed modulo 2<sup>{@link #SIZE}</sup>,
 * unless specified otherwise.
 * <p>
 * WARNING:
 * <ul>
 * <li>All {@code long}s are taken as <em>unsigned</em>.
 * <li>Inputs are <em>not</em> validated, unless specified.
 * It's up to the caller to ensure that the assumptions in the spec are met.
 * If they are not, incorrect values might be returned, or exceptions can be
 * thrown.
 * <li>As a consequence, if no assumptions are specified then the returned
 * values are correct.
 * </ul>
 */
// TODO: once in the platform, declare value class and remove useless final
@jdk.internal.MigratedValueClass
@jdk.internal.ValueBased
public final class UInt128 implements Comparable<UInt128> {

    @jdk.internal.MigratedValueClass
    @jdk.internal.ValueBased
    private record Quo64Rem64(long quo, long rem) {

        QuoRem toQuoRem() {
            return new QuoRem(new UInt128(quo), new UInt128(rem));
        }

    }

    @jdk.internal.MigratedValueClass
    @jdk.internal.ValueBased
    private record Quo64Rem128(long quo, UInt128 rem) {

        QuoRem toQuoRem() {
            return new QuoRem(new UInt128(quo), rem);
        }

    }

    @jdk.internal.MigratedValueClass
    @jdk.internal.ValueBased
    private record Quo128Rem64(UInt128 quo, long rem) {

        QuoRem toQuoRem() {
            return new QuoRem(quo, new UInt128(rem));
        }

    }

    /**
     * A carrier for the quotient and remainder of a division.
     *
     * @param quo   the quotient
     * @param rem   the remainder
     */
    @jdk.internal.MigratedValueClass
    @jdk.internal.ValueBased
    public record QuoRem(UInt128 quo, UInt128 rem) {

        /**
         * As a pair of hexadecimal strings.
         *
         * @return the result described above.
         */
        public String toHexString() {
            return "QuoRem[quo=0x" + quo.toHexString() +
                    ", rem=0x" + rem.toHexString() +
                    "]";
        }

    }

    /**
     * log<sub>2</sub> {@link #SIZE}.
     */
    public static final int LOG2_SIZE = 7;

    /**
     * The number of bits used to represent a value of this class in binary form.
     */
    public static final int SIZE = 1 << LOG2_SIZE;

    /**
     * max{e ∈ ℕ : 10<sup>e</sup> ≤ 2<sup>{@link #SIZE}</sup>}
     */
    public static final int DEC_SIZE = 38;

    // TODO: once in the platform, add it to the AOT cache
    @Stable
    private static final UInt128[] POW_10;  // powers of 10, up to 10^DEC_SIZE

    static {
        POW_10 = new UInt128[DEC_SIZE + 1];
        POW_10[0] = new UInt128(1L);
        for (int i = 1; i < POW_10.length; ++i) {
            POW_10[i] = multiply(POW_10[i - 1], 10);
        }
    }

    /* The value is c0 + c1 2^64 */
    final long c0;
    final long c1;

    /**
     * Constructs the instance with value {@code c0} + {@code c1}⋅2<sup>64</sup>.
     *
     * @param c0 the low bits
     * @param c1 the high bits
     */
    public UInt128(long c0, long c1) {
        this.c0 = c0;
        this.c1 = c1;
    }

    /**
     * Constructs the instance with value {@code c0}.
     *
     * @param c0 the low bits
     */
    public UInt128(long c0) {
        this(c0, 0);
    }

    /***
     * {@return the low bits}
     */
    public long c0() {
        return c0;
    }

    /**
     * {@return the high bits}
     */
    public long c1() {
        return c1;
    }

    /**
     * Returns the instance representing {@code v}.
     *
     * @param v must meet 0 ≤ {@code v} &lt; 2<sup>{@link #SIZE}</sup>.
     * @throws IllegalArgumentException if {@code v} is out of range.
     */
    public UInt128(BigInteger v) {
        if (v.signum() < 0 || v.bitLength() > SIZE) {
            throw new IllegalArgumentException("the argument is out of range");
        }
        byte[] va = v.toByteArray();
        int from = va[0] != 0 ? 0 : 1;
        int len = va.length - from;
        byte[] ba = new byte[SIZE / Byte.SIZE];
        System.arraycopy(va, from, ba, ba.length - len, len);
        this(
                UInt64.getLong(ba, 1 * 8),
                UInt64.getLong(ba, 0 * 8));
    }

    /**
     * Returns the instance representing the unsigned integer in
     * radix {@code radix} of the digits in the portion of {@code s}
     * between index {@code begin} (included) and {@code end} (excluded).
     * <p>
     * The integer value v must meet 0 ≤ v &lt; 2<sup>{@link #SIZE}</sup>.
     * <p>
     * Each character that appears in the portion must be ASCII and must
     * represent a digit in the given radix, or must be the {@code separator}
     * (if ≥ 0).
     * The first and last characters of the input must be valid digits.
     * <p>
     * The characters in the portion are read once, from lower to higher indices,
     * until either an exception is thrown or the end of the portion is reached.
     *
     * @param s the input
     * @param begin the starting index, included
     * @param end the ending index, excluded
     * @param radix the radix
     * @param separator the separator character, negative if none
     * @return the result described above.
     * @throws IllegalArgumentException if radix is not in the interval
     *      [{@link Character#MIN_RADIX}, {@link Character#MAX_RADIX}].
     *      Or if the characters in the portion are neither valid ASCII digits,
     *      nor separators.
     * @throws ArithmeticException if the integer value in the portion overflows.
     * @throws IndexOutOfBoundsException if any index used to access a
     *      character in the given portion falls outside {@code s}.
     */
    public static UInt128 parse(CharSequence s, int begin, int end,
            int radix, int separator) {
        if (MIN_RADIX > radix || radix > MAX_RADIX) {
            throw new IllegalArgumentException("radix must be in the interval " +
                    "[" + MIN_RADIX + ", " + MAX_RADIX + "]");
        }
        return parse(s, begin, end, begin, radix, separator, -1);
    }

    /*
     * Parses the input s between i (included) and end (excluded).
     * Assumes
     *      begin ≤ i ≤ begin + 2
     *      begin = i iff c < 0
     *      begin < i iff c = s[i-1]
     *      begin = i - 2 iff s[begin] = '0'
     */
    private static UInt128 parse(CharSequence s, int begin, int end,
            int i, int radix, int separator, int c) {
        if (c < 0) {
            if (i >= end) {
                throw new IndexOutOfBoundsException();
            }
            c = s.charAt(i++);
        }
        /* c = s[i-1] */
        if (c == separator && i - begin != 2) {
            throw new IllegalArgumentException("leading separator");
        }
        /*
         * This if statement, while logically unnecessary, prevents tons
         * of computations for a huge prefix of useless zeros and separators.
         */
        if (c == '0' || c == separator && i - begin == 2) {
            while (i < end
                    && ((c = s.charAt(i++)) == '0' || c == separator));  //empty body
        }
        UInt128 v = c != separator ? new UInt128(asDigit(c, radix)) : _0();
        while (i < end) {
            c = s.charAt(i++);
            if (c != separator) {
                v = addExact(multiplyExact(v, radix), asDigit(c, radix));
            }
        }
        if (c == separator) {
            throw new IllegalArgumentException("trailing separator");
        }
        return v;
    }

    /**
     * Returns the instance representing the unsigned integer in the portion of {@code s}
     * between index {@code begin} (included) and {@code end} (excluded).
     * The syntax follows the Java Language Specification for integer literals,
     * albeit with no suffix.
     * <p>
     * The characters in the portion are read once, from lower to higher indices,
     * until either an exception is thrown or the end of the portion is reached.
     * <p>
     * The integer value v must meet 0 ≤ v &lt; 2<sup>{@link #SIZE}</sup>.
     *
     * @param s the input
     * @param begin the starting index, included
     * @param end the ending index, excluded
     * @return the result described above.
     * @jls 3.10.1 Integer Literals
     * @throws IllegalArgumentException if radix is not in the interval [2, 36].
     *      Or if the character in the portion are not ASCII and not digits.
     * @throws ArithmeticException if the integer value in the portion overflows.
     * @throws IndexOutOfBoundsException if any index used to access a
     *      character in the given portion falls outside {@code s}.
     */
    public static UInt128 parseLiteral(CharSequence s, int begin, int end) {
        if (begin >= end) {
            throw new IndexOutOfBoundsException();
        }
        int i = begin;
        int c = s.charAt(i++);
        if (c != '0' || i == end) {
            return parse(s, begin, end, i, 10, '_', c);
        }
        c = s.charAt(i++);
        int lc = c | 0b10_0000;  // to lower case
        if (lc == 'x') {
            return parse(s, i, end, i, 16, '_', -1);
        }
        if (lc == 'b') {
            return parse(s, i, end, i, 2, '_', -1);
        }
        return parse(s, begin, end, i, 8, '_', c);
    }

    private static int asDigit(int c, int radix) {
        int d;
        if (c < 0x80 && (d = digit(c, radix)) >= 0) {
            return d;
        }
        throw new IllegalArgumentException("illegal digit for radix=" + radix);
    }

    /**
     * Returns a {@link BigInteger} with the same value as {@code this}.
     *
     * @return the result described above.
     */
    public BigInteger toBigInteger() {
        byte[] ba = new byte[SIZE / Byte.SIZE];
        UInt64.putLong(ba, 0, c1);
        UInt64.putLong(ba, 8, c0);
        return new BigInteger(1, ba);
    }

    /**
     * Returns a {@link String} representing {@code this}.
     *
     * @return the result described above.
     */
    @Override
    public String toString() {
        UInt128 pow10 = pow10(UInt64.DEC_SIZE);
        QuoRem qr = divideAndRemainder(this, pow10);
        long d0 = qr.rem.c0;  // qr.rem < 10^19 < 2^64
        qr = divideAndRemainder(qr.quo, pow10);
        long d1 = qr.rem.c0;  // qr.rem < 10^19 < 2^64
        long d2 = qr.quo.c0;  // qr.quo < 10
        return d2 != 0
                ? toUnsignedString(d2) + toPadString(d1) + toPadString(d0)
                : d1 != 0
                ? toUnsignedString(d1) + toPadString(d0)
                : toUnsignedString(d0);
    }

    private static String toPadString(long v) {
        String s = toUnsignedString(v);
        return s.length() == UInt64.DEC_SIZE
                ? s  // likely case, fast path
                : "0".repeat(UInt64.DEC_SIZE - s.length()) + s;
    }

    /**
     * Returns a {@link String} representing {@code this} in hexadecimal notation.
     *
     * @return the result described above.
     */
    public String toHexString() {
        return c1 != 0
                ? Long.toHexString(c1) + toPadHexString(c0)
                : Long.toHexString(c0);
    }

    private static String toPadHexString(long v) {
        String s = Long.toHexString(v);
        return s.length() == UInt64.SIZE / 4  // likely case, fast path
                ? s
                : "0".repeat(UInt64.SIZE / 4 - s.length()) + s;
    }

    /**
     * {@return the value 0}
     */
    public static UInt128 _0() {
        return new UInt128(0);
    }

    /**
     * {@return the value 1}
     */
    public static UInt128 _1() {
        return new UInt128(1);
    }

    /**
     * Returns the value 2<sup>{@link #SIZE}</sup> - 1.
     *
     * @return the result described above.
     */
    public static UInt128 MAX_VALUE() {
        return new UInt128(-1, -1);
    }

    /**
     * Returns 10<sup>{@code e}</sup>.
     *
     * @param e must meet 0 ≤ {@code e} ≤ {@link #DEC_SIZE}.
     * @return the result described above.
     * @throws RuntimeException if {@code e} is out of range.
     */
    public static UInt128 pow10(int e) {
        return POW_10[e];
    }

    /**
     * Returns {@code x} &lt;&lt; {@code sh}.
     *
     * @param x the parameter
     * @param sh must meet 0 ≤ {@code sh} &lt; 64.
     * @return the result described above.
     */
    static UInt128 shiftLeftSmall(UInt128 x, int sh) {
        /* Branchless code. */
        return new UInt128(
                x.c0 << sh,
                x.c1 << sh | x.c0 >>> 63 - sh >>> 1);
    }

    /**
     * Returns {@code x} &lt;&lt; {@link UInt64#S S} | {@code y}
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    private static UInt128 shiftLeftSOr(UInt128 x, long y) {
        return new UInt128(
                x.c0 << S | y,
                x.c1 << S | x.c0 >>> S);
    }

    /**
     * Returns {@code x} >>> {@code sh}.
     *
     * @param x the paramter
     * @param sh only the 6 least significant bits are relevant.
     * @return the result described above.
     */
    static UInt128 shiftRightSmall(UInt128 x, int sh) {
        /* Branchless code. */
        return new UInt128(
                x.c0 >>> sh | x.c1 << 63 - sh << 1,
                x.c1 >>> sh);
    }

    /**
     * Returns {@code x} >>> {@link UInt64#S S}.
     *
     * @param x the parameter
     * @return the result described above.
     */
    private static UInt128 shiftRightS(UInt128 x) {
        return new UInt128(
                x.c0 >>> S | x.c1 << S,
                x.c1 >>> S);
    }

    /**
     * Returns -{@code x}.
     *
     * @param x the parameter
     * @return the result described above.
     */
    public static UInt128 negate(UInt128 x) {
        long z0, z1;
        z0 = -x.c0;
        z1 = -x.c1 - (x.c0 != 0 ? 1L : 0L);
        return new UInt128(z0, z1);
    }

    /**
     * Returns {@code x} + {@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static UInt128 add(UInt128 x, long y) {
        long z0, z1;
        z0 = x.c0 + y;
        z1 = x.c1 + carry(z0, y);
        return new UInt128(z0, z1);
    }

    /**
     * Returns {@code x} + {@code y}, throwing on overflow.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     * @throws ArithmeticException on overflow.
     */
    public static UInt128 addExact(UInt128 x, long y) {
        long z0, z1;
        z0 = x.c0 + y;
        z1 = UInt64.addExact(x.c1, carry(z0, y));
        return new UInt128(z0, z1);
    }

    /**
     * Returns {@code x} + {@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static UInt128 add(UInt128 x, UInt128 y) {
        long z0, z1;
        z0 = x.c0 + y.c0;
        z1 = x.c1 + y.c1 + carry(z0, y.c0);
        return new UInt128(z0, z1);
    }

    /**
     * Returns {@code x} - {@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static UInt128 subtract(UInt128 x, long y) {
        long z0, z1;
        z0 = x.c0 - y;
        z1 = x.c1 - carry(x.c0, y);
        return new UInt128(z0, z1);
    }

    /**
     * Returns {@code x} - {@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static UInt128 subtract(UInt128 x, UInt128 y) {
        long z0, z1;
        z0 = x.c0 - y.c0;
        z1 = x.c1 - y.c1 - carry(x.c0, y.c0);
        return new UInt128(z0, z1);
    }

    /**
     * Performs an integer division.
     *
     * @param x the dividend.
     * @param y the divisor.
     * @return the quotient q = ⌊{@code x} / {@code y}⌋.
     * @throws ArithmeticException when the divisor is 0.
     */
    public static UInt128 divide(UInt128 x, UInt128 y) {
        return divideAndRemainder(x, y).quo;
    }

    /**
     * Performs an unsigned integer division.
     *
     * @param x the dividend.
     * @param y the divisor.
     * @return the remainder {@code x} - {@link #divide divide(x, y)} {@code y}.
     * @throws ArithmeticException when the divisor is 0.
     */
    public static UInt128 remainder(UInt128 x, UInt128 y) {
        return divideAndRemainder(x, y).rem;
    }

    /**
     * Performs an unsigned integer division with remainder.
     *
     * @param x the dividend.
     * @param y the divisor.
     * @return the quotient q = ⌊{@code x} / {@code y}⌋ and its
     *      remainder {@code x} - q {@code y}, packed as a {@link QuoRem QuoRem}.
     * @throws ArithmeticException when the divisor is 0.
     */
    public static QuoRem divideAndRemainder(UInt128 x, UInt128 y) {
        return (x.c1 | y.c1) == 0
                ? divideAndRemainder64x64(x.c0, y.c0).toQuoRem()  // x, y < 2^64
                : lessThan(x, y)
                ? new QuoRem(_0(), x)  // x < y
                : y.c1 != 0
                ? UInt192.divideAndRemainder(x, y).toQuoRem()  // y ≥ 2^64
                : (UInt64.lessThan(y.c0, B)
                ? divideAndRemainder128x32(x, y.c0)  // y < 2^32
                : divideAndRemainder128x64(x, y.c0)).toQuoRem();  // 2^32 ≤ y < 2^64
    }

    /**
     * Returns the full product {@code x}⋅{@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static UInt128 multiply(long x, long y) {
        return new UInt128(x * y, Math.unsignedMultiplyHigh(x, y));
    }

    /**
     * Returns {@code x}⋅{@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static UInt128 multiply(UInt128 x, long y) {
        long z0, z1, t0;
        z0 = x.c0 * y;
        z1 = Math.unsignedMultiplyHigh(x.c0, y);
        t0 = x.c1 * y;
        z1 += t0;
        return new UInt128(z0, z1);
    }

    /**
     * Returns {@code x}⋅{@code y}, throwing on overflow.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     * @throws ArithmeticException on overflow.
     */
    public static UInt128 multiplyExact(UInt128 x, long y) {
        long z0, z1, t0;
        z0 = x.c0 * y;
        z1 = Math.unsignedMultiplyHigh(x.c0, y);
        t0 = Math.unsignedMultiplyExact(x.c1, y);
        z1 = UInt64.addExact(z1, t0);
        return new UInt128(z0, z1);
    }

    /**
     * Returns {@code x}⋅{@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static UInt128 multiply(UInt128 x, UInt128 y) {
        /*
         * The most significant digit of a 2 by 1 digits product cannot be BB - 1,
         * where BB is the radix of the multidigit representation (here BB = 2^64).
         */
        long z0, z1, t0;

        z0 = x.c0 * y.c0;
        z1 = Math.unsignedMultiplyHigh(x.c0, y.c0);

        t0 = x.c1 * y.c0;
        z1 += t0;
        t0 = x.c0 * y.c1;
        z1 += t0;
        return new UInt128(z0, z1);
    }

    /**
     * Performs an integer division with remainder.
     *
     * @param x the dividend.
     * @param y the divisor.
     * @return the quotient ⌊{@code x} / {@code y}⌋ and its remainder.
     */
    private static Quo64Rem64 divideAndRemainder64x64(long x, long y) {
        long q = divideUnsigned(x, y);
        long r = x - q * y;
        return new Quo64Rem64(q, r);
    }

    /**
     * Performs an integer division with remainder.
     *
     * @param x the dividend.
     * @param y the divisor.
     *          Must meet {@code y} &lt; 2<sup>32</sup> (not checked).
     * @return the quotient q = ⌊{@code x} / {@code y}⌋
     *      and its remainder r = {@code x} - q {@code y}.
     */
    private static Quo128Rem64 divideAndRemainder128x32(UInt128 x, long y) {
        /*
         * This computation can be performed by divideAndRemainder128x64(),
         * but when the divisor consist of just one B-based digit, like here,
         * it's even simpler.
         */
        long u = x.c1;
        long q23 = divideUnsigned(u, y);  // q23 < B^2
        u = u - q23 * y << S | x.c0 >>> S;  // u < y < B
        long q1 = divideUnsigned(u, y);  // q1 < B
        u = u - q1 * y << S | x.c0 & MASK;  // u < y < B
        long q0 = divideUnsigned(u, y);  // q0 < B
        u = u - q0 * y;  // u < y < B
        long q01 = q1 << S | q0;
        return new Quo128Rem64(new UInt128(q01, q23), u);
    }

    /**
     * Performs an integer division with remainder.
     *
     * @param x the dividend.
     * @param y the divisor.
     * @return the quotient q = ⌊{@code x} / {@code y}⌋
     *      and its remainder r = {@code x} - q {@code y}.
     */
    private static Quo128Rem64 divideAndRemainder128x64(UInt128 x, long y) {
        /* Multidigit integer division, as described by Knuth. */
        long q1 = divideUnsigned(x.c1, y);
        x = new UInt128(x.c0, x.c1 - q1 * y);  // x < y B
        int sh = Long.numberOfLeadingZeros(y);
        y <<= sh;  // B^2 / 2 ≤ y < B^2
        long yh0 = y & MASK;
        long yh1 = y >>> S;
        x = shiftLeftSmall(x, sh);
        long x0 = x.c0 & MASK;

        /*
         * Performs the 1st 3-by-2 B-based digits division, and therefore
         * never needs the last, extremely rare correcton described by Knuth.
         * The quotient meets q < B, the final remainder fits in 64 bits,
         * so can be computed in 64 bit arithmetic.
         *
         * Both x and y are also viewed as integers in radix B.
         * Since x < y B implies x < B^3, we get
         *      x = (xh0 + xh1 B) + xh2 B^2
         *      y = (yh0 + yh1 B), yh1 ≥ 2^31
         */
        x = shiftRightS(x);  // x < B^3
        long xh0 = x.c0 & MASK;
        long xh12 = x.c0 >>> S | x.c1 << S;  // xh12 = ⌊x / B⌋ ≤ x / B < y
        long q = divideUnsigned(xh12, yh1);  // q ≤ B + 1
        long r = xh12 - q * yh1;  // q yh1 ≤ (B + 1) (B - 1) < B^2, r < yh1 < B
        if (UInt64.lessThan(r << S | xh0, q * yh0)) {  // q yh0 < B^2
            q -= 1;
            r += yh1;  // r < 2 yh1 < 2 B
            if (r < B && UInt64.lessThan(r << S | xh0, q * yh0)) {
                q -= 1;
            }
        }
        long q0 = q;

        /* Performs the 2nd 3-by-2 B-based digits division. */
        x = shiftLeftSOr(new UInt128(x.c0 - q * y), x0);
        xh0 = x.c0 & MASK;
        xh12 = x.c0 >>> S | x.c1 << S;
        q = divideUnsigned(xh12, yh1);
        r = xh12 - q * yh1;
        if (UInt64.lessThan(r << S | xh0, q * yh0)) {
            q -= 1;
            r += yh1;
            if (r < B && UInt64.lessThan(r << S | xh0, q * yh0)) {
                q -= 1;
            }
        }
        return new Quo128Rem64(new UInt128(q0 << S | q, q1), x.c0 - q * y >>> sh);
    }

    /**
     * Returns the bit length of {@code x}.
     *
     * @param x the argument.
     * @return the result described above.
     */
    public static int len2(UInt128 x) {
        int sz = x.c1 != 0 ? SIZE : UInt64.SIZE;
        long c = x.c1 != 0 ? x.c1 : x.c0;
        return sz - Long.numberOfLeadingZeros(c);
    }

    /**
     * Returns min{e ∈ ℕ : {@code x} &lt; 10<sup>e</sup>}.
     *
     * @param x the parameter
     * @return the value as described above.
     */
    public static int len10(UInt128 x) {
        int e = UInt64.flog10pow2(len2(x));
        return lessThan(x, POW_10[e]) ? e : e + 1;
    }

    /**
     * Returns whether {@code x} = 0.
     *
     * @param x the parameter
     * @return the result described above.
     */
    public static boolean isZero(UInt128 x) {
        return (x.c1 | x.c0) == 0;
    }

    /**
     * Returns whether {@code x} = {@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static boolean equal(UInt128 x, UInt128 y) {
        return x.c1 == y.c1 & x.c0 == y.c0;
    }

    /**
     * Returns whether {@code x} &lt; {@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static boolean lessThan(UInt128 x, UInt128 y) {
        return x.c1 != y.c1
                ? UInt64.lessThan(x.c1, y.c1)
                : UInt64.lessThan(x.c0, y.c0);
    }

    /**
     * Returns whether {@code x} ≤ {@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static boolean lessEqual(UInt128 x, UInt128 y) {
        return !lessThan(y, x);
    }

    /**
     * Returns whether {@code x} > {@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static boolean greaterThan(UInt128 x, UInt128 y) {
        return lessThan(y, x);
    }

    /**
     * Returns whether {@code x} ≥ {@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static boolean greaterEqual(UInt128 x, UInt128 y) {
        return !lessThan(x, y);
    }

    @Override
    public int compareTo(UInt128 o) {
        return lessThan(this, o) ? -1 : equal(this, o) ? 0 : 1;
    }

    /**
     * Returns ~{@code x}.
     *
     * @param x the parameter
     * @return the result described above.
     */
    public static UInt128 not(UInt128 x) {
        return new UInt128(~x.c0, ~x.c1);
    }

    /**
     * Returns {@code x} &amp; {@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static UInt128 and(UInt128 x, UInt128 y) {
        return new UInt128(x.c0 & y.c0, x.c1 & y.c1);
    }

    /**
     * Returns {@code x} &amp; ~{@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static UInt128 andNot(UInt128 x, UInt128 y) {
        return new UInt128(x.c0 & ~y.c0, x.c1 & ~y.c1);
    }

    /**
     * Returns {@code x} | {@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static UInt128 or(UInt128 x, UInt128 y) {
        return new UInt128(x.c0 | y.c0, x.c1 | y.c1);
    }

    /**
     * Returns {@code x} ^ {@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static UInt128 xor(UInt128 x, UInt128 y) {
        return new UInt128(x.c0 ^ y.c0, x.c1 ^ y.c1);
    }

    /**
     * Returns {@code x} &lt;&lt; {@code sh}.
     *
     * @param x the argument.
     * @param sh only the {@link #LOG2_SIZE} least significant bits are relevant.
     * @return the result described above.
     */
    public static UInt128 shiftLeft(UInt128 x, int sh) {
        sh &= SIZE - 1;
        return sh >= 64
                ? new UInt128(0, x.c0 << sh)
                : shiftLeftSmall(x, sh);
    }

    /**
     * Returns {@code x} >>> {@code sh}.
     *
     * @param x the argument.
     * @param sh only the {@link #LOG2_SIZE} least significant bits are relevant.
     * @return the result described above.
     */
    public static UInt128 shiftRight(UInt128 x, int sh) {
        sh &= SIZE - 1;
        return sh >= 64
                ? new UInt128(x.c1 >>> sh)
                : shiftRightSmall(x, sh);
    }

    /*
     * Unsigned 192-bit integers.
     * This is just to support UInt128 division, not a fully fledged class.
     */
    @jdk.internal.MigratedValueClass
    @jdk.internal.ValueBased
    private static final class UInt192 {

        private static final int SIZE = 3 * Long.SIZE;

        /*
         * The value of the instance is
         *      c0 + c1 2^64 + c2 2^128
         */
        private final long c0;
        private final long c1;
        private final long c2;

        private UInt192(long c0, long c1, long c2) {
            this.c0 = c0;
            this.c1 = c1;
            this.c2 = c2;
        }

        /* For debugging purposes. */
        @Override
        public String toString() {
            return toBigInteger().toString();
        }

        /* For interchange. */
        private BigInteger toBigInteger() {
            byte[] ba = new byte[SIZE / Byte.SIZE];
            UInt64.putLong(ba, 0 * 8, c2);
            UInt64.putLong(ba, 1 * 8, c1);
            UInt64.putLong(ba, 2 * 8, c0);
            return new BigInteger(1, ba);
        }

        /**
         * Performs an integer division with remainder.
         *
         * @param x the dividend.
         * @param y the divisor.
         *          Must meet {@code y} ≥ 2<sup>64</sup>.
         * @return the result described above.
         */
        static Quo64Rem128 divideAndRemainder(UInt128 x, UInt128 y) {
            /* Multidigit integer division, as described by Knuth. */
            int sh = Long.numberOfLeadingZeros(y.c1);  // 0 ≤ sh < 2 S
            y = shiftLeftSmall(y, sh);  // B^4 / 2 ≤ y < B^4
            long yh2 = y.c1 & MASK;
            long yh3 = y.c1 >>> S;  // yh3 ≥ B / 2
            UInt192 xp = shiftLeft(x, sh);  // xp < B^6 / 2
            long xp0 = xp.c0 & MASK;

            /*
             * Performs the 1st of two 5-by-4 B-based digits division.
             *
             * Both xp and y are also viewed as integers in radix B.
             * As xp < B^6, after the right shift we get xp < B^5, thus
             *      xp = (xh0 + xh1 B) + (xh2 B^2 + xh3 B^3) + xh4 B^4, xh4 < B / 2
             *      y = (yh0 + yh1 B) + (yh2 B^2 + yh3 B^3), yh3 ≥ B / 2
             */
            xp = shiftRightS(xp);
            long xh2 = xp.c1 & MASK;
            long xh34 = xp.c2 << S | xp.c1 >>> S;  // xh3 + xh4 B < B^2 / 2
            long q = divideUnsigned(xh34, yh3);  // q < B
            long r = xh34 - q * yh3;  // r < yh3 < B
            if (UInt64.lessThan(r << S | xh2, q * yh2)) {
                q -= 1;
                r += yh3;  // r < 2 yh3 < 2 B
                if (r < B && UInt64.lessThan(r << S | xh2, q * yh2)) {
                    q -= 1;
                }
            }
            xp = subtract(xp, multiply(y, q));
            if (xp.c2 < 0) {
                q -= 1;
                xp = add(xp, y);
            }
            long q0 = q;  // q = qr.quo < B

            /* Performs the 2nd of two 5-by-4 B-based digits division. */
            xp = shiftLeftSOr(xp, xp0);
            xh2 = xp.c1 & MASK;
            xh34 = xp.c2 << S | xp.c1 >>> S;
            q = divideUnsigned(xh34, yh3);
            r = xh34 - q * yh3;
            if (UInt64.lessThan(r << S | xh2, q * yh2)) {
                q -= 1;
                r += yh3;
                if (r < B && UInt64.lessThan(r << S | xh2, q * yh2)) {
                    q -= 1;
                }
            }
            xp = subtract(xp, multiply(y, q));
            if (xp.c2 < 0) {
                q -= 1;
                xp = add(xp, y);
            }
            return new Quo64Rem128(q0 << S | q, shiftRightSmall(new UInt128(xp.c0, xp.c1), sh));
        }

        private static UInt192 shiftRightS(UInt192 x) {
            return new UInt192(
                    x.c0 >>> S | x.c1 << S,
                    x.c1 >>> S | x.c2 << S,
                    x.c2 >>> S);
        }

        private static UInt192 add(UInt192 x, UInt128 y) {
            long z0, z1, z2;
            z0 = x.c0 + y.c0;
            z1 = carry(z0, x.c0);
            z1 += x.c1;
            z2 = carry(z1, x.c1);
            z1 += y.c1;
            z2 += carry(z1, y.c1);
            z2 += x.c2;
            return new UInt192(z0, z1, z2);
        }

        private static UInt192 subtract(UInt192 x, UInt192 y) {
            long z0, z1, z2;
            z0 = x.c0 - y.c0;
            z1 = z2 = -carry(x.c0, y.c0);
            z1 += x.c1;
            z2 += carry(z1, x.c1);
            z2 -= carry(z1, y.c1);
            z1 -= y.c1;
            z2 += x.c2;
            z2 -= y.c2;
            return new UInt192(z0, z1, z2);
        }

        private static UInt192 shiftLeft(UInt128 x, int sh) {
            /* Branchless code. */
            return new UInt192(
                    x.c0 << sh,
                    x.c1 << sh | x.c0 >>> 63 - sh >>> 1,
                    x.c1 >>> 63 - sh >>> 1);
        }

        /**
         * Returns {@code x} &lt;&lt; 32 | {@code y}
         * @param x the 1st parameter
         * @param y the 2nd parameter
         * @return the result described above.
         */
        private static UInt192 shiftLeftSOr(UInt192 x, long y) {
            return new UInt192(
                    x.c0 << S | y,
                    x.c1 << S | x.c0 >>> S,
                    x.c1 >>> S);
        }

        /**
         * Returns {@code x}⋅{@code y}.
         *
         * @param x the 1st parameter
         * @param y the 2nd parameter
         * @return the result described above.
         */
        private static UInt192 multiply(UInt128 x, long y) {
            long z0, z1, z2, t0;
            z0 = x.c0 * y;
            z1 = Math.unsignedMultiplyHigh(x.c0, y);
            t0 = x.c1 * y;
            z2 = Math.unsignedMultiplyHigh(x.c1, y);
            z1 += t0;
            z2 += carry(z1, t0);
            return new UInt192(z0, z1, z2);
        }

    }

}
