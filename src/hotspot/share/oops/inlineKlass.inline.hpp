/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "oops/inlineKlass.hpp"

#include "oops/flatArrayKlass.inline.hpp"
#include "oops/instanceKlass.inline.hpp"
#include "oops/layoutKind.hpp"
#include "oops/oopsHierarchy.hpp"
#include "oops/resolvedFieldEntry.hpp"
#include "runtime/fieldDescriptor.hpp"
#include "runtime/handles.hpp"
#include "utilities/debug.hpp"
#include "utilities/devirtualizer.inline.hpp"

template <typename OopOrHandle>
inline void InlineKlassPayloadImpl<OopOrHandle>::assert_invariants() const {
  assert(_holder != nullptr, "Bad null");
  assert(_klass->is_layout_supported(_layout_kind), "Unsupported layout kind: %s",
         LayoutKindHelper::layout_kind_as_string(_layout_kind));
  assert(_layout_kind != LayoutKind::REFERENCE && _layout_kind != LayoutKind::UNKNOWN, "Bad layout kind: %s",
         LayoutKindHelper::layout_kind_as_string(_layout_kind));
  assert((_holder->klass() == _klass) == (_layout_kind == LayoutKind::BUFFERED), "invariant");
}

template <>
inline InlineKlassPayloadImpl<oop>::InlineKlassPayloadImpl(oop oop, InlineKlass* klass, size_t offset, LayoutKind layout_kind)
  : _holder(oop),
    _klass(klass),
    _offset(offset),
    _layout_kind(layout_kind) {
  assert_invariants();
}

template <>
inline InlineKlassPayloadImpl<Handle>::InlineKlassPayloadImpl(oop oop, InlineKlass* klass, size_t offset, LayoutKind layout_kind)
  : _holder(Thread::current(), oop),
    _klass(klass),
    _offset(offset),
    _layout_kind(layout_kind) {
  assert_invariants();
}

template <typename OopOrHandle>
inline InlineKlassPayloadImpl<OopOrHandle>::InlineKlassPayloadImpl(instanceOop oop, size_t offset, InlineLayoutInfo* inline_layout_info)
  : InlineKlassPayloadImpl(oop, inline_layout_info->klass(), offset, inline_layout_info->kind()) {}

template <typename OopOrHandle>
inline InlineKlassPayloadImpl<OopOrHandle>::InlineKlassPayloadImpl(instanceOop oop)
  : InlineKlassPayloadImpl(oop, InlineKlass::cast(oop->klass())) {}

template <typename OopOrHandle>
inline InlineKlassPayloadImpl<OopOrHandle>::InlineKlassPayloadImpl(instanceOop oop, InlineKlass* klass)
  : InlineKlassPayloadImpl(oop, klass, klass->payload_offset(), LayoutKind::BUFFERED) {
  postcond(oop->klass() == klass);
}

template <typename OopOrHandle>
inline InlineKlassPayloadImpl<OopOrHandle>::InlineKlassPayloadImpl(flatArrayOop oop)
  : InlineKlassPayloadImpl(oop, FlatArrayKlass::cast(oop->klass())) {}

template <typename OopOrHandle>
inline InlineKlassPayloadImpl<OopOrHandle>::InlineKlassPayloadImpl(flatArrayOop oop, FlatArrayKlass* klass)
  : InlineKlassPayloadImpl(oop, klass->element_klass(), BAD_OFFSET, klass->layout_kind()) {
  postcond(oop->klass() == klass);
}

template <typename OopOrHandle>
inline InlineKlassPayloadImpl<OopOrHandle>::InlineKlassPayloadImpl(flatArrayOop oop, int index)
  : InlineKlassPayloadImpl(oop, index, FlatArrayKlass::cast(oop->klass())) {}

template <typename OopOrHandle>
inline InlineKlassPayloadImpl<OopOrHandle>::InlineKlassPayloadImpl(flatArrayOop oop, int index, FlatArrayKlass* klass)
  : InlineKlassPayloadImpl(oop, klass->element_klass(), oop->value_offset(index, klass->layout_helper()), klass->layout_kind()) {
  postcond(oop->klass() == klass);
}

template <typename OopOrHandle>
inline InlineKlassPayloadImpl<OopOrHandle>::InlineKlassPayloadImpl(instanceOop oop, fieldDescriptor* field_descriptor)
  : InlineKlassPayloadImpl(oop, field_descriptor, InstanceKlass::cast(oop->klass())) {}

template <typename OopOrHandle>
inline InlineKlassPayloadImpl<OopOrHandle>::InlineKlassPayloadImpl(instanceOop oop, fieldDescriptor* field_descriptor, InstanceKlass* klass)
  : InlineKlassPayloadImpl(oop, klass->field_offset(field_descriptor->index()), klass->inline_layout_info_adr(field_descriptor->index())) {
  postcond(oop->klass() == klass);
}

