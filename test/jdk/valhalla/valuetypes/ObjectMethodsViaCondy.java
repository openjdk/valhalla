/*
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test ObjectMethods::bootstrap call via condy
 * @modules java.base/jdk.internal.value
 *          java.base/jdk.internal.org.objectweb.asm
 * @run testng/othervm -XX:+EnableValhalla ObjectMethodsViaCondy
 */

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup.ClassOption;
import java.lang.invoke.MethodType;
import java.lang.invoke.TypeDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

import jdk.internal.value.PrimitiveClass;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.ConstantDynamic;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Type;
import org.testng.annotations.Test;
import static java.lang.invoke.MethodType.methodType;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_FINAL;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_IDENTITY;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_STATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ALOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.ARETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.H_GETFIELD;
import static jdk.internal.org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static jdk.internal.org.objectweb.asm.Opcodes.RETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.V19;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

public class ObjectMethodsViaCondy {
    public static value record ValueRecord(int i, String name) {
        static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
        static final MethodType TO_STRING_DESC = methodType(String.class, ValueRecord.class);
        static final Handle[] ACCESSORS = accessors();
        static final String NAME_LIST = "i;name";
        private static Handle[] accessors() {
            try {
                return  new Handle[]{
                        new Handle(H_GETFIELD, Type.getInternalName(ValueRecord.class), "i", "I", false),
                        new Handle(H_GETFIELD, Type.getInternalName(ValueRecord.class), "name", String.class.descriptorString(), false)
                };
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        /**
         * Returns the method handle for the given method for this ValueRecord class.
         * This method defines a hidden class to invoke the ObjectMethods::bootstrap method
         * via condy.
         *
         * @param methodName   the name of the method to generate, which must be one of
         *                     {@code "equals"}, {@code "hashCode"}, or {@code "toString"}
         */
        static MethodHandle makeBootstrapMethod(String methodName) throws Throwable {
            ClassFileBuilder builder = new ClassFileBuilder("Test-" + methodName);
            builder.bootstrapMethod(methodName, TO_STRING_DESC, ValueRecord.class, NAME_LIST, ACCESSORS);
            byte[] bytes = builder.build();
            MethodHandles.Lookup lookup = LOOKUP.defineHiddenClass(bytes, true, ClassOption.NESTMATE);
            MethodType mtype = MethodType.methodType(Object.class);
            MethodHandle mh = lookup.findStatic(lookup.lookupClass(), "bootstrap", mtype);
            return (MethodHandle) mh.invoke();
        }
    }

    @Test
    public void testToString() throws Throwable {
        MethodHandle handle = ValueRecord.makeBootstrapMethod("toString");
        assertEquals((String)handle.invokeExact(new ValueRecord(10, "ten")), "ValueRecord[i=10, name=ten]");
        assertEquals((String)handle.invokeExact(new ValueRecord(40, "forty")), "ValueRecord[i=40, name=forty]");
    }

    @Test
    public void testToEquals() throws Throwable {
        MethodHandle handle = ValueRecord.makeBootstrapMethod("equals");
        assertTrue((boolean)handle.invoke(new ValueRecord(10, "ten"), new ValueRecord(10, "ten")));
        assertFalse((boolean)handle.invoke(new ValueRecord(11, "eleven"), new ValueRecord(10, "ten")));
    }

    static class ClassFileBuilder {
        private static final String OBJECT_CLS = "java/lang/Object";
        private static final String OBJ_METHODS_CLS = "java/lang/runtime/ObjectMethods";
        private static final String BSM_DESCR =
                MethodType.methodType(Object.class, MethodHandles.Lookup.class, String.class,
                                      TypeDescriptor.class, Class.class, String.class, MethodHandle[].class)
                          .descriptorString();
        private final ClassWriter cw;
        private final String classname;

        /**
         * A builder to generate a class file to access class data
         *
         * @param classname
         */
        ClassFileBuilder(String classname) {
            this.classname = classname;
            this.cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cw.visit(V19, ACC_FINAL | ACC_IDENTITY, classname, null, OBJECT_CLS, null);
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, OBJECT_CLS, "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        byte[] build() {
            cw.visitEnd();
            byte[] bytes = cw.toByteArray();
            Path p = Paths.get(classname + ".class");
            try (OutputStream os = Files.newOutputStream(p)) {
                os.write(bytes);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return bytes;
        }

        /*
         * Generate the bootstrap method that invokes ObjectMethods::bootstrap via condy
         */
        void bootstrapMethod(String name, TypeDescriptor descriptor, Class<?> recordClass, String names, Handle[] getters) {
            MethodType mtype = MethodType.methodType(Object.class);
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC|ACC_STATIC,
                    "bootstrap", mtype.descriptorString(), null, null);
            mv.visitCode();
            Handle bsm = new Handle(H_INVOKESTATIC, OBJ_METHODS_CLS, "bootstrap",
                                    BSM_DESCR, false);
            Object[] args = Stream.concat(Stream.of(Type.getType(recordClass), names), Arrays.stream(getters)).toArray();
            ConstantDynamic dynamic = new ConstantDynamic(name, MethodHandle.class.descriptorString(), bsm, args);
            mv.visitLdcInsn(dynamic);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }
}
