/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8230397
 * @summary Test removal of an already dead AllocateNode with not-yet removed proj outputs.
 * @run main/othervm -Xbatch TestDeadAllocationRemoval
 */

public class TestDeadAllocationRemoval {

    public static void main(String[] args) {
        Test test = new Test();
        for (int i = 0; i < 10; ++i) {
            test.test();
        }
    }
}

primitive class MyValue {
    public static long instanceCount = 0;
    public float fFld = 0;
    public boolean bFld = true;
    public float fFld1 = 0;
}

class Test {
    public static final int N = 400;

    public static long instanceCount=2149450457L;
    public static double dFld=63.1805;
    public static boolean bFld1=false;
    public static int iFld=-4;
    public static double dArrFld[]=new double[N];
    public static int iArrFld[]=new int[N];
    public static MyValue OFld=new MyValue();

    public static long vMeth_check_sum = 0;
    public static long lMeth_check_sum = 0;

    public void vMeth(int i) {
        for (double d = 8; d < 307; d++) {
            for (int i3 = 1; i3 < 6; i3 += 2) {
                i <<= -23877;
            }
        }
    }

    public void test() {
        int i21=-35918, i22=11, i23=31413, i24=-7, i25=0, i26=70;
        double d3=0.122541;

        vMeth(Test.iFld);
        for (i21 = 20; i21 < 396; ++i21) {
            d3 = 1;
            while (++d3 < 67) {
                byte by=38;
                Test.dArrFld[(int)(d3)] = -7;
                switch ((i21 % 9) + 1) {
                case 1:
                    for (i23 = 1; i23 < 1; i23 += 3) {
                        Test.instanceCount = i22;
                        Test.iFld -= (int)Test.OFld.fFld1;
                        Test.instanceCount >>= MyValue.instanceCount;
                        i22 = (int)Test.OFld.fFld1;
                        Test.bFld1 = false;
                        Test.iArrFld[(int)(d3 - 1)] &= i23;
                        i22 += (i23 + i24);
                        i22 -= (int)d3;
                        Test.iFld |= (int)MyValue.instanceCount;
                    }
                    Test.iFld -= (int)Test.instanceCount;
                    break;
                case 2:
                    for (i25 = 1; i25 < 1; i25++) {
                        i26 += i22;
                        i26 += i25;
                        Test.iArrFld[i25 + 1] += (int)MyValue.instanceCount;
                        i22 += (i25 - Test.instanceCount);
                        i26 += (i25 + i21);
                    }
                    Test.instanceCount -= 2;
                    Test.dFld = i22;
                    Test.iFld += (int)(((d3 * by) + by) - i24);
                    break;
                case 3:
                    i24 = (int)1.84829;
                    Test.OFld = new MyValue();
                    break;
                case 4:
                    Test.OFld = new MyValue();
                    MyValue.instanceCount += (long)d3;
                    break;
                case 5:
                    MyValue.instanceCount += (long)(d3 * d3);
                    break;
                case 6:
                    Test.dFld -= i25;
                case 7:
                    try {
                        i24 = (78 / Test.iFld);
                        Test.iFld = (-5836 / Test.iArrFld[(int)(d3 + 1)]);
                        i24 = (i23 / -205);
                    } catch (ArithmeticException a_e) {}
                    break;
                case 8:
                    if (Test.bFld1) continue;
                case 9:
                default:
                    try {
                        i26 = (i24 / -929688879);
                        i24 = (Test.iArrFld[(int)(d3)] % -1067487586);
                        Test.iArrFld[(int)(d3)] = (-208 % i24);
                    } catch (ArithmeticException a_e) {}
                }
            }
        }

        System.out.println("i21 i22 d3 = " + i21 + "," + i22 + "," + Double.doubleToLongBits(d3));
        System.out.println("i23 i24 Test.OFld.fFld1 = " + i23 + "," + i24 + "," + Float.floatToIntBits(Test.OFld.fFld1));
        System.out.println("MyValue = " + MyValue.instanceCount);
        System.out.println("Test.instanceCount Test.dFld Test.bFld1 = " + Test.instanceCount + "," + Double.doubleToLongBits(Test.dFld) + "," + (Test.bFld1 ? 1 : 0));
        System.out.println("MyValue = " + MyValue.instanceCount);
        System.out.println("lMeth_check_sum: " + lMeth_check_sum);
        System.out.println("vMeth_check_sum: " + vMeth_check_sum);
    }
}
