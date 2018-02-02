/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.base/valhalla.shady
 *          jdk.incubator.mvt
 * @run testng/othervm -XX:+EnableMVT -XX:+ValueArrayFlatten MVTTest
 * @run testng/othervm -XX:+EnableMVT -XX:-ValueArrayFlatten MVTTest
 * @run testng/othervm -XX:+EnableMVT -Dvalhalla.enableValueLambdaForms=true MVTTest
 * @run testng/othervm -XX:+EnableMVT -Dvalhalla.enableValueLambdaForms=true -Dvalhalla.enablePoolPatches=true MVTTest
 */

import jdk.incubator.mvt.ValueType;
import org.testng.annotations.Test;
import valhalla.shady.MinimalValueTypes_1_0;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import static java.lang.invoke.MethodType.methodType;
import static org.testng.Assert.assertEquals;

@Test
public class MVTTest {
    static final Class<?> DVT;

    static final ValueType<?> VT = ValueType.forClass(Point.class);

    static final Class<?>[] FIELD_TYPES;

    static final String[] FIELD_NAMES;

    static String TEMPLATE = "Point[x=#x, y=#y, z=#z]";

    static final Object[] FIELD_VALUES = {42, (short) 43, (short) 44};

    static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    static final MethodHandle PRINT_POINT;

