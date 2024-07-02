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

package jdk.internal.vm.annotation;

import java.lang.annotation.*;

/**
 * A null-restricted array is an array whose elements are of value class type and are
 * never assigned a {@code null} value during the lifetime of array.
 * Element type must also be annotated with {@link ImplicitlyConstructible} annotation
 * to ensure default value assignment to array elements, thereby guarantying initialized
 * value array by construction.
 *
 * The initial value of the elements is the zero instance of the given class, and attempts to
 * write {@code null} to the element will throw an NullPointerException.
 * <p>
 * The HotSpot VM uses this annotation to enable flat runtime layout of an array that would
 * otherwise be impossible.
 * <p>
 * Because these behaviors are not specified by Java SE, this annotation should only be used
 * by internal JDK classes for experimental purposes and should not affect user-observable
 * outcomes.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NullRestrictedArray {
}
