/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/objArrayKlass.inline.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/symbol.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "utilities/macros.hpp"

ObjArrayKlass* ObjArrayKlass::allocate(ClassLoaderData* loader_data, int n, Klass* k, Symbol* name, TRAPS) {
  assert(ObjArrayKlass::header_size() <= InstanceKlass::header_size(),
      "array klasses must be same size as InstanceKlass");

  int size = ArrayKlass::static_size(ObjArrayKlass::header_size());

  return new (loader_data, size, THREAD) ObjArrayKlass(n, k, name);
}

Klass* ObjArrayKlass::allocate_objArray_klass(int n, Klass* element_klass, TRAPS) {
  // Eagerly allocate the direct array supertype.
  Klass* super_klass = NULL;
  if (!Universe::is_bootstrapping() || SystemDictionary::Object_klass_loaded()) {
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
          ek = element_klass->array_klass(n, CHECK_NULL);
        }  // re-lock
        return ek;
      }
    } else {
      // The element type is already Object.  Object[] has direct super of Object.
      super_klass = SystemDictionary::Object_klass();
    }
  }

  // Create type name for klass.
  Symbol* name = ArrayKlass::create_element_klass_array_name(element_klass, CHECK_NULL);

  // Initialize instance variables
  ClassLoaderData* loader_data = element_klass->class_loader_data();
  ObjArrayKlass* oak = ObjArrayKlass::allocate(loader_data, n, element_klass, name, CHECK_NULL);

  ModuleEntry* module = oak->module();
  assert(module != NULL, "No module entry for array");

  // Call complete_create_array_klass after all instance variables has been initialized.
  ArrayKlass::complete_create_array_klass(oak, super_klass, module, CHECK_NULL);

  // Add all classes to our internal class loader list here,
  // including classes in the bootstrap (NULL) class loader.
  // Do this step after creating the mirror so that if the
  // mirror creation fails, loaded_classes_do() doesn't find
  // an array class without a mirror.
  loader_data->add_class(oak);

  return oak;
}

ObjArrayKlass::ObjArrayKlass(int n, Klass* element_klass, Symbol* name) : ArrayKlass(name, ID) {
  this->set_dimension(n);
  this->set_element_klass(element_klass);
  // decrement refcount because object arrays are not explicitly freed.  The
  // InstanceKlass array_name() keeps the name counted while the klass is
  // loaded.
  name->decrement_refcount();

  Klass* bk;
  if (element_klass->is_objArray_klass()) {
    bk = ObjArrayKlass::cast(element_klass)->bottom_klass();
  } else if (element_klass->is_valueArray_klass()) {
    bk = ValueArrayKlass::cast(element_klass)->element_klass();
  } else {
    bk = element_klass;
  }
  assert(bk != NULL && (bk->is_instance_klass()
      || bk->is_typeArray_klass()), "invalid bottom klass");
  this->set_bottom_klass(bk);
  this->set_class_loader_data(bk->class_loader_data());

  this->set_layout_helper(array_layout_helper(T_OBJECT));
  assert(this->is_array_klass(), "sanity");
  assert(this->is_objArray_klass(), "sanity");
}

int ObjArrayKlass::oop_size(oop obj) const {
  assert(obj->is_objArray(), "must be object array");
  return objArrayOop(obj)->object_size();
}

objArrayOop ObjArrayKlass::allocate(int length, TRAPS) {
  check_array_allocation_length(length, arrayOopDesc::max_array_length(T_OBJECT), CHECK_NULL);
  int size = objArrayOopDesc::object_size(length);
  bool populate_null_free = storage_properties().is_null_free();
  objArrayOop array =  (objArrayOop)Universe::heap()->array_allocate(this, size, length,
                                                       /* do_zero */ true, THREAD);
  if (populate_null_free) {
    assert(dimension() == 1, "Can only populate the final dimension");
    assert(element_klass()->is_value(), "Unexpected");
    assert(!element_klass()->is_array_klass(), "ArrayKlass unexpected here");
    assert(!ValueKlass::cast(element_klass())->flatten_array(), "Expected valueArrayOop allocation");
    element_klass()->initialize(CHECK_NULL);
    // Populate default values...
    objArrayHandle array_h(THREAD, array);
    instanceOop value = (instanceOop) ValueKlass::cast(element_klass())->default_value();
    for (int i = 0; i < length; i++) {
      array_h->obj_at_put(i, value);
    }
  }
  return array;
}

