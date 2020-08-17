/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @requires vm.gc != "Z"
 * @bug 8233415
 * @summary Verify that TLAB allocated buffer initialization when returning an inline type works properly with oops.
 * @library /test/lib
 * @run main/othervm -XX:CompileCommand=exclude,compiler.valhalla.inlinetypes.TestStressReturnBuffering::caller -Xmx4m
 *                   compiler.valhalla.inlinetypes.TestStressReturnBuffering
 */

package compiler.valhalla.inlinetypes;

import jdk.test.lib.Asserts;

inline class MyValue {
    public Integer o1;
    public Integer o2;
    public Integer o3;
    public Integer o4;
    public Integer o5;

    public MyValue(Integer o) {
        this.o1 = o;
        this.o2 = o;
        this.o3 = o;
        this.o4 = o;
        this.o5 = o;
    }
}

public class TestStressReturnBuffering {

    static Integer integer = 42;

    public static MyValue callee() {
        return new MyValue(integer);
    }

    public static int caller() {
        int res = 0;
        for (int i = 0; i < 100_000; ++i) {
            MyValue vt = callee();
            res += vt.o1 + vt.o2 + vt.o3 + vt.o4 + vt.o5;
        }
        return res;
    }

    public static void main(String[] args) {
        System.gc();
        int res = caller();
        Asserts.assertEQ(res, 100_000*5*42, "Unexpected result");
    }
}
