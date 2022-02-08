/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/**
 * A restricted interface implemented by all identity objects.
 *
 * IdentityObject: An object with identity.
 *
 * *Identity* is a property of certain objects, determined at instance creation
 * time and preserved throughout the life of the object. While an object's field
 * values may change, its identity is constant. Object identities are unique: no
 * two objects created by different instance creation operations can have the same
 * identity.
 *
 * Every object is either an *identity object* or a *value object*. Value
 * objects lack identity.
 *
 * The following operations have special behavior when applied to identity objects:
 *
 * - The `==` operator, and the default implementation of the `Object.equals`
 * method, compare the identities of their operands, producing `true` for an
 * identity object only if the object is being compared to itself.
 *
 * - The `System.identityHashCode` method, and the default implementation of the
 * `Object.hashCode` method, generate a hash code from an identity object's
 * identity.
 *
 * - The `synchronized` modifier and `synchronized` statement are only able to
 * successfully acquire a lock when applied to an identity object.
 *
 * A class may implement `IdentityObject` or `ValueObject`, but never both.
 * Value classes always implement `ValueObject`, while all other concrete
 * classes (except `Object`) implicitly implement `IdentityObject`.
 *
 * Abstract classes and interfaces may implement or extend this interface if they
 * wish to guarantee that all instances of the class or interface have identity.
 *
 * @since Valhalla
 */
public interface IdentityObject {
}
