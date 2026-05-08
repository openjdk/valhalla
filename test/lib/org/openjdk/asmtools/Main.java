/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools;

import org.openjdk.asmtools.util.I18NResourceBundle;
import org.openjdk.asmtools.util.ProductInfo;

/**
 * Wrapper class that reads the first command line argument and invokes a corresponding
 * tool.
 */
public class Main {

    public static final I18NResourceBundle i18n
            = I18NResourceBundle.getBundleForClass(Main.class);

    /**
     * Parses the first argument and deligates execution to an appropriate tool
     *
     * @param args - command line arguments
     */
    public static void main(String args[]) {
        if (args.length == 0) {
            usage(i18n.getString("main.error.no_arguments"), 1);
        }
        String cmd = args[0];
        if (cmd.equals("-?") || cmd.equals("-h") || cmd.equals("-help")) {
            usage(null, 0);
        } else if (cmd.equals("-version")) {
            printVersion();
        } else {
            String[] newArgs = new String[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, args.length - 1);
            if (cmd.equals("jasm")) {
                jasm(newArgs);
            } else if (cmd.equals("jdis")) {
                jdis(newArgs);
            } else if (cmd.equals("jcoder")) {
                jcoder(newArgs);
            } else if (cmd.equals("jdec")) {
                jdec(newArgs);
            } else if (cmd.equals("jcdec")) {
                jcdec(newArgs);
            } else {
                usage(i18n.getString("main.error.unknown_tool", cmd), 1);
            }
        }
    }

    /**
     * Prints usage info and error message, afterwards invokes System.exit()
     *
     * @param msg - error message to print, or null if no errors occurred
     * @param exitCode - exit code to be returned by System.exit()
     */
    public static void usage(String msg, int exitCode) {
        System.err.println(i18n.getString("main.usage", "asmtools.jar"));
        if (msg != null) {
            System.err.println(msg);
        }
        System.exit(exitCode);
    }

    /**
     * Prints the tools version
     */
    public static void printVersion() {
        System.out.println(ProductInfo.FULL_VERSION);
    }

    /**
     * Invokes jasm main class with passed arguments
     */
    public static void jasm(String[] args) {
        org.openjdk.asmtools.jasm.Main.main(args);
    }

    /**
     * Invokes jcdec main class with passed arguments
     */
    public static void jcdec(String[] args) {
        org.openjdk.asmtools.jcdec.Main.main(args);
    }

    /**
     * Invokes jcoder main class with passed arguments
     */
    public static void jcoder(String[] args) {
        org.openjdk.asmtools.jcoder.Main.main(args);
    }

    /**
     * Invokes jdec main class with passed arguments
     */
    public static void jdec(String[] args) {
        org.openjdk.asmtools.jdec.Main.main(args);
    }

    /**
     * Invokes jdis main class with passed arguments
     */
    public static void jdis(String[] args) {
        org.openjdk.asmtools.jdis.Main.main(args);
    }
}
