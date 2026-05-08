/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;

/**
 *
 */
public class DataVector<T extends Data> implements Iterable<T> {

    ArrayList<T> elements;

    public DataVector(int initSize) {
        elements = new ArrayList<>(initSize);
    }

    public DataVector() {
        this(12);
    }

    public Iterator<T> iterator() {
        return elements.iterator();
    }

    public void add(T element) {
        elements.add(element);
    }

    public void addAll(List<T> collection) {
        elements.addAll(collection);
    }

    // full length of the attribute conveyor
    // declared in Data
    public int getLength() {
        int length = 0;
        // calculate overall size here rather than in add()
        // because it may not be available at the time of invoking of add()
        for (T element : elements) {
            length += element.getLength();
        }

        return 2 + length; // add the length of number of elements
    }

    public void write(CheckedDataOutputStream out)
            throws IOException {
        out.writeShort(elements.size());
        writeElements(out);
    }

    public void writeElements(CheckedDataOutputStream out)
            throws IOException {
        for (Data element : elements) {
            element.write(out);
        }
    }

    /* for compatibility with Vector */
    public void addElement(T element) {
        elements.add(element);
    }

    public int size() {
        return elements.size();
    }

    public Data elementAt(int k) {
        return elements.get(k);
    }
}// end class DataVector

