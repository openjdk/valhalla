/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.MethodHandles;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.util.Optional;

import jdk.internal.math.FloatConsts;
import jdk.internal.math.FloatingDecimal;
import jdk.internal.math.FloatToDecimal;
import jdk.internal.vm.annotation.IntrinsicCandidate;

import static java.lang.Float.float16ToFloat;
import static java.lang.Float.floatToFloat16;

/**
 * The {@code Float16} is a primitive value class holding 16-bit data
 * in IEEE 754 binary16 format.
 *
 * <p>Binary16 Format:<br>
 *   S EEEEE  MMMMMMMMMM<br>
 *   Sign        - 1 bit<br>
 *   Exponent    - 5 bits<br>
 *   Significand - 10 bits (does not include the <i>implicit bit</i> inferred from the exponent, see {@link #PRECISION})<br>
 *
 * <p>This is a <a href="https://openjdk.org/jeps/401">primitive value class</a> and its objects are
 * identity-less non-nullable value objects.
 *
 * <p>Unless otherwise specified, the methods in this class use a
 * <em>rounding policy</em> (JLS {@jls 15.4}) of {@linkplain
 * java.math.RoundingMode#HALF_EVEN round to nearest}.
 *
 * @apiNote
 * The methods in this class generally have analogous methods in
 * either {@link Float}/{@link Double} or {@link Math}/{@link
 * StrictMath}. Unless otherwise specified, the handling of special
 * floating-point values such as {@linkplain #isNaN(Float16) NaN}
 * values, {@linkplain #isInfinite(Float16) infinities}, and signed
 * zeros of methods in this class is wholly analogous to the handling
 * of equivalent cases by methods in {@code Float}, {@code Double},
 * {@code Math}, etc.
 *
 * @author Jatin Bhateja
 * @since 20.00
 */