template <typename OopOrHandle>
inline InlineKlassPayloadImpl<OopOrHandle>::InlineKlassPayloadImpl(instanceOop oop, ResolvedFieldEntry* resolved_field_entry)
  : InlineKlassPayloadImpl(oop, resolved_field_entry, resolved_field_entry->field_holder()) {}

template <typename OopOrHandle>
inline InlineKlassPayloadImpl<OopOrHandle>::InlineKlassPayloadImpl(instanceOop oop, ResolvedFieldEntry* resolved_field_entry, InstanceKlass* klass)
  : InlineKlassPayloadImpl(oop, resolved_field_entry->field_offset(), klass->inline_layout_info_adr(resolved_field_entry->field_index())) {
  // TODO: Is it fine to use the subclass here rather than the exact klass?
  postcond(oop->klass()->is_subclass_of(klass));
}

template <>
inline oop InlineKlassPayloadImpl<oop>::get_holder() const {
  return _holder;
}

template <>
inline oop InlineKlassPayloadImpl<Handle>::get_holder() const {
  return _holder();
}

template <typename OopOrHandle>
inline InlineKlass* InlineKlassPayloadImpl<OopOrHandle>::get_klass() const {
  return _klass;
}

template <typename OopOrHandle>
inline size_t InlineKlassPayloadImpl<OopOrHandle>::get_offset() const {
  precond (_offset != BAD_OFFSET);
  return _offset;
}

template <typename OopOrHandle>
inline LayoutKind InlineKlassPayloadImpl<OopOrHandle>::get_layout_kind() const {
  return _layout_kind;
}

template <typename OopOrHandle>
inline address InlineKlassPayloadImpl<OopOrHandle>::get_address() const {
  return cast_from_oop<address>(get_holder()) + _offset;
}

template <typename OopOrHandle>
inline bool InlineKlassPayloadImpl<OopOrHandle>::has_null_marker() const {
  // TODO: Cleanup this. Should probably be a function on the klass.
  return LayoutKindHelper::is_nullable_flat(get_layout_kind()) ||
         (get_layout_kind() == LayoutKind::BUFFERED &&
          get_klass()->supports_nullable_layouts());
}

template <typename OopOrHandle>
inline void InlineKlassPayloadImpl<OopOrHandle>::mark_as_non_null() {
  precond(has_null_marker());
  get_klass()->mark_payload_as_non_null(get_address());
}

template <typename OopOrHandle>
inline bool InlineKlassPayloadImpl<OopOrHandle>::is_marked_as_null() const {
  return has_null_marker() && get_klass()->is_payload_marked_as_null(get_address());
}

template <typename OopOrHandle>
inline void InlineKlassPayloadImpl<OopOrHandle>::set_index(int index) {
  set_index(index, FlatArrayKlass::cast(get_holder()->klass()));
}

template <typename OopOrHandle>
inline void InlineKlassPayloadImpl<OopOrHandle>::set_index(int index, FlatArrayKlass* klass) {
  precond(get_holder()->klass() == klass);
  set_index(index, klass->layout_helper());
}

template <typename OopOrHandle>
inline void InlineKlassPayloadImpl<OopOrHandle>::set_index(int index, jint layout_helper) {
  precond(FlatArrayKlass::cast(get_holder()->klass())->layout_helper() == layout_helper);
  _offset = flatArrayOop(get_holder())->value_offset(index, layout_helper);
}

template <>
inline instanceOop InlineKlassPayloadImpl<oop>::allocate_instance(TRAPS) const {
  Handle holder(THREAD, _holder);
  instanceOop res = _klass->allocate_instance(THREAD);
  _holder = holder();
  return res;
}

template <>
inline instanceOop InlineKlassPayloadImpl<Handle>::allocate_instance(TRAPS) const {
  return get_klass()->allocate_instance(THREAD);
}

template <typename OopOrHandle>
inline instanceOop InlineKlassPayloadImpl<OopOrHandle>::read(TRAPS) {
  assert(get_layout_kind() != LayoutKind::BUFFERED,
         "Should not need to clone a buffer.");

  switch(get_layout_kind()) {
    case LayoutKind::NULLABLE_ATOMIC_FLAT:
    case LayoutKind::NULLABLE_NON_ATOMIC_FLAT: {
      if (is_marked_as_null()) {
        return nullptr;
      }
    } // Fallthrough
    case LayoutKind::NULL_FREE_ATOMIC_FLAT:
    case LayoutKind::NULL_FREE_NON_ATOMIC_FLAT: {
      instanceOop res = allocate_instance(CHECK_NULL);
      InlineKlassPayload dst(res, get_klass());
      copy_to(dst);
      if (has_null_marker() && dst.is_marked_as_null()) {
        // If the destination is marked as null the copied payload must have
        // been concurrently updated between the is_mark_as_null() check above
        // and the copy of the payload. So the res oop is invalid. We return
        // null instead.
        return nullptr;
      }
      return res;
    }
    break;
    default:
      ShouldNotReachHere();
  }
}

