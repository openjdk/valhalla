/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

import compiler.lib.ir_framework.*;
import jdk.test.lib.Asserts;

import static compiler.valhalla.inlinetypes.InlineTypeIRNode.*;

/*
 * @test
 * @key randomness
 * @summary Test acmp for Inline types
 * @library /test/lib /test/jdk/lib/testlibrary/bytecode /test/jdk/java/lang/invoke/common /
 * @requires (os.simpleArch == "x64" | os.simpleArch == "aarch64")
 * @modules java.base/jdk.internal.value
 * @run main/othervm/timeout=450 -XX:+EnableValhalla compiler.valhalla.inlinetypes.TestAcmp
 */


public class TestAcmp {
    public static value class ManyFieldsValueClass {
        int a;
        short b;
        long c;
        char d;
        double e;
        float f;
        boolean g;
        valuePoint p1;
        identityPoint p2;
        public ManyFieldsValueClass(valuePoint p1, identityPoint p2){
            this.a = 0;
            this.b = 0;
            this.c = 1;
            this.d = 'b';
            this.e = 1.892;
            this.f = 1.35f;
            this.g = true;
            this.p1 = p1;
            this.p2 = p2;
        }
    }

    public static value class valuePoint{
        int x;
        int y;
        public valuePoint(int x, int y){
            this.x = x;
            this.y = y;
        }
    }
    public static class identityPoint{
        int x;
        int y;
        public identityPoint(int x, int y){
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) {
        Scenario[] scenarios = InlineTypes.DEFAULT_SCENARIOS;

        InlineTypes.getFramework()
                .addScenarios(scenarios)
                .start();
    }

    @Test
    @IR(failOn = SUBSTITUTABILITY_TEST)
    public static boolean basic_eq(valuePoint a, valuePoint b) {
        return a == b;
    }

    @Test
    @IR(failOn = SUBSTITUTABILITY_TEST)
    public static boolean basic_ne(valuePoint a, valuePoint b) { return a != b; }

    @Run(test= {"basic_eq", "basic_ne"})
    public void basic_test() {
        int i = 0;
        Asserts.assertTrue(basic_eq(new valuePoint(i, i), new valuePoint(i, i)));
        Asserts.assertFalse(basic_eq(new valuePoint(i, i), new valuePoint(i+1, i)));
        Asserts.assertFalse(basic_eq(new valuePoint(i, i), new valuePoint(i, i+1)));

        Asserts.assertFalse(basic_ne(new valuePoint(i, i), new valuePoint(i, i)));
        Asserts.assertTrue(basic_ne(new valuePoint(i, i), new valuePoint(i+1, i)));
        Asserts.assertTrue(basic_ne(new valuePoint(i, i), new valuePoint(i, i+1)));
    }

    @Test
    @IR(failOn = SUBSTITUTABILITY_TEST)
    public static boolean equal(ManyFieldsValueClass a, ManyFieldsValueClass b) {
        return a == b;
    }

    @Test
    @IR(failOn = SUBSTITUTABILITY_TEST)
    public static boolean notequal(ManyFieldsValueClass a, ManyFieldsValueClass b) {
        return a != b;
    }

    @Run(test = {"equal", "notequal"})
    public void test_many_types() {
        valuePoint a1 = new valuePoint(1, 2);
        valuePoint a2 = new valuePoint(1, 2);
        identityPoint b1 = new identityPoint(1, 2);
        identityPoint b2 = new identityPoint(1, 2);

        Asserts.assertTrue(equal(new ManyFieldsValueClass(a1, b1), new ManyFieldsValueClass(a1, b1)));
        Asserts.assertTrue(equal(new ManyFieldsValueClass(a1, b1), new ManyFieldsValueClass(a2, b1)));
        Asserts.assertFalse(equal(new ManyFieldsValueClass(a1, b1), new ManyFieldsValueClass(a2, b2)));

        Asserts.assertFalse(notequal(new ManyFieldsValueClass(a1, b1), new ManyFieldsValueClass(a1, b1)));
        Asserts.assertFalse(notequal(new ManyFieldsValueClass(a1, b1), new ManyFieldsValueClass(a2, b1)));
        Asserts.assertTrue(notequal(new ManyFieldsValueClass(a1, b1), new ManyFieldsValueClass(a2, b2)));
    }

