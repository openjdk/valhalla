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

#ifndef SHARE_VM_OOPS_INLINEKLASS_HPP
#define SHARE_VM_OOPS_INLINEKLASS_HPP

#include "classfile/classFileParser.hpp"
#include "classfile/javaClasses.hpp"
#include "oops/arrayKlass.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/method.hpp"
#include "runtime/registerMap.hpp"
#include "utilities/powerOfTwo.hpp"

// An InlineKlass is a specialized InstanceKlass for concrete value classes
// (abstract value classes are represented by InstanceKlass)

/**
  There are 2 ways to access a nullable atomic field or array element. If the payload including the
  null marker fits into a jlong, then we can just access the element as a whole. Otherwise, we can
  yry another strategy, since the payload is only relevant if the null marker is 1. We can achieve
  a field that is accessed as if it is atomic even if the access consists of 2 native accesses.

  A store of a not-null Long into a nullable Long field can be executed as:

    store field.value;
    release_fence;
    store field.null_marker;

  and the store of a null into that field will be:

    store field.null_marker;

  While, a load can be executed as:

    load field.null_marker;
    acquire_fence;
    load field.value;

  What we need to prove is that, given n concurrent stores, then:

  1. The final state of the memory must be one of the executed stores:
    Consider the stores into the null marker:
    - If the last state of the null marker is 0, then the field is null
    - If the last state of the null marker is 1, then the field is non-null. In this case, only the
      threads that store non-null Long objects touch the memory of value. One of which would be the
      last state of the memory here. And it is as if we have a single non-null store that is the
      last state.

  Note that the fences are irrelevant for these conditions.

  2. Given a concurrent load, then it must either observe the initial state, or 1 of the
     stores that is executing:

    - If it observes the null marker being 0, then it observes field being null. In this case, only
      the null marker is relevant, and it is trivially atomic.
    - If it observes the null marker being 1, then it observes field being non-null. In this case,
      if the initial state is null, we must observe the null marker being stored by 1 of the
      threads. And since we have fences, we must at least observe the value stored by that thread
      (or another thread, the point here is that we cannot observe the value in its initial state).
      Otherwise, the original state is non-null, we must observe the initial value or one of the
      values stored by the threads that try to store non-null.

  As a result, we can see that in any case, the field accesses act as it they are atomic.

  Note that a store of null to a flattened field ignores the payload, so we avoid flattening like
  this if the class has oop fields because they can leak.
*/
class InlineKlass: public InstanceKlass {
  friend class VMStructs;
  friend class InstanceKlass;
  friend class ClassFileParser;

 public:
  static const KlassKind Kind = InlineKlassKind;

  InlineKlass();

 private:

  // Constructor
  InlineKlass(const ClassFileParser& parser);

  void init_fixed_block();
  inline InlineKlassFixedBlock* inlineklass_static_block() const;
  inline address adr_return_regs() const;

