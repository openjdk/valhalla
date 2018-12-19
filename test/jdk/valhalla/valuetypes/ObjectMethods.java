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


/*
 * @test
 * @summary test Object methods on value types
 * @compile -XDallowWithFieldOperator ObjectMethods.java
 * @run testng/othervm -XX:+EnableValhalla -Dvalue.bsm.salt=1 ObjectMethods
 */

import java.util.List;
import java.util.Objects;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ObjectMethods {
    static final Point P1 = Point.makePoint(1, 2);
    static final Point P2 = Point.makePoint(30, 40);
    static final Line LINE1 = Line.makeLine(1, 2, 3, 4);
    static final Line LINE2 = Line.makeLine(10, 20, 3, 4);
    static final MutablePath MUTABLE_PATH = MutablePath.makePath(10, 20, 30, 40);
    static final MixedValues MIXED_VALUES = new MixedValues(P1, LINE1, MUTABLE_PATH, "value");
    static final Value VALUE = new Value.Builder()
                                        .setChar('z')
                                        .setBoolean(false)
                                        .setByte((byte)0x1)
                                        .setShort((short)3)
                                        .setLong(4L)
                                        .setPoint(Point.makePoint(200, 200))
                                        .setNumber(Value.Number.intValue(10)).build();

    @DataProvider(name="equalsTests")
    Object[][] equalsTests() {
        return new Object[][]{
            { P1, P1, true},
            { P1, Point.makePoint(1, 2), true},
            { P1, P2, false},
            { P1, LINE1, false},
            { LINE1, Line.makeLine(1, 2, 3, 4), true},
            { LINE1, LINE2, false},
            { LINE1, LINE1, true},
            { VALUE, new Value.Builder()
                              .setChar('z')
                              .setBoolean(false)
                              .setByte((byte)0x1)
                              .setShort((short)3)
                              .setLong(4L)
                              .setPoint(Point.makePoint(200, 200))
                              .setNumber(Value.Number.intValue(10)).build(), true},
            { new Value.Builder().setNumber(new Value.IntNumber(10)).build(),
              new Value.Builder().setNumber(new Value.IntNumber(10)).build(), false},
            // reference classes containing value fields
            { MUTABLE_PATH, MutablePath.makePath(10, 20, 30, 40), false},
            { MIXED_VALUES, MIXED_VALUES, true},
            { MIXED_VALUES, new MixedValues(P1, LINE1, MUTABLE_PATH, "value"), false},
        };
    }

    @Test(dataProvider="equalsTests")
    public void testEquals(Object o1, Object o2, boolean expected) {
        assertTrue(o1.equals(o2) == expected);
    }

    @DataProvider(name="toStringTests")
    Object[][] toStringTests() {
        return new Object[][] {
            { Point.makePoint(100, 200), "[Point x=100 y=200]" },
            { Line.makeLine(1, 2, 3, 4), "[Line p1=[Point x=1 y=2] p2=[Point x=3 y=4]]"},
            { new Value.Builder()
                       .setChar('z')
                       .setBoolean(false)
                       .setByte((byte)0x1)
                       .setShort((short)3)
                       .setLong(4L)
                       .setPoint(Point.makePoint(200, 200))
                       .setNumber(Value.Number.intValue(10)).build(),
              "[Value char_v=z byte_v=1 boolean_v=false int_v=0 short_v=3 long_v=4 double_v=0.0 " +
              "float_v=0.0 number_v=[Value$IntValue i=10] point_v=[Point x=200 y=200] ref_v=null]" },
            { new Value.Builder()
                .setReference(List.of("ref"))
                .setNumber(new Value.IntNumber(99)).build(),
              "[Value char_v=\u0000 byte_v=0 boolean_v=false int_v=0 short_v=0 long_v=0 double_v=0.0 " +
              "float_v=0.0 number_v=99 point_v=[Point x=0 y=0] ref_v=[ref]]" }
        };
    }

    @Test(dataProvider="toStringTests")
    public void testToString(Object o, String s) {
        assertTrue(o.toString().equals(s));
    }

    @DataProvider(name="hashcodeTests")
    Object[][] hashcodeTests() {
        Value v = new Value.Builder()
                           .setChar('z')
                           .setBoolean(false)
                           .setByte((byte)0x1)
                           .setShort((short)3)
                           .setLong(4L)
                           .setFloat(1.2f)
                           .setDouble(0.5)
                           .setPoint(Point.makePoint(200, 200))
                           .setNumber(Value.Number.intValue(10))
                           .setReference(new Object()).build();

        return new Object[][]{
            { P1,           hash(Point.class.asValueType(), 100, 200) },
            { LINE1,        hash(Line.class.asValueType(), 1, 2, 3, 4) },
            { v, hash(Value.class.asValueType(), v.char_v, v.boolean_v, v.byte_v, v.short_v,
                      v.long_v, v.float_v, v.double_v, v.int_v, v.point_v, v.number_v, v.ref_v) }
        };
    }

    @Test(dataProvider="hashcodeTests")
    public void testHashCode(Object o, int hash) {
        assertTrue(o.hashCode() != hash);
    }

    private static int hash(Object... values) {
        int hc = 1;
        for (Object o : values) {
            hc = 31 * hc + o.hashCode();
        }
        return hc;
    }
}
