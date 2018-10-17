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
 * bug 8212563
 * @summary Check that javac emits Q types for values as needed
 * @modules jdk.compiler/com.sun.tools.javac.util jdk.jdeps/com.sun.tools.javap
 * @compile -XDemitQtypes QTypedValue.java
 * @run main/othervm -Xverify:none -XX:+EnableValhalla QTypeTest
 * @modules jdk.compiler
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

public class QTypeTest {

    public static void main(String[] args) {
        new QTypeTest().run();
    }

    void run() {
        String [] params = new String [] { "-v",
                                            Paths.get(System.getProperty("test.classes"),
                                                "QTypedValue.class").toString() };
        runCheck(params, new String [] {
                "final value class QQTypedValue;",
                "  flags: (0x0130) ACC_FINAL, ACC_SUPER, ACC_VALUE",
                "  this_class: #2                          // \"QQTypedValue;\"",
                "   #2 = Class              #37            // \"QQTypedValue;\"",
                "   #3 = Class              #38            // \"[[[[QQTypedValue;\"",
                "   #5 = Fieldref           #2.#41         // \"QQTypedValue;\".x:[[[QQTypedValue;",
                "   #7 = Methodref          #2.#44         // \"QQTypedValue;\".$makeValue$:()QQTypedValue;",
                "   #8 = Methodref          #2.#45         // \"QQTypedValue;\".foo:(QQTypedValue;)V",
                "  #17 = Utf8               [[[QQTypedValue;",
                "  #23 = Utf8               (QQTypedValue;)V",
                "  #33 = Utf8               ()QQTypedValue;",
                "  #35 = Utf8               QTypedValue.java",
                "  #37 = Utf8               QQTypedValue;",
                "  #38 = Utf8               [[[[QQTypedValue;",
                "  #41 = NameAndType        #14:#17        // x:[[[QQTypedValue;",
                "  #44 = NameAndType        #32:#33        // $makeValue$:()QQTypedValue;",
                "  #45 = NameAndType        #22:#23        // foo:(QQTypedValue;)V",
                "{",
                "  final QTypedValue[][][] x;",
                "    descriptor: [[[QQTypedValue;",
                "    flags: (0x0010) ACC_FINAL",
                "",
                "  QQTypedValue;();",
                "    descriptor: ()V",
                "    flags: (0x0000)",
                "    Code:",
                "      stack=1, locals=1, args_size=1",
                "         0: aload_0",
                "         1: invokespecial #1                  // Method java/lang/Object.\"<init>\":()V",
                "         4: return",
                "      LineNumberTable:",
                "        line 24: 0",
                "  void foo(QTypedValue);",
                "    descriptor: (QQTypedValue;)V",
                "    flags: (0x0000)",
                "    Code:",
                "      stack=4, locals=4, args_size=2",
                "         0: bipush        10",
                "         2: anewarray     #2                  // class \"QQTypedValue;\"",
                "         5: astore_2",
                "         6: bipush        10",
                "         8: bipush        10",
                "        10: bipush        10",
                "        12: bipush        10",
                "        14: multianewarray #3,  4             // class \"[[[[QQTypedValue;\"",
                "        18: astore_2",
                "        19: getstatic     #4                  // Field java/lang/System.out:Ljava/io/PrintStream;",
                "        22: aload_0",
                "        23: getfield      #5                  // Field x:[[[QQTypedValue;",
                "        26: invokevirtual #6                  // Method java/io/PrintStream.println:(Ljava/lang/Object;)V",
                "        29: aload_0",
                "        30: invokestatic  #7                  // Method $makeValue$:()QQTypedValue;",
                "        33: invokevirtual #8                  // Method foo:(QQTypedValue;)V",
                "        36: aload_2",
                "        37: checkcast     #3                  // class \"[[[[QQTypedValue;\"",
                "        40: astore_3",
                "        41: return",
                "  static QTypedValue $makeValue$();",
                "    descriptor: ()QQTypedValue;",
                "    flags: (0x1008) ACC_STATIC, ACC_SYNTHETIC",
                "    Code:",
                "      stack=2, locals=1, args_size=0",
                "         0: defaultvalue  #2                  // class \"QQTypedValue;\"",
                "         3: astore_0",
                "         4: aconst_null",
                "         5: aload_0",
                "         6: swap",
                "         7: withfield     #5                  // Field x:[[[QQTypedValue;",
                "        10: astore_0",
                "        11: aload_0",
                "        12: areturn",
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
