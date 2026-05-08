/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * JVMS 4.7.16.
 *
 * annotation {
 *     u2 type_index;
 *     u2 num_element_value_pairs;
 *     {   u2            element_name_index;
 *         element_value value;
 *     } element_value_pairs[num_element_value_pairs];
 * }
 */
class AnnotationData implements Data {

    boolean invisible;
    Argument typeCPX;
    ArrayList<ElemValuePair> elemValuePairs;
    int annotationLength = 0;

    /**
     * AnnotationElemValue
     *
     * Used to store Annotation Data
     */
    static public class ElemValuePair implements Data {

        ConstantPool.ConstCell name;
        Data value;

        public ElemValuePair(ConstantPool.ConstCell name, Data value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            name.write(out);
            value.write(out);
        }

        @Override
        public int getLength() {
            return 2 + value.getLength();
        }
    }

    public AnnotationData(Argument typeCPX, boolean invisible) {
        this.typeCPX = typeCPX;
        this.elemValuePairs = new ArrayList<>();
        this.invisible = invisible;
    }

    public void add(ElemValuePair elemValuePair) {
        elemValuePairs.add(elemValuePair);
        annotationLength += elemValuePair.getLength();
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        out.writeShort(typeCPX.arg);
        out.writeShort(elemValuePairs.size());

        for (Data pair : elemValuePairs) {
            pair.write(out);
        }
    }

    @Override
    public int getLength() {
        return 4 + annotationLength;
    }
}
