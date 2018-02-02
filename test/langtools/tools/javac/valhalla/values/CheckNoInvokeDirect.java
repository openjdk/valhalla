/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Check that javac emits invokedirect instruction for method calls on values
 * @modules jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.javap
 * @compile -XDenableValueTypes -g CheckNoInvokeDirect.java
 * @run main/othervm -Xverify:none -XX:+EnableValhalla CheckNoInvokeDirect
 */

import com.sun.tools.javac.util.Assert;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

public class CheckNoInvokeDirect {

    interface I {
        default void foo() {}
        default void goo() {}
    }

    static final __ByValue class Value implements I {

        static void soo() {}
        public void foo() {}
        void boo() {}

        void test() {
            soo();  // static method.
            foo();  // invokedirect, overridden.
            goo();  // invokedirect inherited.
            boo();  // invokedirect fresh instance method
        }
    }

    public static void main(String[] args) {
        new CheckNoInvokeDirect().run();
    }

    void run() {
        String [] params = new String [] { "-v",
                                            Paths.get(System.getProperty("test.classes"),
                                                "CheckNoInvokeDirect$Value.class").toString() };
        runCheck(params, new String [] {

           "#1 = Methodref          #7.#24         // \";Qjava/lang/__Value;\".\"<init>\":()V",
           "#2 = Methodref          #6.#25         // \";QCheckNoInvokeDirect$Value;\".soo:()V",
           "#3 = Methodref          #6.#26         // \";QCheckNoInvokeDirect$Value;\".foo:()V",
           "#4 = Methodref          #6.#27         // \";QCheckNoInvokeDirect$Value;\".goo:()V",
           "#5 = Methodref          #6.#28         // \";QCheckNoInvokeDirect$Value;\".boo:()V",

           "0: vload         0",
           "2: invokespecial #1                  // Method \";Qjava/lang/__Value;\".\"<init>\":()V",
           "5: return",


           "0: invokestatic  #2                  // Method soo:()V",
           "3: vload         0",
           "5: invokevirtual #3                  // Method foo:()V",
           "8: vload         0",
          "10: invokevirtual #4                  // Method goo:()V",
          "13: vload         0",
          "15: invokevirtual #5                  // Method boo:()V",
          "18: return"
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
