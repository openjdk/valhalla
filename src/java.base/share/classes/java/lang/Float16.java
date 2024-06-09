/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.MethodHandles;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.util.Optional;

import jdk.internal.math.FloatConsts;
import jdk.internal.math.FloatingDecimal;
import jdk.internal.math.FloatToDecimal;
import jdk.internal.vm.annotation.IntrinsicCandidate;

/**
 * The {@code Float16} is a primitive value class holding 16-bit data in IEEE 754 binary16 format
 * {@code Float16} contains a single field whose type is {@code short}.
 *
 * Binary16 Format:
 *   S EEEEE  MMMMMMMMMM
 *   Sign        - 1 bit
 *   Exponent    - 5 bits
 *   Significand - 10 bits
 *
 * <p>This is a <a href="https://openjdk.org/jeps/401">primitive value class</a> and its objects are
 * identity-less non-nullable value objects.
 *
 * @author Jatin Bhateja
 * @since 20.00
 */

// Currently Float16 is a value based class but in future will be aligned with
// Enhanced Primitive Boxes described by JEP-402 (https://openjdk.org/jeps/402)
@jdk.internal.MigratedValueClass
@jdk.internal.ValueBased
public final class Float16 extends Number {
    private final short value;

   /**
    * Returns a {@code Float16} instance wrapping IEEE 754 binary16
    * encoded {@code short} value.
    *
    * @param  value a short value.
    * @since  20
    */
    private Float16 (short value ) {
        this.value = value;
    }

   /**
    * Returns a {@code Float16} instance wrapping IEEE 754 binary16
    * encoded {@code short} value.
    *
    * @param  value a short value.
    * @return a {@code Float16} instance representing {@code value}.
    * @since  20
    */
    public static Float16 valueOf(short value) {
       return new Float16(value);
    }

    /**
     * Returns the value of this {@code Float16} as a {@code byte} after
     * a narrowing primitive conversion.
     *
     * @return  the binary16 encoded {@code short} value represented by this object
     *          converted to type {@code byte}
     * @jls 5.1.3 Narrowing Primitive Conversion
     */
    public byte byteValue() {
        return (byte)Float.float16ToFloat(value);
    }

    /**
     * Returns the value of this {@code Float16} as a {@code short}
     * after a narrowing primitive conversion.
     *
     * @return  the binary16 encoded {@code short} value represented by this object
     *          converted to type {@code short}
     * @jls 5.1.3 Narrowing Primitive Conversion
     * @since 1.1
     */
    public short shortValue() {
        return (short)Float.float16ToFloat(value);
    }

    /**
     * Returns the value of this {@code Float16} as an {@code int} after
     * a widening primitive conversion.
     *
     * @return  the binary16 encoded {@code short} value represented by this object
     *          converted to type {@code int}
     * @jls 5.1.3 Widening Primitive Conversion
     */
    public int intValue() {
        return (int)Float.float16ToFloat(value);
    }

    /**
     * Returns value of this {@code Float16} as a {@code long} after a
     * widening conversion.
     *
     * @return  the binary16 encoded {@code short} value represented by this object
     *          converted to type {@code long}
     * @jls 5.1.3 Widening Primitive Conversion
     */
    public long longValue() {
        return (long)Float.float16ToFloat(value);
    }

    /**
     * Returns the {@code float} value of this {@code Float16} object.
     *
     * @return the binary16 encoded {@code short} value represented by this object
     *         converted to type {@code float}
     */
    public float floatValue() {
        return Float.float16ToFloat(value);
    }

    /**
     * Returns the value of this {@code Float16} as a {@code double}
     * after a widening primitive conversion.
     *
     * @apiNote
     * This method corresponds to the convertFormat operation defined
     * in IEEE 754.
     *
     * @return the binary16 encoded {@code short} value represented by this
     *         object converted to type {@code double}
     * @jls 5.1.2 Widening Primitive Conversion
     */
    public double doubleValue() {
        return (double)Float.float16ToFloat(value);
    }

    /**
     * Adds two {@code Float16} values together as per the + operator semantics.
     *
     * @apiNote This method corresponds to the addition operation
     * defined in IEEE 754.
     *
     * @param a the first operand
     * @param b the second operand
     * @return the sum of {@code a} and {@code b}
     * @since 20
     */
    @IntrinsicCandidate
    public static Float16 sum(Float16 a, Float16 b) {
       return Float16.valueOf(Float.floatToFloat16(Float.float16ToFloat(a.float16ToRawShortBits()) + Float.float16ToFloat(b.float16ToRawShortBits())));
    }

    /**
     * Return raw short value.
     * @return raw binary16 encoded {@code short} value represented by this object.
     * @since 20
     */
    public short float16ToRawShortBits() { return value; }

    private static final long serialVersionUID = 16; // Not needed for a primitive class?
}
