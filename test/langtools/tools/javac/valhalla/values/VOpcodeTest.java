/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Check that javac emits vopcodes.
 * @modules jdk.compiler jdk.jdeps/com.sun.tools.javap
 * @compile -XDenableValueTypes VOpcodeTest.java
 * @run main/othervm -Xverify:none -XX:+EnableValhalla VOpcodeTest
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

public class VOpcodeTest {

    public static __ByValue final class InnerValue {
    }

    public static __ByValue final class Value {

        final InnerValue iv = __MakeDefault InnerValue();

        Value foo() {
            return foo();
        }

        Value goo(Value [] va) {
            Value v = foo();
            foo();
            foo().iv.hashCode();
            va[0] = va[1];
            return foo();
        }
    }

    public static void main(String[] args) {
        new VOpcodeTest().run();
    }

    void run() {
        String [] params =
                new String [] { "-v",
                                Paths.get(System.getProperty("test.classes"),
                               "VOpcodeTest$Value.class").toString() };

        runCheck(params, new String [] {
               // goo's code:
               "0: vload         0",
               "2: invokevirtual #4                  // Method foo:()QVOpcodeTest$Value;",
               "5: vstore        2",   // Store directly into v without buffering

               "7: vload         0",
               "9: invokevirtual #4                  // Method foo:()QVOpcodeTest$Value;",
              "12: pop",               // Don't buffer what is being discarded.

              "13: vload         0",
              "15: invokevirtual #4                  // Method foo:()QVOpcodeTest$Value;",
              "18: getfield      #3                  // Field iv:QVOpcodeTest$InnerValue;",
              "21: invokevirtual #5                  // Method \";QVOpcodeTest$InnerValue;\".hashCode:()I",
              "24: pop",
              "25: aload_1",
              "26: iconst_0",
              "27: aload_1",
              "28: iconst_1",
              "29: vaload",
              "30: vastore",         // Direct store without intermediate buffering.
              "31: vload         0",
              "33: invokevirtual #4                  // Method foo:()QVOpcodeTest$Value;",
              "36: vreturn",        // Return value without rebuffering.
                         });
     }

     void runCheck(String [] params, String [] expectedOut) {
        StringWriter s;
        String out;

        try (PrintWriter pw = new PrintWriter(s = new StringWriter())) {
            com.sun.tools.javap.Main.run(params, pw);
            out = s.toString();
        }
        for (String eo: expectedOut) {
            if (!out.contains(eo))
                throw new AssertionError("Unexpected output: " + eo + " \n in: " + out);
        }
    }
}
