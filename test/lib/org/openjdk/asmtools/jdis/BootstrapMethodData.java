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

import org.openjdk.asmtools.jasm.JasmTokens;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 *
 */
public class BootstrapMethodData extends Indenter {

    int bsm_index;
    ArrayList<Integer> bsm_args_indexes;

    // internal references
    private Options options = Options.OptionObject();
    private ClassData cls;
    private PrintWriter out;

    public BootstrapMethodData(ClassData cls) {
        this.cls = cls;
        out = cls.out;
    }


    /*========================================================*/
    /* Read Methods */

    /**
     *
     * read
     *
     * read and resolve the bootstrap method data called from ClassData. precondition:
     * NumFields has already been read from the stream.
     *
     */
    public void read(DataInputStream in) throws IOException {
        // read the Methods CP indexes
        bsm_index = in.readUnsignedShort();
        int arg_num = in.readUnsignedShort();
        bsm_args_indexes = new ArrayList<>(arg_num);
        for (int i = 0; i < arg_num; i++) {
            bsm_args_indexes.add(in.readUnsignedShort());
        }
    }


    /*========================================================*/
    /* Print Methods */
    public void print() throws IOException {
        out.print(getIndentString() + JasmTokens.Token.BOOTSTRAPMETHOD.parseKey() + " #" + bsm_index);
        for (int i = 0; i < bsm_args_indexes.size(); i++) {
            out.print(" #" + bsm_args_indexes.get(i));
        }
        out.println(";");
    }
}
