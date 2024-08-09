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
 * @summary Test that Ideal transformations of ConvF2HF are being performed as expected.
 * @library /test/lib /
 * @run driver compiler.c2.irTests.ConvF2HFIdealizationTests
 */
public class ConvF2HFIdealizationTests {
    public static void main(String[] args) {
        TestFramework.runWithFlags("--enable-preview");
    }

    @Run(test = {"test1"})
    public void runMethod() {
        float f = RunInfo.getRandom().nextFloat();
        Float16 f16 = Float16.valueOf(f);
        assertResult(f16);
    }

    @DontCompile
    public void assertResult(Float16 fp16) {
        Asserts.assertEQ(Float.floatToFloat16((float)Math.sqrt((double)fp16.floatValue())), test1(fp16));
    }

    @Test
    @IR(counts = {IRNode.SQRT_HF, "1", IRNode.REINTERPRET_S2HF, "1", IRNode.REINTERPRET_HF2S, "1"},
        failOn = {IRNode.SQRT_F, IRNode.CONV_HF2F, IRNode.CONV_F2HF},
        applyIfCPUFeatureAnd = {"fphp", "true", "asimdhp", "true"})
    // Test pattern - ConvHF2F -> ConvF2D -> SqrtD -> ConvD2F -> ConvF2HF is optimized to ReinterpretS2HF -> SqrtHF -> ReinterpretHF2S
    public short test1(Float16 x) {
        return Float.floatToFloat16((float)Math.sqrt((double)x.floatValue()));
    }

}