oop ObjArrayKlass::multi_allocate(int rank, jint* sizes, TRAPS) {
  int length = *sizes;
  if (rank == 1) { // last dim may be valueArray, check if we have any special storage requirements
    if ((!element_klass()->is_array_klass()) && storage_properties().is_null_free()) {
      return oopFactory::new_valueArray(element_klass(), length, CHECK_NULL);
    } else {
      return oopFactory::new_objArray(element_klass(), length, CHECK_NULL);
    }
  }
  guarantee(rank > 1, "Rank below 1");
  // Call to lower_dimension uses this pointer, so most be called before a
  // possible GC
  Klass* ld_klass = lower_dimension();
  // If length < 0 allocate will throw an exception.
  objArrayOop array = allocate(length, CHECK_NULL);
  objArrayHandle h_array (THREAD, array);
  if (length != 0) {
    for (int index = 0; index < length; index++) {
      ArrayKlass* ak = ArrayKlass::cast(ld_klass);
      oop sub_array = ak->multi_allocate(rank-1, &sizes[1], CHECK_NULL);
      h_array->obj_at_put(index, sub_array);
    }
  } else {
    // Since this array dimension has zero length, nothing will be
    // allocated, however the lower dimension values must be checked
    // for illegal values.
    for (int i = 0; i < rank - 1; ++i) {
      sizes += 1;
      if (*sizes < 0) {
        THROW_MSG_0(vmSymbols::java_lang_NegativeArraySizeException(), err_msg("%d", *sizes));
      }
    }
  }
  return h_array();
}

ArrayStorageProperties ObjArrayKlass::storage_properties() {
  return name()->is_Q_singledim_array_signature() ? ArrayStorageProperties::null_free : ArrayStorageProperties::empty;
}

// Either oop or narrowOop depending on UseCompressedOops.
void ObjArrayKlass::do_copy(arrayOop s, size_t src_offset,
                            arrayOop d, size_t dst_offset, int length, TRAPS) {
  if (s == d) {
    // since source and destination are equal we do not need conversion checks.
    assert(length > 0, "sanity check");
    ArrayAccess<>::oop_arraycopy(s, src_offset, d, dst_offset, length);
  } else {
    // We have to make sure all elements conform to the destination array
    Klass* bound = ObjArrayKlass::cast(d->klass())->element_klass();
    Klass* stype = ObjArrayKlass::cast(s->klass())->element_klass();
    // Perform null check if dst is null-free but src has no such guarantee
    bool null_check = ((!ArrayKlass::cast(s->klass())->storage_properties().is_null_free()) &&
        ArrayKlass::cast(d->klass())->storage_properties().is_null_free());
    if (stype == bound || stype->is_subtype_of(bound)) {
      if (null_check) {
        ArrayAccess<ARRAYCOPY_DISJOINT | ARRAYCOPY_NOTNULL>::oop_arraycopy(s, src_offset, d, dst_offset, length);
      } else {
        ArrayAccess<ARRAYCOPY_DISJOINT>::oop_arraycopy(s, src_offset, d, dst_offset, length);
      }
    } else {
      if (null_check) {
        ArrayAccess<ARRAYCOPY_DISJOINT | ARRAYCOPY_CHECKCAST | ARRAYCOPY_NOTNULL>::oop_arraycopy(s, src_offset, d, dst_offset, length);
      } else {
        ArrayAccess<ARRAYCOPY_DISJOINT | ARRAYCOPY_CHECKCAST>::oop_arraycopy(s, src_offset, d, dst_offset, length);
      }
    }
  }
}

