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

public primitive class Float16 {
   private final short value;

  /**
   * Returns a {@code Float16} instance wrapping IEEE 754 binary16
   * encoded {@code short} value.
   *
   * @param  value a short value.
   * @since  20
   */
   public Float16 (short value ) {
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
    * Adds two {@code Float16} values together as per the + operator semantics.
    *
    * @apiNote This method corresponds to the addition operation
    * defined in IEEE 754.
    *
    * @param value the first operand
    * @return sum of receiver and {@code value)}.
    * @since 20
    */
   @IntrinsicCandidate
   public Float16 add(Float16 value) {
      return Float16.valueOf(Float.floatToFloat16(Float.float16ToFloat(this.value) + Float.float16ToFloat(value.value)));
   }

   /**
    * Return raw short value.
    * @return raw short value {@code value)}.
    * @since 20
    */
   public short float16ToRawShortBits() { return value; }
}
