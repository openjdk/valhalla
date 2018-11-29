/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test reflection on value types
 * @compile -XDallowWithFieldOperator Point.java Line.java NonFlattenValue.java
 * @run main/othervm -XX:+EnableValhalla Reflection
 */

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Reflection {
    public static void main(String... args) throws Exception {
        testPointClass();
        testLineClass();
        testNonFlattenValue();
    }

    static void testPointClass() throws Exception  {
        Point o = Point.makePoint(10, 20);
        Reflection test = new Reflection(Point.class, "Point", o);
        test.newInstance();
        test.constructor();
        test.accessFieldX(o.x);
        test.setAccessible();
        test.trySetAccessible();
        test.staticField();
    }

    static void testLineClass() throws Exception {
        Line l = Line.makeLine(10, 20, 30, 40);
        Reflection test = new Reflection(Line.class, "Line", l);
        test.checkField("p1", Point.class.asValueType());
        test.checkField("p2", Point.class.asValueType());
        test.checkMethod("p1", Point.class.asValueType());
        test.checkMethod("p2", Point.class.asValueType());
    }

    static void testNonFlattenValue() throws Exception {
        NonFlattenValue nfv = NonFlattenValue.make(10, 20);
        Reflection test = new Reflection(NonFlattenValue.class, "NonFlattenValue", nfv);
        test.checkField("nfp", Point.class.asBoxType());
        test.checkMethod("point", Point.class.asBoxType());
        test.checkMethod("pointValue", Point.class.asValueType());
        test.checkMethod("has", void.class, Point.class.asValueType(), Point.class.asBoxType());
    }

    private final Class<?> c;
    private final Constructor<?> ctor;
    private final Object o;
    Reflection(Class<?> type, String cn, Object o) throws Exception {
        this.c = Class.forName(cn);
        if (!c.isValue() || c != type) {
            throw new RuntimeException(cn + " is not a value class");
        }

        // the box type is the primary mirror
        assertEquals(type, o.getClass());
        assertEquals(type, c.asBoxType());

        this.ctor = c.getDeclaredConstructor();
        this.o = o;

        // TODO: what should Object::getClass return?
        // assertEquals(o.getClass(), c.asValueType());

        // test the box type and value type
        testBoxAndValueType(this.c);
        // test array of Q-type
        // TODO: array of L-type support
        testArrayOfQType();
    }

    private static void testBoxAndValueType(Class<?> c) {
        Class<?> box = c.asBoxType();
        Class<?> val = c.asValueType();
        assertTrue(val != null);
        assertEquals(box.getTypeName(), c.getTypeName());
        assertEquals(val.getTypeName(), c.getTypeName() + "/val");
        assertEquals(box, c);
        assertEquals(val.asBoxType(), box);
        assertEquals(box.asValueType(), val);
    }

    void testArrayOfQType() {
        Class<?> elementType = c.asValueType();
        Object array = Array.newInstance(elementType, 1);
        Class<?> arrayType = array.getClass();
        assertTrue(arrayType.isArray());
        Class<?> componentType = arrayType.getComponentType();
        assertTrue(componentType.isValue());
        assertEquals(componentType, elementType);
        // Array is a reference type
        assertEquals(arrayType.asBoxType(), arrayType);
    }

    void accessFieldX(int x) throws Exception {
        Field field = c.getField("x");
        if (field.getInt(o) != x) {
            throw new RuntimeException("Unexpected Point.x value: " +  field.getInt(o));
        }

        try {
            field.setInt(o, 100);
            throw new RuntimeException("IllegalAccessException not thrown");
        } catch (IllegalAccessException e) {}
    }

    void newInstance() throws Exception {
        try {
            Object o = c.newInstance();
            throw new RuntimeException("newInstance expected to be unsupported on value class");
        } catch (IllegalAccessException e) {}
    }

    void constructor() throws Exception {
        try {
            ctor.newInstance();
            throw new RuntimeException("IllegalAccessException not thrown");
        } catch (IllegalAccessException e) { }
    }

    void setAccessible() throws Exception {
        try {
            ctor.setAccessible(true);
            throw new RuntimeException("InaccessibleObjectException not thrown");
        } catch (InaccessibleObjectException e) { e.printStackTrace(); }
        Field field = c.getField("x");
        try {
            field.setAccessible(true);
            throw new RuntimeException("InaccessibleObjectException not thrown");
        } catch (InaccessibleObjectException e) { e.printStackTrace(); }
    }

    void trySetAccessible() throws Exception {
        if (ctor.trySetAccessible()) {
            throw new RuntimeException("trySetAccessible should not succeed");
        }
        Field field = c.getField("x");
        if (field.trySetAccessible()) {
            throw new RuntimeException("trySetAccessible should not succeed");
        }
    }

    void staticField() throws Exception {
        Field f = c.getDeclaredField("STATIC_FIELD");
        if (f.trySetAccessible()) {
            throw new RuntimeException("trySetAccessible should not succeed");
        }
        try {
            f.setAccessible(true);
            throw new RuntimeException("IllegalAccessException not thrown");
        } catch (InaccessibleObjectException e) { }
    }

    void checkField(String name, Class<?> type) throws Exception {
        Field f = c.getDeclaredField(name);
        System.out.format("Field %s::%s of type %s = %s%n",
                          f.getDeclaringClass().getTypeName(), f.getName(),
                          f.getType().getTypeName(), f.get(o));
        assertEquals(f.getType(), type);
    }

    void checkMethod(String name, Class<?> returnType, Class<?>... params) throws Exception {
        Method m = c.getDeclaredMethod(name, params);

        String paramDesc = (params == null || params.length == 0) ? "" :
            Arrays.stream(params).map(Class::getTypeName).collect(Collectors.joining(", "));
        System.out.format("Method %s::%s(%s)%s%n",
                          m.getDeclaringClass().getTypeName(), m.getName(),
                          paramDesc, returnType.getTypeName());
    }
    
    static void assertEquals(Object o1, Object o2) {
        if (o1 == o2 || o1.equals(o2))
            return;

        throw new AssertionError(o1 + " != " + o2);
    }

    static void assertTrue(boolean value) {
        if (!value)
            throw new AssertionError("expected true");

    }
}
