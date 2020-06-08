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

#ifndef SHARE_VM_OOPS_VALUEKLASS_HPP
#define SHARE_VM_OOPS_VALUEKLASS_HPP

#include "classfile/javaClasses.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/method.hpp"
//#include "oops/oop.inline.hpp"

// A ValueKlass is a specialized InstanceKlass for value types.


class ValueKlass: public InstanceKlass {
  friend class VMStructs;
  friend class InstanceKlass;

 public:
  ValueKlass() { assert(DumpSharedSpaces || UseSharedSpaces, "only for CDS"); }

 private:

  // Constructor
  ValueKlass(const ClassFileParser& parser);

  ValueKlassFixedBlock* valueklass_static_block() const {
    address adr_jf = adr_value_fields_klasses();
    if (adr_jf != NULL) {
      return (ValueKlassFixedBlock*)(adr_jf + this->java_fields_count() * sizeof(Klass*));
    }

    address adr_fing = adr_fingerprint();
    if (adr_fing != NULL) {
      return (ValueKlassFixedBlock*)(adr_fingerprint() + sizeof(u8));
    }

    InstanceKlass** adr_host = adr_unsafe_anonymous_host();
    if (adr_host != NULL) {
      return (ValueKlassFixedBlock*)(adr_host + 1);
    }

    Klass* volatile* adr_impl = adr_implementor();
    if (adr_impl != NULL) {
      return (ValueKlassFixedBlock*)(adr_impl + 1);
    }

    return (ValueKlassFixedBlock*)end_of_nonstatic_oop_maps();
  }

  address adr_extended_sig() const {
    assert(_adr_valueklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_valueklass_fixed_block) + in_bytes(byte_offset_of(ValueKlassFixedBlock, _extended_sig));
  }

  address adr_return_regs() const {
    ValueKlassFixedBlock* vkst = valueklass_static_block();
    return ((address)_adr_valueklass_fixed_block) + in_bytes(byte_offset_of(ValueKlassFixedBlock, _return_regs));
  }

  // pack and unpack handlers for value types return
  address adr_pack_handler() const {
    assert(_adr_valueklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_valueklass_fixed_block) + in_bytes(byte_offset_of(ValueKlassFixedBlock, _pack_handler));
  }

  address adr_pack_handler_jobject() const {
    assert(_adr_valueklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_valueklass_fixed_block) + in_bytes(byte_offset_of(ValueKlassFixedBlock, _pack_handler_jobject));
  }

  address adr_unpack_handler() const {
    assert(_adr_valueklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_valueklass_fixed_block) + in_bytes(byte_offset_of(ValueKlassFixedBlock, _unpack_handler));
  }

  address adr_default_value_offset() const {
    assert(_adr_valueklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_valueklass_fixed_block) + in_bytes(default_value_offset_offset());
  }

  address adr_value_array_klass() const {
    assert(_adr_valueklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_valueklass_fixed_block) + in_bytes(byte_offset_of(ValueKlassFixedBlock, _value_array_klass));
  }

  Klass* get_value_array_klass() const {
    return *(Klass**)adr_value_array_klass();
  }

  Klass* acquire_value_array_klass() const {
    return Atomic::load_acquire((Klass**)adr_value_array_klass());
  }

  Klass* allocate_value_array_klass(TRAPS);

  address adr_alignment() const {
    assert(_adr_valueklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_valueklass_fixed_block) + in_bytes(byte_offset_of(ValueKlassFixedBlock, _alignment));
  }

  address adr_first_field_offset() const {
    assert(_adr_valueklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_valueklass_fixed_block) + in_bytes(byte_offset_of(ValueKlassFixedBlock, _first_field_offset));
  }

