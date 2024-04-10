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

/**
* @test
* @summary Test vectorization of Float16.sum operation.
* @requires vm.compiler2.enabled
* @library /test/lib /
* @compile -XDenablePrimitiveClasses TestFloat16VectorSum.java
* @run driver compiler.vectorization.TestFloat16VectorSum
*/

package compiler.vectorization;
import compiler.lib.ir_framework.*;
import java.util.Random;


public class TestFloat16VectorSum {
    private Float16[] input;
    private Float16[] output;
    private static final int LEN = 2048;
    private Random rng;

    public static void main(String args[]) {
        TestFramework.run(TestFloat16VectorSum.class);
    }

    public TestFloat16VectorSum() {
        input  = new Float16[LEN];
        output = new Float16[LEN];
        rng = new Random(42);
        for (int i = 0; i < LEN; ++i) {
            input[i] = Float16.valueOf(Float.floatToFloat16(rng.nextFloat()));
        }
    }

    @Test
    @Warmup(10000)
    @IR(applyIfCPUFeatureOr = {"avx512_fp16" , "true", "sve", "true"}, counts = {IRNode.ADD_VHF, " >= 1"})
    @IR(applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"}, counts = {IRNode.ADD_VHF, " >= 1"})
    public void vectorSumFloat16() {
        for (int i = 0; i < LEN; ++i) {
            output[i] = Float16.sum(input[i], input[i]);
        }
        checkResult();
    }

    public void checkResult() {
        for (int i = 0; i < LEN; ++i) {
            Float16 expected = Float16.sum(input[i], input[i]);
            if (output[i].float16ToRawShortBits() != expected.float16ToRawShortBits()) {
                throw new RuntimeException("Invalid result: output[" + i + "] = " + output[i].float16ToRawShortBits() + " != " + expected.float16ToRawShortBits());
            }
        }
    }
}
