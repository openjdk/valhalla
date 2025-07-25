/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "classfile/javaClasses.hpp"
#include "classfile/vmClasses.hpp"
#include "gc/shared/allocTracer.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/memAllocator.hpp"
#include "gc/shared/threadLocalAllocBuffer.inline.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "memory/universe.hpp"
#include "oops/arrayOop.hpp"
#include "oops/oop.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/continuationJavaClasses.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/javaThread.hpp"
#include "runtime/sharedRuntime.hpp"
#include "services/lowMemoryDetector.hpp"
#include "utilities/align.hpp"
#include "utilities/copy.hpp"
#include "utilities/globalDefinitions.hpp"

class MemAllocator::Allocation: StackObj {
  friend class MemAllocator;

  const MemAllocator& _allocator;
  JavaThread*         _thread;
  oop*                _obj_ptr;
  bool                _overhead_limit_exceeded;
  bool                _allocated_outside_tlab;
  size_t              _allocated_tlab_size;
  bool                _tlab_end_reset_for_sample;

  bool check_out_of_memory();
  void verify_before();
  void verify_after();
  void notify_allocation();
  void notify_allocation_jvmti_sampler();
  void notify_allocation_low_memory_detector();
  void notify_allocation_jfr_sampler();
  void notify_allocation_dtrace_sampler();
#ifdef ASSERT
  void check_for_valid_allocation_state() const;
#endif

  class PreserveObj;

public:
  Allocation(const MemAllocator& allocator, oop* obj_ptr)
    : _allocator(allocator),
      _thread(JavaThread::cast(allocator._thread)), // Do not use Allocation in non-JavaThreads.
      _obj_ptr(obj_ptr),
      _overhead_limit_exceeded(false),
      _allocated_outside_tlab(false),
      _allocated_tlab_size(0),
      _tlab_end_reset_for_sample(false)
  {
    assert(Thread::current() == allocator._thread, "do not pass MemAllocator across threads");
    verify_before();
  }

  ~Allocation() {
    if (!check_out_of_memory()) {
      notify_allocation();
    }
  }

  oop obj() const { return *_obj_ptr; }
};

class MemAllocator::Allocation::PreserveObj: StackObj {
  HandleMark _handle_mark;
  Handle     _handle;
  oop* const _obj_ptr;

public:
  PreserveObj(JavaThread* thread, oop* obj_ptr)
    : _handle_mark(thread),
      _handle(thread, *obj_ptr),
      _obj_ptr(obj_ptr)
  {
    *obj_ptr = nullptr;
  }

  ~PreserveObj() {
    *_obj_ptr = _handle();
  }

  oop operator()() const {
    return _handle();
  }
};

bool MemAllocator::Allocation::check_out_of_memory() {
  JavaThread* THREAD = _thread; // For exception macros.
  assert(!HAS_PENDING_EXCEPTION, "Unexpected exception, will result in uninitialized storage");

  if (obj() != nullptr) {
    return false;
  }

  const char* message = _overhead_limit_exceeded ? "GC overhead limit exceeded" : "Java heap space";
  if (!_thread->is_in_internal_oome_mark()) {
    // -XX:+HeapDumpOnOutOfMemoryError and -XX:OnOutOfMemoryError support
    report_java_out_of_memory(message);
    if (JvmtiExport::should_post_resource_exhausted()) {
#ifdef CHECK_UNHANDLED_OOPS
      // obj is null, no need to handle, but CheckUnhandledOops is not aware about null
      THREAD->allow_unhandled_oop(_obj_ptr);
#endif // CHECK_UNHANDLED_OOPS
      JvmtiExport::post_resource_exhausted(
        JVMTI_RESOURCE_EXHAUSTED_OOM_ERROR | JVMTI_RESOURCE_EXHAUSTED_JAVA_HEAP,
        message);
    }

    oop exception = _overhead_limit_exceeded ?
        Universe::out_of_memory_error_gc_overhead_limit() :
        Universe::out_of_memory_error_java_heap();
    THROW_OOP_(exception, true);
  } else {
    THROW_OOP_(Universe::out_of_memory_error_java_heap_without_backtrace(), true);
  }
}

