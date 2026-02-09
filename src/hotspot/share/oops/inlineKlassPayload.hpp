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
#include "oops/oopHandle.hpp"
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

class ValuePayload {
private:
  template <typename Holder> struct StorageImpl {
    mutable Holder _holder;
    InlineKlass* _klass;
    ptrdiff_t _offset;
    LayoutKind _layout_kind;
  };

  using Storage = StorageImpl<oop>;

  Storage _storage;
  DEBUG_ONLY(bool _is_raw;)

protected:
  static constexpr ptrdiff_t BAD_OFFSET = -1;

  ValuePayload() = default;
  ValuePayload(const ValuePayload&) = default;
  ValuePayload& operator=(const ValuePayload&) = default;

  // Constructed from parts
  inline ValuePayload(oop holder,
                      InlineKlass* klass,
                      ptrdiff_t offset,
                      LayoutKind layout_kind
                      DEBUG_ONLY(COMMA bool is_raw = false));

  inline void set_offset(ptrdiff_t offset);
  inlineOop allocate_instance(TRAPS) const;

  static inline void copy(const ValuePayload& src, const ValuePayload& dst,
                          LayoutKind copy_layout_kind);

  inline void mark_as_non_null();
  inline void mark_as_null();

private:
  DEBUG_ONLY(inline bool is_raw() const;)
  inline void print_on(outputStream* st) const NOT_DEBUG_RETURN;
  inline void assert_post_construction_invariants() const NOT_DEBUG_RETURN;
  static inline void
  assert_pre_copy_invariants(const ValuePayload& src, const ValuePayload& dst,
                             LayoutKind copy_layout_kind) NOT_DEBUG_RETURN;

public:
  inline oop holder() const;
  inline InlineKlass* klass() const;
  inline ptrdiff_t offset() const;
  inline LayoutKind layout_kind() const;

  inline ::address address() const;

  inline bool has_null_marker() const;
  inline bool is_payload_null() const;

  class Handle;
  class OopHandle;

  inline Handle make_handle(JavaThread* thread) const;
  inline OopHandle make_oop_handle(OopStorage* storage) const;
};

class RawValuePayload : public ValuePayload {
public:
  RawValuePayload() = default;

  inline RawValuePayload(::address payload_address, InlineKlass* klass,
                         LayoutKind layout_kind);
};

class BufferedValuePayload : public ValuePayload {
  friend class FlatValuePayload;

protected:
  using ValuePayload::ValuePayload;

public:
  BufferedValuePayload() = default;
  BufferedValuePayload(const BufferedValuePayload&) = default;
  BufferedValuePayload& operator=(const BufferedValuePayload&) = default;

  explicit inline BufferedValuePayload(inlineOop buffer);
  inline BufferedValuePayload(inlineOop buffer, InlineKlass* klass);

  inline inlineOop get_holder() const;

  [[nodiscard]] inline inlineOop make_private_buffer(TRAPS);

  inline void copy_to(const BufferedValuePayload& dst);

  [[nodiscard]] static inline BufferedValuePayload
  construct_from_parts(oop holder, InlineKlass* klass, ptrdiff_t offset,
                       LayoutKind layout_kind);

  class Handle;
  class OopHandle;

  inline Handle make_handle(JavaThread* thread) const;
  inline OopHandle make_oop_handle(OopStorage* storage) const;
};

class FlatValuePayload : public ValuePayload {
protected:
  using ValuePayload::ValuePayload;

public:
  FlatValuePayload() = default;
  FlatValuePayload(const FlatValuePayload&) = default;
  FlatValuePayload& operator=(const FlatValuePayload&) = default;

  [[nodiscard]] inline bool copy_to(BufferedValuePayload& dst);
  inline void copy_from_non_null(BufferedValuePayload& src);

  inline void copy_to(const FlatValuePayload& dst);

  [[nodiscard]] inline inlineOop read(TRAPS);
  inline void write_without_nullability_check(inlineOop obj);
  inline void write(inlineOop obj, TRAPS);

  [[nodiscard]] static inline FlatValuePayload
  construct_from_parts(oop holder, InlineKlass* klass, ptrdiff_t offset,
                       LayoutKind layout_kind);

  class Handle;
  class OopHandle;

  inline Handle make_handle(JavaThread* thread) const;
  inline OopHandle make_oop_handle(OopStorage* storage) const;
};

class FlatFieldPayload : public FlatValuePayload {
private:
  using FlatValuePayload::FlatValuePayload;

  inline FlatFieldPayload(instanceOop holder, ptrdiff_t offset,
                          InlineLayoutInfo* inline_layout_info);

public:
  FlatFieldPayload() = default;

  inline FlatFieldPayload(instanceOop holder,
                          fieldDescriptor* field_descriptor);
  inline FlatFieldPayload(instanceOop holder, fieldDescriptor* field_descriptor,
                          InstanceKlass* klass);

  inline FlatFieldPayload(instanceOop holder,
                          ResolvedFieldEntry* resolved_field_entry);
  inline FlatFieldPayload(instanceOop holder,
                          ResolvedFieldEntry* resolved_field_entry,
                          InstanceKlass* klass);

  inline instanceOop get_holder() const;

