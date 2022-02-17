/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.asmutils.HexUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class TraceUtils {

    public static String prefixString = "\t";

    public static void trace(String s) {
        if (!(Options.OptionObject()).debug()) {
            return;
        }
        System.out.print(s);
    }

    public static void trace(int prefixLength, String s) {
        if (!(Options.OptionObject()).debug()) {
            return;
        }
        System.out.print((prefixLength > 0) ? new String(new char[prefixLength]).replace("\0", prefixString) + s : s);
    }

    public static void traceln(String s) {
        if (!(Options.OptionObject()).debug()) {
            return;
        }
        System.out.println(s);
    }

    public static void traceln(String... lines) {
        if (!(Options.OptionObject()).debug()) {
            return;
        }

        if (lines.length == 0) {
            System.out.println();
        } else {
            for (String s : lines) {
                System.out.println(s);
            }
        }
    }

    public static void traceln(int prefixLength, String s) {
        if (!(Options.OptionObject()).debug()) {
            return;
        }
        System.out.println((prefixLength > 0) ? new String(new char[prefixLength]).replace("\0", prefixString) + s : s);
    }

    public static void traceln(int prefixLength, String... lines) {
        if (!(Options.OptionObject()).debug()) {
            return;
        }
        if (lines.length == 0) {
            System.out.println();
        } else {
            String prefix = (prefixLength > 0) ? new String(new char[prefixLength]).replace("\0", prefixString) : "";
            for (String s : lines) {
                System.out.println(prefix + s);
            }
        }
    }

    public static String mapToHexString(int[] array) {
        return format("%d %s",
                array.length,
                Arrays.stream(array).
                        mapToObj(val -> HexUtils.toHex(val)).
                        collect(Collectors.joining(" ")));
    }
}
