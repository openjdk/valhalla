/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * TargetInfo (4.7.20.1. The target_info union)
 *
 * BaseClass for any Type Annotation Target-Info.
 */
public abstract class TypeAnnotationTargetInfoData implements Data {

    protected TypeAnnotationTypes.ETargetType targettype = null;

    public TypeAnnotationTargetInfoData(TypeAnnotationTypes.ETargetType tt) {
        targettype = tt;
    }

    public TypeAnnotationTypes.ETargetType getTargetType() {
        return targettype;
    }

    public void print(PrintWriter out, String tab) {
        // print the TargetType and TargetInfo
        out.print(tab + " {");
        targettype.print(out);
        _print(out, tab);
        out.print(tab + "} ");
    }

    public abstract void _print(PrintWriter out, String tab);

    public abstract void write(CheckedDataOutputStream out) throws IOException;

    @Override
    public String toString() {
        return toString(0);
    }

    protected abstract void _toString(StringBuilder sb, int tabLevel);

    public  String toString(int tabLevel)  {
        StringBuilder sb = new StringBuilder(tabString(tabLevel));
        // first print the target info name (
        sb.append(targettype.targetInfo().printValue()).append("_target ");
        // get the sub-classes parts
        _toString(sb, tabLevel);
        return sb.toString();
    }

    /**
     * type_parameter_target (4.7.20.1. The target_info union)
     *
     * The type_parameter_target item indicates that an annotation appears on the declaration of the i'th type parameter
     * of a generic class, generic interface, generic method, or generic constructor.
     *
     * type_parameter_target {
     *     u1 type_parameter_index;
     * }
     */
    public static class type_parameter_target extends TypeAnnotationTargetInfoData {

        int typeParamIndex;

        public type_parameter_target(TypeAnnotationTypes.ETargetType tt, int index) {
            super(tt);
            typeParamIndex = index;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeByte(typeParamIndex);
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            out.print(" ");
            out.print(typeParamIndex);
        }

