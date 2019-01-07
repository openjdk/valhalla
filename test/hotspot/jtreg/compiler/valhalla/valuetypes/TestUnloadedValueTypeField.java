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
 * @library /test/lib
 * @summary Test the handling of fields of unloaded value classes.
 * @compile -XDemitQtypes -XDenableValueTypes -XDallowFlattenabilityModifiers -XDallowWithFieldOperator hack/GetUnresolvedValueFieldWrongSignature.java
 * @compile -XDemitQtypes -XDenableValueTypes -XDallowFlattenabilityModifiers -XDallowWithFieldOperator TestUnloadedValueTypeField.java
 * @run main/othervm -XX:+EnableValhalla -Xcomp
 *        -XX:CompileCommand=compileonly,TestUnloadedValueTypeField::test1
 *        -XX:CompileCommand=compileonly,TestUnloadedValueTypeField::test2
 *        -XX:CompileCommand=compileonly,GetUnresolvedValueFieldWrongSignature::test3
 *        -XX:CompileCommand=compileonly,TestUnloadedValueTypeField::test4
 *        -XX:CompileCommand=compileonly,TestUnloadedValueTypeField::test5
 *      TestUnloadedValueTypeField
 */

import jdk.test.lib.Asserts;

public class TestUnloadedValueTypeField {
    static public void main(String[] args) {
        test1_verifier();
        test2_verifier();
        test3_verifier();
        test4_verifier();
        test5_verifier();
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
    static value final class MyValue1 {
        final int foo = 0;

        static MyValue1 make() {
            return __WithField(MyValue1.default.foo, 1234);
        }
    }

    static class MyValue1Holder {
        MyValue1 v;

        public MyValue1Holder() {
            v = MyValue1.make();
        }
    }

    static MyValue1 test1_precondition() {
        return MyValue1.make();
    }

    static int test1(MyValue1Holder holder) {
        if (holder != null) {
            return holder.v.foo + 1;
        } else {
            return 0;
        }
    }

    static void test1_verifier() {
        for (int i=0; i<10000; i++) {
            // Make sure test1() is compiled for the first iteration of this loop,
            // while MyValue1Holder is yet to be loaded.
            test1(null);
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
    // MyValue2 has already been loaded, because it is not explicitly referenced by
    // TestUnloadedValueTypeField.
    static value final class MyValue2 {
        final int foo = 0;

        static MyValue2 make(int n) {
            return __WithField(MyValue2.default.foo, n);
        }
    }

    static class MyValue2Holder {
        MyValue2 v;

        public MyValue2Holder() {
            v = MyValue2.make(1234);
        }
    }


    static int test2(MyValue2Holder holder) {
        if (holder != null) {
            return holder.v.foo + 2;
        } else {
            return 0;
        }
    }

    static void test2_verifier() {
        for (int i=0; i<10000; i++) {
            // Make sure test2() is compiled for the first iteration of this loop,
            // while MyValue2Holder2 and MyValue2  is yet to be loaded.
            test2(null);
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
    static value final class MyValue3 {
        final int foo = 0;

        static MyValue3 make() {
            return __WithField(MyValue3.default.foo, 1234);
        }
    }

    static class MyValue3Holder {
        MyValue3 v;

        public MyValue3Holder() {
            v = MyValue3.make();
        }
    }

    static MyValue3 test3_precondition() {
        return MyValue3.make();
    }

    static int test3(MyValue3Holder holder) {
        return GetUnresolvedValueFieldWrongSignature.test3(holder);
    }

    static void test3_verifier() {
        for (int i=0; i<10000; i++) {
            // Make sure test3() is compiled for the first iteration of this loop,
            // while MyValue3Holder is yet to be loaded.
            test3(null);
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
    static value final class MyValue4 {
        final int foo = 0;

        static MyValue4 make(int n) {
            return __WithField(MyValue4.default.foo, n);
        }
    }

    static class MyValue4Holder {
        MyValue4 v;

        public MyValue4Holder() {
            v = MyValue4.make(0);
        }
    }

    static MyValue4 test4_precondition() {
        return MyValue4.make(0);
    }

    static void test4(MyValue4Holder holder, MyValue4 v) {
        if (holder != null) {
            holder.v = v;
        }
    }

    static void test4_verifier() {
        MyValue4 v = MyValue4.make(5678);
        for (int i=0; i<10000; i++) {
            // Make sure test4() is compiled for the first iteration of this loop,
            // while MyValue4Holder is yet to be loaded.
            test4(null, v);
            MyValue4Holder holder = new MyValue4Holder();
            test4(holder, v);
            Asserts.assertEQ(holder.v.foo, 5678);
        }
    }

    // Test case 5:
    // Same as case 2, except we use putfield instead of getfield.
    static value final class MyValue5 {
        final int foo = 0;

        static MyValue5 make(int n) {
            return __WithField(MyValue5.default.foo, n);
        }
    }

    static class MyValue5Holder {
        MyValue5 v;

        public MyValue5Holder() {
            v = MyValue5.make(0);
        }
        public Object make(int n) {
            return MyValue5.make(n);
        }
    }

    static void test5(MyValue5Holder holder, Object o) {
        if (holder != null) {
            MyValue5 v = (MyValue5)o;
            holder.v = v;
        }
    }

    static void test5_verifier() {
        for (int i=0; i<10000; i++) {
            // Make sure test5() is compiled for the first iteration of this loop,
            // while both MyValue5Holder and MyValye5 are yet to be loaded.
            test5(null, null);
            MyValue5Holder holder = new MyValue5Holder();
            Object v = holder.make(5679);
            test5(holder, v);
            Asserts.assertEQ(holder.v.foo, 5679);
        }
    }
}
