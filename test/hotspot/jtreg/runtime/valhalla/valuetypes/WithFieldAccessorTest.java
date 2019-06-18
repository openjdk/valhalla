/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8210351
 * @summary test nestmate access to an inline type's public, protected and private final fields.
 * @compile -XDemitQtypes -XDallowWithFieldOperator WithFieldAccessorTest.java
 * @run main/othervm WithFieldAccessorTest
 */

// This test is similar to javac's WithFieldAccessorTest but tests nestmate
// access to public, protected, and private final fields in an inline type.
public class WithFieldAccessorTest {

    public static final inline class V {
        public final char c;
        protected final long l;
        private final int i;
        V() {
            this.c = '0';
            this.l = 0;
            this.i = 0;
        }

        public static V make(char c, long l, int i) {
            V v = V.default;
            v = __WithField(v.c, c);
            v = __WithField(v.l, l);
            v = __WithField(v.i, i);
            return v;
        }
    }

    public static void main(String... args) throws Throwable {
        V v = __WithField(V.make('a', 5, 10).c, 'b');
        if (!v.toString().equals("[WithFieldAccessorTest$V c=b l=5 i=10]")) {
            throw new AssertionError("Withfield of 'c' didn't work!" + v.toString());
        }
        v = __WithField(V.make('a', 5, 10).l, 25);
        if (!v.toString().equals("[WithFieldAccessorTest$V c=a l=25 i=10]")) {
            throw new AssertionError("Withfield of 'l' didn't work!" + v.toString());
        }
        v = __WithField(V.make('a', 5, 10).i, 20);
        if (!v.toString().equals("[WithFieldAccessorTest$V c=a l=5 i=20]")) {
            throw new AssertionError("Withfield of 'i' didn't work!" + v.toString());
        }
    }
}
