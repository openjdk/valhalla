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

#include "ci/ciField.hpp"
#include "ci/ciInlineKlass.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "oops/inlineKlass.inline.hpp"

int ciInlineKlass::compute_nonstatic_fields() {
  int result = ciInstanceKlass::compute_nonstatic_fields();

  // Abstract value classes can also have declared fields.
  ciInstanceKlass* super_klass = super();
  GrowableArray<ciField*>* super_klass_fields = nullptr;
  if (super_klass != nullptr && super_klass->has_nonstatic_fields()) {
    int super_flen = super_klass->nof_nonstatic_fields();
    super_klass_fields = super_klass->_nonstatic_fields;
    assert(super_flen == 0 || super_klass_fields != nullptr, "first get nof_fields");
  }

  // Compute declared non-static fields (without flattening of inline type fields)
  GrowableArray<ciField*>* fields = nullptr;
  GUARDED_VM_ENTRY(fields = compute_nonstatic_fields_impl(super_klass_fields, false /* no flattening */);)
  Arena* arena = CURRENT_ENV->arena();
  _declared_nonstatic_fields = (fields != nullptr) ? fields : new (arena) GrowableArray<ciField*>(arena, 0, 0, 0);
  return result;
}

// Offset of the first field in the inline type
int ciInlineKlass::payload_offset() const {
  GUARDED_VM_ENTRY(return to_InlineKlass()->payload_offset();)
}

// Returns the index of the field with the given offset. If the field at 'offset'
// belongs to a flat field, return the index of the field in the inline type of the flat field.
int ciInlineKlass::field_index_by_offset(int offset) {
  assert(contains_field_offset(offset), "invalid field offset");
  int best_offset = 0;
  int best_index = -1;
  // Search the field with the given offset
  for (int i = 0; i < nof_declared_nonstatic_fields(); ++i) {
    int field_offset = _declared_nonstatic_fields->at(i)->offset_in_bytes();
    if (field_offset == offset) {
      // Exact match
      return i;
    } else if (field_offset < offset && field_offset > best_offset) {
      // No exact match. Save the index of the field with the closest offset that
      // is smaller than the given field offset. This index corresponds to the
      // flat field that holds the field we are looking for.
      best_offset = field_offset;
      best_index = i;
    }
  }
  assert(best_index >= 0, "field not found");
  assert(best_offset == offset || _declared_nonstatic_fields->at(best_index)->type()->is_inlinetype(), "offset should match for non-inline types");
  return best_index;
}

// Are arrays containing this inline type flat arrays?
bool ciInlineKlass::flat_in_array() const {
  GUARDED_VM_ENTRY(return to_InlineKlass()->flat_array();)
}

// Can this inline type be passed as multiple values?
bool ciInlineKlass::can_be_passed_as_fields() const {
  GUARDED_VM_ENTRY(return to_InlineKlass()->can_be_passed_as_fields();)
}

// Can this inline type be returned as multiple values?
bool ciInlineKlass::can_be_returned_as_fields() const {
  GUARDED_VM_ENTRY(return to_InlineKlass()->can_be_returned_as_fields();)
}

bool ciInlineKlass::is_empty() {
  // Do not use InlineKlass::is_empty_inline_type here because it does
  // consider the container empty even if fields of empty inline types
  // are not flat
  return nof_declared_nonstatic_fields() == 0;
}

// When passing an inline type's fields as arguments, count the number
// of argument slots that are needed
int ciInlineKlass::inline_arg_slots() {
  VM_ENTRY_MARK;
  const Array<SigEntry>* sig_vk = get_InlineKlass()->extended_sig();
  int slots = 0;
  for (int i = 0; i < sig_vk->length(); i++) {
    BasicType bt = sig_vk->at(i)._bt;
    if (bt == T_METADATA || bt == T_VOID) {
      continue;
    }
    slots += type2size[bt];
  }
  return slots;
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

bool ciInlineKlass::has_non_atomic_layout() const {
  GUARDED_VM_ENTRY(return get_InlineKlass()->has_non_atomic_layout();)
}

bool ciInlineKlass::has_atomic_layout() const {
  GUARDED_VM_ENTRY(return get_InlineKlass()->has_atomic_layout();)
}

bool ciInlineKlass::has_nullable_atomic_layout() const {
  GUARDED_VM_ENTRY(return get_InlineKlass()->has_nullable_atomic_layout();)
}

int ciInlineKlass::null_marker_offset_in_payload() const {
  GUARDED_VM_ENTRY(return get_InlineKlass()->null_marker_offset_in_payload();)
}

// Convert size of atomic layout in bytes to corresponding BasicType
BasicType ciInlineKlass::atomic_size_to_basic_type(bool null_free) const {
  VM_ENTRY_MARK
  InlineKlass* vk = get_InlineKlass();
  assert(!null_free || vk->has_atomic_layout(), "No null-free atomic layout available");
  assert( null_free || vk->has_nullable_atomic_layout(), "No nullable atomic layout available");
  int size = null_free ? vk->atomic_size_in_bytes() : vk->nullable_atomic_size_in_bytes();
  BasicType bt;
  if (size == sizeof(jlong)) {
    bt = T_LONG;
  } else if (size == sizeof(jint)) {
    bt = T_INT;
  } else if (size == sizeof(jshort)) {
    bt = T_SHORT;
  } else if (size == sizeof(jbyte)) {
    bt = T_BYTE;
  } else {
    assert(false, "Unsupported size: %d", size);
  }
  return bt;
}
