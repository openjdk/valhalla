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

#ifndef SHARE_VM_CI_CIINLINEKLASS_HPP
#define SHARE_VM_CI_CIINLINEKLASS_HPP

#include "ci/ciConstantPoolCache.hpp"
#include "ci/ciEnv.hpp"
#include "ci/ciField.hpp"
#include "ci/ciFlags.hpp"
#include "ci/ciInstanceKlass.hpp"
#include "ci/ciSymbol.hpp"
#include "oops/inlineKlass.hpp"

// ciInlineKlass
//
// Specialized ciInstanceKlass for inline types.
class ciInlineKlass : public ciInstanceKlass {
  CI_PACKAGE_ACCESS

private:
  // Fields declared in the bytecode (without flattened inline type fields)
  GrowableArray<ciField*>* _declared_nonstatic_fields;

  InlineKlass* to_InlineKlass() const {
    return InlineKlass::cast(get_Klass());
  }

protected:
  ciInlineKlass(Klass* h_k) : ciInstanceKlass(h_k), _declared_nonstatic_fields(NULL) {
    assert(is_final(), "InlineKlass must be final");
  };

  ciInlineKlass(ciSymbol* name, jobject loader, jobject protection_domain) :
    ciInstanceKlass(name, loader, protection_domain, T_PRIMITIVE_OBJECT) {}

  int compute_nonstatic_fields();
  const char* type_string() { return "ciInlineKlass"; }

public:
  bool is_inlinetype() const { return true; }

  int nof_declared_nonstatic_fields() {
    if (_declared_nonstatic_fields == NULL) {
      compute_nonstatic_fields();
    }
    return _declared_nonstatic_fields->length();
  }

  // ith non-static declared field (presented by ascending address)
  ciField* declared_nonstatic_field_at(int i) {
    assert(_declared_nonstatic_fields != NULL, "should be initialized");
    // Look for field in preceding multi-field bundle;
    for (int j = 0; j <= i; j++) {
      int bundle_size = _declared_nonstatic_fields->at(j)->secondary_fields_count();
      if (bundle_size > 1 && ((j + bundle_size) > i)) {
        if (j == i) {
          // Multifield base.
          return _declared_nonstatic_fields->at(i);
        } else {
          // Secondary multifield.
          return static_cast<ciMultiField*>(_declared_nonstatic_fields->at(j))->secondary_fields()->at(i - (j + 1));
        }
      } else if (j == i) {
        return _declared_nonstatic_fields->at(i);
      }
    }
    return NULL;
  }

  // Inline type fields
  int first_field_offset() const;
  int field_index_by_offset(int offset);

  bool flatten_array() const;
  bool can_be_passed_as_fields() const;
  bool can_be_returned_as_fields() const;
  bool is_empty();
  int inline_arg_slots();
  int default_value_offset() const;
  ciInstance* default_instance() const;
  ciInstance* ref_instance() const;
  ciInstance* val_instance() const;
  bool contains_oops() const;
  int oop_count() const;
  address pack_handler() const;
  address unpack_handler() const;
  InlineKlass* get_InlineKlass() const;
  ciInstance* ref_mirror();
  ciInstance* val_mirror();
};

#endif // SHARE_VM_CI_CIINLINEKLASS_HPP
