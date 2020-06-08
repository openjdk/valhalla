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
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/valueKlass.hpp"
#include "oops/valueArrayOop.hpp"
#include "oops/valueArrayOop.inline.hpp"
#include "oops/verifyOopClosure.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "utilities/copy.hpp"
#include "utilities/macros.hpp"

#include "oops/valueArrayKlass.hpp"

// Allocation...

ValueArrayKlass::ValueArrayKlass(Klass* element_klass, Symbol* name) : ArrayKlass(name, ID) {
  assert(element_klass->is_value(), "Expected Value");

  set_element_klass(ValueKlass::cast(element_klass));
  set_class_loader_data(element_klass->class_loader_data());
  set_layout_helper(array_layout_helper(ValueKlass::cast(element_klass)));

  assert(is_array_klass(), "sanity");
  assert(is_valueArray_klass(), "sanity");

  CMH("tweak name symbol refcnt ?")
#ifndef PRODUCT
  if (PrintInlineArrayLayout) {
    print();
  }
#endif
}

ValueKlass* ValueArrayKlass::element_klass() const {
  return ValueKlass::cast(_element_klass);
}

void ValueArrayKlass::set_element_klass(Klass* k) {
  _element_klass = k;
}

ValueArrayKlass* ValueArrayKlass::allocate_klass(Klass* element_klass, TRAPS) {
  guarantee((!Universe::is_bootstrapping() || SystemDictionary::Object_klass_loaded()), "Really ?!");
  assert(ValueArrayFlatten, "Flatten array required");
  assert(ValueKlass::cast(element_klass)->is_naturally_atomic() || (!InlineArrayAtomicAccess), "Atomic by-default");

  /*
   *  MVT->LWorld, now need to allocate secondaries array types, just like objArrayKlass...
   *  ...so now we are trying out covariant array types, just copy objArrayKlass
   *  TODO refactor any remaining commonality
   *
   */
  // Eagerly allocate the direct array supertype.
  Klass* super_klass = NULL;
  Klass* element_super = element_klass->super();
  if (element_super != NULL) {
    // The element type has a direct super.  E.g., String[] has direct super of Object[].
    super_klass = element_super->array_klass_or_null();
    bool supers_exist = super_klass != NULL;
    // Also, see if the element has secondary supertypes.
    // We need an array type for each.
    const Array<Klass*>* element_supers = element_klass->secondary_supers();
    for( int i = element_supers->length()-1; i >= 0; i-- ) {
      Klass* elem_super = element_supers->at(i);
      if (elem_super->array_klass_or_null() == NULL) {
        supers_exist = false;
        break;
      }
    }
    if (!supers_exist) {
      // Oops.  Not allocated yet.  Back out, allocate it, and retry.
      Klass* ek = NULL;
      {
        MutexUnlocker mu(MultiArray_lock);
        super_klass = element_super->array_klass(CHECK_NULL);
        for( int i = element_supers->length()-1; i >= 0; i-- ) {
          Klass* elem_super = element_supers->at(i);
          elem_super->array_klass(CHECK_NULL);
        }
        // Now retry from the beginning
        ek = element_klass->array_klass(CHECK_NULL);
      }  // re-lock
      return ValueArrayKlass::cast(ek);
    }
  }

  Symbol* name = ArrayKlass::create_element_klass_array_name(element_klass, CHECK_NULL);
  ClassLoaderData* loader_data = element_klass->class_loader_data();
  int size = ArrayKlass::static_size(ValueArrayKlass::header_size());
  ValueArrayKlass* vak = new (loader_data, size, THREAD) ValueArrayKlass(element_klass, name);

  ModuleEntry* module = vak->module();
  assert(module != NULL, "No module entry for array");
  complete_create_array_klass(vak, super_klass, module, CHECK_NULL);

  loader_data->add_class(vak);

  return vak;
}

void ValueArrayKlass::initialize(TRAPS) {
  element_klass()->initialize(THREAD);
}

