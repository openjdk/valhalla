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
class outputStream;
class ResolvedFieldEntry;

class InlineKlassPayload {
private:
  mutable oop _holder;
  InlineKlass* _klass;
  size_t _offset;
  LayoutKind _layout_kind;

protected:
  static constexpr size_t BAD_OFFSET = ~0u;

  InlineKlassPayload() = default;
  InlineKlassPayload(const InlineKlassPayload&) = default;
  InlineKlassPayload& operator=(const InlineKlassPayload&) = default;

  // Constructed from parts
  inline InlineKlassPayload(oop holder, InlineKlass* klass, size_t offset,
                            LayoutKind layout_kind);

  inline void set_offset(size_t offset);
  inlineOop allocate_instance(TRAPS) const;

  template <typename PayloadA, typename PayloadB>
  static inline void copy(const PayloadA& src, const PayloadB& dst,
                          LayoutKind copy_layout_kind);

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
  inline void print_on(outputStream* st) const NOT_DEBUG_RETURN;
  inline void assert_post_construction_invariants() const NOT_DEBUG_RETURN;
  template <typename PayloadA, typename PayloadB>
  static inline void
  assert_pre_copy_invariants(const PayloadA& src, const PayloadB& dst,
                             LayoutKind copy_layout_kind) NOT_DEBUG_RETURN;
};

class BufferedInlineKlassPayload : public InlineKlassPayload {
protected:
  using InlineKlassPayload::InlineKlassPayload;

public:
  BufferedInlineKlassPayload() = default;
  BufferedInlineKlassPayload(const BufferedInlineKlassPayload&) = default;
  BufferedInlineKlassPayload&
  operator=(const BufferedInlineKlassPayload&) = default;

  explicit inline BufferedInlineKlassPayload(inlineOop buffer);
  inline BufferedInlineKlassPayload(inlineOop buffer, InlineKlass* klass);

  inline inlineOop get_holder() const;

  [[nodiscard]] inline inlineOop make_private_buffer(TRAPS);

  inline void copy_to(const BufferedInlineKlassPayload& dst);

  [[nodiscard]] static inline BufferedInlineKlassPayload
  construct_from_parts(oop holder, InlineKlass* klass, size_t offset,
                       LayoutKind layout_kind);
};

class FlatArrayInlineKlassPayload;
class FlatFieldInlineKlassPayload;

class FlatInlineKlassPayload : public InlineKlassPayload {
protected:
  using InlineKlassPayload::InlineKlassPayload;

private:
  inline void copy_from_helper(InlineKlassPayload& src);

public:
  FlatInlineKlassPayload() = default;
  FlatInlineKlassPayload(const FlatInlineKlassPayload&) = default;
  FlatInlineKlassPayload& operator=(const FlatInlineKlassPayload&) = default;

  [[nodiscard]] inline bool copy_to(BufferedInlineKlassPayload& dst);
  inline void copy_from_non_null(BufferedInlineKlassPayload& src);

  inline void copy_to(const FlatFieldInlineKlassPayload& dst);

  inline void copy_to(const FlatArrayInlineKlassPayload& dst);

  [[nodiscard]] inline inlineOop read(TRAPS);
  inline void write_without_nullability_check(inlineOop obj);
  inline void write(inlineOop obj, TRAPS);

  [[nodiscard]] static inline FlatInlineKlassPayload
  construct_from_parts(oop holder, InlineKlass* klass, size_t offset,
                       LayoutKind layout_kind);
};

class FlatFieldInlineKlassPayload : public FlatInlineKlassPayload {
protected:
  using FlatInlineKlassPayload::FlatInlineKlassPayload;

  inline FlatFieldInlineKlassPayload(instanceOop holder, size_t offset,
                                     InlineLayoutInfo* inline_layout_info);

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

  inline instanceOop get_holder() const;

  [[nodiscard]] static inline FlatFieldInlineKlassPayload
  construct_from_parts(instanceOop holder, InlineKlass* klass, size_t offset,
                       LayoutKind layout_kind);
};

class FlatArrayInlineKlassPayload : public FlatInlineKlassPayload {
private:
  jint _layout_helper;
  int _element_size;

protected:
  using FlatInlineKlassPayload::FlatInlineKlassPayload;

  inline FlatArrayInlineKlassPayload(flatArrayOop holder, InlineKlass* klass,
                                     size_t offset, LayoutKind layout_kind,
                                     jint layout_helper, int element_size);

public:
  FlatArrayInlineKlassPayload() = default;
  FlatArrayInlineKlassPayload(const FlatArrayInlineKlassPayload&) = default;
  FlatArrayInlineKlassPayload&
  operator=(const FlatArrayInlineKlassPayload&) = default;

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

  inline flatArrayOop get_holder() const;

  inline void set_index(int index);
  inline void advance_index(int delta);

  inline void next_element();
  inline void previous_element();

private:
  inline void set_offset(size_t offset);
};

#endif // SHARE_VM_OOPS_INLINEKLASSPAYLOAD_HPP
