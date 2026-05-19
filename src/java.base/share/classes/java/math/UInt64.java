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

/**
 * Static methods operating on unsigned longs.
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
@jdk.internal.MigratedValueClass
@jdk.internal.ValueBased
// TODO: once in the platform, declare as value class and remove useless final
public final class UInt64 {

    /*
     * Except when noted otherwise, this class assumes that long values
     * are unsigned.
     */

    /**
     * The number of bits used to represent a value of this class in binary form.
     */
    public static final int SIZE = Long.SIZE;

    /**
     * max{e ∈ ℕ : 10<sup>e</sup> ≤ 2<sup>{@link #SIZE}</sup>}
     */
    public static final int DEC_SIZE = 19;  // max{e ∈ ℤ: 10^e ≤ 2^SIZE}

    /*
     * For division, we need a 2 digits by 1 digit division primitive.
     * Unfortunately, the platform does not provide it for digits of
     * radix 2^SIZE.
     * Resort to a smaller base B = 2^S of S bits, and smaller digits.
     */
    static final int S = Integer.SIZE;
    static final long B = 1L << S;
    static final long MASK = B - 1;

    // TODO: once in the platform, add it to the AOT cache
    @Stable
    private static final long[] POW_10;

    static {
        POW_10 = new long[DEC_SIZE + 1];
        POW_10[0] = 1L;
        for (int i = 1; i < POW_10.length; ++i) {
            POW_10[i] = POW_10[i - 1] * 10L;
        }
    }

    private UInt64() {
    }

    /**
     * Returns 10<sup>{@code e}</sup>.
     *
     * @param e must meet 0 ≤ {@code e} ≤ {@link #DEC_SIZE}.
     * @return the result described above.
     * @throws RuntimeException if {@code e} is out of range.
     */
    public static long pow10(int e) {
        return POW_10[e];
    }

    /**
     * Returns 10<sup>{@code e}</sup>.
     *
     * @param e must meet 0 ≤ {@code e} ≤ {@link #DEC_SIZE}.
     * @return the result described above.
     * @throws RuntimeException if {@code e} is out of range.
     */
    public static long pow10(long e) {
        return POW_10[(int) e];
    }

    /**
     * Puts all 8 bytes of {@code c} into {@code ba} in big-endian order,
     * replacing the bytes starting at index {@code i}.
     *
     * @param ba the (big-endian ordered) container.
     * @param i the index to start at.
     * @param c the value to put.
     */
    static void putLong(byte[] ba, int i, long c) {
        ba[i] = (byte) (c >>> -1 * 8);
        ba[i + 1] = (byte) (c >>> -2 * 8);
        ba[i + 2] = (byte) (c >>> -3 * 8);
        ba[i + 3] = (byte) (c >>> -4 * 8);
        ba[i + 4] = (byte) (c >>> -5 * 8);
        ba[i + 5] = (byte) (c >>> -6 * 8);
        ba[i + 6] = (byte) (c >>> -7 * 8);
        ba[i + 7] = (byte) c;
    }

    /**
     * Fetches the returned value from {@code ba} in big-endian order,
     * reading 8 bytes starting at index {@code i}.
     *
     * @param ba the (big-endian ordered) container.
     * @param i the index to start at.
     * @return the fetched value.
     */
    static long getLong(byte[] ba, int i) {
        return (ba[i] & 0xffL) << -1 * 8
                | (ba[i + 1] & 0xffL) << -2 * 8
                | (ba[i + 2] & 0xffL) << -3 * 8
                | (ba[i + 3] & 0xffL) << -4 * 8
                | (ba[i + 4] & 0xffL) << -5 * 8
                | (ba[i + 5] & 0xffL) << -6 * 8
                | (ba[i + 6] & 0xffL) << -7 * 8
                | (ba[i + 7] & 0xffL);
    }

    /**
     * Returns the carry/borrow of a sum/difference.
     *
     * @param x the sum (for addition), or the minuend (for subtraction).
     * @param y an addend (for addition), or the subtrahend (for subtraction).
     * @return the carry/borrow, either 0 (no carry/borrow) or 1.
     */
    static long carry(long x, long y) {
        return lessThan(x, y) ? 1L : 0L;
    }

    /**
     * Returns {@code x} + {@code y}, throwing on overflow.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     * @throws ArithmeticException on overflow.
     */
    static long addExact(long x, long y) {
        long z = x + y;
        if (carry(z, x) == 0) {
            return z;
        }
        throw new ArithmeticException("overflow");
    }

    /**
     * Returns whether {@code x} &lt; {@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static boolean lessThan(long x, long y) {
        /* +Long.MIN_VALUE translates directly to unsigned comparison. */
        return (x + Long.MIN_VALUE) < (y + Long.MIN_VALUE);
    }

    /**
     * Returns whether {@code x} ≤ {@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static boolean lessEqual(long x, long y) {
        /* +Long.MIN_VALUE translates directly to unsigned comparison. */
        return (x + Long.MIN_VALUE) <= (y + Long.MIN_VALUE);
    }

    /**
     * Returns whether {@code x} > {@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static boolean greaterThan(long x, long y) {
        /* +Long.MIN_VALUE translates directly to unsigned comparison. */
        return (x + Long.MIN_VALUE) > (y + Long.MIN_VALUE);
    }

    /**
     * Returns whether {@code x} ≥ {@code y}.
     *
     * @param x the 1st parameter
     * @param y the 2nd parameter
     * @return the result described above.
     */
    public static boolean greaterEqual(long x, long y) {
        /* s+Long.MIN_VALUE translates directly to unsigned comparison. */
        return (x + Long.MIN_VALUE) >= (y + Long.MIN_VALUE);
    }

    private static final int Q = 21;
    private static final int C = 631_305;

    /**
     * Returns max{e ∈ ℤ : 10<sup>e</sup> ≤ 2<sup>{@code q}</sup>}.
     * Equivalently, returns ⌊{@code q} log<sub>10</sub>2⌋.
     *
     * @param q must meet |{@code q}| ≤ 2_135 (not checked).
     * @return the result described above.
     */
    static int flog10pow2(int q) {
        return q * C >> Q;
    }

    /**
     * Returns min{e ∈ ℕ : {@code c} &lt; 10<sup>e</sup>}.
     *
     * @param c the parameter
     * @return the value as described above.
     */
    public static int len10(long c) {
        int e = flog10pow2(Long.SIZE - Long.numberOfLeadingZeros(c));
        return lessThan(c, POW_10[e]) ? e : e + 1;
    }

}
