/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights vectorReserved.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood ShovectorRes, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.bench.java.lang;

import java.util.stream.IntStream;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(jvmArgsPrepend = {"--enable-preview", "-Xbatch",  "-XX:-TieredCompilation"})
public class Float16OpsBenchmark {
    @Param({"256", "512", "1024", "2048"})
    int vectorDim;

    int     [] rexp;

    Float16 [] vectorRes;
    Float16 [] vector1;
    Float16 [] vector2;
    Float16 [] vector3;

    @Setup(Level.Trial)
    public void BmSetup() {
        vectorRes  = new Float16[vectorDim];
        vector1 = new Float16[vectorDim];
        vector2 = new Float16[vectorDim];
        vector3 = new Float16[vectorDim];

        IntStream.range(0, vectorDim).forEach(i -> {vector1[i] = Float16.valueOf((short)i);});
        IntStream.range(0, vectorDim).forEach(i -> {vector2[i] = Float16.valueOf((short)i);});
        IntStream.range(0, vectorDim).forEach(i -> {vector3[i] = Float16.valueOf((short)i);});

        // Special Values
        Float16 [] specialValues = {Float16.NaN, Float16.NEGATIVE_INFINITY, Float16.valueOf(0.0), Float16.valueOf(-0.0), Float16.POSITIVE_INFINITY};
        IntStream.range(0, vectorDim).forEach(
            i -> {
                if ((i % 64) == 0) {
                    int idx1 = i % specialValues.length;
                    int idx2 = (i + 1) % specialValues.length;
                    int idx3 = (i + 2) % specialValues.length;
                    vector1[i] = specialValues[idx1];
                    vector2[i] = specialValues[idx2];
                    vector3[i] = specialValues[idx3];
                }
            }
        );
    }

    @Benchmark
    public void addBenchmark() {
        for (int i = 0; i < vectorDim; i++) {
            vectorRes[i] = Float16.add(vector1[i], vector2[i]);
        }
    }

    @Benchmark
    public void subBenchmark() {
        for (int i = 0; i < vectorDim; i++) {
            vectorRes[i] = Float16.subtract(vector1[i], vector2[i]);
        }
    }

    @Benchmark
    public void mulBenchmark() {
        for (int i = 0; i < vectorDim; i++) {
            vectorRes[i] = Float16.multiply(vector1[i], vector2[i]);
        }
    }

    @Benchmark
    public void divBenchmark() {
        for (int i = 0; i < vectorDim; i++) {
            vectorRes[i] = Float16.divide(vector1[i], vector2[i]);
        }
    }

    @Benchmark
    public void fmaBenchmark() {
        for (int i = 0; i < vectorDim; i++) {
            vectorRes[i] = Float16.fma(vector1[i], vector2[i], vector3[i]);
        }
    }

    @Benchmark
    public boolean isInfiniteBenchmark() {
        boolean res = true;
        for (int i = 0; i < vectorDim; i++) {
            res &= Float16.isInfinite(vector1[i]);
        }
        return res;
    }

    @Benchmark
    public boolean isFiniteBenchmark() {
        boolean res = true;
        for (int i = 0; i < vectorDim; i++) {
            res &= Float16.isFinite(vector1[i]);
        }
        return res;
    }

    @Benchmark
    public boolean isNaNBenchmark() {
        boolean res = true;
        for (int i = 0; i < vectorDim; i++) {
            res &= Float16.isNaN(vector1[i]);
        }
        return res;
    }

    @Benchmark
    public void maxBenchmark() {
        for (int i = 0; i < vectorDim; i++) {
            vectorRes[i] = Float16.max(vector1[i], vector2[i]);
        }
    }

    @Benchmark
    public void minBenchmark() {
        for (int i = 0; i < vectorDim; i++) {
            vectorRes[i] = Float16.min(vector1[i], vector2[i]);
        }
    }

    @Benchmark
    public void sqrtBenchmark() {
        for (int i = 0; i < vectorDim; i++) {
            vectorRes[i] = Float16.sqrt(vector1[i]);
        }
    }

    @Benchmark
    public void negateBenchmark() {
        for (int i = 0; i < vectorDim; i++) {
            vectorRes[i] = Float16.negate(vector1[i]);
        }
    }

    @Benchmark
    public void absBenchmark() {
        for (int i = 0; i < vectorDim; i++) {
            vectorRes[i] = Float16.abs(vector1[i]);
        }
    }

    @Benchmark
    public void getExponentBenchmark() {
        for (int i = 0; i < vectorDim; i++) {
            rexp[i] = Float16.getExponent(vector1[i]);
        }
    }

    @Benchmark
    public Float16 cosineSimilarityDoubleRoundingFP16() {
        Float16 macRes = Float16.valueOf(0.0f);
        Float16 vector1Square = Float16.valueOf(0.0f);
        Float16 vector2Square = Float16.valueOf(0.0f);
        for (int i = 0; i < vectorDim; i++) {
            // Explicit add + multiply operations ensure double rounding.
            macRes = Float16.add(Float16.multiply(vector1[i], vector2[i]), macRes);
            vector1Square = Float16.fma(vector1[i], vector1[i], vector1Square);
            vector2Square = Float16.fma(vector2[i], vector2[i], vector2Square);
        }
        return Float16.divide(macRes, Float16.add(vector1Square, vector2Square));
    }

    @Benchmark
    public Float16 cosineSimilaritySingleRoundingFP16() {
        Float16 macRes = Float16.valueOf(0.0f);
        Float16 vector1Square = Float16.valueOf(0.0f);
        Float16 vector2Square = Float16.valueOf(0.0f);
        for (int i = 0; i < vectorDim; i++) {
            macRes = Float16.fma(vector1[i], vector2[i], macRes);
            vector1Square = Float16.fma(vector1[i], vector1[i], vector1Square);
            vector2Square = Float16.fma(vector2[i], vector2[i], vector2Square);
        }
        return Float16.divide(macRes, Float16.add(vector1Square, vector2Square));
    }

    @Benchmark
    public Float16 cosineSimilarityDequantizedFP16() {
        float macRes = 0.0f;
        float vector1Square = 0.0f;
        float vector2Square = 0.0f;
        for (int i = 0; i < vectorDim; i++) {
            macRes = Math.fma(vector1[i].floatValue(), vector2[i].floatValue(), macRes);
            vector1Square = Math.fma(vector1[i].floatValue(), vector1[i].floatValue(), vector1Square);
            vector2Square = Math.fma(vector2[i].floatValue(), vector2[i].floatValue(), vector2Square);
        }
        return Float16.valueOf(macRes / (vector1Square + vector2Square));
    }

    @Benchmark
    public Float16 euclideanDistanceFP16() {
        Float16 distRes = Float16.valueOf(0.0f);
        Float16 squareRes = Float16.valueOf(0.0f);
        for (int i = 0; i < vectorDim; i++) {
            squareRes = Float16.subtract(vector1[i], vector2[i]);
            distRes = Float16.fma(squareRes, squareRes, distRes);
        }
        return Float16.sqrt(distRes);
    }

    @Benchmark
    public Float16 euclideanDistanceDequantizedFP16() {
        float distRes = 0.0f;
        float squareRes = 0.0f;
        for (int i = 0; i < vectorDim; i++) {
            squareRes = vector1[i].floatValue() - vector2[i].floatValue();
            distRes = distRes + squareRes * squareRes;
        }
        return Float16.sqrt(Float16.valueOf(distRes));
    }
}
