/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jdec;

import org.openjdk.asmtools.common.Module;
import org.openjdk.asmtools.asmutils.StringUtils;
import org.openjdk.asmtools.jasm.Modifiers;
import org.openjdk.asmtools.jcoder.JcodTokens;
import org.openjdk.asmtools.util.I18NResourceBundle;

import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;

import static java.lang.String.format;
import static org.openjdk.asmtools.jasm.Tables.*;
import static org.openjdk.asmtools.jasm.Tables.AnnotElemType.AE_UNKNOWN;
import static org.openjdk.asmtools.jasm.TypeAnnotationTypes.*;

/**
 * Class data of the Java Decoder
 */
class ClassData {

    private byte[] types;
    private Object[] cpool;
    private int CPlen;
    private NestedByteArrayInputStream countedin;
    private DataInputStream in;
    private PrintWriter out;
    private int[] cpe_pos;
    private boolean printDetails;
    private String entityType = "";
    private String entityName = "";

    public static I18NResourceBundle i18n
            = I18NResourceBundle.getBundleForClass(Main.class);

    ClassData(DataInputStream dis, int printFlags, PrintWriter out) throws IOException {
        byte[] buf = new byte[dis.available()];
        try {
            if (dis.read(buf) <= 0)
                throw new IOException("The file is empty");
        } finally {
            dis.close();
        }
        countedin = new NestedByteArrayInputStream(buf);
        in = new DataInputStream(countedin);
        this.out = out;
        printDetails = ((printFlags & 1) == 1);
    }

    /*========================================================*/
    private static final char[] hexTable = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private String toHex(long val, int width) {
        StringBuilder s = new StringBuilder();
        for (int i = width * 2 - 1; i >= 0; i--) {
            s.append(hexTable[((int) (val >> (4 * i))) & 0xF]);
        }
        return "0x" + s.toString();
    }

    private String toHex(long val) {
        int width;
        for (width = 8; width > 0; width--) {
            if ((val >> (width - 1) * 8) != 0) {
                break;
            }
        }
        return toHex(val, width);
    }

    private void printByteHex(PrintWriter out, int b) {
        out.print(hexTable[(b >> 4) & 0xF]);
        out.print(hexTable[b & 0xF]);
    }

