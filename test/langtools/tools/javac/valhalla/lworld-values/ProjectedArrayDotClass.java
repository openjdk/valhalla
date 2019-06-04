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
 * @bug 8222722
 * @summary  Javac fails to compile V?[].class
 * @modules jdk.compiler/com.sun.tools.javac.util jdk.jdeps/com.sun.tools.javap
 * @compile ProjectedArrayDotClass.java
 * @run main/othervm -Xverify:none -XX:+EnableValhalla ProjectedArrayDotClass
 * @modules jdk.compiler
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

public class ProjectedArrayDotClass {

    static inline class VT {
        int x = 42;
        public static void main(String[] args) {
            System.out.println(VT?[].class);
            System.out.println(VT[].class);
            System.out.println(ProjectedArrayDotClass.VT?[].class);
            System.out.println(ProjectedArrayDotClass.VT[].class);
        }
    }

    public static void main(String[] args) {
        new ProjectedArrayDotClass().run();
    }

    void run() {
        String [] params = new String [] { "-v",
                                            Paths.get(System.getProperty("test.classes"),
                                                "ProjectedArrayDotClass$VT.class").toString() };
        runCheck(params, new String [] {
        "         3: ldc           #13                 // class \"[LProjectedArrayDotClass$VT;\"",
        "        11: ldc           #21                 // class \"[QProjectedArrayDotClass$VT;\"",
        "        19: ldc           #13                 // class \"[LProjectedArrayDotClass$VT;\"",
        "        27: ldc           #21                 // class \"[QProjectedArrayDotClass$VT;\"",
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
