/*
 * Copyright (c) 1997, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "classfile/classLoader.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/stringTable.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "code/codeCache.hpp"
#include "code/compiledIC.hpp"
#include "code/nmethod.inline.hpp"
#include "code/scopeDesc.hpp"
#include "code/vtableStubs.hpp"
#include "compiler/abstractCompiler.hpp"
#include "compiler/compileBroker.hpp"
#include "compiler/disassembler.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "jvm.h"
#include "jfr/jfrEvents.hpp"
#include "logging/log.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/access.hpp"
#include "oops/fieldStreams.inline.hpp"
#include "metaprogramming/primitiveConversions.hpp"
#include "oops/klass.hpp"
#include "oops/method.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/inlineKlass.inline.hpp"
#include "prims/forte.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/jvmtiThreadState.hpp"
#include "prims/methodHandles.hpp"
#include "prims/nativeLookup.hpp"
#include "runtime/arguments.hpp"
#include "runtime/atomic.hpp"
#include "runtime/basicLock.inline.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/init.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/perfData.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stackWatermarkSet.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/synchronizer.inline.hpp"
#include "runtime/timerTrace.hpp"
#include "runtime/vframe.inline.hpp"
#include "runtime/vframeArray.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/copy.hpp"
#include "utilities/dtrace.hpp"
#include "utilities/events.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/resourceHash.hpp"
#include "utilities/macros.hpp"
#include "utilities/xmlstream.hpp"
#ifdef COMPILER1
#include "c1/c1_Runtime1.hpp"
#endif
#if INCLUDE_JFR
#include "jfr/jfr.hpp"
#endif

// Shared runtime stub routines reside in their own unique blob with a
// single entry point


#define SHARED_STUB_FIELD_DEFINE(name, type) \
  type        SharedRuntime::BLOB_FIELD_NAME(name);
  SHARED_STUBS_DO(SHARED_STUB_FIELD_DEFINE)
#undef SHARED_STUB_FIELD_DEFINE

nmethod*            SharedRuntime::_cont_doYield_stub;

#define SHARED_STUB_NAME_DECLARE(name, type) "Shared Runtime " # name "_blob",
const char *SharedRuntime::_stub_names[] = {
  SHARED_STUBS_DO(SHARED_STUB_NAME_DECLARE)
};

//----------------------------generate_stubs-----------------------------------
void SharedRuntime::generate_initial_stubs() {
  // Build this early so it's available for the interpreter.
  _throw_StackOverflowError_blob =
    generate_throw_exception(SharedStubId::throw_StackOverflowError_id,
                             CAST_FROM_FN_PTR(address, SharedRuntime::throw_StackOverflowError));
}

void SharedRuntime::generate_stubs() {
  _wrong_method_blob =
    generate_resolve_blob(SharedStubId::wrong_method_id,
                          CAST_FROM_FN_PTR(address, SharedRuntime::handle_wrong_method));
  _wrong_method_abstract_blob =
    generate_resolve_blob(SharedStubId::wrong_method_abstract_id,
                          CAST_FROM_FN_PTR(address, SharedRuntime::handle_wrong_method_abstract));
  _ic_miss_blob =
    generate_resolve_blob(SharedStubId::ic_miss_id,
                          CAST_FROM_FN_PTR(address, SharedRuntime::handle_wrong_method_ic_miss));
  _resolve_opt_virtual_call_blob =
    generate_resolve_blob(SharedStubId::resolve_opt_virtual_call_id,
                          CAST_FROM_FN_PTR(address, SharedRuntime::resolve_opt_virtual_call_C));
  _resolve_virtual_call_blob =
    generate_resolve_blob(SharedStubId::resolve_virtual_call_id,
                          CAST_FROM_FN_PTR(address, SharedRuntime::resolve_virtual_call_C));
  _resolve_static_call_blob =
    generate_resolve_blob(SharedStubId::resolve_static_call_id,
                          CAST_FROM_FN_PTR(address, SharedRuntime::resolve_static_call_C));

  _throw_delayed_StackOverflowError_blob =
    generate_throw_exception(SharedStubId::throw_delayed_StackOverflowError_id,
                             CAST_FROM_FN_PTR(address, SharedRuntime::throw_delayed_StackOverflowError));

  _throw_AbstractMethodError_blob =
    generate_throw_exception(SharedStubId::throw_AbstractMethodError_id,
                             CAST_FROM_FN_PTR(address, SharedRuntime::throw_AbstractMethodError));

  _throw_IncompatibleClassChangeError_blob =
    generate_throw_exception(SharedStubId::throw_IncompatibleClassChangeError_id,
                             CAST_FROM_FN_PTR(address, SharedRuntime::throw_IncompatibleClassChangeError));

  _throw_NullPointerException_at_call_blob =
    generate_throw_exception(SharedStubId::throw_NullPointerException_at_call_id,
                             CAST_FROM_FN_PTR(address, SharedRuntime::throw_NullPointerException_at_call));

  AdapterHandlerLibrary::initialize();

#if COMPILER2_OR_JVMCI
  // Vectors are generated only by C2 and JVMCI.
  bool support_wide = is_wide_vector(MaxVectorSize);
  if (support_wide) {
    _polling_page_vectors_safepoint_handler_blob =
      generate_handler_blob(SharedStubId::polling_page_vectors_safepoint_handler_id,
                            CAST_FROM_FN_PTR(address, SafepointSynchronize::handle_polling_page_exception));
  }
#endif // COMPILER2_OR_JVMCI
  _polling_page_safepoint_handler_blob =
    generate_handler_blob(SharedStubId::polling_page_safepoint_handler_id,
                          CAST_FROM_FN_PTR(address, SafepointSynchronize::handle_polling_page_exception));
  _polling_page_return_handler_blob =
    generate_handler_blob(SharedStubId::polling_page_return_handler_id,
                          CAST_FROM_FN_PTR(address, SafepointSynchronize::handle_polling_page_exception));

  generate_deopt_blob();
}

#if INCLUDE_JFR
//------------------------------generate jfr runtime stubs ------
void SharedRuntime::generate_jfr_stubs() {
  ResourceMark rm;
  const char* timer_msg = "SharedRuntime generate_jfr_stubs";
  TraceTime timer(timer_msg, TRACETIME_LOG(Info, startuptime));

  _jfr_write_checkpoint_blob = generate_jfr_write_checkpoint();
  _jfr_return_lease_blob = generate_jfr_return_lease();
}

#endif // INCLUDE_JFR

#include <math.h>

// Implementation of SharedRuntime

#ifndef PRODUCT
// For statistics
uint SharedRuntime::_ic_miss_ctr = 0;
uint SharedRuntime::_wrong_method_ctr = 0;
uint SharedRuntime::_resolve_static_ctr = 0;
uint SharedRuntime::_resolve_virtual_ctr = 0;
uint SharedRuntime::_resolve_opt_virtual_ctr = 0;
uint SharedRuntime::_implicit_null_throws = 0;
uint SharedRuntime::_implicit_div0_throws = 0;

int64_t SharedRuntime::_nof_normal_calls = 0;
int64_t SharedRuntime::_nof_inlined_calls = 0;
int64_t SharedRuntime::_nof_megamorphic_calls = 0;
int64_t SharedRuntime::_nof_static_calls = 0;
int64_t SharedRuntime::_nof_inlined_static_calls = 0;
int64_t SharedRuntime::_nof_interface_calls = 0;
int64_t SharedRuntime::_nof_inlined_interface_calls = 0;

uint SharedRuntime::_new_instance_ctr=0;
uint SharedRuntime::_new_array_ctr=0;
uint SharedRuntime::_multi2_ctr=0;
uint SharedRuntime::_multi3_ctr=0;
uint SharedRuntime::_multi4_ctr=0;
uint SharedRuntime::_multi5_ctr=0;
uint SharedRuntime::_mon_enter_stub_ctr=0;
uint SharedRuntime::_mon_exit_stub_ctr=0;
uint SharedRuntime::_mon_enter_ctr=0;
uint SharedRuntime::_mon_exit_ctr=0;
uint SharedRuntime::_partial_subtype_ctr=0;
uint SharedRuntime::_jbyte_array_copy_ctr=0;
uint SharedRuntime::_jshort_array_copy_ctr=0;
uint SharedRuntime::_jint_array_copy_ctr=0;
uint SharedRuntime::_jlong_array_copy_ctr=0;
uint SharedRuntime::_oop_array_copy_ctr=0;
uint SharedRuntime::_checkcast_array_copy_ctr=0;
uint SharedRuntime::_unsafe_array_copy_ctr=0;
uint SharedRuntime::_generic_array_copy_ctr=0;
uint SharedRuntime::_slow_array_copy_ctr=0;
uint SharedRuntime::_find_handler_ctr=0;
uint SharedRuntime::_rethrow_ctr=0;
uint SharedRuntime::_unsafe_set_memory_ctr=0;

int     SharedRuntime::_ICmiss_index                    = 0;
int     SharedRuntime::_ICmiss_count[SharedRuntime::maxICmiss_count];
address SharedRuntime::_ICmiss_at[SharedRuntime::maxICmiss_count];


void SharedRuntime::trace_ic_miss(address at) {
  for (int i = 0; i < _ICmiss_index; i++) {
    if (_ICmiss_at[i] == at) {
      _ICmiss_count[i]++;
      return;
    }
  }
  int index = _ICmiss_index++;
  if (_ICmiss_index >= maxICmiss_count) _ICmiss_index = maxICmiss_count - 1;
  _ICmiss_at[index] = at;
  _ICmiss_count[index] = 1;
}

void SharedRuntime::print_ic_miss_histogram() {
  if (ICMissHistogram) {
    tty->print_cr("IC Miss Histogram:");
    int tot_misses = 0;
    for (int i = 0; i < _ICmiss_index; i++) {
      tty->print_cr("  at: " INTPTR_FORMAT "  nof: %d", p2i(_ICmiss_at[i]), _ICmiss_count[i]);
      tot_misses += _ICmiss_count[i];
    }
    tty->print_cr("Total IC misses: %7d", tot_misses);
  }
}
#endif // PRODUCT


JRT_LEAF(jlong, SharedRuntime::lmul(jlong y, jlong x))
  return x * y;
JRT_END


JRT_LEAF(jlong, SharedRuntime::ldiv(jlong y, jlong x))
  if (x == min_jlong && y == CONST64(-1)) {
    return x;
  } else {
    return x / y;
  }
JRT_END


JRT_LEAF(jlong, SharedRuntime::lrem(jlong y, jlong x))
  if (x == min_jlong && y == CONST64(-1)) {
    return 0;
  } else {
    return x % y;
  }
JRT_END


#ifdef _WIN64
const juint  float_sign_mask  = 0x7FFFFFFF;
const juint  float_infinity   = 0x7F800000;
const julong double_sign_mask = CONST64(0x7FFFFFFFFFFFFFFF);
const julong double_infinity  = CONST64(0x7FF0000000000000);
#endif

#if !defined(X86)
JRT_LEAF(jfloat, SharedRuntime::frem(jfloat x, jfloat y))
#ifdef _WIN64
  // 64-bit Windows on amd64 returns the wrong values for
  // infinity operands.
  juint xbits = PrimitiveConversions::cast<juint>(x);
  juint ybits = PrimitiveConversions::cast<juint>(y);
  // x Mod Infinity == x unless x is infinity
  if (((xbits & float_sign_mask) != float_infinity) &&
       ((ybits & float_sign_mask) == float_infinity) ) {
    return x;
  }
  return ((jfloat)fmod_winx64((double)x, (double)y));
#else
  return ((jfloat)fmod((double)x,(double)y));
#endif
JRT_END

JRT_LEAF(jdouble, SharedRuntime::drem(jdouble x, jdouble y))
#ifdef _WIN64
  julong xbits = PrimitiveConversions::cast<julong>(x);
  julong ybits = PrimitiveConversions::cast<julong>(y);
  // x Mod Infinity == x unless x is infinity
  if (((xbits & double_sign_mask) != double_infinity) &&
       ((ybits & double_sign_mask) == double_infinity) ) {
    return x;
  }
  return ((jdouble)fmod_winx64((double)x, (double)y));
#else
  return ((jdouble)fmod((double)x,(double)y));
#endif
JRT_END
#endif // !X86

JRT_LEAF(jfloat, SharedRuntime::i2f(jint x))
  return (jfloat)x;
JRT_END

#ifdef __SOFTFP__
JRT_LEAF(jfloat, SharedRuntime::fadd(jfloat x, jfloat y))
  return x + y;
JRT_END

JRT_LEAF(jfloat, SharedRuntime::fsub(jfloat x, jfloat y))
  return x - y;
JRT_END

JRT_LEAF(jfloat, SharedRuntime::fmul(jfloat x, jfloat y))
  return x * y;
JRT_END

JRT_LEAF(jfloat, SharedRuntime::fdiv(jfloat x, jfloat y))
  return x / y;
JRT_END

JRT_LEAF(jdouble, SharedRuntime::dadd(jdouble x, jdouble y))
  return x + y;
JRT_END

JRT_LEAF(jdouble, SharedRuntime::dsub(jdouble x, jdouble y))
  return x - y;
JRT_END

JRT_LEAF(jdouble, SharedRuntime::dmul(jdouble x, jdouble y))
  return x * y;
JRT_END

JRT_LEAF(jdouble, SharedRuntime::ddiv(jdouble x, jdouble y))
  return x / y;
JRT_END

JRT_LEAF(jdouble, SharedRuntime::i2d(jint x))
  return (jdouble)x;
JRT_END

JRT_LEAF(jdouble, SharedRuntime::f2d(jfloat x))
  return (jdouble)x;
JRT_END

JRT_LEAF(int,  SharedRuntime::fcmpl(float x, float y))
  return x>y ? 1 : (x==y ? 0 : -1);  /* x<y or is_nan*/
JRT_END

JRT_LEAF(int,  SharedRuntime::fcmpg(float x, float y))
  return x<y ? -1 : (x==y ? 0 : 1);  /* x>y or is_nan */
JRT_END

JRT_LEAF(int,  SharedRuntime::dcmpl(double x, double y))
  return x>y ? 1 : (x==y ? 0 : -1); /* x<y or is_nan */
JRT_END

JRT_LEAF(int,  SharedRuntime::dcmpg(double x, double y))
  return x<y ? -1 : (x==y ? 0 : 1);  /* x>y or is_nan */
JRT_END

// Functions to return the opposite of the aeabi functions for nan.
JRT_LEAF(int, SharedRuntime::unordered_fcmplt(float x, float y))
  return (x < y) ? 1 : ((g_isnan(x) || g_isnan(y)) ? 1 : 0);
JRT_END

JRT_LEAF(int, SharedRuntime::unordered_dcmplt(double x, double y))
  return (x < y) ? 1 : ((g_isnan(x) || g_isnan(y)) ? 1 : 0);
JRT_END

JRT_LEAF(int, SharedRuntime::unordered_fcmple(float x, float y))
  return (x <= y) ? 1 : ((g_isnan(x) || g_isnan(y)) ? 1 : 0);
JRT_END

JRT_LEAF(int, SharedRuntime::unordered_dcmple(double x, double y))
  return (x <= y) ? 1 : ((g_isnan(x) || g_isnan(y)) ? 1 : 0);
JRT_END

JRT_LEAF(int, SharedRuntime::unordered_fcmpge(float x, float y))
  return (x >= y) ? 1 : ((g_isnan(x) || g_isnan(y)) ? 1 : 0);
JRT_END

JRT_LEAF(int, SharedRuntime::unordered_dcmpge(double x, double y))
  return (x >= y) ? 1 : ((g_isnan(x) || g_isnan(y)) ? 1 : 0);
JRT_END

JRT_LEAF(int, SharedRuntime::unordered_fcmpgt(float x, float y))
  return (x > y) ? 1 : ((g_isnan(x) || g_isnan(y)) ? 1 : 0);
JRT_END

JRT_LEAF(int, SharedRuntime::unordered_dcmpgt(double x, double y))
  return (x > y) ? 1 : ((g_isnan(x) || g_isnan(y)) ? 1 : 0);
JRT_END

// Intrinsics make gcc generate code for these.
float  SharedRuntime::fneg(float f)   {
  return -f;
}

double SharedRuntime::dneg(double f)  {
  return -f;
}

#endif // __SOFTFP__

#if defined(__SOFTFP__) || defined(E500V2)
// Intrinsics make gcc generate code for these.
double SharedRuntime::dabs(double f)  {
  return (f <= (double)0.0) ? (double)0.0 - f : f;
}

#endif

#if defined(__SOFTFP__) || defined(PPC)
double SharedRuntime::dsqrt(double f) {
  return sqrt(f);
}
#endif

JRT_LEAF(jint, SharedRuntime::f2i(jfloat  x))
  if (g_isnan(x))
    return 0;
  if (x >= (jfloat) max_jint)
    return max_jint;
  if (x <= (jfloat) min_jint)
    return min_jint;
  return (jint) x;
JRT_END


JRT_LEAF(jlong, SharedRuntime::f2l(jfloat  x))
  if (g_isnan(x))
    return 0;
  if (x >= (jfloat) max_jlong)
    return max_jlong;
  if (x <= (jfloat) min_jlong)
    return min_jlong;
  return (jlong) x;
JRT_END


JRT_LEAF(jint, SharedRuntime::d2i(jdouble x))
  if (g_isnan(x))
    return 0;
  if (x >= (jdouble) max_jint)
    return max_jint;
  if (x <= (jdouble) min_jint)
    return min_jint;
  return (jint) x;
JRT_END


JRT_LEAF(jlong, SharedRuntime::d2l(jdouble x))
  if (g_isnan(x))
    return 0;
  if (x >= (jdouble) max_jlong)
    return max_jlong;
  if (x <= (jdouble) min_jlong)
    return min_jlong;
  return (jlong) x;
JRT_END


JRT_LEAF(jfloat, SharedRuntime::d2f(jdouble x))
  return (jfloat)x;
JRT_END


JRT_LEAF(jfloat, SharedRuntime::l2f(jlong x))
  return (jfloat)x;
JRT_END


JRT_LEAF(jdouble, SharedRuntime::l2d(jlong x))
  return (jdouble)x;
JRT_END


// Exception handling across interpreter/compiler boundaries
//
// exception_handler_for_return_address(...) returns the continuation address.
// The continuation address is the entry point of the exception handler of the
// previous frame depending on the return address.

