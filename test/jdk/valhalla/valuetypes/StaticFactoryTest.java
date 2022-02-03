/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8273360
 * @summary Test reflection of constructors for value classes
 * @run testng/othervm StaticFactoryTest
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class StaticFactoryTest {
    // Target test class
    static primitive class SimplePrimitive {
        public final int x;

        SimplePrimitive() {
            x = -1;
        }

        public SimplePrimitive(int x) {
            this.x = x;
        }
    }

    static value class SimpleValue {
        private final SimplePrimitive v;

        SimpleValue() {
            this.v = SimplePrimitive.default;
        }

        public SimpleValue(SimplePrimitive v) {
            this.v = v;
        }
    }

    @DataProvider
    static Object[][] classes() {
        return new Object[][]{
                new Object[] { SimplePrimitive.class, true },
                new Object[] { SimpleValue.class, false },
        };
    }

    @Test(dataProvider = "classes")
    public void testConstructor(Class<?> c, boolean isPrimitiveClass) throws ReflectiveOperationException {
        String cn = c.getName();
        Class<?> clz = Class.forName(cn);

        assertTrue(clz.isValue());
        assertTrue(clz.isPrimitiveClass() == isPrimitiveClass);

        Constructor<?> ctor = clz.getDeclaredConstructor();
        Object o = ctor.newInstance();
        assertTrue(o.getClass() == c);

        // Verify that the constructor and field can be set accessible
        ctor.setAccessible(true);
        assertTrue(ctor.trySetAccessible());

        // Check that getDeclaredMethods does not include the static factory method
        Method[] methods = clz.getDeclaredMethods();
        for (Method m : methods) {
            if (Modifier.isStatic(m.getModifiers())) {
                assertFalse(m.getName().equals("<init>"));
            }
        }
    }

    @DataProvider
    static Object[][] ctors() {
        return new Object[][]{
                new Object[] { SimplePrimitive.class, Set.of("public StaticFactoryTest$SimplePrimitive(int)",
                                                             "StaticFactoryTest$SimplePrimitive()")},
                new Object[] { SimpleValue.class, Set.of("public StaticFactoryTest$SimpleValue(StaticFactoryTest$SimplePrimitive)",
                                                         "StaticFactoryTest$SimpleValue()") },
        };
    }

    // Check that the class has the expected Constructors
    @Test(dataProvider = "ctors")
    public static void constructors(Class<?> c, Set<String> signatures) throws ReflectiveOperationException {
        Constructor<?>[] cons = c.getDeclaredConstructors();
        Set<String> actualSig = Arrays.stream(cons).map(Constructor::toString)
                                      .collect(Collectors.toSet());
        boolean ok = signatures.equals(actualSig);
        if (!ok) {
            System.out.printf("expected: %s%n", signatures);
            System.out.printf("declared: %s%n", actualSig);
            assertTrue(ok);
        }
    }

    @DataProvider
    static Object[][] fields() throws ReflectiveOperationException {
        return new Object[][]{
                new Object[] { SimplePrimitive.class.getDeclaredField("x"), new SimplePrimitive(), 200},
                new Object[] { SimpleValue.class.getDeclaredField("v"), new SimpleValue(), new SimplePrimitive(10) },
        };
    }

    // Check that the final field cannot be modified
    @Test(dataProvider = "fields", expectedExceptions = IllegalAccessException.class)
    public static void readOnlyFields(Field field, Object obj, Object newValue) throws ReflectiveOperationException {
        // succeeds to set accessible flag
        field.setAccessible(true);
        assertTrue(field.trySetAccessible());

        // value class' final fields cannot be modified
        if (field.getType() == int.class) {
            field.setInt(obj, ((Integer) newValue).intValue());
        } else {
            field.set(obj, newValue);
        }
    }
}
