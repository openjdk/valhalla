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
 * @bug 8239003
 * @summary C2 should respect larval state when scalarizing
 * @modules java.base/jdk.internal.misc
 * @library /testlibrary /test/lib
 * @compile -XDenablePrimitiveClasses TestMaskedUnsafePutInLoop.java
 * @run main/othervm/timeout=300 -XX:-TieredCompilation -Xbatch -XX:+EnablePrimitiveClasses --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED
 *                               -XX:+UnlockDiagnosticVMOptions compiler.valhalla.inlinetypes.TestMaskedUnsafePutInLoop
 */

package compiler.valhalla.inlinetypes;

import jdk.internal.misc.Unsafe;

public class TestMaskedUnsafePutInLoop {
     primitive class Pair {
         double d1, d2;
         public Pair(double d1, double d2) {
             this.d1 = d1;
             this.d2 = d2;
         }
     }

     static final long[] OFFSETS = new long[] { 16, 24 };

     public static Pair helper(double[] values, boolean[] mask) {
         Pair p = Unsafe.getUnsafe().makePrivateBuffer(Pair.default);

         for (int i = 0; i < OFFSETS.length; i++) {
             if (mask[i]) {
                 Unsafe.getUnsafe().putDouble(p, OFFSETS[i], values[i]);
             }
         }
         return Unsafe.getUnsafe().finishPrivateBuffer(p);
    }


    public static double test(double[] values, boolean[] mask) {
         Pair p = helper(values, mask);
         return p.d1 + p.d2;
    }

    public static void main(String[] args) {
        double[] values = new double[] { 1.0, 2.0 };
        boolean[] mask = new boolean[] { true, false };

        double d = 0.0;
        for (int i = 0; i < 10000; i++) {
            d += test(values, mask);
        }
        if (d != 10000.0) {
            throw new AssertionError("Incorrect Result expected(10000.0) != actual(" + d + ")");
        }
    }
}
