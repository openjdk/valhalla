/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.experimental.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * See JVMS, section 4.
 *
 * Representation of a Constant Pool, where subclasses of PoolInfo represent the
 * entries. Concrete implementations provide access to pool entries by index.
 */
public abstract class Pool {

    public static final int CLASS_TAG = 7;
    public static final int CONSTANTDYNAMIC_TAG = 17;
    public static final int DOUBLE_TAG = 6;
    public static final int FIELDREF_TAG = 9;
    public static final int FLOAT_TAG = 4;
    public static final int INTEGER_TAG = 3;
    public static final int INTERFACEMETHODREF_TAG = 11;
    public static final int INVOKEDYNAMIC_TAG = 18;
    public static final int LONG_TAG = 5;
    public static final int METHODHANDLE_TAG = 15;
    public static final int METHODREF_TAG = 10;
    public static final int METHODTYPE_TAG = 16;
    public static final int MODULE_TAG = 19;
    public static final int NAMEANDTYPE_TAG = 12;
    public static final int PACKAGE_TAG = 20;
    public static final int STRING_TAG = 8;
    public static final int UNICODE_TAG = 2;
    public static final int UTF8_TAG = 1;

    protected abstract PoolInfo getSafe(int index);
    
    public abstract int size();

    public PoolInfo get(int index) throws InvalidIndex {
        if (index <= 0 || index >= size())
            throw new InvalidIndex(index);
        PoolInfo info = getSafe(index);
        if (info == null) {
            // this occurs for indices referencing the "second half" of an
            // 8 byte constant, such as CONSTANT_Double or CONSTANT_Long
            throw new InvalidIndex(index);
        }
        return info;
    }

    public int byteLength() {
        int length = 2;
        for (int i = 1; i < size();) {
            PoolInfo cpInfo = getSafe(i);
            length += cpInfo.byteLength();
            i += cpInfo.size();
        }
        return length;
    }

