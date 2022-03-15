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

import org.openjdk.asmtools.asmutils.HexUtils;
import org.openjdk.asmtools.asmutils.StringUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.openjdk.asmtools.jdis.Utils.commentString;

/**
 *
 * ConstantPool
 *
 * Class representing the ConstantPool
 */
public class ConstantPool {

    private static final Hashtable<Byte, TAG> taghash = new Hashtable<>();
    private static final Hashtable<Byte, SUBTAG> subtaghash = new Hashtable<>();

    private boolean printTAG = false;

    public void setPrintTAG(boolean value) {
        this.printTAG = value;
    }

    public String getPrintedTAG(TAG tag) {
        return (this.printTAG) ? tag.tagname + " " : "" ;
    }

    class Indent {
        private int length, offset, step;

        void inc() {  length+=step; }

        void dec() { length-=step; }

        Indent(int offset, int step) {
            this.length = 0;
            this.step = step;
            this.offset = offset;
        }

        int size() { return  offset + length; }

        /**
         * Creates indent string based on current indent size.
         */
        private String get() {
            return Collections.nCopies(size(), "\t").stream().collect(Collectors.joining());
        }
    }

    private final Indent indent = new Indent(2, 1);

    /**
     * TAG
     *
     * A Tag descriptor of constants in the constant pool
     *
     */
    public enum TAG {
        CONSTANT_UTF8               ((byte) 1, "Asciz", "CONSTANT_UTF8"),
        CONSTANT_UNICODE            ((byte) 2, "unicorn", "CONSTANT_UNICODE"),
        CONSTANT_INTEGER            ((byte) 3, "int", "CONSTANT_INTEGER"),
        CONSTANT_FLOAT              ((byte) 4, "float", "CONSTANT_FLOAT"),
        CONSTANT_LONG               ((byte) 5, "long", "CONSTANT_LONG"),
        CONSTANT_DOUBLE             ((byte) 6, "double", "CONSTANT_DOUBLE"),
        CONSTANT_CLASS              ((byte) 7, "class", "CONSTANT_CLASS"),
        CONSTANT_STRING             ((byte) 8, "String", "CONSTANT_STRING"),
        CONSTANT_FIELD              ((byte) 9, "Field", "CONSTANT_FIELD"),
        CONSTANT_METHOD             ((byte) 10, "Method", "CONSTANT_METHOD"),
        CONSTANT_INTERFACEMETHOD    ((byte) 11, "InterfaceMethod", "CONSTANT_INTERFACEMETHOD"),
        CONSTANT_NAMEANDTYPE        ((byte) 12, "NameAndType", "CONSTANT_NAMEANDTYPE"),
        CONSTANT_METHODHANDLE       ((byte) 15, "MethodHandle", "CONSTANT_METHODHANDLE"),
        CONSTANT_METHODTYPE         ((byte) 16, "MethodType", "CONSTANT_METHODTYPE"),
        CONSTANT_DYNAMIC            ((byte) 17, "Dynamic", "CONSTANT_DYNAMIC"),
        CONSTANT_INVOKEDYNAMIC      ((byte) 18, "InvokeDynamic", "CONSTANT_INVOKEDYNAMIC"),
        CONSTANT_MODULE             ((byte) 19, "Module", "CONSTANT_MODULE"),
        CONSTANT_PACKAGE            ((byte) 20, "Package", "CONSTANT_PACKAGE");

        private final Byte value;
        private final String tagname;
        private final String printval;

        TAG(byte val, String tgname, String print) {
            value = val;
            tagname = tgname;
            printval = print;
        }

        public byte value() {
            return value;
        }

        public String tagname() {
            return tagname;
        }

        public String description() {
            return printval;
        }

        @Override
        public String toString() {
            return "<" + tagname + "> ";
        }

    };


