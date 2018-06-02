/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package oracle.micro.valhalla.lword.ackermann;

import oracle.micro.valhalla.AckermannBase;
import oracle.micro.valhalla.types.PNumber;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;

public class AckermannMix extends AckermannBase {

    private static PNumber ack_interface(PNumber one, PNumber x, PNumber y) {
        return x.totalsum() == 0 ?
                y.inc() :
                (y.totalsum() == 0 ?
                        ack_interface(one, x.dec(), one) :
                        ack_interface(one, x.dec(), ack_interface(one , x, y.dec())));
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int interface1() {
        return ack_interface(Ackermann1.V1_ONE, Ackermann1.V1_X1, Ackermann1.V1_Y1).totalsum() +
               ack_interface(Ackermann1.V1_ONE, Ackermann1.V1_X2, Ackermann1.V1_Y2).totalsum() +
               ack_interface(Ackermann1.V1_ONE, Ackermann1.V1_X3, Ackermann1.V1_Y3).totalsum();
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int interface2() {
        return ack_interface(Ackermann2.V2_ONE, Ackermann2.V2_X1, Ackermann2.V2_Y1).totalsum() +
               ack_interface(Ackermann2.V2_ONE, Ackermann2.V2_X2, Ackermann2.V2_Y2).totalsum() +
               ack_interface(Ackermann2.V2_ONE, Ackermann2.V2_X3, Ackermann2.V2_Y3).totalsum();
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int interface8() {
        return ack_interface(Ackermann8.V8_ONE, Ackermann8.V8_X1, Ackermann8.V8_Y1).totalsum() +
               ack_interface(Ackermann8.V8_ONE, Ackermann8.V8_X2, Ackermann8.V8_Y2).totalsum() +
               ack_interface(Ackermann8.V8_ONE, Ackermann8.V8_X3, Ackermann8.V8_Y3).totalsum();
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int interfaceMixDepth() {
        return ack_interface(Ackermann1.V1_ONE, Ackermann1.V1_X1, Ackermann1.V1_Y1).totalsum() +
               ack_interface(Ackermann2.V2_ONE, Ackermann2.V2_X2, Ackermann2.V2_Y2).totalsum() +
               ack_interface(Ackermann8.V8_ONE, Ackermann8.V8_X3, Ackermann8.V8_Y3).totalsum();
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int interfaceMixWidth() {
        return ack_interface(Ackermann1.V1_ONE, Ackermann2.V2_X1, Ackermann8.V8_Y1).totalsum() +
               ack_interface(Ackermann2.V2_ONE, Ackermann8.V8_X2, Ackermann1.V1_Y2).totalsum() +
               ack_interface(Ackermann8.V8_ONE, Ackermann1.V1_X3, Ackermann2.V2_Y3).totalsum();
    }


}
