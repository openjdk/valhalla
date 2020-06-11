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

#include "precompiled.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "gc/shared/gcLocker.inline.hpp"
#include "interpreter/interpreter.hpp"
#include "logging/log.hpp"
#include "memory/metaspaceClosure.hpp"
#include "memory/metadataFactory.hpp"
#include "oops/access.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/fieldStreams.inline.hpp"
#include "oops/instanceKlass.inline.hpp"
#include "oops/method.hpp"
#include "oops/oop.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/valueKlass.inline.hpp"
#include "oops/valueArrayKlass.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/signature.hpp"
#include "runtime/thread.inline.hpp"
#include "utilities/copy.hpp"

  // Constructor
ValueKlass::ValueKlass(const ClassFileParser& parser)
    : InstanceKlass(parser, InstanceKlass::_kind_inline_type, InstanceKlass::ID) {
  _adr_valueklass_fixed_block = valueklass_static_block();
  // Addresses used for value type calling convention
  *((Array<SigEntry>**)adr_extended_sig()) = NULL;
  *((Array<VMRegPair>**)adr_return_regs()) = NULL;
  *((address*)adr_pack_handler()) = NULL;
  *((address*)adr_pack_handler_jobject()) = NULL;
  *((address*)adr_unpack_handler()) = NULL;
  assert(pack_handler() == NULL, "pack handler not null");
  *((int*)adr_default_value_offset()) = 0;
  *((Klass**)adr_value_array_klass()) = NULL;
  set_prototype_header(markWord::always_locked_prototype());
  assert(is_inline_type_klass(), "invariant");
}

oop ValueKlass::default_value() {
  oop val = java_mirror()->obj_field_acquire(default_value_offset());
  assert(oopDesc::is_oop(val), "Sanity check");
  assert(val->is_value(), "Sanity check");
  assert(val->klass() == this, "sanity check");
  return val;
}

int ValueKlass::first_field_offset_old() {
#ifdef ASSERT
  int first_offset = INT_MAX;
  for (AllFieldStream fs(this); !fs.done(); fs.next()) {
    if (fs.offset() < first_offset) first_offset= fs.offset();
  }
#endif
  int base_offset = instanceOopDesc::base_offset_in_bytes();
  // The first field of value types is aligned on a long boundary
  base_offset = align_up(base_offset, BytesPerLong);
  assert(base_offset == first_offset, "inconsistent offsets");
  return base_offset;
}

int ValueKlass::raw_value_byte_size() {
  int heapOopAlignedSize = nonstatic_field_size() << LogBytesPerHeapOop;
  // If bigger than 64 bits or needs oop alignment, then use jlong aligned
  // which for values should be jlong aligned, asserts in raw_field_copy otherwise
  if (heapOopAlignedSize >= longSize || contains_oops()) {
    return heapOopAlignedSize;
  }
  // Small primitives...
  // If a few small basic type fields, return the actual size, i.e.
  // 1 byte = 1
  // 2 byte = 2
  // 3 byte = 4, because pow2 needed for element stores
  int first_offset = first_field_offset();
  int last_offset  = 0; // find the last offset, add basic type size
  int last_tsz     = 0;
  for (AllFieldStream fs(this); !fs.done(); fs.next()) {
    if (fs.access_flags().is_static()) {
      continue;
    } else if (fs.offset() > last_offset) {
      BasicType type = Signature::basic_type(fs.signature());
      if (is_java_primitive(type)) {
        last_tsz = type2aelembytes(type);
      } else if (type == T_VALUETYPE) {
        // Not just primitives. Layout aligns embedded value, so use jlong aligned it is
        return heapOopAlignedSize;
      } else {
        guarantee(0, "Unknown type %d", type);
      }
      assert(last_tsz != 0, "Invariant");
      last_offset = fs.offset();
    }
  }
  // Assumes VT with no fields are meaningless and illegal
  last_offset += last_tsz;
  assert(last_offset > first_offset && last_tsz, "Invariant");
  return 1 << upper_log2(last_offset - first_offset);
}

instanceOop ValueKlass::allocate_instance(TRAPS) {
  int size = size_helper();  // Query before forming handle.

  instanceOop oop = (instanceOop)Universe::heap()->obj_allocate(this, size, CHECK_NULL);
  assert(oop->mark().is_always_locked(), "Unlocked value type");
  return oop;
}

