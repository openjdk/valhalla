/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8349945
 * @summary Tracking of strict static fields
 * @enablePreview
 * @compile Bnoinit_BAD.jasm Brbefore_BAD.jasm
 * @compile --add-exports=java.base/jdk.internal.vm.annotation=ALL-UNNAMED StrictStaticFieldsTest.java
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions StrictStaticFieldsTest
 */

import java.lang.reflect.*;
import jdk.internal.vm.annotation.Strict;

public class StrictStaticFieldsTest {
    public static void main(String[] args) throws Exception {
        // --------------
        // POSITIVE TESTS
        // --------------

        // Base Case
        printStatics(Aregular_OK.class);

        // Strict statics initialized to null and zero
        printStatics(Anulls_OK.class);

        // Assign strict static twice
        printStatics(Arepeat_OK.class);

        // Read and write initialized strict static
        printStatics(Aupdate_OK.class);

        // --------------
        // NEGATIVE TESTS
        // --------------

        // Strict statics not initialized
        try {
            printStatics(Bnoinit_BAD.class);
            throw new RuntimeException("Should throw");
        } catch(ExceptionInInitializerError ex) {
            Throwable e = (ex.getCause() != null) ? ex.getCause() : ex;
            if (!e.getMessage().contains("unset after initialization")) {
                throw new RuntimeException("wrong exception: " + e.getMessage());
            }
            e.printStackTrace();
        }

        try {
            printStatics(Brbefore_BAD.class);
            throw new RuntimeException("Should throw");
        } catch(ExceptionInInitializerError ex) {
            Throwable e = (ex.getCause() != null) ? ex.getCause() : ex;
            if (!e.getMessage().contains("is unset before first read")) {
                throw new RuntimeException("wrong exception: " + e.getMessage());
            }
            e.printStackTrace();
        }

        System.out.println("Passed");
    }

    static void printStatics(Class<?> cls) throws Exception {
        Field f1 = cls.getDeclaredField("F1__STRICT");
        Field f2 = cls.getDeclaredField("F1__STRICT");
        System.out.println(f1.get(null));
        System.out.println(f2.get(null));
    }
}

class Aregular_OK {
        @Strict static final String F1__STRICT = "hello";
        @Strict static final int    F2__STRICT = 42;
    }

class Anulls_OK {
    @Strict static String F1__STRICT = null;
    @Strict static int    F2__STRICT = 0;
}

class Arepeat_OK {
    @Strict static String F1__STRICT = "hello";
    @Strict static int    F2__STRICT = 42;
    static {
        System.out.print("(making second putstatic)");
        F2__STRICT = 43;
    }
}

class Aupdate_OK {
    @Strict static String F1__STRICT = "hello";
    @Strict static int    F2__STRICT = 42;
    static {
        System.out.println("(making getstatic and second putstatic)");
        F2__STRICT++;
    }
}
