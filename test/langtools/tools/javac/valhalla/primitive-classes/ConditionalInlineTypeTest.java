/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8244513
 * @summary Test conditional expression typing involving inlines.
 * @run main ConditionalInlineTypeTest
 */

public class ConditionalInlineTypeTest {

    static primitive class V {}

    public static void main(String [] args) {

        var r1 = args.length == 0 ? new V() : new V();
        System.out.println(r1.getClass());

        var r2 = args.length == 0 ? (V.ref) new V() : (V.ref) new V();
        System.out.println(r2.getClass());

        int npe = 0;
        try {
            var r3 = args.length != 0 ? new V() : (V.ref) null;
            System.out.println(r3.getClass());
        } catch (NullPointerException e) {
            npe++;
        }
        try {
            var r4 = args.length == 0 ? (V.ref) null : new V();
            System.out.println(r4.getClass());
        } catch (NullPointerException e) {
            npe++;
        }
        if (npe != 2) {
            throw new AssertionError("NPEs = " + npe);
        }
    }
}
