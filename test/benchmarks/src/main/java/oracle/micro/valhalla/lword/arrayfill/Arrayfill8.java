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
package oracle.micro.valhalla.lword.arrayfill;

import oracle.micro.valhalla.ArrayfillBase;
import oracle.micro.valhalla.lword.types.Value8;
import oracle.micro.valhalla.lword.types.Wrapper8;
import oracle.micro.valhalla.types.PNumber;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;

public class Arrayfill8 extends ArrayfillBase {

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object value() {
        Value8[] values = new Value8[SIZE];
        for (int i = 0, k = 0; i < values.length; i++, k += 8) {
            values[i] = Value8.of(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object objects() {
        Object[] values = new Object[SIZE];
        for (int i = 0, k = 0; i < values.length; i++, k += 8) {
            values[i] = Value8.of(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public Object inter() {
        PNumber[] values = new PNumber[SIZE];
        for (int i = 0, k = 0; i < values.length; i++, k += 8) {
            values[i] = Value8.of(k, k + 1, k + 2, k + 3, k + 4, k + 5, k + 6, k + 7);
        }
        return values;
    }

}
