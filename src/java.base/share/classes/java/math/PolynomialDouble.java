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

import java.util.Arrays;

/**
 * Polynomials of one variable using {@code double} values to store
 * coefficients of the terms.
 *
 * The exponents on the polynomial terms are 0 (for the constant term)
 * or positive. Negative exponents, for terms like
 * <i>x</i><sup>&minus;1</sup>, are <em>not</em> supported in this
 * class; in other words, Laurent polynomials are not supported.
 *
 * <p>This class models polynomials as a <i>algebraic ring</i>. This
 * includes having a {@linkplain #ZERO zero element} such that
 * multiplying by a zero polynomial results in zero polynomial as the
 * product. This property would <em>not</em> necessarily hold if the
 * underlying operations on the {@code double} floating-point
 * coefficients were done since {@code 0.0 * Infinity} is {@code NaN}
 * and {@code NaN} is <em>not</em> {@code 0.0}.
 *
 * <p>Blanket statement on nulls: if you pass in a null argument, you
 * should usually expect to get a {@code NullPointerException}.
 *
 * @apiNote
 * As a prototype, this class is <em>not</em> intended for production work.
 *
 * <p>A future refinement of a polynomial class could be parameterized,
 * say {@code Polynomial<N extends Numerical>} where {@link Numerical}
 * indicated a type supported the algebraic ring operations of add,
 * subtract, and multiply. If a type supported field-like divide as
 * well, then a polynomial over that type could support divide too.
 *
 * @since Valhalla
 */
