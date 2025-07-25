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

#include "interpreter/bytecodeStream.hpp"
#include "interpreter/oopMapCache.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/generateOopMap.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/signature.hpp"
#include "utilities/globalCounter.inline.hpp"

class OopMapCacheEntry: private InterpreterOopMap {
  friend class InterpreterOopMap;
  friend class OopMapForCacheEntry;
  friend class OopMapCache;
  friend class VerifyClosure;

 private:
  OopMapCacheEntry* _next;

 protected:
  // Initialization
  void fill(const methodHandle& method, int bci);
  // fills the bit mask for native calls
  void fill_for_native(const methodHandle& method);
  void set_mask(CellTypeState* vars, CellTypeState* stack, int stack_top);

  // Deallocate bit masks and initialize fields
  void flush();

  static void deallocate(OopMapCacheEntry* const entry);

 private:
  void allocate_bit_mask();   // allocates the bit mask on C heap f necessary
  void deallocate_bit_mask(); // allocates the bit mask on C heap f necessary
  bool verify_mask(CellTypeState *vars, CellTypeState *stack, int max_locals, int stack_top);

 public:
  OopMapCacheEntry() : InterpreterOopMap() {
    _next = nullptr;
  }
};


// Implementation of OopMapForCacheEntry
// (subclass of GenerateOopMap, initializes an OopMapCacheEntry for a given method and bci)

class OopMapForCacheEntry: public GenerateOopMap {
  OopMapCacheEntry *_entry;
  int               _bci;
  int               _stack_top;

  virtual bool report_results() const     { return false; }
  virtual bool possible_gc_point          (BytecodeStream *bcs);
  virtual void fill_stackmap_prolog       (int nof_gc_points);
  virtual void fill_stackmap_epilog       ();
  virtual void fill_stackmap_for_opcodes  (BytecodeStream *bcs,
                                           CellTypeState* vars,
                                           CellTypeState* stack,
                                           int stack_top);
  virtual void fill_init_vars             (GrowableArray<intptr_t> *init_vars);

 public:
  OopMapForCacheEntry(const methodHandle& method, int bci, OopMapCacheEntry *entry);

  // Computes stack map for (method,bci) and initialize entry
  bool compute_map(Thread* current);
  int  size();
};


OopMapForCacheEntry::OopMapForCacheEntry(const methodHandle& method, int bci, OopMapCacheEntry* entry) : GenerateOopMap(method) {
  _bci       = bci;
  _entry     = entry;
  _stack_top = -1;
}


bool OopMapForCacheEntry::compute_map(Thread* current) {
  assert(!method()->is_native(), "cannot compute oop map for native methods");
  // First check if it is a method where the stackmap is always empty
  if (method()->code_size() == 0 || method()->max_locals() + method()->max_stack() == 0) {
    _entry->set_mask_size(0);
  } else {
    ResourceMark rm;
    if (!GenerateOopMap::compute_map(current)) {
      fatal("Unrecoverable verification or out-of-memory error");
      return false;
    }
    result_for_basicblock(_bci);
  }
  return true;
}


bool OopMapForCacheEntry::possible_gc_point(BytecodeStream *bcs) {
  return false; // We are not reporting any result. We call result_for_basicblock directly
}


void OopMapForCacheEntry::fill_stackmap_prolog(int nof_gc_points) {
  // Do nothing
}


void OopMapForCacheEntry::fill_stackmap_epilog() {
  // Do nothing
}


void OopMapForCacheEntry::fill_init_vars(GrowableArray<intptr_t> *init_vars) {
  // Do nothing
}


void OopMapForCacheEntry::fill_stackmap_for_opcodes(BytecodeStream *bcs,
                                                    CellTypeState* vars,
                                                    CellTypeState* stack,
                                                    int stack_top) {
  // Only interested in one specific bci
  if (bcs->bci() == _bci) {
    _entry->set_mask(vars, stack, stack_top);
    _stack_top = stack_top;
  }
}


int OopMapForCacheEntry::size() {
  assert(_stack_top != -1, "compute_map must be called first");
  return ((method()->is_static()) ? 0 : 1) + method()->max_locals() + _stack_top;
}


// Implementation of InterpreterOopMap and OopMapCacheEntry

