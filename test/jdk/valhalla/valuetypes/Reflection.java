/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test reflection on value types
 * @compile -XDallowWithFieldOperator Point.java Line.java
 * @run main/othervm -XX:+EnableValhalla Reflection
 */

import java.lang.reflect.*;

public class Reflection {
    public static void main(String... args) throws Exception {
        testPointClass();
        testLineClass();
    }

    static void testPointClass() throws Exception  {
        Point o = Point.makePoint(10, 20);
        Reflection test = new Reflection("Point", o);
        test.newInstance();
        test.constructor();
        test.accessFieldX(o.x);
        test.setAccessible();
        test.trySetAccessible();
        test.staticField();
    }

    static void testLineClass() throws Exception {
        Line l = Line.makeLine(10, 20, 30, 40);
        Reflection test = new Reflection("Line", l);
        test.getDeclaredFields();
    }

    private final Class<?> c;
    private final Constructor<?> ctor;
    private final Object o;
    Reflection(String cn, Object o) throws Exception {
        this.c = Class.forName(cn);
        if (!c.isValue()) {
            throw new RuntimeException(cn + " is not a value class");
        }

        this.ctor = c.getDeclaredConstructor();
        this.o = o;
    }

    void accessFieldX(int x) throws Exception {
        Field field = c.getField("x");
        if (field.getInt(o) != x) {
            throw new RuntimeException("Unexpected Point.x value: " +  field.getInt(o));
        }

        try {
            field.setInt(o, 100);
            throw new RuntimeException("IllegalAccessException not thrown");
        } catch (IllegalAccessException e) {}
    }

    void newInstance() throws Exception {
        try {
            Object o = c.newInstance();
            throw new RuntimeException("newInstance expected to be unsupported on value class");
        } catch (IllegalAccessException e) {}
    }

    void constructor() throws Exception {
        try {
            ctor.newInstance();
            throw new RuntimeException("IllegalAccessException not thrown");
        } catch (IllegalAccessException e) { }
    }

    void setAccessible() throws Exception {
        try {
            ctor.setAccessible(true);
            throw new RuntimeException("InaccessibleObjectException not thrown");
        } catch (InaccessibleObjectException e) { e.printStackTrace(); }
        Field field = c.getField("x");
        try {
            field.setAccessible(true);
            throw new RuntimeException("InaccessibleObjectException not thrown");
        } catch (InaccessibleObjectException e) { e.printStackTrace(); }
    }

    void trySetAccessible() throws Exception {
        if (ctor.trySetAccessible()) {
            throw new RuntimeException("trySetAccessible should not succeed");
        }
        Field field = c.getField("x");
        if (field.trySetAccessible()) {
            throw new RuntimeException("trySetAccessible should not succeed");
        }
    }

    void staticField() throws Exception {
        Field f = c.getDeclaredField("STATIC_FIELD");
        if (f.trySetAccessible()) {
            throw new RuntimeException("trySetAccessible should not succeed");
        }
        try {
            f.setAccessible(true);
            throw new RuntimeException("IllegalAccessException not thrown");
        } catch (InaccessibleObjectException e) { }
    }

    void getDeclaredFields() throws Exception {
        for (Field f : c.getDeclaredFields()) {
            System.out.println(f.getName() + " = " + f.get(o));
        }
    }
}
