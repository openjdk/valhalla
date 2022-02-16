/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.asmutils;

/**
 *
 */
public class HexUtils {
    /*======================================================== Hex */

    private static final String hexString = "0123456789ABCDEF";
    private static final char hexTable[] = hexString.toCharArray();

    public static String toHex(long val, int width) {
        StringBuffer sb = new StringBuffer();
        for (int i = width - 1; i >= 0; i--) {
            sb.append(hexTable[((int) (val >> (4 * i))) & 0xF]);
        }
        String s = sb.toString();
        return "0x" + (s.isEmpty() ? "0" : s);
    }

    public static String toHex(long val) {
        int width;
        for (width = 16; width > 0; width--) {
            if ((val >> (width - 1) * 4) != 0) {
                break;
            }
        }
        return toHex(val, width);
    }

    public static String toHex(int val) {
        int width;
        for (width = 8; width > 0; width--) {
            if ((val >> (width - 1) * 4) != 0) {
                break;
            }
        }
        return toHex(val, width);
    }

}
