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

import java.util.EnumSet;

/**
 * The singleton class to share global options among jdis classes.
 */
public class Options {

    public static final int BODY_INDENT = 2;

    /* Options Fields */
    private static Options ref;

     public enum PR {

        CP, // print Constant Pool
        LNT, // print Line Number table
        PC, // print Program Counter - for all instr
        LABS, // print Labels (as identifiers)
        CPX, // print CP index along with arguments
        SRC, // print Source Line as comment
        HEX, // print numbers as hexadecimals
        VAR, // print local variables declarations
        DEBUG;  // Debug flag
    };

    static private final EnumSet<PR> JASM = EnumSet.<PR>of(PR.LABS); // default options
    static private final EnumSet<PR> CODE = EnumSet.<PR>of(
            PR.CP,
            PR.LNT,
            PR.PC,
            PR.CPX,
            PR.VAR
    );

    static private EnumSet<PR> printOptions = JASM;
    /*-------------------------------------------------------- */

    private Options() {
    }

    public static Options OptionObject() {
        if (ref == null) {
            ref = new Options();
        }
        return ref;
    }

    public void set(PR val) {
        printOptions.add(val);
    }

    public void setCodeOptions() {
        printOptions.addAll(CODE);
    }

    public boolean contains(PR val) {
        return printOptions.contains(val);
    }

    public boolean debug() {
        return printOptions.contains(PR.DEBUG);
    }

    @Override
    public String toString() {
        return printOptions.toString();
    }

}
