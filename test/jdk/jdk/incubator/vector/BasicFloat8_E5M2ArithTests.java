/*
 * Copyright (c) 2016, 2026, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8380667
 * @modules jdk.incubator.vector
 * @summary Basic tests of Float8_E5M2 arithmetic and similar operations
 */

import jdk.incubator.vector.Float8_E5M2;
import jdk.incubator.vector.Float16; // TODO: remove later
import static jdk.incubator.vector.Float8_E5M2.*;
import java.util.HashSet;
import java.util.List;

public class BasicFloat8_E5M2ArithTests {
    private static float InfinityF = Float.POSITIVE_INFINITY;
    private static float NaNf = Float.NaN;

    private static final float MAX_VAL_FP8  = 0x1.cp+15f;
    private static final float MIN_NORM_FP8 = 0x1.0p-14f;
    private static final float MIN_VAL_FP8  = 0x1.0p-16f;

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
        checkOrderable();
        checkSqrt();
        checkGetExponent();
        checkUlp();
        checkValueOfDouble();
        checkValueOfLong();
        checkValueOfString();
        checkBaseConversionRoundTrip();
        FusedMultiplyAddTests.main();
    }

    /*
     * The software implementation of Float8_E5M2 delegates to float or
     * double operations for most of the actual computation. This
     * regression test takes that into account as it generally only
     * has limited testing to probe whether or not the proper
     * functionality is being delegated to.
     *
     * To make the test easier to read, float literals that are exact
     * upon conversion to Float8_E5M2 are used for the test data.
     */

     /**
      * Verify handling of NaN representations
      */
     private static void checkBitWise() {
         byte nanImage = float8ToRawByteBits(Float8_E5M2.NaN);

         int exponent = 0x7c;
         int sign =     0x80;

         // All-zeros significand with a max exponent are infinite
         // values, not NaN values.
         for(int i = 0x1; i <= 0x03; i++) {
             byte posNaNasShort = (byte)(       exponent | i);
             byte negNaNasShort = (byte)(sign | exponent | i);

             Float8_E5M2 posf16 = byteBitsToFloat8(posNaNasShort);
             Float8_E5M2 negf16 = byteBitsToFloat8(negNaNasShort);

             // Mask-off high-order 24 bits to avoid sign extension woes
             checkInt(nanImage & 0xff, float8ToByteBits(posf16) & 0xff, "positive NaN");
             checkInt(nanImage & 0xff, float8ToByteBits(negf16) & 0xff, "negative NaN");

             checkInt(posNaNasShort & 0xff, float8ToRawByteBits(posf16) & 0xff, "positive NaN");
             checkInt(negNaNasShort & 0xff, float8ToRawByteBits(negf16) & 0xff, "negative NaN");
         }
     }

     /**
      * Verify correct number of hashValue's from all Float8 values.
      */
     private static void checkHash() {
         // Slightly over-allocate the HashSet.
         HashSet<Integer> set = HashSet.newHashSet(Byte.MAX_VALUE - Byte.MIN_VALUE + 1);

         // Each non-NaN value should have a distinct hashCode. All NaN
         // values should share a single hashCode. Check the latter
         // property by verifying the overall count of entries in the
         // set.
         for(int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
             Float8_E5M2 f8 = Float8_E5M2.byteBitsToFloat8((byte)i);
             boolean addedToSet = set.add(f8.hashCode());

             if (!Float8_E5M2.isNaN(f8)) {
                 if (!addedToSet) {
                     throwRE("Existing hash value for " + f8);
                 }
             }
         }

         // There are 2^8 = 256 total byte values. Each of these
         // bit patterns is a valid representation of a Float8_E5M2
         // value. However, NaNs have multiple possible encodings.
         // With an exponent = 0x7c, each nonzero significand 0x1 to
         // 0x3 is a NaN, for both positive and negative sign bits.
         //
         // Therefore, the total number of distinct hash codes for
         // Float8_E5M2 values should be:
         // 256 - 2*(3) + 1 = 253

         int setSize = set.size();
         if (setSize != 251) {
             throwRE("Unexpected number of distinct hash values " + setSize);
         }
     }

    private static void checkConstants() {
        checkInt(BYTES,          1, "Float8.BYTES");
        checkInt(MAX_EXPONENT,  15, "Float8.MAX_EXPONENT");
        checkInt(MIN_EXPONENT, -14, "Float8.MIN_EXPONENT");
        checkInt(PRECISION,      3, "Float8.PRECISION");
        checkInt(SIZE,           8, "Float8.SIZE");

        checkFloat8(Float8_E5M2.valueOf(0),  0.0f, "0.0f");
        checkFloat8(Float8_E5M2.valueOf(-0.0f),  -0.0f, "-0.0f");
        checkFloat8(MIN_VALUE,  0x1.0p-16f, "Float8.MIN_VALUE");
        checkFloat8(MIN_NORMAL, 0x1.0p-14f, "Float8.MIN_NORMAL");
        checkFloat8(MAX_VALUE,  57344.0f,  "Float8.MAX_VALUE");

        checkFloat8(POSITIVE_INFINITY,   InfinityF,  "+infinity");
        checkFloat8(NEGATIVE_INFINITY,  -InfinityF,  "-infinity");
        checkFloat8(NaN,                 NaNf,            "NaN");
    }

    private static void checkBoolean(Float8_E5M2 op1, Float8_E5M2 op2, boolean result,
                                     boolean expected, String operator) {
        if (result != expected) {
            throwRE(String.format("Didn't get expected value for " +
                                  "%s %s %s %nexpected %b, got %b%n",
                                  op1, operator, op2,
                                  expected, result));
        }
    }

    private static void checkInt(int value, int expected, String message) {
        if (value != expected) {
            throwRE(String.format("Didn't get expected value for %s;%nexpected %d, got %d",
                                  message, expected, value));
        }
    }

    private static void checkFloat8(Float8_E5M2 value8, float expected, String message) {
        float value = value8.floatValue();
        if (Float.compare(value, expected) != 0) {
            throwRE(String.format("Didn't get expected value for %s;%nexpected %g (%a), got %g (%a)",
                                  message, expected, expected, value, value));
        }
    }

    private static void checkFloat16(Float16 value16, float expected, String message) {
        throw new RuntimeException("Test needs to be ported");
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
            float arg            =  testCase[0];
            Float8_E5M2 argF8    =  valueOfExact(arg);
            float expected       =  testCase[1];
            Float8_E5M2 result   =  negate(valueOfExact(arg));
            Float8_E5M2 resultOp = -argF8;

            if (Float.compare(expected, result.floatValue()) != 0) {
                checkFloat8(result, expected, "negate(" + arg + ")");
            }

            if (Float.compare(expected, resultOp.floatValue()) != 0) {
                checkFloat8(result, expected, "negate(" + arg + ")");
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
            float arg          = testCase[0];
            float expected     = testCase[1];
            Float8_E5M2 result = abs(valueOfExact(arg));

            if (Float.compare(expected, result.floatValue()) != 0) {
                checkFloat8(result, expected, "abs(" + arg + ")");
            }
        }

        return;
    }

    private static void checkIsNaN() {
        if (!isNaN(NaN)) {
            throwRE("Float8_E5M2.isNaN() returns false for a NaN");
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
            boolean result = isNaN(valueOfExact(testCase));
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
            boolean result1 = isFinite(valueOfExact(infinity));
            boolean result2 = isInfinite(valueOfExact(infinity));

            if (result1) {
                throwRE("Float8_E5M2.isFinite returned true for " + infinity);
            }

            if (!result2) {
                throwRE("Float8_E5M2.isInfinite returned false for " + infinity);
            }
        }

        if (isFinite(NaN)) {
            throwRE("Float8_E5M2.isFinite() returns true for a NaN");
        }

        if (isInfinite(NaN)) {
            throwRE("Float8_E5M2.isInfinite() returns true for a NaN");
        }

        float[] finities = {
            -0.0f,
            +0.0f,
             1.0f,
            -1.0f,
        };

        for(var finity : finities) {
            boolean result1 = isFinite(valueOfExact(finity));
            boolean result2 = isInfinite(valueOfExact(finity));

            if (!result1) {
                throwRE("Float8_E5M2.isFinite returned true for " + finity);
            }

            if (result2) {
                throwRE("Float8_E5M2.isInfinite returned true for " + finity);
            }
        }

        return;
    }

    private static void checkMinMax() {
        float small = 1.0f;
        float large = 2.0f;

        if (min(valueOfExact(small), valueOfExact(large)).floatValue() != small) {
            throwRE(String.format("min(%g, %g) not equal to %g)",
                                  small, large, small));
        }

        if (max(valueOfExact(small), valueOfExact(large)).floatValue() != large) {
            throwRE(String.format("max(%g, %g) not equal to %g)",
                                  small, large, large));
        }
    }

    /*
     * Cursory checks to make sure correct operation is being called
     * with arguments in proper order for both two-argument methods
     * and binary operators of the Numerical interface.
     */
    private static void checkArith() {
        float   a   = 1.0f;
        Float8_E5M2 a8 = valueOfExact(a);

        float   b   = 2.0f;
        Float8_E5M2 b8 = valueOfExact(b);

        // Addition
        if (add(a8, b8).floatValue() != (a + b)) {
            throwRE("failure with " + a8 + " + " + b8);
        }
        if ((a8 + b8).floatValue() != (a + b)) { // check + operator
            throwRE("failure with " + a8 + " + " + b8);
        }

        if (add(b8, a8).floatValue() != (b + a)) {
            throwRE("failure with " + b8 + " + " + a8);
        }
        if ((b8 + a8).floatValue() != (b + a)) { // check + operator
            throwRE("failure with " + b8 + " + " + a8);
        }

        // Subtraction
        if (subtract(a8, b8).floatValue() != (a - b)) {
            throwRE("failure with " + a8 + " - " + b8);
        }
        if ((a8 - b8).floatValue() != (a - b)) { // check - operator
            throwRE("failure with " + a8 + " - " + b8);
        }

        if (subtract(b8, a8).floatValue() != (b - a)) {
            throwRE("failure with " + b8 + " - " + a8);
        }
        if ((b8 - a8).floatValue() != (b - a)) { // check - operator
            throwRE("failure with " + b8 + " - " + a8);
        }

        // Multiplication
        if (multiply(a8, b8).floatValue() != (a * b)) {
            throwRE("failure with " + a8 + " * " + b8);
        }
        if ((a8 * b8).floatValue() != (a * b)) { // check * operator
            throwRE("failure with " + a8 + " * " + b8);
        }

        if (multiply(b8, a8).floatValue() != (b * a)) {
            throwRE("failure with " + b8 + " * " + a8);
        }
        if ((b8 * a8).floatValue() != (b * a)) { // check * operator
            throwRE("failure with " + b8 + " * " + a8);
        }

        // Division
        if (divide(a8, b8).floatValue() != (a / b)) {
            throwRE("failure with " + a8 + " / " + b8);
        }
        if ((a8 / b8).floatValue() != (a / b)) { // check / operator
            throwRE("failure with " + a8 + " / " + b8);
        }

        if (divide(b8, a8).floatValue() != (b / a)) {
            throwRE("failure with " + b8 + " / " + a8);
        }
        if ((b8 / a8).floatValue() != (b / a)) { // check / operator
            throwRE("failure with " + b8 + " / " + a8);
        }

        return;
    }

    /*
     * Cursory checks to make sure the ordered comparison operators
     * are behaving as expected.
     */
    private static void checkOrderable() {
        float[] testCases = {NaNf,
                             -InfinityF,
                             -1.0f,
                             -0.0f,
                             +0.0f,
                             1.0f,
                             InfinityF};

        for (float op1_f : testCases) {
            for (float op2_f : testCases) {

                Float8_E5M2 op1_f16 = valueOfExact(op1_f);
                Float8_E5M2 op2_f16 = valueOfExact(op2_f);

                checkBoolean(op1_f16,  op2_f16,
                             op1_f16 < op2_f16,
                             op1_f   < op2_f,  "<");

                checkBoolean(op1_f16,   op2_f16,
                             op1_f16 <= op2_f16,
                             op1_f   <= op2_f, "<=");

                checkBoolean(op1_f16,   op2_f16,
                             op1_f16 >  op2_f16,
                             op1_f   >  op2_f, ">");

                checkBoolean(op1_f16,   op2_f16,
                             op1_f16 >= op2_f16,
                             op1_f   >= op2_f, ">=");
            }
        }
        return;
    }

    private static void checkSqrt() {
        float[][] testCases = {
            {-0.0f,   -0.0f},
            { 0.0f,    0.0f},

            {1.0f,   1.0f},
            {4.0f,   2.0f},
            {16.0f,  4.0f},

            { InfinityF, InfinityF},
            {-InfinityF, NaNf},

            {NaNf,       NaNf},
        };

        for(var testCase : testCases) {
            float arg =      testCase[0];
            float expected = testCase[1];
            Float8_E5M2 result =  sqrt(valueOfExact(arg));

            if (Float.compare(expected, result.floatValue()) != 0) {
                checkFloat8(result, expected, "sqrt(" + arg + ")");
            }
        }

        return;
    }

    private static void checkGetExponent() {
        float[][] testCases = {
            // Non-finite values
            { InfinityF, MAX_EXPONENT + 1},
            {-InfinityF, MAX_EXPONENT + 1},
            { NaNf,      MAX_EXPONENT + 1},

            // Subnormal and almost subnormal values
            {-0.0f,         MIN_EXPONENT - 1},
            {+0.0f,         MIN_EXPONENT - 1},
            { MIN_VAL_FP8,  MIN_EXPONENT - 1},
            {-MIN_VAL_FP8,  MIN_EXPONENT - 1},
            { MIN_NORM_FP8, MIN_EXPONENT},
            {-MIN_NORM_FP8, MIN_EXPONENT},

            // Normal values
            { 1.0f,       0},
            { 2.0f,       1},
            { 4.0f,       2},

            {MAX_VAL_FP8*0.5f, MAX_EXPONENT - 1},
            {MAX_VAL_FP8,      MAX_EXPONENT},
        };

        for(var testCase : testCases) {
            float arg =      testCase[0];
            float expected = testCase[1];
            float result =  (float)getExponent(valueOfExact(arg));

            if (Float.compare(expected, result) != 0) {
                checkFloat8(Float8_E5M2.valueOf(result), expected, "getExponent(" + arg + ")");
            }
        }
        return;
    }

    private static void checkUlp() {
        float MIN_VALUE_F = 0x1.0p-16f;
        float[][] testCases = {
            { InfinityF, InfinityF},
            {-InfinityF, InfinityF},
            { NaNf,      NaNf},

            // Zeros, subnormals, and MIN_VALUE all have MIN_VALUE as an ulp.
            {-0.0f,        MIN_VALUE_F},
            {+0.0f,        MIN_VALUE_F},
            { MIN_VALUE_F, MIN_VALUE_F},
            {-MIN_VALUE_F, MIN_VALUE_F},
            { 0x1.0p-14f,  MIN_VALUE_F},
            {-0x1.0p-14f,  MIN_VALUE_F},

             // ulp is (PRECISION - 1) = 2 bits away
             {0x1.0p0f,       0x1.0p-2f}, // 1.0f
             {0x1.0p1f,       0x1.0p-1f}, // 2.0f
             {0x1.0p2f,       0x1.0p0f},  // 4.0f

             {MAX_VAL_FP8*0.5f, 0x1.0p12f},
             {MAX_VAL_FP8,      0x1.0p13f},
        };

        for(var testCase : testCases) {
            float arg =      testCase[0];
            float expected = testCase[1];
            Float8_E5M2 result = ulp(valueOfExact(arg));

            if (Float.compare(expected, result.floatValue()) != 0) {
                checkFloat8(result, expected, "ulp(" + arg + ")");
            }
        }
        return;
    }

    private static void throwRE(String message) {
        throw new RuntimeException(message);
    }

     private static void checkValueOfDouble() {
         /*
          * Check that double -> Float8_E5M2 conversion rounds properly
          * around the midway point for each finite Float8_E5M2 value by
          * looping over the positive values and checking the negations
          * along the way.
          */

         String roundUpMsg   = "Didn't get half-way case rounding down";
         String roundDownMsg = "Didn't get half-way case rounding up";

         for(int i = 0; i <= Byte.MAX_VALUE; i++ ) {
             boolean isEven = ((i & 0x1) == 0);
             Float8_E5M2 f8 = Float8_E5M2.byteBitsToFloat8((byte)i);
             Float8_E5M2 f8Neg = negate(f8);

             if (!isFinite(f8))
                 continue;

             // System.out.println("\t" + toHexString(f8));

             Float8_E5M2 ulp = ulp(f8);
             double halfWay = f8.doubleValue() + ulp.doubleValue() * 0.5;

             // Under the round to nearest even rounding policy, the
             // half-way case should round down to the starting value
             // if the starting value is even; otherwise, it should round up.
             float roundedBack    = valueOf( halfWay).floatValue();
             float roundedBackNeg = valueOf(-halfWay).floatValue();

             if (isEven) {
                 checkFloat8(f8,    roundedBack,    roundDownMsg);
                 checkFloat8(f8Neg, roundedBackNeg, roundDownMsg);
             } else {
                 checkFloat8(add(f8,         ulp), roundedBack,    roundUpMsg);
                 checkFloat8(subtract(f8Neg, ulp), roundedBackNeg, roundUpMsg);
             }

             // Should always round down
             double halfWayNextDown = Math.nextDown(halfWay);
             checkFloat8(f8,    valueOf( halfWayNextDown).floatValue(), roundDownMsg);
             checkFloat8(f8Neg, valueOf(-halfWayNextDown).floatValue(), roundDownMsg);

             // Should always round up
             double halfWayNextUp =   Math.nextUp(halfWay);
             checkFloat8(add(f8, ulp),         valueOf( halfWayNextUp).floatValue(), roundUpMsg);
             checkFloat8(subtract(f8Neg, ulp), valueOf(-halfWayNextUp).floatValue(), roundUpMsg);
         }
     }

    private static void checkValueOfLong() {
        // The max representable value of this type is 57_344. An ulp
        // of this value is 8192. Therefore, the region (MAX_VALUE +/-
        // 0.5*ulp) should round to 57_344.

        checkFloat8(valueOf(-61_441),  Float.NEGATIVE_INFINITY, "-61_441");
        checkFloat8(valueOf(-61_440),  Float.NEGATIVE_INFINITY, "-61_440");
        checkFloat8(valueOf(-61_439), -MAX_VALUE.floatValue(),  "-61_439");
        checkFloat8(valueOf(-57_344), -MAX_VALUE.floatValue(),  "-57_344");
        checkFloat8(valueOf(-53_249), -MAX_VALUE.floatValue(),  "-53_249");
        checkFloat8(valueOf( 53_249),  MAX_VALUE.floatValue(),   "53_249");
        checkFloat8(valueOf( 57_344),  MAX_VALUE.floatValue(),   "57_344");
        checkFloat8(valueOf( 61_439),  MAX_VALUE.floatValue(),   "61_439");
        checkFloat8(valueOf( 61_440),  Float.POSITIVE_INFINITY,  "61_440");
        checkFloat8(valueOf( 61_441),  Float.POSITIVE_INFINITY,  "61_441");
    }

    private static void checkValueOfString() {
        String2Float8Case[] testCases = {
            new String2Float8Case( "NaN", NaNf),
            new String2Float8Case("+NaN", NaNf),
            new String2Float8Case("-NaN", NaNf),

            new String2Float8Case("+Infinity", +InfinityF),
            new String2Float8Case("-Infinity", -InfinityF),

            new String2Float8Case( "0.0",  0.0f),
            new String2Float8Case("+0.0",  0.0f),
            new String2Float8Case("-0.0", -0.0f),

            // Decimal signed integers are accepted as input; hex
            // signed integers are not, see negative test cases below.
            new String2Float8Case( "1",  1.0f),
            new String2Float8Case("-1", -1.0f),

            new String2Float8Case( "12",  12.0f),
            new String2Float8Case("-12", -12.0f),

//             new String2Float8Case( "123",  123.0f),
//             new String2Float8Case("-123", -123.0f),

            new String2Float8Case( "1.0",  1.0f),
            new String2Float8Case("-1.0", -1.0f),

            // Check for FloatTypeSuffix handling
            new String2Float8Case( "1.5f", 1.5f),
            new String2Float8Case( "1.5F", 1.5f),
            new String2Float8Case( "1.5D", 1.5f),
            new String2Float8Case( "1.5d", 1.5f),

            // TODO: tricking rounding cases needed to be updated for the particular format.
//             new String2Float8Case("65504.0", 65504.0f),  // Float16.MAX_VALUE

//             new String2Float8Case("65520.0", InfinityF), // Float16.MAX_VALUE + 0.5*ulp

//             new String2Float8Case("65520.01", InfinityF), // Float16.MAX_VALUE + > 0.5*ulp
//             new String2Float8Case("65520.001", InfinityF), // Float16.MAX_VALUE + > 0.5*ulp
//             new String2Float8Case("65520.0001", InfinityF), // Float16.MAX_VALUE + > 0.5*ulp
//             new String2Float8Case("65520.00000000001", InfinityF), // Float16.MAX_VALUE + > 0.5*ulp

//             new String2Float8Case("65519.99999999999", 65504.0f), // Float16.MAX_VALUE +  < 0.5*ulp
//             new String2Float8Case("0x1.ffdffffffffffp15", 65504.0f),
//             new String2Float8Case("0x1.ffdfffffffffp15", 65504.0f),


//             new String2Float8Case("65519.999999999999", 65504.0f),
//             new String2Float8Case("65519.9999999999999", 65504.0f),
//             new String2Float8Case("65519.99999999999999", 65504.0f),
//             new String2Float8Case("65519.999999999999999", 65504.0f),

//             // Float16.MAX_VALUE +  < 0.5*ulp
//             new String2Float8Case("65519.9999999999999999999999999999999999999", 65504.0f),

//             // Near MAX_VALUE - 0.5 ulp
//             new String2Float8Case("65488.0", 65472.0f),
//             new String2Float8Case("65487.9999", 65472.0f),
//             new String2Float8Case("65487.99999999", 65472.0f),
//             new String2Float8Case("65487.9999999999999999", 65472.0f),

//             new String2Float8Case("65488.000001", MAX_VAL_FP8), // FIXME

//             new String2Float8Case("65536.0", InfinityF), // Float16.MAX_VALUE + ulp

            // Hex values
            new String2Float8Case("0x1p2",   0x1.0p2f),
            new String2Float8Case("0x1p2f",  0x1.0p2f),
            new String2Float8Case("0x1p2d",  0x1.0p2f),
            new String2Float8Case("0x1.0p1", 0x1.0p1f),

            new String2Float8Case("-0x1p2",  -0x1.0p2f),
            // new String2Float8Case("0x3.45p12", 0x3.45p12f), // TOOD: value too wide for format

            // new String2Float8Case("0x3.4500000001p12", 0x3.45p12f),  // TOOD: value too wide for format

//             // Near half-way double + float cases in hex
//             new String2Float8Case("0x1.ffdfffffffffffffffffffffffffffffffffffp15", 65504.0f),

        };

        for(String2Float8Case testCase : testCases) {
            String input = testCase.input();
            float expected = testCase.expected();
            Float8_E5M2 result = Float8_E5M2.valueOf(input);
            checkFloat8(result, expected, "Float8_E5M2.valueOfExact(String) " + input);
        }

        List<String> negativeCases = List.of("0x1",
                                       "-0x1",
                                        "0x12",
                                       "-0x12");

        for(String negativeCase : negativeCases) {
            try {
                Float8_E5M2 f8 = Float8_E5M2.valueOf(negativeCase);
                throwRE("Did not get expected exception for input " + negativeCase);
            } catch (NumberFormatException nfe) {
                ; // Expected
            }
        }

        return;
    }

    private static record String2Float8Case(String input, float expected) {
    }

    private static void checkBaseConversionRoundTrip() {
        checkFloat8(Float8_E5M2.NaN,
                    Float8_E5M2.valueOf("NaN").floatValue(),
                    "base conversion of NaN");

        // For each non-NaN value, make sure
        // value -> string -> value
        // sequence of conversions gives the expected result.

        for(int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            Float8_E5M2 f8 = Float8_E5M2.byteBitsToFloat8((byte)i);
            if (Float8_E5M2.isNaN(f8))
                continue;

            checkFloat8(f8,
                        Float8_E5M2.valueOf(Float8_E5M2.toString(f8)).floatValue(),
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
             // testRounding();
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

                 {MAX_VAL_FP8, 2.0f,     -InfinityF,
                  -InfinityF},

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
             final float ulpOneFp8 = ulp(valueOfExact(1.0f)).floatValue();

             float [][] testCases = {
                 {1.0f, 2.0f, 3.0f,
                  5.0f},

                 {1.0f, 2.0f, -2.0f,
                  0.0f},

                 {2.0f, 2.0f, -4.0f,
                  0.0f},

                 {0.5f*MAX_VAL_FP8, 2.0f, -0.5f*MAX_VAL_FP8,
                  0.5f*MAX_VAL_FP8},

                 {MAX_VAL_FP8, 2.0f, -MAX_VAL_FP8,
                  MAX_VAL_FP8},

                 {MAX_VAL_FP8, 2.0f, 1.0f,
                  InfinityF},

                 {(1.0f + ulpOneFp8),
                  (1.0f + ulpOneFp8),
                  -1.0f - 2.0f*ulpOneFp8,
                  ulpOneFp8 * ulpOneFp8},

             };

             for (float[] testCase: testCases) {
                 testFusedMacCase(testCase[0], testCase[1], testCase[2], testCase[3]);
             }
         }

         private static void testRounding() {
             final float ulpOneFp8 = ulp(valueOfExact(1.0f)).floatValue();

             float [][] testCases = {
                 // The product is equal to
                 // (MAX_VALUE + 1/2 * ulp(MAX_VALUE) + MAX_VALUE = (0x1.ffcp15 + 0x0.002p15)+ 0x1.ffcp15
                 // so overflows.
                 {0x1.3p1f, 0x1.afp15f, -MAX_VAL_FP8, // FIXME
                  InfinityF},

                 // Product exactly equals 0x1.ffep15, the overflow
                 // threshold; subtracting a non-zero finite value will
                 // result in MAX_VALUE, adding zero or a positive
                 // value will overflow.
                 {0x1.2p10f, 0x1.c7p5f, -0x1.0p-14f,
                  MAX_VAL_FP8},

                 {0x1.2p10f, 0x1.c7p5f, -0.0f,
                  InfinityF},

                 {0x1.2p10f, 0x1.c7p5f, +0.0f,
                  InfinityF},

                 {0x1.2p10f, 0x1.c7p5f, +0x1.0p-14f,
                  InfinityF},

                 {0x1.2p10f, 0x1.c7p5f, InfinityF,
                  InfinityF},

                 // PRECISION bits in the subnormal intermediate product
                 {0x1.ffcp-14f, 0x1.0p-24f, 0x1.0p13f, // Can be held exactly
                  0x1.0p13f},

                 {0x1.ffcp-14f, 0x1.0p-24f, 0x1.0p14f, // *Cannot* be held exactly
                  0x1.0p14f},

                 // Arguments where using float fma or uniform float
                 // arithmetic gives the wrong result
                 {0x1.08p7f, 0x1.04p7f, 0x1.0p-24f,
                  0x1.0c4p14f},

                 // Check values where the exact result cannot be
                 // exactly stored in a double.
                 {0x1.0p-24f, 0x1.0p-24f, 0x1.0p10f,
                  0x1.0p10f},

                 {0x1.0p-24f, 0x1.0p-24f, 0x1.0p14f,
                  0x1.0p14f},

                 // Check subnormal results, underflow to zero
                 {0x1.0p-24f, -0.5f, 0x1.0p-24f,
                  0.0f},

                 // Check subnormal results, underflow to zero
                 {0x1.0p-24f, -0.5f, 0.0f,
                  -0.0f},
             };

             for (float[] testCase: testCases) {
                 testFusedMacCase(testCase[0], testCase[1], testCase[2], testCase[3]);
             }
         }

         private static void testFusedMacCase(float input1, float input2, float input3, float expected) {
             Float8_E5M2 a = valueOfExact(input1);
             Float8_E5M2 b = valueOfExact(input2);
             Float8_E5M2 c = valueOfExact(input3);
             Float8_E5M2 d = valueOfExact(expected);

             test("Float8_E5M2.fma(float)", a, b, c, Float8_E5M2.fma(a, b, c), d);

             // Permute first two inputs
             test("Float8_E5M2.fma(float)", b, a, c, Float8_E5M2.fma(b, a, c), d);
             return;
         }
     }

     private static void test(String testName,
                            Float8_E5M2 input1, Float8_E5M2 input2, Float8_E5M2 input3,
                            Float8_E5M2 result, Float8_E5M2 expected) {
         if (Float8_E5M2.compare(expected, result ) != 0) {
             System.err.println("Failure for "  + testName + ":\n" +
                                "\tFor inputs " + input1   + "\t(" + toHexString(input1) + ") and "
                                                + input2   + "\t(" + toHexString(input2) + ") and"
                                                + input3   + "\t(" + toHexString(input3) + ")\n"  +
                                "\texpected  "  + expected + "\t(" + toHexString(expected) + ")\n" +
                                "\tgot       "  + result   + "\t(" + toHexString(result) + ").");
             throw new RuntimeException();
         }
    }

    /**
     * {@return a Float8_E5M2 value converted from the {@code float}
     * argument throwing an {@code ArithmeticException} if the
     * conversion is inexact}.
     *
     * @param f the {@code float} value to convert exactly
     * @throws ArithmeticException
     */
    private static Float8_E5M2 valueOfExact(float f) {
        Float8_E5M2 f8 = valueOf(f);
        if (Float.compare(f8.floatValue(), f) != 0) {
            throw new ArithmeticException("Inexact conversion to Float8_E5M2 of float value " + f);
        }
        return f8;
    }
}
