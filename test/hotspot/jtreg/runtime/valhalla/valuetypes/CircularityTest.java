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
 * @summary Test initialization of static inline fields with circularity
 * @library /test/lib
 * @compile CircularityTest.java
 * @run main/othervm -Xint -XX:+EnableValhalla runtime.valhalla.valuetypes.CircularityTest
 */


public class CircularityTest {
    static boolean b = true;
    static int counter = 0;
    
    static inline class A {
	static B b;
	static C c;
	int i = 0;
    }

    static inline class B {
	static {
	    Asserts.assertNotNull(A.c, "Should have returned C's default value");
	}
	int i = 0;
    }

    static inline class C {
	int i;
	public C(int i) {
	    this.i = i;
	}
    }

    static inline class D {
	static C c;
	int i = 0;
	static {
	    if (CircularityTest.b) {
		// throw an exception to cause D's initialization to fail
		throw new RuntimeException();
	    }
	}
    }

    static inline class E {
	static F f;
	static C c;
	int i = 0;
    }

    static inline class F {
	int i = 0;
	static {
	    E.c = new C(5);
	}
    }

    static inline class G {
	static H h;
	int i = 0;
    }

    static inline class H {
	int i = 0;
	static {
	    if (CircularityTest.b) {
		// throw an exception to cause H's initialization to fail
		throw new RuntimeException();
	    }
	}
    }

    static inline class I {
	static J j;
	static H h;
	int i = 0;
    }

    static inline class J {
	int i = 0;
	static {
	    CircularityTest.counter = 1;
	    H h = I.h;
	    CircularityTest.counter = 2;
	}
    }
    
    static public void main(String[] args) {
	Throwable exception = null;
	// Test 1:
	// Initialization of A will trigger initialization of B which, in its static
	// initializer will access a static inline field c of A that has not been initialized
	// yet. The access must succeed (no exception) because the field is being
	// accessed during the initialization of D, by the thread initializing D,
	// and the value must be the default value of C (not null).
	try {
	    A a = new A();
	} catch (Throwable t) {
	    exception = t;
	}
	Asserts.assertNull(exception, "Circularity should not have caused exceptions");
	
	// Test 2:
	// Class D will fail to initialized (exception thrown in its static initializer).
	// Attempt to access a static inline field of D *after* its failed initialization
	// should trigger an exception.
	exception = null;
	try {
	    D d = new D();
	} catch (Throwable t) {
	    // ignoring the exception thrown to cause initialization failure
	}
	try {
	    C c = D.c;
	} catch (Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "Accessing static fields of a class which failed to initialized should throw an exception");
	Asserts.assertEquals(exception.getClass(), java.lang.NoClassDefFoundError.class, "Wrong exception class");
	// Test 3:
	// Initialization of E will trigger the initialization of F which, in its static initalizer,
	// will initialized a static inline field of F before the JVM does. The JVM must not
	// overwrite the value set by user code.
	E e = new E();
	Asserts.assertEquals(E.c.i, 5, "JVM must not overwrite fields initialized by user code");
	
	// Test 4:
	// Initialization of G should fail because its static inline field h
	exception = null;
	try {
	    G g = new G();
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "G's initialization should have failed");
	Asserts.assertEquals(exception.getClass(), java.lang.ExceptionInInitializerError.class, "Wrong exception");
	
	// Test 5:
	// Initialization of of I should fail when J tries to access I.h
	exception = null;
	try {
	    I i = new I();
	} catch(Throwable t) {
	    exception = t;
	}
	Asserts.assertNotNull(exception, "I's initialization should have failed");
	Asserts.assertEquals(exception.getClass(), java.lang.NoClassDefFoundError.class, "Wrong exception");
	Asserts.assertEquals(CircularityTest.counter, 1, "Didn't failed at the right place");
    }    
}
