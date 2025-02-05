/*
 * Copyright (c) 2025, Arm Limited. All rights reserved.
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
package compiler.c2.irTests;

import compiler.lib.ir_framework.*;
import static java.lang.Float16.*;
import jdk.test.lib.Asserts;

/*
 * @test
 * @bug 8345053
 * @summary Test that Ideal and identity transformations of ConvD2HF are performing as expected.
 * @library /test/lib /
 * @run driver compiler.c2.irTests.ConvD2HFTransformationTests
 */
public class ConvD2HFTransformationTests {
    private int[] input;
    private Float16[] fin;
    private Float16[] fout;

    private static final int SIZE = 65504; // Tests full range of FP16 values
    public ConvD2HFTransformationTests() {
        input = new int[SIZE];
        fin  = new Float16[SIZE];
        fout = new Float16[SIZE];

        for (int i = 0; i < SIZE; i++) {
            input[i] = i;
            fin[i] = valueOf((float)i);
            fout[i] = valueOf(0);
        }
    }
    public static void main(String[] args) {
        TestFramework.runWithFlags("--enable-preview");
    }

    @Test
    @IR(counts = {IRNode.CONV_I2HF, ">=1"},
        failOn = {IRNode.CONV_D2HF, IRNode.CONV_I2D},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
        // Test Ideal transformation of ConvD2HF node : pattern ConvI2D -> ConvD2HF is optimized to ConvI2HF
        public void testIdeal() {
        for (int i = 0; i < SIZE; i++) {
            fout[i] = valueOf(input[i]);
        }
    }

    @Check(test="testIdeal")
    public void checkTestIdeal() {
        for (int i = 0; i < SIZE; i++) {
            Float16 expected = valueOf((float) input[i]);
            if (expected != fout[i]) {
                throw new RuntimeException("Invalid result for testIdeal : fout[" + i + "] = " + fout[i] + " != " + expected);
            }
        }
    }

    @Test
    @IR(failOn = {IRNode.CONV_D2HF, IRNode.CONV_HF2D},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
        // Test Identity transformation of ConvD2HF node : pattern - ConvHF2D -> ConvD2HF is optimized away
        public void testIdentity() {
        for (int i = 0; i < SIZE; i++) {
            fout[i] = valueOf(fin[i].doubleValue());
        }
    }

    @Check(test="testIdentity")
    public void checkTestIdentity() {
        for (int i = 0; i < SIZE; i++) {
            Float16 expected = fin[i];
            if (expected != fout[i]) {
                throw new RuntimeException("Invalid result for testIdeal : fout[" + i + "] = " + fout[i] + " != " + expected);
            }
        }
    }
}
