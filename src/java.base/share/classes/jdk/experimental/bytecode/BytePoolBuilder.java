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

/**
 * Low-level internal constant pool builder.
 */
class BytePoolBuilder implements PoolBuilder<byte[]> {

    GrowableByteBuffer pool = new GrowableByteBuffer();
    int currentIndex = 1;

    @Override
    public int putClass(int utf8_idx) {
        pool.writeByte(PoolTag.CLASS.tag);
        pool.writeChar(utf8_idx);
        return currentIndex++;
    }

    @Override
    public int putFieldRef(int owner_idx, int nameAndType_idx) {
        return putMemberRef(PoolTag.FIELDREF, owner_idx, nameAndType_idx);
    }

    @Override
    public int putMethodRef(int owner_idx, int nameAndType_idx, boolean isInterface) {
        return putMemberRef((isInterface ? PoolTag.INTERFACEMETHODREF : PoolTag.METHODREF),
                owner_idx, nameAndType_idx);
    }

    @Override
    public int putMemberRef(PoolTag tag, int owner_idx, int nameAndType_idx) {
        pool.writeByte(tag.tag);
        pool.writeChar(owner_idx);
        pool.writeChar(nameAndType_idx);
        return currentIndex++;
    }

    @Override
    public int putInt(int i) {
        pool.writeByte(PoolTag.INTEGER.tag);
        pool.writeInt(i);
        return currentIndex++;
    }

    @Override
    public int putFloat(float f) {
        pool.writeByte(PoolTag.FLOAT.tag);
        pool.writeFloat(f);
        return currentIndex++;
    }

    @Override
    public int putLong(long l) {
        pool.writeByte(PoolTag.LONG.tag);
        pool.writeLong(l);
        int inx = currentIndex;
        currentIndex += 2;
        return inx;
    }

    @Override
    public int putDouble(double d) {
        pool.writeByte(PoolTag.DOUBLE.tag);
        pool.writeDouble(d);
        int inx = currentIndex;
        currentIndex += 2;
        return inx;
    }

    @Override
    public int putInvokeDynamic(int bsmIndex, int nameAndType_idx) {
        pool.writeByte(PoolTag.INVOKEDYNAMIC.tag);
        pool.writeChar(bsmIndex);
        pool.writeChar(nameAndType_idx);
        return currentIndex++;
    }

    @Override
    public int putConstantDynamic(int bsmIndex, int nameAndType_idx) {
        pool.writeByte(PoolTag.CONSTANTDYNAMIC.tag);
        pool.writeChar(bsmIndex);
        pool.writeChar(nameAndType_idx);
        return currentIndex++;
    }

    @Override
    public int putMethodType(int desc_idx) {
        pool.writeByte(PoolTag.METHODTYPE.tag);
        pool.writeChar(desc_idx);
        return currentIndex++;
    }

    @Override
    public int putMethodHandle(int refKind, int ref_idx) {
        pool.writeByte(PoolTag.METHODHANDLE.tag);
        pool.writeByte(refKind);
        pool.writeChar(ref_idx);
        return currentIndex++;
    }

    @Override
    public int putString(int utf8_index) {
        pool.writeByte(PoolTag.STRING.tag);
        pool.writeChar(utf8_index);
        return currentIndex++;
    }

    @Override
    public int putNameAndType(int name_idx, int type_idx) {
        pool.writeByte(PoolTag.NAMEANDTYPE.tag);
        pool.writeChar(name_idx);
        pool.writeChar(type_idx);
        return currentIndex++;
    }

    @Override
    public int putUtf8(CharSequence s) {
        int charLength = s.length();
        if (charLength > 65535) {
            throw new IllegalArgumentException();
        }
        pool.writeByte(PoolTag.UTF8.tag);
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
        return currentIndex++;
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
    private void encodeUTF8(final CharSequence s, int i, int maxByteLength) {
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

    @Override
    public int size() {
        return currentIndex - 1;
    }

    @Override
    public byte[] representation() {
        return pool.bytes();
    }

}
