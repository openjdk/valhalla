/*
 * Copyright (c) 2016, 2025, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8340339
 * @modules jdk.incubator.vector
 * @build Bfloat16
 * @run main BasicBfloat16ArithTests
 * @summary Basic tests of Bfloat16 arithmetic and similar operations
 */

import jdk.incubator.vector.Float16;
// import static Bfloat16.*; Cannot do static import from an unnamed package
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

public class BasicBfloat16ArithTests {
    private static float InfinityF = Float.POSITIVE_INFINITY;
    private static float NaNf = Float.NaN;

    private static final float MAX_VAL_FP16 = 0x1.fep127f;
    private static final float MIN_VAL_FP16 = 0x1.0p-133f;

    public static void main(String... args) {
        checkBitWise();
        checkHash();
        checkConstants();
        checkNegate();
        checkAbs();
        checkIsNaN();
        checkFiniteness();
        checkMinMax();
        checkArith();
        checkSqrt();
        checkGetExponent();
        checkUlp();
        checkValueOfFloat16();
        checkValueOfDoubleSimple();
        checkValueOfDouble();
        checkValueOfLong();
        checkValueOfBigDecimal();
        checkValueOfString();
        // checkBaseConversionRoundTrip();
        FusedMultiplyAddTests.main();
    }

    /*
     * The software implementation of Bfloat16 delegates to float or
     * double operations for most of the actual computation. This
     * regression test takes that into account as it generally only
     * has limited testing to probe whether or not the proper
     * functionality is being delegated to.
     *
     * To make the test easier to read, float literals that are exact
     * upon conversion to Bfloat16 are used for the test data.
     *
     * The float <-> Bfloat16 conversions are well-tested from prior
     * work and are assumed to be correct by this regression test.
     */

    /**
     * Verify handling of NaN representations
     */
    private static void checkBitWise() {
        // TOOD: port
        short nanImage = Bfloat16.bfloat16ToRawShortBits(Bfloat16.NaN);

        int exponent = 0x7F80;
        int sign =     0x8000;

        // All-zeros significand with a max exponent are infinite
        // values, not NaN values.
        for(int i = 0x1; i <= 0x007f; i++) {
            short  posNaNasShort = (short)(       exponent | i);
            short  negNaNasShort = (short)(sign | exponent | i);

            Bfloat16 posf16 = Bfloat16.shortBitsToBfloat16(posNaNasShort);
            Bfloat16 negf16 = Bfloat16.shortBitsToBfloat16(negNaNasShort);

            // Mask-off high-order 16 bits to avoid sign extension woes
            checkInt(nanImage & 0xffff, Bfloat16.bfloat16ToShortBits(posf16) & 0xffff, "positive NaN");
            checkInt(nanImage & 0xffff, Bfloat16.bfloat16ToShortBits(negf16) & 0xffff, "negative NaN");

            checkInt(posNaNasShort & 0xffff, Bfloat16.bfloat16ToRawShortBits(posf16) & 0xffff , "positive NaN");
            checkInt(negNaNasShort & 0xffff, Bfloat16.bfloat16ToRawShortBits(negf16) & 0xffff, "negative NaN");
        }
    }

    /**
     * Verify correct number of hashValue's from Bfloat16's.
     */
    private static void checkHash() {
        // Slightly over-allocate the HashSet.
        HashSet<Integer> set = HashSet.newHashSet(Short.MAX_VALUE - Short.MIN_VALUE + 1);

        // Each non-NaN value should have a distinct hashCode. All NaN
        // values should share a single hashCode. Check the latter
        // property by verifying the overall count of entries in the
        // set.
        for(int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
            Bfloat16 f16 = Bfloat16.shortBitsToBfloat16((short)i);
            boolean addedToSet = set.add(f16.hashCode());

            if (!Bfloat16.isNaN(f16)) {
                if (!addedToSet) {
                    throwRE("Existing hash value for " + f16);
                }
            }
        }

        // There are 2^16 = 65,536 total short values. Each of these
        // bit patterns is a valid representation of a Bfloat16
        // value. However, NaNs have multiple possible encodings.
        // With an exponent = 0x7f80, each nonzero significand 0x1 to
        // 0x007f is a NaN, for both positive and negative sign bits.
        //
        // Therefore, the total number of distinct hash codes for
        // Bfloat16 values should be:
        // 65_536 - 2*(127) + 1 = 65_283

        int NaNcount = 2*((1 << (Bfloat16.PRECISION - 1)) - 1);

        int setSize = set.size();
        if (setSize != (65_536 - NaNcount + 1)) {
            throwRE("Unexpected number of distinct hash values " + setSize);
        }
    }

