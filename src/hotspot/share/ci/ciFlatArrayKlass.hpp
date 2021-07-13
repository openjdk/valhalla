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

#ifndef SHARE_VM_CI_CIFLATARRAYKLASS_HPP
#define SHARE_VM_CI_CIFLATARRAYKLASS_HPP

#include "ci/ciArrayKlass.hpp"

// ciFlatArrayKlass
//
// This class represents a Klass* in the HotSpot virtual machine
// whose Klass part is a FlatArrayKlass.
class ciFlatArrayKlass : public ciArrayKlass {
  CI_PACKAGE_ACCESS
  friend class ciEnv;

private:
  ciKlass* _element_klass;
  ciKlass* _base_element_klass;

protected:
  ciFlatArrayKlass(Klass* h_k);
  ciFlatArrayKlass(ciSymbol* array_name,
                    ciInlineKlass* element_klass,
                    int dimension);

  FlatArrayKlass* get_FlatArrayKlass() {
    return (FlatArrayKlass*)get_Klass();
  }

  static ciArrayKlass* make_impl(ciKlass* element_klass);
  static ciSymbol* construct_array_name(ciSymbol* element_name,
                                        int       dimension);

  const char* type_string() { return "ciFlatArrayKlass"; }

  oop     loader()        { return _base_element_klass->loader(); }
  jobject loader_handle() { return _base_element_klass->loader_handle(); }

  oop     protection_domain()        { return _base_element_klass->protection_domain(); }
  jobject protection_domain_handle() { return _base_element_klass->protection_domain_handle(); }


public:
  // The one-level type of the array elements.
  ciKlass* element_klass();

  int log2_element_size() {
    return Klass::layout_helper_log2_element_size(layout_helper());
  }
  int element_byte_size() { return 1 << log2_element_size(); }

  // The innermost type of the array elements.
  ciKlass* base_element_klass() { return _base_element_klass; }

  // What kind of ciObject is this?
  bool is_flat_array_klass() const { return true; }

  static ciArrayKlass* make(ciKlass* element_klass);

  virtual ciKlass* exact_klass();

  virtual bool can_be_inline_array_klass() {
    return true;
  }

  virtual bool is_elem_null_free() const { return true; }
};


#endif // SHARE_VM_CI_CIFLATARRAYKLASS_HPP
