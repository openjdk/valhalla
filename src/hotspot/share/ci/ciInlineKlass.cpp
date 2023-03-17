/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciEnv.hpp"
#include "ci/ciField.hpp"
#include "ci/ciInlineKlass.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "oops/inlineKlass.inline.hpp"

int ciInlineKlass::compute_nonstatic_fields() {
  int result = ciInstanceKlass::compute_nonstatic_fields();
  assert(super() == NULL || !super()->has_nonstatic_fields(), "an inline type must not inherit fields from its superclass");

  // Compute declared non-static fields (without flattening of inline type fields)
  GrowableArray<ciField*>* fields = NULL;
  GUARDED_VM_ENTRY(fields = compute_nonstatic_fields_impl(NULL, false /* no flattening */);)
  Arena* arena = CURRENT_ENV->arena();
  _declared_nonstatic_fields = (fields != NULL) ? fields : new (arena) GrowableArray<ciField*>(arena, 0, 0, 0);
  return result;
}

// Offset of the first field in the inline type
int ciInlineKlass::first_field_offset() const {
  GUARDED_VM_ENTRY(return to_InlineKlass()->first_field_offset();)
}

// Returns the index of the field with the given offset. If the field at 'offset'
// belongs to a flattened inline type field, return the index of the field
// in the flattened inline type.
int ciInlineKlass::field_index_by_offset(int offset) {
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
      // flattened inline type field that holds the field we are looking for.
      best_offset = field_offset;
      best_index = i;
    }
  }
  assert(best_index >= 0, "field not found");
  assert(best_offset == offset || _declared_nonstatic_fields->at(best_index)->type()->is_inlinetype(), "offset should match for non-inline types");
  return best_index;
}

// Are arrays containing this inline type flattened?
bool ciInlineKlass::flatten_array() const {
  GUARDED_VM_ENTRY(return to_InlineKlass()->flatten_array();)
}

// Can this inline type be passed as multiple values?
bool ciInlineKlass::can_be_passed_as_fields() const {
  GUARDED_VM_ENTRY(return !VectorSupport::skip_value_scalarization(const_cast<ciInlineKlass*>(this)) && to_InlineKlass()->can_be_passed_as_fields();)
}

// Can this inline type be returned as multiple values?
bool ciInlineKlass::can_be_returned_as_fields() const {
  GUARDED_VM_ENTRY(return !VectorSupport::skip_value_scalarization(const_cast<ciInlineKlass*>(this)) && to_InlineKlass()->can_be_returned_as_fields();)
}

bool ciInlineKlass::is_empty() {
  // Do not use InlineKlass::is_empty_inline_type here because it does
  // consider the container empty even if fields of empty inline types
  // are not flattened
  return nof_nonstatic_fields() == 0;
}

// When passing an inline type's fields as arguments, count the number
// of argument slots that are needed
int ciInlineKlass::inline_arg_slots() {
  int slots = 0;
  for (int j = 0; j < nof_nonstatic_fields(); j++) {
    ciField* field = nonstatic_field_at(j);
    slots += type2size[field->type()->basic_type()];
  }
  return slots;
}

// Offset of the default oop in the mirror
int ciInlineKlass::default_value_offset() const {
  GUARDED_VM_ENTRY(return to_InlineKlass()->default_value_offset();)
}

ciInstance* ciInlineKlass::default_instance() const {
  GUARDED_VM_ENTRY(
    oop default_value = to_InlineKlass()->default_value();
    return CURRENT_ENV->get_instance(default_value);
  )
}

ciInstance* ciInlineKlass::ref_instance() const {
  GUARDED_VM_ENTRY(
    oop ref_mirror = to_InlineKlass()->ref_mirror();
    return CURRENT_ENV->get_instance(ref_mirror);
  )
}

ciInstance* ciInlineKlass::val_instance() const {
  GUARDED_VM_ENTRY(
    oop val_mirror = to_InlineKlass()->val_mirror();
    return CURRENT_ENV->get_instance(val_mirror);
  )
}

bool ciInlineKlass::contains_oops() const {
  GUARDED_VM_ENTRY(return get_InlineKlass()->contains_oops();)
}

int ciInlineKlass::oop_count() const {
  GUARDED_VM_ENTRY(return get_InlineKlass()->nonstatic_oop_count();)
}

address ciInlineKlass::pack_handler() const {
  GUARDED_VM_ENTRY(return get_InlineKlass()->pack_handler();)
}

address ciInlineKlass::unpack_handler() const {
  GUARDED_VM_ENTRY(return get_InlineKlass()->unpack_handler();)
}

InlineKlass* ciInlineKlass::get_InlineKlass() const {
  GUARDED_VM_ENTRY(return to_InlineKlass();)
}

ciInstance* ciInlineKlass::ref_mirror() {
  GUARDED_VM_ENTRY(return CURRENT_ENV->get_instance(to_InlineKlass()->ref_mirror());)
}

ciInstance* ciInlineKlass::val_mirror() {
  GUARDED_VM_ENTRY(return CURRENT_ENV->get_instance(to_InlineKlass()->val_mirror());)
}
