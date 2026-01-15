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
 * Unsigned 32-bit two's complement integers.
 *
 * @see Integer
 */
@jdk.internal.ValueBased
public final class /*value record*/ UnsignedInt  {
    // In the future, expect to add "implements Integral<UnsignedInt>"
    // (or similar).
    // Currently *not* extending java.lang.Number, and, for now, *not*
    // implementing Serializable. Might implement Comparable<UnsignedInt>.

    /**
     * The bits of the unsigned value.
     */
    private int value;

    /**
     * Constructs a new unsigned int.
     */
    private UnsignedInt(int value) {
        this.value = value;
    }

    /**
     * {@return an unsigned integer with the bits of the argument}
     *
     * @param x the argument
     */
    public static UnsignedInt valueOf(int x) {
        return new UnsignedInt(x);
    }

    /**
     * {@return an unsigned integer with the bits of the argument if
     * the argument is zero or positive}
     *
     * @param x the argument
     * @throws ArithmeticException if the argument as an {@code int} is negative
     */
    public static UnsignedInt valueOfExact(int x) {
        if (x < 0) {
            throw new ArithmeticException("Attempt to convert negative value to unsigned");
        }
        return new UnsignedInt(x);
    }

    /**
     * {@return the result of parsing the string as an unsigned
     * integer} Base 10 is used as the implicit radix.
     *
     * @implSpec
     * The same grammar of strings is recognized by this method as by
     * {@link Integer#parseUnsignedInt(String)}.
     *
     * @param s the argument
     * @throws    NumberFormatException  if the string does not contain a
     *            parsable unsigned integer.
     * @see Integer#parseUnsignedInt(String)
     */
    public static UnsignedInt valueOf(String s) throws NumberFormatException {
        return new UnsignedInt(Integer.parseUnsignedInt(s));
    }

    /**
     * {@return the result of parsing the string as an unsigned
     * integer in the specified radix}
     *
     * @implSpec
     * The same grammar of strings is recognized by this method as by
     * {@link Integer#parseUnsignedInt(String, int)}.
     *
     * @param      s   the string to be parsed.
     * @param      radix the radix to be used in interpreting {@code s}
     * @throws    NumberFormatException if the {@code String}
     *            does not contain a parsable unsigned integer.
     * @see Integer#parseUnsignedInt(String, int)
     */
    public static UnsignedInt valueOf(String s, int radix) throws NumberFormatException {
        return new UnsignedInt(Integer.parseUnsignedInt(s, radix));
    }

    /**
     * {@return a string representing the unsigned value}
     */
    @Override
    public String toString() {
        return toString(this);
    }

    /**
     * {@return a string representing the unsigned argument}
     *
     * @implSpec
     * The method behaves as if the argument were passed to {@link
     * Integer#toUnsignedString(int)}.
     *
     * @param x the unsigned integer to be represented
     * @see Integer#toUnsignedString(int)
     */
    public static String toString(UnsignedInt x) {
        return Integer.toUnsignedString(x.value);
    }

    /**
     * {@return a string representing the unsigned argument in the
     * specified radix}
     *
     * @implSpec
     * The method behaves as if the arguments were passed to {@link
     * Integer#toUnsignedString(int, int)}.
     *
     * @param x the unsigned integer to be represented
     * @param radix the radix to use in the string representation
     * @see Integer#toUnsignedString(int, int)
     */
    public static String toString(UnsignedInt x, int radix) {
        return Integer.toUnsignedString(x.value, radix);
    }

    /**
     * {@return the bits of this unsigned integer as an {@code int}}
     */
    public int intValue() {
        return this.value;
    }

    /**
     * {@return the value of this unsigned integer as a {@code long}}
     *
     * @see Integer#toUnsignedLong(int)
     */
    public long longValue() {
        return Integer.toUnsignedLong(this.value);
    }

    // Arithmetic operators

    /**
     * Addition operation, binary "{@code +}".
     *
     * @param addend the first operand
     * @param augend the second operand
     * @return the sum of the operands
     */
    public static UnsignedInt add(UnsignedInt addend, UnsignedInt augend) {
        // signed and unsigned addition is the same for 2's complement
        return valueOf(addend.value + augend.value);
    }

    /**
     * Subtraction operation, binary "{@code -}".
     *
     * @param minuend the first operand
     * @param subtrahend the second operand
     * @return the difference of the operands
     */
    public static UnsignedInt subtract(UnsignedInt minuend, UnsignedInt subtrahend) {
        // signed and unsigned subtraction is the same for 2's complement
        return valueOf(minuend.value - subtrahend.value);
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
        return valueOf(multiplier.value * multiplicand.value);
    }

