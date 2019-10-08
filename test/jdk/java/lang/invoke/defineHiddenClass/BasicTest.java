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
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.compiler.CompilerUtils
 *        BasicTest
 * @run testng/othervm BasicTest
 */

// Temporarily disabled until isHidden intrinsic is fixed.
// @run testng/othervm -Xcomp BasicTest

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.lookup;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.Utils;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

interface HiddenTest {
    void test();
}

public class BasicTest {

    private static final Path SRC_DIR = Paths.get(Utils.TEST_SRC, "src");
    private static final Path CLASSES_DIR = Paths.get("classes");
    private static final Path CLASSES_10_DIR = Paths.get("classes_10");

    @BeforeTest
    static void setup() throws IOException {
        compileSources(SRC_DIR, CLASSES_DIR);

        // compile with --release 10 with no NestHost and NestMembers attribute
        compileSources(SRC_DIR.resolve("Outer.java"), CLASSES_10_DIR, "--release", "10");
        compileSources(SRC_DIR.resolve("EnclosingClass.java"), CLASSES_10_DIR, "--release", "10");
    }

    static void compileSources(Path sourceFile, Path dest, String... options) throws IOException {
        Stream<String> ops = Stream.of("-cp", Utils.TEST_CLASSES + File.pathSeparator + CLASSES_DIR);
        if (options != null && options.length > 0) {
            ops = Stream.concat(ops, Arrays.stream(options));
        }
        if (!CompilerUtils.compile(sourceFile, dest, ops.toArray(String[]::new))) {
            throw new RuntimeException("Compilation of the test failed: " + sourceFile);
        }
    }

    static Class<?> defineHiddenClass(String name) throws Exception {
        byte[] bytes = Files.readAllBytes(CLASSES_DIR.resolve(name + ".class"));
        Class<?> hc = lookup().defineHiddenClass(bytes, false).lookupClass();
        assertHiddenClass(hc);
        return hc;
    }

    // basic test on a hidden class
    @Test
    public void hiddenClass() throws Throwable {
        HiddenTest t = (HiddenTest)defineHiddenClass("HiddenClass").newInstance();
        t.test();

        Class<?> c = t.getClass();
        Class<?>[] intfs = c.getInterfaces();
        assertTrue(c.isHiddenClass());
        assertFalse(c.isPrimitive());
        assertTrue(intfs.length == 1);
        assertTrue(intfs[0] == HiddenTest.class);
        assertTrue(c.getCanonicalName() == null);

        // test array of hidden class
        testHiddenArray(c);

        // test setAccessible
        checkSetAccessible(c, "realTest");
        checkSetAccessible(c, "test");
    }

    @Test
    public void primitiveClass() {
        assertFalse(int.class.isHiddenClass());
        assertFalse(String.class.isHiddenClass());
    }

    private void testHiddenArray(Class<?> type) throws Exception {
        // array of hidden class
        Object array = Array.newInstance(type, 2);
        Class<?> arrayType = array.getClass();
        assertTrue(arrayType.isArray());
        assertTrue(Array.getLength(array) == 2);
        assertFalse(arrayType.isHiddenClass());

        assertTrue(arrayType.getComponentType().isHiddenClass());
        assertTrue(arrayType.getComponentType() == type);
        Object t = type.newInstance();
        Array.set(array, 0, t);
        Object o = Array.get(array, 0);
        assertTrue(o == t);
    }

    private void checkSetAccessible(Class<?> c, String name, Class<?>... ptypes) throws Exception {
        Method m = c.getDeclaredMethod(name, ptypes);
        assertTrue(m.trySetAccessible());
        m.setAccessible(true);
    }

    // define a hidden class that uses lambda whic
    @Test
    public void testLambda() throws Throwable {
        HiddenTest t = (HiddenTest)defineHiddenClass("Lambda").newInstance();
        try {
            t.test();
        } catch (Error e) {
            if (!e.getMessage().equals("thrown by " + t.getClass().getName())) {
                throw e;
            }
        }
    }

