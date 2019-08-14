/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Asserts;

/*
 * @test
 * @summary Test circularity in static fields
 * @library /test/lib
 * @compile StaticFieldsTest.java
 * @run main/othervm -Xint -XX:+EnableValhalla runtime.valhalla.valuetypes.StaticFieldsTest
 */

public class StaticFieldsTest {


    // ClassA and ClassB have a simple cycle in their static fields, but they should
    // be able to load and initialize themselves successfully. Access to these
    // static fields after their initialization should return the default value.
    static inline class ClassA {
	static ClassB b;
	public int i;
	
	public ClassA() {
	    i = 3;
	}
    }

    static inline class ClassB {
	static ClassA a;
	public int i;
	
	public ClassB() {
	    i = 700;
	}
    }

    // ClassC has a reference to itself in its static field, but it should be able
    // to initialize itelf successfully. Access to this static field after initialization
    // should return the default value.
    static inline class ClassC {
	static ClassC c;
	int i;

	public ClassC() {
	    i = 42;
	}
    }


    // ClassD and ClassE have circular references in their static fields, and they
    // read these static fields during their initialization, the value read from
    // these fields should be the default value. Both classes should initialize
    // successfully.
    static inline class ClassD {
	static ClassE e;
	int i;

	static {
	    Asserts.assertEquals(e.i, 0, "Static field e.i incorrect");
	}
	
	public ClassD() {
	    i = 42;
	}
    }

    static inline class ClassE {
	static ClassD d;
	int i;

	static {
	    Asserts.assertEquals(d.i, 0, "Static field d.i incorrect");
	}
	
	public ClassE() {
	    i = 42;
	}
    }

    // ClassF and ClassG have circular references in their static fields, and they
    // create new instances of each other type to initialize these static fields
    // during their initialization. Both classes should initialize successfully.
    static inline class ClassF {
	static ClassG g;
	int i;

	static {
	    g = new ClassG();
	    Asserts.assertEquals(g.i, 64, "Static field ClassF.g.i incorrect");
	}

	ClassF() {
	    i = 314;
	}
    }

    static inline class ClassG {
	static ClassF f;
	int i;

	static {
	    f = new ClassF();
	    Asserts.assertEquals(f.i, 314, "Static field ClassG.f.i incorrect");
	}

	ClassG() {
	    i = 64;
	}
    }
    
    public static void main(String[] args) {
	Asserts.assertEquals(ClassA.b.i, 0, "Static field ClassA.b.i incorrect");
	Asserts.assertEquals(ClassB.a.i, 0, "Static field Classb.a.i incorrect");
	Asserts.assertEquals(ClassC.c.i, 0, "Static field ClassC.c.i incorrect");
	new ClassD();
	new ClassF();
    }
}
