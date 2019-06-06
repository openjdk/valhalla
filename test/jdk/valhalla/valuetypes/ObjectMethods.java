/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test Object methods on inline types
 * @compile -XDallowWithFieldOperator Point.java  Line.java MutablePath.java MixedValues.java Value.java
 * @build ObjectMethods
 * @run testng/othervm -XX:+EnableValhalla -Xcomp -Dvalue.bsm.salt=1 ObjectMethods
 * @run testng/othervm -XX:+EnableValhalla -Xcomp -Dvalue.bsm.salt=1 -XX:ValueFieldMaxFlatSize=0 ObjectMethods
 */

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ObjectMethods {
    static final int SALT = 1;
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
            // reference classes containing fields of inline type
            { MUTABLE_PATH, MutablePath.makePath(10, 20, 30, 40), false},
            { MIXED_VALUES, MIXED_VALUES, true},
            { MIXED_VALUES, new MixedValues(P1, LINE1, MUTABLE_PATH, "value"), false},
            // uninitialized default value
            { MyValue1.default, MyValue1.default, true},
            { MyValue1.default, new MyValue1(0,0, null), true},
            { new MyValue1(10, 20, P1), new MyValue1(10, 20, Point.makePoint(1,2)), true},
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
              "float_v=0.0 number_v=99 point_v=[Point x=0 y=0] ref_v=[ref]]" },
            // enclosing instance field `this$0` should be filtered
            { MyValue1.default, "[ObjectMethods$MyValue1 p=[Point x=0 y=0] np=null]" },
            { new MyValue1(0,0, null), "[ObjectMethods$MyValue1 p=[Point x=0 y=0] np=null]" },
            { new MyValue1(0,0, P1), "[ObjectMethods$MyValue1 p=[Point x=0 y=0] np=[Point x=1 y=2]]" },
        };
    }

    @Test(dataProvider="toStringTests")
    public void testToString(Object o, String s) {
        assertTrue(o.toString().equals(s), o.toString());
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
        // this is sensitive to the order of the returned fields from Class::getDeclaredFields
        return new Object[][]{
            { P1,                   hash(Point.class, 1, 2) },
            { LINE1,                hash(Line.class, Point.makePoint(1, 2), Point.makePoint(3, 4)) },
            { v,                    hash(hashCodeComponents(v))},
            { Point.makePoint(0,0), hash(Point.class, 0, 0) },
            { Point.default,        hash(Point.class, 0, 0) },
            { MyValue1.default,     hash(MyValue1.class, Point.default, null) },
            { new MyValue1(0, 0, null), hash(MyValue1.class, Point.makePoint(0,0), null) },
        };
    }

    @Test(dataProvider="hashcodeTests")
    public void testHashCode(Object o, int hash) {
        assertEquals(o.hashCode(), hash);
    }

    private static Object[] hashCodeComponents(Object o) {
        Class<?> type = o.getClass();
        // filter static fields and synthetic fields
        Stream<Object> fields = Arrays.stream(type.getDeclaredFields())
            .filter(f -> !Modifier.isStatic(f.getModifiers()) && !f.isSynthetic())
            .map(f -> {
                try {
                    return f.get(o);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });
        return Stream.concat(Stream.of(type), fields).toArray(Object[]::new);
    }

    private static int hash(Object... values) {
        int hc = SALT;
        for (Object o : values) {
            hc = 31 * hc + (o != null ? o.hashCode() : 0);
        }
        return hc;
    }

    static inline class MyValue1 {
        private Point p;
        private Point? np;

        MyValue1(int x, int y, Point? np) {
            this.p = Point.makePoint(x, y);
            this.np = np;
        }
    }
}
