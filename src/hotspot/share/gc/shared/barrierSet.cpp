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

#include "classfile/vmSymbols.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "gc/shared/barrierSetNMethod.hpp"
#include "gc/shared/barrierSetStackChunk.hpp"
#include "memory/resourceArea.hpp"
#include "oops/objArrayKlass.inline.hpp"
#include "runtime/continuation.hpp"
#include "runtime/javaThread.hpp"
#include "utilities/debug.hpp"
#include "utilities/macros.hpp"

BarrierSet* BarrierSet::_barrier_set = nullptr;

void BarrierSet::set_barrier_set(BarrierSet* barrier_set) {
  assert(_barrier_set == nullptr, "Already initialized");
  _barrier_set = barrier_set;

  // Notify barrier set of the current (main) thread.  Normally the
  // Thread constructor deals with this, but the main thread is
  // created before we get here.  Verify it isn't yet on the thread
  // list, else we'd also need to call BarrierSet::on_thread_attach.
  // This is the only thread that can exist at this point; the Thread
  // constructor objects to other threads being created before the
  // barrier set is available.
  assert(Thread::current()->is_Java_thread(),
         "Expected main thread to be a JavaThread");
  assert(!JavaThread::current()->on_thread_list(),
         "Main thread already on thread list.");
  _barrier_set->on_thread_create(Thread::current());
}

void BarrierSet::throw_array_null_pointer_store_exception(arrayOop src, arrayOop dst, TRAPS) {
  ResourceMark rm(THREAD);
  Klass* bound = ObjArrayKlass::cast(dst->klass())->element_klass();
  stringStream ss;
  ss.print("arraycopy: can not copy null values into %s[]",
           bound->external_name());
  THROW_MSG(vmSymbols::java_lang_NullPointerException(), ss.as_string());
}

void BarrierSet::throw_array_store_exception(arrayOop src, arrayOop dst, TRAPS) {
  ResourceMark rm(THREAD);
  Klass* bound = ObjArrayKlass::cast(dst->klass())->element_klass();
  Klass* stype = ObjArrayKlass::cast(src->klass())->element_klass();
  stringStream ss;
  if (!bound->is_subtype_of(stype)) {
    ss.print("arraycopy: type mismatch: can not copy %s[] into %s[]",
             stype->external_name(), bound->external_name());
  } else {
    // oop_arraycopy should return the index in the source array that
    // contains the problematic oop.
    ss.print("arraycopy: element type mismatch: can not cast one of the elements"
             " of %s[] to the type of the destination array, %s",
             stype->external_name(), bound->external_name());
  }
  THROW_MSG(vmSymbols::java_lang_ArrayStoreException(), ss.as_string());
}

static BarrierSetNMethod* select_barrier_set_nmethod(BarrierSetNMethod* barrier_set_nmethod) {
  if (barrier_set_nmethod != nullptr) {
    // The GC needs nmethod entry barriers to do concurrent GC
    return barrier_set_nmethod;
  } else {
    // The GC needs nmethod entry barriers to deal with continuations
    // and code cache unloading
    return new BarrierSetNMethod();
  }
}

static BarrierSetStackChunk* select_barrier_set_stack_chunk(BarrierSetStackChunk* barrier_set_stack_chunk) {
  if (barrier_set_stack_chunk != nullptr) {
    return barrier_set_stack_chunk;
  } else {
    return new BarrierSetStackChunk();
  }
}

BarrierSet::BarrierSet(BarrierSetAssembler* barrier_set_assembler,
                       BarrierSetC1* barrier_set_c1,
                       BarrierSetC2* barrier_set_c2,
                       BarrierSetNMethod* barrier_set_nmethod,
                       BarrierSetStackChunk* barrier_set_stack_chunk,
                       const FakeRtti& fake_rtti) :
    _fake_rtti(fake_rtti),
    _barrier_set_assembler(barrier_set_assembler),
    _barrier_set_c1(barrier_set_c1),
    _barrier_set_c2(barrier_set_c2),
    _barrier_set_nmethod(select_barrier_set_nmethod(barrier_set_nmethod)),
    _barrier_set_stack_chunk(select_barrier_set_stack_chunk(barrier_set_stack_chunk)) {
}

void BarrierSet::on_thread_attach(Thread* thread) {
  BarrierSetNMethod* bs_nm = barrier_set_nmethod();
  thread->set_nmethod_disarmed_guard_value(bs_nm->disarmed_guard_value());
}

// Called from init.cpp
void gc_barrier_stubs_init() {
  BarrierSet* bs = BarrierSet::barrier_set();
#ifndef ZERO
  BarrierSetAssembler* bs_assembler = bs->barrier_set_assembler();
  bs_assembler->barrier_stubs_init();
#endif
}
