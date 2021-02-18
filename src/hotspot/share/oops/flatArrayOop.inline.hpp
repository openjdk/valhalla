/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_VM_OOPS_FLATARRAYOOP_INLINE_HPP
#define SHARE_VM_OOPS_FLATARRAYOOP_INLINE_HPP

#include "oops/access.inline.hpp"
#include "oops/flatArrayOop.hpp"
#include "oops/inlineKlass.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/globals.hpp"

inline void* flatArrayOopDesc::base() const { return arrayOopDesc::base(T_INLINE_TYPE); }

inline void* flatArrayOopDesc::value_at_addr(int index, jint lh) const {
  assert(is_within_bounds(index), "index out of bounds");

  address addr = (address) base();
  addr += (index << Klass::layout_helper_log2_element_size(lh));
  return (void*) addr;
}

inline int flatArrayOopDesc::object_size() const {
  return object_size(klass()->layout_helper(), length());
}

inline oop flatArrayOopDesc::value_alloc_copy_from_index(flatArrayHandle vah, int index, TRAPS) {
  FlatArrayKlass* vaklass = FlatArrayKlass::cast(vah->klass());
  InlineKlass* vklass = vaklass->element_klass();
  if (vklass->is_empty_inline_type()) {
    return vklass->default_value();
  } else {
    oop buf = vklass->allocate_instance_buffer(CHECK_NULL);
    vklass->inline_copy_payload_to_new_oop(vah->value_at_addr(index, vaklass->layout_helper()), buf);
    return buf;
  }
}

inline void flatArrayOopDesc::value_copy_from_index(int index, oop dst) const {
  FlatArrayKlass* vaklass = FlatArrayKlass::cast(klass());
  InlineKlass* vklass = vaklass->element_klass();
  void* src = value_at_addr(index, vaklass->layout_helper());
  return vklass->inline_copy_payload_to_new_oop(src, dst);
}

inline void flatArrayOopDesc::value_copy_to_index(oop src, int index) const {
  FlatArrayKlass* vaklass = FlatArrayKlass::cast(klass());
  InlineKlass* vklass = vaklass->element_klass();
  if (vklass->is_empty_inline_type()) {
    return;
  }
  void* dst = value_at_addr(index, vaklass->layout_helper());
  vklass->inline_copy_oop_to_payload(src, dst);
}



#endif // SHARE_VM_OOPS_FLATARRAYOOP_INLINE_HPP
