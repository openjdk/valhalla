/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test lambdas with parameter types or return type of value class
 * @run testng/othervm LambdaTest
 */

import java.util.function.Function;
import java.util.function.IntFunction;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class LambdaTest {
    static value class V {
        int v;
        V(int v) {
            this.v = v;
        }

        static V get(int v) {
            return new V(v);
        }
    }

    static primitive class P {
        int p;
        P(int p) {
            this.p = p;
        }

        static P get(int p) {
            return new P(p);
        }
    }

    static int getV(V v) {
        return v.v;
    }

    static int getP(P p) {
        return p.p;
    }

    @Test
    public void testValueParameterType() {
        Function<P.ref, Integer> func1 = LambdaTest::getP;
        assertTrue(func1.apply(new P(100)) == 100);

        Function<V, Integer> func2 = LambdaTest::getV;
        assertTrue(func2.apply(new V(200)) == 200);
    }

    @Test
    public void testValueReturnType() {
        IntFunction<P.ref> func1 = P::get;
        assertEquals(func1.apply(10), new P(10));

        IntFunction<V> func2 = V::get;
        assertEquals(func2.apply(20), new V(20));
    }
}
