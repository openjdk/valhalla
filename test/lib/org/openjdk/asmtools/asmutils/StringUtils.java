/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Utility class to share common tools/methods.
 */
public class StringUtils {
    /**
     * Converts CONSTANT_Utf8_info string to a printable string for jdis/jdes.
     * @param utf8 UTF8 string taken from within ConstantPool of a class file
     * @return output string for jcod/jasm
     */
    public static String Utf8ToString(String utf8) {
        StringBuilder sb = new StringBuilder("\"");
        for (int k = 0; k < utf8.length(); k++) {
            char c = utf8.charAt(k);
            switch (c) {
                case '\t':
                    sb.append('\\').append('t');
                    break;
                case '\n':
                    sb.append('\\').append('n');
                    break;
                case '\r':
                    sb.append('\\').append('r');
                    break;
                case '\b':
                    sb.append('\\').append('b');
                    break;
                case '\f':
                    sb.append('\\').append('f');
                    break;
                case '\"':
                    sb.append('\\').append('\"');
                    break;
                case '\'':
                    sb.append('\\').append('\'');
                    break;
                case '\\':
                    sb.append('\\').append('\\');
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.append('\"').toString();
    }
}
