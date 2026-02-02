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
#include "oops/oopsHierarchy.hpp"
#include "oops/resolvedFieldEntry.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/javaThread.inline.hpp"
#include "utilities/debug.hpp"
#include "utilities/ostream.hpp"
#include "utilities/vmError.hpp"

template <>
inline InlineKlassPayloadImpl<oop>::InlineKlassPayloadImpl(
    oop holder, InlineKlass* klass, size_t offset, LayoutKind layout_kind)
    : _holder(holder), _klass(klass), _offset(offset),
      _layout_kind(layout_kind) {
  assert_post_construction_invariants();
}

template <>
inline InlineKlassPayloadImpl<Handle>::InlineKlassPayloadImpl(
    oop holder, InlineKlass* klass, size_t offset, LayoutKind layout_kind,
    JavaThread* thread)
    : _holder(thread, holder), _klass(klass), _offset(offset),
      _layout_kind(layout_kind) {
  assert_post_construction_invariants();
}

template <>
inline InlineKlassPayloadImpl<oop>::InlineKlassPayloadImpl(
    oop holder, InlineKlass* klass, size_t offset, LayoutKind layout_kind,
    JavaThread* thread)
    : InlineKlassPayloadImpl(holder, klass, offset, layout_kind) {}

template <>
inline InlineKlassPayloadImpl<Handle>::InlineKlassPayloadImpl(
    oop holder, InlineKlass* klass, size_t offset, LayoutKind layout_kind)
    : InlineKlassPayloadImpl(holder, klass, offset, layout_kind,
                             JavaThread::current()) {}

template <typename OopOrHandle>
inline void InlineKlassPayloadImpl<OopOrHandle>::set_offset(size_t offset) {
  _offset = offset;
}

template <> inline oop InlineKlassPayloadImpl<oop>::get_holder() const {
  return _holder;
}

template <> inline oop InlineKlassPayloadImpl<Handle>::get_holder() const {
  return _holder();
}

template <typename OopOrHandle>
inline InlineKlass* InlineKlassPayloadImpl<OopOrHandle>::get_klass() const {
  return _klass;
}

template <typename OopOrHandle>
inline size_t InlineKlassPayloadImpl<OopOrHandle>::get_offset() const {
  precond(_offset != BAD_OFFSET);
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
  return get_klass()->layout_has_null_marker(get_layout_kind());
}

template <typename OopOrHandle>
inline void InlineKlassPayloadImpl<OopOrHandle>::mark_as_non_null() {
  precond(has_null_marker());
  get_klass()->mark_payload_as_non_null(get_address());
}

template <typename OopOrHandle>
inline void InlineKlassPayloadImpl<OopOrHandle>::mark_as_null() {
  precond(has_null_marker());
  get_klass()->mark_payload_as_null(get_address());
}

template <typename OopOrHandle>
inline bool InlineKlassPayloadImpl<OopOrHandle>::is_payload_null() const {
  return has_null_marker() &&
         get_klass()->is_payload_marked_as_null(get_address());
}

template <>
inline inlineOop InlineKlassPayloadImpl<oop>::allocate_instance(TRAPS) const {
  Handle holder(THREAD, _holder);
  inlineOop res = _klass->allocate_instance(THREAD);
  _holder = holder();
  return res;
}

template <>
inline inlineOop
InlineKlassPayloadImpl<Handle>::allocate_instance(TRAPS) const {
  return get_klass()->allocate_instance(THREAD);
}

#ifdef ASSERT
template <typename OopOrHandle>
void InlineKlassPayloadImpl<OopOrHandle>::print_on(outputStream* st) const {
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
    st->print_cr("_offset: %zu", _offset);
  }
  {
    const LayoutKind layout_kind = _layout_kind;
    st->print_cr("--- layout_kind ---");
    StreamIndentor si(st);
    st->print_cr("_layout_kind: %u", (uint32_t)layout_kind);
    LayoutKindHelper::print_on(layout_kind, st);
    st->cr();
  }
}

