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

public class IsCmp extends AcmpBase {

    Object[] objects1;
    Object[] objects2;

    @Param({"0", "1", "25", "50", "75", "99", "100"})
    int eq; // how many elements objects1[i] and objects2[i] equals (in %)

    @Setup
    public void setup() {
        objects1 = new Object[SIZE];
        Arrays.setAll(objects1, i -> new Object());
        objects2 = populate(objects1, eq);
        forcedWarmup();
    }

    public boolean sideB;
    public int sideI;

    // that is required to cover all branches in acmp code
    private void forcedWarmup() {
        Object[] objects1 = new Object[SIZE];
        Arrays.setAll(objects1, i -> new Object());
        Object[] objects2 = populate(objects1, eq);
        objects2[0] = objects1[0] = null;
        objects2[1] = null;
        objects2[2] = null;
        sideI += isCmpSum(objects1, objects2);
        sideB ^= isCmpXor(objects1, objects2);
        sideI += isNotCmpSum(objects1, objects2);
        sideB ^= isNotCmpXor(objects1, objects2);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    public boolean isCmpValue() {
        return isCmpXor(objects1, objects2);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    public int isCmpBranch() {
        return isCmpSum(objects1, objects2);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    public boolean isNotCmpValue() {
        return isNotCmpXor(objects1, objects2);
    }

    @OperationsPerInvocation(SIZE)
    @Benchmark
    public int isNotCmpBranch() {
        return isNotCmpSum(objects1, objects2);
    }


    private static boolean isCmpXor(Object[] objects1, Object[] objects2) {
        boolean s = false;
        for (int i = 0; i < SIZE; i++) {
            s ^= objects1[i] == objects2[i];
        }
        return s;
    }

    private static int isCmpSum(Object[] objects1, Object[] objects2) {
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            if (objects1[i] == objects2[i]) {
                s += 1;
            } else {
                s -= 1;
            }
        }
        return s;
    }

    private static boolean isNotCmpXor(Object[] objects1, Object[] objects2) {
        boolean s = false;
        for (int i = 0; i < SIZE; i++) {
            s ^= objects1[i] != objects2[i];
        }
        return s;
    }

    private static int isNotCmpSum(Object[] objects1, Object[] objects2) {
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            if (objects1[i] != objects2[i]) {
                s += 1;
            } else {
                s -= 1;
            }
        }
        return s;
    }


}
