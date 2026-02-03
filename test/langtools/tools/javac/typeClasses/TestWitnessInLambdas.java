/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @summary Smoke test for witness lookup expressions inside lambda expressions
 * @run junit TestWitnessInLambdas
 */

import java.util.function.BinaryOperator;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class TestWitnessInLambdas {

    record NumBox(int i) {
        __witness Addable<NumBox> ADD = (x, y) -> new NumBox(x.i + y.i);

        static NumBox ZERO = new NumBox(0);
        static NumBox ONE = new NumBox(1);
    }

    interface Addable<N> {
        N add(N first, N second);
    }

    {
        checkSum(NumBox.ZERO, NumBox.ONE, () -> Addable<NumBox>.__witness, NumBox.ONE);
    }

    static {
        checkSum(NumBox.ZERO, NumBox.ONE, () -> Addable<NumBox>.__witness, NumBox.ONE);
    }

    void test_instance() {
        checkSum(NumBox.ZERO, NumBox.ONE, () -> Addable<NumBox>.__witness, NumBox.ONE);
    }

    static void test_static() {
        checkSum(NumBox.ZERO, NumBox.ONE, () -> Addable<NumBox>.__witness, NumBox.ONE);
    }

    static void checkSum(NumBox f1, NumBox f2, Supplier<Addable<NumBox>> addableSupplier, NumBox r) {
        assertEquals(addableSupplier.get().add(f1, f2), r);
    }
}
