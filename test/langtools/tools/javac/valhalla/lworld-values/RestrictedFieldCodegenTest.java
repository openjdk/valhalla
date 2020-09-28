/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @bug 8253312
 * @summary Enable JVM experiments in specialization under an opt-in mode
 * @modules jdk.compiler/com.sun.tools.javac.util jdk.jdeps/com.sun.tools.javap
 * @compile -XDflattenWithTypeRestrictions RestrictedFieldCodegenTest.java
 * @run main/othervm -Xverify:none RestrictedFieldCodegenTest
 * @modules jdk.compiler
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

class PointBox {

    static inline class Point {
        public double x;
        public double y;
        public Point(double x, double y) { this.x = x; this.y = y; }
    }

    public Point p;

    public static void main(String... args) {
        PointBox b = new PointBox();
        if (b.p != new Point(0,0)) throw new RuntimeException();
        b.p = new Point(1.0, 2.0);
        if (b.p != new Point(1.0, 2.0)) throw new RuntimeException();
    }
}

public class RestrictedFieldCodegenTest {

    public static void main(String [] args) {
        new RestrictedFieldCodegenTest().run();
    }

    void run() {
        String [] params = new String [] { "-v",
                                            Paths.get(System.getProperty("test.classes"),
                                                "PointBox.class").toString() };
        runCheck(params, new String [] {

         "public PointBox$Point$ref p;",
         "descriptor: LPointBox$Point$ref;",
         "RestrictedField: #25                    // QPointBox$Point;",
         " 9: getfield      #10                 // Field p:LPointBox$Point$ref;",
         "36: putfield      #10                 // Field p:LPointBox$Point$ref;",
         "40: getfield      #10                 // Field p:LPointBox$Point$ref;",
         });

     }

     void runCheck(String [] params, String [] expectedOut) {
        StringWriter s;
        String out;

        System.out.println("Checking javap");
        try (PrintWriter pw = new PrintWriter(s = new StringWriter())) {
            com.sun.tools.javap.Main.run(params, pw);
            out = s.toString();
        }
        System.out.println("Javap = " + out);
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
