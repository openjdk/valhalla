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
 * @summary test that the right exceptions get thrown for bad value type
 *          class files.
 * @compile verifierTests.jcod
 * @run main/othervm -verify VerifierValueTypes
 */

public class VerifierValueTypes {

    public static void runTestVerifyError(String test_name, String message) throws Exception {
        System.out.println("Testing: " + test_name);
        try {
            Class newClass = Class.forName(test_name);
        } catch (java.lang.VerifyError e) {
            if (!e.getMessage().contains(message)) {
                throw new RuntimeException( "Wrong VerifyError: " + e.getMessage());
            }
        }
    }

    public static void runTestFormatError(String test_name, String message) throws Exception {
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

        // Test that a defaultvalue opcode with an out of bounds cp index causes a VerifyError.
        runTestVerifyError("defValBadCP", "Illegal constant pool index");

        // Test that ClassFormatError is thrown for a class file, with major version 54, that
        // contains a defaultvalue opcode.
        runTestFormatError("defValBadMajorVersion", "defaultvalue not supported by this class file version");

        // Test VerifyError is thrown if a defaultvalue's cp entry is not a class.
        runTestVerifyError("defValWrongCPType", "Illegal type at constant pool entry");

        // Test that a withfield opcode with an out of bounds cp index causes a VerifyError.
        runTestVerifyError("wthFldBadCP", "Illegal constant pool index");

        // Test that VerifyError is thrown if the first operand on the stack is not assignable
        // to withfield's field.
        runTestVerifyError("wthFldBadFldVal", "Bad type on operand stack");

        // Test that VerifyError is thrown if the second operand on the stack is not a reference.
        runTestVerifyError("wthFldBadFldRef", "Bad value type on operand stack in withfield");

        // Test that ClassFormatError is thrown for a class file, with major version 54, that
        // contains a withfield opcode.
        runTestFormatError("wthFldBadMajorVersion", "withfield not supported by this class file version");

        // Test VerifyError is thrown if a withfields's cp entry is not a field.
        runTestVerifyError("wthFldWrongCPType", "Illegal type at constant pool entry");

        // Test that VerifyError is thrown if the class for a withfields's cp fieldref
        // entry is java.lang.Object and the reference on the stack is a value type.
        runTestVerifyError("wthFldObject", "must be identical value types");

        // Test VerifyError is thrown if a new's cp entry is a value type.
        runTestVerifyError("newVT", "Illegal use of value type as operand for new instruction");

        // Test VerifyError is thrown if a monitorenter's cp entry is a value type.
        runTestVerifyError("monEnterVT", "Illegal use of value type as operand for monitorenter");

        // Test VerifyError is thrown if a defaultvalue's cp entry is a value type.
        runTestVerifyError("defValueObj", "Illegal use of an object as operand for defaultvalue");

        // Test VerifyError is thrown if a withfield's class operand is not a value type.
        runTestVerifyError("withfieldObj", "Bad value type on operand stack in withfield");

        // Test VerifyError is thrown if a putfield's class operand is a value type in a
        // method not named '<init>'.
        runTestVerifyError("putfieldVT", "Field for putfield cannot be a member of a value type");
    }
}
