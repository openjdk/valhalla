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
import org.openjdk.asmtools.jasm.Modifiers;

import java.io.DataInputStream;
import java.io.IOException;

import static java.lang.String.format;
import static org.openjdk.asmtools.jasm.Tables.AttrTag;
import static org.openjdk.asmtools.jasm.Tables.CF_Context;
import static org.openjdk.asmtools.jdis.TraceUtils.traceln;

/**
 * Field data for field members in a class of the Java Disassembler
 */
public class FieldData extends MemberData {

    // CP index to the field name
    protected int name_cpx;
    // CP index to the field type
    protected int type_cpx;
    // CP index to the field value
    protected int value_cpx = 0;

    public FieldData(ClassData cls) {
        super(cls);
        memberType = "FieldData";
    }

    @Override
    protected boolean handleAttributes(DataInputStream in, AttrTag attrtag, int attrlen) throws IOException {
        // Read the Attributes
        boolean handled = true;
        switch (attrtag) {
            case ATT_Signature:
                if( signature != null ) {
                    traceln("Record attribute:  more than one attribute Signature are in component.attribute_info_attributes[attribute_count]");
                    traceln("Last one will be used.");
                }
                signature = new SignatureData(cls).read(in, attrlen);
                break;
            case ATT_ConstantValue:
                if (attrlen != 2) {
                    throw new ClassFormatError(format("%s: Invalid attribute length #%d", AttrTag.ATT_ConstantValue.printval(), attrlen));
                }
                value_cpx = in.readUnsignedShort();
                break;
            default:
                handled = false;
                break;
        }
        return handled;
    }

    /**
     * Read and resolve the field data called from ClassData.
     * Precondition: NumFields has already been read from the stream.
     */
    public void read(DataInputStream in) throws IOException {
        // read the Fields CP indexes
        access = in.readUnsignedShort();
        name_cpx = in.readUnsignedShort();
        type_cpx = in.readUnsignedShort();
        // Read the attributes
        readAttributes(in);
        //
        TraceUtils.traceln(2,
                format("FieldData: name[%d]=%s type[%d]=%s%s",
                        name_cpx, cls.pool.getString(name_cpx),
                        type_cpx, cls.pool.getString(type_cpx),
                        signature != null ? signature : ""));
    }


    /**
     * Prints the field data to the current output stream. called from ClassData.
     */
    @Override
    public void print() throws IOException {
        // Print annotations first
        super.printAnnotations(getIndentString());

        StringBuilder bodyPrefix = new StringBuilder(getIndentString()).append(Modifiers.accessString(access, CF_Context.CTX_FIELD));
        StringBuilder tailPrefix = new StringBuilder();

        if (isSynthetic) {
            bodyPrefix.append(JasmTokens.Token.SYNTHETIC.parseKey()).append(' ');
        }
        if (isDeprecated) {
            bodyPrefix.append(JasmTokens.Token.DEPRECATED.parseKey()).append(' ');
        }

        // field
        bodyPrefix.append(JasmTokens.Token.FIELDREF.parseKey()).append(' ');

        if (value_cpx != 0) {
            tailPrefix.append("\t= ").append(cls.pool.ConstantStrValue(value_cpx));
        }

        printVar(bodyPrefix, tailPrefix,name_cpx, type_cpx);
    }
} // end FieldData

