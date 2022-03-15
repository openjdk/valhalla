/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import compiler.lib.ir_framework.*;
import jdk.test.lib.Asserts;

import static compiler.valhalla.inlinetypes.InlineTypes.rI;

/*
 * @test
 * @key randomness
 * @summary Test the handling of fields of unloaded inline classes.
 * @library /test/lib /
 * @requires (os.simpleArch == "x64" | os.simpleArch == "aarch64")
 * @compile hack/GetUnresolvedInlineFieldWrongSignature.java
 * @compile TestUnloadedInlineTypeField.java
 * @run driver/timeout=300 compiler.valhalla.inlinetypes.TestUnloadedInlineTypeField
 */

public class TestUnloadedInlineTypeField {
    // Only prevent loading of classes when testing with C1. Load classes
    // early when executing with C2 to prevent uncommon traps. It's still
    // beneficial to execute this test with C2 because it also checks handling
    // of type mismatches.

    public static void main(String[] args) {
        final Scenario[] scenarios = {
                new Scenario(0),
                new Scenario(1, "-XX:InlineFieldMaxFlatSize=0"),
                new Scenario(2, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:+PatchALot"),
                new Scenario(3, "-XX:InlineFieldMaxFlatSize=0",
                                "-XX:+IgnoreUnrecognizedVMOptions", "-XX:+PatchALot")
        };
        final String[] flags = {// Prevent IR Test Framework from loading classes
                                "-DIgnoreCompilerControls=true",
                                // Some tests trigger frequent re-compilation. Don't mark them as non-compilable.
                                "-XX:PerMethodRecompilationCutoff=-1", "-XX:PerBytecodeRecompilationCutoff=-1"};
        for (Scenario s : scenarios) {
           s.addFlags(flags);
        }
        InlineTypes.getFramework()
                   .addScenarios(scenarios)
                   .start();
    }

    // Test case 1:
    // The inline type field class has been loaded, but the holder class has not been loaded.
    //
    //     aload_0
    //     getfield  MyValue1Holder.v:QMyValue1;
    //               ^ not loaded      ^ already loaded
    //
    // MyValue1 has already been loaded, because it's in the InlineType attribute of
    // TestUnloadedInlineTypeField, due to TestUnloadedInlineTypeField.test1_precondition().
    static final primitive class MyValue1 {
        final int foo;

        MyValue1() {
            foo = rI;
        }
    }

    static class MyValue1Holder {
        MyValue1 v;

        public MyValue1Holder() {
            v = new MyValue1();
        }
    }

    static MyValue1 test1_precondition() {
        return new MyValue1();
    }

    @Test
    public int test1(Object holder) {
        if (holder != null) {
            // Don't use MyValue1Holder in the signature, it might trigger class loading
            return ((MyValue1Holder)holder).v.foo;
        } else {
            return 0;
        }
    }

    @Run(test = "test1")
    public void test1_verifier(RunInfo info) {
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test1(null);
        } else {
            MyValue1Holder holder = new MyValue1Holder();
            Asserts.assertEQ(test1(holder), rI);
        }
    }

    // Test case 2:
    // Both the inline type field class, and the holder class have not been loaded.
    //
    //     aload_0
    //     getfield  MyValue2Holder.v:QMyValue2;
    //               ^ not loaded     ^ not loaded
    //
    // MyValue2 has not been loaded, because it is not explicitly referenced by
    // TestUnloadedInlineTypeField.
    static final primitive class MyValue2 {
        final int foo;

        public MyValue2(int n) {
            foo = n;
        }
    }

    static class MyValue2Holder {
        MyValue2 v;

        public MyValue2Holder() {
            v = new MyValue2(rI);
        }
    }

    @Test
    public int test2(Object holder) {
        if (holder != null) {
            // Don't use MyValue2Holder in the signature, it might trigger class loading
            return ((MyValue2Holder)holder).v.foo;
        } else {
            return 0;
        }
    }

