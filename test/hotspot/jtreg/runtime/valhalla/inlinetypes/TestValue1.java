/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
package runtime.valhalla.inlinetypes;

final class ContainerValue1 {
    static TestValue1.ref staticInlineField;
    TestValue1 nonStaticInlineField;
    TestValue1[] inlineArray;
}

public primitive class TestValue1 {

    static TestValue1.ref staticValue = getInstance();

    final int i;
    final String name;

    public TestValue1() {
        i = (int)System.nanoTime();
        name = Integer.valueOf(i).toString();
    }

    public TestValue1(int i) {
        this.i = i;
        name = Integer.valueOf(i).toString();
    }

    public static TestValue1 getInstance() {
        return new TestValue1();
    }

    public static TestValue1 getNonBufferedInstance() {
        return (TestValue1) staticValue;
    }

    public boolean verify() {
        if (name == null) return i == 0;
        return Integer.valueOf(i).toString().compareTo(name) == 0;
    }
}
