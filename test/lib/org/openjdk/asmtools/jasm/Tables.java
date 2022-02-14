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
package org.openjdk.asmtools.jasm;

import java.io.PrintWriter;
import java.util.HashMap;

/**
 *
 * Tables
 *
 * The classes in Tables are following a Singleton Pattern. These classes are Enums, and
 * they are contained in private hash maps (lookup tables and reverse lookup tables).
 * These hash maps all have public accessors, which clients use to look-up enums.
 *
 * Tokens in this table carry no external state, and are typically treated as constants.
 * They do not need to be reset.
 *
 */
public class Tables {

    public static final int JAVA_MAGIC = 0xCAFEBABE;
    /**
     * Lookup-tables for various types.
     */
    private static HashMap<String, AttrTag> NameToAttrTag = new HashMap<>(9);
    private static HashMap<Integer, AttrTag> AttrTags = new HashMap<>(9);

    private static HashMap<String, SubTag> NameToSubTag = new HashMap<>(9);
    private static HashMap<Integer, SubTag> SubTags = new HashMap<>(9);

    private static HashMap<String, BasicType> NameToBasicType = new HashMap<>(10);
    private static HashMap<Integer, BasicType> BasicTypes = new HashMap<>(10);

    private static HashMap<String, AnnotElemType> NameToAnnotElemType = new HashMap<>(10);
    private static HashMap<Character, AnnotElemType> AnnotElemTypes = new HashMap<>(10);

    private static HashMap<String, StackMapType> KeyToStackMapType = new HashMap<>(10);
    private static HashMap<String, StackMapType> NameToStackMapType = new HashMap<>(10);
    private static HashMap<Integer, StackMapType> StackMapTypes = new HashMap<>(10);

    private static HashMap<String, StackMapFrameType> NameToStackMapFrameType = new HashMap<>(10);
    private static HashMap<Integer, StackMapFrameType> StackMapFrameTypes = new HashMap<>(10);

    private static HashMap<String, ConstType> NameToConstantType = new HashMap<>(ConstType.maxTag);
    private static HashMap<Integer, ConstType> ConstantTypes = new HashMap<>(ConstType.maxTag);

    static {
        // register all of the tokens
        for (ConstType ct : ConstType.values()) {
            registerConstantType(ct);
        }

        /* Type codes for SubTags */
        for (AttrTag at : AttrTag.values()) {
            registerAttrtag(at);
        }

        /* Type codes for SubTags */
        for (SubTag st : SubTag.values()) {
            registerSubtag(st);
        }

        /* Type codes for BasicTypes */
        for (BasicType bt : BasicType.values()) {
            registerBasicType(bt);
        }

        /* Type codes for BasicTypes */
        for (AnnotElemType aet : AnnotElemType.values()) {
            registerAnnotElemType(aet);
        }

        /* Type codes for StackMapTypes */
        for (StackMapType smt : StackMapType.values()) {
            registerStackMapType(smt);
        }

        /* Type codes for StackMapFrame attribute */
        for (StackMapFrameType smft : StackMapFrameType.values()) {
            registerStackMapFrameType(smft);
        }

    }

