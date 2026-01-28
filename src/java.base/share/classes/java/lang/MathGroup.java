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
 * Indicates a type conforms to the requirements of the algebraic
 * structure of a group.
 *
 * @apiNote
 * TODO: discuss properties of a group.
 *
 * @param <MG> The type satisfying group properties
 */
public interface MathGroup<MG> extends Numerical<MG> {
    /**
     * {@return the identity element for the group}
     */
    MG zero();

    /**
     * {@inheritDoc Numerical}
     *
     * @implSpec
     * The addition operation is associative for a group.
     *
     * @param addend {@inheritDoc Numerical}
     * @param augend {@inheritDoc Numerical}
     * @return {@inheritDoc Numerical}
     */
    @Override
    MG add(MG addend, MG augend);

    /**
     * {@inheritDoc Numerical}
     *
     * @implSpec
     * The negation of the operand is its inverse in the group.
     *
     * @param operand {@inheritDoc Numerical}
     */
    @Override
    MG negate(MG operand);
}
