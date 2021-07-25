/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/*
 * @test
 * @bug 8210906
 * @summary [lworld] default value creation should not impose raw types on users.
 * @run main PolyDefaultArgs
 */
public primitive class PolyDefaultArgs<T> {

    static void m(PolyDefaultArgs<String> p) { }

    static <T>void check(PolyDefaultArgs.ref<T> rp) {
    	if (rp != null) {
    		throw new RuntimeException("reference projection must be null");
    	}
    }

    public static void main(String [] args) {
        // Ensure that the arguments to m() can be inferred
        m(new PolyDefaultArgs<>()); // OK
        m(PolyDefaultArgs<String>.default); // OK
        m(PolyDefaultArgs.default); // Now also OK

        // Ensure that the default of a reference type is null
        check(PolyDefaultArgs.ref<String>.default);
        check(PolyDefaultArgs.ref.default);
    }
}
