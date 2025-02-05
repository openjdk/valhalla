/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2024, 2025, Arm Limited. All rights reserved.
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

/**
* @test
* @bug 8308363 8336406
* @summary Test vectorization of Float16 binary operations
* @requires vm.compiler2.enabled
* @library /test/lib /
* @compile TestFloat16VectorOps.java
* @run driver compiler.vectorization.TestFloat16VectorOps
*/

package compiler.vectorization;
import compiler.lib.ir_framework.*;
import java.util.Random;
import static java.lang.Float16.*;

public class TestFloat16VectorOps {
    private Float16[] input1;
    private Float16[] input2;
    private Float16[] input3;

    private int[] iin;
    private double[] din;
    private long[] lin;

    private Float16[] output;
    private int[] iout;
    private long[] lout;
    private double[] dout;
    private float[] fout;

    private static final int LEN = 2048;
    private Random rng;

    public static void main(String args[]) {
        TestFramework.runWithFlags("--enable-preview", "-XX:-TieredCompilation", "-Xbatch");
    }

    public TestFloat16VectorOps() {
        input1 = new Float16[LEN];
        input2 = new Float16[LEN];
        input3 = new Float16[LEN];

        output = new Float16[LEN];

        iin = new int[LEN];
        lin = new long[LEN];
        din = new double[LEN];

        iout = new int[LEN];
        lout = new long[LEN];
        dout = new double[LEN];
        fout = new float[LEN];

        rng = new Random(42);
        for (int i = 0; i < LEN; ++i) {
            input1[i] = shortBitsToFloat16(Float.floatToFloat16(rng.nextFloat()));
            input2[i] = shortBitsToFloat16(Float.floatToFloat16(rng.nextFloat()));
            input3[i] = shortBitsToFloat16(Float.floatToFloat16(rng.nextFloat()));

            iin[i] = input1[i].intValue();
            din[i] = input1[i].doubleValue();
            lin[i] = input1[i].longValue();
        }
    }