template <typename OopOrHandle>
template <typename PayloadA, typename PayloadB>
inline void InlineKlassPayloadImpl<OopOrHandle>::copy(const PayloadA& src, const PayloadB& dst, LayoutKind copy_layout_kind) {
  InlineKlass* const klass = src.get_klass();
  precond(klass == dst.get_klass());
  precond(src.get_layout_kind() == copy_layout_kind ||
          dst.get_layout_kind() == copy_layout_kind);
  precond(klass->layout_size_in_bytes(src.get_layout_kind()) >=
          klass->layout_size_in_bytes(copy_layout_kind) || true /* TODO: Need JDK-8376532 fixed */);
  precond(klass->layout_size_in_bytes(dst.get_layout_kind()) >=
          klass->layout_size_in_bytes(copy_layout_kind) || true /* TODO: Need JDK-8376532 fixed */);

  const auto value_copy = [&] (const auto& src) {
    HeapAccess<>::value_copy(src.get_address(), dst.get_address(), klass, copy_layout_kind);
  };

  switch(copy_layout_kind) {
    case LayoutKind::NULLABLE_ATOMIC_FLAT:
    case LayoutKind::NULLABLE_NON_ATOMIC_FLAT: {
      if (src.is_marked_as_null()) {
        // copy null_reset value to dest
        value_copy(klass->null_payload());
      } else {
        value_copy(src);
      }
    }
    break;
    case LayoutKind::BUFFERED:
    case LayoutKind::NULL_FREE_ATOMIC_FLAT:
    case LayoutKind::NULL_FREE_NON_ATOMIC_FLAT: {
      if (!klass->is_empty_inline_type()) {
        value_copy(src);
      }
    }
    break;
    default:
      ShouldNotReachHere();
  }
}

template <typename OopOrHandle>
template <typename OtherOopOrHandle>
inline void InlineKlassPayloadImpl<OopOrHandle>::copy_to(const InlineKlassPayloadImpl<OtherOopOrHandle>& dst) {
  copy(*this, dst, get_layout_kind());
}

template <typename OopOrHandle>
template <typename OtherOopOrHandle>
inline void InlineKlassPayloadImpl<OopOrHandle>::copy_from(const InlineKlassPayloadImpl<OtherOopOrHandle>& src) {
  copy(src, *this, get_layout_kind());
}

template <typename OopOrHandle>
inline void InlineKlassPayloadImpl<OopOrHandle>::write(instanceOop obj) {
  assert(get_layout_kind() != LayoutKind::BUFFERED, "Why are you cloning something immutable");

  if (obj == nullptr) {
    assert(has_null_marker(), "Null is not allowed");

    // I am not sure what this comment is saying. As I only thought we flatten
    // loaded class fields. So???

    // Writing null to a nullable flat field/element is usually done by writing
    // the whole pre-allocated null_reset_value at the payload address to ensure
    // that the null marker and all potential oops are reset to "zeros".
    // However, the null_reset_value is allocated during class initialization.
    // If the current value of the field is null, it is possible that the class
    // of the field has not been initialized yet and thus the null_reset_value
    // might not be available yet.
    // Writing null over an already null value should not trigger class initialization.
    // The solution is to detect null being written over null cases and return immediately
    // (writing null over null is a no-op from a field modification point of view)
    if (is_marked_as_null()) {
      return;
    }
    // Copy the null payload
    copy_from(get_klass()->null_payload());
  } else {
    // A buffered layout may have an invalid null marker, make sure it is set to
    // non-null before copying. (Only a strict requirement for atomic nullables)
    InlineKlassPayload obj_payload(obj);
    if (obj_payload.has_null_marker()) {
      // After copying, re-check if the payload is now marked as null. Another
      // thread could have marked the src object as null after the initial check
      // but before the copy operation, causing the null-marker to be marked in
      // the destination. In this case, discard the allocated object and
      // return nullptr.
      obj_payload.mark_as_non_null();
    }
    copy_from(obj_payload);
  }
}

template <typename OopOrHandle>
inline void InlineKlassPayloadImpl<OopOrHandle>::write(instanceOop obj, TRAPS) {
  assert(get_layout_kind() != LayoutKind::BUFFERED, "Why are you cloning something immutable");

  if (obj == nullptr && !has_null_marker()) {
    // This payload does not have a null marker and cannot represent a null value.
    THROW_MSG(vmSymbols::java_lang_NullPointerException(), "Value is null");
  }
  write(obj);
}

inline address InlineKlass::payload_addr(oop o) const {
  return cast_from_oop<address>(o) + payload_offset();
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
