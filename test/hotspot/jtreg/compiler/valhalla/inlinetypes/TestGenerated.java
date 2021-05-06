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
 * @bug 8260034 8260225 8260283 8261037 8261874 8262128 8262831
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

primitive class MyValue4 {
    short b = 2;
    int c = 8;
}

class MyValue4Wrapper {
    public MyValue4.ref val;

    public MyValue4Wrapper(MyValue4 val) {
        this.val = val;
    }
}

primitive class MyValue5 {
    int b = 2;
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

    MyValue4[] d = {new MyValue4()};
    MyValue4 e;
    byte f;
    byte test12() {
        MyValue4 i = new MyValue4();
        for (int j = 0; j < 6; ++j) {
            MyValue4[] k = {};
            if (i.b < 0101)
                i = e;
            for (int l = 0; l < 9; ++l) {
                MyValue4 m = new MyValue4();
                i = m;
            }
        }
        if (d[0].c > 1)
            for (int n = 0; n < 7; ++n)
                ;
        return f;
    }

    int test13_iField;
    MyValue5 test13_c;
    MyValue5 test13_t;

    void test13(MyValue5[] array) {
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                test13_iField = 6;
            }
            for (int j = 0; j < 2; ++j) {
                test13_iField += array[0].b;
            }
            MyValue5[] array2 = {new MyValue5()};
            test13_c = array[0];
            array2[0] = test13_t;
        }
    }

    void test14(boolean b, MyValue4 val) {
        for (int i = 0; i < 10; ++i) {
            if (b) {
                val = MyValue4.default;
            }
            MyValue4[] array = new MyValue4[1];
            array[0] = val;

            for (int j = 0; j < 5; ++j) {
                for (int k = 0; k < 5; ++k) {
                }
            }
        }
    }

    void test15() {
        MyValue4 val = new MyValue4();
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                MyValue4[] array = new MyValue4[1];
                for (int k = 0; k < 10; ++k) {
                    array[0] = val;
                    val = array[0];
                }
            }
        }
    }

    void test16() {
        MyValue4 val = MyValue4.default;
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                val = (new MyValue4Wrapper(val)).val;
                for (int k = 0; k < 10; ++k) {
                }
            }
        }
    }

    public static void main(String[] args) {
        TestGenerated t = new TestGenerated();
        EmptyValue[] array1 = { new EmptyValue() };
        MyValue1[] array2 = new MyValue1[10];
        MyValue1[] array3 = { new MyValue1() };
        MyValue3[] array4 = { new MyValue3() };
        MyValue5[] array5 = { new MyValue5() };
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
            t.test12();
            t.test13(array5);
            t.test14(false, MyValue4.default);
            t.test15();
            t.test16();
        }
    }
}