class VerifyClosure : public OffsetClosure {
 private:
  OopMapCacheEntry* _entry;
  bool              _failed;

 public:
  VerifyClosure(OopMapCacheEntry* entry)         { _entry = entry; _failed = false; }
  void offset_do(int offset)                     { if (!_entry->is_oop(offset)) _failed = true; }
  bool failed() const                            { return _failed; }
};

InterpreterOopMap::InterpreterOopMap() {
  initialize();
}

InterpreterOopMap::~InterpreterOopMap() {
  if (has_valid_mask() && mask_size() > small_mask_limit) {
    assert(_bit_mask[0] != 0, "should have pointer to C heap");
    FREE_C_HEAP_ARRAY(uintptr_t, _bit_mask[0]);
  }
}

bool InterpreterOopMap::is_empty() const {
  bool result = _method == nullptr;
  assert(_method != nullptr || (_bci == 0 &&
    (_mask_size == 0 || _mask_size == USHRT_MAX) &&
    _bit_mask[0] == 0), "Should be completely empty");
  return result;
}

void InterpreterOopMap::initialize() {
  _method    = nullptr;
  _mask_size = USHRT_MAX;  // This value should cause a failure quickly
  _bci       = 0;
  _expression_stack_size = 0;
  _num_oops  = 0;
  for (int i = 0; i < N; i++) _bit_mask[i] = 0;
}

void InterpreterOopMap::iterate_oop(OffsetClosure* oop_closure) const {
  int n = number_of_entries();
  int word_index = 0;
  uintptr_t value = 0;
  uintptr_t mask = 0;
  // iterate over entries
  for (int i = 0; i < n; i++, mask <<= bits_per_entry) {
    // get current word
    if (mask == 0) {
      value = bit_mask()[word_index++];
      mask = 1;
    }
    // test for oop
    if ((value & (mask << oop_bit_number)) != 0) oop_closure->offset_do(i);
  }
}

void InterpreterOopMap::print() const {
  int n = number_of_entries();
  tty->print("oop map for ");
  method()->print_value();
  tty->print(" @ %d = [%d] { ", bci(), n);
  for (int i = 0; i < n; i++) {
    if (is_dead(i)) tty->print("%d+ ", i);
    else
    if (is_oop(i)) tty->print("%d ", i);
  }
  tty->print_cr("}");
}

class MaskFillerForNative: public NativeSignatureIterator {
 private:
  uintptr_t * _mask;                             // the bit mask to be filled
  int         _size;                             // the mask size in bits
  int         _num_oops;

  void set_one(int i) {
    _num_oops++;
    i *= InterpreterOopMap::bits_per_entry;
    assert(0 <= i && i < _size, "offset out of bounds");
    _mask[i / BitsPerWord] |= (((uintptr_t) 1 << InterpreterOopMap::oop_bit_number) << (i % BitsPerWord));
  }

 public:
  void pass_byte()                               { /* ignore */ }
  void pass_short()                              { /* ignore */ }
  void pass_int()                                { /* ignore */ }
  void pass_long()                               { /* ignore */ }
  void pass_float()                              { /* ignore */ }
  void pass_double()                             { /* ignore */ }
  void pass_object()                             { set_one(offset()); }

  MaskFillerForNative(const methodHandle& method, uintptr_t* mask, int size) : NativeSignatureIterator(method) {
    _mask   = mask;
    _size   = size;
    _num_oops = 0;
    // initialize with 0
    int i = (size + BitsPerWord - 1) / BitsPerWord;
    while (i-- > 0) _mask[i] = 0;
  }

  void generate() {
    iterate();
  }

  int num_oops() { return _num_oops; }
};

