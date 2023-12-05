/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2017, 2023, Red Hat, Inc. All rights reserved.
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
 * @bug 8182997 8214898
 * @library /test/lib
 * @summary Test the handling of arrays of unloaded value classes.
 * @compile --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED
 *          --add-exports java.base/jdk.internal.misc=ALL-UNNAMED TestUnloadedInlineTypeArray.java
 * @run main/othervm -XX:+EnableValhalla
 *                   --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED
 *                   --add-exports java.base/jdk.internal.misc=ALL-UNNAMED
 *                   -Xcomp
 *                   -XX:CompileCommand=compileonly,TestUnloadedInlineTypeArray::test*
 *                   TestUnloadedInlineTypeArray
 * @run main/othervm -XX:+EnableValhalla
 *                   --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED
 *                   --add-exports java.base/jdk.internal.misc=ALL-UNNAMED
 *                   -Xcomp -XX:FlatArrayElementMaxSize=0
 *                   -XX:CompileCommand=compileonly,TestUnloadedInlineTypeArray::test*
 *                   TestUnloadedInlineTypeArray
 * @run main/othervm -XX:+EnableValhalla
 *                   --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED
 *                   --add-exports java.base/jdk.internal.misc=ALL-UNNAMED
 *                   -Xcomp
 *                   TestUnloadedInlineTypeArray
 * @run main/othervm -XX:+EnableValhalla
 *                   --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED
 *                   --add-exports java.base/jdk.internal.misc=ALL-UNNAMED
 *                   -Xcomp -XX:FlatArrayElementMaxSize=0
 *                   TestUnloadedInlineTypeArray
 * @run main/othervm -XX:+EnableValhalla
 *                   --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED
 *                   --add-exports java.base/jdk.internal.misc=ALL-UNNAMED
 *                   -Xcomp -XX:-TieredCompilation
 *                   -XX:CompileCommand=compileonly,TestUnloadedInlineTypeArray::test*
 *                   TestUnloadedInlineTypeArray
 * @run main/othervm -XX:+EnableValhalla
 *                   --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED
 *                   --add-exports java.base/jdk.internal.misc=ALL-UNNAMED
 *                   -Xcomp -XX:-TieredCompilation -XX:FlatArrayElementMaxSize=0
 *                   -XX:CompileCommand=compileonly,TestUnloadedInlineTypeArray::test*
 *                   TestUnloadedInlineTypeArray
 * @run main/othervm -XX:+EnableValhalla
 *                   --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED
 *                   --add-exports java.base/jdk.internal.misc=ALL-UNNAMED
 *                   -Xcomp -XX:-TieredCompilation
 *                   TestUnloadedInlineTypeArray
 * @run main/othervm -XX:+EnableValhalla
 *                   --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED
 *                   --add-exports java.base/jdk.internal.misc=ALL-UNNAMED
 *                   -Xcomp -XX:-TieredCompilation -XX:FlatArrayElementMaxSize=0
 *                   TestUnloadedInlineTypeArray
 */

import jdk.test.lib.Asserts;

import jdk.internal.misc.VM;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;

