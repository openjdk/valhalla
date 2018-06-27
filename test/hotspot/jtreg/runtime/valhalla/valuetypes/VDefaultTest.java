/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Asserts;

/*
 * @test VDefaultTest
 * @summary vdefault bytecode test
 * @library /test/lib
 * @compile -XDenableValueTypes Point.java
 * @compile -XDenableValueTypes -XDallowFlattenabilityModifiers VDefaultTest.java
 * @run main/othervm -Xint -XX:+EnableValhalla runtime.valhalla.valuetypes.VDefaultTest
 * @run main/othervm -Xcomp -XX:+EnableValhalla runtime.valhalla.valuetypes.VDefaultTest
 */

public class VDefaultTest {

    static __ByValue final class Point {
        final int x;
        final int y;

        static Point make() {
            Point p = __MakeDefault Point();
            return p;
        }

        Point() {
            x = 0;
            y = 0;
        }
    }

    static __ByValue final class Value {
        final char c;
        final byte b;
        final short s;
        final int i;
        final long l;
        final float f;
        final double d;
        __Flattenable final Point p;

        static Value make() {
            Value p = __MakeDefault Value();
            return p;
        }

        Value () {
            c = 0;
            b = 0;
            s = 0;
            i = 0;
            l = 0;
            f = 0;
            d = 0;
            p = Point.make();
        }
    }

    public static void main(String[] args) {
        creationTest();
        creationTest();
    }

    static void creationTest() {
        Value v = Value.make();
        Asserts.assertEquals(v.c, (char)0, "invalid char default value");
        Asserts.assertEquals(v.b, (byte)0, "invalid char default value");
        Asserts.assertEquals(v.s, (short)0, "invalid short default value");
        Asserts.assertEquals(v.i, 0, "invalid int default value");
        Asserts.assertEquals(v.l, 0L, "invalid long default value");
        Asserts.assertEquals(v.f, 0.0F, "invalid float default value");
        Asserts.assertEquals(v.d, 0.0D, "invalid double default value");
        Asserts.assertEquals(v.p.x, 0, "invalid embedded value type value");
        Asserts.assertEquals(v.p.y, 0, "invalid embedded value type value");
    }
}