  [[nodiscard]] static inline FlatFieldPayload
  construct_from_parts(instanceOop holder, InlineKlass* klass, ptrdiff_t offset,
                       LayoutKind layout_kind);

  class Handle;
  class OopHandle;

  inline Handle make_handle(JavaThread* thread) const;
  inline OopHandle make_oop_handle(OopStorage* storage) const;
};

class FlatArrayPayload : public FlatValuePayload {
private:
  struct Storage {
    jint _layout_helper;
    int _element_size;
  } _storage;

  using FlatValuePayload::FlatValuePayload;

  inline FlatArrayPayload(flatArrayOop holder, InlineKlass* klass,
                          ptrdiff_t offset, LayoutKind layout_kind,
                          jint layout_helper, int element_size);

public:
  FlatArrayPayload() = default;
  FlatArrayPayload(const FlatArrayPayload&) = default;
  FlatArrayPayload& operator=(const FlatArrayPayload&) = default;

  explicit inline FlatArrayPayload(flatArrayOop holder);
  inline FlatArrayPayload(flatArrayOop holder, FlatArrayKlass* klass);

  inline FlatArrayPayload(flatArrayOop holder, int index);
  inline FlatArrayPayload(flatArrayOop holder, int index,
                          FlatArrayKlass* klass);

  [[nodiscard]] static inline FlatArrayPayload
  construct_from_parts(flatArrayOop holder, InlineKlass* klass,
                       ptrdiff_t offset, LayoutKind layout_kind);
  [[nodiscard]] static inline FlatArrayPayload
  construct_from_parts(flatArrayOop holder, InlineKlass* klass,
                       ptrdiff_t offset, LayoutKind layout_kind,
                       FlatArrayKlass* holder_klass);

  inline flatArrayOop get_holder() const;

  inline void set_index(int index);
  inline void advance_index(int delta);

  inline void next_element();
  inline void previous_element();

private:
  inline void set_offset(ptrdiff_t offset);

public:
  class Handle;
  class OopHandle;

  inline Handle make_handle(JavaThread* thread) const;
  inline OopHandle make_oop_handle(OopStorage* storage) const;
};

class ValuePayload::Handle {
private:
  using Storage = StorageImpl<::Handle>;

  Storage _storage;

public:
  Handle() = default;
  Handle(const Handle&) = default;
  Handle& operator=(const Handle&) = default;

  inline Handle(const ValuePayload& payload, JavaThread* thread);

  inline oop get_holder() const;
  inline InlineKlass* get_klass() const;
  inline ptrdiff_t get_offset() const;
  inline LayoutKind get_layout_kind() const;

  inline ValuePayload operator()() const;
};

class ValuePayload::OopHandle {
private:
  using Storage = StorageImpl<::OopHandle>;

  Storage _storage;

public:
  OopHandle() = default;
  OopHandle(const OopHandle&) = default;
  OopHandle& operator=(const OopHandle&) = default;

  inline OopHandle(const ValuePayload& payload, OopStorage* storage);

  inline void release(OopStorage* storage);

  inline oop get_holder() const;
  inline InlineKlass* get_klass() const;
  inline ptrdiff_t get_offset() const;
  inline LayoutKind get_layout_kind() const;

  inline ValuePayload operator()() const;
};

class BufferedValuePayload::Handle : public ValuePayload::Handle {
public:
  using ValuePayload::Handle::Handle;

  inline BufferedValuePayload operator()() const;

  inline inlineOop get_holder() const;
};

class BufferedValuePayload::OopHandle : public ValuePayload::OopHandle {
public:
  using ValuePayload::OopHandle::OopHandle;

  inline BufferedValuePayload operator()() const;

  inline inlineOop get_holder() const;
};

class FlatValuePayload::Handle : public ValuePayload::Handle {
public:
  using ValuePayload::Handle::Handle;

  inline FlatValuePayload operator()() const;
};

class FlatValuePayload::OopHandle : public ValuePayload::OopHandle {
public:
  using ValuePayload::OopHandle::OopHandle;

  inline FlatValuePayload operator()() const;
};

class FlatFieldPayload::Handle : public FlatValuePayload::Handle {
public:
  using FlatValuePayload::Handle::Handle;

  inline FlatFieldPayload operator()() const;

  inline instanceOop get_holder() const;
};

class FlatFieldPayload::OopHandle : public FlatValuePayload::OopHandle {
public:
  using FlatValuePayload::OopHandle::OopHandle;

  inline FlatFieldPayload operator()() const;

  inline instanceOop get_holder() const;
};

class FlatArrayPayload::Handle : public FlatValuePayload::Handle {
private:
  FlatArrayPayload::Storage _storage;

public:
  inline Handle(const FlatArrayPayload& payload, JavaThread* thread);

  inline FlatArrayPayload operator()() const;

  inline flatArrayOop get_holder() const;
};

class FlatArrayPayload::OopHandle : public FlatValuePayload::OopHandle {
private:
  FlatArrayPayload::Storage _storage;

public:
  inline OopHandle(const FlatArrayPayload& payload, OopStorage* storage);

  inline FlatArrayPayload operator()() const;

  inline flatArrayOop get_holder() const;
};

#endif // SHARE_VM_OOPS_INLINEKLASSPAYLOAD_HPP
