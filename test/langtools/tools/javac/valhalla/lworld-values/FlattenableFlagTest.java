/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8197799
 * @summary Check generation of ACC_FLATTENABLE flag
 * @modules jdk.compiler/com.sun.tools.javac.util jdk.jdeps/com.sun.tools.javap
 * @compile FlattenableFlagTest.java
 * @run main/othervm -Xverify:none -XX:+EnableValhalla FlattenableFlagTest
 * @modules jdk.compiler
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

public class FlattenableFlagTest {

    __ByValue static final class Value {
        final int xx = 10;
    }

    Value v1;
    static Value v2;
    Value v3;
  
    public static void main(String[] args) {
        new OutputChecker().run();
    }

    static final class OutputChecker {

            void run() {
                String [] params = new String [] { "-v",
                                                    Paths.get(System.getProperty("test.classes"),
                                                        "FlattenableFlagTest.class").toString() };
                runCheck(params, new String [] {
                 "__Flattenable FlattenableFlagTest$Value v1;",
                 "flags: (0x0100) ACC_FLATTENABLE",
                 "FlattenableFlagTest$Value v2;",
                 "flags: (0x0008)",
                 "__Flattenable FlattenableFlagTest$Value v3;",
                 });

             }

             void runCheck(String [] params, String [] expectedOut) {
                StringWriter s;
                String out;

                try (PrintWriter pw = new PrintWriter(s = new StringWriter())) {
                    com.sun.tools.javap.Main.run(params, pw);
                    out = s.toString();
                }
                System.out.println("Out = " + out);
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
}
