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
package oracle.micro.valhalla.baseline.ackermann;

import oracle.micro.valhalla.AckermannBase;
import oracle.micro.valhalla.baseline.types.Box1;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;

public class Ackermann1 extends AckermannBase {

    private static int ack_primitive(int x, int y) {
        return x == 0 ?
                y + 1 :
                (y == 0 ?
                        ack_primitive(x - 1, 1) :
                        ack_primitive(x - 1, ack_primitive(x, y - 1)));
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int primitive() {
        return ack_primitive(X1, Y1) + ack_primitive(X2, Y2) + ack_primitive(X3, Y3);
    }

    public static final Box1 B1_ONE = new Box1(1);
    public static final Box1 B1_X1 = B1_ONE;
    public static final Box1 B1_Y1 = new Box1(1748);
    public static final Box1 B1_X2 = new Box1(2);
    public static final Box1 B1_Y2 = new Box1(1897);
    public static final Box1 B1_X3 = new Box1(3);
    public static final Box1 B1_Y3 = new Box1(8);


    private static Box1 ack_boxed(Box1 x, Box1 y) {
        return x.totalsum() == 0 ?
                y.inc() :
                (y.totalsum() == 0 ?
                        ack_boxed(x.dec(), B1_ONE) :
                        ack_boxed(x.dec(), ack_boxed(x, y.dec())));
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int boxed() {
        return ack_boxed(B1_X1, B1_Y1).totalsum() + ack_boxed(B1_X2, B1_Y2).totalsum() + ack_boxed(B1_X3, B1_Y3).totalsum();
    }

}
