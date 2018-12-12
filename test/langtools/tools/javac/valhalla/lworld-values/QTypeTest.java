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
 * @compile QTypedValue.java
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
              "final value class QTypedValue",
              "  flags: (0x0130) ACC_FINAL, ACC_SUPER, ACC_VALUE",
              "  this_class: #7                          // QTypedValue",
              "   #7 = Class              #46            // QTypedValue",
              "   #8 = Class              #47            // \"QQTypedValue;\"",
              "   #9 = Fieldref           #7.#48         // QTypedValue.f1:[QQTypedValue;",
              "  #10 = Fieldref           #7.#49         // QTypedValue.f2:[QQTypedValue;",
              "  #11 = Class              #19            // \"[[[QQTypedValue;\"",
              "  #12 = Fieldref           #7.#50         // QTypedValue.f3:[[[QQTypedValue;",
              "  #13 = Fieldref           #7.#51         // QTypedValue.f4:[[[QQTypedValue;",
              "  #16 = Utf8               [QQTypedValue;",
              "  #19 = Utf8               [[[QQTypedValue;",
              "  #26 = Utf8               (QQTypedValue;I)V",
              "  #39 = NameAndType        #34:#35        // $makeValue$:()QQTypedValue;",
              "  #40 = NameAndType        #25:#26        // foo:(QQTypedValue;I)V",
              "  #46 = Utf8               QTypedValue",
              "  #47 = Utf8               QQTypedValue;",
              "  #48 = NameAndType        #15:#16        // f1:[QQTypedValue;",
              "  #49 = NameAndType        #17:#16        // f2:[QQTypedValue;",
              "  #50 = NameAndType        #18:#19        // f3:[[[QQTypedValue;",
              "  #51 = NameAndType        #20:#19        // f4:[[[QQTypedValue;",
              " final QTypedValue[] f1;",
              "    descriptor: [QQTypedValue;",
              "    flags: (0x0010) ACC_FINAL",
              "  final QTypedValue[] f2;",
              "    descriptor: [QQTypedValue;",
              "    flags: (0x0010) ACC_FINAL",
              "  final QTypedValue[][][] f3;",
              "    descriptor: [[[QQTypedValue;",
              "    flags: (0x0010) ACC_FINAL",
              "  final QTypedValue[][][] f4;",
              "    descriptor: [[[QQTypedValue;",
              "    flags: (0x0010) ACC_FINAL",
              "  void foo(QTypedValue, int);",
              "    descriptor: (QQTypedValue;I)V",
              "    flags: (0x0000)",
              "    Code:",
              "      stack=3, locals=12, args_size=3",
              "         0: aload_0",
              "         1: invokestatic  #2                  // Method $makeValue$:()QQTypedValue;",
              "         4: bipush        10",
              "         6: invokevirtual #3                  // Method foo:(QQTypedValue;I)V",
              "         9: iload_2",
              "        10: ifne          34",
              "        13: iconst_0",
              "        14: istore        8",
              "        16: dconst_0",
              "        17: dstore        9",
              "        19: invokestatic  #2                  // Method $makeValue$:()QQTypedValue;",
              "        22: astore_3",
              "        23: iload         8",
              "        25: ifne          29",
              "        28: return",
              "        29: invokestatic  #2                  // Method $makeValue$:()QQTypedValue;",
              "        32: astore        11",
              "        34: return",
              "      StackMapTable: number_of_entries = 2",
              "        frame_type = 255 /* full_frame */",
              "          offset_delta = 29",
              "          locals = [ class \"QQTypedValue;\", class \"QQTypedValue;\", int, class \"QQTypedValue;\", top, top, top, top, int, double ]",
              "          stack = []",
              "        frame_type = 255 /* full_frame */",
              "          offset_delta = 4",
              "          locals = [ class \"QQTypedValue;\", class \"QQTypedValue;\", int ]",
              "          stack = []",
              "static QTypedValue $makeValue$();",
              "    descriptor: ()QQTypedValue;",
              "    flags: (0x1008) ACC_STATIC, ACC_SYNTHETIC",
              "    Code:",
              "      stack=2, locals=1, args_size=0",
              "         0: defaultvalue  #7                  // class QTypedValue",
              "         3: astore_0",
              "         4: bipush        10",
              "         6: anewarray     #8                  // class \"QQTypedValue;\"",
              "         9: aload_0",
              "        10: swap",
              "        11: withfield     #9                  // Field f1:[QQTypedValue;",
              "        14: astore_0",
              "        15: bipush        10",
              "        17: anewarray     #8                  // class \"QQTypedValue;\"",
              "        20: aload_0",
              "        21: swap",
              "        22: withfield     #10                 // Field f2:[QQTypedValue;",
              "        25: astore_0",
              "        26: bipush        10",
              "        28: bipush        10",
              "        30: multianewarray #11,  2            // class \"[[[QQTypedValue;\"",
              "        34: aload_0",
              "        35: swap",
              "        36: withfield     #12                 // Field f3:[[[QQTypedValue;",
              "        39: astore_0",
              "        40: bipush        10",
              "        42: bipush        10",
              "        44: multianewarray #11,  2            // class \"[[[QQTypedValue;\"",
              "        48: aload_0",
              "        49: swap",
              "        50: withfield     #13                 // Field f4:[[[QQTypedValue;",
              "        53: astore_0",
              "        54: aload_0",
              "        55: areturn",
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
