/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;

import jdk.test.lib.Asserts;

/*
 * @test
 * @key randomness
 * @summary Verify that chains of getfields on flattened fields are correctly optimized
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires os.simpleArch == "x64"
 * @compile TestGetfieldChains.java NamedRectangle.java Rectangle.java Point.java GetfieldChains.jcod
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=300 -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                               compiler.valhalla.inlinetypes.InlineTypeTest
 *                               compiler.valhalla.inlinetypes.TestGetfieldChains
 */

public class TestGetfieldChains extends InlineTypeTest {
    public static final int C1 = COMP_LEVEL_SIMPLE;
    public static final int C2 = COMP_LEVEL_FULL_OPTIMIZATION;

    public static void main(String[] args) throws Throwable {
        TestGetfieldChains test = new TestGetfieldChains();
        test.run(args, TestGetfieldChains.class);
    }

    @Override
    public int getNumScenarios() {
        return 5;
    }

    @Override
    public String[] getVMParameters(int scenario) {
        switch (scenario) {
        case 0: return new String[] { // C1 only
                "-XX:TieredStopAtLevel=1",
                "-XX:+TieredCompilation",
            };
        case 1: return new String[] { // C2 only. (Make sure the tests are correctly written)
                "-XX:TieredStopAtLevel=4",
                "-XX:-TieredCompilation",
                "-XX:-OmitStackTraceInFastThrow",
            };
        case 2: return new String[] { // interpreter only
                "-Xint",
            };
        case 3: return new String[] {
                // Xcomp Only C1.
                "-XX:TieredStopAtLevel=1",
                "-XX:+TieredCompilation",
                "-Xcomp",
            };
        case 4: return new String[] {
                // Xcomp Only C2.
                "-XX:TieredStopAtLevel=4",
                "-XX:-TieredCompilation",
                "-XX:-OmitStackTraceInFastThrow",
                "-Xcomp",
            };
        }
        return null;
    }

    // Simple chain of getfields ending with primitive field
    @Test(compLevel=C1)
    public int test1() {
        return NamedRectangle.getP1X(new NamedRectangle());
    }

    @DontCompile
    public void test1_verifier(boolean warmup) {
        int res = test1();
        Asserts.assertEQ(res, 4);
    }

    // Simple chain of getfields ending with a flattened field
    @Test(compLevel=C1)
    public Point test2() {
        return NamedRectangle.getP1(new NamedRectangle());
    }

    @DontCompile
    public void test2_verifier(boolean warmup) {
        Point p = test2();
        Asserts.assertEQ(p.x, 4);
        Asserts.assertEQ(p.y, 7);
    }

    // Chain of getfields but the initial receiver is null
    @Test(compLevel=C1)
    public NullPointerException test3() {
        NullPointerException npe = null;
        try {
            NamedRectangle.getP1X(null);
        } catch(NullPointerException e) {
            npe = e;
        }
        return npe;
    }

    @DontCompile
    public void test3_verifier(boolean warmup) {
        NullPointerException npe = test3();
        Asserts.assertNE(npe, null);
        StackTraceElement st = npe.getStackTrace()[0];
        Asserts.assertEQ(st.getMethodName(), "getP1X");
        Asserts.assertEQ(st.getLineNumber(), 31);       // line number depends on file NamedRectangle.java
    }

    // Chain of getfields but one getfield in the middle of the chain trigger an illegal access
    @Test(compLevel=C1)
    public IllegalAccessError test4() {
        IllegalAccessError iae = null;
        try {
            int i = NamedRectangleP.getP1X(new NamedRectangleP());
        } catch(IllegalAccessError e) {
            iae = e;
        }
        return iae;
    }

    @DontCompile
    public void test4_verifier(boolean warmup) {
        IllegalAccessError iae = test4();
        Asserts.assertNE(iae, null);
        StackTraceElement st = iae.getStackTrace()[0];
        Asserts.assertEQ(st.getMethodName(), "getP1X");
        Asserts.assertEQ(st.getLineNumber(), 31);       // line number depends on jcod file generated from NamedRectangle.java
        Asserts.assertTrue(iae.getMessage().contains("class compiler.valhalla.inlinetypes.NamedRectangleP tried to access private field compiler.valhalla.inlinetypes.RectangleP.p1"));
    }

    // Chain of getfields but the last getfield trigger a NoSuchFieldError
    @Test(compLevel=C1)
    public NoSuchFieldError test5() {
        NoSuchFieldError nsfe = null;
        try {
            int i = NamedRectangleN.getP1X(new NamedRectangleN());
        } catch(NoSuchFieldError e) {
            nsfe = e;
        }
        return nsfe;
    }

    @DontCompile
    public void test5_verifier(boolean warmup) {
        NoSuchFieldError nsfe = test5();
        Asserts.assertNE(nsfe, null);
        StackTraceElement st = nsfe.getStackTrace()[0];
        Asserts.assertEQ(st.getMethodName(), "getP1X");
        Asserts.assertEQ(st.getLineNumber(), 31);       // line number depends on jcod file generated from NamedRectangle.java
        Asserts.assertEQ(nsfe.getMessage(), "x");
    }

    static primitive class EmptyType { }
    static primitive class EmptyContainer {
        int i = 0;
        EmptyType et = new EmptyType();
    }
    static primitive class Container {
        EmptyContainer container0 = new EmptyContainer();
        EmptyContainer container1 = new EmptyContainer();
    }

    @Test(compLevel=C1)
    public EmptyType test6() {
        Container c = new Container();
        return c.container1.et;
    }

    @DontCompile
    public void test6_verifier(boolean warmup) {
        EmptyType et = test6();
        Asserts.assertEQ(et, EmptyType.default);
    }

    @Test(compLevel=C1)
    public EmptyType test7() {
        Container[] ca = new Container[10];
        return ca[3].container0.et;
    }

    @DontCompile
    public void test7_verifier(boolean warmup) {
        EmptyType et = test7();
        Asserts.assertEQ(et, EmptyType.default);
    }
}
