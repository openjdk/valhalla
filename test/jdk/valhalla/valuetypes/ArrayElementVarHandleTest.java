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
 * @summary test VarHandle on value array
 * @compile -XDallowWithFieldOperator Point.java
 * @compile -XDallowWithFieldOperator Line.java
 * @compile -XDallowWithFieldOperator MutablePath.java
 * @compile -XDallowFlattenabilityModifiers -XDallowWithFieldOperator MixedValues.java NonFlattenValue.java
 * @run testng/othervm -XX:+EnableValhalla -XX:+ValueArrayFlatten ArrayElementVarHandleTest
 * @run testng/othervm -XX:+EnableValhalla -XX:-ValueArrayFlatten ArrayElementVarHandleTest
 */

import java.lang.invoke.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ArrayElementVarHandleTest {
    private static final Point P = Point.makePoint(10, 20);
    private static final Line L = Line.makeLine(10, 20, 30, 40);
    private static final MutablePath PATH = MutablePath.makePath(10, 20, 30, 40);

    @DataProvider(name="arrayTests")
    static Object[][] arrayTests() {
        return new Object[][]{
            new Object[] { Point[].class,
                           new Point[] { Point.makePoint(1, 2),
                                         Point.makePoint(10, 20),
                                         Point.makePoint(100, 200)}},
            new Object[] { Point[][].class,
                           new Point[][] { new Point[] { Point.makePoint(1, 2),
                                                         Point.makePoint(10, 20)}}},
            new Object[] { Line[].class,
                           new Line[] { Line.makeLine(1, 2, 3, 4),
                                        Line.makeLine(10, 20, 30, 40),
                                        Line.makeLine(15, 25, 35, 45),
                                        Line.makeLine(20, 30, 40, 50)}},
            new Object[] { MutablePath[].class,
                           new MutablePath[] { MutablePath.makePath(1, 2, 3, 4),
                                               MutablePath.makePath(10, 20, 30, 40),
                                               MutablePath.makePath(15, 25, 35, 45),
                                               MutablePath.makePath(20, 30, 40, 50)}},
            new Object[] { MixedValues[].class,
                           new MixedValues[] { new MixedValues(P, L, PATH, "mixed", "values")}},
            new Object[] { NonFlattenValue[].class,
                           new NonFlattenValue[] { NonFlattenValue.make(1, 2),
                                                   NonFlattenValue.make(10, 20),
                                                   NonFlattenValue.make(100, 200)}},
        };
    }

    @Test(dataProvider="arrayTests")
    public void set(Class<?> arrayType, Object[] elements) throws Throwable {
        VarHandle vh = MethodHandles.arrayElementVarHandle(arrayType);
        MethodHandle ctor = MethodHandles.arrayConstructor(arrayType);
        Object[] array = (Object[])ctor.invoke(elements.length);
        for (int i=0; i < elements.length; i++) {
            vh.set(array, i, elements[i]);
        }
        for (int i=0; i < elements.length; i++) {
            Object v = (Object)vh.get(array, i);
            assertEquals(v, elements[i]);
        }
    }

    @Test(dataProvider="arrayTests")
    public void setVolatile(Class<?> arrayType, Object[] elements) throws Throwable {
        VarHandle vh = MethodHandles.arrayElementVarHandle(arrayType);
        MethodHandle ctor = MethodHandles.arrayConstructor(arrayType);
        Object[] array = (Object[])ctor.invoke(elements.length);
        for (int i=0; i < elements.length; i++) {
            vh.setVolatile(array, i, elements[i]);
        }
        for (int i=0; i < elements.length; i++) {
            Object v = (Object)vh.getVolatile(array, i);
            assertEquals(v, elements[i]);
        }
    }

    @Test(dataProvider="arrayTests")
    public void setOpaque(Class<?> arrayType, Object[] elements) throws Throwable {
        VarHandle vh = MethodHandles.arrayElementVarHandle(arrayType);
        MethodHandle ctor = MethodHandles.arrayConstructor(arrayType);
        Object[] array = (Object[])ctor.invoke(elements.length);
        for (int i=0; i < elements.length; i++) {
            vh.setOpaque(array, i, elements[i]);
        }
        for (int i=0; i < elements.length; i++) {
            Object v = (Object)vh.getOpaque(array, i);
            assertEquals(v, elements[i]);
        }
    }

    @Test(dataProvider="arrayTests")
    public void getAndSet(Class<?> arrayType, Object[] elements) throws Throwable {
        VarHandle vh = MethodHandles.arrayElementVarHandle(arrayType);
        MethodHandle ctor = MethodHandles.arrayConstructor(arrayType);
        Object[] array = (Object[])ctor.invoke(elements.length);
        for (int i=0; i < elements.length; i++) {
            Object o = vh.getAndSet(array, i, elements[i]);
        }
        for (int i=0; i < elements.length; i++) {
            Object v = (Object)vh.get(array, i);
            assertEquals(v, elements[i]);
        }
    }
}
