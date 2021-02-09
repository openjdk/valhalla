/*
 * Copyright (c) 2017, 2020, Red Hat, Inc. All rights reserved.
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
 * @summary Test the handling of arrays of unloaded inline classes.
 * @run main/othervm -Xcomp
 *                   -XX:CompileCommand=compileonly,TestUnloadedInlineTypeArray::test*
 *                   TestUnloadedInlineTypeArray
 * @run main/othervm -Xcomp -XX:FlatArrayElementMaxSize=0
 *                   -XX:CompileCommand=compileonly,TestUnloadedInlineTypeArray::test*
 *                   TestUnloadedInlineTypeArray
 * @run main/othervm -Xcomp
 *                   TestUnloadedInlineTypeArray
 * @run main/othervm -Xcomp -XX:FlatArrayElementMaxSize=0
 *                   TestUnloadedInlineTypeArray
 * @run main/othervm -Xcomp -XX:-TieredCompilation
 *                   -XX:CompileCommand=compileonly,TestUnloadedInlineTypeArray::test*
 *                   TestUnloadedInlineTypeArray
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:FlatArrayElementMaxSize=0
 *                   -XX:CompileCommand=compileonly,TestUnloadedInlineTypeArray::test*
 *                   TestUnloadedInlineTypeArray
 * @run main/othervm -Xcomp -XX:-TieredCompilation
 *                   TestUnloadedInlineTypeArray
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:FlatArrayElementMaxSize=0
 *                   TestUnloadedInlineTypeArray
 */

import jdk.test.lib.Asserts;

final primitive class MyValue1 {
    final int foo;

    private MyValue1() {
        foo = 0x42;
    }
}

final primitive class MyValue1Box {
    final int foo;

    private MyValue1Box() {
        foo = 0x42;
    }
}

final primitive class MyValue2 {
    final int foo;

    public MyValue2(int n) {
        foo = n;
    }
}

final primitive class MyValue2Box {
    final int foo;

    public MyValue2Box(int n) {
        foo = n;
    }
}

final primitive class MyValue3 {
    final int foo;

    public MyValue3(int n) {
        foo = n;
    }
}

final primitive class MyValue3Box {
    final int foo;

    public MyValue3Box(int n) {
        foo = n;
    }
}

final primitive class MyValue4 {
    final int foo;

    public MyValue4(int n) {
        foo = n;
    }
}

final primitive class MyValue4Box {
    final int foo;

    public MyValue4Box(int n) {
        foo = n;
    }
}

final primitive class MyValue5 {
    final int foo;

    public MyValue5(int n) {
        foo = n;
    }
}

final primitive class MyValue6 {
    final int foo;

    public MyValue6(int n) {
        foo = n;
    }

    public MyValue6(MyValue6 v, MyValue6[] dummy) {
        foo = v.foo + 1;
    }
}

final primitive class MyValue6Box {
    final int foo;

    public MyValue6Box(int n) {
        foo = n;
    }

    public MyValue6Box(MyValue6Box v, MyValue6Box.ref[] dummy) {
        foo = v.foo + 1;
    }
}

final primitive class MyValue7 {
    final int foo;

    public MyValue7(int n) {
        foo = n;
    }
}

final primitive class MyValue7Box {
    final int foo;

    public MyValue7Box(int n) {
        foo = n;
    }
}

final primitive class MyValue8 {
    final int foo = 123;
    static {
        TestUnloadedInlineTypeArray.MyValue8_inited = true;
    }
}

final primitive class MyValue9 {
    final int foo = 123;
    static {
        TestUnloadedInlineTypeArray.MyValue9_inited = true;
    }
}

final primitive class MyValue10 {
    final int foo = 42;
}

final primitive class MyValue11 {
    final int foo = 42;
}

public class TestUnloadedInlineTypeArray {
    static boolean MyValue8_inited = false;
    static boolean MyValue9_inited = false;

    static MyValue1[] target1() {
        return new MyValue1[10];
    }

    static void test1() {
        target1();
    }

    static MyValue1Box.ref[] target1Box() {
        return new MyValue1Box.ref[10];
    }

    static void test1Box() {
        target1Box();
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
        for (int i=0; i<n; i++) {
            m = test2(null);
        }
        Asserts.assertEQ(m, 1234);

        MyValue2[] arr = new MyValue2[2];
        arr[1] = new MyValue2(5678);
        m = 9999;
        for (int i=0; i<n; i++) {
            m = test2(arr);
        }
        Asserts.assertEQ(m, 5678);
    }

    static int test2Box(MyValue2Box.ref[] arr) {
        if (arr != null) {
            return arr[1].foo;
        } else {
            return 1234;
        }
    }