instanceOop ValueKlass::allocate_instance_buffer(TRAPS) {
  int size = size_helper();  // Query before forming handle.

  instanceOop oop = (instanceOop)Universe::heap()->obj_buffer_allocate(this, size, CHECK_NULL);
  assert(oop->mark().is_always_locked(), "Unlocked value type");
  return oop;
}

int ValueKlass::nonstatic_oop_count() {
  int oops = 0;
  int map_count = nonstatic_oop_map_count();
  OopMapBlock* block = start_of_nonstatic_oop_maps();
  OopMapBlock* end = block + map_count;
  while (block != end) {
    oops += block->count();
    block++;
  }
  return oops;
}

oop ValueKlass::read_field_allocated_inline(oop obj, int offset, TRAPS) {
  oop res = NULL;
  this->initialize(CHECK_NULL); // will throw an exception if in error state
  if (is_empty_inline_type()) {
    res = (instanceOop)default_value();
  } else {
    Handle obj_h(THREAD, obj);
    res = allocate_instance_buffer(CHECK_NULL);
    value_copy_payload_to_new_oop(((char*)(oopDesc*)obj_h()) + offset, res);
  }
  assert(res != NULL, "Must be set in one of two paths above");
  return res;
}

void ValueKlass::write_field_allocated_inline(oop obj, int offset, oop value, TRAPS) {
  if (value == NULL) {
    THROW(vmSymbols::java_lang_NullPointerException());
  }
  if (!is_empty_inline_type()) {
    value_copy_oop_to_payload(value, ((char*)(oopDesc*)obj) + offset);
  }
}

// Arrays of...

bool ValueKlass::flatten_array() {
  if (!ValueArrayFlatten) {
    return false;
  }
  // Too big
  int elem_bytes = raw_value_byte_size();
  if ((InlineArrayElemMaxFlatSize >= 0) && (elem_bytes > InlineArrayElemMaxFlatSize)) {
    return false;
  }
  // Too many embedded oops
  if ((InlineArrayElemMaxFlatOops >= 0) && (nonstatic_oop_count() > InlineArrayElemMaxFlatOops)) {
    return false;
  }
  // Declared atomic but not naturally atomic.
  if (is_declared_atomic() && !is_naturally_atomic()) {
    return false;
  }
  // VM enforcing InlineArrayAtomicAccess only...
  if (InlineArrayAtomicAccess && (!is_naturally_atomic())) {
    return false;
  }
  return true;
}

void ValueKlass::remove_unshareable_info() {
  InstanceKlass::remove_unshareable_info();

  *((Array<SigEntry>**)adr_extended_sig()) = NULL;
  *((Array<VMRegPair>**)adr_return_regs()) = NULL;
  *((address*)adr_pack_handler()) = NULL;
  *((address*)adr_pack_handler_jobject()) = NULL;
  *((address*)adr_unpack_handler()) = NULL;
  assert(pack_handler() == NULL, "pack handler not null");
  *((Klass**)adr_value_array_klass()) = NULL;
}

void ValueKlass::restore_unshareable_info(ClassLoaderData* loader_data, Handle protection_domain, PackageEntry* pkg_entry, TRAPS) {
  InstanceKlass::restore_unshareable_info(loader_data, protection_domain, pkg_entry, CHECK);
  oop val = allocate_instance(CHECK);
  set_default_value(val);
}


Klass* ValueKlass::array_klass_impl(bool or_null, int n, TRAPS) {
  if (flatten_array()) {
    return value_array_klass(or_null, n, THREAD);
  } else {
    return InstanceKlass::array_klass_impl(or_null, n, THREAD);
  }
}

Klass* ValueKlass::array_klass_impl(bool or_null, TRAPS) {
  return array_klass_impl(or_null, 1, THREAD);
}

Klass* ValueKlass::value_array_klass(bool or_null, int rank, TRAPS) {
  Klass* vak = acquire_value_array_klass();
  if (vak == NULL) {
    if (or_null) return NULL;
    ResourceMark rm;
    {
      // Atomic creation of array_klasses
      MutexLocker ma(THREAD, MultiArray_lock);
      if (get_value_array_klass() == NULL) {
        vak = allocate_value_array_klass(CHECK_NULL);
        Atomic::release_store((Klass**)adr_value_array_klass(), vak);
      }
    }
  }
  if (or_null) {
    return vak->array_klass_or_null(rank);
  }
  return vak->array_klass(rank, THREAD);
}

