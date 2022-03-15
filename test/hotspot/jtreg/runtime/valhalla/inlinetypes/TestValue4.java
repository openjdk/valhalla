/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
package runtime.valhalla.inlinetypes;

import java.nio.ByteBuffer;

final class ContainerValue4 {
    static TestValue4.ref staticInlineField;
    TestValue4 nonStaticInlineField;
    TestValue4[] valueArray;
}

public primitive class TestValue4 {

    static TestValue4.ref staticValue = getInstance();

    final byte b1;
    final byte b2;
    final byte b3;
    final byte b4;
    final short s1;
    final short s2;
    final int i;
    final long l;
    final String val;

    public TestValue4() {
        this((int) System.nanoTime());
    }

    public TestValue4(int i) {
        this.i = i;
        val = Integer.valueOf(i).toString();
        ByteBuffer bf = ByteBuffer.allocate(8);
        bf.putInt(0, i);
        bf.putInt(4, i);
        l = bf.getLong(0);
        s1 = bf.getShort(2);
        s2 = bf.getShort(0);
        b1 = bf.get(3);
        b2 = bf.get(2);
        b3 = bf.get(1);
        b4 = bf.get(0);
    }

    public static TestValue4 getInstance() {
        return new TestValue4();
    }

    public static TestValue4 getNonBufferedInstance() {
        return (TestValue4) staticValue;
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