    public static value class MyValue3{
        int a;
        Object b;
        Object c;
        MyValue3(int a, Object b, Object c){
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    @Test
    public static boolean test2(MyValue3 a, MyValue3 b){
        return a == b;
    }

    @Test
    public static boolean test3(MyValue3 a, MyValue3 b) { return a != b; }

    @Run(test = {"test2", "test3"})
    public void test_object_field() {
        valuePoint a1 = new valuePoint(1, 0);
        valuePoint a2 = new valuePoint(1, 0);
        valuePoint a3 = new valuePoint(1, 0);
        identityPoint b1 = new identityPoint(3, 2);
        identityPoint b2 = new identityPoint(3, 2);
        identityPoint b3 = new identityPoint(3, 2);

        Asserts.assertTrue(test2(new MyValue3(1, a1, a2), new MyValue3(1, a1, a2)));
        Asserts.assertTrue(test2(new MyValue3(1, a1, a2), new MyValue3(1, a1, a3)));
        Asserts.assertTrue(test2(new MyValue3(1, b1, b2), new MyValue3(1, b1, b2)));
        Asserts.assertFalse(test2(new MyValue3(1, b1, b2), new MyValue3(1, b1, b3)));

        Asserts.assertFalse(test3(new MyValue3(1, a1, a2), new MyValue3(1, a1, a2)));
        Asserts.assertFalse(test3(new MyValue3(1, a1, a2), new MyValue3(1, a1, a3)));
        Asserts.assertFalse(test3(new MyValue3(1, b1, b2), new MyValue3(1, b1, b2)));
        Asserts.assertTrue(test3(new MyValue3(1, b1, b2), new MyValue3(1, b1, b3)));
    }

    public static value class MyValue4 {
        int p;
        MyValue4 val;
        public MyValue4(int p, MyValue4 val){
            this.p = p;
            this.val = val;
        }
    }

    @Test
    public static boolean test4(MyValue4 a, MyValue4 b){
        return a == b;
    }

    @Test
    public static boolean test5(MyValue4 a, MyValue4 b) { return a != b; }

    @Run(test = {"test4", "test5"})
    public void test_self_recursive() {
        MyValue4 val1 = null;
        MyValue4 val2 = null;

        for (int i = 0; i < 100; ++i) {
            val1 = new MyValue4(1, val1);
            val2 = new MyValue4(1, val2);
        }
        Asserts.assertTrue(test4(val1, val2));
        Asserts.assertFalse(test5(val1, val2));
    }

    @Test
    public static boolean test6(valuePoint a, valuePoint b){
        return a == b;
    }

    @Test
    public static boolean test7(valuePoint a, valuePoint b) { return a != b; }

    @Run(test = {"test6", "test7"})
    public void test_null() {
        valuePoint val1 = new valuePoint(1, 2);
        valuePoint val2 = new valuePoint(1, 2);

        Asserts.assertTrue(test6(null, null));
        Asserts.assertFalse(test6(val1, null));
        Asserts.assertFalse(test6(null, val1));
        Asserts.assertTrue(test6(val1, val2));
        Asserts.assertFalse(test7(null, null));
        Asserts.assertTrue(test7(val1, null));
        Asserts.assertTrue(test7(null, val1));
        Asserts.assertFalse(test7(val1, val2));
    }

    @Test
    public static boolean test8(ManyFieldsValueClass a, ManyFieldsValueClass b){return a == b;}

    @Test
    public static boolean test9(ManyFieldsValueClass a, ManyFieldsValueClass b) { return a != b; }

    @Run(test = {"test8", "test9"})
    public void test_fields_null() {
        valuePoint val1 = new valuePoint(1, 2);
        identityPoint val2 = new identityPoint(1, 2);
        ManyFieldsValueClass a_null = new ManyFieldsValueClass(null, null);
        ManyFieldsValueClass b_null = new ManyFieldsValueClass(null, null);
        ManyFieldsValueClass a = new ManyFieldsValueClass(val1, val2);

        Asserts.assertTrue(test8(a_null, b_null));
        Asserts.assertFalse(test8(a_null, a));
        Asserts.assertFalse(test8(a, a_null));
        Asserts.assertFalse(test9(a_null, b_null));
        Asserts.assertTrue(test9(a_null, a));
        Asserts.assertTrue(test9(a, a_null));
    }
}



