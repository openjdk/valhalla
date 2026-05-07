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
import jdk.internal.vm.annotation.Contended;

/*
 * @test
 * @bug     8384107
 * @summary Test contended oop maps with value class instances
 *
 * @modules java.base/jdk.internal.vm.annotation
 * @run main/othervm -XX:-RestrictContended -XX:ContendedPaddingWidth=128 -Xmx128m OopMapsValue
 */
public class OopMapsValue {
    public static final int COUNT = 10000;

    public static void main(String[] args) throws Exception {
        Integer o01 = Integer.valueOf(101);
        Integer o02 = Integer.valueOf(102);
        Integer o03 = Integer.valueOf(103);
        Integer o04 = Integer.valueOf(104);
        Integer o05 = Integer.valueOf(105);
        Integer o06 = Integer.valueOf(106);
        Integer o07 = Integer.valueOf(107);
        Integer o08 = Integer.valueOf(108);
        Integer o09 = Integer.valueOf(109);
        Integer o10 = Integer.valueOf(110);
        Integer o11 = Integer.valueOf(111);
        Integer o12 = Integer.valueOf(112);
        Integer o13 = Integer.valueOf(113);
        Integer o14 = Integer.valueOf(114);

        R1[] rs = new R1[COUNT];
        for (int i = 0; i < COUNT; i++) {
           R1 r1 = new R1();
           r1.o01 = o01;
           r1.o02 = o02;
           r1.o03 = o03;
           r1.o04 = o04;
           r1.o05 = o05;
           r1.o06 = o06;
           r1.o07 = o07;
           r1.o08 = o08;
           r1.o09 = o09;
           r1.o10 = o10;
           r1.o11 = o11;
           r1.o12 = o12;
           r1.o13 = o13;
           r1.o14 = o14;
           r1.i1 = 1;
           r1.i2 = 2;
           r1.i3 = 3;
           r1.i4 = 4;
           rs[i] = r1;
        }

        System.gc();

        for (int i = 0; i < COUNT; i++) {
           R1 r1 = rs[i];
           if (!o01.equals(r1.o01)) throw new Error("Test Error: o01");
           if (!o02.equals(r1.o02)) throw new Error("Test Error: o02");
           if (!o03.equals(r1.o03)) throw new Error("Test Error: o03");
           if (!o04.equals(r1.o04)) throw new Error("Test Error: o04");
           if (!o05.equals(r1.o05)) throw new Error("Test Error: o05");
           if (!o06.equals(r1.o06)) throw new Error("Test Error: o06");
           if (!o07.equals(r1.o07)) throw new Error("Test Error: o07");
           if (!o08.equals(r1.o08)) throw new Error("Test Error: o08");
           if (!o09.equals(r1.o09)) throw new Error("Test Error: o09");
           if (!o10.equals(r1.o10)) throw new Error("Test Error: o10");
           if (!o11.equals(r1.o11)) throw new Error("Test Error: o11");
           if (!o12.equals(r1.o12)) throw new Error("Test Error: o12");
           if (!o13.equals(r1.o13)) throw new Error("Test Error: o13");
           if (!o14.equals(r1.o14)) throw new Error("Test Error: o14");
           if (r1.i1 != 1)    throw new Error("Test Error: i1");
           if (r1.i2 != 2)    throw new Error("Test Error: i2");
           if (r1.i3 != 3)    throw new Error("Test Error: i3");
           if (r1.i4 != 4)    throw new Error("Test Error: i4");
        }
    }

    public static class R0 {
        int i1;
        int i2;
        Object o01;
        Object o02;
        @Contended
        Object o03;
        @Contended
        Object o04;
        @Contended
        Object o05;
        @Contended
        Object o06;
        @Contended
        Object o07;
   }

   public static class R1 extends R0 {
        int i3;
        int i4;
        Object o08;
        Object o09;
        @Contended
        Object o10;
        @Contended
        Object o11;
        @Contended
        Object o12;
        @Contended
        Object o13;
        @Contended
        Object o14;
   }
}
