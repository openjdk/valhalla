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

import org.openjdk.asmtools.jasm.TypeAnnotationTypes.ETargetType;
import org.openjdk.asmtools.jasm.TypeAnnotationTypes.TypePathEntry;

import java.io.IOException;

/**
 * JVMS 4.7.20.
 * type_annotation {
 *     u1 target_type;
 *     union {
 *         type_parameter_target;
 *         supertype_target;
 *         type_parameter_bound_target;
 *         empty_target;
 *         formal_parameter_target;
 *         throws_target;
 *         localvar_target;
 *         catch_target;
 *         offset_target;
 *         type_argument_target;
 *     } target_info;
 *     type_path target_path;
 *     u2        type_index;
 *     //
 *     //
 *     u2        num_element_value_pairs;
 *     {   u2            element_name_index;
 *         element_value value;
 *     } element_value_pairs[num_element_value_pairs];
 * }
 */
public class TypeAnnotationData extends AnnotationData {

    protected ETargetType targetType;
    protected TypeAnnotationTargetInfoData targetInfo;
    protected TypeAnnotationTypePathData typePath;

    public TypeAnnotationData(Argument typeCPX, boolean invisible) {
        super(typeCPX, invisible);
        typePath = new TypeAnnotationTypePathData();
    }

    @Override
    public int getLength() {
        // lengthOf(annotations[]) + lengthOf(targetType) + lengthOf(targetInfo) + lengthOf(targetInfo)
        return super.getLength() + 1 + targetInfo.getLength() + typePath.getLength();
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        out.writeByte(targetType.value);
        targetInfo.write(out);
        typePath.write(out);
        super.write(out);
    }

    public void addTypePathEntry(TypePathEntry path) {
        typePath.addTypePathEntry(path);
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public String toString(int tabLevel) {
        StringBuilder sb = new StringBuilder(tabString(tabLevel));
        sb.append(targetType.toString()).
                append(' ').
                append(targetInfo.toString(tabLevel)).
                append(typePath.toString(tabLevel));
        return sb.toString();
    }
}
