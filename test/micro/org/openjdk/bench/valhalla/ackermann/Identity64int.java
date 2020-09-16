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
import org.openjdk.bench.valhalla.types.R64int;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;

public class Identity64int extends AckermannBase {

    private static R64int ack_ref(R64int x, R64int y) {
        return x.longValue() == 0 ?
                new R64int(y.longValue() + 1) :
                (y.longValue() == 0 ?
                        ack_ref(new R64int(x.longValue()-1) , new R64int(1)) :
                        ack_ref(new R64int(x.longValue()-1), ack_ref(x, new R64int(y.longValue()-1))));
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public long ack_Ref() {
        return ack_ref(new R64int(X1), new R64int(Y1)).longValue()
                + ack_ref(new R64int(X2), new R64int(Y2)).longValue()
                + ack_ref(new R64int(X3), new R64int(Y3)).longValue();
    }

    private static Int64 ack_inter(Int64 x, Int64 y) {
        return x.longValue() == 0 ?
                new R64int(y.longValue() + 1) :
                (y.longValue() == 0 ?
                        ack_inter(new R64int(x.longValue()-1) , new R64int(1)) :
                        ack_inter(new R64int(x.longValue()-1), ack_inter(x, new R64int(y.longValue()-1))));
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public long ack_Int() {
        return ack_inter(new R64int(X1), new R64int(Y1)).longValue()
                + ack_inter(new R64int(X2), new R64int(Y2)).longValue()
                + ack_inter(new R64int(X3), new R64int(Y3)).longValue();
    }

}
