/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, Red Hat Inc. All rights reserved.
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
#include "c1/c1_MacroAssembler.hpp"
#include "c1/c1_Runtime1.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "interpreter/interpreter.hpp"
#include "oops/arrayOop.hpp"
#include "oops/markWord.hpp"
#include "runtime/basicLock.hpp"
#include "runtime/biasedLocking.hpp"
#include "runtime/os.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"

void C1_MacroAssembler::float_cmp(bool is_float, int unordered_result,
                                  FloatRegister f0, FloatRegister f1,
                                  Register result)
{
  Label done;
  if (is_float) {
    fcmps(f0, f1);
  } else {
    fcmpd(f0, f1);
  }
  if (unordered_result < 0) {
    // we want -1 for unordered or less than, 0 for equal and 1 for
    // greater than.
    cset(result, NE);  // Not equal or unordered
    cneg(result, result, LT);  // Less than or unordered
  } else {
    // we want -1 for less than, 0 for equal and 1 for unordered or
    // greater than.
    cset(result, NE);  // Not equal or unordered
    cneg(result, result, LO);  // Less than
  }
}

int C1_MacroAssembler::lock_object(Register hdr, Register obj, Register disp_hdr, Register scratch, Label& slow_case) {
  const int aligned_mask = BytesPerWord -1;
  const int hdr_offset = oopDesc::mark_offset_in_bytes();
  assert(hdr != obj && hdr != disp_hdr && obj != disp_hdr, "registers must be different");
  Label done;
  int null_check_offset = -1;

  verify_oop(obj);

  // save object being locked into the BasicObjectLock
  str(obj, Address(disp_hdr, BasicObjectLock::obj_offset_in_bytes()));

  null_check_offset = offset();

  if (DiagnoseSyncOnValueBasedClasses != 0) {
    load_klass(hdr, obj);
    ldrw(hdr, Address(hdr, Klass::access_flags_offset()));
    tstw(hdr, JVM_ACC_IS_VALUE_BASED_CLASS);
    br(Assembler::NE, slow_case);
  }

  if (UseBiasedLocking) {
    assert(scratch != noreg, "should have scratch register at this point");
    biased_locking_enter(disp_hdr, obj, hdr, scratch, false, done, &slow_case);
  }

  // Load object header
  ldr(hdr, Address(obj, hdr_offset));
  // and mark it as unlocked
  orr(hdr, hdr, markWord::unlocked_value);

  if (EnableValhalla) {
    assert(!UseBiasedLocking, "Not compatible with biased-locking");
    // Mask always_locked bit such that we go to the slow path if object is an inline type
    andr(hdr, hdr, ~markWord::inline_type_bit_in_place);
  }

  // save unlocked object header into the displaced header location on the stack
  str(hdr, Address(disp_hdr, 0));
  // test if object header is still the same (i.e. unlocked), and if so, store the
  // displaced header address in the object header - if it is not the same, get the
  // object header instead
  lea(rscratch2, Address(obj, hdr_offset));
  cmpxchgptr(hdr, disp_hdr, rscratch2, rscratch1, done, /*fallthough*/NULL);
  // if the object header was the same, we're done
  // if the object header was not the same, it is now in the hdr register
  // => test if it is a stack pointer into the same stack (recursive locking), i.e.:
  //
  // 1) (hdr & aligned_mask) == 0
  // 2) sp <= hdr
  // 3) hdr <= sp + page_size
  //
  // these 3 tests can be done by evaluating the following expression:
  //
  // (hdr - sp) & (aligned_mask - page_size)
  //
  // assuming both the stack pointer and page_size have their least
  // significant 2 bits cleared and page_size is a power of 2
  mov(rscratch1, sp);
  sub(hdr, hdr, rscratch1);
  ands(hdr, hdr, aligned_mask - os::vm_page_size());
  // for recursive locking, the result is zero => save it in the displaced header
  // location (NULL in the displaced hdr location indicates recursive locking)
  str(hdr, Address(disp_hdr, 0));
  // otherwise we don't care about the result and handle locking via runtime call
  cbnz(hdr, slow_case);
  // done
  bind(done);
  if (PrintBiasedLockingStatistics) {
    lea(rscratch2, ExternalAddress((address)BiasedLocking::fast_path_entry_count_addr()));
    addmw(Address(rscratch2, 0), 1, rscratch1);
  }
  return null_check_offset;
}


