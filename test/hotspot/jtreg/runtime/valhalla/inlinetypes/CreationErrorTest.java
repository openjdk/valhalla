/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.experimental.bytecode.MacroCodeBuilder;
import jdk.experimental.bytecode.MacroCodeBuilder.CondKind;
import jdk.experimental.bytecode.TypeTag;
import jdk.test.lib.Platform;
import jdk.test.lib.Utils;

import jdk.experimental.value.MethodHandleBuilder;

import javax.tools.*;

/**
 * @test CreationErrorTest
 * @summary Test data movement with inline types
 * @modules java.base/jdk.experimental.bytecode
 *          java.base/jdk.experimental.value
 * @library /test/lib
 * @run main/othervm -Xint -Xmx128m -XX:-ShowMessageBoxOnError
 *                   -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -Djava.lang.invoke.MethodHandle.DUMP_CLASS_FILES=false
 *                   runtime.valhalla.inlinetypes.CreationErrorTest
 */

public class CreationErrorTest {

    static inline class InlineClass {
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
        MethodHandle testNewOnInlineClass = MethodHandleBuilder.loadCode(
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

    // Note: this test might become obsolete if defaultvalue is extended to accept identity classes
    static void testErroneousValueCreation() {
        MethodHandle testDefaultvalueOnIdentityClass = MethodHandleBuilder.loadCode(
                LOOKUP,
                "testDefaultValueOnIdentityClass",
                MethodType.methodType(boolean.class),
                CODE -> {
                    CODE.defaultvalue(IdentityClass.class)
                        .iconst_1()
                        .return_(TypeTag.Z);
                });
        Throwable error = null;
        try {
            boolean result = (boolean) testDefaultvalueOnIdentityClass.invokeExact();
        } catch (Throwable t) {
            error = t;
        }
        System.out.println("error="+error);
        assertTrue(error != null && error instanceof IncompatibleClassChangeError, "Invariant");

    }
}