    static {
        DVT = MinimalValueTypes_1_0.getValueTypeClass(Point.class);

        Field[] fs = Point.class.getFields();

        FIELD_TYPES = new Class<?>[fs.length];
        FIELD_NAMES = new String[fs.length];

        for (int i = 0; i < fs.length; i++) {
            FIELD_TYPES[i] = fs[i].getType();
            FIELD_NAMES[i] = fs[i].getName();
        }

        try {
            PRINT_POINT = LOOKUP.findStatic(MVTTest.class, "print", methodType(String.class, Point.class))
                    .asType(methodType(String.class, DVT));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testDefaultValue() throws Throwable {
        for (int i = 0; i < FIELD_NAMES.length; i++) {
            MethodHandle getter = MethodHandles.collectArguments(
                    VT.findGetter(LOOKUP, FIELD_NAMES[i], FIELD_TYPES[i]),
                    0,
                    VT.defaultValueConstant());

            assertEquals((int) getter.invoke(), 0);
        }
    }

    public void testWither() throws Throwable {
        testWither(Point.lookup());
        testWither(MethodHandles.privateLookupIn(VT.boxClass(), LOOKUP));
        testWither(MethodHandles.privateLookupIn(VT.valueClass(), LOOKUP));
    }

    void testWither(MethodHandles.Lookup l ) throws Throwable {
        for (int i = 0; i < FIELD_NAMES.length; i++) {
            MethodHandle wither = MethodHandles.collectArguments(
                    VT.findWither(l, FIELD_NAMES[i], FIELD_TYPES[i]), 0, VT.defaultValueConstant());
            String expected = TEMPLATE.replace("#" + FIELD_NAMES[i], String.valueOf(FIELD_VALUES[i]))
                    .replaceAll("#[xyz]", "0");

            assertEquals(printReturn(wither).invoke(FIELD_VALUES[i]), expected);
        }
    }

    public void testSubstitutability() throws Throwable {
        Point[] pts = {new Point(1, (short) 6, (short) 3), new Point(1, (short) 2, (short) 3)};

        MethodHandle substTest = VT.substitutabilityTest();
        for (Point p1 : pts) {
            for (Point p2 : pts) {
                assertEquals((boolean) substTest.invoke(p1, p2), p1.equals(p2));
            }
        }

        MethodHandle hash = VT.substitutabilityHashCode();
        for (Point p1 : pts) {
            for (Point p2 : pts) {
                boolean vHashEq = (int) hash.invoke(p1) == (int) hash.invoke(p2);
                boolean rHashEq = p1.hashCode() == p2.hashCode();
                assertEquals(vHashEq, rHashEq);
            }
        }
    }

    public void testIdentity() throws Throwable {
        String actual = (String) printReturn(MethodHandles.identity(VT.valueClass()))
                .invoke(new Point(1, (short) 2, (short) 3));
        assertEquals(actual, "Point[x=1, y=2, z=3]");
    }

    public void testZero() throws Throwable {
        String actual = (String) printReturn(MethodHandles.zero(VT.valueClass()))
                .invoke();
        assertEquals(actual, "Point[x=0, y=0, z=0]");
    }

    public void testEmpty() throws Throwable {
        String actual = (String) printReturn(MethodHandles.empty(methodType(VT.valueClass(), int.class, String.class)))
                .invoke(1, "");
        assertEquals(actual, "Point[x=0, y=0, z=0]");
    }

    public void testArray1D() throws Throwable {
        //test monodimensional array
        Object arr = MethodHandles.arrayConstructor(VT.arrayValueClass()).invoke(10);
        for (int i = 0; i < 10; i++) {
            Point p = new Point(i, (short) 9, (short) 9);
            MethodHandles.arrayElementSetter(VT.arrayValueClass()).invoke(arr, i, p);
        }
        for (int i = 0; i < 10; i++) {
            String actual = (String) printReturn(MethodHandles.arrayElementGetter(VT.arrayValueClass()))
                    .invoke(arr, i);
            String expected = TEMPLATE.replace("#x", String.valueOf(i))
                    .replaceAll("#[yz]", "9");
            assertEquals(actual, expected);
        }
    }

    public void testArray10D() throws Throwable {
        //test multidimensional array
        Object[] arr2 = (Object[]) MethodHandles.arrayConstructor(VT.arrayValueClass(2)).invoke(10);
        for (int i = 0; i < 10; i++) {
            Object innerArr = MethodHandles.arrayConstructor(VT.arrayValueClass()).invoke(10);
            MethodHandles.arrayElementSetter(VT.arrayValueClass(2)).invoke(arr2, i, innerArr);
            for (int j = 0; i < 10; i++) {
                Point p = new Point(i, (short) j, (short) 9);
                MethodHandles.arrayElementSetter(VT.arrayValueClass()).invoke(innerArr, i, p);
            }
        }
        for (int i = 0; i < 10; i++) {
            Object innerArr = MethodHandles.arrayElementGetter(VT.arrayValueClass(2)).invoke(arr2, i);
            for (int j = 0; i < 10; i++) {
                String actual = (String) printReturn(MethodHandles.arrayElementGetter(VT.arrayValueClass()))
                        .invoke(innerArr, i);
                String expected = TEMPLATE.replace("#x", String.valueOf(i))
                        .replace("#y", String.valueOf(j))
                        .replace("#z", "9");
                assertEquals(actual, expected);
            }
        }
    }

    public void testMultiArray() throws Throwable {
        Object[] arr43 = (Object[]) VT.newMultiArray(2).invoke(4, 3);
        for (int i = 0; i < 4; i++) {
            Object innerArr = arr43[i];
            for (int j = 0; i < 3; i++) {
                Point p = new Point(i, (short) j, (short) 9);
                MethodHandles.arrayElementSetter(VT.arrayValueClass()).invoke(innerArr, i, p);
            }
        }
        for (int i = 0; i < 4; i++) {
            Object innerArr = MethodHandles.arrayElementGetter(VT.arrayValueClass(2)).invoke(arr43, i);
            for (int j = 0; i < 3; i++) {
                String actual = (String) printReturn(MethodHandles.arrayElementGetter(VT.arrayValueClass()))
                        .invoke(innerArr, i);
                String expected = TEMPLATE.replace("#x", String.valueOf(i))
                        .replace("#y", String.valueOf(j))
                        .replace("#z", "9");
                assertEquals(actual, expected);
            }
        }
    }

    public void testLoop() throws Throwable {
        Object arr = MethodHandles.arrayConstructor(VT.arrayValueClass()).invoke(10);
        for (int i = 0; i < 10; i++) {
            Point p = new Point(i, (short) 9, (short) 9);
            MethodHandles.arrayElementSetter(VT.arrayValueClass()).invoke(arr, i, p);
        }

        /*
          iters -> (Point[] )int

          init  -> (Point[] )int

          sum   -> (int, int, int, int)int
          a     -> (int, Point, Point, Point)int
          b     -> (int, Point)int
          c     -> (int, Point[], int)int
          body  -> (int, int, Point[])int
         */

        MethodHandle iters = MethodHandles.arrayLength(VT.arrayValueClass());

        MethodHandle init = MethodHandles.dropArguments(MethodHandles.constant(int.class, 0),
                                                        0,
                                                        VT.arrayValueClass());

        MethodHandle sum = LOOKUP.findStatic(MVTTest.class,
                                             "sum",
                                             methodType(int.class, int.class, int.class, short.class, short.class));

        MethodHandle a = MethodHandles.filterArguments(sum, 1,
                                                       VT.findGetter(LOOKUP, FIELD_NAMES[0], FIELD_TYPES[0]),
                                                       VT.findGetter(LOOKUP, FIELD_NAMES[1], FIELD_TYPES[1]),
                                                       VT.findGetter(LOOKUP, FIELD_NAMES[2], FIELD_TYPES[2]));

        MethodHandle b = MethodHandles.permuteArguments(a,
                                                        methodType(int.class, int.class, VT.valueClass()),
                                                        0, 1, 1, 1);

        MethodHandle c = MethodHandles.collectArguments(b,
                                                        1,
                                                        MethodHandles.arrayElementGetter(VT.arrayValueClass()));

        MethodHandle body = MethodHandles.permuteArguments(c,
                                                           methodType(int.class, int.class, int.class, VT.arrayValueClass()),
                                                           0, 2, 1);

        MethodHandle loop = MethodHandles.countedLoop(iters, init, body);
        int actual = (int) loop.invoke(arr);
        int expected = 9 * 10 * 2 + 10 * (0 + 9) / 2;
        assertEquals(actual, expected);
    }

    static int sum(int v, int x, short y, short z) {
        return v + x + y + z;
    }

    static MethodHandle printReturn(MethodHandle mh) {
        return MethodHandles.filterReturnValue(mh, PRINT_POINT);
    }

    static String print(Point p) {
        return String.format("Point[x=%d, y=%d, z=%d]", p.x, p.y, p.z);
    }
}
