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

#include "classfile/moduleEntry.hpp"
#include "classfile/packageEntry.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "memory/iterator.inline.hpp"
#include "memory/metadataFactory.hpp"
#include "memory/metaspaceClosure.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/arrayKlass.inline.hpp"
#include "oops/arrayOop.hpp"
#include "oops/flatArrayOop.hpp"
#include "oops/flatArrayOop.inline.hpp"
#include "oops/inlineKlass.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/verifyOopClosure.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "utilities/copy.hpp"
#include "utilities/macros.hpp"

#include "oops/flatArrayKlass.hpp"

// Allocation...

FlatArrayKlass::FlatArrayKlass(Klass* element_klass, Symbol* name, LayoutKind lk) : ArrayKlass(name, Kind, markWord::flat_array_prototype(lk)) {
  assert(element_klass->is_inline_klass(), "Expected Inline");
  assert(lk == LayoutKind::NON_ATOMIC_FLAT || lk == LayoutKind::ATOMIC_FLAT || lk == LayoutKind::NULLABLE_ATOMIC_FLAT, "Must be a flat layout");

  set_element_klass(InlineKlass::cast(element_klass));
  set_class_loader_data(element_klass->class_loader_data());
  set_layout_kind(lk);

  set_layout_helper(array_layout_helper(InlineKlass::cast(element_klass), lk));
  assert(is_array_klass(), "sanity");
  assert(is_flatArray_klass(), "sanity");

#ifdef ASSERT
  assert(layout_helper_is_array(layout_helper()), "Must be");
  assert(layout_helper_is_flatArray(layout_helper()), "Must be");
  assert(layout_helper_element_type(layout_helper()) == T_FLAT_ELEMENT, "Must be");
  assert(prototype_header().is_flat_array(), "Must be");
  switch(lk) {
    case LayoutKind::NON_ATOMIC_FLAT:
    case LayoutKind::ATOMIC_FLAT:
      assert(layout_helper_is_null_free(layout_helper()), "Must be");
      assert(prototype_header().is_null_free_array(), "Must be");
    break;
    case LayoutKind::NULLABLE_ATOMIC_FLAT:
      assert(!layout_helper_is_null_free(layout_helper()), "Must be");
      assert(!prototype_header().is_null_free_array(), "Must be");
    break;
    default:
      ShouldNotReachHere();
    break;
  }
#endif // ASSERT

#ifndef PRODUCT
  if (PrintFlatArrayLayout) {
    print();
  }
#endif
}

FlatArrayKlass* FlatArrayKlass::allocate_klass(Klass* eklass, LayoutKind lk, TRAPS) {
  guarantee((!Universe::is_bootstrapping() || vmClasses::Object_klass_loaded()), "Really ?!");
  assert(UseArrayFlattening, "Flatten array required");
  assert(MultiArray_lock->holds_lock(THREAD), "must hold lock after bootstrapping");

  InlineKlass* element_klass = InlineKlass::cast(eklass);
  assert(element_klass->must_be_atomic() || (!AlwaysAtomicAccesses), "Atomic by-default");

  // Eagerly allocate the direct array supertype.
  Klass* super_klass = nullptr;
  Klass* element_super = element_klass->super();
  if (element_super != nullptr) {
    // The element type has a direct super.  E.g., String[] has direct super of Object[].
    super_klass = element_klass->array_klass(CHECK_NULL);
    // Also, see if the element has secondary supertypes.
    // We need an array type for each.
    const Array<Klass*>* element_supers = element_klass->secondary_supers();
    for( int i = element_supers->length()-1; i >= 0; i-- ) {
      Klass* elem_super = element_supers->at(i);
      elem_super->array_klass(CHECK_NULL);
    }
   // Fall through because inheritance is acyclic and we hold the global recursive lock to allocate all the arrays.
  }

  Symbol* name = ArrayKlass::create_element_klass_array_name(element_klass, CHECK_NULL);
  ClassLoaderData* loader_data = element_klass->class_loader_data();
  int size = ArrayKlass::static_size(FlatArrayKlass::header_size());
  FlatArrayKlass* vak = new (loader_data, size, THREAD) FlatArrayKlass(element_klass, name, lk);

  ModuleEntry* module = vak->module();
  assert(module != nullptr, "No module entry for array");
  complete_create_array_klass(vak, super_klass, module, CHECK_NULL);

  loader_data->add_class(vak);

  return vak;
}

