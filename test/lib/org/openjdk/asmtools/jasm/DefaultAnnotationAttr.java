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

/**
 * DefaultAnnotationAttr
 *
 * Used to represent Default Annotation Attributes
 *
 */
public class DefaultAnnotationAttr extends AttrData {

    Data element; // Data

    public DefaultAnnotationAttr(ClassData cls, String name, Data element) {
        super(cls, name);
        this.element = element;
    }

    public DefaultAnnotationAttr(ClassData cls, String name) {
        super(cls, name);
        this.element = null;
    }

    public void add(Data element) {
        this.element = element;
    }

    @Override
    public int attrLength() {
        return element.getLength(); // add the length of number of elements
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        super.write(out);  // attr name, attr len
        element.write(out);
    }
}// end class DataVectorAttr