Klass* ValueKlass::allocate_value_array_klass(TRAPS) {
  if (flatten_array()) {
    return ValueArrayKlass::allocate_klass(this, THREAD);
  }
  return ObjArrayKlass::allocate_objArray_klass(class_loader_data(), 1, this, THREAD);
}

void ValueKlass::array_klasses_do(void f(Klass* k, TRAPS), TRAPS) {
  InstanceKlass::array_klasses_do(f, THREAD);
  if (get_value_array_klass() != NULL)
    ArrayKlass::cast(get_value_array_klass())->array_klasses_do(f, THREAD);
}

void ValueKlass::array_klasses_do(void f(Klass* k)) {
  InstanceKlass::array_klasses_do(f);
  if (get_value_array_klass() != NULL)
    ArrayKlass::cast(get_value_array_klass())->array_klasses_do(f);
}

// Value type arguments are not passed by reference, instead each
// field of the value type is passed as an argument. This helper
// function collects the field allocated inline (recursively)
// in a list. Included with the field's type is
// the offset of each field in the inline type: i2c and c2i adapters
// need that to load or store fields. Finally, the list of fields is
// sorted in order of increasing offsets: the adapters and the
// compiled code need to agree upon the order of fields.
//
// The list of basic types that is returned starts with a T_VALUETYPE
// and ends with an extra T_VOID. T_VALUETYPE/T_VOID pairs are used as
// delimiters. Every entry between the two is a field of the value
// type. If there's an embedded inline type in the list, it also starts
// with a T_VALUETYPE and ends with a T_VOID. This is so we can
// generate a unique fingerprint for the method's adapters and we can
// generate the list of basic types from the interpreter point of view
// (value types passed as reference: iterate on the list until a
// T_VALUETYPE, drop everything until and including the closing
// T_VOID) or the compiler point of view (each field of the value
// types is an argument: drop all T_VALUETYPE/T_VOID from the list).
int ValueKlass::collect_fields(GrowableArray<SigEntry>* sig, int base_off) {
  int count = 0;
  SigEntry::add_entry(sig, T_VALUETYPE, base_off);
  for (AllFieldStream fs(this); !fs.done(); fs.next()) {
    if (fs.access_flags().is_static()) continue;
    int offset = base_off + fs.offset() - (base_off > 0 ? first_field_offset() : 0);
    if (fs.is_allocated_inline()) {
      // Resolve klass of field allocated inline and recursively collect fields
      Klass* vk = get_value_field_klass(fs.index());
      count += ValueKlass::cast(vk)->collect_fields(sig, offset);
    } else {
      BasicType bt = Signature::basic_type(fs.signature());
      if (bt == T_VALUETYPE) {
        bt = T_OBJECT;
      }
      SigEntry::add_entry(sig, bt, offset);
      count += type2size[bt];
    }
  }
  int offset = base_off + size_helper()*HeapWordSize - (base_off > 0 ? first_field_offset() : 0);
  SigEntry::add_entry(sig, T_VOID, offset);
  if (base_off == 0) {
    sig->sort(SigEntry::compare);
  }
  assert(sig->at(0)._bt == T_VALUETYPE && sig->at(sig->length()-1)._bt == T_VOID, "broken structure");
  return count;
}

void ValueKlass::initialize_calling_convention(TRAPS) {
  // Because the pack and unpack handler addresses need to be loadable from generated code,
  // they are stored at a fixed offset in the klass metadata. Since value type klasses do
  // not have a vtable, the vtable offset is used to store these addresses.
  if (is_scalarizable() && (InlineTypeReturnedAsFields || InlineTypePassFieldsAsArgs)) {
    ResourceMark rm;
    GrowableArray<SigEntry> sig_vk;
    int nb_fields = collect_fields(&sig_vk);
    Array<SigEntry>* extended_sig = MetadataFactory::new_array<SigEntry>(class_loader_data(), sig_vk.length(), CHECK);
    *((Array<SigEntry>**)adr_extended_sig()) = extended_sig;
    for (int i = 0; i < sig_vk.length(); i++) {
      extended_sig->at_put(i, sig_vk.at(i));
    }

    if (InlineTypeReturnedAsFields) {
      nb_fields++;
      BasicType* sig_bt = NEW_RESOURCE_ARRAY(BasicType, nb_fields);
      sig_bt[0] = T_METADATA;
      SigEntry::fill_sig_bt(&sig_vk, sig_bt+1);
      VMRegPair* regs = NEW_RESOURCE_ARRAY(VMRegPair, nb_fields);
      int total = SharedRuntime::java_return_convention(sig_bt, regs, nb_fields);

      if (total > 0) {
        Array<VMRegPair>* return_regs = MetadataFactory::new_array<VMRegPair>(class_loader_data(), nb_fields, CHECK);
        *((Array<VMRegPair>**)adr_return_regs()) = return_regs;
        for (int i = 0; i < nb_fields; i++) {
          return_regs->at_put(i, regs[i]);
        }

        BufferedValueTypeBlob* buffered_blob = SharedRuntime::generate_buffered_value_type_adapter(this);
        *((address*)adr_pack_handler()) = buffered_blob->pack_fields();
        *((address*)adr_pack_handler_jobject()) = buffered_blob->pack_fields_jobject();
        *((address*)adr_unpack_handler()) = buffered_blob->unpack_fields();
        assert(CodeCache::find_blob(pack_handler()) == buffered_blob, "lost track of blob");
      }
    }
  }
}

