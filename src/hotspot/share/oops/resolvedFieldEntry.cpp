/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
#include "resolvedFieldEntry.hpp"
#include "interpreter/bytecodes.hpp"
#include "oops/instanceOop.hpp"
#include "utilities/globalDefinitions.hpp"

void ResolvedFieldEntry::print_on(outputStream* st) const {
  st->print_cr("Field Entry:");

  if (field_holder() != nullptr) {
    st->print_cr(" - Holder: " INTPTR_FORMAT " %s", p2i(field_holder()), field_holder()->external_name());
  } else {
    st->print_cr("- Holder: null");
  }
  st->print_cr(" - Offset: %d", field_offset());
  st->print_cr(" - Field Index: %d", field_index());
  st->print_cr(" - CP Index: %d", constant_pool_index());
  st->print_cr(" - TOS: %s", type2name(as_BasicType((TosState)tos_state())));
  st->print_cr(" - Is Final: %d", is_final());
  st->print_cr(" - Is Volatile: %d", is_volatile());
  st->print_cr(" - Is Flat: %d", is_flat());
  st->print_cr(" - Is Null Free Inline Type: %d", is_null_free_inline_type());
  st->print_cr(" - Get Bytecode: %s", Bytecodes::name((Bytecodes::Code)get_code()));
  st->print_cr(" - Put Bytecode: %s", Bytecodes::name((Bytecodes::Code)put_code()));
}

bool ResolvedFieldEntry::is_valid() const {
  return field_holder()->is_instance_klass() &&
    field_offset() >= instanceOopDesc::base_offset_in_bytes() && field_offset() < 0x7fffffff &&
    as_BasicType((TosState)tos_state()) != T_ILLEGAL &&
    _flags < (1 << (max_flag_shift + 1)) &&
    (get_code() == 0 || get_code() == Bytecodes::_getstatic || get_code() == Bytecodes::_getfield) &&
    (put_code() == 0 || put_code() == Bytecodes::_putstatic || put_code() == Bytecodes::_putfield);
}

void ResolvedFieldEntry::remove_unshareable_info() {
  u2 saved_cpool_index = _cpool_index;
  memset(this, 0, sizeof(*this));
  _cpool_index = saved_cpool_index;
}