void C1_MacroAssembler::unlock_object(Register hdr, Register obj, Register disp_hdr, Label& slow_case) {
  const int aligned_mask = BytesPerWord -1;
  const int hdr_offset = oopDesc::mark_offset_in_bytes();
  assert(hdr != obj && hdr != disp_hdr && obj != disp_hdr, "registers must be different");
  Label done;

  if (UseBiasedLocking) {
    // load object
    ldr(obj, Address(disp_hdr, BasicObjectLock::obj_offset_in_bytes()));
    biased_locking_exit(obj, hdr, done);
  }

  // load displaced header
  ldr(hdr, Address(disp_hdr, 0));
  // if the loaded hdr is NULL we had recursive locking
  // if we had recursive locking, we are done
  cbz(hdr, done);
  if (!UseBiasedLocking) {
    // load object
    ldr(obj, Address(disp_hdr, BasicObjectLock::obj_offset_in_bytes()));
  }
  verify_oop(obj);
  // test if object header is pointing to the displaced header, and if so, restore
  // the displaced header in the object - if the object header is not pointing to
  // the displaced header, get the object header instead
  // if the object header was not pointing to the displaced header,
  // we do unlocking via runtime call
  if (hdr_offset) {
    lea(rscratch1, Address(obj, hdr_offset));
    cmpxchgptr(disp_hdr, hdr, rscratch1, rscratch2, done, &slow_case);
  } else {
    cmpxchgptr(disp_hdr, hdr, obj, rscratch2, done, &slow_case);
  }
  // done
  bind(done);
}


// Defines obj, preserves var_size_in_bytes
void C1_MacroAssembler::try_allocate(Register obj, Register var_size_in_bytes, int con_size_in_bytes, Register t1, Register t2, Label& slow_case) {
  if (UseTLAB) {
    tlab_allocate(obj, var_size_in_bytes, con_size_in_bytes, t1, t2, slow_case);
  } else {
    eden_allocate(obj, var_size_in_bytes, con_size_in_bytes, t1, slow_case);
  }
}

void C1_MacroAssembler::initialize_header(Register obj, Register klass, Register len, Register t1, Register t2) {
  assert_different_registers(obj, klass, len);
  if (EnableValhalla) {
    // Need to copy markWord::prototype header for klass
    assert_different_registers(obj, klass, len, t1, t2);
    ldr(t1, Address(klass, Klass::prototype_header_offset()));
  } else {
    // This assumes that all prototype bits fit in an int32_t
    mov(t1, (int32_t)(intptr_t)markWord::prototype().value());
  }
  str(t1, Address(obj, oopDesc::mark_offset_in_bytes()));

  if (UseCompressedClassPointers) { // Take care not to kill klass
    encode_klass_not_null(t1, klass);
    strw(t1, Address(obj, oopDesc::klass_offset_in_bytes()));
  } else {
    str(klass, Address(obj, oopDesc::klass_offset_in_bytes()));
  }

  if (len->is_valid()) {
    strw(len, Address(obj, arrayOopDesc::length_offset_in_bytes()));
  } else if (UseCompressedClassPointers) {
    store_klass_gap(obj, zr);
  }
}

// preserves obj, destroys len_in_bytes
void C1_MacroAssembler::initialize_body(Register obj, Register len_in_bytes, int hdr_size_in_bytes, Register t1) {
  assert(hdr_size_in_bytes >= 0, "header size must be positive or 0");
  Label done;

  // len_in_bytes is positive and ptr sized
  subs(len_in_bytes, len_in_bytes, hdr_size_in_bytes);
  br(Assembler::EQ, done);

  // Preserve obj
  if (hdr_size_in_bytes)
    add(obj, obj, hdr_size_in_bytes);
  zero_memory(obj, len_in_bytes, t1);
  if (hdr_size_in_bytes)
    sub(obj, obj, hdr_size_in_bytes);

  bind(done);
}


