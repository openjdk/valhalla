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
#include "ci/ciField.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "ci/ciValueKlass.hpp"
#include "oops/valueKlass.hpp"

int ciValueKlass::compute_nonstatic_fields() {
  int result = ciInstanceKlass::compute_nonstatic_fields();
  assert(super() == NULL || !super()->has_nonstatic_fields(), "a value type must not inherit fields from its superclass");

  // Compute declared non-static fields (without flattening of value type fields)
  GrowableArray<ciField*>* fields = NULL;
  GUARDED_VM_ENTRY(fields = compute_nonstatic_fields_impl(NULL, false /* no flattening */);)
  Arena* arena = CURRENT_ENV->arena();
  _declared_nonstatic_fields = (fields != NULL) ? fields : new (arena) GrowableArray<ciField*>(arena, 0, 0, 0);
  return result;
}

// Offset of the first field in the value type
int ciValueKlass::first_field_offset() const {
  GUARDED_VM_ENTRY(return ValueKlass::cast(get_Klass())->first_field_offset();)
}

// Returns the index of the field with the given offset. If the field at 'offset'
// belongs to a flattened value type field, return the index of the field
// in the flattened value type.
int ciValueKlass::field_index_by_offset(int offset) {
  assert(contains_field_offset(offset), "invalid field offset");
  int best_offset = 0;
  int best_index = -1;
  // Search the field with the given offset
  for (int i = 0; i < nof_declared_nonstatic_fields(); ++i) {
    int field_offset = _declared_nonstatic_fields->at(i)->offset();
    if (field_offset == offset) {
      // Exact match
      return i;
    } else if (field_offset < offset && field_offset > best_offset) {
      // No exact match. Save the index of the field with the closest offset that
      // is smaller than the given field offset. This index corresponds to the
      // flattened value type field that holds the field we are looking for.
      best_offset = field_offset;
      best_index = i;
    }
  }
  assert(best_index >= 0, "field not found");
  assert(best_offset == offset || _declared_nonstatic_fields->at(best_index)->type()->is_valuetype(), "offset should match for non-VTs");
  return best_index;
}

// Are arrays containing this value type flattened?
bool ciValueKlass::flatten_array() const {
  GUARDED_VM_ENTRY(return ValueKlass::cast(get_Klass())->flatten_array();)
}

// Can this value type be returned as multiple values?
bool ciValueKlass::can_be_returned_as_fields() const {
  GUARDED_VM_ENTRY(return ValueKlass::cast(get_Klass())->can_be_returned_as_fields();)
}

// Can this value type be returned as multiple values?
bool ciValueKlass::is_bufferable() const {
  GUARDED_VM_ENTRY(return ValueKlass::cast(get_Klass())->is_bufferable();)
}

// When passing a value type's fields as arguments, count the number
// of argument slots that are needed
int ciValueKlass::value_arg_slots() {
  int slots = nof_nonstatic_fields();
  for (int j = 0; j < nof_nonstatic_fields(); j++) {
    ciField* f = nonstatic_field_at(j);
    BasicType bt = f->type()->basic_type();
    if (bt == T_LONG || bt == T_DOUBLE) {
      slots++;
    }
  }
  return slots;
}

ciInstance* ciValueKlass::default_value_instance() const {
  GUARDED_VM_ENTRY(
    oop default_value = ValueKlass::cast(get_Klass())->default_value();
    return CURRENT_ENV->get_instance(default_value);
  )
}

bool ciValueKlass::contains_oops() const {
  GUARDED_VM_ENTRY(return ValueKlass::cast(get_Klass())->contains_oops();)
}
