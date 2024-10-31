/*
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
package compiler.c2.irTests;

import compiler.lib.ir_framework.*;
import static java.lang.Float16.*;
import jdk.test.lib.Asserts;

/*
 * @test
 * @bug 8338061
 * @summary Test that Ideal transformations of ConvF2HF, ConvF2D, ConvF2I and ConvF2L
 * for generating FP16 nodes is performing as expected.
 * @library /test/lib /
 * @run driver compiler.c2.irTests.ConvF2XIdealizationTests
 */
public class ConvF2XIdealizationTests {
    private short[] sin;
    private short[] sout;
    private int[] iout;
    private long[] lout;
    private double[] dout;

    private static final int SIZE = 65504;
    public ConvF2XIdealizationTests() {
        sin  = new short[SIZE];
        sout = new short[SIZE];
        iout = new int[SIZE];
        dout = new double[SIZE];
        lout = new long[SIZE];

        for (int i = 0; i < SIZE; i++) {
            sin[i] = Float.floatToFloat16((float)i);
        }
    }
    public static void main(String[] args) {
        TestFramework.runWithFlags("--enable-preview", "-XX:-UseSuperWord");
    }

    @Test
    @IR(counts = {IRNode.SQRT_HF, ">=1", IRNode.REINTERPRET_S2HF, ">=1", IRNode.REINTERPRET_HF2S, ">=1"},
        failOn = {IRNode.SQRT_F, IRNode.CONV_HF2F, IRNode.CONV_F2HF},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
    // Test pattern - ConvHF2F -> ConvF2D -> SqrtD -> ConvD2F -> ConvF2HF is optimized to ReinterpretS2HF -> SqrtHF -> ReinterpretHF2S
    public void test1() {
        for (int i = 0; i < SIZE; i++) {
            sout[i] = Float.floatToFloat16((float)Math.sqrt(shortBitsToFloat16(sin[i]).floatValue()));
        }
    }

    @Check(test="test1")
    public void checkTest1() {
        for (int i = 0; i < SIZE; i++) {
            short expected = float16ToRawShortBits(sqrt(shortBitsToFloat16(sin[i])));
            if (expected != sout[i]) {
                throw new RuntimeException("Invalid result for test1 : sout[" + i + "] = " + sout[i] + " != " + expected);
            }
        }
    }

    @Test
    @IR(counts = {IRNode.CONV_HF2I, ">=1"},
        failOn = {IRNode.CONV_HF2F, IRNode.CONV_F2I},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
    // Test to verify if ConvHF2F -> ConvF2I is optimized to ConvHF2I
    public void test2() {
        for (int i = 0; i < SIZE; i++) {
            iout[i] = shortBitsToFloat16(sin[i]).intValue();
        }
    }

    @Check(test="test2")
    public void checkTest2() {
        int expected = 0;
        for (int i = 0; i < SIZE; i++) {
            expected = Float.valueOf(shortBitsToFloat16(sin[i]).floatValue()).intValue();
            if (expected != iout[i]) {
                throw new RuntimeException("Invalid result for test2 : iout[" + i + "] = " + iout[i] + " != " + expected);
            }
        }
    }

    @Test
    @IR(counts = {IRNode.CONV_HF2L, ">=1"},
        failOn = {IRNode.CONV_HF2F, IRNode.CONV_F2L},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
    // Test to verify if ConvHF2F -> ConvF2L is optimized to ConvHF2L
    public void test3() {
        for (int i = 0; i < SIZE; i++) {
            lout[i] = shortBitsToFloat16(sin[i]).longValue();
        }
    }

    @Check(test="test3")
    public void checkTest3() {
        long expected = 0;
        for (int i = 0; i < SIZE; i++) {
            expected = Float.valueOf(shortBitsToFloat16(sin[i]).floatValue()).longValue();
            if (expected != lout[i]) {
                throw new RuntimeException("Invalid result for test3 : lout[" + i + "] = " + lout[i] + " != " + expected);
            }
        }
    }

    @Test
    @IR(counts = {IRNode.CONV_HF2D, ">=1"},
        failOn = {IRNode.CONV_HF2F, IRNode.CONV_F2D},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
    // Test to verify if ConvHF2F -> ConvF2D is optimized to ConvHF2D
    public void test4() {
        for (int i = 0; i < SIZE; i++) {
            dout[i] = shortBitsToFloat16(sin[i]).doubleValue();
        }
    }

    @Check(test="test4")
    public void checkTest4() {
        double expected = 0;
        for (int i = 0; i < SIZE; i++) {
            expected = Float.valueOf(shortBitsToFloat16(sin[i]).floatValue()).doubleValue();
            if (expected != dout[i]) {
                throw new RuntimeException("Invalid result for test4 : dout[" + i + "] = " + dout[i] + " != " + expected);
            }
        }
    }
}
