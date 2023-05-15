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
#include "code/codeCache.hpp"
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
#include "oops/flatArrayKlass.hpp"
#include "oops/inlineKlass.inline.hpp"
#include "oops/instanceKlass.inline.hpp"
#include "oops/method.hpp"
#include "oops/oop.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "prims/vectorSupport.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/signature.hpp"
#include "runtime/thread.inline.hpp"
#include "utilities/copy.hpp"

  // Constructor
InlineKlass::InlineKlass(const ClassFileParser& parser)
    : InstanceKlass(parser, InlineKlass::Kind) {
  _adr_inlineklass_fixed_block = inlineklass_static_block();
  // Addresses used for inline type calling convention
  *((Array<SigEntry>**)adr_extended_sig()) = NULL;
  *((Array<VMRegPair>**)adr_return_regs()) = NULL;
  *((address*)adr_pack_handler()) = NULL;
  *((address*)adr_pack_handler_jobject()) = NULL;
  *((address*)adr_unpack_handler()) = NULL;
  assert(pack_handler() == NULL, "pack handler not null");
  *((int*)adr_default_value_offset()) = 0;
  *((address*)adr_value_array_klasses()) = NULL;
  set_prototype_header(markWord::inline_type_prototype());
  assert(is_inline_klass(), "sanity");
  assert(prototype_header().is_inline_type(), "sanity");
}

oop InlineKlass::default_value() {
  assert(is_initialized() || is_being_initialized() || is_in_error_state(), "default value is set at the beginning of initialization");
  oop val = java_mirror()->obj_field_acquire(default_value_offset());
  assert(val != NULL, "Sanity check");
  assert(oopDesc::is_oop(val), "Sanity check");
  assert(val->is_inline_type(), "Sanity check");
  assert(val->klass() == this, "sanity check");
  return val;
}

int InlineKlass::first_field_offset_old() {
#ifdef ASSERT
  int first_offset = INT_MAX;
  for (AllFieldStream fs(this); !fs.done(); fs.next()) {
    if (fs.offset() < first_offset) first_offset= fs.offset();
  }
#endif
  int base_offset = instanceOopDesc::base_offset_in_bytes();
  // The first field of line types is aligned on a long boundary
  base_offset = align_up(base_offset, BytesPerLong);
  assert(base_offset == first_offset, "inconsistent offsets");
  return base_offset;
}

instanceOop InlineKlass::allocate_instance(TRAPS) {
  int size = size_helper();  // Query before forming handle.

  instanceOop oop = (instanceOop)Universe::heap()->obj_allocate(this, size, CHECK_NULL);
  assert(oop->mark().is_inline_type(), "Expected inline type");
  return oop;
}

instanceOop InlineKlass::allocate_instance_buffer(TRAPS) {
  int size = size_helper();  // Query before forming handle.

  instanceOop oop = (instanceOop)Universe::heap()->obj_buffer_allocate(this, size, CHECK_NULL);
  assert(oop->mark().is_inline_type(), "Expected inline type");
  return oop;
}

