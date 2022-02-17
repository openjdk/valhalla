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
package org.openjdk.asmtools.jasm;

/**
 *
 */
public class ParseBase {

    protected boolean debugFlag;
    protected Scanner scanner;
    protected Parser parser;
    protected Environment env;

    public ParseBase() {
        init(null, null, null);
    }

    public void init(Scanner scnr, Parser prsr, Environment envr) {
        debugFlag = false;
        scanner = scnr;
        parser = prsr;
        env = envr;
    }

    public void enableDebug(boolean debState) {
        debugFlag = debState;
    }

    protected void debugStr(String str) {
        if (debugFlag) {
            env.traceln(str);
        }
    }

    protected void debugScan(String str) {
        if (debugFlag) {
            scanner.debugScan(str);
        }
    }

}
