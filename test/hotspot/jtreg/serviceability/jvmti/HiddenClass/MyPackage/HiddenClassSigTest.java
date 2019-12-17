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

/**
 * @test
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 * @compile HiddenClassSigTest.java
 * @run main/othervm/native -agentlib:HiddenClassSigTest MyPackage.HiddenClassSigTest
 */

package MyPackage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.Utils;
import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.internal.misc.Unsafe;


interface Test<T> {
    String test(T t);
}

class HiddenClassSig<T> implements Test<T> {
    private String realTest() { return "HiddenClassSig: "; }

    public String test(T t) {
        String str = realTest();
        return str + t.toString();
    }
}

public class HiddenClassSigTest {
    private static void log(String str) { System.out.println(str); }

    private static final String HCName = "MyPackage/HiddenClassSig.class";
    private static final String DIR = Utils.TEST_CLASSES;

    static native void checkHiddenClassSig(Class klass);
    static native boolean checkFailed();

    static {
        try {
            System.loadLibrary("HiddenClassSigTest");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load HiddenClassSigTest library");
            System.err.println("java.library.path: "
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    static byte[] readClassFile(String classFileName) throws Exception {
        File classFile = new File(classFileName);
        try (FileInputStream in = new FileInputStream(classFile);
             ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return out.toByteArray();
        }
    }

    static Class<?> defineHiddenClass(String classFileName) throws Exception {
        Lookup lookup = MethodHandles.lookup();
        byte[] bytes = readClassFile(DIR + File.separator + classFileName);
        Class<?> hc = lookup.defineHiddenClass(bytes, false).lookupClass();
        return hc;
    }

    public static void main(String args[]) throws Exception {
        log("HiddenClassSigTest: started");
        Class<?> hc = defineHiddenClass(HCName);
        checkHiddenClassSig(hc);
        if (checkFailed()) {
          throw new RuntimeException("FAIL: failed status from native agent");
        }
        log("HiddenClassSigTest: finished");
    }
}
