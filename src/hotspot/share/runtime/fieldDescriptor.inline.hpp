/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_FIELDDESCRIPTOR_INLINE_HPP
#define SHARE_RUNTIME_FIELDDESCRIPTOR_INLINE_HPP

#include "runtime/fieldDescriptor.hpp"

#include "oops/fieldInfo.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/signature.hpp"

// All fieldDescriptor inline functions that (directly or indirectly) use "_cp()" or "_cp->"
// must be put in this file, as they require runtime/handles.inline.hpp.

inline Symbol* fieldDescriptor::name() const {
  return field().name(field_holder()->multifield_info(),  _cp());
}

inline Symbol* fieldDescriptor::signature() const {
  return field().signature(_cp());
}

inline ConstantPool* fieldDescriptor::constants() const {
  return _cp();
}

inline int fieldDescriptor::offset()                    const    { return field().offset(); }
inline bool fieldDescriptor::has_initial_value()        const    { return field().field_flags().is_initialized(); }
inline int fieldDescriptor::initial_value_index()       const    { return field().initializer_index(); }

inline void fieldDescriptor::set_is_field_access_watched(const bool value) {
  field_holder()->fields_status()->adr_at(index())->update_access_watched(value);
}

inline void fieldDescriptor::set_is_field_modification_watched(const bool value) {
  field_holder()->fields_status()->adr_at(index())->update_modification_watched(value);
}

inline void fieldDescriptor::set_has_initialized_final_update(const bool value) {
  field_holder()->fields_status()->adr_at(index())->update_initialized_final_update(value);
}

inline BasicType fieldDescriptor::field_type() const {
  return Signature::basic_type(signature());
}

inline bool fieldDescriptor::is_inlined()  const  { return field().field_flags().is_inlined(); }
inline bool fieldDescriptor::is_inline_type() const { return field_type() == T_PRIMITIVE_OBJECT; }

inline bool fieldDescriptor::is_multifield() const { return field().is_multifield(); }
inline bool fieldDescriptor::is_multifield_base() const { return field().is_multifield_base(); }
inline u2   fieldDescriptor::multifield_base() const {
  return is_multifield() ? field_holder()->multifield_info(field().secondary_index()).base_index() : index();
}
inline jbyte fieldDescriptor::multifield_index() const {
 return  is_multifield() ? field_holder()->multifield_info(field().secondary_index()).multifield_index() : (jbyte)0;
}

inline int fieldDescriptor::secondary_fields_count(int base_idx) const {
  Array<MultiFieldInfo>* multifield_info = field_holder()->multifield_info();
  if (!is_multifield_base() || nullptr == multifield_info) {
    return 1;
  }
  int sec_fields_count = 1;
  for (int i = 0; i < multifield_info->length(); i++) {
    if (field_holder()->multifield_info(i).base_index() == base_idx) {
      sec_fields_count++;
    }
  }
  return  sec_fields_count;
}

#endif // SHARE_RUNTIME_FIELDDESCRIPTOR_INLINE_HPP
