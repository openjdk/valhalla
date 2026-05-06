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

/**
 * Type annotation types: target_type, target_info &amp;&amp; target_path
 */
public class TypeAnnotationTypes {

    /**
     * Interpretation of type_path_kind values (Table 4.7.20.2-A)
     */
    public enum EPathKind {
        ARRAY(0),
        INNER_TYPE(1),
        WILDCARD(2),
        TYPE_ARGUMENT(3);

        private final int tag;
        public static final int maxLen = 3;

        EPathKind(int tag) {
            this.tag = tag;
        }

        public int tag() {
            return tag;
        }

        public String parseKey() {
            return this.toString();
        }

        static EPathKind getPathKind(String token) {
            for (EPathKind pk : values()) {
                if( pk.parseKey().equals(token)) {
                    return pk;
                }
            }
            return null;
        }
    }

    // will throw ArrayIndexOutOfBounds if i < 0 or i > 3
    static public EPathKind getPathKind(int i) {
        return EPathKind.values()[i];
    }

    static public class TypePathEntry {

        private final EPathKind kind;
        private final int typeArgumentIndex;

        public TypePathEntry(int kind, int typeArgumentIndex) {
            this.kind = getPathKind(kind);
            this.typeArgumentIndex = typeArgumentIndex;
        }

        public TypePathEntry(EPathKind kind, int typeArgumentIndex) {
            this.kind = kind;
            this.typeArgumentIndex = typeArgumentIndex;
        }

        public int getTypePathKind() {
            return kind.tag();
        }

        public int getTypeArgumentIndex() {
            return typeArgumentIndex;
        }

        @Override
        public String toString() {
            // Chapter 4.7.20.2 The type_path structure
            // if the value of the type_path_kind is 0,1, or 2, thebn the value of the
            // type_argument_index item is 0.
            return kind.parseKey() +  ( kind.tag == 3 ?
                    JasmTokens.Token.LBRACE.parseKey() + typeArgumentIndex + JasmTokens.Token.RBRACE.parseKey() :
                    "");
        }
    }

    /**
     *     union {
     *         type_parameter_target;
     *         supertype_target;
     *         type_parameter_bound_target;
     *         empty_target;
     *         method_formal_parameter_target;
     *         throws_target;
     *         localvar_target;
     *         catch_target;
     *         offset_target;
     *         type_argument_target;
     *     } target_info;
     */
    public enum ETargetInfo {
        TYPEPARAM           ("TYPEPARAM", "type_parameter"),
        SUPERTYPE           ("SUPERTYPE", "supertype"),
        TYPEPARAM_BOUND     ("TYPEPARAM_BOUND", "type_parameter_bound"),
        EMPTY               ("EMPTY", "empty"),
        METHODPARAM         ("METHODPARAM", "formal_parameter"),
        EXCEPTION           ("EXCEPTION", "throws"),
        LOCALVAR            ("LOCALVAR", "localvar"),
        CATCH               ("CATCH", "catch"),
        OFFSET              ("OFFSET", "offset"),
        TYPEARG             ("TYPEARG", "type_argument");

        private final String parseKey;
        private final String printValue;

        ETargetInfo(String parse, String printValue) {
            parseKey = parse;
            this.printValue = printValue;
        }
        public String parseKey() {
            return this.parseKey;
        }

        public String printValue() {
            return this.printValue;
        }
    }

    /**
     *  Interpretation of target_type values (Table 4.7.20-A./B.)
     */
    static public enum ETargetType {
        class_type_param            (0x00, "CLASS_TYPE_PARAMETER",  ETargetInfo.TYPEPARAM, "class/interface type parameter"),
        meth_type_param             (0x01, "METHOD_TYPE_PARAMETER",  ETargetInfo.TYPEPARAM, "method/constructor type parameter"),
        class_exts_impls            (0x10, "CLASS_EXTENDS",  ETargetInfo.SUPERTYPE, "class extends/implements"),
        class_type_param_bnds       (0x11, "CLASS_TYPE_PARAMETER_BOUND",  ETargetInfo.TYPEPARAM_BOUND, "class/interface type parameter bounds"),
        meth_type_param_bnds        (0x12, "METHOD_TYPE_PARAMETER_BOUND",  ETargetInfo.TYPEPARAM_BOUND, "method/constructor type parameter bounds"),
        field                       (0x13, "FIELD",  ETargetInfo.EMPTY, "field"),
        meth_ret_type               (0x14, "METHOD_RETURN",  ETargetInfo.EMPTY, "method return type"),
        meth_receiver               (0x15, "METHOD_RECEIVER",  ETargetInfo.EMPTY, "method receiver"),
        meth_formal_param           (0x16, "METHOD_FORMAL_PARAMETER",  ETargetInfo.METHODPARAM, "method formal parameter type"),
        throws_type                 (0x17, "THROWS",  ETargetInfo.EXCEPTION, "exception type in throws"),

