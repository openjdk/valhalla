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
 * @bug 8222634
 * @summary Check array creation with V and V.ref
 * @modules jdk.compiler/com.sun.tools.javac.util jdk.jdeps/com.sun.tools.javap
 * @compile ArrayCreationWithQuestion.java
 * @run main/othervm -Xverify:none ArrayCreationWithQuestion
 * @modules jdk.compiler
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

public class ArrayCreationWithQuestion {

    static primitive class VT {
        VT.ref[] a1 = new VT.ref[42];
        VT.ref[] a2 = new VT.ref[42];
        VT[] a3 = new VT[42];
        VT[] a4 = new VT[42];
    }

    public static void main(String[] args) {
        new ArrayCreationWithQuestion().run();
    }

    void run() {
        String [] params = new String [] { "-v",
                                            Paths.get(System.getProperty("test.classes"),
                                                "ArrayCreationWithQuestion$VT.class").toString() };
        runCheck(params, new String [] {
        "         6: anewarray     #3                  // class ArrayCreationWithQuestion$VT$ref",
        "        17: anewarray     #3                  // class ArrayCreationWithQuestion$VT$ref",
        "        28: anewarray     #12                 // class \"QArrayCreationWithQuestion$VT;\"",
        "        39: anewarray     #12                 // class \"QArrayCreationWithQuestion$VT;\"",
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