address SharedRuntime::raw_exception_handler_for_return_address(JavaThread* current, address return_address) {
  // Note: This is called when we have unwound the frame of the callee that did
  // throw an exception. So far, no check has been performed by the StackWatermarkSet.
  // Notably, the stack is not walkable at this point, and hence the check must
  // be deferred until later. Specifically, any of the handlers returned here in
  // this function, will get dispatched to, and call deferred checks to
  // StackWatermarkSet::after_unwind at a point where the stack is walkable.
  assert(frame::verify_return_pc(return_address), "must be a return address: " INTPTR_FORMAT, p2i(return_address));
  assert(current->frames_to_pop_failed_realloc() == 0 || Interpreter::contains(return_address), "missed frames to pop?");

  // Reset method handle flag.
  current->set_is_method_handle_return(false);

#if INCLUDE_JVMCI
  // JVMCI's ExceptionHandlerStub expects the thread local exception PC to be clear
  // and other exception handler continuations do not read it
  current->set_exception_pc(nullptr);
#endif // INCLUDE_JVMCI

  if (Continuation::is_return_barrier_entry(return_address)) {
    return StubRoutines::cont_returnBarrierExc();
  }

  // The fastest case first
  CodeBlob* blob = CodeCache::find_blob(return_address);
  nmethod* nm = (blob != nullptr) ? blob->as_nmethod_or_null() : nullptr;
  if (nm != nullptr) {
    // Set flag if return address is a method handle call site.
    current->set_is_method_handle_return(nm->is_method_handle_return(return_address));
    // native nmethods don't have exception handlers
    assert(!nm->is_native_method() || nm->method()->is_continuation_enter_intrinsic(), "no exception handler");
    assert(nm->header_begin() != nm->exception_begin(), "no exception handler");
    if (nm->is_deopt_pc(return_address)) {
      // If we come here because of a stack overflow, the stack may be
      // unguarded. Reguard the stack otherwise if we return to the
      // deopt blob and the stack bang causes a stack overflow we
      // crash.
      StackOverflow* overflow_state = current->stack_overflow_state();
      bool guard_pages_enabled = overflow_state->reguard_stack_if_needed();
      if (overflow_state->reserved_stack_activation() != current->stack_base()) {
        overflow_state->set_reserved_stack_activation(current->stack_base());
      }
      assert(guard_pages_enabled, "stack banging in deopt blob may cause crash");
      // The deferred StackWatermarkSet::after_unwind check will be performed in
      // Deoptimization::fetch_unroll_info (with exec_mode == Unpack_exception)
      return SharedRuntime::deopt_blob()->unpack_with_exception();
    } else {
      // The deferred StackWatermarkSet::after_unwind check will be performed in
      // * OptoRuntime::handle_exception_C_helper for C2 code
      // * exception_handler_for_pc_helper via Runtime1::handle_exception_from_callee_id for C1 code
      return nm->exception_begin();
    }
  }

  // Entry code
  if (StubRoutines::returns_to_call_stub(return_address)) {
    // The deferred StackWatermarkSet::after_unwind check will be performed in
    // JavaCallWrapper::~JavaCallWrapper
    return StubRoutines::catch_exception_entry();
  }
  if (blob != nullptr && blob->is_upcall_stub()) {
    return StubRoutines::upcall_stub_exception_handler();
  }
  // Interpreted code
  if (Interpreter::contains(return_address)) {
    // The deferred StackWatermarkSet::after_unwind check will be performed in
    // InterpreterRuntime::exception_handler_for_exception
    return Interpreter::rethrow_exception_entry();
  }

  guarantee(blob == nullptr || !blob->is_runtime_stub(), "caller should have skipped stub");
  guarantee(!VtableStubs::contains(return_address), "null exceptions in vtables should have been handled already!");

#ifndef PRODUCT
  { ResourceMark rm;
    tty->print_cr("No exception handler found for exception at " INTPTR_FORMAT " - potential problems:", p2i(return_address));
    os::print_location(tty, (intptr_t)return_address);
    tty->print_cr("a) exception happened in (new?) code stubs/buffers that is not handled here");
    tty->print_cr("b) other problem");
  }
#endif // PRODUCT
  ShouldNotReachHere();
  return nullptr;
}


JRT_LEAF(address, SharedRuntime::exception_handler_for_return_address(JavaThread* current, address return_address))
  return raw_exception_handler_for_return_address(current, return_address);
JRT_END


address SharedRuntime::get_poll_stub(address pc) {
  address stub;
  // Look up the code blob
  CodeBlob *cb = CodeCache::find_blob(pc);

  // Should be an nmethod
  guarantee(cb != nullptr && cb->is_nmethod(), "safepoint polling: pc must refer to an nmethod");

  // Look up the relocation information
  assert(cb->as_nmethod()->is_at_poll_or_poll_return(pc),
      "safepoint polling: type must be poll at pc " INTPTR_FORMAT, p2i(pc));

#ifdef ASSERT
  if (!((NativeInstruction*)pc)->is_safepoint_poll()) {
    tty->print_cr("bad pc: " PTR_FORMAT, p2i(pc));
    Disassembler::decode(cb);
    fatal("Only polling locations are used for safepoint");
  }
#endif

  bool at_poll_return = cb->as_nmethod()->is_at_poll_return(pc);
  bool has_wide_vectors = cb->as_nmethod()->has_wide_vectors();
  if (at_poll_return) {
    assert(SharedRuntime::polling_page_return_handler_blob() != nullptr,
           "polling page return stub not created yet");
    stub = SharedRuntime::polling_page_return_handler_blob()->entry_point();
  } else if (has_wide_vectors) {
    assert(SharedRuntime::polling_page_vectors_safepoint_handler_blob() != nullptr,
           "polling page vectors safepoint stub not created yet");
    stub = SharedRuntime::polling_page_vectors_safepoint_handler_blob()->entry_point();
  } else {
    assert(SharedRuntime::polling_page_safepoint_handler_blob() != nullptr,
           "polling page safepoint stub not created yet");
    stub = SharedRuntime::polling_page_safepoint_handler_blob()->entry_point();
  }
  log_debug(safepoint)("... found polling page %s exception at pc = "
                       INTPTR_FORMAT ", stub =" INTPTR_FORMAT,
                       at_poll_return ? "return" : "loop",
                       (intptr_t)pc, (intptr_t)stub);
  return stub;
}

void SharedRuntime::throw_and_post_jvmti_exception(JavaThread* current, Handle h_exception) {
  if (JvmtiExport::can_post_on_exceptions()) {
    vframeStream vfst(current, true);
    methodHandle method = methodHandle(current, vfst.method());
    address bcp = method()->bcp_from(vfst.bci());
    JvmtiExport::post_exception_throw(current, method(), bcp, h_exception());
  }

#if INCLUDE_JVMCI
  if (EnableJVMCI) {
    vframeStream vfst(current, true);
    methodHandle method = methodHandle(current, vfst.method());
    int bci = vfst.bci();
    MethodData* trap_mdo = method->method_data();
    if (trap_mdo != nullptr) {
      // Set exception_seen if the exceptional bytecode is an invoke
      Bytecode_invoke call = Bytecode_invoke_check(method, bci);
      if (call.is_valid()) {
        ResourceMark rm(current);

        // Lock to read ProfileData, and ensure lock is not broken by a safepoint
        MutexLocker ml(trap_mdo->extra_data_lock(), Mutex::_no_safepoint_check_flag);

        ProfileData* pdata = trap_mdo->allocate_bci_to_data(bci, nullptr);
        if (pdata != nullptr && pdata->is_BitData()) {
          BitData* bit_data = (BitData*) pdata;
          bit_data->set_exception_seen();
        }
      }
    }
  }
#endif

  Exceptions::_throw(current, __FILE__, __LINE__, h_exception);
}

void SharedRuntime::throw_and_post_jvmti_exception(JavaThread* current, Symbol* name, const char *message) {
  Handle h_exception = Exceptions::new_exception(current, name, message);
  throw_and_post_jvmti_exception(current, h_exception);
}

#if INCLUDE_JVMTI
JRT_ENTRY(void, SharedRuntime::notify_jvmti_vthread_start(oopDesc* vt, jboolean hide, JavaThread* current))
  assert(hide == JNI_FALSE, "must be VTMS transition finish");
  jobject vthread = JNIHandles::make_local(const_cast<oopDesc*>(vt));
  JvmtiVTMSTransitionDisabler::VTMS_vthread_start(vthread);
  JNIHandles::destroy_local(vthread);
JRT_END

JRT_ENTRY(void, SharedRuntime::notify_jvmti_vthread_end(oopDesc* vt, jboolean hide, JavaThread* current))
  assert(hide == JNI_TRUE, "must be VTMS transition start");
  jobject vthread = JNIHandles::make_local(const_cast<oopDesc*>(vt));
  JvmtiVTMSTransitionDisabler::VTMS_vthread_end(vthread);
  JNIHandles::destroy_local(vthread);
JRT_END

JRT_ENTRY(void, SharedRuntime::notify_jvmti_vthread_mount(oopDesc* vt, jboolean hide, JavaThread* current))
  jobject vthread = JNIHandles::make_local(const_cast<oopDesc*>(vt));
  JvmtiVTMSTransitionDisabler::VTMS_vthread_mount(vthread, hide);
  JNIHandles::destroy_local(vthread);
JRT_END

JRT_ENTRY(void, SharedRuntime::notify_jvmti_vthread_unmount(oopDesc* vt, jboolean hide, JavaThread* current))
  jobject vthread = JNIHandles::make_local(const_cast<oopDesc*>(vt));
  JvmtiVTMSTransitionDisabler::VTMS_vthread_unmount(vthread, hide);
  JNIHandles::destroy_local(vthread);
JRT_END
#endif // INCLUDE_JVMTI

// The interpreter code to call this tracing function is only
// called/generated when UL is on for redefine, class and has the right level
// and tags. Since obsolete methods are never compiled, we don't have
// to modify the compilers to generate calls to this function.
//
JRT_LEAF(int, SharedRuntime::rc_trace_method_entry(
    JavaThread* thread, Method* method))
  if (method->is_obsolete()) {
    // We are calling an obsolete method, but this is not necessarily
    // an error. Our method could have been redefined just after we
    // fetched the Method* from the constant pool.
    ResourceMark rm;
    log_trace(redefine, class, obsolete)("calling obsolete method '%s'", method->name_and_sig_as_C_string());
  }
  return 0;
JRT_END

// ret_pc points into caller; we are returning caller's exception handler
// for given exception
// Note that the implementation of this method assumes it's only called when an exception has actually occured
address SharedRuntime::compute_compiled_exc_handler(nmethod* nm, address ret_pc, Handle& exception,
                                                    bool force_unwind, bool top_frame_only, bool& recursive_exception_occurred) {
  assert(nm != nullptr, "must exist");
  ResourceMark rm;

#if INCLUDE_JVMCI
  if (nm->is_compiled_by_jvmci()) {
    // lookup exception handler for this pc
    int catch_pco = pointer_delta_as_int(ret_pc, nm->code_begin());
    ExceptionHandlerTable table(nm);
    HandlerTableEntry *t = table.entry_for(catch_pco, -1, 0);
    if (t != nullptr) {
      return nm->code_begin() + t->pco();
    } else {
      return Deoptimization::deoptimize_for_missing_exception_handler(nm);
    }
  }
#endif // INCLUDE_JVMCI

  ScopeDesc* sd = nm->scope_desc_at(ret_pc);
  // determine handler bci, if any
  EXCEPTION_MARK;

  int handler_bci = -1;
  int scope_depth = 0;
  if (!force_unwind) {
    int bci = sd->bci();
    bool recursive_exception = false;
    do {
      bool skip_scope_increment = false;
      // exception handler lookup
      Klass* ek = exception->klass();
      methodHandle mh(THREAD, sd->method());
      handler_bci = Method::fast_exception_handler_bci_for(mh, ek, bci, THREAD);
      if (HAS_PENDING_EXCEPTION) {
        recursive_exception = true;
        // We threw an exception while trying to find the exception handler.
        // Transfer the new exception to the exception handle which will
        // be set into thread local storage, and do another lookup for an
        // exception handler for this exception, this time starting at the
        // BCI of the exception handler which caused the exception to be
        // thrown (bugs 4307310 and 4546590). Set "exception" reference
        // argument to ensure that the correct exception is thrown (4870175).
        recursive_exception_occurred = true;
        exception = Handle(THREAD, PENDING_EXCEPTION);
        CLEAR_PENDING_EXCEPTION;
        if (handler_bci >= 0) {
          bci = handler_bci;
          handler_bci = -1;
          skip_scope_increment = true;
        }
      }
      else {
        recursive_exception = false;
      }
      if (!top_frame_only && handler_bci < 0 && !skip_scope_increment) {
        sd = sd->sender();
        if (sd != nullptr) {
          bci = sd->bci();
        }
        ++scope_depth;
      }
    } while (recursive_exception || (!top_frame_only && handler_bci < 0 && sd != nullptr));
  }

  // found handling method => lookup exception handler
  int catch_pco = pointer_delta_as_int(ret_pc, nm->code_begin());

  ExceptionHandlerTable table(nm);
  HandlerTableEntry *t = table.entry_for(catch_pco, handler_bci, scope_depth);
  if (t == nullptr && (nm->is_compiled_by_c1() || handler_bci != -1)) {
    // Allow abbreviated catch tables.  The idea is to allow a method
    // to materialize its exceptions without committing to the exact
    // routing of exceptions.  In particular this is needed for adding
    // a synthetic handler to unlock monitors when inlining
    // synchronized methods since the unlock path isn't represented in
    // the bytecodes.
    t = table.entry_for(catch_pco, -1, 0);
  }

#ifdef COMPILER1
  if (t == nullptr && nm->is_compiled_by_c1()) {
    assert(nm->unwind_handler_begin() != nullptr, "");
    return nm->unwind_handler_begin();
  }
#endif

  if (t == nullptr) {
    ttyLocker ttyl;
    tty->print_cr("MISSING EXCEPTION HANDLER for pc " INTPTR_FORMAT " and handler bci %d, catch_pco: %d", p2i(ret_pc), handler_bci, catch_pco);
    tty->print_cr("   Exception:");
    exception->print();
    tty->cr();
    tty->print_cr(" Compiled exception table :");
    table.print();
    nm->print();
    nm->print_code();
    guarantee(false, "missing exception handler");
    return nullptr;
  }

  if (handler_bci != -1) { // did we find a handler in this method?
    sd->method()->set_exception_handler_entered(handler_bci); // profile
  }
  return nm->code_begin() + t->pco();
}

JRT_ENTRY(void, SharedRuntime::throw_AbstractMethodError(JavaThread* current))
  // These errors occur only at call sites
  throw_and_post_jvmti_exception(current, vmSymbols::java_lang_AbstractMethodError());
JRT_END

JRT_ENTRY(void, SharedRuntime::throw_IncompatibleClassChangeError(JavaThread* current))
  // These errors occur only at call sites
  throw_and_post_jvmti_exception(current, vmSymbols::java_lang_IncompatibleClassChangeError(), "vtable stub");
JRT_END

JRT_ENTRY(void, SharedRuntime::throw_ArithmeticException(JavaThread* current))
  throw_and_post_jvmti_exception(current, vmSymbols::java_lang_ArithmeticException(), "/ by zero");
JRT_END

JRT_ENTRY(void, SharedRuntime::throw_NullPointerException(JavaThread* current))
  throw_and_post_jvmti_exception(current, vmSymbols::java_lang_NullPointerException(), nullptr);
JRT_END

JRT_ENTRY(void, SharedRuntime::throw_NullPointerException_at_call(JavaThread* current))
  // This entry point is effectively only used for NullPointerExceptions which occur at inline
  // cache sites (when the callee activation is not yet set up) so we are at a call site
  throw_and_post_jvmti_exception(current, vmSymbols::java_lang_NullPointerException(), nullptr);
JRT_END

JRT_ENTRY(void, SharedRuntime::throw_StackOverflowError(JavaThread* current))
  throw_StackOverflowError_common(current, false);
JRT_END

JRT_ENTRY(void, SharedRuntime::throw_delayed_StackOverflowError(JavaThread* current))
  throw_StackOverflowError_common(current, true);
JRT_END

void SharedRuntime::throw_StackOverflowError_common(JavaThread* current, bool delayed) {
  // We avoid using the normal exception construction in this case because
  // it performs an upcall to Java, and we're already out of stack space.
  JavaThread* THREAD = current; // For exception macros.
  Klass* k = vmClasses::StackOverflowError_klass();
  oop exception_oop = InstanceKlass::cast(k)->allocate_instance(CHECK);
  if (delayed) {
    java_lang_Throwable::set_message(exception_oop,
                                     Universe::delayed_stack_overflow_error_message());
  }
  Handle exception (current, exception_oop);
  if (StackTraceInThrowable) {
    java_lang_Throwable::fill_in_stack_trace(exception);
  }
  // Remove the ScopedValue bindings in case we got a
  // StackOverflowError while we were trying to remove ScopedValue
  // bindings.
  current->clear_scopedValueBindings();
  // Increment counter for hs_err file reporting
  Atomic::inc(&Exceptions::_stack_overflow_errors);
  throw_and_post_jvmti_exception(current, exception);
}

address SharedRuntime::continuation_for_implicit_exception(JavaThread* current,
                                                           address pc,
                                                           ImplicitExceptionKind exception_kind)
{
  address target_pc = nullptr;

  if (Interpreter::contains(pc)) {
    switch (exception_kind) {
      case IMPLICIT_NULL:           return Interpreter::throw_NullPointerException_entry();
      case IMPLICIT_DIVIDE_BY_ZERO: return Interpreter::throw_ArithmeticException_entry();
      case STACK_OVERFLOW:          return Interpreter::throw_StackOverflowError_entry();
      default:                      ShouldNotReachHere();
    }
  } else {
    switch (exception_kind) {
      case STACK_OVERFLOW: {
        // Stack overflow only occurs upon frame setup; the callee is
        // going to be unwound. Dispatch to a shared runtime stub
        // which will cause the StackOverflowError to be fabricated
        // and processed.
        // Stack overflow should never occur during deoptimization:
        // the compiled method bangs the stack by as much as the
        // interpreter would need in case of a deoptimization. The
        // deoptimization blob and uncommon trap blob bang the stack
        // in a debug VM to verify the correctness of the compiled
        // method stack banging.
        assert(current->deopt_mark() == nullptr, "no stack overflow from deopt blob/uncommon trap");
        Events::log_exception(current, "StackOverflowError at " INTPTR_FORMAT, p2i(pc));
        return SharedRuntime::throw_StackOverflowError_entry();
      }

      case IMPLICIT_NULL: {
        if (VtableStubs::contains(pc)) {
          // We haven't yet entered the callee frame. Fabricate an
          // exception and begin dispatching it in the caller. Since
          // the caller was at a call site, it's safe to destroy all
          // caller-saved registers, as these entry points do.
          VtableStub* vt_stub = VtableStubs::stub_containing(pc);

          // If vt_stub is null, then return null to signal handler to report the SEGV error.
          if (vt_stub == nullptr) return nullptr;

          if (vt_stub->is_abstract_method_error(pc)) {
            assert(!vt_stub->is_vtable_stub(), "should never see AbstractMethodErrors from vtable-type VtableStubs");
            Events::log_exception(current, "AbstractMethodError at " INTPTR_FORMAT, p2i(pc));
            // Instead of throwing the abstract method error here directly, we re-resolve
            // and will throw the AbstractMethodError during resolve. As a result, we'll
            // get a more detailed error message.
            return SharedRuntime::get_handle_wrong_method_stub();
          } else {
            Events::log_exception(current, "NullPointerException at vtable entry " INTPTR_FORMAT, p2i(pc));
            // Assert that the signal comes from the expected location in stub code.
            assert(vt_stub->is_null_pointer_exception(pc),
                   "obtained signal from unexpected location in stub code");
            return SharedRuntime::throw_NullPointerException_at_call_entry();
          }
        } else {
          CodeBlob* cb = CodeCache::find_blob(pc);

          // If code blob is null, then return null to signal handler to report the SEGV error.
          if (cb == nullptr) return nullptr;

          // Exception happened in CodeCache. Must be either:
          // 1. Inline-cache check in C2I handler blob,
          // 2. Inline-cache check in nmethod, or
          // 3. Implicit null exception in nmethod

          if (!cb->is_nmethod()) {
            bool is_in_blob = cb->is_adapter_blob() || cb->is_method_handles_adapter_blob();
            if (!is_in_blob) {
              // Allow normal crash reporting to handle this
              return nullptr;
            }
            Events::log_exception(current, "NullPointerException in code blob at " INTPTR_FORMAT, p2i(pc));
            // There is no handler here, so we will simply unwind.
            return SharedRuntime::throw_NullPointerException_at_call_entry();
          }

          // Otherwise, it's a compiled method.  Consult its exception handlers.
          nmethod* nm = cb->as_nmethod();
          if (nm->inlinecache_check_contains(pc)) {
            // exception happened inside inline-cache check code
            // => the nmethod is not yet active (i.e., the frame
            // is not set up yet) => use return address pushed by
            // caller => don't push another return address
            Events::log_exception(current, "NullPointerException in IC check " INTPTR_FORMAT, p2i(pc));
            return SharedRuntime::throw_NullPointerException_at_call_entry();
          }

          if (nm->method()->is_method_handle_intrinsic()) {
            // exception happened inside MH dispatch code, similar to a vtable stub
            Events::log_exception(current, "NullPointerException in MH adapter " INTPTR_FORMAT, p2i(pc));
            return SharedRuntime::throw_NullPointerException_at_call_entry();
          }

#ifndef PRODUCT
          _implicit_null_throws++;
#endif
          target_pc = nm->continuation_for_implicit_null_exception(pc);
          // If there's an unexpected fault, target_pc might be null,
          // in which case we want to fall through into the normal
          // error handling code.
        }

        break; // fall through
      }


      case IMPLICIT_DIVIDE_BY_ZERO: {
        nmethod* nm = CodeCache::find_nmethod(pc);
        guarantee(nm != nullptr, "must have containing compiled method for implicit division-by-zero exceptions");
#ifndef PRODUCT
        _implicit_div0_throws++;
#endif
        target_pc = nm->continuation_for_implicit_div0_exception(pc);
        // If there's an unexpected fault, target_pc might be null,
        // in which case we want to fall through into the normal
        // error handling code.
        break; // fall through
      }

      default: ShouldNotReachHere();
    }

    assert(exception_kind == IMPLICIT_NULL || exception_kind == IMPLICIT_DIVIDE_BY_ZERO, "wrong implicit exception kind");

    if (exception_kind == IMPLICIT_NULL) {
#ifndef PRODUCT
      // for AbortVMOnException flag
      Exceptions::debug_check_abort("java.lang.NullPointerException");
#endif //PRODUCT
      Events::log_exception(current, "Implicit null exception at " INTPTR_FORMAT " to " INTPTR_FORMAT, p2i(pc), p2i(target_pc));
    } else {
#ifndef PRODUCT
      // for AbortVMOnException flag
      Exceptions::debug_check_abort("java.lang.ArithmeticException");
#endif //PRODUCT
      Events::log_exception(current, "Implicit division by zero exception at " INTPTR_FORMAT " to " INTPTR_FORMAT, p2i(pc), p2i(target_pc));
    }
    return target_pc;
  }

  ShouldNotReachHere();
  return nullptr;
}


