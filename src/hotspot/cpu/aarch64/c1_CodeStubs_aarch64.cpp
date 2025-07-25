/*
 * Copyright (c) 1999, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "asm/macroAssembler.inline.hpp"
#include "c1/c1_CodeStubs.hpp"
#include "c1/c1_FrameMap.hpp"
#include "c1/c1_LIRAssembler.hpp"
#include "c1/c1_MacroAssembler.hpp"
#include "c1/c1_Runtime1.hpp"
#include "classfile/javaClasses.hpp"
#include "nativeInst_aarch64.hpp"
#include "runtime/sharedRuntime.hpp"
#include "vmreg_aarch64.inline.hpp"


#define __ ce->masm()->

void C1SafepointPollStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  InternalAddress safepoint_pc(ce->masm()->pc() - ce->masm()->offset() + safepoint_offset());
  __ adr(rscratch1, safepoint_pc);
  __ str(rscratch1, Address(rthread, JavaThread::saved_exception_pc_offset()));

  assert(SharedRuntime::polling_page_return_handler_blob() != nullptr,
         "polling page return stub not created yet");
  address stub = SharedRuntime::polling_page_return_handler_blob()->entry_point();

  __ far_jump(RuntimeAddress(stub));
}

void CounterOverflowStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  Metadata *m = _method->as_constant_ptr()->as_metadata();
  __ mov_metadata(rscratch1, m);
  ce->store_parameter(rscratch1, 1);
  ce->store_parameter(_bci, 0);
  __ far_call(RuntimeAddress(Runtime1::entry_for(C1StubId::counter_overflow_id)));
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  __ b(_continuation);
}

void RangeCheckStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  if (_info->deoptimize_on_exception()) {
    address a = Runtime1::entry_for(C1StubId::predicate_failed_trap_id);
    __ far_call(RuntimeAddress(a));
    ce->add_call_info_here(_info);
    ce->verify_oop_map(_info);
    DEBUG_ONLY(__ should_not_reach_here());
    return;
  }

  if (_index->is_cpu_register()) {
    __ mov(rscratch1, _index->as_register());
  } else {
    __ mov(rscratch1, _index->as_jint());
  }
  C1StubId stub_id;
  if (_throw_index_out_of_bounds_exception) {
    stub_id = C1StubId::throw_index_exception_id;
  } else {
    assert(_array != LIR_Opr::nullOpr(), "sanity");
    __ mov(rscratch2, _array->as_pointer_register());
    stub_id = C1StubId::throw_range_check_failed_id;
  }
  __ lea(lr, RuntimeAddress(Runtime1::entry_for(stub_id)));
  __ blr(lr);
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  DEBUG_ONLY(__ should_not_reach_here());
}

PredicateFailedStub::PredicateFailedStub(CodeEmitInfo* info) {
  _info = new CodeEmitInfo(info);
}

void PredicateFailedStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  address a = Runtime1::entry_for(C1StubId::predicate_failed_trap_id);
  __ far_call(RuntimeAddress(a));
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  DEBUG_ONLY(__ should_not_reach_here());
}

void DivByZeroStub::emit_code(LIR_Assembler* ce) {
  if (_offset != -1) {
    ce->compilation()->implicit_exception_table()->append(_offset, __ offset());
  }
  __ bind(_entry);
  __ far_call(RuntimeAddress(Runtime1::entry_for(C1StubId::throw_div0_exception_id)));
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
#ifdef ASSERT
  __ should_not_reach_here();
#endif
}

// Implementation of LoadFlattenedArrayStub

LoadFlattenedArrayStub::LoadFlattenedArrayStub(LIR_Opr array, LIR_Opr index, LIR_Opr result, CodeEmitInfo* info) {
  _array = array;
  _index = index;
  _result = result;
  _scratch_reg = FrameMap::r0_oop_opr;
  _info = new CodeEmitInfo(info);
}

void LoadFlattenedArrayStub::emit_code(LIR_Assembler* ce) {
  assert(__ rsp_offset() == 0, "frame size should be fixed");
  __ bind(_entry);
  ce->store_parameter(_array->as_register(), 1);
  ce->store_parameter(_index->as_register(), 0);
  __ far_call(RuntimeAddress(Runtime1::entry_for(C1StubId::load_flat_array_id)));
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  if (_result->as_register() != r0) {
    __ mov(_result->as_register(), r0);
  }
  __ b(_continuation);
}


// Implementation of StoreFlattenedArrayStub

StoreFlattenedArrayStub::StoreFlattenedArrayStub(LIR_Opr array, LIR_Opr index, LIR_Opr value, CodeEmitInfo* info) {
  _array = array;
  _index = index;
  _value = value;
  _scratch_reg = FrameMap::r0_oop_opr;
  _info = new CodeEmitInfo(info);
}


void StoreFlattenedArrayStub::emit_code(LIR_Assembler* ce) {
  assert(__ rsp_offset() == 0, "frame size should be fixed");
  __ bind(_entry);
  ce->store_parameter(_array->as_register(), 2);
  ce->store_parameter(_index->as_register(), 1);
  ce->store_parameter(_value->as_register(), 0);
  __ far_call(RuntimeAddress(Runtime1::entry_for(C1StubId::store_flat_array_id)));
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  __ b(_continuation);
}

// Implementation of SubstitutabilityCheckStub
SubstitutabilityCheckStub::SubstitutabilityCheckStub(LIR_Opr left, LIR_Opr right, CodeEmitInfo* info) {
  _left = left;
  _right = right;
  _scratch_reg = FrameMap::r0_oop_opr;
  _info = new CodeEmitInfo(info);
}

void SubstitutabilityCheckStub::emit_code(LIR_Assembler* ce) {
  assert(__ rsp_offset() == 0, "frame size should be fixed");
  __ bind(_entry);
  ce->store_parameter(_left->as_register(), 1);
  ce->store_parameter(_right->as_register(), 0);
  __ far_call(RuntimeAddress(Runtime1::entry_for(C1StubId::substitutability_check_id)));
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  __ b(_continuation);
}


// Implementation of NewInstanceStub

NewInstanceStub::NewInstanceStub(LIR_Opr klass_reg, LIR_Opr result, ciInstanceKlass* klass, CodeEmitInfo* info, C1StubId stub_id) {
  _result = result;
  _klass = klass;
  _klass_reg = klass_reg;
  _info = new CodeEmitInfo(info);
  assert(stub_id == C1StubId::new_instance_id                 ||
         stub_id == C1StubId::fast_new_instance_id            ||
         stub_id == C1StubId::fast_new_instance_init_check_id,
         "need new_instance id");
  _stub_id   = stub_id;
}

void NewInstanceStub::emit_code(LIR_Assembler* ce) {
  assert(__ rsp_offset() == 0, "frame size should be fixed");
  __ bind(_entry);
  __ mov(r3, _klass_reg->as_register());
  __ far_call(RuntimeAddress(Runtime1::entry_for(_stub_id)));
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  assert(_result->as_register() == r0, "result must in r0,");
  __ b(_continuation);
}


// Implementation of NewTypeArrayStub

// Implementation of NewTypeArrayStub

NewTypeArrayStub::NewTypeArrayStub(LIR_Opr klass_reg, LIR_Opr length, LIR_Opr result, CodeEmitInfo* info) {
  _klass_reg = klass_reg;
  _length = length;
  _result = result;
  _info = new CodeEmitInfo(info);
}


void NewTypeArrayStub::emit_code(LIR_Assembler* ce) {
  assert(__ rsp_offset() == 0, "frame size should be fixed");
  __ bind(_entry);
  assert(_length->as_register() == r19, "length must in r19,");
  assert(_klass_reg->as_register() == r3, "klass_reg must in r3");
  __ far_call(RuntimeAddress(Runtime1::entry_for(C1StubId::new_type_array_id)));
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  assert(_result->as_register() == r0, "result must in r0");
  __ b(_continuation);
}


// Implementation of NewObjectArrayStub

NewObjectArrayStub::NewObjectArrayStub(LIR_Opr klass_reg, LIR_Opr length, LIR_Opr result,
                                       CodeEmitInfo* info, bool is_null_free) {
  _klass_reg = klass_reg;
  _result = result;
  _length = length;
  _info = new CodeEmitInfo(info);
  _is_null_free = is_null_free;
}


void NewObjectArrayStub::emit_code(LIR_Assembler* ce) {
  assert(__ rsp_offset() == 0, "frame size should be fixed");
  __ bind(_entry);
  assert(_length->as_register() == r19, "length must in r19,");
  assert(_klass_reg->as_register() == r3, "klass_reg must in r3");

  if (_is_null_free) {
    __ far_call(RuntimeAddress(Runtime1::entry_for(C1StubId::new_null_free_array_id)));
  } else {
    __ far_call(RuntimeAddress(Runtime1::entry_for(C1StubId::new_object_array_id)));
  }

  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  assert(_result->as_register() == r0, "result must in r0");
  __ b(_continuation);
}

void MonitorEnterStub::emit_code(LIR_Assembler* ce) {
  assert(__ rsp_offset() == 0, "frame size should be fixed");
  __ bind(_entry);
  if (_throw_ie_stub != nullptr) {
    // When we come here, _obj_reg has already been checked to be non-null.
    __ ldr(rscratch1, Address(_obj_reg->as_register(), oopDesc::mark_offset_in_bytes()));
    __ mov(rscratch2, markWord::inline_type_pattern);
    __ andr(rscratch1, rscratch1, rscratch2);

    __ cmp(rscratch1, rscratch2);
    __ br(Assembler::EQ, *_throw_ie_stub->entry());
  }

  ce->store_parameter(_obj_reg->as_register(),  1);
  ce->store_parameter(_lock_reg->as_register(), 0);
  C1StubId enter_id;
  if (ce->compilation()->has_fpu_code()) {
    enter_id = C1StubId::monitorenter_id;
  } else {
    enter_id = C1StubId::monitorenter_nofpu_id;
  }
  __ far_call(RuntimeAddress(Runtime1::entry_for(enter_id)));
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  __ b(_continuation);
}


void MonitorExitStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  if (_compute_lock) {
    // lock_reg was destroyed by fast unlocking attempt => recompute it
    ce->monitor_address(_monitor_ix, _lock_reg);
  }
  ce->store_parameter(_lock_reg->as_register(), 0);
  // note: non-blocking leaf routine => no call info needed
  C1StubId exit_id;
  if (ce->compilation()->has_fpu_code()) {
    exit_id = C1StubId::monitorexit_id;
  } else {
    exit_id = C1StubId::monitorexit_nofpu_id;
  }
  __ adr(lr, _continuation);
  __ far_jump(RuntimeAddress(Runtime1::entry_for(exit_id)));
}


// Implementation of patching:
// - Copy the code at given offset to an inlined buffer (first the bytes, then the number of bytes)
// - Replace original code with a call to the stub
// At Runtime:
// - call to stub, jump to runtime
// - in runtime: preserve all registers (rspecially objects, i.e., source and destination object)
// - in runtime: after initializing class, restore original code, reexecute instruction

int PatchingStub::_patch_info_offset = -NativeGeneralJump::instruction_size;

void PatchingStub::align_patch_site(MacroAssembler* masm) {
}

void PatchingStub::emit_code(LIR_Assembler* ce) {
  assert(false, "AArch64 should not use C1 runtime patching");
}


void DeoptimizeStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  ce->store_parameter(_trap_request, 0);
  __ far_call(RuntimeAddress(Runtime1::entry_for(C1StubId::deoptimize_id)));
  ce->add_call_info_here(_info);
  DEBUG_ONLY(__ should_not_reach_here());
}


void ImplicitNullCheckStub::emit_code(LIR_Assembler* ce) {
  address a;
  if (_info->deoptimize_on_exception()) {
    // Deoptimize, do not throw the exception, because it is probably wrong to do it here.
    a = Runtime1::entry_for(C1StubId::predicate_failed_trap_id);
  } else {
    a = Runtime1::entry_for(C1StubId::throw_null_pointer_exception_id);
  }

  ce->compilation()->implicit_exception_table()->append(_offset, __ offset());
  __ bind(_entry);
  __ far_call(RuntimeAddress(a));
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  DEBUG_ONLY(__ should_not_reach_here());
}


void SimpleExceptionStub::emit_code(LIR_Assembler* ce) {
  assert(__ rsp_offset() == 0, "frame size should be fixed");

  __ bind(_entry);
  // pass the object in a scratch register because all other registers
  // must be preserved
  if (_obj->is_cpu_register()) {
    __ mov(rscratch1, _obj->as_register());
  }
  __ far_call(RuntimeAddress(Runtime1::entry_for(_stub)), rscratch2);
  ce->add_call_info_here(_info);
  DEBUG_ONLY(__ should_not_reach_here());
}


void ArrayCopyStub::emit_code(LIR_Assembler* ce) {
  //---------------slow case: call to native-----------------
  __ bind(_entry);
  // Figure out where the args should go
  // This should really convert the IntrinsicID to the Method* and signature
  // but I don't know how to do that.
  //
  VMRegPair args[5];
  BasicType signature[5] = { T_OBJECT, T_INT, T_OBJECT, T_INT, T_INT};
  SharedRuntime::java_calling_convention(signature, args, 5);

  // push parameters
  // (src, src_pos, dest, destPos, length)
  Register r[5];
  r[0] = src()->as_register();
  r[1] = src_pos()->as_register();
  r[2] = dst()->as_register();
  r[3] = dst_pos()->as_register();
  r[4] = length()->as_register();

  // next registers will get stored on the stack
  for (int i = 0; i < 5 ; i++ ) {
    VMReg r_1 = args[i].first();
    if (r_1->is_stack()) {
      int st_off = r_1->reg2stack() * wordSize;
      __ str (r[i], Address(sp, st_off));
    } else {
      assert(r[i] == args[i].first()->as_Register(), "Wrong register for arg ");
    }
  }

  ce->align_call(lir_static_call);

  ce->emit_static_call_stub();
  if (ce->compilation()->bailed_out()) {
    return; // CodeCache is full
  }
  Address resolve(SharedRuntime::get_resolve_static_call_stub(),
                  relocInfo::static_call_type);
  address call = __ trampoline_call(resolve);
  if (call == nullptr) {
    ce->bailout("trampoline stub overflow");
    return;
  }
  ce->add_call_info_here(info());

#ifndef PRODUCT
  if (PrintC1Statistics) {
    __ lea(rscratch2, ExternalAddress((address)&Runtime1::_arraycopy_slowcase_cnt));
    __ incrementw(Address(rscratch2));
  }
#endif

  __ b(_continuation);
}

#undef __
