/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test accessing inline type arrays that exceed the flattening threshold.
 * @library /test/lib
 * @run main/othervm -Xbatch TestFlatArrayThreshold
 * @run main/othervm -XX:FlatArrayElementMaxOops=1 -Xbatch TestFlatArrayThreshold
 * @run main/othervm -XX:FlatArrayElementMaxSize=1 -Xbatch TestFlatArrayThreshold
 */

import jdk.test.lib.Asserts;

final primitive class MyValue1 {
    final Object o1;
    final Object o2;

    public MyValue1() {
        o1 = new Integer(42);
        o2 = new Integer(43);
    }
}

public class TestFlatArrayThreshold {

    public static MyValue1 test1(MyValue1[] va, MyValue1 vt) {
        va[0] = vt;
        return va[1];
    }

    public static MyValue1.ref test2(MyValue1.ref[] va, MyValue1.ref vt) {
        va[0] = vt;
        return va[1];
    }

    public static Object test3(Object[] va, MyValue1 vt) {
        va[0] = vt;
        return va[1];
    }

    public static Object test4(Object[] va, MyValue1.ref vt) {
        va[0] = vt;
        return va[1];
    }

    public static MyValue1 test5(MyValue1[] va, Object vt) {
        va[0] = (MyValue1)vt;
        return va[1];
    }

    public static MyValue1.ref test6(MyValue1.ref[] va, Object vt) {
        va[0] = (MyValue1.ref)vt;
        return va[1];
    }

    public static Object test7(Object[] va, Object vt) {
        va[0] = vt;
        return va[1];
    }

    static public void main(String[] args) {
        MyValue1 vt = new MyValue1();
        MyValue1[] va = new MyValue1[2];
        MyValue1.ref[] vaB = new MyValue1.ref[2];
        va[1] = vt;
        for (int i = 0; i < 10_000; ++i) {
            MyValue1 result1 = test1(va, vt);
            Asserts.assertEQ(result1.o1, 42);
            Asserts.assertEQ(result1.o2, 43);

            MyValue1.ref result2 = test2(va, vt);
            Asserts.assertEQ(result2.o1, 42);
            Asserts.assertEQ(result2.o2, 43);
            result2 = test2(vaB, null);
            Asserts.assertEQ(result2, null);

            MyValue1.ref result3 = (MyValue1.ref)test3(va, vt);
            Asserts.assertEQ(result3.o1, 42);
            Asserts.assertEQ(result3.o2, 43);
            result3 = (MyValue1.ref)test3(vaB, vt);
            Asserts.assertEQ(result3, null);

            MyValue1.ref result4 = (MyValue1.ref)test4(va, vt);
            Asserts.assertEQ(result4.o1, 42);
            Asserts.assertEQ(result4.o2, 43);
            result4 = (MyValue1.ref)test4(vaB, null);
            Asserts.assertEQ(result4, null);

            MyValue1 result5 = test5(va, vt);
            Asserts.assertEQ(result5.o1, 42);
            Asserts.assertEQ(result5.o2, 43);

            MyValue1.ref result6 = test6(va, vt);
            Asserts.assertEQ(result6.o1, 42);
            Asserts.assertEQ(result6.o2, 43);
            result6 = test6(vaB, null);
            Asserts.assertEQ(result6, null);

            MyValue1.ref result7 = (MyValue1.ref)test7(va, vt);
            Asserts.assertEQ(result7.o1, 42);
            Asserts.assertEQ(result7.o2, 43);
            result7 = (MyValue1.ref)test7(vaB, null);
            Asserts.assertEQ(result7, null);
        }
        try {
            test2(va, null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException npe) {
            // Expected
        }
        try {
            test4(va, null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException npe) {
            // Expected
        }
        try {
            test5(va, null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException npe) {
            // Expected
        }
        try {
            test6(va, null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException npe) {
            // Expected
        }
        try {
            test7(va, null);
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException npe) {
            // Expected
        }
    }
}
