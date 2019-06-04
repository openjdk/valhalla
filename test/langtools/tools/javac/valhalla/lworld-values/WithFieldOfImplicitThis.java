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
 * @compile -XDallowWithFieldOperator WithFieldOfImplicitThis.java
 * @run main/othervm -Xverify:none -XX:+EnableValhalla WithFieldOfImplicitThis
 * @modules jdk.compiler
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

public class WithFieldOfImplicitThis {

    final inline class X {

        final int x;

        X() {
            x = 10;
        }
        
        X getX(Integer xVal, int xi) {
            X xl = X.default;
            xl = __WithField(x, xi);
            xl = __WithField(x, xVal);
            return xl;
        }
    }

    public static void main(String[] args) {
        new WithFieldOfImplicitThis().run();
    }

    void run() {
        String [] params = new String [] { "-v",
                                            Paths.get(System.getProperty("test.classes"),
                                                "WithFieldOfImplicitThis$X.class").toString() };
        runCheck(params, new String [] {

         "0: defaultvalue  #1                  // class WithFieldOfImplicitThis$X",
         "3: astore_3",
         "4: aload_0",
         "5: iload_2",
         "6: withfield     #7                  // Field x:I",
         "9: astore_3",
        "10: aload_0",
        "11: aload_1",
        "12: invokevirtual #11                 // Method java/lang/Integer.intValue:()I",
        "15: withfield     #7                  // Field x:I",
        "18: astore_3",
        "19: aload_3",
        "20: areturn"
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
