/*
 * Copyright (c) 2015, 2025, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_VM_OOPS_FLATARRAYOOP_HPP
#define SHARE_VM_OOPS_FLATARRAYOOP_HPP

#include "oops/arrayOop.hpp"
#include "oops/inlineKlass.hpp"
#include "oops/klass.hpp"
#include "runtime/handles.hpp"

// A flatArrayOop points to a flat array containing inline types (no indirection).
// It may include embedded oops in its elements.

class flatArrayOopDesc : public arrayOopDesc {

 public:
  void*  base() const;
  void* value_at_addr(int index, jint lh) const;

  inline oop read_value_from_flat_array( int index, TRAPS);
  inline void write_value_to_flat_array(oop value, int index, TRAPS);

  // Sizing
  static size_t element_size(int lh, int nof_elements) {
    size_t sz = (size_t) nof_elements;
    return sz << Klass::layout_helper_log2_element_size(lh);
  }

  static int object_size(int lh, int length) {
    julong size_in_bytes = base_offset_in_bytes(Klass::layout_helper_element_type(lh));
    size_in_bytes += element_size(lh, length);
    julong size_in_words = ((size_in_bytes + (HeapWordSize-1)) >> LogHeapWordSize);
    assert(size_in_words <= (julong)max_jint, "no overflow");
    return align_object_size((intptr_t)size_in_words);
  }

  int object_size() const;

};

#endif // SHARE_VM_OOPS_FLATARRAYOOP_HPP
