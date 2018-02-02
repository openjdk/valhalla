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
#include "classfile/moduleEntry.hpp"
#include "classfile/packageEntry.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "memory/metadataFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "memory/universe.inline.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/oop.inline.hpp"
#include "oops/arrayKlass.hpp"
#include "oops/arrayOop.hpp"
#include "oops/valueKlass.hpp"
#include "oops/valueArrayOop.hpp"
#include "oops/verifyOopClosure.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/orderAccess.inline.hpp"
#include "utilities/macros.hpp"

#include "oops/valueArrayKlass.hpp"

// Allocation...

ValueArrayKlass::ValueArrayKlass(Klass* element_klass, Symbol* name) : ArrayKlass(name) {
  assert(element_klass->is_value(), "Expected Value");

  set_element_klass(ValueKlass::cast(element_klass));
  set_class_loader_data(element_klass->class_loader_data());
  set_layout_helper(array_layout_helper(ValueKlass::cast(element_klass)));

  assert(is_array_klass(), "sanity");
  assert(is_valueArray_klass(), "sanity");

  CMH("tweak name symbol refcnt ?")
#ifndef PRODUCT
  if (PrintValueArrayLayout) {
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

ValueArrayKlass* ValueArrayKlass::allocate_klass(Klass*  element_klass,
                                                 Symbol* name,
                                                 TRAPS) {
  assert(ValueArrayFlatten, "Flatten array required");
  assert(ValueKlass::cast(element_klass)->is_atomic() || (!ValueArrayAtomicAccess), "Atomic by-default");

  ClassLoaderData* loader_data = element_klass->class_loader_data();
  int size = ArrayKlass::static_size(ValueArrayKlass::header_size());
  ValueArrayKlass* vak = new (loader_data, size, THREAD) ValueArrayKlass(element_klass, name);
  if (vak == NULL) {
    return NULL;
  }
  loader_data->add_class(vak);
  complete_create_array_klass(vak, vak->super(), vak->module(), CHECK_NULL);
  return vak;
}

ValueArrayKlass* ValueArrayKlass::allocate_klass(Klass* element_klass, TRAPS) {
  Symbol* name = ArrayKlass::create_element_klass_array_name(element_klass, CHECK_NULL);
  return allocate_klass(element_klass, name, THREAD);
}

void ValueArrayKlass::initialize(TRAPS) {
  element_klass()->initialize(THREAD);
}

// Oops allocation...
oop ValueArrayKlass::allocate(int length, bool do_zero, TRAPS) {
  if (length < 0) {
    THROW_0(vmSymbols::java_lang_NegativeArraySizeException());
  }
  if (length > max_elements()) {
    report_java_out_of_memory("Requested array size exceeds VM limit");
    JvmtiExport::post_array_size_exhausted();
    THROW_OOP_0(Universe::out_of_memory_error_array_size());
  }

  size_t size = valueArrayOopDesc::object_size(layout_helper(), length);
  if (do_zero) {
    return CollectedHeap::array_allocate(this, (int)size, length, CHECK_NULL);
  } else {
    return CollectedHeap::array_allocate_nozero(this, (int)size, length, CHECK_NULL);
  }
}


oop ValueArrayKlass::multi_allocate(int rank, jint* last_size, TRAPS) {
  // For valueArrays this is only called for the last dimension
  assert(rank == 1, "just checking");
  int length = *last_size;
  return allocate(length, true, THREAD);
}

jint ValueArrayKlass::array_layout_helper(ValueKlass* vk) {
  BasicType etype = T_VALUETYPE;
  int atag  = _lh_array_tag_vt_value;
  int esize = upper_log2(vk->raw_value_byte_size());
  int hsize = arrayOopDesc::base_offset_in_bytes(etype);

  int lh = (atag       << _lh_array_tag_shift)
    |      (hsize      << _lh_header_size_shift)
    |      ((int)etype << _lh_element_type_shift)
    |      ((esize)    << _lh_log2_element_size_shift);

  assert(lh < (int)_lh_neutral_value, "must look like an array layout");
  assert(layout_helper_is_array(lh), "correct kind");
  assert(layout_helper_is_valueArray(lh), "correct kind");
  assert(!layout_helper_is_typeArray(lh), "correct kind");
  assert(!layout_helper_is_objArray(lh), "correct kind");
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

jint ValueArrayKlass::max_elements() const {
  return arrayOopDesc::max_array_length(arrayOopDesc::header_size(T_VALUETYPE), element_byte_size());
}

oop ValueArrayKlass::protection_domain() const {
  return element_klass()->protection_domain();
}

void ValueArrayKlass::copy_array(arrayOop s, int src_pos,
                                 arrayOop d, int dst_pos, int length, TRAPS) {
  assert(s->is_valueArray(), "must be value array");

   // Check destination
   if (!d->is_valueArray() || element_klass() != ValueArrayKlass::cast(d->klass())->element_klass()) {
     THROW(vmSymbols::java_lang_ArrayStoreException());
   }

   // Check is all offsets and lengths are non negative
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

   valueArrayOop sa = valueArrayOop(s);
   valueArrayOop da = valueArrayOop(d);
   address src = (address) sa->value_at_addr(src_pos, layout_helper());
   address dst = (address) da->value_at_addr(dst_pos, layout_helper());
   if (contains_oops()) {
     int elem_incr = 1 << log2_element_size();
     address src_end = src + (length << log2_element_size());
     while (src < src_end) {
       element_klass()->value_store(src, dst, element_byte_size(), true, false);
       src += elem_incr;
       dst += elem_incr;
     }
   } else {
     // we are basically a type array...don't bother limiting element copy
     // it would have to be a lot wasted space to be worth value_store() calls, need a setting here ?
     Copy::conjoint_memory_atomic(src, dst, (size_t)length << log2_element_size());
   }
}


Klass* ValueArrayKlass::array_klass_impl(bool or_null, int n, TRAPS) {

  assert(dimension() <= n, "check order of chain");
  int dim = dimension();
  if (dim == n) return this;

  if (higher_dimension() == NULL) {
    if (or_null)  return NULL;

    ResourceMark rm;
    JavaThread *jt = (JavaThread *)THREAD;
    {
      MutexLocker mc(Compile_lock, THREAD);   // for vtables
      // Ensure atomic creation of higher dimensions
      MutexLocker mu(MultiArray_lock, THREAD);

      // Check if another thread beat us
      if (higher_dimension() == NULL) {

        // Create multi-dim klass object and link them together
        Klass* k =
          ObjArrayKlass::allocate_objArray_klass(class_loader_data(), dim + 1, this, CHECK_NULL);
        ObjArrayKlass* ak = ObjArrayKlass::cast(k);
        ak->set_lower_dimension(this);
        OrderAccess::storestore();
        set_higher_dimension(ak);
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
  assert(element_klass() != NULL, "ObjArrayKlass returned unexpected NULL bottom_klass");
  // The array is defined in the module of its bottom class
  return element_klass()->module();
}

PackageEntry* ValueArrayKlass::package() const {
  assert(element_klass() != NULL, "ObjArrayKlass returned unexpected NULL bottom_klass");
  return element_klass()->package();
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
    int off = (address) va->value_at_addr(index, layout_helper()) - (address) obj;
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

void ValueArrayKlass::oop_verify_on(oop obj, outputStream* st) {
  ArrayKlass::oop_verify_on(obj, st);
  guarantee(obj->is_valueArray(), "must be valueArray");

  if (element_klass()->contains_oops()) {
    valueArrayOop va = valueArrayOop(obj);
    NoHeaderExtendedOopClosure wrapClosure(&VerifyOopClosure::verify_oop);
    va->oop_iterate(&wrapClosure);
  }
}

void ValueArrayKlass::verify_on(outputStream* st) {
  ArrayKlass::verify_on(st);
  guarantee(element_klass()->is_value(), "should be value type klass");
}