// Currently Float16 is a value based class but in future will be aligned with
// Enhanced Primitive Boxes described by JEP-402 (https://openjdk.org/jeps/402)
@jdk.internal.MigratedValueClass
@jdk.internal.ValueBased
public final class Float16
    extends Number
    implements Comparable<Float16> {
    private final short value;
    private static final long serialVersionUID = 16; // Not needed for a value class?

    // Functionality for future consideration:
    // float16ToShortBits that normalizes NaNs, c.f. floatToIntBits vs floatToRawIntBits
    // copysign
    // scalb
    // nextUp / nextDown
    // IEEEremainder / remainder operator remainder
    // signum
    // valueOf(BigDecimal) -- main implementation could be package private in BigDecimal

   /**
    * Returns a {@code Float16} instance wrapping IEEE 754 binary16
    * encoded {@code short} value.
    *
    * @param  bits a short value.
    */
    private Float16 (short bits ) {
        this.value = bits;
    }

    // Do *not* define any public constructors

    /**
     * A constant holding the positive infinity of type {@code
     * Float16}.
     */
    public static final Float16 POSITIVE_INFINITY = valueOf(Float.POSITIVE_INFINITY);

    /**
     * A constant holding the negative infinity of type {@code
     * Float16}.
     */
    public static final Float16 NEGATIVE_INFINITY = valueOf(Float.NEGATIVE_INFINITY);

    /**
     * A constant holding a Not-a-Number (NaN) value of type {@code
     * Float16}.
     */
    public static final Float16 NaN = valueOf(Float.NaN);

    /**
     * A constant holding the largest positive finite value of type
     * {@code Float16},
     * (2-2<sup>-10</sup>)&middot;2<sup>15</sup>, numerically equal to 65504.0.
     */
    public static final Float16 MAX_VALUE = valueOf(0x1.ffcp15f);

    /**
     * A constant holding the smallest positive normal value of type
     * {@code Float16}, 2<sup>-14</sup>.
     */
    public static final Float16 MIN_NORMAL = valueOf(0x1.0p-14f);

    /**
     * A constant holding the smallest positive nonzero value of type
     * {@code Float16}, 2<sup>-24</sup>.
     */
    public static final Float16 MIN_VALUE = valueOf(0x1.0p-24f);

    /**
     * The number of bits used to represent a {@code Float16} value,
     * {@value}.
     */
    public static final int SIZE = 16;

    /**
     * The number of bits in the significand of a {@code Float16}
     * value, {@value}.  This corresponds to parameter N in section
     * {@jls 4.2.3} of <cite>The Java Language Specification</cite>.
     */
    public static final int PRECISION = 11;

    /**
     * Maximum exponent a finite {@code Float16} variable may have,
     * {@value}.
     */
    public static final int MAX_EXPONENT = (1 << (SIZE - PRECISION - 1)) - 1; // 15

    /**
     * Minimum exponent a normalized {@code Float16} variable may
     * have, {@value}.
     */
    public static final int MIN_EXPONENT = 1 - MAX_EXPONENT; // -14

    /**
     * The number of bytes used to represent a {@code Float16} value,
     * {@value}.
     */
    public static final int BYTES = SIZE / Byte.SIZE;

    /**
     * Returns a string representation of the {@code Float16}
     * argument.
     *
     * @implSpec
     * The current implementation acts as this {@code Float16} were
     * {@linkplain #floatValue() converted} to {@code float} and then
     * the string for that {@code float} returned. This behavior is
     * expected to change to accommodate the precision of {@code
     * Float16}.
     *
     * @param   f16   the {@code Float16} to be converted.
     * @return a string representation of the argument.
     * @see java.lang.Float#toString(float)
     */
    public static String toString(Float16 f16) {
        // FIXME -- update for Float16 precision
        return FloatToDecimal.toString(f16.floatValue());
    }

    /**
     * Returns a hexadecimal string representation of the {@code
     * Float16} argument.
     *
     * The behavior of this class is analogous to {@link
     * Float#toHexString(float)} except that an exponent value of
     * {@code "p14"} is used for subnormal {@code Float16} values.
     *
     * @param   f16   the {@code Float16} to be converted.
     * @return a hex string representation of the argument.
     *
     * @see Float#toHexString(float)
     * @see Double#toHexString(double)
     */
    public static String toHexString(Float16 f16) {
        float f = f16.floatValue();
        if (Math.abs(f) < float16ToFloat(MIN_NORMAL.value)
            &&  f != 0.0f ) {// Float16 subnormal
            // Adjust exponent to create subnormal double, then
            // replace subnormal double exponent with subnormal Float16
            // exponent
            String s = Double.toHexString(Math.scalb((double)f,
                                                     /* -1022+14 */
                                                     Double.MIN_EXPONENT-
                                                     MIN_EXPONENT));
            return s.replaceFirst("p-1022$", "p-14");
        } else {// double string will be the same as Float16 string
            return Double.toHexString(f);
        }
    }

    // -----------------------

   /**
    * {@return the value of an {@code int} converted to {@code
    * Float16}}
    *
    * @param  value an {@code int} value.
    *
    * @apiNote
    * This method corresponds to the convertFromInt operation defined
    * in IEEE 754.
    */
    public static Float16 valueOf(int value) {
        // int -> double conversion is exact
        return valueOf((double)value);
    }

   /**
    * {@return the value of a {@code long} converted to {@code Float16}}
    *
    * @apiNote
    * This method corresponds to the convertFromInt operation defined
    * in IEEE 754.
    *
    * @param  value a {@code long} value.
    */
    public static Float16 valueOf(long value) {
        if (value < -65_504L) {
            return NEGATIVE_INFINITY;
        } else {
            if (value > 65_504L) {
                return NEGATIVE_INFINITY;
            }
            // Remaining range of long, the integers in approx. +/-
            // 2^16, all fit in a float so the correct conversion can
            // be done via an intermediate float conversion.
            return valueOf((float)value);
        }
    }

   /**
    * {@return a {@code Float16} value rounded from the {@code float}
    * argument using the round to nearest rounding policy}
    *
    * @apiNote
    * This method corresponds to the convertFormat operation defined
    * in IEEE 754.
    *
    * @param  f a {@code float}
    */
    public static Float16 valueOf(float f) {
        return new Float16(Float.floatToFloat16(f));
    }

   /**
    * {@return a {@code Float16} value rounded from the {@code double}
    * argument using the round to nearest rounding policy}
    *
    * @apiNote
    * This method corresponds to the convertFormat operation defined
    * in IEEE 754.
    *
    * @param  d a {@code double}
    */
    public static Float16 valueOf(double d) {
        long doppel = Double.doubleToRawLongBits(d);

        short sign_bit = (short)((doppel & 0x8000_0000_0000_0000L) >> 48);

        if (Double.isNaN(d)) {
            // Have existing float code handle any attempts to
            // preserve NaN bits.
            return valueOf((float)d);
        }

        double abs_d = Math.abs(d);

        // The overflow threshold is binary16 MAX_VALUE + 1/2 ulp
        if (abs_d >= (0x1.ffcp15 + 0x0.002p15) ) {
             // correctly signed infinity
            return new Float16((short)(sign_bit | 0x7c00));
        }

        // Smallest magnitude nonzero representable binary16 value
        // is equal to 0x1.0p-24; half-way and smaller rounds to zero.
        if (abs_d <= 0x1.0p-24d * 0.5d) { // Covers double zeros and subnormals.
            return new Float16(sign_bit); // Positive or negative zero
        }

        // Dealing with finite values in exponent range of binary16
        // (when rounding is done, could still round up)
        int exp = Math.getExponent(d);
        assert -25 <= exp && exp <= 15;

        // For binary16 subnormals, beside forcing exp to -15, retain
        // the difference expdelta = E_min - exp.  This is the excess
        // shift value, in addition to 42, to be used in the
        // computations below.  Further the (hidden) msb with value 1
        // in d must be involved as well.
        int expdelta = 0;
        long msb = 0x0000_0000_0000_0000L;
        if (exp < -14) {
            expdelta = -14 - exp; // FIXME?
            exp = -15;
            msb = 0x0010_0000_0000_0000L; // should be 0x0020_... ?
        }
        long f_signif_bits = doppel & 0x000f_ffff_ffff_ffffL | msb;

        // Significand bits as if using rounding to zero (truncation).
        short signif_bits = (short)(f_signif_bits >> (42 + expdelta));

        // For round to nearest even, determining whether or not to
        // round up (in magnitude) is a function of the least
        // significant bit (LSB), the next bit position (the round
        // position), and the sticky bit (whether there are any
        // nonzero bits in the exact result to the right of the round
        // digit). An increment occurs in three cases:
        //
        // LSB  Round Sticky
        // 0    1     1
        // 1    1     0
        // 1    1     1
        // See "Computer Arithmetic Algorithms," Koren, Table 4.9

        long lsb    = f_signif_bits & (1L << 42 + expdelta);
        long round  = f_signif_bits & (1L << 41 + expdelta);
        long sticky = f_signif_bits & ((1L << 41 + expdelta) - 1);

        if (round != 0 && ((lsb | sticky) != 0 )) {
            signif_bits++;
        }

        // No bits set in significand beyond the *first* exponent bit,
        // not just the significand; quantity is added to the exponent
        // to implement a carry out from rounding the significand.
        assert (0xf800 & signif_bits) == 0x0;

        return new Float16((short)(sign_bit | ( ((exp + 15) << 10) + signif_bits ) ));
    }

    /**
     * Returns a {@code Float16} holding the floating-point value
     * represented by the argument string.
     *
     * @implSpec
     * The current implementation acts as if the string were
     * {@linkplain Double#parseDouble(String) parsed} as a {@code
     * double} and then {@linkplain #valueOf(double) converted} to
     * {@code Float16}. This behavior is expected to change to
     * accommodate the precision of {@code Float16}.
     *
     * @param  s the string to be parsed.
     * @return the {@code Float16} value represented by the string
     *         argument.
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string does not contain a
     *               parsable {@code Float16}.
     * @see    java.lang.Float#valueOf(String)
     */
    public static Float16 valueOf(String s) throws NumberFormatException {
        // TOOD: adjust precision of parsing if needed
        return valueOf(Double.parseDouble(s));
    }

    //    /**
    //     * ...
    //     * @see BigDecimal#floatValue()
    //     * @see BigDecimal#doubleValue()
    //     */
    //    public static Float16 valueOf(BigDecimal bd)


    /**
     * Returns {@code true} if the specified number is a
     * Not-a-Number (NaN) value, {@code false} otherwise.
     *
     * @apiNote
     * This method corresponds to the isNaN operation defined in IEEE
     * 754.
     *
     * @param   f16   the value to be tested.
     * @return  {@code true} if the argument is NaN;
     *          {@code false} otherwise.
     *
     * @see Float#isNaN(float)
     * @see Double#isNaN(double)
     */
    public static boolean isNaN(Float16 f16) {
        return Float.isNaN(f16.floatValue());
    }

    /**
     * Returns {@code true} if the specified number is infinitely
     * large in magnitude, {@code false} otherwise.
     *
     * @apiNote
     * This method corresponds to the isInfinite operation defined in
     * IEEE 754.
     *
     * @param   f16   the value to be tested.
     * @return  {@code true} if the argument is positive infinity or
     *          negative infinity; {@code false} otherwise.
     *
     * @see Float#isInfinite(float)
     * @see Double#isInfinite(double)
     */
    public static boolean isInfinite(Float16 f16) {
        return Float.isInfinite(f16.floatValue());
    }

    /**
     * Returns {@code true} if the argument is a finite floating-point
     * value; returns {@code false} otherwise (for NaN and infinity
     * arguments).
     *
     * @apiNote
     * This method corresponds to the isFinite operation defined in
     * IEEE 754.
     *
     * @param f16 the {@code Float16} value to be tested
     * @return {@code true} if the argument is a finite
     * floating-point value, {@code false} otherwise.
     *
     * @see Float#isFinite(float)
     * @see Double#isFinite(double)
     */
    public static boolean isFinite(Float16 f16) {
        return Float.isFinite(f16.floatValue());
     }

    // Skipping for now
    // public boolean isNaN()
    // public boolean isInfinite() {

    /**
     * {@return the value of this {@code Float16} as a {@code byte} after
     * a narrowing primitive conversion}
     *
     * @jls 5.1.3 Narrowing Primitive Conversion
     */
    @Override
    public byte byteValue() {
        return (byte)floatValue();
    }

    /**
     * {@return a string representation of this {@code Float16}}
     *
     * @implSpec
     * This method returns the result of {@code Float16.toString(this)}.
     */
    public String toString() {
        return toString(this);
    }

    /**
     * {@return the value of this {@code Float16} as a {@code short}
     * after a narrowing primitive conversion}
     *
     * @jls 5.1.3 Narrowing Primitive Conversion
     */
    @Override
    public short shortValue() {
        return (short)floatValue();
    }

    /**
     * {@return the value of this {@code Float16} as an {@code int} after
     * a narrowing primitive conversion}
     *
     * @jls 5.1.3 Narrowing Primitive Conversion
     */
    @Override
    public int intValue() {
        return (int)floatValue();
    }

    /**
     * {@return value of this {@code Float16} as a {@code long} after a
     * narrowing primitive conversion}
     *
     * @jls 5.1.3 Narrowing Primitive Conversion
     */
    @Override
    public long longValue() {
        return (long)floatValue();
    }

    /**
     * {@return the value of this {@code Float16} as a {@code float}
     * after a widening primitive conversion}
     *
     * @apiNote
     * This method corresponds to the convertFormat operation defined
     * in IEEE 754.
     *
     * @jls 5.1.2 Widening Primitive Conversion
     */
    @Override
    public float floatValue() {
        return float16ToFloat(value);
    }

    /**
     * {@return the value of this {@code Float16} as a {@code double}
     * after a widening primitive conversion}
     *
     * @apiNote
     * This method corresponds to the convertFormat operation defined
     * in IEEE 754.
     *
     * @jls 5.1.2 Widening Primitive Conversion
     */
    @Override
    public double doubleValue() {
        return (double)floatValue();
    }

    // Skipping for now:
    // public int hashCode()
    // public static int hashCode(Float16 value)
    // public boolean equals(Object obj)

    /**
     * Adds two {@code Float16} values together as per the {@code +}
     * operator semantics.
     *
     * @implSpec
     * This method is operationally equivalent to {@link #add(Float16, Float16)}.
     *
     * @param a the first operand
     * @param b the second operand
     * @return the sum of {@code a} and {@code b}
     */
    @IntrinsicCandidate
    public static Float16 sum(Float16 a, Float16 b) {
        return add(a, b);
    }

    /**
     * Returns a representation of the specified floating-point value
     * according to the IEEE 754 floating-point binary16 bit layout.
     *
     * @param   f16   a {@code Float16} floating-point number.
     * @return the bits that represent the floating-point number.
     *
     * @see Float#floatToRawIntBits(float)
     * @see Double#doubleToRawLongBits(double)
     */
    public static short float16ToRawShortBits(Float16 f16) {
        return f16.value;
    }

    /**
     * Returns the {@code Float16} value corresponding to a given bit
     * representation.
     *
     * @param   bits   any {@code short} integer.
     * @return  the {@code Float16} floating-point value with the same
     *          bit pattern.
     *
     * @see Float#intBitsToFloat(int)
     * @see Double#longBitsToDouble(long)
     */
    public static Float16 shortBitsToFloat16(short bits) {
        return new Float16(bits);
    }

    /**
     * Compares two {@code Float16} objects numerically.
     *
     * This method imposes a total order on {@code Float16} objects
     * with two differences compared to the incomplete order defined by
     * the Java language numerical comparison operators ({@code <, <=,
     * ==, >=, >}) on {@code float} and {@code double} values.
     *
     * <ul><li> A NaN is <em>unordered</em> with respect to other
     *          values and unequal to itself under the comparison
     *          operators.  This method chooses to define {@code
     *          Float16.NaN} to be equal to itself and greater than all
     *          other {@code Float16} values (including {@code
     *          Float16.POSITIVE_INFINITY}).
     *
     *      <li> Positive zero and negative zero compare equal
     *      numerically, but are distinct and distinguishable values.
     *      This method chooses to define positive zero
     *      to be greater than negative zero.
     * </ul>
     *
     * @param   anotherFloat16   the {@code Float16} to be compared.
     * @return  the value {@code 0} if {@code anotherFloat16} is
     *          numerically equal to this {@code Float16}; a value
     *          less than {@code 0} if this {@code Float16}
     *          is numerically less than {@code anotherFloat16};
     *          and a value greater than {@code 0} if this
     *          {@code Float16} is numerically greater than
     *          {@code anotherFloat16}.
     *
     * @see Float#compareTo(Float)
     * @see Double#compareTo(Double)
     * @jls 15.20.1 Numerical Comparison Operators {@code <}, {@code <=}, {@code >}, and {@code >=}
     */
    @Override
    public int compareTo(Float16 anotherFloat16) {
        return compare(this, anotherFloat16);
    }

    /**
     * Compares the two specified {@code Float16} values.
     *
     * @param   f1        the first {@code Float16} to compare
     * @param   f2        the second {@code Float16} to compare
     * @return  the value {@code 0} if {@code f1} is
     *          numerically equal to {@code f2}; a value less than
     *          {@code 0} if {@code f1} is numerically less than
     *          {@code f2}; and a value greater than {@code 0}
     *          if {@code f1} is numerically greater than
     *          {@code f2}.
     *
     * @see Float#compare(float, float)
     * @see Double#compare(double, double)
     */
    public static int compare(Float16 f1, Float16 f2) {
        return Float.compare(f1.floatValue(), f2.floatValue());
    }

    /**
     * Returns the larger of two {@code Float16} values.
     *
     * @apiNote
     * This method corresponds to the maximum operation defined in
     * IEEE 754.
     *
     * @param a the first operand
     * @param b the second operand
     * @return the greater of {@code a} and {@code b}
     * @see java.util.function.BinaryOperator
     * @see Math#max(float, float)
     * @see Math#max(double, double)
     */
    public static Float16 max(Float16 a, Float16 b) {
        return shortBitsToFloat16(floatToFloat16(Math.max(a.floatValue(),
                                                          b.floatValue() )));
    }

    /**
     * Returns the smaller of two {@code Float16} values.
     *
     * @apiNote
     * This method corresponds to the minimum operation defined in
     * IEEE 754.
     *
     * @param a the first operand
     * @param b the second operand
     * @return the smaller of {@code a} and {@code b}
     * @see java.util.function.BinaryOperator
     * @see Math#min(float, float)
     * @see Math#min(double, double)
     */
    public static Float16 min(Float16 a, Float16 b) {
        return shortBitsToFloat16(floatToFloat16(Math.min(a.floatValue(),
                                                          b.floatValue()) ));
    }

    // Skipping for now
    // public Optional<Float16> describeConstable()
    // public Float16 resolveConstantDesc(MethodHandles.Lookup lookup)

    /*
     * Note: for the basic arithmetic operations {+, -, *, /} and
     * square root, among binary interchange formats (binary16,
     * binary32 a.k.a. float, binary64 a.k.a double, etc.) the "2p + 2"
     * property holds. That is, if one format has p bits of precision,
     * if the next larger format has at least 2p + 2 bits of
     * precision, arithmetic on the smaller format can be implemented by:
     *
     * 1) converting each argument to the wider format
     * 2) performing the operation in the wider format
     * 3) converting the result from 2) to the narrower format
     *
     * For example, this property hold between the formats used for the
     * float and double types. Therefore, the following is a valid
     * implementation of a float addition:
     *
     * float add(float addend, float augend) {
     *     return (float)((double)addend + (double)augend);
     * }
     *
     * The same property holds between the float16 format and
     * float. Therefore, the software implementations of Float16 {+,
     * -, *, /} and square root below use the technique of widening
     * the Float16 arguments to float, performing the operation in
     * float arithmetic, and then rounding the float result to
     * Float16.
     *
     * For discussion and derivation of this property see:
     *
     * "When Is Double Rounding Innocuous?" by Samuel Figueroa
     * ACM SIGNUM Newsletter, Volume 30 Issue 3, pp 21-26
     * https://dl.acm.org/doi/pdf/10.1145/221332.221334
     *
     * Figueroa's write-up refers to lecture notes by W. Kahan. Those
     * lectures notes are assumed to be these ones by Kahan and
     * others:
     *
     * https://www.arithmazium.org/classroom/lib/Lecture_08_notes_slides.pdf
     * https://www.arithmazium.org/classroom/lib/Lecture_09_notes_slides.pdf
     */

    /**
     * Adds two {@code Float16} values together as per the {@code +}
     * operator semantics using the round to nearest rounding policy.
     *
     * The handling of signed zeros, NaNs, infinities, and other
     * special cases by this method is the same as for the handling of
     * those cases by the built-in {@code +} operator for
     * floating-point addition (JLS {@jls 15.18.2}).
     *
     * @apiNote This method corresponds to the addition operation
     * defined in IEEE 754.
     *
     * @param addend the first operand
     * @param augend the second operand
     * @return the sum of the operands
     *
     * @jls 15.4 Floating-point Expressions
     */
    // @IntrinsicCandidate
    public static Float16 add(Float16 addend, Float16 augend) {
        return valueOf(addend.floatValue() + augend.floatValue());
    }

    /**
     * Subtracts two {@code Float16} values as per the {@code -}
     * operator semantics using the round to nearest rounding policy.
     *
     * The handling of signed zeros, NaNs, infinities, and other
     * special cases by this method is the same as for the handling of
     * those cases by the built-in {@code -} operator for
     * floating-point subtraction (JLS {@jls 15.18.2}).
     *
     * @apiNote This method corresponds to the subtraction operation
     * defined in IEEE 754.
     *
     * @param minuend the first operand
     * @param  subtrahend the second operand
     * @return the difference of the operands
     *
     * @jls 15.4 Floating-point Expressions
     */
    // @IntrinsicCandidate
    public static Float16 subtract(Float16 minuend, Float16 subtrahend) {
        return valueOf(minuend.floatValue() - subtrahend.floatValue());
    }

    /**
     * Multiplies two {@code Float16} values as per the {@code *}
     * operator semantics using the round to nearest rounding policy.
     *
     * The handling of signed zeros, NaNs, and infinities, other
     * special cases by this method is the same as for the handling of
     * those cases by the built-in {@code *} operator for
     * floating-point multiplication (JLS {@jls 15.17.1}).
     *
     * @apiNote This method corresponds to the multiplication
     * operation defined in IEEE 754.
     *
     * @param multiplier the first operand
     * @param multiplicand the second operand
     * @return the product of the operands
     *
     * @jls 15.4 Floating-point Expressions
     */
    // @IntrinsicCandidate
    public static Float16 multiply(Float16 multiplier, Float16 multiplicand) {
        return valueOf(multiplier.floatValue() * multiplicand.floatValue());
    }

    /**
     * Divides two {@code Float16} values as per the {@code /}
     * operator semantics using the round to nearest rounding policy.
     *
     * The handling of signed zeros, NaNs, and infinities, other
     * special cases by this method is the same as for the handling of
     * those cases by the built-in {@code /} operator for
     * floating-point division (JLS {@jls 15.17.2}).
     *
     * @apiNote This method corresponds to the division
     * operation defined in IEEE 754.
     *
     * @param dividend the first operand
     * @param divisor the second operand
     * @return the quotient of the operands
     *
     * @jls 15.4 Floating-point Expressions
     */
    // @IntrinsicCandidate
    public static Float16 divide(Float16 dividend, Float16 divisor) {
        return valueOf(dividend.floatValue() / divisor.floatValue());
    }

    /**
     * {@return the square root of the operand} The square root is
     * computed using the round to nearest rounding policy.
     *
     * The handling of zeros, NaN, infinities, and negative arguments
     * by this method is analogous to the handling of those cases by
     * {@link Math#sqrt(double)}.
     *
     * @apiNote
     * This method corresponds to the squareRoot operation defined in
     * IEEE 754.
     *
     * @param radicand the argument to have its square root taken
     *
     * @see Math#sqrt(float)
     * @see Math#sqrt(double)
     */
    // @IntrinsicCandidate
    public static Float16 sqrt(Float16 radicand) {
        // Rounding path of sqrt(Float16 -> double) -> Float16 is fine
        // for preserving the correct final value. The conversion
        // Float16 -> double preserves the exact numerical value. The
        // of the double -> Float16 conversion also benefits from the
        // 2p+2 property of IEEE 754 arithmetic.
        return valueOf(Math.sqrt(radicand.doubleValue()));
    }

    /**
     * Returns the fused multiply add of the three arguments; that is,
     * returns the exact product of the first two arguments summed
     * with the third argument and then rounded once to the nearest
     * {@code Float16}.
     *
     * The handling of zeros, NaN, infinities, and other special cases
     * by this method is analogous to the handling of those cases by
     * {@link Math#fma(float, float, float)}.
     *
     * @apiNote This method corresponds to the fusedMultiplyAdd
     * operation defined in IEEE 754.
     *
     * @param a a value
     * @param b a value
     * @param c a value
     *
     * @return (<i>a</i>&nbsp;&times;&nbsp;<i>b</i>&nbsp;+&nbsp;<i>c</i>)
     * computed, as if with unlimited range and precision, and rounded
     * once to the nearest {@code Float16} value
     *
     * @see Math#fma(float, float, float)
     * @see Math#fma(double, double, double)
     */
    // @IntrinsicCandidate
    public static Float16 fma(Float16 a, Float16 b, Float16 c) {
        /*
         * In-progress explanation and discussion of the
         * implementation. Promoting the input arguments to
         * float/double and calculating (exactProdct + c) in double
         * and rounding that double to Float16 is correct for _at
         * least_ most sets of arguments. It may be correct for all
         * arguments, pending further analysis. If it not correct,
         * other computations will be done in those cases.
         *
         * The 2sum algorithm can be used to extract the low-order
         * value of a add/substract that were rounded away.
         *
         * -----
         *
         * When the numerical value of (a*b) + c is stored exactly in
         * a double, after a single rounding to Float16, the answer is
         * of necessity correct since the one double -> Float16
         * conversion is the only source of numerical error.
         *
         * However, for some inputs, the intermediate product-sum will
         * not be exact and additional analysis is needed -- and
         * currently underway -- to verify no corrective computation
         * is needed to return the proper answer.
         *
         * The following analysis will rely on the range of bit
         * positions representable in the intermediate
         * product-sum. For a Float16 value, the bit positions
         * representable in the format range from 2^(-14), MIN_VALUE,
         * to 2^15, the leading bit position of MAX_VALUE. Including
         * the implicit bit, Float16 has 11 bits of precision.
         *
         * For the double format, there are 53 bits of precision
         * (including the implicit bit), and the exponent range goes
         * from -1022 to 1023.
         *
         * For the product a*b of Float16 inputs, the range of
         * exponents for nonzero finite results goes from 2^(-28)
         * (from MIN_VALUE squared) to 2^31 (from the exact value of
         * MAX_VALUE squared). This full range of exponent positions,
         * (31 -(-28) + 1 ) = 60 exceeds the precision of
         * double. However, only the product a*b can exceed the
         * exponent range of Float16. Therefore, there are three main
         * cases to consider:
         *
         * 1) Large exponent product
         *
         * The magnitude of the overflow threshold for Float16 is:
         *
         * MAX_VALUE + 1/2 * ulp(MAX_VALUE) =  0x1.ffcp15 + 0x0.002p15 = 0x1.ffep15
         *
         * Therefore, any product greater than 0x1.ffep15 + MAX_VALUE
         * = 0x1.ffdp16 will certainly overflow (under round to
         * nearest) since adding in c = -MAX_VALUE will still be above
         * the overflow threshold.
         *
         * With a precision of 53 bits and much large exponent range,
         * even if the product has an exponent of 31, the smallest
         * Float16 that could be added in has an exponent of -14,
         * which only requires holding 31 -(-14) + 1 = 46 contiguous
         * bit positions, which is within the precision of double.
         *
         * 2) Exponent of product is within the range of normal
         * Float16 values
         *
         * The exact product has at most 22 contiguous bits in its
         * logical significand. The third number being added in has
         * at most 11 contiguous bits in its significand and the
         * lowest bit position that could be set is
         * 2^(-14). Therefore, when the product has the maximum
         * in-range exponent, 2^15, a single double has enough
         * precision to hold down to the smallest subnormal bit
         * position, 15 - (-14) + 1 = 30 < 53. If the product was
         * large and overflowed when the third operand was added, this
         * would cause the exponent to increase to 16, which is within
         * the range of double, so the product-sum is exact and will
         * be correct when rounded to Float16.
         *
         * 3) Exponent of product is in the range of subnormal values or smaller.
         *
         * The smallest exponent possible in a product is 2^(-48). The
         * precision of double can then hold other bit positions up to
         * -48 + 53 -1 = 4.
         *
         * Further case analysis to-do.
         */

        // product is numerically exact
        double product = (double)(a.floatValue() * b.floatValue());
        double c_double = c.doubleValue();
        // productSum exact in many cases, identify non-exact cases
        // below for further computation.
        double productSum = product + c_double;

//         if (Double.isFinite(product) &&
//             Double.isFinite(c_double) &&
//             (product != 0.0) &&
//             !exactSum(product, c_double, productSum) ) {
//             int productExponent = Math.getExponent(product);
//             assert productExponent <= -15;  // Might be off by one.
//             assert Math.getExponent(c_double) >=
//                 Math.getExponent(product) + 52 - 10 - 1: "exponent = " + Math.getExponent(c_double); // Might be off by a few
//             System.out.println(String.format("Extra work? %a * %a + %a => product %a, rounded %a",
//                                              a.floatValue(), b.floatValue(), c.floatValue(),
//                                              product, productSum));
//             // throw new UnsupportedOperationException("tbd");
//         }
        return valueOf(productSum);
    }

//     private static boolean exactSum(double a, double b, double s) {
//         // 2sum algorithm
//         //
//         // After the computation below, the exact sum of (a + b) is in
//         // (s + t) where s contains all the high-order
//         // bits. Therefore, if t is zero, the sum in s itself is
//         // exact.
//         double a_prime = s - b;
//         double b_prime = s - a_prime;
//         double delta_a = a - a_prime;
//         double delta_b = b - b_prime;
//         double t = delta_a + delta_b;
//         return t == 0.0;
//     }

    /**
     * {@return the negation of the argument}
     *
     * Special cases:
     * <ul>
     * <li> If the argument is zero, the result is a zero with the
     * opposite sign as the argument.
     * <li> If the argument is infinite, the result is an infinity
     * with the opposite sign as the argument.
     * <li> If the argument is a NaN, the result is a NaN.
     * </ul>
     *
     * @apiNote
     * This method corresponds to the negate operation defined in IEEE
     * 754.
     *
     * @param f16 the value to be negated
     * @jls 15.15.4 Unary Minus Operator {@code -}
     */
    // @IntrinsicCandidate
    public static Float16 negate(Float16 f16) {
        // Negate sign bit only. Per IEEE 754-2019 section 5.5.1,
        // negate is a bit-level operation and not a logical
        // operation. Therefore, in this case do _not_ use the float
        // unary minus as an implementation as that is not guaranteed
        // to flip the sign bit of a NaN.
        return shortBitsToFloat16((short)(f16.value ^ (short)0x0000_8000));
    }

    /**
     * {@return the absolute value of the argument}
     *
     * The handling of zeros, NaN, and infinities by this method is
     * analogous to the handling of those cases by {@link
     * Math#abs(float)}.
     *
     * @param f16 the argument whose absolute value is to be determined
     *
     * @see Math#abs(float)
     * @see Math#abs(double)
     */
    // @IntrinsicCandidate
    public static Float16 abs(Float16 f16) {
        // Zero out sign bit. Per IEE 754-2019 section 5.5.1, abs is a
        // bit-level operation and not a logical operation.
        return shortBitsToFloat16((short)(f16.value & (short)0x0000_7FFF));
    }

    /**
     * Returns the unbiased exponent used in the representation of a
     * {@code Float16}.
     *
     * <ul>
     * <li>If the argument is NaN or infinite, then the result is
     * {@link Float16#MAX_EXPONENT} + 1.
     * <li>If the argument is zero or subnormal, then the result is
     * {@link Float16#MIN_EXPONENT} - 1.
     * </ul>
     *
     * @apiNote
     * This method is analogous to the logB operation defined in IEEE
     * 754, but returns a different value on subnormal arguments.
     *
     * @param f16 a {@code Float16} value
     * @return the unbiased exponent of the argument
     *
     * @see Math#getExponent(float)
     * @see Math#getExponent(double)
     */
    public static int getExponent(Float16 f16) {
        return getExponent0(f16.value);
    }

    /**
     * From the bitwise representation of a float16, mask out exponent
     * bits, shift to the right and then subtract out float16's bias
     * adjust, 15, to get true exponent value.
     */
    /*package*/ static int getExponent0(short bits) {
        // package private to be usable in java.lang.Float.
        int bin16ExpBits     = 0x0000_7c00 & bits;     // Five exponent bits.
        return (bin16ExpBits >> (PRECISION - 1)) - 15;
    }

    /**
     * Returns the size of an ulp of the argument.  An ulp, unit in
     * the last place, of a {@code Float16} value is the positive
     * distance between this floating-point value and the {@code
     * Float16} value next larger in magnitude.  Note that for non-NaN
     * <i>x</i>, <code>ulp(-<i>x</i>) == ulp(<i>x</i>)</code>.
     *
     * <p>Special Cases:
     * <ul>
     * <li> If the argument is NaN, then the result is NaN.
     * <li> If the argument is positive or negative infinity, then the
     * result is positive infinity.
     * <li> If the argument is positive or negative zero, then the result is
     * {@code Float16.MIN_VALUE}.
     * <li> If the argument is &plusmn;{@code Float16.MAX_VALUE}, then
     * the result is equal to 2<sup>5</sup>, 32.0.
     * </ul>
     *
     * @param f16 the floating-point value whose ulp is to be returned
     * @return the size of an ulp of the argument
     */
    public static Float16 ulp(Float16 f16) {
        int exp = getExponent(f16);

        return switch(exp) {
        case MAX_EXPONENT + 1 -> abs(f16);          // NaN or infinity
        case MIN_EXPONENT - 1 -> Float16.MIN_VALUE; // zero or subnormal
        default -> {
            assert exp <= MAX_EXPONENT && exp >= MIN_EXPONENT;
            // ulp(x) is usually 2^(SIGNIFICAND_WIDTH-1)*(2^ilogb(x))
            // Let float -> float16 conversion handle encoding issues.
            yield valueOf(Math.scalb(1.0f, exp - (PRECISION - 1)));
        }
        };
    }
}
