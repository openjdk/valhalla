/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import jdk.experimental.bytecode.Pool.Class_info;
import jdk.experimental.bytecode.Pool.Double_info;
import jdk.experimental.bytecode.Pool.Fieldref_info;
import jdk.experimental.bytecode.Pool.Float_info;
import jdk.experimental.bytecode.Pool.Integer_info;
import jdk.experimental.bytecode.Pool.InterfaceMethodref_info;
import jdk.experimental.bytecode.Pool.InvokeDynamic_info;
import jdk.experimental.bytecode.Pool.Long_info;
import jdk.experimental.bytecode.Pool.MethodHandle_info;
import jdk.experimental.bytecode.Pool.MethodType_info;
import jdk.experimental.bytecode.Pool.Methodref_info;
import jdk.experimental.bytecode.Pool.NameAndType_info;
import jdk.experimental.bytecode.Pool.String_info;
import jdk.experimental.bytecode.Pool.Utf8_info;
import jdk.experimental.bytecode.Pool.PoolInfo;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

import static jdk.experimental.bytecode.Pool.*;
import jdk.experimental.bytecode.Pool.PoolInfoAndIndex;

/**
 * A helper for explicitly building immutable constant pools with a defined
 * order.
 */
class ImmutableBytePoolHelper<S, T> extends BytePoolHelper<S, T> {

    private final BuiltPool icpool;

    public ImmutableBytePoolHelper(Consumer<PoolBuilder<byte[]>> cpBuild,
            Function<S, String> symbolToString, Function<T, String> typeToString) {
        super(null /* already built */, symbolToString, typeToString);
        this.icpool = new BuiltPool(cpBuild);
        // go through the constant pool installing all the entries in the
        // helper's map.
        for (PoolInfoAndIndex pii : icpool.entriesAndIndicies()) {
            PoolInfo info = pii.info;
            PoolKey poolKey = new PoolKey();
            try {
                switch (info.getTag()) {
                    case STRING_TAG: {
                        String_info si = (String_info) info;
                        poolKey.setString(si.getString());
                        break;
                    }
                    case INTEGER_TAG:
                        poolKey.setInteger(((Integer_info) info).value);
                        break;
                    case FLOAT_TAG:
                        poolKey.setFloat(((Float_info) info).value);
                        break;
                    case DOUBLE_TAG:
                        poolKey.setDouble(((Double_info) info).value);
                        break;
                    case LONG_TAG:
                        poolKey.setLong(((Long_info) info).value);
                        break;
                    case CLASS_TAG: {
                        Class_info ci = (Class_info) info;
                        poolKey.setClass(ci.getName());
                        break;
                    }
                    case FIELDREF_TAG: {
                        Fieldref_info fr = (Fieldref_info) info;
                        NameAndType_info nat = fr.getNameAndTypeInfo();
                        poolKey.setMemberRef(PoolTag.FIELDREF, fr.getClassName(), nat.getName(), nat.getType());
                        break;
                    }
                    case METHODREF_TAG: {
                        Methodref_info mr = (Methodref_info) info;
                        NameAndType_info nat = mr.getNameAndTypeInfo();
                        poolKey.setMemberRef(PoolTag.METHODREF, mr.getClassName(), nat.getName(), nat.getType());
                        break;
                    }
                    case INTERFACEMETHODREF_TAG: {
                        InterfaceMethodref_info mr = (InterfaceMethodref_info) info;
                        NameAndType_info nat = mr.getNameAndTypeInfo();
                        poolKey.setMemberRef(PoolTag.INTERFACEMETHODREF, mr.getClassName(), nat.getName(), nat.getType());
                        break;
                    }
                    case METHODTYPE_TAG: {
                        MethodType_info mt = (MethodType_info) info;
                        poolKey.setMethodType(mt.getType());
                        break;
                    }
                    case METHODHANDLE_TAG: {
                        MethodHandle_info mh = (MethodHandle_info) info;
                        PoolInfo rinfo = icpool.get(mh.reference_index);
                        String owner;
                        NameAndType_info nat;
                        switch (rinfo.getTag()) {
                            case FIELDREF_TAG:
                                Fieldref_info fr = (Fieldref_info) rinfo;
                                owner = fr.getClassName();
                                nat = fr.getNameAndTypeInfo();
                                break;
                            case METHODREF_TAG:
                                Methodref_info mr = (Methodref_info) rinfo;
                                owner = mr.getClassName();
                                nat = mr.getNameAndTypeInfo();
                                break;
                            case INTERFACEMETHODREF_TAG:
                                InterfaceMethodref_info imr = (InterfaceMethodref_info) rinfo;
                                owner = imr.getClassName();
                                nat = imr.getNameAndTypeInfo();
                                break;
                            default:
                                throw new IllegalStateException(rinfo.toString());
                        }
                        poolKey.setMethodHandle(mh.reference_kind, owner, nat.getName(), nat.getType());
                        break;
                    }
                    case NAMEANDTYPE_TAG: {
                        NameAndType_info nat = (NameAndType_info) info;
                        poolKey.setNameAndType(nat.getName(), nat.getType());
                        break;
                    }
                    case UTF8_TAG: {
                        Utf8_info u = (Utf8_info) info;
                        poolKey.setUtf8(u.value);
                        break;
                    }
                    case INVOKEDYNAMIC_TAG: {
                        InvokeDynamic_info idi = (InvokeDynamic_info) info;
                        NameAndType_info nat = (NameAndType_info) icpool.get(idi.name_and_type_index);
                        PoolInfo bsm = icpool.get(idi.bootstrap_method_attr_index);
                        // idi.bootstrap_method_attr_index, idi.name_and_type_index);
                        throw new UnsupportedOperationException("InvokeDynamic " + idi);
                    }
                    case MODULE_TAG: {
                        throw new UnsupportedOperationException("Module " + info);
                    }
                    case PACKAGE_TAG: {
                        throw new UnsupportedOperationException("Package " + info);
                    }
                    default:
                        throw new IllegalStateException("unsupported pool entry: " + info);
                }
            } catch (PoolException ex) {
            }
            poolKey.at(pii.index);
            entries.enter(poolKey);
        }
    }