    /**
     * Division operation, "{@code /}".
     *
     * @param dividend the value to be divided
     * @param divisor the value being divided by
     * @return the unsigned quotient of the first argument divided by
     * the second argument
     * @throws ArithmeticException if the divisor is zero
     * @see Integer#divideUnsigned(int, int)
     */
    public static UnsignedInt divide(UnsignedInt dividend,
                                     UnsignedInt divisor) {
        return valueOf(Integer.divideUnsigned(dividend.value, divisor.value));
    }

    /**
     * Remainder operation, "{@code %}".
     *
     * @param dividend the value to be divided to compute the remainder
     * @param divisor the value being divided by
     * @return the unsigned remainder of the first argument divided by
     * the second argument
     * @throws ArithmeticException if the divisor is zero
     * @see Integer#remainderUnsigned(int, int)
     */
    public static UnsignedInt remainder(UnsignedInt dividend,
                                        UnsignedInt divisor) {
        return valueOf((Integer.remainderUnsigned(dividend.value, divisor.value)));
    }

    // TODO: API decision, is this method needed?

    /**
     * Unary plus operation, "{@code +}".
     *
     * @param operand the operand
     * @return unary plus of the operand
     */
    public static UnsignedInt plus(UnsignedInt operand) {
        return operand;
    }

    // TODO: API discussion, is this method needed?
    //
    // If there is a single interface defining the operations over
    // integral types, negation should be defined over unsigned
    // values. If there are separate interfaces for signed and
    // unsigned integral types, the negate method can be elided on
    // unsigned types.

    /**
     * Negation operation, unary "{@code -}".
     * Unconditionally throws {@code UnsupportedOperationException}.
     *
     * @param operand the operand
     * @return the negation of the operand
     * @throws UnsupportedOperationException for all inputs
     */
    public static UnsignedInt negate(UnsignedInt operand) {
        // What does negating an unsigned int do in C?
        // From the draft C standard 6.5.3:
        //
        // "The result of the unary - operator is the negative of its
        // (promoted) operand. The integer promotions are performed on
        // the operand, and the result has the promoted type."

        // Could special case 0?
        throw new UnsupportedOperationException("Cannot negate unsigned value");
    }

    /**
     * Compares two unsigned int values numerically. 
     *
     * @param x the first argument
     * @param y the second argument
     * @return the value {@code 0} if {@code x == y};
     *         a value less than {@code 0} if {@code x < y}; and
     *         a value greater than {@code 0} if {@code x > y}
     * @see Integer#compareUnsigned(int, int)
     */
    public static int compare(UnsignedInt x, UnsignedInt y) {
        return Integer.compareUnsigned(x.value, y.value);
    }

//     /**
//      * {@return lorem ipsum}
//      *
//      * @param y the argument to compare to
//      * @see Integer#compareUnsigned(int, int)
//      */
//     @Override
//     public int compareTo(UnsignedInt y) {
//         return compare(this, y);
//     }

    // TODO: replace ordered comparison implementations with better code
    // from "Hackers Delight" or similar.

    // Integral-specific operations
    // and, or, xor, complement, leftShift, rightShift, rightShift unsigned...

    // OrderedComparison
    // min, max, lessThan, lessThanEqual, greaterThan, greaterThanEqual,

    // Ordered comparison operators

    /**
     * {@return {@code true} if the first argument is less than the
     * second argument and {@code false} otherwise}
     *
     * The method corresponds to the less than operator, "{@code <}".
     *
     * @param x the first argument
     * @param y the second argument
     */
    public static boolean lessThan(UnsignedInt x, UnsignedInt y) {
        return compare(x, y) < 0;
    }

    /**
     * {@return {@code true} if the first argument is less than or
     * equal to the second argument and {@code false} otherwise}
     *
     * The method corresponds to the less than or equal to operator,
     * "{@code <=}".
     *
     * @param x the first argument
     * @param y the second argument
     */
    public static boolean lessThanEqual(UnsignedInt x, UnsignedInt y) {
        return compare(x, y) <= 0;
    }

    /**
     * {@return {@code true} if the first argument is greater than the
     * second argument and {@code false} otherwise}
     *
     * The method corresponds to the less than operator, "{@code >}".
     *
     * @param x the first argument
     * @param y the second argument
     */
    public static boolean greaterThan(UnsignedInt x, UnsignedInt y) {
        return compare(x, y) > 0;
    }

