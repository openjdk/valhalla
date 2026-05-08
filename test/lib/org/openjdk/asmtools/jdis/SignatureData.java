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


import org.openjdk.asmtools.jasm.Tables;

import java.io.DataInputStream;
import java.io.IOException;

import static java.lang.String.format;

/**
 * The Signature attribute data
 * <p>
 * since class file 49.0
 */
public class SignatureData {
    ClassData cls;
    int signature_index;
    private Options options = Options.OptionObject();

    public SignatureData(ClassData cls) {
        this.cls = cls;
    }

    public SignatureData read(DataInputStream in, int attribute_length) throws IOException, ClassFormatError {
        if (attribute_length != 2) {
            throw new ClassFormatError(format("%s: Invalid attribute length #%d", Tables.AttrTag.ATT_Signature.printval(), attribute_length));
        }
        signature_index = in.readUnsignedShort();
        return this;
    }

    public void print(String bodyPrefix, String commentPrefix) {
        boolean pr_cpx = options.contains(Options.PR.CPX);
        if (pr_cpx) {
            cls.out.print(format("%s#%d%s%s", bodyPrefix, signature_index, commentPrefix, cls.pool.StringValue(signature_index)));
        } else {
            cls.out.print(format("%s%s%s", bodyPrefix, cls.pool.getName(signature_index), commentPrefix));
        }
    }

    @Override
    public String toString() {
        return format("signature[%d]=%s", signature_index, cls.pool.StringValue(signature_index));
    }
}
