/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.common;


import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public abstract class Tool {

    // Name of the program.
    protected final String programName;
    // Errors counter.
    protected int nerrors = 0;
    // The stream where error message are printed.
    protected PrintWriter err;

    // Output stream
    protected PrintWriter out;

    // A consumer to print a error message if the tool can't read a file
    protected Consumer<String> printCannotReadMsg;

    // A supplier to get a status of a debug flag
    protected BooleanSupplier DebugFlag = () -> false;

    public Tool(PrintWriter out, String programName) {
        this(out, out, programName);
    }

    public Tool(PrintWriter out, PrintWriter err, String programName) {
        this.out = out;
        this.err = err;
        this.programName = programName;
    }


    public String getError(String msg) {
        return programName + ": " + msg;
    }

    /**
     * Top level error message
     */
    public void error(String msg) {
        err.println(getError(msg));
        err.flush();
    }

    /**
     * Top level print message
     */
    public void println(String msg) {
        out.println(msg);
        out.flush();
    }

    public void println() {
        println("");
    }

    public void print(String msg) {
        out.print(getError(msg));
        out.flush();
    }

    /**
     * @param fname file name
     * @return DataInputStream or null if the method can't read a file
     */
    public DataInputStream getDataInputStream(String fname) {
        try {
            return new DataInputStream(new FileInputStream(fname));
        } catch (IOException ex) {
            if (fname.matches("^[A-Za-z]+:.*")) {
                try {
                    final URI uri = new URI(fname);
                    final URL url = uri.toURL();
                    final URLConnection conn = url.openConnection();
                    conn.setUseCaches(false);
                    return new DataInputStream(conn.getInputStream());
                } catch (URISyntaxException | IOException e) {
                    if (DebugFlag.getAsBoolean())
                        e.printStackTrace();
                }
            }
            if (printCannotReadMsg != null)
                printCannotReadMsg.accept(fname);
        }
        return null;
    }

    /**
     * Usage
     */
    protected abstract void usage();
}
