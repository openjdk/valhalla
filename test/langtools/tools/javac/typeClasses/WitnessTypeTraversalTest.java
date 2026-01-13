/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Check that the witness lookup supports various types
 */
public class WitnessTypeTraversalTest {
    interface Mono<X> {
        int id();
    }

    interface Box<X> { }

    interface Provider {
        __witness Mono<Provider> W1 = () -> 1;
        __witness Mono<Box<Provider>> W2 = () -> 2;
        __witness Mono<Box<? extends Provider>> W3 = () -> 3;
        __witness Mono<Box<? super Provider>> W4 = () -> 4;
        __witness Mono<Box<Provider[]>> W5 = () -> 5;
        __witness Mono<Box<? extends Provider[]>> W6 = () -> 6;
        __witness Mono<Box<? super Provider[]>> W7 = () -> 7;
        __witness Mono<Box<Provider>[]> W8 = () -> 8;
        __witness Mono<Box<? extends Provider>[]> W9 = () -> 9;
        __witness Mono<Box<? super Provider>[]> W10 = () -> 10;
    }

    public static void main(String[] args) {
        checkId(Mono<Provider>.__witness.id(), 1);
        checkId(Mono<Box<Provider>>.__witness.id(), 2);
        checkId(Mono<Box<? extends Provider>>.__witness.id(), 3);
        checkId(Mono<Box<? super Provider>>.__witness.id(), 4);
        checkId(Mono<Box<Provider[]>>.__witness.id(), 5);
        checkId(Mono<Box<? extends Provider[]>>.__witness.id(), 6);
        checkId(Mono<Box<? super Provider[]>>.__witness.id(), 7);
        checkId(Mono<Box<Provider>[]>.__witness.id(), 8);
        checkId(Mono<Box<? extends Provider>[]>.__witness.id(), 9);
        checkId(Mono<Box<? super Provider>[]>.__witness.id(), 10);
    }

    static void checkId(int found, int expected) {
        if (found != expected) throw new AssertionError(String.format("Found %s, expected %s", found, expected));
    }
}
