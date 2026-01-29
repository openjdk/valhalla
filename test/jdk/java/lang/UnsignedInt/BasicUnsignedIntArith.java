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

/*
 * @test
 * @bug 8375482
 * @summary Basic tests for unsigned integer arithmetic.
 */
public class BasicUnsignedIntArith {
    public static void main(String... args) {
        int errors = 0;

        errors += checkArithmetic();
        errors += checkComparison();
        // errors += checkBitwise(); // TBD

        if (errors > 0) {
            throw new RuntimeException(errors + " errors found in unsigned operations.");
        }
    }

    private static int checkArithmetic() {
        int errors = 0;

        int[] testCases = {
            0,
            1,
            2,
            3,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            -1,
        };

        for (int op1 : testCases) {
            for (int op2 : testCases) {
                UnsignedInt op1_U = UnsignedInt.valueOf(op1);
                UnsignedInt op2_U = UnsignedInt.valueOf(op2);

                // Addition
                errors += test(op1_U, op2_U,
                               op1_U + op2_U,
                               op1   + op2, "+");
                errors += test(op1_U, op2_U,
                               UnsignedInt.add(op1_U, op2_U),
                               op1   + op2, "+");

                // Subtraction
                errors += test(op1_U, op2_U,
                               op1_U - op2_U,
                               op1   - op2, "-");
                errors += test(op1_U, op2_U,
                               UnsignedInt.subtract(op1_U, op2_U),
                               op1   - op2, "-");

                // Multiplication
                errors += test(op1_U, op2_U,
                               op1_U * op2_U,
                               op1   * op2, "*");
                errors += test(op1_U, op2_U,
                               UnsignedInt.multiply(op1_U, op2_U),
                               op1   * op2, "*");

                if (op2 != 0) {
                    // Division
                    errors += test(op1_U, op2_U,
                                   op1_U / op2_U,
                                   Integer.divideUnsigned(op1, op2), "/");
                    errors += test(op1_U, op2_U,
                                   UnsignedInt.divide(op1_U, op2_U),
                                   Integer.divideUnsigned(op1, op2), "/");

                    // Remainder
                    errors += test(op1_U, op2_U,
                                   op1_U % op2_U,
                                   Integer.remainderUnsigned(op1, op2), "%");
                    errors += test(op1_U, op2_U,
                                   UnsignedInt.remainder(op1_U, op2_U),
                                   Integer.remainderUnsigned(op1, op2), "%");
                } else {

                    // TODO: migrate these two try-catch statements to
                    // method calls once lambda's work.
                    // expectDivideByZero(op1_U, op2_U, () -> op1_U / op2_U); // Verify error :-(
                    // expectDivideByZero(op1_U, op2_U, () -> op1_U % op2_U); // Verify error :-(
                    try {
                        UnsignedInt quotient = op1_U / op2_U;
                        System.err.println("Missing arithmetic exception for divide by zero: " +
                                           op1_U + " / " + op2_U);
                        errors++;
                    } catch(ArithmeticException ae) {
                        ; // expected
                    }

                    try {
                        UnsignedInt quotient = op1_U % op2_U;
                        System.err.println("Missing arithmetic exception for divide by zero: " +
                                           op1_U + " % " + op2_U);
                        errors++;
                    } catch(ArithmeticException ae) {
                        ; // expected
                    }

                    errors += expectDivideByZero(op1_U, op2_U,
                                                 () -> UnsignedInt.divide(op1_U, op2_U));
                    errors += expectDivideByZero(op1_U, op2_U,
                                                 () -> UnsignedInt.remainder(op1_U, op2_U));
                }
            }
        }

        return errors;
    }

    private static int expectDivideByZero(UnsignedInt op1_U,  UnsignedInt op2_U,
                                          java.util.function.Supplier<UnsignedInt> supplier) {
        try {
            UnsignedInt quotient = supplier.get();
            System.err.println("Missing arithmetic exception for divide by zero: " +
                               op1_U + " OP " + op2_U);
            return 1;
        } catch(ArithmeticException ae) {
            return 0; // expected
        }
    }

    private static int checkComparison() {
        int errors = 0;

        int[] testCases = {
            0,
            1,
            2,
            3,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            -1,
        };

        for (int op1 : testCases) {
            for (int op2 : testCases) {
                UnsignedInt op1_U = UnsignedInt.valueOf(op1);
                UnsignedInt op2_U = UnsignedInt.valueOf(op2);

                errors += test(op1_U, op2_U, op1_U <  op2_U,  u_lt( op1, op2), "<");
                errors += test(op1_U, op2_U, op1_U <= op2_U,  u_lte(op1, op2), "<=");
                errors += test(op1_U, op2_U, op1_U >  op2_U,  u_gt( op1, op2), ">");
                errors += test(op1_U, op2_U, op1_U >= op2_U,  u_gte(op1, op2), ">=");

                errors += test(op1_U, op2_U, UnsignedInt.lessThan(op1_U, op2_U),         u_lt( op1, op2), "<");
                errors += test(op1_U, op2_U, UnsignedInt.lessThanEqual(op1_U, op2_U),    u_lte(op1, op2), "<=");
                errors += test(op1_U, op2_U, UnsignedInt.greaterThan(op1_U, op2_U),      u_gt( op1, op2), ">");
                errors += test(op1_U, op2_U, UnsignedInt.greaterThanEqual(op1_U, op2_U), u_gte(op1, op2), ">=");
            }
        }

        return errors;
    }

    private static boolean u_lt(int op1, int op2) {
        return Integer.compareUnsigned(op1, op2) < 0;
    }

    private static boolean u_lte(int op1, int op2) {
        return Integer.compareUnsigned(op1, op2) <= 0;
    }

    private static boolean u_gt(int op1, int op2) {
        return Integer.compareUnsigned(op1, op2) > 0;
    }

    private static boolean u_gte(int op1, int op2) {
        return Integer.compareUnsigned(op1, op2) >= 0;
    }

    private static int test(UnsignedInt op1, UnsignedInt op2,
                            UnsignedInt result, int expected,
                            String operator) {
        if (result.intValue() != expected) {
            System.err.println("Failure for " + op1 + " " + operator + " " + op2 + ";" +
                               "\texpected " + UnsignedInt.valueOf(expected) +
                               " got,\t" + result);
            return 1;
        }
        return 0;
    }

    private static int test(UnsignedInt op1, UnsignedInt op2,
                            boolean result, boolean expected,
                            String operator) {
        if (result != expected) {
            System.err.println("Failure for " + op1 + " " + operator + " " + op2 + ";" +
                               "\texpected " + expected +
                               " got,\t" + result);
            return 1;
        }
        return 0;
    }
}