void FlatArrayKlass::initialize(TRAPS) {
  element_klass()->initialize(THREAD);
}

void FlatArrayKlass::metaspace_pointers_do(MetaspaceClosure* it) {
  ArrayKlass::metaspace_pointers_do(it);
  it->push(&_element_klass);
}

// Oops allocation...
flatArrayOop FlatArrayKlass::allocate(int length, LayoutKind lk, TRAPS) {
  check_array_allocation_length(length, max_elements(), CHECK_NULL);
  int size = flatArrayOopDesc::object_size(layout_helper(), length);
  flatArrayOop array = (flatArrayOop) Universe::heap()->array_allocate(this, size, length, true, CHECK_NULL);
  return array;
}

oop FlatArrayKlass::multi_allocate(int rank, jint* last_size, TRAPS) {
  // FlatArrays only have one dimension
  ShouldNotReachHere();
}

jint FlatArrayKlass::array_layout_helper(InlineKlass* vk, LayoutKind lk) {
  BasicType etype = T_FLAT_ELEMENT;
  int esize = log2i_exact(round_up_power_of_2(vk->layout_size_in_bytes(lk)));
  int hsize = arrayOopDesc::base_offset_in_bytes(etype);
  bool null_free = lk != LayoutKind::NULLABLE_ATOMIC_FLAT;
  int lh = Klass::array_layout_helper(_lh_array_tag_flat_value, null_free, hsize, etype, esize);

  assert(lh < (int)_lh_neutral_value, "must look like an array layout");
  assert(layout_helper_is_array(lh), "correct kind");
  assert(layout_helper_is_flatArray(lh), "correct kind");
  assert(!layout_helper_is_typeArray(lh), "correct kind");
  assert(!layout_helper_is_objArray(lh), "correct kind");
  assert(layout_helper_is_null_free(lh) == null_free, "correct kind");
  assert(layout_helper_header_size(lh) == hsize, "correct decode");
  assert(layout_helper_element_type(lh) == etype, "correct decode");
  assert(layout_helper_log2_element_size(lh) == esize, "correct decode");
  assert((1 << esize) < BytesPerLong || is_aligned(hsize, HeapWordsPerLong), "unaligned base");

  return lh;
}

size_t FlatArrayKlass::oop_size(oop obj) const {
  assert(obj->klass()->is_flatArray_klass(),"must be an flat array");
  flatArrayOop array = flatArrayOop(obj);
  return array->object_size();
}

// For now return the maximum number of array elements that will not exceed:
// nof bytes = "max_jint * HeapWord" since the "oopDesc::oop_iterate_size"
// returns "int" HeapWords, need fix for JDK-4718400 and JDK-8233189
jint FlatArrayKlass::max_elements() const {
  // Check the max number of heap words limit first (because of int32_t in oopDesc_oop_size() etc)
  size_t max_size = max_jint;
  max_size -= (arrayOopDesc::base_offset_in_bytes(T_FLAT_ELEMENT) >> LogHeapWordSize);
  max_size = align_down(max_size, MinObjAlignment);
  max_size <<= LogHeapWordSize;                                  // convert to max payload size in bytes
  max_size >>= layout_helper_log2_element_size(_layout_helper);  // divide by element size (in bytes) = max elements
  // Within int32_t heap words, still can't exceed Java array element limit
  if (max_size > max_jint) {
    max_size = max_jint;
  }
  assert((max_size >> LogHeapWordSize) <= max_jint, "Overflow");
  return (jint) max_size;
}

oop FlatArrayKlass::protection_domain() const {
  return element_klass()->protection_domain();
}

