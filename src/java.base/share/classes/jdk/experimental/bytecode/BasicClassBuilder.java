/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import jdk.experimental.bytecode.AnnotationsBuilder.Kind;
import jdk.experimental.bytecode.MacroCodeBuilder.CondKind;
import jdk.experimental.bytecode.MacroCodeBuilder.FieldAccessKind;
import jdk.experimental.bytecode.MacroCodeBuilder.InvocationKind;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Iterator;

public class BasicClassBuilder extends ClassBuilder<String, String, BasicClassBuilder> {

    public BasicClassBuilder(String thisClass, int majorVersion, int minorVersion) {
        this();
        withMinorVersion(minorVersion);
        withMajorVersion(majorVersion);
        withThisClass(thisClass);
    }

    public BasicClassBuilder() {
        super(new BasicPoolHelper(), new BasicTypeHelper());
    }

    public static class BasicPoolHelper implements PoolHelper<String, String, byte[]> {
        GrowableByteBuffer pool = new GrowableByteBuffer();
        //Map<PoolKey, PoolKey> indicesMap = new HashMap<>();
        int currentIndex = 1;

        PoolKey[] table = new PoolKey[0x10];
        int nelems;

        private void dble() {
            PoolKey[] oldtable = table;
            table = new PoolKey[oldtable.length * 2];
            int n = 0;
            for (int i = oldtable.length; --i >= 0; ) {
                PoolKey e = oldtable[i];
                if (e != null) {
                    table[getIndex(e)] = e;
                    n++;
                }
            }
            // We don't need to update nelems for shared inherited scopes,
            // since that gets handled by leave().
            nelems = n;
        }

        /**
         * Look for slot in the table.
         * We use open addressing with double hashing.
         */
        int getIndex(PoolKey e) {
            int hashMask = table.length - 1;
            int h = e.hashCode();
            int i = h & hashMask;
            // The expression below is always odd, so it is guaranteed
            // to be mutually prime with table.length, a power of 2.
            int x = hashMask - ((h + (h >> 16)) << 1);
            for (; ; ) {
                PoolKey e2 = table[i];
                if (e2 == null)
                    return i;
                else if (e.hash == e2.hash)
                    return i;
                i = (i + x) & hashMask;
            }
        }

        public void enter(PoolKey e) {
            if (nelems * 3 >= (table.length - 1) * 2)
                dble();
            int hash = getIndex(e);
            PoolKey old = table[hash];
            if (old == null) {
                nelems++;
            }
            e.next = old;
            table[hash] = e;
        }

        protected PoolKey lookup(PoolKey other) {
            PoolKey e = table[getIndex(other)];
            while (e != null && !e.equals(other))
                e = e.next;
            return e;
        }


        PoolKey key = new PoolKey();

        protected static class PoolKey {
            PoolTag tag;
            Object o1;
            Object o2;
            Object o3;
            int size = -1;
            int hash;
            int index = -1;
            PoolKey next;

            void setUtf8(CharSequence s) {
                tag = PoolTag.CONSTANT_UTF8;
                o1 = s;
                size = 1;
                hash = tag.tag | (s.hashCode() << 1);
            }

            void setClass(String clazz) {
                tag = PoolTag.CONSTANT_CLASS;
                o1 = clazz;
                size = 1;
                hash = tag.tag | (clazz.hashCode() << 1);
            }

            void setNameAndType(CharSequence name, String type) {
                tag = PoolTag.CONSTANT_NAMEANDTYPE;
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

            void setString(String s) {
                tag = PoolTag.CONSTANT_STRING;
                o1 = s;
                size = 1;
                hash = tag.tag | (s.hashCode() << 1);
            }

            void setInteger(Integer i) {
                tag = PoolTag.CONSTANT_INTEGER;
                o1 = i;
                size = 1;
                hash = tag.tag | (i.hashCode() << 1);
            }

            void setFloat(Float f) {
                tag = PoolTag.CONSTANT_FLOAT;
                o1 = f;
                size = 1;
                hash = tag.tag | (f.hashCode() << 1);
            }

            void setLong(Long l) {
                tag = PoolTag.CONSTANT_LONG;
                o1 = l;
                size = 1;
                hash = tag.tag | (l.hashCode() << 1);
            }

            void setDouble(Double d) {
                tag = PoolTag.CONSTANT_DOUBLE;
                o1 = d;
                size = 1;
                hash = tag.tag | (d.hashCode() << 1);
            }

            void setMethodHandle(MethodHandle mh) {
                tag = PoolTag.CONSTANT_METHODHANDLE;
                o1 = mh;
                size = 1;
                hash = tag.tag | (mh.hashCode() << 1);
            }

            void setMethodType(MethodType mt) {
                tag = PoolTag.CONSTANT_METHODTYPE;
                o1 = mt;
                size = 1;
                hash = tag.tag | (mt.hashCode() << 1);
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
                }
                return true;
            }

