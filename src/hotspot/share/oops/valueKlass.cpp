/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/metadataFactory.hpp"
#include "oops/access.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/fieldStreams.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/method.hpp"
#include "oops/oop.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/valueKlass.hpp"
#include "oops/valueArrayKlass.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/signature.hpp"
#include "utilities/copy.hpp"

int ValueKlass::first_field_offset() const {
#ifdef ASSERT
  int first_offset = INT_MAX;
  for (JavaFieldStream fs(this); !fs.done(); fs.next()) {
    if (fs.offset() < first_offset) first_offset= fs.offset();
  }
#endif
  int base_offset = instanceOopDesc::base_offset_in_bytes();
  // The first field of value types is aligned on a long boundary
  base_offset = align_up(base_offset, BytesPerLong);
  assert(base_offset == first_offset, "inconsistent offsets");
  return base_offset;
}

int ValueKlass::raw_value_byte_size() const {
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
  for (JavaFieldStream fs(this); !fs.done(); fs.next()) {
    if (fs.access_flags().is_static()) {
      continue;
    } else if (fs.offset() > last_offset) {
      BasicType type = fs.field_descriptor().field_type();
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

  instanceOop oop = (instanceOop)CollectedHeap::obj_allocate(this, size, CHECK_NULL);
  assert(oop->mark()->is_always_locked(), "Unlocked value type");
  return oop;
}

instanceOop ValueKlass::allocate_buffered_or_heap_instance(bool* in_heap, TRAPS) {
  assert(THREAD->is_Java_thread(), "Only Java threads can call this method");

  instanceOop value = NULL;
  if (is_bufferable()) {
    value = (instanceOop)VTBuffer::allocate_value(this, CHECK_NULL);
    *in_heap = false;
  }
  if (value == NULL) {
    log_info(valuetypes)("Value buffering failed, allocating in the Java heap");
    value = allocate_instance(CHECK_NULL);
    *in_heap = true;
  }
  return value;
}

bool ValueKlass::is_atomic() {
  return (nonstatic_field_size() * heapOopSize) <= longSize;
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

// Arrays of...

bool ValueKlass::flatten_array() {
  if (!ValueArrayFlatten) {
    return false;
  }

  int elem_bytes = raw_value_byte_size();
  // Too big
  if ((ValueArrayElemMaxFlatSize >= 0) && (elem_bytes > ValueArrayElemMaxFlatSize)) {
    return false;
  }
  // Too many embedded oops
  if ((ValueArrayElemMaxFlatOops >= 0) && (nonstatic_oop_count() > ValueArrayElemMaxFlatOops)) {
    return false;
  }

  return true;
}


Klass* ValueKlass::array_klass_impl(bool or_null, int n, TRAPS) {
  if (!flatten_array()) {
    return InstanceKlass::array_klass_impl(or_null, n, THREAD);
  }

  // Basically the same as instanceKlass, but using "ValueArrayKlass::allocate_klass"
  if (array_klasses() == NULL) {
    if (or_null) return NULL;

    ResourceMark rm;
    JavaThread *jt = (JavaThread *)THREAD;
    {
      // Atomic creation of array_klasses
      MutexLocker mc(Compile_lock, THREAD);   // for vtables
      MutexLocker ma(MultiArray_lock, THREAD);

      // Check if update has already taken place
      if (array_klasses() == NULL) {
        Klass* ak;
        if (is_atomic() || (!ValueArrayAtomicAccess)) {
          ak = ValueArrayKlass::allocate_klass(this, CHECK_NULL);
        } else {
          ak = ObjArrayKlass::allocate_objArray_klass(class_loader_data(), 1, this, CHECK_NULL);
        }
        set_array_klasses(ak);
      }
    }
  }
  // _this will always be set at this point
  ArrayKlass* ak = ArrayKlass::cast(array_klasses());
  if (or_null) {
    return ak->array_klass_or_null(n);
  }
  return ak->array_klass(n, THREAD);
}

Klass* ValueKlass::array_klass_impl(bool or_null, TRAPS) {
  return array_klass_impl(or_null, 1, THREAD);
}

void ValueKlass::raw_field_copy(void* src, void* dst, size_t raw_byte_size) {
  /*
   * Try not to shear fields even if not an atomic store...
   *
   * First 3 cases handle value array store, otherwise works on the same basis
   * as JVM_Clone, at this size data is aligned. The order of primitive types
   * is largest to smallest, and it not possible for fields to stradle long
   * copy boundaries.
   *
   * If MT without exclusive access, possible to observe partial value store,
   * but not partial primitive and reference field values
   */
  switch (raw_byte_size) {
    case 1:
      *((jbyte*) dst) = *(jbyte*)src;
      break;
    case 2:
      *((jshort*) dst) = *(jshort*)src;
      break;
    case 4:
      *((jint*) dst) = *(jint*) src;
      break;
    default:
      assert(raw_byte_size % sizeof(jlong) == 0, "Unaligned raw_byte_size");
      Copy::conjoint_jlongs_atomic((jlong*)src, (jlong*)dst, raw_byte_size >> LogBytesPerLong);
  }
}

/*
 * Store the value of this klass contained with src into dst.
 *
 * This operation is appropriate for use from vastore, vaload and putfield (for values)
 *
 * GC barriers currently can lock with no safepoint check and allocate c-heap,
 * so raw point is "safe" for now.
 *
 * Going forward, look to use machine generated (stub gen or bc) version for most used klass layouts
 *
 */
void ValueKlass::value_store(void* src, void* dst, size_t raw_byte_size, bool dst_heap, bool dst_uninitialized) {
  if (contains_oops()) {
    if (dst_heap) {
      // src/dst aren't oops, need offset to adjust oop map offset
      const address dst_oop_addr = ((address) dst) - first_field_offset();

      ModRefBarrierSet* bs = barrier_set_cast<ModRefBarrierSet>(BarrierSet::barrier_set());

      // Pre-barriers...
      OopMapBlock* map = start_of_nonstatic_oop_maps();
      OopMapBlock* const end = map + nonstatic_oop_map_count();
      while (map != end) {
        // Shame we can't just use the existing oop iterator...src/dst aren't oop
        address doop_address = dst_oop_addr + map->offset();
        // TEMP HACK: barrier code need to migrate to => access API (need own versions of value type ops)
        if (UseCompressedOops) {
          bs->write_ref_array_pre((narrowOop*) doop_address, map->count(), dst_uninitialized);
        } else {
          bs->write_ref_array_pre((oop*) doop_address, map->count(), dst_uninitialized);
        }
        map++;
      }

      raw_field_copy(src, dst, raw_byte_size);

      // Post-barriers...
      map = start_of_nonstatic_oop_maps();
      while (map != end) {
        address doop_address = dst_oop_addr + map->offset();
        bs->write_ref_array((HeapWord*) doop_address, map->count());
        map++;
      }
    } else { // Buffered value case
      raw_field_copy(src, dst, raw_byte_size);
    }
  } else {   // Primitive-only case...
    raw_field_copy(src, dst, raw_byte_size);
  }
}

// Value type arguments are not passed by reference, instead each
// field of the value type is passed as an argument. This helper
// function collects the fields of the value types (including embedded
// value type's fields) in a list. Included with the field's type is
// the offset of each field in the value type: i2c and c2i adapters
// need that to load or store fields. Finally, the list of fields is
// sorted in order of increasing offsets: the adapters and the
// compiled code need and agreed upon order of fields.
//
// The list of basic types that is returned starts with a T_VALUETYPE
// and ends with an extra T_VOID. T_VALUETYPE/T_VOID are used as
// delimiters. Every entry between the two is a field of the value
// type. If there's an embedded value type in the list, it also starts
// with a T_VALUETYPE and ends with a T_VOID. This is so we can
// generate a unique fingerprint for the method's adapters and we can
// generate the list of basic types from the interpreter point of view
// (value types passed as reference: iterate on the list until a
// T_VALUETYPE, drop everything until and including the closing
// T_VOID) or the compiler point of view (each field of the value
// types is an argument: drop all T_VALUETYPE/T_VOID from the list).
GrowableArray<SigEntry> ValueKlass::collect_fields(int base_off) const {
  GrowableArray<SigEntry> sig_extended;
  sig_extended.push(SigEntry(T_VALUETYPE, base_off));
  for (JavaFieldStream fs(this); !fs.done(); fs.next()) {
    if (fs.access_flags().is_static()) continue;
    fieldDescriptor& fd = fs.field_descriptor();
    BasicType bt = fd.field_type();
    int offset = base_off + fd.offset() - (base_off > 0 ? first_field_offset() : 0);
    if (bt == T_VALUETYPE) {
      if (fd.is_flattened()) {
        Symbol* signature = fd.signature();
        JavaThread* THREAD = JavaThread::current();
        oop loader = class_loader();
        oop domain = protection_domain();
        ResetNoHandleMark rnhm;
        HandleMark hm;
        NoSafepointVerifier nsv;
        Klass* klass = SystemDictionary::resolve_or_null(signature,
                                                         Handle(THREAD, loader), Handle(THREAD, domain),
                                                         THREAD);
        assert(klass != NULL && !HAS_PENDING_EXCEPTION, "lookup shouldn't fail");
        const GrowableArray<SigEntry>& embedded = ValueKlass::cast(klass)->collect_fields(offset);
        sig_extended.appendAll(&embedded);
      } else {
        sig_extended.push(SigEntry(T_VALUETYPEPTR, offset));
      }
    } else {
      sig_extended.push(SigEntry(bt, offset));
      if (bt == T_LONG || bt == T_DOUBLE) {
        sig_extended.push(SigEntry(T_VOID, offset));
      }
    }
  }
  int offset = base_off + size_helper()*HeapWordSize - (base_off > 0 ? first_field_offset() : 0);
  sig_extended.push(SigEntry(T_VOID, offset)); // hack: use T_VOID to mark end of value type fields
  if (base_off == 0) {
    sig_extended.sort(SigEntry::compare);
  }
  assert(sig_extended.at(0)._bt == T_VALUETYPE && sig_extended.at(sig_extended.length()-1)._bt == T_VOID, "broken structure");
  return sig_extended;
}

void ValueKlass::initialize_calling_convention() {
  // Because the pack and unpack handler addresses need to be loadable from generated code,
  // they are stored at a fixed offset in the klass metadata. Since value type klasses do
  // not have a vtable, the vtable offset is used to store these addresses.
  //guarantee(vtable_length() == 0, "vtables are not supported in value klasses");
  if (ValueTypeReturnedAsFields || ValueTypePassFieldsAsArgs) {
    Thread* THREAD = Thread::current();
    assert(!HAS_PENDING_EXCEPTION, "should have no exception");
    ResourceMark rm;
    const GrowableArray<SigEntry>& sig_vk = collect_fields();
    int nb_fields = SigEntry::count_fields(sig_vk)+1;
    Array<SigEntry>* extended_sig = MetadataFactory::new_array<SigEntry>(class_loader_data(), sig_vk.length(), CHECK_AND_CLEAR);
    *((Array<SigEntry>**)adr_extended_sig()) = extended_sig;
    for (int i = 0; i < sig_vk.length(); i++) {
      extended_sig->at_put(i, sig_vk.at(i));
    }

    if (ValueTypeReturnedAsFields) {
      BasicType* sig_bt = NEW_RESOURCE_ARRAY(BasicType, nb_fields);
      sig_bt[0] = T_METADATA;
      SigEntry::fill_sig_bt(sig_vk, sig_bt+1, nb_fields-1, true);
      VMRegPair* regs = NEW_RESOURCE_ARRAY(VMRegPair, nb_fields);
      int total = SharedRuntime::java_return_convention(sig_bt, regs, nb_fields);

      if (total > 0) {
        Array<VMRegPair>* return_regs = MetadataFactory::new_array<VMRegPair>(class_loader_data(), nb_fields, CHECK_AND_CLEAR);
        *((Array<VMRegPair>**)adr_return_regs()) = return_regs;
        for (int i = 0; i < nb_fields; i++) {
          return_regs->at_put(i, regs[i]);
        }

        BufferedValueTypeBlob* buffered_blob = SharedRuntime::generate_buffered_value_type_adapter(this);
        *((address*)adr_pack_handler()) = buffered_blob->pack_fields();
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
    *((address*)adr_unpack_handler()) = NULL;
  }
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
    if (bt == T_OBJECT || bt == T_VALUETYPEPTR || bt == T_ARRAY) {
      int off = sig_vk->at(i)._offset;
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
  assert(ValueTypeReturnedAsFields, "inconsistent");
  const Array<SigEntry>* sig_vk = extended_sig();
  const Array<VMRegPair>* regs = return_regs();
  assert(regs != NULL, "inconsistent");

  int j = 1;
  for (int i = 0, k = 0; i < sig_vk->length(); i++) {
    BasicType bt = sig_vk->at(i)._bt;
    if (bt == T_OBJECT || bt == T_ARRAY) {
      int off = sig_vk->at(i)._offset;
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
oop ValueKlass::realloc_result(const RegisterMap& reg_map, const GrowableArray<Handle>& handles, bool buffered, TRAPS) {
  bool ignored = false;
  oop new_vt = NULL;
  if (buffered) {
    new_vt = allocate_buffered_or_heap_instance(&ignored, CHECK_NULL);
  } else {
    new_vt = allocate_instance(CHECK_NULL);
  }

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
    VMRegPair pair = regs->at(j);
    address loc = reg_map.location(pair.first());
    switch(bt) {
    case T_BOOLEAN: {
      jboolean v = *(intptr_t*)loc;
      *(jboolean*)((address)new_vt + off) = v;
      break;
    }
    case T_CHAR: {
      jchar v = *(intptr_t*)loc;
      *(jchar*)((address)new_vt + off) = v;
      break;
    }
    case T_BYTE: {
      jbyte v = *(intptr_t*)loc;
      *(jbyte*)((address)new_vt + off) = v;
      break;
    }
    case T_SHORT: {
      jshort v = *(intptr_t*)loc;
      *(jshort*)((address)new_vt + off) = v;
      break;
    }
    case T_INT: {
      jint v = *(intptr_t*)loc;
      *(jint*)((address)new_vt + off) = v;
      break;
    }
    case T_LONG: {
#ifdef _LP64
      jlong v = *(intptr_t*)loc;
      *(jlong*)((address)new_vt + off) = v;
#else
      Unimplemented();
#endif
      break;
    }
    case T_OBJECT:
    case T_VALUETYPEPTR:
    case T_ARRAY: {
      Handle handle = handles.at(k++);
      HeapAccess<>::oop_store_at(new_vt, off, handle());
      break;
    }
    case T_FLOAT: {
      jfloat v = *(jfloat*)loc;
      *(jfloat*)((address)new_vt + off) = v;
      break;
    }
    case T_DOUBLE: {
      jdouble v = *(jdouble*)loc;
      *(jdouble*)((address)new_vt + off) = v;
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
    oop((HeapWord*)ptr)->verify();
  }
#endif
  return NULL;
}

void ValueKlass::iterate_over_inside_oops(OopClosure* f, oop value) {
  assert(!Universe::heap()->is_in_reserved(value), "This method is used on buffered values");

  oop* addr_mirror = (oop*)(value)->mark_addr_raw();
  f->do_oop_no_buffering(addr_mirror);

  if (!contains_oops()) return;

  OopMapBlock* map = start_of_nonstatic_oop_maps();
  OopMapBlock* const end_map = map + nonstatic_oop_map_count();

  if (!UseCompressedOops) {
    for (; map < end_map; map++) {
      oop* p = (oop*) (((char*)(oopDesc*)value) + map->offset());
      oop* const end = p + map->count();
      for (; p < end; ++p) {
        assert(oopDesc::is_oop_or_null(*p), "Sanity check");
        f->do_oop(p);
      }
    }
  } else {
    for (; map < end_map; map++) {
      narrowOop* p = (narrowOop*) (((char*)(oopDesc*)value) + map->offset());
      narrowOop* const end = p + map->count();
      for (; p < end; ++p) {
        oop o = CompressedOops::decode(*p);
        assert(Universe::heap()->is_in_reserved_or_null(o), "Sanity check");
        assert(oopDesc::is_oop_or_null(o), "Sanity check");
        f->do_oop(p);
      }
    }
  }
}

void ValueKlass::verify_on(outputStream* st) {
  InstanceKlass::verify_on(st);
  guarantee(prototype_header()->is_always_locked(), "Prototype header is not always locked");
}

void ValueKlass::oop_verify_on(oop obj, outputStream* st) {
  InstanceKlass::oop_verify_on(obj, st);
  guarantee(obj->mark()->is_always_locked(), "Header is not always locked");
}
