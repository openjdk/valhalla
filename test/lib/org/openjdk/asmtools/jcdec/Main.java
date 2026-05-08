/*
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jcdec;

import static org.openjdk.asmtools.jcoder.JcodTokens.*;
import org.openjdk.asmtools.jdis.uEscWriter;
import org.openjdk.asmtools.util.I18NResourceBundle;
import org.openjdk.asmtools.util.ProductInfo;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Main program of the JavaCard DeCoder
 *
 */
public class Main {

    /*-------------------------------------------------------- */
    /* Main Fields */
    /**
     * Name of the program.
     */
    String program;

    public static final I18NResourceBundle i18n
            = I18NResourceBundle.getBundleForClass(Main.class);
    /**
     * The stream where error message are printed.
     */
    PrintWriter out;
    boolean DebugFlag = false;
    boolean printDetails = false;
    int shift = 0;
    private static final char hexTable[] = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
    /*-------------------------------------------------------- */

    static String toHex(long val, int width) {
        StringBuffer s = new StringBuffer();
        for (int i = width * 2 - 1; i >= 0; i--) {
            s.append(hexTable[((int) (val >> (4 * i))) & 0xF]);
        }
        return "0x" + s.toString();
    }

    static String toHex(long val) {
        int width;
        for (width = 8; width > 0; width--) {
            if ((val >> (width - 1) * 8) != 0) {
                break;
            }
        }
        return toHex(val, width);
    }

    void printByteHex(PrintWriter out, int b) {
        out.print(hexTable[(b >> 4) & 0xF]);
        out.print(hexTable[b & 0xF]);
    }

    /*========================================================*/
    void out_begin(String s) {
        for (int i = 0; i < shift; i++) {
            out.print("  ");
        }
        out.println(s);
        shift++;
    }

    void out_print(String s) {
        for (int i = 0; i < shift; i++) {
            out.print("  ");
        }
        out.print(s);
    }

    void out_println(String s) {
        for (int i = 0; i < shift; i++) {
            out.print("  ");
        }
        out.println(s);
    }

    void out_end(String s) {
        shift--;
        for (int i = 0; i < shift; i++) {
            out.print("  ");
        }
        out.println(s);
    }

    String startArray(int length) {
        return "[" + (printDetails ? Integer.toString(length) : "") + "]";
    }

    void printBytes(DataInputStream in, int len) throws IOException {
        try {
            for (int i = 0; i < len; i++) {
                if (i % 8 == 0) {
                    out_print("0x");
                }
                printByteHex(out, in.readByte());
                if (i % 8 == 7) {
                    out.println(";");
                }
            }
        } finally {
            if (len % 8 != 0) {
                out.println(";");
            }
        }
    }

    /*========================================================*/
    static final int EXPORT_MAGIC = 0x00FACADE;
    static final int HEADER_MAGIC = 0xDECAFFED;
    static String[] compNames = {
        "Header",
        "Directory",
        "Applet",
        "Import",
        "ConstantPool",
        "Class",
        "Method",
        "StaticField",
        "RefLocation",
        "Export",
        "Descriptor"
    };

    static String compName(int compNum) {
        try {
            return compNames[compNum - 1];
        } catch (ArrayIndexOutOfBoundsException e) {
            return "tag " + compNum + "???";
        }
    }
    String[] cPoolStrings;

