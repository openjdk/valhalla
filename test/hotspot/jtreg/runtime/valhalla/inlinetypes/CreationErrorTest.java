/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.*;
import java.lang.ref.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static jdk.test.lib.Asserts.*;
import test.java.lang.invoke.lib.InstructionHelper;

import jdk.experimental.bytecode.MacroCodeBuilder;
import jdk.experimental.bytecode.MacroCodeBuilder.CondKind;
import jdk.experimental.bytecode.TypeTag;
import jdk.test.lib.Platform;
import jdk.test.lib.Utils;

import javax.tools.*;

/**
 * @test CreationErrorTest
 * @summary Test data movement with inline types
 * @library /test/lib /test/jdk/lib/testlibrary/bytecode /test/jdk/java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @run main/othervm -Xmx128m
 *                   runtime.valhalla.inlinetypes.CreationErrorTest
 */

public class CreationErrorTest {

    static primitive class InlineClass {
        int i = 0;
    }

    static class IdentityClass {
        long l = 0L;
    }

    static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static void main(String[] args) {
        testErroneousObjectCreation();
        testErroneousValueCreation();
    }

    static void testErroneousObjectCreation() {
        MethodHandle testNewOnInlineClass = InstructionHelper.loadCode(
                LOOKUP,
                "testNewOnInlineClass",
                MethodType.methodType(boolean.class),
                CODE -> {
                    CODE.new_(InlineClass.class)
                        .iconst_1()
                        .return_(TypeTag.Z);
                });
        Throwable error = null;
        try {
            boolean result = (boolean) testNewOnInlineClass.invokeExact();
        } catch (Throwable t) {
            error = t;
        }
        System.out.println("error="+error);
        assertTrue(error != null && error instanceof InstantiationError, "Invariant");

    }

    // Note: this test might become obsolete if aconst_init is extended to accept identity classes
    static void testErroneousValueCreation() {
        MethodHandle testAconstInitOnIdentityClass = InstructionHelper.loadCode(
                LOOKUP,
                "testAconstInitOnIdentityClass",
                MethodType.methodType(boolean.class),
                CODE -> {
                    CODE.aconst_init(IdentityClass.class)
                        .iconst_1()
                        .return_(TypeTag.Z);
                });
        Throwable error = null;
        try {
            boolean result = (boolean) testAconstInitOnIdentityClass.invokeExact();
        } catch (Throwable t) {
            error = t;
        }
        System.out.println("error="+error);
        assertTrue(error != null && error instanceof IncompatibleClassChangeError, "Invariant");

    }
}
