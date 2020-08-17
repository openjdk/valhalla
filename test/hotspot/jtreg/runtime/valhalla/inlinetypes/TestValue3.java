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

final class ContainerValue3 {
    static TestValue3.ref staticInlineField;
    TestValue3 nonStaticInlineField;
    TestValue3[] valueArray;
}

public inline final class TestValue3 {

    static TestValue3.ref staticValue = getInstance();

    final byte b;

    private TestValue3() {
        b = 123;
    }

    public static TestValue3 create(byte b) {
        TestValue3 v = TestValue3.default;
        v = __WithField(v.b, b);
        return v;
    }

    public static TestValue3 create() {
        TestValue3 v = TestValue3.default;
        v = __WithField(v.b, 123);
        return v;
    }

    public static TestValue3 getInstance() {
        return create();
    }

    public static TestValue3 getNonBufferedInstance() {
        return (TestValue3) staticValue;
    }

    public boolean verify() {
        return b == 0 || b == 123;
    }
}
