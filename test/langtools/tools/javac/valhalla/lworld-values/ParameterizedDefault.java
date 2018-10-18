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
 * @bug 8210906
 * @summary [lworld] default value creation should not impose raw types on users.
 * @run main/othervm -XX:+EnableValhalla ParameterizedDefault
 */

public value class ParameterizedDefault<E> {
    E value;
    ParameterizedDefault(E value) { this.value = value; }
    static String foo (Object p) {
        return ("Object version");
    }
    static String foo (String p) {
        return ("String version");
    }
    static String foo (java.util.Date p) {
        return ("Date version");
    }
    public static void main(String [] args) {
        var foo = ParameterizedDefault.default;
        var soo = ParameterizedDefault<String>.default;
        if (!foo(foo.value).equals("Object version") ||
            !foo(soo.value).equals("String version") ||
            !foo(ParameterizedDefault<java.util.Date>.default.value).equals("Date version"))
            throw new AssertionError("Broken");
    }
}
