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
 */

package runtime.valhalla.typerestrictions;


/*
 * @test RestrictedTypeAnnotationTest
 * @summary check that code can operate on a field with a restricted type only known by the JVM
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile -XDallowWithFieldOperator RestrictedTypeAnnotationTest.java PointBox.java
 * @run main/othervm -XX:TieredStopAtLevel=1 -XX:CompileThreshold=100 runtime.valhalla.typerestrictions.RestrictedTypeAnnotationTest
 */

import jdk.test.lib.Asserts;
import jdk.internal.misc.Unsafe;
import java.lang.reflect.*;

public class RestrictedTypeAnnotationTest {
	static final int ITERATIONS = 500;
	static final Unsafe U = Unsafe.getUnsafe();
	static double d;

    public static void main(String[] args) {
		PointBox pb = new PointBox();
		Class<?> c = PointBox.class;
		Field p368 = null;
		try {
			p368 = c.getDeclaredField("p368");
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
			return;
		}
		Asserts.assertTrue(U.isFlattened(p368), "field PointBox.p369 should be flattened");
		Class<?> testClass = RestrictedTypeAnnotationTest.class;
		Method[] methods = testClass.getDeclaredMethods();
		for (Method m : methods) {
			if (Modifier.isStatic(m.getModifiers()) &&
				m.getName().startsWith("test") &&
				m.getParameterCount() == 0) {
					System.out.println("Invoking " + m.getName());
					try {
						m.invoke(null);
					} catch(Throwable t) {
						t.printStackTrace();
						return;
					}
				}
		}
    }


    // Reading field x from flattened field p368 without knowing p368 is flattened (getfield)
    static void test1() {
		PointBox pb = new PointBox();
		for (int i = 0; i < ITERATIONS; i++) {
			d = pb.p368.x;
		}
    }

    // Writting to field p368 without knowning p368 is flattened (putfield)
    static void test2() {
		for (int i = 0; i < ITERATIONS; i++) {
			PointBox pb = new PointBox();
			PointBox.Point p = new PointBox.Point(2.0, 3.0);
			pb.p368 = p;
			Asserts.assertEquals(pb.p368, p);
		}
    }

    static PointBox.Point.ref spoint = new PointBox.Point(1.0, 2.0);

    // Trying to write null to field p368 without knowing it is null-free (putfield)
    static void test3() {
		spoint = null;
		for (int i = 0; i < ITERATIONS; i++) {
			PointBox pb = new PointBox();
			Exception e = null;
			try {
				pb.p368 = spoint;
			} catch(NullPointerException npe) {
				e = npe;
			}
			if (e == null) {
				throw new RuntimeException("Missing NPE");
			}
		}
	}

	// Trying to write a value from the wrong type (pufield)
	static void test4() {
		for (int i = 0; i < ITERATIONS; i++) {
			PointBox pb = new PointBox();
			Error e = null;
			try {
				pb.p397 = new String("hello");
			} catch(IncompatibleClassChangeError icce) {
				e = icce;
			}
			if (e == null) {
				throw new RuntimeException("Missing ICCE");
			}
		}
	}

	// Writting to field p368 without knowning p368 is flattened (withfield)
    static void test5() {
		for (int i = 0; i < ITERATIONS; i++) {
			PointBox.Rec rec = new PointBox.Rec();
			PointBox.Point p = new PointBox.Point(2.0, 3.0);
			rec = rec.setp37(p);
			Asserts.assertEquals(rec.p37, p);
		}
    }

    // Trying to write null to field p368 without knowing it is null-free (withfield)
    static void test6() {
		spoint = null;
		for (int i = 0; i < ITERATIONS; i++) {
			PointBox.Rec rec = new PointBox.Rec();
			Exception e = null;
			try {
				rec.setp37(spoint);
			} catch(NullPointerException npe) {
				e = npe;
			}
			if (e == null) {
				throw new RuntimeException("Missing NPE");
			}
		}
	}

	// Trying to write a value from the wrong type (withield)
	static void test7() {
		for (int i = 0; i < ITERATIONS; i++) {
			PointBox.Rec rec = new PointBox.Rec();
			Error e = null;
			try {
				rec.setp23(new String("hello"));
			} catch(IncompatibleClassChangeError icce) {
				e = icce;
			}
			if (e == null) {
				throw new RuntimeException("Missing ICCE");
			}
		}
	}
}
