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
package org.openjdk.asmtools.jcoder;

import org.openjdk.asmtools.common.Tool;
import org.openjdk.asmtools.util.I18NResourceBundle;
import org.openjdk.asmtools.util.ProductInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 *
 */
public class Main extends Tool {

    public static final I18NResourceBundle i18n
            = I18NResourceBundle.getBundleForClass(Main.class);

    public Main(PrintWriter out, String programName) {
        super(out, programName);
        printCannotReadMsg = (fname) -> error(i18n.getString("jcoder.error.cannot_read", fname));
    }

    public Main(PrintStream out, String program) {
        this(new PrintWriter(out), program);
    }

    @Override
    public void usage() {
        println(i18n.getString("jcoder.usage"));
        println(i18n.getString("jcoder.opt.nowrite"));
        println(i18n.getString("jcoder.opt.ignore"));
        println(i18n.getString("jcoder.opt.d"));
        println(i18n.getString("jcoder.opt.version"));
    }

    /**
     * Run the compiler
     */
    public synchronized boolean compile(String argv[]) {
        File destDir = null;
        boolean traceFlag = false;
        DebugFlag = () -> false;
        long tm = System.currentTimeMillis();
        ArrayList<String> v = new ArrayList<>();
        boolean nowrite = false;
        boolean ignore  = false;
        int nwarnings = 0;
        HashMap<String, String> macros = new HashMap<>();
        macros.put("VERSION", "3;45");

        // Parse arguments
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            if (!arg.startsWith("-")) {
                v.add(arg);
            } else if (arg.startsWith("-D")) {
                int argLength = arg.length();
                if (argLength == 2) {
                    error(i18n.getString("jcoder.error.D_needs_macro"));
                    return false;
                }
                int index = arg.indexOf('=');
                if (index == -1) {
                    error(i18n.getString("jcoder.error.D_needs_macro"));
                    return false;
                }
                String macroId = arg.substring(2, index);
                index++;
                if (argLength == index) {
                    error(i18n.getString("jcoder.error.D_needs_macro"));
                    return false;
                }
                String macro;
                if (arg.charAt(index) == '"') {
                    index++;
                    if (argLength == index || arg.charAt(argLength - 1) != '"') {
                        error(i18n.getString("jcoder.error.no_closing_quota"));
                        return false;
                    }
                    macro = arg.substring(index, argLength - 1);
                } else {
                    macro = arg.substring(index, argLength);
                }
                macros.put(macroId, macro);
            } else if (arg.equals("-vv")) {
                DebugFlag = () -> true;
                traceFlag = true;
            } else if (arg.equals("-v")) {
                traceFlag = true;
            } else if (arg.equals("-nowrite")) {
                nowrite = true;
            } else if (arg.equals("-ignore")) {
                ignore = true;
            } else if (arg.equals("-d")) {
                if ((i + 1) == argv.length) {
                    error(i18n.getString("jcoder.error.d_requires_argument"));
                    usage();
                    return false;
                }
                destDir = new File(argv[++i]);
                if (!destDir.exists()) {
                    error(i18n.getString("jcoder.error.does_not_exist", destDir));
                    return false;
                }
            } else if (arg.equals("-version")) {
                println(ProductInfo.FULL_VERSION);
            } else {
                error(i18n.getString("jcoder.error.invalid_option", arg));
                usage();
                return false;
            }
        }
        if (v.isEmpty()) {
            usage();
            return false;
        }
        // compile all input files
        try {
            for (String inpname : v) {
                SourceFile env;
                Jcoder p;

                DataInputStream dataInputStream = getDataInputStream(inpname);
                if( dataInputStream == null ) {
                    nerrors++;
                    continue;
                }
                env = new SourceFile(this, dataInputStream, inpname, out);
                env.traceFlag = traceFlag;
                env.debugInfoFlag = DebugFlag.getAsBoolean();
                p = new Jcoder(env, macros);
                p.parseFile();
                env.traceln("END PARSER");
                env.closeInp();

                nerrors += env.nerrors;
                nwarnings += env.nwarnings;
                if (nowrite || (nerrors > 0 & !ignore)) {
                    continue;
                }
                try {
                    env.traceln("WRITE");
                    p.write(destDir);
                } catch (FileNotFoundException ex) {
                    error(i18n.getString("jcoder.error.cannot_write", ex.getMessage()));
                }
            }
        } catch (Error ee) {
            ee.printStackTrace();
            error(i18n.getString("jcoder.error.fatal_error"));
        } catch (Exception ee) {
            ee.printStackTrace();
            error(i18n.getString("jcoder.error.fatal_exception"));
        }

        boolean errs = nerrors > 0;
        boolean warns = nwarnings > 0;
        if (!errs && !warns) {
            return true;
        }
        println(errs ? (nerrors > 1 ? (nerrors + " errors") : "1 error")
                : "" + ((errs && warns) ? ", " : "") + (warns ? (nwarnings > 1 ? (nwarnings + " warnings") : "1 warning") : ""));
        return !errs;
    }

    /**
     * main program
     */
    public static void main(String[] argv) {
        Main compiler = new Main(new PrintWriter(System.out), "jcoder");
        System.exit(compiler.compile(argv) ? 0 : 1);
    }
}
