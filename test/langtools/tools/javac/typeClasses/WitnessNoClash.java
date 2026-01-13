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
 * @summary Smoke test for witness declaration clashes
 * @compile WitnessNoClash.java
 */
class WitnessNoClash {
    interface T<X> { }

    interface U<X> extends T<X> { }
    interface W<X> extends T<X> { }

    static class K { }

    // witness field only
    static class A {
        __witness U<A> U = null; // ok, common super with W (T<A>)
        __witness W<A> W = null; // ok, common super with U (T<A>)
    }

    // witness method only
    static class B {
        __witness U<B> U() { return null; } // ok, common super with W (T<B>)
        __witness W<B> W() { return null; } // ok, common super with U (T<B>)
    }

    // witness field and method
    static class C {
        __witness U<C> U = null; // ok, common super with W (T<C>)
        __witness W<C> W() { return null; } // ok, common super with U (T<C>)
    }

    // generic vs. non-generic common super
    static class E<X> {
        __witness <Z> U<E<Integer>> E1() { return null; } // ok, common super with E2 (T<Z>, where Z == Integer)
        __witness <Z extends Number> W<E<Z>> E2() { return null; } // ok, common super with E1 (T<Z>, where Z == Integer)
        __witness <Z extends StringBuilder> W<E<Z>> E3() { return null; } // ok
    }
}
