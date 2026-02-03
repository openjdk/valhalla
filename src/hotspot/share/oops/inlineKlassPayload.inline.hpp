/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
#ifndef SHARE_VM_OOPS_INLINEKLASSPAYLOAD_INLINE_HPP
#define SHARE_VM_OOPS_INLINEKLASSPAYLOAD_INLINE_HPP

#include "oops/inlineKlassPayload.hpp"

#include "oops/flatArrayKlass.inline.hpp"
#include "oops/inlineKlass.inline.hpp"
#include "oops/layoutKind.hpp"
#include "oops/oopHandle.inline.hpp"
#include "oops/oopsHierarchy.hpp"
#include "oops/resolvedFieldEntry.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/javaThread.inline.hpp"
#include "utilities/debug.hpp"
#include "utilities/ostream.hpp"
#include "utilities/vmError.hpp"

inline ValuePayload::ValuePayload(oop holder, InlineKlass* klass,
                                  ptrdiff_t offset, LayoutKind layout_kind)
    : _storage{holder, klass, offset, layout_kind} {
  assert_post_construction_invariants();
}

inline void ValuePayload::set_offset(ptrdiff_t offset) {
  _storage._offset = offset;
}

inline oop ValuePayload::get_holder() const { return _storage._holder; }

inline InlineKlass* ValuePayload::get_klass() const { return _storage._klass; }

inline ptrdiff_t ValuePayload::get_offset() const {
  precond(_storage._offset != BAD_OFFSET);
  return _storage._offset;
}

inline LayoutKind ValuePayload::get_layout_kind() const {
  return _storage._layout_kind;
}

inline address ValuePayload::get_address() const {
  return cast_from_oop<address>(get_holder()) + _storage._offset;
}

inline bool ValuePayload::has_null_marker() const {
  return get_klass()->layout_has_null_marker(get_layout_kind());
}

inline void ValuePayload::mark_as_non_null() {
  precond(has_null_marker());
  get_klass()->mark_payload_as_non_null(get_address());
}

inline void ValuePayload::mark_as_null() {
  precond(has_null_marker());
  get_klass()->mark_payload_as_null(get_address());
}

inline bool ValuePayload::is_payload_null() const {
  return has_null_marker() &&
         get_klass()->is_payload_marked_as_null(get_address());
}

inline inlineOop ValuePayload::allocate_instance(TRAPS) const {
  ::Handle holder(THREAD, get_holder());
  inlineOop res = get_klass()->allocate_instance(THREAD);
  _storage._holder = holder();
  return res;
}

#ifdef ASSERT
void ValuePayload::print_on(outputStream* st) const {
  {
    oop holder = get_holder();
    st->print_cr("--- holder ---");
    StreamIndentor si(st);
    st->print_cr("_holder: " PTR_FORMAT, p2i(holder));
    if (holder != nullptr) {
      holder->print_on(st);
      st->cr();
    }
  }
  {
    InlineKlass* const klass = get_klass();
    st->print_cr("--- klass ---");
    StreamIndentor si(st);
    st->print_cr("_klass: " PTR_FORMAT, p2i(klass));
    if (klass != nullptr) {
      klass->print_on(st);
      st->cr();
    }
  }
  {
    st->print_cr("--- offset ---");
    StreamIndentor si(st);
    st->print_cr("_offset: %zd", _storage._offset);
  }
  {
    const LayoutKind layout_kind = get_layout_kind();
    st->print_cr("--- layout_kind ---");
    StreamIndentor si(st);
    st->print_cr("_layout_kind: %u", (uint32_t)layout_kind);
    LayoutKindHelper::print_on(layout_kind, st);
    st->cr();
  }
}

inline void ValuePayload::assert_post_construction_invariants() const {
  OnVMError on_assertion_failuire([&](outputStream* st) {
    st->print_cr("=== assert_post_construction_invariants failure ===");
    StreamIndentor si(st);
    print_on(st);
    st->cr();
  });

  postcond(get_holder() != nullptr);
  postcond(get_klass()->is_layout_supported(get_layout_kind()));
  postcond(get_layout_kind() != LayoutKind::REFERENCE &&
           get_layout_kind() != LayoutKind::UNKNOWN);
  postcond((get_holder()->klass() == get_klass()) ==
           (get_layout_kind() == LayoutKind::BUFFERED));
}

