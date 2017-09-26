/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/gcLocker.hpp"
#include "memory/vtBuffer.hpp"
#include "oops/oop.inline.hpp"
#include "oops/valueKlass.hpp"
#include "runtime/frame.hpp"
#include "runtime/thread.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ticks.hpp"
#include "utilities/ticks.inline.hpp"

VTBufferChunk* VTBuffer::_free_list = NULL;
Mutex* VTBuffer::_pool_lock = new Mutex(Mutex::leaf, "VTBuffer::_pool_lock", true, Monitor::_safepoint_check_never);
int VTBuffer::_pool_counter = 0;
int VTBuffer::_max_pool_counter = 0;
int VTBuffer::_total_allocated = 0;
int VTBuffer::_total_deallocated = 0;
int VTBuffer::_total_failed = 0;

oop VTBuffer::allocate_value(ValueKlass* k, TRAPS) {
  assert(THREAD->is_Java_thread(), "Only JavaThreads have a buffer for value types");
  JavaThread* thread = (JavaThread*)THREAD;
  if (thread->vt_alloc_ptr() == NULL) {
    if (!allocate_vt_chunk(thread)) {
      return NULL; // will trigger fall back strategy: allocation in Java heap
    }
  }
  assert(thread->vt_alloc_ptr() != NULL, "should not be null if chunk allocation was successful");
  int size_in_bytes = k->size_helper() * wordSize;
  if ((char*)thread->vt_alloc_ptr() + size_in_bytes  >= thread->vt_alloc_limit()) {
    if (size_in_bytes > (int)VTBufferChunk::max_alloc_size()) {
      // Too big to be allocated in a buffer
      return NULL;
    }
    if (!allocate_vt_chunk(thread)) {
      return NULL; // will trigger fall back strategy: allocation in Java heap
    }
  }
  assert((char*)thread->vt_alloc_ptr() + size_in_bytes < thread->vt_alloc_limit(),"otherwise the logic above is wrong");
  oop new_vt = (oop)thread->vt_alloc_ptr();
  int size_in_words = k->size_helper();
  thread->increment_vtchunk_total_memory_buffered(size_in_words * HeapWordSize);
  int increment = align_object_size(size_in_words);
  void* new_ptr = (char*)thread->vt_alloc_ptr() + increment * HeapWordSize;
  new_ptr = MIN2(new_ptr, thread->vt_alloc_limit());
  assert(VTBufferChunk::chunk(new_ptr) == VTBufferChunk::chunk(thread->vt_alloc_ptr()),
      "old and new alloc ptr must be in the same chunk");
  thread->set_vt_alloc_ptr(new_ptr);
  // the value and its header must be initialized before being returned!!!
  memset(((char*)(oopDesc*)new_vt), 0, size_in_bytes);
  new_vt->set_klass(k);
  new_vt->set_mark(markOop(k->java_mirror()));
  return new_vt;
}

bool VTBuffer::allocate_vt_chunk(JavaThread* thread) {
  VTBufferChunk* new_chunk = NULL;
  // Trying local cache;
  if (thread->local_free_chunk() != NULL) {
    new_chunk = thread->local_free_chunk();
    thread->set_local_free_chunk(NULL);
  } else {
    // Trying global pool
    MutexLockerEx ml(_pool_lock, Mutex::_no_safepoint_check_flag);
    if (_free_list != NULL) {
      new_chunk = _free_list;
      _free_list = new_chunk->next();
      if (_free_list != NULL) {
        _free_list->set_prev(NULL);
      }
      new_chunk->set_next(NULL);
      _pool_counter--;
    } else {
      // A new chunk has to be allocated
      // Hold _pool_lock to maintain counters
      if ((_total_allocated + 1) <= ValueTypesBufferMaxMemory) {
        // Allocate new chunk only if total size for buffer
        // memory is below its max size
        new_chunk = new VTBufferChunk(thread);
        _total_allocated += new_chunk == NULL ? 0 : 1;
      }
    }
  }
  if (new_chunk == NULL) {
    _total_failed++;
    thread->increment_vtchunk_failed();
    return false; // allocation failed
  }
  VTBufferChunk* current = thread->current_chunk();
  assert(new_chunk->owner() == thread || new_chunk->owner()== NULL, "Sanity check");
  assert(new_chunk->index() == -1, "Sanity check");
  new_chunk->set_owner(thread);
  if(current != NULL) {
    new_chunk->set_prev(current);
    new_chunk->set_index(current->index() + 1);
    current->set_next(new_chunk);
  } else {
    new_chunk->set_index(0);
  }
  thread->increment_vtchunk_in_use();
  thread->set_vt_alloc_ptr(new_chunk->first_alloc());
  thread->set_vt_alloc_limit(new_chunk->alloc_limit());
  return true; // allocation was successful
}

