/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.IntrinsicCandidate;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

/**
 * Utilities to access
 */
public class ValueClass {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final JavaLangReflectAccess JLRA = SharedSecrets.getJavaLangReflectAccess();

    /**
     * {@return true if the given {@code Class} object is implicitly constructible}
     */
    public static native boolean isImplicitlyConstructible(Class<?> cls);

    /**
     * {@eturn the default value of the given value class type}
     *
     * @throws IllegalArgumentException if {@code cls} is not a
     *         value class type or is not annotated with
     *         {@link jdk.internal.vm.annotation.ImplicitlyConstructible}
     */
    public static <T> T zeroInstance(Class<T> cls) {
        if (!cls.isValue()) {
            throw new IllegalArgumentException(cls.getName() + " not a value class");
        }
        if (!isImplicitlyConstructible(cls)) {
            throw new IllegalArgumentException(cls.getName() + " not implicitly constructible");
        }
        UNSAFE.ensureClassInitialized(cls);
        return UNSAFE.uninitializedDefaultValue(cls);
    }

    /**
     * {@return {@code CheckedType} representing the type of the given field}
     */
    public static CheckedType checkedType(Field f) {
        return JLRA.isNullRestrictedField(f) ? NullRestrictedCheckedType.of(f.getType())
                                             : NormalCheckedType.of(f.getType());
    }

    /**
     * {@return {@code CheckedType} representing the component type of the given array}
     */
    public static CheckedType componentCheckedType(Object array) {
        Class<?> componentType = array.getClass().getComponentType();
        return isNullRestrictedArray(array) ? NullRestrictedCheckedType.of(componentType)
                                            : NormalCheckedType.of(componentType);
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
     *         value class type or is not annotated with
     *         {@link jdk.internal.vm.annotation.ImplicitlyConstructible}
     */
    @SuppressWarnings("unchecked")
    public static Object[] newArrayInstance(CheckedType componentType, int length) {
        if (componentType instanceof NullRestrictedCheckedType) {
            return newNullRestrictedArray(componentType.boundingClass(), length);
        } else {
            return (Object[]) Array.newInstance(componentType.boundingClass(), length);
        }
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
     *         value class type or is not annotated with
     *         {@link jdk.internal.vm.annotation.ImplicitlyConstructible}
     */
    @IntrinsicCandidate
    public static native Object[] newNullRestrictedArray(Class<?> componentType,
                                                         int length);

    /**
     * {@return true if the given array is a null-restricted array}
     */
    public static native boolean isNullRestrictedArray(Object array);
}
