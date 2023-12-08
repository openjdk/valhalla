/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.base/java.lang.runtime:open
 *          java.base/jdk.internal.value
 *          java.base/jdk.internal.vm.annotation
 * @compile -XDenablePrimitiveClasses SubstitutabilityTest.java
 * @run junit/othervm -XX:+EnableValhalla SubstitutabilityTest
 */

import java.lang.reflect.Method;
import java.util.stream.Stream;

import jdk.internal.value.ValueClass;
import jdk.internal.vm.annotation.ImplicitlyConstructible;
import jdk.internal.vm.annotation.NullRestricted;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

public class SubstitutabilityTest {
    @ImplicitlyConstructible
    static value class Point {
        public int x;
        public int y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    @ImplicitlyConstructible
    static value class Line {
        @NullRestricted
        Point p1;
        @NullRestricted
        Point p2;

        Line(Point p1, Point p2) {
            this.p1 = p1;
            this.p2 = p2;
        }
        Line(int x1, int y1, int x2, int y2) {
            this(new Point(x1, y1), new Point(x2, y2));
        }
    }

    // contains null-reference and null-restricted fields
    @ImplicitlyConstructible
    static value class MyValue {
        MyValue2 v1;
        @NullRestricted
        MyValue2 v2;
        public MyValue(MyValue2 v1, MyValue2 v2) {
            this.v1 = v1;
            this.v2 = v2;
        }
    }

    @ImplicitlyConstructible
    static value class MyValue2 {
        static int cnt = 0;
        int x;
        MyValue2(int x) {
            this.x = x;
        }
    }

    static Stream<Arguments> substitutableCases() {
        Point p1 = new Point(10, 10);
        Point p2 = new Point(20, 20);
        Line l1 = new Line(p1, p2);
        MyValue v1 = new MyValue(null, ValueClass.zeroInstance(MyValue2.class));
        MyValue v2 = new MyValue(new MyValue2(2), new MyValue2(3));
        MyValue2 value2 = new MyValue2(2);
        MyValue2 value3 = new MyValue2(3);
        MyValue[] va = new MyValue[1];
        return Stream.of(
                Arguments.of(p1, new Point(10, 10)),
                Arguments.of(p2, new Point(20, 20)),
                Arguments.of(l1, new Line(10,10, 20,20)),
                Arguments.of(v1, ValueClass.zeroInstance(MyValue.class)),
                Arguments.of(v2, new MyValue(value2, value3)),
                Arguments.of(va[0], null)
        );
    }

    @ParameterizedTest
    @MethodSource("substitutableCases")
    public void substitutableTest(Object a, Object b) {
        assertTrue(isSubstitutable(a, b));
    }

    static Stream<Arguments> notSubstitutableCases() {
        // MyValue![] va = new MyValue![1];
        MyValue[] va = new MyValue[] { ValueClass.zeroInstance(MyValue.class) };
        Object[] oa = new Object[] { va };
        return Stream.of(
                Arguments.of(new Point(10, 10), new Point(20, 20)),
                /*
                 * Verify ValueObjectMethods::isSubstitutable that does not
                 * throw an exception if any one of parameter is null or if
                 * the parameters are of different types.
                 */
                Arguments.of(va[0], null),
                Arguments.of(null, va[0]),
                Arguments.of(va[0], oa),
                Arguments.of(va[0], oa[0]),
                Arguments.of(va, oa),
                Arguments.of(new Point(10, 10), Integer.valueOf(10)),
                Arguments.of(Integer.valueOf(10), Integer.valueOf(20))
        );
    }

    @ParameterizedTest
    @MethodSource("notSubstitutableCases")
    public void notSubstitutableTest(Object a, Object b) {
        assertFalse(isSubstitutable(a, b));
    }

    @Test
    public void nullArguments() {
        assertTrue(isSubstitutable(null, null));
    }

    private static final Method IS_SUBSTITUTABLE;
    static {
        Method m = null;
        try {
            Class<?> c = Class.forName("java.lang.runtime.ValueObjectMethods");
            m = c.getDeclaredMethod("isSubstitutable", Object.class, Object.class);
            m.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        IS_SUBSTITUTABLE = m;
    }
    private static boolean isSubstitutable(Object a, Object b) {
        try {
            return (boolean) IS_SUBSTITUTABLE.invoke(null, a, b);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