  address adr_exact_size_in_bytes() const {
    assert(_adr_valueklass_fixed_block != NULL, "Should have been initialized");
    return ((address)_adr_valueklass_fixed_block) + in_bytes(byte_offset_of(ValueKlassFixedBlock, _exact_size_in_bytes));
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


 protected:
  // Returns the array class for the n'th dimension
  Klass* array_klass_impl(bool or_null, int n, TRAPS);

  // Returns the array class with this class as element type
  Klass* array_klass_impl(bool or_null, TRAPS);

  // Specifically flat array klass
  Klass* value_array_klass(bool or_null, int rank, TRAPS);

 public:
  // Type testing
  bool is_value_slow() const        { return true; }

  // Casting from Klass*
  static ValueKlass* cast(Klass* k);

  // Use this to return the size of an instance in heap words
  // Implementation is currently simple because all value types are allocated
  // in Java heap like Java objects.
  virtual int size_helper() const {
    return layout_helper_to_size_helper(layout_helper());
  }

  // Metadata iterators
  void array_klasses_do(void f(Klass* k));
  void array_klasses_do(void f(Klass* k, TRAPS), TRAPS);

  // allocate_instance() allocates a stand alone value in the Java heap
  // initialized to default value (cleared memory)
  instanceOop allocate_instance(TRAPS);
  // allocates a stand alone value buffer in the Java heap
  // DOES NOT have memory cleared, user MUST initialize payload before
  // returning to Java (i.e.: value_copy)
  instanceOop allocate_instance_buffer(TRAPS);

  // minimum number of bytes occupied by nonstatic fields, HeapWord aligned or pow2
  int raw_value_byte_size();

  address data_for_oop(oop o) const;
  oop oop_for_data(address data) const;

  // Query if this class promises atomicity one way or another
  bool is_atomic() { return is_naturally_atomic() || is_declared_atomic(); }

  bool flatten_array();

  bool contains_oops() const { return nonstatic_oop_map_count() > 0; }
  int nonstatic_oop_count();

  // General store methods
  //
  // Normally loads and store methods would be found in *Oops classes, but since values can be
  // "in-lined" (flattened) into containing oops, these methods reside here in ValueKlass.
  //
  // "value_copy_*_to_new_*" assume new memory (i.e. IS_DEST_UNINITIALIZED for write barriers)

  void value_copy_payload_to_new_oop(void* src, oop dst);
  void value_copy_oop_to_new_oop(oop src, oop dst);
  void value_copy_oop_to_new_payload(oop src, void* dst);
  void value_copy_oop_to_payload(oop src, void* dst);

  oop read_flattened_field(oop obj, int offset, TRAPS);
  void write_flattened_field(oop obj, int offset, oop value, TRAPS);

  // oop iterate raw value type data pointer (where oop_addr may not be an oop, but backing/array-element)
  template <typename T, class OopClosureType>
  inline void oop_iterate_specialized(const address oop_addr, OopClosureType* closure);

  template <typename T, class OopClosureType>
  inline void oop_iterate_specialized_bounded(const address oop_addr, OopClosureType* closure, void* lo, void* hi);

  // calling convention support
  void initialize_calling_convention(TRAPS);
  Array<SigEntry>* extended_sig() const {
    return *((Array<SigEntry>**)adr_extended_sig());
  }
  Array<VMRegPair>* return_regs() const {
    return *((Array<VMRegPair>**)adr_return_regs());
  }
  bool is_scalarizable() const;
  bool can_be_returned_as_fields() const;
  void save_oop_fields(const RegisterMap& map, GrowableArray<Handle>& handles) const;
  void restore_oop_results(RegisterMap& map, GrowableArray<Handle>& handles) const;
  oop realloc_result(const RegisterMap& reg_map, const GrowableArray<Handle>& handles, TRAPS);
  static ValueKlass* returned_value_klass(const RegisterMap& reg_map);

  address pack_handler() const {
    return *(address*)adr_pack_handler();
  }

  address unpack_handler() const {
    return *(address*)adr_unpack_handler();
  }

  // pack and unpack handlers. Need to be loadable from generated code
  // so at a fixed offset from the base of the klass pointer.
  static ByteSize pack_handler_offset() {
    return byte_offset_of(ValueKlassFixedBlock, _pack_handler);
  }

  static ByteSize pack_handler_jobject_offset() {
    return byte_offset_of(ValueKlassFixedBlock, _pack_handler_jobject);
  }

  static ByteSize unpack_handler_offset() {
    return byte_offset_of(ValueKlassFixedBlock, _unpack_handler);
  }

  static ByteSize default_value_offset_offset() {
    return byte_offset_of(ValueKlassFixedBlock, _default_value_offset);
  }

  static ByteSize first_field_offset_offset() {
    return byte_offset_of(ValueKlassFixedBlock, _first_field_offset);
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
  static void cleanup(ValueKlass* ik) ;

  // Verification
  void verify_on(outputStream* st);
  void oop_verify_on(oop obj, outputStream* st);

};

#endif /* SHARE_VM_OOPS_VALUEKLASS_HPP */
