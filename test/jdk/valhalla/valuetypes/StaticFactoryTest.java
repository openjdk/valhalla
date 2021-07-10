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
 * @summary Test reflection of constructors for primitive classes
 * @run testng/othervm StaticFactoryTest
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

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

    static final Class<?> PRIMITIVE_TYPE = SimplePrimitive.class;

    @Test
    public static void testPrimitiveClassConstructor() throws Exception {
        String cn = PRIMITIVE_TYPE.getName();
        Class<?> c = Class.forName(cn).asValueType();

        assertTrue(c.isPrimitiveClass());
        assertTrue(c == PRIMITIVE_TYPE);
    }

    @Test
    public static void constructor() throws Exception {
        Constructor<?> ctor = PRIMITIVE_TYPE.getDeclaredConstructor();
        Object o = ctor.newInstance();
        assertTrue(o.getClass() == PRIMITIVE_TYPE.asPrimaryType());
    }

    // Check that the class has the expected Constructors
    @Test
    public static void constructors() throws Exception {
        Set<String> expectedSig = Set.of("public StaticFactoryTest$SimplePrimitive(int)",
                                         "StaticFactoryTest$SimplePrimitive()");
        Constructor<? extends Object>[] cons = PRIMITIVE_TYPE.getDeclaredConstructors();
        Set<String> actualSig = Arrays.stream(cons).map(Constructor::toString)
                                      .collect(Collectors.toSet());
        boolean ok = expectedSig.equals(actualSig);
        if (!ok) {
            System.out.printf("expected: %s%n", expectedSig);
            System.out.printf("declared: %s%n", actualSig);
            assertTrue(ok);
        }
    }

    // Check that the constructor and field can be set accessible
    @Test
    public static void setAccessible() throws Exception {
        Constructor<?> ctor = PRIMITIVE_TYPE.getDeclaredConstructor();
        ctor.setAccessible(true);

        Field field = PRIMITIVE_TYPE.getField("x");
        field.setAccessible(true);
    }

    // Check that the constructor and field can be set accessible
    @Test
    public static void trySetAccessible() throws Exception {
        Constructor<?> ctor = PRIMITIVE_TYPE.getDeclaredConstructor();
        assertTrue(ctor.trySetAccessible());

        Field field = PRIMITIVE_TYPE.getField("x");
        assertTrue(field.trySetAccessible());
    }

    // Check that the final field cannot be modified
    @Test(expectedExceptions = IllegalAccessException.class)
    public static void setFinalField() throws Exception {
        Field field = PRIMITIVE_TYPE.getField("x");
        field.setAccessible(true);
        field.setInt(new SimplePrimitive(100), 200);
    }


    // Check that the class does not have a static method with the name <init>
    @Test
    public static void initFactoryNotMethods() {
        Method[] methods = PRIMITIVE_TYPE.getDeclaredMethods();
        for (Method m : methods) {
            if (Modifier.isStatic(m.getModifiers())) {
                assertFalse(m.getName().equals("<init>"));
            }
        }
    }
}
