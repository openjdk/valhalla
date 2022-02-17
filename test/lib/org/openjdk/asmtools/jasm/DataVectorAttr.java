/*
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 */
// public class DataVectorAttr extends AttrData implements Constants {
// }
class DataVectorAttr<T extends Data> extends AttrData implements Iterable<T> {

    private ArrayList<T> elements;
    private boolean byteIndex;

    private DataVectorAttr(ClassData cls, String name, boolean byteIndex, ArrayList<T> initialData) {
        super(cls, name);
        this.elements = initialData;
        this.byteIndex = byteIndex;
    }

    DataVectorAttr(ClassData cls, String name, ArrayList<T> initialData) {
        this(cls, name, false, initialData);
    }

    DataVectorAttr(ClassData cls, String name) {
        this(cls, name, false, new ArrayList<>());

    }

    DataVectorAttr(ClassData cls, String name, boolean byteIndex) {
        this(cls, name, byteIndex, new ArrayList<>());

    }

    public T get(int index) {
        return elements.get(index);
    }

    public void add(T element) {
        elements.add(element);
    }

    public void put(int i, T element) {
        elements.set(i, element);
    }

    public int size() {
        return elements.size();
    }

    @Override
    public Iterator<T> iterator() {
        return elements.iterator();
    }

    @Override
    public int attrLength() {
        int length = 0;
        // calculate overall size here rather than in add()
        // because it may not be available at the time of invoking of add()
        for (T elem : elements) {
            length += elem.getLength();
        }

        // add the length of number of elements
        if (byteIndex) {
            length += 1;
        } else {
            length += 2;
        }

        return length;
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        super.write(out);  // attr name, attr len
        if (byteIndex) {
            out.writeByte(elements.size());
        } else {
            out.writeShort(elements.size());
        } // number of elements
        for (T elem : elements) {
            elem.write(out);
        }
    }

}