void ObjArrayKlass::copy_array(arrayOop s, int src_pos, arrayOop d,
                               int dst_pos, int length, TRAPS) {
  assert(s->is_objArray(), "must be obj array");

  if (EnableValhalla) {
    if (d->is_valueArray()) {
      ValueArrayKlass::cast(d->klass())->copy_array(s, src_pos, d, dst_pos, length, THREAD);
      return;
    }
  }

  if (!d->is_objArray()) {
    ResourceMark rm(THREAD);
    stringStream ss;
    if (d->is_typeArray()) {
      ss.print("arraycopy: type mismatch: can not copy object array[] into %s[]",
               type2name_tab[ArrayKlass::cast(d->klass())->element_type()]);
    } else {
      ss.print("arraycopy: destination type %s is not an array", d->klass()->external_name());
    }
    THROW_MSG(vmSymbols::java_lang_ArrayStoreException(), ss.as_string());
  }

  // Check is all offsets and lengths are non negative
  if (src_pos < 0 || dst_pos < 0 || length < 0) {
    // Pass specific exception reason.
    ResourceMark rm(THREAD);
    stringStream ss;
    if (src_pos < 0) {
      ss.print("arraycopy: source index %d out of bounds for object array[%d]",
               src_pos, s->length());
    } else if (dst_pos < 0) {
      ss.print("arraycopy: destination index %d out of bounds for object array[%d]",
               dst_pos, d->length());
    } else {
      ss.print("arraycopy: length %d is negative", length);
    }
    THROW_MSG(vmSymbols::java_lang_ArrayIndexOutOfBoundsException(), ss.as_string());
  }
  // Check if the ranges are valid
  if ((((unsigned int) length + (unsigned int) src_pos) > (unsigned int) s->length()) ||
      (((unsigned int) length + (unsigned int) dst_pos) > (unsigned int) d->length())) {
    // Pass specific exception reason.
    ResourceMark rm(THREAD);
    stringStream ss;
    if (((unsigned int) length + (unsigned int) src_pos) > (unsigned int) s->length()) {
      ss.print("arraycopy: last source index %u out of bounds for object array[%d]",
               (unsigned int) length + (unsigned int) src_pos, s->length());
    } else {
      ss.print("arraycopy: last destination index %u out of bounds for object array[%d]",
               (unsigned int) length + (unsigned int) dst_pos, d->length());
    }
    THROW_MSG(vmSymbols::java_lang_ArrayIndexOutOfBoundsException(), ss.as_string());
  }

  // Special case. Boundary cases must be checked first
  // This allows the following call: copy_array(s, s.length(), d.length(), 0).
  // This is correct, since the position is supposed to be an 'in between point', i.e., s.length(),
  // points to the right of the last element.
  if (length==0) {
    return;
  }
  if (UseCompressedOops) {
    size_t src_offset = (size_t) objArrayOopDesc::obj_at_offset<narrowOop>(src_pos);
    size_t dst_offset = (size_t) objArrayOopDesc::obj_at_offset<narrowOop>(dst_pos);
    assert(arrayOopDesc::obj_offset_to_raw<narrowOop>(s, src_offset, NULL) ==
           objArrayOop(s)->obj_at_addr_raw<narrowOop>(src_pos), "sanity");
    assert(arrayOopDesc::obj_offset_to_raw<narrowOop>(d, dst_offset, NULL) ==
           objArrayOop(d)->obj_at_addr_raw<narrowOop>(dst_pos), "sanity");
    do_copy(s, src_offset, d, dst_offset, length, CHECK);
  } else {
    size_t src_offset = (size_t) objArrayOopDesc::obj_at_offset<oop>(src_pos);
    size_t dst_offset = (size_t) objArrayOopDesc::obj_at_offset<oop>(dst_pos);
    assert(arrayOopDesc::obj_offset_to_raw<oop>(s, src_offset, NULL) ==
           objArrayOop(s)->obj_at_addr_raw<oop>(src_pos), "sanity");
    assert(arrayOopDesc::obj_offset_to_raw<oop>(d, dst_offset, NULL) ==
           objArrayOop(d)->obj_at_addr_raw<oop>(dst_pos), "sanity");
    do_copy(s, src_offset, d, dst_offset, length, CHECK);
  }
}


