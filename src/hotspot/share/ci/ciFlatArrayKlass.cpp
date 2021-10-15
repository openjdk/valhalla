/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciFlatArrayKlass.hpp"
#include "ci/ciInlineKlass.hpp"
#include "ci/ciInstanceKlass.hpp"
#include "ci/ciObjArrayKlass.hpp"
#include "ci/ciSymbol.hpp"
#include "ci/ciUtilities.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "oops/flatArrayKlass.hpp"
#include "oops/inlineKlass.inline.hpp"

// ciFlatArrayKlass
//
// This class represents a Klass* in the HotSpot virtual machine
// whose Klass part is a FlatArrayKlass.

// ------------------------------------------------------------------
// ciFlatArrayKlass::ciFlatArrayKlass
//
// Constructor for loaded inline type array klasses.
ciFlatArrayKlass::ciFlatArrayKlass(Klass* h_k) : ciArrayKlass(h_k) {
  assert(get_Klass()->is_flatArray_klass(), "wrong type");
  InlineKlass* element_Klass = get_FlatArrayKlass()->element_klass();
  _base_element_klass = CURRENT_ENV->get_klass(element_Klass);
  assert(_base_element_klass->is_inlinetype(), "bad base klass");
  if (dimension() == 1) {
    _element_klass = _base_element_klass;
  } else {
    _element_klass = NULL;
  }
  if (!ciObjectFactory::is_initialized()) {
    assert(_element_klass->is_java_lang_Object(), "only arrays of object are shared");
  }
}

// ------------------------------------------------------------------
// ciFlatArrayKlass::element_klass
//
// What is the one-level element type of this array?
ciKlass* ciFlatArrayKlass::element_klass() {
  if (_element_klass == NULL) {
    assert(dimension() > 1, "_element_klass should not be NULL");
    assert(is_loaded(), "FlatArrayKlass must be loaded");
    // Produce the element klass.
    VM_ENTRY_MARK;
    Klass* element_Klass = get_FlatArrayKlass()->element_klass();
    _element_klass = CURRENT_THREAD_ENV->get_klass(element_Klass);
  }
  return _element_klass;
}

ciKlass* ciFlatArrayKlass::exact_klass() {
  assert(element_klass()->is_loaded() && element_klass()->as_inline_klass()->exact_klass() != NULL, "must have exact klass");
  return this;
}
