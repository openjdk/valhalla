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
#ifndef SHARE_VM_OOPS_INLINEKLASS_INLINE_HPP
#define SHARE_VM_OOPS_INLINEKLASS_INLINE_HPP

#include "memory/iterator.hpp"
#include "oops/flatArrayKlass.hpp"
#include "oops/inlineKlass.hpp"
#include "oops/instanceKlass.inline.hpp"
#include "oops/oop.inline.hpp"
#include "utilities/macros.hpp"

inline InlineKlassFixedBlock* InlineKlass::inlineklass_static_block() const {
  address adr_jf = adr_inline_type_field_klasses();
  if (adr_jf != NULL) {
    return (InlineKlassFixedBlock*)(adr_jf + this->java_fields_count() * sizeof(Klass*));
  }

  InstanceKlass** adr_host = adr_unsafe_anonymous_host();
  if (adr_host != NULL) {
    return (InlineKlassFixedBlock*)(adr_host + 1);
  }

  InstanceKlass* volatile* adr_impl = adr_implementor();
  if (adr_impl != NULL) {
    return (InlineKlassFixedBlock*)(adr_impl + 1);
  }

  return (InlineKlassFixedBlock*)end_of_nonstatic_oop_maps();
}

inline address InlineKlass::adr_return_regs() const {
  InlineKlassFixedBlock* vkst = inlineklass_static_block();
  return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _return_regs));
}

inline Array<VMRegPair>* InlineKlass::return_regs() const {
  return *((Array<VMRegPair>**)adr_return_regs());
}


inline InlineKlass* InlineKlass::cast(Klass* k) {
  assert(k->is_inline_klass(), "cast to InlineKlass");
  return (InlineKlass*) k;
}

inline address InlineKlass::data_for_oop(oop o) const {
  return ((address) (void*) o) + first_field_offset();
}

inline void InlineKlass::inline_copy_payload_to_new_oop(void* src, oop dst) {
  HeapAccess<IS_DEST_UNINITIALIZED>::value_copy(src, data_for_oop(dst), this);
}

inline void InlineKlass::inline_copy_oop_to_new_oop(oop src, oop dst) {
  HeapAccess<IS_DEST_UNINITIALIZED>::value_copy(data_for_oop(src), data_for_oop(dst), this);
}

inline void InlineKlass::inline_copy_oop_to_new_payload(oop src, void* dst) {
  HeapAccess<IS_DEST_UNINITIALIZED>::value_copy(data_for_oop(src), dst, this);
}

inline void InlineKlass::inline_copy_oop_to_payload(oop src, void* dst) {
  HeapAccess<>::value_copy(data_for_oop(src), dst, this);
}


template <typename T, class OopClosureType>
void InlineKlass::oop_iterate_specialized(const address oop_addr, OopClosureType* closure) {
  OopMapBlock* map = start_of_nonstatic_oop_maps();
  OopMapBlock* const end_map = map + nonstatic_oop_map_count();

  for (; map < end_map; map++) {
    T* p = (T*) (oop_addr + map->offset());
    T* const end = p + map->count();
    for (; p < end; ++p) {
      Devirtualizer::do_oop(closure, p);
    }
  }
}

template <typename T, class OopClosureType>
inline void InlineKlass::oop_iterate_specialized_bounded(const address oop_addr, OopClosureType* closure, void* lo, void* hi) {
  OopMapBlock* map = start_of_nonstatic_oop_maps();
  OopMapBlock* const end_map = map + nonstatic_oop_map_count();

  T* const l   = (T*) lo;
  T* const h   = (T*) hi;

  for (; map < end_map; map++) {
    T* p = (T*) (oop_addr + map->offset());
    T* end = p + map->count();
    if (p < l) {
      p = l;
    }
    if (end > h) {
      end = h;
    }
    for (; p < end; ++p) {
      Devirtualizer::do_oop(closure, p);
    }
  }
}


#endif // SHARE_VM_OOPS_INLINEKLASS_INLINE_HPP
