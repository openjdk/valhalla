/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8243204
 * @summary test that the JVM detects illegal super classes for inline types.
 * @compile NotAbstract.java HasNonStaticFields.java CtorHasArgs.java CtorIsNotEmpty.java
 * @compile HasSynchMethod.java ValidSuper.java InlineClassWithBadSupers.jcod
 * @run main/othervm -verify TestSuperClasses
 */

public class TestSuperClasses {

    public static void runTestIncompatibleClassChangeError(String test_name, String message) throws Exception {
        System.out.println("Testing: " + test_name);
        try {
            Class newClass = Class.forName(test_name);
            throw new RuntimeException("Expected IncompatibleClassChangeError exception not thrown");
        } catch (java.lang.IncompatibleClassChangeError e) {
            if (!e.getMessage().contains(message)) {
                throw new RuntimeException("Wrong IncompatibleClassChangeError: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) throws Exception {

        // Test that the super class of an inline type must be java.lang.Object or an abstract class.
        runTestIncompatibleClassChangeError("SuperNotAbstract",
            "has a super class NotAbstract that is not abstract");

        // Test that the super class of an inline type cannot have instance fields.
        runTestIncompatibleClassChangeError("SuperHasNonStaticFields",
            "has a super class HasNonStaticFields containing instance fields");

        // Test that the super class of an inline type cannot contain a synchronized instance method.
        runTestIncompatibleClassChangeError("SuperHasSynchMethod",
            "has a super class HasSynchMethod containing a synchronized instance method");

        // Test that the constructor in a super class of an inline type must have a signature of "()V".
        runTestIncompatibleClassChangeError("SuperCtorHasArgs",
            "has a super class CtorHasArgs containing a constructor with a non-void signature");

        // Test that the constructor in a super class of an inline type must be empty.
        runTestIncompatibleClassChangeError("SuperCtorIsNotEmpty",
            "has a super class CtorIsNotEmpty containing a non-empty constructor");
    }
}
