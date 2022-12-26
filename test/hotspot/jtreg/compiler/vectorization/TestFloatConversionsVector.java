/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8294588
 * @summary Auto-vectorize Float.floatToFloat16, Float.float16ToFloat APIs
 * @requires vm.compiler2.enabled
 * @requires os.simpleArch == "x64"
 * @library /test/lib /
 * @run driver compiler.vectorization.TestFloatConversionsVector
 */

package compiler.vectorization;

import compiler.lib.ir_framework.*;

public class TestFloatConversionsVector {
  private static final int ARRLEN = 1024;
  private static final int ITERS  = 11000;
  private static float  [] finp;
  private static short  [] sout;
  private static short  [] sinp;
  private static float  [] fout;

  public static void main(String args[]) {
      TestFramework.runWithFlags("-XX:-TieredCompilation",
                                 "-XX:CompileThresholdScaling=0.3");
      System.out.println("PASSED");
  }

  @Test
  @IR(counts = {IRNode.VECTOR_CAST_F2HF, "> 0"}, applyIfCPUFeatureOr = {"avx512f", "true", "f16c", "true"})
  public void test_float_float16(short[] sout, float[] finp) {
      for (int i = 0; i < finp.length; i++) {
          sout[i] = Float.floatToFloat16(finp[i]);
      }
  }

  @Run(test = {"test_float_float16"}, mode = RunMode.STANDALONE)
  public void kernel_test_float_float16() {
      finp = new float[ARRLEN];
      sout = new short[ARRLEN];

      for (int i = 0; i < ARRLEN; i++) {
          finp[i] = (float) i * 1.4f;
      }

      for (int i = 0; i < ITERS; i++) {
         test_float_float16(sout, finp);
      }
  }

  @Test
  @IR(counts = {IRNode.VECTOR_CAST_HF2F, "> 0"}, applyIfCPUFeatureOr = {"avx512f", "true", "f16c", "true"})
  public void test_float16_float(float[] fout, short[] sinp) {
      for (int i = 0; i < sinp.length; i++) {
          fout[i] = Float.float16ToFloat(sinp[i]);
      }
  }

  @Run(test = {"test_float16_float"}, mode = RunMode.STANDALONE)
  public void kernel_test_float16_float() {
      sinp = new short[ARRLEN];
      fout = new float[ARRLEN];

      for (int i = 0; i < ARRLEN; i++) {
          sinp[i] = (short)i;
      }

      for (int i = 0; i < ITERS; i++) {
          test_float16_float(fout , sinp);
      }
  }
}