    @Test
    @Warmup(10000)
    @IR(counts = {IRNode.ADD_VHF, ">= 1"},
        applyIfCPUFeatureOr = {"avx512_fp16", "true", "sve", "true"})
    @IR(counts = {IRNode.ADD_VHF, ">= 1"},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
    public void vectorAddFloat16() {
        for (int i = 0; i < LEN; ++i) {
            output[i] = Float16.add(input1[i], input2[i]);
        }
    }

    @Check(test="vectorAddFloat16")
    public void checkResultAdd() {
        for (int i = 0; i < LEN; ++i) {
            Float16 expected = Float16.add(input1[i], input2[i]);
            if (float16ToRawShortBits(output[i]) != float16ToRawShortBits(expected)) {
                throw new RuntimeException("Invalid result for add operation : output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
            }
        }
    }

    @Test
    @Warmup(10000)
    @IR(counts = {IRNode.SUB_VHF, ">= 1"},
        applyIfCPUFeatureOr = {"avx512_fp16", "true", "sve", "true"})
    @IR(counts = {IRNode.SUB_VHF, ">= 1"},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
    public void vectorSubFloat16() {
        for (int i = 0; i < LEN; ++i) {
            output[i] = Float16.subtract(input1[i], input2[i]);
        }
    }

    @Check(test="vectorSubFloat16")
    public void checkResultSub() {
        for (int i = 0; i < LEN; ++i) {
            Float16 expected = Float16.subtract(input1[i], input2[i]);
            if (float16ToRawShortBits(output[i]) != float16ToRawShortBits(expected)) {
                throw new RuntimeException("Invalid result for Float16 sub operation : output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
            }
        }
    }

    @Test
    @Warmup(10000)
    @IR(counts = {IRNode.MUL_VHF, ">= 1"},
        applyIfCPUFeatureOr = {"avx512_fp16", "true", "sve", "true"})
    @IR(counts = {IRNode.MUL_VHF, ">= 1"},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
    public void vectorMulFloat16() {
        for (int i = 0; i < LEN; ++i) {
            output[i] = Float16.multiply(input1[i], input2[i]);
        }
    }

    @Check(test="vectorMulFloat16")
    public void checkResultMul() {
        for (int i = 0; i < LEN; ++i) {
            Float16 expected = Float16.multiply(input1[i], input2[i]);
            if (float16ToRawShortBits(output[i]) != float16ToRawShortBits(expected)) {
                throw new RuntimeException("Invalid result for Float16 mul operation : output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
            }
        }
    }

    @Test
    @Warmup(10000)
    @IR(counts = {IRNode.DIV_VHF, ">= 1"},
        applyIfCPUFeatureOr = {"avx512_fp16", "true", "sve", "true"})
    @IR(counts = {IRNode.DIV_VHF, ">= 1"},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
    public void vectorDivFloat16() {
        for (int i = 0; i < LEN; ++i) {
            output[i] = Float16.divide(input1[i], input2[i]);
        }
    }

    @Check(test="vectorDivFloat16")
    public void checkResultDiv() {
        for (int i = 0; i < LEN; ++i) {
            Float16 expected = Float16.divide(input1[i], input2[i]);
            if (float16ToRawShortBits(output[i]) != float16ToRawShortBits(expected)) {
                throw new RuntimeException("Invalid result for Float16 divide operation : output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
            }
        }
    }

    @Test
    @Warmup(10000)
    @IR(counts = {IRNode.MIN_VHF, ">= 1"},
        applyIfCPUFeatureOr = {"avx512_fp16", "true", "sve", "true"})
    @IR(counts = {IRNode.MIN_VHF, ">= 1"},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
    public void vectorMinFloat16() {
        for (int i = 0; i < LEN; ++i) {
            output[i] = Float16.min(input1[i], input2[i]);
        }
    }

    @Check(test="vectorMinFloat16")
    public void checkResultMin() {
        for (int i = 0; i < LEN; ++i) {
            Float16 expected = Float16.min(input1[i], input2[i]);
            if (float16ToRawShortBits(output[i]) != float16ToRawShortBits(expected)) {
                throw new RuntimeException("Invalid result for Float16 min operation : output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
            }
        }
    }

    @Test
    @Warmup(10000)
    @IR(counts = {IRNode.MAX_VHF, ">= 1"},
        applyIfCPUFeatureOr = {"avx512_fp16", "true", "sve", "true"})
    @IR(counts = {IRNode.MAX_VHF, ">= 1"},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
    public void vectorMaxFloat16() {
        for (int i = 0; i < LEN; ++i) {
            output[i] = Float16.max(input1[i], input2[i]);
        }
    }

    @Check(test="vectorMaxFloat16")
    public void checkResultMax() {
        for (int i = 0; i < LEN; ++i) {
            Float16 expected = Float16.max(input1[i], input2[i]);
            if (float16ToRawShortBits(output[i]) != float16ToRawShortBits(expected)) {
                throw new RuntimeException("Invalid result for Float16 max operation : output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
            }
        }
    }

    @Test
    @Warmup(10000)
    @IR(counts = {IRNode.ABS_VHF, ">= 1"},
        applyIfCPUFeatureOr = {"sve", "true"})
    @IR(counts = {IRNode.ABS_VHF, ">= 1"},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
    public void vectorAbsFloat16() {
        for (int i = 0; i < LEN; ++i) {
            output[i] = Float16.abs(input1[i]);
        }
    }

    @Check(test="vectorAbsFloat16")
    public void checkResultAbs() {
        for (int i = 0; i < LEN; ++i) {
            Float16 expected = Float16.abs(input1[i]);
            if (float16ToRawShortBits(output[i]) != float16ToRawShortBits(expected)) {
                throw new RuntimeException("Invalid result for Float16 Abs operation : output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
            }
        }
    }

    @Test
    @Warmup(10000)
    @IR(counts = {IRNode.NEG_VHF, ">= 1"},
        applyIfCPUFeatureOr = {"sve", "true"})
    @IR(counts = {IRNode.NEG_VHF, ">= 1"},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
    public void vectorNegFloat16() {
        for (int i = 0; i < LEN; ++i) {
            output[i] = Float16.negate(input1[i]);
        }
    }

    @Check(test="vectorNegFloat16")
    public void checkResultNeg() {
        for (int i = 0; i < LEN; ++i) {
            Float16 expected = Float16.negate(input1[i]);
            if (float16ToRawShortBits(output[i]) != float16ToRawShortBits(expected)) {
                throw new RuntimeException("Invalid result for Float16 Negate operation : output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
            }
        }
    }

    @Test
    @Warmup(10000)
    @IR(counts = {IRNode.SQRT_VHF, ">= 1"},
        applyIfCPUFeatureOr = {"avx512_fp16", "true", "sve", "true"})
    @IR(counts = {IRNode.SQRT_VHF, ">= 1"},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
    public void vectorSqrtFloat16() {
        for (int i = 0; i < LEN; ++i) {
            output[i] = Float16.sqrt(input1[i]);
        }
    }

    @Check(test="vectorSqrtFloat16")
    public void checkResultSqrt() {
        for (int i = 0; i < LEN; ++i) {
            Float16 expected = Float16.sqrt(input1[i]);
            if (float16ToRawShortBits(output[i]) != float16ToRawShortBits(expected)) {
                throw new RuntimeException("Invalid result for Float16 sqrt operation : output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
            }
        }
    }

    @Test
    @Warmup(10000)
    @IR(counts = {IRNode.FMA_VHF, ">= 1"},
        applyIfCPUFeatureOr = {"avx512_fp16", "true", "sve", "true"})
    @IR(counts = {IRNode.FMA_VHF, ">= 1"},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
    public void vectorFmaFloat16() {
        for (int i = 0; i < LEN; ++i) {
            output[i] = Float16.fma(input1[i], input2[i], input3[i]);
        }
    }

    @Check(test="vectorFmaFloat16")
    public void checkResultFma() {
        for (int i = 0; i < LEN; ++i) {
            Float16 expected = Float16.fma(input1[i], input2[i], input3[i]);
            if (float16ToRawShortBits(output[i]) != float16ToRawShortBits(expected)) {
                throw new RuntimeException("Invalid result for Float16 FMA operation : output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
            }
        }
    }

    @Test
    @Warmup(10000)
    @IR(counts = {IRNode.VECTOR_CAST_HF2I, ">= 1"},
        applyIfCPUFeature = {"sve", "true"})
    @IR(counts = {IRNode.VECTOR_CAST_HF2I, ">= 1"},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
    public void vectorFloat16ToInt() {
        for (int i = 0; i < LEN; ++i) {
            iout[i] = input1[i].intValue();
        }
    }

    @Check(test="vectorFloat16ToInt")
    public void checkResultFloat16ToInt() {
        int expected;
        for (int i = 0; i < LEN; ++i) {
            expected = input1[i].intValue();
            if (expected != iout[i]) {
                throw new RuntimeException("Invalid result for Float16 to int conversion : iout[" + i + "] = " + iout[i] + " != " + expected);
            }
        }
    }

    @Test
    @Warmup(10000)
    @IR(counts = {IRNode.VECTOR_CAST_HF2L, ">= 1"},
        applyIf = {"MaxVectorSize", "> 16"},
        applyIfCPUFeature = {"sve", "true"})
    public void vectorFloat16ToLong() {
        for (int i = 0; i < LEN; ++i) {
            lout[i] = input1[i].longValue();
        }
    }

    @Check(test="vectorFloat16ToLong")
    public void checkResultFloat16ToLong() {
        long expected;
        for (int i = 0; i < LEN; ++i) {
            expected = input1[i].longValue();
            if (expected != lout[i]) {
                throw new RuntimeException("Invalid result for Float16 to long conversion : lout[" + i + "] = " + lout[i] + " != " + expected);
            }
        }
    }

    @Test
    @Warmup(10000)
    @IR(counts = {IRNode.VECTOR_CAST_HF2D, ">= 1"},
        applyIf = {"MaxVectorSize", "> 16"},
        applyIfCPUFeature = {"sve", "true"})
    public void vectorFloat16ToDouble() {
        for (int i = 0; i < LEN; ++i) {
            dout[i] = input1[i].doubleValue();
        }
    }

    @Check(test="vectorFloat16ToDouble")
    public void checkResultFloat16ToDouble() {
        double expected;
        for (int i = 0; i < LEN; ++i) {
            expected = input1[i].doubleValue();
            if (expected != dout[i]) {
                throw new RuntimeException("Invalid result for Float16 to double conversion : dout[" + i + "] = " + dout[i] + " != " + expected);
            }
        }
    }

    @Test
    @Warmup(10000)
    @IR(counts = {IRNode.VECTOR_CAST_I2HF, ">= 1"},
        applyIfCPUFeature = {"sve", "true"})
    @IR(counts = {IRNode.VECTOR_CAST_I2HF, ">= 1"},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
    public void vectorIntToFloat16() {
        for (int i = 0; i < LEN; ++i) {
            output[i] = valueOf(iin[i]);
        }
    }
    @Check(test="vectorIntToFloat16")
    public void checkResulIntToFloat16() {
        Float16 expected;
        for (int i = 0; i < LEN; ++i) {
            expected = valueOf(iin[i]);
            if (float16ToRawShortBits(expected) != float16ToRawShortBits(output[i])) {
                throw new RuntimeException("Invalid result for int to Float16 conversion : output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
            }
        }
    }

    @Test
    @Warmup(10000)
    @IR(counts = {IRNode.VECTOR_CAST_D2HF, ">= 1"},
        applyIf = {"MaxVectorSize", "> 16"},
        applyIfCPUFeature = {"sve", "true"})
        public void vectorDoubleToFloat16() {
        for (int i = 0; i < LEN; ++i) {
            output[i] = valueOf(din[i]);
        }
    }
    @Check(test="vectorDoubleToFloat16")
    public void checkResulDoubleToFloat16() {
        Float16 expected;
        for (int i = 0; i < LEN; ++i) {
            expected = valueOf(din[i]);
            if (float16ToRawShortBits(expected) != float16ToRawShortBits(output[i])) {
                throw new RuntimeException("Invalid result for double to Float16 conversion : output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
            }
        }
    }

    @Test
    @Warmup(10000)
    @IR(counts = {IRNode.VECTOR_CAST_L2HF, ">= 1"},
        applyIf = {"MaxVectorSize", "> 16"},
        applyIfCPUFeature = {"sve", "true"})
    public void vectorLongToFloat16() {
        for (int i = 0; i < LEN; ++i) {
            output[i] = valueOf(lin[i]);
        }
    }
    @Check(test="vectorLongToFloat16")
    public void checkResulLongToFloat16() {
        Float16 expected;
        for (int i = 0; i < LEN; ++i) {
            expected = valueOf(lin[i]);
            if (float16ToRawShortBits(expected) != float16ToRawShortBits(output[i])) {
                throw new RuntimeException("Invalid result for long to Float16 conversion : output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
            }
        }
    }
}
