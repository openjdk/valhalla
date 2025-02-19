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

#include "cds/archiveUtils.hpp"
#include "cds/cdsConfig.hpp"
#include "classfile/vmSymbols.hpp"
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
  set_prototype_header(markWord::inline_type_prototype());
  assert(is_inline_klass(), "sanity");
  assert(prototype_header().is_inline_type(), "sanity");
}

InlineKlass::InlineKlass() {
  assert(CDSConfig::is_dumping_archive() || UseSharedSpaces, "only for CDS");
}

void InlineKlass::init_fixed_block() {
  _adr_inlineklass_fixed_block = inlineklass_static_block();
  // Addresses used for inline type calling convention
  *((Array<SigEntry>**)adr_extended_sig()) = nullptr;
  *((Array<VMRegPair>**)adr_return_regs()) = nullptr;
  *((address*)adr_pack_handler()) = nullptr;
  *((address*)adr_pack_handler_jobject()) = nullptr;
  *((address*)adr_unpack_handler()) = nullptr;
  assert(pack_handler() == nullptr, "pack handler not null");
  *((address*)adr_non_atomic_flat_array_klass()) = nullptr;
  *((address*)adr_atomic_flat_array_klass()) = nullptr;
  *((address*)adr_nullable_atomic_flat_array_klass()) = nullptr;
  *((address*)adr_null_free_reference_array_klass()) = nullptr;
  set_default_value_offset(0);
  set_null_reset_value_offset(0);
  set_payload_offset(-1);
  set_payload_size_in_bytes(-1);
  set_payload_alignment(-1);
  set_non_atomic_size_in_bytes(-1);
  set_non_atomic_alignment(-1);
  set_atomic_size_in_bytes(-1);
  set_nullable_size_in_bytes(-1);
  set_null_marker_offset(-1);
}

void InlineKlass::set_default_value(oop val) {
  assert(val != nullptr, "Sanity check");
  assert(oopDesc::is_oop(val), "Sanity check");
  assert(val->is_inline_type(), "Sanity check");
  assert(val->klass() == this, "sanity check");
  java_mirror()->obj_field_put(default_value_offset(), val);
}

