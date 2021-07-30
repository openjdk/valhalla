/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8269756
 * @summary test scenarios where getfield, putfield, and withfield access the
 *          same constant pool field_ref and test other withfield error cases.
 * @compile withfieldTests.jcod
 * @run main/othervm -Xverify:remote RunWithfieldTests
 */

public class RunWithfieldTests {

    public static void main(String argv[]) throws Throwable {

        // Check that a withfield on a CONSTANT_Field_info entry that references
        // an identity object will fail and that subsequent putfield and getfield
        // operations on the same CONSTANT_FIELD_info entry will succeed.
        Class wfoClass = Class.forName("withfieldObject");
        withfieldObject wfo = (withfieldObject)wfoClass.getDeclaredConstructor().newInstance();
        String y = wfo.getfield();
        if (!y.equals("cde")) {
            throw new RuntimeException("Unexpected value of wfo.getfield(): " + y);
        }


        // Check that a putfield and getfield on a CONSTANT_Field_info entry that
        // references an identity object will succeed and that a subsequent withfield
        // operation on the same CONSTANT_FIELD_info entry will fail.
        Class pfoClass = Class.forName("putfieldObject");
        putfieldObject pfo = (putfieldObject)pfoClass.getDeclaredConstructor().newInstance();
        String x = pfo.getfield();
        if (!x.equals("abc")) {
            throw new RuntimeException("Unexpected value of pfo.getfield(): " + x);
        }
        try {
            pfo.withfieldFunc();
            throw new RuntimeException("ICCE not thrown");
        } catch (IncompatibleClassChangeError e) {
            if (!e.getMessage().contains("withfield cannot be used on identity class")) {
                throw new RuntimeException("Wrong ICCE thrown: " + e.getMessage());
            }
        }


        // Check that a putfield on a CONSTANT_Field_info entry that references
        // a primitive object will fail and that subsequent withfield and getfield
        // operations on the same CONSTANT_FIELD_info entry will succeed.
        try {
            putfieldPrimitive pfp = new putfieldPrimitive(false);  // putfield on a primitive class
            throw new RuntimeException("ICCE not thrown");
        } catch (IncompatibleClassChangeError e) {
            if (!e.getMessage().contains("putfield cannot be used on primitive class")) {
                throw new RuntimeException("Wrong ICCE thrown: " + e.getMessage());
            }
        }
        putfieldPrimitive pfp = new putfieldPrimitive(true);  // withfield on a primitive class
        if (pfp.getX() != 5) {
            throw new RuntimeException("Unexpected value of d.getfield(): " + pfp.getX());
        }


        // Check that a withfield and getfield on a CONSTANT_Field_info entry that
        // references a primitive object will succeed and that a subsequent putfield
        // operation on the same CONSTANT_FIELD_info entry will fail.
        withfieldPrimitive wfp = new withfieldPrimitive(true);  // withfield on a primitive class
        if (wfp.getX() != 5) {
            throw new RuntimeException("Unexpected value of d.getfield(): " + wfp.getX());
        }
        try {
            withfieldPrimitive wfp2 = new withfieldPrimitive(false);  // putfield on a primitive class
            throw new RuntimeException("ICCE not thrown");
        } catch (IncompatibleClassChangeError e) {
            if (!e.getMessage().contains("putfield cannot be used on primitive class")) {
                throw new RuntimeException("Wrong ICCE thrown: " + e.getMessage());
            }
        }


        // Test withfield with a null stack operand.
        try {
            withfieldNull wfn = new withfieldNull();
            throw new RuntimeException("NPE not thrown");
        } catch (NullPointerException e) {
            if (!e.getMessage().contains("Cannot assign field \"x\"")) {
                throw new RuntimeException("Wrong NPE thrown: " + e.getMessage());
            }
        }


        // Test that a VerifyError exception is thrown for a withfield bytecode if the
        // stack operand is a different primitive type than the primitive type in the
        // constant pool field_ref.
        try {
            WrongPrimWF wPrim = new WrongPrimWF();
            throw new RuntimeException("No exception thrown");
        } catch (VerifyError e) {
            if (!e.getMessage().contains("Bad type on operand stack")) {
                throw new RuntimeException("Wrong VerifyError thrown: " + e.getMessage());
            }
        }
    }
}
