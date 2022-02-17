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
package org.openjdk.asmtools.jdis;

import org.openjdk.asmtools.jasm.JasmTokens;
import org.openjdk.asmtools.jasm.Tables;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.*;
import static org.openjdk.asmtools.jdis.TraceUtils.traceln;

/**
 * The Record attribute data
 * <p>
 * since class file 58.65535 (JEP 359)
 */
public class RecordData extends  Indenter {


    private final ClassData cls;
    private List<Component> components;

    public RecordData(ClassData cls) {
        this.cls = cls;
    }

    public RecordData read(DataInputStream in) throws IOException {
        int count = in.readUnsignedShort();
        traceln("components=" + count);
        components = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            components.add(new Component(cls).read(in));
        }
        return this;
    }

    /**
     * Prints the record data to the current output stream. called from ClassData.
     */
    public void print() throws IOException {
        int count = components.size();
        if (count > 0) {
            cls.out.println(getIndentString() + RECORD.parseKey() + getIndentString() + LBRACE.parseKey());
            for (int i = 0; i < count; i++) {
                Component cn = components.get(i);
                cn.setIndent(indent() * 2);
                if (i != 0 && cn.getAnnotationsCount() > 0)
                    cn.out.println();
                cn.print();
            }
            cls.out.println(getIndentString() + RBRACE.parseKey());
            cls.out.println();
        }
    }

    private class Component extends MemberData {
        // CP index to the name
        private int name_cpx;
        // CP index to the type descriptor
        private int type_cpx;

        public Component(ClassData cls) {
            super(cls);
            memberType = "RecordData";
        }

        @Override
        protected boolean handleAttributes(DataInputStream in, Tables.AttrTag attrtag, int attrlen) throws IOException {
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
                default:
                    handled = false;
                    break;
            }
            return handled;
        }

        /**
         * Read and resolve the component data called from ClassData.
         */
        public Component read(DataInputStream in) throws IOException {
            // read the Component CP indexes
            name_cpx = in.readUnsignedShort();
            type_cpx = in.readUnsignedShort();
            traceln(2, "RecordComponent: name[" + name_cpx + "]=" + cls.pool.getString(name_cpx)
                    + " descriptor[" + type_cpx + "]=" + cls.pool.getString(type_cpx));
            // Read the attributes
            readAttributes(in);
            return this;
        }

        /**
         * Prints the component data to the current output stream. called from RecordData.
         */
        public void print() throws IOException {
            // print component's attributes
                super.printAnnotations(getIndentString());
            // print component
            StringBuilder bodyPrefix = new StringBuilder(getIndentString());
            StringBuilder tailPrefix = new StringBuilder();
            if (isSynthetic) {
                bodyPrefix.append(JasmTokens.Token.SYNTHETIC.parseKey()).append(' ');
            }
            if (isDeprecated) {
                bodyPrefix.append(JasmTokens.Token.DEPRECATED.parseKey()).append(' ');
            }
            // component
            bodyPrefix.append(JasmTokens.Token.COMPONENT.parseKey()).append(' ');

            printVar(bodyPrefix, tailPrefix,name_cpx, type_cpx);
        }
    }
}