void InlineKlass::set_null_reset_value(oop val) {
  assert(val != nullptr, "Sanity check");
  assert(oopDesc::is_oop(val), "Sanity check");
  assert(val->is_inline_type(), "Sanity check");
  assert(val->klass() == this, "sanity check");
  java_mirror()->obj_field_put(null_reset_value_offset(), val);
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

int InlineKlass::layout_size_in_bytes(LayoutKind kind) const {
  switch(kind) {
    case LayoutKind::NON_ATOMIC_FLAT:
      assert(has_non_atomic_layout(), "Layout not available");
      return non_atomic_size_in_bytes();
      break;
    case LayoutKind::ATOMIC_FLAT:
      assert(has_atomic_layout(), "Layout not available");
      return atomic_size_in_bytes();
      break;
    case LayoutKind::NULLABLE_ATOMIC_FLAT:
      assert(has_nullable_atomic_layout(), "Layout not available");
      return nullable_atomic_size_in_bytes();
      break;
    case BUFFERED:
      return payload_size_in_bytes();
      break;
    default:
      ShouldNotReachHere();
  }
}

int InlineKlass::layout_alignment(LayoutKind kind) const {
  switch(kind) {
    case LayoutKind::NON_ATOMIC_FLAT:
      assert(has_non_atomic_layout(), "Layout not available");
      return non_atomic_alignment();
      break;
    case LayoutKind::ATOMIC_FLAT:
      assert(has_atomic_layout(), "Layout not available");
      return atomic_size_in_bytes();
      break;
    case LayoutKind::NULLABLE_ATOMIC_FLAT:
      assert(has_nullable_atomic_layout(), "Layout not available");
      return nullable_atomic_size_in_bytes();
      break;
    case LayoutKind::BUFFERED:
      return payload_alignment();
      break;
    default:
      ShouldNotReachHere();
  }
}

bool InlineKlass::is_layout_supported(LayoutKind lk) {
  switch(lk) {
    case LayoutKind::NON_ATOMIC_FLAT:
      return has_non_atomic_layout();
      break;
    case LayoutKind::ATOMIC_FLAT:
      return has_atomic_layout();
      break;
    case LayoutKind::NULLABLE_ATOMIC_FLAT:
      return has_nullable_atomic_layout();
      break;
    case LayoutKind::BUFFERED:
      return true;
      break;
    default:
      ShouldNotReachHere();
  }
}

void InlineKlass::copy_payload_to_addr(void* src, void* dst, LayoutKind lk, bool dest_is_initialized) {
  assert(is_layout_supported(lk), "Unsupported layout");
  assert(lk != LayoutKind::REFERENCE && lk != LayoutKind::UNKNOWN, "Sanity check");
  switch(lk) {
    case NULLABLE_ATOMIC_FLAT: {
    if (is_payload_marked_as_null((address)src)) {
        if (!contains_oops()) {
          mark_payload_as_null((address)dst);
          return;
        }
        // copy null_reset value to dest
        if (dest_is_initialized) {
          HeapAccess<>::value_copy(payload_addr(null_reset_value()), dst, this, lk);
        } else {
          HeapAccess<IS_DEST_UNINITIALIZED>::value_copy(payload_addr(null_reset_value()), dst, this, lk);
        }
      } else {
        // Copy has to be performed, even if this is an empty value, because of the null marker
        mark_payload_as_non_null((address)src);
        if (dest_is_initialized) {
          HeapAccess<>::value_copy(src, dst, this, lk);
        } else {
          HeapAccess<IS_DEST_UNINITIALIZED>::value_copy(src, dst, this, lk);
        }
      }
    }
    break;
    case BUFFERED:
    case ATOMIC_FLAT:
    case NON_ATOMIC_FLAT: {
      if (is_empty_inline_type()) return; // nothing to do
      if (dest_is_initialized) {
        HeapAccess<>::value_copy(src, dst, this, lk);
      } else {
        HeapAccess<IS_DEST_UNINITIALIZED>::value_copy(src, dst, this, lk);
      }
    }
    break;
    default:
      ShouldNotReachHere();
  }
}

oop InlineKlass::read_payload_from_addr(oop src, int offset, LayoutKind lk, TRAPS) {
  assert(src != nullptr, "Must be");
  assert(is_layout_supported(lk), "Unsupported layout");
  switch(lk) {
    case NULLABLE_ATOMIC_FLAT: {
      if (is_payload_marked_as_null((address)((char*)(oopDesc*)src + offset))) {
        return nullptr;
      }
    } // Fallthrough
    case BUFFERED:
    case ATOMIC_FLAT:
    case NON_ATOMIC_FLAT: {
      if (is_empty_inline_type()) {
        return default_value();
      }
      Handle obj_h(THREAD, src);
      oop res = allocate_instance_buffer(CHECK_NULL);
      copy_payload_to_addr((void*)((char*)(oopDesc*)obj_h() + offset), payload_addr(res), lk, false);
      if (lk == NULLABLE_ATOMIC_FLAT) {
        if(is_payload_marked_as_null(payload_addr(res))) {
          return nullptr;
        }
      }
      return res;
    }
    break;
    default:
      ShouldNotReachHere();
  }
}

void InlineKlass::write_value_to_addr(oop src, void* dst, LayoutKind lk, bool dest_is_initialized, TRAPS) {
  void* src_addr = nullptr;
  if (src == nullptr) {
    if (lk != NULLABLE_ATOMIC_FLAT) {
      THROW_MSG(vmSymbols::java_lang_NullPointerException(), "Value is null");
    }
    // Writing null to a nullable flat field/element is usually done by writing
    // the whole pre-allocated null_reset_value at the payload address to ensure
    // that the null marker and all potential oops are reset to "zeros".
    // However, the null_reset_value is allocated during class initialization.
    // If the current value of the field is null, it is possible that the class
    // of the field has not been initialized yet and thus the null_reset_value
    // might not be available yet.
    // Writing null over an already null value should not trigger class initialization.
    // The solution is to detect null being written over null cases and return immediately
    // (writing null over null is a no-op from a field modification point of view)
    if (is_payload_marked_as_null((address)dst)) return;
    src_addr = payload_addr(null_reset_value());
  } else {
    src_addr = payload_addr(src);
    if (lk == NULLABLE_ATOMIC_FLAT) {
      mark_payload_as_non_null((address)src_addr);
    }
  }
  copy_payload_to_addr(src_addr, dst, lk, dest_is_initialized);
}

// Arrays of...

bool InlineKlass::flat_array() {
  if (!UseArrayFlattening) {
    return false;
  }
  // Too many embedded oops
  if ((FlatArrayElementMaxOops >= 0) && (nonstatic_oop_count() > FlatArrayElementMaxOops)) {
    return false;
  }
  // Declared atomic but not naturally atomic.
  if (must_be_atomic() && !is_naturally_atomic()) {
    return false;
  }
  // VM enforcing AlwaysAtomicAccess only...
  if (AlwaysAtomicAccesses && (!is_naturally_atomic())) {
    return false;
  }
  return true;
}

ObjArrayKlass* InlineKlass::null_free_reference_array(TRAPS) {
  if (Atomic::load_acquire(adr_null_free_reference_array_klass()) == nullptr) {
    // Atomic creation of array_klasses
    RecursiveLocker rl(MultiArray_lock, THREAD);

    // Check if update has already taken place
    if (null_free_reference_array_klass() == nullptr) {
      ObjArrayKlass* k = ObjArrayKlass::allocate_objArray_klass(class_loader_data(), 1, this, true, CHECK_NULL);

      // use 'release' to pair with lock-free load
      Atomic::release_store(adr_null_free_reference_array_klass(), k);
    }
  }
  return null_free_reference_array_klass();
}


// There's no reason for this method to have a TRAP argument
FlatArrayKlass* InlineKlass::flat_array_klass(LayoutKind lk, TRAPS) {
  FlatArrayKlass* volatile* adr_flat_array_klass = nullptr;
  switch(lk) {
    case NON_ATOMIC_FLAT:
      assert(has_non_atomic_layout(), "Must be");
      adr_flat_array_klass = adr_non_atomic_flat_array_klass();
      break;
    case ATOMIC_FLAT:
    assert(has_atomic_layout(), "Must be");
      adr_flat_array_klass = adr_atomic_flat_array_klass();
      break;
    case NULLABLE_ATOMIC_FLAT:
      assert(has_nullable_atomic_layout(), "Must be");
      adr_flat_array_klass = adr_nullable_atomic_flat_array_klass();
      break;
    default:
      ShouldNotReachHere();
  }

  if (Atomic::load_acquire(adr_flat_array_klass) == nullptr) {
    // Atomic creation of array_klasses
    RecursiveLocker rl(MultiArray_lock, THREAD);

    if (*adr_flat_array_klass == nullptr) {
      FlatArrayKlass* k = FlatArrayKlass::allocate_klass(this, lk, CHECK_NULL);
      Atomic::release_store(adr_flat_array_klass, k);
    }
  }
  return *adr_flat_array_klass;
}

FlatArrayKlass* InlineKlass::flat_array_klass_or_null(LayoutKind lk) {
    FlatArrayKlass* volatile* adr_flat_array_klass = nullptr;
  switch(lk) {
    case NON_ATOMIC_FLAT:
      assert(has_non_atomic_layout(), "Must be");
      adr_flat_array_klass = adr_non_atomic_flat_array_klass();
      break;
    case ATOMIC_FLAT:
    assert(has_atomic_layout(), "Must be");
      adr_flat_array_klass = adr_atomic_flat_array_klass();
      break;
    case NULLABLE_ATOMIC_FLAT:
      assert(has_nullable_atomic_layout(), "Must be");
      adr_flat_array_klass = adr_nullable_atomic_flat_array_klass();
      break;
    default:
      ShouldNotReachHere();
  }

  // Need load-acquire for lock-free read
  FlatArrayKlass* k = Atomic::load_acquire(adr_flat_array_klass);
  return k;
}

// Inline type arguments are not passed by reference, instead each
// field of the inline type is passed as an argument. This helper
// function collects the flat field (recursively)
// in a list. Included with the field's type is
// the offset of each field in the inline type: i2c and c2i adapters
// need that to load or store fields. Finally, the list of fields is
// sorted in order of increasing offsets: the adapters and the
// compiled code need to agree upon the order of fields.
//
// The list of basic types that is returned starts with a T_METADATA
// and ends with an extra T_VOID. T_METADATA/T_VOID pairs are used as
// delimiters. Every entry between the two is a field of the inline
// type. If there's an embedded inline type in the list, it also starts
// with a T_METADATA and ends with a T_VOID. This is so we can
// generate a unique fingerprint for the method's adapters and we can
// generate the list of basic types from the interpreter point of view
// (inline types passed as reference: iterate on the list until a
// T_METADATA, drop everything until and including the closing
// T_VOID) or the compiler point of view (each field of the inline
// types is an argument: drop all T_METADATA/T_VOID from the list).
//
// Value classes could also have fields in abstract super value classes.
// Use a HierarchicalFieldStream to get them as well.
int InlineKlass::collect_fields(GrowableArray<SigEntry>* sig, float& max_offset, int base_off, int null_marker_offset) {
  int count = 0;
  SigEntry::add_entry(sig, T_METADATA, name(), base_off);
  max_offset = base_off;
  for (HierarchicalFieldStream<JavaFieldStream> fs(this); !fs.done(); fs.next()) {
    if (fs.access_flags().is_static()) continue;
    int offset = base_off + fs.offset() - (base_off > 0 ? payload_offset() : 0);
    // TODO 8284443 Use different heuristic to decide what should be scalarized in the calling convention
    if (fs.is_flat()) {
      // Resolve klass of flat field and recursively collect fields
      int field_null_marker_offset = -1;
      if (!fs.is_null_free_inline_type()) {
        field_null_marker_offset = base_off + fs.null_marker_offset() - (base_off > 0 ? payload_offset() : 0);
      }
      Klass* vk = get_inline_type_field_klass(fs.index());
      count += InlineKlass::cast(vk)->collect_fields(sig, max_offset, offset, field_null_marker_offset);
    } else {
      BasicType bt = Signature::basic_type(fs.signature());
      SigEntry::add_entry(sig, bt, fs.signature(), offset);
      count += type2size[bt];
    }
    if (fs.field_descriptor().field_holder() != this) {
      // Inherited field, add an empty wrapper to this to distinguish it from a "local" field
      // with a different offset and avoid false adapter sharing. TODO 8348547 Is this sufficient?
      SigEntry::add_entry(sig, T_METADATA, name(), base_off);
      SigEntry::add_entry(sig, T_VOID, name(), offset);
    }
    max_offset = MAX2(max_offset, (float)offset);
  }
  int offset = base_off + size_helper()*HeapWordSize - (base_off > 0 ? payload_offset() : 0);
  // Null markers are no real fields, add them manually at the end (C2 relies on this) of the flat fields
  if (null_marker_offset != -1) {
    max_offset += 0.1f; // We add the markers "in-between" because they are no real fields
    SigEntry::add_entry(sig, T_BOOLEAN, name(), null_marker_offset, max_offset);
    count++;
  }
  SigEntry::add_entry(sig, T_VOID, name(), offset);
  if (base_off == 0) {
    sig->sort(SigEntry::compare);
  }
  assert(sig->at(0)._bt == T_METADATA && sig->at(sig->length()-1)._bt == T_VOID, "broken structure");
  return count;
}

void InlineKlass::initialize_calling_convention(TRAPS) {
  // Because the pack and unpack handler addresses need to be loadable from generated code,
  // they are stored at a fixed offset in the klass metadata. Since inline type klasses do
  // not have a vtable, the vtable offset is used to store these addresses.
  if (InlineTypeReturnedAsFields || InlineTypePassFieldsAsArgs) {
    ResourceMark rm;
    GrowableArray<SigEntry> sig_vk;
    float max_offset = 0;
    int nb_fields = collect_fields(&sig_vk, max_offset);
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
      assert(return_regs() == nullptr, "sanity");
    }
  }
}

