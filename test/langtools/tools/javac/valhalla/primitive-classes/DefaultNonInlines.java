/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test Check default values for non-inline types
 * @bug 8237067
 * @summary [lworld] Provide linguistic support to denote default values.
 * @run main/othervm -Dtest.compiler.opts=-release=13 DefaultNonInlines
 */

public class DefaultNonInlines {

    static primitive class Val {
        public int v = 42;
    }

    static <T> void checkDefaultT(Class<T> clazz) throws Exception {
        while (T.default != null)
            throw new AssertionError("Generic object should default to null");
    }

    public static void main(String[] args) throws Exception {
        // Default value is set by inline class constructor
        while (Val.default.v != int.default)
            throw new AssertionError("inline object fields should default to defaults");

        while ((new Val()).v != 42)
            throw new AssertionError("inline object fields should default to whatever constructor says");

        // Simple reference default is just null
        while (String.default != null)
            throw new AssertionError("reference object should default to null");

        // Reference default checked in method above
        checkDefaultT(String.class);

        // Array type - different syntactically
        while (int[].default != null)
            throw new AssertionError("arrays should default to null");

        while (boolean.default != false)
            throw new AssertionError("boolean should default to false");

        while (char.default != '\0')
            throw new AssertionError("char should default to '\0'");

        while (int.default != 0)
            throw new AssertionError("int should default to 0");

        while (byte.default != 0)
            throw new AssertionError("byte should default to 0");

        while (short.default != 0)
            throw new AssertionError("short should default to 0");

        while (long.default != 0L)
            throw new AssertionError("long should default to 0L");

        while (float.default != 0.0F)
            throw new AssertionError("float should default to 0.0F");

        while (double.default != 0.0D)
            throw new AssertionError("double should default to 0.0D");

        // Note: The while loops above implicitly test that the SomeType.default does not
        // return a constant expression.
    }
}