void VTBuffer::recycle_chunk(JavaThread* thread, VTBufferChunk* chunk) {
  if (thread->local_free_chunk() == NULL) {
    chunk->set_prev(NULL);
    chunk->set_next(NULL);
    chunk->set_index(-1);
    thread->set_local_free_chunk(chunk);
  } else {
    return_vt_chunk(thread, chunk);
  }
  thread->decrement_vtchunk_in_use();
}

// This is the main way to recycle VTBuffer memory, it is called from
// remove_activation() when an interpreter frame is about to be removed
// from the stack. All memory used in the context of this frame is freed,
// and the vt_alloc_ptr is restored to the value it had when the frame
// was created (modulo a possible adjustment if a value is being returned)
void VTBuffer::recycle_vtbuffer(JavaThread* thread, frame current_frame) {
  address current_ptr = (address)thread->vt_alloc_ptr();
  assert(current_ptr != NULL, "Should not reach here if NULL");
  VTBufferChunk* current_chunk = VTBufferChunk::chunk(current_ptr);
  assert(current_chunk->owner() == thread, "Sanity check");
  address previous_ptr = (address)current_frame.interpreter_frame_vt_alloc_ptr();
  if (previous_ptr == NULL) {
    // vt_alloc_ptr has not been initialized in this frame
    // let's initialize it to the first_alloc() value of the first chunk
    VTBufferChunk* first_chunk = current_chunk;
    while (first_chunk->prev() != NULL) {
      first_chunk = first_chunk->prev();
    }
    previous_ptr = (address)first_chunk->first_alloc();
  }
  assert(previous_ptr != NULL, "Should not reach here if NULL");
  VTBufferChunk* previous_chunk = VTBufferChunk::chunk(previous_ptr);
  assert(previous_chunk->owner() == thread, "Sanity check");
  if (current_ptr == previous_ptr) return;
  assert(current_chunk != previous_chunk || current_ptr >= previous_ptr, "Sanity check");
  VTBufferChunk* del = previous_chunk->next();
  previous_chunk->set_next(NULL);
  thread->set_vt_alloc_ptr(previous_ptr);
  thread->set_vt_alloc_limit(previous_chunk->alloc_limit());
  while (del != NULL) {
    VTBufferChunk* temp = del->next();
    VTBuffer::recycle_chunk(thread, del);
    del = temp;
  }
}

void VTBuffer::return_vt_chunk(JavaThread* thread, VTBufferChunk* chunk) {
  chunk->set_prev(NULL);
  chunk->set_owner(NULL);
  chunk->set_index(-1);
  MutexLockerEx ml(_pool_lock, Mutex::_no_safepoint_check_flag);
  if (_pool_counter < _max_free_list) {
    if (_free_list != NULL) {
      chunk->set_next(_free_list);
      _free_list->set_prev(chunk);
      _free_list = chunk;
    } else {
      chunk->set_next(NULL);
      _free_list = chunk;
    }
    _pool_counter++;
    if (_pool_counter > _max_pool_counter) {
      _max_pool_counter = _pool_counter;
    }
  } else {
    delete chunk;
    _total_deallocated++;
  }
  thread->increment_vtchunk_returned();
}

bool VTBuffer::value_belongs_to_frame(oop p, frame* f) {
  // the code below assumes that frame f is the last interpreted frame
  // on the execution stack
  int p_chunk_idx = VTBufferChunk::chunk(p)->index();
  int frame_first_chunk_idx;
  if (f->interpreter_frame_vt_alloc_ptr() != NULL) {
    frame_first_chunk_idx = VTBufferChunk::chunk(f->interpreter_frame_vt_alloc_ptr())->index();
  } else {
    frame_first_chunk_idx = 0;
  }
  if (p_chunk_idx == frame_first_chunk_idx) {
    return (intptr_t*)p >= f->interpreter_frame_vt_alloc_ptr();
  } else {
    return  p_chunk_idx > frame_first_chunk_idx;
  }

}

