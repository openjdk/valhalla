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
package oracle.micro.valhalla.baseline.acmp;

import oracle.micro.valhalla.AcmpBase;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;

import java.util.Arrays;

public class IsNull extends AcmpBase {

    Object[] objects;

    @Param({"0", "1", "25", "50", "75", "99", "100"})
    int nulls; // how many elements if objects[] are nulls (in %)

    @Setup
    public void setup() {
        objects = populate(new Object[SIZE], nulls);
        forcedWarmup();
    }

    public boolean sideB;
    public int sideI;

    // that is required to cover all branches in acmp code
    private void forcedWarmup() {
        Object[] objects = populate(new Object[SIZE], 50);

        sideI += isNullSum(objects);
        sideB ^= isNullXor(objects);
        sideI += isNotNullSum(objects);
        sideB ^= isNotNullXor(objects);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    public boolean isNullValue() {
        return isNullXor(objects);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    public int isNullBranch() {
        return isNullSum(objects);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    public boolean isNotNullValue() {
        return isNotNullXor(objects);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    public int isNotNullBranch() {
        return isNotNullSum(objects);
    }

    private static boolean isNullXor(Object[] objects) {
        boolean s = false;
        for (int i = 0; i < objects.length; i++) {
            s ^= objects[i] == null;
        }
        return s;
    }

    private static int isNullSum(Object[] objects) {
        int s = 0;
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == null) {
                s += 1;
            } else {
                s -= 1;
            }
        }
        return s;
    }

    private static boolean isNotNullXor(Object[] objects) {
        boolean s = false;
        for (int i = 0; i < objects.length; i++) {
            s ^= objects[i] != null;
        }
        return s;
    }

    private static int isNotNullSum(Object[] objects) {
        int s = 0;
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] != null) {
                s += 1;
            } else {
                s -= 1;
            }
        }
        return s;
    }

}