/**
 * Throws an java/lang/UnsatisfiedLinkError.  The address of this method is
 * installed in the native function entry of all native Java methods before
 * they get linked to their actual native methods.
 *
 * \note
 * This method actually never gets called!  The reason is because
 * the interpreter's native entries call NativeLookup::lookup() which
 * throws the exception when the lookup fails.  The exception is then
 * caught and forwarded on the return from NativeLookup::lookup() call
 * before the call to the native function.  This might change in the future.
 */
JNI_ENTRY(void*, throw_unsatisfied_link_error(JNIEnv* env, ...))
{
  // We return a bad value here to make sure that the exception is
  // forwarded before we look at the return value.
  THROW_(vmSymbols::java_lang_UnsatisfiedLinkError(), (void*)badAddress);
}
JNI_END

address SharedRuntime::native_method_throw_unsatisfied_link_error_entry() {
  return CAST_FROM_FN_PTR(address, &throw_unsatisfied_link_error);
}

JRT_ENTRY_NO_ASYNC(void, SharedRuntime::register_finalizer(JavaThread* current, oopDesc* obj))
#if INCLUDE_JVMCI
  if (!obj->klass()->has_finalizer()) {
    return;
  }
#endif // INCLUDE_JVMCI
  assert(oopDesc::is_oop(obj), "must be a valid oop");
  assert(obj->klass()->has_finalizer(), "shouldn't be here otherwise");
  InstanceKlass::register_finalizer(instanceOop(obj), CHECK);
JRT_END

jlong SharedRuntime::get_java_tid(JavaThread* thread) {
  assert(thread != nullptr, "No thread");
  if (thread == nullptr) {
    return 0;
  }
  guarantee(Thread::current() != thread || thread->is_oop_safe(),
            "current cannot touch oops after its GC barrier is detached.");
  oop obj = thread->threadObj();
  return (obj == nullptr) ? 0 : java_lang_Thread::thread_id(obj);
}

/**
 * This function ought to be a void function, but cannot be because
 * it gets turned into a tail-call on sparc, which runs into dtrace bug
 * 6254741.  Once that is fixed we can remove the dummy return value.
 */
int SharedRuntime::dtrace_object_alloc(oopDesc* o) {
  return dtrace_object_alloc(JavaThread::current(), o, o->size());
}

int SharedRuntime::dtrace_object_alloc(JavaThread* thread, oopDesc* o) {
  return dtrace_object_alloc(thread, o, o->size());
}

int SharedRuntime::dtrace_object_alloc(JavaThread* thread, oopDesc* o, size_t size) {
  assert(DTraceAllocProbes, "wrong call");
  Klass* klass = o->klass();
  Symbol* name = klass->name();
  HOTSPOT_OBJECT_ALLOC(
                   get_java_tid(thread),
                   (char *) name->bytes(), name->utf8_length(), size * HeapWordSize);
  return 0;
}

JRT_LEAF(int, SharedRuntime::dtrace_method_entry(
    JavaThread* current, Method* method))
  assert(current == JavaThread::current(), "pre-condition");

  assert(DTraceMethodProbes, "wrong call");
  Symbol* kname = method->klass_name();
  Symbol* name = method->name();
  Symbol* sig = method->signature();
  HOTSPOT_METHOD_ENTRY(
      get_java_tid(current),
      (char *) kname->bytes(), kname->utf8_length(),
      (char *) name->bytes(), name->utf8_length(),
      (char *) sig->bytes(), sig->utf8_length());
  return 0;
JRT_END

JRT_LEAF(int, SharedRuntime::dtrace_method_exit(
    JavaThread* current, Method* method))
  assert(current == JavaThread::current(), "pre-condition");
  assert(DTraceMethodProbes, "wrong call");
  Symbol* kname = method->klass_name();
  Symbol* name = method->name();
  Symbol* sig = method->signature();
  HOTSPOT_METHOD_RETURN(
      get_java_tid(current),
      (char *) kname->bytes(), kname->utf8_length(),
      (char *) name->bytes(), name->utf8_length(),
      (char *) sig->bytes(), sig->utf8_length());
  return 0;
JRT_END


// Finds receiver, CallInfo (i.e. receiver method), and calling bytecode)
// for a call current in progress, i.e., arguments has been pushed on stack
// put callee has not been invoked yet.  Used by: resolve virtual/static,
// vtable updates, etc.  Caller frame must be compiled.
Handle SharedRuntime::find_callee_info(Bytecodes::Code& bc, CallInfo& callinfo, TRAPS) {
  JavaThread* current = THREAD;
  ResourceMark rm(current);

  // last java frame on stack (which includes native call frames)
  vframeStream vfst(current, true);  // Do not skip and javaCalls

  return find_callee_info_helper(vfst, bc, callinfo, THREAD);
}

Method* SharedRuntime::extract_attached_method(vframeStream& vfst) {
  nmethod* caller = vfst.nm();

  address pc = vfst.frame_pc();
  { // Get call instruction under lock because another thread may be busy patching it.
    CompiledICLocker ic_locker(caller);
    return caller->attached_method_before_pc(pc);
  }
  return nullptr;
}

// Finds receiver, CallInfo (i.e. receiver method), and calling bytecode
// for a call current in progress, i.e., arguments has been pushed on stack
// but callee has not been invoked yet.  Caller frame must be compiled.
Handle SharedRuntime::find_callee_info_helper(vframeStream& vfst, Bytecodes::Code& bc,
                                              CallInfo& callinfo, TRAPS) {
  Handle receiver;
  Handle nullHandle;  // create a handy null handle for exception returns
  JavaThread* current = THREAD;

  assert(!vfst.at_end(), "Java frame must exist");

  // Find caller and bci from vframe
  methodHandle caller(current, vfst.method());
  int          bci   = vfst.bci();

  if (caller->is_continuation_enter_intrinsic()) {
    bc = Bytecodes::_invokestatic;
    LinkResolver::resolve_continuation_enter(callinfo, CHECK_NH);
    return receiver;
  }

  // Substitutability test implementation piggy backs on static call resolution
  Bytecodes::Code code = caller->java_code_at(bci);
  if (code == Bytecodes::_if_acmpeq || code == Bytecodes::_if_acmpne) {
    bc = Bytecodes::_invokestatic;
    methodHandle attached_method(THREAD, extract_attached_method(vfst));
    assert(attached_method.not_null(), "must have attached method");
    vmClasses::ValueObjectMethods_klass()->initialize(CHECK_NH);
    LinkResolver::resolve_invoke(callinfo, receiver, attached_method, bc, false, CHECK_NH);
#ifdef ASSERT
    Method* is_subst = vmClasses::ValueObjectMethods_klass()->find_method(vmSymbols::isSubstitutable_name(), vmSymbols::object_object_boolean_signature());
    assert(callinfo.selected_method() == is_subst, "must be isSubstitutable method");
#endif
    return receiver;
  }

  Bytecode_invoke bytecode(caller, bci);
  int bytecode_index = bytecode.index();
  bc = bytecode.invoke_code();

  methodHandle attached_method(current, extract_attached_method(vfst));
  if (attached_method.not_null()) {
    Method* callee = bytecode.static_target(CHECK_NH);
    vmIntrinsics::ID id = callee->intrinsic_id();
    // When VM replaces MH.invokeBasic/linkTo* call with a direct/virtual call,
    // it attaches statically resolved method to the call site.
    if (MethodHandles::is_signature_polymorphic(id) &&
        MethodHandles::is_signature_polymorphic_intrinsic(id)) {
      bc = MethodHandles::signature_polymorphic_intrinsic_bytecode(id);

      // Adjust invocation mode according to the attached method.
      switch (bc) {
        case Bytecodes::_invokevirtual:
          if (attached_method->method_holder()->is_interface()) {
            bc = Bytecodes::_invokeinterface;
          }
          break;
        case Bytecodes::_invokeinterface:
          if (!attached_method->method_holder()->is_interface()) {
            bc = Bytecodes::_invokevirtual;
          }
          break;
        case Bytecodes::_invokehandle:
          if (!MethodHandles::is_signature_polymorphic_method(attached_method())) {
            bc = attached_method->is_static() ? Bytecodes::_invokestatic
                                              : Bytecodes::_invokevirtual;
          }
          break;
        default:
          break;
      }
    } else {
      assert(attached_method->has_scalarized_args(), "invalid use of attached method");
      if (!attached_method->method_holder()->is_inline_klass()) {
        // Ignore the attached method in this case to not confuse below code
        attached_method = methodHandle(current, nullptr);
      }
    }
  }

  assert(bc != Bytecodes::_illegal, "not initialized");

  bool has_receiver = bc != Bytecodes::_invokestatic &&
                      bc != Bytecodes::_invokedynamic &&
                      bc != Bytecodes::_invokehandle;
  bool check_null_and_abstract = true;

  // Find receiver for non-static call
  if (has_receiver) {
    // This register map must be update since we need to find the receiver for
    // compiled frames. The receiver might be in a register.
    RegisterMap reg_map2(current,
                         RegisterMap::UpdateMap::include,
                         RegisterMap::ProcessFrames::include,
                         RegisterMap::WalkContinuation::skip);
    frame stubFrame   = current->last_frame();
    // Caller-frame is a compiled frame
    frame callerFrame = stubFrame.sender(&reg_map2);

    Method* callee = attached_method();
    if (callee == nullptr) {
      callee = bytecode.static_target(CHECK_NH);
      if (callee == nullptr) {
        THROW_(vmSymbols::java_lang_NoSuchMethodException(), nullHandle);
      }
    }
    bool caller_is_c1 = callerFrame.is_compiled_frame() && callerFrame.cb()->as_nmethod()->is_compiled_by_c1();
    if (!caller_is_c1 && callee->is_scalarized_arg(0)) {
      // If the receiver is an inline type that is passed as fields, no oop is available
      // Resolve the call without receiver null checking.
      assert(!callee->mismatch(), "calls with inline type receivers should never mismatch");
      assert(attached_method.not_null() && !attached_method->is_abstract(), "must have non-abstract attached method");
      if (bc == Bytecodes::_invokeinterface) {
        bc = Bytecodes::_invokevirtual; // C2 optimistically replaces interface calls by virtual calls
      }
      check_null_and_abstract = false;
    } else {
      // Retrieve from a compiled argument list
      receiver = Handle(current, callerFrame.retrieve_receiver(&reg_map2));
      assert(oopDesc::is_oop_or_null(receiver()), "");
      if (receiver.is_null()) {
        THROW_(vmSymbols::java_lang_NullPointerException(), nullHandle);
      }
    }
  }

  // Resolve method
  if (attached_method.not_null()) {
    // Parameterized by attached method.
    LinkResolver::resolve_invoke(callinfo, receiver, attached_method, bc, check_null_and_abstract, CHECK_NH);
  } else {
    // Parameterized by bytecode.
    constantPoolHandle constants(current, caller->constants());
    LinkResolver::resolve_invoke(callinfo, receiver, constants, bytecode_index, bc, CHECK_NH);
  }

#ifdef ASSERT
  // Check that the receiver klass is of the right subtype and that it is initialized for virtual calls
  if (has_receiver && check_null_and_abstract) {
    assert(receiver.not_null(), "should have thrown exception");
    Klass* receiver_klass = receiver->klass();
    Klass* rk = nullptr;
    if (attached_method.not_null()) {
      // In case there's resolved method attached, use its holder during the check.
      rk = attached_method->method_holder();
    } else {
      // Klass is already loaded.
      constantPoolHandle constants(current, caller->constants());
      rk = constants->klass_ref_at(bytecode_index, bc, CHECK_NH);
    }
    Klass* static_receiver_klass = rk;
    assert(receiver_klass->is_subtype_of(static_receiver_klass),
           "actual receiver must be subclass of static receiver klass");
    if (receiver_klass->is_instance_klass()) {
      if (InstanceKlass::cast(receiver_klass)->is_not_initialized()) {
        tty->print_cr("ERROR: Klass not yet initialized!!");
        receiver_klass->print();
      }
      assert(!InstanceKlass::cast(receiver_klass)->is_not_initialized(), "receiver_klass must be initialized");
    }
  }
#endif

  return receiver;
}

methodHandle SharedRuntime::find_callee_method(bool is_optimized, bool& caller_is_c1, TRAPS) {
  JavaThread* current = THREAD;
  ResourceMark rm(current);
  // We need first to check if any Java activations (compiled, interpreted)
  // exist on the stack since last JavaCall.  If not, we need
  // to get the target method from the JavaCall wrapper.
  vframeStream vfst(current, true);  // Do not skip any javaCalls
  methodHandle callee_method;
  if (vfst.at_end()) {
    // No Java frames were found on stack since we did the JavaCall.
    // Hence the stack can only contain an entry_frame.  We need to
    // find the target method from the stub frame.
    RegisterMap reg_map(current,
                        RegisterMap::UpdateMap::skip,
                        RegisterMap::ProcessFrames::include,
                        RegisterMap::WalkContinuation::skip);
    frame fr = current->last_frame();
    assert(fr.is_runtime_frame(), "must be a runtimeStub");
    fr = fr.sender(&reg_map);
    assert(fr.is_entry_frame(), "must be");
    // fr is now pointing to the entry frame.
    callee_method = methodHandle(current, fr.entry_frame_call_wrapper()->callee_method());
  } else {
    Bytecodes::Code bc;
    CallInfo callinfo;
    find_callee_info_helper(vfst, bc, callinfo, CHECK_(methodHandle()));
    // Calls via mismatching methods are always non-scalarized
    if (callinfo.resolved_method()->mismatch() && !is_optimized) {
      caller_is_c1 = true;
    }
    callee_method = methodHandle(current, callinfo.selected_method());
  }
  assert(callee_method()->is_method(), "must be");
  return callee_method;
}

// Resolves a call.
methodHandle SharedRuntime::resolve_helper(bool is_virtual, bool is_optimized, bool& caller_is_c1, TRAPS) {
  JavaThread* current = THREAD;
  ResourceMark rm(current);
  RegisterMap cbl_map(current,
                      RegisterMap::UpdateMap::skip,
                      RegisterMap::ProcessFrames::include,
                      RegisterMap::WalkContinuation::skip);
  frame caller_frame = current->last_frame().sender(&cbl_map);

  CodeBlob* caller_cb = caller_frame.cb();
  guarantee(caller_cb != nullptr && caller_cb->is_nmethod(), "must be called from compiled method");
  nmethod* caller_nm = caller_cb->as_nmethod();

  // determine call info & receiver
  // note: a) receiver is null for static calls
  //       b) an exception is thrown if receiver is null for non-static calls
  CallInfo call_info;
  Bytecodes::Code invoke_code = Bytecodes::_illegal;
  Handle receiver = find_callee_info(invoke_code, call_info, CHECK_(methodHandle()));

  NoSafepointVerifier nsv;

  methodHandle callee_method(current, call_info.selected_method());
  // Calls via mismatching methods are always non-scalarized
  if (caller_nm->is_compiled_by_c1() || (call_info.resolved_method()->mismatch() && !is_optimized)) {
    caller_is_c1 = true;
  }

  assert((!is_virtual && invoke_code == Bytecodes::_invokestatic ) ||
         (!is_virtual && invoke_code == Bytecodes::_invokespecial) ||
         (!is_virtual && invoke_code == Bytecodes::_invokehandle ) ||
         (!is_virtual && invoke_code == Bytecodes::_invokedynamic) ||
         ( is_virtual && invoke_code != Bytecodes::_invokestatic ), "inconsistent bytecode");

  assert(!caller_nm->is_unloading(), "It should not be unloading");

#ifndef PRODUCT
  // tracing/debugging/statistics
  uint *addr = (is_optimized) ? (&_resolve_opt_virtual_ctr) :
                 (is_virtual) ? (&_resolve_virtual_ctr) :
                                (&_resolve_static_ctr);
  Atomic::inc(addr);

  if (TraceCallFixup) {
    ResourceMark rm(current);
    tty->print("resolving %s%s (%s) call%s to",
               (is_optimized) ? "optimized " : "", (is_virtual) ? "virtual" : "static",
               Bytecodes::name(invoke_code), (caller_is_c1) ? " from C1" : "");
    callee_method->print_short_name(tty);
    tty->print_cr(" at pc: " INTPTR_FORMAT " to code: " INTPTR_FORMAT,
                  p2i(caller_frame.pc()), p2i(callee_method->code()));
  }
#endif

  if (invoke_code == Bytecodes::_invokestatic) {
    assert(callee_method->method_holder()->is_initialized() ||
           callee_method->method_holder()->is_reentrant_initialization(current),
           "invalid class initialization state for invoke_static");
    if (!VM_Version::supports_fast_class_init_checks() && callee_method->needs_clinit_barrier()) {
      // In order to keep class initialization check, do not patch call
      // site for static call when the class is not fully initialized.
      // Proper check is enforced by call site re-resolution on every invocation.
      //
      // When fast class initialization checks are supported (VM_Version::supports_fast_class_init_checks() == true),
      // explicit class initialization check is put in nmethod entry (VEP).
      assert(callee_method->method_holder()->is_linked(), "must be");
      return callee_method;
    }
  }


  // JSR 292 key invariant:
  // If the resolved method is a MethodHandle invoke target, the call
  // site must be a MethodHandle call site, because the lambda form might tail-call
  // leaving the stack in a state unknown to either caller or callee

  // Compute entry points. The computation of the entry points is independent of
  // patching the call.

  // Make sure the callee nmethod does not get deoptimized and removed before
  // we are done patching the code.


  CompiledICLocker ml(caller_nm);
  if (is_virtual && !is_optimized) {
    CompiledIC* inline_cache = CompiledIC_before(caller_nm, caller_frame.pc());
    inline_cache->update(&call_info, receiver->klass(), caller_is_c1);
  } else {
    // Callsite is a direct call - set it to the destination method
    CompiledDirectCall* callsite = CompiledDirectCall::before(caller_frame.pc());
    callsite->set(callee_method, caller_is_c1);
  }

  return callee_method;
}

