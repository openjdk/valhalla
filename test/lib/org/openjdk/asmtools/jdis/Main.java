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

import org.openjdk.asmtools.common.Tool;
import org.openjdk.asmtools.util.I18NResourceBundle;
import org.openjdk.asmtools.util.ProductInfo;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Main program of the Java Disassembler :: class to jasm
 */
public class Main extends Tool {

    private Options options;

    public static final I18NResourceBundle i18n
            = I18NResourceBundle.getBundleForClass(Main.class);

    public  Main(PrintWriter out, PrintWriter err, String programName) {
        super(out, err, programName);
        // tool specific initialization
        options = Options.OptionObject();
        DebugFlag = () -> options.contains(Options.PR.DEBUG);
        printCannotReadMsg = (fname) -> error( i18n.getString("jdis.error.cannot_read", fname));
    }

    public Main(PrintStream out, String program) {
        this(new PrintWriter(out), new PrintWriter(System.err), program);
    }

    @Override
    public void usage() {
        println(i18n.getString("jdis.usage"));
        println(i18n.getString("jdis.opt.g"));
        println(i18n.getString("jdis.opt.sl"));
        println(i18n.getString("jdis.opt.hx"));
        println(i18n.getString("jdis.opt.v"));
        println(i18n.getString("jdis.opt.version"));
    }

    /**
     * Run the disassembler
     */
    public synchronized boolean  disasm(String argv[]) {
        ArrayList<String> files = new ArrayList<>();

        // Parse arguments
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            switch (arg) {
                case "-g":
                    options.setCodeOptions();
                    break;
                case "-v":
                    options.set(Options.PR.DEBUG);
                    break;
                case "-sl":
                    options.set(Options.PR.SRC);
                    break;
                case "-hx":
                    options.set(Options.PR.HEX);
                    break;
                case "-version":
                    out.println(ProductInfo.FULL_VERSION);
                    break;
                default:
                    if (arg.startsWith("-")) {
                        error(i18n.getString("jdis.error.invalid_option", arg));
                        usage();
                        return false;
                    } else {
                        files.add(arg);
                    }
                    break;
            }
        }

        if (files.isEmpty()) {
            usage();
            return false;
        }

        for (String fname : files) {
            if (fname == null) {
                continue;
            } // cross out by CompilerChoice.compile
            try {
                ClassData cc = new ClassData(out, this);
                cc.read(fname);
                cc.print();
                out.flush();
                continue;
            } catch (Error ee) {
                if (DebugFlag.getAsBoolean())
                    ee.printStackTrace();
                error(i18n.getString("jdis.error.fatal_error", fname));
            } catch (Exception ee) {
                if (DebugFlag.getAsBoolean())
                    ee.printStackTrace();
                error(i18n.getString("jdis.error.fatal_exception", fname));
            }
            return false;
        }
        return true;
    }

    /**
     * Main program
     */
    public static void main(String argv[]) {
        Main disassembler = new Main(new PrintWriter(new uEscWriter(System.out)), new PrintWriter(System.err), "jdis");
        boolean result = disassembler.disasm(argv);
        System.exit(result ? 0 : 1);
    }
}
