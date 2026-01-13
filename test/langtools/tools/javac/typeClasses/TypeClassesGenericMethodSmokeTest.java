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
 * @summary Smoke test for type classes and generic method witnesses
 */

public class TypeClassesGenericMethodSmokeTest {
    interface Convertible<A, B> {
        B convertTo(A a);
    }

    record Box<X>(X x) {
        __witness <Z> Convertible<Box<Z>, Z> convBox() {
            return box -> box.x();
        }
    }

    public static void main(String[] args) {
        testConversion(42, Convertible<Box<Integer>, Integer>.__witness); // implicit
        testConversion(42, Box.convBox()); // explicit
        testConversion(new Box<>(42), Convertible<Box<Box<Integer>>, Box<Integer>>.__witness); // implicit
        testConversion(new Box<>(42), Box.convBox()); // explicit
        testConversion(new Box<>(new Box<>(42)), Convertible<Box<Box<Box<Integer>>>, Box<Box<Integer>>>.__witness); // implicit
        testConversion(new Box<>(new Box<>(42)), Box.convBox()); // explicit
    }

    static <Z> void testConversion(Z expected, Convertible<Box<Z>, Z> convertible) {
        checkEquals(expected, convertible.convertTo(new Box<>(expected)));
    }

    static void checkEquals(Object o1, Object o2) {
        if (!o1.equals(o2)) throw new AssertionError();
    }
}