void VTBuffer::fix_frame_vt_alloc_ptr(frame f, VTBufferChunk* chunk) {
  assert(f.is_interpreted_frame(), "recycling can only be triggered from interpreted frames");
  assert(chunk != NULL, "Should not be called if null");
  while (chunk->prev() != NULL) {
    chunk = chunk->prev();
  }
  f.interpreter_frame_set_vt_alloc_ptr((intptr_t*)chunk->first_alloc());
}

extern "C" {
  static int compare_reloc_entries(const void* void_a, const void* void_b) {
    struct VT_relocation_entry* entry_a = (struct VT_relocation_entry*)void_a;
    struct VT_relocation_entry* entry_b = (struct VT_relocation_entry*)void_b;
    if (entry_a->chunk_index == entry_b->chunk_index) {
      if (entry_a->old_ptr < entry_b->old_ptr) {
        return -1;
      } else {
        return 1;
      }
    } else {
      if (entry_a->chunk_index < entry_b->chunk_index) {
        return -1;
      } else {
        return 1;
      }
    }
  }
}

void dump_reloc_table(struct VT_relocation_entry* table, int nelem, bool print_new_ptr) {
  ResourceMark rm;
  for (int i = 0; i < nelem; i++) {
          InstanceKlass* ik = InstanceKlass::cast(((oop)table[i].old_ptr)->klass());
    tty->print("%d:\t%p\t%d\t%s\t%x", i, table[i].old_ptr, table[i].chunk_index,
                ik->name()->as_C_string(), ik->size_helper() * HeapWordSize);
    if (print_new_ptr) {
        tty->print_cr("\t%p\t%d\n", table[i].new_ptr, VTBufferChunk::chunk(table[i].new_ptr)->index());
    } else {
        tty->print_cr("");
    }
  }
}

// Relocate value 'old' after value 'previous'
address VTBuffer::relocate_value(address old, address previous, int previous_size_in_words) {
  InstanceKlass* ik_old = InstanceKlass::cast(((oop)old)->klass());
  assert(ik_old->is_value(), "Sanity check");
  VTBufferChunk* chunk = VTBufferChunk::chunk(previous);
  address next_alloc = previous + align_object_size(ik_old->size_helper());
  if(next_alloc + ik_old->size_helper() * HeapWordSize < chunk->alloc_limit()) {
    // relocation can be performed in the same chunk
    return previous + align_object_size(previous_size_in_words) * HeapWordSize;
  } else {
    // relocation must be performed in the next chunk
    VTBufferChunk* next_chunk = chunk->next();
    assert(next_chunk != NULL, "Because we are compacting, there should be enough in use chunks");
    return (address)next_chunk->first_alloc();
  }
}

oop VTBuffer::relocate_return_value(JavaThread* thread, frame current_frame, oop obj) {
  assert(!Universe::heap()->is_in_reserved(obj), "This method should never be called on Java heap allocated values");
  assert(obj->klass()->is_value(), "Sanity check");
  if (!VTBuffer::value_belongs_to_frame(obj, &current_frame)) return obj;
  ValueKlass* vk = ValueKlass::cast(obj->klass());
  address current_ptr = (address)thread->vt_alloc_ptr();
  VTBufferChunk* current_chunk = VTBufferChunk::chunk(current_ptr);
  address previous_ptr = (address)current_frame.interpreter_frame_vt_alloc_ptr();
  if (previous_ptr == NULL) {
    fix_frame_vt_alloc_ptr(current_frame, current_chunk);
    previous_ptr = (address)current_frame.interpreter_frame_vt_alloc_ptr();
  }
  VTBufferChunk* previous_chunk = VTBufferChunk::chunk(previous_ptr);
  address dest;
  if ((address)obj != previous_ptr) {
    if (previous_chunk == current_chunk
        || (previous_ptr + vk->size_helper() * wordSize) < previous_chunk->alloc_limit()) {
      dest = previous_ptr;
    } else {
      assert(previous_chunk->next() != NULL, "Should not happen");
      dest = (address)previous_chunk->next()->first_alloc();
    }
    // Copying header
    memcpy(dest, obj, vk->first_field_offset());
    // Copying value content
    vk->value_store(((char*)(address)obj) + vk->first_field_offset(),
                    dest + vk->first_field_offset(), false, true);
  } else {
    dest = (address)obj;
  }
  address new_alloc_ptr = dest + vk->size_helper() * wordSize;
  current_frame.interpreter_frame_set_vt_alloc_ptr((intptr_t*)new_alloc_ptr);
  VTBufferChunk* last = VTBufferChunk::chunk(dest);
  VTBufferChunk* del = last->next();
  thread->set_vt_alloc_ptr(new_alloc_ptr);
  thread->set_vt_alloc_limit(last->alloc_limit());
  last->set_next(NULL);
  while (del != NULL) {
    VTBufferChunk* tmp = del->next();
    VTBuffer::recycle_chunk(thread, del);
    del = tmp;
  }
  return (oop)dest;
}