    /**
     * ConstType
     *
     * A (typed) tag (constant) representing the type of Constant in the Constant Pool.
     */
    static public enum ConstType {
        CONSTANT_ZERO                       (-3, "CONSTANT_ZERO", ""),
        CONSTANT_UTF8                       (1, "CONSTANT_UTF8", "Asciz"),
        CONSTANT_UNICODE                    (2, "CONSTANT_UNICODE", ""),
        CONSTANT_INTEGER                    (3, "CONSTANT_INTEGER", "int"),
        CONSTANT_FLOAT                      (4, "CONSTANT_FLOAT", "float"),
        CONSTANT_LONG                       (5, "CONSTANT_LONG", "long"),
        CONSTANT_DOUBLE                     (6, "CONSTANT_DOUBLE", "double"),
        CONSTANT_CLASS                      (7, "CONSTANT_CLASS", "class"),
        CONSTANT_STRING                     (8, "CONSTANT_STRING", "String"),
        CONSTANT_FIELD                      (9, "CONSTANT_FIELD", "Field"),
        CONSTANT_METHOD                     (10, "CONSTANT_METHOD", "Method"),
        CONSTANT_INTERFACEMETHOD            (11, "CONSTANT_INTERFACEMETHOD", "InterfaceMethod"),
        CONSTANT_NAMEANDTYPE                (12, "CONSTANT_NAMEANDTYPE", "NameAndType"),
        // Constant 13 reserved
        // Constant 14 reserved
        CONSTANT_METHODHANDLE               (15, "CONSTANT_METHODHANDLE", "MethodHandle"),
        CONSTANT_METHODTYPE                 (16, "CONSTANT_METHODTYPE", "MethodType"),
        CONSTANT_DYNAMIC                    (17, "CONSTANT_DYNAMIC", "Dynamic"),
        CONSTANT_INVOKEDYNAMIC              (18, "CONSTANT_INVOKEDYNAMIC", "InvokeDynamic"),
        CONSTANT_MODULE                     (19, "CONSTANT_MODULE",        "Module"),
        CONSTANT_PACKAGE                    (20, "CONSTANT_PACKAGE",       "Package");

        static final public int maxTag = 20;

        private final int value;
        private final String parseKey;
        private final String printval;

        ConstType(int val, String print, String parse) {
            value = val;
            parseKey = parse;
            printval = print;
        }

        public int value() {
            return value;
        }

        public String parseKey() {
            return parseKey;
        }

        public String printval() {
            return printval;
        }

        public void print(PrintWriter out) {
            out.print(parseKey);
        }

        @Override
        public String toString() {
            return "<" + printval + "> [" + Integer.toString(value) + "]";
        }
    };

    static public ConstType tag(int i) {
        return ConstantTypes.get(i);
    }

    static public ConstType tag(String parsekey) {
        return NameToConstantType.get(parsekey);
    }

    private static void registerConstantType(ConstType tt) {
        NameToConstantType.put(tt.parseKey, tt);
        ConstantTypes.put(tt.value, tt);
    }

    /**
     * Attribute descriptor enums
     */
    static public enum AttrTag {

