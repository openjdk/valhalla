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

import static java.lang.String.format;
import static org.openjdk.asmtools.jasm.Tables.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Base class of all AnnotationElement entries
 */
public class AnnotationElement {

    /**
     *
     * CPX_AnnotElem
     *
     * base class for an annotation value.
     *
     */
    public static class AnnotValue {

        /**
         * tag the descriptor for the constant
         */
        public AnnotElemType tag;

        // internal references
        protected ClassData cls;

        public AnnotValue(AnnotElemType tagval, ClassData cls) {
            tag = tagval;
            this.cls = cls;
        }

        public String stringVal() {
            return "";
        }

        public void print(PrintWriter out, String tab) {
            out.print(tag.val() + "  ");
        }

        @Override
        public String toString() {
            return "<AnnotValue " + tag.printval() + " " + stringVal() + ">";
        }
    }

    /**
     *
     * CPX_AnnotElem
     *
     * Annotation value which is described by a single CPX entry (ie. String, byte, char,
     * int, short, boolean, float, long, double, class reference).
     *
     */
    public static class CPX_AnnotValue extends AnnotValue {

        /**
         * tag the descriptor for the constant
         */
        public int cpx;

        public CPX_AnnotValue(AnnotElemType tag, ClassData cls, int cpx) {
            super(tag, cls);
            this.cpx = cpx;
        }

        @Override
        public String stringVal() {
            StringBuilder sb = new StringBuilder();
            switch (tag) {
                case AE_STRING:    // String
                    sb.append('"' + cls.pool.getString(cpx) + '"');
                    break;
                case AE_BYTE:    // Byte
                    sb.append("byte " + cls.pool.getConst(cpx).stringVal());
                    break;
                case AE_CHAR:    // Char
                    sb.append("char " + cls.pool.getConst(cpx).stringVal());
                    break;
                case AE_INT:    // Int  (no need to add keyword)
                    sb.append(cls.pool.getConst(cpx).stringVal());
                    break;
                case AE_SHORT:    // Short
                    sb.append("short " + cls.pool.getConst(cpx).stringVal());
                    break;
                case AE_BOOLEAN:    // Boolean
                    ConstantPool.CP_Int cns = (ConstantPool.CP_Int) cls.pool.getConst(cpx);
                    sb.append("boolean " + (cns.value == 0 ? "false" : "true"));
                    break;
                case AE_FLOAT:    // Float
                    sb.append(cls.pool.getConst(cpx).stringVal()); // + "f");
                    break;
                case AE_DOUBLE:    // Double
                    sb.append(cls.pool.getConst(cpx).stringVal()); // + "d");
                    break;
                case AE_LONG:    // Long
                    sb.append(cls.pool.getConst(cpx).stringVal()); // + "l");
                    break;
                case AE_CLASS:    // Class
                    sb.append("class " + cls.pool.decodeClassDescriptor(cpx));
                    break;
                default:
                    break;
            }
            return sb.toString();
        }

        @Override
        public void print(PrintWriter out, String tab) {
            out.print(tab + stringVal());
        }

        @Override
        public String toString() {
            return "<CPX_AnnotValue tag: '" + tag + "' stringVal=" + this.stringVal() + ">";
        }
    }

    /**
     *
     * CPX_AnnotElem
     *
     * AnnotElements that contain 2 cpx indices (ie. enums).
     *
     */
    public static class CPX2_AnnotValue extends AnnotValue {

        /**
         * tag the descriptor for the constant
         */
        public int cpx1;
        public int cpx2;

        public CPX2_AnnotValue(AnnotElemType tag, ClassData cls, int cpx1, int cpx2) {
            super(tag, cls);
            this.cpx1 = cpx1;
            this.cpx2 = cpx2;
        }

        @Override
        public String stringVal() {
            StringBuilder sb = new StringBuilder();
            switch (tag) {

                case AE_ENUM:    // Enum
                    // print the enum type and constant name
                    sb.append("enum " + cls.pool.decodeClassDescriptor(cpx1)
                            + " " + cls.pool.getName(cpx2));
                    break;
                default:
                    break;
            }
            return sb.toString();
        }

        @Override
        public void print(PrintWriter out, String tab) {
            out.print(tab + stringVal());
        }

        @Override
        public String toString() {
            return "<CPX2_AnnotValue tag: '" + tag + "' stringVal=" + this.stringVal() + ">";
        }
    }

    /**
     *
     * Array_AnnotElem
     *
     * Annotation value that is an array of annotation elements.
     *
     */
    public static class Array_AnnotValue extends AnnotValue {

        /**
         * tag the descriptor for the constant
         */
        public ArrayList<AnnotValue> array = new ArrayList<>();

