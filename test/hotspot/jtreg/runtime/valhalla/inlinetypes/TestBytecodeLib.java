/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package runtime.valhalla.inlinetypes;

import java.lang.invoke.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static jdk.test.lib.Asserts.*;

import jdk.experimental.bytecode.*;

import test.java.lang.invoke.lib.InstructionHelper;

/**
 * @test TestBytecodeLib
 * @summary Check bytecode test library generates the correct code for Valhalla changes to JVMS
 * @library /test/lib /test/jdk/lib/testlibrary/bytecode /test/jdk/java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @compile Point.java TestBytecodeLib.java
 * @run main/othervm runtime.valhalla.inlinetypes.TestBytecodeLib
 */

public class TestBytecodeLib {

    static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static void main(String[] args) throws Throwable {
        testAnewarrayDesc();
        testCheckcastDesc();
        // No support in test library for "ldc(Class<?>)" at all in this incarnation of the API, skip it
        testMultianewarrayDesc();
    }

    // anewarray accepts reference and inline reference type
    // checkcast for arrays accepts reference and inline reference array type
    static void testAnewarrayDesc() throws Throwable {
        Class<?> lClass = Point.ref.class;
        Class<?> qClass = Point.val.class;

        String methodName = "anewarrayLQClass";
        MethodType methodType = MethodType.methodType(void.class);
        byte[] codeBytes = InstructionHelper.buildCode(LOOKUP, methodName, methodType,
            CODE -> {
                CODE
                .iconst_3()
                .anewarray(lClass)
                .checkcast(Point.ref[].class)
                .pop()
                .iconst_3()
                .anewarray(qClass)
                .checkcast(Point.val[].class)
                .pop()
                .return_();
            }
        );

        // Verify correct byte-code
        dumpBytes(methodName + ".class", codeBytes);

        // Verify it works
        InstructionHelper.loadCodeBytes(LOOKUP, methodName, methodType, codeBytes).invokeExact();
    }

    // checkcast accepts reference and inline reference type
    static void testCheckcastDesc() throws Throwable {
        Class<?> lClass = Point.ref.class;
        Class<?> qClass = Point.val.class;

        String methodName = "checkcastLQClass";
        MethodType methodType = MethodType.methodType(void.class);
        byte[] codeBytes = InstructionHelper.buildCode(LOOKUP, methodName, methodType,
            CODE -> {
                CODE
                .aconst_init(Point.class)
                .checkcast(lClass) // expect no descriptor here
                .checkcast(qClass) // expect Q-type descriptor here
                .pop()
                .return_();
            }
        );

        // Verify correct byte-code
        dumpBytes(methodName + ".class", codeBytes);

        // Verify it works
        InstructionHelper.loadCodeBytes(LOOKUP, methodName, methodType, codeBytes).invokeExact();
    }

    // multianewarray accepts reference and inline reference type...it naturally does, but...
    // checkcast for multidim arrays accepts reference and inline reference array type
    static void testMultianewarrayDesc() throws Throwable {
        Class<?> lClass = Point.ref[][].class;
        Class<?> qClass = Point.val[][].class;

        String methodName = "multianewarrayLQClass";
        MethodType methodType = MethodType.methodType(void.class);
        byte dimCnt = (byte) 2;
        byte[] codeBytes = InstructionHelper.buildCode(LOOKUP, methodName, methodType,
            CODE -> {
                CODE
                .iconst_3()
                .iconst_4()
                .multianewarray(lClass, dimCnt)
                .checkcast(lClass)
                .pop()
                .iconst_3()
                .iconst_4()
                .multianewarray(qClass, dimCnt)
                .checkcast(qClass)
                .pop()
                .return_();
            }
        );

        // Verify correct byte-code
        dumpBytes(methodName + ".class", codeBytes);

        // Verify it works
        InstructionHelper.loadCodeBytes(LOOKUP, methodName, methodType, codeBytes).invokeExact();
    }

    /*
        Dump the resulting bytes for inspection.

        TODO: Would prefer programmtic use of ClassReader for verification, but only
        when the JVMS on q-types is less fluid (since it is a lot of work),
        so manual inspection for now.

        Dump in the dir above "test-support/<test-suite-run>/scratch/<n>" so it doesn't get clean up at end of run,
        and use a directory "DUMP_CLASS_FILES" (in keeping with MethodHandles classfile dump)
     */
    static void dumpBytes(String name, byte[] bytes) throws java.io.IOException {
        Path path = Paths.get("../DUMP_CLASS_FILES");
        Files.createDirectories(path);
        path = path.resolve(name);
        System.out.println("Dump: " + path);
        Files.write(path, bytes);
    }


}
