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
 * The {@code Float16} is a primitive value class holding 16-bit data in IEEE 754 binary16 format
 * {@code Float16} contains a single field whose type is {@code short}.
 *
 * Binary16 Format:
 *   S EEEEE  MMMMMMMMMM
 *   Sign        - 1 bit
 *   Exponent    - 5 bits
 *   Significand - 10 bits
 *
 * <p>This is a <a href="https://openjdk.org/jeps/401">primitive value class</a> and its objects are
 * identity-less non-nullable value objects.
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
     * A constant holding the positive infinity of type
     * {@code Float16}.
     */
    public static final Float16 POSITIVE_INFINITY =
        shortBitsToFloat16(floatToFloat16(Float.POSITIVE_INFINITY));

    /**
     * A constant holding the negative infinity of type
     * {@code Float16}.
     */
    public static final Float16 NEGATIVE_INFINITY = valueOf(Float.NEGATIVE_INFINITY);

    /**
     * A constant holding a Not-a-Number (NaN) value of type
     * {@code Float16}.
     */
    public static final Float16 NaN = valueOf(Float.NaN);

    /**
     * A constant holding the largest positive finite value of type
     * {@code Float16},
     * (2-2<sup>-10</sup>)&middot;2<sup>15</sup>, equal to 65504.0.
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
     * Returns a string representation of the {@code float16}
     * argument. All characters mentioned below are ASCII characters.
     *
     * TODO: elaborate on more detailed behavior
     *
     * @param   f16   the {@code float} to be converted.
     * @return a string representation of the argument.
     */
    public static String toString(Float16 f16) {
        // FIXME -- update for Float16 precision
        return FloatToDecimal.toString(f16.floatValue());
    }

    /**
     * Returns a hexadecimal string representation of the
     * {@code Float16} argument.
     *
     * TODO: elaborate on more detailed behavior
     *
     * @param   f16   the {@code Float16} to be converted.
     * @return a hex string representation of the argument.
     *
     * @see Float#toHexString(float)
     * @see Double#toHexString(double)
     */
    public static String toHexString(Float16 f16) {
        float f = f16.floatValue();
        if (Math.abs(f) < float16ToFloat(Float16.MIN_NORMAL.value)
            &&  f != 0.0f ) {// Float16 subnormal
            // Adjust exponent to create subnormal double, then
            // replace subnormal double exponent with subnormal Float16
            // exponent
            String s = Double.toHexString(Math.scalb((double)f,
                                                     /* -1022+14 */
                                                     Double.MIN_EXPONENT-
                                                     Float16.MIN_EXPONENT));
            return s.replaceFirst("p-1022$", "p-14");
        } else {// double string will be the same as Float16 string
            return Double.toHexString(f);
        }
    }

    // -----------------------

   /**
    * {@return the value of a {@code short} converted to {@code Float16}}
    *
    * @param  value a short value.
    */
    public static Float16 valueOf(short value) {
        // The conversion of a short to a float is numerically exact.
        return shortBitsToFloat16(floatToFloat16((float)value));
    }


   /**
    * {@return a {@code Float16} value rounded from the {@code float} argument}
    *
    * @param  f a {@code float}
    */
    public static Float16 valueOf(float f) {
        return new Float16(Float.floatToFloat16(f));
    }

   /**
    * {@return a {@code Float16} value rounded from the {@code double} argument}
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

    //    /**
    //     * ...
    //     * @apiNote
    //     * This method corresponds to the convertFromInt operation defined
    //     * in IEEE 754.
    //     */
    //    public static Float16 valueOf(long ell) // Is this needed for correctness?
    //    public static Float16 valueOf(BigDecimal bd)


    /**
     * Returns a {@code Float16} equal to the value
     * represented by the specified {@code String}.
     *
     * @param  s the string to be parsed.
     * @return the {@code Float16} value represented by the string
     *         argument.
     * @throws NullPointerException  if the string is null
     * @throws NumberFormatException if the string does not contain a
     *               parsable {@code Float16}.
     * @see    java.lang.Float#valueOf(String)
     */
    public static Float16 parseFloat(String s) throws NumberFormatException {
        // TOOD: adjust precision of parsing if needed
        return shortBitsToFloat16(floatToFloat16(Float.parseFloat(s)));
    }

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
     * @param f16 the {@code float} value to be tested
     * @return {@code true} if the argument is a finite
     * floating-point value, {@code false} otherwise.
     */
    public static boolean isFinite(Float16 f16) {
        return Float.isFinite(f16.floatValue());
     }

    // Skipping for now
    // public boolean isNaN()
    // public boolean isInfinite() {

    /**
     * Returns the value of this {@code Float16} as a {@code byte} after
     * a narrowing primitive conversion.
     *
     * @return  the binary16 encoded {@code short} value represented by this object
     *          converted to type {@code byte}
     * @jls 5.1.3 Narrowing Primitive Conversion
     */
    @Override
    public byte byteValue() {
        return (byte)floatValue();
    }

    /**
     * {@return a string representation of this {@code Float16}}
     *
     * @see java.lang.Float#toString(float)
     */
    public String toString() {
        // Is this idiomatic?
        return Float16.toString(this);
    }

    /**
     * Returns the value of this {@code Float16} as a {@code short}
     * after a narrowing primitive conversion.
     *
     * @return  the binary16 encoded {@code short} value represented by this object
     *          converted to type {@code short}
     * @jls 5.1.3 Narrowing Primitive Conversion
     */
    @Override
    public short shortValue() {
        return (short)floatValue();
    }

    /**
     * Returns the value of this {@code Float16} as an {@code int} after
     * a narrowing primitive conversion.
     *
     * @return  the binary16 encoded {@code short} value represented by this object
     *          converted to type {@code int}
     * @jls 5.1.3 Narrowing Primitive Conversion
     */
    @Override
    public int intValue() {
        return (int)floatValue();
    }

    /**
     * Returns value of this {@code Float16} as a {@code long} after a
     * narrowing primitive conversion.
     *
     * @return  the binary16 encoded {@code short} value represented by this object
     *          converted to type {@code long}
     * @jls 5.1.3 Narrowing Primitive Conversion
     */
    @Override
    public long longValue() {
        return (long)floatValue();
    }

    /**
     * Returns the {@code float} value of this {@code Float16} object.
     *
     * @return the binary16 encoded {@code short} value represented by this object
     *         converted to type {@code float}
     * @jls 5.1.2 Widening Primitive Conversion
     */
    @Override
    public float floatValue() {
        return float16ToFloat(value);
    }

    /**
     * Returns the value of this {@code Float16} as a {@code double}
     * after a widening primitive conversion.
     *
     * @apiNote
     * This method corresponds to the convertFormat operation defined
     * in IEEE 754.
     *
     * @return the binary16 encoded {@code short} value represented by this
     *         object converted to type {@code double}
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
     * Adds two {@code Float16} values together as per the + operator semantics.
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
     * ==, >=, >}) on {@code double} values.
     *
     * <ul><li> A NaN is <em>unordered</em> with respect to other
     *          values and unequal to itself under the comparison
     *          operators.  This method chooses to define {@code
     *          Double.NaN} to be equal to itself and greater than all
     *          other {@code double} values (including {@code
     *          Double.POSITIVE_INFINITY}).
     *
     *      <li> Positive zero and negative zero compare equal
     *      numerically, but are distinct and distinguishable values.
     *      This method chooses to define positive zero ({@code +0.0d}),
     *      to be greater than negative zero ({@code -0.0d}).
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
     * @jls 15.20.1 Numerical Comparison Operators {@code <}, {@code <=}, {@code >}, and {@code >=}
     */
    @Override
    public int compareTo(Float16 anotherFloat16) {
        return Float16.compare(this, anotherFloat16);
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
     */
    public static int compare(Float16 f1, Float16 f2) {
        return Float.compare(f1.floatValue(), f2.floatValue());
    }

    /**
     * Returns the greater of two {@code Floa16t} values.
     *
     * @apiNote
     * This method corresponds to the maximum operation defined in
     * IEEE 754.
     *
     * @param a the first operand
     * @param b the second operand
     * @return the greater of {@code a} and {@code b}
     * @see java.util.function.BinaryOperator
     */
    public static Float16 max(Float16 a, Float16 b) {
        return shortBitsToFloat16(floatToFloat16(Math.max(a.floatValue(),
                                                          b.floatValue() )));
    }

    /**
     * Returns the smaller of two {@code float} values.
     *
     * @apiNote
     * This method corresponds to the minimum operation defined in
     * IEEE 754.
     *
     * @param a the first operand
     * @param b the second operand
     * @return the smaller of {@code a} and {@code b}
     * @see java.util.function.BinaryOperator
     */
    public static Float16 min(Float16 a, Float16 b) {
        return shortBitsToFloat16(floatToFloat16(Math.min(a.floatValue(),
                                                          b.floatValue()) ));
    }

    // Skipping for now
    // public Optional<Float16> describeConstable()
    // public Float16 resolveConstantDesc(MethodHandles.Lookup lookup)

    // TODO: add comment explaining 2p + 2 property and implementation.

    /**
     * Adds two {@code Float16} values together as per the + operator semantics.
     *
     * @apiNote This method corresponds to the addition operation
     * defined in IEEE 754.
     *
     * @param addend the first operand
     * @param augend the second operand
     * @return the sum of the operands
     *
     * @jls 15.18.2 Additive Operators ({@code +} and {@code -}) for Numeric Types
     */
    // @IntrinsicCandidate
    public static Float16 add(Float16 addend, Float16 augend) {
        return shortBitsToFloat16(floatToFloat16(addend.floatValue()
                                                 +
                                                 augend.floatValue() ));
    }

/**
     * Subtracts two {@code Float16} values as per the - operator semantics.
     *
     * @apiNote This method corresponds to the subtraction operation
     * defined in IEEE 754.
     *
     * @param minuend the first operand
     * @param  subtrahend the second operand
     * @return the difference of the operands
     *
     * @jls 15.18.2 Additive Operators (+ and -) for Numeric Types
     */
    // @IntrinsicCandidate
    public static Float16 subtract(Float16 minuend, Float16 subtrahend) {
        return shortBitsToFloat16(floatToFloat16(minuend.floatValue()
                                                 -
                                                 subtrahend.floatValue() ));
    }

    /**
     * Multiplies two {@code Float16} values as per the * operator semantics.
     *
     * @apiNote This method corresponds to the multiplication
     * operation defined in IEEE 754.
     *
     * @param multiplier the first operand
     * @param multiplicand the second operand
     * @return the product of the operands
     *
     * @jls 15.17.1 Multiplication Operator {@code *}
     */
    // @IntrinsicCandidate
    public static Float16 multiply(Float16 multiplier, Float16 multiplicand) {
       return shortBitsToFloat16(floatToFloat16(float16ToFloat(multiplier.value)
                                                *
                                                float16ToFloat(multiplicand.value) ));
    }

    /**
     * Divides two {@code Float16} values as per the / operator semantics.
     *
     * @apiNote This method corresponds to the division
     * operation defined in IEEE 754.
     *
     * @param dividend the first operand
     * @param divisor the second operand
     * @return the quotient of the operands
     *
     * @jls 15.17.2 Division Operator {@code /}
     */
    // @IntrinsicCandidate
    public static Float16 divide(Float16 dividend, Float16 divisor) {
       return shortBitsToFloat16(floatToFloat16(float16ToFloat(dividend.value)
                                                /
                                                float16ToFloat(divisor.value) ));
    }

    /**
     * {@return the square root of the operand}
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
        // Rounding path of sqrt(Float16 -> float -> double) -> float
        // -> Float16 is fine for preserving the correct final
        // value. The sequence of conversions Float16 -> float ->
        // double preserves the exact numerical value. Each of the
        // double -> float and float -> Float16 conversions benefits
        // from the 2p+2 property of IEEE 754 arithmetic.
        return shortBitsToFloat16(floatToFloat16((float)Math.sqrt(radicand.floatValue())));
    }

    /**
     * Returns the fused multiply add of the three arguments; that is,
     * returns the exact product of the first two arguments summed
     * with the third argument and then rounded once to the nearest
     * {@code Float16}.
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
        // A simple scaling up to call a float or double fma doesn't
        // always work as double-rounding can occur and the sticky bit
        // information can be lost for rounding to a Float16 position.
        //
        // The quantity:
        //
        // convertToDouble(a)*convertToDouble(b) + convertToDouble(c)
        //
        // will be an exact double value. The number of significand
        // bits in double, 53, is greater than the, maximum difference
        // in exponent values between bit positions of minimum and
        // maximum magnitude for Float16. Therefore, performing a*b+c
        // in double and then doing a single rounding of that value to
        // Float16 will implement this operation.

        return valueOf( ( ((double)a.floatValue()) * (double)b.floatValue() )
                        +
                        ((double)c.floatValue()) );
    }

    /**
     * {@return the negation root of the argument}
     * @param f16 the value to be negated
     */
    // @IntrinsicCandidate
    public static Float16 negate(Float16 f16) {
        // Negate sign bit only. Per IEE 754-2019 section 5.5.1,
        // negate is a bit-level operation and not a logical
        // operation.
        return shortBitsToFloat16((short)(f16.value ^ (short)0x0000_8000));
    }

    /**
     * {@return the absolute value of the argument}
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
     * @param f16 a {@code Floa16t} value
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
     * double} value next larger in magnitude.  Note that for non-NaN
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
        case Float16.MAX_EXPONENT + 1 -> abs(f16);          // NaN or infinity
        case Float16.MIN_EXPONENT - 1 -> Float16.MIN_VALUE; // zero or subnormal
        default -> {
            assert exp <= Float16.MAX_EXPONENT && exp >= Float16.MIN_EXPONENT;
            // ulp(x) is usually 2^(SIGNIFICAND_WIDTH-1)*(2^ilogb(x))
            // Let float -> float16 conversion handle encoding issues.
            yield valueOf(Math.scalb(1.0f, exp - (PRECISION - 1)));
        }
        };
    }

    // To be considered:
    // copysign
    // scalb
    // nextUp / nextDown
    // IEEEremainder
    // signum
}