// Temp hack having this here: need to move towards Access API
static bool needs_backwards_copy(arrayOop s, int src_pos,
                                 arrayOop d, int dst_pos, int length) {
  return (s == d) && (dst_pos > src_pos) && (dst_pos - src_pos) < length;
}

void FlatArrayKlass::copy_array(arrayOop s, int src_pos,
                                arrayOop d, int dst_pos, int length, TRAPS) {

  assert(s->is_objArray() || s->is_flatArray(), "must be obj or flat array");

  // Check destination
  if ((!d->is_flatArray()) && (!d->is_objArray())) {
    THROW(vmSymbols::java_lang_ArrayStoreException());
  }

  // Check if all offsets and lengths are non negative
  if (src_pos < 0 || dst_pos < 0 || length < 0) {
    THROW(vmSymbols::java_lang_ArrayIndexOutOfBoundsException());
  }
  // Check if the ranges are valid
  if  ( (((unsigned int) length + (unsigned int) src_pos) > (unsigned int) s->length())
      || (((unsigned int) length + (unsigned int) dst_pos) > (unsigned int) d->length()) ) {
    THROW(vmSymbols::java_lang_ArrayIndexOutOfBoundsException());
  }
  // Check zero copy
  if (length == 0)
    return;

  ArrayKlass* sk = ArrayKlass::cast(s->klass());
  ArrayKlass* dk = ArrayKlass::cast(d->klass());
  Klass* d_elem_klass = dk->element_klass();
  Klass* s_elem_klass = sk->element_klass();
  /**** CMH: compare and contrast impl, re-factor once we find edge cases... ****/

  if (sk->is_flatArray_klass()) {
    assert(sk == this, "Unexpected call to copy_array");
    FlatArrayKlass* fsk = FlatArrayKlass::cast(sk);
    // Check subtype, all src homogeneous, so just once
    if (!s_elem_klass->is_subtype_of(d_elem_klass)) {
      THROW(vmSymbols::java_lang_ArrayStoreException());
    }

    flatArrayOop sa = flatArrayOop(s);
    InlineKlass* s_elem_vklass = element_klass();

    // flatArray-to-flatArray
    if (dk->is_flatArray_klass()) {
      // element types MUST be exact, subtype check would be dangerous
      if (d_elem_klass != this->element_klass()) {
        THROW(vmSymbols::java_lang_ArrayStoreException());
      }

      FlatArrayKlass* fdk = FlatArrayKlass::cast(dk);
      InlineKlass* vk = InlineKlass::cast(s_elem_klass);
      flatArrayOop da = flatArrayOop(d);
      int src_incr = fsk->element_byte_size();
      int dst_incr = fdk->element_byte_size();

      if (fsk->layout_kind() == fdk->layout_kind()) {
        assert(src_incr == dst_incr, "Must be");
        if (needs_backwards_copy(sa, src_pos, da, dst_pos, length)) {
          address dst = (address) da->value_at_addr(dst_pos + length - 1, fdk->layout_helper());
          address src = (address) sa->value_at_addr(src_pos + length - 1, fsk->layout_helper());
          for (int i = 0; i < length; i++) {
            // because source and destination have the same layout, bypassing the InlineKlass copy methods
            // and call AccessAPI directly
            HeapAccess<>::value_copy(src, dst, vk, fsk->layout_kind());
            dst -= dst_incr;
            src -= src_incr;
          }
        } else {
          // source and destination share same layout, direct copy from array to array is possible
          address dst = (address) da->value_at_addr(dst_pos, fdk->layout_helper());
          address src = (address) sa->value_at_addr(src_pos, fsk->layout_helper());
          for (int i = 0; i < length; i++) {
            // because source and destination have the same layout, bypassing the InlineKlass copy methods
            // and call AccessAPI directly
            HeapAccess<>::value_copy(src, dst, vk, fsk->layout_kind());
            dst += dst_incr;
            src += src_incr;
          }
        }
      } else {
        flatArrayHandle hd(THREAD, da);
        flatArrayHandle hs(THREAD, sa);
        // source and destination layouts mismatch, simpler solution is to copy through an intermediate buffer (heap instance)
        bool need_null_check = fsk->layout_kind() == LayoutKind::NULLABLE_ATOMIC_FLAT && fdk->layout_kind() != LayoutKind::NULLABLE_ATOMIC_FLAT;
        oop buffer = vk->allocate_instance(CHECK);
        address dst = (address) hd->value_at_addr(dst_pos, fdk->layout_helper());
        address src = (address) hs->value_at_addr(src_pos, fsk->layout_helper());
        for (int i = 0; i < length; i++) {
          if (need_null_check) {
            if (vk->is_payload_marked_as_null(src)) {
              THROW(vmSymbols::java_lang_NullPointerException());
            }
          }
          vk->copy_payload_to_addr(src, vk->payload_addr(buffer), fsk->layout_kind(), true);
          if (vk->has_nullable_atomic_layout()) {
            // Setting null marker to not zero for non-nullable source layouts
            vk->mark_payload_as_non_null(vk->payload_addr(buffer));
          }
          vk->copy_payload_to_addr(vk->payload_addr(buffer), dst, fdk->layout_kind(), true);
          dst += dst_incr;
          src += src_incr;
        }
      }
    } else { // flatArray-to-objArray
      assert(dk->is_objArray_klass(), "Expected objArray here");
      // Need to allocate each new src elem payload -> dst oop
      objArrayHandle dh(THREAD, (objArrayOop)d);
      flatArrayHandle sh(THREAD, sa);
      InlineKlass* vk = InlineKlass::cast(s_elem_klass);
      for (int i = 0; i < length; i++) {
        oop o = sh->read_value_from_flat_array(src_pos + i, CHECK);
        dh->obj_at_put(dst_pos + i, o);
      }
    }
  } else {
    assert(s->is_objArray(), "Expected objArray");
    objArrayOop sa = objArrayOop(s);
    assert(d->is_flatArray(), "Expected flatArray");  // objArray-to-flatArray
    InlineKlass* d_elem_vklass = InlineKlass::cast(d_elem_klass);
    flatArrayOop da = flatArrayOop(d);
    FlatArrayKlass* fdk = FlatArrayKlass::cast(da->klass());
    InlineKlass* vk = InlineKlass::cast(d_elem_klass);

    for (int i = 0; i < length; i++) {
      da->write_value_to_flat_array(sa->obj_at(src_pos + i), dst_pos + i, CHECK);
    }
  }
}

