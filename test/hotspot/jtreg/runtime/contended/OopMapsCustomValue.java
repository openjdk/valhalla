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
 * @summary Test contended oop maps with custom value class instances
 *
 * @modules java.base/jdk.internal.vm.annotation
 * @enablePreview
 * @run main/othervm -XX:-RestrictContended -XX:ContendedPaddingWidth=128 -Xmx128m OopMapsCustomValue
 */
public class OopMapsCustomValue {
    public static final int COUNT = 10000;

    static value class Point {
        int x;
        int y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) throws Exception {
        Point o01 = new Point(1, 1);
        Point o02 = new Point(2, 2);
        Point o03 = new Point(3, 3);
        Point o04 = new Point(4, 4);
        Point o05 = new Point(5, 5);
        Point o06 = new Point(6, 6);
        Point o07 = new Point(7, 7);
        Point o08 = new Point(8, 8);
        Point o09 = new Point(9, 9);
        Point o10 = new Point(10, 10);
        Point o11 = new Point(11, 11);
        Point o12 = new Point(12, 12);
        Point o13 = new Point(13, 13);
        Point o14 = new Point(14, 14);

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
