/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package runtime.valhalla.valuetypes;

import jdk.incubator.mvt.ValueType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import static java.lang.reflect.Modifier.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import jdk.experimental.bytecode.*;

import jdk.internal.org.objectweb.asm.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

import static jdk.test.lib.Asserts.*;

/*
 * @test DeriveValueTypeCreation
 * @summary Derive Value Type creation test
 * @library /test/lib
 * @compile DeriveValueTypeCreation.java
 * @modules java.base/jdk.experimental.bytecode
 *          java.base/jdk.internal.org.objectweb.asm
 *          jdk.incubator.mvt
 * @build runtime.valhalla.valuetypes.ValueCapableClass
 * @run main/othervm -Xint -XX:+EnableMVT runtime.valhalla.valuetypes.DeriveValueTypeCreation
 * @run main/othervm -Xcomp -XX:+EnableMVT runtime.valhalla.valuetypes.DeriveValueTypeCreation
 */
public class DeriveValueTypeCreation {

    public static final String VCC_CLASSNAME = "runtime.valhalla.valuetypes.ValueCapableClass";
    public static final String DVT_SUFFIX = "$Value";

    public static final int TEST_DEF_CLASS_ACCESS = ACC_SUPER | ACC_PUBLIC | ACC_FINAL;
    public static final int TEST_DEF_FIELD_ACCESS = ACC_PUBLIC | ACC_FINAL;
    public static final String OBJECT_CLASS_DESC  = "java/lang/Object";

    public static void main(String[] args) {
        DeriveValueTypeCreation test = new DeriveValueTypeCreation();
        test.run();
    }

    public void run() {
        loadAndRunTest();
        notValueCapableClasses();
        loadDvtFirst();
    }

    void loadAndRunTest() {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(VCC_CLASSNAME, true, getClass().getClassLoader());
            clazz.getDeclaredMethod("test").invoke(null);
        }
        catch (ClassNotFoundException cnfe) { fail("VCC class missing", cnfe); }
        catch (NoSuchMethodException nsme) { fail("VCC test method missing", nsme); }
        catch (Throwable t) { fail("Failed to invoke VCC.test()", t); }

