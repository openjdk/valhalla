/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8329817
 * @summary Basic tests of Float16 arithmetic and similar operations
 */

import static java.lang.Float16.*;

public class BasicFloat16ArithTests {
    public static void main(String... args) {
        checkConstants();
        checkNegate();
        checkAbs();
        checkIsNaN();
        checkFiniteness();
        checkMinMax();
        checkArith();
        checkSqrt();
    }

    /*
     * The software implementation of Float16 delegates to float or
     * double operations for most of the actual computation. This
     * regression test takes that into account as it generally only
     * has limited tested to probe whether or not the proper
     * functionality is being delegated to.
     *
     * To make the test easier to read, float literals that are exact
     * upon conversion to Float16 are used for the test data.
     *
     * The float <-> Float16 conversions are well-tested from prior
     * work and are assumed to be correct by this regression test.
     */

    private static void checkConstants() {
        checkInt(BYTES,          2, "Float16.BYTES");
        checkInt(MAX_EXPONENT,  15, "Float16.MAX_EXPONENT");
        checkInt(MIN_EXPONENT, -14, "Float16.MIN_EXPONENT");
        checkInt(PRECISION,     11, "Float16.PRECISION");
        checkInt(SIZE,          16, "Float16.SIZE");

        checkFloat16(MIN_VALUE,  0x1.0p-24f, "Float16.MIN_VALUE");
        checkFloat16(MIN_NORMAL, 0x1.0p-14f, "Float16.MIN_NORMAL");
        checkFloat16(MAX_VALUE,  65504.0f,  "Float16.MAX_VALUE");

        checkFloat16(POSITIVE_INFINITY,  Float.POSITIVE_INFINITY,  "+infinity");
        checkFloat16(NEGATIVE_INFINITY,  Float.NEGATIVE_INFINITY,  "-infinity");
        checkFloat16(NaN,                Float.NaN,            "NaN");
    }
    
    private static void checkInt(int value, int expected, String message) {
        if (value != expected) {
            throwRE(String.format("Didn't get expected value for %s;%nexpected %d, got %d",
                                  message, expected, value));
        }
    }

    private static void checkFloat16(Float16 value16, float expected, String message) {
        float value = value16.floatValue();
        if (Float.compare(value, expected) != 0) {
            throwRE(String.format("Didn't get expected value for %s;%nexpected %g (%a), got %g (%a)",
                                  message, expected, expected, value, value));
        }
    }

    private static void checkNegate() {
        float[][] testCases = {
            {-0.0f,   0.0f},
            { 0.0f,  -0.0f},

            {-1.0f,   1.0f},
            { 1.0f,  -1.0f},

            {Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY},
            {Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY},

            {Float.NaN, Float.NaN},
        };

        for(var testCase : testCases) {
            float arg =      testCase[0];
            float expected = testCase[1];
            Float16 result =  negate(valueOf(arg));

            if (Float.compare(expected, result.floatValue()) != 0) {
                checkFloat16(result, expected, "negate(" + arg + ")");
            }
        }

        return;
    }

    private static void checkAbs() {
        float[][] testCases = {
            {-0.0f,   0.0f},
            { 0.0f,   0.0f},

            {-1.0f,   1.0f},
            { 1.0f,   1.0f},

            {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY},
            {Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY},

            {Float.NaN, Float.NaN},
        };

        for(var testCase : testCases) {
            float arg =      testCase[0];
            float expected = testCase[1];
            Float16 result =  abs(valueOf(arg));

            if (Float.compare(expected, result.floatValue()) != 0) {
                checkFloat16(result, expected, "abs(" + arg + ")");
            }
        }

        return;
    }

    private static void checkIsNaN() {
        if (!isNaN(NaN)) {
            throwRE("Float16.isNaN() returns false for a NaN");
        }

        float[] testCases = {
            Float.NEGATIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            -0.0f,
            +0.0f,
             1.0f,
            -1.0f,
        };

        for(var testCase : testCases) {
            boolean result = isNaN(valueOf(testCase));
            if (result) {
                throwRE("isNaN returned true for " + testCase);
            }
        }

        return;
    }

