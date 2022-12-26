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
package compiler.c2.irTests;

import jdk.test.lib.Asserts;
import compiler.lib.ir_framework.*;

/*
 * @test
 * @bug 8297384
 * @summary Test that Ideal transformations of URShiftLNode* are being performed as expected.
 * @library /test/lib /
 * @run driver compiler.c2.irTests.URShiftLNodeIdealizationTests
 */
public class URShiftLNodeIdealizationTests {

    public static void main(String[] args) {
        TestFramework.run();
    }

    @Run(test = { "test1", "test2" })
    public void runMethod() {
        long a = RunInfo.getRandom().nextInt();

        long min = Long.MIN_VALUE;
        long max = Long.MAX_VALUE;

        assertResult(0);
        assertResult(a);
        assertResult(min);
        assertResult(max);
    }

    @DontCompile
    public void assertResult(long a) {
        Asserts.assertEQ((a << 2022) >>> 2022, test1(a));
        Asserts.assertEQ((a >> 2022) >>> 63, test2(a));
    }

    @Test
    @IR(failOn = { IRNode.LSHIFT, IRNode.URSHIFT })
    @IR(counts = { IRNode.AND, "1" })
    // Checks (x << 2022) >>> 2022 => x & C where C = ((1 << (64 - 38)) - 1)
    public long test1(long x) {
        return (x << 2022) >>> 2022;
    }

    @Test
    @IR(failOn = { IRNode.RSHIFT })
    @IR(counts = { IRNode.URSHIFT, "1" })
    // Checks (x >> 2022) >>> 63 => x >>> 63
    public long test2(long x) {
        return (x >> 2022) >>> 63;
    }
}
