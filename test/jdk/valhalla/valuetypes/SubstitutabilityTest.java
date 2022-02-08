/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test MethodHandle/VarHandle on primitive classes
 * @modules java.base/java.lang.runtime:open
 *          java.base/jdk.internal.org.objectweb.asm
 * @run testng SubstitutabilityTest
 */

import java.lang.reflect.Method;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SubstitutabilityTest {

    @DataProvider(name="substitutable")
    Object[][] substitutableCases() {
        Point p1 = Point.makePoint(10, 10);
        Point p2 = Point.makePoint(20, 20);
        Point.ref box1 = p1;
        Point.ref box2 = p2;
        Line l1 = Line.makeLine(p1, p2);
        var mpath = MutablePath.makePath(10, 20, 30, 40);
        var mixedValues = new MixedValues(p1, l1, mpath, "value");
        var number = Value.Number.intValue(99);
        var list = List.of("list");
        return new Object[][] {
            new Object[] { p1, Point.makePoint(10, 10) },
            new Object[] { l1, Line.makeLine(10,10, 20,20) },
            new Object[] { box1, Point.makePoint(10, 10) },
            new Object[] { mpath, mpath},
            new Object[] { mixedValues, mixedValues},
            new Object[] { valueBuilder().setPoint(p1).build(),
                           valueBuilder().setPoint(Point.makePoint(10, 10)).build() },
            new Object[] { valueBuilder().setPointRef(p2).build(),
                           valueBuilder().setPointRef(Point.makePoint(20, 20)).build() },
            new Object[] { valueBuilder().setReference(p2).build(),
                           valueBuilder().setReference(Point.makePoint(20, 20)).build() },
            new Object[] { valueBuilder().setFloat(Float.NaN).setDouble(Double.NaN).setPoint(p1).build(),
                           valueBuilder().setFloat(Float.NaN).setDouble(Double.NaN).setPoint(l1.p1).build() },
            new Object[] { valueBuilder().setFloat(Float.NaN).setDouble(Double.NaN).setNumber(number).build(),
                           valueBuilder().setFloat(Float.NaN).setDouble(Double.NaN).setNumber(Value.Number.intValue(99)).build() },
            new Object[] { valueBuilder().setFloat(+0.0f).setDouble(+0.0).setReference(list).build(),
                           valueBuilder().setFloat(+0.0f).setDouble(+0.0).setReference(list).build() },
            new Object[] { valueBuilder().setNumber(Value.Number.intValue(100)).build(),
                           valueBuilder().setNumber(Value.Number.intValue(100)).build() },
            new Object[] { valueBuilder().setReference(list).build(),
                           valueBuilder().setReference(list).build() },
            new Object[] { new ValueOptional(p1), new ValueOptional(p1)},
            new Object[] { new ValueOptional(p1), new ValueOptional(Point.makePoint(10, 10))},
            new Object[] { new ValueOptional(list), new ValueOptional(list)},
            new Object[] { new ValueOptional(null), new ValueOptional(null)},
        };
    }

    @Test(dataProvider="substitutable")
    public void substitutableTest(Object a, Object b) {
        assertTrue(isSubstitutable(a, b));
    }

    @DataProvider(name="notSubstitutable")
    Object[][] notSubstitutableCases() {
        var point = Point.makePoint(10, 10);
        var mpath = MutablePath.makePath(10, 20, 30, 40);
        var number = Value.Number.intValue(99);
        return new Object[][] {
            new Object[] { Point.makePoint(10, 10), Point.makePoint(10, 20)},
            new Object[] { mpath, MutablePath.makePath(10, 20, 30, 40)},
            new Object[] { point, mpath},
            new Object[] { valueBuilder().setFloat(+0.0f).setDouble(+0.0).build(),
                           valueBuilder().setFloat(-0.0f).setDouble(+0.0).build() },
            new Object[] { valueBuilder().setFloat(+0.0f).setDouble(+0.0).build(),
                           valueBuilder().setFloat(+0.0f).setDouble(-0.0).build() },
            new Object[] { valueBuilder().setPoint(point).build(),
                           valueBuilder().setPoint(Point.makePoint(20, 20)).build() },
            new Object[] { valueBuilder().setPointRef(point).build(),
                           valueBuilder().setPointRef(Point.makePoint(20, 20)).build() },
            new Object[] { valueBuilder().setNumber(number).build(),
                           valueBuilder().setNumber(new Value.IntNumber(99)).build() },
            new Object[] { valueBuilder().setNumber(Value.Number.intValue(1)).build(),
                           valueBuilder().setNumber(Value.Number.shortValue((short)1)).build() },
            new Object[] { valueBuilder().setNumber(new Value.IntNumber(99)).build(),
                           valueBuilder().setNumber(new Value.IntNumber(99)).build() },
            new Object[] { valueBuilder().setReference(List.of("list")).build(),
                           valueBuilder().setReference(List.of("list")).build() },
            new Object[] { new ValueOptional(point), new ValueOptional(mpath)},
            new Object[] { new ValueOptional(Value.Number.intValue(1)), new ValueOptional(Value.Number.shortValue((short)1))},
        };
    }
    @Test(dataProvider="notSubstitutable")
    public void notSubstitutableTest(Object a, Object b) {
        assertFalse(isSubstitutable(a, b));
    }
    private static Value.Builder valueBuilder() {
        Value.Builder builder = new Value.Builder();
        return builder.setChar('a')
                       .setBoolean(true)
                       .setByte((byte)0x1)
                       .setShort((short)3)
                       .setLong(4L);
    }

    static primitive class MyValue {
        static int cnt = 0;
        final int x;
        final MyValue2 vtField1;
        final MyValue2.ref vtField2;

        public MyValue() {
            this.x = ++cnt;
            this.vtField1 = new MyValue2();
            this.vtField2 = new MyValue2();
        }
    }

    static primitive class MyValue2 {
        static int cnt = 0;
        final int x;
        public MyValue2() {
            this.x = ++cnt;
        }
    }

    @Test
    public void uninitializedArrayElement() throws Exception {
        MyValue[] va = new MyValue[1];
        Object[] oa = new Object[] { va };
        for (int i = 0; i < 100; ++i) {
            Object o = zerothElement(((i % 2) == 0) ? va : oa);
            if ((i % 2) == 0) {
                assertTrue(o instanceof MyValue);
                assertTrue(o == va[0]);
                assertFalse(o != va[0]);
                assertTrue(isSubstitutable(o, va[0]));
            } else {
                assertTrue(o.getClass().isArray());
                assertFalse(o == va[0]);
                assertTrue(o != va[0]);
                assertFalse(isSubstitutable(o, va[0]));
            }
        }
    }

    @DataProvider(name="negativeSubstitutableCases")
    Object[][] negativeSubstitutableCases() {
        MyValue[] va = new MyValue[1];
        Object[] oa = new Object[] { va };
        Point p = Point.makePoint(10, 10);
        Integer i = Integer.valueOf(10);
        return new Object[][] {
                new Object[] { va[0], null },
                new Object[] { null,  va[0] },
                new Object[] { va[0], oa },
                new Object[] { va[0], oa[0] },
                new Object[] { va,    oa },
                new Object[] { p,     i },
                new Object[] { i,     Integer.valueOf(20) },
        };
    }

    /*
     * isSubstitutable method handle invoker requires both parameters are
     * non-null and of the same primitive class.
     *
     * This verifies PrimitiveObjectMethods::isSubstitutable that does not
     * throw an exception if any one of parameter is null or if
     * the parameters are of different types.
     */
    @Test(dataProvider="negativeSubstitutableCases")
    public void testIsSubstitutable(Object a, Object b) {
        assertFalse(isSubstitutable(a, b));
    }

    @Test
    public void nullArguments() throws Exception {
        assertTrue(isSubstitutable(null, null));
    }

    private static Object zerothElement(Object[] oa) {
        return oa[0];
    }

    private static final Method IS_SUBSTITUTABLE;
    static {
        Method m = null;
        try {
            Class<?> c = Class.forName("java.lang.runtime.PrimitiveObjectMethods");
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
