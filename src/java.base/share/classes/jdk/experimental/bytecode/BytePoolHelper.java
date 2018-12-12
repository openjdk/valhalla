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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

/**
 * A helper for building and tracking constant pools whose entries are
 * represented as byte arrays.
 *
 * @param <S> the type of the symbol representation
 * @param <T> the type of type descriptors representation
 */
public class BytePoolHelper<S, T> implements PoolHelper<S, T, byte[]> {

    final BytePoolBuilder builder;
    GrowableByteBuffer bsm_attr = new GrowableByteBuffer();
    //Map<PoolKey, PoolKey> indicesMap = new HashMap<>();
    int currentBsmIndex = 0;

    KeyMap<PoolKey> entries = new KeyMap<>();
    KeyMap<BsmKey> bootstraps = new KeyMap<>();
    PoolKey key = new PoolKey();
    BsmKey bsmKey = new BsmKey();

    Function<S, String> symbolToString;
    Function<T, String> typeToString;

    public BytePoolHelper(Function<S, String> symbolToString, Function<T, String> typeToString) {
        this(new BytePoolBuilder(), symbolToString, typeToString);
    }

    BytePoolHelper(BytePoolBuilder builder, Function<S, String> symbolToString, Function<T, String> typeToString) {
        this.builder = builder;
        this.symbolToString = symbolToString;
        this.typeToString = typeToString;
    }
    
    public static <S, T> BytePoolHelper<S, T> immutable(Consumer<PoolBuilder<byte[]>> cpBuild,
            Function<S, String> symbolToString, Function<T, String> typeToString) {
        return new ImmutableBytePoolHelper<>(cpBuild, symbolToString, typeToString);
    }

    static class KeyMap<K extends AbstractKey<K>> {

        @SuppressWarnings("unchecked")
        K[] table = (K[])new AbstractKey<?>[0x10];
        int nelems;

        public void enter(K e) {
            if (nelems * 3 >= (table.length - 1) * 2)
                dble();
            int hash = getIndex(e);
            K old = table[hash];
            if (old == null) {
                nelems++;
            }
            e.next = old;
            table[hash] = e;
        }

        protected K lookup(K other) {
            K e = table[getIndex(other)];
            while (e != null && !e.equals(other))
                e = e.next;
            return e;
        }

        /**
         * Look for slot in the table.
         * We use open addressing with double hashing.
         */
        int getIndex(K e) {
            int hashMask = table.length - 1;
            int h = e.hashCode();
            int i = h & hashMask;
            // The expression below is always odd, so it is guaranteed
            // to be mutually prime with table.length, a power of 2.
            int x = hashMask - ((h + (h >> 16)) << 1);
            for (; ; ) {
                K e2 = table[i];
                if (e2 == null)
                    return i;
                else if (e.hash == e2.hash)
                    return i;
                i = (i + x) & hashMask;
            }
        }

        @SuppressWarnings("unchecked")
        private void dble() {
            K[] oldtable = table;
            table = (K[])new AbstractKey<?>[oldtable.length * 2];
            int n = 0;
            for (int i = oldtable.length; --i >= 0; ) {
                K e = oldtable[i];
                if (e != null) {
                    table[getIndex(e)] = e;
                    n++;
                }
            }
            // We don't need to update nelems for shared inherited scopes,
            // since that gets handled by leave().
            nelems = n;
        }
    }

    public static abstract class AbstractKey<K extends AbstractKey<K>> {
        int hash;
        int index = -1;
        K next;

        abstract K dup();

        @Override
        public abstract boolean equals(Object o);

        @Override
        public int hashCode() {
            return hash;
        }

        void at(int index) {
            this.index = index;
        }
    }

    public static class PoolKey extends AbstractKey<PoolKey> {
        PoolTag tag;
        Object o1;
        Object o2;
        Object o3;
        Object o4;
        int size = -1;

        void setUtf8(CharSequence s) {
            tag = PoolTag.UTF8;
            o1 = s;
            size = 1;
            hash = tag.tag | (s.hashCode() << 1);
        }