void C1_MacroAssembler::allocate_object(Register obj, Register t1, Register t2, int header_size, int object_size, Register klass, Label& slow_case) {
  assert_different_registers(obj, t1, t2); // XXX really?
  assert(header_size >= 0 && object_size >= header_size, "illegal sizes");

  try_allocate(obj, noreg, object_size * BytesPerWord, t1, t2, slow_case);

  initialize_object(obj, klass, noreg, object_size * HeapWordSize, t1, t2, UseTLAB);
}

void C1_MacroAssembler::initialize_object(Register obj, Register klass, Register var_size_in_bytes, int con_size_in_bytes, Register t1, Register t2, bool is_tlab_allocated) {
  assert((con_size_in_bytes & MinObjAlignmentInBytesMask) == 0,
         "con_size_in_bytes is not multiple of alignment");
  const int hdr_size_in_bytes = instanceOopDesc::header_size() * HeapWordSize;

  initialize_header(obj, klass, noreg, t1, t2);

  if (!(UseTLAB && ZeroTLAB && is_tlab_allocated)) {
     // clear rest of allocated space
     const Register index = t2;
     const int threshold = 16 * BytesPerWord;   // approximate break even point for code size (see comments below)
     if (var_size_in_bytes != noreg) {
       mov(index, var_size_in_bytes);
       initialize_body(obj, index, hdr_size_in_bytes, t1);
     } else if (con_size_in_bytes <= threshold) {
       // use explicit null stores
       int i = hdr_size_in_bytes;
       if (i < con_size_in_bytes && (con_size_in_bytes % (2 * BytesPerWord))) {
         str(zr, Address(obj, i));
         i += BytesPerWord;
       }
       for (; i < con_size_in_bytes; i += 2 * BytesPerWord)
         stp(zr, zr, Address(obj, i));
     } else if (con_size_in_bytes > hdr_size_in_bytes) {
       block_comment("zero memory");
      // use loop to null out the fields

       int words = (con_size_in_bytes - hdr_size_in_bytes) / BytesPerWord;
       mov(index,  words / 8);

       const int unroll = 8; // Number of str(zr) instructions we'll unroll
       int remainder = words % unroll;
       lea(rscratch1, Address(obj, hdr_size_in_bytes + remainder * BytesPerWord));

       Label entry_point, loop;
       b(entry_point);

       bind(loop);
       sub(index, index, 1);
       for (int i = -unroll; i < 0; i++) {
         if (-i == remainder)
           bind(entry_point);
         str(zr, Address(rscratch1, i * wordSize));
       }
       if (remainder == 0)
         bind(entry_point);
       add(rscratch1, rscratch1, unroll * wordSize);
       cbnz(index, loop);

     }
  }

  membar(StoreStore);

  if (CURRENT_ENV->dtrace_alloc_probes()) {
    assert(obj == r0, "must be");
    far_call(RuntimeAddress(Runtime1::entry_for(Runtime1::dtrace_object_alloc_id)));
  }

  verify_oop(obj);
}

void C1_MacroAssembler::allocate_array(Register obj, Register len, Register t1, Register t2, int header_size, int f, Register klass, Label& slow_case) {
  assert_different_registers(obj, len, t1, t2, klass);

  // determine alignment mask
  assert(!(BytesPerWord & 1), "must be a multiple of 2 for masking code to work");

  // check for negative or excessive length
  mov(rscratch1, (int32_t)max_array_allocation_length);
  cmp(len, rscratch1);
  br(Assembler::HS, slow_case);

  const Register arr_size = t2; // okay to be the same
  // align object end
  mov(arr_size, (int32_t)header_size * BytesPerWord + MinObjAlignmentInBytesMask);
  add(arr_size, arr_size, len, ext::uxtw, f);
  andr(arr_size, arr_size, ~MinObjAlignmentInBytesMask);

  try_allocate(obj, arr_size, 0, t1, t2, slow_case);

  initialize_header(obj, klass, len, t1, t2);

  // clear rest of allocated space
  const Register len_zero = len;
  initialize_body(obj, arr_size, header_size * BytesPerWord, len_zero);

  membar(StoreStore);

  if (CURRENT_ENV->dtrace_alloc_probes()) {
    assert(obj == r0, "must be");
    far_call(RuntimeAddress(Runtime1::entry_for(Runtime1::dtrace_object_alloc_id)));
  }

  verify_oop(obj);
}


