/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package valhalla.shady;

import jdk.experimental.bytecode.BasicClassBuilder;
import jdk.internal.misc.Unsafe;
import jdk.internal.misc.VM;
import sun.security.action.GetPropertyAction;

import java.lang.annotation.Annotation;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.ProtectionDomain;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

import jdk.internal.misc.JavaLangAccess;
import jdk.internal.misc.SharedSecrets;
public class MinimalValueTypes_1_0 {

    public static final int V53_1 = 1 << 16 | 53;
    public static final int ACC_VALUE = ACC_NATIVE;
    public static final String OBJECT_CLASS_DESC = "java/lang/Object";
    public static final String VALUE_CLASS_DESC = "java/lang/__Value";

    public static final String DERIVE_VALUE_TYPE_DESC = "Ljdk/incubator/mvt/ValueCapableClass;";
    public static final String DERIVE_VT_CLASSNAME_POSTFIX = "$Value";
    public static final int    DERIVE_VT_CLASS_ACCESS = ACC_PUBLIC|ACC_SUPER|ACC_FINAL|ACC_VALUE|ACC_SYNTHETIC;

    public static final boolean DUMP_CLASS_FILES;
    private static final boolean VALUE_TYPE_ENABLED;
    private static final JavaLangAccess JLA;

    static {
        // Use same property as in j.l.invoke.MethodHandleStatics
        Properties props = GetPropertyAction.privilegedGetProperties();
        DUMP_CLASS_FILES = Boolean.parseBoolean(
            props.getProperty("java.lang.invoke.MethodHandle.DUMP_CLASS_FILES"));

        VALUE_TYPE_ENABLED = Boolean.parseBoolean(
            props.getProperty("valhalla.enableValueType"));

        JLA = SharedSecrets.getJavaLangAccess();
    }

    private static final ConcurrentHashMap<Class<?>, ValueTypeHolder<?>> BOX_TO_VT
        = new ConcurrentHashMap<>();

    /**
     * Returns the {@code ValueTypeHolder} representing the value type
     * for the given value capable type.
     *
     * @throws UnsupportedOperationException if MVT is not enabled
     * @throws IllegalArgumentException if the given class is not value capable class
     */
    @SuppressWarnings("unchecked")
    public static <T> ValueTypeHolder<T> getValueFor(Class<T> vcc) {
        if (!MinimalValueTypes_1_0.isValueTypeEnabled()) {
            throw new UnsupportedOperationException("MVT is not enabled");
        }

        if (!MinimalValueTypes_1_0.isValueCapable(vcc)) {
            throw new IllegalArgumentException("Class " + vcc + " not a value capable class");
        }

        ValueTypeHolder<T> vt = (ValueTypeHolder<T>) BOX_TO_VT.get(vcc);
        if (vt != null) {
            return vt;
        }

        Class<T> valueClass = (Class<T>) MinimalValueTypes_1_0.getValueTypeClass(vcc);
        vt = new ValueTypeHolder<T>(vcc, valueClass);
        ValueTypeHolder<T> old = (ValueTypeHolder<T>) BOX_TO_VT.putIfAbsent(vcc, vt);
        if (old != null) {
            vt = old;
        }
        return vt;
    }

    /**
     * Returns the {@code ValueTypeHolder} representing the value type
     * for the given class is a derived value type; otherwise {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static <T> ValueTypeHolder<T> findValueType(Class<T> c) {
        if (MinimalValueTypes_1_0.isValueType(c)) {
            return (ValueTypeHolder<T>) getValueFor(MinimalValueTypes_1_0.getValueCapableClass(c));
        } else {
            return null;
        }
    }

    /**
     * Returns true if MVT is enabled.
     *
     * jdk.incubator.mvt must be resolved in the boot layer.
     */
    public static boolean isValueTypeEnabled() {
        return VALUE_TYPE_ENABLED;
    }

    public static boolean isValueType(Class<?> dvt) {
        return (dvt.getModifiers() & ACC_VALUE) != 0;
    }

    /**
     * Returns true if the given class is a value-capable class, i.e.
     * annotated with @ValueCapableClass.
     */
    public static boolean isValueCapable(Class<?> vcc) {
        if (!isValueTypeEnabled()) {
            return false;
        }

        return ValueClassHelper.hasValueCapableAnnotation(vcc);
    }

    public static Class<?> getValueCapableClass(Class<?> dvt) {
        if (!isValueType(dvt)) {
            throw new IllegalArgumentException(dvt + " is not a derived value type");
        }

        Class<?> c = Class.forName(dvt.getModule(), getValueCapableClassName(dvt.getName()));
        if (c == null || !isValueCapable(c)) {
            throw new InternalError(dvt + " not bound to ValueType");
        }
        return c;
    }

    public static Class<?> getValueTypeClass(Class<?> vcc) {
        if (!isValueCapable(vcc)) {
            throw new IllegalArgumentException(vcc + " is not a value capable class");
        }
        return loadValueTypeClass(vcc, getValueTypeClassName(vcc.getName()));
    }

    public static Class<?> loadValueTypeClass(Class<?> vcc, String className) {
        if (!isValueCapable(vcc)) {
            throw new IllegalArgumentException(vcc.getName() + " is not a value capable class");
        }
        return JLA.loadValueTypeClass(vcc.getModule(), vcc.getClassLoader(), className);
    }

