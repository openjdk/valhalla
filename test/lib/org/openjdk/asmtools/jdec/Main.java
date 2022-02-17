/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jdec;

import org.openjdk.asmtools.common.Tool;
import org.openjdk.asmtools.jdis.uEscWriter;
import org.openjdk.asmtools.util.I18NResourceBundle;
import org.openjdk.asmtools.util.ProductInfo;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Main program of the Java DECoder :: class to jcod
 */
public class Main extends Tool {

    int     printFlags = 0;

    public static final I18NResourceBundle i18n
            = I18NResourceBundle.getBundleForClass(Main.class);

    public Main(PrintWriter out, PrintWriter err, String programName) {
        super(out, err, programName);
        printCannotReadMsg = (fname) ->
                error( i18n.getString("jdec.error.cannot_read", fname));
    }

    public Main(PrintStream out, String program) {
        this(new PrintWriter(out), new PrintWriter(System.err), program);
    }

    @Override
    public void usage() {
        println(i18n.getString("jdec.usage"));
        println(i18n.getString("jdec.opt.g"));
        println(i18n.getString("jdec.opt.version"));
    }

    /**
     * Run the decoder
     */
    public synchronized boolean decode(String argv[]) {
        long tm = System.currentTimeMillis();
        ArrayList<String> vargs = new ArrayList<>();
        ArrayList<String> vj = new ArrayList<>();
        boolean nowrite = false;
        int addOptions = 0;

        // Parse arguments
        int i = 0;
        for (String arg : argv) {
            //
            if (arg.equals("-g")) {
                printFlags = printFlags | 1;
                vargs.add(arg);
            } else if (arg.equals("-v")) {
                DebugFlag = () -> true;
                vargs.add(arg);
                out.println("arg[" + i + "]=" + argv[i] + "/verbose");
            } else if (arg.equals("-version")) {
                out.println(ProductInfo.FULL_VERSION);
            } else if (arg.startsWith("-")) {
                error(i18n.getString("jdec.error.invalid_flag", arg));
                usage();
                return false;
            } else {
                vargs.add(arg);
                vj.add(arg);
            }
            i += 1;
        }

        if (vj.isEmpty()) {
            usage();
            return false;
        }

        String[] names = new String[0];
        names = vj.toArray(names);
        for (String inpname : names) {
            try {
                DataInputStream dataInputStream = getDataInputStream(inpname);
                if( dataInputStream == null )
                    return false;
                ClassData cc = new ClassData(dataInputStream, printFlags, out);
                cc.DebugFlag = DebugFlag.getAsBoolean();
                cc.decodeClass(inpname);
                out.flush();
                continue;
            } catch (Error ee) {
                if (DebugFlag.getAsBoolean())
                    ee.printStackTrace();
                error(i18n.getString("jdec.error.fatal_error"));
            } catch (Exception ee) {
                if (DebugFlag.getAsBoolean())
                    ee.printStackTrace();
                error(i18n.getString("jdec.error.fatal_exception"));
            }
            return false;
        }
        return true;
    }

    /**
     * Main program
     */
    public static void main(String argv[]) {
        Main decoder = new Main(new PrintWriter(new uEscWriter(System.out)), new PrintWriter(System.err), "jdec");
        System.exit(decoder.decode(argv) ? 0 : 1);
    }
}