Klass* ObjArrayKlass::array_klass_impl(bool or_null, int n, TRAPS) {
  assert(dimension() <= n, "check order of chain");
  int dim = dimension();
  if (dim == n) return this;

  // lock-free read needs acquire semantics
  if (higher_dimension_acquire() == NULL) {
    if (or_null) return NULL;

    ResourceMark rm;
    {
      // Ensure atomic creation of higher dimensions
      MutexLocker mu(THREAD, MultiArray_lock);

      // Check if another thread beat us
      if (higher_dimension() == NULL) {

        // Create multi-dim klass object and link them together
        Klass* k = ObjArrayKlass::allocate_objArray_klass(dim + 1, this, CHECK_NULL);
        ObjArrayKlass* ak = ObjArrayKlass::cast(k);
        ak->set_lower_dimension(this);
        // use 'release' to pair with lock-free load
        release_set_higher_dimension(ak);
        assert(ak->is_objArray_klass(), "incorrect initialization of ObjArrayKlass");
      }
    }
  }

  ObjArrayKlass *ak = ObjArrayKlass::cast(higher_dimension());
  if (or_null) {
    return ak->array_klass_or_null(n);
  }
  THREAD->check_possible_safepoint();
  return ak->array_klass(n, THREAD);
}

Klass* ObjArrayKlass::array_klass_impl(bool or_null, TRAPS) {
  return array_klass_impl(or_null, dimension() +  1, THREAD);
}

bool ObjArrayKlass::can_be_primary_super_slow() const {
  if (!bottom_klass()->can_be_primary_super())
    // array of interfaces
    return false;
  else
    return Klass::can_be_primary_super_slow();
}

GrowableArray<Klass*>* ObjArrayKlass::compute_secondary_supers(int num_extra_slots,
                                                               Array<InstanceKlass*>* transitive_interfaces) {
  assert(transitive_interfaces == NULL, "sanity");
  // interfaces = { cloneable_klass, serializable_klass, elemSuper[], ... };
  const Array<Klass*>* elem_supers = element_klass()->secondary_supers();
  int num_elem_supers = elem_supers == NULL ? 0 : elem_supers->length();
  int num_secondaries = num_extra_slots + 2 + num_elem_supers;
  if (num_secondaries == 2) {
    // Must share this for correct bootstrapping!
    set_secondary_supers(Universe::the_array_interfaces_array());
    return NULL;
  } else {
    GrowableArray<Klass*>* secondaries = new GrowableArray<Klass*>(num_elem_supers+2);
    secondaries->push(SystemDictionary::Cloneable_klass());
    secondaries->push(SystemDictionary::Serializable_klass());
    for (int i = 0; i < num_elem_supers; i++) {
      Klass* elem_super = elem_supers->at(i);
      Klass* array_super = elem_super->array_klass_or_null();
      assert(array_super != NULL, "must already have been created");
      secondaries->push(array_super);
    }
    return secondaries;
  }
}

void ObjArrayKlass::initialize(TRAPS) {
  bottom_klass()->initialize(THREAD);  // dispatches to either InstanceKlass or TypeArrayKlass
}

void ObjArrayKlass::metaspace_pointers_do(MetaspaceClosure* it) {
  ArrayKlass::metaspace_pointers_do(it);
  it->push(&_element_klass);
  it->push(&_bottom_klass);
}