    public Iterable<PoolInfo> entries() {
        return () -> new Iterator<PoolInfo>() {

            @Override
            public boolean hasNext() {
                return next < size;
            }

            @Override
            public PoolInfo next() {
                PoolInfo pi = getSafe(next);
                switch (pi.getTag()) {
                    case DOUBLE_TAG:
                    case LONG_TAG:
                        next += 2;
                        break;
                    default:
                        next += 1;
                }
                return pi;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            private int next = 1;
            private final int size = size();

        };
    }
    
    public Iterable<PoolInfoAndIndex> entriesAndIndicies() {
        return () -> new Iterator<PoolInfoAndIndex>() {

            @Override
            public boolean hasNext() {
                return next < size;
            }

            @Override
            public PoolInfoAndIndex next() {
                int i = next;
                PoolInfo pi = getSafe(i);
                switch (pi.getTag()) {
                    case DOUBLE_TAG:
                    case LONG_TAG:
                        next += 2;
                        break;
                    default:
                        next += 1;
                }
                return new PoolInfoAndIndex(pi, i);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            private int next = 1;
            private final int size = size();

        };
    }
    
    
    public class PoolInfoAndIndex {
        public final PoolInfo info;
        public final int index;
        PoolInfoAndIndex(PoolInfo info, int index) {
            this.info = info;
            this.index = index;
        }
    }

    private PoolInfo get(int index, int expected_type) throws InvalidIndex, UnexpectedEntry {
        PoolInfo info = get(index);
        if (info.getTag() != expected_type) {
            throw new UnexpectedEntry(index, expected_type, info.getTag());
        }
        return info;
    }

    public Utf8_info getUTF8Info(int index) throws InvalidIndex, UnexpectedEntry {
        return ((Utf8_info) get(index, UTF8_TAG));
    }

    public Class_info getClassInfo(int index) throws InvalidIndex, UnexpectedEntry {
        return ((Class_info) get(index, CLASS_TAG));
    }

    public Module_info getModuleInfo(int index) throws InvalidIndex, UnexpectedEntry {
        return ((Module_info) get(index, MODULE_TAG));
    }

    public NameAndType_info getNameAndTypeInfo(int index) throws InvalidIndex, UnexpectedEntry {
        return ((NameAndType_info) get(index, NAMEANDTYPE_TAG));
    }

    public Package_info getPackageInfo(int index) throws InvalidIndex, UnexpectedEntry {
        return ((Package_info) get(index, PACKAGE_TAG));
    }

    public String getUTF8Value(int index) throws InvalidIndex, UnexpectedEntry {
        return getUTF8Info(index).value;
    }

    public int getUTF8Index(String value) throws EntryNotFound {
        for (int i = 1; i < size(); i++) {
            PoolInfo info = getSafe(i);
            if (info instanceof Utf8_info &&
                    ((Utf8_info) info).value.equals(value))
                return i;
        }
        throw new EntryNotFound(value);
    }

    public interface Visitor<R, P> {

        R visitClass(Class_info info, P p);

        R visitDouble(Double_info info, P p);

        R visitFieldref(Fieldref_info info, P p);

        R visitFloat(Float_info info, P p);

        R visitInteger(Integer_info info, P p);

        R visitInterfaceMethodref(InterfaceMethodref_info info, P p);

        R visitInvokeDynamic(InvokeDynamic_info info, P p);

        R visitLong(Long_info info, P p);

        R visitMethodref(Methodref_info info, P p);

        R visitMethodHandle(MethodHandle_info info, P p);

        R visitMethodType(MethodType_info info, P p);

        R visitModule(Module_info info, P p);

        R visitNameAndType(NameAndType_info info, P p);

        R visitPackage(Package_info info, P p);

        R visitString(String_info info, P p);

        R visitUtf8(Utf8_info info, P p);
    }

    public abstract class PoolInfo {

        public Pool getPool() {
            return Pool.this;
        }

        public abstract int getTag();

        /**
         * @return The number of slots in the constant pool used by this entry.
         * 2 for CONSTANT_Double and CONSTANT_Long; 1 for everything else.
         */
        public int size() {
            return 1;
        }

        public abstract int byteLength();

        public abstract <R, D> R accept(Visitor<R, D> visitor, D data);
    }

    public abstract class RefPoolInfo extends PoolInfo {

        protected RefPoolInfo(int tag, int class_index, int name_and_type_index) {
            this.tag = tag;
            this.class_index = class_index;
            this.name_and_type_index = name_and_type_index;
        }

        @Override
        public int getTag() {
            return tag;
        }

        @Override
        public int byteLength() {
            return 5;
        }

        public Class_info getClassInfo() throws PoolException {
            return Pool.this.getClassInfo(class_index);
        }

        public String getClassName() throws PoolException {
            return Pool.this.getClassInfo(class_index).getName();
        }

        public NameAndType_info getNameAndTypeInfo() throws PoolException {
            return Pool.this.getNameAndTypeInfo(name_and_type_index);
        }

        public final int tag;
        public final int class_index;
        public final int name_and_type_index;
    }

    public class Class_info extends PoolInfo {

        public Class_info(int name_index) {
            this.name_index = name_index;
        }

        @Override
        public int getTag() {
            return CLASS_TAG;
        }

        @Override
        public int byteLength() {
            return 3;
        }

        /**
         * Get the raw value of the class referenced by this constant pool
         * entry.This will either be the name of the class, in internal form, or
         * a descriptor for an array class.
         *
         * @return the raw value of the class
         * @throws PoolException
         */
        public String getName() throws PoolException {
            return getUTF8Value(name_index);
        }

        /**
         * If this constant pool entry identifies either a class or interface
         * type, or a possibly multi-dimensional array of a class of interface
         * type, return the name of the class or interface in internal
         * form.Otherwise, (i.e. if this is a possibly multi-dimensional array
         * of a primitive type), return null.
         *
         * @return the base class or interface name
         * @throws PoolException
         */
        public String getBaseName() throws PoolException {
            String name = getName();
            if (name.startsWith("[")) {
                int index = name.indexOf("[L");
                if (index == -1)
                    index = name.indexOf("[Q");
                if (index == -1) {
                    return null;
                }
                return name.substring(index + 2, name.length() - 1);
            } else {
                return name;
            }
        }

        public int getDimensionCount() throws PoolException {
            String name = getName();
            int count = 0;
            while (name.charAt(count) == '[') {
                count++;
            }
            return count;
        }

        @Override
        public String toString() {
            return "CONSTANT_Class_info[name_index: " + name_index + "]";
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitClass(this, data);
        }

        public final int name_index;
    }

    public class Double_info extends PoolInfo {

        public Double_info(double value) {
            this.value = value;
        }

        @Override
        public int getTag() {
            return DOUBLE_TAG;
        }

        @Override
        public int byteLength() {
            return 9;
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public String toString() {
            return "CONSTANT_Double_info[value: " + value + "]";
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitDouble(this, data);
        }

        public final double value;
    }

    public class Fieldref_info extends RefPoolInfo {

        public Fieldref_info(int class_index, int name_and_type_index) {
            super(FIELDREF_TAG, class_index, name_and_type_index);
        }

        @Override
        public String toString() {
            return "CONSTANT_Fieldref_info[class_index: " + class_index + ", name_and_type_index: " + name_and_type_index + "]";
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitFieldref(this, data);
        }
    }

    public class Float_info extends PoolInfo {

        public Float_info(float value) {
            this.value = value;
        }

        @Override
        public int getTag() {
            return FLOAT_TAG;
        }

        @Override
        public int byteLength() {
            return 5;
        }

        @Override
        public String toString() {
            return "CONSTANT_Float_info[value: " + value + "]";
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitFloat(this, data);
        }

        public final float value;
    }

    public class Integer_info extends PoolInfo {

        public Integer_info(int value) {
            this.value = value;
        }

        @Override
        public int getTag() {
            return INTEGER_TAG;
        }

        @Override
        public int byteLength() {
            return 5;
        }

        @Override
        public String toString() {
            return "CONSTANT_Integer_info[value: " + value + "]";
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitInteger(this, data);
        }

        public final int value;
    }

    public class InterfaceMethodref_info extends RefPoolInfo {

        public InterfaceMethodref_info(int class_index, int name_and_type_index) {
            super(INTERFACEMETHODREF_TAG, class_index, name_and_type_index);
        }

        @Override
        public String toString() {
            return "CONSTANT_InterfaceMethodref_info[class_index: " + class_index + ", name_and_type_index: " + name_and_type_index + "]";
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitInterfaceMethodref(this, data);
        }
    }

    public class InvokeDynamic_info extends PoolInfo {

        public InvokeDynamic_info(int bootstrap_method_index, int name_and_type_index) {
            this.bootstrap_method_attr_index = bootstrap_method_index;
            this.name_and_type_index = name_and_type_index;
        }

        @Override
        public int getTag() {
            return INVOKEDYNAMIC_TAG;
        }

        @Override
        public int byteLength() {
            return 5;
        }

        @Override
        public String toString() {
            return "CONSTANT_InvokeDynamic_info[bootstrap_method_index: " + bootstrap_method_attr_index + ", name_and_type_index: " + name_and_type_index + "]";
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitInvokeDynamic(this, data);
        }

        public NameAndType_info getNameAndTypeInfo() throws PoolException {
            return Pool.this.getNameAndTypeInfo(name_and_type_index);
        }

        public final int bootstrap_method_attr_index;
        public final int name_and_type_index;
    }

    public class Long_info extends PoolInfo {

        public Long_info(long value) {
            this.value = value;
        }

        @Override
        public int getTag() {
            return LONG_TAG;
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public int byteLength() {
            return 9;
        }

        @Override
        public String toString() {
            return "CONSTANT_Long_info[value: " + value + "]";
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitLong(this, data);
        }

        public final long value;
    }

    public class MethodHandle_info extends PoolInfo {

        public MethodHandle_info(int ref_kind, int member_index) {
            this.reference_kind = ref_kind;
            this.reference_index = member_index;
        }

        @Override
        public int getTag() {
            return METHODHANDLE_TAG;
        }

        @Override
        public int byteLength() {
            return 4;
        }

        @Override
        public String toString() {
            return "CONSTANT_MethodHandle_info[ref_kind: " + RefKind.getRefkind(reference_kind) + ", member_index: " + reference_index + "]";
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitMethodHandle(this, data);
        }

        public RefPoolInfo getRefPoolInfo() throws PoolException {
            int expected = METHODREF_TAG;
            int actual = Pool.this.get(reference_index).getTag();
            // allow these tag types also:
            switch (actual) {
                case FIELDREF_TAG:
                case INTERFACEMETHODREF_TAG:
                    expected = actual;
            }
            return (RefPoolInfo) Pool.this.get(reference_index, expected);
        }

        public final int reference_kind;
        public final int reference_index;
    }

    public class MethodType_info extends PoolInfo {

        public MethodType_info(int signature_index) {
            this.descriptor_index = signature_index;
        }

        @Override
        public int getTag() {
            return METHODTYPE_TAG;
        }

        @Override
        public int byteLength() {
            return 3;
        }

        @Override
        public String toString() {
            return "CONSTANT_MethodType_info[signature_index: " + descriptor_index + "]";
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitMethodType(this, data);
        }

        public String getType() throws PoolException {
            return Pool.this.getUTF8Value(descriptor_index);
        }

        public final int descriptor_index;
    }

    public class Methodref_info extends RefPoolInfo {

        public Methodref_info(int class_index, int name_and_type_index) {
            super(METHODREF_TAG, class_index, name_and_type_index);
        }

        @Override
        public String toString() {
            return "CONSTANT_Methodref_info[class_index: " + class_index + ", name_and_type_index: " + name_and_type_index + "]";
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitMethodref(this, data);
        }
    }

    public class Module_info extends PoolInfo {

        public Module_info(int name_index) {
            this.name_index = name_index;
        }

        @Override
        public int getTag() {
            return MODULE_TAG;
        }

        @Override
        public int byteLength() {
            return 3;
        }

        /**
         * Get the raw value of the module name referenced by this constant pool
         * entry. This will be the name of the module.
         *
         * @return the raw value of the module name
         */
        public String getName() throws PoolException {
            return Pool.this.getUTF8Value(name_index);
        }

        @Override
        public String toString() {
            return "CONSTANT_Module_info[name_index: " + name_index + "]";
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitModule(this, data);
        }

        public final int name_index;
    }

    public class NameAndType_info extends PoolInfo {

        public NameAndType_info(int name_index, int type_index) {
            this.name_index = name_index;
            this.type_index = type_index;
        }

        @Override
        public int getTag() {
            return NAMEANDTYPE_TAG;
        }

        @Override
        public int byteLength() {
            return 5;
        }

        public String getName() throws PoolException {
            return Pool.this.getUTF8Value(name_index);
        }

        public String getType() throws PoolException {
            return Pool.this.getUTF8Value(type_index);
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitNameAndType(this, data);
        }

        @Override
        public String toString() {
            return "CONSTANT_NameAndType_info[name_index: " + name_index + ", type_index: " + type_index + "]";
        }

        public final int name_index;
        public final int type_index;
    }

    public class Package_info extends PoolInfo {

        public Package_info(int name_index) {
            this.name_index = name_index;
        }

        @Override
        public int getTag() {
            return PACKAGE_TAG;
        }

        @Override
        public int byteLength() {
            return 3;
        }

        /**
         * Get the raw value of the package name referenced by this constant
         * pool entry.This will be the name of the package, in internal form.
         *
         * @return the raw value of the module name
         * @throws jdk.experimental.bytecode.classfile.ConstantPoolException
         */
        public String getName() throws PoolException {
            return Pool.this.getUTF8Value(name_index);
        }

        @Override
        public String toString() {
            return "CONSTANT_Package_info[name_index: " + name_index + "]";
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitPackage(this, data);
        }

        public final int name_index;
    }

    public class String_info extends PoolInfo {

        public String_info(int string_index) {
            this.string_index = string_index;
        }

        @Override
        public int getTag() {
            return STRING_TAG;
        }

        @Override
        public int byteLength() {
            return 3;
        }

        public String getString() throws PoolException {
            return Pool.this.getUTF8Value(string_index);
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitString(this, data);
        }

        @Override
        public String toString() {
            return "CONSTANT_String_info[class_index: " + string_index + "]";
        }

        public final int string_index;
    }

    public class Utf8_info extends PoolInfo {

        public Utf8_info(String value) {
            this.value = value;
        }

        @Override
        public int getTag() {
            return UTF8_TAG;
        }

        @Override
        public int byteLength() {
            class SizeOutputStream extends OutputStream {

                @Override
                public void write(int b) {
                    size++;
                }
                int size;
            }
            SizeOutputStream sizeOut = new SizeOutputStream();
            DataOutputStream out = new DataOutputStream(sizeOut);
            try {
                out.writeUTF(value);
            } catch (IOException ignore) {
            }
            return 1 + sizeOut.size;
        }

        @Override
        public String toString() {
            if (value.length() < 32 && isPrintableAscii(value)) {
                return "CONSTANT_Utf8_info[value: \"" + value + "\"]";
            } else {
                return "CONSTANT_Utf8_info[value: (" + value.length() + " chars)]";
            }
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitUtf8(this, data);
        }

        public final String value;
    }

    public static enum RefKind {
        GETFIELD_REF(1),
        GETSTATIC_REF(2),
        PUTFIELD_REF(3),
        PUTSTATIC_REF(4),
        INVOKEVIRTUAL_REF(5),
        INVOKESTATIC_REF(6),
        INVOKESPECIAL_REF(7),
        NEWINVOKESPECIAL_REF(8),
        INVOKEINTERFACE_REF(9);

        public final int tag;

        RefKind(int tag) {
            this.tag = tag;
        }

        public static RefKind getRefkind(int tag) {
            switch (tag) {
                case 1:
                    return GETFIELD_REF;
                case 2:
                    return GETSTATIC_REF;
                case 3:
                    return PUTFIELD_REF;
                case 4:
                    return PUTSTATIC_REF;
                case 5:
                    return INVOKEVIRTUAL_REF;
                case 6:
                    return INVOKESTATIC_REF;
                case 7:
                    return INVOKESPECIAL_REF;
                case 8:
                    return NEWINVOKESPECIAL_REF;
                case 9:
                    return INVOKEINTERFACE_REF;
                default:
                    return null;
            }
        }
    }

    static boolean isPrintableAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 32 || c >= 127) {
                return false;
            }
        }
        return true;
    }

    public static class InvalidIndex extends PoolException {

        private static final long serialVersionUID = -4350294289300939730L;

        public InvalidIndex(int index) {
            super(index);
        }

        @Override
        public String getMessage() {
            // i18n
            return "invalid index #" + index;
        }
    }

    public static class UnexpectedEntry extends PoolException {

        private static final long serialVersionUID = 6986335935377933211L;

        public UnexpectedEntry(int index, int expected_tag, int found_tag) {
            super(index);
            this.expected_tag = expected_tag;
            this.found_tag = found_tag;
        }

        @Override
        public String getMessage() {
            // i18n?
            return "unexpected entry at #" + index + " -- expected tag " + expected_tag + ", found " + found_tag;
        }

        public final int expected_tag;
        public final int found_tag;
    }

    public static class InvalidEntry extends PoolException {

        private static final long serialVersionUID = 1000087545585204447L;

        public InvalidEntry(int index, int tag) {
            super(index);
            this.tag = tag;
        }

        @Override
        public String getMessage() {
            // i18n?
            return "unexpected tag at #" + index + ": " + tag;
        }

        public final int tag;
    }

    public static class EntryNotFound extends PoolException {

        private static final long serialVersionUID = 2885537606468581850L;

        public EntryNotFound(Object value) {
            super(-1);
            this.value = value;
        }

        @Override
        public String getMessage() {
            // i18n?
            return "value not found: " + value;
        }

        public final Object value;
    }

}
