/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_VM_OOPS_VALUEARRAYOOP_HPP
#define SHARE_VM_OOPS_VALUEARRAYOOP_HPP

#include "oops/arrayOop.hpp"
#include "oops/klass.hpp"
#include "oops/oop.inline.hpp"

#include <limits.h>


// A valueArrayOop is an array containing value types (may include flatten embedded oop elements).

class valueArrayOopDesc : public arrayOopDesc {

 public:
  void*  base() const { return arrayOopDesc::base(T_VALUETYPE); }

  void* value_at_addr(int index, int lh) const {
    assert(is_within_bounds(index), "index out of bounds");

    address addr = (address) base();
    addr += (index << Klass::layout_helper_log2_element_size(lh));
    return (void*) addr;
  }

  // Sizing
  static size_t element_size(int lh, int nof_elements) {
    return nof_elements << Klass::layout_helper_log2_element_size(lh);
  }

  static int object_size(int lh, int length) {
    julong size_in_bytes = header_size_in_bytes() + element_size(lh, length);
    julong size_in_words = ((size_in_bytes + (HeapWordSize-1)) >> LogHeapWordSize);
    assert(size_in_words <= (julong)max_jint, "no overflow");
    return align_object_size((intptr_t)size_in_words);
  }

  int object_size() {
    return object_size(klass()->layout_helper(), length());
  }

};

#endif // SHARE_VM_OOPS_VALUEARRAYOOP_HPP
