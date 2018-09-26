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

final class ContainerValue2 {
    static TestValue2 staticValueField;
    TestValue2 nonStaticValueField;
    TestValue2[] valueArray;
}

public value final class TestValue2 {
    static __NotFlattened TestValue2 staticValue = getInstance();

    final long l;
    final double d;
    final String s;

    private TestValue2() {
        l = System.nanoTime();
        s = Long.valueOf(l).toString();
        d = Double.parseDouble(s);
    }

    public static TestValue2 create(long l) {
        TestValue2 v = TestValue2.default;
        v = __WithField(v.l, l);
        v = __WithField(v.s, Long.valueOf(l).toString());
        v = __WithField(v.d, Double.parseDouble(v.s));
        return v;
    }

    public static TestValue2 create() {
        TestValue2 v = TestValue2.default;
        v = __WithField(v.l, System.nanoTime());
        v = __WithField(v.s, Long.valueOf(v.l).toString());
        v = __WithField(v.d, Double.parseDouble(v.s));
        return v;
    }

    public static TestValue2 getInstance() {
        return create();
    }

    public static TestValue2 getNonBufferedInstance() {
        return staticValue;
    }

    public boolean verify() {
        if (s == null) {
            return d == 0 && l == 0;
        }
        return Long.valueOf(l).toString().compareTo(s) == 0
                && Double.parseDouble(s) == d;
    }
}
