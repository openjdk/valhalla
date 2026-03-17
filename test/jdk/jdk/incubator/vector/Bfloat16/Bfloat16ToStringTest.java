/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8375224
 * @enablePreview
 * @modules jdk.incubator.vector
 * @build Bfloat16
 * @run main Bfloat16ToStringTest
 * @summary Tests Bfloat16.toString()
 */

public class Bfloat16ToStringTest {
    public static void main(String... args) {
        checkSpecialValues();
        checkRepresentativeValues();
        checkRoundTrips();
    }
    

    private static void checkSpecialValues() {
        checkToString((short) 0x0000, "0.0", "+0.0");
        checkToString((short) 0x8000, "-0.0", "-0.0");
        checkToString((short) 0x7f80, "Infinity", "+infinity");
        checkToString((short) 0xff80, "-Infinity", "-infinity");

        for (int sign : new int[] {0x0000, 0x8000}) {
            for (int payload = 1; payload <= 0x007f; payload++) {
                checkToString((short) (sign | 0x7f80 | payload), "NaN",
                              (sign == 0 ? "positive" : "negative") + " NaN payload " + payload);
            }
        }
    }

    private static void checkRepresentativeValues() {
        checkToString((short) 0x8001, "-9.2E-41", "negative Bfloat16.MIN_VALUE");
        checkToString((short) 0x0001, "9.2E-41", "Bfloat16.MIN_VALUE");
        checkToString((short) 0x0002, "1.8E-40", "second subnormal");
        checkToString((short) 0x007f, "1.17E-38", "largest subnormal");
        checkToString((short) 0x0080, "1.18E-38", "Bfloat16.MIN_NORMAL");
        checkToString((short) 0x0081, "1.185E-38", "smallest normal above Bfloat16.MIN_NORMAL");

        checkToString((short) 0x3a82, "9.9E-4", "largest value below plain notation lower bound");
        checkToString((short) 0x3a83, "0.001", "plain notation lower bound");
        checkToString((short) 0x3a84, "0.00101", "next value above plain notation lower bound");
        checkToString((short) 0x3c23, "0.00995", "representative finite value");
        checkToString((short) 0x3dcd, "0.1", "representative finite tenth");
        checkToString((short) 0x3f80, "1.0", "1.0");
        checkToString((short) 0xbf80, "-1.0", "-1.0");
        checkToString((short) 0x4120, "10.0", "10.0");
        checkToString((short) 0x42c7, "99.5", "largest value below plain notation upper bound");
        checkToString((short) 0x42c8, "1.0E2", "plain notation upper bound");
        checkToString((short) 0x42c9, "1.005E2", "next value above plain notation upper bound");
        checkToString((short) 0x42f6, "1.23E2", "representative scientific notation");
        checkToString((short) 0x4300, "1.28E2", "128.0");
        checkToString((short) 0x447a, "1.0E3", "1.0E3");
        checkToString((short) 0x4700, "3.28E4", "3.28E4");
        checkToString((short) 0x7f7e, "3.38E38", "next-to-maximum finite value");
        checkToString((short) 0xff7e, "-3.38E38", "negative next-to-maximum finite value");
        checkToString((short) 0x7f7f, "3.39E38", "Bfloat16.MAX_VALUE");
        checkToString((short) 0xff7f, "-3.39E38", "negative Bfloat16.MAX_VALUE");
    }

    private static void checkRoundTrips() {
        for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
            short bits = (short) i;
            Bfloat16 value = Bfloat16.shortBitsToBfloat16(bits);
            String rendering = Bfloat16.toString(value);

            if (!rendering.equals(value.toString())) {
                throwRE(String.format("Static and instance toString differ for %s;%n" +
                                      "static %s, instance %s",
                                      hex(bits), rendering, value));
            }

            if (Bfloat16.isNaN(value)) {
                if (!"NaN".equals(rendering)) {
                    throwRE(String.format("Unexpected NaN rendering for %s;%n" +
                                          "expected NaN, got %s",
                                          hex(bits), rendering));
                }
                continue;
            }

            short roundTripBits =
                    Bfloat16.bfloat16ToRawShortBits(Bfloat16.valueOf(rendering));
            if (roundTripBits != bits) {
                throwRE(String.format("Round-trip mismatch for %s;%n" +
                                      "rendered %s, parsed %s",
                                      hex(bits), rendering, hex(roundTripBits)));
            }
        }
    }

    private static void checkToString(short bits, String expected, String message) {
        Bfloat16 value = Bfloat16.shortBitsToBfloat16(bits);
        String actual = Bfloat16.toString(value);

        if (!expected.equals(actual)) {
            throwRE(String.format("Unexpected rendering for %s (%s);%n" +
                                  "expected %s, actual %s",
                                  message, hex(bits), expected, actual));
        }

        if (!actual.equals(value.toString())) {
            throwRE(String.format("Static and instance toString differ for %s (%s);%n" +
                                  "static %s, instance %s",
                                  message, hex(bits), actual, value));
        }
    }

    private static String hex(short bits) {
        return String.format("0x%04X", bits & 0xffff);
    }

    private static void throwRE(String message) {
        throw new RuntimeException(message);
    }
}