// Inline caches exist only in compiled code
JRT_BLOCK_ENTRY(address, SharedRuntime::handle_wrong_method_ic_miss(JavaThread* current))
#ifdef ASSERT
  RegisterMap reg_map(current,
                      RegisterMap::UpdateMap::skip,
                      RegisterMap::ProcessFrames::include,
                      RegisterMap::WalkContinuation::skip);
  frame stub_frame = current->last_frame();
  assert(stub_frame.is_runtime_frame(), "sanity check");
  frame caller_frame = stub_frame.sender(&reg_map);
  assert(!caller_frame.is_interpreted_frame() && !caller_frame.is_entry_frame() && !caller_frame.is_upcall_stub_frame(), "unexpected frame");
#endif /* ASSERT */

  methodHandle callee_method;
  bool is_optimized = false;
  bool caller_is_c1 = false;
  JRT_BLOCK
    callee_method = SharedRuntime::handle_ic_miss_helper(is_optimized, caller_is_c1, CHECK_NULL);
    // Return Method* through TLS
    current->set_vm_result_metadata(callee_method());
  JRT_BLOCK_END
  // return compiled code entry point after potential safepoints
  return get_resolved_entry(current, callee_method, false, is_optimized, caller_is_c1);
JRT_END


// Handle call site that has been made non-entrant
JRT_BLOCK_ENTRY(address, SharedRuntime::handle_wrong_method(JavaThread* current))
  // 6243940 We might end up in here if the callee is deoptimized
  // as we race to call it.  We don't want to take a safepoint if
  // the caller was interpreted because the caller frame will look
  // interpreted to the stack walkers and arguments are now
  // "compiled" so it is much better to make this transition
  // invisible to the stack walking code. The i2c path will
  // place the callee method in the callee_target. It is stashed
  // there because if we try and find the callee by normal means a
  // safepoint is possible and have trouble gc'ing the compiled args.
  RegisterMap reg_map(current,
                      RegisterMap::UpdateMap::skip,
                      RegisterMap::ProcessFrames::include,
                      RegisterMap::WalkContinuation::skip);
  frame stub_frame = current->last_frame();
  assert(stub_frame.is_runtime_frame(), "sanity check");
  frame caller_frame = stub_frame.sender(&reg_map);

  if (caller_frame.is_interpreted_frame() ||
      caller_frame.is_entry_frame() ||
      caller_frame.is_upcall_stub_frame()) {
    Method* callee = current->callee_target();
    guarantee(callee != nullptr && callee->is_method(), "bad handshake");
    current->set_vm_result_metadata(callee);
    current->set_callee_target(nullptr);
    if (caller_frame.is_entry_frame() && VM_Version::supports_fast_class_init_checks()) {
      // Bypass class initialization checks in c2i when caller is in native.
      // JNI calls to static methods don't have class initialization checks.
      // Fast class initialization checks are present in c2i adapters and call into
      // SharedRuntime::handle_wrong_method() on the slow path.
      //
      // JVM upcalls may land here as well, but there's a proper check present in
      // LinkResolver::resolve_static_call (called from JavaCalls::call_static),
      // so bypassing it in c2i adapter is benign.
      return callee->get_c2i_no_clinit_check_entry();
    } else {
      if (caller_frame.is_interpreted_frame()) {
        return callee->get_c2i_inline_entry();
      } else {
        return callee->get_c2i_entry();
      }
    }
  }

  // Must be compiled to compiled path which is safe to stackwalk
  methodHandle callee_method;
  bool is_static_call = false;
  bool is_optimized = false;
  bool caller_is_c1 = false;
  JRT_BLOCK
    // Force resolving of caller (if we called from compiled frame)
    callee_method = SharedRuntime::reresolve_call_site(is_static_call, is_optimized, caller_is_c1, CHECK_NULL);
    current->set_vm_result_metadata(callee_method());
  JRT_BLOCK_END
  // return compiled code entry point after potential safepoints
  return get_resolved_entry(current, callee_method, is_static_call, is_optimized, caller_is_c1);
JRT_END

// Handle abstract method call
JRT_BLOCK_ENTRY(address, SharedRuntime::handle_wrong_method_abstract(JavaThread* current))
  // Verbose error message for AbstractMethodError.
  // Get the called method from the invoke bytecode.
  vframeStream vfst(current, true);
  assert(!vfst.at_end(), "Java frame must exist");
  methodHandle caller(current, vfst.method());
  Bytecode_invoke invoke(caller, vfst.bci());
  DEBUG_ONLY( invoke.verify(); )

  // Find the compiled caller frame.
  RegisterMap reg_map(current,
                      RegisterMap::UpdateMap::include,
                      RegisterMap::ProcessFrames::include,
                      RegisterMap::WalkContinuation::skip);
  frame stubFrame = current->last_frame();
  assert(stubFrame.is_runtime_frame(), "must be");
  frame callerFrame = stubFrame.sender(&reg_map);
  assert(callerFrame.is_compiled_frame(), "must be");

  // Install exception and return forward entry.
  address res = SharedRuntime::throw_AbstractMethodError_entry();
  JRT_BLOCK
    methodHandle callee(current, invoke.static_target(current));
    if (!callee.is_null()) {
      oop recv = callerFrame.retrieve_receiver(&reg_map);
      Klass *recv_klass = (recv != nullptr) ? recv->klass() : nullptr;
      res = StubRoutines::forward_exception_entry();
      LinkResolver::throw_abstract_method_error(callee, recv_klass, CHECK_(res));
    }
  JRT_BLOCK_END
  return res;
JRT_END

// return verified_code_entry if interp_only_mode is not set for the current thread;
// otherwise return c2i entry.
address SharedRuntime::get_resolved_entry(JavaThread* current, methodHandle callee_method,
                                          bool is_static_call, bool is_optimized, bool caller_is_c1) {
  if (current->is_interp_only_mode() && !callee_method->is_special_native_intrinsic()) {
    // In interp_only_mode we need to go to the interpreted entry
    // The c2i won't patch in this mode -- see fixup_callers_callsite
    return callee_method->get_c2i_entry();
  }

  if (caller_is_c1) {
    assert(callee_method->verified_inline_code_entry() != nullptr, "Jump to zero!");
    return callee_method->verified_inline_code_entry();
  } else if (is_static_call || is_optimized) {
    assert(callee_method->verified_code_entry() != nullptr, "Jump to zero!");
    return callee_method->verified_code_entry();
  } else {
    assert(callee_method->verified_inline_ro_code_entry() != nullptr, "Jump to zero!");
    return callee_method->verified_inline_ro_code_entry();
  }
}

// resolve a static call and patch code
JRT_BLOCK_ENTRY(address, SharedRuntime::resolve_static_call_C(JavaThread* current ))
  methodHandle callee_method;
  bool caller_is_c1 = false;
  bool enter_special = false;
  JRT_BLOCK
    callee_method = SharedRuntime::resolve_helper(false, false, caller_is_c1, CHECK_NULL);
    current->set_vm_result_metadata(callee_method());
  JRT_BLOCK_END
  // return compiled code entry point after potential safepoints
  return get_resolved_entry(current, callee_method, true, false, caller_is_c1);
JRT_END

// resolve virtual call and update inline cache to monomorphic
JRT_BLOCK_ENTRY(address, SharedRuntime::resolve_virtual_call_C(JavaThread* current))
  methodHandle callee_method;
  bool caller_is_c1 = false;
  JRT_BLOCK
    callee_method = SharedRuntime::resolve_helper(true, false, caller_is_c1, CHECK_NULL);
    current->set_vm_result_metadata(callee_method());
  JRT_BLOCK_END
  // return compiled code entry point after potential safepoints
  return get_resolved_entry(current, callee_method, false, false, caller_is_c1);
JRT_END


// Resolve a virtual call that can be statically bound (e.g., always
// monomorphic, so it has no inline cache).  Patch code to resolved target.
JRT_BLOCK_ENTRY(address, SharedRuntime::resolve_opt_virtual_call_C(JavaThread* current))
  methodHandle callee_method;
  bool caller_is_c1 = false;
  JRT_BLOCK
    callee_method = SharedRuntime::resolve_helper(true, true, caller_is_c1, CHECK_NULL);
    current->set_vm_result_metadata(callee_method());
  JRT_BLOCK_END
  // return compiled code entry point after potential safepoints
  return get_resolved_entry(current, callee_method, false, true, caller_is_c1);
JRT_END



methodHandle SharedRuntime::handle_ic_miss_helper(bool& is_optimized, bool& caller_is_c1, TRAPS) {
  JavaThread* current = THREAD;
  ResourceMark rm(current);
  CallInfo call_info;
  Bytecodes::Code bc;

  // receiver is null for static calls. An exception is thrown for null
  // receivers for non-static calls
  Handle receiver = find_callee_info(bc, call_info, CHECK_(methodHandle()));

  methodHandle callee_method(current, call_info.selected_method());

#ifndef PRODUCT
  Atomic::inc(&_ic_miss_ctr);

  // Statistics & Tracing
  if (TraceCallFixup) {
    ResourceMark rm(current);
    tty->print("IC miss (%s) call%s to", Bytecodes::name(bc), (caller_is_c1) ? " from C1" : "");
    callee_method->print_short_name(tty);
    tty->print_cr(" code: " INTPTR_FORMAT, p2i(callee_method->code()));
  }

  if (ICMissHistogram) {
    MutexLocker m(VMStatistic_lock);
    RegisterMap reg_map(current,
                        RegisterMap::UpdateMap::skip,
                        RegisterMap::ProcessFrames::include,
                        RegisterMap::WalkContinuation::skip);
    frame f = current->last_frame().real_sender(&reg_map);// skip runtime stub
    // produce statistics under the lock
    trace_ic_miss(f.pc());
  }
#endif

  // install an event collector so that when a vtable stub is created the
  // profiler can be notified via a DYNAMIC_CODE_GENERATED event. The
  // event can't be posted when the stub is created as locks are held
  // - instead the event will be deferred until the event collector goes
  // out of scope.
  JvmtiDynamicCodeEventCollector event_collector;

  // Update inline cache to megamorphic. Skip update if we are called from interpreted.
  RegisterMap reg_map(current,
                      RegisterMap::UpdateMap::skip,
                      RegisterMap::ProcessFrames::include,
                      RegisterMap::WalkContinuation::skip);
  frame caller_frame = current->last_frame().sender(&reg_map);
  CodeBlob* cb = caller_frame.cb();
  nmethod* caller_nm = cb->as_nmethod();
  // Calls via mismatching methods are always non-scalarized
  if (caller_nm->is_compiled_by_c1() || call_info.resolved_method()->mismatch()) {
    caller_is_c1 = true;
  }

  CompiledICLocker ml(caller_nm);
  CompiledIC* inline_cache = CompiledIC_before(caller_nm, caller_frame.pc());
  inline_cache->update(&call_info, receiver()->klass(), caller_is_c1);

  return callee_method;
}

//
// Resets a call-site in compiled code so it will get resolved again.
// This routines handles both virtual call sites, optimized virtual call
// sites, and static call sites. Typically used to change a call sites
// destination from compiled to interpreted.
//
methodHandle SharedRuntime::reresolve_call_site(bool& is_static_call, bool& is_optimized, bool& caller_is_c1, TRAPS) {
  JavaThread* current = THREAD;
  ResourceMark rm(current);
  RegisterMap reg_map(current,
                      RegisterMap::UpdateMap::skip,
                      RegisterMap::ProcessFrames::include,
                      RegisterMap::WalkContinuation::skip);
  frame stub_frame = current->last_frame();
  assert(stub_frame.is_runtime_frame(), "must be a runtimeStub");
  frame caller = stub_frame.sender(&reg_map);
  if (caller.is_compiled_frame()) {
    caller_is_c1 = caller.cb()->as_nmethod()->is_compiled_by_c1();
  }

  // Do nothing if the frame isn't a live compiled frame.
  // nmethod could be deoptimized by the time we get here
  // so no update to the caller is needed.

  if ((caller.is_compiled_frame() && !caller.is_deoptimized_frame()) ||
      (caller.is_native_frame() && caller.cb()->as_nmethod()->method()->is_continuation_enter_intrinsic())) {

    address pc = caller.pc();

    nmethod* caller_nm = CodeCache::find_nmethod(pc);
    assert(caller_nm != nullptr, "did not find caller nmethod");

    // Default call_addr is the location of the "basic" call.
    // Determine the address of the call we a reresolving. With
    // Inline Caches we will always find a recognizable call.
    // With Inline Caches disabled we may or may not find a
    // recognizable call. We will always find a call for static
    // calls and for optimized virtual calls. For vanilla virtual
    // calls it depends on the state of the UseInlineCaches switch.
    //
    // With Inline Caches disabled we can get here for a virtual call
    // for two reasons:
    //   1 - calling an abstract method. The vtable for abstract methods
    //       will run us thru handle_wrong_method and we will eventually
    //       end up in the interpreter to throw the ame.
    //   2 - a racing deoptimization. We could be doing a vanilla vtable
    //       call and between the time we fetch the entry address and
    //       we jump to it the target gets deoptimized. Similar to 1
    //       we will wind up in the interprter (thru a c2i with c2).
    //
    CompiledICLocker ml(caller_nm);
    address call_addr = caller_nm->call_instruction_address(pc);

    if (call_addr != nullptr) {
      // On x86 the logic for finding a call instruction is blindly checking for a call opcode 5
      // bytes back in the instruction stream so we must also check for reloc info.
      RelocIterator iter(caller_nm, call_addr, call_addr+1);
      bool ret = iter.next(); // Get item
      if (ret) {
        is_static_call = false;
        is_optimized = false;
        switch (iter.type()) {
          case relocInfo::static_call_type:
            is_static_call = true;
          case relocInfo::opt_virtual_call_type: {
            is_optimized = (iter.type() == relocInfo::opt_virtual_call_type);
            CompiledDirectCall* cdc = CompiledDirectCall::at(call_addr);
            cdc->set_to_clean();
            break;
          }
          case relocInfo::virtual_call_type: {
            // compiled, dispatched call (which used to call an interpreted method)
            CompiledIC* inline_cache = CompiledIC_at(caller_nm, call_addr);
            inline_cache->set_to_clean();
            break;
          }
          default:
            break;
        }
      }
    }
  }

  methodHandle callee_method = find_callee_method(is_optimized, caller_is_c1, CHECK_(methodHandle()));

#ifndef PRODUCT
  Atomic::inc(&_wrong_method_ctr);

  if (TraceCallFixup) {
    ResourceMark rm(current);
    tty->print("handle_wrong_method reresolving call%s to", (caller_is_c1) ? " from C1" : "");
    callee_method->print_short_name(tty);
    tty->print_cr(" code: " INTPTR_FORMAT, p2i(callee_method->code()));
  }
#endif

  return callee_method;
}

address SharedRuntime::handle_unsafe_access(JavaThread* thread, address next_pc) {
  // The faulting unsafe accesses should be changed to throw the error
  // synchronously instead. Meanwhile the faulting instruction will be
  // skipped over (effectively turning it into a no-op) and an
  // asynchronous exception will be raised which the thread will
  // handle at a later point. If the instruction is a load it will
  // return garbage.

  // Request an async exception.
  thread->set_pending_unsafe_access_error();

  // Return address of next instruction to execute.
  return next_pc;
}

#ifdef ASSERT
void SharedRuntime::check_member_name_argument_is_last_argument(const methodHandle& method,
                                                                const BasicType* sig_bt,
                                                                const VMRegPair* regs) {
  ResourceMark rm;
  const int total_args_passed = method->size_of_parameters();
  const VMRegPair*    regs_with_member_name = regs;
        VMRegPair* regs_without_member_name = NEW_RESOURCE_ARRAY(VMRegPair, total_args_passed - 1);

  const int member_arg_pos = total_args_passed - 1;
  assert(member_arg_pos >= 0 && member_arg_pos < total_args_passed, "oob");
  assert(sig_bt[member_arg_pos] == T_OBJECT, "dispatch argument must be an object");

  java_calling_convention(sig_bt, regs_without_member_name, total_args_passed - 1);

  for (int i = 0; i < member_arg_pos; i++) {
    VMReg a =    regs_with_member_name[i].first();
    VMReg b = regs_without_member_name[i].first();
    assert(a->value() == b->value(), "register allocation mismatch: a= %d, b= %d", a->value(), b->value());
  }
  assert(regs_with_member_name[member_arg_pos].first()->is_valid(), "bad member arg");
}
#endif

// ---------------------------------------------------------------------------
// We are calling the interpreter via a c2i. Normally this would mean that
// we were called by a compiled method. However we could have lost a race
// where we went int -> i2c -> c2i and so the caller could in fact be
// interpreted. If the caller is compiled we attempt to patch the caller
// so he no longer calls into the interpreter.
JRT_LEAF(void, SharedRuntime::fixup_callers_callsite(Method* method, address caller_pc))
  AARCH64_PORT_ONLY(assert(pauth_ptr_is_raw(caller_pc), "should be raw"));

  // It's possible that deoptimization can occur at a call site which hasn't
  // been resolved yet, in which case this function will be called from
  // an nmethod that has been patched for deopt and we can ignore the
  // request for a fixup.
  // Also it is possible that we lost a race in that from_compiled_entry
  // is now back to the i2c in that case we don't need to patch and if
  // we did we'd leap into space because the callsite needs to use
  // "to interpreter" stub in order to load up the Method*. Don't
  // ask me how I know this...

  // Result from nmethod::is_unloading is not stable across safepoints.
  NoSafepointVerifier nsv;

  nmethod* callee = method->code();
  if (callee == nullptr) {
    return;
  }

  // write lock needed because we might patch call site by set_to_clean()
  // and is_unloading() can modify nmethod's state
  MACOS_AARCH64_ONLY(ThreadWXEnable __wx(WXWrite, JavaThread::current()));

  CodeBlob* cb = CodeCache::find_blob(caller_pc);
  if (cb == nullptr || !cb->is_nmethod() || !callee->is_in_use() || callee->is_unloading()) {
    return;
  }

  // The check above makes sure this is an nmethod.
  nmethod* caller = cb->as_nmethod();

  // Get the return PC for the passed caller PC.
  address return_pc = caller_pc + frame::pc_return_offset;

  if (!caller->is_in_use() || !NativeCall::is_call_before(return_pc)) {
    return;
  }

  // Expect to find a native call there (unless it was no-inline cache vtable dispatch)
  CompiledICLocker ic_locker(caller);
  ResourceMark rm;

  // If we got here through a static call or opt_virtual call, then we know where the
  // call address would be; let's peek at it
  address callsite_addr = (address)nativeCall_before(return_pc);
  RelocIterator iter(caller, callsite_addr, callsite_addr + 1);
  if (!iter.next()) {
    // No reloc entry found; not a static or optimized virtual call
    return;
  }

  relocInfo::relocType type = iter.reloc()->type();
  if (type != relocInfo::static_call_type &&
      type != relocInfo::opt_virtual_call_type) {
    return;
  }

  CompiledDirectCall* callsite = CompiledDirectCall::before(return_pc);
  callsite->set_to_clean();
