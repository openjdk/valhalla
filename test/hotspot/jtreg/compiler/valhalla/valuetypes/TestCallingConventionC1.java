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

import jdk.test.lib.Asserts;

/*
 * @test
 * @summary Test calls from {C1} to {C2, Interpreter}, and vice versa.
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @requires os.simpleArch == "x64"
 * @compile -XDallowWithFieldOperator TestCallingConventionC1.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI -XX:+EnableValhalla
 *                               compiler.valhalla.valuetypes.ValueTypeTest
 *                               compiler.valhalla.valuetypes.TestCallingConventionC1
 */
public class TestCallingConventionC1 extends ValueTypeTest {
    public static final int C1 = COMP_LEVEL_SIMPLE;
    public static final int C2 = COMP_LEVEL_FULL_OPTIMIZATION;

    @Override
    public int getNumScenarios() {
        return 2;
    }

    @Override
    public String[] getVMParameters(int scenario) {
        switch (scenario) {

        // Default: both C1 and C2 are enabled, tierd compilation enabled
        case 0: return new String[] {"-XX:+EnableValhallaC1", "-XX:CICompilerCount=2"
                                     , "-XX:-CheckCompressedOops", "-XX:CompileCommand=print,*::test3*"
                                     };
        // Only C1. Tierd compilation disabled.
        case 1: return new String[] {"-XX:+EnableValhallaC1", "-XX:TieredStopAtLevel=1"};
        }
        return null;
    }

    public static void main(String[] args) throws Throwable {
        TestCallingConventionC1 test = new TestCallingConventionC1();
        test.run(args,
                 Point.class,
                 Functor.class,
                 Functor1.class,
                 Functor2.class,
                 Functor3.class,
                 Functor4.class);
    }

    static value class Point {
        final int x;
        final int y;
        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @DontCompile // FIXME -- C1 can't handle incoming values yet
        public int func() {
            return x + y;
        }
    }

    static interface FunctorInterface {
        public int apply(Point p);
    }

    static class Functor implements FunctorInterface {
        @DontCompile // FIXME -- C1 can't handle incoming values yet
        @DontInline
        public int apply(Point p) {
            return p.func() + 0;
        }
    }
    static class Functor1 extends Functor {
        @DontCompile // FIXME -- C1 can't handle incoming values yet
        @DontInline
        public int apply(Point p) {
            return p.func() + 10000;
        }
    }
    static class Functor2 extends Functor {
        @DontCompile // FIXME -- C1 can't handle incoming values yet
        @DontInline
        public int apply(Point p) {
            return p.func() + 20000;
        }
    }
    static class Functor3 extends Functor {
        @DontCompile // FIXME -- C1 can't handle incoming values yet
        @DontInline
        public int apply(Point p) {
            return p.func() + 30000;
        }
    }
    static class Functor4 extends Functor {
        @DontCompile // FIXME -- C1 can't handle incoming values yet
        @DontInline
        public int apply(Point p) {
            return p.func() + 40000;
        }
    }

    static Functor functors[] = {
        new Functor(),
        new Functor1(),
        new Functor2(),
        new Functor3(),
        new Functor4()
    };
    static int counter = 0;
    static Functor getFunctor() {
        int n = (++ counter) % functors.length;
        return functors[n];
    }

    static Point pointField = new Point(123, 456);

    //**********************************************************************
    // PART 1 - C1 calls interpreted code
    //**********************************************************************


    //** C1 passes value to interpreter (static)
    @Test(compLevel = C1)
    public int test1() {
        return test1_helper(pointField);
    }

    @DontInline
    @DontCompile
    private static int test1_helper(Point p) {
        return p.func();
    }

    @DontCompile
    public void test1_verifier(boolean warmup) {
        int count = warmup ? 1 : 10;
        for (int i=0; i<count; i++) {
            int result = test1() + i;
            Asserts.assertEQ(result, pointField.func() + i);
        }
    }


    //** C1 passes value to interpreter (monomorphic)
    @Test(compLevel = C1)
    public int test2() {
        return test2_helper(pointField);
    }

    @DontInline
    @DontCompile
    private int test2_helper(Point p) {
        return p.func();
    }

    @DontCompile
    public void test2_verifier(boolean warmup) {
        int count = warmup ? 1 : 10;
        for (int i=0; i<count; i++) {
            int result = test2() + i;
            Asserts.assertEQ(result, pointField.func() + i);
        }
    }

    // C1 passes value to interpreter (megamorphic: vtable)
    @Test(compLevel = C1)
    public int test3(Functor functor) {
        return functor.apply(pointField);
    }

    @DontCompile
    public void test3_verifier(boolean warmup) {
        int count = warmup ? 1 : 100;
        for (int i=0; i<count; i++) {
            Functor functor = warmup ? functors[0] : getFunctor();
            int result = test3(functor) + i;
            Asserts.assertEQ(result, functor.apply(pointField) + i);
        }
    }

    // Same as test3, but compiled with C2. Test the hastable of VtableStubs
    @Test(compLevel = C2)
    public int test3b(Functor functor) {
        return functor.apply(pointField);
    }

    @DontCompile
    public void test3b_verifier(boolean warmup) {
        int count = warmup ? 1 : 100;
        for (int i=0; i<count; i++) {
            Functor functor = warmup ? functors[0] : getFunctor();
            int result = test3b(functor) + i;
            Asserts.assertEQ(result, functor.apply(pointField) + i);
        }
    }

    // C1 passes value to interpreter (megamorphic: itable)
    @Test(compLevel = C1)
    public int test4(FunctorInterface fi) {
        return fi.apply(pointField);
    }

    @DontCompile
    public void test4_verifier(boolean warmup) {
        int count = warmup ? 1 : 100;
        for (int i=0; i<count; i++) {
            Functor functor = warmup ? functors[0] : getFunctor();
            int result = test4(functor) + i;
            Asserts.assertEQ(result, functor.apply(pointField) + i);
        }
    }

    /* not working

    // Interpreter passes value to C1
    @Test(compLevel = C2)
    public int test2(Point p) {
        return p.x + p.y;
    }

    @DontCompile
    public void test2_verifier(boolean warmup) {
        int result = test2(pointField);
        int n = pointField.x + pointField.y;
        Asserts.assertEQ(result, n);
    }

    */


    /*

    // C1 passes value to C2
    @Test(compLevel = C1)
    public int test3() {
        return test3_helper(pointField);
    }

    @DontInline
    @ForceCompile(compLevel = C2)
    private static int test3_helper(Point p) {
        return p.x + p.y;
    }

    @DontCompile
    public void test3_verifier(boolean warmup) {
        int result = test3();
        int n = pointField.x + pointField.y;
        Asserts.assertEQ(result, n);
    }

    */
}