@ImplicitlyConstructible
@LooselyConsistentValue
value class MyValue1 {
    int foo;

    private MyValue1() {
        foo = 0x42;
    }
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class MyValue2 {
    int foo;

    public MyValue2(int n) {
        foo = n;
    }
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class MyValue3 {
    int foo;

    public MyValue3(int n) {
        foo = n;
    }
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class MyValue4 {
    int foo;

    public MyValue4(int n) {
        foo = n;
    }
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class MyValue5 {
    int foo;

    public MyValue5(int n) {
        foo = n;
    }
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class MyValue6 {
    int foo;

    public MyValue6(int n) {
        foo = n;
    }

    public MyValue6(MyValue6 v, MyValue6[] dummy) {
        foo = v.foo + 1;
    }
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class MyValue7 {
    int foo;

    public MyValue7(int n) {
        foo = n;
    }
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class MyValue8 {
    int foo = 123;
    static {
        TestUnloadedInlineTypeArray.MyValue8_inited = true;
    }
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class MyValue9 {
    int foo = 123;
    static {
        TestUnloadedInlineTypeArray.MyValue9_inited = true;
    }
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class MyValue10 {
    int foo = 42;
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class MyValue11 {
    int foo = 42;
}

public class TestUnloadedInlineTypeArray {
    static boolean MyValue8_inited = false;
    static boolean MyValue9_inited = false;

    static MyValue1[] target1() {
        return (MyValue1[])VM.newNullRestrictedArray(MyValue1.class, 10);
    }

    static void test1() {
        target1();
    }

    static MyValue1[] target1Nullable() {
        return new MyValue1[10];
    }

    static void test1Nullable() {
        target1Nullable();
    }

    static int test2(MyValue2[] arr) {
        if (arr != null) {
            return arr[1].foo;
        } else {
            return 1234;
        }
    }

    static void verifyTest2() {
        int n = 50000;

        int m = 9999;
        for (int i = 0; i < n; i++) {
            m = test2(null);
        }
        Asserts.assertEQ(m, 1234);

        MyValue2[] arr = (MyValue2[])VM.newNullRestrictedArray(MyValue2.class, 2);
        arr[1] = new MyValue2(5678);
        m = 9999;
        for (int i = 0; i < n; i++) {
            m = test2(arr);
        }
        Asserts.assertEQ(m, 5678);
    }

    static int test2Nullable(MyValue2[] arr) {
        if (arr != null) {
            return arr[1].foo;
        } else {
            return 1234;
        }
    }

    static void verifyTest2Nullable() {
        int n = 50000;

        int m = 9999;
        for (int i = 0; i < n; i++) {
            m = test2Nullable(null);
        }
        Asserts.assertEQ(m, 1234);

        MyValue2[] arr = new MyValue2[2];
        arr[1] = new MyValue2(5678);
        m = 9999;
        for (int i = 0; i < n; i++) {
            m = test2Nullable(arr);
        }
        Asserts.assertEQ(m, 5678);
    }

    static void test3(MyValue3[] arr) {
        if (arr != null) {
            arr[1] = new MyValue3(2345);
        }
    }

    static void verifyTest3() {
        int n = 50000;

        for (int i = 0; i < n; i++) {
            test3(null);
        }

        MyValue3[] arr = (MyValue3[])VM.newNullRestrictedArray(MyValue3.class, 2);
        for (int i = 0; i < n; i++) {
            test3(arr);
        }
        Asserts.assertEQ(arr[1].foo, 2345);
    }

    static void test3Nullable(MyValue3[] arr) {
        if (arr != null) {
            arr[0] = null;
            arr[1] = new MyValue3(2345);
        }
    }

    static void verifyTest3Nullable() {
        int n = 50000;

        for (int i = 0; i < n; i++) {
            test3Nullable(null);
        }

        MyValue3[] arr = new MyValue3[2];
        for (int i = 0; i < n; i++) {
            test3Nullable(arr);
        }
        Asserts.assertEQ(arr[0], null);
        Asserts.assertEQ(arr[1].foo, 2345);
    }

    static MyValue4[] test4(boolean b) {
        // range check elimination
        if (b) {
            MyValue4[] arr = (MyValue4[])VM.newNullRestrictedArray(MyValue4.class, 10);
            arr[1] = new MyValue4(2345);
            return arr;
        } else {
            return null;
        }
    }

    static void verifyTest4() {
        int n = 50000;

        for (int i = 0; i < n; i++) {
            test4(false);
        }

        MyValue4[] arr = null;
        for (int i = 0; i < n; i++) {
            arr = test4(true);
        }
        Asserts.assertEQ(arr[1].foo, 2345);
    }

    static MyValue4[] test4Nullable(boolean b) {
        // range check elimination
        if (b) {
            MyValue4[] arr = new MyValue4[10];
            arr[0] = null;
            arr[1] = new MyValue4(2345);
            return arr;
        } else {
            return null;
        }
    }

    static void verifyTest4Nullable() {
        int n = 50000;

        for (int i = 0; i < n; i++) {
            test4Nullable(false);
        }

        MyValue4[] arr = null;
        for (int i = 0; i < n; i++) {
            arr = test4Nullable(true);
        }
        Asserts.assertEQ(arr[0], null);
        Asserts.assertEQ(arr[1].foo, 2345);
        arr[3] = null;
    }

    static Object[] test5(int n) {
        if (n == 0) {
            return null;
        } else if (n == 1) {
            MyValue5[] arr = (MyValue5[])VM.newNullRestrictedArray(MyValue5.class, 10);
            arr[1] = new MyValue5(12345);
            return arr;
        } else {
            MyValue5[] arr = new MyValue5[10];
            arr[1] = new MyValue5(22345);
            return arr;
        }
    }

    static void verifyTest5() {
        int n = 50000;

        for (int i = 0; i < n; i++) {
            test5(0);
        }

        {
            MyValue5[] arr = null;
            for (int i = 0; i < n; i++) {
                arr = (MyValue5[])test5(1);
            }
            Asserts.assertEQ(arr[1].foo, 12345);
        }
        {
            MyValue5[] arr = null;
            for (int i = 0; i < n; i++) {
                arr = (MyValue5[])test5(2);
            }
            Asserts.assertEQ(arr[1].foo, 22345);
        }
    }

    static Object test6() {
        return new MyValue6(new MyValue6(123), null);
    }

    static void verifyTest6() {
        Object n = test6();
        Asserts.assertEQ(n.toString(), "MyValue6@" + Integer.toHexString(n.hashCode()));
    }

    static int test7(MyValue7[][] arr) {
        if (arr != null) {
            return arr[0][1].foo;
        } else {
            return 1234;
        }
    }

    static void verifyTest7() {
        int n = 50000;

        int m = 9999;
        for (int i = 0; i < n; i++) {
            m = test7(null);
        }
        Asserts.assertEQ(m, 1234);

        MyValue7[][] arr = { (MyValue7[])VM.newNullRestrictedArray(MyValue7.class, 2),
                             (MyValue7[])VM.newNullRestrictedArray(MyValue7.class, 2) };
        Object[] oa = arr[1];
        Asserts.assertNE(oa[0], null);

        arr[0][1] = new MyValue7(5678);
        m = 9999;
        for (int i = 0; i < n; i++) {
            m = test7(arr);
        }
        Asserts.assertEQ(m, 5678);
    }

    static int test7Nullable(MyValue7[][] arr) {
        if (arr != null) {
            arr[0][0] = null;
            return arr[0][1].foo;
        } else {
            return 1234;
        }
    }

    static void verifyTest7Nullable() {
        int n = 50000;

        int m = 9999;
        for (int i = 0; i < n; i++) {
            m = test7Nullable(null);
        }
        Asserts.assertEQ(m, 1234);

        MyValue7[][] arr = new MyValue7[2][2];
        Object[] oa = arr[1];
        Asserts.assertEQ(oa[0], null);

        arr[0][1] = new MyValue7(5678);
        m = 9999;
        for (int i = 0; i < n; i++) {
            m = test7Nullable(arr);
        }
        Asserts.assertEQ(m, 5678);
        Asserts.assertEQ(arr[0][0], null);
    }

    static void test8() {
        MyValue8 a[] = new MyValue8[0];
        Asserts.assertEQ(MyValue8_inited, false);

        MyValue8 b[] = (MyValue8[])VM.newNullRestrictedArray(MyValue8.class, 0);
        Asserts.assertEQ(MyValue8_inited, true);
    }

    static void test9() {
        MyValue9 a[][] = new MyValue9[10][0];
        Asserts.assertEQ(MyValue9_inited, false);

        a[0] = (MyValue9[])VM.newNullRestrictedArray(MyValue9.class, 0);
        Asserts.assertEQ(MyValue9_inited, true);
    }

    static void test10(MyValue10 dummy) {
        MyValue10[][] a = { (MyValue10[])VM.newNullRestrictedArray(MyValue10.class, 1) };
        if (a[0][0].equals(null)) throw new RuntimeException("test10 failed");
        Asserts.assertNE(a[0][0], null);
    }

    static void test11(MyValue10 dummy) {
        MyValue11[][] a = new MyValue11[1][1];
        Asserts.assertEQ(a[0][0], null);
    }

    static public void main(String[] args) {
        test1();
        test1Nullable();
        verifyTest2();
        verifyTest2Nullable();
        verifyTest3();
        verifyTest3Nullable();
        verifyTest4();
        verifyTest4Nullable();
        verifyTest5();
        verifyTest6();
        verifyTest7();
        verifyTest7Nullable();
        test8();
        test9();
        test10(null);
        test11(null);
    }
}