ModuleEntry* FlatArrayKlass::module() const {
  assert(element_klass() != nullptr, "FlatArrayKlass returned unexpected nullptr bottom_klass");
  // The array is defined in the module of its bottom class
  return element_klass()->module();
}

PackageEntry* FlatArrayKlass::package() const {
  assert(element_klass() != nullptr, "FlatArrayKlass returned unexpected nullptr bottom_klass");
  return element_klass()->package();
}

bool FlatArrayKlass::can_be_primary_super_slow() const {
    return true;
}

GrowableArray<Klass*>* FlatArrayKlass::compute_secondary_supers(int num_extra_slots,
                                                                Array<InstanceKlass*>* transitive_interfaces) {
  assert(transitive_interfaces == nullptr, "sanity");
  // interfaces = { cloneable_klass, serializable_klass, elemSuper[], ... };
  Array<Klass*>* elem_supers = element_klass()->secondary_supers();
  int num_elem_supers = elem_supers == nullptr ? 0 : elem_supers->length();
  int num_secondaries = num_extra_slots + 2 + num_elem_supers;
  GrowableArray<Klass*>* secondaries = new GrowableArray<Klass*>(num_elem_supers+2);

  secondaries->push(vmClasses::Cloneable_klass());
  secondaries->push(vmClasses::Serializable_klass());
  for (int i = 0; i < num_elem_supers; i++) {
    Klass* elem_super = (Klass*) elem_supers->at(i);
    Klass* array_super = elem_super->array_klass_or_null();
    assert(array_super != nullptr, "must already have been created");
    secondaries->push(array_super);
  }
  return secondaries;
}