    /**
    * SUBTAG
    *
    * A Tag descriptor of form method-handle constants
    *
    */
    static public enum SUBTAG {
        REF_GETFIELD            ((byte) 1, "REF_getField", "REF_GETFIELD"),
        REF_GETSTATIC           ((byte) 2, "REF_getStatic", "REF_GETSTATIC"),
        REF_PUTFIELD            ((byte) 3, "REF_putField", "REF_PUTFIELD"),
        REF_PUTSTATIC           ((byte) 4, "REF_putStatic", "REF_PUTSTATIC"),
        REF_INVOKEVIRTUAL       ((byte) 5, "REF_invokeVirtual", "REF_INVOKEVIRTUAL"),
        REF_INVOKESTATIC        ((byte) 6, "REF_invokeStatic", "REF_INVOKESTATIC"),
        REF_INVOKESPECIAL       ((byte) 7, "REF_invokeSpecial", "REF_INVOKESPECIAL"),
        REF_NEWINVOKESPECIAL    ((byte) 8, "REF_newInvokeSpecial", "REF_NEWINVOKESPECIAL"),
        REF_INVOKEINTERFACE     ((byte) 9, "REF_invokeInterface", "REF_INVOKEINTERFACE");

        private final Byte value;
        private final String tagname;
        private final String printval;

        SUBTAG(byte val, String tgname, String print) {
            value = val;
            tagname = tgname;
            printval = print;
//            subtaghash.put(new Byte(val), this);
        }

        public byte value() {
            return value;
        }

        public String tagname() {
            return tagname;
        }

        public String description() {
            return printval;
        }

        @Override
        public String toString() {
            return "<" + tagname + "> ";
        }
    };

    static {

        // Class initializer Code
        //
        // Make sure all of the tags get initialized before being used.
        taghash.put(TAG.CONSTANT_UTF8.value(), TAG.CONSTANT_UTF8);
        taghash.put(TAG.CONSTANT_UNICODE.value(), TAG.CONSTANT_UNICODE);
        taghash.put(TAG.CONSTANT_INTEGER.value(), TAG.CONSTANT_INTEGER);
        taghash.put(TAG.CONSTANT_FLOAT.value(), TAG.CONSTANT_FLOAT);
        taghash.put(TAG.CONSTANT_LONG.value(), TAG.CONSTANT_LONG);
        taghash.put(TAG.CONSTANT_DOUBLE.value(), TAG.CONSTANT_DOUBLE);
        taghash.put(TAG.CONSTANT_CLASS.value(), TAG.CONSTANT_CLASS);
        taghash.put(TAG.CONSTANT_STRING.value(), TAG.CONSTANT_STRING);
        taghash.put(TAG.CONSTANT_FIELD.value(), TAG.CONSTANT_FIELD);
        taghash.put(TAG.CONSTANT_METHOD.value(), TAG.CONSTANT_METHOD);
        taghash.put(TAG.CONSTANT_INTERFACEMETHOD.value(), TAG.CONSTANT_INTERFACEMETHOD);
        taghash.put(TAG.CONSTANT_NAMEANDTYPE.value(), TAG.CONSTANT_NAMEANDTYPE);
        taghash.put(TAG.CONSTANT_METHODHANDLE.value(), TAG.CONSTANT_METHODHANDLE);
        taghash.put(TAG.CONSTANT_METHODTYPE.value(), TAG.CONSTANT_METHODTYPE);
        taghash.put(TAG.CONSTANT_DYNAMIC.value(), TAG.CONSTANT_DYNAMIC);
        taghash.put(TAG.CONSTANT_INVOKEDYNAMIC.value(), TAG.CONSTANT_INVOKEDYNAMIC);
        taghash.put(TAG.CONSTANT_MODULE.value(), TAG.CONSTANT_MODULE);
        taghash.put(TAG.CONSTANT_PACKAGE.value(), TAG.CONSTANT_PACKAGE);

        subtaghash.put(SUBTAG.REF_GETFIELD.value(), SUBTAG.REF_GETFIELD);
        subtaghash.put(SUBTAG.REF_GETSTATIC.value(), SUBTAG.REF_GETSTATIC);
        subtaghash.put(SUBTAG.REF_PUTFIELD.value(), SUBTAG.REF_PUTFIELD);
        subtaghash.put(SUBTAG.REF_PUTSTATIC.value(), SUBTAG.REF_PUTSTATIC);
        subtaghash.put(SUBTAG.REF_INVOKEVIRTUAL.value(), SUBTAG.REF_INVOKEVIRTUAL);
        subtaghash.put(SUBTAG.REF_INVOKESTATIC.value(), SUBTAG.REF_INVOKESTATIC);
        subtaghash.put(SUBTAG.REF_INVOKESPECIAL.value(), SUBTAG.REF_INVOKESPECIAL);
        subtaghash.put(SUBTAG.REF_NEWINVOKESPECIAL.value(), SUBTAG.REF_NEWINVOKESPECIAL);
        subtaghash.put(SUBTAG.REF_INVOKEINTERFACE.value(), SUBTAG.REF_INVOKEINTERFACE);

    }