            @Override
            public int hashCode() {
                return hash;
            }

            PoolKey dup() {
                PoolKey poolKey = new PoolKey();
                poolKey.tag = tag;
                poolKey.size = size;
                poolKey.hash = hash;
                poolKey.o1 = o1;
                poolKey.o2 = o2;
                poolKey.o3 = o3;
                return poolKey;
            }

            void at(int index) {
                this.index = index;
            }
        }

        @Override
        public int putClass(String symbol) {
            key.setClass(symbol);
            PoolKey poolKey = lookup(key);
            if (poolKey == null) {
                poolKey = key.dup();
                int utf8_idx = putUtf8(symbol);
                poolKey.at(currentIndex++);
                enter(poolKey);
                pool.writeByte(PoolTag.CONSTANT_CLASS.tag);
                pool.writeChar(utf8_idx);
            }
            return poolKey.index;
        }

        @Override
        public int putFieldRef(String owner, CharSequence name, String type) {
            return putMemberRef(PoolTag.CONSTANT_FIELDREF, owner, name, type);
        }

        @Override
        public int putMethodRef(String owner, CharSequence name, String type, boolean isInterface) {
            return putMemberRef(isInterface ? PoolTag.CONSTANT_INTERFACEMETHODREF : PoolTag.CONSTANT_METHODREF,
                    owner, name, type);
        }

        int putMemberRef(PoolTag poolTag, String owner, CharSequence name, String type) {
            key.setMemberRef(poolTag, owner, name, type);
            PoolKey poolKey = lookup(key);
            if (poolKey == null) {
                poolKey = key.dup();
                int owner_idx = putClass(owner);
                int nameAndType_idx = putNameAndType(name, type);
                poolKey.at(currentIndex++);
                enter(poolKey);
                pool.writeByte(poolTag.tag);
                pool.writeChar(owner_idx);
                pool.writeChar(nameAndType_idx);
            }
            return poolKey.index;
        }

        @Override
        public int putValue(Object v) {
            if (v instanceof Class<?>) {
                return putClass(((Class<?>) v).getName().replaceAll("\\.", "/"));
            } else if (v instanceof String) {
                key.setString((String) v);
                PoolKey poolKey = lookup(key);
                if (poolKey == null) {
                    poolKey = key.dup();
                    int utf8_index = putUtf8((String) v);
                    poolKey.at(currentIndex++);
                    enter(poolKey);
                    pool.writeByte(PoolTag.CONSTANT_STRING.tag);
                    pool.writeChar(utf8_index);
                }
                return poolKey.index;
            } else if (v instanceof Integer) {
                key.setInteger((Integer) v);
                PoolKey poolKey = lookup(key);
                if (poolKey == null) {
                    poolKey = key.dup();
                    poolKey.at(currentIndex++);
                    enter(poolKey);
                    pool.writeByte(PoolTag.CONSTANT_INTEGER.tag);
                    pool.writeInt((Integer) v);
                }
                return poolKey.index;
            } else if (v instanceof Float) {
                key.setFloat((Float) v);
                PoolKey poolKey = lookup(key);
                if (poolKey == null) {
                    poolKey = key.dup();
                    poolKey.at(currentIndex++);
                    enter(poolKey);
                    pool.writeByte(PoolTag.CONSTANT_FLOAT.tag);
                    pool.writeFloat((Float) v);
                }
                return poolKey.index;
            } else if (v instanceof Double) {
                key.setDouble((Double) v);
                PoolKey poolKey = lookup(key);
                if (poolKey == null) {
                    poolKey = key.dup();
                    poolKey.at(currentIndex++);
                    enter(poolKey);
                    pool.writeByte(PoolTag.CONSTANT_DOUBLE.tag);
                    pool.writeDouble((Double) v);
                    currentIndex++;
                }
                return poolKey.index;
            } else if (v instanceof Long) {
                key.setLong((Long) v);
                PoolKey poolKey = lookup(key);
                if (poolKey == null) {
                    poolKey = key.dup();
                    poolKey.at(currentIndex++);
                    enter(poolKey);
                    pool.writeByte(PoolTag.CONSTANT_LONG.tag);
                    pool.writeLong((Long) v);
                    currentIndex++;
                }
                return poolKey.index;
            } else if (v instanceof MethodHandle) {
                key.setMethodHandle((MethodHandle) v);
                PoolKey poolKey = lookup(key);
                if (poolKey == null) {
                    poolKey = key.dup();
                    MethodHandle mh = (MethodHandle) v;
                    Member member = memberFromHandle(mh);
                    int member_ref_idx = putMemberRef(tagForMember(member),
                            member.getDeclaringClass().getSimpleName(),
                            member.getName(),
                            typeFromHandle(mh));
                    poolKey.at(currentIndex++);
                    enter(poolKey);
                    pool.writeByte(PoolTag.CONSTANT_METHODHANDLE.tag);
                    pool.writeByte(kindForMember(member));
                    pool.writeChar(member_ref_idx);
                }
                return poolKey.index;
            } else if (v instanceof MethodType) {
                key.setMethodType((MethodType) v);
                PoolKey poolKey = lookup(key);
                if (poolKey == null) {
                    poolKey = key.dup();
                    MethodType mt = (MethodType) v;
                    int methdodType_idx = putUtf8(mt.toMethodDescriptorString());
                    poolKey.at(currentIndex++);
                    enter(poolKey);
                    pool.writeByte(PoolTag.CONSTANT_METHODTYPE.tag);
                    pool.writeChar(methdodType_idx);
                }
                return poolKey.index;
            } else {
                throw new UnsupportedOperationException("Unsupported object class: " + v.getClass().getName());
            }
        }