void C1_MacroAssembler::inline_cache_check(Register receiver, Register iCache) {
  verify_oop(receiver);
  // explicit NULL check not needed since load from [klass_offset] causes a trap
  // check against inline cache
  assert(!MacroAssembler::needs_explicit_null_check(oopDesc::klass_offset_in_bytes()), "must add explicit null check");

  cmp_klass(receiver, iCache, rscratch1);
}

void C1_MacroAssembler::build_frame_helper(int frame_size_in_bytes, int sp_inc, bool needs_stack_repair) {
  MacroAssembler::build_frame(frame_size_in_bytes);

  if (needs_stack_repair) {
    int sp_inc_offset = frame_size_in_bytes - 3 * wordSize;  // Immediately below saved LR and FP
    save_stack_increment(sp_inc, frame_size_in_bytes, sp_inc_offset);
  }
}

void C1_MacroAssembler::build_frame(int frame_size_in_bytes, int bang_size_in_bytes, int sp_offset_for_orig_pc, bool needs_stack_repair, bool has_scalarized_args, Label* verified_inline_entry_label) {
  if (has_scalarized_args) {
    // Initialize orig_pc to detect deoptimization during buffering in the entry points
    str(zr, Address(sp, sp_offset_for_orig_pc - frame_size_in_bytes));
  }
  if (!needs_stack_repair && verified_inline_entry_label != NULL) {
    bind(*verified_inline_entry_label);
  }

  // Make sure there is enough stack space for this method's activation.
  // Note that we do this before creating a frame.
  assert(bang_size_in_bytes >= frame_size_in_bytes, "stack bang size incorrect");
  generate_stack_overflow_check(bang_size_in_bytes);

  build_frame_helper(frame_size_in_bytes, 0, needs_stack_repair);

  // Insert nmethod entry barrier into frame.
  BarrierSetAssembler* bs = BarrierSet::barrier_set()->barrier_set_assembler();
  bs->nmethod_entry_barrier(this);

  if (needs_stack_repair && verified_inline_entry_label != NULL) {
    // Jump here from the scalarized entry points that require additional stack space
    // for packing scalarized arguments and therefore already created the frame.
    bind(*verified_inline_entry_label);
  }
}

void C1_MacroAssembler::remove_frame(int frame_size_in_bytes, bool needs_stack_repair,
                                     int sp_inc_offset) {
  MacroAssembler::remove_frame(frame_size_in_bytes, needs_stack_repair, sp_inc_offset);
}

void C1_MacroAssembler::verified_entry() {
  // If we have to make this method not-entrant we'll overwrite its
  // first instruction with a jump.  For this action to be legal we
  // must ensure that this first instruction is a B, BL, NOP, BKPT,
  // SVC, HVC, or SMC.  Make it a NOP.
  nop();
  if (C1Breakpoint) brk(1);
}

