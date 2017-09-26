/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @run testng/othervm -Xverify:none -XX:+EnableMVT TestValueInValue
 */

import jdk.incubator.mvt.ValueType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

@Test
public class TestValueInValue {

    static final ValueType<?> VT_OUTER;

    static final ValueType<?> VT_INNER;

    static final Object[] TEST_OBJECTS;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            VT_INNER = ValueType.make(lookup, "Inner", new String[] {"f_i1", "f_i2"}, new Class<?>[] {int.class, String.class});
            VT_OUTER = ValueType.make(lookup, "Outer", new String[] {"f_o1", "f_o2"}, new Class<?>[] {VT_INNER.valueClass(), double.class});
            MethodHandle c_i = lookup.findConstructor(VT_INNER.boxClass(), MethodType.methodType(void.class, int.class, String.class));
            MethodHandle c_o = lookup.findConstructor(VT_OUTER.boxClass(), MethodType.methodType(void.class, VT_INNER.valueClass(), double.class));
            TEST_OBJECTS = new Object[] {
                    c_o.invoke(c_i.invoke(42, "One"), 42d),
                    c_o.invoke(c_i.invoke(42, "Two"), 42d),
                    c_o.invoke(c_i.invoke(42, "Three"), 42d),
                    c_o.invoke(c_i.invoke(42, "Four"), 42d),
            };
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public void testHashCode() throws Throwable {
        for (int i = 0 ; i < TEST_OBJECTS.length ; i++) {
            assertEquals((int)VT_OUTER.substitutabilityHashCode().invoke(TEST_OBJECTS[i]), TEST_OBJECTS[i].hashCode());
        }
    }

    public void testEquals() throws Throwable {
        for (int i = 0 ; i < TEST_OBJECTS.length ; i++) {
            for (int j = 0 ; j < TEST_OBJECTS.length ; j++) {
                assertEquals((boolean)VT_OUTER.substitutabilityTest().invoke(TEST_OBJECTS[i], TEST_OBJECTS[j]),
                               TEST_OBJECTS[i].equals(TEST_OBJECTS[j]));
            }
        }
    }

    public void testArray() throws Throwable {
        Object arr = MethodHandles.arrayConstructor(VT_OUTER.arrayValueClass()).invoke(TEST_OBJECTS.length);
        int index = 0;
        for (Object o : TEST_OBJECTS) {
            MethodHandles.arrayElementSetter(VT_OUTER.arrayValueClass()).invoke(arr, index, o);
            Object o2 = MethodHandles.arrayElementGetter(VT_OUTER.arrayValueClass()).invoke(arr, index);
            assertEquals(o, o2);
            index++;
        }
    }
}
