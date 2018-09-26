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
 * @summary Check code generation for value creation ops
 * @modules jdk.compiler/com.sun.tools.javac.util jdk.jdeps/com.sun.tools.javap
 * @compile -XDallowWithFieldOperator ValueCreationTest.java
 * @run main/othervm -Xverify:none -XX:+EnableValhalla ValueCreationTest
 * @modules jdk.compiler
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

public class ValueCreationTest {

    value
    static final class Point {

        final int x;
        final int y;

        Point () {
            x = 10;
            y = 10;
        }

        static Point makePoint(int x, int y) {
           Point p = Point.default;
           p = __WithField(p.x, x);
           return __WithField(p.y, y);
        }

        public static void main(String [] args) {
            Point p = makePoint(10, 20);
        }
    }

    public static void main(String[] args) {
        new ValueCreationTest().run();
    }

    void run() {
        String [] params = new String [] { "-v",
                                            Paths.get(System.getProperty("test.classes"),
                                                "ValueCreationTest$Point.class").toString() };
        runCheck(params, new String [] {

         "0: defaultvalue  #2                  // class ValueCreationTest$Point",
         "3: astore_2",
         "4: iload_0",
         "5: aload_2",
         "6: swap",
         "7: withfield     #3                  // Field x:I",
        "10: astore_2",
        "11: iload_1",
        "12: aload_2",
        "13: swap",
        "14: withfield     #4                  // Field y:I",
        "17: areturn"
           
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