    static void verifyTest2Box() {
        int n = 50000;

        int m = 9999;
        for (int i=0; i<n; i++) {
            m = test2Box(null);
        }
        Asserts.assertEQ(m, 1234);

        MyValue2Box.ref[] arr = new MyValue2Box.ref[2];
        arr[1] = new MyValue2Box(5678);
        m = 9999;
        for (int i=0; i<n; i++) {
            m = test2Box(arr);
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

        for (int i=0; i<n; i++) {
            test3(null);
        }

        MyValue3[] arr = new MyValue3[2];
        for (int i=0; i<n; i++) {
            test3(arr);
        }
        Asserts.assertEQ(arr[1].foo, 2345);
    }

    static void test3Box(MyValue3Box.ref[] arr) {
        if (arr != null) {
            arr[0] = null;
            arr[1] = new MyValue3Box(2345);
        }
    }

    static void verifyTest3Box() {
        int n = 50000;

        for (int i=0; i<n; i++) {
            test3Box(null);
        }

        MyValue3Box.ref[] arr = new MyValue3Box.ref[2];
        for (int i=0; i<n; i++) {
            test3Box(arr);
        }
        Asserts.assertEQ(arr[0], null);
        Asserts.assertEQ(arr[1].foo, 2345);
    }

    static MyValue4[] test4(boolean b) {
        // range check elimination
        if (b) {
            MyValue4[] arr = new MyValue4[10];
            arr[1] = new MyValue4(2345);
            return arr;
        } else {
            return null;
        }
    }

    static void verifyTest4() {
        int n = 50000;

        for (int i=0; i<n; i++) {
            test4(false);
        }

        MyValue4[] arr = null;
        for (int i=0; i<n; i++) {
            arr = test4(true);
        }
        Asserts.assertEQ(arr[1].foo, 2345);
    }

    static MyValue4Box.ref[] test4Box(boolean b) {
        // range check elimination
        if (b) {
            MyValue4Box.ref[] arr = new MyValue4Box.ref[10];
            arr[0] = null;
            arr[1] = new MyValue4Box(2345);
            return arr;
        } else {
            return null;
        }
    }

    static void verifyTest4Box() {
        int n = 50000;

        for (int i=0; i<n; i++) {
            test4Box(false);
        }

        MyValue4Box.ref[] arr = null;
        for (int i=0; i<n; i++) {
            arr = test4Box(true);
        }
        Asserts.assertEQ(arr[0], null);
        Asserts.assertEQ(arr[1].foo, 2345);
        arr[3] = null;
    }

    static Object[] test5(int n) {
        if (n == 0) {
            return null;
        } else if (n == 1) {
            MyValue5[] arr = new MyValue5[10];
            arr[1] = new MyValue5(12345);
            return arr;
        } else {
            MyValue5.ref[] arr = new MyValue5.ref[10];
            arr[1] = new MyValue5(22345);
            return arr;
        }
    }

    static void verifyTest5() {
        int n = 50000;

        for (int i=0; i<n; i++) {
            test5(0);
        }

        {
            MyValue5[] arr = null;
            for (int i=0; i<n; i++) {
                arr = (MyValue5[])test5(1);
            }
            Asserts.assertEQ(arr[1].foo, 12345);
        }
        {
            MyValue5.ref[] arr = null;
            for (int i=0; i<n; i++) {
                arr = (MyValue5.ref[])test5(2);
            }
            Asserts.assertEQ(arr[1].foo, 22345);
        }
    }

    static Object test6() {
        return new MyValue6(new MyValue6(123), null);
    }

    static void verifyTest6() {
        Object n = test6();
        Asserts.assertEQ(n.toString(), "[MyValue6 foo=124]");
    }

    static Object test6Box() {
        return new MyValue6Box(new MyValue6Box(123), null);
    }

    static void verifyTest6Box() {
        Object n = test6Box();
        Asserts.assertEQ(n.toString(), "[MyValue6Box foo=124]");
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
        for (int i=0; i<n; i++) {
            m = test7(null);
        }
        Asserts.assertEQ(m, 1234);

        MyValue7[][] arr = new MyValue7[2][2];
        Object[] oa = arr[1];
        Asserts.assertNE(oa[0], null);

        arr[0][1] = new MyValue7(5678);
        m = 9999;
        for (int i=0; i<n; i++) {
            m = test7(arr);
        }
        Asserts.assertEQ(m, 5678);
    }

    static int test7Box(MyValue7Box.ref[][] arr) {
        if (arr != null) {
            arr[0][0] = null;
            return arr[0][1].foo;
        } else {
            return 1234;
        }
    }

    static void verifyTest7Box() {
        int n = 50000;

        int m = 9999;
        for (int i=0; i<n; i++) {
            m = test7Box(null);
        }
        Asserts.assertEQ(m, 1234);

        MyValue7Box.ref[][] arr = new MyValue7Box.ref[2][2];
        Object[] oa = arr[1];
        Asserts.assertEQ(oa[0], null);

        arr[0][1] = new MyValue7Box(5678);
        m = 9999;
        for (int i=0; i<n; i++) {
            m = test7Box(arr);
        }
        Asserts.assertEQ(m, 5678);
        Asserts.assertEQ(arr[0][0], null);
    }

    static void test8() {
        MyValue8.ref a[] = new MyValue8.ref[0];
        Asserts.assertEQ(MyValue8_inited, false);

        MyValue8  b[] = new MyValue8 [0];
        Asserts.assertEQ(MyValue8_inited, true);
    }

    static void test9() {
        MyValue9.ref a[][] = new MyValue9.ref[10][0];
        Asserts.assertEQ(MyValue9_inited, false);

        MyValue9  b[][] = new MyValue9 [10][0];
        Asserts.assertEQ(MyValue9_inited, true);
    }

    static void test10(MyValue10.ref dummy) {
        MyValue10[][] a = new MyValue10[1][1];
        if (a[0][0].equals(null)) throw new RuntimeException("test10 failed");
    }

    static void test11(MyValue10.ref dummy) {
        MyValue11.ref[][] a = new MyValue11.ref[1][1];
        if (a[0][0] != null) throw new RuntimeException("test11 failed");
    }

    static public void main(String[] args) {
        test1();
        test1Box();
        verifyTest2();
        verifyTest2Box();
        verifyTest3();
        verifyTest3Box();
        verifyTest4();
        verifyTest4Box();
        verifyTest5();
        verifyTest6();
        verifyTest6Box();
        verifyTest7();
        verifyTest7Box();
        test8();
        test9();
        test10(null);
        test11(null);
    }
}
