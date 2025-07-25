/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test Object methods on value classes
 * @enablePreview
 * @run junit/othervm -Dvalue.bsm.salt=1 -XX:-UseAtomicValueFlattening ObjectMethods
 * @run junit/othervm -Dvalue.bsm.salt=1 -XX:-UseFieldFlattening ObjectMethods
 */
import java.util.Optional;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Modifier;

import jdk.internal.value.ValueClass;
import jdk.internal.vm.annotation.NullRestricted;
import jdk.internal.vm.annotation.Strict;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectMethods {
    static value class Point {
        public int x;
        public int y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    static value class Line {
        @NullRestricted  @Strict
        Point p1;
        @NullRestricted  @Strict
        Point p2;

        Line(int x1, int y1, int x2, int y2) {
            this.p1 = new Point(x1, y1);
            this.p2 = new Point(x2, y2);
        }
    }

    static class Ref {
        @NullRestricted  @Strict
        Point p;
        Line l;
        Ref(Point p, Line l) {
            this.p = p;
            this.l = l;
        }
    }

    static value class Value {
        @NullRestricted  @Strict
        Point p;
        @NullRestricted  @Strict
        Line l;
        Ref r;
        String s;
        Value(Point p, Line l, Ref r, String s) {
            this.p = p;
            this.l = l;
            this.r = r;
            this.s = s;
        }
    }

    static value class ValueOptional {
        private Object o;
        public ValueOptional(Object o) {
            this.o = o;
        }
    }

    static value record ValueRecord(int i, String name) {}

    static final int SALT = 1;
    static final Point P1 = new Point(1, 2);
    static final Point P2 = new Point(30, 40);
    static final Line L1 = new Line(1, 2, 3, 4);
    static final Line L2 = new Line(10, 20, 3, 4);
    static final Ref R1 = new Ref(P1, L1);
    static final Ref R2 = new Ref(P2, null);
    static final Value V = new Value(P1, L1, R1, "value");

    // Instances to test, classes of each instance are tested too
    static Stream<Arguments> identitiesData() {
        Function<String, String> lambda1 = (a) -> "xyz";
        return Stream.of(
                Arguments.of(lambda1, true, false),         // a lambda (Identity for now)
                Arguments.of(new Object(), true, false),    // java.lang.Object
                Arguments.of("String", true, false),
                Arguments.of(L1, false, true),
                Arguments.of(V, false, true),
                Arguments.of(new ValueRecord(1, "B"), false, true),
                Arguments.of(new int[0], true, false),     // arrays of primitive type are identity objects
                Arguments.of(new Object[0], true, false),  // arrays of identity classes are identity objects
                Arguments.of(new String[0], true, false),  // arrays of identity classes are identity objects
                Arguments.of(new Value[0], true, false)    // arrays of value classes are identity objects
        );
    }

    // Classes to test
    static Stream<Arguments> classesData() {
        return Stream.of(
                Arguments.of(int.class, false, true),       // Fabricated primitive classes
                Arguments.of(long.class, false, true),
                Arguments.of(short.class, false, true),
                Arguments.of(byte.class, false, true),
                Arguments.of(float.class, false, true),
                Arguments.of(double.class, false, true),
                Arguments.of(char.class, false, true),
                Arguments.of(void.class, false, true),
                Arguments.of(String.class, true, false),
                Arguments.of(Object.class, true, false),
                Arguments.of(Function.class, false, true),  // Interface
                Arguments.of(Optional.class, false, true),  // Concrete value classes...
                Arguments.of(Character.class, false, true)
        );
    }

    @ParameterizedTest
    @MethodSource("identitiesData")
    public void identityTests(Object obj, boolean identityClass, boolean valueClass) {
        Class<?> clazz = obj.getClass();
        assertEquals(identityClass, Objects.hasIdentity(obj), "Objects.hasIdentity(" + obj + ")");

        // Run tests on the class
        classTests(clazz, identityClass, valueClass);
    }

    @ParameterizedTest
    @MethodSource("classesData")
    public void classTests(Class<?> clazz, boolean identityClass, boolean valueClass) {
        assertEquals(identityClass, clazz.isIdentity(), "Class.isIdentity(): " + clazz);

        assertEquals(valueClass, clazz.isValue(), "Class.isValue(): " + clazz);

        assertEquals(clazz.accessFlags().contains(AccessFlag.IDENTITY),
                identityClass, "AccessFlag.IDENTITY: " + clazz);

        int modifiers = clazz.getModifiers();
        assertEquals(clazz.isIdentity(), (modifiers & Modifier.IDENTITY) != 0, "Class.getModifiers() & IDENTITY != 0");
        assertEquals(clazz.isValue(), (modifiers & Modifier.IDENTITY) == 0, "Class.getModifiers() & IDENTITY == 0");
    }

    @Test
    public void identityTestNull() {
        assertFalse(Objects.hasIdentity(null), "Objects.hasIdentity(null)");
        assertFalse(Objects.isValueObject(null), "Objects.isValueObject(null)");
    }

    static Stream<Arguments> equalsTests() {
        return Stream.of(
                Arguments.of(P1, P1, true),
                Arguments.of(P1, new Point(1, 2), true),
                Arguments.of(P1, P2, false),
                Arguments.of(P1, L1, false),
                Arguments.of(L1, new Line(1, 2, 3, 4), true),
                Arguments.of(L1, L2, false),
                Arguments.of(L1, L1, true),
                Arguments.of(V, new Value(P1, L1, R1, "value"), true),
                Arguments.of(V, new Value(new Point(1, 2), new Line(1, 2, 3, 4), R1, "value"), true),
                Arguments.of(V, new Value(P1, L1, new Ref(P1, L1), "value"), false),
                Arguments.of(new Value(P1, L1, R2, "value2"), new Value(P1, L1, new Ref(P2, null), "value2"), false),
                Arguments.of(new ValueRecord(50, "fifty"), new ValueRecord(50, "fifty"), true),

                // reference classes containing fields of value class
                Arguments.of(R1, new Ref(P1, L1), false),   // identity object

                // uninitialized default value
                Arguments.of(new ValueOptional(L1), new ValueOptional(L1), true),
                Arguments.of(new ValueOptional(List.of(P1)), new ValueOptional(List.of(P1)), false)
        );
    }

    @ParameterizedTest
    @MethodSource("equalsTests")
    public void testEquals(Object o1, Object o2, boolean expected) {
        assertTrue(o1.equals(o2) == expected);
    }

    static Stream<Arguments> toStringTests() {
        return Stream.of(
                Arguments.of(new Point(100, 200)),
                Arguments.of(new Line(1, 2, 3, 4)),
                Arguments.of(V),
                Arguments.of(R1),
                // enclosing instance field `this$0` should be filtered
                Arguments.of(new Value(P1, L1, null, null)),
                Arguments.of(new Value(P2, L2, new Ref(P1, null), "value")),
                Arguments.of(new ValueOptional(P1))
        );
    }

    @ParameterizedTest
    @MethodSource("toStringTests")
    public void testToString(Object o) {
        String expected = String.format("%s@%s", o.getClass().getName(), Integer.toHexString(o.hashCode()));
        assertEquals(o.toString(), expected);
    }

    @Test
    public void testValueRecordToString() {
        ValueRecord o = new ValueRecord(30, "thirty");
        assertEquals(o.toString(), "ValueRecord[i=30, name=thirty]");
    }

    static Stream<Arguments> hashcodeTests() {
        Point p = new Point(0, 0);
        Line l = new Line(0, 0, 0, 0);
        // this is sensitive to the order of the returned fields from Class::getDeclaredFields
        return Stream.of(
                Arguments.of(P1, hash(Point.class, 1, 2)),
                Arguments.of(L1, hash(Line.class, new Point(1, 2), new Point(3, 4))),
                Arguments.of(V, hash(Value.class, P1, L1, V.r, V.s)),
                Arguments.of(new Point(0, 0), hash(Point.class, 0, 0)),
                Arguments.of(p, hash(Point.class, 0, 0)),
                Arguments.of(new ValueOptional(P1), hash(ValueOptional.class, P1))
        );
    }

    @ParameterizedTest
    @MethodSource("hashcodeTests")
    public void testHashCode(Object o, int hash) {
        assertEquals(o.hashCode(), hash);
        assertEquals(System.identityHashCode(o), hash);
    }

    private static int hash(Object... values) {
        int hc = SALT;
        for (Object o : values) {
            hc = 31 * hc + (o != null ? o.hashCode() : 0);
        }
        return hc;
    }

    interface Number {
        int value();
    }

    static class ReferenceType implements Number {
        int i;
        public ReferenceType(int i) {
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

    static value class ValueType1 implements Number {
        int i;
        public ValueType1(int i) {
            this.i = i;
        }
        public int value() {
            return i;
        }
    }

    static value class ValueType2 implements Number {
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

    static Stream<Arguments> interfaceEqualsTests() {
        return Stream.of(
                Arguments.of(new ReferenceType(10), new ReferenceType(10), false, true),
                Arguments.of(new ValueType1(10),    new ValueType1(10),    true,  true),
                Arguments.of(new ValueType2(10),    new ValueType2(10),    true,  true),
                Arguments.of(new ValueType1(20),    new ValueType2(20),    false, false),
                Arguments.of(new ValueType2(20),    new ValueType1(20),    false, true),
                Arguments.of(new ReferenceType(30), new ValueType1(30),    false, true),
                Arguments.of(new ReferenceType(30), new ValueType2(30),    false, true)
        );
    }

    @ParameterizedTest
    @MethodSource("interfaceEqualsTests")
    public void testNumber(Number n1, Number n2, boolean isSubstitutable, boolean isEquals) {
        assertTrue((n1 == n2) == isSubstitutable);
        assertTrue(n1.equals(n2) == isEquals);
    }
}