template <typename PayloadA, typename PayloadB>
inline void ValuePayload::assert_pre_copy_invariants(
    const PayloadA& src, const PayloadB& dst, LayoutKind copy_layout_kind) {
  OnVMError on_assertion_failuire([&](outputStream* st) {
    st->print_cr("=== assert_post_construction_invariants failure ===");
    StreamIndentor si(st);
    {
      st->print_cr("--- src payload ---");
      StreamIndentor si(st);
      src.print_on(st);
      st->cr();
    }
    {
      st->print_cr("--- dst payload ---");
      StreamIndentor si(st);
      dst.print_on(st);
      st->cr();
    }
    {
      st->print_cr("--- copy layout kind ---");
      StreamIndentor si(st);
      LayoutKindHelper::print_on(copy_layout_kind, st);
      st->cr();
    }
  });

  const bool src_or_dst_is_buffered =
      src.get_layout_kind() == LayoutKind::BUFFERED ||
      dst.get_layout_kind() == LayoutKind::BUFFERED;
  const bool src_and_dst_same_layout_kind =
      src.get_layout_kind() == dst.get_layout_kind();
  const bool src_has_copy_layout = src.get_layout_kind() == copy_layout_kind;
  const bool dst_has_copy_layout = dst.get_layout_kind() == copy_layout_kind;
  const int src_layout_size_in_bytes =
      src.get_klass()->layout_size_in_bytes(src.get_layout_kind());
  const int dst_layout_size_in_bytes =
      dst.get_klass()->layout_size_in_bytes(dst.get_layout_kind());
  const int copy_layout_size_in_bytes =
      src_has_copy_layout
          ? src_layout_size_in_bytes
          : (dst_has_copy_layout ? dst_layout_size_in_bytes : -1);

  precond(src.get_klass() == dst.get_klass());
  precond(src_or_dst_is_buffered || src_and_dst_same_layout_kind);
  precond(src_has_copy_layout || dst_has_copy_layout);
  precond(copy_layout_size_in_bytes <= src_layout_size_in_bytes);
  precond(copy_layout_size_in_bytes <= dst_layout_size_in_bytes);
}
#endif // ASSERT

inline ValuePayload::Handle::Handle(const ValuePayload& payload,
                                    JavaThread* thread)
    : _storage{::Handle(thread, payload.get_holder()), payload.get_klass(),
               payload.get_offset(), payload.get_layout_kind()} {}

inline oop ValuePayload::Handle::get_holder() const {
  return _storage._holder();
}

inline InlineKlass* ValuePayload::Handle::get_klass() const {
  return _storage._klass;
}

inline ptrdiff_t ValuePayload::Handle::get_offset() const {
  return _storage._offset;
}

inline LayoutKind ValuePayload::Handle::get_layout_kind() const {
  return _storage._layout_kind;
}

inline ValuePayload ValuePayload::Handle::operator()() const {
  return ValuePayload(get_holder(), get_klass(), get_offset(),
                      get_layout_kind());
}

inline ValuePayload::OopHandle::OopHandle(const ValuePayload& payload,
                                          OopStorage* storage)
    : _storage{::OopHandle(storage, payload.get_holder()), payload.get_klass(),
               payload.get_offset(), payload.get_layout_kind()} {}

inline oop ValuePayload::OopHandle::get_holder() const {
  return _storage._holder.resolve();
}

inline InlineKlass* ValuePayload::OopHandle::get_klass() const {
  return _storage._klass;
}

inline ptrdiff_t ValuePayload::OopHandle::get_offset() const {
  return _storage._offset;
}

inline LayoutKind ValuePayload::OopHandle::get_layout_kind() const {
  return _storage._layout_kind;
}

inline ValuePayload ValuePayload::OopHandle::operator()() const {
  return ValuePayload(get_holder(), get_klass(), get_offset(),
                      get_layout_kind());
}

inline ValuePayload::Handle ValuePayload::get_handle(JavaThread* thread) const {
  return Handle(*this, thread);
}

inline ValuePayload::OopHandle
ValuePayload::get_oop_handle(OopStorage* storage) const {
  return OopHandle(*this, storage);
}