int C1_MacroAssembler::scalarized_entry(const CompiledEntrySignature* ces, int frame_size_in_bytes, int bang_size_in_bytes, int sp_offset_for_orig_pc, Label& verified_inline_entry_label, bool is_inline_ro_entry) {
  assert(InlineTypePassFieldsAsArgs, "sanity");
  // Make sure there is enough stack space for this method's activation.
  assert(bang_size_in_bytes >= frame_size_in_bytes, "stack bang size incorrect");
  generate_stack_overflow_check(bang_size_in_bytes);

  GrowableArray<SigEntry>* sig    = &ces->sig();
  GrowableArray<SigEntry>* sig_cc = is_inline_ro_entry ? &ces->sig_cc_ro() : &ces->sig_cc();
  VMRegPair* regs      = ces->regs();
  VMRegPair* regs_cc   = is_inline_ro_entry ? ces->regs_cc_ro() : ces->regs_cc();
  int args_on_stack    = ces->args_on_stack();
  int args_on_stack_cc = is_inline_ro_entry ? ces->args_on_stack_cc_ro() : ces->args_on_stack_cc();

  assert(sig->length() <= sig_cc->length(), "Zero-sized inline class not allowed!");
  BasicType* sig_bt = NEW_RESOURCE_ARRAY(BasicType, sig_cc->length());
  int args_passed = sig->length();
  int args_passed_cc = SigEntry::fill_sig_bt(sig_cc, sig_bt);

  // Check if we need to extend the stack for packing
  int sp_inc = 0;
  if (args_on_stack > args_on_stack_cc) {
    sp_inc = extend_stack_for_inline_args(args_on_stack);
  }

  // Create a temp frame so we can call into the runtime. It must be properly set up to accommodate GC.
  build_frame_helper(frame_size_in_bytes, sp_inc, ces->c1_needs_stack_repair());

  // Initialize orig_pc to detect deoptimization during buffering in below runtime call
  str(zr, Address(sp, sp_offset_for_orig_pc));

  // The runtime call might safepoint, make sure nmethod entry barrier is executed
  BarrierSetAssembler* bs = BarrierSet::barrier_set()->barrier_set_assembler();
  bs->nmethod_entry_barrier(this);

  // The runtime call returns the new array in r0 which is also j_rarg7
  // so we must avoid clobbering that. Temporarily save r0 in a
  // non-argument register and pass the buffered array in r20 instead.
  // This is safe because the runtime stub saves all registers.
  Register val_array = r20;
  Register tmp1 = r21;
  mov(tmp1, j_rarg7);

  // FIXME -- call runtime only if we cannot in-line allocate all the incoming inline type args.
  mov(r19, (intptr_t) ces->method());
  if (is_inline_ro_entry) {
    far_call(RuntimeAddress(Runtime1::entry_for(Runtime1::buffer_inline_args_no_receiver_id)));
  } else {
    far_call(RuntimeAddress(Runtime1::entry_for(Runtime1::buffer_inline_args_id)));
  }
  int rt_call_offset = offset();

  mov(val_array, r0);
  mov(j_rarg7, tmp1);

  // Remove the temp frame
  MacroAssembler::remove_frame(frame_size_in_bytes);

  shuffle_inline_args(true, is_inline_ro_entry, sig_cc,
                      args_passed_cc, args_on_stack_cc, regs_cc, // from
                      args_passed, args_on_stack, regs,          // to
                      sp_inc, val_array);

  if (ces->c1_needs_stack_repair()) {
    // Create the real frame. Below jump will then skip over the stack banging and frame
    // setup code in the verified_inline_entry (which has a different real_frame_size).
    build_frame_helper(frame_size_in_bytes, sp_inc, true);
  }

  b(verified_inline_entry_label);
  return rt_call_offset;
}


void C1_MacroAssembler::load_parameter(int offset_in_words, Register reg) {
  // rbp, + 0: link
  //     + 1: return address
  //     + 2: argument with offset 0
  //     + 3: argument with offset 1
  //     + 4: ...

  ldr(reg, Address(rfp, (offset_in_words + 2) * BytesPerWord));
}

#ifndef PRODUCT

void C1_MacroAssembler::verify_stack_oop(int stack_offset) {
  if (!VerifyOops) return;
  verify_oop_addr(Address(sp, stack_offset), "oop");
}

void C1_MacroAssembler::verify_not_null_oop(Register r) {
  if (!VerifyOops) return;
  Label not_null;
  cbnz(r, not_null);
  stop("non-null oop required");
  bind(not_null);
  verify_oop(r);
}

void C1_MacroAssembler::invalidate_registers(bool inv_r0, bool inv_r19, bool inv_r2, bool inv_r3, bool inv_r4, bool inv_r5) {
#ifdef ASSERT
  static int nn;
  if (inv_r0) mov(r0, 0xDEAD);
  if (inv_r19) mov(r19, 0xDEAD);
  if (inv_r2) mov(r2, nn++);
  if (inv_r3) mov(r3, 0xDEAD);
  if (inv_r4) mov(r4, 0xDEAD);
  if (inv_r5) mov(r5, 0xDEAD);
#endif
}
#endif // ifndef PRODUCT
