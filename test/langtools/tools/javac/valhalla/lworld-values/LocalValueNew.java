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
 * @bug 8198749
 * @summary Test value instatiation using new/ctors.
 * @run main/othervm LocalValueNew
 */


public class LocalValueNew {
    int xf = 1234;
    void foo() {
        int xl = 10; int yl = 20;
        final inline class Y {
            final int x;
            final int p = 123456;
            Y() {
                this(123400);
            }

            Y(int x) {
                this.x = x;
            }
            void goo() {
                if (xf + xl + yl + this.x + this.p != 223485)
                    throw new AssertionError("Broken");
            }
        }

        new Y(98765).goo();
    }
    public static void main(String[] args) {
        new LocalValueNew().foo();
    }
}