JRT_END


// same as JVM_Arraycopy, but called directly from compiled code
JRT_ENTRY(void, SharedRuntime::slow_arraycopy_C(oopDesc* src,  jint src_pos,
                                                oopDesc* dest, jint dest_pos,
                                                jint length,
                                                JavaThread* current)) {
#ifndef PRODUCT
  _slow_array_copy_ctr++;
#endif
  // Check if we have null pointers
  if (src == nullptr || dest == nullptr) {
    THROW(vmSymbols::java_lang_NullPointerException());
  }
  // Do the copy.  The casts to arrayOop are necessary to the copy_array API,
  // even though the copy_array API also performs dynamic checks to ensure
  // that src and dest are truly arrays (and are conformable).
  // The copy_array mechanism is awkward and could be removed, but
  // the compilers don't call this function except as a last resort,
  // so it probably doesn't matter.
  src->klass()->copy_array((arrayOopDesc*)src, src_pos,
                                        (arrayOopDesc*)dest, dest_pos,
                                        length, current);
}
JRT_END

// The caller of generate_class_cast_message() (or one of its callers)
// must use a ResourceMark in order to correctly free the result.
char* SharedRuntime::generate_class_cast_message(
    JavaThread* thread, Klass* caster_klass) {

  // Get target class name from the checkcast instruction
  vframeStream vfst(thread, true);
  assert(!vfst.at_end(), "Java frame must exist");
  Bytecode_checkcast cc(vfst.method(), vfst.method()->bcp_from(vfst.bci()));
  constantPoolHandle cpool(thread, vfst.method()->constants());
  Klass* target_klass = ConstantPool::klass_at_if_loaded(cpool, cc.index());
  Symbol* target_klass_name = nullptr;
  if (target_klass == nullptr) {
    // This klass should be resolved, but just in case, get the name in the klass slot.
    target_klass_name = cpool->klass_name_at(cc.index());
  }
  return generate_class_cast_message(caster_klass, target_klass, target_klass_name);
}


// The caller of generate_class_cast_message() (or one of its callers)
// must use a ResourceMark in order to correctly free the result.
char* SharedRuntime::generate_class_cast_message(
    Klass* caster_klass, Klass* target_klass, Symbol* target_klass_name) {
  const char* caster_name = caster_klass->external_name();

  assert(target_klass != nullptr || target_klass_name != nullptr, "one must be provided");
  const char* target_name = target_klass == nullptr ? target_klass_name->as_klass_external_name() :
                                                   target_klass->external_name();

  size_t msglen = strlen(caster_name) + strlen("class ") + strlen(" cannot be cast to class ") + strlen(target_name) + 1;

  const char* caster_klass_description = "";
  const char* target_klass_description = "";
  const char* klass_separator = "";
  if (target_klass != nullptr && caster_klass->module() == target_klass->module()) {
    caster_klass_description = caster_klass->joint_in_module_of_loader(target_klass);
  } else {
    caster_klass_description = caster_klass->class_in_module_of_loader();
    target_klass_description = (target_klass != nullptr) ? target_klass->class_in_module_of_loader() : "";
    klass_separator = (target_klass != nullptr) ? "; " : "";
  }

  // add 3 for parenthesis and preceding space
  msglen += strlen(caster_klass_description) + strlen(target_klass_description) + strlen(klass_separator) + 3;

  char* message = NEW_RESOURCE_ARRAY_RETURN_NULL(char, msglen);
  if (message == nullptr) {
    // Shouldn't happen, but don't cause even more problems if it does
    message = const_cast<char*>(caster_klass->external_name());
  } else {
    jio_snprintf(message,
                 msglen,
                 "class %s cannot be cast to class %s (%s%s%s)",
                 caster_name,
                 target_name,
                 caster_klass_description,
                 klass_separator,
                 target_klass_description
                 );
  }
  return message;
}

char* SharedRuntime::generate_identity_exception_message(JavaThread* current, Klass* klass) {
  assert(klass->is_inline_klass(), "Must be a concrete value class");
  const char* desc = "Cannot synchronize on an instance of value class ";
  const char* className = klass->external_name();
  size_t msglen = strlen(desc) + strlen(className) + 1;
  char* message = NEW_RESOURCE_ARRAY(char, msglen);
  if (nullptr == message) {
    // Out of memory: can't create detailed error message
    message = const_cast<char*>(klass->external_name());
  } else {
    jio_snprintf(message, msglen, "%s%s", desc, className);
  }
  return message;
}

JRT_LEAF(void, SharedRuntime::reguard_yellow_pages())
  (void) JavaThread::current()->stack_overflow_state()->reguard_stack();
JRT_END

void SharedRuntime::monitor_enter_helper(oopDesc* obj, BasicLock* lock, JavaThread* current) {
  if (!SafepointSynchronize::is_synchronizing()) {
    // Only try quick_enter() if we're not trying to reach a safepoint
    // so that the calling thread reaches the safepoint more quickly.
    if (ObjectSynchronizer::quick_enter(obj, lock, current)) {
      return;
    }
  }
  // NO_ASYNC required because an async exception on the state transition destructor
  // would leave you with the lock held and it would never be released.
  // The normal monitorenter NullPointerException is thrown without acquiring a lock
  // and the model is that an exception implies the method failed.
  JRT_BLOCK_NO_ASYNC
  Handle h_obj(THREAD, obj);
  ObjectSynchronizer::enter(h_obj, lock, current);
  assert(!HAS_PENDING_EXCEPTION, "Should have no exception here");
  JRT_BLOCK_END
}

// Handles the uncommon case in locking, i.e., contention or an inflated lock.
JRT_BLOCK_ENTRY(void, SharedRuntime::complete_monitor_locking_C(oopDesc* obj, BasicLock* lock, JavaThread* current))
  SharedRuntime::monitor_enter_helper(obj, lock, current);
JRT_END

void SharedRuntime::monitor_exit_helper(oopDesc* obj, BasicLock* lock, JavaThread* current) {
  assert(JavaThread::current() == current, "invariant");
  // Exit must be non-blocking, and therefore no exceptions can be thrown.
  ExceptionMark em(current);

  // Check if C2_MacroAssembler::fast_unlock() or
  // C2_MacroAssembler::fast_unlock_lightweight() unlocked an inflated
  // monitor before going slow path.  Since there is no safepoint
  // polling when calling into the VM, we can be sure that the monitor
  // hasn't been deallocated.
  ObjectMonitor* m = current->unlocked_inflated_monitor();
  if (m != nullptr) {
    assert(!m->has_owner(current), "must be");
    current->clear_unlocked_inflated_monitor();

    // We need to reacquire the lock before we can call ObjectSynchronizer::exit().
    if (!m->try_enter(current, /*check_for_recursion*/ false)) {
      // Some other thread acquired the lock (or the monitor was
      // deflated). Either way we are done.
      current->dec_held_monitor_count();
      return;
    }
  }

  // The object could become unlocked through a JNI call, which we have no other checks for.
  // Give a fatal message if CheckJNICalls. Otherwise we ignore it.
  if (obj->is_unlocked()) {
    if (CheckJNICalls) {
      fatal("Object has been unlocked by JNI");
    }
    return;
  }
  ObjectSynchronizer::exit(obj, lock, current);
}

// Handles the uncommon cases of monitor unlocking in compiled code
JRT_LEAF(void, SharedRuntime::complete_monitor_unlocking_C(oopDesc* obj, BasicLock* lock, JavaThread* current))
  assert(current == JavaThread::current(), "pre-condition");
  SharedRuntime::monitor_exit_helper(obj, lock, current);
JRT_END

// This is only called when CheckJNICalls is true, and only
// for virtual thread termination.
JRT_LEAF(void,  SharedRuntime::log_jni_monitor_still_held())
  assert(CheckJNICalls, "Only call this when checking JNI usage");
  if (log_is_enabled(Debug, jni)) {
    JavaThread* current = JavaThread::current();
    int64_t vthread_id = java_lang_Thread::thread_id(current->vthread());
    int64_t carrier_id = java_lang_Thread::thread_id(current->threadObj());
    log_debug(jni)("VirtualThread (tid: " INT64_FORMAT ", carrier id: " INT64_FORMAT
                   ") exiting with Objects still locked by JNI MonitorEnter.",
                   vthread_id, carrier_id);
  }
JRT_END

#ifndef PRODUCT

void SharedRuntime::print_statistics() {
  ttyLocker ttyl;
  if (xtty != nullptr)  xtty->head("statistics type='SharedRuntime'");

  SharedRuntime::print_ic_miss_histogram();

  // Dump the JRT_ENTRY counters
  if (_new_instance_ctr) tty->print_cr("%5u new instance requires GC", _new_instance_ctr);
  if (_new_array_ctr) tty->print_cr("%5u new array requires GC", _new_array_ctr);
  if (_multi2_ctr) tty->print_cr("%5u multianewarray 2 dim", _multi2_ctr);
  if (_multi3_ctr) tty->print_cr("%5u multianewarray 3 dim", _multi3_ctr);
  if (_multi4_ctr) tty->print_cr("%5u multianewarray 4 dim", _multi4_ctr);
  if (_multi5_ctr) tty->print_cr("%5u multianewarray 5 dim", _multi5_ctr);

  tty->print_cr("%5u inline cache miss in compiled", _ic_miss_ctr);
  tty->print_cr("%5u wrong method", _wrong_method_ctr);
  tty->print_cr("%5u unresolved static call site", _resolve_static_ctr);
  tty->print_cr("%5u unresolved virtual call site", _resolve_virtual_ctr);
  tty->print_cr("%5u unresolved opt virtual call site", _resolve_opt_virtual_ctr);

  if (_mon_enter_stub_ctr) tty->print_cr("%5u monitor enter stub", _mon_enter_stub_ctr);
  if (_mon_exit_stub_ctr) tty->print_cr("%5u monitor exit stub", _mon_exit_stub_ctr);
  if (_mon_enter_ctr) tty->print_cr("%5u monitor enter slow", _mon_enter_ctr);
  if (_mon_exit_ctr) tty->print_cr("%5u monitor exit slow", _mon_exit_ctr);
  if (_partial_subtype_ctr) tty->print_cr("%5u slow partial subtype", _partial_subtype_ctr);
  if (_jbyte_array_copy_ctr) tty->print_cr("%5u byte array copies", _jbyte_array_copy_ctr);
  if (_jshort_array_copy_ctr) tty->print_cr("%5u short array copies", _jshort_array_copy_ctr);
  if (_jint_array_copy_ctr) tty->print_cr("%5u int array copies", _jint_array_copy_ctr);
  if (_jlong_array_copy_ctr) tty->print_cr("%5u long array copies", _jlong_array_copy_ctr);
  if (_oop_array_copy_ctr) tty->print_cr("%5u oop array copies", _oop_array_copy_ctr);
  if (_checkcast_array_copy_ctr) tty->print_cr("%5u checkcast array copies", _checkcast_array_copy_ctr);
  if (_unsafe_array_copy_ctr) tty->print_cr("%5u unsafe array copies", _unsafe_array_copy_ctr);
  if (_generic_array_copy_ctr) tty->print_cr("%5u generic array copies", _generic_array_copy_ctr);
  if (_slow_array_copy_ctr) tty->print_cr("%5u slow array copies", _slow_array_copy_ctr);
  if (_find_handler_ctr) tty->print_cr("%5u find exception handler", _find_handler_ctr);
  if (_rethrow_ctr) tty->print_cr("%5u rethrow handler", _rethrow_ctr);
  if (_unsafe_set_memory_ctr) tty->print_cr("%5u unsafe set memorys", _unsafe_set_memory_ctr);

  AdapterHandlerLibrary::print_statistics();

  if (xtty != nullptr)  xtty->tail("statistics");
}

inline double percent(int64_t x, int64_t y) {
  return 100.0 * (double)x / (double)MAX2(y, (int64_t)1);
}

class MethodArityHistogram {
 public:
  enum { MAX_ARITY = 256 };
 private:
  static uint64_t _arity_histogram[MAX_ARITY]; // histogram of #args
  static uint64_t _size_histogram[MAX_ARITY];  // histogram of arg size in words
  static uint64_t _total_compiled_calls;
  static uint64_t _max_compiled_calls_per_method;
  static int _max_arity;                       // max. arity seen
  static int _max_size;                        // max. arg size seen

  static void add_method_to_histogram(nmethod* nm) {
    Method* method = (nm == nullptr) ? nullptr : nm->method();
    if (method != nullptr) {
      ArgumentCount args(method->signature());
      int arity   = args.size() + (method->is_static() ? 0 : 1);
      int argsize = method->size_of_parameters();
      arity   = MIN2(arity, MAX_ARITY-1);
      argsize = MIN2(argsize, MAX_ARITY-1);
      uint64_t count = (uint64_t)method->compiled_invocation_count();
      _max_compiled_calls_per_method = count > _max_compiled_calls_per_method ? count : _max_compiled_calls_per_method;
      _total_compiled_calls    += count;
      _arity_histogram[arity]  += count;
      _size_histogram[argsize] += count;
      _max_arity = MAX2(_max_arity, arity);
      _max_size  = MAX2(_max_size, argsize);
    }
  }

  void print_histogram_helper(int n, uint64_t* histo, const char* name) {
    const int N = MIN2(9, n);
    double sum = 0;
    double weighted_sum = 0;
    for (int i = 0; i <= n; i++) { sum += (double)histo[i]; weighted_sum += (double)(i*histo[i]); }
    if (sum >= 1) { // prevent divide by zero or divide overflow
      double rest = sum;
      double percent = sum / 100;
      for (int i = 0; i <= N; i++) {
        rest -= (double)histo[i];
        tty->print_cr("%4d: " UINT64_FORMAT_W(12) " (%5.1f%%)", i, histo[i], (double)histo[i] / percent);
      }
      tty->print_cr("rest: " INT64_FORMAT_W(12) " (%5.1f%%)", (int64_t)rest, rest / percent);
      tty->print_cr("(avg. %s = %3.1f, max = %d)", name, weighted_sum / sum, n);
      tty->print_cr("(total # of compiled calls = " INT64_FORMAT_W(14) ")", _total_compiled_calls);
      tty->print_cr("(max # of compiled calls   = " INT64_FORMAT_W(14) ")", _max_compiled_calls_per_method);
    } else {
      tty->print_cr("Histogram generation failed for %s. n = %d, sum = %7.5f", name, n, sum);
    }
  }

  void print_histogram() {
    tty->print_cr("\nHistogram of call arity (incl. rcvr, calls to compiled methods only):");
    print_histogram_helper(_max_arity, _arity_histogram, "arity");
    tty->print_cr("\nHistogram of parameter block size (in words, incl. rcvr):");
    print_histogram_helper(_max_size, _size_histogram, "size");
    tty->cr();
  }

 public:
  MethodArityHistogram() {
    // Take the Compile_lock to protect against changes in the CodeBlob structures
    MutexLocker mu1(Compile_lock, Mutex::_safepoint_check_flag);
    // Take the CodeCache_lock to protect against changes in the CodeHeap structure
    MutexLocker mu2(CodeCache_lock, Mutex::_no_safepoint_check_flag);
    _max_arity = _max_size = 0;
    _total_compiled_calls = 0;
    _max_compiled_calls_per_method = 0;
    for (int i = 0; i < MAX_ARITY; i++) _arity_histogram[i] = _size_histogram[i] = 0;
    CodeCache::nmethods_do(add_method_to_histogram);
    print_histogram();
  }
};

uint64_t MethodArityHistogram::_arity_histogram[MethodArityHistogram::MAX_ARITY];
uint64_t MethodArityHistogram::_size_histogram[MethodArityHistogram::MAX_ARITY];
uint64_t MethodArityHistogram::_total_compiled_calls;
uint64_t MethodArityHistogram::_max_compiled_calls_per_method;
int MethodArityHistogram::_max_arity;
int MethodArityHistogram::_max_size;

void SharedRuntime::print_call_statistics(uint64_t comp_total) {
  tty->print_cr("Calls from compiled code:");
  int64_t total  = _nof_normal_calls + _nof_interface_calls + _nof_static_calls;
  int64_t mono_c = _nof_normal_calls - _nof_megamorphic_calls;
  int64_t mono_i = _nof_interface_calls;
  tty->print_cr("\t" INT64_FORMAT_W(12) " (100%%)  total non-inlined   ", total);
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.1f%%) |- virtual calls       ", _nof_normal_calls, percent(_nof_normal_calls, total));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.0f%%) |  |- inlined          ", _nof_inlined_calls, percent(_nof_inlined_calls, _nof_normal_calls));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.0f%%) |  |- monomorphic      ", mono_c, percent(mono_c, _nof_normal_calls));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.0f%%) |  |- megamorphic      ", _nof_megamorphic_calls, percent(_nof_megamorphic_calls, _nof_normal_calls));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.1f%%) |- interface calls     ", _nof_interface_calls, percent(_nof_interface_calls, total));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.0f%%) |  |- inlined          ", _nof_inlined_interface_calls, percent(_nof_inlined_interface_calls, _nof_interface_calls));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.0f%%) |  |- monomorphic      ", mono_i, percent(mono_i, _nof_interface_calls));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.1f%%) |- static/special calls", _nof_static_calls, percent(_nof_static_calls, total));
  tty->print_cr("\t" INT64_FORMAT_W(12) " (%4.0f%%) |  |- inlined          ", _nof_inlined_static_calls, percent(_nof_inlined_static_calls, _nof_static_calls));
  tty->cr();
  tty->print_cr("Note 1: counter updates are not MT-safe.");
  tty->print_cr("Note 2: %% in major categories are relative to total non-inlined calls;");
  tty->print_cr("        %% in nested categories are relative to their category");
  tty->print_cr("        (and thus add up to more than 100%% with inlining)");
  tty->cr();

  MethodArityHistogram h;
}
#endif

#ifndef PRODUCT
static int _lookups; // number of calls to lookup
static int _equals;  // number of buckets checked with matching hash
static int _hits;    // number of successful lookups
static int _compact; // number of equals calls with compact signature
#endif

// A simple wrapper class around the calling convention information
// that allows sharing of adapters for the same calling convention.
class AdapterFingerPrint : public CHeapObj<mtCode> {
 private:
  enum {
    _basic_type_bits = 5,
    _basic_type_mask = right_n_bits(_basic_type_bits),
    _basic_types_per_int = BitsPerInt / _basic_type_bits,
    _compact_int_count = 3
  };
  // TO DO:  Consider integrating this with a more global scheme for compressing signatures.
  // For now, 4 bits per components (plus T_VOID gaps after double/long) is not excessive.

  union {
    int  _compact[_compact_int_count];
    int* _fingerprint;
  } _value;
  int _length; // A negative length indicates the fingerprint is in the compact form,
               // Otherwise _value._fingerprint is the array.

