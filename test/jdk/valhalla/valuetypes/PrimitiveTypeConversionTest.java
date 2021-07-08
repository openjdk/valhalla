/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test method handles with primitive narrowing/widening conversion
 * @run testng/othervm PrimitiveTypeConversionTest
 */

import java.lang.invoke.*;

import static java.lang.invoke.MethodType.*;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class PrimitiveTypeConversionTest {
    static primitive class Value {
        Point val;
        Point.ref ref;
        Value(Point p1, Point.ref p2) {
            this.val = p1;
            this.ref = p2;
        }
    }

    static Value narrow(Value.ref v) {
        return v;
    }

    static Value.ref widen(Value v) {
        if (((Object)v) == null) {
            throw new Error("should never reach here: should be caught by runtime");
        }
        return null;
    }

    static final Value VALUE = new Value(new Point(10,10), new Point(20, 20));

    @Test
    public static void primitiveWidening() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh1 = lookup.findStatic(PrimitiveTypeConversionTest.class, "narrow", methodType(Value.class, Value.ref.class));
        MethodHandle mh2 = mh1.asType(methodType(Value.class, Value.class));
        Object v = mh1.invoke(VALUE);
        assertEquals(v, VALUE);
        try {
            Object v1 = mh1.invoke((Object)null);
            fail("Expected NullPointerException but not thrown");
        } catch (NullPointerException e) {}

        try {
            Object v2 = mh2.invoke((Object)null);
            fail("Expected NullPointerException but not thrown");
        } catch (NullPointerException e) {}
    }

    @Test
    public static void primitiveNarrowing() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh = lookup.findStatic(PrimitiveTypeConversionTest.class, "widen", methodType(Value.ref.class, Value.class));
        Object v = mh.invoke(VALUE);
        assertTrue(v == null);
        try {
            Object v1 = mh.invoke((Object)null);
            fail("Expected NullPointerException but not thrown");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        MethodHandle mh2 = mh.asType(methodType(Value.class, Value.ref.class));
        try {
            Value v2 = (Value) mh2.invoke((Value.ref)null);
            fail("Expected NullPointerException but not thrown");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Test
    public static void valToRef() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh1 = lookup.findGetter(Value.class, "val", Point.class);
        MethodHandle mh2 = mh1.asType(methodType(Point.ref.class, Value.class));
        Value v = new Value(new Point(10,10), null);

        Point p1 = (Point) mh1.invokeExact(VALUE);
        Point.ref p2 = (Point.ref) mh2.invokeExact(VALUE);
        assertEquals(p1, p2);
    }

    @Test
    public static void refToVal() throws Throwable {
        MethodHandle mh1 = MethodHandles.lookup().findGetter(Value.class, "ref", Point.ref.class);
        MethodHandle mh2 = mh1.asType(methodType(Point.class, Value.class));
        Point.ref p1 = (Point.ref) mh1.invokeExact(VALUE);
        Point p2 = (Point) mh2.invokeExact(VALUE);
        assertEquals(p1, p2);
    }

    @Test
    public static void valToRef1() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh1 = lookup.findGetter(Value.class, "val", Point.class);
        MethodHandle mh2 = mh1.asType(methodType(Point.class, Value.ref.class));

        Point p1 = (Point) mh1.invokeExact(VALUE);
        Point p2 = (Point) mh2.invoke(VALUE);
        Point p3 = (Point) mh2.invokeExact((Value.ref)VALUE);
        assertEquals(p1, p2);
        assertEquals(p1, p3);
    }

    @Test
    public static void refToVal1() throws Throwable {
        MethodHandle mh1 = MethodHandles.lookup().findGetter(Value.class, "ref", Point.ref.class);
        MethodHandle mh2 = mh1.asType(methodType(Point.ref.class, Value.ref.class));
        Value v = new Value(new Point(10,10), null);

        Point.ref p1 = (Point.ref) mh1.invokeExact(v);
        Point.ref p2 = (Point.ref) mh2.invoke(v);
        Point.ref p3 = (Point.ref) mh2.invokeExact((Value.ref)v);
        assertEquals(p1, p2);
        assertEquals(p1, p3);
    }

    @Test
    public static void refToVal2() throws Throwable {
        MethodHandle mh1 = MethodHandles.lookup().findGetter(Value.class, "ref", Point.ref.class);
        MethodHandle mh2 = mh1.asType(methodType(Point.class, Value.class));
        Value v = new Value(new Point(10,10), null);

        Point.ref p1 = (Point.ref) mh1.invokeExact(v);
        try {
            Point p2 = (Point) mh2.invokeExact(v);
            fail("Expected NullPointerException but not thrown");
        } catch (NullPointerException e) {}
    }
}
