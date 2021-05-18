/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test that mismatches in bottom class of multi-dimensional
            arrays are correctly detected
 * @library /testlibrary /test/lib
 * @compile MultiANewArrayTypeCheck.jcod MultiANewArrayTest.java Element0.java Element1.java
 * @run main/othervm MultiANewArrayTest
 */

import jdk.test.lib.Asserts;

public class MultiANewArrayTest {

    public static void main(String[] args) {
        Error ex = null;
        try {
            MultiANewArrayTypeCheck.createArray0();
        } catch(Error e) {
            ex = e;
        }
        Asserts.assertNotNull(ex, "An ICCE should have been thrown");
        Asserts.assertEquals(ex.getClass(), IncompatibleClassChangeError.class, "Error is not an ICCE");
        ex = null;
        try {
            MultiANewArrayTypeCheck.createArray1();
        } catch(Error e) {
            ex = e;
        }
        Asserts.assertNull(ex, "No error should have been thrown");
        ex = null;
    }
}
