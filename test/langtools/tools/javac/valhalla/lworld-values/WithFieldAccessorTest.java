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
 * @bug 8206147
 * @summary WithField operation on a private inner field should be enclosed in a suitable accessor method.
 * @compile -XDallowWithFieldOperator WithFieldAccessorTest.java
 * @run main/othervm -XX:+EnableValhalla WithFieldAccessorTest
 */

public class WithFieldAccessorTest {

    public static final value class V {
        private final int i;
        V() {
            this.i = 0;
        }

        public static V make(int i) {
            V v = V.default;
            v = __WithField(v.i, i);
            return v;
        }
    }

    public static void main(String... args) throws Throwable {
        V v = __WithField(V.make(10).i, 20);
        if (!v.toString().equals("[WithFieldAccessorTest$V i=20]"))
            throw new AssertionError("Withfield didn't work!" + v.toString());
    }
}
