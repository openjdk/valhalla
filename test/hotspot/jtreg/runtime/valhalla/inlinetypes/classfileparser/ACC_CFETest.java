/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test ACC_CFETest
 * @bug 8281279
 * @summary test class access rules for classes that have ACC_PERMITS_VALUE set.
 * @compile ACCCFETests.jcod
 * @run main/othervm -XX:+EnableValhalla -Xverify:remote ACC_CFETest
 */

public class ACC_CFETest {

    public static void runTest(String test_name, String message) throws Exception {
        System.out.println("Testing: " + test_name);
        try {
            Class newClass = Class.forName(test_name);
            if (message != null) {
                throw new RuntimeException("Expected ClassFormatError with message: " + message + ", but none occurred");
            }
        } catch (java.lang.ClassFormatError e) {
            if (!e.getMessage().contains(message)) {
                throw new RuntimeException( "Wrong ClassFormatError: " + e.getMessage() + " expected: " + message);
            }
        }
    }

    public static void main(String[] args) throws Exception {

        // Test illegal class that has ACC_VALUE set and a non-static field..
        runTest("AbstractVField", "Illegal field modifiers in class AbstractVField");

        // Test illegal class that has both ACC_FINAL and ACC_VALUE set.
        runTest("AbstractVFinal",
                "Illegal class modifiers in class AbstractVFinal (a value class)");

        // Test illegal class that has both ACC_INTERFACE and ACC_VALUE set.
        runTest("AbstractVintf",
                "Illegal field modifiers in class AbstractVintf: 0x8");

        // Test illegal class that has ACC_VALUE set and a non-static synchronized method.
        runTest("AbstractVMethod",
                "Method meth in class AbstractVMethod (not an identity class) has illegal modifiers: 0x21");
    }
}
