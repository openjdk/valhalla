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
 * <i>x</i><sup>&minus;1</sup> are <em>not</em> supported in this
 * class.
 *
 * <p>Blanket statement on nulls: if you pass in a null argument, you
 * should usually expect to get a {@code NullPointerException}.
 *
 * @apiNote
 * This is a prototype class not intended for production work.
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

    /**
     * coeffs[i] is the coeffs for x^i. Therefore coeffs[0] is the
     * constant term, etc.
     *
     * The coeffs arrays should be unshared and not modified after
     * being referenced by a polynomial.
     */
    private final double[] coeffs;
    private final int degree; // could infer this from length of coeffs

    private static final int IMPL_LIMIT = 1000;

    private PolynomialDouble(double[] coeffs) {
        int length = coeffs.length;
        if (length > IMPL_LIMIT) {
            throw new IllegalArgumentException("Size above implementation limit");
        }
        degree = length - 1;
        this.coeffs = coeffs;
    }

    private PolynomialDouble(double coeff, int degree) {
        double[] tmp = new double[1];
        tmp[0] = coeff;
        this.coeffs = tmp;
        this.degree = degree;
    }

    private PolynomialDouble(double coeff) {
        double[] tmp = new double[1];
        tmp[0] = coeff;
        this.coeffs = tmp;
        this.degree = 0;
    }

    /**
     * The zero polynomial.
     */
    public static final PolynomialDouble ZERO = new PolynomialDouble(0.0, -1);

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
     * Describe zero handling, etc.
     *
     * @param coeffs the coefficients
     * @throws IllegalArgumentException if there are zero coefficients
     */
    public static PolynomialDouble valueOf(double... coeffs) {
        int length = coeffs.length; // implicit null check
        if (length == 0) {
            throw new IllegalArgumentException("Zero-length arrays not allowed");
        }
        if (length == 1) {
            return valueOf(coeffs[0]);
        } else {
            // Check for zeros in high-order components and strip out.
            int i;
            for(i = length; i > 0; i--) {
                if (coeffs[i -1] == 0) {
                    continue;
                } else {
                    break;
                }
            }

            return (i == 0) ?
                ZERO :
                new PolynomialDouble(Arrays.copyOf(coeffs, i));
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
        return pd.degree;
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
        if (this.degree <= 0) { // zero or constant
            // Result not a function of x.
            return this.coeffs[0];
        } else {
            int length = this.coeffs.length;
            double result = this.coeffs[length - 1];

            for(int i = length - 2; i >= 0; i--) {
                result = this.coeffs[i] + x*result;
            }
            return result;
        }
    }

    /**
     * {@return the derivative of the polynomial}
     */
    public PolynomialDouble diff() {
        if (this.degree <= 0) { // zero or constant
            return ZERO;
        } else {
            int length = coeffs.length;

            double[] result = new double[length - 1];

            for(int i = 1; i < length; i++) {
                result[i-1] = coeffs[i] * i;
            }
            return valueOf(result);
        }
    }

    /**
     * {@return the coefficients of this polynomial}
     *
     * The value of {@code coefficients[0]} is the coeffiecnet for
     * <i>x</i><sup>0</sup>, the constnat term; the value of {@code
     * coefficients[1]} is the coefficeint for <i>x</i><sup>1</sup> =
     * <i>x</i>; and so on with {@code coefficients[i]} being the
     * coefficient for <i>x</i><sup><i>i</i></sup>.
     */
    public double[] coefficients() {
        return this.coeffs.clone(); // Prevent malicious updates
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
     * @param addend the first operand
     * @param augend the second operand
     * @return the sum of the operands
     */
    public static PolynomialDouble add(PolynomialDouble addend,
                                       PolynomialDouble augend) {
        if(ZERO.equals(addend)) {
            return augend;
        }
        if(ZERO.equals(augend)) {
            return addend;
        }

        // x has at least as many elements as y
        double[] x, y;
        if (addend.degree >= augend.degree) {
            x = addend.coeffs;
            y = augend.coeffs;
        } else {
            x = augend.coeffs;
            y = addend.coeffs;
        }

        // could simplify to x.length per invariant above
        double[] tmp = new double[Math.max(x.length, y.length)];

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
         if (ZERO.equals(multiplier) || ZERO.equals(multiplicand) ) {
             return ZERO;
         }

         if (multiplier.degree == 0) {
             return multiplyByScalar(multiplier.coeffs[0], multiplicand);
         }
         if (multiplicand.degree == 0) {
             return multiplyByScalar(multiplicand.coeffs[0], multiplier);
         }

         // Simple implementation. Could get higher numerical accuracy
         // bucketing a_i*b_j for each z[k] and then doing a
         // compensated summation to compute each z[k].
         double[] x = multiplier.coeffs;
         double[] y = multiplicand.coeffs;
         double[] z = new double[multiplier.degree + multiplicand.degree + 1];

         for(int i = 0; i < x.length; i ++) {
             double x_i = x[i];
             for(int j = 0 ; j < y.length; j++) {
                 z[i+j] += x_i * y[j];
             }
         }
         return valueOf(z);
     }

    private static PolynomialDouble multiplyByScalar(double scalar, PolynomialDouble poly) {
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
         if (ZERO.equals(divisor)) {
             throw new ArithmeticException("Attempt to divide by a zero polynomial");
         }

         PolynomialDouble workingQuotient = ZERO;
         PolynomialDouble workingRemainder = dividend;

         // Eschew using operator overloading for now to avoid any bootstrapping issues
         while(!ZERO.equals(workingRemainder) &&
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
        int pDeg = p.degree;
        int qDeg = q.degree;
        int quotDegree = pDeg - qDeg;
        assert quotDegree >= 0;
        double quotCoeff = p.coeffs[pDeg]/q.coeffs[qDeg];
        double[] quotCoeffs = new double[quotDegree+1];
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
        if (ZERO.equals(operand)) {
            return ZERO; // Don't worry about negative zeros here
        } else {
            int length = operand.coeffs.length;
            double[] tmp = Arrays.copyOf(operand.coeffs, length);
            for(int i = 0; i < length; i++ ){
                tmp[i] = -1.0* tmp[i];
            }
            return new PolynomialDouble(tmp);
        }
    }

    /**
     * {@return whether or not the argument is a polynomial equal to this one}
     *
     * @param o the object to compare to
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof PolynomialDouble that &&
            this.degree == that.degree &&
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
        if (degree <= 0) { // constant or zero
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
