/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package runtime.valhalla.inlinetypes;

import jdk.test.lib.Asserts;

/*
 * @test VWithFieldTest
 * @summary vwithfield bytecode test
 * @library /test/lib
 * @compile -XDallowWithFieldOperator Point.java
 * @compile -XDallowWithFieldOperator VWithFieldTest.java
 * @run main runtime.valhalla.inlinetypes.VWithFieldTest
 */

public class VWithFieldTest {

    static primitive final class Point {
        final private int x;
        final private int y;

        static Point make(int x, int y) {
            Point p = Point.default;
            Asserts.assertEquals(p.x, 0, "invalid x default value");
            Asserts.assertEquals(p.y, 0, "invalid y default value");
            p = __WithField(p.x, x);
            Asserts.assertEquals(p.x, x, "invalid x value");
            Asserts.assertEquals(p.y, 0, "invalid y value");
            p = __WithField(p.y, y);
            Asserts.assertEquals(p.x, x, "invalid x value");
            Asserts.assertEquals(p.y, y, "invalid y value");
            return p;
        }

        Point () {
            x = 0;
            y = 0;
        }

        Point (int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        static Point setX(Point p, int x) {
            p = __WithField(p.x, x);
            return p;
        }

        public int getY() {
            return y;
        }

        static Point setY(Point p, int y) {
            p = __WithField(p.y, y);
            return p;
        }
    }

    static primitive class TestValue {
        boolean z = false;
        static boolean[] z_values = new boolean[] { false, true, false};
        byte b = 0;
        static byte[] b_values = new byte[] { 0, 125, -111};
        short s = 0;
        static short[] s_values = new short[] { 0, 32654, -31836};
        char c = 0;
        static char[] c_values = new char[] { 0, 1, 65528};
        int i = 0;
        static int[] i_values = new int[] { 0, 2137523847, -2037453241};
        long l = 0;
        static long[] l_values = new long[] { 0, 9123456036854775807L, -9112272036854775507L};
        float f = 0.0f;
        static float[] f_values = new float[] { 0.0f, 1.52758043e7f, -7.93757e-5f};
        double d = 0.0d;
        static double[] d_values = new double[] { 0.0d, 3.304786e9d, -0.7548345e-15d};
        Object o = null;
        static Object[] o_values = new Object[] { null, "Hello", "Duke"};
        Point p = new Point(0, 0);
        static Point[] p_values = new Point[] { new Point(0, 0), new Point(-1, 1), new Point(1, -1)};
        int[] map = new int[10];

        TestValue set_z(int i) {
            TestValue t = __WithField(this.z, z_values[i]);
            t.map[0] = i;
            return t;
        }

        TestValue set_b(int i) {
            TestValue t = __WithField(this.b, b_values[i]);
            t.map[1] = i;
            return t;
        }

        TestValue set_s(int i) {
            TestValue t = __WithField(this.s, s_values[i]);
            t.map[2] = i;
            return t;
        }

        TestValue set_c(int i) {
            TestValue t = __WithField(this.c, c_values[i]);
            t.map[3] = i;
            return t;
        }

        TestValue set_i(int i) {
            TestValue t = __WithField(this.i, i_values[i]);
            t.map[4] = i;
            return t;
        }

        TestValue set_l(int i) {
            TestValue t = __WithField(this.l, l_values[i]);
            t.map[5] = i;
            return t;
        }

        TestValue set_f(int i) {
            TestValue t = __WithField(this.f, f_values[i]);
            t.map[6] = i;
            return t;
        }

        TestValue set_d(int i) {
            TestValue t = __WithField(this.d, d_values[i]);
            t.map[7] = i;
            return t;
        }

        TestValue set_o(int i) {
            TestValue t = __WithField(this.o, o_values[i]);
            t.map[8] = i;
            return t;
        }

        TestValue set_p(int i) {
            TestValue t = __WithField(this.p, p_values[i]);
            t.map[9] = i;
            return t;
        }

        void verify() {
            Asserts.assertEquals(z, z_values[map[0]]);
            Asserts.assertEquals(b, b_values[map[1]]);
            Asserts.assertEquals(s, s_values[map[2]]);
            Asserts.assertEquals(c, c_values[map[3]]);
            Asserts.assertEquals(i, i_values[map[4]]);
            Asserts.assertEquals(l, l_values[map[5]]);
            Asserts.assertEquals(f, f_values[map[6]]);
            Asserts.assertEquals(d, d_values[map[7]]);
            Asserts.assertEquals(o, o_values[map[8]]);
            Asserts.assertEquals(p, p_values[map[9]]);
        }

        static void test() {
            TestValue value = new TestValue();
            value.verify();
            for (int i = 2; i >= 0; i--) {
                value = value.set_z(i);
                value.verify();
                value = value.set_b(i);
                value.verify();
                value = value.set_s(i);
                value.verify();
                value = value.set_c(i);
                value.verify();
                value = value.set_i(i);
                value.verify();
                value = value.set_l(i);
                value.verify();
                value = value.set_f(i);
                value.verify();
                value = value.set_d(i);
                value.verify();
            }
        }
    }

    public static void main(String[] args) {
        creationTest();
        creationTest();
        witherTest();
        witherTest();
        TestValue.test();
    }

    static void creationTest() {
        Point p = Point.make(10,20);
        Asserts.assertEquals(p.x, 10, "invalid x value");
        Asserts.assertEquals(p.y, 20, "invalid y value");
    }

    static void witherTest() {
        Point p1 = Point.make(2,12);
        Asserts.assertEquals(p1.x, 2, "invalid x value");
        Asserts.assertEquals(p1.y, 12, "invalid y value");
        Point p2 = Point.setX(p1,3);
        Asserts.assertEquals(p2.x, 3, "invalid x value");
        Asserts.assertEquals(p2.y, 12, "invalid y value");
        Point p3 = Point.setY(p2, 14);
        Asserts.assertEquals(p3.x, 3, "invalid x value");
        Asserts.assertEquals(p3.y, 14, "invalid y value");
    }

}
