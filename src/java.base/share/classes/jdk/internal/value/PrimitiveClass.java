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

package jdk.internal.value;

import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.vm.annotation.IntrinsicCandidate;

import java.lang.constant.ClassDesc;

/**
 * Utilities to access Primitive Classes as described in JEP 401.
 */
public class PrimitiveClass {

    /**
     * ACC_PRIMITIVE modifier defined by JEP 401. Subject to change.
     */
    public static final int PRIMITIVE_CLASS = 0x00000800;

    private static final JavaLangAccess javaLangAccess = SharedSecrets.getJavaLangAccess();


    /**
     * Returns a {@code Class} object representing the primary type
     * of this class or interface.
     * <p>
     * If this {@code Class} object represents a primitive type or an array type,
     * then this method returns this class.
     * <p>
     * If this {@code Class} object represents a {@linkplain #isPrimitiveClass(Class)
     * primitive class}, then this method returns the <em>primitive reference type</em>
     * type of this primitive class.
     * <p>
     * Otherwise, this {@code Class} object represents a non-primitive class or interface
     * and this method returns this class.
     *
     * @param aClass a class
     * @return the {@code Class} representing the primary type of
     *         this class or interface
     * @since Valhalla
     */
    @IntrinsicCandidate
    public static Class<?> asPrimaryType(Class<?> aClass) {
        return javaLangAccess.asPrimaryType(aClass);
    }

    /**
     * Returns a {@code Class} object representing the <em>primitive value type</em>
     * of this class if this class is a {@linkplain #isPrimitiveClass(Class)}  primitive class}.
     *
     * @apiNote Alternatively, this method returns null if this class is not
     *          a primitive class rather than throwing UOE.
     *
     * @param aClass a class
     * @return the {@code Class} representing the {@linkplain #isPrimitiveValueType(Class)
     * primitive value type} of this class if this class is a primitive class
     * @throws UnsupportedOperationException if this class or interface
     *         is not a primitive class
     * @since Valhalla
     */
    @SuppressWarnings("unchecked")
    @IntrinsicCandidate
    public static Class<?> asValueType(Class<?> aClass) {
        return javaLangAccess.asValueType(aClass);
    }

    /**
     * Returns {@code true} if this {@code Class} object represents the primary type
     * of this class or interface.
     * <p>
     * If this {@code Class} object represents a primitive type or an array type,
     * then this method returns {@code true}.
     * <p>
     * If this {@code Class} object represents a {@linkplain #isPrimitiveClass(Class)
     * primitive}, then this method returns {@code true} if this {@code Class}
     * object represents a primitive reference type, or returns {@code false}
     * if this {@code Class} object represents a primitive value type.
     * <p>
     * If this {@code Class} object represents a non-primitive class or interface,
     * then this method returns {@code true}.
     *
     * @param aClass a class
     * @return {@code true} if this {@code Class} object represents
     * the primary type of this class or interface
     * @since Valhalla
     */
    public static boolean isPrimaryType(Class<?> aClass) {
        return javaLangAccess.isPrimaryType(aClass);
    }

    /**
     * Returns {@code true} if this {@code Class} object represents
     * a {@linkplain #isPrimitiveClass(Class)  primitive} value type.
     *
     * @return {@code true} if this {@code Class} object represents
     * the value type of a primitive class
     * @since Valhalla
     */
    public static boolean isPrimitiveValueType(Class<?> aClass) {
        return javaLangAccess.isPrimitiveValueType(aClass);
    }

    /**
     * Returns {@code true} if this class is a primitive class.
     * <p>
     * Each primitive class has a {@linkplain #isPrimaryType(Class)  primary type}
     * representing the <em>primitive reference type</em> and a
     * {@linkplain #isPrimitiveValueType(Class)  secondary type} representing
     * the <em>primitive value type</em>.  The primitive reference type
     * and primitive value type can be obtained by calling the
     * {@link #asPrimaryType(Class)} and {@link PrimitiveClass#asValueType} method
     * of a primitive class respectively.
     * <p>
     * A primitive class is a {@linkplain Class#isValue() value class}.
     *
     * @param aClass a class
     * @return {@code true} if this class is a primitive class, otherwise {@code false}
     * @see Class#isValue()
     * @see #asPrimaryType(Class)
     * @see #asValueType(Class)
     * @since Valhalla
     */
    public static boolean isPrimitiveClass(Class<?> aClass) {
        return javaLangAccess.isPrimitiveClass(aClass);
    }

    /**
     * Returns whether this {@linkplain ClassDesc} describes a
     * {@linkplain #isPrimitiveValueType(Class)}  primitive value type}.
     *
     * @return whether this {@linkplain ClassDesc} describes a primitive value type.
     * @since Valhalla
     */
    public static boolean isPrimitiveValueClassDesc(ClassDesc classDesc) {
        return classDesc.descriptorString().startsWith("Q");
    }
}