// JVM support

jint ObjArrayKlass::compute_modifier_flags(TRAPS) const {
  // The modifier for an objectArray is the same as its element
  if (element_klass() == NULL) {
    assert(Universe::is_bootstrapping(), "partial objArray only at startup");
    return JVM_ACC_ABSTRACT | JVM_ACC_FINAL | JVM_ACC_PUBLIC;
  }
  // Return the flags of the bottom element type.
  jint element_flags = bottom_klass()->compute_modifier_flags(CHECK_0);

  return (element_flags & (JVM_ACC_PUBLIC | JVM_ACC_PRIVATE | JVM_ACC_PROTECTED))
                        | (JVM_ACC_ABSTRACT | JVM_ACC_FINAL);
}

ModuleEntry* ObjArrayKlass::module() const {
  assert(bottom_klass() != NULL, "ObjArrayKlass returned unexpected NULL bottom_klass");
  // The array is defined in the module of its bottom class
  return bottom_klass()->module();
}

PackageEntry* ObjArrayKlass::package() const {
  assert(bottom_klass() != NULL, "ObjArrayKlass returned unexpected NULL bottom_klass");
  return bottom_klass()->package();
}

// Printing

void ObjArrayKlass::print_on(outputStream* st) const {
#ifndef PRODUCT
  Klass::print_on(st);
  st->print(" - element klass: ");
  element_klass()->print_value_on(st);
  st->cr();
#endif //PRODUCT
}

void ObjArrayKlass::print_value_on(outputStream* st) const {
  assert(is_klass(), "must be klass");

  element_klass()->print_value_on(st);
  st->print("[]");
}

#ifndef PRODUCT

void ObjArrayKlass::oop_print_on(oop obj, outputStream* st) {
  ArrayKlass::oop_print_on(obj, st);
  assert(obj->is_objArray(), "must be objArray");
  objArrayOop oa = objArrayOop(obj);
  int print_len = MIN2((intx) oa->length(), MaxElementPrintSize);
  for(int index = 0; index < print_len; index++) {
    st->print(" - %3d : ", index);
    if (oa->obj_at(index) != NULL) {
      oa->obj_at(index)->print_value_on(st);
      st->cr();
    } else {
      st->print_cr("NULL");
    }
  }
  int remaining = oa->length() - print_len;
  if (remaining > 0) {
    st->print_cr(" - <%d more elements, increase MaxElementPrintSize to print>", remaining);
  }
}

#endif //PRODUCT

void ObjArrayKlass::oop_print_value_on(oop obj, outputStream* st) {
  assert(obj->is_objArray(), "must be objArray");
  st->print("a ");
  element_klass()->print_value_on(st);
  int len = objArrayOop(obj)->length();
  st->print("[%d] ", len);
  if (obj != NULL) {
    obj->print_address_on(st);
  } else {
    st->print_cr("NULL");
  }
}

const char* ObjArrayKlass::internal_name() const {
  return external_name();
}


// Verification

void ObjArrayKlass::verify_on(outputStream* st) {
  ArrayKlass::verify_on(st);
  guarantee(element_klass()->is_klass(), "should be klass");
  guarantee(bottom_klass()->is_klass(), "should be klass");
  Klass* bk = bottom_klass();
  guarantee(bk->is_instance_klass() || bk->is_typeArray_klass() || bk->is_valueArray_klass(),
            "invalid bottom klass");
}

void ObjArrayKlass::oop_verify_on(oop obj, outputStream* st) {
  ArrayKlass::oop_verify_on(obj, st);
  guarantee(obj->is_objArray(), "must be objArray");
  objArrayOop oa = objArrayOop(obj);
  for(int index = 0; index < oa->length(); index++) {
    guarantee(oopDesc::is_oop_or_null(oa->obj_at(index)), "should be oop");
  }
}
