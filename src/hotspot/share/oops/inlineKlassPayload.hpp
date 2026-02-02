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

#ifndef SHARE_VM_OOPS_INLINEKLASSPAYLOAD_HPP
#define SHARE_VM_OOPS_INLINEKLASSPAYLOAD_HPP

#include "oops/inlineOop.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/layoutKind.hpp"
#include "oops/oopsHierarchy.hpp"
#include "runtime/handles.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"
#include "utilities/ostream.hpp"

class fieldDescriptor;
class JavaThread;
class outputStream;
class ResolvedFieldEntry;

template <typename OopOrHandle> class InlineKlassPayloadImpl {
  // Friend each other to use the private interface
  friend class InlineKlassPayloadImpl<oop>;
  friend class InlineKlassPayloadImpl<Handle>;

private:
  mutable OopOrHandle _holder;
  InlineKlass* _klass;
  size_t _offset;
  LayoutKind _layout_kind;

protected:
  static constexpr size_t BAD_OFFSET = ~0u;

  InlineKlassPayloadImpl() = default;
  InlineKlassPayloadImpl(const InlineKlassPayloadImpl&) = default;
  InlineKlassPayloadImpl& operator=(const InlineKlassPayloadImpl&) = default;

  // Constructed from parts
  inline InlineKlassPayloadImpl(oop holder, InlineKlass* klass, size_t offset,
                                LayoutKind layout_kind);
  inline InlineKlassPayloadImpl(oop holder, InlineKlass* klass, size_t offset,
                                LayoutKind layout_kind, JavaThread* thread);

  inline void set_offset(size_t offset);

public:
  inline oop get_holder() const;
  inline InlineKlass* get_klass() const;
  inline size_t get_offset() const;
  inline LayoutKind get_layout_kind() const;

  inline address get_address() const;

  inline bool has_null_marker() const;
  inline void mark_as_non_null();
  inline void mark_as_null();
  inline bool is_payload_null() const;

private:
  inlineOop allocate_instance(TRAPS) const;

  inline void print_on(outputStream* st) const NOT_DEBUG_RETURN;
  inline void assert_post_construction_invariants() const NOT_DEBUG_RETURN;
  template <typename PayloadA, typename PayloadB>
  static inline void
  assert_pre_copy_invariants(const PayloadA& src, const PayloadB& dst,
                             LayoutKind copy_layout_kind) NOT_DEBUG_RETURN;

  template <typename PayloadA, typename PayloadB>
  static inline void copy(const PayloadA& src, const PayloadB& dst,
                          LayoutKind copy_layout_kind);

public:
  class BufferedInlineKlassPayloadImpl;
  class FlatInlineKlassPayloadImpl;
  class FlatFieldInlineKlassPayloadImpl;
  class FlatArrayInlineKlassPayloadImpl;
};

using InlineKlassPayload = InlineKlassPayloadImpl<oop>;
using InlineKlassPayloadHandle = InlineKlassPayloadImpl<Handle>;

class BufferedInlineKlassPayload;
class BufferedInlineKlassPayloadHandle;

template <typename OopOrHandle>
class InlineKlassPayloadImpl<OopOrHandle>::BufferedInlineKlassPayloadImpl
    : public InlineKlassPayloadImpl {
protected:
  using InlineKlassPayloadImpl::InlineKlassPayloadImpl;

public:
  BufferedInlineKlassPayloadImpl() = default;
  BufferedInlineKlassPayloadImpl(const BufferedInlineKlassPayloadImpl&) =
      default;
  BufferedInlineKlassPayloadImpl&
  operator=(const BufferedInlineKlassPayloadImpl&) = default;

  inline inlineOop get_holder() const;

  [[nodiscard]] inline inlineOop make_private_buffer(TRAPS);

  inline void copy_to(const BufferedInlineKlassPayload& dst);
  inline void copy_to(const BufferedInlineKlassPayloadHandle& dst);
};

class BufferedInlineKlassPayload
    : public InlineKlassPayload::BufferedInlineKlassPayloadImpl {
protected:
  using BufferedInlineKlassPayloadImpl::BufferedInlineKlassPayloadImpl;

public:
  BufferedInlineKlassPayload() = default;
  explicit inline BufferedInlineKlassPayload(inlineOop buffer);
  inline BufferedInlineKlassPayload(inlineOop buffer, InlineKlass* klass);

  [[nodiscard]] static inline BufferedInlineKlassPayload
  construct_from_parts(oop holder, InlineKlass* klass, size_t offset,
                       LayoutKind layout_kind);
};

class BufferedInlineKlassPayloadHandle
    : public InlineKlassPayloadHandle::BufferedInlineKlassPayloadImpl {
protected:
  using BufferedInlineKlassPayloadImpl::BufferedInlineKlassPayloadImpl;

public:
  BufferedInlineKlassPayloadHandle() = default;

  explicit inline BufferedInlineKlassPayloadHandle(inlineOop buffer,
                                                   JavaThread* thread);
  inline BufferedInlineKlassPayloadHandle(inlineOop buffer, InlineKlass* klass,
                                          JavaThread* thread);

  [[nodiscard]] static inline BufferedInlineKlassPayloadHandle
  construct_from_parts(oop holder, InlineKlass* klass, size_t offset,
                       LayoutKind layout_kind, JavaThread* thread);
};

