/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8321734
 * @requires vm.gc.Serial
 * @summary Test that CmpPNode::sub and SubTypeCheckNode::sub correctly identify unrelated classes based on the
 *          flat in array property of the types.
 * @library /test/lib /
 * @run driver compiler.valhalla.inlinetypes.TestFlatInArraysFolding
 */

package compiler.valhalla.inlinetypes;

import compiler.lib.ir_framework.*;

public class TestFlatInArraysFolding {
    static Object[] oArrArr = new Object[100][100];
    static Object[] oArr = new Object[100];

    static int iFld;

    public static void main(String[] args) {
        // Disable Loop Unrolling for IR matching in testCmpP().
        // Use IgnoreUnrecognizedVMOptions since LoopMaxUnroll is a C2 flag.
        // testSubTypeCheck() only triggers with SerialGC.
        new TestFramework()
                .setDefaultWarmup(0)
                .addFlags("-XX:+UseSerialGC", "-XX:+IgnoreUnrecognizedVMOptions", "-XX:LoopMaxUnroll=0")
                .start();
    }

    // SubTypeCheck is not folded while CheckCastPPNode is replaced by top which results in a bad graph (data dies while
    // control does not).
    @Test
    static void testSubTypeCheck() {
        for (int i = 0; i < 100; i++) {
            Object arrayElement = oArrArr[i];
            oArr = (Object[])arrayElement;
        }
    }

    // Only improve the super class for constants which allows subsequent sub type checks to possibly be commoned up.
    // The other non-constant cases cannot be improved with a cast node here since they could be folded to top.
    // Additionally, the benefit would only be minor in non-constant cases.

    @Test
    @IR(counts = {IRNode.COUNTED_LOOP, "2", // Loop Unswitching done?
                  IRNode.STORE_I, "1"}) // CmpP folded in unswitched loop version with flat in array?
    static void testCmpP() {
        for (int i = 0; i < 100; i++) {
            Object arrayElement = oArrArr[i];
            if (arrayElement == oArr) {
                iFld = 34;
            }
        }
    }
}