void MemAllocator::Allocation::verify_before() {
  // Clear unhandled oops for memory allocation.  Memory allocation might
  // not take out a lock if from tlab, so clear here.
  JavaThread* THREAD = _thread; // For exception macros.
  assert(!HAS_PENDING_EXCEPTION, "Should not allocate with exception pending");
  DEBUG_ONLY(check_for_valid_allocation_state());
  assert(!Universe::heap()->is_stw_gc_active(), "Allocation during GC pause not allowed");
}

#ifdef ASSERT
void MemAllocator::Allocation::check_for_valid_allocation_state() const {
  // How to choose between a pending exception and a potential
  // OutOfMemoryError?  Don't allow pending exceptions.
  // This is a VM policy failure, so how do we exhaustively test it?
  assert(!_thread->has_pending_exception(),
         "shouldn't be allocating with pending exception");
  // Allocation of an oop can always invoke a safepoint.
  _thread->check_for_valid_safepoint_state();
}
#endif

void MemAllocator::Allocation::notify_allocation_jvmti_sampler() {
  // support for JVMTI VMObjectAlloc event (no-op if not enabled)
  JvmtiExport::vm_object_alloc_event_collector(obj());

  if (!JvmtiExport::should_post_sampled_object_alloc()) {
    // Sampling disabled
    return;
  }

  if (!_allocated_outside_tlab && _allocated_tlab_size == 0 && !_tlab_end_reset_for_sample) {
    // Sample if it's a non-TLAB allocation, or a TLAB allocation that either refills the TLAB
    // or expands it due to taking a sampler induced slow path.
    return;
  }

  // If we want to be sampling, protect the allocated object with a Handle
  // before doing the callback. The callback is done in the destructor of
  // the JvmtiSampledObjectAllocEventCollector.
  size_t bytes_since_last = 0;

  {
    PreserveObj obj_h(_thread, _obj_ptr);
    JvmtiSampledObjectAllocEventCollector collector;
    size_t size_in_bytes = _allocator._word_size * HeapWordSize;
    ThreadLocalAllocBuffer& tlab = _thread->tlab();

    if (!_allocated_outside_tlab) {
      bytes_since_last = tlab.bytes_since_last_sample_point();
    }

    _thread->heap_sampler().check_for_sampling(obj_h(), size_in_bytes, bytes_since_last);
  }

  if (_tlab_end_reset_for_sample || _allocated_tlab_size != 0) {
    // Tell tlab to forget bytes_since_last if we passed it to the heap sampler.
    _thread->tlab().set_sample_end(bytes_since_last != 0);
  }
}

void MemAllocator::Allocation::notify_allocation_low_memory_detector() {
  // support low memory notifications (no-op if not enabled)
  LowMemoryDetector::detect_low_memory_for_collected_pools();
}

void MemAllocator::Allocation::notify_allocation_jfr_sampler() {
  HeapWord* mem = cast_from_oop<HeapWord*>(obj());
  size_t size_in_bytes = _allocator._word_size * HeapWordSize;

  if (_allocated_outside_tlab) {
    AllocTracer::send_allocation_outside_tlab(obj()->klass(), mem, size_in_bytes, _thread);
  } else if (_allocated_tlab_size != 0) {
    // TLAB was refilled
    AllocTracer::send_allocation_in_new_tlab(obj()->klass(), mem, _allocated_tlab_size * HeapWordSize,
                                             size_in_bytes, _thread);
  }
}

void MemAllocator::Allocation::notify_allocation_dtrace_sampler() {
  if (DTraceAllocProbes) {
    // support for Dtrace object alloc event (no-op most of the time)
    Klass* klass = obj()->klass();
    size_t word_size = _allocator._word_size;
    if (klass != nullptr && klass->name() != nullptr) {
      SharedRuntime::dtrace_object_alloc(_thread, obj(), word_size);
    }
  }
}

void MemAllocator::Allocation::notify_allocation() {
  notify_allocation_low_memory_detector();
  notify_allocation_jfr_sampler();
  notify_allocation_dtrace_sampler();
  notify_allocation_jvmti_sampler();
}