        // Constant for ME Spec (StackMap does not appear in SE VM Spec)
        ATT_Unrecognized                            (0, "ATT_Unrecognized", ""),
        ATT_StackMap                                (1, "ATT_StackMap", "StackMap"),
        // Numbers corespond to VM spec (chapter 4.7.X)
        ATT_ConstantValue                           (2, "ATT_ConstantValue", "ConstantValue"),
        ATT_Code                                    (3, "ATT_Code", "Code"),
        ATT_StackMapTable                           (4, "ATT_StackMapTable", "StackMapTable"),
        ATT_Exceptions                              (5, "ATT_Exceptions", "Exceptions"),
        ATT_InnerClasses                            (6, "ATT_InnerClasses", "InnerClasses"),
        ATT_EnclosingMethod                         (7, "ATT_EnclosingMethod", "EnclosingMethod"),
        ATT_Synthetic                               (8, "ATT_Synthetic", "Synthetic"),
        ATT_Signature                               (9, "ATT_Signature", "Signature"),
        ATT_SourceFile                              (10, "ATT_SourceFile", "SourceFile"),
        ATT_SourceDebugExtension                    (11, "ATT_SourceDebugExtension", "SourceDebugExtension"),
        ATT_LineNumberTable                         (12, "ATT_LineNumberTable", "LineNumberTable"),
        ATT_LocalVariableTable                      (13, "ATT_LocalVariableTable", "LocalVariableTable"),
        ATT_LocalVariableTypeTable                  (14, "ATT_LocalVariableTypeTable", "LocalVariableTypeTable"),
        ATT_Deprecated                              (15, "ATT_Deprecated", "Deprecated"),
        ATT_RuntimeVisibleAnnotations               (16, "ATT_RuntimeVisibleAnnotations", "RuntimeVisibleAnnotations"),
        ATT_RuntimeInvisibleAnnotations             (17, "ATT_RuntimeInvisibleAnnotations", "RuntimeInvisibleAnnotations"),
        ATT_RuntimeVisibleParameterAnnotations      (18, "ATT_RuntimeVisibleParameterAnnotations", "RuntimeVisibleParameterAnnotations"),
        ATT_RuntimeInvisibleParameterAnnotations    (19, "ATT_RuntimeInvisibleParameterAnnotations", "RuntimeInvisibleParameterAnnotations"),
        ATT_AnnotationDefault                       (20, "ATT_AnnotationDefault", "AnnotationDefault"),
        ATT_BootstrapMethods                        (21, "ATT_BootstrapMethods", "BootstrapMethods"),
        ATT_RuntimeVisibleTypeAnnotations           (22, "ATT_RuntimeVisibleTypeAnnotations", "RuntimeVisibleTypeAnnotations"),
        ATT_RuntimeInvisibleTypeAnnotations         (23, "ATT_RuntimeInvisibleTypeAnnotations", "RuntimeInvisibleTypeAnnotations"),
        ATT_MethodParameters                        (24, "ATT_MethodParameters", "MethodParameters"),
        ATT_Module                                  (25, "ATT_Module",  "Module"),
        ATT_Version                                 (26, "ATT_Version", "Version"),
        ATT_TargetPlatform                          (27, "ATT_TargetPlatform", "TargetPlatform"),
        ATT_MainClass                               (28, "ATT_MainClass", "MainClass"),
        ATT_ModulePackages                          (29, "ATT_ModulePackages", "ModulePackages"),
        ATT_ModuleMainClass                         (30, "ATT_ModuleMainClass", "ModuleMainClass"),
        ATT_ModuleTarget                            (31, "ATT_ModuleTarget", "ModuleTarget"),
        // JEP 181: class file 55.0
        ATT_NestHost                                (32, "ATT_NestHost", "NestHost"),
        ATT_NestMembers                             (33, "ATT_NestMembers", "NestMembers"),
        //  JEP 359 Record(Preview): class file 58.65535
        //  Record_attribute {
        //    u2 attribute_name_index;
        //    u4 attribute_length;
        //    u2 components_count;
        //    component_info components[components_count];
        // }
        ATT_Record                                  (34, "ATT_Record", "Record"),
        // JEP 360 (Sealed types): class file 59.65535
        // PermittedSubclasses_attribute {
        //    u2 attribute_name_index;
        //    u4 attribute_length;
        //    u2 number_of_classes;
        //    u2 classes[number_of_classes];
        // }
        ATT_PermittedSubclasses                     (35, "ATT_PermittedSubclasses", "PermittedSubclasses"),
        ATT_Preload                                 (36, "ATT_Preload", "Preload");

        private final Integer value;
        private final String printval;
        private final String parsekey;

        AttrTag(Integer val, String print, String parse) {
            value = val;
            printval = print;
            parsekey = parse;
        }

        public String printval() {
            return printval;
        }

        public String parsekey() {
            return parsekey;
        }
    }

    private static void registerAttrtag(AttrTag tg) {
        NameToAttrTag.put(tg.parsekey, tg);
        AttrTags.put(tg.value, tg);
    }

    public static AttrTag attrtag(int val) {
        AttrTag tg = AttrTags.get(val);
        if (tg == null) {
            tg = AttrTag.ATT_Unrecognized;
        }
        return tg;
    }

    public static AttrTag attrtag(String idValue) {
        AttrTag tg = NameToAttrTag.get(idValue);
        if (tg == null) {
            tg = AttrTag.ATT_Unrecognized;
        }
        return tg;
    }

