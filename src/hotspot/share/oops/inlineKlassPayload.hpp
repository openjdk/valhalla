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

class fieldDescriptor;
class ResolvedFieldEntry;

template <typename OopOrHandle>
class InlineKlassPayloadImpl {
  // Friend each other to use the private interface
  friend class InlineKlassPayloadImpl<oop>;
  friend class InlineKlassPayloadImpl<Handle>;

private:
  static constexpr size_t BAD_OFFSET = ~0u;

  mutable OopOrHandle _holder;
  InlineKlass* _klass;
  size_t _offset;
  LayoutKind _layout_kind;

  inline void assert_invariants() const;

  inlineOop allocate_instance(TRAPS) const;

  inline InlineKlassPayloadImpl(instanceOop oop, size_t offset, InlineLayoutInfo* inline_layout_info);


public: // TEMPORARY
  inline bool has_null_marker() const;

  inline void mark_as_non_null();

public:
  // Empty constructor
  inline InlineKlassPayloadImpl();
  // Constructed from parts
  inline InlineKlassPayloadImpl(oop oop, InlineKlass* klass, size_t offset, LayoutKind layout_kind);

  explicit inline InlineKlassPayloadImpl(inlineOop oop);
  inline InlineKlassPayloadImpl(inlineOop oop, InlineKlass* klass);

  // TODO: Maybe add a NoIndex{} marker
  explicit inline InlineKlassPayloadImpl(flatArrayOop oop);
  inline InlineKlassPayloadImpl(flatArrayOop oop, FlatArrayKlass* klass);

  inline InlineKlassPayloadImpl(flatArrayOop oop, int index);
  inline InlineKlassPayloadImpl(flatArrayOop oop, int index, FlatArrayKlass* klass);

  inline InlineKlassPayloadImpl(instanceOop oop, fieldDescriptor* field_descriptor);
  inline InlineKlassPayloadImpl(instanceOop oop, fieldDescriptor* field_descriptor, InstanceKlass* klass);
  inline InlineKlassPayloadImpl(instanceOop oop, ResolvedFieldEntry* resolved_field_entry);
  inline InlineKlassPayloadImpl(instanceOop oop, ResolvedFieldEntry* resolved_field_entry, InstanceKlass* klass);

  inline oop get_holder() const;
  inline InlineKlass* get_klass() const;
  inline size_t get_offset() const;
  inline LayoutKind get_layout_kind() const;

  inline address get_address() const;
  inline bool is_marked_as_null() const;

  // TODO: Cache layout helper or create more specialized payload type for arrays.
  // TODO: Maybe add delta index versions as well.
  inline void set_index(int index);
  inline void set_index(int index, FlatArrayKlass* klass);
  inline void set_index(int index, jint layout_helper);

  // Methods to copy payload between containers
  //
  // Methods taking a LayoutKind argument expect that both the source and the destination
  // layouts are compatible with the one specified in argument (alignment, size, presence
  // of a null marker). Reminder: the BUFFERED layout, used in values buffered in heap,
  // is compatible with all the other layouts.

private:
  template <typename PayloadA, typename PayloadB>
  static inline void copy(const PayloadA& src, const PayloadB& dst, LayoutKind copy_layout_kind);

public:

  inline inlineOop read(TRAPS);
  template <typename OtherOopOrHandle>
  inline void copy_to(const InlineKlassPayloadImpl<OtherOopOrHandle>& dst);
  template <typename OtherOopOrHandle>
  inline void copy_from(const InlineKlassPayloadImpl<OtherOopOrHandle>& src);
  inline void write(inlineOop obj);
  inline void write(inlineOop obj, TRAPS);
};

using InlineKlassPayload = InlineKlassPayloadImpl<oop>;
using InlineKlassPayloadHandle = InlineKlassPayloadImpl<Handle>;

#endif // SHARE_VM_OOPS_INLINEKLASSPAYLOAD_HPP
