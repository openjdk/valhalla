/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test Object methods on primitive classes
 * @run testng/othervm -Xint -Dvalue.bsm.salt=1 ObjectMethods
 * @run testng/othervm -Dvalue.bsm.salt=1 -XX:InlineFieldMaxFlatSize=0 ObjectMethods
 */

/* To be enabled by JDK-8267932
 * @run testng/othervm -Xcomp -Dvalue.bsm.salt=1 ObjectMethods
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
    static final Value VALUE1 = new Value.Builder()
                                        .setChar('z')
                                        .setBoolean(false)
                                        .setByte((byte)0x1)
                                        .setShort((short)3)
                                        .setLong(4L)
                                        .setPoint(Point.makePoint(100, 100))
                                        .setPointRef(Point.makePoint(200, 200))
                                        .setReference(Point.makePoint(300, 300))
                                        .setNumber(Value.Number.intValue(20)).build();
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
            // reference classes containing fields of primitive class
            { MUTABLE_PATH, MutablePath.makePath(10, 20, 30, 40), false},
            { MIXED_VALUES, MIXED_VALUES, true},
            { MIXED_VALUES, new MixedValues(P1, LINE1, MUTABLE_PATH, "value"), false},
            // uninitialized default value
            { MyValue1.default, MyValue1.default, true},
            { MyValue1.default, new MyValue1(0,0, null), true},
            { new MyValue1(10, 20, P1), new MyValue1(10, 20, Point.makePoint(1,2)), true},
            { new ReferenceType0(10), new ReferenceType0(10), true},
            { new ValueType1(10),   new ValueType1(10), true},
            { new ValueType2(10),   new ValueType2(10), true},
            { new ValueType1(20),   new ValueType2(20), false},
            { new ValueType2(20),   new ValueType1(20), true},
            { new ReferenceType0(30), new ValueType1(30), true},
            { new ReferenceType0(30), new ValueType2(30), true},
        };
    }

    @Test(dataProvider="equalsTests")
    public void testEquals(Object o1, Object o2, boolean expected) {
        assertTrue(o1.equals(o2) == expected);
    }

    @DataProvider(name="interfaceEqualsTests")
    Object[][] interfaceEqualsTests() {
        return new Object[][]{
                { new ReferenceType0(10), new ReferenceType0(10), false, true},
                { new ValueType1(10),   new ValueType1(10),   true,  true},
                { new ValueType2(10),   new ValueType2(10),   true,  true},
                { new ValueType1(20),   new ValueType2(20),   false, false},
                { new ValueType2(20),   new ValueType1(20),   false, true},
                { new ReferenceType0(30), new ValueType1(30),   false, true},
                { new ReferenceType0(30), new ValueType2(30),   false, true},
        };
    }


    @Test(dataProvider="interfaceEqualsTests")
    public void testNumber(Number n1, Number n2, boolean isSubstitutable, boolean isEquals) {
        assertTrue((n1 == n2) == isSubstitutable);
        assertTrue(n1.equals(n2) == isEquals);
    }


    @DataProvider(name="toStringTests")
    Object[][] toStringTests() {
        return new Object[][] {
            { Point.makePoint(100, 200)  },
            { Line.makeLine(1, 2, 3, 4) },
            { VALUE },
            { VALUE1 },
            { new Value.Builder()
                        .setReference(List.of("ref"))
                        .setNumber(new Value.IntNumber(99)).build() },
            // enclosing instance field `this$0` should be filtered
            { MyValue1.default },
            { new MyValue1(0,0, null) },
            { new MyValue1(0,0, P1) },
        };
    }

    @Test(dataProvider="toStringTests")
    public void testToString(Object o) {
        String expected = String.format("%s@%s", o.getClass().getName(), Integer.toHexString(o.hashCode()));
        assertEquals(o.toString(), expected);
    }

    @DataProvider(name="hashcodeTests")
    Object[][] hashcodeTests() {
        // this is sensitive to the order of the returned fields from Class::getDeclaredFields
        return new Object[][]{
            { P1,                   hash(Point.class, 1, 2) },
            { LINE1,                hash(Line.class, Point.makePoint(1, 2), Point.makePoint(3, 4)) },
            { VALUE,                hash(hashCodeComponents(VALUE))},
            { VALUE1,               hash(hashCodeComponents(VALUE1))},
            { Point.makePoint(0,0), hash(Point.class, 0, 0) },
            { Point.default,        hash(Point.class, 0, 0) },
            { MyValue1.default,     hash(MyValue1.class, Point.default, null) },
            { new MyValue1(0, 0, null), hash(MyValue1.class, Point.makePoint(0,0), null) },
        };
    }

    @Test(dataProvider="hashcodeTests")
    public void testHashCode(Object o, int hash) {
        assertEquals(o.hashCode(), hash);
        assertEquals(System.identityHashCode(o), hash);
    }

    private static Object[] hashCodeComponents(Object o) {
        Class<?> type = o.getClass();
        // filter static fields
        Stream<Object> fields = Arrays.stream(type.getDeclaredFields())
            .filter(f -> !Modifier.isStatic(f.getModifiers()))
            .map(f -> {
                try {
                    return f.get(o);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });
        if (type.isPrimitiveClass()) {
            type = type.asValueType();
        }
        return Stream.concat(Stream.of(type), fields).toArray(Object[]::new);
    }

    private static int hash(Object... values) {
        int hc = SALT;
        for (Object o : values) {
            hc = 31 * hc + (o != null ? o.hashCode() : 0);
        }
        return hc;
    }

    static primitive class MyValue1 {
        private Point p;
        private Point.ref np;

        MyValue1(int x, int y, Point.ref np) {
            this.p = Point.makePoint(x, y);
            this.np = np;
        }
    }


    interface Number {
        int value();
    }

    static class ReferenceType0 implements Number {
        int i;
        public ReferenceType0(int i) {
            this.i = i;
        }
        public int value() {
            return i;
        }
        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof Number) {
                return this.value() == ((Number)o).value();
            }
            return false;
        }
    }

    static primitive class ValueType1 implements Number {
        int i;
        public ValueType1(int i) {
            this.i = i;
        }
        public int value() {
            return i;
        }
    }

    static primitive class ValueType2 implements Number {
        int i;
        public ValueType2(int i) {
            this.i = i;
        }
        public int value() {
            return i;
        }
        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof Number) {
                return this.value() == ((Number)o).value();
            }
            return false;
        }
    }
}
