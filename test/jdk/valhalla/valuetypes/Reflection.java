/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test reflection on primitive classes
 * @compile --enable-preview --source ${jdk.version} Reflection.java
 * @run testng/othervm --enable-preview Reflection
 */

import java.lang.constant.ClassDesc;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class Reflection {
    @Test
    public static void testPointClass() throws Exception  {
        Object o = Point.class.newInstance();
        assertEquals(o.getClass(), Point.class.asPrimaryType());

        Constructor<?> ctor = Point.class.getDeclaredConstructor(int.class, int.class);
        o = ctor.newInstance(20, 30);
        assertEquals(o.getClass(), Point.class.asPrimaryType());

        Field field = Point.class.getField("x");
        if (field.getInt(o) != 20) {
            fail("Unexpected Point.x value: " +  field.getInt(o));
        }
        try {
            field.setInt(o, 100);
            fail("IllegalAccessException not thrown");
        } catch (IllegalAccessException e) {}

        // final static field in a primitive class
        Field f = Point.class.getDeclaredField("STATIC_FIELD");
        assertTrue(f.getType() == Object.class);
        // accessible but no write access
        f.trySetAccessible();
        assertTrue(f.isAccessible());
        checkToString(f);
    }

    @Test
    public static void testLineClass() throws Exception {
        checkInstanceField(Line.class, "p1", Point.class);
        checkInstanceField(Line.class, "p2", Point.class);
        checkInstanceMethod(Line.class, "p1", Point.class);
        checkInstanceMethod(Line.class, "p2", Point.class);
    }

    @Test
    public static void testNonFlattenValue() throws Exception {
        checkInstanceField(NonFlattenValue.class, "nfp", Point.ref.class);
        checkInstanceMethod(NonFlattenValue.class, "pointValue", Point.class);
        checkInstanceMethod(NonFlattenValue.class, "point", Point.ref.class);
        checkInstanceMethod(NonFlattenValue.class, "has", boolean.class, Point.class, Point.ref.class);
    }


    static void checkInstanceField(Class<?> declaringClass, String name, Class<?> type) throws Exception {
        Field f = declaringClass.getDeclaredField(name);
        assertTrue(f.getType() == type);
        checkToString(f);
    }

    static void checkInstanceMethod(Class<?> declaringClass,String name, Class<?> returnType, Class<?>... params) throws Exception {
        Method m = declaringClass.getDeclaredMethod(name, params);
        assertTrue(m.getReturnType() == returnType);
        checkToString(m);
    }

    static void checkToString(Field f) {
        StringBuilder sb = new StringBuilder();
        int mods = f.getModifiers();
        if (Modifier.isPublic(mods)) {
            sb.append("public").append(" ");
        }
        if (Modifier.isStatic(mods)) {
            sb.append("static").append(" ");
        }
        if (Modifier.isFinal(mods)) {
            sb.append("final").append(" ");
        }
        sb.append(displayName(f.getType())).append(" ");
        sb.append(f.getDeclaringClass().getName()).append(".").append(f.getName());
        assertEquals(f.toString(), sb.toString());
    }

    static void checkToString(Method m) {
        StringBuilder sb = new StringBuilder();
        int mods = m.getModifiers();
        if (Modifier.isPublic(mods)) {
            sb.append("public").append(" ");
        }
        if (Modifier.isStatic(mods)) {
            sb.append("static").append(" ");
        }
        if (Modifier.isFinal(mods)) {
            sb.append("final").append(" ");
        }
        sb.append(displayName(m.getReturnType())).append(" ");
        sb.append(m.getDeclaringClass().getName()).append(".").append(m.getName());
        sb.append("(");
        int count = m.getParameterCount();
        for (Class<?> ptype : m.getParameterTypes()) {
            sb.append(displayName(ptype));
            if (--count > 0) {
                sb.append(",");
            }
        }
        sb.append(")");
        assertEquals(m.toString(), sb.toString());
    }

    static String displayName(Class<?> type) {
        if (type.isPrimitive()) {
            ClassDesc classDesc = type.describeConstable().get();
            return classDesc.displayName();
        }
        return type.getTypeName();
    }
}
