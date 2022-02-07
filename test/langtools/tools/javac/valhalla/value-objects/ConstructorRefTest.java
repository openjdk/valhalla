/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8281166
 * @summary javac should generate BSM to invoke the static factory for value class
 * @run main ConstructorRefTest
 */

import java.util.function.Supplier;

public class ConstructorRefTest {

    public static value class V {

        final int x;
        final int y;

        V() {
            x = 1234;
            y = 5678;
        }
    }

    public static primitive class P {

        final int x;
        final int y;

        P() {
            x = 1234;
            y = 5678;
        }
    }

    public static void main(String [] args) {

        Supplier<P.ref> sxp = P::new;
        P p = (P) sxp.get();
        if (p.x != 1234 || p.y != 5678)
            throw new AssertionError(p);

        Supplier<V> sxv = V::new;
        V v = (V) sxv.get();
        if (v.x != 1234 || v.y != 5678)
            throw new AssertionError(v);
    }
}
