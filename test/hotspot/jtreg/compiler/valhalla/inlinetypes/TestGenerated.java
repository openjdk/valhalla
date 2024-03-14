/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8260034 8260225 8260283 8261037 8261874 8262128 8262831 8306986
 * @summary A selection of generated tests that triggered bugs not covered by other tests.
 * @enablePreview
 * @compile --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED
 *          --add-exports java.base/jdk.internal.value=ALL-UNNAMED
 *          TestGenerated.java
 * @run main/othervm -XX:+EnableValhalla -Xbatch
 *                   --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED
 *                   --add-exports java.base/jdk.internal.value=ALL-UNNAMED
 *                   compiler.valhalla.inlinetypes.TestGenerated
 * @run main/othervm -XX:+EnableValhalla -Xbatch -XX:FlatArrayElementMaxSize=0
 *                   --add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED
 *                   --add-exports java.base/jdk.internal.value=ALL-UNNAMED
 *                   compiler.valhalla.inlinetypes.TestGenerated
 */

package compiler.valhalla.inlinetypes;

import jdk.internal.value.ValueClass;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.LooselyConsistentValue;
import jdk.internal.vm.annotation.NullRestricted;

@ImplicitlyConstructible
@LooselyConsistentValue
value class EmptyPrimitive {

}

value class EmptyValue {

}

@ImplicitlyConstructible
@LooselyConsistentValue
value class MyValue1 {
    int x = 42;
    int[] array = new int[1];
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class MyValue2 {
    int[] a = new int[1];
    int[] b = new int[6];
    int[] c = new int[5];
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class MyValue3 {
    int[] intArray = new int[1];
    float[] floatArray = new float[1];
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class MyValue4 {
    short b = 2;
    int c = 8;
}

class MyValue4Wrapper {
    public MyValue4 val;

    public MyValue4Wrapper(MyValue4 val) {
        this.val = val;
    }
}

@ImplicitlyConstructible
@LooselyConsistentValue
value class MyValue5 {
    int b = 2;
}

value class MyValue6 {
    int x = 42;
}

public class TestGenerated {
    @NullRestricted
    EmptyPrimitive f1 = new EmptyPrimitive();
    @NullRestricted
    EmptyPrimitive f2 = new EmptyPrimitive();

    void test1(EmptyPrimitive[] array) {
        for (int i = 0; i < 10; ++i) {
            f1 = array[0];
            f2 = array[0];
        }
    }

    MyValue1 test2(MyValue1[] array) {
        MyValue1 res = new MyValue1();
        for (int i = 0; i < array.length; ++i) {
            res = array[i];
        }
        for (int i = 0; i < 1000; ++i) {

        }
        return res;
    }

    void test3(MyValue1[] array) {
        for (int i = 0; i < array.length; ++i) {
            array[i] = new MyValue1();
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
    @NullRestricted
    MyValue1 f4 = new MyValue1();

    void test6() {
        f3 = 123L;
        int res = f4.x;
        if (res != 42) {
            throw new RuntimeException("test6 failed");
        }
    }

    @NullRestricted
    MyValue2 f5;

    void test7(boolean b) {
        MyValue2[] array1 = (MyValue2[])ValueClass.newNullRestrictedArray(MyValue2.class, 6);
        array1[0] = new MyValue2();
        array1[1] = new MyValue2();
        array1[2] = new MyValue2();
        array1[3] = new MyValue2();
        array1[4] = new MyValue2();
        array1[5] = new MyValue2();

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
        MyValue1[] array = (MyValue1[])ValueClass.newNullRestrictedArray(MyValue1.class, 1);
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

    MyValue4[] d = (MyValue4[])ValueClass.newNullRestrictedArray(MyValue4.class, 1);
    @NullRestricted
    MyValue4 e;
    byte f;

    byte test12() {
        MyValue4 i = new MyValue4();
        for (int j = 0; j < 6; ++j) {
            MyValue4[] k = (MyValue4[])ValueClass.newNullRestrictedArray(MyValue4.class, 0);
            if (i.b < 101) {
                i = e;
            }
            for (int l = 0; l < 9; ++l) {
                MyValue4 m = new MyValue4();
                i = m;
            }
        }
        if (d[0].c > 1) {
            for (int n = 0; n < 7; ++n) {
            }
        }
        return f;
    }

    int test13_iField;
    @NullRestricted
    MyValue5 test13_c;
    @NullRestricted
    MyValue5 test13_t;

    void test13(MyValue5[] array) {
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                test13_iField = 6;
            }
            for (int j = 0; j < 2; ++j) {
                test13_iField += array[0].b;
            }
            MyValue5[] array2 = (MyValue5[])ValueClass.newNullRestrictedArray(MyValue5.class, 1);
            test13_c = array[0];
            array2[0] = test13_t;
        }
    }

    void test14(boolean b, MyValue4 val) {
        for (int i = 0; i < 10; ++i) {
            if (b) {
                val = new MyValue4();
            }
            MyValue4[] array = (MyValue4[])ValueClass.newNullRestrictedArray(MyValue4.class, 1);
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
                MyValue4[] array = (MyValue4[])ValueClass.newNullRestrictedArray(MyValue4.class, 1);
                for (int k = 0; k < 10; ++k) {
                    array[0] = val;
                    val = array[0];
                }
            }
        }
    }

    void test16() {
        MyValue4 val = new MyValue4();
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {
                val = (new MyValue4Wrapper(val)).val;
                for (int k = 0; k < 10; ++k) {
                }
            }
        }
    }

    static MyValue6 test17Field = new MyValue6();

    void test17() {
        for (int i = 0; i < 10; ++i) {
            MyValue6 val = new MyValue6();
            for (int j = 0; j < 10; ++j) {
                test17Field = val;
            }
        }
    }

    EmptyValue test18Field;

    EmptyValue test18() {
        EmptyValue val = new EmptyValue();
        test18Field = val;
        return test18Field;
    }

    @NullRestricted
    MyValue1 test19Field = new MyValue1();

    public void test19() {
        for (int i = 0; i < 10; ++i) {
            MyValue1 val = new MyValue1();
            for (int j = 0; j < 10; ++j)
                test19Field = val;
        }
    }

    public static void main(String[] args) {
        TestGenerated t = new TestGenerated();
        EmptyPrimitive[] array1 = (EmptyPrimitive[])ValueClass.newNullRestrictedArray(EmptyPrimitive.class, 1);
        MyValue1[] array2 = (MyValue1[])ValueClass.newNullRestrictedArray(MyValue1.class, 10);
        MyValue1[] array3 = (MyValue1[])ValueClass.newNullRestrictedArray(MyValue1.class, 1);
        array3[0] = new MyValue1();
        MyValue3[] array4 = (MyValue3[])ValueClass.newNullRestrictedArray(MyValue3.class, 1);
        array4[0] = new MyValue3();
        MyValue5[] array5 = (MyValue5[])ValueClass.newNullRestrictedArray(MyValue5.class, 1);
        array5[0] = new MyValue5();
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
            t.test14(false, new MyValue4());
            t.test15();
            // TODO 8325106 Triggers "nothing between inner and outer loop" assert
            // t.test16();
            t.test17();
            t.test18();
            t.test19();
        }
    }
}
