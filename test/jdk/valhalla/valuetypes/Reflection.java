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
 * @compile Point.java
 * @run main/othervm -XX:+EnableValhalla Reflection
 */

import java.lang.reflect.*;

public class Reflection {
    public static void main(String... args) throws Exception {
        Reflection test = new Reflection("Point");
        test.newInstance();
        test.constructor();
        test.accessField();
        test.setAccessible();
        test.trySetAccessible();
        test.staticField();
    }

    private final Class<?> c;
    private final Constructor<?> ctor;
    private final Field field;
    Reflection(String cn) throws Exception {
        this.c = Class.forName(cn);
        if (!c.isValue()) {
            throw new RuntimeException(cn + " is not a value class");
        }

        this.ctor = Point.class.getDeclaredConstructor();
        this.field = c.getField("x");
    }

    void accessField() throws Exception {
        Point o = Point.makePoint(10, 20);
        if (field.getInt(o) != o.x) {
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
        try {
            field.setAccessible(true);
            throw new RuntimeException("InaccessibleObjectException not thrown");
        } catch (InaccessibleObjectException e) { e.printStackTrace(); }
    }

    void trySetAccessible() throws Exception {
        if (ctor.trySetAccessible()) {
            throw new RuntimeException("trySetAccessible should not succeed");
        }
        if (field.trySetAccessible()) {
            throw new RuntimeException("trySetAccessible should not succeed");
        }
    }

    void staticField() throws Exception {
        Field f = Point.class.getDeclaredField("STATIC_FIELD");
        if (f.trySetAccessible()) {
            throw new RuntimeException("trySetAccessible should not succeed");
        }
        try {
            f.setAccessible(true);
            throw new RuntimeException("IllegalAccessException not thrown");
        } catch (InaccessibleObjectException e) { }
    }
}