@jdk.internal.MigratedValueClass
@jdk.internal.ValueBased
public final /* value */ class PolynomialDouble  {
    /**
     * Witness for the {@code Numerical} interface.
     */
    public static final __witness Numerical<PolynomialDouble> NUM =
        new Numerical<PolynomialDouble>() {

        public PolynomialDouble add(PolynomialDouble addend,
                                    PolynomialDouble augend) {
            return PolynomialDouble.add(addend, augend);
        }

        public PolynomialDouble subtract(PolynomialDouble minuend,
                                         PolynomialDouble subtrahend) {
            return PolynomialDouble.subtract(minuend, subtrahend);
        }

        public PolynomialDouble multiply(PolynomialDouble multiplier,
                                         PolynomialDouble multiplicand) {
            return PolynomialDouble.multiply(multiplier, multiplicand);
        }

        public PolynomialDouble divide(PolynomialDouble dividend,
                                       PolynomialDouble divisor) {
            return PolynomialDouble.divide(dividend, divisor);
        }

        public PolynomialDouble remainder(PolynomialDouble dividend,
                                          PolynomialDouble divisor) {
            return PolynomialDouble.remainder( dividend,  divisor);
        }

        public PolynomialDouble plus(PolynomialDouble operand) {
            return PolynomialDouble.plus(operand);
        }

        public PolynomialDouble negate(PolynomialDouble operand) {
            return PolynomialDouble.negate( operand);
        }
    };

    /*
     * Use a dense array to store the coefficients for nonzero
     * polynomials; coeffs[i] is the coefficient for x^i. Therefore
     * coeffs[0] is the constant term, etc. Putting aside the special
     * case of the zero polynomial, the leading coefficient of a
     * polynomial is nonzero, but any other coefficient may be
     * zero. This leading nonzero constraint is enforced by the
     * valueOf factories.
     *
     * The distinguished zero polynomial is represented using a
     * zero-length coefficients array.
     *
     * The arrays of coefficients should be unshared and not modified
     * after being referenced by a polynomial.
     *
     * Alternatively, an explicit degree instance field could be used
     * along with a length-one array to represent both the zero
     * polynomial and nonzero constant polynomials.
     *
     * Alternatively, a sparse storage scheme for the coefficients
     * could be used, such as a list of (exponent, coefficient)
     * pairs. Such a structure could be used to model Laurent
     * polynomial, which allow negative as well as nonnegative
     * exponents.
     */
    private final double[] coeffs;
    // private final int degree; // could infer this from length of coeffs

    /*
     * Arrays passed to the (private) constructors are trusted.
     * Arrays passed to the (public) valueOf factories are untrusted.
     */

    private static final int IMPL_LIMIT = 1000;

    private PolynomialDouble(double[] coeffs) {
        int length = coeffs.length;
        if (length > IMPL_LIMIT) {
            throw new IllegalArgumentException("Size above implementation limit");
        }
        this.coeffs = coeffs;
    }

     // Used to create the zero polynomial
    private PolynomialDouble() {
        this.coeffs = new double[0];
    }

    private PolynomialDouble(double coeff) {
        assert coeff != 0.0;
        this.coeffs = new double[]{coeff};
    }

    /**
     * The zero polynomial.
     */
    public static final PolynomialDouble ZERO = new PolynomialDouble();

    private boolean isZero() {
        return coeffs.length == 0;
    }

    /**
     * A polynomial of the constant 1.
     */
    public static final PolynomialDouble ONE  = valueOf(1);

    /**
     * {@return a polynomial with a single constant term equal to the argument}
     *
     * @param d the argument
     */
    public static PolynomialDouble valueOf(double d) {
        return (d == 0.0) ?
            ZERO : // +0.0 and -0.0
            new PolynomialDouble(d); // don't special case ONE, for now
    }

    /**
     * {@return a polynomial with coefficients set by the argument values}
     *
     * <p>Describe zero handling, etc.
     *
     * @param coeffs the coefficients
     * @throws IllegalArgumentException if there are no coefficients
     */
    public static PolynomialDouble valueOf(double... coeffs) {
         // Avoid malicious writes; clone upfront.
        double[] clonedCoeffs = coeffs.clone(); // implicit null check
        int length = clonedCoeffs.length;
        if (length == 0) {
            throw new IllegalArgumentException("Zero-length arrays not allowed");
        }
        if (length == 1) {
            return valueOf(clonedCoeffs[0]);
        } else {
            // Check for zeros in high-order components and strip out.
            // See discussion of constrains on coeffs array.
            int i;
            for(i = length; i > 0; i--) {
                if (clonedCoeffs[i -1] == 0) {
                    continue;
                } else {
                    break;
                }
            }

            return (i == 0) ?
                ZERO :
                new PolynomialDouble(Arrays.copyOf(clonedCoeffs, i));
        }
    }

    /**
     * {@return the degree of the polynomial}
     * The {@linkplain #ZERO zero polynomial} is defined to have a
     * degree of -1.
     *
     * @param pd a polynomial
     */
    public static int degree(PolynomialDouble pd) {
        return pd.coeffs.length - 1;
    }

    // Internally use an instance method
    private int deg() {
        return degree(this);
    }

    /**
     * {@return the computed value of the polynomial at the given
     * variable assignment}
     *
     * @implSpec
     * Use Horner's method
     *
     * @param x the assignment of the polynomial's variable
     */
    public double eval(double x) {
        return switch (this.deg()) {
        case -1 -> 0;              // Zero polynomial
        case  0 -> this.coeffs[0]; // Result *not* a function of x.
        default -> horner(this.coeffs, x);
        };
    }

    private static double horner(double[] p, double x) {
        int length = p.length;
        assert length > 0;

        double result = p[length - 1];
        for(int i = length - 2; i >= 0; i--) {
            result = p[i] + x*result;
        }
        return result;
    }

    /**
     * {@return the derivative of the polynomial}
     */
    public PolynomialDouble diff() {
        if (deg() <= 0) { // zero or constant
            return ZERO;
        } else {
            int length = coeffs.length;

            double[] result = new double[length - 1];

            for(int i = 1; i < length; i++) {
                result[i - 1] = coeffs[i] * i;
            }
            return valueOf(result);
        }
    }

    /**
     * {@return the coefficients of this polynomial}
     *
     * The value of {@code coefficients[0]} is the coefficient for
     * <i>x</i><sup>0</sup>, the constant term; the value of {@code
     * coefficients[1]} is the coefficient for <i>x</i><sup>1</sup> =
     * <i>x</i>; and so on with {@code coefficients[i]} being the
     * coefficient for <i>x</i><sup><i>i</i></sup>.
     */
    public double[] coefficients() {
        return  (this.deg() == -1) ?
            (new double[]{0.0}) : // zero special case
            this.coeffs.clone();  // Prevent malicious updates
    }

    // For future consideration.

//     /**
//      * {@return the composition of f and g}
//      *
//      * @param f a polynomial
//      * @param g a polynomial to compose with f
//      */
//     public static double compose(PolynomialDouble f, PolynomialDouble g) {
//         throw new UnsupportedOperationException("compose");
//     }

    /**
     * Addition operation, binary operator "{@code +}".
     *
     * @implSpec
     * Sum coefficients of corresponding terms.
     *
     * @param addend the first operand
     * @param augend the second operand
     * @return the sum of the operands
     */
    public static PolynomialDouble add(PolynomialDouble addend,
                                       PolynomialDouble augend) {
        // For this prototype, don't worry about negative zero
        // coefficients in a nonzero polynomial having their signs
        // changed.
        if(addend.isZero()) {
            return augend;
        }
        if(augend.isZero()) {
            return addend;
        }

        // To simplify the logic, since add is commutative, first
        // argument to add0 has at least as many elements as the
        // second argument.
        return (addend.deg() >= augend.deg()) ?
            add0(addend.coeffs, augend.coeffs):
            add0(augend.coeffs, addend.coeffs);
    }

    /**
     * Compute polynomial sum from raw arrays.
     */
    private static PolynomialDouble add0(double[] x, double[] y) {
        assert x.length >= y.length;
        double[] tmp = new double[x.length];

        int i = 0;
        for( ; i < y.length; i++) {
            tmp[i] = x[i] + y[i];
        }
        if (i < x.length ) {
            for( ; i < x.length; i++) {
                tmp[i] = x[i];
            }
        }
        return valueOf(tmp);
    }

    /**
     * Subtraction operation, binary operator "{@code -}".
     *
     * @implSpec
     * Take the difference of coefficients of corresponding terms.
     *
     * @param minuend the first operand
     * @param  subtrahend the second operand
     * @return the difference of the operands
     */
    public static PolynomialDouble subtract(PolynomialDouble minuend,
                                            PolynomialDouble subtrahend) {
        return add(minuend, negate(subtrahend));
    }

    /**
     * Multiplication operation, binary operator "{@code *}".
     *
     * @apiNote
     * Just a textbook implementation for now.
     *
     * @param multiplier the first operand
     * @param multiplicand the second operand
     * @return the product of the operands
     */
     public static PolynomialDouble multiply(PolynomialDouble multiplier,
                                             PolynomialDouble multiplicand) {
        // For this prototype, don't worry about multiplying a zero
        // polynomial with a polynomial with infinite coefficients
        // generating NaN coefficients in the product. Also don't
        // worry about NaN coefficients not being canceled by being
        // multiplied by zero.
         if (multiplier.isZero() || multiplicand.isZero() ) {
             return ZERO;
         }

         if (multiplier.deg() == 0) {
             return multiplyByScalar(multiplier.coeffs[0], multiplicand);
         }
         if (multiplicand.deg() == 0) {
             return multiplyByScalar(multiplicand.coeffs[0], multiplier);
         }

         // Simple implementation. Could get higher numerical accuracy
         // bucketing a_i*b_j for each z[k] and then doing a
         // compensated summation to compute each z[k].
         double[] x = multiplier.coeffs;
         double[] y = multiplicand.coeffs;
         double[] z = new double[multiplier.deg() + multiplicand.deg() + 1];

         for(int i = 0; i < x.length; i ++) {
             double x_i = x[i];
             for(int j = 0 ; j < y.length; j++) {
                 z[i+j] += x_i * y[j];
             }
         }
         return valueOf(z);
     }

    private static PolynomialDouble multiplyByScalar(double scalar,
                                                     PolynomialDouble poly) {
        return (scalar == 1.0) ?
            poly :
            valueOf(scaleArray(scalar, poly.coeffs));
    }

    private static double[] scaleArray(double scaleFactor, double[] array) {
        double[] result = new double[array.length];
        for(int i = 0; i < result.length; i++) {
            result[i] = scaleFactor * array[i];
        }
        return result;
    }

    /**
     * Division operation, binary operator "{@code /}".
     *
     * @apiNote
     * Synthetic division FTW.
     *
     * @throws ArithmeticException if the divisor is zero
     * @param dividend the first operand
     * @param divisor the second operand
     * @return the quotient of the operands
     */
     public static PolynomialDouble divide(PolynomialDouble dividend,
                                           PolynomialDouble divisor) {
         var qr = divideAndRemainder(dividend, divisor);
         return qr[0];
     }

    /**
     * Remainder operation, binary operator "{@code %}".
     *
     * @apiNote
     * Synthetic division FTW.
     *
     * @throws ArithmeticException if the divisor is zero
     * @param dividend the first operand
     * @param divisor the second operand
     * @return the remainder of the operands
     */
     public static PolynomialDouble remainder(PolynomialDouble dividend,
                                              PolynomialDouble divisor) {
         var qr = divideAndRemainder(dividend, divisor);
         return qr[1];
     }

    /**
     * Compute both the quotient and remainder of the arguments.
     *
     * @apiNote
     * Synthetic division FTW.
     *
     * @throws ArithmeticException if the divisor is zero
     * @param dividend the first operand
     * @param divisor the second operand
     * @return the quotient and remainder of the operands in the
     * zeroth and first positions,, respectively, of the returned
     * array.
     */
     public static PolynomialDouble[] divideAndRemainder(PolynomialDouble dividend,
                                                         PolynomialDouble divisor) {
         if (divisor.isZero()) {
             throw new ArithmeticException("Attempt to divide by a zero polynomial");
         }

         PolynomialDouble workingQuotient  = ZERO;
         PolynomialDouble workingRemainder = dividend;

         // Eschew using operator overloading for now to avoid any bootstrapping issues
         while(!workingRemainder.isZero() &&
               degree(workingRemainder) >= degree(divisor) ) {
             PolynomialDouble tmp = leadQuotient(workingRemainder, divisor);
             // workingQuotient += tmp;
             workingQuotient = add(workingQuotient, tmp);
             // workingRemainder -= tmp * divisor
             workingRemainder = subtract(workingRemainder, multiply(tmp, divisor));
         }
         return new PolynomialDouble[]{workingQuotient, workingRemainder};
     }

    /**
     * {@return a polynomial of dividing the leading terms of p and q}
     * @param p dividend
     * @param q divisor
     */
    private static final PolynomialDouble leadQuotient(PolynomialDouble p,
                                                       PolynomialDouble q) {

        // Probably need some checks here for a zero dividend...
        int pDeg = p.deg();
        int qDeg = q.deg();
        int quotDegree = pDeg - qDeg;
        assert quotDegree >= 0;
        double quotCoeff = p.coeffs[pDeg]/q.coeffs[qDeg];
        double[] quotCoeffs = new double[quotDegree + 1];
        quotCoeffs[quotDegree] = quotCoeff;
        return valueOf(quotCoeffs);
    }

    /**
     * Unary plus operation, unary operator "{@code +}".
     *
     * @implSpec
     * This implementation returns the operand.
     *
     * @param operand the operand
     * @return unary plus of the operand
     */
    public static PolynomialDouble plus(PolynomialDouble operand) {
         return operand;
     }

    /**
     * Negation operation, unary operator "{@code -}".
     *
     * @param operand the operand
     * @return the negation of the operand
     * @throws ArithmeticException if the numerical type does not
     * allow negating the operand in question
     */
    public static PolynomialDouble negate(PolynomialDouble operand) {
        if (operand.isZero()) {
            return ZERO; // Don't worry about negative zeros here
        } else {
            int length = operand.coeffs.length;
            double[] tmp = Arrays.copyOf(operand.coeffs, length);
            for(int i = 0; i < length; i++ ){
                tmp[i] = -1.0 * tmp[i];
            }
            return new PolynomialDouble(tmp);
        }
    }

    /**
     * {@return whether or not the argument is a polynomial equal to this one}
     *
     * @param obj the object to compare to
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof PolynomialDouble that &&
            this.deg() == that.deg() && // Could elide the degree check here
            Arrays.equals(this.coeffs, that.coeffs);
    }

    /**
     * {@return the hashcode of this polynomial}
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(coeffs);
    }

    /**
     * {@return a string representing the polynomial}
     */
    @Override
    public String toString() {
        if (deg() == -1) {
            return "0";
        } else if(deg() == 0) { // nonzero constant
            return Double.toString(coeffs[0]);
        } else {
            StringBuilder sb = new StringBuilder();

            int i = this.coeffs.length - 1;
            String leading = indexToString(this.coeffs[i], i);
            sb.append(leading);
            for(i--; i >= 0; i--) {
                String tmp = indexToString(this.coeffs[i], i);
                if (!tmp.isEmpty() ) {
                    sb.append(" + ").append(tmp);
                }
            }
            if (sb.length() > leading.length()) {
                sb.insert(0, "(");
                sb.append(")");
            }

            return sb.toString();
        }
    }

    private static String indexToString(double coeff, int index) {
        // TOOD: just use string concatenation to get started
        if (coeff == 0.0) {
            return "";
        } else {
            if (index == 0) { // constant
                return Double.toString(coeff);
            } else {
                String indexString = "x";
                if (index > 1) {
                    indexString += "^" + index;
                }
                if (coeff == 1.0) {
                    return indexString;
                } else {
                    return coeff + "*" + indexString;
                }
            }
        }
    }
}