void ValueKlass::deallocate_contents(ClassLoaderData* loader_data) {
  if (extended_sig() != NULL) {
    MetadataFactory::free_array<SigEntry>(loader_data, extended_sig());
  }
  if (return_regs() != NULL) {
    MetadataFactory::free_array<VMRegPair>(loader_data, return_regs());
  }
  cleanup_blobs();
  InstanceKlass::deallocate_contents(loader_data);
}

void ValueKlass::cleanup(ValueKlass* ik) {
  ik->cleanup_blobs();
}

void ValueKlass::cleanup_blobs() {
  if (pack_handler() != NULL) {
    CodeBlob* buffered_blob = CodeCache::find_blob(pack_handler());
    assert(buffered_blob->is_buffered_value_type_blob(), "bad blob type");
    BufferBlob::free((BufferBlob*)buffered_blob);
    *((address*)adr_pack_handler()) = NULL;
    *((address*)adr_pack_handler_jobject()) = NULL;
    *((address*)adr_unpack_handler()) = NULL;
  }
}

// Can this inline type be scalarized?
bool ValueKlass::is_scalarizable() const {
  return ScalarizeInlineTypes;
}

// Can this value type be returned as multiple values?
bool ValueKlass::can_be_returned_as_fields() const {
  return return_regs() != NULL;
}

// Create handles for all oop fields returned in registers that are going to be live across a safepoint
void ValueKlass::save_oop_fields(const RegisterMap& reg_map, GrowableArray<Handle>& handles) const {
  Thread* thread = Thread::current();
  const Array<SigEntry>* sig_vk = extended_sig();
  const Array<VMRegPair>* regs = return_regs();
  int j = 1;

  for (int i = 0; i < sig_vk->length(); i++) {
    BasicType bt = sig_vk->at(i)._bt;
    if (bt == T_OBJECT || bt == T_ARRAY) {
      VMRegPair pair = regs->at(j);
      address loc = reg_map.location(pair.first());
      oop v = *(oop*)loc;
      assert(v == NULL || oopDesc::is_oop(v), "not an oop?");
      assert(Universe::heap()->is_in_or_null(v), "must be heap pointer");
      handles.push(Handle(thread, v));
    }
    if (bt == T_VALUETYPE) {
      continue;
    }
    if (bt == T_VOID &&
        sig_vk->at(i-1)._bt != T_LONG &&
        sig_vk->at(i-1)._bt != T_DOUBLE) {
      continue;
    }
    j++;
  }
  assert(j == regs->length(), "missed a field?");
}

// Update oop fields in registers from handles after a safepoint
void ValueKlass::restore_oop_results(RegisterMap& reg_map, GrowableArray<Handle>& handles) const {
  assert(InlineTypeReturnedAsFields, "inconsistent");
  const Array<SigEntry>* sig_vk = extended_sig();
  const Array<VMRegPair>* regs = return_regs();
  assert(regs != NULL, "inconsistent");

  int j = 1;
  for (int i = 0, k = 0; i < sig_vk->length(); i++) {
    BasicType bt = sig_vk->at(i)._bt;
    if (bt == T_OBJECT || bt == T_ARRAY) {
      VMRegPair pair = regs->at(j);
      address loc = reg_map.location(pair.first());
      *(oop*)loc = handles.at(k++)();
    }
    if (bt == T_VALUETYPE) {
      continue;
    }
    if (bt == T_VOID &&
        sig_vk->at(i-1)._bt != T_LONG &&
        sig_vk->at(i-1)._bt != T_DOUBLE) {
      continue;
    }
    j++;
  }
  assert(j == regs->length(), "missed a field?");
}

