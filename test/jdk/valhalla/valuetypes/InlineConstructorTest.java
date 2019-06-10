/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @run main/othervm -XX:+EnableValhalla InlineConstructorTest
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

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

    private final Class<?> c;

    public static void main(String... args) throws Exception {
        testSimpleInlineClass();
    }

    static void testSimpleInlineClass() throws Exception  {
        InlineConstructorTest test = new InlineConstructorTest(SimpleInline.class);
        test.constructor();
        test.constructors("public InlineConstructorTest$SimpleInline(int)",
                "InlineConstructorTest$SimpleInline()");
        test.setAccessible();
        test.trySetAccessible();
        test.initFactoryNotMethods();
    }

    InlineConstructorTest(Class<?> type) throws Exception {
        String cn = type.getName();
        this.c = Class.forName(cn);

        assertTrue(c.isInlineClass());
        assertEquals(c, type);
    }

    void constructor() throws Exception {
        Constructor<?> ctor = c.getDeclaredConstructor();
        Object o = ctor.newInstance();
        assertEquals(o.getClass(), c);
    }

    // Check that the class has the expected Constructors
    void constructors(String... expected) throws Exception {
        Constructor<? extends Object>[] cons = c.getDeclaredConstructors();
        Set<String> actualSig =
                Arrays.stream(cons).map(Constructor::toString).collect(Collectors.toSet());
        Set<String> expectedSig = Set.of(expected);
        boolean ok = expectedSig.equals(actualSig);
        if (!ok) {
            System.out.printf("expected: %s%n", expectedSig);
            System.out.printf("declared: %s%n", actualSig);
            assertTrue(ok);
        }
    }

    // Check that the constructor can be set accessible and that the field x can not
    void setAccessible() throws Exception {
        Constructor<?> ctor = c.getDeclaredConstructor();
        ctor.setAccessible(true);
        Field field = c.getField("x");
        try {
            field.setAccessible(true);
            throw new RuntimeException("InaccessibleObjectException not thrown");
        } catch (InaccessibleObjectException e) {
            // IOE is expected
        }
    }

    // Check that the constructor can be set accessible and that the field x can not
    void trySetAccessible() throws Exception {
        Constructor<?> ctor = c.getDeclaredConstructor();
        assertTrue(ctor.trySetAccessible());
        Field field = c.getField("x");
        if (field.trySetAccessible()) {
            throw new RuntimeException("trySetAccessible should not succeed");
        }
    }

    // Check that the class does not have a static method with the name <init>
    void initFactoryNotMethods() {
        Method[] methods = c.getDeclaredMethods();
        for (Method m : methods) {
            if (Modifier.isStatic(m.getModifiers())) {
                assertFalse(m.getName().equals("<init>"));
            }
        }
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

    static void assertFalse(boolean value) {
        if (value)
            throw new AssertionError("expected false");
    }
}