    /**
     * {@return {@code true} if the first argument is greater than or
     * equal to the second argument and {@code false} otherwise}
     *
     * The method corresponds to the greater than or equal to operator,
     * "{@code >=}".
     *
     * @param x the first argument
     * @param y the second argument
     */
    public static boolean greaterThanEqual(UnsignedInt x, UnsignedInt y) {
        return compare(x, y) >= 0;
    }

    /**
     * {@return the smaller of the two arguments}.  If the arguments
     * have the same value, the result is that same value.
     *
     * @param x the first argument
     * @param y the second argument
     */
    public static UnsignedInt min(UnsignedInt x, UnsignedInt y) {
        return (compare(x, y) <= 0) ? x : y;
    }

    /**
     * {@return the larger of the two arguments}.  If the arguments
     * have the same value, the result is that same value.
     *
     * @param x the first argument
     * @param y the second argument
     */
    public static UnsignedInt max(UnsignedInt x, UnsignedInt y) {
        return (compare(x, y) >= 0) ? x : y;
    }

    // Bit-wise operators

    /**
     * {@return the bit-wise AND of the arguments}
     *
     * The method corresponds to the AND operator, "{@code &}".
     *
     * @param x the first argument
     * @param y the second argument
     */
    public static UnsignedInt and(UnsignedInt x, UnsignedInt y) {
        return valueOf(x.value & y.value);
    }

    /**
     * {@return the bit-wise OR of the arguments}
     *
     * The method corresponds to the OR operator, "{@code |}".
     *
     * @param x the first argument
     * @param y the second argument
     */
    public static UnsignedInt or(UnsignedInt x, UnsignedInt y) {
        return valueOf(x.value | y.value);
    }

    /**
     * {@return the bit-wise XOR of the arguments}
     *
     * The method corresponds to the XOR operator, "{@code ^}".
     *
     * @param x the first argument
     * @param y the second argument
     */
    public static UnsignedInt xor(UnsignedInt x, UnsignedInt y) {
        return valueOf(x.value ^ y.value);
    }

    /**
     * {@return the bit-wise complement of the argument}
     *
     * The method corresponds to the complement operator, "{@code ~}".
     *
     * @param x the argument
     */
    public static UnsignedInt complement(UnsignedInt x) {
        return valueOf(~x.value);
    }

    // Shift Operators

    // Per the JLS, the shiftDistance is AND-ed with a 5 or 6 bit mask
    // (for int and long value being shifted, respectively) so the
    // distance always non-negative.

    /**
     * {@return the first operand left shifted by the distance
     * indicated by the second operand, operator "{@code <<"}}
     *
     * Only the value of the five low-order bits of the shift distance
     * argument are taken into account in determining the shift
     * distance.
     *
     * The method corresponds to the shift left operator, "{@code <<}".
     *
     * @param x the value to be shifted
     * @param shiftDistance number of bits to shift
     * @jls 15.19 Shift Operators 
     */
    public static UnsignedInt shiftLeft(UnsignedInt x, int shiftDistance) {
        return valueOf(x.value << shiftDistance);
    }

    /**
     * {@return the first operand right shifted by the distance
     * indicated by the second operand}
     *
     * This method corresponds to the shift right operator, "{@code >>}".
     *
     * Note: since this the is unsigned, semantically a (signed) right
     * shift is equivalent to an <em>unsigned</em> right shift.
     *
     * <p>Only the value of the five low-order bits of the shift distance
     * argument are taken into account in determining the shift
     * distance.
     *
     * @param x the value to be shifted
     * @param shiftDistance number of bits to shift
     * @jls 15.19 Shift Operators 
     */
    public static UnsignedInt shiftRight(UnsignedInt x, int shiftDistance) {
        // Shifts of an unsigned value are always unsigned; use >>> *not* >>.
        return valueOf(x.value >>> shiftDistance);
    }

    /**
     * {@return the first operand right shifted (unsigned) by the distance
     * indicated by the second operand}
     *
     * This method corresponds to the shift right unsigned operator, "{@code >>>}".
     *
     * <p>Only the value of the five low-order bits of the shift distance
     * argument are taken into account in determining the shift
     * distance.
     *
     * @param x the value to be shifted
     * @param shiftDistance  number of bits to shift
     * @jls 15.19 Shift Operators 
     */
    public static UnsignedInt shiftRightUnsigned(UnsignedInt x, int shiftDistance) {
        // Shifts of an unsigned value are always unsigned; use >>> *not* >>.
        return valueOf(x.value >>> shiftDistance);
    }
}
