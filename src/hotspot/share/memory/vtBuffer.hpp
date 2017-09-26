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

#ifndef SHARE_VM_MEMORY_VTBUFFER_HPP
#define SHARE_VM_MEMORY_VTBUFFER_HPP

#include "memory/allocation.hpp"
#include "runtime/globals.hpp"
#include "runtime/os.hpp"
#include "utilities/globalDefinitions.hpp"

class VTBufferChunk : public CMmapObj<VTBufferChunk, mtValueTypes> {
  friend class VMStructs;

  static const int MAGIC_NUMBER = 3141592;

private:
  int            _magic;
  int            _index;
  VTBufferChunk* _prev;
  VTBufferChunk* _next;
  JavaThread*    _owner;
 public:

  /* A VTBufferChunk is a 4KB page used to create a thread-local
   * buffer to store values. They are allocated in a global pool,
   * and then threads can get them to create their own buffer.
   * Each thread creates a linked list of VTBufferChunk to build
   * its buffer. Fields _prev and _next are used to link the
   * chunks together, the _owner field indicates to which thread
   * this chunk belongs to (if NULL, it means the chunk has been
   * returned to the global pool). When creating the linked list,
   * the field _index is used to store the position of the chunk
   * in the list. The index is used to optimize the comparison
   * of addresses of buffered values. Because the thread local
   * buffer is made of non-contiguous chunks, it is not possible
   * to directly compare the two addresses. The comparison requires
   * first to compare the indexes of each address' chunk, and if
   * they are equal, compare the addresses directly. Without
   * the _index field, this operation would require to walk the
   * linked list for each comparison.
   */

  VTBufferChunk(JavaThread* thread) {
    _magic = MAGIC_NUMBER;
    _index = -1;
    _prev = NULL;
    _next = NULL;
    _owner = thread;
  }

  int            index() { return _index; }
  void           set_index(int index) { _index = index; }
  VTBufferChunk* prev() { return _prev; }
  void           set_prev(VTBufferChunk* prev) { _prev = prev; }
  VTBufferChunk* next() { return _next; }
  void           set_next(VTBufferChunk* next) { _next = next; }
  JavaThread*    owner() { return _owner; }
  void           set_owner(JavaThread* thread) {
    assert(thread == NULL || _owner == NULL || _owner == thread, "Sanity check");
    _owner = thread;
  }

  bool is_valid() {
    return _magic == MAGIC_NUMBER && _owner != NULL && _index != -1;
  }

  void* first_alloc() { return (void*)((char*)this + align_object_size(sizeof (VTBufferChunk))); }
  void* alloc_limit() { return (void*)((char*)this + chunk_size() - 1); }

  static int chunk_size() {
    return os::vm_page_size();
  }

  static uintptr_t chunk_mask() {
    return ~(chunk_size() - 1);
  }

  static ByteSize index_offset() { return byte_offset_of(VTBufferChunk, _index); }

  static size_t max_alloc_size() {
    return chunk_size() - align_object_size(sizeof (VTBufferChunk));
  }

  static VTBufferChunk* chunk(void* address) {
    VTBufferChunk* c = (VTBufferChunk*)((intptr_t)address & chunk_mask());
    assert(c->is_valid(), "Sanity check");
    return c;
  }

  static bool check_buffered(void* address) {
    assert(address != NULL, "Sanity check");
    VTBufferChunk* c = (VTBufferChunk*)((intptr_t)address & chunk_mask());
    return c->is_valid();
  }

  bool contains(void* address) {
    return address > (char*)chunk(address) && address < ((char*)chunk(address) + chunk_size());
  }
};

class VTBuffer : AllStatic {
  friend class VMStructs;
private:
  static VTBufferChunk* _free_list;
  static Mutex* _pool_lock;
  static int _pool_counter;
  static int _max_pool_counter;
  static int _total_allocated;
  static int _total_deallocated;
  static int _total_failed;
  static const int _max_free_list = 64;  // Should be tunable

public:
  static Mutex* lock() { return _pool_lock; }
  static oop allocate_value(ValueKlass* k, TRAPS);
  static bool allocate_vt_chunk(JavaThread* thread);
  static void recycle_chunk(JavaThread* thread, VTBufferChunk* chunk);
  static void return_vt_chunk(JavaThread* thread, VTBufferChunk* chunk);

  static int in_pool() { return _pool_counter; }
  static int max_in_pool() { return _max_pool_counter; }
  static int total_allocated() { return _total_allocated; }
  static int total_deallocated() { return _total_deallocated; }
  static int total_failed() { return _total_failed; }

  static bool is_in_vt_buffer(const void* p) {
    intptr_t chunk_mask = (~(VTBufferChunk::chunk_size() - 1));
    VTBufferChunk* c = (VTBufferChunk*)((intptr_t)p & chunk_mask);
    return c->is_valid();
  }

  static bool value_belongs_to_frame(oop p, frame *f);
  static void recycle_vt_in_frame(JavaThread* thread, frame* f);
  static void recycle_vtbuffer(JavaThread *thread, frame f);
  static address relocate_value(address old, address previous, int previous_size_in_words);
  static oop relocate_return_value(JavaThread* thread, frame fr, oop old);

  static void fix_frame_vt_alloc_ptr(frame fr, VTBufferChunk* chunk);

};

struct VT_relocation_entry {
  int chunk_index;
  address old_ptr;
  address new_ptr;
};


class BufferedValuesMarking : public BufferedValueClosure {
  frame* _frame;
  struct VT_relocation_entry* _reloc_table;
  int _size;
  int* _index;
public:
  BufferedValuesMarking(frame* frame, struct VT_relocation_entry* reloc_table, int size, int* index) {
    _frame = frame;
    _reloc_table = reloc_table;
    _size = size;
    _index = index;
  }
  virtual void do_buffered_value(oop* p);
};

class BufferedValuesPointersUpdate : public BufferedValueClosure {
  frame* _frame;
public:
  BufferedValuesPointersUpdate(frame* frame) {
    _frame = frame;
  }
  virtual void do_buffered_value(oop* p);
};

#endif /* SHARE_VM_MEMORY_VTBUFFER_HPP */
