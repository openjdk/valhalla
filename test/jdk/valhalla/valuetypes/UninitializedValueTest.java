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
 * @compile --enable-preview --source ${jdk.version} UninitializedValueTest.java
 * @run testng/othervm --enable-preview -XX:InlineFieldMaxFlatSize=128 UninitializedValueTest
 * @run testng/othervm --enable-preview -XX:InlineFieldMaxFlatSize=0 UninitializedValueTest
 * @summary Test reflection and method handle on accessing a field of a primitive class
 *          that may be flattened or non-flattened
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class UninitializedValueTest {
    static primitive class EmptyValue {
        public boolean isEmpty() {
            return true;
        }
    }

    static primitive class Value {
        Object o;
        EmptyValue empty;
        Value() {
            this.o = null;
            this.empty = new EmptyValue();
        }
    }

    static class MutableValue {
        Object o;
        EmptyValue empty;
        volatile EmptyValue vempty;
    }

    @Test
    public void emptyValueClass() throws ReflectiveOperationException {
        EmptyValue e = new EmptyValue();
        Field[] fields = e.getClass().getDeclaredFields();
        assertTrue(fields.length == 0);
    }

    @Test
    public void testValue() throws ReflectiveOperationException {
        Value v = new Value();
        Field f0 = v.getClass().getDeclaredField("o");
        Object o = f0.get(v);
        assertTrue(o == null);

        // field of primitive value type must be non-null
        Field f1 = v.getClass().getDeclaredField("empty");
        assertTrue(f1.getType() == EmptyValue.class);
        EmptyValue empty = (EmptyValue)f1.get(v);
        assertTrue(empty.isEmpty());        // test if empty is non-null with default value
    }

    @Test
    public void testMutableValue() throws ReflectiveOperationException {
        MutableValue v = new MutableValue();
        Field f0 = v.getClass().getDeclaredField("o");
        f0.set(v, null);
        assertTrue( f0.get(v) == null);

        // field of primitive value type type must be non-null
        Field f1 = v.getClass().getDeclaredField("empty");
        assertTrue(f1.getType() == EmptyValue.class);
        EmptyValue empty = (EmptyValue)f1.get(v);
        assertTrue(empty.isEmpty());        // test if empty is non-null with default value

        Field f2 = v.getClass().getDeclaredField("vempty");
        assertTrue(f2.getType() == EmptyValue.class);
        EmptyValue vempty = (EmptyValue)f2.get(v);
        assertTrue(vempty.isEmpty());        // test if vempty is non-null with default value

        f1.set(v, new EmptyValue());
        assertTrue((EmptyValue)f1.get(v) == new EmptyValue());
        f2.set(v, new EmptyValue());
        assertTrue((EmptyValue)f2.get(v) == new EmptyValue());
    }

    @Test
    public void testMethodHandleValue() throws Throwable {
        Value v = new Value();
        MethodHandle mh = MethodHandles.lookup().findGetter(Value.class, "empty", EmptyValue.class);
        EmptyValue empty = (EmptyValue) mh.invokeExact(v);
        assertTrue(empty.isEmpty());        // test if empty is non-null with default value
    }

    @Test
    public void testMethodHandleMutableValue() throws Throwable {
        MutableValue v = new MutableValue();
        MethodHandle getter = MethodHandles.lookup().findGetter(MutableValue.class, "empty", EmptyValue.class);
        EmptyValue empty = (EmptyValue) getter.invokeExact(v);
        assertTrue(empty.isEmpty());        // test if empty is non-null with default value

        MethodHandle getter1 = MethodHandles.lookup().findGetter(MutableValue.class, "vempty", EmptyValue.class);
        EmptyValue vempty = (EmptyValue) getter1.invokeExact(v);
        assertTrue(vempty.isEmpty());        // test if vempty is non-null with default value

        MethodHandle setter = MethodHandles.lookup().findSetter(MutableValue.class, "empty", EmptyValue.class);
        setter.invokeExact(v, new EmptyValue());
        empty = (EmptyValue) getter.invokeExact(v);
        assertTrue(empty == new EmptyValue());

        MethodHandle setter1 = MethodHandles.lookup().findSetter(MutableValue.class, "vempty", EmptyValue.class);
        setter1.invokeExact(v, new EmptyValue());
        vempty = (EmptyValue) getter1.invokeExact(v);
        assertTrue(vempty == new EmptyValue());
    }

    @Test(expectedExceptions = { IllegalAccessException.class})
    public void noWriteAccess() throws ReflectiveOperationException {
        Value v = new Value();
        Field f = v.getClass().getDeclaredField("empty");
        f.set(v, null);
    }

    @Test(expectedExceptions = { NullPointerException.class})
    public void nonNullableField_reflection() throws ReflectiveOperationException {
        MutableValue v = new MutableValue();
        Field f = v.getClass().getDeclaredField("empty");
        f.set(v, null);
    }

    @Test(expectedExceptions = { NullPointerException.class})
    public void nonNullableField_MethodHandle() throws Throwable {
        MutableValue v = new MutableValue();
        MethodHandle mh = MethodHandles.lookup().findSetter(MutableValue.class, "empty", EmptyValue.class);
        EmptyValue.ref e = null;
        EmptyValue empty = (EmptyValue) mh.invokeExact(v, (EmptyValue)e);
    }
}
