/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8308363
 * @summary Initial compiler support for Float16.add operation.
 * @compile FP16ScalarOperations.java
 * @run main/othervm --enable-preview -XX:-TieredCompilation -Xbatch FP16ScalarOperations
 */

import java.util.Random;

import static java.lang.Float16.*;

public class FP16ScalarOperations {

    public static Random r = new Random(1024);

    public static short actual_value(char oper, short val1, short val2) {
        Float16 obj1 = shortBitsToFloat16(val1);
        Float16 obj2 = shortBitsToFloat16(val2);
        switch ((int)oper) {
            case '+' : return float16ToRawShortBits(Float16.sum(obj1, obj2));
            default  : throw new AssertionError("Unsupported Operation!");
        }
    }

    public static void test_add(short [] arr1, short arr2[]) {
        for (int i = 0; i < arr1.length; i++) {
            validate('+', arr1[i], arr2[i]);
        }
    }

    public static short expected_value(char oper, short input1, short input2) {
        switch((int)oper) {
            case '+' : return Float.floatToFloat16(Float.float16ToFloat(input1) + Float.float16ToFloat(input2));
            default  : throw new AssertionError("Unsupported Operation!");
        }
    }

    public static boolean compare(short actual, short expected) {
        return !((0xFFFF & actual) == (0xFFFF & expected));
    }

    public static void validate(char oper, short input1, short input2) {
        short actual = actual_value(oper, input1, input2);
        short expected = expected_value(oper, input1, input2);
        if (compare(actual, expected)) {
            throw new AssertionError("Test Failed: " + input1 + " + " + input2 + " : " + actual + " != " + expected);
        }
    }

    public static short [] get_fp16_array(int size) {
        short [] arr = new short[size];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = Float.floatToFloat16(r.nextFloat());
        }
        return arr;
    }

    public static void main(String [] args) {
        int res = 0;
        short [] input1 = get_fp16_array(1024);
        short [] input2 = get_fp16_array(1024);
        short [] special_values = {
              32256,          // NAN
              31744,          // +Inf
              (short)-1024,   // -Inf
              0,              // +0.0
              (short)-32768,  // -0.0
        };
        for (int i = 0;  i < 1000; i++) {
            test_add(input1, input2);
            test_add(special_values, special_values);
        }
        System.out.println("PASS");
    }
}
