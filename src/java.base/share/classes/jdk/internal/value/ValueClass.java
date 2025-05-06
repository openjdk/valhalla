/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.value;

import jdk.internal.access.JavaLangReflectAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.vm.annotation.IntrinsicCandidate;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

/**
 * Utilities to access package private methods of java.lang.Class and related reflection classes.
 */
public class ValueClass {
    private static final JavaLangReflectAccess JLRA = SharedSecrets.getJavaLangReflectAccess();

    /**
     * {@return {@code true} if the field is NullRestricted}
     */
    public static boolean isNullRestrictedField(Field f) {
        return JLRA.isNullRestrictedField(f);
    }

    /**
     * Allocate an array of a value class type with components that behave in
     * the same way as a {@link jdk.internal.vm.annotation.NullRestricted}
     * field.
     * <p>
     * Because these behaviors are not specified by Java SE, arrays created with
     * this method should only be used by internal JDK code for experimental
     * purposes and should not affect user-observable outcomes.
     *
     * @throws IllegalArgumentException if {@code componentType} is not a
     *                                  value class type.
     */
    @IntrinsicCandidate
    public static native Object[] newNullRestrictedAtomicArray(Class<?> componentType,
                                                               int length, Object initVal);

    @IntrinsicCandidate
    public static native Object[] newNullRestrictedNonAtomicArray(Class<?> componentType,
                                                                  int length, Object initVal);

    @IntrinsicCandidate
    public static native Object[] newNullableAtomicArray(Class<?> componentType,
                                                         int length);

    public static native boolean isFlatArray(Object array);

    public static Object[] copyOfSpecialArray(Object[] array, int from, int to) {
        return copyOfSpecialArray0(array, from, to);
    }

    private static native Object[] copyOfSpecialArray0(Object[] array, int from, int to);

    /**
     * {@return true if the given array is a null-restricted array}
     */
    public static native boolean isNullRestrictedArray(Object array);

    /**
     * {@return true if the given array uses a layout designed for atomic accesses }
     */
    public static native boolean isAtomicArray(Object array);
}
