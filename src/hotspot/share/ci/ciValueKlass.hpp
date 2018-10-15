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
  // Fields declared in the bytecode (without flattened value type fields)
  GrowableArray<ciField*>* _declared_nonstatic_fields;

protected:
  ciValueKlass(Klass* h_k) : ciInstanceKlass(h_k), _declared_nonstatic_fields(NULL) {
    assert(is_final(), "ValueKlass must be final");
  };

  int compute_nonstatic_fields();
  const char* type_string() { return "ciValueKlass"; }

public:
  bool is_valuetype() const { return true; }

  int nof_declared_nonstatic_fields() {
    if (_declared_nonstatic_fields == NULL) {
      compute_nonstatic_fields();
    }
    return _declared_nonstatic_fields->length();
  }

  // ith non-static declared field (presented by ascending address)
  ciField* declared_nonstatic_field_at(int i) {
    assert(_declared_nonstatic_fields != NULL, "should be initialized");
    return _declared_nonstatic_fields->at(i);
  }

  // Value type fields
  int first_field_offset() const;
  int field_index_by_offset(int offset);

  bool flatten_array() const;
  bool can_be_returned_as_fields() const;
  bool is_bufferable() const;
  bool is_scalarizable() const;
  int value_arg_slots();
  int default_value_offset() const;
  ciInstance* default_value_instance() const;
  bool contains_oops() const;
};

#endif // SHARE_VM_CI_CIVALUEKLASS_HPP
