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

/**
 * @test
 * @library /testlibrary /test/lib /compiler/whitebox /
 * @summary Test the handling of fields of unloaded value classes.
 * @compile hack/GetUnresolvedValueFieldWrongSignature.java
 * @compile TestUnloadedValueTypeField.java
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
 * @run main/othervm/timeout=120 -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                               -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI
 *                               compiler.valhalla.valuetypes.ValueTypeTest
 *                               compiler.valhalla.valuetypes.TestUnloadedValueTypeField
 */

public class TestUnloadedValueTypeField extends compiler.valhalla.valuetypes.ValueTypeTest {
    public static void main(String[] args) throws Throwable {
        TestUnloadedValueTypeField test = new TestUnloadedValueTypeField();
        test.run(args);
    }

    static final String[][] scenarios = {
        {},
        {"-XX:ValueFieldMaxFlatSize=0"}
    };

    @Override
    public int getNumScenarios() {
        return scenarios.length;
    }

    @Override
    public String[] getVMParameters(int scenario) {
        return scenarios[scenario];
    }

    // Test case 1:
    // The value type field class has been loaded, but the holder class has not been loaded.
    //
    //     aload_0
    //     getfield  MyValue1Holder.v:QMyValue1;
    //               ^ not loaded      ^ already loaded
    //
    // MyValue1 has already been loaded, because it's in the ValueType attribute of
    // TestUnloadedValueTypeField, due to TestUnloadedValueTypeField.test1_precondition().
    static final inline class MyValue1 {
        final int foo;

        MyValue1() {
            foo = 1234;
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
    public int test1(MyValue1Holder holder) {
        if (holder != null) {
            return holder.v.foo + 1;
        } else {
            return 0;
        }
    }

    public void test1_verifier(boolean warmup) {
        if (warmup) {
            test1(null);
        } else {
            MyValue1Holder holder = new MyValue1Holder();
            Asserts.assertEQ(test1(holder), 1235);
        }
    }

    // Test case 2:
    // Both the value type field class, and the holder class have not been loaded.
    //
    //     aload_0
    //     getfield  MyValueHolder2.v:QMyValue2;
    //               ^ not loaded     ^ not loaded
    //
    // MyValue2 has not been loaded, because it is not explicitly referenced by
    // TestUnloadedValueTypeField.
    static final inline class MyValue2 {
        final int foo;

        public MyValue2(int n) {
            foo = n;
        }
    }

    static class MyValue2Holder {
        MyValue2 v;

        public MyValue2Holder() {
            v = new MyValue2(1234);
        }
    }


    @Test
    public int test2(MyValue2Holder holder) {
        if (holder != null) {
            return holder.v.foo + 2;
        } else {
            return 0;
        }
    }

    public void test2_verifier(boolean warmup) {
        if (warmup) {
            test2(null);
        } else {
            MyValue2Holder holder2 = new MyValue2Holder();
            Asserts.assertEQ(test2(holder2), 1236);
        }
    }

    // Test case 3: same as test1, except we are using an incorrect signature to
    // refer to the value class.
    // The value type field class has been loaded, but the holder class has not been loaded.
    //
    // GetUnresolvedValueFieldWrongSignature::test3() {
    //     aload_0
    //     getfield  MyValueHolder3.v:LMyValue3;
    //               ^ not loaded    ^ already loaded (but should have been "Q")
    //     ...
    // }
    //
    // MyValue3 has already been loaded, because it's in the ValueType attribute of
    // TestUnloadedValueTypeField, due to TestUnloadedValueTypeField.test3_precondition().
    static final inline class MyValue3 {
        final int foo;

        public MyValue3() {
            foo = 1234;
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
    public int test3(MyValue3Holder holder) {
        return GetUnresolvedValueFieldWrongSignature.test3(holder);
    }

    public void test3_verifier(boolean warmup) {
        if (warmup) {
            test3(null);
        } else {
            MyValue3Holder holder = new MyValue3Holder();
            try {
                test3(holder);
                Asserts.fail("Should have thrown NoSuchFieldError");
            } catch (NoSuchFieldError e) {
                // OK
            }
        }
    }

    // Test case 4:
    // Same as case 1, except we use putfield instead of getfield.
    static final inline class MyValue4 {
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

    static MyValue4 test4_precondition() {
        return new MyValue4(0);
    }

    @Test
    public void test4(MyValue4Holder holder, MyValue4 v) {
        if (holder != null) {
            holder.v = v;
        }
    }

    public void test4_verifier(boolean warmup) {
        MyValue4 v = new MyValue4(5678);
        if (warmup) {
            test4(null, v);
        } else {
            MyValue4Holder holder = new MyValue4Holder();
            test4(holder, v);
            Asserts.assertEQ(holder.v.foo, 5678);
        }
    }

    // Test case 5:
    // Same as case 2, except we use putfield instead of getfield.
    static final inline class MyValue5 {
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
    public void test5(MyValue5Holder holder, Object o) {
        if (holder != null) {
            MyValue5 v = (MyValue5)o;
            holder.v = v;
        }
    }

    public void test5_verifier(boolean warmup) {
        if (warmup) {
            test5(null, null);
        } else {
            MyValue5Holder holder = new MyValue5Holder();
            Object v = holder.make(5679);
            test5(holder, v);
            Asserts.assertEQ(holder.v.foo, 5679);
        }
    }


    // Test case 11: (same as test1, except we use getstatic instead of getfield)
    // The value type field class has been loaded, but the holder class has not been loaded.
    //
    //     getstatic  MyValue11Holder.v:QMyValue1;
    //                ^ not loaded       ^ already loaded
    //
    // MyValue11 has already been loaded, because it's in the ValueType attribute of
    // TestUnloadedValueTypeField, due to TestUnloadedValueTypeField.test1_precondition().
    static final inline class MyValue11 {
        final int foo;

        MyValue11() {
            foo = 1234;
        }
    }

    static class MyValue11Holder {
        static MyValue11 v = new MyValue11();
    }

    static MyValue11 test11_precondition() {
        return new MyValue11();
    }

    @Test
    public int test11(int n) {
        if (n == 0) {
            return 0;
        } else {
            return MyValue11Holder.v.foo + n;
        }
    }

    public void test11_verifier(boolean warmup) {
        if (warmup) {
            test11(0);
        } else {
            Asserts.assertEQ(test11(2), 1236);
        }
    }


    // Test case 12:  (same as test2, except we use getstatic instead of getfield)
    // Both the value type field class, and the holder class have not been loaded.
    //
    //     getstatic  MyValueHolder12.v:QMyValue12;
    //                ^ not loaded       ^ not loaded
    //
    // MyValue12 has not been loaded, because it is not explicitly referenced by
    // TestUnloadedValueTypeField.
    static final inline class MyValue12 {
        final int foo;

        MyValue12(int n) {
            foo = n;
        }
    }

    static class MyValue12Holder {
        static MyValue12 v = new MyValue12(12);
    }

    @Test
    public int test12(int n) {
        if (n == 0) {
            return 0;
        } else {
            return MyValue12Holder.v.foo + n;
        }
    }

    public void test12_verifier(boolean warmup) {
        if (warmup) {
            test12(0);
        } else {
          Asserts.assertEQ(test12(1), 13);
        }
    }
}
