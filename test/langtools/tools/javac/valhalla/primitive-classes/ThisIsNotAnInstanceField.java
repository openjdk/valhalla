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
 * @bug 8244235
 * @summary Javac mistakenly treats references to _this_ as a reference to an instance field
 * @run main ThisIsNotAnInstanceField
 */

public primitive class ThisIsNotAnInstanceField {

    int i = 513;

    Inner.ref i2 = new Inner();

    primitive class Inner {
        int c = 511;
    }

    static public void main(String[] args) {
        ThisIsNotAnInstanceField t1 = new ThisIsNotAnInstanceField();
        if (t1.i + t1.i2.c != 1024)
            throw new AssertionError("Unexpected: " + Integer.valueOf(t1.i + t1.i2.c));
    }
}