        private String typeFromHandle(MethodHandle mh) {
            return null;
        } //TODO

        private int kindForMember(Member member) {
            return 0;
        } //TODO

        private PoolTag tagForMember(Member member) {
            return null; //TODO
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

        @Override
        public int putInvokeDynamic(CharSequence invokedName, String invokedType, int bsmKind, String bsmClass, CharSequence bsmName, String bsmType, Object... staticArgs) {
            return 0; //TODO
        }

        @Override
        public int putMethodType(String s) {
            return 0; //TODO
        }

        @Override
        public int putHandle(int refKind, String owner, CharSequence name, String type) {
            return 0; //TODO
        }

        @Override
        public int putType(String s) {
            return putUtf8(s);
        }

        public int putUtf8(CharSequence s) {
            key.setUtf8(s);
            PoolKey poolKey = lookup(key);
            if (poolKey == null) {
                poolKey = key.dup();
                poolKey.at(currentIndex++);
                enter(poolKey);
                pool.writeByte(PoolTag.CONSTANT_UTF8.tag);
                putUTF8Internal(s);
            }
            return poolKey.index;
        }

        /**
         * Puts an UTF8 string into this byte vector. The byte vector is
         * automatically enlarged if necessary.
         *
         * @param s a String whose UTF8 encoded length must be less than 65536.
         * @return this byte vector.
         */
        void putUTF8Internal(final CharSequence s) {
            int charLength = s.length();
            if (charLength > 65535) {
                throw new IllegalArgumentException();
            }
            // optimistic algorithm: instead of computing the byte length and then
            // serializing the string (which requires two loops), we assume the byte
            // length is equal to char length (which is the most frequent case), and
            // we start serializing the string right away. During the serialization,
            // if we find that this assumption is wrong, we continue with the
            // general method.
            pool.writeChar(charLength);
            for (int i = 0; i < charLength; ++i) {
                char c = s.charAt(i);
                if (c >= '\001' && c <= '\177') {
                    pool.writeByte((byte) c);
                } else {
                    encodeUTF8(s, i, 65535);
                    break;
                }
            }
        }

        /**
         * Puts an UTF8 string into this byte vector. The byte vector is
         * automatically enlarged if necessary. The string length is encoded in two
         * bytes before the encoded characters, if there is space for that (i.e. if
         * this.length - i - 2 >= 0).
         *
         * @param s             the String to encode.
         * @param i             the index of the first character to encode. The previous
         *                      characters are supposed to have already been encoded, using
         *                      only one byte per character.
         * @param maxByteLength the maximum byte length of the encoded string, including the
         *                      already encoded characters.
         * @return this byte vector.
         */
        void encodeUTF8(final CharSequence s, int i, int maxByteLength) {
            int charLength = s.length();
            int byteLength = i;
            char c;
            for (int j = i; j < charLength; ++j) {
                c = s.charAt(j);
                if (c >= '\001' && c <= '\177') {
                    byteLength++;
                } else if (c > '\u07FF') {
                    byteLength += 3;
                } else {
                    byteLength += 2;
                }
            }
            if (byteLength > maxByteLength) {
                throw new IllegalArgumentException();
            }
            int byteLengthFinal = byteLength;
            pool.withOffset(pool.offset - i - 2, buf -> buf.writeChar(byteLengthFinal));
            for (int j = i; j < charLength; ++j) {
                c = s.charAt(j);
                if (c >= '\001' && c <= '\177') {
                    pool.writeChar((byte) c);
                } else if (c > '\u07FF') {
                    pool.writeChar((byte) (0xE0 | c >> 12 & 0xF));
                    pool.writeChar((byte) (0x80 | c >> 6 & 0x3F));
                    pool.writeChar((byte) (0x80 | c & 0x3F));
                } else {
                    pool.writeChar((byte) (0xC0 | c >> 6 & 0x1F));
                    pool.writeChar((byte) (0x80 | c & 0x3F));
                }
            }
        }

        int putNameAndType(CharSequence name, String type) {
            key.setNameAndType(name, type);
            PoolKey poolKey = lookup(key);
            if (poolKey == null) {
                poolKey = key.dup();
                int name_idx = putUtf8(name);
                int type_idx = putUtf8(type);
                poolKey.at(currentIndex++);
                enter(poolKey);
                pool.writeByte(PoolTag.CONSTANT_NAMEANDTYPE.tag);
                pool.writeChar(name_idx);
                pool.writeChar(type_idx);
            }
            return poolKey.index;
        }

        @Override
        public int size() {
            return currentIndex - 1;
        }

        @Override
        public byte[] entries() {
            return pool.bytes();
        }
    }

