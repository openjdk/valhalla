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

/**
 * @test
 * @bug 8207341
 * @summary Test value constructor code with side effects.
 * @run main/othervm -XX:+EnableValhalla SideEffectTest
 */


public class SideEffectTest {

	static inline class V {

        static String output = "";

        int x;

		V() {
            foo(x = 1234);
		}

		V(int x) {
            int l = 1234; 
            foo(l += this.x = x);
		}

        static void foo(int x) {
            output += x;
        }
	}

	public static void main(String[] args) {
		V v = new V();
        if (!v.output.equals("1234"))
            throw new AssertionError("Broken");
        if (!v.toString().equals("[SideEffectTest$V x=1234]"))
            throw new AssertionError("Broken");
		v = new V(8765);
        if (!v.output.equals("12349999"))
            throw new AssertionError("Broken");
        if (!v.toString().equals("[SideEffectTest$V x=8765]"))
            throw new AssertionError("Broken");
	}
}