  address adr_extended_sig() const {
    assert(_adr_inlineklass_fixed_block != nullptr, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _extended_sig));
  }

  // pack and unpack handlers for inline types return
  address adr_pack_handler() const {
    assert(_adr_inlineklass_fixed_block != nullptr, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _pack_handler));
  }

  address adr_pack_handler_jobject() const {
    assert(_adr_inlineklass_fixed_block != nullptr, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _pack_handler_jobject));
  }

  address adr_unpack_handler() const {
    assert(_adr_inlineklass_fixed_block != nullptr, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _unpack_handler));
  }

  address adr_null_reset_value_offset() const {
    assert(_adr_inlineklass_fixed_block != nullptr, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(null_reset_value_offset_offset());
  }

  address adr_payload_offset() const {
    assert(_adr_inlineklass_fixed_block != nullptr, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _payload_offset));
  }

  address adr_payload_size_in_bytes() const {
    assert(_adr_inlineklass_fixed_block != nullptr, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _payload_size_in_bytes));
  }

  address adr_payload_alignment() const {
    assert(_adr_inlineklass_fixed_block != nullptr, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _payload_alignment));
  }

  address adr_non_atomic_size_in_bytes() const {
    assert(_adr_inlineklass_fixed_block != nullptr, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _non_atomic_size_in_bytes));
  }

  address adr_non_atomic_alignment() const {
    assert(_adr_inlineklass_fixed_block != nullptr, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _non_atomic_alignment));
  }

  address adr_atomic_size_in_bytes() const {
    assert(_adr_inlineklass_fixed_block != nullptr, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _atomic_size_in_bytes));
  }

  address adr_nullable_atomic_size_in_bytes() const {
    assert(_adr_inlineklass_fixed_block != nullptr, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _nullable_size_in_bytes));
  }

  address adr_null_marker_offset() const {
    assert(_adr_inlineklass_fixed_block != nullptr, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _null_marker_offset));
  }

 public:

  bool is_empty_inline_type() const   { return _misc_flags.is_empty_inline_type(); }
  void set_is_empty_inline_type()     { _misc_flags.set_is_empty_inline_type(true); }

  int payload_offset() const {
    int offset = *(int*)adr_payload_offset();
    assert(offset != 0, "Must be initialized before use");
    return offset;
  }

  void set_payload_offset(int offset) { *(int*)adr_payload_offset() = offset; }

  int payload_size_in_bytes() const { return *(int*)adr_payload_size_in_bytes(); }
  void set_payload_size_in_bytes(int payload_size) { *(int*)adr_payload_size_in_bytes() = payload_size; }

  int payload_alignment() const { return *(int*)adr_payload_alignment(); }
  void set_payload_alignment(int alignment) { *(int*)adr_payload_alignment() = alignment; }

  bool has_non_atomic_layout() const { return non_atomic_size_in_bytes() != -1; }
  int non_atomic_size_in_bytes() const { return *(int*)adr_non_atomic_size_in_bytes(); }
  void set_non_atomic_size_in_bytes(int size) { *(int*)adr_non_atomic_size_in_bytes() = size; }
  int non_atomic_alignment() const { return *(int*)adr_non_atomic_alignment(); }
  void set_non_atomic_alignment(int alignment) { *(int*)adr_non_atomic_alignment() = alignment; }

  bool has_atomic_layout() const { return atomic_size_in_bytes() != -1; }
  int atomic_size_in_bytes() const { return *(int*)adr_atomic_size_in_bytes(); }
  void set_atomic_size_in_bytes(int size) { *(int*)adr_atomic_size_in_bytes() = size; }

  bool has_nullable_atomic_layout() const { return nullable_atomic_size_in_bytes() != -1; }
  int nullable_atomic_size_in_bytes() const { return *(int*)adr_nullable_atomic_size_in_bytes(); }
  void set_nullable_size_in_bytes(int size) { *(int*)adr_nullable_atomic_size_in_bytes() = size; }
  int null_marker_offset() const { return *(int*)adr_null_marker_offset(); }
  int null_marker_offset_in_payload() const { return null_marker_offset() - payload_offset(); }
  void set_null_marker_offset(int offset) { *(int*)adr_null_marker_offset() = offset; }

  bool nullable_atomic_layout_is_natural() const {
    assert(has_nullable_atomic_layout(), "must have the layout the query its nature");
    return is_power_of_2(nullable_atomic_size_in_bytes());
  }

  bool is_payload_marked_as_null(address payload) {
    assert(has_nullable_atomic_layout(), " Must have");
    return *((jbyte*)payload + null_marker_offset_in_payload()) == 0;
  }

  void mark_payload_as_non_null(address payload) {
    assert(has_nullable_atomic_layout(), " Must have");
    *((jbyte*)payload + null_marker_offset_in_payload()) = 1;
  }

  void mark_payload_as_null(address payload) {
    assert(has_nullable_atomic_layout(), " Must have");
    *((jbyte*)payload + null_marker_offset_in_payload()) = 0;
  }

  bool is_layout_supported(LayoutKind lk);

  int layout_alignment(LayoutKind kind) const;
  int layout_size_in_bytes(LayoutKind kind) const;

#if INCLUDE_CDS
  virtual void remove_unshareable_info();
  virtual void remove_java_mirror();
  virtual void restore_unshareable_info(ClassLoaderData* loader_data, Handle protection_domain, PackageEntry* pkg_entry, TRAPS);
  virtual void metaspace_pointers_do(MetaspaceClosure* it);