    @Test
    public void testHiddenNestHost() throws Throwable {
        byte[] hc1 = Files.readAllBytes(CLASSES_DIR.resolve("HiddenClass.class"));
        Lookup lookup1 = lookup().defineHiddenClass(hc1, false);
        byte[] hc2 = Files.readAllBytes(CLASSES_DIR.resolve("Lambda.class"));
        Lookup lookup2 = lookup1.defineHiddenClass(hc2, false, Lookup.ClassOption.NESTMATE);
        Class<?> host = lookup1.lookupClass();
        Class<?> member = lookup2.lookupClass();
        assertTrue(host.getNestHost() == host);
        assertTrue(member.getNestHost() == host.getNestHost());
        assertTrue(host.isNestmateOf(member));
        assertTrue(Arrays.equals(member.getNestMembers(), host.getNestMembers()));
        assertTrue(host.getNestMembers().length == 1);
        assertTrue(host.getNestMembers()[0] == host);
    }

    @Test
    public void hiddenCantReflect() throws Throwable {
        HiddenTest t = (HiddenTest)defineHiddenClass("HiddenCantReflect").newInstance();
        t.test();

        Class<?> c = t.getClass();
        Class<?>[] intfs = c.getInterfaces();
        assertTrue(intfs.length == 1);
        assertTrue(intfs[0] == HiddenTest.class);

        try {
            // this will cause class loading of HiddenCantReflect and fail
            c.getDeclaredMethods();
        } catch (NoClassDefFoundError e) {
            Throwable x = e.getCause();
            if (x == null || !(x instanceof ClassNotFoundException && x.getMessage().contains("HiddenCantReflect"))) {
                throw e;
            }
        }
    }

    @Test
    public void hiddenInterface() throws Exception {
        Class<?> hc = defineHiddenClass("HiddenInterface");
        assertTrue(hc.isInterface());
    }

    @Test
    public void abstractHiddenClass() throws Exception {
        Class<?> hc = defineHiddenClass("AbstractClass");
        assertTrue(Modifier.isAbstract(hc.getModifiers()));
    }

    @Test
    public void hiddenOuterClass() throws Throwable {
        defineHiddenClass("Outer");
    }

    @Test(expectedExceptions = NoClassDefFoundError.class)
    public void hiddenSuperClass() throws Exception {
        defineHiddenClass("HiddenSuper");
    }

    @Test
    public void hasStaticNestMembership() throws Exception {
        byte[] bytes = Files.readAllBytes(CLASSES_DIR.resolve("Outer$Inner.class"));
        Class<?> c = lookup().defineHiddenClass(bytes, false).lookupClass();
        declaringClassNotFound(c, "Outer");
    }

    @Test
    public void hasInnerClassesAttribute() throws Throwable {
        byte[] bytes = Files.readAllBytes(CLASSES_10_DIR.resolve("Outer.class"));
        Class<?> c = lookup().defineHiddenClass(bytes, false).lookupClass();
        assertTrue(c.getSimpleName().startsWith("Outer"));

        bytes = Files.readAllBytes(CLASSES_10_DIR.resolve("Outer$Inner.class"));
        c = lookup().defineHiddenClass(bytes, false).lookupClass();
        declaringClassNotFound(c, "Outer");
    }

    @Test
    public void anonymousClass() throws Throwable {
        byte[] bytes = Files.readAllBytes(CLASSES_10_DIR.resolve("EnclosingClass.class"));
        Class<?> c = lookup().defineHiddenClass(bytes, false).lookupClass();
        assertTrue(c.getSimpleName().startsWith("EnclosingClass"));

        bytes = Files.readAllBytes(CLASSES_10_DIR.resolve("EnclosingClass$1.class"));
        c = lookup().defineHiddenClass(bytes, false).lookupClass();
        declaringClassNotFound(c, "EnclosingClass");
    }

    private void declaringClassNotFound(Class<?> c, String cn) {
        try {
            // fail to find declaring/enclosing class
            c.getSimpleName();
            assertTrue(false);
        } catch (NoClassDefFoundError e) {
            if (!e.getMessage().equals(cn)) {
                throw e;
            }
        }
    }

    private static void assertHiddenClass(Class<?> hc) {
        assertTrue(hc.isHiddenClass());
        assertTrue(hc.getNestHost() == hc);
        assertTrue(hc.getNestMembers().length == 1);
        assertTrue(hc.getNestMembers()[0] == hc);
        assertTrue(hc.getCanonicalName() == null);
        assertTrue(hc.getName().contains("/"));  // implementation-specific
        assertFalse(hc.isAnonymousClass());
        assertFalse(hc.isLocalClass());
        assertFalse(hc.isMemberClass());
        assertFalse(hc.getSimpleName().isEmpty()); // sanity check
    }
}
