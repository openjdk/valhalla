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

final class ContainerValue1 {
    static TestValue1 staticValueField;
    TestValue1 nonStaticValueField;
    TestValue1[] valueArray;
}

public __ByValue final class TestValue1 {

    static __NotFlattened TestValue1 staticValue = getInstance();

    final int i;
    final String name;

    private TestValue1() {
        i = (int)System.nanoTime();
        name = Integer.valueOf(i).toString();
    }

    public static TestValue1 create(int i) {
        TestValue1 v = __MakeDefault TestValue1();
        v = __WithField(v.i, i);
        v = __WithField(v.name, Integer.valueOf(i).toString());
        return v;
    }

    public static TestValue1 create() {
        TestValue1 v = __MakeDefault TestValue1();
        v = __WithField(v.i, (int)System.nanoTime());
        v = __WithField(v.name, Integer.valueOf(v.i).toString());
        return v;
    }

    public static TestValue1 getInstance() {
        return create();
    }

    public static TestValue1 getNonBufferedInstance() {
        return staticValue;
    }

    public boolean verify() {
        if (name == null) return i == 0;
        return Integer.valueOf(i).toString().compareTo(name) == 0;
    }
}
