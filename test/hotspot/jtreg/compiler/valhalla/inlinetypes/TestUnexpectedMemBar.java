/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8270995
 * @summary Membars of non-escaping inline type buffer allocations should be removed.
 * @library /test/lib
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:-TieredCompilation -XX:-ReduceInitialCardMarks
 *                   -XX:+AlwaysIncrementalInline -Xbatch -XX:CompileCommand=compileonly,*TestUnexpectedMemBar::test*
 *                   -XX:+StressIGVN -XX:+StressGCM -XX:+StressLCM -XX:StressSeed=851121348
 *                   compiler.valhalla.inlinetypes.TestUnexpectedMemBar
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:-TieredCompilation -XX:-ReduceInitialCardMarks -XX:+AlwaysIncrementalInline
 *                   -Xbatch -XX:CompileCommand=compileonly,*TestUnexpectedMemBar::test*
 *                   -XX:+StressIGVN -XX:+StressGCM -XX:+StressLCM
 *                   compiler.valhalla.inlinetypes.TestUnexpectedMemBar
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -Xbatch -XX:CompileCommand=compileonly,*TestUnexpectedMemBar::test*
 *                   -XX:+StressIGVN -XX:+StressGCM -XX:+StressLCM
 *                   compiler.valhalla.inlinetypes.TestUnexpectedMemBar
 */

package compiler.valhalla.inlinetypes;

import jdk.test.lib.Asserts;

primitive class MyValue {
    final int a = 0;
    final int b = 0;
    final int c = 0;
    final int d = 0;
    final int e = 0;

    final Integer i;
    final int[] array;

    public MyValue(Integer i, int[] array) {
        this.i = i;
        this.array = array;
    }
}

public class TestUnexpectedMemBar {

    public static int test1(Integer i) {
        int[] array = new int[1];
        MyValue vt = new MyValue(i, array);
        vt = new MyValue(vt.i, vt.array);
        return vt.i + vt.array[0];
    }

    public static int test2(Integer i) {
        int[] array = {i};
        MyValue vt = new MyValue(i, array);
        vt = new MyValue(vt.i, vt.array);
        return vt.i + vt.array[0];
    }

    public static void main(String[] args) {
        for (int i = 0; i < 100_000; ++i) {
            int res = test1(i);
            Asserts.assertEquals(res, i, "test1 failed");
            res = test2(i);
            Asserts.assertEquals(res, 2*i, "test2 failed");
        }
    }
}
