/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @run main Nest
 * @summary Test substitutability of inner class and anonymous class that
 * has the enclosing instance and possibly other captured outer locals
 */

public interface Nest {
    public static void main(String... args) {
        assertEquals(Nest.of(1, null), Nest.of(1, null));
        assertNotEquals(Nest.of(1, null), Nest.of(2, null));

        Outer n = new Outer(1);
        Outer.Inner inner = n.new Inner(10);
        Outer n1 = new Outer(1);
        Outer n2 = new Outer(2);
        assertEquals(n1.new Inner(10), inner);
        assertEquals(n2.new Inner(10), new Outer(2).new Inner(10));
    }

    // o1.new Inner(1) == o2.new Inner(1) iff o1 == o2
    static primitive class Outer {
        final int i;
        Outer(int i) {
            this.i = i;
        }

        primitive class Inner {
            final int ic;
            Inner(int ic) {
                this.ic = ic;
            }
        }
    }

    String toString();

    static Nest of(int value, Object next) {
        // anonymous class capturing outer locals
        return new primitive Nest() {
            public String toString() {
                return value + " -> " + next;
            }
        };
    }

    static void assertEquals(Object o1, Object o2) {
        if (o1 != o2) {
            throw new RuntimeException(o1 + " != " + o2);
        }
    }
    static void assertNotEquals(Object o1, Object o2) {
        if (o1 == o2) {
            throw new RuntimeException(o1 + " == " + o2);
        }
    }
}
