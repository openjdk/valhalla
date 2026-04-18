/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test FlatArrayVectorizationTest
 * @summary Validate C2 superword vectorization of flat value class array
 *          operations for ML math patterns: element-wise add/mul/sub/scale,
 *          dot product, reduction, FMA, and mixed-width value classes.
 *          Tests correctness across interpreter, C2, and -Xcomp modes.
 * @modules java.base/jdk.internal.value
 *          java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @run main/othervm FlatArrayVectorizationTest
 * @run main/othervm -Xint FlatArrayVectorizationTest
 * @run main/othervm -Xcomp FlatArrayVectorizationTest
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:-VectorizeFlatArrays FlatArrayVectorizationTest
 */

import jdk.internal.value.ValueClass;
import jdk.internal.vm.annotation.LooselyConsistentValue;

public class FlatArrayVectorizationTest {

    @LooselyConsistentValue
    static value class Float2 {
        float x, y;
        Float2(float x, float y) { this.x = x; this.y = y; }
    }

    @LooselyConsistentValue
    static value class Float4 {
        float x, y, z, w;
        Float4(float x, float y, float z, float w) {
            this.x = x; this.y = y; this.z = z; this.w = w;
        }
    }

    @LooselyConsistentValue
    static value class Float8 {
        float a, b, c, d, e, f, g, h;
        Float8(float a, float b, float c, float d,
               float e, float f, float g, float h) {
            this.a = a; this.b = b; this.c = c; this.d = d;
            this.e = e; this.f = f; this.g = g; this.h = h;
        }
    }

    static final int N = 1024;
    static final float EPS = 0.01f;

    public static void main(String[] args) {
        // Run each test with enough iterations to trigger C2 compilation
        for (int warm = 0; warm < 5000; warm++) {
            testElementWiseAdd4();
            testElementWiseMul4();
            testElementWiseSub4();
            testScale4();
            testFMA4();
            testDotProduct4();
            testReduction4();
            testElementWiseAdd2();
            testElementWiseAdd8();
            testMixedOperations();
            testLargeArray();
        }
        // Final correctness check
        testElementWiseAdd4();
        testElementWiseMul4();
        testElementWiseSub4();
        testScale4();
        testFMA4();
        testDotProduct4();
        testReduction4();
        testElementWiseAdd2();
        testElementWiseAdd8();
        testMixedOperations();
        testLargeArray();
        System.out.println("All FlatArrayVectorizationTest tests passed.");
    }

    // --- Element-wise operations (tensor add/mul/sub) ---

    @SuppressWarnings("unchecked")
    static void testElementWiseAdd4() {
        Float4[] a = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        Float4[] b = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        Float4[] d = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        for (int i = 0; i < N; i++) {
            a[i] = new Float4(i, i * 2, i * 3, i * 4);
            b[i] = new Float4(1, 1, 1, 1);
        }
        for (int i = 0; i < N; i++) {
            d[i] = new Float4(a[i].x + b[i].x, a[i].y + b[i].y, a[i].z + b[i].z, a[i].w + b[i].w);
        }
        assertClose(d[N-1].x, N, "add4 x");
        assertClose(d[N-1].w, (N-1)*4+1, "add4 w");
    }

    @SuppressWarnings("unchecked")
    static void testElementWiseMul4() {
        Float4[] a = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        Float4[] b = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        Float4[] d = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        for (int i = 0; i < N; i++) {
            a[i] = new Float4(2, 3, 4, 5);
            b[i] = new Float4(10, 20, 30, 40);
        }
        for (int i = 0; i < N; i++) {
            d[i] = new Float4(a[i].x * b[i].x, a[i].y * b[i].y, a[i].z * b[i].z, a[i].w * b[i].w);
        }
        assertClose(d[0].x, 20, "mul4 x");
        assertClose(d[0].w, 200, "mul4 w");
    }

    @SuppressWarnings("unchecked")
    static void testElementWiseSub4() {
        Float4[] a = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        Float4[] b = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        Float4[] d = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        for (int i = 0; i < N; i++) {
            a[i] = new Float4(100, 200, 300, 400);
            b[i] = new Float4(i, i, i, i);
        }
        for (int i = 0; i < N; i++) {
            d[i] = new Float4(a[i].x - b[i].x, a[i].y - b[i].y, a[i].z - b[i].z, a[i].w - b[i].w);
        }
        assertClose(d[N-1].x, 100-(N-1), "sub4 x");
    }

    // --- Scale (multiply by scalar — used in attention, normalization) ---

    @SuppressWarnings("unchecked")
    static void testScale4() {
        Float4[] a = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        Float4[] d = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        float s = 0.5f;
        for (int i = 0; i < N; i++) { a[i] = new Float4(i, i*2, i*3, i*4); }
        for (int i = 0; i < N; i++) {
            d[i] = new Float4(a[i].x * s, a[i].y * s, a[i].z * s, a[i].w * s);
        }
        assertClose(d[100].x, 50, "scale4 x");
        assertClose(d[100].w, 200, "scale4 w");
    }

    // --- FMA (fused multiply-add — used in matmul accumulation) ---

    @SuppressWarnings("unchecked")
    static void testFMA4() {
        Float4[] a = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        Float4[] b = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        Float4[] c = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        Float4[] d = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        for (int i = 0; i < N; i++) {
            a[i] = new Float4(2, 2, 2, 2);
            b[i] = new Float4(3, 3, 3, 3);
            c[i] = new Float4(1, 1, 1, 1);
        }
        // d = a * b + c
        for (int i = 0; i < N; i++) {
            d[i] = new Float4(a[i].x*b[i].x+c[i].x, a[i].y*b[i].y+c[i].y,
                              a[i].z*b[i].z+c[i].z, a[i].w*b[i].w+c[i].w);
        }
        assertClose(d[0].x, 7, "fma4 x");
    }

