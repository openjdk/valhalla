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
 * @summary test MethodHandle/VarHandle on inline types
 * @run testng/othervm -XX:+EnableValhalla SubstitutabilityTest
 */

import java.lang.invoke.ValueBootstrapMethods;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SubstitutabilityTest {
    @DataProvider(name="substitutable")
    Object[][] substitutableCases() {
        Point p1 = Point.makePoint(10, 10);
        Point p2 = Point.makePoint(20, 20);
        Point? box1 = p1;
        Point? box2 = p2;
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
        };
    }

    @Test(dataProvider="substitutable")
    public void substitutableTest(Object a, Object b) {
        assertTrue(ValueBootstrapMethods.isSubstitutable(a, b));
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
            new Object[] { valueBuilder().setNumber(number).build(),
                           valueBuilder().setNumber(new Value.IntNumber(99)).build() },
            new Object[] { valueBuilder().setNumber(Value.Number.intValue(1)).build(),
                           valueBuilder().setNumber(Value.Number.shortValue((short)1)).build() },
            new Object[] { valueBuilder().setNumber(new Value.IntNumber(99)).build(),
                           valueBuilder().setNumber(new Value.IntNumber(99)).build() },
            new Object[] { valueBuilder().setReference(List.of("list")).build(),
                           valueBuilder().setReference(List.of("list")).build() },
        };
    }
    @Test(dataProvider="notSubstitutable")
    public void notSubstitutableTest(Object a, Object b) {
        assertFalse(ValueBootstrapMethods.isSubstitutable(a, b));
    }
    private static Value.Builder valueBuilder() {
        Value.Builder builder = new Value.Builder();
        return builder.setChar('a')
                       .setBoolean(true)
                       .setByte((byte)0x1)
                       .setShort((short)3)
                       .setLong(4L);
    }
}
