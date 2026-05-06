/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jcoder;

/**
 * Compiles just 1 source file
 */
class ByteBuffer extends java.io.OutputStream {

    String myname;
    /**
     * The buffer where elements are stored.
     */
    byte data[];
    /**
     * The number of elements in the buffer.
     */
    int length;
    /**
     * The size of the increment. If it is 0 the size of the the buffer is doubled
     * everytime it needs to grow.
     */
    protected int capacityIncrement;

    /**
     * Constructs an empty vector with the specified storage capacity and the specified
     * capacityIncrement.
     *
     * @param initialCapacity the initial storage capacity of the vector
     * @param capacityIncrement how much to increase the element's size by.
     */
    public ByteBuffer(int initialCapacity, int capacityIncrement) {
//      super();
        this.data = new byte[initialCapacity];
        this.capacityIncrement = capacityIncrement;
    }

    /**
     * Constructs an empty vector with the specified storage capacity.
     *
     * @param initialCapacity the initial storage capacity of the vector
     */
    public ByteBuffer(int initialCapacity) {
        this(initialCapacity, 0);
    }

    /**
     * Constructs an empty vector.
     */
    public ByteBuffer() {
        this(30);
    }

    /**
     * Constructs a full vector.
     */
    public ByteBuffer(byte data[], int capacityIncrement) {
        this.length = data.length;
        this.data = data;
        this.capacityIncrement = capacityIncrement;
    }

    /**
     * Constructs a full vector.
     */
    public ByteBuffer(byte data[]) {
        this(data, 0);
    }

    /**
     * Returns the number of elements in the vector. Note that this is not the same as the
     * vector's capacity.
     */
    public final int size() {
        return length;
    }

    /**
     * Ensures that the vector has at least the specified capacity.
     *
     * @param minCapacity the desired minimum capacity
     */
    public final synchronized void ensureCapacity(int minCapacity) {
        int oldCapacity = data.length;
        if (minCapacity <= oldCapacity) {
            return;
        }
        byte oldData[] = data;
        int newCapacity = (capacityIncrement > 0) ? (oldCapacity + capacityIncrement) : (oldCapacity * 2);
        if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
        }
        data = new byte[newCapacity];
        System.arraycopy(oldData, 0, data, 0, length);
    }

    /*======================================*/
    public void write(int val) {
        ensureCapacity(length + 1);
        data[length++] = (byte) val;
    }

    public void writeAt(int index, long val, int width) {
        for (int i = 0; i < width; i++) {
            data[index + i] = (byte) (val >> (width - 1 - i) * 8);
        }
    }

    public void append(long val, int width) {
        ensureCapacity(length + width);
        writeAt(length, val, width);
        length += width;
    }

    /*======================================================*/
} // end ByteBuffer

