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

package java.lang;

/**
 * An implementation of complex numbers based on polar coordinates and using
 * "textbook" algorithms for the arithmetic operations and using
 * {@code double} values for the {@linkplain r() magnitude} and
 * {@linkplain #theta() angle} components.
 *
 * <p>For explanatory purposes, in the discussions below of the semantics
 * of arithmetic methods, two complex numbers
 * <br>(<i>r</i><sub>1</sub> &ang; &theta;<sub>1</sub>) and
 * (<i>r</i><sub>2</sub> &ang; &theta;<sub>2</sub>)
 * <br>will be used for notational convenience in specifying the
 * calculations used to compute the magnitude and angle components of the result.
 *
 * @apiNote
 * TODO: Discuss issues and limitations. Is a method needed to
 * normalize zeros as well as infinities?
 *
 * <p>This class is intended only for prototyping and
 * <em>not</em> intended for production use.
 */
// @Deprecated(forRemoval=true)
// @SuppressWarnings("removal") // Usages from ComplexTextbook
@jdk.internal.ValueBased
public final /* value */ class ComplexPolarTextbook  {
    // This type should be Numerical, but *not* Orderable since
    // complex numbers are not an ordered field.

    /**
     * Witness for the {@code Numerical} interface.
     */
    public static final __witness Numerical<ComplexPolarTextbook> NUM =
        new Numerical<ComplexPolarTextbook>() {

        public ComplexPolarTextbook add(ComplexPolarTextbook addend,
                                        ComplexPolarTextbook augend) {
            return ComplexPolarTextbook.add(addend, augend);
        }

        public ComplexPolarTextbook subtract(ComplexPolarTextbook minuend,
                                             ComplexPolarTextbook subtrahend) {
            return ComplexPolarTextbook.subtract(minuend, subtrahend);
        }

        public ComplexPolarTextbook multiply(ComplexPolarTextbook multiplier,
                                             ComplexPolarTextbook multiplicand) {
            return ComplexPolarTextbook.multiply(multiplier, multiplicand);
        }

        public ComplexPolarTextbook divide(ComplexPolarTextbook dividend,
                                           ComplexPolarTextbook divisor) {
            return ComplexPolarTextbook.divide(dividend, divisor);
        }

        public ComplexPolarTextbook remainder(ComplexPolarTextbook dividend,
                                              ComplexPolarTextbook divisor) {
            return ComplexPolarTextbook.remainder( dividend,  divisor);
        }

        public ComplexPolarTextbook plus(ComplexPolarTextbook operand) {
            return ComplexPolarTextbook.plus(operand);
        }

        public ComplexPolarTextbook negate(ComplexPolarTextbook operand) {
            return ComplexPolarTextbook.negate( operand);
        }
    };

    /**
     * The magnitude of the complex number.
     */
    private final double  r;

    /**
     * The phase angle of the complex number.
     */
    private final double  theta;

    /**
     * Constructs a complex number.
     */
    private ComplexPolarTextbook(double r, double theta) {
        this.r     = r;
        this.theta = theta;
    }

    /**
     * A complex number with the value of zero, both magnitude and
     * angle are 0.0.
     *
     * @see #isZero(ComplexPolarTextbook)
     */
    public static final ComplexPolarTextbook ZERO = valueOf(0.0, 0.0);

    /**
     * A complex number with a magnitude of 1.0 and an angle of 0.0.
     */
    public static final ComplexPolarTextbook ONE = valueOf(1.0, 0.0);

    /**
     * A complex number with a magnitude of of positive infinity and a
     * 0.0 angle.
     *
     * @see #isInfinite(ComplexPolarTextbook)
     * @see #proj(ComplexPolarTextbook)
     */
    public static final ComplexPolarTextbook INFINITY = valueOf(Double.POSITIVE_INFINITY, 0.0);

    /**
     * A complex number with NaN magnitude and angle.
     *
     * @see #isNaN(ComplexPolarTextbook)
     */
    public static final ComplexPolarTextbook NaN = valueOf(Double.NaN, Double.NaN);

    /**
     * {@return the magnitude (modulus) of this complex number}
     */
    public double r() { // better as a static method?
        return r;
    }

    /**
     * {@return the angle of this complex number}
     */
    public double theta() { // better as a static method?
        return theta;
    }

    /**
     * {@return a complex number with magnitude and angle components
     * equivalent to the magnitude and angle  arguments, respectively}
     *
     * @param r the magnitude of the complex number
     * @param theta the angle of the complex number
     */
    public static ComplexPolarTextbook valueOf(double r, double theta) {
        return new ComplexPolarTextbook(r, theta);
    }

    /**
     * {@return a complex number with the magnitude component equivalent to the
     * argument and a {@code +0.0} angle component}
     *
     * @param r the magnitude of the complex number
     */
    public static ComplexPolarTextbook valueOf(double r) {
        return new ComplexPolarTextbook(r, 0.0);
    }


    /**
     * {@return a complex number of polar coordinates computed from
     * from a complex number using real and imaginary components}
     *
     * @implSpec
     * {@link ComplexTextbook#abs(ComplexTextbook) abs} and
     * {@link Math#atan2(double, double) atan2}
     *
     * @param c a complex number using real and imaginary components
     */
    public static ComplexPolarTextbook valueOf(ComplexTextbook c) {
        return valueOf(ComplexTextbook.abs(c),
                       StrictMath.atan2(c.imag(), c.real()));
    }


    /**
     * {@return lorem ipsum}
     *
     * @param s the string to be parsed
     *
     * @see Double#parseDouble(String)
     */
    public static ComplexPolarTextbook valueOf(String s) {
        throw new UnsupportedOperationException("work in progress");
    }

    /**
     * {@return a string representing the complex number}
     */
    @Override
    public String toString() {
        return "(r=" + r + " + " + "theta=" + theta  + ")";
    }

    /**
     * {@return a string representing the argument}
     *
     * @param c the complex number to be represented
     */
    public static String toString(ComplexPolarTextbook c) {
        return c.toString();
    }

    /**
     * {@return lorem ipsum}
     * @param that lorem ipsum}
     */
    @Override
    public boolean equals(Object that) {
        return that instanceof ComplexPolarTextbook c &&
            this.r == c.r && this.theta == c.theta;
    }

    /**
     * {@return lorem ipsum}
     */
    // @Override
    public int hashCode(){
        // Add 0.0 to be consistent with current equals impl.
        return Double.hashCode(r + 0.0) ^ Double.hashCode(theta + 0.0);
    }

    /**
     * {@return lorem ipsum}
     *
     * @apiNote
     * Relate to {@linkplain Double##repEquivalence equivalence
     * discussion} in {@code double}...
     * Should have a discussion of comparing angles...
     *
     * @param c1 lorem ipsum}
     * @param c2 lorem ipsum}
     */
    public static boolean equivalent(ComplexPolarTextbook c1, ComplexPolarTextbook c2) {
        return Double.compare(c1.r, c2.r) == 0 &&
               Double.compare(c1.theta, c2.theta) == 0;
    }

    // Arithmetic operators

    // NOTE: As an artifact of how they are currently implemented,
    // add and subtract will only return angles within [-pi, pi] while
    // multiply and divide can return angles outside of that range.

    /**
     * Addition operation, binary "{@code +}".
     *
     * @implSpec
     * TBD
     *
     * @param addend the first operand
     * @param augend the second operand
     * @return the sum of the operands
     */
    public static ComplexPolarTextbook add(ComplexPolarTextbook addend,
                                           ComplexPolarTextbook augend) {
        // Convert to (real, imaginary), do the addition, and convert back.
        return valueOf(ComplexTextbook.add(ComplexTextbook.valueOf(addend),
                                           ComplexTextbook.valueOf(addend) ));
    }

    /**
     * Subtraction operation, binary "{@code -}".
     *
     * @implSpec
     * TBD
     *
     * @param minuend the first operand
     * @param subtrahend the second operand
     * @return the difference of the operands
     */
    public static ComplexPolarTextbook subtract(ComplexPolarTextbook minuend,
                                                ComplexPolarTextbook subtrahend) {

        // Convert to (real, imaginary), do the subtraction, and convert back.
        return valueOf(ComplexTextbook.subtract(ComplexTextbook.valueOf(minuend),
                                                ComplexTextbook.valueOf(subtrahend) ));
    }

    /**
     * Multiplication operation, "{@code *}".
     *
     * @implSpec
     * The computed product is calculated as
     * (<i>r</i><sub>1</sub>&middot;<i>r</i><sub>2</sub> &ang;
     * &theta;<sub>1</sub> + &theta;<sub>2</sub>)
     *
     * @param multiplier the first operand
     * @param multiplicand the second operand
     * @return the product of the operands
     */
    public static ComplexPolarTextbook multiply(ComplexPolarTextbook multiplier,
                                                ComplexPolarTextbook multiplicand) {
        double r1     = multiplier.r();
        double theta1 = multiplier.theta();
        double r2     = multiplicand.r();
        double theta2 = multiplicand.theta();

        return valueOf(r1 * r2, theta1 + theta2);
    }

    /**
     * Division operation, "{@code /}".
     *
     * @implSpec
     * The computed quotient is calculated as
     * (<i>r</i><sub>1</sub>/<i>r</i><sub>2</sub> &ang;
     * &theta;<sub>1</sub> &minus; &theta;<sub>2</sub>)
     *
     * @param dividend the value to be divided
     * @param divisor the value being divided by
     * @return the quotient of the first argument divided by
     * the second argument
     */
    public static ComplexPolarTextbook divide(ComplexPolarTextbook dividend,
                                              ComplexPolarTextbook divisor) {
        double r1     = dividend.r();
        double theta1 = dividend.theta();
        double r2     = divisor.r();
        double theta2 = divisor.theta();

        return valueOf(r1 / r2, theta1 - theta2);
    }

    /**
     * Remainder operation, "{@code %}".
     *
     * @param dividend the value to be divided to compute the remainder
     * @param divisor the value being divided by
     * @return the remainder of the first argument divided by
     * the second argument
     */
    public static ComplexPolarTextbook remainder(ComplexPolarTextbook dividend,
                                                 ComplexPolarTextbook divisor) {
        throw new UnsupportedOperationException("tbd");
    }

    // TODO: API decision, is this method needed?

    /**
     * Unary plus operation, "{@code +}".
     *
     * @param operand the operand
     * @return unary plus of the operand
     */
    public static ComplexPolarTextbook plus(ComplexPolarTextbook operand) {
        return operand;
    }

    /**
     * Negation operation, unary "{@code -}".
     *
     * @implSpec
     * The negation is equivalent to
     * (<i>r</i> &ang; &theta;&plusmn;&pi;)
     * <br>such that the resulting angle is within the range of {@link
     * Math#atan2(double, double) atan2}.
     *
     * @apiNote
     * If the angle is large (abs(theta) >> &pi;), it might be more robust to convert to
     * (real, imaginary), negate, and convert back normalizing the
     * angle.
     *
     * @param c the operand
     * @return the negation of the operand
     */
    public static ComplexPolarTextbook negate(ComplexPolarTextbook c) {
        double theta = c.theta();
        return valueOf(c.r(),
                       // Stay within range of atan2
                       (theta > 0) ? theta - Math.PI  : theta + Math.PI);
    }

    /**
     * {@return the complex conjugate of the complex number}
     *
     * @implSpec
     * The conjugate is equivalent to
     * (<i>r</i> &ang; &minus;&theta;)
     */
    public ComplexPolarTextbook conj() {
        return valueOf(this.r(), -this.theta());
    }

    // Utility methods

    /**
     * {@return the absolute value of the argument}
     *
     * @implSpec
     * Just use {@link r()}.
     * @param c a complex number
     */
    public static double abs(ComplexPolarTextbook c) {
        return c.r();
    }

    /**
     * {@return the canonical infinity if the argument is {@linkplain
     * #isInfinite(ComplexPolarTextbook) infinite}; return the argument otherwise}
     *
     * @param c a complex number
     */
    public static ComplexPolarTextbook proj(ComplexPolarTextbook c) {
        return isInfinite(c) ? INFINITY : c;
    }

    /**
     * {@return {@code true} if the modulus of the complex number is
     * zero; {@code false} otherwise}
     *
     * @param c a complex number
     */
    public static boolean isZero(ComplexPolarTextbook c) {
        return c.r() == 0.0;
    }

    /**
     * {@return {@code true} if either the magnitude or angle
     * component is NaN; {@code false} otherwise}
     *
     * @param c a complex number
     */
    public static boolean isNaN(ComplexPolarTextbook c) {
        return Double.isNaN(c.r()) || Double.isNaN(c.theta());
    }

    /**
     * {@return {@code true} if either the magnitude or angle
     * component is infinite; {@code false} otherwise}
     *
     * @param c a complex number
     */
    public static boolean isInfinite(ComplexPolarTextbook c) {
        return Double.isInfinite(c.r()) || Double.isInfinite(c.theta());
    }

    /**
     * {@return {@code true} if both the magnitude and angle
     * components are finite; {@code false} otherwise}
     *
     * @param c a complex number
     */
    public static boolean isFinite(ComplexPolarTextbook c) {
        return Double.isFinite(c.r()) && Double.isFinite(c.theta());
    }
}
