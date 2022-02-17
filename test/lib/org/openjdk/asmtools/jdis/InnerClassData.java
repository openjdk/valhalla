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
package org.openjdk.asmtools.jdis;

import static org.openjdk.asmtools.jasm.Tables.*;
import org.openjdk.asmtools.jasm.Modifiers;
import java.io.DataInputStream;
import java.io.IOException;

/**
 *
 */
class InnerClassData extends Indenter {

    ClassData cls;
    int inner_class_info_index;
    int outer_class_info_index;
    int inner_name_index;
    int access;
    /*-------------------------------------------------------- */

    public InnerClassData(ClassData cls) {
        this.cls = cls;
    }

    public void read(DataInputStream in) throws IOException {
        inner_class_info_index = in.readUnsignedShort();
        outer_class_info_index = in.readUnsignedShort();
        inner_name_index = in.readUnsignedShort();
        access = in.readUnsignedShort();
    }  // end read

    public void print() throws IOException {
        boolean pr_cpx = Options.OptionObject().contains(Options.PR.CPX);
        cls.out.print(getIndentString() + Modifiers.accessString(access, CF_Context.CTX_INNERCLASS));
        cls.out.print("InnerClass ");
        if (pr_cpx) {
            if (inner_name_index != 0) {
                cls.out.print("#" + inner_name_index + "= ");
            }
            cls.out.print("#" + inner_class_info_index);
            if (outer_class_info_index != 0) {
                cls.out.print(" of #" + outer_class_info_index);
            }
            cls.out.print("; // ");
        }
        if (inner_name_index != 0) {
            cls.out.print(cls.pool.getName(inner_name_index) + "=");
        }
        if (inner_class_info_index != 0) {
            cls.pool.PrintConstant(cls.out, inner_class_info_index);
        }
        if (outer_class_info_index != 0) {
            cls.out.print(" of ");
            cls.pool.PrintConstant(cls.out, outer_class_info_index);
        }
        if (pr_cpx) {
            cls.out.println();
        } else {
            cls.out.println(";");
        }
    }
} // end InnerClassData

