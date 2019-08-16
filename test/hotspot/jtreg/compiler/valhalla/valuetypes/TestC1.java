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


package compiler.valhalla.valuetypes;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.Arrays;

import jdk.experimental.value.MethodHandleBuilder;
import jdk.test.lib.Asserts;

/*
 * @test
 * @summary Various tests that are specific for C1.
 * @modules java.base/jdk.experimental.value
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires os.simpleArch == "x64"
 * @compile -XDallowWithFieldOperator TestC1.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=300 -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                               compiler.valhalla.valuetypes.ValueTypeTest
 *                               compiler.valhalla.valuetypes.TestC1
 */
public class TestC1 extends ValueTypeTest {
    public static final int C1 = COMP_LEVEL_SIMPLE;
    public static final int C2 = COMP_LEVEL_FULL_OPTIMIZATION;

    public static void main(String[] args) throws Throwable {
        TestC1 test = new TestC1();
        test.run(args, MyValue1.class, MyValue2.class, MyValue2Inline.class, MyValue3.class, MyValue3Inline.class);
    }

    @Override
    public int getNumScenarios() {
        return 5;
    }

    @Override
    public String[] getVMParameters(int scenario) {
        switch (scenario) {
        case 0: return new String[] { // C1 only
                "-XX:+EnableValhallaC1",
                "-XX:TieredStopAtLevel=1",
            };
        case 1: return new String[] { // C2 only. (Make sure the tests are correctly written)
                "-XX:-EnableValhallaC1",
                "-XX:TieredStopAtLevel=4",
            };
        case 2: return new String[] { // interpreter only
                "-Xint",
            };
        case 3: return new String[] {
                // Xcomp Only C1.
                "-XX:+EnableValhallaC1",
                "-XX:TieredStopAtLevel=1",
                "-Xcomp",
            };
        case 4: return new String[] {
                // Xcomp Only C2.
                "-XX:-EnableValhallaC1",
                "-XX:TieredStopAtLevel=4",
                "-Xcomp",
            };
        }
        return null;
    }

    // JDK-8229799
    @Test(compLevel=C1)
    public long test1(Object a, Object b, long n) {
        long r;
        n += (a == b) ? 0x5678123456781234L : 0x1234567812345678L;
        n -= 1;
        return n;
    }

    @DontCompile
    public void test1_verifier(boolean warmup) {
        MyValue1 v1 = MyValue1.createWithFieldsInline(rI, rL);
        MyValue1 v2 = MyValue1.createWithFieldsInline(rI, rL+1);
        long r1 = test1(v1, v1, 1);
        long r2 = test1(v1, v2, 1);
        Asserts.assertEQ(r1, 0x5678123456781234L);
        Asserts.assertEQ(r2, 0x1234567812345678L);
    }
}
