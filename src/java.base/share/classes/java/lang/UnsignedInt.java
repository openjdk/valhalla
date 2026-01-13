/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/**
 * Unsigned 32-bit integers.
 */
@jdk.internal.ValueBased
public class /*value record*/ UnsignedInt 
/* implements Integral<UnsignedInt> */ {
    private int value;

    private UnsignedInt(int value) {
        this.value = value;
    }

    /**
     * {@return lorem ipsum}
     * @param x lorem ipsum
     */
    public static UnsignedInt valueOf(int x) {
        return new UnsignedInt(x);
    }

    /**
     * {@return lorem ipsum}
     * @param x lorem ipsum
     * @throws ArithmeticException lorem ipsum
     */
    public static UnsignedInt valueOfExact(int x) {
        if (x < 0) {
            throw new ArithmeticException("Attempt to convert negative value to unsigned");
        }
        return new UnsignedInt(x);
    }

    /**
     * Addition operation, binary "{@code +}".
     *
     * @param addend the first operand
     * @param augend the second operand
     * @return the sum of the operands
     */
    public static UnsignedInt add(UnsignedInt addend, UnsignedInt augend) {
        // signed and unsigned addition is the same for 2's complement
        return new UnsignedInt(addend.value + augend.value);
    }

    /**
     * Subtraction operation, binary "{@code -}".
     * @param minuend the first operand
     * @param  subtrahend the second operand
     * @return the difference of the operands
     */
    public static UnsignedInt subtract(UnsignedInt minuend, UnsignedInt subtrahend) {
        // signed and unsigned subtraction is the same for 2's complement
        return new UnsignedInt(minuend.value - subtrahend.value);
    }

    /**
     * Multiplication operation, "{@code *}".
     *
     * @param multiplier the first operand
     * @param multiplicand the second operand
     * @return the product of the operands
     */
    public static UnsignedInt multiply(UnsignedInt multiplier,
                                       UnsignedInt multiplicand) {
        // signed and unsigned multiply is the same for 2's complement
        return new UnsignedInt(multiplier.value * multiplicand.value);
    }

    /**
     * Division operation, "{@code /}".
     *
     * @param dividend the value to be divided
     * @param divisor the value doing the dividing
     * @return the unsigned quotient of the first argument divided by
     * the second argument
     * @throws ArithmeticException if the divisor is zero
     * @see Integer#divideUnsigned(int, int)
     */
    public static UnsignedInt divide(UnsignedInt dividend,
                                     UnsignedInt divisor) {
        return new UnsignedInt(Integer.divideUnsigned(dividend.value,
                                                      divisor.value));
    }

    /**
     * Remainder operation, "{@code %}".
     *
     * @param dividend the value to be divided
     * @param divisor the value doing the dividing
     * @return the unsigned remainder of the first argument divided by
     * the second argument
     * @throws ArithmeticException if the divisor is zero
     * @see Integer#remainderUnsigned(int, int)
     */
    public static UnsignedInt remainder(UnsignedInt dividend,
                                        UnsignedInt divisor) {
        return new UnsignedInt((Integer.remainderUnsigned(dividend.value,
                                                          divisor.value)));
    }

    /**
     * Unary plus operation, "{@code +}".
     *
     * @param operand the operand
     * @return unary plus of the operand
     */
    public static UnsignedInt plus(UnsignedInt operand) {
        return operand;
    }

    /**
     * Negation operation, unary "{@code -}".
     * @param operand the operand
     * @return the negation of the operand
     */
    public static UnsignedInt negate(UnsignedInt operand) {
        // What does negating an unsigned int do in C?
        // From the draft C standard 6.5.3:
        //
        // "The result of the unary - operator is the negative of its
        // (promoted) operand. The integer promotions are performed on
        // the operand, and the result has the promoted type."

        throw new UnsupportedOperationException("Cannot negate unsigned value");
    }

    /**
     * {@return lorem ipsum}
     */
    @Override
    public String toString() {
        return toString(this);
    }

    /**
     * {@return lorem ipsum}
     * @param x lorem ipsum
     * @see Integer#toUnsignedString(int)
     */
    public static String toString(UnsignedInt x) {
        return Integer.toUnsignedString(x.value);
    }

    /**
     * {@return lorem ipsum}
     *
     * @param x lorem ipsum
     * @param radix lorem ipsum
     * @see Integer#toUnsignedString(int, int)
     */
    public static String toString(UnsignedInt x, int radix) {
        return Integer.toUnsignedString(x.value, radix);
    }

    /**
     * {@return lorem ipsum}
     * @param s lorem ipsum
     * @see Integer#parseUnsignedInt(String)
     */
    public static UnsignedInt valueOf(String s) {
        return new UnsignedInt(Integer.parseUnsignedInt(s));
    }

    /**
     * {@return lorem ipsum}
     *
     * @param      s   the string to be parsed.
     * @param      radix the radix to be used in interpreting {@code s}
     * @return     an {@code UnsignedInteger} object holding the value
     *             represented by the string argument in the specified
     *             radix.
     * @throws    NumberFormatException if the {@code String}
     *            does not contain a parsable unsigned integer.
     * @see Integer#parseUnsignedInt(String, int)
     */
    public static UnsignedInteger valueOf(String s, int radix) throws NumberFormatException {
        return new UnsignedInt(Integer.parseUnsignedInt(s, radix));
    }


    /**
     * {@return lorem ipsum}
     * @param x lorem ipsum
     * @param y lorem ipsum
     * @see Integer#compareUnsigned(int, int)
     */
    public static int compare(UnsignedInt x, UnsignedInt y) {
        return Integer.compareUnsigned(x.value, y.value);
    }

    // todo: replace ordered comparision implements with better code
    // from "Hackers Delight" or similar.

    // OrderedComparison
    // min, max, lessThan, lessThanEqual, greaterThan, greaterThanEqual,

    /**
     * {@return lorem ipsum}
     * @param x lorem ipsum
     * @param y lorem ipsum
     */
    public static boolean lessThan(UnsignedInt x, UnsignedInt y) {
        return Integer.compareUnsigned(x.value, y.value) < 0;
    }

    /**
     * {@return lorem ipsum}
     * @param x lorem ipsum
     * @param y lorem ipsum
     */
    public static boolean lessThanEqual(UnsignedInt x, UnsignedInt y) {
        return Integer.compareUnsigned(x.value, y.value) <= 0;
    }

    /**
     * {@return lorem ipsum}
     * @param x lorem ipsum
     * @param y lorem ipsum
     */
    public static boolean greaterThan(UnsignedInt x, UnsignedInt y) {
        return Integer.compareUnsigned(x.value, y.value) > 0;
    }

    /**
     * {@return lorem ipsum}
     * @param x lorem ipsum
     * @param y lorem ipsum
     */
    public static boolean greaterThanEqual(UnsignedInt x, UnsignedInt y) {
        return Integer.compareUnsigned(x.value, y.value) >= 0;
    }

    /**
     * {@return lorem ipsum}
     * @param x lorem ipsum
     * @param y lorem ipsum
     */
    public static UnsignedInt min(UnsignedInt x, UnsignedInt y) {
        return (Integer.compareUnsigned(x.value, y.value) <= 0) ? x : y;
    }

    /**
     * {@return lorem ipsum}
     * @param x lorem ipsum
     * @param y lorem ipsum
     */
    public static UnsignedInt max(UnsignedInt x, UnsignedInt y) {
        return (Integer.compareUnsigned(x.value, y.value) >= 0) ? x : y;
    }

    // Integral
    // and, or, xor, complement, leftShift, rightShift, rightShift unsigned...

    /**
     * {@return the bit-wise AND of the arguments}
     * @param x lorem ipsum
     * @param y lorem ipsum
     */
    public static UnsignedInt and(UnsignedInt x, UnsignedInt y) {
        return new UnsignedInt(x.value & y.value);
    }

    /**
     * {@return the bit-wise OR of the arguments}
     * @param x lorem ipsum
     * @param y lorem ipsum
     */
    public static UnsignedInt or(UnsignedInt x, UnsignedInt y) {
        return new UnsignedInt(x.value | y.value);
    }

    /**
     * {@return the bit-wise XOR of the arguments}
     * @param x lorem ipsum
     * @param y lorem ipsum
     */
    public static UnsignedInt xor(UnsignedInt x, UnsignedInt y) {
        return new UnsignedInt(x.value ^ y.value);
    }

    /**
     * {@return the bit-wise complement of the argument}
     * @param x lorem ipsum
     */
    public static UnsignedInt complement(UnsignedInt x) {
        return new UnsignedInt(~x.value);
    }

    // Per the JLS, the shiftDistance is AND-ed with a 5 or 6 bit mask
    // (for int and long value being shifted, respectively) so the
    // distance always non-negative.

    /**
     * {@return lorem ipsum}
     * @param x lorem ipsum
     * @param shiftDistance lorem ipsum
     * @jls 15.19 Shift Operators 
     */
    public static UnsignedInt leftShift(UnsignedInt x, int shiftDistance) {
        return new UnsignedInt(x.value << shiftDistance);
    }

    /**
     * {@return lorem ipsum}
     * @param x lorem ipsum
     * @param shiftDistance lorem ipsum
     * @jls 15.19 Shift Operators 
     */
    public static UnsignedInt rightShift(UnsignedInt x, int shiftDistance) {
        // Shifts of an unsigned value are always unsigned; use >>> *not* >>.
        return new UnsignedInt(x.value >>> shiftDistance);
    }

    /**
     * {@return lorem ipsum}
     * @param x lorem ipsum
     * @param shiftDistance lorem ipsum
     * @jls 15.19 Shift Operators 
     */
    public static UnsignedInt rightShiftUnsigned(UnsignedInt x, int shiftDistance) {
        // Shifts of an unsigned value are always unsigned; use >>> *not* >>.
        return new UnsignedInt(x.value >>> shiftDistance);
    }

    /**
     * Returns the value of this {@code Integer} as a {@code long}
     * after a widening primitive conversion.
     *
     * @param  x the value to convert to an unsigned {@code long}
     * @return the argument converted to {@code long} by an unsigned
     *         conversion
     * @see Integer#toUnsignedLong(int)
     */
    public static long longValue(UnsignedInt x) {
        return Integer.toUnsignedLong(x.value);
    }
}