    private static void checkConstants() {
        checkInt(Bfloat16.BYTES,           2, "Bfloat16.BYTES");
        checkInt(Bfloat16.MAX_EXPONENT,  127, "Bfloat16.MAX_EXPONENT");
        checkInt(Bfloat16.MIN_EXPONENT, -126, "Bfloat16.MIN_EXPONENT");
        checkInt(Bfloat16.PRECISION,      8, "Bfloat16.PRECISION");
        checkInt(Bfloat16.SIZE,          16, "Bfloat16.SIZE");

        checkBfloat16(Bfloat16.MIN_VALUE,  0x1.0p-133f, "Bfloat16.MIN_VALUE");
        checkBfloat16(Bfloat16.MIN_NORMAL, 0x1.0p-126f, "Bfloat16.MIN_NORMAL");
        checkBfloat16(Bfloat16.MAX_VALUE,  0x1.fep127f,  "Bfloat16.MAX_VALUE");

        checkBfloat16(Bfloat16.POSITIVE_INFINITY,   InfinityF,  "+infinity");
        checkBfloat16(Bfloat16.NEGATIVE_INFINITY,  -InfinityF,  "-infinity");
        checkBfloat16(Bfloat16.NaN,                 NaNf,            "NaN");
    }

    private static void checkInt(int value, int expected, String message) {
        if (value != expected) {
            throwRE(String.format("Didn't get expected value for %s;%nexpected %d, got %d",
                                  message, expected, value));
        }
    }

    private static void checkBfloat16(Bfloat16 value16, float expected, String message) {
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

            { InfinityF, -InfinityF},
            {-InfinityF,  InfinityF},

            {NaNf,       NaNf},
        };

        for(var testCase : testCases) {
            float arg =      testCase[0];
            float expected = testCase[1];
            Bfloat16 result =  Bfloat16.negate(valueOfExact(arg));

            if (Float.compare(expected, result.floatValue()) != 0) {
                checkBfloat16(result, expected, "negate(" + arg + ")");
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

            { InfinityF, InfinityF},
            {-InfinityF, InfinityF},

            {NaNf,       NaNf},
        };

        for(var testCase : testCases) {
            float arg =      testCase[0];
            float expected = testCase[1];
            Bfloat16 result =  Bfloat16.abs(valueOfExact(arg));

            if (Float.compare(expected, result.floatValue()) != 0) {
                checkBfloat16(result, expected, "abs(" + arg + ")");
            }
        }

        return;
    }

    private static void checkIsNaN() {
        if (!Bfloat16.isNaN(Bfloat16.NaN)) {
            throwRE("Bfloat16.isNaN() returns false for a NaN");
        }

        float[] testCases = {
            -InfinityF,
             InfinityF,
            -0.0f,
            +0.0f,
             1.0f,
            -1.0f,
        };

        for(var testCase : testCases) {
            boolean result = Bfloat16.isNaN(valueOfExact(testCase));
            if (result) {
                throwRE("isNaN returned true for " + testCase);
            }
        }

        return;
    }

    private static void checkFiniteness() {
        float[] infinities = {
            -InfinityF,
             InfinityF,
        };

        for(var infinity : infinities) {
            boolean result1 = Bfloat16.isFinite(valueOfExact(infinity));
            boolean result2 = Bfloat16.isInfinite(valueOfExact(infinity));

            if (result1) {
                throwRE("Bfloat16.isFinite returned true for " + infinity);
            }

            if (!result2) {
                throwRE("Bfloat16.isInfinite returned false for " + infinity);
            }
        }

        if (Bfloat16.isFinite(Bfloat16.NaN)) {
            throwRE("Bfloat16.isFinite() returns true for a NaN");
        }

        if (Bfloat16.isInfinite(Bfloat16.NaN)) {
            throwRE("Bfloat16.isInfinite() returns true for a NaN");
        }

        float[] finities = {
            -0.0f,
            +0.0f,
             1.0f,
            -1.0f,
        };

        for(var finity : finities) {
            boolean result1 = Bfloat16.isFinite(valueOfExact(finity));
            boolean result2 = Bfloat16.isInfinite(valueOfExact(finity));

            if (!result1) {
                throwRE("Bfloat16.isFinite returned true for " + finity);
            }

            if (result2) {
                throwRE("Bfloat16.isInfinite returned true for " + finity);
            }
        }

        return;
    }

