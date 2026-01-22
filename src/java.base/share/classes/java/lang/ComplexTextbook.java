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
 * An implementation of complex numbers using "textbook" algorithms
 * for the arithmetic operations and using {@code double} values for
 * the real and imaginary component. This class is intended only for
 * prototyping and <em>not</em> intended for production use.
 *
 * <p>For explanatory purposes, in the discussions below of the semantics
 * of arithmetic methods, two complex numbers
 * <br>(<i>a</i> + <i>i</i>&middot;<i>b</i>) and (<i>c</i> + <i>i</i>&middot;<i>d</i>)
 * <br>will be used for notational convenience in specifying the
 * calculations used to compute the real and imaginary components of the result.
 *
 * @apiNote
 * TODO: For a production-level complex number class, discussion of (real,
 * imaginary) model, polar coordinates and {@linkplain #proj
 * projection method}, infinities (complex plane vs Riemann sphere),
 * branch cuts, component-wise vs norm-wise error, specific algorithms
 * used subject to change, etc.
 */
@Deprecated(forRemoval=true)
@jdk.internal.ValueBased
public final class /*value record*/ ComplexTextbook  {
    // This type should be Numerical, but *not* Orderable since
    // complex numbers are not an ordered field.

    /**
     * The real component of the complex number.
     */
    private final double  real;

    /**
     * The imaginary component of the complex number.
     * (Note that a more than textbook implementation may use a
     * separate imaginary type.)
     */
    private final double  imag;

    /**
     * Constructs a complex number.
     */
    private ComplexTextbook(double real, double imag) {
        this.real = real;
        this.imag = imag;
    }

    /**
     * A complex number with the value of zero, both real and
     * imaginary components of +0.0.
     *
     * @see #isZero(ComplexTextbook)
     */
    public static final ComplexTextbook ZERO = valueOf(0.0, 0.0);

    /**
     * A complex number with a real component of 1.0 and a 0.0
     * imaginary component.
     */
    public static final ComplexTextbook ONE = valueOf(1.0, 0.0);

    /**
     * A complex number with a real component of positive infinity and
     * a 0.0 imaginary component.
     *
     * @see #isInfinite(ComplexTextbook)
     * @see #proj(ComplexTextbook)
     */
    public static final ComplexTextbook INFINITY = valueOf(Double.POSITIVE_INFINITY, 0.0);

    /**
     * A complex number with NaN real component and imaginary
     * component.
     *
     * @see #isNaN(ComplexTextbook)
     */
    public static final ComplexTextbook NaN = valueOf(Double.NaN, Double.NaN);

    /**
     * {@return the real component of this complex number}
     */
    public double real() { // better as a static method?
        return real;
    }

    /**
     * {@return the imaginary component of this complex number}
     */
    public double imag() { // better as a static method?
        return imag;
    }

    /**
     * {@return a complex number with real and imaginary components
     * equivalent to the real and imaginary arguments, respectively}
     *
     * @param real the real component
     * @param imag the imaginary component
     */
    public static ComplexTextbook valueOf(double real, double imag) { 
        return new ComplexTextbook(real, imag);
    }

    /**
     * {@return a complex number with the real component equivalent to the
     * argument and a {@code +0.0} imaginary component}
     *
     * @param real the real component
     */
    public static ComplexTextbook valueOf(double real) { 
        return new ComplexTextbook(real, 0.0);
    }

    /**
     * {@return lorem ipsum}
     *
     * @param s the string to be parsed
     *
     * @see Double#parseDouble(String)
     */
    public static ComplexTextbook valueOf(String s) { 
        throw new UnsupportedOperationException("work in progress");
    }

    /**
     * {@return lorem ipsum}
     *
     * @apiNote
     * To convert <em>to</em> polar form, use {@code r = ComplexTextbook.abs(c)} and
     * {@code theta = Math.atan2(c.imag(), c.real())}.
     *
     * @param r the modulus, {@linkplain abs absolute value}, of the
     * complex number in polar form
     * @param theta the phase angle of the complex number in polar form
     *
     * @see Math#atan2(double, double)
     */
    public static ComplexTextbook valueOfPolar(double r, double theta) { 
        return valueOf(r*Math.cos(theta),
                       r*Math.sin(theta));
    }

