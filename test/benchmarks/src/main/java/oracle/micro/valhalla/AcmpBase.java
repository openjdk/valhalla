/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package oracle.micro.valhalla;

import org.openjdk.jmh.annotations.*;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
public abstract class AcmpBase {

    public static final int SIZE = 1024;

    /*
       creates and returns array ob Objects where `eq` percent of values are equals to base array
     */
    public static Object[] populate(Object[] base, int eq) {
        Object[] objects2 = new Object[base.length];
        Arrays.setAll(objects2, i -> new Object());
        if (eq == 0) {
            // nothing to do
        } else if (eq >= 100) {
            System.arraycopy(base, 0, objects2, 0, base.length);
        } else {
            new Random(42)
                    .ints(0, base.length)
                    .distinct()
                    .limit(eq * base.length / 100)
                    .forEach(i ->  objects2[i] = base[i]);
        }
        return objects2;
    }

}
