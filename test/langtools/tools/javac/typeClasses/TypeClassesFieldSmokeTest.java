/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Smoke test for type classes and field witnesses
 */

public class TypeClassesFieldSmokeTest {
    interface Monoid<X> {
        X add(X x1, X x2);
        X zero();
    }

    record MyInt(int value) {

        __witness Monoid<MyInt> MONOID = new Monoid<>() {
            public MyInt add(MyInt m1, MyInt m2) {
                return new MyInt(m1.value + m2.value);
            }
            public MyInt zero() {
                return new MyInt(0);
            }
        };
    };

    public static void main(String[] args) {
        testMonoid(Monoid<MyInt>.__witness); // implicit
        testMonoid(MyInt.MONOID); // explicit
    }

    static void testMonoid(Monoid<MyInt> mm) {
        check(mm.add(new MyInt(1), new MyInt(42)).equals(new MyInt(43)));
        check(mm.zero().equals(new MyInt(0)));
    }

    static void check(boolean cond) {
        if (!cond) throw new AssertionError();
    }
}
