/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.classfile.impl;

import java.lang.classfile.constantpool.Utf8Entry;

import static java.lang.classfile.ClassFile.ACC_STATIC;
import static java.lang.classfile.ClassFile.ACC_STRICT;

/**
 * An interface to obtain field properties for direct class builders.
 * Required to filter strict instance fields for stack map generation.
 * Public for benchmark access.
 */
public sealed interface WritableField extends Util.Writable
        permits FieldImpl, DirectFieldBuilder {
    Utf8Entry fieldName();
    Utf8Entry fieldType();
    int fieldFlags();

    static WritableField[] filterStrictInstanceFields(WritableField[] array, int count) {
        // assume there's no toctou for array
        int size = 0;
        for (int i = 0; i < count; i++) {
            var field = array[i];
            if ((field.fieldFlags() & (ACC_STATIC | ACC_STRICT)) == ACC_STRICT) {
                size++;
            }
        }
        WritableField[] ret = new WritableField[size];
        int j = 0;
        for (int i = 0; i < count; i++) {
            var field = array[i];
            if ((field.fieldFlags() & (ACC_STATIC | ACC_STRICT)) == ACC_STRICT) {
                ret[j++] = field;
            }
        }
        return ret;
    }
}