template <typename PayloadA, typename PayloadB>
inline void ValuePayload::copy(const PayloadA& src, const PayloadB& dst,
                               LayoutKind copy_layout_kind) {
  assert_pre_copy_invariants(src, dst, copy_layout_kind);

  InlineKlass* const klass = src.get_klass();

  const auto value_copy = [&](const auto& src) {
    HeapAccess<>::value_copy(src.get_address(), dst.get_address(), klass,
                             copy_layout_kind);
  };

  switch (copy_layout_kind) {
  case LayoutKind::NULLABLE_ATOMIC_FLAT:
  case LayoutKind::NULLABLE_NON_ATOMIC_FLAT: {
    if (src.is_payload_null()) {
      // copy null_reset value to dest
      value_copy(klass->null_payload());
    } else {
      value_copy(src);
    }
  } break;
  case LayoutKind::BUFFERED:
  case LayoutKind::NULL_FREE_ATOMIC_FLAT:
  case LayoutKind::NULL_FREE_NON_ATOMIC_FLAT: {
    if (!klass->is_empty_inline_type()) {
      value_copy(src);
    }
  } break;
  default:
    ShouldNotReachHere();
  }
}

inline inlineOop BufferedValuePayload::get_holder() const {
  return inlineOop(ValuePayload::get_holder());
}

inline void BufferedValuePayload::copy_to(const BufferedValuePayload& dst) {
  copy(*this, dst, LayoutKind::BUFFERED);
}

inline BufferedValuePayload::BufferedValuePayload(inlineOop buffer)
    : BufferedValuePayload(buffer, InlineKlass::cast(buffer->klass())) {}

inline BufferedValuePayload::BufferedValuePayload(inlineOop buffer,
                                                  InlineKlass* klass)
    : BufferedValuePayload(buffer, klass, klass->payload_offset(),
                           LayoutKind::BUFFERED) {}

inline BufferedValuePayload BufferedValuePayload::construct_from_parts(
    oop holder, InlineKlass* klass, ptrdiff_t offset, LayoutKind layout_kind) {
  return BufferedValuePayload(holder, klass, offset, layout_kind);
}

BufferedValuePayload BufferedValuePayload::Handle::operator()() const {
  return construct_from_parts(get_holder(), get_klass(), get_offset(),
                              get_layout_kind());
}

BufferedValuePayload BufferedValuePayload::OopHandle::operator()() const {
  return construct_from_parts(get_holder(), get_klass(), get_offset(),
                              get_layout_kind());
}

inline inlineOop BufferedValuePayload::Handle::get_holder() const {
  return inlineOop(ValuePayload::Handle::get_holder());
}

inline inlineOop BufferedValuePayload::OopHandle::get_holder() const {
  return inlineOop(ValuePayload::OopHandle::get_holder());
}

BufferedValuePayload::Handle
BufferedValuePayload::get_handle(JavaThread* thread) const {
  return Handle(*this, thread);
}

BufferedValuePayload::OopHandle
BufferedValuePayload::get_oop_handle(OopStorage* storage) const {
  return OopHandle(*this, storage);
}

inline FlatFieldInlineKlassPayload::FlatFieldInlineKlassPayload(
    instanceOop holder, ptrdiff_t offset, InlineLayoutInfo* inline_layout_info)
    : FlatFieldInlineKlassPayload(holder, inline_layout_info->klass(), offset,
                                  inline_layout_info->kind()) {}

inline instanceOop FlatFieldInlineKlassPayload::get_holder() const {
  return instanceOop(ValuePayload::get_holder());
}

inline void FlatValuePayload::copy_from_helper(ValuePayload& src) {
  // Copy from BUFFERED to FLAT, null marker fix may be required.
  if (has_null_marker()) {
    // The FLAT payload has a null mark. So make sure that buffered is marked as
    // non null. It is the callers responsibility to ensure that this is a
    // valid non null value.
    src.mark_as_non_null();
  }
  copy(src, *this, get_layout_kind());
}

inline bool FlatValuePayload::copy_to(BufferedValuePayload& dst) {
  // Copy from FLAT to BUFFERED, null marker fix may be required.

  // Copy the payload to the buffered object.
  copy(*this, dst, get_layout_kind());

  if (!has_null_marker() && dst.has_null_marker()) {
    // We must fix the null marker if the src does not have a null marker but
    // the buffered object does.
    dst.mark_as_non_null();

    // The buffered object was just marked non null.
    return true;
  }

  // We may have copied a null payload.
  return !dst.is_payload_null();
}

