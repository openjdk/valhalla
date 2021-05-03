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

/*
 * @test
 * @bug 8265423
 * @summary Experimental support for generating a single class file per primitive class
 * @modules jdk.compiler/com.sun.tools.javac.util jdk.jdeps/com.sun.tools.javap
 * @compile SplitPrimitiveClassBytecodeTest.java
 * @run main SplitPrimitiveClassBytecodeTest
 * @modules jdk.compiler
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

public class SplitPrimitiveClassBytecodeTest {

    public primitive class X {

        X.ref xr = null;

        public void foo(X.ref[] xra, X[] xa) {
            xa = new X[10];
            xra = new X.ref[10];
            xra[0] = xa[0];
            xa[1] = xra[0];
            Class<?> c = X.class;
            c = X.ref.class;
        }
    }

    public static void main(String[] args) {
        new SplitPrimitiveClassBytecodeTest().run();
    }

    void run() {
        String [] params = new String [] { "-v",
                                            Paths.get(System.getProperty("test.classes"),
                                                "SplitPrimitiveClassBytecodeTest$X.class").toString() };
        runCheck(params, new String [] {

        // check field
        "final SplitPrimitiveClassBytecodeTest$X$ref xr;",
        "descriptor: LSplitPrimitiveClassBytecodeTest$X$ref;",
        "flags: (0x0010) ACC_FINAL",

        // check method
        "public void foo(SplitPrimitiveClassBytecodeTest$X$ref[], SplitPrimitiveClassBytecodeTest$X[]);",
        "descriptor: ([LSplitPrimitiveClassBytecodeTest$X$ref;[QSplitPrimitiveClassBytecodeTest$X;)V",
        " 0: bipush        10",
        " 2: anewarray     #11                 // class \"QSplitPrimitiveClassBytecodeTest$X;\"",
        " 5: astore_2",
        " 6: bipush        10",
        " 8: anewarray     #13                 // class SplitPrimitiveClassBytecodeTest$X$ref",
        "11: astore_1",
        "12: aload_1",
        "13: iconst_0",
        "14: aload_2",
        "15: iconst_0",
        "16: aaload",
        "17: aastore",
        "18: aload_2",
        "19: iconst_1",
        "20: aload_1",
        "21: iconst_0",
        "22: aaload",
        "23: checkcast     #11                 // class \"QSplitPrimitiveClassBytecodeTest$X;\"",
        "26: aastore",
        "27: ldc           #1                  // class SplitPrimitiveClassBytecodeTest$X",
        "29: astore_3",
        "30: ldc           #13                 // class SplitPrimitiveClassBytecodeTest$X$ref",
        "32: astore_3",
        "33: return",
         });
     }

     void runCheck(String [] params, String [] expectedOut) {
        StringWriter s;
        String out;

        try (PrintWriter pw = new PrintWriter(s = new StringWriter())) {
            com.sun.tools.javap.Main.run(params, pw);
            out = s.toString();
        }
        int errors = 0;
        for (String eo: expectedOut) {
            if (!out.contains(eo)) {
                System.err.println("Match not found for string: " + eo);
                errors++;
            }
        }
         if (errors > 0) {
             throw new AssertionError("Unexpected javap output: " + out);
         }
    }
}
