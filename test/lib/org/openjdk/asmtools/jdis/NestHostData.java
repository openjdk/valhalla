/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * The NestHost attribute data
 * <p>
 * since class file 55.0 (JEP 181)
 */
public class NestHostData extends Indenter{
    ClassData cls;
    int host_class_index;
    private Options options = Options.OptionObject();

    public NestHostData(ClassData cls) {
        this.cls = cls;
    }

    public NestHostData read(DataInputStream in, int attribute_length) throws IOException, ClassFormatError {
        if (attribute_length != 2) {
            throw new ClassFormatError("ATT_NestHost: Invalid attribute length");
        }
        host_class_index = in.readUnsignedShort();
        return this;
    }

    public void print() {
        boolean pr_cpx = options.contains(Options.PR.CPX);
        cls.out.print(getIndentString() + JasmTokens.Token.NESTHOST.parseKey() + " ");
        if (pr_cpx) {
            cls.out.print("#" + host_class_index + "; //");
        }
        if (pr_cpx) {
            cls.pool.PrintConstant(cls.out, host_class_index);
            cls.out.println();
        } else {
            cls.out.println(cls.pool.StringValue(host_class_index) + ";");
        }
    }
}