inline void FlatValuePayload::copy_from_non_null(BufferedValuePayload& src) {
  copy_from_helper(src);
}

inline void FlatValuePayload::copy_to(const FlatFieldInlineKlassPayload& dst) {
  copy(*this, dst, get_layout_kind());
}

inline void FlatValuePayload::copy_to(const FlatArrayInlineKlassPayload& dst) {
  copy(*this, dst, get_layout_kind());
}

inline inlineOop FlatValuePayload::read(TRAPS) {
  switch (get_layout_kind()) {
  case LayoutKind::NULLABLE_ATOMIC_FLAT:
  case LayoutKind::NULLABLE_NON_ATOMIC_FLAT: {
    if (is_payload_null()) {
      return nullptr;
    }
  } // Fallthrough
  case LayoutKind::NULL_FREE_ATOMIC_FLAT:
  case LayoutKind::NULL_FREE_NON_ATOMIC_FLAT: {
    inlineOop res = allocate_instance(CHECK_NULL);
    BufferedValuePayload dst(res, get_klass());
    if (!copy_to(dst)) {
      // copy_to may fail if the payload has been updated with a null value
      // between our is_payload_null() check above and the copy. In this case we
      // have copied a null value into the buffer the payload.
      return nullptr;
    }
    return res;
  } break;
  default:
    ShouldNotReachHere();
  }
}

inline void FlatValuePayload::write_without_nullability_check(inlineOop obj) {
  if (obj == nullptr) {
    assert(has_null_marker(), "Null is not allowed");

    // Writing null to a nullable flat field/element is usually done by copying
    // the null payload to the payload that the null marker and all potential
    // oops are reset to "zeros". However, the null payload is allocated during
    // class initialization. If the current value of the field is null, it is
    // possible that the class of the field has not been initialized yet and
    // thus the null payload might not be available yet.
    // Writing null over an already null value should not trigger class
    // initialization. The solution is to detect null being written over null
    // cases and return immediately (writing null over null is a no-op from a
    // field modification point of view)
    if (is_payload_null()) {
      return;
    }

    // Copy the null payload
    BufferedValuePayload null_payload = get_klass()->null_payload();

    // Use copy directly as copy_from_non_null assumes the buffered value is
    // non-null regardless of the null marker.
    copy(null_payload, *this, get_layout_kind());
  } else {
    // Copy the obj payload
    BufferedValuePayload obj_payload(obj);
    copy_from_non_null(obj_payload);
  }
}

inline void FlatValuePayload::write(inlineOop obj, TRAPS) {
  if (obj == nullptr && !has_null_marker()) {
    // This payload does not have a null marker and cannot represent a null
    // value.
    THROW_MSG(vmSymbols::java_lang_NullPointerException(), "Value is null");
  }
  write_without_nullability_check(obj);
}

inline FlatValuePayload FlatValuePayload::construct_from_parts(
    oop holder, InlineKlass* klass, ptrdiff_t offset, LayoutKind layout_kind) {
  return FlatValuePayload(holder, klass, offset, layout_kind);
}

FlatValuePayload FlatValuePayload::Handle::operator()() const {
  return construct_from_parts(get_holder(), get_klass(), get_offset(),
                              get_layout_kind());
}

FlatValuePayload FlatValuePayload::OopHandle::operator()() const {
  return construct_from_parts(get_holder(), get_klass(), get_offset(),
                              get_layout_kind());
}

FlatValuePayload::Handle
FlatValuePayload::get_handle(JavaThread* thread) const {
  return Handle(*this, thread);
}

FlatValuePayload::OopHandle
FlatValuePayload::get_oop_handle(OopStorage* storage) const {
  return OopHandle(*this, storage);
}

inline FlatFieldInlineKlassPayload::FlatFieldInlineKlassPayload(
    instanceOop holder, fieldDescriptor* field_descriptor)
    : FlatFieldInlineKlassPayload(holder, field_descriptor,
                                  InstanceKlass::cast(holder->klass())) {}

inline FlatFieldInlineKlassPayload::FlatFieldInlineKlassPayload(
    instanceOop holder, fieldDescriptor* field_descriptor, InstanceKlass* klass)
    : FlatFieldInlineKlassPayload(
          holder, klass->field_offset(field_descriptor->index()),
          klass->inline_layout_info_adr(field_descriptor->index())) {
  postcond(holder->klass() == klass);
}

