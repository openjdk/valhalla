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
 * @compile HasSynchMethod.java ValidSuper.java ImplementsIdentityObject.java
 * @compile IntfImplementsIdentityObject.java InlineClassWithBadSupers.jcod
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
            "class SuperNotAbstract has an invalid super class NotAbstract");

        // Test that the super class of an inline type cannot have instance fields.
        runTestIncompatibleClassChangeError("SuperHasNonStaticFields",
            "SuperHasNonStaticFields has an invalid super class HasNonStaticFields");

        // Test that the super class of an inline type cannot contain a synchronized instance method.
        runTestIncompatibleClassChangeError("SuperHasSynchMethod",
            "SuperHasSynchMethod has an invalid super class ValidSuper");

        // Test that the constructor in a super class of an inline type must have a signature of "()V".
        runTestIncompatibleClassChangeError("SuperCtorHasArgs",
            "SuperCtorHasArgs has an invalid super class CtorHasArgs");

        // Test that the constructor in a super class of an inline type must be empty.
        runTestIncompatibleClassChangeError("SuperCtorIsNotEmpty",
            "SuperCtorIsNotEmpty has an invalid super class CtorIsNotEmpty");

        // Test that an inline class cannot implement java.lang.IdentityObject.
        runTestIncompatibleClassChangeError("InlineImplementsIdentityObject",
            "attempts to implement interface java.lang.IdentityObject");

        // Test that an inline class's super type cannot implement java.lang.IdentityObject.
        runTestIncompatibleClassChangeError("SuperImplementsIdentityObject",
            "SuperImplementsIdentityObject has an invalid super class ImplementsIdentityObject");

        // Test that an inline class's super type's interfaces cannot implement java.lang.IdentityObject.
        runTestIncompatibleClassChangeError("SuperIntfImplementsIdentityObject",
            "SuperIntfImplementsIdentityObject has an invalid super class IntfImplementsIdentityObject");
    }
}
