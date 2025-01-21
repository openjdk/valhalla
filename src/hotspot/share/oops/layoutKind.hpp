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

enum LayoutKind {
  REFERENCE            = 0,    // indirection to a heap allocated instance
  PAYLOAD              = 1,    // layout used in heap allocated standalone instances, probably temporary for the transition
  NON_ATOMIC_FLAT      = 2,    // flat, no guarantee of atomic updates, no null marker
  ATOMIC_FLAT          = 3,    // flat, size compatible with atomic updates, alignment requirement is equal to the size
  NULLABLE_ATOMIC_FLAT = 4,    // flat, include a null marker, plus same properties as ATOMIC layout
  UNKNOWN              = 5     // used for uninitialized fields of type LayoutKind
};

#endif // SHARE_OOPS_LAYOUTKIND_HPP