    /**
     * {@return a string representing the complex number}
     */
    @Override
    public String toString() {
        return "(" + real + " + " + "i*" + imag  + ")";
    }

    /**
     * {@return a string representing the argument}
     *
     * @param c the complex number to be represented
     */
    public static String toString(ComplexTextbook c) {
        return c.toString();
    }

    /**
     * {@return lorem ipsum}
     * @param that lorem ipsum}
     */
    @Override
    public boolean equals(Object that) {
        if (that instanceof ComplexTextbook c) {
            return this.real == c.real && this.imag == c.imag;
        } else {
            return false;
        }
    }

    /**
     * {@return lorem ipsum}
     */
    // @Override
    public int hashCode(){
        // Add 0.0 to be consistent with current equals impl.
        return Double.hashCode(real + 0.0) ^ Double.hashCode(imag + 0.0);
    }

    /**
     * {@return lorem ipsum}
     *
     * @apiNote
     * Relate to {@linkplain Double##repEquivalence equivalence
     * discussion} in {@code double}...
     *
     * @param c1 lorem ipsum}
     * @param c2 lorem ipsum}
     */
    public static boolean equivalent(ComplexTextbook c1, ComplexTextbook c2) {
        return Double.compare(c1.real, c2.real) == 0 &&
               Double.compare(c1.imag, c2.imag) == 0;
    }

    // Arithmetic operators

    /**
     * Addition operation, binary "{@code +}".
     *
     * @implSpec
     * The computed sum is equivalent to
     * (<i>a</i>&nbsp;+&nbsp;<i>c</i>)&nbsp;+&nbsp;<i>i</i>&middot;(<i>b</i>&nbsp;+&nbsp;<i>d</i>).
     *
     * @param addend the first operand
     * @param augend the second operand
     * @return the sum of the operands
     */
    public static ComplexTextbook add(ComplexTextbook addend,
                                      ComplexTextbook augend) {
        double a = addend.real;
        double b = addend.imag;
        double c = augend.real;
        double d = augend.imag;

        return valueOf(a + c, b + d);
    }

    /**
     * Subtraction operation, binary "{@code -}".
     *
     * @implSpec
     * The computed difference is equivalent to
     * (<i>a</i>&nbsp;&minus;&nbsp;<i>c</i>)&nbsp;+&nbsp;<i>i</i>&middot;(<i>b</i>&nbsp;&minus;&nbsp;<i>d</i>).
     *
     * @param minuend the first operand
     * @param subtrahend the second operand
     * @return the difference of the operands
     */
    public static ComplexTextbook subtract(ComplexTextbook minuend,
                                           ComplexTextbook subtrahend) {
        double a = minuend.real;
        double b = minuend.imag;
        double c = subtrahend.real;
        double d = subtrahend.imag;

        return valueOf(a - c, b - d);
    }

    /**
     * Multiplication operation, "{@code *}".
     *
     * @apiNote
     * WARNING: while simple, the calculation technique used by this
     * method is subject to spurious underflow and overflow as well as
     * inaccurate component-wise results.
     *
     * @implSpec
     * The computed product is calculated by
     * (<i>ac</i>&nbsp;&minus;&nbsp;<i>bd</i>)&nbsp;+&nbsp;<i>i</i>&middot;(<i>ad</i>&nbsp;+&nbsp;<i>bc</i>)
     *
     * @param multiplier the first operand
     * @param multiplicand the second operand
     * @return the product of the operands
     */
    public static ComplexTextbook multiply(ComplexTextbook multiplier,
                                           ComplexTextbook multiplicand) {
        double a = multiplier.real;
        double b = multiplier.imag;
        double c = multiplicand.real;
        double d = multiplicand.imag;

        return valueOf(a*c - b*d, a*d + b*c);
    }

