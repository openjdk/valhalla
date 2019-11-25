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

/*
 * @test VolatileTest
 * @summary check effect of volatile keyword on flattenable fields
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run main/othervm runtime.valhalla.valuetypes.VolatileTest
 */

import jdk.internal.misc.Unsafe;

import java.lang.reflect.*;
import jdk.test.lib.Asserts;

public class VolatileTest {
    static final Unsafe U = Unsafe.getUnsafe();

    static inline class MyValue {
	int i = 0;
	int j = 0;
    }

    static class MyContainer {
	MyValue mv0;
	volatile MyValue mv1;
    }

    static public void main (String[] args) {
	Class<?> c = MyContainer.class;
	Field f0 = null;
	Field f1 = null;
	try {
	    f0 = c.getDeclaredField("mv0");
	    f1 = c.getDeclaredField("mv1");
	} catch(NoSuchFieldException e) {
	    e.printStackTrace();
	    return;
	}
	Asserts.assertTrue(U.isFlattened(f0), "mv0 should be flattened");
	Asserts.assertFalse(U.isFlattened(f1), "mv1 should not be flattened");
    }
}
