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
 * @compile WithFieldOfExplicitSelector.java
 * @run main/othervm -Xverify:none -XX:+EnableValhalla WithFieldOfExplicitSelector
 * @modules jdk.compiler
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

public class WithFieldOfExplicitSelector {

    final __ByValue class X {

        final int i;

        X() {
            i = 10;
        }
        
        X getX(int i, Integer in) {
            X xl = __WithField(this.i, i);
            xl = __WithField(xl.i, in);
            return xl;
        }
    }

    public static void main(String[] args) {
        new WithFieldOfExplicitSelector().run();
    }

    void run() {
        String [] params = new String [] { "-v",
                                            Paths.get(System.getProperty("test.classes"),
                                                "WithFieldOfExplicitSelector$X.class").toString() };
        runCheck(params, new String [] {

         "0: aload_0",
         "1: iload_1",
         "2: withfield     #2                  // Field i:I",
         "5: astore_3",
         "6: aload_3",
         "7: aload_2",
        "8: invokevirtual #3                  // Method java/lang/Integer.intValue:()I",
        "11: withfield     #2                  // Field i:I",
        "14: astore_3",
        "15: aload_3",
        "16: areturn"
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
