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
 * Indicate a floating-point type in the style of an IEEE 754 standard
 * floating-point type.
 *
 * <p>TODO: write-up about what being an IEEE 754 standard
 * floating-point type means and implies.
 *
 * @apiNote
 * Possible future work: separate subinterface for decimal IEEE 754
 * types.
 *
 * @param <SFP> The standard floating-point type
 */
public interface StandardFloatingPoint<SFP> extends OrderedNumerical<SFP> {

    /**
     * {@inheritDoc Numerical}
     *
     * @apiNote
     * Explain returning NaN on a zero divisor, etc.
     *
     * @param dividend {@inheritDoc Numerical}
     * @param divisor {@inheritDoc Numerical}
     * @return {@inheritDoc Numerical}
     */
    // Note: NOT inheritDoc'ing ArithmeticException on a zero divisor
    @Override
    SFP divide(SFP dividend, SFP divisor);

    /**
     * {@inheritDoc Numerical}
     *
     * @apiNote
     * Explain behavior on a zero divisor, rounding policy, etc.
     *
     * @param dividend {@inheritDoc Numerical}
     * @param divisor {@inheritDoc Numerical}
     * @return {@inheritDoc Numerical}
     */
    // Note: NOT inheritDoc'ing ArithmeticException on a zero divisor
    @Override
    SFP remainder(SFP dividend, SFP divisor);


    /**
     * {@return {@code true} if the operands are considered equal by the
     * IEEE 754 standard and {@code falue} otherwise}
     *
     * @param op1 the first operand
     * @param op2 the second operand
     */
    boolean equalsStd(SFP op1, SFP op2);

    /**
     * {@inheritDoc Orderable}
     *
     * <p>TODO: Need to augment Orderable interface spec with IEEE 754
     * aware one.
     *
     * @param op1 {@inheritDoc Orderable}
     * @param op2 {@inheritDoc Orderable}
     */
     @Override
     boolean lessThan(SFP op1, SFP op2);

    /**
     * {@inheritDoc Orderable}
     *
     * <p>TODO: Need to augment Orderable interface spec with IEEE 754
     * aware one.
     *
     * @apiNote
     * Explain all the IEEE 754-isms.
     *
     * @implSpec
     * Rough draft: the default implementation returns the result of
     * {@code lessThan(op1, op2) || equalsStd(op1, op2)}.
     *
     * @param op1 the {@inheritDoc Orderable}
     * @param op2 the {@inheritDoc Orderable}
     */
     @Override
     default boolean lessThanEqual(SFP op1, SFP op2) {
         return lessThan(op1, op2) || equalsStd(op1, op2);
     }

    /**
     * {@inheritDoc Orderable}
     *
     * <p>TODO: Need to augment Orderable interface spec with IEEE 754
     * aware one.
     *
     * @apiNote
     * Explain all the IEEE 754-isms.
     *
     * @implSpec
     * Rough draft: the default implementation returns the result of
     * {@code !lessThanEqual(op1, op2) && !isUnordered(op1, op2)}.
     *
     * @param op1 {@inheritDoc Orderable}
     * @param op2 {@inheritDoc Orderable}
     */
    @Override
    default boolean greaterThan(SFP op1, SFP op2) {
        return !lessThanEqual(op1, op2) && !isUnordered(op1, op2);
    }

    /**
     * {@inheritDoc Orderable}
     *
     * <p>TODO: Need to augment Orderable interface spec with IEEE 754
     * aware one.
     *
     * @apiNote
     * Explain all the IEEE 754-isms.
     *
     * @implSpec
     * Rough draft: the default implementation returns the result of
     * {@code !lessThan(op1, op2) && !isUnordered(op1, op2)}.
     *
     * @param op1 {@inheritDoc Orderable}
     * @param op2 {@inheritDoc Orderable}
     */
     @Override
     default boolean greaterThanEqual(SFP op1, SFP op2)  {
        return !lessThan(op1, op2) && !isUnordered(op1, op2);
     }

