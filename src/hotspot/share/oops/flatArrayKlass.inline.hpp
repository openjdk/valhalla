/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
#ifndef SHARE_VM_OOPS_FLATARRAYKLASS_INLINE_HPP
#define SHARE_VM_OOPS_FLATARRAYKLASS_INLINE_HPP

#include "memory/memRegion.hpp"
#include "memory/iterator.hpp"
#include "oops/arrayKlass.hpp"
#include "oops/flatArrayKlass.hpp"
#include "oops/flatArrayOop.hpp"
#include "oops/flatArrayOop.inline.hpp"
#include "oops/inlineKlass.hpp"
#include "oops/inlineKlass.inline.hpp"
#include "oops/klass.hpp"
#include "oops/oop.inline.hpp"
#include "utilities/devirtualizer.inline.hpp"
#include "utilities/macros.hpp"

/*
 * Warning incomplete: requires embedded oops, not yet enabled, so consider this a "sketch-up" of oop iterators
 */

template <typename T, class OopClosureType>
void FlatArrayKlass::oop_oop_iterate_elements_specialized(flatArrayOop a,
                                                          OopClosureType* closure) {
  assert(contains_oops(), "Nothing to iterate");

  const int shift = Klass::layout_helper_log2_element_size(layout_helper());
  const int addr_incr = 1 << shift;
  uintptr_t elem_addr = (uintptr_t) a->base();
  const uintptr_t stop_addr = elem_addr + ((uintptr_t)a->length() << shift);
  const int oop_offset = element_klass()->first_field_offset();

  while (elem_addr < stop_addr) {
    element_klass()->oop_iterate_specialized<T>((address)(elem_addr - oop_offset), closure);
    elem_addr += addr_incr;
  }
}

template <typename T, class OopClosureType>
void FlatArrayKlass::oop_oop_iterate_elements_specialized_bounded(flatArrayOop a,
                                                                  OopClosureType* closure,
                                                                  void* lo, void* hi) {
  assert(contains_oops(), "Nothing to iterate");

  const int shift = Klass::layout_helper_log2_element_size(layout_helper());
  const int addr_incr = 1 << shift;
  uintptr_t elem_addr = (uintptr_t)a->base();
  uintptr_t stop_addr = elem_addr + ((uintptr_t)a->length() << shift);
  const int oop_offset = element_klass()->first_field_offset();

  if (elem_addr < (uintptr_t) lo) {
    uintptr_t diff = ((uintptr_t) lo) - elem_addr;
    elem_addr += (diff >> shift) << shift;
  }
  if (stop_addr > (uintptr_t) hi) {
    uintptr_t diff = stop_addr - ((uintptr_t) hi);
    stop_addr -= (diff >> shift) << shift;
  }

  const uintptr_t end = stop_addr;
  while (elem_addr < end) {
    element_klass()->oop_iterate_specialized_bounded<T>((address)(elem_addr - oop_offset), closure, lo, hi);
    elem_addr += addr_incr;
  }
}

template <typename T, class OopClosureType>
void FlatArrayKlass::oop_oop_iterate_elements(flatArrayOop a, OopClosureType* closure) {
  if (contains_oops()) {
    oop_oop_iterate_elements_specialized<T>(a, closure);
  }
}

template <typename T, typename OopClosureType>
void FlatArrayKlass::oop_oop_iterate(oop obj, OopClosureType* closure) {
  assert(obj->klass()->is_flatArray_klass(),"must be a flat array");
  flatArrayOop a = flatArrayOop(obj);

  if (Devirtualizer::do_metadata(closure)) {
    Devirtualizer::do_klass(closure, obj->klass());
    Devirtualizer::do_klass(closure, FlatArrayKlass::cast(obj->klass())->element_klass());
  }

  oop_oop_iterate_elements<T>(a, closure);
}

template <typename T, typename OopClosureType>
void FlatArrayKlass::oop_oop_iterate_reverse(oop obj, OopClosureType* closure) {
  // TODO
  oop_oop_iterate<T>(obj, closure);
}

template <typename T, class OopClosureType>
void FlatArrayKlass::oop_oop_iterate_elements_bounded(flatArrayOop a, OopClosureType* closure, MemRegion mr) {
  if (contains_oops()) {
    oop_oop_iterate_elements_specialized_bounded<T>(a, closure, mr.start(), mr.end());
  }
}


template <typename T, typename OopClosureType>
void FlatArrayKlass::oop_oop_iterate_bounded(oop obj, OopClosureType* closure, MemRegion mr) {
  flatArrayOop a = flatArrayOop(obj);
  if (Devirtualizer::do_metadata(closure)) {
    Devirtualizer::do_klass(closure, a->klass());
    Devirtualizer::do_klass(closure, FlatArrayKlass::cast(obj->klass())->element_klass());
  }
  oop_oop_iterate_elements_bounded<T>(a, closure, mr);
}

#endif // SHARE_VM_OOPS_FLATARRAYKLASS_INLINE_HPP
