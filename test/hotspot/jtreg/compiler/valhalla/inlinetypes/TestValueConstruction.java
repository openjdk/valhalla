/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package compiler.valhalla.inlinetypes;

import java.lang.invoke.*;
import java.lang.reflect.Method;

import jdk.test.lib.Asserts;

/**
 * @test TestValueConstruction
 * @summary Test construction of value objects.
 * @library /testlibrary /test/lib /
 * @enablePreview
 * @run main/othervm -XX:+EnableValhalla
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -XX:+EnableValhalla
 *                   -XX:CompileCommand=compileonly,*TestValueConstruction::test* -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -XX:+EnableValhalla
 *                   -XX:CompileCommand=dontinline,*TestValueConstruction::<init> -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -XX:+EnableValhalla
 *                   -XX:CompileCommand=dontinline,*Object::<init> -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -XX:+EnableValhalla
 *                   -XX:CompileCommand=dontinline,*MyAbstract::<init> -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 *
 * @run main/othervm -XX:+EnableValhalla -XX:-TieredCompilation -XX:+StressIncrementalInlining
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -XX:+EnableValhalla -XX:-TieredCompilation -XX:+StressIncrementalInlining
 *                   -XX:CompileCommand=compileonly,*TestValueConstruction::test* -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -XX:+EnableValhalla -XX:-TieredCompilation -XX:+StressIncrementalInlining
 *                   -XX:CompileCommand=dontinline,*TestValueConstruction::<init> -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -XX:+EnableValhalla -XX:-TieredCompilation -XX:+StressIncrementalInlining
 *                   -XX:CompileCommand=dontinline,*Object::<init> -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 * @run main/othervm -XX:+EnableValhalla -XX:-TieredCompilation -XX:+StressIncrementalInlining
 *                   -XX:CompileCommand=dontinline,*MyAbstract::<init> -Xbatch
 *                   compiler.valhalla.inlinetypes.TestValueConstruction
 */

// TODO 8325106 Add -XX:+DeoptimizeALot and tests with constructor invocations from constructor

public class TestValueConstruction {

    static interface MyInterface {

    }

    static value class MyValue1 implements MyInterface {
        int x;

        public MyValue1(int x) {
            this.x = x;
        }
    }

    static abstract value class MyAbstract { }

    static value class MyValue2 extends MyAbstract {
        int x;

        public MyValue2(int x) {
            this.x = x;
        }
    }

    public static int test1(int i) {
        MyValue1 val = new MyValue1(i);
        return val.x;
    }

    public static MyValue1 helper1(int i) {
        return new MyValue1(i);
    }

    public static Object test2(int i) {
        return helper1(i);
    }

    public static Object test3(int limit) {
        MyValue1 res = null;
        for (int i = 0; i <= 10; ++i) {
            res = new MyValue1(i);
        }
        return res;
    }

    public static MyValue1 test4(int i) {
        MyValue1 v = new MyValue1(i);
        v = new MyValue1(i);
        return v;
    }

    public static int test5(int i) {
        MyValue2 val = new MyValue2(i);
        return val.x;
    }

    public static MyValue2 helper2(int i) {
        return new MyValue2(i);
    }

    public static Object test6(int i) {
        return helper2(i);
    }

    public static Object test7(int limit) {
        MyValue2 res = null;
        for (int i = 0; i <= 10; ++i) {
            res = new MyValue2(i);
        }
        return res;
    }

    public static MyValue2 test8(int i) {
        MyValue2 v = new MyValue2(i);
        v = new MyValue2(i);
        return v;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 50_000; ++i) {
            Asserts.assertEQ(test1(i),i);
            Asserts.assertEQ(test2(i), new MyValue1(i));
            Asserts.assertEQ(test3(10), new MyValue1(10));
            Asserts.assertEQ(test4(i), new MyValue1(i));
            Asserts.assertEQ(test5(i), i);
            Asserts.assertEQ(test6(i), new MyValue2(i));
            Asserts.assertEQ(test7(10), new MyValue2(10));
            Asserts.assertEQ(test8(i), new MyValue2(i));
        }
    }
}