    /**
     * {@return {@code true} if the operands are unordered and {@code false} otherwise}
     *
     * @implSpec
     * Rough draft: the default implementation returns the result of
     * {@code isNaN(op1) || isNaN(op2)}.
     *
     * @param op1 the first operand
     * @param op2 the second operand
     */
    default boolean isUnordered(SFP op1, SFP op2) {
        return isNaN(op1) || isNaN(op2);
    }

    /**
     * {@inheritDoc Orderable}
     *
     * @apiNote
     * TODO: Explain all the IEEE 754-isms, write default method.
     *
     * @param op1 {@inheritDoc Orderable}
     * @param op2 {@inheritDoc Orderable}
     */
    @Override
    SFP min(SFP op1, SFP op2);

    /**
     * {@inheritDoc Orderable}
     *
     * @apiNote
     * TODO: Explain all the IEEE 754-isms, write default method.
     *
     * @param op1 {@inheritDoc Orderable}
     * @param op2 {@inheritDoc Orderable}
     */
    @Override
    SFP max(SFP op1, SFP op2);

    /**
     * {@return the square root of the operand} The square root is
     * computed using the round to nearest rounding policy.
     *
     * @apiNote
     * This method corresponds to the squareRoot operation defined in
     * IEEE 754.
     *
     * @param radicand the argument to have its square root taken
     *
     */
     SFP sqrt(SFP radicand);

    /**
     * Returns the fused multiply add of the three arguments; that is,
     * returns the exact product of the first two arguments summed
     * with the third argument and then rounded once to the nearest
     * floating-point value.
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
     * once to the nearest floating-point value
     */
     SFP fma(SFP a, SFP b, SFP c);

    /**
     * Returns {@code true} if the specified number is a
     * Not-a-Number (NaN) value, {@code false} otherwise.
     *
     * @apiNote
     * This method corresponds to the isNaN operation defined in IEEE
     * 754.
     *
     * @param   operand   the value to be tested.
     * @return  {@code true} if the argument is NaN;
     *          {@code false} otherwise.
     */
     boolean isNaN(SFP operand);

    /**
     * Returns {@code true} if the specified number is infinitely
     * large in magnitude, {@code false} otherwise.
     *
     * @apiNote
     * This method corresponds to the isInfinite operation defined in
     * IEEE 754.
     *
     * @param   operand   the value to be tested.
     * @return  {@code true} if the argument is positive infinity or
     *          negative infinity; {@code false} otherwise.
     */
    boolean isInfinite(SFP operand);

    /**
     * Returns {@code true} if the argument is a finite floating-point
     * value; returns {@code false} otherwise (for NaN and infinity
     * arguments).
     *
     * @apiNote
     * This method corresponds to the isFinite operation defined in
     * IEEE 754.
     *
     * @implSpec
     * Rough draft: the default implementation returns the result of
     * {@code !isInfinite(operand) && !isNaN(operand)}.
     *
     * @param operand the {@code SFP} value to be tested
     * @return {@code true} if the argument is a finite
     * floating-point value, {@code false} otherwise.
     */
    default boolean isFinite(SFP operand) {
        return !isInfinite(operand) && !isNaN(operand);
    }

    /**
     * Returns the size of an ulp of the argument.
     *
     * <p>Special Cases:
     * <ul>
     * <li> If the argument is NaN, then the result is NaN.
     * <li> If the argument is positive or negative infinity, then the
     * result is positive infinity.
     * <li> If the argument is positive or negative zero, then the result is
     * the minimum value of the format.
     * </ul>
     *
     * @param operand the floating-point value whose ulp is to be returned
     * @return the size of an ulp of the argument
     */
    SFP ulp(SFP operand);

    /**
     * Returns a hexadecimal string representation of the argument.
     *
     * @param   operand   the value to be converted.
     * @return a hex string representation of the argument.
     *
     */
    String toHexString(SFP operand);

    // Possible TODO:
    // scaleBy
    // nextUp/nextDown
    // ...
}