  // Remap BasicTypes that are handled equivalently by the adapters.
  // These are correct for the current system but someday it might be
  // necessary to make this mapping platform dependent.
  static BasicType adapter_encoding(BasicType in) {
    switch (in) {
      case T_BOOLEAN:
      case T_BYTE:
      case T_SHORT:
      case T_CHAR:
        // They are all promoted to T_INT in the calling convention
        return T_INT;

      case T_OBJECT:
      case T_ARRAY:
        // In other words, we assume that any register good enough for
        // an int or long is good enough for a managed pointer.
#ifdef _LP64
        return T_LONG;
#else
        return T_INT;
#endif

      case T_INT:
      case T_LONG:
      case T_FLOAT:
      case T_DOUBLE:
      case T_VOID:
        return in;

      default:
        ShouldNotReachHere();
        return T_CONFLICT;
    }
  }

 public:
  AdapterFingerPrint(const GrowableArray<SigEntry>* sig, bool has_ro_adapter = false) {
    // The fingerprint is based on the BasicType signature encoded
    // into an array of ints with eight entries per int.
    int total_args_passed = (sig != nullptr) ? sig->length() : 0;
    int* ptr;
    int len = (total_args_passed + (_basic_types_per_int-1)) / _basic_types_per_int;
    if (len <= _compact_int_count) {
      assert(_compact_int_count == 3, "else change next line");
      _value._compact[0] = _value._compact[1] = _value._compact[2] = 0;
      // Storing the signature encoded as signed chars hits about 98%
      // of the time.
      _length = -len;
      ptr = _value._compact;
    } else {
      _length = len;
      _value._fingerprint = NEW_C_HEAP_ARRAY(int, _length, mtCode);
      ptr = _value._fingerprint;
    }

    // Now pack the BasicTypes with 8 per int
    int sig_index = 0;
    BasicType prev_bt = T_ILLEGAL;
    int vt_count = 0;
    for (int index = 0; index < len; index++) {
      int value = 0;
      for (int byte = 0; byte < _basic_types_per_int; byte++) {
        BasicType bt = T_ILLEGAL;
        if (sig_index < total_args_passed) {
          bt = sig->at(sig_index++)._bt;
          if (bt == T_METADATA) {
            // Found start of inline type in signature
            assert(InlineTypePassFieldsAsArgs, "unexpected start of inline type");
            if (sig_index == 1 && has_ro_adapter) {
              // With a ro_adapter, replace receiver inline type delimiter by T_VOID to prevent matching
              // with other adapters that have the same inline type as first argument and no receiver.
              bt = T_VOID;
            }
            vt_count++;
          } else if (bt == T_VOID && prev_bt != T_LONG && prev_bt != T_DOUBLE) {
            // Found end of inline type in signature
            assert(InlineTypePassFieldsAsArgs, "unexpected end of inline type");
            vt_count--;
            assert(vt_count >= 0, "invalid vt_count");
          } else if (vt_count == 0) {
            // Widen fields that are not part of a scalarized inline type argument
            bt = adapter_encoding(bt);
          }
          prev_bt = bt;
        }
        int bt_val = (bt == T_ILLEGAL) ? 0 : bt;
        assert((bt_val & _basic_type_mask) == bt_val, "must fit in 4 bits");
        value = (value << _basic_type_bits) | bt_val;
      }
      ptr[index] = value;
    }
    assert(vt_count == 0, "invalid vt_count");
  }

  ~AdapterFingerPrint() {
    if (_length > 0) {
      FREE_C_HEAP_ARRAY(int, _value._fingerprint);
    }
  }

  int value(int index) {
    if (_length < 0) {
      return _value._compact[index];
    }
    return _value._fingerprint[index];
  }
  int length() {
    if (_length < 0) return -_length;
    return _length;
  }

  bool is_compact() {
    return _length <= 0;
  }

  unsigned int compute_hash() {
    int hash = 0;
    for (int i = 0; i < length(); i++) {
      int v = value(i);
      hash = (hash << 8) ^ v ^ (hash >> 5);
    }
    return (unsigned int)hash;
  }

  const char* as_string() {
    stringStream st;
    st.print("0x");
    for (int i = 0; i < length(); i++) {
      st.print("%x", value(i));
    }
    return st.as_string();
  }

#ifndef PRODUCT
  // Reconstitutes the basic type arguments from the fingerprint,
  // producing strings like LIJDF
  const char* as_basic_args_string() {
    stringStream st;
    bool long_prev = false;
    for (int i = 0; i < length(); i++) {
      unsigned val = (unsigned)value(i);
      // args are packed so that first/lower arguments are in the highest
      // bits of each int value, so iterate from highest to the lowest
      for (int j = 32 - _basic_type_bits; j >= 0; j -= _basic_type_bits) {
        unsigned v = (val >> j) & _basic_type_mask;
        if (v == 0) {
          assert(i == length() - 1, "Only expect zeroes in the last word");
          continue;
        }
        if (long_prev) {
          long_prev = false;
          if (v == T_VOID) {
            st.print("J");
          } else {
            st.print("L");
          }
        } else if (v == T_LONG) {
          long_prev = true;
        } else if (v != T_VOID){
          st.print("%c", type2char((BasicType)v));
        }
      }
    }
    if (long_prev) {
      st.print("L");
    }
    return st.as_string();
  }
#endif // !product

  bool equals(AdapterFingerPrint* other) {
    if (other->_length != _length) {
      return false;
    }
    if (_length < 0) {
      assert(_compact_int_count == 3, "else change next line");
      return _value._compact[0] == other->_value._compact[0] &&
             _value._compact[1] == other->_value._compact[1] &&
             _value._compact[2] == other->_value._compact[2];
    } else {
      for (int i = 0; i < _length; i++) {
        if (_value._fingerprint[i] != other->_value._fingerprint[i]) {
          return false;
        }
      }
    }
    return true;
  }

  static bool equals(AdapterFingerPrint* const& fp1, AdapterFingerPrint* const& fp2) {
    NOT_PRODUCT(_equals++);
    return fp1->equals(fp2);
  }

  static unsigned int compute_hash(AdapterFingerPrint* const& fp) {
    return fp->compute_hash();
  }
};

// A hashtable mapping from AdapterFingerPrints to AdapterHandlerEntries
using AdapterHandlerTable = ResourceHashtable<AdapterFingerPrint*, AdapterHandlerEntry*, 293,
                  AnyObj::C_HEAP, mtCode,
                  AdapterFingerPrint::compute_hash,
                  AdapterFingerPrint::equals>;
static AdapterHandlerTable* _adapter_handler_table;

// Find a entry with the same fingerprint if it exists
static AdapterHandlerEntry* lookup(const GrowableArray<SigEntry>* sig, bool has_ro_adapter = false) {
  NOT_PRODUCT(_lookups++);
  assert_lock_strong(AdapterHandlerLibrary_lock);
  AdapterFingerPrint fp(sig, has_ro_adapter);
  AdapterHandlerEntry** entry = _adapter_handler_table->get(&fp);
  if (entry != nullptr) {
#ifndef PRODUCT
    if (fp.is_compact()) _compact++;
    _hits++;
#endif
    return *entry;
  }
  return nullptr;
}

#ifndef PRODUCT
static void print_table_statistics() {
  auto size = [&] (AdapterFingerPrint* key, AdapterHandlerEntry* a) {
    return sizeof(*key) + sizeof(*a);
  };
  TableStatistics ts = _adapter_handler_table->statistics_calculate(size);
  ts.print(tty, "AdapterHandlerTable");
  tty->print_cr("AdapterHandlerTable (table_size=%d, entries=%d)",
                _adapter_handler_table->table_size(), _adapter_handler_table->number_of_entries());
  tty->print_cr("AdapterHandlerTable: lookups %d equals %d hits %d compact %d",
                _lookups, _equals, _hits, _compact);
}
#endif

// ---------------------------------------------------------------------------
// Implementation of AdapterHandlerLibrary
AdapterHandlerEntry* AdapterHandlerLibrary::_abstract_method_handler = nullptr;
AdapterHandlerEntry* AdapterHandlerLibrary::_no_arg_handler = nullptr;
AdapterHandlerEntry* AdapterHandlerLibrary::_int_arg_handler = nullptr;
AdapterHandlerEntry* AdapterHandlerLibrary::_obj_arg_handler = nullptr;
AdapterHandlerEntry* AdapterHandlerLibrary::_obj_int_arg_handler = nullptr;
AdapterHandlerEntry* AdapterHandlerLibrary::_obj_obj_arg_handler = nullptr;
const int AdapterHandlerLibrary_size = 48*K;
BufferBlob* AdapterHandlerLibrary::_buffer = nullptr;

BufferBlob* AdapterHandlerLibrary::buffer_blob() {
  return _buffer;
}

static void post_adapter_creation(const AdapterBlob* new_adapter,
                                  const AdapterHandlerEntry* entry) {
  if (Forte::is_enabled() || JvmtiExport::should_post_dynamic_code_generated()) {
    char blob_id[256];
    jio_snprintf(blob_id,
                 sizeof(blob_id),
                 "%s(%s)",
                 new_adapter->name(),
                 entry->fingerprint()->as_string());
    if (Forte::is_enabled()) {
      Forte::register_stub(blob_id, new_adapter->content_begin(), new_adapter->content_end());
    }

    if (JvmtiExport::should_post_dynamic_code_generated()) {
      JvmtiExport::post_dynamic_code_generated(blob_id, new_adapter->content_begin(), new_adapter->content_end());
    }
  }
}

void AdapterHandlerLibrary::initialize() {
  ResourceMark rm;
  AdapterBlob* no_arg_blob = nullptr;
  AdapterBlob* int_arg_blob = nullptr;
  AdapterBlob* obj_arg_blob = nullptr;
  AdapterBlob* obj_int_arg_blob = nullptr;
  AdapterBlob* obj_obj_arg_blob = nullptr;
  {
    _adapter_handler_table = new (mtCode) AdapterHandlerTable();
    MutexLocker mu(AdapterHandlerLibrary_lock);

    // Create a special handler for abstract methods.  Abstract methods
    // are never compiled so an i2c entry is somewhat meaningless, but
    // throw AbstractMethodError just in case.
    // Pass wrong_method_abstract for the c2i transitions to return
    // AbstractMethodError for invalid invocations.
    address wrong_method_abstract = SharedRuntime::get_handle_wrong_method_abstract_stub();
    _abstract_method_handler = AdapterHandlerLibrary::new_entry(new AdapterFingerPrint(nullptr),
                                                                SharedRuntime::throw_AbstractMethodError_entry(),
                                                                wrong_method_abstract, wrong_method_abstract, wrong_method_abstract,
                                                                wrong_method_abstract, wrong_method_abstract);
    _buffer = BufferBlob::create("adapters", AdapterHandlerLibrary_size);

    CompiledEntrySignature no_args;
    no_args.compute_calling_conventions();
    _no_arg_handler = create_adapter(no_arg_blob, no_args, true);

    CompiledEntrySignature obj_args;
    SigEntry::add_entry(obj_args.sig(), T_OBJECT);
    obj_args.compute_calling_conventions();
    _obj_arg_handler = create_adapter(obj_arg_blob, obj_args, true);

    CompiledEntrySignature int_args;
    SigEntry::add_entry(int_args.sig(), T_INT);
    int_args.compute_calling_conventions();
    _int_arg_handler = create_adapter(int_arg_blob, int_args, true);

    CompiledEntrySignature obj_int_args;
    SigEntry::add_entry(obj_int_args.sig(), T_OBJECT);
    SigEntry::add_entry(obj_int_args.sig(), T_INT);
    obj_int_args.compute_calling_conventions();
    _obj_int_arg_handler = create_adapter(obj_int_arg_blob, obj_int_args, true);

    CompiledEntrySignature obj_obj_args;
    SigEntry::add_entry(obj_obj_args.sig(), T_OBJECT);
    SigEntry::add_entry(obj_obj_args.sig(), T_OBJECT);
    obj_obj_args.compute_calling_conventions();
    _obj_obj_arg_handler = create_adapter(obj_obj_arg_blob, obj_obj_args, true);

    assert(no_arg_blob != nullptr &&
          obj_arg_blob != nullptr &&
          int_arg_blob != nullptr &&
          obj_int_arg_blob != nullptr &&
          obj_obj_arg_blob != nullptr, "Initial adapters must be properly created");
  }
  return;

  // Outside of the lock
  post_adapter_creation(no_arg_blob, _no_arg_handler);
  post_adapter_creation(obj_arg_blob, _obj_arg_handler);
  post_adapter_creation(int_arg_blob, _int_arg_handler);
  post_adapter_creation(obj_int_arg_blob, _obj_int_arg_handler);
  post_adapter_creation(obj_obj_arg_blob, _obj_obj_arg_handler);
}

AdapterHandlerEntry* AdapterHandlerLibrary::new_entry(AdapterFingerPrint* fingerprint,
                                                      address i2c_entry,
                                                      address c2i_entry,
                                                      address c2i_inline_entry,
                                                      address c2i_inline_ro_entry,
                                                      address c2i_unverified_entry,
                                                      address c2i_unverified_inline_entry,
                                                      address c2i_no_clinit_check_entry) {
  return new AdapterHandlerEntry(fingerprint, i2c_entry, c2i_entry, c2i_inline_entry, c2i_inline_ro_entry, c2i_unverified_entry,
                              c2i_unverified_inline_entry, c2i_no_clinit_check_entry);
}

AdapterHandlerEntry* AdapterHandlerLibrary::get_simple_adapter(const methodHandle& method) {
  if (method->is_abstract()) {
    return nullptr;
  }
  int total_args_passed = method->size_of_parameters(); // All args on stack
  if (total_args_passed == 0) {
    return _no_arg_handler;
  } else if (total_args_passed == 1) {
    if (!method->is_static()) {
      if (InlineTypePassFieldsAsArgs && method->method_holder()->is_inline_klass()) {
        return nullptr;
      }
      return _obj_arg_handler;
    }
    switch (method->signature()->char_at(1)) {
      case JVM_SIGNATURE_CLASS: {
        if (InlineTypePassFieldsAsArgs) {
          SignatureStream ss(method->signature());
          InlineKlass* vk = ss.as_inline_klass(method->method_holder());
          if (vk != nullptr) {
            return nullptr;
          }
        }
        return _obj_arg_handler;
      }
      case JVM_SIGNATURE_ARRAY:
        return _obj_arg_handler;
      case JVM_SIGNATURE_INT:
      case JVM_SIGNATURE_BOOLEAN:
      case JVM_SIGNATURE_CHAR:
      case JVM_SIGNATURE_BYTE:
      case JVM_SIGNATURE_SHORT:
        return _int_arg_handler;
    }
  } else if (total_args_passed == 2 &&
             !method->is_static() && (!InlineTypePassFieldsAsArgs || !method->method_holder()->is_inline_klass())) {
    switch (method->signature()->char_at(1)) {
      case JVM_SIGNATURE_CLASS: {
        if (InlineTypePassFieldsAsArgs) {
          SignatureStream ss(method->signature());
          InlineKlass* vk = ss.as_inline_klass(method->method_holder());
          if (vk != nullptr) {
            return nullptr;
          }
        }
        return _obj_obj_arg_handler;
      }
      case JVM_SIGNATURE_ARRAY:
        return _obj_obj_arg_handler;
      case JVM_SIGNATURE_INT:
      case JVM_SIGNATURE_BOOLEAN:
      case JVM_SIGNATURE_CHAR:
      case JVM_SIGNATURE_BYTE:
      case JVM_SIGNATURE_SHORT:
        return _obj_int_arg_handler;
    }
  }
  return nullptr;
}

CompiledEntrySignature::CompiledEntrySignature(Method* method) :
  _method(method), _num_inline_args(0), _has_inline_recv(false),
  _regs(nullptr), _regs_cc(nullptr), _regs_cc_ro(nullptr),
  _args_on_stack(0), _args_on_stack_cc(0), _args_on_stack_cc_ro(0),
  _c1_needs_stack_repair(false), _c2_needs_stack_repair(false), _supers(nullptr) {
  _sig = new GrowableArray<SigEntry>((method != nullptr) ? method->size_of_parameters() : 1);
  _sig_cc = new GrowableArray<SigEntry>((method != nullptr) ? method->size_of_parameters() : 1);
  _sig_cc_ro = new GrowableArray<SigEntry>((method != nullptr) ? method->size_of_parameters() : 1);
}

// See if we can save space by sharing the same entry for VIEP and VIEP(RO),
// or the same entry for VEP and VIEP(RO).
CodeOffsets::Entries CompiledEntrySignature::c1_inline_ro_entry_type() const {
  if (!has_scalarized_args()) {
    // VEP/VIEP/VIEP(RO) all share the same entry. There's no packing.
    return CodeOffsets::Verified_Entry;
  }
  if (_method->is_static()) {
    // Static methods don't need VIEP(RO)
    return CodeOffsets::Verified_Entry;
  }

  if (has_inline_recv()) {
    if (num_inline_args() == 1) {
      // Share same entry for VIEP and VIEP(RO).
      // This is quite common: we have an instance method in an InlineKlass that has
      // no inline type args other than <this>.
      return CodeOffsets::Verified_Inline_Entry;
    } else {
      assert(num_inline_args() > 1, "must be");
      // No sharing:
      //   VIEP(RO) -- <this> is passed as object
      //   VEP      -- <this> is passed as fields
      return CodeOffsets::Verified_Inline_Entry_RO;
    }
  }

  // Either a static method, or <this> is not an inline type
  if (args_on_stack_cc() != args_on_stack_cc_ro()) {
    // No sharing:
    // Some arguments are passed on the stack, and we have inserted reserved entries
    // into the VEP, but we never insert reserved entries into the VIEP(RO).
    return CodeOffsets::Verified_Inline_Entry_RO;
  } else {
    // Share same entry for VEP and VIEP(RO).
    return CodeOffsets::Verified_Entry;
  }
}

// Returns all super methods (transitive) in classes and interfaces that are overridden by the current method.
GrowableArray<Method*>* CompiledEntrySignature::get_supers() {
  if (_supers != nullptr) {
    return _supers;
  }
  _supers = new GrowableArray<Method*>();
  // Skip private, static, and <init> methods
  if (_method->is_private() || _method->is_static() || _method->is_object_constructor()) {
    return _supers;
  }
  Symbol* name = _method->name();
  Symbol* signature = _method->signature();
  const Klass* holder = _method->method_holder()->super();
  Symbol* holder_name = holder->name();
  ThreadInVMfromUnknown tiv;
  JavaThread* current = JavaThread::current();
  HandleMark hm(current);
  Handle loader(current, _method->method_holder()->class_loader());

  // Walk up the class hierarchy and search for super methods
  while (holder != nullptr) {
    Method* super_method = holder->lookup_method(name, signature);
    if (super_method == nullptr) {
      break;
    }
    if (!super_method->is_static() && !super_method->is_private() &&
        (!super_method->is_package_private() ||
         super_method->method_holder()->is_same_class_package(loader(), holder_name))) {
      _supers->push(super_method);
    }
    holder = super_method->method_holder()->super();
  }
  // Search interfaces for super methods
  Array<InstanceKlass*>* interfaces = _method->method_holder()->transitive_interfaces();
  for (int i = 0; i < interfaces->length(); ++i) {
    Method* m = interfaces->at(i)->lookup_method(name, signature);
    if (m != nullptr && !m->is_static() && m->is_public()) {
      _supers->push(m);
    }
  }
  return _supers;
}