    /**
     *
     * Constant
     *
     * Base class of all constant entries
     *
     */
    public class Constant {

        /**
         * tag the descriptor for the constant
         */
        public TAG tag;

        public Constant(TAG tagval) {
            tag = tagval;
        }

        public String stringVal() {
            return "";
        }

        public void print(PrintWriter out) {
            out.print(tag.tagname + "\t");
        }

        public int size() {
            return 1;
        }

        @Override
        public String toString() {
            return "<CONSTANT " + tag.toString() + " " + stringVal() + ">";
        }

        private IOException issue;

        public IOException getIssue() {
            return issue;
        }

        public void setIssue(IOException value) {
            issue = value;
        }

    }

    /* -------------------------------------------------------- */
    /* Constant Sub-classes */
    /**
     *
     * CP_Str
     *
     * Constant entries that contain String data. usually is a CONSTANT_UTF8
     *
     */
    class CP_Str extends Constant {

        String value;

        CP_Str(TAG tagval, String str) {
            super(tagval);
            this.value = str;
        }

        @Override
        public String stringVal() {
            return StringUtils.Utf8ToString(value);
        }

        @Override
        public void print(PrintWriter out) {
            super.print(out);
            out.println(stringVal() + ";");
        }
    }

    /**
     *
     * CP_Int
     *
     * Constant entries that contain Integer data. usually is a CONSTANT_INTEGER
     *
     */
    class CP_Int extends Constant {

        Integer value;

        CP_Int(TAG tagval, int intval) {
            super(tagval);
            this.value = intval;
        }

        @Override
        public String stringVal() {
            if (cd.options.contains(Options.PR.HEX)) {
                return HexUtils.toHex(value.intValue());
            }
            return value.toString();
        }

        @Override
        public void print(PrintWriter out) {
            super.print(out);
            out.println(stringVal() + ";");
        }
    }

    /**
     *
     * CP_Long
     *
     * Constant entries that contain LongInteger data. usually is a CONSTANT_LONG
     *
     * These take up 2 slots in the constant pool.
     *
     */
    class CP_Long extends Constant {

        Long value;

        CP_Long(TAG tagval, long intval) {
            super(tagval);
            this.value = intval;
        }

        @Override
        public String stringVal() {
            if (cd.options.contains(Options.PR.HEX)) {
                return HexUtils.toHex(value.longValue()) + 'l';
            }
            return value.toString() + 'l';
        }

        @Override
        public void print(PrintWriter out) {
            super.print(out);
            out.println(stringVal() + ";");
        }

        @Override
        public int size() {
            return 2;
        }
    }

    /**
     *
     * CP_Float
     *
     * Constant entries that contain Float data. usually is a CONSTANT_FLOAT
     *
     */
    class CP_Float extends Constant {

        Float value;

        CP_Float(TAG tagval, float fltvl) {
            super(tagval);
            this.value = fltvl;
        }