inline FlatFieldInlineKlassPayload::FlatFieldInlineKlassPayload(
    instanceOop holder, ResolvedFieldEntry* resolved_field_entry)
    : FlatFieldInlineKlassPayload(holder, resolved_field_entry,
                                  resolved_field_entry->field_holder()) {}

inline FlatFieldInlineKlassPayload::FlatFieldInlineKlassPayload(
    instanceOop holder, ResolvedFieldEntry* resolved_field_entry,
    InstanceKlass* klass)
    : FlatFieldInlineKlassPayload(
          holder, resolved_field_entry->field_offset(),
          klass->inline_layout_info_adr(resolved_field_entry->field_index())) {
  postcond(holder->klass()->is_subclass_of(klass));
}

inline FlatFieldInlineKlassPayload
FlatFieldInlineKlassPayload::construct_from_parts(instanceOop holder,
                                                  InlineKlass* klass,
                                                  ptrdiff_t offset,
                                                  LayoutKind layout_kind) {
  return FlatFieldInlineKlassPayload(holder, klass, offset, layout_kind);
}

FlatFieldInlineKlassPayload
FlatFieldInlineKlassPayload::Handle::operator()() const {
  return construct_from_parts(get_holder(), get_klass(), get_offset(),
                              get_layout_kind());
}

FlatFieldInlineKlassPayload
FlatFieldInlineKlassPayload::OopHandle::operator()() const {
  return construct_from_parts(get_holder(), get_klass(), get_offset(),
                              get_layout_kind());
}

inline instanceOop FlatFieldInlineKlassPayload::Handle::get_holder() const {
  return instanceOop(ValuePayload::Handle::get_holder());
}

inline instanceOop FlatFieldInlineKlassPayload::OopHandle::get_holder() const {
  return instanceOop(ValuePayload::OopHandle::get_holder());
}

FlatFieldInlineKlassPayload::Handle
FlatFieldInlineKlassPayload::get_handle(JavaThread* thread) const {
  return Handle(*this, thread);
}

FlatFieldInlineKlassPayload::OopHandle
FlatFieldInlineKlassPayload::get_oop_handle(OopStorage* storage) const {
  return OopHandle(*this, storage);
}

inline FlatArrayInlineKlassPayload::FlatArrayInlineKlassPayload(
    flatArrayOop holder, InlineKlass* klass, ptrdiff_t offset,
    LayoutKind layout_kind, jint layout_helper, int element_size)
    : FlatValuePayload(holder, klass, offset, layout_kind),
      _storage{layout_helper, element_size} {}

inline flatArrayOop FlatArrayInlineKlassPayload::get_holder() const {
  return flatArrayOop(ValuePayload::get_holder());
}

inline void FlatArrayInlineKlassPayload::set_index(int index) {
  set_offset(
      (ptrdiff_t)get_holder()->value_offset(index, _storage._layout_helper));
}

inline void FlatArrayInlineKlassPayload::advance_index(int delta) {
  set_offset(this->get_offset() + delta * _storage._element_size);
}

inline void FlatArrayInlineKlassPayload::next_element() { advance_index(1); }

inline void FlatArrayInlineKlassPayload::previous_element() {
  advance_index(-1);
}

inline void FlatArrayInlineKlassPayload::set_offset(ptrdiff_t offset) {
#ifdef ASSERT
  // For ease of use as iterators we allow the offset to point one element size
  // beyond the first and last element. If there are no elements only the base
  // offset is allowed. However we treat these as terminal states, and set the
  // offset to a BAD_OFFSET in debug builds.

  const ptrdiff_t element_size = _storage._element_size;
  const ptrdiff_t length = get_holder()->length();
  const ptrdiff_t base_offset =
      (ptrdiff_t)flatArrayOopDesc::base_offset_in_bytes();

  const ptrdiff_t min_offset = base_offset - (length == 0 ? 0 : element_size);
  const ptrdiff_t max_offset = base_offset + length * element_size;
  assert(min_offset <= offset && offset <= max_offset,
         "Offset out-ouf-bounds: %zd <= %zd <= %zd", min_offset, offset,
         max_offset);

  if (offset == min_offset || offset == max_offset) {
    // Terminal state of iteration, set a bad value.
    ValuePayload::set_offset(BAD_OFFSET);
  } else {
    ValuePayload::set_offset(offset);
  }
#else  // ASSERT
  ValuePayload::set_offset(offset);
#endif // ASSERT
}