template <typename OopOrHandle>
inline void
InlineKlassPayloadImpl<OopOrHandle>::assert_post_construction_invariants()
    const {
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

template <typename OopOrHandle>
template <typename PayloadA, typename PayloadB>
inline void InlineKlassPayloadImpl<OopOrHandle>::assert_pre_copy_invariants(
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
  const size_t src_layout_size_in_bytes =
      src.get_klass()->layout_size_in_bytes(src.get_layout_kind());
  const size_t dst_layout_size_in_bytes =
      dst.get_klass()->layout_size_in_bytes(dst.get_layout_kind());
  const size_t copy_layout_size_in_bytes =
      src_has_copy_layout
          ? src_layout_size_in_bytes
          : (dst_has_copy_layout ? dst_layout_size_in_bytes : 0xBADBADu);

  precond(src.get_klass() == dst.get_klass());
  precond(src_or_dst_is_buffered || src_and_dst_same_layout_kind);
  precond(src_has_copy_layout || dst_has_copy_layout);
  precond(copy_layout_size_in_bytes <= src_layout_size_in_bytes);
  precond(copy_layout_size_in_bytes <= dst_layout_size_in_bytes);
}
#endif // ASSERT

template <typename OopOrHandle>
template <typename PayloadA, typename PayloadB>
inline void InlineKlassPayloadImpl<OopOrHandle>::copy(
    const PayloadA& src, const PayloadB& dst, LayoutKind copy_layout_kind) {
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

template <typename OopOrHandle>
inline inlineOop InlineKlassPayloadImpl<
    OopOrHandle>::BufferedInlineKlassPayloadImpl::get_holder() const {
  return inlineOop(InlineKlassPayloadImpl<OopOrHandle>::get_holder());
}

template <typename OopOrHandle>
inline void
InlineKlassPayloadImpl<OopOrHandle>::BufferedInlineKlassPayloadImpl::copy_to(
    const BufferedInlineKlassPayload& dst) {
  copy(*this, dst, LayoutKind::BUFFERED);
}

template <typename OopOrHandle>
inline void
InlineKlassPayloadImpl<OopOrHandle>::BufferedInlineKlassPayloadImpl::copy_to(
    const BufferedInlineKlassPayloadHandle& dst) {
  copy(*this, dst, LayoutKind::BUFFERED);
}

inline BufferedInlineKlassPayload::BufferedInlineKlassPayload(inlineOop buffer)
    : BufferedInlineKlassPayload(buffer, InlineKlass::cast(buffer->klass())) {}

inline BufferedInlineKlassPayload::BufferedInlineKlassPayload(
    inlineOop buffer, InlineKlass* klass)
    : BufferedInlineKlassPayload(buffer, klass, (size_t)klass->payload_offset(),
                                 LayoutKind::BUFFERED) {}

inline BufferedInlineKlassPayload
BufferedInlineKlassPayload::construct_from_parts(oop holder, InlineKlass* klass,
                                                 size_t offset,
                                                 LayoutKind layout_kind) {
  return BufferedInlineKlassPayload(holder, klass, offset, layout_kind);
}

inline BufferedInlineKlassPayloadHandle::BufferedInlineKlassPayloadHandle(
    inlineOop buffer, JavaThread* thread)
    : BufferedInlineKlassPayloadHandle(
          buffer, InlineKlass::cast(buffer->klass()), thread) {}

inline BufferedInlineKlassPayloadHandle::BufferedInlineKlassPayloadHandle(
    inlineOop buffer, InlineKlass* klass, JavaThread* thread)
    : BufferedInlineKlassPayloadHandle(buffer, klass,
                                       (size_t)klass->payload_offset(),
                                       LayoutKind::BUFFERED, thread) {}

inline BufferedInlineKlassPayloadHandle
BufferedInlineKlassPayloadHandle::construct_from_parts(oop holder,
                                                       InlineKlass* klass,
                                                       size_t offset,
                                                       LayoutKind layout_kind,
                                                       JavaThread* thread) {
  return BufferedInlineKlassPayloadHandle(holder, klass, offset, layout_kind,
                                          thread);
}

template <typename OopOrHandle>
inline InlineKlassPayloadImpl<OopOrHandle>::FlatFieldInlineKlassPayloadImpl::
    FlatFieldInlineKlassPayloadImpl(instanceOop holder, size_t offset,
                                    InlineLayoutInfo* inline_layout_info)
    : FlatFieldInlineKlassPayloadImpl(holder, inline_layout_info->klass(),
                                      offset, inline_layout_info->kind()) {}

template <typename OopOrHandle>
inline InlineKlassPayloadImpl<OopOrHandle>::FlatFieldInlineKlassPayloadImpl::
    FlatFieldInlineKlassPayloadImpl(instanceOop holder, size_t offset,
                                    InlineLayoutInfo* inline_layout_info,
                                    JavaThread* thread)
    : FlatFieldInlineKlassPayloadImpl(holder, inline_layout_info->klass(),
                                      offset, inline_layout_info->kind(),
                                      thread) {}

template <typename OopOrHandle>
inline instanceOop InlineKlassPayloadImpl<
    OopOrHandle>::FlatFieldInlineKlassPayloadImpl::get_holder() const {
  return instanceOop(InlineKlassPayloadImpl<OopOrHandle>::get_holder());
}

template <typename OopOrHandle>
template <typename OtherOopOrHandle>
inline bool
InlineKlassPayloadImpl<OopOrHandle>::FlatInlineKlassPayloadImpl::copy_to_helper(
    InlineKlassPayloadImpl<OtherOopOrHandle>& dst) {
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

template <typename OopOrHandle>
template <typename OtherOopOrHandle>
inline void InlineKlassPayloadImpl<OopOrHandle>::FlatInlineKlassPayloadImpl::
    copy_from_helper(InlineKlassPayloadImpl<OtherOopOrHandle>& src) {
  // Copy from BUFFERED to FLAT, null marker fix may be required.
  if (has_null_marker()) {
    // The FLAT payload has a null mark. So make sure that buffered is marked as
    // non null. It is the callers responsibility to ensure that this is a
    // valid non null value.
    src.mark_as_non_null();
  }
  copy(src, *this, get_layout_kind());
}

template <typename OopOrHandle>
inline bool
InlineKlassPayloadImpl<OopOrHandle>::FlatInlineKlassPayloadImpl::copy_to(
    BufferedInlineKlassPayload& dst) {
  return copy_to_helper(dst);
}

template <typename OopOrHandle>
inline bool
InlineKlassPayloadImpl<OopOrHandle>::FlatInlineKlassPayloadImpl::copy_to(
    BufferedInlineKlassPayloadHandle& dst) {
  return copy_to_helper(dst);
}

template <typename OopOrHandle>
inline void InlineKlassPayloadImpl<OopOrHandle>::FlatInlineKlassPayloadImpl::
    copy_from_non_null(BufferedInlineKlassPayload& src) {
  copy_from_helper(src);
}

template <typename OopOrHandle>
inline void InlineKlassPayloadImpl<OopOrHandle>::FlatInlineKlassPayloadImpl::
    copy_from_non_null(BufferedInlineKlassPayloadHandle& src) {
  copy_from_helper(src);
}

template <typename OopOrHandle>
inline void
InlineKlassPayloadImpl<OopOrHandle>::FlatInlineKlassPayloadImpl::copy_to(
    const FlatFieldInlineKlassPayload& dst) {
  copy(*this, dst, get_layout_kind());
}

template <typename OopOrHandle>
inline void
InlineKlassPayloadImpl<OopOrHandle>::FlatInlineKlassPayloadImpl::copy_to(
    const FlatFieldInlineKlassPayloadHandle& dst) {
  copy(*this, dst, get_layout_kind());
}

template <typename OopOrHandle>
inline void
InlineKlassPayloadImpl<OopOrHandle>::FlatInlineKlassPayloadImpl::copy_to(
    const FlatArrayInlineKlassPayload& dst) {
  copy(*this, dst, get_layout_kind());
}

template <typename OopOrHandle>
inline void
InlineKlassPayloadImpl<OopOrHandle>::FlatInlineKlassPayloadImpl::copy_to(
    const FlatArrayInlineKlassPayloadHandle& dst) {
  copy(*this, dst, get_layout_kind());
}

template <typename OopOrHandle>
inline inlineOop
InlineKlassPayloadImpl<OopOrHandle>::FlatInlineKlassPayloadImpl::read(TRAPS) {
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
    BufferedInlineKlassPayload dst(res, get_klass());
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

template <typename OopOrHandle>
inline void InlineKlassPayloadImpl<OopOrHandle>::FlatInlineKlassPayloadImpl::
    write_without_nullability_check(inlineOop obj) {
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
    BufferedInlineKlassPayload null_payload = get_klass()->null_payload();

    // Use copy directly as copy_from_non_null assumes the buffered value is
    // non-null regardless of the null marker.
    copy(null_payload, *this, get_layout_kind());
  } else {
    // Copy the obj payload
    BufferedInlineKlassPayload obj_payload(obj);
    copy_from_non_null(obj_payload);
  }
}

template <typename OopOrHandle>
inline void
InlineKlassPayloadImpl<OopOrHandle>::FlatInlineKlassPayloadImpl::write(
    inlineOop obj, TRAPS) {
  if (obj == nullptr && !has_null_marker()) {
    // This payload does not have a null marker and cannot represent a null
    // value.
    THROW_MSG(vmSymbols::java_lang_NullPointerException(), "Value is null");
  }
  write_without_nullability_check(obj);
}

template <typename OopOrHandle>
inline typename InlineKlassPayloadImpl<OopOrHandle>::FlatInlineKlassPayloadImpl
InlineKlassPayloadImpl<OopOrHandle>::FlatInlineKlassPayloadImpl::
    construct_from_parts(oop holder, InlineKlass* klass, size_t offset,
                         LayoutKind layout_kind) {
  return FlatInlineKlassPayloadImpl(holder, klass, offset, layout_kind);
}

template <typename OopOrHandle>
inline typename InlineKlassPayloadImpl<OopOrHandle>::FlatInlineKlassPayloadImpl
InlineKlassPayloadImpl<OopOrHandle>::FlatInlineKlassPayloadImpl::
    construct_from_parts(oop holder, InlineKlass* klass, size_t offset,
                         LayoutKind layout_kind, JavaThread* thread) {
  return FlatInlineKlassPayloadImpl(holder, klass, offset, layout_kind, thread);
}

inline FlatFieldInlineKlassPayload::FlatFieldInlineKlassPayload(
    instanceOop holder, fieldDescriptor* field_descriptor)
    : FlatFieldInlineKlassPayload(holder, field_descriptor,
                                  InstanceKlass::cast(holder->klass())) {}

inline FlatFieldInlineKlassPayload::FlatFieldInlineKlassPayload(
    instanceOop holder, fieldDescriptor* field_descriptor, InstanceKlass* klass)
    : FlatFieldInlineKlassPayload(
          holder, (size_t)klass->field_offset(field_descriptor->index()),
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
          holder, (size_t)resolved_field_entry->field_offset(),
          klass->inline_layout_info_adr(resolved_field_entry->field_index())) {
  postcond(holder->klass()->is_subclass_of(klass));
}

inline FlatFieldInlineKlassPayload
FlatFieldInlineKlassPayload::construct_from_parts(instanceOop holder,
                                                  InlineKlass* klass,
                                                  size_t offset,
                                                  LayoutKind layout_kind) {
  return FlatFieldInlineKlassPayload(holder, klass, offset, layout_kind);
}

inline FlatFieldInlineKlassPayloadHandle::FlatFieldInlineKlassPayloadHandle(
    instanceOop holder, fieldDescriptor* field_descriptor, JavaThread* thread)
    : FlatFieldInlineKlassPayloadHandle(holder, field_descriptor,
                                        InstanceKlass::cast(holder->klass()),
                                        thread) {}

inline FlatFieldInlineKlassPayloadHandle::FlatFieldInlineKlassPayloadHandle(
    instanceOop holder, fieldDescriptor* field_descriptor, InstanceKlass* klass,
    JavaThread* thread)
    : FlatFieldInlineKlassPayloadHandle(
          holder, (size_t)klass->field_offset(field_descriptor->index()),
          klass->inline_layout_info_adr(field_descriptor->index()), thread) {
  postcond(holder->klass() == klass);
}

inline FlatFieldInlineKlassPayloadHandle::FlatFieldInlineKlassPayloadHandle(
    instanceOop holder, ResolvedFieldEntry* resolved_field_entry,
    JavaThread* thread)
    : FlatFieldInlineKlassPayloadHandle(holder, resolved_field_entry,
                                        resolved_field_entry->field_holder(),
                                        thread) {}

inline FlatFieldInlineKlassPayloadHandle::FlatFieldInlineKlassPayloadHandle(
    instanceOop holder, ResolvedFieldEntry* resolved_field_entry,
    InstanceKlass* klass, JavaThread* thread)
    : FlatFieldInlineKlassPayloadHandle(
          holder, (size_t)resolved_field_entry->field_offset(),
          klass->inline_layout_info_adr(resolved_field_entry->field_index()),
          thread) {
  postcond(holder->klass()->is_subclass_of(klass));
}

inline FlatFieldInlineKlassPayloadHandle
FlatFieldInlineKlassPayloadHandle::construct_from_parts(instanceOop holder,
                                                        InlineKlass* klass,
                                                        size_t offset,
                                                        LayoutKind layout_kind,
                                                        JavaThread* thread) {
  return FlatFieldInlineKlassPayloadHandle(holder, klass, offset, layout_kind,
                                           thread);
}

template <typename OopOrHandle>
inline InlineKlassPayloadImpl<OopOrHandle>::FlatArrayInlineKlassPayloadImpl::
    FlatArrayInlineKlassPayloadImpl(flatArrayOop holder, InlineKlass* klass,
                                    size_t offset, LayoutKind layout_kind,
                                    jint layout_helper, int element_size)
    : FlatInlineKlassPayloadImpl(holder, klass, offset, layout_kind),
      _layout_helper(layout_helper), _element_size(element_size) {}

template <typename OopOrHandle>
inline InlineKlassPayloadImpl<OopOrHandle>::FlatArrayInlineKlassPayloadImpl::
    FlatArrayInlineKlassPayloadImpl(flatArrayOop holder, InlineKlass* klass,
                                    size_t offset, LayoutKind layout_kind,
                                    jint layout_helper, int element_size,
                                    JavaThread* thread)
    : FlatInlineKlassPayloadImpl(holder, klass, offset, layout_kind, thread),
      _layout_helper(layout_helper), _element_size(element_size) {}

template <typename OopOrHandle>
inline flatArrayOop InlineKlassPayloadImpl<
    OopOrHandle>::FlatArrayInlineKlassPayloadImpl::get_holder() const {
  return flatArrayOop(InlineKlassPayloadImpl<OopOrHandle>::get_holder());
}

template <typename OopOrHandle>
inline void
InlineKlassPayloadImpl<OopOrHandle>::FlatArrayInlineKlassPayloadImpl::set_index(
    int index) {
  set_offset(get_holder()->value_offset(index, _layout_helper));
}

template <typename OopOrHandle>
inline void InlineKlassPayloadImpl<
    OopOrHandle>::FlatArrayInlineKlassPayloadImpl::advance_index(int delta) {
  set_offset(this->get_offset() + delta * ssize_t(_element_size));
}

template <typename OopOrHandle>
inline void InlineKlassPayloadImpl<
    OopOrHandle>::FlatArrayInlineKlassPayloadImpl::next_element() {
  advance_index(1);
}

template <typename OopOrHandle>
inline void InlineKlassPayloadImpl<
    OopOrHandle>::FlatArrayInlineKlassPayloadImpl::previous_element() {
  advance_index(-1);
}

template <typename OopOrHandle>
inline void InlineKlassPayloadImpl<
    OopOrHandle>::FlatArrayInlineKlassPayloadImpl::set_offset(size_t offset) {
#ifdef ASSERT
  // For ease of use as iterators we allow the offset to point the one element
  // size beyond the first and last element. If there are no elements only the
  // base offset is allowed. However we treat these as terminal states, and set
  // the offset to a BAD_OFFSET in debug builds.

  assert(flatArrayOopDesc::base_offset_in_bytes() >= (size_t)_element_size,
         "These asserts assumes that the largest element is smaller than the "
         "base offset. %zu >= %zu",
         flatArrayOopDesc::base_offset_in_bytes(), (size_t)_element_size);
  const int length = get_holder()->length();
  const ssize_t min_offset = (ssize_t)flatArrayOopDesc::base_offset_in_bytes() -
                             (length == 0 ? 0 : _element_size);
  const ssize_t max_offset =
      flatArrayOopDesc::base_offset_in_bytes() + length * _element_size;
  assert(min_offset <= (ssize_t)offset && (ssize_t)offset <= max_offset,
         "Offset out-ouf-bounds: %zd <= %zd <= %zd", min_offset,
         (ssize_t)offset, max_offset);

  if ((ssize_t)offset == min_offset || (ssize_t)offset == max_offset) {
    // Terminal state of iteration, set a bad value.
    InlineKlassPayloadImpl::set_offset(BAD_OFFSET);
  } else {
    InlineKlassPayloadImpl::set_offset(offset);
  }
#else  // ASSERT
  InlineKlassPayloadImpl::set_offset(offset);
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
          holder->value_offset(index, klass->layout_helper()),
          klass->layout_kind(), klass->layout_helper(),
          klass->element_byte_size()) {
  postcond(holder->klass() == klass);
}

inline FlatArrayInlineKlassPayload
FlatArrayInlineKlassPayload::construct_from_parts(flatArrayOop holder,
                                                  InlineKlass* klass,
                                                  size_t offset,
                                                  LayoutKind layout_kind) {
  return construct_from_parts(holder, klass, offset, layout_kind,
                              FlatArrayKlass::cast(holder->klass()));
}

inline FlatArrayInlineKlassPayload
FlatArrayInlineKlassPayload::construct_from_parts(
    flatArrayOop holder, InlineKlass* klass, size_t offset,
    LayoutKind layout_kind, FlatArrayKlass* holder_klass) {
  return FlatArrayInlineKlassPayload(holder, klass, offset, layout_kind,
                                     holder_klass->layout_helper(),
                                     holder_klass->element_byte_size());
}

inline FlatArrayInlineKlassPayloadHandle::FlatArrayInlineKlassPayloadHandle(
    flatArrayOop holder, JavaThread* thread)
    : FlatArrayInlineKlassPayloadHandle(
          holder, FlatArrayKlass::cast(holder->klass()), thread) {}

inline FlatArrayInlineKlassPayloadHandle::FlatArrayInlineKlassPayloadHandle(
    flatArrayOop holder, FlatArrayKlass* klass, JavaThread* thread)
    : FlatArrayInlineKlassPayloadHandle(
          holder, klass->element_klass(), BAD_OFFSET, klass->layout_kind(),
          klass->layout_helper(), klass->element_byte_size(), thread) {
  postcond(holder->klass() == klass);
}

inline FlatArrayInlineKlassPayloadHandle::FlatArrayInlineKlassPayloadHandle(
    flatArrayOop holder, int index, JavaThread* thread)
    : FlatArrayInlineKlassPayloadHandle(
          holder, index, FlatArrayKlass::cast(holder->klass()), thread) {}

inline FlatArrayInlineKlassPayloadHandle::FlatArrayInlineKlassPayloadHandle(
    flatArrayOop holder, int index, FlatArrayKlass* klass, JavaThread* thread)
    : FlatArrayInlineKlassPayloadHandle(
          holder, klass->element_klass(), BAD_OFFSET, klass->layout_kind(),
          klass->layout_helper(), klass->element_byte_size(), thread) {
  postcond(holder->klass() == klass);
}

inline FlatArrayInlineKlassPayloadHandle
FlatArrayInlineKlassPayloadHandle::construct_from_parts(flatArrayOop holder,
                                                        InlineKlass* klass,
                                                        size_t offset,
                                                        LayoutKind layout_kind,
                                                        JavaThread* thread) {
  return construct_from_parts(holder, klass, offset, layout_kind,
                              FlatArrayKlass::cast(holder->klass()), thread);
}

inline FlatArrayInlineKlassPayloadHandle
FlatArrayInlineKlassPayloadHandle::construct_from_parts(
    flatArrayOop holder, InlineKlass* klass, size_t offset,
    LayoutKind layout_kind, FlatArrayKlass* holder_klass, JavaThread* thread) {
  return FlatArrayInlineKlassPayloadHandle(
      holder, klass, offset, layout_kind, holder_klass->layout_helper(),
      holder_klass->element_byte_size(), thread);
}

#endif // SHARE_VM_OOPS_INLINEKLASSPAYLOAD_INLINE_HPP
