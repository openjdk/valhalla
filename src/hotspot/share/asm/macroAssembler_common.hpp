/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_ASM_MACROASSEMBLER_COMMON_HPP
#define SHARE_ASM_MACROASSEMBLER_COMMON_HPP

// These are part of the MacroAssembler class that are common for
// all CPUs

// class MacroAssembler ... {
private:
  void skip_unpacked_fields(const GrowableArray<SigEntry>* sig, int& sig_index, VMRegPair* regs_from,
                            int regs_from_count, int& from_index);
  bool is_reg_in_unpacked_fields(const GrowableArray<SigEntry>* sig, int sig_index, VMReg to, VMRegPair* regs_from,
                                 int regs_from_count, int from_index);
  void mark_reg_writable(const VMRegPair* regs, int num_regs, int reg_index, RegState* reg_state);
  void mark_reserved_entries_writable(const GrowableArray<SigEntry>* sig_cc, const VMRegPair* regs, int num_regs, RegState* reg_state);
  RegState* init_reg_state(bool is_packing, const GrowableArray<SigEntry>* sig_cc,
                           VMRegPair* regs, int num_regs, int sp_inc, int max_stack);

  int unpack_value_args_common(Compile* C, bool receiver_only);
  void shuffle_value_args_common(bool is_packing, bool receiver_only, int extra_stack_offset,
                                 BasicType* sig_bt, const GrowableArray<SigEntry>* sig_cc,
                                 int args_passed, int args_on_stack, VMRegPair* regs,
                                 int args_passed_to, int args_on_stack_to, VMRegPair* regs_to,
                                 int sp_inc, int ret_off);

// };

#endif // SHARE_ASM_MACROASSEMBLER_COMMON_HPP