class FlatFieldInlineKlassPayload;
class FlatFieldInlineKlassPayloadHandle;

class FlatArrayInlineKlassPayload;
class FlatArrayInlineKlassPayloadHandle;

template <typename OopOrHandle>
class InlineKlassPayloadImpl<OopOrHandle>::FlatInlineKlassPayloadImpl
    : public InlineKlassPayloadImpl {
protected:
  using InlineKlassPayloadImpl::InlineKlassPayloadImpl;

private:
  template <typename OtherOopOrHandle>
  [[nodiscard]] inline bool
  copy_to_helper(InlineKlassPayloadImpl<OtherOopOrHandle>& dst);
  template <typename OtherOopOrHandle>
  inline void copy_from_helper(InlineKlassPayloadImpl<OtherOopOrHandle>& src);

public:
  FlatInlineKlassPayloadImpl() = default;
  FlatInlineKlassPayloadImpl(const FlatInlineKlassPayloadImpl&) = default;
  FlatInlineKlassPayloadImpl&
  operator=(const FlatInlineKlassPayloadImpl&) = default;

  [[nodiscard]] inline bool copy_to(BufferedInlineKlassPayload& dst);
  [[nodiscard]] inline bool copy_to(BufferedInlineKlassPayloadHandle& dst);
  [[nodiscard]] inline bool
  copy_to_uninitialized(BufferedInlineKlassPayload& dst);
  [[nodiscard]] inline bool
  copy_to_uninitialized(BufferedInlineKlassPayloadHandle& dst);
  inline void copy_from_non_null(BufferedInlineKlassPayload& src);
  inline void copy_from_non_null(BufferedInlineKlassPayloadHandle& src);

  inline void copy_to(const FlatFieldInlineKlassPayload& dst);
  inline void copy_to(const FlatFieldInlineKlassPayloadHandle& dst);

  inline void copy_to(const FlatArrayInlineKlassPayload& dst);
  inline void copy_to(const FlatArrayInlineKlassPayloadHandle& dst);

  [[nodiscard]] inline inlineOop read(TRAPS);
  inline void write_without_nullability_check(inlineOop obj);
  inline void write(inlineOop obj, TRAPS);

  [[nodiscard]] static inline FlatInlineKlassPayloadImpl
  construct_from_parts(oop holder, InlineKlass* klass, size_t offset,
                       LayoutKind layout_kind);
  [[nodiscard]] static inline FlatInlineKlassPayloadImpl
  construct_from_parts(oop holder, InlineKlass* klass, size_t offset,
                       LayoutKind layout_kind, JavaThread* thread);
};

using FlatInlineKlassPayload = InlineKlassPayload::FlatInlineKlassPayloadImpl;
using FlatInlineKlassPayloadHandle =
    InlineKlassPayloadHandle::FlatInlineKlassPayloadImpl;

template <typename OopOrHandle>
class InlineKlassPayloadImpl<OopOrHandle>::FlatFieldInlineKlassPayloadImpl
    : public FlatInlineKlassPayloadImpl {
protected:
  using FlatInlineKlassPayloadImpl::FlatInlineKlassPayloadImpl;

  inline FlatFieldInlineKlassPayloadImpl(instanceOop holder, size_t offset,
                                         InlineLayoutInfo* inline_layout_info);
  inline FlatFieldInlineKlassPayloadImpl(instanceOop holder, size_t offset,
                                         InlineLayoutInfo* inline_layout_info,
                                         JavaThread* thread);

public:
  inline instanceOop get_holder() const;
};

class FlatFieldInlineKlassPayload
    : public InlineKlassPayload::FlatFieldInlineKlassPayloadImpl {
protected:
  using FlatFieldInlineKlassPayloadImpl::FlatFieldInlineKlassPayloadImpl;

public:
  FlatFieldInlineKlassPayload() = default;

  inline FlatFieldInlineKlassPayload(instanceOop holder,
                                     fieldDescriptor* field_descriptor);
  inline FlatFieldInlineKlassPayload(instanceOop holder,
                                     fieldDescriptor* field_descriptor,
                                     InstanceKlass* klass);

  inline FlatFieldInlineKlassPayload(instanceOop holder,
                                     ResolvedFieldEntry* resolved_field_entry);
  inline FlatFieldInlineKlassPayload(instanceOop holder,
                                     ResolvedFieldEntry* resolved_field_entry,
                                     InstanceKlass* klass);

  [[nodiscard]] static inline FlatFieldInlineKlassPayload
  construct_from_parts(instanceOop holder, InlineKlass* klass, size_t offset,
                       LayoutKind layout_kind);
};

