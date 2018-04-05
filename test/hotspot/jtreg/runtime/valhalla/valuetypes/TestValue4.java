/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
package runtime.valhalla.valuetypes;

import java.nio.ByteBuffer;

final class ContainerValue4 {
    static TestValue4 staticValueField;
    TestValue4 nonStaticValueField;
    TestValue4[] valueArray;
}

public __ByValue final class TestValue4 {

    static __NotFlattened TestValue4 staticValue = getInstance();

    final byte b1;
    final byte b2;
    final byte b3;
    final byte b4;
    final short s1;
    final short s2;
    final int i;
    final long l;
    final String val;

    private TestValue4() {
        i = (int)System.nanoTime();
        val = Integer.valueOf(i).toString();
        l = ((long)i) << Integer.SIZE | i;
        s1 = (short)(i & ~Short.MIN_VALUE);
        s2 = (short)(i >> Short.SIZE);
        b1 = (byte)(i & ~Byte.MIN_VALUE);
        b2 = (byte)((i >> Byte.SIZE) & ~Byte.MIN_VALUE);
        b3 = (byte)((i >> (2 * Byte.SIZE)) & ~Byte.MIN_VALUE);
        b4 = (byte)((i >> (3 * Byte.SIZE)) & ~Byte.MIN_VALUE);
    }

    public static TestValue4 create(int i) {
        TestValue4 v = __MakeDefault TestValue4();
        v = __WithField(v.i, i);
        v = __WithField(v.val, Integer.valueOf(i).toString());
        ByteBuffer bf = ByteBuffer.allocate(8);
        bf.putInt(0, i);
        bf.putInt(4, i);
        v = __WithField(v.l, bf.getLong(0));
        v = __WithField(v.s1, bf.getShort(2));
        v = __WithField(v.s2, bf.getShort(0));
        v = __WithField(v.b1, bf.get(3));
        v = __WithField(v.b2, bf.get(2));
        v = __WithField(v.b3, bf.get(1));
        v = __WithField(v.b4, bf.get(0));
        return v;
    }

    public static TestValue4 create() {
        return create((int)System.nanoTime());
    }

    public static TestValue4 getInstance() {
        return create();
    }

    public static TestValue4 getNonBufferedInstance() {
        return staticValue;
    }

    public boolean verify() {
        if (val == null) {
            return i == 0 && l == 0 && b1 == 0 && b2 == 0 && b3 == 0 && b4 == 0
                    && s1 == 0 && s2 == 0;
        }
        ByteBuffer bf = ByteBuffer.allocate(8);
        bf.putInt(0, i);
        bf.putInt(4, i);
        long nl =  bf.getLong(0);
        bf.clear();
        bf.putShort(0, s2);
        bf.putShort(2, s1);
        int from_s = bf.getInt(0);
        bf.clear();
        bf.put(0, b4);
        bf.put(1, b3);
        bf.put(2, b2);
        bf.put(3, b1);
        int from_b = bf.getInt(0);
        return l == nl && Integer.valueOf(i).toString().compareTo(val) == 0
                && from_s == i && from_b == i;
    }
}
