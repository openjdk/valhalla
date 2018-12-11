/*
 * Copyright (c) 2017, 2018, Red Hat, Inc. All rights reserved.
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
 * @summary Test the handling of Arrays of unloaded value classes.
 * @compile -XDemitQtypes -XDenableValueTypes -XDallowFlattenabilityModifiers -XDallowWithFieldOperator TestUnloadedValueTypeArray.java
 * @run main/othervm -XX:+EnableValhalla -Xcomp
 *        -XX:CompileCommand=compileonly,TestUnloadedValueTypeArray::test1
 *        -XX:CompileCommand=compileonly,TestUnloadedValueTypeArray::test2
 *        -XX:CompileCommand=compileonly,TestUnloadedValueTypeArray::test3
 *      TestUnloadedValueTypeArray
 */

import jdk.test.lib.Asserts;

value final class MyValue {
    final int foo;

    private MyValue() {
        foo = 0x42;
    }
}

value final class MyValue2 {
    final int foo;

    private MyValue2() {
        foo = 0x42;
    }
    static MyValue2 make(int n) {
        return __WithField(MyValue2.default.foo, n);
    }
}

value final class MyValue3 {
    final int foo;

    private MyValue3() {
        foo = 0x42;
    }
    static MyValue3 make(int n) {
        return __WithField(MyValue3.default.foo, n);
    }
}



public class TestUnloadedValueTypeArray {

    static MyValue[] target() {
        return new MyValue[10];
    }

    static void test1() {
        target();
    }

    static int test2(MyValue2[] arr) {
        if (arr != null) {
            return arr[1].foo;
        } else {
            return 1234;
        }
    }

    static void test2_verifier() {
        int n = 50000;

        int m = 9999;
        for (int i=0; i<n; i++) {
            m = test2(null);
        }
        Asserts.assertEQ(m, 1234);

        MyValue2[] arr = new MyValue2[2];
        arr[1] = MyValue2.make(5678);
        m = 9999;
        for (int i=0; i<n; i++) {
            m = test2(arr);
        }
        Asserts.assertEQ(m, 5678);
    }

    static void test3(MyValue3[] arr) {
        if (arr != null) {
            arr[1] = MyValue3.make(2345);
        }
    }

    static void test3_verifier() {
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

    static public void main(String[] args) {
        test1();
        test2_verifier();
        test3_verifier();
    }
}
