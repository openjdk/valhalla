/*
 * Copyright (c) 1999, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "ci/ciArrayKlass.hpp"
#include "ci/ciFlatArrayKlass.hpp"
#include "ci/ciInlineKlass.hpp"
#include "ci/ciObjArrayKlass.hpp"
#include "ci/ciTypeArrayKlass.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "oops/inlineKlass.inline.hpp"
#include "memory/universe.hpp"

// ciArrayKlass
//
// This class represents a Klass* in the HotSpot virtual machine
// whose Klass part in an ArrayKlass.

// ------------------------------------------------------------------
// ciArrayKlass::ciArrayKlass
//
// Loaded array klass.
ciArrayKlass::ciArrayKlass(Klass* k) : ciKlass(k) {
  assert(get_Klass()->is_array_klass(), "wrong type");
  _dimension = get_ArrayKlass()->dimension();
}

// ------------------------------------------------------------------
// ciArrayKlass::ciArrayKlass
//
// Unloaded array klass.
ciArrayKlass::ciArrayKlass(ciSymbol* name, int dimension, BasicType bt)
  : ciKlass(name, bt) {
  _dimension = dimension;
}

// ------------------------------------------------------------------
// ciArrayKlass::element_type
//
// What type is obtained when this array is indexed once?
ciType* ciArrayKlass::element_type() {
  if (is_type_array_klass()) {
    return ciType::make(as_type_array_klass()->element_type());
  } else {
    return element_klass()->as_klass();
  }
}


// ------------------------------------------------------------------
// ciArrayKlass::base_element_type
//
// What type is obtained when this array is indexed as many times as possible?
ciType* ciArrayKlass::base_element_type() {
  if (is_type_array_klass()) {
    return ciType::make(as_type_array_klass()->element_type());
  } else if (is_obj_array_klass()) {
    ciKlass* ek = as_obj_array_klass()->base_element_klass();
    if (ek->is_type_array_klass()) {
      return ciType::make(ek->as_type_array_klass()->element_type());
    }
    return ek;
  } else {
    return as_flat_array_klass()->base_element_klass();
  }
}


// ------------------------------------------------------------------
// ciArrayKlass::is_leaf_type
bool ciArrayKlass::is_leaf_type() {
  if (is_type_array_klass()) {
    return true;
  } else {
    return as_obj_array_klass()->base_element_klass()->is_leaf_type();
  }
}


// ------------------------------------------------------------------
// ciArrayKlass::make
//
// Make an array klass of the specified element type.
ciArrayKlass* ciArrayKlass::make(ciType* element_type, bool flat, bool null_free, bool atomic) {
  if (element_type->is_primitive_type()) {
    return ciTypeArrayKlass::make(element_type->basic_type());
  }

  ciKlass* klass = element_type->as_klass();
  assert(!null_free || !klass->is_loaded() || klass->is_inlinetype() || klass->is_abstract() ||
         klass->is_java_lang_Object(), "only value classes are null free");
  if (klass->is_loaded() && klass->is_inlinetype()) {
    GUARDED_VM_ENTRY(
      EXCEPTION_CONTEXT;
      Klass* ak = nullptr;
      InlineKlass* vk = InlineKlass::cast(klass->get_Klass());
      if (flat && vk->flat_array()) {
        LayoutKind lk;
        if (null_free) {
          if (vk->is_naturally_atomic()) {
            atomic = vk->has_atomic_layout();
          }
          if (!atomic && !vk->has_non_atomic_layout()) {
            // TODO 8350865 Impossible type
            lk = vk->has_atomic_layout() ? LayoutKind::ATOMIC_FLAT : LayoutKind::NULLABLE_ATOMIC_FLAT;
          } else {
            lk = atomic ? LayoutKind::ATOMIC_FLAT : LayoutKind::NON_ATOMIC_FLAT;
          }
        } else {
          if (!vk->has_nullable_atomic_layout()) {
            // TODO 8350865 Impossible type, null-able flat is always atomic.
            lk = vk->has_atomic_layout() ? LayoutKind::ATOMIC_FLAT : LayoutKind::NON_ATOMIC_FLAT;
          } else {
            lk = LayoutKind::NULLABLE_ATOMIC_FLAT;
          }
        }
        ak = vk->flat_array_klass(lk, THREAD);
      } else if (null_free) {
        ak = vk->null_free_reference_array(THREAD);
      } else {
        return ciObjArrayKlass::make(klass);
      }
      if (HAS_PENDING_EXCEPTION) {
        CLEAR_PENDING_EXCEPTION;
      } else if (ak->is_flatArray_klass()) {
        return CURRENT_THREAD_ENV->get_flat_array_klass(ak);
      } else if (ak->is_objArray_klass()) {
        return CURRENT_THREAD_ENV->get_obj_array_klass(ak);
      }
    )
  }
  return ciObjArrayKlass::make(klass);
}

int ciArrayKlass::array_header_in_bytes() {
  return get_ArrayKlass()->array_header_in_bytes();
}

ciInstance* ciArrayKlass::component_mirror_instance() const {
  GUARDED_VM_ENTRY(
    oop component_mirror = ArrayKlass::cast(get_Klass())->component_mirror();
    return CURRENT_ENV->get_instance(component_mirror);
  )
}

bool ciArrayKlass::is_elem_null_free() const {
  GUARDED_VM_ENTRY(return !is_type_array_klass() && get_Klass()->is_null_free_array_klass();)
}