    public static String attrtagName(int subtag) {
        AttrTag tg = AttrTags.get(subtag);
        return tg.parsekey;
    }

    public static int attrtagValue(String idValue) {
        AttrTag tg = attrtag(idValue);
        return tg.value;
    }


    /*-------------------------------------------------------- */
    /**
     * SubTag enums
     */
    static public enum SubTag {
        REF_GETFIELD            (1, "REF_getField"),
        REF_GETSTATIC           (2, "REF_getStatic"),
        REF_PUTFIELD            (3, "REF_putField"),
        REF_PUTSTATIC           (4, "REF_putStatic"),
        REF_INVOKEVIRTUAL       (5, "REF_invokeVirtual"),
        REF_INVOKESTATIC        (6, "REF_invokeStatic"),
        REF_INVOKESPECIAL       (7, "REF_invokeSpecial"),
        REF_NEWINVOKESPECIAL    (8, "REF_newInvokeSpecial"),
        REF_INVOKEINTERFACE     (9, "REF_invokeInterface");

        private final Integer value;
        private final String printval;

        SubTag(Integer val, String print) {
            value = val;
            printval = print;
        }

        public String printval() {
            return printval;
        }

        public Integer value() {
            return value;
        }
    }

    private static void registerSubtag(SubTag tg) {
        NameToSubTag.put(tg.printval, tg);
        SubTags.put(tg.value, tg);
    }

    public static SubTag subtag(String subtag) {
        return NameToSubTag.get(subtag);
    }

    public static SubTag subtag(int subtag) {
        return SubTags.get(subtag);
    }

    public static String subtagName(int subtag) {
        String retval = null;
        SubTag tg = SubTags.get(subtag);
        if (tg != null) {
            retval = tg.printval;
        }
        return retval;
    }

    public static int subtagValue(String idValue) {
        int retval = 0;
        SubTag tg = NameToSubTag.get(idValue);
        if (tg != null) {
            retval = tg.value;
        }
        return retval;
    }

    /*-------------------------------------------------------- */
    /**
     * BasicType enums
     */
    static public enum BasicType {
        T_INT       (0x0000000a, "int"),
        T_LONG      (0x0000000b, "long"),
        T_FLOAT     (0x00000006, "float"),
        T_DOUBLE    (0x00000007, "double"),
        T_CLASS     (0x00000002, "class"),
        T_BOOLEAN   (0x00000004, "boolean"),
        T_CHAR      (0x00000005, "char"),
        T_BYTE      (0x00000008, "byte"),
        T_SHORT     (0x00000009, "short");

        private final Integer value;
        private final String printval;

        BasicType(Integer val, String print) {
            value = val;
            printval = print;
        }

        public String printval() {
            return printval;
        }
    }

    private static void registerBasicType(BasicType typ) {
        NameToBasicType.put(typ.printval, typ);
        BasicTypes.put(typ.value, typ);
    }

    public static BasicType basictype(String idValue) {
        return NameToBasicType.get(idValue);
    }

    public static BasicType basictype(int subtag) {
        return BasicTypes.get(subtag);
    }

    public static String basictypeName(int subtag) {
        String retval = null;
        BasicType tg = BasicTypes.get(subtag);
        if (tg != null) {
            retval = tg.printval;
        }
        return retval;
    }

    public static int basictypeValue(String idValue) {
        int retval = -1;
        BasicType tg = NameToBasicType.get(idValue);
        if (tg != null) {
            retval = tg.value;
        }
        return retval;
    }

    /*-------------------------------------------------------- */
    /**
     * AnnotElemType enums
     */
    static public enum AnnotElemType {

