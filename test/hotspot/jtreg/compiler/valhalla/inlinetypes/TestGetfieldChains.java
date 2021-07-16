/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import compiler.lib.ir_framework.CompLevel;
import compiler.lib.ir_framework.Run;
import compiler.lib.ir_framework.Scenario;
import compiler.lib.ir_framework.Test;
import jdk.test.lib.Asserts;


/*
 * @test
 * @key randomness
 * @summary Verify that chains of getfields on flattened fields are correctly optimized
 * @library /test/lib /
 * @requires os.simpleArch == "x64"
 * @compile GetfieldChains.jcod
 * @run driver/timeout=300 compiler.valhalla.inlinetypes.TestGetfieldChains
 */

public class TestGetfieldChains {

    public static void main(String[] args) {

        final Scenario[] scenarios = {
                new Scenario(0,
                        // C1 only
                        "-XX:TieredStopAtLevel=1",
                        "-XX:+TieredCompilation"),
                new Scenario(1,
                        // C2 only. (Make sure the tests are correctly written)
                        "-XX:TieredStopAtLevel=4",
                        "-XX:-TieredCompilation",
                        "-XX:-OmitStackTraceInFastThrow"),
                new Scenario(2,
                        // interpreter only
                        "-Xint"),
                new Scenario(3,
                        // Xcomp Only C1.
                        "-XX:TieredStopAtLevel=1",
                        "-XX:+TieredCompilation",
                        "-Xcomp"),
                new Scenario(4,
                        // Xcomp Only C2.
                        "-XX:TieredStopAtLevel=4",
                        "-XX:-TieredCompilation",
                        "-XX:-OmitStackTraceInFastThrow",
                        "-Xcomp")
        };

        InlineTypes.getFramework()
                   .addScenarios(scenarios)
                   .start();
    }


    // Simple chain of getfields ending with primitive field
    @Test(compLevel = CompLevel.C1_SIMPLE)
    public int test1() {
        return NamedRectangle.getP1X(new NamedRectangle());
    }

    @Run(test = "test1")
    public void test1_verifier() {
        int res = test1();
        Asserts.assertEQ(res, 4);
    }

    // Simple chain of getfields ending with a flattened field
    @Test(compLevel = CompLevel.C1_SIMPLE)
    public Point test2() {
        return NamedRectangle.getP1(new NamedRectangle());
    }

    @Run(test = "test2")
    public void test2_verifier() {
        Point p = test2();
        Asserts.assertEQ(p.x, 4);
        Asserts.assertEQ(p.y, 7);
    }

    // Chain of getfields but the initial receiver is null
    @Test(compLevel = CompLevel.C1_SIMPLE)
    public NullPointerException test3() {
        NullPointerException npe = null;
        try {
            NamedRectangle.getP1X(null);
        } catch(NullPointerException e) {
            npe = e;
        }
        return npe;
    }

    @Run(test = "test3")
    public void test3_verifier() {
        NullPointerException npe = test3();
        Asserts.assertNE(npe, null);
        StackTraceElement st = npe.getStackTrace()[0];
        Asserts.assertEQ(st.getMethodName(), "getP1X");
        Asserts.assertEQ(st.getLineNumber(), 31);       // line number depends on file NamedRectangle.java
    }

    // Chain of getfields but one getfield in the middle of the chain trigger an illegal access
    @Test(compLevel = CompLevel.C1_SIMPLE)
    public IllegalAccessError test4() {
        IllegalAccessError iae = null;
        try {
            int i = NamedRectangleP.getP1X(new NamedRectangleP());
        } catch(IllegalAccessError e) {
            iae = e;
        }
        return iae;
    }

    @Run(test = "test4")
    public void test4_verifier() {
        IllegalAccessError iae = test4();
        Asserts.assertNE(iae, null);
        StackTraceElement st = iae.getStackTrace()[0];
        Asserts.assertEQ(st.getMethodName(), "getP1X");
        Asserts.assertEQ(st.getLineNumber(), 31);       // line number depends on jcod file generated from NamedRectangle.java
        Asserts.assertTrue(iae.getMessage().contains("class compiler.valhalla.inlinetypes.NamedRectangleP tried to access private field compiler.valhalla.inlinetypes.RectangleP.p1"));
    }

    // Chain of getfields but the last getfield trigger a NoSuchFieldError
    @Test(compLevel = CompLevel.C1_SIMPLE)
    public NoSuchFieldError test5() {
        NoSuchFieldError nsfe = null;
        try {
            int i = NamedRectangleN.getP1X(new NamedRectangleN());
        } catch(NoSuchFieldError e) {
            nsfe = e;
        }
        return nsfe;
    }

    @Run(test = "test5")
    public void test5_verifier() {
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

    @Test(compLevel = CompLevel.C1_SIMPLE)
    public EmptyType test6() {
        Container c = new Container();
        return c.container1.et;
    }

    @Run(test = "test6")
    public void test6_verifier() {
        EmptyType et = test6();
        Asserts.assertEQ(et, EmptyType.default);
    }

    @Test(compLevel = CompLevel.C1_SIMPLE)
    public EmptyType test7() {
        Container[] ca = new Container[10];
        return ca[3].container0.et;
    }

    @Run(test = "test7")
    public void test7_verifier() {
        EmptyType et = test7();
        Asserts.assertEQ(et, EmptyType.default);
    }
}
