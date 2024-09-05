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

package com.arm.benchmarks;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

import static java.lang.Float16.*;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 3)
public class FP16Ops {

  @Param({"65504"})
  public static int SIZE;
  public static Float16[] fin;
  public static boolean[] b;

  @Setup(Level.Trial)
  public void BmSetup() {
      fin = new Float16[SIZE];
      b = new boolean[SIZE];
      for (int i = 0; i < SIZE; i++) {
          fin[i] = Float16.valueOf(Float.floatToFloat16((float) i));
      }
  }

  @Benchmark
  public void isFiniteHF() {
      for (int i = 0; i < SIZE; i++) {
          b[i] = Float16.isFinite(fin[i]);
      }
  }

  @Benchmark
  public void isNaNHF() {
      for (int i = 0; i < SIZE; i++) {
          b[i] = Float16.isNaN(fin[i]);
      }
  }

  @Benchmark
  public void isInfiniteHF() {
      for (int i = 0; i < SIZE; i++) {
          b[i] = Float16.isInfinite(fin[i]);
      }
  }
}
