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
 * @build  DefineClassWithClassData
 * @run testng/othervm DefineClassWithClassData
 */

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

import jdk.internal.org.objectweb.asm.*;
import org.testng.annotations.Test;

import static java.lang.invoke.MethodHandles.Lookup.ClassProperty.*;
import static java.lang.invoke.MethodHandles.Lookup.PRIVATE;
import static jdk.internal.org.objectweb.asm.Opcodes.*;
import static org.testng.Assert.*;

public class DefineClassWithClassData {

    private int privMethod() { return 1234; }

    /*
     * invoke int test(DefineClassWithClassData o) method defined in the injected class
     */
    private int testInjectedClass(Class<?> c) throws Throwable {
        try {
            Method m = c.getMethod("test", DefineClassWithClassData.class);
            return (int) m.invoke(c.newInstance(), this);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /*
     * Returns the value of the static final "data" field in the injected class
     */
    private Object injectedData(Class<?> c) throws Throwable {
        return c.getDeclaredField("data").get(null);
    }

    private static final List<String> classData = List.of("nestmate", "classdata");

    @Test
    public void defineNestMate() throws Throwable {
        // define a nestmate
        Lookup lookup = MethodHandles.lookup();
        Class<?> c = lookup.defineClassWithClassData(ClassByteBuilder.classBytes("T"), classData, NESTMATE, HIDDEN);
        assertTrue(c.getNestHost() == DefineClassWithClassData.class);
        assertEquals(classData, injectedData(c));

        // invoke int test(DefineClassWithClassData o)
        int x = testInjectedClass(c);
        assertTrue(x == privMethod());

        // dynamic nestmate is not listed in the return array of getNestMembers
        assertTrue(Stream.of(c.getNestHost().getNestMembers()).noneMatch(k -> k == c));
        assertTrue(c.isNestmateOf(DefineClassWithClassData.class));
    }

    @Test
    public void defineHiddenClass() throws Throwable {
        // define a hidden class
        Lookup lookup = MethodHandles.lookup();
        Class<?> c = lookup.defineClassWithClassData(ClassByteBuilder.classBytes("T"), classData, NESTMATE, HIDDEN);
        assertTrue(c.getNestHost() == DefineClassWithClassData.class);
        // assertTrue(c.isHidden());
        assertEquals(classData, injectedData(c));

        // invoke int test(DefineClassWithClassData o)
        int x = testInjectedClass(c);
        assertTrue(x == privMethod());

        // dynamic nestmate is not listed in the return array of getNestMembers
        assertTrue(Stream.of(c.getNestHost().getNestMembers()).noneMatch(k -> k == c));
        assertTrue(c.isNestmateOf(DefineClassWithClassData.class));
    }

    @Test
    public void defineWeakClass() throws Throwable {
        // define a weak class
        Lookup lookup = MethodHandles.lookup().dropLookupMode(Lookup.PRIVATE);
        Class<?> c = lookup.defineClassWithClassData(ClassByteBuilder.classBytes("T"), classData, WEAK);
        assertTrue(c.getNestHost() == c);
        // assertTrue(c.isHidden());
    }

    @Test(expectedExceptions = IllegalAccessException.class)
    public void noPrivateLookupAccess() throws Throwable {
        Lookup lookup = MethodHandles.lookup().dropLookupMode(Lookup.PRIVATE);
        lookup.defineClassWithClassData(ClassByteBuilder.classBytes("T2"), classData, NESTMATE, HIDDEN);
    }

    @Test(expectedExceptions = IllegalAccessException.class)
    public void teleportToNestmate() throws Throwable {
        byte[] classBytes = ClassByteBuilder.classBytes("T");
        Class<?> c = MethodHandles.lookup()
            .defineClassWithClassData(classBytes, classData, NESTMATE, HIDDEN);
        assertTrue(c.getNestHost() == DefineClassWithClassData.class);
        assertEquals(classData, injectedData(c));
        // assertTrue(c.isHidden());

        // Teleport to a nestmate
        Lookup lookup =  MethodHandles.lookup().in(c);
        assertTrue((lookup.lookupModes() & PRIVATE) == 0);
        // fail to define a nestmate
        lookup.defineClassWithClassData(ClassByteBuilder.classBytes("T2"), classData, NESTMATE, HIDDEN);
    }

    static class ClassByteBuilder {
        static final String OBJECT_CLS = "java/lang/Object";
        static final String STRING_CLS = "java/lang/String";
        static final String LIST_CLS = "java/util/List";
        static final String MH_CLS = "java/lang/invoke/MethodHandles";
        static final String LOOKUP_CLS = "java/lang/invoke/MethodHandles$Lookup";
        static final String LOOKUP_SIG = "Ljava/lang/invoke/MethodHandles$Lookup;";
        static final String LIST_SIG = "Ljava/util/List;";

        static byte[] classBytes(String classname) throws Exception {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            MethodVisitor mv;
            FieldVisitor fv;

            String hostClassName = DefineClassWithClassData.class.getName();

            cw.visit(V11, ACC_FINAL, classname, null, OBJECT_CLS, null);
            {
                fv = cw.visitField(ACC_STATIC | ACC_FINAL, "data", LIST_SIG, null, null);
                fv.visitEnd();
            }
            {
                mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
                mv.visitCode();

                // set up try block
                Label lTryBlockStart =   new Label();
                Label lTryBlockEnd =     new Label();
                Label lCatchBlockStart = new Label();
                Label lCatchBlockEnd =   new Label();
                mv.visitTryCatchBlock(lTryBlockStart, lTryBlockEnd, lCatchBlockStart, "java/lang/IllegalAccessException");

                mv.visitLabel(lTryBlockStart);
                mv.visitMethodInsn(INVOKESTATIC, MH_CLS, "lookup", "()" + LOOKUP_SIG);
                mv.visitMethodInsn(INVOKEVIRTUAL, LOOKUP_CLS, "classData", "()Ljava/lang/Object;");
                mv.visitTypeInsn(CHECKCAST, LIST_CLS);
                mv.visitFieldInsn(PUTSTATIC, classname, "data", LIST_SIG);
                mv.visitLabel(lTryBlockEnd);
                mv.visitJumpInsn(GOTO, lCatchBlockEnd);

                mv.visitLabel(lCatchBlockStart);
                mv.visitVarInsn(ASTORE, 0);
                mv.visitTypeInsn(NEW, "java/lang/Error");
                mv.visitInsn(DUP);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Error", "<init>", "(Ljava/lang/Throwable;)V");
                mv.visitInsn(ATHROW);
                mv.visitLabel(lCatchBlockEnd);
                mv.visitInsn(RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            {
                mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESPECIAL, OBJECT_CLS, "<init>", "()V");
                mv.visitInsn(RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
            {
                mv = cw.visitMethod(ACC_PUBLIC, "test", "(L" + hostClassName + ";)I", null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKEVIRTUAL, hostClassName, "privMethod", "()I");
                mv.visitInsn(IRETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            {
                mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "printData", "()V", null, null);
                mv.visitCode();
                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                mv.visitFieldInsn(GETSTATIC, classname, "data", LIST_SIG);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V");
                mv.visitInsn(RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
            cw.visitEnd();
            return cw.toByteArray();
        }
    }
}


