/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */

#ifndef SHARE_OOPS_LAYOUTKIND_HPP
#define SHARE_OOPS_LAYOUTKIND_HPP

// LayoutKind is an enum used to indicate which layout has been used for a given value field.
// Each layout has its own properties and its own access protocol that is detailed below.
//
// REFERENCE : this layout uses a pointer to a heap allocated instance (no flattening).
//             When used, field_flags().is_flat() is false . The field can be nullable or
//             null-restricted, in the later case, field_flags().is_null_free_inline_type() is true.
//             In case of a null-restricted field, putfield  and putstatic  must perform a null-check
//             before writing a new value. Still for null-restricted fields, if getfield reads a null pointer
//             from the receiver, it means that the field was not initialized yet, and getfield must substitute
//             the null reference with the default value of the field's class.
// NON_ATOMIC_FLAT : this layout is the simplest form of flattening. Any field embedded inside the flat field
//             can be accessed independently. The field is null-restricted, meaning putfield must perform a
//             null-check before performing a field update.
// ATOMIC_FLAT : this flat layout is designed for atomic updates, with size and alignment that make use of
//             atomic instructions possible. All accesses, reads and writes, must be performed atomically.
//             The field is null-restricted, meaning putfield must perform a null-check before performing a
//             field update.
// NULLABLE_ATOMIC_FLAT : this is the flat layout designed for JEP 401. It is designed for atomic updates,
//             with size and alignment that make use of atomic instructions possible. All accesses, reads and
//             writes, must be performed atomically. The layout includes a null marker which indicates if the
//             field's value must be considered as null or not. The null marker is a byte, with the value zero
//             meaning the field's value is null, and a non-zero value meaning the field's value is not null.
//             A getfield must check the value of the null marker before returning a value. If the null marker
//             is zero, getfield  must return the null reference, otherwise it returns the field's value read
//             from the receiver. When a putfield writes a non-null value to such field, the update, including
//             the field's value and the null marker, must be performed in a single atomic operation. If the
//             source of the value is a heap allocated instance of the field's class, it is allowed to set the
//             null marker to non-zero in the heap allocated instance before copying the value to the receiver
//             (the BUFFERED layout used in heap allocated values guarantees that the space for the null marker
//             is included, but has no meaning for the heap allocated instance which is always non-null, and that
//             the whole payload is correctly aligned for atomic operations). When a putfield writes null to such
//             field, the null marker must be set to zero. However, if the field contains oops, those oops must be
//             cleared too in order to prevent memory leaks. In order to simplify such operation, value classes
//             supporting a NULLABLE_ATOMIC_FLAT layout have a pre-allocated reset value instance, filled with
//             zeros, which can be used to simply overwrite the whole flat field and reset everything (oops and
//             null marker). The reset value instance is needed because the VM needs an instance guaranteed to
//             always be filled with zeros, and the default value could have its null marker set to non-zero if
//             it is used as a source to update a NULLABLE_ATOMIC_FLAT field.
// BUFFERED: this layout is only used in heap buffered instances of a value class. It is computed to be compatible
//             to be compatible in size and alignment with all other flat layouts supported by the value class.
//
//
// IMPORTANT: The REFERENCE layout must always be associated with the numerical value zero, because the implementation
// of the lava.lang.invoke.MemberName class relies on this property.

enum class LayoutKind : uint32_t {
  REFERENCE            = 0,    // indirection to a heap allocated instance
  BUFFERED             = 1,    // layout used in heap allocated standalone instances
  NON_ATOMIC_FLAT      = 2,    // flat, no guarantee of atomic updates, no null marker
  ATOMIC_FLAT          = 3,    // flat, size compatible with atomic updates, alignment requirement is equal to the size
  NULLABLE_ATOMIC_FLAT = 4,    // flat, include a null marker, plus same properties as ATOMIC layout
  UNKNOWN              = 5     // used for uninitialized fields of type LayoutKind
};

#endif // SHARE_OOPS_LAYOUTKIND_HPP
