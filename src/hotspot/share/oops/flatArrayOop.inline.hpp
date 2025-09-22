/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "oops/flatArrayOop.hpp"

#include "classfile/vmSymbols.hpp"
#include "oops/access.inline.hpp"
#include "oops/inlineKlass.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/globals.hpp"

inline void* flatArrayOopDesc::base() const { return arrayOopDesc::base(T_FLAT_ELEMENT); }

inline void* flatArrayOopDesc::value_at_addr(int index, jint lh) const {
  assert(is_within_bounds(index), "index out of bounds");

  address addr = (address) base();
  addr += (index << Klass::layout_helper_log2_element_size(lh));
  return (void*) addr;
}

inline int flatArrayOopDesc::object_size(int lh) const {
  return object_size(lh, length());
}

inline oop flatArrayOopDesc::obj_at(int index) const {
  EXCEPTION_MARK;
  return obj_at(index, THREAD);
}

inline oop flatArrayOopDesc::obj_at(int index, TRAPS) const {
  assert(is_within_bounds(index), "index %d out of bounds %d", index, length());
  FlatArrayKlass* faklass = FlatArrayKlass::cast(klass());
  InlineKlass* vk = InlineKlass::cast(faklass->element_klass());
  int offset = ((char*)value_at_addr(index, faklass->layout_helper())) - ((char*)(oopDesc*)this);
  oop res = vk->read_payload_from_addr((oopDesc*)this, offset, faklass->layout_kind(), CHECK_NULL);
  return res;
}

inline void flatArrayOopDesc::obj_at_put(int index, oop value) {
  EXCEPTION_MARK;                                 // What if the caller is not a Java Thread?
  obj_at_put(index, value, THREAD);
}

inline void flatArrayOopDesc::obj_at_put(int index, oop value, TRAPS) {
  assert(is_within_bounds(index), "index %d out of bounds %d", index, length());
  FlatArrayKlass* faklass = FlatArrayKlass::cast(klass());
  InlineKlass* vk = InlineKlass::cast(faklass->element_klass());
  if (value != nullptr) {
    if (value->klass() != vk) {
      THROW(vmSymbols::java_lang_ArrayStoreException());
    }
  } else if(is_null_free_array()) {
    THROW_MSG(vmSymbols::java_lang_NullPointerException(), "Cannot store null in a null-restricted array");
  }
  vk->write_value_to_addr(value, value_at_addr(index, faklass->layout_helper()), faklass->layout_kind(), true, CHECK);
}

#endif // SHARE_VM_OOPS_FLATARRAYOOP_INLINE_HPP
