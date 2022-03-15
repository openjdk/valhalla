/*
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jdis;

/**
 *
 */
public class Utils {

    static public String javaName(String name) {
        if (name == null) {
            return "null";
        }
        int len = name.length();
        if (len == 0) {
            return "\"\"";
        }
        char cc = '/';
fullname:
        { // xxx/yyy/zzz
            for (int k = 0; k < len; k++) {
                char c = name.charAt(k);
                if (cc == '/') {
                    if (!Character.isJavaIdentifierStart(c) && c != '-') {
                        break fullname;
                    }
                } else if (c != '/') {
                    if (!Character.isJavaIdentifierPart(c) && c != '-') {
                        break fullname;
                    }
                }
                cc = c;
            }
            return name;
        }
        return "\"" + name + "\"";
    }

    static public boolean isClassArrayDescriptor(String name) {
        boolean retval = false;
        if (name != null) {
            if (name.startsWith("[")) {
                retval = true;
            }
        }

        return retval;
    }

    static public String commentString(String  str) {
        return commentString(str,"// ");
    }

    static public String commentString(String  str, String prefix) {
        return prefix + str.replace("\n", "\n" + prefix);
    }

}
