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
 * @library /test/lib
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @build  DefineHiddenClassTest
 * @run testng/othervm DefineHiddenClassTest
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import jdk.internal.org.objectweb.asm.*;
import org.testng.annotations.Test;

import static java.lang.invoke.MethodHandles.Lookup.ClassOptions.*;
import static java.lang.invoke.MethodHandles.Lookup.PRIVATE;
import static java.lang.invoke.MethodType.*;

import static jdk.internal.org.objectweb.asm.Opcodes.*;
import static org.testng.Assert.*;

public class DefineHiddenClassTest {
    private static final byte[] bytes = classBytes("Injected");
    private static byte[] classBytes(String classname) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);
        MethodVisitor mv;

        cw.visit(V12, ACC_FINAL, classname, null, "java/lang/Object", null);

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
            mv = cw.visitMethod(ACC_PUBLIC, "test", "(LDefineHiddenClassTest;)I", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "DefineHiddenClassTest", "privMethod", "()I");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    private int privMethod() { return 1234; }

    @Test
    public void defineHiddenClass() throws Throwable {
        // define a hidden class
        Lookup lookup = MethodHandles.lookup();
        Class<?> c = lookup.defineHiddenClass(bytes, false, NESTMATE);
        System.out.println(c.getName());
        assertTrue(c.getNestHost() == DefineHiddenClassTest.class);
        assertTrue(c.isHiddenClass());

        // invoke int test(DefineHiddenClassTest o)
        int x = testInjectedClass(c);
        assertTrue(x == privMethod());

        // dynamic nestmate is not listed in the return array of getNestMembers
        assertTrue(Stream.of(c.getNestHost().getNestMembers()).noneMatch(k -> k == c));
        assertTrue(c.isNestmateOf(DefineHiddenClassTest.class));
    }

    @Test
    public void defineHiddenClassAsLookup() throws Throwable {
        // define a hidden class
        Lookup lookup = MethodHandles.lookup().defineHiddenClassAsLookup(bytes, false, NESTMATE);
        Class<?> c = lookup.lookupClass();
        assertTrue(c.getNestHost() == DefineHiddenClassTest.class);
        assertTrue(c.isHiddenClass());

        // invoke int test(DefineHiddenClassTest o) via MethodHandle
        MethodHandle ctor = lookup.findConstructor(c, methodType(void.class));
        MethodHandle mh = lookup.findVirtual(c, "test", methodType(int.class, DefineHiddenClassTest.class));
        int x = (int)mh.bindTo(ctor.invoke()).invokeExact( this);
        assertTrue(x == privMethod());

        // dynamic nestmate is not listed in the return array of getNestMembers
        assertTrue(Stream.of(c.getNestHost().getNestMembers()).noneMatch(k -> k == c));
        assertTrue(c.isNestmateOf(DefineHiddenClassTest.class));
    }

    @Test
    public void defineWeakClass() throws Throwable {
        // define a weak class
        Class<?> c = MethodHandles.lookup().defineHiddenClass(bytes, false, WEAK);
        assertTrue(c.getNestHost() == c);
        assertTrue(c.isHiddenClass());
    }

    @Test(expectedExceptions = IllegalAccessException.class)
    public void definePackageAccessClass() throws Throwable {
        Lookup lookup = MethodHandles.lookup().dropLookupMode(Lookup.PRIVATE);
        lookup.defineHiddenClass(bytes, false, NESTMATE);
    }

    @Test(expectedExceptions = IllegalAccessException.class)
    public void noPrivateLookupAccess() throws Throwable {
        Lookup lookup = MethodHandles.lookup().dropLookupMode(Lookup.PRIVATE);
        lookup.defineHiddenClass(bytes, false, NESTMATE);
    }

    @Test(expectedExceptions = IllegalAccessException.class)
    public void teleportToNestmate() throws Throwable {
        Class<?> c = MethodHandles.lookup().defineHiddenClass(bytes, false, NESTMATE);
        assertTrue(c.getNestHost() == DefineHiddenClassTest.class);
        assertTrue(c.isHiddenClass());

        // Teleport to a nestmate
        Lookup lookup =  MethodHandles.lookup().in(c);
        assertTrue((lookup.lookupModes() & PRIVATE) == 0);
        // fail to define a nestmate
        lookup.defineHiddenClass(bytes, false, NESTMATE);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void notSamePackage() throws Throwable {
        MethodHandles.lookup().defineHiddenClass(classBytes("p/Injected"), false, NESTMATE);
    }

    /*
     * invoke int test(DefineHiddenClassTest o) method defined in the injected class
     */
    private int testInjectedClass(Class<?> c) throws Throwable {
        try {
            Method m = c.getMethod("test", DefineHiddenClassTest.class);
            return (int) m.invoke(c.newInstance(), this);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
