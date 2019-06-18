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
 * @bug 8222402
 * @summary LW2 array support in javac
 * @run main/othervm ArrayRelationsTest
 */

public inline class ArrayRelationsTest {

    int x = 42;

    public static void main(String [] args) {
        ArrayRelationsTest? [] la = new ArrayRelationsTest?[10];
        ArrayRelationsTest [] qa = new ArrayRelationsTest[10];
        boolean cce = false;
        try {
            qa = (ArrayRelationsTest[]) (Object []) (new String [10]);
        } catch (ClassCastException e) {
            cce = true;
        }
        if (!cce) {
            throw new AssertionError("Missing CCE");
        }
        la = qa;
        ArrayRelationsTest?[] la2 = qa;
        ArrayRelationsTest [] qa2 = (ArrayRelationsTest []) la2;
        boolean npe = false;
        try {
            la2[0] = null;
        } catch (NullPointerException e) {
            npe = true;
        }
        if (!npe) {
            throw new AssertionError("Missing NPE");
        }
        npe = false;
        Object [] oa = qa;
        try {
            oa[0] = null;
        } catch (NullPointerException e) {
            npe = true;
        }
        if (!npe) {
            throw new AssertionError("Missing NPE");
        }

        // round trip;
        Object o = oa = la = qa;
        qa = (ArrayRelationsTest[]) (la = (ArrayRelationsTest? []) (oa = (Object []) o));
        qa [0] = new ArrayRelationsTest();

        npe = false;
        try {
            la[0] = null;
        } catch (NullPointerException e) {
            npe = true;
        }
        if (!npe) {
            throw new AssertionError("Missing NPE");
        }

        la = new ArrayRelationsTest? [10];

        cce = false;
        try {
            qa = (ArrayRelationsTest[]) la;
        } catch (ClassCastException c) {
            cce = true;
        }
        if (!cce) {
            throw new AssertionError("Unexpected CCE behavior");
        }
    }
}