    public static class BasicTypeHelper implements TypeHelper<String, String> {

        @Override
        public String elemtype(String s) {
            if (!s.startsWith("[")) throw new IllegalStateException("elemtype found: " + s);
            return s.substring(1);
        }

        @Override
        public String arrayOf(String s) {
            return "[" + s;
        }

        @Override
        public String type(String s) {
            return "L" + s + ";";
        }

        @Override
        public TypeTag tag(String s) {
            switch (s.charAt(0)) {
                case '[':
                case 'L':
                    return TypeTag.A;
                case 'B':
                case 'C':
                case 'Z':
                case 'S':
                case 'I':
                    return TypeTag.I;
                case 'F':
                    return TypeTag.F;
                case 'J':
                    return TypeTag.J;
                case 'D':
                    return TypeTag.D;
                case 'V':
                    return TypeTag.V;
                case 'Q':
                    return TypeTag.Q;
                default:
                    throw new IllegalStateException("Bad type: " + s);
            }
        }

        @Override
        public String nullType() {
            return "<null>";
        }

        @Override
        public String commonSupertype(String t1, String t2) {
            if (t1.equals(t2)) {
                return t1;
            } else {
                try {
                    Class<?> c1 = from(t1);
                    Class<?> c2 = from(t2);
                    if (c1.isAssignableFrom(c2)) {
                        return t1;
                    } else if (c2.isAssignableFrom(c1)) {
                        return t2;
                    } else {
                        return "Ljava/lang/Object;";
                    }
                } catch (Exception e) {
                    return null;
                }
            }
        }

        public Class<?> from(String desc) throws ReflectiveOperationException {
            if (desc.startsWith("[")) {
                return Class.forName(desc.replaceAll("/", "."));
            } else {
                return Class.forName(symbol(desc).replaceAll("/", "."));
            }
        }

        @Override
        public Iterator<String> parameterTypes(String s) {
            return new Iterator<String>() {
                int ch = 1;

                @Override
                public boolean hasNext() {
                    return s.charAt(ch) != ')';
                }

                @Override
                public String next() {
                    char curr = s.charAt(ch);
                    switch (curr) {
                        case 'C':
                        case 'B':
                        case 'S':
                        case 'I':
                        case 'J':
                        case 'F':
                        case 'D':
                        case 'Z':
                            ch++;
                            return String.valueOf(curr);
                        case '[':
                            ch++;
                            return "[" + next();
                        case 'L':
                        case 'Q':
                            StringBuilder builder = new StringBuilder();
                            while (curr != ';') {
                                builder.append(curr);
                                curr = s.charAt(++ch);
                            }
                            builder.append(';');
                            ch++;
                            return builder.toString();
                        default:
                            throw new AssertionError("cannot parse string: " + s);
                    }
                }
            };
        }

        @Override
        public String symbolFrom(String s) {
            return s;
        }

        @Override
        public String fromTag(TypeTag tag) {
            return tag.name();
        }

