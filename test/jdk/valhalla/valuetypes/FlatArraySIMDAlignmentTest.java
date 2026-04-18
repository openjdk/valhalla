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
 * @test FlatArraySIMDAlignmentTest
 * @summary Validate SIMD-aligned flat arrays: arraycopy correctness, GC
 *          survival, JIT compilation, and element addressing across
 *          Float2 (64b), Float4 (128b), Float8 (256b) value classes.
 * @modules java.base/jdk.internal.value
 *          java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @run main/othervm FlatArraySIMDAlignmentTest
 * @run main/othervm -Xint FlatArraySIMDAlignmentTest
 * @run main/othervm -Xcomp FlatArraySIMDAlignmentTest
 */

import jdk.internal.value.ValueClass;
import jdk.internal.vm.annotation.LooselyConsistentValue;

public class FlatArraySIMDAlignmentTest {

    @LooselyConsistentValue
    static value class Float4 {
        float x, y, z, w;
        Float4(float x, float y, float z, float w) {
            this.x = x; this.y = y; this.z = z; this.w = w;
        }
        float sum() { return x + y + z + w; }
    }

    @LooselyConsistentValue
    static value class Float8 {
        float a, b, c, d, e, f, g, h;
        Float8(float a, float b, float c, float d,
               float e, float f, float g, float h) {
            this.a=a; this.b=b; this.c=c; this.d=d;
            this.e=e; this.f=f; this.g=g; this.h=h;
        }
        float sum() { return a+b+c+d+e+f+g+h; }
    }

    static final float EPS = 1e-4f;

    public static void main(String[] args) {
        testArrayCopyFloat4();
        testArrayCopyFloat8();
        testArrayCopyPartial();
        testArrayCopyOverlap();
        testGCSurvival();
        testJITHotLoop();
        testLargeArray();
        System.out.println("All FlatArraySIMDAlignmentTest tests passed.");
    }

    // --- Arraycopy correctness ---

    @SuppressWarnings("unchecked")
    static void testArrayCopyFloat4() {
        int n = 100;
        Float4[] src = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(
                Float4.class, n, new Float4(0,0,0,0));
        for (int i = 0; i < n; i++) {
            src[i] = new Float4(i, i*2, i*3, i*4);
        }
        Float4[] dst = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(
                Float4.class, n, new Float4(0,0,0,0));
        System.arraycopy(src, 0, dst, 0, n);
        for (int i = 0; i < n; i++) {
            assertClose(dst[i].sum(), i*10, "Float4 copy[" + i + "]");
        }
    }

    @SuppressWarnings("unchecked")
    static void testArrayCopyFloat8() {
        int n = 50;
        Float8[] src = (Float8[]) ValueClass.newNullRestrictedNonAtomicArray(
                Float8.class, n, new Float8(0,0,0,0,0,0,0,0));
        for (int i = 0; i < n; i++) {
            src[i] = new Float8(i,i,i,i,i,i,i,i);
        }
        Float8[] dst = (Float8[]) ValueClass.newNullRestrictedNonAtomicArray(
                Float8.class, n, new Float8(0,0,0,0,0,0,0,0));
        System.arraycopy(src, 0, dst, 0, n);
        for (int i = 0; i < n; i++) {
            assertClose(dst[i].sum(), i*8, "Float8 copy[" + i + "]");
        }
    }

    @SuppressWarnings("unchecked")
    static void testArrayCopyPartial() {
        int n = 20;
        Float4[] src = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(
                Float4.class, n, new Float4(0,0,0,0));
        for (int i = 0; i < n; i++) {
            src[i] = new Float4(i, 0, 0, 0);
        }
        Float4[] dst = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(
                Float4.class, n, new Float4(-1,-1,-1,-1));
        // Copy middle portion
        System.arraycopy(src, 5, dst, 5, 10);
        for (int i = 5; i < 15; i++) {
            assertClose(dst[i].x, i, "partial copy[" + i + "]");
        }
        // Verify untouched elements
        assertClose(dst[0].x, -1, "untouched[0]");
        assertClose(dst[19].x, -1, "untouched[19]");
    }

    @SuppressWarnings("unchecked")
    static void testArrayCopyOverlap() {
        int n = 20;
        Float4[] arr = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(
                Float4.class, n, new Float4(0,0,0,0));
        for (int i = 0; i < n; i++) {
            arr[i] = new Float4(i, 0, 0, 0);
        }
        // Overlapping forward copy
        System.arraycopy(arr, 0, arr, 5, 10);
        assertClose(arr[5].x, 0, "overlap fwd[5]");
        assertClose(arr[14].x, 9, "overlap fwd[14]");
        // Overlapping backward copy
        for (int i = 0; i < n; i++) {
            arr[i] = new Float4(i, 0, 0, 0);
        }
        System.arraycopy(arr, 5, arr, 0, 10);
        assertClose(arr[0].x, 5, "overlap bwd[0]");
        assertClose(arr[9].x, 14, "overlap bwd[9]");
    }

    // --- GC survival ---

    @SuppressWarnings("unchecked")
    static void testGCSurvival() {
        Float4[] arr = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(
                Float4.class, 1000, new Float4(0,0,0,0));
        for (int i = 0; i < 1000; i++) {
            arr[i] = new Float4(i, i, i, i);
        }
        // Force GC
        System.gc();
        System.gc();
        // Verify data survived
        float sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += arr[i].sum();
        }
        // sum = 4 * sum(0..999) = 4 * 499500 = 1998000
        assertClose(sum, 1998000, "GC survival sum");
    }

    // --- JIT hot loop ---

    @SuppressWarnings("unchecked")
    static void testJITHotLoop() {
        int n = 1024;
        Float4[] a = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(
                Float4.class, n, new Float4(0,0,0,0));
        Float4[] b = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(
                Float4.class, n, new Float4(0,0,0,0));
        for (int i = 0; i < n; i++) {
            a[i] = new Float4(1, 1, 1, 1);
            b[i] = new Float4(2, 2, 2, 2);
        }
        // Run enough iterations to trigger JIT
        float result = 0;
        for (int iter = 0; iter < 20000; iter++) {
            result = dotProduct(a, b);
        }
        // dot = sum(1*2 + 1*2 + 1*2 + 1*2) * 1024 = 8192
        assertClose(result, 8192, "JIT hot loop dot product");
    }

    static float dotProduct(Float4[] a, Float4[] b) {
        float sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i].x * b[i].x + a[i].y * b[i].y
                 + a[i].z * b[i].z + a[i].w * b[i].w;
        }
        return sum;
    }

    // --- Large array ---

    @SuppressWarnings("unchecked")
    static void testLargeArray() {
        int n = 100_000;
        Float4[] arr = (Float4[]) ValueClass.newNullRestrictedNonAtomicArray(
                Float4.class, n, new Float4(0,0,0,0));
        for (int i = 0; i < n; i++) {
            arr[i] = new Float4(1, 1, 1, 1);
        }
        float sum = 0;
        for (int i = 0; i < n; i++) {
            sum += arr[i].sum();
        }
        assertClose(sum, n * 4.0f, "large array sum");
    }

    static void assertClose(float actual, float expected, String msg) {
        if (Math.abs(actual - expected) > EPS) {
            throw new AssertionError(msg + ": expected " + expected + " got " + actual);
        }
    }
}
