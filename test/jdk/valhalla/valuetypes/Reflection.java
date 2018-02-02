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
 * @summary test Class.forName on value types
 * @compile -XDenableValueTypes Point.java
 * @compile --add-modules jdk.incubator.mvt PersonVcc.java
 * @run main/othervm -XX:+EnableValhalla Reflection valhalla
 * @run main/othervm -XX:+EnableMVT Reflection MVT
 */

public class Reflection {
    public static void main(String... args) throws Exception {
        Reflection test = args[0].equals("valhalla")
                              ? new Reflection("Point", true)
                              : new Reflection("PersonVcc$Value", false);
        test.testNewInstance();
    }

    private final Class<?> c;
    private final boolean fullValueType;
    Reflection(String cn, boolean fullValueType) {
        this.fullValueType = fullValueType;
        Class<?> vcc = null;
        try {
            vcc = Class.forName(cn);
            System.out.format("loaded %s %s%n", vcc.getName(), vcc.getSuperclass().getName());
            if (!fullValueType)
                throw new RuntimeException("ClassNotFoundException not thrown: " + cn);;
        } catch (ClassNotFoundException e) {
            if (fullValueType)
                throw new RuntimeException(cn + " not found");;
        }
        this.c = vcc;
    }

    void testNewInstance() throws Exception {
        if (c == null) return;

        try {
            c.newInstance();
            throw new RuntimeException("Reflection expected to be unsupported");
        } catch (InstantiationException e) {}
    }
}