    /**
     * @return the count of constant pool indicies.
     */
    @Override
    public int size() {
        return icpool.builder.size();
    }

    /**
     * @return the size in bytes of all constant pool entries.
     */
    @Override
    public byte[] representation() {
        return icpool.builder.representation();
    }

    /**
     * Build the byte array representation of the constant pool, and at the
     * same time build as an array of PoolInfo objects.
     */
    private static class BuiltPool extends Pool {

        final ArrayList<PoolInfo> infos = new ArrayList<>();
        final IBCPB builder;

        BuiltPool(Consumer<PoolBuilder<byte[]>> cpBuild) {
            this.builder = new IBCPB();
            cpBuild.accept(builder);
        }

        @Override
        protected PoolInfo getSafe(int index) {
            return infos.get(index);
        }

        @Override
        public int size() {
            return infos.size();
        }

        private class IBCPB extends BytePoolBuilder {

            @Override
            public int putClass(int utf8_idx) {
                int id = super.putClass(utf8_idx);
                return put(id, new Class_info(utf8_idx));
            }

            @Override
            public int putMemberRef(PoolTag tag, int owner_idx, int nameAndType_idx) {
                int id = super.putMemberRef(tag, owner_idx, nameAndType_idx);
                switch (tag) {
                    case FIELDREF:
                        return put(id, new Fieldref_info(owner_idx, nameAndType_idx));
                    case METHODREF:
                        return put(id, new Methodref_info(owner_idx, nameAndType_idx));
                    case INTERFACEMETHODREF:
                        return put(id, new InterfaceMethodref_info(owner_idx, nameAndType_idx));
                    default:
                        throw new IllegalArgumentException("tag: " + tag);
                }
            }

            @Override
            public int putInt(int i) {
                int id = super.putInt(i);
                return put(id, new Integer_info(i));
            }

            @Override
            public int putFloat(float f) {
                int id = super.putFloat(f);
                return put(id, new Float_info(f));
            }

            @Override
            public int putLong(long l) {
                int id = super.putLong(l);
                return put(id, new Long_info(l));
            }

            @Override
            public int putDouble(double d) {
                int id = super.putDouble(d);
                return put(id, new Double_info(d));
            }

            @Override
            public int putInvokeDynamic(int bsmIndex, int nameAndType_idx) {
                int id = super.putInvokeDynamic(bsmIndex, nameAndType_idx);
                return put(id, new InvokeDynamic_info(bsmIndex, nameAndType_idx));
            }

            @Override
            public int putConstantDynamic(int bsmIndex, int nameAndType_idx) {
                int id = super.putConstantDynamic(bsmIndex, nameAndType_idx);
                throw new UnsupportedOperationException();
            }

            @Override
            public int putMethodType(int desc_idx) {
                int id = super.putMethodType(desc_idx);
                return put(id, new MethodType_info(desc_idx));
            }

            @Override
            public int putMethodHandle(int refKind, int ref_idx) {
                int id = super.putMethodHandle(refKind, ref_idx);
                return put(id, new MethodHandle_info(refKind, ref_idx));
            }

            @Override
            public int putString(int utf8_index) {
                int id = super.putString(utf8_index);
                return put(id, new String_info(utf8_index));
            }

            @Override
            public int putNameAndType(int name_idx, int type_idx) {
                int id = super.putNameAndType(name_idx, type_idx);
                return put(id, new NameAndType_info(name_idx, type_idx));
            }

            @Override
            public int putUtf8(CharSequence s) {
                int id = super.putUtf8(s);
                return put(id, new Utf8_info(s.toString()));
            }

            private int put(int id, PoolInfo info) {
                // fill gaps with nulls
                int fill = id - infos.size();
                for (int i = 0; i < fill; ++i) {
                    infos.add(null);
                }
                infos.add(info);
                return id;
            }
        }
    }
}
