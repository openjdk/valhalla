/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @run main/othervm/native -agentlib:HiddenClassSigTest P.Q.HiddenClassSigTest
 */

package P.Q;

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

    private static final String HCName = "P/Q/HiddenClassSig.class";
    private static final String DIR = Utils.TEST_CLASSES;
    private static final String LOG_PREFIX = "HiddenClassSigTest: ";
    private static final String ARR_PREFIX = "[[L";
    private static final String ARR_POSTFIX = "[][]";
    private static final String CLASS_PREFIX = "class ";

    static native void checkHiddenClass(Class klass, String sign);
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

    static void logClassInfo(Class<?> klass) {
        log("\n### Testing class: " + klass);
        log(LOG_PREFIX + "isHiddenC: " + klass.isHiddenClass());
        log(LOG_PREFIX + "getName:   " + klass.getName());
        log(LOG_PREFIX + "typeName:  " + klass.getTypeName());
        log(LOG_PREFIX + "toString:  " + klass.toString());
        log(LOG_PREFIX + "toGenStr:  " + klass.toGenericString());
        log(LOG_PREFIX + "elem type: " + klass.componentType());
    }

    static boolean checkName(String name, String expName, String msgPart) {
        boolean failed = false;
        if (!name.equals(expName)) {
            log("Test FAIL: result of " + msgPart + " does not match expectation");
            failed = true;
        }
        return failed;
    }

    static boolean checkNameHas(String name, String expNamePart, String msgPart) {
        boolean failed = false;
        if (name.indexOf(expNamePart) < 0) {
            log("Test FAIL: result of " + msgPart + " does not match expectation");
            failed = true;
        }
        return failed;
    }

    static boolean checkArray(Class<?> arrClass) {
        boolean failed = false;
        Class<?> elemClass = arrClass.componentType();

        String arrName = arrClass.getName();
        String arrType = arrClass.getTypeName();
        String arrStr = arrClass.toString().substring(CLASS_PREFIX.length());
        String arrGen = arrClass.toGenericString();

        String elemName = elemClass.getName();
        String elemType = elemClass.getTypeName();
        String elemStr = elemClass.toString().substring(CLASS_PREFIX.length());
        String elemGen = elemClass.toGenericString();

        if (elemClass.isHiddenClass()) {
            elemGen = elemGen.substring(CLASS_PREFIX.length());
        }
        failed |= checkNameHas(arrName, elemName, "klass.getName()");
        failed |= checkNameHas(arrStr, elemStr, "klass.toString()");
        failed |= checkNameHas(arrType, elemType, "klass.getTypeName()");
        failed |= checkNameHas(arrGen, elemGen, "klass.getGenericString()");
        return failed;
    }

    static boolean testClass(Class<?> klass, int arrLevel, String baseName) {
        boolean failed = false;
        boolean isHidden = (arrLevel == 0);
        String prefix = (arrLevel == 0) ? "" : ARR_PREFIX.substring(2 - arrLevel);
        String postfix = (arrLevel == 0) ? "" : ";";

        logClassInfo(klass);

        String expName = ("" + klass).substring(CLASS_PREFIX.length());
        String expType = baseName;
        String expStr = CLASS_PREFIX + prefix + baseName + postfix;
        String expGen = baseName + "<T>" + ARR_POSTFIX.substring(0, 2*arrLevel);

        if (arrLevel > 0) {
            expType = expType + ARR_POSTFIX.substring(0, 2*arrLevel); 
        } else {
            expGen = CLASS_PREFIX + expGen; 
        }
        failed |= checkName(klass.getName(), expName, "klass.getName()");
        failed |= checkName(klass.getTypeName(), expType, "klass.getTypeName()");
        failed |= checkName(klass.toString(), expStr, "klass.toString()");
        failed |= checkName(klass.toGenericString(), expGen, "klass.toGenericString()");

        if (klass.isHiddenClass() != isHidden) {
            log("Test FAIL: result of klass.isHiddenClass() does not match expectation");
            failed = true;
        }
        if (arrLevel > 0) {
            failed |= checkArray(klass);
        }
        String sign = hcSignature(klass);
        checkHiddenClass(klass, sign);
        return failed;
    }

    static String hcSignature(Class<?> klass) {
        boolean isArray = klass.isArray();
        String sign = klass.getName();
        String prefix  = isArray ? "" : "L";
        String postfix = isArray ? "" : ";";
        int idx = sign.indexOf("/");

        sign = prefix + sign.substring(0, idx).replace('.', '/')
                      + "."
                      + sign.substring(idx + 1, sign.length())
                      + postfix;
        return sign;
    }

    public static void main(String args[]) throws Exception {
        log(LOG_PREFIX + "started");
        Class<?> hc = defineHiddenClass(HCName);
        String baseName = ("" + hc).substring("class ".length());

        Test<String> t = (Test<String>)hc.newInstance();
        String str = t.test("Test generic hidden class");
        log(LOG_PREFIX + "hc.test() returned string: " + str);

        boolean failed = testClass(hc, 0, baseName);

        Class<?> hcArr = hc.arrayType();
        failed |= testClass(hcArr, 1, baseName);

        Class<?> hcArrArr = hcArr.arrayType();
        failed |= testClass(hcArrArr, 2, baseName);

        if (failed) {
          throw new RuntimeException("FAIL: failed status from java part");
        }
        if (checkFailed()) {
          throw new RuntimeException("FAIL: failed status from native agent");
        }
        log(LOG_PREFIX + "finished");
    }
}