void InlineKlass::deallocate_contents(ClassLoaderData* loader_data) {
  if (extended_sig() != nullptr) {
    MetadataFactory::free_array<SigEntry>(loader_data, extended_sig());
    *((Array<SigEntry>**)adr_extended_sig()) = nullptr;
  }
  if (return_regs() != nullptr) {
    MetadataFactory::free_array<VMRegPair>(loader_data, return_regs());
    *((Array<VMRegPair>**)adr_return_regs()) = nullptr;
  }
  cleanup_blobs();
  InstanceKlass::deallocate_contents(loader_data);
}

void InlineKlass::cleanup(InlineKlass* ik) {
  ik->cleanup_blobs();
}

void InlineKlass::cleanup_blobs() {
  if (pack_handler() != nullptr) {
    CodeBlob* buffered_blob = CodeCache::find_blob(pack_handler());
    assert(buffered_blob->is_buffered_inline_type_blob(), "bad blob type");
    BufferBlob::free((BufferBlob*)buffered_blob);
    *((address*)adr_pack_handler()) = nullptr;
    *((address*)adr_pack_handler_jobject()) = nullptr;
    *((address*)adr_unpack_handler()) = nullptr;
  }
}

// Can this inline type be passed as multiple values?
bool InlineKlass::can_be_passed_as_fields() const {
  return InlineTypePassFieldsAsArgs;
}