        @Override
        public String stringVal() {
            if (cd.options.contains(Options.PR.HEX)) {
                return "bits " + HexUtils.toHex(Float.floatToIntBits(value.floatValue()));
            }
            String sf = (value).toString();
            if (value.isNaN() || value.isInfinite()) {
                return sf;
            }
            return sf + "f";
        }

        @Override
        public void print(PrintWriter out) {
            super.print(out);
            out.println(stringVal() + ";");
        }
    }

    /**
     *
     * CP_Double
     *
     * Constant entries that contain double-precision float data. usually is a
     * CONSTANT_DOUBLE
     *
     * These take up 2 slots in the constant pool.
     *
     */
    class CP_Double extends Constant {

        Double value;

        CP_Double(TAG tagval, double fltvl) {
            super(tagval);
            this.value = fltvl;
        }

        @Override
        public String stringVal() {
            if (cd.options.contains(Options.PR.HEX)) {
                return "bits " + HexUtils.toHex(Double.doubleToLongBits(value.doubleValue())) + 'l';
            }
            String sd = value.toString();
            if (value.isNaN() || value.isInfinite()) {
                return sd;
            }
            return sd + "d";
        }

        @Override
        public void print(PrintWriter out) {
            super.print(out);
            out.println(stringVal() + ";");
        }

        @Override
        public int size() {
            return 2;
        }
    }

    /**
     *
     * CPX
     *
     * Constant entries that contain a single constant-pool index. Usually, this includes:
     * CONSTANT_CLASS CONSTANT_METHODTYPE CONSTANT_STRING CONSTANT_MODULE CONSTANT_PACKAGE
     *
     */
    class CPX extends Constant {

        int value;

        CPX(TAG tagval, int cpx) {
            super(tagval);
            this.value = cpx;
        }

        @Override
        public String stringVal() {
            String str = "UnknownTag";
            switch (tag) {
                case CONSTANT_CLASS:
                    str = getShortClassName(getClassName(this), cd.pkgPrefix);
                    break;
                case CONSTANT_PACKAGE:
                case CONSTANT_MODULE:
                    str = getString(value);
                    break;
                case CONSTANT_METHODTYPE:
                case CONSTANT_STRING:
                    str = StringValue(value);
                    break;
                default:
                    break;
            }
            return str;
        }

        @Override
        public void print(PrintWriter out) {
            super.print(out);
            switch (tag) {
                case CONSTANT_CLASS:
                case CONSTANT_STRING:
                case CONSTANT_METHODTYPE:
                case CONSTANT_PACKAGE:
                case CONSTANT_MODULE:
                    out.println("#" + (value) + ";\t//  " + stringVal());
                    break;
            }
        }
    }

    /**
     *
     * CPX2
     *
     * Constant entries that contain two constant-pool indices. Usually, this includes:
     * CONSTANT_FIELD CONSTANT_METHOD CONSTANT_INTERFACEMETHOD CONSTANT_NAMEANDTYPE
     * CONSTANT_METHODHANDLE CONSTANT_DYNAMIC CONSTANT_INVOKEDYNAMIC
     *
     */
    class CPX2 extends Constant {

        int value1, value2;

        CPX2(TAG tagval, int cpx1, int cpx2) {
            super(tagval);
            this.value1 = cpx1;
            this.value2 = cpx2;
        }