// This method is called to recycle VTBuffer memory when the VM has detected
// that too much memory is being consumed in the current frame context. This
// can only happen when the method contains at least one loop in which new
// values are created.
void VTBuffer::recycle_vt_in_frame(JavaThread* thread, frame* f) {
  Ticks begin, end;
  Ticks step1, step2, step3, step4, step5, step6, step7;
  int returned_chunks = 0;

  if (ReportVTBufferRecyclingTimes) {
    begin = Ticks::now();
  }
  assert(f->is_interpreted_frame(), "only interpreted frames are using VT buffering so far");
  ResourceMark rm(thread);

  // 1 - allocate relocation table
  Method* m = f->interpreter_frame_method();
  int max_entries = m->max_locals() + m->max_stack();
  VT_relocation_entry* reloc_table = NEW_RESOURCE_ARRAY_IN_THREAD(thread, struct VT_relocation_entry, max_entries);
  int n_entries = 0;
  if (ReportVTBufferRecyclingTimes) {
    step1 = Ticks::now();
  }

  {
    // No GC should occur during the phases 2->5
    // either because the mark word (usually containing the pointer
    // to the Java mirror) is used for marking, or because the values are being relocated
    NoSafepointVerifier nsv;

    // 2 - marking phase + populate relocation table
    BufferedValuesMarking marking_closure = BufferedValuesMarking(f, reloc_table, max_entries, &n_entries);
    f->buffered_values_interpreted_do(&marking_closure);
    if (ReportVTBufferRecyclingTimes) {
      step2 = Ticks::now();
    }

    if (n_entries > 0) {
      // 3 - sort relocation table entries and compute compaction
      qsort(reloc_table, n_entries, sizeof(struct VT_relocation_entry), compare_reloc_entries);
      if (f->interpreter_frame_vt_alloc_ptr() == NULL) {
        VTBufferChunk* chunk = VTBufferChunk::chunk(reloc_table[0].old_ptr);
        while (chunk->prev() != NULL) chunk = chunk->prev();
        //f->interpreter_frame_set_vt_alloc_ptr((intptr_t*)chunk->first_alloc());
        reloc_table[0].new_ptr = (address)chunk->first_alloc();
      } else {
        reloc_table[0].new_ptr = (address)f->interpreter_frame_vt_alloc_ptr();
      }
      ((oop)reloc_table[0].old_ptr)->set_mark((markOop)reloc_table[0].new_ptr);
      for (int i = 1; i < n_entries; i++) {
        reloc_table[i].new_ptr = relocate_value(reloc_table[i].old_ptr, reloc_table[i-1].new_ptr,
            InstanceKlass::cast(((oop)reloc_table[i-1].old_ptr)->klass())->size_helper());
        ((oop)reloc_table[i].old_ptr)->set_mark((markOop)reloc_table[i].new_ptr);
      }
      if (ReportVTBufferRecyclingTimes) {
        step3 = Ticks::now();
      }

      // 4 - update pointers
      BufferedValuesPointersUpdate update_closure = BufferedValuesPointersUpdate(f);
      f->buffered_values_interpreted_do(&update_closure);
      if (ReportVTBufferRecyclingTimes) {
        step4 = Ticks::now();
      }

      // 5 - relocate values
      for (int i = 0; i < n_entries; i++) {
        if (reloc_table[i].old_ptr != reloc_table[i].new_ptr) {
          InstanceKlass* ik_old = InstanceKlass::cast(((oop)reloc_table[i].old_ptr)->klass());
          // instead of memcpy, a value_store() might be required here
          memcpy(reloc_table[i].new_ptr, reloc_table[i].old_ptr, ik_old->size_helper() * HeapWordSize);
        }
        // Resetting the mark word
        ((oop)reloc_table[i].new_ptr)->set_mark(markOop(((oop)reloc_table[i].new_ptr)->klass()->java_mirror()));
      }
      if (ReportVTBufferRecyclingTimes) {
        step5 = Ticks::now();
      }

      // 6 - update thread allocation pointer
      oop last_oop = (oop)reloc_table[n_entries - 1].new_ptr;
      InstanceKlass* ik = InstanceKlass::cast(last_oop->klass());
      thread->set_vt_alloc_ptr((address)last_oop + ik->size_helper() * HeapWordSize);
      thread->set_vt_alloc_limit(VTBufferChunk::chunk(thread->vt_alloc_ptr())->alloc_limit());
      if (ReportVTBufferRecyclingTimes) {
        step6 = Ticks::now();
      }

      // 7 - free/return unused chunks
      VTBufferChunk* chunk = VTBufferChunk::chunk(reloc_table[n_entries - 1].new_ptr);
      VTBufferChunk* temp = chunk;
      chunk = chunk->next();
      temp->set_next(NULL);
      while (chunk != NULL) {
        returned_chunks++;
        temp = chunk->next();
        VTBuffer::recycle_chunk(thread, chunk);
        chunk = temp;
      }
      if (ReportVTBufferRecyclingTimes) {
        step7 = Ticks::now();
      }
    } else {
      f->interpreter_frame_set_vt_alloc_ptr((intptr_t*)thread->vt_alloc_ptr());
    }
  }

  // 8 - free relocation table
  FREE_RESOURCE_ARRAY(struct VT_relocation_entry, reloc_table, max_entries);
  if (ReportVTBufferRecyclingTimes) {
    end = Ticks::now();
    ResourceMark rm(thread);
    tty->print_cr("VTBufferRecyling: %s : %s.%s %s : " JLONG_FORMAT "us",
        thread->name(),
        f->interpreter_frame_method()->klass_name()->as_C_string(),
        f->interpreter_frame_method()->name()->as_C_string(),
        f->interpreter_frame_method()->signature()->as_C_string(),
        (end.value() - begin.value()) / 1000);
    tty->print("Step1 : " JLONG_FORMAT "ns ", step1.value() - begin.value());
    tty->print("Step2 : " JLONG_FORMAT "ns ", step2.value() - step1.value());
    tty->print("Step3 : " JLONG_FORMAT "ns ", step3.value() - step2.value());
    tty->print("Step4 : " JLONG_FORMAT "ns ", step4.value() - step3.value());
    tty->print("Step5 : " JLONG_FORMAT "ns ", step5.value() - step4.value());
    tty->print("Step6 : " JLONG_FORMAT "ns ", step6.value() - step5.value());
    tty->print("Step7 : " JLONG_FORMAT "ns ", step7.value() - step6.value());
    tty->print("Step8 : " JLONG_FORMAT "ns ", end.value() - step7.value());
    tty->print_cr("Returned chunks: %d", returned_chunks);
  }
}

void BufferedValuesMarking::do_buffered_value(oop* p) {
  assert(!Universe::heap()->is_in_reserved_or_null(*p), "Sanity check");
  if (VTBuffer::value_belongs_to_frame(*p, _frame)) {
    if (!(*p)->mark()->is_marked()) {
      assert(*_index < _size, "index outside of relocation table range");
      _reloc_table[*_index].old_ptr = (address)*p;
      _reloc_table[*_index].chunk_index = VTBufferChunk::chunk(*p)->index();
      *_index = (*_index) + 1;
      (*p)->set_mark((*p)->mark()->set_marked());
    }
  }
}

void BufferedValuesPointersUpdate::do_buffered_value(oop* p) {
  assert(!Universe::heap()->is_in_reserved_or_null(*p), "Sanity check");
  // might be coded more efficiently just by checking mark word is not NULL
  if (VTBuffer::value_belongs_to_frame(*p, _frame)) {
    *p = (oop)(*p)->mark();
  }
}
