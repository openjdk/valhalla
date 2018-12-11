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
 * @summary test value bootstrap methods
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @compile -XDallowWithFieldOperator ValueBootstrapMethods.java
 * @run main/othervm -XX:+EnableValhalla ValueBootstrapMethods
 */

import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jdk.internal.org.objectweb.asm.Attribute;
import jdk.internal.org.objectweb.asm.ByteVector;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Type;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public class ValueBootstrapMethods {
    private static final String TEST_CLASSES = System.getProperty("test.classes", ".");

    public static void main(String... args) throws Throwable {
        Class<?> test = valueTestClass();
        Value value = Value.make(10, 5.03, "foo", "bar", "goo");

        Method hashCode = test.getMethod("hashCode", Object.class);
        int hash = (int)hashCode.invoke(null, value);
        assertEquals(hash, value.hashCode());

        Method longHashCode = test.getMethod("longHashCode", Object.class);
        long lHash = (long)longHashCode.invoke(null, value);
        assertEquals(lHash, value.longHashCode());

        Method toString = test.getMethod("toString", Object.class);
        String s = (String)toString.invoke(null, value);
        assertEquals(s, value.toString());

        Method equals = test.getMethod("equals", Object.class, Object.class);
        boolean rc = (boolean)equals.invoke(null, value, value);
        if (!rc) {
            throw new RuntimeException("expected equals");
        }
    }

    public static final value class Value {
        private final int i;
        private final double d;
        private final String s;
        private final List<String> l;
        Value() {
            this.i = 0;
            this.d = 0;
            this.s = "default";
            this.l = List.of();
        }
        public static Value make(int i, double d, String s, String... items) {
            Value v = Value.default;
            v = __WithField(v.i, i);
            v = __WithField(v.d, d);
            v = __WithField(v.s, s);
            v = __WithField(v.l, List.of(items));
            return v;
        }

        List<Object> values() {
            return List.of(Value.class.asValueType(), i, d, s, l);
        }

        public int hashCode() {
            return values().hashCode();
        }

        public long longHashCode() {
            long hash = 1;
            for (Object o : values()) {
                hash = 31 * hash + o.hashCode();
            }
            return hash;
        }

        public String toString() {
            System.out.println(l);
            return String.format("[%s, %s, %s, %s, %s]", Value.class.asValueType(),
                                 i, String.valueOf(d), s, l.toString());
        }
    }

    /*
     * Generate ValueTest class
     */
    private static Class<?> valueTestClass() throws Exception {
        Path path = Paths.get(TEST_CLASSES, "ValueTest.class");
        generate(Value.class, "ValueTest", path);
        return Class.forName("ValueTest");
    }

    private static void assertEquals(Object o1, Object expected) {
        if (!Objects.equals(o1, expected)) {
            throw new RuntimeException(o1 + " expected: " + expected);
        }
    }

    static final int CLASSFILE_VERSION = 55;
    static void generate(Class<?> c, String className, Path path) throws IOException {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);

        cw.visit(CLASSFILE_VERSION,
            ACC_SUPER + ACC_PUBLIC + ACC_FINAL + ACC_SYNTHETIC,
            className,
            null,
            "java/lang/Object",
            null
        );


        MethodType mt = MethodType.methodType(CallSite.class,
            MethodHandles.Lookup.class, String.class, MethodType.class, Class.class);
        Handle bootstrap = new Handle(H_INVOKESTATIC, Type.getInternalName(java.lang.invoke.ValueBootstrapMethods.class),
            "makeBootstrapMethod", mt.toMethodDescriptorString(), false);

        Type type = Type.getType(c);
        MethodVisitor mv = cw.visitMethod(
            ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
            "hashCode",
            "(Ljava/lang/Object;)I",
            null,
            null);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitInvokeDynamicInsn("hashCode", "(Ljava/lang/Object;)I",
            bootstrap, type);
        mv.visitInsn(IRETURN);

        mv.visitMaxs(-1, -1);
        mv.visitEnd();

        mv = cw.visitMethod(
            ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
            "longHashCode",
            "(Ljava/lang/Object;)J",
            null,
            null);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitInvokeDynamicInsn("longHashCode", "(Ljava/lang/Object;)J",
            bootstrap, type);
        mv.visitInsn(LRETURN);

        mv.visitMaxs(-1, -1);
        mv.visitEnd();

        mv = cw.visitMethod(
            ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
            "equals",
            "(Ljava/lang/Object;Ljava/lang/Object;)Z",
            null,
            null);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInvokeDynamicInsn("equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z",
            bootstrap, type);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();

        mv = cw.visitMethod(
            ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
            "toString",
            "(Ljava/lang/Object;)Ljava/lang/String;",
            null,
            null);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitInvokeDynamicInsn("toString", "(Ljava/lang/Object;)Ljava/lang/String;",
            bootstrap,  type);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();

        cw.visitEnd();

        byte[] classBytes = cw.toByteArray();
        System.out.println("writing " + path);
        Files.write(path, classBytes);
    }
}