// Oops allocation...
valueArrayOop ValueArrayKlass::allocate(int length, TRAPS) {
  check_array_allocation_length(length, max_elements(), CHECK_NULL);
  int size = valueArrayOopDesc::object_size(layout_helper(), length);
  return (valueArrayOop) Universe::heap()->array_allocate(this, size, length, true, THREAD);
}


oop ValueArrayKlass::multi_allocate(int rank, jint* last_size, TRAPS) {
  // For valueArrays this is only called for the last dimension
  assert(rank == 1, "just checking");
  int length = *last_size;
  return allocate(length, THREAD);
}

jint ValueArrayKlass::array_layout_helper(ValueKlass* vk) {
  BasicType etype = T_VALUETYPE;
  int esize = upper_log2(vk->raw_value_byte_size());
  int hsize = arrayOopDesc::base_offset_in_bytes(etype);

  int lh = Klass::array_layout_helper(_lh_array_tag_vt_value, true, hsize, etype, esize);

  assert(lh < (int)_lh_neutral_value, "must look like an array layout");
  assert(layout_helper_is_array(lh), "correct kind");
  assert(layout_helper_is_valueArray(lh), "correct kind");
  assert(!layout_helper_is_typeArray(lh), "correct kind");
  assert(!layout_helper_is_objArray(lh), "correct kind");
  assert(layout_helper_is_null_free(lh), "correct kind");
  assert(layout_helper_header_size(lh) == hsize, "correct decode");
  assert(layout_helper_element_type(lh) == etype, "correct decode");
  assert(layout_helper_log2_element_size(lh) == esize, "correct decode");
  assert((1 << esize) < BytesPerLong || is_aligned(hsize, HeapWordsPerLong), "unaligned base");

  return lh;
}

int ValueArrayKlass::oop_size(oop obj) const {
  assert(obj->is_valueArray(),"must be a value array");
  valueArrayOop array = valueArrayOop(obj);
  return array->object_size();
}

