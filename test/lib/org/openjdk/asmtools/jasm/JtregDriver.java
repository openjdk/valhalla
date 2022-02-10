/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jasm;

import java.nio.file.Paths;
import java.io.PrintWriter;

public class JtregDriver {
    
    public static void main(String... args) {
        String srcDir = System.getProperty("test.src", ".");
        String clsDir = System.getProperty("test.classes", ".");
        String[] jasmArgs = new String[args.length+2];
        jasmArgs[0] = "-d";
        jasmArgs[1] = clsDir;
        boolean isOptionArg = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (isOptionArg) {
                isOptionArg = false; // reset for next
            } else {
                if (arg.equals("-d")) {
                    isOptionArg = true;
                } else if (!arg.startsWith("-")) {
                    arg = Paths.get(srcDir, arg).toString();
                }
            }
            jasmArgs[i+2] = arg;
        }
        
        Main compiler = new Main(new PrintWriter(System.out), "jasm");
        boolean success = compiler.compile(jasmArgs);
        if (!success) {
            throw new RuntimeException("jasm execution failed");
        }
    }
    
}
