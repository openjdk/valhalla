/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary basic test for reflection on MVT
 * @run testng/othervm -XX:+EnableMVT MVTReflectionTest
 */

import jdk.incubator.mvt.ValueType;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class MVTReflectionTest {
    private static Class<?> VCC = Point.class;
    private static ValueType<?> VT = ValueType.forClass(VCC);
    private static Class<?> DVT = VT.valueClass();

    public void testValueCapableClass() throws Exception {
        Class<?> pointClass = Class.forName("Point");
        assertEquals(pointClass, VCC);

        // test getSuperClass
        assertEquals(pointClass.getSuperclass(), Object.class);

        // test getFields and getDeclaredFields
        assertTrue(pointClass.getFields().length == 3);
        assertTrue(pointClass.getDeclaredFields().length == 3);
    }

    public void testDerivedValueType() throws Exception {
        // test Class.forName
        try {
            Class<?> c = Class.forName(DVT.getName());
            throw new RuntimeException("should fail to load " + c);
        } catch (ClassNotFoundException e) {}

        Module m = Point.class.getClassLoader().getUnnamedModule();
        Class<?> c = Class.forName(m, DVT.getName());
        assertEquals(c, null);

        // test getSuperClass
        assertEquals(DVT.getSuperclass(), __Value.class);

        // test getFields and getDeclaredFields
        try {
            DVT.getFields();
            throw new RuntimeException("should fail to getFields on " + DVT);
        } catch (UnsupportedOperationException e) {}

        try {
            DVT.newInstance();
            throw new RuntimeException("should fail to newInstance on " + DVT);
        } catch (UnsupportedOperationException e) {}
    }

    public void testValueTypeArray() throws Exception {
        Point[] points = new Point[0];

        Class<?> c1 = Class.forName("[LPoint;");
        Class<?> c2 = VT.arrayValueClass();
        assertEquals(c1, points.getClass());

        String name = "[Q" + DVT.getName() + ";";
        assertEquals(name, c2.getName());

        // cannot load an array DVT class
        try {
            Class.forName(name);
            throw new RuntimeException("should fail to load " + name);
        } catch (ClassNotFoundException e) {}
    }
}