// Fields are in registers. Create an instance of the value type and
// initialize it with the values of the fields.
oop ValueKlass::realloc_result(const RegisterMap& reg_map, const GrowableArray<Handle>& handles, TRAPS) {
  oop new_vt = allocate_instance(CHECK_NULL);
  const Array<SigEntry>* sig_vk = extended_sig();
  const Array<VMRegPair>* regs = return_regs();

  int j = 1;
  int k = 0;
  for (int i = 0; i < sig_vk->length(); i++) {
    BasicType bt = sig_vk->at(i)._bt;
    if (bt == T_VALUETYPE) {
      continue;
    }
    if (bt == T_VOID) {
      if (sig_vk->at(i-1)._bt == T_LONG ||
          sig_vk->at(i-1)._bt == T_DOUBLE) {
        j++;
      }
      continue;
    }
    int off = sig_vk->at(i)._offset;
    assert(off > 0, "offset in object should be positive");
    VMRegPair pair = regs->at(j);
    address loc = reg_map.location(pair.first());
    switch(bt) {
    case T_BOOLEAN: {
      new_vt->bool_field_put(off, *(jboolean*)loc);
      break;
    }
    case T_CHAR: {
      new_vt->char_field_put(off, *(jchar*)loc);
      break;
    }
    case T_BYTE: {
      new_vt->byte_field_put(off, *(jbyte*)loc);
      break;
    }
    case T_SHORT: {
      new_vt->short_field_put(off, *(jshort*)loc);
      break;
    }
    case T_INT: {
      new_vt->int_field_put(off, *(jint*)loc);
      break;
    }
    case T_LONG: {
#ifdef _LP64
      new_vt->double_field_put(off,  *(jdouble*)loc);
#else
      Unimplemented();
#endif
      break;
    }
    case T_OBJECT:
    case T_ARRAY: {
      Handle handle = handles.at(k++);
      new_vt->obj_field_put(off, handle());
      break;
    }
    case T_FLOAT: {
      new_vt->float_field_put(off,  *(jfloat*)loc);
      break;
    }
    case T_DOUBLE: {
      new_vt->double_field_put(off, *(jdouble*)loc);
      break;
    }
    default:
      ShouldNotReachHere();
    }
    *(intptr_t*)loc = 0xDEAD;
    j++;
  }
  assert(j == regs->length(), "missed a field?");
  assert(k == handles.length(), "missed an oop?");
  return new_vt;
}

// Check the return register for a ValueKlass oop
ValueKlass* ValueKlass::returned_value_klass(const RegisterMap& map) {
  BasicType bt = T_METADATA;
  VMRegPair pair;
  int nb = SharedRuntime::java_return_convention(&bt, &pair, 1);
  assert(nb == 1, "broken");

  address loc = map.location(pair.first());
  intptr_t ptr = *(intptr_t*)loc;
  if (is_set_nth_bit(ptr, 0)) {
    // Oop is tagged, must be a ValueKlass oop
    clear_nth_bit(ptr, 0);
    assert(Metaspace::contains((void*)ptr), "should be klass");
    ValueKlass* vk = (ValueKlass*)ptr;
    assert(vk->can_be_returned_as_fields(), "must be able to return as fields");
    return vk;
  }
#ifdef ASSERT
  // Oop is not tagged, must be a valid oop
  if (VerifyOops) {
    oopDesc::verify(oop((HeapWord*)ptr));
  }
#endif
  return NULL;
}

void ValueKlass::verify_on(outputStream* st) {
  InstanceKlass::verify_on(st);
  guarantee(prototype_header().is_always_locked(), "Prototype header is not always locked");
}

void ValueKlass::oop_verify_on(oop obj, outputStream* st) {
  InstanceKlass::oop_verify_on(obj, st);
  guarantee(obj->mark().is_always_locked(), "Header is not always locked");
}

void ValueKlass::metaspace_pointers_do(MetaspaceClosure* it) {
  InstanceKlass::metaspace_pointers_do(it);

  ValueKlass* this_ptr = this;
  it->push_internal_pointer(&this_ptr, (intptr_t*)&_adr_valueklass_fixed_block);
}
