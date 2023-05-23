/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.classfile.attribute;

import jdk.internal.classfile.Attribute;
import jdk.internal.classfile.CodeModel;
import jdk.internal.classfile.Label;
import jdk.internal.classfile.impl.BoundAttribute;

/**
 * Models the {@code Code} attribute {@jvms 4.7.3}, appears on non-native,
 * non-abstract methods and contains the bytecode of the method body.  Delivered
 * as a {@link jdk.internal.classfile.MethodElement} when traversing the elements of a
 * {@link jdk.internal.classfile.MethodModel}.
 */
public sealed interface CodeAttribute extends Attribute<CodeAttribute>, CodeModel
        permits BoundAttribute.BoundCodeAttribute {

    /**
     * {@return The length of the code array in bytes}
     */
    int codeLength();

    /**
     * {@return the bytes (bytecode) of the code array}
     */
    byte[] codeArray();

    /**
     * {@return the position of the {@code Label} in the {@code codeArray}
     * or -1 if the {@code Label} does not point to the {@code codeArray}}
     * @param label a marker for a position within this {@code CodeAttribute}
     */
    int labelToBci(Label label);
}
