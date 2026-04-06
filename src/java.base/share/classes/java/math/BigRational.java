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
 * Rational numbers using {@link BigInteger} values to store the
 * numerator and denominator.
 *
 * <p>TODO: write-up on what a rational number is, (ideally) obeys the
 * field axioms, etc. Describe canonical form, and so on.
 * <p>Blanket statement on null-handling.
 * <p>Explain (<i>a</i>/<i>b</i>) and (<i>c</i>/<i>d</i>) notational convention.
 *
 * @apiNote
 * As a prototype, this class is <em>not</em> intended for production work.
 *
 * @since Valhalla
 */
@jdk.internal.MigratedValueClass
@jdk.internal.ValueBased
//@Deprecated
public final /* value */ class BigRational  {
    private static final OrderedNumerical<BigRational> WITNESS =
        new OrderedNumerical<BigRational>() {
        public BigRational add(BigRational addend,
                               BigRational augend) {
            return BigRational.add(addend, augend);
        }

        public BigRational subtract(BigRational minuend,
                                    BigRational subtrahend) {
            return BigRational.subtract(minuend, subtrahend);
        }

        public BigRational multiply(BigRational multiplier,
                                    BigRational multiplicand) {
            return BigRational.multiply(multiplier, multiplicand);
        }

        public BigRational divide(BigRational dividend,
                                  BigRational divisor) {
            return BigRational.divide(dividend, divisor);
        }

        public BigRational remainder(BigRational dividend,
                                     BigRational divisor) {
            return BigRational.remainder( dividend,  divisor);
        }

        public BigRational plus(BigRational operand) {
            return BigRational.plus(operand);
        }

        public BigRational negate(BigRational operand) {
            return BigRational.negate( operand);
        }

        public boolean lessThan(BigRational op1, BigRational op2) {
            return BigRational.compare(op1, op2) < 0;
        }

        public boolean lessThanEqual(BigRational op1, BigRational op2) {
            return BigRational.compare(op1, op2) <= 0;
        }
    };

    /**
     * Witness for the {@link Numerical} interface.
     */
    public static final __witness Numerical<BigRational> NUM = WITNESS;

    /**
     * Witness for the {@link Orderable} interface.
     */
    public static final __witness Orderable<BigRational> ORD = WITNESS;

    /*
     * If the rational number is negative, the numerator is
     * negative. If the rational number is positive, both numerator
     * and denominator are positive.
     *
     * In this iteration, nothing complicated is attempted to give
     * integral values a distinguished representation, such as having
     * the denominator be a null pointer rather than BigInteger.ONE.
     */
    private final BigInteger num;
    private final BigInteger dem;

    /*
     * Used in lieu of a two-element array to return a pair of values.
     */
    private record Reduced(BigInteger num, BigInteger dem) {}

    private Reduced reduceToLowest(BigInteger num, BigInteger dem) {
        if (BigInteger.ZERO.equals(dem)) {
            throw new ArithmeticException("Attempt to divide by zero");
        }
        if (BigInteger.ZERO.equals(num)) {
            return new Reduced(BigInteger.ZERO, BigInteger.ONE);
        } else {
            int sign = num.signum() * dem.signum();
            var numAbs = num.abs();
            var demAbs = dem.abs();
            BigInteger gcd = numAbs.gcd(demAbs);

            var reducedNum = numAbs.divide(gcd);
            if (sign == -1) {
                reducedNum = reducedNum.negate();
            }
                
            var reducedDem = demAbs.divide(gcd);
            return new Reduced(reducedNum, reducedDem);
        }
    }

    private BigRational(BigInteger num, BigInteger dem) {
        var reduced = reduceToLowest(num, dem);
        this.num = reduced.num;
        this.dem = reduced.dem;
    }

    /**
     * Canonical representation of zero, 0/1.
     */
    public static final BigRational ZERO =
        new BigRational(BigInteger.ZERO, BigInteger.ONE);

    private boolean isZero() {
        return BigInteger.ZERO.equals(num);
    }

    /**
     * Canonical representation of one, 1/1.
     */
    public static final BigRational ONE  =
        new BigRational(BigInteger.ONE, BigInteger.ONE);

    /**
     * {@return a rational number equal to the argument}
     *
     * @param i the argument
     */
    public static BigRational valueOf(int i) {
        return new BigRational(BigInteger.valueOf(i),
                               BigInteger.ONE);
    }
    
    /**
     * {@return a rational number equal to the argument}
     *
     * @param ell the argument
     */
    public static BigRational valueOf(long ell) {
        return new BigRational(BigInteger.valueOf(ell),
                               BigInteger.ONE);
    }

    /**
     * {@return a rational number equal to the value of the argument
     * if the argument is finite} An {@code ArithmeticException} is
     * thrown for non-finite arguments, NaNs and infinities.
     *
     * TODO: signed zero special case; spell out infinity and NaN behavior.
     *
     * @param d the argument
     * @throws ArithmeticException if the argument is not finite.
     */
    public static BigRational valueOf(double d) {
        if (!Double.isFinite(d)) {
            throw new ArithmeticException("non-finite double value " + d);
        }
        // Could do the double -> rational conversion more directly too
        return valueOf(new BigDecimal(d)); // Leverage exact double -> BigDecimal conversion
    }

    /**
     * {@return a rational number equal to the numeric value of the
     * argument}
     *
     * @param bd the argument
     */
    public static BigRational valueOf(BigDecimal bd) {
        BigInteger unscaledValue = copyIfNeeded(bd.unscaledValue());
        int scale = bd.scale();

        var scalingFactor = BigDecimal.TEN.pow(Math.abs(scale)).toBigIntegerExact();

        // Construct final return value as a product or quotient of
        // unscaledValue and scalingFactor.
        if (scale >= 0) {
            return divide(  valueOf(unscaledValue), valueOf(scalingFactor));
        } else {
            return multiply(valueOf(unscaledValue), valueOf(scalingFactor));
        }
    }

    /**
     * {@return a rational number equal to the argument}
     *
     * @param bi the argument
     */
    public static BigRational valueOf(BigInteger bi) {
        return new BigRational(copyIfNeeded(bi), BigInteger.ONE);
    }

    /**
     * {@return a rational number constructed from the string argument}
     * TODO: Talk about the grammar of the accepted strings, etc.
     *
     * @param s the string to parse into a rational number
     * @throws NumberFormatException if the string is not a valid representation
     */
    public static BigRational valueOf(String s) {
        // Look for "/" if not present, parse as big int, ...
        int slashOffset = s.indexOf("/");
        try {
            if (slashOffset == -1) {
                return valueOf(new BigInteger(s), BigInteger.ONE);
            } else {
                BigInteger numerator   = new BigInteger(s.substring(0, slashOffset));
                BigInteger denominator = new BigInteger(s.substring(slashOffset + 1));
                return valueOf(numerator, denominator);
            }
        } catch(NumberFormatException cause) {
            var nfe = new NumberFormatException("Malformed rational number");
            nfe.initCause(cause);
            throw nfe;
        }
    }

    /**
     * {@return a rational number equal to the ratio of the arguments,
     * in lowest terms}
     *
     * @param numerator the numerator
     * @param denominator the denominator
     */
    public static BigRational valueOf(int numerator, int denominator) {
        return new BigRational(BigInteger.valueOf(numerator),
                               BigInteger.valueOf(denominator));
    }
    
    /**
     * {@return a rational number equal to the ratio of the arguments,
     * in lowest terms}
     *
     * @param numerator the numerator
     * @param denominator the denominator
     */
    public static BigRational valueOf(long numerator, long denominator) {
        return new BigRational(BigInteger.valueOf(numerator),
                               BigInteger.valueOf(denominator));
    }
    
    /**
     * {@return a rational number equal to the ratio of the arguments,
     * in lowest terms}
     *
     * @param numerator the numerator
     * @param denominator the denominator
     */
    public static BigRational valueOf(BigInteger numerator,
                                      BigInteger denominator) {
        return new BigRational(copyIfNeeded(numerator),
                               copyIfNeeded(denominator));
    }
    
    /*
     * Guard against unknown BigInteger subclass.
     */
    private static BigInteger copyIfNeeded(BigInteger bi) {
        return (bi.getClass() == BigInteger.class) ?
            bi  :
            new BigInteger(bi.toByteArray());
    }

    /**
     * {@return a {@code BigDecimal} with the numerical value of the
     * argument rounding according to the context settings}
     *
     * @param rat the rational number to convert
     * @param mc  the math context to use in the conversion
     */
    public static BigDecimal toBigDecimal(BigRational rat, MathContext mc) {
        return (new BigDecimal(rat.num)).divide(new BigDecimal(rat.dem), mc);
    }

    /**
     * {@return a {@code BigDecimal} equal to the argument, if possible}
     *
     * @param rat the rational number to convert
     * @throws ArithmeticException is an exact conversion is not possible
     */
    public static BigDecimal toBigDecimalExact(BigRational rat) {
        return (new BigDecimal(rat.num)).divide(new BigDecimal(rat.dem));
    }

    /**
     * Addition operation, binary operator "{@code +}".
     *
     * @implSpec
     * (<i>a</i>/<i>b</i>) + (<i>c</i>/<i>d</i>) =
     * (<i>a</i>&middot;<i>d</i> + <i>b</i>&middot;<i>c</i>) / (<i>b</i>&middot;<i>d</i>)
     *
     * @param addend the first operand
     * @param augend the second operand
     * @return the sum of the operands
     */
    public static BigRational add(BigRational addend,
                                  BigRational augend) {
        if(addend.isZero()) {
            return augend;
        }
        if(augend.isZero()) {
            return addend;
        }

        var a = addend.num; var b = addend.dem;
        var c = augend.num; var d = augend.dem;

        // (ad + bc)/(bd)
        return valueOf(a.multiply(d).add(b.multiply(c)),
                       b.multiply(d));
    }

    /**
     * Subtraction operation, binary operator "{@code -}".
     *
     * @implSpec
     * (<i>a</i>/<i>b</i>) &minus; (<i>c</i>/<i>d</i>) =
     * (<i>a</i>&middot;<i>d</i> &minus; <i>b</i>&middot;<i>c</i>) / (<i>b</i>&middot;<i>d</i>)
     *
     * @param minuend the first operand
     * @param  subtrahend the second operand
     * @return the difference of the operands
     */
    public static BigRational subtract(BigRational minuend,
                                       BigRational subtrahend) {
        // Equivalent to (ad - bc)/(bd)
        return add(minuend, negate(subtrahend));
    }

    /**
     * Multiplication operation, binary operator "{@code *}".
     *
     * @implSpec
     * (<i>a</i>/<i>b</i>) * (<i>c</i>/<i>d</i>) =
     * (<i>a</i>&middot;<i>c</i>)/(<i>b</i>&middot;<i>d</i>)
     *
     * @param multiplier the first operand
     * @param multiplicand the second operand
     * @return the product of the operands
     */
     public static BigRational multiply(BigRational multiplier,
                                        BigRational multiplicand) {
         if (multiplier.isZero() || multiplicand.isZero() ) {
             return ZERO;
         }

         var a = multiplier.num;   var b = multiplier.dem;
         var c = multiplicand.num; var d = multiplicand.dem;

         // (ac)/(bd)
         return valueOf(a.multiply(c), b.multiply(d));
     }

    /**
     * Division operation, binary operator "{@code /}".
     *
     * @implSpec
     * (<i>a</i>/<i>b</i>) / (<i>c</i>/<i>d</i>) =
     * (<i>a</i>&middot;<i>d</i>)/(<i>b</i>&middot;<i>c</i>)
     *
     * @throws ArithmeticException if the divisor is zero
     * @param dividend the first operand
     * @param divisor the second operand
     * @return the quotient of the operands
     */
     public static BigRational divide(BigRational dividend,
                                      BigRational divisor) {
         if (divisor.isZero() ) {
             throw new ArithmeticException("attempt to divide by zero");
         }
         if (dividend.isZero()) {
             return ZERO;
         }

         var a = dividend.num; var b = dividend.dem;
         var c = divisor.num;  var d = divisor.dem;

         // (ad)/(bc)
         return valueOf(a.multiply(d), b.multiply(c));
     }

    /**
     * Remainder operation, binary operator "{@code %}".
     *
     * @throws ArithmeticException if the divisor is zero
     * @param dividend the first operand
     * @param divisor the second operand
     * @return the remainder of the operands
     */
     public static BigRational remainder(BigRational dividend,
                                         BigRational divisor) {
        throw new ArithmeticException("tbd");
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
    public static BigRational plus(BigRational operand) {
         return operand;
     }

    /**
     * Negation operation, unary operator "{@code -}".
     *
     * @param operand the operand
     * @return the negation of the operand
     */
    public static BigRational negate(BigRational operand) {
        return new BigRational(operand.num.negate(), operand.dem);
    }


    /**
     * {@return the reciprocal of the operand}
     *
     * @param operand the operand
     */
    public static BigRational reciprocal(BigRational operand) {
        if (operand.isZero()) {
            throw new ArithmeticException("Zero does not have a reciprocal");
        }
        return new BigRational(operand.dem, operand.num);
    }

    /**
     * Returns the exponentiation of the first operand by the second
     * operand.
     *
     * @param operand the operand
     * @param exponent the exponent
     * @return the first operands raised to the power the second operand
     */
    public static BigRational pow(BigRational operand, int exponent) {
        if (exponent == 0) {
            return BigRational.ONE;
        }

        // TODO: screen out Integer.MIN_VALUE
        // For a negative exponent, flip numerator and denominator
        return (exponent > 0) ?
            valueOf(operand.num.pow( exponent), operand.dem.pow( exponent)):
            valueOf(operand.dem.pow(-exponent), operand.num.pow(-exponent));
    }

    /**
     * {@return the comparison of two specified rational values}
     *
     * @implSpec
     * {@code ad < bd}, etc.
     *
     * @param op1 the first operand
     * @param op2 the second operand
     *
     * @see Double#compare(double, double)
     * @see BigInteger#compareTo(BigInteger)
     */
    public static int compare(BigRational op1,
                              BigRational op2) {
        var a = op1.num; var b = op1.dem;
        var c = op2.num; var d = op2.dem;

        int signumOp1 = a.signum();
        int signumOp2 = c.signum();

        if (signumOp1 < signumOp2) {
            return -1;
        } else if (signumOp1 > signumOp2) {
            return 1;
        } else { // signumOp1 == signumOp2
            if (signumOp1 == 0) {
                return 0;
            } else {
                var a_d = a.multiply(d);
                var b_c = b.multiply(c);

                return a_d.compareTo(b_c);
            }
        }
    }
    
    /**
     * {@return the hashcode of this rational}
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(num.hashCode(), dem.hashCode());
    }

    /**
     * {@return a string representing the rational}
     */
    @Override
    public String toString() {
        // Numerator always represented
        var sb = new StringBuilder(num.toString());
        if (!BigInteger.ONE.equals(dem)) {
            sb.append("/").append(dem.toString());
        }
        return sb.toString();
    }
}
