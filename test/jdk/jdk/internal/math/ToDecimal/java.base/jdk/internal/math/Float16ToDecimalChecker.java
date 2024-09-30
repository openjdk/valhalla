/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package jdk.internal.math;

import java.math.BigDecimal;

import static java.lang.Float16.*;
import static java.lang.Integer.numberOfTrailingZeros;
import static jdk.internal.math.MathUtils.flog10pow2;

public class Float16ToDecimalChecker extends ToDecimalChecker {

    private static final int P =
            numberOfTrailingZeros(float16ToRawShortBits(Float16.valueOf(3))) + 2;
    private static final int W = (SIZE - 1) - (P - 1);
    private static final int Q_MIN = (-1 << (W - 1)) - P + 3;
    private static final int Q_MAX = (1 << (W - 1)) - P;
    private static final int C_MAX = (1 << P) - 1;

    private static final int H = flog10pow2(P) + 2;

    private static final Float16 MIN_VALUE = scalb(Float16.valueOf(1), Q_MIN);
    private static final Float16 MAX_VALUE = scalb(Float16.valueOf(C_MAX), Q_MAX);

    private static final int E_MIN = e(MIN_VALUE.doubleValue());
    private static final int E_MAX = e(MAX_VALUE.doubleValue());

    private final Float16 v;

    private Float16ToDecimalChecker(Float16 v) {
        super(Float16ToDecimal.toString(v));
//        super(Float.toString(v));
        this.v = v;
    }

    @Override
    int h() {
        return H;
    }

    @Override
    int maxStringLength() {
        return H + 5;
    }

    @Override
    BigDecimal toBigDecimal() {
        return new BigDecimal(v.floatValue());
    }

    @Override
    boolean recovers(BigDecimal bd) {
        return bd.float16Value().floatValue() == v.floatValue();
    }

    @Override
    boolean recovers(String s) {
        return Float16.valueOf(s).floatValue() == v.floatValue();
    }

    @Override
    String hexString() {
        return toHexString(v) + "F16";
    }

    @Override
    int minExp() {
        return E_MIN;
    }

    @Override
    int maxExp() {
        return E_MAX;
    }

    @Override
    boolean isNegativeInfinity() {
        return v.floatValue() == Float.NEGATIVE_INFINITY;
    }

    @Override
    boolean isPositiveInfinity() {
        return v.floatValue() == Float.POSITIVE_INFINITY;
    }

    @Override
    boolean isMinusZero() {
        return float16ToRawShortBits(v) == 0xFFFF_8000;
    }

    @Override
    boolean isPlusZero() {
        return float16ToRawShortBits(v) == 0x0000;
    }

    @Override
    boolean isNaN() {
        return Float16.isNaN(v);
    }

    private static void testDec(Float16 v) {
        new Float16ToDecimalChecker(v).check();
    }

    public static void test() {
        int bits = Short.MIN_VALUE;
        for (; bits <= Short.MAX_VALUE; ++bits) {
            testDec(shortBitsToFloat16((short) bits));
        }
        throwOnErrors("Float16ToDecimalChecker");
    }

}