bool OopMapCacheEntry::verify_mask(CellTypeState* vars, CellTypeState* stack, int max_locals, int stack_top) {
  // Check mask includes map
  VerifyClosure blk(this);
  iterate_oop(&blk);
  if (blk.failed()) return false;

  // Check if map is generated correctly
  // (Use ?: operator to make sure all 'true' & 'false' are represented exactly the same so we can use == afterwards)
  const bool log = log_is_enabled(Trace, interpreter, oopmap);
  LogStream st(Log(interpreter, oopmap)::trace());

  if (log) st.print("Locals (%d): ", max_locals);
  for(int i = 0; i < max_locals; i++) {
    bool v1 = is_oop(i)               ? true : false;
    bool v2 = vars[i].is_reference();
    assert(v1 == v2, "locals oop mask generation error");
    if (log) st.print("%d", v1 ? 1 : 0);
  }
  if (log) st.cr();

  if (log) st.print("Stack (%d): ", stack_top);
  for(int j = 0; j < stack_top; j++) {
    bool v1 = is_oop(max_locals + j)  ? true : false;
    bool v2 = stack[j].is_reference();
    assert(v1 == v2, "stack oop mask generation error");
    if (log) st.print("%d", v1 ? 1 : 0);
  }
  if (log) st.cr();
  return true;
}

void OopMapCacheEntry::allocate_bit_mask() {
  if (mask_size() > small_mask_limit) {
    assert(_bit_mask[0] == 0, "bit mask should be new or just flushed");
    _bit_mask[0] = (intptr_t)
      NEW_C_HEAP_ARRAY(uintptr_t, mask_word_size(), mtClass);
  }
}

void OopMapCacheEntry::deallocate_bit_mask() {
  if (mask_size() > small_mask_limit && _bit_mask[0] != 0) {
    assert(!Thread::current()->resource_area()->contains((void*)_bit_mask[0]),
      "This bit mask should not be in the resource area");
    FREE_C_HEAP_ARRAY(uintptr_t, _bit_mask[0]);
    DEBUG_ONLY(_bit_mask[0] = 0;)
  }
}


void OopMapCacheEntry::fill_for_native(const methodHandle& mh) {
  assert(mh->is_native(), "method must be native method");
  set_mask_size(mh->size_of_parameters() * bits_per_entry);
  allocate_bit_mask();
  // fill mask for parameters
  MaskFillerForNative mf(mh, bit_mask(), mask_size());
  mf.generate();
  _num_oops = mf.num_oops();
}


void OopMapCacheEntry::fill(const methodHandle& method, int bci) {
  // Flush entry to deallocate an existing entry
  flush();
  set_method(method());
  set_bci(checked_cast<unsigned short>(bci));  // bci is always u2
  if (method->is_native()) {
    // Native method activations have oops only among the parameters and one
    // extra oop following the parameters (the mirror for static native methods).
    fill_for_native(method);
  } else {
    OopMapForCacheEntry gen(method, bci, this);
    if (!gen.compute_map(Thread::current())) {
      fatal("Unrecoverable verification or out-of-memory error");
    }
  }
}


void OopMapCacheEntry::set_mask(CellTypeState *vars, CellTypeState *stack, int stack_top) {
  // compute bit mask size
  int max_locals = method()->max_locals();
  int n_entries = max_locals + stack_top;
  set_mask_size(n_entries * bits_per_entry);
  allocate_bit_mask();
  set_expression_stack_size(stack_top);

  // compute bits
  int word_index = 0;
  uintptr_t value = 0;
  uintptr_t mask = 1;

  _num_oops = 0;
  CellTypeState* cell = vars;
  for (int entry_index = 0; entry_index < n_entries; entry_index++, mask <<= bits_per_entry, cell++) {
    // store last word
    if (mask == 0) {
      bit_mask()[word_index++] = value;
      value = 0;
      mask = 1;
    }

    // switch to stack when done with locals
    if (entry_index == max_locals) {
      cell = stack;
    }

    // set oop bit
    if (cell->is_reference()) {
      value |= (mask << oop_bit_number );
      _num_oops++;
    }

    // set dead bit
    if (!cell->is_live()) {
      value |= (mask << dead_bit_number);
      assert(!cell->is_reference(), "dead value marked as oop");
    }
  }

  // make sure last word is stored
  bit_mask()[word_index] = value;

  // verify bit mask
  assert(verify_mask(vars, stack, max_locals, stack_top), "mask could not be verified");
}

void OopMapCacheEntry::flush() {
  deallocate_bit_mask();
  initialize();
}

void OopMapCacheEntry::deallocate(OopMapCacheEntry* const entry) {
  entry->flush();
  FREE_C_HEAP_OBJ(entry);
}

// Implementation of OopMapCache

