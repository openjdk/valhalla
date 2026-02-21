/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates. All rights reserved.
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
 * A null-restricted field is a field of a concrete value class type that does
 * not store {@code null}. The field must be initialized according to the strict
 * field initialization rules. Attempts to write {@code null} to the field
 * currently cause a {@link NullPointerException}.
 * <p>
 * The HotSpot VM uses this <em>experimental</em> annotation to enable flattened
 * encodings for a field for experiments.  This annotation introduces behaviors
 * to facilitate experiments with flattened encodings, such as eager loading of
 * the annotated field type and throwing {@link ClassFormatError} if the field
 * type is not a concrete value class.
 * <p>
 * Use of this annotation in trusted code, if any, should not rely on or expose
 * particular behaviors of this annotation in API contracts; for example, an
 * API should throw {@code NullPointerException} explicitly rather than relying
 * on writing {@code null} to a {@code NullRestricted}.
 * <p>
 * <b>Behaviors associated with this annotation are incompatible with null
 * checking features proposed by Project Valhalla. This annotation will be
 * removed in the future, and is subject to behavioral change without
 * replacement or notice.</b>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NullRestricted {
}
