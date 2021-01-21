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
 * @bug 8260034
 * @summary Generated inline type tests.
 * @run main/othervm -Xbatch compiler.valhalla.inlinetypes.TestGenerated
 */

package compiler.valhalla.inlinetypes;

inline class EmptyValue {

}

inline class MyValue1 {
    int x = 42;
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

    public static void main(String[] args) {
        TestGenerated t = new TestGenerated();
        EmptyValue[] array1 = { new EmptyValue() };
        MyValue1[] array2 = new MyValue1[10];

        for (int i = 0; i < 50_000; ++i) {
            t.test1(array1);
            t.test2(array2);
            t.test3(array2);
        }
    }
}