#endif

 private:
  int collect_fields(GrowableArray<SigEntry>* sig, int base_off = 0, int null_marker_offset = -1);

  void cleanup_blobs();

 public:
  // Type testing
  bool is_inline_klass_slow() const        { return true; }

  // Casting from Klass*

  static InlineKlass* cast(Klass* k) {
    return const_cast<InlineKlass*>(cast(const_cast<const Klass*>(k)));
  }

  static const InlineKlass* cast(const Klass* k) {
    assert(k != nullptr, "k should not be null");
    assert(k->is_inline_klass(), "cast to InlineKlass");
    return static_cast<const InlineKlass*>(k);
  }

  // Use this to return the size of an instance in heap words.
  // Note that this size only applies to heap allocated stand-alone instances.
  virtual int size_helper() const {
    return layout_helper_to_size_helper(layout_helper());
  }

  // allocate_instance() allocates a stand alone value in the Java heap
  // initialized to default value (cleared memory)
  instanceOop allocate_instance(TRAPS);
  // allocates a stand alone inline buffer in the Java heap
  // DOES NOT have memory cleared, user MUST initialize payload before
  // returning to Java (i.e.: inline_copy)
  instanceOop allocate_instance_buffer(TRAPS);

  address payload_addr(oop o) const;

  bool maybe_flat_in_array();

  bool contains_oops() const { return nonstatic_oop_map_count() > 0; }
  int nonstatic_oop_count();

  // Methods to copy payload between containers
  // Methods taking a LayoutKind argument expect that both the source and the destination
  // layouts are compatible with the one specified in argument (alignment, size, presence
  // of a null marker). Reminder: the BUFFERED layout, used in values buffered in heap,
  // is compatible with all the other layouts.

  void write_value_to_addr(oop src, void* dst, LayoutKind lk, bool dest_is_initialized, TRAPS);
  oop read_payload_from_addr(const oop src, int offset, LayoutKind lk, TRAPS);
  void copy_payload_to_addr(void* src, void* dst, LayoutKind lk, bool dest_is_initialized);

  // oop iterate raw inline type data pointer (where oop_addr may not be an oop, but backing/array-element)
  template <typename T, class OopClosureType>
  inline void oop_iterate_specialized(const address oop_addr, OopClosureType* closure);

  template <typename T, class OopClosureType>
  inline void oop_iterate_specialized_bounded(const address oop_addr, OopClosureType* closure, void* lo, void* hi);

  // calling convention support
  void initialize_calling_convention(TRAPS);
  Array<SigEntry>* extended_sig() const {
    return *((Array<SigEntry>**)adr_extended_sig());
  }
  inline Array<VMRegPair>* return_regs() const;
  bool can_be_passed_as_fields() const;
  bool can_be_returned_as_fields(bool init = false) const;
  void save_oop_fields(const RegisterMap& map, GrowableArray<Handle>& handles) const;
  void restore_oop_results(RegisterMap& map, GrowableArray<Handle>& handles) const;
  oop realloc_result(const RegisterMap& reg_map, const GrowableArray<Handle>& handles, TRAPS);
  static InlineKlass* returned_inline_klass(const RegisterMap& reg_map, bool* return_oop = nullptr, Method* method = nullptr);

  address pack_handler() const {
    return *(address*)adr_pack_handler();
  }

  address unpack_handler() const {
    return *(address*)adr_unpack_handler();
  }

  // pack and unpack handlers. Need to be loadable from generated code
  // so at a fixed offset from the base of the klass pointer.
  static ByteSize pack_handler_offset() {
    return byte_offset_of(InlineKlassFixedBlock, _pack_handler);
  }

  static ByteSize pack_handler_jobject_offset() {
    return byte_offset_of(InlineKlassFixedBlock, _pack_handler_jobject);
  }

  static ByteSize unpack_handler_offset() {
    return byte_offset_of(InlineKlassFixedBlock, _unpack_handler);
  }

  static ByteSize null_reset_value_offset_offset() {
    return byte_offset_of(InlineKlassFixedBlock, _null_reset_value_offset);
  }

  static ByteSize payload_offset_offset() {
    return byte_offset_of(InlineKlassFixedBlock, _payload_offset);
  }

  static ByteSize null_marker_offset_offset() {
    return byte_offset_of(InlineKlassFixedBlock, _null_marker_offset);
  }

  void set_null_reset_value_offset(int offset) {
    *((int*)adr_null_reset_value_offset()) = offset;
  }

  int null_reset_value_offset() {
    int offset = *((int*)adr_null_reset_value_offset());
    assert(offset != 0, "must not be called if not initialized");
    return offset;
  }

  void set_null_reset_value(oop val);

  oop null_reset_value() {
    assert(is_initialized() || is_being_initialized() || is_in_error_state(), "null reset value is set at the beginning of initialization");
    oop val = java_mirror()->obj_field_acquire(null_reset_value_offset());
    assert(val != nullptr, "Sanity check");
    return val;
  }

  void deallocate_contents(ClassLoaderData* loader_data);
  static void cleanup(InlineKlass* ik) ;

  // Verification
  void verify_on(outputStream* st);
  void oop_verify_on(oop obj, outputStream* st);

};

#endif /* SHARE_VM_OOPS_INLINEKLASS_HPP */
