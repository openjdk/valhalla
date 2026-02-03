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
 * @run main/othervm -Xmx200M -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -XX:CompileOnly=*TestBufferLost::test1
 *                   -XX:CompileOnly=*TestBufferLost::test2 -XX:CompileOnly=*TestBufferLost::test3 -XX:CompileOnly=*TestBufferLost::test4
 *                   -XX:CompileOnly=*TestBufferLost::test5 -XX:CompileOnly=*TestBufferLost*::*Callee
 *                   -XX:CompileCommand=dontinline,*::*Callee -Xbatch -XX:-TieredCompilation ${test.main.class}
 * @run main/othervm -Xmx200M -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -XX:CompileOnly=*TestBufferLost*::*Callee
 *                   -XX:CompileCommand=dontinline,*::*Callee -Xbatch -XX:-TieredCompilation ${test.main.class}
 * @run main/othervm -Xmx200M -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -XX:CompileOnly=*TestBufferLost::test1
 *                   -XX:CompileOnly=*TestBufferLost::test2 -XX:CompileOnly=*TestBufferLost::test3 -XX:CompileOnly=*TestBufferLost::test4
 *                   -XX:CompileOnly=*TestBufferLost::test5 -XX:CompileCommand=dontinline,*::*Callee
 *                   -Xbatch -XX:-TieredCompilation ${test.main.class}
 */


package compiler.valhalla.inlinetypes;

import java.lang.management.*;

// java --enable-preview -Xmx200M -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -XX:CompileCommand=dontinline,*::*Callee -Xbatch -XX:-TieredCompilation TestBufferLost.java
public class TestBufferLost {

    // TODO we need more tests cases with more variants (virtual calls etc.)


    interface I {
        void test5Callee(MyValue val);
    }
    
    static value class MyValue implements I {
        long a = 1;
        long b = 2;
        long c = 3;
        long d = 4;
        long e = 5;

        void test4Callee() {
            VAL = this;
        }

        public void test5Callee(MyValue val) {
            VAL = val;
        }
    }

    static MyValue VAL = new MyValue();
    static MyValue VAL2 = new MyValue();
    static MyValue VAL3 = new MyValue();
    static I INT = new MyValue();

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

    public static void test3Callee(MyValue val1, MyValue val2, MyValue val3) {
        // This will buffer again if the scalarized arg does not contain the buffer oop
        VAL = val1;
        VAL2 = val2;
        VAL3 = val3;
    }

    public static void test3() {
        test3Callee(VAL, VAL2, VAL3);
    }

    public static void test4() {
        VAL.test4Callee();
    }

    public static void test5() {
        INT.test5Callee(VAL);
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10_000_000; ++i) {
            // test1();
            // test2();
            //test3();
            //test4();
            test5();
        }

        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        System.out.println("Heap size: " + mem.getHeapMemoryUsage().getUsed() / (1024*1024) + " MB");
    }
}