HeapWord* MemAllocator::mem_allocate_outside_tlab(Allocation& allocation) const {
  allocation._allocated_outside_tlab = true;
  HeapWord* mem = Universe::heap()->mem_allocate(_word_size, &allocation._overhead_limit_exceeded);
  if (mem == nullptr) {
    return mem;
  }

  size_t size_in_bytes = _word_size * HeapWordSize;
  _thread->incr_allocated_bytes(size_in_bytes);

  return mem;
}

HeapWord* MemAllocator::mem_allocate_inside_tlab_fast() const {
  return _thread->tlab().allocate(_word_size);
}

HeapWord* MemAllocator::mem_allocate_inside_tlab_slow(Allocation& allocation) const {
  HeapWord* mem = nullptr;
  ThreadLocalAllocBuffer& tlab = _thread->tlab();

  if (JvmtiExport::should_post_sampled_object_alloc()) {
    tlab.set_back_allocation_end();
    mem = tlab.allocate(_word_size);

    // We set back the allocation sample point to try to allocate this, reset it
    // when done.
    allocation._tlab_end_reset_for_sample = true;

    if (mem != nullptr) {
      return mem;
    }
  }

  // Retain tlab and allocate object in shared space if
  // the amount free in the tlab is too large to discard.
  if (tlab.free() > tlab.refill_waste_limit()) {
    tlab.record_slow_allocation(_word_size);
    return nullptr;
  }

  // Discard tlab and allocate a new one.
  // To minimize fragmentation, the last TLAB may be smaller than the rest.
  size_t new_tlab_size = tlab.compute_size(_word_size);

  tlab.retire_before_allocation();

  if (new_tlab_size == 0) {
    return nullptr;
  }

  // Allocate a new TLAB requesting new_tlab_size. Any size
  // between minimal and new_tlab_size is accepted.
  size_t min_tlab_size = ThreadLocalAllocBuffer::compute_min_size(_word_size);
  mem = Universe::heap()->allocate_new_tlab(min_tlab_size, new_tlab_size, &allocation._allocated_tlab_size);
  if (mem == nullptr) {
    assert(allocation._allocated_tlab_size == 0,
           "Allocation failed, but actual size was updated. min: %zu"
           ", desired: %zu, actual: %zu",
           min_tlab_size, new_tlab_size, allocation._allocated_tlab_size);
    return nullptr;
  }
  assert(allocation._allocated_tlab_size != 0, "Allocation succeeded but actual size not updated. mem at: "
         PTR_FORMAT " min: %zu, desired: %zu",
         p2i(mem), min_tlab_size, new_tlab_size);

  // ...and clear or zap just allocated TLAB, if needed.
  if (ZeroTLAB) {
    Copy::zero_to_words(mem, allocation._allocated_tlab_size);
  } else if (ZapTLAB) {
    // Skip mangling the space corresponding to the object header to
    // ensure that the returned space is not considered parsable by
    // any concurrent GC thread.
    size_t hdr_size = oopDesc::header_size();
    Copy::fill_to_words(mem + hdr_size, allocation._allocated_tlab_size - hdr_size, badHeapWordVal);
  }

  tlab.fill(mem, mem + _word_size, allocation._allocated_tlab_size);
  return mem;
}

HeapWord* MemAllocator::mem_allocate(Allocation& allocation) const {
  if (UseTLAB) {
    // Try allocating from an existing TLAB.
    HeapWord* mem = mem_allocate_inside_tlab_fast();
    if (mem != nullptr) {
      return mem;
    }
  }

  // Allocation of an oop can always invoke a safepoint.
  DEBUG_ONLY(allocation._thread->check_for_valid_safepoint_state());

  if (UseTLAB) {
    // Try refilling the TLAB and allocating the object in it.
    HeapWord* mem = mem_allocate_inside_tlab_slow(allocation);
    if (mem != nullptr) {
      return mem;
    }
  }

  return mem_allocate_outside_tlab(allocation);
}

