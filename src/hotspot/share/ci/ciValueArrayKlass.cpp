/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciInstanceKlass.hpp"
#include "ci/ciValueArrayKlass.hpp"
#include "ci/ciValueKlass.hpp"
#include "ci/ciSymbol.hpp"
#include "ci/ciUtilities.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "oops/valueArrayKlass.hpp"

// ciValueArrayKlass
//
// This class represents a Klass* in the HotSpot virtual machine
// whose Klass part is a ValueArrayKlass.

// ------------------------------------------------------------------
// ciValueArrayKlass::ciValueArrayKlass
//
// Constructor for loaded value array klasses.
ciValueArrayKlass::ciValueArrayKlass(Klass* h_k) : ciArrayKlass(h_k) {
  assert(get_Klass()->is_valueArray_klass(), "wrong type");
  ValueKlass* element_Klass = get_ValueArrayKlass()->element_klass();
  _base_element_klass = CURRENT_ENV->get_klass(element_Klass);
  assert(_base_element_klass->is_valuetype(), "bad base klass");
  if (dimension() == 1) {
    _element_klass = _base_element_klass;
  } else {
    _element_klass = NULL;
  }
  if (!ciObjectFactory::is_initialized()) {
    assert(_element_klass->is_java_lang_Object(), "only arrays of object are shared");
  }
}

ciValueArrayKlass::ciValueArrayKlass(ciSymbol* array_name,
                                     ciValueKlass* base_element_klass,
                                     int dimension)
  : ciArrayKlass(array_name, dimension, T_VALUETYPE) {
  _base_element_klass = base_element_klass;
  _element_klass = base_element_klass;
}

// ------------------------------------------------------------------
// ciValueArrayKlass::element_klass
//
// What is the one-level element type of this array?
ciKlass* ciValueArrayKlass::element_klass() {
  if (_element_klass == NULL) {
    assert(dimension() > 1, "_element_klass should not be NULL");
    assert(is_loaded(), "valueArrayKlass must be loaded");
    // Produce the element klass.
    VM_ENTRY_MARK;
    Klass* element_Klass = get_ValueArrayKlass()->element_klass();
    _element_klass = CURRENT_THREAD_ENV->get_klass(element_Klass);
  }
  return _element_klass;
}

// ------------------------------------------------------------------
// ciValueArrayKlass::construct_array_name
//
// Build an array name from an element name and a dimension.
ciSymbol* ciValueArrayKlass::construct_array_name(ciSymbol* element_name,
                                                  int dimension) {
  EXCEPTION_CONTEXT;
  int element_len = element_name->utf8_length();

  Symbol* base_name_sym = element_name->get_symbol();
  char* name;

  if (base_name_sym->char_at(0) == JVM_SIGNATURE_ARRAY ||
      (base_name_sym->char_at(0) == JVM_SIGNATURE_CLASS &&  // watch package name 'Lxx'
       base_name_sym->char_at(element_len-1) == JVM_SIGNATURE_ENDCLASS)) {

    int new_len = element_len + dimension + 1; // for the ['s and '\0'
    name = CURRENT_THREAD_ENV->name_buffer(new_len);

    int pos = 0;
    for ( ; pos < dimension; pos++) {
      name[pos] = JVM_SIGNATURE_ARRAY;
    }
    strncpy(name+pos, (char*)element_name->base(), element_len);
    name[new_len-1] = '\0';
  } else {
    int new_len =   3                       // for L, ;, and '\0'
                  + dimension               // for ['s
                  + element_len;

    name = CURRENT_THREAD_ENV->name_buffer(new_len);
    int pos = 0;
    for ( ; pos < dimension; pos++) {
      name[pos] = JVM_SIGNATURE_ARRAY;
    }
    name[pos++] = JVM_SIGNATURE_VALUETYPE;
    strncpy(name+pos, (char*)element_name->base(), element_len);
    name[new_len-2] = JVM_SIGNATURE_ENDCLASS;
    name[new_len-1] = '\0';
  }
  return ciSymbol::make(name);
}

// ------------------------------------------------------------------
// ciValueArrayKlass::make_impl
//
// Implementation of make.
ciValueArrayKlass* ciValueArrayKlass::make_impl(ciKlass* element_klass) {
  assert(ValueArrayFlatten, "should only be used for flattened value type arrays");
  assert(element_klass->is_valuetype(), "element type must be value type");
  assert(element_klass->is_loaded(), "unloaded Q klasses are represented by ciInstanceKlass");
  {
    EXCEPTION_CONTEXT;
    // The element klass is loaded
    Klass* array = element_klass->get_Klass()->array_klass(THREAD);
    if (HAS_PENDING_EXCEPTION) {
      CLEAR_PENDING_EXCEPTION;
      CURRENT_THREAD_ENV->record_out_of_memory_failure();
      // TODO handle this
      guarantee(false, "out of memory");
      return NULL;
    }
    return CURRENT_THREAD_ENV->get_value_array_klass(array);
  }
}

// ------------------------------------------------------------------
// ciValueArrayKlass::make
//
// Make an array klass corresponding to the specified primitive type.
ciValueArrayKlass* ciValueArrayKlass::make(ciKlass* element_klass) {
  GUARDED_VM_ENTRY(return make_impl(element_klass);)
}

ciKlass* ciValueArrayKlass::exact_klass() {
  assert(element_klass()->is_loaded() && element_klass()->as_value_klass()->exact_klass() != NULL, "must have exact klass");
  return this;
}