    private static void checkMinMax() {
        float small = 1.0f;
        float large = 2.0f;

        if (Bfloat16.min(valueOfExact(small), valueOfExact(large)).floatValue() != small) {
            throwRE(String.format("min(%g, %g) not equal to %g)",
                                  small, large, small));
        }

        if (Bfloat16.max(valueOfExact(small), valueOfExact(large)).floatValue() != large) {
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
        Bfloat16 a16 = valueOfExact(a);

        float   b   = 2.0f;
        Bfloat16 b16 = valueOfExact(b);

        if (Bfloat16.add(a16, b16).floatValue() != (a + b)) {
            throwRE("failure with " + a16 + " + " + b16);
        }
        if (Bfloat16.add(b16, a16).floatValue() != (b + a)) {
            throwRE("failure with " + b16 + " + " + a16);
        }

        if (Bfloat16.subtract(a16, b16).floatValue() != (a - b)) {
            throwRE("failure with " + a16 + " - " + b16);
        }
        if (Bfloat16.subtract(b16, a16).floatValue() != (b - a)) {
            throwRE("failure with " + b16 + " - " + a16);
        }

        if (Bfloat16.multiply(a16, b16).floatValue() != (a * b)) {
            throwRE("failure with " + a16 + " * " + b16);
        }
        if (Bfloat16.multiply(b16, a16).floatValue() != (b * a)) {
            throwRE("failure with " + b16 + " * " + a16);
        }

        if (Bfloat16.divide(a16, b16).floatValue() != (a / b)) {
            throwRE("failure with " + a16 + " / " + b16);
        }
        if (Bfloat16.divide(b16, a16).floatValue() != (b / a)) {
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

            { InfinityF, InfinityF},
            {-InfinityF, NaNf},

            {NaNf,       NaNf},
        };

        for(var testCase : testCases) {
            float arg       = testCase[0];
            float expected  = testCase[1];
            Bfloat16 result = Bfloat16.sqrt(valueOfExact(arg));

            if (Float.compare(expected, result.floatValue()) != 0) {
                checkBfloat16(result, expected, "sqrt(" + arg + ")");
            }
        }

        return;
    }

    private static void checkGetExponent() {
        float[][] testCases = {
            // Non-finite values
            { InfinityF,    Bfloat16.MAX_EXPONENT + 1},
            {-InfinityF,    Bfloat16.MAX_EXPONENT + 1},
            { NaNf,         Bfloat16.MAX_EXPONENT + 1},

            // Subnormal and almost subnormal values
            {-0.0f,         Bfloat16.MIN_EXPONENT - 1},
            {+0.0f,         Bfloat16.MIN_EXPONENT - 1},

            // Bfloat16 cardinal values
            { 0x1.0p-133f,  Bfloat16.MIN_EXPONENT - 1}, // Bfloat16.MIN_VALUE
            {-0x1.0p-133f,  Bfloat16.MIN_EXPONENT - 1}, // Bfloat16.MIN_VALUE
            { 0x1.0p-126f,  Bfloat16.MIN_EXPONENT},     // Bfloat16.MIN_NORMAL
            {-0x1.0p-126f,  Bfloat16.MIN_EXPONENT},     // Bfloat16.MIN_NORMAL
            { 0x1.fep127f,  Bfloat16.MAX_EXPONENT},     // Bfloat16.MAX_NORMAL
            {-0x1.fep127f,  Bfloat16.MAX_EXPONENT},     // Bfloat16.MAX_NORMAL

            // Float16 cardinal values
            { 0x1.0p-24f,   -24},                       // Float16.MIN_VALUE
            {-0x1.0p-24f,   -24},                       // Float16.MIN_VALUE
            { 0x1.0p-14f,   -14},                       // Float16.MIN_NORMAL
            {-0x1.0p-14f,   -14},                       // Float16.MIN_NORMAL
            { 0x1.ffcp15f,   16},                       // Float16.MAX_VALUE (rounds up)
            {-0x1.ffcp15f,   16},                       // Float16.MAX_VALUE (rounds up)

            // Normal values
            { 1.0f,       0},
            { 2.0f,       1},
            { 4.0f,       2},

            {MAX_VAL_FP16*0.5f, Bfloat16.MAX_EXPONENT - 1},
            {MAX_VAL_FP16,      Bfloat16.MAX_EXPONENT},
        };

        for(var testCase : testCases) {
            float arg =      testCase[0];
            float expected = testCase[1];
            float result =  Bfloat16.getExponent(Bfloat16.valueOf(arg));

            if (Float.compare(expected, result) != 0) {
                checkBfloat16(Bfloat16.valueOf(result), expected, "getExponent(" + arg + ")");
            }
        }
        return;
    }

    private static void checkUlp() {
        float[][] testCases = {
            { InfinityF, InfinityF},
            {-InfinityF, InfinityF},
            { NaNf,      NaNf},

            // Zeros, subnormals, and MIN_VALUE all have MIN_VALUE as an ulp.
            {-0.0f,        0x1.0p-133f},
            {+0.0f,        0x1.0p-133f},
            { 0x1.0p-133f, 0x1.0p-133f},
            {-0x1.0p-133f, 0x1.0p-133f},
            { 0x1.0p-132f, 0x1.0p-133f},
            {-0x1.0p-132f, 0x1.0p-133f},
            { 0x1.0p-127f, 0x1.0p-133f},
            {-0x1.0p-127f, 0x1.0p-133f},

            // ulp is 7 bits away
            {0x1.0p0f,       0x1.0p-7f}, // 1.0f
            {0x1.0p1f,       0x1.0p-6f}, // 2.0f
            {0x1.0p2f,       0x1.0p-5f}, // 4.0f

            { MAX_VAL_FP16,  0x1.0p120f}, //  MAX_VALUE
            {-MAX_VAL_FP16,  0x1.0p120f}, // -MAX_VALUE

//             {MAX_VAL_FP16*0.5f, 0x0.004p14f},
//             {MAX_VAL_FP16,      0x0.004p15f},
        };

        for(var testCase : testCases) {
            float arg =      testCase[0];
            float expected = testCase[1];
            // Exponents are in-range for Bfloat16
            Bfloat16 result =  Bfloat16.ulp(valueOfExact(arg));

            if (Float.compare(expected, result.floatValue()) != 0) {
                checkBfloat16(result, expected, "ulp(" + arg + ")");
            }
        }
        return;
    }

    private static void throwRE(String message) {
        throw new RuntimeException(message);
    }

    private static void checkValueOfFloat16() {
        // {Float16 value stored as float, Float16 value converted to Bfloat16 stored as float}
        float[][] testCases = {
            {InfinityF, InfinityF},
            {NaNf, NaNf},

            {-0.0f, -0.0f},
            {+0.0f, +0.0f},

            {1.0f,   1.0f},
            {-1.0f, -1.0f},

            {65504.0f,   0x1.0p16f},  // Float16.MIN_VALUE rounds up in Bfloat16.
            {0x1.0p-14f, 0x1.0p-14f}, // Float16.MIN_NORMAL carries over to BFloat16
            {0x1.0p-24f, 0x1.0p-24f}, // Float16.MIN_VALUE carries over to BFloat16
        };

        for(var testCase : testCases) {
            float input     = testCase[0];
            float expected  = testCase[1];
            Bfloat16 actual = Bfloat16.valueOf(Float16.valueOf(input));

            checkBfloat16(actual, expected, "Unexpected conversion of Float16 value" + input);
        }
    }

    private static void checkValueOfDoubleSimple() {
        // Check special cases first; double value to be converted to
        // BFloat16, then converted back
        double[][] testCases = {
            {InfinityF, InfinityF},
            {NaNf, NaNf},

            {-0.0, -0.0},
            {+0.0, +0.0},

            { Double.MIN_VALUE,  0.0},
            {-Double.MIN_VALUE, -0.0},

            { 0x1.0p-133f * 0.5d,  0.0},
            {-0x1.0p-133f * 0.5d, -0.0},

            { 0x1.0p-1024d,  0.0},
            {-0x1.0p-1024d, -0.0},

            {1.0, 1.0},
            {2.0, 2.0},

            {0x1.fep127f + 0x0.00fp127f, (Bfloat16.MAX_VALUE).doubleValue()},
            {0x1.fep127f + 0x0.01p127f,  InfinityF},
            {0x1.fep127f + 0x0.02p127f,  InfinityF},

            {0x1.0p-126f, 0x1.0p-126}, // Bfloat16.MIN_NORMAL
            {0x1.fp-126f, 0x1.fp-126},

            // Reorder after code fixed
             {Math.nextUp( 0x1.0p-133f * 0.5d),  0x1.0p-133}, // Bfloat16.MIN_VALUE
        };

        System.out.println(Bfloat16.toHexString(Bfloat16.MIN_VALUE));
        System.out.println(Double.toHexString(Bfloat16.MIN_VALUE.doubleValue()));

        for(var testCase : testCases) {
            double input   = testCase[0];
            double expected = testCase[1];
            double actual = Bfloat16.valueOf(input).doubleValue();

            if (Double.compare(expected, actual) != 0) {
                System.err.println("Unexpected result handling " + input + " (" + Double.toHexString(input) + ")");
                System.err.println("Expected  " + expected + " (" + Double.toHexString(expected) + ")" + " got " + actual + " (" +  Double.toHexString(actual) + ")");
                throw new RuntimeException();
            }
        }
    }

    private static void checkValueOfDouble() {
        /*
         * Check that double -> Bfloat16 conversion rounds properly
         * around the midway point for each finite Bfloat16 value by
         * looping over the positive values and checking the negations
         * along the way.
         */

        String roundUpMsg   = "Didn't get half-way case rounding down";
        String roundDownMsg = "Didn't get half-way case rounding up";

        for(int i = 0; i <= Short.MAX_VALUE; i++ ) {
            boolean isEven = ((i & 0x1) == 0);
            Bfloat16 f16 = Bfloat16.shortBitsToBfloat16((short)i);
            Bfloat16 f16Neg = Bfloat16.negate(f16);

            if (!Bfloat16.isFinite(f16))
                continue;

            Bfloat16 ulp = Bfloat16.ulp(f16);
            double halfWay = f16.doubleValue() + ulp.doubleValue() * 0.5;

            // Under the round to nearest even rounding policy, the
            // half-way case should round down to the starting value
            // if the starting value is even; otherwise, it should round up.
            float roundedBack = Bfloat16.valueOf(halfWay).floatValue();
            float roundedBackNeg = Bfloat16.valueOf(-halfWay).floatValue();

            if (isEven) {
                checkBfloat16(f16,    roundedBack,    roundDownMsg);
                checkBfloat16(f16Neg, roundedBackNeg, roundDownMsg);
            } else {
                checkBfloat16(Bfloat16.add(f16,         ulp), roundedBack,    roundUpMsg);
                checkBfloat16(Bfloat16.subtract(f16Neg, ulp), roundedBackNeg, roundUpMsg);
            }

            // Should always round down
            double halfWayNextDown = Math.nextDown(halfWay);
            checkBfloat16(f16,    Bfloat16.valueOf(halfWayNextDown).floatValue(),  roundDownMsg);
            checkBfloat16(f16Neg, Bfloat16.valueOf(-halfWayNextDown).floatValue(), roundDownMsg);

            // Should always round up
            double halfWayNextUp =   Math.nextUp(halfWay);
            checkBfloat16(Bfloat16.add(f16, ulp),         Bfloat16.valueOf( halfWayNextUp).floatValue(), roundUpMsg);
            checkBfloat16(Bfloat16.subtract(f16Neg, ulp), Bfloat16.valueOf(-halfWayNextUp).floatValue(), roundUpMsg);
        }
    }

    private static void checkValueOfLong() {
        checkBfloat16(Bfloat16.valueOf(0L),  0.0f, "zero");
        checkBfloat16(Bfloat16.valueOf(1L),  1.0f, "one");
        checkBfloat16(Bfloat16.valueOf(2L),  2.0f, "two");
        checkBfloat16(Bfloat16.valueOf(Long.MIN_VALUE),  -0x1.0p63f, "MIN_VALUE");
        checkBfloat16(Bfloat16.valueOf(Long.MAX_VALUE),   0x1.0p63f, "MAX_VALUE");

        // Values near +/- 2^54, limit where double can hold all contiguous integers
        checkBfloat16(Bfloat16.valueOf( 0x40_0000_0000_0000L),   0x1.0p54f, "transition");
        checkBfloat16(Bfloat16.valueOf(-0x40_0000_0000_0000L),  -0x1.0p54f, "transition");
        checkBfloat16(Bfloat16.valueOf( 0x3f_ffff_ffff_ffffL),   0x1.0p54f, "transition");
        checkBfloat16(Bfloat16.valueOf(-0x3f_ffff_ffff_ffffL),  -0x1.0p54f, "transition");

        // Probe around rounding transition
        checkBfloat16(Bfloat16.valueOf(0x7F80_0000_0000_0001L),  0x1.fep62f, "transition");
        checkBfloat16(Bfloat16.valueOf(0x7FB0_0000_0000_0001L),  0x1.fep62f, "transition");
        checkBfloat16(Bfloat16.valueOf(0x7FBF_0000_0000_0001L),  0x1.fep62f, "transition");
        checkBfloat16(Bfloat16.valueOf(0x7F80_0000_0000_0000L),  0x1.fep62f, "transition");

        checkBfloat16(Bfloat16.valueOf(0x7FC0_0000_0000_0000L),  0x1.00p63f, "transition");

        checkBfloat16(Bfloat16.valueOf(0x7_fbff_ffff_ffff_dffL), 0x1.fe0p62f, "rounding");

        // Double-rounding hazard if full argument is converted to
        // double first before a second conversion to Bfloat16.
        checkBfloat16(Bfloat16.valueOf( 0x7_fbff_ffff_ffff_fffL),  0x1.fe0p62f, "rounding");
        checkBfloat16(Bfloat16.valueOf(-0x7_fbff_ffff_ffff_fffL), -0x1.fe0p62f, "rounding");

        checkBfloat16(Bfloat16.valueOf(0x7_ffff_ffff_ffff_fffL), 0x1.00p63f, "rounding");
    }

    private static void checkValueOfBigDecimal() {
        Bd2Bfloat16Case[] testCases = {
            new Bd2Bfloat16Case(BigDecimal.ZERO,          0.0f),

            new Bd2Bfloat16Case(BigDecimal.ONE,           1.0f),
            new Bd2Bfloat16Case(new BigDecimal("1.0"),    1.0f),
            new Bd2Bfloat16Case(BigDecimal.TEN,           10.0f),
            new Bd2Bfloat16Case(new BigDecimal("10.0"),   10.0f),
            new Bd2Bfloat16Case(new BigDecimal("100"),    100.0f),
            new Bd2Bfloat16Case(new BigDecimal("100.0"),  100.0f),
            new Bd2Bfloat16Case(new BigDecimal("1000"),   1000.0f),
            new Bd2Bfloat16Case(new BigDecimal("1000.0"), 1000.0f),

            new Bd2Bfloat16Case(new BigDecimal("9984"), 9984.0f), // exact value
            new Bd2Bfloat16Case(new BigDecimal("9984.0"), 9984.0f),

            new Bd2Bfloat16Case(new BigDecimal("10000"), 9984.0f), // rounding

            new Bd2Bfloat16Case(new BigDecimal("0.25"),  0.25f),
            new Bd2Bfloat16Case(new BigDecimal("0.125"), 0.125f),
            new Bd2Bfloat16Case(new BigDecimal("0.0625"), 0.0625f),

            new Bd2Bfloat16Case(new BigDecimal("0.6875"), 0.6875f), // "0x1.6p-1"

            // Convert +/-0x7_fbff_ffff_ffff_fffL; would experience
            // double-rounding if a direct conversion to double was
            // done before converting to Bfloat16.
            new Bd2Bfloat16Case(new BigDecimal( "9205357638345293823"),  0x1.fe0p62f),
            new Bd2Bfloat16Case(new BigDecimal("-9205357638345293823"), -0x1.fe0p62f),


        };

        for(Bd2Bfloat16Case testCase : testCases) {
            BigDecimal input = testCase.input();
            float expected = testCase.expected();
            Bfloat16 result = Bfloat16.valueOf(input);
            checkBfloat16(result, expected, "Bfloat16.valueOf(BigDecimal) " + input);
        }

        return;
    }

    private static record Bd2Bfloat16Case(BigDecimal input, float expected) {
    }

    private static void checkValueOfString() {
        String2Bfloat16Case[] testCases = {
            new String2Bfloat16Case( "NaN", NaNf),
            new String2Bfloat16Case("+NaN", NaNf),
            new String2Bfloat16Case("-NaN", NaNf),

            new String2Bfloat16Case("+Infinity", +InfinityF),
            new String2Bfloat16Case("-Infinity", -InfinityF),

            new String2Bfloat16Case( "0.0",  0.0f),
            new String2Bfloat16Case("+0.0",  0.0f),
            new String2Bfloat16Case("-0.0", -0.0f),

            // Decimal signed integers are accepted as input; hex
            // signed integers are not, see negative test cases below.
            new String2Bfloat16Case( "1",  1.0f),
            new String2Bfloat16Case("-1", -1.0f),

            new String2Bfloat16Case( "12",  12.0f),
            new String2Bfloat16Case("-12", -12.0f),

            new String2Bfloat16Case( "123",  123.0f),
            new String2Bfloat16Case("-123", -123.0f),

            new String2Bfloat16Case( "1.0",  1.0f),
            new String2Bfloat16Case("-1.0", -1.0f),

            // Check for FloatTypeSuffix handling
            new String2Bfloat16Case( "1.5f", 1.5f),
            new String2Bfloat16Case( "1.5F", 1.5f),
            new String2Bfloat16Case( "1.5D", 1.5f),
            new String2Bfloat16Case( "1.5d", 1.5f),

            new String2Bfloat16Case("65504.0", 0x1.0p16f),  // Float16.MAX_VALUE, rounds up in Bfloat16

            // Bfloat16.MAX_VALUE in hex and decimal
            new String2Bfloat16Case("0x1.fep127", 0x1.fep127f),
            new String2Bfloat16Case("338953138925153547590470800371487866880.0", 0x1.fep127f),

            // Bfloat16.MAX_VALUE + 0.5*ulp in hex and decimal
            new String2Bfloat16Case("0x1.ffp127", InfinityF),
            new String2Bfloat16Case("339617752923046005526922703901628039168.0", InfinityF),


//             new String2Bfloat16Case("65520.01", InfinityF), // Bfloat16.MAX_VALUE + > 0.5*ulp
//             new String2Bfloat16Case("65520.001", InfinityF), // Bfloat16.MAX_VALUE + > 0.5*ulp
//             new String2Bfloat16Case("65520.0001", InfinityF), // Bfloat16.MAX_VALUE + > 0.5*ulp
//             new String2Bfloat16Case("65520.00000000001", InfinityF), // Bfloat16.MAX_VALUE + > 0.5*ulp

//             new String2Bfloat16Case("65519.99999999999", 65504.0f), // Bfloat16.MAX_VALUE +  < 0.5*ulp
//             new String2Bfloat16Case("0x1.ffdffffffffffp15", 65504.0f),
//             new String2Bfloat16Case("0x1.ffdfffffffffp15", 65504.0f), // -- FIXME


//             new String2Bfloat16Case("65519.999999999999", 65504.0f),
//             new String2Bfloat16Case("65519.9999999999999", 65504.0f),
//             new String2Bfloat16Case("65519.99999999999999", 65504.0f),
//             new String2Bfloat16Case("65519.999999999999999", 65504.0f),

//             // Bfloat16.MAX_VALUE +  < 0.5*ulp
//             new String2Bfloat16Case("65519.9999999999999999999999999999999999999", 65504.0f),

//             // Near MAX_VALUE - 0.5 ulp
//             new String2Bfloat16Case("65488.0", 65472.0f),
//             new String2Bfloat16Case("65487.9999", 65472.0f),
//             new String2Bfloat16Case("65487.99999999", 65472.0f),
//             new String2Bfloat16Case("65487.9999999999999999", 65472.0f),

//             new String2Bfloat16Case("65488.000001", MAX_VAL_FP16),

//             new String2Bfloat16Case("65536.0", InfinityF), // Bfloat16.MAX_VALUE + ulp

            // Double-rounding hazard
            new String2Bfloat16Case("0x7fbffffffffffdffp0",   0x1.fe0p62f),
            new String2Bfloat16Case("0x7fbffffffffffdffp-1",  0x1.fe0p61f),
            new String2Bfloat16Case("0x7fbffffffffffdffp-64", 0x1.fe0p-02f),

            new String2Bfloat16Case("0x7fffffffffffffffp0",   0x1.00p63f),

            // Hex values
            new String2Bfloat16Case("0x1p2",   0x1.0p2f),
            new String2Bfloat16Case("0x1p2f",  0x1.0p2f),
            new String2Bfloat16Case("0x1p2d",  0x1.0p2f),
            new String2Bfloat16Case("0x1.0p1", 0x1.0p1f),

            new String2Bfloat16Case("-0x1p2",  -0x1.0p2f),
            new String2Bfloat16Case("0x3.48p12", 0x3.48p12f),

            new String2Bfloat16Case("0x3.4800000001p12", 0x3.48p12f),

            // Near half-way double + float cases in hex
            //            new String2Bfloat16Case("0x1.ffdfffffffffffffffffffffffffffffffffffp15", 65504.0f),

        };

        for(String2Bfloat16Case testCase : testCases) {
            String input = testCase.input();
            float expected = testCase.expected();
            Bfloat16 result = Bfloat16.valueOf(input);
            checkBfloat16(result, expected, "Bfloat16.valueOf(String) " + input);
        }

        List<String> negativeCases = List.of("0x1",
                                       "-0x1",
                                        "0x12",
                                       "-0x12");

        for(String negativeCase : negativeCases) {
            try {
                Bfloat16 f16 = Bfloat16.valueOf(negativeCase);
                throwRE("Did not get expected exception for input " + negativeCase);
            } catch (NumberFormatException nfe) {
                ; // Expected
            }
        }

        return;
    }

    private static record String2Bfloat16Case(String input, float expected) {
    }

    private static void checkBaseConversionRoundTrip() {
        checkBfloat16(Bfloat16.NaN,
                     Bfloat16.valueOf("NaN").floatValue(),
                     "base conversion of NaN");

        // For each non-NaN value, make sure
        // value -> string -> value
        // sequence of conversions gives the expected result.

        for(int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
            Bfloat16 f16 = Bfloat16.shortBitsToBfloat16((short)i);
            if (Bfloat16.isNaN(f16))
                continue;

            checkBfloat16(f16,
                         Bfloat16.valueOf(Bfloat16.toString(f16)).floatValue(),
                         "base conversion");
        }
        return;
    }

    private static class FusedMultiplyAddTests {
        public static void main(String... args) {
            testZeroNanInfCombos();
            testNonFinite();
            testZeroes();
            testSimple();
            testRounding();
        }

        private static void testZeroNanInfCombos() {
            float [] testInputs = {
                Float.NaN,
                -InfinityF,
                +InfinityF,
                -0.0f,
                +0.0f,
            };

            for (float i : testInputs) {
                for (float j : testInputs) {
                    for (float k : testInputs) {
                        testFusedMacCase(i, j, k, Math.fma(i, j, k));
                    }
                }
            }
        }

        private static void testNonFinite() {
            float [][] testCases = {
                {1.0f,       InfinityF,  2.0f,
                 InfinityF},

                {1.0f,       2.0f,       InfinityF,
                 InfinityF},

                {InfinityF,  1.0f,       InfinityF,
                 InfinityF},

//                 {0x1.ffcp14f, 2.0f,     -InfinityF, // TOOD
//                  -InfinityF},

                {InfinityF,  1.0f,      -InfinityF,
                 NaNf},

                {-InfinityF, 1.0f,       InfinityF,
                 NaNf},

                {1.0f,       NaNf,       2.0f,
                 NaNf},

                {1.0f,       2.0f,       NaNf,
                 NaNf},

                {InfinityF,  2.0f,       NaNf,
                 NaNf},

                {NaNf,       2.0f,       InfinityF,
                 NaNf},
            };

            for (float[] testCase: testCases) {
                testFusedMacCase(testCase[0], testCase[1], testCase[2], testCase[3]);
            }
        }

        private static void testZeroes() {
            float [][] testCases = {
                {+0.0f, +0.0f, +0.0f,
                 +0.0f},

                {-0.0f, +0.0f, +0.0f,
                 +0.0f},

                {+0.0f, +0.0f, -0.0f,
                 +0.0f},

                {+0.0f, +0.0f, -0.0f,
                 +0.0f},

                {-0.0f, +0.0f, -0.0f,
                 -0.0f},

                {-0.0f, -0.0f, -0.0f,
                 +0.0f},

                {-1.0f, +0.0f, -0.0f,
                 -0.0f},

                {-1.0f, +0.0f, +0.0f,
                 +0.0f},

                {-2.0f, +0.0f, -0.0f,
                 -0.0f},
            };

            for (float[] testCase: testCases) {
                testFusedMacCase(testCase[0], testCase[1], testCase[2], testCase[3]);
            }
        }

        private static void testSimple() {
            final float ulpOneFp16 = Bfloat16.ulp(valueOfExact(1.0f)).floatValue();

            float [][] testCases = {
                {1.0f, 2.0f, 3.0f,
                 5.0f},

                {1.0f, 2.0f, -2.0f,
                 0.0f},

                {5.0f, 5.0f, -25.0f,
                 0.0f},

                {0.5f*MAX_VAL_FP16, 2.0f, -0.5f*MAX_VAL_FP16,
                 0.5f*MAX_VAL_FP16},

                {MAX_VAL_FP16, 2.0f, -MAX_VAL_FP16,
                 MAX_VAL_FP16},

                {MAX_VAL_FP16, 2.0f, 1.0f,
                 InfinityF},

                {(1.0f + ulpOneFp16),
                 (1.0f + ulpOneFp16),
                 -1.0f - 2.0f*ulpOneFp16,
                 ulpOneFp16 * ulpOneFp16},

            };

            for (float[] testCase: testCases) {
                testFusedMacCase(testCase[0], testCase[1], testCase[2], testCase[3]);
            }
        }

        private static void testRounding() { // TOOD -- update test cases
            final float ulpOneFp16 = Bfloat16.ulp(valueOfExact(1.0f)).floatValue();

            float [][] testCases = {
//                 // The product is equal to
//                 // (MAX_VALUE + 1/2 * ulp(MAX_VALUE) + MAX_VALUE = (0x1.ffcp15 + 0x0.002p15)+ 0x1.ffcp15
//                 // so overflows.
//                 {0x1.3p1f, 0x1.afp15f, -MAX_VAL_FP16,
//                  InfinityF},

//                 // The product is equal to
//                 // (MAX_VALUE + 1/2 * ulp(MAX_VALUE) + MAX_VALUE = (0x1.ffcp15 + 0x0.002p15)+ 0x1.ffcp15
//                 // so overflows.
//                 {0x1.cp127, 0x1.24p0, -MAX_VAL_FP16,
//                  InfinityF}


                // Product exactly equals 0x1.ffp127, the overflow
                // threshold; subtracting a non-zero finite value will
                // result in MAX_VALUE, adding zero or a positive
                // value will overflow.
                {0x1.cp127f, 0x1.24p0f, -MIN_VAL_FP16,
                 MAX_VAL_FP16},

                {0x1.cp127f, 0x1.24p0f, -0.0f,
                 InfinityF},

                {0x1.cp127f, 0x1.24p0f, +0.0f,
                 InfinityF},

                {0x1.cp127f, 0x1.24p0f, +0x1.0p-14f,
                 InfinityF},

                {0x1.cp127f, 0x1.24p0f, InfinityF,
                 InfinityF},

//                 // PRECISION bits in the subnormal intermediate product
//                 {0x1.ffcp-14f, 0x1.0p-24f, 0x1.0p13f, // Can be held exactly
//                  0x1.0p13f},

//                 {0x1.ffcp-14f, 0x1.0p-24f, 0x1.0p14f, // *Cannot* be held exactly
//                  0x1.0p14f},

                // Check values where the exact result cannot be
                // exactly stored in a double.

                // Use Float16 MIN_VALUE
                {0x1.0p-24f, 0x1.0p-24f, 0x1.0p10f,
                 0x1.0p10f},

                // Use Bfloat16 MIN_VALUE
                {MIN_VAL_FP16, MIN_VAL_FP16, 0x1.0p10f,
                 0x1.0p10f},

                // Combine the min values
                {MIN_VAL_FP16, 0x1.0p-24f, 0x1.0p14f,
                 0x1.0p14f},

                 // Check subnormal results, underflow to zero
                 {MIN_VAL_FP16, -0.5f, MIN_VAL_FP16,
                  0.0f},

                 // Check subnormal results, underflow to zero
                 {MIN_VAL_FP16, -0.5f, 0.0f,
                  -0.0f},
            };

            for (float[] testCase: testCases) {
                testFusedMacCase(testCase[0], testCase[1], testCase[2], testCase[3]);
            }
        }

        private static void testFusedMacCase(float input1, float input2, float input3, float expected) {
            Bfloat16 a = valueOfExact(input1);
            Bfloat16 b = valueOfExact(input2);
            Bfloat16 c = valueOfExact(input3);
            Bfloat16 d = valueOfExact(expected);

            test("Bfloat16.fma(float)", a, b, c, Bfloat16.fma(a, b, c), d);

            // Permute first two inputs
            test("Bfloat16.fma(float)", b, a, c, Bfloat16.fma(b, a, c), d);
            return;
        }
    }

    private static void test(String testName,
                             Bfloat16 input1, Bfloat16 input2, Bfloat16 input3,
                             Bfloat16 result, Bfloat16 expected) {
        if (Bfloat16.compare(expected, result ) != 0) {
            System.err.println("Failure for "  + testName + ":\n" +
                               "\tFor inputs " + input1   + "\t(" + Bfloat16.toHexString(input1) + ") and "
                                               + input2   + "\t(" + Bfloat16.toHexString(input2) + ") and"
                                               + input3   + "\t(" + Bfloat16.toHexString(input3) + ")\n"  +
                               "\texpected  "  + expected + "\t(" + Bfloat16.toHexString(expected) + ")\n" +
                               "\tgot       "  + result   + "\t(" + Bfloat16.toHexString(result) + ").");
            throw new RuntimeException();
        }
    }
    /**
     * {@return a Float16 value converted from the {@code float}
     * argument throwing an {@code ArithmeticException} if the
     * conversion is inexact}.
     *
     * @param f the {@code float} value to convert exactly
     * @throws ArithmeticException
     */
    private static Bfloat16 valueOfExact(float f) {
        Bfloat16 f16 = Bfloat16.valueOf(f);
        if (Float.compare(f16.floatValue(), f) != 0) {
            throw new ArithmeticException("Inexact conversion to Bfloat16 of float value " + f);
        }
        return f16;
    }
}
