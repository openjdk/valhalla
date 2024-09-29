/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2024, Arm Limited. All rights reserved.
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
    private Float16[] output;
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
        rng = new Random(42);
        for (int i = 0; i < LEN; ++i) {
            input1[i] = shortBitsToFloat16(Float.floatToFloat16(rng.nextFloat()));
            input2[i] = shortBitsToFloat16(Float.floatToFloat16(rng.nextFloat()));
            input3[i] = shortBitsToFloat16(Float.floatToFloat16(rng.nextFloat()));
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
                throw new RuntimeException("Invalid result: output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
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
                throw new RuntimeException("Invalid result: output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
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
                throw new RuntimeException("Invalid result: output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
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
                throw new RuntimeException("Invalid result: output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
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
                throw new RuntimeException("Invalid result: output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
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
                throw new RuntimeException("Invalid result: output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
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
                throw new RuntimeException("Invalid result: output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
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
                throw new RuntimeException("Invalid result: output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
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
                throw new RuntimeException("Invalid result: output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
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
                throw new RuntimeException("Invalid result: output[" + i + "] = " + float16ToRawShortBits(output[i]) + " != " + float16ToRawShortBits(expected));
            }
        }
    }
}
