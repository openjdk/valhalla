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
 * Indicates a type supports the basic binary arithmetic operations of
 * addition, subtraction, multiplication, (optionally) division, and
 * (optionally) remainder, ({@code +}, {@code -}, {@code *}, {@code
 * /}, {@code %}, respectively), as well as (optionally) negation (unary
 * {@code -}), and participates in operator overloading of those
 * operators.
 *
 * <p>In mathematical terms, various kinds of algebraic structures
 * support the operations modeled by this interface. For example,
 * integers with Euclidean division support the operations in question
 * as do <dfn>algebraic fields</dfn>. Commonly used algebraic fields
 * include rational numbers, real numbers, and complex numbers.  A
 * field has a set of values and operations on those values. The
 * operations have various properties known as the <dfn>field
 * axioms</dfn>. These include associativity of addition and
 * multiplication, commutativity of addition and multiplication, and
 * multiplication distributing over addition. Fields can be
 * {@linkplain Orderable ordered} (rational numbers, real
 * numbers) or unordered (complex numbers).
 *
 * <p>Types used to approximate a field, such as a floating-point type
 * used to approximate real numbers, will both approximate the set of
 * values of the field and the set of properties over the supported
 * operations. In particular, properties like associativity of
 * addition are <em>not</em> expected to hold for a floating-point
 * type.
 *
 * <p>The intention of this interface is to enable types that
 * customarily support numerical notions of addition, subtraction,
 * multiplication and division to enjoy operator overloading syntax
 * even if the underlying algebraic properties do not hold because of
 * limitations in approximation. This includes fields and field-like
 * numbers as well as rings and ring-links numbers.
 *
 * @apiNote
 * Future work: consider interactions with / support from {@link
 * java.util.Formatter} and numerical types.
 *
 * @param <NT> The numerical type
 * @see Orderable
 */
public interface Numerical<NT> {
    /**
     * Addition operation, binary operator "{@code +}".
     *
     * @param addend the first operand
     * @param augend the second operand
     * @return the sum of the operands
     */
     NT add(NT addend, NT augend);

    /**
     * Subtraction operation, binary operator "{@code -}".
     *
     * @implSpec
     * The default implementation returns the sum of the first
     * operand with the negation of the second operand.
     *
     * @param minuend the first operand
     * @param  subtrahend the second operand
     * @return the difference of the operands
     */
    default NT subtract(NT minuend, NT subtrahend) {
        return this.add(minuend, this.negate(subtrahend));
    }

    /**
     * Multiplication operation, binary operator "{@code *}".
     *
     * @param multiplier the first operand
     * @param multiplicand the second operand
     * @return the product of the operands
     */
     NT multiply(NT multiplier, NT multiplicand);

    /**
     * Division operation, binary operator "{@code /}".
     *
     * @apiNote
     * Numerical types can have different policies regarding how
     * divisors equal to zero are handled. Many types will throw an
     * {@code ArithmeticException} in those cases. However, other
     * types like {@linkplain StandardFloatingPoint floating-point
     * types} can return a special value like NaN (not-a-number).
     *
     * @throws ArithmeticException if the divisor is zero and zero
     * divisors are not allowed
     * @throws UnsupportedOperationException if division is not supported
     * @param dividend the first operand
     * @param divisor the second operand
     * @return the quotient of the operands
     */
     NT divide(NT dividend, NT divisor);

    /**
     * Remainder operation, binary operator "{@code %}".
     *
     * @apiNote
     * Numerical types can have different policies regarding how
     * divisors equal to zero are handled. Many types will throw an
     * {@code ArithmeticException} in those cases. However, other
     * types like {@linkplain StandardFloatingPoint floating-point
     * types} can return a special value like NaN (not-a-number).
     *
     * @throws ArithmeticException if the divisor is zero and zero
     * divisors are not allowed
     * @throws UnsupportedOperationException if remainder is not supported
     * @param dividend the first operand
     * @param divisor the second operand
     * @return the quotient of the operands
     */
     NT remainder(NT dividend, NT divisor);

    /**
     * Unary plus operation, unary operator "{@code +}".
     *
     * @apiNote
     * It this needed? Default to returning this/operand? Or just to
     * be be no-op not recognized for overloading?
     *
     * @implSpec
     * The default implementation returns the operand.
     *
     * @param operand the operand
     * @return unary plus of the operand
     */
     default NT plus(NT operand) {
         return operand;
     }

    /**
     * Negation operation, unary operator "{@code -}".
     *
     * @throws UnsupportedOperationException if negation is not supported
     * @param operand the operand
     * @return the negation of the operand
     */
     NT negate(NT operand);
}
