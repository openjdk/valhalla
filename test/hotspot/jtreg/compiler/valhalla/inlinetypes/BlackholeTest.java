/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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

package compiler.valhalla.inlinetypes;

/*
 * @test BlackholeTest
 * @summary Check that blackholes work with inline types
 * @run main/othervm
 *      -Xbatch
 *      -XX:+UnlockExperimentalVMOptions
 *      -XX:CompileCommand=blackhole,compiler/valhalla/inlinetypes/BlackholeTest.blackhole
 *      compiler.valhalla.inlinetypes.BlackholeTest
 */

public class BlackholeTest {
    static primitive class MyValue {
        int x = 0;
    }

    static MyValue v;
    static volatile MyValue vv;

    public static void main(String[] args) {
        for (int c = 0; c < 5; c++) {
            testNew();
            testDefault();
            testField();
            testVolatileField();
        }
    }

    private static void testNew() {
        for (int c = 0; c < 100000; c++) {
            blackhole(new MyValue());
        }
    }

    private static void testDefault() {
        for (int c = 0; c < 100000; c++) {
            blackhole(MyValue.default);
        }
    }

    private static void testField() {
        for (int c = 0; c < 100000; c++) {
            blackhole(v);
        }
    }

    private static void testVolatileField() {
        for (int c = 0; c < 100000; c++) {
            blackhole(vv);
        }
    }

    public static void blackhole(MyValue v) {
        // Should be empty
    }
}