        @Override
        public String stringVal() {

            String str = "UnknownTag";
            switch (tag) {
                case CONSTANT_FIELD:
                    // CODETOOLS-7902660: the tag Field is not necessary while printing static parameters of a bsm
                    // Example: MethodHandle REF_getField:ClassName.FieldName:"I"
                    str = getShortClassName(getClassName(value1), cd.pkgPrefix) + "." + StringValue(value2);
                    break;
                case CONSTANT_METHOD:
                case CONSTANT_INTERFACEMETHOD:
                    // CODETOOLS-7902648: added printing of the tag: Method/Interface to clarify
                    // interpreting CONSTANT_MethodHandle_info:reference_kind
                    // Example: invokedynamic InvokeDynamic REF_invokeStatic:Method java/lang/runtime/ObjectMethods.bootstrap
                    str = getPrintedTAG(tag) + getShortClassName(getClassName(value1), cd.pkgPrefix) + "." + StringValue(value2);
                    break;
                case CONSTANT_NAMEANDTYPE:
                    str = getName(value1) + ":" + StringValue(value2);
                    break;
                case CONSTANT_METHODHANDLE:
                    str = subtagToString(value1) + ":" + StringValue(value2);
                    break;
                case CONSTANT_DYNAMIC:
                case CONSTANT_INVOKEDYNAMIC:
                    int bsm_attr_idx = value1;
                    int nape_idx = value2;
                    BootstrapMethodData bsmData;
                    try {
                        bsmData = cd.bootstrapMethods.get(bsm_attr_idx);
                    } catch (NullPointerException npe) {
                        return "<Missing BootstrapMethods attribute>";
                    } catch (IndexOutOfBoundsException ioob) {
                        return "<Invalid bootstrap method index:" + bsm_attr_idx + ">";
                    }
                    StringBuilder bsm_args_str = new StringBuilder();
                    String offsetParm,offsetBrace;
                    int bsm_ref = bsmData.bsm_index;
                    int bsm_args_len = bsmData.bsm_args_indexes.size();
                    if (bsm_args_len > 0) {
                        bsm_args_str.append(" {\n");
                        offsetBrace = indent.get();
                        indent.inc();
                        offsetParm = indent.get();
                        for (int i = 0; i < bsm_args_len; i++) {
                            int bsm_arg_idx = bsmData.bsm_args_indexes.get(i);
                            Constant cnt = pool.get(bsm_arg_idx);
                            if (cnt.equals(this)) {
                                String s = "circular reference to " + cnt.tag.tagname() + " #" + bsm_arg_idx;
                                bsm_args_str.append(offsetParm).append("  <").append(s).append(">");
                                cnt.setIssue(new IOException(s));
                            } else {
                                bsm_args_str.append(offsetParm).append(ConstantStrValue(bsm_arg_idx));
                                if (i + 1 < bsm_args_len) {
                                    bsm_args_str.append(",");
                                }
                            }
                            bsm_args_str.append('\n');
                        }
                        indent.dec();
                        bsm_args_str.append(offsetBrace).append("}");
                    }
                    str = StringValue(bsm_ref) + ":" + StringValue(nape_idx) + bsm_args_str.toString();
                default:
                    break;
            }
            return str;
        }



        @Override
        public void print(PrintWriter out) {
            super.print(out);
            switch (tag) {
                case CONSTANT_FIELD:
                case CONSTANT_METHOD:
                case CONSTANT_INTERFACEMETHOD:
                    out.println("#" + value1 + ".#" + value2 + ";\t//  " + stringVal());
                    break;
                case CONSTANT_METHODHANDLE:
                    out.println(value1 + ":#" + value2 + ";\t//  " + stringVal());
                    break;
                case CONSTANT_NAMEANDTYPE:
                    out.println("#" + value1 + ":#" + value2 + ";\t//  " + stringVal());
                    break;
                case CONSTANT_DYNAMIC:
                case CONSTANT_INVOKEDYNAMIC:
                    out.println(value1 + ":#" + value2 + ";\t" + commentString(stringVal()));
                    break;
                default:
                    break;
            }
        }

        public boolean refersClassMember() {
            return tag == TAG.CONSTANT_FIELD || tag == TAG.CONSTANT_METHOD || tag == TAG.CONSTANT_INTERFACEMETHOD;
        }
    }

    /* -------------------------------------------------------- */
    /* ConstantPool Fields */
    /**
     * The actual pool of Constants
     */
    public ArrayList<Constant> pool;
    /**
     * Reference to the class data
     */
    private ClassData cd;


