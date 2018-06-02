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
import oracle.micro.valhalla.baseline.types.Box8;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;

public class Ackermann8 extends AckermannBase {

    private static int ack_primitive(int x0, int x1, int x2, int x3, int x4, int x5, int x6, int x7, int y0, int y1, int y2, int y3, int y4, int y5, int y6, int y7) {
        return (x0 + x1 + x2 + x3 + x4 + x5 + x6 + x7) == 0 ?
                (y0 + y1 + y2 + y3 + y4 + y5 + y6 + y7 + 1) :
                ((y0 + y1 + y2 + y3 + y4 + y5 + y6 + y7) == 0 ?
                        ack_primitive(x1, x2, x3, x4, x5, x6, x7, x0 - 1, 0, 0, 0, 0, 0, 0, 0, 1) :
                        ack_primitive(x1, x2, x3, x4, x5, x6, x7, x0 - 1, 0, 0, 0, 0, 0, 0, 0,
                                ack_primitive(x0, x1, x2, x3, x4, x5, x6, x7, y1, y2, y3, y4, y5, y6, y7, y0 - 1)));
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int primitive() {
        return ack_primitive(0, 0, 0, 0, 0, 0, 0, X1, 0, 0, 0, 0, 0, 0, 0, Y1) +
               ack_primitive(0, 0, 0, 0, 0, 0, 0, X2, 0, 0, 0, 0, 0, 0, 0, Y2) +
               ack_primitive(0, 0, 0, 0, 0, 0, 0, X3, 0, 0, 0, 0, 0, 0, 0, Y3);
    }

    public static final Box8 B8_ONE = new Box8(0, 0, 0, 0, 0, 0, 0, 1);
    public static final Box8 B8_X1 = B8_ONE;
    public static final Box8 B8_Y1 = new Box8(0, 0, 0, 0, 0, 0, 0, 1748);
    public static final Box8 B8_X2 = new Box8(0, 0, 0, 0, 0, 0, 0, 2);
    public static final Box8 B8_Y2 = new Box8(0, 0, 0, 0, 0, 0, 0, 1897);
    public static final Box8 B8_X3 = new Box8(0, 0, 0, 0, 0, 0, 0, 3);
    public static final Box8 B8_Y3 = new Box8(0, 0, 0, 0, 0, 0, 0, 8);


    private static Box8 ack_boxed(Box8 x, Box8 y) {
        return x.totalsum() == 0 ?
                y.inc() :
                (y.totalsum() == 0 ?
                        ack_boxed(x.dec(), B8_ONE) :
                        ack_boxed(x.dec(), ack_boxed(x, y.dec())));
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int boxed() {
        return ack_boxed(B8_X1, B8_Y1).totalsum() + ack_boxed(B8_X2, B8_Y2).totalsum() + ack_boxed(B8_X3, B8_Y3).totalsum();
    }

}
