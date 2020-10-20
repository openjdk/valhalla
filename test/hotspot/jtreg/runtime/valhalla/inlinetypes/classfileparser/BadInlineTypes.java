/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test that the right exceptions get thrown for bad inline type
 *          class files.
 * @compile cfpTests.jcod
 * @run main/othervm BadInlineTypes
 */

public class BadInlineTypes {

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

        // Test that ACC_INLINE with ACC_ABSTRACT is illegal.
        runTest("ValueAbstract", "Illegal class modifiers in class ValueAbstract");

        // Test that ACC_INLINE with ACC_ENUM is illegal.
        runTest("ValueEnum", "Illegal class modifiers in class ValueEnum");

        // Test that inline type fields must be final.
        runTest("ValueFieldNotFinal", "Illegal field modifiers in class ValueFieldNotFinal");

        // Test that ACC_INLINE with ACC_INTERFACE is illegal.
        runTest("ValueInterface", "Illegal class modifiers in class ValueInterface");

        // Test that inline type instance methods cannot be synchronized.
        runTest("ValueMethodSynch",
                "Method getInt in class ValueMethodSynch (an inline class) has illegal modifiers");

        // Test that a class with an old class file version cannot contain a Q signature.
        runTest("OldClassWithQSig",
                "Field \"this\" in class OldClassWithQSig has illegal signature \"QOldClassWithQSig;\"");

        // Test that a class with an old class file version cannot contain an array Q signature.
        runTest("OldClassWithQArraySig",
                "Field \"ia\" in class OldClassWithQArraySig has illegal signature \"[Qjava/lang/Integer;\"");

        // Test that ClassCircularityError gets detected for instance fields.
        System.out.println("Testing ClassCircularityError for instance fields");
        try {
            Class newClass = Class.forName("Circ");
            throw new RuntimeException( "java.lang.ClassCircularityError exception not thrown!");
        } catch (java.lang.ClassCircularityError e) {
             if (!e.getMessage().contains("Circ")) {
                 throw new RuntimeException( "Wrong ClassCircularityError: " + e.getMessage());
             }
         }

        // Test that ClassCircularityError isn't detected for static fields.
        System.out.println("Testing ClassCircularityError for static fields");
        try {
            Class newClass = Class.forName("CircStaticB");
        } catch (java.lang.ClassCircularityError e) {
             throw new RuntimeException( "java.lang.ClassCircularityError exception thrown!");
         }

        runTest("ValueCloneable", "Inline Types do not support Cloneable");
    }
}