inline FlatArrayInlineKlassPayload::FlatArrayInlineKlassPayload(
    flatArrayOop holder)
    : FlatArrayInlineKlassPayload(holder,
                                  FlatArrayKlass::cast(holder->klass())) {}

inline FlatArrayInlineKlassPayload::FlatArrayInlineKlassPayload(
    flatArrayOop holder, FlatArrayKlass* klass)
    : FlatArrayInlineKlassPayload(holder, klass->element_klass(), BAD_OFFSET,
                                  klass->layout_kind(), klass->layout_helper(),
                                  klass->element_byte_size()) {
  postcond(holder->klass() == klass);
}

inline FlatArrayInlineKlassPayload::FlatArrayInlineKlassPayload(
    flatArrayOop holder, int index)
    : FlatArrayInlineKlassPayload(holder, index,
                                  FlatArrayKlass::cast(holder->klass())) {}

inline FlatArrayInlineKlassPayload::FlatArrayInlineKlassPayload(
    flatArrayOop holder, int index, FlatArrayKlass* klass)
    : FlatArrayInlineKlassPayload(
          holder, klass->element_klass(),
          (ptrdiff_t)holder->value_offset(index, klass->layout_helper()),
          klass->layout_kind(), klass->layout_helper(),
          klass->element_byte_size()) {
  postcond(holder->klass() == klass);
}

inline FlatArrayInlineKlassPayload
FlatArrayInlineKlassPayload::construct_from_parts(flatArrayOop holder,
                                                  InlineKlass* klass,
                                                  ptrdiff_t offset,
                                                  LayoutKind layout_kind) {
  return construct_from_parts(holder, klass, offset, layout_kind,
                              FlatArrayKlass::cast(holder->klass()));
}

inline FlatArrayInlineKlassPayload
FlatArrayInlineKlassPayload::construct_from_parts(
    flatArrayOop holder, InlineKlass* klass, ptrdiff_t offset,
    LayoutKind layout_kind, FlatArrayKlass* holder_klass) {
  return FlatArrayInlineKlassPayload(holder, klass, offset, layout_kind,
                                     holder_klass->layout_helper(),
                                     holder_klass->element_byte_size());
}

inline FlatArrayInlineKlassPayload::Handle::Handle(
    const FlatArrayInlineKlassPayload& payload, JavaThread* thread)
    : FlatValuePayload::Handle(payload, thread), _storage(payload._storage) {}

FlatArrayInlineKlassPayload
FlatArrayInlineKlassPayload::Handle::operator()() const {
  return FlatArrayInlineKlassPayload(get_holder(), get_klass(), get_offset(),
                                     get_layout_kind(), _storage._layout_helper,
                                     _storage._element_size);
}

inline flatArrayOop FlatArrayInlineKlassPayload::Handle::get_holder() const {
  return flatArrayOop(ValuePayload::Handle::get_holder());
}

inline flatArrayOop FlatArrayInlineKlassPayload::OopHandle::get_holder() const {
  return flatArrayOop(ValuePayload::OopHandle::get_holder());
}

inline FlatArrayInlineKlassPayload::OopHandle::OopHandle(
    const FlatArrayInlineKlassPayload& payload, OopStorage* storage)
    : FlatValuePayload::OopHandle(payload, storage),
      _storage(payload._storage) {}

FlatArrayInlineKlassPayload
FlatArrayInlineKlassPayload::OopHandle::operator()() const {
  return FlatArrayInlineKlassPayload(get_holder(), get_klass(), get_offset(),
                                     get_layout_kind(), _storage._layout_helper,
                                     _storage._element_size);
}

FlatArrayInlineKlassPayload::Handle
FlatArrayInlineKlassPayload::get_handle(JavaThread* thread) const {
  return Handle(*this, thread);
}

FlatArrayInlineKlassPayload::OopHandle
FlatArrayInlineKlassPayload::get_oop_handle(OopStorage* storage) const {
  return OopHandle(*this, storage);
}

#endif // SHARE_VM_OOPS_INLINEKLASSPAYLOAD_INLINE_HPP
