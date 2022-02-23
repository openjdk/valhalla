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
 * @test
 * @summary withfield bytecode test
 * @library /test/lib
 * @build org.openjdk.asmtools.* org.openjdk.asmtools.jasm.*
 * @run driver org.openjdk.asmtools.JtregDriver jasm -strict WithFieldTestClasses.jasm
 * @compile Point.java
 * @run main runtime.valhalla.inlinetypes.WithFieldTest
 */

public class WithFieldTest {

    public static void main(String[] args) {
        creationTest();
        creationTest();
        witherTest();
        witherTest();
        allTypesTest();
    }

    static void creationTest() {
        WithFieldPoint p = WithFieldPoint.make(10,20);
        p.checkFields(10, 20);
    }

    static void witherTest() {
        WithFieldPoint p1 = WithFieldPoint.make(2,12);
        p1.checkFields(2, 12);
        WithFieldPoint p2 = p1.withX(3);
        p2.checkFields(3, 12);
        WithFieldPoint p3 = p2.withY(14);
        p3.checkFields(3, 14);
        WithFieldPoint p4 = p1.withY(14);
        p4.checkFields(2, 14);
        WithFieldPoint p5 = p4.withX(3);
        p5.checkFields(3, 14);
    }

    static boolean[] z_values = new boolean[] { false, true, false};
    static byte[] b_values = new byte[] { 0, 125, -111};
    static short[] s_values = new short[] { 0, 32654, -31836};
    static char[] c_values = new char[] { 0, 1, 65528};
    static int[] i_values = new int[] { 0, 2137523847, -2037453241};
    static long[] l_values = new long[] { 0, 9123456036854775807L, -9112272036854775507L};
    static float[] f_values = new float[] { 0.0f, 1.52758043e7f, -7.93757e-5f};
    static double[] d_values = new double[] { 0.0d, 3.304786e9d, -0.7548345e-15d};
    static Object[] o_values = new Object[] { null, "Hello", "Duke"};
    static Point[] p_values = new Point[] { new Point(0, 0), new Point(-1, 1), new Point(1, -1)};

    static void allTypesTest() {
        AllTypes value = AllTypes.default;
        int[] map = new int[10];
        verifyAllTypes(value, map);
        for (int i = 2; i >= 0; i--) {
            value = value.set_z(z_values[map[0] = i]);
            verifyAllTypes(value, map);
            value = value.set_b(b_values[map[1] = i]);
            verifyAllTypes(value, map);
            value = value.set_s(s_values[map[2] = i]);
            verifyAllTypes(value, map);
            value = value.set_c(c_values[map[3] = i]);
            verifyAllTypes(value, map);
            value = value.set_i(i_values[map[4] = i]);
            verifyAllTypes(value, map);
            value = value.set_l(l_values[map[5] = i]);
            verifyAllTypes(value, map);
            value = value.set_f(f_values[map[6] = i]);
            verifyAllTypes(value, map);
            value = value.set_d(d_values[map[7] = i]);
            verifyAllTypes(value, map);
            value = value.set_o(o_values[map[8] = i]);
            verifyAllTypes(value, map);
            value = value.set_p(p_values[map[9] = i]);
            verifyAllTypes(value, map);
        }
    }

    static void verifyAllTypes(AllTypes x, int[] map) {
        Asserts.assertEquals(x.z, z_values[map[0]]);
        Asserts.assertEquals(x.b, b_values[map[1]]);
        Asserts.assertEquals(x.s, s_values[map[2]]);
        Asserts.assertEquals(x.c, c_values[map[3]]);
        Asserts.assertEquals(x.i, i_values[map[4]]);
        Asserts.assertEquals(x.l, l_values[map[5]]);
        Asserts.assertEquals(x.f, f_values[map[6]]);
        Asserts.assertEquals(x.d, d_values[map[7]]);
        Asserts.assertEquals(x.o, o_values[map[8]]);
        Asserts.assertEquals(x.p, p_values[map[9]]);
    }

}
