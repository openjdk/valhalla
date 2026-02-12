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
 * Indicates an integral type that supports:
 * <ul>
 * <li>{@linkplain Numerical arithmetic operations} ({@code +}, {@code
 * -}, {@code*}, {@code /}, {@code %}) and so on.
 *
 * <li>{@linkplain Orderable ordered comparison operators}
 * ({@code <}, {@code <=}, {@code >}, {@code >=})
 *
 * <li>integer-related bit-wise operators ({@code &}, {@code |},
 * {@code ^}, {@code ~})
 *
 * <li>shifts ({@code * <<}, {@code >>}, {@code >>>})
 *
 * </ul>
 *
 * and participates in operator overloading of all those operators.
 *
 * @param <IT> The integral type
 */
public interface Integral<IT> extends OrderedNumerical<IT> {

    /**
     * {@return the AND of the two operands, binary operator "{@code &}"}
     *
     * @param op1 the first operand
     * @param op2 the second operand
     */
    IT and(IT op1, IT op2);

    /**
     * {@return the OR of the two operands, binary operator "{@code |}"}
     *
     * @param op1 the first operand
     * @param op2 the second operand
     */
    IT or(IT op1, IT op2);

    /**
     * {@return the XOR of the two operands, binary operator "{@code ^}"}
     *
     * @param op1 the first operand
     * @param op2 the second operand
     */
    IT xor(IT op1, IT op2);

    /**
     * {@return the complement of the operand, unary operator "{@code ~}"}
     *
     * @param op1 the operand
     * @throws UnsupportedOperationException if complement is not supposed
     */
    IT complement(IT op1);

    /**
     * {@return the first operand left shifted by the distance
     * indicated by the second operand, binary operator "{@code <<"}}
     *
     * @param x the operand to be shifted
     * @param shiftDistance the shift distance
     */
    IT shiftLeft(IT x, int shiftDistance);

    /**
     * {@return the first operand right shifted by the distance
     * indicated by the second operand, operator "{@code >>}"}
     *
     * @param x the operand to be shifted
     * @param shiftDistance the shift distance
     */
    IT shiftRight(IT x, int shiftDistance);

    /**
     * {@return the first operand right shifted, unsigned, by the
     * distance indicated by the second operand, operator "{@code >>>}"}
     *
     * @param x the operand to be shifted
     * @param shiftDistance the shift distance
     * @throws UnsupportedOperationException if unsigned right shift is not supposed
     */
    IT shiftRightUnsigned(IT x, int shiftDistance);
}