u2 FlatArrayKlass::compute_modifier_flags() const {
  // The modifier for an flatArray is the same as its element
  // With the addition of ACC_IDENTITY
  u2 element_flags = element_klass()->compute_modifier_flags();

  u2 identity_flag = (Arguments::enable_preview()) ? JVM_ACC_IDENTITY : 0;

  return (element_flags & (JVM_ACC_PUBLIC | JVM_ACC_PRIVATE | JVM_ACC_PROTECTED))
                        | (identity_flag | JVM_ACC_ABSTRACT | JVM_ACC_FINAL);
}

void FlatArrayKlass::print_on(outputStream* st) const {
#ifndef PRODUCT
  assert(!is_objArray_klass(), "Unimplemented");

  st->print("Flat Type Array: ");
  Klass::print_on(st);

  st->print(" - element klass: ");
  element_klass()->print_value_on(st);
  st->cr();

  int elem_size = element_byte_size();
  st->print(" - element size %i ", elem_size);
  st->print("aligned layout size %i", 1 << layout_helper_log2_element_size(layout_helper()));
  st->cr();
#endif //PRODUCT
}

void FlatArrayKlass::print_value_on(outputStream* st) const {
  assert(is_klass(), "must be klass");

  element_klass()->print_value_on(st);
  st->print("[]");
}


#ifndef PRODUCT
void FlatArrayKlass::oop_print_on(oop obj, outputStream* st) {
  ArrayKlass::oop_print_on(obj, st);
  flatArrayOop va = flatArrayOop(obj);
  InlineKlass* vk = element_klass();
  int print_len = MIN2(va->length(), MaxElementPrintSize);
  for(int index = 0; index < print_len; index++) {
    int off = (address) va->value_at_addr(index, layout_helper()) - cast_from_oop<address>(obj);
    st->print_cr(" - Index %3d offset %3d: ", index, off);
    oop obj = cast_to_oop((address)va->value_at_addr(index, layout_helper()) - vk->payload_offset());
    FieldPrinter print_field(st, obj);
    vk->do_nonstatic_fields(&print_field);
    st->cr();
  }
  int remaining = va->length() - print_len;
  if (remaining > 0) {
    st->print_cr(" - <%d more elements, increase MaxElementPrintSize to print>", remaining);
  }
}
#endif //PRODUCT

void FlatArrayKlass::oop_print_value_on(oop obj, outputStream* st) {
  assert(obj->is_flatArray(), "must be flatArray");
  st->print("a ");
  element_klass()->print_value_on(st);
  int len = flatArrayOop(obj)->length();
  st->print("[%d] ", len);
  obj->print_address_on(st);
  if (PrintMiscellaneous && (WizardMode || Verbose)) {
    int lh = layout_helper();
    st->print("{");
    for (int i = 0; i < len; i++) {
      if (i > 4) {
        st->print("..."); break;
      }
      st->print(" " INTPTR_FORMAT, (intptr_t)(void*)flatArrayOop(obj)->value_at_addr(i , lh));
    }
    st->print(" }");
  }
}

// Verification
class VerifyElementClosure: public BasicOopIterateClosure {
 public:
  virtual void do_oop(oop* p)       { VerifyOopClosure::verify_oop.do_oop(p); }
  virtual void do_oop(narrowOop* p) { VerifyOopClosure::verify_oop.do_oop(p); }
};

void FlatArrayKlass::oop_verify_on(oop obj, outputStream* st) {
  ArrayKlass::oop_verify_on(obj, st);
  guarantee(obj->is_flatArray(), "must be flatArray");

  if (contains_oops()) {
    flatArrayOop va = flatArrayOop(obj);
    VerifyElementClosure ec;
    va->oop_iterate(&ec);
  }
}

void FlatArrayKlass::verify_on(outputStream* st) {
  ArrayKlass::verify_on(st);
  guarantee(element_klass()->is_inline_klass(), "should be inline type klass");
}