    void decodeAttr(DataInputStream in) throws IOException {
        int name_cpx = in.readUnsignedShort(), len = in.readInt();
        String AttrName = null;
        String endingComment = "Attr(#" + name_cpx + ")";
        try {
            endingComment = AttrName = cPoolStrings[name_cpx];
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        if (printDetails) {
            out_begin("Attr(#" + name_cpx + ", " + len + ") { // " + AttrName);
        } else {
            out_begin("Attr(#" + name_cpx + ") { // " + AttrName);
        }
        if (AttrName == null) {
            printBytes(in, len);
        } else if (AttrName.equals("ConstantValue")) {
            if (len != 2) {
                out_println("// invalid length of ConstantValue attr: " + len + " (should be 2)");
                printBytes(in, len);
            } else {
                out_println("#" + in.readUnsignedShort() + ";");
            }
        } else {
            printBytes(in, len);
        }
        out_end("} // end " + endingComment);
    }

    void decodeExp(String inpName) throws IOException {
        DataInputStream in = new DataInputStream(new FileInputStream(inpName));
        out_println("file " + inpName);
        out_begin("{  // export file");

        int magic = in.readInt();
        out_print(toHex(magic, 4) + ";  // ");
        if (magic != EXPORT_MAGIC) {
            out.print("wrong magic: 0x" + Integer.toString(EXPORT_MAGIC, 16) + " expected");
        } else {
            out_print("magic");
        }
        out.println();
        out_println(in.readUnsignedByte() + "b;  // minor version");
        out_println(in.readUnsignedByte() + "b;  // major version");

        int cp_count = in.readUnsignedShort();
        cPoolStrings = new String[cp_count];
        out_begin(startArray(cp_count) + " { //  Constant Pool");
        for (int i = 0; i < cp_count; i++) {
            int tag = in.readUnsignedByte();
            ConstType tg = constType(tag);
            switch (tg) {
                case CONSTANT_UTF8:
                    out_print("Utf8 \"");

                    StringBuffer sb = new StringBuffer();
                    String s = in.readUTF();
                    cPoolStrings[i] = s;
                    for (int k = 0; k < s.length(); k++) {
                        char c = s.charAt(k);
                        switch (c) {
                            case '\t':
                                sb.append('\\').append('t');
                                break;
                            case '\n':
                                sb.append('\\').append('n');
                                break;
                            case '\r':
                                sb.append('\\').append('r');
                                break;
                            case '\"':
                                sb.append('\\').append('\"');
                                break;
                            default:
                                sb.append(c);
                        }
                    }
                    out.println(sb.append("\"; // #").append(i).toString());
                    break;

                case CONSTANT_INTEGER:
                    out_println("int " + toHex(in.readInt(), 4) + "; // #" + i);
                    break;

                case CONSTANT_CLASS:
                    out_println("class #" + in.readUnsignedShort() + "; // #" + i);
                    break;

                case CONSTANT_JAVACARD_PACKAGE:
                    out_begin("package { // #" + i);
                    out_println(toHex(in.readUnsignedByte(), 1) + ";  // flags");
                    out_println("#" + in.readUnsignedShort() + "; // name");
                    out_println(in.readUnsignedByte() + "b;  // minor version");
                    out_println(in.readUnsignedByte() + "b;  // major version");
                    int aid_len = in.readUnsignedByte();
                    out_begin("Bytes" + startArray(aid_len) + "b {");
                    printBytes(in, aid_len);
                    out_end("};"); // Bytes[]
                    out_end("};"); // package info
                    break;

                default:
                    throw new Error("invalid constant type: " + (int) tag);
            }
        }
        ;
        out_end("} // Constant pool");
        out_println("#" + in.readUnsignedShort() + ";  // this package");
        int class_count = in.readUnsignedByte();
        out_begin(startArray(class_count) + "b { //  classes");
        for (int i = 0; i < class_count; i++) {
            out_begin("{ // class " + i);

            out_println(in.readUnsignedByte() + "b; // token");

            int flags = in.readUnsignedShort();
            out_print("0x");
            printByteHex(out, flags >> 8);
            printByteHex(out, flags);
            out.println("; // flags");

            out_println("#" + in.readUnsignedShort() + ";  // this class");

            int sup_count = in.readUnsignedShort();
            out_begin(startArray(sup_count) + " { // supers");
            for (int k = 0; k < sup_count; k++) {
                out_println("#" + in.readUnsignedShort() + ";");
            }
            out_end("} // supers");

            int int_count = in.readUnsignedByte();
            out_begin(startArray(int_count) + "b { // interfaces");
            for (int k = 0; k < int_count; k++) {
                out_println("#" + in.readUnsignedShort() + ";");
            }
            out_end("} // interfaces");

            int field_count = in.readUnsignedShort();
            out_begin(startArray(field_count) + " { // fields");
            for (int k = 0; k < field_count; k++) {
                out_begin("{ // field " + k);
                out_println(in.readUnsignedByte() + "b; // token");

                int f_flags = in.readUnsignedShort();
                out_print("0x");
                printByteHex(out, f_flags >> 8);
                printByteHex(out, f_flags);
                out.println("; // flags");

                out_println("#" + in.readUnsignedShort() + ";  // this field name");
                out_println("#" + in.readUnsignedShort() + ";  // this field descriptor");

                int attr_count = in.readUnsignedShort();
                out_begin(startArray(attr_count) + " { // Attributes");
                for (int ai = 0; ai < attr_count; ai++) {
                    decodeAttr(in);
                }
                out_end("} // Attributes");
                out_end("};");
            }
            out_end("} // fields");

            int mth_count = in.readUnsignedShort();
            out_begin(startArray(mth_count) + " { // methods");
            for (int k = 0; k < mth_count; k++) {
                out_begin("{ // method " + k);
                out_println(in.readUnsignedByte() + "b; // token");

                int mth_flags = in.readUnsignedShort();
                out_print("0x");
                printByteHex(out, mth_flags >> 8);
                printByteHex(out, mth_flags);
                out.println("; // flags");

                out_println("#" + in.readUnsignedShort() + ";  // this method name");
                out_println("#" + in.readUnsignedShort() + ";  // this method descriptor");
                out_end("};");
            }
            out_end("} // methods");
            out_end("};");
        }
        out_end("} // classes");
        endComponent(in);
    }

    DataInputStream beginComponent(String inpName) throws IOException {
        DataInputStream in = new DataInputStream(new FileInputStream(inpName));
        out_println("file " + inpName);

        int tag = in.readUnsignedByte();
        out_print("Component(" + tag);
        int size = in.readUnsignedShort();
        if (printDetails) {
            out.print(", " + size);
        }
        out_begin(") { // " + compName(tag));
        return in;
    }

    void endComponent(DataInputStream in) throws IOException {
        out_end("};"); // Component
        int avail = in.available();
        if (avail > 0) {
            out.println("=========== extra bytes:");
            for (int k = 0; k < 8; k++) {
                printBytes(in, avail >= 8 ? 8 : avail);
                avail = in.available();
                if (avail == 0) {
                    break;
                }
            }
            if (avail > 0) {
                out.println("  there is also " + avail + " bytes available");
            }
        }
        in.close();
    }

    ArrayList<Integer> methodsLengths = null;
    ArrayList<Integer> methodsOffsets = null;

    void decodeHeader(String inpName) throws IOException {
        DataInputStream in = beginComponent(inpName);

        int magic = in.readInt();
        out_print(toHex(magic, 4) + ";  // ");
        if (magic != HEADER_MAGIC) {
            out.print("wrong magic: 0x" + Integer.toString(HEADER_MAGIC, 16) + " expected");
        } else {
            out_print("magic");
        }
        out.println();
        out_println(in.readUnsignedByte() + "b;  // minor version");
        out_println(in.readUnsignedByte() + "b;  // major version");
        out_println(toHex(in.readUnsignedByte(), 1) + ";  // flags");

        out_begin("{  // package info");
        out_println(in.readUnsignedByte() + "b;  // minor version");
        out_println(in.readUnsignedByte() + "b;  // major version");
        int aid_len = in.readUnsignedByte();
        out_begin("Bytes" + startArray(aid_len) + "b {");
        printBytes(in, aid_len);
        out_end("};"); // Bytes[]
        out_end("};"); // package info
        endComponent(in);
    }

    void decodeDirectory(String inpName) throws IOException {
        DataInputStream in = beginComponent(inpName);

        int i;
        out_begin("{  // component sizes");
        for (i = 0; i < 11; i++) {
            out_println(in.readUnsignedShort() + ";  // " + (i + 1));
        }
        out_end("};");

        out_begin("{  // static field size");
        out_println(in.readUnsignedShort() + ";  // image size");
        out_println(in.readUnsignedShort() + ";  // array init count");
        out_println(in.readUnsignedShort() + ";  // array init size");
        out_end("};");

        out_println(in.readUnsignedByte() + "b;  // import count");
        out_println(in.readUnsignedByte() + "b;  // applet count");

        int custom_count = in.readUnsignedByte();
        out_begin(startArray(custom_count) + "b { // custom components");
        for (i = 0; i < custom_count; i++) {
            out_print("Comp(" + in.readUnsignedByte());  // tag;
            int size2 = in.readUnsignedShort();
            if (printDetails) {
                out_print(", " + size2);
            }
            out_begin(") {");
            int aid_len = in.readUnsignedByte();
            out_begin("Bytes" + startArray(aid_len) + "b {");
            printBytes(in, aid_len);
            out_end("};");
            out_end("};");
        }
        out_end("};");

        endComponent(in);
    }

    void decodeApplet(String inpName) throws IOException {
        DataInputStream in = beginComponent(inpName);

        int applet_count = in.readUnsignedByte();
        out_begin(startArray(applet_count) + "b { // applets");
        for (int i = 0; i < applet_count; i++) {
            out_begin("{ // applet " + i);
            int aid_len = in.readUnsignedByte();
            out_begin("Bytes" + startArray(aid_len) + "b {");
            printBytes(in, aid_len);
            out_end("};"); // Bytes[]
            out_println(in.readUnsignedShort() + ";  // install method offset");
            out_end("};"); // applet
        }
        out_end("};"); // applets
        endComponent(in);
    }

    void decodeImport(String inpName) throws IOException {
        DataInputStream in = beginComponent(inpName);

        int package_count = in.readUnsignedByte();
        out_begin(startArray(package_count) + "b { //  packages");
        for (int i = 0; i < package_count; i++) {
            out_begin("{ // package " + i);
            out_println(in.readUnsignedByte() + "b;  // minor version");
            out_println(in.readUnsignedByte() + "b;  // major version");
            int aid_len = in.readUnsignedByte();
            out_begin("Bytes" + startArray(aid_len) + "b {");
            printBytes(in, aid_len);
            out_end("};"); // Bytes[]
            out_end("};"); // package info
        }
        out_end("};"); //  package info
        endComponent(in);
    }

    static String[] refNames = {
        "Classref",
        "InstanceFieldref",
        "VirtualMethodref",
        "SuperMethodref",
        "StaticFieldref",
        "StaticMethodref"
    };

    void decodeConstantPool(String inpName) throws IOException {
        DataInputStream in = beginComponent(inpName);

        int items_count = in.readUnsignedShort();
        out_begin(startArray(items_count) + " { //  items");
        for (int i = 0; i < items_count; i++) {
            int tag = in.readUnsignedByte();
            int info1 = in.readUnsignedByte(),
                    info2 = in.readUnsignedByte(),
                    info3 = in.readUnsignedByte();
            out_print(tag + "b ");
            if ((tag > 0) && (tag <= 6)) {
                if ((info1 & 0x80) == 0) {
                    if (tag <= 4) {
                        out_print(((info1 << 8) | info2) + " " + info3 + "b;");
                    } else {
                        out_print(info1 + "b " + ((info2 << 8) | info3) + ";");
                    }
                    out.print(" // internal ");
                } else {
                    out.print(info1 + "b " + info2 + "b " + info3 + "b;");
                    out.print(" // external ");
                }
                out.println(refNames[tag - 1]);
            } else {
                out.print(info1 + "b " + info2 + "b " + info3 + "b;");
                out.println(" // unknown tag ");
            }
        }
        out_end("};"); //  CP array
        endComponent(in);
    }

    void printClassref(DataInputStream in) throws IOException {
        int info1 = in.readUnsignedByte(),
                info2 = in.readUnsignedByte();
        if ((info1 & 0x80) == 0) {
            out_print(((info1 << 8) | info2) + ";");
            out_print(" // internal ");
        } else {
            out_print(info1 + "b " + info2 + "b;");
            out_print(" // external ");
        }
        out_println(" Classref ");
    }

    void decodeClass(String inpName) throws IOException {
        DataInputStream in = beginComponent(inpName);

        for (int i = 0; in.available() > 0; i++) {
            out_begin("{ // class " + i);
            int bitfield = in.readUnsignedByte();
            int interface_count = bitfield & 0x0F;
            out_print("0x");
            printByteHex(out, bitfield);
            out.println("; // bitfield");
            if ((bitfield & 0x80) != 0) {
                // interface
                for (int k = 0; k < interface_count; k++) {
                    printClassref(in);
                }
            } else {
                // class
                printClassref(in);
                out_println(in.readUnsignedByte() + "b;  // declared instance size");
                out_println(in.readUnsignedByte() + "b;  // first reference token");
                out_println(in.readUnsignedByte() + "b;  // reference count");
                out_println(in.readUnsignedByte() + "b;  // public method table base");
                int pumrc = in.readUnsignedByte();
                out_println(pumrc + "b;  // public method table count");
                out_println(in.readUnsignedByte() + "b;  // package method table base");
                int pamrc = in.readUnsignedByte();
                out_println(pamrc + "b;  // package method table count");
                out_begin("{ // public method table");
                for (int k = 0; k < pumrc; k++) {
                    out_println(in.readUnsignedShort() + ";");
                }
                out_end("};");
                out_begin("{ // package method table");
                for (int k = 0; k < pamrc; k++) {
                    out_println(in.readUnsignedShort() + ";");
                }
                out_end("};");
                out_begin("{ // implemented interfaces");
                for (int k = 0; k < interface_count; k++) {
                    out_begin("{ // interface " + k);
                    printClassref(in);
                    int count = in.readUnsignedByte();
                    out_begin("Bytes" + startArray(count) + "b {");
                    printBytes(in, count);
                    out_end("};"); // Bytes[]
                    out_end("};");
                }
                out_end("};");
            }
            out_end("};");
        }
        endComponent(in);
    }

    void decodeDescriptor(String inpName) throws IOException {
        DataInputStream in = beginComponent(inpName);

        methodsLengths = new ArrayList<>();
        methodsOffsets = new ArrayList<>();
        int class_count = in.readUnsignedByte();
        out_begin(startArray(class_count) + "b { // classes");
        for (int c = 0; c < class_count; c++) {
            out_begin("{ // class " + c);
            out_println(in.readUnsignedByte() + "b; // token");
            out_print("0x");
            printByteHex(out, in.readUnsignedByte());
            out.println("; // flags");
            printClassref(in);
            int icount = in.readUnsignedByte();
            out_println(icount + "b; // interface count");
            int fcount = in.readUnsignedShort();
            out_println(fcount + "; // field count");
            int mcount = in.readUnsignedShort();
            out_println(mcount + "; // method count");
            if (icount != 0) {
                out_begin("{ // interfaces");
                for (int i = 0; i < icount; i++) {
                    printClassref(in);
                }
                out_end("};");
            }
            for (int i = 0; i < fcount; i++) {
                out_begin("{ // field " + i);
                out_println(in.readUnsignedByte() + "b; // token");
                int flags = in.readUnsignedByte();
                out_print("0x");
                printByteHex(out, flags);
                out.println("; // flags");
                if ((flags & 0x08) == 0) {
                    printClassref(in);
                    out_println(in.readUnsignedByte() + "b; // token");
                } else { // static field
                    int info1 = in.readUnsignedByte(),
                            info2 = in.readUnsignedByte(),
                            info3 = in.readUnsignedByte();
                    if ((info1 & 0x80) == 0) {
                        out_print(info1 + "b " + ((info2 << 8) | info3) + ";");
                        out.println(" // internal field");
                    } else {
                        out.print(info1 + "b " + info2 + "b " + info3 + "b;");
                        out.println(" // external field");
                    }
                }
                int type = in.readUnsignedShort();
                if ((type & 0x8000) == 0) {
                    out_println(type + "; // reference type");
                } else {
                    out_print("0x");
                    printByteHex(out, type >> 8);
                    printByteHex(out, type);
                    out.println("; // primitive type");
                }
                out_end("};");
            }
            for (int i = 0; i < mcount; i++) {
                int token = in.readUnsignedByte();
                int flags = in.readUnsignedByte();
                int m_offset = in.readUnsignedShort();
                int t_offset = in.readUnsignedShort();
                int bytecode_count = in.readUnsignedShort();
                if (m_offset != 0) {
                    out_begin("{ // method " + i + " (" + methodsLengths.size() + ")");
                    methodsLengths.add(bytecode_count);
                    methodsOffsets.add(m_offset);
                } else {
                    out_begin("{ // method " + i);
                }
                out_println(token + "b; // token");
                out_print("0x");
                printByteHex(out, flags);
                out.println("; // flags");
                out_println(m_offset + "; // method offset");
                out_println(t_offset + "; // type offset");
                out_println(bytecode_count + "; // bytecode count");
                out_println(in.readUnsignedShort() + "; // exception handler count");
                out_println(in.readUnsignedShort() + "; // exception handler index");
                out_end("};");
            }
            out_end("};"); // class i
        }
        out_end("}; // classes");

        int cp_count = in.readUnsignedShort();
        out_begin(startArray(cp_count) + " { // constant pool types");
        for (int i = 0; i < cp_count; i++) {
            int type = in.readUnsignedShort();
            if (type == 0xFFFF) {
                out_println("0xFFFF;");
            } else {
                out_println(type + "; ");
            }
        }
        out_end("}; // constant pool types");

        out_begin("{ // type descriptors");
        for (int i = 0; in.available() > 0; i++) {
            int nibble_count = in.readUnsignedByte();
            out_print(nibble_count + "b; ");
            printBytes(in, (nibble_count + 1) / 2);
        }
        out_end("}; // type descriptors");
        endComponent(in);
    }

    void decodeMethod(String inpName) throws IOException {
        DataInputStream in = beginComponent(inpName);

        int handler_count = in.readUnsignedByte();
        out_begin(startArray(handler_count) + "b { // exception handlers");
        for (int i = 0; i < handler_count; i++) {
            out_print(in.readUnsignedShort() + ", ");
            int bitfield = in.readUnsignedShort();
            out.print("0x");
            printByteHex(out, bitfield >> 8);
            printByteHex(out, bitfield);
            out.print(", " + in.readUnsignedShort() + ", ");
            out.println(in.readUnsignedShort() + "; // handler " + i);
        }
        out_end("};"); // handlers

        if (methodsLengths == null) {
            out.println("// Descriptor.cap absent - methods not printed");
        } else {
            int f_offset = 1 + handler_count * 8;
            for (int i = 0; i < methodsLengths.size(); i++) {
                out_begin("{ // method " + i);
                int m_offset = methodsOffsets.get(i);
                if (m_offset != f_offset) {
                    out.println("file offset=" + f_offset + " but m_offset=" + m_offset);
                    break;
                }
                int bitfield = in.readUnsignedByte();
                if ((bitfield & 0x80) == 0) {
                    out_print("0x");
                    printByteHex(out, bitfield);
                    out.println("; // flags, max_stack");
                    out_print("0x");
                    printByteHex(out, in.readUnsignedByte());
                    out.println("; // nargs, max_locals");
                    f_offset += 2;
                } else {
                    out_print("0x");
                    printByteHex(out, bitfield);
                    out.println("; // flags, padding");
                    out_println(in.readUnsignedByte() + "b; // max_stack");
                    out_println(in.readUnsignedByte() + "b; // nargs");
                    out_println(in.readUnsignedByte() + "b; // max_locals");
                    f_offset += 4;
                }
                int bytecode_count = methodsLengths.get(i);
                out_begin("{ // bytecodes");
                printBytes(in, bytecode_count);
                f_offset += bytecode_count;
                out_end("};");
                out_end("};");
            }
        }

        endComponent(in);
    }

    void decodeStaticField(String inpName) throws IOException {
        DataInputStream in = beginComponent(inpName);

        int image_size = in.readUnsignedShort();
        out_println(image_size + "; // image size");
        int reference_count = in.readUnsignedShort();
        out_println(reference_count + "; // reference count");
        int array_init_count = in.readUnsignedShort();
        out_begin(startArray(array_init_count) + " { // array_init_info");
        for (int i = 0; i < array_init_count; i++) {
            out_println(in.readUnsignedByte() + "b // type ");
            int count = in.readUnsignedShort();
            out_begin("Bytes" + startArray(count) + "s { // values");
            printBytes(in, count);
            out_end("};"); // Bytes[]
        }
        out_end("};"); // array_init_info
        int default_value_count = in.readUnsignedShort();
        out_println(default_value_count + "; // default value count");
        int non_default_value_count = in.readUnsignedShort();
        out_begin("Bytes" + startArray(non_default_value_count) + "s { // non default values");
        printBytes(in, non_default_value_count);
        out_end("};"); // Bytes[]

        endComponent(in);
    }

    void decodeRefLocation(String inpName) throws IOException {
        DataInputStream in = beginComponent(inpName);

        int byte_index_count = in.readUnsignedShort();
        out_begin("Bytes" + startArray(byte_index_count) + "s { // offsets to byte indices");
        printBytes(in, byte_index_count);
        out_end("};"); // Bytes[]

        byte_index_count = in.readUnsignedShort();
        out_begin("Bytes" + startArray(byte_index_count) + "s { // offsets to byte2 indices");
        printBytes(in, byte_index_count);
        out_end("};"); // Bytes[]

        endComponent(in);
    }

    void decodeExport(String inpName) throws IOException {
        DataInputStream in = beginComponent(inpName);
        int class_count = in.readUnsignedByte();
        out_begin(startArray(class_count) + "b { // classes");
        for (int i = 0; i < class_count; i++) {
            out_begin("{ // class " + i);
            out_println(in.readUnsignedShort() + "; // class offset");
            int fcount = in.readUnsignedByte();
            out_println(fcount + "b; // static field count");
            int mcount = in.readUnsignedByte();
            out_println(mcount + "b; // static method count");
            out_begin("{ // static field offsets");
            for (int j = 0; j < fcount; j++) {
                out_println(in.readUnsignedShort() + "; // field " + j + " offset");
            }
            out_end("};");
            out_begin("{ // static method offsets");
            for (int j = 0; j < mcount; j++) {
                out_println(in.readUnsignedShort() + "; // method " + j + " offset");
            }
            out_end("};");
            out_end("};"); // class i
        }
        out_end("};"); // classes
        endComponent(in);
    }
    /*========================================================*/

    /**
     * Constructor.
     */
    public Main(PrintWriter out, String program) {
        this.out = out;
        this.program = program;
    }

    public void error(String msg) {
        out.println(program + ": " + msg);
    }

    /**
     * Usage
     */
    public void usage() {
        out.println(i18n.getString("jcdec.usage"));
        out.println(i18n.getString("jcdec.opt.g"));
        out.println(i18n.getString("jcdec.opt.version"));
    }

    /**
     * Run the decoder
     */
    public synchronized boolean decode(String argv[]) {
//      int flags = F_WARNINGS;
        long tm = System.currentTimeMillis();
        ArrayList<String> vargs = new ArrayList<>();
        ArrayList<String> vj = new ArrayList<>();
        boolean nowrite = false;
        int addOptions = 0;

        // Parse arguments
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            if (arg.equals("-g")) {
                printDetails = true;
                vargs.add(arg);
            } else if (arg.equals("-v")) {
                DebugFlag = true;
                vargs.add(arg);
                out.println("arg[" + i + "]=" + argv[i] + "/verbose");
            } else if (arg.equals("-version")) {
                out.println(ProductInfo.FULL_VERSION);
            } else if (arg.startsWith("-")) {
//out.println("arg["+i+"]="+argv[i]+"/invalid flag");
                error(i18n.getString("jcdec.error.invalid_flag", arg));
                usage();
                return false;
            } else {
                vargs.add(arg);
                vj.add(arg);
            }
        }

        if (vj.isEmpty()) {
            usage();
            return false;
        }

//        String[] names = new String[vj.size()];
//        vj.copyInto(names);
        String[] names = null;
        names = vj.toArray(names);
decode:
        for (int k = 0; k < names.length; k++) {
            String inpname = names[k];
            try {
                if (inpname.endsWith(".cap")) {
                    String shortName = inpname.substring(0, inpname.length() - 4);
                    if (shortName.endsWith("Header")) {
                        decodeHeader(inpname);
                    } else if (shortName.endsWith("Directory")) {
                        decodeDirectory(inpname);
                    } else if (shortName.endsWith("Applet")) {
                        decodeApplet(inpname);
                    } else if (shortName.endsWith("Import")) {
                        decodeImport(inpname);
                    } else if (shortName.endsWith("ConstantPool")) {
                        decodeConstantPool(inpname);
                    } else if (shortName.endsWith("Class")) {
                        decodeClass(inpname);
                    } else if (shortName.endsWith("Descriptor")) {
                        decodeDescriptor(inpname);
                    } else if (shortName.endsWith("Method")) {
                        decodeMethod(inpname);
                    } else if (shortName.endsWith("StaticField")) {
                        decodeStaticField(inpname);
                    } else if (shortName.endsWith("RefLocation")) {
                        decodeRefLocation(inpname);
                    } else if (shortName.endsWith("Export")) {
                        decodeExport(inpname);
                    } else {
                        continue decode;
                    }
                    out.println("");
                } else if (inpname.endsWith(".exp")) {
                    decodeExp(inpname);
                    out.println("");
                }
                continue decode;
            } catch (FileNotFoundException ee) {
                error(i18n.getString("jcdec.error.cannot_read", inpname));
            } catch (Error ee) {
                ee.printStackTrace();
                error(i18n.getString("jcdec.error.fatal_error"));
            } catch (Exception ee) {
                ee.printStackTrace();
                error(i18n.getString("jcdec.error.fatal_exception"));
            }
            return false;
        }
        return true;
    }

    /**
     * Main program
     */
    public static void main(String argv[]) {
        Main decoder = new Main(new PrintWriter(new uEscWriter(System.out)), "jcdec");
        System.exit(decoder.decode(argv) ? 0 : 1);
    }
}