    // --- Dot product (used in attention QK^T) ---

    @SuppressWarnings("unchecked")
    static void testDotProduct4() {
        Float4[] a = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        Float4[] b = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        for (int i = 0; i < N; i++) {
            a[i] = new Float4(1, 1, 1, 1);
            b[i] = new Float4(2, 2, 2, 2);
        }
        float sum = 0;
        for (int i = 0; i < N; i++) {
            sum += a[i].x*b[i].x + a[i].y*b[i].y + a[i].z*b[i].z + a[i].w*b[i].w;
        }
        assertClose(sum, N * 8, "dot4");
    }

    // --- Reduction (used in softmax denominator, norms) ---

    @SuppressWarnings("unchecked")
    static void testReduction4() {
        Float4[] a = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        for (int i = 0; i < N; i++) { a[i] = new Float4(1, 1, 1, 1); }
        float sum = 0;
        for (int i = 0; i < N; i++) {
            sum += a[i].x + a[i].y + a[i].z + a[i].w;
        }
        assertClose(sum, N * 4, "reduce4");
    }

    // --- Float2 (64-bit, sub-register width) ---

    @SuppressWarnings("unchecked")
    static void testElementWiseAdd2() {
        Float2[] a = (Float2[]) ValueClass.newNullRestrictedAtomicArray(Float2.class, N, new Float2(0,0));
        Float2[] b = (Float2[]) ValueClass.newNullRestrictedAtomicArray(Float2.class, N, new Float2(0,0));
        Float2[] d = (Float2[]) ValueClass.newNullRestrictedAtomicArray(Float2.class, N, new Float2(0,0));
        for (int i = 0; i < N; i++) { a[i] = new Float2(i, i*2); b[i] = new Float2(1, 1); }
        for (int i = 0; i < N; i++) {
            d[i] = new Float2(a[i].x + b[i].x, a[i].y + b[i].y);
        }
        assertClose(d[N-1].x, N, "add2 x");
    }

    // --- Float8 (256-bit, AVX2 width) ---

    @SuppressWarnings("unchecked")
    static void testElementWiseAdd8() {
        Float8[] a = (Float8[]) ValueClass.newNullRestrictedNonAtomicArray(Float8.class, N, new Float8(0,0,0,0,0,0,0,0));
        Float8[] b = (Float8[]) ValueClass.newNullRestrictedNonAtomicArray(Float8.class, N, new Float8(0,0,0,0,0,0,0,0));
        Float8[] d = (Float8[]) ValueClass.newNullRestrictedNonAtomicArray(Float8.class, N, new Float8(0,0,0,0,0,0,0,0));
        for (int i = 0; i < N; i++) {
            a[i] = new Float8(i,i,i,i,i,i,i,i);
            b[i] = new Float8(1,1,1,1,1,1,1,1);
        }
        for (int i = 0; i < N; i++) {
            d[i] = new Float8(a[i].a+b[i].a, a[i].b+b[i].b, a[i].c+b[i].c, a[i].d+b[i].d,
                              a[i].e+b[i].e, a[i].f+b[i].f, a[i].g+b[i].g, a[i].h+b[i].h);
        }
        assertClose(d[N-1].a, N, "add8 a");
        assertClose(d[N-1].h, N, "add8 h");
    }

    // --- Mixed operations (add then scale — residual + normalization) ---

    @SuppressWarnings("unchecked")
    static void testMixedOperations() {
        Float4[] a = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        Float4[] b = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        Float4[] d = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, N, new Float4(0,0,0,0));
        float s = 0.1f;
        for (int i = 0; i < N; i++) {
            a[i] = new Float4(10, 20, 30, 40);
            b[i] = new Float4(1, 2, 3, 4);
        }
        // d = (a + b) * s
        for (int i = 0; i < N; i++) {
            float rx = (a[i].x + b[i].x) * s;
            float ry = (a[i].y + b[i].y) * s;
            float rz = (a[i].z + b[i].z) * s;
            float rw = (a[i].w + b[i].w) * s;
            d[i] = new Float4(rx, ry, rz, rw);
        }
        assertClose(d[0].x, 1.1f, "mixed x");
        assertClose(d[0].w, 4.4f, "mixed w");
    }

    // --- Large array (stress test) ---

    @SuppressWarnings("unchecked")
    static void testLargeArray() {
        int n = 100_000;
        Float4[] a = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, n, new Float4(0,0,0,0));
        Float4[] b = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, n, new Float4(0,0,0,0));
        Float4[] d = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(Float4.class, n, new Float4(0,0,0,0));
        for (int i = 0; i < n; i++) { a[i] = new Float4(1,1,1,1); b[i] = new Float4(2,2,2,2); }
        for (int i = 0; i < n; i++) {
            d[i] = new Float4(a[i].x+b[i].x, a[i].y+b[i].y, a[i].z+b[i].z, a[i].w+b[i].w);
        }
        float sum = 0;
        for (int i = 0; i < n; i++) { sum += d[i].x + d[i].y + d[i].z + d[i].w; }
        assertClose(sum, n * 12.0f, "large");
    }

    static void assertClose(float actual, float expected, String msg) {
        if (Math.abs(actual - expected) > EPS) {
            throw new AssertionError(msg + ": expected " + expected + " got " + actual);
        }
    }
}
