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
import oracle.micro.valhalla.lword.types.Value8;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;

public class Ackermann8 extends AckermannBase {

    public static final Value8 V8_ONE = Value8.of(0, 0, 0, 0, 0, 0, 0, 1);
    public static final Value8 V8_X1 = V8_ONE;
    public static final Value8 V8_Y1 = Value8.of(0, 0, 0, 0, 0, 0, 0, 1748);
    public static final Value8 V8_X2 = Value8.of(0, 0, 0, 0, 0, 0, 0, 2);
    public static final Value8 V8_Y2 = Value8.of(0, 0, 0, 0, 0, 0, 0, 1897);
    public static final Value8 V8_X3 = Value8.of(0, 0, 0, 0, 0, 0, 0, 3);
    public static final Value8 V8_Y3 = Value8.of(0, 0, 0, 0, 0, 0, 0, 8);


    private static Value8 ack_value(Value8 x, Value8 y) {
        return x.totalsum() == 0 ?
                y.inc() :
                (y.totalsum() == 0 ?
                        ack_value(x.dec(), V8_ONE) :
                        ack_value(x.dec(), ack_value(x, y.dec())));
    }

    @Benchmark
    @OperationsPerInvocation(OPI)
    public int value() {
        return ack_value(V8_X1, V8_Y1).totalsum() + ack_value(V8_X2, V8_Y2).totalsum() + ack_value(V8_X3, V8_Y3).totalsum();
    }

}
