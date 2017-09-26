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

#ifndef SHARE_VM_CI_CIVALUEKLASS_HPP
#define SHARE_VM_CI_CIVALUEKLASS_HPP

#include "ci/ciConstantPoolCache.hpp"
#include "ci/ciEnv.hpp"
#include "ci/ciFlags.hpp"
#include "ci/ciInstanceKlass.hpp"
#include "ci/ciSymbol.hpp"
#include "oops/valueKlass.hpp"

// ciValueKlass
//
// Specialized ciInstanceKlass for value types.
class ciValueKlass : public ciInstanceKlass {
  CI_PACKAGE_ACCESS

private:
  // Index fields of a value type, indeces range from 0 to the number of fields of the
  // value type - 1.
  // For each index constructed, _field_index_map records the field's index
  // in InstanceKlass::_fields (i.e., _field_index_map records the value returned by
  // fieldDescriptor::index() for each field).
  GrowableArray<int>* _field_index_map;

protected:
  ciValueKlass(Klass* h_k) : ciInstanceKlass(h_k), _field_index_map(NULL) {
    assert(is_final(), "ValueKlass must be final");
  };

  const char* type_string() { return "ciValueKlass"; }
  int compute_field_index_map();

  ValueKlass* get_valueKlass() const {
    return ValueKlass::cast(get_Klass());
  }

public:
  bool      is_valuetype() const { return true; }
  bool      flatten_array() const;
  bool      contains_oops() const;

  // Value type fields
  int       field_count();
  int       field_size();
  int       flattened_field_count() {
    return nof_nonstatic_fields();
  }
  int       field_index_by_offset(int offset);
  int       field_offset_by_index(int index);
  ciType*   field_type_by_index(int index);
  int       first_field_offset() const;

  int value_arg_slots();

  // Can a value type instance of this type be returned as multiple
  // returned values?
  bool can_be_returned_as_fields() const {
    return this != ciEnv::current()->___Value_klass() && get_valueKlass()->return_regs() != NULL;
  }
};

#endif // SHARE_VM_CI_CIVALUEKLASS_HPP