        public Array_AnnotValue(AnnotElemType tagval, ClassData cls) {
            super(tagval, cls);
        }

        @Override
        public String stringVal() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.stringVal() + " = ");
            sb.append("{");
            int i = 0;
            int cnt = array.size();
            for (AnnotValue arrayelem : array) {
                sb.append(arrayelem.toString());
                if (i < cnt - 1) {
                    sb.append(",");
                }
            }
            sb.append("}");
            return sb.toString();
        }

        public void add(AnnotValue elem) {
            array.add(elem);
        }

        @Override
        public void print(PrintWriter out, String tab) {
            out.println("{");
            int i = 0;
            int cnt = array.size();
            for (AnnotValue arrayelem : array) {
                arrayelem.print(out, tab + "  ");
                if (i < cnt - 1) {
                    out.println(",");
                }
                i += 1;
            }
            out.println("}");
        }

        @Override
        public String toString() {
            return "<Array_AnnotValue " + tag + " " + stringVal() + ">";
        }
    }

    /**
     *
     * Annot_AnnotValue
     *
     * Annotation value that is a reference to an annotation.
     *
     */
    public static class Annot_AnnotValue extends AnnotValue {

        /**
         * tag the descriptor for the constant
         */
        AnnotationData annot;

        public Annot_AnnotValue(AnnotElemType tagval, ClassData cls, AnnotationData annot) {
            super(tagval, cls);
            this.annot = annot;
        }

        @Override
        public String stringVal() {
            return annot.toString();
        }

        @Override
        public void print(PrintWriter out, String tab) {
//            out.print(tag + "\t");
            annot.print(out, tab);
        }

        @Override
        public String toString() {
            return "<Annot_AnnotValue " + tag + " " + stringVal() + ">";
        }
    }

    /*========================================================*/
    /* Factory Method */
    /**
     *
     * read
     *
     * Static factory - creates Annotation Elements.
     *
     */
    public static AnnotValue readValue(DataInputStream in, ClassData cls, boolean invisible) throws IOException {
        AnnotValue val = null;
        char tg = (char) in.readByte();
        AnnotElemType tag = annotElemType(tg);

        switch (tag) {
            case AE_STRING:     // String
            case AE_BYTE:       // Byte
            case AE_CHAR:       // Char
            case AE_INT:        // Int  (no need to add keyword)
            case AE_SHORT:      // Short
            case AE_BOOLEAN:    // Boolean
            case AE_FLOAT:      // Float
            case AE_DOUBLE:     // Double
            case AE_LONG:       // Long
            case AE_CLASS:      // Class
                // CPX based Annotation
                int CPX = in.readShort();
                val = new CPX_AnnotValue(tag, cls, CPX);
                break;
            case AE_ENUM:    // Enum
                // CPX2 based Annotation
                int CPX1 = in.readShort();
                int CPX2 = in.readShort();
                val = new CPX2_AnnotValue(tag, cls, CPX1, CPX2);
                break;
            case AE_ANNOTATION:    // Annotation
                AnnotationData ad = new AnnotationData(invisible, cls);
                ad.read(in);
                val = new Annot_AnnotValue(tag, cls, ad);
                break;
            case AE_ARRAY:    // Array
                Array_AnnotValue aelem = new Array_AnnotValue(tag, cls);
                val = aelem;
                int cnt = in.readShort();
                for (int i = 0; i < cnt; i++) {
                    aelem.add(readValue(in, cls, invisible));
                }
                break;
            default:
                throw new IOException("Unknown tag in annotation '" + tg + "' [" + Integer.toHexString(tg) + "]");
        }

        return val;
    }

    /*========================================================*/

    /*-------------------------------------------------------- */
    /* AnnotElem Fields */
    /**
     * constant pool index for the name of the Annotation Element
     */
    public int name_cpx;

    public AnnotValue value = null;

    // internal references
    protected ClassData cls;
    /*-------------------------------------------------------- */

    public AnnotationElement(ClassData cls) {
        this.cls = cls;
    }

    /**
     *
     * read
     *
     * read and resolve the method data called from ClassData. precondition: NumFields has
     * already been read from the stream.
     *
     */
    public void read(DataInputStream in, boolean invisible) throws IOException {
        name_cpx = in.readShort();
        value = readValue(in, cls, invisible);
        TraceUtils.traceln(format("                   AnnotElem: name[%d]=%s value=%s", name_cpx, cls.pool.getString(name_cpx), value.toString()));
    }

    public String stringVal() {
        return cls.pool.getName(name_cpx);
    }

    public void print(PrintWriter out, String tab) {
        out.print(stringVal() + " = ");
        value.print(out, "");
    }

    @Override
    public String toString() {
        return "<AnnotElem " + stringVal() + " = " + value.toString() + ">";
    }
}
