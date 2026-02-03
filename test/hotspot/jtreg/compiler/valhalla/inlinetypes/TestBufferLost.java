/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8372268
 * @summary [lworld] Keep buffer oop on scalarized calls
 * @enablePreview
 * @run main/othervm -Xmx200M -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -XX:CompileOnly=TestBufferLost::test1 -XX:CompileCommand=dontinline,*::*Callee -Xbatch -XX:-TieredCompilation ${test.main.class}
 */

package compiler.valhalla.inlinetypes;

import java.lang.management.*;

// java --enable-preview -Xmx200M -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -XX:CompileCommand=dontinline,*::*Callee -Xbatch -XX:-TieredCompilation TestBufferLost.java
public class TestBufferLost {

    // TODO we need more tests cases with more variants (virtual calls etc.)

    static value class MyValue {
        long a = 1;
        long b = 2;
        long c = 3;
        long d = 4;
        long e = 5;
    }

    static MyValue VAL = new MyValue();

    public static void test1Callee(MyValue val) {
        // This will buffer again if the scalarized arg does not contain the buffer oop
        VAL = val;
    }

    public static void test1() {
        test1Callee(VAL);
    }

    public static MyValue test2Callee() {
        return VAL;
    }

    public static void test2() {
        // This will buffer again if the scalarized return does not contain the buffer oop
        VAL = test2Callee();
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10_000_000; ++i) {
            test1();
            test2();
        }

        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        System.out.println("Heap size: " + mem.getHeapMemoryUsage().getUsed() / (1024*1024) + " MB");
    }
}