oop MemAllocator::allocate() const {
  oop obj = nullptr;
  {
    Allocation allocation(*this, &obj);
    HeapWord* mem = mem_allocate(allocation);
    if (mem != nullptr) {
      obj = initialize(mem);
    } else {
      // The unhandled oop detector will poison local variable obj,
      // so reset it to null if mem is null.
      obj = nullptr;
    }
  }
  return obj;
}

void MemAllocator::mem_clear(HeapWord* mem) const {
  assert(mem != nullptr, "cannot initialize null object");
  const size_t hs = oopDesc::header_size();
  assert(_word_size >= hs, "unexpected object size");
  if (oopDesc::has_klass_gap()) {
    oopDesc::set_klass_gap(mem, 0);
  }
  Copy::fill_to_aligned_words(mem + hs, _word_size - hs);
}

oop MemAllocator::finish(HeapWord* mem) const {
  assert(mem != nullptr, "null object pointer");
  // Need a release store to ensure array/class length, mark word, and
  // object zeroing are visible before setting the klass non-null, for
  // concurrent collectors.
  if (UseCompactObjectHeaders) {
    oopDesc::release_set_mark(mem, Klass::default_prototype_header(_klass));
  } else {
    if (EnableValhalla) {
      oopDesc::set_mark(mem, Klass::default_prototype_header(_klass));
    } else {
      oopDesc::set_mark(mem, markWord::prototype());
    }
    oopDesc::release_set_klass(mem, _klass);
  }
  return cast_to_oop(mem);
}

oop ObjAllocator::initialize(HeapWord* mem) const {
  mem_clear(mem);
  return finish(mem);
}

oop ObjBufferAllocator::initialize(HeapWord* mem) const {
  mem_clear(mem);
  return finish(mem);
}


oop ObjArrayAllocator::initialize(HeapWord* mem) const {
  // Set array length before setting the _klass field because a
  // non-null klass field indicates that the object is parsable by
  // concurrent GC.
  assert(_length >= 0, "length should be non-negative");
  if (_do_zero) {
    mem_clear(mem);
    mem_zap_start_padding(mem);
    mem_zap_end_padding(mem);
  }
  arrayOopDesc::set_length(mem, _length);
  return finish(mem);
}

#ifndef PRODUCT
void ObjArrayAllocator::mem_zap_start_padding(HeapWord* mem) const {
  const BasicType element_type = ArrayKlass::cast(_klass)->element_type();
  const size_t base_offset_in_bytes = arrayOopDesc::base_offset_in_bytes(element_type);
  const size_t header_size_in_bytes = arrayOopDesc::header_size_in_bytes();

  const address base = reinterpret_cast<address>(mem) + base_offset_in_bytes;
  const address header_end = reinterpret_cast<address>(mem) + header_size_in_bytes;

  if (header_end < base) {
    const size_t padding_in_bytes = base - header_end;
    Copy::fill_to_bytes(header_end, padding_in_bytes, heapPaddingByteVal);
  }
}

void ObjArrayAllocator::mem_zap_end_padding(HeapWord* mem) const {
  const size_t length_in_bytes = static_cast<size_t>(_length) << ArrayKlass::cast(_klass)->log2_element_size();
  const BasicType element_type = ArrayKlass::cast(_klass)->element_type();
  const size_t base_offset_in_bytes = arrayOopDesc::base_offset_in_bytes(element_type);
  const size_t size_in_bytes = _word_size * BytesPerWord;

  const address obj_end = reinterpret_cast<address>(mem) + size_in_bytes;
  const address base = reinterpret_cast<address>(mem) + base_offset_in_bytes;
  const address elements_end = base + length_in_bytes;
  assert(elements_end <= obj_end, "payload must fit in object");
  if (elements_end < obj_end) {
    const size_t padding_in_bytes = obj_end - elements_end;
    Copy::fill_to_bytes(elements_end, padding_in_bytes, heapPaddingByteVal);
  }
}
#endif

oop ClassAllocator::initialize(HeapWord* mem) const {
  // Set oop_size field before setting the _klass field because a
  // non-null _klass field indicates that the object is parsable by
  // concurrent GC.
  assert(_word_size > 0, "oop_size must be positive.");
  mem_clear(mem);
  java_lang_Class::set_oop_size(mem, _word_size);
  return finish(mem);
}
