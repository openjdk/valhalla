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
 * @summary Check emission of ValueTypes attribute
 * @modules jdk.compiler/com.sun.tools.javac.util jdk.jdeps/com.sun.tools.javap
 * @compile MultiValues.java
 * @compile -g ValueTypesAttributeTest.java
 * @run main/othervm -Xverify:none -XX:+EnableValhalla ValueTypesAttributeTest
 * @modules jdk.compiler
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

public class ValueTypesAttributeTest {

    static final value class X {
        final V1 [] v1 = null; // field descriptor 
        V2[] foo() {  // method descriptor encoding value type
            return null;
        }
        void foo(V3 v3) { // method descriptor encoding value type
        }
        void foo(int x) {
            V4 [] v4 = null; // local variable.
        }
        void goo() {
            V5 [] v5 = null;
            if (v5 == null) {
               V6 [] v61 = null;  // stack map table.
            } else {
               V5 [] v52 = null;
            }
        }
    }

    public static void main(String[] args) {
        new ValueTypesAttributeTest().run();
    }

    void run() {
        String [] params = new String [] { "-v",
                                            Paths.get(System.getProperty("test.classes"),
                                                "ValueTypesAttributeTest$X.class").toString() };
        runCheck(params, new String [] {

         "ValueTypes:",
         "#36;                                    // value class V4",
         "#24;                                    // value class V2",
         "#6;                                     // value class ValueTypesAttributeTest$X",
         "#10;                                    // value class V1",
         "#27;                                    // value class V3",
         "#41;                                    // value class V5",
           
         }, new String [] {
         });

     }

     void runCheck(String [] params, String [] expectedOut, String [] unexpectedOut) {
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
        for (String eo: unexpectedOut) {
            if (out.contains(eo)) {
                System.err.println("Unexpected output found for string: " + eo);
                errors++;
            }
        }
        if (errors > 0) {
             throw new AssertionError("Unexpected javap output: " + out);
        }
    }
}
