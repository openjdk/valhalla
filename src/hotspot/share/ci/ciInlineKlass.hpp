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
  // Fields declared in the bytecode (without nested fields in flat fields)
  GrowableArray<ciField*>* _declared_nonstatic_fields;

  InlineKlass* to_InlineKlass() const {
    return InlineKlass::cast(get_Klass());
  }

protected:
  ciInlineKlass(Klass* h_k) : ciInstanceKlass(h_k), _declared_nonstatic_fields(nullptr) {
    assert(is_final(), "InlineKlass must be final");
  };

  ciInlineKlass(ciSymbol* name, jobject loader) :
    ciInstanceKlass(name, loader, T_OBJECT) {}

  int compute_nonstatic_fields();
  const char* type_string() { return "ciInlineKlass"; }

public:
  bool is_inlinetype() const { return true; }

  int nof_declared_nonstatic_fields() {
    if (_declared_nonstatic_fields == nullptr) {
      compute_nonstatic_fields();
    }
    return _declared_nonstatic_fields->length();
  }

  // ith non-static declared field (presented by ascending address)
  ciField* declared_nonstatic_field_at(int i) {
    assert(_declared_nonstatic_fields != nullptr, "should be initialized");
    return _declared_nonstatic_fields->at(i);
  }

  // Inline type fields
  int payload_offset() const;
  int field_index_by_offset(int offset);

  bool flat_in_array() const;
  bool can_be_passed_as_fields() const;
  bool can_be_returned_as_fields() const;
  bool is_empty();
  int inline_arg_slots();
  bool contains_oops() const;
  int oop_count() const;
  address pack_handler() const;
  address unpack_handler() const;
  InlineKlass* get_InlineKlass() const;
  int nullable_size_in_bytes() const;
  bool has_non_atomic_layout() const;
  bool has_atomic_layout() const;
  bool has_nullable_atomic_layout() const;
  int null_marker_offset_in_payload() const;
  BasicType atomic_size_to_basic_type(bool null_free) const;
};

#endif // SHARE_VM_CI_CIINLINEKLASS_HPP
