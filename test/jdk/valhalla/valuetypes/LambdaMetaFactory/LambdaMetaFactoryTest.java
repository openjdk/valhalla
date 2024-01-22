/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @summary LambdaMetaFactory rejects value or identity superinterface
 * @modules jdk.compiler
 * @library /test/lib
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.compiler.CompilerUtils
 * @run junit LambdaMetaFactoryTest
 */

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.Utils;

import java.io.IOException;
import java.lang.invoke.LambdaConversionException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class LambdaMetaFactoryTest {
    private static final Path SRC_DIR = Paths.get(Utils.TEST_SRC);
    private static final Path CLASSES_DIR = Paths.get("classes");

    @BeforeAll
    static void setup() throws IOException {
        // IdentityRunnable and ValueRunnable under the src directory are non-identity
        // and non-value interface to get Test class to compile
        assertTrue(CompilerUtils.compile(SRC_DIR.resolve("src"), CLASSES_DIR));

        // compile the proper version of IdentityRunnable and ValueRunnable
        assertTrue(CompilerUtils.compile(SRC_DIR.resolve("patch"), CLASSES_DIR));
    }

    @Test
    public void testValueRunnable() throws Throwable {
        URLClassLoader loader = new URLClassLoader("loader",
                                                   new URL[]{ CLASSES_DIR.toUri().toURL()},
                                                   ClassLoader.getPlatformClassLoader());
        Class<?> testClass = Class.forName("Test", false, loader);
        Method m = testClass.getMethod("testValueRunnable");
        try {
            m.invoke(null);
        } catch (InvocationTargetException e) {
            Throwable bme = e.getCause();
            assertTrue(bme.getCause() instanceof LambdaConversionException);
            assertTrue(bme.getCause().getMessage().contains("ValueRunnable is a value interface"));
        }
    }

    @Test
    public void testIdentityRunnable() throws Throwable {
        URLClassLoader loader = new URLClassLoader("loader",
                                                   new URL[]{ CLASSES_DIR.toUri().toURL()},
                                                   ClassLoader.getPlatformClassLoader());
        Class<?> testClass = Class.forName("Test", false, loader);
        Method m = testClass.getMethod("testIdentityRunnable");
        try {
            m.invoke(null);
        } catch (InvocationTargetException e) {
            Throwable bme = e.getCause();
            assertTrue(bme.getCause() instanceof LambdaConversionException);
            assertTrue(bme.getCause().getMessage().contains("IdentityRunnable is an identity interface"));
        }
    }
}
