/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test core reflection, dynamic proxy and lambdas that generates
 *          classes dynamically that reference Q-type and L-type
 * @run testng/othervm QTypeDescriptorTest
 * @run testng/othervm -Dsun.reflect.noInflation=true QTypeDescriptorTest
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.function.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class QTypeDescriptorTest {
    static final Point P0 = Point.makePoint(10, 20);
    static final Point P1 = Point.makePoint(30, 40);
    static final NonFlattenValue NFV = NonFlattenValue.make(30, 40);

    @Test
    public static void testLambda() {
        newArray(Point[]::new, 2);
        newArray(Point[][]::new, 1);

        newArray(NonFlattenValue[]::new, 3);
        newArray(MutablePath[]::new, 4);

        Function<Point[], T> f =
            (points) -> { return new T(points); };
        f.apply(new Point[] { P0, P1});
    }

    @Test
    public static void testMethodInvoke() throws Exception {
        Class<?> pointQType = Point.class;
        Class<?> nonFlattenValueQType = NonFlattenValue.class;
        Method m = QTypeDescriptorTest.class
            .getDeclaredMethod("toLine", pointQType, nonFlattenValueQType);
        makeLine(m, P0, NFV);

        m = QTypeDescriptorTest.class
                .getDeclaredMethod("toLine", Point[].class);
        makeLine(m, (Object) new Point[] { P0, P1});
    }

    private static void makeLine(Method m, Object... args) throws Exception {
        Line l = (Line) m.invoke(null, args);
        assertEquals(l.p1, P0);
        assertEquals(l.p2, NFV.pointValue());
    }

    @Test
    public static void testStaticMethod() throws Throwable {
        // static method in an inline type with no parameter and void return type
        Runnable r = () -> ValueTest.run();
        r.run();

        // via Method::invoke
        Method m = ValueTest.class.getMethod("run");
        m.invoke(null);

        // via MethodHandle
        MethodHandle mh = MethodHandles.lookup()
            .findStatic(ValueTest.class, "run", MethodType.methodType(void.class));
        mh.invokeExact();

        mh = MethodHandles.lookup().unreflect(m);
        mh.invokeExact();
    }

    @Test
    public static void testConstructor() throws Exception {
        Constructor<T> ctor = T.class.getDeclaredConstructor(Point[].class);
        Point[] points = new Point[] { P0, P1 };
        T test = (T) ctor.newInstance((Object)points);
        assertEquals(test.points[0], P0);
        assertEquals(test.points[1], P1);
    }

    @Test
    public static void testProxy() throws Exception {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("toLine")) {
                    return toLine((Point)args[0], (NonFlattenValue)args[1]);
                }
                throw new UnsupportedOperationException(method.toString());
            }
        };

        Class<?>[] intfs = new Class<?>[] { I.class };
        I intf = (I) Proxy.newProxyInstance(QTypeDescriptorTest.class.getClassLoader(), intfs, handler);
        Line l = intf.toLine(P0, NFV);
        assertEquals(l.p1, P0);
        assertEquals(l.p2, NFV.pointValue());
    }

    @DataProvider
    static Object[][] descriptors() {
        return new Object[][]{
            { QTypeDescriptorTest.class, "toLine", new Class<?>[] { Point.class, NonFlattenValue.class},     true},
            { QTypeDescriptorTest.class, "toLine", new Class<?>[] { Point.ref.class, NonFlattenValue.class}, false},
            { QTypeDescriptorTest.class, "toLine", new Class<?>[] { Point[].class },                         true},
            { NonFlattenValue.class, "point",      null,                                                     true},
            { NonFlattenValue.class, "pointValue", null,                                                     true},
            { NonFlattenValue.class, "has",        new Class<?>[] { Point.class, Point.ref.class},           true},
            { NonFlattenValue.class, "has",        new Class<?>[] { Point.class, Point.class},               false},
        };
    }

    @Test(dataProvider = "descriptors")
    public static void testDescriptors(Class<?> defc, String name, Class<?>[] params, boolean found) throws Exception {
        try {
            // TODO: methods are in the reference projection
            Class<?> declaringClass = defc /* defc.referenceType().get() */;
            declaringClass.getDeclaredMethod(name, params);
            if (!found) throw new AssertionError("Expected NoSuchMethodException");
        } catch (NoSuchMethodException e) {
            if (found) throw e;
        }
    }

    @DataProvider
    static Object[][] methodTypes() {
        ClassLoader loader = QTypeDescriptorTest.class.getClassLoader();
        return new Object[][]{
            { "point",      MethodType.methodType(Point.ref.class),                                     true },
            { "pointValue", MethodType.methodType(Point.class),                                         true },
            { "has",        MethodType.methodType(boolean.class, Point.class, Point.ref.class),         true },
            { "point",      MethodType.methodType(Point.class),                                         false },
            { "pointValue", MethodType.methodType(Point.ref.class),                                     false },
            { "has",        MethodType.methodType(boolean.class, Point.ref.class, Point.class),         false },
            { "point",      MethodType.fromMethodDescriptorString("()LPoint$ref;", loader),             true },
            { "point",      MethodType.fromMethodDescriptorString("()QPoint;", loader),                 false },
            { "pointValue", MethodType.fromMethodDescriptorString("()QPoint;", loader),                 true },
            { "pointValue", MethodType.fromMethodDescriptorString("()LPoint$ref;", loader),             false },
            { "has",        MethodType.fromMethodDescriptorString("(QPoint;LPoint$ref;)Z", loader),     true },
            { "has",        MethodType.fromMethodDescriptorString("(LPoint$ref;LPoint$ref;)Z", loader), false },
        };
    }

    @Test(dataProvider = "methodTypes")
    public static void methodHandleLookup(String name, MethodType mtype, boolean found) throws Throwable {
        try {
            MethodHandles.lookup().findVirtual(NonFlattenValue.class, name, mtype);
            if (!found) throw new AssertionError("Expected NoSuchMethodException");
        } catch (NoSuchMethodException e) {
            if (found) throw e;
        }
    }

    private static <T> T[] newArray(IntFunction<T[]> arrayCreator, int size) {
        return arrayCreator.apply(size);
    }

    private static Line toLine(Point p, NonFlattenValue nfv) {
        return Line.makeLine(p, nfv.pointValue());
    }

    private static Line toLine(Point[] points) {
        assertTrue(points.length == 2);
        return Line.makeLine(points[0], points[1]);
    }

    static class T {
        final Point[] points;
        T(Point[] points) {
            this.points = points;
        }
    }

    interface I {
        Line toLine(Point p, NonFlattenValue nfv);
    }

    static primitive class ValueTest {
        private final int value;
        public ValueTest() { this.value = 0; }

        public static void run() {
            Runnable r = () -> {
                System.out.println("called ValueTest::run");
            };
            r.run();
        }
    }

}
