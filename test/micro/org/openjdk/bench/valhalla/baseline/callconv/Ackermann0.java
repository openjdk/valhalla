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

public class Ackermann0 extends AckermannBase {

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
        return ack_primitive(X1, Y1)
                + ack_primitive(X2, Y2)
                + ack_primitive(X3, Y3);
    }


    public static class PairExact {
        final int x;
        final int y;

        public PairExact(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public PairExact cross(PairExact o) {
            return new PairExact(x, o.getY());
        }

        public PairExact decX() {
            return new PairExact(x - 1, y);
        }

        public PairExact decY() {
            return new PairExact(x, y - 1);
        }

        public PairExact incX() {
            return new PairExact(x + 1, y);
        }

        public PairExact incY() {
            return new PairExact(x, y + 1);
        }
    }

    interface Pair {
        public int getX();

        public int getY();

        public Pair cross(Pair o);

        public Pair decX();

        public Pair decY();

        public Pair incX();

        public Pair incY();
    }

    public static class PairImpl1 implements Pair {
        final int x;
        final int y;

        public PairImpl1(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public Pair cross(Pair o) {
            return new PairImpl1(x, o.getY());
        }

        public Pair decX() {
            return new PairImpl1(x - 1, y);
        }

        public Pair decY() {
            return new PairImpl1(x, y - 1);
        }

        public Pair incX() {
            return new PairImpl1(x + 1, y);
        }

        public Pair incY() {
            return new PairImpl1(x, y + 1);
        }
    }

    public static class PairImpl2 implements Pair {
        final int x;
        final int y;

        public PairImpl2(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public Pair cross(Pair o) {
            return new PairImpl2(x, o.getY());
        }

        public Pair decX() {
            return new PairImpl2(x - 1, y);
        }

        public Pair decY() {
            return new PairImpl2(x, y - 1);
        }

        public Pair incX() {
            return new PairImpl2(x + 1, y);
        }

        public Pair incY() {
            return new PairImpl2(x, y + 1);
        }
    }

    public static class PairImpl3 implements Pair {
        final int x;
        final int y;

        public PairImpl3(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public Pair cross(Pair o) {
            return new PairImpl3(x, o.getY());
        }

        public Pair decX() {
            return new PairImpl3(x - 1, y);
        }

        public Pair decY() {
            return new PairImpl3(x, y - 1);
        }

        public Pair incX() {
            return new PairImpl3(x + 1, y);
        }

        public Pair incY() {
            return new PairImpl3(x, y + 1);
        }
    }

    public static PairExact ackermannExactType(PairExact arg) {
        if (arg.getX() == 0) {
            return arg.incY();
        } else if (arg.getY() == 0) {
            return ackermannExactType(arg.decX().incY());
        } else {
            return ackermannExactType(arg.decX().cross(ackermannExactType(arg.decY())));
        }
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int reference() {
        return ackermannExactType(new PairExact(X1, Y1)).getY()
                + ackermannExactType(new PairExact(X2, Y2)).getY()
                + ackermannExactType(new PairExact(X3, Y3)).getY();
    }

    public static Pair ackermannInterface(Pair arg) {
        if (arg.getX() == 0) {
            return arg.incY();
        } else if (arg.getY() == 0) {
            return ackermannInterface(arg.decX().incY());
        } else {
            return ackermannInterface(arg.decX().cross(ackermannInterface(arg.decY())));
        }
    }

    private static int ack1() {
        return ackermannInterface(new PairImpl1(X1, Y1)).getY()
                + ackermannInterface(new PairImpl1(X2, Y2)).getY()
                + ackermannInterface(new PairImpl1(X3, Y3)).getY();
    }

    private static int ack2() {
        return ackermannInterface(new PairImpl2(X1, Y1)).getY()
                + ackermannInterface(new PairImpl2(X2, Y2)).getY()
                + ackermannInterface(new PairImpl2(X3, Y3)).getY();
    }

    private static int ack3() {
        return ackermannInterface(new PairImpl3(X1, Y1)).getY()
                + ackermannInterface(new PairImpl3(X2, Y2)).getY()
                + ackermannInterface(new PairImpl3(X3, Y3)).getY();
    }


    @Benchmark
    @OperationsPerInvocation(OPI)
    public int ack_interface1() {
        return ack1();
    }

    @Benchmark
    @OperationsPerInvocation(OPI * 2)
    public int ack_interface2() {
        return ack1() + ack2();
    }

    @Benchmark
    @OperationsPerInvocation(OPI * 3)
    public int ack_interface3() {
        return ack1() + ack2() + ack3();
    }

}