    private static void checkFiniteness() {
        float[] infinities = {
            Float.NEGATIVE_INFINITY,
            Float.POSITIVE_INFINITY,
        };

        for(var infinity : infinities) {
            boolean result1 = isFinite(valueOf(infinity));
            boolean result2 = isInfinite(valueOf(infinity));

            if (result1) {
                throw new RuntimeException("Float16.isFinite returned true for " + infinity);
            }

            if (!result2) {
                throwRE("Float16.isInfinite returned false for " + infinity);
            }
        }

        if (isFinite(NaN)) {
            throwRE("Float16.isFinite() returns true for a NaN");
        }

        if (isInfinite(NaN)) {
            throwRE("Float16.isInfinite() returns true for a NaN");
        }

        float[] finities = {
            -0.0f,
            +0.0f,
             1.0f,
            -1.0f,
        };

        for(var finity : finities) {
            boolean result1 = isFinite(valueOf(finity));
            boolean result2 = isInfinite(valueOf(finity));

            if (!result1) {
                throwRE("Float16.isFinite returned true for " + finity);
            }

            if (result2) {
                throwRE("Float16.isInfinite returned true for " + finity);
            }
        }

        return;
    }

    private static void checkMinMax() {
        float small = 1.0f;
        float large = 2.0f;

        if (min(valueOf(small), valueOf(large)).floatValue() != small) {
            throwRE(String.format("min(%g, %g) not equal to %g)",
                                  small, large, small));
        }

        if (max(valueOf(small), valueOf(large)).floatValue() != large) {
            throwRE(String.format("max(%g, %g) not equal to %g)",
                                  small, large, large));
        }
    }

    /*
     * Cursory checks to make sure correct operation is being called
     * with arguments in proper order.
     */
    private static void checkArith() {
        float   a   = 1.0f;
        Float16 a16 = valueOf(a);

        float   b   = 2.0f;
        Float16 b16 = valueOf(b);

        if (add(a16, b16).floatValue() != (a + b)) {
            throwRE("failure with " + a16 + " + " + b16);
        }
        if (add(b16, a16).floatValue() != (b + a)) {
            throwRE("failure with " + b16 + " + " + a16);
        }

        if (subtract(a16, b16).floatValue() != (a - b)) {
            throwRE("failure with " + a16 + " - " + b16);
        }
        if (subtract(b16, a16).floatValue() != (b - a)) {
            throwRE("failure with " + b16 + " - " + a16);
        }

        if (multiply(a16, b16).floatValue() != (a * b)) {
            throwRE("failure with " + a16 + " * " + b16);
        }
        if (multiply(b16, a16).floatValue() != (b * a)) {
            throwRE("failure with " + b16 + " * " + a16);
        }

        if (divide(a16, b16).floatValue() != (a / b)) {
            throwRE("failure with " + a16 + " / " + b16);
        }
        if (divide(b16, a16).floatValue() != (b / a)) {
            throwRE("failure with " + b16 + " / " + a16);
        }
        return;
    }

    private static void checkSqrt() {
        float[][] testCases = {
            {-0.0f,   -0.0f},
            { 0.0f,    0.0f},

            {1.0f,   1.0f},
            {4.0f,   2.0f},
            {9.0f,   3.0f},

            {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY},
            {Float.NEGATIVE_INFINITY, Float.NaN},

            {Float.NaN, Float.NaN},
        };

        for(var testCase : testCases) {
            float arg =      testCase[0];
            float expected = testCase[1];
            Float16 result =  sqrt(valueOf(arg));

            if (Float.compare(expected, result.floatValue()) != 0) {
                checkFloat16(result, expected, "sqrt(" + arg + ")");
            }
        }

        return;
    }

    private static void throwRE(String message) {
        throw new RuntimeException(message);
    }
}
