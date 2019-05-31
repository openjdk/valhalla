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
package org.openjdk.bench.valhalla.baseline.callconv;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.bench.valhalla.AckermannBase;
import org.openjdk.bench.valhalla.baseline.types.Ref1;

public class Ackermann1 extends AckermannBase {


    private static Ref1 ack_reference(Ref1 x, Ref1 y) {
        return x.reduce() == 0 ?
                y.inc() :
                (y.reduce() == 0 ?
                        ack_reference(x.dec(), new Ref1(1)) :
                        ack_reference(x.dec(), ack_reference(x, y.dec())));
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int reference() {
        return ack_reference(new Ref1(X1), new Ref1(Y1)).reduce()
             + ack_reference(new Ref1(X2), new Ref1(Y2)).reduce()
             + ack_reference(new Ref1(X3), new Ref1(Y3)).reduce();
    }

}
