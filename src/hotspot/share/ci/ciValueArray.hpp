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

#ifndef SHARE_VM_CI_CIVALUEARRAY_HPP
#define SHARE_VM_CI_CIVALUEARRAY_HPP

#include "ci/ciArray.hpp"
#include "ci/ciClassList.hpp"
#include "oops/inlineArrayOop.hpp"

// ciValueArray
//
// This class represents an inlineArrayOop in the HotSpot virtual
// machine.
class ciValueArray : public ciArray {
  CI_PACKAGE_ACCESS

protected:
  ciValueArray(inlineArrayHandle h_o) : ciArray(h_o) {}

  ciValueArray(ciValueKlass* klass, int len) : ciArray(klass, len) {}

  inlineArrayOop get_valueArrayOop() {
    return (inlineArrayOop)get_oop();
  }

  const char* type_string() { return "ciValuejArray"; }

public:
  // What kind of ciObject is this?
  bool is_value_array() { return true; }
};

#endif // SHARE_VM_CI_CIVALUEARRAY_HPP
