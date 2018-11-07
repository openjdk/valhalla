/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @build  DefineClassTest
 * @run testng/othervm DefineClassTest
 */

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import jdk.internal.org.objectweb.asm.*;
import org.testng.annotations.Test;

import static java.lang.invoke.MethodHandles.Lookup.ClassProperty.*;
import static java.lang.invoke.MethodHandles.Lookup.PRIVATE;

import static jdk.internal.org.objectweb.asm.Opcodes.*;
import static org.testng.Assert.*;

public class DefineClassTest {
    private static final byte[] bytes = classBytes("Injected");
    private static byte[] classBytes(String classname) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);
        MethodVisitor mv;

        cw.visit(V11, ACC_FINAL, classname, null, "java/lang/Object", null);

        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        {
            // access a private member of the nest host class
            mv = cw.visitMethod(ACC_PUBLIC, "test", "(LDefineClassTest;)I", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "DefineClassTest", "privMethod", "()I");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    private int privMethod() { return 1234; }

    @Test
    public void defineNestMate() throws Throwable {
        // define a nestmate
        Lookup lookup = MethodHandles.lookup();
        Class<?> c = lookup.defineClass(bytes, NESTMATE);
        assertTrue(c.getNestHost() == DefineClassTest.class);
        assertTrue(c == Class.forName("Injected"));

        // invoke int test(DefineClassTest o)
        int x = testInjectedClass(c);
        assertTrue(x == privMethod());

        // dynamic nestmate is not listed in the return array of getNestMembers
        assertTrue(Stream.of(c.getNestHost().getNestMembers()).noneMatch(k -> k == c));
        assertTrue(c.isNestmateOf(DefineClassTest.class));
    }

    @Test
    public void defineHiddenClass() throws Throwable {
        // define a hidden class
        Lookup lookup = MethodHandles.lookup();
        Class<?> c = lookup.defineClass(bytes, NESTMATE, HIDDEN);
        System.out.println(c.getName());
        assertTrue(c.getNestHost() == DefineClassTest.class);
        assertTrue(c.isHidden());

        // invoke int test(DefineClassTest o)
        int x = testInjectedClass(c);
        assertTrue(x == privMethod());

        // dynamic nestmate is not listed in the return array of getNestMembers
        assertTrue(Stream.of(c.getNestHost().getNestMembers()).noneMatch(k -> k == c));
        assertTrue(c.isNestmateOf(DefineClassTest.class));
    }

    @Test
    public void defineWeakClass() throws Throwable {
        // define a weak class
        Lookup lookup = MethodHandles.lookup().dropLookupMode(Lookup.PRIVATE);
        Class<?> c = lookup.defineClass(bytes, WEAK);
        System.out.println(c.getName());
        assertTrue(c.getNestHost() == c);
        assertTrue(c.isHidden());
    }

    @Test(expectedExceptions = IllegalAccessError.class)
    public void definePackageAccessClass() throws Throwable {
        Lookup lookup = MethodHandles.lookup().dropLookupMode(Lookup.PRIVATE);
        Class<?> c = lookup.defineClass(bytes, HIDDEN);
        assertTrue(c.getNestHost() == c);
        assertTrue(c.isHidden());

        // fail to access DefineClassTest::privMethod method
        testInjectedClass(c);
    }

    @Test(expectedExceptions = IllegalAccessException.class)
    public void noPrivateLookupAccess() throws Throwable {
        Lookup lookup = MethodHandles.lookup().dropLookupMode(Lookup.PRIVATE);
        lookup.defineClass(bytes, NESTMATE);
    }

    @Test(expectedExceptions = IllegalAccessException.class)
    public void teleportToNestmate() throws Throwable {
        Class<?> c = MethodHandles.lookup().defineClass(bytes, NESTMATE, HIDDEN);
        assertTrue(c.getNestHost() == DefineClassTest.class);
        assertTrue(c.isHidden());

        // Teleport to a nestmate
        Lookup lookup =  MethodHandles.lookup().in(c);
        assertTrue((lookup.lookupModes() & PRIVATE) == 0);
        // fail to define a nestmate
        lookup.defineClass(bytes, NESTMATE, HIDDEN);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void notSamePackage() throws Throwable {
        MethodHandles.lookup().defineClass(classBytes("p/Injected"), NESTMATE);
    }

    /*
     * invoke int test(DefineClassTest o) method defined in the injected class
     */
    private int testInjectedClass(Class<?> c) throws Throwable {
        try {
            Method m = c.getMethod("test", DefineClassTest.class);
            return (int) m.invoke(c.newInstance(), this);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}


