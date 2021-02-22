/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8260034 8260225 8260283 8261037 8261874 8262128
 * @summary Generated inline type tests.
 * @run main/othervm -Xbatch
 *                   compiler.valhalla.inlinetypes.TestGenerated
 * @run main/othervm -Xbatch -XX:FlatArrayElementMaxSize=0
 *                   compiler.valhalla.inlinetypes.TestGenerated
 */

package compiler.valhalla.inlinetypes;

primitive class EmptyValue {

}

primitive class MyValue1 {
    int x = 42;
    int[] array = new int[1];
}

primitive class MyValue2 {
    int[] a = new int[1];
    int[] b = new int[6];
    int[] c = new int[5];
}

primitive class MyValue3 {
    int[] intArray = new int[1];
    float[] floatArray = new float[1];
}

public class TestGenerated {
    EmptyValue f1 = new EmptyValue();
    EmptyValue f2 = new EmptyValue();

    void test1(EmptyValue[] array) {
        for (int i = 0; i < 10; ++i) {
            f1 = array[0];
            f2 = array[0];
        }
    }

    MyValue1 test2(MyValue1[] array) {
        MyValue1 res = MyValue1.default;
        for (int i = 0; i < array.length; ++i) {
            res = array[i];
        }
        for (int i = 0; i < 1000; ++i) {

        }
        return res;
    }

    void test3(MyValue1[] array) {
        for (int i = 0; i < array.length; ++i) {
            array[i] = MyValue1.default;
        }
        for (int i = 0; i < 1000; ++i) {

        }
    }

    void test4(MyValue1[] array) {
        array[0].array[0] = 0;
    }

    int test5(MyValue1[] array) {
        return array[0].array[0];
    }

    long f3;
    MyValue1 f4 = new MyValue1();

    void test6() {
        f3 = 123L;
        int res = f4.x;
        if (res != 42) {
            throw new RuntimeException("test6 failed");
        }
    }

    MyValue2 f5;

    void test7(boolean b) {
        MyValue2[] array1 = {new MyValue2(), new MyValue2(), new MyValue2(),
                             new MyValue2(), new MyValue2(), new MyValue2()};
        MyValue2 h = new MyValue2();
        MyValue2 n = new MyValue2();
        int[] array2 = new int[1];

        for (int i = 0; i < 10; ++i) {
          for (int j = 0; j < 10; ++j) {
            array1[0] = array1[0];
            if (i == 1) {
              h = h;
              array2[0] *= 42;
            }
          }
        }
        if (b) {
          f5 = n;
        }
    }

    boolean test8(MyValue1[] array) {
        return array[0].array == array[0].array;
    }

    void test9(boolean b) {
        MyValue1[] array = { new MyValue1() };
        if (b) {
            for (int i = 0; i < 10; ++i) {
                if (array != array) {
                    array = null;
                }
            }
        }
    }

    int[] f6 = new int[1];

    void test10(MyValue3[] array) {
        float[] floatArray = array[0].floatArray;
        if (f6 == f6) {
            f6 = array[0].intArray;
        }
    }

    void test11(MyValue3[] array) {
        float[] floatArray = array[0].floatArray;
        if (array[0].intArray[0] != 42) {
            throw new RuntimeException("test11 failed");
        }
    }

    public static void main(String[] args) {
        TestGenerated t = new TestGenerated();
        EmptyValue[] array1 = { new EmptyValue() };
        MyValue1[] array2 = new MyValue1[10];
        MyValue1[] array3 = { new MyValue1() };
        MyValue3[] array4 = { new MyValue3() };
        array4[0].intArray[0] = 42;

        for (int i = 0; i < 50_000; ++i) {
            t.test1(array1);
            t.test2(array2);
            t.test3(array2);
            t.test4(array3);
            t.test5(array3);
            t.test6();
            t.test7(false);
            t.test8(array3);
            t.test9(true);
            t.test10(array4);
            t.test11(array4);
        }
    }
}