// For now return the maximum number of array elements that will not exceed:
// nof bytes = "max_jint * HeapWord" since the "oopDesc::oop_iterate_size"
// returns "int" HeapWords, need fix for JDK-4718400 and JDK-8233189
jint ValueArrayKlass::max_elements() const {
  // Check the max number of heap words limit first (because of int32_t in oopDesc_oop_size() etc)
  size_t max_size = max_jint;
  max_size -= arrayOopDesc::header_size(T_VALUETYPE);
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

oop ValueArrayKlass::protection_domain() const {
  return element_klass()->protection_domain();
}

// Temp hack having this here: need to move towards Access API
static bool needs_backwards_copy(arrayOop s, int src_pos,
                                 arrayOop d, int dst_pos, int length) {
  return (s == d) && (dst_pos > src_pos) && (dst_pos - src_pos) < length;
}

void ValueArrayKlass::copy_array(arrayOop s, int src_pos,
                                 arrayOop d, int dst_pos, int length, TRAPS) {

  assert(s->is_objArray() || s->is_valueArray(), "must be obj or value array");

   // Check destination
   if ((!d->is_valueArray()) && (!d->is_objArray())) {
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

   if (sk->is_valueArray_klass()) {
     assert(sk == this, "Unexpected call to copy_array");
     // Check subtype, all src homogeneous, so just once
     if (!s_elem_klass->is_subtype_of(d_elem_klass)) {
       THROW(vmSymbols::java_lang_ArrayStoreException());
     }

     valueArrayOop sa = valueArrayOop(s);
     ValueKlass* s_elem_vklass = element_klass();

     // valueArray-to-valueArray
     if (dk->is_valueArray_klass()) {
       // element types MUST be exact, subtype check would be dangerous
       if (dk != this) {
         THROW(vmSymbols::java_lang_ArrayStoreException());
       }

       valueArrayOop da = valueArrayOop(d);
       address dst = (address) da->value_at_addr(dst_pos, layout_helper());
       address src = (address) sa->value_at_addr(src_pos, layout_helper());
       if (contains_oops()) {
         int elem_incr = 1 << log2_element_size();
         address src_end = src + (length << log2_element_size());
         if (needs_backwards_copy(s, src_pos, d, dst_pos, length)) {
           swap(src, src_end);
           dst = dst + (length << log2_element_size());
           do {
             src -= elem_incr;
             dst -= elem_incr;
             HeapAccess<>::value_copy(src, dst, s_elem_vklass);
           } while (src > src_end);
         } else {
           address src_end = src + (length << log2_element_size());
           while (src < src_end) {
             HeapAccess<>::value_copy(src, dst, s_elem_vklass);
             src += elem_incr;
             dst += elem_incr;
           }
         }
       } else {
         // we are basically a type array...don't bother limiting element copy
         // it would have to be a lot wasted space to be worth value_store() calls, need a setting here ?
         Copy::conjoint_memory_atomic(src, dst, (size_t)length << log2_element_size());
       }
     }
     else { // valueArray-to-objArray
       assert(dk->is_objArray_klass(), "Expected objArray here");
       // Need to allocate each new src elem payload -> dst oop
       objArrayHandle dh(THREAD, (objArrayOop)d);
       valueArrayHandle sh(THREAD, sa);
       int dst_end = dst_pos + length;
       while (dst_pos < dst_end) {
         oop o = valueArrayOopDesc::value_alloc_copy_from_index(sh, src_pos, CHECK);
         dh->obj_at_put(dst_pos, o);
         dst_pos++;
         src_pos++;
       }
     }
   } else {
     assert(s->is_objArray(), "Expected objArray");
     objArrayOop sa = objArrayOop(s);
     assert(d->is_valueArray(), "Excepted valueArray");  // objArray-to-valueArray
     ValueKlass* d_elem_vklass = ValueKlass::cast(d_elem_klass);
     valueArrayOop da = valueArrayOop(d);

     int src_end = src_pos + length;
     int delem_incr = 1 << dk->log2_element_size();
     address dst = (address) da->value_at_addr(dst_pos, layout_helper());
     while (src_pos < src_end) {
       oop se = sa->obj_at(src_pos);
       if (se == NULL) {
         THROW(vmSymbols::java_lang_NullPointerException());
       }
       // Check exact type per element
       if (se->klass() != d_elem_klass) {
         THROW(vmSymbols::java_lang_ArrayStoreException());
       }
       d_elem_vklass->value_copy_oop_to_payload(se, dst);
       dst += delem_incr;
       src_pos++;
     }
   }
}


Klass* ValueArrayKlass::array_klass_impl(bool or_null, int n, TRAPS) {
  assert(dimension() <= n, "check order of chain");
  int dim = dimension();
  if (dim == n) return this;

  if (higher_dimension_acquire() == NULL) {
    if (or_null)  return NULL;

    ResourceMark rm;
    {
      // Ensure atomic creation of higher dimensions
      MutexLocker mu(THREAD, MultiArray_lock);

      // Check if another thread beat us
      if (higher_dimension() == NULL) {

        // Create multi-dim klass object and link them together
        Klass* k =
          ObjArrayKlass::allocate_objArray_klass(dim + 1, this, CHECK_NULL);
        ObjArrayKlass* ak = ObjArrayKlass::cast(k);
        ak->set_lower_dimension(this);
        OrderAccess::storestore();
        release_set_higher_dimension(ak);
        assert(ak->is_objArray_klass(), "incorrect initialization of ObjArrayKlass");
      }
    }
  } else {
    CHECK_UNHANDLED_OOPS_ONLY(Thread::current()->clear_unhandled_oops());
  }

  ObjArrayKlass *ak = ObjArrayKlass::cast(higher_dimension());
  if (or_null) {
    return ak->array_klass_or_null(n);
  }
  return ak->array_klass(n, THREAD);
}

Klass* ValueArrayKlass::array_klass_impl(bool or_null, TRAPS) {
  return array_klass_impl(or_null, dimension() +  1, THREAD);
}

ModuleEntry* ValueArrayKlass::module() const {
  assert(element_klass() != NULL, "ValueArrayKlass returned unexpected NULL bottom_klass");
  // The array is defined in the module of its bottom class
  return element_klass()->module();
}

PackageEntry* ValueArrayKlass::package() const {
  assert(element_klass() != NULL, "ValuerrayKlass returned unexpected NULL bottom_klass");
  return element_klass()->package();
}

bool ValueArrayKlass::can_be_primary_super_slow() const {
    return true;
}

GrowableArray<Klass*>* ValueArrayKlass::compute_secondary_supers(int num_extra_slots,
                                                                 Array<InstanceKlass*>* transitive_interfaces) {
  assert(transitive_interfaces == NULL, "sanity");
  // interfaces = { cloneable_klass, serializable_klass, elemSuper[], ... };
  Array<Klass*>* elem_supers = element_klass()->secondary_supers();
  int num_elem_supers = elem_supers == NULL ? 0 : elem_supers->length();
  int num_secondaries = num_extra_slots + 2 + num_elem_supers;
  if (num_secondaries == 2) {
    // Must share this for correct bootstrapping!
    set_secondary_supers(Universe::the_array_interfaces_array());
    return NULL;
  } else {
    GrowableArray<Klass*>* secondaries = new GrowableArray<Klass*>(num_elem_supers+3);
    secondaries->push(SystemDictionary::Cloneable_klass());
    secondaries->push(SystemDictionary::Serializable_klass());
    secondaries->push(SystemDictionary::IdentityObject_klass());
    for (int i = 0; i < num_elem_supers; i++) {
      Klass* elem_super = (Klass*) elem_supers->at(i);
      Klass* array_super = elem_super->array_klass_or_null();
      assert(array_super != NULL, "must already have been created");
      secondaries->push(array_super);
    }
    return secondaries;
  }
}

void ValueArrayKlass::print_on(outputStream* st) const {
#ifndef PRODUCT
  assert(!is_objArray_klass(), "Unimplemented");

  st->print("Value Type Array: ");
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

void ValueArrayKlass::print_value_on(outputStream* st) const {
  assert(is_klass(), "must be klass");

  element_klass()->print_value_on(st);
  st->print("[]");
}


#ifndef PRODUCT
void ValueArrayKlass::oop_print_on(oop obj, outputStream* st) {
  ArrayKlass::oop_print_on(obj, st);
  valueArrayOop va = valueArrayOop(obj);
  ValueKlass* vk = element_klass();
  int print_len = MIN2((intx) va->length(), MaxElementPrintSize);
  for(int index = 0; index < print_len; index++) {
    int off = (address) va->value_at_addr(index, layout_helper()) - cast_from_oop<address>(obj);
    st->print_cr(" - Index %3d offset %3d: ", index, off);
    oop obj = (oop) ((address)va->value_at_addr(index, layout_helper()) - vk->first_field_offset());
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

void ValueArrayKlass::oop_print_value_on(oop obj, outputStream* st) {
  assert(obj->is_valueArray(), "must be valueArray");
  st->print("a ");
  element_klass()->print_value_on(st);
  int len = valueArrayOop(obj)->length();
  st->print("[%d] ", len);
  obj->print_address_on(st);
  if (PrintMiscellaneous && (WizardMode || Verbose)) {
    int lh = layout_helper();
    st->print("{");
    for (int i = 0; i < len; i++) {
      if (i > 4) {
        st->print("..."); break;
      }
      st->print(" " INTPTR_FORMAT, (intptr_t)(void*)valueArrayOop(obj)->value_at_addr(i , lh));
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

void ValueArrayKlass::oop_verify_on(oop obj, outputStream* st) {
  ArrayKlass::oop_verify_on(obj, st);
  guarantee(obj->is_valueArray(), "must be valueArray");

  if (contains_oops()) {
    valueArrayOop va = valueArrayOop(obj);
    VerifyElementClosure ec;
    va->oop_iterate(&ec);
  }
}

void ValueArrayKlass::verify_on(outputStream* st) {
  ArrayKlass::verify_on(st);
  guarantee(element_klass()->is_value(), "should be value type klass");
}
