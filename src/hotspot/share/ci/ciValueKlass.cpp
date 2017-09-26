/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciValueKlass.hpp"
#include "oops/fieldStreams.hpp"
#include "oops/valueKlass.hpp"

int ciValueKlass::compute_field_index_map() {
  assert(is_loaded(), "value class must be loaded to compute mapping of field indeces");

  if (_field_index_map != NULL) {
    return _field_index_map->length();
  }

  Arena* arena = CURRENT_ENV->arena();
  _field_index_map = new (arena) GrowableArray<int>(arena, nof_declared_nonstatic_fields(), 0, 0);
  if (!has_nonstatic_fields()) {
    return 0;
  }

  // FIXME: Once it is possible to construct class hierarchies with value types.
  assert(!super()->has_nonstatic_fields(), "a value type must not inherit fields from its superclass");

  ValueKlass* vklass = ValueKlass::cast(get_Klass());
  for (JavaFieldStream fs(vklass); !fs.done(); fs.next()) {
    if (fs.access_flags().is_static()) {
      continue;
    }
    _field_index_map->append(fs.field_descriptor().index());
  }
  return _field_index_map->length();
}

// Number of value type fields
int ciValueKlass::field_count() {
  if (_field_index_map == NULL) {
    return compute_field_index_map();
  } else {
    return _field_index_map->length();
  }
}

// Size of value type fields in words
int ciValueKlass::field_size() {
  int size = 0;
  for (int i = 0; i < field_count(); ++i) {
    size += field_type_by_index(i)->size();
  }
  return size;
}

// Returns the index of the field with the given offset. If the field at 'offset'
// belongs to a flattened value type field, return the index of the field
// in the flattened value type.
int ciValueKlass::field_index_by_offset(int offset) {
  assert(contains_field_offset(offset), "invalid field offset");
  int best_offset = 0;
  int best_index = -1;
  // Search the field with the given offset
  for (int i = 0; i < field_count(); ++i) {
    int field_offset = field_offset_by_index(i);
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
  assert(best_offset == offset || field_type_by_index(best_index)->is_valuetype(), "offset should match for non-VTs");
  return best_index;
}

// Returns the field offset of the field with the given index
int ciValueKlass::field_offset_by_index(int index) {
  if (_field_index_map == NULL) {
    compute_field_index_map();
  }
  GUARDED_VM_ENTRY(
    ValueKlass* vklass = ValueKlass::cast(get_Klass());
    return vklass->field_offset(_field_index_map->at(index));
  )
}

// Returns the field type of the field with the given index
ciType* ciValueKlass::field_type_by_index(int index) {
  int offset = field_offset_by_index(index);
  VM_ENTRY_MARK;
  return get_field_type_by_offset(offset);
}

// Offset of the first field in the value type
int ciValueKlass::first_field_offset() const {
  GUARDED_VM_ENTRY(
    ValueKlass* vklass = ValueKlass::cast(get_Klass());
    return vklass->first_field_offset();
  )
}

bool ciValueKlass::flatten_array() const {
  GUARDED_VM_ENTRY(
    ValueKlass* vklass = ValueKlass::cast(get_Klass());
    return vklass->flatten_array();
  )
}

bool ciValueKlass::contains_oops() const {
  GUARDED_VM_ENTRY(
    ValueKlass* vklass = ValueKlass::cast(get_Klass());
    return vklass->contains_oops();
  )
}

// When passing a value type's fields as arguments, count the number
// of argument slots that are needed
int ciValueKlass::value_arg_slots() {
  int slots = nof_nonstatic_fields();
  for (int j = 0; j < nof_nonstatic_fields(); j++) {
    ciField* f = nonstatic_field_at(j);
    BasicType bt = f->type()->basic_type();
    assert(bt != T_VALUETYPE, "embedded");
    if (bt == T_LONG || bt == T_DOUBLE) {
      slots++;
    }
  }
  return slots;
}