    /* -------------------------------------------------------- */
    /* ConstantPool Methods */

    /* ConstantPool Constructors */
    public ConstantPool(ClassData cd) {
        pool = null;
        this.cd = cd;
    }

    public ConstantPool(ClassData cd, int size) {
        pool = new ArrayList<>(size);
        this.cd = cd;
    }

    /**
     *
     * read
     *
     * decodes a ConstantPool and it's constants from a data stream.
     *
     */
    void read(DataInputStream in) throws IOException {
        int length = in.readUnsignedShort();
        pool = new ArrayList<>(length);
        pool.add(0, null);
        TraceUtils.traceln("CP len=" + length);
        for (int i = 1; i < length; i++) {
            byte tag = in.readByte();
            TAG tagobj = taghash.get(tag);
            TraceUtils.traceln("CP entry #" + i + " + tagindex=" + tag + " tag=" + tagobj);
            switch (tagobj) {
                case CONSTANT_UTF8:
                    pool.add(i, new CP_Str(tagobj, in.readUTF()));
                    break;
                case CONSTANT_INTEGER:
                    pool.add(i, new CP_Int(tagobj, in.readInt()));
                    break;
                case CONSTANT_LONG:
                    pool.add(i, new CP_Long(tagobj, in.readLong()));
                    // handle null entry to account for Longs taking up 2 CP slots
                    i += 1;
                    pool.add(null);
                    break;
                case CONSTANT_FLOAT:
                    pool.add(i, new CP_Float(tagobj, in.readFloat()));
                    break;
                case CONSTANT_DOUBLE:
                    pool.add(i, new CP_Double(tagobj, in.readDouble()));
                    // handle null entry to account for Doubles taking up 2 CP slots
                    i += 1;
                    pool.add(null);
                    break;
                case CONSTANT_CLASS:
                case CONSTANT_STRING:
                case CONSTANT_METHODTYPE:
                case CONSTANT_PACKAGE:
                case CONSTANT_MODULE:
                    pool.add(i, new CPX(tagobj, in.readUnsignedShort()));
                    break;
                case CONSTANT_FIELD:
                case CONSTANT_METHOD:
                case CONSTANT_INTERFACEMETHOD:
                case CONSTANT_NAMEANDTYPE:
                case CONSTANT_DYNAMIC:
                case CONSTANT_INVOKEDYNAMIC:
                    pool.add(i, new CPX2(tagobj, in.readUnsignedShort(), in.readUnsignedShort()));
                    break;
                case CONSTANT_METHODHANDLE:
                    pool.add(i, new CPX2(tagobj, in.readUnsignedByte(), in.readUnsignedShort()));
                    break;

                default:
                    throw new ClassFormatError("invalid constant type: " + (int) tag);
            }
        }
    }

    /**
     *
     * inbounds
     *
     * bounds-check a CP index.
     *
     */
    private boolean inbounds(int cpx) {
        return !(cpx == 0 || cpx >= pool.size());
    }

    /**
     *
     * getConst
     *
     * Public getter - Safely gets a Constant from the CP at a given index.
     *
     */
    public Constant getConst(int cpx) {
        if (inbounds(cpx)) {
            return pool.get(cpx);
        } else {
            return null;
        }
    }

    /**
     *
     * StringTag
     *
     * Public string val - Safely gets the string-rep of a Constant from the CP at a given
     * index.
     *
     */
    public String StringTag(int cpx) {
        String str = "Incorrect CP index:" + cpx;
        if (inbounds(cpx)) {
            Constant cns = pool.get(cpx);
            if (cns != null) {
                str = cns.tag.tagname;
            }
        }
        return str;
    }

