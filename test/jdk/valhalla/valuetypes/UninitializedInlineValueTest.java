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
 * @compile --enable-preview --source ${jdk.version} UninitializedInlineValueTest.java
 * @run testng/othervm --enable-preview -XX:InlineFieldMaxFlatSize=128 UninitializedInlineValueTest
 * @run testng/othervm --enable-preview -XX:InlineFieldMaxFlatSize=0 UninitializedInlineValueTest
 * @summary Test reflection and method handle on accessing a field of inline type
 *          that may be flattened or non-flattened
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class UninitializedInlineValueTest {
    static primitive class EmptyInline {
        public boolean isEmpty() {
            return true;
        }
    }

    static primitive class InlineValue {
        Object o;
        EmptyInline empty;
        InlineValue() {
            this.o = null;
            this.empty = new EmptyInline();
        }
    }

    static class MutableValue {
        Object o;
        EmptyInline empty;
        volatile EmptyInline vempty;
    }

    @Test
    public void emptyInlineClass() throws ReflectiveOperationException {
        EmptyInline e = new EmptyInline();
        Field[] fields = e.getClass().getDeclaredFields();
        assertTrue(fields.length == 0);
    }

    @Test
    public void testInlineValue() throws ReflectiveOperationException {
        InlineValue v = new InlineValue();
        Field f0 = v.getClass().getDeclaredField("o");
        Object o = f0.get(v);
        assertTrue(o == null);

        // field of inline type must be non-null
        Field f1 = v.getClass().getDeclaredField("empty");
        assertTrue(f1.getType() == EmptyInline.class);
        EmptyInline empty = (EmptyInline)f1.get(v);
        assertTrue(empty.isEmpty());        // test if empty is non-null with default value
    }

    @Test
    public void testMutableValue() throws ReflectiveOperationException {
        MutableValue v = new MutableValue();
        Field f0 = v.getClass().getDeclaredField("o");
        f0.set(v, null);
        assertTrue( f0.get(v) == null);

        // field of inline type must be non-null
        Field f1 = v.getClass().getDeclaredField("empty");
        assertTrue(f1.getType() == EmptyInline.class);
        EmptyInline empty = (EmptyInline)f1.get(v);
        assertTrue(empty.isEmpty());        // test if empty is non-null with default value

        Field f2 = v.getClass().getDeclaredField("vempty");
        assertTrue(f2.getType() == EmptyInline.class);
        EmptyInline vempty = (EmptyInline)f2.get(v);
        assertTrue(vempty.isEmpty());        // test if vempty is non-null with default value

        f1.set(v, new EmptyInline());
        assertTrue((EmptyInline)f1.get(v) == new EmptyInline());
        f2.set(v, new EmptyInline());
        assertTrue((EmptyInline)f2.get(v) == new EmptyInline());
    }

    @Test
    public void testMethodHandleInlineValue() throws Throwable {
        InlineValue v = new InlineValue();
        MethodHandle mh = MethodHandles.lookup().findGetter(InlineValue.class, "empty", EmptyInline.class);
        EmptyInline empty = (EmptyInline) mh.invokeExact(v);
        assertTrue(empty.isEmpty());        // test if empty is non-null with default value
    }

    @Test
    public void testMethodHandleMutableValue() throws Throwable {
        MutableValue v = new MutableValue();
        MethodHandle getter = MethodHandles.lookup().findGetter(MutableValue.class, "empty", EmptyInline.class);
        EmptyInline empty = (EmptyInline) getter.invokeExact(v);
        assertTrue(empty.isEmpty());        // test if empty is non-null with default value

        MethodHandle getter1 = MethodHandles.lookup().findGetter(MutableValue.class, "vempty", EmptyInline.class);
        EmptyInline vempty = (EmptyInline) getter1.invokeExact(v);
        assertTrue(vempty.isEmpty());        // test if vempty is non-null with default value

        MethodHandle setter = MethodHandles.lookup().findSetter(MutableValue.class, "empty", EmptyInline.class);
        setter.invokeExact(v, new EmptyInline());
        empty = (EmptyInline) getter.invokeExact(v);
        assertTrue(empty == new EmptyInline());

        MethodHandle setter1 = MethodHandles.lookup().findSetter(MutableValue.class, "vempty", EmptyInline.class);
        setter1.invokeExact(v, new EmptyInline());
        vempty = (EmptyInline) getter1.invokeExact(v);
        assertTrue(vempty == new EmptyInline());
    }

    @Test(expectedExceptions = { IllegalAccessException.class})
    public void noWriteAccess() throws ReflectiveOperationException {
        InlineValue v = new InlineValue();
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
        MethodHandle mh = MethodHandles.lookup().findSetter(MutableValue.class, "empty", EmptyInline.class);
        EmptyInline.ref e = null;
        EmptyInline empty = (EmptyInline) mh.invokeExact(v, (EmptyInline)e);
    }
}