    @Run(test = "test2")
    public void test2_verifier(RunInfo info) {
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test2(null);
        } else {
            MyValue2Holder holder = new MyValue2Holder();
            Asserts.assertEQ(test2(holder), rI);
        }
    }

    // Test case 3: same as test1, except we are using an incorrect signature to
    // refer to the inline class.
    // The inline type field class has been loaded, but the holder class has not been loaded.
    //
    // GetUnresolvedInlineFieldWrongSignature::test3() {
    //     aload_0
    //     getfield  MyValue3Holder.v:LMyValue3;
    //               ^ not loaded    ^ already loaded (but should have been "Q")
    //     ...
    // }
    //
    // MyValue3 has already been loaded, because it's in the InlineType attribute of
    // TestUnloadedInlineTypeField, due to TestUnloadedInlineTypeField.test3_precondition().
    static final primitive class MyValue3 {
        final int foo;

        public MyValue3() {
            foo = rI;
        }
    }

    static class MyValue3Holder {
        MyValue3 v;

        public MyValue3Holder() {
            v = new MyValue3();
        }
    }

    static MyValue3 test3_precondition() {
        return new MyValue3();
    }

    @Test
    public int test3(Object holder) {
        // Don't use MyValue3Holder in the signature, it might trigger class loading
        return GetUnresolvedInlineFieldWrongSignature.test3(holder);
    }

    @Run(test = "test3")
    public void test3_verifier(RunInfo info) {
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test3(null);
        } else {
            // Make sure klass is resolved
            for (int i = 0; i < 10; ++i) {
                MyValue3Holder holder = new MyValue3Holder();
                try {
                    test3(holder);
                    Asserts.fail("Should have thrown NoSuchFieldError");
                } catch (NoSuchFieldError e) {
                    // OK
                }
            }
        }
    }

    // Test case 4:
    // Same as case 1, except we use putfield instead of getfield.
    static final primitive class MyValue4 {
        final int foo;

        MyValue4(int n) {
            foo = n;
        }
    }

    static class MyValue4Holder {
        MyValue4 v;

        public MyValue4Holder() {
            v = new MyValue4(0);
        }
    }

    @Test
    public void test4(Object holder, MyValue4 v) {
        if (holder != null) {
            // Don't use MyValue4Holder in the signature, it might trigger class loading
            ((MyValue4Holder)holder).v = v;
        }
    }

    @Run(test = "test4")
    public void test4_verifier(RunInfo info) {
        MyValue4 v = new MyValue4(rI);
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test4(null, v);
        } else {
            MyValue4Holder holder = new MyValue4Holder();
            test4(holder, v);
            Asserts.assertEQ(holder.v.foo, rI);
        }
    }

    // Test case 5:
    // Same as case 2, except we use putfield instead of getfield.
    static final primitive class MyValue5 {
        final int foo;

        MyValue5(int n) {
            foo = n;
        }
    }

    static class MyValue5Holder {
        MyValue5 v;

        public MyValue5Holder() {
            v = new MyValue5(0);
        }

        public Object make(int n) {
            return new MyValue5(n);
        }
    }

    @Test
    public void test5(Object holder, Object o) {
        if (holder != null) {
            // Don't use MyValue5 and MyValue5Holder in the signature, it might trigger class loading
            MyValue5 v = (MyValue5)o;
            ((MyValue5Holder)holder).v = v;
        }
    }

    @Run(test = "test5")
    public void test5_verifier(RunInfo info) {
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test5(null, null);
        } else {
            MyValue5Holder holder = new MyValue5Holder();
            Object v = holder.make(rI);
            test5(holder, v);
            Asserts.assertEQ(holder.v.foo, rI);
        }
    }


    // Test case 6: (same as test1, except we use getstatic instead of getfield)
    // The inline type field class has been loaded, but the holder class has not been loaded.
    //
    //     getstatic  MyValue6Holder.v:QMyValue1;
    //                ^ not loaded       ^ already loaded
    //
    // MyValue6 has already been loaded, because it's in the InlineType attribute of
    // TestUnloadedInlineTypeField, due to TestUnloadedInlineTypeField.test1_precondition().
    static final primitive class MyValue6 {
        final int foo;

        MyValue6() {
            foo = rI;
        }
    }

    static class MyValue6Holder {
        static MyValue6 v = new MyValue6();
    }

    static MyValue6 test6_precondition() {
        return new MyValue6();
    }

    @Test
    public int test6(int n) {
        if (n == 0) {
            return 0;
        } else {
            return MyValue6Holder.v.foo + n;
        }
    }

    @Run(test = "test6")
    public void test6_verifier(RunInfo info) {
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test6(0);
        } else {
            Asserts.assertEQ(test6(rI), 2*rI);
        }
    }


    // Test case 7:  (same as test2, except we use getstatic instead of getfield)
    // Both the inline type field class, and the holder class have not been loaded.
    //
    //     getstatic  MyValue7Holder.v:QMyValue7;
    //                ^ not loaded       ^ not loaded
    //
    // MyValue7 has not been loaded, because it is not explicitly referenced by
    // TestUnloadedInlineTypeField.
    static final primitive class MyValue7 {
        final int foo;

        MyValue7(int n) {
            foo = n;
        }
    }

    static class MyValue7Holder {
        static MyValue7 v = new MyValue7(rI);
    }

    @Test
    public int test7(int n) {
        if (n == 0) {
            return 0;
        } else {
            return MyValue7Holder.v.foo + n;
        }
    }

    @Run(test = "test7")
    public void test7_verifier(RunInfo info) {
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test7(0);
        } else {
            Asserts.assertEQ(test7(rI), 2*rI);
        }
    }

    // Test case 8:
    // Same as case 1, except holder is allocated in test method (-> no holder null check required)
    static final primitive class MyValue8 {
        final int foo;

        MyValue8() {
            foo = rI;
        }
    }

    static class MyValue8Holder {
        MyValue8 v;

        public MyValue8Holder() {
            v = new MyValue8();
        }
    }

    static MyValue8 test8_precondition() {
        return new MyValue8();
    }

    @Test
    public int test8(boolean warmup) {
        if (!warmup) {
            MyValue8Holder holder = new MyValue8Holder();
            return holder.v.foo;
        } else {
            return 0;
        }
    }

    @Run(test = "test8")
    public void test8_verifier(RunInfo info) {
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test8(true);
        } else {
            Asserts.assertEQ(test8(false), rI);
        }
    }

    // Test case 9:
    // Same as case 2, except holder is allocated in test method (-> no holder null check required)
    static final primitive class MyValue9 {
        final int foo;

        public MyValue9(int n) {
            foo = n;
        }
    }

    static class MyValue9Holder {
        MyValue9 v;

        public MyValue9Holder() {
            v = new MyValue9(rI);
        }
    }

    @Test
    public int test9(boolean warmup) {
        if (!warmup) {
            MyValue9Holder holder = new MyValue9Holder();
            return holder.v.foo;
        } else {
            return 0;
        }
    }

    @Run(test = "test9")
    public void test9_verifier(RunInfo info) {
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test9(true);
        } else {
            Asserts.assertEQ(test9(false), rI);
        }
    }

    // Test case 10:
    // Same as case 4, but with putfield
    static final primitive class MyValue10 {
        final int foo;

        public MyValue10() {
            foo = rI;
        }
    }

    static class MyValue10Holder {
        MyValue10 v1;
        MyValue10 v2;

        public MyValue10Holder() {
            v1 = new MyValue10();
            v2 = new MyValue10();
        }
    }

    static MyValue10 test10_precondition() {
        return new MyValue10();
    }

    @Test
    public void test10(Object holder) {
        // Don't use MyValue10Holder in the signature, it might trigger class loading
        GetUnresolvedInlineFieldWrongSignature.test10(holder);
    }

    @Run(test = "test10")
    public void test10_verifier(RunInfo info) {
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test10(null);
        } else {
            // Make sure klass is resolved
            for (int i = 0; i < 10; ++i) {
                MyValue10Holder holder = new MyValue10Holder();
                try {
                    test10(holder);
                    Asserts.fail("Should have thrown NoSuchFieldError");
                } catch (NoSuchFieldError e) {
                    // OK
                }
            }
        }
    }

    // Test case 11:
    // Same as case 4, except holder is allocated in test method (-> no holder null check required)
    static final primitive class MyValue11 {
        final int foo;

        MyValue11(int n) {
            foo = n;
        }
    }

    static class MyValue11Holder {
        MyValue11 v;

        public MyValue11Holder() {
            v = new MyValue11(0);
        }
    }

    @Test
    public Object test11(boolean warmup, MyValue11 v) {
        if (!warmup) {
            MyValue11Holder holder = new MyValue11Holder();
            holder.v = v;
            return holder;
        } else {
            return null;
        }
    }

    @Run(test = "test11")
    public void test11_verifier(RunInfo info) {
        MyValue11 v = new MyValue11(rI);
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test11(true, v);
        } else {
            MyValue11Holder holder = (MyValue11Holder)test11(false, v);
            Asserts.assertEQ(holder.v.foo, rI);
        }
    }

    // Test case 12:
    // Same as case 5, except holder is allocated in test method (-> no holder null check required)
    static final primitive class MyValue12 {
        final int foo;

        MyValue12(int n) {
            foo = n;
        }
    }

    static class MyValue12Holder {
        MyValue12 v;

        public MyValue12Holder() {
            v = new MyValue12(0);
        }
    }

    @Test
    public Object test12(boolean warmup, Object o) {
        if (!warmup) {
            // Don't use MyValue12 in the signature, it might trigger class loading
            MyValue12Holder holder = new MyValue12Holder();
            holder.v = (MyValue12)o;
            return holder;
        } else {
            return null;
        }
    }

    @Run(test = "test12")
    public void test12_verifier(RunInfo info) {
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test12(true, null);
        } else {
            MyValue12 v = new MyValue12(rI);
            MyValue12Holder holder = (MyValue12Holder)test12(false, v);
            Asserts.assertEQ(holder.v.foo, rI);
        }
    }

    // Test case 13:
    // Same as case 10, except MyValue13 is allocated in test method
    static final primitive class MyValue13 {
        final int foo;

        public MyValue13() {
            foo = rI;
        }
    }

    static class MyValue13Holder {
        MyValue13 v;

        public MyValue13Holder() {
            v = new MyValue13();
        }
    }

    static MyValue13 test13_precondition() {
        return new MyValue13();
    }

    @Test
    public void test13(Object holder) {
        // Don't use MyValue13Holder in the signature, it might trigger class loading
        GetUnresolvedInlineFieldWrongSignature.test13(holder);
    }

    @Run(test = "test13")
    public void test13_verifier(RunInfo info) {
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test13(null);
        } else {
            // Make sure klass is resolved
            for (int i = 0; i < 10; ++i) {
                MyValue13Holder holder = new MyValue13Holder();
                try {
                    test13(holder);
                    Asserts.fail("Should have thrown InstantiationError");
                } catch (InstantiationError e) {
                    // OK
                }
            }
        }
    }

    // Test case 14:
    // Same as case 10, except storing null
    static final primitive class MyValue14 {
        final int foo;

        public MyValue14() {
            foo = rI;
        }
    }

    static class MyValue14Holder {
        MyValue14 v;

        public MyValue14Holder() {
            v = new MyValue14();
        }
    }

    static MyValue14 test14_precondition() {
        return new MyValue14();
    }

    @Test
    public void test14(Object holder) {
        // Don't use MyValue14Holder in the signature, it might trigger class loading
        GetUnresolvedInlineFieldWrongSignature.test14(holder);
    }

    @Run(test = "test14")
    public void test14_verifier(RunInfo info) {
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test14(null);
        } else {
            // Make sure klass is resolved
            for (int i = 0; i < 10; ++i) {
                MyValue14Holder holder = new MyValue14Holder();
                try {
                    test14(holder);
                    Asserts.fail("Should have thrown NoSuchFieldError");
                } catch (NoSuchFieldError e) {
                    // OK
                }
            }
        }
    }

    // Test case 15:
    // Same as case 13, except MyValue15 is unloaded
    static final primitive class MyValue15 {
        final int foo;

        public MyValue15() {
            foo = rI;
        }
    }

    static class MyValue15Holder {
        MyValue15 v;

        public MyValue15Holder() {
            v = new MyValue15();
        }
    }

    @Test
    public void test15(Object holder) {
        // Don't use MyValue15Holder in the signature, it might trigger class loading
        GetUnresolvedInlineFieldWrongSignature.test15(holder);
    }

    @Run(test = "test15")
    public void test15_verifier(RunInfo info) {
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test15(null);
        } else {
            // Make sure klass is resolved
            for (int i = 0; i < 10; ++i) {
                MyValue15Holder holder = new MyValue15Holder();
                try {
                    test15(holder);
                    Asserts.fail("Should have thrown InstantiationError");
                } catch (InstantiationError e) {
                    // OK
                }
            }
        }
    }

    // Test case 16:
    // aconst_init with type which is not an inline type
    static final class MyValue16 {
        final int foo;

        public MyValue16() {
            foo = rI;
        }
    }

    static MyValue16 test16_precondition() {
        return new MyValue16();
    }

    @Test
    public Object test16(boolean warmup) {
        return GetUnresolvedInlineFieldWrongSignature.test16(warmup);
    }

    @Run(test = "test16")
    public void test16_verifier(RunInfo info) {
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test16(true);
        } else {
            // Make sure klass is resolved
            for (int i = 0; i < 10; ++i) {
                try {
                    test16(false);
                    Asserts.fail("Should have thrown IncompatibleClassChangeError");
                } catch (IncompatibleClassChangeError e) {
                    // OK
                }
            }
        }
    }

    // Test case 17:
    // Same as test16 but with unloaded type at aconst_init
    static final class MyValue17 {
        final int foo;

        public MyValue17() {
            foo = rI;
        }
    }

    @Test
    public Object test17(boolean warmup) {
        return GetUnresolvedInlineFieldWrongSignature.test17(warmup);
    }

    @Run(test = "test17")
    public void test17_verifier(RunInfo info) {
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test17(true);
        } else {
            // Make sure klass is resolved
            for (int i = 0; i < 10; ++i) {
                try {
                    test17(false);
                    Asserts.fail("Should have thrown IncompatibleClassChangeError");
                } catch (IncompatibleClassChangeError e) {
                    // OK
                }
            }
        }
    }

    // Test case 18:
    // Same as test7 but with the holder being loaded
    static final primitive class MyValue18 {
        final int foo;

        MyValue18(int n) {
            foo = n;
        }
    }

    static class MyValue18Holder {
        static MyValue18 v = new MyValue18(rI);
    }

    @Test
    public int test18(int n) {
        if (n == 0) {
            return 0;
        } else {
            return MyValue18Holder.v.foo + n;
        }
    }

    @Run(test = "test18")
    public void test18_verifier(RunInfo info) {
        // Make sure MyValue18Holder is loaded
        MyValue18Holder holder = new MyValue18Holder();
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test18(0);
        } else {
            Asserts.assertEQ(test18(rI), 2*rI);
        }
    }

    // Test case 19:
    // Same as test18 but uninitialized (null) static inline type field
    static final primitive class MyValue19 {
        final int foo;

        MyValue19(int n) {
            foo = n;
        }
    }

    static class MyValue19Holder {
        static MyValue19 v;
    }

    @Test
    public int test19(int n) {
        if (n == 0) {
            return 0;
        } else {
            return MyValue19Holder.v.foo + n;
        }
    }

    @Run(test = "test19")
    public void test19_verifier(RunInfo info) {
        // Make sure MyValue19Holder is loaded
        MyValue19Holder holder = new MyValue19Holder();
        if (info.isWarmUp() && !info.isC2CompilationEnabled()) {
            test19(0);
        } else {
            Asserts.assertEQ(test19(rI), rI);
        }
    }

    // Test case 20:
    // Inline type with object field of unloaded type.
    static class MyObject20 {
        int x = 42;
    }

    static final primitive class MyValue20 {
        MyObject20 obj;

        MyValue20() {
            this.obj = null;
        }
    }

    @Test
    public MyValue20 test20() {
        return new MyValue20();
    }

    @Run(test = "test20")
    public void test20_verifier() {
        MyValue20 vt = test20();
        Asserts.assertEQ(vt.obj, null);
    }

    static primitive class Test21ClassA {
        static Test21ClassB b;
        static Test21ClassC c;
    }

    static primitive class Test21ClassB {
        static int x = Test21ClassA.c.x;
    }

    static primitive class Test21ClassC {
        int x = 42;
    }

    // Test access to static inline type field with unloaded type
    @Test
    public Object test21() {
        return new Test21ClassA();
    }

    @Run(test = "test21")
    public void test21_verifier() {
        Object ret = test21();
        Asserts.assertEQ(Test21ClassA.b.x, 0);
        Asserts.assertEQ(Test21ClassA.c.x, 0);
    }

    static boolean test22FailInit = true;

    static primitive class Test22ClassA {
        int x = 0;
        static Test22ClassB b;
    }

    static primitive class Test22ClassB {
        int x = 0;
        static {
            if (test22FailInit) {
                throw new RuntimeException();
            }
        }
    }

    // Test that load from static field of uninitialized inline type throws an exception
    @Test
    public Object test22() {
        return Test22ClassA.b;
    }

    @Run(test = "test22")
    public void test22_verifier() {
        // Trigger initialization error in Test22ClassB
        try {
            Test22ClassB b = new Test22ClassB();
            throw new RuntimeException("Should have thrown error during initialization");
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // Expected
        }
        try {
            test22();
            throw new RuntimeException("Should have thrown NoClassDefFoundError");
        } catch (NoClassDefFoundError e) {
            // Expected
        }
    }

    static boolean test23FailInit = true;

    static primitive class Test23ClassA {
        int x = 0;
        static Test23ClassB b;
    }

    static primitive class Test23ClassB {
        static {
            if (test23FailInit) {
                throw new RuntimeException();
            }
        }
    }

    // Same as test22 but with empty ClassB
    @Test
    public Object test23() {
        return Test23ClassA.b;
    }

    @Run(test = "test23")
    public void test23_verifier() {
        // Trigger initialization error in Test23ClassB
        try {
            Test23ClassB b = new Test23ClassB();
            throw new RuntimeException("Should have thrown error during initialization");
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // Expected
        }
        try {
            test23();
            throw new RuntimeException("Should have thrown NoClassDefFoundError");
        } catch (NoClassDefFoundError e) {
            // Expected
        }
    }

    static boolean test24FailInit = true;

    static primitive class Test24ClassA {
        Test24ClassB b = Test24ClassB.default;
    }

    static primitive class Test24ClassB {
        int x = 0;
        static {
            if (test24FailInit) {
                throw new RuntimeException();
            }
        }
    }

    // Test that access to non-static field of uninitialized inline type throws an exception
    @Test
    public Object test24() {
        return Test24ClassA.default.b.x;
    }

    @Run(test = "test24")
    public void test24_verifier() {
        // Trigger initialization error in Test24ClassB
        try {
            Test24ClassB b = new Test24ClassB();
            throw new RuntimeException("Should have thrown error during initialization");
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // Expected
        }
        try {
            test24();
            throw new RuntimeException("Should have thrown NoClassDefFoundError");
        } catch (NoClassDefFoundError e) {
            // Expected
        }
    }

    static boolean test25FailInit = true;

    static primitive class Test25ClassA {
        Test25ClassB b = Test25ClassB.default;
    }

    static primitive class Test25ClassB {
        int x = 24;
        static {
            if (test25FailInit) {
                throw new RuntimeException();
            }
        }
    }

    // Same as test24 but with field access outside of test method
    @Test
    public Test25ClassB test25() {
        return Test25ClassA.default.b;
    }

    @Run(test = "test25")
    public void test25_verifier() {
        // Trigger initialization error in Test25ClassB
        try {
            Test25ClassB b = new Test25ClassB();
            throw new RuntimeException("Should have thrown error during initialization");
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // Expected
        }
        try {
            Test25ClassB res = test25();
            Asserts.assertEQ(res.x, 0);
            throw new RuntimeException("Should have thrown NoClassDefFoundError");
        } catch (NoClassDefFoundError e) {
            // Expected
        }
    }

    static boolean test26FailInit = true;

    static primitive class Test26ClassA {
        Test26ClassB b = Test26ClassB.default;
    }

    static primitive class Test26ClassB {
        static {
            if (test26FailInit) {
                throw new RuntimeException();
            }
        }
    }

    // Same as test25 but with empty ClassB
    @Test
    public Object test26() {
        return Test26ClassA.default.b;
    }

    @Run(test = "test26")
    public void test26_verifier() {
        // Trigger initialization error in Test26ClassB
        try {
            Test26ClassB b = new Test26ClassB();
            throw new RuntimeException("Should have thrown error during initialization");
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // Expected
        }
        try {
            test26();
            throw new RuntimeException("Should have thrown NoClassDefFoundError");
        } catch (NoClassDefFoundError e) {
            // Expected
        }
    }
}
