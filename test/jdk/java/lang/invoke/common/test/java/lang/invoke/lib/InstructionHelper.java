/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.java.lang.invoke.lib;

import jdk.experimental.bytecode.BasicClassBuilder;
import jdk.experimental.bytecode.BasicTypeHelper;
import jdk.experimental.bytecode.BytePoolHelper;
import jdk.experimental.bytecode.ClassBuilder;
import jdk.experimental.bytecode.CodeBuilder;
import jdk.experimental.bytecode.Flag;
import jdk.experimental.bytecode.MethodBuilder;
import jdk.experimental.bytecode.PoolHelper;
import jdk.experimental.bytecode.TypedCodeBuilder;
import jdk.experimental.bytecode.TypeHelper;
import jdk.experimental.bytecode.TypeTag;

import java.io.FileOutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.invoke.MethodType.fromMethodDescriptorString;
import static java.lang.invoke.MethodType.methodType;

import jdk.internal.value.PrimitiveClass;

public class InstructionHelper {

    static final BasicTypeHelper BTH = new BasicTypeHelper();

    static final AtomicInteger COUNT = new AtomicInteger();

    static String generateClassNameFromLookupClass(MethodHandles.Lookup l) {
        return l.lookupClass().getCanonicalName().replace('.', '/') + "$Code_" + COUNT.getAndIncrement();
    }

    static BasicClassBuilder classBuilder(MethodHandles.Lookup l) {
        String className = generateClassNameFromLookupClass(l);
        return new BasicClassBuilder(className, 55, 0)
                .withSuperclass("java/lang/Object")
                .withMethod("<init>", "()V", M ->
                        M.withFlags(Flag.ACC_PUBLIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.aload_0().invokespecial("java/lang/Object", "<init>", "()V", false).return_()
                                ));
    }