        void setClass(String clazz) {
            tag = PoolTag.CLASS;
            o1 = clazz;
            size = 1;
            hash = tag.tag | (clazz.hashCode() << 1);
        }

        void setNameAndType(CharSequence name, String type) {
            tag = PoolTag.NAMEANDTYPE;
            o1 = name;
            o2 = type;
            size = 2;
            hash = tag.tag | ((name.hashCode() | type.hashCode()) << 1);
        }

        void setMemberRef(PoolTag poolTag, String owner, CharSequence name, String type) {
            tag = poolTag;
            o1 = owner;
            o2 = name;
            o3 = type;
            size = 3;
            hash = tag.tag | ((owner.hashCode() | name.hashCode() | type.hashCode()) << 1);
        }

        void setInvokeDynamic(int bsmIndex, CharSequence name, String type) {
            tag = PoolTag.INVOKEDYNAMIC;
            o1 = bsmIndex;
            o2 = name;
            o3 = type;
            size = 3;
            hash = tag.tag | ((bsmIndex | name.hashCode() | type.hashCode()) << 1);
        }

        void setConstantDynamic(int bsmIndex, CharSequence name, String type) {
            tag = PoolTag.INVOKEDYNAMIC;
            o1 = bsmIndex;
            o2 = name;
            o3 = type;
            size = 3;
            hash = tag.tag | ((bsmIndex | name.hashCode() | type.hashCode()) << 1);
        }

        void setString(String s) {
            tag = PoolTag.STRING;
            o1 = s;
            size = 1;
            hash = tag.tag | (s.hashCode() << 1);
        }

        void setInteger(Integer i) {
            tag = PoolTag.INTEGER;
            o1 = i;
            size = 1;
            hash = tag.tag | (i.hashCode() << 1);
        }

        void setFloat(Float f) {
            tag = PoolTag.FLOAT;
            o1 = f;
            size = 1;
            hash = tag.tag | (f.hashCode() << 1);
        }

        void setLong(Long l) {
            tag = PoolTag.LONG;
            o1 = l;
            size = 1;
            hash = tag.tag | (l.hashCode() << 1);
        }

        void setDouble(Double d) {
            tag = PoolTag.DOUBLE;
            o1 = d;
            size = 1;
            hash = tag.tag | (d.hashCode() << 1);
        }

        void setMethodType(String type) {
            tag = PoolTag.METHODTYPE;
            o1 = type;
            size = 1;
            hash = tag.tag | (type.hashCode() << 1);
        }

        void setMethodType(MethodType mt) {
            tag = PoolTag.METHODTYPE;
            o1 = mt;
            size = 1;
            hash = tag.tag | (mt.hashCode() << 1);
        }

        void setMethodHandle(int bsmKind, String owner, CharSequence name, String type) {
            tag = PoolTag.METHODHANDLE;
            o1 = bsmKind;
            o2 = owner;
            o3 = name;
            o4 = type;
            size = 4;
            hash = tag.tag | (bsmKind | owner.hashCode() | name.hashCode() | type.hashCode() << 1);
        }

        void setMethodHandle(MethodHandle mh) {
            tag = PoolTag.METHODHANDLE;
            o1 = mh;
            size = 1;
            hash = tag.tag | (mh.hashCode() << 1);
        }

        @Override
        public boolean equals(Object obj) {
            PoolKey that = (PoolKey) obj;
            if (tag != that.tag) return false;
            switch (size) {
                case 1:
                    if (!o1.equals(that.o1)) {
                        return false;
                    }
                    break;
                case 2:
                    if (!o2.equals(that.o2) || !o1.equals(that.o1)) {
                        return false;
                    }
                    break;
                case 3:
                    if (!o3.equals(that.o3) || !o2.equals(that.o2) || !o1.equals(that.o1)) {
                        return false;
                    }
                    break;
                case 4:
                    if (!o4.equals(that.o4) || !o3.equals(that.o3) || !o2.equals(that.o2) || !o1.equals(that.o1)) {
                        return false;
                    }
                    break;
            }
            return true;
        }