// Iterate over arguments and compute scalarized and non-scalarized signatures
void CompiledEntrySignature::compute_calling_conventions(bool init) {
  bool has_scalarized = false;
  if (_method != nullptr) {
    InstanceKlass* holder = _method->method_holder();
    int arg_num = 0;
    if (!_method->is_static()) {
      // We shouldn't scalarize 'this' in a value class constructor
      if (holder->is_inline_klass() && InlineKlass::cast(holder)->can_be_passed_as_fields() && !_method->is_object_constructor() &&
          (init || _method->is_scalarized_arg(arg_num))) {
        _sig_cc->appendAll(InlineKlass::cast(holder)->extended_sig());
        has_scalarized = true;
        _has_inline_recv = true;
        _num_inline_args++;
      } else {
        SigEntry::add_entry(_sig_cc, T_OBJECT, holder->name());
      }
      SigEntry::add_entry(_sig, T_OBJECT, holder->name());
      SigEntry::add_entry(_sig_cc_ro, T_OBJECT, holder->name());
      arg_num++;
    }
    for (SignatureStream ss(_method->signature()); !ss.at_return_type(); ss.next()) {
      BasicType bt = ss.type();
      if (bt == T_OBJECT) {
        InlineKlass* vk = ss.as_inline_klass(holder);
        if (vk != nullptr && vk->can_be_passed_as_fields() && (init || _method->is_scalarized_arg(arg_num))) {
          // Check for a calling convention mismatch with super method(s)
          bool scalar_super = false;
          bool non_scalar_super = false;
          GrowableArray<Method*>* supers = get_supers();
          for (int i = 0; i < supers->length(); ++i) {
            Method* super_method = supers->at(i);
            if (super_method->is_scalarized_arg(arg_num)) {
              scalar_super = true;
            } else {
              non_scalar_super = true;
            }
          }
#ifdef ASSERT
          // Randomly enable below code paths for stress testing
          bool stress = init && StressCallingConvention;
          if (stress && (os::random() & 1) == 1) {
            non_scalar_super = true;
            if ((os::random() & 1) == 1) {
              scalar_super = true;
            }
          }
#endif
          if (non_scalar_super) {
            // Found a super method with a non-scalarized argument. Fall back to the non-scalarized calling convention.
            if (scalar_super) {
              // Found non-scalar *and* scalar super methods. We can't handle both.
              // Mark the scalar method as mismatch and re-compile call sites to use non-scalarized calling convention.
              for (int i = 0; i < supers->length(); ++i) {
                Method* super_method = supers->at(i);
                if (super_method->is_scalarized_arg(arg_num) DEBUG_ONLY(|| (stress && (os::random() & 1) == 1))) {
                  super_method->set_mismatch();
                  MutexLocker ml(Compile_lock, Mutex::_safepoint_check_flag);
                  JavaThread* thread = JavaThread::current();
                  HandleMark hm(thread);
                  methodHandle mh(thread, super_method);
                  DeoptimizationScope deopt_scope;
                  CodeCache::mark_for_deoptimization(&deopt_scope, mh());
                  deopt_scope.deoptimize_marked();
                }
              }
            }
            // Fall back to non-scalarized calling convention
            SigEntry::add_entry(_sig_cc, T_OBJECT, ss.as_symbol());
            SigEntry::add_entry(_sig_cc_ro, T_OBJECT, ss.as_symbol());
          } else {
            _num_inline_args++;
            has_scalarized = true;
            int last = _sig_cc->length();
            int last_ro = _sig_cc_ro->length();
            _sig_cc->appendAll(vk->extended_sig());
            _sig_cc_ro->appendAll(vk->extended_sig());
            if (bt == T_OBJECT) {
              // Nullable inline type argument, insert InlineTypeNode::NullMarker field right after T_METADATA delimiter
              _sig_cc->insert_before(last+1, SigEntry(T_BOOLEAN, -1, nullptr, true));
              _sig_cc_ro->insert_before(last_ro+1, SigEntry(T_BOOLEAN, -1, nullptr, true));
            }
          }
        } else {
          SigEntry::add_entry(_sig_cc, T_OBJECT, ss.as_symbol());
          SigEntry::add_entry(_sig_cc_ro, T_OBJECT, ss.as_symbol());
        }
        bt = T_OBJECT;
      } else {
        SigEntry::add_entry(_sig_cc, ss.type(), ss.as_symbol());
        SigEntry::add_entry(_sig_cc_ro, ss.type(), ss.as_symbol());
      }
      SigEntry::add_entry(_sig, bt, ss.as_symbol());
      if (bt != T_VOID) {
        arg_num++;
      }
    }
  }

  // Compute the non-scalarized calling convention
  _regs = NEW_RESOURCE_ARRAY(VMRegPair, _sig->length());
  _args_on_stack = SharedRuntime::java_calling_convention(_sig, _regs);

  // Compute the scalarized calling conventions if there are scalarized inline types in the signature
  if (has_scalarized && !_method->is_native()) {
    _regs_cc = NEW_RESOURCE_ARRAY(VMRegPair, _sig_cc->length());
    _args_on_stack_cc = SharedRuntime::java_calling_convention(_sig_cc, _regs_cc);

    _regs_cc_ro = NEW_RESOURCE_ARRAY(VMRegPair, _sig_cc_ro->length());
    _args_on_stack_cc_ro = SharedRuntime::java_calling_convention(_sig_cc_ro, _regs_cc_ro);

    _c1_needs_stack_repair = (_args_on_stack_cc < _args_on_stack) || (_args_on_stack_cc_ro < _args_on_stack);
    _c2_needs_stack_repair = (_args_on_stack_cc > _args_on_stack) || (_args_on_stack_cc > _args_on_stack_cc_ro);

    // Upper bound on stack arguments to avoid hitting the argument limit and
    // bailing out of compilation ("unsupported incoming calling sequence").
    // TODO we need a reasonable limit (flag?) here
    if (MAX2(_args_on_stack_cc, _args_on_stack_cc_ro) <= 60) {
      return; // Success
    }
  }

  // No scalarized args
  _sig_cc = _sig;
  _regs_cc = _regs;
  _args_on_stack_cc = _args_on_stack;

  _sig_cc_ro = _sig;
  _regs_cc_ro = _regs;
  _args_on_stack_cc_ro = _args_on_stack;
}

AdapterHandlerEntry* AdapterHandlerLibrary::get_adapter(const methodHandle& method) {
  // Use customized signature handler.  Need to lock around updates to
  // the _adapter_handler_table (it is not safe for concurrent readers
  // and a single writer: this could be fixed if it becomes a
  // problem).

  // Fast-path for trivial adapters
  AdapterHandlerEntry* entry = get_simple_adapter(method);
  if (entry != nullptr) {
    return entry;
  }

  ResourceMark rm;
  AdapterBlob* new_adapter = nullptr;

  CompiledEntrySignature ces(method());
  ces.compute_calling_conventions();
  if (ces.has_scalarized_args()) {
    if (!method->has_scalarized_args()) {
      method->set_has_scalarized_args();
    }
    if (ces.c1_needs_stack_repair()) {
      method->set_c1_needs_stack_repair();
    }
    if (ces.c2_needs_stack_repair() && !method->c2_needs_stack_repair()) {
      method->set_c2_needs_stack_repair();
    }
  } else if (method->is_abstract()) {
    return _abstract_method_handler;
  }

  {
    MutexLocker mu(AdapterHandlerLibrary_lock);

    if (ces.has_scalarized_args() && method->is_abstract()) {
      // Save a C heap allocated version of the signature for abstract methods with scalarized inline type arguments
      address wrong_method_abstract = SharedRuntime::get_handle_wrong_method_abstract_stub();
      entry = AdapterHandlerLibrary::new_entry(new AdapterFingerPrint(nullptr),
                                               SharedRuntime::throw_AbstractMethodError_entry(),
                                               wrong_method_abstract, wrong_method_abstract, wrong_method_abstract,
                                               wrong_method_abstract, wrong_method_abstract);
      GrowableArray<SigEntry>* heap_sig = new (mtInternal) GrowableArray<SigEntry>(ces.sig_cc_ro()->length(), mtInternal);
      heap_sig->appendAll(ces.sig_cc_ro());
      entry->set_sig_cc(heap_sig);
      return entry;
    }

    // Lookup method signature's fingerprint
    entry = lookup(ces.sig_cc(), ces.has_inline_recv());

    if (entry != nullptr) {
#ifdef ASSERT
      if (VerifyAdapterSharing) {
        AdapterBlob* comparison_blob = nullptr;
        AdapterHandlerEntry* comparison_entry = create_adapter(comparison_blob, ces, false);
        assert(comparison_blob == nullptr, "no blob should be created when creating an adapter for comparison");
        assert(comparison_entry->compare_code(entry), "code must match");
        // Release the one just created and return the original
        delete comparison_entry;
      }
#endif
      return entry;
    }

    entry = create_adapter(new_adapter, ces, /* allocate_code_blob */ true);
  }

  // Outside of the lock
  if (new_adapter != nullptr) {
    post_adapter_creation(new_adapter, entry);
  }
  return entry;
}

AdapterHandlerEntry* AdapterHandlerLibrary::create_adapter(AdapterBlob*& new_adapter,
                                                           CompiledEntrySignature& ces,
                                                           bool allocate_code_blob) {
  if (log_is_enabled(Info, perf, class, link)) {
    ClassLoader::perf_method_adapters_count()->inc();
  }

  // StubRoutines::_final_stubs_code is initialized after this function can be called. As a result,
  // VerifyAdapterCalls and VerifyAdapterSharing can fail if we re-use code that generated prior
  // to all StubRoutines::_final_stubs_code being set. Checks refer to runtime range checks generated
  // in an I2C stub that ensure that an I2C stub is called from an interpreter frame or stubs.
  bool contains_all_checks = StubRoutines::final_stubs_code() != nullptr;

  BufferBlob* buf = buffer_blob(); // the temporary code buffer in CodeCache
  CodeBuffer buffer(buf);
  short buffer_locs[20];
  buffer.insts()->initialize_shared_locs((relocInfo*)buffer_locs,
                                          sizeof(buffer_locs)/sizeof(relocInfo));

  // Make a C heap allocated version of the fingerprint to store in the adapter
  AdapterFingerPrint* fingerprint = new AdapterFingerPrint(ces.sig_cc(), ces.has_inline_recv());
  MacroAssembler _masm(&buffer);
  AdapterHandlerEntry* entry = SharedRuntime::generate_i2c2i_adapters(&_masm,
                                                ces.args_on_stack(),
                                                ces.sig(),
                                                ces.regs(),
                                                ces.sig_cc(),
                                                ces.regs_cc(),
                                                ces.sig_cc_ro(),
                                                ces.regs_cc_ro(),
                                                fingerprint,
                                                new_adapter,
                                                allocate_code_blob);

  if (ces.has_scalarized_args()) {
    // Save a C heap allocated version of the scalarized signature and store it in the adapter
    GrowableArray<SigEntry>* heap_sig = new (mtInternal) GrowableArray<SigEntry>(ces.sig_cc()->length(), mtInternal);
    heap_sig->appendAll(ces.sig_cc());
    entry->set_sig_cc(heap_sig);
  }

#ifdef ASSERT
  if (VerifyAdapterSharing) {
    entry->save_code(buf->code_begin(), buffer.insts_size());
    if (!allocate_code_blob) {
      return entry;
    }
  }
#endif

  NOT_PRODUCT(int insts_size = buffer.insts_size());
  if (new_adapter == nullptr) {
    // CodeCache is full, disable compilation
    // Ought to log this but compile log is only per compile thread
    // and we're some non descript Java thread.
    return nullptr;
  }
  entry->relocate(new_adapter->content_begin());
#ifndef PRODUCT
  // debugging support
  if (PrintAdapterHandlers || PrintStubCode) {
    ttyLocker ttyl;
    entry->print_adapter_on(tty);
    tty->print_cr("i2c argument handler #%d for: %s %s (%d bytes generated)",
                  _adapter_handler_table->number_of_entries(), fingerprint->as_basic_args_string(),
                  fingerprint->as_string(), insts_size);
    tty->print_cr("c2i argument handler starts at " INTPTR_FORMAT, p2i(entry->get_c2i_entry()));
    if (Verbose || PrintStubCode) {
      address first_pc = entry->base_address();
      if (first_pc != nullptr) {
        Disassembler::decode(first_pc, first_pc + insts_size, tty
                             NOT_PRODUCT(COMMA &new_adapter->asm_remarks()));
        tty->cr();
      }
    }
  }
#endif

  // Add the entry only if the entry contains all required checks (see sharedRuntime_xxx.cpp)
  // The checks are inserted only if -XX:+VerifyAdapterCalls is specified.
  if (contains_all_checks || !VerifyAdapterCalls) {
    assert_lock_strong(AdapterHandlerLibrary_lock);
    _adapter_handler_table->put(fingerprint, entry);
  }
  return entry;
}

address AdapterHandlerEntry::base_address() {
  address base = _i2c_entry;
  if (base == nullptr)  base = _c2i_entry;
  assert(base <= _c2i_entry || _c2i_entry == nullptr, "");
  assert(base <= _c2i_inline_entry || _c2i_inline_entry == nullptr, "");
  assert(base <= _c2i_inline_ro_entry || _c2i_inline_ro_entry == nullptr, "");
  assert(base <= _c2i_unverified_entry || _c2i_unverified_entry == nullptr, "");
  assert(base <= _c2i_unverified_inline_entry || _c2i_unverified_inline_entry == nullptr, "");
  assert(base <= _c2i_no_clinit_check_entry || _c2i_no_clinit_check_entry == nullptr, "");
  return base;
}

void AdapterHandlerEntry::relocate(address new_base) {
  address old_base = base_address();
  assert(old_base != nullptr, "");
  ptrdiff_t delta = new_base - old_base;
  if (_i2c_entry != nullptr)
    _i2c_entry += delta;
  if (_c2i_entry != nullptr)
    _c2i_entry += delta;
  if (_c2i_inline_entry != nullptr)
    _c2i_inline_entry += delta;
  if (_c2i_inline_ro_entry != nullptr)
    _c2i_inline_ro_entry += delta;
  if (_c2i_unverified_entry != nullptr)
    _c2i_unverified_entry += delta;
  if (_c2i_unverified_inline_entry != nullptr)
    _c2i_unverified_inline_entry += delta;
  if (_c2i_no_clinit_check_entry != nullptr)
    _c2i_no_clinit_check_entry += delta;
  assert(base_address() == new_base, "");
}


AdapterHandlerEntry::~AdapterHandlerEntry() {
  delete _fingerprint;
  if (_sig_cc != nullptr) {
    delete _sig_cc;
  }
#ifdef ASSERT
  FREE_C_HEAP_ARRAY(unsigned char, _saved_code);
#endif
}


#ifdef ASSERT
// Capture the code before relocation so that it can be compared
// against other versions.  If the code is captured after relocation
// then relative instructions won't be equivalent.
void AdapterHandlerEntry::save_code(unsigned char* buffer, int length) {
  _saved_code = NEW_C_HEAP_ARRAY(unsigned char, length, mtCode);
  _saved_code_length = length;
  memcpy(_saved_code, buffer, length);
}


bool AdapterHandlerEntry::compare_code(AdapterHandlerEntry* other) {
  assert(_saved_code != nullptr && other->_saved_code != nullptr, "code not saved");

  if (other->_saved_code_length != _saved_code_length) {
    return false;
  }

  return memcmp(other->_saved_code, _saved_code, _saved_code_length) == 0;
}
#endif


/**
 * Create a native wrapper for this native method.  The wrapper converts the
 * Java-compiled calling convention to the native convention, handles
 * arguments, and transitions to native.  On return from the native we transition
 * back to java blocking if a safepoint is in progress.
 */
void AdapterHandlerLibrary::create_native_wrapper(const methodHandle& method) {
  ResourceMark rm;
  nmethod* nm = nullptr;

  // Check if memory should be freed before allocation
  CodeCache::gc_on_allocation();

  assert(method->is_native(), "must be native");
  assert(method->is_special_native_intrinsic() ||
         method->has_native_function(), "must have something valid to call!");

  {
    // Perform the work while holding the lock, but perform any printing outside the lock
    MutexLocker mu(AdapterHandlerLibrary_lock);
    // See if somebody beat us to it
    if (method->code() != nullptr) {
      return;
    }

    const int compile_id = CompileBroker::assign_compile_id(method, CompileBroker::standard_entry_bci);
    assert(compile_id > 0, "Must generate native wrapper");


    ResourceMark rm;
    BufferBlob*  buf = buffer_blob(); // the temporary code buffer in CodeCache
    if (buf != nullptr) {
      CodeBuffer buffer(buf);

      if (method->is_continuation_enter_intrinsic()) {
        buffer.initialize_stubs_size(192);
      }

      struct { double data[20]; } locs_buf;
      struct { double data[20]; } stubs_locs_buf;
      buffer.insts()->initialize_shared_locs((relocInfo*)&locs_buf, sizeof(locs_buf) / sizeof(relocInfo));
#if defined(AARCH64) || defined(PPC64)
      // On AArch64 with ZGC and nmethod entry barriers, we need all oops to be
      // in the constant pool to ensure ordering between the barrier and oops
      // accesses. For native_wrappers we need a constant.
      // On PPC64 the continuation enter intrinsic needs the constant pool for the compiled
      // static java call that is resolved in the runtime.
      if (PPC64_ONLY(method->is_continuation_enter_intrinsic() &&) true) {
        buffer.initialize_consts_size(8 PPC64_ONLY(+ 24));
      }
#endif
      buffer.stubs()->initialize_shared_locs((relocInfo*)&stubs_locs_buf, sizeof(stubs_locs_buf) / sizeof(relocInfo));
      MacroAssembler _masm(&buffer);

      // Fill in the signature array, for the calling-convention call.
      const int total_args_passed = method->size_of_parameters();

      BasicType stack_sig_bt[16];
      VMRegPair stack_regs[16];
      BasicType* sig_bt = (total_args_passed <= 16) ? stack_sig_bt : NEW_RESOURCE_ARRAY(BasicType, total_args_passed);
      VMRegPair* regs = (total_args_passed <= 16) ? stack_regs : NEW_RESOURCE_ARRAY(VMRegPair, total_args_passed);

      int i = 0;
      if (!method->is_static()) {  // Pass in receiver first
        sig_bt[i++] = T_OBJECT;
      }
      SignatureStream ss(method->signature());
      for (; !ss.at_return_type(); ss.next()) {
        sig_bt[i++] = ss.type();  // Collect remaining bits of signature
        if (ss.type() == T_LONG || ss.type() == T_DOUBLE) {
          sig_bt[i++] = T_VOID;   // Longs & doubles take 2 Java slots
        }
      }
      assert(i == total_args_passed, "");
      BasicType ret_type = ss.type();

      // Now get the compiled-Java arguments layout.
      SharedRuntime::java_calling_convention(sig_bt, regs, total_args_passed);

      // Generate the compiled-to-native wrapper code
      nm = SharedRuntime::generate_native_wrapper(&_masm, method, compile_id, sig_bt, regs, ret_type);

      if (nm != nullptr) {
        {
          MutexLocker pl(NMethodState_lock, Mutex::_no_safepoint_check_flag);
          if (nm->make_in_use()) {
            method->set_code(method, nm);
          }
        }

        DirectiveSet* directive = DirectivesStack::getMatchingDirective(method, CompileBroker::compiler(CompLevel_simple));
        if (directive->PrintAssemblyOption) {
          nm->print_code();
        }
        DirectivesStack::release(directive);
      }
    }
  } // Unlock AdapterHandlerLibrary_lock


  // Install the generated code.
  if (nm != nullptr) {
    const char *msg = method->is_static() ? "(static)" : "";
    CompileTask::print_ul(nm, msg);
    if (PrintCompilation) {
      ttyLocker ttyl;
      CompileTask::print(tty, nm, msg);
    }
    nm->post_compiled_method_load_event();
  }
}

