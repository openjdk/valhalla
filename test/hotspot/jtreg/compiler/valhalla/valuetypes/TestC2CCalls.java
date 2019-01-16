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
 * @summary Test value type calling convention with compiled to compiled calls.
 * @run main/othervm -XX:+EnableValhalla -XX:-UseBimorphicInlining -Xbatch
 *                   -XX:CompileCommand=compileonly,TestC2CCalls*::test*
 *                   -XX:CompileCommand=dontinline,TestC2CCalls*::test*
 *                   TestC2CCalls
 */

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;

public class TestC2CCalls {

    public static final int rI = Utils.getRandomInstance().nextInt() % 1000;

    static value class OtherVal {
        public final int x;

        private OtherVal(int x) {
            this.x = x;
        }
    }

    static interface MyInterface {
        public int test1(OtherVal other, int y);
        public int test2(OtherVal.val other1, OtherVal.box other2, int y);
    }

    static value class MyValue implements MyInterface {
        public final int x;

        private MyValue(int x) {
            this.x = x;
        }

        @Override
        public int test1(OtherVal other, int y) {
            return x + other.x + y;
        }

        @Override
        public int test2(OtherVal.val other1, OtherVal.box other2, int y) {
            return x + other1.x + other2.x + y;
        }
    }

    static class MyObject implements MyInterface {
        private final int x;

        private MyObject(int x) {
            this.x = x;
        }

        @Override
        public int test1(OtherVal other, int y) {
            return x + other.x + y;
        }

        @Override
        public int test2(OtherVal.val other1, OtherVal.box other2, int y) {
            return x + other1.x + other2.x + y;
        }
    }

    // Test calling methods with value type arguments through an interface
    public static int test1(MyInterface intf, OtherVal other, int y) {
        return intf.test1(other, y);
    }

    public static int test2(MyInterface intf, OtherVal other, int y) {
        return intf.test2(other, other, y);
    }

    // Test mixing null-tolerant and null-free value type arguments
    public static int test3(MyValue vt, OtherVal other, int y) {
        return vt.test2(other, other, y);
    }

    public static int test4(MyObject obj, OtherVal other, int y) {
        return obj.test2(other, other, y);
    }

    public static void main(String[] args) {
        MyValue val = new MyValue(rI);
        OtherVal other = new OtherVal(rI+1);
        MyObject obj = new MyObject(rI+2);

        // Make sure callee methods are compiled
        for (int i = 0; i < 10_000; ++i) {
            Asserts.assertEQ(val.test1(other, rI), 3*rI+1);
            Asserts.assertEQ(obj.test1(other, rI), 3*rI+3);
            Asserts.assertEQ(val.test2(other, other, rI), 4*rI+2);
            Asserts.assertEQ(obj.test2(other, other, rI), 4*rI+4);
        }

        for (int i = 0; i < 100_000; ++i) {
            Asserts.assertEQ(test1(val, other, rI), 3*rI+1);
            Asserts.assertEQ(test1(obj, other, rI), 3*rI+3);
            Asserts.assertEQ(test2(val, other, rI), 4*rI+2);
            Asserts.assertEQ(test2(obj, other, rI), 4*rI+4);
            Asserts.assertEQ(test3(val, other, rI), 4*rI+2);
            Asserts.assertEQ(test4(obj, other, rI), 4*rI+4);
        }
    }
}