        @Override
        public String symbol(String type) {
            return (type.startsWith("L") || type.startsWith("Q")) ?
                    type.substring(1, type.length() - 1) : type;
        }

        @Override
        public String returnType(String s) {
            return s.substring(s.indexOf(')') + 1, s.length());
        }
    }

    static class MyAttribute {

        int n;

        public MyAttribute(int n) {
            this.n = n;
        }

        static final String name = "MyAttr";

        void write(PoolHelper<String, String, byte[]> poolHelper, GrowableByteBuffer buf) {
            buf.writeInt(n);
        }
    }

    public static void main(String[] args) {
        byte[] byteArray = new BasicClassBuilder("Foo", 51, 0)
                .withSuperclass("java/lang/Object")
                .withSuperinterface("java/lang/Runnable")
                .withAnnotation(Kind.RUNTIME_VISIBLE, "LMyAnno;", EB -> EB.withPrimitive("i", 10).withEnum("en", "LE;", 10))
                .withAttribute(MyAttribute.name, new MyAttribute(42), MyAttribute::write)
                .withField("f", "Ljava/lang/String;")
                .withMethod("foo", "(IIJ)V", M ->
                        M.withFlags(Flag.ACC_STATIC)
                                .withParameterAnnotation(Kind.RUNTIME_VISIBLE, 0, "LMyAnno2;")
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.load(0).i2l().load(1).i2l().load(2).invokestatic("Foo", "m", "(JJJ)V", false).return_()
                                ))
                .withMethod("<init>", "()V", M ->
                        M.withFlags(Flag.ACC_PUBLIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.load(0).dup()
                                                .invokespecial("java/lang/Object", "<init>", "()V", false)
                                                .ldc("Hello!")
                                                .putfield(FieldAccessKind.INSTANCE, "Foo", "f", "Ljava/lang/String;")
                                                .return_()
                                ))
                .withMethod("run", "()V", M ->
                        M.withFlags(Flag.ACC_PUBLIC)
                                .withExceptions("java/io/IOException", "java/lang/ReflectiveOperationException")
                                .withCode(TypedCodeBuilder::new, C ->
                                        C.load(0)
                                                .invoke(InvocationKind.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false)
                                                .pop()
                                                .return_()
                                ))
                .withMethod("main", "([Ljava/lang/String;)V", M ->
                        M.withFlags(Flag.ACC_PUBLIC, Flag.ACC_STATIC)
                                .withCode(TypedCodeBuilder::new, C ->
                                                C.new_("Foo")
                                                        .dup()
                                                        .invoke(InvocationKind.INVOKESPECIAL, "Foo", "<init>", "()V", false)
                                                        .invoke(InvocationKind.INVOKEVIRTUAL, "Foo", "run", "()V", false)
                                                        .label("hey")
                                                        .iconst_0()
                                                        .iconst_0()
                                                        .typed(TypeTag.I)
                                                        .if_acmpne("hey")
                                                        .const_(10)
                                                        .typed(TypeTag.I)
                                                        .astore_1()
                                                        .typed(TypeTag.I)
                                                        .aload_1()
                                                        .typed(TypeTag.F)
                                                        .anewarray("F")
                                                        .typed(TypeTag.F)
                                                        .aconst_null()
                                                        .typed(TypeTag.F)
                                                        .aaload()
                                                        .pop()
                                                        .const_(1)
                                                        .pop()
                                                        .withLocal("50", "B")
                                                        .const_(50)
                                                        .store("50")
                                                        .withLocal("150", "S")
                                                        .const_(150)
                                                        .store("150")
                                                        .const_(500000000)
                                                        .switch_(S ->
                                                                S.withCase(1, one ->
                                                                        one.const_(42)
                                                                                .withLocal("foo", "I")
                                                                                .store("foo"), false)
                                                                        .withCase(3, three ->
                                                                                three.nop()
                                                                                        .nop(), false)
                                                                        .withDefault(D ->
                                                                                D.nop()
                                                                                        .nop()
                                                                                        .nop()))
                                                        .label("endSwitch2")
                                                        .const_(50f)
                                                        .const_(1f)
                                                        .ifcmp(TypeTag.F, CondKind.GT, "bar")
                                                        .goto_("hey")
                                                        .label("bar")
                                                        .aload_0()
                                                        .invoke(InvocationKind.INVOKEINTERFACE, "Bar", "name", "()J", true)
                                                        .return_(TypeTag.J)
                                ))
                .build();
        try (FileOutputStream fos = new FileOutputStream("Foo.class")) {
            fos.write(byteArray);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
