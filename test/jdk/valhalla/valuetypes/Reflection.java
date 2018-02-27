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
 * @summary test reflection on value types
 * @compile -XDenableValueTypes Point.java
 * @run main/othervm -XX:+EnableValhalla Reflection
 */

import java.lang.reflect.*;

public class Reflection {
    public static void main(String... args) throws Exception {
        Reflection test = new Reflection("Point");
        test.newInstance();
        test.accessField();
    }

    private final Class<?> c;
    Reflection(String cn) throws Exception {
        this.c = Class.forName(cn);
        if (!c.isValue()) {
            throw new RuntimeException(cn + " is not a value class");
        }
    }

    void accessField() throws Exception {
        Point o = Point.origin;
        Field x = c.getField("x");
        if (x.getInt(o) != o.x) {
            throw new RuntimeException("Unexpected Point.x value: " +  x.getInt(o));
        }

        try {
            x.setInt(o, 100);
            throw new RuntimeException("IllegalAccessException not thrown");
        } catch (IllegalAccessException e) {}
    }

    void newInstance() throws Exception {
        if (c == null) return;

        try {
            Object o = c.newInstance();
            throw new RuntimeException("newInstance expected to be unsupported on value class");
        } catch (UnsupportedOperationException e) {}
    }
}