    public static MethodHandle invokedynamic(MethodHandles.Lookup l,
                                      String name, MethodType type,
                                      String bsmMethodName, MethodType bsmType,
                                      Consumer<PoolHelper.StaticArgListBuilder<String, String, byte[]>> staticArgs) throws Exception {
        byte[] byteArray = classBuilder(l)
                .withMethod("m", type.toMethodDescriptorString(), M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new,
                                          C -> {
                                              for (int i = 0; i < type.parameterCount(); i++) {
                                                  C.load(BTH.tag(cref(type.parameterType(i))), i);
                                              }
                                              C.invokedynamic(name, type.toMethodDescriptorString(),
                                                              csym(l.lookupClass()), bsmMethodName, bsmType.toMethodDescriptorString(),
                                                              staticArgs);
                                              C.return_(BTH.tag(cref(type.returnType())));
                                          }
                                ))
                .build();
        Class<?> gc = l.defineClass(byteArray);
        return l.findStatic(gc, "m", type);
    }

    public static MethodHandle ldcMethodHandle(MethodHandles.Lookup l,
                                        int refKind, Class<?> owner, String name, MethodType type) throws Exception {
        return ldc(l, MethodHandle.class,
                   P -> P.putHandle(refKind, csym(owner), name, type.toMethodDescriptorString()));
    }

    public static MethodHandle ldcDynamicConstant(MethodHandles.Lookup l,
                                                  String name, Class<?> type,
                                                  String bsmMethodName, MethodType bsmType,
                                                  Consumer<PoolHelper.StaticArgListBuilder<String, String, byte[]>> staticArgs) throws Exception {
        return ldcDynamicConstant(l, name, type, l.lookupClass(), bsmMethodName, bsmType, staticArgs);
    }

    public static MethodHandle ldcDynamicConstant(MethodHandles.Lookup l,
                                                  String name, Class<?> type,
                                                  Class<?> bsmClass, String bsmMethodName, MethodType bsmType,
                                                  Consumer<PoolHelper.StaticArgListBuilder<String, String, byte[]>> staticArgs) throws Exception {
        return ldcDynamicConstant(l, name, cref(type), csym(bsmClass), bsmMethodName, bsmType.toMethodDescriptorString(), staticArgs);
    }

    public static MethodHandle ldcDynamicConstant(MethodHandles.Lookup l,
                                                  String name, String type,
                                                  String bsmMethodName, String bsmType,
                                                  Consumer<PoolHelper.StaticArgListBuilder<String, String, byte[]>> staticArgs) throws Exception {
        return ldcDynamicConstant(l, name, type, csym(l.lookupClass()), bsmMethodName, bsmType, staticArgs);
    }

    public static MethodHandle ldcDynamicConstant(MethodHandles.Lookup l,
                                                  String name, String type,
                                                  String bsmClass, String bsmMethodName, String bsmType,
                                                  Consumer<PoolHelper.StaticArgListBuilder<String, String, byte[]>> staticArgs) throws Exception {
        return ldc(l, type,
                   P -> P.putDynamicConstant(name, type,
                                             bsmClass, bsmMethodName, bsmType,
                                             staticArgs));
    }

    public static MethodHandle ldc(MethodHandles.Lookup l,
                            Class<?> type,
                            Function<PoolHelper<String, String, byte[]>, Integer> poolFunc) throws Exception {
        return ldc(l, cref(type), poolFunc);
    }

    public static MethodHandle ldc(MethodHandles.Lookup l,
                                   String type,
                                   Function<PoolHelper<String, String, byte[]>, Integer> poolFunc) throws Exception {
        String methodType = "()" + type;
        byte[] byteArray = classBuilder(l)
                .withMethod("m", "()" + type, M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new,
                                          C -> {
                                              C.ldc(null, (P, v) -> poolFunc.apply(P));
                                              C.return_(BTH.tag(type));
                                          }
                                ))
                .build();
        Class<?> gc = l.defineClass(byteArray);
        return l.findStatic(gc, "m", fromMethodDescriptorString(methodType, l.lookupClass().getClassLoader()));
    }

    public static String csym(Class<?> c) {
        return c.getCanonicalName().replace('.', '/');
    }

    public static String cref(Class<?> c) {
        return methodType(c).toMethodDescriptorString().substring(2);
    }


    // loadCode(MethodHandles.Lookup, String, MethodType, Consumer<? super MethodHandleCodeBuilder<?>>) et al...

    public static MethodHandle loadCode(MethodHandles.Lookup lookup, String methodName, MethodType type, Consumer<? super MethodHandleCodeBuilder<?>> builder) {
        String className = generateClassNameFromLookupClass(lookup);
        return loadCode(lookup, className, methodName, type, builder);
    }

    public static MethodHandle loadCode(MethodHandles.Lookup lookup, String className, String methodName, MethodType type, Consumer<? super MethodHandleCodeBuilder<?>> builder) {
        String descriptor = type.toMethodDescriptorString();
        return loadCode(lookup, className, methodName, descriptor, MethodHandleCodeBuilder::new,
                    clazz -> {
                        try {
                            return lookup.findStatic(clazz, methodName, MethodType.fromMethodDescriptorString(descriptor, lookup.lookupClass().getClassLoader()));
                        } catch (ReflectiveOperationException ex) {
                            throw new IllegalStateException(ex);
                        }
                    },
                    builder);
    }

    // Helper method to load code built with "buildCode()"
    public static MethodHandle loadCodeBytes(MethodHandles.Lookup lookup, String methodName, MethodType type, byte[] byteCode) {
        try {
            Class<?> clazz = lookup.defineClass(byteCode);
            return lookup.findStatic(clazz, methodName, type);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to loadCodeBytes \"" + methodName + "\"", t);
        }
    }


    private static <Z, C extends CodeBuilder<Class<?>, String, byte[], ?>> Z loadCode(
            MethodHandles.Lookup lookup, String className, String methodName, String type,
            Function<MethodBuilder<Class<?>, String, byte[]>, ? extends C> builderFunc,
            Function<Class<?>, Z> resFunc, Consumer<? super C> builder) {
        try {
            byte[] byteArray = buildCode(lookup, className, methodName, type, builderFunc, builder);
            Class<?> clazz = lookup.defineClass(byteArray);
            return resFunc.apply(clazz);
        } catch (Throwable e) {
             throw new IllegalStateException(e);
        }
    }

    public static byte[] buildCode(MethodHandles.Lookup lookup, String methodName, MethodType type, Consumer<? super MethodHandleCodeBuilder<?>> builder) {
        String className = generateClassNameFromLookupClass(lookup);
        return buildCode(lookup, className, methodName, type.toMethodDescriptorString(), MethodHandleCodeBuilder::new, builder);
    }

    public static <C extends CodeBuilder<Class<?>, String, byte[], ?>> byte[] buildCode(
        MethodHandles.Lookup lookup, String className, String methodName, String type,
            Function<MethodBuilder<Class<?>, String, byte[]>, ? extends C> builderFunc,
            Consumer<? super C> builder) {

                return new IsolatedMethodBuilder(className, lookup)
                    .withSuperclass(Object.class)
                    .withMajorVersion(65)
                    .withMinorVersion(0)
                    .withFlags(Flag.ACC_PUBLIC, Flag.ACC_IDENTITY)
                    .withMethod(methodName, type, M ->
                        M.withFlags(Flag.ACC_STATIC, Flag.ACC_PUBLIC)
                            .withCode(builderFunc, builder)).build();

    }

    private static class IsolatedMethodBuilder extends ClassBuilder<Class<?>, String, IsolatedMethodBuilder> {

        private static final Class<?> THIS_CLASS = new Object() { }.getClass();

        private IsolatedMethodBuilder(String clazz, MethodHandles.Lookup lookup) {
            super(new IsolatedMethodPoolHelper(clazz),
                  new IsolatedMethodTypeHelper(lookup));
            withThisClass(THIS_CLASS);
        }

        public Class<?> thisClass() {
            return THIS_CLASS;
        }

        static String classToInternalName(Class<?> c) {
            if (c.isArray()) {
                return c.descriptorString();
            }
            return c.getName().replace('.', '/');
        }

        private static class IsolatedMethodTypeHelper implements TypeHelper<Class<?>, String> {

            BasicTypeHelper basicTypeHelper = new BasicTypeHelper();
            MethodHandles.Lookup lookup;

            private IsolatedMethodTypeHelper(MethodHandles.Lookup lookup) {
                this.lookup = lookup;
            }

            @Override
            public String elemtype(String s) {
                return basicTypeHelper.elemtype(s);
            }

            @Override
            public String arrayOf(String s) {
                return basicTypeHelper.arrayOf(s);
            }

            @Override
            public Iterator<String> parameterTypes(String s) {
                return basicTypeHelper.parameterTypes(s);
            }

            @Override
            public String fromTag(TypeTag tag) {
                return basicTypeHelper.fromTag(tag);
            }

            @Override
            public String returnType(String s) {
                return basicTypeHelper.returnType(s);
            }

            @Override
            public String type(Class<?> aClass) {
                return aClass.descriptorString();
            }

            @Override
            public boolean isInlineClass(String desc) {
                Class<?> aClass = symbol(desc);
                return aClass != null && PrimitiveClass.isPrimitiveValueType(aClass);
            }

            @Override
            public Class<?> symbol(String desc) {
                try {
                    if (desc.startsWith("[")) {
                        return Class.forName(desc.replaceAll("/", "."), true, lookup.lookupClass().getClassLoader());
                    } else {
                        Class<?> c = Class.forName(basicTypeHelper.symbol(desc).replaceAll("/", "."), true, lookup.lookupClass().getClassLoader());
                        return basicTypeHelper.isInlineClass(desc) ? PrimitiveClass.asValueType(c) : PrimitiveClass.asPrimaryType(c);
                    }
                } catch (ReflectiveOperationException ex) {
                    throw new AssertionError(ex);
                }
            }

            @Override
            public TypeTag tag(String s) {
                return basicTypeHelper.tag(s);
            }

            @Override
            public Class<?> symbolFrom(String s) {
                return symbol(s);
            }

            @Override
            public String commonSupertype(String t1, String t2) {
                return basicTypeHelper.commonSupertype(t1, t2);
            }

            @Override
            public String nullType() {
                return basicTypeHelper.nullType();
            }
        }

        private static class IsolatedMethodPoolHelper extends BytePoolHelper<Class<?>, String> {
            final String clazz;

            private IsolatedMethodPoolHelper(String clazz) {
                super(c -> from(c, clazz), s->s);
                this.clazz = clazz;
            }

            static String from(Class<?> c, String clazz) {
                return c == THIS_CLASS ? clazz.replace('.', '/')
                                       : classToInternalName(c);
            }
        }

        @Override
        public byte[] build() {
            return super.build();
        }
    }

    public static class MethodHandleCodeBuilder<T extends MethodHandleCodeBuilder<T>> extends TypedCodeBuilder<Class<?>, String, byte[], T> {

        BasicTypeHelper basicTypeHelper = new BasicTypeHelper();

        public MethodHandleCodeBuilder(jdk.experimental.bytecode.MethodBuilder<Class<?>, String, byte[]> methodBuilder) {
            super(methodBuilder);
        }

        TypeTag getTagType(String s) {
            return basicTypeHelper.tag(s);
        }

        public T ifcmp(String s, CondKind cond, CharSequence label) {
            return super.ifcmp(getTagType(s), cond, label);
        }

        public T return_(String s) {
            return super.return_(getTagType(s));
        }
    }



}
