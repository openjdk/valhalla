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

#ifndef SHARE_VM_OOPS_INLINEKLASS_HPP
#define SHARE_VM_OOPS_INLINEKLASS_HPP

#include "classfile/javaClasses.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/method.hpp"
#include "runtime/registerMap.hpp"
//#include "oops/oop.inline.hpp"

// An InlineKlass is a specialized InstanceKlass for inline types.


class InlineKlass: public InstanceKlass {
  friend class VMStructs;
  friend class InstanceKlass;

 public:
  InlineKlass() { assert(DumpSharedSpaces || UseSharedSpaces, "only for CDS"); }

 private:

  // Constructor
  InlineKlass(const ClassFileParser& parser);

  inline InlineKlassFixedBlock* inlineklass_static_block() const;
  inline address adr_return_regs() const;

  address adr_extended_sig() const {
    assert(_adr_inlineklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _extended_sig));
  }

  // pack and unpack handlers for inline types return
  address adr_pack_handler() const {
    assert(_adr_inlineklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _pack_handler));
  }

  address adr_pack_handler_jobject() const {
    assert(_adr_inlineklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _pack_handler_jobject));
  }

  address adr_unpack_handler() const {
    assert(_adr_inlineklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _unpack_handler));
  }

  address adr_default_value_offset() const {
    assert(_adr_inlineklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(default_value_offset_offset());
  }

  address adr_alignment() const {
    assert(_adr_inlineklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _alignment));
  }

  address adr_first_field_offset() const {
    assert(_adr_inlineklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _first_field_offset));
  }

  address adr_exact_size_in_bytes() const {
    assert(_adr_inlineklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_inlineklass_fixed_block) + in_bytes(byte_offset_of(InlineKlassFixedBlock, _exact_size_in_bytes));
  }

 public:
  int get_alignment() const {
    return *(int*)adr_alignment();
  }

  void set_alignment(int alignment) {
    *(int*)adr_alignment() = alignment;
  }

  int first_field_offset() const {
    int offset = *(int*)adr_first_field_offset();
    assert(offset != 0, "Must be initialized before use");
    return *(int*)adr_first_field_offset();
  }

  void set_first_field_offset(int offset) {
    *(int*)adr_first_field_offset() = offset;
  }

  int get_exact_size_in_bytes() const {
    return *(int*)adr_exact_size_in_bytes();
  }

  void set_exact_size_in_bytes(int exact_size) {
    *(int*)adr_exact_size_in_bytes() = exact_size;
  }

  int first_field_offset_old();

  virtual void remove_unshareable_info();
  virtual void restore_unshareable_info(ClassLoaderData* loader_data, Handle protection_domain, PackageEntry* pkg_entry, TRAPS);
  virtual void metaspace_pointers_do(MetaspaceClosure* it);

 private:
  int collect_fields(GrowableArray<SigEntry>* sig, int base_off = 0);

  void cleanup_blobs();

 public:
  // Returns the array class for the n'th dimension
  virtual Klass* array_klass(int n, TRAPS);
  virtual Klass* array_klass_or_null(int n);

  // Returns the array class with this class as element type
  virtual Klass* array_klass(TRAPS);
  virtual Klass* array_klass_or_null();


  // Type testing
  bool is_inline_klass_slow() const        { return true; }

  // Casting from Klass*
  static InlineKlass* cast(Klass* k);

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

  address data_for_oop(oop o) const;

  // Query if this class promises atomicity one way or another
  bool is_atomic() { return is_naturally_atomic() || is_declared_atomic(); }

  bool flatten_array();

  bool contains_oops() const { return nonstatic_oop_map_count() > 0; }
  int nonstatic_oop_count();

  // General store methods
  //
  // Normally loads and store methods would be found in *Oops classes, but since values can be
  // "in-lined" (flattened) into containing oops, these methods reside here in InlineKlass.
  //
  // "inline_copy_*_to_new_*" assume new memory (i.e. IS_DEST_UNINITIALIZED for write barriers)

  void inline_copy_payload_to_new_oop(void* src, oop dst);
  void inline_copy_oop_to_new_oop(oop src, oop dst);
  void inline_copy_oop_to_new_payload(oop src, void* dst);
  void inline_copy_oop_to_payload(oop src, void* dst);

  oop read_inlined_field(oop obj, int offset, TRAPS);
  void write_inlined_field(oop obj, int offset, oop value, TRAPS);

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
  bool is_scalarizable() const;
  bool can_be_passed_as_fields() const;
  bool can_be_returned_as_fields(bool init = false) const;
  void save_oop_fields(const RegisterMap& map, GrowableArray<Handle>& handles) const;
  void restore_oop_results(RegisterMap& map, GrowableArray<Handle>& handles) const;
  oop realloc_result(const RegisterMap& reg_map, const GrowableArray<Handle>& handles, TRAPS);
  static InlineKlass* returned_inline_klass(const RegisterMap& reg_map);

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

  static ByteSize default_value_offset_offset() {
    return byte_offset_of(InlineKlassFixedBlock, _default_value_offset);
  }

  static ByteSize first_field_offset_offset() {
    return byte_offset_of(InlineKlassFixedBlock, _first_field_offset);
  }

  void set_default_value_offset(int offset) {
    *((int*)adr_default_value_offset()) = offset;
  }

  int default_value_offset() {
    int offset = *((int*)adr_default_value_offset());
    assert(offset != 0, "must not be called if not initialized");
    return offset;
  }

  void set_default_value(oop val) {
    java_mirror()->obj_field_put(default_value_offset(), val);
  }

  oop default_value();
  void deallocate_contents(ClassLoaderData* loader_data);
  static void cleanup(InlineKlass* ik) ;

  // Verification
  void verify_on(outputStream* st);
  void oop_verify_on(oop obj, outputStream* st);

};

#endif /* SHARE_VM_OOPS_INLINEKLASS_HPP */
