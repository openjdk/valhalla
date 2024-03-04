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
 * @ignore
 * @test ACC_ICCETest
 * @bug 8281279
 * @summary test that a value class cannot sub-class an identity class
 * @compile ACCICCETests.jcod
 * @run main/othervm -XX:+EnableValhalla -XX:-EnablePrimitiveClasses ACC_ICCETest
 */

public class ACC_ICCETest {

    public static void runTest(String test_name, String message) throws Exception {
        System.out.println("Testing: " + test_name);
        try {
            Class newClass = Class.forName(test_name);
        } catch (java.lang.IncompatibleClassChangeError e) {
            if (!e.getMessage().contains(message)) {
                throw new RuntimeException( "Wrong IncompatibleClassChangeError: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        runTest("Dot", "Value type Dot has an identity type as supertype");
    }
}
