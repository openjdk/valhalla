/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
* @bug     8308363
* @summary Validate compiler IR for FP16 scalar operations.
* @requires vm.compiler2.enabled
* @library /test/lib /
* @enablePreview
* @run driver compiler.vectorization.TestFP16ScalarAdd
*/

package compiler.vectorization;
import compiler.lib.ir_framework.*;
import java.util.Random;
import static java.lang.Float16.*;

public class TestFP16ScalarAdd {
    private static final int count = 1024;

    private float[] flin;
    private float[] flout;
    private Float16[] fin;
    private Float16[] fout;
    private short[] src;
    private short[] dst;
    private short res;

    private Random rng;

    public static void main(String args[]) {
        TestFramework.runWithFlags("--enable-preview");
    }

    public TestFP16ScalarAdd() {
        flin = new float[count];
        flout = new float[count];
        fin = new Float16[count];
        fout = new Float16[count];
        src = new short[count];
        dst = new short[count];
        rng = new Random(0);

        for (int i = 0; i < count; i++) {
            flin[i] = rng.nextFloat();
            fin[i] = Float16.valueOf(Float.floatToFloat16(rng.nextFloat()));
            src[i] = Float.floatToFloat16(i);
        }
    }

    @Test
    @IR(applyIfCPUFeature = {"avx512_fp16", "true"}, counts = {IRNode.ADD_HF, "> 0", IRNode.REINTERPRET_S2HF, "> 0", IRNode.REINTERPRET_HF2S, "> 0"})
    @IR(applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"}, counts = {IRNode.ADD_HF, "> 0", IRNode.REINTERPRET_S2HF, "> 0", IRNode.REINTERPRET_HF2S, "> 0"})
    public void test1() {
        Float16 res = shortBitsToFloat16((short)0);
        for (int i = 0; i < count; i++) {
            res = Float16.sum(res, shortBitsToFloat16(src[i]));
            dst[i] = float16ToRawShortBits(res);
        }
    }

    @Test
    @IR(applyIfCPUFeature = {"avx512_fp16", "true"}, failOn = {IRNode.ADD_HF, IRNode.REINTERPRET_S2HF, IRNode.REINTERPRET_HF2S})
    @IR(applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"}, failOn = {IRNode.ADD_HF, IRNode.REINTERPRET_S2HF, IRNode.REINTERPRET_HF2S})
    public void test2() {
        Float16 hf0 = shortBitsToFloat16((short)0);
        Float16 hf1 = shortBitsToFloat16((short)15360);
        Float16 hf2 = shortBitsToFloat16((short)16384);
        Float16 hf3 = shortBitsToFloat16((short)16896);
        Float16 hf4 = shortBitsToFloat16((short)17408);
        res = float16ToRawShortBits(Float16.sum(Float16.sum(Float16.sum(Float16.sum(hf0, hf1), hf2), hf3), hf4));
    }

    // Test for optimizing sequence - "dst (ReinterpretS2HF (ConvF2HF src)" in the backend to a single convert
    // operation and do away with redundant moves
    @Test
    @IR(applyIfCPUFeature = {"avx512_fp16" , "true"}, counts = {IRNode.CONVF2HFANDS2HF, " >= 1"})
    @IR(applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"}, counts = {IRNode.CONVF2HFANDS2HF, " >= 1"})
    public void test3() {
        for (int i = 0; i < count; ++i) {
            fout[i] = Float16.sum(Float16.valueOf(Float.floatToFloat16(flin[i])), Float16.valueOf(Float.floatToFloat16(flin[i])));
        }
        checkResultTest3();
    }

    public void checkResultTest3() {
        for (int i = 0; i < count; ++i) {
            Float16 expected = Float16.valueOf(Float.floatToFloat16(flin[i] + flin[i]));
            if (fout[i].float16ToRawShortBits() != expected.float16ToRawShortBits()) {
                throw new RuntimeException("Invalid result: fout[" + i + "] = " + fout[i].float16ToRawShortBits() + " != " + expected.float16ToRawShortBits());
            }
        }
    }

    // Test for optimizing sequence - "dst (ConvHF2F (ReinterpretHF2S src)" in the backend to a single convert
    // operation and do away with redundant moves
    @Test
    @IR(applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"}, counts = {IRNode.REINTERPRETHF2SANDHF2F, " >= 1"})
    public void test4() {
        for (int i = 0; i < count; ++i) {
            flout[i] = Float16.sum(fin[i], fin[i]).floatValue();
        }
        checkResultTest4();
    }

    public void checkResultTest4() {
        for (int i = 0; i < count; ++i) {
            float expected = fin[i].floatValue() + fin[i].floatValue();
            if (flout[i] != expected) {
                throw new RuntimeException("Invalid result: flout[" + i + "] = " + flout[i] + " != " + expected);
            }
        }
    }
}
