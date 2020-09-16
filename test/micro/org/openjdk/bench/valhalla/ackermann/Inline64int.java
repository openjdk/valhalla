/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.valhalla.ackermann;

import org.openjdk.bench.valhalla.types.Int64;
import org.openjdk.bench.valhalla.types.Q64int;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;

public class Inline64int extends AckermannBase {

    private static Q64int ack_value(Q64int x, Q64int y) {
        return x.longValue() == 0 ?
                new Q64int(y.longValue() + 1) :
                (y.longValue() == 0 ?
                        ack_value(new Q64int(x.longValue()-1) , new Q64int(1)) :
                        ack_value(new Q64int(x.longValue()-1), ack_value(x, new Q64int(y.longValue()-1))));
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public long ack_Val() {
        return ack_value(new Q64int(X1), new Q64int(Y1)).longValue()
                + ack_value(new Q64int(X2), new Q64int(Y2)).longValue()
                + ack_value(new Q64int(X3), new Q64int(Y3)).longValue();
    }

    private static Q64int.ref ack_ref(Q64int.ref x, Q64int.ref y) {
        return x.longValue() == 0 ?
                new Q64int(y.longValue() + 1) :
                (y.longValue() == 0 ?
                        ack_ref(new Q64int(x.longValue()-1) , new Q64int(1)) :
                        ack_ref(new Q64int(x.longValue()-1), ack_ref(x, new Q64int(y.longValue()-1))));
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public long ack_Ref() {
        return ack_ref(new Q64int(X1), new Q64int(Y1)).longValue()
                + ack_ref(new Q64int(X2), new Q64int(Y2)).longValue()
                + ack_ref(new Q64int(X3), new Q64int(Y3)).longValue();
    }

    private static Int64 ack_inter(Int64 x, Int64 y) {
        return x.longValue() == 0 ?
                new Q64int(y.longValue() + 1) :
                (y.longValue() == 0 ?
                        ack_inter(new Q64int(x.longValue()-1) , new Q64int(1)) :
                        ack_inter(new Q64int(x.longValue()-1), ack_inter(x, new Q64int(y.longValue()-1))));
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public long ack_Int() {
        return ack_inter(new Q64int(X1), new Q64int(Y1)).longValue()
                + ack_inter(new Q64int(X2), new Q64int(Y2)).longValue()
                + ack_inter(new Q64int(X3), new Q64int(Y3)).longValue();
    }

}
