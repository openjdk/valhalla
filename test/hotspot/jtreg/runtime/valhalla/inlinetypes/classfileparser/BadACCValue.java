/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @summary test that if a class file has ACC_VALUE set then it must be run
 *          with option -XX:+EnableValhalla.
 * @compile cfpTests.jcod
 * @run main/othervm -XX:-EnableValhalla BadACCValue
 */

public class BadACCValue {

    public static void runTest(String test_name, String message) throws Exception {
        System.out.println("Testing: " + test_name);
        try {
            Class newClass = Class.forName(test_name);
        } catch (java.lang.ClassFormatError e) {
            if (!e.getMessage().contains(message)) {
                throw new RuntimeException( "Wrong ClassFormatError: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) throws Exception {

        // Test ACC_VALUE causes a CFE unless -XX:+EnableValhalla is specified.
        runTest("ValueFieldNotFinal",
                "Class modifier ACC_VALUE in class ValueFieldNotFinal requires option -XX:+EnableValhalla");
    }
}
