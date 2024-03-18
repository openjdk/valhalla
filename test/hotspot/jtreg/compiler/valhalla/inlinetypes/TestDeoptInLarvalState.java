/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8239003
 * @summary C2 should respect larval state when scalarizing
 * @modules java.base/jdk.internal.misc
 * @library /testlibrary /test/lib
 * @compile TestDeoptInLarvalState.java
 * @run main/othervm/timeout=300 -Xbatch -XX:CompileThresholdScaling=0.3 -XX:+UnlockDiagnosticVMOptions -XX:+PrintDeoptimizationDetails -XX:CompileOnly=compiler.valhalla.inlinetypes.TestDeoptInLarvalState::test1 --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED  compiler.valhalla.inlinetypes.TestDeoptInLarvalState
 * @run main/othervm/timeout=300 -Xbatch -XX:CompileThresholdScaling=0.3 -XX:+UnlockDiagnosticVMOptions -XX:+PrintCompilation -XX:CompileOnly=compiler.valhalla.inlinetypes.TestDeoptInLarvalState::test2 -XX:CompileCommand=DontInline,compiler.valhalla.inlinetypes.TestDeoptInLarvalState::cleanup --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED  compiler.valhalla.inlinetypes.TestDeoptInLarvalState
 * @run main/othervm/timeout=300 -Xbatch -XX:CompileThresholdScaling=0.3 -XX:+UnlockDiagnosticVMOptions -XX:+PrintDeoptimizationDetails -XX:CompileOnly=compiler.valhalla.inlinetypes.TestDeoptInLarvalState::test2 --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED  compiler.valhalla.inlinetypes.TestDeoptInLarvalState
 */

package compiler.valhalla.inlinetypes;

import jdk.internal.misc.Unsafe;

value class Pair {
    public double d1, d2;
    public Pair(double d1, double d2) {
        this.d1 = d1;
        this.d2 = d2;
    }
}

public class TestDeoptInLarvalState {
    static final long OFFSET = 16;

    public static Pair cleanup(Pair p) {
        return Unsafe.getUnsafe().finishPrivateBuffer(p);
    }

    public static double test1(double value, int iter) {
        Pair p = Unsafe.getUnsafe().makePrivateBuffer(new Pair(10.0, 20.0));
        Unsafe.getUnsafe().putDouble(p, OFFSET, value);
        if (iter == 5000) {
           throw new RuntimeException("test1 : Failed to update p with value = " + p.d1);
        }
        p = Unsafe.getUnsafe().finishPrivateBuffer(p);
        return p.d1 + p.d2;
    }

    public static double test2(double value, int iter) {
        Pair p = Unsafe.getUnsafe().makePrivateBuffer(new Pair(10.0, 20.0));
        Unsafe.getUnsafe().putDouble(p, OFFSET, value);
        if (iter == 5000) {
           throw new RuntimeException("test2 : Failed to update p with value = " + p.d1);
        }
        p = cleanup(p);
        return p.d1 + p.d2;
    }

    public static void main(String[] args) {
        double res = 0.0;
        try {
            for (int i = 0; i < 10000; i++) {
                res += test1((double)i, i);
            }
            System.out.println("[res]" + res);
        } catch (Exception e) {
            System.out.println("Caught Exception: " + e);
        }
        try {
            for (int i = 0; i < 10000; i++) {
                res += test2((double)i, i);
            }
            System.out.println("[res]" + res);
        } catch (Exception e) {
            System.out.println("Caught Exception: " + e);
        }
    }
}