class FlatFieldInlineKlassPayloadHandle
    : public InlineKlassPayloadHandle::FlatFieldInlineKlassPayloadImpl {
protected:
  using FlatFieldInlineKlassPayloadImpl::FlatFieldInlineKlassPayloadImpl;

public:
  FlatFieldInlineKlassPayloadHandle() = default;

  inline FlatFieldInlineKlassPayloadHandle(instanceOop holder,
                                           fieldDescriptor* field_descriptor,
                                           JavaThread* thread);
  inline FlatFieldInlineKlassPayloadHandle(instanceOop holder,
                                           fieldDescriptor* field_descriptor,
                                           InstanceKlass* klass,
                                           JavaThread* thread);

  inline FlatFieldInlineKlassPayloadHandle(
      instanceOop holder, ResolvedFieldEntry* resolved_field_entry,
      JavaThread* thread);
  inline FlatFieldInlineKlassPayloadHandle(
      instanceOop holder, ResolvedFieldEntry* resolved_field_entry,
      InstanceKlass* klass, JavaThread* thread);

  [[nodiscard]] static inline FlatFieldInlineKlassPayloadHandle
  construct_from_parts(instanceOop holder, InlineKlass* klass, size_t offset,
                       LayoutKind layout_kind, JavaThread* thread);
};

template <typename OopOrHandle>
class InlineKlassPayloadImpl<OopOrHandle>::FlatArrayInlineKlassPayloadImpl
    : public FlatInlineKlassPayloadImpl {
private:
  jint _layout_helper;
  int _element_size;

protected:
  using FlatInlineKlassPayloadImpl::FlatInlineKlassPayloadImpl;

  inline FlatArrayInlineKlassPayloadImpl(flatArrayOop holder,
                                         InlineKlass* klass, size_t offset,
                                         LayoutKind layout_kind,
                                         jint layout_helper, int element_size);
  inline FlatArrayInlineKlassPayloadImpl(flatArrayOop holder,
                                         InlineKlass* klass, size_t offset,
                                         LayoutKind layout_kind,
                                         jint layout_helper, int element_size,
                                         JavaThread* thread);

public:
  FlatArrayInlineKlassPayloadImpl() = default;
  FlatArrayInlineKlassPayloadImpl(const FlatArrayInlineKlassPayloadImpl&) =
      default;
  FlatArrayInlineKlassPayloadImpl&
  operator=(const FlatArrayInlineKlassPayloadImpl&) = default;

  inline flatArrayOop get_holder() const;

  inline void set_index(int index);
  inline void advance_index(int delta);

  inline void next_element();
  inline void previous_element();

private:
  inline void set_offset(size_t offset);
};

class FlatArrayInlineKlassPayload
    : public InlineKlassPayload::FlatArrayInlineKlassPayloadImpl {
protected:
  using FlatArrayInlineKlassPayloadImpl::FlatArrayInlineKlassPayloadImpl;

public:
  FlatArrayInlineKlassPayload() = default;

  explicit inline FlatArrayInlineKlassPayload(flatArrayOop holder);
  inline FlatArrayInlineKlassPayload(flatArrayOop holder,
                                     FlatArrayKlass* klass);

  inline FlatArrayInlineKlassPayload(flatArrayOop holder, int index);
  inline FlatArrayInlineKlassPayload(flatArrayOop holder, int index,
                                     FlatArrayKlass* klass);

  [[nodiscard]] static inline FlatArrayInlineKlassPayload
  construct_from_parts(flatArrayOop holder, InlineKlass* klass, size_t offset,
                       LayoutKind layout_kind);
  [[nodiscard]] static inline FlatArrayInlineKlassPayload
  construct_from_parts(flatArrayOop holder, InlineKlass* klass, size_t offset,
                       LayoutKind layout_kind, FlatArrayKlass* holder_klass);
};

class FlatArrayInlineKlassPayloadHandle
    : public InlineKlassPayloadHandle::FlatArrayInlineKlassPayloadImpl {
protected:
  using FlatArrayInlineKlassPayloadImpl::FlatArrayInlineKlassPayloadImpl;

public:
  FlatArrayInlineKlassPayloadHandle() = default;

  explicit inline FlatArrayInlineKlassPayloadHandle(flatArrayOop holder,
                                                    JavaThread* thread);
  inline FlatArrayInlineKlassPayloadHandle(flatArrayOop holder,
                                           FlatArrayKlass* klass,
                                           JavaThread* thread);

  inline FlatArrayInlineKlassPayloadHandle(flatArrayOop holder, int index,
                                           JavaThread* thread);
  inline FlatArrayInlineKlassPayloadHandle(flatArrayOop holder, int index,
                                           FlatArrayKlass* klass,
                                           JavaThread* thread);

  [[nodiscard]] static inline FlatArrayInlineKlassPayloadHandle
  construct_from_parts(flatArrayOop holder, InlineKlass* klass, size_t offset,
                       LayoutKind layout_kind, JavaThread* thread);
  [[nodiscard]] static inline FlatArrayInlineKlassPayloadHandle
  construct_from_parts(flatArrayOop holder, InlineKlass* klass, size_t offset,
                       LayoutKind layout_kind, FlatArrayKlass* holder_klass,
                       JavaThread* thread);
};

#endif // SHARE_VM_OOPS_INLINEKLASSPAYLOAD_HPP