// -------------------------------------------------------------------------
// Java-Java calling convention
// (what you use when Java calls Java)

//------------------------------name_for_receiver----------------------------------
// For a given signature, return the VMReg for parameter 0.
VMReg SharedRuntime::name_for_receiver() {
  VMRegPair regs;
  BasicType sig_bt = T_OBJECT;
  (void) java_calling_convention(&sig_bt, &regs, 1);
  // Return argument 0 register.  In the LP64 build pointers
  // take 2 registers, but the VM wants only the 'main' name.
  return regs.first();
}

VMRegPair *SharedRuntime::find_callee_arguments(Symbol* sig, bool has_receiver, bool has_appendix, int* arg_size) {
  // This method is returning a data structure allocating as a
  // ResourceObject, so do not put any ResourceMarks in here.

  BasicType *sig_bt = NEW_RESOURCE_ARRAY(BasicType, 256);
  VMRegPair *regs = NEW_RESOURCE_ARRAY(VMRegPair, 256);
  int cnt = 0;
  if (has_receiver) {
    sig_bt[cnt++] = T_OBJECT; // Receiver is argument 0; not in signature
  }

  for (SignatureStream ss(sig); !ss.at_return_type(); ss.next()) {
    BasicType type = ss.type();
    sig_bt[cnt++] = type;
    if (is_double_word_type(type))
      sig_bt[cnt++] = T_VOID;
  }

  if (has_appendix) {
    sig_bt[cnt++] = T_OBJECT;
  }

  assert(cnt < 256, "grow table size");

  int comp_args_on_stack;
  comp_args_on_stack = java_calling_convention(sig_bt, regs, cnt);

  // the calling convention doesn't count out_preserve_stack_slots so
  // we must add that in to get "true" stack offsets.

  if (comp_args_on_stack) {
    for (int i = 0; i < cnt; i++) {
      VMReg reg1 = regs[i].first();
      if (reg1->is_stack()) {
        // Yuck
        reg1 = reg1->bias(out_preserve_stack_slots());
      }
      VMReg reg2 = regs[i].second();
      if (reg2->is_stack()) {
        // Yuck
        reg2 = reg2->bias(out_preserve_stack_slots());
      }
      regs[i].set_pair(reg2, reg1);
    }
  }

  // results
  *arg_size = cnt;
  return regs;
}

// OSR Migration Code
//
// This code is used convert interpreter frames into compiled frames.  It is
// called from very start of a compiled OSR nmethod.  A temp array is
// allocated to hold the interesting bits of the interpreter frame.  All
// active locks are inflated to allow them to move.  The displaced headers and
// active interpreter locals are copied into the temp buffer.  Then we return
// back to the compiled code.  The compiled code then pops the current
// interpreter frame off the stack and pushes a new compiled frame.  Then it
// copies the interpreter locals and displaced headers where it wants.
// Finally it calls back to free the temp buffer.
//
// All of this is done NOT at any Safepoint, nor is any safepoint or GC allowed.

JRT_LEAF(intptr_t*, SharedRuntime::OSR_migration_begin( JavaThread *current) )
  assert(current == JavaThread::current(), "pre-condition");

  // During OSR migration, we unwind the interpreted frame and replace it with a compiled
  // frame. The stack watermark code below ensures that the interpreted frame is processed
  // before it gets unwound. This is helpful as the size of the compiled frame could be
  // larger than the interpreted frame, which could result in the new frame not being
  // processed correctly.
  StackWatermarkSet::before_unwind(current);

  //
  // This code is dependent on the memory layout of the interpreter local
  // array and the monitors. On all of our platforms the layout is identical
  // so this code is shared. If some platform lays the their arrays out
  // differently then this code could move to platform specific code or
  // the code here could be modified to copy items one at a time using
  // frame accessor methods and be platform independent.

  frame fr = current->last_frame();
  assert(fr.is_interpreted_frame(), "");
  assert(fr.interpreter_frame_expression_stack_size()==0, "only handle empty stacks");

  // Figure out how many monitors are active.
  int active_monitor_count = 0;
  for (BasicObjectLock *kptr = fr.interpreter_frame_monitor_end();
       kptr < fr.interpreter_frame_monitor_begin();
       kptr = fr.next_monitor_in_interpreter_frame(kptr) ) {
    if (kptr->obj() != nullptr) active_monitor_count++;
  }

  // QQQ we could place number of active monitors in the array so that compiled code
  // could double check it.

  Method* moop = fr.interpreter_frame_method();
  int max_locals = moop->max_locals();
  // Allocate temp buffer, 1 word per local & 2 per active monitor
  int buf_size_words = max_locals + active_monitor_count * BasicObjectLock::size();
  intptr_t *buf = NEW_C_HEAP_ARRAY(intptr_t,buf_size_words, mtCode);

  // Copy the locals.  Order is preserved so that loading of longs works.
  // Since there's no GC I can copy the oops blindly.
  assert(sizeof(HeapWord)==sizeof(intptr_t), "fix this code");
  Copy::disjoint_words((HeapWord*)fr.interpreter_frame_local_at(max_locals-1),
                       (HeapWord*)&buf[0],
                       max_locals);

  // Inflate locks.  Copy the displaced headers.  Be careful, there can be holes.
  int i = max_locals;
  for (BasicObjectLock *kptr2 = fr.interpreter_frame_monitor_end();
       kptr2 < fr.interpreter_frame_monitor_begin();
       kptr2 = fr.next_monitor_in_interpreter_frame(kptr2) ) {
    if (kptr2->obj() != nullptr) {         // Avoid 'holes' in the monitor array
      BasicLock *lock = kptr2->lock();
      if (LockingMode == LM_LEGACY) {
        // Inflate so the object's header no longer refers to the BasicLock.
        if (lock->displaced_header().is_unlocked()) {
          // The object is locked and the resulting ObjectMonitor* will also be
          // locked so it can't be async deflated until ownership is dropped.
          // See the big comment in basicLock.cpp: BasicLock::move_to().
          ObjectSynchronizer::inflate_helper(kptr2->obj());
        }
        // Now the displaced header is free to move because the
        // object's header no longer refers to it.
        buf[i] = (intptr_t)lock->displaced_header().value();
      } else if (UseObjectMonitorTable) {
        buf[i] = (intptr_t)lock->object_monitor_cache();
      }
#ifdef ASSERT
      else {
        buf[i] = badDispHeaderOSR;
      }
#endif
      i++;
      buf[i++] = cast_from_oop<intptr_t>(kptr2->obj());
    }
  }
  assert(i - max_locals == active_monitor_count*2, "found the expected number of monitors");

  RegisterMap map(current,
                  RegisterMap::UpdateMap::skip,
                  RegisterMap::ProcessFrames::include,
                  RegisterMap::WalkContinuation::skip);
  frame sender = fr.sender(&map);
  if (sender.is_interpreted_frame()) {
    current->push_cont_fastpath(sender.sp());
  }

  return buf;
JRT_END

JRT_LEAF(void, SharedRuntime::OSR_migration_end( intptr_t* buf) )
  FREE_C_HEAP_ARRAY(intptr_t, buf);
JRT_END

bool AdapterHandlerLibrary::contains(const CodeBlob* b) {
  bool found = false;
  auto findblob = [&] (AdapterFingerPrint* key, AdapterHandlerEntry* a) {
    return (found = (b == CodeCache::find_blob(a->get_i2c_entry())));
  };
  assert_locked_or_safepoint(AdapterHandlerLibrary_lock);
  _adapter_handler_table->iterate(findblob);
  return found;
}

void AdapterHandlerLibrary::print_handler_on(outputStream* st, const CodeBlob* b) {
  bool found = false;
  auto findblob = [&] (AdapterFingerPrint* key, AdapterHandlerEntry* a) {
    if (b == CodeCache::find_blob(a->get_i2c_entry())) {
      found = true;
      st->print("Adapter for signature: ");
      a->print_adapter_on(st);
      return true;
    } else {
      return false; // keep looking
    }
  };
  assert_locked_or_safepoint(AdapterHandlerLibrary_lock);
  _adapter_handler_table->iterate(findblob);
  assert(found, "Should have found handler");
}

void AdapterHandlerEntry::print_adapter_on(outputStream* st) const {
  st->print("AHE@" INTPTR_FORMAT ": %s", p2i(this), fingerprint()->as_string());
  if (get_i2c_entry() != nullptr) {
    st->print(" i2c: " INTPTR_FORMAT, p2i(get_i2c_entry()));
  }
  if (get_c2i_entry() != nullptr) {
    st->print(" c2i: " INTPTR_FORMAT, p2i(get_c2i_entry()));
  }
  if (get_c2i_entry() != nullptr) {
    st->print(" c2iVE: " INTPTR_FORMAT, p2i(get_c2i_inline_entry()));
  }
  if (get_c2i_entry() != nullptr) {
    st->print(" c2iVROE: " INTPTR_FORMAT, p2i(get_c2i_inline_ro_entry()));
  }
  if (get_c2i_unverified_entry() != nullptr) {
    st->print(" c2iUE: " INTPTR_FORMAT, p2i(get_c2i_unverified_entry()));
  }
  if (get_c2i_unverified_entry() != nullptr) {
    st->print(" c2iUVE: " INTPTR_FORMAT, p2i(get_c2i_unverified_inline_entry()));
  }
  if (get_c2i_no_clinit_check_entry() != nullptr) {
    st->print(" c2iNCI: " INTPTR_FORMAT, p2i(get_c2i_no_clinit_check_entry()));
  }
  st->cr();
}

#ifndef PRODUCT

void AdapterHandlerLibrary::print_statistics() {
  print_table_statistics();
}

#endif /* PRODUCT */

JRT_LEAF(void, SharedRuntime::enable_stack_reserved_zone(JavaThread* current))
  assert(current == JavaThread::current(), "pre-condition");
  StackOverflow* overflow_state = current->stack_overflow_state();
  overflow_state->enable_stack_reserved_zone(/*check_if_disabled*/true);
  overflow_state->set_reserved_stack_activation(current->stack_base());
JRT_END

frame SharedRuntime::look_for_reserved_stack_annotated_method(JavaThread* current, frame fr) {
  ResourceMark rm(current);
  frame activation;
  nmethod* nm = nullptr;
  int count = 1;

  assert(fr.is_java_frame(), "Must start on Java frame");

  RegisterMap map(JavaThread::current(),
                  RegisterMap::UpdateMap::skip,
                  RegisterMap::ProcessFrames::skip,
                  RegisterMap::WalkContinuation::skip); // don't walk continuations
  for (; !fr.is_first_frame(); fr = fr.sender(&map)) {
    if (!fr.is_java_frame()) {
      continue;
    }

    Method* method = nullptr;
    bool found = false;
    if (fr.is_interpreted_frame()) {
      method = fr.interpreter_frame_method();
      if (method != nullptr && method->has_reserved_stack_access()) {
        found = true;
      }
    } else {
      CodeBlob* cb = fr.cb();
      if (cb != nullptr && cb->is_nmethod()) {
        nm = cb->as_nmethod();
        method = nm->method();
        // scope_desc_near() must be used, instead of scope_desc_at() because on
        // SPARC, the pcDesc can be on the delay slot after the call instruction.
        for (ScopeDesc *sd = nm->scope_desc_near(fr.pc()); sd != nullptr; sd = sd->sender()) {
          method = sd->method();
          if (method != nullptr && method->has_reserved_stack_access()) {
            found = true;
          }
        }
      }
    }
    if (found) {
      activation = fr;
      warning("Potentially dangerous stack overflow in "
              "ReservedStackAccess annotated method %s [%d]",
              method->name_and_sig_as_C_string(), count++);
      EventReservedStackActivation event;
      if (event.should_commit()) {
        event.set_method(method);
        event.commit();
      }
    }
  }
  return activation;
}

void SharedRuntime::on_slowpath_allocation_exit(JavaThread* current) {
  // After any safepoint, just before going back to compiled code,
  // we inform the GC that we will be doing initializing writes to
  // this object in the future without emitting card-marks, so
  // GC may take any compensating steps.

  oop new_obj = current->vm_result_oop();
  if (new_obj == nullptr) return;

  BarrierSet *bs = BarrierSet::barrier_set();
  bs->on_slowpath_allocation_exit(current, new_obj);
}

// We are at a compiled code to interpreter call. We need backing
// buffers for all inline type arguments. Allocate an object array to
// hold them (convenient because once we're done with it we don't have
// to worry about freeing it).
oop SharedRuntime::allocate_inline_types_impl(JavaThread* current, methodHandle callee, bool allocate_receiver, TRAPS) {
  assert(InlineTypePassFieldsAsArgs, "no reason to call this");
  ResourceMark rm;

  int nb_slots = 0;
  InstanceKlass* holder = callee->method_holder();
  allocate_receiver &= !callee->is_static() && holder->is_inline_klass() && callee->is_scalarized_arg(0);
  if (allocate_receiver) {
    nb_slots++;
  }
  int arg_num = callee->is_static() ? 0 : 1;
  for (SignatureStream ss(callee->signature()); !ss.at_return_type(); ss.next()) {
    BasicType bt = ss.type();
    if (bt == T_OBJECT && callee->is_scalarized_arg(arg_num)) {
      nb_slots++;
    }
    if (bt != T_VOID) {
      arg_num++;
    }
  }
  objArrayOop array_oop = oopFactory::new_objectArray(nb_slots, CHECK_NULL);
  objArrayHandle array(THREAD, array_oop);
  arg_num = callee->is_static() ? 0 : 1;
  int i = 0;
  if (allocate_receiver) {
    InlineKlass* vk = InlineKlass::cast(holder);
    oop res = vk->allocate_instance(CHECK_NULL);
    array->obj_at_put(i++, res);
  }
  for (SignatureStream ss(callee->signature()); !ss.at_return_type(); ss.next()) {
    BasicType bt = ss.type();
    if (bt == T_OBJECT && callee->is_scalarized_arg(arg_num)) {
      InlineKlass* vk = ss.as_inline_klass(holder);
      assert(vk != nullptr, "Unexpected klass");
      oop res = vk->allocate_instance(CHECK_NULL);
      array->obj_at_put(i++, res);
    }
    if (bt != T_VOID) {
      arg_num++;
    }
  }
  return array();
}

JRT_ENTRY(void, SharedRuntime::allocate_inline_types(JavaThread* current, Method* callee_method, bool allocate_receiver))
  methodHandle callee(current, callee_method);
  oop array = SharedRuntime::allocate_inline_types_impl(current, callee, allocate_receiver, CHECK);
  current->set_vm_result_oop(array);
  current->set_vm_result_metadata(callee()); // TODO: required to keep callee live?
JRT_END

// We're returning from an interpreted method: load each field into a
// register following the calling convention
JRT_LEAF(void, SharedRuntime::load_inline_type_fields_in_regs(JavaThread* current, oopDesc* res))
{
  assert(res->klass()->is_inline_klass(), "only inline types here");
  ResourceMark rm;
  RegisterMap reg_map(current,
                      RegisterMap::UpdateMap::include,
                      RegisterMap::ProcessFrames::include,
                      RegisterMap::WalkContinuation::skip);
  frame stubFrame = current->last_frame();
  frame callerFrame = stubFrame.sender(&reg_map);
  assert(callerFrame.is_interpreted_frame(), "should be coming from interpreter");

  InlineKlass* vk = InlineKlass::cast(res->klass());

  const Array<SigEntry>* sig_vk = vk->extended_sig();
  const Array<VMRegPair>* regs = vk->return_regs();

  if (regs == nullptr) {
    // The fields of the inline klass don't fit in registers, bail out
    return;
  }

  int j = 1;
  for (int i = 0; i < sig_vk->length(); i++) {
    BasicType bt = sig_vk->at(i)._bt;
    if (bt == T_METADATA) {
      continue;
    }
    if (bt == T_VOID) {
      if (sig_vk->at(i-1)._bt == T_LONG ||
          sig_vk->at(i-1)._bt == T_DOUBLE) {
        j++;
      }
      continue;
    }
    int off = sig_vk->at(i)._offset;
    assert(off > 0, "offset in object should be positive");
    VMRegPair pair = regs->at(j);
    address loc = reg_map.location(pair.first(), nullptr);
    switch(bt) {
    case T_BOOLEAN:
      *(jboolean*)loc = res->bool_field(off);
      break;
    case T_CHAR:
      *(jchar*)loc = res->char_field(off);
      break;
    case T_BYTE:
      *(jbyte*)loc = res->byte_field(off);
      break;
    case T_SHORT:
      *(jshort*)loc = res->short_field(off);
      break;
    case T_INT: {
      *(jint*)loc = res->int_field(off);
      break;
    }
    case T_LONG:
#ifdef _LP64
      *(intptr_t*)loc = res->long_field(off);
#else
      Unimplemented();
#endif
      break;
    case T_OBJECT:
    case T_ARRAY: {
      *(oop*)loc = res->obj_field(off);
      break;
    }
    case T_FLOAT:
      *(jfloat*)loc = res->float_field(off);
      break;
    case T_DOUBLE:
      *(jdouble*)loc = res->double_field(off);
      break;
    default:
      ShouldNotReachHere();
    }
    j++;
  }
  assert(j == regs->length(), "missed a field?");

#ifdef ASSERT
  VMRegPair pair = regs->at(0);
  address loc = reg_map.location(pair.first(), nullptr);
  assert(*(oopDesc**)loc == res, "overwritten object");
#endif

  current->set_vm_result_oop(res);
}
JRT_END

// We've returned to an interpreted method, the interpreter needs a
// reference to an inline type instance. Allocate it and initialize it
// from field's values in registers.
JRT_BLOCK_ENTRY(void, SharedRuntime::store_inline_type_fields_to_buf(JavaThread* current, intptr_t res))
{
  ResourceMark rm;
  RegisterMap reg_map(current,
                      RegisterMap::UpdateMap::include,
                      RegisterMap::ProcessFrames::include,
                      RegisterMap::WalkContinuation::skip);
  frame stubFrame = current->last_frame();
  frame callerFrame = stubFrame.sender(&reg_map);

#ifdef ASSERT
  InlineKlass* verif_vk = InlineKlass::returned_inline_klass(reg_map);
#endif

  if (!is_set_nth_bit(res, 0)) {
    // We're not returning with inline type fields in registers (the
    // calling convention didn't allow it for this inline klass)
    assert(!Metaspace::contains((void*)res), "should be oop or pointer in buffer area");
    current->set_vm_result_oop((oopDesc*)res);
    assert(verif_vk == nullptr, "broken calling convention");
    return;
  }

  clear_nth_bit(res, 0);
  InlineKlass* vk = (InlineKlass*)res;
  assert(verif_vk == vk, "broken calling convention");
  assert(Metaspace::contains((void*)res), "should be klass");

  // Allocate handles for every oop field so they are safe in case of
  // a safepoint when allocating
  GrowableArray<Handle> handles;
  vk->save_oop_fields(reg_map, handles);

  // It's unsafe to safepoint until we are here
  JRT_BLOCK;
  {
    JavaThread* THREAD = current;
    oop vt = vk->realloc_result(reg_map, handles, CHECK);
    current->set_vm_result_oop(vt);
  }
  JRT_BLOCK_END;
}
JRT_END