        AE_BYTE         ('B', "byte"),
        AE_CHAR         ('C', "char"),
        AE_SHORT        ('S', "short"),
        AE_INT          ('I', "int"),
        AE_LONG         ('J', "long"),
        AE_FLOAT        ('F', "float"),
        AE_DOUBLE       ('D', "double"),
        AE_BOOLEAN      ('Z', "boolean"),
        AE_STRING       ('s', "string"),
        AE_ENUM         ('e', "enum"),
        AE_CLASS        ('c', "class"),
        AE_ANNOTATION   ('@', "annotation"),
        AE_ARRAY        ('[', "array"),
        AE_UNKNOWN      ((char)0, "unknown");

        private char value;
        private final String printval;

        AnnotElemType(char val, String print) {
            value = val;
            printval = print;
        }

        public char val() {
            return value;
        }

        public String printval() {
            return printval;
        }
    }

    private static void registerAnnotElemType(AnnotElemType typ) {
        NameToAnnotElemType.put(typ.printval, typ);
        AnnotElemTypes.put(typ.value, typ);
    }

    public static AnnotElemType annotElemType(String idValue) {
        return NameToAnnotElemType.get(idValue);
    }

    public static AnnotElemType annotElemType(char subtag) {
        AnnotElemType type = AnnotElemTypes.get(subtag);
        if ( type == null ) {
            type = AnnotElemType.AE_UNKNOWN;
        }
        return type;
    }

    public static String annotElemTypeName(char subtag) {
        String retval = null;
        AnnotElemType tg = AnnotElemTypes.get(subtag);
        if (tg != null) {
            retval = tg.printval;
        }
        return retval;
    }

    public static char annotElemTypeVal(String idValue) {
        char retval = 0;
        AnnotElemType tg = NameToAnnotElemType.get(idValue);
        if (tg != null) {
            retval = tg.value;
        }
        return retval;
    }


    /*-------------------------------------------------------- */
    /**
     * MapTypes table. These constants are used in stackmap pseudo-instructions only.
     */
    static public enum StackMapType {
        /* Type codes for StackMap attribute */
        ITEM_Bogus      (0,     "bogus",    "B"),           // an unknown or uninitialized value
        ITEM_Integer    (1,     "int",      "I"),           // a 32-bit integer
        ITEM_Float      (2,     "float",    "F"),           // not used
        ITEM_Double     (3,     "double",   "D"),           // not used
        ITEM_Long       (4,     "long",     "L"),           // a 64-bit integer
        ITEM_Null       (5,     "null",     "N"),           // the type of null
        ITEM_InitObject (6,     "this",     "IO"),          // "this" in constructor
        ITEM_Object     (7,     "CP",       "O"),           // followed by 2-byte index of class name
        ITEM_NewObject  (8,     "at",       "NO"),          // followed by 2-byte ref to "new"
        ITEM_UNKNOWN    (null,  "UNKNOWN",  "UNKNOWN");     // placeholder for wrong types

        private Integer value;
        private final String printval;
        private final String parsekey;

        StackMapType(Integer val, String print, String parse) {
            value = val;
            printval = print;
            parsekey = parse;
        }

        public String parsekey() {
            return parsekey;
        }

        public String printval() {
            return printval;
        }

        public Integer value() {
            return value;
        }
    }

    private static void registerStackMapType(StackMapType typ) {
        KeyToStackMapType.put(typ.parsekey, typ);
        NameToStackMapType.put(typ.printval, typ);
        StackMapTypes.put(typ.value, typ);
    }

    public static StackMapType stackMapType(int subtag, PrintWriter out) {
        StackMapType type = StackMapTypes.get(subtag);
        if (type == null || type == StackMapType.ITEM_UNKNOWN) {
            if (out != null)
                out.println("// Unknown StackMap type " + subtag);
            type = StackMapType.ITEM_UNKNOWN;
            type.value = subtag;
        }
        return type;
    }

    public static StackMapType stackMapType(String subtag) {
        return NameToStackMapType.get(subtag);
    }

    public static StackMapType stackMapTypeKey(String subtag) {
        return KeyToStackMapType.get(subtag);
    }