void InterpreterOopMap::copy_from(const OopMapCacheEntry* src) {
  // The expectation is that this InterpreterOopMap is recently created
  // and empty. It is used to get a copy of a cached entry.
  assert(!has_valid_mask(), "InterpreterOopMap object can only be filled once");
  assert(src->has_valid_mask(), "Cannot copy entry with an invalid mask");

  set_method(src->method());
  set_bci(src->bci());
  set_mask_size(src->mask_size());
  set_expression_stack_size(src->expression_stack_size());
  _num_oops = src->num_oops();

  // Is the bit mask contained in the entry?
  if (src->mask_size() <= small_mask_limit) {
    memcpy(_bit_mask, src->_bit_mask, mask_word_size() * BytesPerWord);
  } else {
    _bit_mask[0] = (uintptr_t) NEW_C_HEAP_ARRAY(uintptr_t, mask_word_size(), mtClass);
    memcpy((void*) _bit_mask[0], (void*) src->_bit_mask[0], mask_word_size() * BytesPerWord);
  }
}

inline unsigned int OopMapCache::hash_value_for(const methodHandle& method, int bci) const {
  // We use method->code_size() rather than method->identity_hash() below since
  // the mark may not be present if a pointer to the method is already reversed.
  return   ((unsigned int) bci)
         ^ ((unsigned int) method->max_locals()         << 2)
         ^ ((unsigned int) method->code_size()          << 4)
         ^ ((unsigned int) method->size_of_parameters() << 6);
}

OopMapCacheEntry* volatile OopMapCache::_old_entries = nullptr;

OopMapCache::OopMapCache() {
  for(int i = 0; i < size; i++) _array[i] = nullptr;
}


OopMapCache::~OopMapCache() {
  // Deallocate oop maps that are allocated out-of-line
  flush();
}

OopMapCacheEntry* OopMapCache::entry_at(int i) const {
  return Atomic::load_acquire(&(_array[i % size]));
}

bool OopMapCache::put_at(int i, OopMapCacheEntry* entry, OopMapCacheEntry* old) {
  return Atomic::cmpxchg(&_array[i % size], old, entry) == old;
}

void OopMapCache::flush() {
  for (int i = 0; i < size; i++) {
    OopMapCacheEntry* entry = _array[i];
    if (entry != nullptr) {
      _array[i] = nullptr;  // no barrier, only called in OopMapCache destructor
      OopMapCacheEntry::deallocate(entry);
    }
  }
}

void OopMapCache::flush_obsolete_entries() {
  assert(SafepointSynchronize::is_at_safepoint(), "called by RedefineClasses in a safepoint");
  for (int i = 0; i < size; i++) {
    OopMapCacheEntry* entry = _array[i];
    if (entry != nullptr && !entry->is_empty() && entry->method()->is_old()) {
      // Cache entry is occupied by an old redefined method and we don't want
      // to pin it down so flush the entry.
      if (log_is_enabled(Debug, redefine, class, oopmap)) {
        ResourceMark rm;
        log_debug(redefine, class, interpreter, oopmap)
          ("flush: %s(%s): cached entry @%d",
           entry->method()->name()->as_C_string(), entry->method()->signature()->as_C_string(), i);
      }
      _array[i] = nullptr;
      OopMapCacheEntry::deallocate(entry);
    }
  }
}

