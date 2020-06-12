/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test reflection of constructors for inline classes
 * @run testng/othervm InlineConstructorTest
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

public class InlineConstructorTest {

    // Target test class
    static inline class SimpleInline {
        public final int x;

        SimpleInline() {
            x = -1;
        }

        public SimpleInline(int x) {
            this.x = x;
        }
    }

    static final Class<?> INLINE_TYPE = SimpleInline.class;

    @Test
    public static void testInlineClassConstructor() throws Exception {
        String cn = INLINE_TYPE.getName();
        Class<?> c = Class.forName(cn);

        assertTrue(c.isInlineClass());
        assertEquals(c, INLINE_TYPE);
    }

    @Test
    public static void constructor() throws Exception {
        Constructor<?> ctor = INLINE_TYPE.getDeclaredConstructor();
        Object o = ctor.newInstance();
        assertEquals(o.getClass(), INLINE_TYPE);
    }

    // Check that the class has the expected Constructors
    @Test
    public static void constructors() throws Exception {
        Set<String> expectedSig = Set.of("public InlineConstructorTest$SimpleInline(int)",
                                         "InlineConstructorTest$SimpleInline()");
        Constructor<? extends Object>[] cons = INLINE_TYPE.getDeclaredConstructors();
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
        Constructor<?> ctor = INLINE_TYPE.getDeclaredConstructor();
        ctor.setAccessible(true);

        Field field = INLINE_TYPE.getField("x");
        field.setAccessible(true);
    }

    // Check that the constructor and field can be set accessible
    @Test
    public static void trySetAccessible() throws Exception {
        Constructor<?> ctor = INLINE_TYPE.getDeclaredConstructor();
        assertTrue(ctor.trySetAccessible());

        Field field = INLINE_TYPE.getField("x");
        assertTrue(field.trySetAccessible());
    }

    // Check that the final field cannot be modified
    @Test(expectedExceptions = IllegalAccessException.class)
    public static void setFinalField() throws Exception {
        Field field = INLINE_TYPE.getField("x");
        field.setAccessible(true);
        field.setInt(new SimpleInline(100), 200);
    }


    // Check that the class does not have a static method with the name <init>
    @Test
    public static void initFactoryNotMethods() {
        Method[] methods = INLINE_TYPE.getDeclaredMethods();
        for (Method m : methods) {
            if (Modifier.isStatic(m.getModifiers())) {
                assertFalse(m.getName().equals("<init>"));
            }
        }
    }
}