int InlineKlass::nonstatic_oop_count() {
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

oop InlineKlass::read_inlined_field(oop obj, int offset, TRAPS) {
  oop res = NULL;
  assert(is_initialized() || is_being_initialized()|| is_in_error_state(),
        "Must be initialized, initializing or in a corner case of an escaped instance of a class that failed its initialization");
  if (is_empty_inline_type()) {
    res = (instanceOop)default_value();
  } else {
    Handle obj_h(THREAD, obj);
    res = allocate_instance_buffer(CHECK_NULL);
    inline_copy_payload_to_new_oop(((char*)(oopDesc*)obj_h()) + offset, res);
  }
  assert(res != NULL, "Must be set in one of two paths above");
  return res;
}

void InlineKlass::write_inlined_field(oop obj, int offset, oop value, TRAPS) {
  if (value == NULL) {
    THROW(vmSymbols::java_lang_NullPointerException());
  }
  if (!is_empty_inline_type()) {
    inline_copy_oop_to_payload(value, ((char*)(oopDesc*)obj) + offset);
  }
}

// Arrays of...

bool InlineKlass::flatten_array() {
  if (!UseFlatArray) {
    return false;
  }
  // Too big
  int elem_bytes = get_exact_size_in_bytes();
  if ((FlatArrayElementMaxSize >= 0) && (elem_bytes > FlatArrayElementMaxSize)) {
    return false;
  }
  // Too many embedded oops
  if ((FlatArrayElementMaxOops >= 0) && (nonstatic_oop_count() > FlatArrayElementMaxOops)) {
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

Klass* InlineKlass::value_array_klass(int n, TRAPS) {
  if (Atomic::load_acquire(adr_value_array_klasses()) == NULL) {
    ResourceMark rm(THREAD);
    JavaThread *jt = JavaThread::cast(THREAD);
    {
      // Atomic creation of array_klasses
      MutexLocker ma(THREAD, MultiArray_lock);

      // Check if update has already taken place
      if (value_array_klasses() == NULL) {
        ArrayKlass* k;
        if (flatten_array()) {
          k = FlatArrayKlass::allocate_klass(this, CHECK_NULL);
        } else {
          k = ObjArrayKlass::allocate_objArray_klass(class_loader_data(), 1, this, true, true, CHECK_NULL);

        }
        // use 'release' to pair with lock-free load
        Atomic::release_store(adr_value_array_klasses(), k);
      }
    }
  }
  ArrayKlass* ak = value_array_klasses();
  return ak->array_klass(n, THREAD);
}

Klass* InlineKlass::value_array_klass_or_null(int n) {
  // Need load-acquire for lock-free read
  ArrayKlass* ak = Atomic::load_acquire(adr_value_array_klasses());
  if (ak == NULL) {
    return NULL;
  } else {
    return ak->array_klass_or_null(n);
  }
}

Klass* InlineKlass::value_array_klass(TRAPS) {
  return value_array_klass(1, THREAD);
}

Klass* InlineKlass::value_array_klass_or_null() {
  return value_array_klass_or_null(1);
}

// Inline type arguments are not passed by reference, instead each
// field of the inline type is passed as an argument. This helper
// function collects the inlined field (recursively)
// in a list. Included with the field's type is
// the offset of each field in the inline type: i2c and c2i adapters
// need that to load or store fields. Finally, the list of fields is
// sorted in order of increasing offsets: the adapters and the
// compiled code need to agree upon the order of fields.
//
// The list of basic types that is returned starts with a T_PRIMITIVE_OBJECT
// and ends with an extra T_VOID. T_PRIMITIVE_OBJECT/T_VOID pairs are used as
// delimiters. Every entry between the two is a field of the inline
// type. If there's an embedded inline type in the list, it also starts
// with a T_PRIMITIVE_OBJECT and ends with a T_VOID. This is so we can
// generate a unique fingerprint for the method's adapters and we can
// generate the list of basic types from the interpreter point of view
// (inline types passed as reference: iterate on the list until a
// T_PRIMITIVE_OBJECT, drop everything until and including the closing
// T_VOID) or the compiler point of view (each field of the inline
// types is an argument: drop all T_PRIMITIVE_OBJECT/T_VOID from the list).
int InlineKlass::collect_fields(GrowableArray<SigEntry>* sig, int base_off) {
  int count = 0;
  SigEntry::add_entry(sig, T_PRIMITIVE_OBJECT, name(), base_off);
  for (JavaFieldStream fs(this); !fs.done(); fs.next()) {
    if (fs.access_flags().is_static()) continue;
    if (fs.is_multifield()) continue;
    int offset = base_off + fs.offset() - (base_off > 0 ? first_field_offset() : 0);
    if (fs.is_inlined()) {
      // Resolve klass of inlined field and recursively collect fields
      Klass* vk = get_inline_type_field_klass(fs.index());
      count += InlineKlass::cast(vk)->collect_fields(sig, offset);
    } else {
      BasicType bt = Signature::basic_type(fs.signature());
      if (bt == T_PRIMITIVE_OBJECT) {
        bt = T_OBJECT;
      }
      SigEntry::add_entry(sig, bt, fs.signature(), offset);
      count += type2size[bt];
    }
  }
  int offset = base_off + size_helper()*HeapWordSize - (base_off > 0 ? first_field_offset() : 0);
  SigEntry::add_entry(sig, T_VOID, name(), offset);
  if (base_off == 0) {
    sig->sort(SigEntry::compare);
  }
  assert(sig->at(0)._bt == T_PRIMITIVE_OBJECT && sig->at(sig->length()-1)._bt == T_VOID, "broken structure");
  return count;
}

void InlineKlass::initialize_calling_convention(TRAPS) {
  // Because the pack and unpack handler addresses need to be loadable from generated code,
  // they are stored at a fixed offset in the klass metadata. Since inline type klasses do
  // not have a vtable, the vtable offset is used to store these addresses.
  if (InlineTypeReturnedAsFields || InlineTypePassFieldsAsArgs) {
    ResourceMark rm;
    GrowableArray<SigEntry> sig_vk;
    int nb_fields = collect_fields(&sig_vk);
    Array<SigEntry>* extended_sig = MetadataFactory::new_array<SigEntry>(class_loader_data(), sig_vk.length(), CHECK);
    *((Array<SigEntry>**)adr_extended_sig()) = extended_sig;
    for (int i = 0; i < sig_vk.length(); i++) {
      extended_sig->at_put(i, sig_vk.at(i));
    }
    if (can_be_returned_as_fields(/* init= */ true)) {
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

        BufferedInlineTypeBlob* buffered_blob = SharedRuntime::generate_buffered_inline_type_adapter(this);
        *((address*)adr_pack_handler()) = buffered_blob->pack_fields();
        *((address*)adr_pack_handler_jobject()) = buffered_blob->pack_fields_jobject();
        *((address*)adr_unpack_handler()) = buffered_blob->unpack_fields();
        assert(CodeCache::find_blob(pack_handler()) == buffered_blob, "lost track of blob");
        assert(can_be_returned_as_fields(), "sanity");
      }
    }
    if (!can_be_returned_as_fields() && !can_be_passed_as_fields()) {
      MetadataFactory::free_array<SigEntry>(class_loader_data(), extended_sig);
      assert(return_regs() == NULL, "sanity");
    }
  }
}

void InlineKlass::deallocate_contents(ClassLoaderData* loader_data) {
  if (extended_sig() != NULL) {
    MetadataFactory::free_array<SigEntry>(loader_data, extended_sig());
  }
  if (return_regs() != NULL) {
    MetadataFactory::free_array<VMRegPair>(loader_data, return_regs());
  }
  cleanup_blobs();
  InstanceKlass::deallocate_contents(loader_data);
}

void InlineKlass::cleanup(InlineKlass* ik) {
  ik->cleanup_blobs();
}

void InlineKlass::cleanup_blobs() {
  if (pack_handler() != NULL) {
    CodeBlob* buffered_blob = CodeCache::find_blob(pack_handler());
    assert(buffered_blob->is_buffered_inline_type_blob(), "bad blob type");
    BufferBlob::free((BufferBlob*)buffered_blob);
    *((address*)adr_pack_handler()) = NULL;
    *((address*)adr_pack_handler_jobject()) = NULL;
    *((address*)adr_unpack_handler()) = NULL;
  }
}

// Can this inline type be passed as multiple values?
bool InlineKlass::can_be_passed_as_fields() const {
  return !VectorSupport::skip_value_scalarization(const_cast<InlineKlass*>(this)) && InlineTypePassFieldsAsArgs;
}

// Can this inline type be returned as multiple values?
bool InlineKlass::can_be_returned_as_fields(bool init) const {
  return !VectorSupport::skip_value_scalarization(const_cast<InlineKlass*>(this)) && InlineTypeReturnedAsFields && (init || return_regs() != NULL);
}

// Create handles for all oop fields returned in registers that are going to be live across a safepoint
void InlineKlass::save_oop_fields(const RegisterMap& reg_map, GrowableArray<Handle>& handles) const {
  Thread* thread = Thread::current();
  const Array<SigEntry>* sig_vk = extended_sig();
  const Array<VMRegPair>* regs = return_regs();
  int j = 1;

  for (int i = 0; i < sig_vk->length(); i++) {
    BasicType bt = sig_vk->at(i)._bt;
    if (bt == T_OBJECT || bt == T_ARRAY) {
      VMRegPair pair = regs->at(j);
      address loc = reg_map.location(pair.first(), nullptr);
      oop v = *(oop*)loc;
      assert(v == NULL || oopDesc::is_oop(v), "not an oop?");
      assert(Universe::heap()->is_in_or_null(v), "must be heap pointer");
      handles.push(Handle(thread, v));
    }
    if (bt == T_PRIMITIVE_OBJECT) {
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
void InlineKlass::restore_oop_results(RegisterMap& reg_map, GrowableArray<Handle>& handles) const {
  assert(InlineTypeReturnedAsFields, "Inline types should never be returned as fields");
  const Array<SigEntry>* sig_vk = extended_sig();
  const Array<VMRegPair>* regs = return_regs();
  assert(regs != NULL, "inconsistent");

  int j = 1;
  for (int i = 0, k = 0; i < sig_vk->length(); i++) {
    BasicType bt = sig_vk->at(i)._bt;
    if (bt == T_OBJECT || bt == T_ARRAY) {
      VMRegPair pair = regs->at(j);
      address loc = reg_map.location(pair.first(), nullptr);
      *(oop*)loc = handles.at(k++)();
    }
    if (bt == T_PRIMITIVE_OBJECT) {
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

// Fields are in registers. Create an instance of the inline type and
// initialize it with the values of the fields.
oop InlineKlass::realloc_result(const RegisterMap& reg_map, const GrowableArray<Handle>& handles, TRAPS) {
  oop new_vt = allocate_instance(CHECK_NULL);
  const Array<SigEntry>* sig_vk = extended_sig();
  const Array<VMRegPair>* regs = return_regs();

  int j = 1;
  int k = 0;
  for (int i = 0; i < sig_vk->length(); i++) {
    BasicType bt = sig_vk->at(i)._bt;
    if (bt == T_PRIMITIVE_OBJECT) {
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
    address loc = reg_map.location(pair.first(), nullptr);
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

// Check the return register for an InlineKlass oop
InlineKlass* InlineKlass::returned_inline_klass(const RegisterMap& map) {
  BasicType bt = T_METADATA;
  VMRegPair pair;
  int nb = SharedRuntime::java_return_convention(&bt, &pair, 1);
  assert(nb == 1, "broken");

  address loc = map.location(pair.first(), nullptr);
  intptr_t ptr = *(intptr_t*)loc;
  if (is_set_nth_bit(ptr, 0)) {
    // Oop is tagged, must be an InlineKlass oop
    clear_nth_bit(ptr, 0);
    assert(Metaspace::contains((void*)ptr), "should be klass");
    InlineKlass* vk = (InlineKlass*)ptr;
    assert(vk->can_be_returned_as_fields(), "must be able to return as fields");
    return vk;
  }
#ifdef ASSERT
  // Oop is not tagged, must be a valid oop
  if (VerifyOops) {
    oopDesc::verify(cast_to_oop(ptr));
  }
#endif
  return NULL;
}

// CDS support

void InlineKlass::metaspace_pointers_do(MetaspaceClosure* it) {
  InstanceKlass::metaspace_pointers_do(it);

  InlineKlass* this_ptr = this;
  it->push_internal_pointer(&this_ptr, (intptr_t*)&_adr_inlineklass_fixed_block);
  it->push((Klass**)adr_value_array_klasses());
}

void InlineKlass::remove_unshareable_info() {
  InstanceKlass::remove_unshareable_info();

  *((Array<SigEntry>**)adr_extended_sig()) = NULL;
  *((Array<VMRegPair>**)adr_return_regs()) = NULL;
  *((address*)adr_pack_handler()) = NULL;
  *((address*)adr_pack_handler_jobject()) = NULL;
  *((address*)adr_unpack_handler()) = NULL;
  assert(pack_handler() == NULL, "pack handler not null");
  if (value_array_klasses() != NULL) {
    value_array_klasses()->remove_unshareable_info();
  }
}

void InlineKlass::remove_java_mirror() {
  InstanceKlass::remove_java_mirror();
  if (value_array_klasses() != NULL) {
    value_array_klasses()->remove_java_mirror();
  }
}

void InlineKlass::restore_unshareable_info(ClassLoaderData* loader_data, Handle protection_domain, PackageEntry* pkg_entry, TRAPS) {
  InstanceKlass::restore_unshareable_info(loader_data, protection_domain, pkg_entry, CHECK);
  if (value_array_klasses() != NULL) {
    value_array_klasses()->restore_unshareable_info(ClassLoaderData::the_null_class_loader_data(), Handle(), CHECK);
  }
}

// oop verify

void InlineKlass::verify_on(outputStream* st) {
  InstanceKlass::verify_on(st);
  guarantee(prototype_header().is_inline_type(), "Prototype header is not inline type");
}

void InlineKlass::oop_verify_on(oop obj, outputStream* st) {
  InstanceKlass::oop_verify_on(obj, st);
  guarantee(obj->mark().is_inline_type(), "Header is not inline type");
}