        checkValueCapableClass(clazz);
    }

    void checkValueCapableClass(Class<?> clazz) {
        if (!ValueType.classHasValueType(clazz)) {
            fail("!classHasValueType: " + clazz);
        }

        ValueType<?> vt = ValueType.forClass(clazz);
        if (vt == null) {
            fail("ValueType.forClass failed");
        }

        System.out.println("ValueType: " + vt);

        if (vt.boxClass() != clazz) {
            fail("ValueType.boxClass() failed");
        }

        // DVT class matches our expectations for the current implementation...
        Class<?> vtClass = vt.valueClass();
        if (!vtClass.getName().equals(clazz.getName() + DVT_SUFFIX)) {
            fail("ValueType.valueClass() failed");
        }
        if (!vtClass.getSuperclass().getName().equals("java.lang.__Value")) {
            fail("ValueType.valueClass() isn't a Value Type class");
        }

        // Exercise "Class.getSimpleName()", we've cause problems with it before
        String sn = vtClass.getSimpleName();
        System.out.println("SimpleName: " + sn);

        if (clazz.getClassLoader() != vtClass.getClassLoader()) {
            fail("ClassLoader mismatch");
        }
        if (clazz.getProtectionDomain() != vtClass.getProtectionDomain()) {
            fail("ProtectionDomain mismatch");
        }
    }

    void notValueCapableClasses() {
        // First a control test to check createTestVccClass is working
        try {
            Class<?> cls = createTestVccClass("Control_Case_is_a_VCC", TEST_DEF_CLASS_ACCESS, OBJECT_CLASS_DESC, "I", TEST_DEF_FIELD_ACCESS);
            checkValueCapableClass(cls);
        }
        catch (Exception e) {
            fail("Control test failed", e);
        }

        testFailCase("Not_a_final_class", ACC_SUPER | ACC_PUBLIC, OBJECT_CLASS_DESC, "I", TEST_DEF_FIELD_ACCESS, "not a final class");
        testFailCase("No_fields", TEST_DEF_CLASS_ACCESS, OBJECT_CLASS_DESC, null, TEST_DEF_FIELD_ACCESS, "has no instance fields");
        testFailCase("Not_final_field", TEST_DEF_CLASS_ACCESS, OBJECT_CLASS_DESC, "I", ACC_PUBLIC, "contains non-final instance field");
        testFailCase("Super_not_Object", TEST_DEF_CLASS_ACCESS, "java/lang/Throwable", "I", TEST_DEF_FIELD_ACCESS, "does not derive from Object");
    }

    void testFailCase(String clsName,
                      int klassAccess,
                      String superKlass,
                      String fieldType,
                      int fieldAccess,
                      String errMsgRequired) {
        try {
            createTestVccClass(clsName, klassAccess, superKlass, fieldType, fieldAccess);
            fail(clsName + " : failed to fail with Error");
        }
        catch (ClassNotFoundException cnfe) {
            fail(clsName + " : Unexpected ClassNotFoundException", cnfe);
        }
        catch (Error err) {
            if (!err.getMessage().contains(errMsgRequired)) {
                fail(clsName + " : Not the error we were looking for", err);
            }
        }
    }

    byte[] createTestVccClassBytes(String name,
                                boolean vccAnnotation) {
        return createTestVccClassBytes(name, TEST_DEF_CLASS_ACCESS, vccAnnotation, OBJECT_CLASS_DESC, "I", TEST_DEF_FIELD_ACCESS);
    }

    byte[] createTestVccClassBytes(String name,
                                int klassAccess,
                                String superKlass,
                                String fieldType,
                                int fieldAccess)  {
        return createTestVccClassBytes(name, klassAccess, true, superKlass, fieldType, fieldAccess);
    }

    byte[] createTestVccClassBytes(String name,
                                int klassAccess,
                                boolean vccAnnotation,
                                String superKlass,
                                String fieldType,
                                int fieldAccess)  {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(52, klassAccess, name, null, superKlass, null);
        if (vccAnnotation ) {
            cw.visitAnnotation("Ljdk/incubator/mvt/ValueCapableClass;", true);
        }
        if (fieldType != null) {
            cw.visitField(fieldAccess, "x", fieldType, null, null);
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    Class<?> createTestVccClass(String name,
                             int klassAccess,
                             String superKlass,
                             String fieldType,
                             int fieldAccess) throws ClassNotFoundException {
        return new TestClassLoader(name,
                                   createTestVccClassBytes(name, klassAccess, superKlass, fieldType, fieldAccess)).loadClass(name);
    }

    class TestClassLoader extends ClassLoader {

        HashMap<String, byte[]> namedBytes = new HashMap<>();
        ArrayList<String> findNames = new ArrayList<String>();

        TestClassLoader() {}
        TestClassLoader(String name, byte[] classBytes) {
            addNamedBytes(name, classBytes);
        }

        void addNamedBytes(String name, byte[] classBytes) {
            namedBytes.put(name, classBytes);
        }

        @Override
        public Class findClass(String name) throws ClassNotFoundException {
            byte[] classBytes = null;
            synchronized (findNames) {
                findNames.add(name);
                classBytes = namedBytes.get(name);
            }
            if (classBytes != null) {
                return defineClass(name, classBytes, 0, classBytes.length);
            }
            throw new ClassNotFoundException(name);
        }

        public ArrayList<String> getFindNames() {
            return findNames;
        }
    }

    void loadDvtFirst() {
        try {
            loadDvtFirstNoVcc();
            loadDvtFirstNotVcc();
            loadDvtFirstBadVcc();
            loadDvtFirstVcc();
        } catch (Throwable t) {
            fail("loadDvtFirst failed", t);
        }
    }

    void loadDvtFirstNoVcc() throws Throwable {
        String vccName = "TestNoVcc";
        try {
            newDvtUserInstance(vccName, null, false);
        } catch (NoClassDefFoundError ncdfe) {}
        try {
            newDvtUserInstance(vccName, null, true);
        } catch (NoClassDefFoundError ncdfe) {}
    }

    void loadDvtFirstNotVcc() throws Throwable {
        String vccName = "TestNotVcc";
        byte[] vccBytes = createTestVccClassBytes(vccName, false);
        try {
            newDvtUserInstance(vccName, vccBytes, false);
        } catch (NoClassDefFoundError ncdfe) {}
        try {
            newDvtUserInstance(vccName, vccBytes, true);
        } catch (NoClassDefFoundError ncdfe) {}
    }

    void loadDvtFirstBadVcc() throws Throwable {
        String vccName = "TestBadVcc";
        byte[] vccBytes = createTestVccClassBytes(vccName, TEST_DEF_CLASS_ACCESS,
                                                  true, OBJECT_CLASS_DESC, "I",
                                                  ACC_PUBLIC);
        try {
            newDvtUserInstance(vccName, vccBytes, false);
        } catch (IncompatibleClassChangeError icce) {}
        try {
            newDvtUserInstance(vccName, vccBytes, true);
        } catch (IncompatibleClassChangeError icce) {}
    }

    void loadDvtFirstVcc() throws Throwable {
        String vccName = "TestValidVcc";
        byte[] vccBytes = createTestVccClassBytes(vccName, TEST_DEF_CLASS_ACCESS,
                                                  true, OBJECT_CLASS_DESC, "I",
                                                  TEST_DEF_FIELD_ACCESS);
        newDvtUserInstance(vccName, vccBytes, false);
        newDvtUserInstance(vccName, vccBytes, true);
    }

    void newDvtUserInstance(String vccName, byte[] vccBytes, boolean withField) throws Throwable {
        TestClassLoader cl = new TestClassLoader();
        if (vccBytes != null) {
            cl.addNamedBytes(vccName, vccBytes);
        }
        String dvtUserName = "UseValidDvt";
        String dvtName = vccName + DVT_SUFFIX;
        String dvtFieldDesc = "Q" + dvtName + ";";
        String dvtClassDesc = ";" + dvtFieldDesc;
        byte[] classBytes = createTestDvtUserClassBytes(dvtUserName, dvtClassDesc, (withField) ? dvtFieldDesc : null);
        cl.addNamedBytes(dvtUserName, classBytes);
        try {
            Class.forName(dvtUserName, true, cl).getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException ite) { throw ite.getTargetException(); }
    }

    byte[] createTestDvtUserClassBytes(String className, String dvtDesc, String dvtFieldDesc) {
        BasicClassBuilder builder = new BasicClassBuilder(className, 53, 1)
            .withFlags(Flag.ACC_PUBLIC)
            .withSuperclass("java/lang/Object")
            .withMethod("<init>", "()V", M ->
                M.withFlags(Flag.ACC_PUBLIC).withCode(TypedCodeBuilder::new, C ->
                    C
                    .load(0).invokespecial("java/lang/Object", "<init>", "()V", false)
                    .iconst_1().anewarray(dvtDesc).pop()
                    .return_()));
        if (dvtFieldDesc != null) {
            builder.withField("dvtField", dvtFieldDesc);
        }
        return builder.build();
    }
}
