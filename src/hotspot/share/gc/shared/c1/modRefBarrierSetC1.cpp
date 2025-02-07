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

#include "precompiled.hpp"
#include "ci/ciInlineKlass.hpp"
#include "gc/shared/c1/modRefBarrierSetC1.hpp"
#include "utilities/macros.hpp"

#ifdef ASSERT
#define __ gen->lir(__FILE__, __LINE__)->
#else
#define __ gen->lir()->
#endif

void ModRefBarrierSetC1::store_at_resolved(LIRAccess& access, LIR_Opr value) {
  DecoratorSet decorators = access.decorators();
  bool is_array = (decorators & IS_ARRAY) != 0;
  bool on_anonymous = (decorators & ON_UNKNOWN_OOP_REF) != 0;

  // Is this a flat, atomic access that might require gc barriers on oop fields?
  ciInlineKlass* vk = access.vk();
  if (vk != nullptr && vk->has_object_fields()) {
    // Add pre-barriers for oop fields
    for (int i = 0; i < vk->nof_nonstatic_fields(); i++) {
      ciField* field = vk->nonstatic_field_at(i);
      if (!field->type()->is_primitive_type()) {
        int off = access.offset().opr().as_jint() + field->offset_in_bytes() - vk->payload_offset();
        LIRAccess inner_access(access.gen(), decorators, access.base(), LIR_OprFact::intConst(off), field->type()->basic_type(), access.patch_emit_info(), access.access_emit_info());
        pre_barrier(inner_access, resolve_address(inner_access, false),
                    LIR_OprFact::illegalOpr /* pre_val */, inner_access.patch_emit_info());
      }
    }
  }

  if (access.is_oop()) {
    pre_barrier(access, access.resolved_addr(),
                LIR_OprFact::illegalOpr /* pre_val */, access.patch_emit_info());
  }

  BarrierSetC1::store_at_resolved(access, value);

  if (access.is_oop()) {
    bool precise = is_array || on_anonymous;
    LIR_Opr post_addr = precise ? access.resolved_addr() : access.base().opr();
    post_barrier(access, post_addr, value);
  }

  if (vk != nullptr && vk->has_object_fields()) {
    // Add post-barriers for oop fields
    for (int i = 0; i < vk->nof_nonstatic_fields(); i++) {
      ciField* field = vk->nonstatic_field_at(i);
      if (!field->type()->is_primitive_type()) {
        int inner_off = field->offset_in_bytes() - vk->payload_offset();
        int off = access.offset().opr().as_jint() + inner_off;
        LIRAccess inner_access(access.gen(), decorators, access.base(), LIR_OprFact::intConst(off), field->type()->basic_type(), access.patch_emit_info(), access.access_emit_info());

        // Shift long value to extract the narrow oop field value and zero-extend
        LIR_Opr field_val = access.gen()->new_register(T_LONG);
        access.gen()->lir()->unsigned_shift_right(value,
                                                  LIR_OprFact::intConst(inner_off << LogBitsPerByte),
                                                  field_val, LIR_Opr::illegalOpr());
        LIR_Opr mask = access.gen()->load_immediate((julong) max_juint, T_LONG);
        access.gen()->lir()->logical_and(field_val, mask, field_val);
        LIR_Opr oop_val = access.gen()->new_register(T_OBJECT);
        access.gen()->lir()->move(field_val, oop_val);

        assert(!is_array && !on_anonymous, "not suppported");
        post_barrier(inner_access, access.base().opr(), oop_val);
      }
    }
  }
}

LIR_Opr ModRefBarrierSetC1::atomic_cmpxchg_at_resolved(LIRAccess& access, LIRItem& cmp_value, LIRItem& new_value) {
  if (access.is_oop()) {
    pre_barrier(access, access.resolved_addr(),
                LIR_OprFact::illegalOpr /* pre_val */, nullptr);
  }

  LIR_Opr result = BarrierSetC1::atomic_cmpxchg_at_resolved(access, cmp_value, new_value);

  if (access.is_oop()) {
    post_barrier(access, access.resolved_addr(), new_value.result());
  }

  return result;
}

LIR_Opr ModRefBarrierSetC1::atomic_xchg_at_resolved(LIRAccess& access, LIRItem& value) {
  if (access.is_oop()) {
    pre_barrier(access, access.resolved_addr(),
                LIR_OprFact::illegalOpr /* pre_val */, nullptr);
  }

  LIR_Opr result = BarrierSetC1::atomic_xchg_at_resolved(access, value);

  if (access.is_oop()) {
    post_barrier(access, access.resolved_addr(), value.result());
  }

  return result;
}

// This overrides the default to resolve the address into a register,
// assuming it will be used by a write barrier anyway.
LIR_Opr ModRefBarrierSetC1::resolve_address(LIRAccess& access, bool resolve_in_register) {
  DecoratorSet decorators = access.decorators();
  bool needs_patching = (decorators & C1_NEEDS_PATCHING) != 0;
  bool is_write = (decorators & ACCESS_WRITE) != 0;
  bool is_array = (decorators & IS_ARRAY) != 0;
  bool on_anonymous = (decorators & ON_UNKNOWN_OOP_REF) != 0;
  bool precise = is_array || on_anonymous;
  resolve_in_register |= !needs_patching && is_write && access.is_oop() && precise;
  return BarrierSetC1::resolve_address(access, resolve_in_register);
}
