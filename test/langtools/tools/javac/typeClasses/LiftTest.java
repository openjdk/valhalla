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
 * @summary Smoke test for type class generic lifting
 */

public class LiftTest {
    interface Monoid<X> {
        X zero();
        X add(X x1, X x2);
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
    }

    record Box<X>(X x) {
        __witness <Z> Monoid<Box<Z>> monoid(Monoid<Z> monoidZ) {
            return new Monoid<>() {
                public Box<Z> zero() {
                    return new Box<>(monoidZ.zero());
                }
                public Box<Z> add(Box<Z> b1, Box<Z> b2) {
                    return new Box<>(monoidZ.add(b1.x, b2.x));
                }
            };
        }
    }

    public static void main(String[] args) {
        testMonoid(Monoid<Box<MyInt>>.__witness); // implicit/implicit
        testMonoid(Box.monoid(Monoid<MyInt>.__witness)); // explicit/implicit
        testMonoid(Box.monoid(MyInt.MONOID)); // explicit/explicit
    }

    static void testMonoid(Monoid<Box<MyInt>> mm) {
        checkEquals(mm.add(new Box<>(new MyInt(1)), new Box<>(new MyInt(42))), new Box<>(new MyInt(43)));
        checkEquals(mm.zero(), new Box<>(new MyInt(0)));
    }

    static void checkEquals(Object o1, Object o2) {
        if (!o1.equals(o2)) throw new AssertionError();
    }
}
