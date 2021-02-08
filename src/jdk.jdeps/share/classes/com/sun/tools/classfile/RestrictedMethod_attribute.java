/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.tools.classfile;

import java.io.IOException;

public class RestrictedMethod_attribute extends Attribute {

    RestrictedMethod_attribute(ClassReader cr, int name_index, int length) throws IOException {
        super(name_index, length);
        num_params = cr.readUnsignedByte();
        restricted_param_type = new int [num_params];
        for (int i = 0; i < num_params; i++) {
            restricted_param_type[i] = cr.readUnsignedShort();
        }
        restricted_return_type = cr.readUnsignedShort();
    }

    @Override
    public <R, D> R accept(Visitor<R, D> visitor, D data) {
        return visitor.visitRestrictedMethod(this, data);
    }

    public int getParameterCount() {
        return num_params;
    }

    public int getRestrictedParameterType(int i) {
        return restricted_param_type[i];
    }

    public int getRestrictedReturnType() {
        return restricted_return_type;
    }

    int num_params;
    int restricted_param_type[];
    int restricted_return_type;
}