// Can this inline type be returned as multiple values?
bool InlineKlass::can_be_returned_as_fields(bool init) const {
  return InlineTypeReturnedAsFields && (init || return_regs() != nullptr);
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
      assert(v == nullptr || oopDesc::is_oop(v), "not an oop?");
      assert(Universe::heap()->is_in_or_null(v), "must be heap pointer");
      handles.push(Handle(thread, v));
    }
    if (bt == T_METADATA) {
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
  assert(regs != nullptr, "inconsistent");

  int j = 1;
  for (int i = 0, k = 0; i < sig_vk->length(); i++) {
    BasicType bt = sig_vk->at(i)._bt;
    if (bt == T_OBJECT || bt == T_ARRAY) {
      VMRegPair pair = regs->at(j);
      address loc = reg_map.location(pair.first(), nullptr);
      *(oop*)loc = handles.at(k++)();
    }
    if (bt == T_METADATA) {
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
    if (bt == T_METADATA) {
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
    // Return value is tagged, must be an InlineKlass pointer
    clear_nth_bit(ptr, 0);
    assert(Metaspace::contains((void*)ptr), "should be klass");
    InlineKlass* vk = (InlineKlass*)ptr;
    assert(vk->can_be_returned_as_fields(), "must be able to return as fields");
    return vk;
  }
  // Return value is not tagged, must be a valid oop
  assert(oopDesc::is_oop_or_null(cast_to_oop(ptr), true),
         "Bad oop return: " PTR_FORMAT, ptr);
  return nullptr;
}

// CDS support

void InlineKlass::metaspace_pointers_do(MetaspaceClosure* it) {
  InstanceKlass::metaspace_pointers_do(it);

  InlineKlass* this_ptr = this;
  it->push((Klass**)adr_non_atomic_flat_array_klass());
  it->push((Klass**)adr_atomic_flat_array_klass());
  it->push((Klass**)adr_nullable_atomic_flat_array_klass());
  it->push((Klass**)adr_null_free_reference_array_klass());
}

void InlineKlass::remove_unshareable_info() {
  InstanceKlass::remove_unshareable_info();

  // update it to point to the "buffered" copy of this class.
  _adr_inlineklass_fixed_block = inlineklass_static_block();
  ArchivePtrMarker::mark_pointer((address*)&_adr_inlineklass_fixed_block);

  *((Array<SigEntry>**)adr_extended_sig()) = nullptr;
  *((Array<VMRegPair>**)adr_return_regs()) = nullptr;
  *((address*)adr_pack_handler()) = nullptr;
  *((address*)adr_pack_handler_jobject()) = nullptr;
  *((address*)adr_unpack_handler()) = nullptr;
  assert(pack_handler() == nullptr, "pack handler not null");
  if (non_atomic_flat_array_klass() != nullptr) {
    non_atomic_flat_array_klass()->remove_unshareable_info();
  }
  if (atomic_flat_array_klass() != nullptr) {
    atomic_flat_array_klass()->remove_unshareable_info();
  }
  if (nullable_atomic_flat_array_klass() != nullptr) {
    nullable_atomic_flat_array_klass()->remove_unshareable_info();
  }
  if (null_free_reference_array_klass() != nullptr) {
    null_free_reference_array_klass()->remove_unshareable_info();
  }
}

void InlineKlass::remove_java_mirror() {
  InstanceKlass::remove_java_mirror();
  if (non_atomic_flat_array_klass() != nullptr) {
    non_atomic_flat_array_klass()->remove_java_mirror();
  }
  if (atomic_flat_array_klass() != nullptr) {
    atomic_flat_array_klass()->remove_java_mirror();
  }
  if (nullable_atomic_flat_array_klass() != nullptr) {
    nullable_atomic_flat_array_klass()->remove_java_mirror();
  }
  if (null_free_reference_array_klass() != nullptr) {
    null_free_reference_array_klass()->remove_java_mirror();
  }
}

void InlineKlass::restore_unshareable_info(ClassLoaderData* loader_data, Handle protection_domain, PackageEntry* pkg_entry, TRAPS) {
  InstanceKlass::restore_unshareable_info(loader_data, protection_domain, pkg_entry, CHECK);
  if (non_atomic_flat_array_klass() != nullptr) {
    non_atomic_flat_array_klass()->restore_unshareable_info(ClassLoaderData::the_null_class_loader_data(), Handle(), CHECK);
  }
  if (atomic_flat_array_klass() != nullptr) {
    atomic_flat_array_klass()->restore_unshareable_info(ClassLoaderData::the_null_class_loader_data(), Handle(), CHECK);
  }
  if (nullable_atomic_flat_array_klass() != nullptr) {
    nullable_atomic_flat_array_klass()->restore_unshareable_info(ClassLoaderData::the_null_class_loader_data(), Handle(), CHECK);
  }
  if (null_free_reference_array_klass() != nullptr) {
    null_free_reference_array_klass()->restore_unshareable_info(ClassLoaderData::the_null_class_loader_data(), Handle(), CHECK);
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
