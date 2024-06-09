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

public class TestFP16ScalarAdd {
    private static final int count = 1024;

    private short[] src;
    private short[] dst;
    private short res;

    public static void main(String args[]) {
        TestFramework.runWithFlags("--enable-preview");
    }

    public TestFP16ScalarAdd() {
        src = new short[count];
        dst = new short[count];
        for (int i = 0; i < count; i++) {
            src[i] = Float.floatToFloat16(i);
        }
    }

    @Test
    @IR(applyIfCPUFeature = {"avx512_fp16", "true"}, counts = {IRNode.ADD_HF, "> 0", IRNode.REINTERPRET_S2HF, "> 0", IRNode.REINTERPRET_HF2S, "> 0"})
    public void test1() {
        Float16 res = Float16.valueOf((short)0);
        for (int i = 0; i < count; i++) {
            res = Float16.sum(res, Float16.valueOf(src[i]));
            dst[i] = res.float16ToRawShortBits();
        }
    }

    @Test
    @IR(applyIfCPUFeature = {"avx512_fp16", "true"}, failOn = {IRNode.ADD_HF, IRNode.REINTERPRET_S2HF, IRNode.REINTERPRET_HF2S})
    public void test2() {
        Float16 hf0 = Float16.valueOf((short)0);
        Float16 hf1 = Float16.valueOf((short)15360);
        Float16 hf2 = Float16.valueOf((short)16384);
        Float16 hf3 = Float16.valueOf((short)16896);
        Float16 hf4 = Float16.valueOf((short)17408);
        res = Float16.sum(Float16.sum(Float16.sum(Float16.sum(hf0, hf1), hf2), hf3), hf4).float16ToRawShortBits();
    }
}
