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
 */
package runtime.valhalla.valuetypes;

import java.lang.invoke.*;

import jdk.experimental.value.MethodHandleBuilder;

import jdk.test.lib.Asserts;

/*
 * @test
 * @summary Flattenable field semantic test
 * @modules java.base/jdk.experimental.bytecode
 *          java.base/jdk.experimental.value
 * @library /test/lib
 * @compile -XDemitQtypes -XDenableValueTypes -XDallowWithFieldOperator Point.java JumboValue.java
 * @compile -XDemitQtypes -XDenableValueTypes -XDallowWithFieldOperator FlattenableSemanticTest.java
 * @run main/othervm -Xint -XX:ValueFieldMaxFlatSize=64 -XX:+EnableValhalla runtime.valhalla.valuetypes.FlattenableSemanticTest
 * @run main/othervm -Xcomp -XX:+EnableValhalla -XX:ValueFieldMaxFlatSize=64 -XX:+NullableValueTypes runtime.valhalla.valuetypes.FlattenableSemanticTest
 * // debug: -XX:+PrintValueLayout -XX:-ShowMessageBoxOnError
 */
public class FlattenableSemanticTest {

    static Point.box nfsp;
    static Point.val fsp;

    Point.box nfip;
    Point.val fip;

    static JumboValue.box nfsj;
    static JumboValue.val fsj;

    JumboValue.box nfij;
    JumboValue.val fij;

    static Object getNull() {
        return null;
    }

    FlattenableSemanticTest() { }

    public static void main(String[] args) {
        FlattenableSemanticTest test = new FlattenableSemanticTest();

        // Uninitialized value fields must be null for non flattenable fields
        Asserts.assertNull(nfsp, "Invalid non null value for unitialized non flattenable field");
        Asserts.assertNull(nfsj, "Invalid non null value for unitialized non flattenable field");
        Asserts.assertNull(test.nfip, "Invalid non null value for unitialized non flattenable field");
        Asserts.assertNull(test.nfij, "Invalid non null value for unitialized non flattenable field");

        // fsp.equals(null);

        // Uninitialized value fields must be non null for flattenable fields
        Asserts.assertNotNull(fsp, "Invalid null value for unitialized flattenable field");
        Asserts.assertNotNull(fsj, "Invalid null value for unitialized flattenable field");
        Asserts.assertNotNull(test.fip, "Invalid null value for unitialized flattenable field");
        Asserts.assertNotNull(test.fij, "Invalid null value for unitialized flattenable field");

        // Assigning null must be allowed for non flattenable value fields
	boolean exception = true;
	try {
	    nfsp = (Point.box)getNull();
	    nfsp = null;
	    exception = false;
	} catch (NullPointerException e) {
	    exception = true;
	}
	Asserts.assertFalse(exception, "Invalid NPE when assigning null to a non flattenable field");
	
	try {
	    nfsj = (JumboValue.box)getNull();
	    nfsj = null;
	    exception = false;
	} catch (NullPointerException e) {
	    exception = true;
	}
	Asserts.assertFalse(exception, "Invalid NPE when assigning null to a non flattenable field");

	try {
	    test.nfip = (Point.box)getNull();
	    test.nfip = null;
	    exception = false;
	} catch (NullPointerException e) {
	    exception = true;
	}
        Asserts.assertFalse(exception, "Invalid NPE when assigning null to a non flattenable field");
	
	try {
	    test.nfij = (JumboValue.box)getNull();
	    test.nfij = null;
	    exception = false;
	} catch (NullPointerException e) {
	    exception = true;
	}
        Asserts.assertFalse(exception, "Invalid NPE when assigning null to a non flattenable field");

        // Assigning null to a flattenable value field must trigger a NPE
	exception = false;
        try {
            fsp = (Point)getNull();
        } catch(NullPointerException e) {
            exception = true;
        }
        Asserts.assertTrue(exception, "NPE not thrown when assigning null to a flattenable field");
        exception = false;
        try {
            fsj = (JumboValue)getNull();
        } catch(NullPointerException e) {
            exception = true;
        }
        Asserts.assertTrue(exception, "NPE not thrown when assigning null to a flattenable field");
        exception = false;
        try {
            test.fip = (Point)getNull();
        } catch(NullPointerException e) {
            exception = true;
        }
        Asserts.assertTrue(exception, "NPE not thrown when assigning null to a flattenable field");
        exception = false;
        try {
            test.fij = (JumboValue)getNull();
        } catch(NullPointerException e) {
            exception = true;
        }
        Asserts.assertTrue(exception, "NPE not thrown when assigning null to a flattenable field");
        exception = false;
    }

}
