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
#include "memory/iterator.hpp"
#include "runtime/globals.hpp"
#include "runtime/os.hpp"
#include "utilities/globalDefinitions.hpp"

class VTBufferChunk {
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
    fatal("Should not reach here");
    VTBufferChunk::init(this, thread);
  }

  static void init(VTBufferChunk* chunk, JavaThread* thread) {
    chunk->_magic = MAGIC_NUMBER;
    chunk->_index = -1;
    chunk->_prev = NULL;
    chunk->_next = NULL;
    chunk->_owner = thread;
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

  bool contains(void* address) {
    return address > (char*)chunk(address) && address < ((char*)chunk(address) + chunk_size());
  }

  void zap(void *start);
};

/* VTBuffer is a thread-local buffer used to store values, or TLVB (Thread-Local Value Buffer).
 * Values allocated in the TLVB have the same layout as values allocated in the Java heap:
 * same header size, same offsets for fields. The only difference is on the meaning of the
 * mark word: in a buffered value, the mark word contains an oop pointing to the Java mirror
 * of the value's class, with the two least significant bits used for internal marking.
 * Values allocated in the TLVB are references through oops, however, because TLVBs are not
 * part of the Java heap, those oops *must never be exposed to GCs*. But buffered values
 * can contain references to Java heap allocated objects or values, in addition to the
 * reference to the Java mirror, and these oops have to be processed by GC. The solution is
 * to let GC closures iterate over the internal oops, but not directly on the buffered value
 * itself (see ValueKlass::iterate_over_inside_oops() method).
 */

class VTBuffer : AllStatic {
  friend class VMStructs;
  friend class TemplateTable;

private:
  static address _base;
  static size_t _size;
  static address _commit_ptr;

  static VTBufferChunk* _free_list;
  static Mutex* _pool_lock;
  static int _pool_counter;
  static int _max_pool_counter;
  static int _total_allocated;
  static int _total_failed;

public:
  static void init();
  static address base() { return _base; }
  static size_t  size() { return _size; }


  static address top_addr() { return _base; }
  static address end_addr() { return _base + _size; }

  static VTBufferChunk* get_new_chunk(JavaThread* thread);

  static Mutex* lock() { return _pool_lock; }
  static oop allocate_value(ValueKlass* k, TRAPS);
  static bool allocate_vt_chunk(JavaThread* thread);
  static void recycle_chunk(JavaThread* thread, VTBufferChunk* chunk);
  static void return_vt_chunk(JavaThread* thread, VTBufferChunk* chunk);

  static int in_pool() { return _pool_counter; }
  static int max_in_pool() { return _max_pool_counter; }
  static int total_allocated() { return _total_allocated; }
  static int total_failed() { return _total_failed; }

  static bool is_in_vt_buffer(const void* p) {
#ifdef ASSERT
    if (p >= _base && p < (_base + _size)) {
      assert(p < _commit_ptr, "should not point to an uncommited page");
      intptr_t chunk_mask = (~(VTBufferChunk::chunk_size() - 1));
      VTBufferChunk* c = (VTBufferChunk*)((intptr_t)p & chunk_mask);
      assert(c->is_valid(), "Sanity check");
    }
#endif // ASSERT
    return p >= _base && p < (_base + _size);
  }

  static bool value_belongs_to_frame(oop p, frame *f);
  static bool is_value_allocated_after(oop p, void* a);
  static void recycle_vt_in_frame(JavaThread* thread, frame* f);
  static void recycle_vtbuffer(JavaThread *thread, void* alloc_ptr);
  static address relocate_value(address old, address previous, int previous_size_in_words);
  static oop relocate_return_value(JavaThread* thread, void* alloc_ptr, oop old);

  static void fix_frame_vt_alloc_ptr(frame fr, VTBufferChunk* chunk);

  enum Mark {
    mark_A = 1,
    mark_B = 2,
    mark_mask = 3
  };

  static Mark switch_mark(Mark m) {
    assert(m == mark_A || m == mark_B, "Sanity check");
    return m == mark_A ? mark_B : mark_A;
  }

};

struct VT_relocation_entry {
  int chunk_index;
  address old_ptr;
  address new_ptr;
  markOop mark_word;
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

/* Value buffered in a TLVB expose their internal oops as roots for GCs.
 * A GC root must only be processed once by each GC closure. However,
 * a Java Thread can have multiple oops (aliases) pointing to the same
 * buffered value (from local variable entries, operand stack slots,
 * Handles or runtime data structures). To prevent duplicated processing
 * of a buffered value, each function processing a Java Thread's GC roots
 * must allocates a BufferedValuesDealiaser which uses a marking mechanism
 * to avoid processing a buffered value twice.
 */
class BufferedValuesDealiaser : public StackObj {
private:
  const JavaThread* _target;
  VTBuffer::Mark _current_mark;

public:
  BufferedValuesDealiaser(JavaThread* thread);
  VTBuffer::Mark current_mark() const { return _current_mark; }
  void oops_do(OopClosure* f, oop value);

  ~BufferedValuesDealiaser();
};

#endif /* SHARE_VM_MEMORY_VTBUFFER_HPP */