        @Override
        public int getLength() {
            return 1;
        }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            sb.append(tabString(tabLevel)).append(String.format("{ type_parameter_index: %d; }",typeParamIndex));
        }
    }

    /**
     * supertype_target (4.7.20.1. The target_info union)
     *
     * The supertype_target item indicates that an annotation appears on a type in the extends or implements clause of
     * a class or interface declaration.
     *
     * supertype_target {
     *     u2 supertype_index;
     * }
     */
    public static class supertype_target extends TypeAnnotationTargetInfoData {

        int superTypeIndex;

        public supertype_target(TypeAnnotationTypes.ETargetType tt, int index) {
            super(tt);
            superTypeIndex = index;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeShort(superTypeIndex);
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            out.print(" ");
            out.print(superTypeIndex);
        }

        @Override
        public int getLength() {
            return 2;
        }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            sb.append(tabString(tabLevel)).append(String.format("{ supertype_index: %d; }",superTypeIndex));
        }
    }

    /**
     * type_parameter_bound_target (4.7.20.1. The target_info union)
     *
     * The type_parameter_bound_target item indicates that an annotation appears on the i'th bound of the j'th type parameter
     * declaration of a generic class, interface, method, or constructor.
     *
     * type_parameter_bound_target {
     *     u1 type_parameter_index;
     *     u1 bound_index;
     * }
     */
    public static class type_parameter_bound_target extends TypeAnnotationTargetInfoData {

        int typeParamIndex;
        int boundIndex;

        public type_parameter_bound_target(TypeAnnotationTypes.ETargetType tt, int pindx, int bindx) {
            super(tt);
            typeParamIndex = pindx;
            boundIndex = bindx;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeByte(typeParamIndex);
            out.writeByte(boundIndex);
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            out.print(" ");
            out.print(typeParamIndex);
            out.print(" ");
            out.print(boundIndex);
        }

        @Override
        public int getLength() {
            return 2;
        }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            sb.append(tabString(tabLevel)).append(String.format("{ type_parameter_index: %d; bound_index: %d; }",
                    typeParamIndex, boundIndex));
        }
    }

    /**
     * empty_target (4.7.20.1. The target_info union)
     *
     * The empty_target item indicates that an annotation appears on either the type in a field declaration,
     * the return type of a method, the type of a newly constructed object, or the receiver type of a method or constructor.
     *
     * empty_target {
     * }
     */
    public static class empty_target extends TypeAnnotationTargetInfoData {

        public empty_target(TypeAnnotationTypes.ETargetType tt) {
            super(tt);
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            // do nothing
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            // do nothing
        }

        @Override
        public int getLength() { return 0; }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            sb.append(tabString(tabLevel)).append("{ }");
        }
    }

    /**
     * formal_parameter_target (4.7.20.1. The target_info union)
     *
     * The formal_parameter_target item indicates that an annotation appears on the type in a formal parameter
     * declaration of a method, constructor, or lambda expression.
     *
     * formal_parameter_target {
     *     u1 formal_parameter_index;
     * }
     */
    public static class formal_parameter_target extends TypeAnnotationTargetInfoData {

        int formalParamIndex;

        public formal_parameter_target(TypeAnnotationTypes.ETargetType tt, int index) {
            super(tt);
            formalParamIndex = index;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeByte(formalParamIndex);
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            out.print(" ");
            out.print(formalParamIndex);
        }

        @Override
        public int getLength() {
            return 1;
        }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            sb.append(tabString(tabLevel)).append(String.format("{ formal_parameter_index: %d; }",formalParamIndex));
        }
    }

    /**
     * throws_target (4.7.20.1. The target_info union)
     *
     * The throws_target item indicates that an annotation appears on the i'th type in the throws clause of a method or
     * constructor declaration.
     *
     * throws_target {
     *     u2 throws_type_index;
     * }
     */
    public static class throws_target extends TypeAnnotationTargetInfoData {

        int throwsTypeIndex;

        public throws_target(TypeAnnotationTypes.ETargetType tt, int index) {
            super(tt);
            throwsTypeIndex = index;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeShort(throwsTypeIndex);
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            out.print(" ");
            out.print(throwsTypeIndex);
        }

        @Override
        public int getLength() {
            return 2;
        }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            sb.append(tabString(tabLevel)).append(String.format("{ throws_type_index: %d; }",throwsTypeIndex));
        }
    }

    /**
     * localvar_target (4.7.20.1. The target_info union)
     *
     * The localvar_target item indicates that an annotation appears on the type in a local variable declaration,
     * including a variable declared as a resource in a try-with-resources statement.
     *
     * localvar_target {
     *     u2 table_length;
     *     {   u2 start_pc;
     *         u2 length;
     *         u2 index;
     *     } table[table_length];
     * }
     */
    public static class localvar_target extends TypeAnnotationTargetInfoData {

        public class LocalVar_Entry {

            public int startPC;
            public int length;
            public int cpx;

            public LocalVar_Entry(int st, int len, int index) {
                startPC = st;
                length = len;
                cpx = index;
            }

            void write(CheckedDataOutputStream out) throws IOException {
                out.writeShort(startPC);
                out.writeShort(length);
                out.writeShort(cpx);
            }

            public void _print(PrintWriter out, String tab) {
                out.print(tab + "{");
                out.print(startPC);
                out.print(" ");
                out.print(length);
                out.print(" ");
                out.print(cpx);
                out.print("}");
            }

            public String toString() {
                return String.format("start_pc: %d, length: %d, index: %d", startPC, length, cpx);
            }
        }

        ArrayList<LocalVar_Entry> table = null;

        public localvar_target(TypeAnnotationTypes.ETargetType tt, int size) {
            super(tt);
            table = new ArrayList<>(size);
        }

        public void addEntry(int startPC, int length, int cpx) {
            LocalVar_Entry entry = new LocalVar_Entry(startPC, length, cpx);
            table.add(entry);
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeShort(table.size());
            for (LocalVar_Entry entry : table) {
                entry.write(out);
            }
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            String innerTab = tab + " ";
            for (LocalVar_Entry entry : table) {
                entry._print(out, innerTab);
            }
            out.print(tab);
        }

        @Override
        public int getLength() {
            return 2 + // U2 for table size
                    (6 * table.size()); // (3 * U2) for each table entry
        }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            int i = 0;
            sb.append(tabString(tabLevel)).append(String.format("{ %d  {", table.size()));
            for (LocalVar_Entry entry : table) {
                sb.append(String.format(" [%d]: %s;", i++, entry.toString()));
            }
            sb.append(" } }");
        }
    }

    /**
     * catch_target (4.7.20.1. The target_info union)
     *
     * The catch_target item indicates that an annotation appears on the i'th type in an exception parameter declaration.
     *
     * catch_target {
     *     u2 exception_table_index;
     * }
     */
    public static class catch_target extends TypeAnnotationTargetInfoData {

        int exceptionTableIndex;

        public catch_target(TypeAnnotationTypes.ETargetType tt, int index) {
            super(tt);
            exceptionTableIndex = index;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeShort(exceptionTableIndex);
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            out.print(" ");
            out.print(exceptionTableIndex);
        }

        @Override
        public int getLength() {
            return 2;
        }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            sb.append(tabString(tabLevel)).append(String.format("{ exception_table_index: %d; }",exceptionTableIndex));
        }
    }

    /**
     * offset_target (4.7.20.1. The target_info union)
     *
     *  The offset_target item indicates that an annotation appears on either the type in an instanceof expression or
     *  a new expression, or the type before the :: in a method reference expression.
     *
     *  offset_target {
     *     u2 offset;
     * }
     */
    public static class offset_target extends TypeAnnotationTargetInfoData {

        int offset;

        public offset_target(TypeAnnotationTypes.ETargetType tt, int offset) {
            super(tt);
            this.offset = offset;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeShort(offset);
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            out.print(" ");
            out.print(offset);
        }

        @Override
        public int getLength() {
            return 2;
        }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            sb.append(tabString(tabLevel)).append(String.format("{ offset: %d; }", offset));
        }
    }

    /**
     * type_argument_target (4.7.20.1. The target_info union)
     *
     *  The type_argument_target item indicates that an annotation appears either on the i'th type in a cast expression,
     *  or on the i'th type argument in the explicit type argument list for any of the following: a new expression,
     *  an explicit constructor invocation statement, a method invocation expression, or a method reference expression
     *
     *  type_argument_target {
     *     u2 offset;
     *     u1 type_argument_index;
     * }
     */
    public static class type_argument_target extends TypeAnnotationTargetInfoData {

        int offset;
        int typeArgumentIndex;

        public type_argument_target(TypeAnnotationTypes.ETargetType tt, int offset, int index) {
            super(tt);
            this.offset = offset;
            typeArgumentIndex = index;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeShort(offset);
            out.writeByte(typeArgumentIndex);
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            out.print(" ");
            out.print(offset);
            out.print(" ");
            out.print(typeArgumentIndex);
        }

        @Override
        public int getLength() {
            return 3;
        }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            sb.append(tabString(tabLevel)).append(String.format("{ offset: %d; type_argument_index: %d; }",
                    offset, typeArgumentIndex));
        }
    }

}