    /**
     *
     * getString
     *
     * Public string val - Safely gets the string-rep of a ConstantUTF8 from the CP at a
     * given index.
     *
     * Returns either null (if invalid), or the string value of the UTF8
     *
     */
    public String getString(int cpx) {
        String str = null;
        if (inbounds(cpx)) {
            Constant cns = pool.get(cpx);
            if (cns != null && cns.tag == TAG.CONSTANT_UTF8) {
                CP_Str cns1 = (CP_Str) cns;
                str = cns1.value;
            }
        }
        return str;
    }

    /**
     *
     * getModule
     *
     * Public string val - Safely gets the string-rep of a ConstantModule from the CP at a
     * given index.
     *
     * Returns either null (if invalid), or the string value of the ConstantModule
     *
     */
    public String getModule(int cpx) {
        String str = null;
        if (inbounds(cpx)) {
            Constant cns = pool.get(cpx);
            if (cns != null && cns.tag == TAG.CONSTANT_MODULE) {
                str = cns.stringVal();
            }
        }
        return str;
    }

    /**
     *
     * getPackage
     *
     * Public string val - Safely gets the string-rep of a ConstantPackage from the CP at a
     * given index.
     *
     * Returns either null (if invalid), or the string value of the ConstantPackage
     *
     */
    public String getPackage(int cpx) {
        String str = null;
        if (inbounds(cpx)) {
            Constant cns = pool.get(cpx);
            if (cns != null && cns.tag == TAG.CONSTANT_PACKAGE) {
                str = cns.stringVal();
            }
        }
        return str;
    }

    /**
     *
     * getTypeName
     *
     * Safely gets a Java name from a ConstantUTF8 from the CP at a given index.
     *
     * Returns either null (if invalid), or the Java name value of the UTF8
     *
     */
    public String getName(int cpx) {
        String str = getString(cpx);
        if (str == null) {
            return "<invalid constant pool index:" + cpx + ">";
        }

        return Utils.javaName(str);
    }

    /**
     *
     * getClassName
     *
     * Safely gets a Java class name from a ConstantClass from the CP at a given index.
     *
     * Returns either the Java class name, or a CP index reference string.
     *
     */
    public String getClassName(int cpx) {
        String res = "#" + cpx;
        if (cpx == 0) {
            return res;
        }
        if (!inbounds(cpx)) {
            return res;
        }
        Constant cns = pool.get(cpx);
        if (cns == null || cns.tag != TAG.CONSTANT_CLASS) {
            return res;
        }

        return getClassName((CPX) cns);
    }

    /**
     *
     * getClassName
     *
     * Safely gets a Java class name from a ConstantClass from a CPX2 constant pool
     * object. (eg. Method/Field/Interface Ref)
     *
     * Returns either the Java class name, or a CP index reference string.
     *
     */
    public String getClassName(CPX2 classConst) {
        return _getClassName(classConst.value1);
    }

    /**
     *
     * getClassName
     *
     * Safely gets a Java class name from a ConstantClass from a CPX constant pool object.
     * (eg. Class Ref)
     *
     * Returns either the Java class name, or a CP index reference string.
     *
     */
    public String getClassName(CPX classConst) {
        return _getClassName(classConst.value);
    }

    /**
     *
     * _getClassName
     *
     * Helper for getting class name. Checks bounds, does name conversion.
     *
     */
    private String _getClassName(int nameIndex) {
        String res = "#" + nameIndex;
        if (!inbounds(nameIndex)) {
            return res;
        }
        Constant nameconst = pool.get(nameIndex);
        if (nameconst == null || nameconst.tag != TAG.CONSTANT_UTF8) {
            return res;
        }
        CP_Str name = (CP_Str) nameconst;

        String classname = name.value;

        if (Utils.isClassArrayDescriptor(classname)) {
            classname = "\"" + classname + "\"";
        }
        return classname;
    }

    /**
     *
     * getShortClassName
     *
     * shortens a class name (if the class is in the given package). works with a
     * string-encoded classname.
     *
     */
    public String getShortClassName(String className, String pkgPrefix) {
        if (className.startsWith(pkgPrefix)) {
            return className.substring(pkgPrefix.length());
        }
        return className;
    }

