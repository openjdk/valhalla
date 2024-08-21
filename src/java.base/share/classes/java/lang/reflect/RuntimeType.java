/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.reflect;

import java.lang.constant.Constable;

/**
 * A {@code RuntimeType} represents a stable property of an object reference
 * that can be dynamically checked at run time. These checks may be applied
 * to a {@link Field} or an {@link Array} component.
 *
 * A {@code RuntimeType} might be a class, interface, or array type,
 * represented with a {@link java.lang.Class} object. It might also be a
 * null-restricted class, interface, or array type, which excludes the
 * {@code null} reference. Other kinds of {@code RuntimeType}s may be
 * supported in a future release.
 *
 * @param <T> the compile-time type enforced by this {@code RuntimeType}
 */
public sealed interface RuntimeType<T> extends Constable
                                       permits Class, NullRestrictedClass {

    /**
     * Ensure an object has this type. If not, throw a {@link RuntimeException}.
     *
     * @param ref the object reference to check
     * @return the reference after casting
     */
    T cast(Object ref);

    /**
     * Tests whether a cast to this type will succeed.
     *
     * @param ref the object reference to check
     * @return {@code true} iff the {@link #cast} operation will succeed
     */
    boolean canCast(Object ref);

    /**
     * The most-specific {@code Class} that includes all values of this
     * {@code RuntimeType}.
     *
     * @return a {@code Class}
     */
    Class<?> baseClass();

    /**
     * Create a null-restricted class, interface, or array type.
     *
     * @param <T> the compile-time type of the {@code Class} object
     * @param c   the class to which instances of the {@code RuntimeType}
     *            should belong
     * @return    a null-restricted {@code RuntimeType}
     *
     * @throws IllegalArgumentException if {@code c} represents a primitive type
     */
    public static <T> RuntimeType<T> nullRestricted(Class<T> c) {
        if (c.isPrimitive()) throw new IllegalArgumentException();
        return new NullRestrictedClass<T>(c);
    }

}