        @Override
        PoolKey dup() {
            PoolKey poolKey = new PoolKey();
            poolKey.tag = tag;
            poolKey.size = size;
            poolKey.hash = hash;
            poolKey.o1 = o1;
            poolKey.o2 = o2;
            poolKey.o3 = o3;
            poolKey.o4 = o4;
            return poolKey;
        }
    }

    static class BsmKey extends AbstractKey<BsmKey> {
        String bsmClass;
        CharSequence bsmName;
        String bsmType;
        List<Integer> bsmArgs;

        void set(String bsmClass, CharSequence bsmName, String bsmType, List<Integer> bsmArgs) {
            this.bsmClass = bsmClass;
            this.bsmName = bsmName;
            this.bsmType = bsmType;
            this.bsmArgs = bsmArgs;
            hash = bsmClass.hashCode() | bsmName.hashCode() | bsmType.hashCode() | Objects.hash(bsmArgs);
        }

        @Override
        BsmKey dup() {
            BsmKey bsmKey = new BsmKey();
            bsmKey.bsmClass = bsmClass;
            bsmKey.bsmName = bsmName;
            bsmKey.bsmType = bsmType;
            bsmKey.bsmArgs = bsmArgs;
            bsmKey.hash = hash;
            return bsmKey;
        }

        //TODO: missing hashCode()

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof BsmKey) {
                BsmKey that = (BsmKey)obj;
                return Objects.equals(bsmClass, that.bsmClass) &&
                        Objects.equals(bsmName, that.bsmName) &&
                        Objects.equals(bsmType, that.bsmType) &&
                        Objects.deepEquals(bsmArgs, that.bsmArgs);
            } else {
                return false;
            }
        }
    }

    @Override
    public int putClass(S symbol) {
        return putClassInternal(symbolToString.apply(symbol));
    }

    @Override
    public int putValueClass(S symbol) {
        return putClassInternal("Q" + symbolToString.apply(symbol) + ";");
    }

    private int putClassInternal(String symbol) {
        key.setClass(symbol);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            int utf8_idx = putUtf8(symbol);
            poolKey.at(builder.putClass(utf8_idx));
            entries.enter(poolKey);
        }
        return poolKey.index;
    }

    @Override
    public int putFieldRef(S owner, CharSequence name, T type) {
        return putMemberRef(PoolTag.FIELDREF, owner, name, type);
    }

    @Override
    public int putMethodRef(S owner, CharSequence name, T type, boolean isInterface) {
        return putMemberRef(isInterface ? PoolTag.INTERFACEMETHODREF : PoolTag.METHODREF,
                owner, name, type);
    }

    int putMemberRef(PoolTag poolTag, S owner, CharSequence name, T type) {
        return putMemberRefInternal(poolTag, symbolToString.apply(owner), name, typeToString.apply(type));
    }

    int putMemberRefInternal(PoolTag poolTag, String owner, CharSequence name, String type) {
        key.setMemberRef(poolTag, owner, name, type);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            int owner_idx = putClassInternal(owner);
            int nameAndType_idx = putNameAndType(name, type);
            poolKey.at(builder.putMemberRef(poolTag, owner_idx, nameAndType_idx));
            entries.enter(poolKey);
        }
        return poolKey.index;
    }

    @Override
    public int putInt(int i) {
        key.setInteger(i);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            poolKey.at(builder.putInt(i));
            entries.enter(poolKey);
        }
        return poolKey.index;
    }

    @Override
    public int putFloat(float f) {
        key.setFloat(f);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            poolKey.at(builder.putFloat(f));
            entries.enter(poolKey);
        }
        return poolKey.index;
    }

    @Override
    public int putLong(long l) {
        key.setLong(l);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            poolKey.at(builder.putLong(l));
            entries.enter(poolKey);
        }
        return poolKey.index;
    }

    @Override
    public int putDouble(double d) {
        key.setDouble(d);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            poolKey.at(builder.putDouble(d));
            entries.enter(poolKey);
        }
        return poolKey.index;
    }


    @Override
    public int putInvokeDynamic(CharSequence invokedName, T invokedType, S bsmClass, CharSequence bsmName, T bsmType, Consumer<StaticArgListBuilder<S, T, byte[]>> staticArgs) {
        return putInvokeDynamicInternal(invokedName, typeToString.apply(invokedType), symbolToString.apply(bsmClass), bsmName, typeToString.apply(bsmType), staticArgs);
    }

    @Override
    public int putConstantDynamic(CharSequence constName, T constType, S bsmClass, CharSequence bsmName, T bsmType, Consumer<StaticArgListBuilder<S, T, byte[]>> staticArgs) {
        return putConstantDynamicInternal(constName, typeToString.apply(constType), symbolToString.apply(bsmClass), bsmName, typeToString.apply(bsmType), staticArgs);
    }

    private int putInvokeDynamicInternal(CharSequence invokedName, String invokedType, String bsmClass, CharSequence bsmName, String bsmType, Consumer<StaticArgListBuilder<S, T, byte[]>> staticArgs) {
        int bsmIndex = putBsmInternal(bsmClass, bsmName, bsmType, staticArgs);
        key.setInvokeDynamic(bsmIndex, invokedName, invokedType);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            int nameAndType_idx = putNameAndType(invokedName, invokedType);
            poolKey.at(builder.putInvokeDynamic(bsmIndex, nameAndType_idx));
            entries.enter(poolKey);
        }
        return poolKey.index;
    }

    private int putConstantDynamicInternal(CharSequence constName, String constType, String bsmClass, CharSequence bsmName, String bsmType, Consumer<StaticArgListBuilder<S, T, byte[]>> staticArgs) {
        int bsmIndex = putBsmInternal(bsmClass, bsmName, bsmType, staticArgs);
        key.setConstantDynamic(bsmIndex, constName, constType);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            int nameAndType_idx = putNameAndType(constName, constType);
            poolKey.at(builder.putConstantDynamic(bsmIndex, nameAndType_idx));
            entries.enter(poolKey);
        }
        return poolKey.index;
    }

    private int putBsmInternal(String bsmClass, CharSequence bsmName, String bsmType, Consumer<StaticArgListBuilder<S, T, byte[]>> staticArgs) {
        ByteStaticArgListBuilder staticArgsBuilder = new ByteStaticArgListBuilder();
        staticArgs.accept(staticArgsBuilder);
        List<Integer> static_idxs = staticArgsBuilder.indexes;
        bsmKey.set(bsmClass, bsmName, bsmType, static_idxs);
        BsmKey poolKey = bootstraps.lookup(bsmKey);
        if (poolKey == null) {
            poolKey = bsmKey.dup();
            int bsm_ref = putMethodHandleInternal(MethodHandleInfo.REF_invokeStatic, bsmClass, bsmName, bsmType, false);
            poolKey.at(currentBsmIndex++);
            bootstraps.enter(poolKey);
            bsm_attr.writeChar(bsm_ref);
            bsm_attr.writeChar(static_idxs.size());
            for (int i : static_idxs) {
                bsm_attr.writeChar(i);
            }
        }
        return poolKey.index;
    }
    //where
        class ByteStaticArgListBuilder implements StaticArgListBuilder<S, T, byte[]> {

            List<Integer> indexes = new ArrayList<>();

            @Override
            public ByteStaticArgListBuilder add(int i) {
                indexes.add(putInt(i));
                return this;
            }
            @Override
            public ByteStaticArgListBuilder add(float f) {
                indexes.add(putFloat(f));
                return this;
            }
            @Override
            public ByteStaticArgListBuilder add(long l) {
                indexes.add(putLong(l));
                return this;
            }
            @Override
            public ByteStaticArgListBuilder add(double d) {
                indexes.add(putDouble(d));
                return this;
            }
            @Override
            public ByteStaticArgListBuilder add(String s) {
                indexes.add(putString(s));
                return this;
            }
            @Override
            public StaticArgListBuilder<S, T, byte[]> add(int refKind, S owner, CharSequence name, T type) {
                indexes.add(putMethodHandle(refKind, owner, name, type));
                return this;
            }
            @Override
            public <Z> ByteStaticArgListBuilder add(Z z, ToIntBiFunction<PoolHelper<S, T, byte[]>, Z> poolFunc) {
                indexes.add(poolFunc.applyAsInt(BytePoolHelper.this, z));
                return this;
            }
            @Override
            public ByteStaticArgListBuilder add(CharSequence constName, T constType, S bsmClass, CharSequence bsmName, T bsmType, Consumer<StaticArgListBuilder<S, T, byte[]>> staticArgs) {
                indexes.add(putConstantDynamic(constName, constType, bsmClass, bsmName, bsmType, staticArgs));
                return this;
            }
        }

    @Override
    public int putMethodType(T s) {
        return putMethodTypeInternal(typeToString.apply(s));
    }

    private int putMethodTypeInternal(String s) {
        key.setMethodType(s);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            int desc_idx = putUtf8(s);
            poolKey.at(builder.putMethodType(desc_idx));
            entries.enter(poolKey);
        }
        return poolKey.index;
    }

    @Override
    public int putMethodHandle(int refKind, S owner, CharSequence name, T type) {
        return putMethodHandle(refKind, owner, name, type, false);
    }

    @Override
    public int putMethodHandle(int refKind, S owner, CharSequence name, T type, boolean isInterface) {
        return putMethodHandleInternal(refKind, symbolToString.apply(owner), name, typeToString.apply(type), isInterface);
    }

    private int putMethodHandleInternal(int refKind, String owner, CharSequence name, String type, boolean isInterface) {
        key.setMethodHandle(refKind, owner, name, type);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            int ref_idx = putMemberRefInternal(fromKind(refKind, isInterface), owner, name, type);
            poolKey.at(builder.putMethodHandle(refKind, ref_idx));
            entries.enter(poolKey);
        }
        return poolKey.index;
    }

    @Override
    public int putValue(Object v) {
        if (v instanceof Class<?>) {
            return putClassInternal(((Class<?>) v).getName().replaceAll("\\.", "/"));
        } else if (v instanceof String) {
            key.setString((String) v);
            PoolKey poolKey = entries.lookup(key);
            if (poolKey == null) {
                poolKey = key.dup();
                int utf8_index = putUtf8((String) v);
                poolKey.at(builder.putString(utf8_index));
                entries.enter(poolKey);
            }
            return poolKey.index;
        } else if (v instanceof Integer) {
            key.setInteger((Integer) v);
            PoolKey poolKey = entries.lookup(key);
            if (poolKey == null) {
                poolKey = key.dup();
                poolKey.at(builder.putInt((Integer) v));
                entries.enter(poolKey);
            }
            return poolKey.index;
        } else if (v instanceof Float) {
            key.setFloat((Float) v);
            PoolKey poolKey = entries.lookup(key);
            if (poolKey == null) {
                poolKey = key.dup();
                poolKey.at(builder.putFloat((Float) v));
                entries.enter(poolKey);
            }
            return poolKey.index;
        } else if (v instanceof Double) {
            key.setDouble((Double) v);
            PoolKey poolKey = entries.lookup(key);
            if (poolKey == null) {
                poolKey = key.dup();
                poolKey.at(builder.putDouble((Double) v));
                entries.enter(poolKey);
            }
            return poolKey.index;
        } else if (v instanceof Long) {
            key.setLong((Long) v);
            PoolKey poolKey = entries.lookup(key);
            if (poolKey == null) {
                poolKey = key.dup();
                poolKey.at(builder.putLong((Long) v));
                entries.enter(poolKey);
            }
            return poolKey.index;
        } else if (v instanceof MethodHandle) {
            key.setMethodHandle((MethodHandle) v);
            PoolKey poolKey = entries.lookup(key);
            if (poolKey == null) {
                poolKey = key.dup();
                MethodHandle mh = (MethodHandle) v;
                Member member = memberFromHandle(mh);
                // ## TODO
                String type = null;   // type from handle
                int refKind = 0;      // kind for member
                PoolTag tag = null;   // tag for member
                int ref_idx = putMemberRefInternal(tag,
                    member.getDeclaringClass().getSimpleName(),
                    member.getName(),
                    type);
                poolKey.at(builder.putMethodHandle(refKind, ref_idx));
                entries.enter(poolKey);
            }
            return poolKey.index;
        } else if (v instanceof MethodType) {
            key.setMethodType((MethodType) v);
            PoolKey poolKey = entries.lookup(key);
            if (poolKey == null) {
                poolKey = key.dup();
                MethodType mt = (MethodType) v;
                int desc_idx = putUtf8(mt.toMethodDescriptorString());
                poolKey.at(builder.putMethodType(desc_idx));
                entries.enter(poolKey);
            }
            return poolKey.index;
        } else {
            throw new UnsupportedOperationException("Unsupported object class: " + v.getClass().getName());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Member memberFromHandle(MethodHandle mh) {
        Class<Member>[] targets = new Class[]{Method.class, Field.class, Constructor.class};
        for (Class<Member> target : targets) {
            try {
                return MethodHandles.reflectAs(target, mh);
            } catch (ClassCastException ex) {
                //swallow
            }
        }
        throw new UnsupportedOperationException("Cannot crack method handle!");
    }

    PoolTag fromKind(int bsmKind, boolean isInterface) {
        switch (bsmKind) {
            case 1: // REF_getField
            case 2: // REF_getStatic
            case 3: // REF_putField
            case 4: // REF_putStatic
                return PoolTag.FIELDREF;
            case 5: // REF_invokeVirtual
            case 7: // REF_invokeSpecial
            case 8: // REF_newInvokeSpecial
                return PoolTag.METHODREF;
            case 6: // REF_invokeStatic
            case 9: // REF_invokeInterface
                return isInterface ? PoolTag.INTERFACEMETHODREF : PoolTag.METHODREF;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public int putType(T s) {
        return putUtf8(typeToString.apply(s));
    }

    @Override
    public int putUtf8(CharSequence s) {
        key.setUtf8(s);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            poolKey.at(builder.putUtf8(s));
            entries.enter(poolKey);
        }
        return poolKey.index;
    }

    @Override
    public int putString(String s) {
        key.setString(s);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            int utf8_index = putUtf8(s);
            poolKey.at(builder.putString(utf8_index));
            entries.enter(poolKey);
        }
        return poolKey.index;
    }

    public int putNameAndType(CharSequence name, String type) {
        key.setNameAndType(name, type);
        PoolKey poolKey = entries.lookup(key);
        if (poolKey == null) {
            poolKey = key.dup();
            int name_idx = putUtf8(name);
            int type_idx = putUtf8(type);
            poolKey.at(builder.putNameAndType(name_idx, type_idx));
            entries.enter(poolKey);
        }
        return poolKey.index;
    }

    /**
     * @return the count of constant pool indicies.
     */
    @Override
    public int size() {
        return builder.size();
    }

    /**
     * @return the size in bytes of all constant pool entries.
     */
    @Override
    public byte[] representation() {
        return builder.representation();
    }

    <Z extends ClassBuilder<S, T, Z>> void addAttributes(ClassBuilder<S , T, Z> cb) {
        if (currentBsmIndex > 0) {
            GrowableByteBuffer bsmAttrBuf = new GrowableByteBuffer();
            bsmAttrBuf.writeChar(currentBsmIndex);
            bsmAttrBuf.writeBytes(bsm_attr);
            cb.withAttribute("BootstrapMethods", bsmAttrBuf.bytes());
        }
    }
}
