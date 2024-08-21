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

import java.lang.constant.*;
import java.util.Optional;
import java.util.Objects;

/**
 * A {@code NullRestrictedClass} represents the null-restricted type of a class,
 * interface, or array type.
 */
final class NullRestrictedClass<T> implements RuntimeType<T> {

    private final Class<T> c;

    /**
     * Assumes {@code c} is a class, interface, or array type, not a primitive.
     */
    NullRestrictedClass(Class<T> c) {
        this.c = c;
    }

    /**
     * Ensure an object is non-null and an instance of the class.
     * @throws NullPointerException if {@code arg} is {@code null}
     * @throws ClassCastException if {@code arg} is an instance of an incompatible class
     */
    public T cast(Object arg) {
        if (arg == null) throw new NullPointerException();
        return c.cast(arg);
    }

    /**
     * Tests whether {@code arg} is a non-null instance of the class.
     */
    public boolean canCast(Object arg) {
        return c.isInstance(arg);
    }

    public Class<T> baseClass() {
        return c;
    }

    public String toString() {
        // note that array types use descriptors
        return c.getName() + "!";
    }

    public boolean equals(Object o) {
        return o instanceof NullRestrictedClass<?> that &&
               this.c.equals(that.c);
    }

    public int hashCode() {
        return Objects.hash(NullRestrictedClass.class, c);
    }

    public Optional<DynamicConstantDesc<NullRestrictedClass<T>>> describeConstable() {
        return c.describeConstable().map(classDesc ->
            DynamicConstantDesc.of(BSM_NULL_RESTRICTED_CLASS, classDesc));
    }

    private static final DirectMethodHandleDesc BSM_NULL_RESTRICTED_CLASS =
        ConstantDescs.ofConstantBootstrap(
            ConstantDescs.CD_ConstantBootstraps,
            "nullRestrictedClass",
            RuntimeType.class.describeConstable().get(),
            ConstantDescs.CD_Class);

}
