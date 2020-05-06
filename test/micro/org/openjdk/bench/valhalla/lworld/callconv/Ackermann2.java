/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.valhalla.lworld.callconv;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.bench.valhalla.AckermannBase;
import org.openjdk.bench.valhalla.lworld.types.Val2;

public class Ackermann2 extends AckermannBase {

    private static Val2 ack_value(Val2 x, Val2 y) {
        return x.reduce() == 0 ?
                y.inc() :
                (y.reduce() == 0 ?
                        ack_value(x.dec(), new Val2(0, 1)) :
                        ack_value(x.dec(), ack_value(x, y.dec())));
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int value() {
        return ack_value(new Val2(0, X1), new Val2(0, Y1)).reduce() +
               ack_value(new Val2(0, X2), new Val2(0, Y2)).reduce() +
               ack_value(new Val2(0, X3), new Val2(0, Y3)).reduce();
    }

    private static Val2.ref ack_boxed(Val2.ref x, Val2.ref y) {
        return x.reduce() == 0 ?
                y.inc() :
                (y.reduce() == 0 ?
                        ack_boxed(x.dec(), new Val2(0, 1)) :
                        ack_boxed(x.dec(), ack_boxed(x, y.dec())));
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int boxed() {
        return ack_boxed(new Val2(0, X1), new Val2(0, Y1)).reduce() +
               ack_boxed(new Val2(0, X2), new Val2(0, Y2)).reduce() +
               ack_boxed(new Val2(0, X3), new Val2(0, Y3)).reduce();
    }

}
