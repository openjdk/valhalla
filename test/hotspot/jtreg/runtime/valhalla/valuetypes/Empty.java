/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

value final class EmptyValue {

    private EmptyValue() {
    }

    public static EmptyValue createEmptyValue() {
        EmptyValue e = EmptyValue.default;
        return e;
    }
}

class EmptyTest {
    public void run() {
	EmptyValue.createEmptyValue();
	throw new RuntimeException("Expected class file parse error");
    }
}

/**
 * @test Empty
 * @summary Test empty value type
 * @compile -XDemitQtypes -XDenableValueTypes -XDallowEmptyValues Empty.java
 * @run main/othervm -Xint -XX:+EnableValhalla runtime.valhalla.valuetypes.Empty
 * @run main/othervm -Xcomp -XX:+EnableValhalla runtime.valhalla.valuetypes.Empty
 */
public class Empty {
    public static void main(String[] args) {
	try {
	    EmptyTest test = new EmptyTest();
	    test.run();
	} catch (ClassFormatError cfe) {}
    }
}
