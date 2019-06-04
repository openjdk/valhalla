/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @bug 8222792
 * @summary Javac should enforce the latest relationship rules between an inline type and its nullable projection
 * @run main/othervm -XX:+EnableValhalla TypeRelationsTest
 */

public inline class TypeRelationsTest {

    int x = 42;

    static boolean foo(TypeRelationsTest x, TypeRelationsTest? xq, boolean nullPassed) {
        TypeRelationsTest xl;
        TypeRelationsTest? xql;
        boolean npe = false;

        xl = x;
        xl = (TypeRelationsTest) x;
        try {
            xl = (TypeRelationsTest) xq;
        } catch (NullPointerException e) {
            npe = true;
        }

        xql = x;
        xql = (TypeRelationsTest ?) x;
        xql = xq;
        xql = (TypeRelationsTest?) xq;
        return npe;
    }

    static String foo(Object o) {
        return "Object";
    }

    static String foo(TypeRelationsTest x) {
        return "TypeRelationsTest";
    }

    static String foo(TypeRelationsTest? xq) {
        return "TypeRelationsTest?";
    }

    public static void main(String [] args) {
       if (foo(new TypeRelationsTest(), new TypeRelationsTest(), false))
            throw new AssertionError("Unexpected NPE");
       if (!foo(new TypeRelationsTest(), null, true))
            throw new AssertionError("Missing NPE");

       TypeRelationsTest x = new TypeRelationsTest();
       TypeRelationsTest? xq = null;
       if (!foo(x).equals("TypeRelationsTest"))
            throw new AssertionError("Wrong overload");
       if (!foo(xq).equals("TypeRelationsTest?"))
            throw new AssertionError("Wrong overload");
       if (!foo((TypeRelationsTest?) x).equals("TypeRelationsTest?"))
            throw new AssertionError("Wrong overload");

       boolean npe = false;
       try  {
           foo((TypeRelationsTest) xq);
       } catch (NullPointerException e) {
            npe = true;
       }
       if (!npe) {
            throw new AssertionError("Missing NPE");
       }
       xq = x;
       if (!foo((TypeRelationsTest?) xq).equals("TypeRelationsTest?"))
            throw new AssertionError("Wrong overload");
       checkArrays();
    }

    static void checkArrays() {
        TypeRelationsTest [] xa = new TypeRelationsTest[10];
        TypeRelationsTest? [] xqa;
        Object [] oa;
        Object o;

        o = oa = xqa = xa;
        xa = (TypeRelationsTest []) (xqa = (TypeRelationsTest?[]) (oa = (Object []) o));
        xa[0] = new TypeRelationsTest(); // OK, after round trip back and forth.


        xqa = (TypeRelationsTest?[]) xa;
        boolean npe = false;
        try {
            xqa[0] = null;
        } catch (NullPointerException e) {
            npe = true;
        }
        if (!npe) {
           throw new AssertionError("Missing NPE");
        }
        npe = false;

        oa = xa;
        try {
            oa[0] = null;
        } catch (NullPointerException e) {
            npe = true;
        }
        if (!npe) {
           throw new AssertionError("Missing NPE");
        }
    }
}