        local_var                   (0x40, "LOCAL_VARIABLE",  ETargetInfo.LOCALVAR, "local variable"),
        resource_var                (0x41, "RESOURCE_VARIABLE",  ETargetInfo.LOCALVAR, "resource variable"),
        exception_param             (0x42, "EXCEPTION_PARAM",  ETargetInfo.CATCH, "exception parameter"),
        type_test                   (0x43, "INSTANCEOF",  ETargetInfo.OFFSET, "type test (instanceof)"),
        obj_creat                   (0x44, "NEW",  ETargetInfo.OFFSET, "object creation (new)"),
        constr_ref_receiver         (0x45, "CONSTRUCTOR_REFERENCE_RECEIVER", ETargetInfo.OFFSET, "constructor reference receiver"),
        meth_ref_receiver           (0x46, "METHOD_REFERENCE_RECEIVER", ETargetInfo.OFFSET, "method reference receiver"),
        cast                        (0x47, "CAST",  ETargetInfo.TYPEARG, "cast"),
        constr_invoc_typearg        (0x48, "CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT",  ETargetInfo.TYPEARG, "type argument in constructor call"),
        meth_invoc_typearg          (0x49, "METHOD_INVOCATION_TYPE_ARGUMENT", ETargetInfo.TYPEARG, "type argument in method call"),
        constr_ref_typearg          (0x4A, "CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT", ETargetInfo.TYPEARG, "type argument in constructor reference"),
        meth_ref_typearg            (0x4B, "METHOD_REFERENCE_TYPE_ARGUMENT",  ETargetInfo.TYPEARG, "type argument in method reference");

        public static final int maxTag = 0x9A;
        public static final int maxLen = 36;

        public final int value;
        private final String parseKey;
        private final ETargetInfo targetInfo;
        private final String printVal;

        ETargetType(int val, String parse, ETargetInfo targetInfo, String printVal) {
            value = val;
            parseKey = parse;
            this.targetInfo = targetInfo;
            this.printVal = printVal;
        }

        public String parseKey() {
            return parseKey;
        }

        public String infoKey() {
            return targetInfo.parseKey();
        }

        public ETargetInfo targetInfo() {
            return targetInfo;
        }

        public void print(PrintWriter out) {
            out.print(parseKey);
        }

        @Override
        public String toString() {
            return String.format("%s[%#x]", parseKey, value);
        }

        public static ETargetType getTargetType(int typeCode)  {
            for( ETargetType type: ETargetType.values() ) {
                if (type.value == typeCode) {
                    return type;
                }
            }
            return null;
        }

        static public ETargetType getTargetType(String typeName) {
            for( ETargetType type: ETargetType.values() ) {
                if (type.parseKey.equals(typeName)) {
                    return type;
                }
            }
            return null;
        }
    };

    /* TypeAnnotationVisitor Methods */
    public static class TypeAnnotationTargetVisitor {

        public final void visit(ETargetType tt) {
            switch (tt) {
                case class_type_param:
                case meth_type_param:
                    visit_type_param_target(tt);
                    break;
                case class_exts_impls:
                    visit_supertype_target(tt);
                    break;
                case class_type_param_bnds:
                case meth_type_param_bnds:
                    visit_typeparam_bound_target(tt);
                    break;
                case field:
                case meth_ret_type:
                case meth_receiver:
                    visit_empty_target(tt);
                    break;
                case meth_formal_param:
                    visit_methodformalparam_target(tt);
                    break;
                case throws_type:
                    visit_throws_target(tt);
                    break;
                case local_var:
                case resource_var:
                    visit_localvar_target(tt);
                    break;
                case exception_param:
                    visit_catch_target(tt);
                    break;
                case type_test:
                case obj_creat:
                case constr_ref_receiver:
                case meth_ref_receiver:
                    visit_offset_target(tt);
                    break;

                case cast:
                case constr_invoc_typearg:
                case meth_invoc_typearg:
                case constr_ref_typearg:
                case meth_ref_typearg:

                    visit_typearg_target(tt);
                    break;
            }
        }

        public void visit_type_param_target(ETargetType tt) {
        }

        public void visit_supertype_target(ETargetType tt) {
        }

        public void visit_typeparam_bound_target(ETargetType tt) {
        }

        public void visit_empty_target(ETargetType tt) {
        }

        public void visit_methodformalparam_target(ETargetType tt) {
        }

        public void visit_throws_target(ETargetType tt) {
        }

        public void visit_localvar_target(ETargetType tt) {
        }

        public void visit_catch_target(ETargetType tt) {
        }

        public void visit_offset_target(ETargetType tt) {
        }

        public void visit_typearg_target(ETargetType tt) {
        }
    }
}
