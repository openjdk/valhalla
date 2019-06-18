/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8214421 8221545 8222792
 * @summary Q<->L mixing should be OK for upcasts and should use checkcasts for downcasts.
 * @modules jdk.compiler/com.sun.tools.javac.util jdk.jdeps/com.sun.tools.javap
 * @compile BoxValCastTest.java
 * @run main/othervm -Xverify:none BoxValCastTest
 * @modules jdk.compiler
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

public class BoxValCastTest {

    static inline class VT {
        int f = 0;
        static final VT? vtbox = (VT?) new VT(); // no binary cast
        static VT vt = (VT) vtbox; // binary cast
        static VT? box = vt; // no binary cast
        static VT? box2 = (VT) box; // no binary cast
        static VT? box3 = id(new VT()); // no binary cast + binary cast

        static VT id(VT? vtb) {
            return (VT) vtb; // binary
        }
    }

    public static void main(String[] args) {
        new BoxValCastTest().run();
    }

    void run() {
        String [] params = new String [] { "-v",
                                            Paths.get(System.getProperty("test.classes"),
                                                "BoxValCastTest$VT.class").toString() };
        runCheck(params, new String [] {

        "checkcast     #7                  // class \"QBoxValCastTest$VT;\""
           
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
        String [] splits = out.split("checkcast     #7", -1);
        if (splits.length != 4) {
             throw new AssertionError("Unexpected javap output: " + splits.length);
        }
        splits = out.split("checkcast", -1);
        if (splits.length != 4) {
             throw new AssertionError("Unexpected javap output: " + splits.length);
        }
    }
}
