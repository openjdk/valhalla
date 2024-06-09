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

package jdk.internal.value;

public final class NullRestrictedCheckedType implements CheckedType {
    private final Class<?> type;
    private NullRestrictedCheckedType(Class<?> cls) {
        this.type = cls;
    }

    @Override
    public Object cast(Object obj) {
        if (obj == null) {
            throw new NullPointerException("null not allowed for null-restricted type " + type.getName());
        }
        return type.cast(obj);
    }

    @Override
    public boolean canCast(Object obj) {
        if (obj == null) return false;
        return type.isAssignableFrom(obj.getClass());
    }

    @Override
    public Class<?> boundingClass() {
        return type;
    }

    /**
     * {@returns a {@linkplain NullRestrictedCheckedType null-restricted checked type}
     * for the given class if it is a checked type}
     *
     * A primitive type and {@code void} is not a checked type.
     *
     * @param cls {@code Class} object
     * @throws IllegalArgumentException if the given class is a primitive type
     * @throws IllegalArgumentException if the given class is not a value class
     */
    public static NullRestrictedCheckedType of(Class<?> cls) {
        if (cls.isPrimitive())
            throw new IllegalArgumentException(cls.getName() + " not a checked type");
        if (!cls.isValue()) {
            throw new IllegalArgumentException(cls.getName() + " not a value class");
        }
        return new NullRestrictedCheckedType(cls);
    }
}