// Lookup or compute/cache the entry.
void OopMapCache::lookup(const methodHandle& method,
                         int bci,
                         InterpreterOopMap* entry_for) {
  int probe = hash_value_for(method, bci);

  if (log_is_enabled(Debug, interpreter, oopmap)) {
    static int count = 0;
    ResourceMark rm;
    log_debug(interpreter, oopmap)
          ("%d - Computing oopmap at bci %d for %s at hash %d", ++count, bci,
           method()->name_and_sig_as_C_string(), probe);
  }

  // Search hashtable for match.
  // Need a critical section to avoid race against concurrent reclamation.
  {
    GlobalCounter::CriticalSection cs(Thread::current());
    for (int i = 0; i < probe_depth; i++) {
      OopMapCacheEntry *entry = entry_at(probe + i);
      if (entry != nullptr && !entry->is_empty() && entry->match(method, bci)) {
        entry_for->copy_from(entry);
        assert(!entry_for->is_empty(), "A non-empty oop map should be returned");
        log_debug(interpreter, oopmap)("- found at hash %d", probe + i);
        return;
      }
    }
  }

  // Entry is not in hashtable.
  // Compute entry

  OopMapCacheEntry* tmp = NEW_C_HEAP_OBJ(OopMapCacheEntry, mtClass);
  tmp->initialize();
  tmp->fill(method, bci);
  entry_for->copy_from(tmp);

  if (method->should_not_be_cached()) {
    // It is either not safe or not a good idea to cache this Method*
    // at this time. We give the caller of lookup() a copy of the
    // interesting info via parameter entry_for, but we don't add it to
    // the cache. See the gory details in Method*.cpp.
    OopMapCacheEntry::deallocate(tmp);
    return;
  }

  // First search for an empty slot
  for (int i = 0; i < probe_depth; i++) {
    OopMapCacheEntry* entry = entry_at(probe + i);
    if (entry == nullptr) {
      if (put_at(probe + i, tmp, nullptr)) {
        assert(!entry_for->is_empty(), "A non-empty oop map should be returned");
        return;
      }
    }
  }

  log_debug(interpreter, oopmap)("*** collision in oopmap cache - flushing item ***");

  // No empty slot (uncommon case). Use (some approximation of a) LRU algorithm
  // where the first entry in the collision array is replaced with the new one.
  OopMapCacheEntry* old = entry_at(probe + 0);
  if (put_at(probe + 0, tmp, old)) {
    // Cannot deallocate old entry on the spot: it can still be used by readers
    // that got a reference to it before we were able to replace it in the map.
    // Instead of synchronizing on GlobalCounter here and incurring heavy thread
    // walk, we do this clean up out of band.
    enqueue_for_cleanup(old);
  } else {
    OopMapCacheEntry::deallocate(tmp);
  }

  assert(!entry_for->is_empty(), "A non-empty oop map should be returned");
  return;
}

void OopMapCache::enqueue_for_cleanup(OopMapCacheEntry* entry) {
  while (true) {
    OopMapCacheEntry* head = Atomic::load(&_old_entries);
    entry->_next = head;
    if (Atomic::cmpxchg(&_old_entries, head, entry) == head) {
      // Enqueued successfully.
      break;
    }
  }

  if (log_is_enabled(Debug, interpreter, oopmap)) {
    ResourceMark rm;
    log_debug(interpreter, oopmap)("enqueue %s at bci %d for cleanup",
                          entry->method()->name_and_sig_as_C_string(), entry->bci());
  }
}

bool OopMapCache::has_cleanup_work() {
  return Atomic::load(&_old_entries) != nullptr;
}

void OopMapCache::try_trigger_cleanup() {
  // See we can take the lock for the notification without blocking.
  // This allows triggering the cleanup from GC paths, that can hold
  // the service lock for e.g. oop iteration in service thread.
  if (has_cleanup_work() && Service_lock->try_lock_without_rank_check()) {
    Service_lock->notify_all();
    Service_lock->unlock();
  }
}

void OopMapCache::cleanup() {
  OopMapCacheEntry* entry = Atomic::xchg(&_old_entries, (OopMapCacheEntry*)nullptr);
  if (entry == nullptr) {
    // No work.
    return;
  }

  // About to delete the entries than might still be accessed by other threads
  // on lookup path. Need to sync up with them before proceeding.
  GlobalCounter::write_synchronize();

  while (entry != nullptr) {
    if (log_is_enabled(Debug, interpreter, oopmap)) {
      ResourceMark rm;
      log_debug(interpreter, oopmap)("cleanup entry %s at bci %d",
                          entry->method()->name_and_sig_as_C_string(), entry->bci());
    }
    OopMapCacheEntry* next = entry->_next;
    OopMapCacheEntry::deallocate(entry);
    entry = next;
  }
}

void OopMapCache::compute_one_oop_map(const methodHandle& method, int bci, InterpreterOopMap* entry) {
  // Due to the invariants above it's tricky to allocate a temporary OopMapCacheEntry on the stack
  OopMapCacheEntry* tmp = NEW_C_HEAP_OBJ(OopMapCacheEntry, mtClass);
  tmp->initialize();
  tmp->fill(method, bci);
  if (tmp->has_valid_mask()) {
    entry->copy_from(tmp);
  }
  OopMapCacheEntry::deallocate(tmp);
}
