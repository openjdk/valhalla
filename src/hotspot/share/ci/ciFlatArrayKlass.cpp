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
#include "ci/ciFlatArrayKlass.hpp"
#include "ci/ciInlineKlass.hpp"
#include "ci/ciInstanceKlass.hpp"
#include "ci/ciObjArrayKlass.hpp"
#include "ci/ciSymbol.hpp"
#include "ci/ciUtilities.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "oops/flatArrayKlass.hpp"
#include "oops/inlineKlass.inline.hpp"

// ciFlatArrayKlass
//
// This class represents a Klass* in the HotSpot virtual machine
// whose Klass part is a FlatArrayKlass.

// ------------------------------------------------------------------
// ciFlatArrayKlass::ciFlatArrayKlass
//
// Constructor for loaded inline type array klasses.
ciFlatArrayKlass::ciFlatArrayKlass(Klass* h_k) : ciArrayKlass(h_k) {
  assert(get_Klass()->is_flatArray_klass(), "wrong type");
  InlineKlass* element_Klass = get_FlatArrayKlass()->element_klass();
  _base_element_klass = CURRENT_ENV->get_klass(element_Klass);
  assert(_base_element_klass->is_inlinetype(), "bad base klass");
  if (dimension() == 1) {
    _element_klass = _base_element_klass;
  } else {
    _element_klass = NULL;
  }
  if (!ciObjectFactory::is_initialized()) {
    assert(_element_klass->is_java_lang_Object(), "only arrays of object are shared");
  }
}

ciFlatArrayKlass::ciFlatArrayKlass(ciSymbol* array_name,
                                     ciInlineKlass* base_element_klass,
                                     int dimension)
  : ciArrayKlass(array_name, dimension, T_INLINE_TYPE) {
  _base_element_klass = base_element_klass;
  _element_klass = base_element_klass;
}

// ------------------------------------------------------------------
// ciFlatArrayKlass::element_klass
//
// What is the one-level element type of this array?
ciKlass* ciFlatArrayKlass::element_klass() {
  if (_element_klass == NULL) {
    assert(dimension() > 1, "_element_klass should not be NULL");
    assert(is_loaded(), "FlatArrayKlass must be loaded");
    // Produce the element klass.
    VM_ENTRY_MARK;
    Klass* element_Klass = get_FlatArrayKlass()->element_klass();
    _element_klass = CURRENT_THREAD_ENV->get_klass(element_Klass);
  }
  return _element_klass;
}

// ------------------------------------------------------------------
// ciFlatArrayKlass::construct_array_name
//
// Build an array name from an element name and a dimension.
ciSymbol* ciFlatArrayKlass::construct_array_name(ciSymbol* element_name,
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
    name[pos++] = JVM_SIGNATURE_INLINE_TYPE;
    strncpy(name+pos, (char*)element_name->base(), element_len);
    name[new_len-2] = JVM_SIGNATURE_ENDCLASS;
    name[new_len-1] = '\0';
  }
  return ciSymbol::make(name);
}

// ------------------------------------------------------------------
// ciFlatArrayKlass::make_impl
//
// Implementation of make.
ciArrayKlass* ciFlatArrayKlass::make_impl(ciKlass* element_klass) {
  assert(UseFlatArray, "should only be used for flat arrays");
  assert(element_klass->is_loaded(), "unloaded inline klasses are represented by ciInstanceKlass");
  assert(element_klass->is_inlinetype(), "element type must be an inline type");
  {
    EXCEPTION_CONTEXT;
    Klass* array = InlineKlass::cast(element_klass->get_Klass())->null_free_inline_array_klass(THREAD);
    if (HAS_PENDING_EXCEPTION) {
      CLEAR_PENDING_EXCEPTION;
      CURRENT_THREAD_ENV->record_out_of_memory_failure();
      // Use unloaded ciObjArrayKlass here because flatArrayKlasses are always loaded
      // and since this is only used for OOM detection, the actual type does not matter.
      return ciEnv::unloaded_ciobjarrayklass();
    }
    return CURRENT_THREAD_ENV->get_flat_array_klass(array);
  }
}

// ------------------------------------------------------------------
// ciFlatArrayKlass::make
//
// Make an array klass corresponding to the specified primitive type.
ciArrayKlass* ciFlatArrayKlass::make(ciKlass* element_klass) {
  GUARDED_VM_ENTRY(return make_impl(element_klass);)
}

ciKlass* ciFlatArrayKlass::exact_klass() {
  assert(element_klass()->is_loaded() && element_klass()->as_inline_klass()->exact_klass() != NULL, "must have exact klass");
  return this;
}