    /**
     * Division operation, "{@code /}".
     *
     * @apiNote
     * TODO: Bad numerical things can happen warning...
     *
     * @implSpec
     * The computed quotient is calculated by
     * (<i>ac</i> + <i>bd</i>)/(<i>c</i>&sup2; + <i>d</i>&sup2;) + <i>i</i>*(<i>bc</i> &minus; <i>ad</i>)/(<i>c</i>&sup2; + <i>d</i>&sup2;)
     *
     * @param dividend the value to be divided
     * @param divisor the value being divided by
     * @return the quotient of the first argument divided by
     * the second argument
     */
    public static ComplexTextbook divide(ComplexTextbook dividend,
                                         ComplexTextbook divisor) {
        double a = dividend.real;
        double b = dividend.imag;
        double c = divisor.real;
        double d = divisor.imag;

        double scale = c*c + d*d;

        return valueOf((a*c + b*d)/scale, (b*c - a*d)/scale);
    }

    /**
     * Remainder operation, "{@code %}".
     *
     * @param dividend the value to be divided to compute the remainder
     * @param divisor the value being divided by
     * @return the remainder of the first argument divided by
     * the second argument
     */
    public static ComplexTextbook remainder(ComplexTextbook dividend,
                                        ComplexTextbook divisor) {
        throw new UnsupportedOperationException("tbd");
    }

    // TODO: API decision, is this method needed?

    /**
     * Unary plus operation, "{@code +}".
     *
     * @param operand the operand
     * @return unary plus of the operand
     */
    public static ComplexTextbook plus(ComplexTextbook operand) {
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
     *
     * @implSpec
     * The negation is equivalent to
     * &minus;<i>a</i>&nbsp;+&nbsp;&minus;<i>i</i>&middot;<i>b</i>
     *
     * @param c the operand
     * @return the negation of the operand
     */
    public static ComplexTextbook negate(ComplexTextbook c) {
        return valueOf(-c.real, -c.imag);
    }

    /**
     * {@return lorem ipsum}
     *
     * @implSpec
     * The conjugate is equivalent to
     * <i>a</i>&nbsp;+&nbsp;&minus;<i>i</i>&middot;<i>b</i>
     *
     * @param c a complex number
     */
    public static ComplexTextbook conj(ComplexTextbook c) {
        return valueOf(c.real, -c.imag);
    }

    // Utility methods

    /**
     * {@return lorem ipsum}
     *
     * @implSpec
     * use hypot
     * @param c a complex number
     * @see Math#abs(double)
     */
    public static double abs(ComplexTextbook c) {
        double a = c.real;
        double b = c.imag;
        return StrictMath.hypot(a, b);
    }

    /**
     * {@return lorem ipsum}
     *
     * If the argument is infinite, return the canonical infinity,
     * otherwise return the argument.
     *
     * @param c a complex number
     */
    public static ComplexTextbook proj(ComplexTextbook c) {
        return isInfinite(c) ? INFINITY : c;
    }

    /**
     * {@return {@code true} if both the real and imaginary components
     * are zero; {@code false} otherwise}
     *
     * @param c a complex number
     */
    public static boolean isZero(ComplexTextbook c) {
        return c.real == 0.0 && c.imag == 0.0;
    }

    /**
     * {@return {@code true} if either the real or imaginary component
     * is NaN; {@code false} otherwise}
     *
     * @param c a complex number
     */
    public static boolean isNaN(ComplexTextbook c) {
        return Double.isNaN(c.real) || Double.isNaN(c.imag);
    }

    /**
     * {@return {@code true} if either the real or imaginary component
     * is infinite; {@code false} otherwise}
     *
     * @param c a complex number
     */
    public static boolean isInfinite(ComplexTextbook c) {
        return Double.isInfinite(c.real) || Double.isInfinite(c.imag);
    }

    /**
     * {@return {@code true} if both the real and imaginary components
     * are finite; {@code false} otherwise}
     *
     * @param c a complex number
     */
    public static boolean isFinite(ComplexTextbook c) {
        return Double.isFinite(c.real) && Double.isFinite(c.imag);
    }
}