    /**
     *
     * getShortClassName
     *
     * shortens a class name (if the class is in the given package). works with a CP index
     * to a ConstantClass.
     *
     */
    public String getShortClassName(int cpx, String pkgPrefix) {
        String name = Utils.javaName(getClassName(cpx));
        return getShortClassName(name, pkgPrefix);
    }

    /**
     *
     * decodeClassDescriptor
     *
     * Pulls the class name out of a string (at the CP index). (drops any array
     * descriptors, and the class descriptors ("L" and ";")
     *
     */
    public String decodeClassDescriptor(int cpx) {
        // enum type is encoded as a descriptor
        // need to remove '"'s and L (class descriptor)

        // TODO: might have to count '['s at the beginning for Arrays
        String rawEnumName = getName(cpx);
        int len = rawEnumName.length();
        int begin = (rawEnumName.startsWith("\"L")) ? 2 : 0;
        int end = (begin > 0) ? len - 2 : len;
        return rawEnumName.substring(begin, end);
    }

    /**
     *
     * subtagToString
     *
     * Getter that safely gets the string descriptor of a subtag
     *
     */
    private String subtagToString(int subtag) {
        SUBTAG st = subtaghash.get((byte) subtag);
        if (st == null) {
            return "BOGUS_SUBTAG:" + subtag;
        }
        return st.tagname;
    }

    /**
     *
     * StringValue
     *
     * Safely gets the string value of any Constant at any CP index.
     *
     */
    public String StringValue(int cpx) {
        if (cpx == 0) {
            return "#0";
        }
        if (!inbounds(cpx)) {
            return "<Incorrect CP index:" + cpx + ">";
        }
        Constant cnst = pool.get(cpx);
        if (cnst == null) {
            return "<NULL>";
        }
        return cnst.stringVal();
    }

    /**
     * ConstantStrValue
     *
     * Safely gets the string value of any Constant at any CP index. This string is either
     * a Constant's String value, or a CP index reference string. The Constant string has
     * a tag descriptor in the beginning.
     *
     */
    public String ConstantStrValue(int cpx) {
        if (cpx == 0) {
            return "#0";
        }
        if (!inbounds(cpx)) {
            return "#" + cpx;
        }
        Constant cns = pool.get(cpx);
        if (cns == null) {
            return "#" + cpx;
        }
        if (cns instanceof CPX2) {
            CPX2 cns2 = (CPX2) cns;
            if (cns2.value1 == cd.this_cpx && cns2.refersClassMember()) {
                cpx = cns2.value2;
            }
        }
        return cns.tag.tagname + " " + StringValue(cpx);
    }

    /**
     * prints the entire constant pool.
     */
    public void print(PrintWriter out) throws IOException {
        int cpx = 0;
        for (Constant cns : pool) {
            if (cpx == 0) {
                cpx += 1;
                continue;
            }

            out.print("\tconst #" + cpx + " = ");

            if (cns == null) {
                // do something
                out.println("null");
                cpx += 1;
            } else {
                cns.print(out);
                cpx += cns.size();
            }
        }
    }

    /**
     * prints the Constant value at a given CP index.
     */
    void PrintConstant(PrintWriter out, int cpx) {
        out.print(ConstantStrValue(cpx));
    }

    /**
     * prints a constant value, with the print format based on the print options.
     */
    public void printlnClassId(PrintWriter out, int cpx) throws IOException {
        printlnClassId(out, cpx, false);
    }

    public void printlnClassId(PrintWriter out, int cpx, boolean addComma) throws IOException {
        if (!cd.options.contains(Options.PR.CPX)) {
            out.print(getShortClassName(cpx, cd.pkgPrefix) + (addComma ? "," : ""));
        } else {
            out.print("\t#" + cpx + (addComma ? "," : "") + " //");
            PrintConstant(out, cpx);
        }
    }

}
