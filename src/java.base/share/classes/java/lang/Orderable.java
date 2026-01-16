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

// TODO: could make this interface sealed and only allow an
// "OrderedNumerics" implementation or make this interface extend
// Numerical directly.

/**
 * Indicates a type supports ordered comparison operations ({@code
 * <}, {@code <=}, {@code >}, {@code >=}) and participates in operator
 * overloading of those operators.
 *
 * @param <OC> The type supporting ordered comparison
 * @see Comparable
 */
public interface Orderable<OC> {
    /**
     * {@return {@code true} if the first operand is less than the second
     * operand and {@code false} otherwise}
     *
     * The method corresponds to the less than operator, "{@code <}".
     *
     * @param op1 the first operand
     * @param op2 the second operand
     */
     boolean lessThan(OC op1, OC op2);

    /**
     * {@return {@code true} if the first operand is less than or
     * equal to the second operand and {@code false} otherwise}
     *
     * The method corresponds to the less than operator, "{@code <=}".
     *
     * @param op1 the first operand
     * @param op2 the second operand
     */
     boolean lessThanEqual(OC op1, OC op2);

    /**
     * {@return {@code true} if the first operand is greater than the
     * second operand and {@code false} otherwise}
     *
     * The method corresponds to the greater than operator, "{@code >}".
     *
     * @param op1 the first operand
     * @param op2 the second operand
     */
     boolean greaterThan(OC op1, OC op2);

    /**
     * {@return {@code true} if the first operand is greater than or
     * equal to the second operand and {@code false} otherwise}
     *
     * The method corresponds to the greater than operator, "{@code >=}".
     *
     * @param op1 the first operand
     * @param op2 the second operand
     */
     boolean greaterThanEqual(OC op1, OC op2);

    /**
     * {@return the smaller of the two operands}
     *
     * @apiNote
     * Subtypes of this interface can define policies concerning which
     * operand to return if they are the same size.
     *
     * @param op1 the first operand
     * @param op2 the second operand
     */
     OC min(OC op1, OC op2);

    /**
     * {@return the larger of the two operands}
     *
     * @apiNote
     * Subtypes of this interface can define policies concerning which
     * operand to return if they are the same size.
     *
     * @param op1 the first operand
     * @param op2 the second operand
     */
     OC max(OC op1, OC op2);
}