    /**
     * This method is invoked by the VM.
     *
     * @param fds   : name/sig pairs
     * @param fmods : field modifiers
     */
    public static String createDerivedValueType(String vccInternalClassName,
                                                ClassLoader cl,
                                                ProtectionDomain pd,
                                                String[] fds,
                                                int[] fmods) {
        String vtInternalClassName = getValueTypeClassName(vccInternalClassName);
        ValueTypeDesc valueTypeDesc = new ValueTypeDesc(vccInternalClassName, fds, fmods);
        byte[] valueTypeBytes = createValueType(valueTypeDesc);
        Class<?> vtClass = Unsafe.getUnsafe().defineClass(vtInternalClassName, valueTypeBytes, 0, valueTypeBytes.length, cl, pd);
        return vtInternalClassName;
    }

    public static byte[] createValueType(ValueTypeDesc valueTypeDesc) {

        String valueTypeClassName = getValueTypeClassName(valueTypeDesc);

        BasicClassBuilder builder = new BasicClassBuilder(mangleValueClassName(valueTypeClassName), 53, 1)
            .withFlags(DERIVE_VT_CLASS_ACCESS)
            .withSuperclass(mangleValueClassName(VALUE_CLASS_DESC));

        ValueTypeDesc.Field[] fields = valueTypeDesc.getFields();
        for (ValueTypeDesc.Field field : fields) {
            builder.withField(field.name, field.type, F -> F.withFlags(field.modifiers));
        }

        byte[] newBytes = builder.build();
        maybeDump(valueTypeClassName, newBytes);
        return newBytes;
    }

    /** debugging flag for saving generated class files */
    private static final File DUMP_CLASS_FILES_DIR;

    static {
        if (DUMP_CLASS_FILES) {
            try {
                File dumpDir = new File("DUMP_CLASS_FILES");
                if (!dumpDir.exists()) {
                    dumpDir.mkdirs();
                }
                DUMP_CLASS_FILES_DIR = dumpDir;
                System.out.println("Dumping class files to "+DUMP_CLASS_FILES_DIR+"/...");
            } catch (Exception e) {
                throw new InternalError(e);
            }
        }
        else {
            DUMP_CLASS_FILES_DIR = null;
        }
    }

    public static void maybeDump(final String className, final byte[] classFile) {
        if (!DUMP_CLASS_FILES) {
            return;
        }

        java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<>() {
                public Void run() {
                    String dumpName = className;
                    File dumpFile = new File(DUMP_CLASS_FILES_DIR, dumpName + ".class");
                    File root = dumpFile.getParentFile();
                    if (!root.exists() && !root.mkdirs()) {
                        throw new IllegalStateException("Could not create dump file directory: " + root);
                    }
                    System.out.println("dump: " + dumpFile);
                    try (OutputStream os = new FileOutputStream(dumpFile);
                         BufferedOutputStream bos = new BufferedOutputStream(os)) {
                        bos.write(classFile);
                    } catch (IOException ex) {
                        throw new InternalError(ex);
                    }
                    return null;
                }
            });
    }

    private final native Class<?> getDerivedValueType(Class<?> ofClass);

    public static Class<?> getValueClass() {
        return isValueTypeEnabled() ? ValueClassHelper.VALUE_CLASS : null;
    }

    public static String getValueTypeClassName(ValueTypeDesc valueTypeDesc) {
        return getValueTypeClassName(valueTypeDesc.getName());
    }

    public static String getValueTypeClassName(String vccName) {
        return vccName + DERIVE_VT_CLASSNAME_POSTFIX;
    }

    public static String getValueCapableClassName(String valName) {
        return valName.substring(0, valName.length() - DERIVE_VT_CLASSNAME_POSTFIX.length());
    }

    public static String mangleValueClassName(String name) {
        return ";Q" + name + ";";
    }

    /*
     * This helper class should only be loaded when MVT is enabled.
     * Otherwise, it will load __Value but if MVT is not enabled and
     * that would fail verification.
     */
    private static class ValueClassHelper {
        static final Class<?> VALUE_CLASS =
             (Class<?>)(Object)__Value.class; //hack around static type-system checks

        static volatile Class<? extends Annotation> annotationClass;
        static volatile Class<?> valueTypeClass;

        static boolean hasValueCapableAnnotation(Class<?> c) {
            if (!VM.isModuleSystemInited()) {
                return false;
            }
            Class<? extends Annotation> annClass = annotationClass;
            if (annClass == null) {
                annotationClass = annClass = loadValueCapableAnnotation();
            }
            return annClass != null &&
                    c.getDeclaredAnnotation(annClass) != null;
        }

        /*
         * Returns jdk.incubator.mvt.ValueCapableClass annotation class
         */
        static Class<? extends Annotation> loadValueCapableAnnotation() {
            Module module = ModuleLayer.boot().findModule("jdk.incubator.mvt")
                                       .orElse(null);
            if (module != null) {
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> c = (Class<? extends Annotation>)
                    Class.forName(module, "jdk.incubator.mvt.ValueCapableClass");
                return c;
            } else {
                return null;
            }
        }

        /*
         * Returns jdk.incubator.mvt.ValueType class
         */
        static Class<?> incubatorValueTypeClass() {
            Class<?> c = valueTypeClass;
            if (c == null) {
                Module module = ModuleLayer.boot().findModule("jdk.incubator.mvt")
                                           .orElse(null);
                if (module != null) {
                    valueTypeClass = c =
                        Class.forName(module, "jdk.incubator.mvt.ValueType");
                }
            }
            return valueTypeClass;
        }
    }

    /*
     * Returns jdk.incubator.mvt.ValueType class from jdk.incubator.mvt module
     */
    static Class<?> getIncubatorValueTypeClass() {
        if (!isValueTypeEnabled()) {
            throw new UnsupportedOperationException("MVT is not enabled");
        }
        return ValueClassHelper.incubatorValueTypeClass();
    }
}