    public static String stackMapTypeName(int subtag) {
        String retval = null;
        StackMapType tg = StackMapTypes.get(subtag);
        if (tg != null) {
            retval = tg.printval;
        }
        return retval;
    }

    public static int stackMapTypeValue(String idValue) {
        int retval = 0;
        StackMapType tg = NameToStackMapType.get(idValue);
        if (tg != null) {
            retval = tg.value;
        }
        return retval;
    }


    /*-------------------------------------------------------- */
    /**
     * StackMap-FrameType table. These constants are used in stackmap pseudo-instructions
     * only.
     */
    static public enum StackMapFrameType {
        /* Type codes for StackMapFrame attribute */
        SAME_FRAME                              (0, "same"),
        SAME_LOCALS_1_STACK_ITEM_FRAME          (64, "stack1"),
        SAME_LOCALS_1_STACK_ITEM_EXTENDED_FRAME (247, "stack1_ex"),
        CHOP_1_FRAME                            (250, "chop1"),
        CHOP_2_FRAME                            (249, "chop2"),
        CHOP_3_FRAME                            (248, "chop3"),
        SAME_FRAME_EX                           (251, "same_ex"),
        APPEND_FRAME                            (252, "append"),
        FULL_FRAME                              (255, "full");

        private final Integer value;
        private final String parsekey;

        StackMapFrameType(Integer val, String print) {
            value = val;
            parsekey = print;
        }

        public String parsekey() {
            return parsekey;
        }

        public Integer value() {
            return value;
        }
    }

    private static void registerStackMapFrameType(StackMapFrameType typ) {
        NameToStackMapFrameType.put(typ.parsekey, typ);
        StackMapFrameTypes.put(typ.value, typ);
    }

    public static StackMapFrameType stackMapFrameTypeVal(int subtag) {
        return StackMapFrameTypes.get(subtag);
    }

    public static String stackMapFrameTypeName(int subtag) {
        String retval = null;
        StackMapFrameType tg = StackMapFrameTypes.get(subtag);
        if (tg != null) {
            retval = tg.parsekey;
        }
        return retval;
    }

    public static StackMapFrameType stackMapFrameType(int subtag) {
        StackMapFrameType frametype;
        if (subtag < StackMapFrameType.SAME_LOCALS_1_STACK_ITEM_FRAME.value()) {
            // type is same_frame;
            frametype = StackMapFrameType.SAME_FRAME;
        } else if (subtag >= StackMapFrameType.SAME_LOCALS_1_STACK_ITEM_FRAME.value()
                && subtag <= 127) {
            // type is same_locals_1_stack_item_frame
            frametype = StackMapFrameType.SAME_LOCALS_1_STACK_ITEM_FRAME;

        } else if (subtag >= StackMapFrameType.APPEND_FRAME.value()
                && subtag < StackMapFrameType.FULL_FRAME.value()) {
            // type is append_frame
            frametype = StackMapFrameType.APPEND_FRAME;
        } else {
            frametype = StackMapFrameTypes.get(subtag);
        }
        return frametype;
    }

    public static int stackMapFrameTypeValue(String idValue) {
        int retval = 0;
        StackMapFrameType tg = NameToStackMapFrameType.get(idValue);
        if (tg != null) {
            retval = tg.value;
        }
        return retval;
    }

    /**
     * CF_Context enums
     */
    public enum CF_Context {

        CTX_CLASS       (0, "class"),
        CTX_FIELD       (1, "field"),
        CTX_METHOD      (2, "method"),
        CTX_INNERCLASS  (3, "inner-class"),
        CTX_MODULE      (4, "module") ;

        private final int value;
        private final String printval;

        CF_Context(int val, String print) {
            value = val;
            printval = print;
        }

        boolean isOneOf(CF_Context... items) {
            for(CF_Context item : items) {
                if(item.value == value) {
                    return true;
                }
            }
            return false;
        }

        public int val() {
            return value;
        }

        public String printval() {
            return printval;
        }
    }
}
