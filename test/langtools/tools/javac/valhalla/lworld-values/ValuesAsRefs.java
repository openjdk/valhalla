/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test that values code like a class - i.e are accepted in some places where only references used be, when invoked with the experimental mode -XDallowGenericsOverValues
   @compile  ValuesAsRefs.java
 * @run main/othervm -XX:+EnableValhalla ValuesAsRefs
 */
import java.util.ArrayList;

public final inline class ValuesAsRefs {

    final ArrayList<? extends ValuesAsRefs?> ao = null; // values can be wildcard bounds.

    final inline class I implements java.io.Serializable {
        final int y = 42;
    }

    void foo() {
        I i = this.new I();  // values can be enclosing instances.
        i = ValuesAsRefs.I.default;
        Object o = (I? & java.io.Serializable) i; // values can be used in intersection casts
    }
    <T> void goo() {
        this.<ValuesAsRefs?>goo(); // values can be type arguments to generic method calls
    }

    public static void main(String [] args) {
        Object o = null;
        ArrayList<ValuesAsRefs.I?> aloi = new ArrayList<>(); // values can be type arguments.
        boolean OK = false;
        try {
            aloi.add((ValuesAsRefs.I) o);
        } catch (NullPointerException npe) {
            OK = true;
        }
        if (!OK)
            throw new AssertionError("Missing NPE");
    }
}
