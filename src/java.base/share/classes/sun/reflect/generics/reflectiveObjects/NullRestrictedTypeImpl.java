/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package sun.reflect.generics.reflectiveObjects;

import java.lang.reflect.NullRestrictedType;
import java.lang.reflect.Type;
import java.util.Objects;

/** Implementing class for NullRestrictedType interface. */

public class NullRestrictedTypeImpl implements NullRestrictedType {
    private final Type baseType;

    private NullRestrictedTypeImpl(Type baseType) {
        this.baseType = baseType;
        checkNonRestricted(baseType);
    }

    private static void checkNonRestricted(Type type) {
        if (type instanceof NullRestrictedType) {
            throw new IllegalArgumentException("Already a null-restricted type");
        }
    }

    /**
     * Static factory. Given a type, creates a null-restricted type
     * @param baseType the type to which the null restriction is applied
     * @throws IllegalArgumentException if {@code baseType} is a null-restricted type
     */
    public static NullRestrictedTypeImpl make(Type baseType) {
        return new NullRestrictedTypeImpl(baseType);
    }

    @Override
    public Type getBaseType() {
        return baseType;
    }

    /*
     * From the JavaDoc for java.lang.reflect.NullRestrictedType
     * "Instances of classes that implement this interface must implement
     * an equals() method that equates any two null rrestricted type instances that
     * share the same base type."
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        return o instanceof NullRestrictedType that &&
                Objects.equals(baseType, that.getBaseType());
    }

    @Override
    public int hashCode() {
        return
            Objects.hashCode(baseType) ^ 17;
    }

    public String toString() {
        return baseType.getTypeName() + "!";
    }
}
