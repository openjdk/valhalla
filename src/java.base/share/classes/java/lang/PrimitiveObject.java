/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * A restricted interface implemented by all primitive objects.
 *
 * A primitive object is an instance of a primitive class, lacking identity.
 *
 * Every object is either an *identity object* or a *primitive object*. Identity
 * objects have a unique identity determined for them at instance creation time and
 * preserved throughout their life.
 *
 * Primitive objects do *not* have an identity. Instead, they simply aggregate a
 * set of immutable field values. The lack of identity enables certain performance
 * optimizations by Java Virtual Machine implementations.
 * The following operations have special behavior when applied to primitive
 * objects:
 *
 * - The `==` operator, and the default implementation of the `Object.equals`
 * method, compare the values of the operands' fields. Primitive objects
 * created at different points in a program may be `==`.
 *
 * - The `System.identityHashCode` method, and the default implementation of the
 * `Object.hashCode` method, generate a hash code from the hash codes of a
 * primitive object's fields.
 *
 * - The `synchronized` modifier and `synchronized` statement always fail when
 * applied to a primitive object.
 *
 * A class may implement `PrimitiveObject` or `IdentityObject`, but never both.
 * Primitive classes always implement `PrimitiveObject`, while all other concrete
 * classes (except `Object`) implicitly implement `IdentityObject`.
 *
 * Abstract classes and interfaces may implement or extend this interface if they
 * wish to guarantee that all instances of the class or interface are primitive
 * objects.
 *
 * @since 1.16
 */

public interface PrimitiveObject {
}
