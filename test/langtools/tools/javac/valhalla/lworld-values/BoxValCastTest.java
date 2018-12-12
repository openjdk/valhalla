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
 * @bug 8214421
 * @summary Q<->L mixing should be via checkcasts
 * @modules jdk.compiler/com.sun.tools.javac.util jdk.jdeps/com.sun.tools.javap
 * @compile -XDallowWithFieldOperator BoxValCastTest.java
 * @run main/othervm -Xverify:none -XX:+EnableValhalla BoxValCastTest
 * @modules jdk.compiler
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

public class BoxValCastTest {

    static value class VT {
        int f = 0;
        static final VT.box vtbox = new VT(); // cast
        static VT.val vtval = vtbox; // cast
        static VT vt = vtbox; // cast
        static VT.val vtval2 = vtval; // no cast
        static VT.box box = vtval; // cast
        static VT.box box2 = box; // no cast
        static VT.box box3 = id(new VT()); // cast + cast

        static VT id(VT.box vtb) {
            return vtb;
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

        "static {};", 
        "    descriptor: ()V",
        "   flags: (0x0008) ACC_STATIC",
        "   Code:",
        "     stack=1, locals=0, args_size=0",
        "        0: invokestatic  #6                  // Method $makeValue$:()QBoxValCastTest$VT;",
        "        3: checkcast     #7                  // class BoxValCastTest$VT",
        "        6: putstatic     #8                  // Field vtbox:LBoxValCastTest$VT;",
        "        9: getstatic     #8                  // Field vtbox:LBoxValCastTest$VT;",
        "       12: checkcast     #2                  // class \"QBoxValCastTest$VT;\"",
        "       15: putstatic     #9                  // Field vtval:QBoxValCastTest$VT;",
        "       18: getstatic     #8                  // Field vtbox:LBoxValCastTest$VT;",
        "       21: checkcast     #2                  // class \"QBoxValCastTest$VT;\"",
        "       24: putstatic     #10                 // Field vt:QBoxValCastTest$VT;",
        "       27: getstatic     #9                  // Field vtval:QBoxValCastTest$VT;",
        "       30: putstatic     #11                 // Field vtval2:QBoxValCastTest$VT;",
        "       33: getstatic     #9                  // Field vtval:QBoxValCastTest$VT;",
        "       36: checkcast     #7                  // class BoxValCastTest$VT",
        "       39: putstatic     #12                 // Field box:LBoxValCastTest$VT;",
        "       42: getstatic     #12                 // Field box:LBoxValCastTest$VT;",
        "       45: putstatic     #13                 // Field box2:LBoxValCastTest$VT;",
        "       48: invokestatic  #6                  // Method $makeValue$:()QBoxValCastTest$VT;",
        "       51: checkcast     #7                  // class BoxValCastTest$VT",
        "       54: invokestatic  #14                 // Method id:(LBoxValCastTest$VT;)QBoxValCastTest$VT;",
        "       57: checkcast     #7                  // class BoxValCastTest$VT",
        "       60: putstatic     #15                 // Field box3:LBoxValCastTest$VT;",
        "       63: return",
           
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