    private void printBytes(PrintWriter out, DataInputStream in, int len)
            throws IOException {
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

    private void printRestOfBytes() {
        for (int i = 0; ; i++) {
            try {
                byte b = in.readByte();
                if (i % 8 == 0) {
                    out_print("0x");
                }
                printByteHex(out, b);
                if (i % 8 == 7) {
                    out.print(";\n");
                }
            } catch (IOException e) {
                return;
            }
        }
    }

    private void printUtf8InfoIndex(int index, String indexName) {
        String name = (String) cpool[index];
        out_print("#" + index + "; // ");
        if (printDetails) {
            out.println(String.format("%-16s",indexName) + " : " + name);
        } else {
            out.println(indexName);
        }
    }

    /*========================================================*/
    private int shift = 0;

    private void out_begin(String s) {
        for (int i = 0; i < shift; i++) {
            out.print("  ");
        }
        out.println(s);
        shift++;
    }

    private void out_print(String s) {
        for (int i = 0; i < shift; i++) {
            out.print("  ");
        }
        out.print(s);
    }

    private void out_println(String s) {
        for (int i = 0; i < shift; i++) {
            out.print("  ");
        }
        out.println(s);
    }

    private void out_end(String s) {
        shift--;
        for (int i = 0; i < shift; i++) {
            out.print("  ");
        }
        out.println(s);
    }

    private String startArray(int length) {
        return "[" + (printDetails ? Integer.toString(length) : "") + "]";
    }

    private void startArrayCmt(int length, String comment) {
        out_begin(startArray(length) + format(" {%s", comment == null ? "" : " // " + comment));
    }

    private void startArrayCmtB(int length, String comment) {
        out_begin(startArray(length) + format("b {%s", comment == null ? "" : " // " + comment));
    }

    /*========================================================*/
    private void readCP(DataInputStream in) throws IOException {
        int length = in.readUnsignedShort();
        CPlen = length;
        traceln(i18n.getString("jdec.trace.CP_len", length));
        types = new byte[length];
        cpool = new Object[length];
        cpe_pos = new int[length];
        for (int i = 1; i < length; i++) {
            byte btag;
            int v1;
            long lv;
            cpe_pos[i] = countedin.getPos();
            btag = in.readByte();
            traceln(i18n.getString("jdec.trace.CP_entry", i, btag));
            types[i] = btag;
            ConstType tg = tag(btag);
            switch (tg) {
                case CONSTANT_UTF8:
                    cpool[i] = in.readUTF();
                    break;
                case CONSTANT_INTEGER:
                    v1 = in.readInt();
                    cpool[i] = v1;
                    break;
                case CONSTANT_FLOAT:
                    v1 = Float.floatToIntBits(in.readFloat());
                    cpool[i] = v1;
                    break;
                case CONSTANT_LONG:
                    lv = in.readLong();
                    cpool[i] = lv;
                    i++;
                    break;
                case CONSTANT_DOUBLE:
                    lv = Double.doubleToLongBits(in.readDouble());
                    cpool[i] = lv;
                    i++;
                    break;
                case CONSTANT_CLASS:
                case CONSTANT_STRING:
                case CONSTANT_MODULE:
                case CONSTANT_PACKAGE:
                    v1 = in.readUnsignedShort();
                    cpool[i] = v1;
                    break;
                case CONSTANT_INTERFACEMETHOD:
                case CONSTANT_FIELD:
                case CONSTANT_METHOD:
                case CONSTANT_NAMEANDTYPE:
                    cpool[i] = "#" + in.readUnsignedShort() + " #" + in.readUnsignedShort();
                    break;
                case CONSTANT_DYNAMIC:
                case CONSTANT_INVOKEDYNAMIC:
                    cpool[i] = in.readUnsignedShort() + "s #" + in.readUnsignedShort();
                    break;
                case CONSTANT_METHODHANDLE:
                    cpool[i] = in.readUnsignedByte() + "b #" + in.readUnsignedShort();
                    break;
                case CONSTANT_METHODTYPE:
                    cpool[i] = "#" + in.readUnsignedShort();
                    break;
                default:
                    CPlen = i;
                    printCP(out);
                    out_println(toHex(btag, 1) + "; // invalid constant type: " + (int) btag + " for element " + i);
                    throw new ClassFormatError();
            }
        }
    }

    private void printCP(PrintWriter out) {
        int length = CPlen;
        startArrayCmt(length, "Constant Pool");
        out_println("; // first element is empty");
        try {
            int size;
            for (int i = 1; i < length; i = i + size) {
                size = 1;
                byte btag = types[i];
                ConstType tg = tag(btag);
                int pos = cpe_pos[i];
                String tagstr;
                String valstr;
                int v1;
                long lv;
                if (tg != null) {
                    tagstr = tg.parseKey();
                } else {
                    throw new Error("Can't get a tg representing the type of Constant in the Constant Pool at: " + i);
                }
                switch (tg) {
                    case CONSTANT_UTF8: {
                        tagstr = "Utf8";
                        valstr = StringUtils.Utf8ToString((String) cpool[i]);
                    }
                    break;
                    case CONSTANT_FLOAT:
                    case CONSTANT_INTEGER:
                        v1 = (Integer) cpool[i];
                        valstr = toHex(v1, 4);
                        break;
                    case CONSTANT_DOUBLE:
                    case CONSTANT_LONG:
                        lv = (Long) cpool[i];
                        valstr = toHex(lv, 8) + ";";
                        size = 2;
                        break;
                    case CONSTANT_CLASS:
                    case CONSTANT_MODULE:
                    case CONSTANT_PACKAGE:
                    case CONSTANT_STRING:
                        v1 = (Integer) cpool[i];
                        valstr = "#" + v1;
                        break;
                    case CONSTANT_INTERFACEMETHOD:
                    case CONSTANT_FIELD:
                    case CONSTANT_METHOD:
                    case CONSTANT_NAMEANDTYPE:
                    case CONSTANT_METHODHANDLE:
                    case CONSTANT_METHODTYPE:
                    case CONSTANT_DYNAMIC:
                    case CONSTANT_INVOKEDYNAMIC:
                        valstr = (String) cpool[i];
                        break;
                    default:
                        throw new Error("invalid constant type: " + (int) btag);
                }
                out_print(tagstr + " " + valstr + "; // #" + i);
                if (printDetails) {
                    out_println(" at " + toHex(pos));
                } else {
                    out.println();
                }
            }
        } finally {
            out_end("} // Constant Pool");
            out.println();
        }
    }

    private String getStringPos() {
        return " at " + toHex(countedin.getPos());
    }

    private String getCommentPosCond() {
        if (printDetails) {
            return " // " + getStringPos();
        } else {
            return "";
        }
    }

    private void decodeCPXAttr(DataInputStream in, int len, String attrname, PrintWriter out) throws IOException {
        decodeCPXAttrM(in, len, attrname, out, 1);
    }

    private void decodeCPXAttrM(DataInputStream in, int len, String attrname, PrintWriter out, int expectedIndices) throws IOException {
        if (len != expectedIndices * 2) {
            out_println("// invalid length of " + attrname + " attr: " + len + " (should be " + (expectedIndices * 2) + ") > ");
            printBytes(out, in, len);
        } else {
            StringBuilder outputString = new StringBuilder();
            for (int k = 1; k <= expectedIndices; k++) {
                outputString.append("#").append(in.readUnsignedShort()).append("; ");
                if (k % 16 == 0) {
                    out_println(outputString.toString().replaceAll("\\s+$",""));
                    outputString = new StringBuilder();
                }
            }
            if (outputString.length() > 0) {
                out_println(outputString.toString().replaceAll("\\s+$",""));
            }
        }
    }

    private void printStackMap(DataInputStream in, int elementsNum) throws IOException {
        int num;
        if (elementsNum > 0) {
            num = elementsNum;
        } else {
            num = in.readUnsignedShort();
        }
        out.print(startArray(num) + (elementsNum > 0 ? "z" : "") + "{");
        try {
            for (int k = 0; k < num; k++) {
                int maptype = in.readUnsignedByte();
                StackMapType mptyp = stackMapType(maptype, out);
                String maptypeImg;
                if (printDetails) {
                    maptypeImg = maptype + "b";
                } else {
                    try {
                        maptypeImg = mptyp.parsekey();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        maptypeImg = "/* BAD TYPE: */ " + maptype + "b";
                    }
                }
                switch (mptyp) {
                    case ITEM_Object:
                    case ITEM_NewObject:
                        maptypeImg = maptypeImg + "," + in.readUnsignedShort();
                        break;
                    case ITEM_UNKNOWN:
                        maptypeImg = maptype + "b";
                        break;
                    default:
                }
                out.print(maptypeImg);
                if (k < num - 1) {
                    out.print("; ");
                }
            }
        } finally {
            out.print("}");
        }
    }

    /**
     * Processes 4.7.20 The RuntimeVisibleTypeAnnotations Attribute, 4.7.21 The RuntimeInvisibleTypeAnnotations Attribute
     * <code>type_annotation</code> structure.
     */
    private void decodeTargetTypeAndRefInfo(DataInputStream in) throws IOException {
        int tt = in.readUnsignedByte(); // [4.7.20] annotations[], type_annotation { u1 target_type; ...}
        ETargetType targetType = ETargetType.getTargetType(tt);
        if( targetType == null ) {
            throw new Error("Type annotation: invalid target_type(u1) " + tt);
        }
        ETargetInfo targetInfo = targetType.targetInfo();
        out_println(toHex(tt, 1) + ";  //  target_type: " + targetType.parseKey());
        switch (targetInfo) {
            case TYPEPARAM:          //[3.3.1] meth_type_param, class_type_param:
                out_println(toHex(in.readUnsignedByte(), 1) + ";  //  param_index");
                break;
            case SUPERTYPE:         //[3.3.2]  class_exts_impls
                out_println(toHex(in.readUnsignedShort(), 2) + ";  //  type_index");
                break;
            case TYPEPARAM_BOUND:   //[3.3.3]  class_type_param_bnds, meth_type_param_bnds
                out_println(toHex(in.readUnsignedByte(), 1) + ";  //  param_index");
                out_println(toHex(in.readUnsignedByte(), 1) + ";  //  bound_index");
                break;
            case EMPTY:             //[3.3.4]  meth_receiver, meth_ret_type, field
                // NOTE: reference_info is empty for this annotation's target
                break;
            case METHODPARAM:       //[3.3.5]  meth_formal_param:
                out_println(toHex(in.readUnsignedByte(), 1) + ";  //  parameter_index");
                break;
            case EXCEPTION:         //[3.3.61]  throws_type
                //KTL:  Updated index to UShort for JSR308 change
                out_println(in.readUnsignedShort() + ";  //  type_index");
                break;
            case LOCALVAR: //[3.3.7]  local_var, resource_var
            {
                int lv_num = in.readUnsignedShort();
                startArrayCmt(lv_num, "local_variables");
                try {
                    for (int i = 0; i < lv_num; i++) {
                        out_println(in.readUnsignedShort() + " " + in.readUnsignedShort()
                                + " " + in.readUnsignedShort() + ";" + getCommentPosCond());
                    }
                } finally {
                    out_end("}");
                }
            }
            break;
            case CATCH:             //[3.3.8]  exception_param
                out_println(in.readUnsignedShort() + ";  //  exception_table_index");
                break;
            case OFFSET:            //[3.3.9]  type_test (instanceof), obj_creat (new)
                // constr_ref_receiver, meth_ref_receiver
                out_println(in.readUnsignedShort() + ";  //  offset");
                break;
            case TYPEARG:           //[3.3.10]  cast, constr_ref_typearg, meth_invoc_typearg
                // constr_invoc_typearg, meth_ref_typearg
                out_println(in.readUnsignedShort() + ";  //  offset");
                out_println(toHex(in.readUnsignedByte(), 1) + ";  //  type_index");
                break;
            default:                // should never happen
                out_println(toHex(tt, 1) + "; // invalid target_info: " + tt);
                throw new ClassFormatError();
        }
        // [4.7.20.2]
        int path_length = in.readUnsignedByte();  // type_path { u1 path_length; ...}
        startArrayCmtB(path_length, "type_paths");
        try {
            for (int i = 0; i < path_length; i++) {
                // print the type_path elements
                out_println("{ " + toHex(in.readUnsignedByte(), 1)  // { u1 type_path_kind;
                        + "; " + toHex(in.readUnsignedByte(), 1)    //   u1 type_argument_index; }
                        + "; } // type_path[" + i + "]");           // path[i]
            }
        } finally {
            out_end("}");
        }
    }

    private void decodeElementValue(DataInputStream in, PrintWriter out) throws IOException {
        out_begin("{  //  element_value");
        try {
            char tg = (char) in.readByte();
            AnnotElemType tag = annotElemType(tg);
            if (tag != AE_UNKNOWN) {
                out_println("'" + tg + "';");
            }
            switch (tag) {
                case AE_BYTE:
                case AE_CHAR:
                case AE_DOUBLE:
                case AE_FLOAT:
                case AE_INT:
                case AE_LONG:
                case AE_SHORT:
                case AE_BOOLEAN:
                case AE_STRING:
                    decodeCPXAttr(in, 2, "const_value_index", out);
                    break;
                case AE_ENUM:
                    out_begin("{  //  enum_const_value");
                    decodeCPXAttr(in, 2, "type_name_index", out);
                    decodeCPXAttr(in, 2, "const_name_index", out);
                    out_end("}  //  enum_const_value");
                    break;
                case AE_CLASS:
                    decodeCPXAttr(in, 2, "class_info_index", out);
                    break;
                case AE_ANNOTATION:
                    decodeAnnotation(in, out);
                    break;
                case AE_ARRAY:
                    int ev_num = in.readUnsignedShort();
                    startArrayCmt(ev_num, "array_value");
                    try {
                        for (int i = 0; i < ev_num; i++) {
                            decodeElementValue(in, out);
                            if (i < ev_num - 1) {
                                out_println(";");
                            }
                        }
                    } finally {
                        out_end("}  //  array_value");
                    }
                    break;
                case AE_UNKNOWN:
                default:
                    String msg = "invalid element_value" + (isPrintableChar(tg) ? " tag type : " + tg : "");
                    out_println(toHex(tg, 1) + "; // " + msg);
                    throw new ClassFormatError(msg);
            }
        } finally {
            out_end("}  //  element_value");
        }
    }

    public boolean isPrintableChar(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return (!Character.isISOControl(c)) &&
                c != KeyEvent.CHAR_UNDEFINED &&
                block != null &&
                block != Character.UnicodeBlock.SPECIALS;
    }

    private void decodeAnnotation(DataInputStream in, PrintWriter out) throws IOException {
        out_begin("{  //  annotation");
        try {
            decodeCPXAttr(in, 2, "field descriptor", out);
            int evp_num = in.readUnsignedShort();
            decodeElementValuePairs(evp_num, in, out);
        } finally {
            out_end("}  //  annotation");
        }
    }

    private void decodeElementValuePairs(int count, DataInputStream in, PrintWriter out) throws IOException {
        startArrayCmt(count, "element_value_pairs");
        try {
            for (int i = 0; i < count; i++) {
                out_begin("{  //  element value pair");
                try {
                    decodeCPXAttr(in, 2, "name of the annotation type element", out);
                    decodeElementValue(in, out);
                } finally {
                    out_end("}  //  element value pair");
                    if (i < count - 1) {
                        out_println(";");
                    }
                }
            }
        } finally {
            out_end("}  //  element_value_pairs");
        }
    }

    /**
     * component_info {     JEP 359 Record(Preview): class file 58.65535
     *     u2               name_index;
     *     u2               descriptor_index;
     *     u2               attributes_count;
     *     attribute_info attributes[attributes_count];
     * }
     *
     * or
     * field_info {
     *     u2             access_flags;
     *     u2             name_index;
     *     u2             descriptor_index;
     *     u2             attributes_count;
     *     attribute_info attributes[attributes_count];
     * }
     * or
     * method_info {
     *     u2             access_flags;
     *     u2             name_index;
     *     u2             descriptor_index;
     *     u2             attributes_count;
     *     attribute_info attributes[attributes_count];
     * }
     *
     */
    private void decodeInfo(DataInputStream in, PrintWriter out, String elementName, boolean hasAccessFlag) throws IOException {
        out_begin("{  // " + elementName + (printDetails ? getStringPos() : ""));
        try {
            if(hasAccessFlag) {
                //  u2 access_flags;
                out_println(toHex(in.readShort(), 2) + "; // access");
            }
            // u2 name_index
            printUtf8InfoIndex(in.readUnsignedShort(), "name_index");
            // u2 descriptor_index
            printUtf8InfoIndex(in.readUnsignedShort(), "descriptor_index");
            // u2 attributes_count;
            // attribute_info attributes[attributes_count]
            decodeAttrs(in, out);
        } finally {
            out_end("}");
        }
    }

    private void decodeTypeAnnotation(DataInputStream in, PrintWriter out) throws IOException {
        out_begin("{  //  type_annotation");
        try {
            decodeTargetTypeAndRefInfo(in);
            decodeCPXAttr(in, 2, "field descriptor", out);
            int evp_num = in.readUnsignedShort();
            decodeElementValuePairs(evp_num, in, out);
        } finally {
            out_end("}  //  type_annotation");
        }
    }

    private void decodeBootstrapMethod(DataInputStream in) throws IOException {
        out_begin("{  //  bootstrap_method");
        try {
            out_println("#" + in.readUnsignedShort() + "; // bootstrap_method_ref");
            int bm_args_cnt = in.readUnsignedShort();
            startArrayCmt(bm_args_cnt, "bootstrap_arguments");
            try {
                for (int i = 0; i < bm_args_cnt; i++) {
                    out_println("#" + in.readUnsignedShort() + ";" + getCommentPosCond());
                }
            } finally {
                out_end("}  //  bootstrap_arguments");
            }
        } finally {
            out_end("}  //  bootstrap_method");
        }
    }

    private void decodeAttr(DataInputStream in, PrintWriter out) throws IOException {
        // Read one attribute
        String posComment = getStringPos();
        int name_cpx = in.readUnsignedShort(), btag, len;

        String AttrName = "";
        try {
            btag = types[name_cpx];
            ConstType tag = tag(btag);

            if (tag == ConstType.CONSTANT_UTF8) {
                AttrName = (String) cpool[name_cpx];
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
        AttrTag tg = attrtag(AttrName);
        String endingComment = AttrName;
        len = in.readInt();
        countedin.enter(len);
        try {
            if (printDetails) {
                out_begin("Attr(#" + name_cpx + ", " + len + ") { // " + AttrName + posComment);
            } else {
                out_begin("Attr(#" + name_cpx + ") { // " + AttrName);
            }

            switch (tg) {
                case ATT_Code:
                    out_println(in.readUnsignedShort() + "; // max_stack");
                    out_println(in.readUnsignedShort() + "; // max_locals");
                    int code_len = in.readInt();
                    out_begin("Bytes" + startArray(code_len) + "{");
                    try {
                        printBytes(out, in, code_len);
                    } finally {
                        out_end("}");
                    }
                    int trap_num = in.readUnsignedShort();
                    startArrayCmt(trap_num, "Traps");
                    try {
                        for (int i = 0; i < trap_num; i++) {
                            out_println(in.readUnsignedShort() + " " +
                                    in.readUnsignedShort() + " " +
                                    in.readUnsignedShort() + " " +
                                    in.readUnsignedShort() + ";" +
                                    getCommentPosCond());
                        }
                    } finally {
                        out_end("} // end Traps");
                    }
                    // Read the attributes
                    decodeAttrs(in, out);
                    break;

                case ATT_Exceptions:
                    int count = in.readUnsignedShort();
                    startArrayCmt(count, AttrName);
                    try {
                        for (int i = 0; i < count; i++) {
                            out_println("#" + in.readUnsignedShort() + ";" +
                                    getCommentPosCond());
                        }
                    } finally {
                        out_end("}");
                    }
                    break;
                case ATT_LineNumberTable:
                    int ll_num = in.readUnsignedShort();
                    startArrayCmt(ll_num, "line_number_table");
                    try {
                        for (int i = 0; i < ll_num; i++) {
                            out_println(in.readUnsignedShort() + "  " +
                                    in.readUnsignedShort() + ";" +
                                    getCommentPosCond());
                        }
                    } finally {
                        out_end("}");
                    }
                    break;
                case ATT_LocalVariableTable:
                case ATT_LocalVariableTypeTable:
                    int lvt_num = in.readUnsignedShort();
                    startArrayCmt(lvt_num, AttrName);
                    try {
                        for (int i = 0; i < lvt_num; i++) {
                            out_println(in.readUnsignedShort() + " " +
                                    in.readUnsignedShort() + " " +
                                    in.readUnsignedShort() + " " +
                                    in.readUnsignedShort() + " " +
                                    in.readUnsignedShort() + ";" +
                                    getCommentPosCond());
                        }
                    } finally {
                        out_end("}");
                    }
                    break;
                case ATT_InnerClasses:
                    int ic_num = in.readUnsignedShort();
                    startArrayCmt(ic_num, "classes");
                    try {
                        for (int i = 0; i < ic_num; i++) {
                            out_println("#" + in.readUnsignedShort() + " #" +
                                    in.readUnsignedShort() + " #" +
                                    in.readUnsignedShort() + " " +
                                    in.readUnsignedShort() + ";" + getCommentPosCond());
                        }
                    } finally {
                        out_end("}");
                    }
                    break;
                case ATT_StackMap:
                    int e_num = in.readUnsignedShort();
                    startArrayCmt(e_num, "");
                    try {
                        for (int k = 0; k < e_num; k++) {
                            int start_pc = in.readUnsignedShort();
                            out_print("" + start_pc + ", ");
                            printStackMap(in, 0);
                            out.print(", ");
                            printStackMap(in, 0);
                            out.println(";");
                        }
                    } finally {
                        out_end("}");
                    }
                    break;
                case ATT_StackMapTable:
                    int et_num = in.readUnsignedShort();
                    startArrayCmt(et_num, "");
                    try {
                        for (int k = 0; k < et_num; k++) {
                            int frame_type = in.readUnsignedByte();
                            StackMapFrameType ftype = stackMapFrameType(frame_type);
                            switch (ftype) {
                                case SAME_FRAME:
                                    // type is same_frame;
                                    out_print("" + frame_type + "b");
                                    out.println("; // same_frame");
                                    break;
                                case SAME_LOCALS_1_STACK_ITEM_FRAME:
                                    // type is same_locals_1_stack_item_frame
                                    out_print("" + frame_type + "b, ");
                                    // read additional single stack element
                                    printStackMap(in, 1);
                                    out.println("; // same_locals_1_stack_item_frame");
                                    break;
                                case SAME_LOCALS_1_STACK_ITEM_EXTENDED_FRAME:
                                    // type is same_locals_1_stack_item_frame_extended
                                    int noffset = in.readUnsignedShort();
                                    out_print("" + frame_type + "b, " + noffset + ", ");
                                    // read additional single stack element
                                    printStackMap(in, 1);
                                    out.println("; // same_locals_1_stack_item_frame_extended");
                                    break;
                                case CHOP_1_FRAME:
                                case CHOP_2_FRAME:
                                case CHOP_3_FRAME:
                                    // type is chop_frame
                                    int coffset = in.readUnsignedShort();
                                    out_print("" + frame_type + "b, " + coffset);
                                    out.println("; // chop_frame " + (251 - frame_type));
                                    break;
                                case SAME_FRAME_EX:
                                    // type is same_frame_extended;
                                    int xoffset = in.readUnsignedShort();
                                    out_print("" + frame_type + "b, " + xoffset);
                                    out.println("; // same_frame_extended");
                                    break;
                                case APPEND_FRAME:
                                    // type is append_frame
                                    int aoffset = in.readUnsignedShort();
                                    out_print("" + frame_type + "b, " + aoffset + ", ");
                                    // read additional locals
                                    printStackMap(in, frame_type - 251);
                                    out.println("; // append_frame " + (frame_type - 251));
                                    break;
                                case FULL_FRAME:
                                    // type is full_frame
                                    int foffset = in.readUnsignedShort();
                                    out_print("" + frame_type + "b, " + foffset + ", ");
                                    printStackMap(in, 0);
                                    out.print(", ");
                                    printStackMap(in, 0);
                                    out.println("; // full_frame");
                                    break;
                            }
                        }
                    } finally {
                        out_end("}");
                    }
                    break;
                case ATT_EnclosingMethod:
                    decodeCPXAttrM(in, len, AttrName, out, 2);
                    break;
                case ATT_AnnotationDefault:
                    decodeElementValue(in, out);
                    break;
                case ATT_RuntimeInvisibleAnnotations:
                case ATT_RuntimeVisibleAnnotations:
                    int an_num = in.readUnsignedShort();
                    startArrayCmt(an_num, "annotations");
                    try {
                        for (int i = 0; i < an_num; i++) {
                            decodeAnnotation(in, out);
                            if (i < an_num - 1) {
                                out_println(";");
                            }
                        }
                    } finally {
                        out_end("}");
                    }
                    break;
                // 4.7.20 The RuntimeVisibleTypeAnnotations Attribute
                // 4.7.21 The RuntimeInvisibleTypeAnnotations Attribute
                case ATT_RuntimeInvisibleTypeAnnotations:
                case ATT_RuntimeVisibleTypeAnnotations:
                    int ant_num = in.readUnsignedShort();
                    startArrayCmt(ant_num, "annotations");
                    try {
                        for (int i = 0; i < ant_num; i++) {
                            decodeTypeAnnotation(in, out);
                            if (i < ant_num - 1) {
                                out_println(";");
                            }
                        }
                    } finally {
                        out_end("}");
                    }
                    break;
                case ATT_RuntimeInvisibleParameterAnnotations:
                case ATT_RuntimeVisibleParameterAnnotations:
                    int pm_num = in.readUnsignedByte();
                    startArrayCmtB(pm_num, "parameters");
                    try {
                        for (int k = 0; k < pm_num; k++) {
                            int anp_num = in.readUnsignedShort();
                            startArrayCmt(anp_num, "annotations");
                            try {
                                for (int i = 0; i < anp_num; i++) {
                                    decodeAnnotation(in, out);
                                    if (k < anp_num - 1) {
                                        out_println(";");
                                    }
                                }
                            } finally {
                                out_end("}");
                            }
                            if (k < pm_num - 1) {
                                out_println(";");
                            }
                        }
                    } finally {
                        out_end("}");
                    }
                    break;
                case ATT_BootstrapMethods:
                    int bm_num = in.readUnsignedShort();
                    startArrayCmt(bm_num, "bootstrap_methods");
                    try {
                        for (int i = 0; i < bm_num; i++) {
                            decodeBootstrapMethod(in);
                            if (i < bm_num - 1) {
                                out_println(";");
                            }
                        }
                    } finally {
                        out_end("}");
                    }
                    break;
                case ATT_Module:
                    decodeModule(in);
                    break;
                case ATT_TargetPlatform:
                    decodeCPXAttrM(in, len, AttrName, out, 3);
                    break;
                case ATT_ModulePackages:
                    int p_num = in.readUnsignedShort();
                    startArrayCmt(p_num, null);
                    try {
                        decodeCPXAttrM(in, len - 2, AttrName, out, p_num);
                    } finally {
                        out_end("}");
                    }
                    break;
                //  MethodParameters_attribute {
                //    u2 attribute_name_index;
                //    u4 attribute_length;
                //    u1 parameters_count;
                //    {   u2 name_index;
                //        u2 access_flags;
                //    } parameters[parameters_count];
                //  }
                case ATT_MethodParameters:
                    int pcount = in.readUnsignedByte();
                    startArrayCmtB(pcount, AttrName);
                    try {
                        for (int i = 0; i < pcount; i++) {
                            out_println("#" + in.readUnsignedShort() + "  " +
                                    toHex(in.readUnsignedShort(), 2) + ";" +
                                    getCommentPosCond());
                        }
                    } finally {
                        out_end("}");
                    }
                    break;
                //  JEP 359 Record(Preview): class file 58.65535
                //  Record_attribute {
                //      u2 attribute_name_index;
                //      u4 attribute_length;
                //      u2 components_count;
                //      component_info components[components_count];
                //  }
                case ATT_Record:
                    int ncomps = in.readUnsignedShort();
                    startArrayCmt(ncomps, "components");
                    try {
                        for (int i = 0; i < ncomps; i++) {
                            decodeInfo(in,out,"component",false);
                            if (i < ncomps - 1) {
                                out_println(";");
                            }
                        }
                    } finally {
                        out_end("}");
                    }
                    break;
                case ATT_ConstantValue:
                case ATT_Signature:
                case ATT_SourceFile:
                    decodeCPXAttr(in, len, AttrName, out);
                    break;
                    //  JEP 181 (Nest-based Access Control): class file 55.0
                    //  NestHost_attribute {
                    //    u2 attribute_name_index;
                    //    u4 attribute_length;
                    //    u2 host_class_index;
                    //  }
                case ATT_NestHost:
                    decodeTypes(in, out, 1);
                    break;
                    //  JEP 181 (Nest-based Access Control): class file 55.0
                    //  NestMembers_attribute {
                    //    u2 attribute_name_index;
                    //    u4 attribute_length;
                    //    u2 number_of_classes;
                    //    u2 classes[number_of_classes];
                    //  }
                case ATT_NestMembers:
                    //  JEP 360 (Sealed types): class file 59.65535
                    //  PermittedSubclasses_attribute {
                    //    u2 attribute_name_index;
                    //    u4 attribute_length;
                    //    u2 number_of_classes;
                    //    u2 classes[number_of_classes];
                    //  }
                case ATT_PermittedSubclasses:
                    // Preload attribute has same format
                case ATT_Preload:
                    int nsubtypes = in.readUnsignedShort();
                    startArrayCmt(nsubtypes, "classes");
                    try {
                        decodeTypes(in, out, nsubtypes);
                    } finally {
                        out_end("}");
                    }
                    break;
                default:
                    printBytes(out, in, len);
                    if (AttrName == null) {
                        endingComment = "Attr(#" + name_cpx + ")";
                    }
            }

        } catch (EOFException e) {
            out.println("// ======== unexpected end of attribute array");
        } finally {
            int rest = countedin.available();
            if (rest > 0) {
                out.println("// ======== attribute array started " + posComment + " has " + rest + " bytes more:");
                printBytes(out, in, rest);
            }
            out_end("} // end " + endingComment);
            countedin.leave();
        }
    }

    private void decodeModuleStatement(String statementName, DataInputStream in) throws IOException {
        // u2 {exports|opens}_count
        int count = in.readUnsignedShort();
        startArrayCmt(count, statementName);
        try {
            for (int i = 0; i < count; i++) {
                // u2 {exports|opens}_index; u2 {exports|opens}_flags
                int index = in.readUnsignedShort();
                int nFlags = in.readUnsignedShort();
                String sFlags = printDetails ? Module.Modifier.getStatementFlags(nFlags) : "";
                out_println("#" + index + " " + toHex(nFlags, 2) + (sFlags.isEmpty() ? "" : " // [ " + sFlags + " ]"));
                int exports_to_count = in.readUnsignedShort();
                startArrayCmt(exports_to_count, null);
                try {
                    for (int j = 0; j < exports_to_count; j++) {
                        out_println("#" + in.readUnsignedShort() + ";");
                    }
                } finally {
                    out_end("};");
                }
            }
        } finally {
            out_end("} // " + statementName + "\n");
        }
    }

    private void decodeModule(DataInputStream in) throws IOException {
        //u2 module_name_index
        int index = in.readUnsignedShort();
        entityName = (String) cpool[(Integer) cpool[index]];
        out_print("#" + index + "; // ");
        if (printDetails) {
            out.println(String.format("%-16s","name_index") + " : " + entityName);
        } else {
            out.println("name_index");
        }

        // u2 module_flags
        int moduleFlags = in.readUnsignedShort();
        out_print(toHex(moduleFlags, 2) + "; // flags");
        if (printDetails) {
            out_print(" " + Module.Modifier.getModuleFlags(moduleFlags));
        }
        out.println();

        //u2 module_version
        int versionIndex = in.readUnsignedShort();
        out_println("#" + versionIndex + "; // version");

        // u2 requires_count
        int count = in.readUnsignedShort();
        startArrayCmt(count, "requires");
        try {
            for (int i = 0; i < count; i++) {
                // u2 requires_index; u2 requires_flags; u2 requires_version_index
                index = in.readUnsignedShort();
                int nFlags = in.readUnsignedShort();
                versionIndex = in.readUnsignedShort();
                String sFlags = printDetails ? Module.Modifier.getStatementFlags(nFlags) : "";
                out_println("#" + index + " " + toHex(nFlags, 2) + " #" + versionIndex + ";" + (sFlags.isEmpty() ? "" : " // " + sFlags));
            }
        } finally {
            out_end("} // requires\n");
        }

        decodeModuleStatement("exports", in);

        decodeModuleStatement("opens", in);
        // u2 uses_count
        count = in.readUnsignedShort();
        startArrayCmt(count, "uses");
        try {
            for (int i = 0; i < count; i++) {
                // u2 uses_index
                out_println("#" + in.readUnsignedShort() + ";");
            }
        } finally {
            out_end("} // uses\n");
        }
        count = in.readUnsignedShort(); // u2 provides_count
        startArrayCmt(count, "provides");
        try {
            for (int i = 0; i < count; i++) {
                // u2 provides_index
                out_println("#" + in.readUnsignedShort());
                int provides_with_count = in.readUnsignedShort();
                // u2 provides_with_count
                startArrayCmt(provides_with_count, null);
                try {
                    for (int j = 0; j < provides_with_count; j++) {
                        // u2 provides_with_index;
                        out_println("#" + in.readUnsignedShort() + ";");
                    }
                } finally {
                    out_end("};");
                }
            }
        } finally {
            out_end("} // provides\n");
        }
    }

    private void decodeAttrs(DataInputStream in, PrintWriter out) throws IOException {
        // Read the attributes
        int attr_num = in.readUnsignedShort();
        startArrayCmt(attr_num, "Attributes");
        try {
            for (int i = 0; i < attr_num; i++) {
                decodeAttr(in, out);
                if (i + 1 < attr_num) {
                    out_println(";");
                }
            }
        } finally {
            out_end("} // Attributes");
        }
    }

    private void decodeMembers(DataInputStream in, PrintWriter out, String groupName, String elementName) throws IOException {
        int count = in.readUnsignedShort();
        traceln(groupName + "=" + count);
        startArrayCmt(count, groupName);
        try {
            for (int i = 0; i < count; i++) {
                decodeInfo(in,out,elementName,true);
                if (i + 1 < count) {
                    out_println(";");
                }
            }
        } finally {
            out_end("} // " + groupName);
            out.println();
        }
    }

    void decodeClass(String fileName) throws IOException {
        // Read the header
        try {
            int magic = in.readInt();
            int min_version = in.readUnsignedShort();
            int version = in.readUnsignedShort();

            // Read the constant pool
            readCP(in);
            short access = in.readShort(); // don't care about sign
            int this_cpx = in.readUnsignedShort();

            try {
                entityName = (String) cpool[(Integer) cpool[this_cpx]];
                if (entityName.equals("module-info")) {
                    entityType = "module";
                    entityName = "";
                } else {
                    entityType = "class";
                }
                if (!entityName.isEmpty() && (JcodTokens.keyword_token_ident(entityName) != JcodTokens.Token.IDENT || JcodTokens.constValue(entityName) != -1)) {
                    // JCod can't parse a entityName matching a keyword or a constant value,
                    // then use the filename instead:
                    out_begin(String.format("file \"%s.class\" {", entityName));
                } else {
                    out_begin(format("%s %s {", entityType, entityName));
                }
            } catch (Exception e) {
                entityName = fileName;
                out.println("// " + e.getMessage() + " while accessing entityName");
                out_begin(format("%s %s { // source file name", entityType, entityName));
            }

            out_print(toHex(magic, 4) + ";");
            if (magic != JAVA_MAGIC) {
                out.print(" // wrong magic: 0x" + Integer.toString(JAVA_MAGIC, 16) + " expected");
            }
            out.println();
            out_println(min_version + "; // minor version");
            out_println(version + "; // version");

            // Print the constant pool
            printCP(out);
            out_println(toHex(access, 2) + "; // access" +
                    (printDetails ? " [" + (" " + Modifiers.accessString(access, CF_Context.CTX_CLASS).toUpperCase()).replaceAll(" (\\S)", " ACC_$1") + "]" : ""));
            out_println("#" + this_cpx + ";// this_cpx");
            int super_cpx = in.readUnsignedShort();
            out_println("#" + super_cpx + ";// super_cpx");
            traceln(i18n.getString("jdec.trace.access_thisCpx_superCpx", access, this_cpx, super_cpx));
            out.println();

            // Read the interfaces
            int numinterfaces = in.readUnsignedShort();
            traceln(i18n.getString("jdec.trace.numinterfaces", numinterfaces));
            startArrayCmt(numinterfaces, "Interfaces");
            try {
                decodeTypes(in, out, numinterfaces);
            } finally {
                out_end("} // Interfaces\n");
            }
            // Read the fields
            decodeMembers(in, out, "Fields", "field");

            // Read the methods
            decodeMembers(in, out, "Methods", "method");

            // Read the attributes
            decodeAttrs(in, out);
        } catch (EOFException ignored) {
        } catch (ClassFormatError err) {
            String msg = err.getMessage();
            out.println("//------- ClassFormatError" +
                    (msg == null || msg.isEmpty() ? "" : ": " + msg));
            printRestOfBytes();
        } finally {
            out_end(format("} // end %s %s", entityType, entityName));
        }
    } // end decodeClass()

    private void decodeTypes(DataInputStream in, PrintWriter out, int count) throws IOException {
        for (int i = 0; i < count; i++) {
            int type_cpx = in.readUnsignedShort();
            traceln(i18n.getString("jdec.trace.type", i, type_cpx));
            out_print("#" + type_cpx + ";");
            if (printDetails) {
                String name = (String) cpool[(int)cpool[type_cpx]];
                out.println(" // " + name + getStringPos());
            } else {
                out.println();
            }
        }
    }

    /* ====================================================== */
    boolean DebugFlag = false;

    public void trace(String s) {
        if (!DebugFlag) {
            return;
        }
        System.out.print(s);
    }

    public void traceln(String s) {
        if (!DebugFlag) {
            return;
        }
        System.out.println(s);
    }
}// end class ClassData
