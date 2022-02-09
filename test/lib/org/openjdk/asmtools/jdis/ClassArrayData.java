/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jdis;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Base class of the "classes[]" data of attributes
 * <p>
 * JEP 181 (Nest-based Access Control): class file 55.0
 * NestMembers_attribute {
 * u2 attribute_name_index;
 * u4 attribute_length;
 * u2 number_of_classes;
 * u2 classes[number_of_classes];
 * }
 * <p>
 * JEP 360 (Sealed types): class file 59.65535
 * PermittedSubclasses_attribute {
 * u2 attribute_name_index;
 * u4 attribute_length;
 * u2 number_of_classes;
 * u2 classes[number_of_classes];
 * }
 * </p>
 */
public class ClassArrayData extends Indenter {
    String name;
    ClassData cls;
    int[] classes;
    private Options options = Options.OptionObject();

    protected ClassArrayData(ClassData cls, String attrName) {
        this.cls = cls;
        this.name = attrName;
    }

    public ClassArrayData read(DataInputStream in, int attribute_length) throws IOException, ClassFormatError {
        int number_of_classes = in.readUnsignedShort();
        if (attribute_length != 2 + number_of_classes * 2) {
            throw new ClassFormatError(name + "_attribute: Invalid attribute length");
        }
        classes = new int[number_of_classes];
        for (int i = 0; i < number_of_classes; i++) {
            classes[i] = in.readUnsignedShort();
        }
        return this;
    }

    public void print() {
        String indexes = "";
        String names = "";
        boolean pr_cpx = options.contains(Options.PR.CPX);
        cls.out.print(getIndentString() + name + " ");
        for (int i = 0; i < classes.length; i++) {
            if (pr_cpx) {
                indexes += (indexes.isEmpty() ? "" : ", ") + "#" + classes[i];
            }
            names += (names.isEmpty() ? "" : ", ") + cls.pool.StringValue(classes[i]);
        }
        if (pr_cpx) {
            cls.out.print(indexes + "; // ");
        }
        cls.out.print(names);
        if (pr_cpx) {
            cls.out.println();
        } else {
            cls.out.println(";");
        }
    }

}
