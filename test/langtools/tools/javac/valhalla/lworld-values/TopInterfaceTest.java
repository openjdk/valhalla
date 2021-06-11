/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8237069
 * @summary Introduce and wire-in the new top interfaces
 * @run main/othervm TopInterfaceTest
 */

public class TopInterfaceTest  {

    static class C {}

    static primitive class V {
        int x = 42;
    }

    interface I {
    }

    @interface A {
    }

    public static void main(String args[]) {

        V inln_o = new V();
        C id_o = new C();


        Class<?> [] ca = inln_o.getClass().getInterfaces();
        if (ca.length != 1 || !ca[0].getCanonicalName().equals("java.lang.PrimitiveObject"))
            throw new AssertionError("Found wrong super interfaces");

        // Check that V's super class is Object in class file.
        Class<?> jlo = inln_o.getClass().getSuperclass();
        if (!jlo.getCanonicalName().equals("java.lang.Object"))
            throw new AssertionError("Wrong super type for value type");
        if (jlo.getInterfaces().length != 0)
            throw new AssertionError("Wrong number of super interfaces for jlO");

        if (!(id_o instanceof IdentityObject))
            throw new AssertionError("Expected identity Object");



        // Check that no super interface injection has happened for interfaces.
        if (I.class.getInterfaces().length != 0)
            throw new AssertionError("Found extraneous super interfaces");

        // Check that no super interface injection has happened for annotation types.
        ca = A.class.getInterfaces();
        if (ca.length != 1)
            throw new AssertionError("Found extraneous super interfaces");
        if (!ca[0].getCanonicalName().equals("java.lang.annotation.Annotation"))
            throw new AssertionError("Found wrong super interfaces");
    }
}
